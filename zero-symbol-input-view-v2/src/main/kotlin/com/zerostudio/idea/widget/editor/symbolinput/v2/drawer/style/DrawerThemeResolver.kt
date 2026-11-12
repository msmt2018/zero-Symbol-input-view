package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style

import android.content.Context
import android.util.TypedValue

/** Resolves theme attributes at runtime so host Activity theme changes can be reflected. */
object DrawerThemeResolver {
    @JvmStatic
    fun resolve(context: Context, style: DrawerStyle = DrawerStyle()): DrawerColors {
        val attrs = style.colorAttributes
        val inherited = DrawerColors(
            surface = resolveColor(context, attrs.surfaceAttr),
            primaryText = resolveColor(context, attrs.primaryTextAttr),
            secondaryText = resolveColor(context, attrs.secondaryTextAttr),
            accent = resolveColor(context, attrs.accentAttr)
        )
        return inherited.overlay(style.colors)
    }

    private fun resolveColor(context: Context, attribute: Int): Int? {
        if (attribute == 0) return null
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attribute, value, true)) {
            when {
                value.resourceId != 0 -> runCatching {
                    @Suppress("DEPRECATION")
                    context.resources.getColor(value.resourceId)
                }.getOrNull()
                value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> value.data
                else -> null
            }
        } else null
    }
}
