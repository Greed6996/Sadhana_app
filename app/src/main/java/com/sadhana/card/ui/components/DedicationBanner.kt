package com.sadhana.card.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.BannerShape
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.RustPrimary

/**
 * Expandable devotional banner celebrating Srila Prabhupada and the Sadhana Card.
 */
@Composable
fun DedicationBanner(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(RustPrimary)
            .clickable(role = Role.Button) { isExpanded = !isExpanded }
            .padding(16.dp)
            .semantics {
                contentDescription = "Sadhana Card Dedication. Tap to ${if (isExpanded) "collapse" else "expand"}."
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sadhana Card for Sadhaka",
                fontFamily = NewsreaderFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color(0xFFFBF1DF)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFF8DCC0)
            )
        }

        Text(
            text = "Offered with humility and devotion at the lotus feet of His Divine Grace A.C. Bhaktivedanta Swami Prabhupāda.",
            fontFamily = DmSansFontFamily,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = Color(0xFFFBF1DF).copy(alpha = 0.92f)
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(
                    color = Color(0xFFFBF1DF).copy(alpha = 0.25f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "śrī-caitanya-mano-'bhīṣṭaṁ sthāpitaṁ yena bhū-tale\nsvayaṁ rūpaḥ kadā mahyaṁ dadāti sva-padāntikam",
                    fontFamily = NewsreaderFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFFBF1DF)
                )
                Text(
                    text = "A sincere sadhaka regulates bodily demands of sleep and eating in order to dedicate maximum energy to chanting the holy names and hearing transcendental literatures.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFFBF1DF).copy(alpha = 0.85f)
                )
            }
        }
    }
}
