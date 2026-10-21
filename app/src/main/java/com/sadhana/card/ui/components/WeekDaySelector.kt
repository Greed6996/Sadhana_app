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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DarkBg
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily
import java.time.LocalDate

/**
 * 7-Day horizontal selector strip matching the V5 Dark Rings aesthetic:
 * 7 vertical rounded pills (Sun to Sat).
 * Active day: Solid Warm Saffron Gold (#F5B041) with dark bold text.
 * Inactive days: Elevated Dark Charcoal (#17171B) with subtle 1.dp border (#26262C).
 */
@Composable
fun WeekDaySelector(
    startOfWeek: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    activeColor: Color = DarkPrimary,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val dayName = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val dayNum = date.dayOfMonth.toString()

            val accessibilityText = "${date.dayOfWeek.name}, ${date.month.name} ${date.dayOfMonth}${if (isSelected) ", Selected" else ""}"

            val bg = if (isSelected) activeColor else DarkSurface
            val textColor = if (isSelected) DarkBg else DarkTextPrimary
            val nameColor = if (isSelected) DarkBg.copy(alpha = 0.85f) else DarkTextMuted
            val borderModifier = if (isSelected) {
                Modifier
            } else {
                Modifier.border(BorderStroke(1.dp, DarkBorder), pillShape)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 62.dp)
                    .clip(pillShape)
                    .background(bg)
                    .then(borderModifier)
                    .clickable(role = Role.Tab) { onDateSelected(date) }
                    .padding(vertical = 10.dp)
                    .semantics { contentDescription = accessibilityText },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayName,
                    fontFamily = DmSansFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 11.5.sp,
                    color = nameColor
                )
                Text(
                    text = dayNum,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    color = textColor
                )
            }
        }
    }
}
