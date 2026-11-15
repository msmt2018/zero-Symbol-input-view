# 07 — 通用 Drawer 框架 API

通用 Drawer 包：`com.zerostudio.idea.widget.editor.symbolinput.v2.drawer` 及其子包。

## 1. 与 SymbolDrawerView 的关系

`DrawerContainer` 是独立、功能中立的通用抽屉容器，面向任意宿主内容和页面。它提供 BottomSheet/FullScreen 呈现、锚点、拖动、嵌套滚动、面板、导航、样式和 Insets。

`SymbolDrawerView` 是专用符号工具栏，内部仍使用自己的 ViewGroup + ViewPager2 + SymbolPageAdapter。它只复用通用交互控制器/锚点适配边界，不会被 DrawerContainer 包裹。二者不应混用同名配置类。

## 2. DrawerContainer

构造：

```kotlin
DrawerContainer(
    context: Context,
    config: drawer.config.DrawerConfig = DrawerConfig.builder().build()
)
```

继承 `FrameLayout` 并实现 `NestedScrollingParent3`。

公开属性：

- `controller: DrawerController`：运行时控制面。
- `navigationHost: DrawerPageHost`：页面导航/内容宿主。

公开方法：

### setMainContent(view: View?)

设置 Drawer 后方的宿主主内容。旧主内容若挂载在本容器内会先移除；新 View 若已有父容器，会先从旧父容器移除，再加入当前容器。

### setTitle(value: CharSequence?)

设置抽屉标题；null/空字符串隐藏标题。标题变化后重新计算锚点。

### setPeekContent(content: DrawerPanelContent?)

设置 Peek/折叠面板内容。null 移除该面板。

### setExpandedContent(content: DrawerPanelContent?)

设置 Expanded 面板内容。null 移除展开面板；如果页面导航已有页面，页面宿主可成为展开内容。

### setPages(pages, preserveSelection = true)

设置通用页面列表并显示页面宿主。会清除直接设置的 Expanded 面板内容。页面 ID 必须唯一。

### expand(animated = true) / collapse(animated = true)

控制展开/收起。内部委托给 controller。

### refreshAnchors()

内容尺寸、标题或窗口约束异步变化后，重新测量面板并解析锚点。

### handleBack(): Boolean

当启用 dismissOnBack 且当前高度高于 Peek 时收起并返回 true；若不需要处理返回键则 false。宿主应仅在返回值 true 时消费 Back。

## 3. DrawerController

`DrawerContainer.controller` 返回的控制接口。

### 状态转换

- `expand(animated = true)`：移动到最高展开锚点。
- `collapse(animated = true)`：移动到最低 Peek 锚点。
- `moveToAnchor(anchorId, animated = true)`：移动到指定稳定锚点。
- `refreshAnchors()`：重新测量内容和锚点。
- `setAnchorStrategy(strategy)`：替换锚点解析策略。
- `setPresentationMode(mode)`：切换 BottomSheet/FullScreen。

### 原子配置

`applyConfig(config)` 替换完整配置快照。配置变更会影响当前 UI 状态，应避免在动画过程中高频重复提交配置。

### 样式设置

- `setStyle(style)`
- `setThemeMode(mode)`
- `setColors(colors)`
- `setDimensions(dimensions)`
- `setTypography(typography)`
- `setMotion(motion)`
- `setShape(shape)`

这些方法分别替换对应样式子对象；颜色 role 为 null 时允许继承宿主主题色。

### 内容设置

- `setPeekContent(content)`
- `setExpandedContent(content)`

null 表示移除对应面板内容。

### 页面导航

- `setTitle(title)`
- `setPages(pages, preserveSelection = true)`
- `selectPage(pageId): Boolean`
- `setNavigationConfig(config)`

页面 ID 不存在时 selectPage 返回 false。

### 功能开关

- `setDragEnabled(enabled)`
- `setNestedScrollEnabled(enabled)`
- `setEdgeToEdgeEnabled(enabled)`
- `setDismissOnScrimTap(enabled)`
- `setDismissOnBack(enabled)`
- `setDragHandleEnabled(enabled)`
- `setAccessibilityEnabled(enabled)`

### 状态观察

- `currentState(): DrawerState?`：获取当前状态快照；布局/锚点尚未完成初始化时可能为 null。
- `addStateListener(listener)`：添加状态监听。
- `removeStateListener(listener)`：移除同一函数对象的监听。

请在宿主生命周期结束时移除监听，避免闭包长期持有 Activity/Fragment。

## 4. DrawerConfig（drawer.config 包）

这是通用 Drawer 配置，与 `model.DrawerConfig` 不同。

字段：

- `anchorStrategy`：锚点策略。
- `style`：颜色、尺寸、形状、字体和运动参数。
- `peekContent` / `expandedContent`：两种面板内容。
- `enableDrag`、`enableNestedScroll`、`enableEdgeToEdge`。
- `dismissOnScrimTap`、`dismissOnBack`。
- `dragHandleEnabled`、`accessibilityEnabled`。
- `navigationConfig`：页面导航配置。
- `presentationMode`：BottomSheet 或 FullScreen。

### Builder

通过 `DrawerConfig.builder()` 创建 Builder。支持：

- `setAnchorStrategy`、`setPresentationMode`。
- `setPeekContent`、`setExpandedContent`。
- `setDragEnabled`、`setNestedScrollEnabled`、`setEdgeToEdgeEnabled`。
- `setDismissOnScrimTap`、`setDismissOnBack`。
- `setDragHandleEnabled`、`setAccessibilityEnabled`。
- `setNavigationConfig`、`setNavigationEnabled`、`setNavigationPlacement`、`setNavigationIndicator`、`setNavigationScrollable`、`setNavigationShowTitles`。
- `setStyle`、`setThemeMode`、`setFollowHostTheme`。
- `setColors`、`setColorAttributes`，以及 surface/primary/secondary/accent/outline/scrim 单色设置。
- `setDimensions`、`setTypography`、`setMotion`、`setShape`。
- `setElevationDp`、`setScrimEnabled`、`setClipContentToShape`。
- Peek/Expanded 高度、圆角、内边距、面板间距、Handle 尺寸、字体尺寸、字体族、四角半径、描边、fling 速度、spring、settle 时长和拖动阻尼设置。
- `build()`：生成不可变 DrawerConfig。

## 5. DrawerPresentationMode

- `BOTTOM_SHEET`：底部浮层，具有圆角/阴影等 Sheet 呈现。
- `FULL_SCREEN`：占据 DrawerContainer 测量到的完整可用视口；容器会去除 sheet 圆角、阴影和 scrim 等浮层特征。

FullScreen 仍然受宿主父容器尺寸约束，不会自动把 Activity Window 扩展到屏幕边缘。

## 6. DrawerAnchor 与 DrawerAnchorInput

### DrawerAnchorState

`PEEK`、`HALF_EXPANDED`、`EXPANDED`、`CUSTOM`。

### DrawerAnchor

```kotlin
data class DrawerAnchor(
    val id: String,
    val heightPx: Int,
    val state: DrawerAnchorState
)
```

高度以可见抽屉高度像素为单位，不能为负数；ID 不能为空。

### DrawerAnchorInput

输入参数：

- `minHeightPx`、`maxHeightPx`：允许高度范围。
- `viewportHeightPx`：父容器视口高度。
- `contentHeightPx`：内容测量高度。
- `existingExpandedHeightPx`：兼容模式下的既有展开高度。
- `density`：必须有限且大于 0。
- `viewportWidthPx`：可选视口宽度，供 16:9 Peek 计算。

## 7. DrawerAnchorStrategy 与内置策略

接口：

```kotlin
interface DrawerAnchorStrategy {
    val id: String
    fun resolve(input: DrawerAnchorInput): List<DrawerAnchor>
}
```

实现必须确定性、不能直接读取 Android View。这样可单元测试并可被宿主自定义。

### ExistingHeightAnchorStrategy

两锚点策略：`peek` 使用 minHeight，`expanded` 使用既有展开高度并 clamp 到合法范围。ID 为 `existing-height`。

### ThreeStageAnchorStrategy

```kotlin
ThreeStageAnchorStrategy(
    halfExpandedFraction: Float = 0.5f,
    useAutomaticPeekHeight: Boolean = true
)
```

提供 Peek / Half / Expanded 三个稳定状态。half fraction 范围 0.1..0.9；Expanded 达到最大可用高度；Half 按父视口高度计算；自动 Peek 使用宽度感知 16:9 keyline 并避免锚点顺序倒置。

### ContentBasedAnchorStrategy

Peek 为最小高度，第二锚点以内容测量高度为基础并 clamp 到可用范围。

### CustomAnchorStrategy

接收非空锚点列表，ID 必须唯一。构造时防御性复制并暴露只读快照；resolve 返回该列表，最终仍由 Resolver clamp 和规范化。

### DrawerAnchorStrategies 工厂

- `existingHeight()`
- `threeStage(halfExpandedFraction, useAutomaticPeekHeight)`
- `contentBased()`
- `custom(anchors)`

## 8. DrawerAnchorResolver

### resolve(strategy, input)

执行策略解析、ID 唯一校验、高度 clamp、排序和重复像素位置合并。重复高度时优先保留语义上更展开的状态。

### nearestAnchor(anchors, heightPx)

返回离当前高度最近的锚点。精确中点时选择较低可见高度，确保结果不依赖输入顺序。

### flingTarget(anchors, currentHeightPx, velocityY, minimumFlingVelocity)

速度低于阈值时选择最近锚点；达到阈值时只向 fling 方向前进一个稳定锚点，不会一次跳过所有阶段。velocityY 使用可见高度坐标：正值表示展开。

## 9. DrawerState

状态快照字段：

- `currentAnchorId`、`currentHeightPx`、`anchors`。
- `isDragging`、`isAnimating`。

派生属性：

- `currentAnchor`：当前锚点。
- `isAtStableAnchor`：未拖动/动画且高度精确匹配当前锚点。
- `isExpanded`：当前高度高于最低锚点。
- `expansionFraction`：最低至最高锚点之间的归一化进度。
- `isAtPeekAnchor` / `isAtExpandedAnchor`：完全停靠在最低/最高锚点时为 true。

构造时校验 ID、非负高度、锚点非空、ID 唯一且当前锚点存在。


## 10. DrawerInteractionController 的详细方法契约

该控制器虽然由通用 Drawer 与符号抽屉共享，但它不是一个 View，也不负责布局。构造时必须提供高度变化回调，宿主收到回调后更新自己的测量/布局。

只读状态：
- `currentHeightPx`：当前像素高度。
- `isDragging`：当前是否由手指直接拖动。
- `isAnimating`：当前 settle animator 是否运行。
- `isGestureAxisLocked`：触摸轴是否已锁定。
- `isHorizontalGestureLocked`：是否锁定为横向子控件手势。

公开方法：
- `setBounds(minimumHeightPx, maximumHeightPx, currentHeightPx, animationDurationMs, flingVelocityDpPerSecond)`：更新合法高度范围和动画/速度参数，并同步当前高度。最小高度非负、最大高度不小于最小高度、时长非负、速度有限且非负。
- `synchronizeHeight(heightPx)`：取消动画并同步宿主已经恢复的高度。
- `onAttached()`：标记宿主已 attach，允许后续动画执行。
- `onDetached()`：标记 detach，取消动画并清除当前手势。
- `onInterceptTouchEvent(event, childCanScrollUp)`：在 ViewGroup 拦截阶段进行方向仲裁。横向锁定交给子 Pager；纵向拖动根据边界和 child scroll 状态决定是否接管。
- `onTouchEvent(event)`：处理已经接管的触摸序列；MOVE 更新高度，UP/CANCEL settle 到目标高度。
- `expand(animated)` / `collapse(animated)`：settle 到最大/最小高度。
- `settleToHeight(heightPx, animated)`：把目标高度 clamp 到合法范围后 settle。
- `cancelAnimation()`：取消动画；如果动画实际运行过，会发出状态回调。
- `resetGesture()`：回收 VelocityTracker、清除方向锁定/拖动状态并恢复父容器拦截。

静态方法 `shouldInterceptVerticalDrag(fingerDeltaY, currentHeightPx, collapsedHeightPx, expandedHeightPx, childCanScrollUp)` 的 delta 采用手指坐标方向：负数为向上，正数为向下。向上时抽屉未达到最大高度即可接管；向下时只有抽屉高于最小高度且子控件不能继续向上滚动才接管。

### 方向仲裁和坐标约定

当前实现以 touch slop 判定手势方向，横向判定使用固定轴向比例 1.15。抽屉拖动高度使用 rawY 差值，避免抽屉移动过程中局部坐标系随布局变化。宿主不要在外层父布局对所有 MOVE 事件提前调用 disallow-intercept，否则可能破坏 Drawer 的纵向接管。
