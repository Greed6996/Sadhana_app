package com.sadhana.card.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun testGetStartOfWeek_fromSundayReturnsSameDay() {
        // 2026-10-11 is Sunday
        val sunday = LocalDate.of(2026, 10, 11)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(sunday, DateUtils.getStartOfWeek(sunday))
    }

    @Test
    fun testGetStartOfWeek_fromWednesdayReturnsPrecedingSunday() {
        // 2026-10-14 is Wednesday
        val wednesday = LocalDate.of(2026, 10, 14)
        val expectedSunday = LocalDate.of(2026, 10, 11)
        assertEquals(expectedSunday, DateUtils.getStartOfWeek(wednesday))
    }

    @Test
    fun testGetStartOfWeek_fromSaturdayReturnsPrecedingSunday() {
        // 2026-10-17 is Saturday
        val saturday = LocalDate.of(2026, 10, 17)
        val expectedSunday = LocalDate.of(2026, 10, 11)
        assertEquals(expectedSunday, DateUtils.getStartOfWeek(saturday))
    }

    @Test
    fun testGetWeekSundayStart_returnsSevenConsecutiveDays() {
        val anyDay = LocalDate.of(2026, 10, 13) // Tuesday
        val days = DateUtils.getWeekSundayStart(anyDay)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 10, 11), days[0]) // Sun
        assertEquals(LocalDate.of(2026, 10, 12), days[1]) // Mon
        assertEquals(LocalDate.of(2026, 10, 13), days[2]) // Tue
        assertEquals(LocalDate.of(2026, 10, 14), days[3]) // Wed
        assertEquals(LocalDate.of(2026, 10, 15), days[4]) // Thu
        assertEquals(LocalDate.of(2026, 10, 16), days[5]) // Fri
        assertEquals(LocalDate.of(2026, 10, 17), days[6]) // Sat
    }

    @Test
    fun testFormatWeekRangeUpper() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals("11 – 17 OCT 2026", DateUtils.formatWeekRangeUpper(date))
    }

    @Test
    fun testFormatWeekRange() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals("11 – 17 Oct 2026", DateUtils.formatWeekRange(date))
    }

    @Test
    fun testFormatDayScoreTitle() {
        val sunday = LocalDate.of(2026, 10, 11)
        assertEquals("Sunday’s score", DateUtils.formatDayScoreTitle(sunday))
    }
}
