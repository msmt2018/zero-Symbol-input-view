# 06 — Compose 设置模型与持久化

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.settings`

## 1. SymbolDrawerSettings

设置是不可变数据类。通过 `copy(...)` 创建新快照，不要原地修改。构造时会校验字段范围；当前设置覆盖主题、网格尺寸、分组指示器、符号标签、动画/拖动、触觉、性能及大屏停靠策略。

## 2. SymbolDrawerSettingsRepository

仓储契约：
- `load(): SettingsLoadResult`
- `save(settings)`
- `reset()`
- `hasStoredSettings()`

`SharedPreferencesSymbolDrawerSettingsRepository` 是默认 Android 持久化实现。它使用 applicationContext 和版本化 JSON；无效本地数据会回退默认值并通过 `recoveredFromInvalidData` 标记恢复状态。

## 3. SymbolDrawerSettingsCodec

- `encode(settings)`：编码当前设置。
- `decode(json)`：用于本地读取和旧 schema 迁移。
- `decodeStrict(json)`：用于外部备份导入，严格检查 schema 字段集合和类型。

不支持的 schema 版本、未知字段或字段类型错误会拒绝导入，不应静默丢弃数据。

## 4. SymbolDrawerSettingsScreen

当前入口是 Composable，而不是 View：

```kotlin
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null
)
```

页面编辑本地 Compose state，并在值变化时通过 `onSettingsChange` 发出新的不可变快照。宿主负责调用 Repository 保存，并将新设置应用到抽屉。重置操作通过可选 `onReset` 回调交给宿主。

## 5. 宿主集成边界

设置 Composable 不接收 Context、Activity、Fragment、View 或 LifecycleOwner。Android 持久化实现可以在组合外由宿主创建并注入；不要在每次重组时创建 Repository。设置状态的长期所有权属于宿主/Repository，而不是 Composition。

## 6. 失败处理

- 严格备份解码失败：保留当前设置，不调用保存。
- Repository 保存失败：宿主应报告错误，不应假装持久化成功。
- reset：只有仓储重置成功后，宿主才发布默认设置快照。