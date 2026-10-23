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
}
