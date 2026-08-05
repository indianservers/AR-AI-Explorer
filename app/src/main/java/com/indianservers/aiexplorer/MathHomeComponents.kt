package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavInk = Color(0xFFF4F7FF)
private val NavMuted = Color(0xFFAAB3CE)

@Composable
internal fun MathQuickLaunchButton(
    label: String,
    icon: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(.11f))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .width(30.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Text(label, color = NavInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun MathHomeNavItem(
    icon: String,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) accent.copy(.17f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(icon, color = if (selected) accent else NavMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = if (selected) NavInk else NavMuted,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
