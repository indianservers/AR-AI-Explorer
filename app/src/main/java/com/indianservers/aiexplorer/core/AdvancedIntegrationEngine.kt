package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.tan

enum class IntegralConvergence { Convergent, Divergent, Unresolved }
data class CertifiedIntegral(
    val value: Double?,
    val errorEstimate: Double,
    val evaluations: Int,
    val convergence: IntegralConvergence,
    val method: String,
    val steps: List<String>,
    val verification: String,
)
data class IntegrationBound(val variable: String, val from: Double, val to: Double) { init { require(variable.isNotBlank() && from < to) } }
data class ParameterizedIntegralResult(val parameter: String, val samples: Map<Double, CertifiedIntegral>, val continuousOnSamples: Boolean)

/** Certified numerical integration for improper, parameterized and rectangular multiple integrals. */
class AdvancedIntegrationEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun definite(source: String, bound: IntegrationBound, parameters: Map<String, Double> = emptyMap(), tolerance: Double = 1e-8): CertifiedIntegral {
        val compiled = expressions.compile(stripEquation(source)); var evaluations = 0
        fun f(x: Double): Double { evaluations++; return compiled.eval(parameters + (bound.variable to x)).also { require(it.isFinite()) } }
        val fine = adaptive(bound.from, bound.to, tolerance, ::f)
        val coarse = adaptive(bound.from, bound.to, tolerance * 32, ::f)
        val error = abs(fine - coarse)
        return CertifiedIntegral(fine, error, evaluations, IntegralConvergence.Convergent, "adaptive Simpson",
            listOf("Validate finite bounds and integrand values.", "Adaptively subdivide ${bound.variable} on [${number(bound.from)}, ${number(bound.to)}].", "Compare fine and coarse tolerance passes."),
            "Independent tolerance passes differ by ${scientific(error)}.")
    }

    fun improperToPositiveInfinity(source: String, variable: String, from: Double, tolerance: Double = 1e-7): CertifiedIntegral =
        improper(source, variable, from, positive = true, tolerance = tolerance)

    fun improperFromNegativeInfinity(source: String, variable: String, to: Double, tolerance: Double = 1e-7): CertifiedIntegral =
        improper(source, variable, to, positive = false, tolerance = tolerance)

    fun improperBothInfinite(source: String, variable: String = "x", tolerance: Double = 1e-7): CertifiedIntegral {
        val left = improperFromNegativeInfinity(source, variable, 0.0, tolerance)
        val right = improperToPositiveInfinity(source, variable, 0.0, tolerance)
        if (left.convergence != IntegralConvergence.Convergent || right.convergence != IntegralConvergence.Convergent) {
            return CertifiedIntegral(null, Double.POSITIVE_INFINITY, left.evaluations + right.evaluations, IntegralConvergence.Divergent, "split improper integral", listOf("Split at 0.", "Require both one-sided improper integrals to converge."), "At least one tail failed convergence certification.")
        }
        return CertifiedIntegral(left.value!! + right.value!!, left.errorEstimate + right.errorEstimate, left.evaluations + right.evaluations, IntegralConvergence.Convergent, "split improper integral", listOf("Split (-infinity,infinity) at 0.", "Certify each tail independently.", "Add the two convergent values."), "Both tails independently converged.")
    }

    fun doubleIntegral(source: String, outer: IntegrationBound, inner: IntegrationBound, tolerance: Double = 1e-7): CertifiedIntegral {
        require(outer.variable != inner.variable)
        val compiled = expressions.compile(stripEquation(source)); var evaluations = 0
        fun outerValue(a: Double): Double = adaptive(inner.from, inner.to, tolerance / 8, { b ->
            evaluations++; compiled.eval(mapOf(outer.variable to a, inner.variable to b)).also { require(it.isFinite()) }
        })
        val fine = adaptive(outer.from, outer.to, tolerance, ::outerValue)
        val coarse = adaptive(outer.from, outer.to, tolerance * 24, ::outerValue)
        return CertifiedIntegral(fine, abs(fine - coarse), evaluations, IntegralConvergence.Convergent, "iterated adaptive Simpson",
            listOf("Use order d${inner.variable} d${outer.variable}.", "Integrate the inner bound for each outer sample.", "Adaptively integrate the resulting outer function."),
            "Repeated integration with relaxed tolerance differs by ${scientific(abs(fine - coarse))}.")
    }

    fun tripleIntegral(source: String, first: IntegrationBound, second: IntegrationBound, third: IntegrationBound, tolerance: Double = 1e-6): CertifiedIntegral {
        require(setOf(first.variable, second.variable, third.variable).size == 3)
        val compiled = expressions.compile(stripEquation(source)); var evaluations = 0
        fun firstValue(a: Double) = adaptive(second.from, second.to, tolerance / 10, { b ->
            adaptive(third.from, third.to, tolerance / 20, { c -> evaluations++; compiled.eval(mapOf(first.variable to a, second.variable to b, third.variable to c)).also { require(it.isFinite()) } })
        })
        val fine = adaptive(first.from, first.to, tolerance, ::firstValue)
        return CertifiedIntegral(fine, tolerance, evaluations, IntegralConvergence.Convergent, "nested adaptive Simpson",
            listOf("Respect order d${third.variable} d${second.variable} d${first.variable}.", "Evaluate nested one-dimensional certified integrals.", "Propagate the requested tolerance across levels."),
            "All sampled values were finite; reported error envelope is ${scientific(tolerance)}.")
    }

    fun parameterized(source: String, integration: IntegrationBound, parameter: String, values: List<Double>, tolerance: Double = 1e-8): ParameterizedIntegralResult {
        require(parameter != integration.variable && values.isNotEmpty() && values.all(Double::isFinite))
        val samples = values.distinct().associateWith { value -> definite(source, integration, mapOf(parameter to value), tolerance) }
        return ParameterizedIntegralResult(parameter, samples, samples.values.all { it.convergence == IntegralConvergence.Convergent && it.value?.isFinite() == true })
    }

    private fun improper(source: String, variable: String, edge: Double, positive: Boolean, tolerance: Double): CertifiedIntegral {
        val compiled = expressions.compile(stripEquation(source)); var evaluations = 0
        val cutoffs = listOf(1e-2, 3e-3, 1e-3, 3e-4, 1e-4, 3e-5, 1e-5)
        val estimates = cutoffs.mapNotNull { epsilon ->
            runCatching {
                fun transformed(t: Double): Double {
                    evaluations++
                    val distance = t / (1 - t)
                    val x = if (positive) edge + distance else edge - distance
                    return compiled.eval(mapOf(variable to x)) / ((1 - t) * (1 - t))
                }
                adaptive(0.0, 1.0 - epsilon, tolerance / 4, ::transformed)
            }.getOrNull()?.takeIf(Double::isFinite)?.let { epsilon to it }
        }
        if (estimates.size < 4) return CertifiedIntegral(null, Double.POSITIVE_INFINITY, evaluations, IntegralConvergence.Unresolved, "infinite-interval transform", emptyList(), "Too few finite truncations for certification.")
        val deltas = estimates.zipWithNext { a, b -> abs(b.second - a.second) }
        val decreasing = deltas.takeLast(4).zipWithNext().all { (a, b) -> b < a }
        val (epsilon1, value1) = estimates[estimates.lastIndex - 1]; val (epsilon2, value2) = estimates.last()
        val extrapolated = (epsilon1 * value2 - epsilon2 * value1) / (epsilon1 - epsilon2)
        val error = max(abs(extrapolated - value2), deltas.last())
        val converged = decreasing && deltas.last() < max(tolerance * 100, 2e-4 * max(1.0, abs(value2)))
        return CertifiedIntegral(extrapolated.takeIf { converged }, error, evaluations, if (converged) IntegralConvergence.Convergent else IntegralConvergence.Divergent, "x=edge+t/(1-t)",
            listOf("Map the infinite tail to 0<=t<1.", "Evaluate successively closer to t=1.", "Require shrinking truncation differences before accepting a value."),
            if (converged) "Last tail correction is ${scientific(error)} and decreases monotonically." else "Tail corrections did not decay to the requested scale; no finite answer was emitted.")
    }

    private fun adaptive(from: Double, to: Double, tolerance: Double, f: (Double) -> Double): Double {
        fun simpson(a: Double, b: Double, fa: Double, fm: Double, fb: Double) = (b - a) * (fa + 4 * fm + fb) / 6
        fun recurse(a: Double, b: Double, fa: Double, fm: Double, fb: Double, whole: Double, tol: Double, depth: Int): Double {
            val middle = (a + b) / 2; val lm = (a + middle) / 2; val rm = (middle + b) / 2; val fl = f(lm); val fr = f(rm)
            val left = simpson(a, middle, fa, fl, fm); val right = simpson(middle, b, fm, fr, fb); val delta = left + right - whole
            return if (depth == 0 || abs(delta) <= 15 * tol) left + right + delta / 15 else recurse(a, middle, fa, fl, fm, left, tol / 2, depth - 1) + recurse(middle, b, fm, fr, fb, right, tol / 2, depth - 1)
        }
        val middle = (from + to) / 2; val fa = f(from); val fm = f(middle); val fb = f(to)
        return recurse(from, to, fa, fm, fb, simpson(from, to, fa, fm, fb), tolerance, 18)
    }

    private fun stripEquation(source: String) = source.substringAfter('=').trim().ifBlank { source.trim() }
    private fun number(value: Double) = String.format(java.util.Locale.US, "%.7f", value).trimEnd('0').trimEnd('.')
    private fun scientific(value: Double) = String.format(java.util.Locale.US, "%.3e", value)
}
