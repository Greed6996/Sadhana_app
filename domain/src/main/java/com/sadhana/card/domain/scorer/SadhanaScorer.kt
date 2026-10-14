package com.sadhana.card.domain.scorer

import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import java.time.LocalTime

/**
 * Pure Kotlin scoring engine for Sadhana Card.
 * No Android framework dependencies.
 *
 * Supports both category-based scoring (new) and time-based scoring (legacy).
 * Category fields take precedence when present.
 */
object SadhanaScorer {

    data class DailyScores(
        val bedScore: Int?,
        val wakeScore: Int?,
        val napScore: Int?,
        val japaScore: Int?,
        val totalScore: Int?
    )

    // ── Category-based scoring (primary) ──────────────────────────────

    /**
     * Bedtime score from selected category.
     */
    fun scoreBedtimeCategory(category: BedtimeCategory?): Int? {
        return category?.points
    }

    /**
     * Wake-up score from selected category.
     */
    fun scoreWakeUpCategory(category: WakeUpCategory?): Int? {
        return category?.points
    }

    /**
     * Day sleep score from selected category.
     * Note: Train journey allowance is NOT applied to category-based scoring
     * because the user selects the effective duration category directly.
     */
    fun scoreDaySleepCategory(category: DaySleepCategory?): Int? {
        return category?.points
    }

    /**
     * Japa score from selected category and completion state.
     * If japaCompleted is false, score is always 0 (if a category is selected)
     * or null (if no category is selected).
     */
    fun scoreJapaCategory(category: JapaCategory?, japaCompleted: Boolean): Int? {
        if (category == null) return null
        return if (japaCompleted) category.points else 0
    }

    // ── Legacy time-based scoring (fallback for historical data) ──────

    /**
     * Bedtime scoring (0–5) from exact time.
     * <= 21:30 -> 5, 21:31–22:00 -> 4, 22:01–22:30 -> 3,
     * 22:31–22:45 -> 2, 22:46–23:00 -> 1, after 23:00 -> 0.
     * EDGE CASE: 00:00 to 03:59 are late-night and score 0.
     * null -> null (not logged)
     */
    fun scoreBedtime(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            // Late night 00:00 to 03:59 (0 <= m < 240) -> 0
            m in 0 until 4 * 60 -> 0
            // 04:00 to 21:30 (240 <= m <= 1290) -> 5
            m in (4 * 60)..(21 * 60 + 30) -> 5
            // 21:31 to 22:00 (1291 <= m <= 1320) -> 4
            m in (21 * 60 + 31)..(22 * 60) -> 4
            // 22:01 to 22:30 (1321 <= m <= 1350) -> 3
            m in (22 * 60 + 1)..(22 * 60 + 30) -> 3
            // 22:31 to 22:45 (1351 <= m <= 1365) -> 2
            m in (22 * 60 + 31)..(22 * 60 + 45) -> 2
            // 22:46 to 23:00 (1366 <= m <= 1380) -> 1
            m in (22 * 60 + 46)..(23 * 60) -> 1
            // After 23:00 (1381 <= m <= 1439) -> 0
            else -> 0
        }
    }

    /**
     * Wake-up scoring (0–5) from exact time.
     * before 03:00 -> 5, 03:00–03:30 -> 4, 03:31–04:00 -> 3,
     * 04:01–04:15 -> 2, 04:16–04:30 -> 1, after 04:30 -> 0.
     * null -> null (not logged)
     */
    fun scoreWakeUp(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            m < 3 * 60 -> 5
            m in (3 * 60)..(3 * 60 + 30) -> 4
            m in (3 * 60 + 31)..(4 * 60) -> 3
            m in (4 * 60 + 1)..(4 * 60 + 15) -> 2
            m in (4 * 60 + 16)..(4 * 60 + 30) -> 1
            else -> 0
        }
    }

    /**
     * Day sleep scoring (0–5) from exact minutes with optional train journey allowance.
     * effective = max(0, napMinutes - (if trainJourney 60 else 0))
     * effective <= 30 -> 5, 31–45 -> 4, 46–60 -> 2, > 60 -> 0.
     * null -> null (not logged)
     */
    fun scoreDaySleep(napMinutes: Int?, trainJourney: Boolean = false): Int? {
        if (napMinutes == null) return null
        val allowance = if (trainJourney) 60 else 0
        val effective = maxOf(0, napMinutes - allowance)
        return when {
            effective <= 30 -> 5
            effective in 31..45 -> 4
            effective in 46..60 -> 2
            else -> 0
        }
    }

    /**
     * Japa scoring (0–5) from exact time.
     * <= 07:15 -> 5, 07:16–09:00 -> 4, 09:01–11:59 -> 3,
     * 12:00–15:00 -> 2, 15:01–18:00 -> 1, after 18:00 -> 0.
     * null -> null (not logged)
     */
    fun scoreJapa(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            m <= 7 * 60 + 15 -> 5
            m in (7 * 60 + 16)..(9 * 60) -> 4
            m in (9 * 60 + 1)..(11 * 60 + 59) -> 3
            m in (12 * 60)..(15 * 60) -> 2
            m in (15 * 60 + 1)..(18 * 60) -> 1
            else -> 0
        }
    }

    // ── Unified daily scoring ─────────────────────────────────────────

    /**
     * Computes daily scores using the legacy time-based inputs.
     * If all fields are null (unlogged day), totalScore is null.
     * Otherwise totalScore is sum of the non-null scores (max 20).
     */
    fun scoreDaily(
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        napMinutes: Int?,
        trainJourney: Boolean,
        japaTime: LocalTime?
    ): DailyScores {
        val bed = scoreBedtime(bedtime)
        val wake = scoreWakeUp(wakeTime)
        val nap = scoreDaySleep(napMinutes, trainJourney)
        val japa = scoreJapa(japaTime)

        val logged = listOfNotNull(bed, wake, nap, japa)
        val total = if (logged.isEmpty()) null else logged.sum()

        return DailyScores(
            bedScore = bed,
            wakeScore = wake,
            napScore = nap,
            japaScore = japa,
            totalScore = total
        )
    }

    /**
     * Scores a [SadhanaEntry] using category fields when available,
     * falling back to legacy time-based fields for backward compatibility.
     */
    fun scoreEntry(entry: SadhanaEntry): DailyScores {
        // Bedtime: category takes precedence over legacy time
        val bed = if (entry.bedtimeCategory != null) {
            scoreBedtimeCategory(entry.bedtimeCategory)
        } else {
            scoreBedtime(entry.bedtime)
        }

        // Wake-up: category takes precedence
        val wake = if (entry.wakeUpCategory != null) {
            scoreWakeUpCategory(entry.wakeUpCategory)
        } else {
            scoreWakeUp(entry.wakeTime)
        }

        // Day sleep: category takes precedence
        // When using category, train journey allowance is NOT applied
        // because the user already selects the effective duration category.
        val nap = if (entry.daySleepCategory != null) {
            scoreDaySleepCategory(entry.daySleepCategory)
        } else {
            scoreDaySleep(entry.napMinutes, entry.trainJourney)
        }

        // Japa: category takes precedence
        val japa = if (entry.japaCategory != null) {
            scoreJapaCategory(entry.japaCategory, entry.japaCompleted)
        } else {
            scoreJapa(entry.japaTime)
        }

        val logged = listOfNotNull(bed, wake, nap, japa)
        val total = if (logged.isEmpty()) null else logged.sum()

        return DailyScores(
            bedScore = bed,
            wakeScore = wake,
            napScore = nap,
            japaScore = japa,
            totalScore = total
        )
    }

    /**
     * Formats total minutes into "Xh Ym" format (e.g. 150 -> "2h 30m").
     */
    fun formatMinutesToHoursMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun minuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute
}
