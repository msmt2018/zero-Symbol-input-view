# 02 — SymbolDrawerView API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2`

完整 import：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
```

注意：`DrawerConfig` 是 `model` 包中的符号抽屉配置；它与通用 Drawer 的同名配置不是同一个类型。

## 1. 类职责

`SymbolDrawerView` 是面向符号输入场景的独立 Android `ViewGroup`。内部组合：

- `SymbolGroupIndicator`：分组指示器和设置入口。
- `ViewPager2`：横向切换符号组。
- `SymbolPageAdapter`：每组的符号网格页面。
- `DrawerInteractionController`：纵向抽屉拖动、横向手势轴仲裁、速度和 settle。
- `SymbolGroupRepository`：数据读取和保存。
- `SymbolDrawerSettingsRepository`：设置快照读取与同步。

它与编辑器实现解耦。符号动作通过回调交给宿主。

## 2. 构造函数

```kotlin
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    config: DrawerConfig = DrawerConfig()
)
```

| 参数 | 说明 |
|---|---|
| `context` | View 所属 Context；应使用当前 Activity/主题 Context 创建 |
| `attrs` | XML 属性集，可空 |
| `defStyleAttr` | Android 默认样式属性 |
| `config` | 初始符号抽屉布局配置；设置仓储中已有持久化设置时，持久化设置会成为运行时配置来源 |

`DrawerConfig` 来自 `model` 包，与通用 Drawer 的 `drawer.config.DrawerConfig` 不是同一个类。导入时请检查完整包名。

## 3. 回调与监听属性

### 3.1 `OnSymbolActionListener`

```kotlin
fun interface OnSymbolActionListener {
    fun onAction(symbol: SymbolEntry, isLongPress: Boolean)
}
```

当符号短按或长按动作被分发时调用。参数：

- `symbol`：被触发的完整符号数据。
- `isLongPress`：true 表示长按，false 表示普通点击。

回调不是动作执行结果；宿主需要自行执行 `symbol.shortAction` 或 `symbol.longAction`。如果对应动作为空，不会产生可执行动作。

### 3.2 `onSymbolActionListener`

主要动作分发回调。应在 View 可交互前设置。建议在回调内快速完成动作路由，不要执行磁盘 I/O 或耗时计算。

### 3.3 `onSymbolUsedListener: ((SymbolEntry, Boolean) -> Unit)?`

普通符号动作成功进入宿主回调分发路径后触发的使用通知，参数与动作监听相同。它不是编辑器文本已成功修改的确认；若需要准确统计“执行成功”，应由宿主根据 `SymbolActionResult` 自行记录。

### 3.4 `onOpenManagerListener: (() -> Unit)?`

当 `SymbolAction.OpenManager` 被触发时调用。该动作由 View 路由到管理入口，不会继续作为普通编辑器动作分发。

### 3.5 `onOpenSettingsListener: (() -> Unit)?`

设置入口回调。若已设置，点击设置入口后交由宿主处理；若为 null，则创建内置 `SymbolDrawerSettingsScreen` 并通过 AlertDialog 展示。

### 3.6 `onGroupChangedListener: ((Int, SymbolGroup?) -> Unit)?`

当前组切换时回调：

- 第一个参数是当前组索引。
- 第二个参数是当前组数据；索引对应数据不存在时可能为 null。

提交新组数据后恢复当前组，也可能触发该通知。不要假定只有用户手势才会触发。

## 4. 数据源与分组方法

### `setRepository(value, reload = true)`

```kotlin
fun setRepository(value: SymbolGroupRepository, reload: Boolean = true)
```

替换当前数据仓储。若 `reload` 为 true（默认），立即从新仓储读取并提交分组。若 false，只替换数据源，不自动刷新 View。

### `submitGroups(value, preserveSelectedGroup = true, onCommitted = null)`

```kotlin
fun submitGroups(
    value: List<SymbolGroup>,
    preserveSelectedGroup: Boolean = true,
    onCommitted: (() -> Unit)? = null
)
```

向当前 View 提交完整分组快照。

- 校验分组 ID 唯一。
- 校验每个组内部符号 ID 唯一。
- 对传入符号列表创建防御性浅快照。
- 尽可能按当前组稳定 ID 保留选中页；若原组已删除，则回退到合法页索引。
- 使用 Adapter 的列表提交完成回调执行 `onCommitted`。

异常：重复分组 ID 或组内重复符号 ID 会抛出 `IllegalArgumentException`。该方法的提交完成回调是 UI 列表更新完成通知，不代表调用了 `saveGroups()`。

### `reloadGroups()`

从当前 `SymbolGroupRepository` 读取全部分组并更新 UI。仓储异常/损坏处理取决于具体 Repository 实现。

### `saveGroups()`

将当前 View 内存中的分组快照写入当前 Repository。默认仓储同步提交 JSON；自定义仓储可定义自己的持久化策略。应在主线程以外的策略是否可行，取决于自定义实现；不要阻塞 UI。

### `getGroups(): List<SymbolGroup>`

返回当前分组快照的 List 副本。符号/组数据类本身不可变；不要将返回值视为仓储的实时可变引用。

## 5. 当前分页和展开状态

### `getCurrentGroupIndex(): Int`

返回 ViewPager2 当前页索引。空数据时该值不应被当作有效分组索引使用。

### `selectGroup(index, smoothScroll = true)`

选择指定分组。索引不在当前分组范围时直接忽略，不抛出越界异常。

- `index`：目标分组位置。
- `smoothScroll`：是否使用 ViewPager2 平滑切换。

### `isExpanded(): Boolean`

当内部展开进度 `expansion >= 0.5f` 时返回 true。它表达的是折叠/展开状态判断，不代表已精确停靠在某个稳定锚点。

### `setExpanded(expanded, animate = true)`

设置展开/折叠目标。动画受设置中的动画时长和减少动态效果配置影响。若启用“记忆展开状态”，该方法同时写入持久化布尔值。

## 6. 设置与主题

### `applySettings(value: SymbolDrawerSettings)`

将完整设置快照应用到当前 View。

- 更新布局配置、颜色、分组指示器设置和预取策略。
- 仅在影响单元格显示或网格几何的配置变化时替换页面 Adapter。
- 尝试保留当前分组的稳定 ID。
- 若关闭 `rememberExpandedState`，移除已保存的展开状态。
- 调用后设置快照成为当前运行时设置。

设置值构造时已校验数值范围；请使用 `SymbolDrawerSettings` 的合法字段值。

### `getSettings(): SymbolDrawerSettings`

返回当前应用的不可变设置快照。

### `refreshHostTheme()`

宿主主题变化但 Activity 未重建时调用。刷新抽屉、指示器颜色并重新应用当前设置。

## 7. 宿主生命周期

### `onHostResume()`

为从管理界面返回的宿主提供显式刷新入口；当前实现调用 `reloadGroups()`。若宿主自行维护数据流，可直接调用 `reloadGroups()`。

### `onAttachedToWindow()` / `onDetachedFromWindow()`

由 Android View 生命周期调用，不应由应用手动调用。

- attach：注册设置 SharedPreferences 监听、读取最新设置、连接交互控制器、恢复记忆的展开状态。
- detach：保存展开状态（若启用）、解除设置监听、关闭设置对话框并清理交互控制器。

### `onSaveInstanceState()` / `onRestoreInstanceState(state)`

由 Android View 状态保存流程调用。保存当前分组 ID 和连续展开进度；恢复时按稳定 ID 还原当前页并恢复展开进度。持久化的“记忆展开状态”与 View 层级状态保存是两个机制：前者跨 detach/重新 attach，后者参与 Android View 状态恢复。

## 8. 手势与交互约定

- 纵向拖动由抽屉交互控制器仲裁。
- 横向拖动由内部 ViewPager2 处理。
- 网格仅在抽屉完全展开时允许纵向滚动（当前适配器回调以展开进度判定）。
- 符号长按与页面滑动需要由触摸监听协同处理；宿主不应在外层父容器无条件拦截所有 MOVE。
- 不要通过手动调用 `selectGroup` 伪造用户滑动；真实触摸与程序选择应保持语义区分。

## 9. 推荐的生命周期绑定方式

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val drawer = view.findViewById<SymbolDrawerView>(R.id.symbol_drawer_v2)
    drawer.onSymbolActionListener = actionListener
    drawer.onOpenManagerListener = { findNavController().navigate(R.id.symbolManager) }
    drawer.onOpenSettingsListener = { findNavController().navigate(R.id.symbolSettings) }
    drawer.reloadGroups()
}

override fun onResume() {
    super.onResume()
    drawer.onHostResume()
}
```

如果 Fragment 的 View 会销毁重建，回调应绑定到当前 View 生命周期，不要让旧 View 持有已销毁 Fragment。

## 扩展注册与动作宿主

`SymbolDrawerView.extensions` 返回该 View 独立的 `SymbolDrawerExtensionRegistry`。可通过 `registerAction`、`registerIndicator`、`registerSettingsTab`、`registerSetting`、`registerTheme` 注册自定义贡献；列表分别通过 `actions()`、`indicators()`、`settingsTabs()`、`settingsItems()`、`themes()` 读取。每次注册返回 `AutoCloseable`，页面或编辑器会话结束时关闭。

`bindActionHost(host)` 返回经过扩展注册表装饰的 `SymbolActionHost`，使 `SymbolAction.Extension` 自动分发到当前 View 已注册的动作 handler；未知扩展仍回退到宿主原始 `executeExtension`。该方法不会创建或持有新的编辑器对象。

`extensions.setBuiltInVisible(featureId, visible)` 设置宿主定义的内置功能可见性覆盖；注册表只保存状态，具体 UI 列表应读取 `isBuiltInVisible` 后决定是否显示。扩展注册对象不持久化，重建后需重新注册。
