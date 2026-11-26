# Zero Symbol Input View V2 — Compose API Documentation

This documentation targets `zero-symbol-input-view-v2-compose`. Its symbol drawer and generic Drawer UI are native Jetpack Compose APIs. The legacy `zero-symbol-input-view-v2` artifact is a separate Android View implementation.

## Documentation map

01 Quick start · 02 SymbolDrawer Compose API · 03 Models and actions · 04 Action runtime · 05 Data and management · 06 Settings · 07 Universal Compose Drawer · 08 Navigation/panels/style · 09 Adaptive window · 10 API index · 11 Interaction/built-ins · 12 Extension framework · 13 Public packages · 14 Drawer API · 15 Symbol extensions · 16 Testing/release.

## Canonical packages

- Compose symbol UI: `com.zerostudio.idea.widget.editor.symbolinput.v2`.
- Symbol models/data/settings: `com.zerostudio.idea.widget.editor.symbolinput.v2.*`.
- Generic Compose Drawer: `com.zerostudio.idea.widget.drawer.*`.
- Editor-neutral actions: `com.zerostudio.idea.widget.drawer.action`.

The former `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` namespace is no longer canonical.

## Dependency

```kotlin
dependencies {
    implementation(project(":zero-symbol-input-view-v2-compose"))
}
```

Use the Compose artifact's own published coordinates and a successfully published version. The View artifact's publication validation does not prove that the Compose artifact has been published.

## Architecture layers

1. Editor-neutral text/action contracts.
2. Domain models, validation and action execution.
3. Repositories, codecs and migration.
4. Drawer anchors, state, geometry and interaction policy.
5. Navigation, panels, adaptive layout and extensions.
6. Compose UI and screens.

Refactor bottom-up. Keep core/data/domain independent of UI rendering.

## Lifecycle boundary

The Compose API does not require Activity, Fragment, ViewModel or LifecycleOwner as its page-state contract, and does not wrap legacy controls with AndroidView. Compose still has Composition, recomposition, remember scopes and effect cancellation. “Zero lifecycle” does not mean absence of all lifecycle semantics.

## Artifact separation

The legacy artifact exposes View classes and View factories; this artifact exposes Composables and Composable slots. Their similarly named Drawer APIs are not interchangeable. Do not depend on both UI artifacts in the same app.
