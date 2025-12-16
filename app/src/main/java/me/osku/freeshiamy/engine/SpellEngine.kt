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

    init {
        // Preserve original file order via sourceOrder (CinParser assigns it while reading).
        val ordered = entries.sortedBy { it.sourceOrder }

        val spellMap = LinkedHashMap<String, MutableList<String>>(8_000)
        val valueMap = HashMap<String, String>(32_000)

        for (entry in ordered) {
            val code = entry.code
            val value = entry.value

            spellMap.getOrPut(code) { ArrayList() }.add(value)
            // Prefer the first pronunciation encountered for heteronyms.
            valueMap.putIfAbsent(value, code)
        }

        spellToValues = spellMap.mapValues { it.value.toList() }
        valueToSpell = valueMap
    }

    fun spellFor(value: String): String? = valueToSpell[value]

    fun valuesForSpell(spell: String): List<String> = spellToValues[spell].orEmpty()
}

