package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

enum class GraphDiscontinuityKind { Hole, VerticalAsymptote, Jump, Undefined }
data class GraphInterval(val from: Double, val to: Double) {
    init { require(from <= to) }
}
data class GraphDiscontinuity(val x: Double, val kind: GraphDiscontinuityKind, val leftLimit: Double?, val rightLimit: Double?)
data class GraphAsymptote(val kind: String, val equation: String, val direction: String)
data class GraphLineAnalysis(val point: Vec2, val slope: Double?, val tangentEquation: String, val normalEquation: String)
data class AdvancedGraphFeatures(
    val domain: List<GraphInterval>,
    val discontinuities: List<GraphDiscontinuity>,
    val asymptotes: List<GraphAsymptote>,
    val increasing: List<GraphInterval>,
    val decreasing: List<GraphInterval>,
    val concaveUp: List<GraphInterval>,
    val concaveDown: List<GraphInterval>,
    val inflectionPoints: List<Vec2>,
)
data class GraphAreaResult(val from: Double, val to: Double, val signedArea: Double, val geometricArea: Double, val errorEstimate: Double)

/** Deterministic numerical feature analysis for explicit real-valued functions. */
class AdvancedGraphFeatureEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun analyze(source: String, minimum: Double = -10.0, maximum: Double = 10.0, samples: Int = 1600): AdvancedGraphFeatures {
        require(minimum < maximum && samples >= 200)
        val expression = expressions.compile(stripEquation(source))
        fun value(x: Double) = runCatching { expression.eval(mapOf("x" to x)) }.getOrNull()?.takeIf { it.isFinite() }
        val step = (maximum - minimum) / samples
        val xs = (0..samples).map { minimum + it * step }
        val values = xs.map(::value)
        val domain = intervals(xs, values.map { it != null })
        val discontinuities = discontinuities(xs, values, step, ::value)
        val excluded = discontinuities.map { it.x }
        fun safe(x: Double) = excluded.none { abs(it - x) < step * 2 }
        val firstSigns = xs.map { x -> derivative(::value, x, step / 5)?.takeIf { safe(x) }?.sign() }
        val secondSigns = xs.map { x -> secondDerivative(::value, x, step / 3)?.takeIf { safe(x) }?.sign() }
        val inflections = signChanges(xs, secondSigns).mapNotNull { x -> value(x)?.let { Vec2(x, it) } }
        return AdvancedGraphFeatures(
            domain = domain,
            discontinuities = discontinuities,
            asymptotes = asymptotes(::value, discontinuities),
            increasing = signIntervals(xs, firstSigns, 1),
            decreasing = signIntervals(xs, firstSigns, -1),
            concaveUp = signIntervals(xs, secondSigns, 1),
            concaveDown = signIntervals(xs, secondSigns, -1),
            inflectionPoints = inflections,
        )
    }

    fun tangentAndNormal(source: String, at: Double): GraphLineAnalysis {
        val expression = expressions.compile(stripEquation(source))
        fun value(x: Double) = runCatching { expression.eval(mapOf("x" to x)) }.getOrNull()?.takeIf(Double::isFinite)
        val y = value(at) ?: error("The function is undefined at x=${number(at)}")
        val h = max(1e-5, abs(at) * 1e-5)
        val slope = derivative(::value, at, h) ?: error("No finite tangent exists at x=${number(at)}")
        val tangent = lineEquation(slope, at, y)
        val normal = if (abs(slope) < 1e-12) "x = ${number(at)}" else lineEquation(-1.0 / slope, at, y)
        return GraphLineAnalysis(Vec2(at, y), slope, tangent, normal)
    }

    fun areaBetween(first: String, second: String, from: Double, to: Double, tolerance: Double = 1e-8): GraphAreaResult {
        require(from <= to && tolerance > 0)
        val a = expressions.compile(stripEquation(first)); val b = expressions.compile(stripEquation(second))
        fun difference(x: Double) = (a.eval(mapOf("x" to x)) - b.eval(mapOf("x" to x))).also { require(it.isFinite()) }
        val signed = adaptive(from, to, tolerance, ::difference)
        val geometric = adaptive(from, to, tolerance) { abs(difference(it)) }
        val coarse = adaptive(from, to, tolerance * 16, ::difference)
        return GraphAreaResult(from, to, signed, geometric, abs(signed - coarse))
    }

    private fun discontinuities(xs: List<Double>, values: List<Double?>, step: Double, value: (Double) -> Double?): List<GraphDiscontinuity> {
        val candidates = mutableListOf<Double>()
        values.indices.forEach { index ->
            if (values[index] == null) candidates += xs[index]
            if (index > 0 && values[index] != null && values[index - 1] != null) {
                val scale = max(1.0, max(abs(values[index]!!), abs(values[index - 1]!!)))
                if (abs(values[index]!! - values[index - 1]!!) > 25 * scale) candidates += (xs[index] + xs[index - 1]) / 2
            }
        }
        return candidates.sorted().fold(mutableListOf<Double>()) { result, x -> if (result.lastOrNull()?.let { abs(it - x) < step * 3 } != true) result += x; result }
            .map { approximate ->
                val x = refineUndefined(approximate, step, value)
                val h = max(1e-6, step / 40)
                val left = value(x - h); val right = value(x + h); val center = value(x)
                val growing = listOfNotNull(left, right).any { abs(it) > 1e3 } || listOfNotNull(value(x - h * 5), value(x + h * 5), left, right).zipWithNext().any { abs(it.second) > abs(it.first) * 3 }
                val kind = when {
                    growing -> GraphDiscontinuityKind.VerticalAsymptote
                    center == null && left != null && right != null && abs(left - right) < 1e-3 * max(1.0, abs(left)) -> GraphDiscontinuityKind.Hole
                    left != null && right != null && abs(left - right) > 1e-3 * max(1.0, max(abs(left), abs(right))) -> GraphDiscontinuityKind.Jump
                    else -> GraphDiscontinuityKind.Undefined
                }
                GraphDiscontinuity(x, kind, left, right)
            }
    }

    private fun refineUndefined(center: Double, radius: Double, value: (Double) -> Double?): Double {
        var best = center; var score = value(center)?.let { abs(it) } ?: Double.POSITIVE_INFINITY
        repeat(60) { index ->
            val x = center - radius + 2 * radius * index / 59.0
            val current = value(x)?.let { abs(it) } ?: return x
            if (current > score) { score = current; best = x }
        }
        return best
    }

    private fun asymptotes(value: (Double) -> Double?, discontinuities: List<GraphDiscontinuity>): List<GraphAsymptote> = buildList {
        discontinuities.filter { it.kind == GraphDiscontinuityKind.VerticalAsymptote }.forEach { add(GraphAsymptote("vertical", "x = ${number(it.x)}", "local")) }
        listOf(-1.0 to "x -> -infinity", 1.0 to "x -> +infinity").forEach { (direction, label) ->
            val x1 = direction * 1e3; val x2 = direction * 2e3; val x3 = direction * 4e3
            val y1 = value(x1); val y2 = value(x2); val y3 = value(x3)
            if (y1 != null && y2 != null && y3 != null) {
                val slope = (y2 - y1) / (x2 - x1); val intercept = y2 - slope * x2
                val firstLimit = 2 * y2 - y1
                val refinedLimit = 2 * y3 - y2
                val stableLimit = abs(refinedLimit - firstLimit) < 1e-3 * max(1.0, abs(refinedLimit))
                if (abs(slope) < 1e-6 && stableLimit) {
                    val limit = if (abs(refinedLimit) < 1e-6) 0.0 else refinedLimit
                    add(GraphAsymptote("horizontal", "y = ${number(limit)}", label))
                }
                else if (abs(slope) > 1e-8) {
                    val predicted = slope * x3 + intercept
                    if (abs(y3 - predicted) < 1e-3 * max(1.0, abs(y3))) add(GraphAsymptote("oblique", "y = ${number(slope)}*x + ${number(intercept)}", label))
                }
            }
        }
    }.distinctBy { it.kind to it.equation }

    private fun intervals(xs: List<Double>, active: List<Boolean>): List<GraphInterval> {
        val result = mutableListOf<GraphInterval>(); var start: Double? = null
        active.indices.forEach { i ->
            if (active[i] && start == null) start = xs[i]
            val intervalStart = start
            if ((!active[i] || i == active.lastIndex) && intervalStart != null) {
                val end = if (active[i]) xs[i] else xs[(i - 1).coerceAtLeast(0)]
                if (end >= intervalStart) result += GraphInterval(intervalStart, end)
                start = null
            }
        }
        return result
    }

    private fun signIntervals(xs: List<Double>, signs: List<Int?>, target: Int) = intervals(xs, signs.map { it == target })
    private fun signChanges(xs: List<Double>, signs: List<Int?>): List<Double> {
        val changes = mutableListOf<Double>(); var previousSign: Int? = null; var previousX = xs.first()
        signs.indices.forEach { index ->
            val sign = signs[index]
            if (sign != null && sign != 0) {
                if (previousSign != null && sign != previousSign) changes += (previousX + xs[index]) / 2
                previousSign = sign; previousX = xs[index]
            }
        }
        return changes
    }
    private fun derivative(value: (Double) -> Double?, x: Double, h: Double): Double? { val left = value(x - h) ?: return null; val right = value(x + h) ?: return null; return (right - left) / (2 * h) }
    private fun secondDerivative(value: (Double) -> Double?, x: Double, h: Double): Double? { val left = value(x - h) ?: return null; val center = value(x) ?: return null; val right = value(x + h) ?: return null; return (right - 2 * center + left) / (h * h) }
    private fun Double.sign() = when { this > 1e-7 -> 1; this < -1e-7 -> -1; else -> 0 }
    private fun lineEquation(slope: Double, x: Double, y: Double) = "y - ${number(y)} = ${number(slope)}*(x - ${number(x)})"

    private fun adaptive(from: Double, to: Double, tolerance: Double, f: (Double) -> Double): Double {
        if (from == to) return 0.0
        fun simpson(a: Double, b: Double, fa: Double, fm: Double, fb: Double) = (b - a) * (fa + 4 * fm + fb) / 6
        fun recurse(a: Double, b: Double, fa: Double, fm: Double, fb: Double, whole: Double, tol: Double, depth: Int): Double {
            val middle = (a + b) / 2; val lm = (a + middle) / 2; val rm = (middle + b) / 2; val fl = f(lm); val fr = f(rm)
            val left = simpson(a, middle, fa, fl, fm); val right = simpson(middle, b, fm, fr, fb); val delta = left + right - whole
            return if (depth == 0 || abs(delta) <= 15 * tol) left + right + delta / 15 else recurse(a, middle, fa, fl, fm, left, tol / 2, depth - 1) + recurse(middle, b, fm, fr, fb, right, tol / 2, depth - 1)
        }
        val middle = (from + to) / 2; val fa = f(from); val fm = f(middle); val fb = f(to)
        return recurse(from, to, fa, fm, fb, simpson(from, to, fa, fm, fb), tolerance, 18)
    }

    private fun number(value: Double): String { val clean = if (abs(value) < 1e-9) 0.0 else value; val whole = round(clean); return if (abs(clean - whole) < 1e-7) whole.toLong().toString() else String.format(java.util.Locale.US, "%.7f", clean).trimEnd('0').trimEnd('.') }
}
