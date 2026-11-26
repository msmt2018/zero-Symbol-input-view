# 13 — Public packages and editor-neutral document contract

This page describes the current Android View artifact source layout. The generic Drawer implementation has been physically moved to its public package; the symbol-input feature retains its own product namespace.

## 1. Canonical packages

| API area | Package |
|---|---|
| Generic View Drawer | `com.zerostudio.idea.widget.drawer` |
| Core anchors/controller/state | `com.zerostudio.idea.widget.drawer.core` |
| Navigation and page host | `com.zerostudio.idea.widget.drawer.navigation` |
| View panels | `com.zerostudio.idea.widget.drawer.panel` |
| Configuration | `com.zerostudio.idea.widget.drawer.config` |
| Styles and theme resolution | `com.zerostudio.idea.widget.drawer.style` |
| Symbol drawer adapters | `com.zerostudio.idea.widget.drawer.adapter` |
| Editor-neutral actions | `com.zerostudio.idea.widget.drawer.action` |
| Symbol-input product | `com.zerostudio.idea.widget.editor.symbolinput.v2` |
| Symbol domain models | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` |

The former `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` namespace is no longer the canonical implementation package. New integrations must not import it.

## 2. Recommended imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.core.DrawerController
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.drawer.action.SymbolTextDocument
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
```

The View artifact's symbol control remains an Android View. Some compatibility entry points are Kotlin `typealias` declarations; aliases do not emit independent Java classes. Java consumers must use the package of the actual JVM declaration.

## 3. Editor-neutral text API

The action/document adapter targets the currently focused document, not a specific editor implementation. Offsets use UTF-16 code units; line and column values are zero-based; ranges are end-exclusive `[start, end)`; line-end offsets exclude CR/LF separators.

For multi-range edits, validate non-overlap and apply edits from right to left so earlier replacements do not shift later offsets.

## 4. Execution and threading

`SymbolActionExecutor` is synchronous and does not switch threads. Invoke it on the thread required by the host editor. A host returning false means unsupported or rejected. Invalid input, cancellation, exceptions and partial Composite completion are distinct `SymbolActionResult` outcomes.

Composite actions are sequential and are not automatically transactional. Atomic rollback requires real transaction support from the host document adapter.

## 5. Artifact boundary

This module implements Android Views: `SymbolDrawerView`, generic `DrawerContainer`, page hosts and panel hosts are View APIs. Do not apply these View construction, Context, attach/detach or saved-state contracts to the separate Compose artifact.
