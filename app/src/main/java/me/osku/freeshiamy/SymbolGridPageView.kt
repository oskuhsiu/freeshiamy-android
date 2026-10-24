package me.osku.freeshiamy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.inputmethodservice.Keyboard
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import me.osku.freeshiamy.catalog.CatalogItem
import me.osku.freeshiamy.catalog.CatalogMode
import kotlin.math.abs

/** One attached page owns only its own strings, gesture, and alternate window. */
internal class SymbolGridPageView(
    context: Context,
    private val mode: CatalogMode,
    private val items: List<CatalogItem>,
    private val listener: SymbolPickerView.Listener,
    private val hapticsEnabled: () -> Boolean,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val columns = when (mode) {
        CatalogMode.SYMBOLS -> 10
        CatalogMode.EMOJI -> 7
        CatalogMode.EMOTICONS -> 4
    }
    private val deleteId = mode.capacity
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawBounds = Rect()
    private val textMetrics = Paint.FontMetrics()
    private val pressedState = intArrayOf(android.R.attr.state_pressed)
    private val normalState = intArrayOf()
    private val keyBackground = context.getDrawable(R.drawable.keyboard_key_background)!!
    private val deleteIcon = context.getDrawable(R.drawable.ic_keyboard_backspace)!!
    private val popup = KeyAlternatesPopup(this)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var pointerId = -1
    private var pressedId = ExploreByTouchHelper.INVALID_ID
    private var downX = 0f
    private var downY = 0f
    private var deleteHeld = false
    private var repeatingDelete = false
    private var cancelled = false
    private val accessibility = CellAccessibility()
    private val deleteRepeat = object : Runnable {
        override fun run() {
            if (pressedId != deleteId || pointerId < 0 || cancelled) return
            repeatingDelete = true
            listener.onKey(Keyboard.KEYCODE_DELETE)
            if (pressedId == deleteId && pointerId >= 0 && !cancelled) postDelayed(this, 50L)
        }
    }
    private val longPress = Runnable {
        val id = pressedId
        if (pointerId >= 0 && !cancelled && id != deleteId) showVariants(id, accessible = false)
    }

    init {
        isFocusable = true
        ViewCompat.setAccessibilityDelegate(this, accessibility)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (id in items.indices) drawCell(canvas, id)
        drawCell(canvas, deleteId)
    }

    private fun drawCell(canvas: Canvas, id: Int) {
        val bounds = cellBounds(id, drawBounds)
        keyBackground.bounds = bounds
        keyBackground.state = if (pressedId == id) pressedState else normalState
        keyBackground.draw(canvas)
        if (id == deleteId) {
            val size = minOf(dp(24), bounds.width() - dp(8), bounds.height() - dp(8)).coerceAtLeast(1)
            val left = bounds.centerX() - size / 2
            val top = bounds.centerY() - size / 2
            deleteIcon.setBounds(left, top, left + size, top + size)
            deleteIcon.draw(canvas)
        } else {
            val text = items[id].text
            paint.color = resources.getColor(R.color.candidate_normal, null)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = (if (mode == CatalogMode.EMOTICONS) 19f else 25f) * resources.displayMetrics.scaledDensity
            val availableWidth = (bounds.width() - dp(8)).coerceAtLeast(1)
            val measured = paint.measureText(text)
            if (measured > availableWidth) paint.textSize *= availableWidth / measured
            paint.getFontMetrics(textMetrics)
            val availableHeight = (bounds.height() - dp(8)).coerceAtLeast(1)
            val textHeight = textMetrics.descent - textMetrics.ascent
            if (textHeight > availableHeight) paint.textSize *= availableHeight / textHeight
            paint.getFontMetrics(textMetrics)
            canvas.drawText(text, bounds.exactCenterX(),
                bounds.exactCenterY() - (textMetrics.ascent + textMetrics.descent) / 2f, paint)
            if (items[id].variants.isNotEmpty()) {
                paint.color = resources.getColor(R.color.candidate_other, null)
                canvas.drawCircle(bounds.right - dp(6).toFloat(), bounds.bottom - dp(6).toFloat(), density, paint)
            }
        }
    }

    private fun cellBounds(id: Int, result: Rect = Rect()): Rect {
        val row = if (id == deleteId) 2 else id / columns
        val column = if (id == deleteId) mode.capacity - columns * 2 else id % columns
        val left = width * column / columns
        val right = if (id == deleteId) width else width * (column + 1) / columns
        result.set(left, height * row / 3, right, height * (row + 1) / 3)
        return result
    }

    private fun cellAt(x: Float, y: Float): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return ExploreByTouchHelper.INVALID_ID
        if (cellBounds(deleteId).contains(x.toInt(), y.toInt())) return deleteId
        val id = (y * 3 / height).toInt() * columns + (x * columns / width).toInt()
        return if (id in items.indices) id else ExploreByTouchHelper.INVALID_ID
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelGesture()
                pointerId = event.getPointerId(0)
                cancelled = false
                downX = event.x
                downY = event.y
                pressedId = cellAt(event.x, event.y)
                if (pressedId != ExploreByTouchHelper.INVALID_ID) haptic()
                if (pressedId == deleteId) {
                    deleteHeld = true
                    listener.onDeleteTouch(true)
                    postDelayed(deleteRepeat, ViewConfiguration.getLongPressTimeout().toLong())
                } else if (items.getOrNull(pressedId)?.variants?.isNotEmpty() == true) {
                    postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId < 0 || cancelled) return true
                val index = event.findPointerIndex(pointerId)
                if (index < 0) cancelGesture() else if (popup.isShowing) {
                    if (event.getX(index) < 0 || event.getX(index) >= width || event.getY(index) >= height) {
                        cancelGesture()
                        return true
                    }
                    popup.move(event.getX(index), event.getY(index))
                } else if (abs(event.getX(index) - downX) > slop || abs(event.getY(index) - downY) > slop ||
                    cellAt(event.getX(index), event.getY(index)) != pressedId) {
                    cancelGesture()
                }
            }
            MotionEvent.ACTION_UP -> {
                val index = event.findPointerIndex(pointerId)
                val id = pressedId
                val text = if (!cancelled && index >= 0 && popup.isShowing &&
                    event.getX(index) >= 0 && event.getX(index) < width && event.getY(index) < height) {
                    popup.move(event.getX(index), event.getY(index))
                    popup.selection()
                } else null
                val click = !cancelled && id != ExploreByTouchHelper.INVALID_ID && index >= 0 && !popup.isShowing &&
                    cellAt(event.getX(index), event.getY(index)) == id && !repeatingDelete
                // Clear the owner before dispatch: a key can synchronously replace this page.
                cancelGesture()
                if (text != null) listener.onText(text)
                else if (click) activate(id)
                performClick()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> cancelGesture()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun activate(id: Int) {
        if (id == deleteId) listener.onKey(Keyboard.KEYCODE_DELETE)
        else items.getOrNull(id)?.let { listener.onText(it.text) }
        accessibility.sendEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    private fun showVariants(id: Int, accessible: Boolean): Boolean {
        val item = items.getOrNull(id) ?: return false
        val variants = item.variants.take(12)
        if (variants.isEmpty()) return false
        val callback: ((String) -> Unit)? = if (accessible) ({ text ->
            cancelGesture()
            haptic()
            listener.onText(text)
        }) else null
        if (!popup.show(cellBounds(id), variants, downX, 0, callback)) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        haptic()
        return true
    }

    fun handleBack(): Boolean {
        if (!popup.isShowing) return false
        cancelGesture()
        return true
    }

    fun release() = cancelGesture()

    private fun cancelGesture() {
        removeCallbacks(longPress)
        removeCallbacks(deleteRepeat)
        pointerId = -1
        pressedId = ExploreByTouchHelper.INVALID_ID
        repeatingDelete = false
        cancelled = true
        popup.dismiss()
        parent?.requestDisallowInterceptTouchEvent(false)
        if (deleteHeld) {
            deleteHeld = false
            listener.onDeleteTouch(false)
        }
        invalidate()
    }

    private fun haptic() {
        if (hapticsEnabled()) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        accessibility.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    private fun dp(value: Int) = (value * density).toInt()

    private inner class CellAccessibility : ExploreByTouchHelper(this@SymbolGridPageView) {
        override fun getVirtualViewAt(x: Float, y: Float) = cellAt(x, y)

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            virtualViewIds.addAll(items.indices)
            virtualViewIds.add(deleteId)
        }

        override fun onPopulateNodeForVirtualView(id: Int, node: AccessibilityNodeInfoCompat) {
            node.className = "android.widget.Button"
            node.contentDescription = if (id == deleteId) context.getString(R.string.catalog_delete)
                else items.getOrNull(id)?.let { item ->
                    if (item.variants.isEmpty()) item.text
                    else context.getString(R.string.catalog_item_with_variants, item.text, item.variants.joinToString(" "))
                }.orEmpty()
            node.setBoundsInParent(cellBounds(id))
            node.isClickable = true
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            if (items.getOrNull(id)?.variants?.isNotEmpty() == true) {
                node.isLongClickable = true
                node.addAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
            }
        }

        override fun onPerformActionForVirtualView(id: Int, action: Int, arguments: Bundle?): Boolean =
            when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                    haptic()
                    activate(id)
                    true
                }
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> showVariants(id, accessible = true)
                else -> false
            }
    }
}
