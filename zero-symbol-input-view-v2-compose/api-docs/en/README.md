# Zero Symbol Input View V2 — Compose API Documentation

This directory documents only `zero-symbol-input-view-v2-compose`. Production Kotlin source is authoritative for signatures, visibility, default arguments and exception behavior.

## Documentation map

| Documents | Scope |
|---|---|
| 01–06 | Quick start, symbol drawer, models/actions, action runtime, data management and settings |
| 07–09 | Generic Compose Drawer, navigation/panels/style and adaptive window |
| 10–13 | API index, interactions/built-ins, extension framework and editor-neutral contracts |
| 14–16 | Drawer reference, symbol extension guide and testing/release/troubleshooting |
| [17 Complete KDoc API reference](17-kdoc-api-reference.md) | Public API map, parameter contracts, state/thread/error and compatibility boundaries |

## Canonical packages

- Symbol UI: `com.zerostudio.idea.widget.editor.symbolinput.v2`
- Symbol models/actions/data/settings: `com.zerostudio.idea.widget.editor.symbolinput.v2.*`
- Generic Compose Drawer: `com.zerostudio.idea.widget.drawer.*`
- Editor-neutral actions: `com.zerostudio.idea.widget.drawer.action`

The generic Drawer source is physically located in `com.zerostudio.idea.widget.drawer.*`; the former symbol-input `.drawer` namespace is not canonical.

## Dependency

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

External Maven consumers must use the Compose artifact's actual published coordinates and a successfully published version. Do not reuse the View artifact coordinates.

## API selection

- Symbol input: `SymbolDrawer` + `rememberSymbolDrawerState`
- Generic drawer: `DrawerContainer` + `rememberDrawerContainerState`
- Pages/panels: `DrawerPage`, `DrawerPageHost`, `DrawerPanelContent`
- State/anchors: `DrawerStateHolder`, `DrawerController`, `DrawerAnchorStrategy`
- Data: `SymbolGroupRepository`, `SymbolGroupManager`
- Settings: `SymbolDrawerSettingsRepository`, `SymbolDrawerSettingsScreen`
- Editor integration: `SymbolActionHost`, `SymbolTextDocument`

## KDoc / Dokka

Use standard KDoc for public Kotlin declarations. Document `@param`, `@return`, `@throws` and `@see` where applicable. Distinguish internal implementation, Composable slots, Kotlin typealiases and persistence contracts. A typealias is not a standalone Java class; a Composable slot is not a View factory.

## Composition boundary

The module does not require Activity, Fragment, ViewModel or LifecycleOwner as its UI state contract. It creates no traditional Android Views and does not wrap legacy controls with AndroidView. Compose still has Composition, recomposition, remember and effect-cancellation semantics.

## Artifact separation

The legacy View artifact exposes Android View classes/factories; this module exposes Composables/Composable slots. They are not interchangeable and should not both be included as an application's Drawer UI dependency.
