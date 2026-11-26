# 14 — Generic Drawer public API (Android View)

This page describes the generic Drawer in the `zero-symbol-input-view-v2` artifact. It is a traditional Android View implementation and is not interchangeable with the same-concept Compose API in the separate Compose artifact.

## Imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.*
import com.zerostudio.idea.widget.drawer.navigation.*
import com.zerostudio.idea.widget.drawer.panel.*
import com.zerostudio.idea.widget.drawer.style.*
```

## Component model

```text
DrawerContainer (FrameLayout / NestedScrollingParent3)
├── main content
├── Peek panel
├── Expanded panel / DrawerPageHost
├── navigation tabs
└── DrawerController
    ├── anchor strategy and resolver
    ├── touch/drag/fling interaction controller
    └── DrawerState snapshot
```

`DrawerContainer(context, config)` is added to a ViewGroup. Set host content with `setMainContent(view)`, optional title with `setTitle(value)`, and Peek/Expanded panels with `setPeekContent` / `setExpandedContent`. `setPages(pages, preserveSelection)` delegates the expanded area to the page host.

## Controller surface

`drawer.controller` exposes expand/collapse/move-to-anchor, anchor refresh and strategy replacement, presentation mode and full config updates, style/theme setters, panel setters, page selection, navigation config, drag/nested-scroll/edge-to-edge/dismiss/handle/accessibility switches, current state and state listeners.

`DrawerState` is a snapshot containing current anchor ID, current pixel height, resolved anchors, drag/animation flags and derived stable-anchor/expansion properties.

## Configuration and policies

`drawer.config.DrawerConfig` is the generic container config; it is distinct from `editor.symbolinput.v2.model.DrawerConfig`, which configures the symbol grid. The generic config combines anchor strategy, style, panel content, gesture and dismissal flags, navigation config and presentation mode.

Built-in anchor strategies include `ExistingHeightAnchorStrategy`, `ThreeStageAnchorStrategy`, `ContentBasedAnchorStrategy` and `CustomAnchorStrategy`. Custom strategies implement `DrawerAnchorStrategy.resolve(input)`.

## Navigation and panels

`DrawerPage` has a stable ID, title, `DrawerPanelContent` and optional Android Drawable icon. `DrawerPanelContent` is a View factory with panel attached/visible/hidden/detached callbacks. It is not a Composable slot.

## Android lifecycle and state

The container uses Android View attach/detach and saved-state mechanisms to manage gesture resources and restore drawer/page state. The host must release references with its Fragment View or Activity lifecycle and connect Back handling through `handleBack()` as appropriate.

## Compose artifact distinction

In `zero-symbol-input-view-v2-compose`, `DrawerContainer` is a Composable function, `DrawerPanelContent` wraps a Composable lambda, and `DrawerPageHost` is a Composable. The two artifacts are separate implementations; do not put both on the same dependency graph.
