package me.osku.freeshiamy.engine

import java.io.InputStream

/** Character conversion for selected CIN values; candidates and codes stay traditional. */
class SimplifiedChineseConverter(input: InputStream) {
    private val characters: Map<Int, String> = input.bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.associate { line ->
            val (traditional, simplified) = line.split('\t', limit = 2)
            traditional.codePointAt(0) to simplified.substringBefore(' ')
        }
    }

    fun convert(text: String): String = buildString(text.length) {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            val replacement = characters[codePoint]
            if (replacement != null) append(replacement) else appendCodePoint(codePoint)
            offset += Character.charCount(codePoint)
        }
    }
}
