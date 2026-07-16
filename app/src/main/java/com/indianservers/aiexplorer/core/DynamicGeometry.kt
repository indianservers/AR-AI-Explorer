package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface DynamicPointRule {
    data class Free(val position: Vec2) : DynamicPointRule
    data class Midpoint(val first: String, val second: String) : DynamicPointRule
    data class Translate(val source: String, val dx: Double, val dy: Double) : DynamicPointRule
    data class Rotate(val source: String, val center: String, val degrees: Double) : DynamicPointRule
    data class ReflectLine(val source: String, val lineA: String, val lineB: String) : DynamicPointRule
    data class Dilate(val source: String, val center: String, val scale: Double) : DynamicPointRule
    data class LineIntersection(val a1: String, val a2: String, val b1: String, val b2: String) : DynamicPointRule
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
    data class TangentCircle(override val id: String, val circleId: String, val at: String) : DynamicGeometryObject { override val dependencies = listOf(circleId, at) }
    data class FixedAngle(override val id: String, val vertex: String, val armPoint: String, val degrees: Double) : DynamicGeometryObject { override val dependencies = listOf(vertex, armPoint) }
    data class Locus(override val id: String, val driver: String, val dependent: String, val samples: List<Vec2>) : DynamicGeometryObject { override val dependencies = listOf(driver, dependent) }
}

data class ProtocolStep(val id: String, val title: String, val dependencies: List<String>, val reason: String, val visible: Boolean = true)
data class DynamicGeometryDocument(
    val points: List<DynamicPoint> = emptyList(),
    val objects: List<DynamicGeometryObject> = emptyList(),
    val protocol: List<ProtocolStep> = emptyList(),
)
data class ExactGeometryMeasurement(val label: String, val exact: String, val decimal: Double, val unit: String)
data class GeometryRelation(val holds: Boolean, val statement: String, val residual: Double, val reason: String)

class DynamicGeometryEngine {
    fun addPoint(document: DynamicGeometryDocument, point: DynamicPoint, reason: String): DynamicGeometryDocument {
        require(document.points.none { it.id == point.id })
        val updated = document.copy(
            points = document.points + point,
            protocol = document.protocol + ProtocolStep(point.id, "Create point " + point.label, dependencies(point.rule), reason),
        )
        resolve(updated)
        return updated
    }

    fun addObject(document: DynamicGeometryDocument, value: DynamicGeometryObject, reason: String): DynamicGeometryDocument {
        require(document.objects.none { it.id == value.id })
        val updated = document.copy(
            objects = document.objects + value,
            protocol = document.protocol + ProtocolStep(value.id, "Create " + value.javaClass.simpleName, value.dependencies, reason),
        )
        resolve(updated)
        return updated
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
            val value = when (rule) {
                is DynamicPointRule.Free -> rule.position
                is DynamicPointRule.Midpoint -> (p(rule.first) + p(rule.second)) * .5
                is DynamicPointRule.Translate -> p(rule.source) + Vec2(rule.dx, rule.dy)
                is DynamicPointRule.Rotate -> {
                    val source = p(rule.source); val center = p(rule.center); val radians = rule.degrees * PI / 180
                    val delta = source - center
                    center + Vec2(delta.x * cos(radians) - delta.y * sin(radians), delta.x * sin(radians) + delta.y * cos(radians))
                }
                is DynamicPointRule.ReflectLine -> reflect(p(rule.source), p(rule.lineA), p(rule.lineB))
                is DynamicPointRule.Dilate -> p(rule.center) + (p(rule.source) - p(rule.center)) * rule.scale
                is DynamicPointRule.LineIntersection -> intersect(p(rule.a1), p(rule.a2), p(rule.b1), p(rule.b2))
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
            protocol = document.protocol.mapIndexed { index, step -> step.copy(visible = index < count) },
        )
    }

    fun setStepVisible(document: DynamicGeometryDocument, id: String, visible: Boolean) =
        document.copy(protocol = document.protocol.map { if (it.id == id) it.copy(visible = visible) else it })

    fun dependencyTree(document: DynamicGeometryDocument): Map<String, List<String>> = document.protocol.associate { it.id to it.dependencies }

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
        val points = resolve(document)
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
        if (ellipse != null && otherLine != null) return lineEllipse(document, ellipse, otherLine.first, otherLine.second)
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
        is DynamicPointRule.Translate -> listOf(rule.source)
        is DynamicPointRule.Rotate -> listOf(rule.source, rule.center)
        is DynamicPointRule.ReflectLine -> listOf(rule.source, rule.lineA, rule.lineB)
        is DynamicPointRule.Dilate -> listOf(rule.source, rule.center)
        is DynamicPointRule.LineIntersection -> listOf(rule.a1, rule.a2, rule.b1, rule.b2)
    }
    private fun intersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Vec2 {
        val r = b - a; val s = d - c; val denominator = r.x * s.y - r.y * s.x
        require(abs(denominator) > 1e-12) { "Parallel lines do not have a unique intersection." }
        val q = c - a; return a + r * ((q.x * s.y - q.y * s.x) / denominator)
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
    private fun lineEllipse(document: DynamicGeometryDocument, ellipse: DynamicGeometryObject.Ellipse, a: Vec2, b: Vec2): List<Vec2> {
        val p = resolve(document); val f1 = p.getValue(ellipse.focusA); val f2 = p.getValue(ellipse.focusB); val through = p.getValue(ellipse.through)
        val center = (f1 + f2) * .5; val major = (through.distanceTo(f1) + through.distanceTo(f2)) / 2; val focal = f1.distanceTo(f2) / 2
        val minor = sqrt((major * major - focal * focal).coerceAtLeast(0.0)); if (minor < 1e-12) return emptyList()
        val direction = if (focal < 1e-12) Vec2(1.0, 0.0) else (f2 - f1) * (1 / (2 * focal)); val normal = Vec2(-direction.y, direction.x)
        fun local(point: Vec2): Vec2 { val delta = point - center; return Vec2(delta.x * direction.x + delta.y * direction.y, delta.x * normal.x + delta.y * normal.y) }
        fun world(point: Vec2) = center + direction * point.x + normal * point.y
        val la = local(a); val ld = local(b) - la
        val aa = ld.x * ld.x / (major * major) + ld.y * ld.y / (minor * minor)
        val bb = 2 * (la.x * ld.x / (major * major) + la.y * ld.y / (minor * minor))
        val cc = la.x * la.x / (major * major) + la.y * la.y / (minor * minor) - 1
        val disc = bb * bb - 4 * aa * cc; if (disc < -1e-10) return emptyList()
        if (abs(disc) <= 1e-10) return listOf(world(la + ld * (-bb / (2 * aa))))
        val root = sqrt(disc); return listOf(world(la + ld * ((-bb - root) / (2 * aa))), world(la + ld * ((-bb + root) / (2 * aa))))
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
