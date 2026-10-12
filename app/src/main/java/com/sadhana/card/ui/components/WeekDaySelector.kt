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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.DayButtonShape
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SaffronSoft
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextDark
import com.sadhana.card.ui.theme.TextMuted
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

            val (bg, textColor, nameColor, borderStroke) = if (isSelected) {
                listOf(
                    RustPrimary,
                    com.sadhana.card.ui.theme.CreamBackground,
                    SaffronSoft,
                    BorderStroke(1.5.dp, RustPrimary)
                )
            } else {
                listOf(
                    SurfaceWhite,
                    TextDark,
                    TextMuted,
                    BorderStroke(1.dp, BorderCream)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 64.dp)
                    .clip(DayButtonShape)
                    .background(bg as androidx.compose.ui.graphics.Color)
                    .border(borderStroke as BorderStroke, DayButtonShape)
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
                    color = nameColor as androidx.compose.ui.graphics.Color
                )
                Text(
                    text = dayNum,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor as androidx.compose.ui.graphics.Color
                )
            }
        }
    }
}
