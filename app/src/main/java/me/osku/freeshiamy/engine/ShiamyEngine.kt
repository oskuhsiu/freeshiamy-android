package me.osku.freeshiamy.engine

class ShiamyEngine(
    private val entries: List<CinEntry>,
) {
    private val codesByValue: Map<String, List<String>> = buildCodesByValue(entries)

    fun queryPrefix(prefix: String): List<CinEntry> {
        if (prefix.isEmpty()) return emptyList()
        if (prefix.any { it.isWhitespace() }) return emptyList()
        return entries.filter { it.code.startsWith(prefix) }
    }

    fun shortestCodeForValue(value: String): String? = codesByValue[value]?.firstOrNull()

    fun codesForValue(value: String): List<String> = codesByValue[value].orEmpty()

    private fun buildCodesByValue(entries: List<CinEntry>): Map<String, List<String>> {
        val map = HashMap<String, MutableSet<String>>(entries.size)
        for (entry in entries) {
            map.getOrPut(entry.value) { HashSet() }.add(entry.code)
        }
        val order = compareBy<String> { it.length }.thenBy { it }
        return map.mapValues { (_, codes) -> codes.sortedWith(order) }
    }
}
