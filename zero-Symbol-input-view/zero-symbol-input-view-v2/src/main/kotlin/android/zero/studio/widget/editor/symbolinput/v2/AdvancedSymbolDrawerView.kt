package android.zero.studio.widget.editor.symbolinput.v2

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import android.zero.studio.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import android.zero.studio.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import android.zero.studio.widget.editor.symbolinput.v2.model.DrawerConfig
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
import android.zero.studio.widget.editor.symbolinput.v2.state.DrawerGeometry
import android.zero.studio.widget.editor.symbolinput.v2.ui.SymbolGroupIndicator
import android.zero.studio.widget.editor.symbolinput.v2.ui.SymbolPageAdapter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A self-contained symbol drawer: vertical drag controls its height while
 * ViewPager2 owns horizontal group paging. The two axes are arbitrated before
 * the parent intercepts, so horizontal page swipes are not stolen by the drawer.
 *
 * Symbol actions are exposed as callbacks; this view has no dependency on a
 * particular editor implementation.
 */
class AdvancedSymbolDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val config: DrawerConfig = DrawerConfig()
) : ViewGroup(context, attrs, defStyleAttr) {

    fun interface OnSymbolActionListener {
        fun onAction(symbol: SymbolEntry, isLongPress: Boolean)
    }

    var onSymbolActionListener: OnSymbolActionListener? = null
    var onOpenManagerListener: (() -> Unit)? = null
    var onGroupChangedListener: ((Int, SymbolGroup?) -> Unit)? = null

    private val indicator = SymbolGroupIndicator(context) { selectGroup(it, true) }
    private val pager = ViewPager2(context).apply {
        orientation = ViewPager2.ORIENTATION_HORIZONTAL
        offscreenPageLimit = 1
        isUserInputEnabled = true
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private val pageAdapter = SymbolPageAdapter(config) { symbol, longPress ->
        val action = if (longPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            if (action.type == MANAGER_ACTION_TYPE) onOpenManagerListener?.invoke()
            else onSymbolActionListener?.onAction(symbol, longPress)
        }
    }

    private var repository: SymbolGroupRepository =
        SharedPreferencesSymbolGroupRepository(context.applicationContext)
    private var groups: List<SymbolGroup> = emptyList()
    private var collapsedHeight = DrawerGeometry.collapsedHeightPx(config, resources.displayMetrics.density)
    private var expandedGridHeight = collapsedHeight
    private var expandedHeight = collapsedHeight
    private var drawerHeight = collapsedHeight
    private var pagerHeight = collapsedHeight
    private var expansion = 0f
    private var heightAnimator: ValueAnimator? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var draggingDrawer = false
    private var velocityTracker: VelocityTracker? = null

    init {
        clipChildren = false
        clipToPadding = false
        addView(indicator, LayoutParams(LayoutParams.MATCH_PARENT, 0))
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, collapsedHeight))
        pager.adapter = pageAdapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                indicator.setSelectedIndex(position)
                onGroupChangedListener?.invoke(position, groups.getOrNull(position))
            }
        })
        reloadGroups()
        updateVisualState(0f)
    }

    fun setRepository(value: SymbolGroupRepository, reload: Boolean = true) {
        repository = value
        if (reload) reloadGroups()
    }

    fun submitGroups(value: List<SymbolGroup>, preserveSelectedGroup: Boolean = true) {
        val previousId = groups.getOrNull(pager.currentItem)?.id
        groups = value.filter { it.symbols.isNotEmpty() }.map { it.copy(symbols = it.symbols.toList()) }
        val restored = if (preserveSelectedGroup && previousId != null) {
            groups.indexOfFirst { it.id == previousId }.takeIf { it >= 0 }
        } else null
        val target = restored ?: pager.currentItem.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
        indicator.submitGroups(groups)
        pageAdapter.submitList(groups) {
            if (groups.isNotEmpty() && target in groups.indices) {
                pager.setCurrentItem(target, false)
            }
        }
        recalculateGeometry()
    }

    fun reloadGroups() {
        submitGroups(repository.loadGroups())
    }

    fun saveGroups() {
        repository.saveGroups(groups)
    }

    fun getGroups(): List<SymbolGroup> = groups.toList()
    fun getCurrentGroupIndex(): Int = pager.currentItem
    fun isExpanded(): Boolean = expansion >= 0.5f

    fun selectGroup(index: Int, smoothScroll: Boolean = true) {
        if (index !in groups.indices) return
        pager.setCurrentItem(index, smoothScroll)
    }

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        animateTo(if (expanded) 1f else 0f, animate)
    }

    fun onHostResume() {
        // Kept explicit for hosts that want to restore state after returning from a manager.
        reloadGroups()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val indicatorHeight = indicator.layoutParams.height.coerceAtLeast(0)
        indicator.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(indicatorHeight, MeasureSpec.EXACTLY)
        )
        pager.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(pagerHeight, MeasureSpec.EXACTLY)
        )
        val desiredHeight = indicator.measuredHeight + pager.measuredHeight
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, resolvedHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val indicatorHeight = indicator.measuredHeight
        indicator.layout(0, 0, width, indicatorHeight)
        pager.layout(0, indicatorHeight, width, indicatorHeight + pager.measuredHeight)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.rawY
                lastY = event.rawY
                draggingDrawer = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dx = event.x - downX
                val dy = event.rawY - downY
                if (!draggingDrawer && abs(dy) > touchSlop && abs(dy) > abs(dx) * 1.15f &&
                    expandedHeight > collapsedHeight
                ) {
                    draggingDrawer = true
                    heightAnimator?.cancel()
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> resetGesture()
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocityTracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!draggingDrawer) return false
                val delta = lastY - event.rawY
                lastY = event.rawY
                setExpansionFromHeight((drawerHeight + delta.roundToInt()).coerceIn(collapsedHeight, expandedHeight))
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (draggingDrawer) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityY = -(velocityTracker?.yVelocity ?: 0f)
                    val target = when {
                        abs(velocityY) >= minimumFlingVelocity &&
                            abs(velocityY) >= config.flingVelocityDpPerSecond * resources.displayMetrics.density ->
                            velocityY > 0f
                        else -> expansion >= 0.5f
                    }
                    animateTo(if (target) 1f else 0f, true)
                }
                resetGesture()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (draggingDrawer) animateTo(if (expansion >= 0.5f) 1f else 0f, true)
                resetGesture()
                return true
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        heightAnimator?.cancel()
        heightAnimator = null
        velocityTracker?.recycle()
        velocityTracker = null
        super.onDetachedFromWindow()
    }

    private fun resetGesture() {
        draggingDrawer = false
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun recalculateGeometry() {
        val density = resources.displayMetrics.density
        val oldFraction = expansion
        collapsedHeight = DrawerGeometry.collapsedHeightPx(config, density)
        expandedGridHeight = DrawerGeometry.expandedHeightPx(groups, config, density)
        val indicatorHeight = (config.indicatorHeightDp * density).roundToInt()
        // The indicator must still be revealable when every group fits in the
        // collapsed grid; its height is part of the expanded drawer geometry.
        expandedHeight = maxOf(collapsedHeight, expandedGridHeight) + indicatorHeight
        setExpansionFromHeight(
            (collapsedHeight + (expandedHeight - collapsedHeight) * oldFraction).roundToInt()
        )
    }

    private fun setExpansionFromHeight(height: Int) {
        drawerHeight = height.coerceIn(collapsedHeight, expandedHeight)
        expansion = (drawerHeight - collapsedHeight).toFloat() /
            (expandedHeight - collapsedHeight).coerceAtLeast(1)
        pagerHeight = (
            collapsedHeight + (expandedGridHeight - collapsedHeight) * expansion
        ).roundToInt().coerceAtLeast(collapsedHeight)
        updateVisualState(expansion)
        requestLayout()
    }

    private fun updateVisualState(fraction: Float) {
        val reveal = DrawerGeometry.indicatorReveal(fraction, config)
        val indicatorHeight = (config.indicatorHeightDp * resources.displayMetrics.density * reveal).roundToInt()
        val params = indicator.layoutParams
        if (params.height != indicatorHeight) {
            params.height = indicatorHeight
            indicator.layoutParams = params
        }
        indicator.alpha = reveal
        indicator.translationY = -((1f - reveal) * config.indicatorHeightDp *
            resources.displayMetrics.density * 0.2f)
        indicator.visibility = if (indicatorHeight == 0) View.INVISIBLE else View.VISIBLE
    }

    private companion object {
        const val MANAGER_ACTION_TYPE = 22
    }

    private fun animateTo(target: Float, animated: Boolean) {
        val destination = target.coerceIn(0f, 1f)
        heightAnimator?.cancel()
        if (!animated || !isAttachedToWindow) {
            setExpansionFromHeight(
                (collapsedHeight + (expandedHeight - collapsedHeight) * destination).roundToInt()
            )
            return
        }
        val start = expansion
        heightAnimator = ValueAnimator.ofFloat(start, destination).apply {
            duration = config.animationDurationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                setExpansionFromHeight(
                    (collapsedHeight + (expandedHeight - collapsedHeight) * fraction).roundToInt()
                )
            }
            start()
        }
    }
}
