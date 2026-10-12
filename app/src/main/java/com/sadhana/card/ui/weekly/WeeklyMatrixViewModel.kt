package com.sadhana.card.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyMatrixViewModel @Inject constructor(
    private val sadhanaRepository: SadhanaRepository
) : ViewModel() {

    private val startOfWeekFlow = MutableStateFlow(DateUtils.getStartOfWeek(LocalDate.now()))

    val uiState: StateFlow<WeeklyMatrixUiState> = startOfWeekFlow.flatMapLatest { startOfWeek ->
        sadhanaRepository.getEntriesForWeek(startOfWeek).map { entries ->
            val entryMap = entries.associateBy { it.date }
            val weekDates = DateUtils.getWeekSundayStart(startOfWeek)

            var weekTotal = 0
            var pathanTotal = 0
            var sravanTotal = 0
            var sevaTotal = 0

            val columns = weekDates.map { date ->
                val entry = entryMap[date]
                val scores = if (entry != null) {
                    SadhanaScorer.scoreEntry(entry)
                } else {
                    SadhanaScorer.DailyScores(null, null, null, null, null)
                }

                if (entry != null) {
                    pathanTotal += entry.pathanMinutes
                    sravanTotal += entry.sravanMinutes
                    sevaTotal += entry.sevaMinutes
                }

                val dayTotal = scores.totalScore
                if (dayTotal != null) {
                    weekTotal += dayTotal
                }

                MatrixDayColumn(
                    date = date,
                    dayName = DateUtils.formatDayName(date),
                    dayNumber = DateUtils.formatDayNumber(date),
                    entry = entry,
                    scores = scores,
                    totalScore = scores.totalScore
                )
            }

            WeeklyMatrixUiState(
                startOfWeek = startOfWeek,
                columns = columns,
                weekTotalScore = weekTotal,
                totalPathanMinutes = pathanTotal,
                totalSravanMinutes = sravanTotal,
                totalSevaMinutes = sevaTotal,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeeklyMatrixUiState()
    )

    fun previousWeek() {
        startOfWeekFlow.value = startOfWeekFlow.value.minusWeeks(1)
    }

    fun nextWeek() {
        startOfWeekFlow.value = startOfWeekFlow.value.plusWeeks(1)
    }

    fun goToToday() {
        startOfWeekFlow.value = DateUtils.getStartOfWeek(LocalDate.now())
    }

    fun setWeek(date: LocalDate) {
        startOfWeekFlow.value = DateUtils.getStartOfWeek(date)
    }
}
