package com.zerostudio.idea.widget.editor.symbolinput.v2.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.util.TypedValue
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.ThemeMode

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
                setTextColor(resolveTextColor(context, settings.themeMode))
                val selectableBackground = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, selectableBackground, true) && selectableBackground.resourceId != 0) {
                    setBackgroundResource(selectableBackground.resourceId)
                }
                textSize = 18f
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
            holder.label.text = if (settings.showSymbolLabels) item.label else item.icon?.takeIf { it.isNotBlank() } ?: item.label
            holder.label.contentDescription = if (settings.showActionHints) {
                "${item.label}, ${item.shortAction}"
            } else item.label
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
                longPressed = true
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
                    view.isPressed = true
                    if (hasLongAction) view.postDelayed(longPressTask, longPressTimeoutMs.toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!cancelled && (
                        kotlin.math.abs(event.x - downX) > touchSlop ||
                            kotlin.math.abs(event.y - downY) > touchSlop
                    )) {
                        cancelled = true
                        view.removeCallbacks(longPressTask)
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPressTask)
                    val shouldClick = !cancelled && !longPressed
                    view.isPressed = false
                    if (shouldClick) view.performClick()
                    cancelled = true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    view.removeCallbacks(longPressTask)
                    view.isPressed = false
                    cancelled = true
                }
            }
            return true
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

        private fun resolveTextColor(context: Context, mode: ThemeMode): Int {
            return when (mode) {
                ThemeMode.DARK -> Color.WHITE
                ThemeMode.LIGHT -> Color.BLACK
                ThemeMode.SYSTEM -> {
                    val value = TypedValue()
                    if (!context.theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)) {
                        Color.WHITE
                    } else if (value.resourceId != 0) {
                        runCatching { context.resources.getColor(value.resourceId) }
                            .getOrDefault(Color.WHITE)
                    } else {
                        value.data
                    }
                }
            }
        }
    }
}
