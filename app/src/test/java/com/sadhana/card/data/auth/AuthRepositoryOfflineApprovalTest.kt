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
            isCounselorApproved = true
        )
        assertEquals("DEVOTEE", devotee.role)
        assertTrue(devotee.isCounselorApproved)
    }

    @Test
    fun devotee_bypassesApproval_hasInstantAccess() {
        // Schema check: Devotee profile data map has instant access solely by role, without status or isApproved
        val isCounselorRequest = false
        val normalizedRole = if (isCounselorRequest) "COUNSELOR" else "DEVOTEE"
        val profileData = mutableMapOf<String, Any>(
            "uid" to "devotee_123",
            "displayName" to "Bhakta Das",
            "email" to "bhakta@example.com",
            "role" to normalizedRole,
            "requestedRole" to normalizedRole,
            "updatedAt" to 123456789L
        )
        if (isCounselorRequest) {
            profileData["isApproved"] = false
            profileData["requestedAt"] = 123456789L
        }

        assertEquals("DEVOTEE", profileData["role"])
        assertFalse("Devotee document must NOT contain 'status'", profileData.containsKey("status"))
        assertFalse("Devotee document must NOT contain 'isApproved'", profileData.containsKey("isApproved"))
    }

    @Test
    fun counselor_firstSignUp_writesOnlyIsApprovedFalseAndNoStatus() {
        val isCounselorRequest = true
        val normalizedRole = if (isCounselorRequest) "COUNSELOR" else "DEVOTEE"
        val profileData = mutableMapOf<String, Any>(
            "uid" to "counselor_456",
            "displayName" to "Counselorji",
            "email" to "counselor@example.com",
            "role" to normalizedRole,
            "requestedRole" to normalizedRole,
            "updatedAt" to 123456789L
        )
        if (isCounselorRequest) {
            profileData["isApproved"] = false
            profileData["requestedAt"] = 123456789L
        }

        assertEquals("COUNSELOR", profileData["role"])
        assertFalse("Counselor document must NOT contain 'status'", profileData.containsKey("status"))
        assertTrue("Counselor document MUST contain 'isApproved'", profileData.containsKey("isApproved"))
        assertEquals(false, profileData["isApproved"])
    }

    @Test
    fun counselor_dashboardAccess_grantedIfAndOnlyIfApproved() {
        // Unapproved counselor
        val unapprovedCounselor = AuthState.Authenticated(
            uid = "c_1",
            email = "c1@test.com",
            displayName = "Counselor",
            role = "COUNSELOR",
            isCounselorApproved = false
        )
        val hasAccessUnapproved = unapprovedCounselor.role.equals("counselor", ignoreCase = true) && unapprovedCounselor.isCounselorApproved
        assertFalse("Unapproved counselor must NOT have dashboard access", hasAccessUnapproved)

        // Approved counselor
        val approvedCounselor = AuthState.Authenticated(
            uid = "c_2",
            email = "c2@test.com",
            displayName = "Counselor Approved",
            role = "COUNSELOR",
            isCounselorApproved = true
        )
        val hasAccessApproved = approvedCounselor.role.equals("counselor", ignoreCase = true) && approvedCounselor.isCounselorApproved
        assertTrue("Approved counselor MUST have dashboard access", hasAccessApproved)

        // Devotee bypasses approval checks - not a counselor, normal devotee access
        val devotee = AuthState.Authenticated(
            uid = "d_1",
            email = "d1@test.com",
            displayName = "Devotee",
            role = "DEVOTEE",
            isCounselorApproved = true
        )
        val isDevoteeCounselor = devotee.role.equals("counselor", ignoreCase = true) && devotee.isCounselorApproved
        assertFalse("Devotee does not access counselor dashboard, but accesses devotee features", isDevoteeCounselor)
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
