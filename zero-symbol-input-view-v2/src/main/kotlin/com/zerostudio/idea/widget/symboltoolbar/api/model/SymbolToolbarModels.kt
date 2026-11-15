/*
 * Symbol toolbar invocation metadata. Symbol data itself is the original V1 model.
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */
package com.zerostudio.idea.widget.symboltoolbar.api.model

import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy.SymbolItem

/**
 * Gesture metadata around the original mutable V1 symbol records.
 * No second group/item model is introduced: callers receive the exact objects
 * submitted to the toolbar, including the numeric action ID and text payload.
 */
data class SymbolToolbarInvocation(
    val group: SymbolGroup,
    val item: SymbolItem,
    val gesture: Gesture,
    val eventTimeMillis: Long = System.currentTimeMillis()
) {
    enum class Gesture { TAP, LONG_PRESS, ACCESSIBILITY }

    val isLongPress: Boolean get() = gesture == Gesture.LONG_PRESS
    val actionId: Int get() = if (isLongPress) item.longAction ?: item.shortAction else item.shortAction
    val payload: String? get() = if (isLongPress) item.longText else item.shortText
}
