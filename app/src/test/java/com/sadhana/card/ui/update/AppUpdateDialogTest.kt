package com.sadhana.card.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateDialogTest {

    @Test
    fun testParseGithubChangelogWithMarkdownBullets() {
        val markdown = """
            ## Release v1.2.0
            - **New V5 Design**: Dark rings and cosmic update aesthetics
            * _Scoring fixes_: Updated bedtime and wake-up tiers
            • Performance optimizations for weekly heatmap
            - [Click here](https://github.com) for details
        """.trimIndent()

        val points = parseGithubChangelog(markdown)
        assertEquals(4, points.size)
        assertEquals("New V5 Design: Dark rings and cosmic update aesthetics", points[0])
        assertEquals("Scoring fixes: Updated bedtime and wake-up tiers", points[1])
        assertEquals("Performance optimizations for weekly heatmap", points[2])
        assertEquals("Click here for details", points[3])
    }

    @Test
    fun testParseGithubChangelogWithEmptyBodyUsesFallback() {
        val points = parseGithubChangelog("")
        assertEquals(3, points.size)
        assertTrue(points[0].contains("Daily Sadhana", ignoreCase = true))
        assertTrue(points[1].contains("Weekly Matrix", ignoreCase = true))
        assertTrue(points[2].contains("Offline data", ignoreCase = true))
    }

    @Test
    fun testParseGithubChangelogWithPlainTextCommaSeparated() {
        val text = "Category-based scoring, WhatsApp matrix sharing, Sanskrit dedication, and offline data stability."
        val points = parseGithubChangelog(text)
        assertTrue(points.size >= 3)
        assertEquals("Category-based scoring", points[0])
        assertEquals("WhatsApp matrix sharing", points[1])
        assertEquals("Sanskrit dedication", points[2])
    }
}
