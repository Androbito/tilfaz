#!/usr/bin/env node
// Zero-dependency mock API server for mobile development.
// Node 18+. MIT License.
//
// Usage:
//   node mock-server.mjs [--dir ./mocks] [--port 3001] [--host 0.0.0.0]
//                        [--delay 0] [--fail-rate 0] [--scenario default]
//                        [--proxy https://dev-api.example.com] [--record]
//
// Mock file format (any *.json file under --dir, subfolders allowed):
// {
//   "method": "GET",
//   "path": "/orders/:id",
//   "scenarios": {
//     "default":   { "status": 200, "body": { "id": "{{params.id}}" }, "delayMs": 200 },
//     "not_found": { "status": 404, "body": { "error": "ORDER_NOT_FOUND" } }
//   }
// }
// A file may also contain an array of such route objects.

import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

// ---------- CLI ----------
const args = parseArgs(process.argv.slice(2));
const config = {
  dir: path.resolve(args.dir ?? "./mocks"),
  port: Number(args.port ?? process.env.MOCK_PORT ?? 3001),
  host: args.host ?? "0.0.0.0",
  delay: Number(args.delay ?? 0),
  failRate: Number(args["fail-rate"] ?? 0),
  scenario: args.scenario ?? "default",
  proxy: args.proxy ? String(args.proxy).replace(/\/$/, "") : null,
  record: Boolean(args.record),
};

if (args.help || args.h) {
  console.log(fs.readFileSync(new URL(import.meta.url)).toString().split("\n").slice(1, 20).join("\n"));
  process.exit(0);
}

// Runtime overrides set through the admin API: { "<METHOD> <path>": "<scenario>" }
const scenarioOverrides = {};

// ---------- Loading mocks (re-read on every request = hot reload) ----------
function loadRoutes() {
  const routes = [];
  if (!fs.existsSync(config.dir)) return routes;
  for (const file of walk(config.dir)) {
    if (!file.endsWith(".json") || file.includes(`${path.sep}fixtures${path.sep}`)) continue;
    let parsed;
    try {
      parsed = JSON.parse(fs.readFileSync(file, "utf8"));
    } catch (e) {
      console.error(`  ! invalid JSON in ${path.relative(config.dir, file)}: ${e.message}`);
      continue;
    }
    for (const route of Array.isArray(parsed) ? parsed : [parsed]) {
      if (!route || !route.method || !route.path || !route.scenarios) continue;
      routes.push({ ...route, method: route.method.toUpperCase(), file, matcher: compilePath(route.path) });
    }
  }
  // Static segments win over params: sort by number of params ascending, then by length descending.
  // Routes that also match on query parameters (e.g. Xtream's ?action=...) win over routes on the same path without them.
  routes.sort(
    (a, b) =>
      a.matcher.paramCount - b.matcher.paramCount ||
      Object.keys(b.query ?? {}).length - Object.keys(a.query ?? {}).length ||
      b.path.length - a.path.length,
  );
  return routes;
}

function* walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) yield* walk(full);
    else yield full;
  }
}

function compilePath(p) {
  const names = [];
  const pattern = p
    .replace(/\/+$/, "")
    .split("/")
    .map((seg) => {
      if (seg === "*") return ".*";
      const m = seg.match(/^:(\w+)$/) || seg.match(/^\{(\w+)\}$/);
      if (m) {
        names.push(m[1]);
        return "([^/]+)";
      }
      return seg.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    })
    .join("/");
  return { regex: new RegExp(`^${pattern || ""}/?$`), names, paramCount: names.length };
}

function matchRoute(routes, method, pathname, searchParams) {
  for (const route of routes) {
    if (route.method !== method && route.method !== "ANY") continue;
    const m = pathname.match(route.matcher.regex);
    if (!m) continue;
    // Optional query matcher: { "query": { "action": "get_live_streams" } }. A "*" value only requires presence.
    if (route.query && searchParams) {
      const ok = Object.entries(route.query).every(([k, v]) => (v === "*" ? searchParams.has(k) : searchParams.get(k) === String(v)));
      if (!ok) continue;
    }
    const params = {};
    route.matcher.names.forEach((n, i) => (params[n] = decodeURIComponent(m[i + 1])));
    return { route, params };
  }
  return null;
}

// ---------- Templating ----------
function render(value, ctx) {
  if (typeof value === "string") {
    const whole = value.match(/^\{\{\s*([^}]+?)\s*\}\}$/);
    if (whole) {
      const v = resolveExpr(whole[1], ctx);
      return v === undefined ? value : v; // keep type (number, object) when the whole string is a token
    }
    return value.replace(/\{\{\s*([^}]+?)\s*\}\}/g, (_, expr) => {
      const v = resolveExpr(expr, ctx);
      return v === undefined ? "" : typeof v === "object" ? JSON.stringify(v) : String(v);
    });
  }
  if (Array.isArray(value)) return value.map((v) => render(v, ctx));
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, render(v, ctx)]));
  }
  return value;
}

function resolveExpr(expr, ctx) {
  switch (expr) {
    case "uuid": return crypto.randomUUID();
    case "now": return new Date().toISOString();
    case "timestamp": return Date.now();
  }
  // {{unix}} and {{unix+3600}} / {{unix-1800}}: unix seconds relative to now, for EPG-style payloads.
  const unix = expr.match(/^unix\s*([+-]\s*\d+)?$/);
  if (unix) return Math.floor(Date.now() / 1000) + (unix[1] ? Number(unix[1].replace(/\s/g, "")) : 0);
  return expr.split(".").reduce((acc, key) => (acc == null ? undefined : acc[key]), ctx);
}

// ---------- Helpers ----------
function readBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => resolve(Buffer.concat(chunks)));
  });
}

function parseJsonSafe(buf) {
  if (!buf.length) return undefined;
  try { return JSON.parse(buf.toString("utf8")); } catch { return buf.toString("utf8"); }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function send(res, status, body, headers = {}) {
  const isJson = body !== undefined && typeof body !== "string" && !Buffer.isBuffer(body);
  const payload = body === undefined ? "" : isJson ? JSON.stringify(body, null, 2) : body;
  res.writeHead(status, {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "*",
    "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
    ...(isJson ? { "Content-Type": "application/json; charset=utf-8" } : {}),
    ...headers,
  });
  res.end(payload);
}

function log(method, url, status, note = "") {
  const color = status >= 500 ? 31 : status >= 400 ? 33 : 32;
  console.log(`  \x1b[${color}m${status}\x1b[0m ${method.padEnd(6)} ${url}${note ? `  \x1b[2m${note}\x1b[0m` : ""}`);
}

// ---------- Proxy + record ----------
async function proxyRequest(req, url, rawBody) {
  const target = config.proxy + url.pathname + url.search;
  const headers = { ...req.headers };
  delete headers.host;
  delete headers["content-length"];
  const upstream = await fetch(target, {
    method: req.method,
    headers,
    body: ["GET", "HEAD"].includes(req.method) ? undefined : rawBody,
  });
  const buf = Buffer.from(await upstream.arrayBuffer());
  const outHeaders = {};
  upstream.headers.forEach((v, k) => {
    if (!["content-encoding", "transfer-encoding", "connection", "content-length"].includes(k)) outHeaders[k] = v;
  });
  return { status: upstream.status, headers: outHeaders, body: buf };
}

function recordMock(method, pathname, status, body) {
  const safe = pathname.replace(/^\//, "").replace(/[^\w-]+/g, "_") || "root";
  const dir = path.join(config.dir, "_recorded");
  fs.mkdirSync(dir, { recursive: true });
  const file = path.join(dir, `${method}_${safe}.json`);
  const parsed = parseJsonSafe(body);
  const route = { method, path: pathname, scenarios: { default: { status, body: parsed } } };
  fs.writeFileSync(file, JSON.stringify(route, null, 2));
  return path.relative(config.dir, file);
}

// ---------- Admin API ----------
async function handleAdmin(req, res, url, routes, body) {
  const route = url.pathname.replace(/^\/__mock/, "");
  if (req.method === "GET" && (route === "" || route === "/" || route === "/routes")) {
    return send(res, 200, routes.map((r) => ({
      route: `${r.method} ${r.path}`,
      scenarios: Object.keys(r.scenarios),
      active: scenarioOverrides[`${r.method} ${r.path}`] ?? config.scenario,
      file: path.relative(config.dir, r.file),
    })));
  }
  if (req.method === "POST" && route === "/scenario") {
    // { "route": "GET /orders/:id", "scenario": "not_found" }  or  { "scenario": "slow" } for global
    const json = parseJsonSafe(body) ?? {};
    if (!json.scenario) return send(res, 400, { error: "scenario is required" });
    if (json.route) scenarioOverrides[json.route] = json.scenario;
    else config.scenario = json.scenario;
    return send(res, 200, { ok: true, global: config.scenario, overrides: scenarioOverrides });
  }
  if (req.method === "POST" && route === "/reset") {
    for (const k of Object.keys(scenarioOverrides)) delete scenarioOverrides[k];
    config.scenario = args.scenario ?? "default";
    return send(res, 200, { ok: true });
  }
  return send(res, 404, { error: "unknown admin endpoint" });
}

// ---------- Server ----------
const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host ?? "localhost"}`);
  const rawBody = await readBody(req);

  if (req.method === "OPTIONS") return send(res, 204);

  const routes = loadRoutes();
  if (url.pathname.startsWith("/__mock")) return handleAdmin(req, res, url, routes, rawBody);

  const match = matchRoute(routes, req.method, url.pathname, url.searchParams);

  if (!match) {
    if (config.proxy) {
      try {
        const up = await proxyRequest(req, url, rawBody);
        let note = "proxied";
        if (config.record && up.status < 500) note += `, recorded ${recordMock(req.method, url.pathname, up.status, up.body)}`;
        log(req.method, url.pathname + url.search, up.status, note);
        return send(res, up.status, up.body, up.headers);
      } catch (e) {
        log(req.method, url.pathname, 502, `proxy error: ${e.message}`);
        return send(res, 502, { error: "MOCK_PROXY_ERROR", message: e.message });
      }
    }
    log(req.method, url.pathname + url.search, 404, "no mock");
    return send(res, 404, {
      error: "MOCK_NOT_FOUND",
      message: `No mock for ${req.method} ${url.pathname}. Add a JSON file in ${config.dir}`,
    });
  }

  const { route, params } = match;
  const key = route.query ? `${route.method} ${route.path}?${new URLSearchParams(route.query)}` : `${route.method} ${route.path}`;
  const requested =
    req.headers["x-mock-scenario"] ?? url.searchParams.get("__scenario") ?? scenarioOverrides[key] ?? config.scenario;
  const scenarioName = route.scenarios[requested] ? requested : "default";
  const scenario = route.scenarios[scenarioName];
  if (!scenario) {
    log(req.method, url.pathname, 500, `route has no "${requested}" or "default" scenario`);
    return send(res, 500, { error: "MOCK_SCENARIO_MISSING", scenarios: Object.keys(route.scenarios) });
  }

  // Chaos: random 503 for resilience testing
  if (config.failRate > 0 && Math.random() < config.failRate) {
    await sleep(config.delay);
    log(req.method, url.pathname, 503, "chaos");
    return send(res, 503, { error: "MOCK_CHAOS_FAILURE" });
  }

  const ctx = {
    params,
    query: Object.fromEntries(url.searchParams),
    body: parseJsonSafe(rawBody),
    headers: req.headers,
  };

  let body = scenario.body;
  if (scenario.bodyFile) {
    const fixture = path.resolve(path.dirname(route.file), scenario.bodyFile);
    body = JSON.parse(fs.readFileSync(fixture, "utf8"));
  }
  body = render(body, ctx);
  const headers = render(scenario.headers ?? {}, ctx);
  const delay = (scenario.delayMs ?? 0) + config.delay;
  if (delay > 0) await sleep(delay);

  if (scenario.dropConnection) {
    log(req.method, url.pathname, 0, `${scenarioName}: connection dropped`);
    return req.socket.destroy();
  }

  const status = scenario.status ?? 200;
  log(req.method, url.pathname + url.search, status, scenarioName === "default" ? "" : `scenario=${scenarioName}`);
  return send(res, status, status === 204 ? undefined : body, headers);
});

server.listen(config.port, config.host, () => {
  const count = loadRoutes().length;
  console.log(`\nMock API running on http://${config.host}:${config.port}`);
  console.log(`  mocks:     ${config.dir} (${count} route${count === 1 ? "" : "s"}, hot reload on)`);
  console.log(`  scenario:  ${config.scenario}${config.delay ? `, +${config.delay}ms latency` : ""}${config.failRate ? `, fail rate ${config.failRate}` : ""}`);
  if (config.proxy) console.log(`  proxy:     ${config.proxy}${config.record ? " (recording)" : ""}`);
  console.log(`  admin:     GET /__mock/routes, POST /__mock/scenario, POST /__mock/reset`);
  console.log(`  emulator:  Android http://10.0.2.2:${config.port}, iOS simulator http://localhost:${config.port}\n`);
});

// ---------- arg parsing ----------
function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (!a.startsWith("--")) continue;
    const [k, inline] = a.slice(2).split("=");
    if (inline !== undefined) out[k] = inline;
    else if (argv[i + 1] && !argv[i + 1].startsWith("--")) out[k] = argv[++i];
    else out[k] = true;
  }
  return out;
}
