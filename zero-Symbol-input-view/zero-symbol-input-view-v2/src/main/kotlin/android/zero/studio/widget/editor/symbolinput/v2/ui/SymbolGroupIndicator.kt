package android.zero.studio.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup

internal class SymbolGroupIndicator(
    context: Context,
    onGroupSelected: (Int) -> Unit
) : RecyclerView(context) {
    private val groupAdapter = GroupAdapter(onGroupSelected)

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        adapter = groupAdapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
    }

    fun submitGroups(groups: List<SymbolGroup>) {
        groupAdapter.submitGroups(groups)
    }

    fun setSelectedIndex(index: Int) = groupAdapter.setSelectedIndex(index)

    private class GroupAdapter(
        private val onGroupSelected: (Int) -> Unit
    ) : ListAdapter<SymbolGroup, GroupHolder>(DIFF) {
        private var selectedIndex = 0

        fun submitGroups(groups: List<SymbolGroup>) {
            submitList(groups.toList()) { setSelectedIndex(selectedIndex) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
            val view = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 13f
                maxLines = 1
                setPadding(dp(12), dp(4), dp(12), dp(4))
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
            holder.title.text = group.title
            val selected = position == selectedIndex
            holder.title.alpha = if (selected) 1f else 0.58f
            holder.title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            holder.title.setOnClickListener { onGroupSelected(holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener) }
        }

        fun setSelectedIndex(index: Int) {
            val previous = selectedIndex
            selectedIndex = index.coerceAtLeast(0)
            if (previous in 0 until itemCount) notifyItemChanged(previous)
            if (selectedIndex in 0 until itemCount) {
                notifyItemChanged(selectedIndex)
                (layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(selectedIndex, 0)
            }
        }

        private fun TextView.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

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
