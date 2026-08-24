package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.trim
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private enum class RealWorldScene(val label: String) {
    FerrisWheel("Ferris Wheel"),
    Pendulum("Pendulum"),
    SoundWave("Sound Wave"),
    SatelliteOrbit("Satellite Orbit"),
}

private data class RealWorldReadout(
    val equationTitle: String,
    val equation: String,
    val primaryLabel: String,
    val primaryValue: String,
    val secondaryLabel: String,
    val secondaryValue: String,
)

@Composable
internal fun TrigRealWorldApplication(
    selectedApplication: String,
    time: Float,
    playing: Boolean,
    speed: Float,
    amplitude: Float,
    offset: Float,
    onApplicationChange: (String) -> Unit,
    onTimeChange: (Float) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
) {
    val scene = RealWorldScene.entries.firstOrNull { it.label == selectedApplication } ?: RealWorldScene.FerrisWheel
    val readout = realWorldReadout(scene, time, speed, amplitude, offset)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RealWorldScene.entries.forEach { item ->
                RealWorldSceneTab(item, item == scene) { onApplicationChange(item.label) }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 620.dp
            if (wide) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RealWorldSceneCanvas(scene, time, speed, amplitude, offset, onTimeChange, Modifier.weight(1.7f).height(250.dp))
                    RealWorldReadoutPanel(readout, Modifier.weight(1f).height(250.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RealWorldSceneCanvas(scene, time, speed, amplitude, offset, onTimeChange, Modifier.fillMaxWidth().height(235.dp))
                    RealWorldReadoutPanel(readout, Modifier.fillMaxWidth())
                }
            }
        }

        RealWorldPlaybackControls(
            time = time,
            playing = playing,
            onTimeChange = onTimeChange,
            onPlayingChange = onPlayingChange,
        )
    }
}

@Composable
private fun RealWorldSceneTab(scene: RealWorldScene, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .height(42.dp)
            .background(if (selected) Cyan.copy(alpha = .18f) else SurfaceA.copy(alpha = .72f), shape)
            .border(1.dp, if (selected) Cyan.copy(alpha = .72f) else Muted.copy(alpha = .16f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        RealWorldSceneIcon(scene, if (selected) Cyan else Muted)
        Text(scene.label, color = if (selected) Cyan else Ink, fontSize = 11.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

@Composable
private fun RealWorldSceneIcon(scene: RealWorldScene, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (scene) {
            RealWorldScene.FerrisWheel -> {
                drawCircle(color, radius = size.minDimension * .37f, style = stroke)
                drawLine(color, Offset(size.width / 2, size.height * .5f), Offset(size.width * .24f, size.height * .92f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width / 2, size.height * .5f), Offset(size.width * .76f, size.height * .92f), strokeWidth = stroke.width)
            }
            RealWorldScene.Pendulum -> {
                drawLine(color, Offset(size.width * .2f, size.height * .12f), Offset(size.width * .8f, size.height * .12f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width / 2, size.height * .12f), Offset(size.width * .68f, size.height * .72f), strokeWidth = stroke.width)
                drawCircle(color, radius = size.minDimension * .14f, center = Offset(size.width * .68f, size.height * .78f))
            }
            RealWorldScene.SoundWave -> {
                val path = Path().apply {
                    moveTo(0f, size.height / 2)
                    for (i in 1..24) {
                        val x = size.width * i / 24f
                        lineTo(x, size.height / 2 + sin(i / 24f * PI * 4).toFloat() * size.height * .28f)
                    }
                }
                drawPath(path, color, style = stroke)
            }
            RealWorldScene.SatelliteOrbit -> {
                drawOval(color, topLeft = Offset(size.width * .08f, size.height * .24f), size = Size(size.width * .84f, size.height * .52f), style = stroke)
                drawCircle(color, radius = size.minDimension * .15f)
                drawCircle(Amber, radius = size.minDimension * .09f, center = Offset(size.width * .84f, size.height * .35f))
            }
        }
    }
}

@Composable
private fun RealWorldSceneCanvas(
    scene: RealWorldScene,
    time: Float,
    speed: Float,
    amplitude: Float,
    offset: Float,
    onTimeChange: (Float) -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Canvas(
        modifier
            .background(Color(0xFF07101A), shape)
            .border(1.dp, Cyan.copy(alpha = .30f), shape)
            .pointerInput(Unit) {
                detectTapGestures { point -> onTimeChange((point.x / size.width * 12.57f).coerceIn(0f, 12.57f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onTimeChange((change.position.x / size.width * 12.57f).coerceIn(0f, 12.57f))
                }
            },
    ) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF07111E), Color(0xFF101329), Color(0xFF091018))))
        drawSceneGrid()
        when (scene) {
            RealWorldScene.FerrisWheel -> drawFerrisWheel(time, speed, amplitude, offset)
            RealWorldScene.Pendulum -> drawPendulum(time, speed)
            RealWorldScene.SoundWave -> drawSoundWave(time, speed)
            RealWorldScene.SatelliteOrbit -> drawSatelliteOrbit(time, speed)
        }
    }
}

private fun DrawScope.drawSceneGrid() {
    val gridColor = Cyan.copy(alpha = .055f)
    for (x in 0..12) drawLine(gridColor, Offset(size.width * x / 12f, 0f), Offset(size.width * x / 12f, size.height), 1f)
    for (y in 0..8) drawLine(gridColor, Offset(0f, size.height * y / 8f), Offset(size.width, size.height * y / 8f), 1f)
}

private fun DrawScope.drawFerrisWheel(time: Float, speed: Float, amplitude: Float, offset: Float) {
    val ground = size.height * .82f
    val center = Offset(size.width * .25f, size.height * .48f)
    val radius = size.minDimension * .28f
    drawRect(Color(0xFF091018), Offset(0f, ground), Size(size.width, size.height - ground))
    val buildings = listOf(.02f to .18f, .10f to .28f, .20f to .14f, .33f to .23f, .48f to .31f, .61f to .17f, .72f to .26f, .86f to .20f)
    buildings.forEach { (x, h) -> drawRect(Color(0xFF111827), Offset(size.width * x, ground - size.height * h), Size(size.width * .11f, size.height * h)) }
    drawCircle(Color(0xFFFFB45C).copy(alpha = .22f), radius + 4f, center)
    drawCircle(Ink.copy(alpha = .82f), radius, center, style = Stroke(2.2f))
    repeat(12) { index ->
        val theta = index * 2 * PI / 12
        val edge = Offset(center.x + cos(theta).toFloat() * radius, center.y + sin(theta).toFloat() * radius)
        drawLine(Ink.copy(alpha = .42f), center, edge, 1.2f)
        drawCircle(if (index % 2 == 0) Amber else Color(0xFFFF7A59), 4.2f, edge)
    }
    drawLine(Ink.copy(alpha = .65f), center, Offset(center.x - radius * .58f, ground), 3f)
    drawLine(Ink.copy(alpha = .65f), center, Offset(center.x + radius * .58f, ground), 3f)
    val phase = speed * time
    val rider = Offset(center.x + cos(phase).toFloat() * radius, center.y - sin(phase).toFloat() * radius)
    drawCircle(Cyan.copy(alpha = .28f), 11f, rider)
    drawCircle(Cyan, 5.5f, rider)

    val left = size.width * .43f
    val right = size.width * .97f
    val mid = size.height * .46f
    val graphAmplitude = size.height * .23f
    val wave = Path()
    repeat(81) { index ->
        val u = index / 80f
        val x = left + (right - left) * u
        val y = mid - sin(u * PI * 2).toFloat() * graphAmplitude
        if (index == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
    }
    drawPath(wave, Cyan, style = Stroke(2.4f, cap = StrokeCap.Round))
    val u = ((time % (2 * PI).toFloat()) / (2 * PI).toFloat()).coerceIn(0f, 1f)
    val marker = Offset(left + (right - left) * u, mid - sin(u * PI * 2).toFloat() * graphAmplitude)
    drawLine(Cyan.copy(alpha = .45f), Offset(marker.x, ground), marker, 1.2f)
    drawCircle(Amber, 5f, marker)
    drawLabel("Height ${trim((offset + amplitude * sin(phase)).toDouble())} m", left + 8f, ground - 12f, Green)
}

private fun DrawScope.drawPendulum(time: Float, speed: Float) {
    val pivot = Offset(size.width / 2f, size.height * .18f)
    val length = size.height * .55f
    val theta = Math.toRadians(30.0) * cos(speed * time)
    val bob = Offset(pivot.x + sin(theta).toFloat() * length, pivot.y + cos(theta).toFloat() * length)
    drawLine(Ink.copy(alpha = .7f), Offset(size.width * .3f, pivot.y), Offset(size.width * .7f, pivot.y), 6f, StrokeCap.Round)
    drawCircle(Amber, 5f, pivot)
    drawArc(Cyan.copy(alpha = .35f), 60f, 60f, false, Offset(pivot.x - length, pivot.y - length * .2f), Size(length * 2, length * 1.4f), style = Stroke(1.5f))
    drawLine(Ink, pivot, bob, 3f)
    drawCircle(Cyan.copy(alpha = .2f), 24f, bob)
    drawCircle(Cyan, 16f, bob)
    drawLabel("theta ${trim(Math.toDegrees(theta))} deg", bob.x + 18f, bob.y, Amber)
    drawLabel("Rest position", pivot.x + 8f, pivot.y + length, Muted)
    drawLine(Muted.copy(alpha = .4f), pivot, Offset(pivot.x, pivot.y + length), 1f)
}

private fun DrawScope.drawSoundWave(time: Float, speed: Float) {
    val mid = size.height / 2f
    val speakerX = size.width * .12f
    drawRect(Cyan.copy(alpha = .18f), Offset(speakerX - 24f, mid - 42f), Size(32f, 84f))
    drawCircle(Cyan, 18f, Offset(speakerX - 8f, mid))
    val phase = speed * time
    listOf(.85f to Cyan, .55f to Amber, .30f to Green).forEachIndexed { index, (scale, color) ->
        val path = Path()
        repeat(121) { sample ->
            val u = sample / 120f
            val x = speakerX + size.width * .82f * u
            val y = mid + sin(u * PI * (4 + index * 2) - phase).toFloat() * size.height * .28f * scale
            if (sample == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = if (index == 0) 1f else .45f), style = Stroke(if (index == 0) 3f else 1.5f, cap = StrokeCap.Round))
    }
    val sampleX = speakerX + size.width * .82f * .58f
    val sampleY = mid + sin(.58f * PI * 4 - phase).toFloat() * size.height * .28f * .85f
    drawLine(Ink.copy(alpha = .3f), Offset(sampleX, size.height * .15f), Offset(sampleX, size.height * .85f), 1f)
    drawCircle(Amber, 6f, Offset(sampleX, sampleY))
}

private fun DrawScope.drawSatelliteOrbit(time: Float, speed: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val orbit = Rect(center.x - size.width * .38f, center.y - size.height * .32f, center.x + size.width * .38f, center.y + size.height * .32f)
    drawOval(Cyan.copy(alpha = .58f), orbit.topLeft, orbit.size, style = Stroke(2f))
    drawCircle(Color(0xFF176B9C), size.minDimension * .14f, center)
    drawCircle(Green.copy(alpha = .72f), size.minDimension * .14f, center, style = Stroke(5f))
    val theta = speed * time * .55f
    val satellite = Offset(center.x + cos(theta) * orbit.width / 2f, center.y + sin(theta) * orbit.height / 2f)
    drawLine(Amber.copy(alpha = .45f), center, satellite, 1.5f)
    drawRect(Ink, satellite - Offset(7f, 5f), Size(14f, 10f))
    drawRect(Cyan, satellite - Offset(23f, 4f), Size(13f, 8f))
    drawRect(Cyan, satellite + Offset(10f, -4f), Size(13f, 8f))
    drawCircle(Amber.copy(alpha = .24f), 16f, satellite)
}

private fun DrawScope.drawLabel(text: String, x: Float, y: Float, color: Color) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 11.dp.toPx()
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    })
}

@Composable
private fun RealWorldReadoutPanel(readout: RealWorldReadout, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().background(SurfaceA.copy(alpha = .9f), RoundedCornerShape(9.dp)).border(1.dp, Muted.copy(alpha = .22f), RoundedCornerShape(9.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(readout.equationTitle, color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text(readout.equation, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadoutValue(readout.primaryLabel, readout.primaryValue, Cyan, Modifier.weight(1f))
            ReadoutValue(readout.secondaryLabel, readout.secondaryValue, Amber, Modifier.weight(1f))
        }
        Text("Drag the visual or scrub the timeline", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun ReadoutValue(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(SurfaceB.copy(alpha = .82f), RoundedCornerShape(8.dp)).border(1.dp, accent.copy(alpha = .25f), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun RealWorldPlaybackControls(
    time: Float,
    playing: Boolean,
    onTimeChange: (Float) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PlaybackButton("|<", "Previous", false) { onTimeChange((time - .5f).coerceAtLeast(0f)) }
        PlaybackButton(if (playing) "||" else ">", if (playing) "Pause" else "Play", true) { onPlayingChange(!playing) }
        PlaybackButton(">|", "Next", false) { onTimeChange((time + .5f).coerceAtMost(12.57f)) }
        Spacer(Modifier.width(2.dp))
        Slider(value = time, onValueChange = onTimeChange, valueRange = 0f..12.57f, modifier = Modifier.weight(1f))
        Text("${trim(time.toDouble())} s", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(46.dp))
    }
}

@Composable
private fun PlaybackButton(symbol: String, description: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(if (primary) 42.dp else 36.dp)
            .semantics { contentDescription = description }
            .background(if (primary) Cyan.copy(alpha = .28f) else SurfaceA.copy(alpha = .9f), RoundedCornerShape(10.dp))
            .border(1.dp, if (primary) Cyan.copy(alpha = .75f) else Muted.copy(alpha = .2f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (primary) Cyan else Ink, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier)
    }
}

private fun realWorldReadout(scene: RealWorldScene, time: Float, speed: Float, amplitude: Float, offset: Float): RealWorldReadout {
    return when (scene) {
        RealWorldScene.FerrisWheel -> {
            val height = offset + amplitude * sin(speed * time)
            RealWorldReadout("Height Equation", "h(t) = ${trim(amplitude.toDouble())} sin(${trim(speed.toDouble())}t) + ${trim(offset.toDouble())}", "Height", "${trim(height.toDouble())} m", "Time", "${trim(time.toDouble())} s")
        }
        RealWorldScene.Pendulum -> {
            val theta = 30.0 * cos(speed * time)
            val period = 2 * PI / speed.coerceAtLeast(.01f)
            RealWorldReadout("Angular Displacement", "theta(t) = 30 cos(${trim(speed.toDouble())}t)", "Angle", "${trim(theta)} deg", "Period", "${trim(period)} s")
        }
        RealWorldScene.SoundWave -> {
            val y = sin(speed * time * 2 * PI)
            RealWorldReadout("Sound Wave", "y(t) = sin(2pi(${trim(speed.toDouble())})t)", "Displacement", trim(y), "Frequency", "${trim(speed.toDouble())} Hz")
        }
        RealWorldScene.SatelliteOrbit -> {
            val orbitalAngle = ((speed * time * .55f * 180f / PI).toFloat() % 360f + 360f) % 360f
            val period = 2 * PI / (speed.coerceAtLeast(.01f) * .55f)
            RealWorldReadout("Orbital Position", "x = a cos(omega t),  y = b sin(omega t)", "Orbit angle", "${trim(orbitalAngle.toDouble())} deg", "Period", "${trim(period)} s")
        }
    }
}
