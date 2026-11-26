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

package com.zerostudio.idea.widget.editor.symbolinput.v2.model

/**
 * A logical page/group of symbols displayed by SymbolDrawer.
 *
 * @property id Stable, non-blank identity used to preserve selection across refreshes.
 * @property title User-visible group title.
 * @property symbols Ordered snapshot of the entries in this group.
 */
data class SymbolGroup(
    val id: String,
    val title: String,
    val symbols: List<SymbolEntry>
)

/**
 * A selectable symbol and its short-/long-press action definitions.
 *
 * IDs must be unique across the repository snapshot. Legacy fields are retained
 * so V1 JSON can be decoded and re-encoded without losing numeric action data.
 *
 * @property id Stable symbol identifier; zero may be reserved by manager APIs for generated IDs.
 * @property label Visible symbol label or insertion glyph.
 * @property shortAction Action invoked by a short click.
 * @property longAction Optional action invoked by a long click.
 * @property icon Optional presentation icon token.
 * @property legacyShortActionId Original V1 short-action numeric identifier.
 * @property legacyShortText Original V1 short-action payload/text.
 * @property legacyLongActionId Original V1 long-action numeric identifier.
 * @property legacyLongText Original V1 long-action payload/text.
 */
data class SymbolEntry(
    val id: Long,
    val label: String,
    val shortAction: SymbolAction,
    val longAction: SymbolAction? = null,
    /** V1 numeric action IDs and payloads are retained verbatim for lossless JSON compatibility. */
    val icon: String? = null,
    val legacyShortActionId: Int? = null,
    val legacyShortText: String? = null,
    val legacyLongActionId: Int? = null,
    val legacyLongText: String? = null
)

/**
 * Typed, editor-neutral operation associated with a SymbolEntry.
 * Implementations describe intent; SymbolActionHost performs the operation
 * against the currently focused editor/document.
 */
sealed interface SymbolAction {
    data class InsertText(val template: String) : SymbolAction
    data class ReplaceSelection(val text: String) : SymbolAction
    data class SurroundSelection(val prefix: String, val suffix: String) : SymbolAction
    data class MoveCursor(val target: CursorTarget) : SymbolAction
    data class Delete(val kind: DeleteKind) : SymbolAction
    data class TransformSelection(val transform: SelectionTransform) : SymbolAction
    data class EditorCommand(val commandId: String, val payload: String? = null) : SymbolAction
    data class Clipboard(val operation: ClipboardOperation) : SymbolAction
    data object OpenManager : SymbolAction
    data class Composite(val actions: List<SymbolAction>) : SymbolAction
    data class Extension(val extensionId: String, val payload: String? = null) : SymbolAction
}

/** Relative or absolute cursor destination used by MoveCursor. */
enum class CursorTarget {
    LEFT, RIGHT, UP, DOWN, LINE_START, LINE_END, DOCUMENT_START, DOCUMENT_END
}
/** Deletion direction/range used by Delete. */
enum class DeleteKind { BACKWARD, FORWARD, CURRENT_LINE, TO_LINE_START, TO_LINE_END }
/** Built-in transformation applied to the current selection. */
enum class SelectionTransform { UPPERCASE, LOWERCASE, TOGGLE_CASE }
/** Clipboard operation requested by Clipboard. */
enum class ClipboardOperation { COPY, CUT, PASTE }

/**
 * Loss-preserving representation of a first-generation numeric action.
 * This type is intended for import/migration compatibility, not new integrations.
 *
 * @property type Original V1 numeric action identifier.
 * @property payload Original optional action payload.
 */
data class LegacySymbolAction(val type: Int, val payload: String? = null)

/**
 * Symbol-grid presentation and interaction configuration.
 *
 * This feature-level configuration is distinct from the generic Drawer
 * container configuration in the drawer.config package. Dimensions are dp
 * unless a property name states otherwise. Constructor validation rejects
 * values outside the supported ranges.
 *
 * @property columns Preferred number of symbol columns, in the range 1..30.
 * @property collapsedRows Number of rows visible while the drawer is collapsed.
 * @property cellHeightDp Symbol cell height in dp.
 * @property cellHorizontalSpacingDp Horizontal cell spacing in dp; negative values allow overlap.
 * @property cellVerticalSpacingDp Vertical cell spacing in dp; negative values allow overlap.
 * @property indicatorHeightDp Group-indicator height in dp.
 * @property animationDurationMs Drawer transition duration in milliseconds.
 * @property flingVelocityDpPerSecond Fling threshold in dp per second.
 * @property maxExpandedHeightDp Upper bound for expanded drawer height in dp.
 * @property minimumTouchTargetDp Minimum symbol interaction target in dp.
 */
data class DrawerConfig(
    val columns: Int = 10,
    val collapsedRows: Int = 2,
    val cellHeightDp: Int = 28,
    val cellHorizontalSpacingDp: Int = 2,
    val cellVerticalSpacingDp: Int = 2,
    val indicatorHeightDp: Int = 26,
    val animationDurationMs: Long = 200L,
    val flingVelocityDpPerSecond: Int = 900,
    val maxExpandedHeightDp: Int = 480,
    val minimumTouchTargetDp: Int = 28
) {
    init {
        require(columns in 1..30) { "columns must be in 1..30" }
        require(collapsedRows in 1..16) { "collapsedRows must be in 1..16" }
        require(cellHeightDp in 6..128) { "cellHeightDp must be in 6..128" }
        require(indicatorHeightDp in 0..96) { "indicatorHeightDp must be in 0..96" }
        require(cellHorizontalSpacingDp in -24..48)
        require(cellVerticalSpacingDp in -24..48)
        require(animationDurationMs in 0L..1200L)
        require(flingVelocityDpPerSecond in 100..6000)
        require(maxExpandedHeightDp in 80..1800)
        require(minimumTouchTargetDp in 20..112)
    }
}
