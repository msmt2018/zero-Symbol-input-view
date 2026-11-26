# 13 — Public packages and editor-neutral document contract

The Compose artifact has a physically relocated generic Drawer implementation under the public package. The symbol-input product remains in its own namespace.

## Canonical package map

| API | Package |
|---|---|
| Generic Compose Drawer | `com.zerostudio.idea.widget.drawer` |
| Anchors, controller and state | `com.zerostudio.idea.widget.drawer.core` |
| Navigation and Composable page host | `com.zerostudio.idea.widget.drawer.navigation` |
| Composable panel slots | `com.zerostudio.idea.widget.drawer.panel` |
| Configuration | `com.zerostudio.idea.widget.drawer.config` |
| Compose style tokens | `com.zerostudio.idea.widget.drawer.style` |
| Editor-neutral actions | `com.zerostudio.idea.widget.drawer.action` |
| Symbol-input Composables | `com.zerostudio.idea.widget.editor.symbolinput.v2` |
| Symbol models | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` |

Do not import the former `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` path.

## Recommended imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.rememberSymbolDrawerState
```

The Compose artifact does not expose `SymbolDrawerView`. Use the `@Composable SymbolDrawer` entry point.

## Text document contract

The action engine operates on the focused document through `SymbolTextDocument`. Offsets are UTF-16 code units, line/column are zero-based, ranges are end-exclusive `[start, end)`, and line-end offsets exclude CR/LF separators. Multi-range edits must validate overlap and apply from right to left.

## Execution semantics

`SymbolActionExecutor` is synchronous and never changes threads. Host adapters must honor their editor's thread rules. Unsupported operations, invalid input, cancellation, exceptions and partial Composite completion are represented separately. Composite is not an automatic transaction; rollback depends on host-provided transaction semantics.

Domain models, repositories, codecs and core state policies remain UI-neutral. Compose slots and effect handling belong at the UI boundary. Compose has Composition/recomposition/effect lifetimes, but this API does not require Activity, Fragment, ViewModel or LifecycleOwner page orchestration.
