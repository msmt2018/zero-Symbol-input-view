package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.panel

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Minimal replaceable content host. It deliberately knows nothing about
 * symbols, pages, repositories or action execution.
 */
class DrawerPanelHost @JvmOverloads constructor(
    context: Context,
    val slot: DrawerPanelSlot,
    private val contentDescriptionLabel: CharSequence? = null
) : FrameLayout(context) {
    private var panelContent: DrawerPanelContent? = null
    private var contentView: View? = null
    private var panelVisible = false

    init {
        contentDescription = contentDescriptionLabel
        clipChildren = false
        clipToPadding = false
    }

    fun setContent(content: DrawerPanelContent?) = setContent(content, context)

    fun setContent(content: DrawerPanelContent?, viewContext: Context) {
        if (panelContent === content && contentView != null) return
        replaceContent(content, viewContext)
    }

    fun refreshContentContext(viewContext: Context) {
        val current = panelContent ?: return
        replaceContent(current, viewContext)
    }

    private fun replaceContent(content: DrawerPanelContent?, viewContext: Context) {
        if (panelVisible) panelContent?.onPanelHidden()
        panelVisible = false
        panelContent?.onPanelDetached()
        contentView?.let { old ->
            if (old.parent === this) removeView(old)
        }
        panelContent = content
        contentView = content?.createView(viewContext)?.also { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            content.onPanelAttached()
        }
    }

    fun measureContentHeight(maxWidthPx: Int, maxHeightPx: Int): Int {
        val view = contentView ?: return 0
        view.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidthPx.coerceAtLeast(0), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeightPx.coerceAtLeast(0), View.MeasureSpec.AT_MOST)
        )
        return view.measuredHeight
    }

    fun content(): DrawerPanelContent? = panelContent

    fun dispatchPanelVisible(visible: Boolean) {
        if (panelVisible == visible) return
        panelVisible = visible
        if (visible) panelContent?.onPanelVisible() else panelContent?.onPanelHidden()
    }

    override fun onDetachedFromWindow() {
        dispatchPanelVisible(false)
        super.onDetachedFromWindow()
    }
}
