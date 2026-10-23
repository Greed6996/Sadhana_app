package com.sadhana.card.ui.daily

import androidx.compose.runtime.Immutable
import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import java.time.LocalDate

@Immutable
data class DailyEntryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val startOfWeek: LocalDate = DateUtils.getStartOfWeek(LocalDate.now()),
    val entry: SadhanaEntry = SadhanaEntry(date = LocalDate.now()),
    val score: SadhanaScorer.DailyScores = SadhanaScorer.DailyScores(null, null, null, null, null),
    val sadhakaName: String = "",
    val counselorName: String = "",
    val hasAcceptedTerms: Boolean = false,
    val isLogged: Boolean = false,
    val isEditable: Boolean = true,
    val isLoading: Boolean = false
)
