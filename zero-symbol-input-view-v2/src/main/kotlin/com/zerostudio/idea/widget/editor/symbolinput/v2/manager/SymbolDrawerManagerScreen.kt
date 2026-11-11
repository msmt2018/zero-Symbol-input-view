package com.zerostudio.idea.widget.editor.symbolinput.v2.manager

import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.TextView
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
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolEntry
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.LegacySymbolAction
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionAdapter
import com.zerostudio.idea.widget.editor.symbolinput.v2.data.LegacySymbolActionCatalog
import com.zerostudio.idea.widget.editor.symbolinput.v2.model.SymbolGroup
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SharedPreferencesSymbolDrawerSettingsRepository

private class SymbolHolder(val title: TextView) : RecyclerView.ViewHolder(title)

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
    private val groupSpinner = Spinner(context)
    private val searchField = EditText(context)
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

    private fun buildHeader() {
        val row = horizontalRow()
        row.addView(actionButton("新建分组") { promptGroupName("新建分组") { manager.addGroup(it); selectedGroupId = manager.groups.lastOrNull()?.id; refresh() } })
        row.addView(actionButton("新建符号") { if (currentGroup() == null) toast("请先创建分组") else editSymbol(null) })
        row.addView(actionButton("导出") {
            runCatching { exportBundle() }.onSuccess { bundle ->
                onExportBundle?.invoke(bundle)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Symbol drawer backup", bundle))
                toast("备份 JSON 已复制")
            }.onFailure(::reportError)
        })
        row.addView(actionButton("导入") { promptImport() })
        addView(row, matchWrap())
    }

    private fun buildGroupControls() {
        addView(groupSpinner, matchWrap())
        val row = horizontalRow()
        row.addView(actionButton("重命名") {
            currentGroup()?.let { group -> promptGroupName("重命名分组", group.title) { manager.renameGroup(group.id, it); refresh() } }
        })
        row.addView(actionButton("复制") {
            currentGroup()?.let { group -> selectedGroupId = manager.duplicateGroup(group.id).id; refresh() }
        })
        row.addView(actionButton("上移") { moveCurrentGroup(-1) })
        row.addView(actionButton("下移") { moveCurrentGroup(1) })
        row.addView(actionButton("删除") { confirmDeleteGroup() })
        addView(row, matchWrap())
        groupSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedGroupId = manager.groups.getOrNull(position)?.id
                refreshSymbols()
            }
        }
    }

    private fun buildSearch() {
        searchField.hint = "搜索符号名称"
        searchField.setSingleLine(true)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshSymbols()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        addView(searchField, matchWrap())
    }

    private fun buildSymbolList() {
        emptyView.text = "当前分组暂无符号，点击“新建符号”添加"
        emptyView.textSize = 13f
        emptyView.alpha = 0.65f
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
        groupSpinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            groups.map { it.title }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        if (groups.isNotEmpty()) groupSpinner.setSelection(selectedIndex, false)
        val enabled = groups.isNotEmpty()
        searchField.isEnabled = enabled
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

    private fun confirmDeleteGroup() {
        val group = currentGroup() ?: return
        AlertDialog.Builder(context)
            .setTitle("删除分组")
            .setMessage("删除“${group.title}”及其中全部符号？此操作无法撤销。")
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
            .show()
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
            .show()
    }

    private fun promptImport() {
        val input = EditText(context).apply {
            hint = "粘贴符号抽屉备份 JSON"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 5
            gravity = android.view.Gravity.TOP
        }
        AlertDialog.Builder(context)
            .setTitle("导入备份")
            .setMessage("导入会替换当前全部分组。建议先导出备份。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("继续") { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle("确认替换")
                    .setMessage("确定用导入的数据替换当前所有分组吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("替换") { _, _ ->
                        runCatching { importBundle(input.text.toString()) }.onFailure(::reportError)
                    }
                    .show()
            }
            .show()
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
        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "新建符号" else "编辑符号")
            .setView(form)
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
            .show()
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
            .show()
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
            .show()
    }

    private fun horizontalRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }

    private fun actionButton(title: String, action: () -> Unit) = Button(context).apply {
        text = title
        textSize = 12f
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
            val view = TextView(parent.context).apply {
                textSize = 16f
                setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
                isClickable = true
                isFocusable = true
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return SymbolHolder(view)
        }

        override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
            val symbol = getItem(position)
            holder.title.text = if (symbol.icon.isNullOrEmpty()) symbol.label else "${symbol.icon}  ${symbol.label}"
            holder.title.contentDescription = symbol.label
            holder.title.setOnClickListener { onClick(symbol) }
            holder.title.setOnLongClickListener { onLongClick(symbol, holder.title); true }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<SymbolEntry>() {
                override fun areItemsTheSame(old: SymbolEntry, new: SymbolEntry) = old.id == new.id
                override fun areContentsTheSame(old: SymbolEntry, new: SymbolEntry) = old == new
            }
            private fun dp(context: Context, value: Int) =
                (value * context.resources.displayMetrics.density).toInt()
        }
    }
}
