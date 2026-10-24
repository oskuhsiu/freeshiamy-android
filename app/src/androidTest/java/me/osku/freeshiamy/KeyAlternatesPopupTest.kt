package me.osku.freeshiamy

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class KeyAlternatesPopupTest {
    @Test
    fun selectsDisplayedCellsAfterWindowRelocation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, TestPanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        var popup: KeyAlternatesPopup? = null
        try {
            lateinit var anchor: View
            val anchorReady = CountDownLatch(1)
            instrumentation.runOnMainSync {
                anchor = View(activity)
                activity.setContentView(FrameLayout(activity).apply {
                    addView(anchor, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                    ))
                })
                anchor.doOnLayout { anchorReady.countDown() }
            }
            assertTrue("Anchor did not lay out", anchorReady.await(5, TimeUnit.SECONDS))

            val options = (0 until 15).map(Int::toString)
            val gridReady = CountDownLatch(1)
            lateinit var grid: GridLayout
            var requestedTop = 0
            instrumentation.runOnMainSync {
                val density = anchor.resources.displayMetrics.density
                fun dp(value: Int) = (value * density).roundToInt()
                val anchorLocation = IntArray(2)
                anchor.getLocationOnScreen(anchorLocation)
                // Three rows above a key at the top of the activity cannot fit on screen.
                requestedTop = anchorLocation[1] - dp(56) * 3 - dp(4) * 2 - dp(6)
                popup = KeyAlternatesPopup(anchor).also {
                    assertTrue(it.show(Rect(dp(48), 0, dp(96), dp(56)), options, dp(72).toFloat(), 3))
                }
                val windowField = KeyAlternatesPopup::class.java.getDeclaredField("window").apply {
                    isAccessible = true
                }
                grid = (windowField.get(popup) as PopupWindow).contentView as GridLayout
                grid.doOnLayout { gridReady.countDown() }
            }
            assertTrue("Popup did not lay out", gridReady.await(5, TimeUnit.SECONDS))

            instrumentation.runOnMainSync {
                val menu = checkNotNull(popup)
                val anchorLocation = IntArray(2)
                val cellLocation = IntArray(2)
                anchor.getLocationOnScreen(anchorLocation)
                grid.getLocationOnScreen(cellLocation)
                assertTrue("WindowManager did not relocate the popup", cellLocation[1] > requestedTop)
                assertEquals("Default selection changed", options[3], menu.selection())
                assertEquals(15, grid.childCount)
                for (index in 0 until grid.childCount) {
                    val cell = grid.getChildAt(index) as TextView
                    cell.getLocationOnScreen(cellLocation)
                    menu.move(
                        cellLocation[0] - anchorLocation[0] + cell.width / 2f,
                        cellLocation[1] - anchorLocation[1] + cell.height / 2f,
                    )
                    assertEquals("Hit testing displayed cell ${cell.text}", cell.text.toString(), menu.selection())
                }
                menu.move(-anchor.width.toFloat(), -anchor.height.toFloat())
                assertNull("Moving outside should cancel selection", menu.selection())
            }
        } finally {
            instrumentation.runOnMainSync {
                popup?.dismiss()
                activity.finish()
            }
        }
    }
}
