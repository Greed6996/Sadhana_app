package com.sadhana.card.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.sadhana.card.ui.theme.DmSansFontFamily
import com.sadhana.card.ui.theme.NewsreaderFontFamily
import com.sadhana.card.ui.theme.PillShape

/**
 * Dedicated read-only viewer for Privacy Policy and Terms & Conditions.
 * Accessible from Settings without requiring re-acceptance.
 */
@Composable
fun TermsPrivacyViewerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Privacy Policy, 1 = Terms & Conditions

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = "Go back" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Terms & Privacy",
                    fontFamily = NewsreaderFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = {
                        Text(
                            text = "Privacy Policy",
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == 0,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = {
                        Text(
                            text = "Terms & Conditions",
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == 1,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Scrollable text content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (selectedTab == 0) LegalContent.PRIVACY_POLICY_TEXT else LegalContent.TERMS_CONDITIONS_TEXT,
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
