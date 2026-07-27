package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.arengine.analysis.ArAnalysisMeasurement
import com.indianservers.aiexplorer.arengine.analysis.ArContourEngine
import com.indianservers.aiexplorer.arengine.analysis.ArCrossSectionEngine
import com.indianservers.aiexplorer.arengine.analysis.ArGradientPathEngine
import com.indianservers.aiexplorer.arengine.analysis.ArMeasurementEngine
import com.indianservers.aiexplorer.arengine.analysis.ArMeasurementTruth
import com.indianservers.aiexplorer.arengine.analysis.ArPlane
import com.indianservers.aiexplorer.arengine.analysis.ArSurfaceAnalysisEngine
import com.indianservers.aiexplorer.arengine.analysis.ArUncertaintyBudget
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.SurfaceMesh
import com.indianservers.aiexplorer.core.Vec3

data class ArPhase5AnalysisOptions(
    val enabled: Boolean = false,
    val traceX: Double = 0.0,
    val traceY: Double = 0.0,
    val contourLevel: Double = 0.0,
    val sectionNormal: Vec3 = Vec3(0.0, 1.0, 0.0),
    val sectionOffset: Double = 0.0,
    val ascending: Boolean = true,
    val gradientStep: Int = 0,
)

data class ArPhase5AnalysisResult(
    val scene: SpatialRenderScene,
    val measurements: List<ArAnalysisMeasurement>,
    val surfaceStatus: String,
    val sectionStatus: String,
    val gradientSteps: Int,
)

/**
 * Converts the renderer-neutral Phase 5 analysis into the same scene primitives used by
 * the regular 3D workspace and AR compositor. The mathematical model remains authoritative;
 * depth is used only for environmental presentation and uncertainty.
 */
object ArPhase5AnalysisBridge {
    private val normalMaterial = SpatialMaterial("surface normal", listOf(.2f, .95f, .62f, 1f), roughness = .25f, emissive = .35f)
    private val gradientMaterial = SpatialMaterial("surface gradient", listOf(1f, .56f, .14f, 1f), roughness = .25f, emissive = .35f)
    private val tangentMaterial = SpatialMaterial("tangent plane", listOf(.22f, .68f, 1f, .24f), roughness = .35f, blendMode = SpatialBlendMode.Transparent)
    private val contourMaterial = SpatialMaterial("contour", listOf(.95f, .26f, .78f, 1f), roughness = .2f, emissive = .4f)
    private val sectionMaterial = SpatialMaterial("cross section", listOf(.96f, .86f, .24f, 1f), roughness = .2f, emissive = .4f)
    private val handleMaterial = SpatialMaterial("analysis handle", listOf(1f, 1f, 1f, 1f), roughness = .15f, emissive = .55f)

    fun enrich(
        source: SpatialRenderScene,
        surface: SurfaceMesh?,
        expression: String,
        solids: List<Solid>,
        selectedObjectIds: Set<String>,
        options: ArPhase5AnalysisOptions,
        metersPerMathUnit: Double,
        poseUncertaintyMeters: Double,
        depthAvailable: Boolean,
    ): ArPhase5AnalysisResult {
        if (!options.enabled) return ArPhase5AnalysisResult(source, measurements(solids, selectedObjectIds, metersPerMathUnit, poseUncertaintyMeters, depthAvailable), "Analysis handles are off.", "Section plane is off.", 0)
        val overlays = mutableListOf<SpatialPrimitive>()
        val annotations = source.annotations.toMutableList()
        val sectionMeasurements = mutableListOf<ArAnalysisMeasurement>()
        var surfaceStatus = "No explicit surface is available."
        var gradientSteps = 0

        surface?.let { surfaceMesh ->
            val mesh = surfaceMesh.toArMesh()
            val exact = runCatching { SurfaceCalculus().analyze(expression, options.traceX, options.traceY) }.getOrNull()
            val requested = exact?.point?.toAr() ?: ArVector3(options.traceX, options.traceY, 0.0)
            val handle = ArSurfaceAnalysisEngine.constrain("surface", mesh, requested)
            if (handle != null) {
                val gradientHint = exact?.gradient?.toAr() ?: ArVector3.Zero
                val differential = ArSurfaceAnalysisEngine.differential(mesh, handle, gradientHint)
                val point = differential.pointUnits.toVec()
                val normalEnd = (differential.pointUnits + differential.unitNormal * .8).toVec()
                val gradient = differential.gradient
                val gradientEnd = if (gradient.magnitude() > 1e-9) (differential.pointUnits + gradient * (.8 / gradient.magnitude())).toVec() else point
                overlays += pointPrimitive("analysis-trace", point, handleMaterial, .11, "Constrained surface trace handle")
                overlays += linePrimitive("analysis-normal", listOf(point, normalEnd), normalMaterial, "Unit normal")
                overlays += linePrimitive("analysis-gradient", listOf(point, gradientEnd), gradientMaterial, if (options.ascending) "Gradient ascent" else "Gradient descent")
                overlays += tangentPlanePrimitive(point, differential.tangentU.toVec(), differential.tangentV.toVec())

                val contour = ArContourEngine.horizontal(mesh, options.contourLevel)
                contour.segments.forEachIndexed { index, segment ->
                    overlays += linePrimitive("analysis-contour-$index", listOf(segment.first.toVec(), segment.second.toVec()), contourMaterial, "Contour z = ${format(options.contourLevel)}")
                }
                val path = ArGradientPathEngine.generate(
                    mesh = mesh,
                    start = differential.pointUnits,
                    gradientAt = { p ->
                        runCatching { SurfaceCalculus().analyze(expression, p.x, p.y).gradient.toAr() }.getOrElse { ArVector3.Zero }
                    },
                    ascending = options.ascending,
                    steps = 80,
                )
                gradientSteps = path.pointsUnits.size
                if (path.pointsUnits.size >= 2) {
                    overlays += linePrimitive(
                        "analysis-gradient-path",
                        path.pointsUnits.map(ArVector3::toVec),
                        gradientMaterial.copy(colorRgba = listOf(1f, .35f, .12f, 1f)),
                        if (options.ascending) "Gradient ascent path" else "Gradient descent path",
                    )
                    path.pointsUnits.getOrNull(options.gradientStep.coerceIn(0, path.pointsUnits.lastIndex))?.let {
                        overlays += pointPrimitive("analysis-playhead", it.toVec(), gradientMaterial, .09, "Gradient playback handle")
                    }
                }
                annotations += SpatialAnnotation("analysis-value", point + Vec3(.12, .12, .12), "(${format(point.x)}, ${format(point.y)}, ${format(point.z)})")
                surfaceStatus = "Trace constrained to triangle ${handle.triangleIndex + 1}; normal, tangent plane, gradient, contour and path are live."
            }
        }

        var sectionStatus = "Select a solid to edit its cross-section."
        selectedObjectIds.firstNotNullOfOrNull { id -> source.primitives.firstOrNull { it.id == id && it.kind == SpatialPrimitiveKind.Solid } }?.let { primitive ->
            val normal = options.sectionNormal.normalizedOr(Vec3(0.0, 1.0, 0.0))
            val plane = ArPlane((normal * options.sectionOffset).toAr(), normal.toAr())
            overlays += sectionPlanePrimitive(normal * options.sectionOffset, normal)
            val loops = ArCrossSectionEngine.section(primitive.geometry.toArMesh(), plane)
            loops.forEachIndexed { loopIndex, loop ->
                if (loop.pointsUnits.size >= 2) {
                    val points = loop.pointsUnits.map(ArVector3::toVec)
                    overlays += linePrimitive("analysis-section-$loopIndex", points + if (loop.closed) listOf(points.first()) else emptyList(), sectionMaterial, "Editable cross-section")
                    sectionMeasurements += ArMeasurementEngine.sectionPerimeter(
                        loop,
                        "units",
                        ArMeasurementTruth.ExactMathematical,
                    )
                }
            }
            sectionStatus = if (loops.isEmpty()) "The editable section plane does not currently intersect ${primitive.label}." else {
                val totalPerimeter = loops.sumOf { it.perimeterUnits }
                "${loops.size} section loop(s) · perimeter ${format(totalPerimeter)} units."
            }
        }

        val resultMeasurements = measurements(solids, selectedObjectIds, metersPerMathUnit, poseUncertaintyMeters, depthAvailable) + sectionMeasurements
        val measurementHandles = resultMeasurements
            .filter { it.truth == ArMeasurementTruth.ExactMathematical && it.points.size >= 2 }
            .flatMapIndexed { index, measurement ->
                val points = measurement.points.map(ArVector3::toVec)
                val lines = when (measurement.kind) {
                    com.indianservers.aiexplorer.arengine.contract.ArMeasurementKind.Angle ->
                        listOf(linePrimitive("phase5-angle-a-$index", listOf(points[1], points[0]), handleMaterial, "Angle arm"),
                            linePrimitive("phase5-angle-b-$index", listOf(points[1], points[2]), handleMaterial, "Angle arm"))
                    else -> listOf(linePrimitive("phase5-measure-line-$index", points, handleMaterial, measurement.kind.name))
                }
                lines + points.mapIndexed { pointIndex, point ->
                    pointPrimitive("phase5-measure-handle-$index-$pointIndex", point, handleMaterial, .075, "${measurement.kind.name} handle")
                }
            }
        val selectedCenter = selectedObjectIds.firstNotNullOfOrNull { id ->
            source.primitives.firstOrNull { it.id == id && it.kind == SpatialPrimitiveKind.Solid }?.geometry?.vertices?.takeIf(List<Vec3>::isNotEmpty)?.let { vertices ->
                vertices.reduce(Vec3::plus) * (1.0 / vertices.size)
            }
        }
        selectedCenter?.let { center ->
            resultMeasurements.filter { it.points.isEmpty() }.forEachIndexed { index, measurement ->
                annotations += SpatialAnnotation("phase5-measure-label-$index", center + Vec3(0.0, .18 + index * .14, 0.0), measurement.display)
            }
        }
        val measurementOverlays = resultMeasurements.filter { it.points.size >= 2 }.mapIndexed { index, measurement ->
            SpatialMeasurementOverlay(
                id = "phase5-measure-$index",
                from = measurement.points.first().toVec(),
                to = measurement.points.last().toVec(),
                value = measurement.value,
                unit = measurement.unit,
                uncertainty = measurement.uncertainty,
                educationalEstimate = measurement.truth == ArMeasurementTruth.EnvironmentalEstimate,
            )
        }
        return ArPhase5AnalysisResult(
            scene = source.copy(primitives = source.primitives + overlays + measurementHandles, annotations = annotations, measurements = source.measurements + measurementOverlays),
            measurements = resultMeasurements,
            surfaceStatus = surfaceStatus,
            sectionStatus = sectionStatus,
            gradientSteps = gradientSteps,
        )
    }

    private fun measurements(
        solids: List<Solid>,
        selectedIds: Set<String>,
        metersPerMathUnit: Double,
        poseUncertaintyMeters: Double,
        depthAvailable: Boolean,
    ): List<ArAnalysisMeasurement> {
        val selected = selectedIds.mapNotNull { id -> id.removePrefix("solid-").toIntOrNull()?.let(solids::getOrNull) }
        val result = mutableListOf<ArAnalysisMeasurement>()
        val exact = ArUncertaintyBudget()
        val environmental = ArUncertaintyBudget(
            poseMeters = poseUncertaintyMeters,
            depthMeters = if (depthAvailable) .015 else .05,
            endpointMeters = listOf(poseUncertaintyMeters, poseUncertaintyMeters),
            scaleFraction = .02,
        )
        if (selected.size >= 2) {
            val first = selected[0].position.toAr()
            val second = selected[1].position.toAr()
            result += ArMeasurementEngine.distance(first, second, "units", ArMeasurementTruth.ExactMathematical, exact)
            result += ArMeasurementEngine.distance(first * metersPerMathUnit, second * metersPerMathUnit, "m", ArMeasurementTruth.EnvironmentalEstimate, environmental)
        }
        if (selected.size >= 3) {
            result += ArMeasurementEngine.angle(selected[0].position.toAr(), selected[1].position.toAr(), selected[2].position.toAr(), ArMeasurementTruth.ExactMathematical)
        }
        selected.firstOrNull()?.let { solid ->
            val values = Geometry3D.measure(solid)
            result += ArMeasurementEngine.volume(values.volume, "units³", ArMeasurementTruth.ExactMathematical)
            result += ArAnalysisMeasurement(
                kind = com.indianservers.aiexplorer.arengine.contract.ArMeasurementKind.Area,
                value = values.surfaceArea,
                unit = "units²",
                points = emptyList(),
                truth = ArMeasurementTruth.ExactMathematical,
                uncertainty = 0.0,
                explanation = "Analytic surface area of the selected mathematical solid.",
            )
        }
        return result
    }

    private fun SurfaceMesh.toArMesh(): ArMesh {
        val triangles = mutableListOf<Int>()
        for (row in 0 until rows - 1) for (column in 0 until columns - 1) {
            val a = row * columns + column
            val b = a + 1
            val c = a + columns
            val d = c + 1
            triangles += listOf(a, c, b, b, c, d)
        }
        return ArMesh(vertices.map(Vec3::toAr), triangles)
    }

    private fun SpatialGeometry.toArMesh() = ArMesh(
        vertices = vertices.map(Vec3::toAr),
        triangleIndices = triangles,
        lineIndices = lines.flatMap { listOf(it.first, it.second) },
        pointRadiusUnits = pointRadius,
    )

    private fun pointPrimitive(id: String, point: Vec3, material: SpatialMaterial, radius: Double, label: String) =
        SpatialPrimitive(id, SpatialPrimitiveKind.Point, SpatialGeometry(listOf(point), pointRadius = radius), material, label, selectable = false)

    private fun linePrimitive(id: String, points: List<Vec3>, material: SpatialMaterial, label: String): SpatialPrimitive {
        val lines = (0 until points.lastIndex).map { it to it + 1 }
        return SpatialPrimitive(id, SpatialPrimitiveKind.Curve, SpatialGeometry(points, lines = lines, pointRadius = .045), material, label, selectable = false)
    }

    private fun tangentPlanePrimitive(point: Vec3, tangentU: Vec3, tangentV: Vec3): SpatialPrimitive {
        val u = tangentU.normalizedOr(Vec3(1.0, 0.0, 0.0)) * .55
        val v = tangentV.normalizedOr(Vec3(0.0, 1.0, 0.0)) * .55
        return SpatialPrimitive(
            "analysis-tangent-plane",
            SpatialPrimitiveKind.Surface,
            SpatialGeometry(listOf(point - u - v, point + u - v, point + u + v, point - u + v), listOf(0, 1, 2, 0, 2, 3), listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)),
            tangentMaterial,
            "Tangent plane",
            selectable = false,
        )
    }

    private fun sectionPlanePrimitive(point: Vec3, normal: Vec3): SpatialPrimitive {
        val reference = if (kotlin.math.abs(normal.z) < .85) Vec3(0.0, 0.0, 1.0) else Vec3(1.0, 0.0, 0.0)
        val u = cross(normal, reference).normalizedOr(Vec3(1.0, 0.0, 0.0)) * 1.7
        val v = cross(normal, u).normalizedOr(Vec3(0.0, 1.0, 0.0)) * 1.7
        return SpatialPrimitive(
            "analysis-section-plane",
            SpatialPrimitiveKind.Surface,
            SpatialGeometry(listOf(point - u - v, point + u - v, point + u + v, point - u + v), listOf(0, 1, 2, 0, 2, 3), listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)),
            sectionMaterial.copy(colorRgba = listOf(.96f, .86f, .24f, .16f), blendMode = SpatialBlendMode.Transparent),
            "Draggable section plane",
            selectable = false,
        )
    }
}

private fun Vec3.toAr() = ArVector3(x, y, z)
private fun ArVector3.toVec() = Vec3(x, y, z)
private fun Vec3.normalizedOr(fallback: Vec3): Vec3 {
    val size = magnitude()
    return if (size < 1e-12) fallback else this * (1.0 / size)
}
private fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
private fun format(value: Double) = "%.4g".format(java.util.Locale.US, value)
