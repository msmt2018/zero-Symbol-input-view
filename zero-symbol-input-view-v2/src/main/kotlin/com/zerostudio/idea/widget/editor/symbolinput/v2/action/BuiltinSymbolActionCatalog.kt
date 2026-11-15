package com.zerostudio.idea.widget.editor.symbolinput.v2.action

/** A user-facing, editor-neutral action that can be persisted as EditorCommand. */
data class BuiltinSymbolActionOption(
    val commandId: String,
    val label: String,
    val payloadHint: String? = null
) {
    val hasPayload: Boolean get() = payloadHint != null
}

/** Stable V2 action palette; V1 numeric entries remain separately available. */
object BuiltinSymbolActionCatalog {
    val options: List<BuiltinSymbolActionOption> = listOf(
        BuiltinSymbolActionOption("line.duplicate", "行：复制当前行"),
        BuiltinSymbolActionOption("line.delete", "行：删除当前行"),
        BuiltinSymbolActionOption("line.clear", "行：清空当前行"),
        BuiltinSymbolActionOption("line.replace", "行：替换当前行", "输入替换后的整行文本"),
        BuiltinSymbolActionOption("line.insertAbove", "行：上方插入空行"),
        BuiltinSymbolActionOption("line.insertBelow", "行：下方插入空行"),
        BuiltinSymbolActionOption("line.joinNext", "行：合并下一行"),
        BuiltinSymbolActionOption("line.moveUp", "行：上移"),
        BuiltinSymbolActionOption("line.moveDown", "行：下移"),
        BuiltinSymbolActionOption("line.select", "行：选择当前行"),
        BuiltinSymbolActionOption("line.uppercase", "行：转为大写"),
        BuiltinSymbolActionOption("line.lowercase", "行：转为小写"),
        BuiltinSymbolActionOption("line.toggleCase", "行：切换大小写"),
        BuiltinSymbolActionOption("line.trimTrailingWhitespace", "行：删除行尾空白"),
        BuiltinSymbolActionOption("line.trimLeadingWhitespace", "行：删除行首空白"),
        BuiltinSymbolActionOption("line.duplicateAbove", "行：向上复制当前行"),
        BuiltinSymbolActionOption("line.toggleComment", "行：注释/取消注释", "注释符号，默认 //"),
        BuiltinSymbolActionOption("editor.toggleLineComment", "编辑器：切换行注释", "注释符号，默认 //"),
        BuiltinSymbolActionOption("line.indent", "行：缩进", "缩进文本，默认 Tab"),
        BuiltinSymbolActionOption("line.unindent", "行：反缩进"),
        BuiltinSymbolActionOption("selection.delete", "选区：删除"),
        BuiltinSymbolActionOption("selection.duplicate", "选区：复制到后方"),
        BuiltinSymbolActionOption("selection.uppercase", "选区：转为大写"),
        BuiltinSymbolActionOption("selection.lowercase", "选区：转为小写"),
        BuiltinSymbolActionOption("selection.toggleCase", "选区：切换大小写"),
        BuiltinSymbolActionOption("selection.wrapParentheses", "选区：圆括号包裹"),
        BuiltinSymbolActionOption("selection.wrapBrackets", "选区：方括号包裹"),
        BuiltinSymbolActionOption("selection.wrapBraces", "选区：花括号包裹"),
        BuiltinSymbolActionOption("selection.wrapQuotes", "选区：双引号包裹"),
        BuiltinSymbolActionOption("selection.wrapSingleQuotes", "选区：单引号包裹"),
        BuiltinSymbolActionOption("selection.wrapBackticks", "选区：反引号包裹"),
        BuiltinSymbolActionOption("selection.wrapAngleBrackets", "选区：尖括号包裹"),
        BuiltinSymbolActionOption("selection.wrapAsterisks", "选区：星号包裹"),
        BuiltinSymbolActionOption("selection.wrapUnderscores", "选区：下划线包裹"),
        BuiltinSymbolActionOption("selection.wrapHtmlTag", "选区：HTML 标签包裹", "标签名称，例如 span"),
        BuiltinSymbolActionOption("selection.indentLines", "选区：多行缩进", "缩进文本，默认 Tab"),
        BuiltinSymbolActionOption("selection.unindentLines", "选区：多行反缩进"),
        BuiltinSymbolActionOption("selection.reverseLines", "选区：反转行顺序"),
        BuiltinSymbolActionOption("selection.sortLines", "选区：按字典序排列行"),
        BuiltinSymbolActionOption("selection.comment", "选区：注释/取消注释", "注释符号，默认 //"),
        BuiltinSymbolActionOption("cursor.wordLeft", "光标：移动到上一个单词"),
        BuiltinSymbolActionOption("cursor.wordRight", "光标：移动到下一个单词"),
        BuiltinSymbolActionOption("cursor.deleteWordBackward", "光标：删除前一个单词"),
        BuiltinSymbolActionOption("cursor.deleteWordForward", "光标：删除后一个单词"),
        BuiltinSymbolActionOption("cursor.selectWord", "光标：选择当前单词"),
        BuiltinSymbolActionOption("cursor.selectLine", "光标：选择当前行"),
        BuiltinSymbolActionOption("cursor.lineStart", "光标：移动到行首"),
        BuiltinSymbolActionOption("cursor.lineEnd", "光标：移动到行尾"),
        BuiltinSymbolActionOption("cursor.selectToLineStart", "光标：选择到行首"),
        BuiltinSymbolActionOption("cursor.selectToLineEnd", "光标：选择到行尾"),
        BuiltinSymbolActionOption("cursor.selectToDocumentStart", "光标：选择到文档开头"),
        BuiltinSymbolActionOption("cursor.selectToDocumentEnd", "光标：选择到文档末尾"),
        BuiltinSymbolActionOption("cursor.documentStart", "光标：移动到文档开头"),
        BuiltinSymbolActionOption("cursor.documentEnd", "光标：移动到文档末尾"),
        BuiltinSymbolActionOption("document.selectAll", "文档：全选"),
        BuiltinSymbolActionOption("document.trimTrailingWhitespace", "文档：清理行尾空白"),
        BuiltinSymbolActionOption("document.trimLeadingWhitespace", "文档：清理行首空白"),
        BuiltinSymbolActionOption("document.ensureFinalNewline", "文档：确保末尾换行", "换行符，默认 \\n"),
        BuiltinSymbolActionOption("document.removeBlankLines", "文档：删除空白行"),
        BuiltinSymbolActionOption("document.toggleComment", "文档：注释/取消注释", "注释符号，默认 //"),
        BuiltinSymbolActionOption("undo", "编辑器：撤销"),
        BuiltinSymbolActionOption("redo", "编辑器：重做"),
        BuiltinSymbolActionOption("indent", "编辑器：缩进选区"),
        BuiltinSymbolActionOption("unindent", "编辑器：反缩进选区"),
        BuiltinSymbolActionOption("format", "编辑器：格式化代码"),
        BuiltinSymbolActionOption("insert_payload", "编辑器：插入参数文本", "要插入的文本")
    )

    private val byId = options.associateBy(BuiltinSymbolActionOption::commandId)
    fun find(commandId: String): BuiltinSymbolActionOption? = byId[commandId]
}
