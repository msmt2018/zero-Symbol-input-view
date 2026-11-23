# 09 — 自适应窗口与大屏布局 API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive`

## 1. Compose 窗口观测

`AdaptiveWindowObserver` 通过 `BoxWithConstraints`、当前 Density、LayoutDirection 和 Compose WindowInsets 计算窗口快照。窗口尺寸变化、分屏和密度变化由 Compose 布局重新计算，不注册 ViewTree observer 或 attach listener。

```kotlin
AdaptiveWindowObserver(
    foldingFeature = FoldingFeatureInfo(),
    insetsMode = WindowInsetsMode.SUBTRACT_SYSTEM_INSETS
) { info ->
    // 根据 info 选择布局规格
}
```

`WindowInsetsMode`：
- `SUBTRACT_SYSTEM_INSETS`：从 Compose 布局边界扣除 safe-drawing insets。
- `VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS`：调用方提供的布局边界已扣除 insets，观测器不重复扣除。该枚举名称保留了历史语义；Compose API 本身不接收 View。

## 2. WindowAdaptiveInfo

快照字段：
- `widthDp` / `heightDp`：当前 Compose 布局边界尺寸。
- `density`：当前密度。
- `insets`：转换为 dp 的安全区域 Insets。
- `foldingFeature`：调用方提供的折叠特征快照。

`availableWidthDp` 和 `availableHeightDp` 扣除 Insets 后不会小于 0。尺寸分类使用可用宽度，而不是设备方向。

断点：
- COMPACT：小于 600dp
- MEDIUM：600dp 至小于 840dp
- EXPANDED：大于等于 840dp

## 3. 纯解析层

`AdaptiveLayoutResolver`、`AdaptiveGridResolver`、`SymbolDrawerDockingLayoutResolver` 和相关策略类型负责把不可变窗口规格映射为布局结果。它们不持有 Composable、Context、Activity 或 View。

## 4. Insets 约定

必须明确窗口尺寸是否已包含系统 Insets。只在一个层级扣除 Insets，避免重复缩小可用宽高。折叠特征数据由宿主传入；当前观测器不自行注册 WindowManager/Activity listener。