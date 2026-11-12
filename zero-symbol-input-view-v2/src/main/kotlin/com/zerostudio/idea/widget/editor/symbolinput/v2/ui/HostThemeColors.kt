package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContextCompat

/**
 * Resolves colors from the live host theme without linking against a particular
 * Material Components R class. Resource IDs in dependency R classes are not a
 * stable runtime contract for a library embedded in arbitrary host APKs.
 *
 * Attribute lookup is name based and checks the host app first, then Material,
 * AppCompat and finally the Android framework. This also allows host-defined
 * theme overlays and custom palettes to take precedence.
 */
internal object HostThemeColors {

    fun surface(context: Context): Int = resolve(
        context,
        names = listOf("colorSurface", "colorBackground", "windowBackground"),
        fallback = Color.WHITE
    )

    fun onSurface(context: Context): Int = resolve(
        context,
        names = listOf("colorOnSurface", "textColorPrimary"),
        fallback = Color.BLACK
    )

    fun secondaryText(context: Context): Int = resolve(
        context,
        names = listOf("colorOnSurfaceVariant", "textColorSecondary"),
        fallback = onSurface(context)
    )

    fun primary(context: Context): Int = resolve(
        context,
        names = listOf("colorPrimary", "colorAccent"),
        fallback = 0xff6750a4.toInt()
    )

    fun onPrimary(context: Context): Int = resolve(
        context,
        names = listOf("colorOnPrimary", "textColorPrimaryInverse"),
        fallback = Color.WHITE
    )

    fun outline(context: Context): Int {
        val color = resolve(
            context,
            names = listOf("colorOutline", "colorControlHighlight", "textColorSecondary"),
            fallback = onSurface(context)
        )
        // Keep the host theme's RGB value while using a subtle outline alpha.
        return (color and 0x00ffffff) or 0x33000000
    }

    private fun resolve(
        context: Context,
        names: List<String>,
        fallback: Int
    ): Int {
        val packages = linkedSetOf(
            context.packageName,
            "com.google.android.material",
            "androidx.appcompat",
            "android"
        )

        for (name in names) {
            for (packageName in packages) {
                val attrId = context.resources.getIdentifier(name, "attr", packageName)
                if (attrId == 0) continue

                val value = TypedValue()
                if (!context.theme.resolveAttribute(attrId, value, true)) continue
                resolveValue(context, value, fallback)?.let { return it }
            }
        }
        return fallback
    }

    private fun resolveValue(
        context: Context,
        value: TypedValue,
        fallback: Int
    ): Int? {
        if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data
        }

        if (value.resourceId != 0) {
            val colorStateList: ColorStateList? = runCatching {
                ContextCompat.getColorStateList(context, value.resourceId)
            }.getOrNull()
            if (colorStateList != null) return colorStateList.defaultColor

            return runCatching {
                ContextCompat.getColor(context, value.resourceId)
            }.getOrNull() ?: fallback
        }

        return null
    }
}
