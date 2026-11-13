package com.zerostudio.idea.widget.editor.symbolinput.v2

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.AdaptiveWindowController
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.FoldingFeatureInfo
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowInsetsMode
import com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowPosture
import com.zerostudio.idea.widget.editor.symbolinput.v2.testing.DrawerTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveWindowControllerTest {
    @Test
    fun controllerSharesSnapshotsAndReleasesListeners() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = FrameLayout(activity)
                activity.content.addView(
                    host,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                host.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(900, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(700, android.view.View.MeasureSpec.EXACTLY)
                )
                host.layout(0, 0, 900, 700)

                val controller = AdaptiveWindowController(
                    host,
                    WindowInsetsMode.VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS
                )
                assertNotNull(controller.current)
                val snapshots = mutableListOf<Float>()
                val callback: (com.zerostudio.idea.widget.editor.symbolinput.v2.adaptive.WindowAdaptiveInfo) -> Unit =
                    { snapshots += it.widthDp }
                val subscription = controller.addListener(callback)
                val secondSubscription = controller.addListener(callback)
                assertTrue(snapshots.isNotEmpty())

                host.layout(0, 0, 520, 700)
                assertEquals(520f / activity.resources.displayMetrics.density, snapshots.last(), 0.5f)

                subscription.close()
                val countAfterFirstClose = snapshots.size
                host.layout(0, 0, 500, 700)
                assertTrue("Closing one handle must not remove the duplicate subscription", snapshots.size > countAfterFirstClose)

                secondSubscription.close()
                secondSubscription.close()
                val countAfterAllClosed = snapshots.size
                host.layout(0, 0, 480, 700)
                assertEquals(countAfterAllClosed, snapshots.size)

                controller.updateFoldingFeature(
                    FoldingFeatureInfo(
                        posture = WindowPosture.HALF_OPENED,
                        isSeparating = true,
                        boundsLeftDp = 250f,
                        boundsRightDp = 270f,
                        boundsBottomDp = 700f / activity.resources.displayMetrics.density
                    )
                )
                assertEquals(WindowPosture.HALF_OPENED, controller.current?.foldingFeature?.posture)
                controller.close()
                assertNull(controller.current)
            }
        }
    }
    @Test
    fun closeIsIdempotentAndRejectsFurtherOperations() {
        ActivityScenario.launch(DrawerTestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = FrameLayout(activity)
                activity.content.addView(
                    host,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                host.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY)
                )
                host.layout(0, 0, 600, 800)

                val controller = AdaptiveWindowController(
                    host,
                    WindowInsetsMode.VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS
                )
                controller.close()
                controller.close()
                assertNull(controller.current)
                assertTrue(runCatching { controller.addListener({}) }.isFailure)
                assertTrue(runCatching { controller.refresh() }.isFailure)
                assertTrue(
                    runCatching {
                        controller.updateFoldingFeature(
                            FoldingFeatureInfo(
                                posture = WindowPosture.FLAT,
                                isSeparating = false
                            )
                        )
                    }.isFailure
                )
            }
        }
    }

}
