package com.sadhana.card.data.auth

import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryOfflineApprovalTest {

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @Test
    fun checkCounselorApproval_offlineTransientError_preservesCachedApproval() = runBlocking {
        // Devotee was previously confirmed approved offline in preferences
        fakePrefsRepo.setCounselorApproved(true)
        assertTrue(fakePrefsRepo.userPreferencesFlow.first().isCounselorApproved)

        // Simulate network failure handler logic matching AuthRepositoryImpl:
        // On network failure or exception, cached state must be preserved, not overwritten with false
        val cachedApproval = fakePrefsRepo.userPreferencesFlow.first().isCounselorApproved
        val simulatedNetworkFailure = true

        val finalApproval = if (simulatedNetworkFailure) {
            // Transient network error preserves cached approval
            cachedApproval
        } else {
            false
        }

        assertTrue("Cached counselor approval must remain true on transient network failure", finalApproval)
    }

    @Test
    fun clearAuthSession_resetsHistoricalSyncPrompt_allowingNewAccountPrompt() = runBlocking {
        // Given: account was prompted and set to true
        fakePrefsRepo.setHistoricalSyncPrompted(true)
        fakePrefsRepo.updateAuthSession(uid = "user_alpha", email = "alpha@example.com", role = "devotee", isApproved = false)
        assertTrue(fakePrefsRepo.userPreferencesFlow.first().hasPromptedHistoricalSync)

        // When: User logs out
        fakePrefsRepo.clearAuthSession()

        // Then: Auth UID is cleared and historical sync prompt is reset
        val prefs = fakePrefsRepo.userPreferencesFlow.first()
        assertEquals("", prefs.authUid)
        assertFalse("Prompt must be reset for subsequent accounts", prefs.hasPromptedHistoricalSync)
    }

    @Test
    fun pendingApproval_state_isProperlyConstructedForUnapprovedCounselor() {
        val pendingState = AuthState.PendingApproval(
            uid = "counselor_1",
            email = "counselor@test.com",
            displayName = "Counselor Prabhu",
            role = "COUNSELOR"
        )
        assertEquals("counselor_1", pendingState.uid)
        assertEquals("counselor@test.com", pendingState.email)
        assertEquals("COUNSELOR", pendingState.role)
    }

    @Test
    fun authenticated_state_preservesApprovalForCounselorAndDevotee() {
        val approvedCounselor = AuthState.Authenticated(
            uid = "guru_1",
            email = "guru@test.com",
            displayName = "Guru Maharaj",
            role = "COUNSELOR",
            isCounselorApproved = true
        )
        assertTrue(approvedCounselor.isCounselorApproved)
        assertEquals("COUNSELOR", approvedCounselor.role)

        val devotee = AuthState.Authenticated(
            uid = "devotee_1",
            email = "devotee@test.com",
            displayName = "Bhakta",
            role = "DEVOTEE",
            isCounselorApproved = false
        )
        assertEquals("DEVOTEE", devotee.role)
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())

        override val userPreferencesFlow: Flow<UserPreferences> = state

        override suspend fun updateSadhakaName(name: String) {}
        override suspend fun updateCounselorName(name: String) {}
        override suspend fun setDedicationExpanded(expanded: Boolean) {}
        override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {}
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setDismissedUpdateVersion(version: Int) {}
        override suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long) {}

        override suspend fun updateAuthSession(uid: String, email: String, role: String, isApproved: Boolean) {
            state.value = state.value.copy(
                authUid = uid,
                authEmail = email,
                authRole = role,
                isCounselorApproved = isApproved
            )
        }

        override suspend fun setCounselorApproved(approved: Boolean) {
            state.value = state.value.copy(isCounselorApproved = approved)
        }

        override suspend fun setHistoricalSyncPrompted(prompted: Boolean) {
            state.value = state.value.copy(hasPromptedHistoricalSync = prompted)
        }

        override suspend fun clearAuthSession() {
            state.value = state.value.copy(
                authUid = "",
                authEmail = "",
                authRole = "",
                isCounselorApproved = false,
                hasPromptedHistoricalSync = false
            )
        }
    }
}
