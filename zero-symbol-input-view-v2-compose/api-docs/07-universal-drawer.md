# 07 — Compose 通用 Drawer 框架

包：`com.zerostudio.idea.widget.drawer` 及其子包。

## 1. 架构

通用 Drawer 是功能中立的 Compose 容器，可承载主内容、Peek/Expanded 面板和页面导航。它与符号输入组件解耦。

```text
DrawerContainer (@Composable)
├── host content (BoxScope slot)
├── peek / expanded panel (DrawerPanelContent)
├── DrawerPageHost (Compose HorizontalPager)
└── DrawerContainerState
    └── DefaultDrawerController
        └── DrawerStateHolder
            ├── DrawerAnchorResolver
            └── drag / nested-scroll policy
```

锚点解析、几何计算、状态转换和嵌套滚动策略位于 UI-neutral core。Compose state adapters 将状态快照桥接到 Composition；容器、动画、拖动和 nested-scroll connection 属于 Compose 适配层。

## 2. DrawerContainer

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

主内容由 `content` 提供。父 Compose 布局约束组件尺寸；容器不创建 Activity、不调整应用 Window，也不要求 View 容器。

## 3. 状态

`rememberDrawerContainerState(initiallyExpanded)` 创建 Composition 记忆状态。状态支持 `expanded`、`expand()`、`collapse()`、`toggle()` 和底层 `controller`。在同一个 Drawer 会话内保持一个稳定 state，不要另建平行状态机。

## 4. 配置

`DrawerConfig` 包含：

- `anchorStrategy`、`style`。
- `peekContent`、`expandedContent`。
- `enableDrag`、`enableNestedScroll`、`enableEdgeToEdge`。
- `dismissOnScrimTap`、`dismissOnBack`。
- `dragHandleEnabled`、`accessibilityEnabled`。
- `navigationConfig`、`presentationMode`（`BOTTOM_SHEET` / `FULL_SCREEN`）。

配置为不可变快照。高频手势期间不要重复构建配置。

## 5. 页面与面板

`DrawerPage(id, title, content, icon)` 要求非空稳定 ID 和标题；icon 是可选 Composable lambda。页面由 `DrawerPageHost` 以 Compose Pager 呈现。

`DrawerPanelContent` 包装 `@Composable ColumnScope.() -> Unit`，可通过 `drawerPanelContent { ... }` 创建。`DrawerPanelSlot` 区分 `PEEK` 与 `EXPANDED`。这里没有 View factory 或 attach/detach 回调。

## 6. Core API

- `DrawerAnchor`、`DrawerAnchorInput`、`DrawerAnchorStrategy`、`DrawerAnchorResolver`：锚点模型与纯计算策略。
- `DrawerStateHolder`：连续高度、拖动、动画 generation 和状态监听的唯一引擎。
- `DrawerController` / `DefaultDrawerController`：状态命令和配置控制。
- `DrawerNestedScrollPolicy`：纯 Kotlin PRE/POST nested-scroll 仲裁。
- `rememberDrawerStateHolder`、`rememberDefaultDrawerController`、`rememberDrawerStateSnapshot`：Composition adapters。
- `DrawerNestedScrollConnection`：Compose NestedScrollConnection adapter。

不要将 Composable slots 引入锚点解析或领域状态层。

## 7. 宿主职责与 Composition

容器不安装 Activity BackHandler；宿主负责系统 Back policy 和 Window Insets 协调。页面和面板由 Composition 添加/移除；LaunchedEffect 等副作用按 key 启动/取消。Compose 有 Composition 与 effect 生命周期，不应称为完全没有生命周期。
