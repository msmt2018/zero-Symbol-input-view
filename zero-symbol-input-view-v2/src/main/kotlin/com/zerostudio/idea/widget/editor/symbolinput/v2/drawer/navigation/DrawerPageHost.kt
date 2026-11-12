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
        previous?.content?.onPanelHidden()
        if (visiblePageId != null) {
            pageViews[visiblePageId]?.let { old ->
                if (old.parent === this) removeView(old)
            }
        }

        visiblePageId = pageId
        val page = navigationController.pages().firstOrNull { it.id == pageId } ?: return
        val view = pageViews.getOrPut(page.id) { page.content.createView() }
        (view.parent as? ViewGroup)?.removeView(view)
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        page.content.onPanelAttached()
        page.content.onPanelVisible()
    }

    override fun onDetachedFromWindow() {
        visiblePageId?.let { id ->
            navigationController.pages().firstOrNull { it.id == id }?.content?.onPanelHidden()
        }
        super.onDetachedFromWindow()
    }

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
