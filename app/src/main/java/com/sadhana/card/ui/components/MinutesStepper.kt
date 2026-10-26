package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily

/**
 * Reusable stepper for minutes (e.g. Day Sleep, Reading, Hearing)
 * with [-] / [+] decrement/increment buttons and tap-to-type dialog.
 */
@Composable
fun MinutesStepper(
    minutes: Int?,
    onMinutesChanged: (Int?) -> Unit,
    step: Int = 15,
    min: Int = 0,
    max: Int = 720,
    title: String = "Minutes",
    modifier: Modifier = Modifier
) {
    val currentMinutes = minutes ?: 0
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement button
        Box(
            modifier = Modifier
                .size(44.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                .clickable(
                    enabled = currentMinutes > min,
                    role = Role.Button
                ) {
                    val next = (currentMinutes - step).coerceAtLeast(min)
                    onMinutesChanged(if (next == 0 && minutes == null) null else next)
                }
                .semantics {
                    contentDescription = "Decrease $title by $step minutes"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = if (currentMinutes > min) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Minutes display / tap to type
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showEditDialog = true }
                .semantics {
                    contentDescription = "$title: ${minutes ?: 0} minutes"
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${minutes ?: 0}",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "min",
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Increment button
        Box(
            modifier = Modifier
                .size(44.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                .clickable(
                    enabled = currentMinutes < max,
                    role = Role.Button
                ) {
                    val next = (currentMinutes + step).coerceAtMost(max)
                    onMinutesChanged(next)
                }
                .semantics {
                    contentDescription = "Increase $title by $step minutes"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = if (currentMinutes < max) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showEditDialog) {
        var inputVal by remember { mutableStateOf((minutes ?: 0).toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Enter $title (Minutes)",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                            inputVal = newVal
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = inputVal.toIntOrNull()?.coerceIn(min, max) ?: 0
                            onMinutesChanged(parsed)
                            showEditDialog = false
                        }
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputVal.toIntOrNull()?.coerceIn(min, max) ?: 0
                        onMinutesChanged(parsed)
                        showEditDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
