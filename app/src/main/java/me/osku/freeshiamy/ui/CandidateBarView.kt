package me.osku.freeshiamy.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
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
    }

    private val candidateBarRow: LinearLayout
    private val rawTextView: TextView
    private val hintTextView: TextView
    private val candidateScroll: HorizontalScrollView
    private val candidateContainer: LinearLayout
    private val moreButton: Button
    private val moreScroll: ScrollView
    private val candidateGrid: GridLayout

    private var expanded = false
    private var currentCandidates: List<CinEntry> = emptyList()
    private var currentExactCount: Int = 0
    private var inlineLimit: Int = 10
    private var maxMoreItems: Int = 200

    private val baseRowHeightPx: Int by lazy { resources.getDimensionPixelSize(R.dimen.key_height) }
    private var headerRowHeightPx: Int = 0
    private var autoSizeMinSp: Int = 12
    private var autoSizeMaxSp: Int = 18

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
        moreScroll = findViewById(R.id.more_scroll)
        candidateGrid = findViewById(R.id.candidate_grid)

        rawTextView.setOnClickListener { listener?.onRawClick() }
        moreButton.setOnClickListener { setExpanded(!expanded) }

        // Default to 1x keyboard row height so the top view matches a key row out of the box.
        setHeightScale(1f)
    }

    fun setLimits(inlineLimit: Int, maxMoreItems: Int) {
        this.inlineLimit = inlineLimit.coerceAtLeast(1)
        this.maxMoreItems = maxMoreItems.coerceAtLeast(this.inlineLimit)
    }

    fun setState(rawText: String, prefixCandidates: List<CinEntry>, exactCount: Int, hintText: String? = null) {
        rawTextView.text = rawText

        val shouldShowHint = rawText.isEmpty() && !hintText.isNullOrBlank()
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
    }

    fun setExpanded(expand: Boolean) {
        expanded = expand
        moreScroll.visibility = if (expanded) View.VISIBLE else View.GONE
        moreButton.text = if (expanded) "▲" else "▼"
        if (expanded) {
            renderExpandedCandidates()
        }
    }

    private fun renderMoreButton() {
        moreButton.visibility =
            if (currentCandidates.size > inlineLimit) View.VISIBLE else View.GONE
        if (!moreButton.isShown) setExpanded(false)
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
            params.setMargins(6, 6, 6, 6)
            params.height = headerRowHeightPx
            button.layoutParams = params
            candidateGrid.addView(button)
        }
    }

    fun setHeightScale(scale: Float) {
        val safeScale = scale.coerceAtLeast(0.5f)
        val newRowHeightPx = (baseRowHeightPx * safeScale).roundToInt().coerceAtLeast(1)
        if (newRowHeightPx == headerRowHeightPx) return

        headerRowHeightPx = newRowHeightPx
        val rowLayoutParams = candidateBarRow.layoutParams
            ?: LayoutParams(LayoutParams.MATCH_PARENT, headerRowHeightPx)
        rowLayoutParams.height = headerRowHeightPx
        candidateBarRow.layoutParams = rowLayoutParams

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

    private fun createCandidateButton(entry: CinEntry, index: Int): Button {
        val button = Button(context)
        button.text = entry.value
        button.isAllCaps = false
        button.minWidth = 0
        button.minHeight = 0
        button.setPadding(16, 0, 16, 0)
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
        lp.setMargins(6, 0, 6, 0)
        button.layoutParams = lp
        button.setOnClickListener { listener?.onCandidateClick(entry) }
        return button
    }
}
