package com.indianservers.aiexplorer.solver.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.indianservers.aiexplorer.Cyan
import com.indianservers.aiexplorer.GlowButton
import com.indianservers.aiexplorer.Green
import com.indianservers.aiexplorer.Ink
import com.indianservers.aiexplorer.Muted
import com.indianservers.aiexplorer.SurfaceA
import com.indianservers.aiexplorer.Violet
import com.indianservers.aiexplorer.solver.domain.visualisation.AreaRectangle
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualPoint
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualSeries
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationData
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationSpec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun SolverVisualisationPanel(
    specification: VisualisationSpec,
    position: Int,
    count: Int,
    playing: Boolean,
    reducedMotion: Boolean,
    expanded: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Violet.copy(alpha = .55f), RoundedCornerShape(6.dp))
            .background(SurfaceA.copy(alpha = .9f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(specification.title, color = Cyan, fontSize = 14.sp)
                Text("${position + 1} of $count | ${specification.type.name}", color = Muted, fontSize = 9.sp)
            }
            GlowButton("Full screen", onClick = onExpand)
        }
        SolverVisualisationCanvas(specification, Modifier.fillMaxWidth().height(210.dp))
        specification.domainStatement?.let { Text("Domain: $it", color = Muted, fontSize = 9.sp) }
        Text(specification.accessibilityDescription, color = Ink, fontSize = 10.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Previous", icon = "back", iconOnly = true, onClick = onPrevious)
            GlowButton(if (playing) "Pause" else if (reducedMotion) "Next state" else "Play", onClick = onPlayPause)
            GlowButton("Next", onClick = onNext)
            GlowButton("Reset", onClick = onReset)
        }
        if (reducedMotion) Text("Reduced motion is active; Play advances one state at a time.", color = Green, fontSize = 9.sp)
    }
    if (expanded) {
        Dialog(onDismissRequest = onExpand) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SurfaceA)
                    .padding(14.dp),
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(specification.title, color = Cyan, fontSize = 20.sp)
                        GlowButton("Close", onClick = onExpand)
                    }
                    SolverVisualisationCanvas(specification, Modifier.fillMaxWidth().weight(1f))
                    Text(specification.accessibilityDescription, color = Ink, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SolverVisualisationCanvas(specification: VisualisationSpec, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .background(Color(0xFF06131B), RoundedCornerShape(5.dp))
            .semantics { contentDescription = specification.accessibilityDescription },
    ) {
        drawRect(Color(0xFF06131B))
        when (val data = specification.mathematicalData) {
            is VisualisationData.NumberLine -> drawNumberLine(data)
            is VisualisationData.BalanceScale -> drawBalance(data)
            is VisualisationData.FractionArea -> drawFraction(data)
            is VisualisationData.PercentageBar -> drawPercentage(data)
            is VisualisationData.RatioTable -> drawTable(data.rows.size + 1, data.headings.size)
            is VisualisationData.AlgebraTiles -> drawTiles(data)
            is VisualisationData.TransformationHighlight -> drawTransformation()
            is VisualisationData.CoordinateGraph -> drawCoordinate(data)
            is VisualisationData.UnitCircle -> drawUnitCircle(data)
            is VisualisationData.Triangle -> drawTriangle(data.vertices)
            is VisualisationData.MatrixGrid -> drawMatrix(data)
            is VisualisationData.SequencePattern -> drawSequence(data)
            is VisualisationData.BarChart -> drawBarChart(data)
            is VisualisationData.ComplexPlane -> drawComplex(data.points)
            is VisualisationData.DerivativeTangent -> {
                drawSeries(data.curve, -4.0, 4.0, data.curve.points.minOf { it.y }, data.curve.points.maxOf { it.y }, Cyan)
                drawSeries(data.tangent, -4.0, 4.0, data.curve.points.minOf { it.y }, data.curve.points.maxOf { it.y }, Green)
                data.secants.forEach { drawSeries(it, -4.0, 4.0, data.curve.points.minOf { point -> point.y }, data.curve.points.maxOf { point -> point.y }, Violet.copy(alpha = .5f)) }
            }
            is VisualisationData.IntegralArea -> drawIntegral(data.curve, data.rectangles)
            is VisualisationData.VerificationComparison -> drawVerification(data)
        }
    }
}

private fun DrawScope.drawNumberLine(data: VisualisationData.NumberLine) {
    val y = size.height * .55f
    drawLine(Muted, Offset(size.width * .08f, y), Offset(size.width * .92f, y), 3f, cap = StrokeCap.Round)
    fun x(value: Double) = (size.width * (.08 + .84 * (value - data.minimum) / (data.maximum - data.minimum))).toFloat()
    val start = Offset(x(data.start), y)
    val end = Offset(x(data.end), y)
    drawCircle(Cyan, 8f, start)
    drawLine(Green, start, end, 7f, cap = StrokeCap.Round)
    drawCircle(Green, 10f, end)
    data.boundary?.let {
        drawCircle(if (data.boundaryClosed) Violet else Color(0xFF06131B), 12f, Offset(x(it), y))
        drawCircle(Violet, 12f, Offset(x(it), y), style = Stroke(4f))
    }
}

private fun DrawScope.drawBalance(data: VisualisationData.BalanceScale) {
    val centre = Offset(size.width / 2, size.height * .72f)
    val difference = ((data.leftWeight - data.rightWeight) * 10).toFloat().coerceIn(-18f, 18f)
    drawLine(Muted, centre, Offset(centre.x, size.height * .32f), 5f)
    val left = Offset(size.width * .2f, size.height * .4f + difference)
    val right = Offset(size.width * .8f, size.height * .4f - difference)
    drawLine(Cyan, left, right, 5f)
    listOf(left, right).forEach { point ->
        drawLine(Muted, point, Offset(point.x, point.y + 42f), 3f)
        drawRoundRect(Violet.copy(alpha = .22f), Offset(point.x - 62f, point.y + 38f), Size(124f, 45f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
    }
}

private fun DrawScope.drawFraction(data: VisualisationData.FractionArea) {
    fun area(left: Float, top: Float, width: Float, numerator: Int, denominator: Int, color: Color) {
        val safe = denominator.coerceAtLeast(1)
        val cell = width / safe
        repeat(safe) { index ->
            drawRect(if (index < numerator.coerceIn(0, safe)) color.copy(alpha = .55f) else Color.Transparent, Offset(left + index * cell, top), Size(cell, 70f))
            drawRect(Muted.copy(alpha = .7f), Offset(left + index * cell, top), Size(cell, 70f), style = Stroke(2f))
        }
    }
    area(size.width * .08f, size.height * .28f, size.width * .84f, data.numerator, data.denominator, Cyan)
    if (data.comparisonNumerator != null && data.comparisonDenominator != null) {
        area(size.width * .08f, size.height * .65f, size.width * .84f, data.comparisonNumerator, data.comparisonDenominator, Violet)
    }
}

private fun DrawScope.drawPercentage(data: VisualisationData.PercentageBar) {
    val bounds = Rect(size.width * .08f, size.height * .38f, size.width * .92f, size.height * .68f)
    drawRoundRect(Muted.copy(alpha = .12f), bounds.topLeft, bounds.size, androidx.compose.ui.geometry.CornerRadius(10f))
    drawRoundRect(Cyan.copy(alpha = .65f), bounds.topLeft, Size(bounds.width * (data.percentage / 100).toFloat().coerceIn(0f, 1f), bounds.height), androidx.compose.ui.geometry.CornerRadius(10f))
    drawRoundRect(Muted, bounds.topLeft, bounds.size, androidx.compose.ui.geometry.CornerRadius(10f), style = Stroke(2f))
}

private fun DrawScope.drawBarChart(data: VisualisationData.BarChart) {
    if (data.values.isEmpty()) return
    val left = size.width * .1f
    val right = size.width * .94f
    val top = size.height * .12f
    val bottom = size.height * .86f
    val maximum = data.values.max().coerceAtLeast(1.0)
    val slot = (right - left) / data.values.size
    drawLine(Muted, Offset(left, bottom), Offset(right, bottom), 3f)
    drawLine(Muted, Offset(left, top), Offset(left, bottom), 3f)
    data.values.forEachIndexed { index, value ->
        val barHeight = ((bottom - top) * (value / maximum)).toFloat()
        val barLeft = left + slot * index + slot * .18f
        drawRoundRect(
            if (index % 2 == 0) Cyan.copy(alpha = .75f) else Violet.copy(alpha = .72f),
            Offset(barLeft, bottom - barHeight),
            Size(slot * .64f, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f),
        )
    }
}

private fun DrawScope.drawTable(rows: Int, columns: Int) {
    val left = size.width * .12f
    val top = size.height * .18f
    val width = size.width * .76f
    val height = size.height * .66f
    repeat(rows + 1) { r ->
        val y = top + height * r / rows.coerceAtLeast(1)
        drawLine(if (r == 1) Cyan else Muted, Offset(left, y), Offset(left + width, y), if (r == 1) 4f else 2f)
    }
    repeat(columns + 1) { c ->
        val x = left + width * c / columns.coerceAtLeast(1)
        drawLine(Muted, Offset(x, top), Offset(x, top + height), 2f)
    }
}

private fun DrawScope.drawTiles(data: VisualisationData.AlgebraTiles) {
    val counts = listOf(data.positiveVariables to Cyan, data.negativeVariables to Color(0xFFFF6688))
    var x = size.width * .1f
    counts.forEach { (count, color) ->
        repeat(count.coerceAtMost(8)) {
            drawRoundRect(color.copy(alpha = .6f), Offset(x, size.height * .2f), Size(34f, size.height * .55f), androidx.compose.ui.geometry.CornerRadius(7f))
            x += 42f
        }
    }
    repeat((data.positiveUnits + data.negativeUnits).coerceAtMost(20)) { index ->
        val color = if (index < data.positiveUnits) Green else Color(0xFFFF6688)
        val row = index / 10
        val column = index % 10
        drawRect(color.copy(alpha = .65f), Offset(size.width * .1f + column * 24f, size.height * .8f + row * 24f), Size(18f, 18f))
    }
}

private fun DrawScope.drawTransformation() {
    val left = Rect(size.width * .07f, size.height * .25f, size.width * .43f, size.height * .75f)
    val right = Rect(size.width * .57f, size.height * .25f, size.width * .93f, size.height * .75f)
    drawRoundRect(Violet.copy(alpha = .18f), left.topLeft, left.size, androidx.compose.ui.geometry.CornerRadius(9f))
    drawRoundRect(Green.copy(alpha = .18f), right.topLeft, right.size, androidx.compose.ui.geometry.CornerRadius(9f))
    drawLine(Cyan, Offset(left.right + 10f, size.height / 2), Offset(right.left - 10f, size.height / 2), 5f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCoordinate(data: VisualisationData.CoordinateGraph) {
    drawAxes()
    data.series.forEachIndexed { index, series ->
        drawSeries(series, data.xMinimum, data.xMaximum, data.yMinimum, data.yMaximum, if (index == 0) Cyan else Violet)
    }
    data.markers.forEach { point ->
        drawCircle(Green, 8f, map(point, data.xMinimum, data.xMaximum, data.yMinimum, data.yMaximum))
    }
}

private fun DrawScope.drawUnitCircle(data: VisualisationData.UnitCircle) {
    val centre = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) * .34f
    drawCircle(Muted, radius, centre, style = Stroke(3f))
    drawLine(Muted.copy(alpha = .6f), Offset(centre.x - radius - 20, centre.y), Offset(centre.x + radius + 20, centre.y), 2f)
    drawLine(Muted.copy(alpha = .6f), Offset(centre.x, centre.y - radius - 20), Offset(centre.x, centre.y + radius + 20), 2f)
    val point = Offset(centre.x + radius * data.cosine.toFloat(), centre.y - radius * data.sine.toFloat())
    drawLine(Cyan, centre, point, 5f)
    drawLine(Green, point, Offset(point.x, centre.y), 3f)
    drawCircle(Violet, 9f, point)
}

private fun DrawScope.drawTriangle(vertices: List<VisualPoint>) {
    if (vertices.size != 3) return
    val mapped = vertices.map { map(it, vertices.minOf { p -> p.x }, vertices.maxOf { p -> p.x }, vertices.minOf { p -> p.y }, vertices.maxOf { p -> p.y }) }
    drawLine(Cyan, mapped[0], mapped[1], 5f)
    drawLine(Green, mapped[1], mapped[2], 5f)
    drawLine(Violet, mapped[2], mapped[0], 5f)
}

private fun DrawScope.drawMatrix(data: VisualisationData.MatrixGrid) {
    val rows = data.values.size.coerceAtLeast(1)
    val columns = data.values.firstOrNull()?.size?.coerceAtLeast(1) ?: 1
    val left = size.width * .18f
    val top = size.height * .14f
    val width = size.width * .64f
    val height = size.height * .72f
    repeat(rows) { row ->
        repeat(columns) { column ->
            val rect = Rect(left + width * column / columns, top + height * row / rows, left + width * (column + 1) / columns, top + height * (row + 1) / rows)
            if (row in data.highlightedRows || column in data.highlightedColumns) drawRect(Cyan.copy(alpha = .16f), rect.topLeft, rect.size)
            drawRect(Muted.copy(alpha = .65f), rect.topLeft, rect.size, style = Stroke(2f))
        }
    }
    drawLine(Violet, Offset(left - 18f, top), Offset(left - 18f, top + height), 5f)
    drawLine(Violet, Offset(left + width + 18f, top), Offset(left + width + 18f, top + height), 5f)
}

private fun DrawScope.drawSequence(data: VisualisationData.SequencePattern) {
    val all = data.terms + data.partialSums
    val minY = all.minOfOrNull { it.y } ?: 0.0
    val maxY = all.maxOfOrNull { it.y } ?: 1.0
    drawAxes()
    data.terms.forEach { drawCircle(Cyan, 7f, map(it, 0.0, (data.terms.size + 1).toDouble(), minY, maxY)) }
    data.partialSums.zipWithNext().forEach { (a, b) ->
        drawLine(Violet, map(a, 0.0, (data.terms.size + 1).toDouble(), minY, maxY), map(b, 0.0, (data.terms.size + 1).toDouble(), minY, maxY), 3f)
    }
}

private fun DrawScope.drawComplex(points: List<VisualPoint>) {
    val bound = max(2.0, points.maxOfOrNull { max(abs(it.x), abs(it.y)) } ?: 2.0)
    drawAxes()
    points.forEachIndexed { index, point ->
        val mapped = map(point, -bound, bound, -bound, bound)
        drawLine(if (index % 2 == 0) Cyan else Violet, Offset(size.width / 2, size.height / 2), mapped, 4f)
        drawCircle(Green, 8f, mapped)
    }
}

private fun DrawScope.drawIntegral(curve: VisualSeries, rectangles: List<AreaRectangle>) {
    val minX = min(curve.points.minOf { it.x }, rectangles.minOfOrNull { it.left } ?: 0.0)
    val maxX = max(curve.points.maxOf { it.x }, rectangles.maxOfOrNull { it.right } ?: 1.0)
    val minY = min(0.0, curve.points.minOf { it.y })
    val maxY = max(0.0, curve.points.maxOf { it.y })
    drawAxes()
    rectangles.forEach { rectangle ->
        val a = map(VisualPoint(rectangle.left, 0.0), minX, maxX, minY, maxY)
        val b = map(VisualPoint(rectangle.right, rectangle.height), minX, maxX, minY, maxY)
        drawRect(if (rectangle.height >= 0) Green.copy(alpha = .22f) else Color(0xFFFF6688).copy(alpha = .22f), Offset(min(a.x, b.x), min(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)))
    }
    drawSeries(curve, minX, maxX, minY, maxY, Cyan)
}

private fun DrawScope.drawVerification(data: VisualisationData.VerificationComparison) {
    val width = size.width * .72f
    data.checks.take(5).forEachIndexed { index, check ->
        val top = size.height * (.16f + index * .15f)
        drawRoundRect(Muted.copy(alpha = .12f), Offset(size.width * .14f, top), Size(width, 16f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawRoundRect((if (check.passed) Green else Color(0xFFFF6688)).copy(alpha = .75f), Offset(size.width * .14f, top), Size(width * if (check.passed) 1f else .45f, 16f), androidx.compose.ui.geometry.CornerRadius(8f))
    }
}

private fun DrawScope.drawAxes() {
    drawLine(Muted.copy(alpha = .5f), Offset(size.width * .06f, size.height / 2), Offset(size.width * .94f, size.height / 2), 2f)
    drawLine(Muted.copy(alpha = .5f), Offset(size.width / 2, size.height * .08f), Offset(size.width / 2, size.height * .92f), 2f)
}

private fun DrawScope.drawSeries(series: VisualSeries, minX: Double, maxX: Double, minY: Double, maxY: Double, color: Color) {
    if (series.points.size < 2) return
    val path = Path()
    var drawing = false
    series.points.forEach { point ->
        val mapped = map(point, minX, maxX, minY, maxY)
        if (!drawing) {
            path.moveTo(mapped.x, mapped.y)
            drawing = true
        } else {
            path.lineTo(mapped.x, mapped.y)
        }
    }
    drawPath(path, color, style = Stroke(4f, cap = StrokeCap.Round))
}

private fun DrawScope.map(point: VisualPoint, minX: Double, maxX: Double, minY: Double, maxY: Double): Offset {
    val dx = (maxX - minX).takeUnless { abs(it) < 1e-12 } ?: 1.0
    val dy = (maxY - minY).takeUnless { abs(it) < 1e-12 } ?: 1.0
    return Offset(
        (size.width * (.08 + .84 * (point.x - minX) / dx)).toFloat(),
        (size.height * (.92 - .84 * (point.y - minY) / dy)).toFloat(),
    )
}
