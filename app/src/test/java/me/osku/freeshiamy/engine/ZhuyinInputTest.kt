package me.osku.freeshiamy.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhuyinInputTest {
    @Test
    fun composesCanonicalSyllableAndReplacesComponents() {
        val input = ZhuyinInput()
        "0us3".forEach { assertTrue(input.input(it)) }

        assertEquals("su03", input.code)
        assertEquals("ㄋㄧㄢˇ", input.display)
        assertTrue(input.hasTone)

        "wjl6".forEach { assertTrue(input.input(it)) }
        assertEquals("wjl6", input.code)
        assertEquals("ㄊㄨㄠˊ", input.display)
        listOf("wjl", "wj", "w", "").forEach { expected ->
            input.backspace()
            assertEquals(expected, input.code)
        }
    }

    @Test
    fun firstToneIsExplicitButDoesNotAppendToLookupCode() {
        val input = ZhuyinInput()
        assertFalse(input.input(' '))
        assertTrue(input.input('5'))
        assertFalse(input.hasTone)
        assertTrue(input.input(' '))

        assertEquals("5", input.code)
        assertEquals("ㄓˉ", input.display)
        assertTrue(input.hasTone)

        input.backspace()
        assertEquals("5", input.code)
        assertFalse(input.hasTone)
        assertEquals("ㄓ", input.display)
    }

    @Test
    fun lightToneDisplaysBeforeSyllableAndBackspaceNeverLeavesTheQuery() {
        val input = ZhuyinInput()
        "2k7".forEach { input.input(it) }

        assertEquals("2k7", input.code)
        assertEquals("˙ㄉㄜ", input.display)
        input.backspace()
        assertEquals("2k", input.code)
        input.backspace()
        assertEquals("2", input.code)
        input.backspace()
        input.backspace()
        assertEquals("", input.code)
        assertEquals("", input.display)
        assertFalse(input.hasTone)
    }

    @Test
    fun mapsAllZhuyinSymbolsAndAcceptsPhysicalKeyCase() {
        assertEquals(42, ZhuyinInput.symbols.size)
        assertEquals('s', ZhuyinInput.keyForSymbol('ㄋ'))
        assertEquals(' ', ZhuyinInput.keyForSymbol('ˉ'))
        val input = ZhuyinInput()
        assertTrue(input.input('S'))
        assertTrue(input.input('ㄧ'))
        assertTrue(input.input('ˇ'))
        assertFalse(input.input('!'))
        assertEquals("su3", input.code)
        assertEquals("ㄋㄧˇ", input.display)

        input.clear()
        assertEquals("", input.code)
        assertEquals("", input.display)
        assertFalse(input.hasTone)
    }
}
