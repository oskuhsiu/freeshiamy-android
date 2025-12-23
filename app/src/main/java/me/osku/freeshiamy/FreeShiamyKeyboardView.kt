package me.osku.freeshiamy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.roundToInt

/**
 * Created by Osku on 2021/4/9.
 */
class FreeShiamyKeyboardView : KeyboardView {

    private var requestedHeightScale: Float = 1f
    private var effectiveHeightScale: Float = 1f
    private var softDeleteTouchDown: Boolean = false
    private var labelTopAligned: Boolean = false
    private var labelTopPaddingPx: Int = 0
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelCache = ArrayList<CharSequence?>(64)
    private val invalidateLabelCache = ArrayList<CharSequence?>(64)
    private var keyTextSizePx: Int = 0
    private var labelTextSizePx: Int = 0
    private var keyTextColor: Int = Color.WHITE
    private var shadowColor: Int = 0
    private var shadowRadius: Float = 0f

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initLabelPaint(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initLabelPaint(context, attrs)
    }

    fun setHeightScale(scale: Float) {
        val newScale = scale.coerceIn(0.9f, 2.2f)
        if (newScale == requestedHeightScale) return
        requestedHeightScale = newScale
        requestLayout()
        invalidate()
    }

    fun setLabelTopAligned(enabled: Boolean) {
        if (labelTopAligned == enabled) return
        labelTopAligned = enabled
        invalidate()
    }

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
        val saveCount = canvas.save()
        if (effectiveHeightScale != 1f) {
            canvas.scale(1f, effectiveHeightScale)
        }
        if (labelTopAligned) {
            drawWithTopLabels(canvas)
        } else {
            super.onDraw(canvas)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun invalidateKey(keyIndex: Int) {
        if (!labelTopAligned) {
            super.invalidateKey(keyIndex)
            return
        }
        withLabelsSuppressed(invalidateLabelCache) {
            super.invalidateKey(keyIndex)
        }
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (effectiveHeightScale == 1f) {
            handleSoftDeleteTouch(me)
            return super.onTouchEvent(me)
        }

        val scaled = MotionEvent.obtain(me)
        scaled.setLocation(me.x, me.y / effectiveHeightScale)
        handleSoftDeleteTouch(scaled)
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
            Keyboard.KEYCODE_MODE_CHANGE -> {
                getOnKeyboardActionListener().onKey(KEYCODE_EMOJI, null)
                true
            }
            else -> super.onLongPress(key)
        }
    }

    private fun handleSoftDeleteTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                softDeleteTouchDown = isDeleteKeyTouch(event.x, event.y)
                if (softDeleteTouchDown) {
                    notifyImeSoftDeleteTouch(isDown = true)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                if (softDeleteTouchDown) {
                    notifyImeSoftDeleteTouch(isDown = false)
                    softDeleteTouchDown = false
                }
            }
        }
    }

    private fun notifyImeSoftDeleteTouch(isDown: Boolean) {
        val listener = getOnKeyboardActionListener()
        if (listener is FreeShiamyIME) {
            listener.onSoftDeleteTouch(isDown)
        }
    }

    private fun isDeleteKeyTouch(x: Float, y: Float): Boolean {
        val kb = keyboard ?: return false
        val xi = x.toInt()
        val yi = y.toInt()
        for (key in kb.keys) {
            if (key.codes.getOrNull(0) != Keyboard.KEYCODE_DELETE) continue
            if (key.isInside(xi, yi)) return true
        }
        return false
    }

    private fun initLabelPaint(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FreeShiamyKeyboardView)
        keyTextSizePx = a.getDimensionPixelSize(R.styleable.FreeShiamyKeyboardView_android_keyTextSize, 0)
        labelTextSizePx =
            a.getDimensionPixelSize(R.styleable.FreeShiamyKeyboardView_android_labelTextSize, keyTextSizePx)
        keyTextColor = a.getColor(R.styleable.FreeShiamyKeyboardView_android_keyTextColor, Color.WHITE)
        shadowColor = a.getColor(R.styleable.FreeShiamyKeyboardView_android_shadowColor, 0)
        shadowRadius = a.getDimension(R.styleable.FreeShiamyKeyboardView_android_shadowRadius, 0f)
        a.recycle()

        if (keyTextSizePx == 0) {
            keyTextSizePx = (18f * resources.displayMetrics.scaledDensity).roundToInt()
        }
        if (labelTextSizePx == 0) {
            labelTextSizePx = (14f * resources.displayMetrics.scaledDensity).roundToInt()
        }

        labelTopPaddingPx = resources.getDimensionPixelOffset(R.dimen.key_label_top_padding)
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.color = keyTextColor
    }

    private fun drawWithTopLabels(canvas: Canvas) {
        val kb = keyboard
        if (kb == null) {
            super.onDraw(canvas)
            return
        }

        val keys = kb.keys
        withLabelsSuppressed(labelCache) {
            super.onDraw(canvas)
        }

        drawTopLabels(canvas, keys)
    }

    private inline fun withLabelsSuppressed(
        cache: ArrayList<CharSequence?>,
        action: () -> Unit,
    ) {
        val kb = keyboard ?: run {
            action()
            return
        }
        val keys = kb.keys
        cache.clear()
        cache.ensureCapacity(keys.size)
        for (key in keys) {
            cache.add(key.label)
            if (key.label != null) {
                key.label = null
            }
        }
        try {
            action()
        } finally {
            for (i in keys.indices) {
                keys[i].label = cache[i]
            }
        }
    }

    private fun drawTopLabels(canvas: Canvas, keys: List<Keyboard.Key>) {
        labelPaint.color = keyTextColor
        labelPaint.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)

        for (key in keys) {
            val label = key.label ?: continue
            val labelText = label.toString()
            if (labelText.isBlank()) continue

            labelPaint.textSize = if (labelText.length > 1) labelTextSizePx.toFloat() else keyTextSizePx.toFloat()
            val fm = labelPaint.fontMetrics
            val x = key.x + key.width / 2f
            val y = key.y + labelTopPaddingPx - fm.ascent
            canvas.drawText(labelText, x, y, labelPaint)
        }
    }

    companion object{
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_LANGUAGE_SWITCH = -101
        const val KEYCODE_EMOJI = -102
    }
}
