---
name: tdd-craftsmanship
description: Drive development with Test-Driven Development (red, green, refactor), apply software craftsmanship practices, refactor safely in small steps, and get legacy code under test with characterization tests and seams. Use this skill whenever the user asks to implement a feature "with TDD", "test first", wants to refactor without breaking things, works on untested or legacy code, asks for a kata, mentions craftsmanship, clean code, Boy Scout rule, pair programming, or wants help making a messy function safe to change.
---

# TDD and Software Craftsmanship

Craftsmanship means steadily delivering well-crafted, well-tested software in small, safe steps. This skill turns that into a concrete working loop.

## Mode 1: New behaviour with TDD

Work in tiny cycles. Show each cycle to the user when pairing; batch them when working autonomously, but keep the order.

1. **List the behaviours** as a short test list (plain sentences) before writing code. Start with the simplest, degenerate cases (empty input, single item), end with the complex ones.
2. **Red.** Write one failing test for the next behaviour. Name it after the behaviour, not the method: `cancelling a shipped order fails with NotCancellable`. Run it and confirm it fails for the right reason.
3. **Green.** Write the minimum code to pass. Hard-coding is allowed at first; the next tests force generalisation (triangulation).
4. **Refactor.** With all tests green, remove duplication, improve names, extract functions. Run tests after each change. Refactor tests too.
5. Tick the behaviour off the list, add any new cases you discovered, repeat.

Rules of thumb:
- One assertion concept per test. Arrange / Act / Assert (or Given / When / Then) blocks separated visibly.
- Test through the public API of the unit. Don't test private functions directly.
- Use fakes for your own interfaces, mocks only to verify interactions that are the behaviour (an analytics event sent, a call not made).
- Control time, randomness, IDs and dispatchers by injecting them.
- If a test is hard to write, the design is telling you something: too many dependencies, hidden state, mixed responsibilities.

## Mode 2: Refactoring existing tested code

1. Make sure tests pass before touching anything.
2. Pick one named refactoring at a time (Extract Function, Rename, Introduce Parameter Object, Replace Conditional with Polymorphism, Move Function, Inline Variable, Split Phase).
3. Apply it, run tests, commit. Separate refactoring commits from behaviour commits.
4. Never refactor and change behaviour in the same step.

## Mode 3: Legacy code without tests

Follow Michael Feathers' approach: "legacy code is code without tests".

1. **Identify the change point** and what must not break around it.
2. **Find or create a seam**: a place where behaviour can be substituted without editing the code under test (constructor parameter, protocol/interface extraction, overridable method, function parameter with a default).
3. **Break dependencies minimally** to get the class into a test harness: Extract Interface, Parameterize Constructor, Extract and Override Call, wrap static calls.
4. **Write characterization tests**: call the code, assert on whatever it currently returns, even if it looks wrong. They document current behaviour; flag suspicious results to the user instead of "fixing" them silently.
5. **Make the change with TDD** now that there is a safety net.
6. **Leave it cleaner** (Boy Scout Rule), but stay within the scope the user asked for.

## Craftsmanship habits to apply and suggest

| Habit | How to apply it here |
| --- | --- |
| Small commits | Each green + refactor cycle can be a commit with a clear message |
| Small PRs | Suggest splitting anything over ~400 changed lines |
| Intention-revealing names | Rename before adding comments |
| Boy Scout Rule | Fix small things you touch, note bigger ones as follow-ups |
| Pairing | When the user is present, propose the next test and let them choose |
| Katas | Offer Gilded Rose, Bowling Game, Mars Rover or Tennis kata when the user wants practice |

## Platform test tooling

- Kotlin: JUnit 5 (or JUnit 4 on Android instrumented), kotlinx-coroutines-test (`runTest`, `StandardTestDispatcher`), Turbine for Flow, AssertK or Truth.
- Swift: Swift Testing (`@Test`, `#expect`) for new code, XCTest where the project already uses it; `async` tests natively supported.

## Output when working autonomously

Report the cycles briefly: the test list, which tests were added, and any characterization results that looked like existing bugs. Don't narrate every keystroke.
