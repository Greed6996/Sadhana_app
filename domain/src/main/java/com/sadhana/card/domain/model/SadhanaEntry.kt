package com.sadhana.card.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Predefined scoring categories for Bedtime.
 * Each option maps to a specific point value.
 */
enum class BedtimeCategory(val points: Int, val label: String) {
    BY_9_30_PM(25, "9:30 PM or before"),
    BEFORE_10_00_PM(20, "Before 10:00 PM"),
    BEFORE_10_30_PM(15, "Before 10:30 PM"),
    BEFORE_10_45_PM(10, "Before 10:45 PM"),
    BEFORE_11_00_PM(5, "Before 11:00 PM"),
    AFTER_11_00_PM(0, "11:00 PM or later");
}

/**
 * Predefined scoring categories for Wake-up.
 */
enum class WakeUpCategory(val points: Int, val label: String) {
    BEFORE_3_00_AM(25, "Before 3:00 AM"),
    BEFORE_3_30_AM(20, "Before 3:30 AM"),
    BEFORE_4_00_AM(15, "Before 4:00 AM"),
    BEFORE_4_15_AM(10, "Before 4:15 AM"),
    BEFORE_4_30_AM(5, "Before 4:30 AM"),
    AFTER_4_30_AM(0, "4:30 AM or later");
}

/**
 * Predefined scoring categories for Day Sleep.
 */
enum class DaySleepCategory(val points: Int, val label: String) {
    UP_TO_30_MIN(25, "30 minutes or less"),
    FROM_31_TO_45_MIN(20, "31–45 minutes"),
    FROM_46_TO_60_MIN(10, "46–60 minutes"),
    MORE_THAN_60_MIN(0, "More than 60 minutes");
}

/**
 * Predefined scoring categories for Japa – 16 rounds.
 */
enum class JapaCategory(val points: Int, val label: String) {
    BY_7_15_AM(50, "Before 7:15 AM"),
    BY_9_00_AM(40, "Before 9:00 AM"),
    BY_11_59_AM(30, "Before 11:59 AM"),
    BY_3_00_PM(20, "Before 3:00 PM"),
    BY_6_00_PM(10, "Before 6:00 PM"),
    AFTER_6_00_PM(0, "After 6:00 PM");
}

/**
 * Domain representation of a daily Sadhana entry.
 * One row per date.
 *
 * Supports both legacy time-based fields (bedtime, wakeTime, napMinutes, japaTime)
 * and new category-based fields (bedtimeCategory, wakeUpCategory, daySleepCategory,
 * japaCategory, japaCompleted). The scoring engine uses category fields when available,
 * falling back to legacy fields for historical data.
 */
data class SadhanaEntry(
    val date: LocalDate,
    // Legacy fields – preserved for backward compatibility with historical data
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val napMinutes: Int? = null,
    val trainJourney: Boolean = false,
    val japaTime: LocalTime? = null,
    // New category-based fields
    val bedtimeCategory: BedtimeCategory? = null,
    val wakeUpCategory: WakeUpCategory? = null,
    val daySleepCategory: DaySleepCategory? = null,
    val japaCategory: JapaCategory? = null,
    val japaCompleted: Boolean = false,
    // Unscored activity fields
    val pathanMinutes: Int = 0,
    val sravanMinutes: Int = 0,
    val sevaMinutes: Int = 0,
    val sevaTags: List<String> = emptyList(),
    val userId: String = "local",
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
