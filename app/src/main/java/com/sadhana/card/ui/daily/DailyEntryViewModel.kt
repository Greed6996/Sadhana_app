package com.sadhana.card.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.data.preferences.CURRENT_TERMS_VERSION
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import com.sadhana.card.domain.repository.SadhanaRepository
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DailyEntryViewModel internal constructor(
    private val sadhanaRepository: SadhanaRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        sadhanaRepository: SadhanaRepository,
        userPreferencesRepository: UserPreferencesRepository
    ) : this(sadhanaRepository, userPreferencesRepository, Dispatchers.IO)

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val pendingEntryFlow = MutableStateFlow<SadhanaEntry?>(null)

    // Debounced autosave channels (300 ms off main thread)
    private val debouncedEntrySaveFlow = MutableSharedFlow<SadhanaEntry>(extraBufferCapacity = 64)
    private val debouncedSadhakaNameFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val debouncedCounselorNameFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    // Local in-memory names for instant typing feedback
    private val inMemorySadhakaName = MutableStateFlow<String?>(null)
    private val inMemoryCounselorName = MutableStateFlow<String?>(null)

    init {
        // Autosave entry to Room on ioDispatcher debounced by 300ms
        viewModelScope.launch(ioDispatcher) {
            debouncedEntrySaveFlow
                .debounce(300)
                .collectLatest { entry ->
                    sadhanaRepository.upsertEntry(entry)
                }
        }

        // Autosave sadhaka name to DataStore on ioDispatcher debounced by 300ms
        viewModelScope.launch(ioDispatcher) {
            debouncedSadhakaNameFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { name ->
                    userPreferencesRepository.updateSadhakaName(name)
                }
        }

        // Autosave counselor name to DataStore on ioDispatcher debounced by 300ms
        viewModelScope.launch(ioDispatcher) {
            debouncedCounselorNameFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { name ->
                    userPreferencesRepository.updateCounselorName(name)
                }
        }
    }

    // Stream entry from repository whenever selected date changes
    private val currentEntryFlow = selectedDateFlow.flatMapLatest { date ->
        sadhanaRepository.getEntry(date)
    }

    val uiState: StateFlow<DailyEntryUiState> = combine(
        selectedDateFlow,
        currentEntryFlow,
        pendingEntryFlow,
        userPreferencesRepository.userPreferencesFlow,
        inMemorySadhakaName,
        inMemoryCounselorName
    ) { args ->
        val date = args[0] as LocalDate
        val repositoryEntry = args[1] as? SadhanaEntry
        val pendingEntry = args[2] as? SadhanaEntry
        val preferences = args[3] as com.sadhana.card.data.preferences.UserPreferences
        val localSadhaka = args[4] as? String
        val localCounselor = args[5] as? String

        val activeEntry = if (pendingEntry != null && pendingEntry.date == date) {
            pendingEntry
        } else {
            repositoryEntry ?: SadhanaEntry(date = date)
        }

        val score = SadhanaScorer.scoreEntry(activeEntry)

        DailyEntryUiState(
            selectedDate = date,
            startOfWeek = DateUtils.getStartOfWeek(date),
            entry = activeEntry,
            score = score,
            sadhakaName = localSadhaka ?: preferences.sadhakaName,
            counselorName = localCounselor ?: preferences.counselorName,
            hasAcceptedTerms = preferences.hasAcceptedTerms,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyEntryUiState()
    )

    fun acceptTerms() {
        viewModelScope.launch {
            userPreferencesRepository.setTermsAccepted(true, CURRENT_TERMS_VERSION)
        }
    }

    fun selectDate(date: LocalDate) {
        pendingEntryFlow.value = null
        selectedDateFlow.value = date
    }

    fun previousWeek() {
        pendingEntryFlow.value = null
        selectedDateFlow.value = selectedDateFlow.value.minusWeeks(1)
    }

    fun nextWeek() {
        pendingEntryFlow.value = null
        selectedDateFlow.value = selectedDateFlow.value.plusWeeks(1)
    }

    fun goToToday() {
        pendingEntryFlow.value = null
        selectedDateFlow.value = LocalDate.now()
    }

    // ── Legacy update methods (kept for any remaining usage) ──────────

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

    // ── New category-based update methods ─────────────────────────────

    fun updateBedtimeCategory(category: BedtimeCategory?) {
        updateCurrentEntry { it.copy(bedtimeCategory = category, bedtime = null) }
    }

    fun updateWakeUpCategory(category: WakeUpCategory?) {
        updateCurrentEntry { it.copy(wakeUpCategory = category, wakeTime = null) }
    }

    fun updateDaySleepCategory(category: DaySleepCategory?) {
        updateCurrentEntry { it.copy(daySleepCategory = category, napMinutes = null) }
    }

    fun updateJapaCategory(category: JapaCategory?) {
        updateCurrentEntry {
            it.copy(
                japaCategory = category,
                japaCompleted = category != null,
                japaTime = null
            )
        }
    }

    fun updateJapaCompleted(completed: Boolean) {
        updateCurrentEntry { it.copy(japaCompleted = completed) }
    }

    // ── Unscored activity updates ─────────────────────────────────────

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
        inMemorySadhakaName.value = name
        debouncedSadhakaNameFlow.tryEmit(name)
    }

    fun updateCounselorName(name: String) {
        inMemoryCounselorName.value = name
        debouncedCounselorNameFlow.tryEmit(name)
    }

    private fun updateCurrentEntry(transform: (SadhanaEntry) -> SadhanaEntry) {
        val current = uiState.value.entry
        val updated = transform(current).copy(
            date = selectedDateFlow.value,
            updatedAt = System.currentTimeMillis()
        )
        // Immediate in-memory update for 60fps frame rendering
        pendingEntryFlow.value = updated
        // Debounced disk write off main thread
        debouncedEntrySaveFlow.tryEmit(updated)
    }
}
