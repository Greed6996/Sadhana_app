package com.sadhana.card.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.CardShape
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Dedication Card matching the V5 Dark Rings design:
 * - Collapsed by default (isExpanded = false), showing the gold title and chevron
 * - Smooth AnimatedVisibility expansion on tap to reveal dedication text and italicized verse
 */
@Composable
fun DedicationBanner(
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "DedicationChevronRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .clickable(role = Role.Button) { isExpanded = !isExpanded }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = "Sadhana Card Dedication: Sri Sri Guru Gouranga Jayate. ${if (isExpanded) "Expanded" else "Collapsed"}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sri Sri Guru Gouranga Jayate",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkPrimary
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse dedication" else "Expand dedication",
                tint = DarkPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Sadhana Card for Sadhaka for the pleasure of my Counselor, Authorities, Sri Gurudev, Srila Prabhupada, Sri Sri Gaur Nitai,",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    color = DarkTextMuted
                )

                Text(
                    text = "yukta ahara viharasya yukta cestasya karmasu / Yukta svapnava bodhasya yogo bhavati dukha ha",
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = DarkTextMuted.copy(alpha = 0.85f)
                )
            }
        }
    }
}
