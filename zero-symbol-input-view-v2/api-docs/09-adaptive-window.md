# 09 — 自适应窗口与大屏布局 API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive`

## 1. 设计原则

所有布局决策都基于当前应用窗口的可用尺寸，而不是设备型号或物理屏幕方向。这样可以正确响应分屏、多窗口、自由窗口、桌面模式和折叠设备。

解析器只返回不可变规格；宿主负责把规格应用到自己的 View hierarchy。自适应 API 不会擅自替换宿主的 Insets listener 或重新组织 Activity 布局。

## 2. WindowAdaptiveInfo

```kotlin
data class WindowAdaptiveInfo(
    val widthDp: Float,
    val heightDp: Float,
    val density: Float = 1f,
    val insets: WindowInsetsDp = WindowInsetsDp(),
    val foldingFeature: FoldingFeatureInfo = FoldingFeatureInfo()
)
```

- `widthDp` / `heightDp`：当前 host window 的宽高。
- `density`：像素到 dp 转换系数。
- `insets`：系统栏/显示切口等需从可用尺寸扣除的 Insets。
- `foldingFeature`：折叠状态及折叠区域边界。

派生属性：
- `availableWidthDp` = width - left - right，最小为 0。
- `availableHeightDp` = height - top - bottom，最小为 0。
- `sizeClass`：按 availableWidthDp 判断。

断点常量：
- `MEDIUM_WIDTH_DP = 600f`
- `EXPANDED_WIDTH_DP = 840f`

分类：小于 600dp 为 COMPACT；600..839.999dp 为 MEDIUM；大于等于 840dp 为 EXPANDED。

### fromPixels(...)

```kotlin
WindowAdaptiveInfo.fromPixels(
    widthPx,
    heightPx,
    density,
    insets = WindowInsetsDp(),
    foldingFeature = FoldingFeatureInfo()
)
```

把像素宽高转换为 dp；负宽高 clamp 为 0，density 必须大于 0。

## 3. WindowSizeClass / WindowPosture

- `WindowSizeClass`: COMPACT / MEDIUM / EXPANDED。
- `WindowPosture`: UNKNOWN / FLAT / HALF_OPENED。

size class 描述窗口宽度级别；posture 描述折叠设备姿态，二者不可互相替代。

## 4. WindowInsetsDp

包含 left/top/right/bottom dp 值，默认 0。构造值不能为负数。

## 5. FoldingFeatureInfo

字段：
- `posture`
- `isSeparating`
- `boundsLeftDp` / `boundsTopDp` / `boundsRightDp` / `boundsBottomDp`

`hasBounds` 在边界宽高均大于 0 时为 true。边界需非负且 right >= left、bottom >= top。

该类型不直接依赖 AndroidX WindowManager；宿主可将 WindowManager FoldingFeature 转换成此快照。

## 6. AdaptiveWindowObserver

构造参数：
- `view: View`：用于读取当前窗口内容 View 的尺寸和 attach 状态。
- `onChanged: (WindowAdaptiveInfo) -> Unit`：尺寸/Insets/折叠信息改变时通知。
- `insetsMode: WindowInsetsMode`：是否扣除系统 Insets。

观察者监听 View layout 和 attach/detach：
- 仅在 View 已 attach 且宽高大于 0 时发布快照。
- 可选读取 root WindowInsets 的 systemBars/displayCutout。
- 相同快照默认不重复派发。
- detach 时清除上次快照。
- `updateFoldingFeature(value)` 更新折叠状态。
- `refresh()` 强制重新派发当前测量快照。
- `close()` 解除 View listeners，幂等。

### WindowInsetsMode

- `INCLUDE_SYSTEM_INSETS`：窗口尺寸中保留 Insets。
- `SUBTRACT_SYSTEM_INSETS`：从可用尺寸扣除系统栏和 display cutout Insets。

## 7. AdaptiveWindowController

```kotlin
AdaptiveWindowController(
    hostView: View,
    insetsMode: WindowInsetsMode = ...
) : AutoCloseable
```

比 Observer 增加多订阅者分发和错误诊断。

属性：
- `current: WindowAdaptiveInfo?`：最近快照。
- `onListenerError: ((Exception) -> Unit)?`：订阅者抛出 Exception 时的可选通知。
- `lastListenerFailure: Exception?`：最近一次订阅者失败。

方法：
- `addListener(listener, emitCurrent = true): AutoCloseable`：添加订阅；若当前已有快照且 emitCurrent 为 true，立即回调一次。返回句柄用于取消订阅。
- `updateFoldingFeature(value)`：更新折叠信息；已 close 时抛 IllegalStateException。
- `refresh()`：强制重新测量；已 close 时抛 IllegalStateException。
- `close()`：清空监听、关闭 Observer；幂等。

订阅者抛出的 Exception 不会从 View layout callback 继续向外传播；控制器记录错误并调用 onListenerError。监听回调默认发生在 View 事件/布局触发的线程（通常主线程）。

## 8. AdaptiveContentLayoutResolver

### AdaptiveContentLayoutPolicy

定义三种窗口级别的内容最大宽度、padding、dialog width fraction 和双列断点。

默认：
- compact max width 560dp，padding 12dp，dialog fraction 0.96。
- medium max width 840dp，padding 24dp，dialog fraction 0.88。
- expanded max width 1040dp，padding 32dp，dialog fraction 0.72。
- 双列断点 840dp。

### AdaptiveContentLayoutSpec

解析输出：
- `maxContentWidthDp`
- `horizontalPaddingDp`
- `verticalSpacingDp`
- `cardColumns`
- `dialogWidthFraction`

### resolve(window, policy)

根据 sizeClass 返回内容规格。Compact/Medium 单列；Expanded 在达到双列断点时返回两列。

### resolveDialogWidthPx(windowWidthPx, density, window, policy)

计算 Dialog 宽度，取窗口宽度、可用宽度乘比例、最大内容宽度三者的最小值。windowWidthPx 必须非负、density 大于 0。

## 9. AdaptiveLayoutResolver

适用于列表详情、Feed 和 Supporting Pane 等通用 canonical layout。

### CanonicalLayoutType

- LIST_DETAIL
- FEED
- SUPPORTING_PANE

### PaneArrangement

- SINGLE
- SIDE_BY_SIDE
- MAIN_WITH_SUPPORTING
- FEED_GRID
- STACKED

### AdaptiveLayoutPolicy

可配置列表详情断点、Supporting Pane 断点、Feed 最小列宽、最大列数和主/次 Pane 权重。

### AdaptiveLayoutSpec

输出布局类型、排列方式、主次 Pane 权重、Feed 列数和是否把分隔铰链作为布局边界。

### resolve(info, type, policy)

- LIST_DETAIL：窗口宽度达到阈值或存在 separating hinge 时使用 SIDE_BY_SIDE，否则 SINGLE。
- FEED：根据 availableWidth / feedMinimumColumnWidth 计算列数，并 clamp 到 1..feedMaximumColumns。
- SUPPORTING_PANE：达到阈值或有 separating hinge 时显示 MAIN_WITH_SUPPORTING，否则 STACKED。

该 API 只计算结果，不创建 Pane View。

## 10. AdaptiveGridResolver

### AdaptiveGridSpec

`columnCount`、`horizontalSpacingDp`、`verticalSpacingDp`。列数至少 1，间距范围 -24..48。

### resolve(...)

参数：
- `window`
- `preferredColumns`
- `minimumCellWidthDp`
- `baseHorizontalSpacingDp`
- `adaptive = true`
- `baseVerticalSpacingDp = baseHorizontalSpacingDp`
- `lockColumnCount = false`

行为：
- adaptive=false：保留首选列数，间距 clamp。
- adaptive=true：按 COMPACT/MEDIUM/EXPANDED 对间距增加 0/1/3dp。
- 未锁列数时按可用宽度和最小单元格宽度计算列数，范围 1..preferredColumns。
- lockColumnCount=true 时保留 preferredColumns。

## 11. AdaptiveDimensions

### AdaptiveDimensionPolicy

设置 compact/medium/expanded scale、最小触控尺寸（默认至少 48dp）和最大缩放尺寸（默认 160dp）。

### AdaptiveDimensions(info, policy)

提供按窗口级别缩放尺寸的工具对象。

- `scale`：当前窗口级别缩放因子。
- `dp(baseDp, scale = true)`：可选缩放并 clamp 到 maximumScaledDimensionDp。
- `touchTargetDp(requestedDp)`：保证返回值不小于最小触控尺寸。
- `px(baseDp, scale = true)`：转换为像素并四舍五入。

该工具不替代 Android density 或 fontScale；文字仍应使用 sp。

## 12. 符号抽屉侧边停靠

### SymbolDrawerDocking

BOTTOM / LEFT / RIGHT。

### SymbolDrawerDockingPolicy

`resolve(settings, window)`：
- BOTTOM 设置始终返回 BOTTOM。
- SIDE 设置按 sideDockEdge 返回 LEFT/RIGHT。
- AUTO 在 availableWidth >= 600dp 时侧边停靠，否则底部。

`sideWidthDp(baseWidthDp = 72)` 将侧栏宽度 clamp 到 56..88dp。

### SymbolDrawerDockingLayoutSpec

输出：
- `docking`
- `collapsedExtentDp` / `expandedExtentDp`：主展开轴尺寸（底部模式为高度，侧边模式为宽度）。
- `crossAxisExtentDp`：交叉轴尺寸。
- `remainingEditorWidthDp`
- `sideRailWidthDp`
- `isSideDocked`

### SymbolDrawerDockingLayoutResolver.resolve

有两个重载：

1. 接受 `settings + window`，内部先调用 DockingPolicy 解析停靠边缘。
2. 接受显式 `docking + window`，按给定边缘计算尺寸。

底部模式保持原有 collapsed/expanded 高度；侧边模式根据窗口宽度、折叠宽度、最大展开宽度和最小剩余编辑器宽度计算可用尺寸。默认侧边展开最大宽度 480dp，最小剩余编辑器宽度 360dp，side rail 48dp。

Resolver 只返回尺寸，不会自动修改宿主布局参数。

### SymbolDrawerDockingCoordinator

构造：

```kotlin
SymbolDrawerDockingCoordinator(
    hostView: View,
    settings: SymbolDrawerSettings,
    bottomCollapsedHeightDp: Float,
    bottomExpandedHeightDp: Float,
    onLayoutResolved: (SymbolDrawerDockingLayoutSpec) -> Unit
) : AutoCloseable
```

内部创建 AdaptiveWindowObserver 并连接设置与窗口快照。

- `updateSettings(value)`：设置变化时重新解析。
- `updateBottomGeometry(collapsedHeightDp, expandedHeightDp)`：底部原始几何变化时更新；要求非负且 expanded >= collapsed。
- `updateFoldingFeature(value)`：传递折叠信息。
- `refresh()`：按最新 View bounds 重新计算。
- `close()`：关闭观察器并停止派发；幂等。

宿主收到 onLayoutResolved 后自行更新布局。不要在回调里反复创建 Coordinator。

## 13. SymbolDrawerAnchorAdapter

包：`drawer.adapter`。

### SymbolDrawerAnchors

包含 `collapsedHeightPx`、`expandedGridHeightPx`、`expandedHeightPx`。

### SymbolDrawerAnchorAdapter.resolve(groups, config, density)

把符号抽屉旧有网格几何转换成通用 Drawer 锚点：
- collapsed 高度由 `DrawerGeometry.collapsedHeightPx` 计算。
- expanded grid 高度由 `DrawerGeometry.expandedHeightPx` 计算。
- indicator 高度只加到 expanded height。
- density 必须有限且大于 0。

这是符号专用兼容适配器，不应把通用 Drawer 的三阶段策略直接套在符号工具栏而改变其既有几何合同。
