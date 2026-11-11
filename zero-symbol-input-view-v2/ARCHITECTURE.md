# Symbol Drawer V2 Architecture

## Product contract

This library implements one integrated symbol drawer:

- vertical drag expands/collapses the symbol surface;
- the group indicator is progressively revealed by drawer expansion;
- horizontal swipe changes the active group;
- tapping a group indicator changes the same pager;
- symbol click and long-click are dispatched through one callback contract;
- group identity and selected page survive data refreshes when IDs remain stable.

The legacy library remains untouched and is the behavioural reference during V2 development.

## Runtime composition

```text
AdvancedSymbolDrawerView
├── SymbolGroupIndicator (horizontal RecyclerView)
└── ViewPager2 (horizontal paging)
    └── SymbolPageAdapter
        └── RecyclerView + GridLayoutManager per page
            └── SymbolAdapter (DiffUtil)
```

## Responsibilities

- **AdvancedSymbolDrawerView**: owns the integrated interaction, height, indicator reveal, pager synchronization and public API. Its expansion geometry includes indicator height separately from grid height, so the indicator remains revealable even when every symbol group fits in the collapsed grid.
- **DrawerGeometry**: pure row/height/reveal calculations; no View dependencies.
- **SymbolGroupRepository**: persistence boundary. The default SharedPreferences implementation uses a V2-only namespace.
- **LegacySymbolDataMigration**: one-way imports the legacy compact JSON preference when the V2 store is empty; it never writes to the legacy store.
- **SymbolGroupManager**: repository-backed group/symbol commands that a dedicated manager UI can consume.
- **SymbolGroupIndicator**: recycles group labels and only updates changed selection rows.
- **SymbolPageAdapter**: pages are recycled by ViewPager2; each page uses a grid RecyclerView and shared recycled pool.
- **Action configuration**: every symbol exposes independent V1-compatible short-press and optional long-press action selectors, using the exact V1 action IDs, ordering and text-parameter rules. The manager list is also embedded in the settings screen.
- **Settings repository and screen**: immutable schema-versioned settings snapshots persisted independently from symbol data; the programmatic settings screen exposes each setting and reports saved snapshots to the host. `AdvancedSymbolDrawerView.applySettings` applies grid geometry, expansion cap, indicator style, labels, touch targets, long-press timing, haptics, motion, cache and fling behavior.
- **Bundle codec**: import/export and SharedPreferences writes use the original V1 compact JSON schema (`n`, `d`, `a`, `b`, `c`, `d`, `e`). Runtime-only IDs are synthesized because V1 did not store IDs; raw action IDs and payloads are retained to keep round trips lossless.

## Gesture arbitration

The drawer intercepts only predominantly vertical movement after touch slop. Horizontal movement remains with ViewPager2. This is intentionally not implemented as a competing horizontal pager. The drag controller tracks velocity and snaps to the nearest endpoint, with a fling threshold for deliberate fast movement.

The V2 interaction contract must continue to be validated against the legacy control:
1. a vertical upward drag expands the drawer;
2. a vertical downward drag collapses it;
3. the group indicator appears continuously during expansion and disappears continuously during collapse;
4. a horizontal swipe changes groups without changing drawer height;
5. tapping a group indicator changes the same page;
6. short and long symbol gestures remain distinct;
7. updating groups preserves the selected group by stable group ID.

## Performance strategy

- RecyclerView-backed indicator instead of creating every indicator child manually.
- ViewPager2 page recycling instead of recreating all pages on every data update.
- DiffUtil for symbol cells.
- Shared RecycledViewPool for page grids.
- No page item animator during rapid symbol data changes.
- Immutable snapshots at the UI boundary.
- Height math is calculated in a pure object and only updates layout params when values actually change.

## Compatibility and migration

- Package: `com.zerostudio.idea.widget.editor.symbolinput.v2`.
- Persistence: `symbol_drawer_v2/groups_json_v2`; legacy keys are never written.
- Editor integration is callback-based. A host can bridge the callback to the existing `SymbolActionExecutor`.
- This is a parallel implementation. Do not remove or redirect the legacy widget until behavioural tests and an app-level comparison pass.

## Deliberate non-goals for this foundation

- The V1 compact data classes are retained in `model.legacy`; migration and all new persistence writes use the same original JSON schema. Action IDs and payloads are preserved verbatim.
- The settings screen embeds the symbol manager. Each symbol row opens the V1-style editor for its own short/long action configuration.
