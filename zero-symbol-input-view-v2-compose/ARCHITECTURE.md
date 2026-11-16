# Zero Symbol Drawer V2 — Compose Architecture

## Product intent

This module is an independent Jetpack Compose implementation of the symbol drawer. It is not a wrapper or port that embeds the Android View library. The sibling `zero-symbol-input-view-v2` module is a separate, immutable product and is not a dependency.

## Behavioral contract

- Vertical drag expands/collapses the drawer with deterministic velocity settling.
- The group navigation indicator is progressively revealed with expansion.
- Horizontal paging changes the active symbol group without accidentally changing drawer height.
- Selecting a group indicator selects the corresponding page.
- Short and long symbol gestures dispatch distinct typed actions.
- Stable group identity preserves selection across data refreshes.
- Manager/settings operations update durable data atomically and expose failures to the UI.
- State restoration, accessibility, focus, keyboard input, reduced motion and touch target requirements are part of the component contract.

## Target runtime architecture

```
Compose public API and composable slots
  └─ Compose presentation (drawer, indicator, pager, grid, manager, settings)
       └─ UI state + use cases + Compose extension registry
            └─ platform-neutral models, actions, codecs, policies, repository contracts
                 └─ Android storage and SAF adapters (no View UI)
```

## Implementation boundaries

- All UI uses Compose UI, Foundation, Material 3 and Animation APIs.
- No Android View widgets, ViewGroup, ViewPager2, RecyclerView, View-based Material, AndroidView or AndroidViewBinding.
- Public customization uses composable slots/renderers, immutable state and explicit event callbacks.
- Platform services such as Context, ContentResolver and Activity Result may be used only in non-UI adapters or Compose host routes.
- Shared behavior is limited to UI-independent models, JSON compatibility, action protocols, validation and pure algorithms.
- The artifact must build and run without the sibling View library.

## Data compatibility

The versioned V2 typed-action JSON envelope and legacy V1 compact JSON import remain compatibility contracts. Stable IDs, icon metadata, short/long actions and legacy payload metadata must survive decode/encode. Settings schema versions, enum identifiers and defaults are also compatibility contracts.

## Migration status

See [COMPOSE_REFACTOR_PLAN.md](COMPOSE_REFACTOR_PLAN.md) for the audited source inventory, ordered migration phases and release gates. The copied View baseline is transitional technical debt and must be removed before the Compose artifact is considered complete.
