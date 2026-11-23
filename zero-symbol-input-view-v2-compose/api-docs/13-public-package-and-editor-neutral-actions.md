# 13 — Public package and editor-neutral document contract

## 1. Public package direction

New integrations should use the public namespace:

```kotlin
com.zerostudio.idea.widget.drawer
com.zerostudio.idea.widget.drawer.core
com.zerostudio.idea.widget.drawer.navigation
com.zerostudio.idea.widget.drawer.panel
com.zerostudio.idea.widget.drawer.style
com.zerostudio.idea.widget.drawer.adapter
com.zerostudio.idea.widget.drawer.action
```

The V2 symbol-input product namespace remains:

```kotlin
com.zerostudio.idea.widget.editor.symbolinput.v2
```

This separation is intentional: the generic drawer is a reusable widget framework; the symbol-input module is one product built on top of it.

### Migration status — transitional API facade

The current branch adds Kotlin type-alias entry points under `com.zerostudio.idea.widget.drawer` for the generic drawer container, configuration, anchors, navigation, panels, styles and adapters. Existing implementation classes still reside in the historical `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` packages. This facade keeps source migration incremental and avoids breaking existing consumers, but it is not yet a physical source/package relocation.

Consequences:
- Kotlin callers can start importing the new public package now.
- Java callers cannot consume Kotlin `typealias` declarations as Java classes.
- Do not mix the new facade and historical implementation imports in new Kotlin code.
- A later source relocation must update package declarations, imports, test package declarations, documentation and published API compatibility together.

## 2. Editor independence

V2 does not require an editor-core object, editor View, editor controller, syntax tree, Sora instance, or any editor-specific class.

The action engine operates against a small document capability contract:

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocumentActionHost
```

A host adapter is required for the *currently focused text document*, not for an editor core instance. It can be implemented by:
- a custom code editor's text buffer;
- a plain Android text input;
- a terminal/editor document model;
- an in-memory or remote document;
- a test fake with no Android View at all.

The adapter is the only layer that knows how the host stores text, maps selection/caret state, and commits edits. V2 algorithms never cast it to a particular editor implementation.

## 3. SymbolTextDocument

All offsets use UTF-16 code units, matching JVM `String` indexing. Line and column are zero-based. `lineEndOffset` excludes the line separator.

| Member | Contract |
|---|---|
| `isEditable` | Whether the current document accepts edits |
| `textLength` | Current UTF-16 document length |
| `lineCount` | Number of logical lines |
| `cursorOffset` | Active caret offset |
| `selectionStart/selectionEnd` | Selection endpoints; may be reversed by the host |
| `lineForOffset(offset)` | Resolve a UTF-16 offset to a zero-based line |
| `lineStartOffset(line)` | Start offset of a logical line |
| `lineEndOffset(line)` | End offset excluding CR/LF separator |
| `lineText(line)` | Text content of a line |
| `textBetween(start,end)` | Read a UTF-16 range; end-exclusive |
| `charAt(offset)` | Read one UTF-16 code unit |
| `replace(start,end,text)` | Replace a range; false means rejected |
| `setSelection(start,end)` | Update selection/caret |
| `beginEditTransaction()` | Optional host transaction hook |
| `endEditTransaction(commit)` | Optional commit/rollback hook |
| `selectedText()` | Default normalized selection read |
| `positionForOffset(offset)` | Convert offset to line/column |
| `offsetForPosition(position)` | Convert line/column to a clamped offset |
| `setCursorPosition(position)` | Move caret by line/column |
| `applyEdits(edits)` | Validate non-overlapping ranges and apply from right to left |

### TextDocumentPosition

`TextDocumentPosition(line, column)` is a zero-based logical line/column pair. Negative values are rejected.

### TextDocumentEdit

`TextDocumentEdit(start, end, replacement)` describes one UTF-16 range replacement. The end is exclusive. Edits passed to `applyEdits` must not overlap.

### Transaction semantics

The default `applyEdits` implementation calls the optional transaction hooks and applies edits from right to left, preserving earlier offsets. The default hooks are no-ops: an adapter that needs true atomic rollback must override them or override `applyEdits` with its own transaction implementation. A false return from `replace` stops the batch and calls `endEditTransaction(false)`.

## 4. Action execution

`SymbolTextDocumentActionHost` implements the built-in text action semantics over the document contract:
- insertion and macro markers;
- replace/surround selection;
- cursor movement by character, line and document boundary;
- backspace/delete, current-line deletion and delete-to-line-boundary;
- selection case transforms;
- generic editor-neutral text commands;
- clipboard and manager navigation through injected host callbacks.

Clipboard and manager navigation are intentionally capabilities supplied by the application. A generic document does not imply Android clipboard access or a manager screen.

### Minimal adapter sketch

```kotlin
class MyBufferAdapter(
    private val buffer: MyTextBuffer
) : SymbolTextDocument {
    override val isEditable get() = buffer.isEditable
    override val textLength get() = buffer.length
    override val lineCount get() = buffer.lineCount
    override val cursorOffset get() = buffer.caret
    override val selectionStart get() = buffer.selectionStart
    override val selectionEnd get() = buffer.selectionEnd

    override fun lineForOffset(offset: Int) = buffer.lineForOffset(offset)
    override fun lineStartOffset(line: Int) = buffer.lineStart(line)
    override fun lineEndOffset(line: Int) = buffer.lineEnd(line)
    override fun lineText(line: Int) = buffer.lineText(line)
    override fun textBetween(start: Int, end: Int) = buffer.substring(start, end)
    override fun charAt(offset: Int) = buffer[offset]
    override fun replace(start: Int, end: Int, replacement: String) =
        buffer.replace(start, end, replacement)
    override fun setSelection(start: Int, end: Int) = buffer.setSelection(start, end)
}
```

The sample uses a fictional `MyTextBuffer`; adapt those calls to the host's buffer API. No editor-core object is passed into the V2 library.

## 5. Large-document behavior

The action host's adjacent cursor movement and single-code-point deletion inspect at most two UTF-16 code units around the caret. They do not materialize the whole document. Operations that inherently transform a whole line or selection may read that affected range. Host adapters should implement `textBetween` efficiently and avoid converting the entire document to a String for small-range reads.

The generic command catalog applies explicit document-size/line-count limits to document-wide transformations. A host should surface unsupported/limit outcomes rather than silently truncating text.

## 6. Compose extension boundary

The extension registry exposes runtime contributions and snapshots. Current extension UI contracts are Composable interfaces:

- SymbolDrawerItemRenderer.Content(entry, state, onClick, onLongClick)
- SymbolGroupIndicatorExtension.Content(state)
- SymbolDrawerSettingsTabExtension.Content(scope)
- SymbolDrawerSettingExtension.Content(scope)

These contracts do not expose View factories, Context, ViewHolder callbacks, or attach/detach hooks. See 12-extension-framework.md for the exact registry methods and ownership rules.

The registry itself is a runtime session container. It does not persist lambdas or automatically persist third-party settings. Hosts must keep stable IDs and persist only serializable action IDs/payloads or their own extension-owned data.

## 7. Compatibility

The old action-package SymbolTextDocument name may remain available as a Kotlin typealias to the public contract. Kotlin typealiases do not create JVM classes and do not establish Java ABI compatibility.

V1 integer action IDs and legacy symbol models remain isolated behind explicit legacy adapters/codecs. The Compose module does not import or host the old View UI. Legacy data migration is a data compatibility path, not a runtime UI bridge.

## 8. Compose state and ownership

Compose UI state belongs to the relevant Composition/session scope. Do not store Activity, Fragment, Android View, Context, or LifecycleOwner inside document adapters, action models, repositories, or serialized data.

A host may adapt its current focused editor/document to SymbolTextDocument and SymbolActionHost. The adapter must be replaced when the focused document changes; do not accidentally retain a previous editor tab's adapter.

Compose has Composition and effect lifetimes. This module avoids traditional View attach/detach and Fragment View lifecycle callbacks; it does not claim that Compose has no lifecycle semantics.
