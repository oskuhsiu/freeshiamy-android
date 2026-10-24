package me.osku.freeshiamy

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
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
import me.osku.freeshiamy.engine.SpellEngine
import me.osku.freeshiamy.settings.SettingsKeys
import me.osku.freeshiamy.settings.SettingsActivity
import me.osku.freeshiamy.ui.CandidateBarView
import me.osku.freeshiamy.catalog.CatalogMode
import java.util.Locale

/**
 * FreeShiamy IME
 */
class FreeShiamyIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var inputMethodManager: InputMethodManager? = null
    private var inputView: FreeShiamyKeyboardView? = null
    private var candidateBarView: CandidateBarView? = null
    private var symbolPickerView: SymbolPickerView? = null
    private var pickerMode: CatalogMode? = null

    private val rawBuffer = StringBuilder()
    // Explicit picker selections stay whole when deleting pending raw text.
    private val rawPickerSegments = mutableListOf<Pair<Int, Int>>()
    private var prefixCandidates: List<CinEntry> = emptyList()
    private var exactCount: Int = 0
    private var reverseLookupState: ReverseLookupState = ReverseLookupState.NONE
    private var candidateInlineLimit: Int = SettingsKeys.DEFAULT_CANDIDATE_INLINE_LIMIT
    private var candidateMoreLimit: Int = SettingsKeys.DEFAULT_CANDIDATE_MORE_LIMIT
    private var keyboardHeightPercent: Int = SettingsKeys.DEFAULT_KEYBOARD_HEIGHT_PERCENT
    private var showShortestCodeHint: Boolean = SettingsKeys.DEFAULT_SHOW_SHORTEST_CODE_HINT
    private var shortestCodeHintText: String? = null
    private var disableImeInSensitiveFields: Boolean = SettingsKeys.DEFAULT_DISABLE_IME_IN_SENSITIVE_FIELDS
    private var sensitiveIncludeNoPersonalizedLearning: Boolean =
        SettingsKeys.DEFAULT_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING
    private var isInSensitiveField: Boolean = false

    private var lastShiftTime: Long = 0
    private var letterShifted = false
    private var capsLock = false
    private var englishLocked = false
    private var qwertyFamiliarKeyboard: FreeShiamyKeyboard? = null
    private var qwertyFamiliarKeyboardNoNumber: FreeShiamyKeyboard? = null

    private var qwertyKeyboard: FreeShiamyKeyboard? = null
    private var curKeyboard: FreeShiamyKeyboard? = null
    private var showNumberRow: Boolean = SettingsKeys.DEFAULT_SHOW_NUMBER_ROW
    private var alwaysShowIme: Boolean = SettingsKeys.DEFAULT_ALWAYS_SHOW_IME
    private var displayContext: Context? = null
    private var lastConfig: Configuration? = null

    // When Backspace/Delete is held and rawBuffer had content at the start of the press,
    // we must NOT continue deleting the editor text after rawBuffer becomes empty.
    private var suppressExternalDeleteUntilDeleteReleased: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        lastConfig = Configuration(resources.configuration)
        ensureEnginesLoaded()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        refreshAlwaysShowImeSetting()
        return alwaysShowIme || super.onEvaluateInputViewShown()
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        refreshAlwaysShowImeSetting()
        return if (alwaysShowIme) true else super.onShowInputRequested(flags, configChange)
    }

    private fun ensureEnginesLoaded() {
        ensureShiamyEngineLoaded()
        ensureSpellEngineLoaded()
    }

    private fun ensureShiamyEngineLoaded() {
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

    private fun ensureSpellEngineLoaded() {
        if (sharedSpellEngine != null || spellEngineLoading) return
        spellEngineLoading = true

        Thread {
            try {
                val entries = assets.open("cht_spells.cin").use { CinParser.parse(it) }
                sharedSpellEngine = SpellEngine(entries)
            } catch (_: Exception) {
                // Ignore; reverse lookup will be disabled until the table loads successfully.
            } finally {
                spellEngineLoading = false
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
        displayContext = getDisplayContextCompat()
        resetKeyboardCache()
    }

    override fun onCreateInputView(): View {
        inputView?.closing()
        symbolPickerView?.releasePages()
        val root = layoutInflater.inflate(R.layout.input, null) as ViewGroup
        candidateBarView = root.findViewById(R.id.candidate_bar)
        candidateBarView?.listener = object : CandidateBarView.Listener {
            override fun onSettingsClick() = openSettings()

            override fun onLanguageSwitchClick() = handleLanguageSwitch()

            override fun onHideKeyboardClick() = handleClose()

            override fun onEnglishLockClick() = toggleEnglishLock()

            override fun onRawClick() {
                commitRawBuffer()
            }

            override fun onCandidateClick(entry: CinEntry) {
                if (reverseLookupState != ReverseLookupState.ACTIVE &&
                    isReverseLookupEntering(rawBuffer.toString()) &&
                    exactCount > 0
                ) {
                    val index = prefixCandidates.indexOf(entry)
                    if (index in 0 until exactCount) {
                        triggerReverseLookup(index)
                        return
                    }
                }
                commitCandidate(entry)
            }
        }

        inputView = root.findViewById(R.id.keyboard)
        inputView?.setOnKeyboardActionListener(this)
        symbolPickerView = root.findViewById<SymbolPickerView>(R.id.symbol_picker).apply {
            listener = object : SymbolPickerView.Listener {
                override fun onText(text: String) = onPickerText(text)
                override fun onKey(code: Int) = handlePrimaryCode(code)
                override fun onDeleteTouch(down: Boolean) = onSoftDeleteTouch(down)
            }
        }
        reloadSettings()
        setLatinKeyboard(qwertyKeyboard)
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        return root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val activePicker = if (pickerMode != null) symbolPickerView?.mode else null
        super.onConfigurationChanged(newConfig)
        maybeRecreateKeyboardsForConfigChange(newConfig)
        if (activePicker != null && isInputViewShown) showPicker(activePicker)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        lastShiftTime = 0
        clearState()
        isInSensitiveField = disableImeInSensitiveFields && isSensitiveField(attribute)
        curKeyboard = qwertyKeyboard
        curKeyboard?.setImeOptions(resources, attribute.imeOptions)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        lastShiftTime = 0
        inputView?.closing()
        symbolPickerView?.releasePages()
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        clearState()
        suppressExternalDeleteUntilDeleteReleased = false
        inputView?.closing()
        symbolPickerView?.releasePages()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (!isInputViewShown) symbolPickerView?.releasePages()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (!isInputViewShown) symbolPickerView?.releasePages()
    }

    override fun onDestroy() {
        inputView?.closing()
        symbolPickerView?.releasePages()
        symbolPickerView?.listener = null
        symbolPickerView = null
        inputView = null
        candidateBarView = null
        super.onDestroy()
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        suppressExternalDeleteUntilDeleteReleased = false
        inputView?.closing()
        val didRecreate = maybeRecreateKeyboardsForConfigChange(resources.configuration)
        if (!didRecreate) {
            reloadSettings()
        }
        isInSensitiveField = disableImeInSensitiveFields && isSensitiveField(attribute)
        setLatinKeyboard(curKeyboard ?: qwertyKeyboard)
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        if (isInSensitiveField) {
            clearState()
            updateUi()
            mainHandler.post { exitImeForSensitiveFieldIfNeeded() }
        }
        updateUi()
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

    internal fun onKeyAlternative(text: String) {
        if (pickerMode != null) {
            onPickerText(text)
            return
        }
        val ic = currentInputConnection ?: return
        lastShiftTime = 0
        shortestCodeHintText = null
        // Explicit selections preserve case and insert digits literally, not as candidate indices.
        if (text.length == 1) {
            handleCharacter(text.single().code, ic, preserveLetterCase = true)
        } else {
            if (reverseLookupState == ReverseLookupState.ACTIVE) cancelReverseLookup()
            if (!englishLocked && !isInSensitiveField && rawBuffer.isNotEmpty()) {
                appendToRawBuffer(text)
            } else {
                ic.commitText(text, 1)
            }
        }
        if (rawBuffer.isEmpty()) updateUi()
    }

    private fun onPickerText(text: String) {
        if (text.isEmpty()) return
        val ic = currentInputConnection ?: return
        lastShiftTime = 0
        shortestCodeHintText = null
        if (reverseLookupState == ReverseLookupState.ACTIVE) cancelReverseLookup()
        if (!englishLocked && !isInSensitiveField && rawBuffer.isNotEmpty()) {
            val start = rawBuffer.length
            rawPickerSegments.add(start to start + text.length)
            appendToRawBuffer(text)
        } else {
            // One commit preserves ZWJ, variation selectors, flags and emoticon spaces.
            ic.commitText(text, 1)
            updateUi()
        }
    }

    override fun onText(text: CharSequence?) {
        if (text.isNullOrEmpty()) return
        if (pickerMode != null) {
            onPickerText(text.toString())
            return
        }
        val ic = currentInputConnection ?: return
        lastShiftTime = 0
        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        val shouldDirectCommit = text.length != 1 || containsSurrogate(text)
        if (shouldDirectCommit) {
            if (reverseLookupState == ReverseLookupState.ACTIVE) {
                cancelReverseLookup()
            }
            ic.commitText(text, 1)
            if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
                updateUi()
            }
            return
        }

        for (c in text) {
            handlePrimaryCode(c.code)
        }

        if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
            updateUi()
        }
    }

    // ---- Hardware keys ----

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Intercept only when we have an input connection.
        val ic = currentInputConnection ?: return super.onKeyDown(keyCode, event)
        lastShiftTime = 0

        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        fun finish(result: Boolean): Boolean {
            if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
                updateUi()
            }
            return result
        }

        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (event.repeatCount == 0 && symbolPickerView?.handleBack() == true) return finish(true)
                if (event.repeatCount == 0 && inputView?.handleBack() == true) return finish(true)
            }
            KeyEvent.KEYCODE_DEL -> {
                if (event.repeatCount == 0) {
                    suppressExternalDeleteUntilDeleteReleased = rawBuffer.isNotEmpty()
                }
                if (suppressExternalDeleteUntilDeleteReleased || rawBuffer.isNotEmpty()) {
                    handleBackspace(ic)
                    return finish(true)
                }
            }

            KeyEvent.KEYCODE_SPACE -> {
                handleSpace(ic)
                return finish(true)
            }

            KeyEvent.KEYCODE_ENTER -> {
                handleEnter(ic)
                return finish(true)
            }
        }

        val unicodeChar = event.unicodeChar
        if (unicodeChar != 0 && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
            handlePrimaryCode(unicodeChar, preserveLetterCase = englishLocked)
            return finish(true)
        }

        return finish(super.onKeyDown(keyCode, event))
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            suppressExternalDeleteUntilDeleteReleased = false
        }
        return super.onKeyUp(keyCode, event)
    }

    internal fun onSoftDeleteTouch(isDown: Boolean) {
        suppressExternalDeleteUntilDeleteReleased = isDown && rawBuffer.isNotEmpty()
    }

    // ---- Core logic ----

    private fun handlePrimaryCode(primaryCode: Int, preserveLetterCase: Boolean = false) {
        val ic = currentInputConnection ?: return
        if (primaryCode != Keyboard.KEYCODE_SHIFT) lastShiftTime = 0
        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_CANCEL -> handleClose()
            Keyboard.KEYCODE_MODE_CHANGE -> handleModeChange()
            FreeShiamyKeyboardView.KEYCODE_LANGUAGE_SWITCH -> handleLanguageSwitch()
            FreeShiamyKeyboardView.KEYCODE_EMOJI -> handleEmojiSwitch()
            FreeShiamyKeyboardView.KEYCODE_RAW_SPACE -> handleRawSpace()
            FreeShiamyKeyboardView.KEYCODE_OPTIONS -> {
                openSettings()
            }

            10 -> handleEnter(ic)
            32 -> handleSpace(ic)
            in 48..57 -> handleDigit(primaryCode, ic)
            else -> handleCharacter(primaryCode, ic, preserveLetterCase)
        }

        if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
            updateUi()
        }
    }

    private fun applyShiftToLetter(ch: Char, preserveCase: Boolean = false): Char {
        val base = ch.lowercaseChar()
        val typed = if (preserveCase) ch else if (letterShifted) base.uppercaseChar() else base
        // An intervening letter consumes one-shot Shift and ends the double-tap window.
        lastShiftTime = 0
        if (letterShifted && !capsLock) {
            letterShifted = false
            updateLetterShift()
        }
        return typed
    }

    private fun handleCharacter(primaryCode: Int, ic: InputConnection, preserveLetterCase: Boolean = false) {
        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            cancelReverseLookup()
        }

        val ch = primaryCode.toChar()

        if (englishLocked) {
            val typed = if (ch.isLetter() && !isSymbolsKeyboardActive()) {
                applyShiftToLetter(ch, preserveLetterCase)
            } else ch
            ic.commitText(typed.toString(), 1)
            return
        }

        if (isSymbolsKeyboardActive()) {
            if (rawBuffer.isNotEmpty() && !isInSensitiveField) {
                appendToRawBuffer(ch.toString())
            } else {
                ic.commitText(ch.toString(), 1)
            }
            return
        }

        if (ch.isLetter()) {
            val typed = applyShiftToLetter(ch, preserveLetterCase)
            if (isInSensitiveField) {
                ic.commitText(typed.toString(), 1)
                return
            }
            rawBuffer.append(typed)
            updateCandidates()
            updateUi()
            return
        }

        if (isCodeSymbol(ch)) {
            if (isInSensitiveField) {
                ic.commitText(ch.toString(), 1)
                return
            }
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
        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            commitCandidateAt(0, ic)
            return
        }

        if (isReverseLookupEntering(rawBuffer.toString()) && exactCount > 0) {
            triggerReverseLookup(0)
            return
        }

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
        if (englishLocked) {
            ic.commitText(primaryCode.toChar().toString(), 1)
            return
        }
        val digitIndex = primaryCode - '0'.toInt()

        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            if (digitIndex < exactCount) {
                commitCandidateAt(digitIndex, ic)
            }
            return
        }

        if (isSymbolsKeyboardActive()) {
            if (rawBuffer.isNotEmpty() && !isInSensitiveField) {
                appendToRawBuffer(primaryCode.toChar().toString())
            } else {
                ic.commitText(primaryCode.toChar().toString(), 1)
            }
            return
        }

        if (isReverseLookupEntering(rawBuffer.toString()) && exactCount > 0) {
            triggerReverseLookup(digitIndex)
            return
        }

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
        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            cancelReverseLookup()
            return
        }

        if (rawBuffer.isNotEmpty()) {
            val selection = rawPickerSegments.lastOrNull()
            if (selection != null && selection.second == rawBuffer.length) {
                rawBuffer.delete(selection.first, selection.second)
                rawPickerSegments.removeAt(rawPickerSegments.lastIndex)
            } else {
                val start = Character.offsetByCodePoints(rawBuffer, rawBuffer.length, -1)
                rawBuffer.delete(start, rawBuffer.length)
            }
            updateCandidates()
            updateUi()
            return
        }
        if (suppressExternalDeleteUntilDeleteReleased) return
        sendKeyDownUp(ic, KeyEvent.KEYCODE_DEL)
    }

    private fun handleEnter(ic: InputConnection) {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer(ic)
            updateUi()
            return
        }

        val options = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE
        val action = if (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) EditorInfo.IME_ACTION_NONE
            else options and EditorInfo.IME_MASK_ACTION
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (ic.performEditorAction(action)) return
        }

        ic.commitText("\n", 1)
    }

    private fun handleRawSpace() {
        if (isInSensitiveField) return
        if (rawBuffer.isEmpty()) return
        appendToRawBuffer(" ")
    }

    private fun commitCandidate(entry: CinEntry) {
        val ic = currentInputConnection ?: return
        val typedCode = rawBuffer.toString()
        val isReverseCommit = reverseLookupState == ReverseLookupState.ACTIVE
        commitValue(entry.value, typedCode, isReverseCommit, ic)
        updateUi()
    }

    private fun commitCandidateAt(index: Int, ic: InputConnection) {
        if (index < 0 || index >= exactCount) return
        val typedCode = rawBuffer.toString()
        val isReverseCommit = reverseLookupState == ReverseLookupState.ACTIVE
        val value = prefixCandidates[index].value
        commitValue(value, typedCode, isReverseCommit, ic)
        updateUi()
    }

    private fun commitValue(value: String, typedCode: String, isReverseCommit: Boolean, ic: InputConnection) {
        val hintText = buildShortestCodeHintText(value, typedCode, isReverseCommit)
        ic.commitText(value, 1)
        clearState()
        shortestCodeHintText = hintText
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
        rawPickerSegments.clear()
        prefixCandidates = emptyList()
        exactCount = 0
        reverseLookupState = ReverseLookupState.NONE
        candidateBarView?.setExpanded(false)
        shortestCodeHintText = null
        inputView?.setSpaceSwipeEnabled(false)
        symbolPickerView?.setSpaceSwipeEnabled(false)
    }

    private fun updateCandidates() {
        val engine = sharedEngine ?: run {
            prefixCandidates = emptyList()
            exactCount = 0
            return
        }

        if (reverseLookupState == ReverseLookupState.ACTIVE) return

        val rawText = rawBuffer.toString()
        val normalizedRawText = rawText.lowercase(Locale.ROOT)
        val prefix =
            if (isReverseLookupEntering(rawText)) {
                reverseLookupState = ReverseLookupState.ENTERING
                normalizedRawText.substring(1)
            } else {
                reverseLookupState = ReverseLookupState.NONE
                normalizedRawText
            }
        prefixCandidates = engine.queryPrefix(prefix)
        exactCount = 0
        val prefixLen = prefix.length
        while (exactCount < prefixCandidates.size && prefixCandidates[exactCount].codeLength == prefixLen) {
            exactCount++
        }
    }

    private fun appendToRawBuffer(text: CharSequence) {
        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            cancelReverseLookup()
        }
        rawBuffer.append(text)
        updateCandidates()
        updateUi()
    }

    private fun isReverseLookupEntering(rawText: String): Boolean {
        if (reverseLookupState == ReverseLookupState.ACTIVE) return false
        if (rawText.length < 2) return false
        if (rawText[0] != '\'') return false
        // Avoid hijacking punctuation codes like "''" / "'''".
        return rawText[1].isLetter()
    }

    private fun triggerReverseLookup(baseIndex: Int) {
        val spellEngine = sharedSpellEngine ?: return

        val rawText = rawBuffer.toString()
        if (!isReverseLookupEntering(rawText)) return

        val baseCode = rawText.substring(1)
        if (baseCode.isEmpty()) return

        if (exactCount <= 0) return
        if (baseIndex < 0 || baseIndex >= exactCount) return

        val baseChar = prefixCandidates[baseIndex].value
        val spell = spellEngine.spellFor(baseChar)

        val values =
            if (spell != null) {
                spellEngine.valuesForSpell(spell)
            } else {
                emptyList()
            }

        val entries =
            if (values.isNotEmpty()) {
                values.mapIndexed { index, value -> CinEntry(code = spell!!, value = value, sourceOrder = index) }
            } else {
                listOf(CinEntry(code = spell ?: "", value = baseChar, sourceOrder = 0))
            }

        reverseLookupState = ReverseLookupState.ACTIVE
        candidateBarView?.setExpanded(false)
        prefixCandidates = entries
        exactCount = entries.size
        updateUi()
    }

    private fun cancelReverseLookup() {
        if (reverseLookupState != ReverseLookupState.ACTIVE) return
        reverseLookupState = ReverseLookupState.ENTERING
        candidateBarView?.setExpanded(false)
        updateCandidates()
        updateUi()
    }

    private fun updateUi() {
        val rawText = rawBuffer.toString()
        val hintText = if (showShortestCodeHint) shortestCodeHintText else null
        // Keep the candidate bar space reserved to avoid keyboard jumping (layout thrash).
        candidateBarView?.visibility = View.VISIBLE
        candidateBarView?.setEnglishLocked(englishLocked)
        candidateBarView?.setState(
            rawText = rawText,
            prefixCandidates = prefixCandidates,
            exactCount = exactCount,
            hintText = hintText,
        )
        val spaceSwipeEnabled = rawText.isNotEmpty() && !isInSensitiveField
        inputView?.setSpaceSwipeEnabled(spaceSwipeEnabled)
        symbolPickerView?.setSpaceSwipeEnabled(spaceSwipeEnabled)
    }

    private fun reloadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        keyboardHeightPercent = prefs.getInt(
            SettingsKeys.KEY_KEYBOARD_HEIGHT_PERCENT,
            SettingsKeys.DEFAULT_KEYBOARD_HEIGHT_PERCENT,
        ).coerceAtLeast(SettingsKeys.MIN_KEYBOARD_HEIGHT_PERCENT)
        showNumberRow = prefs.getBoolean(SettingsKeys.KEY_SHOW_NUMBER_ROW, SettingsKeys.DEFAULT_SHOW_NUMBER_ROW)
        alwaysShowIme = prefs.getBoolean(SettingsKeys.KEY_ALWAYS_SHOW_IME, SettingsKeys.DEFAULT_ALWAYS_SHOW_IME)
        val wasQwerty = curKeyboard == null || curKeyboard === qwertyKeyboard
        qwertyKeyboard = getQwertyFamiliarKeyboard(showNumberRow)
        if (wasQwerty) curKeyboard = qwertyKeyboard
        inputView?.setKeyHapticFeedbackEnabled(
            prefs.getBoolean(SettingsKeys.KEY_HAPTIC_FEEDBACK, SettingsKeys.DEFAULT_HAPTIC_FEEDBACK),
        )
        symbolPickerView?.setHapticFeedbackEnabled(
            prefs.getBoolean(SettingsKeys.KEY_HAPTIC_FEEDBACK, SettingsKeys.DEFAULT_HAPTIC_FEEDBACK),
        )
        symbolPickerView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateInlineLimit = prefs.getInt(
            SettingsKeys.KEY_CANDIDATE_INLINE_LIMIT,
            SettingsKeys.DEFAULT_CANDIDATE_INLINE_LIMIT,
        )
        candidateMoreLimit = prefs.getInt(
            SettingsKeys.KEY_CANDIDATE_MORE_LIMIT,
            SettingsKeys.DEFAULT_CANDIDATE_MORE_LIMIT,
        )
        showShortestCodeHint = prefs.getBoolean(
            SettingsKeys.KEY_SHOW_SHORTEST_CODE_HINT,
            SettingsKeys.DEFAULT_SHOW_SHORTEST_CODE_HINT,
        )
        if (!showShortestCodeHint) {
            shortestCodeHintText = null
        }
        disableImeInSensitiveFields = prefs.getBoolean(
            SettingsKeys.KEY_DISABLE_IME_IN_SENSITIVE_FIELDS,
            SettingsKeys.DEFAULT_DISABLE_IME_IN_SENSITIVE_FIELDS,
        )
        sensitiveIncludeNoPersonalizedLearning = prefs.getBoolean(
            SettingsKeys.KEY_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING,
            SettingsKeys.DEFAULT_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING,
        )
    }

    private fun refreshAlwaysShowImeSetting() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        alwaysShowIme = prefs.getBoolean(SettingsKeys.KEY_ALWAYS_SHOW_IME, SettingsKeys.DEFAULT_ALWAYS_SHOW_IME)
    }

    private fun isSensitiveField(attribute: EditorInfo?): Boolean {
        if (attribute == null) return false

        val inputType = attribute.inputType
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        val isPassword =
            when (klass) {
                InputType.TYPE_CLASS_TEXT -> {
                    variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                }
                InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else -> false
            }
        if (isPassword) return true

        if (sensitiveIncludeNoPersonalizedLearning) {
            val hasNoPersonalizedLearning =
                (attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
            if (hasNoPersonalizedLearning) return true
        }

        val privateOpts = attribute.privateImeOptions
        if (!privateOpts.isNullOrBlank()) {
            val parts = privateOpts.split(',', ';', ' ')
            if (parts.any { it == "freeshiamy:disable" || it == "freeshiamy:disable=true" }) return true
        }

        return false
    }

    private fun exitImeForSensitiveFieldIfNeeded() {
        if (!isInSensitiveField) return

        val imm = inputMethodManager ?: return
        val token = getToken()
        if (token == null) {
            mainHandler.postDelayed({ exitImeForSensitiveFieldIfNeeded() }, 50L)
            return
        }
        try {
            val offered = imm.shouldOfferSwitchingToNextInputMethod(token)
            if (offered) {
                imm.switchToNextInputMethod(token, false /* onlyCurrentIme */)
                return
            }
        } catch (_: Exception) {
            // Ignore and fall back to showing picker / hiding self.
        }

        try {
            imm.showInputMethodPicker()
        } catch (_: Exception) {
            // ignore
        }
        requestHideSelf(0)
    }

    private fun buildShortestCodeHintText(value: String, typedCode: String, isReverseCommit: Boolean): String? {
        if (!showShortestCodeHint) return null
        if (value.codePointCount(0, value.length) != 1) return null

        val engine = sharedEngine ?: return null
        val shortestCode = engine.shortestCodeForValue(value) ?: return null

        if (!isReverseCommit) {
            if (typedCode.length <= 2) return null
            if (shortestCode.length >= typedCode.length) return null
        }

        return "字根：${shortestCode.uppercase(Locale.ROOT)}"
    }

    private fun setLatinKeyboard(nextKeyboard: FreeShiamyKeyboard?) {
        pickerMode = null
        symbolPickerView?.releasePages()
        symbolPickerView?.visibility = View.GONE
        inputView?.visibility = View.VISIBLE
        val shouldSupportLanguageSwitchKey =
            inputMethodManager?.shouldOfferSwitchingToNextInputMethod(getToken()) ?: false
        nextKeyboard?.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey)
        inputView?.keyboard = nextKeyboard
        updateLetterShift()
    }

    private fun toggleEnglishLock() {
        val ic = currentInputConnection ?: return
        lastShiftTime = 0
        // A mode change must not choose a Chinese candidate or discard pending literal text.
        commitRawBuffer(ic)
        englishLocked = !englishLocked
        updateUi()
    }

    private fun updateLetterShift() {
        val view = inputView ?: return
        if (view.keyboard === qwertyKeyboard) view.setLetterShiftState(letterShifted, capsLock)
    }

    private fun getToken(): IBinder? {
        val dialog = window ?: return null
        val w = dialog.window ?: return null
        return w.attributes.token
    }

    private fun handleModeChange() {
        if (pickerMode != null) setLatinKeyboard(qwertyKeyboard)
        else showPicker(CatalogMode.SYMBOLS)
    }

    private fun handleEmojiSwitch() {
        showPicker(CatalogMode.EMOJI)
    }

    private fun showPicker(mode: CatalogMode) {
        val picker = symbolPickerView ?: return
        inputView?.closing()
        inputView?.visibility = View.GONE
        pickerMode = mode
        picker.visibility = View.VISIBLE
        picker.setImeOptions(currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE)
        picker.show(mode)
        updateUi()
    }

    private fun handleLanguageSwitch() {
        inputMethodManager?.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */)
    }

    private fun handleShift() {
        if (pickerMode != null) return
        val view = inputView ?: return
        val currentKeyboard = view.keyboard

        if (qwertyKeyboard === currentKeyboard) {
            val now = SystemClock.uptimeMillis()
            if (capsLock) {
                capsLock = false
                lastShiftTime = 0
                letterShifted = false
            } else if (letterShifted && lastShiftTime != 0L && now - lastShiftTime < 800) {
                capsLock = true
                lastShiftTime = 0
                letterShifted = true
            } else {
                letterShifted = !letterShifted
                lastShiftTime = if (letterShifted) now else 0
            }
            updateLetterShift()
            return
        }
    }

    private fun handleClose() {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer()
        }
        requestHideSelf(0)
        inputView?.closing()
        symbolPickerView?.releasePages()
    }

    private fun isSymbolsKeyboardActive(): Boolean {
        return pickerMode != null
    }

    private fun containsSurrogate(text: CharSequence): Boolean {
        for (i in 0 until text.length) {
            if (Character.isSurrogate(text[i])) return true
        }
        return false
    }

    private fun requireDisplayContext(): Context {
        val existing = displayContext
        if (existing != null) return existing
        val created = getDisplayContextCompat()
        displayContext = created
        return created
    }

    private fun resetKeyboardCache() {
        qwertyFamiliarKeyboard = null
        qwertyFamiliarKeyboardNoNumber = null
        qwertyKeyboard = null
        curKeyboard = null
    }

    private fun hasKeyboardMetricsChanged(prev: Configuration, next: Configuration): Boolean {
        return prev.orientation != next.orientation ||
            prev.screenWidthDp != next.screenWidthDp ||
            prev.screenHeightDp != next.screenHeightDp ||
            prev.smallestScreenWidthDp != next.smallestScreenWidthDp ||
            prev.densityDpi != next.densityDpi
    }

    private fun maybeRecreateKeyboardsForConfigChange(newConfig: Configuration): Boolean {
        val prevConfig = lastConfig
        if (prevConfig != null && !hasKeyboardMetricsChanged(prevConfig, newConfig)) {
            lastConfig = Configuration(newConfig)
            return false
        }
        lastConfig = Configuration(newConfig)

        val activePicker = if (pickerMode != null) symbolPickerView?.mode else null
        displayContext = getDisplayContextCompat()
        resetKeyboardCache()
        reloadSettings()
        curKeyboard = qwertyKeyboard
        setLatinKeyboard(qwertyKeyboard)
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        inputView?.invalidateAllKeys()
        inputView?.requestLayout()
        if (activePicker != null && isInputViewShown) showPicker(activePicker)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        candidateBarView?.requestLayout()
        return true
    }

    private fun getQwertyFamiliarKeyboard(withNumberRow: Boolean): FreeShiamyKeyboard {
        val cached = if (withNumberRow) qwertyFamiliarKeyboard else qwertyFamiliarKeyboardNoNumber
        if (cached != null) return cached
        val resource = if (withNumberRow) R.xml.qwerty_familiar else R.xml.qwerty_familiar_no_number
        val created = FreeShiamyKeyboard(requireDisplayContext(), resource)
        if (withNumberRow) qwertyFamiliarKeyboard = created else qwertyFamiliarKeyboardNoNumber = created
        return created
    }

    private fun sendKeyDownUp(ic: InputConnection, keyEventCode: Int) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
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
    override fun onPress(primaryCode: Int) {
        if (primaryCode != 0) inputView?.performKeyHapticFeedback()
    }

    override fun onRelease(primaryCode: Int) {}

    companion object {
        @Volatile private var sharedEngine: ShiamyEngine? = null
        @Volatile private var engineLoading: Boolean = false

        @Volatile private var sharedSpellEngine: SpellEngine? = null
        @Volatile private var spellEngineLoading: Boolean = false
    }

    private enum class ReverseLookupState {
        NONE,
        ENTERING,
        ACTIVE,
    }
}
