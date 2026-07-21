package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class GeometryContextTool(
    val toolName: String,
    val label: String,
    val category: String,
    val pointIndices: List<Int>,
    val enabled: Boolean,
    val reason: String,
)

data class GeometryInspectorSnapshot(
    val title: String,
    val kind: String,
    val properties: List<Pair<String, String>>,
    val parentIds: List<String>,
    val dependentIds: List<String>,
    val tools: List<GeometryContextTool>,
)

enum class ConstraintFeedbackLevel { Satisfied, NearlySatisfied, Violated, Invalid }

data class GeometryConstraintFeedback(
    val constraint: GeometryConstraint2D,
    val level: ConstraintFeedbackLevel,
    val residual: Double?,
    val statement: String,
    val guidance: String,
)

data class GeometryConstraintSuggestion(
    val label: String,
    val constraint: GeometryConstraint2D,
    val preview: GeometryConstraintFeedback,
)

enum class GeometryProtocolStatus { Complete, Current, Future, Blocked }

data class GeometryProtocolEntry(
    val id: String,
    val title: String,
    val detail: String,
    val parentIds: List<String>,
    val status: GeometryProtocolStatus,
)

data class GeometryProtocolTimeline(
    val entries: List<GeometryProtocolEntry>,
    val visibleIds: Set<String>,
    val focusedId: String?,
    val dependencyChain: Set<String>,
    val blockedBy: Map<String, Set<String>>,
)

object Geometry2DInteractionEngine {
    private val linearTypes = setOf(Shape2DType.Line, Shape2DType.Segment, Shape2DType.Ray, Shape2DType.Vector, Shape2DType.Parallel, Shape2DType.Perpendicular)
    private val circleTypes = setOf(Shape2DType.Circle, Shape2DType.CircleThreePoints)

    fun selectedPointIndices(state: WorkspaceState, selectedPoint: Int, selectedShapes: Set<Int>): List<Int> = buildList {
        if (selectedPoint in state.points.indices) add(selectedPoint)
        selectedShapes.sorted().mapNotNull(state.shapes::getOrNull).forEach { addAll(it.pointIndices) }
    }.distinct()

    fun inspect(state: WorkspaceState, selectedPoint: Int, selectedShapes: Set<Int>): GeometryInspectorSnapshot {
        val shapeValues = selectedShapes.sorted().mapNotNull(state.shapes::getOrNull)
        val points = selectedPointIndices(state, selectedPoint, selectedShapes)
        val title = when {
            shapeValues.size == 1 -> shapeValues.single().name
            shapeValues.size > 1 -> "${shapeValues.size} selected objects"
            selectedPoint in state.points.indices -> "Point ${pointId(selectedPoint)}"
            else -> "No selection"
        }
        val kind = when {
            shapeValues.size == 1 -> shapeValues.single().type.name
            shapeValues.size > 1 -> "Multi-selection"
            selectedPoint in state.points.indices -> if (state.pointDependencies.any { it.outputIndex == selectedPoint }) "Dependent point" else "Free point"
            else -> "Canvas"
        }
        val properties = buildList {
            if (selectedPoint in state.points.indices && shapeValues.isEmpty()) {
                val p = state.points[selectedPoint]; add("Coordinates" to "(${format(p.x)}, ${format(p.y)})")
                add("Freedom" to if (state.pointDependencies.any { it.outputIndex == selectedPoint }) "Dependent" else "Free")
            }
            if (shapeValues.size == 1) addAll(shapeProperties(state, shapeValues.single()))
            if (shapeValues.size > 1) {
                add("Objects" to shapeValues.joinToString { it.name })
                add("Control points" to points.size.toString())
            }
        }
        val parents = points.flatMap { index -> state.pointDependencies.firstOrNull { it.outputIndex == index }?.inputIndices.orEmpty() }.distinct().map(::pointId)
        val dependentPoints = state.pointDependencies.filter { dependency -> dependency.inputIndices.any { it in points } }.map { pointId(it.outputIndex) }
        val dependentShapes = state.shapes.filter { shape -> shape.pointIndices.any { it in points } && shape !in shapeValues }.map { it.id }
        return GeometryInspectorSnapshot(title, kind, properties, parents, (dependentPoints + dependentShapes).distinct(), contextualTools(state, selectedPoint, selectedShapes))
    }

    fun contextualTools(state: WorkspaceState, selectedPoint: Int, selectedShapes: Set<Int>): List<GeometryContextTool> {
        val shapes = selectedShapes.sorted().mapNotNull(state.shapes::getOrNull)
        val points = selectedPointIndices(state, selectedPoint, selectedShapes)
        val result = linkedMapOf<String, GeometryContextTool>()
        fun offer(name: String, label: String, category: String, required: Int, supplied: List<Int> = points, reason: String) {
            result[name] = GeometryContextTool(name, label, category, supplied.take(required), supplied.size >= required, reason)
        }
        offer("Segment", "Segment", "Connect", 2, reason = "Connect two selected points")
        offer("Line", "Line", "Connect", 2, reason = "Extend through two selected points")
        offer("Ray", "Ray", "Connect", 2, reason = "Start at the first selected point")
        offer("Vector", "Vector", "Connect", 2, reason = "Create a directed displacement")
        offer("Midpoint", "Midpoint", "Dependent point", 2, reason = "Depends on both endpoints")
        offer("Circle", "Circle", "Circle", 2, reason = "First point is the centre")
        offer("Triangle", "Triangle", "Polygon", 3, reason = "Use three selected points")
        offer("CircleThreePoints", "Circle through 3", "Circle", 3, reason = "Circumcircle through three points")
        offer("Centroid", "Centroid", "Triangle centre", 3, reason = "Intersection of the medians")
        offer("Circumcenter", "Circumcenter", "Triangle centre", 3, reason = "Equidistant from the vertices")
        offer("Incenter", "Incenter", "Triangle centre", 3, reason = "Equidistant from the sides")
        offer("Orthocenter", "Orthocenter", "Triangle centre", 3, reason = "Intersection of the altitudes")
        if (shapes.size == 1 && shapes.single().type in linearTypes && selectedPoint in state.points.indices) {
            val base = shapes.single().pointIndices.take(2) + selectedPoint
            offer("Parallel", "Parallel through point", "Dependent line", 3, base, "Uses the selected line as its direction")
            offer("Perpendicular", "Perpendicular through point", "Dependent line", 3, base, "Uses a 90° direction to the selected line")
        }
        if (shapes.size == 2 && shapes.all { it.type in linearTypes }) {
            offer("Intersection", "Intersection", "Dependent point", 4, shapes.flatMap { it.pointIndices.take(2) }, "Recomputes when either line moves")
        }
        if (points.size >= 4) offer("Polygon", "Polygon", "Polygon", 4, reason = "Create from the first four selected points")
        return result.values.toList()
    }

    fun constraintSuggestions(state: WorkspaceState, selectedPoint: Int, selectedShapes: Set<Int>): List<GeometryConstraintSuggestion> {
        val shapes = selectedShapes.sorted().mapNotNull(state.shapes::getOrNull)
        val points = selectedPointIndices(state, selectedPoint, selectedShapes)
        val seed = state.geometryConstraints.size + 1
        val values = mutableListOf<GeometryConstraint2D>()
        if (shapes.size == 2 && shapes.all { it.pointIndices.size >= 2 && it.type in linearTypes }) {
            val indices = shapes.flatMap { it.pointIndices.take(2) }
            values += GeometryConstraint2D("constraint-$seed-equal", GeometryConstraint2DType.EqualLength, indices)
            values += GeometryConstraint2D("constraint-$seed-parallel", GeometryConstraint2DType.Parallel, indices)
            values += GeometryConstraint2D("constraint-$seed-perpendicular", GeometryConstraint2DType.Perpendicular, indices)
        }
        if (shapes.size == 2 && shapes.all { it.type in circleTypes }) {
            values += GeometryConstraint2D("constraint-$seed-concentric", GeometryConstraint2DType.Concentric, shapeIds = shapes.map { it.id })
            values += GeometryConstraint2D("constraint-$seed-tangent", GeometryConstraint2DType.Tangent, shapeIds = shapes.map { it.id })
        }
        if (shapes.size == 2 && shapes.any { it.type in circleTypes } && shapes.any { it.type in linearTypes }) {
            values += GeometryConstraint2D("constraint-$seed-tangent", GeometryConstraint2DType.Tangent, shapeIds = shapes.map { it.id })
        }
        if (shapes.size == 1 && shapes.single().type in linearTypes && shapes.single().pointIndices.size >= 2) {
            val endpoints = shapes.single().pointIndices.take(2)
            val length = state.points[endpoints[0]].distanceTo(state.points[endpoints[1]])
            values += GeometryConstraint2D("constraint-$seed-length", GeometryConstraint2DType.FixedLength, endpoints, target = length)
        } else if (shapes.isEmpty() && points.size == 2) {
            val length = state.points[points[0]].distanceTo(state.points[points[1]])
            values += GeometryConstraint2D("constraint-$seed-length", GeometryConstraint2DType.FixedLength, points.take(2), target = length)
        }
        if (shapes.size == 1 && shapes.single().pointIndices.size >= 3) {
            val vertices = shapes.single().pointIndices.take(3)
            values += GeometryConstraint2D("constraint-$seed-angle", GeometryConstraint2DType.FixedAngle, vertices, target = angleDegrees(state.points[vertices[0]], state.points[vertices[1]], state.points[vertices[2]]))
        } else if (shapes.isEmpty() && points.size == 3) {
            values += GeometryConstraint2D("constraint-$seed-angle", GeometryConstraint2DType.FixedAngle, points.take(3), target = angleDegrees(state.points[points[0]], state.points[points[1]], state.points[points[2]]))
        }
        return values.map { constraint -> GeometryConstraintSuggestion(constraint.type.label, constraint, evaluateConstraint(state, constraint)) }
    }

    fun evaluateConstraints(state: WorkspaceState): List<GeometryConstraintFeedback> = state.geometryConstraints.map { evaluateConstraint(state, it) }

    fun evaluateConstraint(state: WorkspaceState, constraint: GeometryConstraint2D): GeometryConstraintFeedback {
        fun invalid(message: String) = GeometryConstraintFeedback(constraint, ConstraintFeedbackLevel.Invalid, null, constraint.type.label, message)
        val residualAndStatement = when (constraint.type) {
            GeometryConstraint2DType.EqualLength -> {
                val p = constraint.pointIndices.mapNotNull(state.points::getOrNull); if (p.size != 4) return invalid("Select two valid segments.")
                abs(p[0].distanceTo(p[1]) - p[2].distanceTo(p[3])) to "Segment lengths differ by"
            }
            GeometryConstraint2DType.Parallel, GeometryConstraint2DType.Perpendicular -> {
                val p = constraint.pointIndices.mapNotNull(state.points::getOrNull); if (p.size != 4) return invalid("Select two valid lines.")
                val u = p[1] - p[0]; val v = p[3] - p[2]; val scale = hypot(u.x, u.y) * hypot(v.x, v.y)
                if (scale < 1e-12) return invalid("A defining segment has zero length.")
                val value = if (constraint.type == GeometryConstraint2DType.Parallel) abs(u.x * v.y - u.y * v.x) / scale else abs(u.x * v.x + u.y * v.y) / scale
                value to if (constraint.type == GeometryConstraint2DType.Parallel) "Normalized direction mismatch" else "Normalized dot product"
            }
            GeometryConstraint2DType.FixedLength -> {
                val p = constraint.pointIndices.mapNotNull(state.points::getOrNull); val target = constraint.target ?: return invalid("No target length is stored.")
                if (p.size != 2) return invalid("Select two valid points.")
                abs(p[0].distanceTo(p[1]) - target) to "Length error from ${format(target)}"
            }
            GeometryConstraint2DType.FixedAngle -> {
                val p = constraint.pointIndices.mapNotNull(state.points::getOrNull); val target = constraint.target ?: return invalid("No target angle is stored.")
                if (p.size != 3) return invalid("Select three valid points.")
                abs(angleDegrees(p[0], p[1], p[2]) - target) to "Angle error from ${format(target)}°"
            }
            GeometryConstraint2DType.Concentric -> {
                val shapes = constraint.shapeIds.mapNotNull { id -> state.shapes.firstOrNull { it.id == id } }; if (shapes.size != 2) return invalid("Select two valid circles.")
                val centers = shapes.mapNotNull { state.points.getOrNull(it.pointIndices.firstOrNull() ?: -1) }; if (centers.size != 2) return invalid("A circle centre is missing.")
                centers[0].distanceTo(centers[1]) to "Distance between centres"
            }
            GeometryConstraint2DType.Tangent -> tangentResidual(state, constraint) ?: return invalid("Select a circle with a line or another circle.")
        }
        val residual = residualAndStatement.first
        val tolerance = when (constraint.type) { GeometryConstraint2DType.FixedAngle -> .05; else -> 1e-4 }
        val level = when { residual <= tolerance -> ConstraintFeedbackLevel.Satisfied; residual <= max(tolerance * 50, .02) -> ConstraintFeedbackLevel.NearlySatisfied; else -> ConstraintFeedbackLevel.Violated }
        val guidance = when (level) {
            ConstraintFeedbackLevel.Satisfied -> "Satisfied — the relation will be monitored while you drag."
            ConstraintFeedbackLevel.NearlySatisfied -> "Nearly satisfied — use precision drag or snapping."
            ConstraintFeedbackLevel.Violated -> "Move a defining point until the residual approaches zero."
            ConstraintFeedbackLevel.Invalid -> "Repair the referenced construction."
        }
        return GeometryConstraintFeedback(constraint, level, residual, "${residualAndStatement.second}: ${format(residual)}", guidance)
    }

    fun protocolTimeline(state: WorkspaceState, throughStep: Int = Int.MAX_VALUE, focusedId: String? = null): GeometryProtocolTimeline {
        val definitions = mutableListOf<Triple<String, String, Pair<String, List<String>>>>()
        state.points.indices.forEach { index ->
            val dependency = state.pointDependencies.firstOrNull { it.outputIndex == index }
            definitions += Triple(pointId(index), dependency?.let { "Create ${it.name}" } ?: "Create free point ${pointId(index)}", (dependency?.type?.name ?: "Coordinates ${format(state.points[index].x)}, ${format(state.points[index].y)}") to dependency?.inputIndices.orEmpty().map(::pointId))
        }
        state.shapes.forEach { shape -> definitions += Triple(shape.id, "Create ${shape.name}", shape.type.name to shape.pointIndices.map(::pointId)) }
        state.geometryConstraints.forEach { constraint -> definitions += Triple(constraint.id, "Apply ${constraint.type.label}", "Constraint" to (constraint.pointIndices.map(::pointId) + constraint.shapeIds)) }
        val count = throughStep.coerceIn(0, definitions.size); val visible = definitions.take(count).mapTo(linkedSetOf()) { it.first }
        val known = definitions.mapTo(linkedSetOf()) { it.first }
        val blocked = definitions.associate { it.first to (it.third.second.filterNot(known::contains).toSet()) }.filterValues { it.isNotEmpty() }
        val parentMap = definitions.associate { it.first to it.third.second }
        val chain = linkedSetOf<String>()
        fun visit(id: String) { parentMap[id].orEmpty().forEach { if (chain.add(it)) visit(it) } }
        focusedId?.let(::visit)
        val entries = definitions.mapIndexed { index, item ->
            val status = when { item.first in blocked -> GeometryProtocolStatus.Blocked; index >= count -> GeometryProtocolStatus.Future; index == count - 1 -> GeometryProtocolStatus.Current; else -> GeometryProtocolStatus.Complete }
            GeometryProtocolEntry(item.first, item.second, item.third.first, item.third.second, status)
        }
        return GeometryProtocolTimeline(entries, visible, focusedId, chain, blocked)
    }

    private fun shapeProperties(state: WorkspaceState, shape: Shape2D): List<Pair<String, String>> {
        val p = shape.pointIndices.mapNotNull(state.points::getOrNull)
        return buildList {
            add("Type" to shape.type.name); add("Definition" to shape.pointIndices.joinToString { pointId(it) })
            if (p.size >= 2) {
                add("First span" to format(p[0].distanceTo(p[1])))
                val dx = p[1].x - p[0].x; val dy = p[1].y - p[0].y
                add("Direction" to "${format(atan2(dy, dx) * 180 / PI)}°")
            }
            if (shape.type in circleTypes && p.size >= 2) { add("Centre" to "(${format(p[0].x)}, ${format(p[0].y)})"); add("Radius" to format(p[0].distanceTo(p[1]))) }
            if (shape.type in setOf(Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.RegularPolygon) && p.size >= 3) add("Area" to format(Geometry2D.polygonArea(p)))
            add("State" to listOfNotNull(if (shape.visible) "Visible" else "Hidden", if (shape.locked) "Locked" else null).joinToString(" · "))
        }
    }

    private fun tangentResidual(state: WorkspaceState, constraint: GeometryConstraint2D): Pair<Double, String>? {
        val shapes = constraint.shapeIds.mapNotNull { id -> state.shapes.firstOrNull { it.id == id } }
        if (shapes.size != 2) return null
        val circle = shapes.firstOrNull { it.type in circleTypes } ?: return null
        val circlePoints = circle.pointIndices.mapNotNull(state.points::getOrNull); if (circlePoints.size < 2) return null
        val center = circlePoints[0]; val radius = center.distanceTo(circlePoints[1]); val other = shapes.first { it.id != circle.id }
        if (other.type in linearTypes) {
            val line = other.pointIndices.mapNotNull(state.points::getOrNull); if (line.size < 2) return null
            val d = line[1] - line[0]; val length = hypot(d.x, d.y); if (length < 1e-12) return null
            val distance = abs(d.y * center.x - d.x * center.y + line[1].x * line[0].y - line[1].y * line[0].x) / length
            return abs(distance - radius) to "Line-to-circle tangency error"
        }
        if (other.type in circleTypes) {
            val q = other.pointIndices.mapNotNull(state.points::getOrNull); if (q.size < 2) return null
            val otherRadius = q[0].distanceTo(q[1]); val centers = center.distanceTo(q[0])
            return minOf(abs(centers - (radius + otherRadius)), abs(centers - abs(radius - otherRadius))) to "Circle tangency error"
        }
        return null
    }

    private fun angleDegrees(a: Vec2, vertex: Vec2, b: Vec2): Double {
        val u = a - vertex; val v = b - vertex; val scale = hypot(u.x, u.y) * hypot(v.x, v.y)
        if (scale < 1e-12) return Double.NaN
        return acos(((u.x * v.x + u.y * v.y) / scale).coerceIn(-1.0, 1.0)) * 180 / PI
    }

    private fun pointId(index: Int) = "P${index + 1}"
    private fun format(value: Double) = if (!value.isFinite()) "undefined" else "%.3f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}
