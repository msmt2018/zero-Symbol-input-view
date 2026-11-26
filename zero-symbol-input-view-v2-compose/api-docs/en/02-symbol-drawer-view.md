# 02 — SymbolDrawer Compose API

The Compose entry point is `SymbolDrawer`, not an Android View class.

```kotlin
@Composable
fun SymbolDrawer(
    groups: List<SymbolGroup>,
    modifier: Modifier = Modifier,
    state: SymbolDrawerState = rememberSymbolDrawerState(),
    settings: SymbolDrawerSettings = SymbolDrawerSettings(),
    config: DrawerConfig = settings.toDrawerConfig(),
    onSymbolClick: (SymbolEntry) -> Unit,
    onSymbolLongClick: ((SymbolEntry) -> Unit)? = null,
    onOpenManager: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
)
```

- `groups`: immutable domain snapshot.
- `state`: Composition-owned selected-page and drawer presentation state.
- `settings/config`: display, geometry and interaction policy.
- callbacks: host-owned actions and navigation.

Group paging uses Compose Foundation `HorizontalPager` with stable group IDs as keys. Expansion uses Compose drag/animation APIs and the shared drawer state engine. No RecyclerView, ViewPager2, AndroidView or custom ViewGroup is created.

`SymbolDrawerState` exposes `expand()`, `collapse()`, `toggle()` and `selectPage(index)`. Use `rememberSymbolDrawerState` inside the Composition that owns this transient presentation state.