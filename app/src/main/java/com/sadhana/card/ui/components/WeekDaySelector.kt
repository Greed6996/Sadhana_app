package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.sadhana.card.ui.theme.DayButtonShape
import com.sadhana.card.ui.theme.DmSansFontFamily
import java.time.LocalDate

/**
 * 7-day segmented horizontal selector for Monday–Sunday of the active week.
 */
@Composable
fun WeekDaySelector(
    startOfWeek: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val dayName = date.dayOfWeek.name.take(3)
            val dayNum = date.dayOfMonth.toString()

            val accessibilityText = "${date.dayOfWeek.name}, ${date.month.name} ${date.dayOfMonth}${if (isSelected) ", Selected" else ""}"

            val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val nameColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            val borderStroke = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 64.dp)
                    .clip(DayButtonShape)
                    .background(bg)
                    .border(borderStroke, DayButtonShape)
                    .clickable(role = Role.Tab) { onDateSelected(date) }
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = accessibilityText },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayName,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = nameColor
                )
                Text(
                    text = dayNum,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor
                )
            }
        }
    }
}
