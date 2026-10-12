package com.sadhana.card.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.InputShape

/**
 * Top header input fields for Sadhaka name and Counselor name.
 */
@Composable
fun HeaderFieldsRow(
    sadhakaName: String,
    onSadhakaNameChanged: (String) -> Unit,
    counselorName: String,
    onCounselorNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HeaderInputField(
            label = "Sadhaka",
            value = sadhakaName,
            onValueChanged = onSadhakaNameChanged,
            placeholder = "Your Name",
            modifier = Modifier.weight(1f)
        )
        HeaderInputField(
            label = "Counselor",
            value = counselorName,
            onValueChanged = onCounselorNameChanged,
            placeholder = "Counselor Name",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HeaderInputField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(InputShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), InputShape)
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = "$label input field: current value $value" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChanged,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = TextStyle(
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
