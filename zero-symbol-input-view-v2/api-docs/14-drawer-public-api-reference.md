# 14 — 通用 Drawer 公共 API 参考（Android View）

本文对应 artifact：`zero-symbol-input-view-v2`。本模块的通用 Drawer 是传统 Android View 实现，不能与 Compose artifact 中同名 Composable 混用。

## 1. 公共包

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.drawer.config.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.*
import com.zerostudio.idea.widget.drawer.navigation.*
import com.zerostudio.idea.widget.drawer.panel.*
import com.zerostudio.idea.widget.drawer.style.*
```

通用 Drawer 的实现源码位于 `com.zerostudio.idea.widget.drawer.*`。符号输入产品仍位于 `com.zerostudio.idea.widget.editor.symbolinput.v2`。不要将 `model.DrawerConfig`（符号网格配置）与 `drawer.config.DrawerConfig`（通用 Drawer 配置）混用。

## 2. 组件关系

```text
Host Activity / Fragment / View
  └── com.zerostudio.idea.widget.drawer.DrawerContainer (FrameLayout)
       ├── main content
       ├── Peek panel
       ├── Expanded panel / DrawerPageHost
       ├── navigation tabs
       └── DrawerController
            ├── DrawerAnchorStrategy / DrawerAnchorResolver
            ├── DrawerInteractionController
            └── DrawerState snapshot
```

- `DrawerContainer` 是 ViewGroup / NestedScrollingParent3 容器，负责测量、布局、触摸和嵌套滚动。
- `DrawerController` 是运行时命令接口。
- `DrawerAnchorStrategy` 解析 Peek、Expanded 或自定义锚点。
- `DrawerInteractionController` 负责方向仲裁、拖动、fling 和 settle。
- `DrawerPageHost` 创建、缓存和切换页面 View。
- `DrawerPanelContent` 提供面板 View 工厂及 attach/visible/hidden/detach 通知。

## 3. 创建与设置主内容

```kotlin
val config = DrawerConfig.builder()
    .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
    .setDragEnabled(true)
    .setNestedScrollEnabled(true)
    .setDismissOnBack(true)
    .setDismissOnScrimTap(true)
    .build()

val drawer = DrawerContainer(context, config)
parent.addView(drawer)
drawer.setMainContent(editorView)
drawer.setTitle("Editor tools")
drawer.expand(animated = false)
```

`context` 应来自当前 Activity/Fragment 的主题上下文。主内容由 `setMainContent(view)` 设置；Peek 和 Expanded 内容分别通过 `setPeekContent`、`setExpandedContent` 设置。调用 `setPages(pages, preserveSelection)` 后，页面 host 接管 Expanded 区域。

## 4. 控制器

`drawer.controller` 暴露 `DrawerController`：

- `expand(animated)` / `collapse(animated)` / `moveToAnchor(anchorId, animated)`
- `refreshAnchors()`、`setAnchorStrategy(strategy)`、`setPresentationMode(mode)`
- `applyConfig(config)`
- `setStyle`、`setThemeMode`、`setColors`、`setDimensions`、`setTypography`、`setMotion`、`setShape`
- `setPeekContent` / `setExpandedContent`
- `setTitle`、`setPages`、`selectPage`、`setNavigationConfig`
- 拖动、嵌套滚动、Edge-to-edge、Scrim、Back、Handle 和无障碍开关
- `currentState()`、`addStateListener`、`removeStateListener`

状态快照 `DrawerState` 包含当前锚点 ID、高度、锚点列表、拖动/动画标志，以及稳定锚点和展开进度派生属性。

## 5. 配置、锚点和样式

`DrawerConfig` 是不可变配置，包含锚点策略、样式、Peek/Expanded 面板、拖动、嵌套滚动、Edge-to-edge、Scrim/Back、Handle、无障碍、导航配置和 `DrawerPresentationMode`。

`DrawerAnchorInput` 提供最小/最大高度、视口和内容高度、现有展开高度、density 与宽度。实现 `DrawerAnchorStrategy.resolve(input)` 可自定义锚点；内置策略包括：

- `ExistingHeightAnchorStrategy`：兼容现有高度的双锚点策略。
- `ThreeStageAnchorStrategy`：Peek / Half / Expanded 三阶段。
- `ContentBasedAnchorStrategy`：按内容高度计算。
- `CustomAnchorStrategy`：使用调用方提供的锚点。

`DrawerStyle` 聚合 `DrawerColors`、`DrawerDimensions`、`DrawerTypography`、`DrawerMotion`、`DrawerShape` 和主题模式。空颜色角色可继续继承宿主主题。

## 6. 页面、面板与导航

`DrawerPage` 使用稳定唯一 ID、标题、`DrawerPanelContent` 和可选 Drawable 图标。页面 ID 不应使用列表索引。导航配置控制位置、指示器、标题/图标显示和滚动行为。

`DrawerPanelContent` 是 View 内容契约，提供 `createView()` / `createView(context)` 及 panel attached/visible/hidden/detached 回调。它不是 Compose lambda。

## 7. Back、状态保存与 View 生命周期

`DrawerContainer` 通过 Android View 状态保存机制保存当前锚点/高度/页面选择，并在 attach/detach 时管理交互资源。宿主应遵循 Fragment View 生命周期，避免在 View 销毁后继续持有容器或监听器。Back 消费由 `handleBack()` 和配置共同决定，宿主负责接入 Activity/Fragment 的 Back 分发。

## 8. 与 Compose artifact 的区别

Compose artifact 使用同一公共包名，但 `DrawerContainer` 是 `@Composable` 函数，`DrawerPanelContent` 是 Composable slot，`DrawerPageHost` 是 Composable 页面 host。两种 artifact 是互斥的 UI 实现，不要在同一依赖图中同时引入，也不要把 View API 签名用于 Compose 模块。
