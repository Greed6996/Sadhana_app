package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.PillShape
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextDark

val DefaultServiceTags = listOf(
    "Kitchen / Cooking",
    "Deity Worship",
    "Cleaning",
    "Book Distribution",
    "Preaching",
    "Harinam",
    "AV / Sound",
    "Temple Maintenance"
)

/**
 * Multi-select chip group for Practical Service tags (Seva),
 * with support for custom tag addition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceTagPicker(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    availableTags: List<String> = DefaultServiceTags,
    modifier: Modifier = Modifier
) {
    var showCustomTagDialog by remember { mutableStateOf(false) }

    // Combine default and any already selected custom tags
    val allTags = remember(selectedTags, availableTags) {
        (availableTags + selectedTags).distinct()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            allTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onTagsChanged(selectedTags - tag)
                        } else {
                            onTagsChanged(selectedTags + tag)
                        }
                    },
                    label = {
                        Text(
                            text = tag,
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceWhite,
                        labelColor = TextDark,
                        selectedContainerColor = RustPrimary,
                        selectedLabelColor = Color(0xFFFBF1DF)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = BorderCream,
                        selectedBorderColor = RustPrimary
                    )
                )
            }

            // Add custom tag chip
            FilterChip(
                selected = false,
                onClick = { showCustomTagDialog = true },
                label = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = RustPrimary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "Add Custom",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = RustPrimary
                        )
                    }
                },
                shape = PillShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CreamBackground,
                    labelColor = RustPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = BorderCream
                )
            )
        }
    }

    if (showCustomTagDialog) {
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomTagDialog = false },
            title = {
                Text(
                    text = "Add Custom Service Tag",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = RustPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("e.g. Flower Garland") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTagName.trim()
                        if (trimmed.isNotEmpty() && !selectedTags.contains(trimmed)) {
                            onTagsChanged(selectedTags + trimmed)
                        }
                        showCustomTagDialog = false
                    }
                ) {
                    Text("Add", fontWeight = FontWeight.Bold, color = RustPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
