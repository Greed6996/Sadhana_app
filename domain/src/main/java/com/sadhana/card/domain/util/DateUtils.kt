package com.sadhana.card.domain.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date and time utilities for Sunday-start Sadhana tracking week.
 */
object DateUtils {

    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /**
     * Returns the Sunday at the start of the week for any given date.
     * In ISO-8601, MONDAY=1 ... SUNDAY=7.
     * So for Sunday, daysFromSunday = 0; for Monday, daysFromSunday = 1; etc.
     */
    fun getStartOfWeek(date: LocalDate): LocalDate {
        val daysFromSunday = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0L else date.dayOfWeek.value.toLong()
        return date.minusDays(daysFromSunday)
    }

    /**
     * Returns the 7 days (Sunday through Saturday) for the week containing [date].
     */
    fun getWeekSundayStart(date: LocalDate): List<LocalDate> {
        val sunday = getStartOfWeek(date)
        return (0L..6L).map { sunday.plusDays(it) }
    }

    /**
     * Formats week range as "11 – 17 OCT 2026" (uppercase for Daily screen).
     */
    fun formatWeekRangeUpper(date: LocalDate): String {
        val sunday = getStartOfWeek(date)
        val saturday = sunday.plusDays(6)
        return if (sunday.month == saturday.month && sunday.year == saturday.year) {
            "${sunday.dayOfMonth} – ${saturday.dayOfMonth} ${sunday.format(MONTH_YEAR_FORMATTER).uppercase(Locale.ENGLISH)}"
        } else if (sunday.year == saturday.year) {
            val startMonth = sunday.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
            val endMonthYear = saturday.format(MONTH_YEAR_FORMATTER).uppercase(Locale.ENGLISH)
            "${sunday.dayOfMonth} $startMonth – ${saturday.dayOfMonth} $endMonthYear"
        } else {
            val startFull = sunday.format(MONTH_YEAR_FORMATTER).uppercase(Locale.ENGLISH)
            val endFull = saturday.format(MONTH_YEAR_FORMATTER).uppercase(Locale.ENGLISH)
            "${sunday.dayOfMonth} $startFull – ${saturday.dayOfMonth} $endFull"
        }
    }

    /**
     * Formats week range as "11 – 17 Oct 2026" (title case for Weekly matrix header).
     */
    fun formatWeekRange(date: LocalDate): String {
        val sunday = getStartOfWeek(date)
        val saturday = sunday.plusDays(6)
        return if (sunday.month == saturday.month && sunday.year == saturday.year) {
            "${sunday.dayOfMonth} – ${saturday.dayOfMonth} ${sunday.format(MONTH_YEAR_FORMATTER)}"
        } else if (sunday.year == saturday.year) {
            val startMonth = sunday.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
            val endMonthYear = saturday.format(MONTH_YEAR_FORMATTER)
            "${sunday.dayOfMonth} $startMonth – ${saturday.dayOfMonth} $endMonthYear"
        } else {
            val startFull = sunday.format(MONTH_YEAR_FORMATTER)
            val endFull = saturday.format(MONTH_YEAR_FORMATTER)
            "${sunday.dayOfMonth} $startFull – ${saturday.dayOfMonth} $endFull"
        }
    }

    /**
     * Formats short day name: "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat".
     */
    fun formatDayName(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
    }

    /**
     * Formats day of month: "1", "11", etc.
     */
    fun formatDayNumber(date: LocalDate): String {
        return date.dayOfMonth.toString()
    }

    /**
     * Formats title: "Sunday's score", "Monday's score", etc.
     */
    fun formatDayScoreTitle(date: LocalDate): String {
        val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
        return "$dayName’s score"
    }

    /**
     * Formats time to 12-hour AM/PM string, or "—" if null.
     */
    fun formatTime(time: LocalTime?): String {
        return time?.format(TIME_FORMATTER) ?: "—"
    }
}
