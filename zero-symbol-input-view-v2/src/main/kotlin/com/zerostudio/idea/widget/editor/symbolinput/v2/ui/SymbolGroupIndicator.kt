package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.GroupIndicatorStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

/** Horizontal group navigation with a persistent selected-item indicator. */
internal class SymbolGroupIndicator(
    context: Context,
    onGroupSelected: (Int) -> Unit
) : RecyclerView(context) {
    private val groupAdapter = GroupAdapter(onGroupSelected)

    init {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setBackgroundColor(HostThemeColors.surface(context))
        adapter = groupAdapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
    }

    fun submitGroups(groups: List<SymbolGroup>) = groupAdapter.submitGroups(groups)
    fun applySettings(settings: SymbolDrawerSettings) = groupAdapter.applySettings(settings)

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
            val root = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(this, 14), 0, dp(this, 14), 0)
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
            holder.title.text = if (settings.showGroupTitles) group.title else (position + 1).toString()
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
