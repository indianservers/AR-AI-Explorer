package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.ArtCurve
import com.indianservers.aiexplorer.core.ArtTriangle
import com.indianservers.aiexplorer.core.MandelbrotArt
import com.indianservers.aiexplorer.core.MathematicalArtEngine
import com.indianservers.aiexplorer.core.PolarCurveType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.formatCoordinate
import com.indianservers.aiexplorer.workspace.MathModule
import kotlin.math.max

private val ArtCyan = Color(0xFF31D7FF)
private val ArtViolet = Color(0xFFA878FF)
private val ArtGreen = Color(0xFF55E6A5)
private val ArtAmber = Color(0xFFFFC857)
private val ArtPink = Color(0xFFFF6FAE)

private enum class ArtMode(val label: String) { Polar("Polar Studio"), Parametric("Parametric Studio"), Fractal("Fractal Studio") }
private enum class ParametricArtType(val label: String) { Lissajous("Lissajous"), Harmonograph("Harmonograph"), Custom("Custom") }
private enum class FractalArtType(val label: String) { Koch("Koch"), Sierpinski("Sierpiński"), Mandelbrot("Mandelbrot") }
private enum class ArtPalette(val label: String, val colors: List<Color>) {
    Aurora("Aurora", listOf(ArtCyan, ArtViolet, ArtGreen)),
    Sunset("Sunset", listOf(ArtAmber, ArtPink, Color(0xFFFF735C))),
    Ocean("Ocean", listOf(Color(0xFF32B8FF), Color(0xFF445BFF), Color(0xFF32F0D2))),
    Mono("Mono", listOf(Color.White, Color.LightGray, Color(0xFF8290A8))),
}

@Composable
internal fun MathematicalArtWorkspace(vm: ExplorerViewModel) {
    var mode by remember { mutableStateOf(ArtMode.Polar) }
    var palette by remember { mutableStateOf(ArtPalette.Aurora) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Mathematical Art", color = ArtPink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Create visual patterns from polar symmetry, coupled oscillations and recursive geometry.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtMode.entries.forEach { choice -> Button(onClick = { mode = choice }, enabled = choice != mode) { Text(choice.label) } }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtPalette.entries.forEach { choice -> Button(onClick = { palette = choice }, enabled = choice != palette) { Text(choice.label) } }
        }
        when (mode) {
            ArtMode.Polar -> PolarArtPanel(vm, palette)
            ArtMode.Parametric -> ParametricArtPanel(vm, palette)
            ArtMode.Fractal -> FractalArtPanel(vm, palette)
        }
    }
}

@Composable
private fun PolarArtPanel(vm: ExplorerViewModel, palette: ArtPalette) {
    var type by remember { mutableStateOf(PolarCurveType.Rose) }
    var parameter by rememberLabText(vm, MathModule.MathematicalArt, "polar.parameter", "5"); var scale by rememberLabText(vm, MathModule.MathematicalArt, "polar.scale", "1")
    var copies by rememberLabText(vm, MathModule.MathematicalArt, "polar.copies", "1"); var stroke by rememberLabText(vm, MathModule.MathematicalArt, "polar.stroke", "3")
    Text("POLAR SYMMETRY", color = ArtPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PolarCurveType.entries.forEach { choice -> Button(onClick = { type = choice; parameter = if (choice == PolarCurveType.Rose) "5" else "1" }, enabled = choice != type) { Text(choice.name) } }
    }
    ArtFields(listOf(
        ArtField("Parameter", parameter) { parameter = it }, ArtField("Scale", scale) { scale = it },
        ArtField("Radial copies", copies) { copies = it }, ArtField("Stroke", stroke) { stroke = it },
    ))
    val result = remember(type, parameter, scale) { runCatching { MathematicalArtEngine.polar(type, parameter.toDouble(), scale.toDouble()) } }
    val art = result.getOrNull()
    if (art == null) return ArtError(result)
    val copyCount = copies.toIntOrNull()?.coerceIn(1, 48) ?: 1
    val curves = remember(art, copyCount) { MathematicalArtEngine.radialCopies(art.points, copyCount) }
    ArtCurveCanvas(curves, palette.colors, stroke.toFloatOrNull()?.coerceIn(1f, 12f) ?: 3f)
    ArtCurveMeasurements(art)
    ArtInsight("Symmetry", if (type == PolarCurveType.Rose) "Frequency ${parameter} creates repeated petals; parity changes the petal count." else art.description)
    art.graphSource?.let { source -> Button(onClick = { vm.addFunction(source); vm.open(MathModule.Graph2D) }) { Text("Open polar equation in Graph") } }
}

@Composable
private fun ParametricArtPanel(vm: ExplorerViewModel, palette: ArtPalette) {
    var type by remember { mutableStateOf(ParametricArtType.Lissajous) }
    var first by rememberLabText(vm, MathModule.MathematicalArt, "parametric.first", "3"); var second by rememberLabText(vm, MathModule.MathematicalArt, "parametric.second", "2")
    var customX by rememberLabText(vm, MathModule.MathematicalArt, "parametric.customX", "cos(3*t)")
    var customY by rememberLabText(vm, MathModule.MathematicalArt, "parametric.customY", "sin(2*t)")
    var phase by rememberLabText(vm, MathModule.MathematicalArt, "parametric.phase", "1.5708"); var damping by rememberLabText(vm, MathModule.MathematicalArt, "parametric.damping", "0.035")
    var copies by rememberLabText(vm, MathModule.MathematicalArt, "parametric.copies", "1"); var stroke by rememberLabText(vm, MathModule.MathematicalArt, "parametric.stroke", "2.5")
    Text("COUPLED OSCILLATIONS", color = ArtPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParametricArtType.entries.forEach { choice -> Button(onClick = { type = choice }, enabled = choice != type) { Text(choice.label) } }
    }
    val fields = if (type == ParametricArtType.Custom) listOf(
        ArtField("x(t)", customX) { customX = it }, ArtField("y(t)", customY) { customY = it },
        ArtField("Radial copies", copies) { copies = it }, ArtField("Stroke", stroke) { stroke = it },
    ) else listOf(
        ArtField(if (type == ParametricArtType.Lissajous) "x frequency" else "Frequency 1", first) { first = it },
        ArtField(if (type == ParametricArtType.Lissajous) "y frequency" else "Frequency 2", second) { second = it },
        ArtField("Phase (rad)", phase) { phase = it }, ArtField("Damping", damping) { damping = it },
        ArtField("Radial copies", copies) { copies = it }, ArtField("Stroke", stroke) { stroke = it },
    )
    ArtFields(fields)
    val result = remember(type, first, second, phase, damping, customX, customY) { runCatching {
        when (type) {
            ParametricArtType.Lissajous -> MathematicalArtEngine.lissajous(first.toInt(), second.toInt(), phase.toDouble())
            ParametricArtType.Harmonograph -> MathematicalArtEngine.harmonograph(first.toDouble(), second.toDouble(), phase.toDouble(), damping.toDouble())
            ParametricArtType.Custom -> MathematicalArtEngine.customParametric(customX, customY)
        }
    } }
    val art = result.getOrNull()
    if (art == null) return ArtError(result)
    val copyCount = copies.toIntOrNull()?.coerceIn(1, 48) ?: 1
    val curves = remember(art, copyCount) { MathematicalArtEngine.radialCopies(art.points, copyCount) }
    ArtCurveCanvas(curves, palette.colors, stroke.toFloatOrNull()?.coerceIn(1f, 12f) ?: 2.5f)
    ArtCurveMeasurements(art)
    ArtInsight("Mathematics", art.description)
    art.graphSource?.let { source -> Button(onClick = { vm.addFunction(source); vm.open(MathModule.Graph2D) }) { Text("Open parametric equation in Graph") } }
}

@Composable
private fun FractalArtPanel(vm: ExplorerViewModel, palette: ArtPalette) {
    var type by remember { mutableStateOf(FractalArtType.Koch) }
    var depth by rememberLabText(vm, MathModule.MathematicalArt, "fractal.depth", "4"); var iterations by rememberLabText(vm, MathModule.MathematicalArt, "fractal.iterations", "64")
    Text("RECURSIVE & COMPLEX ART", color = ArtPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FractalArtType.entries.forEach { choice -> Button(onClick = { type = choice }, enabled = choice != type) { Text(choice.label) } }
    }
    when (type) {
        FractalArtType.Koch -> {
            ArtFields(listOf(ArtField("Recursion depth (0–6)", depth) { depth = it }))
            val result = remember(depth) { runCatching { MathematicalArtEngine.koch(depth.toInt()) } }
            result.getOrNull()?.let { art ->
                ArtCurveCanvas(listOf(art.points), palette.colors, 3f)
                ArtCurveMeasurements(art)
                ArtInsight("Self-similarity", "${art.points.size - 1} segments · dimension log(4)/log(3) ≈ 1.26186")
            } ?: ArtError(result)
        }
        FractalArtType.Sierpinski -> {
            ArtFields(listOf(ArtField("Recursion depth (0–7)", depth) { depth = it }))
            val result = remember(depth) { runCatching { MathematicalArtEngine.sierpinski(depth.toInt()) } }
            result.getOrNull()?.let { triangles ->
                SierpinskiCanvas(triangles, palette.colors)
                ArtInsight("Self-similarity", "${triangles.size} visible triangles · dimension log(3)/log(2) ≈ 1.58496")
            } ?: ArtError(result)
        }
        FractalArtType.Mandelbrot -> {
            ArtFields(listOf(ArtField("Maximum iterations (8–300)", iterations) { iterations = it }))
            val result = remember(iterations) { runCatching { MathematicalArtEngine.mandelbrot(maximumIterations = iterations.toInt()) } }
            result.getOrNull()?.let { art ->
                MandelbrotCanvas(art, palette.colors)
                ArtInsight("Complex iteration", "Each pixel iterates zₙ₊₁ = zₙ² + c; dark points did not escape within ${art.maximumIterations} iterations.")
            } ?: ArtError(result)
        }
    }
}

private data class ArtField(val label: String, val value: String, val update: (String) -> Unit)

@Composable
private fun ArtFields(fields: List<ArtField>) {
    fields.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { field -> OutlinedTextField(field.value, field.update, label = { Text(field.label) }, singleLine = true, modifier = Modifier.weight(1f)) }
            if (row.size == 1) Column(Modifier.weight(1f)) {}
        }
    }
}

@Composable
private fun ArtCurveCanvas(curves: List<List<Vec2>>, colors: List<Color>, strokeWidth: Float) {
    Canvas(Modifier.fillMaxWidth().height(390.dp).background(Color(0xFF07101E), RoundedCornerShape(18.dp)).border(1.dp, colors.first().copy(.55f), RoundedCornerShape(18.dp)).padding(12.dp)) {
        val all = curves.flatten(); if (all.size < 2) return@Canvas
        val minX = all.minOf { it.x }; val maxX = all.maxOf { it.x }; val minY = all.minOf { it.y }; val maxY = all.maxOf { it.y }
        val span = max(maxX - minX, maxY - minY).coerceAtLeast(1e-9) * 1.08
        val centerX = (minX + maxX) / 2; val centerY = (minY + maxY) / 2
        fun map(point: Vec2) = Offset(
            (size.width / 2 + (point.x - centerX) / span * size.minDimension).toFloat(),
            (size.height / 2 - (point.y - centerY) / span * size.minDimension).toFloat(),
        )
        curves.forEachIndexed { curveIndex, curve ->
            if (curve.size < 2) return@forEachIndexed
            val path = Path(); curve.forEachIndexed { index, point -> val p = map(point); if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
            drawPath(path, colors[curveIndex % colors.size], style = Stroke(strokeWidth))
        }
    }
}

@Composable
private fun SierpinskiCanvas(triangles: List<ArtTriangle>, colors: List<Color>) {
    Canvas(Modifier.fillMaxWidth().height(390.dp).background(Color(0xFF07101E), RoundedCornerShape(18.dp)).border(1.dp, colors.first().copy(.55f), RoundedCornerShape(18.dp)).padding(12.dp)) {
        fun map(point: Vec2) = Offset((size.width * (.5 + point.x * .45)).toFloat(), (size.height * (.54 - point.y * .45)).toFloat())
        triangles.forEachIndexed { index, triangle ->
            val path = Path().apply { val a = map(triangle.a); val b = map(triangle.b); val c = map(triangle.c); moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }
            drawPath(path, colors[index % colors.size].copy(.78f))
        }
    }
}

@Composable
private fun MandelbrotCanvas(art: MandelbrotArt, colors: List<Color>) {
    Canvas(Modifier.fillMaxWidth().height(390.dp).background(Color.Black, RoundedCornerShape(18.dp)).border(1.dp, colors.first().copy(.55f), RoundedCornerShape(18.dp))) {
        val width = size.width / art.columns; val height = size.height / art.rows
        art.cells.forEach { cell ->
            val color = if (cell.iterations == art.maximumIterations) Color(0xFF02040A) else blendArtColors(colors, cell.normalized.toFloat())
            drawRect(color, Offset(cell.column * width, (art.rows - 1 - cell.row) * height), Size(width + .5f, height + .5f))
        }
    }
}

private fun blendArtColors(colors: List<Color>, amount: Float): Color {
    val scaled = amount.coerceIn(0f, 1f) * colors.size
    val first = scaled.toInt().coerceAtMost(colors.lastIndex)
    val second = (first + 1).coerceAtMost(colors.lastIndex)
    return androidx.compose.ui.graphics.lerp(colors[first], colors[second], scaled - first)
}

@Composable
private fun ArtInsight(label: String, value: String) {
    Column(Modifier.fillMaxWidth().border(1.dp, ArtViolet.copy(.45f), RoundedCornerShape(14.dp)).background(ArtViolet.copy(.08f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label.uppercase(), color = ArtViolet, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun ArtCurveMeasurements(art: ArtCurve) {
    val metrics = remember(art) { MathematicalArtEngine.measure(art) }
    ArtInsight("Curve measurements", "Length ${formatCoordinate(metrics.pathLength)} · ${if (metrics.closed) "closed curve" else "open curve; endpoint gap ${formatCoordinate(metrics.closureError)}"}")
    Text(
        "Bounds x ${formatCoordinate(metrics.boundsMin.x)}…${formatCoordinate(metrics.boundsMax.x)} · y ${formatCoordinate(metrics.boundsMin.y)}…${formatCoordinate(metrics.boundsMax.y)}",
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
    )
}

@Composable
private fun ArtError(result: Result<*>) {
    Text(result.exceptionOrNull()?.message ?: "Enter valid art parameters.", color = MaterialTheme.colorScheme.error)
}
