# 11 — 手势交互控制器与通用内置文本动作

## 1. DrawerInteractionController

包：`drawer.core`。

这是 UI 布局无关的连续拖动/settle 引擎。它不创建 View、不测量内容、不决定符号网格几何；功能 View 传入 min/max/current height，并在回调中更新自己的布局。

构造：

```kotlin
DrawerInteractionController(
    context: Context,
    density: Float = context.resources.displayMetrics.density,
    onHeightChanged: (heightPx: Int, isDragging: Boolean, isAnimating: Boolean) -> Unit,
    onDisallowInterceptChanged: (Boolean) -> Unit = {}
)
```

回调：
- `onHeightChanged`：高度或拖动/动画状态变化时通知宿主。
- `onDisallowInterceptChanged`：请求父容器允许/禁止拦截当前手势。

只读属性：
- `currentHeightPx`：当前高度。
- `isDragging`：是否正在直接拖动。
- `isAnimating`：settle 动画是否运行。
- `isGestureAxisLocked`：触摸方向是否已锁定。
- `isHorizontalGestureLocked`：当前是否锁定为横向手势。

### setBounds(minimumHeightPx, maximumHeightPx, currentHeightPx, animationDurationMs, flingVelocityDpPerSecond)

更新宿主几何和交互参数，不改变当前高度之外的展开比例语义。要求 min >= 0、max >= min、动画时长非负、fling 速度有限且非负。

### synchronizeHeight(heightPx)

取消当前动画并将内部高度同步到宿主已恢复/已测量的高度，同时通知高度变化回调。

### onAttached() / onDetached()

宿主 View attach/detach 时调用。detach 会取消动画并重置手势；不要把 controller 绑定到已销毁 View 后继续使用。

### onInterceptTouchEvent(event, childCanScrollUp): Boolean

在 ViewGroup 的拦截阶段调用。内部按 touch slop 判断横向/纵向：
- 横向锁定后不拦截，交给子 Pager。
- 纵向锁定后根据高度边界和子滚动容器是否还能向上滚动判断是否接管。
- 当接管发生时返回 true，后续事件由 drawer 接收。
- ACTION_UP/CANCEL 会重置手势状态。

childCanScrollUp(x,y) 应针对触摸坐标对应的子 View 返回其是否还能向上滚动。

### onTouchEvent(event): Boolean

仅用于处理已被控制器接管的触摸序列。MOVE 更新高度；UP 根据速度或当前展开比例 settle；CANCEL 按展开比例 settle。不要在未接管时强行把事件传给它以取代子控件正常触摸流程。

### expand(animated = true) / collapse(animated = true)

分别移动到 max/min height。

### settleToHeight(heightPx, animated = true)

将目标高度 clamp 到 min/max 后 settle。

### cancelAnimation()

取消当前 ValueAnimator；若动画正在运行，会通知状态更新。

### resetGesture()

清理 VelocityTracker、方向锁定和 dragging 标志，并允许父容器重新拦截。

### shouldInterceptVerticalDrag(...)

静态共享策略：

```kotlin
DrawerInteractionController.shouldInterceptVerticalDrag(
    fingerDeltaY,
    currentHeightPx,
    collapsedHeightPx,
    expandedHeightPx,
    childCanScrollUp
)
```

- 没有有效高度范围或 finger delta 为 0：false。
- 手指向上拖（负 delta）：只要当前未到最大高度即可由 Drawer 接管。
- 手指向下拖（正 delta）：只有 Drawer 高于折叠高度且子 View 已不能向上滚动时才接管。

## 2. SymbolDrawerDragPolicy

符号抽屉的兼容策略入口，内部用于判定纵向拖动能否被抽屉接管。新宿主一般不需要直接调用；如需独立测试，请优先使用 `DrawerInteractionController.shouldInterceptVerticalDrag` 共享策略。

## 3. SymbolDrawerAnchorAdapter

见 [07 — 通用 Drawer 框架](07-universal-drawer.md)。它把符号抽屉现有像素几何转成锚点 API，但不改变符号抽屉内部布局。

## 4. BuiltinSymbolActionCatalog

包：`action`。

`BuiltinSymbolActionOption` 描述一个可由 Manager 选择的内置命令：
- `commandId`：稳定命令字符串。
- `label`：用户可见名称。
- `payloadHint`：可选参数输入提示。
- `hasPayload`：是否需要/允许 payload（以 option 实际声明为准）。

`BuiltinSymbolActionCatalog.find(commandId)` 根据稳定命令 ID 返回 option；未找到返回 null。目录项包括编辑器常用撤销、重做、缩进、反缩进、格式化，以及 GenericSymbolTextActions 所支持的行/选区/光标/文档命令。Catalog 是 Manager 的选择器目录，不保证每个宿主都实现所有命令。

## 5. GenericSymbolTextActions

通用文本命令集，通过 `SymbolTextDocument` 工作，不依赖具体编辑器。

### createRegistry(): SymbolActionRegistry<SymbolTextDocument>

创建一份新的注册表并注册所有通用命令。每次调用返回独立注册表，不共享 handler 生命周期。

### supportedCommandIds(): Set<String>

返回当前通用命令 ID 集合的只读视图/快照。宿主可用于能力展示或测试。

### 命令 ID 清单

以下 ID 在当前实现中由通用文本动作处理器注册。是否可用还取决于文档是否可编辑、是否有选区、是否处于文件边界以及操作所需的 payload。

**行操作**
- `line.duplicate`：复制当前行。
- `line.delete`：删除当前行。
- `line.clear`：清空当前行。
- `line.replace`：用 payload 替换当前行。
- `line.insertAbove` / `line.insertBelow`：在当前行上方/下方插入空行。
- `line.joinNext`：与下一行合并。
- `line.moveUp` / `line.moveDown`：移动当前行。
- `line.select`：选择当前行。
- `line.uppercase` / `line.lowercase` / `line.toggleCase`：转换当前行大小写。
- `line.trimTrailingWhitespace` / `line.trimLeadingWhitespace`：清理当前行行尾/行首空白。
- `line.duplicateAbove`：在当前行上方复制。
- `line.toggleComment`：注释/取消注释当前行或选中行；payload 为注释标记，默认 `//`。
- `editor.toggleLineComment`：同类注释命令别名。
- `line.indent`：当前行缩进；payload 默认 Tab。
- `line.unindent`：当前行反缩进。

**选区操作**
- `selection.delete`、`selection.duplicate`。
- `selection.uppercase`、`selection.lowercase`、`selection.toggleCase`。
- `selection.wrapParentheses`、`selection.wrapBrackets`、`selection.wrapBraces`。
- `selection.wrapQuotes`、`selection.wrapSingleQuotes`、`selection.wrapBackticks`。
- `selection.wrapAngleBrackets`、`selection.wrapAsterisks`、`selection.wrapUnderscores`。
- `selection.wrapHtmlTag`：payload 为合法标签名。
- `selection.indentLines`：payload 为缩进字符串，默认 Tab。
- `selection.unindentLines`。
- `selection.reverseLines`、`selection.sortLines`。
- `selection.comment`：payload 为注释标记，默认 `//`。

**光标/单词操作**
- `cursor.wordLeft` / `cursor.wordRight`。
- `cursor.deleteWordBackward` / `cursor.deleteWordForward`。
- `cursor.selectWord` / `cursor.selectLine`。
- `cursor.lineStart` / `cursor.lineEnd`。
- `cursor.selectToLineStart` / `cursor.selectToLineEnd`。
- `cursor.selectToDocumentStart` / `cursor.selectToDocumentEnd`。
- `cursor.documentStart` / `cursor.documentEnd`。

**文档操作**
- `document.selectAll`。
- `document.trimTrailingWhitespace` / `document.trimLeadingWhitespace`。
- `document.ensureFinalNewline`：payload 可指定 `\n`、`\r` 或 `\r\n`，默认 LF。
- `document.removeBlankLines`。
- `document.toggleComment`：payload 为注释标记，默认 `//`。

**通用插入**
- `insert_payload`：把 payload 插入/替换当前选区。

### 文档大小保护

文档级全量遍历操作限制：
- 最大文本长度 32 MiB。
- 最大行数 200,000。

超过限制时相关操作返回 false，不会尝试无限制全量处理。行级/选区级命令是否受限取决于具体命令实现。

## 6. SymbolTextEditing

纯文本计算工具，不直接持有 Android View：
- `expandMacro(template, selectedText)`：解析模板宏并返回展开信息。
- `codePointDeletionRange(text, cursorOffset, backwards)`：计算 Unicode code point 删除区间。
- `duplicateLineInsertion(text, cursorOffset)`：计算复制行插入结果。

具体返回数据类型为 `SymbolTextMacroExpansion`、`SymbolTextRange`、`SymbolTextInsertion`。调用方负责把计算结果写回自己的文档模型。

## 7. 实现自定义文本 adapter 的约束

1. offset 统一使用 Kotlin String UTF-16 索引语义。
2. `lineStartOffset`、`lineEndOffset`、`lineForOffset` 的边界定义必须一致。
3. `replace` 返回 false 表示拒绝/失败，不应悄悄吞掉半次替换。
4. 每次 replace 后由动作实现显式设置 selection/caret。
5. 对只读文档，所有修改方法应返回 false。
6. 大文档遍历操作应遵守 32 MiB / 200,000 行保护策略，或提供等价的安全限制。
