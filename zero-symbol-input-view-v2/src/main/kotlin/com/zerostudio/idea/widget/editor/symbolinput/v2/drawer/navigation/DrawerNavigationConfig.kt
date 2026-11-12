package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

/** Placement is independent of the page host and may be changed by a consumer. */
enum class DrawerNavigationPlacement { TOP, BOTTOM, HIDDEN }

/** Visual indicator contract for TabLayout-like navigation widgets. */
enum class DrawerIndicatorShape { LINE, PILL, NONE }

data class DrawerIndicatorStyle(
    val shape: DrawerIndicatorShape = DrawerIndicatorShape.LINE,
    val heightDp: Float = 3f,
    val widthFraction: Float = 0.72f,
    val cornerRadiusDp: Float = 3f,
    val color: Int? = null,
    val animate: Boolean = true
) {
    init {
        require(heightDp >= 0f)
        require(widthFraction in 0f..1f)
        require(cornerRadiusDp >= 0f)
    }
}

/** Configuration for optional page/group navigation; no dependency on SymbolGroup. */
data class DrawerNavigationConfig(
    val enabled: Boolean = true,
    val placement: DrawerNavigationPlacement = DrawerNavigationPlacement.TOP,
    val indicator: DrawerIndicatorStyle = DrawerIndicatorStyle(),
    val scrollable: Boolean = true,
    val showTitles: Boolean = true,
    val showIcons: Boolean = false,
    val itemMinWidthDp: Float = 64f,
    val itemHorizontalPaddingDp: Float = 12f,
    val itemVerticalPaddingDp: Float = 10f,
    val titleTextSizeSp: Float = 14f,
    val selectedTitleTextSizeSp: Float = 14f,
    val selectedTitleBold: Boolean = false,
    val keepSelectedPageVisible: Boolean = true,
    val showInPeekPanel: Boolean = false
) {
    init {
        require(itemMinWidthDp >= 0f)
        require(itemHorizontalPaddingDp >= 0f && itemVerticalPaddingDp >= 0f)
        require(titleTextSizeSp > 0f && selectedTitleTextSizeSp > 0f)
    }
}
