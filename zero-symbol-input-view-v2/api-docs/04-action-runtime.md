# 04 — 动作注册、执行与文本编辑 API

**当前源码包路径**

- 执行宿主、执行器、结果、取消、注册表和文本文档 API：`com.zerostudio.idea.widget.drawer.action`。
- `SymbolAction` 及 `CursorTarget`、`DeleteKind`、`SelectionTransform`、`ClipboardOperation` 模型：`com.zerostudio.idea.widget.editor.symbolinput.v2.model`。
- 符号产品兼容 facade、`SymbolActionJsonCodec`、`BuiltinSymbolActionCatalog` 等：`com.zerostudio.idea.widget.editor.symbolinput.v2.action`。

```kotlin
import com.zerostudio.idea.widget.drawer.action.SymbolActionExecutor
import com.zerostudio.idea.widget.drawer.action.SymbolActionHost
import com.zerostudio.idea.widget.drawer.action.SymbolActionResult
import com.zerostudio.idea.widget.drawer.action.SymbolActionCancellation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
```

## 1. 运行模型

`SymbolAction` 是数据，`SymbolActionExecutor` 是解释器，`SymbolActionHost` 是宿主能力边界。Executor 不引用 Sora Editor、EditText 或具体 IDE API。

```text
SymbolEntry.shortAction / longAction
              |
              v
      SymbolActionExecutor
              |
              v
       SymbolActionHost
              |
              v
     Host Editor / Clipboard
```

## 2. SymbolActionHost

宿主必须实现的同步接口：

```kotlin
interface SymbolActionHost {
    fun insertText(template: String): Boolean
    fun replaceSelection(text: String): Boolean
    fun surroundSelection(prefix: String, suffix: String): Boolean
    fun moveCursor(target: CursorTarget): Boolean
    fun delete(kind: DeleteKind): Boolean
    fun transformSelection(transform: SelectionTransform): Boolean
    fun executeEditorCommand(commandId: String, payload: String?): Boolean
    fun performClipboard(operation: ClipboardOperation): Boolean
    fun openManager(): Boolean
    fun executeExtension(extensionId: String, payload: String?): Boolean
}
```

所有方法同步调用，返回值含义：

- true：宿主已接受并完成该动作。
- false：当前上下文不支持、不可执行或宿主主动拒绝。
- 抛出异常：执行器捕获普通 Exception，并返回 `SymbolActionResult.Failed`。

**线程：** Executor 不切换线程。若编辑器只能在主线程操作，调用方必须在主线程执行。不要在宿主方法中阻塞等待异步编辑器操作后再返回；当前接口没有异步结果协议。

### 方法职责

| 方法 | 对应动作 | 实现要求 |
|---|---|---|
| `insertText(template)` | InsertText | 插入模板文本 |
| `replaceSelection(text)` | ReplaceSelection | 替换当前选区 |
| `surroundSelection(prefix,suffix)` | SurroundSelection | 包裹选区 |
| `moveCursor(target)` | MoveCursor | 根据枚举定位光标 |
| `delete(kind)` | Delete | 执行指定删除策略 |
| `transformSelection(transform)` | TransformSelection | 转换选区文本 |
| `executeEditorCommand(commandId,payload)` | EditorCommand | 路由到编辑器命令表 |
| `performClipboard(operation)` | Clipboard | 处理剪贴板 |
| `openManager()` | OpenManager | 导航到管理界面 |
| `executeExtension(id,payload)` | Extension | 执行扩展动作 |

宿主实现应避免对未知 command ID 进行反射调用或任意脚本执行。建议使用白名单注册表。

## 3. SymbolActionCancellation

```kotlin
fun interface SymbolActionCancellation {
    fun isCancelled(): Boolean

    companion object {
        val NEVER: SymbolActionCancellation
    }
}
```

执行器在动作步骤之间检查取消状态。取消不是强制中断正在运行的 host 方法；host 调用同步返回后才会检查下一步。默认 `NEVER` 永不取消。

## 4. SymbolActionResult

sealed interface，执行结果：

| 结果 | 含义 |
|---|---|
| `Executed` | 整个动作成功 |
| `Cancelled(completedActions)` | 在执行中被取消，已完成动作数 |
| `Unsupported(action)` | host 对该动作返回 false |
| `Invalid(reason)` | 动作树/ID/参数/限制不合法 |
| `Failed(cause)` | host 执行抛出异常 |
| `PartialFailure(completedActions, cause)` | Composite 前面若干步骤成功，后续步骤失败 |

注意：Composite 不是事务回滚。执行器可在开始前完整验证结构，但不可能自动撤销已成功执行的编辑器操作。

## 5. SymbolActionExecutor

构造：

```kotlin
SymbolActionExecutor(
    maxCompositeActions: Int = 64,
    maxNestingDepth: Int = 8,
    maxTextLength: Int = 1_048_576,
    maxTotalActions: Int = 256
)
```

参数范围：

- `maxCompositeActions`：1..256。
- `maxNestingDepth`：1..16。
- `maxTextLength`：1..4,194,304 个 Kotlin Char。
- `maxTotalActions`：1..4,096。

超范围构造会抛 `IllegalArgumentException`。

### execute

```kotlin
fun execute(
    action: SymbolAction,
    host: SymbolActionHost,
    cancellation: SymbolActionCancellation = SymbolActionCancellation.NEVER
): SymbolActionResult
```

执行顺序：

1. 校验整个动作树的最大深度、动作数、文本长度、命令 ID 和扩展 ID。
2. 若验证失败，返回 `Invalid`，不调用 host。
3. 逐项同步调用 host。
4. Composite 按原顺序执行。
5. 在每个动作开始前检查取消状态。
6. 将 host 返回 false 映射为 `Unsupported`。
7. 将普通 Exception 映射为 `Failed`；Composite 中已有成功步骤时包装为 `PartialFailure`。

执行器不捕获 JVM Error。不要把它作为隔离不可信代码的安全沙箱。

## 6. SymbolActionId

```kotlin
data class SymbolActionId(val namespace: String, val value: String)
```

两个字段均必须匹配：

```regex
[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}
```

工厂方法：

- `SymbolActionId.legacy(numericId: Int)`：构造 namespace=`legacy` 的数字 ID。
- `SymbolActionId.extension(extensionId: String)`：构造 namespace=`extension` 的扩展 ID。

命名空间用于区分内置动作、旧版数字动作和扩展动作。ID 是持久化身份，不应因 handler 重建而改变。

## 7. SymbolActionHandler

```kotlin
fun interface SymbolActionHandler<C> {
    fun execute(context: C, payload: String?): Boolean
}
```

泛型 `C` 是注册表使用者提供的执行上下文。handler 不应持有比其注册生命周期更长的 Activity/View 引用。

## 8. SymbolActionRegistry

`SymbolActionRegistry<C>` 是线程安全的进程内 handler 注册表，不负责持久化。

### register(id, handler)

注册或替换 handler，返回 `AutoCloseable`。

- 同一个 ID 再注册会替换当前 handler。
- 返回句柄 close 时，仅当该注册仍是当前版本才删除，避免旧句柄误删后来注册的 handler。
- 建议把句柄保存在 Activity/Fragment/EditorSession 生命周期中，并在结束时 close。

### register(legacyNumericId, handler)

数字 ID 便捷重载，内部映射到 `SymbolActionId.legacy`。

### get(id) / get(legacyNumericId)

获取 handler；不存在时返回 null。只返回 handler 引用，不执行。

### execute(id, context, payload) / execute(legacyNumericId, context, payload)

执行 handler。返回：

- null：没有注册 handler。
- true：handler 接受并完成。
- false：handler 已存在但拒绝/无法处理。

null 与 false 必须区分。false 不代表“没有注册”。

### unregister(id)

删除当前 ID，返回是否实际删除。

### clear()

清空全部 handler。仅在注册表生命周期确实结束时调用；共享注册表中不要随意清空其他模块的动作。

### size

当前注册 handler 数量。

## 9. RegistryBackedSymbolActionHost

装饰现有 `SymbolActionHost`：

```kotlin
RegistryBackedSymbolActionHost(
    delegate: SymbolActionHost,
    registry: SymbolActionRegistry<SymbolActionHost>,
    fallback: (String, String?) -> Boolean = { id, payload ->
        delegate.executeExtension(id, payload)
    }
)
```

其余基础动作全部委托给 delegate。扩展动作分派规则：

1. 对 `legacy.action.<integer>` 形式，先尝试 legacy 数字注册表。
2. 再尝试字符串扩展 ID 注册表。
3. 只有 handler 未注册（返回 null）才执行 fallback。
4. handler 已注册并返回 false 时，false 是最终结果，不再 fallback，防止同一动作执行两次。

## 10. SymbolActionJsonCodec

对象：`SymbolActionJsonCodec`

### encode(action): JsonObject

将动作编码为带 `schemaVersion` 和 `kind` discriminator 的 JSON 对象。当前 schema version 为 1。Composite 递归编码子动作。

### decode(json, depth = 0): SymbolAction

解码 typed action。校验字段类型、枚举值、命令/扩展 ID、嵌套深度、总节点数和文本字段大小。缺失 schemaVersion 按 V1 schema 读取；未知 schema version 会抛 `IllegalArgumentException`。

限制：

- 最大嵌套深度：8。
- Composite 子动作数：1..64。
- 动作树节点总数：最多 256。
- 文本字段最大长度：1,048,576。
- Editor command ID：`[a-zA-Z][a-zA-Z0-9_.-]{0,127}`。
- Extension ID：`[a-zA-Z0-9][a-zA-Z0-9_.:/-]{0,255}`。

不要把任意用户提供的 JSON 直接交给反射/脚本执行；Codec 只负责数据校验和构造类型。

## 11. SymbolTextDocument

面向通用文本编辑操作的抽象接口：

```kotlin
interface SymbolTextDocument {
    val textLength: Int
    val lineCount: Int
    val cursorOffset: Int
    val selectionStart: Int
    val selectionEnd: Int
    val isEditable: Boolean
    fun lineForOffset(offset: Int): Int
    fun lineStartOffset(line: Int): Int
    fun lineEndOffset(line: Int): Int
    fun lineText(line: Int): String
    fun textBetween(start: Int, end: Int): String
    fun charAt(offset: Int): Char
    fun replace(start: Int, end: Int, replacement: String): Boolean
    fun setSelection(start: Int, end: Int)
}
```

宿主 adapter 应确保 offset 单位与 Kotlin String 索引一致（UTF-16 code unit），并对所有 range 做边界检查。若底层 Editor 使用不同 offset 语义，必须在 adapter 中转换。

## 12. SymbolTextEditing

工具对象提供纯文本/文档操作辅助。公开数据类型：

- `SymbolTextMacroExpansion`：宏展开结果及其选区/光标信息。
- `SymbolTextRange(start, end)`：半开区间文本范围。
- `SymbolTextInsertion`：插入文本及光标/选区位置描述。

公开工具函数：

- `expandMacro(template, selectedText)`：展开 V1 风格模板宏，返回展开文本与相关标记位置。
- `codePointDeletionRange(text, cursorOffset, backwards)`：计算按 Unicode code point 删除的范围，避免把代理对拆开。
- `duplicateLineInsertion(text, cursorOffset)`：计算复制当前行所需插入内容及位置。

这些工具返回计算结果，不会自动写入 Editor。调用方需使用结果更新文档并设置选择区。完整宏语法以该文件实现为准；与 Kotlin 字符串模板转义无关。


## SymbolTextDocumentActionHost

`SymbolTextDocumentActionHost` 是 V2 提供的默认编辑器中立动作宿主。开发者实现 `SymbolTextDocument` 后，可以直接复用通用插入、选区、光标、删除、大小写转换和 GenericSymbolTextActions 命令，无需自己再实现完整的 `SymbolActionHost`。

构造：

```kotlin
SymbolTextDocumentActionHost(
    document: SymbolTextDocument,
    clipboard: SymbolTextClipboard = ...,
    openManagerAction: () -> Boolean = ...,
    extensionAction: (String, String?) -> Boolean = ...
)
```

- `document`：当前编辑器文档 Adapter。
- `clipboard`：剪贴板能力注入。默认返回 false；实际剪贴板仍由宿主实现。
- `openManagerAction`：打开宿主管理页的回调，默认返回 false。
- `extensionAction`：未被扩展注册表接管时的扩展动作 fallback。

方法映射：
- `insertText`：解析 V1 兼容宏 `$S/$E/$T/$$)，替换当前选区并恢复插入/选区标记。
- `replaceSelection`：替换当前选区并将光标置于插入文本末尾。
- `surroundSelection`：用 prefix/suffix 包围选区，并保持内部文本为选中状态；空选区返回 false。
- `moveCursor`：实现左右 Unicode code point 移动、上下行同列移动、行首/行尾和文档首尾。
- `delete`：实现选区删除、向前/向后 code point 删除、当前行删除、删除到行首/行尾。
- `transformSelection`：实现大写、小写和切换大小写。
- `executeEditorCommand`：使用 `GenericSymbolTextActions.createRegistry()` 查找 `builtin` namespace 命令。
- `performClipboard` / `openManager` / `executeExtension`：调用构造参数注入的宿主回调。

该实现假定 `SymbolTextDocument` 遵守 UTF-16 offset、半开区间和至少一行文档约定。对空文档/无效行状态会拒绝无法安全执行的操作。剪贴板、Manager UI、编辑器专属命令仍归宿主所有。
