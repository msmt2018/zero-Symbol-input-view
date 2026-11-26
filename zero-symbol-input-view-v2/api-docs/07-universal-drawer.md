# 07 — 通用 Drawer 框架 API（Android View）

包：`com.zerostudio.idea.widget.drawer` 及其子包。

## 1. 与 SymbolDrawerView 的关系

`DrawerContainer` 是独立、功能中立的 Android View 抽屉容器，面向任意宿主内容和页面；支持 BottomSheet/FullScreen 呈现、锚点、拖动、嵌套滚动、面板、导航和样式。

`SymbolDrawerView` 是专用符号工具栏，仍使用自己的 ViewGroup、ViewPager2 和 SymbolPageAdapter。它与通用 Drawer 是不同组件，不应把符号控件再包进通用 Drawer。符号模型的 `model.DrawerConfig` 与通用配置 `drawer.config.DrawerConfig` 也不是同一类型。

## 2. DrawerContainer

```kotlin
val config = DrawerConfig.builder()
    .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
    .setDragEnabled(true)
    .setNestedScrollEnabled(true)
    .setDismissOnBack(true)
    .build()

val drawer = DrawerContainer(context, config)
parent.addView(drawer)
drawer.setMainContent(editorView)
drawer.setTitle("Tools")
drawer.expand()
```

`DrawerContainer` 继承 `FrameLayout` 并实现 `NestedScrollingParent3`。主要属性：`controller: DrawerController`、`navigationHost: DrawerPageHost`。

主要方法：

- `setMainContent(view)`：设置抽屉后方主内容。
- `setTitle(title)`：设置/隐藏标题并刷新锚点。
- `setPeekContent(content)` / `setExpandedContent(content)`：设置两个面板。
- `setPages(pages, preserveSelection)`：切换到页面 host；页面 ID 必须唯一。
- `expand(animated)` / `collapse(animated)`：移动到最高/最低锚点。
- `refreshAnchors()`：内容或窗口尺寸变化后重新测量。
- `handleBack()`：当前配置允许收起并已处理时返回 true。

## 3. DrawerController

通过 `drawer.controller` 获取运行时控制接口。包括：

- 锚点与呈现：`expand`、`collapse`、`moveToAnchor`、`refreshAnchors`、`setAnchorStrategy`、`setPresentationMode`。
- 配置：`applyConfig`。
- 样式：`setStyle`、`setThemeMode`、`setColors`、`setDimensions`、`setTypography`、`setMotion`、`setShape`。
- 内容与导航：`setPeekContent`、`setExpandedContent`、`setTitle`、`setPages`、`selectPage`、`setNavigationConfig`。
- 行为开关：拖动、嵌套滚动、Edge-to-edge、Scrim/Back dismiss、Handle 和无障碍。
- 状态：`currentState`、`addStateListener`、`removeStateListener`。

页面通过稳定 ID 选择；未知 ID 的 `selectPage` 返回 false。监听器应在所属宿主 View/Session 结束时移除。

## 4. DrawerConfig 与锚点

`drawer.config.DrawerConfig` 是不可变配置，包含锚点策略、样式、Peek/Expanded 内容、交互开关、导航配置和呈现模式。Builder 提供相同配置域的链式设置方法。

`DrawerAnchorInput` 提供 min/max/viewport/content 高度、现有展开高度、density 和宽度。通过 `DrawerAnchorStrategy.resolve(input)` 自定义锚点。内置策略：

- `ExistingHeightAnchorStrategy`
- `ThreeStageAnchorStrategy`
- `ContentBasedAnchorStrategy`
- `CustomAnchorStrategy`

`DrawerAnchorResolver` 负责解析、归一化、最近锚点和 fling 目标。

## 5. 页面、面板和样式

`DrawerPage` 包含稳定 ID、标题、`DrawerPanelContent` 和可选 Drawable 图标。页面 host 管理 View 页面缓存和切换。

`DrawerPanelContent` 是 Android View 工厂契约，支持 createView 与 attached/visible/hidden/detached 回调。它不是 Composable lambda。

`DrawerStyle` 聚合主题模式、颜色角色、尺寸、字体、运动、形状、elevation、scrim 和裁剪配置。

## 6. 生命周期和宿主职责

本 artifact 的容器、页面和面板属于 Android View 层。宿主必须遵循 Activity/Fragment View 生命周期，释放监听器和 View 引用；Back 分发、Window 和未由容器处理的 Insets 仍由宿主管理。
