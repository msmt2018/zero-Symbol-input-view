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

    init {
        contentDescription = contentDescriptionLabel
        clipChildren = false
        clipToPadding = false
    }

    fun setContent(content: DrawerPanelContent?) {
        if (panelContent === content) return
        panelContent?.onPanelHidden()
        panelContent?.onPanelDetached()
        contentView?.let { old ->
            if (old.parent === this) removeView(old)
        }
        panelContent = content
        contentView = content?.createView()?.also { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            content.onPanelAttached()
        }
    }

    fun content(): DrawerPanelContent? = panelContent

    fun dispatchPanelVisible(visible: Boolean) {
        if (visible) panelContent?.onPanelVisible() else panelContent?.onPanelHidden()
    }

    override fun onDetachedFromWindow() {
        panelContent?.onPanelHidden()
        super.onDetachedFromWindow()
    }
}
