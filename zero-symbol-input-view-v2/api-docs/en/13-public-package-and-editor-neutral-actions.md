# 13 — Public Packages and Editor-Neutral Document Contract

## Package direction

Use the generic Drawer facade:
```kotlin
com.zerostudio.idea.widget.drawer
com.zerostudio.idea.widget.drawer.core
com.zerostudio.idea.widget.drawer.navigation
com.zerostudio.idea.widget.drawer.panel
com.zerostudio.idea.widget.drawer.style
com.zerostudio.idea.widget.drawer.adapter
com.zerostudio.idea.widget.drawer.extension
com.zerostudio.idea.widget.drawer.action
```

The symbol-input product remains under `com.zerostudio.idea.widget.editor.symbolinput.v2`.

## Transitional facade and Java ABI

The current public package includes Kotlin typealiases to implementation classes in the historical V2 drawer packages. Kotlin consumers can import the new names. Typealiases do not create JVM classes and cannot be imported as Java classes. This is a source-level Kotlin facade, not a physical package relocation. A real Java-facing API requires actual public JVM declarations and Java compile fixtures.

## SymbolTextDocument

The editor-neutral adapter exposes editability, UTF-16 text length, line count, caret/selection, line/range reads, character reads, replacement and selection updates. Offsets are UTF-16 code units, line/column are zero-based, and ranges are end-exclusive. `lineEndOffset` excludes CR/LF separators.

Additional helpers include selected-text reading, offset/position conversion, caret positioning, optional edit transactions and multi-edit application.

## Transactions

The default multi-edit implementation validates non-overlapping ranges and applies edits from right to left. Default transaction hooks are no-ops. For true atomic rollback, override the hooks or implement `applyEdits` with the host editor's transaction/undo API. A rejected replacement stops the batch and reports failure.

## Editor independence

The host supplies an adapter for the currently focused document. It may wrap a custom code editor, Android text input, terminal buffer, in-memory model or test fake. V2 does not cast it to Sora or another editor implementation.

## Generic text action host

`SymbolTextDocumentActionHost` implements common insertion, selection, caret, deletion and editor-neutral commands. Clipboard and manager navigation are injected host capabilities. Document-wide commands have explicit size/line limits.

## Registry observation

The registry exposes immutable snapshots, a monotonically increasing revision and an `observe(emitCurrent, listener)` handle. The data/notification API does not automatically inject contributions into every UI surface. Each consumer must subscribe and close its observer with its View lifecycle.

## Template directives

The source contract documents markers including `$$`, `$S`, `$E`, `$C`, `$T`, `$N`, `$TAB`, `$L`, `$W`, `$I`, `$U`/`$UP`, `$V`/`$LOW`, `$R`/`$REV`, `$CAP` and `$TRIM`. Markers are consumed; unknown marker forms remain literal. Check the current source before relying on a token in a persisted template.
