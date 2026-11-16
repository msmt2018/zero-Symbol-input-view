/*
 * This file is part of zero-Symbol-input-view-V2 Compose.
 * Copyright (C) 2026 android_zero
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlin.math.abs

/**
 * A stable, normalized resting position for the Compose symbol drawer.
 *
 * [fraction] is the visible expansion progress: 0 is fully collapsed and 1 is
 * fully expanded. IDs, rather than list positions, are persisted across process
 * recreation and configuration changes.
 */
@Immutable
data class SymbolDrawerDetent(
    val id: String,
    val fraction: Float
) {
    init {
        require(id.isNotBlank()) { "Detent id must not be blank" }
        require(fraction.isFinite() && fraction in 0f..1f) {
            "Detent fraction must be finite and within 0..1"
        }
    }

    companion object {
        val Collapsed = SymbolDrawerDetent("collapsed", 0f)
        val HalfExpanded = SymbolDrawerDetent("half-expanded", 0.5f)
        val Expanded = SymbolDrawerDetent("expanded", 1f)

        val Defaults = listOf(Collapsed, HalfExpanded, Expanded)
    }
}

/**
 * Compose-native multi-anchor state engine. It deliberately knows nothing about
 * pixel dimensions, Views, Activities, or editor instances; the layout converts
 * pointer deltas and velocities into normalized expansion units.
 */
@Stable
class SymbolDrawerDetentState(
    detents: List<SymbolDrawerDetent> = SymbolDrawerDetent.Defaults,
    initialDetentId: String = SymbolDrawerDetent.Collapsed.id,
    selectedGroupId: String? = null
) {
    private var configuredDetents by mutableStateOf(validateDetents(detents))
    internal val expansion = Animatable(
        configuredDetents.first { it.id == initialDetentId }.fraction
    )

    var targetDetentId by mutableStateOf(initialDetentId)
        private set
    var currentDetentId by mutableStateOf(initialDetentId)
        private set
    var selectedGroupId by mutableStateOf(selectedGroupId)
        private set

    val availableDetents: List<SymbolDrawerDetent> get() = configuredDetents
    val expansionFraction: Float get() = expansion.value
    val isExpanded: Boolean get() = expansion.value > 0f
    val targetDetent: SymbolDrawerDetent
        get() = configuredDetents.first { it.id == targetDetentId }

    fun selectGroup(groupId: String) {
        require(groupId.isNotBlank()) { "Group id must not be blank" }
        selectedGroupId = groupId
    }

    /**
     * Replaces the anchor set after viewport/content changes. If the previous
     * target disappears, its normalized position is retained by selecting the
     * nearest new anchor; ties prefer the lower (less expanded) anchor.
     */
    suspend fun updateDetents(detents: List<SymbolDrawerDetent>) {
        val validated = validateDetents(detents)
        val previousTargetId = targetDetentId
        val previousCurrentId = currentDetentId
        val oldTargetFraction = configuredDetents.firstOrNull { it.id == previousTargetId }
            ?.fraction ?: expansion.value
        val oldCurrentFraction = configuredDetents.firstOrNull { it.id == previousCurrentId }
            ?.fraction ?: oldTargetFraction
        val wasSettled = previousTargetId == previousCurrentId &&
            abs(expansion.value - oldCurrentFraction) <= POSITION_EPSILON

        configuredDetents = validated

        val retainedTarget = validated.firstOrNull { it.id == previousTargetId }
            ?: nearestDetent(validated, oldTargetFraction)
        val retainedCurrent = validated.firstOrNull { it.id == previousCurrentId }
            ?: nearestDetent(validated, oldCurrentFraction)

        targetDetentId = retainedTarget.id
        currentDetentId = retainedCurrent.id
        // When idle, keep the same semantic anchor at its newly measured
        // fraction. While dragging/settling, preserve the live visual position.
        if (wasSettled) expansion.snapTo(retainedCurrent.fraction)
    }

    suspend fun snapToDetent(detentId: String) {
        val detent = requireDetent(detentId)
        targetDetentId = detent.id
        currentDetentId = detent.id
        expansion.snapTo(detent.fraction)
    }

    suspend fun animateToDetent(
        detentId: String,
        animationDurationMillis: Int = 220
    ) {
        val detent = requireDetent(detentId)
        targetDetentId = detent.id
        expansion.animateTo(
            detent.fraction,
            tween(animationDurationMillis.coerceAtLeast(0))
        )
        currentDetentId = detent.id
    }

    /** Positive delta expands; negative delta collapses. */
    internal suspend fun dragBy(deltaFraction: Float) {
        require(deltaFraction.isFinite()) { "deltaFraction must be finite" }
        expansion.snapTo((expansion.value + deltaFraction).coerceIn(0f, 1f))
    }

    /**
     * Settles to an adjacent anchor on a decisive fling, otherwise to the
     * nearest anchor. Positive velocity expands; negative velocity collapses.
     */
    internal suspend fun settle(
        velocityFraction: Float = 0f,
        velocityThreshold: Float = 0.8f,
        animationDurationMillis: Int = 220
    ) {
        require(velocityFraction.isFinite()) { "velocityFraction must be finite" }
        require(velocityThreshold.isFinite() && velocityThreshold >= 0f) {
            "velocityThreshold must be finite and non-negative"
        }
        val anchors = configuredDetents
        val position = expansion.value
        val target = when {
            velocityFraction > velocityThreshold ->
                anchors.firstOrNull { it.fraction > position + 0.0001f } ?: anchors.last()
            velocityFraction < -velocityThreshold ->
                anchors.lastOrNull { it.fraction < position - 0.0001f } ?: anchors.first()
            else -> nearestDetent(anchors, position)
        }
        animateToDetent(target.id, animationDurationMillis)
    }

    private fun requireDetent(id: String): SymbolDrawerDetent =
        configuredDetents.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("Unknown drawer detent: $id")

    companion object {
        private const val POSITION_EPSILON = 0.0001f

        private fun validateDetents(detents: List<SymbolDrawerDetent>): List<SymbolDrawerDetent> {
            require(detents.size >= 2) { "At least two drawer detents are required" }
            require(detents.map { it.id }.distinct().size == detents.size) {
                "Drawer detent IDs must be unique"
            }
            val sorted = detents.sortedBy { it.fraction }
            require(sorted.first().fraction == 0f && sorted.last().fraction == 1f) {
                "Drawer detents must include collapsed (0) and expanded (1) anchors"
            }
            require(sorted.zipWithNext().all { (a, b) -> b.fraction > a.fraction }) {
                "Drawer detent fractions must be strictly increasing"
            }
            return sorted
        }

        private fun nearestDetent(
            detents: List<SymbolDrawerDetent>,
            fraction: Float
        ): SymbolDrawerDetent = detents.minBy { abs(it.fraction - fraction) }

        /** Saver retains stable IDs; callers supply the current configurable anchors. */
        fun saver(detents: List<SymbolDrawerDetent>): Saver<SymbolDrawerDetentState, Any> {
            val validated = validateDetents(detents)
            return listSaver(
                save = {
                    listOf(
                        it.targetDetentId,
                        it.currentDetentId,
                        it.selectedGroupId.orEmpty()
                    )
                },
                restore = { saved ->
                    val targetId = saved.getOrNull(0) as? String
                    val currentId = saved.getOrNull(1) as? String
                    val selectedId = (saved.getOrNull(2) as? String)?.takeIf(String::isNotEmpty)
                    val restoredTarget = validated.firstOrNull { it.id == targetId }
                        ?: nearestDetent(validated, 0f)
                    val restoredCurrent = validated.firstOrNull { it.id == currentId }
                        ?: restoredTarget
                    SymbolDrawerDetentState(validated, restoredTarget.id, selectedId).also {
                        it.currentDetentId = restoredCurrent.id
                    }
                }
            )
        }
    }
}
