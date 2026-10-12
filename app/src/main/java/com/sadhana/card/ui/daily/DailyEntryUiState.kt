package com.sadhana.card.ui.daily

import com.sadhana.card.domain.model.SadhanaEntry
import com.sadhana.card.domain.scorer.SadhanaScorer
import com.sadhana.card.domain.util.DateUtils
import java.time.LocalDate

data class DailyEntryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val startOfWeek: LocalDate = DateUtils.getStartOfWeek(LocalDate.now()),
    val entry: SadhanaEntry = SadhanaEntry(date = LocalDate.now()),
    val score: SadhanaScorer.DailyScores = SadhanaScorer.DailyScores(null, null, null, null, null),
    val sadhakaName: String = "",
    val counselorName: String = "",
    val isLoading: Boolean = false
)
