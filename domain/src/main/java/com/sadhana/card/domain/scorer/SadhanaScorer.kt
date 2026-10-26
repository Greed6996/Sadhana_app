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

    const val MAX_DAILY_SCORE = 150

    data class DailyScores(
        val bedScore: Int? = null,
        val wakeScore: Int? = null,
        val japaScore: Int? = null,
        val napScore: Int? = null,
        val pathanScore: Int? = null,
        val sravanScore: Int? = null,
        val totalScore: Int? = null
    ) {
        // Legacy 5-parameter constructor preserved for backward compatibility
        constructor(
            bedScore: Int?,
            wakeScore: Int?,
            napScore: Int?,
            japaScore: Int?,
            totalScore: Int?
        ) : this(
            bedScore = bedScore,
            wakeScore = wakeScore,
            japaScore = japaScore,
            napScore = napScore,
            pathanScore = null,
            sravanScore = null,
            totalScore = totalScore
        )
    }

    // ── Category-based scoring (primary) ──────────────────────────────

    /**
     * Bedtime score from selected category (0–25 points).
     */
    fun scoreBedtimeCategory(category: BedtimeCategory?): Int? {
        return category?.points
    }

    /**
     * Wake-up score from selected category (0–25 points).
     */
    fun scoreWakeUpCategory(category: WakeUpCategory?): Int? {
        return category?.points
    }

    /**
     * Day sleep score from selected category (0–25 points).
     */
    fun scoreDaySleepCategory(category: DaySleepCategory?): Int? {
        return category?.points
    }

    /**
     * Japa score from selected category (0–25 points, or -5 for after 9pm / incomplete).
     */
    fun scoreJapaCategory(category: JapaCategory?): Int? {
        return category?.points
    }

    /**
     * Japa score from selected category and completion state.
     */
    fun scoreJapaCategory(category: JapaCategory?, japaCompleted: Boolean): Int? {
        if (category == null) return null
        return if (japaCompleted) category.points else JapaCategory.AFTER_9_00_PM_OR_INCOMPLETE.points
    }

    // ── Activity-based scoring (Pathan, Shravan) ──────────────────────

    /**
     * Pathan (Reading) scoring:
     * >= 30 mins -> 25 points
     * 20..29 mins -> 20 points
     * 15..19 mins -> 10 points
     * 10..14 mins -> 5 points
     * 1..9 mins -> 0 points
     * 0 mins (or <= 0) -> -5 points (Penalty for zero reading)
     */
    fun scorePathan(minutes: Int?): Int? {
        if (minutes == null) return null
        return when {
            minutes >= 30 -> 25
            minutes in 20..29 -> 20
            minutes in 15..19 -> 10
            minutes in 10..14 -> 5
            minutes in 1..9 -> 0
            else -> -5
        }
    }

    /**
     * Shravan (Listening) scoring:
     * >= 30 mins -> 25 points
     * 20..29 mins -> 20 points
     * 15..19 mins -> 10 points
     * 10..14 mins -> 5 points
     * 1..9 mins -> 0 points
     * 0 mins (or <= 0) -> -5 points (Penalty for zero listening)
     */
    fun scoreShravan(minutes: Int?): Int? {
        if (minutes == null) return null
        return when {
            minutes >= 30 -> 25
            minutes in 20..29 -> 20
            minutes in 15..19 -> 10
            minutes in 10..14 -> 5
            minutes in 1..9 -> 0
            else -> -5
        }
    }

    fun scoreSravan(minutes: Int?): Int? = scoreShravan(minutes)

    // ── Legacy time-based scoring (fallback for historical data) ──────

    /**
     * Bedtime scoring (0–25) from exact time.
     * <= 21:30 -> 25, 21:31–22:00 -> 20, 22:01–22:30 -> 15,
     * 22:31–22:45 -> 10, 22:46–23:00 -> 5, after 23:00 -> 0.
     * EDGE CASE: 00:00 to 03:59 are late-night and score 0.
     * null -> null (not logged)
     */
    fun scoreBedtime(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            // Late night 00:00 to 03:59 (0 <= m < 240) -> 0
            m in 0 until 4 * 60 -> 0
            // 04:00 to 21:30 (240 <= m <= 1290) -> 25
            m in (4 * 60)..(21 * 60 + 30) -> 25
            // 21:31 to 22:00 (1291 <= m <= 1320) -> 20
            m in (21 * 60 + 31)..(22 * 60) -> 20
            // 22:01 to 22:30 (1321 <= m <= 1350) -> 15
            m in (22 * 60 + 1)..(22 * 60 + 30) -> 15
            // 22:31 to 22:45 (1351 <= m <= 1365) -> 10
            m in (22 * 60 + 31)..(22 * 60 + 45) -> 10
            // 22:46 to 23:00 (1366 <= m <= 1380) -> 5
            m in (22 * 60 + 46)..(23 * 60) -> 5
            // After 23:00 (1381 <= m <= 1439) -> 0
            else -> 0
        }
    }

    /**
     * Wake-up scoring (0–25) from exact time.
     * before 03:00 -> 25, 03:00–03:30 -> 20, 03:31–04:00 -> 15,
     * 04:01–04:15 -> 10, 04:16–04:30 -> 5, after 04:30 -> 0.
     * null -> null (not logged)
     */
    fun scoreWakeUp(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            m < 3 * 60 -> 25
            m in (3 * 60)..(3 * 60 + 30) -> 20
            m in (3 * 60 + 31)..(4 * 60) -> 15
            m in (4 * 60 + 1)..(4 * 60 + 15) -> 10
            m in (4 * 60 + 16)..(4 * 60 + 30) -> 5
            else -> 0
        }
    }

    /**
     * Day sleep scoring (0–25) from exact minutes.
     * <= 30 -> 25, 31–45 -> 20, 46–60 -> 10, > 60 -> 0.
     * null -> null (not logged)
     */
    fun scoreDaySleep(napMinutes: Int?, trainJourney: Boolean = false): Int? {
        if (napMinutes == null) return null
        return when {
            napMinutes <= 30 -> 25
            napMinutes in 31..45 -> 20
            napMinutes in 46..60 -> 10
            else -> 0
        }
    }

    /**
     * Japa scoring (-5–25) from exact time.
     * <= 09:00 -> 25, 09:01–12:00 -> 20, 12:01–15:00 -> 15,
     * 15:01–18:00 -> 10, 18:01–21:00 -> 0, after 21:00 -> -5.
     * null -> null (not logged)
     */
    fun scoreJapa(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            m <= 9 * 60 -> 25
            m in (9 * 60 + 1)..(12 * 60) -> 20
            m in (12 * 60 + 1)..(15 * 60) -> 15
            m in (15 * 60 + 1)..(18 * 60) -> 10
            m in (18 * 60 + 1)..(21 * 60) -> 0
            else -> -5
        }
    }

    // ── Unified daily scoring ─────────────────────────────────────────

    /**
     * Computes daily scores using legacy time-based inputs.
     * If all fields are null (unlogged day), totalScore is null.
     */
    fun scoreDaily(
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        napMinutes: Int?,
        trainJourney: Boolean = false,
        japaTime: LocalTime?,
        pathanMinutes: Int? = null,
        sravanMinutes: Int? = null
    ): DailyScores {
        val bed = scoreBedtime(bedtime)
        val wake = scoreWakeUp(wakeTime)
        val japa = scoreJapa(japaTime)
        val nap = scoreDaySleep(napMinutes)
        val pathan = scorePathan(pathanMinutes)
        val sravan = scoreShravan(sravanMinutes)

        val logged = listOfNotNull(bed, wake, japa, nap, pathan, sravan)
        val total = if (logged.isEmpty()) null else logged.sum()

        return DailyScores(
            bedScore = bed,
            wakeScore = wake,
            japaScore = japa,
            napScore = nap,
            pathanScore = pathan,
            sravanScore = sravan,
            totalScore = total
        )
    }

    /**
     * Scores a [SadhanaEntry] using category fields when available,
     * falling back to legacy time-based fields for backward compatibility.
     *
     * Order of categories in breakdown:
     * 1. Bedtime (0–25)
     * 2. Wake-up (0–25)
     * 3. 16 Rounds Japa (-5 to 25)
     * 4. Day Sleep / Nap (0–25)
     * 5. Pathan (-5 to 25)
     * 6. Shravan (-5 to 25)
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

        // Japa: category takes precedence
        val japa = if (entry.japaCategory != null) {
            scoreJapaCategory(entry.japaCategory, entry.japaCompleted)
        } else {
            scoreJapa(entry.japaTime)
        }

        // Day sleep: category takes precedence (placed after Japa)
        val nap = if (entry.daySleepCategory != null) {
            scoreDaySleepCategory(entry.daySleepCategory)
        } else {
            scoreDaySleep(entry.napMinutes)
        }

        // Determine if entry has any recorded input (is logged)
        val hasLoggedData = entry.bedtimeCategory != null ||
            entry.wakeUpCategory != null ||
            entry.daySleepCategory != null ||
            entry.japaCategory != null ||
            entry.bedtime != null ||
            entry.wakeTime != null ||
            entry.napMinutes != null ||
            entry.japaTime != null ||
            entry.pathanMinutes > 0 ||
            entry.sravanMinutes > 0

        // Study scores (null if unlogged)
        val pathan = if (!hasLoggedData) null else scorePathan(entry.pathanMinutes)
        val sravan = if (!hasLoggedData) null else scoreShravan(entry.sravanMinutes)

        val total = if (!hasLoggedData) {
            null
        } else {
            (bed ?: 0) + (wake ?: 0) + (japa ?: 0) + (nap ?: 0) +
                (pathan ?: 0) + (sravan ?: 0)
        }

        return DailyScores(
            bedScore = bed,
            wakeScore = wake,
            japaScore = japa,
            napScore = nap,
            pathanScore = pathan,
            sravanScore = sravan,
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
