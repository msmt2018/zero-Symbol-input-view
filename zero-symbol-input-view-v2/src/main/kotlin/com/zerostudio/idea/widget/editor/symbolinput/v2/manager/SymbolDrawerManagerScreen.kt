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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Toast
import android.app.AlertDialog
import android.text.InputType
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SharedPreferencesSymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupBundleCodec
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupManager
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolDataMigration
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolDrawerDefaults
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.SymbolGroupRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.ui.HostThemeColors
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveContentLayoutResolver

private class SymbolHolder(val root: LinearLayout, val title: TextView, val detail: TextView) : RecyclerView.ViewHolder(root)

/**
 * First-party manager screen for groups and symbols. The screen is intentionally
 * a View rather than an Activity so the host controls navigation and window policy.
 */
class SymbolDrawerManagerScreen(
    context: Context,
    repository: SymbolGroupRepository = SharedPreferencesSymbolGroupRepository(context),
    private val embeddedInSettings: Boolean = false
) : LinearLayout(context) {

    var onGroupsChanged: ((List<SymbolGroup>) -> Unit)? = null
    var onExportBundle: ((String) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private val manager = SymbolGroupManager(repository.also {
        LegacySymbolDataMigration.importIfV2Empty(context, it)
    })
    private val settings = SharedPreferencesSymbolDrawerSettingsRepository(context).load().settings
    private val groupTabs = TabLayout(context)
    private val searchField = EditText(context)
    private val searchToggle = Button(context)
    private val searchContainer = LinearLayout(context)
    private var rebuildingTabs = false
    private val emptyView = TextView(context)
    private val symbolAdapter = SymbolAdapter(
        onClick = { symbol -> editSymbol(symbol) },
        onLongClick = { symbol, anchor -> showSymbolMenu(symbol, anchor) }
    )
    private var selectedGroupId: String? = manager.groups.firstOrNull()?.id
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
        buildUndoBar()
        refresh()
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
    }

    fun reload() {
        manager.reload()
        selectedGroupId = manager.groups.firstOrNull()?.id
        refresh()
    }

    fun exportBundle(): String = SymbolGroupBundleCodec.encode(manager.groups)

    fun importBundle(json: String) {
        val previous = manager.groups.toList()
        val previousSelection = selectedGroupId
        val imported = SymbolGroupBundleCodec.decode(json)
        manager.replaceAll(imported)
        selectedGroupId = imported.firstOrNull()?.id
        refresh()
        showUndo("已导入符号抽屉备份") {
            manager.replaceAll(previous)
            selectedGroupId = previousSelection
        }
    }

    private var groupActionsButton: Button? = null

    private fun buildHeader() {
        val row = horizontalRow().apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        row.addView(TextView(context).apply {
            text = "符号管理"
            textSize = 17f
            setTextColor(HostThemeColors.onSurface(context))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        val addButton = actionButton("＋ 添加") { }
        addButton.setOnClickListener { showAddMenu(addButton) }
        row.addView(addButton, LayoutParams(LayoutParams.WRAP_CONTENT, dp(40)))
        searchToggle.text = "⌕"
        searchToggle.textSize = 22f
        searchToggle.minWidth = dp(40)
        searchToggle.setPadding(dp(8), 0, dp(8), 0)
        searchToggle.setOnClickListener {
            val show = searchContainer.visibility != View.VISIBLE
            searchContainer.visibility = if (show) View.VISIBLE else View.GONE
            if (!show) searchField.text?.clear() else searchField.requestFocus()
        }
        row.addView(searchToggle, LayoutParams(dp(44), dp(40)))
        addView(row, matchWrap())
    }

    private fun buildGroupControls() {
        groupTabs.tabMode = TabLayout.MODE_SCROLLABLE
        groupTabs.tabGravity = TabLayout.GRAVITY_START
        groupTabs.setTabTextColors(secondaryTextColor(), primaryColor())
        groupTabs.setSelectedTabIndicatorColor(primaryColor())
        groupTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (rebuildingTabs) return
                selectedGroupId = manager.groups.getOrNull(tab.position)?.id
                refreshSymbols()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        addView(groupTabs, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
    }

    private fun showAddMenu(anchor: View) {
        val menu = PopupMenu(context, anchor)
        menu.menu.add("新建分组").setOnMenuItemClickListener {
            promptGroupName("新建分组") {
                val created = manager.addGroup(it)
                selectedGroupId = created.id
                refresh()
            }
            true
        }
        val group = currentGroup()
        menu.menu.add(if (group == null) "新建符号（请先创建分组）" else "在“${group.title}”中新建符号")
            .apply { isEnabled = group != null }
            .setOnMenuItemClickListener { editSymbol(null); true }
        menu.show()
    }

    private fun showGroupTabMenu(anchor: View, groupId: String) {
        val group = manager.groups.firstOrNull { it.id == groupId } ?: return
        selectedGroupId = group.id
        val index = manager.groups.indexOfFirst { it.id == group.id }
        groupTabs.getTabAt(index)?.select()
        PopupMenu(context, anchor).apply {
            menu.add("左移").apply { isEnabled = index > 0 }
                .setOnMenuItemClickListener { moveGroup(group.id, -1); true }
            menu.add("右移").apply { isEnabled = index >= 0 && index < manager.groups.lastIndex }
                .setOnMenuItemClickListener { moveGroup(group.id, 1); true }
            menu.add("添加").setOnMenuItemClickListener { showAddGroupDialog(); true }
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
            groupTabs.getTabAt(target)?.select()
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

    private fun buildSearch() {
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

    private fun buildSymbolList() {
        emptyView.text = "当前分组暂无符号，点击“添加”并选择“新建符号”"
        emptyView.textSize = 13f
        emptyView.setTextColor(secondaryTextColor())
        emptyView.alpha = 0.75f
        emptyView.setPadding(dp(8), dp(16), dp(8), dp(16))
        addView(emptyView, matchWrap())
        val list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = symbolAdapter
            itemAnimator = null
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
        if (embeddedInSettings) {
            list.isNestedScrollingEnabled = false
            addView(list, LayoutParams(LayoutParams.MATCH_PARENT, dp(320)))
        } else {
            addView(list, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
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
        val groups = manager.groups
        val selectedIndex = groups.indexOfFirst { it.id == selectedGroupId }.takeIf { it >= 0 } ?: 0
        selectedGroupId = groups.getOrNull(selectedIndex)?.id
        rebuildingTabs = true
        groupTabs.removeAllTabs()
        groups.forEachIndexed { index, group ->
            val tab = groupTabs.newTab().setText(group.title)
            groupTabs.addTab(tab, index == selectedIndex)
            tab.view.setOnLongClickListener {
                showGroupTabMenu(it, group.id)
                true
            }
        }
        rebuildingTabs = false
        if (groups.isNotEmpty() && groupTabs.selectedTabPosition != selectedIndex) {
            groupTabs.getTabAt(selectedIndex)?.select()
        }
        searchField.isEnabled = groups.isNotEmpty()
        searchToggle.isEnabled = groups.isNotEmpty()
        refreshSymbols()
        onGroupsChanged?.invoke(groups)
    }

    private fun refreshSymbols() {
        val group = currentGroup()
        val query = searchField.text?.toString()?.trim().orEmpty()
        val symbols = group?.symbols.orEmpty().filter { symbol ->
            query.isEmpty() ||
                symbol.label.contains(query, ignoreCase = true) ||
                (settings.searchIncludesActionPayload && (
                    symbol.shortAction.toString().contains(query, ignoreCase = true) ||
                        symbol.longAction?.let {
                            it.toString().contains(query, ignoreCase = true)
                        } == true
                    ))
        }
        symbolAdapter.submitList(symbols)
        emptyView.visibility = if (symbols.isEmpty()) View.VISIBLE else View.GONE
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
                val previousSelection = selectedGroupId
                manager.removeGroup(group.id)
                selectedGroupId = manager.groups.firstOrNull()?.id
                refresh()
                showUndo("已删除分组“${group.title}”") {
                    manager.replaceAll(previous)
                    selectedGroupId = previousSelection
                }
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
        val shortIndex = existing?.legacyShortActionId
            ?.let { LegacySymbolActionCatalog.values.indexOf(it) }
            ?.takeIf { it >= 0 }
            ?: LegacySymbolActionCatalog.values.indexOfFirst { value ->
                existing?.shortAction == LegacySymbolActionAdapter.convert(LegacySymbolAction(value, null))
            }.coerceAtLeast(0)
        val longIndex = existing?.legacyLongActionId?.let { LegacySymbolActionCatalog.values.indexOf(it) }
            ?.takeIf { it >= 0 }?.plus(1)
            ?: existing?.longAction?.let { action ->
                val found = LegacySymbolActionCatalog.values.indexOfFirst { value ->
                    LegacySymbolActionAdapter.convert(LegacySymbolAction(value, null)) == action
                }
                when {
                    found >= 0 -> found + 1
                    action is com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.InsertText -> 1
                    else -> 0
                }
            } ?: 0
        val shortAction = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, LegacySymbolActionCatalog.labels)
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(shortIndex, false)
        }
        val longLabels = listOf("无长按动作") + LegacySymbolActionCatalog.labels
        val longAction = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, longLabels)
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(longIndex, false)
        }
        val shortText = EditText(context).apply {
            hint = "短按插入文本（仅“插入文本”动作使用）"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(existing?.legacyShortText
                ?: (existing?.shortAction as? com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.InsertText)?.template.orEmpty())
        }
        val longText = EditText(context).apply {
            hint = "长按插入文本（仅“插入文本”动作使用）"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            setText(existing?.legacyLongText
                ?: (existing?.longAction as? com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolAction.InsertText)?.template.orEmpty())
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
                text = "\$\$ 表示字面量 \$；\$S/\$E 标记选区起止；\$T 表示当前选中文本"
                textSize = 12f
                alpha = 0.7f
            }, matchWrap())
        }
        val formScroll = ScrollView(context).apply {
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
                    val shortId = LegacySymbolActionCatalog.values[shortAction.selectedItemPosition]
                    val longPosition = longAction.selectedItemPosition
                    val longId = if (longPosition == 0) null else LegacySymbolActionCatalog.values[longPosition - 1]
                    val shortPayload = shortText.text.toString().takeIf { shortId == 0 }
                    val longPayload = longText.text.toString().takeIf { longId == 0 }
                    val symbol = SymbolEntry(
                        id = existing?.id ?: 0L,
                        label = display,
                        shortAction = LegacySymbolActionAdapter.convert(LegacySymbolAction(shortId, shortPayload)),
                        longAction = longId?.let { LegacySymbolActionAdapter.convert(LegacySymbolAction(it, longPayload)) },
                        legacyShortActionId = shortId,
                        legacyShortText = shortPayload,
                        legacyLongActionId = longId,
                        legacyLongText = longPayload
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
                val previousSelection = selectedGroupId
                manager.removeSymbol(group.id, symbol.id)
                refresh()
                showUndo("已删除符号“${symbol.label}”") {
                    manager.replaceAll(previous)
                    selectedGroupId = previousSelection
                }
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
            val root = rootView
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

    private class SymbolAdapter(
        private val onClick: (SymbolEntry) -> Unit,
        private val onLongClick: (SymbolEntry, View) -> Unit
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
                isClickable = true
                isFocusable = true
                val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(ctx, 7)
                layoutParams = lp
            }
            return SymbolHolder(root, title, detail)
        }

        override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
            val symbol = getItem(position)
            holder.title.text = if (symbol.icon.isNullOrEmpty()) symbol.label else "${symbol.icon}  ${symbol.label}"
            val shortName = symbol.legacyShortActionId?.let(::actionName) ?: symbol.shortAction.javaClass.simpleName
            val longName = symbol.legacyLongActionId?.let(::actionName) ?: symbol.longAction?.javaClass?.simpleName ?: "无"
            holder.detail.text = "短按  $shortName    ·    长按  $longName"
            holder.root.contentDescription = "${symbol.label}，短按$shortName，长按$longName"
            holder.root.setOnClickListener { onClick(symbol) }
            holder.root.setOnLongClickListener { onLongClick(symbol, holder.root); true }
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
