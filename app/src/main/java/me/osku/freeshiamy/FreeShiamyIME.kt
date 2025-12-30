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
import java.util.Locale

/**
 * FreeShiamy IME
 */
class FreeShiamyIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var inputMethodManager: InputMethodManager? = null
    private var inputView: FreeShiamyKeyboardView? = null
    private var candidateBarView: CandidateBarView? = null

    private val rawBuffer = StringBuilder()
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
    private var capsLock = false

    private var symbolsKeyboard: FreeShiamyKeyboard? = null
    private var symbolsShiftedKeyboard: FreeShiamyKeyboard? = null
    private var emojiKeyboard: FreeShiamyKeyboard? = null
    private var qwertyOriginalKeyboard: FreeShiamyKeyboard? = null
    private var qwertyOriginalKeyboardNoNumber: FreeShiamyKeyboard? = null
    private var qwertyStandardKeyboard: FreeShiamyKeyboard? = null
    private var qwertyStandardKeyboardNoNumber: FreeShiamyKeyboard? = null
    private var qwertyStandardSpaciousKeyboard: FreeShiamyKeyboard? = null
    private var qwertyStandardSpaciousKeyboardNoNumber: FreeShiamyKeyboard? = null
    private var qwertyKeyboard: FreeShiamyKeyboard? = null
    private var curKeyboard: FreeShiamyKeyboard? = null
    private var keyboardLayout: String = SettingsKeys.DEFAULT_KEYBOARD_LAYOUT
    private var showNumberRow: Boolean = SettingsKeys.DEFAULT_SHOW_NUMBER_ROW
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
        resetKeyboardCache(precreateSymbols = true)
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.input, null) as ViewGroup
        candidateBarView = root.findViewById(R.id.candidate_bar)
        candidateBarView?.listener = object : CandidateBarView.Listener {
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
        inputView?.setPreviewEnabled(false)
        reloadSettings()
        setLatinKeyboard(qwertyKeyboard)
        inputView?.setHeightScale(keyboardHeightPercent / 100f)
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        return root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        maybeRecreateKeyboardsForConfigChange(newConfig)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        clearState()
        isInSensitiveField = disableImeInSensitiveFields && isSensitiveField(attribute)
        curKeyboard = qwertyKeyboard
        curKeyboard?.setImeOptions(resources, attribute.imeOptions)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        clearState()
        suppressExternalDeleteUntilDeleteReleased = false
        inputView?.closing()
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        suppressExternalDeleteUntilDeleteReleased = false
        inputView?.closing()
        reloadSettings()
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

    override fun onText(text: CharSequence?) {
        if (text.isNullOrEmpty()) return
        val ic = currentInputConnection ?: return
        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        val shouldDirectCommit =
            inputView?.keyboard === emojiKeyboard || text.length != 1 || containsSurrogate(text)
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

        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        fun finish(result: Boolean): Boolean {
            if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
                updateUi()
            }
            return result
        }

        when (keyCode) {
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
            handlePrimaryCode(unicodeChar)
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

    private fun handlePrimaryCode(primaryCode: Int) {
        val ic = currentInputConnection ?: return
        val clearedHint = shortestCodeHintText != null
        shortestCodeHintText = null

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_CANCEL -> handleClose()
            Keyboard.KEYCODE_MODE_CHANGE -> handleModeChange()
            FreeShiamyKeyboardView.KEYCODE_LANGUAGE_SWITCH -> handleLanguageSwitch()
            FreeShiamyKeyboardView.KEYCODE_EMOJI -> handleEmojiSwitch()
            FreeShiamyKeyboardView.KEYCODE_OPTIONS -> {
                openSettings()
            }

            10 -> handleEnter(ic)
            32 -> handleSpace(ic)
            in 48..57 -> handleDigit(primaryCode, ic)
            else -> handleCharacter(primaryCode, ic)
        }

        if (clearedHint && rawBuffer.isEmpty() && shortestCodeHintText == null) {
            updateUi()
        }
    }

    private fun applyShiftToLetter(ch: Char): Char {
        val view = inputView
        val isShifted = view?.isShifted == true
        val base = ch.lowercaseChar()
        val typed = if (isShifted) base.uppercaseChar() else base
        if (isShifted && !capsLock) {
            view?.isShifted = false
        }
        return typed
    }

    private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            cancelReverseLookup()
        }

        val ch = primaryCode.toChar()

        if (isSymbolsKeyboardActive()) {
            ic.commitText(ch.toString(), 1)
            return
        }

        if (ch.isLetter()) {
            val typed = applyShiftToLetter(ch)
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
        val digitIndex = primaryCode - '0'.toInt()

        if (reverseLookupState == ReverseLookupState.ACTIVE) {
            if (digitIndex < exactCount) {
                commitCandidateAt(digitIndex, ic)
            }
            return
        }

        if (isSymbolsKeyboardActive()) {
            ic.commitText(primaryCode.toChar().toString(), 1)
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
            rawBuffer.deleteCharAt(rawBuffer.length - 1)
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

        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (ic.performEditorAction(action)) return
        }

        ic.commitText("\n", 1)
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
        prefixCandidates = emptyList()
        exactCount = 0
        reverseLookupState = ReverseLookupState.NONE
        candidateBarView?.setExpanded(false)
        shortestCodeHintText = null
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
        val shouldShowCandidateBar = rawText.isNotEmpty() || !hintText.isNullOrBlank()
        candidateBarView?.visibility = if (shouldShowCandidateBar) View.VISIBLE else View.INVISIBLE
        candidateBarView?.setState(
            rawText = rawText,
            prefixCandidates = prefixCandidates,
            exactCount = exactCount,
            hintText = hintText,
        )
    }

    private fun reloadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        keyboardHeightPercent = prefs.getInt(
            SettingsKeys.KEY_KEYBOARD_HEIGHT_PERCENT,
            SettingsKeys.DEFAULT_KEYBOARD_HEIGHT_PERCENT,
        ).coerceAtLeast(SettingsKeys.MIN_KEYBOARD_HEIGHT_PERCENT)
        keyboardLayout = prefs.getString(SettingsKeys.KEY_KEYBOARD_LAYOUT, SettingsKeys.DEFAULT_KEYBOARD_LAYOUT)
            ?: SettingsKeys.DEFAULT_KEYBOARD_LAYOUT
        showNumberRow = prefs.getBoolean(SettingsKeys.KEY_SHOW_NUMBER_ROW, SettingsKeys.DEFAULT_SHOW_NUMBER_ROW)

        qwertyKeyboard =
            when (keyboardLayout) {
                "original" ->
                    if (showNumberRow) getQwertyOriginalKeyboard() else getQwertyOriginalKeyboardNoNumber()
                "standard_spacious" ->
                    if (showNumberRow) getQwertyStandardSpaciousKeyboard() else getQwertyStandardSpaciousKeyboardNoNumber()
                "standard_spacious_label_top" ->
                    if (showNumberRow) getQwertyStandardSpaciousKeyboard() else getQwertyStandardSpaciousKeyboardNoNumber()
                "standard_label_top" ->
                    if (showNumberRow) getQwertyStandardKeyboard() else getQwertyStandardKeyboardNoNumber()
                else ->
                    if (showNumberRow) getQwertyStandardKeyboard() else getQwertyStandardKeyboardNoNumber()
            }

        if (
            curKeyboard === qwertyOriginalKeyboard ||
            curKeyboard === qwertyOriginalKeyboardNoNumber ||
            curKeyboard === qwertyStandardKeyboard ||
            curKeyboard === qwertyStandardKeyboardNoNumber ||
            curKeyboard === qwertyStandardSpaciousKeyboard ||
            curKeyboard === qwertyStandardSpaciousKeyboardNoNumber
        ) {
            curKeyboard = qwertyKeyboard
        }
        inputView?.setLabelTopAligned(
            keyboardLayout == "standard_label_top" || keyboardLayout == "standard_spacious_label_top",
        )
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
        val shouldSupportLanguageSwitchKey =
            inputMethodManager?.shouldOfferSwitchingToNextInputMethod(getToken()) ?: false
        nextKeyboard?.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey)
        inputView?.keyboard = nextKeyboard
    }

    private fun getToken(): IBinder? {
        val dialog = window ?: return null
        val w = dialog.window ?: return null
        return w.attributes.token
    }

    private fun handleModeChange() {
        val view = inputView ?: return
        val current = view.keyboard
        val symbols = symbolsKeyboard
        val symbolsShifted = symbolsShiftedKeyboard
        if (current === symbols || current === symbolsShifted || current === emojiKeyboard) {
            setLatinKeyboard(qwertyKeyboard)
        } else {
            val target = symbols ?: getSymbolsKeyboard()
            setLatinKeyboard(target)
            target.isShifted = false
        }
    }

    private fun handleEmojiSwitch() {
        val view = inputView ?: return
        val current = view.keyboard
        val emoji = emojiKeyboard
        if (current === emoji) {
            setLatinKeyboard(qwertyKeyboard)
            return
        }
        val target = emoji ?: getEmojiKeyboard()
        setLatinKeyboard(target)
        target.isShifted = false
    }

    private fun handleLanguageSwitch() {
        inputMethodManager?.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */)
    }

    private fun handleShift() {
        val view = inputView ?: return
        val currentKeyboard = view.keyboard

        if (qwertyKeyboard === currentKeyboard) {
            val now = System.currentTimeMillis()
            if (capsLock) {
                capsLock = false
                lastShiftTime = 0
                view.isShifted = false
                return
            }

            val alreadyShifted = view.isShifted
            if (alreadyShifted && lastShiftTime + 800 > now) {
                capsLock = true
                lastShiftTime = 0
                view.isShifted = true
            } else {
                view.isShifted = !alreadyShifted
                lastShiftTime = now
            }
            return
        }

        val symbols = getSymbolsKeyboard()
        val symbolsShifted = getSymbolsShiftedKeyboard()
        if (currentKeyboard === symbols) {
            symbols.isShifted = true
            setLatinKeyboard(symbolsShifted)
            symbolsShifted.isShifted = true
            return
        }

        if (currentKeyboard === symbolsShifted) {
            symbolsShifted.isShifted = false
            setLatinKeyboard(symbols)
            symbols.isShifted = false
        }
    }

    private fun handleClose() {
        if (rawBuffer.isNotEmpty()) {
            commitRawBuffer()
        }
        requestHideSelf(0)
        inputView?.closing()
    }

    private fun isSymbolsKeyboardActive(): Boolean {
        val currentKeyboard = inputView?.keyboard
        return currentKeyboard === symbolsKeyboard ||
            currentKeyboard === symbolsShiftedKeyboard ||
            currentKeyboard === emojiKeyboard
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

    private fun resetKeyboardCache(precreateSymbols: Boolean) {
        qwertyOriginalKeyboard = null
        qwertyOriginalKeyboardNoNumber = null
        qwertyStandardKeyboard = null
        qwertyStandardKeyboardNoNumber = null
        qwertyStandardSpaciousKeyboard = null
        qwertyStandardSpaciousKeyboardNoNumber = null
        qwertyKeyboard = null
        if (precreateSymbols) {
            symbolsKeyboard = FreeShiamyKeyboard(requireDisplayContext(), R.xml.symbols)
            symbolsShiftedKeyboard = FreeShiamyKeyboard(requireDisplayContext(), R.xml.symbols_shift)
        } else {
            symbolsKeyboard = null
            symbolsShiftedKeyboard = null
        }
        emojiKeyboard = null
        curKeyboard = null
    }

    private fun hasKeyboardMetricsChanged(prev: Configuration, next: Configuration): Boolean {
        return prev.orientation != next.orientation ||
            prev.screenWidthDp != next.screenWidthDp ||
            prev.screenHeightDp != next.screenHeightDp ||
            prev.smallestScreenWidthDp != next.smallestScreenWidthDp ||
            prev.densityDpi != next.densityDpi
    }

    private fun maybeRecreateKeyboardsForConfigChange(newConfig: Configuration) {
        val prevConfig = lastConfig
        if (prevConfig != null && !hasKeyboardMetricsChanged(prevConfig, newConfig)) {
            lastConfig = Configuration(newConfig)
            return
        }
        lastConfig = Configuration(newConfig)

        val view = inputView
        val currentKeyboard = view?.keyboard
        val wasSymbols = currentKeyboard === symbolsKeyboard
        val wasSymbolsShifted = currentKeyboard === symbolsShiftedKeyboard
        val wasEmoji = currentKeyboard === emojiKeyboard
        val wasShifted = view?.isShifted == true

        displayContext = getDisplayContextCompat()
        resetKeyboardCache(precreateSymbols = true)
        reloadSettings()
        curKeyboard = qwertyKeyboard

        val nextKeyboard =
            when {
                wasEmoji -> getEmojiKeyboard()
                wasSymbolsShifted -> {
                    val symbols = getSymbolsKeyboard()
                    val shifted = getSymbolsShiftedKeyboard()
                    symbols.isShifted = true
                    shifted.isShifted = true
                    shifted
                }
                wasSymbols -> {
                    val symbols = getSymbolsKeyboard()
                    val shifted = getSymbolsShiftedKeyboard()
                    symbols.isShifted = false
                    shifted.isShifted = false
                    symbols
                }
                else -> qwertyKeyboard
            }

        if (view != null && nextKeyboard != null) {
            setLatinKeyboard(nextKeyboard)
            if (!wasSymbols && !wasSymbolsShifted && !wasEmoji) {
                view.isShifted = wasShifted
            }
            view.setHeightScale(keyboardHeightPercent / 100f)
            view.invalidateAllKeys()
            view.requestLayout()
        }
        candidateBarView?.setLimits(candidateInlineLimit, candidateMoreLimit)
        candidateBarView?.requestLayout()
    }

    private fun getQwertyOriginalKeyboard(): FreeShiamyKeyboard {
        val cached = qwertyOriginalKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_original)
        qwertyOriginalKeyboard = created
        return created
    }

    private fun getQwertyOriginalKeyboardNoNumber(): FreeShiamyKeyboard {
        val cached = qwertyOriginalKeyboardNoNumber
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_original_no_number)
        qwertyOriginalKeyboardNoNumber = created
        return created
    }

    private fun getQwertyStandardKeyboard(): FreeShiamyKeyboard {
        val cached = qwertyStandardKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_standard)
        qwertyStandardKeyboard = created
        return created
    }

    private fun getQwertyStandardKeyboardNoNumber(): FreeShiamyKeyboard {
        val cached = qwertyStandardKeyboardNoNumber
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_standard_no_number)
        qwertyStandardKeyboardNoNumber = created
        return created
    }

    private fun getQwertyStandardSpaciousKeyboard(): FreeShiamyKeyboard {
        val cached = qwertyStandardSpaciousKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_standard_spacious)
        qwertyStandardSpaciousKeyboard = created
        return created
    }

    private fun getQwertyStandardSpaciousKeyboardNoNumber(): FreeShiamyKeyboard {
        val cached = qwertyStandardSpaciousKeyboardNoNumber
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.qwerty_standard_spacious_no_number)
        qwertyStandardSpaciousKeyboardNoNumber = created
        return created
    }

    private fun getSymbolsKeyboard(): FreeShiamyKeyboard {
        val cached = symbolsKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.symbols)
        symbolsKeyboard = created
        return created
    }

    private fun getSymbolsShiftedKeyboard(): FreeShiamyKeyboard {
        val cached = symbolsShiftedKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.symbols_shift)
        symbolsShiftedKeyboard = created
        return created
    }

    private fun getEmojiKeyboard(): FreeShiamyKeyboard {
        val cached = emojiKeyboard
        if (cached != null) return cached
        val created = FreeShiamyKeyboard(requireDisplayContext(), R.xml.emoji)
        emojiKeyboard = created
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
    override fun onPress(primaryCode: Int) {}

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
