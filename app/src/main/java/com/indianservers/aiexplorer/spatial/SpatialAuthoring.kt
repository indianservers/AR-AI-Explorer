package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Line3D
import com.indianservers.aiexplorer.core.Plane3D
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tan

data class SurfaceDomain(
    val first: ClosedFloatingPointRange<Double> = -3.0..3.0,
    val second: ClosedFloatingPointRange<Double> = -3.0..3.0,
    val third: ClosedFloatingPointRange<Double> = -3.0..3.0,
) {
    init { require(first.start < first.endInclusive && second.start < second.endInclusive && third.start < third.endInclusive) }
}

sealed interface SurfaceDefinition3D {
    val id: String
    val domain: SurfaceDomain
    data class Explicit(override val id: String, val z: String, override val domain: SurfaceDomain = SurfaceDomain()) : SurfaceDefinition3D
    data class Implicit(override val id: String, val equation: String, override val domain: SurfaceDomain = SurfaceDomain()) : SurfaceDefinition3D
    data class Parametric(
        override val id: String,
        val x: String,
        val y: String,
        val z: String,
        val firstParameter: String = "u",
        val secondParameter: String = "v",
        override val domain: SurfaceDomain = SurfaceDomain(),
    ) : SurfaceDefinition3D
}

data class SurfaceGenerationResult(val geometry: SpatialGeometry, val sampledValues: Int, val discardedCells: Int, val watertightCandidate: Boolean)

class TypedSurfaceMesher(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun mesh(definition: SurfaceDefinition3D, density: Int = 32): SurfaceGenerationResult {
        require(density in 4..64)
        return when (definition) {
            is SurfaceDefinition3D.Explicit -> regular(definition.domain, density) { a, b -> Vec3(a, b, expressions.compile(strip(definition.z)).eval(mapOf("x" to a, "y" to b))) }
            is SurfaceDefinition3D.Parametric -> {
                require(definition.firstParameter != definition.secondParameter)
                val x = expressions.compile(strip(definition.x)); val y = expressions.compile(strip(definition.y)); val z = expressions.compile(strip(definition.z))
                regular(definition.domain, density) { a, b ->
                    val variables = mapOf(definition.firstParameter to a, definition.secondParameter to b)
                    Vec3(x.eval(variables), y.eval(variables), z.eval(variables))
                }
            }
            is SurfaceDefinition3D.Implicit -> implicit(definition, density)
        }
    }

    private fun regular(domain: SurfaceDomain, density: Int, evaluator: (Double, Double) -> Vec3): SurfaceGenerationResult {
        val values = Array((density + 1) * (density + 1)) { Vec3(Double.NaN, Double.NaN, Double.NaN) }
        for (row in 0..density) for (column in 0..density) {
            val a = interpolate(domain.first, row.toDouble() / density); val b = interpolate(domain.second, column.toDouble() / density)
            values[row * (density + 1) + column] = runCatching { evaluator(a, b) }.getOrElse { Vec3(Double.NaN, Double.NaN, Double.NaN) }
        }
        val vertices = mutableListOf<Vec3>(); val triangles = mutableListOf<Int>(); var discarded = 0
        for (row in 0 until density) for (column in 0 until density) {
            val stride = density + 1; val cell = listOf(values[row * stride + column], values[row * stride + column + 1], values[(row + 1) * stride + column], values[(row + 1) * stride + column + 1])
            if (cell.any { !it.finite() }) { discarded++; continue }
            val base = vertices.size; vertices += cell; triangles += listOf(base, base + 2, base + 1, base + 1, base + 2, base + 3)
        }
        return SurfaceGenerationResult(SpatialGeometry(vertices, triangles), values.size, discarded, watertightCandidate = false)
    }

    private fun implicit(definition: SurfaceDefinition3D.Implicit, density: Int): SurfaceGenerationResult {
        val source = definition.equation
        val compiled = if ('=' in source) {
            val parts = source.split('=', limit = 2); expressions.compile("(${parts[0]})-(${parts[1]})")
        } else expressions.compile(source)
        val size = density + 1; val points = Array(size * size * size) { Vec3(0.0, 0.0, 0.0) }; val values = DoubleArray(points.size)
        fun index(x: Int, y: Int, z: Int) = (x * size + y) * size + z
        for (ix in 0..density) for (iy in 0..density) for (iz in 0..density) {
            val point = Vec3(interpolate(definition.domain.first, ix.toDouble() / density), interpolate(definition.domain.second, iy.toDouble() / density), interpolate(definition.domain.third, iz.toDouble() / density))
            val i = index(ix, iy, iz); points[i] = point; values[i] = runCatching { compiled.eval(mapOf("x" to point.x, "y" to point.y, "z" to point.z)) }.getOrDefault(Double.NaN)
        }
        val corners = arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 0, 0), intArrayOf(1, 1, 0), intArrayOf(0, 1, 0), intArrayOf(0, 0, 1), intArrayOf(1, 0, 1), intArrayOf(1, 1, 1), intArrayOf(0, 1, 1))
        val tetrahedra = arrayOf(intArrayOf(0, 5, 1, 6), intArrayOf(0, 1, 2, 6), intArrayOf(0, 2, 3, 6), intArrayOf(0, 3, 7, 6), intArrayOf(0, 7, 4, 6), intArrayOf(0, 4, 5, 6))
        val vertices = mutableListOf<Vec3>(); val triangles = mutableListOf<Int>(); var discarded = 0
        for (ix in 0 until density) for (iy in 0 until density) for (iz in 0 until density) {
            val ids = corners.map { index(ix + it[0], iy + it[1], iz + it[2]) }
            if (ids.any { !values[it].isFinite() }) { discarded++; continue }
            tetrahedra.forEach { tetra -> polygoniseTetra(tetra.map { ids[it] }, points, values, vertices, triangles) }
        }
        return SurfaceGenerationResult(SpatialGeometry(vertices, triangles), values.size, discarded, watertightCandidate = discarded == 0)
    }

    private fun polygoniseTetra(ids: List<Int>, points: Array<Vec3>, values: DoubleArray, vertices: MutableList<Vec3>, triangles: MutableList<Int>) {
        val edges = arrayOf(0 to 1, 0 to 2, 0 to 3, 1 to 2, 1 to 3, 2 to 3); val hits = mutableListOf<Vec3>()
        edges.forEach { (a, b) ->
            val ia = ids[a]; val ib = ids[b]; val va = values[ia]; val vb = values[ib]
            if ((va <= 0 && vb >= 0) || (va >= 0 && vb <= 0)) {
                val denominator = va - vb; val t = if (abs(denominator) < 1e-14) .5 else va / denominator
                hits += points[ia] + (points[ib] - points[ia]) * t.coerceIn(0.0, 1.0)
            }
        }
        val unique = hits.distinctBy(::quantized)
        if (unique.size == 3) addTriangle(unique[0], unique[1], unique[2], vertices, triangles)
        else if (unique.size == 4) { addTriangle(unique[0], unique[1], unique[2], vertices, triangles); addTriangle(unique[0], unique[2], unique[3], vertices, triangles) }
    }

    private fun addTriangle(a: Vec3, b: Vec3, c: Vec3, vertices: MutableList<Vec3>, triangles: MutableList<Int>) {
        if (AnalyticGeometry3D.triangleArea(a, b, c) < 1e-12) return
        val base = vertices.size; vertices += listOf(a, b, c); triangles += listOf(base, base + 1, base + 2)
    }

    private fun strip(value: String) = value.substringAfter('=').trim().ifBlank { value.trim() }
    private fun interpolate(range: ClosedFloatingPointRange<Double>, t: Double) = range.start + (range.endInclusive - range.start) * t
}

object TypedSpatialSceneBuilder {
    private val material = SpatialMaterial("typed surface", listOf(.12f, .72f, 1f, .82f), metallic = .08f, roughness = .38f, blendMode = SpatialBlendMode.Transparent)
    fun build(id: String, surfaces: List<SurfaceDefinition3D>, density: Int = 32, base: SpatialRenderScene = SharedSpatialSceneBuilder.build(id)): SpatialRenderScene {
        val mesher = TypedSurfaceMesher(); val primitives = surfaces.map { definition ->
            val result = mesher.mesh(definition, density)
            SpatialPrimitive(definition.id, SpatialPrimitiveKind.Surface, result.geometry, material, definition.id)
        }
        return base.copy(id = id, primitives = base.primitives + primitives)
    }
}

sealed interface SpatialConstruction3D {
    val id: String
    val dependencies: List<String>
    data class Point(override val id: String, val position: Vec3) : SpatialConstruction3D { override val dependencies = emptyList<String>() }
    data class FreeVector(override val id: String, val components: Vec3) : SpatialConstruction3D { override val dependencies = emptyList<String>() }
    data class Vector(override val id: String, val startPoint: String, val endPoint: String) : SpatialConstruction3D { override val dependencies = listOf(startPoint, endPoint) }
    data class Line(override val id: String, val firstPoint: String, val secondPoint: String) : SpatialConstruction3D { override val dependencies = listOf(firstPoint, secondPoint) }
    data class DirectLine(override val id: String, val point: Vec3, val direction: Vec3) : SpatialConstruction3D { init { require(direction.magnitude() > 1e-12) }; override val dependencies = emptyList<String>() }
    data class Plane(override val id: String, val firstPoint: String, val secondPoint: String, val thirdPoint: String) : SpatialConstruction3D { override val dependencies = listOf(firstPoint, secondPoint, thirdPoint) }
    data class DirectPlane(override val id: String, val point: Vec3, val normal: Vec3) : SpatialConstruction3D { init { require(normal.magnitude() > 1e-12) }; override val dependencies = emptyList<String>() }
}

data class SpatialConstructionDocument(val entities: Map<String, SpatialConstruction3D> = emptyMap(), val order: List<String> = emptyList())

class SpatialConstructionEngine {
    fun add(document: SpatialConstructionDocument, entity: SpatialConstruction3D): SpatialConstructionDocument {
        require(entity.id !in document.entities); require(entity.dependencies.all { it in document.entities })
        if (entity is SpatialConstruction3D.Vector || entity is SpatialConstruction3D.Line || entity is SpatialConstruction3D.Plane) {
            require(entity.dependencies.all { document.entities[it] is SpatialConstruction3D.Point }) { "Point-based construction requires point dependencies." }
        }
        val updated = document.copy(entities = document.entities + (entity.id to entity), order = document.order + entity.id); resolve(updated); return updated
    }

    fun resolve(document: SpatialConstructionDocument): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        document.order.forEach { id ->
            val entity = document.entities.getValue(id)
            result[id] = when (entity) {
                is SpatialConstruction3D.Point -> entity.position
                is SpatialConstruction3D.FreeVector -> entity.components
                is SpatialConstruction3D.Vector -> point(result, entity.endPoint) - point(result, entity.startPoint)
                is SpatialConstruction3D.Line -> Line3D(point(result, entity.firstPoint), point(result, entity.secondPoint) - point(result, entity.firstPoint))
                is SpatialConstruction3D.DirectLine -> Line3D(entity.point, entity.direction)
                is SpatialConstruction3D.Plane -> AnalyticGeometry3D.planeThrough(point(result, entity.firstPoint), point(result, entity.secondPoint), point(result, entity.thirdPoint)) ?: error("Plane points are collinear.")
                is SpatialConstruction3D.DirectPlane -> Plane3D(entity.point, entity.normal.normalized())
            }
        }
        return result
    }

    private fun point(values: Map<String, Any>, id: String) = values[id] as? Vec3 ?: error("'$id' is not a point.")
}

enum class MeshSubObjectKind { Vertex, Edge, Face }
data class MeshSubSelection(val kind: MeshSubObjectKind, val indices: Set<Int>)
sealed interface MeshConstraint3D {
    data class LockedVertices(val indices: Set<Int>) : MeshConstraint3D
    data class FixedDistance(val first: Int, val second: Int, val distance: Double) : MeshConstraint3D { init { require(distance >= 0) } }
    data class PlanarVertices(val indices: Set<Int>, val plane: Plane3D) : MeshConstraint3D
    data class Extrusion(val direction: Vec3, val distance: Double) : MeshConstraint3D { init { require(direction.magnitude() > 1e-12) } }
}
data class EditableSpatialMesh(val vertices: List<Vec3>, val triangles: List<Int>, val constraints: List<MeshConstraint3D> = emptyList()) {
    init { require(triangles.size % 3 == 0 && triangles.all { it in vertices.indices }) }
}
data class MeshEditResult(val mesh: EditableSpatialMesh, val createdVertices: Set<Int>, val createdFaces: Set<Int>)

object SpatialMeshEditor {
    fun move(mesh: EditableSpatialMesh, selection: MeshSubSelection, delta: Vec3): EditableSpatialMesh {
        val selectedVertices = when (selection.kind) {
            MeshSubObjectKind.Vertex -> selection.indices
            MeshSubObjectKind.Face -> selection.indices.flatMap { face -> mesh.triangles.drop(face * 3).take(3) }.toSet()
            MeshSubObjectKind.Edge -> edges(mesh).let { values -> selection.indices.flatMap { listOf(values[it].first, values[it].second) } }.toSet()
        }
        val locked = mesh.constraints.filterIsInstance<MeshConstraint3D.LockedVertices>().flatMap { it.indices }.toSet()
        val moved = mesh.copy(vertices = mesh.vertices.mapIndexed { index, point -> if (index in selectedVertices && index !in locked) point + delta else point })
        return enforce(moved)
    }

    fun extrude(mesh: EditableSpatialMesh, faceIndices: Set<Int>, requestedDistance: Double, requestedDirection: Vec3? = null): MeshEditResult {
        require(faceIndices.isNotEmpty() && faceIndices.all { it * 3 + 2 < mesh.triangles.size })
        val selectedTriangles = faceIndices.map { mesh.triangles.drop(it * 3).take(3) }; val selectedVertices = selectedTriangles.flatten().toSet()
        val locked = mesh.constraints.filterIsInstance<MeshConstraint3D.LockedVertices>().flatMap { it.indices }.toSet(); require((selectedVertices intersect locked).isEmpty()) { "Locked vertices cannot be extruded." }
        val extrusion = mesh.constraints.filterIsInstance<MeshConstraint3D.Extrusion>().lastOrNull(); val distance = extrusion?.distance ?: requestedDistance
        val direction = (extrusion?.direction ?: requestedDirection ?: averageNormal(mesh, selectedTriangles)).normalized(); require(direction.magnitude() > 1e-12)
        val mapping = selectedVertices.associateWith { mesh.vertices.size + selectedVertices.sorted().indexOf(it) }; val addedVertices = selectedVertices.sorted().map { mesh.vertices[it] + direction * distance }
        val resultTriangles = mesh.triangles.chunked(3).filterIndexed { index, _ -> index !in faceIndices }.flatten().toMutableList(); val createdFaces = linkedSetOf<Int>()
        selectedTriangles.forEach { triangle -> createdFaces += resultTriangles.size / 3; resultTriangles += triangle.map { mapping.getValue(it) } }
        val counts = linkedMapOf<Pair<Int, Int>, Int>(); selectedTriangles.forEach { triangle -> listOf(triangle[0] to triangle[1], triangle[1] to triangle[2], triangle[2] to triangle[0]).forEach { edge -> val key = if (edge.first < edge.second) edge else edge.second to edge.first; counts[key] = counts.getOrDefault(key, 0) + 1 } }
        counts.filterValues { it == 1 }.keys.forEach { (a, b) ->
            val aa = mapping.getValue(a); val bb = mapping.getValue(b); createdFaces += resultTriangles.size / 3; resultTriangles += listOf(a, b, bb, a, bb, aa)
        }
        val created = mapping.values.toSet(); val result = enforce(EditableSpatialMesh(mesh.vertices + addedVertices, resultTriangles, mesh.constraints))
        return MeshEditResult(result, created, createdFaces)
    }

    fun splitEdge(mesh: EditableSpatialMesh, edgeIndex: Int, ratio: Double = .5): MeshEditResult {
        val edge = edges(mesh).getOrNull(edgeIndex) ?: error("Unknown edge index $edgeIndex."); val t = ratio.coerceIn(1e-6, 1 - 1e-6)
        val newVertex = mesh.vertices[edge.first] + (mesh.vertices[edge.second] - mesh.vertices[edge.first]) * t; val newIndex = mesh.vertices.size; val triangles = mutableListOf<Int>(); val createdFaces = linkedSetOf<Int>()
        mesh.triangles.chunked(3).forEach { triangle ->
            val position = triangle.indices.firstOrNull { index -> triangle[index] == edge.first && triangle[(index + 1) % 3] == edge.second || triangle[index] == edge.second && triangle[(index + 1) % 3] == edge.first }
            if (position == null) triangles += triangle else {
                val a = triangle[position]; val b = triangle[(position + 1) % 3]; val c = triangle[(position + 2) % 3]
                createdFaces += triangles.size / 3; triangles += listOf(a, newIndex, c); createdFaces += triangles.size / 3; triangles += listOf(newIndex, b, c)
            }
        }
        return MeshEditResult(enforce(mesh.copy(vertices = mesh.vertices + newVertex, triangles = triangles)), setOf(newIndex), createdFaces)
    }

    fun deleteFaces(mesh: EditableSpatialMesh, faceIndices: Set<Int>): EditableSpatialMesh {
        require(faceIndices.all { it >= 0 && it * 3 + 2 < mesh.triangles.size })
        return mesh.copy(triangles = mesh.triangles.chunked(3).filterIndexed { index, _ -> index !in faceIndices }.flatten())
    }

    fun edges(mesh: EditableSpatialMesh): List<Pair<Int, Int>> = mesh.triangles.chunked(3).flatMap { listOf(it[0] to it[1], it[1] to it[2], it[2] to it[0]) }.map { if (it.first < it.second) it else it.second to it.first }.distinct()

    fun enforce(mesh: EditableSpatialMesh, iterations: Int = 6): EditableSpatialMesh {
        var vertices = mesh.vertices
        repeat(iterations) {
            mesh.constraints.forEach { constraint -> when (constraint) {
                is MeshConstraint3D.LockedVertices -> Unit
                is MeshConstraint3D.FixedDistance -> {
                    val a = vertices[constraint.first]; val b = vertices[constraint.second]; val delta = b - a; val direction = delta.normalized().takeIf { it.magnitude() > 1e-12 } ?: Vec3(1.0, 0.0, 0.0)
                    vertices = vertices.replace(constraint.second, a + direction * constraint.distance)
                }
                is MeshConstraint3D.PlanarVertices -> vertices = vertices.mapIndexed { index, point -> if (index in constraint.indices) AnalyticGeometry3D.project(constraint.plane, point) else point }
                is MeshConstraint3D.Extrusion -> Unit
            } }
        }
        return mesh.copy(vertices = vertices)
    }

    private fun averageNormal(mesh: EditableSpatialMesh, triangles: List<List<Int>>): Vec3 = triangles.map { AnalyticGeometry3D.cross(mesh.vertices[it[1]] - mesh.vertices[it[0]], mesh.vertices[it[2]] - mesh.vertices[it[0]]).normalized() }.reduce(Vec3::plus).normalized()
    private fun List<Vec3>.replace(index: Int, value: Vec3) = toMutableList().apply { this[index] = value }.toList()
}

data class SectionLoop3D(val points: List<Vec3>, val area: Double, val perimeter: Double, val closed: Boolean)
data class SpatialMeasurement3D(val kind: String, val value: Double, val unit: String, val points: List<Vec3>, val explanation: String)

object SpatialAnalysisTools3D {
    fun crossSection(geometry: SpatialGeometry, plane: Plane3D, tolerance: Double = 1e-8): List<SectionLoop3D> {
        val segments = mutableListOf<Pair<Vec3, Vec3>>()
        geometry.triangles.chunked(3).filter { it.size == 3 }.forEach { triangle ->
            val points = triangle.map { geometry.vertices[it] }; val hits = mutableListOf<Vec3>()
            listOf(0 to 1, 1 to 2, 2 to 0).forEach { (a, b) ->
                val first = points[a]; val second = points[b]; val da = AnalyticGeometry3D.signedDistance(plane, first); val db = AnalyticGeometry3D.signedDistance(plane, second)
                if (abs(da) <= tolerance) hits += first
                if (da * db < -tolerance * tolerance) hits += first + (second - first) * (da / (da - db))
            }
            val unique = hits.distinctBy(::quantized); if (unique.size >= 2) segments += unique[0] to unique[1]
        }
        return chainSegments(segments, plane, tolerance)
    }

    fun linePlane(line: Line3D, plane: Plane3D) = AnalyticGeometry3D.intersect(line, plane)
    fun planePlane(first: Plane3D, second: Plane3D) = AnalyticGeometry3D.planeIntersection(first, second)
    fun rayMesh(ray: SpatialRay, geometry: SpatialGeometry): List<Vec3> {
        val direction = ray.direction.normalized(); require(direction.magnitude() > 1e-12)
        return geometry.triangles.chunked(3).filter { it.size == 3 }.mapNotNull { triangle ->
            rayTriangle(ray.origin, direction, geometry.vertices[triangle[0]], geometry.vertices[triangle[1]], geometry.vertices[triangle[2]])?.let { ray.origin + direction * it }
        }.distinctBy(::quantized).sortedBy { (it - ray.origin).magnitude() }
    }
    fun project(point: Vec3, line: Line3D): Vec3 { val d = line.direction; return line.point + d * ((point - line.point).dot(d) / d.dot(d)) }
    fun project(point: Vec3, plane: Plane3D) = AnalyticGeometry3D.project(plane, point)

    fun project(point: Vec3, geometry: SpatialGeometry): Vec3? {
        var closest: Vec3? = null; var distance = Double.POSITIVE_INFINITY
        geometry.triangles.chunked(3).filter { it.size == 3 }.forEach { triangle ->
            val candidate = closestPointOnTriangle(point, geometry.vertices[triangle[0]], geometry.vertices[triangle[1]], geometry.vertices[triangle[2]]); val d = (candidate - point).magnitude()
            if (d < distance) { distance = d; closest = candidate }
        }
        return closest
    }

    fun distance(first: Vec3, second: Vec3) = SpatialMeasurement3D("distance", (second - first).magnitude(), "units", listOf(first, second), "Euclidean distance in three dimensions.")
    fun angle(first: Vec3, vertex: Vec3, second: Vec3): SpatialMeasurement3D {
        val a = first - vertex; val b = second - vertex; val degrees = Math.toDegrees(acos((a.dot(b) / max(1e-15, a.magnitude() * b.magnitude())).coerceIn(-1.0, 1.0)))
        return SpatialMeasurement3D("angle", degrees, "degrees", listOf(first, vertex, second), "Angle from the normalized dot product.")
    }
    fun triangleArea(first: Vec3, second: Vec3, third: Vec3) = SpatialMeasurement3D("area", AnalyticGeometry3D.triangleArea(first, second, third), "units^2", listOf(first, second, third), "Half the magnitude of the cross product.")
    fun meshArea(geometry: SpatialGeometry) = geometry.triangles.chunked(3).filter { it.size == 3 }.sumOf { AnalyticGeometry3D.triangleArea(geometry.vertices[it[0]], geometry.vertices[it[1]], geometry.vertices[it[2]]) }
    fun signedVolume(geometry: SpatialGeometry) = geometry.triangles.chunked(3).filter { it.size == 3 }.sumOf { geometry.vertices[it[0]].dot(AnalyticGeometry3D.cross(geometry.vertices[it[1]], geometry.vertices[it[2]])) / 6.0 }
    fun volume(geometry: SpatialGeometry) = abs(signedVolume(geometry))

    private fun chainSegments(source: List<Pair<Vec3, Vec3>>, plane: Plane3D, tolerance: Double): List<SectionLoop3D> {
        val remaining = source.toMutableList(); val loops = mutableListOf<SectionLoop3D>()
        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0); val points = mutableListOf(first.first, first.second); var extended = true
            while (extended) {
                extended = false; val end = points.last(); val index = remaining.indexOfFirst { (it.first - end).magnitude() <= tolerance * 10 || (it.second - end).magnitude() <= tolerance * 10 }
                if (index >= 0) { val segment = remaining.removeAt(index); points += if ((segment.first - end).magnitude() <= tolerance * 10) segment.second else segment.first; extended = true }
            }
            val closed = (points.first() - points.last()).magnitude() <= tolerance * 10; val clean = if (closed) points.dropLast(1) else points
            val perimeter = clean.indices.sumOf { index -> if (index == clean.lastIndex && !closed) 0.0 else (clean[(index + 1) % clean.size] - clean[index]).magnitude() }
            loops += SectionLoop3D(clean, if (closed) polygonArea(clean, plane.normal) else 0.0, perimeter, closed)
        }
        return loops
    }

    private fun polygonArea(points: List<Vec3>, normal: Vec3): Double {
        if (points.size < 3) return 0.0; var sum = Vec3(0.0, 0.0, 0.0)
        points.indices.forEach { index -> sum += AnalyticGeometry3D.cross(points[index], points[(index + 1) % points.size]) }
        return abs(sum.dot(normal.normalized())) * .5
    }

    private fun closestPointOnTriangle(p: Vec3, a: Vec3, b: Vec3, c: Vec3): Vec3 {
        val ab = b - a; val ac = c - a; val ap = p - a; val d1 = ab.dot(ap); val d2 = ac.dot(ap); if (d1 <= 0 && d2 <= 0) return a
        val bp = p - b; val d3 = ab.dot(bp); val d4 = ac.dot(bp); if (d3 >= 0 && d4 <= d3) return b
        val vc = d1 * d4 - d3 * d2; if (vc <= 0 && d1 >= 0 && d3 <= 0) return a + ab * (d1 / (d1 - d3))
        val cp = p - c; val d5 = ab.dot(cp); val d6 = ac.dot(cp); if (d6 >= 0 && d5 <= d6) return c
        val vb = d5 * d2 - d1 * d6; if (vb <= 0 && d2 >= 0 && d6 <= 0) return a + ac * (d2 / (d2 - d6))
        val va = d3 * d6 - d5 * d4; if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) return b + (c - b) * ((d4 - d3) / ((d4 - d3) + (d5 - d6)))
        val denominator = 1.0 / (va + vb + vc); val v = vb * denominator; val w = vc * denominator; return a + ab * v + ac * w
    }

    private fun rayTriangle(origin: Vec3, direction: Vec3, a: Vec3, b: Vec3, c: Vec3): Double? {
        val edge1 = b - a; val edge2 = c - a; val h = AnalyticGeometry3D.cross(direction, edge2); val determinant = edge1.dot(h); if (abs(determinant) < 1e-12) return null
        val inverse = 1.0 / determinant; val s = origin - a; val u = inverse * s.dot(h); if (u !in 0.0..1.0) return null
        val q = AnalyticGeometry3D.cross(s, edge1); val v = inverse * direction.dot(q); if (v < 0 || u + v > 1) return null
        return (inverse * edge2.dot(q)).takeIf { it >= 0 }
    }
}

enum class SpatialRenderBackend { GPU, CPU }
data class SpatialGpuCapability(val openGlEsMajor: Int, val openGlEsMinor: Int, val supportsDerivatives: Boolean, val maximumTextureSize: Int, val blacklisted: Boolean = false)
data class SpatialRendererDecision(val backend: SpatialRenderBackend, val quality: RenderQuality, val surfaceDensity: Int, val antialiasingSamples: Int, val shadows: Boolean, val reason: String)

object SpatialRendererQualitySelector {
    fun choose(capability: SpatialGpuCapability?, thermal: ThermalLevel, averageFrameMillis: Double, batterySaver: Boolean = false, forceCpu: Boolean = false): SpatialRendererDecision {
        val policy = SpatialPerformanceManager.policy(thermal, averageFrameMillis, batterySaver)
        val gpuReady = !forceCpu && capability != null && !capability.blacklisted && capability.openGlEsMajor >= 3 && capability.supportsDerivatives && capability.maximumTextureSize >= 2048
        val samples = when (policy.quality) { RenderQuality.Ultra -> 4; RenderQuality.High -> 4; RenderQuality.Balanced -> 2; RenderQuality.Low, RenderQuality.Safety -> 1 }
        return SpatialRendererDecision(if (gpuReady) SpatialRenderBackend.GPU else SpatialRenderBackend.CPU, policy.quality, if (gpuReady) policy.surfaceDensity else min(policy.surfaceDensity, 20), samples, gpuReady && policy.shadows, if (gpuReady) "OpenGL ES renderer selected." else "Deterministic CPU fallback selected.")
    }
}

data class SpatialCameraState(val eye: Vec3, val target: Vec3, val up: Vec3 = Vec3(0.0, 1.0, 0.0), val fieldOfViewDegrees: Double = 45.0, val near: Double = .05, val far: Double = 100.0)
data class CpuProjectedTriangle(val primitiveId: String, val points: List<Vec2>, val depth: Double, val colorRgba: List<Float>)
data class CpuRenderPlan(val triangles: List<CpuProjectedTriangle>, val width: Int, val height: Int)

object CpuSpatialRenderer {
    fun compile(scene: SpatialRenderScene, camera: SpatialCameraState, width: Int, height: Int): CpuRenderPlan {
        require(width > 0 && height > 0); val valid = SpatialCameraValidator.validate(camera).camera; val forward = (valid.target - valid.eye).normalized(); val right = AnalyticGeometry3D.cross(forward, valid.up).normalized(); val up = AnalyticGeometry3D.cross(right, forward).normalized(); val scale = 1.0 / tan(valid.fieldOfViewDegrees * PI / 360)
        fun project(point: Vec3): Pair<Vec2, Double>? { val d = point - valid.eye; val depth = d.dot(forward); if (depth !in valid.near..valid.far) return null; val x = d.dot(right) * scale / depth; val y = d.dot(up) * scale / depth; return Vec2((x + 1) * width * .5, (1 - y) * height * .5) to depth }
        val triangles = mutableListOf<CpuProjectedTriangle>()
        scene.primitives.filter { it.visible }.forEach { primitive -> primitive.geometry.triangles.chunked(3).filter { it.size == 3 }.forEach { triangle ->
            val projected = triangle.map { project(primitive.geometry.vertices[it]) }; if (projected.all { it != null }) triangles += CpuProjectedTriangle(primitive.id, projected.map { it!!.first }, projected.map { it!!.second }.average(), primitive.material.colorRgba)
        } }
        return CpuRenderPlan(triangles.sortedByDescending { it.depth }, width, height)
    }
}

data class CameraValidation(val camera: SpatialCameraState, val corrected: Boolean, val diagnostics: List<String>)
object SpatialCameraValidator {
    fun validate(camera: SpatialCameraState): CameraValidation {
        val diagnostics = mutableListOf<String>(); var target = camera.target; var up = camera.up
        if ((target - camera.eye).magnitude() < 1e-6) { target = camera.eye + Vec3(0.0, 0.0, -1.0); diagnostics += "Camera target was separated from the eye." }
        val forward = (target - camera.eye).normalized(); if (AnalyticGeometry3D.cross(forward, up).magnitude() < 1e-6) { up = if (abs(forward.y) < .9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0); diagnostics += "Camera up vector was repaired." }
        val fov = camera.fieldOfViewDegrees.coerceIn(15.0, 100.0); val near = camera.near.coerceIn(.001, 10.0); val far = max(camera.far, near + .1)
        if (fov != camera.fieldOfViewDegrees || near != camera.near || far != camera.far) diagnostics += "Camera clipping or field of view was clamped."
        val result = camera.copy(target = target, up = up.normalized(), fieldOfViewDegrees = fov, near = near, far = far); return CameraValidation(result, result != camera, diagnostics)
    }
}

enum class SpatialScreenClass { Phone, Tablet }
enum class SpatialInputDevice { Touch, Stylus, Mouse }
enum class SpatialGestureIntent { Select, Orbit, Pan, Dolly, MoveSubObject, ExtrudeFace, PrecisionMeasure }
data class SpatialGesture(val pointers: Int, val drag: Vec2 = Vec2(0.0, 0.0), val scale: Double = 1.0, val barrelButton: Boolean = false, val pressure: Double = 0.0)
data class SpatialGestureDecision(val intent: SpatialGestureIntent, val precisionMultiplier: Double, val consumesViewport: Boolean)

object SpatialGesturePolicy3D {
    fun decide(screen: SpatialScreenClass, device: SpatialInputDevice, gesture: SpatialGesture, selected: MeshSubObjectKind? = null): SpatialGestureDecision {
        require(gesture.pointers >= 1); val precision = when { device == SpatialInputDevice.Stylus -> if (gesture.pressure > .6) .2 else .35; screen == SpatialScreenClass.Tablet -> .65; else -> 1.0 }
        val intent = when {
            device == SpatialInputDevice.Stylus && gesture.barrelButton && selected == MeshSubObjectKind.Face -> SpatialGestureIntent.ExtrudeFace
            device == SpatialInputDevice.Stylus && selected != null -> SpatialGestureIntent.MoveSubObject
            gesture.pointers >= 2 && abs(gesture.scale - 1.0) > .01 -> SpatialGestureIntent.Dolly
            gesture.pointers >= 2 -> SpatialGestureIntent.Pan
            device == SpatialInputDevice.Mouse && gesture.barrelButton -> SpatialGestureIntent.Pan
            gesture.drag.x != 0.0 || gesture.drag.y != 0.0 -> SpatialGestureIntent.Orbit
            else -> SpatialGestureIntent.Select
        }
        return SpatialGestureDecision(intent, precision, intent in setOf(SpatialGestureIntent.Orbit, SpatialGestureIntent.Pan, SpatialGestureIntent.Dolly))
    }
}

data class SpatialDeviceValidationCase(val screen: SpatialScreenClass, val input: SpatialInputDevice, val cameraValid: Boolean, val pickingTolerance: Double, val supportedIntents: Set<SpatialGestureIntent>)
object SpatialDeviceValidationMatrix {
    fun cases(): List<SpatialDeviceValidationCase> = SpatialScreenClass.entries.flatMap { screen -> SpatialInputDevice.entries.map { input ->
        val tolerance = when (input) { SpatialInputDevice.Stylus -> .015; SpatialInputDevice.Mouse -> .02; SpatialInputDevice.Touch -> if (screen == SpatialScreenClass.Phone) .06 else .045 }
        val intents = buildSet { addAll(setOf(SpatialGestureIntent.Select, SpatialGestureIntent.Orbit, SpatialGestureIntent.Pan, SpatialGestureIntent.Dolly)); if (input == SpatialInputDevice.Stylus) addAll(setOf(SpatialGestureIntent.MoveSubObject, SpatialGestureIntent.ExtrudeFace, SpatialGestureIntent.PrecisionMeasure)) }
        SpatialDeviceValidationCase(screen, input, SpatialCameraValidator.validate(SpatialCameraState(Vec3(0.0, 0.0, 4.0), Vec3(0.0, 0.0, 0.0))).diagnostics.isEmpty(), tolerance, intents)
    } }
}

private fun Vec3.finite() = x.isFinite() && y.isFinite() && z.isFinite()
private fun quantized(value: Vec3) = Triple((value.x * 1e7).toLong(), (value.y * 1e7).toLong(), (value.z * 1e7).toLong())
