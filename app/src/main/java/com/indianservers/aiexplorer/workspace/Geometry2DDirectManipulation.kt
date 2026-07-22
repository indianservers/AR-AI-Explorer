package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.InteractionGeometry
import com.indianservers.aiexplorer.core.Vec2
import kotlin.math.abs

enum class GeometryObjectKind { Point, Line, Ray, Segment, Circle, Conic, Polygon, Constraint, Group, Measurement, Label }
enum class GeometryHandleKind { Vertex, Endpoint, Radius, MajorAxis, MinorAxis, Translate, Rotate, Dilate, Reflect, LabelAnchor }
enum class GeometryCanvasAction { Rename, Lock, Unlock, Hide, Show, Duplicate, BringForward, SendBackward, Style, Copy, Delete, EditCoordinates }

data class GeometryObjectRef(val id: String, val kind: GeometryObjectKind, val pointIndices: List<Int> = emptyList(), val shapeIndex: Int? = null)
data class GeometrySelectionHandle(val id: String, val kind: GeometryHandleKind, val position: Vec2, val label: String)
data class GeometrySnapPreview(val proposed: Vec2, val committed: Vec2, val label: String, val visible: Boolean)
data class GeometryConstraintGlyph(val id: String, val symbol: String, val position: Vec2, val feedback: GeometryConstraintFeedback)
data class GeometryAnnotation(val id: String, val anchor: Vec2, val position: Vec2, val text: String, val draggable: Boolean = true)
data class GeometryAccessibilityNode(val id: String, val description: String, val parents: List<String>, val dependents: List<String>, val actions: List<String>)
data class GeometryRecoveryAction(val label: String, val explanation: String, val pointIndices: List<Int> = emptyList())
data class GeometryCrossViewHighlight(val geometryIds: Set<String>, val algebraIds: Set<String>, val measurementIds: Set<String>, val casSymbols: Set<String>)
data class GeometryMacroDraft(val name: String, val recordedIds: List<String> = emptyList(), val recording: Boolean = false)
data class GeometryTraceSession(val objectId: String, val samples: List<Vec2> = emptyList(), val recording: Boolean = false, val limit: Int = 2_000)

enum class ProtocolPlaybackMode { Paused, Playing }
data class GeometryProtocolPlayback(val step: Int, val size: Int, val mode: ProtocolPlaybackMode = ProtocolPlaybackMode.Paused, val isolatedId: String? = null) {
    fun play() = copy(mode = ProtocolPlaybackMode.Playing)
    fun pause() = copy(mode = ProtocolPlaybackMode.Paused)
    fun scrub(value: Int) = copy(step = value.coerceIn(0, size), mode = ProtocolPlaybackMode.Paused)
    fun tick() = if (mode == ProtocolPlaybackMode.Playing && step < size) copy(step = step + 1) else copy(mode = ProtocolPlaybackMode.Paused)
    fun isolate(id: String?) = copy(isolatedId = id, mode = ProtocolPlaybackMode.Paused)
}

/** Shared interaction policy used by touch, mouse, keyboard, stylus and accessibility surfaces. */
object Geometry2DDirectManipulation {
    fun objects(state: WorkspaceState): List<GeometryObjectRef> = buildList {
        state.points.indices.forEach { add(GeometryObjectRef("P${it + 1}", GeometryObjectKind.Point, listOf(it))) }
        state.shapes.forEachIndexed { index, shape -> add(GeometryObjectRef(shape.id, kind(shape.type), shape.pointIndices, index)) }
        state.geometryConstraints.forEach { add(GeometryObjectRef(it.id, GeometryObjectKind.Constraint, it.pointIndices)) }
        state.geometryGroups.forEach { group -> add(GeometryObjectRef(group.id, GeometryObjectKind.Group, shapeIndex = state.shapes.indexOfFirst { it.id in group.shapeIds }.takeIf { it >= 0 })) }
        state.shapes.forEach { shape -> if (shape.pointIndices.size >= 2) add(GeometryObjectRef("measure:${shape.id}", GeometryObjectKind.Measurement, shape.pointIndices, state.shapes.indexOf(shape))) }
    }

    fun boxSelect(state: WorkspaceState, first: Vec2, second: Vec2, includeIntersecting: Boolean = true): Set<Int> {
        val minimum = Vec2(minOf(first.x, second.x), minOf(first.y, second.y)); val maximum = Vec2(maxOf(first.x, second.x), maxOf(first.y, second.y))
        fun inside(point: Vec2) = point.x in minimum.x..maximum.x && point.y in minimum.y..maximum.y
        return state.shapes.indices.filterTo(linkedSetOf()) { index ->
            val points = state.shapes[index].pointIndices.mapNotNull(state.points::getOrNull)
            points.isNotEmpty() && if (includeIntersecting) points.any(::inside) || InteractionGeometry.bounds(points)?.center?.let(::inside) == true else points.all(::inside)
        }
    }

    fun handles(state: WorkspaceState, shapeIndex: Int): List<GeometrySelectionHandle> {
        val shape = state.shapes.getOrNull(shapeIndex) ?: return emptyList(); val points = shape.pointIndices.mapNotNull(state.points::getOrNull)
        if (points.isEmpty()) return emptyList()
        val handles = points.mapIndexed { index, point ->
            val handleKind = when {
                shape.type in setOf(Shape2DType.Circle, Shape2DType.CircleThreePoints) && index > 0 -> GeometryHandleKind.Radius
                shape.type == Shape2DType.Ellipse && index == 1 -> GeometryHandleKind.MajorAxis
                shape.type == Shape2DType.Ellipse && index == 2 -> GeometryHandleKind.MinorAxis
                shape.type in setOf(Shape2DType.Line, Shape2DType.Ray, Shape2DType.Segment, Shape2DType.Vector) -> GeometryHandleKind.Endpoint
                else -> GeometryHandleKind.Vertex
            }
            GeometrySelectionHandle("${shape.id}:$index", handleKind, point, "${handleKind.name.lowercase()} ${index + 1}")
        }.toMutableList()
        val bounds = InteractionGeometry.bounds(points) ?: return handles
        handles += GeometrySelectionHandle("${shape.id}:move", GeometryHandleKind.Translate, bounds.center, "move")
        handles += GeometrySelectionHandle("${shape.id}:rotate", GeometryHandleKind.Rotate, Vec2(bounds.center.x, bounds.maximum.y + maxOf(.5, bounds.height * .2)), "rotate")
        handles += GeometrySelectionHandle("${shape.id}:dilate", GeometryHandleKind.Dilate, bounds.maximum, "dilate")
        handles += GeometrySelectionHandle("${shape.id}:reflect", GeometryHandleKind.Reflect, Vec2(bounds.minimum.x, bounds.center.y), "reflect")
        return handles
    }

    fun contextActions(state: WorkspaceState, target: GeometryObjectRef): List<GeometryCanvasAction> = buildList {
        add(GeometryCanvasAction.Rename)
        val shape = target.shapeIndex?.let(state.shapes::getOrNull)
        add(if (shape?.locked == true) GeometryCanvasAction.Unlock else GeometryCanvasAction.Lock)
        add(if (shape?.visible == false) GeometryCanvasAction.Show else GeometryCanvasAction.Hide)
        addAll(listOf(GeometryCanvasAction.Duplicate, GeometryCanvasAction.BringForward, GeometryCanvasAction.SendBackward, GeometryCanvasAction.Style, GeometryCanvasAction.Copy))
        if (target.kind == GeometryObjectKind.Point) add(GeometryCanvasAction.EditCoordinates)
        add(GeometryCanvasAction.Delete)
    }

    fun snapPreview(proposed: Vec2, candidates: List<Vec2>, threshold: Double = .18): GeometrySnapPreview {
        val nearest = candidates.minByOrNull(proposed::distanceTo)
        val committed = nearest?.takeIf { proposed.distanceTo(it) <= threshold }
        return GeometrySnapPreview(proposed, committed ?: proposed, committed?.let { "Snap to (${format(it.x)}, ${format(it.y)})" } ?: "Free placement", committed != null)
    }

    fun constraintGlyphs(state: WorkspaceState): List<GeometryConstraintGlyph> = state.geometryConstraints.mapNotNull { constraint ->
        val feedback = Geometry2DInteractionEngine.evaluateConstraint(state, constraint)
        val points = constraint.pointIndices.mapNotNull(state.points::getOrNull) + constraint.shapeIds.flatMap { id -> state.shapes.firstOrNull { it.id == id }?.pointIndices.orEmpty().mapNotNull(state.points::getOrNull) }
        val anchor = InteractionGeometry.bounds(points)?.center ?: return@mapNotNull null
        GeometryConstraintGlyph(constraint.id, when (constraint.type) {
            GeometryConstraint2DType.EqualLength -> "="; GeometryConstraint2DType.Parallel -> "∥"; GeometryConstraint2DType.Perpendicular -> "⊥"
            GeometryConstraint2DType.Tangent -> "⊙"; GeometryConstraint2DType.Concentric -> "◎"; GeometryConstraint2DType.FixedLength -> "↔"; GeometryConstraint2DType.FixedAngle -> "∠"
        }, anchor, feedback)
    }

    fun avoidAnnotationCollisions(values: List<GeometryAnnotation>, minimumDistance: Double = .45): List<GeometryAnnotation> {
        val placed = mutableListOf<GeometryAnnotation>()
        values.forEach { annotation ->
            var position = annotation.position; var attempts = 0
            while (placed.any { it.position.distanceTo(position) < minimumDistance } && attempts++ < 12) position += Vec2(.18, .18)
            placed += annotation.copy(position = position)
        }
        return placed
    }

    fun accessibilityGraph(state: WorkspaceState): List<GeometryAccessibilityNode> {
        val timeline = Geometry2DInteractionEngine.protocolTimeline(state)
        val parents = timeline.entries.associate { it.id to it.parentIds }
        return timeline.entries.map { entry ->
            GeometryAccessibilityNode(entry.id, "${entry.title}. ${entry.detail}", entry.parentIds, parents.filterValues { entry.id in it }.keys.toList(), listOf("select", "inspect", "move to parent", "move to dependent"))
        }
    }

    fun movePointByKeyboard(state: WorkspaceState, pointIndex: Int, dx: Int, dy: Int, precision: Boolean): Vec2? {
        if (pointIndex !in state.points.indices || state.pointDependencies.any { it.outputIndex == pointIndex }) return null
        val amount = if (precision) .01 else .1
        return state.points[pointIndex] + Vec2(dx * amount, dy * amount)
    }

    fun recoverDegenerate(state: WorkspaceState, inputIndices: List<Int>): List<GeometryRecoveryAction> {
        val points = inputIndices.mapNotNull(state.points::getOrNull)
        val duplicates = points.indices.any { i -> points.indices.any { j -> i < j && points[i].distanceTo(points[j]) < 1e-8 } }
        val collinear = points.size >= 3 && abs((points[1] - points[0]).cross(points[2] - points[0])) < 1e-8
        return buildList {
            if (duplicates) add(GeometryRecoveryAction("Separate coincident points", "Drag the highlighted point or offset it by one precision step.", inputIndices))
            if (collinear) add(GeometryRecoveryAction("Move off the common line", "Nudge the final point perpendicular to the defining line.", inputIndices.takeLast(1)))
            add(GeometryRecoveryAction("Choose different parents", "Return to parent selection while preserving the current tool."))
            add(GeometryRecoveryAction("Create as free object", "Remove the failing dependency but keep the visible location."))
        }
    }

    fun crossViewHighlight(objectId: String): GeometryCrossViewHighlight {
        val point = objectId.removePrefix("P").toIntOrNull()
        val symbol = if (point != null) "P$point" else objectId
        return GeometryCrossViewHighlight(setOf(objectId), setOf("algebra:$objectId"), setOf("measure:$objectId"), setOf(symbol))
    }

    fun recordMacro(draft: GeometryMacroDraft, selectedIds: Collection<String>) =
        if (!draft.recording) draft else draft.copy(recordedIds = (draft.recordedIds + selectedIds).distinct())

    fun appendTrace(session: GeometryTraceSession, point: Vec2): GeometryTraceSession =
        if (!session.recording || !point.x.isFinite() || !point.y.isFinite()) session else session.copy(samples = (session.samples + point).takeLast(session.limit))

    private fun kind(type: Shape2DType) = when (type) {
        Shape2DType.Line, Shape2DType.Parallel, Shape2DType.Perpendicular, Shape2DType.AngleBisector -> GeometryObjectKind.Line
        Shape2DType.Ray -> GeometryObjectKind.Ray; Shape2DType.Segment, Shape2DType.Vector -> GeometryObjectKind.Segment
        Shape2DType.Circle, Shape2DType.CircleThreePoints, Shape2DType.Arc -> GeometryObjectKind.Circle
        Shape2DType.Ellipse -> GeometryObjectKind.Conic
        else -> GeometryObjectKind.Polygon
    }
    private fun Vec2.cross(other: Vec2) = x * other.y - y * other.x
    private fun format(value: Double) = "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}
