package com.sadhana.card.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.CardShape
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkRingTrack
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Returns the dynamic status color based on total score:
 * - Red: totalScore < 75
 * - Yellow: 75 <= totalScore < 200
 * - Green: totalScore == 200
 */
fun getDailyScoreColor(totalScore: Int): Color {
    return when {
        totalScore < 50 -> Color(0xFFEF4444)  // Red
        totalScore < 150 -> Color(0xFFF5B041) // Yellow
        else -> Color(0xFF10B981)             // Green (Full Marks)
    }
}

/**
 * Hero Score Card:
 * - Circular total score indicator (out of 150 pts) with dynamic color thresholds:
 *   - Red (< 50), Yellow (50–149), Green (150 Full Marks)
 * - Individual score breakdown list for all 6 categories in exact sequence:
 *   1. Bedtime (Sleep) [Max: 25 pts]
 *   2. Wake-up [Max: 25 pts]
 *   3. 16 Rounds Japa [Max: 25 pts]
 *   4. Day Sleep / Nap [Max: 25 pts]
 *   5. Pathan (Reading) [Max: 25 pts]
 *   6. Shravan (Listening) [Max: 25 pts]
 */
@Composable
fun ScoreCard(
    score: Int?,
    maxScore: Int = 150,
    bedScore: Int? = null,
    wakeScore: Int? = null,
    japaScore: Int? = null,
    napScore: Int? = null,
    pathanScore: Int? = null,
    sravanScore: Int? = null,
    modifier: Modifier = Modifier
) {
    val total = score ?: 0
    val scoreColor = getDailyScoreColor(total)

    val accessibilityDesc = "Daily total score: $total out of $maxScore points. " +
        "Bed: ${bedScore ?: 0}, Wake: ${wakeScore ?: 0}, Japa: ${japaScore ?: 0}, " +
        "Nap: ${napScore ?: 0}, Pathan: ${pathanScore ?: 0}, Shravan: ${sravanScore ?: 0}."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .padding(vertical = 20.dp, horizontal = 16.dp)
            .semantics { contentDescription = accessibilityDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Large Circular Total Score Indicator ───────────────────────
        TotalScoreCircle(
            totalScore = total,
            maxScore = maxScore,
            scoreColor = scoreColor
        )

        // ── Individual Score Breakdown (Sequence: Bed, Wake, Japa, Nap, Pathan, Shravan) ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Bed (25), Wake (25), Japa (25), Nap (25)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreCircularRing(label = "Bed", score = bedScore ?: 0, maxPoints = 25)
                ScoreCircularRing(label = "Wake", score = wakeScore ?: 0, maxPoints = 25)
                ScoreCircularRing(label = "Japa", score = japaScore ?: 0, maxPoints = 25)
                ScoreCircularRing(label = "Nap", score = napScore ?: 0, maxPoints = 25)
            }

            // Row 2: Pathan (25), Shravan (25)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreCircularRing(label = "Pathan", score = pathanScore ?: 0, maxPoints = 25)
                ScoreCircularRing(label = "Shravan", score = sravanScore ?: 0, maxPoints = 25)
            }
        }
    }
}

/**
 * Large animated circular total score indicator.
 * Displays total score centered inside a radial arc with dynamic Red/Yellow/Green state color.
 */
@Composable
fun TotalScoreCircle(
    totalScore: Int,
    maxScore: Int = 150,
    scoreColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (totalScore.coerceAtLeast(0).toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "TotalScoreSweep"
    )

    val strokeWidth = 8.dp
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val haloWidthPx = with(LocalDensity.current) { (strokeWidth + 4.dp).toPx() }
    val solidRed = Color(0xFFE53935)

    Box(
        modifier = modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(136.dp)) {
            if (totalScore <= 0) {
                // If points <= 0: circle stroke renders in solid red instead of default dark/grey outline
                drawArc(
                    color = solidRed.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = haloWidthPx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = solidRed,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            } else {
                // Subtle glowing halo when score is in red territory
                if (totalScore < 50) {
                    drawArc(
                        color = scoreColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = haloWidthPx, cap = StrokeCap.Round)
                    )
                }

                // Background Ring Track
                drawArc(
                    color = DarkRingTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Dynamic Active Progress Arc
                if (animatedSweep > 0f) {
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Centered Total Score & Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$totalScore",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 38.sp,
                color = if (totalScore < 0) solidRed else DarkTextPrimary
            )
            Text(
                text = "/ $maxScore pts",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = DarkTextMuted
            )
            if (totalScore >= maxScore) {
                Text(
                    text = "Full Marks",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

/**
 * Single Circular Progress Ring for individual categories:
 * - 54.dp size
 * - If points == 0: The circle stroke renders in solid red (Color.Red / #E53935) instead of the default dark/grey outline.
 * - If points < 0: The circle stroke renders in solid red, and inner label displays the negative value.
 * - If points > 0: Maintain existing progress arc coloring (green for full completion, yellow/amber for partial).
 */
@Composable
fun ScoreCircularRing(
    label: String,
    score: Int,
    maxPoints: Int = 25,
    ringColor: Color? = null,
    trackColor: Color = DarkRingTrack,
    modifier: Modifier = Modifier
) {
    val clampedScore = score.coerceIn(0, maxPoints)
    val progress = clampedScore.toFloat() / maxPoints.toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(durationMillis = 500),
        label = "${label}Sweep"
    )

    val strokeWidth = 4.5.dp
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val solidRed = Color(0xFFE53935)

    val activeColor = ringColor ?: when {
        score <= 0 -> solidRed
        clampedScore >= maxPoints -> Color(0xFF10B981) // Green
        else -> DarkPrimary                           // Amber Yellow (#F5B041)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(54.dp)) {
                if (score <= 0) {
                    // Points <= 0: circle stroke must render in solid red (#E53935) instead of default outline
                    drawArc(
                        color = solidRed,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                } else {
                    // Background Track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Active Progress Arc
                    if (animatedSweep > 0f) {
                        drawArc(
                            color = activeColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Numeric score inside
            Text(
                text = if (score > 0) "+$score" else "$score",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (score >= 100 || score <= -10) 12.sp else 14.sp,
                color = if (score < 0) solidRed else DarkTextPrimary
            )
        }

        // Label below ring in muted gray
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = DarkTextMuted
        )
    }
}
