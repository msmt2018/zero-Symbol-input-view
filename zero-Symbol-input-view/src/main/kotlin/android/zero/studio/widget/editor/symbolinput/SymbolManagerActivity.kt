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
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class SymbolManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private var symbolGroups = mutableListOf<SymbolGroup>()
    private lateinit var pagerAdapter: GroupPagerAdapter

    private lateinit var actionValues: IntArray
    private lateinit var actionNames: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        actionValues = resources.getIntArray(R.array.symbol_action_values)
        actionNames = resources.getStringArray(R.array.symbol_action_names)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        symbolGroups = SymbolDataManager.loadData(this)
        
        pagerAdapter = GroupPagerAdapter()
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
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
                Toast.makeText(this, "预留接口，支持标准格式", Toast.LENGTH_SHORT).show()
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

        val longNames = mutableListOf("无长按动作").apply { addAll(actionNames) }
        spShortAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionNames)
        spLongAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, longNames)

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
        pagerAdapter.notifyDataSetChanged()
    }

    private inner class GroupPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = symbolGroups.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

        override fun getPageTitle(position: Int): CharSequence = symbolGroups[position].name

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val rv = RecyclerView(this@SymbolManagerActivity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(this@SymbolManagerActivity)
                adapter = ItemsAdapter(symbolGroups[position])
            }
            container.addView(rv)
            return rv
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
        
        override fun getItemPosition(`object`: Any): Int = POSITION_NONE
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
            
            val shortDesc = "短按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, item.shortAction, item.shortText)}"
            val longDesc = item.longAction?.let { ", 长按: ${SymbolDataManager.getActionDesc(this@SymbolManagerActivity, it, item.longText)}" } ?: ""
            holder.tvSubtitle.text = shortDesc + longDesc

            holder.itemView.setOnClickListener {
                showEditDialog(group, item)
            }
        }

        override fun getItemCount() = group.items.size
    }
}