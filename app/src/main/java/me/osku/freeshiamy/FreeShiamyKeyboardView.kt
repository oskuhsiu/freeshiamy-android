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

    private var heightScale: Float = 1f

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setHeightScale(scale: Float) {
        val newScale = scale.coerceIn(0.8f, 1.4f)
        if (newScale == heightScale) return
        heightScale = newScale
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (heightScale == 1f) return
        val scaledHeight = (measuredHeight * heightScale).roundToInt()
        setMeasuredDimension(measuredWidth, scaledHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (heightScale == 1f) {
            super.onDraw(canvas)
            return
        }
        val saveCount = canvas.save()
        canvas.scale(1f, heightScale)
        super.onDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (heightScale == 1f) return super.onTouchEvent(me)
        val scaled = MotionEvent.obtain(me)
        scaled.setLocation(me.x, me.y / heightScale)
        val handled = super.onTouchEvent(scaled)
        scaled.recycle()
        return handled
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        return if (key.codes.get(0) === Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null)
            true
        } else {
            super.onLongPress(key)
        }
    }

    fun setSubtypeOnSpaceKey(subtype: InputMethodSubtype) {
        val keyboard: FreeShiamyKeyboard = getKeyboard() as FreeShiamyKeyboard
        keyboard.setSpaceIcon(getResources().getDrawable(subtype.iconResId))
        invalidateAllKeys()
    }

    companion object{
        val KEYCODE_OPTIONS = -100
        val KEYCODE_LANGUAGE_SWITCH = -101
    }
}
