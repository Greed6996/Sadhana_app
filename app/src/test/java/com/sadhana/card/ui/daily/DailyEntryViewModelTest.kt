package com.sadhana.card.ui.daily

import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DailyEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSadhanaRepository
    private lateinit var fakePreferences: FakeUserPreferencesRepository
    private lateinit var viewModel: DailyEntryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSadhanaRepository()
        fakePreferences = FakeUserPreferencesRepository()
        viewModel = DailyEntryViewModel(fakeRepository, fakePreferences, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_loadsTodayAndDefaultScore() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocalDate.now(), state.selectedDate)
        assertNotNull(state.score)
    }

    @Test
    fun testUpdateBedtime_savesToRepositoryAndUpdatesScore() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val date = LocalDate.of(2026, 10, 11)
        viewModel.selectDate(date)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateBedtime(LocalTime.of(21, 15)) // <= 21:30 -> 5 pts
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeRepository.getEntry(date).first()
        assertNotNull(saved)
        assertEquals(LocalTime.of(21, 15), saved?.bedtime)

        val state = viewModel.uiState.value
        assertEquals(5, state.score.bedScore)
    }

    @Test
    fun testTrainJourney_adjustsNapScore() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val date = LocalDate.of(2026, 10, 11)
        viewModel.selectDate(date)
        testDispatcher.scheduler.advanceUntilIdle()

        // 75 min nap without train journey -> > 60 min -> 0 pts
        viewModel.updateNapMinutes(75)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.score.napScore)

        // Turn on train journey -> effective = 75 - 60 = 15 min -> <= 30 min -> 5 pts!
        viewModel.updateTrainJourney(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.score.napScore)
    }

    @Test
    fun testUpdateNames_persistsToPreferences() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSadhakaName("Arjuna Dasa")
        viewModel.updateCounselorName("Bhima Dasa")
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        val prefs = fakePreferences.userPreferencesFlow.first()
        assertEquals("Arjuna Dasa", prefs.sadhakaName)
        assertEquals("Bhima Dasa", prefs.counselorName)
    }

    @Test
    fun testAcceptTerms_updatesPreferences() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.hasAcceptedTerms)

        viewModel.acceptTerms()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasAcceptedTerms)
        assertEquals(true, fakePreferences.userPreferencesFlow.first().hasAcceptedTerms)
    }

    // --- Fakes ---

    private class FakeSadhanaRepository : SadhanaRepository {
        val storage = mutableMapOf<LocalDate, SadhanaEntry>()
        private val state = MutableStateFlow<Map<LocalDate, SadhanaEntry>>(emptyMap())

        override fun getEntry(date: LocalDate): Flow<SadhanaEntry?> {
            return state.map { it[date] }
        }

        override fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SadhanaEntry>> {
            return state.map { map ->
                map.values.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
                    .sortedBy { it.date }
            }
        }

        override fun getEntriesForWeek(startOfWeek: LocalDate): Flow<List<SadhanaEntry>> {
            val endOfWeek = startOfWeek.plusDays(6)
            return getEntriesForRange(startOfWeek, endOfWeek)
        }

        override suspend fun upsertEntry(entry: SadhanaEntry) {
            storage[entry.date] = entry
            state.value = storage.toMap()
        }

        override suspend fun deleteEntry(date: LocalDate) {
            storage.remove(date)
            state.value = storage.toMap()
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val prefsFlow = MutableStateFlow(
            UserPreferences(
                sadhakaName = "Bhaktin Sarah",
                counselorName = "Govinda Dasa",
                acceptedTerms = false,
                acceptedTermsVersion = 0
            )
        )

        override val userPreferencesFlow: Flow<UserPreferences> = prefsFlow

        override suspend fun updateSadhakaName(name: String) {
            prefsFlow.value = prefsFlow.value.copy(sadhakaName = name)
        }

        override suspend fun updateCounselorName(name: String) {
            prefsFlow.value = prefsFlow.value.copy(counselorName = name)
        }

        override suspend fun setDedicationExpanded(expanded: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(isDedicationExpanded = expanded)
        }

        override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {
            prefsFlow.value = prefsFlow.value.copy(
                acceptedTerms = accepted,
                acceptedTermsVersion = version
            )
        }

        override suspend fun setThemeMode(themeMode: com.sadhana.card.data.preferences.ThemeMode) {
            prefsFlow.value = prefsFlow.value.copy(themeMode = themeMode)
        }

        override suspend fun setDismissedUpdateVersion(version: Int) {
            prefsFlow.value = prefsFlow.value.copy(dismissedUpdateVersion = version)
        }
    }
}
