package com.indianservers.aiexplorer.mathworkspace.vector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.FloatingWorkspaceTool
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import com.indianservers.aiexplorer.mathworkspace.components.NumericField
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceAction
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSegmentedControl
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSectionTitle
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceTopBar
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceBottomSheet
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceFloatingTools
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.pow

@Composable
fun VectorLabWorkspace(viewModel: VectorLabViewModel, onBack: () -> Unit) {
    val vm = viewModel
    val dimension by vm.dimension
    val stop by vm.sheetStop
    val panel by vm.panel
    val density = LocalDensity.current
    val vectorA = vm.vectors.firstOrNull { it.id == vm.selectedA.value } ?: vm.vectors.firstOrNull()
    val vectorB = vm.vectors.firstOrNull { it.id == vm.selectedB.value && it.id != vectorA?.id }
        ?: vm.vectors.firstOrNull { it.id != vectorA?.id }
    val a = vectorA?.let { MathVector3(it.x, it.y, it.z) } ?: MathVector3(0.0, 0.0)
    val b = vectorB?.let { MathVector3(it.x, it.y, it.z) } ?: MathVector3(0.0, 0.0)
    val result = remember(vm.operation.value, a, b, vm.alpha.value, vm.beta.value) {
        VectorMath.calculate(vm.operation.value, a, b, vm.alpha.value.toDoubleOrNull() ?: 1.0, vm.beta.value.toDoubleOrNull() ?: 1.0)
    }
    MathWorkspaceShell(
        title = "Vector Lab",
        subtitle = "Explore vectors in an open coordinate space",
        onBack = onBack,
        sheetTitle = if (panel == VectorPanel.Vectors) "Vectors" else "Operations",
        sheetStop = stop,
        onSheetStopChange = { vm.sheetStop.value = it },
        topBarTrailing = {
            WorkspaceSegmentedControl(listOf("2D", "3D"), if (dimension == VectorDimension.TwoD) 0 else 1) {
                vm.dimension.value = if (it == 0) VectorDimension.TwoD else VectorDimension.ThreeD
            }
        },
        canvas = {
            VectorCanvas(vm, result, Modifier.fillMaxSize())
        },
        tools = {
            WorkspaceFloatingTools(
                listOf(
                    FloatingWorkspaceTool("Fit vectors", "◎") { vm.fitToVectors() },
                    FloatingWorkspaceTool("Zoom in", "+") { vm.zoom.value = (vm.zoom.value * 1.3f).coerceAtMost(100000f) },
                    FloatingWorkspaceTool("Zoom out", "−") { vm.zoom.value = (vm.zoom.value / 1.3f).coerceAtLeast(.0001f) },
                    FloatingWorkspaceTool("Toggle grid", if (vm.showGrid.value) "Grid" else "Grid off", vm.showGrid.value) { vm.showGrid.value = !vm.showGrid.value },
                    FloatingWorkspaceTool("Add vector", "+") { vm.addVector() },
                ),
                Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            )
        },
        sheet = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkspaceAction("Vectors", "Edit vectors", { vm.panel.value = VectorPanel.Vectors }, panel == VectorPanel.Vectors)
                WorkspaceAction("Operations", "Vector operations", { vm.panel.value = VectorPanel.Operations }, panel == VectorPanel.Operations)
                Spacer(Modifier.weight(1f))
                WorkspaceAction("Undo", "Undo vector edit", vm::undo, enabled = vm.canUndo)
                WorkspaceAction("Redo", "Redo vector edit", vm::redo, enabled = vm.canRedo)
            }
            if (panel == VectorPanel.Vectors) VectorEditorPanel(vm, dimension)
            else VectorOperationsPanel(vm, dimension, a, b, result)
        },
    )
}

@Composable
private fun VectorCanvas(vm: VectorLabViewModel, result: VectorOperationResult, modifier: Modifier = Modifier) {
    val vectors = vm.vectors.toList()
    val dimension = vm.dimension.value
    val zoom = vm.zoom.value
    val pan = vm.pan.value
    val density = LocalDensity.current
    BoxWithConstraints(modifier.background(P.snow).pointerInput(vm) {
        detectTransformGestures { centroid, panDelta, zoomChange, _ ->
            vm.zoom.value = (vm.zoom.value * zoomChange).coerceIn(.0001f, 100000f)
            val adjusted = centroid - Offset(size.width / 2f, size.height / 2f) - vm.pan.value
            vm.pan.value = vm.pan.value + panDelta + adjusted * (1f - zoomChange)
        }
    }) {
        val canvasWidth = with(density) { maxWidth.toPx() }
        val canvasHeight = with(density) { maxHeight.toPx() }
        val unitPx = (minOf(canvasWidth, canvasHeight) / 10f) * zoom
        Canvas(Modifier.fillMaxSize()) {
            val unit = (size.minDimension / 10f) * zoom
            val origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
            fun project(v: MathVector3): Offset = if (dimension == VectorDimension.TwoD) {
                Offset(origin.x + v.x.toFloat() * unit, origin.y - v.y.toFloat() * unit)
            } else {
                Offset(origin.x + ((v.x - v.y) * .7071).toFloat() * unit, origin.y - ((v.x + v.y) * .38 - v.z * .88).toFloat() * unit)
            }
            if (vm.showGrid.value) drawVectorGrid(origin, unit, dimension)
            val axisPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(101, 122, 155); textSize = 12.sp.toPx()
            }
            val axes = if (dimension == VectorDimension.TwoD) listOf(
                Triple(MathVector3(1.0, 0.0), P.red, "x"), Triple(MathVector3(0.0, 1.0), P.green, "y"),
            ) else listOf(
                Triple(MathVector3(1.0, 0.0, 0.0), P.red, "x"), Triple(MathVector3(0.0, 1.0, 0.0), P.green, "y"), Triple(MathVector3(0.0, 0.0, 1.0), P.blue, "z"),
            )
            axes.forEach { (axis, color, label) -> drawVectorArrow(project(MathVector3(0.0, 0.0)), project(axis * 4.0), color, 2.2f); drawContext.canvas.nativeCanvas.drawText(label, project(axis * 4.0).x + 7f, project(axis * 4.0).y - 7f, axisPaint) }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 13.sp.toPx(); typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
            val shown = vectors.filter { it.visible }
            shown.forEach { v ->
                val end = MathVector3(v.x, v.y, v.z)
                val color = Color(v.color)
                drawVectorArrow(project(MathVector3(0.0, 0.0)), project(end), color, 4f)
                paint.color = v.color.toInt()
                drawContext.canvas.nativeCanvas.drawText("${v.name} (${fmt(v.x)}, ${fmt(v.y)}${if (dimension == VectorDimension.ThreeD) ", ${fmt(v.z)}" else ""})", project(end).x + 10f, project(end).y - 10f, paint)
            }
            val va = vm.vectors.firstOrNull { it.id == vm.selectedA.value }
            val vb = vm.vectors.firstOrNull { it.id == vm.selectedB.value && it.id != va?.id }
            val av = va?.let { MathVector3(it.x, it.y, it.z) } ?: MathVector3(0.0, 0.0)
            val bv = vb?.let { MathVector3(it.x, it.y, it.z) } ?: MathVector3(0.0, 0.0)
            if (vm.operation.value == VectorOperation.Add) {
                val o = project(MathVector3(0.0, 0.0)); val ae = project(av); val be = project(bv)
                drawLine(P.violet.copy(alpha = .58f), ae, project(av + bv), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
                drawLine(P.violet.copy(alpha = .58f), be, project(av + bv), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            }
            if (vm.operation.value == VectorOperation.Projection) {
                val projection = av.projectionOn(bv)
                drawLine(P.amber.copy(.8f), project(av), project(projection), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)))
            }
            if (vm.operation.value == VectorOperation.Angle && dimension == VectorDimension.TwoD) {
                val start = Math.toDegrees(kotlin.math.atan2(av.y, av.x)).toFloat()
                val end = Math.toDegrees(kotlin.math.atan2(bv.y, bv.x)).toFloat()
                var sweep = end - start
                while (sweep > 180f) sweep -= 360f
                while (sweep < -180f) sweep += 360f
                val radius = (unit * minOf(av.magnitude(), bv.magnitude(), 1.2).toFloat()).coerceAtLeast(18f)
                drawArc(P.violet, startAngle = -start, sweepAngle = -sweep, useCenter = false, topLeft = Offset(origin.x - radius, origin.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(2.dp.toPx()))
            }
            result.vector?.takeIf { vm.operation.value != VectorOperation.Unit || it.magnitude() > 0.0 }?.let {
                drawVectorArrow(project(MathVector3(0.0, 0.0)), project(it), P.violet, 3f)
                paint.color = P.violet.toArgbCompat()
                drawContext.canvas.nativeCanvas.drawText("${result.label} ${vectorText(it, dimension)}", project(it).x + 10f, project(it).y + 18f, paint)
            }
            if (result.scalar != null) {
                paint.color = P.ink.toArgbCompat()
                drawContext.canvas.nativeCanvas.drawText("${result.label} = ${fmt(result.scalar)}", 18f, size.height - 20f, paint)
            }
        }
        vectors.filter { it.visible }.forEach { vector ->
            val position = if (dimension == VectorDimension.TwoD) {
                Offset(canvasWidth / 2f + pan.x + vector.x.toFloat() * unitPx, canvasHeight / 2f + pan.y - vector.y.toFloat() * unitPx)
            } else {
                Offset(canvasWidth / 2f + pan.x + ((vector.x - vector.y) * .7071).toFloat() * unitPx, canvasHeight / 2f + pan.y - ((vector.x + vector.y) * .38 - vector.z * .88).toFloat() * unitPx)
            }
            Box(
                Modifier.offset { IntOffset(position.x.roundToInt() - with(density) { 11.dp.roundToPx() }, position.y.roundToInt() - with(density) { 11.dp.roundToPx() }) }
                    .size(22.dp).clip(CircleShape).background(Color(vector.color)).border(2.dp, Color.White, CircleShape)
                    .semantics { contentDescription = "Drag endpoint of vector ${vector.name}" }
                    .pointerInput(vector.id, dimension, zoom) {
                        detectDragGestures(
                            onDragStart = { vm.beginEndpointDrag() },
                            onDrag = { change, delta ->
                                change.consume()
                                val scalePx = unitPx
                                val mathDelta = if (dimension == VectorDimension.TwoD) MathVector3((delta.x / scalePx).toDouble(), (-delta.y / scalePx).toDouble()) else {
                                    val u = delta.x / (scalePx * .7071f)
                                    val v = -delta.y / (scalePx * .38f)
                                    MathVector3((u + v) / 2.0, (v - u) / 2.0, 0.0)
                                }
                                vm.dragEndpoint(vector.id, mathDelta)
                            },
                            onDragEnd = vm::endEndpointDrag,
                            onDragCancel = vm::endEndpointDrag,
                        )
                    },
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVectorGrid(origin: Offset, unit: Float, dimension: VectorDimension) {
    if (unit <= 1f || unit.isNaN()) return
    val target = 52f / unit
    val power = 10.0.pow(kotlin.math.floor(kotlin.math.log10(target.coerceAtLeast(1e-10f).toDouble())))
    val base = target / power
    val step = ((if (base < 1.5) 1.0 else if (base < 3.5) 2.0 else if (base < 7.5) 5.0 else 10.0) * power).toFloat()
    val screenStep = unit * step
    val countX = (size.width / screenStep).toInt().coerceIn(1, 180)
    val countY = (size.height / screenStep).toInt().coerceIn(1, 180)
    for (i in -countX..countX) {
        val x = origin.x + i * screenStep
        drawLine(if (i == 0) P.muted.copy(.45f) else P.border.copy(.55f), Offset(x, 0f), Offset(x, size.height), if (i == 0) 1.5f else 1f)
    }
    for (i in -countY..countY) {
        val y = origin.y + i * screenStep
        drawLine(if (i == 0) P.muted.copy(.45f) else P.border.copy(.55f), Offset(0f, y), Offset(size.width, y), if (i == 0) 1.5f else 1f)
    }
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(101, 122, 155); textSize = 10.sp.toPx() }
    for (i in -countX..countX step maxOf(1, (3f / step).toInt())) {
        if (i != 0) drawContext.canvas.nativeCanvas.drawText(fmt((i * step).toDouble()), origin.x + i * screenStep + 2f, origin.y + 13f, labelPaint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVectorArrow(start: Offset, end: Offset, color: Color, stroke: Float) {
    drawLine(color.copy(alpha = .14f), start, end, stroke + 7f, StrokeCap.Round)
    drawLine(color, start, end, stroke, StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val head = 11f + stroke
    drawLine(color, end, Offset(end.x + cos(angle + 2.55f) * head, end.y + sin(angle + 2.55f) * head), stroke, StrokeCap.Round)
    drawLine(color, end, Offset(end.x + cos(angle - 2.55f) * head, end.y + sin(angle - 2.55f) * head), stroke, StrokeCap.Round)
}

@Composable
private fun VectorEditorPanel(vm: VectorLabViewModel, dimension: VectorDimension) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.vectors.forEach { vector ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(P.snow).border(1.dp, P.border, RoundedCornerShape(12.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(vector.color)))
                    VectorNameField(vector.name, Modifier.weight(1f)) { name -> vm.update(vector.id) { v -> v.copy(name = name) } }
                    Text("Visible", color = P.muted, fontSize = 10.sp)
                    Switch(checked = vector.visible, onCheckedChange = { vm.update(vector.id) { v -> v.copy(visible = it) } })
                    WorkspaceAction("×", "Delete vector ${vector.name}", { vm.remove(vector.id) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    VectorNumberField("x", vector.x, Modifier.weight(1f)) { vm.update(vector.id) { v -> v.copy(x = it) } }
                    VectorNumberField("y", vector.y, Modifier.weight(1f)) { vm.update(vector.id) { v -> v.copy(y = it) } }
                    if (dimension == VectorDimension.ThreeD) VectorNumberField("z", vector.z, Modifier.weight(1f)) { vm.update(vector.id) { v -> v.copy(z = it) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    WorkspaceAction(if (vm.selectedA.value == vector.id) "A selected" else "Use as A", "Select ${vector.name} as vector A", { vm.selectedA.value = vector.id }, vm.selectedA.value == vector.id)
                    WorkspaceAction(if (vm.selectedB.value == vector.id) "B selected" else "Use as B", "Select ${vector.name} as vector B", { vm.selectedB.value = vector.id }, vm.selectedB.value == vector.id)
                }
            }
        }
        WorkspaceAction("+ Add vector", "Add a vector", { vm.addVector() })
    }
}

@Composable
private fun VectorNumberField(label: String, value: Double, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    var text by androidx.compose.runtime.remember(label) { androidx.compose.runtime.mutableStateOf(fmt(value)) }
    LaunchedEffect(value) { if (text.toDoubleOrNull() != value) text = fmt(value) }
    NumericField(label, text, { next -> text = next; next.toDoubleOrNull()?.let(onChange) }, modifier)
}

@Composable
private fun VectorNameField(value: String, modifier: Modifier, onChange: (String) -> Unit) {
    var text by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableStateOf(value) }
    androidx.compose.foundation.text.BasicTextField(
        value = text,
        onValueChange = { text = it; if (it.isNotBlank()) onChange(it) },
        modifier = modifier.background(Color.White, RoundedCornerShape(7.dp)).padding(horizontal = 7.dp, vertical = 5.dp)
            .semantics { contentDescription = "Vector name" },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun VectorOperationsPanel(vm: VectorLabViewModel, dimension: VectorDimension, a: MathVector3, b: MathVector3, result: VectorOperationResult) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VectorOperation.entries.forEach { operation ->
                WorkspaceAction(operation.title, operation.title, { vm.operation.value = operation }, vm.operation.value == operation)
            }
        }
        if (vm.operation.value == VectorOperation.Scalar) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            NumericField("α", vm.alpha.value, { value -> if (value.toDoubleOrNull() != null || value in listOf("", "-", ".", "-.")) vm.alpha.value = value }, Modifier.weight(1f))
            NumericField("β", vm.beta.value, { value -> if (value.toDoubleOrNull() != null || value in listOf("", "-", ".", "-.")) vm.beta.value = value }, Modifier.weight(1f))
        }
        WorkspaceSectionTitle("Result · ${result.label}")
        Text(result.vector?.let { vectorText(it, dimension) } ?: result.scalar?.let { fmt(it) } ?: "Undefined for a zero vector", color = P.blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("A = ${vectorText(a, dimension)}    B = ${vectorText(b, dimension)}", color = P.muted, fontSize = 12.sp)
    }
}

private fun vectorText(v: MathVector3, dimension: VectorDimension) = if (dimension == VectorDimension.TwoD) "[${fmt(v.x)}, ${fmt(v.y)}]" else "[${fmt(v.x)}, ${fmt(v.y)}, ${fmt(v.z)}]"
private fun fmt(value: Double): String = if (!value.isFinite()) "undefined" else if (kotlin.math.abs(value) < 1e-10) "0" else String.format(java.util.Locale.ROOT, "%.4g", value)
private fun Color.toArgbCompat(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
