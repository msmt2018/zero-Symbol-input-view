# 06 — 设置中心 API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.settings`

## 1. SymbolDrawerSettings

不可变、带校验的设置快照。当前 schema 版本为 3。

| 属性 | 默认值 | 有效范围/含义 |
|---|---:|---|
| `schemaVersion` | 3 | 1..3；持久化 schema 版本 |
| `themeMode` | SYSTEM | SYSTEM / LIGHT / DARK |
| `columns` | 10 | 1..30，目标列数 |
| `collapsedRows` | 2 | 1..16，折叠可见行数 |
| `cellHeightDp` | 28 | 6..128，单元格高度 |
| `cellHorizontalSpacingDp` | 2 | -24..48 |
| `cellVerticalSpacingDp` | -2 | -24..48 |
| `indicatorHeightDp` | 26 | 0..96 |
| `indicatorStyle` | UNDERLINE | 分组指示器渲染样式 |
| `showGroupTitles` | true | 是否显示组标题 |
| `showSymbolLabels` | true | 是否显示符号标题 |
| `rememberExpandedState` | true | 是否记忆抽屉展开/折叠状态 |
| `showFullSymbolLabels` | false | 是否允许完整显示标题，不截断 |
| `symbolLabelTextSizeSp` | 18 | 8..32 |
| `showActionHints` | false | 是否显示动作提示 |
| `maxExpandedHeightDp` | 480 | 80..1800 |
| `animationDurationMs` | 200 | 0..1200 |
| `reduceMotion` | false | 减少动画；转换到 DrawerConfig 时动画时长变为 0 |
| `flingVelocityDpPerSecond` | 900 | 100..6000 |
| `longPressTimeoutMs` | 420 | 250..1000 |
| `hapticMode` | SELECTION | OFF / SELECTION / ALL |
| `autoCollapseAfterAction` | false | 动作分发后自动收起 |
| `pageCacheLimit` | 3 | 1..9 |
| `prefetchAdjacentGroups` | true | 是否预取相邻分组 |
| `minimumTouchTargetDp` | 40 | 20..112 |
| `searchIncludesActionPayload` | false | 搜索是否包含动作 payload |
| `largeScreenLayoutMode` | AUTO | AUTO / BOTTOM / SIDE |
| `sideDockEdge` | RIGHT | LEFT / RIGHT |

构造 data class 时执行范围校验，越界会抛出 `IllegalArgumentException`。

### toDrawerConfig()

`fun toDrawerConfig(): model.DrawerConfig`

把设置快照中影响符号抽屉基础几何/交互的字段映射到符号包的 `model.DrawerConfig`。注意它不是通用 Drawer 配置类。

## 2. 设置枚举

### ThemeMode
- `SYSTEM`：跟随系统/宿主主题。
- `LIGHT`：浅色偏好。
- `DARK`：深色偏好。

### GroupIndicatorStyle
包含传统 `TEXT_TABS`、`TEXT`、`PILL`、`DOTS`、`UNDERLINE`，以及 gooey、弹性、液态、刻度盘、雷达、轨道、波形、进度条、几何标记、缩略图、数字徽章、垂直轨道等风格枚举。枚举是渲染策略标识；每个风格的细节仍由 `SymbolGroupIndicator` 实现决定。

### HapticMode
- `OFF`：关闭触觉反馈。
- `SELECTION`：普通点击触觉反馈。
- `ALL`：普通点击和长按触觉反馈。

### LargeScreenLayoutMode
- `AUTO`：根据窗口特征自动选择。
- `BOTTOM`：固定底部布局。
- `SIDE`：侧边停靠布局。

### SideDockEdge
`LEFT` / `RIGHT`，侧边停靠时优先使用的边缘。

## 3. SymbolDrawerSettingsRepository

```kotlin
interface SymbolDrawerSettingsRepository {
    fun load(): SettingsLoadResult
    fun save(settings: SymbolDrawerSettings)
    fun reset()
    fun hasStoredSettings(): Boolean = false
}
```

- `load()`：返回设置快照和恢复状态。
- `save(settings)`：保存完整快照。
- `reset()`：清除持久化用户设置，恢复默认值。
- `hasStoredSettings()`：是否存在用户保存的设置；默认实现 false。

### SettingsLoadResult

```kotlin
data class SettingsLoadResult(
    val settings: SymbolDrawerSettings,
    val recoveredFromInvalidData: Boolean = false
)
```

`recoveredFromInvalidData=true` 表示本次加载发现非法持久化数据并返回默认设置。宿主可显示恢复提示；不要将其当成成功恢复原始用户设置。

## 4. SharedPreferencesSymbolDrawerSettingsRepository

```kotlin
SharedPreferencesSymbolDrawerSettingsRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2_settings"
)
```

默认 SharedPreferences 实现。数据 key：`settings_json_v1`。

- `load` 解码失败时返回默认设置并设置恢复标记。
- `save` 编码后使用 commit；失败抛异常。
- `reset` 删除设置 key；失败抛异常。
- `hasStoredSettings` 查询 key 是否存在。

## 5. SymbolDrawerSettingsCodec

版本化 JSON 编解码工具对象。

### encode(settings): String

序列化完整设置对象。

### decode(json): SymbolDrawerSettings

兼容本地历史快照的宽松读取方式：允许旧版本缺失字段，使用默认值补齐；拒绝未知字段、错误类型、非法枚举和不支持版本。解码后迁移到当前 schema 版本。

### decodeStrict(json): SymbolDrawerSettings

用于外部备份导入。要求 JSON 字段集合与对应 schema 版本严格匹配；旧 schema 版本允许其版本定义中不存在的后续字段缺失。字段多出、缺失或类型不匹配会抛出 `IllegalArgumentException`。

## 6. SymbolDrawerSettingsScreen

构造：

```kotlin
SymbolDrawerSettingsScreen(
    context: Context,
    settingsRepository: SymbolDrawerSettingsRepository = SharedPreferencesSymbolDrawerSettingsRepository(context),
    groupRepository: SymbolGroupRepository = SharedPreferencesSymbolGroupRepository(context)
)
```

程序化设置 View。可嵌入 Activity、Fragment 或 Dialog。

回调属性：

- `onSettingsChanged: ((SymbolDrawerSettings) -> Unit)?`：保存/导入/重置等变更成功后提供新快照。宿主通常连接 `drawer::applySettings`。
- `onGroupsChanged: ((List<SymbolGroup>) -> Unit)?`：涉及分组数据变更时提供快照。
- `onExportJson: ((String) -> Unit)?`：导出设置 JSON 后通知宿主。
- `onError: ((Throwable) -> Unit)?`：操作失败通知。

公开方法：

- `reload()`：重新从设置 Repository 加载并刷新 UI。
- `getSettings(): SymbolDrawerSettings`：返回当前屏幕编辑/加载的设置快照。

使用示例：

```kotlin
val screen = SymbolDrawerSettingsScreen(context)
screen.onSettingsChanged = drawer::applySettings
screen.onError = { error -> showError(error) }
screen.reload()
```

屏幕负责保存设置；宿主负责把新设置同步给已存在的 Drawer 实例。

## 7. 记忆展开状态和完整标题

### 记忆展开状态

`rememberExpandedState` 开启时：
- `setExpanded` 写入展开状态。
- View detach / 保存状态时更新记忆值。
- View attach 时恢复状态。
关闭时移除记忆 key，attach 后不恢复上一次展开状态。

该设置只记忆展开/折叠布尔值，不记忆任意中间拖动高度。

### 完整标题

`showFullSymbolLabels` 开启时，符号网格适配器使用完整标题显示策略；为了避免文本截断/省略，布局解析器可能降低列数或增加单元格所需宽度。完整标题与“强行保持原列数”并非同时保证；在窄窗口中优先保证标题可读性。

## 8. 运行时更新建议

- 使用完整快照更新，不要逐字段绕过 Repository。
- 批量变更设置时构造一次新 `SymbolDrawerSettings`，一次 save，再 apply。
- 对用户输入的 JSON 备份使用 `decodeStrict`。
- 不要直接编辑 SharedPreferences JSON key。
- 多个 Drawer 实例共享设置仓储时，实例会监听设置 key 的变化并重新读取；宿主仍可显式调用 `applySettings` 保证当前界面同步。
