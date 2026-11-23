# 01 — Quick start

The Compose artifact exposes Composable UI and UI-neutral data/action APIs. Create repositories outside Composable bodies and pass immutable snapshots into screens.

## Symbol drawer

```kotlin
val state = rememberSymbolDrawerState()
SymbolDrawer(
    groups = groups,
    state = state,
    settings = settings,
    onSymbolClick = { entry -> editorActions.execute(entry) },
    onSymbolLongClick = { entry -> /* optional host action */ }
)
```

`SymbolDrawer` uses Compose Foundation paging and Material 3 layout. `rememberSymbolDrawerState` owns short-lived presentation state in the calling Composition. It does not retain a View, Context, Activity or LifecycleOwner.

## Manager and settings screens

- `SymbolDrawerManagerScreen(repository, onClose)` uses the UI-neutral `SymbolGroupManager` and repository.
- `SymbolDrawerSettingsScreen(settings, onSettingsChange, onReset)` emits immutable settings snapshots. The host persists them through `SymbolDrawerSettingsRepository`.

## Host responsibilities

The host owns editor integration, persistence instances, navigation and system Back dispatch. The library does not install lifecycle-bound Activity callbacks. Keep I/O and irreversible work out of Composable bodies.

See [architecture layers](../README.md) and [Drawer API](14-drawer-public-api-reference.md).