package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SubjectOption(val title: String, val description: String, val symbol: String, val enabled: Boolean)

private val SubjectOptions = listOf(
    SubjectOption("Maths", "Interactive mathematics laboratory", "Sum", true),
    SubjectOption("Physics", "Upcoming science laboratory", "F", false),
    SubjectOption("Chemistry", "Upcoming science laboratory", "Ch", false),
    SubjectOption("Biology", "Upcoming science laboratory", "DNA", false),
    SubjectOption("Astro Physics", "Stars, space and cosmology", "Star", false),
    SubjectOption("IQ Labs", "Logic, patterns and reasoning", "IQ", false),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubjectHubScreen(
    modifier: Modifier = Modifier,
    wide: Boolean,
    onOpenSubject: (String) -> Unit,
    onOpenLearningIntelligence: () -> Unit,
    onOpenSmartBoard: () -> Unit,
    onOpenAugmentedReality: () -> Unit,
    onOpenMathFormulas: () -> Unit,
    onOpenMathGraph: () -> Unit,
    onOpenMathArGraph: () -> Unit,
    onCopyLaunchReport: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    fun launch(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 42.dp else 12.dp, vertical = if (wide) 28.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("AI Explorer", color = Ink, fontSize = if (wide) 42.sp else 30.sp, fontWeight = FontWeight.ExtraBold)
        Box(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Cyan.copy(.12f))
                .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text("MATHS ONLY LAUNCH", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text("Choose a learning laboratory", color = Muted, fontSize = if (wide) 20.sp else 15.sp)
        Text("Maths is available now. Physics, Chemistry, Biology and other labs are upcoming.", color = Cyan, fontSize = 12.sp, textAlign = TextAlign.Center)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickLaunchButton("Start Maths", "home", wide) { launch { onOpenSubject("Maths") } }
            QuickLaunchButton("Formulas", "f", wide) { launch(onOpenMathFormulas) }
            QuickLaunchButton("Graph", "graph", wide) { launch(onOpenMathGraph) }
            QuickLaunchButton("AR Graph", "ar", wide) { launch(onOpenMathArGraph) }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubjectOptions.forEach { subject ->
                Column(
                    Modifier
                        .width(if (wide) 250.dp else 158.dp)
                        .heightIn(min = if (wide) 178.dp else 154.dp)
                        .shadow(if (subject.enabled) 14.dp else 5.dp, RoundedCornerShape(26.dp), ambientColor = Cyan.copy(.18f), spotColor = Violet.copy(.24f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (subject.enabled) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        Cyan.copy(alpha = 0.22f),
                                        Green.copy(alpha = 0.14f),
                                    ),
                                )
                            } else {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))
                            },
                        )
                        .border(2.dp, if (subject.enabled) Cyan.copy(.78f) else Muted.copy(.28f), RoundedCornerShape(24.dp))
                        .clickable(enabled = subject.enabled) { launch { onOpenSubject(subject.title) } }
                        .focusable()
                        .semantics { contentDescription = if (subject.enabled) "Open ${subject.title} laboratory" else "${subject.title}, coming soon" }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SubjectMicroIcon(subject.title, if (subject.enabled) Cyan else Muted)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background((if (subject.enabled) Green else Amber).copy(.12f))
                                .border(1.dp, (if (subject.enabled) Green else Amber).copy(.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                        ) {
                            Text(if (subject.enabled) "LIVE" else "SOON", color = if (subject.enabled) Green else Amber.copy(.8f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(subject.title, color = if (subject.enabled) Ink else Muted, fontSize = if (wide) 22.sp else 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(subject.description, color = Muted, fontSize = 10.sp, maxLines = 2)
                        if (subject.enabled) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Enter lab", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(">", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Text("Upcoming", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Button(onClick = onOpenSmartBoard, enabled = false, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Smart Board, upcoming" }) {
            Text("Smart Board - Upcoming")
        }
        Button(onClick = onOpenAugmentedReality, enabled = false, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Augmented Reality, upcoming from home" }) {
            Text("Augmented Reality - Upcoming")
        }
        Button(onClick = onOpenLearningIntelligence, enabled = false, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Learning Intelligence, upcoming" }) {
            Text("Learning Intelligence - Upcoming")
        }
        Button(onClick = { launch(onCopyLaunchReport) }, modifier = Modifier.fillMaxWidth()) {
            Text("Copy Launch Diagnostics")
        }
        Text("v1.0 · Offline Maths core · Touch, mouse, keyboard and TV remote ready", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun QuickLaunchButton(label: String, icon: String, wide: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = Ink,
        ),
        modifier = Modifier
            .heightIn(min = if (wide) 44.dp else 38.dp)
            .semantics { contentDescription = "Open $label" },
    ) {
        TransparentIcon(icon, Cyan)
        Text("  $label", fontSize = if (wide) 12.sp else 10.sp, maxLines = 1)
    }
}

@Composable
private fun SubjectMicroIcon(subject: String, tint: Color) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.radialGradient(listOf(tint.copy(.24f), tint.copy(.05f))))
            .border(1.dp, tint.copy(.5f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp).semantics { contentDescription = "$subject icon" }) {
            val w = size.width
            val h = size.height
            val stroke = 1.8.dp.toPx()
            when (subject) {
                "Maths" -> {
                    drawLine(tint.copy(.7f), Offset(w * .16f, h * .78f), Offset(w * .88f, h * .78f), stroke)
                    drawLine(tint.copy(.7f), Offset(w * .24f, h * .88f), Offset(w * .24f, h * .12f), stroke)
                    val curve = Path().apply {
                        moveTo(w * .16f, h * .68f)
                        cubicTo(w * .36f, h * .18f, w * .58f, h * .92f, w * .88f, h * .22f)
                    }
                    drawPath(curve, tint, style = Stroke(stroke))
                }
                "Physics" -> {
                    drawCircle(tint, w * .10f, Offset(w / 2, h / 2))
                    drawOval(tint.copy(.85f), Offset(w * .08f, h * .34f), Size(w * .84f, h * .32f), style = Stroke(stroke))
                    drawOval(tint.copy(.65f), Offset(w * .34f, h * .08f), Size(w * .32f, h * .84f), style = Stroke(stroke))
                    drawCircle(tint, w * .055f, Offset(w * .88f, h / 2))
                }
                "Chemistry" -> {
                    val flask = Path().apply {
                        moveTo(w * .38f, h * .12f)
                        lineTo(w * .62f, h * .12f)
                        moveTo(w * .44f, h * .12f)
                        lineTo(w * .44f, h * .42f)
                        lineTo(w * .20f, h * .82f)
                        quadraticBezierTo(w * .18f, h * .91f, w * .30f, h * .91f)
                        lineTo(w * .70f, h * .91f)
                        quadraticBezierTo(w * .82f, h * .91f, w * .80f, h * .82f)
                        lineTo(w * .56f, h * .42f)
                        lineTo(w * .56f, h * .12f)
                    }
                    drawPath(flask, tint, style = Stroke(stroke))
                    drawLine(tint.copy(.7f), Offset(w * .27f, h * .71f), Offset(w * .73f, h * .71f), stroke)
                }
                "Biology" -> {
                    val left = Path()
                    val right = Path()
                    repeat(7) { index ->
                        val y = h * (.10f + index * .13f)
                        val x1 = if (index % 2 == 0) w * .28f else w * .52f
                        val x2 = w - x1
                        if (index == 0) {
                            left.moveTo(x1, y)
                            right.moveTo(x2, y)
                        } else {
                            left.lineTo(x1, y)
                            right.lineTo(x2, y)
                        }
                        drawLine(tint.copy(.55f), Offset(x1, y), Offset(x2, y), stroke * .72f)
                    }
                    drawPath(left, tint, style = Stroke(stroke))
                    drawPath(right, tint, style = Stroke(stroke))
                }
                "Astro Physics" -> {
                    drawCircle(tint.copy(.22f), w * .25f, Offset(w * .48f, h * .52f))
                    drawCircle(tint, w * .25f, Offset(w * .48f, h * .52f), style = Stroke(stroke))
                    drawLine(tint, Offset(w * .10f, h * .68f), Offset(w * .88f, h * .34f), stroke)
                    drawCircle(tint, w * .05f, Offset(w * .82f, h * .18f))
                }
                else -> {
                    val nodes = listOf(
                        Offset(w * .22f, h * .28f),
                        Offset(w * .72f, h * .20f),
                        Offset(w * .48f, h * .52f),
                        Offset(w * .24f, h * .78f),
                        Offset(w * .78f, h * .76f),
                    )
                    listOf(0 to 2, 1 to 2, 2 to 3, 2 to 4, 3 to 4).forEach { (a, b) -> drawLine(tint.copy(.55f), nodes[a], nodes[b], stroke) }
                    nodes.forEach { drawCircle(tint, w * .065f, it) }
                }
            }
        }
    }
}
