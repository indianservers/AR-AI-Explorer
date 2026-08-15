package com.indianservers.aiexplorer.solver.domain.visualisation

import com.indianservers.aiexplorer.core.CalculatorComplex
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SolverVisualisationGenerator(
    private val expressions: ExpressionEngine = ExpressionEngine(),
    private val cache: SolverVisualisationCache = SolverVisualisationCache(),
) {
    fun generate(solution: SolverSolution): List<VisualisationSpec> {
        if (!solution.supported || solution.steps.isEmpty()) return emptyList()
        val cacheKey = "${solution.input.normalized}|${solution.finalAnswer}|${solution.steps.size}"
        cache[cacheKey]?.let { return it }
        val linked = solution.steps.map { it.id }
        val specific = when (solution.classification.type) {
            ProblemType.ArithmeticExpression -> arithmetic(solution, linked)
            ProblemType.FractionSimplification -> fraction(solution, linked)
            ProblemType.PercentageProblem -> percentage(solution, linked)
            ProblemType.RatioOrProportion -> ratio(solution, linked)
            ProblemType.AlgebraicSimplification, ProblemType.Expansion, ProblemType.Factorisation ->
                algebraTiles(solution, linked)
            ProblemType.LinearEquation -> balance(solution, linked)
            ProblemType.LinearInequality -> inequality(solution, linked)
            ProblemType.SimultaneousLinearEquations -> graph(solution, linked, VisualisationType.FunctionIntersection)
            ProblemType.PolynomialOperation, ProblemType.QuadraticEquation ->
                graph(solution, linked, VisualisationType.QuadraticGeometry)
            ProblemType.FunctionAnalysis, ProblemType.CoordinateGeometry ->
                graph(solution, linked, VisualisationType.CoordinateGraph)
            ProblemType.TrigonometricProblem -> unitCircle(solution, linked)
            ProblemType.SequenceOrSeries -> sequence(solution, linked)
            ProblemType.MatrixOperation -> matrix(solution, linked)
            ProblemType.Calculus -> calculus(solution, linked)
            ProblemType.ComplexNumbers -> complex(solution, linked)
            ProblemType.NumberTheory, ProblemType.RationalOrRadicalEquation,
            ProblemType.Probability, ProblemType.Statistics, ProblemType.UnsupportedOrAmbiguous -> null
        }
        val transformations = solution.steps.mapIndexed { index, step ->
            VisualisationSpec(
                id = "transformation-${index + 1}",
                type = VisualisationType.TransformationHighlight,
                title = "What changed in step ${index + 1}",
                linkedStepIds = listOf(step.id),
                mathematicalData = VisualisationData.TransformationHighlight(
                    SolverExpressionRenderer.render(step.before),
                    SolverExpressionRenderer.render(step.after),
                    step.optionalDetails.map { it.value }.ifEmpty { listOf(step.explanation) },
                ),
                interactionMode = InteractionMode.StepLinked,
                accessibilityDescription = "Step ${index + 1}. Before: ${SolverExpressionRenderer.render(step.before)}. After: ${SolverExpressionRenderer.render(step.after)}. ${step.explanation}",
                explanationKeys = listOf(step.explanationKey),
            )
        }
        val verification = visualVerification(solution, linked)
        return buildList {
            specific?.let(::add)
            addAll(transformations)
            verification?.let(::add)
        }.also { cache[cacheKey] = it }
    }

    fun verification(solution: SolverSolution, specs: List<VisualisationSpec>): VisualVerificationResult {
        if (!solution.supported || specs.isEmpty()) {
            return VisualVerificationResult(false, false, "No mathematically supported visual verification is available.", emptyList())
        }
        val checks = specs.flatMap { spec ->
            when (val data = spec.mathematicalData) {
                is VisualisationData.VerificationComparison -> data.checks
                is VisualisationData.DerivativeTangent -> listOf(
                    VisualVerificationDatum("Tangent passes through selected point", data.point.y, interpolate(data.tangent.points, data.point.x), 1e-6),
                )
                is VisualisationData.IntegralArea -> listOf(
                    VisualVerificationDatum("Rectangle sum matches displayed area", data.signedArea, data.rectangles.sumOf { (it.right - it.left) * it.height }, max(0.08, abs(data.signedArea) * .08)),
                )
                is VisualisationData.UnitCircle -> listOf(
                    VisualVerificationDatum("sin squared plus cos squared", 1.0, data.sine * data.sine + data.cosine * data.cosine, 1e-9),
                )
                else -> emptyList()
            }
        }
        return VisualVerificationResult(
            supported = checks.isNotEmpty(),
            consistent = checks.isNotEmpty() && checks.all(VisualVerificationDatum::passed),
            summary = when {
                checks.isEmpty() -> "The visual explains the transformation; symbolic verification remains authoritative."
                checks.all(VisualVerificationDatum::passed) -> "Visual checks agree with the independently verified symbolic result."
                else -> "A visual check disagrees with the symbolic result, so the visual must not be treated as proof."
            },
            checks = checks,
        )
    }

    private fun arithmetic(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val match = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([+-])\s*(-?\d+(?:\.\d+)?)\s*$""").matchEntire(solution.input.normalized)
            ?: return null
        val start = match.groupValues[1].toDouble()
        val delta = match.groupValues[3].toDouble() * if (match.groupValues[2] == "-") -1 else 1
        val end = start + delta
        return VisualisationSpec(
            "number-line", VisualisationType.NumberLine, "Move on the number line", linked,
            VisualisationData.NumberLine(min(-10.0, min(start, end) - 1), max(10.0, max(start, end) + 1), start, end),
            InteractionMode.Scrubbable,
            "Number line movement from ${number(start)} to ${number(end)} by ${number(delta)}.",
            listOf("exact-arithmetic"),
        )
    }

    private fun fraction(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val fractions = Regex("""(-?\d+)\s*/\s*(\d+)""").findAll(solution.input.normalized).take(2).toList()
        val first = fractions.firstOrNull() ?: return null
        val second = fractions.getOrNull(1)
        return VisualisationSpec(
            "fraction-area", VisualisationType.FractionArea, "Fractions share one whole", linked,
            VisualisationData.FractionArea(
                first.groupValues[1].toInt(), first.groupValues[2].toInt(),
                second?.groupValues?.get(1)?.toInt(), second?.groupValues?.get(2)?.toInt(),
            ),
            InteractionMode.StepLinked,
            "Partitioned area model for ${first.value}${second?.let { " and ${it.value}" }.orEmpty()}.",
            listOf("fraction-reduction", "least-common-denominator"),
        )
    }

    private fun percentage(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val percentage = Regex("""(-?\d+(?:\.\d+)?)\s*%""").find(solution.input.normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return null
        return VisualisationSpec(
            "percentage-bar", VisualisationType.PercentageBar, "Percentage of one whole", linked,
            VisualisationData.PercentageBar(percentage), InteractionMode.Inspectable,
            "${number(percentage)} percent shown on a bar divided into one hundred equal parts.",
            listOf("percentage-definition"),
        )
    }

    private fun ratio(solution: SolverSolution, linked: List<String>) = VisualisationSpec(
        "ratio-table", VisualisationType.RatioTable, "Equivalent ratio scaling", linked,
        VisualisationData.RatioTable(listOf("Quantity A", "Quantity B"), listOf(listOf("a", "b"), listOf("ka", "kb"))),
        InteractionMode.StepLinked,
        "A ratio table showing that both quantities scale by the same factor.",
        listOf("proportion-cross-product"),
    )

    private fun algebraTiles(solution: SolverSolution, linked: List<String>): VisualisationSpec {
        val source = solution.input.normalized.replace(" ", "")
        val positiveVariables = Regex("""(?<!-)(?:\d+\*?)?x""").findAll(source).count().coerceAtLeast(1)
        val negativeVariables = Regex("""-(?:\d+\*?)?x""").findAll(source).count()
        val units = Regex("""(?<![\^A-Za-z])[-+]?\d+""").findAll(source).mapNotNull { it.value.toIntOrNull() }.toList()
        return VisualisationSpec(
            "algebra-tiles", VisualisationType.AlgebraTiles, "Terms as algebra tiles", linked,
            VisualisationData.AlgebraTiles(positiveVariables, negativeVariables, units.filter { it > 0 }.sum(), -units.filter { it < 0 }.sum()),
            InteractionMode.StepLinked,
            "Algebra tiles group variable tiles separately from positive and negative unit tiles.",
            listOf("combine-like-terms", "distributive-property"),
        )
    }

    private fun balance(solution: SolverSolution, linked: List<String>): VisualisationSpec {
        val sides = solution.input.normalized.split('=', limit = 2)
        return VisualisationSpec(
            "balance-scale", VisualisationType.BalanceScale, "Equality stays balanced", linked,
            VisualisationData.BalanceScale(sides.firstOrNull().orEmpty(), sides.getOrNull(1).orEmpty(), 1.0, 1.0),
            InteractionMode.StepLinked,
            "A balanced scale with ${sides.firstOrNull().orEmpty()} on the left and ${sides.getOrNull(1).orEmpty()} on the right.",
            listOf("addition-property-equality", "division-property-equality"),
        )
    }

    private fun inequality(solution: SolverSolution, linked: List<String>): VisualisationSpec {
        val boundary = Regex("""-?\d+(?:\.\d+)?""").findAll(solution.finalAnswer.orEmpty()).lastOrNull()?.value?.toDoubleOrNull() ?: 0.0
        val closed = solution.finalAnswer.orEmpty().contains("<=") || solution.finalAnswer.orEmpty().contains(">=")
        return VisualisationSpec(
            "inequality-line", VisualisationType.NumberLine, "Solution interval", linked,
            VisualisationData.NumberLine(boundary - 8, boundary + 8, boundary, if (solution.finalAnswer.orEmpty().contains(">")) boundary + 7 else boundary - 7, boundary, closed),
            InteractionMode.Inspectable,
            "Number line solution with a ${if (closed) "closed" else "open"} boundary at ${number(boundary)}.",
            listOf("inequality-negative"),
        )
    }

    private fun graph(solution: SolverSolution, linked: List<String>, type: VisualisationType): VisualisationSpec? {
        val source = residualExpression(solution.input.normalized)
        val series = sample(source, -8.0, 8.0, 129) ?: return null
        return VisualisationSpec(
            "coordinate-graph", type, if (type == VisualisationType.QuadraticGeometry) "Symbolic roots on the parabola" else "Coordinate interpretation",
            linked, VisualisationData.CoordinateGraph(-8.0, 8.0, -10.0, 10.0, listOf(series)),
            InteractionMode.Scrubbable,
            "Coordinate graph of $source. Gaps are preserved where the expression is undefined.",
            listOf("graph-interpretation"),
            domainStatement = "Only finite samples in x from -8 to 8 are joined.",
        )
    }

    private fun unitCircle(solution: SolverSolution, linked: List<String>): VisualisationSpec {
        val degrees = Regex("""-?\d+(?:\.\d+)?""").find(solution.input.normalized)?.value?.toDoubleOrNull() ?: 0.0
        val angle = degrees * PI / 180.0
        return VisualisationSpec(
            "unit-circle", VisualisationType.UnitCircle, "Angle on the unit circle", linked,
            VisualisationData.UnitCircle(angle, sin(angle), cos(angle)), InteractionMode.Scrubbable,
            "Unit circle at ${number(degrees)} degrees. Horizontal coordinate ${number(cos(angle))}; vertical coordinate ${number(sin(angle))}.",
            listOf("pythagorean-identity"),
        )
    }

    private fun sequence(solution: SolverSolution, linked: List<String>): VisualisationSpec {
        val values = Regex("""-?\d+(?:\.\d+)?""").findAll(solution.input.normalized).map { it.value.toDouble() }.toList()
        val first = values.firstOrNull() ?: 1.0
        val difference = values.getOrNull(1) ?: 1.0
        val geometric = solution.input.normalized.contains("geometric", true)
        val terms = (1..8).map { n ->
            val value = if (geometric) first * Math.pow(difference, (n - 1).toDouble()) else first + (n - 1) * difference
            VisualPoint(n.toDouble(), value, "a$n")
        }
        var sum = 0.0
        val sums = terms.map { VisualPoint(it.x, (sum + it.y).also { next -> sum = next }, "S${it.x.toInt()}") }
        return VisualisationSpec(
            "sequence-pattern", VisualisationType.SequencePattern, "Terms and partial sums", linked,
            VisualisationData.SequencePattern(terms, sums, if (geometric) abs(difference) < 1 else null),
            InteractionMode.Scrubbable,
            "First eight terms and their running partial sums.",
            listOf("geometric-sum"),
        )
    }

    private fun matrix(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val matrixSource = Regex("""\[\[(.*?)]]""").find(solution.input.normalized)?.value ?: return null
        val rows = matrixSource.removePrefix("[[").removeSuffix("]]").split(Regex("""\]\s*,\s*\["""))
            .map { row -> row.split(',').map(String::trim) }
        return VisualisationSpec(
            "matrix-grid", VisualisationType.RowReductionGrid, "Matrix entries and active row", linked,
            VisualisationData.MatrixGrid(rows, highlightedRows = setOf(0)),
            InteractionMode.StepLinked,
            "Matrix grid with ${rows.size} rows and ${rows.firstOrNull()?.size ?: 0} columns.",
            listOf("matrix-product", "matrix-inverse"),
        )
    }

    private fun calculus(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val lower = solution.input.normalized.lowercase()
        val source = calculusExpression(solution.input.normalized)
        val curve = sample(source, -4.0, 4.0, 129) ?: return null
        if ("integr" in lower) {
            val bounds = Regex("""(?i)\bfrom\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)""").find(solution.input.normalized)
            val from = bounds?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val to = bounds?.groupValues?.get(2)?.toDoubleOrNull() ?: 2.0
            val rectangles = rectangles(source, from, to, 24)
            val area = rectangles.sumOf { (it.right - it.left) * it.height }
            return VisualisationSpec(
                "integral-area", VisualisationType.IntegralArea, "Accumulated signed area", linked,
                VisualisationData.IntegralArea(curve, from, to, rectangles, area),
                InteractionMode.Scrubbable,
                "Signed area from ${number(from)} to ${number(to)} using 24 midpoint rectangles.",
                listOf("integration-power-rule"),
                domainStatement = "The curve must remain finite throughout the displayed interval.",
            )
        }
        val x = 1.0
        val compiled = runCatching { expressions.compile(source) }.getOrNull() ?: return null
        val y = runCatching { compiled.eval(mapOf("x" to x)) }.getOrNull()?.takeIf(Double::isFinite) ?: return null
        val h = 1e-4
        val slope = (compiled.eval(mapOf("x" to x + h)) - compiled.eval(mapOf("x" to x - h))) / (2 * h)
        val tangent = VisualSeries("tangent", "tangent", listOf(-4.0, 4.0).map { px -> VisualPoint(px, y + slope * (px - x)) })
        val secants = listOf(1.5, .75, .25).map { delta ->
            val otherY = compiled.eval(mapOf("x" to x + delta))
            val secantSlope = (otherY - y) / delta
            VisualSeries("secant-$delta", "h=$delta", listOf(VisualPoint(x, y), VisualPoint(x + delta, otherY)), emptyList())
                .copy(points = listOf(-4.0, 4.0).map { px -> VisualPoint(px, y + secantSlope * (px - x)) })
        }
        return VisualisationSpec(
            "derivative-tangent", VisualisationType.DerivativeTangent, "Secants approach the tangent", linked,
            VisualisationData.DerivativeTangent(curve, VisualPoint(x, y, "x=1"), tangent, secants, slope),
            InteractionMode.Scrubbable,
            "Curve, three secant lines, and the limiting tangent at x equals 1 with slope ${number(slope)}.",
            listOf("derivative-power-rule", "product-rule", "chain-rule"),
        )
    }

    private fun complex(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        val candidates = Regex("""[+-]?(?:\d+(?:\.\d+)?)?(?:\s*[+-]\s*\d+(?:\.\d+)?)?i|[+-]?\d+(?:\.\d+)?""")
            .findAll(solution.finalAnswer.orEmpty()).mapNotNull { runCatching { CalculatorComplex.parse(it.value) }.getOrNull() }.take(24).toList()
        if (candidates.isEmpty()) return null
        return VisualisationSpec(
            "complex-plane", VisualisationType.ComplexPlane, "Complex values on the Argand plane", linked,
            VisualisationData.ComplexPlane(
                candidates.mapIndexed { index, value -> VisualPoint(value.real, value.imaginary, "z${index + 1}") },
                branchConvention = "Principal argument is in (-pi, pi]; roots use (theta + 2k*pi)/n.",
            ),
            InteractionMode.Inspectable,
            "Argand plane with real coordinates horizontally and imaginary coordinates vertically.",
            listOf("complex-polar-form"),
            domainStatement = "Principal argument in (-pi, pi].",
        )
    }

    private fun visualVerification(solution: SolverSolution, linked: List<String>): VisualisationSpec? {
        if (solution.verification.checks.isEmpty()) return null
        val data = solution.verification.checks.mapNotNull { check ->
            val expected = check.expected.toDoubleOrNull()
            val actual = check.actual.toDoubleOrNull()
            if (expected == null || actual == null) null else VisualVerificationDatum(check.label, expected, actual, 1e-7)
        }
        if (data.isEmpty()) return null
        return VisualisationSpec(
            "visual-verification", VisualisationType.VerificationComparison, "Visual verification", linked.takeLast(1),
            VisualisationData.VerificationComparison(solution.finalAnswer.orEmpty(), data),
            InteractionMode.Static,
            "Independent comparison of expected and actual verification values.",
            listOf("verification"),
        )
    }

    private fun sample(source: String, minimum: Double, maximum: Double, count: Int): VisualSeries? {
        val compiled = runCatching { expressions.compile(source) }.getOrNull() ?: return null
        val points = mutableListOf<VisualPoint>()
        val gaps = mutableListOf<Double>()
        repeat(count) { index ->
            val x = minimum + (maximum - minimum) * index / (count - 1)
            val y = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
            if (y.isFinite() && abs(y) < 1e6) points += VisualPoint(x, y) else gaps += x
        }
        return points.takeIf { it.size >= 2 }?.let { VisualSeries("source", source, it, gaps) }
    }

    private fun rectangles(source: String, from: Double, to: Double, count: Int): List<AreaRectangle> {
        val compiled = runCatching { expressions.compile(source) }.getOrNull() ?: return emptyList()
        val width = (to - from) / count
        return (0 until count).mapNotNull { index ->
            val left = from + index * width
            val right = left + width
            val height = runCatching { compiled.eval(mapOf("x" to (left + right) / 2)) }.getOrNull()
            height?.takeIf(Double::isFinite)?.let { AreaRectangle(left, right, it) }
        }
    }

    private fun residualExpression(source: String): String {
        val clean = source.trim().removePrefix("solve ").removePrefix("Solve ")
        return if ('=' in clean) clean.split('=', limit = 2).let { "(${it[0]})-(${it[1]})" } else clean
    }

    private fun calculusExpression(source: String): String = source
        .replace(Regex("""(?i)\b(?:differentiate|derivative of|integrate|integral of)\b"""), "")
        .replace(Regex("""(?i)\s+from\s+.+$"""), "")
        .replace(Regex("""\s+d[a-z]\s*$"""), "")
        .trim()

    private fun interpolate(points: List<VisualPoint>, x: Double): Double {
        val pair = points.zipWithNext().firstOrNull { (a, b) -> x in min(a.x, b.x)..max(a.x, b.x) } ?: return Double.NaN
        val (a, b) = pair
        return if (abs(b.x - a.x) < 1e-12) a.y else a.y + (b.y - a.y) * (x - a.x) / (b.x - a.x)
    }

    private fun number(value: Double): String =
        if (abs(value - value.toLong()) < 1e-10) value.toLong().toString()
        else String.format(Locale.US, "%.5f", value).trimEnd('0').trimEnd('.')
}

class SolverVisualisationCache(private val maximumEntries: Int = 48) {
    private val values = object : LinkedHashMap<String, List<VisualisationSpec>>(maximumEntries, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<VisualisationSpec>>?) = size > maximumEntries
    }

    @Synchronized
    operator fun get(key: String): List<VisualisationSpec>? = values[key]

    @Synchronized
    operator fun set(key: String, value: List<VisualisationSpec>) {
        values[key] = value
    }
}

object SolverVisualisationValidator {
    fun validate(specification: VisualisationSpec): List<String> = buildList {
        if (specification.linkedStepIds.isEmpty()) add("A visualisation must link to at least one solution step.")
        if (specification.accessibilityDescription.isBlank()) add("A text alternative is required.")
        when (val data = specification.mathematicalData) {
            is VisualisationData.NumberLine -> {
                if (!(data.minimum < data.maximum)) add("Number-line bounds are invalid.")
                if (data.start !in data.minimum..data.maximum || data.end !in data.minimum..data.maximum) add("Movement leaves the displayed interval.")
            }
            is VisualisationData.FractionArea -> if (data.denominator <= 0 || (data.comparisonDenominator ?: 1) <= 0) add("Fraction partitions require positive denominators.")
            is VisualisationData.PercentageBar -> if (!data.percentage.isFinite()) add("Percentage must be finite.")
            is VisualisationData.CoordinateGraph -> {
                if (data.series.any { it.points.any { point -> !point.x.isFinite() || !point.y.isFinite() } }) add("Graph contains a non-finite joined point.")
                if (data.series.any { it.points.size < 2 }) add("A graph series needs at least two points.")
            }
            is VisualisationData.UnitCircle -> if (abs(data.sine * data.sine + data.cosine * data.cosine - 1) > 1e-8) add("Unit-circle coordinates violate the Pythagorean identity.")
            is VisualisationData.Triangle -> if (data.vertices.size != 3) add("A triangle requires three vertices.")
            is VisualisationData.MatrixGrid -> if (data.values.isEmpty() || data.values.any { it.size != data.values.first().size }) add("Matrix rows must be rectangular.")
            is VisualisationData.SequencePattern -> if (data.terms.isEmpty()) add("Sequence visual requires at least one term.")
            is VisualisationData.BarChart -> if (
                data.values.isEmpty() ||
                data.labels.size != data.values.size ||
                data.values.any { !it.isFinite() || it < 0.0 }
            ) add("Bar-chart labels and finite non-negative values must align.")
            is VisualisationData.ComplexPlane -> if (data.points.any { !it.x.isFinite() || !it.y.isFinite() }) add("Complex-plane point is non-finite.")
            is VisualisationData.DerivativeTangent -> if (!data.slope.isFinite()) add("Tangent slope must be finite.")
            is VisualisationData.IntegralArea -> if (data.rectangles.any { !it.height.isFinite() }) add("Area rectangle is non-finite.")
            is VisualisationData.VerificationComparison -> if (data.checks.isEmpty()) add("Visual verification requires at least one check.")
            is VisualisationData.BalanceScale,
            is VisualisationData.RatioTable,
            is VisualisationData.AlgebraTiles,
            is VisualisationData.TransformationHighlight -> Unit
        }
    }
}
