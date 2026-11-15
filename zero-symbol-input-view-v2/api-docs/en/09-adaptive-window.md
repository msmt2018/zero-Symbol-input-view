# 09 — Adaptive Window and Large-Screen APIs

## Window-first design

Layout decisions use the current application window, not device model or physical orientation. This supports split-screen, freeform windows, desktop mode and foldables. Resolvers return immutable specifications; the host applies them to its own View hierarchy.

## WindowAdaptiveInfo

The snapshot contains window width/height in dp, density, Insets and folding-feature information. Available dimensions subtract the supplied Insets and clamp to zero.

Width breakpoints:
- COMPACT: below 600dp;
- MEDIUM: 600dp to below 840dp;
- EXPANDED: 840dp and above.

These are window-width classes, not device-orientation values.

## Folding and Insets

`WindowInsetsDp` stores non-negative left/top/right/bottom values. `FoldingFeatureInfo` stores posture, separating state and optional bounds. The library-neutral snapshot avoids requiring AndroidX WindowManager; the host converts its platform folding feature.

`WindowAdaptiveInfo.fromPixels` converts pixel dimensions to dp and requires positive density.

## Observer and controller

`AdaptiveWindowObserver` listens to View layout/attach and optionally root WindowInsets. It publishes valid attached-window snapshots, suppresses identical snapshots by default, supports folding-feature updates and can be closed idempotently.

`AdaptiveWindowController` adds multi-subscriber distribution and error diagnostics. Close it with the host View lifecycle.

## Resolvers

- `AdaptiveContentLayoutResolver`: content width and dialog sizing.
- `AdaptiveLayoutResolver`: canonical list/detail/feed/supporting-pane arrangements.
- `AdaptiveGridResolver`: grid column count and spacing.
- `AdaptiveDimensions`: dimension scaling.
- `SymbolDrawerDockingLayoutResolver`: bottom/side docking dimensions.
- `SymbolDrawerDockingCoordinator`: coordinates settings with window observation.

Resolvers calculate values; the host must apply them. Avoid subtracting Insets twice. Recompute after window bounds, density, posture or Insets change.
