package com.sadhana.card.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Bottom navigation bar matching the V5 Dark Rings design:
 * Clean minimalist bar with "Daily" and "Weekly" tabs.
 * Active tab features Warm Saffron Gold text with a gold underline indicator bar.
 * Top edge features a subtle 1.dp divider, and navigationBarsPadding ensures
 * complete clearance over 3-button navigation systems.
 */
@Composable
fun SadhanaBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCounselor: Boolean = false
) {
    val items = if (isCounselor) {
        listOf(Screen.Daily, Screen.Weekly, Screen.Counselor)
    } else {
        listOf(Screen.Daily, Screen.Weekly)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val contentColor = if (isSelected) DarkPrimary else DarkTextMuted

                Column(
                    modifier = Modifier
                        .clickable(role = Role.Tab) {
                            if (!isSelected) {
                                onNavigateToRoute(screen.route)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics {
                            contentDescription = "${screen.title} tab${if (isSelected) ", selected" else ""}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = screen.title,
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = contentColor
                    )

                    // Gold underline indicator for the active tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DarkPrimary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.5.dp))
                    }
                }
            }
        }
    }
}

