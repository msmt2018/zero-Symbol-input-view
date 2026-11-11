package android.zero.studio.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.zero.studio.widget.editor.symbolinput.v2.model.DrawerConfig
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

internal class SymbolPageAdapter(
    private val config: DrawerConfig,
    private val onSymbolAction: (SymbolEntry, Boolean) -> Unit
) : RecyclerView.Adapter<SymbolPageAdapter.PageHolder>() {
    private var groups: List<SymbolGroup> = emptyList()
    private val recycledPool = RecyclerView.RecycledViewPool().apply {
        setMaxRecycledViews(0, config.columns * 4)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = groups.size
    override fun getItemId(position: Int) = groups[position].id.hashCode().toLong()

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
        return PageHolder(grid, config, onSymbolAction)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(groups[position].symbols)
    }

    override fun onViewRecycled(holder: PageHolder) {
        holder.clear()
    }

    fun submitGroups(next: List<SymbolGroup>) {
        groups = next.toList()
        notifyDataSetChanged()
    }

    class PageHolder(
        private val grid: RecyclerView,
        private val config: DrawerConfig,
        private val onSymbolAction: (SymbolEntry, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(grid) {
        private val symbolsAdapter = SymbolAdapter(config, onSymbolAction)

        init {
            grid.adapter = symbolsAdapter
        }

        fun bind(symbols: List<SymbolEntry>) = symbolsAdapter.submitList(symbols.toList())
        fun clear() = symbolsAdapter.submitList(emptyList())
    }

    private class SymbolAdapter(
        private val config: DrawerConfig,
        private val onSymbolAction: (SymbolEntry, Boolean) -> Unit
    ) : ListAdapter<SymbolEntry, SymbolHolder>(DIFF) {
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
            holder.label.text = item.label
            holder.label.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != NO_POSITION) onSymbolAction(getItem(current), false)
            }
            holder.label.setOnLongClickListener {
                val current = holder.bindingAdapterPosition
                if (current != NO_POSITION) onSymbolAction(getItem(current), true)
                true
            }
        }

        private companion object {
            val DIFF = object : DiffUtil.ItemCallback<SymbolEntry>() {
                override fun areItemsTheSame(old: SymbolEntry, new: SymbolEntry) = old.id == new.id
                override fun areContentsTheSame(old: SymbolEntry, new: SymbolEntry) = old == new
            }
        }
    }

    private class SymbolHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        val label: TextView = view
    }

    companion object {
        private fun dp(context: Context, value: Int) =
            (value * context.resources.displayMetrics.density).toInt()
    }
}

private fun dp(context: Context, value: Int) =
    (value * context.resources.displayMetrics.density).toInt()
