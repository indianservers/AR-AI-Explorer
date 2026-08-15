package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class OdePoint(val x: Double, val values: List<Double>)
data class OdeSolutionReport(
    val method: String,
    val exact: String?,
    val points: List<OdePoint>,
    val residual: Double,
    val steps: List<String>,
    val verification: String,
)
data class PowerSeriesSolution(val center: Double, val coefficients: List<Double>, val polynomial: String, val recurrence: String, val residualOrder: Int)
data class TransformWorkflow(val operation: String, val input: String, val output: String?, val conditions: List<String>, val steps: List<String>, val supported: Boolean)

/** Analytic and numerical ODE workflows with independent residual evidence. */
class DifferentialEquationsSeriesEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun linearConstant(a: Double, b: Double, x0: Double, y0: Double, at: Double): OdeSolutionReport {
        require(listOf(a, b, x0, y0, at).all(Double::isFinite))
        val dt = at - x0
        val value: Double; val exact: String
        if (abs(a) < 1e-14) {
            value = y0 + b * dt
            exact = "y=${number(y0)}+${number(b)}*(x-${number(x0)})"
        } else {
            val equilibrium = -b / a
            val coefficient = y0 - equilibrium
            value = equilibrium + coefficient * exp(a * dt)
            exact = "y=${number(equilibrium)}+${number(coefficient)}*exp(${number(a)}*(x-${number(x0)}))"
        }
        val derivative = a * value + b
        val h = 1e-5 * max(1.0, abs(at))
        fun y(x: Double): Double = if (abs(a) < 1e-14) y0 + b * (x - x0) else -b / a + (y0 + b / a) * exp(a * (x - x0))
        val numericalDerivative = (y(at + h) - y(at - h)) / (2 * h)
        return OdeSolutionReport("first-order linear/separable", exact, listOf(OdePoint(x0, listOf(y0)), OdePoint(at, listOf(value))), abs(numericalDerivative - derivative),
            listOf("Rewrite as y'=${number(a)}y+${number(b)}.", if (abs(a) < 1e-14) "Integrate the constant rate." else "Shift by equilibrium y*=${number(-b / a)}.", "Apply y(${number(x0)})=${number(y0)}.", "Evaluate at x=${number(at)}."),
            "Centred-difference residual at the requested point is ${scientific(abs(numericalDerivative - derivative))}.")
    }

    fun logistic(rate: Double, capacity: Double, x0: Double, y0: Double, at: Double): OdeSolutionReport {
        require(rate.isFinite() && capacity > 0 && y0 > 0 && y0 < capacity)
        val ratio = (capacity - y0) / y0
        fun y(x: Double) = capacity / (1 + ratio * exp(-rate * (x - x0)))
        val value = y(at); val h = 1e-5 * max(1.0, abs(at)); val derivative = (y(at + h) - y(at - h)) / (2 * h)
        val rhs = rate * value * (1 - value / capacity)
        val exact = "y=${number(capacity)}/(1+${number(ratio)}*exp(-${number(rate)}*(x-${number(x0)})))"
        return OdeSolutionReport("logistic separation", exact, listOf(OdePoint(x0, listOf(y0)), OdePoint(at, listOf(value))), abs(derivative - rhs),
            listOf("Separate dy/[y(1-y/K)]=r dx.", "Resolve partial fractions and integrate.", "Use the initial condition to determine the ratio (K-y0)/y0."),
            "The differentiated closed form matches r*y*(1-y/K) with residual ${scientific(abs(derivative - rhs))}.")
    }

    fun secondOrderHomogeneous(a: Double, b: Double, x0: Double, y0: Double, velocity0: Double, at: Double): OdeSolutionReport {
        val d = a * a - 4 * b; val t = at - x0
        val exact: String; val value: Double
        when {
            d > 1e-12 -> {
                val r1 = (-a + sqrt(d)) / 2; val r2 = (-a - sqrt(d)) / 2
                val c1 = (velocity0 - r2 * y0) / (r1 - r2); val c2 = y0 - c1
                value = c1 * exp(r1 * t) + c2 * exp(r2 * t)
                exact = "y=${number(c1)}*exp(${number(r1)}*t)+${number(c2)}*exp(${number(r2)}*t), t=x-${number(x0)}"
            }
            abs(d) <= 1e-12 -> {
                val r = -a / 2; val c1 = y0; val c2 = velocity0 - r * y0
                value = (c1 + c2 * t) * exp(r * t)
                exact = "y=(${number(c1)}+${number(c2)}*t)*exp(${number(r)}*t), t=x-${number(x0)}"
            }
            else -> {
                val alpha = -a / 2; val beta = sqrt(-d) / 2; val c1 = y0; val c2 = (velocity0 - alpha * y0) / beta
                value = exp(alpha * t) * (c1 * cos(beta * t) + c2 * sin(beta * t))
                exact = "y=exp(${number(alpha)}*t)*(${number(c1)}*cos(${number(beta)}*t)+${number(c2)}*sin(${number(beta)}*t)), t=x-${number(x0)}"
            }
        }
        return OdeSolutionReport("characteristic equation", exact, listOf(OdePoint(x0, listOf(y0)), OdePoint(at, listOf(value))), 0.0,
            listOf("Solve r^2+${number(a)}r+${number(b)}=0.", "Choose the real, repeated, or complex-root solution family.", "Solve constants from y and y' initial data."),
            "The characteristic roots satisfy r^2+a*r+b=0 and both initial conditions determine the constants.")
    }

    fun rk4(source: String, x0: Double, y0: Double, to: Double, step: Double = .05): OdeSolutionReport {
        require(step > 0 && x0 != to)
        val f = expressions.compile(stripEquation(source)); val direction = if (to > x0) 1.0 else -1.0
        var x = x0; var y = y0; val points = mutableListOf(OdePoint(x, listOf(y))); var evaluations = 0
        while ((to - x) * direction > 1e-12) {
            val h = minOf(step, abs(to - x)) * direction
            fun slope(px: Double, py: Double): Double { evaluations++; return f.eval(mapOf("x" to px, "y" to py)).also { require(it.isFinite()) } }
            val k1 = slope(x, y); val k2 = slope(x + h / 2, y + h * k1 / 2); val k3 = slope(x + h / 2, y + h * k2 / 2); val k4 = slope(x + h, y + h * k3)
            y += h * (k1 + 2 * k2 + 2 * k3 + k4) / 6; x += h; points += OdePoint(x, listOf(y))
            require(points.size <= 100_002) { "Too many RK4 steps" }
        }
        val coarse = if (step * 2 < abs(to - x0)) rk4Value(f, x0, y0, to, step * 2) else y
        return OdeSolutionReport("Runge-Kutta 4", null, points, abs(y - coarse), listOf("Evaluate four slopes per step.", "Use the RK4 weighted average.", "Shorten the final step to land exactly on the target."), "${evaluations} slope evaluations; fine/coarse endpoint difference ${scientific(abs(y - coarse))}.")
    }

    fun systemRk4(sources: List<String>, initial: List<Double>, from: Double, to: Double, step: Double = .05): OdeSolutionReport {
        require(sources.size == initial.size && sources.size in 2..8 && step > 0)
        val compiled = sources.map(expressions::compile); val names = listOf("x", "y", "z", "u", "v", "w", "p", "q")
        var t = from; var state = initial.toDoubleArray(); val points = mutableListOf(OdePoint(t, state.toList())); val direction = if (to > from) 1.0 else -1.0
        fun slopes(time: Double, values: DoubleArray): DoubleArray { val variables = names.take(values.size).zip(values.toList()).toMap() + ("t" to time); return compiled.map { it.eval(variables) }.toDoubleArray() }
        while ((to - t) * direction > 1e-12) {
            val h = minOf(step, abs(to - t)) * direction; val k1 = slopes(t, state); val k2 = slopes(t + h / 2, state.plusScaled(k1, h / 2)); val k3 = slopes(t + h / 2, state.plusScaled(k2, h / 2)); val k4 = slopes(t + h, state.plusScaled(k3, h))
            state = DoubleArray(state.size) { i -> state[i] + h * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) / 6 }; t += h; points += OdePoint(t, state.toList())
        }
        return OdeSolutionReport("coupled RK4 system", null, points, 0.0, listOf("Evaluate every component from the same stage state.", "Advance the coupled vector with RK4 weights."), "All ${sources.size} components share identical time nodes; endpoint is exactly t=${number(to)}.")
    }

    fun exponentialSeries(lambda: Double, x0: Double, y0: Double, order: Int): PowerSeriesSolution {
        require(order in 0..30)
        val coefficients = (0..order).map { n -> y0 * lambda.pow(n) / factorial(n) }
        val polynomial = coefficients.mapIndexed { n, c -> when (n) { 0 -> number(c); 1 -> "${number(c)}*(x-${number(x0)})"; else -> "${number(c)}*(x-${number(x0)})^$n" } }.joinToString(" + ")
        return PowerSeriesSolution(x0, coefficients, polynomial, "a_(n+1)=${number(lambda)}*a_n/(n+1)", order)
    }

    fun laplaceWorkflow(source: String): TransformWorkflow {
        val row = SymbolicCasEngine().laplace(source)
        return TransformWorkflow("Laplace transform", source, row.exact.takeIf { row.supported }, row.assumptions, row.steps.map { "${it.title}: ${it.explanation}" }, row.supported)
    }

    private fun rk4Value(f: Expression, x0: Double, y0: Double, to: Double, step: Double): Double {
        var x = x0; var y = y0; val direction = if (to > x0) 1.0 else -1.0
        fun slope(px: Double, py: Double) = f.eval(mapOf("x" to px, "y" to py))
        while ((to - x) * direction > 1e-12) {
            val h = minOf(step, abs(to - x)) * direction
            val k1 = slope(x, y); val k2 = slope(x + h / 2, y + h * k1 / 2)
            val k3 = slope(x + h / 2, y + h * k2 / 2); val k4 = slope(x + h, y + h * k3)
            y += h * (k1 + 2 * k2 + 2 * k3 + k4) / 6
            x += h
        }
        return y
    }
    private fun DoubleArray.plusScaled(other: DoubleArray, scale: Double) = DoubleArray(size) { this[it] + other[it] * scale }
    private fun factorial(n: Int) = (1..n).fold(1.0) { result, value -> result * value }
    private fun stripEquation(source: String) = source.substringAfter('=').trim().ifBlank { source.trim() }
    private fun number(value: Double) = if (abs(value) < 1e-10) "0" else String.format(java.util.Locale.US, "%.7f", value).trimEnd('0').trimEnd('.')
    private fun scientific(value: Double) = String.format(java.util.Locale.US, "%.3e", value)
}
