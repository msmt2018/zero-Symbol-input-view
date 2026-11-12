package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Generic page content host. It owns view attachment and visibility lifecycle,
 * while selection remains in DrawerNavigationController.
 */
class DrawerPageHost @JvmOverloads constructor(
    context: Context,
    val navigationController: DrawerNavigationController = DrawerNavigationController()
) : FrameLayout(context) {
    private val pageViews = LinkedHashMap<String, View>()
    private var visiblePageId: String? = null
    private var hostAttached = false
    private val navigationListener = DrawerNavigationController.Listener { snapshot, _ ->
        render(snapshot.selectedPageId)
    }

    init {
        clipChildren = false
        clipToPadding = false
        navigationController.addListener(navigationListener)
    }

    fun setPages(pages: List<DrawerPage>, preserveSelection: Boolean = true) {
        val validIds = pages.mapTo(HashSet()) { it.id }
        val removedIds = pageViews.keys.filterNot { it in validIds }
        removedIds.forEach { id ->
            pagesOrCurrent(id)?.content?.let { content ->
                content.onPanelHidden()
                content.onPanelDetached()
            }
            pageViews.remove(id)?.let { view ->
                if (view.parent === this) removeView(view)
            }
        }
        navigationController.submitPages(pages, preserveSelection)
    }

    fun selectPage(id: String): Boolean = navigationController.selectPage(id)
    fun selectPage(index: Int): Boolean = navigationController.selectPage(index)
    fun currentPage(): DrawerPage? = navigationController.selectedPage()

    private fun render(pageId: String?) {
        if (pageId == visiblePageId) return
        val previous = navigationController.pages().firstOrNull { it.id == visiblePageId }
        if (hostAttached) previous?.content?.onPanelHidden()
        if (visiblePageId != null) {
            pageViews[visiblePageId]?.let { old ->
                if (old.parent === this) removeView(old)
            }
        }

        visiblePageId = pageId
        val page = navigationController.pages().firstOrNull { it.id == pageId } ?: return
        val isNewView = !pageViews.containsKey(page.id)
        val view = pageViews.getOrPut(page.id) { page.content.createView() }
        (view.parent as? ViewGroup)?.removeView(view)
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        if (isNewView) page.content.onPanelAttached()
        if (hostAttached) page.content.onPanelVisible()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hostAttached = true
        visiblePageId?.let { id ->
            navigationController.pages().firstOrNull { it.id == id }?.content?.onPanelVisible()
        }
    }

    override fun onDetachedFromWindow() {
        if (hostAttached) {
            visiblePageId?.let { id ->
                navigationController.pages().firstOrNull { it.id == id }?.content?.onPanelHidden()
            }
        }
        hostAttached = false
        super.onDetachedFromWindow()
    }

    private fun pagesOrCurrent(id: String): DrawerPage? =
        navigationController.pages().firstOrNull { it.id == id }

    fun dispose() {
        navigationController.removeListener(navigationListener)
        navigationController.pages().forEach { page ->
            page.content.onPanelHidden()
            page.content.onPanelDetached()
        }
        pageViews.values.forEach { view ->
            if (view.parent === this) removeView(view)
        }
        pageViews.clear()
        visiblePageId = null
    }
}
