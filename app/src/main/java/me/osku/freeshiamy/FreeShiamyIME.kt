package me.osku.freeshiamy

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.preference.PreferenceManager
import me.osku.freeshiamy.engine.CinEntry
import me.osku.freeshiamy.engine.CinParser
import me.osku.freeshiamy.engine.ShiamyEngine
import me.osku.freeshiamy.settings.SettingsKeys
import me.osku.freeshiamy.settings.SettingsActivity
import me.osku.freeshiamy.ui.CandidateBarView
import kotlin.math.abs

/**
 * FreeShiamy IME (嘸蝦米)
 */
class FreeShiamyIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var inputMethodManager: InputMethodManager? = null
    private var inputView: FreeShiamyKeyboardView? = null
    private var candidateBarView: CandidateBarView? = null

    private val rawBuffer = StringBuilder()
    private var prefixCandidates: List<CinEntry> = emptyList()
    private var exactCount: Int = 0
    private var candidateInlineLimit: Int = SettingsKeys.DEFAULT_CANDIDATE_INLINE_LIMIT
    private var candidateMoreLimit: Int = SettingsKeys.DEFAULT_CANDIDATE_MORE_LIMIT
    private var keyboardHeightPercent: Int = SettingsKeys.DEFAULT_KEYBOARD_HEIGHT_PERCENT

    private var lastShiftTime: Long = 0
    private var capsLock = false

    private var symbolsKeyboard: FreeShiamyKeyboard? = null
    private var symbolsShiftedKeyboard: FreeShiamyKeyboard? = null
    private var qwertyKeyboard: FreeShiamyKeyboard? = null
    private var curKeyboard: FreeShiamyKeyboard? = null

    private var pendingCandidateBarSync: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        ensureEngineLoaded()
    }

    private fun ensureEngineLoaded() {
        if (sharedEngine != null || engineLoading) return
        engineLoading = true

        Thread {
            try {
                val entries = assets.open("freeshiamy.cin").use { CinParser.parse(it) }
                sharedEngine = ShiamyEngine(entries)
            } catch (_: Exception) {
                // Ignore; IME will work as raw keyboard until engine loads successfully.
            } finally {
                engineLoading = false
                mainHandler.post { updateUi() }
            }
        }.start()
    }

    /**
     * Create new context object whose resources are adjusted to match the metrics of the display
     * which is managed by WindowManager.
     */
    private fun getDisplayContextCompat(): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return this
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return createDisplayContext(wm.defaultDisplay)
    }

    override fun onInitializeInterface() {
        val displayContext: Context = getDisplayContextCompat()
        qwertyKeyboard = FreeShiamyKeyboard(displayContext, R.xml.qwerty)
        symbolsKeyboard = FreeShiamyKeyboard(displayContext, R.xml.symbols)
        symbolsShiftedKeyboard = FreeShiamyKeyboard(displayContext, R.xml.symbols_shift)
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.input, null) as ViewGroup
        candidateBarView = root.findViewById(R.id.candidate_bar)
        candidateBarView?.listener = object : CandidateBarView.Listener {
            override fun onRawClick() {
                commitRawBuffer()
            }

            override fun onCandidateClick(entry: CinEntry) {
                commitCandidate(entry)
            }
        }

        inputView = root.findViewById(R.id.keyboard)
        inputView?.setOnKeyboardActionListener(this)
        inputView?.setPreviewEnabled(false)
        setLatinKeyboard(qwertyKeyboard)
        reloadSettings()
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        requestCandidateBarSync()
        return root
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        clearState()
        curKeyboard = qwertyKeyboard
        curKeyboard?.setImeOptions(resources, attribute.imeOptions)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        clearState()
        inputView?.closing()
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        setLatinKeyboard(curKeyboard)
        inputView?.closing()
        reloadSettings()
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        updateUi()
        requestCandidateBarSync()
    }

    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype?) {
        // No-op: we intentionally keep the spacebar icon stable (not subtype/app icon).
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (rawBuffer.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            clearState()
        }
    }

    // ---- Keyboard callbacks (soft keys) ----

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        handlePrimaryCode(primaryCode)
    }

    override fun onText(text: CharSequence?) {
        if (text.isNullOrEmpty()) return
        for (c in text) {
            handlePrimaryCode(c.toInt())
        }
    }

    // ---- Hardware keys ----

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Intercept only when we have an input connection.
        val ic = currentInputConnection ?: return super.onKeyDown(keyCode, event)

        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                if (rawBuffer.isNotEmpty()) {
                    handleBackspace(ic)
                    return true
                }
            }

            KeyEvent.KEYCODE_SPACE -> {
                handleSpace(ic)
                return true
            }

            KeyEvent.KEYCODE_ENTER -> {
                handleEnter(ic)
                return true
            }
        }

        val unicodeChar = event.unicodeChar
        if (unicodeChar != 0 && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
            handlePrimaryCode(unicodeChar)
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    // ---- Core logic ----

    private fun handlePrimaryCode(primaryCode: Int) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_CANCEL -> handleClose()
            Keyboard.KEYCODE_MODE_CHANGE -> handleModeChange()
            FreeShiamyKeyboardView.KEYCODE_LANGUAGE_SWITCH -> handleLanguageSwitch()
            FreeShiamyKeyboardView.KEYCODE_OPTIONS -> {
                openSettings()
            }
            FreeShiamyKeyboardView.KEYCODE_INDENT -> {
                handleIndent(ic)
            }

            10 -> handleEnter(ic)
            32 -> handleSpace(ic)
            in 48..57 -> handleDigit(primaryCode, ic)
            else -> handleCharacter(primaryCode, ic)
        }
    }

    private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
        val ch = primaryCode.toChar()

        if (ch.isLetter()) {
            rawBuffer.append(ch.toLowerCase())
            updateCandidates()
            updateUi()
            return
        }

        if (isCodeSymbol(ch)) {
            rawBuffer.append(ch)
            updateCandidates()
            updateUi()
            return
        }

        // Non-code symbol: if user already has rawBuffer, treat as raw accumulation;
        // otherwise, commit directly like a normal keyboard.
        if (rawBuffer.isNotEmpty()) {
            rawBuffer.append(ch)
            updateCandidates()
            updateUi()
        } else {
            ic.commitText(ch.toString(), 1)
        }
    }

    private fun handleSpace(ic: InputConnection) {
        if (rawBuffer.isEmpty()) {
            ic.commitText(" ", 1)
            return
        }

        if (exactCount > 0) {
            commitCandidateAt(0, ic)
            return
        }

        rawBuffer.append(' ')
        updateCandidates()
        updateUi()
    }

    private fun handleDigit(primaryCode: Int, ic: InputConnection) {
        val digitIndex = primaryCode - '0'.toInt()

        if (rawBuffer.isNotEmpty() && exactCount > 0) {
            if (digitIndex < exactCount) {
                commitCandidateAt(digitIndex, ic)
            }
            return
        }

        if (rawBuffer.isNotEmpty()) {
            rawBuffer.append(primaryCode.toChar())
            updateCandidates()
            updateUi()
            return
        }

        ic.commitText(primaryCode.toChar().toString(), 1)
    }

    private fun handleBackspace(ic: InputConnection) {
        if (rawBuffer.isNotEmpty()) {
            rawBuffer.deleteCharAt(rawBuffer.length - 1)
            updateCandidates()
            updateUi()
            return
        }
        keyDownUp(KeyEvent.KEYCODE_DEL)
    }

    private fun handleEnter(ic: InputConnection) {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer(ic)
        }

        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (ic.performEditorAction(action)) return
        }

        ic.commitText("\n", 1)
    }

    private fun handleIndent(ic: InputConnection) {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer(ic)
        }
        ic.commitText("    ", 1)
    }

    private fun commitCandidate(entry: CinEntry) {
        val ic = currentInputConnection ?: return
        ic.commitText(entry.value, 1)
        clearState()
        updateUi()
    }

    private fun commitCandidateAt(index: Int, ic: InputConnection) {
        if (index < 0 || index >= exactCount) return
        ic.commitText(prefixCandidates[index].value, 1)
        clearState()
        updateUi()
    }

    private fun commitRawBuffer() {
        val ic = currentInputConnection ?: return
        commitRawBuffer(ic)
        updateUi()
    }

    private fun commitRawBuffer(ic: InputConnection) {
        val text = rawBuffer.toString()
        if (text.isNotEmpty()) {
            ic.commitText(text, 1)
        }
        clearState()
    }

    private fun clearState() {
        rawBuffer.setLength(0)
        prefixCandidates = emptyList()
        exactCount = 0
        candidateBarView?.setExpanded(false)
    }

    private fun updateCandidates() {
        val engine = sharedEngine ?: run {
            prefixCandidates = emptyList()
            exactCount = 0
            return
        }

        val prefix = rawBuffer.toString()
        prefixCandidates = engine.queryPrefix(prefix)
        exactCount = 0
        val prefixLen = prefix.length
        while (exactCount < prefixCandidates.size && prefixCandidates[exactCount].codeLength == prefixLen) {
            exactCount++
        }
    }

    private fun updateUi() {
        val rawText = rawBuffer.toString()
        // Keep the candidate bar space reserved to avoid keyboard jumping (layout thrash).
        val shouldShowCandidateBar = rawText.isNotEmpty()
        candidateBarView?.visibility = if (shouldShowCandidateBar) View.VISIBLE else View.INVISIBLE
        candidateBarView?.setState(
            rawText = rawText,
            prefixCandidates = prefixCandidates,
            exactCount = exactCount,
        )
    }

    private fun requestCandidateBarSync() {
        val view = inputView ?: return
        if (pendingCandidateBarSync) return
        pendingCandidateBarSync = true
        view.post {
            pendingCandidateBarSync = false
            syncCandidateBarToKeyboard(iterationsLeft = 3)
        }
    }

    private fun syncCandidateBarToKeyboard(iterationsLeft: Int) {
        val view = inputView ?: return
        val bar = candidateBarView ?: return

        val scale = view.getEffectiveHeightScale()
        bar.setHeightScale(scale)

        if (iterationsLeft <= 0) return
        view.post {
            val newScale = view.getEffectiveHeightScale()
            if (abs(newScale - scale) > 0.01f) {
                syncCandidateBarToKeyboard(iterationsLeft - 1)
            }
        }
    }

    private fun reloadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        keyboardHeightPercent = prefs.getInt(
            SettingsKeys.KEY_KEYBOARD_HEIGHT_PERCENT,
            SettingsKeys.DEFAULT_KEYBOARD_HEIGHT_PERCENT,
        )
        candidateInlineLimit = prefs.getInt(
            SettingsKeys.KEY_CANDIDATE_INLINE_LIMIT,
            SettingsKeys.DEFAULT_CANDIDATE_INLINE_LIMIT,
        )
        candidateMoreLimit = prefs.getInt(
            SettingsKeys.KEY_CANDIDATE_MORE_LIMIT,
            SettingsKeys.DEFAULT_CANDIDATE_MORE_LIMIT,
        )
    }

    private fun setLatinKeyboard(nextKeyboard: FreeShiamyKeyboard?) {
        val shouldSupportLanguageSwitchKey =
            inputMethodManager?.shouldOfferSwitchingToNextInputMethod(getToken()) ?: false
        nextKeyboard?.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey)
        inputView?.keyboard = nextKeyboard
        requestCandidateBarSync()
    }

    private fun getToken(): IBinder? {
        val dialog = window ?: return null
        val w = dialog.window ?: return null
        return w.attributes.token
    }

    private fun handleModeChange() {
        val view = inputView ?: return
        val current = view.keyboard
        if (current === symbolsKeyboard || current === symbolsShiftedKeyboard) {
            setLatinKeyboard(qwertyKeyboard)
        } else {
            setLatinKeyboard(symbolsKeyboard)
            symbolsKeyboard?.isShifted = false
        }
    }

    private fun handleLanguageSwitch() {
        inputMethodManager?.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */)
    }

    private fun handleShift() {
        val view = inputView ?: return
        val currentKeyboard = view.keyboard

        if (qwertyKeyboard === currentKeyboard) {
            checkToggleCapsLock()
            view.isShifted = capsLock || !view.isShifted
            return
        }

        if (currentKeyboard === symbolsKeyboard) {
            symbolsKeyboard?.isShifted = true
            setLatinKeyboard(symbolsShiftedKeyboard)
            symbolsShiftedKeyboard?.isShifted = true
            return
        }

        if (currentKeyboard === symbolsShiftedKeyboard) {
            symbolsShiftedKeyboard?.isShifted = false
            setLatinKeyboard(symbolsKeyboard)
            symbolsKeyboard?.isShifted = false
        }
    }

    private fun checkToggleCapsLock() {
        val now = System.currentTimeMillis()
        if (lastShiftTime + 800 > now) {
            capsLock = !capsLock
            lastShiftTime = 0
        } else {
            lastShiftTime = now
        }
    }

    private fun handleClose() {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer()
        }
        requestHideSelf(0)
        inputView?.closing()
    }

    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    private fun openSettings() {
        try {
            val intent = Intent(this, SettingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun isCodeSymbol(ch: Char): Boolean {
        return when (ch) {
            '\'', ',', '.', '[', ']' -> true
            else -> false
        }
    }

    // Unused callbacks
    override fun swipeRight() {}
    override fun swipeLeft() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}

    companion object {
        @Volatile private var sharedEngine: ShiamyEngine? = null
        @Volatile private var engineLoading: Boolean = false
    }
}
