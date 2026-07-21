package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

sealed interface GeometryTransformation {
    fun apply(point: Vec2, resolver: (String) -> Vec2): Vec2
    val dependencies: List<String> get() = emptyList()

    data class Translation(val dx: Double, val dy: Double) : GeometryTransformation {
        override fun apply(point: Vec2, resolver: (String) -> Vec2) = point + Vec2(dx, dy)
    }
    data class Rotation(val center: String, val degrees: Double) : GeometryTransformation {
        override val dependencies = listOf(center)
        override fun apply(point: Vec2, resolver: (String) -> Vec2): Vec2 {
            val origin = resolver(center); val d = point - origin; val angle = degrees * PI / 180.0
            return origin + Vec2(d.x * cos(angle) - d.y * sin(angle), d.x * sin(angle) + d.y * cos(angle))
        }
    }
    data class Reflection(val lineA: String, val lineB: String) : GeometryTransformation {
        override val dependencies = listOf(lineA, lineB)
        override fun apply(point: Vec2, resolver: (String) -> Vec2): Vec2 {
            val a = resolver(lineA); val b = resolver(lineB); val d = b - a
            require(d.x * d.x + d.y * d.y > 1e-20) { "Reflection line requires two distinct points." }
            val scale = ((point - a).x * d.x + (point - a).y * d.y) / (d.x * d.x + d.y * d.y)
            return (a + d * scale) * 2.0 - point
        }
    }
    data class Dilation(val center: String, val scale: Double) : GeometryTransformation {
        override val dependencies = listOf(center)
        override fun apply(point: Vec2, resolver: (String) -> Vec2): Vec2 {
            val origin = resolver(center); return origin + (point - origin) * scale
        }
    }
    data class Affine(val m00: Double, val m01: Double, val m10: Double, val m11: Double, val tx: Double = 0.0, val ty: Double = 0.0) : GeometryTransformation {
        override fun apply(point: Vec2, resolver: (String) -> Vec2) = Vec2(m00 * point.x + m01 * point.y + tx, m10 * point.x + m11 * point.y + ty)
    }
    data class Composite(val steps: List<GeometryTransformation>) : GeometryTransformation {
        init { require(steps.isNotEmpty()) }
        override val dependencies = steps.flatMap { it.dependencies }.distinct()
        override fun apply(point: Vec2, resolver: (String) -> Vec2) = steps.fold(point) { value, transform -> transform.apply(value, resolver) }
    }
}

sealed interface DynamicPointRule {
    data class Free(val position: Vec2) : DynamicPointRule
    data class Midpoint(val first: String, val second: String) : DynamicPointRule
    data class Section(val first: String, val second: String, val ratioFromFirst: Double) : DynamicPointRule {
        init { require(ratioFromFirst in 0.0..1.0) }
    }
    data class Centroid(val first: String, val second: String, val third: String) : DynamicPointRule
    data class Circumcenter(val first: String, val second: String, val third: String) : DynamicPointRule
    data class Incenter(val first: String, val second: String, val third: String) : DynamicPointRule
    data class Orthocenter(val first: String, val second: String, val third: String) : DynamicPointRule
    data class Translate(val source: String, val dx: Double, val dy: Double) : DynamicPointRule
    data class Rotate(val source: String, val center: String, val degrees: Double) : DynamicPointRule
    data class ReflectLine(val source: String, val lineA: String, val lineB: String) : DynamicPointRule
    data class Dilate(val source: String, val center: String, val scale: Double) : DynamicPointRule
    data class LineIntersection(val a1: String, val a2: String, val b1: String, val b2: String) : DynamicPointRule
    data class PointOnObject(val objectId: String, val parameter: Double) : DynamicPointRule
    data class ObjectIntersection(val firstObjectId: String, val secondObjectId: String, val branch: Int = 0) : DynamicPointRule
    data class Transform(val source: String, val transformation: GeometryTransformation) : DynamicPointRule
}

data class DynamicPoint(val id: String, val rule: DynamicPointRule, val label: String = id)

sealed interface DynamicGeometryObject {
    val id: String
    val dependencies: List<String>
    data class Line(override val id: String, val throughA: String, val throughB: String) : DynamicGeometryObject { override val dependencies = listOf(throughA, throughB) }
    data class Parallel(override val id: String, val through: String, val lineA: String, val lineB: String) : DynamicGeometryObject { override val dependencies = listOf(through, lineA, lineB) }
    data class Perpendicular(override val id: String, val through: String, val lineA: String, val lineB: String) : DynamicGeometryObject { override val dependencies = listOf(through, lineA, lineB) }
    data class Circle(override val id: String, val center: String, val through: String) : DynamicGeometryObject { override val dependencies = listOf(center, through) }
    data class Ellipse(override val id: String, val focusA: String, val focusB: String, val through: String) : DynamicGeometryObject { override val dependencies = listOf(focusA, focusB, through) }
    data class Parabola(override val id: String, val focus: String, val directrixA: String, val directrixB: String) : DynamicGeometryObject { override val dependencies = listOf(focus, directrixA, directrixB) }
    data class Hyperbola(override val id: String, val focusA: String, val focusB: String, val through: String) : DynamicGeometryObject { override val dependencies = listOf(focusA, focusB, through) }
    data class TangentCircle(override val id: String, val circleId: String, val at: String) : DynamicGeometryObject { override val dependencies = listOf(circleId, at) }
    data class TangentConic(override val id: String, val conicId: String, val at: String) : DynamicGeometryObject { override val dependencies = listOf(conicId, at) }
    data class FixedAngle(override val id: String, val vertex: String, val armPoint: String, val degrees: Double) : DynamicGeometryObject { override val dependencies = listOf(vertex, armPoint) }
    data class Locus(override val id: String, val driver: String, val dependent: String, val samples: List<Vec2>) : DynamicGeometryObject { override val dependencies = listOf(driver, dependent) }
    data class Trace(override val id: String, val source: String, val samples: List<Vec2>) : DynamicGeometryObject { override val dependencies = listOf(source) }
}

sealed interface GeometryConstraint {
    val id: String
    val dependencies: List<String>
    data class Equal(override val id: String, val a: String, val b: String, val c: String, val d: String) : GeometryConstraint { override val dependencies = listOf(a, b, c, d) }
    data class Parallel(override val id: String, val a: String, val b: String, val c: String, val d: String) : GeometryConstraint { override val dependencies = listOf(a, b, c, d) }
    data class Perpendicular(override val id: String, val a: String, val b: String, val c: String, val d: String) : GeometryConstraint { override val dependencies = listOf(a, b, c, d) }
    data class Tangent(override val id: String, val lineObjectId: String, val conicObjectId: String) : GeometryConstraint { override val dependencies = listOf(lineObjectId, conicObjectId) }
    data class Concentric(override val id: String, val firstCircleId: String, val secondCircleId: String) : GeometryConstraint { override val dependencies = listOf(firstCircleId, secondCircleId) }
    data class FixedLength(override val id: String, val a: String, val b: String, val length: Double) : GeometryConstraint { init { require(length >= 0) }; override val dependencies = listOf(a, b) }
    data class FixedAngle(override val id: String, val a: String, val vertex: String, val b: String, val degrees: Double) : GeometryConstraint { override val dependencies = listOf(a, vertex, b) }
}

data class ConstraintEvaluation(val constraintId: String, val satisfied: Boolean, val residual: Double, val explanation: String)
data class GeometryGroup(val id: String, val name: String, val memberIds: Set<String>) { init { require(memberIds.isNotEmpty()) } }
data class GeometryClipboard(val points: List<DynamicPoint>, val objects: List<DynamicGeometryObject>, val sourceIds: Set<String>)
data class DependencyInspection(val id: String, val direct: List<String>, val transitive: Set<String>, val dependents: Set<String>, val missing: Set<String>, val circular: Boolean)
data class ProtocolReplayFrame(val document: DynamicGeometryDocument, val activeStep: Int, val availableIds: Set<String>, val blockedSteps: Map<String, Set<String>>)

data class ProtocolStep(val id: String, val title: String, val dependencies: List<String>, val reason: String, val visible: Boolean = true)
data class DynamicGeometryDocument(
    val points: List<DynamicPoint> = emptyList(),
    val objects: List<DynamicGeometryObject> = emptyList(),
    val protocol: List<ProtocolStep> = emptyList(),
    val constraints: List<GeometryConstraint> = emptyList(),
    val groups: List<GeometryGroup> = emptyList(),
    val selection: Set<String> = emptySet(),
)
data class ExactGeometryMeasurement(val label: String, val exact: String, val decimal: Double, val unit: String)
data class GeometryRelation(val holds: Boolean, val statement: String, val residual: Double, val reason: String)

class DynamicGeometryEngine {
    fun addPoint(document: DynamicGeometryDocument, point: DynamicPoint, reason: String): DynamicGeometryDocument {
        require(document.points.none { it.id == point.id } && document.objects.none { it.id == point.id })
        val updated = document.copy(
            points = document.points + point,
            protocol = document.protocol + ProtocolStep(point.id, "Create point " + point.label, dependencies(point.rule), reason),
        )
        resolve(updated)
        return updated
    }

    fun addObject(document: DynamicGeometryDocument, value: DynamicGeometryObject, reason: String): DynamicGeometryDocument {
        require(document.objects.none { it.id == value.id } && document.points.none { it.id == value.id })
        val known = (document.points.map { it.id } + document.objects.map { it.id }).toSet()
        require(value.dependencies.all { it in known }) { "Object '${value.id}' has an unknown dependency." }
        val updated = document.copy(
            objects = document.objects + value,
            protocol = document.protocol + ProtocolStep(value.id, "Create " + value.javaClass.simpleName, value.dependencies, reason),
        )
        resolve(updated)
        return updated
    }

    fun addConstraint(document: DynamicGeometryDocument, constraint: GeometryConstraint, reason: String): DynamicGeometryDocument {
        require(document.constraints.none { it.id == constraint.id })
        val known = (document.points.map { it.id } + document.objects.map { it.id }).toSet()
        require(constraint.dependencies.all { it in known }) { "Constraint '${constraint.id}' has an unknown dependency." }
        val updated = document.copy(
            constraints = document.constraints + constraint,
            protocol = document.protocol + ProtocolStep(constraint.id, "Apply ${constraint.javaClass.simpleName}", constraint.dependencies, reason),
        )
        return enforceConstraints(updated)
    }

    fun moveFreePoint(document: DynamicGeometryDocument, id: String, position: Vec2): DynamicGeometryDocument {
        val updated = document.copy(points = document.points.map { point ->
            if (point.id != id) point else {
                require(point.rule is DynamicPointRule.Free) { "Only free points can be dragged." }
                point.copy(rule = DynamicPointRule.Free(position))
            }
        })
        resolve(updated)
        return updated
    }

    fun resolve(document: DynamicGeometryDocument): Map<String, Vec2> {
        val definitions = document.points.associateBy { it.id }
        val cache = mutableMapOf<String, Vec2>()
        fun point(id: String, stack: Set<String>): Vec2 {
            cache[id]?.let { return it }
            require(id !in stack) { "Circular geometry dependency: " + (stack + id).joinToString(" → ") }
            val rule = definitions[id]?.rule ?: error("Unknown point '" + id + "'")
            fun p(name: String) = point(name, stack + id)
            fun prepareObject(value: DynamicGeometryObject, objectStack: Set<String> = emptySet()) {
                require(value.id !in objectStack) { "Circular object dependency: " + (objectStack + value.id).joinToString(" -> ") }
                value.dependencies.forEach { dependency ->
                    if (dependency in definitions) p(dependency)
                    else prepareObject(document.objects.firstOrNull { it.id == dependency } ?: error("Unknown geometry dependency '$dependency'"), objectStack + value.id)
                }
            }
            val value = when (rule) {
                is DynamicPointRule.Free -> rule.position
                is DynamicPointRule.Midpoint -> (p(rule.first) + p(rule.second)) * .5
                is DynamicPointRule.Section -> p(rule.first) + (p(rule.second) - p(rule.first)) * rule.ratioFromFirst
                is DynamicPointRule.Centroid -> (p(rule.first) + p(rule.second) + p(rule.third)) * (1.0 / 3.0)
                is DynamicPointRule.Circumcenter -> circumcenter(p(rule.first), p(rule.second), p(rule.third))
                is DynamicPointRule.Incenter -> incenter(p(rule.first), p(rule.second), p(rule.third))
                is DynamicPointRule.Orthocenter -> {
                    val a = p(rule.first); val b = p(rule.second); val c = p(rule.third)
                    a + b + c - circumcenter(a, b, c) * 2.0
                }
                is DynamicPointRule.Translate -> p(rule.source) + Vec2(rule.dx, rule.dy)
                is DynamicPointRule.Rotate -> {
                    val source = p(rule.source); val center = p(rule.center); val radians = rule.degrees * PI / 180
                    val delta = source - center
                    center + Vec2(delta.x * cos(radians) - delta.y * sin(radians), delta.x * sin(radians) + delta.y * cos(radians))
                }
                is DynamicPointRule.ReflectLine -> reflect(p(rule.source), p(rule.lineA), p(rule.lineB))
                is DynamicPointRule.Dilate -> p(rule.center) + (p(rule.source) - p(rule.center)) * rule.scale
                is DynamicPointRule.LineIntersection -> intersect(p(rule.a1), p(rule.a2), p(rule.b1), p(rule.b2))
                is DynamicPointRule.PointOnObject -> {
                    val objectValue = document.objects.firstOrNull { it.id == rule.objectId } ?: error("Unknown object '${rule.objectId}'")
                    prepareObject(objectValue)
                    pointOnObject(objectValue, rule.parameter, cache)
                }
                is DynamicPointRule.ObjectIntersection -> {
                    val first = document.objects.firstOrNull { it.id == rule.firstObjectId } ?: error("Unknown object '${rule.firstObjectId}'")
                    val second = document.objects.firstOrNull { it.id == rule.secondObjectId } ?: error("Unknown object '${rule.secondObjectId}'")
                    prepareObject(first); prepareObject(second)
                    val values = intersectionsWithPoints(document, first, second, cache)
                    require(values.isNotEmpty()) { "Objects '${first.id}' and '${second.id}' do not intersect." }
                    values[rule.branch.coerceIn(0, values.lastIndex)]
                }
                is DynamicPointRule.Transform -> rule.transformation.apply(p(rule.source), ::p)
            }
            require(value.x.isFinite() && value.y.isFinite())
            cache[id] = value
            return value
        }
        definitions.keys.forEach { point(it, emptySet()) }
        return cache
    }

    fun replay(document: DynamicGeometryDocument, throughStep: Int): DynamicGeometryDocument {
        val count = throughStep.coerceIn(0, document.protocol.size)
        val enabled = document.protocol.take(count).map { it.id }.toSet()
        return document.copy(
            points = document.points.filter { it.id in enabled },
            objects = document.objects.filter { it.id in enabled },
            constraints = document.constraints.filter { it.id in enabled },
            groups = document.groups.mapNotNull { group -> group.copy(memberIds = group.memberIds intersect enabled).takeIf { it.memberIds.isNotEmpty() } },
            selection = document.selection intersect enabled,
            protocol = document.protocol.mapIndexed { index, step -> step.copy(visible = index < count) },
        )
    }

    fun setStepVisible(document: DynamicGeometryDocument, id: String, visible: Boolean) =
        document.copy(protocol = document.protocol.map { if (it.id == id) it.copy(visible = visible) else it })

    fun dependencyTree(document: DynamicGeometryDocument): Map<String, List<String>> = document.protocol.associate { it.id to it.dependencies }

    fun inspectDependency(document: DynamicGeometryDocument, id: String): DependencyInspection {
        val tree = dependencyTree(document); require(id in tree) { "Unknown protocol id '$id'." }
        val transitive = linkedSetOf<String>(); var circular = false
        fun visit(node: String, stack: Set<String>) {
            if (node in stack) { circular = true; return }
            tree[node].orEmpty().forEach { dependency -> if (transitive.add(dependency)) visit(dependency, stack + node) }
        }
        visit(id, emptySet())
        val dependents = tree.filterValues { id in it }.keys
        val known = tree.keys + document.points.map { it.id } + document.objects.map { it.id }
        return DependencyInspection(id, tree.getValue(id), transitive, dependents, transitive - known, circular)
    }

    fun replayFrame(document: DynamicGeometryDocument, throughStep: Int): ProtocolReplayFrame {
        val count = throughStep.coerceIn(0, document.protocol.size); val available = document.protocol.take(count).map { it.id }.toSet()
        val blocked: Map<String, Set<String>> = document.protocol.drop(count).associate { step ->
            step.id to step.dependencies.filterNot(available::contains).toSet()
        }.filterValues { it.isNotEmpty() }
        return ProtocolReplayFrame(replay(document, count), count, available, blocked)
    }

    fun setPointParameter(document: DynamicGeometryDocument, id: String, parameter: Double): DynamicGeometryDocument {
        val updated = document.copy(points = document.points.map { point ->
            if (point.id != id) point else {
                val rule = point.rule as? DynamicPointRule.PointOnObject ?: error("Point '$id' is not attached to an object.")
                point.copy(rule = rule.copy(parameter = parameter))
            }
        })
        resolve(updated); return updated
    }

    fun trace(document: DynamicGeometryDocument, driverId: String, dependentId: String, driverPath: List<Vec2>): List<Vec2> {
        require(driverPath.isNotEmpty()); require(document.points.firstOrNull { it.id == driverId }?.rule is DynamicPointRule.Free) { "Trace driver must be a free point." }
        return driverPath.map { position -> resolve(moveFreePoint(document, driverId, position)).getValue(dependentId) }
    }

    fun generateLocus(document: DynamicGeometryDocument, id: String, driverId: String, dependentId: String, driverPath: List<Vec2>, reason: String = "Trace dependent point while the driver moves"): DynamicGeometryDocument =
        addObject(document, DynamicGeometryObject.Locus(id, driverId, dependentId, trace(document, driverId, dependentId, driverPath)), reason)

    fun generateTrace(document: DynamicGeometryDocument, id: String, sourceId: String, samples: List<Vec2>, reason: String = "Record the point path"): DynamicGeometryDocument {
        require(samples.isNotEmpty()); return addObject(document, DynamicGeometryObject.Trace(id, sourceId, samples), reason)
    }

    fun evaluateConstraints(document: DynamicGeometryDocument, tolerance: Double = 1e-7): List<ConstraintEvaluation> {
        require(tolerance > 0); val p = resolve(document); val objects = document.objects.associateBy { it.id }
        fun direction(a: String, b: String) = p.getValue(b) - p.getValue(a)
        fun normalizedCross(a: Vec2, b: Vec2) = abs(a.x * b.y - a.y * b.x) / max(1e-15, hypot(a.x, a.y) * hypot(b.x, b.y))
        fun normalizedDot(a: Vec2, b: Vec2) = abs(a.x * b.x + a.y * b.y) / max(1e-15, hypot(a.x, a.y) * hypot(b.x, b.y))
        return document.constraints.map { constraint ->
            val (residual, explanation) = when (constraint) {
                is GeometryConstraint.Equal -> abs(p.getValue(constraint.a).distanceTo(p.getValue(constraint.b)) - p.getValue(constraint.c).distanceTo(p.getValue(constraint.d))) to "The two segment lengths are equal."
                is GeometryConstraint.Parallel -> normalizedCross(direction(constraint.a, constraint.b), direction(constraint.c, constraint.d)) to "Parallel directions have zero normalized cross product."
                is GeometryConstraint.Perpendicular -> normalizedDot(direction(constraint.a, constraint.b), direction(constraint.c, constraint.d)) to "Perpendicular directions have zero normalized dot product."
                is GeometryConstraint.FixedLength -> abs(p.getValue(constraint.a).distanceTo(p.getValue(constraint.b)) - constraint.length) to "The segment has the required fixed length."
                is GeometryConstraint.FixedAngle -> {
                    val a = direction(constraint.vertex, constraint.a); val b = direction(constraint.vertex, constraint.b)
                    val angle = acos(((a.x * b.x + a.y * b.y) / max(1e-15, hypot(a.x, a.y) * hypot(b.x, b.y))).coerceIn(-1.0, 1.0)) * 180 / PI
                    abs(angle - constraint.degrees) to "The angle has the required measure."
                }
                is GeometryConstraint.Concentric -> {
                    val first = objects[constraint.firstCircleId] as? DynamicGeometryObject.Circle ?: error("Concentric constraint requires circles.")
                    val second = objects[constraint.secondCircleId] as? DynamicGeometryObject.Circle ?: error("Concentric constraint requires circles.")
                    p.getValue(first.center).distanceTo(p.getValue(second.center)) to "The circles share one center."
                }
                is GeometryConstraint.Tangent -> {
                    val line = objects[constraint.lineObjectId] ?: error("Unknown tangent line object."); val conic = objects[constraint.conicObjectId] ?: error("Unknown tangent conic object.")
                    val count = intersectionsWithPoints(document, line, conic, p).size
                    (if (count == 1) 0.0 else count.coerceAtLeast(1).toDouble()) to "A tangent has exactly one real contact point."
                }
            }
            ConstraintEvaluation(constraint.id, residual <= tolerance, residual, explanation)
        }
    }

    fun enforceConstraints(document: DynamicGeometryDocument, iterations: Int = 8): DynamicGeometryDocument {
        require(iterations in 1..100); var current = document
        repeat(iterations) {
            var points = current.points
            fun positions() = resolve(current.copy(points = points))
            fun moveFree(id: String, target: Vec2) { points = points.map { point -> if (point.id == id && point.rule is DynamicPointRule.Free) point.copy(rule = DynamicPointRule.Free(target)) else point } }
            current.constraints.forEach { constraint ->
                val p = positions()
                fun align(a: String, b: String, c: String, d: String, perpendicular: Boolean) {
                    val source = p.getValue(b) - p.getValue(a); val sourceLength = hypot(source.x, source.y); if (sourceLength < 1e-12) return
                    val length = p.getValue(c).distanceTo(p.getValue(d)).takeIf { it > 1e-12 } ?: sourceLength
                    val unit = source * (1 / sourceLength); val direction = if (perpendicular) Vec2(-unit.y, unit.x) else unit
                    moveFree(d, p.getValue(c) + direction * length)
                }
                when (constraint) {
                    is GeometryConstraint.FixedLength -> {
                        val d = p.getValue(constraint.b) - p.getValue(constraint.a); val length = hypot(d.x, d.y); val unit = if (length < 1e-12) Vec2(1.0, 0.0) else d * (1 / length)
                        moveFree(constraint.b, p.getValue(constraint.a) + unit * constraint.length)
                    }
                    is GeometryConstraint.FixedAngle -> {
                        val arm = p.getValue(constraint.a) - p.getValue(constraint.vertex); val target = p.getValue(constraint.b) - p.getValue(constraint.vertex); val length = max(1e-12, hypot(target.x, target.y)); val base = atan2(arm.y, arm.x) + constraint.degrees * PI / 180
                        moveFree(constraint.b, p.getValue(constraint.vertex) + Vec2(cos(base), sin(base)) * length)
                    }
                    is GeometryConstraint.Equal -> {
                        val targetLength = p.getValue(constraint.a).distanceTo(p.getValue(constraint.b)); val d = p.getValue(constraint.d) - p.getValue(constraint.c); val length = hypot(d.x, d.y); val unit = if (length < 1e-12) Vec2(1.0, 0.0) else d * (1 / length)
                        moveFree(constraint.d, p.getValue(constraint.c) + unit * targetLength)
                    }
                    is GeometryConstraint.Parallel -> align(constraint.a, constraint.b, constraint.c, constraint.d, false)
                    is GeometryConstraint.Perpendicular -> align(constraint.a, constraint.b, constraint.c, constraint.d, true)
                    is GeometryConstraint.Concentric -> {
                        val objects = current.objects.associateBy { it.id }; val first = objects[constraint.firstCircleId] as? DynamicGeometryObject.Circle; val second = objects[constraint.secondCircleId] as? DynamicGeometryObject.Circle
                        if (first != null && second != null) moveFree(second.center, p.getValue(first.center))
                    }
                    is GeometryConstraint.Tangent -> {
                        val objects = current.objects.associateBy { it.id }; val line = objects[constraint.lineObjectId] as? DynamicGeometryObject.Line; val circle = objects[constraint.conicObjectId] as? DynamicGeometryObject.Circle
                        if (line != null && circle != null) {
                            val anchor = p.getValue(line.throughA); val center = p.getValue(circle.center); val radius = center.distanceTo(p.getValue(circle.through)); val toCenter = center - anchor; val distance = hypot(toCenter.x, toCenter.y)
                            if (distance >= radius - 1e-10 && distance > 1e-12) {
                                val currentDirection = p.getValue(line.throughB) - anchor; val length = max(1e-12, hypot(currentDirection.x, currentDirection.y)); val base = atan2(toCenter.y, toCenter.x)
                                val offset = if (abs(distance - radius) < 1e-10) PI / 2 else kotlin.math.asin((radius / distance).coerceIn(-1.0, 1.0))
                                val candidates = listOf(base - offset, base + offset); val currentAngle = atan2(currentDirection.y, currentDirection.x)
                                fun angularDistance(first: Double, second: Double) = abs(atan2(sin(first - second), cos(first - second)))
                                val angle = candidates.minBy { angularDistance(it, currentAngle) }
                                moveFree(line.throughB, anchor + Vec2(cos(angle), sin(angle)) * length)
                            }
                        }
                    }
                }
            }
            current = current.copy(points = points)
        }
        resolve(current); return current
    }

    fun select(document: DynamicGeometryDocument, ids: Set<String>, additive: Boolean = false): DynamicGeometryDocument {
        val known = (document.points.map { it.id } + document.objects.map { it.id } + document.groups.map { it.id }).toSet(); require(ids.all { it in known })
        val expanded = ids + document.groups.filter { it.id in ids }.flatMap { it.memberIds }
        return document.copy(selection = if (additive) document.selection + expanded else expanded)
    }

    fun clearSelection(document: DynamicGeometryDocument) = document.copy(selection = emptySet())

    fun groupSelection(document: DynamicGeometryDocument, groupId: String, name: String = groupId): DynamicGeometryDocument {
        require(document.selection.isNotEmpty()); require(document.groups.none { it.id == groupId })
        return document.copy(groups = document.groups + GeometryGroup(groupId, name, document.selection))
    }

    fun ungroup(document: DynamicGeometryDocument, groupId: String) = document.copy(groups = document.groups.filterNot { it.id == groupId })

    fun copySelection(document: DynamicGeometryDocument): GeometryClipboard {
        val objectById = document.objects.associateBy { it.id }; val pointById = document.points.associateBy { it.id }
        val selected = document.selection + document.groups.filter { it.id in document.selection }.flatMap { it.memberIds }
        val pointIds = selected.filterTo(linkedSetOf()) { it in pointById }
        selected.mapNotNull(objectById::get).flatMapTo(pointIds) { it.dependencies.filter(pointById::containsKey) }
        var changed = true
        while (changed) {
            val before = pointIds.size
            pointIds.toList().mapNotNull(pointById::get).flatMap { dependencies(it.rule) }.filterTo(pointIds) { it in pointById }
            changed = pointIds.size != before
        }
        return GeometryClipboard(document.points.filter { it.id in pointIds }, document.objects.filter { it.id in selected }, selected)
    }

    fun pasteClipboard(document: DynamicGeometryDocument, clipboard: GeometryClipboard, idPrefix: String = "copy", offset: Vec2 = Vec2(.5, -.5)): DynamicGeometryDocument {
        require(idPrefix.isNotBlank()); val occupied = (document.points.map { it.id } + document.objects.map { it.id }).toMutableSet(); val remap = linkedMapOf<String, String>()
        (clipboard.points.map { it.id } + clipboard.objects.map { it.id }).forEach { original ->
            var candidate = "$idPrefix-$original"; var suffix = 2
            while (candidate in occupied) candidate = "$idPrefix-$original-${suffix++}"
            remap[original] = candidate; occupied += candidate
        }
        fun id(value: String) = remap[value] ?: value
        val points = clipboard.points.map { point -> point.copy(id = id(point.id), label = id(point.label), rule = remapRule(point.rule, ::id, offset)) }
        val objects = clipboard.objects.map { remapObject(it, ::id) }
        val steps = points.map { ProtocolStep(it.id, "Paste point ${it.label}", dependencies(it.rule), "Clipboard paste") } + objects.map { ProtocolStep(it.id, "Paste ${it.javaClass.simpleName}", it.dependencies, "Clipboard paste") }
        val pastedIds = (points.map { it.id } + objects.map { it.id }).toSet()
        val updated = document.copy(points = document.points + points, objects = document.objects + objects, protocol = document.protocol + steps, selection = pastedIds)
        resolve(updated); return updated
    }

    fun deleteSelection(document: DynamicGeometryDocument): DynamicGeometryDocument {
        val removed = document.selection.toMutableSet(); var changed = true
        while (changed) {
            val before = removed.size
            document.points.filter { dependencies(it.rule).any(removed::contains) }.forEach { removed += it.id }
            document.objects.filter { it.dependencies.any(removed::contains) }.forEach { removed += it.id }
            document.constraints.filter { it.dependencies.any(removed::contains) }.forEach { removed += it.id }
            changed = removed.size != before
        }
        return document.copy(
            points = document.points.filterNot { it.id in removed }, objects = document.objects.filterNot { it.id in removed }, constraints = document.constraints.filterNot { it.id in removed },
            protocol = document.protocol.filterNot { it.id in removed }, groups = document.groups.mapNotNull { it.copy(memberIds = it.memberIds - removed).takeIf { group -> group.memberIds.isNotEmpty() } }, selection = emptySet(),
        )
    }

    fun transformSelection(document: DynamicGeometryDocument, transformation: GeometryTransformation, idPrefix: String = "transform"): DynamicGeometryDocument {
        val clipboard = copySelection(document); require(clipboard.points.isNotEmpty()); require(idPrefix.isNotBlank())
        val occupied = (document.points.map { it.id } + document.objects.map { it.id }).toMutableSet(); val remap = linkedMapOf<String, String>()
        (clipboard.points.map { it.id } + clipboard.objects.map { it.id }).forEach { original ->
            var candidate = "$idPrefix-$original"; var suffix = 2
            while (candidate in occupied) candidate = "$idPrefix-$original-${suffix++}"
            remap[original] = candidate; occupied += candidate
        }
        fun id(value: String) = remap[value] ?: value
        val points = clipboard.points.map { point -> DynamicPoint(id(point.id), DynamicPointRule.Transform(point.id, transformation), id(point.label)) }
        val objects = clipboard.objects.map { remapObject(it, ::id) }; val selected = (points.map { it.id } + objects.map { it.id }).toSet()
        val steps = points.map { ProtocolStep(it.id, "Transform point ${it.label}", dependencies(it.rule), "Reusable transformation") } + objects.map { ProtocolStep(it.id, "Transform ${it.javaClass.simpleName}", it.dependencies, "Transform selected construction") }
        val updated = document.copy(points = document.points + points, objects = document.objects + objects, protocol = document.protocol + steps, selection = selected)
        resolve(updated); return updated
    }

    private fun remapRule(rule: DynamicPointRule, id: (String) -> String, offset: Vec2): DynamicPointRule = when (rule) {
        is DynamicPointRule.Free -> rule.copy(position = rule.position + offset)
        is DynamicPointRule.Midpoint -> rule.copy(first = id(rule.first), second = id(rule.second))
        is DynamicPointRule.Section -> rule.copy(first = id(rule.first), second = id(rule.second))
        is DynamicPointRule.Centroid -> rule.copy(first = id(rule.first), second = id(rule.second), third = id(rule.third))
        is DynamicPointRule.Circumcenter -> rule.copy(first = id(rule.first), second = id(rule.second), third = id(rule.third))
        is DynamicPointRule.Incenter -> rule.copy(first = id(rule.first), second = id(rule.second), third = id(rule.third))
        is DynamicPointRule.Orthocenter -> rule.copy(first = id(rule.first), second = id(rule.second), third = id(rule.third))
        is DynamicPointRule.Translate -> rule.copy(source = id(rule.source))
        is DynamicPointRule.Rotate -> rule.copy(source = id(rule.source), center = id(rule.center))
        is DynamicPointRule.ReflectLine -> rule.copy(source = id(rule.source), lineA = id(rule.lineA), lineB = id(rule.lineB))
        is DynamicPointRule.Dilate -> rule.copy(source = id(rule.source), center = id(rule.center))
        is DynamicPointRule.LineIntersection -> rule.copy(a1 = id(rule.a1), a2 = id(rule.a2), b1 = id(rule.b1), b2 = id(rule.b2))
        is DynamicPointRule.PointOnObject -> rule.copy(objectId = id(rule.objectId))
        is DynamicPointRule.ObjectIntersection -> rule.copy(firstObjectId = id(rule.firstObjectId), secondObjectId = id(rule.secondObjectId))
        is DynamicPointRule.Transform -> rule.copy(source = id(rule.source), transformation = remapTransformation(rule.transformation, id))
    }

    private fun remapTransformation(value: GeometryTransformation, id: (String) -> String): GeometryTransformation = when (value) {
        is GeometryTransformation.Translation, is GeometryTransformation.Affine -> value
        is GeometryTransformation.Rotation -> value.copy(center = id(value.center))
        is GeometryTransformation.Reflection -> value.copy(lineA = id(value.lineA), lineB = id(value.lineB))
        is GeometryTransformation.Dilation -> value.copy(center = id(value.center))
        is GeometryTransformation.Composite -> value.copy(steps = value.steps.map { remapTransformation(it, id) })
    }

    private fun remapObject(value: DynamicGeometryObject, id: (String) -> String): DynamicGeometryObject = when (value) {
        is DynamicGeometryObject.Line -> value.copy(id = id(value.id), throughA = id(value.throughA), throughB = id(value.throughB))
        is DynamicGeometryObject.Parallel -> value.copy(id = id(value.id), through = id(value.through), lineA = id(value.lineA), lineB = id(value.lineB))
        is DynamicGeometryObject.Perpendicular -> value.copy(id = id(value.id), through = id(value.through), lineA = id(value.lineA), lineB = id(value.lineB))
        is DynamicGeometryObject.Circle -> value.copy(id = id(value.id), center = id(value.center), through = id(value.through))
        is DynamicGeometryObject.Ellipse -> value.copy(id = id(value.id), focusA = id(value.focusA), focusB = id(value.focusB), through = id(value.through))
        is DynamicGeometryObject.Parabola -> value.copy(id = id(value.id), focus = id(value.focus), directrixA = id(value.directrixA), directrixB = id(value.directrixB))
        is DynamicGeometryObject.Hyperbola -> value.copy(id = id(value.id), focusA = id(value.focusA), focusB = id(value.focusB), through = id(value.through))
        is DynamicGeometryObject.TangentCircle -> value.copy(id = id(value.id), circleId = id(value.circleId), at = id(value.at))
        is DynamicGeometryObject.TangentConic -> value.copy(id = id(value.id), conicId = id(value.conicId), at = id(value.at))
        is DynamicGeometryObject.FixedAngle -> value.copy(id = id(value.id), vertex = id(value.vertex), armPoint = id(value.armPoint))
        is DynamicGeometryObject.Locus -> value.copy(id = id(value.id), driver = id(value.driver), dependent = id(value.dependent))
        is DynamicGeometryObject.Trace -> value.copy(id = id(value.id), source = id(value.source))
    }

    fun distance(document: DynamicGeometryDocument, first: String, second: String): ExactGeometryMeasurement {
        val p = resolve(document); val dx = p.getValue(second).x - p.getValue(first).x; val dy = p.getValue(second).y - p.getValue(first).y
        val squared = dx * dx + dy * dy
        return ExactGeometryMeasurement("Distance " + first + second, radical(squared), sqrt(squared), "units")
    }

    fun slope(document: DynamicGeometryDocument, first: String, second: String): ExactGeometryMeasurement {
        val p = resolve(document); val dx = p.getValue(second).x - p.getValue(first).x; val dy = p.getValue(second).y - p.getValue(first).y
        require(abs(dx) > 1e-12) { "Vertical line has undefined slope." }
        return ExactGeometryMeasurement("Slope " + first + second, fraction(dy, dx), dy / dx, "")
    }

    fun polygonArea(document: DynamicGeometryDocument, ids: List<String>): ExactGeometryMeasurement {
        require(ids.size >= 3)
        val p = resolve(document); val twice = ids.indices.sumOf { index ->
            val a = p.getValue(ids[index]); val b = p.getValue(ids[(index + 1) % ids.size]); a.x * b.y - a.y * b.x
        }
        return ExactGeometryMeasurement("Area", fraction(abs(twice), 2.0), abs(twice) / 2, "units²")
    }

    fun angle(document: DynamicGeometryDocument, first: String, vertex: String, second: String): ExactGeometryMeasurement {
        val p = resolve(document); val a = p.getValue(first) - p.getValue(vertex); val b = p.getValue(second) - p.getValue(vertex)
        val degrees = kotlin.math.acos(((a.x * b.x + a.y * b.y) / (hypot(a.x, a.y) * hypot(b.x, b.y))).coerceIn(-1.0, 1.0)) * 180 / PI
        return ExactGeometryMeasurement("Angle " + first + vertex + second, format(degrees) + "°", degrees, "degrees")
    }

    fun circleMeasurements(document: DynamicGeometryDocument, id: String): List<ExactGeometryMeasurement> {
        val circle = document.objects.filterIsInstance<DynamicGeometryObject.Circle>().first { it.id == id }
        val radius = distance(document, circle.center, circle.through)
        return listOf(radius.copy(label = "Radius"), ExactGeometryMeasurement("Diameter", "2(" + radius.exact + ")", radius.decimal * 2, "units"))
    }

    fun ellipseParameters(document: DynamicGeometryDocument, id: String): Map<String, ExactGeometryMeasurement> {
        val ellipse = document.objects.filterIsInstance<DynamicGeometryObject.Ellipse>().first { it.id == id }
        val p = resolve(document); val f1 = p.getValue(ellipse.focusA); val f2 = p.getValue(ellipse.focusB); val point = p.getValue(ellipse.through)
        val a = (point.distanceTo(f1) + point.distanceTo(f2)) / 2; val c = f1.distanceTo(f2) / 2; val b = sqrt((a * a - c * c).coerceAtLeast(0.0))
        return mapOf(
            "semiMajor" to ExactGeometryMeasurement("a", format(a), a, "units"),
            "semiMinor" to ExactGeometryMeasurement("b", radical(b * b), b, "units"),
            "focalDistance" to ExactGeometryMeasurement("c", format(c), c, "units"),
        )
    }

    /** Intersects every currently renderable line/circle/ellipse pair and refuses unsupported pairs explicitly. */
    fun intersections(document: DynamicGeometryDocument, first: DynamicGeometryObject, second: DynamicGeometryObject): List<Vec2> {
        return intersectionsWithPoints(document, first, second, resolve(document))
    }

    private fun intersectionsWithPoints(document: DynamicGeometryDocument, first: DynamicGeometryObject, second: DynamicGeometryObject, points: Map<String, Vec2>): List<Vec2> {
        fun line(value: DynamicGeometryObject): Pair<Vec2, Vec2>? = when (value) {
            is DynamicGeometryObject.Line -> points.getValue(value.throughA) to points.getValue(value.throughB)
            is DynamicGeometryObject.Parallel -> points.getValue(value.through) to (points.getValue(value.through) + points.getValue(value.lineB) - points.getValue(value.lineA))
            is DynamicGeometryObject.Perpendicular -> {
                val origin = points.getValue(value.through); val d = points.getValue(value.lineB) - points.getValue(value.lineA)
                origin to origin + Vec2(-d.y, d.x)
            }
            is DynamicGeometryObject.FixedAngle -> {
                val origin = points.getValue(value.vertex); val arm = points.getValue(value.armPoint) - origin; val r = value.degrees * PI / 180
                origin to origin + Vec2(arm.x * cos(r) - arm.y * sin(r), arm.x * sin(r) + arm.y * cos(r))
            }
            is DynamicGeometryObject.TangentCircle -> {
                val circle = document.objects.filterIsInstance<DynamicGeometryObject.Circle>().first { it.id == value.circleId }
                val at = points.getValue(value.at); val radial = at - points.getValue(circle.center)
                at to at + Vec2(-radial.y, radial.x)
            }
            is DynamicGeometryObject.TangentConic -> tangentLine(document, value.conicId, value.at, points)
            else -> null
        }
        fun circle(value: DynamicGeometryObject): Pair<Vec2, Double>? = (value as? DynamicGeometryObject.Circle)?.let {
            val center = points.getValue(it.center); center to center.distanceTo(points.getValue(it.through))
        }
        val l1 = line(first); val l2 = line(second)
        if (l1 != null && l2 != null) return runCatching { listOf(intersect(l1.first, l1.second, l2.first, l2.second)) }.getOrDefault(emptyList())
        val c1 = circle(first); val c2 = circle(second)
        if (l1 != null && c2 != null) return lineCircle(l1.first, l1.second, c2.first, c2.second)
        if (l2 != null && c1 != null) return lineCircle(l2.first, l2.second, c1.first, c1.second)
        if (c1 != null && c2 != null) return circleCircle(c1.first, c1.second, c2.first, c2.second)
        val ellipse = (first as? DynamicGeometryObject.Ellipse) ?: (second as? DynamicGeometryObject.Ellipse)
        val otherLine = if (ellipse === first) l2 else l1
        if (ellipse != null && otherLine != null) return lineEllipse(ellipse, otherLine.first, otherLine.second, points)
        val parabola = (first as? DynamicGeometryObject.Parabola) ?: (second as? DynamicGeometryObject.Parabola)
        if (parabola != null && otherLine != null) return lineParabola(parabola, otherLine.first, otherLine.second, points)
        val hyperbola = (first as? DynamicGeometryObject.Hyperbola) ?: (second as? DynamicGeometryObject.Hyperbola)
        if (hyperbola != null && otherLine != null) return lineHyperbola(hyperbola, otherLine.first, otherLine.second, points)
        error("Intersection is not defined for ${first.javaClass.simpleName} and ${second.javaClass.simpleName}.")
    }

    fun relation(document: DynamicGeometryDocument, first: DynamicGeometryObject, second: DynamicGeometryObject): GeometryRelation {
        val p = resolve(document)
        fun direction(value: DynamicGeometryObject): Vec2 = when (value) {
            is DynamicGeometryObject.Line -> p.getValue(value.throughB) - p.getValue(value.throughA)
            is DynamicGeometryObject.Parallel -> p.getValue(value.lineB) - p.getValue(value.lineA)
            is DynamicGeometryObject.Perpendicular -> {
                val d = p.getValue(value.lineB) - p.getValue(value.lineA); Vec2(-d.y, d.x)
            }
            is DynamicGeometryObject.TangentCircle -> tangentLine(document, value.circleId, value.at, p).let { it.second - it.first }
            is DynamicGeometryObject.TangentConic -> tangentLine(document, value.conicId, value.at, p).let { it.second - it.first }
            is DynamicGeometryObject.FixedAngle -> {
                val arm = p.getValue(value.armPoint) - p.getValue(value.vertex); val r = value.degrees * PI / 180
                Vec2(arm.x * cos(r) - arm.y * sin(r), arm.x * sin(r) + arm.y * cos(r))
            }
            else -> error("Relation compares line-like objects.")
        }
        val a = direction(first); val b = direction(second)
        val cross = abs(a.x * b.y - a.y * b.x); val dot = abs(a.x * b.x + a.y * b.y)
        return if (cross <= dot) GeometryRelation(cross < 1e-8, "parallel", cross, "Parallel directions have zero cross product.")
        else GeometryRelation(dot < 1e-8, "perpendicular", dot, "Perpendicular directions have zero dot product.")
    }

    private fun dependencies(rule: DynamicPointRule): List<String> = when (rule) {
        is DynamicPointRule.Free -> emptyList()
        is DynamicPointRule.Midpoint -> listOf(rule.first, rule.second)
        is DynamicPointRule.Section -> listOf(rule.first, rule.second)
        is DynamicPointRule.Centroid -> listOf(rule.first, rule.second, rule.third)
        is DynamicPointRule.Circumcenter -> listOf(rule.first, rule.second, rule.third)
        is DynamicPointRule.Incenter -> listOf(rule.first, rule.second, rule.third)
        is DynamicPointRule.Orthocenter -> listOf(rule.first, rule.second, rule.third)
        is DynamicPointRule.Translate -> listOf(rule.source)
        is DynamicPointRule.Rotate -> listOf(rule.source, rule.center)
        is DynamicPointRule.ReflectLine -> listOf(rule.source, rule.lineA, rule.lineB)
        is DynamicPointRule.Dilate -> listOf(rule.source, rule.center)
        is DynamicPointRule.LineIntersection -> listOf(rule.a1, rule.a2, rule.b1, rule.b2)
        is DynamicPointRule.PointOnObject -> listOf(rule.objectId)
        is DynamicPointRule.ObjectIntersection -> listOf(rule.firstObjectId, rule.secondObjectId)
        is DynamicPointRule.Transform -> listOf(rule.source) + rule.transformation.dependencies
    }
    private fun intersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Vec2 {
        val r = b - a; val s = d - c; val denominator = r.x * s.y - r.y * s.x
        require(abs(denominator) > 1e-12) { "Parallel lines do not have a unique intersection." }
        val q = c - a; return a + r * ((q.x * s.y - q.y * s.x) / denominator)
    }

    private fun circumcenter(a: Vec2, b: Vec2, c: Vec2): Vec2 {
        val denominator = 2.0 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        require(abs(denominator) > 1e-12) { "A circumcenter requires three non-collinear points." }
        val aa = a.x * a.x + a.y * a.y; val bb = b.x * b.x + b.y * b.y; val cc = c.x * c.x + c.y * c.y
        return Vec2(
            (aa * (b.y - c.y) + bb * (c.y - a.y) + cc * (a.y - b.y)) / denominator,
            (aa * (c.x - b.x) + bb * (a.x - c.x) + cc * (b.x - a.x)) / denominator,
        )
    }

    private fun incenter(a: Vec2, b: Vec2, c: Vec2): Vec2 {
        val oppositeA = b.distanceTo(c); val oppositeB = a.distanceTo(c); val oppositeC = a.distanceTo(b)
        val perimeter = oppositeA + oppositeB + oppositeC
        require(perimeter > 1e-12 && abs((b - a).x * (c - a).y - (b - a).y * (c - a).x) > 1e-12) { "An incenter requires a non-degenerate triangle." }
        return (a * oppositeA + b * oppositeB + c * oppositeC) * (1.0 / perimeter)
    }
    private fun lineCircle(a: Vec2, b: Vec2, center: Vec2, radius: Double): List<Vec2> {
        val d = b - a; val f = a - center
        val aa = d.x * d.x + d.y * d.y; val bb = 2 * (f.x * d.x + f.y * d.y); val cc = f.x * f.x + f.y * f.y - radius * radius
        val discriminant = bb * bb - 4 * aa * cc
        if (discriminant < -1e-10) return emptyList()
        if (abs(discriminant) <= 1e-10) return listOf(a + d * (-bb / (2 * aa)))
        val root = sqrt(discriminant); return listOf(a + d * ((-bb - root) / (2 * aa)), a + d * ((-bb + root) / (2 * aa)))
    }
    private fun circleCircle(a: Vec2, ra: Double, b: Vec2, rb: Double): List<Vec2> {
        val d = a.distanceTo(b)
        if (d < 1e-12 || d > ra + rb + 1e-10 || d < abs(ra - rb) - 1e-10) return emptyList()
        val along = (ra * ra - rb * rb + d * d) / (2 * d); val heightSquared = ra * ra - along * along
        val unit = (b - a) * (1 / d); val base = a + unit * along
        if (heightSquared <= 1e-10) return listOf(base)
        val normal = Vec2(-unit.y, unit.x) * sqrt(heightSquared)
        return listOf(base + normal, base - normal)
    }
    private fun lineEllipse(ellipse: DynamicGeometryObject.Ellipse, a: Vec2, b: Vec2, p: Map<String, Vec2>): List<Vec2> {
        val f1 = p.getValue(ellipse.focusA); val f2 = p.getValue(ellipse.focusB); val through = p.getValue(ellipse.through)
        val center = (f1 + f2) * .5; val major = (through.distanceTo(f1) + through.distanceTo(f2)) / 2; val focal = f1.distanceTo(f2) / 2
        val minor = sqrt((major * major - focal * focal).coerceAtLeast(0.0)); if (minor < 1e-12) return emptyList()
        val direction = if (focal < 1e-12) Vec2(1.0, 0.0) else (f2 - f1) * (1 / (2 * focal)); val normal = Vec2(-direction.y, direction.x)
        fun local(point: Vec2): Vec2 { val delta = point - center; return Vec2(delta.x * direction.x + delta.y * direction.y, delta.x * normal.x + delta.y * normal.y) }
        fun world(point: Vec2): Vec2 {
            val alongMajor: Vec2 = direction * point.x
            val alongMinor: Vec2 = normal * point.y
            return center + alongMajor + alongMinor
        }
        val la = local(a); val ld = local(b) - la
        val aa = ld.x * ld.x / (major * major) + ld.y * ld.y / (minor * minor)
        val bb = 2 * (la.x * ld.x / (major * major) + la.y * ld.y / (minor * minor))
        val cc = la.x * la.x / (major * major) + la.y * la.y / (minor * minor) - 1
        val disc = bb * bb - 4 * aa * cc; if (disc < -1e-10) return emptyList()
        if (abs(disc) <= 1e-10) return listOf(world(la + ld * (-bb / (2 * aa))))
        val root = sqrt(disc); return listOf(world(la + ld * ((-bb - root) / (2 * aa))), world(la + ld * ((-bb + root) / (2 * aa))))
    }

    private fun pointOnObject(value: DynamicGeometryObject, parameter: Double, p: Map<String, Vec2>): Vec2 = when (value) {
        is DynamicGeometryObject.Line -> p.getValue(value.throughA) + (p.getValue(value.throughB) - p.getValue(value.throughA)) * parameter
        is DynamicGeometryObject.Parallel -> p.getValue(value.through) + (p.getValue(value.lineB) - p.getValue(value.lineA)) * parameter
        is DynamicGeometryObject.Perpendicular -> {
            val d = p.getValue(value.lineB) - p.getValue(value.lineA); p.getValue(value.through) + Vec2(-d.y, d.x) * parameter
        }
        is DynamicGeometryObject.FixedAngle -> {
            val origin = p.getValue(value.vertex); val arm = p.getValue(value.armPoint) - origin; val r = value.degrees * PI / 180
            origin + Vec2(arm.x * cos(r) - arm.y * sin(r), arm.x * sin(r) + arm.y * cos(r)) * parameter
        }
        is DynamicGeometryObject.Circle -> {
            val center = p.getValue(value.center); val radius = center.distanceTo(p.getValue(value.through)); val angle = parameter * 2 * PI
            center + Vec2(cos(angle), sin(angle)) * radius
        }
        is DynamicGeometryObject.Ellipse -> {
            val f1 = p.getValue(value.focusA); val f2 = p.getValue(value.focusB); val through = p.getValue(value.through)
            val center = (f1 + f2) * .5; val c = f1.distanceTo(f2) * .5; val major = (through.distanceTo(f1) + through.distanceTo(f2)) * .5
            val minor = sqrt((major * major - c * c).coerceAtLeast(0.0)); val axis = if (c < 1e-12) Vec2(1.0, 0.0) else (f2 - f1) * (1 / (2 * c)); val normal = Vec2(-axis.y, axis.x); val angle = parameter * 2 * PI
            center + axis * (major * cos(angle)) + normal * (minor * sin(angle))
        }
        is DynamicGeometryObject.Parabola -> {
            val frame = parabolaFrame(value, p); val u = parameter
            frame.vertex + frame.tangent * u + frame.axis * (u * u / (4 * frame.focalLength))
        }
        is DynamicGeometryObject.Hyperbola -> {
            val frame = hyperbolaFrame(value, p); val branch = if (parameter < 0) -1.0 else 1.0; val t = abs(parameter)
            frame.center + frame.axis * (branch * frame.a * cosh(t)) + frame.normal * (frame.b * sinh(parameter))
        }
        is DynamicGeometryObject.TangentCircle -> error("Point-on-object requires circle '${value.circleId}', not its tangent wrapper.")
        is DynamicGeometryObject.TangentConic -> error("Point-on-object requires conic '${value.conicId}', not its tangent wrapper.")
        is DynamicGeometryObject.Locus -> value.samples.also { require(it.isNotEmpty()) }[(parameter.coerceIn(0.0, 1.0) * (value.samples.size - 1)).toInt()]
        is DynamicGeometryObject.Trace -> value.samples.also { require(it.isNotEmpty()) }[(parameter.coerceIn(0.0, 1.0) * (value.samples.size - 1)).toInt()]
    }

    private data class ParabolaFrame(val vertex: Vec2, val axis: Vec2, val tangent: Vec2, val focalLength: Double)
    private fun parabolaFrame(value: DynamicGeometryObject.Parabola, p: Map<String, Vec2>): ParabolaFrame {
        val focus = p.getValue(value.focus); val a = p.getValue(value.directrixA); val b = p.getValue(value.directrixB); val d = b - a
        require(d.x * d.x + d.y * d.y > 1e-20) { "Parabola directrix requires two distinct points." }
        val tangent = d * (1 / hypot(d.x, d.y)); val projection = a + tangent * ((focus - a).x * tangent.x + (focus - a).y * tangent.y)
        val delta = focus - projection; val distance = hypot(delta.x, delta.y); require(distance > 1e-12) { "Focus cannot lie on the directrix." }
        return ParabolaFrame((focus + projection) * .5, delta * (1 / distance), tangent, distance * .5)
    }

    private data class HyperbolaFrame(val center: Vec2, val axis: Vec2, val normal: Vec2, val a: Double, val b: Double)
    private fun hyperbolaFrame(value: DynamicGeometryObject.Hyperbola, p: Map<String, Vec2>): HyperbolaFrame {
        val f1 = p.getValue(value.focusA); val f2 = p.getValue(value.focusB); val through = p.getValue(value.through); val center = (f1 + f2) * .5
        val c = f1.distanceTo(f2) * .5; require(c > 1e-12) { "Hyperbola foci must be distinct." }
        val a = abs(through.distanceTo(f1) - through.distanceTo(f2)) * .5; require(a > 1e-12 && a < c) { "Hyperbola point must define 0 < a < c." }
        val axis = (f2 - f1) * (1 / (2 * c)); return HyperbolaFrame(center, axis, Vec2(-axis.y, axis.x), a, sqrt(c * c - a * a))
    }

    private fun tangentLine(document: DynamicGeometryDocument, conicId: String, atId: String, p: Map<String, Vec2>): Pair<Vec2, Vec2> {
        val conic = document.objects.firstOrNull { it.id == conicId } ?: error("Unknown conic '$conicId'."); val at = p.getValue(atId)
        val direction = when (conic) {
            is DynamicGeometryObject.Circle -> {
                val radial = at - p.getValue(conic.center); Vec2(-radial.y, radial.x)
            }
            is DynamicGeometryObject.Ellipse -> {
                val f1 = p.getValue(conic.focusA); val f2 = p.getValue(conic.focusB); val through = p.getValue(conic.through); val center = (f1 + f2) * .5
                val c = f1.distanceTo(f2) * .5; val major = (through.distanceTo(f1) + through.distanceTo(f2)) * .5; val minor = sqrt((major * major - c * c).coerceAtLeast(0.0))
                require(major > 1e-12 && minor > 1e-12); val axis = if (c < 1e-12) Vec2(1.0, 0.0) else (f2 - f1) * (1 / (2 * c)); val normal = Vec2(-axis.y, axis.x); val delta = at - center
                val x = delta.x * axis.x + delta.y * axis.y; val y = delta.x * normal.x + delta.y * normal.y
                axis * (-y / (minor * minor)) + normal * (x / (major * major))
            }
            is DynamicGeometryObject.Parabola -> {
                val frame = parabolaFrame(conic, p); val delta = at - frame.vertex; val x = delta.x * frame.tangent.x + delta.y * frame.tangent.y
                frame.tangent * (4 * frame.focalLength) + frame.axis * (2 * x)
            }
            is DynamicGeometryObject.Hyperbola -> {
                val frame = hyperbolaFrame(conic, p); val delta = at - frame.center; val x = delta.x * frame.axis.x + delta.y * frame.axis.y; val y = delta.x * frame.normal.x + delta.y * frame.normal.y
                frame.axis * (2 * y / (frame.b * frame.b)) + frame.normal * (2 * x / (frame.a * frame.a))
            }
            else -> error("Object '$conicId' is not a supported conic.")
        }
        require(hypot(direction.x, direction.y) > 1e-12) { "Tangent is undefined at '$atId'." }; return at to at + direction
    }

    private fun lineParabola(value: DynamicGeometryObject.Parabola, a: Vec2, b: Vec2, p: Map<String, Vec2>): List<Vec2> {
        val frame = parabolaFrame(value, p); val la = a - frame.vertex; val d = b - a
        val x0 = la.x * frame.tangent.x + la.y * frame.tangent.y; val y0 = la.x * frame.axis.x + la.y * frame.axis.y
        val dx = d.x * frame.tangent.x + d.y * frame.tangent.y; val dy = d.x * frame.axis.x + d.y * frame.axis.y
        return quadratic(dx * dx, 2 * x0 * dx - 4 * frame.focalLength * dy, x0 * x0 - 4 * frame.focalLength * y0).map { a + d * it }
    }

    private fun lineHyperbola(value: DynamicGeometryObject.Hyperbola, a: Vec2, b: Vec2, p: Map<String, Vec2>): List<Vec2> {
        val frame = hyperbolaFrame(value, p); val la = a - frame.center; val d = b - a
        val x0 = la.x * frame.axis.x + la.y * frame.axis.y; val y0 = la.x * frame.normal.x + la.y * frame.normal.y
        val dx = d.x * frame.axis.x + d.y * frame.axis.y; val dy = d.x * frame.normal.x + d.y * frame.normal.y
        return quadratic(dx * dx / (frame.a * frame.a) - dy * dy / (frame.b * frame.b), 2 * x0 * dx / (frame.a * frame.a) - 2 * y0 * dy / (frame.b * frame.b), x0 * x0 / (frame.a * frame.a) - y0 * y0 / (frame.b * frame.b) - 1).map { a + d * it }
    }

    private fun quadratic(a: Double, b: Double, c: Double): List<Double> {
        if (abs(a) < 1e-12) return if (abs(b) < 1e-12) emptyList() else listOf(-c / b)
        val disc = b * b - 4 * a * c; if (disc < -1e-10) return emptyList()
        if (abs(disc) <= 1e-10) return listOf(-b / (2 * a))
        val root = sqrt(disc); return listOf((-b - root) / (2 * a), (-b + root) / (2 * a))
    }
    private fun reflect(point: Vec2, a: Vec2, b: Vec2): Vec2 {
        val d = b - a; val scale = ((point - a).x * d.x + (point - a).y * d.y) / (d.x * d.x + d.y * d.y)
        return (a + d * scale) * 2.0 - point
    }
    private fun radical(value: Double): String {
        val rounded = value.toLong(); val root = sqrt(rounded.toDouble()).toLong()
        return if (abs(value - rounded) < 1e-9) if (root * root == rounded) root.toString() else "√" + rounded else "√" + format(value)
    }
    private fun fraction(n: Double, d: Double) = if (abs(n - n.toLong()) < 1e-9 && abs(d - d.toLong()) < 1e-9) n.toLong().toString() + "/" + d.toLong() else format(n) + "/" + format(d)
    private fun format(value: Double) = if (abs(value - value.toLong()) < 1e-9) value.toLong().toString() else String.format(java.util.Locale.US, "%.7f", value).trimEnd('0').trimEnd('.')
}
