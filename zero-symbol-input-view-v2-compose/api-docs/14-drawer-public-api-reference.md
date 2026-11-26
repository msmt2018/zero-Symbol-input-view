# 14 — Compose Drawer public API reference

This page documents the generic Drawer API in the `zero-symbol-input-view-v2-compose` artifact. It is not the Android View implementation shipped by the separate `zero-symbol-input-view-v2` artifact.

## 1. Canonical imports

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.rememberDrawerContainerState
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.drawer.panel.drawerPanelContent
```

The generic Drawer implementation physically resides in `com.zerostudio.idea.widget.drawer.*`. The symbol-input feature remains under `com.zerostudio.idea.widget.editor.symbolinput.v2.*`.

## 2. DrawerContainer

The current public Composable contract is:

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

- `config`: anchor strategy, presentation mode, panel behavior and visual style.
- `modifier`: caller-owned Compose layout modifiers.
- `state`: state object created by `rememberDrawerContainerState`.
- `title`: optional drawer title.
- `pages`: stable-ID navigation pages hosted in the expanded surface.
- `selectedPageId`: caller-selected page ID; null means no externally selected page.
- `onPageSelected`: receives the selected `DrawerPage`.
- `onDismiss`: called when the drawer requests dismissal.
- `content`: main host content as a `BoxScope` Composable slot.

The parent Compose layout constrains the drawer. It does not create an Activity, change the application Window, or require a View container.

## 3. State

`rememberDrawerContainerState(initiallyExpanded = false)` creates a Composition-remembered `DrawerContainerState`.

- `expanded`: whether the current state is expanded.
- `expand()`, `collapse()`, `toggle()`: request state transitions.
- `controller`: runtime controller for anchor/configuration and state operations.

Use one state instance for the drawer instance it owns. Do not mirror the same state in a second mutable controller.

## 4. Configuration and style

`DrawerConfig` contains:

- `anchorStrategy`
- `style`
- `peekContent` and `expandedContent`
- `enableDrag`, `enableNestedScroll`, `enableEdgeToEdge`
- `dismissOnScrimTap`, `dismissOnBack`
- `dragHandleEnabled`, `accessibilityEnabled`
- `navigationConfig`
- `presentationMode` (`BOTTOM_SHEET` or `FULL_SCREEN`)

`DrawerStyle` groups theme mode, color tokens, dimensions, typography, motion, shape, elevation and scrim/clipping behavior. Compose typography uses `FontWeight`; colors are ARGB integer tokens or null to inherit the active theme.

## 5. Pages and panel slots

`DrawerPage(id, title, content, icon)` requires a nonblank stable ID and title. The optional icon is a Composable lambda. The page host uses page IDs to map selection to pager positions.

`DrawerPanelContent` wraps `@Composable ColumnScope.() -> Unit`. Create one with `drawerPanelContent { ... }`. `DrawerPanelSlot` distinguishes `PEEK` and `EXPANDED`. These are Composable slots, not View factories; there are no attach/detach callbacks.

## 6. Core state and navigation helpers

The public core package exposes anchor input/models/strategies, `DrawerStateHolder`, `DrawerController`, nested-scroll policy types and Composition adapters:

- `rememberDrawerStateHolder(...)`
- `rememberDefaultDrawerController(...)`
- `rememberDrawerStateSnapshot(holder)`
- `rememberDrawerNavigationSnapshot(controller)`

The holder owns resolved anchors, continuous pixel height, drag/animation state and listener publication. The Compose host renders snapshots and owns animation/effect jobs. Keep anchor resolution and policy code free of UI rendering concerns.

## 7. Artifact separation

The legacy artifact exposes `DrawerContainer` as an Android View class and `DrawerPanelContent` as a View factory contract. This artifact exposes Composables with the same conceptual names. They are not binary/source interchangeable. Depend on one Drawer UI artifact only.
