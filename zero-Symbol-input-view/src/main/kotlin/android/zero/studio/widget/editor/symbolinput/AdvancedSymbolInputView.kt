package android.zero.studio.widget.editor.symbolinput

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.roundToInt

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager
    private val tabLayout: TabLayout
    private val tabRow: View
    private val dragIndicator: View

    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val pagerAdapter = SymbolPagerAdapter()

    private val expandedHeightPx by lazy { (220 * resources.displayMetrics.density).roundToInt() }
    private var isExpanded = false

    // 手势状态记录
    private var initialY = 0f
    private var isDragging = false

    // 为兼容 MainActivity 旧代码提供空实现
    var followSystemIme: Boolean = false

    init {
        orientation = VERTICAL
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)
        dragIndicator = root.findViewById(R.id.drag_indicator)

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        
        setupDragBehavior()
        refreshData()
    }

    /**
     * 为兼容而保留的方法名，内部实际已不需要外部的 BottomSheet 支持
     */
    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        // Do nothing. 我们现在依靠自己的手势和 RelativeLayout 机制。
    }

    fun onHostResume() {
        // 恢复时自动折叠
        animateToHeight(0)
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    fun refreshData() {
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            groups.addAll(buildFallbackGroups())
        }
        pagerAdapter.notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragBehavior() {
        // 点击整个 Header 也可切换展开/收起
        tabRow.setOnClickListener {
            toggleExpansion()
        }

        tabRow.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    if (Math.abs(deltaY) > 10) {
                        isDragging = true
                        val currentHeight = viewPager.layoutParams.height
                        var newHeight = currentHeight - deltaY.toInt()
                        newHeight = newHeight.coerceIn(0, expandedHeightPx)
                        updatePagerHeight(newHeight)
                        initialY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        toggleExpansion()
                    } else {
                        // 拖拽结束，判断是吸附到顶部还是底部
                        val currentHeight = viewPager.layoutParams.height
                        if (currentHeight > expandedHeightPx / 2) {
                            animateToHeight(expandedHeightPx)
                        } else {
                            animateToHeight(0)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpansion() {
        if (isExpanded) {
            animateToHeight(0)
        } else {
            animateToHeight(expandedHeightPx)
        }
    }

    private fun animateToHeight(targetHeight: Int) {
        val currentHeight = viewPager.layoutParams.height
        val animator = ValueAnimator.ofInt(currentHeight, targetHeight)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            updatePagerHeight(animation.animatedValue as Int)
        }
        animator.start()
        isExpanded = targetHeight > 0
    }

    private fun updatePagerHeight(height: Int) {
        val params = viewPager.layoutParams
        params.height = height
        viewPager.layoutParams = params
    }

    private fun buildFallbackGroups(): List<SymbolGroup> {
        return listOf(
            SymbolGroup("default", mutableListOf(
                SymbolItem(0, "注释", "//"), SymbolItem(18, "←"), SymbolItem(20, "↑"),
                SymbolItem(19, "→"), SymbolItem(0, "\"", "\""), SymbolItem(0, "'", "'"),
                SymbolItem(0, ".", "."), SymbolItem(0, ",", ","), SymbolItem(0, "/", "/"),
                SymbolItem(21, "↓"), SymbolItem(0, ":", ":"), SymbolItem(0, ";", ";"),
                SymbolItem(0, "+", "+"), SymbolItem(0, "-", "-"), SymbolItem(22, "...")
            ))
        )
    }

    private inner class SymbolPagerAdapter : PagerAdapter() {

        override fun getCount(): Int = groups.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence {
            return groups[position].name
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val group = groups[position]

            val scrollView = NestedScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFillViewport = true
                overScrollMode = OVER_SCROLL_NEVER
            }

            val gridLayout = GridLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                columnCount = 8
                val padding = (6 * resources.displayMetrics.density).roundToInt()
                setPadding(padding, padding, padding, padding)
            }

            for (item in group.items) {
                val tv = TextView(context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    minHeight = (36 * resources.displayMetrics.density).roundToInt()
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    text = item.display
                    isClickable = true
                    isFocusable = true

                    val tvColor = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                    setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

                    val tvBg = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                    setBackgroundResource(tvBg.resourceId)

                    setOnClickListener {
                        editor?.let { ed ->
                            SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener)
                        }
                    }

                    setOnLongClickListener {
                        if (item.longAction != null) {
                            editor?.let { ed ->
                                SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener)
                            }
                            true
                        } else false
                    }
                }
                gridLayout.addView(tv)
            }

            scrollView.addView(gridLayout)
            container.addView(scrollView)
            return scrollView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
        
        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE // 强制刷新
        }
    }
}