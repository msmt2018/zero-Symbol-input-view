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

package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Toast
import android.app.AlertDialog
import android.text.InputType
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaults
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.action.BuiltinSymbolActionOption
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutResolver
import com.zerostudio.idea.widget.editor.symbolinput.v2.SymbolDrawerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerActionExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerExtensionRegistry
import com.google.android.material.tabs.TabLayout
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPage
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation.DrawerPageHost
import com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel.DrawerPanelContent

private const val SETTINGS_PREFERENCE_KEY = "settings_json_v1"

private class SymbolHolder(val root: LinearLayout, val title: TextView, val detail: TextView, val dragHandle: TextView) : RecyclerView.ViewHolder(root)

private data class SymbolActionOption(
    val label: String,
    val legacyId: Int? = null,
    val builtin: BuiltinSymbolActionOption? = null,
    val keepCurrent: Boolean = false,
    val none: Boolean = false,
    val extension: SymbolDrawerActionExtension? = null
) {
    val hasPayload: Boolean
        get() = extension != null || (builtin?.hasPayload ?: (legacyId?.let(LegacySymbolActionCatalog::hasTextPayload) == true))

    val payloadHint: String?
        get() = extension?.payloadHint ?: builtin?.payloadHint
}

/**
 * Caps an editor form to the available app-window height. AlertDialog can then
 * keep its normal wrap-content behavior for short forms while long forms
 * become independently scrollable instead of extending beyond split-screen
 * or compact landscape bounds.
 */
internal class CappedScrollView(
    context: Context,
    private val maxHeightPx: Int
) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentLimit = View.MeasureSpec.getSize(heightMeasureSpec)
        val cap = minOf(maxHeightPx.coerceAtLeast(1), parentLimit.takeIf { it > 0 } ?: maxHeightPx)
        val cappedHeight = View.MeasureSpec.makeMeasureSpec(cap, View.MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, cappedHeight)
    }
}

/**
 * Keeps the editor form strictly inside the app window even when the window is
 * shorter than the normal dialog chrome reservation (compact landscape and
 * heavily constrained split-screen windows).
 */
internal fun resolveEditorFormMaxHeightPx(
    windowHeightPx: Int,
    reservedHeightPx: Int
): Int {
    require(windowHeightPx >= 0) { "windowHeightPx must not be negative" }
    require(reservedHeightPx >= 0) { "reservedHeightPx must not be negative" }
    return (windowHeightPx - reservedHeightPx).coerceAtLeast(1)
}

/**
 * First-party manager screen for groups and symbols. The screen is intentionally
 * a View rather than an Activity so the host controls navigation and window policy.
 */
class SymbolDrawerManagerScreen(
    context: Context,
    private val repository: SymbolGroupRepository = SharedPreferencesSymbolGroupRepository(context),
    private val embeddedInSettings: Boolean = false,
    /** Optional session registry; when supplied, registered app actions appear in the action picker. */
    private val extensionRegistry: SymbolDrawerExtensionRegistry? = null,
    /** Optional live toolbar target; when supplied, manager imports/editing sync automatically. */
    private val liveDrawer: SymbolDrawerView? = null
) : LinearLayout(context) {

    var onGroupsChanged: ((List<SymbolGroup>) -> Unit)? = null
    private var drawerBindingHandle: AutoCloseable? = null
    var onExportBundle: ((String) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private val manager = SymbolGroupManager(repository.also {
        LegacySymbolDataMigration.importIfV2Empty(context, it)
    })
    private val settingsRepository = SharedPreferencesSymbolDrawerSettingsRepository(context)
    private var settings = settingsRepository.load().settings
    private val settingsPreferences = context.applicationContext.getSharedPreferences(
        "symbol_drawer_v2_settings",
        Context.MODE_PRIVATE
    )
    private var settingsListenerRegistered = false
    private val settingsPreferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SETTINGS_PREFERENCE_KEY) {
            settings = settingsRepository.load().settings
            refreshSymbols()
            previewDrawer?.applySettings(settings)
        }
    }
    private val groupTabs = TabLayout(context)
    private val groupPageHost = DrawerPageHost(context)
    private val groupPageContents = LinkedHashMap<String, GroupPageContent>()
    private var searchButton: MaterialButton? = null
    private val searchField = EditText(context)
    private val searchContainer = LinearLayout(context)
    private val preferenceStore = context.getSharedPreferences("zero_symbol_manager_usage_v2", Context.MODE_PRIVATE)
    private val favoriteIds = linkedSetOf<Long>().apply {
        addAll(preferenceStore.getStringSet("favorites", emptySet()).orEmpty().mapNotNull(String::toLongOrNull))
    }
    private val recentIds = ArrayDeque<Long>().apply {
        preferenceStore.getString("recent", "").orEmpty().split(',').mapNotNull(String::toLongOrNull)
            .take(30).forEach(::addLast)
    }
    private var activeFilter = ManagerSymbolFilter.ALL
    private val emptyView = TextView(context)
    private var symbolList: RecyclerView? = null
    /** Inline preview editor lives above the group tabs, never inside the symbol list. */
    private var previewPanel: LinearLayout? = null
    private var previewContent: FrameLayout? = null
    private var previewPopup: PopupWindow? = null
    private var dragPreviewPanel: LinearLayout? = null
    private var previewDrawer: SymbolDrawerView? = null
    private var previewModeActive = false
    private var previewEditSnapshot: List<SymbolGroup>? = null
    private var previewEditSelectedGroupId: String? = null
    private var reorderTouchHelper: ItemTouchHelper? = null
    /** One immutable snapshot for the whole drag gesture; intermediate swaps are not separate undo steps. */
    private var reorderGestureSnapshot: List<SymbolGroup>? = null
    private var reorderGestureGroupId: String? = null
    /** In-memory order while ItemTouchHelper owns the gesture; persisted once on release. */
    private var reorderWorkingGroupId: String? = null
    private var reorderWorkingSymbols: MutableList<SymbolEntry>? = null
    private val symbolAdapter = SymbolAdapter(
        onClick = { symbol -> editSymbol(symbol) },
        onLongClick = { symbol, anchor -> showSymbolMenu(symbol, anchor) },
        onDragHandle = { holder -> reorderTouchHelper?.startDrag(holder) },
        isBatchMode = { batchMode },
        isSelected = { symbol -> symbol.id in selectedSymbolIds },
        onBatchToggle = { symbol -> toggleBatchSelection(symbol) }
    )
    private var selectedGroupId: String? = manager.groups.firstOrNull()?.id
    private val selectedSymbolIds = linkedSetOf<Long>()
    private var batchMode = false
    private val batchBar = LinearLayout(context)
    private val batchSummary = TextView(context)
    private val undoBar = LinearLayout(context)
    private val undoMessage = TextView(context)
    private val undoButton = Button(context)
    private var pendingUndo: (() -> Unit)? = null

    init {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = roundedSurface(surfaceColor(), 18)
        if (manager.groups.isEmpty()) {
            manager.replaceAll(SymbolDrawerDefaults.createGroups())
        }
        buildHeader()
        buildGroupControls()
        buildSearch()
        buildSymbolList()
        addView(
            groupPageHost,
            if (embeddedInSettings) LayoutParams(LayoutParams.MATCH_PARENT, dp(320))
            else LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
        buildUndoBar()
        refresh()
        liveDrawer?.let(::bindToDrawer)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val density = resources.displayMetrics.density
        val window = WindowAdaptiveInfo(w / density, h / density, density)
        val spec = AdaptiveContentLayoutResolver.resolve(window)
        val horizontal = dp(spec.horizontalPaddingDp.toInt())
        val vertical = dp(if (spec.verticalSpacingDp >= 16f) 20 else 12)
        setPadding(horizontal, vertical, horizontal, vertical)

        // Reflow the symbol editor list as cards on expanded windows. The
        // compact/medium path remains the original single-column list.
        (symbolList?.layoutManager as? GridLayoutManager)?.let { grid ->
            if (grid.spanCount != spec.cardColumns) grid.spanCount = spec.cardColumns
        }
        groupTabs.layoutParams?.let { params ->
            val targetHeight = dp(if (spec.cardColumns > 1) 52 else 48)
            if (params.height != targetHeight) {
                params.height = targetHeight
                groupTabs.layoutParams = params
            }
        }
    }

    fun reload() {
        manager.reload()
        selectedGroupId = manager.groups.firstOrNull()?.id
        refresh()
    }

    /**
     * Connects manager transactions to the live toolbar instance.
     *
     * The manager remains the source of durable group data; each committed
     * manager refresh is submitted directly to the existing drawer adapter so
     * imports, edits, deletes and undo do not depend on Activity recreation or
     * a later host-resume callback.
     *
     * Close the returned handle when this manager surface is disposed.
     */
    fun bindToDrawer(drawer: SymbolDrawerView): AutoCloseable {
        drawerBindingHandle?.close()
        drawerBindingHandle = null
        val previousListener = onGroupsChanged
        val previousUsageListener = drawer.onSymbolUsedListener
        val listener: (List<SymbolGroup>) -> Unit = { committedGroups ->
            // The visible toolbar is the primary consumer. An unrelated observer
            // must never prevent the actual drawer from receiving imported data.
            drawer.submitGroups(committedGroups, preserveSelectedGroup = true)
            runCatching { previousListener?.invoke(committedGroups) }
                .onFailure(::reportError)
        }
        val usageListener: (SymbolEntry, Boolean) -> Unit = { symbol, _ ->
            recordRecentlyUsed(symbol.id)
        }
        onGroupsChanged = listener
        drawer.onSymbolUsedListener = usageListener
        drawer.setRepository(repository, reload = false)
        drawer.submitGroups(manager.groups, preserveSelectedGroup = true)
        lateinit var ownerHandle: AutoCloseable
        ownerHandle = AutoCloseable {
            if (onGroupsChanged === listener) onGroupsChanged = previousListener
            if (drawer.onSymbolUsedListener === usageListener) {
                drawer.onSymbolUsedListener = previousUsageListener
            }
            if (drawerBindingHandle === ownerHandle) drawerBindingHandle = null
        }
        drawerBindingHandle = ownerHandle
        return ownerHandle
    }

    fun exportBundle(): String = SymbolGroupBundleCodec.encode(manager.groups)

    /**
     * Exports the legacy compact JSON array used by the V1 symbol-toolbar
     * interchange format (group fields n/d; item fields a/b/c/d/e).
     *
     * This is intended for MT Manager/V1-compatible import-export workflows.
     * V2-only actions that cannot be represented by the legacy numeric action
     * catalog cause an IllegalArgumentException instead of being silently lost.
     */
    fun exportMtManagerJson(): String = LegacySymbolDataCodec.encode(manager.groups)

    /**
     * Imports a legacy compact symbol JSON file. Decoding is strict and validates
     * the complete payload before replacing current data. Existing import undo
     * behaviour is preserved.
     */
    fun importMtManagerJson(json: String) {
        val previous = manager.groups.toList()
        val previousUsage = snapshotUsageState()
        val previousSelection = selectedGroupId
        val imported = LegacySymbolDataCodec.decode(json)
        manager.replaceAll(imported)
        selectedGroupId = imported.firstOrNull()?.id
        refresh()
        showUndo("已导入旧版/MT 管理器符号 JSON") {
            manager.replaceAll(previous)
            restoreUsageState(previousUsage)
            selectedGroupId = previousSelection
        }
    }

    fun importBundle(json: String) {
        val previous = manager.groups.toList()
        val previousUsage = snapshotUsageState()
        val previousSelection = selectedGroupId
        val imported = SymbolGroupBundleCodec.decode(json)
        manager.replaceAll(imported)
        selectedGroupId = imported.firstOrNull()?.id
        refresh()
        showUndo("已导入符号抽屉备份") {
            manager.replaceAll(previous)
            restoreUsageState(previousUsage)
            selectedGroupId = previousSelection
        }
    }

    private fun buildHeader() {
        val row = horizontalRow().apply { gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL }
        val searchButton = MaterialButton(context).apply {
            text = "搜索"
            contentDescription = "搜索符号"
            tag = "symbol_manager_search_button"
            isAllCaps = false
            textSize = 13f
            minWidth = dp(64)
            minHeight = dp(40)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(12)
            icon = context.getDrawable(android.R.drawable.ic_menu_search)
            iconSize = dp(18)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconTint = android.content.res.ColorStateList.valueOf(HostThemeColors.onSurface(context))
            setTextColor(HostThemeColors.onSurface(context))
            backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor())
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(HostThemeColors.outline(context))
            setOnClickListener {
                setSearchVisible(searchContainer.visibility != View.VISIBLE)
            }
        }
        this.searchButton = searchButton
        row.addView(searchButton, LayoutParams(LayoutParams.WRAP_CONTENT, dp(40)))
        val menuButton = MaterialButton(context).apply {
            text = "更多"
            isAllCaps = false
            textSize = 13f
            minWidth = dp(64)
            minHeight = dp(40)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(12)
            icon = context.getDrawable(android.R.drawable.ic_menu_more)
            iconSize = dp(18)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconTint = android.content.res.ColorStateList.valueOf(HostThemeColors.onSurface(context))
            setTextColor(HostThemeColors.onSurface(context))
            backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor())
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(HostThemeColors.outline(context))
            setOnClickListener { showManagerMenu(this) }
        }
        row.addView(menuButton, LayoutParams(LayoutParams.WRAP_CONTENT, dp(40)))
        addView(row, matchWrap())
        buildDragPreviewPanel()
    }

    private fun showManagerMenu(anchor: View) {
        val menu = PopupMenu(context, anchor, android.view.Gravity.END)
        menu.menu.add(if (batchMode) "退出批量选择" else "批量")
            .setOnMenuItemClickListener { setBatchMode(!batchMode); true }
        menu.menu.add("添加符号").setOnMenuItemClickListener { editSymbol(null); true }
        menu.menu.add(if (previewModeActive) "退出预览编辑" else "预览编辑")
            .setOnMenuItemClickListener {
                if (previewModeActive) finishPreviewEditMode() else enterPreviewEditMode()
                true
            }
        menu.menu.add("添加分组").setOnMenuItemClickListener {
            promptGroupName("新建分组") { title ->
                runCatching { selectedGroupId = manager.addGroup(title).id; refresh() }
                    .onFailure(::reportError)
            }
            true
        }
        val filters = menu.menu.addSubMenu("筛选")
        listOf(
            ManagerSymbolFilter.ALL to "全部",
            ManagerSymbolFilter.FAVORITES to "收藏",
            ManagerSymbolFilter.RECENT to "最近"
        ).forEach { (filter, label) ->
            filters.add(if (activeFilter == filter) "✓ $label" else label)
                .setOnMenuItemClickListener { setActiveFilter(filter); true }
        }
        menu.show()
    }

    private fun setActiveFilter(filter: ManagerSymbolFilter) {
        activeFilter = filter
        refreshSymbols()
    }

    private fun buildGroupControls() {
        groupTabs.tabMode = TabLayout.MODE_SCROLLABLE
        groupTabs.isInlineLabel = true
        groupTabs.setSelectedTabIndicatorColor(HostThemeColors.primary(context))
        groupTabs.setTabTextColors(HostThemeColors.secondaryText(context), HostThemeColors.primary(context))
        groupTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val id = tab.tag as? String ?: return
                selectedGroupId = id
                groupPageHost.selectPage(groupPageId(id))
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        groupPageHost.navigationController.addListener { snapshot, _ ->
            snapshot.selectedPageId?.removePrefix(GROUP_PAGE_PREFIX)?.let { id ->
                if (selectedGroupId != id) {
                    selectedGroupId = id
                    refreshSymbols()
                }
            }
            updateGroupNavigationButtons()
        }
        addView(groupTabs, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
    }
    private fun showGroupTabMenu(anchor: View, groupId: String) {
        val group = manager.groups.firstOrNull { it.id == groupId } ?: return
        selectedGroupId = group.id
        val index = manager.groups.indexOfFirst { it.id == group.id }
        groupPageHost.selectPage(groupPageId(group.id))
        PopupMenu(context, anchor).apply {
            menu.add("左移").apply { isEnabled = index > 0 }
                .setOnMenuItemClickListener { moveGroup(group.id, -1); true }
            menu.add("右移").apply { isEnabled = index >= 0 && index < manager.groups.lastIndex }
                .setOnMenuItemClickListener { moveGroup(group.id, 1); true }
            menu.add("添加").setOnMenuItemClickListener { showAddGroupDialog(); true }
            menu.add("复制整个分组").setOnMenuItemClickListener {
                runCatching {
                    val duplicate = manager.duplicateGroup(group.id, index + 1)
                    selectedGroupId = duplicate.id
                    refresh()
                    toast("已复制分组“${duplicate.title}”")
                }.onFailure(::reportError)
                true
            }
            menu.add("复制到…").apply { isEnabled = manager.groups.size > 1 }
                .setOnMenuItemClickListener { promptCopyGroupTo(group.id); true }
            menu.add("重命名").setOnMenuItemClickListener {
                promptGroupName("重命名“${group.title}”", group.title) {
                    manager.renameGroup(group.id, it)
                    refresh()
                }
                true
            }
            menu.add("删除").setOnMenuItemClickListener { confirmDeleteGroup(group.id); true }
            show()
        }
    }

    private fun showAddGroupDialog() {
        promptGroupName("新建分组") {
            val created = manager.addGroup(it)
            selectedGroupId = created.id
            refresh()
        }
    }

    private fun moveGroup(groupId: String, delta: Int) {
        val index = manager.groups.indexOfFirst { it.id == groupId }
        val target = index + delta
        if (index >= 0 && target in manager.groups.indices) {
            manager.moveGroup(index, target)
            refresh()
            groupPageHost.selectPage(groupPageId(groupId))
        }
    }

    private fun promptCopyGroupTo(sourceGroupId: String) {
        val source = manager.groups.firstOrNull { it.id == sourceGroupId } ?: return
        val targets = manager.groups.filter { it.id != sourceGroupId }
        if (targets.isEmpty()) return
        AlertDialog.Builder(context)
            .setTitle("复制“${source.title}”中的符号到…")
            .setItems(targets.map { it.title }.toTypedArray()) { _, which ->
                runCatching {
                    val target = targets[which]
                    val ids = source.symbols.map { it.id }.toSet()
                    if (ids.isNotEmpty()) manager.copySymbols(source.id, ids, target.id)
                    selectedGroupId = target.id
                    refresh()
                    toast("已复制到“${target.title}”")
                }.onFailure(::reportError)
            }
            .setNegativeButton("取消", null)
            .showAdaptiveDialog()
    }

    private fun setSearchVisible(show: Boolean) {
        searchContainer.visibility = if (show) View.VISIBLE else View.GONE
        searchButton?.text = if (show) "收起" else "搜索"
        searchButton?.contentDescription = if (show) "收起搜索" else "搜索符号"
        if (show) {
            searchField.requestFocus()
        } else {
            searchField.text?.clear()
            searchField.clearFocus()
        }
    }

    private fun buildSearch() {
        searchContainer.tag = "symbol_manager_search_container"
        searchContainer.orientation = VERTICAL
        searchContainer.visibility = View.GONE
        searchField.hint = "搜索符号名称或动作"
        searchField.setSingleLine(true)
        searchField.textSize = 14f
        searchField.setPadding(dp(14), dp(10), dp(14), dp(10))
        searchField.background = roundedSurface(surfaceColor(), 14)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshSymbols()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        searchContainer.addView(searchField, matchWrap())
        addView(searchContainer, matchWrap())
    }

    private data class UsageState(
        val favorites: Set<Long>,
        val recent: List<Long>
    )

    private fun snapshotUsageState() = UsageState(favoriteIds.toSet(), recentIds.toList())

    private fun restoreUsageState(state: UsageState) {
        favoriteIds.clear()
        favoriteIds.addAll(state.favorites)
        recentIds.clear()
        state.recent.take(30).forEach(recentIds::addLast)
        persistUsageState()
    }

    private fun pruneUsageState() {
        val liveIds = manager.groups.asSequence()
            .flatMap { it.symbols.asSequence() }
            .mapTo(mutableSetOf()) { it.id }
        val retainedFavorites = favoriteIds.filterTo(linkedSetOf()) { it in liveIds }
        val retainedRecent = recentIds.filter { it in liveIds }
        if (retainedFavorites != favoriteIds || retainedRecent != recentIds.toList()) {
            favoriteIds.clear()
            favoriteIds.addAll(retainedFavorites)
            recentIds.clear()
            retainedRecent.forEach(recentIds::addLast)
            persistUsageState()
        }
    }

    private fun persistUsageState() {
        preferenceStore.edit()
            .putStringSet("favorites", favoriteIds.map(Long::toString).toSet())
            .putString("recent", recentIds.joinToString(","))
            .apply()
    }

    private fun toggleFavorite(symbol: SymbolEntry) {
        // Usage-state edits are newer user mutations; an older undo snapshot
        // must not restore stale favorites over this change.
        invalidateUndo()
        if (!favoriteIds.add(symbol.id)) favoriteIds.remove(symbol.id)
        persistUsageState()
        refreshSymbols()
        toast(if (symbol.id in favoriteIds) "已加入收藏" else "已取消收藏")
    }

    /** Records a real toolbar dispatch; unknown/stale IDs are ignored safely. */
    fun recordRecentlyUsed(symbolId: Long) {
        val symbol = manager.groups.asSequence()
            .flatMap { it.symbols.asSequence() }
            .firstOrNull { it.id == symbolId } ?: return
        markRecentlyUsed(symbol)
    }

    private fun markRecentlyUsed(symbol: SymbolEntry) {
        // A later symbol dispatch changes durable usage state, so discard any
        // earlier undo snapshot that captured an obsolete favorites/recent set.
        invalidateUndo()
        recentIds.remove(symbol.id)
        recentIds.addFirst(symbol.id)
        while (recentIds.size > 30) recentIds.removeLast()
        persistUsageState()
        if (activeFilter == ManagerSymbolFilter.RECENT) refreshSymbols()
    }

    private fun buildSymbolList() {
        emptyView.text = "当前分组暂无符号，点击“添加”并选择“新建符号”"
        emptyView.textSize = 13f
        emptyView.setTextColor(secondaryTextColor())
        emptyView.alpha = 0.75f
        emptyView.setPadding(dp(8), dp(16), dp(8), dp(16))
        val list = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 1)
            adapter = symbolAdapter
            itemAnimator = null
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
        symbolList = list
        if (embeddedInSettings) list.isNestedScrollingEnabled = false
        installSymbolReordering(list)
        buildBatchBar()
    }

    private fun buildBatchBar() {
        batchBar.orientation = HORIZONTAL
        batchBar.gravity = android.view.Gravity.CENTER_VERTICAL
        batchBar.visibility = View.GONE
        batchBar.setPadding(dp(4), dp(4), dp(4), dp(4))
        batchSummary.setTextColor(HostThemeColors.secondaryText(context))
        batchSummary.textSize = 12f
        batchBar.addView(batchSummary, LayoutParams(0, dp(40), 1f))
        batchBar.addView(actionButton("复制") { batchCopy() }, LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)))
        batchBar.addView(actionButton("移动") { batchMove() }, LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)))
        batchBar.addView(actionButton("删除") { batchDelete() }, LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)))
    }

    private fun setBatchMode(enabled: Boolean) {
        batchMode = enabled
        selectedSymbolIds.clear()
        batchBar.visibility = if (enabled) View.VISIBLE else View.GONE
        updateBatchSummary()
        symbolAdapter.notifyDataSetChanged()
    }

    private fun updateBatchSummary() {
        batchSummary.text = "已选择 ${selectedSymbolIds.size} 项"
    }

    private fun toggleBatchSelection(symbol: SymbolEntry) {
        if (!batchMode) return
        if (!selectedSymbolIds.add(symbol.id)) selectedSymbolIds.remove(symbol.id)
        updateBatchSummary()
        symbolAdapter.notifyDataSetChanged()
    }

    private fun selectedBatch(): Pair<SymbolGroup, Set<Long>>? {
        val group = currentGroup() ?: return null
        val ids = selectedSymbolIds.toSet()
        if (ids.isEmpty()) {
            toast("请先选择符号")
            return null
        }
        return group to ids
    }

    private fun batchDelete() {
        val (group, ids) = selectedBatch() ?: return
        val before = manager.groups.toList()
        val previousUsage = snapshotUsageState()
        runCatching {
            manager.removeSymbols(group.id, ids)
            pruneUsageState()
            selectedSymbolIds.clear()
            updateBatchSummary()
            refresh()
            showUndo("已删除 ${ids.size} 个符号") {
                manager.replaceAll(before)
                restoreUsageState(previousUsage)
            }
        }.onFailure(::reportError)
    }

    private fun batchCopy() {
        val (group, ids) = selectedBatch() ?: return
        val targets = manager.groups.filter { it.id != group.id }
        if (targets.isEmpty()) { toast("请先创建另一个分组"); return }
        AlertDialog.Builder(context)
            .setTitle("复制所选符号到")
            .setItems(targets.map { it.title }.toTypedArray()) { _, which ->
                val before = manager.groups.toList()
                runCatching {
                    val target = targets[which]
                    manager.copySymbols(group.id, ids, target.id)
                    selectedSymbolIds.clear()
                    refresh()
                    showUndo("已复制 ${ids.size} 个符号") { manager.replaceAll(before) }
                }.onFailure(::reportError)
            }
            .setNegativeButton("取消", null)
            .showAdaptiveDialog()
    }

    private fun batchMove() {
        val (group, ids) = selectedBatch() ?: return
        val targets = manager.groups.filter { it.id != group.id }
        if (targets.isEmpty()) { toast("请先创建另一个分组"); return }
        AlertDialog.Builder(context)
            .setTitle("移动所选符号到")
            .setItems(targets.map { it.title }.toTypedArray()) { _, which ->
                val before = manager.groups.toList()
                runCatching {
                    val target = targets[which]
                    manager.moveSymbolsToGroup(group.id, ids, target.id)
                    selectedGroupId = target.id
                    selectedSymbolIds.clear()
                    refresh()
                    showUndo("已移动 ${ids.size} 个符号") { manager.replaceAll(before) }
                }.onFailure(::reportError)
            }
            .setNegativeButton("取消", null)
            .showAdaptiveDialog()
    }

    /**
     * Reordering uses the same repository-backed manager transaction as the
     * explicit move commands. The preview is a fresh SymbolDrawerView instance,
     * not the Activity's live toolbar; it is detached and released as soon as
     * the drag gesture ends.
     */
    private fun installSymbolReordering(list: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (searchField.text?.isNotBlank() == true) return false
                val group = currentGroup() ?: return false
                if (reorderWorkingGroupId != group.id) return false
                val items = reorderWorkingSymbols ?: group.symbols.toMutableList().also {
                    reorderWorkingSymbols = it
                    reorderWorkingGroupId = group.id
                }
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION ||
                    from !in items.indices || to !in items.indices || from == to
                ) return false
                val moved = items.removeAt(from)
                items.add(to, moved)
                symbolAdapter.submitList(items.toList())
                updateDragPreview()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Capture once per gesture, not once per onMove callback. ItemTouchHelper
                    // may cross many rows and invoke onMove repeatedly during one drag.
                    if (reorderGestureSnapshot == null) {
                        reorderGestureSnapshot = manager.groups.map { group ->
                            group.copy(symbols = group.symbols.toList())
                        }
                        reorderGestureGroupId = selectedGroupId
                        reorderWorkingGroupId = selectedGroupId
                        reorderWorkingSymbols = currentGroup()?.symbols?.toMutableList()
                    }
                    showDragPreview()
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val before = reorderGestureSnapshot
                val previousGroupId = reorderGestureGroupId
                val changedGroupId = reorderWorkingGroupId
                val orderedIds = reorderWorkingSymbols?.map { it.id }
                reorderGestureSnapshot = null
                reorderGestureGroupId = null
                reorderWorkingGroupId = null
                reorderWorkingSymbols = null
                runCatching {
                    // ItemTouchHelper can report many swaps during one physical drag.
                    // Keep those swaps transient and write one repository transaction
                    // only after the gesture has completed.
                    if (before != null && changedGroupId != null && orderedIds != null) {
                        val previousOrder = before.firstOrNull { it.id == changedGroupId }
                            ?.symbols?.map { it.id }
                        if (previousOrder != null && previousOrder != orderedIds) {
                            manager.reorderSymbols(changedGroupId, orderedIds)
                        }
                    }
                    refresh()
                    if (before != null && before != manager.groups) {
                        showUndo("已调整符号顺序") {
                            manager.replaceAll(before.map { it.copy(symbols = it.symbols.toList()) })
                            selectedGroupId = previousGroupId
                        }
                    }
                }.onFailure {
                    refresh()
                    reportError(it)
                }
                destroyDragPreview()
            }

            override fun isLongPressDragEnabled(): Boolean = false
        }
        reorderTouchHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(list) }
    }

    private fun buildDragPreviewPanel() {
        val panel = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setPadding(dp(8), dp(6), dp(8), dp(8))
            background = roundedSurface(surfaceColor(), 14)
        }
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        controls.addView(TextView(context).apply {
            text = "工具栏预览编辑"
            textSize = 13f
            setTextColor(HostThemeColors.onSurface(context))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LayoutParams(0, dp(36), 1f))
        controls.addView(actionButton("恢复") { restorePreviewEditMode() },
            LayoutParams(LayoutParams.WRAP_CONTENT, dp(34)))
        controls.addView(actionButton("完成") { finishPreviewEditMode() },
            LayoutParams(LayoutParams.WRAP_CONTENT, dp(34)))
        panel.addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, dp(36)))
        previewContent = FrameLayout(context).also {
            panel.addView(it, LayoutParams(LayoutParams.MATCH_PARENT, dp(144)))
        }
        previewPanel = panel
        // This is deliberately outside DrawerPageHost and RecyclerView. Toggling
        // preview mode changes only the manager's available content height.
        addView(panel, matchWrap())
    }

    private fun createPreviewDrawer(): SymbolDrawerView = SymbolDrawerView(context).apply {
        setRepository(repository, reload = false)
        submitGroups(manager.groups, preserveSelectedGroup = true)
        manager.groups.indexOfFirst { it.id == selectedGroupId }
            .takeIf { it >= 0 }?.let { selectGroup(it, false) }
        onSymbolActionListener = null
        onSymbolUsedListener = null
        onOpenManagerListener = null
        onOpenSettingsListener = null
        onGroupChangedListener = null
    }

    private fun enterPreviewEditMode() {
        if (previewModeActive) return
        destroyDragPreview()
        previewEditSnapshot = manager.groups.map { it.copy(symbols = it.symbols.toList()) }
        previewEditSelectedGroupId = selectedGroupId
        previewModeActive = true
        previewPanel?.visibility = View.VISIBLE
        val preview = createPreviewDrawer()
        previewDrawer = preview
        previewContent?.addView(
            preview,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        preview.post { preview.setExpanded(true, false) }
        requestLayout()
    }

    private fun restorePreviewEditMode() {
        val snapshot = previewEditSnapshot ?: return
        runCatching {
            manager.replaceAll(snapshot.map { it.copy(symbols = it.symbols.toList()) })
            selectedGroupId = previewEditSelectedGroupId
            refresh()
            toast("已恢复进入预览编辑前的布局")
        }.onFailure(::reportError)
    }

    private fun finishPreviewEditMode() {
        if (!previewModeActive) return
        previewModeActive = false
        previewEditSnapshot = null
        previewEditSelectedGroupId = null
        previewPanel?.visibility = View.GONE
        releasePreviewDrawer()
        requestLayout()
    }

    private fun releasePreviewDrawer() {
        val preview = previewDrawer ?: return
        preview.onSymbolActionListener = null
        preview.onSymbolUsedListener = null
        preview.onOpenManagerListener = null
        preview.onOpenSettingsListener = null
        preview.onGroupChangedListener = null
        (preview.parent as? ViewGroup)?.removeView(preview)
        previewDrawer = null
    }

    private fun showDragPreview() {
        if (previewModeActive || previewDrawer != null) return
        val panel = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
            background = roundedSurface(surfaceColor(), 14)
            addView(TextView(context).apply {
                text = "实时工具栏预览 · 拖动结束自动关闭"
                textSize = 11f
                setTextColor(secondaryTextColor())
            }, LayoutParams(LayoutParams.MATCH_PARENT, dp(22)))
        }
        dragPreviewPanel = panel
        val preview = createPreviewDrawer()
        previewDrawer = preview
        panel.addView(preview, LayoutParams(LayoutParams.MATCH_PARENT, dp(140)))
        val popupWidth = dp(320).coerceAtMost(
            (resources.displayMetrics.widthPixels - dp(24)).coerceAtLeast(dp(180))
        )
        val popupHeight = dp(174)
        val popup = PopupWindow(panel, popupWidth, popupHeight, false).apply {
            // The preview is visual feedback only. It must never become the
            // touch target while ItemTouchHelper owns the symbol drag stream.
            isTouchable = false
            isFocusable = false
            isOutsideTouchable = false
            elevation = dp(12).toFloat()
        }
        previewPopup = popup

        // Anchor the preview to the symbol list instead of a fixed screen Y.
        // Its bottom edge overlaps roughly 5% of the list viewport, keeping
        // the preview higher without covering the active drag area.
        val rootLocation = IntArray(2)
        val listLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)
        val list = symbolList
        val overlapPx = ((list?.height ?: 0) * 0.05f).toInt()
        val listTopInRoot = if (list != null && list.height > 0) {
            list.getLocationOnScreen(listLocation)
            listLocation[1] - rootLocation[1]
        } else dp(96)
        val previewTop = (listTopInRoot - popupHeight + overlapPx).coerceAtLeast(0)
        popup.showAtLocation(rootView, android.view.Gravity.TOP or android.view.Gravity.END, dp(12), previewTop)
        preview.post { preview.setExpanded(true, false) }
    }

    private fun updateDragPreview() {
        previewDrawer?.let { preview ->
            val workingGroupId = reorderWorkingGroupId
            val workingSymbols = reorderWorkingSymbols
            val previewGroups = if (workingGroupId != null && workingSymbols != null) {
                manager.groups.map { group ->
                    if (group.id == workingGroupId) group.copy(symbols = workingSymbols.toList()) else group
                }
            } else manager.groups
            preview.submitGroups(previewGroups, preserveSelectedGroup = true)
            previewGroups.indexOfFirst { it.id == selectedGroupId }
                .takeIf { it >= 0 }?.let { preview.selectGroup(it, false) }
        }
    }

    private fun destroyDragPreview() {
        // A persistent inline preview belongs to preview-edit mode and must
        // survive ItemTouchHelper.clearView after each reorder operation.
        if (previewModeActive) return
        releasePreviewDrawer()
        previewPopup?.dismiss()
        previewPopup = null
        dragPreviewPanel = null
    }

    private fun buildUndoBar() {
        undoBar.orientation = HORIZONTAL
        undoBar.gravity = android.view.Gravity.CENTER_VERTICAL
        undoBar.setPadding(dp(8), dp(4), dp(4), dp(4))
        undoBar.visibility = View.GONE
        undoMessage.textSize = 13f
        undoBar.addView(undoMessage, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        undoButton.text = "撤销"
        undoButton.setOnClickListener {
            val restore = pendingUndo ?: return@setOnClickListener
            pendingUndo = null
            undoBar.visibility = View.GONE
            runCatching {
                restore()
                refresh()
            }.onFailure(::reportError)
        }
        undoBar.addView(undoButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(undoBar, matchWrap())
    }

    private fun refresh() {
        // Undo snapshots are valid only until another manager mutation or reload.
        // Clearing them here prevents an old delete/import snapshot from replacing
        // newer edits when the user later presses Undo.
        invalidateUndo()
        // Reconcile usage references after imports, reloads and restores too.
        pruneUsageState()
        val groups = manager.groups
        val selectedIndex = groups.indexOfFirst { it.id == selectedGroupId }.takeIf { it >= 0 } ?: 0
        selectedGroupId = groups.getOrNull(selectedIndex)?.id
        val activeIds = groups.mapTo(mutableSetOf()) { it.id }
        groupPageContents.keys.retainAll(activeIds)
        val pages = groups.map { group ->
            val content = groupPageContents.getOrPut(group.id) { GroupPageContent(group.id) }
            DrawerPage(groupPageId(group.id), group.title, content)
        }
        groupPageHost.setPages(pages, preserveSelection = true)
        if (selectedGroupId != null) groupPageHost.selectPage(groupPageId(selectedGroupId!!))
        updateGroupNavigationButtons()
        searchField.isEnabled = groups.isNotEmpty()
        refreshSymbols()
        // Keep the inline toolbar preview in lockstep with every committed
        // manager mutation, not only drag-reorder operations.
        updateDragPreview()
        runCatching { onGroupsChanged?.invoke(groups) }
            .onFailure(::reportError)
    }

    private fun groupPageId(groupId: String) = GROUP_PAGE_PREFIX + groupId

    private fun updateGroupNavigationButtons() {
        val existing = (0 until groupTabs.tabCount).mapNotNull { groupTabs.getTabAt(it)?.tag as? String }
        val next = manager.groups.map { it.id }
        if (existing != next) {
            groupTabs.removeAllTabs()
            manager.groups.forEach { group ->
                val tab = groupTabs.newTab()
                tab.text = group.title
                tab.tag = group.id
                groupTabs.addTab(tab, false)
            }
        } else {
            manager.groups.forEachIndexed { index, group -> groupTabs.getTabAt(index)?.text = group.title }
        }
        val index = manager.groups.indexOfFirst { it.id == selectedGroupId }
        if (index >= 0 && groupTabs.selectedTabPosition != index) groupTabs.getTabAt(index)?.select()
        val tabStrip = groupTabs.getChildAt(0) as? ViewGroup
        if (tabStrip != null) {
            for (tabIndex in 0 until minOf(groupTabs.tabCount, tabStrip.childCount)) {
                val tabView = tabStrip.getChildAt(tabIndex)
                val groupId = groupTabs.getTabAt(tabIndex)?.tag as? String ?: continue
                tabView.setOnLongClickListener {
                    showGroupTabMenu(it, groupId)
                    true
                }
            }
        }
    }

    private inner class GroupPageContent(
        private val groupId: String
    ) : DrawerPanelContent {
        private var page: LinearLayout? = null

        override fun createView(): View = LinearLayout(context).apply {
            orientation = VERTICAL
            page = this
        }

        override fun onPanelVisible() {
            selectedGroupId = groupId
            refreshSymbols()
            val container = page ?: return
            detachManagerContent()
            container.addView(batchBar, matchWrap())
            container.addView(emptyView, matchWrap())
            val list = symbolList ?: return
            // The page host already owns the embedded height. Let the list
            // consume only the remainder after the batch and empty-state rows,
            // otherwise a second fixed 320dp height makes the page overflow.
            container.addView(list, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }

        override fun onPanelHidden() {
            detachManagerContent()
        }

        override fun onPanelDetached() {
            detachManagerContent()
            page = null
        }

        private fun detachManagerContent() {
            (batchBar.parent as? ViewGroup)?.removeView(batchBar)
            (emptyView.parent as? ViewGroup)?.removeView(emptyView)
            (symbolList?.parent as? ViewGroup)?.removeView(symbolList)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (drawerBindingHandle == null) liveDrawer?.let(::bindToDrawer)
        if (!settingsListenerRegistered) {
            settingsPreferences.registerOnSharedPreferenceChangeListener(settingsPreferenceListener)
            settingsListenerRegistered = true
        }
        // Settings may have changed while this manager was detached.
        settings = settingsRepository.load().settings
        refreshSymbols()
        previewDrawer?.applySettings(settings)
        groupPageHost.setPanelVisible(true)
        if (reorderTouchHelper == null) symbolList?.let(::installSymbolReordering)
    }

    override fun onDetachedFromWindow() {
        groupPageHost.setPanelVisible(false)
        if (settingsListenerRegistered) {
            settingsPreferences.unregisterOnSharedPreferenceChangeListener(settingsPreferenceListener)
            settingsListenerRegistered = false
        }
        drawerBindingHandle?.close()
        drawerBindingHandle = null
        if (previewModeActive) finishPreviewEditMode()
        destroyDragPreview()
        // A detached manager must not retain an in-flight gesture snapshot that
        // could be mistaken for a later drag after the view is reattached.
        reorderGestureSnapshot = null
        reorderGestureGroupId = null
        reorderWorkingGroupId = null
        reorderWorkingSymbols = null
        reorderTouchHelper?.attachToRecyclerView(null)
        reorderTouchHelper = null
        super.onDetachedFromWindow()
    }

    private fun refreshSymbols() {
        val group = currentGroup()
        val query = searchField.text?.toString()?.trim().orEmpty()
        val source = group?.symbols.orEmpty()
        val filtered = source.filter { symbol ->
            val matchesQuery = query.isEmpty() ||
                symbol.label.contains(query, ignoreCase = true) ||
                (settings.searchIncludesActionPayload && (
                    symbol.shortAction.toString().contains(query, ignoreCase = true) ||
                        symbol.longAction?.let { it.toString().contains(query, ignoreCase = true) } == true
                    ))
            val matchesFilter = when (activeFilter) {
                ManagerSymbolFilter.ALL -> true
                ManagerSymbolFilter.FAVORITES -> symbol.id in favoriteIds
                ManagerSymbolFilter.RECENT -> symbol.id in recentIds
            }
            matchesQuery && matchesFilter
        }
        val symbols = if (activeFilter == ManagerSymbolFilter.RECENT) {
            val order = recentIds.withIndex().associate { it.value to it.index }
            filtered.sortedBy { order[it.id] ?: Int.MAX_VALUE }
        } else filtered
        symbolAdapter.submitList(symbols)
        emptyView.visibility = if (symbols.isEmpty()) View.VISIBLE else View.GONE
        selectedSymbolIds.retainAll(group?.symbols?.mapTo(mutableSetOf()) { it.id }.orEmpty())
        updateBatchSummary()
    }

    private fun currentGroup(): SymbolGroup? =
        selectedGroupId?.let { id -> manager.groups.firstOrNull { it.id == id } }

    private fun moveCurrentGroup(delta: Int) {
        val index = manager.groups.indexOfFirst { it.id == selectedGroupId }
        val target = index + delta
        if (index >= 0 && target in manager.groups.indices) {
            manager.moveGroup(index, target)
            refresh()
        }
    }

    private fun confirmDeleteGroup(groupId: String) {
        val group = manager.groups.firstOrNull { it.id == groupId } ?: return
        AlertDialog.Builder(context)
            .setTitle("删除分组")
            .setMessage("删除“${group.title}”及其中全部符号？删除后可使用页面底部的“撤销”恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                val previous = manager.groups.toList()
                val previousUsage = snapshotUsageState()
                val previousSelection = selectedGroupId
                runCatching {
                    manager.removeGroup(group.id)
                    pruneUsageState()
                    selectedGroupId = manager.groups.firstOrNull()?.id
                    refresh()
                    showUndo("已删除分组“${group.title}”") {
                        manager.replaceAll(previous)
                        restoreUsageState(previousUsage)
                        selectedGroupId = previousSelection
                    }
                }.onFailure(::reportError)
            }
            .showAdaptiveDialog()
    }

    private fun promptGroupName(title: String, initial: String = "", done: (String) -> Unit) {
        val input = EditText(context).apply {
            setSingleLine(true)
            setText(initial)
            hint = "分组名称"
        }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) runCatching { done(value) }.onFailure(::reportError)
                else toast("分组名称不能为空")
            }
            .showAdaptiveDialog()
    }

    private fun editSymbol(existing: SymbolEntry?) {
        val group = currentGroup() ?: return
        val label = EditText(context).apply {
            hint = "显示文本"
            setSingleLine(true)
            setText(existing?.label.orEmpty())
        }
        val shortLegacyIndex = LegacyActionPickerResolver.legacyActionIndex(
            existing?.shortAction,
            existing?.legacyShortActionId,
            existing?.legacyShortText
        )
        val preserveShortAction = existing?.shortAction != null && shortLegacyIndex == null
        val longLegacyIndex = existing?.longAction?.let { action ->
            LegacyActionPickerResolver.legacyActionIndex(
                action,
                existing.legacyLongActionId,
                existing.legacyLongText
            )
        }
        val preserveLongAction = existing?.longAction != null && longLegacyIndex == null

        val legacyOptions = LegacySymbolActionCatalog.values.mapIndexed { index, id ->
            SymbolActionOption("[V1] " + LegacySymbolActionCatalog.labels[index], legacyId = id)
        }
        val builtinOptions = BuiltinSymbolActionCatalog.options.map { option ->
            SymbolActionOption("[V2] " + option.label, builtin = option)
        }
        val extensionOptions = extensionRegistry?.actions().orEmpty().map { extension ->
            SymbolActionOption("[扩展] " + extension.title, extension = extension)
        }
        val shortOptions = buildList {
            if (preserveShortAction) add(SymbolActionOption("保留当前动作", keepCurrent = true))
            addAll(legacyOptions)
            addAll(builtinOptions)
            addAll(extensionOptions)
        }
        val shortOffset = if (preserveShortAction) 1 else 0
        val shortIndex = if (preserveShortAction) 0 else (shortLegacyIndex ?: 0) + shortOffset

        val longOptions = buildList {
            if (preserveLongAction) add(SymbolActionOption("保留当前 V2 动作", keepCurrent = true))
            add(SymbolActionOption("无长按动作", none = true))
            addAll(legacyOptions)
            addAll(builtinOptions)
            addAll(extensionOptions)
        }
        val longOffset = if (preserveLongAction) 2 else 1
        val longIndex = when {
            preserveLongAction -> 0
            existing?.longAction == null -> 0
            else -> (longLegacyIndex ?: 0) + longOffset
        }

        val shortAction = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                shortOptions.map(SymbolActionOption::label)
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(shortIndex.coerceIn(shortOptions.indices), false)
        }
        val longAction = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                longOptions.map(SymbolActionOption::label)
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(longIndex.coerceIn(longOptions.indices), false)
        }
        val shortText = EditText(context).apply {
            hint = shortOptions.getOrNull(shortIndex)?.payloadHint
                ?: "动作参数文本（插入文本/替换行等动作使用）"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(
                existing?.legacyShortText?.takeIf { shortLegacyIndex != null }
                    ?: (existing?.shortAction as? SymbolAction.InsertText)?.template
                    ?: (existing?.shortAction as? SymbolAction.EditorCommand)?.payload.orEmpty()
            )
            isEnabled = shortOptions.getOrNull(shortIndex)?.hasPayload == true
        }
        val longText = EditText(context).apply {
            hint = longOptions.getOrNull(longIndex)?.payloadHint
                ?: "动作参数文本（插入文本/替换行等动作使用）"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(
                existing?.legacyLongText?.takeIf { longLegacyIndex != null }
                    ?: (existing?.longAction as? SymbolAction.InsertText)?.template
                    ?: (existing?.longAction as? SymbolAction.EditorCommand)?.payload.orEmpty()
            )
            isEnabled = longOptions.getOrNull(longIndex)?.hasPayload == true
        }
        shortAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { shortText.isEnabled = false }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = shortOptions.getOrNull(position)
                shortText.isEnabled = option?.hasPayload == true
                option?.payloadHint?.let { shortText.hint = it }
            }
        }
        longAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { longText.isEnabled = false }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = longOptions.getOrNull(position)
                longText.isEnabled = option?.hasPayload == true
                option?.payloadHint?.let { longText.hint = it }
            }
        }
        val form = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(label, matchWrap())
            addView(TextView(context).apply { text = "短按动作" }, matchWrap())
            addView(shortAction, matchWrap())
            addView(shortText, matchWrap())
            addView(TextView(context).apply { text = "长按动作（可选）" }, matchWrap())
            addView(longAction, matchWrap())
            addView(longText, matchWrap())
            addView(TextView(context).apply {
                text = listOf(
                    "\$\$  输出字面量 \$",
                    "\$S   设置选区起始位置",
                    "\$E   设置选区结束位置",
                    "\$T   插入当前选中文本",
                    "\$C   设置光标位置（与选区标记分开使用）",
                    "\$N   插入当前文档的换行符",
                    "\$TAB 插入一个制表符",
                    "\$L   插入光标所在行的文本",
                    "\$W   插入光标所在单词",
                    "\$I   插入光标所在行的缩进",
                    "\$U   将当前选中文本转换为大写",
                    "\$V   将当前选中文本转换为小写",
                    "\$R   反转当前选中文本",
                    "\$UP  将当前选中文本转换为大写（长指令）",
                    "\$LOW 将当前选中文本转换为小写（长指令）",
                    "\$CAP 将当前选中文本首字母大写",
                    "\$TRIM 删除当前选中文本首尾空白",
                    "\$REV 反转当前选中文本（长指令）"
                ).joinToString("\n")
                textSize = 12f
                alpha = 0.7f
            }, matchWrap())
        }
        val windowHeight = rootView.height.takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val formScroll = CappedScrollView(
            context,
            resolveEditorFormMaxHeightPx(windowHeight, dp(96))
        ).apply {
            isFillViewport = false
            addView(form, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "新建符号" else "编辑符号")
            .setView(formScroll)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                runCatching {
                    val display = label.text.toString()
                    require(display.isNotEmpty()) { "符号显示文本不能为空" }
                    val shortOption = shortOptions.getOrNull(shortAction.selectedItemPosition)
                        ?: error("请选择有效的短按动作")
                    val keepShort = shortOption.keepCurrent
                    val shortId = if (keepShort) existing?.legacyShortActionId else shortOption.legacyId
                    val shortPayload = if (shortOption.hasPayload) {
                        shortText.text.toString().takeIf { it.isNotEmpty() }
                    } else null
                    val shortTypedAction: SymbolAction = when {
                        keepShort -> existing!!.shortAction
                        shortOption.builtin != null ->
                            SymbolAction.EditorCommand(shortOption.builtin.commandId, shortPayload)
                        shortOption.extension != null ->
                            SymbolAction.Extension(shortOption.extension.id, shortPayload)
                        shortOption.legacyId != null ->
                            LegacySymbolActionAdapter.convert(LegacySymbolAction(shortOption.legacyId, shortPayload))
                        else -> SymbolAction.InsertText("")
                    }
                    val shortLegacyText = when {
                        keepShort -> existing?.legacyShortText
                        shortId?.let(LegacySymbolActionCatalog::hasTextPayload) == true -> shortPayload
                        else -> null
                    }

                    val longOption = longOptions.getOrNull(longAction.selectedItemPosition)
                        ?: error("请选择有效的长按动作")
                    val keepLong = longOption.keepCurrent
                    val longId = if (keepLong) existing?.legacyLongActionId else longOption.legacyId
                    val longPayload = if (longOption.hasPayload) {
                        longText.text.toString().takeIf { it.isNotEmpty() }
                    } else null
                    val longTypedAction = when {
                        keepLong -> existing?.longAction
                        longOption.none -> null
                        longOption.builtin != null ->
                            SymbolAction.EditorCommand(longOption.builtin.commandId, longPayload)
                        longOption.extension != null ->
                            SymbolAction.Extension(longOption.extension.id, longPayload)
                        longOption.legacyId != null ->
                            LegacySymbolActionAdapter.convert(LegacySymbolAction(longOption.legacyId, longPayload))
                        else -> null
                    }
                    val longLegacyText = when {
                        keepLong -> existing?.legacyLongText
                        longId?.let(LegacySymbolActionCatalog::hasTextPayload) == true -> longPayload
                        else -> null
                    }
                    val symbol = SymbolEntry(
                        id = existing?.id ?: 0L,
                        label = display,
                        shortAction = shortTypedAction,
                        longAction = longTypedAction,
                        legacyShortActionId = shortId,
                        legacyShortText = shortLegacyText,
                        legacyLongActionId = longId,
                        legacyLongText = longLegacyText,
                        icon = existing?.icon
                    )
                    if (existing == null) manager.addSymbol(group.id, symbol)
                    else manager.updateSymbol(group.id, symbol)
                    refresh()
                }.onFailure(::reportError)
            }
            .showAdaptiveDialog()
    }


    private fun showSymbolMenu(symbol: SymbolEntry, anchor: View) {
        val menu = PopupMenu(context, anchor)
        menu.menu.add(if (symbol.id in favoriteIds) "取消收藏" else "加入收藏")
            .setOnMenuItemClickListener { toggleFavorite(symbol); true }
        menu.menu.add("标记为最近使用").setOnMenuItemClickListener {
            markRecentlyUsed(symbol)
            refreshSymbols()
            true
        }
        menu.menu.add("编辑").setOnMenuItemClickListener { editSymbol(symbol); true }
        menu.menu.add("复制到本分组").setOnMenuItemClickListener {
            currentGroup()?.let { manager.copySymbol(it.id, symbol.id, it.id); refresh() }
            true
        }
        menu.menu.add("移动到分组").setOnMenuItemClickListener { promptMoveSymbol(symbol); true }
        menu.menu.add("上移").setOnMenuItemClickListener { moveSymbol(symbol, -1); true }
        menu.menu.add("下移").setOnMenuItemClickListener { moveSymbol(symbol, 1); true }
        menu.menu.add("删除").setOnMenuItemClickListener {
            confirmDeleteSymbol(symbol)
            true
        }
        menu.show()
    }

    private fun confirmDeleteSymbol(symbol: SymbolEntry) {
        val group = currentGroup() ?: return
        AlertDialog.Builder(context)
            .setTitle("删除符号")
            .setMessage("确定删除“${symbol.label}”吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                val previous = manager.groups.toList()
                val previousUsage = snapshotUsageState()
                val previousSelection = selectedGroupId
                runCatching {
                    manager.removeSymbol(group.id, symbol.id)
                    pruneUsageState()
                    refresh()
                    showUndo("已删除符号“${symbol.label}”") {
                        manager.replaceAll(previous)
                        restoreUsageState(previousUsage)
                        selectedGroupId = previousSelection
                    }
                }.onFailure(::reportError)
            }
            .showAdaptiveDialog()
    }

    private fun moveSymbol(symbol: SymbolEntry, delta: Int) {
        val group = currentGroup() ?: return
        val index = group.symbols.indexOfFirst { it.id == symbol.id }
        val target = index + delta
        if (index >= 0 && target in group.symbols.indices) {
            manager.moveSymbol(group.id, index, target)
            refresh()
        }
    }

    private fun promptMoveSymbol(symbol: SymbolEntry) {
        val source = currentGroup() ?: return
        val targets = manager.groups.filter { it.id != source.id }
        if (targets.isEmpty()) {
            toast("没有其他可移动到的分组")
            return
        }
        AlertDialog.Builder(context)
            .setTitle("移动到分组")
            .setItems(targets.map { it.title }.toTypedArray()) { _, which ->
                runCatching { manager.moveSymbolToGroup(source.id, symbol.id, targets[which].id); refresh() }
                    .onFailure(::reportError)
            }
            .setNegativeButton("取消", null)
            .showAdaptiveDialog()
    }

    private fun AlertDialog.Builder.showAdaptiveDialog(): AlertDialog {
        val dialog = create()
        dialog.setOnShowListener {
            val density = resources.displayMetrics.density
            val root = this@SymbolDrawerManagerScreen.rootView
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

    private fun horizontalRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }

    private fun actionButton(title: String, action: () -> Unit) = Button(context).apply {
        text = title
        textSize = 12f
        isAllCaps = false
        setTextColor(primaryColor())
        background = roundedSurface(surfaceColor(), 12)
        setPadding(dp(8), dp(2), dp(8), dp(2))
        setOnClickListener { runCatching(action).onFailure(::reportError) }
    }

    private fun invalidateUndo() {
        pendingUndo = null
        undoBar.visibility = View.GONE
    }

    private fun showUndo(message: String, restore: () -> Unit) {
        pendingUndo = restore
        undoMessage.text = message
        undoBar.visibility = View.VISIBLE
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    private fun reportError(error: Throwable) {
        onError?.invoke(error)
        toast(error.message ?: "操作失败")
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private enum class ManagerSymbolFilter { ALL, FAVORITES, RECENT }

    private companion object {
        const val GROUP_PAGE_PREFIX = "symbol-group:"
    }

    private class SymbolAdapter(
        private val onClick: (SymbolEntry) -> Unit,
        private val onLongClick: (SymbolEntry, View) -> Unit,
        private val onDragHandle: (RecyclerView.ViewHolder) -> Unit,
        private val isBatchMode: () -> Boolean,
        private val isSelected: (SymbolEntry) -> Boolean,
        private val onBatchToggle: (SymbolEntry) -> Unit
    ) : ListAdapter<SymbolEntry, SymbolHolder>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
            val ctx = parent.context
            val title = TextView(ctx).apply {
                textSize = 15f
                setTextColor(HostThemeColors.onSurface(ctx))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val detail = TextView(ctx).apply {
                textSize = 12f
                setTextColor(HostThemeColors.secondaryText(ctx))
                setPadding(0, dp(ctx, 4), 0, 0)
            }
            val textColumn = LinearLayout(ctx).apply {
                orientation = VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
                addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(detail, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            val root = LinearLayout(ctx).apply {
                orientation = HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = roundedSurface(ctx, HostThemeColors.surface(ctx), dp(ctx, 12))
                addView(View(ctx).apply {
                    setBackgroundColor(HostThemeColors.primary(ctx))
                }, LinearLayout.LayoutParams(dp(ctx, 3), ViewGroup.LayoutParams.MATCH_PARENT))
                addView(textColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val dragHandle = TextView(ctx).apply {
                    text = "⋮⋮"
                    textSize = 20f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(HostThemeColors.secondaryText(ctx))
                    contentDescription = "拖动调整符号位置"
                    isClickable = true
                    isFocusable = true
                }
                addView(dragHandle, LinearLayout.LayoutParams(dp(ctx, 40), ViewGroup.LayoutParams.MATCH_PARENT))
                isClickable = true
                isFocusable = true
                val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(ctx, 7)
                layoutParams = lp
            }
            val dragHandle = root.getChildAt(root.childCount - 1) as TextView
            return SymbolHolder(root, title, detail, dragHandle)
        }

        override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
            val symbol = getItem(position)
            holder.title.text = if (symbol.icon.isNullOrEmpty()) symbol.label else "${symbol.icon}  ${symbol.label}"
            val shortName = symbol.legacyShortActionId?.let(::actionName) ?: symbol.shortAction.javaClass.simpleName
            val longName = symbol.legacyLongActionId?.let(::actionName) ?: symbol.longAction?.javaClass?.simpleName ?: "无"
            holder.detail.text = "短按  $shortName    ·    长按  $longName"
            holder.root.contentDescription = "${symbol.label}，短按$shortName，长按$longName"
            val selecting = isBatchMode()
            val selected = isSelected(symbol)
            holder.root.alpha = if (selecting && selected) 0.72f else 1f
            holder.title.text = (if (selecting) (if (selected) "☑ " else "□ ") else "") +
                (if (symbol.icon.isNullOrEmpty()) symbol.label else "${symbol.icon}  ${symbol.label}")
            holder.root.setOnClickListener {
                if (isBatchMode()) onBatchToggle(symbol) else onClick(symbol)
            }
            holder.root.setOnLongClickListener {
                if (isBatchMode()) onBatchToggle(symbol) else onLongClick(symbol, holder.root)
                true
            }
            holder.dragHandle.visibility = if (selecting) View.GONE else View.VISIBLE
            holder.dragHandle.isEnabled = true
            holder.dragHandle.alpha = if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) 0.45f else 1f
            // Keep ownership of the complete pointer stream. Returning false
            // for MOVE/UP after consuming DOWN lets the handle's normal touch
            // handling compete with ItemTouchHelper and can terminate an
            // upward drag before the finger is released.
            var dragPointerActive = false
            holder.dragHandle.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        dragPointerActive = true
                        onDragHandle(holder)
                        true
                    }
                    android.view.MotionEvent.ACTION_MOVE,
                    android.view.MotionEvent.ACTION_POINTER_DOWN,
                    android.view.MotionEvent.ACTION_POINTER_UP -> dragPointerActive
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        val consumed = dragPointerActive
                        dragPointerActive = false
                        consumed
                    }
                    else -> dragPointerActive
                }
            }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<SymbolEntry>() {
                override fun areItemsTheSame(old: SymbolEntry, new: SymbolEntry) = old.id == new.id
                override fun areContentsTheSame(old: SymbolEntry, new: SymbolEntry) = old == new
            }
            private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
            private fun actionName(id: Int): String {
                val index = LegacySymbolActionCatalog.values.indexOf(id)
                return LegacySymbolActionCatalog.labels.getOrNull(index) ?: "动作 $id"
            }
            private fun roundedSurface(context: Context, color: Int, radius: Int) = GradientDrawable().apply {
                setColor(color)
                cornerRadius = radius.toFloat()
                setStroke(1, HostThemeColors.outline(context))
            }
        }
    }

    private fun roundedSurface(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), HostThemeColors.outline(context))
    }
    private fun surfaceColor() = HostThemeColors.surface(context)
    private fun primaryColor() = HostThemeColors.primary(context)
    private fun secondaryTextColor() = HostThemeColors.secondaryText(context)

}
