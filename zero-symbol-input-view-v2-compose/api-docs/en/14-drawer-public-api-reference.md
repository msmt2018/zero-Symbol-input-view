# 14 — Compose Drawer public API reference

This page documents the generic Drawer in the `zero-symbol-input-view-v2-compose` artifact. It is not the Android View implementation from `zero-symbol-input-view-v2`.

## Canonical imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
```

## DrawerContainer

```kotlin
@Composable
fun DrawerContainer(
    config: DrawerConfig = DrawerConfig(),
    modifier: Modifier = Modifier,
    state: DrawerContainerState = rememberDrawerContainerState(),
    title: String? = null,
    pages: List<DrawerPage> = emptyList(),
    selectedPageId: String? = null,
    onPageSelected: (DrawerPage) -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
)
```

The main content is a `BoxScope` slot. Pages are selected by stable ID. The parent Compose layout constrains the drawer; the component does not create an Activity, resize the application Window or require a View container.

## State

`rememberDrawerContainerState(initiallyExpanded = false)` returns `DrawerContainerState`. It exposes `expanded`, `expand()`, `collapse()`, `toggle()` and a runtime controller. Keep one remembered state object for the drawer instance it owns.

## Config and style

`DrawerConfig` combines anchor strategy, `DrawerStyle`, Peek/Expanded Composable panel content, drag/nested-scroll/edge-to-edge options, scrim/back dismissal, handle/accessibility options, navigation config and `BOTTOM_SHEET` / `FULL_SCREEN` presentation mode.

Compose `DrawerStyle` uses Compose `FontWeight` for typography and color tokens that may inherit from the active Material theme.

## Pages and panel slots

`DrawerPage(id, title, content, icon)` requires nonblank ID and title. Its optional icon is a Composable lambda. `DrawerPanelContent` wraps `@Composable ColumnScope.() -> Unit`; create it with `drawerPanelContent { ... }`. These are Composition slots, not View factories, and have no attach/detach callbacks.

## Core adapters

The core package provides anchor models/strategies, `DrawerStateHolder`, `DrawerController`, nested-scroll policy types and Composition adapters: `rememberDrawerStateHolder`, `rememberDefaultDrawerController`, `rememberDrawerStateSnapshot` and `rememberDrawerNavigationSnapshot`.

The holder owns resolved anchors, continuous pixel height and drag/animation state. Compose renders snapshots and owns animation/effect jobs.

## Artifact distinction

The legacy artifact exposes `DrawerContainer` as a View class and `DrawerPanelContent` as a View factory. This artifact exposes Composables with the same conceptual names. They are not binary/source interchangeable; depend on only one Drawer UI artifact.
