package me.osku.freeshiamy.engine

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupAssetsTest {
    @Test
    fun bundledTablesSupportPhoneticRescueAndCodeDisplay() {
        val shiamy = ShiamyEngine(File("src/main/assets/freeshiamy.cin").inputStream().use(CinParser::parse))
        val spells = SpellEngine(File("src/main/assets/cht_spells.cin").inputStream().use(CinParser::parse))

        assertTrue(spells.valuesForPronunciation("su3", true).containsAll(listOf("你", "妳")))
        assertEquals(listOf("pns"), shiamy.codesForValue("你"))
        assertEquals(listOf("gns"), shiamy.codesForValue("妳"))
        assertTrue(spells.valuesForPronunciation("ji3", true).contains("我"))
        assertEquals(listOf("ix"), shiamy.codesForValue("我"))

        val allTones = spells.valuesForPronunciation("a8", false)
        assertTrue(allTones.containsAll(listOf("媽", "麻", "馬", "罵", "嗎")))
        assertTrue(spells.valuesForPronunciation("a8", true).contains("媽"))
        assertTrue(spells.valuesForPronunciation("a87", true).contains("嗎"))
        assertTrue(spells.valuesForPronunciation("j03", true).all { it.codePointCount(0, it.length) == 1 })
    }
}
