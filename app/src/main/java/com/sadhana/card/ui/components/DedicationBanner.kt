package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * - Elevated Dark Charcoal card (#17171B), 20.dp rounded corners, 1.dp border (#26262C)
 * - Gold title: "Sri Sri Guru Gouranga Jayate"
 * - Dedication text in muted gray (#8E8E99)
 * - Italicized Sanskrit verse underneath
 */
@Composable
fun DedicationBanner(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), CardShape)
            .padding(16.dp)
            .semantics {
                contentDescription = "Sadhana Card Dedication: Sri Sri Guru Gouranga Jayate"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Sri Sri Guru Gouranga Jayate",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DarkPrimary
        )

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
