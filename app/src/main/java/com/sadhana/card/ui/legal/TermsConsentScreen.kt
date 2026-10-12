package com.sadhana.card.ui.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import kotlinx.coroutines.launch

/**
 * Fullscreen onboarding consent screen displayed when terms have not been accepted,
 * or when acceptedTermsVersion is lower than the current version constant.
 */
@Composable
fun TermsConsentScreen(
    onAcceptTerms: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    var agreedPrivacy by remember { mutableStateOf(false) }
    var agreedTerms by remember { mutableStateOf(false) }
    var policyDetailToShow by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val canProceed = agreedPrivacy && agreedTerms && !isSubmitting

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Sacred Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "WELCOME TO",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Sadhana Card",
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Description card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sadhana Card is an offline-first spiritual companion for your daily devotional discipline. All your chanting, hearing, reading, and sleep records stay strictly on this device.\n\nPlease review and agree to the Privacy Policy and Terms & Conditions to begin.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Privacy Policy consent row
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

            // Terms and Conditions consent row
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

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Button(
                onClick = {
                    if (canProceed) {
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                onAcceptTerms()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                },
                enabled = canProceed,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Accept Privacy Policy and Terms and Conditions" }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
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

    // Modal Document Reader
    policyDetailToShow?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { policyDetailToShow = null },
            title = {
                Text(
                    text = title,
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { policyDetailToShow = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
