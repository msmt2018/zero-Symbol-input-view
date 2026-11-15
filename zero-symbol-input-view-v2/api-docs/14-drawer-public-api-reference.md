# 14 — 通用 Drawer 公共 API 参考

本篇以新公共 Kotlin 命名空间为入口，集中说明通用抽屉框架的对象关系、配置、锚点、控制器、导航、面板和样式。它是通用 UI 框架，不是符号工具栏的另一套实现。

## 1. 公共包与导入

```kotlin
import com.zerostudio.idea.widget.drawer.DrawerContainer
import com.zerostudio.idea.widget.drawer.DrawerConfig
import com.zerostudio.idea.widget.drawer.DrawerPresentationMode
import com.zerostudio.idea.widget.drawer.core.*
import com.zerostudio.idea.widget.drawer.navigation.*
import com.zerostudio.idea.widget.drawer.panel.*
import com.zerostudio.idea.widget.drawer.style.*
```

扩展类型位于 `com.zerostudio.idea.widget.drawer.extension`，适配器位于 `com.zerostudio.idea.widget.drawer.adapter`。

当前部分入口是 Kotlin `typealias` facade：Kotlin 调用方可使用新包；Java ABI 尚不能把这些别名视为独立 public class。内部实现类暂时仍位于 `com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.*`。不要在新 Kotlin 集成中混用两套导入。

## 2. 组件关系与职责

```text
Host Activity / Fragment / View
  └── DrawerContainer
       ├── main content (behind drawer)
       ├── peek panel
       ├── expanded panel OR DrawerPageHost
       │    └── DrawerPage / DrawerPanelContent
       ├── navigation presentation
       └── DrawerController
            ├── DrawerAnchorStrategy -> DrawerAnchorResolver
            ├── DrawerInteractionController
            └── DrawerState snapshot
```

- `DrawerContainer` 是 ViewGroup 和 NestedScrollingParent3，负责布局、面板和宿主内容。
- `DrawerController` 是运行时命令入口，负责状态/配置/锚点和监听。
- `DrawerAnchorStrategy` 是纯计算策略，不读取 Android View。
- `DrawerInteractionController` 负责触摸轴仲裁、拖动、fling 和 settle。
- `DrawerPageHost` 管理页面 View 创建、缓存和生命周期。
- `DrawerPanelContent` 描述一个面板内容及生命周期。
- `DrawerStyle` 聚合颜色、尺寸、字体、运动和形状。

## 3. 创建 DrawerContainer

```kotlin
val config = DrawerConfig.builder()
    .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
    .setDragEnabled(true)
    .setNestedScrollEnabled(true)
    .setDismissOnBack(true)
    .setDismissOnScrimTap(true)
    .setDragHandleEnabled(true)
    .build()

val drawer = DrawerContainer(context, config)
parent.addView(drawer)
drawer.setMainContent(editorView)
drawer.setTitle("Tools")
drawer.expand(animated = false)
```

`DrawerContainer(context, config)` 的 Context 应来自当前 Activity/Fragment 主题上下文。容器不会替宿主创建 Activity、扩大 Window 或自动处理所有系统 Insets。

### 容器方法

| 方法 | 行为与约定 |
|---|---|
| `setMainContent(view)` | 设置抽屉后方主内容；新 View 若已有父容器会先从旧父容器移除 |
| `setTitle(value)` | 设置标题；空值隐藏标题，并刷新锚点 |
| `setPeekContent(content)` | 设置/移除 Peek 面板 |
| `setExpandedContent(content)` | 设置/移除 Expanded 面板 |
| `setPages(pages, preserveSelection)` | 使用页面宿主显示页面；页面 ID 必须唯一 |
| `expand(animated)` / `collapse(animated)` | 请求到最高/最低锚点 |
| `refreshAnchors()` | 内容尺寸、标题、窗口尺寸变化后重新测量锚点 |
| `handleBack()` | 当前配置允许且可收起时返回 true；宿主据此决定是否消费 Back |

设置 pages 会让页面宿主成为展开内容，并清除直接设置的 Expanded 面板内容。Peek 与 Expanded 可分别使用不同内容。

## 4. DrawerConfig 与 Builder

通用配置类型是 `drawer.config.DrawerConfig`，不要与符号模型中的 `model.DrawerConfig` 混淆。

配置快照涵盖：

- `anchorStrategy`：锚点策略。
- `presentationMode`：BottomSheet / FullScreen。
- `style`：颜色、尺寸、字体、运动、形状。
- `peekContent` / `expandedContent`：面板内容。
- 拖动、嵌套滚动、Edge-to-edge 开关。
- Scrim 点击、Back、Drag handle、可访问性开关。
- `navigationConfig`：页面导航呈现配置。

Builder 常用方法：

```kotlin
val config = DrawerConfig.builder()
    .setAnchorStrategy(DrawerAnchorStrategies.threeStage())
    .setPresentationMode(DrawerPresentationMode.BOTTOM_SHEET)
    .setDragEnabled(true)
    .setNestedScrollEnabled(true)
    .setEdgeToEdgeEnabled(true)
    .setDismissOnScrimTap(true)
    .setDismissOnBack(true)
    .setDragHandleEnabled(true)
    .setAccessibilityEnabled(true)
    .setNavigationEnabled(true)
    .setNavigationPlacement(DrawerNavigationPlacement.TOP)
    .setNavigationScrollable(true)
    .setNavigationShowTitles(true)
    .setThemeMode(DrawerThemeMode.FOLLOW_HOST)
    .build()
```

Builder 还提供颜色属性、尺寸、字体、运动、形状、Elevation、Scrim 和内容裁剪等设置；完整字段语义参见 [07](07-universal-drawer.md) 与 [08](08-navigation-panels-style.md)。配置对象应先构建完整快照，再一次性提交，避免动画过程中高频改写多个字段。

### Presentation mode

- `BOTTOM_SHEET`：底部浮层，使用圆角、阴影和可选 Scrim。
- `FULL_SCREEN`：占据 DrawerContainer 的可用视口，移除 Sheet 浮层外观。它仍受父容器尺寸约束，不会自动扩展 Activity Window。

## 5. DrawerController

通过 `drawer.controller` 获取，不需要自行创建另一个状态控制器。

| API | 说明 |
|---|---|
| `expand(animated)` / `collapse(animated)` | 移动到最高/最低锚点 |
| `moveToAnchor(anchorId, animated)` | 移动到指定稳定锚点；ID 必须存在 |
| `refreshAnchors()` | 重新测量并解析锚点 |
| `setAnchorStrategy(strategy)` | 替换解析策略 |
| `setPresentationMode(mode)` | 切换呈现模式 |
| `applyConfig(config)` | 应用完整配置快照 |
| `setStyle(style)` | 替换完整样式快照 |
| `setThemeMode(mode)` | 修改主题模式 |
| `setColors/Dimensions/Typography/Motion/Shape(...)` | 替换对应样式子对象 |
| `setPeekContent / setExpandedContent` | 更新面板内容 |
| `setTitle / setPages / selectPage` | 更新导航和页面 |
| `setDragEnabled`、`setNestedScrollEnabled` | 控制手势能力 |
| `setEdgeToEdgeEnabled` | 控制容器 edge-to-edge 行为 |
| `setDismissOnScrimTap / setDismissOnBack` | 控制关闭策略 |
| `setDragHandleEnabled / setAccessibilityEnabled` | 控制把手和辅助功能 |
| `currentState()` | 获取状态快照；初始化/布局前可能为 null |
| `addStateListener / removeStateListener` | 观察状态；宿主负责移除监听 |

状态监听可能捕获 Activity 或 Fragment。必须在对应 View 生命周期结束时移除同一 listener 实例。

## 6. 锚点策略

### 核心类型

- `DrawerAnchorState`：`PEEK`、`HALF_EXPANDED`、`EXPANDED`、`CUSTOM`。
- `DrawerAnchor(id, heightPx, state)`：稳定 ID、可见高度像素和语义状态。
- `DrawerAnchorInput`：最小/最大高度、viewport/content 尺寸、兼容展开高度和 density。
- `DrawerAnchorStrategy.resolve(input)`：纯计算策略，返回锚点列表。

高度单位是 px，不是 dp。输入 density 必须有限且大于 0；高度非负；自定义 ID 不可重复。

### 内置策略

```kotlin
val twoStage = DrawerAnchorStrategies.existingHeight()
val threeStage = DrawerAnchorStrategies.threeStage(
    halfExpandedFraction = 0.5f,
    useAutomaticPeekHeight = true
)
val contentBased = DrawerAnchorStrategies.contentBased()
val custom = DrawerAnchorStrategies.custom(
    listOf(
        DrawerAnchor("peek", 120, DrawerAnchorState.PEEK),
        DrawerAnchor("half", 420, DrawerAnchorState.HALF_EXPANDED),
        DrawerAnchor("expanded", 900, DrawerAnchorState.EXPANDED)
    )
)
```

- `ExistingHeightAnchorStrategy`：Peek + 既有展开高度。
- `ThreeStageAnchorStrategy`：Peek / Half / Expanded；fraction 合法范围 0.1..0.9。
- `ContentBasedAnchorStrategy`：根据内容测量高度产生展开锚点。
- `CustomAnchorStrategy`：使用调用方给定锚点，最终仍经过 Resolver 规范化。

`DrawerAnchorResolver` 负责 clamp、排序、重复高度合并、最近锚点和 fling 目标。fling 只沿速度方向前进一个锚点，不会越级跳过整个阶段；高度坐标中正 velocity 表示展开。

## 7. DrawerState

状态快照包含 `currentAnchorId`、`currentHeightPx`、`anchors`、`isDragging`、`isAnimating`。

派生属性包括当前锚点、是否稳定停靠、是否展开、展开进度、是否处于 Peek/Expanded。注意“展开”表示高于最低锚点，不等于已经稳定停靠在最高锚点。

## 8. 手势与嵌套滚动

`DrawerContainer` 实现 `NestedScrollingParent3`。内部交互控制器以 touch slop 识别轴向；横向手势应留给子控件（例如 ViewPager），纵向拖动在抽屉边界和子滚动能力允许时接管。

`DrawerInteractionController` 关键方法：

- `setBounds(...)`：更新高度边界、动画时长和 fling 阈值。
- `synchronizeHeight(heightPx)`：取消动画并同步宿主恢复的高度。
- `onAttached()` / `onDetached()`：连接/清理交互生命周期。
- `onInterceptTouchEvent(event, childCanScrollUp)`：触摸拦截阶段做方向仲裁。
- `onTouchEvent(event)`：处理已接管触摸序列。
- `expand`、`collapse`、`settleToHeight`、`cancelAnimation`、`resetGesture`。

不要在外层父布局无条件对所有 MOVE 调用 `requestDisallowInterceptTouchEvent(true)`，否则可能破坏抽屉纵向拖动。横向分页与纵向抽屉拖动必须保持独立。

## 9. 页面导航

公共包：`drawer.navigation`。

- `DrawerPage(id, title, content, icon?)`：页面 ID 必须唯一且稳定。
- `DrawerNavigationController`：提交页面、按 ID/index 选择、读取当前页和 snapshot、添加/移除 listener。
- `DrawerNavigationConfig`：启用状态、TOP/BOTTOM/HIDDEN、指示器、滚动、标题/图标和尺寸。
- `DrawerNavigationPlacement`：TOP、BOTTOM、HIDDEN。
- `DrawerIndicatorShape`：LINE、PILL、NONE。

`selectPage(id/index)` 对不存在/越界目标返回 false。使用 ID 保存选择；index 仅表示当前顺序位置。导航 Controller 不负责绘制 UI。

## 10. 面板和页面生命周期

`DrawerPanelContent` 负责创建 View：

```kotlin
val content = object : DrawerPanelContent {
    override fun createView(context: Context): View =
        TextView(context).apply { text = "Tools" }

    override fun onPanelAttached() = Unit
    override fun onPanelVisible() = Unit
    override fun onPanelHidden() = Unit
    override fun onPanelDetached() = Unit
}
```

生命周期语义：

1. `createView(context)`：创建尚未挂载的新 View。
2. `onPanelAttached`：内容进入页面宿主生命周期。
3. `onPanelVisible`：面板可见。
4. `onPanelHidden`：内容暂时不可见，可暂停动画/任务。
5. `onPanelDetached`：缓存淘汰、Context 重建或宿主 dispose，释放独占资源。

隐藏不等于销毁；缓存页面可能再次显示。不要在 `onPanelHidden` 中永久释放仍要复用的对象。

`DrawerPageHost` 支持 setPages、setContentContext、selectPage、currentPage、measureCurrentContentHeight、setPanelVisible 和幂等 dispose。更换 Context 并要求重建缓存 View 时使用 `setContentContext(context, recreateCachedViews = true)`。

`DrawerPanelSlot` 为 PEEK / EXPANDED；`DrawerPanelHost` 管理指定 slot 的内容、View Context、测量和 visible 分发。

## 11. 样式与主题

`DrawerStyle` 聚合：

- `DrawerThemeMode`：FOLLOW_HOST / LIGHT / DARK。
- `DrawerColors`：surface、primaryText、secondaryText、accent、outline、scrim 等角色；未指定颜色可继承宿主主题。
- `DrawerColorAttributes`：主题属性映射，不是最终颜色值。
- `DrawerDimensions`：Peek/Expanded 高度、圆角、padding、handle 和间距，单位 dp。
- `DrawerTypography`：字体尺寸单位 sp。
- `DrawerMotion`：fling、spring、settle 时长和拖动阻尼。
- `DrawerShape`：四角圆角与描边。
- Elevation、Scrim 绘制和内容裁剪选项。

`DrawerThemeResolver` 负责主题模式解析和主题 Context。不要跨 Activity 长期持有旧 ContextThemeWrapper。

## 12. 预设

`DrawerPresets.config(preset)` 提供初始配置。预设包含 STANDARD、COMPACT、FULL_SCREEN、SPLIT_PANEL。预设不是强制限制；调用方可以在生成后调整 Builder 或配置快照。

## 13. 错误处理与集成建议

- 配置/锚点参数非法时应在构建或解析阶段修正/拒绝，不要依赖布局阶段容错。
- 页面 ID 重复属于调用方数据错误，应在提交前修复。
- 无效 page ID/index 返回 false；不要假定选择成功。
- 内容测量依赖 View 生命周期和有效尺寸，布局完成前可能得到 0。
- `currentState()` 在首次布局/锚点解析前可能为 null。
- 自定义页面/面板应避免阻塞主线程，并正确释放动画、监听器和协程。
- `DrawerContainer` 与 `SymbolDrawerView` 是两个不同入口；不要把通用 Drawer 当作符号控件的替代实现。
