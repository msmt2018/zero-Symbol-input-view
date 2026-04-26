package android.zero.studio.widget.editor.symbolinput

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout

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
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (isBatchMode && position != batchGroupIndex) {
                    exitBatchMode()
                }
            }
        })
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
                    Toast.makeText(this, "请先导入数据或拥有至少一个分组", Toast.LENGTH_SHORT).show()
                } else {
                    val currentGroup = viewPager.currentItem
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
            R.id.action_import_file, R.id.action_export_file -> {
                Toast.makeText(this, "预留接口，支持标准格式", Toast.LENGTH_SHORT).show()
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

    private fun showEditDialog(group: SymbolGroup, itemToEdit: SymbolItem?) {
        showSymbolDialog(
            title = if (itemToEdit == null) "添加符号" else "编辑符号",
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

        val longNames = mutableListOf("无长按动作").apply { addAll(actionNames) }
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
        pagerAdapter.notifyDataSetChanged()
    }

    private fun invertBatchSelection() {
        if (!isBatchMode || batchGroupIndex !in symbolGroups.indices) return
        val group = symbolGroups[batchGroupIndex]
        val newSelected = linkedSetOf<SymbolItem>()
        group.items.forEach { if (!selectedItems.contains(it)) newSelected.add(it) }
        selectedItems.clear()
        selectedItems.addAll(newSelected)
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

    private fun onGroupsChanged(targetGroupIndex: Int? = null) {
        pagerAdapter.notifyDataSetChanged()
        tabLayout.post {
            val target = targetGroupIndex ?: viewPager.currentItem.coerceAtMost(symbolGroups.lastIndex)
            if (symbolGroups.isNotEmpty() && target >= 0) {
                viewPager.currentItem = target
            }
        }
    }

    private inner class GroupPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = symbolGroups.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

        override fun getPageTitle(position: Int): CharSequence = symbolGroups[position].name

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
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

            val shortDesc = "短按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, item.shortAction, item.shortText)}"
            val longDesc = item.longAction?.let { ", 长按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, it, item.longText)}" } ?: ""
            holder.tvSubtitle.text = shortDesc + longDesc

            val selected = isBatchMode && groupIndex == batchGroupIndex && selectedItems.contains(item)
            holder.itemView.setBackgroundColor(if (selected) Color.parseColor("#66BEEB") else Color.TRANSPARENT)

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
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !isBatchMode) {
                    touchHelper?.startDrag(holder)
                }
                false
            }
        }

        override fun getItemCount() = group.items.size
    }
}
