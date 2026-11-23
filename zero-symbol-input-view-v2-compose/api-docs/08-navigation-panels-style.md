# 08 — 页面导航、面板与样式 API

本页只描述 Compose 模块当前实现。页面内容是 Composable slot，不创建 View，不维护 View attach/detach 回调。

## 1. DrawerPage

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation`

```kotlin
data class DrawerPage(
    val id: String,
    val title: String,
    val content: DrawerPanelContent,
    val icon: (@Composable () -> Unit)? = null
)
```

- `id` 必须非空且稳定；用于 pager key 和选择同步，不要用 index 作为身份。
- `title` 必须非空。
- `content` 是 Compose 面板内容。
- `icon` 是可选 Composable slot，不是 Drawable 或 View。

## 2. DrawerNavigationController

这是 UI 中立的页面选择状态模型。它负责页面 ID、顺序和选中项，不创建页面、不保存 Activity/Context，也不接管 Composition。

核心操作：
- `submitPages(pages, preserveSelection)`：校验 ID 唯一并提交页面集合。
- `selectPage(id)` / `selectPage(index)`：按稳定 ID 或有效索引选择。
- `pages()`、`selectedPage()`、`selectedIndex()`：读取当前快照。
- `snapshot()`：取得 `DrawerNavigationSnapshot(selectedPageId, pageIds)`。

## 3. DrawerPageHost

`DrawerPageHost(pages, selectedPageId, onPageSelected, modifier)` 使用 Compose Foundation `HorizontalPager` 呈现页面。它以 page ID 作为 key，并将外部选择同步到 pager；只在 pager settled 后回传用户切页结果。

Pager 的页面组合、离屏页面处置与状态作用域由 Compose 管理。不要再套一层 ViewPager2，也不要在宿主保存第二份 index 真值。

## 4. DrawerPanelContent / DrawerPanelHost

包：`drawer.panel`

```kotlin
data class DrawerPanelContent(
    val content: @Composable ColumnScope.() -> Unit
)
enum class DrawerPanelSlot { PEEK, EXPANDED }
```

使用 `drawerPanelContent { ... }` 创建 slot。 `DrawerPanelHost(content, modifier, slot)` 将内容放入 Compose Column；当前 host 不定义 View factory、attach/detach 回调或传统页面生命周期。

## 5. DrawerContainer 集成

`DrawerContainer` 提供抽屉容器、锚点状态和内容 slot。页面导航由调用方传入 `pages`、`selectedPageId` 和 `onPageSelected`。Back 导航/关闭由宿主导航策略处理；不要假定容器内部安装 Activity BackHandler。

## 6. 样式与主题

样式模型位于 `drawer.style`，包括 `DrawerStyle`、`DrawerColors`、`DrawerDimensions`、`DrawerTypography`、`DrawerMotion`、`DrawerShape` 和 `DrawerThemeResolver`。这些类型表达外观参数，不创建 Android View 或包装 Activity Context。

## 7. Compose 状态边界

- 可保存的业务状态应放在 UI 中立模型/Repository 中。
- 短期交互状态可由 Compose state 承载。
- `LaunchedEffect` 等 effect 只在对应 Composition 存在期间运行，离开 Composition 时会取消；这不是 Android View 生命周期。
- 不要将“Compose-native”误称为字面上的“没有任何生命周期”。