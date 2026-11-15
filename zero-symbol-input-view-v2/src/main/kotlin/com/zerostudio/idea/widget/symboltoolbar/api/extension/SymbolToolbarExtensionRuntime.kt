/*
 * Runtime dispatcher for symbol toolbar extensions.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarConfig
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionInvocation
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds extension contributions to one toolbar control instance.
 *
 * The runtime has no reference to an editor, drawer, Activity or concrete View.
 * The embedding app supplies the action host and owns the runtime lifetime.
 */
class SymbolToolbarExtensionRuntime(
    private val controlId: String,
    private val registry: SymbolToolbarExtensionRegistry,
    private val actionHost: SymbolToolbarActionHost
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    init {
        require(controlId.isNotBlank()) { "controlId must not be blank" }
        registry.lifecycleExtensions().forEach { extension ->
            runCatching { extension.onAttached(controlId) }
        }
    }

    fun onConfigChanged(config: SymbolToolbarConfig) {
        checkActive()
        registry.lifecycleExtensions().forEach { extension ->
            runCatching { extension.onConfigChanged(config) }
        }
    }

    /**
     * Runs interceptors in priority order, then dispatches to the handler keyed
     * by action.type. Unsupported types are returned to the caller so built-in
     * or host-native action handling can remain the fallback.
     */
    suspend fun invoke(invocation: SymbolToolbarInvocation): SymbolToolbarActionResult {
        checkActive()
        val interceptors = registry.invocationInterceptors()
        for (interceptor in interceptors) {
            val decision = runCatching { interceptor.beforeInvoke(invocation) }
                .getOrElse { return SymbolToolbarActionResult.Failed(it) }
            when (decision) {
                SymbolToolbarInvocationDecision.Continue -> Unit
                SymbolToolbarInvocationDecision.Consume -> return notifyAfter(
                    interceptors, invocation, SymbolToolbarActionResult.Success
                )
                is SymbolToolbarInvocationDecision.Reject -> return notifyAfter(
                    interceptors,
                    invocation,
                    SymbolToolbarActionResult.Rejected(decision.reason)
                )
            }
        }

        val handler = registry.findAction(invocation.action.type)
            ?: return notifyAfter(
                interceptors,
                invocation,
                SymbolToolbarActionResult.Unsupported(invocation.action.type)
            )

        val result = runCatching {
            handler.execute(
                SymbolToolbarActionInvocation(
                    groupId = invocation.groupId,
                    itemId = invocation.itemId,
                    action = invocation.action,
                    isLongPress = invocation.gesture ==
                        com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation.Gesture.LONG_PRESS
                ),
                actionHost
            )
        }.getOrElse { SymbolToolbarActionResult.Failed(it) }

        return notifyAfter(interceptors, invocation, result)
    }

    private fun notifyAfter(
        interceptors: List<SymbolToolbarInvocationInterceptor>,
        invocation: SymbolToolbarInvocation,
        result: SymbolToolbarActionResult
    ): SymbolToolbarActionResult {
        interceptors.asReversed().forEach { interceptor ->
            runCatching { interceptor.afterInvoke(invocation, result) }
        }
        return result
    }

    private fun checkActive() {
        check(!closed.get()) { "SymbolToolbarExtensionRuntime is closed" }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        registry.lifecycleExtensions().asReversed().forEach { extension ->
            runCatching { extension.onDetached(controlId) }
        }
    }
}
