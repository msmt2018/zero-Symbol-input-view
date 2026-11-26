# Symbol Drawer V2 — Android View Architecture

## Product contract

This artifact implements the Android View symbol toolbar:

- Vertical drag expands/collapses the symbol surface.
- The group indicator is progressively revealed by drawer expansion.
- Horizontal swipe changes the active group.
- Tapping a group indicator selects the corresponding page.
- Short-click and long-click actions are dispatched separately.
- Stable group IDs preserve selection across data refreshes.

## Runtime composition

```text
SymbolDrawerView
├── SymbolGroupIndicator
└── ViewPager2
    └── SymbolPageAdapter
        └── RecyclerView grid pages
```

## Responsibilities

- `SymbolDrawerView`: owns symbol toolbar interaction, height, indicator reveal, pager synchronization and public callbacks.
- `DrawerGeometry`: pure row/height/reveal calculations.
- `SymbolGroupRepository`: persistence boundary; default storage uses a V2-only namespace.
- `LegacySymbolDataMigration`: one-way legacy JSON import when the V2 store is empty; it never writes back to legacy preferences.
- `SymbolGroupManager`: repository-backed group/symbol commands and immutable snapshots.
- `SymbolGroupIndicator` / `SymbolPageAdapter`: View-based indicator and page rendering.
- Settings and codecs: schema-versioned settings, typed action envelope, legacy JSON decoding and backup bundle import/export.

## Generic Drawer package layout

The reusable generic Drawer is a separate framework from the symbol toolbar and now has canonical source packages:

- `com.zerostudio.idea.widget.drawer`: `DrawerContainer`.
- `com.zerostudio.idea.widget.drawer.core`: anchors, resolver, controller, state and interaction controller.
- `com.zerostudio.idea.widget.drawer.config`: generic Drawer config and presentation mode.
- `com.zerostudio.idea.widget.drawer.navigation`: pages, navigation controller and View page host.
- `com.zerostudio.idea.widget.drawer.panel`: View panel content and panel host.
- `com.zerostudio.idea.widget.drawer.style`: colors, dimensions, typography, motion, shape and theme.
- `com.zerostudio.idea.widget.drawer.adapter`: symbol drawer anchor adapter.

The previous `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` package is no longer canonical. The symbol product remains in `com.zerostudio.idea.widget.editor.symbolinput.v2`; its model-level `DrawerConfig` is not the same type as `drawer.config.DrawerConfig`.

## Gesture arbitration

The symbol toolbar claims predominantly vertical movement after touch slop; horizontal movement remains with ViewPager2. Nested child scrolling is considered before the drawer takes over downward movement. The generic Drawer has its own controller/anchor model and should not be layered around the symbol toolbar.

## Performance strategy

- ViewPager2 page recycling.
- RecyclerView-backed symbol grids and group indicator.
- Diff-based symbol data updates and shared recycled pools where configured.
- Immutable snapshots at data/UI boundaries.
- Pure geometry calculations separated from View measurement and layout.
- Avoid repeated full adapter replacement for unrelated settings changes.

## Editor integration

The action runtime is editor-neutral. The host implements `SymbolActionHost` and optionally `SymbolTextDocument`; Sora-specific or other editor adapters belong to the host application or a separate adapter module.

## Compatibility and persistence

- Symbol product package: `com.zerostudio.idea.widget.editor.symbolinput.v2`.
- Generic Drawer package: `com.zerostudio.idea.widget.drawer.*`.
- V2 data is stored separately from V1. Legacy data import is one-way.
- Stable group/symbol IDs, typed short/long actions and legacy metadata are preserved by codecs.
- The View artifact and Compose artifact are independent UI implementations. Do not depend on both in the same application.
