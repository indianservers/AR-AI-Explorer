package com.indianservers.aiexplorer.physics.ui

import android.graphics.Paint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.physics.simulation.GraphSample
import com.indianservers.aiexplorer.physics.simulation.LaboratorySection
import com.indianservers.aiexplorer.physics.simulation.MeasurementValue
import com.indianservers.aiexplorer.physics.simulation.SimulationPlayback
import kotlin.math.abs
import kotlin.math.roundToInt

object PhysicsColors {
    val Background = Color(0xFF050A12)
    val Panel = Color(0xE80B1724)
    val PanelRaised = Color(0xEE12243A)
    val Cyan = Color(0xFF42DCF5)
    val Violet = Color(0xFFB78CFF)
    val Green = Color(0xFF65E6A7)
    val Amber = Color(0xFFFFC766)
    val Red = Color(0xFFFF7185)
    val Ink = Color(0xFFF1F7FF)
    val Muted = Color(0xFF9DB0C5)
}

@Composable
fun LaboratoryPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(PhysicsColors.PanelRaised, PhysicsColors.Panel)))
            .border(1.dp, PhysicsColors.Cyan.copy(alpha = .28f), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        title?.let { Text(it, color = PhysicsColors.Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        content()
    }
}

@Composable
fun SimulationTransport(
    playback: SimulationPlayback,
    speed: Double,
    onTogglePlayback: () -> Unit,
    onStep: () -> Unit,
    onReset: () -> Unit,
    onSpeedChange: (Double) -> Unit,
    onPulse: (() -> Unit)? = null,
) {
    LaboratoryPanel(title = "Simulation") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PhysicsButton(if (playback == SimulationPlayback.Playing) "Pause" else "Play", onTogglePlayback)
            PhysicsButton("Step", onStep)
            PhysicsButton("Reset", onReset)
            onPulse?.let { PhysicsButton("Create pulse", it) }
        }
        ParameterSlider("Simulation speed", speed, 0.1..2.0, "×", onSpeedChange)
    }
}

@Composable
fun SectionSelector(selected: LaboratorySection, onSelected: (LaboratorySection) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LaboratorySection.entries.forEach { section ->
            ChoiceChip(section.label, selected == section) { onSelected(section) }
        }
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) PhysicsColors.Background else PhysicsColors.Ink,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PhysicsColors.Cyan else PhysicsColors.PanelRaised)
            .border(1.dp, PhysicsColors.Cyan.copy(alpha = if (selected) 1f else .35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label option${if (selected) ", selected" else ""}" }
            .padding(horizontal = 13.dp, vertical = 10.dp),
    )
}

@Composable
fun ParameterSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    unit: String,
    onValueChange: (Double) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = PhysicsColors.Muted, fontSize = 12.sp)
            Text("${format(value)} $unit", color = PhysicsColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics { contentDescription = "$label, ${format(value)} $unit" },
        )
    }
}

@Composable
fun ToggleSetting(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PhysicsColors.Muted, fontSize = 12.sp)
        Switch(checked = value, onCheckedChange = onValueChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
fun MeasurementPanel(values: List<MeasurementValue>, modifier: Modifier = Modifier) {
    LaboratoryPanel(modifier = modifier, title = "Live measurements") {
        values.chunked(2).forEach { rowValues ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { measurement ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(PhysicsColors.Background.copy(alpha = .55f)).padding(9.dp),
                    ) {
                        Text(measurement.label, color = PhysicsColors.Muted, fontSize = 10.sp)
                        Text("${measurement.value}${if (measurement.unit.isBlank()) "" else " ${measurement.unit}"}", color = PhysicsColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveGraph(
    samples: List<GraphSample>,
    labels: List<String>,
    units: String,
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var inspected by remember { mutableStateOf<GraphSample?>(null) }
    val colors = listOf(PhysicsColors.Cyan, PhysicsColors.Amber, PhysicsColors.Violet, PhysicsColors.Green)
    LaboratoryPanel(modifier = modifier, title = "Live graph · pinch to zoom, drag to pan, tap to inspect") {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PhysicsColors.Background)
                .pointerInput(samples) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                        panX = (panX + pan.x).coerceIn(-size.width * (zoom - 1f), 0f)
                    }
                }
                .pointerInput(samples, zoom, panX) {
                    detectTapGestures { tap ->
                        if (samples.isNotEmpty()) {
                            val fraction = ((tap.x - panX) / (size.width * zoom)).coerceIn(0f, 1f)
                            inspected = samples[(fraction * samples.lastIndex).roundToInt().coerceIn(0, samples.lastIndex)]
                        }
                    }
                }
                .semantics { contentDescription = "Live graph of ${labels.joinToString()} in $units" },
        ) {
            val left = 42f
            val right = size.width - 12f
            val top = 14f
            val bottom = size.height - 30f
            val plotWidth = (right - left).coerceAtLeast(1f)
            val plotHeight = (bottom - top).coerceAtLeast(1f)
            drawLine(PhysicsColors.Muted, Offset(left, top), Offset(left, bottom), 2f)
            drawLine(PhysicsColors.Muted, Offset(left, bottom), Offset(right, bottom), 2f)
            drawLine(PhysicsColors.Muted.copy(alpha = .35f), Offset(left, (top + bottom) / 2f), Offset(right, (top + bottom) / 2f), 1f)
            if (samples.size > 1) {
                val maximum = samples.flatMap { it.values }.maxOfOrNull { abs(it) }?.coerceAtLeast(.01) ?: 1.0
                labels.indices.forEach { dataIndex ->
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val rawFraction = index / samples.lastIndex.toFloat()
                        val x = left + rawFraction * plotWidth * zoom + panX
                        val value = sample.values.getOrElse(dataIndex) { 0.0 }
                        val y = (top + bottom) / 2f - (value / maximum).toFloat() * plotHeight * .43f
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, colors[dataIndex % colors.size], style = Stroke(3.5f, cap = StrokeCap.Round))
                }
            }
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(157, 176, 197); textSize = 26f }
                drawText(units, 4f, top + 12f, paint)
                drawText("time", right - 48f, size.height - 5f, paint)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            labels.forEachIndexed { index, label -> Text("● $label", color = colors[index % colors.size], fontSize = 11.sp) }
        }
        inspected?.let { sample ->
            Text("t = ${format(sample.timeSeconds)} s · ${labels.mapIndexed { index, label -> "$label ${format(sample.values.getOrElse(index) { 0.0 })}" }.joinToString(" · ")}", color = PhysicsColors.Ink, fontSize = 11.sp)
        }
        PhysicsButton("Reset axes", onClick = { zoom = 1f; panX = 0f; inspected = null })
    }
}

@Composable
fun PhysicsButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PhysicsColors.PanelRaised, contentColor = PhysicsColors.Ink),
        modifier = Modifier.heightIn(min = 44.dp).widthIn(min = 64.dp).semantics { contentDescription = label },
    ) { Text(label, fontSize = 12.sp) }
}

fun format(value: Double): String = when {
    !value.isFinite() -> "—"
    abs(value) >= 100.0 -> "%.1f".format(value)
    abs(value) >= 10.0 -> "%.2f".format(value)
    else -> "%.3f".format(value).trimEnd('0').trimEnd('.')
}
