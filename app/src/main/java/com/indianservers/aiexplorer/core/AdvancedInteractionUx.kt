package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.cos
import kotlin.math.sin

enum class SpatialTransformSpace { World, Local }
enum class SpatialDragPlane { Free, XY, XZ, YZ }
enum class SpatialVisualMode { Solid, Wireframe, XRay }
enum class SpatialAlignment { X, Y, Z }

data class GraphFitResult(val expression: String, val coefficients: List<Double>, val residuals: List<Vec2>, val rSquared: Double)
data class SpatialCameraBookmark(val name: String, val rotation: Vec3, val zoom: Float, val panX: Float, val panY: Float)
data class SpatialCollision(val first: Int, val second: Int)

object GraphDirectManipulationEngine {
    fun translate(expression: String, delta: Vec2): String {
        val horizontal = GraphUxEngine.transform(expression, GraphTransformKind.TranslateX, delta.x)
        return GraphUxEngine.transform(horizontal, GraphTransformKind.TranslateY, delta.y)
    }

    fun signedArea(expression: Expression, from: Double, to: Double, steps: Int = 240): Double {
        val a = min(from, to)
        val b = max(from, to)
        if (a == b) return 0.0
        val n = steps.coerceIn(16, 4000)
        val width = (b - a) / n
        return (0 until n).sumOf { index ->
            val x1 = a + index * width
            val x2 = x1 + width
            val y1 = expression.eval(mapOf("x" to x1))
            val y2 = expression.eval(mapOf("x" to x2))
            if (y1.isFinite() && y2.isFinite()) (y1 + y2) * .5 * width else 0.0
        }
    }

    /** Least-squares line or quadratic for a freehand/data-point sketch. */
    fun fit(points: List<Vec2>, degree: Int = 2): GraphFitResult? {
        if (points.size < if (degree == 1) 2 else 3) return null
        val sampled = points.filter { it.x.isFinite() && it.y.isFinite() }.distinctBy { (it.x * 1000).toInt() }.take(800)
        if (sampled.size < 3) return null
        val coefficients = if (degree <= 1) {
            val meanX = sampled.map { it.x }.average(); val meanY = sampled.map { it.y }.average()
            val denominator = sampled.sumOf { (it.x - meanX) * (it.x - meanX) }
            if (abs(denominator) < 1e-12) return null
            val slope = sampled.sumOf { (it.x - meanX) * (it.y - meanY) } / denominator
            listOf(meanY - slope * meanX, slope)
        } else solve3(
            arrayOf(
                doubleArrayOf(sampled.size.toDouble(), sampled.sumOf { it.x }, sampled.sumOf { it.x * it.x }),
                doubleArrayOf(sampled.sumOf { it.x }, sampled.sumOf { it.x * it.x }, sampled.sumOf { it.x * it.x * it.x }),
                doubleArrayOf(sampled.sumOf { it.x * it.x }, sampled.sumOf { it.x * it.x * it.x }, sampled.sumOf { it.x * it.x * it.x * it.x }),
            ),
            doubleArrayOf(sampled.sumOf { it.y }, sampled.sumOf { it.x * it.y }, sampled.sumOf { it.x * it.x * it.y }),
        )?.toList() ?: return null
        fun predicted(x: Double) = coefficients.getOrElse(0) { 0.0 } + coefficients.getOrElse(1) { 0.0 } * x + coefficients.getOrElse(2) { 0.0 } * x * x
        val residuals = sampled.map { Vec2(it.x, it.y - predicted(it.x)) }
        val mean = sampled.map { it.y }.average()
        val total = sampled.sumOf { (it.y - mean) * (it.y - mean) }
        val error = residuals.sumOf { it.y * it.y }
        val r2 = if (total < 1e-12) 1.0 else 1.0 - error / total
        val clean: (Double) -> String = { value -> if (abs(value - round(value)) < 1e-8) round(value).toLong().toString() else "%.4f".format(value).trimEnd('0').trimEnd('.') }
        val expression = if (coefficients.size == 2) "${clean(coefficients[1])}*x+${clean(coefficients[0])}" else "${clean(coefficients[2])}*x^2+${clean(coefficients[1])}*x+${clean(coefficients[0])}"
        return GraphFitResult(expression, coefficients, residuals, r2)
    }

    private fun solve3(matrix: Array<DoubleArray>, values: DoubleArray): DoubleArray? {
        val augmented = Array(3) { row -> DoubleArray(4) { column -> if (column < 3) matrix[row][column] else values[row] } }
        for (pivot in 0..2) {
            val best = (pivot..2).maxBy { abs(augmented[it][pivot]) }
            if (abs(augmented[best][pivot]) < 1e-12) return null
            val swap = augmented[pivot]; augmented[pivot] = augmented[best]; augmented[best] = swap
            val divisor = augmented[pivot][pivot]
            for (column in pivot..3) augmented[pivot][column] /= divisor
            for (row in 0..2) if (row != pivot) {
                val factor = augmented[row][pivot]
                for (column in pivot..3) augmented[row][column] -= factor * augmented[pivot][column]
            }
        }
        return DoubleArray(3) { augmented[it][3] }
    }
}

object AdvancedSpatialInteractionEngine {
    fun transformSpace(delta: Vec3, rotationDegrees: Vec3, space: SpatialTransformSpace): Vec3 {
        if (space == SpatialTransformSpace.World) return delta
        val z = Math.toRadians(rotationDegrees.z)
        val y = Math.toRadians(rotationDegrees.y)
        val afterZ = Vec3(delta.x * cos(z) - delta.y * sin(z), delta.x * sin(z) + delta.y * cos(z), delta.z)
        return Vec3(afterZ.x * cos(y) + afterZ.z * sin(y), afterZ.y, -afterZ.x * sin(y) + afterZ.z * cos(y))
    }

    fun constrain(delta: Vec3, plane: SpatialDragPlane): Vec3 = when (plane) {
        SpatialDragPlane.Free -> delta
        SpatialDragPlane.XY -> delta.copy(z = 0.0)
        SpatialDragPlane.XZ -> delta.copy(y = 0.0)
        SpatialDragPlane.YZ -> delta.copy(x = 0.0)
    }

    fun snap(position: Vec3, others: List<Vec3>, step: Double = .25, threshold: Double = .16): Vec3 {
        fun grid(value: Double) = round(value / step) * step
        var result = Vec3(grid(position.x), grid(position.y), grid(position.z))
        others.forEach { candidate ->
            if (abs(result.x - candidate.x) < threshold) result = result.copy(x = candidate.x)
            if (abs(result.y - candidate.y) < threshold) result = result.copy(y = candidate.y)
            if (abs(result.z - candidate.z) < threshold) result = result.copy(z = candidate.z)
        }
        return result
    }

    fun extrude(solid: Solid, faceIndex: Int, amount: Double): Solid = when (faceIndex.mod(3)) {
        0 -> solid.copy(width = (solid.width + amount).coerceAtLeast(.1))
        1 -> solid.copy(height = (solid.height + amount).coerceAtLeast(.1))
        else -> solid.copy(depth = (solid.depth + amount).coerceAtLeast(.1))
    }

    fun reflect(solid: Solid, axis: SpatialAlignment): Solid = solid.copy(position = when (axis) {
        SpatialAlignment.X -> solid.position.copy(x = -solid.position.x)
        SpatialAlignment.Y -> solid.position.copy(y = -solid.position.y)
        SpatialAlignment.Z -> solid.position.copy(z = -solid.position.z)
    })

    fun exploded(solids: List<Solid>, amount: Double): List<Solid> {
        if (amount == 0.0 || solids.isEmpty()) return solids
        val center = Vec3(solids.map { it.position.x }.average(), solids.map { it.position.y }.average(), solids.map { it.position.z }.average())
        return solids.mapIndexed { index, solid ->
            var direction = solid.position - center
            if (direction.magnitude() < 1e-8) direction = Vec3(if (index % 2 == 0) 1.0 else -1.0, 0.0, if (index % 3 == 0) .5 else -.5)
            solid.copy(position = solid.position + direction.normalized() * amount)
        }
    }

    fun align(solids: List<Solid>, indices: Set<Int>, axis: SpatialAlignment): List<Solid> {
        val selected = indices.mapNotNull(solids::getOrNull)
        if (selected.size < 2) return solids
        val target = when (axis) { SpatialAlignment.X -> selected.map { it.position.x }.average(); SpatialAlignment.Y -> selected.map { it.position.y }.average(); SpatialAlignment.Z -> selected.map { it.position.z }.average() }
        return solids.mapIndexed { index, solid -> if (index !in indices) solid else solid.copy(position = when (axis) {
            SpatialAlignment.X -> solid.position.copy(x = target)
            SpatialAlignment.Y -> solid.position.copy(y = target)
            SpatialAlignment.Z -> solid.position.copy(z = target)
        }) }
    }

    fun distribute(solids: List<Solid>, indices: Set<Int>, axis: SpatialAlignment): List<Solid> {
        if (indices.size < 3) return solids
        val ordered = indices.sortedBy { index -> when (axis) { SpatialAlignment.X -> solids[index].position.x; SpatialAlignment.Y -> solids[index].position.y; SpatialAlignment.Z -> solids[index].position.z } }
        val values = ordered.map { index -> when (axis) { SpatialAlignment.X -> solids[index].position.x; SpatialAlignment.Y -> solids[index].position.y; SpatialAlignment.Z -> solids[index].position.z } }
        val step = (values.last() - values.first()) / (values.size - 1)
        val replacements = ordered.mapIndexed { rank, index -> index to (values.first() + rank * step) }.toMap()
        return solids.mapIndexed { index, solid -> replacements[index]?.let { value -> solid.copy(position = when (axis) {
            SpatialAlignment.X -> solid.position.copy(x = value); SpatialAlignment.Y -> solid.position.copy(y = value); SpatialAlignment.Z -> solid.position.copy(z = value)
        }) } ?: solid }
    }

    fun collisions(solids: List<Solid>): List<SpatialCollision> {
        fun half(solid: Solid) = Vec3(max(solid.width, solid.radius * 2) / 2, max(solid.height, solid.radius * 2) / 2, max(solid.depth, solid.radius * 2) / 2)
        return buildList {
            for (first in solids.indices) for (second in first + 1 until solids.size) {
                val a = solids[first]; val b = solids[second]; val ah = half(a); val bh = half(b)
                if (abs(a.position.x - b.position.x) <= ah.x + bh.x && abs(a.position.y - b.position.y) <= ah.y + bh.y && abs(a.position.z - b.position.z) <= ah.z + bh.z) add(SpatialCollision(first, second))
            }
        }
    }
}

/** Auditable registry tying every requested UI enhancement to a concrete interaction capability. */
object AdvancedInteractionFeatureRegistry {
    val graph = listOf("curve-drag", "coefficient-handles", "feature-snap", "live-tangent", "tangent-drag", "area-brush", "between-curves", "pinned-traces", "crosshairs", "discontinuities", "domain-handles", "ghost-transform", "animated-transform", "multi-select", "radial-menu", "draggable-labels", "calculus-layers", "intersection-magnets", "split-comparison", "viewport-thumbnails", "sketch-fit", "draggable-data", "residual-plot", "advanced-coordinate-modes", "parameter-timeline")
    val spatial = listOf("axis-gizmo", "rotation-rings", "scale-handles", "plane-drag", "local-world-axes", "spatial-snap", "live-dimensions", "face-extrusion", "subobject-edit", "section-plane", "xray-wireframe", "orientation-cube", "multi-select-group", "collision-warning", "exploded-view", "measurement-anchors", "symmetry-reflection", "copy-drag", "camera-bookmarks", "projection-presets", "depth-cues", "scene-navigator", "multitouch-object-transform", "motion-trails", "spatial-constraints")
    val all = graph + spatial
}
