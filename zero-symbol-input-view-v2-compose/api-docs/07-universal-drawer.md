# 07 — Compose 通用 Drawer 框架

包：com.zerostudio.idea.widget.editor.symbolinput.v2.drawer 及其子包。

## 1. 架构

通用 Drawer 是功能中立的 Compose 容器，可承载任意主内容、Peek 面板、Expanded 面板和页面导航。它与符号输入组件解耦。

    DrawerContainer (@Composable)
    ├── host content (@Composable BoxScope)
    ├── peek / expanded panel (DrawerPanelContent)
    ├── DrawerPageHost (HorizontalPager)
    └── DrawerContainerState
        └── DefaultDrawerController
            └── DrawerStateHolder
                ├── DrawerAnchorResolver
                └── interaction / nested-scroll policy

几何、锚点、状态转换和手势策略位于 UI 中立的 core 层；Compose 容器负责将状态映射到布局、拖动和动画。

## 2. DrawerContainer

主要参数：
- config：完整不可变 DrawerConfig。
- modifier：Compose Modifier。
- state：由 rememberDrawerContainerState 创建的状态。
- title：可选标题。
- pages：使用稳定 ID 的页面列表。
- selectedPageId：外部选择状态。
- onPageSelected：页面选择回调。
- onDismiss：关闭请求回调。
- content：抽屉后方主内容的 Composable lambda。

## 3. 状态控制

rememberDrawerContainerState(initiallyExpanded) 创建状态。状态支持 expand()、collapse()、toggle()；底层 controller 提供锚点与状态控制命令。状态对象不持有 Android View、Activity、Fragment、Context 或 LifecycleOwner。

不要在每次重组时重新构造状态；使用 rememberDrawerContainerState 或显式提供稳定 state。

## 4. 配置

DrawerConfig 包含：
- anchorStrategy：DrawerAnchorStrategy，用于解析 Peek / Half / Expanded 锚点。
- style：颜色、尺寸、字体、运动和形状。
- peekContent / expandedContent：可组合面板内容。
- enableDrag、enableNestedScroll、enableEdgeToEdge。
- dismissOnScrimTap、dismissOnBack、dragHandleEnabled、accessibilityEnabled。
- navigationConfig：导航位置、指示器和标题/图标显示。
- presentationMode：BOTTOM_SHEET 或 FULL_SCREEN。

配置为不可变数据类；优先构造完整快照后传入，不要在高频手势回调中反复创建。

## 5. 页面与面板

DrawerPage 包含稳定 id、title、DrawerPanelContent 和可选 Compose icon lambda。页面内容由 DrawerPageHost 使用 HorizontalPager 组合。

DrawerPanelContent 持有 @Composable ColumnScope.() -> Unit，不是 View 工厂。它没有 createView、attach/detach、View 缓存或手动 dispose 生命周期。

## 6. 锚点、拖动与嵌套滚动

锚点核心类型位于 drawer.core：
- DrawerAnchor / DrawerAnchorInput：稳定 ID、高度和几何输入。
- DrawerAnchorStrategy：纯计算策略。
- DrawerAnchorResolver：校验、clamp、排序、重复高度归并和 fling 目标。
- DrawerStateHolder：唯一状态源，负责拖动、动画代次和稳定锚点。
- DrawerInteractionController / DrawerNestedScrollConnection：输入仲裁与嵌套滚动策略。

核心 API 不应依赖 Compose UI 类型或传统 Android View 事件；Compose adapter 负责把 pointer / drag / nested-scroll 事件转换为核心命令。

## 7. Back、Insets 和宿主职责

容器不创建 Activity，也不接管应用导航栈。系统 Back 的最终消费权由宿主导航策略决定；onDismiss 是关闭请求，不代表自动退出 Activity。Insets 和主窗口尺寸由宿主 Compose 根布局配置。

## 8. Composition 资源语义

页面和面板通过 Composition 添加/移除。LaunchedEffect 等副作用由 Compose 按 key 启动与取消；这与传统 View attach/detach、Fragment View lifecycle 不同，但不等于没有任何生命周期。
