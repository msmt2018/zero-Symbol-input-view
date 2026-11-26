# 12 — Compose extension registry

`SymbolDrawerExtensionRegistry` registers action handlers, group indicators, symbol item renderers, legacy group providers, settings tabs/items, themes and built-in visibility overrides. Registration methods return `AutoCloseable` handles. The owner closes handles explicitly when its business session ends.

Composable UI extensions:
- `SymbolGroupIndicatorExtension.Content(state)`
- `SymbolDrawerItemRenderer.Content(entry, state, onClick, onLongClick)`
- `SymbolDrawerSettingsTabExtension.Content(scope)`
- `SymbolDrawerSettingExtension.Content(scope)`

These are Composable slots, not View factories. No RecyclerView ViewHolder, MotionEvent or View attach/detach callbacks are part of this module's extension contract.

`SymbolDrawerExtensionSnapshot` exposes a revision and immutable extension lists. Registry observers receive ordered snapshots; close the observer handle when the owning consumer no longer needs updates. Avoid static retention of UI or editor objects.

`clear()` removes all registrations and action handlers; call it only when the registry is no longer in use.