package com.sadhana.card.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyEntryViewModel @Inject constructor(
    private val sadhanaRepository: SadhanaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())

    // Stream entry from repository whenever selected date changes
    private val currentEntryFlow = selectedDateFlow.flatMapLatest { date ->
        sadhanaRepository.getEntry(date)
    }

    val uiState: StateFlow<DailyEntryUiState> = combine(
        selectedDateFlow,
        currentEntryFlow,
        userPreferencesRepository.userPreferencesFlow
    ) { date, repositoryEntry, preferences ->
        val entry = repositoryEntry ?: SadhanaEntry(date = date)
        val score = SadhanaScorer.scoreEntry(entry)
        DailyEntryUiState(
            selectedDate = date,
            startOfWeek = DateUtils.getStartOfWeek(date),
            entry = entry,
            score = score,
            sadhakaName = preferences.sadhakaName,
            counselorName = preferences.counselorName,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyEntryUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun previousWeek() {
        selectedDateFlow.value = selectedDateFlow.value.minusWeeks(1)
    }

    fun nextWeek() {
        selectedDateFlow.value = selectedDateFlow.value.plusWeeks(1)
    }

    fun goToToday() {
        selectedDateFlow.value = LocalDate.now()
    }

    fun updateBedtime(time: LocalTime?) {
        updateCurrentEntry { it.copy(bedtime = time) }
    }

    fun updateWakeTime(time: LocalTime?) {
        updateCurrentEntry { it.copy(wakeTime = time) }
    }

    fun updateNapMinutes(minutes: Int?) {
        updateCurrentEntry { it.copy(napMinutes = minutes) }
    }

    fun updateTrainJourney(enabled: Boolean) {
        updateCurrentEntry { it.copy(trainJourney = enabled) }
    }

    fun updateJapaTime(time: LocalTime?) {
        updateCurrentEntry { it.copy(japaTime = time) }
    }

    fun updatePathanMinutes(minutes: Int?) {
        updateCurrentEntry { it.copy(pathanMinutes = minutes ?: 0) }
    }

    fun updateSravanMinutes(minutes: Int?) {
        updateCurrentEntry { it.copy(sravanMinutes = minutes ?: 0) }
    }

    fun updateSevaMinutes(minutes: Int?) {
        updateCurrentEntry { it.copy(sevaMinutes = minutes ?: 0) }
    }

    fun updateSevaTags(tags: List<String>) {
        updateCurrentEntry { it.copy(sevaTags = tags) }
    }

    fun updateSadhakaName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateSadhakaName(name)
        }
    }

    fun updateCounselorName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateCounselorName(name)
        }
    }

    private fun updateCurrentEntry(transform: (SadhanaEntry) -> SadhanaEntry) {
        val current = uiState.value.entry
        val updated = transform(current).copy(
            date = selectedDateFlow.value,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            sadhanaRepository.upsertEntry(updated)
        }
    }
}
