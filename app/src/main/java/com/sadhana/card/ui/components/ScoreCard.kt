package com.sadhana.card.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.BannerShape
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.DividerCream
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.PillShape
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextMuted

/**
 * Daily Score header card displaying earned points, total possible (20),
 * and an animated progress bar.
 */
@Composable
fun ScoreCard(
    score: Int?,
    maxScore: Int = 20,
    modifier: Modifier = Modifier
) {
    val currentScore = score ?: 0
    val progress = (currentScore.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "ScoreProgress")

    val accessibilityDesc = if (score != null) {
        "Daily total score: $score out of $maxScore points."
    } else {
        "Daily total score not yet calculated."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, BorderCream), BannerShape)
            .padding(16.dp)
            .semantics { contentDescription = accessibilityDesc },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Daily Score",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextMuted
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${score ?: 0}",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    color = RustPrimary
                )
                Text(
                    text = " / $maxScore",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = TextMuted,
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
                .background(DividerCream)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(PillShape)
                    .background(RustPrimary)
            )
        }

        Text(
            text = "Sleep (5), Wake (5), Day Nap (5), and Japa (5) scored",
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}
