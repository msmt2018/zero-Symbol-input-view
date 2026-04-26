package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.max
import kotlin.math.roundToInt

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val tabLayout: TabLayout
    private val tabRow: View
    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val groupAdapter = GroupPagerAdapter()
    private var tabMediator: TabLayoutMediator? = null

    private val minRows = 2
    private val maxRows = 5
    private val spanCount = 8
    private var visibleRows = minRows
    private val fullTabHeight by lazy { (44 * resources.displayMetrics.density).roundToInt() }

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.adapter = groupAdapter
        viewPager.offscreenPageLimit = 1
        setExpansionFraction(0f)
        refreshData()
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    /**
     * Called by parent BottomSheetBehavior callback.
     * 0f = collapsed, 1f = expanded.
     */
    fun setExpansionFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val targetHeight = max(0, (fullTabHeight * clamped).roundToInt())
        val layoutParams = tabRow.layoutParams
        if (layoutParams.height != targetHeight) {
            layoutParams.height = targetHeight
            tabRow.layoutParams = layoutParams
        }
        tabRow.alpha = clamped
        tabRow.translationY = (1f - clamped) * -8f * resources.displayMetrics.density

        val newRows = minRows + ((maxRows - minRows) * clamped).roundToInt()
        if (newRows != visibleRows) {
            visibleRows = newRows
            groupAdapter.notifyVisibleRowsChanged()
        }
    }

    fun refreshData() {
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            groups.addAll(buildFallbackGroups())
        }
        viewPager.setCurrentItem(0, false)
        groupAdapter.notifyDataSetChanged()
        bindTabs()
    }

    private fun buildFallbackGroups(): List<SymbolGroup> {
        return listOf(
            SymbolGroup(
                name = "default",
                items = mutableListOf(
                    SymbolItem(0, "注释", "//"),
                    SymbolItem(18, "←"),
                    SymbolItem(20, "↑"),
                    SymbolItem(19, "→"),
                    SymbolItem(0, "\"", "\""),
                    SymbolItem(0, "'", "'"),
                    SymbolItem(0, ".", "."),
                    SymbolItem(0, ",", ","),
                    SymbolItem(0, "/", "/"),
                    SymbolItem(0, "//", "//"),
                    SymbolItem(21, "↓"),
                    SymbolItem(0, ":", ":"),
                    SymbolItem(0, ";", ";"),
                    SymbolItem(0, "#", "#"),
                    SymbolItem(0, "+", "+"),
                    SymbolItem(0, "-", "-"),
                    SymbolItem(22, "..."),
                )
            )
        )
    }

    private fun bindTabs() {
        tabMediator?.detach()
        if (groups.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = groups.getOrNull(position)?.name ?: "Tab ${position + 1}"
        }.apply { attach() }
    }

    private inner class GroupPagerAdapter : RecyclerView.Adapter<GroupPagerAdapter.GroupViewHolder>() {
        private val pageAdapters = mutableMapOf<Int, SymbolAdapter>()

        inner class GroupViewHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val rv = RecyclerView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                overScrollMode = OVER_SCROLL_NEVER
                isNestedScrollingEnabled = true
                layoutManager = GridLayoutManager(context, spanCount)
                clipToPadding = false
                val horizontal = (8 * resources.displayMetrics.density).roundToInt()
                val vertical = (2 * resources.displayMetrics.density).roundToInt()
                setPadding(horizontal, vertical, horizontal, vertical)
            }
            return GroupViewHolder(rv)
        }

        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            val group = groups.getOrNull(position) ?: return
            val adapter = pageAdapters.getOrPut(position) { SymbolAdapter(group.items) }
            holder.rv.adapter = adapter
        }

        override fun getItemCount(): Int = groups.size

        override fun notifyDataSetChanged() {
            pageAdapters.clear()
            super.notifyDataSetChanged()
        }

        fun notifyVisibleRowsChanged() {
            pageAdapters.values.forEach { it.notifyDataSetChanged() }
        }
    }

    private inner class SymbolAdapter(private val items: List<SymbolItem>) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {

        inner class SymbolViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
            val tv = TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                minHeight = (34 * resources.displayMetrics.density).roundToInt()
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                isClickable = true
                isFocusable = true

                val tvColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

                val tvBg = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                setBackgroundResource(tvBg.resourceId)
            }
            return SymbolViewHolder(tv)
        }

        override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
            val item = items[position]
            holder.tv.text = item.display

            holder.tv.setOnClickListener {
                editor?.let { ed ->
                    SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener)
                }
            }

            holder.tv.setOnLongClickListener {
                if (item.longAction != null) {
                    editor?.let { ed ->
                        SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener)
                    }
                    true
                } else {
                    false
                }
            }
        }

        override fun getItemCount(): Int = items.size.coerceAtMost(spanCount * visibleRows)
    }
}
