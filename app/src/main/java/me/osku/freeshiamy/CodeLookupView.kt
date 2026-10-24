package me.osku.freeshiamy

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import me.osku.freeshiamy.engine.ShiamyEngine
import me.osku.freeshiamy.engine.SpellEngine
import me.osku.freeshiamy.engine.ZhuyinInput
import java.util.Locale
import kotlin.math.roundToInt

/** A local lookup surface. No query keystroke or text edit is sent to the host editor. */
internal class CodeLookupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    interface Listener {
        fun onClose()
        fun onPaste()
        fun onCommit(value: String)
    }

    var listener: Listener? = null
    private val pronunciation = ZhuyinInput()
    private var textQuery = ""
    private var phoneticMode = true
    private var clipboardEmpty = false
    private var shiamy: ShiamyEngine? = null
    private var spells: SpellEngine? = null
    private var tablesLoading = false
    private var heightScale = 1f
    private var haptics = true
    private val expanded = mutableSetOf<Int>()
    private var resultColumns = 2
    private var candidateLimit = CANDIDATE_PAGE_SIZE

    private val phoneticTab = action(R.string.lookup_phonetic) { setMode(true) }.apply {
        id = R.id.lookup_phonetic_tab
    }
    private val textTab = action(R.string.lookup_text) { setMode(false) }.apply {
        id = R.id.lookup_text_tab
    }
    private val query = TextView(context).apply {
        id = R.id.lookup_query
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(color(R.color.candidate_normal))
        setPadding(dp(12), 0, dp(8), 0)
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        autoSize(this, 12, 22)
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val results = GridLayout(context).apply { id = R.id.lookup_results }
    private val resultScroll = ScrollView(context).apply {
        isFillViewport = false
        addView(results, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }
    private val keys = LinearLayout(context).apply {
        id = R.id.lookup_keyboard
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(color(R.color.keyboard_background))
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.candidate_background))
            addView(ImageButton(context).apply {
                id = R.id.lookup_close
                setImageResource(R.drawable.ic_keyboard_back)
                contentDescription = context.getString(R.string.lookup_close)
                styleControl(this)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { listener?.onClose() }
            }, LayoutParams(dp(48), LayoutParams.MATCH_PARENT))
            addView(phoneticTab, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(textTab, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(action(R.string.lookup_paste) { listener?.onPaste() }.apply {
                id = R.id.lookup_paste
            }, LayoutParams(dp(56), LayoutParams.MATCH_PARENT))
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(query, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(action(R.string.lookup_clear) { clearQuery() },
                LayoutParams(dp(56), LayoutParams.MATCH_PARENT))
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(resultScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(112)))

        // Standard Taiwan Zhuyin key positions; the final column holds the extra controls.
        listOf("1234567890-", "qwertyuiop", "asdfghjkl;", "zxcvbnm,./").forEachIndexed { rowIndex, row ->
            keys.addView(LinearLayout(context).apply {
                row.forEach { key ->
                    addView(keyButton(ZhuyinInput.symbols.getValue(key)) { input(key.code) },
                        LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                }
                when (rowIndex) {
                    1 -> addView(keyButton("⌫", R.string.lookup_delete) { backspace() },
                        LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                    2 -> addView(keyButton(context.getString(R.string.lookup_first_tone)) { input(' '.code) },
                        LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                    3 -> addView(keyButton(context.getString(R.string.lookup_clear)) { clearQuery() },
                        LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                }
            }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
        addView(keys, LayoutParams(LayoutParams.MATCH_PARENT, dp(192)))
        render()
    }

    fun open(selectedText: String?) {
        pronunciation.clear()
        textQuery = selectedText.orEmpty()
        phoneticMode = selectedText.isNullOrBlank()
        clipboardEmpty = false
        expanded.clear()
        candidateLimit = CANDIDATE_PAGE_SIZE
        render()
    }

    fun reset() {
        pronunciation.clear()
        textQuery = ""
        clipboardEmpty = false
        expanded.clear()
        candidateLimit = CANDIDATE_PAGE_SIZE
    }

    fun setEngines(engine: ShiamyEngine?, spellEngine: SpellEngine?, loading: Boolean) {
        if (shiamy === engine && spells === spellEngine && tablesLoading == loading) return
        shiamy = engine
        spells = spellEngine
        tablesLoading = loading
        render()
    }

    fun setHeightScale(scale: Float) {
        heightScale = scale.coerceIn(0.9f, 2.2f)
        requestLayout()
    }

    fun setHaptics(enabled: Boolean) { haptics = enabled }

    fun showClipboard(text: String?) {
        textQuery = text.orEmpty()
        clipboardEmpty = text.isNullOrBlank()
        phoneticMode = false
        expanded.clear()
        render()
    }

    fun input(code: Int) {
        when (code) {
            android.inputmethodservice.Keyboard.KEYCODE_CANCEL -> listener?.onClose()
            android.inputmethodservice.Keyboard.KEYCODE_DELETE -> backspace()
            // Enter is deliberately local; only an explicit candidate tap sends a character.
            10 -> Unit
            else -> if (phoneticMode && code in Char.MIN_VALUE.code..Char.MAX_VALUE.code &&
                pronunciation.input(code.toChar())) {
                expanded.clear()
                candidateLimit = CANDIDATE_PAGE_SIZE
                render()
            }
        }
    }

    fun inputText(text: CharSequence) {
        // onText may carry a pasted character or a full Unicode value from a hardware IME.
        if (text.any { it in '\u3400'..'\u9fff' || it in '\uf900'..'\ufaff' } ||
            text.any { Character.isSurrogate(it) }) {
            showClipboard(text.toString())
        } else {
            text.forEach { input(it.code) }
        }
    }

    fun backspace() {
        if (phoneticMode) pronunciation.backspace()
        else if (textQuery.isNotEmpty()) {
            textQuery = textQuery.substring(0, Character.offsetByCodePoints(textQuery, textQuery.length, -1))
        }
        clipboardEmpty = false
        expanded.clear()
        candidateLimit = CANDIDATE_PAGE_SIZE
        render()
    }

    private fun clearQuery() {
        if (phoneticMode) pronunciation.clear() else textQuery = ""
        clipboardEmpty = false
        expanded.clear()
        candidateLimit = CANDIDATE_PAGE_SIZE
        render()
    }

    private fun setMode(phonetic: Boolean) {
        phoneticMode = phonetic
        expanded.clear()
        render()
    }

    private fun render() {
        phoneticTab.isSelected = phoneticMode
        textTab.isSelected = !phoneticMode
        phoneticTab.setTextColor(color(if (phoneticMode) R.color.candidate_recommended else R.color.candidate_other))
        textTab.setTextColor(color(if (phoneticMode) R.color.candidate_other else R.color.candidate_recommended))
        keys.visibility = if (phoneticMode) View.VISIBLE else View.GONE
        query.text = if (phoneticMode) pronunciation.display.ifEmpty {
            context.getString(R.string.lookup_phonetic_hint)
        } else textQuery.ifBlank { context.getString(R.string.lookup_text_hint) }
        results.removeAllViews()
        results.columnCount = resultColumns

        if (shiamy == null || (phoneticMode && spells == null)) {
            message(if (tablesLoading) R.string.lookup_loading else R.string.lookup_load_failed)
        } else if (phoneticMode) {
            val base = pronunciation.code.trimEnd('3', '4', '6', '7')
            if (base.isEmpty()) {
                message(R.string.lookup_phonetic_help)
            } else {
                val candidates = spells!!.valuesForPronunciation(pronunciation.code, pronunciation.hasTone)
                if (candidates.isEmpty()) message(R.string.lookup_no_pronunciation)
                candidates.take(candidateLimit).forEachIndexed { index, value -> addResult(value, index, canCommit = true) }
                if (candidates.size > candidateLimit) {
                    val position = results.childCount
                    results.addView(action(R.string.lookup_more_candidates) {
                        val scrollY = resultScroll.scrollY
                        candidateLimit += CANDIDATE_PAGE_SIZE
                        render()
                        resultScroll.post { resultScroll.scrollTo(0, scrollY) }
                    }, GridLayout.LayoutParams().apply {
                        columnSpec = GridLayout.spec(0, resultColumns, 1f)
                        rowSpec = GridLayout.spec((position + resultColumns - 1) / resultColumns)
                        width = 0
                        height = dp(48)
                    })
                }
            }
        } else {
            val values = characters(textQuery)
            if (values.isEmpty()) message(if (clipboardEmpty) R.string.lookup_clipboard_empty else R.string.lookup_text_help)
            values.forEachIndexed { index, value -> addResult(value, index, canCommit = false) }
            if (textQuery.codePointCount(0, textQuery.length) > MAX_TEXT_CHARACTERS) {
                message(R.string.lookup_text_limit)
            }
        }
        resultScroll.scrollTo(0, 0)
        requestLayout()
    }

    private fun addResult(value: String, index: Int, canCommit: Boolean) {
        val codes = shiamy?.codesForValue(value).orEmpty()
        val shortest = codes.firstOrNull()?.uppercase(Locale.ROOT)
            ?: context.getString(R.string.lookup_no_code)
        val cell = LinearLayout(context).apply { orientation = VERTICAL }
        val heading = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val caption = SpannableString("$value\n$shortest").apply {
            setSpan(RelativeSizeSpan(0.6f), value.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(TypefaceSpan("monospace"), value.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val label = if (canCommit) Button(context).apply {
            styleControl(this)
            isAllCaps = false
            contentDescription = context.getString(R.string.lookup_commit_description, value, shortest)
            setOnClickListener { listener?.onCommit(value) }
        } else TextView(context).apply {
            gravity = Gravity.CENTER
            contentDescription = context.getString(R.string.lookup_result_description, value, shortest)
        }
        label.text = caption
        label.setTextColor(color(R.color.candidate_normal))
        label.setPadding(dp(4), dp(4), dp(4), dp(4))
        label.minHeight = dp(64)
        label.maxLines = 2
        autoSize(label, 18, 28)
        heading.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (codes.size > 1) {
            heading.addView(action(if (index in expanded) R.string.lookup_less_codes else R.string.lookup_more_codes) {
                val scrollY = resultScroll.scrollY
                if (!expanded.add(index)) expanded.remove(index)
                render()
                resultScroll.post { resultScroll.scrollTo(0, scrollY) }
            }.apply {
                contentDescription = context.getString(R.string.lookup_more_description, value)
            }, LayoutParams(dp(48), dp(64)))
        }
        cell.addView(heading, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        if (index in expanded) {
            cell.addView(TextView(context).apply {
                text = codes.joinToString("  ") { it.uppercase(Locale.ROOT) }
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextColor(color(R.color.candidate_other))
                setPadding(dp(8), dp(4), dp(8), dp(8))
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        val position = results.childCount
        results.addView(cell, GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(position % resultColumns, 1f)
            rowSpec = GridLayout.spec(position / resultColumns)
            width = 0
            height = LayoutParams.WRAP_CONTENT
            setMargins(dp(2), dp(1), dp(2), dp(1))
        })
    }

    private fun message(resource: Int) {
        val position = results.childCount
        results.addView(TextView(context).apply {
            text = context.getString(resource)
            textSize = 14f
            setTextColor(color(R.color.candidate_other))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }, GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(0, resultColumns, 1f)
            rowSpec = GridLayout.spec((position + resultColumns - 1) / resultColumns)
            width = 0
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
            else MeasureSpec.getSize(heightMeasureSpec)
        val screenCap = (resources.displayMetrics.heightPixels * 0.65f).roundToInt()
        val desired = dp(208 + 192 * heightScale)
        val height = minOf(desired, available, screenCap)
        val contentHeight = (height - dp(96)).coerceAtLeast(0)
        // One complete 64dp candidate plus its margins must remain visible even in landscape.
        val keyHeight = if (phoneticMode) minOf(dp(192 * heightScale),
            contentHeight - minOf(dp(68), contentHeight)) else 0
        keys.layoutParams.height = keyHeight
        resultScroll.layoutParams.height = contentHeight - keyHeight
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val columns = (w / dp(144)).coerceIn(1, 4)
        if (resultColumns != columns) {
            resultColumns = columns
            render()
        }
    }

    private fun characters(text: String): List<String> {
        val values = ArrayList<String>()
        var offset = 0
        var count = 0
        while (offset < text.length && count < MAX_TEXT_CHARACTERS) {
            val codePoint = Character.codePointAt(text, offset)
            val length = Character.charCount(codePoint)
            if (!Character.isWhitespace(codePoint)) values.add(text.substring(offset, offset + length))
            offset += length
            count++
        }
        return values
    }

    private fun action(resource: Int, onClick: () -> Unit): Button = Button(context).apply {
        text = context.getString(resource)
        isAllCaps = false
        gravity = Gravity.CENTER
        styleControl(this)
        setTextColor(color(R.color.candidate_normal))
        setPadding(dp(2), 0, dp(2), 0)
        autoSize(this, 11, 14)
        setOnClickListener { onClick() }
    }

    private fun keyButton(label: String, description: Int? = null, onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            contentDescription = description?.let(context::getString) ?: label
            isAllCaps = false
            styleControl(this)
            setTextColor(color(R.color.candidate_normal))
            setPadding(0, 0, 0, 0)
            autoSize(this, 10, 22)
            setOnClickListener {
                if (haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        }

    private fun styleControl(view: View) {
        view.setBackgroundResource(R.drawable.keyboard_key_background)
        view.backgroundTintList = null
        view.minimumWidth = 0
        view.minimumHeight = 0
        if (view is Button) {
            view.minWidth = 0
            view.minHeight = 0
        }
    }

    private fun autoSize(view: TextView, min: Int, max: Int) {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(view, min, max, 1, TypedValue.COMPLEX_UNIT_SP)
    }

    private fun color(resource: Int) = resources.getColor(resource, null)
    private fun dp(value: Int) = dp(value.toFloat())
    private fun dp(value: Float) = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val MAX_TEXT_CHARACTERS = 128
        private const val CANDIDATE_PAGE_SIZE = 40
    }
}
