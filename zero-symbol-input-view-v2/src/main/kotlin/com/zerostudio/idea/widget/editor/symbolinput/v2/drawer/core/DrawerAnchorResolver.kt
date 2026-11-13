/*
 * This file is part of ZeroStudio.
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
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core

/**
 * Normalizes strategy output into an ordered, bounded anchor list.
 * Duplicate pixel positions are collapsed deterministically, preferring the
 * more expanded semantic state while retaining a stable id.
 */
object DrawerAnchorResolver {
    fun resolve(
        strategy: DrawerAnchorStrategy,
        input: DrawerAnchorInput
    ): List<DrawerAnchor> {
        val resolved = strategy.resolve(input)
        require(resolved.isNotEmpty()) { "Anchor strategy returned no anchors" }

        val normalized = resolved.map {
            it.copy(heightPx = it.heightPx.coerceIn(input.minHeightPx, input.maxHeightPx))
        }.sortedWith(compareBy<DrawerAnchor> { it.heightPx }.thenBy { rank(it.state) })

        val unique = LinkedHashMap<Int, DrawerAnchor>()
        normalized.forEach { candidate ->
            val current = unique[candidate.heightPx]
            if (current == null || rank(candidate.state) >= rank(current.state)) {
                unique[candidate.heightPx] = candidate
            }
        }
        return unique.values.toList()
    }

    fun nearestAnchor(anchors: List<DrawerAnchor>, heightPx: Int): DrawerAnchor {
        require(anchors.isNotEmpty()) { "Cannot select from an empty anchor list" }
        return anchors.minBy { kotlin.math.abs(it.heightPx - heightPx) }
    }

    /** Resolve a fling target without allowing a single gesture to skip every stage. */
    fun flingTarget(
        anchors: List<DrawerAnchor>,
        currentHeightPx: Int,
        velocityY: Float,
        minimumFlingVelocity: Float
    ): DrawerAnchor {
        require(anchors.isNotEmpty()) { "Cannot select from an empty anchor list" }
        val ordered = anchors.sortedBy { it.heightPx }
        val currentIndex = ordered.indices.minBy { kotlin.math.abs(ordered[it].heightPx - currentHeightPx) }
        if (kotlin.math.abs(velocityY) < minimumFlingVelocity) {
            return nearestAnchor(ordered, currentHeightPx)
        }
        val nextIndex = (currentIndex + if (velocityY > 0f) 1 else -1)
            .coerceIn(ordered.indices)
        return ordered[nextIndex]
    }

    private fun rank(state: DrawerAnchorState): Int = when (state) {
        DrawerAnchorState.PEEK -> 0
        DrawerAnchorState.CUSTOM -> 1
        DrawerAnchorState.HALF_EXPANDED -> 2
        DrawerAnchorState.EXPANDED -> 3
    }
}
