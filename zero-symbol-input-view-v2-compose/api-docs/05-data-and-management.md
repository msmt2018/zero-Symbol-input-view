# 05 — 分组仓储、领域管理与数据迁移

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.data`

## 1. SymbolGroupRepository

`SymbolGroupRepository` 是分组数据的持久化边界：

```kotlin
interface SymbolGroupRepository {
    fun loadGroups(): List<SymbolGroup>
    fun saveGroups(groups: List<SymbolGroup>)
    fun isLegacyMigrationComplete(): Boolean
    fun markLegacyMigrationComplete()
}
```

`ObservableSymbolGroupRepository` 额外提供 `observeGroups(listener): AutoCloseable`，用于共享仓储变更通知。通知句柄由订阅方显式关闭。

## 2. SharedPreferencesSymbolGroupRepository

默认实现将版本化 JSON 保存到应用级 SharedPreferences。它使用 applicationContext，读入时解码并规范化旧存储；损坏数据会保留备份与错误原因，不会在首次读取失败时静默覆盖原始 JSON。

仓储同步读写；大型或高频写入场景可实现自定义 Repository，但必须保持校验、持久化成功后再发布快照的语义。

## 3. SymbolGroupManager

这是 UI 中立的命令/领域层，不是页面或 ViewModel。它读取 Repository 快照，校验数据，并对每条命令生成不可变快照；先成功持久化，再更新内存状态。

主要命令：
- 分组：`addGroup`、`duplicateGroup`、`renameGroup`、`removeGroup`、`moveGroup`、`replaceAll`、`reload`。
- 符号：`addSymbol`、`updateSymbol`、`removeSymbol`、`moveSymbol`、`copySymbol`。
- 批量：`copySymbols`、`removeSymbols`、`moveSymbolsToGroup`、`reorderSymbols`。

批量命令先验证所选 ID 与目标分组，再以单次 commit 写入。符号 ID 在整个仓储范围内唯一；复制时生成新 ID，移动时在冲突情况下重新分配 ID。

## 4. 编码与迁移

- `SymbolGroupStorageCodec`：当前仓储 envelope 的编码/解码和结构校验。
- `SymbolGroupBundleCodec`：备份/导入 bundle 编解码及大小、动作树限制。
- `LegacySymbolDataCodec` / `LegacySymbolDataMigration`：读取旧数据并迁移到当前领域模型。
- `LegacySymbolActionAdapter` / `LegacySymbolActionCatalog`：适配旧数字动作 ID 与动作 payload。

外部 JSON 必须完整解码和校验后才能替换现有数据。导入失败不能先清空现有分组。

## 5. Compose Manager UI

`SymbolDrawerManagerScreen(repository, modifier, onClose)` 是纯 Compose 页面。它使用 `SymbolGroupManager` 提交命令，并通过 Compose state 展示分组/符号快照。UI 不拥有存储格式或迁移逻辑；不需要传入 Context、Activity、Fragment 或 View。

当前界面提供分组增删改、复制、排序、符号增删改、批量复制/移动/删除和 JSON 剪贴板导入导出。需要文件选择器或云同步时，由宿主在页面外提供相应能力，不应把文件 IO 混入领域管理器。

## 6. 事务与失败语义

- 输入先校验，再构建下一份快照。
- 持久化成功后才发布新快照。
- 校验或写入失败时保留原内存状态。
- 相同快照是 no-op，不产生冗余写入。
- 对共享 Repository 的外部更新，调用方可重新加载并同步 Compose state。