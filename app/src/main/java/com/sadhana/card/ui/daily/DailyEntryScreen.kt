package com.sadhana.card.ui.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sadhana.card.ui.components.DedicationBanner
import com.sadhana.card.ui.components.ScoreCard
import com.sadhana.card.ui.components.WeekDaySelector
import com.sadhana.card.ui.theme.CardShape
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkGoldBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkScorePillBg
import com.sadhana.card.ui.theme.DarkScorePillText
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkSurfaceVariant
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.PillShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Daily Entry screen matching the V5 Dark Rings aesthetic.
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
        onUpdateNapMinutes = viewModel::updateNapMinutes,
        onUpdateTrainJourney = viewModel::updateTrainJourney,
        onUpdateJapaCategory = viewModel::updateJapaCategory,
        onUpdatePathanMinutes = viewModel::updatePathanMinutes,
        onUpdateSravanMinutes = viewModel::updateSravanMinutes,
        onUpdateSevaMinutes = viewModel::updateSevaMinutes,
        onUpdateSevaTags = viewModel::updateSevaTags,
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
    onUpdateNapMinutes: (Int?) -> Unit = {},
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
    val monthShort = uiState.startOfWeek.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
    val weekTitle = "Week of ${uiState.startOfWeek.dayOfMonth} $monthShort"
    val dayFullName = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    val formattedDate = uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

    val total = uiState.score.totalScore ?: 0
    val topDateColor = if (!uiState.isLogged) {
        Color(0xFFF5B041) // Default State (Unlogged / Normal): Yellow
    } else {
        when {
            total < 75 -> Color(0xFFEF4444)   // Red: totalScore < 75
            total < 200 -> Color(0xFFF5B041)  // Yellow: 75 <= totalScore < 200
            else -> Color(0xFF10B981)         // Green: totalScore == 200
        }
    }

    var expandedSection by rememberSaveable { mutableStateOf(DailyEntryAccordionSection.NONE) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Row: Sadhaka Name + Day & Date + Week Chevrons & Settings Button
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (uiState.sadhakaName.isNotBlank()) {
                        Text(
                            text = uiState.sadhakaName,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = DarkTextMuted
                        )
                    }
                    Text(
                        text = dayFullName,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        color = topDateColor
                    )
                    Text(
                        text = formattedDate,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = topDateColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                            .clickable(role = Role.Button, onClick = onPreviousWeek)
                            .semantics { contentDescription = "Previous week" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = DarkTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                            .clickable(role = Role.Button, onClick = onNextWeek)
                            .semantics { contentDescription = "Next week" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DarkTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                            .clickable(role = Role.Button, onClick = onOpenSettings)
                            .semantics { contentDescription = "Settings" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Dedication Card
        item(key = "dedication") {
            DedicationBanner()
        }

        // 3. 7-Day Selector Strip
        item(key = "weekday_selector") {
            WeekDaySelector(
                startOfWeek = uiState.startOfWeek,
                selectedDate = uiState.selectedDate,
                onDateSelected = onSelectDate,
                activeColor = topDateColor
            )
        }

        // 4. Hero Score Card with Circular Total Score and 7 Category Breakdown
        item(key = "score_hero") {
            ScoreCard(
                score = uiState.score.totalScore,
                maxScore = 200,
                bedScore = uiState.score.bedScore,
                wakeScore = uiState.score.wakeScore,
                japaScore = uiState.score.japaScore,
                napScore = uiState.score.napScore,
                pathanScore = uiState.score.pathanScore,
                sravanScore = uiState.score.sravanScore,
                sevaScore = uiState.score.sevaScore
            )
        }

        if (!uiState.isEditable) {
            item(key = "historical_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1B16))
                        .border(BorderStroke(1.dp, DarkGoldBorder.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 16.sp
                        )
                        Column {
                            Text(
                                text = "Historical Entry • Read-Only",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DarkPrimary
                            )
                            Text(
                                text = "Entries for past dates are preserved and cannot be modified.",
                                fontFamily = DmSansFontFamily,
                                fontSize = 11.5.sp,
                                color = DarkTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 5. Section Header: SLEEP & JAPA
        item(key = "header_sleep_japa") {
            V5SectionHeader(title = "SLEEP & JAPA")
        }

        // Unified Card: SLEEP & JAPA
        item(key = "card_sleep_japa") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), CardShape)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Bedtime
                V5AccordionRow(
                    title = "Bedtime",
                    subtitle = "25 pts if by 9:30 PM",
                    score = uiState.score.bedScore ?: 0,
                    isExpanded = expandedSection == DailyEntryAccordionSection.BEDTIME,
                    enabled = uiState.isEditable,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == DailyEntryAccordionSection.BEDTIME) {
                            DailyEntryAccordionSection.NONE
                        } else {
                            DailyEntryAccordionSection.BEDTIME
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BedtimeCategory.entries.forEach { cat ->
                            V5OptionItem(
                                label = cat.label,
                                points = cat.points,
                                isSelected = uiState.entry.bedtimeCategory == cat,
                                enabled = uiState.isEditable,
                                onClick = {
                                    if (uiState.isEditable) {
                                        onUpdateBedtimeCategory(cat)
                                        expandedSection = DailyEntryAccordionSection.NONE
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 2: Wake-up
                V5AccordionRow(
                    title = "Wake-up",
                    subtitle = "25 pts if before 3:00 AM",
                    score = uiState.score.wakeScore ?: 0,
                    isExpanded = expandedSection == DailyEntryAccordionSection.WAKE_UP,
                    enabled = uiState.isEditable,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == DailyEntryAccordionSection.WAKE_UP) {
                            DailyEntryAccordionSection.NONE
                        } else {
                            DailyEntryAccordionSection.WAKE_UP
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WakeUpCategory.entries.forEach { cat ->
                            V5OptionItem(
                                label = cat.label,
                                points = cat.points,
                                isSelected = uiState.entry.wakeUpCategory == cat,
                                enabled = uiState.isEditable,
                                onClick = {
                                    if (uiState.isEditable) {
                                        onUpdateWakeUpCategory(cat)
                                        expandedSection = DailyEntryAccordionSection.NONE
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 3: 16 rounds done by (JAPA) - Placed BEFORE Day Sleep
                V5AccordionRow(
                    title = "16 rounds done by",
                    subtitle = "50 pts if by 7:15 AM",
                    score = uiState.score.japaScore ?: 0,
                    isExpanded = expandedSection == DailyEntryAccordionSection.JAPA,
                    enabled = uiState.isEditable,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == DailyEntryAccordionSection.JAPA) {
                            DailyEntryAccordionSection.NONE
                        } else {
                            DailyEntryAccordionSection.JAPA
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        JapaCategory.entries.forEach { cat ->
                            V5OptionItem(
                                label = cat.label,
                                points = cat.points,
                                isSelected = uiState.entry.japaCategory == cat,
                                enabled = uiState.isEditable,
                                onClick = {
                                    if (uiState.isEditable) {
                                        onUpdateJapaCategory(cat)
                                        expandedSection = DailyEntryAccordionSection.NONE
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 4: Day sleep (Nap) - Placed AFTER 16 Rounds Japa
                V5AccordionRow(
                    title = "Day sleep",
                    subtitle = "25 pts up to 30 min",
                    score = uiState.score.napScore ?: 0,
                    isExpanded = expandedSection == DailyEntryAccordionSection.DAY_SLEEP,
                    enabled = uiState.isEditable,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == DailyEntryAccordionSection.DAY_SLEEP) {
                            DailyEntryAccordionSection.NONE
                        } else {
                            DailyEntryAccordionSection.DAY_SLEEP
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DaySleepCategory.entries.forEach { cat ->
                            V5OptionItem(
                                label = cat.label,
                                points = cat.points,
                                isSelected = uiState.entry.daySleepCategory == cat,
                                enabled = uiState.isEditable,
                                onClick = {
                                    if (uiState.isEditable) {
                                        onUpdateDaySleepCategory(cat)
                                        expandedSection = DailyEntryAccordionSection.NONE
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 6. Section Header: STUDY & SERVICE · MINUTES
        item(key = "header_study_service") {
            V5SectionHeader(title = "STUDY & SERVICE · MINUTES")
        }

        // Unified Card: STUDY & SERVICE · MINUTES
        item(key = "card_study_service") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), CardShape)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Pathan (Reading)
                V5MinuteStepperRow(
                    title = "Pathan",
                    subtitle = "Reading Srila Prabhupada’s books",
                    minutes = uiState.entry.pathanMinutes,
                    score = uiState.score.pathanScore,
                    onMinutesChanged = onUpdatePathanMinutes,
                    step = 5,
                    enabled = uiState.isEditable
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 2: Sravan (Hearing)
                V5MinuteStepperRow(
                    title = "Sravan",
                    subtitle = "Hearing lectures",
                    minutes = uiState.entry.sravanMinutes,
                    score = uiState.score.sravanScore,
                    onMinutesChanged = onUpdateSravanMinutes,
                    step = 5,
                    enabled = uiState.isEditable
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 3: Seva (Practical Service) - Tags removed
                V5MinuteStepperRow(
                    title = "Seva",
                    subtitle = "Practical devotional service",
                    minutes = uiState.entry.sevaMinutes,
                    score = uiState.score.sevaScore,
                    onMinutesChanged = onUpdateSevaMinutes,
                    step = 5,
                    enabled = uiState.isEditable
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun V5SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = DarkTextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

/**
 * Dynamic Score Pill Badge matching V5 Specifications:
 * - Score = 0: Dark Crimson tint (#2D1416), vibrant red text (#EF4444), subtle red border
 * - Score = 1..4: Dark Amber tint (#2B2113), Saffron Gold text (#F5B041), subtle amber border
 * - Score = 5: Dark Emerald tint (#112A20), vibrant Emerald Green text (#10B981), subtle emerald border
 */
@Composable
private fun V5ScorePillBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when {
        score <= 0 -> Triple(
            Color(0xFF2D1416),
            Color(0xFFEF4444),
            Color(0xFFEF4444).copy(alpha = 0.35f)
        )
        score in 1..24 -> Triple(
            Color(0xFF2B2113),
            Color(0xFFF5B041),
            Color(0xFFD4932E).copy(alpha = 0.35f)
        )
        else -> Triple(
            Color(0xFF112A20),
            Color(0xFF10B981),
            Color(0xFF10B981).copy(alpha = 0.35f)
        )
    }

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (score > 0) "+$score" else "$score",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = textColor
        )
    }
}

/**
 * Accordion Row for Bedtime, Wake-up, 16 rounds done by, and Day sleep:
 * Left: Title & Subtitle
 * Right: Dynamic Score Badge (+0, +3, +5, etc.)
 */
@Composable
private fun V5AccordionRow(
    title: String,
    subtitle: String,
    score: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onToggleExpand)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (enabled) DarkTextPrimary else DarkTextMuted
                )
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = DarkTextMuted
                )
            }

            // Dynamic score badge pill e.g. +0, +3, +5
            V5ScorePillBadge(score = score)
        }

        AnimatedVisibility(
            visible = isExpanded && enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            content()
        }
    }
}

/**
 * Option item for expanded accordion list
 */
@Composable
private fun V5OptionItem(
    label: String,
    points: Int,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) DarkScorePillBg else DarkSurfaceVariant)
            .border(
                BorderStroke(1.dp, if (isSelected) DarkGoldBorder else DarkBorder),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.5.sp,
            color = if (isSelected) DarkPrimary else DarkTextPrimary
        )

        Text(
            text = "+$points pts",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isSelected) DarkPrimary else DarkTextMuted
        )
    }
}

/**
 * Stepper row for Study & Service (Pathan, Sravan, Seva)
 */
@Composable
private fun V5MinuteStepperRow(
    title: String,
    subtitle: String,
    minutes: Int,
    score: Int? = null,
    onMinutesChanged: (Int?) -> Unit,
    step: Int = 5,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) DarkTextPrimary else DarkTextMuted
            )
            Text(
                text = subtitle,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = DarkTextMuted
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (score != null) {
                V5ScorePillBadge(score = score)
            }

            V5InlineStepper(
                value = minutes,
                enabled = enabled,
                onDecrement = {
                    val next = (minutes - step).coerceAtLeast(0)
                    onMinutesChanged(next)
                },
                onIncrement = {
                    val next = (minutes + step).coerceAtMost(720)
                    onMinutesChanged(next)
                }
            )
        }
    }
}

/**
 * Inline Stepper: [ —  45  + ]
 */
@Composable
private fun V5InlineStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onNumberClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(BorderStroke(1.dp, if (enabled) DarkGoldBorder else DarkBorder), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = if (enabled) DarkTextPrimary else DarkTextMuted
            )
        }

        // Value text
        Box(
            modifier = Modifier
                .width(42.dp)
                .then(if (onNumberClick != null && enabled) Modifier.clickable(onClick = onNumberClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (enabled) DarkTextPrimary else DarkTextMuted
            )
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = if (enabled) DarkTextPrimary else DarkTextMuted
            )
        }
    }
}

