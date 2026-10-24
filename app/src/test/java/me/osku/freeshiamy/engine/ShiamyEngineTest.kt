package me.osku.freeshiamy.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiamyEngineTest {
    @Test
    fun reverseLookupDeduplicatesAndSortsAllCodes() {
        val engine = ShiamyEngine(listOf(
            CinEntry("zzz", "你", 0),
            CinEntry("az", "你", 1),
            CinEntry("a", "你", 2),
            CinEntry("aa", "你", 3),
            CinEntry("a", "你", 4),
            CinEntry("ab", "妳", 5),
        ))

        assertEquals(listOf("a", "aa", "az", "zzz"), engine.codesForValue("你"))
        assertEquals("a", engine.shortestCodeForValue("你"))
        assertEquals(listOf("ab"), engine.codesForValue("妳"))
        assertEquals(emptyList<String>(), engine.codesForValue("未收錄"))
        assertNull(engine.shortestCodeForValue("未收錄"))
    }

    @Test
    fun reverseLookupTreatsSupplementaryCharacterAsWholeValue() {
        val character = String(Character.toChars(0x20000))
        val entries = listOf(CinEntry("abc", character, 0), CinEntry("a", "一", 1))
        val engine = ShiamyEngine(entries)

        assertEquals(listOf("abc"), engine.codesForValue(character))
        assertEquals(entries, engine.queryPrefix("a"))
        assertEquals(emptyList<CinEntry>(), engine.queryPrefix("a "))
    }
}
