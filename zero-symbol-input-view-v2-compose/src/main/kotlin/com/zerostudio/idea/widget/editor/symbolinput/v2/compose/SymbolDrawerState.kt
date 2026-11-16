/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 * Author: android_zero · https://github.com/msmt2018
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/** State holder for the Compose drawer. It is independent of Activity and editor instances. */
@Stable
class SymbolDrawerState internal constructor(
    initiallyExpanded: Boolean,
    selectedGroupId: String?
) {
    internal val expansion = Animatable(if (initiallyExpanded) 1f else 0f)
    var targetExpanded: Boolean by mutableStateOf(initiallyExpanded)
        private set
    var selectedGroupId: String? by mutableStateOf(selectedGroupId)
        private set

    val expansionFraction: Float get() = expansion.value
    val isExpanded: Boolean get() = targetExpanded

    fun selectGroup(groupId: String) {
        selectedGroupId = groupId
    }

    suspend fun expand(animationDurationMillis: Int = 220) {
        targetExpanded = true
        expansion.animateTo(1f, tween(animationDurationMillis.coerceAtLeast(0)))
    }

    suspend fun collapse(animationDurationMillis: Int = 220) {
        targetExpanded = false
        expansion.animateTo(0f, tween(animationDurationMillis.coerceAtLeast(0)))
    }

    internal suspend fun dragBy(deltaFraction: Float) {
        expansion.snapTo((expansion.value + deltaFraction).coerceIn(0f, 1f))
        targetExpanded = expansion.value >= 0.5f
    }

    internal suspend fun settle(
        velocityFraction: Float = 0f,
        animationDurationMillis: Int = 220,
        threshold: Float = 0.5f,
        velocityThreshold: Float = 0.8f
    ) {
        require(velocityFraction.isFinite()) { "velocityFraction must be finite" }
        require(threshold in 0f..1f)
        require(velocityThreshold >= 0f && velocityThreshold.isFinite())
        val expand = when {
            velocityFraction < -velocityThreshold -> true
            velocityFraction > velocityThreshold -> false
            else -> expansion.value >= threshold
        }
        if (expand) expand(animationDurationMillis) else collapse(animationDurationMillis)
    }

    companion object {
        val Saver: Saver<SymbolDrawerState, Any> = listSaver<SymbolDrawerState, Any>(
            save = { listOf(it.targetExpanded, it.selectedGroupId.orEmpty()) },
            restore = {
                SymbolDrawerState(
                    initiallyExpanded = it.getOrNull(0) as? Boolean ?: false,
                    selectedGroupId = (it.getOrNull(1) as? String)?.takeIf(String::isNotEmpty)
                )
            }
        )
    }
}
