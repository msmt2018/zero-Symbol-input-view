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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass

/** Horizontal group navigation with a persistent selected-item indicator. */
internal class SymbolGroupIndicator(
    context: Context,
    onGroupSelected: (Int) -> Unit
) : FrameLayout(context) {
    private val groupAdapter = GroupAdapter(onGroupSelected)
    private val groupList = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setBackgroundColor(HostThemeColors.surface(context))
        adapter = groupAdapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
    }
    init {
        setBackgroundColor(HostThemeColors.surface(context))
        addView(groupList, LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        ))
    }

    fun submitGroups(groups: List<SymbolGroup>) = groupAdapter.submitGroups(groups)
    fun applySettings(settings: SymbolDrawerSettings) = groupAdapter.applySettings(settings)

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color)
        groupList.setBackgroundColor(color)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) {
            groupAdapter.updateWindowWidth(
                w.coerceAtLeast(1),
                resources.displayMetrics.density
            )
        }
    }

    fun setSelectedIndex(index: Int) {
        groupAdapter.setSelectedIndex(index)
        if (index in 0 until groupAdapter.itemCount) {
            (groupList.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        }
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private class GroupAdapter(
        private val onGroupSelected: (Int) -> Unit
    ) : ListAdapter<SymbolGroup, GroupHolder>(DIFF) {
        private var selectedIndex = 0
        private var settings = SymbolDrawerSettings()
        private var horizontalPaddingDp = 14
        private var titleTextSizeSp = 14f

        fun updateWindowWidth(widthPx: Int, density: Float) {
            lastWindowWidthPx = widthPx
            lastDensity = density
            val info = WindowAdaptiveInfo(widthPx / density, 0f, density)
            val fixedDimensions =
                settings.largeScreenLayoutMode == com.zerostudio.idea.widget.editor.symbolinput.v2.settings.LargeScreenLayoutMode.BOTTOM
            val next = if (fixedDimensions) 14 else when (info.sizeClass) {
                WindowSizeClass.COMPACT -> 10
                WindowSizeClass.MEDIUM -> 14
                WindowSizeClass.EXPANDED -> 18
            }
            val nextTextSize = if (fixedDimensions) 14f else when (info.sizeClass) {
                WindowSizeClass.COMPACT -> 13f
                WindowSizeClass.MEDIUM -> 14f
                WindowSizeClass.EXPANDED -> 15f
            }
            if (next == horizontalPaddingDp && nextTextSize == titleTextSizeSp) return
            horizontalPaddingDp = next
            titleTextSizeSp = nextTextSize
            notifyItemRangeChanged(0, itemCount)
        }

        private var lastWindowWidthPx = 0
        private var lastDensity = 1f

        fun applySettings(value: SymbolDrawerSettings) {
            settings = value
            if (lastWindowWidthPx > 0) updateWindowWidth(lastWindowWidthPx, lastDensity)
            notifyItemRangeChanged(0, itemCount)
        }

        fun submitGroups(groups: List<SymbolGroup>) {
            submitList(groups.toList()) { setSelectedIndex(selectedIndex) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
            val root = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(this, horizontalPaddingDp), 0, dp(this, horizontalPaddingDp), 0)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isClickable = true
            }
            val title = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                textSize = 14f
                maxLines = 1
                setPadding(0, dp(this, 5), 0, dp(this, 5))
                setTextColor(HostThemeColors.onSurface(context))
            }
            val indicator = View(parent.context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(parent, 3).toFloat()
                    setColor(HostThemeColors.primary(context))
                }
                visibility = View.INVISIBLE
            }
            root.addView(title, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1f
            ))
            root.addView(indicator, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(root, 3)
            ))
            return GroupHolder(root, title, indicator)
        }

        override fun onBindViewHolder(holder: GroupHolder, position: Int) {
            val group = getItem(position)
            val selected = position == selectedIndex
            val context = holder.title.context
            holder.root.setPadding(
                dp(holder.root, horizontalPaddingDp), 0,
                dp(holder.root, horizontalPaddingDp), 0
            )
            holder.title.text = if (settings.showGroupTitles) group.title else (position + 1).toString()
            holder.title.textSize = titleTextSizeSp
            holder.title.alpha = if (selected) 1f else 0.68f
            holder.title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            holder.title.setTextColor(
                if (selected && settings.indicatorStyle != GroupIndicatorStyle.PILL) {
                    HostThemeColors.primary(context)
                } else if (selected) {
                    HostThemeColors.onPrimary(context)
                } else {
                    HostThemeColors.onSurface(context)
                }
            )
            holder.indicator.background = GradientDrawable().apply {
                cornerRadius = dp(holder.indicator, 3).toFloat()
                setColor(HostThemeColors.primary(context))
            }
            holder.indicator.visibility =
                if (selected && settings.indicatorStyle == GroupIndicatorStyle.UNDERLINE) View.VISIBLE
                else View.INVISIBLE
            holder.root.background = if (selected && settings.indicatorStyle == GroupIndicatorStyle.PILL) {
                GradientDrawable().apply {
                    cornerRadius = dp(holder.root, 16).toFloat()
                    setColor(HostThemeColors.primary(context))
                }
            } else null
            holder.root.contentDescription = group.title
            holder.root.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) onGroupSelected(current)
            }
        }

        fun setSelectedIndex(index: Int) {
            val previous = selectedIndex
            selectedIndex = index.coerceAtLeast(0)
            if (previous in 0 until itemCount) notifyItemChanged(previous)
            if (selectedIndex in 0 until itemCount) notifyItemChanged(selectedIndex)
        }

        private fun dp(view: View, value: Int) =
            (value * view.resources.displayMetrics.density).toInt()

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<SymbolGroup>() {
                override fun areItemsTheSame(old: SymbolGroup, new: SymbolGroup) = old.id == new.id
                override fun areContentsTheSame(old: SymbolGroup, new: SymbolGroup) = old == new
            }
        }
    }

    private class GroupHolder(
        val root: LinearLayout,
        val title: TextView,
        val indicator: View
    ) : RecyclerView.ViewHolder(root)
}
