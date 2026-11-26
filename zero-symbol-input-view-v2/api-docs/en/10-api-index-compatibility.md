# 10 — API index, package layout and compatibility

This page indexes the current `zero-symbol-input-view-v2` Android View artifact.

## 1. Symbol-input product APIs

| Type | Package | Purpose |
|---|---|---|
| `SymbolDrawerView` | `com.zerostudio.idea.widget.editor.symbolinput.v2` | Android View symbol toolbar |
| `SymbolGroup`, `SymbolEntry`, `SymbolAction` | `editor.symbolinput.v2.model` | Domain models and typed actions |
| Product `DrawerConfig` | `editor.symbolinput.v2.model` | Symbol grid/toolbar geometry |
| `SymbolDrawerSettings` | `editor.symbolinput.v2.settings` | Immutable settings snapshot |
| `SymbolDrawerManagerScreen` | `editor.symbolinput.v2.manager` | View-based manager UI |
| `SymbolDrawerSettingsScreen` | `editor.symbolinput.v2.settings` | View-based settings UI |

## 2. Generic Drawer APIs

| Type | Package | Purpose |
|---|---|---|
| `DrawerContainer` | `com.zerostudio.idea.widget.drawer` | Generic Android ViewGroup |
| `DrawerController`, `DrawerState` | `drawer.core` | Runtime control and state snapshot |
| `DrawerAnchor`, `DrawerAnchorInput`, `DrawerAnchorStrategy` | `drawer.core` | Anchor models and policies |
| `DrawerInteractionController` | `drawer.core` | Touch-axis arbitration and settling |
| `DrawerConfig`, `DrawerPresentationMode` | `drawer.config` | Generic container configuration |
| `DrawerPage`, `DrawerNavigationController`, `DrawerPageHost` | `drawer.navigation` | View page navigation |
| `DrawerPanelContent`, `DrawerPanelHost` | `drawer.panel` | View panel factory and host |
| `DrawerStyle`, `DrawerColors`, `DrawerDimensions` | `drawer.style` | Appearance tokens |
| `DrawerPreset`, `DrawerPresets` | `drawer.preset` | Preset configurations |
| `SymbolDrawerAnchorAdapter` | `drawer.adapter` | Symbol geometry adapter |

## 3. Editor-neutral action APIs

The canonical package is `com.zerostudio.idea.widget.drawer.action`:

- `SymbolActionHost`, `SymbolActionExecutor`, `SymbolActionResult`, `SymbolActionCancellation`.
- `SymbolActionRegistry`, `SymbolActionHandler`, `SymbolActionId`.
- `SymbolTextDocument`, `SymbolTextDocumentActionHost`, `SymbolTextClipboard`.
- `SymbolTextEditing`, `GenericSymbolTextActions`, `BuiltinSymbolActionCatalog`.

The `SymbolAction` sealed model and its enums remain in `editor.symbolinput.v2.model`.

## 4. Runtime contracts

- IDs are stable identities; persisted group/symbol IDs must be unique.
- Action execution is synchronous and does not switch threads.
- Composite actions are sequential and may return partial failure; no automatic rollback is provided.
- Text offsets use UTF-16 code units and end-exclusive ranges.
- Repository writes must succeed before the manager publishes a new snapshot.
- Extension registrations return `AutoCloseable` handles and must be closed by their owner.
- This artifact uses Android View attach/detach and saved-state contracts.

## 5. Artifact compatibility

The Compose artifact is a separate implementation. Its `DrawerContainer`, `DrawerPanelContent` and `DrawerPageHost` are Composables/Composable slots, unlike the View types documented here. Do not add both UI artifacts to one app dependency graph.

## 6. Troubleshooting

Check the artifact and fully qualified package first. In particular, distinguish `editor.symbolinput.v2.model.DrawerConfig` from `drawer.config.DrawerConfig`. Kotlin `typealias` declarations do not emit independent Java classes.
