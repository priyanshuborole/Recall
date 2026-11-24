package com.zerotwodev.recall

import com.zerotwodev.recall.util.toFullDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.LocalDateTime
import com.zerotwodev.recall.util.minuteOfDayText
import com.zerotwodev.recall.util.toDisplayDate
import com.zerotwodev.recall.util.toLongDay
import com.zerotwodev.recall.util.toMonthHeader
import com.zerotwodev.recall.util.yearsAgoText
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateTextTest {
    private val utc = TimeZone.UTC
    private val instant = Instant.parse("2026-11-10T13:30:00Z")

    @Test
    fun displayDate() = assertEquals("10 Nov 2026", instant.toDisplayDate(utc))

    @Test
    fun monthHeader() = assertEquals("November 2026", instant.toMonthHeader(utc))

    @Test
    fun longDay() = assertEquals("Tuesday, 10 November", instant.toLongDay(utc))

    @Test
    fun yearsAgo() = assertEquals("2 years ago", Instant.parse("2024-11-10T10:00:00Z").yearsAgoText(instant, utc))

    @Test
    fun reminderTime() = assertEquals("21:30", minuteOfDayText(21 * 60 + 30))

    @Test
    fun fullDateTimeUsesTwelveHourClock() {
        val evening = LocalDateTime(2026, 10, 25, 20, 40).toInstant(TimeZone.UTC)
        assertEquals("Sunday, 25 October 2026 · 8:40 PM", evening.toFullDateTime(TimeZone.UTC))
        val midnight = LocalDateTime(2026, 1, 1, 0, 5).toInstant(TimeZone.UTC)
        assertEquals("Thursday, 1 January 2026 · 12:05 AM", midnight.toFullDateTime(TimeZone.UTC))
    }
}
