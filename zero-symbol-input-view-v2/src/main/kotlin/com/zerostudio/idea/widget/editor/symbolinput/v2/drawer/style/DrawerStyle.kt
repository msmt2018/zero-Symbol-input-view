package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style

/** Theme behavior of a drawer. FOLLOW_HOST is the default and never overrides the Activity theme. */
enum class DrawerThemeMode { FOLLOW_HOST, LIGHT, DARK }

data class DrawerColorAttributes(
    val surfaceAttr: Int = android.R.attr.colorBackground,
    val primaryTextAttr: Int = android.R.attr.textColorPrimary,
    val secondaryTextAttr: Int = android.R.attr.textColorSecondary,
    val accentAttr: Int = android.R.attr.colorAccent
)

data class DrawerColors(
    val surface: Int? = null,
    val primaryText: Int? = null,
    val secondaryText: Int? = null,
    val accent: Int? = null,
    val outline: Int? = null,
    val scrim: Int? = null
) {
    /** Explicit values override the host theme; null values inherit from it. */
    fun overlay(override: DrawerColors): DrawerColors = DrawerColors(
        surface = override.surface ?: surface,
        primaryText = override.primaryText ?: primaryText,
        secondaryText = override.secondaryText ?: secondaryText,
        accent = override.accent ?: accent,
        outline = override.outline ?: outline,
        scrim = override.scrim ?: scrim
    )
}

data class DrawerDimensions(
    val peekHeightDp: Float = 56f,
    val expandedMaxHeightDp: Float = 640f,
    val cornerRadiusDp: Float = 24f,
    val horizontalPaddingDp: Float = 16f,
    val verticalPaddingDp: Float = 12f,
    val panelSpacingDp: Float = 8f,
    val handleWidthDp: Float = 36f,
    val handleHeightDp: Float = 4f
) {
    init {
        require(peekHeightDp >= 0f && expandedMaxHeightDp >= peekHeightDp)
        require(cornerRadiusDp >= 0f && horizontalPaddingDp >= 0f)
        require(verticalPaddingDp >= 0f && panelSpacingDp >= 0f)
        require(handleWidthDp >= 0f && handleHeightDp >= 0f)
    }
}

data class DrawerTypography(
    val titleSizeSp: Float = 18f,
    val bodySizeSp: Float = 14f,
    val captionSizeSp: Float = 12f,
    val titleStyle: Int = android.graphics.Typeface.BOLD,
    val bodyStyle: Int = android.graphics.Typeface.NORMAL,
    val fontFamily: String? = null
) {
    init {
        require(titleSizeSp > 0f && bodySizeSp > 0f && captionSizeSp > 0f)
    }
}

data class DrawerMotion(
    val settleDurationMs: Long = 260L,
    val dragResistance: Float = 0.72f,
    val minimumFlingVelocityDp: Float = 900f,
    val maximumFlingVelocityDp: Float = 6000f,
    val enableSpring: Boolean = true
) {
    init {
        require(settleDurationMs >= 0L)
        require(dragResistance in 0.05f..1f)
        require(minimumFlingVelocityDp >= 0f)
        require(maximumFlingVelocityDp >= minimumFlingVelocityDp)
    }
}

data class DrawerShape(
    val topStartRadiusDp: Float = 24f,
    val topEndRadiusDp: Float = 24f,
    val bottomStartRadiusDp: Float = 0f,
    val bottomEndRadiusDp: Float = 0f,
    val outlineWidthDp: Float = 0f
) {
    init {
        require(topStartRadiusDp >= 0f && topEndRadiusDp >= 0f)
        require(bottomStartRadiusDp >= 0f && bottomEndRadiusDp >= 0f)
        require(outlineWidthDp >= 0f)
    }
}

/** Complete visual token set. Values are immutable and safe to share between drawers. */
data class DrawerStyle(
    val themeMode: DrawerThemeMode = DrawerThemeMode.FOLLOW_HOST,
    val colorAttributes: DrawerColorAttributes = DrawerColorAttributes(),
    val colors: DrawerColors = DrawerColors(),
    val dimensions: DrawerDimensions = DrawerDimensions(),
    val typography: DrawerTypography = DrawerTypography(),
    val motion: DrawerMotion = DrawerMotion(),
    val shape: DrawerShape = DrawerShape(),
    val elevationDp: Float = 12f,
    val drawScrim: Boolean = true,
    val clipContentToShape: Boolean = true
) {
    init { require(elevationDp >= 0f) }
}
