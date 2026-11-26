# 10 — API Index, Compatibility and Troubleshooting

## Main API areas

| Area | Main types |
|---|---|
| Symbol toolbar | `SymbolDrawerView`, `SymbolGroup`, `SymbolEntry`, `SymbolAction` |
| Action runtime | `SymbolActionHost`, `SymbolActionExecutor`, `SymbolActionResult`, `SymbolActionRegistry`, `SymbolActionJsonCodec` |
| Text adapter | `SymbolTextDocument`, `SymbolTextDocumentActionHost`, `GenericSymbolTextActions` |
| Data | `SymbolGroupRepository`, `SymbolGroupManager`, storage/bundle codecs, migration adapters |
| Settings | `SymbolDrawerSettings`, settings repository/codec/screen |
| Generic Drawer | `DrawerContainer`, `DrawerController`, anchor strategies/resolver, interaction controller |
| Navigation/panels | `DrawerPage`, `DrawerNavigationController`, `DrawerPageHost`, `DrawerPanelContent` |
| Style | `DrawerStyle`, colors, dimensions, typography, motion, shape and presets |
| Adaptive | `WindowAdaptiveInfo`, observers/controllers and adaptive resolvers |
| Extensions | `SymbolDrawerExtensionRegistry`, action/indicator/item/settings/theme extension contracts |

Use the detailed page for exact signatures and behavior.

## Same-name configuration types

Two different types are named `DrawerConfig`:
- `model.DrawerConfig`: symbol toolbar configuration.
- `drawer.config.DrawerConfig`: generic Drawer configuration.

Use import aliases when both are needed.

## Compatibility contracts

- Keep V1 numeric action IDs and payload meanings stable.
- V1 migration is read-only against legacy preferences and writes to the independent V2 repository.
- Current documented schema versions: settings 3 and typed actions 1. Storage/bundle envelopes are handled by their own codecs.
- Do not rename stable IDs, JSON fields or action IDs without a migration/version plan.
- Kotlin typealiases in the new Drawer facade do not create Java-visible JVM classes.

## Manager screen

`SymbolDrawerManagerScreen` supports group/symbol CRUD, ordering, duplication, copy/move, batch selection, search, favorites/recent use and bundle import/export. Its UI state (favorites/recent) is not the same as the group repository snapshot. Bind it to the drawer/session and close the returned handle when the screen exits.

## Troubleshooting

- No symbols: call `reloadGroups()` or `submitGroups`; inspect repository and parent height.
- Action does not edit: verify short/long selection, host capability, thread and structured result.
- Manager changes do not appear: bind the same drawer or submit/reload the changed snapshot.
- Page resources linger: distinguish hidden from detached/disposed.
- Large-screen layout is wrong: use current window dimensions and avoid double-subtracting Insets.
- Settings do not refresh: apply the new snapshot or observe repository changes.

Add tests for malformed JSON, failed writes, migration, unsupported host methods, partial composite failure, gestures, lifecycle and window changes.
