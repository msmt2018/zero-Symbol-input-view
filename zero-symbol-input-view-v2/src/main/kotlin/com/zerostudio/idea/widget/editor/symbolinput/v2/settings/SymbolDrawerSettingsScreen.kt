/*
 * This file is part of zero-Symbol-input-view-V2.
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

package com.zerostudio.idea.widget.editor.symbolinput.v2.settings

import com.zerostudio.idea.widget.editor.symbolinput.v2.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.doOnLayout
import android.widget.Toast
import android.app.AlertDialog
import android.text.InputType
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerSettingsScope
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerTheme
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView

/**
 * Settings-only surface. Symbol-group editing stays in the dedicated manager
 * dialog; this screen owns the two backup/import workflows.
 */
class SymbolDrawerSettingsScreen(
    context: Context,
    private val repository: SymbolDrawerSettingsRepository =
        SharedPreferencesSymbolDrawerSettingsRepository(context),
    private val groupRepository: SymbolGroupRepository =
        SharedPreferencesSymbolGroupRepository(context),
    /** Optional session registry for app-contributed settings tabs and rows. */
    private val extensionRegistry: SymbolDrawerExtensionRegistry? = null
) : LinearLayout(context) {

    var onSettingsChanged: ((SymbolDrawerSettings) -> Unit)? = null
    var onGroupsChanged: ((List<SymbolGroup>) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private var current = repository.load().settings.copy(themeMode = ThemeMode.SYSTEM)
    private var activePage: LinearLayout? = null
    private var pendingImport: ImportKind = ImportKind.SETTINGS
    private var adaptiveSpacingScale: Float = 1f
    private var adaptiveTypographyScale: Float = 1f
    private var selectedTabIndex: Int = 0
    private var activeScrollView: ScrollView? = null
    private var spacingReflowInProgress = false
    private val content = LinearLayout(context).apply { orientation = VERTICAL }
    private var extensionObserver: AutoCloseable? = null
    private var themeObserver: AutoCloseable? = null
    private var themeSource: SymbolDrawerView? = null
    private var drawerTheme: SymbolDrawerTheme? = null
    private val extensionViewDisposers = mutableListOf<() -> Unit>()
    private var extensionViewsDisposedOnDetach = false
    private var detachedScrollY = 0
    private val documentPicker = JsonDocumentPicker(
        context,
        onImported = ::handleImportedJson,
        onError = { onError?.invoke(it); toast(context.getString(R.string.symbol_drawer_file_error, it.message ?: context.getString(R.string.symbol_drawer_unknown_error))) }
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(surfaceColor())
        setPadding(dp(12), dp(12), dp(12), dp(8))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        render()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val density = resources.displayMetrics.density
        val window = WindowAdaptiveInfo(w / density, h / density, density)
        val spec = AdaptiveContentLayoutResolver.resolve(window)
        val nextSpacingScale = when (window.sizeClass) {
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.COMPACT -> 0.96f
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.MEDIUM -> 1f
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.EXPANDED -> 1.08f
        }
        val nextTypographyScale = when (window.sizeClass) {
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.COMPACT -> 0.98f
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.MEDIUM -> 1f
            com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowSizeClass.EXPANDED -> 1.04f
        }
        val metricsChanged = nextSpacingScale != adaptiveSpacingScale ||
            nextTypographyScale != adaptiveTypographyScale
        adaptiveSpacingScale = nextSpacingScale
        adaptiveTypographyScale = nextTypographyScale
        val horizontal = dp(spec.horizontalPaddingDp.toInt())
        setPadding(horizontal, dp(12), horizontal, dp(8))
        gravity = Gravity.CENTER_HORIZONTAL
        val params = content.layoutParams as? LayoutParams ?: return
        val targetWidth = minOf((w - horizontal * 2).coerceAtLeast(0), dp(spec.maxContentWidthDp.toInt()))
        if (params.width != targetWidth) {
            params.width = targetWidth
            content.layoutParams = params
        }
        if (metricsChanged && !spacingReflowInProgress && content.childCount > 0) {
            spacingReflowInProgress = true
            val previousScroll = activeScrollView?.scrollY ?: 0
            render()
            restoreActiveScrollPosition(previousScroll)
            spacingReflowInProgress = false
        }
    }

    /** Bind the settings surface to the owning drawer without changing its constructor ABI. */
    fun bindThemeSource(drawer: SymbolDrawerView) {
        if (themeSource === drawer) return
        themeObserver?.close()
        themeObserver = null
        themeSource = drawer
        drawerTheme = drawer.themeSnapshot
        if (isAttachedToWindow) observeDrawerTheme()
    }

    private fun observeDrawerTheme() {
        if (themeObserver != null) return
        themeObserver = themeSource?.observeTheme(emitCurrent = false) { theme ->
            drawerTheme = theme
            post { if (isAttachedToWindow) render() }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (extensionViewsDisposedOnDetach) {
            extensionViewsDisposedOnDetach = false
            val restoreScroll = detachedScrollY
            post {
                if (isAttachedToWindow) {
                    render()
                    restoreActiveScrollPosition(restoreScroll)
                }
            }
        }
        observeDrawerTheme()
        if (extensionObserver == null) {
            extensionObserver = extensionRegistry?.observe(emitCurrent = false) {
                post { if (isAttachedToWindow) render() }
            }
        }
    }

    override fun onDetachedFromWindow() {
        detachedScrollY = activeScrollView?.scrollY ?: detachedScrollY
        extensionObserver?.close()
        extensionObserver = null
        themeObserver?.close()
        themeObserver = null
        disposeExtensionViews()
        extensionViewsDisposedOnDetach = true
        super.onDetachedFromWindow()
    }

    /**
     * Reflow replaces the active ScrollView. Restore its offset only after the
     * replacement has completed a real layout pass, otherwise scrollTo() can
     * be clamped against the transient zero-height viewport.
     */
    private fun restoreActiveScrollPosition(scrollY: Int) {
        val target = activeScrollView ?: return
        target.doOnLayout { laidOut ->
            // A newly rendered ScrollView can complete its own layout before
            // its WRAP_CONTENT page has received the final measured height.
            // Restore on the next UI turn so scroll range is computed from the
            // replacement page, not the transient zero-height measurement.
            laidOut.post {
                val contentHeight = laidOut.getChildAt(0)?.height ?: 0
                val maxScroll = (contentHeight - laidOut.height).coerceAtLeast(0)
                laidOut.scrollTo(0, scrollY.coerceIn(0, maxScroll))
            }
        }
    }

    private fun disposeExtensionViews() {
        val pending = extensionViewDisposers.toList()
        extensionViewDisposers.clear()
        pending.forEach { dispose -> runCatching(dispose) }
    }

    private fun addExtensionView(
        page: LinearLayout,
        view: View,
        extensionId: String,
        dispose: () -> Unit
    ) {
        val safeDispose: () -> Unit = {
            runCatching(dispose).onFailure { extensionRegistry?.reportError(extensionId, it) }
            Unit
        }
        runCatching { page.addView(view, matchWrap()) }
            .onSuccess { extensionViewDisposers += safeDispose }
            .onFailure { error ->
                extensionRegistry?.reportError(extensionId, error)
                safeDispose()
                page.addView(TextView(context).apply {
                    text = context.getString(R.string.symbol_extension_failed_to_mount_extension_view) + extensionId
                    setTextColor(android.graphics.Color.RED)
                }, matchWrap())
            }
    }

    fun reload() {
        current = repository.load().settings.copy(themeMode = ThemeMode.SYSTEM)
        render()
        onSettingsChanged?.invoke(current)
    }

    fun getSettings(): SymbolDrawerSettings = current

    private fun render() {
        disposeExtensionViews()
        content.removeAllViews()
        content.setBackgroundColor(surfaceColor())
        activePage = null
        activeScrollView = null

        content.addView(LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(spaceDp(18), spaceDp(16), spaceDp(18), spaceDp(16))
            background = surfaceBackground(surfaceColor(), 0.10f, 22)
            addView(TextView(context).apply {
                text = context.getString(R.string.symbol_settings_symbol_drawer_settings)
                textSize = textSp(23f)
                setTextColor(primaryColor())
                setTypeface(typeface, Typeface.BOLD)
            }, matchWrap())
            addView(TextView(context).apply {
                text = context.getString(R.string.symbol_layout_manage_appearance_layout_interaction_performance_and_da)
                textSize = textSp(13f)
                setTextColor(onSurfaceColor())
                alpha = 0.72f
                setPadding(0, dp(5), 0, 0)
            }, matchWrap())
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val extensionSnapshot = extensionRegistry?.snapshot()
        val extensionTabs = extensionSnapshot?.settingsTabs.orEmpty()
        val extensionSettings = extensionSnapshot?.settingsItems.orEmpty()
        val tabs = TabLayout(context).apply {
            tabMode = TabLayout.MODE_SCROLLABLE
            tabGravity = TabLayout.GRAVITY_START
            setSelectedTabIndicatorColor(primaryColor())
            setTabTextColors(onSurfaceColor(), primaryColor())
            setBackgroundColor(surfaceColor())
        }
        content.addView(tabs, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))

        val pageHost = FrameLayout(context)
        content.addView(pageHost, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val builders: List<Pair<String, () -> Unit>> = listOf(
            context.getString(R.string.symbol_settings_appearance) to {
                section(context.getString(R.string.symbol_settings_theme_and_display))
                addChoice(context.getString(R.string.symbol_settings_theme_mode), listOf(ThemeMode.SYSTEM), ThemeMode.SYSTEM) {
                    update { copy(themeMode = ThemeMode.SYSTEM) }
                }
                addChoice(context.getString(R.string.symbol_group_group_indicator_style), GroupIndicatorStyle.values().toList(), current.indicatorStyle) {
                    update { copy(indicatorStyle = it) }
                }
                addSwitch(context.getString(R.string.symbol_group_show_group_names), current.showGroupTitles) { update { copy(showGroupTitles = it) } }
                addSwitch(context.getString(R.string.symbol_item_show_symbol_labels), current.showSymbolLabels) { update { copy(showSymbolLabels = it) } }
                addSwitch(context.getString(R.string.symbol_item_show_full_symbol_labels), current.showFullSymbolLabels) { update { copy(showFullSymbolLabels = it) } }
                addHint(context.getString(R.string.symbol_layout_keep_symbol_titles_on_one_line_without_truncation_the_g))
                addSeek(context.getString(R.string.symbol_item_symbol_label_text_size_sp), current.symbolLabelTextSizeSp, 8, 32) { update { copy(symbolLabelTextSizeSp = it) } }
                addSwitch(context.getString(R.string.symbol_interaction_show_action_hints), current.showActionHints) { update { copy(showActionHints = it) } }
            },
            context.getString(R.string.symbol_layout_layout) to {
                section(context.getString(R.string.symbol_layout_size_and_spacing))
                addSeek(context.getString(R.string.symbol_layout_columns_per_row), current.columns, 1, 30) { update { copy(columns = it) } }
                addSeek(context.getString(R.string.symbol_layout_rows_shown_when_collapsed), current.collapsedRows, 1, 16) { update { copy(collapsedRows = it) } }
                addSeek(context.getString(R.string.symbol_layout_cell_height_dp), current.cellHeightDp, 6, 128) { update { copy(cellHeightDp = it) } }
                addSeek(context.getString(R.string.symbol_layout_horizontal_spacing_dp), current.cellHorizontalSpacingDp, -24, 48) { update { copy(cellHorizontalSpacingDp = it) } }
                addSeek(context.getString(R.string.symbol_layout_vertical_spacing_dp), current.cellVerticalSpacingDp, -24, 48) { update { copy(cellVerticalSpacingDp = it) } }
                addSeek(context.getString(R.string.symbol_layout_group_bar_height_dp), current.indicatorHeightDp, 0, 96) { update { copy(indicatorHeightDp = it) } }
                addSeek(context.getString(R.string.symbol_layout_maximum_expanded_height_dp), current.maxExpandedHeightDp, 80, 1800) { update { copy(maxExpandedHeightDp = it) } }
                addSeek(context.getString(R.string.symbol_layout_minimum_touch_target_dp), current.minimumTouchTargetDp, 20, 112) { update { copy(minimumTouchTargetDp = it) } }
            },
            context.getString(R.string.symbol_layout_tablet_mode) to {
                section(context.getString(R.string.symbol_layout_bottom_toolbar_and_tablet_adaptation))
                addHint(
                    context.getString(R.string.symbol_layout_the_toolbar_stays_docked_to_the_bottom_on_tablets_split) +
                        context.getString(R.string.symbol_layout_only_internal_control_sizes_grid_and_spacing_adapt_draw)
                )
            },
            context.getString(R.string.symbol_interaction_interaction) to {
                section(context.getString(R.string.symbol_interaction_gestures_and_actions))
                addSeek(context.getString(R.string.symbol_interaction_long_press_timeout_ms), current.longPressTimeoutMs, 250, 1000) { update { copy(longPressTimeoutMs = it) } }
                addSeek(context.getString(R.string.symbol_interaction_expand_fling_threshold_dp_s), current.flingVelocityDpPerSecond, 100, 6000) { update { copy(flingVelocityDpPerSecond = it) } }
                addSwitch(context.getString(R.string.symbol_interaction_collapse_after_action), current.autoCollapseAfterAction) { update { copy(autoCollapseAfterAction = it) } }
                addSwitch(context.getString(R.string.symbol_manager_remember_expanded_state), current.rememberExpandedState) { update { copy(rememberExpandedState = it) } }
                addHint(context.getString(R.string.symbol_interaction_remember_the_drawers_last_expanded_or_collapsed_state_w))
                section(context.getString(R.string.symbol_interaction_animation_and_haptics))
                addSeek(context.getString(R.string.symbol_interaction_animation_duration_ms), current.animationDurationMs, 0, 1200) { update { copy(animationDurationMs = it) } }
                addSwitch(context.getString(R.string.symbol_interaction_reduce_motion), current.reduceMotion) { update { copy(reduceMotion = it) } }
                addChoice(context.getString(R.string.symbol_interaction_haptic_feedback), HapticMode.values().toList(), current.hapticMode) { update { copy(hapticMode = it) } }
            },
            context.getString(R.string.symbol_search_performance_and_search) to {
                section(context.getString(R.string.symbol_performance_performance))
                addSeek(context.getString(R.string.symbol_performance_page_cache_size), current.pageCacheLimit, 1, 9) { update { copy(pageCacheLimit = it) } }
                addSwitch(context.getString(R.string.symbol_performance_preload_adjacent_groups), current.prefetchAdjacentGroups) { update { copy(prefetchAdjacentGroups = it) } }
                section(context.getString(R.string.symbol_search_search))
                addSwitch(context.getString(R.string.symbol_search_include_action_content_in_search), current.searchIncludesActionPayload) {
                    update { copy(searchIncludesActionPayload = it) }
                }
            },
            context.getString(R.string.symbol_manager_data_management) to {
                addDataManagementCard(
                    context.getString(R.string.symbol_data_settings_backup),
                    context.getString(R.string.symbol_data_back_up_appearance_layout_interaction_and_performance_o),
                    listOf(
                        context.getString(R.string.symbol_data_copy_settings_json) to ::copySettingsBackup,
                        context.getString(R.string.symbol_data_export_settings_file) to ::exportSettingsFile,
                        context.getString(R.string.symbol_data_import_settings_from_clipboard) to ::promptImportSettings,
                        context.getString(R.string.symbol_data_import_settings_from_file) to { pendingImport = ImportKind.SETTINGS; documentPicker.openJson() }
                    )
                )
                addDataManagementCard(
                    context.getString(R.string.symbol_data_symbol_group_backup),
                    context.getString(R.string.symbol_data_includes_groups_symbol_content_and_tap_long_press_actio),
                    listOf(
                        context.getString(R.string.symbol_data_copy_symbol_groups_json) to ::copyGroupsBackup,
                        context.getString(R.string.symbol_data_export_symbol_groups_file) to ::exportGroupsFile,
                        context.getString(R.string.symbol_data_import_symbol_groups_from_clipboard) to ::promptImportGroups,
                        context.getString(R.string.symbol_data_import_symbol_groups_from_file) to { pendingImport = ImportKind.GROUPS; documentPicker.openJson() }
                    )
                )
                addDataSafetyNotice()
                activePage?.addView(actionButton(context.getString(R.string.symbol_settings_restore_default_settings)) {
                    runCatching {
                        val defaults = SymbolDrawerSettings()
                        repository.save(defaults)
                        current = defaults
                        render()
                        onSettingsChanged?.invoke(current)
                        toast(context.getString(R.string.symbol_settings_settings_restored_to_defaults))
                    }.onFailure(::reportFailure)
                }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(50)).apply {
                    topMargin = spaceDp(14)
                })
            }
        )

        val totalTabCount = builders.size + extensionTabs.size
        selectedTabIndex = selectedTabIndex.coerceIn(0, (totalTabCount - 1).coerceAtLeast(0))
        val builtInTabIds = listOf("appearance", "layout", "tablet", "interaction", "performance-search", "data")
        val settingsScope = object : SymbolDrawerSettingsScope {
            override val current: SymbolDrawerSettings get() = this@SymbolDrawerSettingsScreen.current
            override fun update(transform: (SymbolDrawerSettings) -> SymbolDrawerSettings) {
                this@SymbolDrawerSettingsScreen.update { transform(this) }
            }
        }

        builders.forEachIndexed { index, entry ->
            tabs.addTab(tabs.newTab().setText(entry.first), index == selectedTabIndex)
            val page = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(spaceDp(16), spaceDp(8), spaceDp(16), spaceDp(28))
            }
            val scroll = ScrollView(context).apply {
                isFillViewport = true
                addView(page, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                visibility = if (index == selectedTabIndex) View.VISIBLE else View.GONE
            }
            pageHost.addView(scroll, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            if (index == selectedTabIndex) activeScrollView = scroll
            activePage = page
            entry.second.invoke()
            extensionSettings.asSequence()
                .filter { it.tabId == builtInTabIds.getOrNull(index) }
                .forEach { extension ->
                    runCatching { extension.createView(context, settingsScope) }
                        .onSuccess { view ->
                            addExtensionView(page, view, extension.id) { extension.onViewDisposed(view) }
                        }
                        .onFailure { error ->
                            extensionRegistry?.reportError(extension.id, error)
                            page.addView(TextView(context).apply {
                                text = context.getString(R.string.symbol_drawer_ext_settings_error, extension.title, error.message ?: context.getString(R.string.symbol_drawer_unknown_error))
                                setTextColor(android.graphics.Color.RED)
                            }, matchWrap())
                        }
                }
        }
        extensionTabs.forEachIndexed { extensionIndex, extension ->
            val index = builders.size + extensionIndex
            tabs.addTab(tabs.newTab().setText(extension.title), index == selectedTabIndex)
            val page = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(spaceDp(16), spaceDp(8), spaceDp(16), spaceDp(28))
            }
            val scroll = ScrollView(context).apply {
                isFillViewport = true
                addView(page, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                visibility = if (index == selectedTabIndex) View.VISIBLE else View.GONE
            }
            pageHost.addView(scroll, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            if (index == selectedTabIndex) activeScrollView = scroll
            runCatching { extension.createView(context, settingsScope) }
                .onSuccess { view ->
                    addExtensionView(page, view, extension.id) { extension.onViewDisposed(view) }
                }
                .onFailure { error ->
                    extensionRegistry?.reportError(extension.id, error)
                    page.addView(TextView(context).apply {
                        text = context.getString(R.string.symbol_drawer_ext_page_error, extension.title, error.message ?: context.getString(R.string.symbol_drawer_unknown_error))
                        setTextColor(android.graphics.Color.RED)
                    }, matchWrap())
                }
            extensionSettings.asSequence()
                .filter { it.tabId == extension.id }
                .forEach { setting ->
                    runCatching { setting.createView(context, settingsScope) }
                        .onSuccess { view ->
                            addExtensionView(page, view, setting.id) { setting.onViewDisposed(view) }
                        }
                        .onFailure { error -> page.addView(TextView(context).apply {
                            text = context.getString(R.string.symbol_drawer_setting_error, setting.title, error.message ?: context.getString(R.string.symbol_drawer_unknown_error))
                            setTextColor(android.graphics.Color.RED)
                        }, matchWrap()) }
                }
        }
        activePage = null
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTabIndex = tab.position
                activeScrollView = pageHost.getChildAt(tab.position) as? ScrollView
                for (i in 0 until pageHost.childCount) {
                    pageHost.getChildAt(i).visibility = if (i == tab.position) View.VISIBLE else View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    /**
     * Adds the backup safety notice at most once to the active settings page.
     * This also protects against repeated extension-driven page composition
     * accidentally invoking the data-page builder more than once.
     */
    private fun addDataSafetyNotice() {
        val page = activePage ?: return
        val notice = context.getString(R.string.symbol_data_the_two_backups_are_independent_json_structure_field_ty)
        val tag = "zero.symbol.drawer.data-safety-notice"
        val existing = mutableListOf<View>()
        fun collect(parent: ViewGroup) {
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (child.tag == tag || (child is TextView && child.text?.toString() == notice)) {
                    existing += child
                }
                if (child is ViewGroup) collect(child)
            }
        }
        collect(page)
        if (existing.isNotEmpty()) {
            existing.drop(1).forEach { duplicate ->
                (duplicate.parent as? ViewGroup)?.removeView(duplicate)
            }
            return
        }
        page.addView(TextView(context).apply {
            this.tag = tag
            text = notice
            textSize = textSp(12f)
            setTextColor(onSurfaceColor())
            alpha = 0.72f
            setPadding(spaceDp(4), spaceDp(12), spaceDp(4), spaceDp(8))
        }, matchWrap())
    }

    private fun addDataManagementCard(
        title: String,
        description: String,
        actions: List<Pair<String, () -> Unit>>
    ) {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(spaceDp(14), spaceDp(14), spaceDp(14), spaceDp(12))
            background = surfaceBackground(surfaceColor(), 0.10f, 18)
        }
        card.addView(TextView(context).apply {
            text = title
            textSize = textSp(16f)
            setTextColor(onSurfaceColor())
            setTypeface(typeface, Typeface.BOLD)
        }, matchWrap())
        card.addView(TextView(context).apply {
            text = description
            textSize = textSp(12f)
            setTextColor(onSurfaceColor())
            alpha = 0.72f
            setPadding(0, spaceDp(5), 0, spaceDp(12))
        }, matchWrap())
        actions.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            pair.forEachIndexed { index, action ->
                row.addView(actionButton(action.first, action.second), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    if (index == 0) marginEnd = spaceDp(5) else marginStart = spaceDp(5)
                })
            }
            card.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = spaceDp(8)
            })
        }
        activePage?.addView(card, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = spaceDp(12)
        })
    }

    private fun actionButton(title: String, action: () -> Unit) =
        com.google.android.material.button.MaterialButton(context).apply {
            text = title
            textSize = textSp(12f)
            isAllCaps = false
            cornerRadius = dp(12)
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(primaryColor())
            backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor())
            setTextColor(primaryColor())
            insetTop = 0
            insetBottom = 0
            minHeight = dp(48)
            setOnClickListener { runCatching(action).onFailure(::reportFailure) }
        }

    private fun section(title: String) {
        activePage?.addView(TextView(context).apply {
            text = title
            textSize = textSp(17f)
            setTextColor(onSurfaceColor())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(spaceDp(2), spaceDp(22), spaceDp(2), spaceDp(10))
        }, matchWrap())
    }

    private fun addHint(message: String) {
        activePage?.addView(TextView(context).apply {
            text = message
            textSize = textSp(12f)
            setTextColor(onSurfaceColor())
            alpha = 0.72f
            setPadding(spaceDp(6), spaceDp(2), spaceDp(6), spaceDp(12))
        }, matchWrap())
    }

    private fun addSwitch(title: String, checked: Boolean, changed: (Boolean) -> Unit) {
        activePage?.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(spaceDp(14), spaceDp(5), spaceDp(10), spaceDp(5))
            background = surfaceBackground(surfaceColor(), 0.08f, 14)
            addView(TextView(context).apply {
                text = title
                textSize = textSp(14f)
                setTextColor(onSurfaceColor())
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(Switch(context).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, value -> changed(value) }
            }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = spaceDp(8)
        })
    }

    private fun <T : Enum<T>> addChoice(title: String, values: List<T>, selected: T, changed: (T) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(spaceDp(14), spaceDp(10), spaceDp(14), spaceDp(10))
            background = surfaceBackground(surfaceColor(), 0.08f, 14)
        }
        row.addView(label(title), matchWrap())
        val spinner = Spinner(context)
        val initialIndex = values.indexOf(selected).coerceAtLeast(0)
        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            values.map { value ->
                when (value) {
                    ThemeMode.SYSTEM -> context.getString(R.string.symbol_manager_follow_host_activity)
                    LargeScreenLayoutMode.AUTO -> context.getString(R.string.symbol_layout_adapt_to_large_screens)
                    LargeScreenLayoutMode.BOTTOM -> context.getString(R.string.symbol_layout_dock_to_bottom)
                    LargeScreenLayoutMode.SIDE -> context.getString(R.string.symbol_layout_dock_to_side)
                    SideDockEdge.LEFT -> context.getString(R.string.symbol_manager_left)
                    SideDockEdge.RIGHT -> context.getString(R.string.symbol_manager_right)
                    GroupIndicatorStyle.TEXT, GroupIndicatorStyle.TEXT_TABS -> context.getString(R.string.group_indicator_style_text_tabs)
                    GroupIndicatorStyle.PILL -> context.getString(R.string.group_indicator_style_pill_slider)
                    GroupIndicatorStyle.DOTS -> context.getString(R.string.group_indicator_style_page_dots)
                    GroupIndicatorStyle.UNDERLINE -> context.getString(R.string.group_indicator_style_bottom_underline)
                    GroupIndicatorStyle.GOOEY_METABALL -> context.getString(R.string.group_indicator_style_liquid_metaball)
                    GroupIndicatorStyle.ELASTIC_WORM -> context.getString(R.string.group_indicator_style_elastic_worm)
                    GroupIndicatorStyle.SPRING_BLOB -> context.getString(R.string.group_indicator_style_spring_blob)
                    GroupIndicatorStyle.LIQUID_MERGE -> context.getString(R.string.group_indicator_style_liquid_merge)
                    GroupIndicatorStyle.VERNIER_TICKS -> context.getString(R.string.group_indicator_style_precision_vernier_ticks)
                    GroupIndicatorStyle.MECHANICAL_RULER -> context.getString(R.string.group_indicator_style_mechanical_ruler)
                    GroupIndicatorStyle.WHEEL_ROLLER -> context.getString(R.string.group_indicator_style_3d_roller)
                    GroupIndicatorStyle.CAMERA_DIAL -> context.getString(R.string.group_indicator_style_camera_dial)
                    GroupIndicatorStyle.RADIAL_RING -> context.getString(R.string.group_indicator_style_radial_progress)
                    GroupIndicatorStyle.RADAR_SWEEP -> context.getString(R.string.symbol_manager_radar_sweep)
                    GroupIndicatorStyle.HUD_ARC -> context.getString(R.string.group_indicator_style_hud_arc_gauge)
                    GroupIndicatorStyle.ORBIT_DOTS -> context.getString(R.string.group_indicator_style_orbit_dots)
                    GroupIndicatorStyle.ELASTIC_WAVE -> context.getString(R.string.group_indicator_style_elastic_wave)
                    GroupIndicatorStyle.SINE_WAVE -> context.getString(R.string.group_indicator_style_sine_wave)
                    GroupIndicatorStyle.BOUNCE_TRACK -> context.getString(R.string.group_indicator_style_bounce_track)
                    GroupIndicatorStyle.SPRING_SLIDER -> context.getString(R.string.group_indicator_style_spring_slider)
                    GroupIndicatorStyle.MORPHING_BAR -> context.getString(R.string.group_indicator_style_morphing_progress_bar)
                    GroupIndicatorStyle.SEGMENTED_TRACK -> context.getString(R.string.group_indicator_style_segmented_track)
                    GroupIndicatorStyle.PROGRESS_LINE -> context.getString(R.string.group_indicator_style_continuous_progress_line)
                    GroupIndicatorStyle.DOUBLE_TRACK -> context.getString(R.string.group_indicator_style_double_track)
                    GroupIndicatorStyle.MINIMAL_DASHES -> context.getString(R.string.group_indicator_style_minimal_ticks)
                    GroupIndicatorStyle.SQUARE_TICKS -> context.getString(R.string.group_indicator_style_square_ticks)
                    GroupIndicatorStyle.DIAMOND_MARKERS -> context.getString(R.string.group_indicator_style_diamond_markers)
                    GroupIndicatorStyle.TRIANGLE_POINTER -> context.getString(R.string.group_indicator_style_triangle_pointer)
                    GroupIndicatorStyle.HEXAGON_NODES -> context.getString(R.string.group_indicator_style_hexagon_nodes)
                    GroupIndicatorStyle.GLOWING_NODES -> context.getString(R.string.group_indicator_style_glowing_nodes)
                    GroupIndicatorStyle.PULSE_DOTS -> context.getString(R.string.group_indicator_style_pulse_dots)
                    GroupIndicatorStyle.STACKED_CARDS -> context.getString(R.string.group_indicator_style_stacked_cards)
                    GroupIndicatorStyle.MINI_PREVIEW -> context.getString(R.string.group_indicator_style_mini_page_preview)
                    GroupIndicatorStyle.THUMBNAIL_STRIP -> context.getString(R.string.group_indicator_style_thumbnail_strip)
                    GroupIndicatorStyle.NUMBER_BADGES -> context.getString(R.string.group_indicator_style_number_badges)
                    GroupIndicatorStyle.VERTICAL_RAIL -> context.getString(R.string.group_indicator_style_vertical_rail)
                    GroupIndicatorStyle.ARC_NODES -> context.getString(R.string.group_indicator_style_arc_nodes)
                    GroupIndicatorStyle.FLUID_TRAIL -> context.getString(R.string.group_indicator_style_fluid_trail)
                    GroupIndicatorStyle.DIGITAL_COUNTER -> context.getString(R.string.group_indicator_style_digital_counter)
                    GroupIndicatorStyle.STEPPER -> context.getString(R.string.group_indicator_style_stepper)
                    else -> value.name.lowercase().replace('_', ' ')
                }
            }
        )
        spinner.setSelection(initialIndex, false)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            private var lastPosition = initialIndex
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == lastPosition) return
                lastPosition = position
                values.getOrNull(position)?.let(changed)
            }
        }
        row.addView(spinner, matchWrap())
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = spaceDp(8)
        })
    }

    private fun addSeek(title: String, value: Int, minimum: Int, maximum: Int, changed: (Int) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(spaceDp(10), spaceDp(6), spaceDp(10), spaceDp(6))
            background = surfaceBackground(surfaceColor(), 0.05f, 12)
        }
        val valueLabel = label(context.getString(R.string.symbol_settings_value_summary, title, value))
        row.addView(valueLabel, matchWrap())
        row.addView(SeekBar(context).apply {
            max = maximum - minimum
            progress = (value - minimum).coerceIn(0, max)
            var trackingTouch = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueLabel.text = context.getString(R.string.symbol_settings_value_summary, title, progress + minimum)
                    // Keyboard and accessibility adjustments do not dispatch
                    // onStart/onStopTrackingTouch. Persist those user changes
                    // immediately, while touch drags still commit once on release.
                    if (fromUser && !trackingTouch) changed(progress + minimum)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {
                    trackingTouch = true
                }
                override fun onStopTrackingTouch(bar: SeekBar?) {
                    trackingTouch = false
                    changed((bar?.progress ?: 0) + minimum)
                }
            })
        }, matchWrap())
        activePage?.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = spaceDp(8)
        })
    }

    private fun copySettingsBackup() = copyJson(
        "Zero Symbol Drawer settings", SymbolDrawerSettingsCodec.encode(current), context.getString(R.string.symbol_data_settings_backup_json_copied)
    )

    private fun exportSettingsFile() = documentPicker.saveJson(
        "symbol-drawer-settings.json", SymbolDrawerSettingsCodec.encode(current)
    )

    private fun copyGroupsBackup() = copyJson(
        "Zero Symbol Drawer groups", SymbolGroupBundleCodec.encode(groupRepository.loadGroups()), context.getString(R.string.symbol_data_symbol_groups_json_copied)
    )

    private fun exportGroupsFile() = documentPicker.saveJson(
        "symbol-drawer-groups.json", SymbolGroupBundleCodec.encode(groupRepository.loadGroups())
    )

    private fun copyJson(label: String, json: String, message: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, json))
        toast(message)
    }

    private fun promptImportSettings() {
        promptJson(context.getString(R.string.symbol_data_import_settings_backup), context.getString(R.string.symbol_data_paste_settings_json_a_successful_import_replaces_curren)) { json ->
            importSettings(json)
        }
    }

    private fun promptImportGroups() {
        promptJson(context.getString(R.string.symbol_data_import_symbol_groups), context.getString(R.string.symbol_data_paste_v1_symbol_group_json_a_successful_import_replaces)) { json ->
            confirmGroupImport(json)
        }
    }

    private fun promptJson(title: String, message: String, imported: (String) -> Unit) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.symbol_data_paste_json)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setNegativeButton(context.getString(R.string.symbol_manager_cancel), null)
            .setPositiveButton(context.getString(R.string.symbol_manager_continue)) { _, _ ->
                runCatching { imported(input.text.toString()) }.onFailure(::reportFailure)
            }
            .showAdaptiveDialog()
    }

    private fun handleImportedJson(json: String) {
        when (pendingImport) {
            ImportKind.SETTINGS -> importSettings(json)
            ImportKind.GROUPS -> confirmGroupImport(json)
        }
    }

    private fun importSettings(json: String) {
        val next = SymbolDrawerSettingsCodec.decodeStrict(json)
        repository.save(next)
        current = next.copy(themeMode = ThemeMode.SYSTEM)
        render()
        onSettingsChanged?.invoke(current)
        toast(context.getString(R.string.symbol_data_settings_backup_validated_and_imported))
    }

    private fun confirmGroupImport(json: String) {
        // Decode and validate before asking for confirmation. Invalid files never
        // reach the repository and cannot partially overwrite the current data.
        val imported = SymbolGroupBundleCodec.decode(json)
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.symbol_group_confirm_replacing_symbol_groups))
            .setMessage(context.getString(R.string.symbol_data_validated_groups_replace_all_current_groups, imported.size))
            .setNegativeButton(context.getString(R.string.symbol_manager_cancel), null)
            .setPositiveButton(context.getString(R.string.symbol_data_import)) { _, _ ->
                runCatching {
                    groupRepository.saveGroups(imported)
                    onGroupsChanged?.invoke(imported)
                    toast(context.getString(R.string.symbol_data_symbol_groups_imported))
                }.onFailure(::reportFailure)
            }
            .showAdaptiveDialog()
    }

    private fun AlertDialog.Builder.showAdaptiveDialog(): AlertDialog {
        val dialog = create()
        dialog.setOnShowListener {
            val density = resources.displayMetrics.density
            val root = this@SymbolDrawerSettingsScreen.rootView
            val widthPx = root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            val heightPx = root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            val window = WindowAdaptiveInfo(widthPx / density, heightPx / density, density)
            val dialogWidth = AdaptiveContentLayoutResolver.resolveDialogWidthPx(
                windowWidthPx = widthPx,
                density = density,
                window = window
            )
            dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        return dialog
    }

    private fun update(transform: SymbolDrawerSettings.() -> SymbolDrawerSettings) {
        runCatching {
            val next = current.transform().copy(themeMode = ThemeMode.SYSTEM)
            // SharedPreferences commit() is synchronous; return only after the
            // local persistent snapshot has been written successfully.
            repository.save(next)
            current = next
            onSettingsChanged?.invoke(next)
        }.onFailure(::reportFailure)
    }

    private fun label(value: String) = TextView(context).apply {
        text = value
        textSize = textSp(14f)
        setTextColor(onSurfaceColor())
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun reportFailure(error: Throwable) {
        onError?.invoke(error)
        toast(error.message ?: context.getString(R.string.symbol_manager_operation_failed))
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    private fun surfaceColor() = drawerTheme?.surface ?: HostThemeColors.surface(context)
    private fun onSurfaceColor() = drawerTheme?.onSurface ?: HostThemeColors.onSurface(context)
    private fun primaryColor() = drawerTheme?.primary ?: HostThemeColors.primary(context)
    private fun outlineColor() = drawerTheme?.outline ?: HostThemeColors.outline(context)

    private fun surfaceBackground(fill: Int, strokeAlpha: Float, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), (HostThemeColors.outline(context) and 0x00ffffff) or
            ((strokeAlpha.coerceIn(0f, 1f) * 255).toInt() shl 24))
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun spaceDp(value: Int) = (value * resources.displayMetrics.density * adaptiveSpacingScale).toInt()
    private fun textSp(value: Float) = value * adaptiveTypographyScale

    private enum class ImportKind { SETTINGS, GROUPS }
}
