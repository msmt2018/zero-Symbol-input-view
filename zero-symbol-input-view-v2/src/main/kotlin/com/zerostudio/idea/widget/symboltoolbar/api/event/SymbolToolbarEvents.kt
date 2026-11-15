/*
 * Public event contracts for symbol toolbar extensions.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.event

import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarConfig
import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarState
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarAction
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarGroup
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarItem

/** Common metadata shared by every public toolbar event. */
interface SymbolToolbarEvent {
    val controlId: String
    val timestampMillis: Long
    val sequence: Long
}

/** Group data and selection events. */
sealed interface SymbolToolbarGroupEvent : SymbolToolbarEvent {
    data class GroupsChanged(
        override val controlId: String,
        val groups: List<SymbolToolbarGroup>,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarGroupEvent

    data class GroupSelected(
        override val controlId: String,
        val group: SymbolToolbarGroup,
        val previousGroupId: String?,
        val animated: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarGroupEvent
}

/** Item interaction events are emitted before action execution. */
sealed interface SymbolToolbarItemEvent : SymbolToolbarEvent {
    val groupId: String
    val item: SymbolToolbarItem

    data class Pressed(
        override val controlId: String,
        override val groupId: String,
        override val item: SymbolToolbarItem,
        val gesture: SymbolToolbarInvocation.Gesture,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent

    data class Released(
        override val controlId: String,
        override val groupId: String,
        override val item: SymbolToolbarItem,
        val cancelled: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent

    data class Invoked(
        override val controlId: String,
        override val groupId: String,
        override val item: SymbolToolbarItem,
        val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent
}

/** Action pipeline events; result is available on completed/failed/rejected events. */
sealed interface SymbolToolbarActionEvent : SymbolToolbarEvent {
    val invocation: SymbolToolbarInvocation

    data class BeforeExecute(
        override val controlId: String,
        override val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarActionEvent

    data class Started(
        override val controlId: String,
        override val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarActionEvent

    data class Completed(
        override val controlId: String,
        override val invocation: SymbolToolbarInvocation,
        val result: SymbolToolbarActionResult,
        val durationMillis: Long,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarActionEvent

    data class Failed(
        override val controlId: String,
        override val invocation: SymbolToolbarInvocation,
        val error: Throwable,
        val durationMillis: Long,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarActionEvent

    data class Rejected(
        override val controlId: String,
        override val invocation: SymbolToolbarInvocation,
        val reason: String,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarActionEvent
}

/** Configuration and host state notifications. */
sealed interface SymbolToolbarStateEvent : SymbolToolbarEvent {
    data class ConfigChanged(
        override val controlId: String,
        val previous: SymbolToolbarConfig,
        val current: SymbolToolbarConfig,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarStateEvent

    data class StateChanged(
        override val controlId: String,
        val previous: SymbolToolbarState,
        val current: SymbolToolbarState,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarStateEvent

    data class EnabledChanged(
        override val controlId: String,
        val enabled: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarStateEvent
}

/** Control lifecycle notifications. */
sealed interface SymbolToolbarLifecycleEvent : SymbolToolbarEvent {
    data class Attached(
        override val controlId: String,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarLifecycleEvent

    data class Ready(
        override val controlId: String,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarLifecycleEvent

    data class Detached(
        override val controlId: String,
        val permanent: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarLifecycleEvent
}

/** Extension points can observe this event without depending on Android View classes. */
data class SymbolToolbarExtensionEvent(
    override val controlId: String,
    val extensionId: String,
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    override val timestampMillis: Long,
    override val sequence: Long
) : SymbolToolbarEvent
