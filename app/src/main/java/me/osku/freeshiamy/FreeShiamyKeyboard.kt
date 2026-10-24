package me.osku.freeshiamy

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.inputmethodservice.Keyboard
import android.view.inputmethod.EditorInfo
import kotlin.math.roundToInt


/**
 * Created by Osku on 2021/4/9.
 */
class FreeShiamyKeyboard : Keyboard {

    private data class KeyRow(val keys: List<Key>, val gapAfter: Int)

    private var baseRows: List<KeyRow>? = null
    private var layoutHeight: Int? = null
    private val keyIndices by lazy { IntArray(keys.size) { it } }

    /** Rebuild from the original rows so adjusting height never compounds a previous scale. */
    fun updateKeyHeights(scale: Float, density: Float, maxHeight: Int): Boolean {
        val rows = baseRows ?: keys.groupBy { it.y }.toSortedMap().values.toList().let { groups ->
            groups.mapIndexed { index, row ->
                val height = row.maxOf { it.height }
                val nextTop = groups.getOrNull(index + 1)?.first()?.y
                KeyRow(row, nextTop?.minus(row.first().y + height)?.coerceAtLeast(0) ?: 0)
            }
        }.also { baseRows = it }
        if (rows.isEmpty()) return false

        val heights = rows.map { row ->
            val isNumberRow = row.keys.all {
                it.codes.firstOrNull()?.let { code -> code in '0'.code..'9'.code } == true
            }
            val isBottomRow = row.keys.any { it.codes.firstOrNull() == ' '.code }
            val heightDp = when {
                isBottomRow -> 56f
                isNumberRow -> 48f
                else -> 52f
            }
            heightDp * density * scale
        }
        val desiredHeight = heights.sum() + rows.sumOf { it.gapAfter } * scale
        val fit = minOf(1f, maxHeight.coerceAtLeast(rows.size) / desiredHeight)
        var top = 0f
        var changed = false
        for ((index, row) in rows.withIndex()) {
            val y = top.roundToInt()
            top += heights[index] * fit
            val height = (top.roundToInt() - y).coerceAtLeast(1)
            for (key in row.keys) {
                changed = changed || key.y != y || key.height != height
                key.y = y
                key.height = height
            }
            top += row.gapAfter * scale * fit
        }
        val height = top.roundToInt()
        changed = changed || layoutHeight != height
        layoutHeight = height
        return changed
    }

    override fun getHeight(): Int = layoutHeight ?: super.getHeight()

    // Framework's private neighbor grid cannot be invalidated after changing key heights.
    // The small keyboard (about 50 keys) can use its existing precise hit/proximity checks directly.
    override fun getNearestKeys(x: Int, y: Int): IntArray =
        if (x in 0 until minWidth && y in 0 until height) keyIndices else intArrayOf()

    private var mEnterKey: Key? = null

    /**
     * Stores the current state of the mode change key. Its width will be dynamically updated to
     * match the region of [.mModeChangeKey] when [.mModeChangeKey] becomes invisible.
     */
    private var mModeChangeKey: Key? = null

    /**
     * Stores the current state of the language switch key (a.k.a. globe key). This should be
     * visible while [InputMethodManager.shouldOfferSwitchingToNextInputMethod]
     * returns true. When this key becomes invisible, its width will be shrunk to zero.
     */
    private var mLanguageSwitchKey: Key? = null

    /**
     * Stores the size and other information of [.mModeChangeKey] when
     * [.mLanguageSwitchKey] is visible. This should be immutable and will be used only as a
     * reference size when the visibility of [.mLanguageSwitchKey] is changed.
     */
    private var mSavedModeChangeKey: Key? = null

    /**
     * Stores the size and other information of [.mLanguageSwitchKey] when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * [.mLanguageSwitchKey] is changed.
     */
    private var mSavedLanguageSwitchKey: Key? = null

    constructor(context: Context, xmlLayoutResId: Int) : super(context, xmlLayoutResId)
    constructor(
        context: Context, layoutTemplateResId: Int,
        characters: CharSequence?, columns: Int, horizontalPadding: Int
    ) : super(context, layoutTemplateResId, characters, columns, horizontalPadding)

    override fun createKeyFromXml(
        res: Resources?, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ): Key? {
        val key: Key = LatinKey(res, parent, x, y, parser)
        if (key.codes[0] == 10) {
            mEnterKey = key
        } else if (key.codes[0] == KEYCODE_MODE_CHANGE) {
            mModeChangeKey = key
            mSavedModeChangeKey = LatinKey(res, parent, x, y, parser)
        } else if (key.codes[0] == FreeShiamyKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            mLanguageSwitchKey = key
            mSavedLanguageSwitchKey = LatinKey(res, parent, x, y, parser)
        }
        return key
    }

    /**
     * Dynamically change the visibility of the language switch key (a.k.a. globe key).
     * @param visible True if the language switch key should be visible.
     */
    fun setLanguageSwitchKeyVisibility(visible: Boolean) {
        val modeKey = mModeChangeKey ?: return
        val languageKey = mLanguageSwitchKey ?: return
        val savedMode = mSavedModeChangeKey ?: return
        val savedLanguage = mSavedLanguageSwitchKey ?: return

        if (visible) {
            // The language switch key should be visible. Restore the size of the mode change key
            // and language switch key using the saved layout.
            modeKey.width = savedMode.width
            modeKey.x = savedMode.x
            languageKey.width = savedLanguage.width
            languageKey.icon = savedLanguage.icon
            languageKey.iconPreview = savedLanguage.iconPreview
        } else {
            // The language switch key should be hidden. Change the width of the mode change key
            // to fill the space of the language key so that the user will not see any strange gap.
            modeKey.width = savedMode.width + savedLanguage.width
            modeKey.x = if (savedMode.x < savedLanguage.x) savedMode.x else savedLanguage.x
            languageKey.width = 0
            languageKey.icon = null
            languageKey.iconPreview = null
        }
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    fun setImeOptions(res: Resources, options: Int) {
        if (mEnterKey == null) {
            return
        }
        when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_GO -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_go_key)
            }
            EditorInfo.IME_ACTION_NEXT -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_next_key)
            }
            EditorInfo.IME_ACTION_SEARCH -> {
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_keyboard_search)
                mEnterKey!!.label = null
            }
            EditorInfo.IME_ACTION_SEND -> {
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_send_key)
            }
            else -> {
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_keyboard_enter)
                mEnterKey!!.label = null
            }
        }
    }

    internal class LatinKey(
        res: Resources?, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ) :
        Key(res, parent, x, y, parser) {
        override fun isInside(x: Int, y: Int): Boolean {
            if (width <= 0) return false
            return super.isInside(x, if (codes[0] == KEYCODE_CANCEL) y - 10 else y)
        }
    }
}
