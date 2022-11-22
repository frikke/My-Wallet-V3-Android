package com.blockchain.utils

fun String.capitalizeFirstChar(): String =
    this.lowercase().replaceFirstChar { it.uppercase() }

fun String.withBearerPrefix() = "Bearer $this"

fun StringBuilder.appendSpaced(string: Any) = apply {
    append(" $string")
}

fun String.abbreviate(maxLength: Int, indicator: String = "..."): String {
    return if (length > maxLength) {
        take(maxLength) + indicator
    } else {
        this
    }
}

fun String.abbreviate(startLength: Int, endLength: Int, indicator: String = "..."): String {
    return if (length > startLength + endLength) {
        take(startLength) + indicator + takeLast(endLength)
    } else {
        this
    }
}
