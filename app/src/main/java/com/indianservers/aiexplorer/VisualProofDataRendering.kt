package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.indianservers.aiexplorer.core.ProofFrame
import com.indianservers.aiexplorer.core.trim
import kotlin.math.round

internal fun DrawScope.drawAnscombeProof(frame: ProofFrame, width: Float, height: Float) {
    val series = frame.parameters.getValue("series").toInt().coerceIn(1, 4)
    val rows = frame.lab.dataSet?.rows.orEmpty().filter { it[0].toInt() == series }
    val left = width * .1f
    val right = width * .9f
    val top = height * .14f
    val bottom = height * .82f

    fun dataPoint(x: Double, y: Double) = Offset(
        left + ((x - 3.0) / 17.0).toFloat() * (right - left),
        bottom - ((y - 2.0) / 12.0).toFloat() * (bottom - top),
    )
    fun value(number: Double): String = trim(round(number * 100.0) / 100.0)
    fun label(text: String, at: Offset, color: Color, textSize: Float) {
        drawContext.canvas.nativeCanvas.drawText(
            text,
            at.x,
            at.y,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.rgb(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
                this.textSize = textSize
                isFakeBoldText = true
            },
        )
    }

    drawLine(Ink, Offset(left, bottom), Offset(right, bottom), 3f)
    drawLine(Ink, Offset(left, top), Offset(left, bottom), 3f)
    listOf(5, 10, 15).forEach { tick ->
        val x = dataPoint(tick.toDouble(), 2.0).x
        drawLine(Grid, Offset(x, top), Offset(x, bottom), 1.5f)
        label(tick.toString(), Offset(x - 8f, bottom + 25f), Muted, 17f)
    }
    listOf(4, 8, 12).forEach { tick ->
        val y = dataPoint(3.0, tick.toDouble()).y
        drawLine(Grid, Offset(left, y), Offset(right, y), 1.5f)
        label(tick.toString(), Offset(left - 28f, y + 6f), Muted, 17f)
    }

    val slope = frame.measurements.getValue("slope")
    val intercept = frame.measurements.getValue("intercept")
    drawLine(
        Violet,
        dataPoint(3.0, intercept + slope * 3.0),
        dataPoint(20.0, intercept + slope * 20.0),
        4f,
    )
    rows.forEachIndexed { index, row ->
        val point = dataPoint(row[1], row[2])
        drawCircle(
            color = listOf(Cyan, Green, Amber, Violet)[series - 1],
            radius = if (frame.step >= 3) 9f else 7f,
            center = point,
        )
        if (frame.step == 0) label((index + 1).toString(), point + Offset(8f, -8f), Muted, 13f)
    }

    label("Published series $series · 11 observations", Offset(width * .48f, height * .1f), Cyan, 21f)
    label("y = ${value(intercept)} + ${value(slope)}x", Offset(width * .58f, height * .2f), Violet, 20f)
    label("R² ${value(frame.measurements.getValue("R²"))}", Offset(width * .7f, height * .28f), Green, 19f)
    label("same summaries, different shape", Offset(width * .46f, height * .92f), Green, 22f)
}
