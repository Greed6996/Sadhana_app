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
                sravanMinutes = 30
            )
        )

        // Monday
        fakeRepository.upsertEntry(
            SadhanaEntry(
                date = sunday.plusDays(1),
                bedtime = LocalTime.of(21, 45),
                wakeTime = LocalTime.of(3, 15),
                napMinutes = 40,
                japaTime = LocalTime.of(8, 0),
                pathanMinutes = 30,
                sravanMinutes = 60
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.columns.size)
        assertEquals(285, state.weekTotalScore) // 150 + 135
        assertEquals(90, state.totalPathanMinutes) // 60 + 30
        assertEquals(90, state.totalSravanMinutes) // 30 + 60
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

    @Test
    fun testWeeklyProgressPercentageAndColor() {
        // Calculation logic
        fun calc(current: Int, target: Int): Int =
            if (target > 0) ((current.toFloat() / target) * 100).toInt() else 0

        assertEquals(0, calc(0, 1400))
        assertEquals(7, calc(107, 1400))
        assertEquals(74, calc(1036, 1400))
        assertEquals(75, calc(1050, 1400))
        assertEquals(99, calc(1386, 1400))
        assertEquals(100, calc(1400, 1400))
        assertEquals(107, calc(1500, 1400))

        // Color thresholds
        val red = androidx.compose.ui.graphics.Color(0xFFFF5252)
        val yellow = androidx.compose.ui.graphics.Color(0xFFFFD54F)
        val green = androidx.compose.ui.graphics.Color(0xFF4CAF50)

        assertEquals(red, getWeeklyProgressColor(0))
        assertEquals(red, getWeeklyProgressColor(50))
        assertEquals(red, getWeeklyProgressColor(74))
        assertEquals(yellow, getWeeklyProgressColor(75))
        assertEquals(yellow, getWeeklyProgressColor(85))
        assertEquals(yellow, getWeeklyProgressColor(99))
        assertEquals(green, getWeeklyProgressColor(100))
        assertEquals(green, getWeeklyProgressColor(115))
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
