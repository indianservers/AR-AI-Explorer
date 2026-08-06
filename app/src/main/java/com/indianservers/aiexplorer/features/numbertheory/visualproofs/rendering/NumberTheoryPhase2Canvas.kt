package com.indianservers.aiexplorer.features.numbertheory.visualproofs.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal fun DrawScope.drawRemainderBuckets(state: NumberTheoryProofState.Ready) {
    when (state.topic.id) {
        "divisibility-11" -> drawAlternatingDigitBuckets(state)
        "euclid-primes" -> drawEuclidRemainders(state)
        "remainder-classes" -> drawRemainderClasses(state)
        else -> drawEqualGroups(state)
    }
}

private fun DrawScope.drawEqualGroups(state: NumberTheoryProofState.Ready) {
    val a = state.parameters["a"] ?: 24
    val b = state.parameters["b"] ?: 36
    val group = state.evidence.values["largest group"]?.toInt()?.coerceAtLeast(1) ?: 1
    val redGroups = (a / group).coerceAtMost(12)
    val blueGroups = (b / group).coerceAtMost(12)
    val slot = size.width * .78f / maxOf(redGroups, blueGroups).coerceAtLeast(1)
    repeat(redGroups) { index ->
        drawRoundRect(Coral.copy(.24f), Offset(size.width * .1f + index * slot, size.height * .2f), Size(slot * .72f, 62f))
        drawTextLabel(group.toString(), size.width * .1f + index * slot + slot * .36f, size.height * .29f, Coral, 22f)
    }
    repeat(blueGroups) { index ->
        drawRoundRect(Cyan.copy(.24f), Offset(size.width * .1f + index * slot, size.height * .55f), Size(slot * .72f, 62f))
        drawTextLabel(group.toString(), size.width * .1f + index * slot + slot * .36f, size.height * .64f, Cyan, 22f)
    }
    drawTextLabel("largest equal size = $group", size.width / 2, size.height * .88f, Green, 24f)
}

private fun DrawScope.drawAlternatingDigitBuckets(state: NumberTheoryProofState.Ready) {
    val digits = state.evidence.sequence
    val leftX = size.width * .28f
    val rightX = size.width * .72f
    drawRoundRect(Green.copy(.12f), Offset(size.width * .1f, size.height * .18f), Size(size.width * .36f, size.height * .55f), style = Stroke(3f))
    drawRoundRect(Coral.copy(.12f), Offset(size.width * .54f, size.height * .18f), Size(size.width * .36f, size.height * .55f), style = Stroke(3f))
    drawTextLabel("+", leftX, size.height * .14f, Green, 28f)
    drawTextLabel("-", rightX, size.height * .14f, Coral, 28f)
    digits.forEachIndexed { index, digit ->
        val x = if ((digits.lastIndex - index) % 2 == 0) leftX else rightX
        val row = index / 2
        drawCircle(if (x == leftX) Green.copy(.3f) else Coral.copy(.3f), 24f, Offset(x, size.height * .3f + row * 55f))
        drawTextLabel(digit.toString(), x, size.height * .32f + row * 55f, if (x == leftX) Green else Coral, 22f)
    }
    drawTextLabel("difference = ${state.evidence.values["difference"]}", size.width / 2, size.height * .88f, Amber, 25f)
}

private fun DrawScope.drawEuclidRemainders(state: NumberTheoryProofState.Ready) {
    val primes = state.evidence.sequence
    val constructed = state.evidence.values["product plus one"] ?: 1L
    val slot = size.width * .82f / primes.size.coerceAtLeast(1)
    primes.forEachIndexed { index, prime ->
        val x = size.width * .09f + index * slot + slot / 2
        drawCircle(Cyan.copy(.25f), 29f, Offset(x, size.height * .35f))
        drawTextLabel(prime.toString(), x, size.height * .37f, Cyan, 24f)
        drawLine(Muted, Offset(x, size.height * .46f), Offset(x, size.height * .64f), 2f)
        drawTextLabel("r 1", x, size.height * .72f, Green, 21f)
    }
    drawTextLabel("product + 1 = $constructed", size.width / 2, size.height * .16f, Amber, 25f)
    drawTextLabel("new prime factor: ${state.evidence.values["new prime factor"]}", size.width / 2, size.height * .9f, Green, 23f)
}

internal fun DrawScope.drawDivisorMap(state: NumberTheoryProofState.Ready) {
    if (state.topic.id == "perfect-numbers") {
        drawPerfectDivisors(state)
        return
    }
    val pairs = state.evidence.factorPairs
    val value = state.evidence.values["number"]?.toInt() ?: return
    val boundary = sqrt(value.toDouble())
    val center = Offset(size.width / 2, size.height / 2)
    val radiusX = size.width * .38f
    val radiusY = size.height * .31f
    drawCircle(Amber.copy(.08f), min(radiusX, radiusY), center)
    drawCircle(Amber.copy(.65f), min(radiusX, radiusY), center, style = Stroke(2f))
    pairs.take(12).forEachIndexed { index, pair ->
        val angle = 2 * PI * index / pairs.size.coerceAtLeast(1)
        val point = Offset(
            center.x + cos(angle).toFloat() * radiusX,
            center.y + sin(angle).toFloat() * radiusY,
        )
        drawLine(Muted.copy(.45f), center, point, 2f)
        drawCircle(if (pair.first <= boundary) Green else Coral, 24f, point)
        drawTextLabel("${pair.first}×${pair.second}", point.x, point.y + 7f, Stage, 18f)
    }
    drawTextLabel("√$value = ${"%.2f".format(boundary)}", center.x, center.y + 7f, Amber, 25f)
    drawTextLabel("one side of every pair reaches the green side", center.x, size.height * .91f, Green, 19f)
}
