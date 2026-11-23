# 14 — Drawer public API reference

The primary generic container is `DrawerContainer`. It accepts `DrawerConfig`, `Modifier`, `DrawerContainerState`, optional title/pages/selection callbacks, dismiss callback and a required Composable host content slot.

Use `rememberDrawerContainerState` for Composition-owned presentation state. `DrawerPage` and `DrawerPanelContent` describe Composable page/panel content. `DrawerPageHost` and `DrawerPanelHost` render those slots with Compose Foundation/Runtime.

Core geometry and state types are UI-neutral: `DrawerAnchor`, `DrawerAnchorResolver`, `DrawerStateHolder`, `DrawerController` and interaction/nested-scroll policies. They can be tested without rendering Compose UI.

The container does not install system Back callbacks. The host owns navigation and Insets policy. The Compose artifact does not expose legacy ViewGroup, Activity or Fragment APIs.