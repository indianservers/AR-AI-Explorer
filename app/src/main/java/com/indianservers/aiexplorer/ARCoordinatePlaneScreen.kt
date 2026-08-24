package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.measurement.ArPhysicalScale
import com.indianservers.aiexplorer.arengine.measurement.ArMeasuredPoint
import com.indianservers.aiexplorer.arengine.measurement.ArMeasurementFormatter
import com.indianservers.aiexplorer.arengine.measurement.ArMeasurementMath
import com.indianservers.aiexplorer.arengine.session.ArLabSessionController
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val CoordinateBg = Color(0xFF03070D)
private val CoordinatePanel = Color(0xEE07101A)
private val CoordinateBorder = Color(0xFF1E2A3C)
private val CoordinateText = Color(0xFFF4F7FF)
private val CoordinateMuted = Color(0xFFAAB4C5)
private val CoordinatePurple = Color(0xFF8A52FF)
private val CoordinateGreen = Color(0xFF42E48B)
private val CoordinateRed = Color(0xFFFF515A)
private val CoordinateYellow = Color(0xFFFFC43D)
private val CoordinateBlue = Color(0xFF38A9FF)

private data class CoordinatePoint(
    val id: Int,
    val name: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val worldPositionMeters: ArVector3? = null,
    val anchorId: String? = null,
)

private enum class CoordinateTool(val label: String, val symbol: String) {
    Plot("Plot Point", "+"),
    Line("Draw Line", "/"),
    Measure("Measure", "<>"),
    Shapes("Shapes", "△"),
    More("More", "..."),
}

private val initialCoordinatePoints = listOf(
    CoordinatePoint(1, "A", -3f, 2f, CoordinateYellow),
    CoordinatePoint(2, "B", 2f, 3f, CoordinateBlue),
    CoordinatePoint(3, "C", -2f, -1f, CoordinatePurple),
    CoordinatePoint(4, "D", 3f, -2f, CoordinateGreen),
)

@Composable
fun ARCoordinatePlaneScreen(onBack: () -> Unit) {
    var points by remember { mutableStateOf(initialCoordinatePoints) }
    var history by remember { mutableStateOf<List<List<CoordinatePoint>>>(emptyList()) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var selectedTool by remember { mutableStateOf(CoordinateTool.Plot) }
    var showGrid by remember { mutableStateOf(true) }
    var showAxes by remember { mutableStateOf(true) }
    var showOrigin by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var arLive by remember { mutableStateOf(false) }
    var arStatus by remember { mutableStateOf("AR Preview") }
    var arSession by remember { mutableStateOf<ArLabSessionController?>(null) }
    var arOrigin by remember { mutableStateOf<ArVector3?>(null) }
    val physicalScale = remember { ArPhysicalScale(.1) }
    val selected = points.firstOrNull { it.id == selectedId }

    fun commit(next: List<CoordinatePoint>) {
        history = (history + listOf(points)).takeLast(20)
        points = next
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CoordinateBg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoordinateHeader(onBack, arLive, arStatus)
        CoordinateArStage(
            points = points,
            selectedId = selectedId,
            showGrid = showGrid,
            showAxes = showAxes,
            showOrigin = showOrigin,
            locked = locked,
            arLive = arLive,
            onPointSelected = { selectedId = it },
            onPlot = { x, y ->
                if (points.size < 20 && !locked) {
                    val id = (points.maxOfOrNull { it.id } ?: 0) + 1
                    val palette = listOf(CoordinateRed, CoordinateYellow, CoordinateGreen, CoordinateBlue, CoordinatePurple, Color.White)
                    commit(points + CoordinatePoint(id, ('A'.code + points.size).toChar().toString(), x, y, palette[points.size % palette.size]))
                    selectedId = id
                }
            },
            activeTool = selectedTool,
            onGrid = { showGrid = !showGrid },
            onAxes = { showAxes = !showAxes },
            onOrigin = { showOrigin = !showOrigin },
            onLock = { locked = !locked },
            onReset = {
                commit(initialCoordinatePoints)
                selectedId = null
            },
            canUndo = history.isNotEmpty(),
            onUndo = {
                history.lastOrNull()?.let {
                    points = it
                    history = history.dropLast(1)
                    selectedId = null
                }
            },
            onArStateChanged = { live, status ->
                arLive = live
                arStatus = status
            },
            onArSessionReady = { arSession = it },
            onArPlot = { screenPoint ->
                arSession?.addMeasurementPoint(screenPoint, System.currentTimeMillis())?.onSuccess { anchor ->
                    val origin = arOrigin
                    if (origin == null) {
                        history = history + listOf(points)
                        arOrigin = anchor.pose.positionMeters
                        points = listOf(CoordinatePoint(1, "A", 0f, 0f, CoordinateYellow, anchor.pose.positionMeters, anchor.id))
                        selectedId = 1
                    } else if (points.size < 20) {
                        val delta = anchor.pose.positionMeters - origin
                        val id = (points.maxOfOrNull { it.id } ?: 0) + 1
                        val palette = listOf(CoordinateRed, CoordinateYellow, CoordinateGreen, CoordinateBlue, CoordinatePurple, Color.White)
                        commit(
                            points + CoordinatePoint(
                                id = id,
                                name = ('A'.code + points.size).toChar().toString(),
                                x = physicalScale.lengthFromMeters(delta.x, com.indianservers.aiexplorer.arengine.measurement.ArLengthUnit.MathematicalUnit).toFloat().coerceIn(-5f, 5f),
                                y = physicalScale.lengthFromMeters(-delta.z, com.indianservers.aiexplorer.arengine.measurement.ArLengthUnit.MathematicalUnit).toFloat().coerceIn(-5f, 5f),
                                color = palette[points.size % palette.size],
                                worldPositionMeters = anchor.pose.positionMeters,
                                anchorId = anchor.id,
                            ),
                        )
                        selectedId = id
                    }
                }
            },
        )
        CoordinateToolTabs(selectedTool) { selectedTool = it }
        CoordinatePointWorkspace(
            points = points,
            selected = selected,
            locked = locked,
            onSelect = { selectedId = it },
            onDelete = { id ->
                if (!locked) {
                    commit(points.filterNot { it.id == id })
                    if (selectedId == id) selectedId = null
                }
            },
            onUpdate = { updated ->
                if (!locked) commit(points.map { if (it.id == updated.id) updated else it })
            },
            onAdd = { x, y, color ->
                if (points.size < 20 && !locked) {
                    val id = (points.maxOfOrNull { it.id } ?: 0) + 1
                    commit(points + CoordinatePoint(id, ('A'.code + points.size).toChar().toString(), x, y, color))
                    selectedId = id
                }
            },
        )
        CoordinateMeasurements(points, selectedTool, arLive)
        CoordinateBottomNav()
    }
}

@Composable
private fun CoordinateHeader(onBack: () -> Unit, arLive: Boolean, arStatus: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("< Back", color = CoordinatePurple, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AR Coordinate Plane", color = CoordinateText, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Plot points, draw lines and explore in AR", color = CoordinateMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Row(
            Modifier.background(CoordinateGreen.copy(.12f), RoundedCornerShape(18.dp)).border(1.dp, CoordinateGreen.copy(.35f), RoundedCornerShape(18.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(if (arLive) CoordinateGreen else CoordinateYellow, CircleShape))
            Text(if (arLive) "AR Active" else arStatus.take(18), color = if (arLive) CoordinateGreen else CoordinateYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun CoordinateArStage(
    points: List<CoordinatePoint>,
    selectedId: Int?,
    showGrid: Boolean,
    showAxes: Boolean,
    showOrigin: Boolean,
    locked: Boolean,
    arLive: Boolean,
    activeTool: CoordinateTool,
    canUndo: Boolean,
    onPointSelected: (Int) -> Unit,
    onPlot: (Float, Float) -> Unit,
    onGrid: () -> Unit,
    onAxes: () -> Unit,
    onOrigin: () -> Unit,
    onLock: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onArStateChanged: (Boolean, String) -> Unit,
    onArSessionReady: (ArLabSessionController?) -> Unit,
    onArPlot: (ArVector2) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .background(Brush.verticalGradient(listOf(Color(0xFF33251C), Color(0xFF1B1715), Color(0xFF111820))), RoundedCornerShape(16.dp))
            .border(1.dp, CoordinateBorder, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "AR coordinate plane with plotted points" },
    ) {
        SharedARLabCamera(
            modifier = Modifier.fillMaxSize(),
            labId = "ar-coordinate-plane",
            needsDepth = true,
            onStateChanged = onArStateChanged,
            onSessionReady = onArSessionReady,
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .pointerInput(points, locked, activeTool, arLive) {
                    detectTapGestures { tap ->
                        val left = size.width * .08f
                        val right = size.width * .85f
                        val top = size.height * .15f
                        val bottom = size.height * .90f
                        fun screen(point: CoordinatePoint) = Offset(
                            left + (point.x + 5f) / 10f * (right - left),
                            bottom - (point.y + 5f) / 10f * (bottom - top),
                        )
                        val hit = points.minByOrNull { (screen(it) - tap).getDistance() }
                        if (hit != null && (screen(hit) - tap).getDistance() < 42f) {
                            onPointSelected(hit.id)
                        } else if (activeTool == CoordinateTool.Plot && !locked && arLive) {
                            onArPlot(ArVector2(tap.x + 24.dp.toPx(), tap.y + 18.dp.toPx()))
                        } else if (activeTool == CoordinateTool.Plot && !locked && tap.x in left..right && tap.y in top..bottom) {
                            val x = ((tap.x - left) / (right - left) * 10f - 5f).coerceIn(-5f, 5f)
                            val y = (5f - (tap.y - top) / (bottom - top) * 10f).coerceIn(-5f, 5f)
                            onPlot((x * 2f).roundToInt() / 2f, (y * 2f).roundToInt() / 2f)
                        }
                    }
                },
        ) {
            val left = size.width * .08f
            val right = size.width * .85f
            val top = size.height * .15f
            val bottom = size.height * .90f
            fun map(x: Float, y: Float) = Offset(left + (x + 5f) / 10f * (right - left), bottom - (y + 5f) / 10f * (bottom - top))
            fun label(text: String, at: Offset, color: Color, size: Float = 26f, center: Boolean = false) {
                drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y, android.graphics.Paint().apply {
                    this.color = android.graphics.Color.argb((color.alpha * 255).roundToInt(), (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt())
                    textSize = size
                    isAntiAlias = true
                    isFakeBoldText = true
                    textAlign = if (center) android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT
                })
            }
            if (showGrid) {
                (-5..5).forEach { tick ->
                    val xLine = map(tick.toFloat(), 0f).x
                    val yLine = map(0f, tick.toFloat()).y
                    drawLine(Color.White.copy(if (tick == 0) .28f else .15f), Offset(xLine, top), Offset(xLine, bottom), if (tick == 0) 2f else 1f)
                    drawLine(Color.White.copy(if (tick == 0) .28f else .15f), Offset(left, yLine), Offset(right, yLine), if (tick == 0) 2f else 1f)
                    if (tick != 0) {
                        label(tick.toString(), Offset(xLine, map(0f, 0f).y + 24f), CoordinateMuted, 19f, true)
                        label(tick.toString(), Offset(map(0f, 0f).x - 15f, yLine + 6f), CoordinateMuted, 19f, true)
                    }
                }
            }
            if (showAxes) {
                val origin = map(0f, 0f)
                drawLine(CoordinateRed, Offset(left, origin.y), Offset(right, origin.y), 4f, StrokeCap.Round)
                drawLine(CoordinateGreen, Offset(origin.x, bottom), Offset(origin.x, top), 4f, StrokeCap.Round)
                label("X", Offset(right + 8f, origin.y + 8f), CoordinateText, 30f)
                label("Y", Offset(origin.x - 8f, top - 12f), CoordinateText, 30f, true)
            }
            if (showOrigin) {
                drawCircle(CoordinateText, 6f, map(0f, 0f))
                label("0", map(0f, 0f) + Offset(10f, 24f), CoordinateText, 20f)
            }
            if (points.size >= 2 && activeTool in setOf(CoordinateTool.Line, CoordinateTool.Measure, CoordinateTool.Shapes)) {
                val path = Path().apply {
                    val first = map(points.first().x, points.first().y)
                    moveTo(first.x, first.y)
                    points.drop(1).forEach { lineTo(map(it.x, it.y).x, map(it.x, it.y).y) }
                    if (activeTool == CoordinateTool.Shapes && points.size >= 3) close()
                }
                if (activeTool == CoordinateTool.Shapes && points.size >= 3) {
                    drawPath(path, CoordinatePurple.copy(.16f))
                }
                drawPath(path, if (activeTool == CoordinateTool.Measure) CoordinateYellow else CoordinatePurple, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, pathEffect = if (activeTool == CoordinateTool.Measure) PathEffect.dashPathEffect(floatArrayOf(9f, 7f)) else null))
            }
            points.forEach { point ->
                val p = map(point.x, point.y)
                val axis = map(point.x, 0f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
                drawLine(point.color.copy(.72f), p, axis, 2f, pathEffect = dash)
                drawLine(point.color.copy(.72f), axis, map(0f, 0f), 2f, pathEffect = dash)
                if (point.id == selectedId) drawCircle(Color.White.copy(.38f), 15f, p)
                drawCircle(point.color, 9f, p)
                val yOffset = if (point.y >= 0f) -16f else 30f
                label("(${coord(point.x)}, ${coord(point.y)})", p + Offset(-4f, yOffset), point.color, 24f, true)
            }
        }
        CoordinateStageButton("Calibrate Plane", Modifier.align(Alignment.TopStart).padding(14.dp))
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).background(Color(0xD9091019), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.12f), RoundedCornerShape(14.dp)),
        ) {
            CoordinateToggle("Axes", showAxes, onAxes)
            CoordinateToggle("Grid", showGrid, onGrid)
            CoordinateToggle("Origin", showOrigin, onOrigin)
            CoordinateToggle("Lock", locked, onLock)
            CoordinateToggle("Reset", false, onReset)
        }
        Row(Modifier.align(Alignment.BottomStart).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoordinateStageButton(if (canUndo) "Undo" else "Undo", Modifier.clickable(enabled = canUndo, onClick = onUndo), if (canUndo) CoordinateText else CoordinateMuted)
            CoordinateStageButton("Redo", Modifier, CoordinateMuted)
        }
        CoordinateStageButton("3D View", Modifier.align(Alignment.BottomEnd).padding(14.dp), CoordinatePurple)
    }
}

@Composable
private fun CoordinateStageButton(label: String, modifier: Modifier, accent: Color = CoordinateText) {
    Row(modifier.background(Color(0xD9091019), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.24f), RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CoordinateToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(66.dp).clickable(onClick = onClick).background(if (selected) CoordinatePurple.copy(.16f) else Color.Transparent).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(if (selected) "*" else "+", color = if (selected) CoordinatePurple else CoordinateText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CoordinateText, fontSize = 10.sp)
    }
}

@Composable
private fun CoordinateToolTabs(selected: CoordinateTool, onSelect: (CoordinateTool) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(CoordinatePanel, RoundedCornerShape(12.dp)).border(1.dp, CoordinateBorder, RoundedCornerShape(12.dp))) {
        CoordinateTool.entries.forEach { tool ->
            Row(
                Modifier.width(150.dp).clickable { onSelect(tool) }.background(if (tool == selected) CoordinatePurple.copy(.14f) else Color.Transparent).border(1.dp, if (tool == selected) CoordinatePurple else Color.Transparent).padding(horizontal = 13.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tool.symbol, color = if (tool == selected) CoordinatePurple else CoordinateMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(tool.label, color = if (tool == selected) CoordinateText else CoordinateMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CoordinatePointWorkspace(
    points: List<CoordinatePoint>,
    selected: CoordinatePoint?,
    locked: Boolean,
    onSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onUpdate: (CoordinatePoint) -> Unit,
    onAdd: (Float, Float, Color) -> Unit,
) {
    var xText by remember(selected?.id) { mutableStateOf(selected?.x?.let(::coord) ?: "0") }
    var yText by remember(selected?.id) { mutableStateOf(selected?.y?.let(::coord) ?: "0") }
    var chosenColor by remember(selected?.id) { mutableStateOf(selected?.color ?: CoordinateGreen) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Column(coordinatePanel(Modifier.weight(1.25f)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Points", color = CoordinateText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${points.size} / 20 points used", color = CoordinatePurple, fontSize = 12.sp)
            }
            points.forEach { point ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(point.id) }.background(if (selected?.id == point.id) CoordinatePurple.copy(.10f) else Color.Transparent, RoundedCornerShape(8.dp)).border(1.dp, if (selected?.id == point.id) CoordinatePurple.copy(.45f) else CoordinateBorder, RoundedCornerShape(8.dp)).padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(15.dp).background(point.color, CircleShape))
                    Text(point.name, color = CoordinateMuted, fontSize = 13.sp)
                    Text("(${coord(point.x)}, ${coord(point.y)})", color = CoordinateText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("Edit", color = CoordinateMuted, fontSize = 11.sp)
                    Text("Delete", color = CoordinateRed, fontSize = 11.sp, modifier = Modifier.clickable(enabled = !locked) { onDelete(point.id) })
                }
            }
        }
        Column(coordinatePanel(Modifier.weight(1f)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (selected == null) "New Point" else "Point ${selected.name}", color = CoordinateText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoordinateInput("x", xText, Modifier.weight(1f)) {
                    xText = it
                    val value = it.toFloatOrNull()
                    if (selected != null && value != null) onUpdate(selected.copy(x = value.coerceIn(-5f, 5f)))
                }
                CoordinateInput("y", yText, Modifier.weight(1f)) {
                    yText = it
                    val value = it.toFloatOrNull()
                    if (selected != null && value != null) onUpdate(selected.copy(y = value.coerceIn(-5f, 5f)))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(CoordinateRed, CoordinateYellow, CoordinateGreen, CoordinateBlue, CoordinatePurple, Color.White).forEach { color ->
                    Box(
                        Modifier.size(29.dp).background(color, CircleShape).border(if (chosenColor == color) 3.dp else 1.dp, if (chosenColor == color) Color.White else CoordinateBorder, CircleShape).clickable { chosenColor = color },
                    )
                }
            }
            Button(
                onClick = {
                    val x = xText.toFloatOrNull() ?: 0f
                    val y = yText.toFloatOrNull() ?: 0f
                    if (selected == null) onAdd(x.coerceIn(-5f, 5f), y.coerceIn(-5f, 5f), chosenColor)
                    else onUpdate(selected.copy(x = x.coerceIn(-5f, 5f), y = y.coerceIn(-5f, 5f), color = chosenColor))
                },
                enabled = !locked && (selected != null || points.size < 20),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoordinatePurple, contentColor = CoordinateText),
            ) {
                Text(if (selected == null) "+ Add Point" else "Update Point", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoordinateInput(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = CoordinateMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = CoordinateText, fontSize = 14.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoordinatePurple,
                unfocusedBorderColor = CoordinateBorder,
                focusedContainerColor = Color(0xFF050A11),
                unfocusedContainerColor = Color(0xFF050A11),
                cursorColor = CoordinatePurple,
            ),
        )
    }
}

@Composable
private fun CoordinateMeasurements(points: List<CoordinatePoint>, tool: CoordinateTool, arLive: Boolean) {
    val a = points.getOrNull(0)
    val b = points.getOrNull(1)
    val c = points.getOrNull(2)
    fun measured(point: CoordinatePoint?) = point?.worldPositionMeters?.let { ArMeasuredPoint(point.name, it, .03, .65) }
    fun distance(p: CoordinatePoint?, q: CoordinatePoint?): String {
        val worldP = measured(p)
        val worldQ = measured(q)
        return when {
            worldP != null && worldQ != null -> "${ArMeasurementFormatter.number(ArMeasurementMath.distance(worldP, worldQ).distanceMeters * 100.0, 3.0)} cm"
            p == null || q == null -> "--"
            else -> coord(sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y)))
        }
    }
    val midpoint = if (a != null && b != null) "(${coord((a.x + b.x) / 2f)}, ${coord((a.y + b.y) / 2f)})" else "--"
    val slope = if (a != null && b != null && b.x != a.x) coord((b.y - a.y) / (b.x - a.x)) else "Undefined"
    val worldA = measured(a)
    val worldB = measured(b)
    val worldC = measured(c)
    val angle = if (worldA != null && worldB != null && worldC != null) {
        "${ArMeasurementFormatter.number(ArMeasurementMath.angle(worldA, worldB, worldC).degrees, 1.0)} deg"
    } else if (a != null && b != null && c != null) {
        runCatching {
            val result = ArMeasurementMath.angle(
                ArMeasuredPoint("A", ArVector3(a.x.toDouble(), a.y.toDouble(), 0.0)),
                ArMeasuredPoint("B", ArVector3(b.x.toDouble(), b.y.toDouble(), 0.0)),
                ArMeasuredPoint("C", ArVector3(c.x.toDouble(), c.y.toDouble(), 0.0)),
            )
            "${ArMeasurementFormatter.number(result.degrees, 1.0)} deg"
        }.getOrDefault("--")
    } else "--"
    Column(coordinatePanel(Modifier.fillMaxWidth()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceEvenly) {
            CoordinateMetric("Distance AB", distance(a, b))
            CoordinateMetric("Distance AC", distance(a, c))
            CoordinateMetric("Midpoint AB", midpoint)
            CoordinateMetric("Slope AB", slope)
            if (tool == CoordinateTool.Measure) CoordinateMetric("Angle ABC", angle)
        }
        if (arLive && points.any { it.worldPositionMeters != null }) {
            Text("Approximate AR measurement - accuracy depends on tracking, lighting and surface detail.", color = CoordinateYellow, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CoordinateMetric(label: String, value: String) {
    Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = CoordinateMuted, fontSize = 11.sp)
        Text(value, color = CoordinateText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CoordinateBottomNav() {
    Row(
        Modifier.fillMaxWidth().background(CoordinatePanel, RoundedCornerShape(18.dp)).border(1.dp, CoordinateBorder, RoundedCornerShape(18.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("3D Graph", "3D Shapes", "AR Labs", "2D Graph", "Formula Explorer").forEach { item ->
            Text(
                item,
                color = if (item == "AR Labs") CoordinatePurple else CoordinateMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (item == "AR Labs") FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.background(if (item == "AR Labs") CoordinatePurple.copy(.12f) else Color.Transparent, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 10.dp),
            )
        }
    }
}

private fun coordinatePanel(modifier: Modifier) = modifier
    .background(CoordinatePanel, RoundedCornerShape(12.dp))
    .border(1.dp, CoordinateBorder, RoundedCornerShape(12.dp))
    .padding(12.dp)

private fun coord(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}
