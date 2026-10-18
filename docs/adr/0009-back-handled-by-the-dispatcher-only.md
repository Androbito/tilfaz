# 0009. Back is handled by the back dispatcher only

- Status: Accepted
- Date: 2026-10-18
- Deciders: Ayoub

## Context

On the Android 12 TV the first Back press on Favourites, Continue watching and Watch list only dropped focus from the tile,
and the second one went back. Logs at the Activity showed why: Compose reads an unhandled Back as `FocusDirection.Exit`
("move focus out of this item"). Inside a focus group (the items of a lazy row) that succeeds, so Compose consumes the key
down, the Activity never starts tracking the press, and the key up does nothing.

Android 13+ is different: Back reaches the app through the system's `OnBackInvokedCallback`, which fires whether or not the
key was handled. A first fix that took the key and handed it to the dispatcher on every screen therefore popped two screens
on an Android 16 emulator.

## Options considered

1. **A key handler per screen that forwards Back to the dispatcher.** Pros: small. Cons: every new screen can forget it; doubles the back press on Android 13+ (proven on Android 16).
2. **`focusProperties { exit = ... }` on every focus group.** Pros: official hook. Cons: `Cancel` still consumes the key; properties do not inherit across focus targets, so it is needed on every group.
3. **One root `onKeyEvent` that takes only an unwanted Back, and forwards it to the dispatcher only before Android 13.** Pros: one place, bubbling order lets any element that wants Back keep it, correct on both paths. Cons: relies on Compose reading Back as Exit, which a Compose update could change (the tests would show it).

## Decision

We will use option 3: `Modifier.backKeyForDispatcherOnly()` once at the root in `MainActivity`, and
`android:enableOnBackInvokedCallback="true"` in the manifest. Screens with something to undo register a `BackHandler`
and nothing else; they never read the Back key.

## Consequences

- Positive: one press goes back one level everywhere, focus stays on the tile, on Android 12 and on 13+.
- Positive: no per-screen Back code left; `BackKeyTest` and the library screen test cover both Android paths (sdk 32 and 34).
- Negative: on Android 12 and older Back is forwarded from the key up, so a press held down does nothing until released.
- Follow-up: if Compose stops mapping Back to Exit, the focus-stays tests turn green without the handler and it can be removed.
