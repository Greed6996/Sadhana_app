package com.sadhana.card.ui.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sadhana.card.ui.theme.BorderCream
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.RustPrimary
import com.sadhana.card.ui.theme.SaffronAccent
import com.sadhana.card.ui.theme.SurfaceWhite
import com.sadhana.card.ui.theme.TextDark
import com.sadhana.card.ui.theme.TextMuted

/**
 * First-launch onboarding dialog for Privacy Policy and Terms & Conditions acceptance.
 */
@Composable
fun LegalConsentDialog(
    onConsentAccepted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var agreedPrivacy by remember { mutableStateOf(false) }
    var agreedTerms by remember { mutableStateOf(false) }
    var policyDetailToShow by remember { mutableStateOf<Pair<String, String>?>(null) }

    val canProceed = agreedPrivacy && agreedTerms

    Dialog(
        onDismissRequest = { /* Block dismiss until accepted */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.5.dp, BorderCream), RoundedCornerShape(24.dp)),
            color = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.sadhana.card.ui.theme.TileBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = RustPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Welcome to",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SaffronAccent
                        )
                        Text(
                            text = "Sadhana Card",
                            fontFamily = NewsreaderFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = TextDark
                        )
                    }
                }

                Text(
                    text = "Sadhana Card is a completely offline spiritual companion. All your records, times, and notes stay on this device with zero tracking and zero server storage. Please accept our policies to proceed.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextMuted
                )

                // Privacy Policy checkbox row
                ConsentRow(
                    checked = agreedPrivacy,
                    onCheckedChange = { agreedPrivacy = it },
                    title = "I agree to the Privacy Policy",
                    subtitle = "100% offline, zero network access, zero data collection",
                    onViewDetails = {
                        policyDetailToShow = Pair(
                            LegalContent.PRIVACY_POLICY_TITLE,
                            LegalContent.PRIVACY_POLICY_TEXT
                        )
                    }
                )

                // Terms and Conditions checkbox row
                ConsentRow(
                    checked = agreedTerms,
                    onCheckedChange = { agreedTerms = it },
                    title = "I agree to the Terms & Conditions",
                    subtitle = "Voluntary devotional discipline and self-regulation",
                    onViewDetails = {
                        policyDetailToShow = Pair(
                            LegalContent.TERMS_CONDITIONS_TITLE,
                            LegalContent.TERMS_CONDITIONS_TEXT
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onConsentAccepted,
                    enabled = canProceed,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RustPrimary,
                        contentColor = Color(0xFFFBF1DF),
                        disabledContainerColor = BorderCream,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics { contentDescription = "Accept Privacy Policy and Terms and Conditions" }
                ) {
                    Text(
                        text = "Accept & Begin Sadhana",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // Detail policy reader dialog
    policyDetailToShow?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { policyDetailToShow = null },
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = text,
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextDark
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { policyDetailToShow = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = RustPrimary)
                }
            }
        )
    }
}

@Composable
fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CreamBackground)
            .border(BorderStroke(1.dp, BorderCream), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = RustPrimary,
                        checkmarkColor = Color(0xFFFBF1DF)
                    )
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = title,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextDark
                    )
                    Text(
                        text = subtitle,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }
            }

            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Read Full Policy",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = SaffronAccent
                )
            }
        }
    }
}
