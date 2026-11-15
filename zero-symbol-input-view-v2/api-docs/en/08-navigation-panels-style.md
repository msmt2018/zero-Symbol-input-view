# 08 — Navigation, Panels and Style

## Pages and navigation

`DrawerPage` has a stable ID, title, content provider and optional icon. Do not use list indices as persistent identity.

`DrawerNavigationController` is a UI-independent selection model. It supports submitting pages, selecting by ID/index, retrieving page snapshots and observing selection. It does not draw tabs or create Views.

`DrawerNavigationConfig` controls enabled state, placement (TOP/BOTTOM/HIDDEN), indicator, scrolling, titles/icons, item dimensions and selected-page visibility.

## DrawerPageHost

`DrawerPageHost` creates, caches, attaches and disposes page Views. It can:
- submit pages while preserving selection;
- change the Context used for page creation;
- select by ID or index;
- measure the current page;
- notify panel visibility;
- dispose cached Views and listeners.

A disposed host rejects further selection. A page may remain cached while hidden; use hidden callbacks to pause work and detached/dispose callbacks to release resources.

## Panels

`DrawerPanelContent` describes a panel and its lifecycle. `DrawerPanelSlot` distinguishes Peek and Expanded content. `DrawerPanelHost` owns the View for a panel. Do not assume hidden means destroyed.

## Style

The style package includes:
- `DrawerColors` and `DrawerColorAttributes`;
- `DrawerDimensions`;
- `DrawerTypography`;
- `DrawerMotion`;
- `DrawerShape`;
- aggregate `DrawerStyle`;
- `DrawerThemeMode` and `DrawerThemeResolver`.

Style setters replace their corresponding immutable style snapshots. Null color roles may inherit host theme colors where supported.

## Presets

`DrawerPreset` and `DrawerPresets` provide reusable configurations. Treat presets as starting points; apply host-specific dimensions, theme and Insets policy.

## Lifecycle rules

Create page Views using the current themed Context. Release listeners, animations and resources in the matching lifecycle callback. Do not retain destroyed Activity/View instances in cached pages. Test Context recreation and disposal.
