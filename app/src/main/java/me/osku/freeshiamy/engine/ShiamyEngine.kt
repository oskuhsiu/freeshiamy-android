package me.osku.freeshiamy.engine

class ShiamyEngine(
    private val entries: List<CinEntry>,
) {
    fun queryPrefix(prefix: String): List<CinEntry> {
        if (prefix.isEmpty()) return emptyList()
        if (prefix.any { it.isWhitespace() }) return emptyList()
        return entries.filter { it.code.startsWith(prefix) }
    }
}

