# 08 — 页面导航、面板、主题与样式 API

## 1. 页面导航模型

包：`drawer.navigation`

### DrawerPage

```kotlin
data class DrawerPage(
    val id: String,
    val title: CharSequence,
    val content: DrawerPanelContent,
    val icon: Drawable? = null
)
```

- `id`：稳定且非空页面 ID。
- `title`：非空标题。
- `content`：页面内容提供者。
- `icon`：可选 Drawable。

页面 ID 用于保存/恢复选择和 Diff，不应使用当前 index 代替。

### DrawerNavigationSnapshot

包含 `selectedPageId: String?` 和 `pageIds: List<String>`。用于观察当前选择与页面 ID 顺序，不包含 View 或内容对象。

### DrawerNavigationController

UI 无关的页面选择模型。

- `submitPages(value, preserveSelection = true)`：提交页面；ID 必须唯一。若原选择仍存在且 preserveSelection 为 true，则保留；否则选择第一项。
- `selectPage(id): Boolean`：按 ID 选择；不存在返回 false。
- `selectPage(index): Boolean`：按索引选择；越界返回 false。
- `pages(): List<DrawerPage>`：返回当前页面快照。
- `selectedPage(): DrawerPage?`：当前页面；无页面时 null。
- `selectedIndex(): Int`：当前索引；无选择时 -1。
- `snapshot(): DrawerNavigationSnapshot`：返回 ID 状态快照。
- `addListener(listener)`：添加监听并立即以当前状态回调一次。
- `removeListener(listener)`：移除监听。

监听器回调参数为快照和选中索引。Controller 不负责绘制 Tab 或创建 View。

### DrawerNavigationConfig

页面导航视觉/布局配置：

- `enabled`：是否显示导航。
- `placement`：TOP / BOTTOM / HIDDEN。
- `indicator`：指示器样式。
- `scrollable`：导航是否横向滚动。
- `showTitles`、`showIcons`。
- `itemMinWidthDp`、横/纵 padding。
- `titleTextSizeSp`、`selectedTitleTextSizeSp`、`selectedTitleBold`。
- `keepSelectedPageVisible`、`showInPeekPanel`。

构造时检查尺寸有限且非负、字体尺寸大于 0。

### DrawerNavigationPlacement

`TOP`、`BOTTOM`、`HIDDEN`。

### DrawerIndicatorStyle

字段：`shape`、`heightDp`、`widthFraction`、`cornerRadiusDp`、`color`、`animate`。

### DrawerIndicatorShape

`LINE`、`PILL`、`NONE`。

## 2. DrawerPageHost

继承 FrameLayout，管理页面 View 的创建、缓存、挂载和可见生命周期。

构造：

```kotlin
DrawerPageHost(
    context: Context,
    navigationController: DrawerNavigationController = DrawerNavigationController()
)
```

公开属性：`navigationController`。

### setPages(pages, preserveSelection = true)

校验页面 ID 唯一后提交。校验失败不会先触发生命周期回调或清除当前页面。

### setContentContext(value, recreateCachedViews = false)

更换创建页面 View 使用的 Context。recreateCachedViews 为 true 时，先对当前可见页面触发 hidden，再对缓存页面触发 detached，移除旧 View 并按新 Context 重建选中页面。用于主题 Context 改变等需要重建页面 View 的场景。

### selectPage(id) / selectPage(index)

选择页面；host 已 dispose 时抛 IllegalStateException；无效 ID/index 返回 false。

### currentPage()

返回当前选中 DrawerPage。

### measureCurrentContentHeight(maxWidthPx, maxHeightPx)

对当前缓存页面 View 执行宽度 EXACTLY、高度 AT_MOST 测量并返回 measuredHeight；无当前页面/缓存 View 时返回 0。

### setPanelVisible(visible)

通知当前页面面板是否可见。仅在宿主已 attach 时派发生命周期回调。

### dispose()

释放页面缓存、解除 Controller 监听、隐藏当前页面并对缓存内容触发 detached。幂等；dispose 后不应继续提交页面或更换 Context。

### 生命周期顺序

- 首次创建页面 View：`createView(context)`。
- 页面首次进入 host：`onPanelAttached()`。
- 面板可见：`onPanelVisible()`。
- 切换/隐藏：`onPanelHidden()`。
- 缓存淘汰、Context 重建或 dispose：`onPanelDetached()`。

缓存页面被切换时，View 可能暂时从容器移除，但内容对象不一定立即 detached；不要把“当前不可见”当作“资源已释放”。

## 3. DrawerPanelContent

包：`drawer.panel`

```kotlin
interface DrawerPanelContent {
    fun createView(): View
    fun createView(context: Context): View = createView()
    fun onPanelAttached() = Unit
    fun onPanelVisible() = Unit
    fun onPanelHidden() = Unit
    fun onPanelDetached() = Unit
}
```

实现者可以只覆盖无参 createView，也可覆盖带 Context 的主题感知版本。生命周期回调默认空实现。

建议：
- 不要在构造时就把 View 添加到父容器。
- createView 每次需要创建新 View 时返回未挂载 View。
- onPanelHidden 暂停动画/传感器/高频任务。
- onPanelDetached 释放页面独占资源。
- 不要在 onPanelHidden 中永久销毁仍会缓存复用的对象。

### DrawerPanelSlot

`PEEK` 与 `EXPANDED`，区分紧凑面板和展开面板。

### DrawerPanelHost

负责一个 slot 的内容实例和 View 管理。

- `setContent(content)`：使用 host Context 设置内容。
- `setContent(content, viewContext)`：使用指定主题 Context 创建内容。
- `refreshContentContext(viewContext)`：刷新 Context 并按实现策略重建内容 View。
- `measureContentHeight(maxWidthPx, maxHeightPx)`：测量内容高度。
- `content()`：返回当前内容对象。
- `dispatchPanelVisible(visible)`：分发可见/隐藏状态。

## 4. 样式 API（drawer.style）

### DrawerThemeMode

- `FOLLOW_HOST`：跟随宿主主题。
- `LIGHT`：浅色。
- `DARK`：深色。

### DrawerColors

颜色角色包含 surface、primaryText、secondaryText、accent、outline、scrim 等。null 表示不覆盖该角色，由主题解析器继承宿主/主题颜色。

`overlay(override)`：将非空 override 角色覆盖到当前颜色集，未指定角色保留原值。

### DrawerColorAttributes

声明从主题属性解析颜色的属性映射。用于主题集成，不是像素颜色本身。

### DrawerDimensions

控制 Peek 高度、展开最大高度、圆角、水平/垂直 padding、面板间距、handle 宽高等尺寸。尺寸单位为 dp；构造值应为有限且非负数（以具体字段验证为准）。

### DrawerTypography

控制标题/正文/说明字体尺寸、字体族和文字样式。字体大小以 sp 为单位。

### DrawerMotion

控制最小/最大 fling 速度、spring 开关、settle 动画时长和拖动阻尼等运动参数。拖动阻尼应保持在实现允许范围内，避免异常跳变。

### DrawerShape

控制四角圆角和 outline 宽度；FullScreen 呈现会覆盖圆角/阴影，使面板占满容器视口。

### DrawerStyle

聚合 `themeMode`、`colors`、`colorAttributes`、`dimensions`、`typography`、`motion`、`shape`、`elevationDp`、`drawScrim` 和 `clipContentToShape`。

### DrawerThemeResolver

负责将 FOLLOW_HOST/LIGHT/DARK 转为用于创建内容的主题 Context，并解析最终颜色。若使用自定义 ContextThemeWrapper，应避免跨 Activity 长期持有旧 Context。

## 5. DrawerPresets

`DrawerPreset` 枚举：

- `STANDARD`
- `COMPACT`
- `FULL_SCREEN`
- `SPLIT_PANEL`

`DrawerPresets.config(preset): DrawerConfig` 返回相应预设配置。预设是可修改的初始配置，不是运行时强制限制；调用者可用 Builder 或 data class copy 覆盖。

## 6. 示例：自定义页面和面板

```kotlin
val pageContent = object : DrawerPanelContent {
    override fun createView(context: Context): View =
        TextView(context).apply { text = "自定义页面" }

    override fun onPanelVisible() {
        // 开始当前页可见期间的工作
    }

    override fun onPanelHidden() {
        // 暂停可见期间任务
    }

    override fun onPanelDetached() {
        // 释放资源
    }
}

val drawer = DrawerContainer(context)
drawer.setPages(
    listOf(DrawerPage("overview", "概览", pageContent))
)
```
