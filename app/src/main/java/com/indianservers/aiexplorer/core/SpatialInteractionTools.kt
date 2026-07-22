package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.hypot

enum class SpatialSubObjectType { Vertex, Edge, Face }

data class ProjectedSpatialPoint(val screen: Vec2, val depth: Double, val world: Vec3)
data class ProjectedSpatialMesh(val solidIndex: Int, val vertices: List<ProjectedSpatialPoint>, val edges: List<Pair<Int, Int>>, val faces: List<List<Int>>)
data class SpatialSubObjectHit(val solidIndex: Int, val type: SpatialSubObjectType, val index: Int, val screenDistance: Double, val depth: Double, val worldPoint: Vec3)

object SpatialSubObjectPicker {
    fun pick(meshes: List<ProjectedSpatialMesh>, target: Vec2, type: SpatialSubObjectType, tolerance: Double): SpatialSubObjectHit? =
        pickAll(meshes, target, type, tolerance).firstOrNull()

    fun pickAll(meshes: List<ProjectedSpatialMesh>, target: Vec2, type: SpatialSubObjectType, tolerance: Double): List<SpatialSubObjectHit> {
        val candidates = buildList {
            meshes.forEach { mesh -> when (type) {
                SpatialSubObjectType.Vertex -> mesh.vertices.forEachIndexed { index, vertex ->
                    add(SpatialSubObjectHit(mesh.solidIndex, type, index, vertex.screen.distanceTo(target), vertex.depth, vertex.world))
                }
                SpatialSubObjectType.Edge -> mesh.edges.forEachIndexed { index, edge ->
                    val a = mesh.vertices.getOrNull(edge.first) ?: return@forEachIndexed
                    val b = mesh.vertices.getOrNull(edge.second) ?: return@forEachIndexed
                    val projection = projectSegment(target, a.screen, b.screen)
                    add(SpatialSubObjectHit(mesh.solidIndex, type, index, projection.distance, a.depth + (b.depth - a.depth) * projection.t, a.world + (b.world - a.world) * projection.t))
                }
                SpatialSubObjectType.Face -> mesh.faces.forEachIndexed { index, face ->
                    val vertices = face.mapNotNull(mesh.vertices::getOrNull); if (vertices.size < 3) return@forEachIndexed
                    val polygon = vertices.map { it.screen }
                    val inside = pointInPolygon(target, polygon)
                    val distance = if (inside) 0.0 else polygon.indices.minOf { i -> projectSegment(target, polygon[i], polygon[(i + 1) % polygon.size]).distance }
                    val world = vertices.map { it.world }.reduce(Vec3::plus) * (1.0 / vertices.size)
                    add(SpatialSubObjectHit(mesh.solidIndex, type, index, distance, vertices.map { it.depth }.average(), world))
                }
            } }
        }
        return candidates.filter { it.screenDistance <= tolerance }.sortedWith(compareBy<SpatialSubObjectHit> { it.screenDistance }.thenBy { it.depth }.thenBy { it.solidIndex }.thenBy { it.index })
    }

    private data class SegmentProjection(val distance: Double, val t: Double)
    private fun projectSegment(point: Vec2, a: Vec2, b: Vec2): SegmentProjection {
        val d = b - a; val denominator = d.x * d.x + d.y * d.y
        val t = if (denominator < 1e-16) 0.0 else (((point - a).x * d.x + (point - a).y * d.y) / denominator).coerceIn(0.0, 1.0)
        return SegmentProjection(point.distanceTo(a + d * t), t)
    }

    private fun pointInPolygon(point: Vec2, polygon: List<Vec2>): Boolean {
        var inside = false; var j = polygon.lastIndex
        polygon.indices.forEach { i ->
            val a = polygon[i]; val b = polygon[j]
            if ((a.y > point.y) != (b.y > point.y) && point.x < (b.x - a.x) * (point.y - a.y) / ((b.y - a.y).takeUnless { abs(it) < 1e-16 } ?: 1e-16) + a.x) inside = !inside
            j = i
        }
        return inside
    }
}

enum class TransformGizmoAxis(val label: String) { X("X"), Y("Y"), Z("Z"), Uniform("XYZ") }
enum class TransformGizmoKind { Move, Rotate, Scale }
data class TransformGizmoHandle(val axis: TransformGizmoAxis, val start: Vec2, val end: Vec2, val depth: Double = 0.0)
data class TransformGizmoHit(val axis: TransformGizmoAxis, val distance: Double)

object TransformGizmoEngine {
    fun hitTest(target: Vec2, kind: TransformGizmoKind, handles: List<TransformGizmoHandle>, tolerance: Double = 18.0): TransformGizmoHit? {
        val candidates = handles.map { handle ->
            val distance = when (kind) {
                TransformGizmoKind.Move -> segmentDistance(target, handle.start, handle.end)
                TransformGizmoKind.Rotate -> abs(target.distanceTo(handle.start) - handle.start.distanceTo(handle.end))
                TransformGizmoKind.Scale -> target.distanceTo(handle.end)
            }
            TransformGizmoHit(handle.axis, distance)
        }
        return candidates.filter { it.distance <= tolerance }.minWithOrNull(compareBy<TransformGizmoHit> { it.distance }.thenBy { it.axis.ordinal })
    }

    fun constrainTranslation(raw: Vec3, axis: TransformGizmoAxis): Vec3 = when (axis) {
        TransformGizmoAxis.X -> Vec3(raw.x, 0.0, 0.0)
        TransformGizmoAxis.Y -> Vec3(0.0, raw.y, 0.0)
        TransformGizmoAxis.Z -> Vec3(0.0, 0.0, raw.z)
        TransformGizmoAxis.Uniform -> raw
    }

    fun rotationDelta(axis: TransformGizmoAxis, degrees: Double): Vec3 = when (axis) {
        TransformGizmoAxis.X -> Vec3(degrees, 0.0, 0.0)
        TransformGizmoAxis.Y -> Vec3(0.0, degrees, 0.0)
        TransformGizmoAxis.Z -> Vec3(0.0, 0.0, degrees)
        TransformGizmoAxis.Uniform -> Vec3(degrees, degrees, degrees)
    }

    private fun segmentDistance(point: Vec2, a: Vec2, b: Vec2): Double {
        val d = b - a; val denominator = d.x * d.x + d.y * d.y
        val t = if (denominator < 1e-16) 0.0 else (((point - a).x * d.x + (point - a).y * d.y) / denominator).coerceIn(0.0, 1.0)
        return point.distanceTo(a + d * t)
    }
}

data class EditableSectionPlane(val origin: Vec3 = Vec3(0.0, 0.0, 0.0), val normal: Vec3 = Vec3(0.0, 1.0, 0.0)) {
    init { require(normal.magnitude() > 1e-12) }
    val unitNormal: Vec3 get() = normal.normalized()
    val offset: Double get() = unitNormal.dot(origin)
    fun moved(distance: Double) = copy(origin = origin + unitNormal * distance)
    fun withNormal(value: Vec3) = if (value.magnitude() <= 1e-12) this else copy(normal = value.normalized())
    fun basis(): Pair<Vec3, Vec3> {
        val helper = if (abs(unitNormal.y) < .9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val first = AnalyticGeometry3D.cross(unitNormal, helper).normalized()
        return first to AnalyticGeometry3D.cross(unitNormal, first).normalized()
    }
}

data class SurfaceAnalysisHandle(val gridIndex: Int, val point: Vec3, val differential: SurfaceDifferential)

object SurfaceAnalysisHandleEngine {
    fun pick(mesh: SurfaceMesh, projectedVertices: List<Vec2>, target: Vec2, tolerance: Double = 40.0): Vec3? {
        if (projectedVertices.size != mesh.vertices.size) return null
        val index = projectedVertices.indices.minByOrNull { projectedVertices[it].distanceTo(target) } ?: return null
        return mesh.vertices[index].takeIf { projectedVertices[index].distanceTo(target) <= tolerance }
    }

    fun tangentPlaneEquation(differential: SurfaceDifferential): String {
        val n = differential.unitNormal; val p = differential.point
        val d = n.dot(p)
        return "${format(n.x)}(x−${format(p.x)}) + ${format(n.y)}(y−${format(p.y)}) + ${format(n.z)}(z−${format(p.z)}) = 0 · d=${format(d)}"
    }

    private fun format(value: Double) = "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}
