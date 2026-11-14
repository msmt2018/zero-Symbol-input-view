/*
 * This file is part of ZeroStudio.
 *
 * Copyright (C) 2026 android_zero
 *
 * SPDX-License-Identifier: AGPL-3.0-only AND GPL-3.0-only AND Apache-2.0
 *
 * License Information:
 *
 * This file is distributed under the following licenses, which apply
 * cumulatively to the same work:
 *
 * 1. GNU Affero General Public License v3.0 only (AGPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    When a modified version is used to provide network services to users,
 *    the corresponding source code must be made available under the
 *    applicable AGPL terms.
 *
 * 2. GNU General Public License v3.0 only (GPL-3.0-only):
 *    Grants the rights to use, study, modify, and redistribute this work.
 *    Distribution of covered binaries requires compliance with the source
 *    code, license notice, attribution, and other applicable GPL terms.
 *
 * 3. Apache License 2.0 (Apache-2.0):
 *    Grants copyright and applicable patent rights to use, modify, and
 *    distribute this work, subject to its license conditions, including
 *    preservation of notices, inclusion of the license, and marking changes.
 *
 * The SPDX operator "AND" means that all listed license terms apply
 * cumulatively. It does not mean that recipients may choose only one
 * of the listed licenses.
 *
 * You must comply with the applicable terms of all three licenses.
 * This notice is a summary and does not replace the full license texts.
 *
 * Full license texts:
 * - GNU AGPL v3.0: https://www.gnu.org/licenses/agpl-3.0.html
 * - GNU GPL v3.0:  https://www.gnu.org/licenses/gpl-3.0.html
 * - Apache 2.0:    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Author: android_zero
 * GitHub: https://github.com/msmt2018
 * WeChat: android_zero
 */

package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.app.AlertDialog
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.DrawerConfig
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.HapticMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettingsScreen
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.ThemeMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.state.DrawerGeometry
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.adapter.SymbolDrawerAnchorAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.core.DrawerInteractionController
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolGroupIndicator
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.SymbolPageAdapter
import kotlin.math.roundToInt

/**
 * A self-contained symbol drawer: vertical drag controls its height while
 * ViewPager2 owns horizontal group paging. The two axes are arbitrated before
 * the parent intercepts, so horizontal page swipes are not stolen by the drawer.
 *
 * Symbol actions are exposed as callbacks; this view has no dependency on a
 * particular editor implementation.
 */
class SymbolDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var config: DrawerConfig = DrawerConfig()
) : ViewGroup(context, attrs, defStyleAttr) {

    fun interface OnSymbolActionListener {
        fun onAction(symbol: SymbolEntry, isLongPress: Boolean)
    }

    var onSymbolActionListener: OnSymbolActionListener? = null
    var onOpenManagerListener: (() -> Unit)? = null
    /** Optional dedicated settings entry callback; falls back to the manager callback for V1 hosts. */
    var onOpenSettingsListener: (() -> Unit)? = null
    var onGroupChangedListener: ((Int, SymbolGroup?) -> Unit)? = null

    private val settingsRepository = SharedPreferencesSymbolDrawerSettingsRepository(context.applicationContext)
    private val hasPersistedSettings = settingsRepository.hasStoredSettings()
    private var settings = settingsRepository.load().settings.copy(themeMode = ThemeMode.SYSTEM)
    private var settingsDialog: AlertDialog? = null
    internal val isSettingsDialogShowing: Boolean get() = settingsDialog?.isShowing == true
    private val indicator = SymbolGroupIndicator(context) { selectGroup(it, true) }.apply {
        onSettingsClick = { openSettings() }
    }
    private val pager = ViewPager2(context).apply {
        orientation = ViewPager2.ORIENTATION_HORIZONTAL
        offscreenPageLimit = 1
        isUserInputEnabled = true
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private var pageAdapter = createPageAdapter()
    private var suppressPageCallbacks = false
    private var adapterSwapGeneration = 0

    private var repository: SymbolGroupRepository =
        SharedPreferencesSymbolGroupRepository(context.applicationContext)
    private var groups: List<SymbolGroup> = emptyList()
    private var collapsedHeight = DrawerGeometry.collapsedHeightPx(config, resources.displayMetrics.density)
    private var expandedGridHeight = collapsedHeight
    private var expandedHeight = collapsedHeight
    private var drawerHeight = collapsedHeight
    private var pagerHeight = collapsedHeight
    private var expansion = 0f
    /**
     * The universal drawer core owns gesture arbitration, velocity tracking,
     * continuous drag state and settling; the feature retains its exact layout.
     */
    private val interactionController = DrawerInteractionController(
        context = context,
        onHeightChanged = { height, _, _ -> setExpansionFromHeight(height) },
        onDisallowInterceptChanged = ::setParentIntercept
    )

    init {
        clipChildren = false
        clipToPadding = false
        addView(indicator, LayoutParams(LayoutParams.MATCH_PARENT, 0))
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, collapsedHeight))
        pager.adapter = pageAdapter
        // Load the durable settings snapshot before the first visible layout.
        // Rebuild the adapter once below so grid geometry and interactions use
        // the same persisted values as the settings dialog.
        if (hasPersistedSettings) config = settings.toDrawerConfig()
        LegacySymbolDataMigration.importIfV2Empty(context, repository)
        if (repository.loadGroups().isEmpty()) {
            repository.saveGroups(com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaults.createGroups())
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (suppressPageCallbacks) return
                indicator.setSelectedIndex(position)
                onGroupChangedListener?.invoke(position, groups.getOrNull(position))
            }
        })
        reloadGroups()
        if (hasPersistedSettings) applySettings(settings)
        updateVisualState(0f)
    }

    /**
     * Applies a persisted settings snapshot to the live drawer. Rebuilds only the
     * lightweight page adapter when grid geometry changes; group data is retained.
     */
    fun applySettings(value: SymbolDrawerSettings) {
        settings = value.copy(themeMode = com.zerostudio.idea.widget.editor.symbolinput.v2.settings.ThemeMode.SYSTEM)
        config = settings.toDrawerConfig()
        setBackgroundColor(com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors.surface(context))
        indicator.applySettings(settings)
        pager.offscreenPageLimit = if (settings.prefetchAdjacentGroups) settings.pageCacheLimit else 1

        // Prepare the replacement adapter while the current adapter remains
        // attached and interactive. Assigning a fresh ListAdapter immediately
        // resets ViewPager2 to page zero; doing that before the new list is
        // committed caused a visible jump back after a user swiped to another
        // group. Swap only after the replacement has its complete data set.
        val replacement = createPageAdapter()
        val generation = ++adapterSwapGeneration
        replacement.submitList(groups.toList()) {
            if (generation != adapterSwapGeneration) return@submitList

            val selectedGroupId = groups.getOrNull(pager.currentItem)?.id
            val restored = selectedGroupId?.let { id ->
                groups.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            } ?: pager.currentItem.coerceIn(0, (groups.size - 1).coerceAtLeast(0))

            suppressPageCallbacks = true
            pageAdapter = replacement
            pager.adapter = replacement
            if (groups.isNotEmpty()) pager.setCurrentItem(restored, false)
            suppressPageCallbacks = false

            indicator.setSelectedIndex(restored)
            onGroupChangedListener?.invoke(restored, groups.getOrNull(restored))
            recalculateGeometry()
        }
    }

    private fun openSettings() {
        onOpenSettingsListener?.let {
            it.invoke()
            return
        }
        val screen = SymbolDrawerSettingsScreen(
            context,
            settingsRepository,
            repository
        ).apply {
            onSettingsChanged = { applySettings(it) }
            onGroupsChanged = {
                submitGroups(it)
                saveGroups()
            }
        }
        settingsDialog?.dismiss()
        settingsDialog = AlertDialog.Builder(context)
            .setTitle("符号抽屉设置")
            .setView(screen)
            .setPositiveButton("完成", null)
            .create()
            .also { it.show() }
    }

    fun getSettings(): SymbolDrawerSettings = settings

    /** Refresh palette after the host changes its theme without recreating its Activity. */
    fun refreshHostTheme() {
        setBackgroundColor(com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors.surface(context))
        indicator.setBackgroundColor(com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors.surface(context))
        indicator.applySettings(settings)
        applySettings(settings)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshHostTheme()
    }

    private fun createPageAdapter() = SymbolPageAdapter(config, settings) { symbol, longPress ->
        val action = if (longPress) symbol.longAction else symbol.shortAction
        if (action != null) {
            if (action == SymbolAction.OpenManager) {
                onOpenManagerListener?.invoke()
            } else {
                when (settings.hapticMode) {
                    HapticMode.OFF -> Unit
                    HapticMode.SELECTION -> if (!longPress) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    HapticMode.ALL -> performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                onSymbolActionListener?.onAction(symbol, longPress)
                if (settings.autoCollapseAfterAction) setExpanded(false, true)
            }
        }
    }

    fun setRepository(value: SymbolGroupRepository, reload: Boolean = true) {
        repository = value
        if (reload) reloadGroups()
    }

    fun submitGroups(
        value: List<SymbolGroup>,
        preserveSelectedGroup: Boolean = true,
        onCommitted: (() -> Unit)? = null
    ) {
        require(value.map { it.id }.distinct().size == value.size) {
            "Symbol group IDs must be unique"
        }
        value.forEach { group ->
            require(group.symbols.map { it.id }.distinct().size == group.symbols.size) {
                "Symbol IDs must be unique within group ${group.id}"
            }
        }
        val previousId = groups.getOrNull(pager.currentItem)?.id
        groups = value.map { it.copy(symbols = it.symbols.toList()) }
        val restored = if (preserveSelectedGroup && previousId != null) {
            groups.indexOfFirst { it.id == previousId }.takeIf { it >= 0 }
        } else null
        val target = restored ?: pager.currentItem.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
        indicator.submitGroups(groups)
        pageAdapter.submitList(groups) {
            if (groups.isNotEmpty() && target in groups.indices) {
                pager.setCurrentItem(target, false)
            }
            onCommitted?.invoke()
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
        // Kept explicit for hosts that want to refresh after returning from a manager.
        reloadGroups()
    }

    override fun onSaveInstanceState(): Parcelable {
        return DrawerSavedState(super.onSaveInstanceState()).apply {
            selectedGroupId = groups.getOrNull(pager.currentItem)?.id
            expansion = this@SymbolDrawerView.expansion
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is DrawerSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        pageAdapter.submitList(groups) {
            val selectedIndex = state.selectedGroupId?.let { id ->
                groups.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            }
            if (selectedIndex != null) pager.setCurrentItem(selectedIndex, false)
            animateTo(state.expansion, animated = false)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams =
        LayoutParams(context, attrs)

    override fun generateLayoutParams(params: LayoutParams): LayoutParams =
        LayoutParams(params)

    override fun checkLayoutParams(params: LayoutParams): Boolean = params is LayoutParams

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

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        interactionController.onInterceptTouchEvent(event) { x, y ->
            canScrollableDescendantScrollVertically(-1, x, y)
        }

    /**
     * Finds a vertically scrollable descendant under the active pointer.
     * A downward gesture should scroll that content to its start before the
     * drawer itself begins collapsing.
     */
    private fun canScrollableDescendantScrollVertically(
        direction: Int,
        x: Float,
        y: Float
    ): Boolean {
        fun scan(view: View, localX: Float, localY: Float): Boolean {
            if (view !is ViewGroup) return view.canScrollVertically(direction)
            for (index in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(index)
                if (child.visibility != View.VISIBLE || child.width <= 0 || child.height <= 0) continue
                val childX = localX + view.scrollX - child.left
                val childY = localY + view.scrollY - child.top
                if (childX >= 0f && childX < child.width &&
                    childY >= 0f && childY < child.height &&
                    scan(child, childX, childY)
                ) return true
            }
            return view.canScrollVertically(direction)
        }
        return scan(this, x, y)
    }

    /**
     * RecyclerView/ViewPager2 may request that ancestors do not intercept.
     * Ignore that request until this container has identified the gesture axis;
     * otherwise a child can accidentally disable the drawer's vertical drag.
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (interactionController.isGestureAxisLocked) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean =
        interactionController.onTouchEvent(event)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        interactionController.onAttached()
        recalculateGeometry()
    }

    override fun onDetachedFromWindow() {
        settingsDialog?.dismiss()
        settingsDialog = null
        interactionController.onDetached()
        super.onDetachedFromWindow()
    }

    private fun setParentIntercept(disallowIntercept: Boolean) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    private fun recalculateGeometry() {
        val oldFraction = expansion
        // Resolve the symbol-specific geometry through the public Drawer anchor
        // contract. The adapter deliberately preserves the established pixel
        // formulas; this is a policy migration, not a visual/layout redesign.
        val resolved = SymbolDrawerAnchorAdapter.resolve(
            groups = groups,
            config = config,
            density = resources.displayMetrics.density
        )
        collapsedHeight = resolved.collapsedHeightPx
        expandedGridHeight = resolved.expandedGridHeightPx
        expandedHeight = resolved.expandedHeightPx
        val targetHeight = (collapsedHeight + (expandedHeight - collapsedHeight) * oldFraction).roundToInt()
        interactionController.setBounds(
            minimumHeightPx = collapsedHeight,
            maximumHeightPx = expandedHeight,
            currentHeightPx = targetHeight,
            animationDurationMs = config.animationDurationMs.toLong(),
            flingVelocityDpPerSecond = config.flingVelocityDpPerSecond.toFloat()
        )
        setExpansionFromHeight(targetHeight)
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
        val reveal = DrawerGeometry.indicatorReveal(fraction)
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

    internal class DrawerSavedState : View.BaseSavedState {
        var selectedGroupId: String? = null
        var expansion: Float = 0f

        constructor(superState: Parcelable?) : super(superState)
        constructor(source: Parcel) : super(source) {
            selectedGroupId = source.readString()
            expansion = source.readFloat().coerceIn(0f, 1f)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(selectedGroupId)
            out.writeFloat(expansion)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<DrawerSavedState> =
                object : Parcelable.Creator<DrawerSavedState> {
                    override fun createFromParcel(source: Parcel) = DrawerSavedState(source)
                    override fun newArray(size: Int): Array<DrawerSavedState?> = arrayOfNulls(size)
                }
        }
    }

    private fun animateTo(target: Float, animated: Boolean) {
        val destination = target.coerceIn(0f, 1f)
        val targetHeight = (collapsedHeight + (expandedHeight - collapsedHeight) * destination).roundToInt()
        interactionController.settleToHeight(targetHeight, animated)
    }
}


/** @deprecated Use [SymbolDrawerView]. */
@Deprecated("Use SymbolDrawerView")
typealias AdvancedSymbolDrawerView = SymbolDrawerView
