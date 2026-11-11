package com.zerostudio.idea.widget.editor.symbolinput.v2.model

/**
 * Immutable symbol data consumed by the drawer UI.
 *
 * IDs must remain stable across refreshes. The UI never mutates caller-owned lists.
 */
data class SymbolGroup(
    val id: String,
    val title: String,
    val symbols: List<SymbolEntry>
)

data class SymbolEntry(
    val id: Long,
    val label: String,
    val shortAction: SymbolAction,
    val longAction: SymbolAction? = null
)

/**
 * Typed V2 action contract. This is deliberately independent from legacy integer
 * action IDs and from any concrete editor implementation.
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

enum class CursorTarget {
    LEFT, RIGHT, UP, DOWN, LINE_START, LINE_END, DOCUMENT_START, DOCUMENT_END
}

enum class DeleteKind {
    BACKWARD, FORWARD, CURRENT_LINE, TO_LINE_START, TO_LINE_END
}

enum class SelectionTransform {
    UPPERCASE, LOWERCASE, TOGGLE_CASE
}

enum class ClipboardOperation {
    COPY, CUT, PASTE
}

/** Versioned migration-only representation of the first-generation action schema. */
data class LegacySymbolAction(
    val type: Int,
    val payload: String? = null
)

data class DrawerConfig(
    val columns: Int = 10,
    val collapsedRows: Int = 2,
    val cellHeightDp: Int = 28,
    val cellHorizontalSpacingDp: Int = 2,
    val cellVerticalSpacingDp: Int = 2,
    val indicatorHeightDp: Int = 26,
    val animationDurationMs: Long = 200L,
    val flingVelocityDpPerSecond: Int = 900
) {
    init {
        require(columns in 1..20) { "columns must be in 1..20" }
        require(collapsedRows in 1..10) { "collapsedRows must be in 1..10" }
        require(cellHeightDp in 20..96) { "cellHeightDp must be in 20..96" }
        require(indicatorHeightDp in 0..64) { "indicatorHeightDp must be in 0..64" }
        require(cellHorizontalSpacingDp in 0..24)
        require(cellVerticalSpacingDp in 0..24)
        require(animationDurationMs in 0L..1200L)
        require(flingVelocityDpPerSecond in 100..6000)
    }
}
