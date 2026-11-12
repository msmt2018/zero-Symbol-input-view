package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

internal class SymbolGroupIndicator(
    context: Context,
    onGroupSelected: (Int) -> Unit
) : RecyclerView(context) {
    private val groupAdapter = GroupAdapter(onGroupSelected)

    init {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = groupAdapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
    }

    fun submitGroups(groups: List<SymbolGroup>) = groupAdapter.submitGroups(groups)

    fun applySettings(settings: SymbolDrawerSettings) {
        groupAdapter.applySettings(settings)
    }

    fun setSelectedIndex(index: Int) {
        groupAdapter.setSelectedIndex(index)
        if (index in 0 until groupAdapter.itemCount) {
            (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        }
    }

    private class GroupAdapter(
        private val onGroupSelected: (Int) -> Unit
    ) : ListAdapter<SymbolGroup, GroupHolder>(DIFF) {
        private var selectedIndex = 0
        private var settings = SymbolDrawerSettings()

        fun applySettings(value: SymbolDrawerSettings) {
            settings = value
            notifyItemRangeChanged(0, itemCount)
        }

        fun submitGroups(groups: List<SymbolGroup>) {
            submitList(groups.toList()) { setSelectedIndex(selectedIndex) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
            val view = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                textSize = 13f
                maxLines = 1
                setPadding(dp(parent, 12), dp(parent, 4), dp(parent, 12), dp(parent, 4))
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isClickable = true
            }
            return GroupHolder(view)
        }

        override fun onBindViewHolder(holder: GroupHolder, position: Int) {
            val group = getItem(position)
            val selected = position == selectedIndex
            holder.title.text = if (settings.showGroupTitles) group.title else (position + 1).toString()
            holder.title.alpha = if (selected) 1f else 0.68f
            holder.title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            val primary = HostThemeColors.onSurface(holder.title.context)
            val accent = HostThemeColors.primary(holder.title.context)
            holder.title.setTextColor(
                if (selected && settings.indicatorStyle == GroupIndicatorStyle.TEXT) accent
                else if (selected && settings.indicatorStyle == GroupIndicatorStyle.PILL) HostThemeColors.onPrimary(holder.title.context)
                else primary
            )
            holder.title.background = if (selected && settings.indicatorStyle == GroupIndicatorStyle.PILL) {
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(holder.title, 16).toFloat()
                    setColor(accent)
                }
            } else null
            holder.title.paintFlags = if (selected && settings.indicatorStyle == GroupIndicatorStyle.UNDERLINE) {
                holder.title.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            } else {
                holder.title.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            }
            holder.title.contentDescription = group.title
            holder.title.setOnClickListener {
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

    private class GroupHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view as TextView
    }
}
