---
name: adr-writer
description: Write and maintain Architecture Decision Records (ADRs) in the repository: capture context, options considered, the decision and its consequences in a short markdown file. Use this skill whenever the user makes or discusses a technical choice worth remembering (library, architecture pattern, DI framework, navigation, persistence, KMP or not, min SDK, state management), asks "why did we choose X", wants to document a decision, mentions ADR, RFC or decision log, or when another skill leads to a significant architectural choice.
---

# ADR Writer

An ADR answers, months later, "why is it like this?" in two minutes of reading. One decision per file, short, stored next to the code.

## Workflow

1. **Locate the log.** Look for `docs/adr/`, `docs/decisions/`, `adr/`. If none exists, create `docs/adr/` with a `README.md` index and ADR `0001-record-architecture-decisions.md` (the decision to use ADRs).
2. **Number** the new record with the next 4-digit sequence and a kebab-case title: `0007-use-koin-for-dependency-injection.md`.
3. **Gather** from the conversation and code: the problem, constraints (team size, deadlines, platform versions, existing stack), options actually considered, and who decided. Ask the user only for what can't be inferred, typically the real reason behind the choice.
4. **Write** using `templates/adr-template.md`. Keep it under one page. Be honest about downsides.
5. **Update the index** in `docs/adr/README.md` (number, title, status, date).
6. **Superseding**: never edit the decision of an accepted ADR. Write a new ADR, set the old one's status to `Superseded by 00NN` and link both ways.

## Writing rules

- Context explains forces, not history lessons. Two to five sentences.
- List at least two real options, including "do nothing" when relevant.
- The decision is one clear sentence in active voice: "We will use Hilt for dependency injection."
- Consequences list positives AND negatives, plus any follow-up actions.
- Use today's date in ISO format. Status is one of: Proposed, Accepted, Deprecated, Superseded.
- No marketing language. Concrete numbers where they exist (build time, APK size, team familiarity).
