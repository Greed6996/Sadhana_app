package com.sadhana.card.domain.scorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class SadhanaScorerTest {

    // ---------------------------------------------------------
    // BEDTIME BOUNDARY TESTS
    // <= 21:30 -> 5
    // 21:31–22:00 -> 4
    // 22:01–22:30 -> 3
    // 22:31–22:45 -> 2
    // 22:46–23:00 -> 1
    // after 23:00 -> 0
    // 00:00 to 03:59 (late-night) -> 0
    // 04:00 to 21:30 -> 5
    // ---------------------------------------------------------

    @Test
    fun testBedtime_boundary21_30_scores5() {
        assertEquals(5, SadhanaScorer.scoreBedtime(LocalTime.of(21, 30)))
    }

    @Test
    fun testBedtime_boundary21_31_scores4() {
        assertEquals(4, SadhanaScorer.scoreBedtime(LocalTime.of(21, 31)))
    }

    @Test
    fun testBedtime_boundary22_00_scores4() {
        assertEquals(4, SadhanaScorer.scoreBedtime(LocalTime.of(22, 0)))
    }

    @Test
    fun testBedtime_boundary22_01_scores3() {
        assertEquals(3, SadhanaScorer.scoreBedtime(LocalTime.of(22, 1)))
    }

    @Test
    fun testBedtime_boundary22_30_scores3() {
        assertEquals(3, SadhanaScorer.scoreBedtime(LocalTime.of(22, 30)))
    }

    @Test
    fun testBedtime_boundary22_31_scores2() {
        assertEquals(2, SadhanaScorer.scoreBedtime(LocalTime.of(22, 31)))
    }

    @Test
    fun testBedtime_boundary22_45_scores2() {
        assertEquals(2, SadhanaScorer.scoreBedtime(LocalTime.of(22, 45)))
    }

    @Test
    fun testBedtime_boundary22_46_scores1() {
        assertEquals(1, SadhanaScorer.scoreBedtime(LocalTime.of(22, 46)))
    }

    @Test
    fun testBedtime_boundary23_00_scores1() {
        assertEquals(1, SadhanaScorer.scoreBedtime(LocalTime.of(23, 0)))
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
    fun testBedtime_earlyMorning04_00_scores5() {
        assertEquals(5, SadhanaScorer.scoreBedtime(LocalTime.of(4, 0)))
    }

    @Test
    fun testBedtime_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreBedtime(null))
    }

    // ---------------------------------------------------------
    // WAKE-UP BOUNDARY TESTS
    // before 03:00 -> 5
    // 03:00–03:30 -> 4
    // 03:31–04:00 -> 3
    // 04:01–04:15 -> 2
    // 04:16–04:30 -> 1
    // after 04:30 -> 0
    // ---------------------------------------------------------

    @Test
    fun testWakeUp_boundary02_59_scores5() {
        assertEquals(5, SadhanaScorer.scoreWakeUp(LocalTime.of(2, 59)))
    }

    @Test
    fun testWakeUp_boundary03_00_scores4() {
        assertEquals(4, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 0)))
    }

    @Test
    fun testWakeUp_boundary03_30_scores4() {
        assertEquals(4, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 30)))
    }

    @Test
    fun testWakeUp_boundary03_31_scores3() {
        assertEquals(3, SadhanaScorer.scoreWakeUp(LocalTime.of(3, 31)))
    }

    @Test
    fun testWakeUp_boundary04_00_scores3() {
        assertEquals(3, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 0)))
    }

    @Test
    fun testWakeUp_boundary04_01_scores2() {
        assertEquals(2, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 1)))
    }

    @Test
    fun testWakeUp_boundary04_15_scores2() {
        assertEquals(2, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 15)))
    }

    @Test
    fun testWakeUp_boundary04_16_scores1() {
        assertEquals(1, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 16)))
    }

    @Test
    fun testWakeUp_boundary04_30_scores1() {
        assertEquals(1, SadhanaScorer.scoreWakeUp(LocalTime.of(4, 30)))
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
    // DAY SLEEP / NAP BOUNDARY TESTS
    // effective = max(0, napMinutes - (if trainJourney 60 else 0))
    // effective <= 30 -> 5
    // 31–45 -> 4
    // 46–60 -> 2
    // > 60 -> 0
    // ---------------------------------------------------------

    @Test
    fun testDaySleep_noTrain_boundary30_scores5() {
        assertEquals(5, SadhanaScorer.scoreDaySleep(30, trainJourney = false))
    }

    @Test
    fun testDaySleep_noTrain_boundary31_scores4() {
        assertEquals(4, SadhanaScorer.scoreDaySleep(31, trainJourney = false))
    }

    @Test
    fun testDaySleep_noTrain_boundary45_scores4() {
        assertEquals(4, SadhanaScorer.scoreDaySleep(45, trainJourney = false))
    }

    @Test
    fun testDaySleep_noTrain_boundary46_scores2() {
        assertEquals(2, SadhanaScorer.scoreDaySleep(46, trainJourney = false))
    }

    @Test
    fun testDaySleep_noTrain_boundary60_scores2() {
        assertEquals(2, SadhanaScorer.scoreDaySleep(60, trainJourney = false))
    }

    @Test
    fun testDaySleep_noTrain_boundary61_scores0() {
        assertEquals(0, SadhanaScorer.scoreDaySleep(61, trainJourney = false))
    }

    @Test
    fun testDaySleep_withTrain_boundary90_scores5() {
        // 90 - 60 = 30 -> 5 pts
        assertEquals(5, SadhanaScorer.scoreDaySleep(90, trainJourney = true))
    }

    @Test
    fun testDaySleep_withTrain_boundary91_scores4() {
        // 91 - 60 = 31 -> 4 pts
        assertEquals(4, SadhanaScorer.scoreDaySleep(91, trainJourney = true))
    }

    @Test
    fun testDaySleep_withTrain_boundary105_scores4() {
        // 105 - 60 = 45 -> 4 pts
        assertEquals(4, SadhanaScorer.scoreDaySleep(105, trainJourney = true))
    }

    @Test
    fun testDaySleep_withTrain_boundary106_scores2() {
        // 106 - 60 = 46 -> 2 pts
        assertEquals(2, SadhanaScorer.scoreDaySleep(106, trainJourney = true))
    }

    @Test
    fun testDaySleep_withTrain_boundary120_scores2() {
        // 120 - 60 = 60 -> 2 pts
        assertEquals(2, SadhanaScorer.scoreDaySleep(120, trainJourney = true))
    }

    @Test
    fun testDaySleep_withTrain_boundary121_scores0() {
        // 121 - 60 = 61 -> 0 pts
        assertEquals(0, SadhanaScorer.scoreDaySleep(121, trainJourney = true))
    }

    @Test
    fun testDaySleep_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreDaySleep(null))
    }

    // ---------------------------------------------------------
    // JAPA (16 ROUNDS COMPLETED BY) BOUNDARY TESTS
    // <= 07:15 -> 5
    // 07:16–09:00 -> 4
    // 09:01–11:59 -> 3
    // 12:00–15:00 -> 2
    // 15:01–18:00 -> 1
    // after 18:00 -> 0
    // ---------------------------------------------------------

    @Test
    fun testJapa_boundary07_15_scores5() {
        assertEquals(5, SadhanaScorer.scoreJapa(LocalTime.of(7, 15)))
    }

    @Test
    fun testJapa_boundary07_16_scores4() {
        assertEquals(4, SadhanaScorer.scoreJapa(LocalTime.of(7, 16)))
    }

    @Test
    fun testJapa_boundary09_00_scores4() {
        assertEquals(4, SadhanaScorer.scoreJapa(LocalTime.of(9, 0)))
    }

    @Test
    fun testJapa_boundary09_01_scores3() {
        assertEquals(3, SadhanaScorer.scoreJapa(LocalTime.of(9, 1)))
    }

    @Test
    fun testJapa_boundary11_59_scores3() {
        assertEquals(3, SadhanaScorer.scoreJapa(LocalTime.of(11, 59)))
    }

    @Test
    fun testJapa_boundary12_00_scores2() {
        assertEquals(2, SadhanaScorer.scoreJapa(LocalTime.of(12, 0)))
    }

    @Test
    fun testJapa_boundary15_00_scores2() {
        assertEquals(2, SadhanaScorer.scoreJapa(LocalTime.of(15, 0)))
    }

    @Test
    fun testJapa_boundary15_01_scores1() {
        assertEquals(1, SadhanaScorer.scoreJapa(LocalTime.of(15, 1)))
    }

    @Test
    fun testJapa_boundary18_00_scores1() {
        assertEquals(1, SadhanaScorer.scoreJapa(LocalTime.of(18, 0)))
    }

    @Test
    fun testJapa_boundary18_01_scores0() {
        assertEquals(0, SadhanaScorer.scoreJapa(LocalTime.of(18, 1)))
    }

    @Test
    fun testJapa_nullReturnsNull() {
        assertNull(SadhanaScorer.scoreJapa(null))
    }

    // ---------------------------------------------------------
    // DAILY TOTAL & UNLOGGED TESTS
    // ---------------------------------------------------------

    @Test
    fun testDailyScore_allLogged_sampleMockupScores16() {
        // Bedtime 9:45 PM (21:45) -> 4
        // Wake-up 3:20 AM (03:20) -> 4
        // Nap 40 min without train -> 4
        // Japa 8:10 AM (08:10) -> 4
        // Total = 16 / 20
        val scores = SadhanaScorer.scoreDaily(
            bedtime = LocalTime.of(21, 45),
            wakeTime = LocalTime.of(3, 20),
            napMinutes = 40,
            trainJourney = false,
            japaTime = LocalTime.of(8, 10)
        )
        assertEquals(4, scores.bedScore)
        assertEquals(4, scores.wakeScore)
        assertEquals(4, scores.napScore)
        assertEquals(4, scores.japaScore)
        assertEquals(16, scores.totalScore)
    }

    @Test
    fun testDailyScore_maxScores20() {
        val scores = SadhanaScorer.scoreDaily(
            bedtime = LocalTime.of(21, 0),   // 5
            wakeTime = LocalTime.of(2, 45),  // 5
            napMinutes = 20,                 // 5
            trainJourney = false,
            japaTime = LocalTime.of(7, 0)    // 5
        )
        assertEquals(20, scores.totalScore)
    }

    @Test
    fun testDailyScore_allNullReturnsNullTotal() {
        val scores = SadhanaScorer.scoreDaily(
            bedtime = null,
            wakeTime = null,
            napMinutes = null,
            trainJourney = false,
            japaTime = null
        )
        assertNull(scores.bedScore)
        assertNull(scores.wakeScore)
        assertNull(scores.napScore)
        assertNull(scores.japaScore)
        assertNull(scores.totalScore)
    }

    @Test
    fun testDailyScore_partialLoggingSumsLoggedOnly() {
        val scores = SadhanaScorer.scoreDaily(
            bedtime = LocalTime.of(21, 15), // 5
            wakeTime = null,
            napMinutes = 30,                // 5
            trainJourney = false,
            japaTime = null
        )
        assertEquals(5, scores.bedScore)
        assertNull(scores.wakeScore)
        assertEquals(5, scores.napScore)
        assertNull(scores.japaScore)
        assertEquals(10, scores.totalScore)
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
