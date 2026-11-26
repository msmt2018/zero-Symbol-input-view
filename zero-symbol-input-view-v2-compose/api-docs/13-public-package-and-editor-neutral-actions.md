# 13 — Public packages and editor-neutral document contract

This page describes the current source layout of the Compose artifact. The generic Drawer implementation has been physically moved to its public package. The symbol-input feature remains in its product namespace.

## 1. Current package map

| API area | Canonical package | Contract |
|---|---|---|
| Generic Compose Drawer | `com.zerostudio.idea.widget.drawer` | `DrawerContainer`, `DrawerContainerState` |
| Anchors, controller and state | `com.zerostudio.idea.widget.drawer.core` | UI-neutral state engine plus Compose state adapters |
| Navigation | `com.zerostudio.idea.widget.drawer.navigation` | Stable page IDs and Composable page host |
| Panels | `com.zerostudio.idea.widget.drawer.panel` | Composable panel slots |
| Configuration | `com.zerostudio.idea.widget.drawer.config` | Generic Drawer configuration |
| Style | `com.zerostudio.idea.widget.drawer.style` | Compose typography, colors, motion and shape tokens |
| Adapter | `com.zerostudio.idea.widget.drawer.adapter` | Symbol drawer geometry adapter |
| Extensions | `com.zerostudio.idea.widget.drawer.extension` and `editor.symbolinput.v2.extension` | Public aliases and feature registry |
| Editor-neutral actions | `com.zerostudio.idea.widget.drawer.action` | Generic text/action APIs |
| Symbol-input UI | `com.zerostudio.idea.widget.editor.symbolinput.v2` | `SymbolDrawer` and `SymbolDrawerState` |
| Symbol models | `com.zerostudio.idea.widget.editor.symbolinput.v2.model` | Groups, entries and typed actions |

The former implementation namespace `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` is no longer the canonical source package. New code must import from `com.zerostudio.idea.widget.drawer`.

## 2. Recommended imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawer
import com.zerostudio.idea.widget.editor.symbolinput.v2.rememberSymbolDrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
```

The Compose artifact does not expose `SymbolDrawerView`. Its symbol entry point is the `@Composable SymbolDrawer` function. The generic Drawer is also Compose-native in this artifact.

## 3. Editor-neutral text contract

The action engine works against the currently focused document, not a specific editor implementation. A host may implement `SymbolTextDocument` over a custom editor buffer, plain text input, terminal document, in-memory model or test fake.

- Offsets are UTF-16 code units, matching JVM `String` indexing.
- Line and column values are zero-based.
- Ranges are end-exclusive: `[start, end)`.
- `lineEndOffset` excludes CR/LF separators.
- Multi-range edits must validate non-overlap and apply from right to left.

## 4. Action execution

`SymbolActionExecutor` is synchronous and does not switch threads. The host must call it on the thread required by its editor. A host returning false means the capability is unsupported or rejected. Invalid input, cancellation, exceptions and partial Composite completion are represented separately by `SymbolActionResult`.

Composite actions are sequential, not automatically transactional. Atomic rollback is only available when the host document adapter implements the transaction hooks with real rollback semantics.

## 5. Composition boundary

Domain models, codecs, repositories and the core state engine remain independent of Compose UI. Compose-specific slots and state adapters are confined to UI integration packages. The artifact does not use Android View widgets or require Activity, Fragment, ViewModel or LifecycleOwner as a page-state contract. Compose still has Composition, recomposition and effect-cancellation semantics.
