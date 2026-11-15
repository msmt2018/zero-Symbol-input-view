/*
 * Public events expose the canonical legacy symbol records and settings.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.event

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.symboltoolbar.api.SymbolToolbarState
import com.zerostudio.idea.widget.symboltoolbar.api.action.SymbolToolbarActionResult
import com.zerostudio.idea.widget.symboltoolbar.api.model.SymbolToolbarInvocation

interface SymbolToolbarEvent {
    val controlId: String
    val timestampMillis: Long
    val sequence: Long
}

sealed interface SymbolToolbarGroupEvent : SymbolToolbarEvent {
    data class GroupsChanged(
        override val controlId: String,
        val groups: List<SymbolGroup>,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarGroupEvent

    data class GroupSelected(
        override val controlId: String,
        val group: SymbolGroup,
        val previousGroupIndex: Int,
        val selectedGroupIndex: Int,
        val animated: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarGroupEvent
}

sealed interface SymbolToolbarItemEvent : SymbolToolbarEvent {
    val group: SymbolGroup
    val item: SymbolItem

    data class Pressed(
        override val controlId: String,
        override val group: SymbolGroup,
        override val item: SymbolItem,
        val gesture: SymbolToolbarInvocation.Gesture,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent

    data class Released(
        override val controlId: String,
        override val group: SymbolGroup,
        override val item: SymbolItem,
        val cancelled: Boolean,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent

    data class Invoked(
        override val controlId: String,
        override val group: SymbolGroup,
        override val item: SymbolItem,
        val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long,
        override val sequence: Long
    ) : SymbolToolbarItemEvent
}

sealed interface SymbolToolbarActionEvent : SymbolToolbarEvent {
    val invocation: SymbolToolbarInvocation

    data class BeforeExecute(
        override val controlId: String, override val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarActionEvent
    data class Started(
        override val controlId: String, override val invocation: SymbolToolbarInvocation,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarActionEvent
    data class Completed(
        override val controlId: String, override val invocation: SymbolToolbarInvocation,
        val result: SymbolToolbarActionResult, val durationMillis: Long,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarActionEvent
    data class Failed(
        override val controlId: String, override val invocation: SymbolToolbarInvocation,
        val error: Throwable, val durationMillis: Long,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarActionEvent
    data class Rejected(
        override val controlId: String, override val invocation: SymbolToolbarInvocation,
        val reason: String, override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarActionEvent
}

sealed interface SymbolToolbarStateEvent : SymbolToolbarEvent {
    data class SettingsChanged(
        override val controlId: String,
        val previous: SymbolDrawerSettings,
        val current: SymbolDrawerSettings,
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
        override val controlId: String, val enabled: Boolean,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarStateEvent
}

sealed interface SymbolToolbarLifecycleEvent : SymbolToolbarEvent {
    data class Attached(
        override val controlId: String, override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarLifecycleEvent
    data class Ready(
        override val controlId: String, override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarLifecycleEvent
    data class Detached(
        override val controlId: String, val permanent: Boolean,
        override val timestampMillis: Long, override val sequence: Long
    ) : SymbolToolbarLifecycleEvent
}

data class SymbolToolbarExtensionEvent(
    override val controlId: String,
    val extensionId: String,
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    override val timestampMillis: Long,
    override val sequence: Long
) : SymbolToolbarEvent
