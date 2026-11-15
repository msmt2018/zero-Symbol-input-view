package com.zerostudio.idea.widget.editor.symbolinput.v2

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerTheme
import com.zerostudio.idea.widget.editor.symbolinput.v2.extension.SymbolDrawerThemeExtension
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import com.zerostudio.idea.widget.editor.symbolinput.v2.settings.SymbolDrawerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SymbolDrawerThemeObserverTest {

    private fun theme(seed: Int) = SymbolDrawerTheme(
        primary = seed,
        onPrimary = seed + 1,
        surface = seed + 2,
        onSurface = seed + 3,
        secondaryText = seed + 4,
        outline = seed + 5,
        drawerBackground = seed + 6,
        selectedCellBackground = seed + 7
    )

    private fun provider(id: String, value: SymbolDrawerTheme) =
        object : SymbolDrawerThemeExtension {
            override val id = id
            override val title = id
            override fun resolve(
                context: android.content.Context,
                settings: SymbolDrawerSettings
            ): SymbolDrawerTheme = value
        }

    @Test
    fun observerPublishesOnlyChangedSnapshotsAndCloseIsIdempotent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            val observed = mutableListOf<SymbolDrawerTheme?>()
            lateinit var drawer: SymbolDrawerView
            lateinit var observer: AutoCloseable
            val expected = theme(100)

            scenario.onActivity { activity ->
                drawer = SymbolDrawerView(activity)
                activity.content.addView(drawer)
                observer = drawer.observeTheme { observed += it }

                drawer.extensions.registerTheme(provider("observer.theme", expected))
                assertTrue(drawer.setThemeExtension("observer.theme"))
                assertEquals(expected, drawer.themeSnapshot)
                drawer.refreshHostTheme()
            }

            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(listOf(null, expected), observed)
                observer.close()
                observer.close()
                drawer.refreshHostTheme()
                assertTrue(drawer.setThemeExtension(null))
                assertNull(drawer.themeSnapshot)
                assertEquals(listOf(null, expected), observed)
            }
        }
    }

    @Test
    fun replacingAndUnregisteringSelectedThemeUpdatesTheLiveSnapshot() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            lateinit var drawer: SymbolDrawerView
            lateinit var newHandle: AutoCloseable
            val firstTheme = theme(200)
            val replacementTheme = theme(300)

            scenario.onActivity { activity ->
                drawer = SymbolDrawerView(activity)
                activity.content.addView(drawer)
                val oldHandle = drawer.extensions.registerTheme(
                    provider("replace.theme", firstTheme)
                )
                assertTrue(drawer.setThemeExtension("replace.theme"))

                newHandle = drawer.extensions.registerTheme(
                    provider("replace.theme", replacementTheme)
                )
                // A stale registration handle must not remove a same-ID replacement.
                oldHandle.close()
            }

            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(replacementTheme, drawer.themeSnapshot)
                newHandle.close()
            }

            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertNull(drawer.themeSnapshot)
            }
        }
    }
}
