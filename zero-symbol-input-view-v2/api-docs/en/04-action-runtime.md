# 04 — Action Registration, Execution and Text Editing

## Canonical package layout

- Execution API, host contract, registry and text-document API: `com.zerostudio.idea.widget.drawer.action`.
- `SymbolAction` and its enums: `com.zerostudio.idea.widget.editor.symbolinput.v2.model`.
- Product compatibility facades, `SymbolActionJsonCodec` and `BuiltinSymbolActionCatalog`: `com.zerostudio.idea.widget.editor.symbolinput.v2.action`.

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
```

The runtime separates action data, execution and host capabilities:

```text
SymbolEntry.shortAction / longAction
              |
      SymbolActionExecutor
              |
        SymbolActionHost
              |
       Host editor / clipboard
```

## SymbolActionHost

The host implements synchronous capabilities: `insertText`, `replaceSelection`, `surroundSelection`, `moveCursor`, `delete`, `transformSelection`, `executeEditorCommand`, `performClipboard`, `openManager` and `executeExtension`.

Return `true` only after the operation has been accepted and completed. Return `false` for unsupported or rejected operations. The executor catches ordinary `Exception` and reports `Failed`; it does not switch threads. Call it on the thread required by the editor.

## Cancellation and results

`SymbolActionCancellation` is checked between action steps. It cannot interrupt a host method already executing synchronously.

`SymbolActionResult` distinguishes:
- `Executed`
- `Cancelled(completedActions)`
- `Unsupported(action)`
- `Invalid(reason)`
- `Failed(cause)`
- `PartialFailure(completedActions, cause)`

Composite actions are sequential, not transactional. A partial failure does not roll back completed edits. Use a host editor transaction/undo group if atomicity is required.

## Executor

`SymbolActionExecutor.execute(action, host, cancellation)` validates and executes a typed action tree. It applies bounds to nested/composite actions and rejects invalid IDs/payloads rather than dispatching arbitrary commands. Unknown editor command IDs should be handled through an explicit host allowlist.

## Registry

`SymbolActionRegistry<C>` registers handlers by stable string `SymbolActionId` and supports legacy numeric IDs where required. Registration handles should be closed by their owner. `RegistryBackedSymbolActionHost` decorates a host with registry dispatch; if no extension handler matches, the host fallback remains available. A matched handler returning false is a rejection, not a reason to execute the fallback a second time.

## JSON

Use `SymbolActionJsonCodec` to encode/decode typed actions. Treat schema versions, IDs and payload constraints as compatibility contracts. Reject malformed/deep/oversized data; do not silently reinterpret unknown actions.

## Editor-neutral document

`SymbolTextDocument` exposes text length, line count, caret/selection, line/range reads, replacement and selection updates. Offsets are UTF-16 code units and ranges are end-exclusive. See [13](13-public-package-and-editor-neutral-actions.md) for transaction and adapter semantics.

`SymbolTextDocumentActionHost` maps common editing commands to a document adapter. Clipboard and manager navigation remain injected host capabilities.
