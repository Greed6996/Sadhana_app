package com.sadhana.card.ui.components

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
 * Hero Score Card matching the V5 Dark Rings design:
 * - Centered total score: "16 / 20 pts today"
 * - 4 Circular Progress Rings for Bed, Wake, Nap, and Japa (0-5 pts each)
 */
@Composable
fun ScoreCard(
    score: Int?,
    maxScore: Int = 20,
    bedScore: Int? = null,
    wakeScore: Int? = null,
    napScore: Int? = null,
    japaScore: Int? = null,
    modifier: Modifier = Modifier
) {
    val total = score ?: 0

    val accessibilityDesc = "Daily total score: $total out of $maxScore points. " +
        "Bed: ${bedScore ?: 0}, Wake: ${wakeScore ?: 0}, Nap: ${napScore ?: 0}, Japa: ${japaScore ?: 0}."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .padding(vertical = 22.dp, horizontal = 16.dp)
            .semantics { contentDescription = accessibilityDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Centered Total Score Header: "16 / 20 pts today"
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$total",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 44.sp,
                color = DarkTextPrimary
            )
            Text(
                text = " / $maxScore pts today",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = DarkTextMuted,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }

        // 4 Circular Progress Rings side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreCircularRing(label = "Bed", score = bedScore ?: 0)
            ScoreCircularRing(label = "Wake", score = wakeScore ?: 0)
            ScoreCircularRing(label = "Nap", score = napScore ?: 0)
            ScoreCircularRing(label = "Japa", score = japaScore ?: 0)
        }
    }
}

/**
 * Single Circular Progress Ring component:
 * - 54.dp size
 * - Dark track (#26262C)
 * - Gold arc (#F5B041) sweeping (score / 5f) * 360f
 * - Score number centered inside in bold white
 * - Label underneath in muted gray
 */
@Composable
fun ScoreCircularRing(
    label: String,
    score: Int,
    maxPoints: Int = 5,
    ringColor: Color = DarkPrimary,
    trackColor: Color = DarkRingTrack,
    modifier: Modifier = Modifier
) {
    val progress = (score.coerceIn(0, maxPoints).toFloat() / maxPoints.toFloat())
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(durationMillis = 500),
        label = "${label}Sweep"
    )

    val strokeWidth = 5.5.dp
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(54.dp)) {
                // Background Track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Gold Progress Arc
                if (animatedSweep > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            // Numeric score inside in bold white
            Text(
                text = "$score",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DarkTextPrimary
            )
        }

        // Label below ring in muted gray
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = DarkTextMuted
        )
    }
}
