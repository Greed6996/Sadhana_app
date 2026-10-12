package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.CardShape
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.TileShape

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/**
 * Clean card container for a daily sadhana entry item.
 * Adapts to theme tokens dynamically.
 */
@Composable
fun SadhanaEntryCard(
    title: String,
    hint: String,
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    score: Int? = null,
    maxScore: Int = 5,
    modifier: Modifier = Modifier,
    trailingControl: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CardShape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(TileShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (iconContent != null) {
                    iconContent()
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hint,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (score != null || maxScore > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ScorePill(score = score, max = maxScore)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Stepper / Time
            trailingControl()
        }
    }
}
