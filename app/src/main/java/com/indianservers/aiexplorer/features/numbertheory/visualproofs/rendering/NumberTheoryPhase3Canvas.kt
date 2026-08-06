package com.indianservers.aiexplorer.features.numbertheory.visualproofs.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryMath
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun DrawScope.drawPhase3ModularClock(state: NumberTheoryProofState.Ready) {
    val modulus = state.parameters["modulus"] ?: 7
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) * .34f
    drawCircle(Cyan.copy(.12f), radius, center)
    drawCircle(Cyan, radius, center, style = Stroke(3f))
    val clockPoints = (0 until modulus).map { value ->
        val angle = -PI / 2 + 2 * PI * value / modulus
        Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
    }
    clockPoints.forEachIndexed { value, point ->
        drawCircle(Violet.copy(.65f), 9f, point)
        drawTextLabel(value.toString(), point.x, point.y - 16f, Muted, 18f)
    }

    val route = state.evidence.sequence.ifEmpty {
        listOf(0L, state.evidence.values["remainder"] ?: 0L)
    }.take(18)
    val path = Path()
    route.forEachIndexed { index, raw ->
        val position = NumberTheoryMath.normalizedMod(raw.toInt(), modulus)
        val point = clockPoints[position]
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        drawCircle(
            color = if (index == route.lastIndex) Amber else Green,
            radius = if (index == route.lastIndex) 14f else 8f,
            center = point,
        )
        if (state.topic.id == "modular-multiplication") {
            drawTextLabel(index.toString(), point.x + 13f, point.y + 18f, Green, 15f)
        }
    }
    drawPath(path, Green.copy(.75f), style = Stroke(4f))
    val result = state.evidence.values["remainder"]
    drawTextLabel("endpoint = $result", center.x, size.height * .91f, Amber, 25f)
}

internal fun DrawScope.drawRemainderClasses(state: NumberTheoryProofState.Ready) {
    val modulus = state.parameters["modulus"] ?: 4
    val selected = state.evidence.values["class label"]?.toInt() ?: 0
    val centerValue = state.parameters["value"] ?: 0
    val gap = 5f
    val bucketWidth = (size.width * .9f - gap * (modulus - 1)) / modulus
    val left = size.width * .05f
    repeat(modulus) { remainder ->
        val x = left + remainder * (bucketWidth + gap)
        drawRoundRect(
            color = if (remainder == selected) Amber.copy(.18f) else Cyan.copy(.08f),
            topLeft = Offset(x, size.height * .14f),
            size = Size(bucketWidth, size.height * .68f),
            style = Stroke(if (remainder == selected) 4f else 2f),
        )
        drawTextLabel(remainder.toString(), x + bucketWidth / 2, size.height * .11f, if (remainder == selected) Amber else Cyan, 22f)
        val members = (centerValue - 2 * modulus..centerValue + 2 * modulus)
            .filter { NumberTheoryMath.normalizedMod(it, modulus) == remainder }
            .take(5)
        members.forEachIndexed { index, value ->
            drawCircle(
                if (value == centerValue) Green.copy(.5f) else Violet.copy(.25f),
                min(bucketWidth * .3f, 20f),
                Offset(x + bucketWidth / 2, size.height * (.26f + index * .12f)),
            )
            drawTextLabel(value.toString(), x + bucketWidth / 2, size.height * (.28f + index * .12f), ProofTextColor(remainder == selected), 17f)
        }
    }
    drawTextLabel("$centerValue belongs to class $selected", size.width / 2, size.height * .93f, Amber, 22f)
}

internal fun DrawScope.drawExponentChain(state: NumberTheoryProofState.Ready) {
    val base = state.parameters["base"] ?: 2
    val m = state.parameters["m"] ?: state.evidence.values["cancelled factors"]?.toInt() ?: 0
    val n = state.parameters["n"] ?: state.evidence.values["denominator factors"]?.toInt() ?: 0
    val firstCount = m.coerceIn(0, 9)
    val secondCount = n.coerceIn(0, 9)
    val slot = size.width * .82f / maxOf(firstCount, secondCount, 1)
    val left = size.width * .09f

    drawTextLabel("base $base", size.width / 2, size.height * .12f, Cyan, 25f)
    repeat(firstCount) { index ->
        val cancelled = state.topic.id in setOf("exponent-quotient", "zero-exponent") && index < secondCount && state.stepIndex >= 2
        drawFactorBlock(left + index * slot, size.height * .25f, slot, base, Cyan, cancelled)
    }
    repeat(secondCount) { index ->
        val cancelled = state.topic.id in setOf("exponent-quotient", "zero-exponent") && index < firstCount && state.stepIndex >= 2
        drawFactorBlock(left + index * slot, size.height * .58f, slot, base, if (state.topic.id == "power-of-power") Green else Violet, cancelled)
    }

    if (state.topic.id == "negative-exponent") {
        drawLine(Amber, Offset(size.width * .12f, size.height * .49f), Offset(size.width * .88f, size.height * .49f), 4f)
        drawTextLabel("1", size.width * .07f, size.height * .39f, Amber, 25f)
    } else {
        drawTextLabel(if (secondCount > 0) "second chain / group" else "all factors cancel", size.width / 2, size.height * .53f, Muted, 18f)
    }
    drawTextLabel(state.evidence.labels["formula"].orEmpty(), size.width / 2, size.height * .91f, Green, 23f)
}

private fun DrawScope.drawFactorBlock(
    x: Float,
    y: Float,
    slot: Float,
    base: Int,
    color: androidx.compose.ui.graphics.Color,
    cancelled: Boolean,
) {
    val width = (slot * .72f).coerceIn(24f, 62f)
    drawRoundRect(color.copy(.25f), Offset(x, y), Size(width, 54f))
    drawRoundRect(color, Offset(x, y), Size(width, 54f), style = Stroke(2f))
    drawTextLabel(base.toString(), x + width / 2, y + 35f, color, 23f)
    if (cancelled) {
        drawLine(Coral, Offset(x - 3f, y + 50f), Offset(x + width + 3f, y + 4f), 4f)
    }
}

internal fun DrawScope.drawPerfectDivisors(state: NumberTheoryProofState.Ready) {
    val value = state.parameters["value"] ?: return
    val divisors = state.evidence.sequence
    val center = Offset(size.width / 2, size.height * .44f)
    val radiusX = size.width * .39f
    val radiusY = size.height * .28f
    drawCircle(Amber.copy(.2f), 39f, center)
    drawTextLabel(value.toString(), center.x, center.y + 9f, Amber, 29f)
    divisors.take(18).forEachIndexed { index, divisor ->
        val angle = 2 * PI * index / divisors.size.coerceAtLeast(1)
        val point = Offset(
            center.x + cos(angle).toFloat() * radiusX,
            center.y + sin(angle).toFloat() * radiusY,
        )
        drawLine(Muted.copy(.35f), center, point, 2f)
        drawCircle(if (state.evidence.values["proper divisor sum"] == value.toLong()) Green else Violet, 23f, point)
        drawTextLabel(divisor.toString(), point.x, point.y + 7f, Stage, 18f)
    }
    drawTextLabel(
        "proper divisor sum = ${state.evidence.values["proper divisor sum"]}",
        center.x,
        size.height * .91f,
        if (state.evidence.labels["prediction"] == "perfect") Green else Amber,
        23f,
    )
}

private fun ProofTextColor(selected: Boolean) = if (selected) Amber else Muted
