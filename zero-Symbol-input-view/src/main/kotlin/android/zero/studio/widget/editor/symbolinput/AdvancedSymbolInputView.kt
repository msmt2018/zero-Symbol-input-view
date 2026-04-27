package android.zero.studio.widget.editor.symbolinput

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
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
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
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
    private var uiSettings = SymbolUiSettings()
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("symbol_") == true) {
            refreshData()
        }
    }

    private val rowHeightPx by lazy { (36 * resources.displayMetrics.density).roundToInt() }
    private var collapsedHeightPx = rowHeightPx * 2 + (20 * resources.displayMetrics.density).roundToInt()
    private var expandedHeightPx = (220 * resources.displayMetrics.density).roundToInt()
    private var panelHeightPx = collapsedHeightPx
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
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (uiSettings.rememberLastPage) {
                    SymbolDataManager.setLastPageIndex(context, position)
                }
                if (!uiSettings.uniformGroupHeight) {
                    recalculateHeights(animate = false)
                }
            }
        })

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
        val shouldExpand = uiSettings.rememberExpanded && SymbolDataManager.getLastExpanded(context)
        animateToHeight(if (shouldExpand) expandedHeightPx else collapsedHeightPx)
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    fun refreshData() {
        uiSettings = SymbolDataManager.getUiSettings(context)
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            groups.addAll(buildFallbackGroups())
        }
        applyIndicatorStyle()
        recalculateHeights()
        pagerAdapter.notifyDataSetChanged()
        if (groups.isNotEmpty()) {
            viewPager.offscreenPageLimit = groups.size.coerceIn(1, 4)
            val target = if (uiSettings.rememberLastPage) {
                SymbolDataManager.getLastPageIndex(context).coerceIn(0, groups.lastIndex)
            } else {
                0
            }
            viewPager.currentItem = target
        }
        tabLayout.post { applyIndicatorStyle() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        applyIndicatorStyle()
    }

    override fun onDetachedFromWindow() {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDetachedFromWindow()
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
                val currentHeight = panelHeightPx.coerceAtLeast(collapsedHeightPx)
                val nextHeight = (currentHeight - deltaY.toInt()).coerceIn(collapsedHeightPx, expandedHeightPx)
                updatePagerHeight(nextHeight)
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val currentHeight = panelHeightPx.coerceAtLeast(collapsedHeightPx)
                    val midpoint = (collapsedHeightPx + expandedHeightPx) / 2
                    val targetHeight = if (currentHeight >= midpoint) expandedHeightPx else collapsedHeightPx
                    if (uiSettings.rememberExpanded) {
                        SymbolDataManager.setLastExpanded(context, targetHeight == expandedHeightPx)
                    }
                    animateToHeight(targetHeight)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateToHeight(targetHeight: Int) {
        val currentHeight = panelHeightPx.coerceAtLeast(collapsedHeightPx)
        val animator = ValueAnimator.ofInt(currentHeight, targetHeight)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            updatePagerHeight(animation.animatedValue as Int)
        }
        animator.start()
    }

    private fun updatePagerHeight(height: Int) {
        val clamped = height.coerceIn(collapsedHeightPx, expandedHeightPx)
        val params = viewPager.layoutParams
        if (params.height != clamped) {
            params.height = clamped
            viewPager.layoutParams = params
        }
        panelHeightPx = clamped
        val range = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(1)
        val fraction = (clamped - collapsedHeightPx).toFloat() / range.toFloat()
        applyTabRowByFraction(fraction)
    }

    private fun applyTabRowByFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        tabRow.alpha = clamped
        tabRow.translationY = (1f - clamped) * -6f * resources.displayMetrics.density
        tabRow.visibility = if (clamped <= 0.01f) View.INVISIBLE else View.VISIBLE
    }

    private fun buildFallbackGroups(): List<SymbolGroup> = buildFallbackSymbolGroups()



    private fun recalculateHeights(animate: Boolean = false) {
        collapsedHeightPx = rowHeightPx * uiSettings.collapsedRows.coerceAtLeast(1) + (20 * resources.displayMetrics.density).roundToInt()
        val baseExpanded = (220 * resources.displayMetrics.density).roundToInt()
        val current = groups.getOrNull(viewPager.currentItem)
        expandedHeightPx = (current?.let(::calculateExpandedHeightForGroup) ?: baseExpanded).coerceAtLeast(baseExpanded)
        val currentHeight = panelHeightPx
        val targetHeight = currentHeight.coerceIn(collapsedHeightPx, expandedHeightPx)
        val shouldAnimate = animate && currentHeight > collapsedHeightPx && targetHeight != currentHeight
        if (shouldAnimate) {
            animateToHeight(targetHeight)
        } else {
            updatePagerHeight(targetHeight)
        }
        val currentHeight = panelHeightPx
        val targetHeight = currentHeight.coerceIn(collapsedHeightPx, expandedHeightPx)
        val shouldAnimate = animate && currentHeight > collapsedHeightPx && targetHeight != currentHeight
        if (shouldAnimate) {
            animateToHeight(targetHeight)
        } else {
            updatePagerHeight(targetHeight)
        }
        val panelHeightNow = panelHeightPx
        val clampedTargetHeight = panelHeightNow.coerceIn(collapsedHeightPx, expandedHeightPx)
        val animateResize = animate && panelHeightNow > collapsedHeightPx && clampedTargetHeight != panelHeightNow
        if (animateResize) {
            animateToHeight(clampedTargetHeight)
        } else {
            updatePagerHeight(clampedTargetHeight)
        }
    }

    private fun calculateExpandedHeightForGroup(group: SymbolGroup): Int {
        val cols = uiSettings.symbolsPerRow.coerceIn(1, 20)
        val rows = ((group.items.size + cols - 1) / cols).coerceAtLeast(1)
        val itemHeight = (44 * resources.displayMetrics.density).roundToInt()
        val verticalPadding = (12 * resources.displayMetrics.density).roundToInt()
        return rows * itemHeight + verticalPadding
    }

    private fun applyIndicatorStyle() {
        tabLayout.setSelectedTabIndicatorColor(fetchColor(android.R.attr.colorAccent))
        tabLayout.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).roundToInt())
        tabLayout.setSelectedTabIndicator(ColorDrawable(fetchColor(android.R.attr.colorAccent)))
        tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_BOTTOM)
        tabLayout.isInlineLabel = false

        when (uiSettings.indicatorStyle) {
            0 -> {
                // 标准
                tabLayout.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).roundToInt())
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_BOTTOM)
            }
            1 -> {
                // 简洁胶囊
                tabLayout.setSelectedTabIndicator(
                    ContextCompat.getDrawable(context, R.drawable.bg_indicator_capsule)
                )
                tabLayout.setSelectedTabIndicatorHeight((6 * resources.displayMetrics.density).roundToInt())
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_BOTTOM)
            }
            2 -> {
                // 隐藏
                tabLayout.setSelectedTabIndicatorHeight(0)
                tabLayout.setSelectedTabIndicator(ColorDrawable(0))
            }
            3 -> {
                // 顶部线条
                tabLayout.setSelectedTabIndicatorHeight((3 * resources.displayMetrics.density).roundToInt())
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_TOP)
            }
            4 -> {
                // 块状
                tabLayout.setSelectedTabIndicator(
                    ContextCompat.getDrawable(context, R.drawable.bg_indicator_block)
                )
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_STRETCH)
            }
        }
    }

    private fun fetchColor(attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
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
                columnCount = group.items.size.coerceAtMost(uiSettings.symbolsPerRow.coerceIn(1, 20)).coerceAtLeast(1)
                val padding = (6 * resources.displayMetrics.density).roundToInt()
                setPadding(padding, padding, padding, padding)
            }

            for (item in group.items) {
                val tv = AppCompatTextView(context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    minHeight = (36 * resources.displayMetrics.density).roundToInt()
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, uiSettings.symbolTextSizeSp.toFloat())
                    text = item.display
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    isClickable = true
                    isFocusable = true
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this,
                        10,
                        uiSettings.symbolTextSizeSp.coerceAtLeast(12),
                        1,
                        TypedValue.COMPLEX_UNIT_SP
                    )

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
