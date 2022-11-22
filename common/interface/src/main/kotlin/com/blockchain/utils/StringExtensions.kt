package com.blockchain.utils

fun String.capitalizeFirstChar(): String =
    this.lowercase().replaceFirstChar { it.uppercase() }

fun String.withBearerPrefix() = "Bearer $this"

fun StringBuilder.appendSpaced(string: Any) = apply {
    append(" $string")
}

fun String.abbreviate(maxLength: Int, indicator: String = "..."): String {
    return take(maxLength).let {
        if (length > it.length) it + indicator
        else it
    }
}
