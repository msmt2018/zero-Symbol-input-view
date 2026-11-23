# 08 — Navigation, panels and style

`DrawerPage` contains a stable ID, non-empty title, `DrawerPanelContent` and optional Composable icon. Do not use the current list index as page identity.

`DrawerPageHost` renders pages with Compose Foundation `HorizontalPager`, keys pages by ID, synchronizes the external selected ID and reports settled user navigation. Compose owns page composition and disposal.

`DrawerPanelContent` is a Composable `ColumnScope` content slot. `DrawerPanelHost` renders it in a Compose Column; there is no View factory or attach/detach callback.

Style contracts live in `drawer.style`: `DrawerStyle`, `DrawerColors`, `DrawerDimensions`, `DrawerTypography`, `DrawerMotion`, `DrawerShape` and `DrawerThemeResolver`. They describe appearance and motion without owning Activity Context.

The host owns system Back dispatch. Compose effects are cancelled when their Composition leaves; this is not an Android View lifecycle.