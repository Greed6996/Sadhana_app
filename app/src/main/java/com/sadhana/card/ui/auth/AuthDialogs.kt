package com.sadhana.card.ui.auth

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sadhana.card.ui.theme.DarkBorder
import com.sadhana.card.ui.theme.DarkGoldBorder
import com.sadhana.card.ui.theme.DarkPrimary
import com.sadhana.card.ui.theme.DarkSurface
import com.sadhana.card.ui.theme.DarkSurfaceVariant
import com.sadhana.card.ui.theme.DarkTextMuted
import com.sadhana.card.ui.theme.DarkTextPrimary
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Pop-up 3: "Welcome to Sadhana Card" with Devotee and Counselor cards.
 */
@Composable
fun AuthChoiceDialog(
    onSelectRole: (role: String) -> Unit,
    onContinueOffline: () -> Unit,
    isLoading: Boolean = false
) {
    Dialog(
        onDismissRequest = onContinueOffline,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, DarkGoldBorder), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to Sadhana Card",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DarkTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect with Google to securely synchronize your devotional practices or oversee assigned devotees.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = DarkTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Card 1: Login as Devotee
                RoleChoiceCard(
                    title = "Login as Devotee",
                    subtitle = "Log daily Sadhana, track your rounds, and share weekly progress with counselors.",
                    icon = Icons.Default.Person,
                    accentColor = DarkPrimary,
                    enabled = !isLoading,
                    onClick = { onSelectRole("devotee") }
                )

                // Card 2: Login as Counselor
                RoleChoiceCard(
                    title = "Login as Counselor",
                    subtitle = "Review weekly devotee completion scores and guide practitioners' spiritual sadhana.",
                    icon = Icons.Default.SupervisorAccount,
                    accentColor = Color(0xFF60A5FA),
                    enabled = !isLoading,
                    onClick = { onSelectRole("counselor") }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Offline fallback option
                TextButton(
                    onClick = onContinueOffline,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Continue Offline (No Account)",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = DarkTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    color = DarkTextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Dialog shown when an unapproved user selects "Login as Counselor".
 */
@Composable
fun CounselorPendingDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, DarkGoldBorder), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(DarkPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = DarkPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Counselor Access Pending",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = DarkTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Counselor access is pending approval. Please contact the administrator. Once approved, you will have access to the counselor dashboard.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.5.sp,
                    color = DarkTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimary,
                        contentColor = Color(0xFF0D0D0F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "OK, Continue as Devotee",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Historical Data Consent Dialog for linking pre-existing offline records.
 */
@Composable
fun HistoricalSyncConsentDialog(
    localRecordCount: Int,
    onConfirmSyncAll: () -> Unit,
    onSkipHistorical: () -> Unit
) {
    Dialog(onDismissRequest = onSkipHistorical) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, DarkGoldBorder), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Sync Existing Records?",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You have $localRecordCount saved offline records on this device. Would you like to link them to your Google account so your counselor can view your historical progress?",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = DarkTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onConfirmSyncAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimary,
                        contentColor = Color(0xFF0D0D0F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Link & Sync All History",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onSkipHistorical,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Start Fresh From This Week",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = DarkTextMuted
                    )
                }
            }
        }
    }
}
