# 02 — SymbolDrawer Compose API

包：com.zerostudio.idea.widget.editor.symbolinput.v2

## 1. 组件职责

SymbolDrawer 是符号输入抽屉的 Jetpack Compose 入口。它使用 Foundation 的分页、拖动、布局与 Material 3 表面，不创建或包装 Android View。

主要参数：
- groups：不可变分组快照。
- modifier：Compose Modifier。
- state：SymbolDrawerState，默认由 rememberSymbolDrawerState 创建。
- settings：符号抽屉设置快照。
- config：由设置映射出的布局和手势配置；显式传入时以调用方配置为准。
- onSymbolClick：符号短按回调。
- onSymbolLongClick：可选长按回调。
- onOpenManager / onOpenSettings：可选导航回调。

## 2. 状态

rememberSymbolDrawerState(initialPage, initiallyExpanded) 在当前 Composition 中创建 SymbolDrawerState。

- selectedPage：当前分组页索引。
- expanded：当前展开目标/状态。
- expand() / collapse() / toggle()：请求展开、折叠或切换。
- selectPage(index)：选择非负页索引。

State 只持有展示状态和抽屉状态机；不持有 View、Context、Activity、Fragment、Adapter 或 Lifecycle 对象。不要把同一个 State 实例跨无关编辑器会话共享。

## 3. 输入数据和回调

回调由宿主定义，不意味着编辑器文本已经成功修改。动作执行结果应由宿主的 SymbolActionExecutor / SymbolActionHost 路径确认。groups 更新通过提交新的数据快照触发重组；不要在 Composable 函数体中执行阻塞磁盘 I/O。

## 4. 展开状态与手势

内部共享通用 Drawer 的锚点状态机和交互策略。纵向拖动、fling、settle 与动画由 Compose gesture / effect / animation 机制协调；不暴露传统 onAttachedToWindow、onDetachedFromWindow 或 View 状态保存回调。

## 5. 宿主布局

组件尺寸、窗口 Insets 和编辑器整体布局由宿主 Compose 树决定。不要使用 XML、自定义 ViewGroup、RecyclerView、ViewPager2 或 AndroidView 作为适配层。

## 6. Composition 与资源语义

本组件没有 Activity / Fragment View 生命周期回调，也不要求 LifecycleOwner。Compose 仍会管理 Composition、重组、LaunchedEffect 的启动和取消；页面离开 Composition 时，相关 effect 按 key 和作用域规则取消。
