package com.zerotwodev.recall.data

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Memory rules kept apart from Room, so a future sync source applies exactly the same ones.

/** "MM-dd" in [timeZone], taken from the ISO date string ("2026-11-10" -> "11-10"). */
internal fun monthDayOf(instant: Instant, timeZone: TimeZone): String =
    instant.toLocalDateTime(timeZone).date.toString().substring(5)

/** A short title from the first words, used when the user gives none. */
internal fun titleFrom(body: String): String {
    val words = body.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "Untitled memory"
    val head = words.take(6).joinToString(" ")
    return if (words.size > 6) "$head…" else head
}
