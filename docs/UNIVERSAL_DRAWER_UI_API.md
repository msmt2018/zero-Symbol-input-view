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

All strategies resolve through DrawerAnchorResolver, which clamps heights, sorts them, removes duplicate pixel positions, and provides nearest-anchor and one-stage fling targeting. Anchor heights represent the visible sheet height measured upward from the parent's bottom edge; viewport dimensions come from the measured container, not guessed device display metrics. The container will provide measured viewport/content sizes and convert dp-based public configuration into pixels.

The three-stage strategy follows a BottomSheetBehavior-style stable-state model: Collapsed/Peek, Half-Expanded, and Expanded. The half-expanded visible sheet height defaults to 50% of the measured parent height. Expanded uses the actual available maximum height (the measured container or configured max-height constraint), never a fixed viewport fraction. Automatic Peek follows the Material 16:9 keyline (`parentHeight - parentWidth * 9/16`) when width is measured. If that keyline would place Collapsed above Half-Expanded, the strategy falls back to the configured minimum Peek height so the three states remain meaningfully distinct; callers can also disable automatic Peek explicitly. If measured constraints make anchors coincide, the resolver collapses duplicate pixel positions rather than manufacturing zero-distance states.

## Public API foundation and runtime container

- drawer.core: anchor models, four strategies, normalization, drawer state, and runtime controller contract.
- drawer.panel: lifecycle-aware, theme-aware content contract and replaceable Peek/Expanded hosts.
- drawer.navigation: generic DrawerPage, optional page icons, observable DrawerNavigationController, and cached DrawerPageHost.
- drawer.style: host theme resolver, immutable style tokens, Material 3 theme overlays, and typography/shape/motion settings.
- drawer.DrawerContainer: reusable bottom drawer implementation with anchor measurement, animation, drag handle, scrim, nested vertical scrolling, optional navigation, and runtime setters.

The runtime container is independent of AdvancedSymbolDrawerView. The symbol view remains unchanged until the public API is sufficiently verified and a separate parity migration is planned.

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

## Implementation roadmap

1. **Complete** Public contracts and pure anchor resolver.
2. **Complete** Generic DrawerContainer and state/controller binding.
3. **Complete** Independent Peek/Expanded panel composition.
4. **Complete** Generic navigation bar, indicators and page host.
5. **Core complete** Drag physics and nested vertical-scroll coordination; additional horizontal gesture arbitration and device testing remain.
6. **Core complete** Host theme/style system and Standard, Compact, FullScreen, SplitPanel presets; edge-to-edge/insets refinements remain.
7. Pending: migrate the Symbol drawer through a feature adapter without changing visible behavior.
8. Ongoing: documentation, lifecycle tests, Android instrumentation tests, accessibility and performance validation.


## Style, theme and configuration API

The style API is feature-neutral and uses immutable value objects:

- `DrawerColors`: optional explicit surface, primary/secondary text, accent, outline and scrim colors. A null value inherits the host theme.
- `DrawerColorAttributes`: lets an application map its own theme attributes into the drawer color roles.
- `DrawerDimensions`: peek/maximum heights, corner radius, paddings, spacing and drag-handle size in dp.
- `DrawerTypography`: title/body/caption sizes in sp, typeface styles and optional font family.
- `DrawerShape`: independent corner radii and outline width.
- `DrawerMotion`: settle duration, drag resistance, fling velocity thresholds and spring enablement.
- `DrawerStyle`: combines all tokens, theme mode, elevation and clipping/scrim flags.

`DrawerThemeResolver.resolve(context, style)` resolves color attributes at runtime. `FOLLOW_HOST` uses the supplied Activity/Context theme, so the drawer inherits host theme values rather than maintaining a parallel light/dark state. Explicit LIGHT and DARK modes use Material 3 theme overlays. Theme-aware content providers can override `createView(context)` to construct their views from the resolved theme context; legacy providers using `createView()` remain compatible. Explicit color values override inherited values individually; unspecified roles continue to inherit.

`DrawerConfig.Builder` provides both grouped setters and granular setters, including `setAnchorStrategy`, `setPeekContent`, `setExpandedContent`, `setThemeMode`, `setFollowHostTheme`, `setColors`, `setSurfaceColor`, `setAccentColor`, `setDimensions`, `setPeekHeightDp`, `setExpandedMaxHeightDp`, `setCornerRadiusDp`, `setCornerRadiiDp`, `setTypography`, `setTitleTextSizeSp`, `setMotion`, `setDragResistance`, `setSettleDurationMs`, `setDragEnabled`, `setNestedScrollEnabled`, and dismissal/accessibility setters.

## Baseline layout presets

`DrawerPresets.config(...)` supplies overridable baselines:

- STANDARD: compatibility height behavior and default style.
- COMPACT: content-sized, reduced padding and compact maximum height.
- FULL_SCREEN: three-stage anchors with a full-viewport expanded stage.
- SPLIT_PANEL: three-stage layout with additional spacing for independent panel composition.

Presets are starting configurations, not locked templates. Build a preset config, copy its style into a new builder, and override only the desired properties.


## Generic page host and runtime setter contract

`DrawerPageHost` is an optional Android `FrameLayout` that renders the selected `DrawerPage` from `DrawerNavigationController`. It caches page Views, detaches the previous page before attaching the next one, and dispatches panel attach/visible/hidden/detach callbacks. The page host does not draw a navigation bar; applications may bind a Material TabLayout, a custom underline indicator, or any other navigation UI to the controller and the `DrawerNavigationConfig`.

The runtime `DrawerController` is implemented by `DrawerContainer` and includes atomic `applyConfig`, anchor strategy replacement, style/theme/color/dimension/typography/motion/shape setters, independent Peek/Expanded content replacement, title and page setters, navigation selection/configuration, drag and nested-scroll toggles, scrim/back dismissal, drag-handle and accessibility controls.

`DrawerContainer` also exposes `setMainContent(View?)`, `setPeekContent(...)`, `setExpandedContent(...)`, `setPages(...)`, `expand()`, `collapse()`, `handleBack()`, `navigationHost`, and a `DrawerController` facade. The app module's `DrawerApiLabActivity` is available from the MainActivity overflow menu as **Universal Drawer API Lab**.


## Runtime container usage

```kotlin
val drawer = DrawerContainer(context, DrawerPresets.config(DrawerPreset.SPLIT_PANEL))
drawer.setMainContent(applicationContent)
drawer.controller.setTitle("Workspace")
drawer.setPeekContent(peekContent)
drawer.controller.setPages(
    listOf(
        DrawerPage("logs", "Logcat", logcatContent),
        DrawerPage("tools", "Tools", toolsContent)
    )
)
drawer.controller.setAnchorStrategy(DrawerAnchorStrategies.threeStage())
drawer.controller.setThemeMode(DrawerThemeMode.FOLLOW_HOST)
drawer.controller.expand()
```

`DrawerPanelContent.createView(context)` is the theme-aware construction hook. The no-argument `createView()` remains supported for existing implementations. On explicit theme-mode changes, the container recreates cached page/panel views using the new themed context and dispatches lifecycle callbacks around replacement.

When `enableEdgeToEdge` is enabled, the container consumes system-bar and display-cutout insets for its content padding while its surface can draw edge-to-edge. The host Activity must enable edge-to-edge at the Window level; the reusable View does not mutate host Window flags.

Nested vertical scrolling is coordinated through `NestedScrollingParent3`: upward scroll expands the drawer until its active maximum anchor; downward scroll collapses it only after the nested child reaches its top. Direct handle dragging remains continuous at every intermediate pixel; the nearest anchor is only semantic metadata during the drag. On release, low-velocity gestures settle to the nearest stable anchor, while a qualifying fling advances at most one adjacent anchor. Cancellation returns to the gesture's starting anchor. Horizontal child gestures are not intercepted by the handle drag recognizer.

## API lab coverage

The app module exposes a dedicated Activity through MainActivity's menu. It exercises:

- Existing-height, three-stage, content-sized and custom four-anchor strategies.
- Standard, Compact, Full Screen and Split Panel presets. Full Screen is a distinct full-viewport presentation mode, not merely a larger bottom-sheet height.
- Host-following, explicit light/dark overlays and custom color palettes.
- Navigation top/bottom/hidden placement, line/pill indicators, scrollable tabs and page selection.
- Independent Peek/Expanded content, runtime page replacement and cached page hosting.
- Peek/max heights, drag resistance, settle duration, scrim dismissal, drag handle and accessibility.
- A nested-scrollable page with enough content to test drawer/child scroll hand-off.

The Activity is a manual interaction/visual test harness. CI verifies compilation and JVM unit tests; device-specific gesture feel, edge-to-edge behavior and rendering still require running the Activity on target Android versions.


The API lab includes page-icon/title combinations, icon-only and title-only navigation modes, selected-title emphasis, live corner-radius and typography controls. These controls are intended to validate the public setter surface while the symbol-specific integration remains untouched.
