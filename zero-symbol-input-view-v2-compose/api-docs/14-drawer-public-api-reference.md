# 14 — Compose Drawer 公共 API 参考

本篇说明 Compose 通用抽屉 API 的公开入口和稳定性边界。

## 1. 导入

    import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.DrawerContainer
    import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.rememberDrawerContainerState
    import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
    import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
    import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.drawerPanelContent

通用 Drawer 与符号输入组件分别位于 drawer 与 model / 根 UI 包。不要将符号输入的 model.DrawerConfig 与通用抽屉的 drawer.config.DrawerConfig 混用。

## 2. 核心入口

DrawerContainer 是唯一的 Compose 容器入口；DrawerContainerState 是 Composition 记忆的状态对象；DrawerPage 和 DrawerPanelContent 表达页面/面板内容。它们都不接收 Context、View、Activity、Fragment 或 LifecycleOwner。

## 3. 状态与控制

rememberDrawerContainerState(initiallyExpanded) 创建状态。常用命令：
- expand() / collapse() / toggle()：请求切换稳定锚点。
- controller：底层控制器入口，可设置锚点策略、刷新锚点或读取状态快照。
- stateHolder：内部唯一状态引擎，不应由 UI 层另建第二份并行状态。

状态变化以 Compose snapshot 驱动重组；不要另加 Activity / Fragment listener 来同步同一份状态。

## 4. 配置和呈现

DrawerConfig 提供锚点策略、DrawerStyle、Peek / Expanded 内容、拖动/嵌套滚动/Insets/关闭策略、导航配置和呈现模式。

BOTTOM_SHEET 使用浮层视觉；FULL_SCREEN 使用可用 Compose 容器区域。二者都受父 Compose 布局约束，不会自行扩展应用 Window。

## 5. 页面导航

DrawerPage 使用稳定且非空的 ID。外部通过 selectedPageId 和 onPageSelected 同步选择；DrawerPageHost 内部以页面 ID 作为 pager key，避免仅依赖列表索引造成错位。

## 6. 面板

DrawerPanelContent 是 Composable lambda 包装，不是 Android View 工厂。面板在其 Compose slot 中组合；不提供传统 View 生命周期回调。

## 7. API 稳定性和兼容边界

Compose 模块与旧 View 模块是不同构建产物。旧 View 模块的 Activity / Fragment / XML / View API 不属于本 Compose API。Kotlin facade 中若存在 typealias，它只提供 Kotlin 源码级别别名，不会自动生成 Java 可见的独立类；Java ABI 兼容必须通过真实 facade class 单独实现并测试。

## 8. 生命周期说明

本模块不把 Android Lifecycle、LifecycleOwner、ViewModel 或 Fragment 生命周期作为页面状态模型。Compose 自身仍具有 Composition、重组和 effect 取消语义。不要将 Compose-native / 不依赖传统 View 生命周期表述为绝对零生命周期。
