package com.blockchain.utils

import java.math.BigInteger
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import org.apache.commons.lang3.time.DateUtils

/**
 * Converts a [String] from an ISO 8601 date to a [Date] object. The receiving [String] can specify
 * both seconds AND seconds + milliseconds. This is necessary because the Coinify API seems to
 * return one buy specify the other in their documentation, and we can't be sure that we won't see
 * the documented format at some point. If the [String] is for some reason not parsable due to
 * otherwise incorrect formatting, the resulting [Date] will be null.
 *
 * The returned times will always be in UTC.
 *
 * @return A [Date] object or null if the [String] isn't formatted correctly.
 */
fun String.fromIso8601ToUtc(): Date? {
    val millisFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val secondsFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    return try {
        DateUtils.parseDate(this, millisFormat, secondsFormat)
    } catch (e: ParseException) {
        e.printStackTrace()
        null
    }
}

fun Date.toUtcIso8601(locale: Locale = Locale.getDefault()): String {
    val s = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", locale)
    s.timeZone = TimeZone.getTimeZone("UTC")
    return s.format(this)
}

fun Date.toLocalTime(): Date {
    val calendar = Calendar.getInstance()
    val timeZone = calendar.timeZone
    val offset = timeZone.getOffset(this.time)

    return Date(this.time + offset)
}

fun ZonedDateTime.to12HourFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("ha")
    return formatter.format(this).toString()
}

fun ZonedDateTime.isLastDayOfTheMonth(): Boolean {
    val nextDay = this.plusDays(1)
    return this.month != nextDay.month
}

/**
 * Takes a [Date] object and converts it to ZonedDateTime, ie Fri, July 23.
 *
 * @return A formatted [String] object.
 */
fun Date.toFormattedDateWithoutYear(): String {
    val zonedDateTime = ZonedDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
    return "${
    zonedDateTime.dayOfWeek.getDisplayName(
        TextStyle.SHORT,
        Locale.getDefault()
    ).toString().capitalizeFirstChar()
    }, " +
        "${zonedDateTime.month.toString().capitalizeFirstChar()} " +
        "${zonedDateTime.dayOfMonth}"
}

/**
 * Takes a [Date] object and converts it to our standard date format, ie March 09, 2018 @11:47.
 *
 * @param locale The current [Locale].
 * @return A formatted [String] object.
 */
fun Date.toFormattedString(locale: Locale = Locale.getDefault()): String {
    val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    val timeFormat = SimpleDateFormat("hh:mm a", locale)
    val dateText = dateFormat.format(this)
    val timeText = timeFormat.format(this)

    return "$timeText on $dateText"
}

/**
 * Takes a [Date] object and converts it to the standard MEDIUM date format, ie 21 Jun 2020.
 *
 * @return A formatted [String] object.
 */
fun Date.toFormattedDate(): String {
    val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    return dateFormat.format(this)
}

/**
 * Takes a [Date] object and converts it to our standard date and time format, ie June 21, 01:23 pm.
 *
 * @param locale The current [Locale].
 * @return A formatted [String] object.
 */
fun Date.toFormattedDateTime(locale: Locale = Locale.getDefault()): String {
    val dateTimeFormat = SimpleDateFormat("MMMM dd, hh:mm a", locale)
    return dateTimeFormat.format(this)
}

fun BigInteger.secondsToDays(): Long =
    ceil(this.toDouble() / SECONDS_OF_DAY).toLong()

fun Int.secondsToDays(): Int =
    ceil(this.toDouble() / SECONDS_OF_DAY).toInt()

fun String.toZonedDateTime(): ZonedDateTime =
    ZonedDateTime.ofInstant(
        Instant.parse(this),
        ZoneId.systemDefault()
    )

private const val SECONDS_OF_DAY: Long = 86400

/**
 * Takes a [Date] object and returns a string with the full month name, ie 'June'
 *
 * @param locale The current [Locale].
 * @return The month's name.
 */
fun Date.getMonthName(locale: Locale = Locale.getDefault()): String {
    val dateTimeFormat = SimpleDateFormat("MMMM", locale)
    return dateTimeFormat.format(this)
}

fun Calendar.getMonthName(locale: Locale = Locale.getDefault()): String {
    return time.getMonthName(locale)
}

/**
 * Takes an expiration date in "MMyy" format and formats it to UI-ready "MM/yy" format
 *
 * @param locale The current [Locale].
 * @return the formatted expiration date.
 */
fun String.toFormattedExpirationDate(locale: Locale = Locale.getDefault()): String {
    val expDateOriginalFormat = SimpleDateFormat("MMyy", locale)
    val expDateFinalFormat = SimpleDateFormat("MM/yy", locale)

    val expDateOriginal = expDateOriginalFormat.parse(this)

    return expDateFinalFormat.format(expDateOriginal)
}

/**
 * Takes a date string in "MM/yy" format and formats it to UI-ready "Jun, 2022" format
 *
 * @param locale The current [Locale].
 * @return the formatted date string.
 */
fun String.toShortMonthYearDate(locale: Locale = Locale.getDefault()): String {
    val originalFormat = SimpleDateFormat("MM/yyyy", locale)
    val finalFormat = SimpleDateFormat("MMM, yyyy", locale)

    val originalDate = originalFormat.parse(this)

    return finalFormat.format(originalDate)
}

fun Date.toDayAndMonth(locale: Locale = Locale.getDefault()): String {
    val defaultDateTimeFormat = SimpleDateFormat("dd MMMM", locale)
    val usDateTimeFormat = SimpleDateFormat("MMMM dd", locale)
    return if (locale == Locale.US) usDateTimeFormat.format(this) else defaultDateTimeFormat.format(this)
}

fun Calendar.toMonthAndYear(locale: Locale = Locale.getDefault()): String {
    val defaultDateTimeFormat = SimpleDateFormat("MMMM yyyy", locale)
    return defaultDateTimeFormat.format(time)
}
