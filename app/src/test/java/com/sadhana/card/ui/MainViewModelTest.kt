package com.sadhana.card.ui

import com.sadhana.card.data.auth.AuthRepository
import com.sadhana.card.data.auth.AuthState
import com.sadhana.card.data.preferences.CURRENT_TERMS_VERSION
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun isTermsAccepted_emitsFalse_whenTermsNotAccepted() = runTest {
        fakeRepository.setInitialPreferences(
            UserPreferences(
                acceptedTerms = false,
                acceptedTermsVersion = 0
            )
        )
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse(viewModel.isTermsAccepted.value == true)
        assertEquals(false, viewModel.isTermsAccepted.value)
    }

    @Test
    fun isTermsAccepted_emitsFalse_whenAcceptedVersionIsOutdated() = runTest {
        // e.g. User accepted version 0, but current required version is 1
        fakeRepository.setInitialPreferences(
            UserPreferences(
                acceptedTerms = true,
                acceptedTermsVersion = CURRENT_TERMS_VERSION - 1
            )
        )
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(false, viewModel.isTermsAccepted.value)
    }

    @Test
    fun isTermsAccepted_emitsTrue_whenAcceptedAndCurrentVersion() = runTest {
        fakeRepository.setInitialPreferences(
            UserPreferences(
                acceptedTerms = true,
                acceptedTermsVersion = CURRENT_TERMS_VERSION
            )
        )
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(true, viewModel.isTermsAccepted.value)
    }

    @Test
    fun acceptTerms_writesCurrentVersionAndUpdatesState() = runTest {
        fakeRepository.setInitialPreferences(
            UserPreferences(
                acceptedTerms = false,
                acceptedTermsVersion = 0
            )
        )
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()
        assertEquals(false, viewModel.isTermsAccepted.value)

        viewModel.acceptTerms()
        advanceUntilIdle()

        val updatedPrefs = fakeRepository.userPreferencesFlow.first()
        assertTrue(updatedPrefs.acceptedTerms)
        assertEquals(CURRENT_TERMS_VERSION, updatedPrefs.acceptedTermsVersion)
        assertEquals(true, viewModel.isTermsAccepted.value)
    }

    @Test
    fun setThemeMode_persistsAndUpdatesState() = runTest {
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, fakeRepository.userPreferencesFlow.first().themeMode)
    }

    @Test
    fun saveProfile_persistsAndUpdatesState() = runTest {
        viewModel = MainViewModel(fakeRepository)
        advanceUntilIdle()
        assertEquals("", viewModel.sadhakaName.value)
        assertEquals("", viewModel.counselorName.value)

        viewModel.saveProfile("Radharani Dasi", "Jayapataka Swami")
        advanceUntilIdle()

        assertEquals("Radharani Dasi", viewModel.sadhakaName.value)
        assertEquals("Jayapataka Swami", viewModel.counselorName.value)
        assertEquals("Radharani Dasi", fakeRepository.userPreferencesFlow.first().sadhakaName)
        assertEquals("Jayapataka Swami", fakeRepository.userPreferencesFlow.first().counselorName)
    }

    @Test
    fun devotee_bypassesApproval_grantedAuthenticatedUiState_noCounselorDashboardAccess() = runTest {
        val fakeAuthRepo = FakeAuthRepository(
            initialState = AuthState.Authenticated(
                uid = "d1",
                email = "devotee@krishna.com",
                displayName = "Devotee Das",
                role = "DEVOTEE",
                isCounselorApproved = false // Devotees bypass approval
            )
        )
        viewModel = MainViewModel(
            userPreferencesRepository = fakeRepository,
            updateManager = null,
            authRepository = fakeAuthRepo,
            syncManager = null
        )
        advanceUntilIdle()

        val uiState = viewModel.authUiState.value
        assertTrue("Devotee must be directly Authenticated in UI", uiState is com.sadhana.card.ui.auth.AuthUiState.Authenticated)
        val authUi = uiState as com.sadhana.card.ui.auth.AuthUiState.Authenticated
        assertTrue("Devotee approval check must be bypassed", authUi.isApproved)
        assertEquals("DEVOTEE", authUi.role)

        assertFalse("Devotee cannot access counselor dashboard", viewModel.isCounselorDashboardAccessible.value)
        assertFalse("Devotee cannot access counselor dashboard", viewModel.canAccessCounselorDashboard())
    }

    @Test
    fun counselor_dashboardAccess_grantedIfAndOnlyIfApproved() = runTest {
        // 1. Unapproved counselor: cannot access dashboard, routed to pending approval
        val fakeAuthRepo = FakeAuthRepository(
            initialState = AuthState.Authenticated(
                uid = "c1",
                email = "counselor@krishna.com",
                displayName = "Counselor Prabhu",
                role = "COUNSELOR",
                isCounselorApproved = false
            )
        )
        viewModel = MainViewModel(
            userPreferencesRepository = fakeRepository,
            updateManager = null,
            authRepository = fakeAuthRepo,
            syncManager = null
        )
        advanceUntilIdle()

        assertTrue("Unapproved counselor must be in PendingApproval UI state", viewModel.authUiState.value is com.sadhana.card.ui.auth.AuthUiState.PendingApproval)
        assertFalse("Unapproved counselor cannot access counselor dashboard", viewModel.isCounselorDashboardAccessible.value)
        assertFalse("Unapproved counselor cannot access counselor dashboard", viewModel.canAccessCounselorDashboard())

        // 2. Approved counselor: granted dashboard access
        fakeAuthRepo.setAuthState(
            AuthState.Authenticated(
                uid = "c1",
                email = "counselor@krishna.com",
                displayName = "Counselor Prabhu",
                role = "COUNSELOR",
                isCounselorApproved = true
            )
        )
        advanceUntilIdle()

        val uiState = viewModel.authUiState.value
        assertTrue("Approved counselor must be Authenticated", uiState is com.sadhana.card.ui.auth.AuthUiState.Authenticated)
        assertTrue("Approved counselor must have isApproved == true", (uiState as com.sadhana.card.ui.auth.AuthUiState.Authenticated).isApproved)
        assertTrue("Approved counselor MUST access counselor dashboard", viewModel.isCounselorDashboardAccessible.value)
        assertTrue("Approved counselor MUST access counselor dashboard", viewModel.canAccessCounselorDashboard())
    }

    private class FakeAuthRepository(initialState: AuthState = AuthState.Unauthenticated) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(initialState)
        override val authState: kotlinx.coroutines.flow.StateFlow<AuthState> = _authState

        fun setAuthState(state: AuthState) {
            _authState.value = state
        }

        override suspend fun signInWithGoogleCredential(idToken: String, requestedRole: String): Result<AuthState> =
            Result.success(_authState.value)

        override suspend fun checkCounselorApproval(uid: String): Boolean = true
        override suspend fun refreshAuthStatus(): AuthState = _authState.value
        override suspend fun claimLocalRecords(googleUid: String) {}
        override suspend fun getLocalRecordCount(): Int = 0
        override suspend fun setHistoricalSyncPrompted(prompted: Boolean) {}
        override suspend fun signOut() {
            _authState.value = AuthState.Unauthenticated
        }
        override suspend fun syncProfileToCloud(sadhakaName: String, counselorName: String) {}
        override fun getWebClientId(): String? = "fake_client_id"
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val prefs = MutableStateFlow(UserPreferences())

        fun setInitialPreferences(initial: UserPreferences) {
            prefs.value = initial
        }

        override val userPreferencesFlow: Flow<UserPreferences> = prefs

        override suspend fun updateSadhakaName(name: String) {
            prefs.value = prefs.value.copy(sadhakaName = name)
        }

        override suspend fun updateCounselorName(name: String) {
            prefs.value = prefs.value.copy(counselorName = name)
        }

        override suspend fun setDedicationExpanded(expanded: Boolean) {
            prefs.value = prefs.value.copy(isDedicationExpanded = expanded)
        }

        override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {
            prefs.value = prefs.value.copy(
                acceptedTerms = accepted,
                acceptedTermsVersion = version
            )
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            prefs.value = prefs.value.copy(themeMode = themeMode)
        }

        override suspend fun setDismissedUpdateVersion(version: Int) {
            recordDismissedUpdate(version, System.currentTimeMillis())
        }

        override suspend fun recordDismissedUpdate(versionCode: Int, timestamp: Long) {
            prefs.value = prefs.value.copy(
                dismissedUpdateVersion = versionCode,
                lastDismissedVersionCode = versionCode,
                lastDismissedTimestamp = timestamp
            )
        }
    }
}
