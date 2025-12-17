package me.osku.freeshiamy.engine

class ShiamyEngine(
    private val entries: List<CinEntry>,
) {
    private val shortestCodeByValue: Map<String, String> = buildShortestCodeByValue(entries)

    fun queryPrefix(prefix: String): List<CinEntry> {
        if (prefix.isEmpty()) return emptyList()
        if (prefix.any { it.isWhitespace() }) return emptyList()
        return entries.filter { it.code.startsWith(prefix) }
    }

    fun shortestCodeForValue(value: String): String? = shortestCodeByValue[value]

    private fun buildShortestCodeByValue(entries: List<CinEntry>): Map<String, String> {
        val map = HashMap<String, String>(entries.size)
        for (entry in entries) {
            if (map.containsKey(entry.value)) continue
            map[entry.value] = entry.code
        }
        return map
    }
}
