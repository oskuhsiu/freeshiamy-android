package me.osku.freeshiamy.ui

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import me.osku.freeshiamy.R
import me.osku.freeshiamy.engine.CinEntry
import kotlin.math.roundToInt

class CandidateBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onRawClick()
        fun onCandidateClick(entry: CinEntry)
        fun onSettingsClick()
        fun onLanguageSwitchClick()
        fun onHideKeyboardClick()
        fun onEnglishLockClick()
        fun onLookupClick()
    }

    private val candidateBarRow: LinearLayout
    private val rawTextView: TextView
    private val hintTextView: TextView
    private val candidateScroll: HorizontalScrollView
    private val candidateContainer: LinearLayout
    private val moreButton: Button
    private val toolsButton: ImageButton
    private val toolbarRow: LinearLayout
    private val settingsButton: ImageButton
    private val languageSwitchButton: ImageButton
    private val hideKeyboardButton: ImageButton
    private val englishLockButton: ImageButton
    private val returnButton: ImageButton
    private val moreScroll: ScrollView
    private val candidateGrid: GridLayout

    private var expanded = false
    private var toolsRequested = false
    private var hasCandidateContent = false
    private var currentCandidates: List<CinEntry> = emptyList()
    private var currentExactCount: Int = 0
    private var inlineLimit: Int = 10
    private var maxMoreItems: Int = 200

    private val baseRowHeightPx: Int by lazy { resources.getDimensionPixelSize(R.dimen.candidate_bar_height) }
    private var headerRowHeightPx: Int = 0
    private var autoSizeMinSp: Int = 12
    private var autoSizeMaxSp: Int = 24

    private val candidateButtonMinWidthPx: Int by lazy { dpToPx(48f) }
    private val candidateButtonHorizontalPaddingPx: Int by lazy { dpToPx(8f) }
    private val candidateButtonHorizontalMarginPx: Int by lazy { dpToPx(1f) }

    var listener: Listener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.candidate_bar, this, true)

        candidateBarRow = findViewById(R.id.candidate_bar_row)
        rawTextView = findViewById(R.id.raw_text)
        hintTextView = findViewById(R.id.hint_text)
        candidateScroll = findViewById(R.id.candidate_scroll)
        candidateContainer = findViewById(R.id.candidate_container)
        moreButton = findViewById(R.id.more_button)
        toolsButton = findViewById(R.id.tools_button)
        toolbarRow = findViewById(R.id.keyboard_toolbar_row)
        settingsButton = findViewById(R.id.keyboard_settings_button)
        languageSwitchButton = findViewById(R.id.keyboard_switch_button)
        hideKeyboardButton = findViewById(R.id.keyboard_hide_button)
        englishLockButton = findViewById(R.id.keyboard_english_lock_button)
        englishLockButton.imageTintList = context.getColorStateList(R.color.keyboard_toggle_tint)
        returnButton = findViewById(R.id.keyboard_return_button)
        moreScroll = findViewById(R.id.more_scroll)
        candidateGrid = findViewById(R.id.candidate_grid)

        rawTextView.setOnClickListener { listener?.onRawClick() }
        moreButton.setOnClickListener { setExpanded(!expanded) }
        toolsButton.setOnClickListener {
            toolsRequested = true
            updateSlotVisibility()
        }
        returnButton.setOnClickListener {
            showCandidates()
        }
        settingsButton.setOnClickListener { listener?.onSettingsClick() }
        languageSwitchButton.setOnClickListener { listener?.onLanguageSwitchClick() }
        hideKeyboardButton.setOnClickListener { listener?.onHideKeyboardClick() }
        englishLockButton.setOnClickListener { listener?.onEnglishLockClick() }
        findViewById<ImageButton>(R.id.keyboard_lookup_button).setOnClickListener { listener?.onLookupClick() }

        // Keep the header height stable as candidates and hints change.
        setHeightScale(1f)
        updateSlotVisibility()
    }

    fun setLimits(inlineLimit: Int, maxMoreItems: Int) {
        this.inlineLimit = inlineLimit.coerceAtLeast(1)
        this.maxMoreItems = maxMoreItems.coerceAtLeast(this.inlineLimit)
    }

    fun setEnglishLocked(locked: Boolean) {
        englishLockButton.isSelected = locked
        englishLockButton.contentDescription = context.getString(
            if (locked) R.string.keyboard_english_lock_on else R.string.keyboard_english_lock_off
        )
    }

    fun showCandidates() {
        toolsRequested = false
        updateSlotVisibility()
    }

    fun setState(rawText: String, prefixCandidates: List<CinEntry>, exactCount: Int, hintText: String? = null) {
        rawTextView.text = formatRawText(rawText)
        rawTextView.contentDescription = context.getString(
            R.string.candidate_commit_raw_description, rawTextView.text
        )

        val shouldShowHint = rawText.isEmpty() && !hintText.isNullOrBlank()
        hasCandidateContent = rawText.isNotEmpty() || shouldShowHint
        if (!hasCandidateContent) toolsRequested = false
        if (shouldShowHint) {
            hintTextView.text = hintText
            hintTextView.visibility = View.VISIBLE
            candidateScroll.visibility = View.GONE
            moreButton.visibility = View.GONE
            moreScroll.visibility = View.GONE
            expanded = false
            currentCandidates = emptyList()
            currentExactCount = 0
            candidateContainer.removeAllViews()
            candidateGrid.removeAllViews()
            updateSlotVisibility()
            return
        }

        hintTextView.visibility = View.GONE
        candidateScroll.visibility = View.VISIBLE
        currentCandidates = prefixCandidates
        currentExactCount = exactCount.coerceIn(0, prefixCandidates.size)

        if (expanded && currentCandidates.isEmpty()) {
            setExpanded(false)
        } else if (expanded) {
            renderExpandedCandidates()
        }

        renderInlineCandidates()
        renderMoreButton()
        updateSlotVisibility()
    }

    fun setExpanded(expand: Boolean) {
        expanded = expand
        moreButton.text = if (expanded) "▲" else "▼"
        moreButton.contentDescription = context.getString(
            if (expanded) R.string.candidate_collapse else R.string.candidate_expand
        )
        if (expanded) {
            renderExpandedCandidates()
        }
        updateSlotVisibility()
    }

    private fun updateSlotVisibility() {
        val showTools = toolsRequested || !hasCandidateContent
        toolbarRow.visibility = if (showTools) View.VISIBLE else View.GONE
        candidateBarRow.visibility = if (showTools) View.GONE else View.VISIBLE
        returnButton.visibility = if (hasCandidateContent) View.VISIBLE else View.GONE
        moreScroll.visibility = if (!showTools && expanded) View.VISIBLE else View.GONE
    }

    private fun renderMoreButton() {
        val hasMoreCandidates = currentCandidates.size > inlineLimit
        moreButton.visibility = if (hasMoreCandidates) View.VISIBLE else View.GONE
        if (!hasMoreCandidates) setExpanded(false)
    }

    private fun renderInlineCandidates() {
        candidateContainer.removeAllViews()
        val count = minOf(inlineLimit, currentCandidates.size)
        for (i in 0 until count) {
            val entry = currentCandidates[i]
            candidateContainer.addView(createCandidateButton(entry, i))
        }
    }

    private fun renderExpandedCandidates() {
        candidateGrid.removeAllViews()

        val count = minOf(maxMoreItems, currentCandidates.size)
        for (i in 0 until count) {
            val entry = currentCandidates[i]
            val button = createCandidateButton(entry, i)
            val params = GridLayout.LayoutParams()
            params.columnSpec = GridLayout.spec(i % candidateGrid.columnCount, 1f)
            params.rowSpec = GridLayout.spec(i / candidateGrid.columnCount)
            params.setMargins(candidateButtonHorizontalMarginPx, candidateButtonHorizontalMarginPx, candidateButtonHorizontalMarginPx, candidateButtonHorizontalMarginPx)
            params.width = 0
            params.height = headerRowHeightPx
            button.layoutParams = params
            candidateGrid.addView(button)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val availableWidth = (w - candidateGrid.paddingLeft - candidateGrid.paddingRight).coerceAtLeast(0)
        val minCellWidth = candidateButtonMinWidthPx + candidateButtonHorizontalMarginPx * 2
        val columnCount = (availableWidth / minCellWidth).coerceIn(1, 5)
        if (candidateGrid.columnCount != columnCount) {
            candidateGrid.removeAllViews()
            candidateGrid.columnCount = columnCount
            if (expanded) renderExpandedCandidates()
        }
    }

    fun setHeightScale(scale: Float) {
        val safeScale = scale.coerceAtLeast(0.5f)
        val newRowHeightPx = (baseRowHeightPx * safeScale).roundToInt().coerceAtLeast(dpToPx(48f))
        if (newRowHeightPx == headerRowHeightPx) return

        headerRowHeightPx = newRowHeightPx
        for (row in listOf(candidateBarRow, toolbarRow)) {
            val rowLayoutParams = row.layoutParams
                ?: LayoutParams(LayoutParams.MATCH_PARENT, headerRowHeightPx)
            rowLayoutParams.height = headerRowHeightPx
            row.layoutParams = rowLayoutParams
        }

        autoSizeMinSp = (12f * safeScale).roundToInt().coerceAtLeast(8)
        autoSizeMaxSp = (18f * safeScale).roundToInt().coerceAtLeast(autoSizeMinSp)
        applyAutoSize(rawTextView)
        applyAutoSize(hintTextView)
        applyAutoSize(moreButton)

        // Re-create buttons to apply the new sizing config.
        renderInlineCandidates()
        if (expanded) renderExpandedCandidates()
    }

    private fun applyAutoSize(view: TextView) {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            view,
            autoSizeMinSp,
            autoSizeMaxSp,
            1,
            TypedValue.COMPLEX_UNIT_SP,
        )
    }

    private fun formatRawText(rawText: String): CharSequence {
        if (rawText.indexOf(' ') == -1) return rawText
        return rawText.replace(" ", "⎵")
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt()
            .coerceAtLeast(0)
    }

    private fun createCandidateButton(entry: CinEntry, index: Int): Button {
        val button = Button(context)
        button.setBackgroundResource(R.drawable.keyboard_key_background)
        button.backgroundTintList = null
        button.text = entry.value
        button.isAllCaps = false
        button.minWidth = candidateButtonMinWidthPx
        button.minimumWidth = candidateButtonMinWidthPx
        button.minHeight = 0
        button.isSingleLine = true
        button.ellipsize = TextUtils.TruncateAt.END
        button.setPadding(candidateButtonHorizontalPaddingPx, 0, candidateButtonHorizontalPaddingPx, 0)
        applyAutoSize(button)

        val normal = resources.getColor(R.color.candidate_normal)
        val recommended = resources.getColor(R.color.candidate_recommended)
        val other = resources.getColor(R.color.candidate_other)

        button.setTextColor(
            when {
                index == 0 && index < currentExactCount -> recommended
                index < currentExactCount -> normal
                else -> other
            }
        )

        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        lp.setMargins(candidateButtonHorizontalMarginPx, 0, candidateButtonHorizontalMarginPx, 0)
        button.layoutParams = lp
        button.setOnClickListener { listener?.onCandidateClick(entry) }
        return button
    }
}
