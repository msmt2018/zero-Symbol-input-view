# 17 — Compose API 完整开发参考（KDoc / JavaDoc）

> 本文依据 `zero-symbol-input-view-v2-compose/src/main` 当前源码整理。API 签名、可见性和默认值以 Kotlin 源码为准；本文件不把内部实现类型提升为稳定公共契约。

## 1. API 文档约定

公开 Kotlin 声明采用 KDoc（Dokka 可直接读取），Java 互操作说明采用 JavaDoc 术语。每个公开类型/成员应说明用途、状态所有权、输入约束和副作用；函数按需包含 `@param`、`@return`、`@throws`、`@see`。不适用的标签不应为了形式而添加。

```kotlin
/**
 * Creates a composition-owned state holder for the symbol drawer.
 *
 * The returned state survives recomposition while its call site remains
 * in the same Composition identity. It is not a ViewModel or saved-state
 * persistence mechanism.
 *
 * @param initialPage zero-based initial page index; negative values are normalized to zero.
 * @param initiallyExpanded whether the first resolved geometry should start expanded.
 * @return a state object remembered by the current Composition.
 */
@Composable
fun rememberSymbolDrawerState(
    initialPage: Int = 0,
    initiallyExpanded: Boolean = false
): SymbolDrawerState
```

文档必须区分：
- **公开 API**：可由宿主应用导入并依赖的 public 声明。
- **内部实现**：`internal`、private、内部协调器及 UI 私有状态；除非为理解行为所必需，不承诺兼容。
- **Kotlin typealias**：仅为 Kotlin 源码别名，不会生成独立 JVM/Java 类。
- **Composable slot**：调用方提供的 Compose 内容 lambda，不是 View factory。

## 2. 模块和包

| 层 | 规范包 | 主要源码/职责 |
|---|---|---|
| 符号抽屉 UI | `com.zerostudio.idea.widget.editor.symbolinput.v2` | `SymbolDrawer.kt`：`SymbolDrawer`、`SymbolDrawerState`、`rememberSymbolDrawerState`、`SymbolDrawerDragPolicy` |
| 通用 Drawer 门面 | `com.zerostudio.idea.widget.drawer` | `DrawerContainer.kt`：`DrawerContainer`、`DrawerContainerState`、`rememberDrawerContainerState`；`DrawerApi.kt` Kotlin aliases |
| Drawer 配置 | `drawer.config` | `DrawerConfig`、`DrawerPresentationMode` |
| Drawer 状态/控制 | `drawer.core` | 锚点、状态快照、状态 holder、controller、动画/拖动/nested-scroll 策略 |
| Drawer 导航 | `drawer.navigation` | 页面模型、导航配置、状态引擎和 `DrawerPageHost` |
| Drawer 面板 | `drawer.panel` | `DrawerPanelContent`、`DrawerPanelSlot`、`DrawerPanelHost` |
| Drawer 样式 | `drawer.style` | `DrawerStyle`、颜色、尺寸、字体、动效、形状及主题解析 |
| Drawer 预设 | `drawer.preset` | `DrawerPreset`、`DrawerPresets` |
| Drawer 扩展/主题 | `drawer.extension` | `DrawerExtensionApi`、`SymbolDrawerTheme` |
| 编辑器中立动作 | `drawer.action` | 文档、编辑操作、动作 host、registry、执行引擎 |
| 符号模型 | `editor.symbolinput.v2.model` | `SymbolGroup`、`SymbolEntry`、`SymbolAction`、`DrawerConfig`、旧动作模型 |
| 符号动作 | `editor.symbolinput.v2.action` | 内置动作目录、JSON codec、executor/host aliases |
| 数据/迁移 | `editor.symbolinput.v2.data` | repository、manager、storage/bundle codec、legacy adapter/migration |
| 设置 | `editor.symbolinput.v2.settings` | 设置快照、repository、codec、Compose 设置页、JSON IO |
| 自适应 | `editor.symbolinput.v2.adaptive` | 窗口信息、尺寸/网格/布局解析、停靠策略和快照发布 |
| 管理 | `editor.symbolinput.v2.manager` | 管理 Compose 页面、动作编辑器、使用状态 |
| UI 辅助 | `editor.symbolinput.v2.ui` | 指示器、符号页、页面适配、主题颜色和默认值 |
| 状态几何 | `editor.symbolinput.v2.state` | `DrawerGeometry` |

## 3. 符号抽屉 API

### `SymbolDrawerState`

Composition 记忆的符号抽屉状态。公开成员：
- `selectedPage: Int`：当前页索引，零基；外部只能通过 `selectPage` 请求更新。
- `expanded: Boolean`：由当前锚点/高度快照推导的展开状态。
- `expand()`、`collapse()`、`toggle()`：请求锚点状态转换。
- `selectPage(index: Int)`：选择页索引，负数归一化为 0。

该状态不保存 Activity、Context、View、LifecycleOwner、Adapter 或宿主回调。它不是跨进程持久化存储；需要进程重建恢复时，应由宿主持久化业务数据/设置，并重新创建状态。

### `rememberSymbolDrawerState`

`@Composable` 工厂。参数 `initialPage` 为零基页索引，`initiallyExpanded` 控制首次几何初始化时的展开锚点。状态身份由调用点的 Composition identity 决定。

### `SymbolDrawer`

`@Composable` 符号输入组件。核心参数：

| 参数 | 类型 | 契约 |
|---|---|---|
| `groups` | `List<SymbolGroup>` | 当前符号分组快照；ID 应稳定 |
| `modifier` | `Modifier` | 宿主布局修饰符 |
| `state` | `SymbolDrawerState` | 组件状态；默认在调用点 remember |
| `settings` | `SymbolDrawerSettings` | 不可变设置快照 |
| `config` | `DrawerConfig` | 展示/网格配置，默认从 settings 转换 |
| `onSymbolClick` | `(SymbolEntry) -> Unit` | 必须提供；短按回调 |
| `onSymbolLongClick` | `((SymbolEntry) -> Unit)?` | 可选长按回调 |
| `onOpenManager` | `(() -> Unit)?` | 可选管理页导航请求 |
| `onOpenSettings` | `(() -> Unit)?` | 可选设置页导航请求 |
| `usageState` | `SymbolDrawerUsageState?` | 可选使用频率状态 |

组件只呈现数据并回调宿主；不要在 Composable 函数体内执行阻塞文件 IO、同步网络请求或持久化写入。管理/设置页面导航由宿主回调承接。

## 4. 领域模型与动作

### 模型

- `SymbolGroup(id, title, symbols)`：分组标识、标题和条目集合。ID 应跨刷新稳定。
- `SymbolEntry(id, label, shortAction, longAction, icon, legacyShortActionId, legacyShortText, legacyLongActionId, legacyLongText)`：符号条目；保留 legacy 字段用于 V1 JSON 无损迁移。
- `SymbolAction`：sealed typed action。变体包括 `InsertText`、`ReplaceSelection`、`SurroundSelection`、`MoveCursor`、`Delete`、`TransformSelection`、`EditorCommand`、`Clipboard`、`OpenManager`、`Composite`、`Extension`。
- `CursorTarget`、`DeleteKind`、`SelectionTransform`、`ClipboardOperation`：动作参数枚举。
- `LegacySymbolAction(type, payload)`：仅用于旧动作 schema 迁移。
- `model.DrawerConfig`：符号网格/抽屉尺寸配置；注意它与通用包 `drawer.config.DrawerConfig` 是不同类型。

### 动作执行

`drawer.action` 是编辑器中立执行层。主要契约：
- `SymbolTextDocument`：宿主文档适配接口；偏移量按 UTF-16 code unit 计，行/列为零基，范围为 end-exclusive。
- `SymbolActionHost`：将 typed action 映射为宿主编辑器能力。
- `SymbolActionExecutor` / `ActionExecutionEngine`：解释并执行动作，不替宿主切换线程。
- `SymbolActionResult`：区分完成、不支持/拒绝、取消、无效输入及执行错误；不要将所有失败折叠为成功布尔值。
- `SymbolActionRegistry`：按稳定 ID 注册处理器；注册句柄由调用方关闭。
- `SymbolTextDocumentActionHost` / `SymbolTextEditing`：基于文档契约提供编辑动作实现。

动作执行线程由宿主负责。若宿主编辑器要求主线程，必须在宿主调用边界切换到主线程。Composite 按顺序执行，不应假设默认具备事务回滚能力。

## 5. 通用 Drawer

### `DrawerContainerState`

Composition-owned state facade，公开 `controller`、`expanded`、`expand()`、`collapse()` 和 `toggle()`。同一个 Drawer 实例应复用同一个 state，不要再创建第二个独立状态机镜像。

### `rememberDrawerContainerState(initiallyExpanded)`

`@Composable` 工厂。状态在调用点 Composition identity 内保持；它不是 SavedStateHandle 或数据库。

### `DrawerContainer`

`@Composable` 通用容器，实际签名：

```kotlin
@Composable
fun DrawerContainer(
    config: DrawerConfig = DrawerConfig(),
    modifier: Modifier = Modifier,
    state: DrawerContainerState = rememberDrawerContainerState(),
    title: String? = null,
    pages: List<DrawerPage> = emptyList(),
    selectedPageId: String? = null,
    onPageSelected: (DrawerPage) -> Unit = {},
    onDismiss: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
)
```

- `config`：锚点策略、样式、面板 slot、拖动、嵌套滚动、dismiss、无障碍、导航和展示模式。
- `modifier`：外部布局修饰符。
- `state`：容器状态。
- `title`：可选标题。
- `pages`：带稳定 ID 的导航页。
- `selectedPageId`：外部选中页 ID；null 表示不指定外部选中项。
- `onPageSelected`：用户选择页时回调。
- `onDismiss`：容器请求关闭时回调。
- `content`：宿主内容 slot，接收 `BoxScope`。

容器受父 Compose layout constraints 约束；不创建 Activity/Window，不接管宿主系统栏，也不包装传统 View。

### 配置、锚点、控制和快照

- `drawer.config.DrawerConfig`：通用容器配置；`Builder` 提供链式配置入口。
- `DrawerPresentationMode`：`BOTTOM_SHEET` / `FULL_SCREEN`。
- `DrawerAnchor` / `DrawerAnchorInput`：锚点和几何输入模型。
- `DrawerAnchorStrategy`、策略解析器及预设：从可用空间和内容尺寸解析锚点。
- `DrawerState`：不可变状态快照，包含当前锚点、高度、拖动/动画状态和目标锚点。
- `DrawerStateHolder`：唯一可变几何状态引擎；负责锚点、拖动、动画代次及快照发布。
- `DrawerController` / `DefaultDrawerController`：宿主操作门面。
- `DrawerNestedScrollConnection` / `DrawerNestedScrollPolicy`：Compose nested-scroll 仲裁策略；内部消费量应遵循实际滚动方向与剩余 delta。
- `rememberDrawerStateHolder`、`rememberDefaultDrawerController`、`rememberDrawerStateSnapshot`：Composition 适配 API。
- `SynchronousSnapshotPublisher`：同步快照发布基础设施，不应被当作 UI state holder 的替代品。

锚点和控制层不负责绘制 UI；Composable 层观察快照并负责动画/手势 effect。不要在宿主再次维护一份可独立变化的高度或锚点状态。

## 6. 页面、面板、样式和预设

- `DrawerPage(id, title, content, icon)`：页面 ID 必须非空且稳定；`content` 为 `DrawerPanelContent`，`icon` 为可选 Composable。
- `DrawerPageDescriptor`：页面导航元数据。
- `DrawerPageHost`：以稳定 ID 同步页面选择和 pager 索引。
- `DrawerNavigationConfig` / `DrawerNavigationController` / `DrawerNavigationStateEngine`：导航配置、状态和选择请求。
- `DrawerNavigationPlacement` / `DrawerIndicatorStyle`：导航位置和指示器样式。
- `DrawerPanelContent`：`ColumnScope.() -> Unit` Composable slot；`drawerPanelContent { }` 创建 slot。
- `DrawerPanelSlot`：区分 peek 与 expanded 内容；`DrawerPanelHost` 负责呈现。
- `DrawerStyle`：组合颜色、尺寸、字体、动效、形状、elevation、scrim 和 clipping。
- `DrawerColors` / `DrawerColorAttributes` / `DrawerDimensions` / `DrawerTypography` / `DrawerMotion` / `DrawerShape`：不可变样式 token。
- `DrawerThemeResolver`：解析主题 token；null 颜色表示按实现约定继承主题。
- `DrawerPreset` / `DrawerPresets`：可复用预设，不代表强制全局主题。
- `SymbolDrawerTheme` / `SymbolDrawerThemeExtension`：符号抽屉主题扩展契约。

Composable slot 不拥有 View attach/detach 回调，也不要求调用方手动 dispose。

## 7. 仓储、管理、JSON 和迁移

- `SymbolGroupRepository`：`loadGroups` / `saveGroups` 持久化契约。
- `ObservableSymbolGroupRepository`：额外提供 `observeGroups(listener)`，返回的 `AutoCloseable` 必须释放。
- `SymbolGroupManager`：在 repository 之上执行分组/符号新增、更新、删除、复制、移动、排序和批量操作；领域校验失败通过异常显式报告。
- `SymbolGroupStorageCodec`：存储格式编码/解码。
- `SymbolGroupBundleCodec`：完整分组 bundle 导入/导出及校验。
- `LegacySymbolDataCodec` / `LegacySymbolActionAdapter` / `LegacySymbolActionCatalog` / `LegacySymbolActionDefinitions`：旧 V1 JSON/action schema 的适配。
- `LegacySymbolDataMigration` / `AndroidLegacySymbolDataMigration`：迁移逻辑与 Android 存储适配；不要将 Android adapter 当成纯 Kotlin 领域 API。
- `SharedPreferencesSymbolGroupRepository` / `SharedPreferencesSymbolUsageRepository`：Android SharedPreferences 实现。
- `SymbolDrawerDefaultGroups` / `SymbolDrawerDefaults`：默认模型数据，不是持久化层。

导入流程应先 decode/validate，再提交 repository；不能在解析半途修改 UI 可见列表。持久化成功后应发布新快照，使 Compose UI 读取最新数据。

## 8. 设置 API

`SymbolDrawerSettings` 是不可变、带校验的设置快照，包含主题、网格列/行、间距、指示器、完整标签、触控尺寸、动画/减少动效、分页缓存、搜索、haptic 和大屏停靠偏好。构造时执行范围校验；非法值应在边界处修正或拒绝，不应绕过模型约束。

- `ThemeMode`：SYSTEM / LIGHT / DARK。
- `GroupIndicatorStyle`：指示器 renderer 选择。
- `HapticMode`：OFF / SELECTION / ALL。
- `LargeScreenLayoutMode`：AUTO / BOTTOM / SIDE。
- `SideDockEdge`：LEFT / RIGHT。
- `SymbolDrawerSettingsRepository`：`load` / `save` / `reset` / `hasStoredSettings`。
- `SettingsLoadResult`：设置快照和无效数据恢复标记。
- `SymbolDrawerSettingsCodec.encode/decode`：本地/兼容解码；缺失字段使用默认值。
- `SymbolDrawerSettingsCodec.decodeStrict`：外部备份导入；校验 schema version、字段集合和字段类型，不接受未知字段。
- `SharedPreferencesSymbolDrawerSettingsRepository`：Android 偏好存储实现。
- `SymbolDrawerSettingsScreen`：Compose 设置 UI；通过状态快照和更新回调与宿主同步。
- `JsonDocumentIO` / `JsonDocumentPicker`：JSON 文档读取/写入和宿主文件选择适配。

当前 settings schema version 为 3。外部备份严格模式必须符合对应版本字段集合；不要将宽松本地恢复解码与严格导入解码混为一谈。

## 9. 自适应布局和 UI 组件

- `WindowAdaptiveInfo`：由实际可用窗口宽高和 density 构成的窗口快照；不要依赖物理屏幕尺寸推断分屏布局。
- `AdaptiveDimensions`：解析后的布局尺寸。
- `AdaptiveGridResolver`：依据窗口宽度、最小单元格宽度、首选列数和间距解析网格。
- `AdaptiveLayoutResolver` / `AdaptiveContentLayout`：解析布局模式和内容尺寸。
- `AdaptiveWindowController` / `AdaptiveWindowObserver` / `AdaptiveWindowComposition`：窗口信息观察与 Composition 接入。
- `ConcurrentSnapshotPublisher`：并发快照发布。
- `SymbolDrawerDockingPolicy` / `SymbolDrawerDockingLayoutResolver` / `SymbolDrawerDockingCoordinator`：大屏底部/侧边停靠策略与协调。
- `SymbolGroupIndicator`：指示器 Composable；`SymbolPage`：符号页 Composable。
- `SymbolPageAdapter`：分页内容与符号网格适配。
- `ComposeSymbolDrawerDefaults` / `HostThemeColors`：默认参数和宿主主题颜色适配。
- `DrawerGeometry`：符号抽屉几何值模型。

布局解析应以父容器真实 constraints 为输入；自适应解析器不应读写 Window 或依赖 Activity。

## 10. 扩展注册 API

`SymbolDrawerExtensionRegistry` 是运行时注册表，提供 snapshot/revision、可见性配置和以下注册入口：
- `registerAction` / `registerLegacyAction`
- `registerIndicator`
- `registerItemRenderer`
- `registerGroupProvider`
- `registerSettingsTab` / `registerSetting`
- `registerTheme`

所有注册返回 `AutoCloseable`；调用方必须持有并在扩展所属 session/Composition 结束时关闭。相同 ID 的替换/注销遵循 registry 当前 token 所有权语义，旧句柄不应删除后来替换的注册。

扩展契约：
- `SymbolDrawerActionExtension`：稳定 ID、标题、描述、payload hint、priority 和 handler。
- `SymbolDrawerLegacyGroupProvider`：旧分组数据 provider。
- `SymbolDrawerItemRenderer`：通过 `accepts(entry)` 选择条目并提供 Composable 内容。
- `SymbolGroupIndicatorExtension`：接收 groups、selectedIndex 和 onSelectGroup。
- `SymbolDrawerSettingsTabExtension` / `SymbolDrawerSettingExtension`：提供设置页或设置项内容。
- `SymbolDrawerSettingsScope`：读取当前设置快照并通过 `update(transform)` 提交变更。
- `SymbolDrawerExtensionSnapshot`：不可变 registry snapshot。

**注册不等于自动呈现。** 当前具体页面是否消费某类贡献，必须以该页面源码为准。不要在扩展 handler 中保存 Activity/View/Context，不要在 Composable body 内执行一次性注册；应由明确 owner 管理句柄。

## 11. 错误、线程和状态所有权

1. Compose state 只能由其 owner 更新；不要跨组件复制多个权威状态机。
2. Repository/codec 的阻塞 IO 应由宿主的数据层调度；本库不会替所有宿主决定 Dispatcher。
3. 动作执行同步运行在调用线程；线程切换由宿主适配器负责。
4. 导入数据先完整校验再写入，避免部分更新。
5. 回调是请求/事件边界；组件不替宿主导航或持久化。
6. `AutoCloseable` 注册句柄必须关闭，观察者句柄必须取消。
7. `require` / `IllegalArgumentException` 用于输入范围/结构不合法；找不到目标或状态冲突可能通过显式异常报告。具体函数抛出条件以实现为准。

## 12. 生命周期与平台边界

本模块不要求 Activity、Fragment、ViewModel 或 LifecycleOwner 作为 UI 状态管理合同，不创建 Android View，不使用 AndroidView 包装传统控件。它仍然运行在 Compose Runtime 中，具有 Composition identity、recomposition、remember 作用域和 effect cancellation。不得将“零传统 View/LifecycleOwner 耦合”表述为“没有任何生命周期语义”。

## 13. API 变更与兼容策略

- 公共包名、公开声明、参数默认值、序列化字段和稳定 ID 都是兼容性敏感面。
- 新增可选参数优先提供合理默认值；改变参数语义需更新中英文文档和示例。
- Kotlin typealias 的迁移不等于 Java 二进制类迁移。
- V1 JSON 字段与 numeric action ID 由 legacy codec/adapter 保持兼容；不要将它们改写为新的不兼容 schema。
- 通用 Drawer 的 `DrawerConfig` 与符号 UI 的 `model.DrawerConfig` 不可混用。
- View artifact 和 Compose artifact 的同名概念 API 不可互换。

## 14. 文档与源码核对清单

- [ ] 每个公开类/接口/枚举/对象有 KDoc 类型说明。
- [ ] 每个公开函数说明参数、返回值、异常和副作用（适用时）。
- [ ] 每个公开属性说明单位、可空性、可变性和状态 owner。
- [ ] 每个 Composable 说明 slot、回调时机和 Composition 状态行为。
- [ ] 每个 codec 说明宽松/严格模式、schema version 和错误策略。
- [ ] 每个 repository 说明持久化/观察/线程边界。
- [ ] 每个扩展点说明注册句柄生命周期和是否被 UI 消费。
- [ ] 中文/英文 API 文档和 import 示例同步。
- [ ] Dokka 生成结果不包含不应公开的 internal/private 实现细节。
