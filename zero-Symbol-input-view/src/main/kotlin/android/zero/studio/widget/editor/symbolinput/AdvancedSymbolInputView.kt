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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.max
import kotlin.math.roundToInt

// @author android_zero

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val tabLayout: TabLayout
    private val tabRow: View
    private var editor: CodeEditor? = null
    
    /**
     * 打开管理器界面回调监听器
     */
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val groupAdapter = GroupPagerAdapter()
    private var tabMediator: TabLayoutMediator? = null

    private val spanCount = 8
    private val fullTabHeight by lazy { (44 * resources.displayMetrics.density).roundToInt() }
    
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var registeredBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null

    /**
     * 是否跟随系统输入法的动画而变动。
     * 当前实现下依赖外部 CoordinatorLayout 通过 applyEdgeToEdge 的 Padding 自动将其撑起，
     */
    var followSystemIme: Boolean = false

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.adapter = groupAdapter
        viewPager.offscreenPageLimit = 1
        viewPager.isSaveEnabled = false
        setExpansionFraction(0f)
        refreshData()
    }

    /**
     * 绑定代码编辑器实例以响应符号动作
     */
    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    /**
     * 在抽屉伸缩期间动态处理顶端 TabRow 的隐藏与展示动作。
     * 取消对可见行数的反复重建更新，避免滑动被打断及引发适配器崩溃。
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
    }

    /**
     * 重新从本地存储加载符号配置数据并刷新适配器
     */
    fun refreshData() {
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            groups.addAll(buildFallbackGroups())
        }
        val newCount = groups.size
        if (viewPager.currentItem >= newCount) {
            viewPager.setCurrentItem(max(0, newCount - 1), false)
        }
        groupAdapter.clearPageAdapters()
        groupAdapter.notifyDataSetChanged()
        bindTabs()
    }

    /**
     * 设置底栏相关的控制行为逻辑
     */
    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.let { previousBehavior ->
            registeredBottomSheetCallback?.let { previousCallback ->
                previousBehavior.removeBottomSheetCallback(previousCallback)
            }
        }
        bottomSheetBehavior = behavior
        behavior.saveFlags = BottomSheetBehavior.SAVE_NONE
        behavior.isHideable = false
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.isFitToContents = true
        
        bottomSheet.post {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        val sheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                    BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setExpansionFraction(slideOffset.coerceIn(0f, 1f))
            }
        }
        behavior.addBottomSheetCallback(sheetCallback)
        registeredBottomSheetCallback = sheetCallback
    }

    /**
     * 生命周期在应用切回前台时折叠菜单
     */
    fun onHostResume() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
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
        detachTabMediatorSafely()
        if (groups.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = groups.getOrNull(position)?.name ?: "Tab ${position + 1}"
        }.apply { attach() }
    }

    private fun detachTabMediatorSafely() {
        val mediator = tabMediator ?: return
        try {
            mediator.detach()
        } catch (_: IllegalStateException) {
        }
        tabMediator = null
    }

    private inner class GroupPagerAdapter : RecyclerView.Adapter<GroupPagerAdapter.GroupViewHolder>() {

        private val pageAdapters = mutableMapOf<Int, SymbolAdapter>()

        inner class GroupViewHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val rv = RecyclerView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setHasFixedSize(true)
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
            val symbolAdapter = pageAdapters.getOrPut(position) { SymbolAdapter(group.items) }
            if (holder.rv.adapter !== symbolAdapter) {
                holder.rv.adapter = symbolAdapter
            }
        }

        override fun getItemCount(): Int = groups.size

        fun clearPageAdapters() {
            pageAdapters.clear()
        }
    }

    override fun onDetachedFromWindow() {
        tabMediator?.detach()
        tabMediator = null
        bottomSheetBehavior?.let { behavior ->
            registeredBottomSheetCallback?.let { callback ->
                behavior.removeBottomSheetCallback(callback)
            }
        }
        registeredBottomSheetCallback = null
        bottomSheetBehavior = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (tabMediator == null && groups.isNotEmpty()) {
            bindTabs()
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

        override fun getItemCount(): Int = items.size
    }
}