package com.sadhana.card.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadhana.card.data.export.SadhanaWeeklyExcelExporter
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.preferences.UserPreferences
import com.sadhana.card.data.preferences.UserPreferencesRepository
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.repository.SadhanaRepository
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyMatrixViewModel @Inject constructor(
    private val sadhanaRepository: SadhanaRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val excelExporter: SadhanaWeeklyExcelExporter
) : ViewModel() {

    sealed interface ExportEvent {
        data class Success(val file: File) : ExportEvent
        data class Error(val message: String) : ExportEvent
    }

    private val startOfWeekFlow = MutableStateFlow(DateUtils.getStartOfWeek(LocalDate.now()))
    private val isExportingFlow = MutableStateFlow(false)
    private val exportErrorFlow = MutableStateFlow<String?>(null)

    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent: SharedFlow<ExportEvent> = _exportEvent.asSharedFlow()

    // Secondary constructor for existing lightweight unit tests
    constructor(sadhanaRepository: SadhanaRepository) : this(
        sadhanaRepository = sadhanaRepository,
        userPreferencesRepository = object : UserPreferencesRepository {
            override val userPreferencesFlow = flowOf(UserPreferences())
            override suspend fun updateSadhakaName(name: String) {}
            override suspend fun updateCounselorName(name: String) {}
            override suspend fun setDedicationExpanded(expanded: Boolean) {}
            override suspend fun setTermsAccepted(accepted: Boolean, version: Int) {}
            override suspend fun setThemeMode(themeMode: ThemeMode) {}
        },
        excelExporter = SadhanaWeeklyExcelExporter(android.content.ContextWrapper(null))
    )

    val uiState: StateFlow<WeeklyMatrixUiState> = combine(
        startOfWeekFlow.flatMapLatest { startOfWeek ->
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
        },
        userPreferencesRepository.userPreferencesFlow,
        isExportingFlow,
        exportErrorFlow
    ) { baseState, prefs, isExporting, exportError ->
        baseState.copy(
            isExporting = isExporting,
            exportError = exportError,
            sadhakaName = prefs.sadhakaName,
            counselorName = prefs.counselorName
        )
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

    fun clearExportError() {
        exportErrorFlow.value = null
    }

    /**
     * Generates the XLSX spreadsheet for the current sadhaka and week off the main thread.
     */
    fun exportWeeklyReport(
        onSuccess: ((File) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isExportingFlow.value) return

        viewModelScope.launch {
            isExportingFlow.value = true
            exportErrorFlow.value = null
            try {
                val currentStartOfWeek = startOfWeekFlow.value
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val entries = sadhanaRepository.getEntriesForWeek(currentStartOfWeek).first()

                val file = excelExporter.exportWeeklyReport(
                    sadhakaName = prefs.sadhakaName,
                    counselorName = prefs.counselorName,
                    startOfWeek = currentStartOfWeek,
                    entries = entries
                )

                _exportEvent.emit(ExportEvent.Success(file))
                onSuccess?.invoke(file)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to generate weekly Excel report."
                exportErrorFlow.value = msg
                _exportEvent.emit(ExportEvent.Error(msg))
                onError?.invoke(msg)
            } finally {
                isExportingFlow.value = false
            }
        }
    }
}
