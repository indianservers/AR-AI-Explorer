package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.adaptive.adaptiveFocusRing

private val NavInk get() = themedColor(Color(0xFFF4F7FF), ActiveAppPalette.ink)
private val NavMuted get() = themedColor(Color(0xFFAAB3CE), Color(0xFFCCD4E5))

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
            .height(74.dp)
            .shadow(5.dp, RoundedCornerShape(14.dp), ambientColor = accent.copy(.28f), spotColor = accent.copy(.34f))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        themedColor(accent.copy(.22f), ActiveAppPalette.surface),
                        themedColor(accent.copy(.09f), ActiveAppPalette.surface),
                        themedColor(Color(0xE6081120), ActiveAppPalette.surface),
                    ),
                ),
            )
            .border(1.dp, themedColor(accent.copy(.55f), ActiveAppPalette.border), RoundedCornerShape(14.dp))
            .adaptiveFocusRing(shape = RoundedCornerShape(14.dp), focusColor = accent)
            .clickable(onClick = onClick)
            .focusable()
            .semantics { contentDescription = "Open $label" }
            .padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .width(38.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.radialGradient(listOf(accent.copy(.30f), accent.copy(.10f))))
                .border(1.dp, accent.copy(.72f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            MathHomeFeatureIcon(label = label, fallback = icon, accent = accent, modifier = Modifier.fillMaxWidth().height(38.dp))
        }
        Text(label, color = NavInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun MathHomeFeatureIcon(label: String, fallback: String, accent: Color, modifier: Modifier = Modifier) {
    if (label == "Formulas") {
        Text("f(x)", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
        return
    }
    if (label !in setOf("2D", "3D", "Graphs", "3D Graph", "AR", "Spatial AR", "AR 3D Graph", "Proofs", "Visual Proofs", "Theorems")) {
        Text(fallback, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
        return
    }
    Canvas(modifier.padding(6.dp)) {
        val stroke = (size.minDimension * .075f).coerceAtLeast(1.5f)
        val glow = accent.copy(.28f)
        fun glowingLine(start: Offset, end: Offset, width: Float = stroke) {
            drawLine(glow, start, end, width * 2.6f)
            drawLine(accent, start, end, width)
        }
        when (label) {
            "2D" -> {
                val origin = Offset(size.width * .20f, size.height * .80f)
                glowingLine(origin, Offset(size.width * .88f, origin.y))
                glowingLine(origin, Offset(origin.x, size.height * .10f))
                drawLine(accent.copy(.82f), Offset(size.width * .28f, size.height * .28f), Offset(size.width * .73f, size.height * .28f), stroke * .7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
                drawLine(accent.copy(.82f), Offset(size.width * .73f, size.height * .28f), Offset(size.width * .73f, size.height * .68f), stroke * .7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
                drawCircle(Color(0xFF43D7FF), stroke * 1.45f, Offset(size.width * .73f, size.height * .28f))
            }
            "3D" -> {
                val top = Offset(size.width * .50f, size.height * .10f)
                val left = Offset(size.width * .18f, size.height * .30f)
                val right = Offset(size.width * .82f, size.height * .30f)
                val center = Offset(size.width * .50f, size.height * .50f)
                val bottomLeft = Offset(size.width * .18f, size.height * .66f)
                val bottomRight = Offset(size.width * .82f, size.height * .66f)
                val bottom = Offset(size.width * .50f, size.height * .88f)
                listOf(top to left, top to right, left to center, right to center, center to bottom, left to bottomLeft, right to bottomRight, bottomLeft to bottom, bottomRight to bottom).forEach { glowingLine(it.first, it.second, stroke * .85f) }
                drawLine(accent.copy(.7f), top, center, stroke * .65f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
            }
            "Graphs" -> {
                val origin = Offset(size.width * .16f, size.height * .82f)
                glowingLine(origin, Offset(size.width * .88f, origin.y))
                glowingLine(origin, Offset(origin.x, size.height * .12f))
                val curve = Path().apply {
                    moveTo(size.width * .20f, size.height * .72f)
                    cubicTo(size.width * .35f, size.height * .66f, size.width * .38f, size.height * .46f, size.width * .52f, size.height * .48f)
                    cubicTo(size.width * .70f, size.height * .50f, size.width * .65f, size.height * .18f, size.width * .84f, size.height * .16f)
                }
                drawPath(curve, glow, style = Stroke(stroke * 3f))
                drawPath(curve, accent, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawCircle(accent, stroke * 1.15f, Offset(size.width * .52f, size.height * .48f))
                drawCircle(accent, stroke * 1.15f, Offset(size.width * .84f, size.height * .16f))
            }
            "3D Graph" -> {
                val origin = Offset(size.width * .48f, size.height * .66f)
                glowingLine(origin, Offset(size.width * .48f, size.height * .10f))
                glowingLine(origin, Offset(size.width * .88f, size.height * .68f))
                glowingLine(origin, Offset(size.width * .16f, size.height * .88f))
                drawCircle(accent, stroke * 1.3f, origin)
                drawCircle(accent.copy(.7f), stroke, Offset(size.width * .48f, size.height * .10f))
                drawCircle(accent.copy(.7f), stroke, Offset(size.width * .88f, size.height * .68f))
                drawCircle(accent.copy(.7f), stroke, Offset(size.width * .16f, size.height * .88f))
            }
            "AR", "Spatial AR", "AR 3D Graph" -> {
                val frame = Path().apply {
                    moveTo(size.width * .18f, size.height * .22f)
                    lineTo(size.width * .82f, size.height * .22f)
                    lineTo(size.width * .82f, size.height * .78f)
                    lineTo(size.width * .18f, size.height * .78f)
                    close()
                }
                drawPath(frame, glow, style = Stroke(stroke * 2.4f))
                drawPath(frame, accent, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val origin = Offset(size.width * .48f, size.height * .62f)
                glowingLine(origin, Offset(size.width * .48f, size.height * .34f), stroke * .78f)
                glowingLine(origin, Offset(size.width * .72f, size.height * .64f), stroke * .78f)
                glowingLine(origin, Offset(size.width * .30f, size.height * .76f), stroke * .78f)
                drawCircle(Color(0xFF43D7FF), stroke * 1.25f, origin)
                drawCircle(accent.copy(.75f), stroke * .95f, Offset(size.width * .48f, size.height * .34f))
                drawCircle(accent.copy(.75f), stroke * .95f, Offset(size.width * .72f, size.height * .64f))
                drawCircle(accent.copy(.75f), stroke * .95f, Offset(size.width * .30f, size.height * .76f))
                drawLine(accent.copy(.68f), Offset(size.width * .25f, size.height * .30f), Offset(size.width * .36f, size.height * .30f), stroke * .75f)
                drawLine(accent.copy(.68f), Offset(size.width * .25f, size.height * .30f), Offset(size.width * .25f, size.height * .41f), stroke * .75f)
                drawLine(accent.copy(.68f), Offset(size.width * .75f, size.height * .70f), Offset(size.width * .64f, size.height * .70f), stroke * .75f)
                drawLine(accent.copy(.68f), Offset(size.width * .75f, size.height * .70f), Offset(size.width * .75f, size.height * .59f), stroke * .75f)
            }
            "Proofs", "Visual Proofs" -> {
                drawCircle(accent, size.minDimension * .22f, Offset(size.width * .30f, size.height * .43f), style = Stroke(stroke))
                val triangle = Path().apply { moveTo(size.width * .52f, size.height * .75f); lineTo(size.width * .70f, size.height * .25f); lineTo(size.width * .88f, size.height * .75f); close() }
                drawPath(triangle, accent, style = Stroke(stroke))
                drawRect(accent.copy(.9f), Offset(size.width * .62f, size.height * .55f), Size(size.width * .25f, size.height * .25f), style = Stroke(stroke * .7f))
                drawLine(accent.copy(.8f), Offset(size.width * .30f, size.height * .20f), Offset(size.width * .30f, size.height * .68f), stroke * .55f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
            }
            "Theorems" -> {
                val book = Path().apply {
                    moveTo(size.width * .10f, size.height * .25f); quadraticTo(size.width * .30f, size.height * .17f, size.width * .49f, size.height * .33f)
                    lineTo(size.width * .49f, size.height * .80f); quadraticTo(size.width * .30f, size.height * .65f, size.width * .10f, size.height * .73f); close()
                    moveTo(size.width * .51f, size.height * .33f); quadraticTo(size.width * .70f, size.height * .17f, size.width * .90f, size.height * .25f)
                    lineTo(size.width * .90f, size.height * .73f); quadraticTo(size.width * .70f, size.height * .65f, size.width * .51f, size.height * .80f); close()
                }
                drawPath(book, glow, style = Stroke(stroke * 3f))
                drawPath(book, accent, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                glowingLine(Offset(size.width * .50f, size.height * .32f), Offset(size.width * .50f, size.height * .80f), stroke * .6f)
            }
            else -> Unit
        }
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
            .adaptiveFocusRing(shape = RoundedCornerShape(15.dp), focusColor = accent)
            .clickable(onClick = onClick)
            .focusable()
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
