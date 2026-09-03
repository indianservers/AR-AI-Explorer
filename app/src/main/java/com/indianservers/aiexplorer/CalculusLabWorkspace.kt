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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.CalculusLabEngine
import com.indianservers.aiexplorer.core.AreaBetweenLabResult
import com.indianservers.aiexplorer.core.DerivativeLabResult
import com.indianservers.aiexplorer.core.IntegralLabResult
import com.indianservers.aiexplorer.core.LimitClassification
import com.indianservers.aiexplorer.core.LimitLabResult
import com.indianservers.aiexplorer.core.LinearizationLabResult
import com.indianservers.aiexplorer.core.RiemannSample
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.MathModule
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val CalculusCyan get() = themedColor(Color(0xFF31D7FF), Cyan)
private val CalculusViolet get() = themedColor(Color(0xFFA878FF), Violet)
private val CalculusGreen get() = themedColor(Color(0xFF55E6A5), Green)
private val CalculusAmber get() = themedColor(Color(0xFFFFC857), Amber)

private enum class CalculusMode(val label: String) { Derivative("Derivative"), Integral("Integral"), AreaBetween("Area Between"), Limit("Limit"), Applications("Applications") }

@Composable
internal fun CalculusLabWorkspace(vm: ExplorerViewModel) {
    val engine = remember { CalculusLabEngine() }
    var expression by rememberLabText(vm, MathModule.CalculusLab, "expression", "x^3 - 3*x")
    var secondExpression by rememberLabText(vm, MathModule.CalculusLab, "secondExpression", "x")
    var mode by remember { mutableStateOf(CalculusMode.Derivative) }
    var point by rememberLabText(vm, MathModule.CalculusLab, "point", "1")
    var target by rememberLabText(vm, MathModule.CalculusLab, "target", "1.1")
    var lower by rememberLabText(vm, MathModule.CalculusLab, "lower", "-2")
    var upper by rememberLabText(vm, MathModule.CalculusLab, "upper", "2")
    var rectangles by rememberLabText(vm, MathModule.CalculusLab, "rectangles", "12")
    var riemannSample by remember { mutableStateOf(RiemannSample.Midpoint) }

    val from = lower.toDoubleOrNull()
    val to = upper.toDoubleOrNull()
    val at = point.toDoubleOrNull()
    val derivative = remember(expression, at, from, to, mode) {
        if (mode == CalculusMode.Derivative && at != null && from != null && to != null) runCatching { engine.derivative(expression, at, from..to) } else null
    }
    val linearization = remember(expression, at, target, mode) {
        val targetX = target.toDoubleOrNull()
        if (mode == CalculusMode.Derivative && at != null && targetX != null) runCatching { engine.linearization(expression, at, targetX) } else null
    }
    val integral = remember(expression, from, to, rectangles, riemannSample, mode) {
        if (mode == CalculusMode.Integral && from != null && to != null) runCatching { engine.integral(expression, from, to, rectangles.toIntOrNull() ?: 12, riemannSample) } else null
    }
    val areaBetween = remember(expression, secondExpression, from, to, mode) {
        if (mode == CalculusMode.AreaBetween && from != null && to != null) runCatching { engine.areaBetween(expression, secondExpression, from, to) } else null
    }
    val limit = remember(expression, at, mode) {
        if (mode == CalculusMode.Limit && at != null) runCatching { engine.limit(expression, at) } else null
    }
    val applications = remember(expression, from, to, mode) {
        if (mode == CalculusMode.Applications && from != null && to != null) runCatching { engine.applications(expression, from, to) } else null
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Calculus Lab", color = CalculusViolet, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Connect exact calculus, numerical evidence and interactive graphs.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        OutlinedTextField(expression, { expression = it }, label = { Text("f(x)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalculusMode.entries.forEach { choice -> Button(onClick = { mode = choice }, enabled = mode != choice) { Text(choice.label) } }
        }
        when (mode) {
            CalculusMode.Derivative -> DerivativePanel(derivative, linearization, point, { point = it }, target, { target = it }, lower, { lower = it }, upper, { upper = it })
            CalculusMode.Integral -> IntegralPanel(integral, lower, { lower = it }, upper, { upper = it }, rectangles, { rectangles = it }, riemannSample, { riemannSample = it })
            CalculusMode.AreaBetween -> AreaBetweenPanel(areaBetween, secondExpression, { secondExpression = it }, lower, { lower = it }, upper, { upper = it })
            CalculusMode.Limit -> LimitPanel(limit, point, { point = it })
            CalculusMode.Applications -> ApplicationsPanel(applications, lower, { lower = it }, upper, { upper = it })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { expression = "sin(x)/x"; point = "0"; mode = CalculusMode.Limit; vm.reportStatus("Classic limit loaded") }) { Text("sin(x)/x") }
            Button(onClick = { expression = "x^2"; lower = "0"; upper = "2"; mode = CalculusMode.Integral; vm.reportStatus("Area example loaded") }) { Text("Area x²") }
            Button(onClick = { expression = "x^3 - 3*x"; point = "1"; lower = "-2"; upper = "2"; mode = CalculusMode.Applications; vm.reportStatus("Extrema example loaded") }) { Text("Extrema") }
            Button(onClick = { vm.addFunction(expression); vm.open(MathModule.Graph2D) }) { Text("Open in Graph") }
        }
    }
}

@Composable
private fun DerivativePanel(
    result: Result<DerivativeLabResult>?, linearization: Result<LinearizationLabResult>?, point: String, onPoint: (String) -> Unit,
    target: String, onTarget: (String) -> Unit,
    lower: String, onLower: (String) -> Unit, upper: String, onUpper: (String) -> Unit,
) {
    CalculusInputs(point, onPoint, "Centre x", lower, onLower, upper, onUpper)
    CalcField(target, onTarget, "Linearize to x", Modifier.fillMaxWidth())
    val value = result?.getOrNull()
    if (value == null) return CalculusError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalculusMetric("f(x)", calcNumber(value.numerical.point.y), CalculusCyan, Modifier.weight(1f))
        CalculusMetric("f′(x)", calcNumber(value.numerical.derivative), CalculusGreen, Modifier.weight(1f))
        CalculusMetric("f″(x)", calcNumber(value.numerical.secondDerivative), CalculusAmber, Modifier.weight(1f))
    }
    Text("Exact: ${value.symbolic.exact}", color = CalculusViolet, fontWeight = FontWeight.Bold)
    Text("${value.numerical.classification} · tangent ${value.numerical.tangent}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    linearization?.getOrNull()?.let { approximation ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalculusMetric("L(${calcNumber(approximation.targetX)})", calcNumber(approximation.predictedY), CalculusCyan, Modifier.weight(1f))
            CalculusMetric("Actual", calcNumber(approximation.actualY), CalculusGreen, Modifier.weight(1f))
            CalculusMetric("Approx. error", calcNumber(approximation.absoluteError), CalculusAmber, Modifier.weight(1f))
        }
        Text(approximation.equation, color = CalculusViolet, fontFamily = FontFamily.Monospace)
    }
    CalculusCanvas(value.curve, derivative = value)
    CasSteps(value.symbolic.steps.map { "${it.title}: ${it.expression} — ${it.explanation}" })
}

@Composable
private fun IntegralPanel(
    result: Result<IntegralLabResult>?, lower: String, onLower: (String) -> Unit, upper: String, onUpper: (String) -> Unit,
    rectangles: String, onRectangles: (String) -> Unit, sample: RiemannSample, onSample: (RiemannSample) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcField(lower, onLower, "Lower", Modifier.weight(1f)); CalcField(upper, onUpper, "Upper", Modifier.weight(1f)); CalcField(rectangles, onRectangles, "Rectangles", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RiemannSample.entries.forEach { choice -> Button(onClick = { onSample(choice) }, enabled = choice != sample) { Text(choice.name) } }
    }
    val value = result?.getOrNull()
    if (value == null) return CalculusError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalculusMetric("Accurate ∫", calcNumber(value.numerical.signedArea), CalculusGreen, Modifier.weight(1f))
        CalculusMetric("Riemann sum", calcNumber(value.riemannEstimate), CalculusCyan, Modifier.weight(1f))
        CalculusMetric("Error", calcNumber(abs(value.riemannEstimate - value.numerical.signedArea)), CalculusAmber, Modifier.weight(1f))
    }
    Text("Antiderivative: ${value.symbolic.exact}", color = CalculusViolet, fontWeight = FontWeight.Bold)
    Text("Geometric area ${calcNumber(value.numerical.geometricArea)} · numerical error ≤ ${"%.2e".format(Locale.US, value.numerical.errorEstimate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    CalculusCanvas(value.curve, integral = value)
    CasSteps(value.symbolic.steps.map { "${it.title}: ${it.expression} — ${it.explanation}" })
}

@Composable
private fun AreaBetweenPanel(
    result: Result<AreaBetweenLabResult>?, second: String, onSecond: (String) -> Unit,
    lower: String, onLower: (String) -> Unit, upper: String, onUpper: (String) -> Unit,
) {
    CalcField(second, onSecond, "g(x)", Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcField(lower, onLower, "From", Modifier.weight(1f)); CalcField(upper, onUpper, "To", Modifier.weight(1f))
    }
    val value = result?.getOrNull()
    if (value == null) return CalculusError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalculusMetric("Geometric area", calcNumber(value.numerical.geometricArea), CalculusGreen, Modifier.weight(1f))
        CalculusMetric("Signed ∫(f−g)", calcNumber(value.numerical.signedArea), CalculusCyan, Modifier.weight(1f))
        CalculusMetric("Error ≤", "%.2e".format(Locale.US, value.numerical.errorEstimate), CalculusAmber, Modifier.weight(1f))
    }
    AreaBetweenCanvas(value)
    Text("The geometric area integrates |f(x) − g(x)|, so crossings are handled automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LimitPanel(result: Result<LimitLabResult>?, point: String, onPoint: (String) -> Unit) {
    CalcField(point, onPoint, "x approaches", Modifier.fillMaxWidth())
    val value = result?.getOrNull()
    if (value == null) return CalculusError(result)
    val report = value.rigorous
    val answer = when (report.classification) {
        LimitClassification.Finite -> report.value?.let(::calcNumber) ?: "—"
        LimitClassification.PositiveInfinity -> "+∞"
        LimitClassification.NegativeInfinity -> "−∞"
        LimitClassification.DoesNotExist -> "DNE"
        LimitClassification.Unresolved -> "Unresolved"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalculusMetric("Left", report.left.estimate?.let(::calcNumber) ?: report.left.classification.name, CalculusCyan, Modifier.weight(1f))
        CalculusMetric("Limit", answer, CalculusGreen, Modifier.weight(1f))
        CalculusMetric("Right", report.right.estimate?.let(::calcNumber) ?: report.right.classification.name, CalculusViolet, Modifier.weight(1f))
    }
    Text("Continuity: ${report.continuity.name.lowercase().replace('_', ' ')} · method: ${report.method.name}", color = CalculusAmber)
    Text("Exact CAS: ${value.symbolic.exact}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    CasSteps(report.steps + report.verification)
}

@Composable
private fun ApplicationsPanel(
    result: Result<com.indianservers.aiexplorer.core.DerivativeApplicationReport>?,
    lower: String, onLower: (String) -> Unit, upper: String, onUpper: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcField(lower, onLower, "From", Modifier.weight(1f)); CalcField(upper, onUpper, "To", Modifier.weight(1f))
    }
    val value = result?.getOrNull()
    if (value == null) return CalculusError(result)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalculusMetric("Absolute min", value.absoluteMinimum?.let { "(${calcNumber(it.x)}, ${calcNumber(it.y)})" } ?: "—", CalculusGreen, Modifier.weight(1f))
        CalculusMetric("Absolute max", value.absoluteMaximum?.let { "(${calcNumber(it.x)}, ${calcNumber(it.y)})" } ?: "—", CalculusAmber, Modifier.weight(1f))
    }
    Text("STATIONARY POINTS", color = CalculusViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    if (value.stationaryPoints.isEmpty()) Text("No classified stationary points detected in this interval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    value.stationaryPoints.forEach { Text("${it.kind}: (${calcNumber(it.point.x)}, ${calcNumber(it.point.y)})", color = MaterialTheme.colorScheme.onSurface) }
    Text("Increasing: ${value.increasing.joinToString { "[${calcNumber(it.from)}, ${calcNumber(it.to)}]" }.ifBlank { "none" }}", color = CalculusGreen)
    Text("Decreasing: ${value.decreasing.joinToString { "[${calcNumber(it.from)}, ${calcNumber(it.to)}]" }.ifBlank { "none" }}", color = CalculusAmber)
}

@Composable
private fun CalculusInputs(point: String, onPoint: (String) -> Unit, pointLabel: String, lower: String, onLower: (String) -> Unit, upper: String, onUpper: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcField(point, onPoint, pointLabel, Modifier.weight(1f)); CalcField(lower, onLower, "Graph min", Modifier.weight(1f)); CalcField(upper, onUpper, "Graph max", Modifier.weight(1f))
    }
}

@Composable
private fun CalcField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier) =
    OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true, modifier = modifier)

@Composable
private fun CalculusMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.border(1.dp, accent.copy(.5f), RoundedCornerShape(13.dp)).background(accent.copy(.08f), RoundedCornerShape(13.dp)).padding(10.dp)) {
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun CasSteps(steps: List<String>) {
    Text("REASONING & VERIFICATION", color = CalculusViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    steps.forEachIndexed { index, step -> Text("${index + 1}. $step", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
}

@Composable
private fun CalculusError(result: Result<*>?) {
    Text(result?.exceptionOrNull()?.message ?: "Enter valid values to analyse.", color = MaterialTheme.colorScheme.error)
}

@Composable
private fun CalculusCanvas(curve: List<Vec2>, derivative: DerivativeLabResult? = null, integral: IntegralLabResult? = null) {
    Canvas(Modifier.fillMaxWidth().height(280.dp).border(1.dp, CalculusViolet.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        if (curve.size < 2) return@Canvas
        val minX = curve.minOf { it.x }; val maxX = curve.maxOf { it.x }
        val extraY = buildList {
            derivative?.let { add(it.numerical.point.y) }
            integral?.rectangles?.forEach { add(it.height) }
            add(0.0)
        }
        val rawMinY = (curve.map { it.y } + extraY).minOrNull() ?: -1.0
        val rawMaxY = (curve.map { it.y } + extraY).maxOrNull() ?: 1.0
        val centreY = (rawMinY + rawMaxY) / 2
        val spanY = max(1.0, rawMaxY - rawMinY)
        val minY = centreY - spanY * .6; val maxY = centreY + spanY * .6
        fun map(point: Vec2) = Offset(
            ((point.x - minX) / (maxX - minX) * size.width).toFloat(),
            (size.height - (point.y - minY) / (maxY - minY) * size.height).toFloat(),
        )
        if (0.0 in minX..maxX) drawLine(Color.Gray.copy(.45f), map(Vec2(0.0, minY)), map(Vec2(0.0, maxY)), 1.5f)
        if (0.0 in minY..maxY) drawLine(Color.Gray.copy(.45f), map(Vec2(minX, 0.0)), map(Vec2(maxX, 0.0)), 1.5f)
        integral?.rectangles?.forEach { rectangle ->
            val first = map(Vec2(rectangle.from, 0.0)); val second = map(Vec2(rectangle.to, rectangle.height))
            drawRect(if (rectangle.height >= 0) CalculusGreen.copy(.2f) else CalculusAmber.copy(.2f), Offset(first.x, minOf(first.y, second.y)), androidx.compose.ui.geometry.Size(second.x - first.x, abs(second.y - first.y)))
            drawRect(CalculusGreen.copy(.65f), Offset(first.x, minOf(first.y, second.y)), androidx.compose.ui.geometry.Size(second.x - first.x, abs(second.y - first.y)), style = Stroke(1.5f))
        }
        val path = Path()
        curve.forEachIndexed { index, point -> val mapped = map(point); if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y) }
        drawPath(path, CalculusCyan, style = Stroke(4f))
        derivative?.let { result ->
            val p = result.numerical.point; val slope = result.numerical.derivative
            val left = Vec2(minX, p.y + slope * (minX - p.x)); val right = Vec2(maxX, p.y + slope * (maxX - p.x))
            drawLine(CalculusAmber, map(left), map(right), 3f)
            drawCircle(CalculusGreen, 7f, map(p))
        }
    }
}

@Composable
private fun AreaBetweenCanvas(result: AreaBetweenLabResult) {
    Canvas(Modifier.fillMaxWidth().height(280.dp).border(1.dp, CalculusViolet.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        val all = result.firstCurve + result.secondCurve
        if (result.firstCurve.size < 2 || result.secondCurve.size < 2) return@Canvas
        val minX = all.minOf { it.x }; val maxX = all.maxOf { it.x }
        val rawMinY = minOf(0.0, all.minOf { it.y }); val rawMaxY = maxOf(0.0, all.maxOf { it.y })
        val paddingY = max(1e-9, rawMaxY - rawMinY) * .08
        val minY = rawMinY - paddingY; val maxY = rawMaxY + paddingY
        fun map(point: Vec2) = Offset(
            ((point.x - minX) / max(1e-12, maxX - minX) * size.width).toFloat(),
            (size.height - (point.y - minY) / max(1e-12, maxY - minY) * size.height).toFloat(),
        )
        result.firstCurve.zip(result.secondCurve).forEachIndexed { index, (first, second) ->
            if (index % 3 == 0) drawLine(CalculusViolet.copy(.18f), map(first), map(second), 5f)
        }
        fun path(points: List<Vec2>) = Path().apply { points.forEachIndexed { index, point -> val p = map(point); if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        drawPath(path(result.firstCurve), CalculusCyan, style = Stroke(4f))
        drawPath(path(result.secondCurve), CalculusAmber, style = Stroke(4f))
    }
}

private fun calcNumber(value: Double): String = when {
    !value.isFinite() -> "—"
    abs(value - value.toLong()) < 1e-8 -> value.toLong().toString()
    else -> "%.5f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}
