package me.osku.freeshiamy.engine

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class SimplifiedChineseConverterTest {
    @Test
    fun bundledDictionaryConvertsSelectedCharactersAndPreservesOtherCodePoints() {
        val converter = File("src/main/assets/opencc/TSCharacters.txt").inputStream().use {
            SimplifiedChineseConverter(it)
        }

        assertEquals("台湾国学体发发后", converter.convert("臺灣國學體發髮後"))
        assertEquals("𠀾", converter.convert("𠁞"))
        assertEquals("你我ABC123，。🙂𠀀", converter.convert("你我ABC123，。🙂𠀀"))
        assertEquals("", converter.convert(""))
    }
}
