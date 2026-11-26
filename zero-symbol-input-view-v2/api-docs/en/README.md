# Zero Symbol Input View V2 — API Documentation

English documentation for Android/Kotlin integrators and extension developers. This guide targets the `zero-symbol-input-view-v2` Android View artifact.

## Documentation map

| Page | Topic |
|---|---|
| 01 Quick start | Gradle, XML/View setup and integration |
| 02 SymbolDrawerView | Constructor, callbacks, data, paging and View state |
| 03 Models and actions | Groups, entries and typed actions |
| 04 Action runtime | Host, executor, results and cancellation |
| 05 Data and management | Repository, manager, codecs and migration |
| 06 Settings | Settings schema and persistence |
| 07 Universal Drawer | Android View Drawer container and controller |
| 08 Navigation, panels and style | View page host, panels and style |
| 09 Adaptive window | Window snapshots and adaptive policies |
| 10 API index | Public API packages and compatibility |
| 11 Interaction and built-ins | Gesture policy and text actions |
| 12 Extension framework | Runtime extension registry |
| 13 Public packages | Public namespace and document contract |
| 14 Drawer API reference | View Drawer API |
| 15 Symbol control extensions | Extension examples |
| 16 Testing and release | Test matrix and diagnostics |
| 17 Source audit notes | Package and implementation boundaries |

## Canonical packages

- Symbol toolbar: `com.zerostudio.idea.widget.editor.symbolinput.v2`.
- Symbol models: `com.zerostudio.idea.widget.editor.symbolinput.v2.model`.
- Generic Drawer: `com.zerostudio.idea.widget.drawer` and its subpackages.
- Editor-neutral actions: `com.zerostudio.idea.widget.drawer.action`.

The former `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` package is no longer canonical. New imports must use `com.zerostudio.idea.widget.drawer.*` for generic Drawer APIs.

## Dependency

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2"))
}
```

For remote use, select a View artifact Git tag that has actually been published successfully. Do not infer Compose publication from the legacy View artifact's Maven validation.

## Choose an entry point

- Symbol toolbar: `SymbolDrawerView`.
- Generic View drawer: `DrawerContainer`.
- Data: `SymbolGroupRepository` / `SymbolGroupManager`.
- Editor integration: `SymbolActionHost`; optionally `SymbolTextDocument`.
- Generic Drawer config: `drawer.config.DrawerConfig`.
- Symbol grid config: `model.DrawerConfig`.

## Artifact distinction

The Compose implementation is a separate artifact. It exposes Composable APIs under the same generic Drawer concepts, but `DrawerContainer`, `DrawerPanelContent` and `DrawerPageHost` have different type shapes. Do not add both UI artifacts to one app dependency graph.
