package me.osku.freeshiamy.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import me.osku.freeshiamy.R
import me.osku.freeshiamy.engine.CinEntry

class CandidateBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onRawClick()
        fun onCandidateClick(entry: CinEntry)
    }

    private val rawTextView: TextView
    private val candidateContainer: LinearLayout
    private val moreButton: Button
    private val moreScroll: ScrollView
    private val candidateGrid: GridLayout

    private var expanded = false
    private var currentCandidates: List<CinEntry> = emptyList()
    private var currentExactCount: Int = 0
    private var inlineLimit: Int = 10
    private var maxMoreItems: Int = 200

    var listener: Listener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.candidate_bar, this, true)

        rawTextView = findViewById(R.id.raw_text)
        candidateContainer = findViewById(R.id.candidate_container)
        moreButton = findViewById(R.id.more_button)
        moreScroll = findViewById(R.id.more_scroll)
        candidateGrid = findViewById(R.id.candidate_grid)

        rawTextView.setOnClickListener { listener?.onRawClick() }
        moreButton.setOnClickListener { setExpanded(!expanded) }
    }

    fun setLimits(inlineLimit: Int, maxMoreItems: Int) {
        this.inlineLimit = inlineLimit.coerceAtLeast(1)
        this.maxMoreItems = maxMoreItems.coerceAtLeast(this.inlineLimit)
    }

    fun setState(rawText: String, prefixCandidates: List<CinEntry>, exactCount: Int) {
        rawTextView.text = rawText
        currentCandidates = prefixCandidates
        currentExactCount = exactCount.coerceIn(0, prefixCandidates.size)

        renderInlineCandidates()
        renderMoreButton()

        if (expanded && currentCandidates.isEmpty()) {
            setExpanded(false)
        } else if (expanded) {
            renderExpandedCandidates()
        }
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
            button.layoutParams = params
            candidateGrid.addView(button)
        }
    }

    private fun createCandidateButton(entry: CinEntry, index: Int): Button {
        val button = Button(context)
        button.text = entry.value
        button.minWidth = 0
        button.setPadding(16, 8, 16, 8)

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

        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.setMargins(6, 0, 6, 0)
        button.layoutParams = lp
        button.setOnClickListener { listener?.onCandidateClick(entry) }
        return button
    }
}

