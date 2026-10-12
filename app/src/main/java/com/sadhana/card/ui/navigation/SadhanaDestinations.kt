package com.sadhana.card.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Daily : Screen("daily", "Daily", Icons.Default.EditCalendar)
    object Weekly : Screen("weekly", "Weekly", Icons.Default.ViewWeek)
}
