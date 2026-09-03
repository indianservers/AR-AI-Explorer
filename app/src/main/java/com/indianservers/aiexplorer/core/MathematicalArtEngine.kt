package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

data class ArtCurve(val points: List<Vec2>, val graphSource: String?, val description: String)
data class ArtCurveMetrics(
    val pathLength: Double,
    val boundsMin: Vec2,
    val boundsMax: Vec2,
    val closureError: Double,
    val closed: Boolean,
)
data class ArtTriangle(val a: Vec2, val b: Vec2, val c: Vec2)
data class MandelbrotCell(val column: Int, val row: Int, val iterations: Int, val normalized: Double)
data class MandelbrotArt(val columns: Int, val rows: Int, val maximumIterations: Int, val cells: List<MandelbrotCell>)

object MathematicalArtEngine {
    fun customParametric(xExpression: String, yExpression: String, samples: Int = 960): ArtCurve {
        require(xExpression.isNotBlank() && yExpression.isNotBlank() && samples in 64..5_000) { "Enter x(t), y(t), and a valid sample count." }
        val source = "x(t)=${xExpression.substringAfter('=')}; y(t)=${yExpression.substringAfter('=')}"
        val points = GraphAnalysis().sampleParametric(source, samples).points
        require(points.size >= 2) { "The expressions did not produce a drawable curve." }
        return ArtCurve(points, source, "A custom parametric curve sampled over −2π ≤ t ≤ 2π.")
    }

    fun measure(curve: ArtCurve): ArtCurveMetrics {
        require(curve.points.isNotEmpty()) { "A curve needs points to measure." }
        val length = curve.points.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y) }
        val first = curve.points.first(); val last = curve.points.last()
        val closure = hypot(last.x - first.x, last.y - first.y)
        val min = Vec2(curve.points.minOf { it.x }, curve.points.minOf { it.y })
        val max = Vec2(curve.points.maxOf { it.x }, curve.points.maxOf { it.y })
        val span = maxOf(max.x - min.x, max.y - min.y).coerceAtLeast(1e-12)
        return ArtCurveMetrics(length, min, max, closure, closure <= span * .01)
    }

    fun polar(type: PolarCurveType, parameter: Double, scale: Double = 1.0, samples: Int = 721): ArtCurve {
        require(parameter.isFinite() && scale.isFinite() && scale > 0.0 && samples in 64..5_000)
        val points = InteractiveTrigEngine.polarSamples(type, parameter, samples).map { it * scale }
        val radius = when (type) {
            PolarCurveType.Rose -> "$scale*cos($parameter*t)"
            PolarCurveType.Cardioid -> "$scale*(1+cos(t))"
            PolarCurveType.Spiral -> "${scale * parameter}*t/(2*pi)"
            PolarCurveType.Lemniscate -> null
        }
        return ArtCurve(points, radius?.let { "r=$it" }, when (type) {
            PolarCurveType.Rose -> "A polar rose; rational frequency ratios control rotational symmetry."
            PolarCurveType.Cardioid -> "A cardioid formed by a first-harmonic polar radius."
            PolarCurveType.Spiral -> "An Archimedean spiral with radius proportional to angle."
            PolarCurveType.Lemniscate -> "A two-lobed lemniscate generated from cos(2θ)."
        })
    }

    fun lissajous(
        xFrequency: Int,
        yFrequency: Int,
        phaseRadians: Double,
        xAmplitude: Double = 1.0,
        yAmplitude: Double = 1.0,
        samples: Int = 960,
    ): ArtCurve {
        require(xFrequency in 1..24 && yFrequency in 1..24 && phaseRadians.isFinite())
        require(xAmplitude > 0.0 && yAmplitude > 0.0 && samples in 64..5_000)
        val source = "x(t)=$xAmplitude*sin($xFrequency*t+$phaseRadians); y(t)=$yAmplitude*sin($yFrequency*t)"
        val points = GraphAnalysis().sampleParametric(source, samples).points
        return ArtCurve(points, source, "A Lissajous figure; the frequency ratio $xFrequency:$yFrequency determines its lobes and closure.")
    }

    fun harmonograph(
        firstFrequency: Double,
        secondFrequency: Double,
        phaseRadians: Double,
        damping: Double,
        duration: Double = 30.0,
        samples: Int = 1_500,
    ): ArtCurve {
        require(firstFrequency > 0.0 && secondFrequency > 0.0 && damping >= 0.0 && duration > 0.0 && samples in 64..5_000)
        val points = (0..samples).map { index ->
            val t = duration * index / samples
            val fade = exp(-damping * t)
            Vec2(
                fade * (sin(firstFrequency * t + phaseRadians) + .45 * sin((firstFrequency + secondFrequency) * t)),
                fade * (sin(secondFrequency * t) + .45 * cos((secondFrequency - firstFrequency) * t + phaseRadians)),
            )
        }
        return ArtCurve(points, null, "A damped harmonograph combining coupled sinusoidal motions.")
    }

    fun koch(depth: Int): ArtCurve {
        require(depth in 0..6) { "Koch depth must be between 0 and 6." }
        var points = listOf(Vec2(-1.0, 0.0), Vec2(1.0, 0.0))
        repeat(depth) {
            points = points.zipWithNext().flatMapIndexed { index, (a, b) ->
                val delta = (b - a) * (1.0 / 3.0)
                val first = a + delta
                val third = a + delta * 2.0
                val rotated = Vec2(delta.x * .5 - delta.y * (kotlin.math.sqrt(3.0) / 2.0), delta.x * (kotlin.math.sqrt(3.0) / 2.0) + delta.y * .5)
                listOf(a, first, first + rotated, third) + if (index == points.size - 2) listOf(b) else emptyList()
            }
        }
        return ArtCurve(points, null, "Koch curve depth $depth: each segment becomes four self-similar segments.")
    }

    fun sierpinski(depth: Int): List<ArtTriangle> {
        require(depth in 0..7) { "Sierpiński depth must be between 0 and 7." }
        var triangles = listOf(ArtTriangle(Vec2(-1.0, -.72), Vec2(1.0, -.72), Vec2(0.0, 1.0)))
        repeat(depth) {
            triangles = triangles.flatMap { triangle ->
                val ab = midpoint(triangle.a, triangle.b); val bc = midpoint(triangle.b, triangle.c); val ca = midpoint(triangle.c, triangle.a)
                listOf(ArtTriangle(triangle.a, ab, ca), ArtTriangle(ab, triangle.b, bc), ArtTriangle(ca, bc, triangle.c))
            }
        }
        return triangles
    }

    fun mandelbrot(
        columns: Int = 120,
        rows: Int = 90,
        maximumIterations: Int = 64,
        center: Vec2 = Vec2(-.5, 0.0),
        span: Double = 3.2,
    ): MandelbrotArt {
        require(columns in 24..320 && rows in 24..240 && maximumIterations in 8..300 && span > 0.0)
        val aspect = rows.toDouble() / columns
        val cells = ArrayList<MandelbrotCell>(columns * rows)
        for (row in 0 until rows) for (column in 0 until columns) {
            val cx = center.x + span * (column.toDouble() / (columns - 1) - .5)
            val cy = center.y + span * aspect * (row.toDouble() / (rows - 1) - .5)
            var zx = 0.0; var zy = 0.0; var iteration = 0
            while (zx * zx + zy * zy <= 4.0 && iteration < maximumIterations) {
                val nextX = zx * zx - zy * zy + cx
                zy = 2.0 * zx * zy + cy
                zx = nextX
                iteration++
            }
            cells += MandelbrotCell(column, row, iteration, iteration.toDouble() / maximumIterations)
        }
        return MandelbrotArt(columns, rows, maximumIterations, cells)
    }

    fun radialCopies(seed: List<Vec2>, copies: Int): List<List<Vec2>> {
        require(copies in 1..48)
        return (0 until copies).map { copy ->
            val angle = 2.0 * PI * copy / copies
            val cosine = cos(angle); val sine = sin(angle)
            seed.map { Vec2(it.x * cosine - it.y * sine, it.x * sine + it.y * cosine) }
        }
    }

    private fun midpoint(a: Vec2, b: Vec2) = (a + b) * .5
}
