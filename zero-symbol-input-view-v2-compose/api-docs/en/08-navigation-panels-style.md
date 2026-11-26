# 08 — Navigation, panels and style

## DrawerPage

`DrawerPage` is an immutable page descriptor with a stable, unique ID, a non-empty title, `DrawerPanelContent`, and an optional Composable icon. Use the ID—not the current list index—as page identity. Its content and icon are Composable slots rendered by the Compose page host.

## DrawerNavigationController

The controller owns page identity and selection. It does not hold Compose Runtime snapshot state, a View, an adapter, or a lifecycle object. Its immutable state/snapshot accessors expose navigation state, and listeners receive synchronous notifications.

Operations include:

- `submitPages(value, preserveSelection)`: validate unique IDs and replace the page set while optionally preserving the selected ID.
- `selectPage(id)` / `selectPage(index)`: select an existing page; return `false` for an unknown ID or invalid index.
- `pages()`, `selectedPage()`, `selectedIndex()`, `state`, and `snapshot()`: read page and selection snapshots.
- `addListener(listener)` / `removeListener(listener)`: manage observers. A newly added listener immediately receives the current snapshot.

**Threading contract:** `DrawerNavigationController` is single-thread-confined and is not thread-safe. Callers must serialize every operation—including reads, listener registration, and listener removal—on one thread. Change notifications run synchronously on the thread that publishes the change; the initial callback from `addListener` runs on the registering thread. Compose hosts must invoke controller operations on their UI/composition dispatcher so callbacks can safely update Compose state. The controller does not switch threads or retain a dispatcher.

Reentrant notifications are queued so every observer receives the current snapshot before a reentrant, newer snapshot. Listener failures are reported to the caller after the queued notifications have been delivered.

The controller itself has no Compose Runtime dependency. `rememberDrawerNavigationSnapshot(controller)` is the Composition adapter; it subscribes to the controller and removes its listener when its `DisposableEffect` leaves Composition. The controller does not own Compose snapshot state. However, `DrawerPage` descriptors contain Composable content slots, so these page types remain part of the Compose module API rather than a fully independent Kotlin/JVM API.

## DrawerPageHost

`DrawerPageHost` is a Composable page host backed by Foundation `HorizontalPager` and `rememberPagerState`. It keys pages by ID, synchronizes the pager with the selected ID, reports settled user navigation, and renders an empty container for an empty page set. It has no View factory, View cache, attach/detach callback, or manual disposal API.

The host owns application navigation, system Back handling, window insets, and editor layout. `DrawerPageHost` does not own the application navigation stack.

## DrawerPanelContent and DrawerPanelHost

`DrawerPanelContent` is a Composable `ColumnScope` content slot. `drawerPanelContent { ... }` is its convenience factory. `DrawerPanelHost` composes the slot in the current Composition; panel state and effects follow Compose Composition semantics.

## Style model

The `drawer.style` package provides immutable `DrawerStyle`, `DrawerColors`, `DrawerDimensions`, `DrawerTypography`, `DrawerMotion`, `DrawerShape`, and `DrawerThemeResolver` contracts. They describe appearance and motion without owning an Activity Context.

## Integration boundary

- `DrawerContainer` owns the Compose container, layout, gestures, and content slots.
- `DrawerNavigationController` owns page identity and selection.
- `DrawerPageHost` renders the Compose pager.
- The host application owns navigation, system Back, window insets, and editor layout.
- Do not wrap the legacy implementation with `AndroidView`, `AndroidViewBinding`, `FrameLayout`, `RecyclerView`, or `ViewPager2`.

Compose has Composition, recomposition, and effect start/stop semantics. This design removes traditional View/Fragment page lifecycle callbacks; it does not claim that Compose itself has no lifecycle.
