# 16 — Testing, Release, Compatibility and Troubleshooting

## Validation layers

| Layer | What it proves |
|---|---|
| Kotlin/JVM unit tests | codecs, action parsing, anchor algorithms, data validation |
| Android build | Kotlin/Java compilation, resources, manifest and AAR output |
| View tests | adapter binding, measurement, state and recycling |
| Device/instrumentation tests | touch arbitration, nested scrolling, fling, animation, Insets and accessibility |
| Host integration tests | focused editor, undo stack, clipboard and Fragment lifecycle |

A passing JVM test does not prove real Android gesture or window behavior.

## Useful Gradle tasks

```bash
./gradlew :zero-symbol-input-view-v2:compileDebugKotlin
./gradlew :zero-symbol-input-view-v2:assembleDebug
./gradlew :zero-symbol-input-view-v2:assembleRelease
./gradlew :zero-symbol-input-view-v2:test
./gradlew :zero-symbol-input-view-v2:testDebugUnitTest
./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest
```

Task availability depends on the Android Gradle Plugin and module configuration. Check `./gradlew :zero-symbol-input-view-v2:tasks --all` before interpreting a missing task as a source failure.

## Required test coverage

- Valid/invalid action payloads, unsupported hosts, host false/exception, cancellation and partial Composite failure.
- UTF-16 offsets, surrogate pairs, combining characters, CRLF, reversed selection and end-exclusive ranges.
- Read-only documents, non-overlapping edits, right-to-left multi-edit and transaction fallback.
- Drawer horizontal/vertical gesture arbitration, fling, ACTION_CANCEL, nested scrolling and anchor changes.
- Renderer/indicator creation failure and built-in fallback.
- Fragment destruction, reattach, rotation, duplicate listeners and disposed page hosts.
- Compact/medium/expanded windows, split screen, Insets and folding posture.
- Corrupt JSON, failed persistence, migration already complete and non-empty V2 store.

## Public API and release checks

Review package names, public types, Java ABI, default methods, stable IDs, JSON schema, consumer rules, manifest and resources. Do not remove or change public constructors/methods without a compatibility plan. Kotlin typealiases are not Java-visible classes. Verify the AAR does not bundle an unintended editor-core dependency.

A release tag must point to the commit that passed the relevant CI. JitPack artifact publication is a separate check from ordinary Android CI; verify the POM, AAR and sources JAR independently.

## Common diagnostics

- Blank control: reload/submit groups; inspect repository contents and parent height.
- Action not applied: check listener, selected short/long action, focused-document host, thread and structured result.
- Extension missing: verify same registry instance, handle lifetime, stable ID and screen observer.
- Indicator absent: register before selecting its ID; inspect factory errors and retain built-in fallback.
- Gesture conflict: let horizontal pager and scrollable children consume their axes before drawer takeover.
- Imported JSON not visible: use the repository/manager commit path, not a temporary list.
- Wrong adaptive layout: use current window bounds and avoid double-subtracting Insets.

## Bug report contents

Include commit SHA, tag, Android version, window size/mode, minimal reproduction, expected/actual behavior, relevant action ID/payload (redacted), structured result, stack trace and CI job. Never include tokens or private user data.
