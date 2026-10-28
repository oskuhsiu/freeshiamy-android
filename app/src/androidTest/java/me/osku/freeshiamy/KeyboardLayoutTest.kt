package me.osku.freeshiamy

import android.view.LayoutInflater
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardLayoutTest {
    @Test
    fun widthChangesAndSameSizeReplacementPreserveStaggeredRows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val root = LayoutInflater.from(context).inflate(R.layout.input, null)
            val view = root.findViewById<FreeShiamyKeyboardView>(R.id.keyboard)
            // Repeating each resource replaces the keyboard while the view keeps the same bounds.
            for (resource in listOf(
                R.xml.qwerty_familiar, R.xml.qwerty_familiar,
                R.xml.qwerty_familiar_no_number, R.xml.qwerty_familiar_no_number,
            )) {
                val keyboard = FreeShiamyKeyboard(context, resource)
                val originalWidth = keyboard.minWidth
                val keys = keyboard.keys.associateBy { it.codes[0] }
                view.keyboard = keyboard

                for (width in listOf(originalWidth / 2, originalWidth, originalWidth * 3 / 4, originalWidth / 2)) {
                    view.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST),
                    )
                    assertEquals("keyboard retained its previous measured width", width, view.measuredWidth)
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                    val pitch = width / 10f
                    val a = keys.getValue('a'.code)
                    val s = keys.getValue('s'.code)
                    val l = keys.getValue('l'.code)
                    assertEquals("q row key width changed at width=$width", pitch, keys.getValue('q'.code).width.toFloat(), 1f)
                    assertEquals("a row lost its leading half-key gap at width=$width", pitch / 2, a.x.toFloat(), 1f)
                    assertEquals("a row key width differs from QWERTY at width=$width", pitch, a.width.toFloat(), 1f)
                    assertEquals("gap between a and s changed at width=$width", pitch, (s.x - a.x).toFloat(), 1f)
                    assertTrue("l is clipped at width=$width", l.x + l.width <= width)
                    assertEquals("z row lost its stagger at width=$width", pitch, keys.getValue('z'.code).x.toFloat(), 1f)
                    assertTrue("a touch bounds moved at width=$width", a.isInside(pitch.toInt(), a.y + a.height / 2))
                }
            }
            view.closing()
        }
    }
}
