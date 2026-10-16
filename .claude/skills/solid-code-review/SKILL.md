---
name: solid-code-review
description: Review code against SOLID, KISS, YAGNI, DRY, Law of Demeter, composition over inheritance and clean code rules, and propose concrete refactorings. Use this skill whenever the user asks for a code review, a PR review, "is this good code", "how can I improve this class", wants feedback on design or readability, pastes a class and asks what's wrong with it, or mentions SOLID, code smells, technical debt or refactoring candidates. Works for Kotlin, Swift and any mainstream language.
---

# SOLID Code Review

Give a review a senior engineer would give: specific, prioritised, and balanced. The point is better code, not a list of every principle that could be quoted. Over-engineering is a defect too, so weigh SOLID against KISS and YAGNI.

## Workflow

1. **Understand intent first.** Read the code and its callers/tests if available. Identify what the unit is supposed to do in one sentence. If you can't, that is finding number one.
2. **Scan for smells** using the table below. Note file and line for each.
3. **Classify** each finding by severity:
   - **Blocker**: bugs, data loss, security issues, thread-safety problems, broken contracts (Liskov violations that will crash or misbehave).
   - **Major**: design problems that will make the next change painful (SRP/DIP violations in core code, hidden coupling, untestable code).
   - **Minor**: readability, naming, small duplication.
   - **Nit**: style preferences. Only mention if few other findings.
4. **Propose the fix**, not just the problem. Show a short before/after snippet for each Blocker and Major.
5. **Check for over-engineering** in the opposite direction: interfaces with one implementation and no test seam need, factories for one product, generic abstractions used once, layers that only forward calls.
6. **Close with what is good.** Name one or two things done well so the author knows what to keep doing.

## Smell to principle map

| Smell | Principle | Typical fix |
| --- | --- | --- |
| Class name with "Manager", "Helper", "Util", "Handler" doing several things | SRP | Split by reason to change |
| File over ~300 lines or function over ~30 lines | SRP, Clean Code | Extract class/function |
| `when`/`switch` on a type that grows with each new variant | OCP | Polymorphism, strategy, sealed hierarchy handled in one place |
| Subclass overriding a method to throw or do nothing | LSP | Composition, narrower interface |
| Fake in tests needs `TODO()` / `fatalError()` for most methods | ISP | Split the interface |
| `SomeClass()` constructed inside business logic, static singletons | DIP | Inject via constructor |
| `a.b.c.d.doSomething()` | Law of Demeter | Tell, don't ask; add a method on `a` |
| Deep inheritance (`BaseViewModel` -> `BaseListViewModel` -> ...) | Composition over inheritance | Delegate to small collaborators |
| Same knowledge (rule, constant, mapping) in several places | DRY | Single source of truth |
| Similar-looking code that changes for different reasons | Wrong DRY | Keep duplicated, it's fine |
| Boolean parameters (`load(true, false)`) | Clean Code | Named params, enums, separate functions |
| Comments explaining what the code does | Clean Code | Better names; comment the why |
| Magic numbers and strings | Clean Code | Named constants, enums |
| Nullable/optional returned for errors | Clean Code | `Result`, sealed errors, typed throws |
| Abstraction with one implementation and no tests | YAGNI | Inline it until a second need appears |

## Mobile-specific checks

- Business logic inside Composables, SwiftUI `body`, Activities, Fragments or ViewControllers.
- ViewModel holding a `Context`, `View`, `UIViewController` reference (leak and SRP).
- Blocking calls on the main thread; `GlobalScope`, unstructured `Task {}` without cancellation.
- Mutable state exposed publicly from a ViewModel.
- DTOs or database entities leaking to UI.

## Output format

```
## Summary
<one paragraph: what the code does, overall verdict>

## Blockers
1. <file:line> <problem>. <why it matters>.
   <before/after snippet>

## Major
...

## Minor
...

## Over-engineering check
<findings or "None">

## What's good
<1-2 items>
```

Keep the tone factual and collegial. Explain the "why" briefly; most developers know the acronyms but not always the consequence.
