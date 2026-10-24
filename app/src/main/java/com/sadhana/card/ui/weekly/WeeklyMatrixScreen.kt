package com.sadhana.card.ui.weekly

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.sadhana.card.ui.theme.CardShape
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkGoldBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkRingTrack
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkSurfaceVariant
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily
import java.io.File
import java.time.LocalDate

// Heatmap Cell Colors matching V5 Specifications: 0 = Red, 1–4 = Yellow, 5 = Green, unlogged = Muted Slate
private val CellScore5Bg = Color(0xFF10B981)
private val CellScore5Text = Color(0xFFF4F4F6)
private val CellScore1to4Bg = Color(0xFFF5B041)
private val CellScore1to4Text = Color(0xFF0D0D0F)
private val CellScore0Bg = Color(0xFFEF4444)
private val CellScore0Text = Color(0xFFF4F4F6)
private val CellUnloggedBg = Color(0xFF222228)
private val CellUnloggedText = Color(0xFF8E8E99)
private val CellMinutesBg = Color(0xFF1E1E24)
private val CellTotalBg = Color(0xFFF4F4F6)
private val CellTotalText = Color(0xFF0D0D0F)

/**
 * Weekly Matrix screen matching the V5 Dark Rings aesthetic.
 * Preserves 100% of Excel exporting, sharing, WhatsApp feedback, and day navigation.
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
            .background(DarkBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Week Navigation Header
        item(key = "header_weekly") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Week (<) Circular Dark Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                        .clickable(role = Role.Button, onClick = onPreviousWeek)
                        .semantics { contentDescription = "Previous Week" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = DarkTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center: "Weekly sadhana" + date range title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Weekly sadhana",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = DarkTextMuted
                    )
                    Text(
                        text = weekTitle,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        color = DarkTextPrimary
                    )
                }

                // Next Week (>) Circular Dark Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                        .clickable(role = Role.Button, onClick = onNextWeek)
                        .semantics { contentDescription = "Next Week" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = DarkTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Weekly Summary Hero Card (Circular Progress Ring + Study Totals)
        item(key = "hero_weekly_summary") {
            WeeklySummaryHeroCard(uiState = uiState)
        }

        // 3. Unified Weekly Heatmap Matrix Card
        item(key = "card_spreadsheet_grid") {
            WeeklyHeatmapMatrixCard(
                columns = uiState.columns,
                onDayClick = onDayClick
            )
        }

        // 4. Excel Export Card
        item(key = "card_excel_export") {
            WeeklyExcelExportCard(
                uiState = uiState,
                onShareReport = onShareReport,
                onSaveReport = onSaveReport
            )
        }

        // 5. Feedback & Feature Requests Section
        item(key = "card_feedback") {
            FeedbackAndFeatureRequestsSection()
        }

        // Bottom navigation clearance
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * V5 Weekly Summary Hero Card:
 * Left side: Large circular ring showing total score e.g. "103 / of 140"
 * Right side: Study breakdown rows (Pathan, Sravan, Read + hear) with bold gold numbers
 */
@Composable
fun WeeklySummaryHeroCard(
    uiState: WeeklyMatrixUiState,
    modifier: Modifier = Modifier
) {
    val maxScore = uiState.maxWeeklyScore.toFloat()
    val progress = (uiState.weekTotalScore.toFloat() / maxScore).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "WeeklyRingSweep"
    )
    val animatedScore by animateIntAsState(
        targetValue = uiState.weekTotalScore,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "WeeklyScoreCounter"
    )

    val strokeWidth = 7.dp
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Circular Progress Ring
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    // Background Track
                    drawArc(
                        color = DarkRingTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Gold Progress Arc
                    if (animatedSweep > 0f) {
                        drawArc(
                            color = DarkPrimary,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }

                // Centered Score & "of 1400"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$animatedScore",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "of ${uiState.maxWeeklyScore}",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = DarkTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right Side: Study Breakdown (3 rows with subtle dividers)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyStudyRow(label = "Pathan", value = uiState.pathanHoursMinutes)
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)
                WeeklyStudyRow(label = "Sravan", value = uiState.sravanHoursMinutes)
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)
                WeeklyStudyRow(label = "Read + hear", value = uiState.bothHoursMinutes)
            }
        }
    }
}

@Composable
private fun WeeklyStudyRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = DarkTextMuted
        )
        Text(
            text = value,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = DarkPrimary
        )
    }
}

/**
 * 8-row x 7-column spreadsheet matrix card matching the V5 design.
 */
@Composable
fun WeeklyHeatmapMatrixCard(
    columns: List<MatrixDayColumn>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // Header row with Day Names (Sun through Sat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty corner for row labels
                Box(modifier = Modifier.weight(1.3f))

                columns.forEach { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(role = Role.Button) { onDayClick(col.date) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = col.dayName,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = DarkTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 1. Bedtime (pts)
            V5MatrixScoreRow(
                label = "Bedtime",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.bedScore },
                onDayClick = onDayClick
            )

            // 2. Wake-up (pts)
            V5MatrixScoreRow(
                label = "Wake-up",
                unit = "pts",
                columns = columns,
                getScore = { it.scores.wakeScore },
                onDayClick = onDayClick
            )

            // 3. Japa (pts) - Sequence: Japa before Day sleep
            V5MatrixScoreRow(
                label = "Japa",
                unit = "pts",
                columns = columns,
                maxScore = 50,
                getScore = { it.scores.japaScore },
                onDayClick = onDayClick
            )

            // 4. Day sleep (pts) - Sequence: Day sleep after 16 Rounds Japa
            V5MatrixScoreRow(
                label = "Day sleep",
                unit = "pts",
                columns = columns,
                maxScore = 25,
                getScore = { it.scores.napScore },
                onDayClick = onDayClick
            )

            // 5. Pathan (pts)
            V5MatrixScoreRow(
                label = "Pathan",
                unit = "pts",
                columns = columns,
                maxScore = 25,
                getScore = { it.scores.pathanScore },
                onDayClick = onDayClick
            )

            // 6. Sravan (pts)
            V5MatrixScoreRow(
                label = "Sravan",
                unit = "pts",
                columns = columns,
                maxScore = 25,
                getScore = { it.scores.sravanScore },
                onDayClick = onDayClick
            )

            // 7. Seva (pts)
            V5MatrixScoreRow(
                label = "Seva",
                unit = "pts",
                columns = columns,
                maxScore = 25,
                getScore = { it.scores.sevaScore },
                onDayClick = onDayClick
            )

            // 8. Total (of 200)
            V5MatrixTotalRow(
                label = "Total",
                unit = "of 200",
                columns = columns,
                onDayClick = onDayClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Score Key Legend Row at bottom of matrix card
            WeeklyScoreKeyLegend()
        }
    }
}

@Composable
private fun V5MatrixScoreRow(
    label: String,
    unit: String,
    columns: List<MatrixDayColumn>,
    getScore: (MatrixDayColumn) -> Int?,
    onDayClick: (LocalDate) -> Unit,
    maxScore: Int = 25
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V5MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val score = getScore(col)
            V5ScoreMatrixCell(
                score = score,
                maxScore = maxScore,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun V5MatrixMinuteRow(
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
        V5MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val min = getMinutes(col)
            V5MinuteMatrixCell(
                minutes = min,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun V5MatrixTotalRow(
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
        V5MatrixRowLabel(label = label, unit = unit, modifier = Modifier.weight(1.3f))

        columns.forEach { col ->
            val total = col.totalScore
            V5TotalMatrixCell(
                total = total,
                onClick = { onDayClick(col.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun V5MatrixRowLabel(
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
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            color = DarkTextPrimary
        )
        Text(
            text = unit,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            softWrap = false,
            color = DarkTextMuted
        )
    }
}

/**
 * Scored matrix cell (0–5 pts or 0–25/50 pts) using Red (0), Yellow (partial), Green (max), and Muted Slate (null).
 */
@Composable
private fun V5ScoreMatrixCell(
    score: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxScore: Int = 25
) {
    val (bg, textColor) = when {
        score == null -> Pair(CellUnloggedBg, CellUnloggedText)
        score <= 0 -> Pair(CellScore0Bg, CellScore0Text)
        score >= maxScore -> Pair(CellScore5Bg, CellScore5Text)
        else -> Pair(CellScore1to4Bg, CellScore1to4Text)
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (score != null) "${score.coerceAtLeast(0)}" else "-",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = textColor
        )
    }
}

/**
 * Minute matrix cell (Pathan, Sravan, Seva) with dark fill and crisp white text.
 */
@Composable
private fun V5MinuteMatrixCell(
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CellMinutesBg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$minutes",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp,
            color = DarkTextPrimary
        )
    }
}

/**
 * Daily Total cell with crisp white background and bold dark text.
 */
@Composable
private fun V5TotalMatrixCell(
    total: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CellTotalBg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (total != null) "$total" else "-",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = CellTotalText
        )
    }
}

/**
 * Score Key Legend matching the 3 primary tiers:
 * 0 (Red), 1–4 (Yellow), 5 (Green)
 */
@Composable
fun WeeklyScoreKeyLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Score key",
            fontFamily = DmSansFontFamily,
            fontSize = 11.5.sp,
            color = DarkTextMuted
        )

        V5LegendChip(text = "0", bg = CellScore0Bg, textColor = CellScore0Text)
        V5LegendChip(text = "1–4", bg = CellScore1to4Bg, textColor = CellScore1to4Text, width = 36.dp)
        V5LegendChip(text = "5", bg = CellScore5Bg, textColor = CellScore5Text)
    }
}

@Composable
private fun V5LegendChip(
    text: String,
    bg: Color,
    textColor: Color,
    width: androidx.compose.ui.unit.Dp = 28.dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = 22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
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
 * Safe helper to construct a WhatsApp click-to-chat URL and launch it via Intent.ACTION_VIEW.
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
 * Feedback & Feature Requests section styled with V5 Dark Rings aesthetic.
 */
@Composable
fun FeedbackAndFeatureRequestsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .clickable(role = Role.Button) {
                val message = "Hello Developer! 👋\n\nI would like to share feedback / request a feature / report an issue in the Sadhana Card app."
                launchWhatsApp(context, message)
            }
            .semantics {
                contentDescription = "Request a Feature or Report a Bug via WhatsApp"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF172554))
                .border(BorderStroke(1.dp, Color(0xFF1E40AF)), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "Request a Feature or Report a Bug",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkTextPrimary
            )
            Text(
                text = "Share an idea, suggest an improvement, or report a problem you've found.",
                fontFamily = DmSansFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = DarkTextMuted
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DarkTextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Excel Export action card allowing users to share or save the weekly XLSX report.
 */
@Composable
fun WeeklyExcelExportCard(
    uiState: WeeklyMatrixUiState,
    onShareReport: () -> Unit,
    onSaveReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
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
                        .background(Color(0xFF143823))
                        .border(BorderStroke(1.dp, Color(0xFF1E5B37)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Excel Export",
                        tint = Color(0xFF81C995),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export Weekly Excel (.xlsx)",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "Generates formatted spreadsheet based on the ISKCON Sadhana template.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = DarkTextMuted,
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
                    .background(DarkSurfaceVariant)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 $sadhakaDisplay • 🙏 $counselorDisplay",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    color = DarkTextMuted,
                    maxLines = 1
                )
                Text(
                    text = "📅 $weekRange",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkPrimary,
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
                        containerColor = Color(0xFF2E6340),
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
                        containerColor = DarkSurfaceVariant,
                        contentColor = DarkTextPrimary
                    ),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DarkPrimary
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
