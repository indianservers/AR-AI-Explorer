package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.CurveSample
import com.indianservers.aiexplorer.core.DistributionEngine
import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.DistributionPoint
import com.indianservers.aiexplorer.core.DistributionSummary
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.ProbabilityDistribution
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import kotlin.math.abs
import kotlin.math.round

enum class LinkedMathView { CAS, Algebra, Graph, Table, Geometry, Probability }
enum class LinkedMathObjectKind { Function, GeometryShape, ProbabilityDistribution }

data class ExactNumericValue(
    val exact: String,
    val decimal: Double,
    val provenance: String,
)

data class LinkedTableRow(
    val input: ExactNumericValue,
    val output: ExactNumericValue,
)

data class LinkedGraphData(
    val expression: String,
    val sample: CurveSample,
    val roots: List<ExactNumericValue>,
    val extrema: List<Vec2>,
)

data class LinkedAlgebraData(
    val canonicalExpression: String,
    val exactClassification: String,
    val exactFacts: List<String>,
)

data class LinkedGeometryData(
    val shapeId: String,
    val shapeName: String,
    val shapeType: Shape2DType,
    val measurements: Map<String, ExactNumericValue>,
)

data class LinkedProbabilityData(
    val distribution: ProbabilityDistribution,
    val summary: DistributionSummary,
    val interval: ClosedFloatingPointRange<Double>,
    val intervalProbability: ExactNumericValue,
    val plot: List<DistributionPoint>,
)

data class LinkedMathObject(
    val id: String,
    val name: String,
    val kind: LinkedMathObjectKind,
    val views: Set<LinkedMathView>,
    val algebra: LinkedAlgebraData? = null,
    val graph: LinkedGraphData? = null,
    val table: List<LinkedTableRow> = emptyList(),
    val geometry: LinkedGeometryData? = null,
    val probability: LinkedProbabilityData? = null,
)

data class LinkedMathSnapshot(
    val universalDocument: UniversalMathDocument,
    val objects: List<LinkedMathObject>,
    val diagnostics: List<String>,
) {
    fun objectsFor(view: LinkedMathView): List<LinkedMathObject> = objects.filter { view in it.views }
    fun objectByName(name: String): LinkedMathObject? = objects.firstOrNull { it.name == name }
}

data class ProbabilityLinkRequest(
    val kind: DistributionKind = DistributionKind.Normal,
    val first: Double = 0.0,
    val second: Double = 1.0,
    val lower: Double = -1.0,
    val upper: Double = 1.0,
)

class LinkedMathKernel(
    private val expressionEngine: ExpressionEngine = ExpressionEngine(),
    private val graphAnalysis: GraphAnalysis = GraphAnalysis(expressionEngine),
) {
    fun snapshot(
        state: WorkspaceState,
        tableInputs: List<Double> = listOf(-2.0, -1.0, 0.0, 1.0, 2.0),
        probability: ProbabilityLinkRequest? = ProbabilityLinkRequest(),
    ): LinkedMathSnapshot {
        val diagnostics = mutableListOf<String>()
        var universalDocument = UniversalWorkspaceBridge.fromWorkspace(state)
        diagnostics += UniversalMathDocumentEngine().validate(universalDocument).diagnostics
        val objects = mutableListOf<LinkedMathObject>()
        state.functions.forEach { function ->
            runCatching { functionObject(function, tableInputs) }
                .onSuccess(objects::add)
                .onFailure { diagnostics += "${function.name}: ${it.message ?: "could not link function"}" }
        }
        state.shapes.forEach { shape ->
            geometryObject(state, shape)?.let(objects::add)
        }
        if (probability != null) {
            runCatching { probabilityObject(probability) }
                .onSuccess { linked ->
                    objects += linked
                    val universal = UniversalMathObjectFactory.probability(
                        linked.id, linked.name, probability.kind.name,
                        mapOf("first" to probability.first, "second" to probability.second, "lower" to probability.lower, "upper" to probability.upper),
                    )
                    universalDocument = universalDocument.copy(objects = universalDocument.objects + (universal.id to universal))
                }
                .onFailure { diagnostics += "Probability: ${it.message ?: "could not link distribution"}" }
        }
        return LinkedMathSnapshot(universalDocument, objects, diagnostics)
    }

    private fun functionObject(function: FunctionDefinition, tableInputs: List<Double>): LinkedMathObject {
        val expression = stripEquation(function.expression)
        val compiled = expressionEngine.compile(expression)
        val algebra = algebraData(expression)
        val table = tableInputs.map { x ->
            LinkedTableRow(
                input = exactNumber(x, "table input"),
                output = exactNumber(compiled.eval(mapOf("x" to x)), "evaluated ${function.name} at x=${trim(x)}"),
            )
        }
        val graph = LinkedGraphData(
            expression = expression,
            sample = graphAnalysis.sampleDefinition(expression, -6.0, 6.0, 160),
            roots = graphAnalysis.roots(expression, -10.0, 10.0).map { exactNumber(it, "graph root refined numerically") },
            extrema = graphAnalysis.extrema(expression, -6.0, 6.0),
        )
        return LinkedMathObject(
            id = function.id,
            name = function.name,
            kind = LinkedMathObjectKind.Function,
            views = setOf(LinkedMathView.CAS, LinkedMathView.Algebra, LinkedMathView.Graph, LinkedMathView.Table),
            algebra = algebra,
            graph = graph,
            table = table,
        )
    }

    private fun algebraData(expression: String): LinkedAlgebraData {
        val canonical = expression.replace(Regex("\\s+"), "")
        val coefficients = quadraticCoefficients(canonical)
        if (coefficients != null) {
            val (a, b, c) = coefficients
            val discriminant = b * b - 4.0 * a * c
            val facts = mutableListOf("a=${exactNumber(a, "coefficient").exact}", "b=${exactNumber(b, "coefficient").exact}", "c=${exactNumber(c, "coefficient").exact}", "Delta=${exactNumber(discriminant, "exact discriminant").exact}")
            if (abs(discriminant) < 1e-12) facts += "one repeated real root"
            else if (discriminant > 0.0) facts += "two real roots"
            else facts += "complex conjugate roots"
            return LinkedAlgebraData(canonical, "quadratic polynomial", facts)
        }
        if (Regex("^[+-]?\\d+(?:\\.\\d+)?(?:\\*?x)?(?:[+-]\\d+(?:\\.\\d+)?)?$").matches(canonical)) {
            return LinkedAlgebraData(canonical, "linear or constant expression", listOf("numeric table and graph samples share this expression"))
        }
        return LinkedAlgebraData(canonical, "numeric expression tree", listOf("compiled once and shared by graph/table views"))
    }

    private fun geometryObject(state: WorkspaceState, shape: Shape2D): LinkedMathObject? {
        val points = shape.pointIndices.map { state.points.getOrNull(it) ?: return null }
        val measurements = mutableMapOf<String, ExactNumericValue>()
        when (shape.type) {
            Shape2DType.Segment, Shape2DType.Line, Shape2DType.Ray, Shape2DType.Vector -> if (points.size >= 2) {
                val segment = Geometry2D.segment(points[0], points[1])
                measurements["distance"] = ExactNumericValue(segment.exactDistance, segment.distance, "computed from parent points ${shape.pointIndices}")
                measurements["midpoint.x"] = exactNumber(segment.midpoint.x, "midpoint x")
                measurements["midpoint.y"] = exactNumber(segment.midpoint.y, "midpoint y")
                segment.slope?.let { measurements["slope"] = exactNumber(it, "dy/dx") }
            }
            Shape2DType.Triangle -> if (points.size >= 3) {
                measurements["area"] = exactNumber(Geometry2D.polygonArea(points.take(3)), "shoelace area")
                measurements["angle.A"] = exactNumber(Geometry2D.angle(points[1], points[0], points[2]), "angle at first vertex")
            }
            Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon -> {
                measurements["area"] = exactNumber(Geometry2D.polygonArea(points), "shoelace area")
            }
            else -> Unit
        }
        if (measurements.isEmpty()) return null
        return LinkedMathObject(
            id = shape.id,
            name = shape.name,
            kind = LinkedMathObjectKind.GeometryShape,
            views = setOf(LinkedMathView.Algebra, LinkedMathView.Geometry),
            geometry = LinkedGeometryData(shape.id, shape.name, shape.type, measurements),
        )
    }

    private fun probabilityObject(request: ProbabilityLinkRequest): LinkedMathObject {
        val distribution = DistributionEngine.create(request.kind, request.first, request.second)
        val lower = minOf(request.lower, request.upper)
        val upper = maxOf(request.lower, request.upper)
        val probability = distribution.probabilityBetween(lower, upper)
        return LinkedMathObject(
            id = "probability-${request.kind.name.lowercase()}",
            name = "${request.kind.name} distribution",
            kind = LinkedMathObjectKind.ProbabilityDistribution,
            views = setOf(LinkedMathView.Algebra, LinkedMathView.Graph, LinkedMathView.Table, LinkedMathView.Probability),
            probability = LinkedProbabilityData(
                distribution = distribution,
                summary = distribution.summary,
                interval = lower..upper,
                intervalProbability = ExactNumericValue(trim(probability), probability, "CDF($upper) - CDF($lower)"),
                plot = distribution.plotPoints(80),
            ),
            table = distribution.plotPoints(12).map {
                LinkedTableRow(exactNumber(it.x, "distribution x"), exactNumber(it.probability, "density or mass"))
            },
            algebra = LinkedAlgebraData(
                canonicalExpression = request.kind.name,
                exactClassification = "${distribution.summary.domain.name.lowercase()} probability distribution",
                exactFacts = listOf("mean=${trim(distribution.summary.mean)}", "variance=${trim(distribution.summary.variance)}"),
            ),
        )
    }

    private fun quadraticCoefficients(expression: String): Triple<Double, Double, Double>? {
        if (!expression.contains("x")) return null
        val samples = listOf(0.0, 1.0, 2.0)
        val values = samples.map { x -> runCatching { expressionEngine.compile(expression).eval(mapOf("x" to x)) }.getOrNull() ?: return null }
        val c = values[0]
        val a = (values[2] - 2.0 * values[1] + values[0]) / 2.0
        val b = values[1] - a - c
        val check = runCatching { expressionEngine.compile(expression).eval(mapOf("x" to -1.5)) }.getOrNull() ?: return null
        val predicted = a * 2.25 - 1.5 * b + c
        return if (abs(check - predicted) < 1e-7 && abs(a) > 1e-12) Triple(a, b, c) else null
    }

    private fun exactNumber(value: Double, provenance: String): ExactNumericValue {
        val clean = if (abs(value) < 1e-12) 0.0 else value
        val whole = round(clean)
        if (abs(clean - whole) < 1e-10) return ExactNumericValue(whole.toLong().toString(), clean, provenance)
        val rational = smallRational(clean)
        return ExactNumericValue(rational ?: trim(clean), clean, provenance)
    }

    private fun smallRational(value: Double): String? {
        for (denominator in 1..64) {
            val numerator = round(value * denominator).toLong()
            if (abs(value - numerator.toDouble() / denominator) < 1e-10) return "$numerator/$denominator"
        }
        return null
    }
}
