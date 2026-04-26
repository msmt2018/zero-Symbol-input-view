package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val tabLayout: TabLayout
    private val tabRow: View
    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val groupAdapter = GroupPagerAdapter()
    private var tabMediator: TabLayoutMediator? = null

    private val minRows = 2
    private val maxRows = 5
    private val spanCount = 8
    private var visibleRows = minRows
    private val fullTabHeight by lazy { (44 * resources.displayMetrics.density).roundToInt() }
    private var imeBottomInsetLast = 0
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            val behavior = bottomSheetBehavior ?: return
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                BottomSheetBehavior.STATE_HIDDEN -> {
                    resetTransientOffsets()
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            setExpansionFraction(slideOffset.coerceIn(0f, 1f))
        }
    }
    private var managedBottomSheet: View? = null
    private var managedFollowView: View? = null
    private var managedRootView: View? = null
    private var initialSheetBottomMargin = 0
    private var initialFollowBottomMargin = 0
    private var registeredBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null
    private var lastStableImeBottomInset = 0

    /**
     * Whether this view should follow IME top edge when keyboard expands/collapses.
     * Set true in Activity if bottom sheet needs to move with system IME animation.
     */
    var followSystemIme: Boolean = false
        set(value) {
            field = value
            if (!value) {
                resetTransientOffsets()
            } else {
                managedRootView?.let(ViewCompat::requestApplyInsets)
            }
        }

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.adapter = groupAdapter
        viewPager.offscreenPageLimit = 1
        viewPager.isSaveEnabled = false
        setExpansionFraction(0f)
        refreshData()
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    /**
     * Called by parent BottomSheetBehavior callback.
     * 0f = collapsed, 1f = expanded.
     */
    fun setExpansionFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val targetHeight = max(0, (fullTabHeight * clamped).roundToInt())
        val layoutParams = tabRow.layoutParams
        if (layoutParams.height != targetHeight) {
            layoutParams.height = targetHeight
            tabRow.layoutParams = layoutParams
        }
        tabRow.alpha = clamped
        tabRow.translationY = (1f - clamped) * -8f * resources.displayMetrics.density

        val newRows = minRows + ((maxRows - minRows) * clamped).roundToInt()
        if (newRows != visibleRows) {
            visibleRows = newRows
            groupAdapter.notifyVisibleRowsChanged()
        }
    }

    fun refreshData() {
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            groups.addAll(buildFallbackGroups())
        }
        if (viewPager.currentItem >= groups.size) {
            viewPager.setCurrentItem(0, false)
        }
        groupAdapter.clearPageAdapters()
        groupAdapter.notifyDataSetChanged()
        bindTabs()
    }

    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.let { previousBehavior ->
            registeredBottomSheetCallback?.let { previousCallback ->
                previousBehavior.removeBottomSheetCallback(previousCallback)
            }
        }
        bottomSheetBehavior = behavior
        managedRootView = rootView
        managedBottomSheet = bottomSheet
        managedFollowView = followView
        managedRootView = rootView
        behavior.saveFlags = BottomSheetBehavior.SAVE_NONE
        behavior.isHideable = false
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.isFitToContents = true
        bottomSheet.post {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        val sheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                    BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        resetTransientOffsets()
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setExpansionFraction(slideOffset.coerceIn(0f, 1f))
            }
        }
        behavior.addBottomSheetCallback(sheetCallback)
        registeredBottomSheetCallback = sheetCallback

        val bottomSheetLp = bottomSheet.layoutParams as? MarginLayoutParams
        val followLp = followView?.layoutParams as? MarginLayoutParams
        initialSheetBottomMargin = bottomSheetLp?.bottomMargin ?: 0
        initialFollowBottomMargin = followLp?.bottomMargin ?: 0
        val updateImeFollowMargins: (Int) -> Unit = { imeBottom ->
            bottomSheetLp?.let {
                it.bottomMargin = initialSheetBottomMargin + imeBottom
                bottomSheet.layoutParams = it
            }
            followLp?.let {
                it.bottomMargin = initialFollowBottomMargin + imeBottom
                followView?.layoutParams = it
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (abs(imeBottom - imeBottomInsetLast) > 1 && followSystemIme) {
                updateImeFollowMargins(imeBottom)
                if (imeBottom == 0 && behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            lastStableImeBottomInset = imeBottom
            imeBottomInsetLast = imeBottom
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    if (!followSystemIme) {
                        return insets
                    }
                    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    if (abs(imeBottom - imeBottomInsetLast) > 1) {
                        updateImeFollowMargins(imeBottom)
                        imeBottomInsetLast = imeBottom
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (followSystemIme) {
                        updateImeFollowMargins(lastStableImeBottomInset)
                        imeBottomInsetLast = lastStableImeBottomInset
                    }
                }
            }
        )
        ViewCompat.requestApplyInsets(rootView)
        ViewCompat.requestApplyInsets(bottomSheet)
        followView?.let { ViewCompat.requestApplyInsets(it) }
    }

    fun onHostResume() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun resetTransientOffsets() {
        (managedBottomSheet?.layoutParams as? MarginLayoutParams)?.let {
            if (it.bottomMargin != initialSheetBottomMargin) {
                it.bottomMargin = initialSheetBottomMargin
                managedBottomSheet?.layoutParams = it
            }
        }
        (managedFollowView?.layoutParams as? MarginLayoutParams)?.let {
            if (it.bottomMargin != initialFollowBottomMargin) {
                it.bottomMargin = initialFollowBottomMargin
                managedFollowView?.layoutParams = it
            }
        }
        imeBottomInsetLast = 0
        lastStableImeBottomInset = 0
    }

    private fun buildFallbackGroups(): List<SymbolGroup> {
        return listOf(
            SymbolGroup(
                name = "default",
                items = mutableListOf(
                    SymbolItem(0, "注释", "//"),
                    SymbolItem(18, "←"),
                    SymbolItem(20, "↑"),
                    SymbolItem(19, "→"),
                    SymbolItem(0, "\"", "\""),
                    SymbolItem(0, "'", "'"),
                    SymbolItem(0, ".", "."),
                    SymbolItem(0, ",", ","),
                    SymbolItem(0, "/", "/"),
                    SymbolItem(0, "//", "//"),
                    SymbolItem(21, "↓"),
                    SymbolItem(0, ":", ":"),
                    SymbolItem(0, ";", ";"),
                    SymbolItem(0, "#", "#"),
                    SymbolItem(0, "+", "+"),
                    SymbolItem(0, "-", "-"),
                    SymbolItem(22, "..."),
                )
            )
        )
    }

    private fun bindTabs() {
        detachTabMediatorSafely()
        if (groups.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = groups.getOrNull(position)?.name ?: "Tab ${position + 1}"
        }.apply { attach() }
    }

    private fun detachTabMediatorSafely() {
        val mediator = tabMediator ?: return
        try {
            mediator.detach()
        } catch (_: IllegalStateException) {
            // TabLayoutMediator may already be detached during transient host lifecycle changes.
        }
        tabMediator = null
    }

    private inner class GroupPagerAdapter : RecyclerView.Adapter<GroupPagerAdapter.GroupViewHolder>() {

        private val pageAdapters = mutableMapOf<Int, SymbolAdapter>()

        inner class GroupViewHolder(val rv: RecyclerView) : RecyclerView.ViewHolder(rv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val rv = RecyclerView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setHasFixedSize(true)
                overScrollMode = OVER_SCROLL_NEVER
                isNestedScrollingEnabled = true
                layoutManager = GridLayoutManager(context, spanCount)
                clipToPadding = false
                val horizontal = (8 * resources.displayMetrics.density).roundToInt()
                val vertical = (2 * resources.displayMetrics.density).roundToInt()
                setPadding(horizontal, vertical, horizontal, vertical)
            }
            return GroupViewHolder(rv)
        }

        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            val group = groups.getOrNull(position) ?: return
            val symbolAdapter = pageAdapters.getOrPut(position) { SymbolAdapter(group.items) }
            if (holder.rv.adapter !== symbolAdapter) {
                holder.rv.adapter = symbolAdapter
            }
        }

        override fun getItemCount(): Int = groups.size

        fun clearPageAdapters() {
            pageAdapters.clear()
        }

        fun notifyVisibleRowsChanged() {
            pageAdapters.values.forEach { it.notifyDataSetChanged() }
        }
    }

    override fun onDetachedFromWindow() {
        tabMediator?.detach()
        tabMediator = null
        managedRootView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
            ViewCompat.setWindowInsetsAnimationCallback(it, null)
        }
        managedRootView = null
        bottomSheetBehavior?.let { behavior ->
            registeredBottomSheetCallback?.let { callback ->
                behavior.removeBottomSheetCallback(callback)
            }
        }
        registeredBottomSheetCallback = null
        bottomSheetBehavior = null
        managedBottomSheet = null
        managedFollowView = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (tabMediator == null && groups.isNotEmpty()) {
            bindTabs()
        }
        managedRootView?.let { ViewCompat.requestApplyInsets(it) }
        managedBottomSheet?.let { ViewCompat.requestApplyInsets(it) }
    }

    private inner class SymbolAdapter(private val items: List<SymbolItem>) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {

        inner class SymbolViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
            val tv = TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                minHeight = (34 * resources.displayMetrics.density).roundToInt()
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                isClickable = true
                isFocusable = true

                val tvColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

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
                editor?.let { ed ->
                    SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener)
                }
            }

            holder.tv.setOnLongClickListener {
                if (item.longAction != null) {
                    editor?.let { ed ->
                        SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener)
                    }
                    true
                } else {
                    false
                }
            }
        }

        override fun getItemCount(): Int = items.size.coerceAtMost(spanCount * visibleRows)
    }
}
