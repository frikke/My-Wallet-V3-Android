package com.blockchain.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

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

/**
 * 01 -> 1
 * 0.10 -> 0.10
 */
fun String.removeLeadingZeros(): String {
    if (isBlank()) return this
    val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator
    val parts = split(decimalSeparator)
    val nonZeroLeading = parts[0].replaceFirst("^0+(?!$)".toRegex(), "")
    return nonZeroLeading.ifEmpty { "0" } + parts.getOrNull(1)?.let { "$decimalSeparator" + parts[1] }.orEmpty()
}

fun String.stripThousandSeparators(): String {
    val decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
    return this.replace(decimalFormatSymbols.groupingSeparator.toString(), "")
}

fun String.replaceGroupingSeparatorWithDecimal(): String {
    val decimalSeparator = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator
    val groupingSeparator = DecimalFormatSymbols(Locale.getDefault()).groupingSeparator
    // Check if the string contains none decimal separator and exactly one grouping
    if (this.contains(decimalSeparator)) return this
    if (this.count { it == groupingSeparator } == 1) {
        // Replace thousands separator with decimal separator
        return this.replace(groupingSeparator, decimalSeparator)
    }
    if (this.count { !it.isDigit() } == 1) {
        return this.replaceFirst(Regex("\\D"), decimalSeparator.toString())
    }
    return this // Return the original string if it contains any of the separators
}

fun String.toBigDecimalFromLocalisedInput(): BigDecimal {
    toBigDecimalOrNull()?.let {
        return it
    }
    val df = NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat
    df.isParseBigDecimal = true
    return try {
        df.parseObject(this) as BigDecimal
    } catch (e: ParseException) {
        toBigDecimal()
    }
}

fun String.toBigDecimalOrNullFromLocalisedInput(): BigDecimal? {
    toBigDecimalOrNull()?.let {
        return it
    }
    val df = NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat
    df.isParseBigDecimal = true
    return try {
        df.parseObject(this) as BigDecimal
    } catch (e: ParseException) {
        toBigDecimalOrNull()
    }
}
