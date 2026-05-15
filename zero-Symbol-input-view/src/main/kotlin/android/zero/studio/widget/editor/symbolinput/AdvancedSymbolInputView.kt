package android.zero.studio.widget.editor.symbolinput

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.roundToInt

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val indicatorScrollView: HorizontalScrollView
    private val indicatorContainer: LinearLayout
    private val pagerHost: SymbolPagerHostView

    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val indicatorItems = mutableListOf<AppCompatTextView>()
    private val pagerAdapter = SymbolPagerAdapter()
    private var uiSettings = SymbolUiSettings()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (SymbolDataManager.shouldTriggerUiRefresh(key)) refreshData()
    }

    private val rowHeightPx by lazy { (36 * resources.displayMetrics.density).roundToInt() }
    private val itemHeightPx by lazy { (44 * resources.displayMetrics.density).roundToInt() }
    private val collapsedExtraPaddingPx by lazy { (20 * resources.displayMetrics.density).roundToInt() }
    private val gridTopPaddingPx by lazy { (2 * resources.displayMetrics.density).roundToInt() }
    private val gridBottomPaddingPx by lazy { (8 * resources.displayMetrics.density).roundToInt() }
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private var collapsedHeightPx = rowHeightPx * 2 + collapsedExtraPaddingPx
    private var expandedHeightPx = (220 * resources.displayMetrics.density).roundToInt()

    private var initialY = 0f
    private var initialX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var heightAnimator: ValueAnimator? = null
    private var lastSavedPageIndex = -1

    private val expandedHeightCache = mutableMapOf<ExpandedHeightKey, Int>()

    private data class ExpandedHeightKey(val pageIndex: Int, val itemCount: Int, val symbolsPerRow: Int)

    var followSystemIme: Boolean = false

    init {
        orientation = VERTICAL

        indicatorScrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * resources.displayMetrics.density).roundToInt())
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            setPadding((8 * resources.displayMetrics.density).roundToInt(), 0, (8 * resources.displayMetrics.density).roundToInt(), 0)
        }

        indicatorContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        indicatorScrollView.addView(indicatorContainer)

        pagerHost = SymbolPagerHostView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
            setAdapter(pagerAdapter)
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    updateIndicatorSelection(position)
                    ensureIndicatorVisible(position)
                    if (uiSettings.rememberLastPage && lastSavedPageIndex != position) {
                        lastSavedPageIndex = position
                        SymbolDataManager.setLastPageIndex(context, position)
                    }
                    recalculateHeights()
                }
            })
        }

        addView(indicatorScrollView)
        addView(pagerHost)
        updatePagerHeight(collapsedHeightPx)
        applyIndicatorRowByFraction(0f)
        refreshData()
    }

    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) = Unit

    fun onHostResume() {
        val shouldExpand = uiSettings.rememberExpanded && SymbolDataManager.getLastExpanded(context)
        animateToHeight(if (shouldExpand) expandedHeightPx else collapsedHeightPx)
    }

    fun bindEditor(editor: CodeEditor) { this.editor = editor }

    fun refreshData() {
        uiSettings = SymbolDataManager.getUiSettings(context)
        groups.clear()
        groups.addAll(SymbolDataManager.loadData(context).filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            val defaults = SymbolDefaults.createFallbackGroups()
            groups.addAll(defaults)
            SymbolDataManager.saveData(context, defaults)
        }
        expandedHeightCache.clear()
        rebuildIndicators()
        pagerAdapter.notifyDataSetChanged()
        val target = if (uiSettings.rememberLastPage) SymbolDataManager.getLastPageIndex(context).coerceIn(0, groups.lastIndex) else 0
        pagerHost.currentItem = target
        lastSavedPageIndex = target
        updateIndicatorSelection(target)
        ensureIndicatorVisible(target)
        recalculateHeights()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDetachedFromWindow() {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDetachedFromWindow()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { initialY = ev.rawY; lastY = ev.rawY; initialX = ev.rawX; isDragging = false }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - initialY; val dx = ev.rawX - initialX
                if (!isDragging && kotlin.math.abs(dy) > touchSlop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    isDragging = true; parent?.requestDisallowInterceptTouchEvent(true); return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { heightAnimator?.cancel(); initialY = event.rawY; lastY = event.rawY; return true }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return super.onTouchEvent(event)
                val deltaY = event.rawY - lastY
                val currentHeight = pagerHost.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                updatePagerHeight((currentHeight - deltaY.toInt()).coerceIn(collapsedHeightPx, expandedHeightPx))
                lastY = event.rawY; return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val currentHeight = pagerHost.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                    val target = if (currentHeight >= (collapsedHeightPx + expandedHeightPx) / 2) expandedHeightPx else collapsedHeightPx
                    if (uiSettings.rememberExpanded) SymbolDataManager.setLastExpanded(context, target == expandedHeightPx)
                    animateToHeight(target)
                }
                isDragging = false; return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateToHeight(targetHeight: Int) {
        val current = pagerHost.layoutParams.height.coerceAtLeast(collapsedHeightPx)
        if (current == targetHeight) return
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofInt(current, targetHeight).apply {
            duration = 200
            addUpdateListener { updatePagerHeight(it.animatedValue as Int) }
            start()
        }
    }

    private fun updatePagerHeight(height: Int) {
        val clamped = height.coerceIn(collapsedHeightPx, expandedHeightPx)
        pagerHost.layoutParams = pagerHost.layoutParams.apply { this.height = clamped }
        val range = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(1)
        applyIndicatorRowByFraction((clamped - collapsedHeightPx).toFloat() / range)
    }

    private fun applyIndicatorRowByFraction(fraction: Float) {
        val p = ((fraction.coerceIn(0f, 1f) - 0.08f) / (0.55f - 0.08f)).coerceIn(0f, 1f)
        indicatorScrollView.layoutParams = indicatorScrollView.layoutParams.apply {
            height = (((44 * resources.displayMetrics.density).roundToInt()) * p).roundToInt()
        }
        indicatorScrollView.alpha = p
        indicatorScrollView.visibility = if (p == 0f) View.INVISIBLE else View.VISIBLE
    }

    private fun recalculateHeights() {
        collapsedHeightPx = rowHeightPx * uiSettings.collapsedRows.coerceAtLeast(1) + collapsedExtraPaddingPx
        val minExpanded = collapsedHeightPx + rowHeightPx
        expandedHeightPx = calculateExpandedHeightForPage(pagerHost.currentItem).coerceAtLeast(minExpanded)
        updatePagerHeight(pagerHost.layoutParams.height.coerceIn(collapsedHeightPx, expandedHeightPx))
    }

    private fun calculateExpandedHeightForPage(pageIndex: Int): Int {
        val cols = uiSettings.symbolsPerRow.coerceIn(1, 20)
        val group = groups.getOrNull(pageIndex) ?: return collapsedHeightPx + rowHeightPx
        val key = ExpandedHeightKey(pageIndex, group.items.size, cols)
        return expandedHeightCache.getOrPut(key) {
            val rows = (group.items.size + cols - 1) / cols
            (rows.coerceAtLeast(2) * itemHeightPx) + gridTopPaddingPx + gridBottomPaddingPx
        }
    }

    private fun rebuildIndicators() {
        indicatorContainer.removeAllViews()
        indicatorItems.clear()
        groups.forEachIndexed { index, group ->
            val item = AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                    marginStart = (4 * resources.displayMetrics.density).roundToInt()
                    marginEnd = (4 * resources.displayMetrics.density).roundToInt()
                }
                gravity = Gravity.CENTER
                minWidth = (44 * resources.displayMetrics.density).roundToInt()
                setPadding((12 * resources.displayMetrics.density).roundToInt(), 0, (12 * resources.displayMetrics.density).roundToInt(), 0)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                text = if (uiSettings.indicatorStyle == 1) "•" else group.name
                setOnClickListener { pagerHost.currentItem = index }
            }
            indicatorItems.add(item)
            indicatorContainer.addView(item)
        }
    }

    private fun updateIndicatorSelection(selected: Int) {
        indicatorItems.forEachIndexed { index, tv ->
            tv.isSelected = index == selected
            tv.alpha = if (index == selected) 1f else 0.65f
            tv.textSize = if (index == selected) 14f else 13f
        }
    }

    private fun ensureIndicatorVisible(position: Int) {
        val item = indicatorItems.getOrNull(position) ?: return
        indicatorScrollView.post {
            val left = item.left - (indicatorScrollView.width - item.width) / 2
            indicatorScrollView.smoothScrollTo(left.coerceAtLeast(0), 0)
        }
    }

    private inner class SymbolPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = groups.size
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`
        override fun getItemPosition(`object`: Any): Int = POSITION_NONE

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val group = groups[position]
            val scrollView = NestedScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                isFillViewport = false
                overScrollMode = OVER_SCROLL_NEVER
            }
            val pageContainer = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            }
            val grid = GridLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                columnCount = uiSettings.symbolsPerRow.coerceIn(1, 20)
                val horizontalPadding = (6 * resources.displayMetrics.density).roundToInt()
                setPadding(horizontalPadding, gridTopPaddingPx, horizontalPadding, gridBottomPaddingPx)
            }
            group.items.forEach { item ->
                grid.addView(AppCompatTextView(context).apply {
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
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 10, uiSettings.symbolTextSizeSp.coerceAtLeast(12), 1, TypedValue.COMPLEX_UNIT_SP)
                    setOnClickListener { editor?.let { ed -> SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener) } }
                    setOnLongClickListener {
                        if (item.longAction != null) {
                            editor?.let { ed -> SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener) }; true
                        } else false
                    }
                })
            }
            pageContainer.addView(grid)
            scrollView.addView(pageContainer)
            container.addView(scrollView)
            return scrollView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }
}

private class SymbolPagerHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val pager = ViewPager(context)

    init {
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setAdapter(adapter: PagerAdapter) { pager.adapter = adapter }
    fun addOnPageChangeListener(listener: ViewPager.OnPageChangeListener) = pager.addOnPageChangeListener(listener)
    var currentItem: Int
        get() = pager.currentItem
        set(value) { pager.currentItem = value }
}
