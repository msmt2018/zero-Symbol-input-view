/*
 * Runtime dispatcher for symbol toolbar extensions and public events.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.extension

import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarConfig
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionHost
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionInvocation
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarActionEvent
import com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarEventHub
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Per-control extension runtime. Exposes a public event hub and action dispatcher,
 * but deliberately holds no editor, drawer, Activity or concrete View reference.
 */
class SymbolToolbarExtensionRuntime(
    private val controlId: String,
    private val registry: SymbolToolbarExtensionRegistry,
    private val actionHost: SymbolToolbarActionHost
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    val events: SymbolToolbarEventHub = SymbolToolbarEventHub(controlId)

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

    suspend fun invoke(invocation: SymbolToolbarInvocation): SymbolToolbarActionResult {
        checkActive()
        val startedAt = System.currentTimeMillis()
        events.publish(SymbolToolbarActionEvent.BeforeExecute(
            controlId, invocation, events.timestamp(), events.nextSequence()
        ))

        val interceptors = registry.invocationInterceptors()
        for (interceptor in interceptors) {
            val decision = runCatching { interceptor.beforeInvoke(invocation) }
                .getOrElse {
                    val failed = SymbolToolbarActionResult.Failed(it)
                    publishFailure(invocation, it, startedAt)
                    return notifyAfter(interceptors, invocation, failed)
                }
            when (decision) {
                SymbolToolbarInvocationDecision.Continue -> Unit
                SymbolToolbarInvocationDecision.Consume -> {
                    val result = SymbolToolbarActionResult.Success
                    publishCompleted(invocation, result, startedAt)
                    return notifyAfter(interceptors, invocation, result)
                }
                is SymbolToolbarInvocationDecision.Reject -> {
                    val result = SymbolToolbarActionResult.Rejected(decision.reason)
                    events.publish(SymbolToolbarActionEvent.Rejected(
                        controlId, invocation, decision.reason, events.timestamp(), events.nextSequence()
                    ))
                    return notifyAfter(interceptors, invocation, result)
                }
            }
        }

        val handler = registry.findAction(invocation.action.type)
        if (handler == null) {
            val result = SymbolToolbarActionResult.Unsupported(invocation.action.type)
            publishCompleted(invocation, result, startedAt)
            return notifyAfter(interceptors, invocation, result)
        }

        events.publish(SymbolToolbarActionEvent.Started(
            controlId, invocation, events.timestamp(), events.nextSequence()
        ))
        val result = try {
            handler.execute(
                SymbolToolbarActionInvocation(
                    groupId = invocation.groupId,
                    itemId = invocation.itemId,
                    action = invocation.action,
                    isLongPress = invocation.gesture == SymbolToolbarInvocation.Gesture.LONG_PRESS,
                    gesture = invocation.gesture,
                    eventTimeMillis = invocation.eventTimeMillis
                ),
                actionHost
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val failed = SymbolToolbarActionResult.Failed(error)
            publishFailure(invocation, error, startedAt)
            return notifyAfter(interceptors, invocation, failed)
        }

        when (result) {
            is SymbolToolbarActionResult.Failed -> publishFailure(invocation, result.cause, startedAt)
            is SymbolToolbarActionResult.Rejected -> events.publish(SymbolToolbarActionEvent.Rejected(
                controlId, invocation, result.reason, events.timestamp(), events.nextSequence()
            ))
            else -> publishCompleted(invocation, result, startedAt)
        }
        return notifyAfter(interceptors, invocation, result)
    }

    private fun publishCompleted(
        invocation: SymbolToolbarInvocation,
        result: SymbolToolbarActionResult,
        startedAt: Long
    ) {
        events.publish(SymbolToolbarActionEvent.Completed(
            controlId, invocation, result,
            (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
            events.timestamp(), events.nextSequence()
        ))
    }

    private fun publishFailure(
        invocation: SymbolToolbarInvocation,
        error: Throwable,
        startedAt: Long
    ) {
        events.publish(SymbolToolbarActionEvent.Failed(
            controlId, invocation, error,
            (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
            events.timestamp(), events.nextSequence()
        ))
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
        events.publish(
            com.zerostudio.idea.widget.symboltoolbar.api.event.SymbolToolbarLifecycleEvent.Detached(
                controlId, true, events.timestamp(), events.nextSequence()
            )
        )
        events.close()
    }
}
