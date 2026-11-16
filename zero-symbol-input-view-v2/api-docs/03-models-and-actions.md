# 03 — 数据模型与动作类型 API

模型包：`com.zerostudio.idea.widget.editor.symbolinput.v2.model`

完整 import：

```kotlin
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
```

## 1. 设计原则

模型层描述“符号是什么”和“触发时想做什么”，不直接操作 Android Editor。所有数据模型均以 Kotlin data class/sealed interface 表达，适合构造不可变快照、比较差异和 JSON 持久化。

## 2. SymbolGroup

```kotlin
data class SymbolGroup(
    val id: String,
    val title: String,
    val symbols: List<SymbolEntry>
)
```

| 属性 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 稳定分组标识；仓储范围内唯一且不可空白 |
| `title` | `String` | 用户可见分组名称；Manager 要求非空白 |
| `symbols` | `List<SymbolEntry>` | 当前组符号顺序 |

**身份规则：** 重命名不应更换 ID；排序/分页使用 ID 识别组。删除后重新创建同名组应使用新 ID。

## 3. SymbolEntry

```kotlin
data class SymbolEntry(
    val id: Long,
    val label: String,
    val shortAction: SymbolAction,
    val longAction: SymbolAction? = null,
    val icon: String? = null,
    val legacyShortActionId: Int? = null,
    val legacyShortText: String? = null,
    val legacyLongActionId: Int? = null,
    val legacyLongText: String? = null
)
```

- `id`：符号稳定身份。Manager 和存储要求全局唯一且不能为 0。
- `label`：符号显示标题；不等于动作插入文本。
- `shortAction`：短按动作，必需。
- `longAction`：可选长按动作；null 表示无独立长按动作。
- `icon`：可选显示前缀/图标文本。
- `legacyShortActionId`、`legacyShortText`、`legacyLongActionId`、`legacyLongText`：旧版数字动作 ID 与文本参数的兼容元数据，供迁移、旧数据往返和兼容 UI 使用。新功能不应把这些字段作为 V2 主动作协议。

V2 当前 Manager UI 保留 V1 风格动作选择器，因此新建/编辑的部分动作可能同时保存 typed action 和 legacy metadata。

## 4. SymbolAction sealed interface

`SymbolAction` 是动作定义的封闭类型集合。执行逻辑由 `SymbolActionExecutor` 分派到宿主 `SymbolActionHost`。

| 类型 | 构造参数 | 语义 / 宿主映射 |
|---|---|---|
| `InsertText(template)` | 插入模板 | 在光标处插入模板，宏替换由具体宿主/文本工具支持 |
| `ReplaceSelection(text)` | 替换文本 | 用给定文本替换当前选区 |
| `SurroundSelection(prefix, suffix)` | 前后缀 | 用前缀和后缀包裹当前选区 |
| `MoveCursor(target)` | `CursorTarget` | 移动光标到指定方向/边界 |
| `Delete(kind)` | `DeleteKind` | 删除指定范围 |
| `TransformSelection(transform)` | `SelectionTransform` | 转换选区文本 |
| `EditorCommand(commandId, payload)` | 命令 ID、可选 payload | 调用宿主自定义/编辑器命令 |
| `Clipboard(operation)` | `ClipboardOperation` | 请求复制、剪切或粘贴 |
| `OpenManager` | 无 | 请求打开符号管理 UI |
| `Composite(actions)` | 子动作列表 | 按顺序执行多个动作 |
| `Extension(extensionId, payload)` | 扩展 ID、可选 payload | 调用注册扩展或宿主扩展回退 |

### InsertText

`template` 是待插入文本模板。执行器只负责把它交给 host 的 `insertText`；宏解析、光标占位符等行为应由宿主明确实现，不应假定所有 Editor adapter 自动支持。

### ReplaceSelection

`text` 是替换选区的文本。无选区时由宿主决定是插入还是返回 false。

### SurroundSelection

`prefix` 和 `suffix` 分别插入到选区两端。是否保留选区、是否重新选中包裹内容由宿主适配器定义。

### MoveCursor

接受 `CursorTarget`：

- `LEFT`、`RIGHT`：水平移动。
- `UP`、`DOWN`：垂直移动。
- `LINE_START`、`LINE_END`：当前行首尾。
- `DOCUMENT_START`、`DOCUMENT_END`：文档首尾。

### Delete

`DeleteKind`：

- `BACKWARD`：向后删除。
- `FORWARD`：向前删除。
- `CURRENT_LINE`：删除当前行。
- `TO_LINE_START`：删除到行首。
- `TO_LINE_END`：删除到行尾。

具体 Unicode code point、换行符和选区处理由宿主实现。

### TransformSelection

`SelectionTransform`：

- `UPPERCASE`
- `LOWERCASE`
- `TOGGLE_CASE`

### Clipboard

`ClipboardOperation`：`COPY`、`CUT`、`PASTE`。权限、剪贴板服务和空剪贴板行为由宿主决定。

### Composite

顺序执行子动作。执行器在开始调用 host 前会先校验整棵动作树，避免后续步骤格式无效时前面步骤已经执行。执行期间仍可能因 host 不支持、抛异常或取消而发生部分执行；结果会区分 `PartialFailure` 和 `Cancelled`。

### Extension

`extensionId` 是稳定的字符串标识，不能使用任意空白值。推荐以应用/插件命名空间组织 ID，例如 `com.example.editor.format`。不要把 lambda 序列化进数据；持久化 ID + payload，进程重建后重新注册 handler。

## 5. 动作相关枚举

### CursorTarget
`LEFT`、`RIGHT`、`UP`、`DOWN`、`LINE_START`、`LINE_END`、`DOCUMENT_START`、`DOCUMENT_END`。

### DeleteKind
`BACKWARD`、`FORWARD`、`CURRENT_LINE`、`TO_LINE_START`、`TO_LINE_END`。

### SelectionTransform
`UPPERCASE`、`LOWERCASE`、`TOGGLE_CASE`。

### ClipboardOperation
`COPY`、`CUT`、`PASTE`。

## 6. LegacySymbolAction

```kotlin
data class LegacySymbolAction(val type: Int, val payload: String? = null)
```

仅用于 V1 迁移和兼容转换。不要将它作为新 API 的主要存储或执行合同。数字 ID 的含义取决于旧版动作目录，不能跨版本自行重排 ID。

## 7. 符号 DrawerConfig（model 包）

注意：该类型与通用 Drawer 的 `drawer.config.DrawerConfig` 同名但完全不同。

```kotlin
data class DrawerConfig(
    val columns: Int = 10,
    val collapsedRows: Int = 2,
    val cellHeightDp: Int = 28,
    val cellHorizontalSpacingDp: Int = 2,
    val cellVerticalSpacingDp: Int = 2,
    val indicatorHeightDp: Int = 26,
    val animationDurationMs: Long = 200L,
    val flingVelocityDpPerSecond: Int = 900,
    val maxExpandedHeightDp: Int = 480,
    val minimumTouchTargetDp: Int = 28
)
```

| 字段 | 范围 | 说明 |
|---|---:|---|
| `columns` | 1..30 | 网格目标列数 |
| `collapsedRows` | 1..16 | 折叠状态显示行数 |
| `cellHeightDp` | 6..128 | 单元格高度 |
| `cellHorizontalSpacingDp` | -24..48 | 横向间距；负值表示允许重叠布局 |
| `cellVerticalSpacingDp` | -24..48 | 纵向间距 |
| `indicatorHeightDp` | 0..96 | 分组指示器高度 |
| `animationDurationMs` | 0..1200 | 展开/收起动画时长 |
| `flingVelocityDpPerSecond` | 100..6000 | fling 阈值 |
| `maxExpandedHeightDp` | 80..1800 | 展开高度上限 |
| `minimumTouchTargetDp` | 20..112 | 单元格最小触控目标 |

构造时会执行 `require` 校验，超范围抛出 `IllegalArgumentException`。更完整的用户设置由 `SymbolDrawerSettings` 管理，再通过 `toDrawerConfig()` 转换。

## 8. 数据示例

```kotlin
val symbol = SymbolEntry(
    id = 42L,
    label = "if",
    shortAction = SymbolAction.InsertText("if (4) {\n    \u00024\n}"),
    longAction = SymbolAction.SurroundSelection("if (", ") {\n\n}"),
    icon = null
)

val group = SymbolGroup(
    id = "control-flow",
    title = "控制流",
    symbols = listOf(symbol)
)
```

在 Kotlin 字符串中，美元符号宏标记需要按具体宿主宏协议处理；不要误把 Kotlin 模板插值和编辑器宏语法混为一谈。

## 9. 不可变性与复制规则

data class 的 `val` 属性并不自动深度冻结传入的 List。跨层传递时应使用 `toList()` 或不可变集合；仓储、Manager 和 View 会在各自边界建立必要快照，但调用方仍应避免共享可变集合。
