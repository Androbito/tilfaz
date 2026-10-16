# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub (owner)

## Context

Tilfaz is built largely by an AI agent following the `mobile-craft-skills` set, across many sessions. Decisions made in one
session are invisible to the next unless they are written down next to the code.

## Options considered

1. **ADRs in `docs/adr/`**: one short markdown file per decision. Pros: versioned with the code, cheap. Cons: needs discipline.
2. **Comments in code only**: Pros: zero ceremony. Cons: the "why" gets lost at the first refactor.
3. **Do nothing**: Pros: none. Cons: the same debates come back every month.

## Decision

We will record significant technical decisions as ADRs in `docs/adr/`, using the `adr-writer` skill's template.

## Consequences

- Positive: a new session (human or agent) understands the constraints in minutes.
- Negative: a few minutes of writing per decision.
- Follow-up: update the index table in this folder's README when adding or superseding an ADR.
