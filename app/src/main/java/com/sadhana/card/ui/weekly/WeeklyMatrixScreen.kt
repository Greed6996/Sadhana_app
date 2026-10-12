package com.sadhana.card.ui.weekly

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.theme.BannerShape
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.PillShape
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SaffronAccent
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextDark
import com.sadhana.card.ui.theme.TextMuted
import java.time.LocalDate

/**
 * Screen 2: Weekly Matrix screen rebuilt entirely in native Jetpack Compose.
 */
@Composable
fun WeeklyMatrixScreen(
    viewModel: WeeklyMatrixViewModel,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WeeklyMatrixContent(
        uiState = uiState,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onDayClick = onDayClick,
        modifier = modifier
    )
}

@Composable
fun WeeklyMatrixContent(
    uiState: WeeklyMatrixUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekTitle = DateUtils.formatWeekRange(uiState.startOfWeek)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with week switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onPreviousWeek)
                        .semantics { contentDescription = "Previous Week" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "WEEKLY SADHANA",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = weekTitle,
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onNextWeek)
                        .semantics { contentDescription = "Next Week" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Weekly aggregate banner
        item {
            WeeklyAggregateCard(uiState = uiState)
        }

        // 3. Matrix spreadsheet grid
        item {
            WeeklySpreadsheetGrid(
                columns = uiState.columns,
                onDayClick = onDayClick
            )
        }

        // 4. Score key Legend
        item {
            WeeklyScoreKeyLegend()
        }

        // Bottom navigation spacing
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun WeeklyAggregateCard(
    uiState: WeeklyMatrixUiState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.scoreProgress,
        label = "WeeklyProgress"
    )
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else RustPrimary
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(BannerShape)
        .background(cardBg)

    val finalModifier = if (isDark) {
        baseModifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), BannerShape)
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Weekly score",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFFFBF1DF).copy(alpha = 0.9f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${uiState.weekTotalScore}",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 38.sp,
                    lineHeight = 38.sp,
                    color = Color(0xFFFBF1DF)
                )
                Text(
                    text = " / ${uiState.maxWeeklyScore}",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color(0xFFFBF1DF).copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(PillShape)
                .background(Color(0xFFFBF1DF).copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(PillShape)
                    .background(Color(0xFFF4B26B))
            )
        }

        HorizontalDivider(
            color = Color(0xFFFBF1DF).copy(alpha = 0.25f),
            modifier = Modifier.padding(top = 2.dp)
        )

        // 3 columns: Pathan, Sravan, Read + hear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AggregateColumn(label = "Pathan", value = uiState.pathanHoursMinutes, modifier = Modifier.weight(1f))
            AggregateColumn(label = "Sravan", value = uiState.sravanHoursMinutes, modifier = Modifier.weight(1f))
            AggregateColumn(label = "Read + hear", value = uiState.bothHoursMinutes, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AggregateColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontSize = 11.sp,
            color = Color(0xFFFBF1DF).copy(alpha = 0.85f)
        )
        Text(
            text = value,
            fontFamily = NewsreaderFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Color(0xFFFBF1DF)
        )
    }
}

/**
 * 8-row x 7-column spreadsheet matrix card matching Screen 2.
 */
@Composable
fun WeeklySpreadsheetGrid(
    columns: List<MatrixDayColumn>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), BannerShape)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header row with Day Names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty corner for label
                Box(modifier = Modifier.weight(1.3f))

                columns.forEach { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) { onDayClick(col.date) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = col.dayName,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 1. Bedtime (pts)
            MatrixScoreRow(
                label = "Bedtime",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.bedScore },
                onDayClick = onDayClick
            )

            // 2. Wake-up (pts)
            MatrixScoreRow(
                label = "Wake-up",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.wakeScore },
                onDayClick = onDayClick
            )

            // 3. Day sleep (pts)
            MatrixScoreRow(
                label = "Day sleep",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.napScore },
                onDayClick = onDayClick
            )

            // 4. Japa (pts)
            MatrixScoreRow(
                label = "Japa",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.japaScore },
                onDayClick = onDayClick
            )

            // 5. Pathan (min)
            MatrixMinuteRow(
                label = "Pathan",
                unit = "min",
                columns = columns,
                getMinutes = { it.entry?.pathanMinutes ?: 0 },
                onDayClick = onDayClick
            )

            // 6. Sravan (min)
            MatrixMinuteRow(
                label = "Sravan",
                unit = "min",
                columns = columns,
                getMinutes = { it.entry?.sravanMinutes ?: 0 },
                onDayClick = onDayClick
            )

            // 7. Seva (min)
            MatrixMinuteRow(
                label = "Seva",
                unit = "min",
                columns = columns,
                getMinutes = { it.entry?.sevaMinutes ?: 0 },
                onDayClick = onDayClick
            )

            // 8. Total (of 20)
            MatrixTotalRow(
                label = "Total",
                unit = "of 20",
                columns = columns,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
fun MatrixScoreRow(
    label: String,
    unit: String,
    columns: List<MatrixDayColumn>,
    getScore: (MatrixDayColumn) -> Int?,
    onDayClick: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val score = getScore(col)
            ScoreMatrixCell(
                score = score,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MatrixMinuteRow(
    label: String,
    unit: String,
    columns: List<MatrixDayColumn>,
    getMinutes: (MatrixDayColumn) -> Int,
    onDayClick: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val min = getMinutes(col)
            MinuteMatrixCell(
                minutes = min,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MatrixTotalRow(
    label: String,
    unit: String,
    columns: List<MatrixDayColumn>,
    onDayClick: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val total = col.totalScore
            TotalMatrixCell(
                total = total,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MatrixRowLabel(
    label: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScoreMatrixCell(
    score: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    val (bg, textColor, border) = if (isDark) {
        when (score) {
            5 -> Triple(Color(0xFFF4B26B), Color(0xFF1F140E), null)
            4 -> Triple(Color(0xFFC27338), Color.White, null)
            3 -> Triple(Color(0xFF874820), Color(0xFFFBF1DF), null)
            2 -> Triple(Color(0xFF5A331A), Color(0xFFFBF1DF), null)
            0, 1 -> Triple(Color(0xFF362015), Color(0xFFCDBBA8), BorderStroke(1.dp, Color(0xFF4A3428)))
            else -> Triple(Color(0xFF2A1C14), Color(0xFF7D6755), BorderStroke(1.dp, Color(0xFF3F2B20)))
        }
    } else {
        when (score) {
            5 -> Triple(RustPrimary, Color.White, null)
            4 -> Triple(Color(0xFFB5541A), Color.White, null)
            3 -> Triple(Color(0xFFF0B27A), Color(0xFF3A1C08), null)
            2 -> Triple(Color(0xFFF8DCC0), Color(0xFF4A2A12), null)
            0, 1 -> Triple(Color(0xFFFBEEDD), Color(0xFF4A2A12), BorderStroke(1.dp, BorderCream))
            else -> Triple(Color(0xFFFBEEDD), Color(0xFF8C5228), BorderStroke(1.dp, BorderCream))
        }
    }

    val baseMod = modifier
        .height(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(bg)
        .clickable(role = Role.Button, onClick = onClick)

    val finalMod = if (border != null) baseMod.border(border, RoundedCornerShape(8.dp)) else baseMod

    Box(
        modifier = finalMod,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (score != null) "$score" else "—",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun MinuteMatrixCell(
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$minutes",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun TotalMatrixCell(
    total: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (total != null) "$total" else "—",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WeeklyScoreKeyLegend(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Score key",
            fontFamily = DmSansFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isDark) {
            LegendChip(text = "0–1", bg = Color(0xFF362015), textColor = Color(0xFFCDBBA8), hasBorder = true)
            LegendChip(text = "2", bg = Color(0xFF5A331A), textColor = Color(0xFFFBF1DF))
            LegendChip(text = "3", bg = Color(0xFF874820), textColor = Color(0xFFFBF1DF))
            LegendChip(text = "4", bg = Color(0xFFC27338), textColor = Color.White)
            LegendChip(text = "5", bg = Color(0xFFF4B26B), textColor = Color(0xFF1F140E))
        } else {
            LegendChip(text = "0–1", bg = Color(0xFFFBEEDD), textColor = Color(0xFF4A2A12), hasBorder = true)
            LegendChip(text = "2", bg = Color(0xFFF8DCC0), textColor = Color(0xFF4A2A12))
            LegendChip(text = "3", bg = Color(0xFFF0B27A), textColor = Color(0xFF3A1C08))
            LegendChip(text = "4", bg = Color(0xFFB5541A), textColor = Color.White)
            LegendChip(text = "5", bg = RustPrimary, textColor = Color.White)
        }
    }
}

@Composable
fun LegendChip(
    text: String,
    bg: Color,
    textColor: Color,
    hasBorder: Boolean = false
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val base = Modifier
        .size(width = 26.dp, height = 22.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(bg)

    val mod = if (hasBorder) base.border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp)) else base

    Box(
        modifier = mod,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
