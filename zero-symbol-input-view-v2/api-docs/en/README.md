# Zero Symbol Input View V2 — API Documentation

English documentation for Android/Kotlin integrators and extension developers. These pages describe the public contract and known implementation boundaries of the current V2 source.

## Documentation map

| Page | Topic |
|---|---|
| [01 — Quick start](01-quick-start.md) | Gradle setup, view setup, first data and lifecycle |
| [02 — SymbolDrawerView](02-symbol-drawer-view.md) | Constructor, callbacks, data, paging and expansion |
| [03 — Models and actions](03-models-and-actions.md) | Groups, entries, typed actions and configuration |
| [04 — Action runtime](04-action-runtime.md) | Host, executor, results, cancellation and JSON |
| [05 — Data and management](05-data-and-management.md) | Repositories, manager operations, codecs and migration |
| [06 — Settings](06-settings.md) | Settings schema, persistence and runtime application |
| [07 — Universal Drawer](07-universal-drawer.md) | Container, controller, anchors and gestures |
| [08 — Navigation, panels and style](08-navigation-panels-style.md) | Page host, panel lifecycle, theme and presets |
| [09 — Adaptive window](09-adaptive-window.md) | Window size, folding, grid and side docking |
| [10 — API index and compatibility](10-api-index-compatibility.md) | Type index, compatibility rules and troubleshooting |
| [11 — Interaction and built-ins](11-interaction-and-builtins.md) | Gesture arbitration and generic text commands |
| [12 — Extension framework](12-extension-framework.md) | Actions, renderers, indicators, settings and themes |
| [13 — Public packages and document contract](13-public-package-and-editor-neutral-actions.md) | Public namespace, UTF-16 offsets and editor-neutral editing |
| [14 — Drawer public API reference](14-drawer-public-api-reference.md) | Generic Drawer API reference |
| [15 — Symbol control extension guide](15-symbol-control-extension-api.md) | Practical extension recipes and lifecycle |
| [16 — Testing, release and troubleshooting](16-testing-release-troubleshooting.md) | Test layers, release checks and diagnostics |
| [17 — Source audit notes](17-source-audit-notes.md) | Confirmed API boundaries and remaining validation |

## Packages

The symbol-input product API remains under `com.zerostudio.idea.widget.editor.symbolinput.v2`. The reusable Drawer Kotlin facade is under `com.zerostudio.idea.widget.drawer` and subpackages `core`, `navigation`, `panel`, `style`, `adapter`, `extension`, and `action`.

**Java ABI warning:** some public facade entries are Kotlin `typealias` declarations. Kotlin source can import them; a typealias does not create a JVM class and is not directly importable from Java. See [13](13-public-package-and-editor-neutral-actions.md).

## Dependency

Local module:
```kotlin
dependencies { implementation(project(":zero-symbol-input-view-v2")) }
```

JitPack (use a tag that has actually been published successfully):
```kotlin
repositories { maven("https://jitpack.io") }
dependencies {
    implementation("com.github.msmt2018:zero-Symbol-input-view:4.1-v2")
}
```

## Choose an entry point

- Embed the symbol toolbar: `SymbolDrawerView`.
- Connect an editor: `SymbolActionHost`; implement `SymbolTextDocument` for generic text commands.
- Persist groups: `SymbolGroupRepository` / `SymbolGroupManager`.
- Add extensions: `drawer.extensions`.
- Build a reusable drawer: `DrawerContainer` + `DrawerConfig`.
- Support large/foldable windows: `WindowAdaptiveInfo` and adaptive resolvers.

## Global contracts

1. Use stable IDs, never list indices, for persisted identity.
2. Treat submitted and returned collections as snapshots.
3. UI and editor callbacks run synchronously on the caller's thread; the library does not switch to the main thread for the host.
4. Distinguish invalid input, unsupported capability, failure, cancellation and partial completion.
5. Close every `AutoCloseable` registration/observer with its owning View or editor session.
6. Keep one owner for each source of truth: repository data, pager selection, drawer state and host editor state.
7. Unit tests do not replace device tests for gestures, Insets, nested scrolling, accessibility or real editor integration.

See the corresponding Chinese pages for the original detailed examples. This English set is a companion reference and must be kept synchronized with source changes.
