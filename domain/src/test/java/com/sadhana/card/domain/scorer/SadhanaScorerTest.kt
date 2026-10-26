package com.sadhana.card.domain.scorer

import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SadhanaScorerTest {

    // ---------------------------------------------------------
    // BEDTIME BOUNDARY TESTS (0–25 points)
    // <= 21:30 -> 25
    // 21:31–22:00 -> 20
    // 22:01–22:30 -> 15
    // 22:31–22:45 -> 10
    // 22:46–23:00 -> 5
    // after 23:00 -> 0
    // 00:00 to 03:59 (late-night) -> 0
    // 04:00 to 21:30 -> 25
    // ---------------------------------------------------------

    @Test
    fun testBedtime_boundary21_30_scores25() {
        assertEquals(25, SadhanaScorer.scoreBedtime(LocalTime.of(21, 30)))
    }

    @Test
    fun testBedtime_boundary21_31_scores20() {
        assertEquals(20, SadhanaScorer.scoreBedtime(LocalTime.of(21, 31)))
    }

    @Test
    fun testBedtime_boundary22_00_scores20() {
        assertEquals(20, SadhanaScorer.scoreBedtime(LocalTime.of(22, 0)))
    }

    @Test
    fun testBedtime_boundary22_01_scores15() {
        assertEquals(15, SadhanaScorer.scoreBedtime(LocalTime.of(22, 1)))
    }

    @Test
    fun testBedtime_boundary22_30_scores15() {
        assertEquals(15, SadhanaScorer.scoreBedtime(LocalTime.of(22, 30)))
    }

    @Test
    fun testBedtime_boundary22_31_scores10() {
        assertEquals(10, SadhanaScorer.scoreBedtime(LocalTime.of(22, 31)))
    }

    @Test
    fun testBedtime_boundary22_45_scores10() {
        assertEquals(10, SadhanaScorer.scoreBedtime(LocalTime.of(22, 45)))
    }

    @Test
    fun testBedtime_boundary22_46_scores5() {
        assertEquals(5, SadhanaScorer.scoreBedtime(LocalTime.of(22, 46)))
    }

    @Test
    fun testBedtime_boundary23_00_scores5() {
        assertEquals(5, SadhanaScorer.scoreBedtime(LocalTime.of(23, 0)))
    }

    @Test
    fun testBedtime_boundary23_01_scores0() {
        assertEquals(0, SadhanaScorer.scoreBedtime(LocalTime.of(23, 1)))
    }

    @Test
    fun testBedtime_lateNight00_30_scores0() {
        assertEquals(0, SadhanaScorer.scoreBedtime(LocalTime.of(0, 30)))
    }

    @Test
    fun testBedtime_lateNight03_59_scores0() {
        assertEquals(0, SadhanaScorer.scoreBedtime(LocalTime.of(3, 59)))
    }

    @Test
    fun testBedtime_earlyMorning04_00_scores25() {
        assertEquals(25, SadhanaScorer.scoreBedtime(LocalTime.of(4, 0)))
    }

    @Test
    fun testBedtime_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreBedtime(null))
    }

    // ---------------------------------------------------------
    // WAKE-UP BOUNDARY TESTS (0–25 points)
    // before 03:00 -> 25
    // 03:00–03:30 -> 20
    // 03:31–04:00 -> 15
    // 04:01–04:15 -> 10
    // 04:16–04:30 -> 5
    // after 04:30 -> 0
    // ---------------------------------------------------------

    @Test
    fun testWakeUp_boundary02_59_scores25() {
        assertEquals(25, SadhanaScorer.scoreWakeUp(LocalTime.of(2, 59)))
    }

    @Test
    fun testWakeUp_boundary03_00_scores20() {
        assertEquals(20, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 0)))
    }

    @Test
    fun testWakeUp_boundary03_30_scores20() {
        assertEquals(20, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 30)))
    }

    @Test
    fun testWakeUp_boundary03_31_scores15() {
        assertEquals(15, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 31)))
    }

    @Test
    fun testWakeUp_boundary04_00_scores15() {
        assertEquals(15, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 0)))
    }

    @Test
    fun testWakeUp_boundary04_01_scores10() {
        assertEquals(10, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 1)))
    }

    @Test
    fun testWakeUp_boundary04_15_scores10() {
        assertEquals(10, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 15)))
    }

    @Test
    fun testWakeUp_boundary04_16_scores5() {
        assertEquals(5, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 16)))
    }

    @Test
    fun testWakeUp_boundary04_30_scores5() {
        assertEquals(5, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 30)))
    }

    @Test
    fun testWakeUp_boundary04_31_scores0() {
        assertEquals(0, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 31)))
    }

    @Test
    fun testWakeUp_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreWakeUp(null))
    }

    // ---------------------------------------------------------
    // DAY SLEEP / NAP BOUNDARY TESTS (0–25 points)
    // Travel allowance removed
    // <= 30 -> 25
    // 31–45 -> 20
    // 46–60 -> 10
    // > 60 -> 0
    // ---------------------------------------------------------

    @Test
    fun testDaySleep_boundary30_scores25() {
        assertEquals(25, SadhanaScorer.scoreDaySleep(30))
    }

    @Test
    fun testDaySleep_boundary31_scores20() {
        assertEquals(20, SadhanaScorer.scoreDaySleep(31))
    }

    @Test
    fun testDaySleep_boundary45_scores20() {
        assertEquals(20, SadhanaScorer.scoreDaySleep(45))
    }

    @Test
    fun testDaySleep_boundary46_scores10() {
        assertEquals(10, SadhanaScorer.scoreDaySleep(46))
    }

    @Test
    fun testDaySleep_boundary60_scores10() {
        assertEquals(10, SadhanaScorer.scoreDaySleep(60))
    }

    @Test
    fun testDaySleep_boundary61_scores0() {
        assertEquals(0, SadhanaScorer.scoreDaySleep(61))
    }

    @Test
    fun testDaySleep_travelAllowanceIgnored() {
        // Without travel allowance: 90 mins is > 60 mins -> 0 pts
        assertEquals(0, SadhanaScorer.scoreDaySleep(90, trainJourney = true))
    }

    @Test
    fun testDaySleep_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreDaySleep(null))
    }

    // ---------------------------------------------------------
    // JAPA (16 ROUNDS COMPLETED BY) BOUNDARY TESTS (-5–25 points)
    // <= 09:00 -> 25
    // 09:01–12:00 -> 20
    // 12:01–15:00 -> 15
    // 15:01–18:00 -> 10
    // 18:01–21:00 -> 0
    // after 21:00 -> -5
    // ---------------------------------------------------------

    @Test
    fun testJapa_boundary09_00_scores25() {
        assertEquals(25, SadhanaScorer.scoreJapa(LocalTime.of(9, 0)))
    }

    @Test
    fun testJapa_boundary09_01_scores20() {
        assertEquals(20, SadhanaScorer.scoreJapa(LocalTime.of(9, 1)))
    }

    @Test
    fun testJapa_boundary12_00_scores20() {
        assertEquals(20, SadhanaScorer.scoreJapa(LocalTime.of(12, 0)))
    }

    @Test
    fun testJapa_boundary12_01_scores15() {
        assertEquals(15, SadhanaScorer.scoreJapa(LocalTime.of(12, 1)))
    }

    @Test
    fun testJapa_boundary15_00_scores15() {
        assertEquals(15, SadhanaScorer.scoreJapa(LocalTime.of(15, 0)))
    }

    @Test
    fun testJapa_boundary15_01_scores10() {
        assertEquals(10, SadhanaScorer.scoreJapa(LocalTime.of(15, 1)))
    }

    @Test
    fun testJapa_boundary18_00_scores10() {
        assertEquals(10, SadhanaScorer.scoreJapa(LocalTime.of(18, 0)))
    }

    @Test
    fun testJapa_boundary18_01_scores0() {
        assertEquals(0, SadhanaScorer.scoreJapa(LocalTime.of(18, 1)))
    }

    @Test
    fun testJapa_boundary21_00_scores0() {
        assertEquals(0, SadhanaScorer.scoreJapa(LocalTime.of(21, 0)))
    }

    @Test
    fun testJapa_boundary21_01_scoresMinus5() {
        assertEquals(-5, SadhanaScorer.scoreJapa(LocalTime.of(21, 1)))
    }

    @Test
    fun testJapa_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreJapa(null))
    }

    // ---------------------------------------------------------
    // BEDTIME CATEGORY TESTS (6 options: 25, 20, 15, 10, 5, 0)
    // ---------------------------------------------------------

    @Test
    fun testBedtimeCategory_allSixCategories() {
        assertEquals(25, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.BY_9_30_PM))
        assertEquals(20, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.BEFORE_10_00_PM))
        assertEquals(15, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.BEFORE_10_30_PM))
        assertEquals(10, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.BEFORE_10_45_PM))
        assertEquals(5, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.BEFORE_11_00_PM))
        assertEquals(0, SadhanaScorer.scoreBedtimeCategory(BedtimeCategory.AFTER_11_00_PM))
        assertNull(SadhanaScorer.scoreBedtimeCategory(null))
    }

    // ---------------------------------------------------------
    // WAKE-UP CATEGORY TESTS (6 options: 25, 20, 15, 10, 5, 0)
    // ---------------------------------------------------------

    @Test
    fun testWakeUpCategory_allSixCategories() {
        assertEquals(25, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.BEFORE_3_00_AM))
        assertEquals(20, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.BEFORE_3_30_AM))
        assertEquals(15, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.BEFORE_4_00_AM))
        assertEquals(10, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.BEFORE_4_15_AM))
        assertEquals(5, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.BEFORE_4_30_AM))
        assertEquals(0, SadhanaScorer.scoreWakeUpCategory(WakeUpCategory.AFTER_4_30_AM))
        assertNull(SadhanaScorer.scoreWakeUpCategory(null))
    }

    // ---------------------------------------------------------
    // DAY SLEEP CATEGORY TESTS (4 options: 25, 20, 10, 0)
    // ---------------------------------------------------------

    @Test
    fun testDaySleepCategory_allFourCategories() {
        assertEquals(25, SadhanaScorer.scoreDaySleepCategory(DaySleepCategory.UP_TO_30_MIN))
        assertEquals(20, SadhanaScorer.scoreDaySleepCategory(DaySleepCategory.FROM_31_TO_45_MIN))
        assertEquals(10, SadhanaScorer.scoreDaySleepCategory(DaySleepCategory.FROM_46_TO_60_MIN))
        assertEquals(0, SadhanaScorer.scoreDaySleepCategory(DaySleepCategory.MORE_THAN_60_MIN))
        assertNull(SadhanaScorer.scoreDaySleepCategory(null))
    }

    // ---------------------------------------------------------
    // JAPA CATEGORY TESTS (6 options: 25, 20, 15, 10, 0, -5)
    // ---------------------------------------------------------

    @Test
    fun testJapaCategory_whenCompleted_allSixCategories() {
        assertEquals(25, SadhanaScorer.scoreJapaCategory(JapaCategory.BY_9_00_AM, japaCompleted = true))
        assertEquals(20, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_12_00_PM, japaCompleted = true))
        assertEquals(15, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_3_00_PM, japaCompleted = true))
        assertEquals(10, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_6_00_PM, japaCompleted = true))
        assertEquals(0, SadhanaScorer.scoreJapaCategory(JapaCategory.AFTER_6_00_PM, japaCompleted = true))
        assertEquals(-5, SadhanaScorer.scoreJapaCategory(JapaCategory.AFTER_9_00_PM_OR_INCOMPLETE, japaCompleted = true))
    }

    @Test
    fun testJapaCategory_directScoringWithoutCompletionFlag() {
        assertEquals(25, SadhanaScorer.scoreJapaCategory(JapaCategory.BY_9_00_AM))
        assertEquals(20, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_12_00_PM))
        assertEquals(15, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_3_00_PM))
        assertEquals(10, SadhanaScorer.scoreJapaCategory(JapaCategory.BEFORE_6_00_PM))
        assertEquals(0, SadhanaScorer.scoreJapaCategory(JapaCategory.AFTER_6_00_PM))
        assertEquals(-5, SadhanaScorer.scoreJapaCategory(JapaCategory.AFTER_9_00_PM_OR_INCOMPLETE))
        assertNull(SadhanaScorer.scoreJapaCategory(null))
    }

    @Test
    fun testJapaCategory_whenIncomplete_scoresMinus5() {
        assertEquals(-5, SadhanaScorer.scoreJapaCategory(JapaCategory.BY_9_00_AM, japaCompleted = false))
        assertEquals(-5, SadhanaScorer.scoreJapaCategory(JapaCategory.AFTER_9_00_PM_OR_INCOMPLETE, japaCompleted = false))
    }

    // ---------------------------------------------------------
    // PATHAN AND SHRAVAN SCORING TESTS
    // ---------------------------------------------------------

    @Test
    fun testPathanScoring_tiersAndPenalties() {
        assertEquals(25, SadhanaScorer.scorePathan(30))
        assertEquals(25, SadhanaScorer.scorePathan(45))
        assertEquals(25, SadhanaScorer.scorePathan(60))
        assertEquals(20, SadhanaScorer.scorePathan(20))
        assertEquals(20, SadhanaScorer.scorePathan(25))
        assertEquals(10, SadhanaScorer.scorePathan(15))
        assertEquals(5, SadhanaScorer.scorePathan(10))
        assertEquals(0, SadhanaScorer.scorePathan(5))
        assertEquals(-5, SadhanaScorer.scorePathan(0)) // -5 penalty for zero reading
        assertNull(SadhanaScorer.scorePathan(null))
    }

    @Test
    fun testShravanScoring_tiersAndPenalties() {
        assertEquals(25, SadhanaScorer.scoreShravan(30))
        assertEquals(25, SadhanaScorer.scoreShravan(45))
        assertEquals(25, SadhanaScorer.scoreShravan(60))
        assertEquals(20, SadhanaScorer.scoreShravan(20))
        assertEquals(20, SadhanaScorer.scoreShravan(25))
        assertEquals(10, SadhanaScorer.scoreShravan(15))
        assertEquals(5, SadhanaScorer.scoreShravan(10))
        assertEquals(0, SadhanaScorer.scoreShravan(5))
        assertEquals(-5, SadhanaScorer.scoreShravan(0)) // -5 penalty for zero hearing
        assertNull(SadhanaScorer.scoreShravan(null))
    }

    // ---------------------------------------------------------
    // SADHANA ENTRY INTEGRATED SCORING (scoreEntry)
    // ---------------------------------------------------------

    @Test
    fun testScoreEntry_allSixCategoriesFullMarks_scores150Max() {
        val entry = SadhanaEntry(
            date = LocalDate.of(2026, 10, 14),
            bedtimeCategory = BedtimeCategory.BY_9_30_PM,       // 25
            wakeUpCategory = WakeUpCategory.BEFORE_3_00_AM,    // 25
            japaCategory = JapaCategory.BY_9_00_AM,            // 25
            japaCompleted = true,
            daySleepCategory = DaySleepCategory.UP_TO_30_MIN,  // 25
            pathanMinutes = 30,                                // 25
            sravanMinutes = 30                                 // 25
        )
        val scores = SadhanaScorer.scoreEntry(entry)
        assertEquals(25, scores.bedScore)
        assertEquals(25, scores.wakeScore)
        assertEquals(25, scores.japaScore)
        assertEquals(25, scores.napScore)
        assertEquals(25, scores.pathanScore)
        assertEquals(25, scores.sravanScore)
        assertEquals(150, scores.totalScore)
    }

    @Test
    fun testScoreEntry_unloggedReturnsNullTotalScore() {
        val entry = SadhanaEntry(date = LocalDate.of(2026, 10, 14))
        val scores = SadhanaScorer.scoreEntry(entry)
        assertNull(scores.totalScore)
        assertNull(scores.pathanScore)
        assertNull(scores.sravanScore)
        assertNull(scores.japaScore)
        assertNull(scores.bedScore)
        assertNull(scores.wakeScore)
        assertNull(scores.napScore)
    }

    @Test
    fun testFormatMinutesToHoursMinutes() {
        assertEquals("0h 0m", SadhanaScorer.formatMinutesToHoursMinutes(0))
        assertEquals("0h 45m", SadhanaScorer.formatMinutesToHoursMinutes(45))
        assertEquals("1h 15m", SadhanaScorer.formatMinutesToHoursMinutes(75))
        assertEquals("2h 0m", SadhanaScorer.formatMinutesToHoursMinutes(120))
        assertEquals("3h 45m", SadhanaScorer.formatMinutesToHoursMinutes(225))
    }
}
