package com.sadhana.card.ui

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
            prefs.value = prefs.value.copy(dismissedUpdateVersion = version)
        }
    }
}
