/*
 * This file is part of zero-Symbol-input-view-V2.
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

import androidx.compose.runtime.Immutable

/** Runtime snapshot shared by the container and application-facing controller. */
@Immutable
class DrawerState(
    val currentAnchorId: String,
    val currentHeightPx: Int,
    anchors: List<DrawerAnchor>,
    val isDragging: Boolean = false,
    val isAnimating: Boolean = false,
    /** Stable destination selected for the active animation, if any. */
    val targetAnchorId: String? = null
) {
    /**
     * Immutable defensive snapshot. A caller may pass a mutable list to the
     * constructor, but subsequent mutations cannot alter this published state.
     */
    val anchors: List<DrawerAnchor> =
        java.util.Collections.unmodifiableList(ArrayList(anchors))

    init {
        require(currentAnchorId.isNotBlank())
        require(currentHeightPx >= 0)
        require(this.anchors.isNotEmpty())
        require(this.anchors.map { it.id }.distinct().size == this.anchors.size) {
            "Drawer state anchor IDs must be unique"
        }
        require(targetAnchorId == null || this.anchors.any { it.id == targetAnchorId }) {
            "Target anchor '$targetAnchorId' must exist in the state anchor list"
        }
        require(this.anchors.any { it.id == currentAnchorId }) {
            "Current anchor '$currentAnchorId' must exist in the state anchor list"
        }
    }

    val currentAnchor: DrawerAnchor?
        get() = anchors.firstOrNull { it.id == currentAnchorId }

    /** True only when the continuous height exactly matches a stable anchor. */
    val isAtStableAnchor: Boolean
        get() = !isDragging && !isAnimating && currentAnchor?.heightPx == currentHeightPx

    /** Whether the drawer is above its most-collapsed (Peek) position. */
    val isExpanded: Boolean
        get() = currentHeightPx > (anchors.minOfOrNull { it.heightPx } ?: 0)

    /** Normalized progress from the lowest to the highest resolved anchor. */
    val expansionFraction: Float
        get() {
            val minHeight = anchors.minOfOrNull { it.heightPx } ?: return 0f
            val maxHeight = anchors.maxOfOrNull { it.heightPx } ?: return 0f
            if (maxHeight <= minHeight) return 0f
            return ((currentHeightPx - minHeight).toFloat() / (maxHeight - minHeight))
                .coerceIn(0f, 1f)
        }

    val isAtPeekAnchor: Boolean
        get() = anchors.minByOrNull { it.heightPx }?.id == currentAnchorId &&
            currentAnchor?.heightPx == currentHeightPx && !isDragging && !isAnimating

    val isAtExpandedAnchor: Boolean
        get() = anchors.maxByOrNull { it.heightPx }?.id == currentAnchorId &&
            currentAnchor?.heightPx == currentHeightPx && !isDragging && !isAnimating

    /** Retains the source-level copy contract of the former data class. */
    fun copy(
        currentAnchorId: String = this.currentAnchorId,
        currentHeightPx: Int = this.currentHeightPx,
        anchors: List<DrawerAnchor> = this.anchors,
        isDragging: Boolean = this.isDragging,
        isAnimating: Boolean = this.isAnimating,
        targetAnchorId: String? = this.targetAnchorId
    ): DrawerState = DrawerState(
        currentAnchorId,
        currentHeightPx,
        anchors,
        isDragging,
        isAnimating,
        targetAnchorId
    )

    operator fun component1(): String = currentAnchorId
    operator fun component2(): Int = currentHeightPx
    operator fun component3(): List<DrawerAnchor> = anchors
    operator fun component4(): Boolean = isDragging
    operator fun component5(): Boolean = isAnimating
    operator fun component6(): String? = targetAnchorId

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is DrawerState &&
                currentAnchorId == other.currentAnchorId &&
                currentHeightPx == other.currentHeightPx &&
                anchors == other.anchors &&
                isDragging == other.isDragging &&
                isAnimating == other.isAnimating &&
                targetAnchorId == other.targetAnchorId
            )

    override fun hashCode(): Int {
        var result = currentAnchorId.hashCode()
        result = 31 * result + currentHeightPx
        result = 31 * result + anchors.hashCode()
        result = 31 * result + isDragging.hashCode()
        result = 31 * result + isAnimating.hashCode()
        result = 31 * result + (targetAnchorId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "DrawerState(currentAnchorId=$currentAnchorId, currentHeightPx=$currentHeightPx, " +
            "anchors=$anchors, isDragging=$isDragging, isAnimating=$isAnimating, " +
            "targetAnchorId=$targetAnchorId)"
}
