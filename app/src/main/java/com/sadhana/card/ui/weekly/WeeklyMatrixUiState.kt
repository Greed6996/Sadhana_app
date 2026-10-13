package com.sadhana.card.ui.weekly

import androidx.compose.runtime.Immutable
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import java.time.LocalDate

@Immutable
data class MatrixDayColumn(
    val date: LocalDate,
    val dayName: String, // "Sun", "Mon", ...
    val dayNumber: String, // "11", ...
    val entry: SadhanaEntry?,
    val scores: SadhanaScorer.DailyScores,
    val totalScore: Int?
)

@Immutable
data class WeeklyMatrixUiState(
    val startOfWeek: LocalDate = DateUtils.getStartOfWeek(LocalDate.now()),
    val columns: List<MatrixDayColumn> = emptyList(),
    val weekTotalScore: Int = 0,
    val totalPathanMinutes: Int = 0,
    val totalSravanMinutes: Int = 0,
    val totalSevaMinutes: Int = 0,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportError: String? = null,
    val sadhakaName: String = "",
    val counselorName: String = ""
) {
    val maxWeeklyScore: Int = 140 // 7 days * 20 max points
    val scoreProgress: Float
        get() = if (maxWeeklyScore > 0) (weekTotalScore.toFloat() / maxWeeklyScore.toFloat()).coerceIn(0f, 1f) else 0f

    val pathanHoursMinutes: String
        get() = SadhanaScorer.formatMinutesToHoursMinutes(totalPathanMinutes)

    val sravanHoursMinutes: String
        get() = SadhanaScorer.formatMinutesToHoursMinutes(totalSravanMinutes)

    val bothHoursMinutes: String
        get() = SadhanaScorer.formatMinutesToHoursMinutes(totalPathanMinutes + totalSravanMinutes)
}
