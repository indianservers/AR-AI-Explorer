package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.WorkspaceState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ArMathWorkspaceMode(val label: String, val shortLabel: String, val description: String) {
    Geometry2D("2D Geometry", "2D", "Place linked points, lines, circles and polygons on a spatial plane."),
    Geometry3D("3D Geometry", "3D", "Place and directly manipulate the existing solids and vectors."),
    Graph2D("Graph", "Graph", "Lift the existing 2D functions into a readable spatial graph plane."),
    Graph3D("3D Graph", "G3D", "Explore the current explicit, implicit or parametric surface in space."),
    CAS("CAS Objects", "CAS", "Visualize graphable CAS and Algebra objects without flattening the notebook into a static answer."),
}

data class ArMathWorkspaceScene(
    val mode: ArMathWorkspaceMode,
    val scene: SpatialRenderScene,
    val sourceObjectCount: Int,
    val visualizedObjectCount: Int,
    val diagnostics: List<String> = emptyList(),
) {
    val status: String
        get() = "$visualizedObjectCount of $sourceObjectCount linked object${if (sourceObjectCount == 1) "" else "s"} visible"
}

/**
 * A read-only projection from the canonical mathematics workspace into AR.
 * Editing remains owned by the source 2D, Graph, 3D Graph and CAS engines.
 */
object ArMathWorkspaceBridge {
    private val graph = GraphAnalysis()
    private val cyan = SpatialMaterial("AR cyan", listOf(.08f, .82f, 1f, .92f), metallic = .08f, roughness = .34f)
    private val violet = SpatialMaterial("AR violet", listOf(.63f, .36f, 1f, .88f), metallic = .08f, roughness = .38f)
    private val green = SpatialMaterial("AR green", listOf(.18f, .92f, .56f, .92f), emissive = .08f)
    private val amber = SpatialMaterial("AR amber", listOf(1f, .65f, .12f, .9f), emissive = .05f)
    private val palette = listOf(cyan, violet, green, amber)

    fun build(
        mode: ArMathWorkspaceMode,
        workspace: WorkspaceState,
        universalDocument: UniversalMathDocument? = workspace.universalMathDocument,
        surfaceDensity: Int = 24,
    ): ArMathWorkspaceScene = when (mode) {
        ArMathWorkspaceMode.Geometry2D -> geometry2D(workspace)
        ArMathWorkspaceMode.Geometry3D -> geometry3D(workspace)
        ArMathWorkspaceMode.Graph2D -> graph2D(workspace)
        ArMathWorkspaceMode.Graph3D -> graph3D(workspace, surfaceDensity)
        ArMathWorkspaceMode.CAS -> cas(universalDocument, surfaceDensity)
    }

    private fun geometry2D(workspace: WorkspaceState): ArMathWorkspaceScene {
        val base = SharedSpatialSceneBuilder.build("ar-2d")
        val pointPrimitives = workspace.points.mapIndexed { index, point ->
            SpatialPrimitive(
                id = "point-$index",
                kind = SpatialPrimitiveKind.Point,
                geometry = SpatialGeometry(listOf(point.arPoint()), pointRadius = .085),
                material = palette[index % palette.size],
                label = pointName(index),
            )
        }
        val shapePrimitives = workspace.shapes.mapIndexedNotNull { index, shape ->
            shapePrimitive(shape, workspace.points, palette[index % palette.size])
        }
        val annotations = workspace.points.mapIndexed { index, point ->
            SpatialAnnotation("point-label-$index", point.arPoint() + Vec3(.08, .08, .01), pointName(index))
        }
        val sourceCount = workspace.points.size + workspace.shapes.size
        return ArMathWorkspaceScene(
            ArMathWorkspaceMode.Geometry2D,
            base.copy(id = "ar-2d-geometry", primitives = base.primitives + shapePrimitives + pointPrimitives, annotations = annotations),
            sourceCount,
            pointPrimitives.size + shapePrimitives.size,
            if (shapePrimitives.size < workspace.shapes.size) listOf("${workspace.shapes.size - shapePrimitives.size} incomplete 2D object(s) need more defining points.") else emptyList(),
        )
    }

    private fun geometry3D(workspace: WorkspaceState): ArMathWorkspaceScene {
        val scene = SharedSpatialSceneBuilder.build(
            id = "ar-3d-geometry",
            solids = workspace.solids,
            vectors = workspace.vectors3D,
            annotations = listOf(SpatialAnnotation("ar-3d-origin", Vec3(0.0, .18, 0.0), "3D origin")),
        )
        val count = workspace.solids.size + workspace.vectors3D.size
        return ArMathWorkspaceScene(ArMathWorkspaceMode.Geometry3D, scene, count, count)
    }

    private fun graph2D(workspace: WorkspaceState): ArMathWorkspaceScene {
        val base = SharedSpatialSceneBuilder.build("ar-graph")
        val diagnostics = mutableListOf<String>()
        val curves = workspace.functions.filter { it.visible }.mapIndexedNotNull { index, function ->
            val sample = runCatching { graph.sampleDefinition(function.expression, -6.0, 6.0, 420) }
                .onFailure { diagnostics += "${function.name}: ${it.message ?: "could not be sampled"}" }
                .getOrNull()
                ?: return@mapIndexedNotNull null
            val vertices = sample.points.map { it.arPoint(z = .02 + index * .015) }
            if (vertices.size < 2) {
                diagnostics += "${function.name}: this graph type needs an AR sampler."
                return@mapIndexedNotNull null
            }
            val lines = (0 until vertices.lastIndex)
                .filterNot { it + 1 in sample.breaks }
                .map { it to it + 1 }
            SpatialPrimitive(
                id = function.id,
                kind = SpatialPrimitiveKind.Curve,
                geometry = SpatialGeometry(vertices, lines = lines, pointRadius = .035),
                material = material(function.colorKey, index),
                label = "${function.name} = ${function.expression}",
            )
        }
        val annotations = curves.mapIndexed { index, curve ->
            SpatialAnnotation("graph-label-${curve.id}", Vec3(-5.8, 5.6 - index * .35, .06), curve.label, curve.material.colorRgba)
        }
        return ArMathWorkspaceScene(
            ArMathWorkspaceMode.Graph2D,
            base.copy(id = "ar-2d-graph", primitives = base.primitives + curves, annotations = annotations),
            workspace.functions.count { it.visible },
            curves.size,
            diagnostics,
        )
    }

    private fun graph3D(workspace: WorkspaceState, density: Int): ArMathWorkspaceScene {
        val mesh = runCatching { Graph3D().mesh(workspace.surfaceExpression, density.coerceIn(12, 64).toDouble()) }
        val scene = mesh.getOrNull()?.let {
            SharedSpatialSceneBuilder.build(
                id = "ar-3d-graph",
                surface = it,
                annotations = listOf(SpatialAnnotation("surface-label", Vec3(0.0, 2.6, 0.0), workspace.surfaceExpression)),
            )
        } ?: SharedSpatialSceneBuilder.build("ar-3d-graph")
        return ArMathWorkspaceScene(
            ArMathWorkspaceMode.Graph3D,
            scene,
            1,
            if (mesh.isSuccess) 1 else 0,
            mesh.exceptionOrNull()?.message?.let { listOf(it) }.orEmpty(),
        )
    }

    private fun cas(document: UniversalMathDocument?, density: Int): ArMathWorkspaceScene {
        if (document == null) return ArMathWorkspaceScene(
            ArMathWorkspaceMode.CAS,
            SharedSpatialSceneBuilder.build("ar-cas"),
            0,
            0,
            listOf("Create or evaluate a CAS/Algebra object first."),
        )
        val sourceObjects = document.objects.values.filter {
            it.sourceView.contains("CAS", ignoreCase = true) ||
                it.kind in setOf(
                    UniversalMathKind.Point2D, UniversalMathKind.Point3D, UniversalMathKind.Vector,
                    UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction, UniversalMathKind.Surface,
                )
        }
        val primitives = mutableListOf<SpatialPrimitive>()
        val annotations = mutableListOf<SpatialAnnotation>()
        val diagnostics = mutableListOf<String>()
        sourceObjects.forEachIndexed { index, value ->
            val material = palette[index % palette.size]
            when (val payload = value.payload) {
                is UniversalMathPayload.Coordinates -> {
                    val coordinates = payload.values
                    if (coordinates.size in 2..3) {
                        val point = Vec3(coordinates[0], coordinates[1], coordinates.getOrElse(2) { 0.0 })
                        primitives += SpatialPrimitive(value.id, SpatialPrimitiveKind.Point, SpatialGeometry(listOf(point), pointRadius = .09), material, value.name)
                        annotations += SpatialAnnotation("cas-label-${value.id}", point + Vec3(.1, .1, .04), value.name, material.colorRgba)
                    }
                }
                is UniversalMathPayload.Symbolic -> when (value.kind) {
                    UniversalMathKind.Surface -> {
                        val mesh = runCatching { Graph3D().mesh(payload.source, density.coerceIn(12, 48).toDouble()) }.getOrNull()
                        if (mesh == null) diagnostics += "${value.name}: surface could not be sampled."
                        else {
                            val surface = SharedSpatialSceneBuilder.build("cas-${value.id}", surface = mesh).primitives.firstOrNull { it.id == "surface" }
                            surface?.let { primitives += it.copy(id = value.id, material = material.copy(blendMode = SpatialBlendMode.Transparent), label = value.name) }
                        }
                    }
                    UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction, UniversalMathKind.Expression -> {
                        val sample = runCatching { graph.sampleDefinition(payload.source, -6.0, 6.0, 360) }.getOrNull()
                        val vertices = sample?.points.orEmpty().map { it.arPoint(z = .03 + index * .01) }
                        if (vertices.size < 2) diagnostics += "${value.name}: symbolic result is not directly graphable."
                        else {
                            val lines = (0 until vertices.lastIndex).filterNot { it + 1 in sample!!.breaks }.map { it to it + 1 }
                            primitives += SpatialPrimitive(value.id, SpatialPrimitiveKind.Curve, SpatialGeometry(vertices, lines = lines), material, value.name)
                        }
                    }
                    else -> diagnostics += "${value.name}: linked to CAS but has no spatial representation yet."
                }
                else -> diagnostics += "${value.name}: linked to CAS but has no spatial representation yet."
            }
        }
        val base = SharedSpatialSceneBuilder.build("ar-cas")
        return ArMathWorkspaceScene(
            ArMathWorkspaceMode.CAS,
            base.copy(id = "ar-cas-objects", primitives = base.primitives + primitives, annotations = annotations),
            sourceObjects.size,
            primitives.size,
            diagnostics,
        )
    }

    private fun shapePrimitive(shape: Shape2D, points: List<Vec2>, material: SpatialMaterial): SpatialPrimitive? {
        if (!shape.visible) return null
        val defining = shape.pointIndices.mapNotNull(points::getOrNull)
        if (defining.isEmpty()) return null
        val sampled = when (shape.type) {
            Shape2DType.Line -> extendedLine(defining, ray = false)
            Shape2DType.Ray -> extendedLine(defining, ray = true)
            Shape2DType.Circle, Shape2DType.CircleThreePoints -> sampledCircle(defining)
            Shape2DType.Ellipse -> sampledEllipse(defining)
            else -> defining
        }
        if (sampled.isEmpty()) return null
        val closed = shape.type in setOf(
            Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square,
            Shape2DType.RegularPolygon, Shape2DType.Circle, Shape2DType.CircleThreePoints, Shape2DType.Ellipse,
        )
        val vertices = sampled.map { it.arPoint() }
        val lines: List<Pair<Int, Int>> = (0 until vertices.lastIndex).map { it to it + 1 } +
            if (closed && vertices.size > 2) listOf(vertices.lastIndex to 0) else emptyList()
        val triangles = if (closed && vertices.size in 3..16) (1 until vertices.lastIndex).flatMap { listOf(0, it, it + 1) } else emptyList()
        return SpatialPrimitive(
            id = shape.id,
            kind = if (triangles.isEmpty()) SpatialPrimitiveKind.Curve else SpatialPrimitiveKind.Surface,
            geometry = SpatialGeometry(vertices, triangles = triangles, lines = lines, pointRadius = .045),
            material = if (triangles.isEmpty()) material else material.copy(colorRgba = material.colorRgba.toMutableList().also { it[3] = .32f }, blendMode = SpatialBlendMode.Transparent),
            label = shape.name,
        )
    }

    private fun extendedLine(points: List<Vec2>, ray: Boolean): List<Vec2> {
        if (points.size < 2) return points
        val a = points[0]
        val b = points[1]
        val length = kotlin.math.hypot(b.x - a.x, b.y - a.y).takeIf { it > 1e-9 } ?: return points
        val direction = Vec2((b.x - a.x) / length, (b.y - a.y) / length)
        return if (ray) listOf(a, a + direction * 12.0) else listOf(a - direction * 12.0, a + direction * 12.0)
    }

    private fun sampledCircle(points: List<Vec2>): List<Vec2> {
        if (points.size < 2) return points
        val center = points[0]
        val radius = kotlin.math.hypot(points[1].x - center.x, points[1].y - center.y)
        return (0 until 72).map { step ->
            val angle = 2 * PI * step / 72
            Vec2(center.x + radius * cos(angle), center.y + radius * sin(angle))
        }
    }

    private fun sampledEllipse(points: List<Vec2>): List<Vec2> {
        if (points.size < 2) return points
        val center = points[0]
        val rx = kotlin.math.abs(points[1].x - center.x).coerceAtLeast(.1)
        val ry = points.getOrNull(2)?.let { kotlin.math.abs(it.y - center.y) }?.coerceAtLeast(.1) ?: (rx * .62)
        return (0 until 72).map { step ->
            val angle = 2 * PI * step / 72
            Vec2(center.x + rx * cos(angle), center.y + ry * sin(angle))
        }
    }

    private fun material(colorKey: String, index: Int) = when (colorKey.lowercase()) {
        "cyan" -> cyan
        "violet" -> violet
        "green" -> green
        "amber" -> amber
        else -> palette[index % palette.size]
    }

    private fun Vec2.arPoint(z: Double = 0.0) = Vec3(x, y, z)
    private fun pointName(index: Int): String = if (index < 26) ('A'.code + index).toChar().toString() else "P${index + 1}"
}
