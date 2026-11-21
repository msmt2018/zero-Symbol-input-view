# 03 — Data Models and Typed Actions

Model package: `com.zerostudio.idea.widget.editor.symbolinput.v2.model`.

## SymbolGroup

```kotlin
data class SymbolGroup(
    val id: String,
    val title: String,
    val symbols: List<SymbolEntry>
)
```

The ID is a stable, nonblank identity. Renaming must not change it. A newly created group should receive a new ID even if its title matches a deleted group.

## SymbolEntry

```kotlin
data class SymbolEntry(
    val id: Long,
    val label: String,
    val shortAction: SymbolAction,
    val longAction: SymbolAction? = null,
    val icon: String? = null,
    val legacyShortActionId: Int? = null,
    val legacyShortText: String? = null,
    val legacyLongActionId: Int? = null,
    val legacyLongText: String? = null
)
```

`label` is display text, not necessarily inserted text. The short action is required; the long action is optional. Legacy numeric IDs and text fields are compatibility metadata, not the V2 action protocol. Manager and storage require globally unique, nonzero symbol IDs.

## SymbolAction variants

The sealed action model describes intent; `SymbolActionExecutor` dispatches it to `SymbolActionHost`.

| Variant | Meaning |
|---|---|
| `InsertText(template)` | Insert a text template |
| `ReplaceSelection(text)` | Replace the current selection |
| `SurroundSelection(prefix, suffix)` | Wrap the selection |
| `MoveCursor(target)` | Move the caret |
| `Delete(kind)` | Delete a specified range |
| `TransformSelection(transform)` | Transform selected text |
| `EditorCommand(commandId, payload)` | Invoke a host command |
| `Clipboard(operation)` | Copy, cut or paste through host capability |
| `OpenManager` | Request manager navigation |
| `Composite(actions)` | Execute child actions in order |
| `Extension(extensionId, payload)` | Invoke a registered extension or host fallback |

The executor passes `InsertText.template` to the host. Macro expansion is available only when the host/document adapter implements it; do not assume every editor adapter supports template markers.

## Related enums

- `CursorTarget`: horizontal/vertical movement and line/document boundaries.
- `DeleteKind`: deletion scope.
- `SelectionTransform`: selection transformation.
- `ClipboardOperation`: clipboard operation.

Use the enum values declared by the source version you compile against; do not persist display labels as identifiers.

## Legacy actions and DrawerConfig

`LegacySymbolAction` preserves V1 action metadata for migration and compatibility. The symbol-input `model.DrawerConfig` configures symbol-grid behavior; the generic Drawer configuration is a different class in `drawer.config`.

Treat model collections as immutable snapshots. Replace data by submitting a new list rather than mutating nested lists after submission.
