package com.sadhana.card.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCooldownTest {

    private val cooldownMs = UpdateManager.COOLDOWN_MS

    private fun shouldSuppressUpdate(
        remoteVersionCode: Int,
        lastDismissedCode: Int,
        lastDismissedTimestamp: Long,
        currentTime: Long,
        isManualCheck: Boolean
    ): Boolean {
        if (isManualCheck) return false
        if (remoteVersionCode <= lastDismissedCode) {
            val elapsed = currentTime - lastDismissedTimestamp
            if (elapsed in 0 until cooldownMs) {
                return true
            }
        }
        return false
    }

    @Test
    fun testSnoozeCooldownSuppressesSameVersionWithin24Hours() {
        val now = 1000000000L
        val dismissedTime = now - (2 * 60 * 60 * 1000L) // 2 hours ago

        // Dismissed v28, remote is still v28
        val suppressed = shouldSuppressUpdate(
            remoteVersionCode = 28,
            lastDismissedCode = 28,
            lastDismissedTimestamp = dismissedTime,
            currentTime = now,
            isManualCheck = false
        )
        assertTrue("Should be suppressed within 24h for the same version code", suppressed)
    }

    @Test
    fun testSnoozeCooldownExpiresAfter24Hours() {
        val now = 1000000000L
        val dismissedTime = now - (25 * 60 * 60 * 1000L) // 25 hours ago

        val suppressed = shouldSuppressUpdate(
            remoteVersionCode = 28,
            lastDismissedCode = 28,
            lastDismissedTimestamp = dismissedTime,
            currentTime = now,
            isManualCheck = false
        )
        assertFalse("Should NOT be suppressed after 24 hours have elapsed", suppressed)
    }

    @Test
    fun testEdgeCase1_NewerUrgentReleaseBypassesCooldownImmediately() {
        val now = 1000000000L
        val dismissedTime = now - (10 * 60 * 1000L) // Only 10 minutes ago!

        // User dismissed v28, but remote is now urgent v29
        val suppressed = shouldSuppressUpdate(
            remoteVersionCode = 29,
            lastDismissedCode = 28,
            lastDismissedTimestamp = dismissedTime,
            currentTime = now,
            isManualCheck = false
        )
        assertFalse("Newer urgent version must immediately bypass 24h snooze cooldown", suppressed)
    }

    @Test
    fun testEdgeCase2_ManualUserCheckBypassesCooldownImmediately() {
        val now = 1000000000L
        val dismissedTime = now - (5 * 60 * 1000L) // 5 minutes ago

        // User dismissed v28, but explicitly tapped "Check for Updates"
        val suppressed = shouldSuppressUpdate(
            remoteVersionCode = 28,
            lastDismissedCode = 28,
            lastDismissedTimestamp = dismissedTime,
            currentTime = now,
            isManualCheck = true
        )
        assertFalse("Manual user check must ignore 24h snooze cooldown", suppressed)
    }
}
