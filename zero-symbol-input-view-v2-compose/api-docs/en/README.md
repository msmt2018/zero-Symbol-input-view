# Zero Symbol Input View V2 — Compose API docs

This documentation targets `zero-symbol-input-view-v2-compose`. UI entry points and extension slots are Jetpack Compose APIs. The legacy `zero-symbol-input-view-v2` module remains a separate View-based compatibility artifact and is not part of the Compose module.

## Documentation map

01 Quick start · 02 SymbolDrawer Compose API · 03 Models and actions · 04 Action runtime · 05 Data and management · 06 Settings · 07 Universal drawer · 08 Navigation and panels · 09 Adaptive window · 10 API index and compatibility · 11 Interaction and built-ins · 12 Extension framework · 13 Public packages and editor-neutral actions · 14 Drawer public API · 15 Extension examples · 16 Testing and troubleshooting.

## Architecture layers

1. Editor-neutral text/action contracts.
2. Domain models, validation and action runtime.
3. Repository, codecs and migration.
4. Drawer state, geometry, anchors and interaction policy.
5. Navigation, panels, adaptive layout and extensions.
6. Compose UI and screens.

Refactor bottom-up. Do not make domain and core APIs Composable merely to support a UI implementation.

## Lifecycle boundary

Compose pages do not depend on Activity, Fragment, ViewModel or LifecycleOwner as their page-state model, and do not wrap legacy controls with AndroidView. Compose still has Composition, recomposition, remember scopes and effect cancellation; “zero lifecycle” must not be interpreted as absence of all runtime lifecycle semantics.

## Legacy module

The old `zero-symbol-input-view-v2` artifact retains its View APIs and compatibility tests. Do not copy its RecyclerView, ViewPager2, Fragment or View integration examples into the Compose module.