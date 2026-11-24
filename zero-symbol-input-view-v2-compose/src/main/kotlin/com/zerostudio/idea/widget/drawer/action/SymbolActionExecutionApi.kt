/*
 * This file is part of zero-Symbol-input-view-V2.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.drawer.action

import com.zerostudio.idea.widget.drawer.action.core.ActionExecutionEngine
import com.zerostudio.idea.widget.drawer.action.core.ActionExecutionPlan
import com.zerostudio.idea.widget.drawer.action.core.ActionExecutionResult
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.ClipboardOperation
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.CursorTarget
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DeleteKind
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SelectionTransform
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction

/**
 * Domain adapter implemented by the application/editor integration. Methods are
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

/** Cooperative cancellation checked between action steps; host calls remain synchronous. */
fun interface SymbolActionCancellation {
    fun isCancelled(): Boolean

    companion object {
        val NEVER = SymbolActionCancellation { false }
    }
}

sealed interface SymbolActionResult {
    data object Executed : SymbolActionResult
    data class Cancelled(val completedActions: Int) : SymbolActionResult
    data class Unsupported(val action: SymbolAction) : SymbolActionResult
    data class Invalid(val reason: String) : SymbolActionResult
    data class Failed(val cause: Throwable) : SymbolActionResult
    data class PartialFailure(val completedActions: Int, val cause: SymbolActionResult) : SymbolActionResult
}

/**
 * Public symbol-domain facade. Tree traversal, resource limits, cancellation
 * and partial-failure accounting live in the lower-level generic engine.
 */
class SymbolActionExecutor(
    private val maxCompositeActions: Int = 64,
    private val maxNestingDepth: Int = 8,
    private val maxTextLength: Int = 1_048_576,
    private val maxTotalActions: Int = 256
) {
    init {
        require(maxCompositeActions in 1..256)
        require(maxNestingDepth in 1..16)
        require(maxTextLength in 1..4_194_304)
        require(maxTotalActions in 1..4_096)
    }

    private val engine = ActionExecutionEngine<SymbolAction>(
        maxCompositeActions = maxCompositeActions,
        maxNestingDepth = maxNestingDepth,
        maxTotalActions = maxTotalActions
    )

    fun execute(
        action: SymbolAction,
        host: SymbolActionHost,
        cancellation: SymbolActionCancellation = SymbolActionCancellation.NEVER
    ): SymbolActionResult {
        val plan = action.toExecutionPlan()
        val result = engine.execute(
            plan = plan,
            executeLeaf = { leaf -> dispatch(leaf, host) },
            validateLeaf = ::validateLeaf,
            isCancelled = cancellation::isCancelled
        )
        return result.toSymbolActionResult()
    }

    private fun SymbolAction.toExecutionPlan(): ActionExecutionPlan<SymbolAction> =
        when (this) {
            is SymbolAction.Composite -> ActionExecutionPlan.Composite(actions.map { it.toExecutionPlan() })
            else -> ActionExecutionPlan.Leaf(this)
        }

    private fun validateLeaf(action: SymbolAction): String? = when (action) {
        is SymbolAction.InsertText ->
            if (action.template.length > maxTextLength) "Insert template is too large" else null
        is SymbolAction.ReplaceSelection ->
            if (action.text.length > maxTextLength) "Replacement text is too large" else null
        is SymbolAction.SurroundSelection ->
            if (action.prefix.length.toLong() + action.suffix.length > maxTextLength) {
                "Surround template is too large"
            } else null
        is SymbolAction.EditorCommand -> when {
            !COMMAND_ID.matches(action.commandId) -> "Invalid editor command ID"
            (action.payload?.length ?: 0) > maxTextLength -> "Editor command payload is too large"
            else -> null
        }
        is SymbolAction.Extension -> when {
            !EXTENSION_ID.matches(action.extensionId) -> "Invalid extension ID"
            (action.payload?.length ?: 0) > maxTextLength -> "Extension payload is too large"
            else -> null
        }
        else -> null
    }

    private fun dispatch(action: SymbolAction, host: SymbolActionHost): Boolean = when (action) {
        is SymbolAction.InsertText -> host.insertText(action.template)
        is SymbolAction.ReplaceSelection -> host.replaceSelection(action.text)
        is SymbolAction.SurroundSelection -> host.surroundSelection(action.prefix, action.suffix)
        is SymbolAction.MoveCursor -> host.moveCursor(action.target)
        is SymbolAction.Delete -> host.delete(action.kind)
        is SymbolAction.TransformSelection -> host.transformSelection(action.transform)
        is SymbolAction.EditorCommand -> host.executeEditorCommand(action.commandId, action.payload)
        is SymbolAction.Clipboard -> host.performClipboard(action.operation)
        SymbolAction.OpenManager -> host.openManager()
        is SymbolAction.Extension -> host.executeExtension(action.extensionId, action.payload)
        is SymbolAction.Composite -> error("Composite actions must be expanded into execution plans")
    }

    private fun ActionExecutionResult<SymbolAction>.toSymbolActionResult(): SymbolActionResult =
        when (this) {
            ActionExecutionResult.Executed -> SymbolActionResult.Executed
            is ActionExecutionResult.Cancelled -> SymbolActionResult.Cancelled(completedActions)
            is ActionExecutionResult.Unsupported -> SymbolActionResult.Unsupported(action)
            is ActionExecutionResult.Invalid -> SymbolActionResult.Invalid(reason)
            is ActionExecutionResult.Failed -> SymbolActionResult.Failed(cause)
            is ActionExecutionResult.PartialFailure -> SymbolActionResult.PartialFailure(
                completedActions = completedActions,
                cause = cause.toSymbolActionResult()
            )
        }

    private companion object {
        val COMMAND_ID = Regex("[a-zA-Z][a-zA-Z0-9_.-]{0,127}")
        val EXTENSION_ID = Regex("[a-zA-Z0-9][a-zA-Z0-9_.:/-]{0,255}")
    }
}
