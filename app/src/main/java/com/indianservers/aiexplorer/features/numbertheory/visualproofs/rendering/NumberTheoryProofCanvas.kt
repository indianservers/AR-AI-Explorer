package com.indianservers.aiexplorer.features.numbertheory.visualproofs.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal val Stage = Color(0xFF030914)
internal val Cyan = Color(0xFF2ADBF2)
internal val Green = Color(0xFF4DE3A7)
internal val Amber = Color(0xFFFFBD47)
internal val Violet = Color(0xFF9A80FF)
internal val Coral = Color(0xFFFF765E)
internal val Muted = Color(0xFF8FA2BE)

@Composable
internal fun NumberTheoryProofCanvas(
    state: NumberTheoryProofState.Ready,
    onTapIndex: (Int) -> Unit = {},
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(Stage, RoundedCornerShape(8.dp))
            .pointerInput(state.topic.id) {
                detectTapGestures { point -> onTapIndex((point.x / size.width * 20).toInt()) }
            }
            .semantics { contentDescription = state.evidence.accessibilityDescription },
    ) {
        drawGrid()
        when (state.topic.visualModel) {
            NumberTheoryVisualModel.Staircase -> drawStaircase(state)
            NumberTheoryVisualModel.TileGrid -> drawTileGrid(state)
            NumberTheoryVisualModel.BarStaircase -> drawBars(state)
            NumberTheoryVisualModel.PlaceValueBlocks -> drawPlaceValues(state)
            NumberTheoryVisualModel.FactorRectangle -> drawFactorRectangles(state)
            NumberTheoryVisualModel.CycleTrack -> drawCycleTrack(state)
            NumberTheoryVisualModel.EuclideanRectangle -> drawEuclidean(state)
            NumberTheoryVisualModel.PrimeFactorTree -> drawPrimeFactors(state)
            NumberTheoryVisualModel.SieveGrid -> drawSieve(state)
            NumberTheoryVisualModel.ModularClock -> {
                if (state.topic.id in setOf("modular-addition", "modular-multiplication", "negative-modulo")) {
                    drawPhase3ModularClock(state)
                } else {
                    drawModularClock(state)
                }
            }
            NumberTheoryVisualModel.RemainderBuckets -> drawRemainderBuckets(state)
            NumberTheoryVisualModel.DivisorMap -> drawDivisorMap(state)
            NumberTheoryVisualModel.ExponentChain -> drawExponentChain(state)
            else -> drawNumberLine(state)
        }
    }
}

private fun DrawScope.drawGrid() {
    repeat(9) { index ->
        val x = size.width * index / 8f
        val y = size.height * index / 8f
        drawLine(Muted.copy(alpha = .08f), Offset(x, 0f), Offset(x, size.height), 1f)
        drawLine(Muted.copy(alpha = .08f), Offset(0f, y), Offset(size.width, y), 1f)
    }
}

private fun DrawScope.drawStaircase(state: NumberTheoryProofState.Ready) {
    val n = state.parameters["n"] ?: 5
    val cell = min((size.width * .42f) / (n + 1), (size.height * .72f) / n).coerceAtLeast(5f)
    val left = size.width * .08f
    val bottom = size.height * .86f
    if (state.topic.id == "even-sum") {
        val evenCell = min((size.width * .78f) / (2 * n), (size.height * .7f) / n).coerceAtLeast(4f)
        repeat(n) { row ->
            repeat(2 * (row + 1)) { column ->
                val color = if (column < row + 1) Cyan else Green
                drawCircle(color, evenCell * .25f, Offset(left + column * evenCell, bottom - row * evenCell))
            }
        }
        if (state.stepIndex >= 2) {
            drawRect(
                Amber.copy(alpha = .14f),
                Offset(left - evenCell / 2, bottom - (n - 1) * evenCell - evenCell / 2),
                Size((n + 1) * evenCell, n * evenCell),
                style = Stroke(3f),
            )
        }
        return
    }
    repeat(n) { row ->
        repeat(row + 1) { column ->
            drawCircle(if (row == n - 1) Amber else Cyan, cell * .28f, Offset(left + column * cell, bottom - row * cell))
        }
    }
    if (state.stepIndex >= 1) {
        val right = size.width * .56f
        repeat(n) { row ->
            repeat(n - row) { column ->
                drawCircle(if (row == 0) Green else Violet, cell * .28f, Offset(right + column * cell, bottom - row * cell))
            }
        }
    }
    if (state.stepIndex >= 3) {
        drawRect(Green.copy(alpha = .12f), Offset(size.width * .53f, bottom - (n - 1) * cell - cell / 2), Size((n + 1) * cell, n * cell), style = Stroke(3f))
    }
}

private fun DrawScope.drawTileGrid(state: NumberTheoryProofState.Ready) {
    val n = state.parameters["n"] ?: 5
    val side = min(size.width * .72f, size.height * .78f)
    val cell = side / (n + 1)
    val origin = Offset((size.width - n * cell) / 2, (size.height - n * cell) / 2)
    repeat(n) { row ->
        repeat(n) { column ->
            val layer = maxOf(row, column)
            val color = listOf(Cyan, Green, Violet, Amber, Coral)[layer % 5]
            drawRect(color.copy(alpha = .72f), origin + Offset(column * cell, row * cell), Size(cell - 2f, cell - 2f))
            if (layer == n - 1) drawRect(Amber, origin + Offset(column * cell, row * cell), Size(cell - 2f, cell - 2f), style = Stroke(2f))
        }
    }
}

private fun DrawScope.drawBars(state: NumberTheoryProofState.Ready) {
    val values = state.evidence.sequence
    val minimum = minOf(0L, values.minOrNull() ?: 0L)
    val maximum = maxOf(1L, values.maxOrNull() ?: 1L)
    val range = (maximum - minimum).toFloat().coerceAtLeast(1f)
    val baseline = size.height * (.18f + .64f * maximum / range)
    val slot = size.width * .82f / values.size.coerceAtLeast(1)
    values.forEachIndexed { index, value ->
        val height = kotlin.math.abs(value.toFloat()) / range * size.height * .62f
        val top = if (value >= 0) baseline - height else baseline
        drawRect(if (index % 2 == 0) Cyan else Violet, Offset(size.width * .09f + index * slot, top), Size(slot * .72f, height))
    }
    drawLine(Amber.copy(alpha = .7f), Offset(size.width * .06f, baseline), Offset(size.width * .94f, baseline), 2f)
    if (state.stepIndex >= 1) {
        values.reversed().forEachIndexed { index, value ->
            val height = kotlin.math.abs(value.toFloat()) / range * size.height * .62f
            val top = if (value >= 0) baseline - height else baseline
            drawRect(Green.copy(alpha = .42f), Offset(size.width * .09f + index * slot, top), Size(slot * .72f, height), style = Stroke(3f))
        }
    }
}

private fun DrawScope.drawPlaceValues(state: NumberTheoryProofState.Ready) {
    val digits = state.evidence.sequence
    val slot = size.width / (digits.size + 1)
    digits.forEachIndexed { index, digit ->
        val x = slot * (index + 1)
        drawRoundRect(if (index % 2 == 0) Cyan.copy(.2f) else Violet.copy(.2f), Offset(x - 34f, size.height * .18f), Size(68f, 74f))
        drawTextLabel(digit.toString(), x, size.height * .31f, if (index % 2 == 0) Cyan else Violet, 38f)
        repeat(digit.toInt().coerceAtMost(9)) { unit ->
            drawCircle(if (unit / 3 * 3 + 2 < digit) Green else Amber, 7f, Offset(x - 25f + (unit % 3) * 25f, size.height * .58f + (unit / 3) * 22f))
        }
    }
    val summary = when {
        state.evidence.values.containsKey("digit sum") -> "digit sum = ${state.evidence.values["digit sum"]}"
        state.evidence.values.containsKey("retained tail") -> "retained tail = ${state.evidence.values["retained tail"]}"
        state.evidence.values.containsKey("units digit") -> "units digit = ${state.evidence.values["units digit"]}"
        else -> state.evidence.labels["formula"].orEmpty()
    }
    drawTextLabel(summary, size.width / 2, size.height * .9f, Amber, 25f)
}

private fun DrawScope.drawFactorRectangles(state: NumberTheoryProofState.Ready) {
    val pairs = state.evidence.factorPairs.take(4)
    val panel = size.width / pairs.size.coerceAtLeast(1)
    pairs.forEachIndexed { index, pair ->
        val maxSide = maxOf(pair.first, pair.second).toFloat()
        val cell = min(panel * .72f / pair.second, size.height * .55f / pair.first).coerceAtLeast(2f)
        val origin = Offset(index * panel + panel * .14f, size.height * .2f)
        repeat(pair.first) { row ->
            repeat(pair.second) { column ->
                drawRect(if ((row + column) % 2 == 0) Cyan else Violet, origin + Offset(column * cell, row * cell), Size(cell - 1f, cell - 1f))
            }
        }
        drawTextLabel("${pair.first}×${pair.second}", index * panel + panel / 2, size.height * .88f, Green, 22f)
    }
}

private fun DrawScope.drawCycleTrack(state: NumberTheoryProofState.Ready) {
    val a = state.parameters["a"] ?: 4
    val b = state.parameters["b"] ?: 6
    val lcm = state.evidence.values["first alignment"]?.toInt() ?: 12
    val limit = min(lcm * 2, 60)
    val left = size.width * .08f
    val width = size.width * .84f
    repeat(limit + 1) { value ->
        val x = left + width * value / limit
        if (value % a == 0) drawCircle(if (value == lcm) Amber else Cyan, 8f, Offset(x, size.height * .38f))
        if (value % b == 0) drawRect(if (value == lcm) Amber else Violet, Offset(x - 7f, size.height * .62f - 7f), Size(14f, 14f))
    }
    drawLine(Muted, Offset(left, size.height * .38f), Offset(left + width, size.height * .38f), 2f)
    drawLine(Muted, Offset(left, size.height * .62f), Offset(left + width, size.height * .62f), 2f)
    drawTextLabel("first alignment = $lcm", size.width / 2, size.height * .86f, Amber, 25f)
}

private fun DrawScope.drawEuclidean(state: NumberTheoryProofState.Ready) {
    val steps = state.evidence.euclideanSteps
    if (steps.isEmpty()) return
    val first = steps.first()
    val scale = min(size.width * .78f / first.dividend, size.height * .55f / first.divisor)
    val origin = Offset(size.width * .1f, size.height * .18f)
    drawRect(Cyan.copy(alpha = .13f), origin, Size(first.dividend * scale, first.divisor * scale), style = Stroke(4f))
    var x = origin.x
    repeat(first.quotient) {
        drawRect(Violet.copy(alpha = .26f), Offset(x, origin.y), Size(first.divisor * scale, first.divisor * scale), style = Stroke(3f))
        x += first.divisor * scale
    }
    if (first.remainder > 0) drawRect(Amber.copy(alpha = .35f), Offset(x, origin.y), Size(first.remainder * scale, first.divisor * scale), style = Stroke(3f))
    steps.take(state.stepIndex + 1).forEachIndexed { index, step ->
        drawTextLabel("${step.dividend}=${step.quotient}×${step.divisor}+${step.remainder}", size.width / 2, size.height * .76f + index * 24f, if (step.remainder == 0) Green else Amber, 19f)
    }
}

private fun DrawScope.drawPrimeFactors(state: NumberTheoryProofState.Ready) {
    val factors = state.evidence.primeFactors.entries.flatMap { entry -> List(entry.value) { entry.key } }
    val center = Offset(size.width / 2, size.height * .18f)
    drawCircle(Cyan.copy(.25f), 34f, center)
    val root = state.evidence.values["number"]
        ?: state.evidence.values["each product"]
        ?: state.evidence.values["product plus one"]
        ?: 1L
    drawTextLabel(root.toString(), center.x, center.y + 8f, Cyan, 28f)
    factors.forEachIndexed { index, prime ->
        val x = size.width * (index + 1) / (factors.size + 1)
        val target = Offset(x, size.height * .7f)
        drawLine(Muted, center, target, 2f)
        drawCircle(listOf(Green, Amber, Violet, Coral)[prime % 4], 28f, target)
        drawTextLabel(prime.toString(), x, target.y + 7f, Stage, 24f)
    }
}

private fun DrawScope.drawSieve(state: NumberTheoryProofState.Ready) {
    val limit = state.parameters["limit"] ?: 50
    val columns = 10
    val rows = (limit + columns - 1) / columns
    val cellW = size.width * .9f / columns
    val cellH = size.height * .82f / rows
    val left = size.width * .05f
    repeat(limit) { index ->
        val value = index + 1
        val row = index / columns
        val column = index % columns
        val center = Offset(left + column * cellW + cellW / 2, size.height * .08f + row * cellH + cellH / 2)
        val prime = value in state.evidence.primes
        val crossed = value in state.evidence.crossed
        if (prime) drawCircle(Green.copy(.3f), min(cellW, cellH) * .38f, center)
        drawTextLabel(value.toString(), center.x, center.y + 6f, if (prime) Green else Muted, min(cellW, cellH) * .45f)
        if (crossed && state.stepIndex >= 1) {
            drawLine(Coral, center - Offset(8f, 8f), center + Offset(8f, 8f), 2f)
            drawLine(Coral, center + Offset(-8f, 8f), center + Offset(8f, -8f), 2f)
        }
    }
}

private fun DrawScope.drawModularClock(state: NumberTheoryProofState.Ready) {
    val modulus = state.parameters["modulus"] ?: 12
    val remainder = state.evidence.values["remainder"]?.toInt() ?: 0
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) * .34f
    drawCircle(Cyan.copy(.15f), radius, center)
    drawCircle(Cyan, radius, center, style = Stroke(3f))
    repeat(modulus) { value ->
        val angle = -PI / 2 + 2 * PI * value / modulus
        val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
        drawCircle(if (value == remainder) Amber else Violet, if (value == remainder) 15f else 9f, point)
        drawTextLabel(value.toString(), point.x, point.y - 17f, if (value == remainder) Amber else Muted, 18f)
    }
    val angle = -PI / 2 + 2 * PI * remainder / modulus
    val end = Offset(center.x + cos(angle).toFloat() * radius * .76f, center.y + sin(angle).toFloat() * radius * .76f)
    drawLine(Green, center, end, 5f)
}

private fun DrawScope.drawNumberLine(state: NumberTheoryProofState.Ready) {
    val y = size.height / 2
    drawLine(Cyan, Offset(size.width * .08f, y), Offset(size.width * .92f, y), 3f)
    val points = if (state.evidence.primes.isNotEmpty()) state.evidence.primes.map(Int::toLong) else state.evidence.sequence
    val visible = points.takeLast(24)
    val minimum = minOf(0L, visible.minOrNull() ?: 0L)
    val maximum = (visible.maxOrNull() ?: 1L).coerceAtLeast(minimum + 1)
    visible.forEachIndexed { index, value ->
        val x = size.width * (.08f + .84f * (value - minimum) / (maximum - minimum).toFloat())
        val twin = state.topic.id == "twin-primes" && state.evidence.sequence.contains(value)
        drawCircle(if (twin) Amber else if (index % 2 == 0) Green else Violet, if (twin) 13f else 9f, Offset(x, y))
        drawTextLabel(value.toString(), x, y - 20f, Muted, 18f)
    }
    drawTextLabel(state.evidence.labels["reasoningStatus"].orEmpty(), size.width / 2, size.height * .82f, Amber, 20f)
}

internal fun DrawScope.drawTextLabel(text: String, x: Float, y: Float, color: Color, size: Float) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y,
        android.graphics.Paint().apply {
            this.color = color.toArgb()
            textSize = size
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        },
    )
}

private fun Color.toArgb(): Int =
    (alpha * 255).toInt().shl(24) or (red * 255).toInt().shl(16) or (green * 255).toInt().shl(8) or (blue * 255).toInt()
