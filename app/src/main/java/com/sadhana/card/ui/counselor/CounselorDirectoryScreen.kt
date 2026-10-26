package com.sadhana.card.ui.counselor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkGoldBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkSurfaceVariant
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.weekly.MatrixDayColumn
import com.sadhana.card.ui.weekly.WeeklyMatrixUiState

// Percentage color boundaries: Red (<80%), Yellow (80–99%), Green (100%)
fun getPercentageBadgeColor(percentage: Int): Color {
    return when {
        percentage >= 100 -> Color(0xFF10B981) // Green
        percentage in 80..99 -> Color(0xFFF59E0B) // Yellow
        else -> Color(0xFFEF4444) // Red (< 80%)
    }
}

// Matrix cell colors matching V5
private val CellScore5Bg = Color(0xFF10B981)
private val CellScore5Text = Color(0xFFF4F4F6)
private val CellScore1to4Bg = Color(0xFFF5B041)
private val CellScore1to4Text = Color(0xFF0D0D0F)
private val CellScore0Bg = Color(0xFFEF4444)
private val CellScore0Text = Color(0xFFF4F4F6)
private val CellUnloggedBg = Color(0xFF222228)
private val CellUnloggedText = Color(0xFF8E8E99)

@Composable
fun CounselorDirectoryScreen(
    viewModel: CounselorDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDevotee by viewModel.selectedDevotee.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (selectedDevotee != null) {
            DevoteeWeeklyReadOnlyView(
                devotee = selectedDevotee!!,
                viewModel = viewModel,
                onBack = { viewModel.clearSelectedDevotee() }
            )
        } else {
            DevoteeDirectoryListView(viewModel = viewModel)
        }
    }
}

@Composable
private fun DevoteeDirectoryListView(
    viewModel: CounselorDashboardViewModel
) {
    val devotees by viewModel.devotees.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Devotee Directory",
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "Counselor Overview • Lowest % First",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = DarkTextMuted
                    )
                }

                IconButton(onClick = { viewModel.loadDevotees() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh devotees",
                        tint = DarkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search devotees...",
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        color = DarkTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = DarkTextMuted
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkGoldBorder,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (isLoading && devotees.isEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkPrimary)
                }
            }
        } else if (errorMessage != null && devotees.isEmpty()) {
            item(key = "error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error loading devotees",
                        color = Color(0xFFEF4444),
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (devotees.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No devotees match \"$searchQuery\"" else "No devotees found in the community yet.",
                        color = DarkTextMuted,
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(devotees, key = { it.uid }) { devotee ->
                DevoteeCard(
                    devotee = devotee,
                    onClick = { viewModel.selectDevotee(devotee) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DevoteeCard(
    devotee: DevoteeSummary,
    onClick: () -> Unit
) {
    val badgeColor = getPercentageBadgeColor(devotee.currentWeekPercentage)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Devotee avatar icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(BorderStroke(1.dp, DarkBorder), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = DarkPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = devotee.name,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (devotee.counselorName.isNotBlank()) {
                    "Counselor: ${devotee.counselorName}"
                } else if (devotee.email.isNotBlank()) {
                    devotee.email
                } else {
                    "Score: ${devotee.currentWeekScore} pts"
                }
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = DarkTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Percentage pill badge (Red <80%, Yellow 80-99%, Green 100%)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.16f))
                    .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${devotee.currentWeekPercentage}%",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = DarkTextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DevoteeWeeklyReadOnlyView(
    devotee: DevoteeSummary,
    viewModel: CounselorDashboardViewModel,
    onBack: () -> Unit
) {
    val weeklyState by viewModel.devoteeWeeklyState.collectAsStateWithLifecycle()
    val selectedWeekStart by viewModel.selectedWeekStart.collectAsStateWithLifecycle()
    val isLoadingWeekly by viewModel.isLoadingWeekly.collectAsStateWithLifecycle()

    val weekTitle = DateUtils.formatWeekRange(selectedWeekStart)
    val maxScore = 1050
    val totalScore = weeklyState?.weekTotalScore ?: 0
    val percentage = ((totalScore.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) * 100).toInt()
    val badgeColor = getPercentageBadgeColor(percentage)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "top_bar") {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Directory",
                        tint = DarkPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = devotee.name,
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "Read-Only Weekly Report",
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.5.sp,
                        color = DarkTextMuted
                    )
                }
            }
        }

        // Week Navigation Bar
        item(key = "week_nav") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousWeek() }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Week",
                        tint = DarkTextPrimary
                    )
                }

                Text(
                    text = weekTitle,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkTextPrimary
                )

                IconButton(onClick = { viewModel.nextWeek() }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Week",
                        tint = DarkTextPrimary
                    )
                }
            }
        }

        // Summary Card
        item(key = "summary_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Completion",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = DarkTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalScore / $maxScore pts",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Reading: ${weeklyState?.totalPathanMinutes ?: 0}m • Hearing: ${weeklyState?.totalSravanMinutes ?: 0}m",
                            fontFamily = DmSansFontFamily,
                            fontSize = 11.sp,
                            color = DarkTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .border(BorderStroke(1.5.dp, badgeColor), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$percentage%",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = badgeColor
                        )
                    }
                }
            }
        }

        // Matrix Grid
        if (isLoadingWeekly) {
            item(key = "matrix_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkPrimary)
                }
            }
        } else if (weeklyState != null) {
            item(key = "matrix_grid") {
                ReadOnlyMatrixGrid(state = weeklyState!!)
            }
        }

        item(key = "read_only_notice") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒 Read-only view for spiritual counselors. Entries cannot be altered.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    color = DarkTextMuted,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ReadOnlyMatrixGrid(state: WeeklyMatrixUiState) {
    val columns = state.columns

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Day Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "METRIC",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = DarkTextMuted
                )
            }
            columns.forEach { col ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = col.dayName.take(2).uppercase(),
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = DarkTextMuted
                        )
                        Text(
                            text = col.dayNumber,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = DarkTextPrimary
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

        // Bedtime
        MatrixMetricRow("Bedtime", columns, maxScore = 25) { it.scores.bedScore }
        // Wake-up
        MatrixMetricRow("Wake-up", columns, maxScore = 25) { it.scores.wakeScore }
        // 16 rounds
        MatrixMetricRow("16 Rnds", columns, maxScore = 25) { it.scores.japaScore }
        // Day Sleep
        MatrixMetricRow("Day Sleep", columns, maxScore = 25) { it.scores.napScore }

        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

        // Pathan
        MatrixMetricRow("Pathan", columns, maxScore = 25) { it.scores.pathanScore }
        // Sravan
        MatrixMetricRow("Sravan", columns, maxScore = 25) { it.scores.sravanScore }

        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp)

        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "TOTAL",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DarkPrimary
                )
            }
            columns.forEach { col ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val total = col.totalScore
                    Text(
                        text = total?.toString() ?: "—",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (total != null) DarkPrimary else DarkTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixMetricRow(
    title: String,
    columns: List<MatrixDayColumn>,
    maxScore: Int = 25,
    scoreSelector: (MatrixDayColumn) -> Int?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1.3f)) {
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                color = DarkTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        columns.forEach { col ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                val score = scoreSelector(col)
                if (score != null) {
                    val (bg, text) = when {
                        score <= 0 -> CellScore0Bg to CellScore0Text
                        score >= maxScore -> CellScore5Bg to CellScore5Text
                        else -> CellScore1to4Bg to CellScore1to4Text
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${score.coerceAtLeast(0)}",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = text
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(CellUnloggedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "—",
                            fontFamily = DmSansFontFamily,
                            fontSize = 10.sp,
                            color = CellUnloggedText
                        )
                    }
                }
            }
        }
    }
}
