package android.zero.studio.widget.editor.symbolinput

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement

object SymbolActionExecutor {

    fun execute(editor: CodeEditor, actionId: Int, text: String?, onOpenManager: (() -> Unit)?) {
        if (!editor.isEditable && actionId in listOf(0, 3, 4, 7, 8, 9, 11, 29, 30, 32)) return

        when (actionId) {
            0 -> insertTextWithMacro(editor, text ?: "")
            3 -> deleteLine(editor)
            4 -> clearLine(editor)
            7 -> toUpperCase(editor)
            8 -> toLowerCase(editor)
            9 -> editor.indentOrCommitTab()
            11 -> toggleComment(editor)
            16 -> editor.moveSelection(SelectionMovement.LINE_START)
            17 -> editor.moveSelection(SelectionMovement.LINE_END)
            18 -> editor.moveSelection(SelectionMovement.LEFT)
            19 -> editor.moveSelection(SelectionMovement.RIGHT)
            20 -> editor.moveSelection(SelectionMovement.UP)
            21 -> editor.moveSelection(SelectionMovement.DOWN)
            22 -> onOpenManager?.invoke()
            23 -> editor.moveSelection(SelectionMovement.TEXT_START)
            24 -> editor.moveSelection(SelectionMovement.TEXT_END)
            25 -> editor.selectAll()
            28 -> editor.copyText()
            29 -> editor.cutText()
            30 -> editor.pasteText()
            32 -> editor.formatCodeAsync()
        }
    }

    private fun insertTextWithMacro(editor: CodeEditor, text: String) {
        val cursorToken = "\$T"
        if (text.contains(cursorToken)) {
            val parts = text.split(cursorToken, limit = 2)
            val insertStr = parts[0] + parts[1]
            editor.insertText(insertStr, parts[0].length)
        } else {
            editor.insertText(text, text.length)
        }
    }

    private fun deleteLine(editor: CodeEditor) {
        val cursor = editor.cursor
        val line = cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
        if (line < editor.text.lineCount - 1) {
            editor.text.delete(line, editor.text.getColumnCount(line), line + 1, 0)
        } else if (line > 0) {
            editor.text.delete(line - 1, editor.text.getColumnCount(line - 1), line, 0)
        }
    }

    private fun clearLine(editor: CodeEditor) {
        val line = editor.cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
    }

    private fun toggleComment(editor: CodeEditor) {
        val textObj = editor.text
        val startLine = editor.cursor.leftLine
        val endLine = editor.cursor.rightLine
        
        val commentStr = "//"

        textObj.beginBatchEdit()
        for (i in startLine..endLine) {
            val lineStr = textObj.getLineString(i)
            val trimmed = lineStr.trimStart()
            
            if (trimmed.startsWith(commentStr)) {
                val startIdx = lineStr.indexOf(commentStr)
                textObj.delete(i, startIdx, i, startIdx + commentStr.length)
            } else {
                val startIdx = lineStr.length - trimmed.length
                textObj.insert(i, startIdx, commentStr)
            }
        }
        textObj.endBatchEdit()
    }
    
    private fun toUpperCase(editor: CodeEditor) {
        if (editor.cursor.isSelected) {
            val leftIdx = editor.cursor.left().index
            val rightIdx = editor.cursor.right().index
            val text = editor.text.subSequence(leftIdx, rightIdx).toString()
            editor.commitText(text.uppercase())
        }
    }

    private fun toLowerCase(editor: CodeEditor) {
        if (editor.cursor.isSelected) {
            val leftIdx = editor.cursor.left().index
            val rightIdx = editor.cursor.right().index
            val text = editor.text.subSequence(leftIdx, rightIdx).toString()
            editor.commitText(text.lowercase())
        }
    }
    
}