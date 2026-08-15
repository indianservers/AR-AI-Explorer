package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Line3D
import com.indianservers.aiexplorer.core.Plane3D
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.SectionLoop3D
import com.indianservers.aiexplorer.spatial.SpatialAnalysisTools3D
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.spatial.TypedSurfaceMesher
import kotlin.math.abs
import kotlin.math.max

enum class SpatialEquationForm { ExplicitSurface, ImplicitSurface, ParametricSurface, ParametricCurve, Plane }

data class SpatialEquationRecord(
    val id: String,
    val form: SpatialEquationForm,
    val equations: List<String>,
    val parameters: List<String> = emptyList(),
    val dependencies: Set<String> = emptySet(),
)

data class SurfaceDifferentialRecord(
    val surfaceId: String,
    val point: Vec3,
    val gradient: Vec3,
    val unitNormal: Vec3,
    val tangentPlane: Plane3D,
    val tangentPlaneEquation: String,
)

data class ParametricCurve3D(
    val id: String,
    val x: String,
    val y: String,
    val z: String,
    val parameter: String = "t",
    val range: ClosedFloatingPointRange<Double> = -5.0..5.0,
)

data class SampledSpaceCurve(val definition: ParametricCurve3D, val points: List<Vec3>, val discardedSamples: Int)
data class SpatialProjectionPoint(val world: Vec3, val planeCoordinates: Vec2)
data class SpatialCrossSectionRecord(val surfaceId: String, val plane: Plane3D, val loops: List<SectionLoop3D>, val projectedLoops: List<List<SpatialProjectionPoint>>)

data class SharedSpatialMathSchema(
    val equations: List<SpatialEquationRecord> = emptyList(),
    val differentials: List<SurfaceDifferentialRecord> = emptyList(),
    val curves: List<SampledSpaceCurve> = emptyList(),
    val crossSections: List<SpatialCrossSectionRecord> = emptyList(),
    val planeIntersections: List<Line3D> = emptyList(),
)

/** Renderer-independent 3D mathematical analysis shared by Algebra, Graph, Solver, tables and AR. */
class SharedSpatialMathEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun equation(definition: SurfaceDefinition3D): SpatialEquationRecord = when (definition) {
        is SurfaceDefinition3D.Explicit -> SpatialEquationRecord(definition.id, SpatialEquationForm.ExplicitSurface, listOf(normalizeExplicit(definition.z)))
        is SurfaceDefinition3D.Implicit -> SpatialEquationRecord(definition.id, SpatialEquationForm.ImplicitSurface, listOf(definition.equation))
        is SurfaceDefinition3D.Parametric -> SpatialEquationRecord(definition.id, SpatialEquationForm.ParametricSurface,
            listOf("x=${definition.x}", "y=${definition.y}", "z=${definition.z}"), listOf(definition.firstParameter, definition.secondParameter))
    }

    fun differential(definition: SurfaceDefinition3D, first: Double, second: Double, third: Double? = null): SurfaceDifferentialRecord {
        val h = max(1e-5, max(abs(first), abs(second)) * 1e-5)
        val (point, gradient) = when (definition) {
            is SurfaceDefinition3D.Explicit -> {
                val f = expressions.compile(stripExplicit(definition.z))
                fun z(x: Double, y: Double) = f.eval(mapOf("x" to x, "y" to y))
                val value = z(first, second)
                val fx = (z(first + h, second) - z(first - h, second)) / (2 * h)
                val fy = (z(first, second + h) - z(first, second - h)) / (2 * h)
                Vec3(first, second, value) to Vec3(-fx, -fy, 1.0)
            }
            is SurfaceDefinition3D.Implicit -> {
                val f = compileImplicit(definition.equation); val z = third ?: error("Implicit surface analysis requires x, y and z")
                fun value(x: Double, y: Double, zValue: Double) = f.eval(mapOf("x" to x, "y" to y, "z" to zValue))
                val residual = value(first, second, z)
                require(abs(residual) < 1e-4) { "Point is not on ${definition.id}; residual=$residual" }
                val fx = (value(first + h, second, z) - value(first - h, second, z)) / (2 * h)
                val fy = (value(first, second + h, z) - value(first, second - h, z)) / (2 * h)
                val fz = (value(first, second, z + h) - value(first, second, z - h)) / (2 * h)
                Vec3(first, second, z) to Vec3(fx, fy, fz)
            }
            is SurfaceDefinition3D.Parametric -> {
                val evaluators = listOf(definition.x, definition.y, definition.z).map { expressions.compile(it) }
                fun point(u: Double, v: Double) = evaluators.map { it.eval(mapOf(definition.firstParameter to u, definition.secondParameter to v)) }.let { Vec3(it[0], it[1], it[2]) }
                val center = point(first, second)
                val du = (point(first + h, second) - point(first - h, second)) * (1.0 / (2 * h))
                val dv = (point(first, second + h) - point(first, second - h)) * (1.0 / (2 * h))
                center to AnalyticGeometry3D.cross(du, dv)
            }
        }
        require(gradient.magnitude() > 1e-10) { "Surface normal is undefined at the requested point" }
        val normal = gradient.normalized(); val plane = Plane3D(point, normal)
        return SurfaceDifferentialRecord(definition.id, point, gradient, normal, plane, planeEquation(point, gradient))
    }

    fun sample(curve: ParametricCurve3D, samples: Int = 240): SampledSpaceCurve {
        require(samples in 2..5000 && curve.range.start < curve.range.endInclusive)
        val evaluators = listOf(curve.x, curve.y, curve.z).map { expressions.compile(it) }
        var discarded = 0
        val points = (0..samples).mapNotNull { index ->
            val t = curve.range.start + (curve.range.endInclusive - curve.range.start) * index / samples
            runCatching { evaluators.map { it.eval(mapOf(curve.parameter to t)) }.let { Vec3(it[0], it[1], it[2]) } }
                .getOrNull()?.takeIf(::finite) ?: run { discarded++; null }
        }
        return SampledSpaceCurve(curve, points, discarded)
    }

    fun planeIntersection(first: Plane3D, second: Plane3D): Line3D? = AnalyticGeometry3D.planeIntersection(first, second)

    fun crossSection(definition: SurfaceDefinition3D, plane: Plane3D, density: Int = 32): SpatialCrossSectionRecord {
        val geometry = TypedSurfaceMesher(expressions).mesh(definition, density).geometry
        val loops = SpatialAnalysisTools3D.crossSection(geometry, plane, 1e-6)
        val projected = loops.map { loop -> project(loop.points, plane) }
        return SpatialCrossSectionRecord(definition.id, plane, loops, projected)
    }

    fun schema(definitions: List<SurfaceDefinition3D>, probes: Map<String, List<Double>> = emptyMap()): SharedSpatialMathSchema {
        val differentials = definitions.mapNotNull { definition -> probes[definition.id]?.let { values ->
            require(values.size in 2..3); differential(definition, values[0], values[1], values.getOrNull(2))
        } }
        return SharedSpatialMathSchema(equations = definitions.map(::equation), differentials = differentials)
    }

    private fun project(points: List<Vec3>, plane: Plane3D): List<SpatialProjectionPoint> {
        val normal = plane.normal.normalized()
        val reference = if (abs(normal.x) < .8) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val u = AnalyticGeometry3D.cross(normal, reference).normalized()
        val v = AnalyticGeometry3D.cross(normal, u).normalized()
        return points.map { point -> val offset = point - plane.point; SpatialProjectionPoint(point, Vec2(offset.dot(u), offset.dot(v))) }
    }

    private fun compileImplicit(source: String) = if ('=' in source) source.split('=', limit = 2).let { expressions.compile("(${it[0]})-(${it[1]})") } else expressions.compile(source)
    private fun stripExplicit(source: String) = source.substringAfter('=').trim().ifBlank { source.trim() }
    private fun normalizeExplicit(source: String) = if ('=' in source) source else "z=$source"
    private fun finite(point: Vec3) = point.x.isFinite() && point.y.isFinite() && point.z.isFinite()
    private fun planeEquation(point: Vec3, normal: Vec3) = "${number(normal.x)}*(x-${number(point.x)}) + ${number(normal.y)}*(y-${number(point.y)}) + ${number(normal.z)}*(z-${number(point.z)}) = 0"
    private fun number(value: Double): String = if (abs(value) < 1e-9) "0" else if (abs(value - value.toLong()) < 1e-8) value.toLong().toString() else String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
}
