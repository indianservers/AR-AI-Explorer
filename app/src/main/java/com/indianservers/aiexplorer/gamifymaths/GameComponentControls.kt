package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class GameComponentAction(
    val label: String,
    val symbol: String,
    val enabled: Boolean = true,
    val description: String = label,
    val onClick: () -> Unit,
)

@Composable
internal fun GameComponentControls(
    status: String,
    accent: Color,
    actions: List<GameComponentAction>,
    guidance: String = "Tap a choice to add or replace. Use the controls below to remove or reset.",
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(GamePanel.copy(alpha = .88f), RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = .52f), RoundedCornerShape(16.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("COMPONENT CONTROLS", color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Text(status, color = GameInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            actions.forEach { action ->
                Row(
                    Modifier
                        .alpha(if (action.enabled) 1f else .35f)
                        .background(accent.copy(alpha = .12f), RoundedCornerShape(11.dp))
                        .border(1.dp, accent.copy(alpha = if (action.enabled) .62f else .24f), RoundedCornerShape(11.dp))
                        .clickable(enabled = action.enabled, onClick = action.onClick)
                        .focusable(action.enabled)
                        .semantics { contentDescription = action.description }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(action.symbol, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(action.label, color = GameInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(guidance, color = GameMuted, fontSize = 9.sp, lineHeight = 11.sp)
    }
}
