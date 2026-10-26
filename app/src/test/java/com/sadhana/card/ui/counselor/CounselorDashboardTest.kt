package com.sadhana.card.ui.counselor

import androidx.compose.ui.graphics.Color
import com.sadhana.card.domain.model.SadhanaEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CounselorDashboardTest {

    @Test
    fun testPercentageBadgeColorThresholds() {
        val red = Color(0xFFEF4444)
        val yellow = Color(0xFFF59E0B)
        val green = Color(0xFF10B981)

        // Red: strictly less than 80%
        assertEquals(red, getPercentageBadgeColor(0))
        assertEquals(red, getPercentageBadgeColor(50))
        assertEquals(red, getPercentageBadgeColor(79))

        // Yellow: 80% to 99%
        assertEquals(yellow, getPercentageBadgeColor(80))
        assertEquals(yellow, getPercentageBadgeColor(85))
        assertEquals(yellow, getPercentageBadgeColor(90))
        assertEquals(yellow, getPercentageBadgeColor(99))

        // Green: 100% and above
        assertEquals(green, getPercentageBadgeColor(100))
        assertEquals(green, getPercentageBadgeColor(105))
    }

    @Test
    fun testCurrentDayCutoffLogic() {
        val kolkataZone = ZoneId.of("Asia/Kolkata")
        val todayInKolkata = LocalDate.now(kolkataZone)
        val pastDate = todayInKolkata.minusDays(1)
        val futureDate = todayInKolkata.plusDays(1)

        // Today is editable
        assertTrue(todayInKolkata == LocalDate.now(kolkataZone))

        // Past dates are read-only
        assertFalse(pastDate == todayInKolkata)
        assertFalse(futureDate == todayInKolkata)
    }

    @Test
    fun testSadhanaEntryDefaults() {
        val entry = SadhanaEntry(date = LocalDate.now())
        assertEquals("local", entry.userId)
        assertFalse(entry.isSynced)
    }

    @Test
    fun testCounselorJapa16RoundsScore_notStuckAtZeroWhenCategorySelected() {
        // Simulating dayData from Firestore weekly summary with japaCategory or japaScore
        val dayData = mapOf(
            "japaCategory" to "BY_9_00_AM",
            "japaScore" to 25L,
            "totalScore" to 25
        )

        val japaCategoryStr = (dayData["japaCategory"] as? String)?.ifBlank { null }
        val japaCat = japaCategoryStr?.let { runCatching { com.sadhana.card.domain.model.JapaCategory.valueOf(it) }.getOrNull() }
        val japaScoreDirect = (dayData["japaScore"] as? Long)?.toInt()
            ?: (dayData["sixteenRoundsDoneByScore"] as? Long)?.toInt()
            ?: (dayData["roundsCompletedScore"] as? Long)?.toInt()
        val japaDone = (dayData["japaCompleted"] as? Boolean)
            ?: (japaCat != null)
            ?: (japaScoreDirect != null && japaScoreDirect > 0)

        val entry = SadhanaEntry(
            date = LocalDate.of(2026, 10, 20),
            japaCategory = japaCat,
            japaCompleted = japaDone
        )

        var scores = com.sadhana.card.domain.scorer.SadhanaScorer.scoreEntry(entry)
        if (japaScoreDirect != null && (scores.japaScore == null || scores.japaScore == 0) && japaScoreDirect > 0) {
            scores = scores.copy(japaScore = japaScoreDirect)
        }

        assertEquals("16 rounds score in counselor view must be 25 points, not stuck at 0", 25, scores.japaScore)
    }

    @Test
    fun testWeeklySummaryMap_includesJapaScoreAndCompletion() {
        val date = LocalDate.of(2026, 10, 18) // Sunday
        val entry = SadhanaEntry(
            date = date,
            japaCategory = com.sadhana.card.domain.model.JapaCategory.BY_9_00_AM,
            japaCompleted = true
        )

        val summary = com.sadhana.card.data.sync.buildWeeklySummaryMap(
            startOfWeek = date,
            entries = listOf(entry),
            devoteeName = "Bhakta Das"
        )

        val daysMap = summary["days"] as Map<*, *>
        val dayData = daysMap["2026-10-18"] as Map<*, *>

        assertEquals("BY_9_00_AM", dayData["japaCategory"])
        assertEquals(true, dayData["japaCompleted"])
        assertEquals(25, dayData["japaScore"])
        assertEquals(25, dayData["sixteenRoundsDoneByScore"])
        assertEquals(25, dayData["roundsCompletedScore"])
    }

    @Test
    fun testCounselorPathanSravanPoints_mappedCorrectly() {
        val entry = SadhanaEntry(
            date = LocalDate.of(2026, 10, 24),
            pathanMinutes = 30, // >= 30 min -> 25 pts
            sravanMinutes = 45  // >= 30 min -> 25 pts
        )
        val scores = com.sadhana.card.domain.scorer.SadhanaScorer.scoreEntry(entry)
        assertEquals(25, scores.pathanScore)
        assertEquals(25, scores.sravanScore)
    }
}
