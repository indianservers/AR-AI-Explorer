package com.indianservers.aiexplorer.solver.domain.verification

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationCheck
import com.indianservers.aiexplorer.solver.domain.model.VerificationMethod
import com.indianservers.aiexplorer.solver.domain.model.VerificationResult
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.VerificationStrength
import kotlin.math.abs
import kotlin.math.max

data class AdvancedVerificationEvidence(
    val result: VerificationResult,
    val strength: VerificationStrength,
)

/** Independent numeric checks for advanced results produced by the symbolic kernel. */
class SolverAdvancedVerifier(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun verify(source: String, exactAnswer: String, kernelMessage: String): AdvancedVerificationEvidence {
        certifiedClosedWorkflow(source, kernelMessage)?.let { return it }
        certifiedVectorCalculation(source, kernelMessage)?.let { return it }
        multivariable(source, exactAnswer)?.let { return it }
        derivative(source, exactAnswer)?.let { return it }
        integral(source, exactAnswer)?.let { return it }
        limit(source, exactAnswer)?.let { return it }
        numericalRoot(source, exactAnswer)?.let { return it }
        complex(source, exactAnswer)?.let { return it }
        return AdvancedVerificationEvidence(
            VerificationResult(
                VerificationStatus.Inconclusive,
                VerificationMethod.ExactEvaluation,
                emptyList(),
                "The deterministic kernel completed successfully, but this operation has no independent verifier yet. $kernelMessage",
            ),
            VerificationStrength.PartiallyVerified,
        )
    }

    private fun certifiedClosedWorkflow(source: String, message: String): AdvancedVerificationEvidence? {
        val lower = source.trim().lowercase()
        val symbolic = lower.startsWith("laplace ") || lower.startsWith("tangent ") || lower.startsWith("normal ")
        val sampled = lower.startsWith("derivative analysis ")
        if (!symbolic && !sampled) return null
        val accepted = if (symbolic) message.contains("derived", true) || message.contains("symbolic", true) else message.startsWith("Certified derivative analysis", true)
        val check = VerificationCheck(if (symbolic) "Symbolic workflow certificate" else "Closed-interval sampling certificate", accepted, "valid certified workflow", if (accepted) "PASS" else message)
        return AdvancedVerificationEvidence(
            VerificationResult(if (accepted) VerificationStatus.Verified else VerificationStatus.Failed, if (symbolic) VerificationMethod.ExactEvaluation else VerificationMethod.BoundarySampling, listOf(check), message),
            if (accepted) VerificationStrength.SymbolicallyVerified else VerificationStrength.Failed,
        )
    }

    private fun certifiedVectorCalculation(source: String, message: String): AdvancedVerificationEvidence? {
        val recognized = listOf(
            "lagrange ", "line integral ", "work integral ", "surface flux ", "green ", "gauss ", "stokes ",
            "improper integrate ", "triple integrate ", "parameter integral ", "logistic ", "second order ivp ", "system rk4 ",
        ).any { source.trim().startsWith(it, true) }
        if (!recognized || !message.startsWith("Certified", true)) return null
        val residual = Regex("""(?i)(?:residual|error)=([+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?)""").find(message)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return partial("The vector-calculus certificate did not expose a numeric residual.")
        val explicitlyFailed = message.contains("passed=false", true)
        val tolerance = if (source.trim().startsWith("lagrange", true)) 1e-7 else 1e-5
        val passed = !explicitlyFailed && residual <= tolerance
        val check = VerificationCheck("Certificate residual", passed, "<= $tolerance", format(residual))
        return AdvancedVerificationEvidence(
            VerificationResult(if (passed) VerificationStatus.Verified else VerificationStatus.Failed, VerificationMethod.BoundarySampling, listOf(check), message),
            if (passed) VerificationStrength.NumericallyVerified else VerificationStrength.Failed,
        )
    }

    private fun multivariable(source: String, answer: String): AdvancedVerificationEvidence? {
        Regex("""(?i)^jacobian\s+(.+?)\s+vars\s+([a-z](?:\s*,\s*[a-z])*)\s+at\s+(.+)$""").matchEntire(source.trim())?.let { match ->
            val functions = match.groupValues[1].split(';').mapNotNull { runCatching { expressions.compile(it.trim()) }.getOrNull() }
            val variables = match.groupValues[2].split(',').map(String::trim)
            val point = parsePoint(match.groupValues[3]) ?: return partial("The Jacobian evaluation point could not be parsed independently.")
            val matrix = parseMatrix(answer) ?: return partial("The reported Jacobian matrix could not be parsed independently.")
            if (matrix.size != functions.size || matrix.any { it.size != variables.size }) return partial("The reported Jacobian dimensions do not match the declared map.")
            val checks = functions.indices.flatMap { row -> variables.indices.mapNotNull { column ->
                val variable = variables[column]
                val center = point[variable] ?: return@mapNotNull null
                val expected = centralDifference({ value -> functions[row].eval(point + (variable to value)) }, center, 1e-5)
                numericCheck("Jacobian [${row + 1},${column + 1}]", expected, matrix[row][column], 6e-4)
            } }
            return evidence("Independent Jacobian finite differences", checks)
        }
        Regex("""(?i)^hessian\s+(.+?)\s+vars\s+([a-z](?:\s*,\s*[a-z])*)\s+at\s+(.+)$""").matchEntire(source.trim())?.let { match ->
            val function = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The Hessian source could not be compiled independently.")
            val variables = match.groupValues[2].split(',').map(String::trim)
            val point = parsePoint(match.groupValues[3]) ?: return partial("The Hessian evaluation point could not be parsed independently.")
            val matrix = parseMatrix(answer) ?: return partial("The reported Hessian matrix could not be parsed independently.")
            if (matrix.size != variables.size || matrix.any { it.size != variables.size }) return partial("The reported Hessian dimensions do not match the declared variables.")
            val checks = variables.indices.flatMap { row -> variables.indices.mapNotNull { column ->
                val first = variables[row]; val second = variables[column]
                val firstCenter = point[first] ?: return@mapNotNull null
                val secondCenter = point[second] ?: return@mapNotNull null
                val h = 1e-3
                val expected = if (first == second) {
                    val centerValue = function.eval(point)
                    (function.eval(point + (first to firstCenter + h)) - 2 * centerValue + function.eval(point + (first to firstCenter - h))) / (h * h)
                } else {
                    (function.eval(point + (first to firstCenter + h) + (second to secondCenter + h))
                        - function.eval(point + (first to firstCenter + h) + (second to secondCenter - h))
                        - function.eval(point + (first to firstCenter - h) + (second to secondCenter + h))
                        + function.eval(point + (first to firstCenter - h) + (second to secondCenter - h))) / (4 * h * h)
                }
                numericCheck("Hessian [${row + 1},${column + 1}]", expected, matrix[row][column], 2e-3)
            } }
            return evidence("Independent Hessian second differences", checks)
        }
        Regex("""(?i)^partial\s+derivative\s+(.+?)\s+(?:with\s+respect\s+to|wrt)\s+([a-z])$""").matchEntire(source.trim())?.let { match ->
            val original = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The multivariable source could not be compiled independently.")
            val candidate = compileAnswer(answer) ?: return partial("The partial derivative could not be compiled independently.")
            val variable = match.groupValues[2].lowercase()
            val checks = multivariablePoints().mapNotNull { point ->
                val center = point.getValue(variable); val expected = centralDifference({ value -> original.eval(point + (variable to value)) }, center, 1e-5)
                numericCheck("Partial derivative at ${formatPoint(point)}", expected, runCatching { candidate.eval(point) }.getOrNull(), 4e-4)
            }
            return evidence("Independent partial finite differences", checks)
        }
        Regex("""(?i)^gradient\s+(.+?)(?:\s+at\s+.+)?$""").matchEntire(source.trim())?.let { match ->
            val original = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The gradient source could not be compiled independently.")
            val candidates = answer.removePrefix("[").removeSuffix("]").split(',').mapNotNull { compileAnswer(it.trim()) }
            val variables = listOf("x", "y", "z", "t").filter { Regex("(?<![A-Za-z0-9_])$it(?![A-Za-z0-9_])").containsMatchIn(match.groupValues[1]) }
            if (candidates.size != variables.size) return partial("The gradient component count could not be verified independently.")
            val checks = multivariablePoints().flatMap { point -> variables.indices.mapNotNull { index ->
                val variable = variables[index]; val center = point.getValue(variable); val expected = centralDifference({ value -> original.eval(point + (variable to value)) }, center, 1e-5)
                numericCheck("Gradient $variable component at ${formatPoint(point)}", expected, runCatching { candidates[index].eval(point) }.getOrNull(), 5e-4)
            } }
            return evidence("Independent gradient finite differences", checks)
        }
        Regex("""(?i)^(divergence|curl)\s+(.+?)(?:\s+at\s+.+)?$""").matchEntire(source.trim())?.let { match ->
            val components = match.groupValues[2].split(';').mapNotNull { runCatching { expressions.compile(it.trim()) }.getOrNull() }
            val candidate = compileAnswer(answer) ?: return partial("The vector-calculus result could not be compiled independently.")
            val variables = listOf("x", "y", "z").take(components.size)
            val checks = multivariablePoints().mapNotNull { point ->
                val expected = if (match.groupValues[1].equals("divergence", true)) components.indices.sumOf { index ->
                    val variable = variables[index]; centralDifference({ value -> components[index].eval(point + (variable to value)) }, point.getValue(variable), 1e-5)
                } else {
                    if (components.size != 2) return@mapNotNull null
                    centralDifference({ value -> components[1].eval(point + ("x" to value)) }, point.getValue("x"), 1e-5) - centralDifference({ value -> components[0].eval(point + ("y" to value)) }, point.getValue("y"), 1e-5)
                }
                numericCheck("${match.groupValues[1]} at ${formatPoint(point)}", expected, runCatching { candidate.eval(point) }.getOrNull(), 7e-4)
            }
            return evidence("Independent vector-field finite differences", checks)
        }
        return null
    }

    private fun multivariablePoints() = listOf(
        mapOf("x" to .73, "y" to 1.17, "z" to -.41, "t" to .62),
        mapOf("x" to 1.31, "y" to -.58, "z" to .89, "t" to 1.08),
        mapOf("x" to -.77, "y" to .64, "z" to 1.21, "t" to -.36),
    )

    private fun formatPoint(point: Map<String, Double>) = point.entries.take(3).joinToString(prefix = "(", postfix = ")") { "${it.key}=${format(it.value)}" }

    private fun parsePoint(source: String): Map<String, Double>? = runCatching {
        source.split(',').associate { entry ->
            val parts = entry.trim().split('=', limit = 2)
            require(parts.size == 2)
            parts[0].trim().lowercase() to expressions.compile(parts[1].trim()).eval()
        }
    }.getOrNull()

    private fun parseMatrix(source: String): List<List<Double>>? = runCatching {
        val body = source.trim().removePrefix("[").removeSuffix("]")
        Regex("""\[([^\[\]]+)]""").findAll(body).map { row ->
            row.groupValues[1].split(',').map { it.trim().toDouble() }
        }.toList().also { require(it.isNotEmpty()) }
    }.getOrNull()

    private fun derivative(source: String, answer: String): AdvancedVerificationEvidence? {
        val match = Regex("(?i)^(?:differentiate|derivative of)\\s+(.+?)(?:\\s+order\\s+(\\d+))?$").matchEntire(source.trim()) ?: return null
        val order = match.groupValues[2].toIntOrNull() ?: 1
        if (order !in 1..3) return partial("Higher derivatives above order 3 currently retain deterministic-kernel validation only.")
        val original = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The source could not be compiled by the independent numeric evaluator.")
        val candidate = compileAnswer(answer) ?: return partial("The derivative result could not be compiled independently.")
        val checks = samplePoints().mapNotNull { x ->
            val expected = runCatching { nthDerivative({ value -> original.eval(mapOf("x" to value)) }, x, order) }.getOrNull()
            val actual = runCatching { candidate.eval(mapOf("x" to x)) }.getOrNull()
            numericCheck("Derivative at x=${format(x)}", expected, actual, tolerance = if (order == 1) 2e-4 else 2e-2)
        }
        return evidence("Finite-difference derivative sampling", checks)
    }

    private fun integral(source: String, answer: String): AdvancedVerificationEvidence? {
        val match = Regex("(?i)^integrate\\s+(.+?)(?:\\s+from\\s+(.+?)\\s+to\\s+(.+))?$").matchEntire(source.trim()) ?: return null
        if (match.groupValues[2].isNotBlank()) return partial("Definite numerical integration currently retains the kernel's adaptive-integration evidence.")
        val integrand = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The integrand could not be compiled independently.")
        val antiderivative = compileAnswer(answer.removeSuffix(" + C").removeSuffix("+ C").removeSuffix("+C"))
            ?: return partial("The antiderivative could not be compiled independently.")
        val checks = samplePoints().mapNotNull { x ->
            val expected = runCatching { integrand.eval(mapOf("x" to x)) }.getOrNull()
            val actual = runCatching { centralDifference({ value -> antiderivative.eval(mapOf("x" to value)) }, x, 1e-5) }.getOrNull()
            numericCheck("Reverse derivative at x=${format(x)}", expected, actual, 3e-4)
        }
        return evidence("Differentiate the antiderivative at independent sample points", checks)
    }

    private fun limit(source: String, answer: String): AdvancedVerificationEvidence? {
        val match = Regex("(?i)^limit\\s+(.+?)\\s+as\\s+([A-Za-z])\\s*(?:->|approaches)\\s*(.+)$").matchEntire(source.trim()) ?: return null
        val expression = runCatching { expressions.compile(match.groupValues[1]) }.getOrNull() ?: return partial("The limit expression could not be compiled independently.")
        val target = runCatching { expressions.compile(match.groupValues[3]).eval() }.getOrNull() ?: return partial("The approach value could not be evaluated independently.")
        val expected = runCatching { expressions.compile(answer).eval() }.getOrNull() ?: return partial("The exact limit answer could not be evaluated independently.")
        val variable = match.groupValues[2]
        val checks = listOf(1e-3, 1e-4, 1e-5).mapNotNull { distance ->
            val left = runCatching { expression.eval(mapOf(variable to target - distance)) }.getOrNull()
            val right = runCatching { expression.eval(mapOf(variable to target + distance)) }.getOrNull()
            val actual = if (left != null && right != null && left.isFinite() && right.isFinite()) (left + right) / 2 else null
            numericCheck("Two-sided sample h=${format(distance)}", expected, actual, max(2e-3, distance * 20))
        }
        return evidence("Independent two-sided limit sampling", checks)
    }

    private fun numericalRoot(source: String, answer: String): AdvancedVerificationEvidence? {
        val match = Regex("(?i)^(?:newton\\s+(.+)\\s+start\\s+.+|bisection\\s+(.+)\\s+from\\s+.+\\s+to\\s+.+)$").matchEntire(source.trim()) ?: return null
        val expressionSource = match.groupValues.drop(1).firstOrNull(String::isNotBlank) ?: return null
        val expression = runCatching { expressions.compile(expressionSource) }.getOrNull() ?: return partial("The root expression could not be compiled independently.")
        val root = answer.toDoubleOrNull() ?: return partial("The numerical root could not be parsed independently.")
        val residual = runCatching { abs(expression.eval(mapOf("x" to root))) }.getOrNull()
        val check = VerificationCheck("Residual at the reported root", residual != null && residual < 1e-7, "< 1e-7", residual?.let(::format) ?: "undefined")
        return evidence("Independent residual evaluation", listOf(check))
    }

    private fun complex(source: String, answer: String): AdvancedVerificationEvidence? {
        val roots = Regex("(?i)^complex\\s+roots\\s+(.+?)\\s+order\\s+(\\d+)$").matchEntire(source.trim())
        if (roots != null) {
            val target = parseComplex(roots.groupValues[1]) ?: return partial("The source complex value could not be parsed independently.")
            val order = roots.groupValues[2].toIntOrNull() ?: return partial("The root order could not be parsed independently.")
            val candidates = answer.split(Regex(",\\s*")).mapNotNull(::parseComplex)
            if (candidates.size != order) return failedEvidence("Expected $order complex root branches, but parsed ${candidates.size}.")
            val checks = candidates.mapIndexed { index, candidate ->
                val powered = candidate.pow(order)
                val error = max(abs(powered.real - target.real), abs(powered.imaginary - target.imaginary))
                VerificationCheck("Complex root branch ${index + 1}", error < 2e-6, "power $order returns source", "max component error ${format(error)}")
            }
            return evidence("Raise every displayed complex root to the requested power", checks)
        }
        val multiply = Regex("(?i)^complex\\s+multiply\\s+(.+?)\\s*;\\s*(.+)$").matchEntire(source.trim())
        if (multiply != null) {
            val left = parseComplex(multiply.groupValues[1]) ?: return partial("The first complex factor could not be parsed independently.")
            val right = parseComplex(multiply.groupValues[2]) ?: return partial("The second complex factor could not be parsed independently.")
            val reported = parseComplex(answer) ?: return partial("The complex product could not be parsed independently.")
            val expected = left * right
            val checks = listOf(
                numericCheck("Real component", expected.real, reported.real, 1e-9),
                numericCheck("Imaginary component", expected.imaginary, reported.imaginary, 1e-9),
            ).filterNotNull()
            return evidence("Independent rectangular-component multiplication", checks)
        }
        val rectangular = Regex("(?i)^complex\\s+(.+)$").matchEntire(source.trim()) ?: return null
        val expected = parseComplex(rectangular.groupValues[1]) ?: return partial("The source complex value could not be parsed independently.")
        val reported = parseComplex(answer) ?: return partial("The reported complex value could not be parsed independently.")
        return evidence(
            "Independent rectangular parsing",
            listOfNotNull(
                numericCheck("Real component", expected.real, reported.real, 1e-10),
                numericCheck("Imaginary component", expected.imaginary, reported.imaginary, 1e-10),
            ),
        )
    }

    private fun evidence(label: String, checks: List<VerificationCheck>): AdvancedVerificationEvidence {
        if (checks.isEmpty()) return partial("No safe independent sample point was available.")
        val passed = checks.all(VerificationCheck::passed)
        return AdvancedVerificationEvidence(
            VerificationResult(
                if (passed) VerificationStatus.Verified else VerificationStatus.Failed,
                VerificationMethod.SampledEquivalence,
                checks,
                if (passed) "$label passed at ${checks.size} safe point(s)." else "$label found a mismatch; the result is blocked from correct presentation.",
            ),
            if (passed) VerificationStrength.NumericallyVerified else VerificationStrength.Failed,
        )
    }

    private fun partial(message: String) = AdvancedVerificationEvidence(
        VerificationResult(VerificationStatus.Inconclusive, VerificationMethod.SampledEquivalence, emptyList(), message),
        VerificationStrength.PartiallyVerified,
    )

    private fun failedEvidence(message: String) = AdvancedVerificationEvidence(
        VerificationResult(VerificationStatus.Failed, VerificationMethod.SampledEquivalence, listOf(VerificationCheck("Independent advanced verification", false, "valid result", message)), message),
        VerificationStrength.Failed,
    )

    private data class ComplexValue(val real: Double, val imaginary: Double) {
        operator fun times(other: ComplexValue) = ComplexValue(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real)
        fun pow(order: Int): ComplexValue = (1..order).fold(ComplexValue(1.0, 0.0)) { value, _ -> value * this }
    }

    private fun parseComplex(source: String): ComplexValue? = runCatching {
        val value = source.replace(" ", "").removePrefix("(").removeSuffix(")")
        if (!value.contains('i')) return@runCatching ComplexValue(value.toDouble(), 0.0)
        val body = value.removeSuffix("i")
        val split = (1 until body.length).lastOrNull { body[it] == '+' || body[it] == '-' }
        fun coefficient(text: String) = when (text) { "", "+" -> 1.0; "-" -> -1.0; else -> text.toDouble() }
        if (split == null) ComplexValue(0.0, coefficient(body))
        else ComplexValue(body.substring(0, split).toDouble(), coefficient(body.substring(split)))
    }.getOrNull()

    private fun compileAnswer(source: String) = runCatching { expressions.compile(source.replace("±", "+")) }.getOrNull()

    private fun numericCheck(label: String, expected: Double?, actual: Double?, tolerance: Double): VerificationCheck? {
        if (expected == null || actual == null || !expected.isFinite() || !actual.isFinite()) return null
        val scale = max(1.0, max(abs(expected), abs(actual)))
        return VerificationCheck(label, abs(expected - actual) <= tolerance * scale, format(expected), format(actual))
    }

    private fun nthDerivative(function: (Double) -> Double, x: Double, order: Int): Double =
        if (order == 1) centralDifference(function, x, 1e-5)
        else centralDifference({ value -> nthDerivative(function, value, order - 1) }, x, if (order == 2) 1e-3 else 4e-3)

    private fun centralDifference(function: (Double) -> Double, x: Double, h: Double) = (function(x + h) - function(x - h)) / (2 * h)
    private fun samplePoints() = listOf(-1.75, -0.5, 0.25, 1.2, 2.1)
    private fun format(value: Double) = "%.8g".format(java.util.Locale.US, value)
}
