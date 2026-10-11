package com.sadhana.card.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Domain representation of a daily Sadhana entry.
 * One row per date.
 * Null time/minutes means "not logged".
 */
data class SadhanaEntry(
    val date: LocalDate,
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val napMinutes: Int? = null,
    val trainJourney: Boolean = false,
    val japaTime: LocalTime? = null,
    val pathanMinutes: Int = 0,
    val sravanMinutes: Int = 0,
    val sevaMinutes: Int = 0,
    val sevaTags: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
