package me.osku.freeshiamy.engine

/** One lookup syllable, independent of the text editor and the normal Shiamy buffer. */
class ZhuyinInput {
    private var initial: Char? = null
    private var medial: Char? = null
    private var final: Char? = null
    private var tone: Char? = null

    val code: String
        get() = buildString {
            initial?.let { append(it) }
            medial?.let { append(it) }
            final?.let { append(it) }
            tone?.takeUnless { it == ' ' }?.let { append(it) }
        }

    val hasTone: Boolean
        get() = tone != null

    val display: String
        get() = buildString {
            if (tone == '7') append(symbols.getValue('7'))
            initial?.let { append(symbols.getValue(it)) }
            medial?.let { append(symbols.getValue(it)) }
            final?.let { append(symbols.getValue(it)) }
            tone?.takeUnless { it == '7' }?.let { append(symbols.getValue(it)) }
        }

    /** The latest key replaces its component instead of appending a second syllable. */
    fun input(key: Char): Boolean {
        val normalized = key.lowercaseChar().takeIf { it in symbols } ?: keyForSymbol(key) ?: return false
        when (normalized) {
            in INITIAL_KEYS -> initial = normalized
            in MEDIAL_KEYS -> medial = normalized
            in FINAL_KEYS -> final = normalized
            in TONE_KEYS -> {
                if (initial == null && medial == null && final == null) return false
                tone = normalized
            }
            else -> return false
        }
        return true
    }

    fun backspace() {
        when {
            tone != null -> tone = null
            final != null -> final = null
            medial != null -> medial = null
            else -> initial = null
        }
    }

    fun clear() {
        initial = null
        medial = null
        final = null
        tone = null
    }

    companion object {
        private const val INITIAL_KEYS = "1qaz2wsxedcrfv5tgbyhn"
        private const val MEDIAL_KEYS = "ujm"
        private const val FINAL_KEYS = "8ik,9ol.0p;/-"
        private const val TONE_KEYS = " 6347"

        val symbols: Map<Char, String> = linkedMapOf(
            '1' to "ㄅ", 'q' to "ㄆ", 'a' to "ㄇ", 'z' to "ㄈ",
            '2' to "ㄉ", 'w' to "ㄊ", 's' to "ㄋ", 'x' to "ㄌ",
            'e' to "ㄍ", 'd' to "ㄎ", 'c' to "ㄏ", 'r' to "ㄐ",
            'f' to "ㄑ", 'v' to "ㄒ", '5' to "ㄓ", 't' to "ㄔ",
            'g' to "ㄕ", 'b' to "ㄖ", 'y' to "ㄗ", 'h' to "ㄘ",
            'n' to "ㄙ", 'u' to "ㄧ", 'j' to "ㄨ", 'm' to "ㄩ",
            '8' to "ㄚ", 'i' to "ㄛ", 'k' to "ㄜ", ',' to "ㄝ",
            '9' to "ㄞ", 'o' to "ㄟ", 'l' to "ㄠ", '.' to "ㄡ",
            '0' to "ㄢ", 'p' to "ㄣ", ';' to "ㄤ", '/' to "ㄥ",
            '-' to "ㄦ", ' ' to "ˉ", '6' to "ˊ", '3' to "ˇ",
            '4' to "ˋ", '7' to "˙",
        )

        fun keyForSymbol(symbol: Char): Char? =
            symbols.entries.firstOrNull { it.value.single() == symbol }?.key
    }
}
