package com.zerostudio.idea.widget.editor.symbolinput.v2.action

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction

/**
 * Host adapter implemented by the application/editor integration. Methods are
 * invoked synchronously on the caller thread; UI/editor hosts should call execute
 * from their required thread and report unavailable capabilities with false.
 */
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

sealed interface SymbolActionResult {
    data object Executed : SymbolActionResult
    data class Unsupported(val action: SymbolAction) : SymbolActionResult
    data class Invalid(val reason: String) : SymbolActionResult
    data class Failed(val cause: Throwable) : SymbolActionResult
    data class PartialFailure(val completedActions: Int, val cause: SymbolActionResult) : SymbolActionResult
}

class SymbolActionExecutor(
    private val maxCompositeActions: Int = 64,
    private val maxNestingDepth: Int = 8,
    private val maxTextLength: Int = 1_048_576
) {
    init {
        require(maxCompositeActions in 1..256)
        require(maxNestingDepth in 1..16)
        require(maxTextLength in 1..4_194_304)
    }

    fun execute(action: SymbolAction, host: SymbolActionHost): SymbolActionResult =
        executeInternal(action, host, 0)

    private fun executeInternal(
        action: SymbolAction,
        host: SymbolActionHost,
        depth: Int
    ): SymbolActionResult {
        if (depth > maxNestingDepth) return SymbolActionResult.Invalid("Composite action nesting limit exceeded")
        return try {
            when (action) {
                is SymbolAction.InsertText -> {
                    if (action.template.length > maxTextLength) invalid("Insert template is too large")
                    else supported(host.insertText(action.template), action)
                }
                is SymbolAction.ReplaceSelection -> {
                    if (action.text.length > maxTextLength) invalid("Replacement text is too large")
                    else supported(host.replaceSelection(action.text), action)
                }
                is SymbolAction.SurroundSelection -> {
                    if (action.prefix.length + action.suffix.length > maxTextLength) invalid("Surround template is too large")
                    else supported(host.surroundSelection(action.prefix, action.suffix), action)
                }
                is SymbolAction.MoveCursor -> supported(host.moveCursor(action.target), action)
                is SymbolAction.Delete -> supported(host.delete(action.kind), action)
                is SymbolAction.TransformSelection -> supported(host.transformSelection(action.transform), action)
                is SymbolAction.EditorCommand -> {
                    if (!COMMAND_ID.matches(action.commandId)) invalid("Invalid editor command ID")
                    else supported(host.executeEditorCommand(action.commandId, action.payload), action)
                }
                is SymbolAction.Clipboard -> supported(host.performClipboard(action.operation), action)
                SymbolAction.OpenManager -> supported(host.openManager(), action)
                is SymbolAction.Extension -> {
                    if (!EXTENSION_ID.matches(action.extensionId)) invalid("Invalid extension ID")
                    else supported(host.executeExtension(action.extensionId, action.payload), action)
                }
                is SymbolAction.Composite -> executeComposite(action, host, depth)
            }
        } catch (error: Throwable) {
            SymbolActionResult.Failed(error)
        }
    }

    private fun executeComposite(
        composite: SymbolAction.Composite,
        host: SymbolActionHost,
        depth: Int
    ): SymbolActionResult {
        if (composite.actions.isEmpty()) return SymbolActionResult.Invalid("Composite action cannot be empty")
        if (composite.actions.size > maxCompositeActions) {
            return SymbolActionResult.Invalid("Composite action contains too many steps")
        }
        var completed = 0
        composite.actions.forEach { child ->
            val result = executeInternal(child, host, depth + 1)
            if (result != SymbolActionResult.Executed) {
                return if (completed == 0) result else SymbolActionResult.PartialFailure(completed, result)
            }
            completed++
        }
        return SymbolActionResult.Executed
    }

    private fun supported(success: Boolean, action: SymbolAction) =
        if (success) SymbolActionResult.Executed else SymbolActionResult.Unsupported(action)

    private fun invalid(reason: String) = SymbolActionResult.Invalid(reason)

    private companion object {
        val COMMAND_ID = Regex("[a-zA-Z][a-zA-Z0-9_.-]{0,127}")
        val EXTENSION_ID = Regex("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}")
    }
}
