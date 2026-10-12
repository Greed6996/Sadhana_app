package com.sadhana.card.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Checkbox row for Train Journey sleep allowance toggle (+60 min exemption).
 */
@Composable
fun TrainJourneySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val desc = "Train Journey sleep allowance: ${if (checked) "Enabled (+60 min free)" else "Disabled"}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
            .semantics { contentDescription = desc }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null, // Handled by row click
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Train Journey (Traveling)",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "+60 min daytime sleep allowance (exempt from score deductions)",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
