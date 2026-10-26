package com.sadhana.card.ui.counselor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.model.WakeUpCategory
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.weekly.MatrixDayColumn
import com.sadhana.card.ui.weekly.WeeklyMatrixUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DevoteeSummary(
    val uid: String,
    val name: String,
    val email: String,
    val counselorName: String,
    val currentWeekScore: Int,
    val currentWeekPercentage: Int,
    val lastUpdated: Long
)

@HiltViewModel
class CounselorDashboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _rawDevotees = MutableStateFlow<List<DevoteeSummary>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDevotee = MutableStateFlow<DevoteeSummary?>(null)
    val selectedDevotee: StateFlow<DevoteeSummary?> = _selectedDevotee.asStateFlow()

    private val _selectedWeekStart = MutableStateFlow(DateUtils.getStartOfWeek(LocalDate.now()))
    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()

    private val _devoteeWeeklyState = MutableStateFlow<WeeklyMatrixUiState?>(null)
    val devoteeWeeklyState: StateFlow<WeeklyMatrixUiState?> = _devoteeWeeklyState.asStateFlow()

    private val _isLoadingWeekly = MutableStateFlow(false)
    val isLoadingWeekly: StateFlow<Boolean> = _isLoadingWeekly.asStateFlow()

    // Filtered and sorted devotees: lowest percentage first (<80% prioritized)
    val devotees: StateFlow<List<DevoteeSummary>> = combine(_rawDevotees, _searchQuery) { list, query ->
        val trimmed = query.trim().lowercase()
        val filtered = if (trimmed.isEmpty()) {
            list
        } else {
            list.filter {
                it.name.lowercase().contains(trimmed) ||
                it.email.lowercase().contains(trimmed) ||
                it.counselorName.lowercase().contains(trimmed)
            }
        }
        filtered.sortedBy { it.currentWeekPercentage }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadDevotees()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadDevotees() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .whereIn("role", listOf("devotee", "DEVOTEE"))
                        .get()
                        .await()
                }

                val list = snapshot.documents.map { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val name = doc.getString("displayName")?.ifBlank { "Devotee" } ?: "Devotee"
                    val email = doc.getString("email") ?: ""
                    val counselor = doc.getString("counselorName") ?: ""
                    val score = (doc.getLong("currentWeekScore") ?: 0L).toInt()
                    val percentage = (doc.getLong("currentWeekPercentage") ?: 0L).toInt()
                    val updatedAt = doc.getLong("currentWeekUpdatedAt") ?: 0L

                    DevoteeSummary(
                        uid = uid,
                        name = name,
                        email = email,
                        counselorName = counselor,
                        currentWeekScore = score,
                        currentWeekPercentage = percentage,
                        lastUpdated = updatedAt
                    )
                }

                _rawDevotees.value = list
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load devotees."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDevotee(devotee: DevoteeSummary) {
        _selectedDevotee.value = devotee
        _selectedWeekStart.value = DateUtils.getStartOfWeek(LocalDate.now())
        loadSelectedDevoteeWeeklyData()
    }

    fun clearSelectedDevotee() {
        _selectedDevotee.value = null
        _devoteeWeeklyState.value = null
    }

    fun previousWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.minusWeeks(1)
        loadSelectedDevoteeWeeklyData()
    }

    fun nextWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.plusWeeks(1)
        loadSelectedDevoteeWeeklyData()
    }

    private fun loadSelectedDevoteeWeeklyData() {
        val devotee = _selectedDevotee.value ?: return
        val startOfWeek = _selectedWeekStart.value
        val weekKey = startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            _isLoadingWeekly.value = true
            try {
                val doc = withContext(Dispatchers.IO) {
                    firestore.collection("users").document(devotee.uid)
                        .collection("weekly_summaries").document(weekKey)
                        .get()
                        .await()
                }

                val weekDates = DateUtils.getWeekSundayStart(startOfWeek)
                val daysMap = doc.get("days") as? Map<*, *> ?: emptyMap<String, Any>()

                var weekTotal = 0
                var pathanTotal = 0
                var sravanTotal = 0

                val columns = weekDates.map { date ->
                    val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val dayData = daysMap[dateKey] as? Map<*, *>

                    if (dayData != null) {
                        val bedCategoryStr = (dayData["bedtimeCategory"] as? String)?.ifBlank { null }
                        val wakeCategoryStr = (dayData["wakeUpCategory"] as? String)?.ifBlank { null }
                        val japaCategoryStr = (dayData["japaCategory"] as? String)?.ifBlank { null }
                        val napCategoryStr = (dayData["daySleepCategory"] as? String)?.ifBlank { null }
                        val pathanMin = (dayData["pathanMinutes"] as? Long)?.toInt() ?: 0
                        val sravanMin = (dayData["sravanMinutes"] as? Long)?.toInt() ?: 0

                        val bedCat = bedCategoryStr?.let { runCatching { BedtimeCategory.valueOf(it) }.getOrNull() }
                        val wakeCat = wakeCategoryStr?.let { runCatching { WakeUpCategory.valueOf(it) }.getOrNull() }
                        val japaCat = japaCategoryStr?.let { raw ->
                            runCatching { JapaCategory.valueOf(raw) }.getOrElse {
                                when (raw) {
                                    "BY_7_15_AM" -> JapaCategory.BY_9_00_AM
                                    "BY_11_59_AM" -> JapaCategory.BEFORE_12_00_PM
                                    else -> null
                                }
                            }
                        }
                        val napCat = napCategoryStr?.let { runCatching { DaySleepCategory.valueOf(it) }.getOrNull() }

                        val japaScoreDirect = (dayData["japaScore"] as? Long)?.toInt()
                            ?: (dayData["sixteenRoundsDoneByScore"] as? Long)?.toInt()
                            ?: (dayData["roundsCompletedScore"] as? Long)?.toInt()

                        val japaDone = (dayData["japaCompleted"] as? Boolean)
                            ?: (japaCat != null)
                            ?: (japaScoreDirect != null && japaScoreDirect > 0)

                        val entry = SadhanaEntry(
                            date = date,
                            bedtimeCategory = bedCat,
                            wakeUpCategory = wakeCat,
                            japaCategory = japaCat,
                            japaCompleted = japaDone,
                            daySleepCategory = napCat,
                            pathanMinutes = pathanMin,
                            sravanMinutes = sravanMin,
                            userId = devotee.uid,
                            isSynced = true
                        )

                        var scores = SadhanaScorer.scoreEntry(entry)
                        if (japaScoreDirect != null && (scores.japaScore == null || scores.japaScore == 0) && japaScoreDirect > 0) {
                            scores = scores.copy(japaScore = japaScoreDirect)
                        }

                        val pathanScoreDirect = (dayData["pathanScore"] as? Long)?.toInt()
                        val sravanScoreDirect = (dayData["sravanScore"] as? Long)?.toInt()

                        if (pathanScoreDirect != null && (scores.pathanScore == null || scores.pathanScore == 0) && pathanScoreDirect > 0) {
                            scores = scores.copy(pathanScore = pathanScoreDirect)
                        }
                        if (sravanScoreDirect != null && (scores.sravanScore == null || scores.sravanScore == 0) && sravanScoreDirect > 0) {
                            scores = scores.copy(sravanScore = sravanScoreDirect)
                        }

                        val dayScore = scores.totalScore
                        if (dayScore != null) {
                            pathanTotal += pathanMin
                            sravanTotal += sravanMin
                            weekTotal += dayScore
                        }

                        MatrixDayColumn(
                            date = date,
                            dayName = DateUtils.formatDayName(date),
                            dayNumber = DateUtils.formatDayNumber(date),
                            entry = if (dayScore != null) entry else null,
                            scores = scores,
                            totalScore = dayScore
                        )
                    } else {
                        MatrixDayColumn(
                            date = date,
                            dayName = DateUtils.formatDayName(date),
                            dayNumber = DateUtils.formatDayNumber(date),
                            entry = null,
                            scores = SadhanaScorer.DailyScores(),
                            totalScore = null
                        )
                    }
                }

                _devoteeWeeklyState.value = WeeklyMatrixUiState(
                    startOfWeek = startOfWeek,
                    columns = columns,
                    weekTotalScore = weekTotal,
                    totalPathanMinutes = pathanTotal,
                    totalSravanMinutes = sravanTotal,
                    sadhakaName = devotee.name,
                    counselorName = devotee.counselorName,
                    isLoading = false
                )
            } catch (e: Exception) {
                // If error or unlogged, construct empty state
                val weekDates = DateUtils.getWeekSundayStart(startOfWeek)
                val columns = weekDates.map { date ->
                    MatrixDayColumn(
                        date = date,
                        dayName = DateUtils.formatDayName(date),
                        dayNumber = DateUtils.formatDayNumber(date),
                        entry = null,
                        scores = SadhanaScorer.DailyScores(null, null, null, null, null),
                        totalScore = null
                    )
                }
                _devoteeWeeklyState.value = WeeklyMatrixUiState(
                    startOfWeek = startOfWeek,
                    columns = columns,
                    weekTotalScore = 0,
                    sadhakaName = devotee.name,
                    counselorName = devotee.counselorName,
                    isLoading = false
                )
            } finally {
                _isLoadingWeekly.value = false
            }
        }
    }
}
