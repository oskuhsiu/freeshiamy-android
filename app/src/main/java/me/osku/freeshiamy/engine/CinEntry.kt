package me.osku.freeshiamy.engine

data class CinEntry(
    val code: String,
    val value: String,
    val sourceOrder: Int,
) {
    val codeLength: Int = code.length
}

