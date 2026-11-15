# 07 — Universal Drawer Framework

## Relationship to SymbolDrawerView

`DrawerContainer` is a general-purpose drawer for arbitrary host content and pages. It supports bottom-sheet/full-screen presentation, anchors, drag, nested scrolling, panels, navigation, styles and Insets integration.

`SymbolDrawerView` remains a specialized symbol toolbar with its own ViewPager2 and symbol adapter. Do not wrap it in another Drawer or confuse its `model.DrawerConfig` with the generic `drawer.config.DrawerConfig`.

## DrawerContainer

`DrawerContainer(context, config)` is a ViewGroup and NestedScrollingParent3. Its main surface methods include:
- `setMainContent(view)`
- `setTitle(title)`
- `setPeekContent(content)`
- `setExpandedContent(content)`
- `setPages(pages, preserveSelection)`
- `expand(animated)` / `collapse(animated)`
- `refreshAnchors()`
- `handleBack(): Boolean`

Setting pages replaces direct expanded content. Page IDs must be unique. `handleBack` returns true only when the configured back behavior consumed the request.

## DrawerController

The controller exposes state transitions, anchor strategy, presentation mode, atomic config application, style updates, panel/page configuration, drag/nested-scroll/edge-to-edge/back/scrim/handle switches and state observation. Select pages by stable ID; unknown IDs return false.

## Configuration and presentation

`drawer.config.DrawerConfig` is built through its builder and defines presentation mode and interaction behavior. `DrawerPresentationMode` distinguishes bottom-sheet and full-screen modes.

## Anchors

`DrawerAnchor`, `DrawerAnchorInput` and `DrawerAnchorState` describe stable anchor identity and resolved geometry. Strategies include existing-height compatibility, three-stage, content-based and custom strategies. `DrawerAnchorResolver` normalizes anchors, finds the nearest anchor and computes fling targets.

## Interaction

`DrawerInteractionController` handles gesture-axis arbitration, drag, velocity, animation and settle. It does not measure Views or create layouts; the container supplies geometry and applies callbacks. Horizontal gestures should remain with horizontal child content. A vertically scrollable child should be allowed to consume its own scroll before the drawer takes over.

## Insets and host responsibilities

The host remains responsible for its Activity/window layout and any Insets policy not explicitly provided by the container. Re-measure and call `refreshAnchors()` after content or window constraints change.
