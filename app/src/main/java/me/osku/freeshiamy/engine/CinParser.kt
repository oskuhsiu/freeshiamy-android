package me.osku.freeshiamy.engine

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CinParser {
    fun parse(inputStream: InputStream): List<CinEntry> {
        val entries = ArrayList<CinEntry>(32_000)
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String?
            var sourceOrder = 0
            while (true) {
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#")) continue

                val splitIndex = trimmed.indexOfAny(charArrayOf(' ', '\t'))
                if (splitIndex <= 0) continue

                val code = trimmed.substring(0, splitIndex)
                val value = trimmed.substring(splitIndex).trimStart()
                if (value.isEmpty()) continue

                entries.add(
                    CinEntry(
                        code = code,
                        value = value,
                        sourceOrder = sourceOrder++,
                    )
                )
            }
        }

        // iOS 版行為：先依 code 長度，再依 code 字典序排序（同 key 保持穩定）
        entries.sortWith(
            compareBy<CinEntry> { it.codeLength }
                .thenBy { it.code }
                .thenBy { it.sourceOrder }
        )
        return entries
    }
}

