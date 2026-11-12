package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.config.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchor
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorInput
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorState
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerAnchorStrategy
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerController
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerState
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerNavigationPlacement
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelSlot
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerDimensions
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerMotion
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerShape
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerStyle
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerThemeResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.style.DrawerTypography
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Reusable bottom drawer container. The host content, Peek panel and Expanded panel
 * are independent; page navigation is optional and remains feature-neutral.
 */
class DrawerContainer @JvmOverloads constructor(
    context: Context,
    config: DrawerConfig = DrawerConfig.builder().build()
) : FrameLayout(context), NestedScrollingParent3 {

    private val scrim = View(context)
    private val sheet = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val handle = View(context)
    private val title = TextView(context)
    private val tabs = TabLayout(context)
    private val peekHost = DrawerPanelHost(context, DrawerPanelSlot.PEEK)
    private val expandedHost = DrawerPanelHost(context, DrawerPanelSlot.EXPANDED)
    private val pageHost = DrawerPageHost(context)
    private var currentConfig = config
    private var anchors: List<DrawerAnchor> = emptyList()
    private var state: DrawerState? = null
    private var animator: ValueAnimator? = null
    private var velocityTracker: VelocityTracker? = null
    private var downY = 0f
    private var dragStartHeight = 0
    private var dragging = false
    private var peekContentVisible = false
    private var expandedContentVisible = false
    private var containerAttached = false
    private var hostContent: View? = null
    private val stateListeners = LinkedHashSet<(DrawerState) -> Unit>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val nestedScrollHelper = NestedScrollingParentHelper(this)
    private val controllerImpl = Controller()

    val controller: DrawerController get() = controllerImpl
    val navigationHost: DrawerPageHost get() = pageHost

    init {
        clipChildren = false
        clipToPadding = false
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        scrim.setOnClickListener {
            if (currentConfig.dismissOnScrimTap) controller.collapse()
        }

        addView(sheet, LayoutParams(LayoutParams.MATCH_PARENT, dp(640), Gravity.BOTTOM))
        sheet.addView(handle, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(10)
            bottomMargin = dp(8)
        })
        sheet.addView(title, LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = dp(20); rightMargin = dp(20); bottomMargin = dp(8) })
        sheet.addView(tabs, LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ))
        sheet.addView(peekHost, LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, 0, 1f
        ))
        sheet.addView(expandedHost, LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, 0, 1f
        ))
        expandedHost.addView(pageHost, FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        ))
        handle.setOnTouchListener(::onHandleTouch)
        sheet.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                downY = event.rawY
                true
            } else false
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                (tab.tag as? String)?.let(pageHost::selectPage)
                applyNavigationTypography()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        controllerImpl.applyConfig(config)
        post { recalculateAnchors() }
    }

    fun setMainContent(view: View?) {
        hostContent?.let { if (it.parent === this) removeView(it) }
        hostContent = view
        if (view != null) {
            (view.parent as? ViewGroup)?.removeView(view)
            addView(view, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        bringChildToFront(scrim)
        bringChildToFront(sheet)
    }

    fun setTitle(value: CharSequence?) {
        title.text = value ?: ""
        title.visibility = if (title.text.isNullOrEmpty()) GONE else VISIBLE
    }

    private fun contentThemeContext() = DrawerThemeResolver.contextFor(context, currentConfig.style)

    fun setPeekContent(content: DrawerPanelContent?) {
        val changed = peekHost.content() !== content
        peekHost.setContent(content, contentThemeContext())
        peekHost.visibility = if (content == null) GONE else VISIBLE
        if (changed) refreshPanelVisibilityCallbacks()
        recalculateAnchors()
    }

    fun setExpandedContent(content: DrawerPanelContent?) {
        pageHost.visibility = if (content == null && pageHost.navigationController.pages().isNotEmpty()) VISIBLE else GONE
        val changed = expandedHost.content() !== content
        expandedHost.setContent(content, contentThemeContext())
        if (changed) refreshPanelVisibilityCallbacks()
        recalculateAnchors()
    }

    fun setPages(pages: List<DrawerPage>, preserveSelection: Boolean = true) {
        expandedHost.content()?.let { expandedHost.setContent(null) }
        pageHost.visibility = VISIBLE
        pageHost.setPages(pages, preserveSelection)
        rebuildTabs()
        recalculateAnchors()
    }

    fun expand(animated: Boolean = true) = controller.expand(animated)
    fun collapse(animated: Boolean = true) = controller.collapse(animated)

    fun handleBack(): Boolean {
        if (!currentConfig.dismissOnBack) return false
        val current = state ?: return false
        if (current.currentHeightPx <= (anchors.firstOrNull()?.heightPx ?: 0)) return false
        collapse()
        return true
    }

    private fun rebuildTabs() {
        tabs.removeAllTabs()
        val navigation = currentConfig.navigationConfig
        pageHost.navigationController.pages().forEach { page ->
            val label = TextView(context).apply {
                text = if (navigation.showTitles) page.title else ""
                gravity = Gravity.CENTER
                textSize = navigation.titleTextSizeSp
                minWidth = dp(navigation.itemMinWidthDp)
                setPadding(
                    dp(navigation.itemHorizontalPaddingDp),
                    dp(navigation.itemVerticalPaddingDp),
                    dp(navigation.itemHorizontalPaddingDp),
                    dp(navigation.itemVerticalPaddingDp)
                )
                if (navigation.showIcons && page.icon != null) {
                    setCompoundDrawablesWithIntrinsicBounds(page.icon, null, null, null)
                    compoundDrawablePadding = dp(6)
                }
                contentDescription = page.title
            }
            val tab = tabs.newTab().setCustomView(label).setTag(page.id)
            tabs.addTab(tab, false)
        }
        val selected = pageHost.navigationController.selectedIndex()
        if (selected >= 0 && selected < tabs.tabCount) tabs.getTabAt(selected)?.select()
        updateNavigationVisibility()
        applyNavigationTypography()
    }

    private fun applyNavigationTypography() {
        val navigation = currentConfig.navigationConfig
        for (index in 0 until tabs.tabCount) {
            val tab = tabs.getTabAt(index) ?: continue
            val label = tab.customView as? TextView ?: continue
            val selected = tab.isSelected
            label.textSize = if (selected) navigation.selectedTitleTextSizeSp else navigation.titleTextSizeSp
            label.setTypeface(
                label.typeface,
                if (selected && navigation.selectedTitleBold) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL
            )
            label.setTextColor(
                if (selected) resolvedColors().accent ?: Color.BLUE
                else resolvedColors().secondaryText ?: Color.GRAY
            )
        }
    }

    private fun updateNavigationVisibility() {
        val nav = currentConfig.navigationConfig
        if (tabs.parent === sheet) sheet.removeView(tabs)
        val tabIndex = if (nav.placement == DrawerNavigationPlacement.BOTTOM) sheet.childCount else 2
        sheet.addView(tabs, tabIndex.coerceIn(0, sheet.childCount),
            LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        tabs.visibility = if (nav.enabled && nav.placement != DrawerNavigationPlacement.HIDDEN &&
            pageHost.visibility == VISIBLE && pageHost.navigationController.pages().isNotEmpty()) VISIBLE else GONE
        tabs.isTabIndicatorFullWidth = nav.indicator.widthFraction >= 0.99f
        tabs.tabMode = if (nav.scrollable) TabLayout.MODE_SCROLLABLE else TabLayout.MODE_FIXED
        tabs.tabGravity = if (nav.scrollable) TabLayout.GRAVITY_START else TabLayout.GRAVITY_FILL
        val indicatorColor = nav.indicator.color ?: resolvedColors().accent ?: Color.BLUE
        tabs.setSelectedTabIndicator(GradientDrawable().apply {
            setColor(if (nav.indicator.shape == com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerIndicatorShape.NONE) {
                Color.TRANSPARENT
            } else indicatorColor)
            cornerRadius = if (nav.indicator.shape == com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerIndicatorShape.PILL) {
                dp(nav.indicator.heightDp).toFloat()
            } else 0f
        })
        tabs.setSelectedTabIndicatorHeight(
            if (nav.indicator.shape == com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerIndicatorShape.NONE) 0
            else dp(nav.indicator.heightDp)
        )
        tabs.setTabTextColors(
            resolvedColors().secondaryText ?: Color.GRAY,
            resolvedColors().accent ?: Color.BLUE
        )
    }

    private fun applyVisualStyle() {
        val style = currentConfig.style
        val colors = resolvedColors()
        val radius = style.shape.topStartRadiusDp.takeIf { it > 0f } ?: style.dimensions.cornerRadiusDp
        sheet.background = GradientDrawable().apply {
            setColor(colors.surface ?: Color.WHITE)
            cornerRadii = floatArrayOf(dp(radius).toFloat(), dp(radius).toFloat(),
                dp(radius).toFloat(), dp(radius).toFloat(),
                dp(style.shape.bottomEndRadiusDp).toFloat(), dp(style.shape.bottomEndRadiusDp).toFloat(),
                dp(style.shape.bottomStartRadiusDp).toFloat(), dp(style.shape.bottomStartRadiusDp).toFloat())
            if (style.shape.outlineWidthDp > 0f) {
                setStroke(dp(style.shape.outlineWidthDp), colors.outline ?: (colors.primaryText ?: Color.GRAY))
            }
        }
        handle.background = GradientDrawable().apply {
            setColor(colors.secondaryText ?: Color.GRAY)
            cornerRadius = dp(style.dimensions.handleHeightDp).toFloat()
        }
        title.setTextColor(colors.primaryText ?: Color.BLACK)
        title.textSize = style.typography.titleSizeSp
        title.typeface = style.typography.fontFamily?.let {
            android.graphics.Typeface.create(it, style.typography.titleStyle)
        } ?: android.graphics.Typeface.create(title.typeface, style.typography.titleStyle)
        sheet.clipToOutline = style.clipContentToShape
        handle.layoutParams = (handle.layoutParams as LinearLayout.LayoutParams).apply {
            width = dp(style.dimensions.handleWidthDp)
            height = dp(style.dimensions.handleHeightDp)
        }
        sheet.elevation = dp(style.elevationDp).toFloat()
        sheet.setPadding(dp(style.dimensions.horizontalPaddingDp), 0,
            dp(style.dimensions.horizontalPaddingDp), dp(style.dimensions.verticalPaddingDp))
        scrim.setBackgroundColor(colors.scrim ?: Color.BLACK)
        scrim.visibility = if (style.drawScrim) VISIBLE else GONE
        title.visibility = if (title.text.isNullOrEmpty()) GONE else VISIBLE
        updateNavigationVisibility()
        updateVisualState()
        requestLayout()
    }

    private fun refreshPanelVisibilityCallbacks() {
        peekContentVisible = false
        expandedContentVisible = false
        updateVisualState()
    }

    private fun resolvedColors(): DrawerColors = DrawerThemeResolver.resolve(context, currentConfig.style)

    private fun recalculateAnchors() {
        if (height <= 0) return
        val density = resources.displayMetrics.density
        val maxHeight = min(height, dp(currentConfig.style.dimensions.expandedMaxHeightDp))
        val minHeight = min(maxHeight, dp(currentConfig.style.dimensions.peekHeightDp))
        sheet.layoutParams = sheet.layoutParams.apply { this.height = maxHeight }
        val contentWidth = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val bodyHeight = max(
            peekHost.measureContentHeight(contentWidth, maxHeight),
            max(
                expandedHost.measureContentHeight(contentWidth, maxHeight),
                pageHost.measureCurrentContentHeight(contentWidth, maxHeight)
            )
        )
        val headerHeight = dp(currentConfig.style.dimensions.handleHeightDp + 18f) +
            (if (title.visibility == VISIBLE) dp(36) else 0) +
            (if (tabs.visibility == VISIBLE) dp(48) else 0) +
            dp(currentConfig.style.dimensions.verticalPaddingDp)
        val contentHeight = (bodyHeight + headerHeight).coerceAtMost(maxHeight)
        anchors = DrawerAnchorResolver.resolve(
            currentConfig.anchorStrategy,
            DrawerAnchorInput(minHeight, maxHeight, height, contentHeight, maxHeight, density)
        )
        val oldHeight = state?.currentHeightPx ?: minHeight
        val nearest = DrawerAnchorResolver.nearestAnchor(anchors, oldHeight)
        updateState(nearest, false, false)
    }

    private fun updateState(anchor: DrawerAnchor, isDragging: Boolean, isAnimating: Boolean) {
        state = DrawerState(anchor.id, anchor.heightPx, anchors.ifEmpty { listOf(anchor) }, isDragging, isAnimating)
        updateVisualState()
        val snapshot = state ?: return
        stateListeners.toList().forEach { it(snapshot) }
    }

    private fun updateVisualState() {
        val current = state ?: return
        val sheetHeight = (sheet.layoutParams?.height ?: sheet.measuredHeight)
            .takeIf { it > 0 } ?: current.currentHeightPx
        sheet.translationY = (sheetHeight - current.currentHeightPx).toFloat()
        val minHeight = anchors.firstOrNull()?.heightPx ?: 1
        val maxHeight = anchors.lastOrNull()?.heightPx ?: minHeight
        val fraction = if (maxHeight == minHeight) 0f else
            ((current.currentHeightPx - minHeight).toFloat() / (maxHeight - minHeight)).coerceIn(0f, 1f)
        scrim.alpha = fraction * 0.48f
        scrim.isClickable = fraction > 0.01f && currentConfig.dismissOnScrimTap
        val expanded = current.currentHeightPx > minHeight
        val showPeek = !expanded && peekHost.content() != null
        val showExpanded = expanded && (expandedHost.content() != null || pageHost.navigationController.pages().isNotEmpty())
        peekHost.visibility = if (showPeek) VISIBLE else GONE
        expandedHost.visibility = if (showExpanded) VISIBLE else GONE
        val navigation = currentConfig.navigationConfig
        tabs.visibility = if (navigation.enabled &&
            navigation.placement != DrawerNavigationPlacement.HIDDEN &&
            pageHost.visibility == VISIBLE &&
            pageHost.navigationController.pages().isNotEmpty() &&
            (expanded || navigation.showInPeekPanel)) VISIBLE else GONE
        if (containerAttached && showPeek != peekContentVisible) {
            peekHost.dispatchPanelVisible(showPeek)
            peekContentVisible = showPeek
        }
        pageHost.setPanelVisible(showExpanded && pageHost.visibility == VISIBLE)
        if (containerAttached && showExpanded != expandedContentVisible) {
            expandedHost.dispatchPanelVisible(showExpanded)
            expandedContentVisible = showExpanded
        }
    }

    private fun moveTo(anchor: DrawerAnchor, animated: Boolean) {
        animator?.cancel()
        val start = state?.currentHeightPx ?: anchor.heightPx
        if (!animated || currentConfig.style.motion.settleDurationMs == 0L) {
            updateState(anchor, false, false)
            return
        }
        animator = ValueAnimator.ofInt(start, anchor.heightPx).apply {
            duration = currentConfig.style.motion.settleDurationMs
            addUpdateListener { value ->
                val heightPx = value.animatedValue as Int
                val nearest = DrawerAnchorResolver.nearestAnchor(anchors, heightPx)
                val intermediate = nearest.copy(heightPx = heightPx)
                state = DrawerState(nearest.id, heightPx, anchors, false, true)
                updateVisualState()
                stateListeners.toList().forEach { it(state!!) }
            }
            start()
        }
    }

    private fun onHandleTouch(view: View, event: MotionEvent): Boolean {
        if (!currentConfig.enableDrag) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                downY = event.rawY
                dragStartHeight = state?.currentHeightPx ?: anchors.firstOrNull()?.heightPx ?: dp(56f)
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dy = event.rawY - downY
                if (!dragging && abs(dy) > touchSlop) dragging = true
                if (dragging) {
                    val resistance = currentConfig.style.motion.dragResistance
                    val proposed = (dragStartHeight - dy * resistance).toInt()
                    val minHeight = anchors.firstOrNull()?.heightPx ?: 0
                    val maxHeight = anchors.lastOrNull()?.heightPx ?: minHeight
                    val bounded = when {
                        proposed < minHeight -> minHeight - ((minHeight - proposed) * 0.18f).toInt()
                        proposed > maxHeight -> maxHeight + ((proposed - maxHeight) * 0.18f).toInt()
                        else -> proposed
                    }
                    val nearest = DrawerAnchorResolver.nearestAnchor(anchors, bounded.coerceIn(minHeight, maxHeight))
                    state = DrawerState(nearest.id, bounded.coerceIn(minHeight, maxHeight), anchors, true, false)
                    updateVisualState()
                    stateListeners.toList().forEach { it(state!!) }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val velocityY = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null
                val currentHeight = state?.currentHeightPx ?: dragStartHeight
                val maximumVelocity = dp(currentConfig.style.motion.maximumFlingVelocityDp).toFloat()
                val boundedVelocity = velocityY.coerceIn(-maximumVelocity, maximumVelocity)
                val target = if (abs(boundedVelocity) >= dp(currentConfig.style.motion.minimumFlingVelocityDp)) {
                    DrawerAnchorResolver.flingTarget(anchors, currentHeight, -boundedVelocity,
                        dp(currentConfig.style.motion.minimumFlingVelocityDp).toFloat())
                } else DrawerAnchorResolver.nearestAnchor(anchors, currentHeight)
                moveTo(target, true)
                dragging = false
                return true
            }
        }
        return true
    }

    private fun updateHeightFromNestedScroll(heightPx: Int) {
        if (anchors.isEmpty()) return
        val minHeight = anchors.first().heightPx
        val maxHeight = anchors.last().heightPx
        val bounded = heightPx.coerceIn(minHeight, maxHeight)
        val nearest = DrawerAnchorResolver.nearestAnchor(anchors, bounded)
        state = DrawerState(nearest.id, bounded, anchors, isDragging = true, isAnimating = false)
        updateVisualState()
        state?.let { snapshot -> stateListeners.toList().forEach { it(snapshot) } }
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean =
        currentConfig.enableNestedScroll && (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean =
        onStartNestedScroll(child, target, axes)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        nestedScrollHelper.onNestedScrollAccepted(child, target, axes)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        nestedScrollHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View) {
        nestedScrollHelper.onStopNestedScroll(target)
        settleAfterNestedScroll()
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollHelper.onStopNestedScroll(target, type)
        settleAfterNestedScroll()
    }

    private fun settleAfterNestedScroll() {
        val currentHeight = state?.currentHeightPx ?: return
        if (anchors.isNotEmpty()) {
            moveTo(DrawerAnchorResolver.nearestAnchor(anchors, currentHeight), true)
        }
    }

    override fun getNestedScrollAxes(): Int = nestedScrollHelper.nestedScrollAxes

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!currentConfig.enableNestedScroll || anchors.isEmpty() || dy == 0) return
        val current = state?.currentHeightPx ?: anchors.first().heightPx
        val minHeight = anchors.first().heightPx
        val maxHeight = anchors.last().heightPx
        val next = when {
            dy > 0 && current < maxHeight -> (current + dy).coerceAtMost(maxHeight)
            dy < 0 && !target.canScrollVertically(-1) && current > minHeight ->
                (current + dy).coerceAtLeast(minHeight)
            else -> current
        }
        if (next != current) {
            consumed[1] += next - current
            updateHeightFromNestedScroll(next)
        }
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int
    ) = Unit

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int
    ) = Unit

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int, consumed: IntArray
    ) = Unit

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateAnchors()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        containerAttached = true
        updateVisualState()
    }

    override fun onDetachedFromWindow() {
        if (peekContentVisible) peekHost.dispatchPanelVisible(false)
        if (expandedContentVisible) expandedHost.dispatchPanelVisible(false)
        peekContentVisible = false
        expandedContentVisible = false
        containerAttached = false
        animator?.cancel()
        animator = null
        velocityTracker?.recycle()
        velocityTracker = null
        super.onDetachedFromWindow()
    }

    private inner class Controller : DrawerController {
        override fun expand(animated: Boolean) {
            anchors.lastOrNull()?.let { moveTo(it, animated) }
        }
        override fun collapse(animated: Boolean) {
            anchors.firstOrNull()?.let { moveTo(it, animated) }
        }
        override fun moveToAnchor(anchorId: String, animated: Boolean) {
            anchors.firstOrNull { it.id == anchorId }?.let { moveTo(it, animated) }
        }
        override fun setAnchorStrategy(strategy: DrawerAnchorStrategy) {
            currentConfig = currentConfig.copy(anchorStrategy = strategy)
            recalculateAnchors()
        }
        override fun applyConfig(config: DrawerConfig) {
            currentConfig = config
            val themedContext = contentThemeContext()
            if (peekHost.content() === config.peekContent) peekHost.refreshContentContext(themedContext)
            else peekHost.setContent(config.peekContent, themedContext)
            if (expandedHost.content() === config.expandedContent) expandedHost.refreshContentContext(themedContext)
            else expandedHost.setContent(config.expandedContent, themedContext)
            pageHost.setContentContext(themedContext, recreateCachedViews = true)
            refreshPanelVisibilityCallbacks()
            pageHost.visibility = if (config.expandedContent == null) VISIBLE else GONE
            applyVisualStyle()
            recalculateAnchors()
        }
        override fun setStyle(style: DrawerStyle) {
            val themeChanged = currentConfig.style.themeMode != style.themeMode
            currentConfig = currentConfig.copy(style = style)
            if (themeChanged) {
                val themedContext = contentThemeContext()
                peekHost.refreshContentContext(themedContext)
                expandedHost.refreshContentContext(themedContext)
                pageHost.setContentContext(themedContext, recreateCachedViews = true)
                refreshPanelVisibilityCallbacks()
            }
            applyVisualStyle()
            recalculateAnchors()
        }
        override fun setThemeMode(mode: DrawerThemeMode) = setStyle(currentConfig.style.copy(themeMode = mode))
        override fun setColors(colors: DrawerColors) = setStyle(currentConfig.style.copy(colors = colors))
        override fun setDimensions(dimensions: DrawerDimensions) = setStyle(currentConfig.style.copy(dimensions = dimensions))
        override fun setTypography(typography: DrawerTypography) = setStyle(currentConfig.style.copy(typography = typography))
        override fun setMotion(motion: DrawerMotion) = setStyle(currentConfig.style.copy(motion = motion))
        override fun setShape(shape: DrawerShape) = setStyle(currentConfig.style.copy(shape = shape))
        override fun setPeekContent(content: DrawerPanelContent?) {
            currentConfig = currentConfig.copy(peekContent = content)
            val changed = peekHost.content() !== content
            peekHost.setContent(content, contentThemeContext())
            if (changed) refreshPanelVisibilityCallbacks()
            recalculateAnchors()
        }
        override fun setExpandedContent(content: DrawerPanelContent?) {
            currentConfig = currentConfig.copy(expandedContent = content)
            this@DrawerContainer.setExpandedContent(content)
        }
        override fun setTitle(title: CharSequence?) = this@DrawerContainer.setTitle(title)
        override fun setPages(pages: List<DrawerPage>, preserveSelection: Boolean) =
            this@DrawerContainer.setPages(pages, preserveSelection)
        override fun selectPage(pageId: String): Boolean = pageHost.selectPage(pageId)
        override fun setNavigationConfig(config: DrawerNavigationConfig) {
            currentConfig = currentConfig.copy(navigationConfig = config)
            applyVisualStyle()
        }
        override fun setDragEnabled(enabled: Boolean) { currentConfig = currentConfig.copy(enableDrag = enabled); handle.isEnabled = enabled }
        override fun setNestedScrollEnabled(enabled: Boolean) { currentConfig = currentConfig.copy(enableNestedScroll = enabled) }
        override fun setEdgeToEdgeEnabled(enabled: Boolean) { currentConfig = currentConfig.copy(enableEdgeToEdge = enabled) }
        override fun setDismissOnScrimTap(enabled: Boolean) { currentConfig = currentConfig.copy(dismissOnScrimTap = enabled); updateVisualState() }
        override fun setDismissOnBack(enabled: Boolean) { currentConfig = currentConfig.copy(dismissOnBack = enabled) }
        override fun setDragHandleEnabled(enabled: Boolean) { currentConfig = currentConfig.copy(dragHandleEnabled = enabled); handle.visibility = if (enabled) VISIBLE else GONE }
        override fun setAccessibilityEnabled(enabled: Boolean) {
            currentConfig = currentConfig.copy(accessibilityEnabled = enabled)
            sheet.importantForAccessibility = if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        override fun currentState(): DrawerState? = state
        override fun addStateListener(listener: (DrawerState) -> Unit) { stateListeners += listener; state?.let(listener) }
        override fun removeStateListener(listener: (DrawerState) -> Unit) { stateListeners -= listener }
    }
}
