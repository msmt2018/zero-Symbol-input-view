package android.zero.studio.widget.editor.symbolinput

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// @author android_zero

class SymbolManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var symbolGroups = mutableListOf<SymbolGroup>()
    private lateinit var groupAdapter: GroupPagerAdapter
    private var tabMediator: TabLayoutMediator? = null

    private lateinit var actionValues: IntArray
    private lateinit var actionNames: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_manager)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        actionValues = resources.getIntArray(R.array.symbol_action_values)
        actionNames = resources.getStringArray(R.array.symbol_action_names)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        symbolGroups = SymbolDataManager.loadData(this)
        
        groupAdapter = GroupPagerAdapter()
        viewPager.adapter = groupAdapter

        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = symbolGroups[position].name
        }.apply { attach() }
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
            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_export_clipboard -> exportToClipboard()
            R.id.action_import_file, R.id.action_export_file -> {
                Toast.makeText(this, "文件 I/O 留作以后扩展，目前已完美支持剪贴板标准格式", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
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
            } catch (e: Exception) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(group: SymbolGroup, itemToEdit: SymbolItem?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_symbol_edit, null)
        val etDisplay = view.findViewById<EditText>(R.id.et_display)
        val spShortAction = view.findViewById<Spinner>(R.id.sp_short_action)
        val etShortText = view.findViewById<EditText>(R.id.et_short_text)
        val spLongAction = view.findViewById<Spinner>(R.id.sp_long_action)
        val etLongText = view.findViewById<EditText>(R.id.et_long_text)

        // Setup Spinners
        val longNames = mutableListOf("无长按动作").apply { addAll(actionNames) }
        spShortAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionNames)
        spLongAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, longNames)

        // Pre-fill
        if (itemToEdit != null) {
            etDisplay.setText(itemToEdit.display)
            etShortText.setText(itemToEdit.shortText)
            etLongText.setText(itemToEdit.longText)
            spShortAction.setSelection(actionValues.indexOf(itemToEdit.shortAction).coerceAtLeast(0))
            if (itemToEdit.longAction != null) {
                spLongAction.setSelection(actionValues.indexOf(itemToEdit.longAction!!) + 1)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (itemToEdit == null) "添加符号" else "编辑符号")
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

                if (itemToEdit == null) {
                    group.items.add(newItem)
                } else {
                    val index = group.items.indexOf(itemToEdit)
                    group.items[index] = newItem
                }
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
            }
            .setNeutralButton(R.string.dialog_delete) { _, _ ->
                if (itemToEdit != null) {
                    group.items.remove(itemToEdit)
                    SymbolDataManager.saveData(this, symbolGroups)
                    onGroupsChanged()
                }
            }
            .show()
    }

    private fun onGroupsChanged() {
        groupAdapter.notifyDataSetChanged()
        detachMediatorSafely()
        if (symbolGroups.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        val safeCurrent = viewPager.currentItem.coerceIn(0, symbolGroups.lastIndex)
        if (viewPager.currentItem != safeCurrent) {
            viewPager.setCurrentItem(safeCurrent, false)
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = symbolGroups.getOrNull(position)?.name ?: "Group ${position + 1}"
        }.apply { attach() }
    }

    override fun onDestroy() {
        detachMediatorSafely()
        super.onDestroy()
    }

    private fun detachMediatorSafely() {
        val mediator = tabMediator ?: return
        try {
            mediator.detach()
        } catch (_: IllegalStateException) {
            // Can happen if host is being recreated while mediator is already detached.
        }
        tabMediator = null
    }

    private inner class GroupPagerAdapter : RecyclerView.Adapter<GroupPagerAdapter.GroupViewHolder>() {

        inner class GroupViewHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val rv = RecyclerView(this@SymbolManagerActivity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(this@SymbolManagerActivity)
            }
            return GroupViewHolder(rv)
        }

        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            holder.rv.adapter = ItemsAdapter(symbolGroups.getOrNull(position) ?: return)
        }

        override fun getItemCount() = symbolGroups.size
    }

    private inner class ItemsAdapter(val group: SymbolGroup) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {
        inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_title)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(this@SymbolManagerActivity).inflate(R.layout.item_symbol_manage, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = group.items[position]
            holder.tvTitle.text = item.display
            
            // 构建副标题："短按: xxx, 长按: xxx"
            val shortDesc = "短按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, item.shortAction, item.shortText)}"
            val longDesc = item.longAction?.let { ", 长按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, it, item.longText)}" } ?: ""
            holder.tvSubtitle.text = shortDesc + longDesc

            // 点击编辑
            holder.itemView.setOnClickListener {
                showEditDialog(group, item)
            }
        }

        override fun getItemCount() = group.items.size
    }
}