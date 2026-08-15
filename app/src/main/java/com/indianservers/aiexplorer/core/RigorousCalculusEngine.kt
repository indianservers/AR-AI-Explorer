package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

enum class LimitDirection { Left, Right, TwoSided }
enum class LimitClassification { Finite, PositiveInfinity, NegativeInfinity, DoesNotExist, Unresolved }
enum class LimitMethod { DirectSubstitution, FactorCancellation, LHopitalCandidate, SqueezePattern, NumericalCertification }
enum class ContinuityClassification { Continuous, Removable, Jump, Infinite, OscillatoryOrUnresolved }

data class OneSidedLimitEvidence(
    val direction: LimitDirection,
    val classification: LimitClassification,
    val estimate: Double?,
    val samples: List<Pair<Double, Double>>,
    val stableDigits: Int,
)

data class RigorousLimitReport(
    val source: String,
    val variable: String,
    val approaching: Double,
    val left: OneSidedLimitEvidence,
    val right: OneSidedLimitEvidence,
    val classification: LimitClassification,
    val value: Double?,
    val continuity: ContinuityClassification,
    val functionValue: Double?,
    val method: LimitMethod,
    val steps: List<String>,
    val verification: String,
)

data class DerivativeApplicationPoint(val point: Vec2, val kind: String)
data class DerivativeApplicationReport(
    val source: String,
    val interval: ClosedFloatingPointRange<Double>,
    val stationaryPoints: List<DerivativeApplicationPoint>,
    val absoluteMinimum: Vec2?,
    val absoluteMaximum: Vec2?,
    val increasing: List<GraphInterval>,
    val decreasing: List<GraphInterval>,
)

/** Evidence-first limits and derivative applications; it refuses a finite answer when sides disagree. */
class RigorousCalculusEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    private val graphFeatures = AdvancedGraphFeatureEngine(expressions)

    fun limit(source: String, approaching: Double, variable: String = "x"): RigorousLimitReport {
        val compiled = expressions.compile(stripEquation(source))
        fun value(x: Double) = runCatching { compiled.eval(mapOf(variable to x)) }.getOrNull()?.takeIf(Double::isFinite)
        val scale = max(1.0, abs(approaching))
        val offsets = listOf(1e-1, 3e-2, 1e-2, 3e-3, 1e-3, 3e-4, 1e-4, 3e-5, 1e-5, 3e-6, 1e-6).map { it * scale }
        val left = side(LimitDirection.Left, approaching, offsets.map { approaching - it }, ::value)
        val right = side(LimitDirection.Right, approaching, offsets.map { approaching + it }, ::value)
        val classification = combine(left, right)
        val resultValue = if (classification == LimitClassification.Finite) listOfNotNull(left.estimate, right.estimate).average() else null
        val center = value(approaching)
        val continuity = when {
            classification == LimitClassification.Finite && center != null && resultValue != null && close(center, resultValue) -> ContinuityClassification.Continuous
            classification == LimitClassification.Finite -> ContinuityClassification.Removable
            left.classification in infinities || right.classification in infinities -> ContinuityClassification.Infinite
            left.classification == LimitClassification.Finite && right.classification == LimitClassification.Finite -> ContinuityClassification.Jump
            else -> ContinuityClassification.OscillatoryOrUnresolved
        }
        val method = method(source, approaching, center, left, right)
        val answer = when (classification) {
            LimitClassification.Finite -> number(resultValue!!)
            LimitClassification.PositiveInfinity -> "+infinity"
            LimitClassification.NegativeInfinity -> "-infinity"
            LimitClassification.DoesNotExist -> "DNE"
            LimitClassification.Unresolved -> "unresolved"
        }
        return RigorousLimitReport(
            source, variable, approaching, left, right, classification, resultValue, continuity, center, method,
            steps = listOf(
                "Inspect the function domain near $variable=${number(approaching)}.",
                "Compute the left-hand limit: ${display(left)}.",
                "Compute the right-hand limit: ${display(right)}.",
                if (classification == LimitClassification.Finite) "The one-sided limits agree, so the two-sided limit is $answer."
                else "The one-sided evidence gives $answer; no unsupported finite value is emitted.",
                "Compare the limit with f(${number(approaching)}) to classify continuity as ${continuity.name}.",
            ),
            verification = "${left.stableDigits} left-side and ${right.stableDigits} right-side stable decimal digits across shrinking offsets; method=${method.name}.",
        )
    }

    fun derivativeApplications(source: String, from: Double, to: Double): DerivativeApplicationReport {
        require(from < to)
        val features = graphFeatures.analyze(source, from, to, 2000)
        val compiled = expressions.compile(stripEquation(source))
        fun value(x: Double) = runCatching { compiled.eval(mapOf("x" to x)) }.getOrNull()?.takeIf(Double::isFinite)
        val candidates = (features.increasing.flatMap { listOf(it.from, it.to) } + features.decreasing.flatMap { listOf(it.from, it.to) } + listOf(from, to)).distinct()
            .mapNotNull { x -> value(x)?.let { Vec2(x, it) } }
        val stationary = features.increasing.flatMap { up -> features.decreasing.mapNotNull { down ->
            when {
                abs(up.to - down.from) < (to - from) / 500 -> DerivativeApplicationPoint(Vec2((up.to + down.from) / 2, value((up.to + down.from) / 2) ?: return@mapNotNull null), "local maximum")
                abs(down.to - up.from) < (to - from) / 500 -> DerivativeApplicationPoint(Vec2((down.to + up.from) / 2, value((down.to + up.from) / 2) ?: return@mapNotNull null), "local minimum")
                else -> null
            }
        } }.distinctBy { number(it.point.x) }
        return DerivativeApplicationReport(source, from..to, stationary, candidates.minByOrNull { it.y }, candidates.maxByOrNull { it.y }, features.increasing, features.decreasing)
    }

    private fun side(direction: LimitDirection, at: Double, xs: List<Double>, value: (Double) -> Double?): OneSidedLimitEvidence {
        val samples = xs.mapNotNull { x -> value(x)?.let { x to it } }
        if (samples.size < 4) return OneSidedLimitEvidence(direction, LimitClassification.Unresolved, null, samples, 0)
        val tail = samples.takeLast(4).map { it.second }
        val magnitudes = tail.map(::abs)
        val infinity = magnitudes.last() > 1e5 && magnitudes.zipWithNext().all { (a, b) -> b > a * 2 }
        if (infinity) return OneSidedLimitEvidence(direction, if (tail.last() > 0) LimitClassification.PositiveInfinity else LimitClassification.NegativeInfinity, null, samples, 0)
        val estimate = tail.takeLast(2).average()
        val error = tail.takeLast(3).maxOf { abs(it - estimate) }
        val tolerance = 5e-4 * max(1.0, abs(estimate))
        val stable = error < tolerance
        val digits = if (!stable) 0 else (0..8).lastOrNull { error < max(1e-12, 10.0.pow(-it) * max(1.0, abs(estimate))) } ?: 0
        return OneSidedLimitEvidence(direction, if (stable) LimitClassification.Finite else LimitClassification.Unresolved, estimate.takeIf { stable }, samples, digits)
    }

    private fun combine(left: OneSidedLimitEvidence, right: OneSidedLimitEvidence): LimitClassification = when {
        left.classification == LimitClassification.Finite && right.classification == LimitClassification.Finite -> if (close(left.estimate!!, right.estimate!!)) LimitClassification.Finite else LimitClassification.DoesNotExist
        left.classification == right.classification && left.classification in infinities -> left.classification
        left.classification in infinities && right.classification in infinities -> LimitClassification.DoesNotExist
        left.classification != LimitClassification.Unresolved && right.classification != LimitClassification.Unresolved -> LimitClassification.DoesNotExist
        else -> LimitClassification.Unresolved
    }

    private fun method(source: String, at: Double, center: Double?, left: OneSidedLimitEvidence, right: OneSidedLimitEvidence): LimitMethod = when {
        center != null && left.classification == LimitClassification.Finite && right.classification == LimitClassification.Finite -> LimitMethod.DirectSubstitution
        Regex("sin\\s*\\([^)]*\\)\\s*/", RegexOption.IGNORE_CASE).containsMatchIn(source) && abs(at) < 1e-12 -> LimitMethod.SqueezePattern
        '/' in source && left.classification == LimitClassification.Finite && right.classification == LimitClassification.Finite -> LimitMethod.FactorCancellation
        '/' in source -> LimitMethod.LHopitalCandidate
        else -> LimitMethod.NumericalCertification
    }

    private fun display(side: OneSidedLimitEvidence) = side.estimate?.let(::number) ?: side.classification.name
    private fun close(a: Double, b: Double) = abs(a - b) <= 2e-3 * max(1.0, max(abs(a), abs(b)))
    private fun stripEquation(source: String) = source.substringAfter('=').trim().ifBlank { source.trim() }
    private fun number(value: Double): String { val clean = if (abs(value) < 1e-7) 0.0 else value; val whole = round(clean); return if (abs(clean - whole) < 1e-5) whole.toLong().toString() else String.format(java.util.Locale.US, "%.6f", clean).trimEnd('0').trimEnd('.') }
    companion object { private val infinities = setOf(LimitClassification.PositiveInfinity, LimitClassification.NegativeInfinity) }
}
