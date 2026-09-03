package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.hypot

data class CoordinateLineAnalysis(
    val first: Vec2,
    val second: Vec2,
    val delta: Vec2,
    val distance: Double,
    val midpoint: Vec2,
    val slope: Double?,
    val yIntercept: Double?,
) {
    val isVertical: Boolean get() = slope == null

    fun equation(): String = if (isVertical) {
        "x = ${formatCoordinate(first.x)}"
    } else {
        val m = requireNotNull(slope)
        val b = requireNotNull(yIntercept)
        when {
            abs(m) < EPSILON -> "y = ${formatCoordinate(b)}"
            abs(b) < EPSILON -> "y = ${formatCoefficient(m)}x"
            b > 0.0 -> "y = ${formatCoefficient(m)}x + ${formatCoordinate(b)}"
            else -> "y = ${formatCoefficient(m)}x - ${formatCoordinate(abs(b))}"
        }
    }

    fun parallelEquation(through: Vec2): String = equationWithSlope(slope, through)

    fun perpendicularEquation(through: Vec2): String = when {
        slope == null -> equationWithSlope(0.0, through)
        abs(slope) < EPSILON -> "x = ${formatCoordinate(through.x)}"
        else -> equationWithSlope(-1.0 / slope, through)
    }
}

enum class TriangleOrientation { Counterclockwise, Clockwise, Collinear }

data class CoordinateTriangleAnalysis(
    val vertices: List<Vec2>,
    val sideLengths: List<Double>,
    val perimeter: Double,
    val signedArea: Double,
    val area: Double,
    val centroid: Vec2,
    val orientation: TriangleOrientation,
    val sideClassification: String,
    val angleClassification: String,
    val circumcenter: Vec2?,
) {
    val isCollinear: Boolean get() = orientation == TriangleOrientation.Collinear
}

object CoordinatePlaneEngine {
    fun analyse(first: Vec2, second: Vec2): CoordinateLineAnalysis? {
        val dx = second.x - first.x
        val dy = second.y - first.y
        if (abs(dx) < EPSILON && abs(dy) < EPSILON) return null
        val slope = if (abs(dx) < EPSILON) null else dy / dx
        return CoordinateLineAnalysis(
            first = first,
            second = second,
            delta = Vec2(dx, dy),
            distance = hypot(dx, dy),
            midpoint = Vec2((first.x + second.x) / 2.0, (first.y + second.y) / 2.0),
            slope = slope,
            yIntercept = slope?.let { first.y - it * first.x },
        )
    }

    fun snap(point: Vec2, interval: Double = 0.5): Vec2 {
        require(interval > 0.0 && interval.isFinite())
        return Vec2(
            kotlin.math.round(point.x / interval) * interval,
            kotlin.math.round(point.y / interval) * interval,
        )
    }

    fun analyseTriangle(first: Vec2, second: Vec2, third: Vec2): CoordinateTriangleAnalysis {
        val signedDoubleArea = first.x * (second.y - third.y) +
            second.x * (third.y - first.y) + third.x * (first.y - second.y)
        val signedArea = signedDoubleArea / 2.0
        val sides = listOf(
            hypot(second.x - first.x, second.y - first.y),
            hypot(third.x - second.x, third.y - second.y),
            hypot(first.x - third.x, first.y - third.y),
        )
        val sortedSquares = sides.map { it * it }.sorted()
        val angleComparison = sortedSquares[0] + sortedSquares[1] - sortedSquares[2]
        val sideClassification = when {
            sides.maxOrNull()!! - sides.minOrNull()!! <= EPSILON -> "equilateral"
            sides.indices.any { i -> abs(sides[i] - sides[(i + 1) % 3]) <= EPSILON } -> "isosceles"
            else -> "scalene"
        }
        val denominator = 2.0 * signedDoubleArea
        val circumcenter = if (abs(denominator) <= EPSILON) null else {
            val a2 = first.x * first.x + first.y * first.y
            val b2 = second.x * second.x + second.y * second.y
            val c2 = third.x * third.x + third.y * third.y
            Vec2(
                (a2 * (second.y - third.y) + b2 * (third.y - first.y) + c2 * (first.y - second.y)) / denominator,
                (a2 * (third.x - second.x) + b2 * (first.x - third.x) + c2 * (second.x - first.x)) / denominator,
            )
        }
        return CoordinateTriangleAnalysis(
            vertices = listOf(first, second, third),
            sideLengths = sides,
            perimeter = sides.sum(),
            signedArea = signedArea,
            area = abs(signedArea),
            centroid = Vec2((first.x + second.x + third.x) / 3.0, (first.y + second.y + third.y) / 3.0),
            orientation = when {
                signedArea > EPSILON -> TriangleOrientation.Counterclockwise
                signedArea < -EPSILON -> TriangleOrientation.Clockwise
                else -> TriangleOrientation.Collinear
            },
            sideClassification = sideClassification,
            angleClassification = when {
                abs(signedArea) <= EPSILON -> "degenerate"
                abs(angleComparison) <= EPSILON -> "right"
                angleComparison > 0.0 -> "acute"
                else -> "obtuse"
            },
            circumcenter = circumcenter,
        )
    }
}

private fun equationWithSlope(slope: Double?, through: Vec2): String {
    if (slope == null) return "x = ${formatCoordinate(through.x)}"
    val intercept = through.y - slope * through.x
    return when {
        abs(slope) < EPSILON -> "y = ${formatCoordinate(intercept)}"
        abs(intercept) < EPSILON -> "y = ${formatCoefficient(slope)}x"
        intercept > 0.0 -> "y = ${formatCoefficient(slope)}x + ${formatCoordinate(intercept)}"
        else -> "y = ${formatCoefficient(slope)}x - ${formatCoordinate(abs(intercept))}"
    }
}

private fun formatCoefficient(value: Double): String = when {
    abs(value - 1.0) < EPSILON -> ""
    abs(value + 1.0) < EPSILON -> "-"
    else -> formatCoordinate(value)
}

fun formatCoordinate(value: Double): String {
    if (!value.isFinite()) return "—"
    val integral = value.toLong()
    return if (abs(value - integral) < EPSILON) integral.toString()
    else "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}

private const val EPSILON = 1e-9
