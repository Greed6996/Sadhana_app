package com.sadhana.card.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkScorePillBg
import com.sadhana.card.ui.theme.DarkScorePillText
import com.sadhana.card.ui.theme.DarkScorePillZeroBg
import com.sadhana.card.ui.theme.DarkScorePillZeroText
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.PillShape
import com.sadhana.card.ui.theme.ScorePillBg
import com.sadhana.card.ui.theme.ScorePillText
import com.sadhana.card.ui.theme.ScorePillZeroBg
import com.sadhana.card.ui.theme.ScorePillZeroText

/**
 * Reusable pill component displaying points earned vs maximum (e.g., "5/5 pts", "0/5 pts", or "—").
 * Automatically adapts between Light and Dark themes maintaining >= 4.5:1 text contrast.
 */
@Composable
fun ScorePill(
    score: Int?,
    max: Int = 5,
    modifier: Modifier = Modifier
) {
    val isZero = score == 0
    val isNull = score == null
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    val (bgColor, textColor, hasBorder) = when {
        isNull -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, false)
        isZero -> if (isDark) {
            Triple(DarkScorePillZeroBg, DarkScorePillZeroText, true)
        } else {
            Triple(ScorePillZeroBg, ScorePillZeroText, true)
        }
        else -> if (isDark) {
            Triple(DarkScorePillBg, DarkScorePillText, false)
        } else {
            Triple(ScorePillBg, ScorePillText, false)
        }
    }

    val text = if (score != null) "$score/$max pts" else "— pts"
    val accessibilityLabel = if (score != null) "$score out of $max points" else "No points recorded"

    val baseModifier = modifier
        .semantics { contentDescription = accessibilityLabel }
        .clip(PillShape)
        .background(bgColor)

    val finalModifier = if (hasBorder) {
        baseModifier.border(1.dp, MaterialTheme.colorScheme.outline, PillShape)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier.padding(horizontal = 11.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
    }
}
