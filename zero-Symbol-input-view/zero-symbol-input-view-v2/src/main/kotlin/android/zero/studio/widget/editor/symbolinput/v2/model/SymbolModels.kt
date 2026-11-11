package android.zero.studio.widget.editor.symbolinput.v2.model

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

data class SymbolAction(
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
    val indicatorRevealStart: Float = 0.08f,
    val indicatorRevealEnd: Float = 0.55f,
    val animationDurationMs: Long = 200L,
    val flingVelocityDpPerSecond: Int = 900
) {
    init {
        require(columns in 1..20) { "columns must be in 1..20" }
        require(collapsedRows in 1..10) { "collapsedRows must be in 1..10" }
        require(cellHeightDp in 20..96) { "cellHeightDp must be in 20..96" }
        require(indicatorHeightDp in 0..64) { "indicatorHeightDp must be in 0..64" }
        require(indicatorRevealStart in 0f..1f)
        require(indicatorRevealEnd in 0f..1f)
        require(indicatorRevealEnd > indicatorRevealStart)
    }
}
