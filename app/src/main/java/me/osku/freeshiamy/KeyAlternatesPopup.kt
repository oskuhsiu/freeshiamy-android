package me.osku.freeshiamy

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.Keyboard
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.widget.GridLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.roundToInt

/** Google Zhuyin 2.4.5 English reference; see plan/gboard-symbols-english-lock-plan.md. */
internal object KeyAlternates {
    private val letterSymbols = mapOf(
        'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5",
        'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0",
        'a' to "@", 's' to "*", 'd' to "+", 'f' to "-", 'g' to "=",
        'h' to "/", 'j' to "#", 'k' to "(", 'l' to ")",
        'z' to "、'", 'x' to ":", 'c' to "\"", 'v' to "?", 'b' to "!",
        'n' to "~", 'm' to "…",
    ).mapKeys { it.key.code }.mapValues { (code, symbols) ->
        listOf(code.toChar().uppercaseChar().toString()) + symbols.map { it.toString() }
    }

    private val extras = mapOf(
        'a' to "ãåæāáǎàâä", 'e' to "ěēéèêë", 'i' to "ìǐīíïî",
        'o' to "œøõòǒóōöô", 'u' to "ǔūúùûü", 's' to "ß", 'c' to "ç",
        'v' to "ǚǖǘǜ¿ü", 'b' to "¡", 'n' to "ñ",
    )
    private val periodChoices = listOf(
        ";", ":", "…", "?", "!", "&", "^", "$", "_", "%",
        "^_^", ":)", ":(", ":P", ":D",
    )
    private data class SymbolAlternates(val choices: String, val default: Char)
    private val symbolChoices = mapOf(
        '1' to SymbolAlternates("¹½⅓¼⅛", '¹'),
        '2' to SymbolAlternates("²⅔", '²'),
        '3' to SymbolAlternates("³¾⅜", '³'),
        '4' to SymbolAlternates("⁴", '⁴'),
        '5' to SymbolAlternates("⅝", '⅝'),
        '7' to SymbolAlternates("⅞", '⅞'),
        '0' to SymbolAlternates("ⁿ∅", '∅'),
        '!' to SymbolAlternates("¡", '¡'),
        '$' to SymbolAlternates("¥£¢€₱", '¢'),
        '%' to SymbolAlternates("‰", '‰'),
        '^' to SymbolAlternates("↑↓←→", '↑'),
        '*' to SymbolAlternates("★†‡", '★'),
        '(' to SymbolAlternates("<{[", '<'),
        ')' to SymbolAlternates(">}]", '>'),
        '\'' to SymbolAlternates("‚‘’‹›", '‚'),
        '"' to SymbolAlternates("”„“«»", '„'),
        '=' to SymbolAlternates("≠≈∞", '≠'),
        '?' to SymbolAlternates("¿", '¿'),
        '+' to SymbolAlternates("±", '±'),
        '-' to SymbolAlternates("—_–·", '—'),
        '<' to SymbolAlternates("≤‹«", '≤'),
        '>' to SymbolAlternates("≥›»", '≥'),
        '.' to SymbolAlternates("…", '…'),
    ).mapKeys { it.key.code }

    fun hintForCode(code: Int): String? = letterSymbols[code]?.get(1)

    fun forCode(code: Int, shifted: Boolean = false, alphabet: Boolean = true): List<String>? {
        if (code == '.'.code && alphabet) return periodChoices
        symbolChoices[code]?.let { return it.choices.map(Char::toString) }
        val base = letterSymbols[code] ?: return null
        val extra = extras[code.toChar()].orEmpty().map {
            (if (shifted) it.uppercaseChar() else it).toString()
        }
        return base + extra
    }

    fun defaultSelection(code: Int, alphabet: Boolean): Int = when {
        code == '.'.code && alphabet -> 3
        code in symbolChoices -> symbolChoices.getValue(code).let { it.choices.indexOf(it.default) }
        else -> 1
    }
}

/** A passive window: the original keyboard pointer owns sliding, release and cancellation. */
internal class KeyAlternatesPopup(private val anchor: View) {
    private val density = anchor.resources.displayMetrics.density
    private val slop = maxOf(ViewConfiguration.get(anchor.context).scaledTouchSlop, dp(8))
    private var window: PopupWindow? = null
    private var choices = emptyList<String>()
    private var cells = emptyList<TextView>()
    private var selected = -1
    private var defaultIndex = 1
    private var left = 0
    private var top = 0
    private var cellWidth = 0
    private var columns = 0
    private var rows = 0
    private var cellHeight = 0
    private var popupHeight = 0
    private var padding = 0
    private var startX = 0f
    private var keyBottom = 0
    private val anchorLocation = IntArray(2)
    private val popupLocation = IntArray(2)

    val isShowing: Boolean get() = window?.isShowing == true

    fun show(key: Keyboard.Key, options: List<String>, touchX: Float, defaultSelection: Int): Boolean {
        return show(Rect(key.x, key.y, key.x + key.width, key.y + key.height),
            options, touchX, defaultSelection)
    }

    /** Coordinates are local to the anchor; catalog entries may contain complete Unicode sequences. */
    fun show(key: Rect, options: List<String>, touchX: Float, defaultSelection: Int,
             onAccessibleSelection: ((String) -> Unit)? = null): Boolean {
        if (anchor.windowToken == null || anchor.width <= dp(8) || options.isEmpty()) return false
        dismiss()
        padding = dp(4)
        val maxColumns = minOf(5, (anchor.width - padding * 2) / dp(48)).coerceAtLeast(1)
        rows = (options.size + maxColumns - 1) / maxColumns
        columns = (options.size + rows - 1) / rows
        cellWidth = minOf(dp(48), (anchor.width - padding * 2) / columns)
        cellHeight = dp(56)
        popupHeight = cellHeight * rows + padding * 2
        val popupWidth = cellWidth * columns + padding * 2
        val center = key.exactCenterX()
        left = (center - popupWidth / 2f).roundToInt()
            .coerceIn(0, anchor.width - popupWidth)
        // Keep the default symbol in the nearest bottom-row cell, even at screen edges.
        defaultIndex = ((center - left - padding) / cellWidth).toInt().coerceIn(0, columns - 1)
        left = (center - cellWidth * (defaultIndex + 0.5f) - padding).roundToInt()
            .coerceIn(0, anchor.width - popupWidth)
        choices = options.toMutableList().apply {
            val default = removeAt(defaultSelection.coerceIn(indices))
            add(defaultIndex, default)
        }
        top = key.top - popupHeight - dp(6)
        keyBottom = key.bottom
        startX = touchX

        val grid = GridLayout(anchor.context).apply {
            columnCount = columns
            rowCount = rows
            setPadding(padding, padding, padding, padding)
            background = background(Color.WHITE, outlined = true)
        }
        cells = choices.mapIndexed { index, choice ->
            TextView(anchor.context).apply {
                text = choice
                contentDescription = choice
                textSize = 26f
                setSingleLine()
                val textWidth = paint.measureText(choice)
                if (textWidth > cellWidth - dp(8)) {
                    textScaleX = ((cellWidth - dp(8)).coerceAtLeast(1) / textWidth).coerceAtMost(1f)
                }
                if (onAccessibleSelection != null) {
                    isFocusable = true
                    setOnClickListener {
                        dismiss()
                        onAccessibleSelection(choice)
                    }
                }
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTextColor(anchor.resources.getColor(R.color.candidate_normal))
                grid.addView(this, GridLayout.LayoutParams(
                    GridLayout.spec(rows - 1 - index / columns), GridLayout.spec(index % columns),
                ).apply { width = cellWidth; height = cellHeight })
            }
        }
        updateHighlight(defaultIndex)
        anchor.getLocationInWindow(anchorLocation)
        window = PopupWindow(grid, popupWidth, popupHeight, onAccessibleSelection != null).apply {
            isTouchable = onAccessibleSelection != null
            isOutsideTouchable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            elevation = dp(8).toFloat()
            showAtLocation(anchor, Gravity.NO_GRAVITY, anchorLocation[0] + left, anchorLocation[1] + top)
        }
        return true
    }

    fun move(x: Float, y: Float) {
        val content = window?.contentView ?: return
        if (!content.isLaidOut) return
        // WindowManager may move the popup to fit the screen, especially in landscape.
        anchor.getLocationOnScreen(anchorLocation)
        content.getLocationOnScreen(popupLocation)
        left = popupLocation[0] - anchorLocation[0]
        top = popupLocation[1] - anchorLocation[1]
        val index = when {
            y < top - dp(24) || y > maxOf(keyBottom, top + popupHeight) + dp(24) -> -1
            y <= top + popupHeight -> {
                val relativeX = x - left - padding
                val relativeY = y - top - padding
                if (relativeX < 0 || relativeX >= cellWidth * columns ||
                    relativeY < 0 || relativeY >= cellHeight * rows) -1
                else {
                    val column = (relativeX / cellWidth).toInt()
                    val row = (relativeY / cellHeight).toInt()
                    ((rows - 1 - row) * columns + column).takeIf { it in choices.indices } ?: -1
                }
            }
            else -> {
                // The finger may remain on the original key below the raised menu.
                val dx = x - startX
                if (kotlin.math.abs(dx) <= slop) defaultIndex
                else (defaultIndex + (dx / cellWidth).roundToInt()).takeIf { it in 0 until columns } ?: -1
            }
        }
        updateHighlight(index)
    }

    fun selection(): String? = choices.getOrNull(selected)

    fun dismiss() {
        window?.dismiss()
        window = null
        cells = emptyList()
        choices = emptyList()
        selected = -1
    }

    private fun updateHighlight(index: Int) {
        selected = index
        cells.forEachIndexed { i, cell ->
            cell.isSelected = i == index
            cell.background = background(if (i == index) 0xFFD7EAE7.toInt() else Color.TRANSPARENT)
        }
    }

    private fun background(color: Int, outlined: Boolean = false) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
        if (outlined) setStroke(dp(1), 0xFFA3C9C3.toInt())
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()
}
