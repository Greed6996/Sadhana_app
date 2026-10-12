package com.sadhana.card.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SaffronSoft
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextMuted

@Composable
fun SadhanaBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(Screen.Daily, Screen.Weekly)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, BorderCream))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route

                val (bg, contentColor) = if (isSelected) {
                    Pair(SaffronSoft, RustPrimary)
                } else {
                    Pair(Color.Transparent, TextMuted)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable(role = Role.Tab) {
                            if (!isSelected) {
                                onNavigateToRoute(screen.route)
                            }
                        }
                        .padding(vertical = 6.dp)
                        .semantics { contentDescription = "${screen.title} tab${if (isSelected) ", selected" else ""}" },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = screen.title,
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
