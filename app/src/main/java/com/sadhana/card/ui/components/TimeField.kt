package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.domain.util.DateUtils
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.RustPrimary
import java.time.LocalTime

/**
 * Reusable time input triggering Material 3 TimePicker dialog with preset quick-chips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimeField(
    time: LocalTime?,
    onTimeSelected: (LocalTime?) -> Unit,
    title: String,
    presetTimes: List<LocalTime> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val formattedTime = if (time != null) DateUtils.formatTime(time) else "Set Time"
    val accessibilityLabel = "$title: $formattedTime"

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = accessibilityLabel },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, RustPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CreamBackground,
            contentColor = RustPrimary
        )
    ) {
        Text(
            text = formattedTime,
            fontFamily = NewsreaderFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
    }

    if (showDialog) {
        val initialHour = time?.hour ?: 21
        val initialMinute = time?.minute ?: 30
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = title,
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = RustPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)

                    if (presetTimes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Quick Presets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RustPrimary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetTimes.forEach { preset ->
                                AssistChip(
                                    onClick = {
                                        onTimeSelected(preset)
                                        showDialog = false
                                    },
                                    label = { Text(DateUtils.formatTime(preset)) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CreamBackground,
                                        labelColor = RustPrimary
                                    ),
                                    border = BorderStroke(1.dp, BorderCream)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = RustPrimary)
                }
            },
            dismissButton = {
                Row {
                    if (time != null) {
                        TextButton(
                            onClick = {
                                onTimeSelected(null)
                                showDialog = false
                            }
                        ) {
                            Text("Clear", color = RustPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
