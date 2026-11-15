# 14 — Universal Drawer Public API Reference

## Imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.DrawerConfig
import com.zerostudio.idea.widget.drawer.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.*
import com.zerostudio.idea.widget.drawer.navigation.*
import com.zerostudio.idea.widget.drawer.panel.*
import com.zerostudio.idea.widget.drawer.style.*
```

Some entries are Kotlin typealiases; see [13](13-public-package-and-editor-neutral-actions.md) for Java ABI limitations.

## Object model

`DrawerContainer` owns layout, main content and panels. `DrawerController` is the runtime command surface. `DrawerAnchorStrategy` computes anchors. `DrawerInteractionController` handles gestures and settling. `DrawerPageHost` owns page View creation/cache/lifecycle. `DrawerPanelContent` defines panel content/lifecycle. `DrawerStyle` aggregates appearance tokens.

## Create a container

```kotlin
val config = DrawerConfig.builder()
    .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
    .setDragEnabled(true)
    .setNestedScrollEnabled(true)
    .setDismissOnBack(true)
    .setDismissOnScrimTap(true)
    .setDragHandleEnabled(true)
    .build()

val drawer = DrawerContainer(context, config)
parent.addView(drawer)
drawer.setMainContent(editorView)
drawer.setTitle("Tools")
drawer.expand(animated = false)
```

Use the current themed Activity Context. The container does not create an Activity or automatically own every host-window Insets policy.

## Container and controller

The container supports main content, title, Peek/Expanded content, page lists, expand/collapse, anchor refresh and Back handling. The controller supports anchor movement, strategy and presentation changes, config/style updates, page selection, drag/nested-scroll/edge-to-edge/scrim/Back/handle switches and state observation.

Apply a complete config snapshot atomically rather than repeatedly changing it during animation.

## Anchor strategies

Available strategies include existing-height compatibility, three-stage, content-based and custom. The resolver normalizes anchor definitions, chooses the nearest anchor and computes fling targets. IDs must be stable and unique.

## Navigation, panels and style

Pages use stable IDs. The page host caches Views and sends visibility/detach/dispose lifecycle events. A hidden page may remain cached. Style consists of colors, dimensions, typography, motion and shape, plus theme resolution and presets.

## Integration boundaries

The host remains responsible for window layout, app navigation and platform-specific Insets not explicitly handled by the container. Refresh anchors after content/window measurement changes. Test nested scrolling, horizontal children, Back, scrim, detach and Context recreation on device.
