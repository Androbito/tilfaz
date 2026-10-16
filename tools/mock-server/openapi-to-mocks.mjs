#!/usr/bin/env node
// Generate mock route files from an OpenAPI 3.x spec (JSON, or YAML if the `yaml` package is installed).
// Uses declared examples first, then builds realistic sample data from schemas.
// Node 18+. MIT License.
//
// Usage:
//   node openapi-to-mocks.mjs <spec.json|spec.yaml> [--out ./mocks] [--force]
//
// Existing files are kept unless --force is passed, so hand-edited mocks are never overwritten.

import fs from "node:fs";
import path from "node:path";

const argv = process.argv.slice(2);
const specPath = argv.find((a) => !a.startsWith("--"));
const outDir = path.resolve(argValue("--out") ?? "./mocks");
const force = argv.includes("--force");

if (!specPath) {
  console.error("Usage: node openapi-to-mocks.mjs <spec.json|spec.yaml> [--out ./mocks] [--force]");
  process.exit(1);
}

const spec = await loadSpec(specPath);
const methods = ["get", "post", "put", "patch", "delete"];
let created = 0, skipped = 0;

for (const [rawPath, item] of Object.entries(spec.paths ?? {})) {
  for (const method of methods) {
    const op = item[method];
    if (!op) continue;
    const mockPath = rawPath.replace(/\{(\w+)\}/g, ":$1");
    const scenarios = {};

    for (const [code, response] of Object.entries(op.responses ?? {})) {
      const status = code === "default" ? 500 : Number(code.replace(/X/gi, "0"));
      const resolved = deref(response);
      const media = resolved.content?.["application/json"] ?? Object.values(resolved.content ?? {})[0];
      let body;
      if (media) {
        if (media.example !== undefined) body = media.example;
        else if (media.examples) body = deref(Object.values(media.examples)[0])?.value;
        else if (media.schema) body = sample(media.schema, 0, path.basename(mockPath));
      }
      const name = status >= 200 && status < 300 && !scenarios.default ? "default" : scenarioName(status, resolved.description);
      scenarios[name] = { status, ...(body !== undefined && status !== 204 ? { body } : {}) };
    }

    if (!scenarios.default) scenarios.default = { status: 200, body: {} };
    scenarios.slow = { ...scenarios.default, delayMs: 3000 };
    scenarios.server_error = { status: 500, body: { error: "INTERNAL_ERROR", message: "Mocked server error" } };
    scenarios.offline = { dropConnection: true };

    const route = {
      method: method.toUpperCase(),
      path: mockPath,
      ...(op.operationId ? { operationId: op.operationId } : {}),
      ...(op.summary ? { summary: op.summary } : {}),
      scenarios,
    };

    const fileName = `${method.toUpperCase()}_${mockPath.replace(/^\//, "").replace(/[^\w-]+/g, "_") || "root"}.json`;
    const file = path.join(outDir, fileName);
    if (fs.existsSync(file) && !force) { skipped++; continue; }
    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(route, null, 2) + "\n");
    created++;
  }
}

console.log(`Generated ${created} mock file(s) in ${outDir}${skipped ? `, skipped ${skipped} existing (use --force to overwrite)` : ""}.`);

// ---------- helpers ----------
function argValue(flag) {
  const i = argv.indexOf(flag);
  return i >= 0 ? argv[i + 1] : undefined;
}

async function loadSpec(file) {
  const text = fs.readFileSync(file, "utf8");
  if (/\.ya?ml$/i.test(file)) {
    try {
      const { parse } = await import("yaml");
      return parse(text);
    } catch {
      console.error("YAML spec detected. Run `npm i yaml` next to this script, or convert to JSON first.");
      process.exit(1);
    }
  }
  return JSON.parse(text);
}

function deref(node, seen = new Set()) {
  if (!node || typeof node !== "object" || !node.$ref) return node;
  if (seen.has(node.$ref)) return {};
  seen.add(node.$ref);
  const target = node.$ref.replace(/^#\//, "").split("/").reduce((acc, k) => acc?.[k.replace(/~1/g, "/").replace(/~0/g, "~")], spec);
  return deref(target, seen);
}

function scenarioName(status, description = "") {
  const known = { 400: "bad_request", 401: "unauthorized", 403: "forbidden", 404: "not_found", 409: "conflict", 422: "validation_error", 429: "rate_limited", 500: "server_error", 503: "unavailable" };
  return known[status] ?? (description ? description.toLowerCase().replace(/[^a-z0-9]+/g, "_").slice(0, 30) : `status_${status}`);
}

function sample(schemaIn, depth, hint = "") {
  const schema = deref(schemaIn);
  if (!schema || depth > 6) return null;
  if (schema.example !== undefined) return schema.example;
  if (schema.examples?.length) return schema.examples[0];
  if (schema.default !== undefined) return schema.default;
  if (schema.enum) return schema.enum[0];
  if (schema.const !== undefined) return schema.const;
  if (schema.allOf) return Object.assign({}, ...schema.allOf.map((s) => sample(s, depth + 1, hint)));
  if (schema.oneOf || schema.anyOf) return sample((schema.oneOf ?? schema.anyOf)[0], depth + 1, hint);

  const type = Array.isArray(schema.type) ? schema.type.find((t) => t !== "null") : schema.type ?? (schema.properties ? "object" : undefined);
  switch (type) {
    case "object": {
      const out = {};
      for (const [k, v] of Object.entries(schema.properties ?? {})) out[k] = sample(v, depth + 1, k);
      return out;
    }
    case "array": {
      const n = Math.max(schema.minItems ?? 2, 1);
      return Array.from({ length: Math.min(n, 3) }, (_, i) => withIndex(sample(schema.items ?? {}, depth + 1, hint), i));
    }
    case "integer": return clamp(intFor(hint), schema);
    case "number": return clamp(Number((intFor(hint) + 0.99).toFixed(2)), schema);
    case "boolean": return true;
    case "string": return stringFor(schema, hint);
    default: return null;
  }
}

function withIndex(value, i) {
  if (i === 0 || !value || typeof value !== "object") return value;
  const copy = structuredClone(value);
  for (const k of Object.keys(copy)) {
    if (/^id$|Id$|_id$/.test(k) && typeof copy[k] === "string") copy[k] = `${copy[k]}-${i + 1}`;
    if (/^id$|Id$|_id$/.test(k) && typeof copy[k] === "number") copy[k] = copy[k] + i;
  }
  return copy;
}

function clamp(n, s) {
  if (s.minimum !== undefined && n < s.minimum) return s.minimum;
  if (s.maximum !== undefined && n > s.maximum) return s.maximum;
  return n;
}

function intFor(hint) {
  const h = hint.toLowerCase();
  if (/price|amount|total|cents/.test(h)) return 4990;
  if (/count|quantity|qty/.test(h)) return 2;
  if (/age/.test(h)) return 34;
  if (/year/.test(h)) return 2026;
  if (/page/.test(h)) return 1;
  if (/id$/.test(h)) return 1001;
  return 42;
}

function stringFor(schema, hint) {
  const h = hint.toLowerCase();
  switch (schema.format) {
    case "date-time": return "2026-10-03T10:00:00Z";
    case "date": return "2026-10-03";
    case "email": return "jane.doe@example.com";
    case "uuid": return "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    case "uri": case "url": return "https://example.com/resource";
    case "byte": return "aGVsbG8=";
  }
  if (/^id$|id$/.test(h)) return "id-1";
  if (/email/.test(h)) return "jane.doe@example.com";
  if (/first.?name/.test(h)) return "Jane";
  if (/last.?name/.test(h)) return "Doe";
  if (/name|title/.test(h)) return "Sample " + (hint || "name");
  if (/phone/.test(h)) return "+33 6 12 34 56 78";
  if (/currency/.test(h)) return "EUR";
  if (/country/.test(h)) return "FR";
  if (/city/.test(h)) return "Paris";
  if (/url|image|avatar|photo/.test(h)) return "https://picsum.photos/200";
  if (/status|state/.test(h)) return "ACTIVE";
  if (/token/.test(h)) return "mock-token-123";
  if (/description|message|text|comment/.test(h)) return "Lorem ipsum dolor sit amet.";
  return "string";
}
