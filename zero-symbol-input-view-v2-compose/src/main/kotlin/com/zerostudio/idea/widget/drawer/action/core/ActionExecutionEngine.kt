/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.drawer.action.core

/**
 * A domain-neutral action tree. Leaf values belong to the caller's domain;
 * the execution engine only owns tree traversal, limits and result semantics.
 */
sealed interface ActionExecutionPlan<out A> {
    data class Leaf<A>(val action: A) : ActionExecutionPlan<A>
    data class Composite<A>(val actions: List<ActionExecutionPlan<A>>) : ActionExecutionPlan<A>
}

/** Result contract for the platform- and domain-neutral execution engine. */
sealed interface ActionExecutionResult<out A> {
    data object Executed : ActionExecutionResult<Nothing>
    data class Cancelled(val completedActions: Int) : ActionExecutionResult<Nothing>
    data class Unsupported<A>(val action: A) : ActionExecutionResult<A>
    data class Invalid(val reason: String) : ActionExecutionResult<Nothing>
    data class Failed(val cause: Throwable) : ActionExecutionResult<Nothing>
    data class PartialFailure<out A>(
        val completedActions: Int,
        val cause: ActionExecutionResult<A>
    ) : ActionExecutionResult<A>
}

/**
 * Executes a validated action tree without depending on Android, Compose,
 * editor models, or a particular host implementation.
 *
 * The complete tree is validated before the first host callback. Cancellation
 * is cooperative and checked between nodes; host callbacks remain synchronous.
 */
class ActionExecutionEngine<A>(
    private val maxCompositeActions: Int = 64,
    private val maxNestingDepth: Int = 8,
    private val maxTotalActions: Int = 256
) {
    init {
        require(maxCompositeActions in 1..256)
        require(maxNestingDepth in 1..16)
        require(maxTotalActions in 1..4_096)
    }

    fun execute(
        plan: ActionExecutionPlan<A>,
        executeLeaf: (A) -> Boolean,
        validateLeaf: (A) -> String? = { null },
        isCancelled: () -> Boolean = { false }
    ): ActionExecutionResult<A> {
        val validationError = validate(
            plan,
            depth = 0,
            budget = ActionBudget(maxTotalActions),
            validateLeaf = validateLeaf
        )
        if (validationError != null) return ActionExecutionResult.Invalid(validationError)

        return executeInternal(
            plan = plan,
            depth = 0,
            executeLeaf = executeLeaf,
            isCancelled = isCancelled,
            progress = ExecutionProgress(),
            budget = ActionBudget(maxTotalActions)
        )
    }

    private fun validate(
        plan: ActionExecutionPlan<A>,
        depth: Int,
        budget: ActionBudget,
        validateLeaf: (A) -> String?
    ): String? {
        if (depth > maxNestingDepth) return "Composite action nesting limit exceeded"
        if (!budget.consume()) return "Composite action total action limit exceeded"

        return when (plan) {
            is ActionExecutionPlan.Leaf -> validateLeaf(plan.action)
            is ActionExecutionPlan.Composite -> when {
                plan.actions.isEmpty() -> "Composite action cannot be empty"
                plan.actions.size > maxCompositeActions -> "Composite action contains too many steps"
                else -> plan.actions.firstNotNullOfOrNull {
                    validate(it, depth + 1, budget, validateLeaf)
                }
            }
        }
    }

    private fun executeInternal(
        plan: ActionExecutionPlan<A>,
        depth: Int,
        executeLeaf: (A) -> Boolean,
        isCancelled: () -> Boolean,
        progress: ExecutionProgress,
        budget: ActionBudget
    ): ActionExecutionResult<A> {
        if (depth > maxNestingDepth) {
            return ActionExecutionResult.Invalid("Composite action nesting limit exceeded")
        }

        val cancelled = try {
            isCancelled()
        } catch (error: Exception) {
            return ActionExecutionResult.Failed(error)
        }
        if (cancelled) return ActionExecutionResult.Cancelled(progress.completedActions)
        if (!budget.consume()) {
            return ActionExecutionResult.Invalid("Composite action total action limit exceeded")
        }

        return when (plan) {
            is ActionExecutionPlan.Leaf -> {
                val result = try {
                    if (executeLeaf(plan.action)) {
                        ActionExecutionResult.Executed
                    } else {
                        ActionExecutionResult.Unsupported(plan.action)
                    }
                } catch (error: Exception) {
                    ActionExecutionResult.Failed(error)
                }
                if (result == ActionExecutionResult.Executed) {
                    progress.completedActions++
                }
                result
            }
            is ActionExecutionPlan.Composite -> executeComposite(
                plan = plan,
                depth = depth,
                executeLeaf = executeLeaf,
                isCancelled = isCancelled,
                progress = progress,
                budget = budget
            )
        }
    }

    private fun executeComposite(
        plan: ActionExecutionPlan.Composite<A>,
        depth: Int,
        executeLeaf: (A) -> Boolean,
        isCancelled: () -> Boolean,
        progress: ExecutionProgress,
        budget: ActionBudget
    ): ActionExecutionResult<A> {
        if (plan.actions.isEmpty()) return ActionExecutionResult.Invalid("Composite action cannot be empty")
        if (plan.actions.size > maxCompositeActions) {
            return ActionExecutionResult.Invalid("Composite action contains too many steps")
        }

        plan.actions.forEach { child ->
            val result = executeInternal(
                plan = child,
                depth = depth + 1,
                executeLeaf = executeLeaf,
                isCancelled = isCancelled,
                progress = progress,
                budget = budget
            )
            if (result is ActionExecutionResult.Cancelled) return result
            if (result != ActionExecutionResult.Executed) {
                if (result is ActionExecutionResult.PartialFailure) return result
                return if (progress.completedActions == 0) {
                    result
                } else {
                    ActionExecutionResult.PartialFailure(
                        completedActions = progress.completedActions,
                        cause = result
                    )
                }
            }
        }
        return ActionExecutionResult.Executed
    }

    private class ExecutionProgress(var completedActions: Int = 0)

    private class ActionBudget(private var remaining: Int) {
        fun consume(): Boolean {
            if (remaining <= 0) return false
            remaining--
            return true
        }
    }
}
