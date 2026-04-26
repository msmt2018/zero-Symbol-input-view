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
import android.view.ViewConfiguration
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

    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val pagerAdapter = SymbolPagerAdapter()

    private val rowHeightPx by lazy { (36 * resources.displayMetrics.density).roundToInt() }
    private val collapsedHeightPx by lazy { rowHeightPx * 2 + (20 * resources.displayMetrics.density).roundToInt() }
    private val expandedHeightPx by lazy { (220 * resources.displayMetrics.density).roundToInt() }
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private var initialY = 0f
    private var initialX = 0f
    private var lastY = 0f
    private var isDragging = false

    // 为兼容 MainActivity 旧代码提供空实现
    var followSystemIme: Boolean = false

    init {
        orientation = VERTICAL
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        updatePagerHeight(collapsedHeightPx)
        applyTabRowByFraction(0f)
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
        animateToHeight(collapsedHeightPx)
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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.rawY
                lastY = ev.rawY
                initialX = ev.rawX
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.rawY - initialY
                val deltaX = ev.rawX - initialX
                if (!isDragging && kotlin.math.abs(deltaY) > touchSlop && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = event.rawY
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return super.onTouchEvent(event)
                val deltaY = event.rawY - lastY
                val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                val nextHeight = (currentHeight - deltaY.toInt()).coerceIn(collapsedHeightPx, expandedHeightPx)
                updatePagerHeight(nextHeight)
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                    val midpoint = (collapsedHeightPx + expandedHeightPx) / 2
                    val targetHeight = if (currentHeight >= midpoint) expandedHeightPx else collapsedHeightPx
                    animateToHeight(targetHeight)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateToHeight(targetHeight: Int) {
        val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
        val animator = ValueAnimator.ofInt(currentHeight, targetHeight)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            updatePagerHeight(animation.animatedValue as Int)
        }
        animator.start()
    }

    private fun updatePagerHeight(height: Int) {
        val params = viewPager.layoutParams
        val clamped = height.coerceIn(collapsedHeightPx, expandedHeightPx)
        params.height = clamped
        viewPager.layoutParams = params
        val fraction = (clamped - collapsedHeightPx).toFloat() / (expandedHeightPx - collapsedHeightPx).toFloat()
        applyTabRowByFraction(fraction)
    }

    private fun applyTabRowByFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        tabRow.alpha = 0.55f + (0.45f * clamped)
        tabRow.translationY = (1f - clamped) * -6f * resources.displayMetrics.density
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
