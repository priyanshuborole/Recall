package com.zerotwodev.recall.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// Plain-Kotlin date text so every platform shows identical strings (no java.time, no NSDateFormatter).

private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val longMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

// Read from the ISO string ("2026-11-10") so this does not depend on accessor names that differ across versions.
private fun monthIndex(date: LocalDate): Int = date.toString().substring(5, 7).toInt() - 1
private fun dayOf(date: LocalDate): Int = date.toString().substring(8, 10).toInt()

/** "10 Nov 2026" */
fun Instant.toDisplayDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val date = toLocalDateTime(timeZone).date
    return "${dayOf(date)} ${shortMonths[monthIndex(date)]} ${date.year}"
}

/** "November 2026" */
fun Instant.toMonthHeader(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val date = toLocalDateTime(timeZone).date
    return "${longMonths[monthIndex(date)]} ${date.year}"
}

/** "Tuesday, 10 November" */
fun Instant.toLongDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val date = toLocalDateTime(timeZone).date
    return "${weekdays[date.dayOfWeek.ordinal]}, ${dayOf(date)} ${longMonths[monthIndex(date)]}"
}

/** "Good morning" / "Good afternoon" / "Good evening" */
fun greetingFor(now: Instant = Clock.System.now(), timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    when (now.toLocalDateTime(timeZone).hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

/** "2 years ago" relative to [now], for On this day. */
fun Instant.yearsAgoText(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val years = now.toLocalDateTime(timeZone).date.year - toLocalDateTime(timeZone).date.year
    return if (years == 1) "1 year ago" else "$years years ago"
}

/** "21:30" for a minute-of-day value. */
fun minuteOfDayText(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}
