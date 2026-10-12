package com.sadhana.card.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadhana.card.ui.theme.NewsreaderFontFamily

/**
 * Lightweight splash screen shown while reading DataStore preferences.
 * Prevents any flash of the terms screen or unauthorized routes.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Sadhana Card",
                fontFamily = NewsreaderFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
