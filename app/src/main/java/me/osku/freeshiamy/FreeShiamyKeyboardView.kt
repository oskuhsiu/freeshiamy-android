package me.osku.freeshiamy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
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
    private var spaceSwipeEnabled: Boolean = false
    private var spaceSwipeTracking: Boolean = false
    private var spaceSwipeConsumed: Boolean = false
    private var spaceRepeatActive: Boolean = false
    private var spaceSwipeStartX: Float = 0f
    private var spaceSwipeStartY: Float = 0f
    private val spaceSwipeThresholdPx: Float by lazy { 48f * resources.displayMetrics.density }
    private val spaceSwipeVerticalSlopPx: Float by lazy {
        maxOf(ViewConfiguration.get(context).scaledTouchSlop.toFloat(), 12f * resources.displayMetrics.density)
    }
    private val spaceRepeatStartDelayMs: Long by lazy { ViewConfiguration.getLongPressTimeout().toLong() }
    private val spaceRepeatIntervalMs: Long = 50L
    private val spaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (!spaceSwipeTracking || spaceSwipeConsumed) return
            notifyImeSpace()
            postDelayed(this, spaceRepeatIntervalMs)
        }
    }
    private val spaceRepeatStarter = Runnable {
        if (!spaceSwipeTracking || spaceSwipeConsumed) return@Runnable
        spaceRepeatActive = true
        notifyImeSpace()
        postDelayed(spaceRepeatRunnable, spaceRepeatIntervalMs)
    }

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

    fun setSpaceSwipeEnabled(enabled: Boolean) {
        if (spaceSwipeEnabled == enabled) return
        spaceSwipeEnabled = enabled
        if (!enabled) {
            resetSpaceSwipe()
        }
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
            if (handleSpaceSwipe(me)) return true
            handleSoftDeleteTouch(me)
            return super.onTouchEvent(me)
        }

        val scaled = MotionEvent.obtain(me)
        scaled.setLocation(me.x, me.y / effectiveHeightScale)
        val consumed = handleSpaceSwipe(scaled)
        if (consumed) {
            scaled.recycle()
            return true
        }
        handleSoftDeleteTouch(scaled)
        val handled = super.onTouchEvent(scaled)
        scaled.recycle()
        return handled
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        val primaryCode = key.codes.getOrNull(0) ?: return super.onLongPress(key)
        return when (primaryCode) {
            KEYCODE_LANGUAGE_SWITCH -> {
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

    private fun handleSpaceSwipe(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (!spaceSwipeEnabled) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                resetSpaceSwipe()
            }
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSpaceKeyTouch(event.x, event.y)) {
                    spaceSwipeTracking = true
                    spaceSwipeConsumed = false
                    spaceRepeatActive = false
                    spaceSwipeStartX = event.x
                    spaceSwipeStartY = event.y
                    scheduleSpaceRepeat()
                    return true
                } else {
                    resetSpaceSwipe()
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!spaceSwipeTracking || spaceSwipeConsumed) return spaceSwipeConsumed
                val dx = event.x - spaceSwipeStartX
                val dy = abs(event.y - spaceSwipeStartY)
                if (dx >= spaceSwipeThresholdPx &&
                    dy <= spaceSwipeVerticalSlopPx &&
                    isSpaceKeyTouch(event.x, event.y)
                ) {
                    spaceSwipeConsumed = true
                    cancelSpaceRepeat()
                    notifyImeRawSpace()
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                if (spaceSwipeTracking) {
                    if (!spaceSwipeConsumed && !spaceRepeatActive) {
                        notifyImeSpace()
                    }
                    cancelSpaceRepeat()
                    resetSpaceSwipe()
                    return true
                }
                resetSpaceSwipe()
                return false
            }
        }
        return spaceSwipeTracking
    }

    private fun resetSpaceSwipe() {
        spaceSwipeTracking = false
        spaceSwipeConsumed = false
        spaceRepeatActive = false
        cancelSpaceRepeat()
    }

    private fun notifyImeRawSpace() {
        val listener = getOnKeyboardActionListener()
        listener?.onKey(KEYCODE_RAW_SPACE, null)
    }

    private fun notifyImeSpace() {
        val listener = getOnKeyboardActionListener()
        listener?.onKey(32, null)
    }

    private fun scheduleSpaceRepeat() {
        cancelSpaceRepeat()
        postDelayed(spaceRepeatStarter, spaceRepeatStartDelayMs)
    }

    private fun cancelSpaceRepeat() {
        removeCallbacks(spaceRepeatStarter)
        removeCallbacks(spaceRepeatRunnable)
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

    private fun isSpaceKeyTouch(x: Float, y: Float): Boolean {
        val kb = keyboard ?: return false
        val xi = x.toInt()
        val yi = y.toInt()
        for (key in kb.keys) {
            if (key.codes.getOrNull(0) != 32) continue
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
        const val KEYCODE_RAW_SPACE = -120
    }
}
