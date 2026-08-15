package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Vec2
import kotlin.math.abs
import kotlin.math.round

/** Derives stable Algebra/Graph objects from geometry without duplicating the source construction. */
object GeometryGraphEquationProjector {
    fun project(state: WorkspaceState, document: UniversalMathDocument): UniversalMathDocument {
        val derived = (state.shapes.flatMap { shape -> derivedObjects(state, shape) } + state.pointDependencies.mapNotNull { pointRelation(state, it) }).associateBy { it.id }
        val staleIds = document.objects.keys.filter { it.endsWith(EQUATION_SUFFIX) || it.endsWith(MEASUREMENT_SUFFIX) || it.endsWith(RELATION_SUFFIX) }
        return document.copy(objects = (document.objects - staleIds.toSet()) + derived)
    }

    private fun derivedObjects(state: WorkspaceState, shape: Shape2D): List<UniversalMathObject> {
        val points = shape.pointIndices.mapNotNull(state.points::getOrNull)
        if (points.size != shape.pointIndices.size) return emptyList()
        val dependencies = setOf(shape.id)
        val objects = mutableListOf<UniversalMathObject>()
        when (shape.type) {
            Shape2DType.Line, Shape2DType.Segment, Shape2DType.Ray, Shape2DType.Vector -> if (points.size >= 2) {
                val equation = lineEquation(points[0], points[1])
                objects += equationObject(shape, equation.text, dependencies, equation.coefficients)
                val segment = Geometry2D.segment(points[0], points[1])
                objects += measurementObject(shape, "length", segment.exactDistance, segment.distance, dependencies)
            }
            Shape2DType.Circle -> if (points.size >= 2) {
                val center = points[0]; val through = points[1]; val radiusSquared = squareDistance(center, through)
                val equation = "(x - ${number(center.x)})^2 + (y - ${number(center.y)})^2 = ${number(radiusSquared)}"
                objects += equationObject(shape, equation, dependencies, mapOf("center.x" to center.x, "center.y" to center.y, "radiusSquared" to radiusSquared))
            }
            Shape2DType.CircleThreePoints -> if (points.size >= 3) {
                circleThroughThree(points[0], points[1], points[2])?.let { circle ->
                    val equation = "(x - ${number(circle.center.x)})^2 + (y - ${number(circle.center.y)})^2 = ${number(circle.radiusSquared)}"
                    objects += equationObject(shape, equation, dependencies, mapOf("center.x" to circle.center.x, "center.y" to circle.center.y, "radiusSquared" to circle.radiusSquared))
                }
            }
            Shape2DType.Ellipse -> if (points.size >= 3) {
                ellipseEquation(points[0], points[1], points[2])?.let { ellipse ->
                    objects += equationObject(shape, ellipse.text, dependencies, ellipse.values)
                    objects += measurementObject(shape, "majorAxis", number(2 * ellipse.a), 2 * ellipse.a, dependencies)
                }
            }
            Shape2DType.Parallel, Shape2DType.Perpendicular -> if (points.size >= 3) {
                val through = points[0]; val first = points[1]; val second = points[2]
                val direction = second - first
                val target = if (shape.type == Shape2DType.Parallel) through + direction else through + Vec2(-direction.y, direction.x)
                val equation = lineEquation(through, target)
                objects += equationObject(shape, equation.text, dependencies, equation.coefficients)
            }
            Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon -> if (points.size >= 3) {
                val area = Geometry2D.polygonArea(points)
                objects += measurementObject(shape, "area", number(area), area, dependencies)
            }
            else -> Unit
        }
        return objects
    }

    private fun pointRelation(state: WorkspaceState, dependency: PointDependency): UniversalMathObject? {
        val output = state.points.getOrNull(dependency.outputIndex) ?: return null
        val inputIds = dependency.inputIndices.map { "point-$it" }
        if (inputIds.size != dependency.inputIndices.size) return null
        val command = when (dependency.type) {
            PointDependencyType.Midpoint -> "Midpoint"
            PointDependencyType.Centroid -> "Centroid"
            PointDependencyType.Circumcenter -> "Circumcenter"
            PointDependencyType.Incenter -> "Incenter"
            PointDependencyType.Orthocenter -> "Orthocenter"
            PointDependencyType.Intersection -> "Intersect"
            PointDependencyType.PointOnObject -> "PointOnObject"
            PointDependencyType.TangentPoint -> "TangentPoint"
            PointDependencyType.Translate -> "Translate"
            PointDependencyType.Rotate -> "Rotate"
            PointDependencyType.ReflectX -> "Reflect"
            PointDependencyType.Dilate -> "Dilate"
        }
        val arguments = inputIds + dependency.parameters.map(::number)
        val relation = "point-${dependency.outputIndex} = $command(${arguments.joinToString()})"
        return UniversalMathObject(
            id = "point-${dependency.outputIndex}$RELATION_SUFFIX",
            kind = UniversalMathKind.Relation,
            name = "${dependency.name} relation",
            payload = UniversalMathPayload.Properties(mapOf("relation" to relation, "command" to command, "output" to "point-${dependency.outputIndex}")),
            dependencies = (inputIds + "point-${dependency.outputIndex}").toSet(),
            sourceView = "geometry/algebra/graph",
            definition = UniversalMathDefinition.Construction(command, arguments),
            valueState = UniversalMathValueState(values = mapOf(
                "x" to UniversalExactApproxValue(number(output.x), output.x, provenance = "$command construction", verification = UniversalVerificationStatus.Exact),
                "y" to UniversalExactApproxValue(number(output.y), output.y, provenance = "$command construction", verification = UniversalVerificationStatus.Exact),
            )),
        )
    }

    private fun equationObject(shape: Shape2D, equation: String, dependencies: Set<String>, values: Map<String, Double>) = UniversalMathObject(
        id = shape.id + EQUATION_SUFFIX,
        kind = UniversalMathKind.Equation,
        name = "Equation of ${shape.name}",
        payload = UniversalMathPayload.Properties(mapOf("equation" to equation, "sourceShape" to shape.id)),
        dependencies = dependencies,
        sourceView = "geometry/algebra/graph",
        definition = UniversalMathDefinition.Construction("Equation", listOf(shape.id)),
        valueState = UniversalMathValueState(values = values.mapValues { (key, value) ->
            UniversalExactApproxValue(number(value), value, provenance = "$key derived from ${shape.name}", verification = UniversalVerificationStatus.Exact)
        }),
        presentation = UniversalMathPresentation(visible = shape.visible, styleKey = shape.styleKey),
    )

    private fun measurementObject(shape: Shape2D, label: String, exact: String, decimal: Double, dependencies: Set<String>) = UniversalMathObject(
        id = shape.id + MEASUREMENT_SUFFIX,
        kind = UniversalMathKind.Measurement,
        name = "${shape.name} $label",
        payload = UniversalMathPayload.Properties(mapOf("measurement" to label, "exact" to exact, "decimal" to decimal.toString())),
        dependencies = dependencies,
        sourceView = "geometry/algebra",
        definition = UniversalMathDefinition.Construction(label.replaceFirstChar(Char::uppercase), listOf(shape.id)),
        valueState = UniversalMathValueState(values = mapOf(label to UniversalExactApproxValue(exact, decimal, provenance = "derived from ${shape.name}", verification = UniversalVerificationStatus.Exact))),
        presentation = UniversalMathPresentation(visible = shape.visible, styleKey = shape.styleKey),
    )

    private data class LineEquation(val text: String, val coefficients: Map<String, Double>)
    private fun lineEquation(first: Vec2, second: Vec2): LineEquation {
        require(first.distanceTo(second) > 1e-12) { "A line requires two distinct points" }
        var a = second.y - first.y; var b = first.x - second.x; var c = a * first.x + b * first.y
        val scale = listOf(abs(a), abs(b)).firstOrNull { it > 1e-12 } ?: 1.0
        a /= scale; b /= scale; c /= scale
        if (a < -1e-12 || abs(a) < 1e-12 && b < 0) { a = -a; b = -b; c = -c }
        return LineEquation("${number(a)}*x + ${number(b)}*y = ${number(c)}", mapOf("a" to a, "b" to b, "c" to c))
    }

    private data class CircleEquation(val center: Vec2, val radiusSquared: Double)
    private fun circleThroughThree(a: Vec2, b: Vec2, c: Vec2): CircleEquation? {
        val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        if (abs(d) < 1e-12) return null
        val aa = a.x * a.x + a.y * a.y; val bb = b.x * b.x + b.y * b.y; val cc = c.x * c.x + c.y * c.y
        val center = Vec2((aa * (b.y - c.y) + bb * (c.y - a.y) + cc * (a.y - b.y)) / d, (aa * (c.x - b.x) + bb * (a.x - c.x) + cc * (b.x - a.x)) / d)
        return CircleEquation(center, squareDistance(center, a))
    }

    private data class EllipseEquation(val text: String, val a: Double, val values: Map<String, Double>)
    private fun ellipseEquation(firstFocus: Vec2, secondFocus: Vec2, through: Vec2): EllipseEquation? {
        val center = (firstFocus + secondFocus) * .5
        val focalVector = secondFocus - firstFocus
        val focalDistance = focalVector.distanceTo(Vec2(0.0, 0.0)) / 2
        val a = (through.distanceTo(firstFocus) + through.distanceTo(secondFocus)) / 2
        if (a <= focalDistance + 1e-10) return null
        val a2 = a * a; val b2 = a2 - focalDistance * focalDistance
        val length = focalVector.distanceTo(Vec2(0.0, 0.0))
        val ux = if (length < 1e-12) 1.0 else focalVector.x / length
        val uy = if (length < 1e-12) 0.0 else focalVector.y / length
        val text = when {
            abs(uy) < 1e-10 -> "(x - ${number(center.x)})^2/${number(a2)} + (y - ${number(center.y)})^2/${number(b2)} = 1"
            abs(ux) < 1e-10 -> "(x - ${number(center.x)})^2/${number(b2)} + (y - ${number(center.y)})^2/${number(a2)} = 1"
            else -> "(((x - ${number(center.x)})*${number(ux)} + (y - ${number(center.y)})*${number(uy)})^2)/${number(a2)} + (((x - ${number(center.x)})*${number(-uy)} + (y - ${number(center.y)})*${number(ux)})^2)/${number(b2)} = 1"
        }
        return EllipseEquation(text, a, mapOf("center.x" to center.x, "center.y" to center.y, "aSquared" to a2, "bSquared" to b2, "focusDistance" to focalDistance))
    }

    private fun squareDistance(a: Vec2, b: Vec2) = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
    private fun number(value: Double): String {
        val clean = if (abs(value) < 1e-12) 0.0 else value; val whole = round(clean)
        return if (abs(clean - whole) < 1e-10) whole.toLong().toString() else String.format(java.util.Locale.US, "%.10f", clean).trimEnd('0').trimEnd('.')
    }

    private const val EQUATION_SUFFIX = ":equation"
    private const val MEASUREMENT_SUFFIX = ":measurement"
    private const val RELATION_SUFFIX = ":relation"
}
