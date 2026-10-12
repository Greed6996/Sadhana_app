package com.sadhana.card.ui.weekly

import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import com.sadhana.card.domain.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyMatrixViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSadhanaRepository
    private lateinit var viewModel: WeeklyMatrixViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSadhanaRepository()
        viewModel = WeeklyMatrixViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testWeeklyAggregateCalculation() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val sunday = LocalDate.of(2026, 10, 11) // Sunday
        viewModel.setWeek(sunday)
        testDispatcher.scheduler.advanceUntilIdle()

        // Sunday: 5 bed, 5 wake, 5 nap, 5 japa -> 20 pts, 60 min pathan, 30 min sravan
        fakeRepository.upsertEntry(
            SadhanaEntry(
                date = sunday,
                bedtime = LocalTime.of(21, 15),
                wakeTime = LocalTime.of(2, 45),
                napMinutes = 20,
                japaTime = LocalTime.of(7, 0),
                pathanMinutes = 60,
                sravanMinutes = 30,
                sevaMinutes = 45
            )
        )

        // Monday: 4 bed, 4 wake, 4 nap, 4 japa -> 16 pts, 30 min pathan, 60 min sravan
        fakeRepository.upsertEntry(
            SadhanaEntry(
                date = sunday.plusDays(1),
                bedtime = LocalTime.of(21, 45),
                wakeTime = LocalTime.of(3, 15),
                napMinutes = 40,
                japaTime = LocalTime.of(8, 0),
                pathanMinutes = 30,
                sravanMinutes = 60,
                sevaMinutes = 15
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.columns.size)
        assertEquals(36, state.weekTotalScore) // 20 + 16
        assertEquals(90, state.totalPathanMinutes) // 60 + 30
        assertEquals(90, state.totalSravanMinutes) // 30 + 60
        assertEquals(60, state.totalSevaMinutes) // 45 + 15
        assertEquals("1h 30m", state.pathanHoursMinutes)
        assertEquals("1h 30m", state.sravanHoursMinutes)
        assertEquals("3h 0m", state.bothHoursMinutes)
    }

    @Test
    fun testWeekNavigation() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val initialStart = viewModel.uiState.value.startOfWeek
        viewModel.nextWeek()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialStart.plusWeeks(1), viewModel.uiState.value.startOfWeek)

        viewModel.previousWeek()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialStart, viewModel.uiState.value.startOfWeek)
    }

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
}
