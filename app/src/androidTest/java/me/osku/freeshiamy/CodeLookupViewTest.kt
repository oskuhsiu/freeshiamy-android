package me.osku.freeshiamy

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.osku.freeshiamy.engine.CinEntry
import me.osku.freeshiamy.engine.ShiamyEngine
import me.osku.freeshiamy.engine.SpellEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class CodeLookupViewTest {
    @Test
    fun shortViewportKeepsTheFirstCharacterAndCodeFullyVisible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, TestPanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        try {
            val laidOut = CountDownLatch(1)
            lateinit var lookup: CodeLookupView
            instrumentation.runOnMainSync {
                val density = activity.resources.displayMetrics.density
                fun dp(value: Int) = (value * density).roundToInt()
                lookup = CodeLookupView(activity).apply {
                    setEngines(
                        ShiamyEngine(listOf(CinEntry("pns", "你", 0))),
                        SpellEngine(listOf(CinEntry("su3", "你", 0))),
                        loading = false,
                    )
                    open(null)
                    inputText("su3")
                    doOnLayout { laidOut.countDown() }
                }
                activity.setContentView(FrameLayout(activity).apply {
                    addView(lookup, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(232)))
                })
            }
            assertTrue("Lookup did not lay out", laidOut.await(5, TimeUnit.SECONDS))

            instrumentation.runOnMainSync {
                val grid = lookup.findViewById<GridLayout>(R.id.lookup_results)
                val cell = grid.getChildAt(0) as ViewGroup
                val heading = cell.getChildAt(0) as ViewGroup
                val label = heading.getChildAt(0) as TextView
                val visible = Rect()
                assertTrue("Candidate is outside the viewport", label.getLocalVisibleRect(visible))
                assertTrue("Character/code row is clipped: $visible, height=${label.height}",
                    visible.top == 0 && visible.bottom == label.height)
                assertTrue("Code line is missing", label.layout.lineCount >= 2)
                val codeBottom = label.totalPaddingTop + label.layout.getLineBottom(1)
                assertTrue("Code line is cut by the keypad", codeBottom <= visible.bottom)
                assertTrue("Lookup must retain its input keys", lookup.findViewById<View>(R.id.lookup_keyboard).height > 0)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
