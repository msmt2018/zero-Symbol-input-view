package android.zero.studio.widget.editor.symbolinput.v2.state

import android.zero.studio.widget.editor.symbolinput.v2.model.DrawerConfig
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolEntry
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolGroup
import android.zero.studio.widget.editor.symbolinput.v2.model.SymbolAction
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerGeometryTest {
    private val config = DrawerConfig(columns = 4, collapsedRows = 2, cellHeightDp = 30)

    @Test
    fun collapsedHeightUsesConfiguredRows() {
        assertEquals(70, DrawerGeometry.collapsedHeightPx(config, 1f))
    }

    @Test
    fun expandedHeightUsesLargestGroup() {
        val groups = listOf(
            group("a", 5),
            group("b", 9)
        )
        assertEquals(102, DrawerGeometry.expandedHeightPx(groups, config, 1f))
    }

    @Test
    fun indicatorRevealIsClampedAndInterpolated() {
        assertEquals(0f, DrawerGeometry.indicatorReveal(0f, config), 0.0001f)
        assertEquals(0.315f, DrawerGeometry.indicatorReveal(0.315f, config), 0.0001f)
        assertEquals(1f, DrawerGeometry.indicatorReveal(1f, config), 0.0001f)
    }

    private fun group(id: String, count: Int) = SymbolGroup(
        id = id,
        title = id,
        symbols = (0 until count).map {
            SymbolEntry(it.toLong(), it.toString(), SymbolAction(0, it.toString()))
        }
    )
}
