package com.sadhana.card.domain.scorer

import com.sadhana.card.domain.model.SadhanaEntry
import java.time.LocalTime

/**
 * Pure Kotlin scoring engine for Sadhana Card.
 * No Android framework dependencies.
 * All time comparisons use minute-of-day integers.
 */
object SadhanaScorer {

    data class DailyScores(
        val bedScore: Int?,
        val wakeScore: Int?,
        val napScore: Int?,
        val japaScore: Int?,
        val totalScore: Int?
    )

    /**
     * Bedtime scoring (0–5):
     * <= 21:30 -> 5
     * 21:31–22:00 -> 4
     * 22:01–22:30 -> 3
     * 22:31–22:45 -> 2
     * 22:46–23:00 -> 1
     * after 23:00 -> 0
     * EDGE CASE: 00:00 to 03:59 are late-night and score 0 (treated as after 23:00, not early).
     * 04:00 to 21:30 score 5.
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
     * Wake-up scoring (0–5):
     * before 03:00 -> 5
     * 03:00–03:30 -> 4
     * 03:31–04:00 -> 3
     * 04:01–04:15 -> 2
     * 04:16–04:30 -> 1
     * after 04:30 -> 0
     * null -> null (not logged)
     */
    fun scoreWakeUp(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            // Before 03:00 (m < 180) -> 5
            m < 3 * 60 -> 5
            // 03:00 to 03:30 (180 <= m <= 210) -> 4
            m in (3 * 60)..(3 * 60 + 30) -> 4
            // 03:31 to 04:00 (211 <= m <= 240) -> 3
            m in (3 * 60 + 31)..(4 * 60) -> 3
            // 04:01 to 04:15 (241 <= m <= 255) -> 2
            m in (4 * 60 + 1)..(4 * 60 + 15) -> 2
            // 04:16 to 04:30 (256 <= m <= 270) -> 1
            m in (4 * 60 + 16)..(4 * 60 + 30) -> 1
            // After 04:30 -> 0
            else -> 0
        }
    }

    /**
     * Day sleep scoring (0–5):
     * effective = max(0, napMinutes - (if trainJourney 60 else 0))
     * effective <= 30 -> 5
     * 31–45 -> 4
     * 46–60 -> 2
     * > 60 -> 0
     * Note: Gap at 3 points is intentional per the source card.
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
     * Japa scoring, 16 rounds completed by (0–5):
     * <= 07:15 -> 5
     * 07:16–09:00 -> 4
     * 09:01–11:59 -> 3
     * 12:00–15:00 -> 2
     * 15:01–18:00 -> 1
     * after 18:00 -> 0
     * null -> null (not logged)
     */
    fun scoreJapa(time: LocalTime?): Int? {
        if (time == null) return null
        val m = minuteOfDay(time)
        return when {
            // <= 07:15 (m <= 435) -> 5
            m <= 7 * 60 + 15 -> 5
            // 07:16 to 09:00 (436 <= m <= 540) -> 4
            m in (7 * 60 + 16)..(9 * 60) -> 4
            // 09:01 to 11:59 (541 <= m <= 719) -> 3
            m in (9 * 60 + 1)..(11 * 60 + 59) -> 3
            // 12:00 to 15:00 (720 <= m <= 900) -> 2
            m in (12 * 60)..(15 * 60) -> 2
            // 15:01 to 18:00 (901 <= m <= 1080) -> 1
            m in (15 * 60 + 1)..(18 * 60) -> 1
            // After 18:00 -> 0
            else -> 0
        }
    }

    /**
     * Computes daily scores.
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

    fun scoreEntry(entry: SadhanaEntry): DailyScores {
        return scoreDaily(
            bedtime = entry.bedtime,
            wakeTime = entry.wakeTime,
            napMinutes = entry.napMinutes,
            trainJourney = entry.trainJourney,
            japaTime = entry.japaTime
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
