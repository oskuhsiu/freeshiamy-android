package me.osku.freeshiamy

import android.content.Context
import android.graphics.Canvas
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.InputMethodSubtype
import kotlin.math.roundToInt


/**
 * Created by Osku on 2021/4/9.
 */
class FreeShiamyKeyboardView : KeyboardView {

    private var requestedHeightScale: Float = 1f
    private var effectiveHeightScale: Float = 1f

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setHeightScale(scale: Float) {
        val newScale = scale.coerceIn(0.8f, 2.2f)
        if (newScale == requestedHeightScale) return
        requestedHeightScale = newScale
        requestLayout()
        invalidate()
    }

    fun getEffectiveHeightScale(): Float = effectiveHeightScale

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val baseHeight = measuredHeight
        if (baseHeight <= 0) return

        val desiredHeight = (baseHeight * requestedHeightScale).roundToInt()
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val maxHeight = if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else MeasureSpec.getSize(heightMeasureSpec)
        val finalHeight = desiredHeight.coerceAtMost(maxHeight)

        effectiveHeightScale = finalHeight.toFloat() / baseHeight.toFloat()
        setMeasuredDimension(measuredWidth, finalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (effectiveHeightScale == 1f) {
            super.onDraw(canvas)
            return
        }
        val saveCount = canvas.save()
        canvas.scale(1f, effectiveHeightScale)
        super.onDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (effectiveHeightScale == 1f) return super.onTouchEvent(me)
        val scaled = MotionEvent.obtain(me)
        scaled.setLocation(me.x, me.y / effectiveHeightScale)
        val handled = super.onTouchEvent(scaled)
        scaled.recycle()
        return handled
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        val primaryCode = key.codes.getOrNull(0) ?: return super.onLongPress(key)
        return when (primaryCode) {
            Keyboard.KEYCODE_CANCEL -> {
                getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null)
                true
            }
            KEYCODE_INDENT -> {
                // Long press on indent behaves like Enter/Return (newline or editor action).
                getOnKeyboardActionListener().onKey(10, null)
                true
            }
            else -> super.onLongPress(key)
        }
    }

    fun setSubtypeOnSpaceKey(subtype: InputMethodSubtype) {
        val keyboard: FreeShiamyKeyboard = getKeyboard() as FreeShiamyKeyboard
        keyboard.setSpaceIcon(getResources().getDrawable(subtype.iconResId))
        invalidateAllKeys()
    }

    companion object{
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_LANGUAGE_SWITCH = -101
        const val KEYCODE_INDENT = -102
    }
}
