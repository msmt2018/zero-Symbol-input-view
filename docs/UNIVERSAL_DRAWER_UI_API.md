# Universal Drawer UI API

## Goal

Universal Drawer UI API is a reusable Android bottom-drawer framework. The drawer core must not depend on symbol models, symbol repositories, editor actions, Logcat, or any concrete feature. The current V2 symbol drawer is the compatibility reference and will be migrated onto these public contracts incrementally.

## Architectural boundaries

1. **Core** owns drawer state, measurement inputs, anchor resolution, drag/settle behavior, nested-scroll coordination, animation, and lifecycle.
2. **Panel** owns replaceable content slots. Peek and Expanded are independent content surfaces; either may be used alone.
3. **Navigation** owns generic page identity, selection, snapshots, and observer notifications. It has no dependency on SymbolGroup.
4. **Style** will own host-theme inheritance, dimensions, typography, shapes, colors, motion, and presets.
5. **Feature** modules own domain models and behavior. The Symbol feature is only one consumer of the framework.

Dependency direction is one-way: Feature -> public Drawer API. Core/Panel/Navigation/Style must never import Feature packages.

## Four independent anchor strategies

The four anchor strategies are alternatives selected through the same DrawerAnchorStrategy contract. They are not four stops in one combined layout.

| Strategy | Contract | Purpose |
|---|---|---|
| ExistingHeight | ExistingHeightAnchorStrategy | Compatibility with the current V2 collapsed/expanded height behavior. This is the initial default during migration. |
| ThreeStage | ThreeStageAnchorStrategy | Independent Peek, Half-Expanded, Expanded anchors. |
| ContentBased | ContentBasedAnchorStrategy | Peek plus a content-measured height bounded by the viewport and configured maximum. |
| Custom | CustomAnchorStrategy | Application-provided anchor IDs, semantic states, and pixel heights. |

All strategies resolve through DrawerAnchorResolver, which clamps heights, sorts them, removes duplicate pixel positions, and provides nearest-anchor and one-stage fling targeting. The container will provide measured viewport/content sizes and convert dp-based public configuration into pixels.

The three-stage strategy defaults to a half point at 50% of the viewport and an expanded point at 92%. If the available height makes two or more anchors coincide, the resolver collapses duplicates instead of creating zero-distance states.

## Public API foundation in this phase

- drawer.core: anchor models, four strategies, normalization, drawer state, and controller contract.
- drawer.panel: lifecycle-aware content contract and replaceable DrawerPanelHost.
- drawer.navigation: generic DrawerPage and observable DrawerNavigationController.

The controller and panel contracts are intentionally independent of the current AdvancedSymbolDrawerView. Integration will happen after the core container and gesture engine have explicit ownership of measurement and touch dispatch.

## Panel composition

- Peek Panel: compact/always-available content shown in the collapsed state.
- Expanded Panel: independent expanded content. It may host a single custom View or a page host.
- Either panel may be omitted.
- A panel content provider owns its own view construction and receives attach/visible/hidden/detach callbacks.
- Panel hosts do not know about symbols, repositories, or actions.

## Navigation

DrawerNavigationController is the source of truth for page identity and selection. A navigation widget (TabLayout, RecyclerView, or custom View) is an optional observer/adapter. Pages can be Logcat tabs, terminal tabs, symbol groups, or any other application content.

## Migration invariants

- Preserve the current symbol drawer's initial visual layout, dimensions, gesture direction arbitration, action dispatch, group selection, and V1/V2 data compatibility.
- Do not modify V1 source files.
- Do not switch the existing Symbol feature to a new anchor strategy until parity tests cover the existing-height strategy.
- Avoid duplicating the current ViewPager2 and gesture implementation while the new core is being extracted; migration should have one owner for each interaction at every stage.
- Public API types should be stable and domain-neutral. Internal implementation classes may change without requiring application changes.

## Planned implementation sequence

1. Public contracts and pure anchor resolver.
2. Generic DrawerContainer and state/controller binding.
3. Independent Peek/Expanded panel composition.
4. Generic navigation bar/indicator/page host.
5. Gesture physics and nested-scroll coordination, preserving horizontal child gestures.
6. Host theme/style system and Standard, Compact, FullScreen, SplitPanel presets.
7. Migrate Symbol drawer to a feature module/adapter without changing visible behavior.
8. API documentation, lifecycle tests, Android instrumentation tests, and performance validation.


## Style, theme and configuration API

The style API is feature-neutral and uses immutable value objects:

- `DrawerColors`: optional explicit surface, primary/secondary text, accent, outline and scrim colors. A null value inherits the host theme.
- `DrawerColorAttributes`: lets an application map its own theme attributes into the drawer color roles.
- `DrawerDimensions`: peek/maximum heights, corner radius, paddings, spacing and drag-handle size in dp.
- `DrawerTypography`: title/body/caption sizes in sp, typeface styles and optional font family.
- `DrawerShape`: independent corner radii and outline width.
- `DrawerMotion`: settle duration, drag resistance, fling velocity thresholds and spring enablement.
- `DrawerStyle`: combines all tokens, theme mode, elevation and clipping/scrim flags.

`DrawerThemeResolver.resolve(context, style)` resolves color attributes at runtime. `FOLLOW_HOST` uses the supplied Activity/Context theme, so the drawer inherits host theme values rather than maintaining a parallel light/dark state. Explicit LIGHT and DARK modes use platform Material theme wrappers. Explicit color values override inherited values individually; unspecified roles continue to inherit.

`DrawerConfig.Builder` provides both grouped setters and granular setters, including `setAnchorStrategy`, `setPeekContent`, `setExpandedContent`, `setThemeMode`, `setFollowHostTheme`, `setColors`, `setSurfaceColor`, `setAccentColor`, `setDimensions`, `setPeekHeightDp`, `setExpandedMaxHeightDp`, `setCornerRadiusDp`, `setCornerRadiiDp`, `setTypography`, `setTitleTextSizeSp`, `setMotion`, `setDragResistance`, `setSettleDurationMs`, `setDragEnabled`, `setNestedScrollEnabled`, and dismissal/accessibility setters.

## Baseline layout presets

`DrawerPresets.config(...)` supplies overridable baselines:

- STANDARD: compatibility height behavior and default style.
- COMPACT: content-sized, reduced padding and compact maximum height.
- FULL_SCREEN: three-stage anchors with a full-viewport expanded stage.
- SPLIT_PANEL: three-stage layout with additional spacing for independent panel composition.

Presets are starting configurations, not locked templates. Build a preset config, copy its style into a new builder, and override only the desired properties.
