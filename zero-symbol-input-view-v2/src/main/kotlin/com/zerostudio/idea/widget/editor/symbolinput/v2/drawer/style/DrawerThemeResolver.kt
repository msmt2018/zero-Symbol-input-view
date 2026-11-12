package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style

import android.content.Context
import android.view.ContextThemeWrapper
import android.util.TypedValue
import com.zerostudio.idea.widget.editor.symbolinput.v2.R

/** Resolves theme attributes at runtime so host Activity theme changes can be reflected. */
object DrawerThemeResolver {
    @JvmStatic
    fun contextFor(context: Context, style: DrawerStyle = DrawerStyle()): Context =
        when (style.themeMode) {
            DrawerThemeMode.FOLLOW_HOST -> context
            DrawerThemeMode.LIGHT -> ContextThemeWrapper(context, R.style.DrawerThemeOverlay_Light)
            DrawerThemeMode.DARK -> ContextThemeWrapper(context, R.style.DrawerThemeOverlay_Dark)
        }

    @JvmStatic
    fun resolve(context: Context, style: DrawerStyle = DrawerStyle()): DrawerColors {
        val themedContext = contextFor(context, style)
        val attrs = style.colorAttributes
        val inherited = DrawerColors(
            surface = resolveColor(themedContext, attrs.surfaceAttr),
            primaryText = resolveColor(themedContext, attrs.primaryTextAttr),
            secondaryText = resolveColor(themedContext, attrs.secondaryTextAttr),
            accent = resolveColor(themedContext, attrs.accentAttr)
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
