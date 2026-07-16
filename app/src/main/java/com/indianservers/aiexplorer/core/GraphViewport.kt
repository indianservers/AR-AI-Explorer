package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Pure viewport maths shared by the graph canvas and its regression tests. */
object GraphViewport {
    const val minimumZoom = .01f
    const val maximumZoom = 100f

    fun zoom(current: Float, gestureFactor: Float): Float =
        (current * gestureFactor).coerceIn(minimumZoom, maximumZoom)

    fun axisStep(minimum: Double, maximum: Double, targetTicks: Int = 12): Double {
        require(minimum.isFinite() && maximum.isFinite() && maximum > minimum && targetTicks in 4..30)
        val rough = (maximum - minimum) / targetTicks
        val magnitude = 10.0.pow(floor(log10(rough)))
        val normalized = rough / magnitude
        val nice = when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    fun ticks(minimum: Double, maximum: Double, targetTicks: Int = 12): List<Double> {
        val step = axisStep(minimum, maximum, targetTicks)
        val first = ceil(minimum / step).toLong()
        val last = floor(maximum / step).toLong()
        if (last < first) return emptyList()
        val count = (last - first + 1).coerceAtMost(64).toInt()
        return List(count) { index ->
            val value = (first + index) * step
            if (abs(value) < step * 1e-10) 0.0 else value
        }
    }
}

object GraphEquationNames {
    fun at(index: Int): String {
        require(index >= 0)
        return if (index <= 'z'.code - 'f'.code) "${('f'.code + index).toChar()}(x)" else "f${index + 1}(x)"
    }

    fun next(existing: Set<String>): String = generateSequence(0) { it + 1 }.map(::at).first { it !in existing }
}
