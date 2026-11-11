package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings

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
            layoutManager = GridLayoutManager(context, config.columns)
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
                setTextColor(Color.WHITE)
                textSize = 18f
                isClickable = true
                isFocusable = true
                minHeight = dp(context, config.cellHeightDp)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, config.cellHeightDp)
                ).apply {
                    marginStart = dp(context, config.cellHorizontalSpacingDp / 2)
                    marginEnd = dp(context, config.cellHorizontalSpacingDp / 2)
                    topMargin = dp(context, config.cellVerticalSpacingDp / 2)
                    bottomMargin = dp(context, config.cellVerticalSpacingDp / 2)
                }
            }
            return SymbolHolder(label)
        }

        override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
            val item = getItem(position)
            holder.label.text = if (settings.showSymbolLabels) item.label else item.icon.orEmpty()
            holder.label.contentDescription = if (settings.showActionHints) {
                "${item.label}, ${item.shortAction}"
            } else item.label
            holder.label.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) onSymbolAction(getItem(current), false)
            }
            holder.label.setOnLongClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) onSymbolAction(getItem(current), true)
                true
            }
        }
    }

    private class SymbolHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        val label: TextView = view
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
    }
}
