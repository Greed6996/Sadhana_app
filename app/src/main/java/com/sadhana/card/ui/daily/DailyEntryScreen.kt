package com.sadhana.card.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.components.DedicationBanner
import com.sadhana.card.ui.components.HeaderFieldsRow
import com.sadhana.card.ui.components.MinutesStepper
import com.sadhana.card.ui.components.SadhanaEntryCard
import com.sadhana.card.ui.components.ScoreCard
import com.sadhana.card.ui.components.SectionHeader
import com.sadhana.card.ui.components.ServiceTagPicker
import com.sadhana.card.ui.components.TimeField
import com.sadhana.card.ui.components.TrainJourneySwitch
import com.sadhana.card.ui.components.WeekDaySelector
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SaffronAccent
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextDark
import java.time.LocalTime

/**
 * Screen 1: Daily Entry screen rebuilt entirely in native Jetpack Compose.
 */
@Composable
fun DailyEntryScreen(
    viewModel: DailyEntryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DailyEntryContent(
        uiState = uiState,
        onSelectDate = viewModel::selectDate,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onGoToToday = viewModel::goToToday,
        onUpdateBedtime = viewModel::updateBedtime,
        onUpdateWakeTime = viewModel::updateWakeTime,
        onUpdateNapMinutes = viewModel::updateNapMinutes,
        onUpdateTrainJourney = viewModel::updateTrainJourney,
        onUpdateJapaTime = viewModel::updateJapaTime,
        onUpdatePathanMinutes = viewModel::updatePathanMinutes,
        onUpdateSravanMinutes = viewModel::updateSravanMinutes,
        onUpdateSevaMinutes = viewModel::updateSevaMinutes,
        onUpdateSevaTags = viewModel::updateSevaTags,
        onUpdateSadhakaName = viewModel::updateSadhakaName,
        onUpdateCounselorName = viewModel::updateCounselorName,
        modifier = modifier
    )
}

@Composable
fun DailyEntryContent(
    uiState: DailyEntryUiState,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGoToToday: () -> Unit,
    onUpdateBedtime: (LocalTime?) -> Unit,
    onUpdateWakeTime: (LocalTime?) -> Unit,
    onUpdateNapMinutes: (Int?) -> Unit,
    onUpdateTrainJourney: (Boolean) -> Unit,
    onUpdateJapaTime: (LocalTime?) -> Unit,
    onUpdatePathanMinutes: (Int?) -> Unit,
    onUpdateSravanMinutes: (Int?) -> Unit,
    onUpdateSevaMinutes: (Int?) -> Unit,
    onUpdateSevaTags: (List<String>) -> Unit,
    onUpdateSadhakaName: (String) -> Unit,
    onUpdateCounselorName: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekTitle = "WEEK OF ${DateUtils.formatWeekRangeUpper(uiState.startOfWeek)}"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weekTitle,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = SaffronAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily Entry",
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        color = TextDark
                    )
                }

                // Week navigation controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderIconButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Week",
                        onClick = onPreviousWeek
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Today,
                        contentDescription = "Jump to Today",
                        onClick = onGoToToday
                    )
                    HeaderIconButton(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Next Week",
                        onClick = onNextWeek
                    )
                }
            }
        }

        // 2. Sadhaka & Counselor Name Inputs
        item {
            HeaderFieldsRow(
                sadhakaName = uiState.sadhakaName,
                onSadhakaNameChanged = onUpdateSadhakaName,
                counselorName = uiState.counselorName,
                onCounselorNameChanged = onUpdateCounselorName
            )
        }

        // 3. Devotional Dedication Banner
        item {
            DedicationBanner()
        }

        // 4. Week Day Selector (Sunday to Saturday)
        item {
            WeekDaySelector(
                startOfWeek = uiState.startOfWeek,
                selectedDate = uiState.selectedDate,
                onDateSelected = onSelectDate
            )
        }

        // 5. Daily Score Summary Card
        item {
            ScoreCard(
                score = uiState.score.totalScore,
                maxScore = 20
            )
        }

        // 6. Section: SLEEP & WAKE
        item {
            SectionHeader(title = "Sleep & Wake Regulation")
        }

        // Bedtime
        item {
            SadhanaEntryCard(
                title = "Rest / Bedtime",
                hint = "Target: ≤ 21:30 (5 pts)",
                icon = Icons.Default.NightsStay,
                score = uiState.score.bedScore,
                maxScore = 5,
                trailingControl = {
                    TimeField(
                        time = uiState.entry.bedtime,
                        onTimeSelected = onUpdateBedtime,
                        title = "Select Bedtime",
                        presetTimes = listOf(
                            LocalTime.of(21, 0),
                            LocalTime.of(21, 30),
                            LocalTime.of(22, 0),
                            LocalTime.of(22, 30)
                        )
                    )
                }
            )
        }

        // Wake-up Time
        item {
            SadhanaEntryCard(
                title = "Wake-up Time",
                hint = "Target: < 03:00 (5 pts), ≤ 03:30 (4 pts)",
                icon = Icons.Default.WbSunny,
                score = uiState.score.wakeScore,
                maxScore = 5,
                trailingControl = {
                    TimeField(
                        time = uiState.entry.wakeTime,
                        onTimeSelected = onUpdateWakeTime,
                        title = "Select Wake-up Time",
                        presetTimes = listOf(
                            LocalTime.of(2, 45),
                            LocalTime.of(3, 0),
                            LocalTime.of(3, 30),
                            LocalTime.of(4, 0)
                        )
                    )
                }
            )
        }

        // Daytime Sleep (Nap)
        item {
            SadhanaEntryCard(
                title = "Daytime Sleep (Nap)",
                hint = if (uiState.entry.trainJourney) {
                    "Train Journey: +60 min exemption active"
                } else {
                    "Target: ≤ 30 min (5 pts), ≤ 45 min (4 pts)"
                },
                icon = Icons.Default.Bed,
                score = uiState.score.napScore,
                maxScore = 5,
                trailingControl = {
                    MinutesStepper(
                        minutes = uiState.entry.napMinutes,
                        onMinutesChanged = onUpdateNapMinutes,
                        step = 15,
                        title = "Day Sleep"
                    )
                }
            )
        }

        // Train Journey Toggle
        item {
            TrainJourneySwitch(
                checked = uiState.entry.trainJourney,
                onCheckedChange = onUpdateTrainJourney
            )
        }

        // 7. Section: CHANTING (JAPA)
        item {
            SectionHeader(title = "Chanting (Japa)")
        }

        item {
            SadhanaEntryCard(
                title = "16 Rounds Finished By",
                hint = "Target: ≤ 07:15 (5 pts), ≤ 09:00 (4 pts)",
                icon = Icons.Default.HourglassTop,
                score = uiState.score.japaScore,
                maxScore = 5,
                trailingControl = {
                    TimeField(
                        time = uiState.entry.japaTime,
                        onTimeSelected = onUpdateJapaTime,
                        title = "16 Rounds Finished By",
                        presetTimes = listOf(
                            LocalTime.of(7, 0),
                            LocalTime.of(7, 15),
                            LocalTime.of(8, 30),
                            LocalTime.of(11, 30)
                        )
                    )
                }
            )
        }

        // 8. Section: SRAVAN & PATHAN
        item {
            SectionHeader(title = "Hearing & Study (Sravanam & Pathan)")
        }

        item {
            SadhanaEntryCard(
                title = "Reading (Pathan / Svadhyaya)",
                hint = "Srila Prabhupada's Books",
                icon = Icons.Default.AutoStories,
                maxScore = 0,
                trailingControl = {
                    MinutesStepper(
                        minutes = uiState.entry.pathanMinutes,
                        onMinutesChanged = onUpdatePathanMinutes,
                        step = 15,
                        title = "Reading"
                    )
                }
            )
        }

        item {
            SadhanaEntryCard(
                title = "Hearing (Sravanam)",
                hint = "Classes, Lectures & Kirtan",
                icon = Icons.Default.Hearing,
                maxScore = 0,
                trailingControl = {
                    MinutesStepper(
                        minutes = uiState.entry.sravanMinutes,
                        onMinutesChanged = onUpdateSravanMinutes,
                        step = 15,
                        title = "Hearing"
                    )
                }
            )
        }

        // 9. Section: PRACTICAL SERVICE (SEVA)
        item {
            SectionHeader(title = "Practical Service (Seva)")
        }

        item {
            SadhanaEntryCard(
                title = "Active Service Duration",
                hint = "Physical devotional service",
                icon = Icons.Default.Handyman,
                maxScore = 0,
                trailingControl = {
                    MinutesStepper(
                        minutes = uiState.entry.sevaMinutes,
                        onMinutesChanged = onUpdateSevaMinutes,
                        step = 15,
                        title = "Seva"
                    )
                }
            )
        }

        item {
            ServiceTagPicker(
                selectedTags = uiState.entry.sevaTags,
                onTagsChanged = onUpdateSevaTags
            )
        }

        // Bottom spacing for navigation bar clearance
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(androidx.compose.foundation.BorderStroke(1.dp, BorderCream), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RustPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
