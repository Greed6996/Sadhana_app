package com.sadhana.card.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sadhana.card.R
import com.sadhana.card.ui.theme.DmSansFontFamily

/**
 * Parses raw Markdown release body into a clean list of changelog items.
 *
 * - Extracts lines starting with `-`, `*`, or `•`
 * - Strips bullet symbols and markdown formatting (bold, italic, links, code)
 * - Returns a fallback list of points if the body is empty or unformatted
 */
fun parseGithubChangelog(body: String?): List<String> {
    if (body.isNullOrBlank()) {
        return defaultFallbackChangelog()
    }

    val lines = body.lines()
    val bulletPattern = Regex("^[\\s]*[-*•]\\s+(.*)")
    val bulletPoints = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        val match = bulletPattern.matchEntire(trimmed)
        if (match != null) {
            val content = cleanMarkdownFormatting(match.groupValues[1])
            if (content.isNotBlank()) {
                bulletPoints.add(content)
            }
        }
    }

    if (bulletPoints.isNotEmpty()) {
        return bulletPoints
    }

    // Fallback: check if text contains comma/semicolon separated items
    val cleanedBody = cleanMarkdownFormatting(body).trim()
    val commaSplit = cleanedBody.split(Regex("[,;\\n]+"))
        .map { it.trim().removePrefix("and ").trim() }
        .filter { it.isNotBlank() }

    if (commaSplit.size >= 2) {
        return commaSplit
    }

    if (cleanedBody.isNotBlank()) {
        return listOf(cleanedBody) + defaultFallbackChangelog().take(2)
    }

    return defaultFallbackChangelog()
}

private fun cleanMarkdownFormatting(text: String): String {
    return text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("__(.*?)__"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("_(.*?)_"), "$1")
        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .trim()
}

private fun defaultFallbackChangelog(): List<String> = listOf(
    "Daily Sadhana tracking enhancements & UI polish",
    "Weekly Matrix scoring and performance improvements",
    "Offline data stability and sync optimizations"
)

/**
 * Custom App Update Dialog matching the cosmic meditation illustration design:
 *
 * - 28.dp rounded card taking 0.88f of screen width
 * - 250.dp cosmic meditation illustration header with floating version badge
 * - Overlapping white bottom sheet (-20.dp offset) with 28.dp top rounded corners
 * - Centered "New Blessings & Improvements" title
 * - Left-aligned 3-point changelog with purple bullet dots
 * - Dynamic "+X more changes — View on GitHub ↗" link
 * - Horizontal gradient "UPDATE NOW" CTA pill button
 * - In-dialog smooth AnimatedContent download progress bar with percentage
 */
@Composable
fun AppUpdateDialog(
    versionName: String,
    changelogPoints: List<String>,
    githubReleaseUrl: String,
    isDownloading: Boolean = false,
    downloadPercent: Int = 0,
    isReadyToInstall: Boolean = false,
    onUpdateNow: () -> Unit,
    onInstallNow: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // 1. Top Graphic Header (250.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_update_blessings),
                        contentDescription = "New Blessings & Improvements Header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Version Pill Badge in Top-Right Corner
                    val displayVersion = if (versionName.startsWith("v", ignoreCase = true)) {
                        versionName
                    } else {
                        "v$versionName"
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA1A103C))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayVersion,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFEDE8FF)
                        )
                    }
                }

                // 2. Overlapping White Bottom Sheet (-20.dp offset)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "New Blessings & Improvements",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = Color(0xFF1F1235),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3-Point Changelog List (Strictly Capped at 3)
                        val displayPoints = changelogPoints.take(3)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayPoints.forEach { point ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6B5AED),
                                        lineHeight = 20.sp
                                    )
                                    Text(
                                        text = point,
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF4A4A5A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // GitHub Release Notes Link
                        val extraCount = (changelogPoints.size - 3).coerceAtLeast(0)
                        val githubLinkText = if (extraCount > 0) {
                            "+$extraCount more changes — View on GitHub ↗"
                        } else {
                            "See full release notes on GitHub ↗"
                        }

                        Text(
                            text = githubLinkText,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF6B5AED),
                            modifier = Modifier
                                .clickable {
                                    openBrowserUrl(context, githubReleaseUrl)
                                }
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // In-Dialog Download Progress / Action Buttons
                        val buttonGradient = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4AC2B5), Color(0xFF8E7CFB))
                        )

                        AnimatedContent(
                            targetState = isDownloading to isReadyToInstall,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "UpdateStateContentAnimation"
                        ) { (downloading, readyToInstall) ->
                            when {
                                downloading -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (downloadPercent > 0) {
                                            val progressFloat = (downloadPercent / 100f).coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { progressFloat },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(CircleShape),
                                                color = Color(0xFF6B5AED),
                                                trackColor = Color(0xFFEEEEEE)
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(CircleShape),
                                                color = Color(0xFF6B5AED),
                                                trackColor = Color(0xFFEEEEEE)
                                            )
                                        }

                                        val statusText = if (downloadPercent >= 100) {
                                            "Installing update..."
                                        } else {
                                            "Downloading update... $downloadPercent%"
                                        }

                                        Text(
                                            text = statusText,
                                            fontFamily = DmSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF4A4A5A)
                                        )
                                    }
                                }

                                readyToInstall -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .clip(CircleShape)
                                                .background(buttonGradient)
                                                .clickable(role = Role.Button, onClick = onInstallNow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "INSTALL NOW",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                letterSpacing = 0.5.sp,
                                                color = Color.White
                                            )
                                        }

                                        TextButton(
                                            onClick = onDismiss,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Maybe Later",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = Color(0xFF888899)
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .clip(CircleShape)
                                                .background(buttonGradient)
                                                .clickable(role = Role.Button, onClick = onUpdateNow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "UPDATE NOW",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                letterSpacing = 0.5.sp,
                                                color = Color.White
                                            )
                                        }

                                        TextButton(
                                            onClick = onDismiss,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Maybe Later",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = Color(0xFF888899)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openBrowserUrl(context: Context, url: String) {
    try {
        val targetUrl = if (url.isBlank()) {
            "https://github.com/Greed6996/Sadhana_app/releases"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Safe no-op if no browser is installed
    }
}
