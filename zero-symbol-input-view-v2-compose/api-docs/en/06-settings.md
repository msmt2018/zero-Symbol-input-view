# 06 — Settings API

## Settings snapshot

`SymbolDrawerSettings` is an immutable validated snapshot. The current schema version documented by the source is 3. It covers theme mode, columns, collapsed rows, cell dimensions/spacing, indicator height/style, labels, expansion memory, animation, fling, long-press, haptics, page caching, adjacent prefetch, touch target, search behavior and large-screen docking.

Constructor validation rejects out-of-range values with `IllegalArgumentException`. Use `toDrawerConfig()` to map symbol-drawer geometry/interaction settings to the symbol-input `model.DrawerConfig`; it is not the generic Drawer config.

## Enums

- `ThemeMode`: SYSTEM, LIGHT, DARK.
- `GroupIndicatorStyle`: text tabs, text, pill, dots, underline and additional visual styles. The enum identifies a renderer strategy; actual rendering depends on the indicator implementation.
- `HapticMode`: OFF, SELECTION, ALL.
- `LargeScreenLayoutMode`: AUTO, BOTTOM, SIDE.
- `SideDockEdge`: LEFT, RIGHT.

## Repository

`SymbolDrawerSettingsRepository` provides `load()`, `save(settings)`, `reset()` and `hasStoredSettings()`. `SettingsLoadResult` reports the loaded snapshot and recovery state. `SharedPreferencesSymbolDrawerSettingsRepository` is the default implementation.

## Codec

`SymbolDrawerSettingsCodec` supports lenient `decode` for recovery and strict `decodeStrict` for validation. Do not bypass the codec or write unversioned JSON directly into the repository.

## Settings screen

`SymbolDrawerSettingsScreen` is a programmatic Android View. It can report changes to the host, which should apply the resulting snapshot with `drawer.applySettings(settings)`. When using extension tabs/items, pass the same drawer-session extension registry.

## Runtime behavior

Settings persistence and runtime application are separate concerns. A saved setting does not guarantee every existing View has refreshed; apply the snapshot or observe repository changes. Avoid retaining an old Activity Context across configuration changes. Test reset, corrupt settings, migration, range validation and runtime updates.
