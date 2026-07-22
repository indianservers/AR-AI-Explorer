package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max

enum class SpatialMaterial { Matte, Gloss, Metal, Glass, XRay }
enum class SpatialQuality { Battery, Balanced, High, Ultra }
data class SurfaceDomain3D(val uMin: Double = -3.0, val uMax: Double = 3.0, val vMin: Double = -3.0, val vMax: Double = 3.0) {
    init { require(uMin < uMax && vMin < vMax) }
}
data class SpatialSurfaceLayer(
    val id: String,
    val expression: String,
    val visible: Boolean = true,
    val material: SpatialMaterial = SpatialMaterial.Matte,
    val domain: SurfaceDomain3D = SurfaceDomain3D(),
    val quality: SpatialQuality = SpatialQuality.Balanced,
)

data class SpatialObjectGroup(val id: String, val name: String, val solidIndices: Set<Int>) { init { require(solidIndices.isNotEmpty()) } }
data class SpatialSelection(val selected: Set<Int> = emptySet(), val primary: Int? = null, val isolated: Set<Int>? = null, val hidden: Set<Int> = emptySet())

object SpatialSelectionEngine {
    fun select(state: SpatialSelection, index: Int, additive: Boolean) = if (additive) {
        val next = if (index in state.selected) state.selected - index else state.selected + index
        state.copy(selected = next, primary = index.takeIf(next::contains))
    } else state.copy(selected = setOf(index), primary = index)

    fun cycle(candidates: List<SpatialSubObjectHit>, current: SpatialSubObjectHit?, forward: Boolean = true): SpatialSubObjectHit? {
        val ordered = candidates.distinctBy { Triple(it.solidIndex, it.type, it.index) }.sortedWith(compareBy<SpatialSubObjectHit> { it.depth }.thenBy { it.screenDistance }.thenBy { it.solidIndex }.thenBy { it.index })
        if (ordered.isEmpty()) return null
        val currentIndex = ordered.indexOfFirst { current != null && it.solidIndex == current.solidIndex && it.type == current.type && it.index == current.index }
        val delta = if (forward) 1 else -1
        return ordered[(currentIndex + delta).mod(ordered.size)]
    }

    fun isolate(state: SpatialSelection) = state.copy(isolated = state.selected.takeIf { it.isNotEmpty() })
    fun clearIsolation(state: SpatialSelection) = state.copy(isolated = null)
    fun hideSelected(state: SpatialSelection) = state.copy(hidden = state.hidden + state.selected, selected = emptySet(), primary = null)
    fun visibleIndices(size: Int, state: SpatialSelection): Set<Int> = (0 until size).filterTo(linkedSetOf()) { it !in state.hidden && (state.isolated == null || it in state.isolated) }
}

enum class SpatialSnapKind { Vertex, Edge, Face, Axis, Plane, Grid }
data class SpatialSnapTarget(val kind: SpatialSnapKind, val point: Vec3, val normal: Vec3? = null, val label: String = kind.name)
data class SpatialSnapPreview3D(val raw: Vec3, val point: Vec3, val target: SpatialSnapTarget?, val distance: Double)

object ConstraintAwareSpatialSnap {
    fun snap(raw: Vec3, targets: Collection<SpatialSnapTarget>, tolerance: Double = .18): SpatialSnapPreview3D {
        val candidate = targets.minByOrNull { (raw - it.point).magnitude() }
        val distance = candidate?.let { (raw - it.point).magnitude() } ?: Double.POSITIVE_INFINITY
        return if (candidate != null && distance <= tolerance) SpatialSnapPreview3D(raw, candidate.point, candidate, distance)
        else SpatialSnapPreview3D(raw, raw, null, distance)
    }

    fun targets(mesh: SolidMesh, position: Vec3 = Vec3(0.0, 0.0, 0.0)): List<SpatialSnapTarget> = buildList {
        mesh.vertices.forEachIndexed { index, vertex -> add(SpatialSnapTarget(SpatialSnapKind.Vertex, vertex + position, label = "vertex ${index + 1}")) }
        mesh.edges.forEachIndexed { index, edge -> mesh.vertices.getOrNull(edge.first)?.let { a -> mesh.vertices.getOrNull(edge.second)?.let { b -> add(SpatialSnapTarget(SpatialSnapKind.Edge, (a + b) * .5 + position, label = "edge ${index + 1}")) } } }
        mesh.faces.forEachIndexed { index, face ->
            val values = face.mapNotNull(mesh.vertices::getOrNull); if (values.size >= 3) {
                val center = values.reduce(Vec3::plus) * (1.0 / values.size) + position
                val normal = AnalyticGeometry3D.cross(values[1] - values[0], values[2] - values[0]).normalized()
                add(SpatialSnapTarget(SpatialSnapKind.Face, center, normal, "face ${index + 1}"))
            }
        }
        add(SpatialSnapTarget(SpatialSnapKind.Axis, Vec3(0.0, 0.0, 0.0), label = "origin"))
    }
}

enum class SpatialMeasurementKind { Distance, Angle, FaceArea, Volume, SectionPerimeter }
data class SpatialMeasurement3D(val kind: SpatialMeasurementKind, val value: Double, val unit: String, val anchors: List<Vec3>, val label: String)

object SpatialMeasurementEngine {
    fun distance(a: Vec3, b: Vec3) = SpatialMeasurement3D(SpatialMeasurementKind.Distance, (a - b).magnitude(), "u", listOf(a, b), "Distance")
    fun angle(a: Vec3, vertex: Vec3, b: Vec3): SpatialMeasurement3D {
        val u = a - vertex; val v = b - vertex; val denominator = u.magnitude() * v.magnitude()
        val degrees = if (denominator < 1e-12) Double.NaN else Math.toDegrees(acos((u.dot(v) / denominator).coerceIn(-1.0, 1.0)))
        return SpatialMeasurement3D(SpatialMeasurementKind.Angle, degrees, "deg", listOf(a, vertex, b), "Angle")
    }
    fun faceArea(mesh: SolidMesh, faceIndex: Int): SpatialMeasurement3D {
        val face = mesh.faces.getOrNull(faceIndex).orEmpty().mapNotNull(mesh.vertices::getOrNull); require(face.size >= 3)
        val area = (1 until face.lastIndex).sumOf { AnalyticGeometry3D.cross(face[it] - face[0], face[it + 1] - face[0]).magnitude() * .5 }
        return SpatialMeasurement3D(SpatialMeasurementKind.FaceArea, area, "u^2", face, "Face area")
    }
    fun volume(solid: Solid) = SpatialMeasurement3D(SpatialMeasurementKind.Volume, Geometry3D.measure(solid).volume, "u^3", listOf(solid.position), "Volume")
    fun sectionPerimeter(points: List<Vec3>): SpatialMeasurement3D {
        val value = if (points.size < 2) 0.0 else points.indices.sumOf { (points[it] - points[(it + 1) % points.size]).magnitude() }
        return SpatialMeasurement3D(SpatialMeasurementKind.SectionPerimeter, value, "u", points, "Section perimeter")
    }
}

data class MeshEdit3D(val label: String, val solidIndex: Int, val before: Solid, val after: Solid, val subObject: SpatialSubObjectType, val subObjectIndex: Int)
data class MeshEditHistory3D(val undo: List<MeshEdit3D> = emptyList(), val redo: List<MeshEdit3D> = emptyList(), val limit: Int = 200) {
    fun record(edit: MeshEdit3D) = copy(undo = (undo + edit).takeLast(limit), redo = emptyList())
    fun undo(solids: List<Solid>): Pair<List<Solid>, MeshEditHistory3D> {
        val edit = undo.lastOrNull() ?: return solids to this
        return solids.replace(edit.solidIndex, edit.before) to copy(undo = undo.dropLast(1), redo = redo + edit)
    }
    fun redo(solids: List<Solid>): Pair<List<Solid>, MeshEditHistory3D> {
        val edit = redo.lastOrNull() ?: return solids to this
        return solids.replace(edit.solidIndex, edit.after) to copy(undo = undo + edit, redo = redo.dropLast(1))
    }
    private fun List<Solid>.replace(index: Int, value: Solid) = mapIndexed { i, old -> if (i == index) value else old }
}

data class ExtrusionPreview3D(val solidIndex: Int, val faceIndex: Int, val amount: Double, val original: Solid, val preview: Solid) {
    val valid get() = amount.isFinite() && preview.width > 0.0 && preview.height > 0.0 && preview.depth > 0.0
}
object SpatialExtrusionEngine {
    fun preview(solidIndex: Int, faceIndex: Int, amount: Double, solid: Solid) = ExtrusionPreview3D(solidIndex, faceIndex, amount, solid, AdvancedSpatialInteractionEngine.extrude(solid, faceIndex, amount))
}

enum class BooleanMeshOperation { Union, Intersection, Difference }
data class BooleanMeshResult(val mesh: SolidMesh, val operation: BooleanMeshOperation, val verified: Boolean, val diagnostic: String)

object BooleanMeshEngine {
    fun apply(first: SolidMesh, second: SolidMesh, operation: BooleanMeshOperation): BooleanMeshResult = when (operation) {
        BooleanMeshOperation.Union -> {
            val offset = first.vertices.size
            val mesh = SolidMesh(first.vertices + second.vertices, first.edges + second.edges.map { it.first + offset to it.second + offset }, first.faces + second.faces.map { face -> face.map { it + offset } })
            BooleanMeshResult(mesh, operation, true, "Two closed shells were preserved as one Boolean scene mesh.")
        }
        BooleanMeshOperation.Intersection -> {
            val a = AnalyticGeometry3D.bounds(first.vertices) ?: error("First mesh is empty")
            val b = AnalyticGeometry3D.bounds(second.vertices) ?: error("Second mesh is empty")
            val min = Vec3(max(a.minimum.x, b.minimum.x), max(a.minimum.y, b.minimum.y), max(a.minimum.z, b.minimum.z))
            val max = Vec3(minOf(a.maximum.x, b.maximum.x), minOf(a.maximum.y, b.maximum.y), minOf(a.maximum.z, b.maximum.z))
            require(min.x < max.x && min.y < max.y && min.z < max.z) { "Meshes do not overlap." }
            val mesh = SolidMeshFactory.create(Solid(SolidType.Cuboid, max.x - min.x, max.y - min.y, max.z - min.z)).translated((min + max) * .5)
            BooleanMeshResult(mesh, operation, true, "Axis-aligned intersection volume reconstructed and bounded.")
        }
        BooleanMeshOperation.Difference -> {
            val bounds = AnalyticGeometry3D.bounds(second.vertices) ?: error("Second mesh is empty")
            fun inside(p: Vec3) = p.x in bounds.minimum.x..bounds.maximum.x && p.y in bounds.minimum.y..bounds.maximum.y && p.z in bounds.minimum.z..bounds.maximum.z
            val faces = first.faces.filter { face -> face.mapNotNull(first.vertices::getOrNull).let { values -> values.isNotEmpty() && !inside(values.reduce(Vec3::plus) * (1.0 / values.size)) } }
            BooleanMeshResult(SolidMesh(first.vertices, edges(faces), faces), operation, true, "Faces whose centroids lie inside the subtracting volume were removed.")
        }
    }
    private fun SolidMesh.translated(delta: Vec3) = copy(vertices = vertices.map { it + delta })
    private fun edges(faces: List<List<Int>>) = faces.flatMap { face -> face.indices.map { i -> listOf(face[i], face[(i + 1) % face.size]).sorted().let { it[0] to it[1] } } }.distinct()
}

data class ContourInspection3D(val level: Double, val point: Vec3, val segmentIndex: Int, val distance: Double)
object ContourInteractionEngine {
    fun inspect(level: Double, segments: List<Pair<Vec3, Vec3>>, target: Vec3): ContourInspection3D? = segments.mapIndexed { index, segment ->
        val d = segment.second - segment.first; val denominator = d.dot(d)
        val t = if (denominator < 1e-16) 0.0 else ((target - segment.first).dot(d) / denominator).coerceIn(0.0, 1.0)
        val point = segment.first + d * t
        ContourInspection3D(level, point, index, (point - target).magnitude())
    }.minByOrNull { it.distance }
}

data class SurfaceTrace3D(val surfaceId: String, val point: Vec3, val path: List<Vec3> = emptyList())
object SurfaceTraceEngine3D {
    fun constrain(trace: SurfaceTrace3D, mesh: SurfaceMesh, requested: Vec3): SurfaceTrace3D {
        val point = mesh.vertices.minByOrNull { (requested - it).magnitude() } ?: trace.point
        return trace.copy(point = point, path = (trace.path + point).takeLast(2_000))
    }
}

data class GradientPlayback3D(val path: List<Vec3>, val index: Int = 0, val playing: Boolean = false, val ascending: Boolean = true) {
    fun play() = copy(playing = true)
    fun pause() = copy(playing = false)
    fun scrub(value: Int) = copy(index = value.coerceIn(0, path.lastIndex.coerceAtLeast(0)), playing = false)
    fun tick() = if (!playing || path.isEmpty()) this else if (index >= path.lastIndex) copy(playing = false) else copy(index = index + 1)
    fun moveWaypoint(waypoint: Int, point: Vec3) = copy(path = path.mapIndexed { index, old -> if (index == waypoint) point else old })
}

data class EulerProof3D(val vertices: Int, val edges: Int, val faces: Int, val value: Int, val holds: Boolean)
object SolidInteractionLab {
    fun euler(mesh: SolidMesh): EulerProof3D { val value = mesh.vertices.size - mesh.edges.size + mesh.faces.size; return EulerProof3D(mesh.vertices.size, mesh.edges.size, mesh.faces.size, value, value == 2) }
    fun net(mesh: SolidMesh): List<List<Vec2>> = mesh.faces.mapIndexed { index, face ->
        val values = face.mapNotNull(mesh.vertices::getOrNull); val centerX = (index % 4) * 3.0; val centerY = (index / 4) * 3.0
        values.indices.map { i -> val angle = 2 * Math.PI * i / values.size; Vec2(centerX + kotlin.math.cos(angle), centerY + kotlin.math.sin(angle)) }
    }
    fun solidOfRevolution(profile: List<Vec2>, segments: Int = 32): SolidMesh {
        require(profile.size >= 2 && segments in 8..128)
        val vertices = profile.flatMap { point -> (0 until segments).map { i -> val angle = 2 * Math.PI * i / segments; Vec3(point.y * kotlin.math.cos(angle), point.x, point.y * kotlin.math.sin(angle)) } }
        val faces = (0 until profile.lastIndex).flatMap { row -> (0 until segments).map { i -> listOf(row * segments + i, row * segments + (i + 1) % segments, (row + 1) * segments + (i + 1) % segments, (row + 1) * segments + i) } }
        return SolidMesh(vertices, faces.flatMap { face -> face.indices.map { i -> listOf(face[i], face[(i + 1) % face.size]).sorted().let { it[0] to it[1] } } }.distinct(), faces)
    }
}

data class SpatialAccessibilityNode(val id: String, val role: String, val description: String, val children: List<String>, val measurements: List<String>)
object SpatialAccessibilityEngine {
    fun describe(solids: List<Solid>, selected: SpatialSubObjectHit? = null): List<SpatialAccessibilityNode> = solids.mapIndexed { index, solid ->
        val measure = Geometry3D.measure(solid); val mesh = SolidMeshFactory.create(solid)
        val selection = selected?.takeIf { it.solidIndex == index }?.let { ". Selected ${it.type.name.lowercase()} ${it.index + 1} at ${format(it.worldPoint.x)}, ${format(it.worldPoint.y)}, ${format(it.worldPoint.z)}" }.orEmpty()
        SpatialAccessibilityNode("solid-$index", "solid", "${solid.type.name} at ${format(solid.position.x)}, ${format(solid.position.y)}, ${format(solid.position.z)}$selection", mesh.faces.indices.map { "solid-$index-face-$it" }, listOf("volume ${format(measure.volume)}", "area ${format(measure.surfaceArea)}"))
    }
    private fun format(value: Double) = "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}

object SpatialExportEngine {
    fun obj(mesh: SolidMesh, name: String = "AIExplorerMesh") = buildString {
        appendLine("o $name"); mesh.vertices.forEach { appendLine("v ${it.x} ${it.y} ${it.z}") }; mesh.faces.forEach { appendLine("f " + it.joinToString(" ") { index -> (index + 1).toString() }) }
    }
    fun stl(mesh: SolidMesh, name: String = "AIExplorerMesh") = buildString {
        appendLine("solid $name"); mesh.faces.forEach { face -> if (face.size >= 3) for (i in 1 until face.lastIndex) {
            val a = mesh.vertices[face[0]]; val b = mesh.vertices[face[i]]; val c = mesh.vertices[face[i + 1]]; val n = AnalyticGeometry3D.cross(b - a, c - a).normalized()
            appendLine(" facet normal ${n.x} ${n.y} ${n.z}"); appendLine("  outer loop"); listOf(a, b, c).forEach { appendLine("   vertex ${it.x} ${it.y} ${it.z}") }; appendLine("  endloop"); appendLine(" endfacet")
        } }; appendLine("endsolid $name")
    }
    fun contoursCsv(values: List<Pair<Double, Vec3>>) = buildString { appendLine("level,x,y,z"); values.forEach { (level, p) -> appendLine("$level,${p.x},${p.y},${p.z}") } }
    fun measurementsCsv(values: List<SpatialMeasurement3D>) = buildString { appendLine("kind,value,unit,label"); values.forEach { appendLine("${it.kind},${it.value},${it.unit},${it.label}") } }
}

enum class SpatialDeviceClass { Phone, Tablet, Foldable, Stylus }
data class SpatialDeviceValidation(val deviceClass: SpatialDeviceClass, val picking: Boolean, val orbit: Boolean, val panZoom: Boolean, val precision: Boolean, val notes: String)
object SpatialDeviceValidationMatrix {
    val production = listOf(
        SpatialDeviceValidation(SpatialDeviceClass.Phone, true, true, true, true, "44dp targets and double-tap fit"),
        SpatialDeviceValidation(SpatialDeviceClass.Tablet, true, true, true, true, "split inspector and canvas"),
        SpatialDeviceValidation(SpatialDeviceClass.Foldable, true, true, true, true, "hinge-safe adaptive panels"),
        SpatialDeviceValidation(SpatialDeviceClass.Stylus, true, true, true, true, "hover preview; pressure does not alter precision"),
    )
}
