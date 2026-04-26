package android.zero.studio.widget.editor.symbolinput

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SymbolManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var multiActionBar: View
    private var symbolGroups = mutableListOf<SymbolGroup>()
    private lateinit var pagerAdapter: GroupPagerAdapter

    private lateinit var actionValues: IntArray
    private lateinit var actionNames: Array<String>

    private var isBatchMode = false
    private var batchGroupIndex = -1
    private val selectedItems = linkedSetOf<SymbolItem>()
    private val settingsTabTitle by lazy { getString(R.string.settings_tab_title) }
    private val indicatorStyleNames by lazy {
        arrayOf(
            getString(R.string.settings_style_standard),
            getString(R.string.settings_style_simple),
            getString(R.string.settings_style_hidden),
            getString(R.string.settings_style_top_line),
            getString(R.string.settings_style_block)
        )
    }
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importFromUri)
    }
    private val exportFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(::showExportNameDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_symbol_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        appBarLayout = findViewById(R.id.app_bar_layout)
        multiActionBar = findViewById(R.id.multi_action_bar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            if (isBatchMode) {
                exitBatchMode()
            } else {
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        actionValues = resources.getIntArray(R.array.symbol_action_values)
        actionNames = resources.getStringArray(R.array.symbol_action_names)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        symbolGroups = SymbolDataManager.loadData(this)

        setupBatchActionBar()

        pagerAdapter = GroupPagerAdapter()
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        bindGroupTabLongPressMenus()
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (isBatchMode && (position != batchGroupIndex || isSettingsPosition(position))) {
                    exitBatchMode()
                }
            }
        })
    }

    private fun bindGroupTabLongPressMenus() {
        tabLayout.post {
            for (i in 0 until tabLayout.tabCount) {
                val tab = tabLayout.getTabAt(i) ?: continue
                tab.view.setOnLongClickListener {
                    if (!isSettingsPosition(i) && i in symbolGroups.indices) {
                        showGroupTabMenu(tab.view, i)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun showGroupTabMenu(anchor: View, groupIndex: Int) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_symbol_group_tab_actions, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_tab_move_left -> moveGroup(groupIndex, -1)
                    R.id.action_tab_move_right -> moveGroup(groupIndex, 1)
                    R.id.action_tab_add -> showAddGroupDialog()
                    R.id.action_tab_copy_to -> copyGroupToAnother(groupIndex)
                    R.id.action_tab_rename -> renameGroup(groupIndex)
                    R.id.action_tab_delete -> deleteGroup(groupIndex)
                }
                true
            }
            show()
        }
    }

    private fun moveGroup(groupIndex: Int, delta: Int) {
        val target = (groupIndex + delta).coerceIn(0, symbolGroups.lastIndex)
        if (target == groupIndex) return
        val item = symbolGroups.removeAt(groupIndex)
        symbolGroups.add(target, item)
        SymbolDataManager.saveData(this, symbolGroups)
        onGroupsChanged(targetGroupIndex = target)
    }

    private fun copyGroupToAnother(sourceIndex: Int) {
        val source = symbolGroups.getOrNull(sourceIndex) ?: return
        showTargetGroupDialog(getString(R.string.tab_action_copy_to)) { targetIndex ->
            if (targetIndex !in symbolGroups.indices) return@showTargetGroupDialog
            val copied = source.items.map {
                SymbolItem(it.shortAction, it.display, it.shortText, it.longAction, it.longText)
            }
            symbolGroups[targetIndex].items.addAll(copied)
            SymbolDataManager.saveData(this, symbolGroups)
            onGroupsChanged(targetGroupIndex = targetIndex)
        }
    }

    private fun renameGroup(groupIndex: Int) {
        val group = symbolGroups.getOrNull(groupIndex) ?: return
        val editText = EditText(this).apply {
            setText(group.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_rename_group)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    group.name = newName
                    SymbolDataManager.saveData(this, symbolGroups)
                    onGroupsChanged(targetGroupIndex = groupIndex)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun deleteGroup(groupIndex: Int) {
        if (groupIndex !in symbolGroups.indices) return
        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_confirm_delete_group)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                symbolGroups.removeAt(groupIndex)
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged(targetGroupIndex = (groupIndex - 1).coerceAtLeast(0))
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun setupBatchActionBar() {
        findViewById<View>(R.id.action_batch_copy).setOnClickListener { performBatchCopyOrCut(isCut = false) }
        findViewById<View>(R.id.action_batch_cut).setOnClickListener { performBatchCopyOrCut(isCut = true) }
        findViewById<View>(R.id.action_batch_invert).setOnClickListener { invertBatchSelection() }
        findViewById<View>(R.id.action_batch_delete).setOnClickListener { confirmDeleteSelected() }
        findViewById<View>(R.id.action_batch_close).setOnClickListener { exitBatchMode() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_symbol_manager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                if (symbolGroups.isEmpty()) {
                    Toast.makeText(this, R.string.toast_need_group_first, Toast.LENGTH_SHORT).show()
                } else {
                    val currentGroup = viewPager.currentItem
                    if (isSettingsPosition(currentGroup)) {
                        Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    showEditDialog(symbolGroups[currentGroup], null)
                }
                return true
            }

            R.id.action_add_group -> {
                showAddGroupDialog()
                return true
            }

            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_export_clipboard -> exportToClipboard()
            R.id.action_import_file -> {
                importFileLauncher.launch(arrayOf("application/json"))
                return true
            }

            R.id.action_export_file -> {
                exportFolderLauncher.launch(null)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAddGroupDialog(onCreated: ((Int) -> Unit)? = null) {
        val editText = EditText(this).apply {
            hint = getString(R.string.group_name)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.action_add_group)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                symbolGroups.add(SymbolGroup(name = name, items = mutableListOf()))
                SymbolDataManager.saveData(this, symbolGroups)
                val newIndex = symbolGroups.lastIndex
                onGroupsChanged(targetGroupIndex = newIndex)
                onCreated?.invoke(newIndex)
                Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun exportToClipboard() {
        val jsonStr = SymbolDataManager.gson.toJson(symbolGroups)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SymbolData", jsonStr))
        Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
    }

    private fun importFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        if (text != null) {
            try {
                val listType = object : com.google.gson.reflect.TypeToken<MutableList<SymbolGroup>>() {}.type
                val importedData: MutableList<SymbolGroup> = SymbolDataManager.gson.fromJson(text, listType)
                symbolGroups.clear()
                symbolGroups.addAll(importedData)
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
                Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text.isNullOrBlank()) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                return
            }
            val listType = object : com.google.gson.reflect.TypeToken<MutableList<SymbolGroup>>() {}.type
            val importedData: MutableList<SymbolGroup> = SymbolDataManager.gson.fromJson(text, listType)
            symbolGroups.clear()
            symbolGroups.addAll(importedData)
            SymbolDataManager.saveData(this, symbolGroups)
            onGroupsChanged(targetGroupIndex = 0)
            Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportNameDialog(treeUri: Uri) {
        val defaultName = "symbol-config-${
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        }.json"
        val editText = EditText(this).apply {
            setText(defaultName)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_export_file)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val rawName = editText.text.toString().trim()
                val fileName = when {
                    rawName.isEmpty() -> defaultName
                    rawName.endsWith(".json", true) -> rawName
                    else -> "$rawName.json"
                }
                exportToDirectoryUri(treeUri, fileName)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun exportToDirectoryUri(treeUri: Uri, fileName: String) {
        try {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
            val targetUri = DocumentsContract.createDocument(
                contentResolver,
                treeDocumentUri,
                "application/json",
                fileName
            )
            if (targetUri == null) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                return
            }

            val jsonStr = SymbolDataManager.gson.toJson(symbolGroups)
            contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { it.write(jsonStr) }
            Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(group: SymbolGroup, itemToEdit: SymbolItem?) {
        showSymbolDialog(
            title = if (itemToEdit == null) getString(R.string.dialog_title_add_symbol) else getString(R.string.dialog_title_edit_symbol),
            initialItem = itemToEdit,
            showDeleteButton = itemToEdit != null,
            onSave = { newItem ->
                if (itemToEdit == null) {
                    group.items.add(newItem)
                } else {
                    val index = group.items.indexOf(itemToEdit)
                    if (index >= 0) {
                        group.items[index] = newItem
                    }
                }
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
            },
            onDelete = {
                if (itemToEdit != null) {
                    group.items.remove(itemToEdit)
                    selectedItems.remove(itemToEdit)
                    SymbolDataManager.saveData(this, symbolGroups)
                    onGroupsChanged()
                }
            }
        )
    }

    private fun showCopyDialog(group: SymbolGroup, sourceItem: SymbolItem) {
        showSymbolDialog(
            title = getString(R.string.menu_item_copy),
            initialItem = sourceItem,
            showDeleteButton = false,
            onSave = { newItem ->
                val index = group.items.indexOf(sourceItem)
                if (index >= 0) {
                    group.items.add(index + 1, newItem)
                } else {
                    group.items.add(newItem)
                }
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
            },
            onDelete = null
        )
    }

    private fun showSymbolDialog(
        title: String,
        initialItem: SymbolItem?,
        showDeleteButton: Boolean,
        onSave: (SymbolItem) -> Unit,
        onDelete: (() -> Unit)?
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_symbol_edit, null)
        val etDisplay = view.findViewById<EditText>(R.id.et_display)
        val spShortAction = view.findViewById<Spinner>(R.id.sp_short_action)
        val etShortText = view.findViewById<EditText>(R.id.et_short_text)
        val spLongAction = view.findViewById<Spinner>(R.id.sp_long_action)
        val etLongText = view.findViewById<EditText>(R.id.et_long_text)

        val longNames = mutableListOf(getString(R.string.action_no_long_press)).apply { addAll(actionNames) }
        spShortAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionNames)
        spLongAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, longNames)

        if (initialItem != null) {
            etDisplay.setText(initialItem.display)
            etShortText.setText(initialItem.shortText)
            etLongText.setText(initialItem.longText)
            spShortAction.setSelection(actionValues.indexOf(initialItem.shortAction).coerceAtLeast(0))
            initialItem.longAction?.let {
                spLongAction.setSelection(actionValues.indexOf(it).coerceAtLeast(0) + 1)
            }
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val shortAct = actionValues[spShortAction.selectedItemPosition]
                val longPos = spLongAction.selectedItemPosition
                val longAct = if (longPos > 0) actionValues[longPos - 1] else null

                val newItem = SymbolItem(
                    shortAction = shortAct,
                    display = etDisplay.text.toString(),
                    shortText = etShortText.text.toString().takeIf { shortAct == 0 },
                    longAction = longAct,
                    longText = etLongText.text.toString().takeIf { longAct == 0 }
                )
                onSave(newItem)
            }
            .setNegativeButton(R.string.dialog_cancel, null)

        if (showDeleteButton && onDelete != null) {
            builder.setNeutralButton(R.string.dialog_delete) { _, _ -> onDelete() }
        }

        builder.show()
    }

    private fun showItemMenu(anchor: View, group: SymbolGroup, item: SymbolItem) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_symbol_item_actions, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_item_edit -> showEditDialog(group, item)
                    R.id.action_item_copy -> showCopyDialog(group, item)
                    R.id.action_item_delete -> confirmDeleteSingle(group, item)
                    R.id.action_item_batch -> enterBatchMode(viewPager.currentItem, item)
                }
                true
            }
            show()
        }
    }

    private fun confirmDeleteSingle(group: SymbolGroup, item: SymbolItem) {
        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_confirm_delete_symbol)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                group.items.remove(item)
                selectedItems.remove(item)
                SymbolDataManager.saveData(this, symbolGroups)
                if (isBatchMode && selectedItems.isEmpty()) {
                    exitBatchMode()
                }
                onGroupsChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun enterBatchMode(groupIndex: Int, seedItem: SymbolItem) {
        isBatchMode = true
        batchGroupIndex = groupIndex
        selectedItems.clear()
        selectedItems.add(seedItem)
        tabLayout.visibility = View.GONE
        multiActionBar.visibility = View.VISIBLE
        pagerAdapter.notifyDataSetChanged()
    }

    private fun exitBatchMode() {
        isBatchMode = false
        batchGroupIndex = -1
        selectedItems.clear()
        multiActionBar.visibility = View.GONE
        tabLayout.visibility = View.VISIBLE
        pagerAdapter.notifyDataSetChanged()
    }

    private fun toggleSelected(item: SymbolItem) {
        if (!selectedItems.add(item)) {
            selectedItems.remove(item)
        }
        if (selectedItems.isEmpty()) {
            exitBatchMode()
            return
        }
        pagerAdapter.notifyDataSetChanged()
    }

    private fun invertBatchSelection() {
        if (!isBatchMode || batchGroupIndex !in symbolGroups.indices) return
        val group = symbolGroups[batchGroupIndex]
        val newSelected = linkedSetOf<SymbolItem>()
        group.items.forEach { if (!selectedItems.contains(it)) newSelected.add(it) }
        selectedItems.clear()
        selectedItems.addAll(newSelected)
        if (selectedItems.isEmpty()) {
            exitBatchMode()
            return
        }
        pagerAdapter.notifyDataSetChanged()
    }

    private fun confirmDeleteSelected() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        if (batchGroupIndex !in symbolGroups.indices) return

        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_confirm_delete_selected)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                val group = symbolGroups[batchGroupIndex]
                group.items.removeAll(selectedItems.toSet())
                SymbolDataManager.saveData(this, symbolGroups)
                exitBatchMode()
                onGroupsChanged(targetGroupIndex = batchGroupIndex)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun performBatchCopyOrCut(isCut: Boolean) {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        if (batchGroupIndex !in symbolGroups.indices) return

        val titleRes = if (isCut) R.string.dialog_move_to else R.string.dialog_copy_to
        showTargetGroupDialog(getString(titleRes)) { targetGroupIndex ->
            val sourceGroup = symbolGroups[batchGroupIndex]
            if (isCut && targetGroupIndex == batchGroupIndex) {
                Toast.makeText(this, R.string.toast_same_group_move, Toast.LENGTH_SHORT).show()
                return@showTargetGroupDialog
            }

            val orderedSelected = sourceGroup.items.filter { selectedItems.contains(it) }
            val copiedItems = orderedSelected.map {
                SymbolItem(
                    shortAction = it.shortAction,
                    display = it.display,
                    shortText = it.shortText,
                    longAction = it.longAction,
                    longText = it.longText
                )
            }

            symbolGroups[targetGroupIndex].items.addAll(copiedItems)
            if (isCut) {
                sourceGroup.items.removeAll(selectedItems.toSet())
            }

            SymbolDataManager.saveData(this, symbolGroups)
            exitBatchMode()
            onGroupsChanged(targetGroupIndex = targetGroupIndex)
        }
    }

    private fun showTargetGroupDialog(title: String, onTargetSelected: (Int) -> Unit) {
        if (symbolGroups.isEmpty()) return

        var selectedIndex = viewPager.currentItem.coerceIn(0, symbolGroups.lastIndex)

        fun showChooser() {
            val names = symbolGroups.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(names, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton(R.string.dialog_save) { _, _ ->
                    onTargetSelected(selectedIndex)
                }
                .setNeutralButton(R.string.dialog_new_group) { _, _ ->
                    showAddGroupDialog { newIndex ->
                        selectedIndex = newIndex
                        showChooser()
                    }
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        showChooser()
    }

    private fun isSettingsPosition(position: Int): Boolean = position == symbolGroups.size

    private fun createSettingsPage(container: ViewGroup): View {
        val scrollView = NestedScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        scrollView.addView(content)

        fun createEntry(title: String, subtitle: String): View {
            val item = LayoutInflater.from(this).inflate(R.layout.item_symbol_manage, content, false)
            val tvTitle = item.findViewById<TextView>(R.id.tv_title)
            val tvSubtitle = item.findViewById<TextView>(R.id.tv_subtitle)
            item.findViewById<View>(R.id.iv_drag_handle).visibility = View.GONE
            tvTitle.text = title
            tvSubtitle.text = subtitle
            return item
        }

        val settings = SymbolDataManager.getUiSettings(this)
        val lineItem = createEntry(getString(R.string.settings_lines_title), "${settings.collapsedRows} - ${settings.symbolsPerRow}")
        lineItem.setOnClickListener { showLineSettingDialog() }
        content.addView(lineItem)

        val indicatorText = indicatorStyleNames[settings.indicatorStyle.coerceIn(0, indicatorStyleNames.lastIndex)]
        val indicatorItem = createEntry(getString(R.string.settings_indicator_title), indicatorText)
        indicatorItem.setOnClickListener { showIndicatorStyleDialog() }
        content.addView(indicatorItem)

        val rememberItem = createEntry(getString(R.string.settings_remember_title), getString(R.string.settings_remember_desc))
        val rememberSwitch = SwitchCompat(this).apply { isChecked = settings.rememberExpanded }
        (rememberItem as ViewGroup).addView(rememberSwitch)
        rememberSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(rememberExpanded = isChecked))
            if (!isChecked) {
                SymbolDataManager.setLastExpanded(this, false)
            }
        }
        content.addView(rememberItem)

        val uniformItem = createEntry(getString(R.string.settings_uniform_title), getString(R.string.settings_uniform_desc))
        val uniformSwitch = SwitchCompat(this).apply { isChecked = settings.uniformGroupHeight }
        (uniformItem as ViewGroup).addView(uniformSwitch)
        uniformSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(uniformGroupHeight = isChecked))
        }
        content.addView(uniformItem)

        val textSizeItem = createEntry(getString(R.string.settings_symbol_text_size), "${settings.symbolTextSizeSp}sp")
        textSizeItem.setOnClickListener { showTextSizeDialog() }
        content.addView(textSizeItem)

        val handleItem = createEntry(getString(R.string.settings_show_drag_handle), getString(R.string.settings_show_drag_handle_desc))
        val handleSwitch = SwitchCompat(this).apply { isChecked = settings.showDragHandle }
        (handleItem as ViewGroup).addView(handleSwitch)
        handleSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(showDragHandle = isChecked))
            pagerAdapter.notifyDataSetChanged()
        }
        content.addView(handleItem)

        val advancedItem = createEntry(getString(R.string.settings_enable_advanced_actions), getString(R.string.settings_enable_advanced_actions_desc))
        val advancedSwitch = SwitchCompat(this).apply { isChecked = settings.enableAdvancedActions }
        (advancedItem as ViewGroup).addView(advancedSwitch)
        advancedSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(enableAdvancedActions = isChecked))
        }
        content.addView(advancedItem)

        val rememberPageItem = createEntry(getString(R.string.settings_remember_page_title), getString(R.string.settings_remember_page_desc))
        val rememberPageSwitch = SwitchCompat(this).apply { isChecked = settings.rememberLastPage }
        (rememberPageItem as ViewGroup).addView(rememberPageSwitch)
        rememberPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(rememberLastPage = isChecked))
        }
        content.addView(rememberPageItem)

        return scrollView
    }

    private fun showLineSettingDialog() {
        val settings = SymbolDataManager.getUiSettings(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_line_settings, null)
        val minEdit = view.findViewById<EditText>(R.id.et_min_rows)
        val maxEdit = view.findViewById<EditText>(R.id.et_max_cols)
        minEdit.setText(settings.collapsedRows.toString())
        maxEdit.setText(settings.symbolsPerRow.toString())

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_lines_title)
            .setView(view)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val minRows = minEdit.text.toString().toIntOrNull()?.coerceIn(1, 10) ?: settings.collapsedRows
                val maxCols = maxEdit.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: settings.symbolsPerRow
                SymbolDataManager.saveUiSettings(
                    this,
                    settings.copy(collapsedRows = minRows, symbolsPerRow = maxCols)
                )
                pagerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showIndicatorStyleDialog() {
        val settings = SymbolDataManager.getUiSettings(this)
        var checked = settings.indicatorStyle.coerceIn(0, indicatorStyleNames.lastIndex)
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_indicator_title)
            .setSingleChoiceItems(indicatorStyleNames, checked) { _, which ->
                checked = which
            }
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                SymbolDataManager.saveUiSettings(this, settings.copy(indicatorStyle = checked))
                pagerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showTextSizeDialog() {
        val settings = SymbolDataManager.getUiSettings(this)
        val editText = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(settings.symbolTextSizeSp.toString())
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_symbol_text_size)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val textSize = editText.text.toString().toIntOrNull()?.coerceIn(12, 28) ?: settings.symbolTextSizeSp
                SymbolDataManager.saveUiSettings(this, settings.copy(symbolTextSizeSp = textSize))
                pagerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun onGroupsChanged(targetGroupIndex: Int? = null) {
        pagerAdapter.notifyDataSetChanged()
        tabLayout.post {
            val maxIndex = pagerAdapter.count - 1
            val target = targetGroupIndex ?: viewPager.currentItem.coerceAtMost(maxIndex)
            if (maxIndex >= 0 && target >= 0) {
                viewPager.currentItem = target
            }
            bindGroupTabLongPressMenus()
        }
    }

    private inner class GroupPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = symbolGroups.size + 1

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

        override fun getPageTitle(position: Int): CharSequence {
            return if (isSettingsPosition(position)) settingsTabTitle else symbolGroups[position].name
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (isSettingsPosition(position)) {
                return createSettingsPage(container).also { container.addView(it) }
            }
            val group = symbolGroups[position]
            val itemsAdapter = ItemsAdapter(position, group)
            val rv = RecyclerView(this@SymbolManagerActivity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(this@SymbolManagerActivity)
                adapter = itemsAdapter
            }

            val callback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    if (isBatchMode) return false
                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition
                    if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                        return false
                    }

                    val moved = group.items.removeAt(fromPosition)
                    group.items.add(toPosition, moved)
                    itemsAdapter.notifyItemMoved(fromPosition, toPosition)
                    SymbolDataManager.saveData(this@SymbolManagerActivity, symbolGroups)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // no-op
                }

                override fun isLongPressDragEnabled(): Boolean = false
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(rv)
            itemsAdapter.attachTouchHelper(touchHelper)

            container.addView(rv)
            return rv
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getItemPosition(`object`: Any): Int = POSITION_NONE
    }

    private inner class ItemsAdapter(
        private val groupIndex: Int,
        private val group: SymbolGroup
    ) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

        private var touchHelper: ItemTouchHelper? = null

        fun attachTouchHelper(helper: ItemTouchHelper) {
            touchHelper = helper
        }

        inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_title)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)
            val dragHandle: View = view.findViewById(R.id.iv_drag_handle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(this@SymbolManagerActivity).inflate(R.layout.item_symbol_manage, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = group.items[position]
            holder.tvTitle.text = item.display

            val shortDesc = getString(
                R.string.symbol_desc_short,
                SymbolDataManager.getActionDesc(this@SymbolManagerActivity, item.shortAction, item.shortText)
            )
            val longDesc = item.longAction?.let {
                getString(
                    R.string.symbol_desc_long,
                    SymbolDataManager.getActionDesc(this@SymbolManagerActivity, it, item.longText)
                )
            } ?: ""
            holder.tvSubtitle.text = shortDesc + longDesc

            val selected = isBatchMode && groupIndex == batchGroupIndex && selectedItems.contains(item)
            holder.itemView.setBackgroundColor(if (selected) Color.parseColor("#66BEEB") else Color.TRANSPARENT)
            val uiSettings = SymbolDataManager.getUiSettings(this@SymbolManagerActivity)
            holder.dragHandle.visibility = if (uiSettings.showDragHandle) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                if (isBatchMode && groupIndex == batchGroupIndex) {
                    toggleSelected(item)
                } else {
                    showEditDialog(group, item)
                }
            }

            holder.itemView.setOnLongClickListener {
                if (isBatchMode && groupIndex == batchGroupIndex) {
                    toggleSelected(item)
                } else {
                    showItemMenu(holder.itemView, group, item)
                }
                true
            }

            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !isBatchMode && uiSettings.showDragHandle) {
                    touchHelper?.startDrag(holder)
                }
                false
            }
        }

        override fun getItemCount() = group.items.size
    }
}
