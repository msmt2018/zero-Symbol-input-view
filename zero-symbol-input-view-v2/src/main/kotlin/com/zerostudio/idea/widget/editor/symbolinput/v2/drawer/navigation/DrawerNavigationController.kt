package com.zerostudio.idea.widget.editor.symbolinput.v2.drawer.navigation

/**
 * UI-agnostic page selection model. Any TabLayout, RecyclerView or custom
 * navigation view can observe and bind to this controller.
 */
class DrawerNavigationController {
    fun interface Listener {
        fun onNavigationChanged(snapshot: DrawerNavigationSnapshot, selectedIndex: Int)
    }

    private var pages: List<DrawerPage> = emptyList()
    private var selectedId: String? = null
    private val listeners = LinkedHashSet<Listener>()

    fun submitPages(value: List<DrawerPage>, preserveSelection: Boolean = true) {
        require(value.map { it.id }.distinct().size == value.size) {
            "Drawer page IDs must be unique"
        }
        val previous = selectedId
        pages = value.toList()
        selectedId = if (preserveSelection && previous != null && pages.any { it.id == previous }) {
            previous
        } else {
            pages.firstOrNull()?.id
        }
        dispatch()
    }

    fun selectPage(id: String): Boolean {
        if (pages.none { it.id == id }) return false
        if (selectedId == id) return true
        selectedId = id
        dispatch()
        return true
    }

    fun selectPage(index: Int): Boolean {
        val page = pages.getOrNull(index) ?: return false
        return selectPage(page.id)
    }

    fun pages(): List<DrawerPage> = pages.toList()
    fun selectedPage(): DrawerPage? = pages.firstOrNull { it.id == selectedId }
    fun selectedIndex(): Int = pages.indexOfFirst { it.id == selectedId }
    fun snapshot() = DrawerNavigationSnapshot(selectedId, pages.map { it.id })

    fun addListener(listener: Listener) {
        listeners += listener
        listener.onNavigationChanged(snapshot(), selectedIndex())
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun dispatch() {
        val state = snapshot()
        val index = selectedIndex()
        listeners.toList().forEach { it.onNavigationChanged(state, index) }
    }
}
