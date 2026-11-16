# 12 — Extension Registry API

## Registry scope

Each `SymbolDrawerView` owns a session-scoped `SymbolDrawerExtensionRegistry`. It stores runtime handlers/providers, not serializable data. Registrations return `AutoCloseable`; close them when their owning View/editor session ends. Re-register callbacks after process recreation.

## Registration points

- `registerAction`: custom action ID, title/description, optional payload hint, priority and handler.
- `registerIndicator`: custom group-indicator provider.
- `registerItemView`: custom symbol-cell renderer.
- `registerSettingsTab`: a complete settings tab.
- `registerSetting`: a row contributed to a settings tab.
- `registerTheme`: theme token provider.
- `registerGroupProvider`: legacy group-data provider.
- `registerLifecycleExtension`: lifecycle callbacks where supported.
- `registerLegacyAction`: numeric compatibility action handler.

The registry exposes sorted snapshots/list methods, visibility controls, `snapshot()`, `revision()`, `observe()` and `clear()`. Lists are snapshots; do not mutate them as if they were the registry.

## IDs and replacement

Use stable reverse-domain IDs for new contributions. A later registration with the same ID replaces the current contribution. Closing an older handle must not remove a newer replacement. Store the stable ID and payload in data; never serialize lambdas, Views or handler instances.

## Built-in visibility

The public feature IDs include SEARCH, FAVORITES, RECENT, SETTINGS and MANAGER. Visibility is runtime state unless the host persists and restores its snapshot. A registry ID is not proof that every corresponding UI consumer is wired; consult the implementation-specific consumer notes.

## Action dispatch

Register an action, store `SymbolAction.Extension(id, payload)`, then execute through the drawer-bound host. If a registered handler matches and returns false, treat it as a rejection; do not execute the fallback twice. If no handler matches, the host fallback may handle it.

## Indicator and settings integration

The drawer can install a registered indicator in its real indicator slot and restore the built-in indicator. ViewPager2 remains the only page-selection owner.

Settings screens must receive the same session registry to render contributed tabs/rows. A custom settings scope updates the existing settings path; extension-specific fields should be stored by the host, not added to the fixed V2 schema without a versioned API change.

## Theme scope

The theme provider supplies ARGB color tokens. Current source notes specify that the drawer shell and built-in group indicator consume these tokens; do not assume the provider recolors every manager, settings screen or symbol cell.

## Threading and errors

The registry is synchronized, but View factories, handlers and UI updates must obey host thread requirements. Avoid blocking I/O in callbacks. Isolate extension failures, log diagnostics and release all observers/handles.
