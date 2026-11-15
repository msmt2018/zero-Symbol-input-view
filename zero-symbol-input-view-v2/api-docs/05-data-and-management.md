# 05 — 仓储、符号管理与迁移 API

包：`com.zerostudio.idea.widget.editor.symbolinput.v2.data`

## 1. SymbolGroupRepository

```kotlin
interface SymbolGroupRepository {
    fun loadGroups(): List<SymbolGroup>
    fun saveGroups(groups: List<SymbolGroup>)
    fun isLegacyMigrationComplete(): Boolean = loadGroups().isNotEmpty()
    fun markLegacyMigrationComplete() = Unit
}
```

### loadGroups()

读取完整分组快照。建议返回新 List，且不要向调用方暴露数据库内部可变对象。

### saveGroups(groups)

保存完整快照。默认实现使用同步 SharedPreferences commit；若失败会抛异常。自定义实现应保证“成功返回”意味着后续读取可观察到已保存快照。

### isLegacyMigrationComplete()

迁移状态查询。默认实现以当前组数据非空作为判断；具体仓储可覆盖。

### markLegacyMigrationComplete()

记录迁移已完成。默认实现不执行操作；需要迁移标记的仓储应覆盖。

## 2. SharedPreferencesSymbolGroupRepository

```kotlin
SharedPreferencesSymbolGroupRepository(
    context: Context,
    preferencesName: String = "symbol_drawer_v2"
)
```

默认 V2 仓储。数据 key 为 `groups_json_v2`，迁移标记为 `legacy_migration_complete`。

行为：

- 从版本化 JSON 读取分组和 typed actions。
- 读取旧版 V1/早期 V2 数据后可规范化并写回当前格式。
- 若数据解码失败，尽可能先保留原始 JSON 到损坏备份 key，再返回空列表。
- `saveGroups` 编码后同步 commit；写入失败抛出异常。
- 不修改旧版 `advanced_symbol_prefs`。

损坏数据备份不是“自动修复原数据”；应用应在检测到恢复状态时告知用户并提供备份/重置入口。

## 3. SymbolGroupManager

Manager 是仓储之上的命令模型。每个成功命令创建新快照，校验后持久化一次，再更新内存 `groups`。写入失败时旧快照仍保留。

构造：

```kotlin
SymbolGroupManager(
    repository: SymbolGroupRepository,
    newId: () -> String = UUID,
    newSymbolId: () -> Long = UUID-derived
)
```

可注入 ID 生成器用于测试或业务 ID 体系。生成器必须提供足够随机/唯一性；Manager 最多尝试 32 次生成唯一 ID，失败会抛异常。

### groups

只读属性，当前 Manager 内存快照。不能直接赋值。

### reload()

从仓储重新读取并校验；只有验证通过才替换内存快照。仓储读取或校验失败时不会发布坏快照。

### addGroup(title, index = groups.size): SymbolGroup

新增空分组。标题不能空白；index 会 clamp 到合法插入位置。返回新建组。

### replaceAll(value)

用完整快照替换全部分组。执行全量验证后一次性持久化。

### duplicateGroup(groupId, index = groups.size): SymbolGroup

复制指定分组，标题追加 ` copy`，并为新组及每个复制符号分配新 ID。源组不存在时抛异常。

### renameGroup(groupId, title)

重命名分组。标题不可空白；分组不存在或标题非法时抛异常。

### removeGroup(groupId)

删除分组。若组已不存在则幂等 no-op，不执行冗余写入。

### moveGroup(fromIndex, toIndex)

在分组列表内移动位置。两个 index 必须有效，否则抛异常。

### addSymbol(groupId, symbol, index = null)

向指定组添加符号。若 symbol.id 为 0，Manager 自动分配新 ID；否则检查它不能与其他组符号 ID 冲突。index 默认追加，越界时 clamp。

### updateSymbol(groupId, symbol)

按 symbol.id 更新现有符号。要求该组已存在该 ID，且该 ID 不与其他组冲突。不能用此方法把一个组的符号“移动”到另一个组。

### removeSymbol(groupId, symbolId)

删除单个符号。组或符号不存在时抛异常。

### moveSymbol(groupId, fromIndex, toIndex)

在同一分组内调整顺序。索引必须有效。

### copySymbol(sourceGroupId, symbolId, targetGroupId): SymbolEntry

复制单个符号到目标组，生成新 ID，返回副本。

### copySymbols(sourceGroupId, symbolIds, targetGroupId): List<SymbolEntry>

批量复制。要求选择集合非空、源/目标组存在、所有被选 ID 仍存在。单次事务提交；返回新建符号。

### removeSymbols(groupId, symbolIds): Int

批量删除。集合非空且所有 ID 均存在，否则拒绝整个操作。返回删除数量。

### moveSymbolsToGroup(sourceGroupId, symbolIds, targetGroupId)

批量跨组移动。源目标必须不同，集合非空，所有 ID 必须存在。移动时保留可用 ID；若发现冲突则重新分配。单次提交。

### moveSymbolToGroup(sourceGroupId, symbolId, targetGroupId)

单个跨组移动的便捷方法，内部委托批量移动。

### reorderSymbols(groupId, orderedIds)

按稳定 ID 重排。传入列表必须包含该组全部符号 ID，且每个 ID 恰好一次；缺少、多余或重复 ID 都会抛异常。

## 4. 事务与错误语义

Manager 的 commit 顺序：

1. 比较新旧快照；完全相同则 no-op。
2. 验证组 ID、标题、符号 ID 和 label。
3. 将新快照写入 Repository。
4. 写入成功后替换内存 `groups`。

因此：
- Repository 写入失败：抛异常，内存快照不更新。
- 参数错误/目标不存在/重复 ID：抛 `IllegalArgumentException` 或 `IllegalStateException`（取决于具体检查）。
- 删除不存在的 group 是特例：静默 no-op。
- 批量方法要求所有选择项仍存在，防止部分成功。

## 5. SymbolDrawerDefaults

`SymbolDrawerDefaults.createGroups()` 提供默认符号分组数据，用于首次初始化。调用者可复制返回值后按业务需求修改；默认数据不是永久锁定的系统保留数据。

## 6. JSON Codec

### SymbolGroupStorageCodec

对分组、符号、typed action 和 legacy metadata 执行版本化 JSON 编解码。由默认仓储调用；直接使用时应把解码异常当作不可信/损坏输入处理。

### SymbolGroupBundleCodec

用于便携式备份包导入导出：

- `encode(groups)`：生成 JSON bundle。
- `decode(json)`：解析并校验 bundle，返回分组快照。

Manager Screen 的 `exportBundle()` / `importBundle(json)` 是面向 UI 的入口；Codec 是纯数据边界。

## 7. 旧版迁移

### LegacySymbolDataMigration

`importIfV2Empty(context, repository)` 在 V2 仓储为空时尝试读取旧版偏好数据并转换。迁移是单向复制：

- 源：旧版 `advanced_symbol_prefs/symbol_json_data`。
- 目标：V2 Repository。
- 旧版数据不会被删除或改写。
- 旧 schema 没有稳定 ID，因此迁移时会分配确定性/生成的 ID。
- 动作 ID 和 payload 作为兼容元数据保留。

不要在每次页面刷新时手动重复执行迁移；默认 `SymbolDrawerView` 初始化流程会调用迁移适配器。

### LegacySymbolActionAdapter

负责将旧数字动作模型转换成 typed `SymbolAction`。未知数字动作应保留为扩展/兼容动作，不要把未知值映射成一个猜测的内置动作。

### LegacySymbolActionCatalog

旧动作 ID、名称和 payload 行为目录。主要供迁移及 Manager 的 V1 风格选择器使用。ID 顺序是兼容合同，不可随意重排或复用。

## 8. 自定义数据库仓储建议

- 将 `saveGroups` 包装在数据库事务中。
- 保存成功后再返回。
- 使用唯一约束保证组 ID 和符号 ID。
- JSON 导入前先完整 decode/validate，再执行替换事务。
- 保留失败导入的原始备份。
- 避免在主线程进行大型数据库写入；如需异步仓储，应在 View 更新前协调结果，避免旧快照覆盖新快照。
