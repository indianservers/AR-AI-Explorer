package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

enum class GraphNumericConfidence(val label: String) {
    Exact("exact"), High("high"), Medium("medium"), Low("low")
}

data class GraphNumericEvidence(
    val value: Double,
    val exact: String? = null,
    val errorEstimate: Double,
    val residual: Double,
    val confidence: GraphNumericConfidence,
    val method: String,
) {
    val display: String get() = exact ?: formatGraphNumber(value)
}

data class RobustGraphPoint(
    val kind: GraphPointKind,
    val point: Vec2,
    val x: GraphNumericEvidence,
    val y: GraphNumericEvidence,
    val classification: String,
)

data class RobustGraphTangent(
    val point: Vec2,
    val slope: GraphNumericEvidence,
    val tangentEquation: String,
    val normalEquation: String,
)

data class RobustGraphIntegral(
    val from: Double,
    val to: Double,
    val signed: GraphNumericEvidence,
    val geometric: GraphNumericEvidence,
    val evaluations: Int,
)

data class RobustGraphAnalysisReport(
    val roots: List<RobustGraphPoint>,
    val extrema: List<RobustGraphPoint>,
    val intersections: List<RobustGraphPoint>,
    val tangent: RobustGraphTangent?,
    val integral: RobustGraphIntegral?,
    val areaBetween: RobustGraphIntegral?,
    val warnings: List<String>,
)

/** Confidence-aware numerical analysis used by graph tools and accessibility summaries. */
class RobustGraphAnalysisEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    private val calculus = CompetitiveGraphCalculus(expressions)
    private data class FeatureKey(val source: String, val second: String?, val minimum: Double, val maximum: Double, val parameters: List<Pair<String, Double>>)
    private data class CachedFeatures(val roots: List<RobustGraphPoint>, val extrema: List<RobustGraphPoint>, val intersections: List<RobustGraphPoint>)
    private val featureCache = object : LinkedHashMap<FeatureKey, CachedFeatures>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<FeatureKey, CachedFeatures>?) = size > 8
    }

    fun analyze(
        source: String,
        minimum: Double,
        maximum: Double,
        at: Double,
        secondSource: String? = null,
        integralFrom: Double = 0.0,
        integralTo: Double = at,
        parameters: Map<String, Double> = emptyMap(),
    ): RobustGraphAnalysisReport {
        require(minimum.isFinite() && maximum.isFinite() && minimum < maximum)
        val clean = stripEquation(source)
        val cleanSecond = secondSource?.let(::stripEquation)
        val key = FeatureKey(clean, cleanSecond, minimum, maximum, parameters.toList().sortedBy { it.first })
        val features = synchronized(featureCache) { featureCache[key] } ?: CachedFeatures(
            roots(clean, minimum, maximum, parameters),
            extrema(clean, minimum, maximum, parameters),
            cleanSecond?.let { intersections(clean, it, minimum, maximum, parameters) }.orEmpty(),
        ).also { synchronized(featureCache) { featureCache[key] = it } }
        val tangent = runCatching { tangent(clean, at, parameters) }.getOrNull()
        val integral = runCatching { integral(clean, integralFrom, integralTo, parameters) }.getOrNull()
        val area = cleanSecond?.let { second -> runCatching { areaBetween(clean, second, integralFrom, integralTo, parameters) }.getOrNull() }
        return RobustGraphAnalysisReport(
            roots = features.roots,
            extrema = features.extrema,
            intersections = features.intersections,
            tangent = tangent,
            integral = integral,
            areaBetween = area,
            warnings = buildList {
                if (features.roots.any { it.x.confidence == GraphNumericConfidence.Low }) add("One or more roots are weak candidates; zoom or narrow the interval.")
                if (features.extrema.any { it.x.confidence == GraphNumericConfidence.Low }) add("A flat or non-smooth point limited extremum confidence.")
                if (integral == null) add("The selected integral crosses an undefined or non-finite value.")
            },
        )
    }

    fun roots(source: String, minimum: Double, maximum: Double, parameters: Map<String, Double> = emptyMap()): List<RobustGraphPoint> {
        val expression = expressions.compile(stripEquation(source))
        val candidates = rootCandidates(minimum, maximum) { x -> evaluate(expression, x, parameters) }
        return candidates.mapNotNull { candidate ->
            val y = evaluate(expression, candidate.value, parameters) ?: return@mapNotNull null
            val exact = exactValue(candidate.value) { x -> evaluate(expression, x, parameters) }
            RobustGraphPoint(
                GraphPointKind.Root,
                Vec2(candidate.value, y),
                candidate.copy(exact = exact, confidence = if (exact != null) GraphNumericConfidence.Exact else candidate.confidence),
                evidence(y, abs(y), candidate.errorEstimate, null, "function residual"),
                "x-axis crossing or touch",
            )
        }
    }

    fun intersections(first: String, second: String, minimum: Double, maximum: Double, parameters: Map<String, Double> = emptyMap()): List<RobustGraphPoint> {
        val a = expressions.compile(stripEquation(first)); val b = expressions.compile(stripEquation(second))
        return rootCandidates(minimum, maximum) { x ->
            val firstValue = evaluate(a, x, parameters); val secondValue = evaluate(b, x, parameters)
            if (firstValue == null || secondValue == null) null else firstValue - secondValue
        }.mapNotNull { xEvidence ->
            val y = evaluate(a, xEvidence.value, parameters) ?: return@mapNotNull null
            val residual = abs((evaluate(b, xEvidence.value, parameters) ?: return@mapNotNull null) - y)
            val exact = exactValue(xEvidence.value) { x ->
                val av = evaluate(a, x, parameters); val bv = evaluate(b, x, parameters)
                if (av == null || bv == null) null else av - bv
            }
            RobustGraphPoint(
                GraphPointKind.Intersection,
                Vec2(xEvidence.value, y),
                xEvidence.copy(exact = exact, confidence = if (exact != null) GraphNumericConfidence.Exact else xEvidence.confidence),
                evidence(y, residual, xEvidence.errorEstimate, exactValue(y) { null }, "cross-curve agreement"),
                "curve intersection",
            )
        }
    }

    fun extrema(source: String, minimum: Double, maximum: Double, parameters: Map<String, Double> = emptyMap()): List<RobustGraphPoint> {
        val expression = expressions.compile(stripEquation(source))
        val width = maximum - minimum
        val derivativeRoots = rootCandidates(minimum, maximum, samples = 3072) { x -> firstDerivative(expression, x, parameters).first }
        return derivativeRoots.mapNotNull { candidate ->
            val y = evaluate(expression, candidate.value, parameters) ?: return@mapNotNull null
            val second = secondDerivative(expression, candidate.value, parameters)
            val classification = when {
                second == null -> "stationary candidate"
                second > 1e-6 -> "local minimum"
                second < -1e-6 -> "local maximum"
                else -> "flat stationary point"
            }
            val h = max(width / 200_000.0, 1e-6 * max(1.0, abs(candidate.value)))
            val left = evaluate(expression, candidate.value - h, parameters)
            val right = evaluate(expression, candidate.value + h, parameters)
            val isExtremum = left != null && right != null && (y >= left && y >= right || y <= left && y <= right)
            if (!isExtremum && abs(second ?: 0.0) < 1e-7) return@mapNotNull null
            val exact = exactValue(candidate.value) { x -> firstDerivative(expression, x, parameters).first }
            RobustGraphPoint(
                GraphPointKind.Extremum,
                Vec2(candidate.value, y),
                candidate.copy(exact = exact, confidence = if (exact != null) GraphNumericConfidence.Exact else candidate.confidence),
                evidence(y, abs(firstDerivative(expression, candidate.value, parameters).first ?: Double.POSITIVE_INFINITY), candidate.errorEstimate, exactValue(y) { null }, "stationary-point evaluation"),
                classification,
            )
        }.deduplicatePoints()
    }

    fun tangent(source: String, at: Double, parameters: Map<String, Double> = emptyMap()): RobustGraphTangent {
        val expression = expressions.compile(stripEquation(source))
        val y = evaluate(expression, at, parameters) ?: error("Function is undefined at x=${formatGraphNumber(at)}")
        val (slope, error) = firstDerivative(expression, at, parameters)
        require(slope?.isFinite() == true) { "No finite tangent exists at x=${formatGraphNumber(at)}" }
        val evidence = evidence(slope, error, error, exactValue(slope) { null }, "five-point derivative with step comparison")
        val tangent = "y - ${formatGraphNumber(y)} = ${evidence.display}*(x - ${formatGraphNumber(at)})"
        val normal = if (abs(slope) < 1e-12) "x = ${formatGraphNumber(at)}" else "y - ${formatGraphNumber(y)} = ${formatGraphNumber(-1.0 / slope)}*(x - ${formatGraphNumber(at)})"
        return RobustGraphTangent(Vec2(at, y), evidence, tangent, normal)
    }

    fun integral(source: String, from: Double, to: Double, parameters: Map<String, Double> = emptyMap()): RobustGraphIntegral {
        if (from == to) {
            val zero = GraphNumericEvidence(0.0, "0", 0.0, 0.0, GraphNumericConfidence.Exact, "zero-width interval")
            return RobustGraphIntegral(from, to, zero, zero, 0)
        }
        val result = calculus.integral(bindParameters(source, parameters), from, to)
        val signed = evidence(result.signedArea, result.errorEstimate, result.errorEstimate, exactValue(result.signedArea) { null }, "adaptive Simpson integration")
        val geometric = evidence(result.geometricArea, result.errorEstimate, result.errorEstimate, exactValue(result.geometricArea) { null }, "adaptive absolute-area integration")
        return RobustGraphIntegral(from, to, signed, geometric, result.intervals)
    }

    fun areaBetween(first: String, second: String, from: Double, to: Double, parameters: Map<String, Double> = emptyMap()): RobustGraphIntegral {
        if (from == to) return integral("0", from, to, parameters)
        val result = calculus.areaBetween(bindParameters(first, parameters), bindParameters(second, parameters), from, to)
        return RobustGraphIntegral(
            from, to,
            evidence(result.signedArea, result.errorEstimate, result.errorEstimate, exactValue(result.signedArea) { null }, "adaptive difference integration"),
            evidence(result.geometricArea, result.errorEstimate, result.errorEstimate, exactValue(result.geometricArea) { null }, "adaptive between-curves area"),
            result.intervals,
        )
    }

    private fun rootCandidates(
        minimum: Double,
        maximum: Double,
        samples: Int = 4096,
        evaluate: (Double) -> Double?,
    ): List<GraphNumericEvidence> {
        val step = (maximum - minimum) / samples
        val values = (0..samples).map { index -> minimum + index * step }.map { it to evaluate(it) }
        val candidates = mutableListOf<Pair<Double, String>>()
        values.zipWithNext().forEach { (left, right) ->
            val a = left.second; val b = right.second
            if (a != null && abs(a) < 1e-10) candidates += left.first to "sampled zero"
            if (a != null && b != null && a * b < 0.0) candidates += bisect(left.first, right.first, evaluate) to "bracketed bisection"
        }
        for (index in 1 until values.lastIndex) {
            val left = values[index - 1].second; val center = values[index].second; val right = values[index + 1].second
            if (left != null && center != null && right != null && abs(center) <= abs(left) && abs(center) <= abs(right)) {
                val refined = newton(values[index].first, step, minimum, maximum, evaluate)
                val residual = evaluate(refined)?.let(::abs) ?: continue
                val localScale = max(1.0, max(abs(left), abs(right)))
                if (residual < 1e-8 * localScale || abs(center) < step * step * localScale) candidates += refined to "stationary-touch refinement"
            }
        }
        return candidates.sortedBy { it.first }.fold(mutableListOf<Pair<Double, String>>()) { result, item ->
            if (result.lastOrNull()?.let { abs(it.first - item.first) < max(1e-8, step / 20) } != true) result += item
            result
        }.map { (root, method) ->
            val refined = newton(root, step / 4, minimum, maximum, evaluate)
            val residual = evaluate(refined)?.let(::abs) ?: Double.POSITIVE_INFINITY
            val error = abs(refined - root) + step / 2.0.powSafe(24)
            GraphNumericEvidence(refined, null, error, residual, confidence(residual, error, refined), method)
        }
    }

    private fun firstDerivative(expression: Expression, x: Double, parameters: Map<String, Double>): Pair<Double?, Double> {
        val h = max(2e-5, abs(x) * 2e-5)
        fun derivative(step: Double): Double? {
            val m2 = evaluate(expression, x - 2 * step, parameters) ?: return null
            val m1 = evaluate(expression, x - step, parameters) ?: return null
            val p1 = evaluate(expression, x + step, parameters) ?: return null
            val p2 = evaluate(expression, x + 2 * step, parameters) ?: return null
            return (m2 - 8 * m1 + 8 * p1 - p2) / (12 * step)
        }
        val coarse = derivative(h); val fine = derivative(h / 2)
        return fine to if (coarse == null || fine == null) Double.POSITIVE_INFINITY else abs(fine - coarse)
    }

    private fun secondDerivative(expression: Expression, x: Double, parameters: Map<String, Double>): Double? {
        val h = max(1e-4, abs(x) * 1e-4)
        val left = evaluate(expression, x - h, parameters) ?: return null
        val center = evaluate(expression, x, parameters) ?: return null
        val right = evaluate(expression, x + h, parameters) ?: return null
        return (left - 2 * center + right) / (h * h)
    }

    private fun bisect(from: Double, to: Double, evaluate: (Double) -> Double?): Double {
        var left = from; var right = to; var leftValue = evaluate(left) ?: return (from + to) / 2
        repeat(80) {
            val middle = (left + right) / 2; val middleValue = evaluate(middle) ?: return@repeat
            if (leftValue * middleValue <= 0) right = middle else { left = middle; leftValue = middleValue }
        }
        return (left + right) / 2
    }

    private fun newton(seed: Double, initialStep: Double, minimum: Double, maximum: Double, evaluate: (Double) -> Double?): Double {
        var x = seed
        repeat(18) {
            val value = evaluate(x) ?: return x
            val h = max(1e-7, initialStep / (it + 1))
            val left = evaluate(x - h) ?: return x; val right = evaluate(x + h) ?: return x
            val derivative = (right - left) / (2 * h)
            if (!derivative.isFinite() || abs(derivative) < 1e-14) return x
            val next = (x - value / derivative).coerceIn(minimum, maximum)
            if (abs(next - x) < 1e-13 * max(1.0, abs(x))) return next
            x = next
        }
        return x
    }

    private fun evaluate(expression: Expression, x: Double, parameters: Map<String, Double>): Double? =
        runCatching { expression.eval(parameters + ("x" to x)) }.getOrNull()?.takeIf(Double::isFinite)

    private fun bindParameters(source: String, parameters: Map<String, Double>): String = parameters.entries
        .sortedByDescending { it.key.length }
        .fold(source) { result, (name, value) ->
            result.replace(Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])", RegexOption.IGNORE_CASE), "(${String.format(java.util.Locale.US, "%.17g", value)})")
        }

    private fun evidence(value: Double, residual: Double, error: Double, exact: String?, method: String) = GraphNumericEvidence(
        value, exact, error, residual,
        if (exact != null) GraphNumericConfidence.Exact else confidence(residual, error, value), method,
    )

    private fun confidence(residual: Double, error: Double, scale: Double): GraphNumericConfidence {
        val relative = max(residual, error) / max(1.0, abs(scale))
        return when {
            relative <= 1e-10 -> GraphNumericConfidence.High
            relative <= 1e-6 -> GraphNumericConfidence.Medium
            else -> GraphNumericConfidence.Low
        }
    }

    private fun exactValue(value: Double, verifier: (Double) -> Double?): String? {
        if (!value.isFinite()) return null
        for (denominator in 1..64) {
            val numerator = round(value * denominator).toLong()
            val candidate = numerator.toDouble() / denominator
            if (abs(candidate - value) <= 1e-9 * max(1.0, abs(value))) {
                val residual = verifier(candidate)?.let(::abs)
                if (residual == null || residual <= 1e-9) {
                    val divisor = gcd(abs(numerator), denominator.toLong())
                    val reducedNumerator = numerator / divisor; val reducedDenominator = denominator / divisor
                    return if (reducedDenominator == 1L) reducedNumerator.toString() else "$reducedNumerator/$reducedDenominator"
                }
            }
        }
        val square = round(value * value).toInt()
        if (square in 2..10_000 && abs(abs(value) - sqrt(square.toDouble())) < 1e-9) return if (value < 0) "-sqrt($square)" else "sqrt($square)"
        return null
    }

    private fun List<RobustGraphPoint>.deduplicatePoints(): List<RobustGraphPoint> = sortedBy { it.point.x }.fold(mutableListOf()) { result, point ->
        if (result.lastOrNull()?.point?.distanceTo(point.point)?.let { it < 1e-6 } != true) result += point
        result
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a.coerceAtLeast(1) else gcd(b, a % b)
}

private fun Double.powSafe(power: Int): Double {
    var result = 1.0
    repeat(power) { result *= this }
    return result
}

internal fun formatGraphNumber(value: Double): String {
    val clean = if (abs(value) < 1e-12) 0.0 else value
    val whole = round(clean)
    return if (abs(clean - whole) < 1e-10) whole.toLong().toString()
    else String.format(java.util.Locale.US, "%.9g", clean)
}
