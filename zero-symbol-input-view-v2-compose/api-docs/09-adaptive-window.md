# 09 — Compose 自适应窗口 API

本模块的自适应层由不可变窗口数据、纯布局解析器和 Compose 测量适配器组成。它不监听传统 View 的 layout/attach/detach，也不要求宿主传入 Activity 或 LifecycleOwner。

## 1. WindowAdaptiveInfo

WindowAdaptiveInfo 是当前应用窗口的不可变快照：

- widthDp / heightDp：Compose 可用布局边界的宽高。
- density：当前 density，必须为有限正数。
- insets：WindowInsetsDp，包含 left/top/right/bottom。
- foldingFeature：折叠姿态、separating 状态和折叠边界。

availableWidthDp / availableHeightDp 会扣除快照中的 Insets。sizeClass 根据可用宽度计算：

- COMPACT：小于 600dp。
- MEDIUM：600dp 至小于 840dp。
- EXPANDED：不小于 840dp。

fromPixels 可将宿主提供的像素尺寸转换为 dp；density 必须大于 0。WindowPosture 包含 UNKNOWN、FLAT、HALF_OPENED。

## 2. AdaptiveWindowObserver

AdaptiveWindowObserver 是 Compose 测量入口。它使用 BoxWithConstraints 获取当前组合布局边界，结合 LocalDensity、LocalLayoutDirection 和 safeDrawing Insets，向 content 提供 WindowAdaptiveInfo。

参数：

- modifier：Compose Modifier。
- foldingFeature：宿主提供的 FoldingFeatureInfo；默认 UNKNOWN。
- insetsMode：SUBTRACT_SYSTEM_INSETS 或 VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS。
- content：接收 WindowAdaptiveInfo 的 Composable lambda。

VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS 表示调用方的布局边界已排除系统 Insets，观察器不再重复扣减。该枚举名称是兼容性标识，不代表观察器接收 Android View。

折叠屏特征需要由宿主/平台层采集后作为不可变数据传入；当前观察器不会自行注册 WindowManager folding feature listener。

## 3. AdaptiveWindowController

AdaptiveWindowController 是纯 Kotlin 快照发布器，不依赖 Compose Runtime；它保存最新 WindowAdaptiveInfo，并提供：

- snapshot：Compose snapshot 可观察属性。
- latest()：读取最新快照。
- update(value)：更新快照；相同值不重复通知。
- refresh()：重新广播当前快照。
- addListener(emitCurrent, listener)：添加监听并返回 AutoCloseable。
- rememberAdaptiveWindowController(initialSnapshot)：在当前 Composition 中记忆控制器实例；该函数位于 Compose 适配层。
- rememberAdaptiveWindowSnapshot(controller)：在 Composition 中订阅快照，并在 DisposableEffect 结束时关闭监听句柄。

监听通知会排队处理重入更新；单个 listener 抛异常不会阻断其它 listener。控制器不自行测量窗口，也不拥有窗口 listener。

## 4. AdaptiveLayoutResolver

AdaptiveLayoutResolver.resolve(info, type, policy) 返回 AdaptiveLayoutSpec，不创建 UI。

CanonicalLayoutType：

- LIST_DETAIL：宽窗口或 separating hinge 下使用 SIDE_BY_SIDE，否则 SINGLE。
- FEED：按 feedMinimumColumnWidthDp 和 feedMaximumColumns 计算网格列数。
- SUPPORTING_PANE：达到 supportingPaneBreakpointDp 或存在 separating hinge 时使用 MAIN_WITH_SUPPORTING，否则 STACKED。

AdaptiveLayoutSpec 包含 arrangement、主/次 Pane 权重、feedColumnCount 和 useHingeAsBoundary。最终如何排列内容由 Compose 容器决定。

## 5. 其它自适应 API

AdaptiveGridResolver、AdaptiveContentLayout、AdaptiveDimensions 和 SymbolDrawerDocking* 负责网格尺寸、内容布局及抽屉停靠策略。解析器应只返回可检查的规格/策略；Compose UI 根据结果决定 Modifier、Row/Column、网格列数和停靠位置。

## 6. 边界与验证

- 尺寸、density、Insets 和折叠边界必须是有限且非负数。
- 分屏、自由窗口和桌面模式以当前应用窗口尺寸为准，不以设备物理屏幕方向推断。
- 不要把同一 Insets 同时在宿主和 AdaptiveWindowObserver 中扣减。
- 不要把 View bounds、Context 或 Window 引用存入 WindowAdaptiveInfo。
- 纯解析器应通过 JVM 单元测试覆盖阈值、边界、hinge 和非法输入。
- Compose 测量行为应通过 Compose UI 测试验证。

Compose Composition 和 effect 仍有其自身的进入/离开语义；本 API 仅消除了传统 View attach/detach 与 Activity/Fragment 生命周期依赖。