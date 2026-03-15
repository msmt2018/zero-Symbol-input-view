package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.github.rosemoe.sora.widget.CodeEditor

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val btnSettings: ImageButton
    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null
    
    private val groups = mutableListOf<SymbolGroup>()
    private val groupAdapter = GroupPagerAdapter()

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        btnSettings = root.findViewById(R.id.btn_symbol_settings)

        viewPager.adapter = groupAdapter
        
        btnSettings.setOnClickListener {
            onOpenManagerListener?.invoke()
        }
        
        refreshData() // 初始化加载
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    // 重新加载数据 (当从管理器Activity返回时调用)
    fun refreshData() {
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData)
        groupAdapter.notifyDataSetChanged()
    }

    private inner class GroupPagerAdapter : RecyclerView.Adapter<GroupPagerAdapter.GroupViewHolder>() {
        inner class GroupViewHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val rv = RecyclerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                overScrollMode = OVER_SCROLL_NEVER 
            }
            return GroupViewHolder(rv)
        }

        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            holder.rv.adapter = SymbolAdapter(groups[position].items)
        }

        override fun getItemCount(): Int = groups.size
    }

    private inner class SymbolAdapter(private val items: List<SymbolItem>) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {
        inner class SymbolViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
            val tv = TextView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                minWidth = (context.resources.displayMetrics.density * 45).toInt()
                gravity = Gravity.CENTER
                textSize = 18f
                isClickable = true
                isFocusable = true
                
                // 动态主题文字色
                val tvColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)
                
                // 动态点击涟漪背景
                val tvBg = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                setBackgroundResource(tvBg.resourceId)
            }
            return SymbolViewHolder(tv)
        }

        override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
            val item = items[position]
            holder.tv.text = item.display

            holder.tv.setOnClickListener {
                editor?.let { ed -> SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener) }
            }

            holder.tv.setOnLongClickListener {
                if (item.longAction != null) {
                    editor?.let { ed -> SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener) }
                    true
                } else false
            }
        }

        override fun getItemCount(): Int = items.size
    }
}