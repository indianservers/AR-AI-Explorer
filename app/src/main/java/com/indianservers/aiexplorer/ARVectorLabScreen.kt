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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val VectorBg = Color(0xFF03070D)
private val VectorPanel = Color(0xEE07101A)
private val VectorPanelSoft = Color(0xDD0A1320)
private val VectorBorder = Color(0xFF1E2A3C)
private val VectorText = Color(0xFFF4F7FF)
private val VectorMuted = Color(0xFFAAB4C5)
private val VectorPurple = Color(0xFF8A52FF)
private val VectorBlue = Color(0xFF40A6FF)
private val VectorYellow = Color(0xFFFFC43D)
private val VectorGreen = Color(0xFF42E48B)
private val VectorRed = Color(0xFFFF5A66)

private data class ArVector(val x: Float, val y: Float, val z: Float) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
    operator fun plus(other: ArVector) = ArVector(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: ArVector) = ArVector(x - other.x, y - other.y, z - other.z)
    fun dot(other: ArVector) = x * other.x + y * other.y + z * other.z
    fun cross(other: ArVector) = ArVector(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
    fun scale(k: Float) = ArVector(x * k, y * k, z * k)
}

private enum class VectorOperation(val label: String, val formula: String) {
    Addition("Addition", "a + b"),
    Subtraction("Subtraction", "a - b"),
    Dot("Dot Product", "a . b"),
    Cross("Cross Product", "a x b"),
    Scalar("Scalar Mult.", "k a"),
}

@Composable
fun ARVectorLabScreen(onBack: () -> Unit) {
    var operation by remember { mutableStateOf(VectorOperation.Addition) }
    var vectorA by remember { mutableStateOf(ArVector(2f, 1f, 1f)) }
    var vectorB by remember { mutableStateOf(ArVector(-1f, 2f, 1f)) }
    var scalar by remember { mutableStateOf(2f) }
    var showGrid by remember { mutableStateOf(true) }
    var showAxes by remember { mutableStateOf(true) }
    var showOrigin by remember { mutableStateOf(true) }
    var arLive by remember { mutableStateOf(false) }
    var arStatus by remember { mutableStateOf("AR Preview") }
    val result = remember(operation, vectorA, vectorB, scalar) {
        when (operation) {
            VectorOperation.Addition -> vectorA + vectorB
            VectorOperation.Subtraction -> vectorA - vectorB
            VectorOperation.Dot -> ArVector(vectorA.dot(vectorB), 0f, 0f)
            VectorOperation.Cross -> vectorA.cross(vectorB)
            VectorOperation.Scalar -> vectorA.scale(scalar)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(VectorBg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VectorHeader(onBack, arLive, arStatus)
        VectorArStage(
            vectorA = vectorA,
            vectorB = vectorB,
            result = result,
            operation = operation,
            showGrid = showGrid,
            showAxes = showAxes,
            showOrigin = showOrigin,
            onGrid = { showGrid = !showGrid },
            onAxes = { showAxes = !showAxes },
            onOrigin = { showOrigin = !showOrigin },
            onReset = {
                vectorA = ArVector(2f, 1f, 1f)
                vectorB = ArVector(-1f, 2f, 1f)
                scalar = 2f
            },
            onArStateChanged = { live, status ->
                arLive = live
                arStatus = status
            },
        )
        VectorOperationTabs(operation, onSelect = { operation = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VectorControlCard("Vector a", VectorYellow, vectorA, Modifier.weight(1f)) { vectorA = it }
            VectorControlCard("Vector b", VectorBlue, vectorB, Modifier.weight(1f)) { vectorB = it }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (operation == VectorOperation.Scalar) {
                ScalarControlCard(scalar, Modifier.weight(1f)) { scalar = it }
            }
            VectorControlCard("Result ${operation.formula}", VectorPurple, result, Modifier.weight(1f), readOnly = true) {}
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VectorMagnitudeCard(vectorA, vectorB, result, operation, Modifier.weight(1f))
            VectorExplanationCard(operation, vectorA, vectorB, result, scalar, Modifier.weight(1f))
        }
        VectorActionRow()
        VectorBottomNav()
    }
}

@Composable
private fun VectorHeader(onBack: () -> Unit, arLive: Boolean, arStatus: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "< Back",
            color = VectorPurple,
            fontSize = 16.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AR Vector Lab", color = VectorText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Visualize vectors and perform operations in AR", color = VectorMuted, fontSize = 13.sp)
        }
        Row(
            Modifier
                .background(VectorGreen.copy(.12f), RoundedCornerShape(18.dp))
                .border(1.dp, VectorGreen.copy(.35f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(8.dp).background(if (arLive) VectorGreen else VectorYellow, CircleShape))
            Text(if (arLive) "AR Active" else arStatus.take(18), color = if (arLive) VectorGreen else VectorYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun VectorArStage(
    vectorA: ArVector,
    vectorB: ArVector,
    result: ArVector,
    operation: VectorOperation,
    showGrid: Boolean,
    showAxes: Boolean,
    showOrigin: Boolean,
    onGrid: () -> Unit,
    onAxes: () -> Unit,
    onOrigin: () -> Unit,
    onReset: () -> Unit,
    onArStateChanged: (Boolean, String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.36f)
            .background(Brush.verticalGradient(listOf(Color(0xFF211915), Color(0xFF121923), Color(0xFF090B10))), RoundedCornerShape(16.dp))
            .border(1.dp, VectorBorder, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "AR vector viewport with axes, grid and editable vectors" },
    ) {
        SharedARLabCamera(
            modifier = Modifier.fillMaxSize(),
            labId = "ar-vector-lab",
            needsDepth = true,
            onStateChanged = onArStateChanged,
        )
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            fun project(v: ArVector): Offset {
                val scale = size.minDimension / 8.2f
                val origin = Offset(size.width * .47f, size.height * .58f)
                return Offset(
                    origin.x + (v.x - v.y) * scale * .72f,
                    origin.y + (v.x + v.y) * scale * .26f - v.z * scale * .88f,
                )
            }
            fun label(text: String, at: Offset, color: Color, textSize: Float = 30f) {
                drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y, android.graphics.Paint().apply {
                    this.color = android.graphics.Color.argb((color.alpha * 255).roundToInt(), (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt())
                    this.textSize = textSize
                    isAntiAlias = true
                    isFakeBoldText = true
                })
            }
            fun arrow(start: Offset, end: Offset, color: Color, stroke: Float = 5f) {
                drawLine(color.copy(.30f), start, end, stroke * 2.5f, StrokeCap.Round)
                drawLine(color, start, end, stroke, StrokeCap.Round)
                val angle = atan2(end.y - start.y, end.x - start.x)
                val wing = 18f
                val a1 = angle + 2.55f
                val a2 = angle - 2.55f
                drawLine(color, end, Offset(end.x + cos(a1) * wing, end.y + sin(a1) * wing), stroke, StrokeCap.Round)
                drawLine(color, end, Offset(end.x + cos(a2) * wing, end.y + sin(a2) * wing), stroke, StrokeCap.Round)
            }
            val zero = project(ArVector(0f, 0f, 0f))
            if (showGrid) {
                (-4..4).forEach { tick ->
                    drawLine(Color.White.copy(.12f), project(ArVector(tick.toFloat(), -4f, 0f)), project(ArVector(tick.toFloat(), 4f, 0f)), 1.2f)
                    drawLine(Color.White.copy(.12f), project(ArVector(-4f, tick.toFloat(), 0f)), project(ArVector(4f, tick.toFloat(), 0f)), 1.2f)
                }
            }
            if (showAxes) {
                arrow(project(ArVector(0f, 0f, 0f)), project(ArVector(3.8f, 0f, 0f)), VectorRed, 4f)
                arrow(project(ArVector(0f, 0f, 0f)), project(ArVector(0f, 3.8f, 0f)), VectorGreen, 4f)
                arrow(project(ArVector(0f, 0f, -3f)), project(ArVector(0f, 0f, 3.6f)), VectorBlue, 4f)
                label("x", project(ArVector(3.95f, 0f, 0f)) + Offset(4f, 12f), VectorText, 28f)
                label("y", project(ArVector(0f, 3.95f, 0f)) + Offset(4f, 12f), VectorText, 28f)
                label("z", project(ArVector(0f, 0f, 3.75f)) + Offset(-10f, -8f), VectorText, 28f)
            }
            if (showOrigin) drawCircle(Color.White, 7f, zero)
            val aEnd = project(vectorA)
            val bEnd = project(vectorB)
            val rEnd = project(result)
            arrow(zero, aEnd, VectorYellow, 5f)
            arrow(zero, bEnd, VectorBlue, 5f)
            if (operation != VectorOperation.Dot) arrow(zero, rEnd, VectorPurple, 5f)
            drawLine(VectorYellow.copy(.7f), aEnd, project(ArVector(vectorA.x, vectorA.y, 0f)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            drawLine(VectorBlue.copy(.7f), bEnd, project(ArVector(vectorB.x, vectorB.y, 0f)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            if (operation != VectorOperation.Dot) {
                drawLine(VectorPurple.copy(.55f), rEnd, project(ArVector(result.x, result.y, 0f)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            }
            label("a (${trim(vectorA.x)}, ${trim(vectorA.y)}, ${trim(vectorA.z)})", aEnd + Offset(8f, -16f), VectorYellow, 27f)
            label("b (${trim(vectorB.x)}, ${trim(vectorB.y)}, ${trim(vectorB.z)})", bEnd + Offset(8f, -16f), VectorBlue, 27f)
            label("${operation.formula} ${resultLabel(operation, result, vectorA, vectorB)}", rEnd + Offset(8f, -16f), VectorPurple, 27f)
        }
        VectorStageButton("Calibrate Plane", Modifier.align(Alignment.TopStart).padding(14.dp))
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).background(Color(0xCC0A101A), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.12f), RoundedCornerShape(14.dp)),
        ) {
            VectorToggle("Axes", showAxes, onAxes)
            VectorToggle("Grid", showGrid, onGrid)
            VectorToggle("Origin", showOrigin, onOrigin)
            VectorToggle("Reset", false, onReset)
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(14.dp).background(Color(0xCC0A101A), RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Tips", color = VectorYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Drag the arrows\nto adjust vectors", color = VectorText, fontSize = 12.sp, lineHeight = 16.sp)
        }
        VectorStageButton("3D View", Modifier.align(Alignment.BottomEnd).padding(14.dp), VectorPurple)
    }
}

@Composable
private fun VectorStageButton(label: String, modifier: Modifier, accent: Color = VectorText) {
    Row(
        modifier.background(Color(0xCC0A101A), RoundedCornerShape(14.dp)).border(1.dp, accent.copy(.24f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VectorToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick).background(if (selected) VectorPurple.copy(.18f) else Color.Transparent).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (selected) "*" else "+", color = if (selected) VectorPurple else VectorText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(label, color = VectorText, fontSize = 11.sp)
    }
}

@Composable
private fun VectorOperationTabs(selected: VectorOperation, onSelect: (VectorOperation) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(VectorPanel, RoundedCornerShape(14.dp)).border(1.dp, VectorBorder, RoundedCornerShape(14.dp)),
    ) {
        VectorOperation.entries.forEach { operation ->
            Column(
                Modifier.width(154.dp).clickable { onSelect(operation) }.background(if (operation == selected) VectorPurple.copy(.16f) else Color.Transparent).border(1.dp, if (operation == selected) VectorPurple else Color.Transparent).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(operation.label, color = if (operation == selected) VectorText else VectorMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(operation.formula, color = if (operation == selected) VectorPurple else VectorMuted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun VectorControlCard(title: String, accent: Color, value: ArVector, modifier: Modifier, readOnly: Boolean = false, onChange: (ArVector) -> Unit) {
    Column(vectorPanel(modifier), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComponentInput("x", value.x, readOnly, Modifier.weight(1f)) { onChange(value.copy(x = it)) }
            ComponentInput("y", value.y, readOnly, Modifier.weight(1f)) { onChange(value.copy(y = it)) }
            ComponentInput("z", value.z, readOnly, Modifier.weight(1f)) { onChange(value.copy(z = it)) }
        }
        Slider(
            value = value.magnitude.coerceIn(0f, 6f),
            onValueChange = { magnitude ->
                if (!readOnly) {
                    val current = value.magnitude.takeIf { it > .01f } ?: 1f
                    onChange(value.copy(x = value.x / current * magnitude, y = value.y / current * magnitude, z = value.z / current * magnitude))
                }
            },
            valueRange = 0f..6f,
            enabled = !readOnly,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = VectorBorder),
        )
    }
}

@Composable
private fun ComponentInput(label: String, value: Float, readOnly: Boolean, modifier: Modifier, onChange: (Float) -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = VectorMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = trim(value),
            onValueChange = { text -> text.toFloatOrNull()?.let(onChange) },
            enabled = !readOnly,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = VectorText, fontSize = 14.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VectorPurple,
                unfocusedBorderColor = VectorBorder,
                disabledBorderColor = VectorBorder,
                focusedContainerColor = Color(0xFF050A11),
                unfocusedContainerColor = Color(0xFF050A11),
                disabledContainerColor = Color(0xFF050A11),
                disabledTextColor = VectorText,
                cursorColor = VectorPurple,
            ),
        )
    }
}

@Composable
private fun ScalarControlCard(value: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    Column(vectorPanel(modifier), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Scalar k", color = VectorGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(trim(value), color = VectorText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Slider(value, onChange, valueRange = -4f..4f, colors = SliderDefaults.colors(thumbColor = VectorGreen, activeTrackColor = VectorGreen, inactiveTrackColor = VectorBorder))
    }
}

@Composable
private fun VectorMagnitudeCard(a: ArVector, b: ArVector, result: ArVector, operation: VectorOperation, modifier: Modifier) {
    Column(vectorPanel(modifier), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Vector Magnitudes", color = VectorText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        MagnitudeLine("a", a, VectorYellow)
        MagnitudeLine("b", b, VectorBlue)
        if (operation == VectorOperation.Dot) {
            Text("a . b = ${trim(a.dot(b))}", color = VectorPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
            MagnitudeLine(operation.formula, result, VectorPurple)
        }
    }
}

@Composable
private fun MagnitudeLine(label: String, value: ArVector, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(4.dp).height(28.dp).background(color, RoundedCornerShape(3.dp)))
        Text("|$label| = sqrt(${trim(value.x)}^2 + ${trim(value.y)}^2 + ${trim(value.z)}^2) = ${trim(value.magnitude)}", color = VectorText, fontSize = 13.sp)
    }
}

@Composable
private fun VectorExplanationCard(operation: VectorOperation, a: ArVector, b: ArVector, result: ArVector, scalar: Float, modifier: Modifier) {
    val explanation = when (operation) {
        VectorOperation.Addition -> "The resultant a + b is the diagonal of the parallelogram formed by a and b placed tail to tail."
        VectorOperation.Subtraction -> "The vector a - b points from the head of b to the head of a when both vectors share the same origin."
        VectorOperation.Dot -> "The dot product is ${trim(a.dot(b))}. Positive values mean the vectors point mostly in the same direction."
        VectorOperation.Cross -> "The cross product creates a perpendicular vector with direction given by the right-hand rule."
        VectorOperation.Scalar -> "The scalar k = ${trim(scalar)} stretches vector a while preserving direction when k is positive."
    }
    Column(vectorPanel(modifier), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Explanation", color = VectorText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(explanation, color = VectorMuted, fontSize = 13.sp, lineHeight = 17.sp)
        Canvas(Modifier.fillMaxWidth().height(82.dp)) {
            val base = Offset(size.width * .16f, size.height * .78f)
            val aEnd = Offset(size.width * .45f, size.height * .52f)
            val bEnd = Offset(size.width * .36f, size.height * .16f)
            val rEnd = Offset(size.width * .72f, size.height * .18f)
            drawLine(VectorYellow, base, aEnd, 4f, StrokeCap.Round)
            drawLine(VectorBlue, base, bEnd, 4f, StrokeCap.Round)
            drawLine(VectorPurple, base, rEnd, 4f, StrokeCap.Round)
            drawLine(VectorMuted.copy(.45f), aEnd, rEnd, 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)))
            drawLine(VectorMuted.copy(.45f), bEnd, rEnd, 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)))
        }
    }
}

@Composable
private fun VectorActionRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VectorActionButton("Save Workspace", Modifier.weight(1f))
        VectorActionButton("Capture Screenshot", Modifier.weight(1.25f), primary = true)
        VectorActionButton("Share", Modifier.weight(1f))
    }
}

@Composable
private fun VectorActionButton(label: String, modifier: Modifier, primary: Boolean = false) {
    Button(
        onClick = {},
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) VectorPurple else VectorPanelSoft, contentColor = VectorText),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, VectorBorder),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun VectorBottomNav() {
    Row(
        Modifier.fillMaxWidth().background(VectorPanel, RoundedCornerShape(18.dp)).border(1.dp, VectorBorder, RoundedCornerShape(18.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("3D Graph", "3D Shapes", "AR Labs", "2D Graph", "More").forEach { item ->
            Text(
                item,
                color = if (item == "AR Labs") VectorPurple else VectorMuted,
                fontSize = 12.sp,
                fontWeight = if (item == "AR Labs") FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.background(if (item == "AR Labs") VectorPurple.copy(.12f) else Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 13.dp, vertical = 10.dp),
            )
        }
    }
}

private fun vectorPanel(modifier: Modifier) = modifier
    .background(VectorPanel, RoundedCornerShape(12.dp))
    .border(1.dp, VectorBorder, RoundedCornerShape(12.dp))
    .padding(12.dp)

private fun resultLabel(operation: VectorOperation, result: ArVector, a: ArVector, b: ArVector): String = when (operation) {
    VectorOperation.Dot -> "= ${trim(a.dot(b))}"
    else -> "(${trim(result.x)}, ${trim(result.y)}, ${trim(result.z)})"
}

private fun trim(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}
