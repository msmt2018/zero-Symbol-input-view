# 15 — Symbol drawer extension examples

Register a custom indicator with `registerIndicator` and implement `SymbolGroupIndicatorExtension.Content(state)`. The state supplies group snapshots, selected index and `onSelectGroup`; do not create a second pager or selection source of truth.

Implement `SymbolDrawerItemRenderer.Content` to render a symbol entry in Compose. The renderer receives an immutable render state and click/long-click callbacks. It does not create or bind a View.

Settings contributions use `SymbolDrawerSettingsTabExtension` and `SymbolDrawerSettingExtension`. Update settings through `SymbolDrawerSettingsScope.update`.

Every registration returns an `AutoCloseable`. The owning session explicitly closes handles. Do not bind registrations to Fragment viewLifecycleOwner or retain Activity/View/editor objects in static singletons. Composable bodies must remain free of persistence and other irreversible side effects.