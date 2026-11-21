# 10 — API 类型索引、兼容性与排错

本页汇总 V2 主源码中的公开类型与扩展点，便于开发者定位入口。详细行为请以对应章节为准。

## 1. 主 API 类型索引

### 符号抽屉与模型

| 类型 | 包 | 作用 |
|---|---|---|
| SymbolDrawerView | 根 v2 包 | 专用符号抽屉 View |
| SymbolDrawerView.OnSymbolActionListener | 根 v2 包 | 短按/长按动作通知 |
| SymbolGroup | model | 分组数据 |
| SymbolEntry | model | 符号数据 |
| SymbolAction | model | typed action sealed interface |
| CursorTarget | model | 光标移动目标 |
| DeleteKind | model | 删除范围 |
| SelectionTransform | model | 选区转换 |
| ClipboardOperation | model | 剪贴板操作 |
| LegacySymbolAction | model | V1 迁移表示 |
| model.DrawerConfig | model | 符号网格/抽屉基础配置 |

### 动作执行

| 类型 | 作用 |
|---|---|
| SymbolActionHost | 编辑器能力适配接口 |
| SymbolActionExecutor | 动作树校验和同步执行 |
| SymbolActionResult | 执行结果 sealed interface |
| SymbolActionCancellation | 步骤间取消协议 |
| SymbolActionId | 稳定命名空间动作 ID |
| SymbolActionHandler<C> | 注册 handler 函数接口 |
| SymbolActionRegistry<C> | 进程内 handler 注册表 |
| RegistryBackedSymbolActionHost | 带注册表的 host 装饰器 |
| SymbolActionJsonCodec | typed action JSON 编解码 |
| SymbolTextDocument | 通用文本编辑接口 |
| SymbolTextDocumentActionHost | 基于通用文档 Adapter 的默认动作宿主 |
| SymbolTextClipboard | 剪贴板能力注入接口 |
| GenericSymbolTextActions | 通用文本动作 handler 集 |
| SymbolTextEditing | 宏/文本范围/插入计算工具 |
| BuiltinSymbolActionCatalog | 内置编辑器命令目录 |

### 数据、仓储与迁移

| 类型 | 作用 |
|---|---|
| SymbolGroupRepository | 分组仓储接口 |
| SharedPreferencesSymbolGroupRepository | 默认 V2 分组仓储 |
| SymbolGroupManager | 仓储命令模型 |
| SymbolDrawerDefaults | 默认分组工厂 |
| SymbolGroupStorageCodec | 持久化 JSON codec |
| SymbolGroupBundleCodec | 备份 bundle codec |
| LegacySymbolDataMigration | 一次性旧数据迁移 |
| LegacySymbolActionAdapter | 数字动作转换 |
| LegacySymbolActionCatalog | V1 动作 ID 目录 |

### 设置

| 类型 | 作用 |
|---|---|
| SymbolDrawerSettings | 设置快照 |
| ThemeMode / GroupIndicatorStyle / HapticMode | 主题、指示器、触觉枚举 |
| LargeScreenLayoutMode / SideDockEdge | 大屏停靠偏好 |
| SymbolDrawerSettingsRepository | 设置仓储接口 |
| SettingsLoadResult | 设置加载与恢复结果 |
| SharedPreferencesSymbolDrawerSettingsRepository | 默认设置仓储 |
| SymbolDrawerSettingsCodec | 设置 JSON codec |
| SymbolDrawerSettingsScreen | 设置 View |

### 通用 Drawer

| 类型 | 作用 |
|---|---|
| DrawerContainer | 独立通用抽屉 ViewGroup |
| DrawerController | 运行时控制接口 |
| DrawerConfig（drawer.config） | 通用 Drawer 配置 |
| DrawerPresentationMode | BottomSheet / FullScreen |
| DrawerAnchor / DrawerAnchorInput / DrawerAnchorState | 锚点与解析输入 |
| DrawerAnchorStrategy | 自定义锚点策略接口 |
| ExistingHeightAnchorStrategy | 双状态兼容策略 |
| ThreeStageAnchorStrategy | 三阶段策略 |
| ContentBasedAnchorStrategy | 内容高度策略 |
| CustomAnchorStrategy | 用户锚点策略 |
| DrawerAnchorStrategies | 策略工厂 |
| DrawerAnchorResolver | 锚点规范化/最近锚点/fling 目标 |
| DrawerState | 运行状态快照 |
| DrawerInteractionController | 通用手势/动画控制器 |
| SymbolDrawerAnchorAdapter | 符号工具栏旧几何适配器 |

### 导航、面板与样式

| 类型 | 作用 |
|---|---|
| DrawerPage | 通用页面定义 |
| DrawerNavigationController | UI 无关页面选择模型 |
| DrawerNavigationSnapshot | 页面选择快照 |
| DrawerNavigationConfig | 导航外观/布局配置 |
| DrawerNavigationPlacement | 导航位置 |
| DrawerIndicatorStyle / DrawerIndicatorShape | 指示器配置 |
| DrawerPageHost | 页面 View 缓存和生命周期 |
| DrawerPanelContent | 面板内容与生命周期接口 |
| DrawerPanelHost | 单个面板 View host |
| DrawerPanelSlot | Peek / Expanded 面板类型 |
| DrawerStyle | 样式聚合 |
| DrawerColors / DrawerColorAttributes | 颜色角色 |
| DrawerDimensions | 尺寸 |
| DrawerTypography | 字体 |
| DrawerMotion | 动画/拖动参数 |
| DrawerShape | 形状 |
| DrawerThemeMode / DrawerThemeResolver | 主题模式与解析 |
| DrawerPreset / DrawerPresets | 预设配置 |

### 自适应布局

| 类型 | 作用 |
|---|---|
| WindowAdaptiveInfo | 当前应用窗口快照 |
| WindowSizeClass / WindowPosture | 窗口级别/设备姿态 |
| WindowInsetsDp | dp Insets |
| FoldingFeatureInfo | 折叠特征 |
| AdaptiveWindowObserver | View 窗口观察器 |
| AdaptiveWindowController | 多订阅者窗口控制器 |
| AdaptiveContentLayoutPolicy / Spec / Resolver | 内容宽度和 Dialog 布局 |
| AdaptiveLayoutPolicy / Spec / Resolver | 列表详情/Feed/Supporting Pane |
| AdaptiveGridSpec / Resolver | 网格列数和间距 |
| AdaptiveDimensionPolicy / AdaptiveDimensions | 尺寸缩放 |
| SymbolDrawerDocking / Policy | 底部/左右停靠策略 |
| SymbolDrawerDockingLayoutSpec / Resolver | 停靠尺寸解析 |
| SymbolDrawerDockingCoordinator | 设置与窗口观察协调器 |

### 管理 UI

| 类型 | 作用 |
|---|---|
| SymbolDrawerManagerScreen | 分组/符号管理 View |
| LegacyActionPickerResolver | 管理界面旧动作选择器辅助解析 |

## 2. SymbolDrawerManagerScreen API

包：`manager`。

构造：

```kotlin
SymbolDrawerManagerScreen(
    context: Context,
    repository: SymbolGroupRepository = SharedPreferencesSymbolGroupRepository(context),
    embeddedInSettings: Boolean = false
)
```

回调：
- `onGroupsChanged: ((List<SymbolGroup>) -> Unit)?`：分组/符号数据变更通知。
- `onExportBundle: ((String) -> Unit)?`：备份 JSON 导出通知。
- `onError: ((Throwable) -> Unit)?`：操作错误。

方法：
- `reload()`：从仓储重新加载并刷新屏幕。
- `bindToDrawer(drawer): AutoCloseable`：绑定到 SymbolDrawerView；返回句柄应在页面退出时 close，防止旧屏幕继续持有 Drawer。
- `exportBundle(): String`：导出当前分组 JSON bundle。
- `importBundle(json)`：验证并导入备份；UI 会要求用户确认替换当前分组。
- `recordRecentlyUsed(symbolId)`：记录最近使用的符号 ID。

屏幕内置分组 CRUD、排序/复制、符号新增/编辑/删除、复制/移动、批量选择、搜索、收藏和最近使用 UI。管理 UI 使用自己的本地偏好记录收藏/最近使用状态；不要把这些 UI 状态与 SymbolGroupRepository 的分组快照混为一谈。

## 3. 兼容性规则

### V1 数字动作

- V2 主动作协议是 `SymbolAction`。
- V1 numeric action ID 和 payload 仅作为迁移/兼容元数据保留。
- 不要更改 V1 action catalog 的 ID 顺序、含义或 payload 约定。
- 未知旧 ID 不应被猜测映射到其他动作。

### 旧版存储

- 迁移仅在 V2 数据为空时执行。
- 旧版 `advanced_symbol_prefs` 数据只读。
- 迁移结果写入独立 V2 repository。
- 不要在重构时把 V2 key 改回旧版 key。

### JSON 版本

- Settings schema 当前版本 3。
- Action schema 当前版本 1。
- Group storage/bundle 使用各自 codec 和 envelope。
- 外部备份导入必须完整验证后再替换数据。

### 同名类型冲突

下列类型同名但 package 不同：

- `model.DrawerConfig`：符号网格和抽屉配置。
- `drawer.config.DrawerConfig`：通用抽屉容器配置。

建议使用显式 import alias：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig as SymbolDrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig as UniversalDrawerConfig
```

## 4. 常见问题

### 动作回调触发但文本没有插入

检查：
1. 宿主是否实现了 `SymbolActionHost.insertText`。
2. 是否在正确线程调用。
3. 是否把 longPress=true 时的动作误读为 shortAction。
4. host 是否因只读文档/无效选区返回 false。
5. Executor 是否返回 Unsupported / Invalid / Failed。

### Composite 只执行一部分

Composite 不支持自动回滚。检查 `PartialFailure.completedActions` 和内部 cause；需要原子编辑时由宿主提供编辑器事务或撤销组。

### Manager 修改没有反映到 Drawer

设置 `onGroupsChanged` 或 `bindToDrawer(drawer)`，并在宿主导航返回时按需调用 `reloadGroups()`。

### Drawer 页面切换后资源仍存在

页面可能被缓存。不可见时用 `onPanelHidden` 暂停工作；真正释放放在 `onPanelDetached` 或 `DrawerPageHost.dispose()`。

### 大屏判断不符合设备方向

API 判断当前窗口可用宽度，不是设备方向。确认传入的是 Activity 内容窗口尺寸，并检查是否重复扣除了 Insets。

### 设置变化未同步到现有 View

监听 `onSettingsChanged` 并调用 `drawer.applySettings(snapshot)`；或确保多个 View 实例共享同一设置仓储。

## 5. 开发与验证建议

- 对纯模型/Resolver/Codec 使用单元测试。
- 对 ViewPager2 横向分页、抽屉纵向拖动、嵌套滚动、长按和 Back 使用 instrumentation test。
- 覆盖 compact、medium、expanded、分屏、旋转和折叠设备。
- 对 Repository 测试写入失败、损坏 JSON、迁移已完成、迁移目标非空等路径。
- 对动作测试 host 返回 false、抛异常、取消、深层 Composite、超长 payload 和未知扩展 ID。
- 公开 API 新增/删除/改签名时同步更新本目录和 README，并增加契约测试。


扩展注册 API 类型索引见 [12 — 扩展注册框架](12-extension-framework.md)，包括 SymbolDrawerExtensionRegistry、SymbolDrawerActionExtension、SymbolGroupIndicatorExtension、SymbolDrawerSettingsTabExtension、SymbolDrawerSettingExtension、SymbolDrawerThemeExtension 和 SymbolDrawerTheme。
