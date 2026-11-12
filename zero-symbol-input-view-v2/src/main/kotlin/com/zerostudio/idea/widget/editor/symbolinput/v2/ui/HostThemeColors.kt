package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContextCompat

/** Resolves the live host Activity palette, preferring Material theme attributes. */
internal object HostThemeColors {
    fun surface(context: Context): Int = resolve(context, com.google.android.material.R.attr.colorSurface, android.R.attr.colorBackground, Color.WHITE)
    fun onSurface(context: Context): Int = resolve(context, com.google.android.material.R.attr.colorOnSurface, android.R.attr.textColorPrimary, Color.BLACK)
    fun secondaryText(context: Context): Int = resolve(context, com.google.android.material.R.attr.colorOnSurfaceVariant, android.R.attr.textColorSecondary, onSurface(context))
    fun primary(context: Context): Int = resolve(context, com.google.android.material.R.attr.colorPrimary, android.R.attr.colorAccent, 0xff6750a4.toInt())
    fun onPrimary(context: Context): Int = resolve(context, com.google.android.material.R.attr.colorOnPrimary, android.R.attr.textColorPrimaryInverse, Color.WHITE)

    fun outline(context: Context): Int {
        val color = resolve(context, com.google.android.material.R.attr.colorOutline, android.R.attr.textColorSecondary, onSurface(context))
        return (color and 0x00ffffff) or 0x33000000
    }

    private fun resolve(context: Context, materialAttr: Int, platformAttr: Int, fallback: Int): Int {
        val value = TypedValue()
        val found = context.theme.resolveAttribute(materialAttr, value, true) ||
            context.theme.resolveAttribute(platformAttr, value, true)
        if (!found) return fallback
        if (value.resourceId != 0) {
            val list: ColorStateList? = runCatching {
                ContextCompat.getColorStateList(context, value.resourceId)
            }.getOrNull()
            if (list != null) return list.defaultColor
            return runCatching { ContextCompat.getColor(context, value.resourceId) }.getOrDefault(fallback)
        }
        return if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) value.data else fallback
    }
}
