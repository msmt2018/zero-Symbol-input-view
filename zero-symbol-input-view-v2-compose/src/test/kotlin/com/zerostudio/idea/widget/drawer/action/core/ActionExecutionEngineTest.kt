/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.drawer.action.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionExecutionEngineTest {
    @Test
    fun validatesTheEntireTreeBeforeInvokingAnyHostCallback() {
        val calls = mutableListOf<String>()
        val plan = ActionExecutionPlan.Composite(
            listOf(
                ActionExecutionPlan.Leaf("valid"),
                ActionExecutionPlan.Leaf("invalid")
            )
        )

        val result = ActionExecutionEngine<String>().execute(
            plan = plan,
            executeLeaf = { calls += it; true },
            validateLeaf = { if (it == "invalid") "rejected" else null }
        )

        assertEquals(ActionExecutionResult.Invalid("rejected"), result)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun reportsCompletedLeafCountAcrossNestedCompositeFailure() {
        val plan = ActionExecutionPlan.Composite(
            listOf(
                ActionExecutionPlan.Leaf("first"),
                ActionExecutionPlan.Composite(
                    listOf(ActionExecutionPlan.Leaf("second"), ActionExecutionPlan.Leaf("decline"))
                )
            )
        )
        val calls = mutableListOf<String>()

        val result = ActionExecutionEngine<String>().execute(
            plan = plan,
            executeLeaf = {
                calls += it
                it != "decline"
            }
        )

        assertEquals(
            ActionExecutionResult.PartialFailure(
                completedActions = 2,
                cause = ActionExecutionResult.Invalid(
                    "Unsupported action cannot be represented in a domain-neutral partial failure"
                )
            ),
            result
        )
        assertEquals(listOf("first", "second", "decline"), calls)
    }

    @Test
    fun cancellationIsCheckedBetweenLeaves() {
        val calls = mutableListOf<String>()
        var checks = 0
        val result = ActionExecutionEngine<String>().execute(
            plan = ActionExecutionPlan.Composite(
                listOf(ActionExecutionPlan.Leaf("first"), ActionExecutionPlan.Leaf("second"))
            ),
            executeLeaf = { calls += it; true },
            isCancelled = { ++checks >= 3 }
        )

        assertEquals(ActionExecutionResult.Cancelled(1), result)
        assertEquals(listOf("first"), calls)
    }

    @Test
    fun hostExceptionsBecomeFailedResults() {
        val error = IllegalStateException("host failure")
        val result = ActionExecutionEngine<String>().execute(
            plan = ActionExecutionPlan.Leaf("action"),
            executeLeaf = { throw error }
        )

        assertTrue(result is ActionExecutionResult.Failed)
        assertTrue((result as ActionExecutionResult.Failed).cause === error)
    }
}
