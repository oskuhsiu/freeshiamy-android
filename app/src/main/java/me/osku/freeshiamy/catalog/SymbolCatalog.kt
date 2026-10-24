package me.osku.freeshiamy.catalog

import android.content.Context
import android.os.Build

internal enum class CatalogMode(val capacity: Int) {
    SYMBOLS(28),
    EMOJI(20),
    EMOTICONS(11),
}

internal data class CatalogItem(val text: String, val variants: List<String>)

internal data class CatalogCategory(
    val id: String,
    val label: String,
    val pageCount: Int,
    val itemCount: Int,
    val assetPath: String,
)

/** Small metadata index with synchronous, bounded reads of individual pages. */
internal class SymbolCatalog(context: Context) {
    private val assets = context.assets
    private val categoriesByMode: Map<CatalogMode, List<CatalogCategory>>

    init {
        val emojiApi = if (Build.VERSION.SDK_INT >= 25) 25 else 23
        val selected = mutableMapOf<CatalogMode, MutableList<CatalogCategory>>()
        for (line in readAsset("catalog/index.tsv").dropLast(1).split('\n')) {
            val columns = line.split('\t')
            check(columns.size == 7) { "Invalid catalog index row" }
            val mode = CatalogMode.valueOf(columns[0])
            val minimumApi = columns[1].toInt()
            if (minimumApi != if (mode == CatalogMode.EMOJI) emojiApi else 23) continue
            val category = CatalogCategory(
                id = columns[2],
                label = columns[3],
                pageCount = columns[4].toInt(),
                itemCount = columns[5].toInt(),
                assetPath = columns[6],
            )
            check(category.itemCount > 0 && category.pageCount ==
                (category.itemCount + mode.capacity - 1) / mode.capacity) {
                "Invalid catalog page count"
            }
            selected.getOrPut(mode) { mutableListOf() }.add(category)
        }
        categoriesByMode = selected.mapValues { (_, categories) -> categories.toList() }
        check(CatalogMode.values().all { !categoriesByMode[it].isNullOrEmpty() }) {
            "Missing catalog mode"
        }
    }

    fun categories(mode: CatalogMode): List<CatalogCategory> = categoriesByMode.getValue(mode)

    fun loadPage(category: CatalogCategory, page: Int): List<CatalogItem> {
        require(page in 0 until category.pageCount) { "Catalog page out of range: $page" }
        val mode = categoriesByMode.entries.firstOrNull { category in it.value }?.key
        requireNotNull(mode) { "Unknown catalog category" }
        val text = readAsset("${category.assetPath}/$page.tsv")
        val items = text.dropLast(1).split('\n').map { line ->
            val columns = line.split('\t')
            check(columns.all { it.isNotEmpty() }) { "Empty catalog text" }
            CatalogItem(columns.first(), columns.drop(1))
        }
        val expectedSize = minOf(mode.capacity, category.itemCount - page * mode.capacity)
        check(items.size == expectedSize) { "Invalid catalog page size" }
        return items
    }

    private fun readAsset(path: String): String {
        // One extra byte detects oversize assets without reading an entire file.
        val bytes = ByteArray(MAX_ASSET_BYTES + 1)
        val size = assets.open(path).use { input ->
            var count = 0
            while (count < bytes.size) {
                val read = input.read(bytes, count, bytes.size - count)
                if (read < 0) break
                count += read
            }
            count
        }
        check(size <= MAX_ASSET_BYTES) { "Catalog asset exceeds 16 KiB: $path" }
        check(size > 0 && bytes[size - 1] == '\n'.code.toByte()) { "Invalid catalog asset: $path" }
        return String(bytes, 0, size, Charsets.UTF_8)
    }

    private companion object {
        const val MAX_ASSET_BYTES = 16 * 1024
    }
}
