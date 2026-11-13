/*
 * This file is part of ZeroStudio.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import android.view.ViewGroup
import android.util.TypedValue
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveGridResolver

/**
 * Material Symbols Outlined "settings" vector, rendered directly from Kotlin.
 *
 * Keeping the path in source avoids platform-dependent menu preference glyphs
 * and guarantees the drawer uses the same gear silhouette on every API level.
 */
private class MaterialSettingsDrawable(
    color: Int
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }
    private val path = PathParser.createPathFromPathData(
        "M9.25,22l-0.4-3.2q-0.32-0.13-0.61-0.3T7.68,18.13L4.7,19.38L1.95,14.63l2.58-1.95Q4.5,12.5 4.5,12.34q0-0.16 0-0.34t0-0.34t0.03-0.34L1.95,9.38L4.7,4.63L7.68,5.88Q7.95,5.68 8.25,5.5t0.6-0.3L9.25,2h5.5l0.4,3.2q0.33,0.13 0.61,0.3t0.56,0.38L19.3,4.63l2.75,4.75l-2.57,1.95q0.02,0.18 0.02,0.34t0,0.34t0,0.34t-0.05,0.34l2.57,1.95l-2.75,4.75l-2.95-1.25q-0.27,0.2-0.57,0.38t-0.6,0.3L14.75,22H9.25ZM11,20h1.98l0.35-2.65q0.78-0.2 1.44-0.59t1.21-0.94l2.47,1.03l0.98-1.7l-2.15-1.63q0.13-0.35 0.17-0.74T17.5,12t-0.05-0.79q-0.05-0.39-0.17-0.74l2.15-1.63l-0.98-1.7L15.98,8.2q-0.55-0.57-1.21-0.96T13.33,6.65L13,4H11.03L10.68,6.65Q9.9,6.85 9.24,7.24T8.03,8.17L5.55,7.15L4.58,8.85l2.15,1.6Q6.6,10.83 6.55,11.2T6.5,12q0,0.4 0.05,0.77t0.17,0.75L4.58,15.15l0.98,1.7L8.03,15.8q0.55,0.58 1.21,0.96t1.44,0.59L11,20Zm1.05-4.5q1.45,0 2.47-1.03T15.55,12T14.53,9.52T12.05,8.5q-1.47,0-2.49,1.02T8.55,12t1.01,2.47t2.49,1.03ZM12,12Z"
    ) ?: Path()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        val source = RectF(0f, 0f, 24f, 24f)
        val target = RectF(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat())
        val matrix = Matrix().apply {
            setRectToRect(source, target, Matrix.ScaleToFit.CENTER)
        }
        val transformed = Path().apply { path.transform(matrix, this) }
        val save = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.drawPath(transformed, paint)
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** ViewPager2 page adapter backed by recycled grids and DiffUtil. */
internal class SymbolPageAdapter(
    private val config: DrawerConfig,
    private val settings: SymbolDrawerSettings,
    private val onSymbolAction: (SymbolEntry, Boolean) -> Unit
) : ListAdapter<SymbolGroup, SymbolPageAdapter.PageHolder>(GROUP_DIFF) {

    private val recycledPool = RecyclerView.RecycledViewPool().apply {
        setMaxRecycledViews(0, config.columns * 4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val grid = RecyclerView(parent.context).apply {
            // ViewPager2 validates the direct page child's LayoutParams when it
            // attaches it. RecyclerView's generated default is not guaranteed
            // to be MATCH_PARENT in both dimensions, so declare the page
            // contract explicitly instead of relying on its default params.
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(HostThemeColors.surface(context))
            val adaptiveGridLayout = GridLayoutManager(context, config.columns)
            layoutManager = adaptiveGridLayout
            isNestedScrollingEnabled = false
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            itemAnimator = null
            setRecycledViewPool(recycledPool)
            setPadding(
                dp(context, config.cellHorizontalSpacingDp),
                dp(context, 4),
                dp(context, config.cellHorizontalSpacingDp),
                dp(context, 4)
            )
            clipToPadding = false
            var appliedHorizontalPadding = Int.MIN_VALUE
            addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
                val availableWidth = (right - left - view.paddingLeft - view.paddingRight).coerceAtLeast(1)
                if (availableWidth <= 0) return@addOnLayoutChangeListener
                val density = resources.displayMetrics.density
                val info = WindowAdaptiveInfo(
                    availableWidth / density,
                    (bottom - top).coerceAtLeast(0) / density,
                    density
                )
                val gridSpec = AdaptiveGridResolver.resolve(
                    window = info,
                    preferredColumns = config.columns,
                    minimumCellWidthDp = maxOf(36f, settings.minimumTouchTargetDp.toFloat()),
                    baseHorizontalSpacingDp = config.cellHorizontalSpacingDp,
                    adaptive = true
                )
                if (adaptiveGridLayout.spanCount != gridSpec.columnCount) {
                    adaptiveGridLayout.spanCount = gridSpec.columnCount
                }
                val horizontal = dp(context, gridSpec.horizontalSpacingDp.coerceAtLeast(0))
                if (horizontal != appliedHorizontalPadding) {
                    appliedHorizontalPadding = horizontal
                    view.setPadding(horizontal, view.paddingTop, horizontal, view.paddingBottom)
                }
            }
        }
        return PageHolder(grid, config, settings, onSymbolAction)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(getItem(position).symbols)
    }

    override fun onViewRecycled(holder: PageHolder) {
        holder.clear()
    }

    class PageHolder(
        private val grid: RecyclerView,
        config: DrawerConfig,
        settings: SymbolDrawerSettings,
        onSymbolAction: (SymbolEntry, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(grid) {
        private val symbolsAdapter = SymbolAdapter(config, settings, onSymbolAction)
        init { grid.adapter = symbolsAdapter }
        fun bind(symbols: List<SymbolEntry>) = symbolsAdapter.submitList(symbols.toList())
        fun clear() = symbolsAdapter.submitList(emptyList())
    }

    private class SymbolAdapter(
        private val config: DrawerConfig,
        private val settings: SymbolDrawerSettings,
        private val onSymbolAction: (SymbolEntry, Boolean) -> Unit
    ) : ListAdapter<SymbolEntry, SymbolHolder>(SYMBOL_DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
            val label = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                setTextColor(HostThemeColors.onSurface(context))
                val selectableBackground = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, selectableBackground, true) && selectableBackground.resourceId != 0) {
                    setBackgroundResource(selectableBackground.resourceId)
                }
                textSize = 18f
                isSingleLine = true
                maxLines = 1
                includeFontPadding = false
                ellipsize = android.text.TextUtils.TruncateAt.END
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this, 8, 18, 1, TypedValue.COMPLEX_UNIT_SP
                )
                isClickable = true
                isFocusable = true
                minHeight = dp(context, maxOf(config.cellHeightDp, config.minimumTouchTargetDp))
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, maxOf(config.cellHeightDp, config.minimumTouchTargetDp))
                ).apply {
                    marginStart = dp(context, config.cellHorizontalSpacingDp / 2)
                    marginEnd = dp(context, config.cellHorizontalSpacingDp / 2)
                    topMargin = dp(context, config.cellVerticalSpacingDp / 2)
                    bottomMargin = dp(context, config.cellVerticalSpacingDp / 2)
                }
            }
            return SymbolHolder(label)
        }

        override fun onViewRecycled(holder: SymbolHolder) {
            holder.touchListener?.cancel()
            holder.label.setOnTouchListener(null)
            super.onViewRecycled(holder)
        }

        override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
            val item = getItem(position)
            val isSettingsAction = item.shortAction == SymbolAction.OpenManager
            if (isSettingsAction) {
                holder.label.text = ""
                val icon = MaterialSettingsDrawable(
                    HostThemeColors.onSurface(holder.label.context)
                )
                val size = dp(holder.label.context, 22)
                icon?.setBounds(0, 0, size, size)
                holder.label.setCompoundDrawables(null, icon, null, null)
                holder.label.contentDescription = "设置"
            } else {
                holder.label.setCompoundDrawables(null, null, null, null)
                holder.label.text = if (settings.showSymbolLabels) item.label else item.icon?.takeIf { it.isNotBlank() } ?: item.label
                holder.label.contentDescription = if (settings.showActionHints) {
                    "${item.label}, ${item.shortAction}"
                } else item.label
            }
            holder.label.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) onSymbolAction(getItem(current), false)
            }
            holder.touchListener?.cancel()
            holder.touchListener = SymbolCellTouchListener(
                view = holder.label,
                longPressTimeoutMs = settings.longPressTimeoutMs,
                hasLongAction = item.longAction != null,
                onLongPress = {
                    val current = holder.bindingAdapterPosition
                    if (current != RecyclerView.NO_POSITION && getItem(current).id == item.id) {
                        onSymbolAction(getItem(current), true)
                    }
                }
            )
            holder.label.setOnTouchListener(holder.touchListener)
        }
    }


    /**
     * Uses the configured timeout rather than the platform-global long-press
     * timeout. The touch stream is owned by this listener so a completed long
     * press cannot also dispatch the short action on ACTION_UP.
     */
    private class SymbolCellTouchListener(
        private val view: View,
        private val longPressTimeoutMs: Int,
        private val hasLongAction: Boolean,
        private val onLongPress: () -> Unit
    ) : View.OnTouchListener {
        private val touchSlop = android.view.ViewConfiguration.get(view.context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var cancelled = false
        private var longPressed = false
        private val longPressTask = Runnable {
            if (!cancelled && view.isPressed && hasLongAction) {
                // Clear the pressed state before dispatching the long action so
                // the TextView's native ACTION_UP handling cannot also click.
                longPressed = true
                view.isPressed = false
                onLongPress()
            }
        }

        fun cancel() {
            view.removeCallbacks(longPressTask)
            view.isPressed = false
            cancelled = true
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    cancelled = false
                    longPressed = false
                    if (hasLongAction) view.postDelayed(longPressTask, longPressTimeoutMs.toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!cancelled && (
                        kotlin.math.abs(event.x - downX) > touchSlop ||
                            kotlin.math.abs(event.y - downY) > touchSlop
                    )) {
                        cancelled = true
                        view.removeCallbacks(longPressTask)
                        // Keep the normal View/RecyclerView touch pipeline alive:
                        // scrolling and ViewPager2 swipes must be able to take over.
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPressTask)
                    if (cancelled || longPressed) view.isPressed = false
                    cancelled = true
                    // Do not synthesize a click here. Returning false lets the
                    // TextView perform its normal click exactly once for taps.
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    view.removeCallbacks(longPressTask)
                    cancelled = true
                    view.isPressed = false
                }
            }
            // Never steal the stream from RecyclerView/ViewPager2. The host view
            // remains responsible for taps; this listener only observes timing.
            return false
        }
    }

    private class SymbolHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        val label: TextView = view
        var touchListener: SymbolCellTouchListener? = null
    }

    private companion object {
        val GROUP_DIFF = object : DiffUtil.ItemCallback<SymbolGroup>() {
            override fun areItemsTheSame(old: SymbolGroup, new: SymbolGroup) = old.id == new.id
            override fun areContentsTheSame(old: SymbolGroup, new: SymbolGroup) = old == new
        }
        val SYMBOL_DIFF = object : DiffUtil.ItemCallback<SymbolEntry>() {
            override fun areItemsTheSame(old: SymbolEntry, new: SymbolEntry) = old.id == new.id
            override fun areContentsTheSame(old: SymbolEntry, new: SymbolEntry) = old == new
        }
        fun dp(context: Context, value: Int) =
            (value * context.resources.displayMetrics.density).toInt()

        // Colors always follow the host Activity theme.
    }
}
