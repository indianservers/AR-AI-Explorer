package com.indianservers.aiexplorer.arengine.analysis

import com.indianservers.aiexplorer.arengine.contract.ArMeasurementKind
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

data class ArPlane(val point: ArVector3, val normal: ArVector3) {
    init {
        require(normal.magnitude() > 1e-12)
    }
    val unitNormal: ArVector3 get() = normal * (1.0 / normal.magnitude())
    fun signedDistance(value: ArVector3) = unitNormal.dot(value - point)
    fun project(value: ArVector3) = value - unitNormal * signedDistance(value)
}

data class ArSurfaceHandle(
    val objectId: String,
    val triangleIndex: Int,
    val pointUnits: ArVector3,
    val barycentric: ArVector3,
)

data class ArSurfaceDifferential(
    val pointUnits: ArVector3,
    val unitNormal: ArVector3,
    val tangentU: ArVector3,
    val tangentV: ArVector3,
    val gradient: ArVector3,
    val tangentPlane: ArPlane,
)

object ArSurfaceAnalysisEngine {
    fun constrain(objectId: String, mesh: ArMesh, requested: ArVector3): ArSurfaceHandle? {
        var best: ArSurfaceHandle? = null
        var bestDistance = Double.POSITIVE_INFINITY
        mesh.triangleIndices.chunked(3).forEachIndexed { index, triangle ->
            if (triangle.size != 3) return@forEachIndexed
            val a = mesh.vertices[triangle[0]]
            val b = mesh.vertices[triangle[1]]
            val c = mesh.vertices[triangle[2]]
            val closest = closestPointAndBarycentric(requested, a, b, c)
            val distance = (closest.first - requested).magnitude()
            if (distance < bestDistance) {
                bestDistance = distance
                best = ArSurfaceHandle(objectId, index, closest.first, closest.second)
            }
        }
        return best
    }

    fun differential(
        mesh: ArMesh,
        handle: ArSurfaceHandle,
        gradientHint: ArVector3 = ArVector3.Zero,
    ): ArSurfaceDifferential {
        val triangle = mesh.triangleIndices.drop(handle.triangleIndex * 3).take(3)
        require(triangle.size == 3)
        val a = mesh.vertices[triangle[0]]
        val b = mesh.vertices[triangle[1]]
        val c = mesh.vertices[triangle[2]]
        val tangentU = (b - a).normalizedOr(ArVector3(1.0, 0.0, 0.0))
        val normal = cross(b - a, c - a).normalizedOr(ArVector3.Up)
        val tangentV = cross(normal, tangentU).normalizedOr(ArVector3(0.0, 0.0, 1.0))
        val tangentGradient = gradientHint - normal * gradientHint.dot(normal)
        return ArSurfaceDifferential(
            pointUnits = handle.pointUnits,
            unitNormal = normal,
            tangentU = tangentU,
            tangentV = tangentV,
            gradient = tangentGradient,
            tangentPlane = ArPlane(handle.pointUnits, normal),
        )
    }

    private fun closestPointAndBarycentric(p: ArVector3, a: ArVector3, b: ArVector3, c: ArVector3): Pair<ArVector3, ArVector3> {
        val ab = b - a
        val ac = c - a
        val ap = p - a
        val d1 = ab.dot(ap)
        val d2 = ac.dot(ap)
        if (d1 <= 0.0 && d2 <= 0.0) return a to ArVector3(1.0, 0.0, 0.0)
        val bp = p - b
        val d3 = ab.dot(bp)
        val d4 = ac.dot(bp)
        if (d3 >= 0.0 && d4 <= d3) return b to ArVector3(0.0, 1.0, 0.0)
        val vc = d1 * d4 - d3 * d2
        if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0) {
            val v = d1 / (d1 - d3)
            return (a + ab * v) to ArVector3(1.0 - v, v, 0.0)
        }
        val cp = p - c
        val d5 = ab.dot(cp)
        val d6 = ac.dot(cp)
        if (d6 >= 0.0 && d5 <= d6) return c to ArVector3(0.0, 0.0, 1.0)
        val vb = d5 * d2 - d1 * d6
        if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0) {
            val w = d2 / (d2 - d6)
            return (a + ac * w) to ArVector3(1.0 - w, 0.0, w)
        }
        val va = d3 * d6 - d5 * d4
        if (va <= 0.0 && d4 - d3 >= 0.0 && d5 - d6 >= 0.0) {
            val w = (d4 - d3) / ((d4 - d3) + (d5 - d6))
            return (b + (c - b) * w) to ArVector3(0.0, 1.0 - w, w)
        }
        val denominator = 1.0 / (va + vb + vc)
        val v = vb * denominator
        val w = vc * denominator
        return (a + ab * v + ac * w) to ArVector3(1.0 - v - w, v, w)
    }
}

data class ArSectionLoop(
    val pointsUnits: List<ArVector3>,
    val closed: Boolean,
    val areaSquareUnits: Double,
    val perimeterUnits: Double,
)

object ArCrossSectionEngine {
    fun section(mesh: ArMesh, plane: ArPlane, tolerance: Double = 1e-7): List<ArSectionLoop> {
        val segments = mutableListOf<Pair<ArVector3, ArVector3>>()
        mesh.triangleIndices.chunked(3).forEach { triangle ->
            if (triangle.size != 3) return@forEach
            val values = triangle.map(mesh.vertices::get)
            val hits = mutableListOf<ArVector3>()
            listOf(0 to 1, 1 to 2, 2 to 0).forEach { (first, second) ->
                val a = values[first]
                val b = values[second]
                val da = plane.signedDistance(a)
                val db = plane.signedDistance(b)
                if (abs(da) <= tolerance) hits += a
                if (da * db < 0.0) hits += a + (b - a) * (da / (da - db))
            }
            val unique = hits.distinctBy { quantize(it, tolerance * 10.0) }
            if (unique.size >= 2) segments += unique[0] to unique[1]
        }
        return chain(segments, plane, tolerance * 20.0)
    }

    private fun chain(source: List<Pair<ArVector3, ArVector3>>, plane: ArPlane, tolerance: Double): List<ArSectionLoop> {
        val remaining = source.toMutableList()
        val result = mutableListOf<ArSectionLoop>()
        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val points = mutableListOf(seed.first, seed.second)
            while (true) {
                val end = points.last()
                val index = remaining.indexOfFirst { (it.first - end).magnitude() <= tolerance || (it.second - end).magnitude() <= tolerance }
                if (index < 0) break
                val segment = remaining.removeAt(index)
                points += if ((segment.first - end).magnitude() <= tolerance) segment.second else segment.first
            }
            val closed = points.size >= 3 && (points.first() - points.last()).magnitude() <= tolerance
            val clean = if (closed) points.dropLast(1) else points
            val perimeter = clean.indices.sumOf { index ->
                if (!closed && index == clean.lastIndex) 0.0 else (clean[(index + 1) % clean.size] - clean[index]).magnitude()
            }
            result += ArSectionLoop(clean, closed, if (closed) polygonArea(clean, plane.unitNormal) else 0.0, perimeter)
        }
        return result
    }

    private fun polygonArea(points: List<ArVector3>, normal: ArVector3): Double {
        var sum = ArVector3.Zero
        points.indices.forEach { index -> sum += cross(points[index], points[(index + 1) % points.size]) }
        return abs(sum.dot(normal)) * .5
    }
}

data class ArContour(val level: Double, val segments: List<Pair<ArVector3, ArVector3>>)

object ArContourEngine {
    fun horizontal(mesh: ArMesh, level: Double): ArContour {
        require(level.isFinite())
        val segments = mutableListOf<Pair<ArVector3, ArVector3>>()
        mesh.triangleIndices.chunked(3).forEach { triangle ->
            if (triangle.size != 3) return@forEach
            val values = triangle.map(mesh.vertices::get)
            val hits = mutableListOf<ArVector3>()
            listOf(0 to 1, 1 to 2, 2 to 0).forEach { (first, second) ->
                val a = values[first]
                val b = values[second]
                val da = a.z - level
                val db = b.z - level
                if (abs(da) < 1e-9) hits += a.copy(z = level)
                if (da * db < 0.0) hits += (a + (b - a) * (da / (da - db))).copy(z = level)
            }
            val unique = hits.distinctBy { quantize(it, 1e-7) }
            if (unique.size >= 2) segments += unique[0] to unique[1]
        }
        return ArContour(level, segments)
    }
}

data class ArGradientPath(
    val pointsUnits: List<ArVector3>,
    val ascending: Boolean,
    val currentStep: Int = 0,
) {
    val currentPoint: ArVector3? get() = pointsUnits.getOrNull(currentStep.coerceIn(0, pointsUnits.lastIndex.coerceAtLeast(0)))
}

object ArGradientPathEngine {
    fun generate(
        mesh: ArMesh,
        start: ArVector3,
        gradientAt: (ArVector3) -> ArVector3,
        ascending: Boolean,
        steps: Int = 80,
        stepUnits: Double = .08,
    ): ArGradientPath {
        require(steps in 1..2_000 && stepUnits.isFinite() && stepUnits > 0.0)
        var handle = ArSurfaceAnalysisEngine.constrain("surface", mesh, start) ?: return ArGradientPath(emptyList(), ascending)
        val points = mutableListOf<ArVector3>()
        repeat(steps) {
            points += handle.pointUnits
            val differential = ArSurfaceAnalysisEngine.differential(mesh, handle, gradientAt(handle.pointUnits))
            val magnitude = differential.gradient.magnitude()
            if (magnitude < 1e-10) return ArGradientPath(points, ascending)
            val sign = if (ascending) 1.0 else -1.0
            val requested = handle.pointUnits + differential.gradient * (sign * stepUnits / magnitude)
            handle = ArSurfaceAnalysisEngine.constrain("surface", mesh, requested) ?: return ArGradientPath(points, ascending)
        }
        return ArGradientPath(points, ascending)
    }

    fun scrub(path: ArGradientPath, step: Int) = path.copy(currentStep = step.coerceIn(0, path.pointsUnits.lastIndex.coerceAtLeast(0)))
    fun edit(path: ArGradientPath, index: Int, point: ArVector3, mesh: ArMesh): ArGradientPath {
        val constrained = ArSurfaceAnalysisEngine.constrain("surface", mesh, point) ?: return path
        return path.copy(pointsUnits = path.pointsUnits.mapIndexed { i, old -> if (i == index) constrained.pointUnits else old })
    }
}

enum class ArMeasurementTruth { ExactMathematical, EnvironmentalEstimate }

data class ArUncertaintyBudget(
    val poseMeters: Double = 0.0,
    val depthMeters: Double = 0.0,
    val endpointMeters: List<Double> = emptyList(),
    val scaleFraction: Double = 0.0,
) {
    init {
        require(poseMeters >= 0.0 && depthMeters >= 0.0 && scaleFraction >= 0.0)
        require(endpointMeters.all { it >= 0.0 })
    }
    fun absolute(value: Double) = sqrt(
        poseMeters * poseMeters +
            depthMeters * depthMeters +
            endpointMeters.sumOf { it * it } +
            value * value * scaleFraction * scaleFraction,
    )
}

data class ArAnalysisMeasurement(
    val kind: ArMeasurementKind,
    val value: Double,
    val unit: String,
    val points: List<ArVector3>,
    val truth: ArMeasurementTruth,
    val uncertainty: Double,
    val explanation: String,
) {
    val display: String
        get() = if (truth == ArMeasurementTruth.ExactMathematical) {
            "${format(value)} $unit · exact mathematical"
        } else {
            "${format(value)} ± ${format(uncertainty)} $unit · educational estimate"
        }
}

object ArMeasurementEngine {
    fun distance(
        first: ArVector3,
        second: ArVector3,
        unit: String,
        truth: ArMeasurementTruth,
        uncertainty: ArUncertaintyBudget = ArUncertaintyBudget(),
    ): ArAnalysisMeasurement {
        val value = (second - first).magnitude()
        return measurement(ArMeasurementKind.Distance, value, unit, listOf(first, second), truth, uncertainty, "Euclidean endpoint distance.")
    }

    fun angle(first: ArVector3, vertex: ArVector3, second: ArVector3, truth: ArMeasurementTruth, uncertainty: ArUncertaintyBudget = ArUncertaintyBudget()): ArAnalysisMeasurement {
        val a = first - vertex
        val b = second - vertex
        val value = Math.toDegrees(acos((a.dot(b) / max(1e-15, a.magnitude() * b.magnitude())).coerceIn(-1.0, 1.0)))
        val angularUncertainty = if (truth == ArMeasurementTruth.ExactMathematical) 0.0 else {
            Math.toDegrees(uncertainty.absolute(1.0) / max(1e-6, minOf(a.magnitude(), b.magnitude())))
        }
        return ArAnalysisMeasurement(ArMeasurementKind.Angle, value, "deg", listOf(first, vertex, second), truth, angularUncertainty, "Normalized dot-product angle.")
    }

    fun area(points: List<ArVector3>, unit: String, truth: ArMeasurementTruth, uncertainty: ArUncertaintyBudget = ArUncertaintyBudget()): ArAnalysisMeasurement {
        require(points.size >= 3)
        var sum = ArVector3.Zero
        points.indices.forEach { index -> sum += cross(points[index], points[(index + 1) % points.size]) }
        return measurement(ArMeasurementKind.Area, sum.magnitude() * .5, unit, points, truth, uncertainty, "Planar polygon area.")
    }

    fun volume(value: Double, unit: String, truth: ArMeasurementTruth, uncertainty: ArUncertaintyBudget = ArUncertaintyBudget()) =
        measurement(ArMeasurementKind.Volume, value, unit, emptyList(), truth, uncertainty, "Closed mathematical solid volume.")

    fun sectionPerimeter(loop: ArSectionLoop, unit: String, truth: ArMeasurementTruth, uncertainty: ArUncertaintyBudget = ArUncertaintyBudget()) =
        measurement(ArMeasurementKind.SectionPerimeter, loop.perimeterUnits, unit, loop.pointsUnits, truth, uncertainty, "Cross-section perimeter.")

    private fun measurement(
        kind: ArMeasurementKind,
        value: Double,
        unit: String,
        points: List<ArVector3>,
        truth: ArMeasurementTruth,
        uncertainty: ArUncertaintyBudget,
        explanation: String,
    ) = ArAnalysisMeasurement(
        kind,
        value,
        unit,
        points,
        truth,
        if (truth == ArMeasurementTruth.ExactMathematical) 0.0 else uncertainty.absolute(value),
        explanation,
    )
}

private fun ArVector3.normalizedOr(fallback: ArVector3): ArVector3 {
    val magnitude = magnitude()
    return if (magnitude < 1e-12) fallback else this * (1.0 / magnitude)
}

private fun cross(a: ArVector3, b: ArVector3) = ArVector3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x,
)

private fun quantize(value: ArVector3, scale: Double) = Triple(
    kotlin.math.round(value.x / scale).toLong(),
    kotlin.math.round(value.y / scale).toLong(),
    kotlin.math.round(value.z / scale).toLong(),
)

private fun format(value: Double) = "%.4g".format(java.util.Locale.US, value)
