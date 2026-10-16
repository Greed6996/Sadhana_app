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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.sadhana.card.domain.model.BedtimeCategory
import com.sadhana.card.domain.model.DaySleepCategory
import com.sadhana.card.domain.model.JapaCategory
import com.sadhana.card.domain.model.WakeUpCategory
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.components.DedicationBanner
import com.sadhana.card.ui.components.HeaderFieldsRow
import com.sadhana.card.ui.components.JapaCompletionToggle
import com.sadhana.card.ui.components.MinutesStepper
import com.sadhana.card.ui.components.SadhanaEntryCard
import com.sadhana.card.ui.components.ScoreCard
import com.sadhana.card.ui.components.ScoringCategoryCard
import com.sadhana.card.ui.components.ScoringOptionRow
import com.sadhana.card.ui.components.SectionHeader
import com.sadhana.card.ui.components.ServiceTagPicker
import com.sadhana.card.ui.components.TrainJourneySwitch
import com.sadhana.card.ui.components.WeekDaySelector
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import java.time.LocalDate

/**
 * Screen 1: Daily Entry screen rebuilt entirely in native Jetpack Compose.
 */
@Composable
fun DailyEntryScreen(
    viewModel: DailyEntryViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DailyEntryContent(
        uiState = uiState,
        onSelectDate = viewModel::selectDate,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onGoToToday = viewModel::goToToday,
        onUpdateBedtimeCategory = viewModel::updateBedtimeCategory,
        onUpdateWakeUpCategory = viewModel::updateWakeUpCategory,
        onUpdateDaySleepCategory = viewModel::updateDaySleepCategory,
        onUpdateTrainJourney = viewModel::updateTrainJourney,
        onUpdateJapaCategory = viewModel::updateJapaCategory,
        onUpdateJapaCompleted = viewModel::updateJapaCompleted,
        onUpdatePathanMinutes = viewModel::updatePathanMinutes,
        onUpdateSravanMinutes = viewModel::updateSravanMinutes,
        onUpdateSevaMinutes = viewModel::updateSevaMinutes,
        onUpdateSevaTags = viewModel::updateSevaTags,
        onUpdateSadhakaName = viewModel::updateSadhakaName,
        onUpdateCounselorName = viewModel::updateCounselorName,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

enum class DailyEntryAccordionSection {
    NONE,
    BEDTIME,
    WAKE_UP,
    DAY_SLEEP,
    JAPA
}

@Composable
fun DailyEntryContent(
    uiState: DailyEntryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGoToToday: () -> Unit,
    onUpdateBedtimeCategory: (BedtimeCategory?) -> Unit,
    onUpdateWakeUpCategory: (WakeUpCategory?) -> Unit,
    onUpdateDaySleepCategory: (DaySleepCategory?) -> Unit,
    onUpdateTrainJourney: (Boolean) -> Unit,
    onUpdateJapaCategory: (JapaCategory?) -> Unit,
    onUpdateJapaCompleted: (Boolean) -> Unit = {},
    onUpdatePathanMinutes: (Int?) -> Unit,
    onUpdateSravanMinutes: (Int?) -> Unit,
    onUpdateSevaMinutes: (Int?) -> Unit,
    onUpdateSevaTags: (List<String>) -> Unit,
    onUpdateSadhakaName: (String) -> Unit = {},
    onUpdateCounselorName: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val weekTitle = "WEEK OF ${DateUtils.formatWeekRangeUpper(uiState.startOfWeek)}"
    var expandedSection by rememberSaveable { mutableStateOf(DailyEntryAccordionSection.NONE) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Row
        item(key = "header") {
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
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily Entry",
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Week navigation and info controls
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
                    HeaderIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings
                    )
                }
            }
        }

        // 2. Devotional Dedication Banner
        item(key = "dedication") {
            DedicationBanner()
        }

        // 3. Week Day Selector (Sunday to Saturday)
        item(key = "weekday_selector") {
            WeekDaySelector(
                startOfWeek = uiState.startOfWeek,
                selectedDate = uiState.selectedDate,
                onDateSelected = onSelectDate
            )
        }

        // 4. Daily Score Summary Card
        item(key = "score_summary") {
            ScoreCard(
                score = uiState.score.totalScore,
                maxScore = 20
            )
        }

        // 5. Section: SLEEP & WAKE
        item(key = "section_sleep") {
            SectionHeader(title = "Sleep & Wake Regulation")
        }

        // Bedtime — Category selector (Single-expand accordion)
        item(key = "bedtime") {
            ScoringCategoryCard(
                title = "Bedtime",
                subtitle = "Select when you went to bed",
                icon = Icons.Default.NightsStay,
                score = uiState.score.bedScore,
                maxScore = 5,
                isExpanded = expandedSection == DailyEntryAccordionSection.BEDTIME,
                onHeaderClick = {
                    expandedSection = if (expandedSection == DailyEntryAccordionSection.BEDTIME) {
                        DailyEntryAccordionSection.NONE
                    } else {
                        DailyEntryAccordionSection.BEDTIME
                    }
                },
                selectedLabel = uiState.entry.bedtimeCategory?.label
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BedtimeCategory.entries.forEach { category ->
                        ScoringOptionRow(
                            label = category.label,
                            points = category.points,
                            isSelected = uiState.entry.bedtimeCategory == category,
                            onClick = { onUpdateBedtimeCategory(category) }
                        )
                    }
                }
            }
        }

        // Wake-up — Category selector (Single-expand accordion)
        item(key = "wake_time") {
            ScoringCategoryCard(
                title = "Wake-up",
                subtitle = "Select when you woke up",
                icon = Icons.Default.WbSunny,
                score = uiState.score.wakeScore,
                maxScore = 5,
                isExpanded = expandedSection == DailyEntryAccordionSection.WAKE_UP,
                onHeaderClick = {
                    expandedSection = if (expandedSection == DailyEntryAccordionSection.WAKE_UP) {
                        DailyEntryAccordionSection.NONE
                    } else {
                        DailyEntryAccordionSection.WAKE_UP
                    }
                },
                selectedLabel = uiState.entry.wakeUpCategory?.label
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WakeUpCategory.entries.forEach { category ->
                        ScoringOptionRow(
                            label = category.label,
                            points = category.points,
                            isSelected = uiState.entry.wakeUpCategory == category,
                            onClick = { onUpdateWakeUpCategory(category) }
                        )
                    }
                }
            }
        }

        // Daytime Sleep — Category selector (Single-expand accordion)
        item(key = "day_nap") {
            ScoringCategoryCard(
                title = "Day sleep",
                subtitle = "Select your total daytime sleep",
                icon = Icons.Default.Bed,
                score = uiState.score.napScore,
                maxScore = 5,
                isExpanded = expandedSection == DailyEntryAccordionSection.DAY_SLEEP,
                onHeaderClick = {
                    expandedSection = if (expandedSection == DailyEntryAccordionSection.DAY_SLEEP) {
                        DailyEntryAccordionSection.NONE
                    } else {
                        DailyEntryAccordionSection.DAY_SLEEP
                    }
                },
                selectedLabel = uiState.entry.daySleepCategory?.label
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DaySleepCategory.entries.forEach { category ->
                        ScoringOptionRow(
                            label = category.label,
                            points = category.points,
                            isSelected = uiState.entry.daySleepCategory == category,
                            onClick = { onUpdateDaySleepCategory(category) }
                        )
                    }
                }
            }
        }

        // Train Journey Toggle
        item(key = "train_journey") {
            TrainJourneySwitch(
                checked = uiState.entry.trainJourney,
                onCheckedChange = onUpdateTrainJourney
            )
        }

        // 6. Section: CHANTING (JAPA)
        item(key = "section_japa") {
            SectionHeader(title = "Chanting (Japa)")
        }

        // Japa — 16 rounds — Category selector (Single-expand accordion, direct selection, no checkbox)
        item(key = "japa") {
            ScoringCategoryCard(
                title = "Japa — 16 rounds",
                subtitle = "Select when you completed all 16 rounds",
                icon = Icons.Default.HourglassTop,
                score = uiState.score.japaScore,
                maxScore = 5,
                isExpanded = expandedSection == DailyEntryAccordionSection.JAPA,
                onHeaderClick = {
                    expandedSection = if (expandedSection == DailyEntryAccordionSection.JAPA) {
                        DailyEntryAccordionSection.NONE
                    } else {
                        DailyEntryAccordionSection.JAPA
                    }
                },
                selectedLabel = uiState.entry.japaCategory?.label
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    JapaCategory.entries.forEach { category ->
                        ScoringOptionRow(
                            label = category.label,
                            points = category.points,
                            isSelected = uiState.entry.japaCategory == category,
                            onClick = { onUpdateJapaCategory(category) }
                        )
                    }
                }
            }
        }

        // 8. Section: SRAVAN & PATHAN
        item(key = "section_study") {
            SectionHeader(title = "Hearing & Study (Sravanam & Pathan)")
        }

        item(key = "pathan") {
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

        item(key = "sravan") {
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
        item(key = "section_seva") {
            SectionHeader(title = "Practical Service (Seva)")
        }

        item(key = "seva_minutes") {
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

        item(key = "seva_tags") {
            ServiceTagPicker(
                selectedTags = uiState.entry.sevaTags,
                onTagsChanged = onUpdateSevaTags
            )
        }

        // Bottom spacing for navigation bar clearance
        item(key = "bottom_spacer") {
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
            .background(MaterialTheme.colorScheme.surface)
            .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}
