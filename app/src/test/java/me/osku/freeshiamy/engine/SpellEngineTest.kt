package me.osku.freeshiamy.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SpellEngineTest {
    @Test
    fun exactToneAndAllToneQueriesPreserveSourceOrderWithoutPrefixBleed() {
        val engine = SpellEngine(listOf(
            CinEntry("su4", "逆", 5),
            CinEntry("su3", "你", 0),
            CinEntry("su6", "尼", 2),
            CinEntry("su3", "妳", 1),
            CinEntry("su3", "你", 3),
            CinEntry("su06", "娘", 4),
            CinEntry("s3", "擬", 6),
        ))

        assertEquals(listOf("你", "妳"), engine.valuesForPronunciation("su3", true))
        assertEquals(listOf("你", "妳", "尼", "逆"), engine.valuesForPronunciation("su", false))
        assertEquals(listOf("你", "妳", "尼", "逆"), engine.valuesForPronunciation("su3", false))
        assertEquals(listOf("擬"), engine.valuesForPronunciation("s", false))
        assertEquals(emptyList<String>(), engine.valuesForPronunciation("", false))
        assertEquals(emptyList<String>(), engine.valuesForPronunciation("su7", true))
    }

    @Test
    fun firstAndLightTonesRemainDistinctAndPolyphonicCharactersKeepAllReadings() {
        val engine = SpellEngine(listOf(
            CinEntry("2k", "的", 0),
            CinEntry("2k7", "的", 1),
            CinEntry("2u6", "的", 2),
            CinEntry("2u4", "的", 3),
        ))

        assertEquals(listOf("的"), engine.valuesForPronunciation("2k", true))
        assertEquals(listOf("的"), engine.valuesForPronunciation("2k7", true))
        assertEquals(listOf("的"), engine.valuesForPronunciation("2u6", true))
        assertEquals(listOf("的"), engine.valuesForPronunciation("2u4", true))
        assertEquals(listOf("的"), engine.valuesForPronunciation("2k", false))
        assertEquals("2k", engine.spellFor("的"))
        assertEquals(listOf("的"), engine.valuesForSpell("2k7"))
    }

    @Test
    fun annotationsCannotBecomeCandidatesAndSupplementaryCharactersAreSupported() {
        val character = String(Character.toChars(0x20000))
        val engine = SpellEngine(listOf(
            CinEntry("j03", "腕 # non official", 0),
            CinEntry("j03", "多字", 1),
            CinEntry("j03", character, 2),
            CinEntry("j03", "", 3),
        ))

        assertEquals(listOf("腕", character), engine.valuesForPronunciation("j03", true))
        assertEquals(listOf("腕", character), engine.valuesForPronunciation("j0", false))
    }
}
