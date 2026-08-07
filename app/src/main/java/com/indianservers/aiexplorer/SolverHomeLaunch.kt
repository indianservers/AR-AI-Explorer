package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.indianservers.aiexplorer.adaptive.adaptiveFocusRing

@Composable
internal fun SolverHomeLaunch(
    wide: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(0xFF4DE6B1)
    val secondary = Color(0xFF32CFFF)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (wide) 112.dp else 92.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = accent.copy(alpha = .28f),
                spotColor = secondary.copy(alpha = .34f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF12392F),
                        Color(0xFF102B43),
                        Color(0xFF11162D),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = .78f), RoundedCornerShape(22.dp))
            .adaptiveFocusRing(shape = RoundedCornerShape(22.dp), focusColor = accent)
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .semantics {
                role = Role.Button
                contentDescription = "Open Offline Solver. Enter mathematics, learn every verified step, and practise similar problems."
            }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(if (wide) 70.dp else 60.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Brush.radialGradient(listOf(accent.copy(.38f), Color(0xFF0E2331))))
                .border(1.dp, secondary.copy(.72f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolverQuestIcon(accent, secondary, Modifier.fillMaxSize().padding(8.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("SOLVER QUEST", color = Color.White, fontSize = if (wide) 21.sp else 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("Solve it. Understand it. Master the next one.", color = Ink.copy(alpha = .88f), fontSize = 10.sp, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SolverBadge("OFFLINE", accent)
                SolverBadge("VERIFIED STEPS", secondary)
                if (wide) {
                    SolverBadge("PRACTICE", Amber)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LEVEL UP", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text("SOLVE  >", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SolverBadge(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 7.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = .1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = .35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun SolverQuestIcon(accent: Color, secondary: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        val shield = Path().apply {
            moveTo(size.width * .5f, size.height * .06f)
            lineTo(size.width * .86f, size.height * .2f)
            lineTo(size.width * .82f, size.height * .62f)
            quadraticBezierTo(size.width * .72f, size.height * .82f, size.width * .5f, size.height * .94f)
            quadraticBezierTo(size.width * .28f, size.height * .82f, size.width * .18f, size.height * .62f)
            lineTo(size.width * .14f, size.height * .2f)
            close()
        }
        drawPath(shield, accent.copy(alpha = .16f))
        drawPath(shield, accent, style = Stroke(stroke))
        drawLine(secondary, Offset(size.width * .3f, size.height * .48f), Offset(size.width * .43f, size.height * .62f), strokeWidth = stroke)
        drawLine(secondary, Offset(size.width * .43f, size.height * .62f), Offset(size.width * .72f, size.height * .32f), strokeWidth = stroke)
        drawCircle(Color.White, radius = 1.7.dp.toPx(), center = Offset(size.width * .75f, size.height * .18f))
        drawRect(accent.copy(alpha = .72f), topLeft = Offset(size.width * .08f, size.height * .68f), size = Size(size.width * .12f, size.height * .04f))
    }
}
