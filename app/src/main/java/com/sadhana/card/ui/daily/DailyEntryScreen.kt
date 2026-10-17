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

    var expandedSection by rememberSaveable { mutableStateOf(DailyEntryAccordionSection.NONE) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Row: Sadhaka Name + Day Title + Week Chevrons & Settings Button
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
                        color = DarkTextPrimary
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
                onDateSelected = onSelectDate
            )
        }

        // 4. Hero Score Card with 4 Circular Progress Rings
        item(key = "score_hero") {
            ScoreCard(
                score = uiState.score.totalScore,
                maxScore = 20,
                bedScore = uiState.score.bedScore,
                wakeScore = uiState.score.wakeScore,
                napScore = uiState.score.napScore,
                japaScore = uiState.score.japaScore
            )
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
                    subtitle = "5 pts if by 9:30 PM",
                    scoreBadgeText = "+${uiState.score.bedScore ?: 0}",
                    isExpanded = expandedSection == DailyEntryAccordionSection.BEDTIME,
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
                                onClick = {
                                    onUpdateBedtimeCategory(cat)
                                    expandedSection = DailyEntryAccordionSection.NONE
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 2: Wake-up
                V5AccordionRow(
                    title = "Wake-up",
                    subtitle = "5 pts if before 3:00 AM",
                    scoreBadgeText = "+${uiState.score.wakeScore ?: 0}",
                    isExpanded = expandedSection == DailyEntryAccordionSection.WAKE_UP,
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
                                onClick = {
                                    onUpdateWakeUpCategory(cat)
                                    expandedSection = DailyEntryAccordionSection.NONE
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 3: 16 rounds done by (JAPA) - No checkbox!
                V5AccordionRow(
                    title = "16 rounds done by",
                    subtitle = "5 pts if by 7:15 AM",
                    scoreBadgeText = "+${uiState.score.japaScore ?: 0}",
                    isExpanded = expandedSection == DailyEntryAccordionSection.JAPA,
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
                                onClick = {
                                    // Selecting immediately awards points & marks japa completed
                                    onUpdateJapaCategory(cat)
                                    expandedSection = DailyEntryAccordionSection.NONE
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 4: Day sleep
                val napDisplay = uiState.entry.napMinutes ?: when (uiState.entry.daySleepCategory) {
                    DaySleepCategory.UP_TO_30_MIN -> 30
                    DaySleepCategory.FROM_31_TO_45_MIN -> 40
                    DaySleepCategory.FROM_46_TO_60_MIN -> 55
                    DaySleepCategory.MORE_THAN_60_MIN -> 75
                    null -> 0
                }
                V5DaySleepRow(
                    title = "Day sleep",
                    subtitle = "5 pts up to 30 min",
                    scoreBadgeText = "+${uiState.score.napScore ?: 0} pts",
                    minutes = napDisplay,
                    onMinutesChanged = onUpdateNapMinutes,
                    isExpanded = expandedSection == DailyEntryAccordionSection.DAY_SLEEP,
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
                                onClick = {
                                    onUpdateDaySleepCategory(cat)
                                    expandedSection = DailyEntryAccordionSection.NONE
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 5: Train journey / travel Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Checkbox) {
                            onUpdateTrainJourney(!uiState.entry.trainJourney)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (uiState.entry.trainJourney) DarkPrimary else DarkSurfaceVariant)
                            .border(
                                BorderStroke(1.dp, if (uiState.entry.trainJourney) DarkPrimary else DarkBorder),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.entry.trainJourney) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Train journey / travel",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "+60 min allowance before scoring",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = DarkTextMuted
                        )
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
            var showCustomTagDialog by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), CardShape)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Pathan
                V5MinuteStepperRow(
                    title = "Pathan",
                    subtitle = "Reading Prabhupada's books",
                    minutes = uiState.entry.pathanMinutes,
                    onMinutesChanged = onUpdatePathanMinutes,
                    step = 15
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 2: Sravan
                V5MinuteStepperRow(
                    title = "Sravan",
                    subtitle = "Hearing lectures",
                    minutes = uiState.entry.sravanMinutes,
                    onMinutesChanged = onUpdateSravanMinutes,
                    step = 15
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Row 3: Seva
                V5MinuteStepperRow(
                    title = "Seva",
                    subtitle = "Practical service",
                    minutes = uiState.entry.sevaMinutes,
                    onMinutesChanged = onUpdateSevaMinutes,
                    step = 15
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Service Tags Multi-Select
                val defaultTags = listOf("Cooking", "Mentoring", "Lecturing", "Calling", "Temple cleaning")
                val allTags = remember(uiState.entry.sevaTags) {
                    (defaultTags + uiState.entry.sevaTags).distinct()
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allTags.forEach { tag ->
                        val isSelected = uiState.entry.sevaTags.contains(tag)
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isSelected) DarkPrimary else DarkSurfaceVariant)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) DarkPrimary else DarkBorder),
                                    PillShape
                                )
                                .clickable(role = Role.Checkbox) {
                                    if (isSelected) {
                                        onUpdateSevaTags(uiState.entry.sevaTags - tag)
                                    } else {
                                        onUpdateSevaTags(uiState.entry.sevaTags + tag)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) DarkBg else DarkTextPrimary
                            )
                        }
                    }

                    // + Add tag dashed pill
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .border(BorderStroke(1.dp, DarkGoldBorder), PillShape)
                            .clickable(role = Role.Button) { showCustomTagDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Add tag",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DarkPrimary
                        )
                    }
                }
            }

            if (showCustomTagDialog) {
                var newTagName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCustomTagDialog = false },
                    title = {
                        Text(
                            text = "Add Service Tag",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkPrimary
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            placeholder = { Text("e.g. Deity Garland", color = DarkTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = newTagName.trim()
                                if (trimmed.isNotEmpty() && !uiState.entry.sevaTags.contains(trimmed)) {
                                    onUpdateSevaTags(uiState.entry.sevaTags + trimmed)
                                }
                                showCustomTagDialog = false
                            }
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold, color = DarkPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomTagDialog = false }) {
                            Text("Cancel", color = DarkTextMuted)
                        }
                    }
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
 * Accordion Row for Bedtime, Wake-up, 16 rounds done by:
 * Left: Title & Subtitle
 * Right: Score Badge (+0, +3, +5, etc.)
 */
@Composable
private fun V5AccordionRow(
    title: String,
    subtitle: String,
    scoreBadgeText: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToggleExpand)
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
                    color = DarkTextPrimary
                )
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = DarkTextMuted
                )
            }

            // Score badge e.g. +4, +5, +0
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(DarkScorePillBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scoreBadgeText,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkScorePillText
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            content()
        }
    }
}

/**
 * Day sleep row with tap-to-expand category list AND stepper [ — 40 + ] on the right
 */
@Composable
private fun V5DaySleepRow(
    title: String,
    subtitle: String,
    scoreBadgeText: String,
    minutes: Int,
    onMinutesChanged: (Int?) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onToggleExpand)
            ) {
                Text(
                    text = title,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkTextPrimary
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score badge e.g. +4 pts
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(DarkScorePillBg)
                        .clickable(role = Role.Button, onClick = onToggleExpand)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scoreBadgeText,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DarkScorePillText
                    )
                }

                // [ - 40 + ] Stepper
                V5InlineStepper(
                    value = minutes,
                    onDecrement = {
                        val next = (minutes - 5).coerceAtLeast(0)
                        onMinutesChanged(if (next == 0) null else next)
                    },
                    onIncrement = {
                        val next = (minutes + 5).coerceAtMost(360)
                        onMinutesChanged(next)
                    },
                    onNumberClick = onToggleExpand
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
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
            .clickable(role = Role.Button, onClick = onClick)
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
    onMinutesChanged: (Int?) -> Unit,
    step: Int = 15
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
                color = DarkTextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = DarkTextMuted
            )
        }

        V5InlineStepper(
            value = minutes,
            onDecrement = {
                val next = (minutes - step).coerceAtLeast(0)
                onMinutesChanged(if (next == 0) null else next)
            },
            onIncrement = {
                val next = (minutes + step).coerceAtMost(720)
                onMinutesChanged(next)
            }
        )
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
    onNumberClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(BorderStroke(1.dp, DarkGoldBorder), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = DarkTextPrimary
            )
        }

        // Value text
        Box(
            modifier = Modifier
                .width(42.dp)
                .then(if (onNumberClick != null) Modifier.clickable(onClick = onNumberClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkPrimary
            )
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = DarkTextPrimary
            )
        }
    }
}

