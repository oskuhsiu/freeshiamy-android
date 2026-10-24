package me.osku.freeshiamy.engine

/**
 * 注音/拼音（含聲調）同音字查詢引擎。
 *
 * 資料來源：`cht_spells.cin`
 * - code: 音碼（含聲調）
 * - value: 單字（或少量符號/注音符號）
 */
class SpellEngine(
    entries: List<CinEntry>,
) {
    private val spellToValues: Map<String, List<String>>
    private val valueToSpell: Map<String, String>
    private val pronunciationToValues: Map<String, List<String>>
    private val syllableToValues: Map<String, List<String>>

    init {
        // Preserve original file order via sourceOrder (CinParser assigns it while reading).
        val ordered = entries.sortedBy { it.sourceOrder }

        val spellMap = LinkedHashMap<String, MutableList<String>>(8_000)
        val valueMap = HashMap<String, String>(32_000)
        val pronunciationMap = LinkedHashMap<String, MutableSet<String>>()
        val syllableMap = LinkedHashMap<String, MutableSet<String>>()

        for (entry in ordered) {
            val code = entry.code
            val value = entry.value

            spellMap.getOrPut(code) { ArrayList() }.add(value)
            // Prefer the first pronunciation encountered for heteronyms.
            if (!valueMap.containsKey(value)) {
                valueMap[value] = code
            }

            // A source row may carry an inline annotation, e.g. "腕 # non official".
            // Lookup candidates must remain single characters, including supplementary Han.
            val lookupValue = value.substringBefore(" #").trimEnd()
            if (lookupValue.codePointCount(0, lookupValue.length) == 1) {
                pronunciationMap.getOrPut(code) { LinkedHashSet() }.add(lookupValue)
                syllableMap.getOrPut(withoutTone(code)) { LinkedHashSet() }.add(lookupValue)
            }
        }

        spellToValues = spellMap.mapValues { it.value.toList() }
        valueToSpell = valueMap
        pronunciationToValues = pronunciationMap.mapValues { it.value.toList() }
        syllableToValues = syllableMap.mapValues { it.value.toList() }
    }

    fun spellFor(value: String): String? = valueToSpell[value]

    fun valuesForSpell(spell: String): List<String> = spellToValues[spell].orEmpty()

    /** Match a complete syllable; omitted tones include every tone of that syllable. */
    fun valuesForPronunciation(code: String, matchTone: Boolean): List<String> {
        if (code.isEmpty()) return emptyList()
        return if (matchTone) pronunciationToValues[code].orEmpty()
        else syllableToValues[withoutTone(code)].orEmpty()
    }

    private fun withoutTone(code: String): String = when (code.lastOrNull()) {
        '6', '3', '4', '7' -> code.dropLast(1)
        else -> code
    }
}
