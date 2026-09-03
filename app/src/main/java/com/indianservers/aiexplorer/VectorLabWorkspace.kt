package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.core.VectorLabEngine
import com.indianservers.aiexplorer.core.formatCoordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val VectorLabBg get() = themedColor(Color(0xFF040811), Background)
private val VectorLabPanel get() = themedColor(Color(0xFF0A1220), SurfaceA)
private val VectorLabBorder get() = themedColor(Color(0xFF26344D), AppBorder)
private val VectorLabCyan get() = themedColor(Color(0xFF38D9FF), Cyan)
private val VectorLabBlue get() = themedColor(Color(0xFF4A8DFF), ActiveAppPalette.primary)
private val VectorLabViolet get() = themedColor(Color(0xFFA878FF), Violet)
private val VectorLabGreen get() = themedColor(Color(0xFF55E6A5), Green)
private val VectorLabAmber get() = themedColor(Color(0xFFFFC857), Amber)

private enum class VectorLabOperation(val label: String) {
    Add("a + b"), Subtract("a − b"), Dot("a · b"), Cross("a × b"), Projection("projᵦ a"), Scalar("αa + βb")
}

@Composable
internal fun VectorLabWorkspace(vm: ExplorerViewModel) {
    var firstIndex by remember { mutableStateOf(0) }
    var secondIndex by remember { mutableStateOf(1) }
    var operation by remember { mutableStateOf(VectorLabOperation.Add) }
    var alpha by remember { mutableStateOf(2.0) }
    var beta by remember { mutableStateOf(-1.0) }
    var isometric by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }

    val vectors = vm.state.vectors3D
    val effectiveFirstIndex = firstIndex.takeIf { it in vectors.indices } ?: (vectors.indices.firstOrNull() ?: -1)
    val effectiveSecondIndex = secondIndex.takeIf { it in vectors.indices && it != effectiveFirstIndex }
        ?: (vectors.indices.firstOrNull { it != effectiveFirstIndex } ?: -1)
    val first = vectors.getOrNull(effectiveFirstIndex)
    val second = vectors.getOrNull(effectiveSecondIndex)
    val a = first?.components ?: Vec3(0.0, 0.0, 0.0)
    val b = second?.components ?: Vec3(0.0, 0.0, 0.0)
    val analysis = remember(a, b) { VectorLabEngine.analyse(a, b) }
    val orthonormal = remember(a, b) { VectorLabEngine.gramSchmidt(a, b) }
    val result = when (operation) {
        VectorLabOperation.Add -> analysis.sum
        VectorLabOperation.Subtract -> analysis.difference
        VectorLabOperation.Cross -> analysis.cross
        VectorLabOperation.Projection -> analysis.projectionAOnB
        VectorLabOperation.Scalar -> VectorLabEngine.linearCombination(a, alpha, b, beta)
        VectorLabOperation.Dot -> null
    }

    Column(
        Modifier.fillMaxSize().background(VectorLabBg).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Vector Lab", color = VectorLabViolet, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Build vectors, compare components, and explore products geometrically", color = Color(0xFFABB8CC), fontSize = 12.sp)
            }
            Text("${vectors.size} vector${if (vectors.size == 1) "" else "s"}", color = VectorLabGreen, fontSize = 11.sp)
        }

        if (vectors.isEmpty()) {
            EmptyVectorLab { addExampleVectors(vm) }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VectorLabOperation.entries.forEach { candidate ->
                    Button(
                        onClick = { operation = candidate },
                        colors = ButtonDefaults.buttonColors(containerColor = if (operation == candidate) VectorLabViolet else VectorLabPanel),
                    ) { Text(candidate.label) }
                }
                OutlinedButton(onClick = { isometric = !isometric }) { Text(if (isometric) "3D view" else "XY view") }
                OutlinedButton(onClick = { showGrid = !showGrid }) { Text(if (showGrid) "Grid on" else "Grid off") }
            }

            VectorLabCanvas(a, b, result, operation, isometric, showGrid)

            if (operation == VectorLabOperation.Scalar) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("α ${formatCoordinate(alpha)}", color = VectorLabCyan, modifier = Modifier.width(58.dp))
                    Slider(alpha.toFloat(), { alpha = it.toDouble() }, valueRange = -4f..4f, modifier = Modifier.weight(1f))
                    Text("β ${formatCoordinate(beta)}", color = VectorLabBlue, modifier = Modifier.width(58.dp))
                    Slider(beta.toFloat(), { beta = it.toDouble() }, valueRange = -4f..4f, modifier = Modifier.weight(1f))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                VectorEditor("Vector a", VectorLabCyan, effectiveFirstIndex, first, vectors, Modifier.weight(1f), onSelect = { firstIndex = it }, onUpdate = { index, vector -> vm.transformVector3D(index) { vector } }, onDelete = vm::deleteVector3D)
                VectorEditor("Vector b", VectorLabBlue, effectiveSecondIndex, second, vectors, Modifier.weight(1f), onSelect = { secondIndex = it }, onUpdate = { index, vector -> vm.transformVector3D(index) { vector } }, onDelete = vm::deleteVector3D)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VectorMetric("|a|", formatCoordinate(a.magnitude()), VectorLabCyan, Modifier.weight(1f))
                VectorMetric("|b|", formatCoordinate(b.magnitude()), VectorLabBlue, Modifier.weight(1f))
                VectorMetric("a · b", formatCoordinate(analysis.dot), VectorLabAmber, Modifier.weight(1f))
                VectorMetric("Angle", analysis.angleDegrees?.let { "${formatCoordinate(it)}°" } ?: "undefined", VectorLabGreen, Modifier.weight(1f))
            }

            Column(Modifier.fillMaxWidth().background(VectorLabPanel, RoundedCornerShape(14.dp)).border(1.dp, VectorLabGreen.copy(.45f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("RELATIONSHIP DIAGNOSTICS", color = VectorLabGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    when {
                        analysis.areParallel -> "The vectors are parallel; their span is one-dimensional."
                        analysis.areOrthogonal -> "The vectors are orthogonal; their dot product is zero."
                        else -> "The vectors are independent in their plane and meet at ${analysis.angleDegrees?.let { "${formatCoordinate(it)}°" } ?: "an undefined angle"}."
                    },
                    color = Color.White,
                )
                Text("cos θ = ${analysis.cosineSimilarity?.let(::formatCoordinate) ?: "undefined"} · compᵦ a = ${analysis.scalarComponentAOnB?.let(::formatCoordinate) ?: "undefined"} · parallelogram area = ${formatCoordinate(analysis.parallelogramArea)}", color = Color(0xFFABB8CC), fontSize = 12.sp)
                if (orthonormal != null) {
                    Text("Gram–Schmidt basis: e₁ ${vectorText(orthonormal.first)}, e₂ ${vectorText(orthonormal.second)}", color = VectorLabCyan, fontSize = 12.sp)
                    OutlinedButton(onClick = {
                        vm.addVector3D("e1", Vec3(0.0, 0.0, 0.0), orthonormal.first, "orthonormal basis")
                        vm.addVector3D("e2", Vec3(0.0, 0.0, 0.0), orthonormal.second, "orthonormal basis")
                    }) { Text("Save orthonormal basis") }
                } else Text("Gram–Schmidt needs two non-zero, non-parallel vectors.", color = VectorLabAmber, fontSize = 12.sp)
            }

            VectorResultPanel(operation, result, analysis.dot, analysis.cross, analysis.projectionAOnB, analysis.rejectionAFromB, alpha, beta)

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.addVector3D(namePrefix = "v", start = Vec3(0.0, 0.0, 0.0), end = Vec3(1.0, 1.0, 0.0), statusLabel = "vector") }) { Text("Add vector") }
                Button(
                    enabled = result != null,
                    onClick = {
                        result?.let { vm.addVector3D(namePrefix = "r", start = Vec3(0.0, 0.0, 0.0), end = it, statusLabel = "result vector") }
                    },
                ) { Text("Save result") }
                OutlinedButton(onClick = { vm.open(com.indianservers.aiexplorer.workspace.MathModule.Geometry3D) }) { Text("Open in 3D Geometry") }
                OutlinedButton(onClick = { addExampleVectors(vm) }) { Text("Add example pair") }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::undo, enabled = vm.canUndo, modifier = Modifier.weight(1f)) { Text("Undo") }
            OutlinedButton(onClick = vm::redo, enabled = vm.canRedo, modifier = Modifier.weight(1f)) { Text("Redo") }
            OutlinedButton(onClick = vm::saveWorkspace, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = vm::clearCurrentWorkspace, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
    }
}

@Composable
private fun EmptyVectorLab(onExamples: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(340.dp).background(VectorLabPanel, RoundedCornerShape(18.dp)).border(1.dp, VectorLabBorder, RoundedCornerShape(18.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No vectors yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Start with two editable vectors at the origin.", color = Color(0xFFABB8CC), modifier = Modifier.padding(vertical = 10.dp))
        Button(onClick = onExamples) { Text("Create example vectors") }
    }
}

@Composable
private fun VectorLabCanvas(a: Vec3, b: Vec3, result: Vec3?, operation: VectorLabOperation, isometric: Boolean, showGrid: Boolean) {
    Canvas(
        Modifier.fillMaxWidth().height(410.dp).background(Color(0xFF07101C), RoundedCornerShape(18.dp)).border(1.dp, VectorLabViolet.copy(.4f), RoundedCornerShape(18.dp)).semantics {
            contentDescription = "Vector visualization for ${operation.label}"
        },
    ) {
        val origin = Offset(size.width * .48f, size.height * .58f)
        val scale = size.minDimension / 10f
        fun project(v: Vec3): Offset = if (isometric) {
            Offset(origin.x + ((v.x - v.y) * scale * .72).toFloat(), origin.y + ((v.x + v.y) * scale * .26 - v.z * scale * .88).toFloat())
        } else Offset(origin.x + (v.x * scale).toFloat(), origin.y - (v.y * scale).toFloat())
        fun arrow(vector: Vec3, color: Color, label: String, startVector: Vec3 = Vec3(0.0, 0.0, 0.0)) {
            val start = project(startVector); val end = project(startVector + vector)
            drawLine(color.copy(.24f), start, end, 11f, StrokeCap.Round)
            drawLine(color, start, end, 5f, StrokeCap.Round)
            val angle = atan2(end.y - start.y, end.x - start.x)
            val wing = 16f
            drawLine(color, end, Offset(end.x + cos(angle + 2.55f) * wing, end.y + sin(angle + 2.55f) * wing), 5f, StrokeCap.Round)
            drawLine(color, end, Offset(end.x + cos(angle - 2.55f) * wing, end.y + sin(angle - 2.55f) * wing), 5f, StrokeCap.Round)
            drawContext.canvas.nativeCanvas.drawText(label, end.x + 9f, end.y - 10f, android.graphics.Paint().apply { this.color = android.graphics.Color.WHITE; textSize = 25f; isAntiAlias = true; isFakeBoldText = true })
        }
        if (showGrid) {
            (-5..5).forEach { tick ->
                if (isometric) {
                    drawLine(Color.White.copy(.10f), project(Vec3(tick.toDouble(), -5.0, 0.0)), project(Vec3(tick.toDouble(), 5.0, 0.0)), 1f)
                    drawLine(Color.White.copy(.10f), project(Vec3(-5.0, tick.toDouble(), 0.0)), project(Vec3(5.0, tick.toDouble(), 0.0)), 1f)
                } else {
                    drawLine(Color.White.copy(.10f), project(Vec3(tick.toDouble(), -5.0, 0.0)), project(Vec3(tick.toDouble(), 5.0, 0.0)), 1f)
                    drawLine(Color.White.copy(.10f), project(Vec3(-5.0, tick.toDouble(), 0.0)), project(Vec3(5.0, tick.toDouble(), 0.0)), 1f)
                }
            }
        }
        arrow(Vec3(4.5, 0.0, 0.0), Color(0xFFFF646F), "x")
        arrow(Vec3(0.0, 4.5, 0.0), VectorLabGreen, "y")
        if (isometric) arrow(Vec3(0.0, 0.0, 4.2), VectorLabBlue, "z")
        arrow(a, VectorLabCyan, "a ${vectorText(a)}")
        arrow(b, VectorLabBlue, "b ${vectorText(b)}")
        if (result != null) arrow(result, VectorLabViolet, "${operation.label} ${vectorText(result)}")
        if (operation == VectorLabOperation.Add) {
            drawLine(VectorLabBlue.copy(.55f), project(a), project(a + b), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            drawLine(VectorLabCyan.copy(.55f), project(b), project(a + b), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
        }
        drawCircle(Color.White, 6f, origin)
    }
}

@Composable
private fun VectorEditor(
    title: String,
    accent: Color,
    selectedIndex: Int,
    vector: Vector3D?,
    all: List<Vector3D>,
    modifier: Modifier,
    onSelect: (Int) -> Unit,
    onUpdate: (Int, Vector3D) -> Unit,
    onDelete: (Int) -> Unit,
) {
    Column(modifier.background(VectorLabPanel, RoundedCornerShape(14.dp)).border(1.dp, accent.copy(.45f), RoundedCornerShape(14.dp)).padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            all.forEachIndexed { index, item ->
                Text(item.name, color = if (index == selectedIndex) Color.White else Color(0xFFABB8CC), modifier = Modifier.background(if (index == selectedIndex) accent.copy(.25f) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onSelect(index) }.padding(horizontal = 9.dp, vertical = 7.dp))
            }
        }
        vector?.let { active ->
            Vector3Fields("Start", active.start) { onUpdate(selectedIndex, active.copy(start = it)) }
            Vector3Fields("Components", active.components) { components -> onUpdate(selectedIndex, active.copy(end = active.start + components)) }
            OutlinedTextField(active.name, { onUpdate(selectedIndex, active.copy(name = it.take(18))) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { onDelete(selectedIndex) }) { Text("Delete ${active.name}") }
        }
    }
}

@Composable
private fun Vector3Fields(label: String, value: Vec3, onChange: (Vec3) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFFABB8CC), fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("x" to value.x, "y" to value.y, "z" to value.z).forEach { (axis, current) ->
                OutlinedTextField(
                    formatCoordinate(current),
                    { text -> text.toDoubleOrNull()?.let { next -> onChange(when (axis) { "x" -> value.copy(x = next); "y" -> value.copy(y = next); else -> value.copy(z = next) }) } },
                    label = { Text(axis) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VectorResultPanel(operation: VectorLabOperation, result: Vec3?, dot: Double, cross: Vec3, projection: Vec3?, rejection: Vec3?, alpha: Double, beta: Double) {
    Column(Modifier.fillMaxWidth().background(VectorLabPanel, RoundedCornerShape(14.dp)).border(1.dp, VectorLabBorder, RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Result · ${operation.label}", color = VectorLabViolet, fontWeight = FontWeight.Bold)
        when (operation) {
            VectorLabOperation.Dot -> Text("a · b = ${formatCoordinate(dot)}", color = Color.White, fontSize = 20.sp)
            VectorLabOperation.Cross -> { Text("a × b = ${vectorText(cross)}", color = Color.White, fontSize = 20.sp); Text("The result is perpendicular to both input vectors.", color = Color(0xFFABB8CC)) }
            VectorLabOperation.Projection -> { Text("projᵦ a = ${projection?.let(::vectorText) ?: "undefined"}", color = Color.White, fontSize = 20.sp); Text("Rejection = ${rejection?.let(::vectorText) ?: "undefined"}", color = Color(0xFFABB8CC)) }
            VectorLabOperation.Scalar -> Text("${formatCoordinate(alpha)}a + ${formatCoordinate(beta)}b = ${result?.let(::vectorText) ?: "undefined"}", color = Color.White, fontSize = 20.sp)
            else -> Text("${operation.label} = ${result?.let(::vectorText) ?: "undefined"}", color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
private fun VectorMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(accent.copy(.08f), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.45f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun addExampleVectors(vm: ExplorerViewModel) {
    vm.addVector3D("a", Vec3(0.0, 0.0, 0.0), Vec3(2.0, 1.0, 1.0), "example vector")
    vm.addVector3D("b", Vec3(0.0, 0.0, 0.0), Vec3(-1.0, 2.0, 1.0), "example vector")
}

private fun vectorText(value: Vec3) = "⟨${formatCoordinate(value.x)}, ${formatCoordinate(value.y)}, ${formatCoordinate(value.z)}⟩"
