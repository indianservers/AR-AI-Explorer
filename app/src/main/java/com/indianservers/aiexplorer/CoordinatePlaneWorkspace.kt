package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.CoordinatePlaneEngine
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.formatCoordinate
import com.indianservers.aiexplorer.workspace.Shape2DType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val PlaneBackground get() = themedColor(Color(0xFF040811), Background)
private val PlanePanel get() = themedColor(Color(0xFF0A1220), SurfaceA)
private val PlaneBorder get() = themedColor(Color(0xFF26344D), AppBorder)
private val PlaneCyan get() = themedColor(Color(0xFF38D9FF), Cyan)
private val PlaneViolet get() = themedColor(Color(0xFFA878FF), Violet)
private val PlaneGreen get() = themedColor(Color(0xFF55E6A5), Green)
private val PlaneAmber get() = themedColor(Color(0xFFFFC857), Amber)

private enum class PlaneTool(val label: String) { Select("Select"), Plot("Plot point"), Line("Draw line"), Measure("Measure"), Triangle("Triangle") }

@Composable
internal fun CoordinatePlaneWorkspace(vm: ExplorerViewModel) {
    var tool by remember { mutableStateOf(PlaneTool.Plot) }
    var selected by remember { mutableStateOf<List<Int>>(emptyList()) }
    var xDraft by remember { mutableStateOf("0") }
    var yDraft by remember { mutableStateOf("0") }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var snap by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var dragPreview by remember { mutableStateOf<Pair<Int, Vec2>?>(null) }

    val points = vm.state.points.mapIndexed { index, point ->
        if (dragPreview?.first == index) requireNotNull(dragPreview).second else point
    }
    val pair = selected.takeLast(2).takeIf { it.size == 2 }
    val analysis = pair?.let { indices ->
        val first = points.getOrNull(indices[0])
        val second = points.getOrNull(indices[1])
        if (first != null && second != null) CoordinatePlaneEngine.analyse(first, second) else null
    }
    val triangleIndices = selected.takeLast(3).takeIf { tool == PlaneTool.Triangle && it.size == 3 }
    val triangle = triangleIndices?.mapNotNull(points::getOrNull)?.takeIf { it.size == 3 }
        ?.let { CoordinatePlaneEngine.analyseTriangle(it[0], it[1], it[2]) }

    fun choosePoint(index: Int) {
        selected = when (tool) {
            PlaneTool.Select, PlaneTool.Plot -> listOf(index)
            PlaneTool.Line, PlaneTool.Measure -> (selected.filterNot { it == index } + index).takeLast(2)
            PlaneTool.Triangle -> (selected.filterNot { it == index } + index).takeLast(3)
        }
    }

    Column(
        Modifier.fillMaxSize().background(PlaneBackground).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Coordinate Plane", color = PlaneCyan, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Plot, drag, connect and measure exact Cartesian points", color = Color(0xFFABB8CC), fontSize = 12.sp)
            }
            Text("${points.size} points · ${vm.state.shapes.count { it.type in coordinateLineTypes }} lines", color = PlaneGreen, fontSize = 11.sp)
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaneTool.entries.forEach { candidate ->
                Button(
                    onClick = { tool = candidate; selected = emptyList() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tool == candidate) PlaneViolet else PlanePanel),
                ) { Text(candidate.label) }
            }
            OutlinedButton(onClick = { showGrid = !showGrid }) { Text(if (showGrid) "Grid on" else "Grid off") }
            OutlinedButton(onClick = { snap = !snap }) { Text(if (snap) "Snap ½" else "Free") }
        }

        CoordinateCanvas(
            points = points,
            shapes = vm.state.shapes.filter { it.type in coordinateLineTypes && it.visible },
            selected = selected,
            zoom = zoom,
            pan = pan,
            showGrid = showGrid,
            snap = snap,
            tool = tool,
            onPointChosen = ::choosePoint,
            onPointAdded = { vm.addPoint(it); selected = listOf(vm.state.points.lastIndex) },
            onDragPreview = { index, point -> dragPreview = index to point },
            onDragEnd = {
                dragPreview?.let { (index, point) -> vm.movePoint(index, point) }
                dragPreview = null
            },
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Zoom", color = Color.White, fontSize = 11.sp)
            Slider(zoom, { zoom = it }, valueRange = .55f..2.4f, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { pan = Vec2(0.0, 0.0); zoom = 1f }) { Text("Fit") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("←" to Vec2(-1.0, 0.0), "↑" to Vec2(0.0, 1.0), "↓" to Vec2(0.0, -1.0), "→" to Vec2(1.0, 0.0)).forEach { (label, delta) ->
                OutlinedButton(onClick = { pan += delta }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(xDraft, { xDraft = it }, label = { Text("x") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(yDraft, { yDraft = it }, label = { Text("y") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val point = Vec2(xDraft.toDoubleOrNull() ?: return@Button, yDraft.toDoubleOrNull() ?: return@Button)
                    vm.addPoint(if (snap) CoordinatePlaneEngine.snap(point) else point)
                    selected = listOf(vm.state.points.lastIndex)
                },
            ) { Text("Add") }
        }

        if (pair != null && analysis != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaneMetric("Δ", "(${formatCoordinate(analysis.delta.x)}, ${formatCoordinate(analysis.delta.y)})", PlaneCyan, Modifier.weight(1f))
                PlaneMetric("Distance", formatCoordinate(analysis.distance), PlaneGreen, Modifier.weight(1f))
                PlaneMetric("Midpoint", "(${formatCoordinate(analysis.midpoint.x)}, ${formatCoordinate(analysis.midpoint.y)})", PlaneAmber, Modifier.weight(1f))
                PlaneMetric("Slope", analysis.slope?.let(::formatCoordinate) ?: "undefined", PlaneViolet, Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().background(PlanePanel, RoundedCornerShape(14.dp)).border(1.dp, PlaneBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Text("Line through P${pair[0] + 1} and P${pair[1] + 1}", color = PlaneViolet, fontWeight = FontWeight.Bold)
                Text(analysis.equation(), color = Color.White, fontSize = 20.sp)
                Text("Parallel through midpoint: ${analysis.parallelEquation(analysis.midpoint)}", color = Color(0xFFB8C3D5), fontSize = 12.sp)
                Text("Perpendicular through midpoint: ${analysis.perpendicularEquation(analysis.midpoint)}", color = Color(0xFFB8C3D5), fontSize = 12.sp)
            }
            if (tool == PlaneTool.Line) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Line", "Segment", "Ray").forEach { kind ->
                        Button(onClick = { vm.applyContextualGeometryTool(kind, pair); tool = PlaneTool.Select; selected = emptyList() }) { Text("Create $kind") }
                    }
                }
            }
        } else {
            Text(if (tool == PlaneTool.Triangle) "Select three points to analyse a triangle." else "Select two different points with Draw line or Measure to see distance, midpoint, slope and equation.", color = Color(0xFFABB8CC), fontSize = 12.sp)
        }

        if (triangle != null) {
            Text("TRIANGLE ANALYTICS", color = PlaneAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaneMetric("Area", formatCoordinate(triangle.area), PlaneAmber, Modifier.weight(1f))
                PlaneMetric("Perimeter", formatCoordinate(triangle.perimeter), PlaneGreen, Modifier.weight(1f))
                PlaneMetric("Centroid", "(${formatCoordinate(triangle.centroid.x)}, ${formatCoordinate(triangle.centroid.y)})", PlaneCyan, Modifier.weight(1f))
            }
            Text(
                "P${triangleIndices[0] + 1} → P${triangleIndices[1] + 1} → P${triangleIndices[2] + 1}: ${triangle.sideClassification} ${triangle.angleClassification} triangle · ${triangle.orientation.name.lowercase()} orientation",
                color = if (triangle.isCollinear) PlaneAmber else Color(0xFFB8C3D5), fontSize = 12.sp,
            )
            Text("Sides ${triangle.sideLengths.joinToString { formatCoordinate(it) }}", color = Color(0xFFB8C3D5), fontSize = 12.sp)
            triangle.circumcenter?.let { centre ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Circumcenter (${formatCoordinate(centre.x)}, ${formatCoordinate(centre.y)})", color = PlaneViolet, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { vm.addPoint(centre); vm.reportStatus("Circumcenter added to Coordinate Plane") }) { Text("Plot center") }
                }
            }
        }

        selected.singleOrNull()?.let { index ->
            points.getOrNull(index)?.let { point ->
                Row(
                    Modifier.fillMaxWidth().background(PlanePanel, RoundedCornerShape(12.dp)).border(1.dp, PlaneBorder, RoundedCornerShape(12.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("P${index + 1} = (${formatCoordinate(point.x)}, ${formatCoordinate(point.y)})", color = Color.White, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { vm.deleteCoordinatePoint(index); selected = emptyList() }) { Text("Delete") }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::undo, enabled = vm.canUndo, modifier = Modifier.weight(1f)) { Text("Undo") }
            OutlinedButton(onClick = vm::redo, enabled = vm.canRedo, modifier = Modifier.weight(1f)) { Text("Redo") }
            OutlinedButton(onClick = vm::saveWorkspace, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = { vm.clearCurrentWorkspace(); selected = emptyList() }, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
    }
}

@Composable
private fun CoordinateCanvas(
    points: List<Vec2>,
    shapes: List<com.indianservers.aiexplorer.workspace.Shape2D>,
    selected: List<Int>,
    zoom: Float,
    pan: Vec2,
    showGrid: Boolean,
    snap: Boolean,
    tool: PlaneTool,
    onPointChosen: (Int) -> Unit,
    onPointAdded: (Vec2) -> Unit,
    onDragPreview: (Int, Vec2) -> Unit,
    onDragEnd: () -> Unit,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    Canvas(
        Modifier.fillMaxWidth().height(430.dp)
            .background(Color(0xFF07101C), RoundedCornerShape(18.dp))
            .border(1.dp, PlaneCyan.copy(.35f), RoundedCornerShape(18.dp))
            .semantics { contentDescription = "Interactive Cartesian coordinate plane" }
            .pointerInput(points, zoom, pan, tool, snap) {
                fun scale() = 38f * zoom
                fun screen(point: Vec2) = Offset(size.width / 2f + ((point.x - pan.x) * scale()).toFloat(), size.height / 2f - ((point.y - pan.y) * scale()).toFloat())
                fun world(offset: Offset): Vec2 {
                    val raw = Vec2((offset.x - size.width / 2f) / scale() + pan.x, (size.height / 2f - offset.y) / scale() + pan.y)
                    return if (snap) CoordinatePlaneEngine.snap(raw) else raw
                }
                detectTapGestures { tap ->
                    val hit = points.indices.minByOrNull { (screen(points[it]) - tap).getDistance() }
                        ?.takeIf { (screen(points[it]) - tap).getDistance() <= 28f }
                    if (hit != null) onPointChosen(hit)
                    else if (tool == PlaneTool.Plot) onPointAdded(world(tap))
                }
            }
            .pointerInput(points, zoom, pan, tool, snap) {
                fun scale() = 38f * zoom
                fun screen(point: Vec2) = Offset(size.width / 2f + ((point.x - pan.x) * scale()).toFloat(), size.height / 2f - ((point.y - pan.y) * scale()).toFloat())
                fun world(offset: Offset): Vec2 {
                    val raw = Vec2((offset.x - size.width / 2f) / scale() + pan.x, (size.height / 2f - offset.y) / scale() + pan.y)
                    return if (snap) CoordinatePlaneEngine.snap(raw) else raw
                }
                var current = Offset.Zero
                detectDragGestures(
                    onDragStart = { start ->
                        current = start
                        draggingIndex = points.indices.minByOrNull { (screen(points[it]) - start).getDistance() }
                            ?.takeIf { (screen(points[it]) - start).getDistance() <= 32f }
                    },
                    onDrag = { change, amount ->
                        change.consume(); current += amount
                        draggingIndex?.let { onDragPreview(it, world(current)) }
                    },
                    onDragEnd = { if (draggingIndex != null) onDragEnd(); draggingIndex = null },
                    onDragCancel = { onDragEnd(); draggingIndex = null },
                )
            },
    ) {
        val scale = 38f * zoom
        val centre = Offset(size.width / 2f - (pan.x * scale).toFloat(), size.height / 2f + (pan.y * scale).toFloat())
        fun screen(point: Vec2) = Offset(centre.x + (point.x * scale).toFloat(), centre.y - (point.y * scale).toFloat())
        val minX = floor(pan.x - size.width / scale / 2.0).toInt() - 1
        val maxX = ceil(pan.x + size.width / scale / 2.0).toInt() + 1
        val minY = floor(pan.y - size.height / scale / 2.0).toInt() - 1
        val maxY = ceil(pan.y + size.height / scale / 2.0).toInt() + 1
        if (showGrid) {
            (minX..maxX).forEach { x -> drawLine(Color.White.copy(if (x == 0) .48f else .10f), screen(Vec2(x.toDouble(), minY.toDouble())), screen(Vec2(x.toDouble(), maxY.toDouble())), if (x == 0) 2.5f else 1f) }
            (minY..maxY).forEach { y -> drawLine(Color.White.copy(if (y == 0) .48f else .10f), screen(Vec2(minX.toDouble(), y.toDouble())), screen(Vec2(maxX.toDouble(), y.toDouble())), if (y == 0) 2.5f else 1f) }
        }
        shapes.forEach { shape ->
            val a = shape.pointIndices.getOrNull(0)?.let(points::getOrNull) ?: return@forEach
            val b = shape.pointIndices.getOrNull(1)?.let(points::getOrNull) ?: return@forEach
            val start = screen(a); val end = screen(b); val delta = end - start
            when (shape.type) {
                Shape2DType.Line, Shape2DType.Parallel, Shape2DType.Perpendicular -> drawLine(PlaneViolet.copy(.75f), start - delta * 100f, end + delta * 100f, 3f)
                Shape2DType.Ray -> drawLine(PlaneViolet.copy(.75f), start, end + delta * 100f, 3f)
                else -> drawLine(PlaneViolet.copy(.75f), start, end, 3f)
            }
        }
        selected.takeLast(3).takeIf { tool == PlaneTool.Triangle && it.size == 3 }?.let { triangle ->
            val vertices = triangle.mapNotNull(points::getOrNull)
            if (vertices.size == 3) {
                drawLine(PlaneAmber, screen(vertices[0]), screen(vertices[1]), 4f, StrokeCap.Round)
                drawLine(PlaneAmber, screen(vertices[1]), screen(vertices[2]), 4f, StrokeCap.Round)
                drawLine(PlaneAmber, screen(vertices[2]), screen(vertices[0]), 4f, StrokeCap.Round)
            }
        }
        selected.takeLast(2).takeIf { tool != PlaneTool.Triangle && it.size == 2 }?.let { pair ->
            val a = points.getOrNull(pair[0]); val b = points.getOrNull(pair[1])
            if (a != null && b != null) drawLine(if (tool == PlaneTool.Measure) PlaneAmber else PlaneCyan, screen(a), screen(b), 4f, StrokeCap.Round)
        }
        points.forEachIndexed { index, point ->
            val at = screen(point)
            if (index in selected) drawCircle(Color.White.copy(.32f), 16f, at)
            drawCircle(listOf(PlaneCyan, PlaneViolet, PlaneGreen, PlaneAmber)[index % 4], 9f, at)
            drawCircle(Color.White.copy(.8f), 9f, at, style = Stroke(1.5f))
            drawContext.canvas.nativeCanvas.drawText("P${index + 1} (${formatCoordinate(point.x)}, ${formatCoordinate(point.y)})", at.x + 12f, at.y - 12f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 23f; isAntiAlias = true })
        }
    }
}

@Composable
private fun PlaneMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(accent.copy(.08f), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.45f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

private val coordinateLineTypes = setOf(Shape2DType.Line, Shape2DType.Segment, Shape2DType.Ray, Shape2DType.Parallel, Shape2DType.Perpendicular)
