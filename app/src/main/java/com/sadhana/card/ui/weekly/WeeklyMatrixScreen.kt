package com.sadhana.card.ui.weekly

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import java.io.File
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
    val context = LocalContext.current
    var fileToSave by remember { mutableStateOf<File?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null && fileToSave != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    fileToSave!!.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Excel report saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val onShareReport = {
        viewModel.exportWeeklyReport(
            onSuccess = { file ->
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Weekly Sadhana Report: ${file.name}")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hare Krishna! Attached is the weekly Sadhana Card spreadsheet report for ${DateUtils.formatWeekRange(uiState.startOfWeek)}."
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Sadhana Excel Report"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sharing report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        )
    }

    val onSaveReport = {
        viewModel.exportWeeklyReport(
            onSuccess = { file ->
                fileToSave = file
                saveFileLauncher.launch(file.name)
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        )
    }

    WeeklyMatrixContent(
        uiState = uiState,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onDayClick = onDayClick,
        onShareReport = onShareReport,
        onSaveReport = onSaveReport,
        modifier = modifier
    )
}

@Composable
fun WeeklyMatrixContent(
    uiState: WeeklyMatrixUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onShareReport: () -> Unit,
    onSaveReport: () -> Unit,
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
                        fontSize = 11.5.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = weekTitle,
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
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

        // 4. Excel Export Card
        item {
            WeeklyExcelExportCard(
                uiState = uiState,
                onShareReport = onShareReport,
                onSaveReport = onSaveReport
            )
        }

        // 5. Score key Legend
        item {
            WeeklyScoreKeyLegend()
        }

        // 6. Feedback & Feature Requests Section
        item {
            FeedbackAndFeatureRequestsSection()
        }

        // Bottom navigation spacing
        item {
            Spacer(modifier = Modifier.height(72.dp))
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

// Developer WhatsApp phone number constant (international format without +)
private const val DEVELOPER_WHATSAPP_NUMBER = "916002630803"

/**
 * Types of feedback and feature requests sent via WhatsApp click-to-chat.
 */
enum class FeedbackType(
    val title: String,
    val description: String,
    val prefilledMessage: String
) {
    REQUEST_FEATURE(
        title = "Request a Feature",
        description = "Have an idea for a new feature? Let us know.",
        prefilledMessage = "Hello Developer! 👋\n\nI have a suggestion for a new feature."
    ),
    REPORT_BUG(
        title = "Report a Bug",
        description = "Found a problem? Help us identify and fix it.",
        prefilledMessage = "Hello Developer! 👋\n\nI found a bug in the app."
    )
}

/**
 * Safe helper to construct a WhatsApp click-to-chat URL and launch it via Intent.ACTION_VIEW.
 * Gracefully falls back to browser or shows an error Toast if neither is available.
 */
fun launchWhatsApp(context: Context, message: String) {
    try {
        val encodedMessage = Uri.encode(message)
        val url = "https://wa.me/$DEVELOPER_WHATSAPP_NUMBER?text=$encodedMessage"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Could not open WhatsApp or browser. Please install WhatsApp or a web browser.",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Unable to open link: ${e.localizedMessage ?: "Unknown error"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Feedback type enum for the unified feedback form.
 */
enum class FeedbackFormType(val displayName: String) {
    FEATURE_REQUEST("Feature Request"),
    BUG_REPORT("Bug Report")
}

/**
 * Polished "Feedback & Feature Requests" section on the Weekly page.
 * Displays a single unified entry that opens a modal form allowing users
 * to submit either a Feature Request or a Bug Report.
 */
@Composable
fun FeedbackAndFeatureRequestsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    var showFeedbackDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF14281A) else Color(0xFFE8F8EE))
                    .border(
                        BorderStroke(1.dp, if (isDark) Color(0xFF1E462B) else Color(0xFFC1EACD)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Feedback & Feature Requests",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Help us improve the app. Your suggestions and feedback matter.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Single Unified Entry Row
        UnifiedFeedbackActionRow(
            title = "Request a Feature or Report a Bug",
            description = "Share an idea, suggest an improvement, or report a problem you've found.",
            icon = Icons.AutoMirrored.Filled.Chat,
            iconTint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
            iconBg = if (isDark) Color(0xFF172554) else Color(0xFFEFF6FF),
            iconBorder = if (isDark) Color(0xFF1E40AF) else Color(0xFFBFDBFE),
            onClick = { showFeedbackDialog = true }
        )
    }

    if (showFeedbackDialog) {
        UnifiedFeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { formType, subject, description, reproSteps, expectedActual ->
                val typeLabel = formType.displayName
                val message = buildString {
                    append("Hello Developer! 👋\n\n")
                    append("📝 *Feedback Type:* $typeLabel\n")
                    append("📌 *Subject:* $subject\n\n")
                    append("💬 *Description:*\n$description\n")
                    if (formType == FeedbackFormType.BUG_REPORT) {
                        if (reproSteps.isNotBlank()) {
                            append("\n🔁 *Steps to Reproduce:*\n$reproSteps\n")
                        }
                        if (expectedActual.isNotBlank()) {
                            append("\n⚖️ *Expected vs Actual Behavior:*\n$expectedActual\n")
                        }
                    }
                    append("\n_Sent via ISKCON Sadhana Card App_")
                }
                launchWhatsApp(context, message)
                showFeedbackDialog = false
            }
        )
    }
}

@Composable
fun UnifiedFeedbackActionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    iconBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val rowBg = if (isDark) Color(0xFF1F140E) else Color(0xFFFDFBF7)
    val rowBorder = if (isDark) Color(0xFF3F2B20) else Color(0xFFEFE4D1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(BorderStroke(1.dp, rowBorder), RoundedCornerShape(14.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "$title: $description"
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg)
                .border(BorderStroke(1.dp, iconBorder), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontFamily = DmSansFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Unified feedback dialog supporting both Feature Request and Bug Report workflows.
 */
@Composable
fun UnifiedFeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        type: FeedbackFormType,
        subject: String,
        description: String,
        reproSteps: String,
        expectedActual: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedbackFormType.FEATURE_REQUEST) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reproSteps by remember { mutableStateOf("") }
    var expectedActual by remember { mutableStateOf("") }

    var subjectTouched by remember { mutableStateOf(false) }
    var descriptionTouched by remember { mutableStateOf(false) }

    val isSubjectError = subjectTouched && subject.isBlank()
    val isDescriptionError = descriptionTouched && description.isBlank()

    val isDark = MaterialTheme.colorScheme.background == DarkBg

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Feedback",
                            fontFamily = NewsreaderFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Help us improve Sadhana Card",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Type Selection: Feature Request vs Bug Report
                Text(
                    text = "Feedback Type",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isFeature = selectedType == FeedbackFormType.FEATURE_REQUEST
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFeature) {
                                    if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                BorderStroke(
                                    if (isFeature) 2.dp else 1.dp,
                                    if (isFeature) (if (isDark) Color(0xFFD97706) else Color(0xFFB45309))
                                    else MaterialTheme.colorScheme.outline
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = FeedbackFormType.FEATURE_REQUEST }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (isFeature) (if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309))
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Feature",
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (isFeature) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isFeature) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isBug = selectedType == FeedbackFormType.BUG_REPORT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isBug) {
                                    if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                BorderStroke(
                                    if (isBug) 2.dp else 1.dp,
                                    if (isBug) (if (isDark) Color(0xFFDC2626) else Color(0xFFB91C1C))
                                    else MaterialTheme.colorScheme.outline
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = FeedbackFormType.BUG_REPORT }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = if (isBug) (if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626))
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Bug Report",
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (isBug) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isBug) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Subject Field
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (subjectTouched) subjectTouched = false
                    },
                    label = { Text("Subject *") },
                    placeholder = {
                        Text(
                            if (selectedType == FeedbackFormType.FEATURE_REQUEST) "e.g. Export to PDF format"
                            else "e.g. Score calculation on Sunday"
                        )
                    },
                    isError = isSubjectError,
                    supportingText = if (isSubjectError) {
                        { Text("Subject is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (descriptionTouched) descriptionTouched = false
                    },
                    label = { Text("Detailed Description *") },
                    placeholder = {
                        Text(
                            if (selectedType == FeedbackFormType.FEATURE_REQUEST) "Describe your suggestion and why it would be helpful..."
                            else "Describe what happened and any relevant details..."
                        )
                    },
                    isError = isDescriptionError,
                    supportingText = if (isDescriptionError) {
                        { Text("Description is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Conditional Bug Report Fields
                if (selectedType == FeedbackFormType.BUG_REPORT) {
                    OutlinedTextField(
                        value = reproSteps,
                        onValueChange = { reproSteps = it },
                        label = { Text("Steps to Reproduce (optional)") },
                        placeholder = { Text("1. Tap on Sunday...\n2. Select 9:30 PM...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = expectedActual,
                        onValueChange = { expectedActual = it },
                        label = { Text("Expected vs Actual Behavior (optional)") },
                        placeholder = { Text("Expected: score 5\nActual: showed 0") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text(
                    text = "Submitting will open WhatsApp to send your feedback directly to the developer.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            subjectTouched = true
                            descriptionTouched = true
                            if (subject.isNotBlank() && description.isNotBlank()) {
                                onSubmit(
                                    selectedType,
                                    subject.trim(),
                                    description.trim(),
                                    reproSteps.trim(),
                                    expectedActual.trim()
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RustPrimary
                        )
                    ) {
                        Text(
                            text = "Submit",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBF1DF)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated, beautifully styled Excel Export action card allowing users to share or save
 * the authentic weekly XLSX report.
 */
@Composable
fun WeeklyExcelExportCard(
    uiState: WeeklyMatrixUiState,
    onShareReport: () -> Unit,
    onSaveReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFDFBF7)
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFEFE4D1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), BannerShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF143823) else Color(0xFFE6F4EA))
                        .border(
                            BorderStroke(1.dp, if (isDark) Color(0xFF1E5B37) else Color(0xFFCEEAD6)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Excel Export",
                        tint = if (isDark) Color(0xFF81C995) else Color(0xFF137333),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export Weekly Excel (.xlsx)",
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Generates formatted spreadsheet based on the ISKCON Sadhana template.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Info details: Sadhaka, Counselor, Week
            val sadhakaDisplay = uiState.sadhakaName.ifBlank { "Sadhaka" }
            val counselorDisplay = uiState.counselorName.ifBlank { "Counselor" }
            val weekRange = DateUtils.formatWeekRange(uiState.startOfWeek)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF1F140E) else Color(0xFFF7F2E9))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 $sadhakaDisplay • 🙏 $counselorDisplay",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "📅 $weekRange",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            // Buttons: Share Sheet & Save as File
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareReport,
                    enabled = !uiState.isExporting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF2E6340) else Color(0xFF137333),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exporting...",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Report",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Button(
                    onClick = onSaveReport,
                    enabled = !uiState.isExporting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFE8DB),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save File",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

