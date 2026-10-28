package me.osku.freeshiamy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Created by Osku on 2021/4/9.
 */
class FreeShiamyKeyboardView : KeyboardView {

    private var requestedHeightScale: Float = 1f
    private var keyHapticFeedbackEnabled: Boolean = true
    private var softDeleteTouchDown: Boolean = false
    private val spaceBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val symbolHintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var alternatesPopup: KeyAlternatesPopup? = null
    private var nativePointerId = -1
    private var alternatePointerId = -1
    private var lastNativeTouchX = 0f
    private var lastNativeTouchY = 0f
    private var drainingAlternateTouch = false
    private var spaceSwipeEnabled: Boolean = false
    private var spaceSwipeTracking: Boolean = false
    private var spaceSwipeConsumed: Boolean = false
    private var spaceRepeatActive: Boolean = false
    private var pressedSpaceKey: Keyboard.Key? = null
    private var rolloverActive: Boolean = false
    private var rolloverPointerId: Int = -1
    private var rolloverDownTime: Long = 0L
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
            if (spaceSwipeTracking && !spaceSwipeConsumed) postDelayed(this, spaceRepeatIntervalMs)
        }
    }
    private val spaceRepeatStarter = Runnable {
        if (!spaceSwipeTracking || spaceSwipeConsumed) return@Runnable
        spaceRepeatActive = true
        notifyImeSpace()
        if (spaceSwipeTracking && !spaceSwipeConsumed) postDelayed(spaceRepeatRunnable, spaceRepeatIntervalMs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setHeightScale(scale: Float) {
        val newScale = scale.coerceIn(0.9f, 2.2f)
        if (newScale == requestedHeightScale) return
        requestedHeightScale = newScale
        requestLayout()
        invalidate()
    }

    fun setKeyHapticFeedbackEnabled(enabled: Boolean) {
        keyHapticFeedbackEnabled = enabled
    }

    fun setOutputModeLabel(simplified: Boolean) {
        keyboard.keys.first { it.codes[0] == 32 }.label = context.getString(
            if (simplified) R.string.keyboard_mode_simplified else R.string.keyboard_mode_traditional,
        )
        invalidateAllKeys()
    }

    fun setLetterShiftState(shifted: Boolean, capsLocked: Boolean) {
        isShifted = shifted
        val shiftKey = keyboard?.keys?.firstOrNull {
            it.codes.firstOrNull() == Keyboard.KEYCODE_SHIFT
        } ?: return
        shiftKey.icon = context.getDrawable(when {
            capsLocked -> R.drawable.ic_keyboard_caps_lock
            shifted -> R.drawable.ic_keyboard_shift_on
            else -> R.drawable.ic_keyboard_shift
        })
        invalidateAllKeys()
    }

    fun performKeyHapticFeedback() {
        if (keyHapticFeedbackEnabled) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun setSpaceSwipeEnabled(enabled: Boolean) {
        if (spaceSwipeEnabled == enabled) return
        spaceSwipeEnabled = enabled
        if (!enabled && spaceSwipeTracking) {
            // Committing a candidate may disable the gesture during this very press. Keep
            // ownership until UP/CANCEL, without passing an orphan UP to KeyboardView.
            spaceSwipeConsumed = true
            cancelSpaceRepeat()
            releaseSpaceKey()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val maxHeight = if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else
            (MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom).coerceAtLeast(1)
        val changed = (keyboard as? FreeShiamyKeyboard)?.updateKeyHeights(
            requestedHeightScale, resources.displayMetrics.density, maxHeight,
        ) == true
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, resolveSize(measuredHeight, heightMeasureSpec))
        if (changed) invalidateAllKeys()
    }

    override fun onDraw(canvas: Canvas) {
        keyboard?.keys?.firstOrNull { it.codes.firstOrNull() == 32 && it.width > 0 }?.let { key ->
            val insetX = 2f * resources.displayMetrics.density
            val insetY = key.height * 0.22f
            val radius = 4f * resources.displayMetrics.density
            spaceBarPaint.color = resources.getColor(R.color.keyboard_space_background)
            canvas.drawRoundRect(key.x + insetX, key.y + insetY,
                key.x + key.width - insetX, key.y + key.height - insetY, radius, radius, spaceBarPaint)
        }
        withoutLetterLabels { super.onDraw(canvas) }
        letterPaint.apply {
            color = resources.getColor(R.color.candidate_normal)
            textAlign = Paint.Align.CENTER
        }
        symbolHintPaint.apply {
            color = resources.getColor(R.color.candidate_other)
            textAlign = Paint.Align.CENTER
        }
        val mainSize = resources.getDimension(R.dimen.keyboard_letter_text_size)
        val hintSize = resources.getDimension(R.dimen.keyboard_symbol_hint_text_size)
        val gap = 2f * resources.displayMetrics.density
        val padding = 4f * resources.displayMetrics.density
        keyboard?.keys?.forEach { key ->
            val hint = KeyAlternates.hintForCode(key.codes.firstOrNull() ?: 0) ?: return@forEach
            val label = key.label?.toString() ?: return@forEach
            letterPaint.textSize = mainSize
            symbolHintPaint.textSize = hintSize
            val mainMetrics = letterPaint.fontMetrics
            val hintMetrics = symbolHintPaint.fontMetrics
            val textHeight = mainMetrics.descent - mainMetrics.ascent + hintMetrics.descent - hintMetrics.ascent
            val available = (key.height - padding).coerceAtLeast(gap + 1f)
            val fit = minOf(1f, (available - gap) / textHeight)
            letterPaint.textSize = mainSize * fit
            symbolHintPaint.textSize = hintSize * fit
            val main = letterPaint.fontMetrics
            val secondary = symbolHintPaint.fontMetrics
            val groupHeight = main.descent - main.ascent + gap + secondary.descent - secondary.ascent
            val baseline = key.y + (key.height - groupHeight) / 2f - main.ascent
            val center = key.x + key.width / 2f
            canvas.drawText(if (isShifted) label.uppercase() else label, center, baseline, letterPaint)
            canvas.drawText(hint, center, baseline + main.descent + gap - secondary.ascent, symbolHintPaint)
        }
    }

    override fun invalidateKey(keyIndex: Int) {
        // Framework can redraw its private buffer synchronously on press/release.
        withoutLetterLabels { super.invalidateKey(keyIndex) }
    }

    private inline fun withoutLetterLabels(draw: () -> Unit) {
        val labels = keyboard?.keys?.filter {
            it.label != null && KeyAlternates.hintForCode(it.codes.firstOrNull() ?: 0) != null
        }?.map { it to it.label }.orEmpty()
        labels.forEach { (key, _) -> key.label = null }
        try {
            draw()
        } finally {
            // Native preview and accessibility callbacks need the original labels.
            labels.forEach { (key, label) -> key.label = label }
        }
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (drainingAlternateTouch) {
            if (me.actionMasked == MotionEvent.ACTION_DOWN) drainingAlternateTouch = false
            else {
                if (me.actionMasked == MotionEvent.ACTION_UP || me.actionMasked == MotionEvent.ACTION_CANCEL) {
                    drainingAlternateTouch = false
                }
                return true
            }
        }
        if (alternatesPopup?.isShowing == true) return handleAlternateTouch(me)
        if (me.actionMasked == MotionEvent.ACTION_DOWN) updateKeyPreview(me.x, me.y)
        if (rolloverActive) return handleRolloverTouch(me)
        if (handleSpaceSwipe(me)) return true
        handleSoftDeleteTouch(me)
        recordNativeTouch(me, me.getPointerId(0))
        return super.onTouchEvent(me)
    }

    override fun setKeyboard(keyboard: Keyboard) {
        resetCustomTouches()
        super.setKeyboard(keyboard)
    }

    override fun closing() {
        resetCustomTouches()
        super.closing()
    }

    override fun onDetachedFromWindow() {
        closing()
        super.onDetachedFromWindow()
    }

    private fun resetCustomTouches() {
        // Keep draining across a synchronous keyboard change until the physical gesture ends.
        drainingAlternateTouch = drainingAlternateTouch || alternatesPopup?.isShowing == true
        alternatesPopup?.dismiss()
        alternatePointerId = -1
        nativePointerId = -1
        resetSpaceSwipe()
        rolloverActive = false
        rolloverPointerId = -1
        if (softDeleteTouchDown) notifyImeSoftDeleteTouch(isDown = false)
        softDeleteTouchDown = false
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        val primaryCode = key.codes.getOrNull(0) ?: return super.onLongPress(key)
        val alphabet = keyboard?.keys?.any {
            (it.codes.firstOrNull() ?: 0) in 'a'.code..'z'.code
        } == true
        KeyAlternates.forCode(primaryCode, isShifted, alphabet)?.let { choices ->
            val popup = alternatesPopup ?: KeyAlternatesPopup(this).also { alternatesPopup = it }
            val default = KeyAlternates.defaultSelection(primaryCode, alphabet)
            if (popup.show(key, choices, lastNativeTouchX, default)) {
                alternatePointerId = nativePointerId
                return true
            }
        }
        return when (primaryCode) {
            KEYCODE_LANGUAGE_SWITCH -> {
                getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null)
                true
            }
            Keyboard.KEYCODE_MODE_CHANGE -> {
                getOnKeyboardActionListener().onKey(KEYCODE_EMOJI, null)
                true
            }
            ','.code -> if (alphabet) {
                getOnKeyboardActionListener().onKey(KEYCODE_EMOJI, null)
                true
            } else super.onLongPress(key)
            else -> super.onLongPress(key)
        }
    }

    override fun handleBack(): Boolean {
        if (alternatesPopup?.isShowing != true) return super.handleBack()
        finishAlternateTouch(null, drain = true)
        return true
    }

    private fun recordNativeTouch(event: MotionEvent, pointerId: Int) {
        nativePointerId = pointerId
        lastNativeTouchX = event.x
        lastNativeTouchY = event.y
    }

    private fun handleAlternateTouch(event: MotionEvent): Boolean {
        val pointer = event.findPointerIndex(alternatePointerId)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (pointer >= 0) alternatesPopup?.move(event.getX(pointer), event.getY(pointer))
                else finishAlternateTouch(null, drain = true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == alternatePointerId) {
                    if (pointer >= 0) alternatesPopup?.move(event.getX(pointer), event.getY(pointer))
                    finishAlternateTouch(alternatesPopup?.selection(), drain = event.pointerCount > 1)
                }
            }
            MotionEvent.ACTION_CANCEL -> finishAlternateTouch(null, drain = false)
            MotionEvent.ACTION_POINTER_DOWN -> finishAlternateTouch(null, drain = true)
        }
        return true
    }

    private fun finishAlternateTouch(text: String?, drain: Boolean) {
        // Clear every owner before dispatch: the IME can synchronously rebuild the keyboard.
        val x = lastNativeTouchX
        val y = lastNativeTouchY
        resetCustomTouches()
        val now = SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, x, y, 0)
        try {
            super.onTouchEvent(cancel)
        } finally {
            cancel.recycle()
        }
        drainingAlternateTouch = drain
        if (text != null) {
            val listener = getOnKeyboardActionListener()
            if (listener is FreeShiamyIME) listener.onKeyAlternative(text)
            else listener?.onText(text)
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
        if (!spaceSwipeEnabled && !spaceSwipeTracking) return false

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (isSpaceKeyTouch(event.x, event.y)) {
                    spaceSwipeTracking = true
                    spaceSwipeConsumed = false
                    spaceRepeatActive = false
                    spaceSwipeStartX = event.x
                    spaceSwipeStartY = event.y
                    pressedSpaceKey = keyboard.keys.firstOrNull { it.codes.firstOrNull() == 32 }
                    pressedSpaceKey?.pressed = true
                    invalidateAllKeys()
                    performKeyHapticFeedback()
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
                if (dy > spaceSwipeVerticalSlopPx || !isSpaceKeyTouch(event.x, event.y)) {
                    spaceSwipeConsumed = true
                    cancelSpaceRepeat()
                    releaseSpaceKey()
                    return true
                }
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
            MotionEvent.ACTION_UP -> {
                if (spaceSwipeTracking) {
                    if (!spaceSwipeConsumed && !spaceRepeatActive && isSpaceKeyTouch(event.x, event.y)) {
                        notifyImeSpace()
                    }
                    cancelSpaceRepeat()
                    resetSpaceSwipe()
                    return true
                }
                resetSpaceSwipe()
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                val owned = spaceSwipeTracking
                resetSpaceSwipe()
                return owned
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (spaceSwipeTracking) {
                    val sendSpace = !spaceSwipeConsumed && !spaceRepeatActive
                    resetSpaceSwipe()
                    if (sendSpace) notifyImeSpace()
                    // Resolve Space, then hand the second thumb to the framework as a
                    // single-pointer stream. Either release order must emit each key once.
                    rolloverActive = true
                    rolloverPointerId = event.getPointerId(event.actionIndex)
                    rolloverDownTime = event.eventTime
                    forwardRolloverTouch(event, MotionEvent.ACTION_DOWN)
                    return true
                }
            }
        }
        return spaceSwipeTracking
    }

    private fun handleRolloverTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (rolloverPointerId < 0) {
                    rolloverPointerId = event.getPointerId(event.actionIndex)
                    rolloverDownTime = event.eventTime
                    forwardRolloverTouch(event, MotionEvent.ACTION_DOWN)
                }
            }
            MotionEvent.ACTION_MOVE -> forwardRolloverTouch(event, MotionEvent.ACTION_MOVE)
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == rolloverPointerId) {
                    forwardRolloverTouch(event, MotionEvent.ACTION_UP)
                    rolloverPointerId = -1
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                forwardRolloverTouch(event, event.actionMasked)
                rolloverActive = false
                rolloverPointerId = -1
            }
        }
        return true
    }

    private fun forwardRolloverTouch(event: MotionEvent, action: Int) {
        val index = event.findPointerIndex(rolloverPointerId)
        if (index < 0) return
        val forwarded = MotionEvent.obtain(rolloverDownTime, event.eventTime, action,
            event.getX(index), event.getY(index), event.metaState)
        forwarded.source = event.source
        try {
            if (action == MotionEvent.ACTION_DOWN) updateKeyPreview(forwarded.x, forwarded.y)
            handleSoftDeleteTouch(forwarded)
            recordNativeTouch(forwarded, rolloverPointerId)
            super.onTouchEvent(forwarded)
        } finally {
            forwarded.recycle()
        }
    }

    private fun resetSpaceSwipe() {
        releaseSpaceKey()
        spaceSwipeTracking = false
        spaceSwipeConsumed = false
        spaceRepeatActive = false
        cancelSpaceRepeat()
    }

    private fun releaseSpaceKey() {
        pressedSpaceKey?.let {
            it.pressed = false
            invalidateAllKeys()
        }
        pressedSpaceKey = null
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

    private fun updateKeyPreview(x: Float, y: Float) {
        // Preview text entry keys, not Space, Shift, Delete or other action keys.
        val key = keyboard?.keys?.firstOrNull { it.isInside(x.toInt(), y.toInt()) }
        val code = key?.codes?.firstOrNull() ?: 0
        isPreviewEnabled = code > 32 && key?.label != null
    }

    companion object{
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_LANGUAGE_SWITCH = -101
        const val KEYCODE_EMOJI = -102
        const val KEYCODE_RAW_SPACE = -120
    }
}
