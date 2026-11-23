# 07 — Universal Compose drawer

`DrawerContainer` is the reusable Compose container. Its host content, page content and peek/expanded panel content are Composable lambdas. Drawer geometry and interaction decisions are implemented in UI-neutral core classes; Compose applies them with drag, nested-scroll and animation modifiers.

## State

Use `rememberDrawerContainerState()` to create Composition-owned presentation state. It exposes expand/collapse/toggle behavior through the container state contract. Core anchor and interaction classes can also be used independently of UI.

## Pages and panels

- `DrawerPage` uses a stable string ID and a Composable icon slot.
- `DrawerPageHost` uses Compose Foundation `HorizontalPager`.
- `DrawerPanelContent` wraps a `@Composable ColumnScope.() -> Unit`.
- `DrawerPanelHost` composes the selected panel slot.

The container does not install Activity BackHandler. The host owns system Back policy and calls the state/controller accordingly. Insets handling must be coordinated with the host layout.