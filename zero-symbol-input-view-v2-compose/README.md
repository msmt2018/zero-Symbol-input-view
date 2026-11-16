# Zero Symbol Input View V2 — Compose

This module is a parallel Jetpack Compose implementation of the V2 symbol drawer. The original
View-based `zero-symbol-input-view-v2` module remains unchanged.

## Public UI entry point

```kotlin
val drawerState = rememberSymbolDrawerState()
SymbolDrawer(
    groups = groups,
    state = drawerState,
    settings = SymbolDrawerSettings(),
    onSymbolClick = { entry -> /* dispatch entry.shortAction */ },
    onSymbolLongClick = { entry -> /* dispatch entry.longAction */ }
)
```

## Compose architecture

- Material 3 `Surface`, typography, color scheme, shapes and tab components.
- `HorizontalPager` owns horizontal symbol-group paging.
- `LazyVerticalGrid` owns virtualized symbol cells.
- A separate vertical draggable handle owns expansion, so it cannot steal horizontal pager gestures.
- `SymbolDrawerState` is saveable and remembers the selected group by stable group ID.
- Existing typed action, JSON codec, legacy migration, repositories and settings models are copied as the initial domain baseline.
- Compose extension points use composable renderers and indicators; they do not accept Android Views.

## Responsive layout

The first implementation uses Compose constraints and adaptive grid primitives. The public layout API
will grow to expose window-size-class policies for compact, medium, expanded and large desktop-like
windows, including bottom/side docking. The grid must remain virtualized at all sizes.

## Migration status

The initial commit copies every file from the View V2 module as a traceable baseline. The Compose UI
entry point is implemented separately; remaining View-coupled managers, settings, lifecycle and
extension contracts are being replaced incrementally. Do not treat the copied View adapters as the
final Compose API.
