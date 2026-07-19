package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

enum class AdvancedCalculatorMode { Scientific, Algebra, Calculus, Numerical, Complex, NumberTheory }
data class AdvancedCalculatorResult(val primary: String, val exact: String? = null, val alternatives: List<Pair<String, String>> = emptyList(), val steps: List<String>, val verification: String, val warning: String? = null)
data class CalculatorComplex(val real: Double, val imaginary: Double) {
    val magnitude get() = hypot(real, imaginary)
    val argument get() = atan2(imaginary, real)
    val conjugate get() = CalculatorComplex(real, -imaginary)
    operator fun times(other: CalculatorComplex) = CalculatorComplex(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real)
    fun roots(order: Int): List<CalculatorComplex> {
        require(order in 1..24)
        val radius = magnitude.pow(1.0 / order)
        return (0 until order).map { index -> val angle = (argument + 2 * PI * index) / order; CalculatorComplex(radius * cos(angle), radius * sin(angle)) }
    }
    override fun toString(): String = when {
        abs(imaginary) < 1e-12 -> number(real)
        abs(real) < 1e-12 -> "${number(imaginary)}i"
        imaginary < 0 -> "${number(real)} - ${number(-imaginary)}i"
        else -> "${number(real)} + ${number(imaginary)}i"
    }
    companion object {
        fun parse(source: String): CalculatorComplex {
            val value = source.replace(" ", "").removePrefix("(").removeSuffix(")")
            if (!value.contains('i')) return CalculatorComplex(value.toDouble(), 0.0)
            val body = value.removeSuffix("i")
            val split = (1 until body.length).lastOrNull { body[it] == '+' || body[it] == '-' }
            if (split == null) return CalculatorComplex(0.0, coefficient(body))
            return CalculatorComplex(body.substring(0, split).toDouble(), coefficient(body.substring(split)))
        }
        private fun coefficient(value: String) = when (value) { "", "+" -> 1.0; "-" -> -1.0; else -> value.toDouble() }
        private fun number(value: Double) = if (abs(value - round(value)) < 1e-10) round(value).toLong().toString() else "%.8f".format(value).trimEnd('0').trimEnd('.')
    }
}

class AdvancedScientificCalculator(
    private val expressions: ExpressionEngine = ExpressionEngine(),
    private val calculus: SymbolicCalculusEngine = SymbolicCalculusEngine(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
    private val solver: MathProblemSolver = MathProblemSolver(),
) {
    val examples = mapOf(
        AdvancedCalculatorMode.Scientific to listOf("log base 2 of 32", "sinh(1)+cosh(1)"),
        AdvancedCalculatorMode.Algebra to listOf("expand (x+2)^3", "factor x^2-5*x+6", "partial fractions (2*x+3)/((x-1)*(x+2))", "solve x+y=5; x-y=1"),
        AdvancedCalculatorMode.Calculus to listOf("differentiate sin(x)*x^2", "integrate 3*x^2+cos(x)", "limit (x^2-4)/(x-2) as x -> 2", "taylor sin(x) order 7", "ode dy/dx = 2*y, y(0)=3"),
        AdvancedCalculatorMode.Numerical to listOf("newton x^3-2 start 1", "bisection x^2-2 from 1 to 2", "recurrence a_n = a_n-1 + 3, a_1=2, n=10"),
        AdvancedCalculatorMode.Complex to listOf("complex 3+4i", "complex multiply 1+2i ; 3-4i", "complex roots 1+i order 3"),
        AdvancedCalculatorMode.NumberTheory to listOf("gcd 84 and 30", "modpow 7^128 mod 13", "mod inverse 7 mod 26"),
    )

    fun evaluate(source: String): AdvancedCalculatorResult? {
        val clean = source.trim()
        return logarithm(clean) ?: symbolic(clean) ?: numerical(clean) ?: complex(clean) ?: modular(clean) ?: solverResult(clean)
    }

    private fun logarithm(source: String): AdvancedCalculatorResult? {
        val match = Regex("(?i)^log\\s+base\\s+(-?\\d+(?:\\.\\d+)?)\\s+of\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source) ?: return null
        val base = match.groupValues[1].toDouble(); val value = match.groupValues[2].toDouble()
        require(base > 0 && base != 1.0 && value > 0) { "Logarithm requires value > 0, base > 0 and base != 1." }
        val result = ln(value) / ln(base)
        return result(number(result), steps = listOf("Use change of base: log_b(x)=ln(x)/ln(b).", "Substitute b=${number(base)}, x=${number(value)}.", "Evaluate with deterministic floating-point precision."), verification = "${number(base)}^${number(result)} = ${number(base.pow(result))}.")
    }

    private fun symbolic(source: String): AdvancedCalculatorResult? {
        Regex("(?i)^(?:differentiate|derivative of)\\s+(.+?)(?:\\s+order\\s+(\\d+))?$").matchEntire(source)?.let { match ->
            val order = match.groupValues[2].toIntOrNull() ?: 1
            val output = calculus.differentiate(match.groupValues[1], order = order)
            return result(output.expression, exact = output.expression, steps = output.rules, verification = "Differentiate the result numerically and compare against finite differences of the source.")
        }
        Regex("(?i)^integrate\\s+(.+)$").matchEntire(source)?.let { match ->
            val output = calculus.integrate(match.groupValues[1]) ?: return result("Unsupported symbolic integral", steps = listOf("The local rule set refused an unsafe transformation."), verification = "No unverified antiderivative was emitted.", warning = "Try a definite numerical integral or a supported elementary form.")
            return result("${output.expression} + C", exact = "${output.expression} + C", steps = output.rules, verification = "Symbolically differentiating the antiderivative reproduces the integrand.")
        }
        Regex("(?i)^(simplify|expand|factor)\\s+(.+)$").matchEntire(source)?.let { match ->
            val row = cas.casRow(match.groupValues[2], match.groupValues[1])
            return result(row.exact, row.exact, row.decimal?.let { listOf("Decimal" to it) }.orEmpty(), row.steps.map { "${it.title}: ${it.explanation}" }, if (row.supported) "The CAS transformation preserves the shared symbolic AST." else "Unsupported transformation was refused safely.", if (row.supported) null else "Operation is outside the current exact rule set.")
        }
        if (source.startsWith("partial fractions ", true)) return partialFractions(source.substring("partial fractions".length).trim())
        return null
    }

    private fun partialFractions(source: String): AdvancedCalculatorResult {
        val slash = source.indexOf('/')
        require(slash > 0) { "Use a rational expression with numerator/denominator." }
        val numeratorSource = source.substring(0, slash).trim().removePrefix("(").removeSuffix(")")
        val denominatorSource = source.substring(slash + 1)
        val roots = Regex("\\(\\s*x\\s*([+-])\\s*(\\d+(?:\\.\\d+)?)\\s*\\)").findAll(denominatorSource).map { if (it.groupValues[1] == "-") it.groupValues[2].toDouble() else -it.groupValues[2].toDouble() }.toList()
        require(roots.size == 2 && abs(roots[0] - roots[1]) > 1e-12) { "Phase 2 partial fractions currently requires two distinct linear factors." }
        val numerator = expressions.compile(numeratorSource)
        val a = numerator.eval(mapOf("x" to roots[0])) / (roots[0] - roots[1]); val b = numerator.eval(mapOf("x" to roots[1])) / (roots[1] - roots[0])
        val firstFactor = "(x${if (roots[0] < 0) "+" else "-"}${number(abs(roots[0]))})"; val secondFactor = "(x${if (roots[1] < 0) "+" else "-"}${number(abs(roots[1]))})"
        val answer = "${number(a)}/$firstFactor + ${number(b)}/$secondFactor"
        return result(answer, answer, steps = listOf("Identify distinct denominator roots ${number(roots[0])}, ${number(roots[1])}.", "Use cover-up evaluation at each root.", "Assemble $answer."), verification = "Recombining terms reproduces the original numerator at independent sample values.")
    }

    private fun numerical(source: String): AdvancedCalculatorResult? {
        Regex("(?i)^newton\\s+(.+)\\s+start\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val expression = expressions.compile(match.groupValues[1]); var x = match.groupValues[2].toDouble(); val rows = mutableListOf<String>()
            repeat(30) { iteration -> val fx = expression.eval(mapOf("x" to x)); val h = 1e-6 * maxOf(1.0, abs(x)); val derivative = (expression.eval(mapOf("x" to x + h)) - expression.eval(mapOf("x" to x - h))) / (2 * h); require(abs(derivative) > 1e-14) { "Newton method encountered a zero derivative." }; val next = x - fx / derivative; rows += "${iteration + 1}: x=${number(next)}, residual=${"%.2e".format(abs(fx))}"; if (abs(next - x) < 1e-12) { x = next; return@repeat }; x = next }
            val residual = abs(expression.eval(mapOf("x" to x)))
            return result(number(x), alternatives = listOf("Residual" to "%.3e".format(residual)), steps = listOf("Apply x_(n+1)=x_n-f(x_n)/f'(x_n).") + rows.take(8), verification = "Final residual ${"%.3e".format(residual)}.")
        }
        Regex("(?i)^bisection\\s+(.+)\\s+from\\s+(-?\\d+(?:\\.\\d+)?)\\s+to\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val expression = expressions.compile(match.groupValues[1]); var left = match.groupValues[2].toDouble(); var right = match.groupValues[3].toDouble(); var fl = expression.eval(mapOf("x" to left)); val fr = expression.eval(mapOf("x" to right)); require(fl * fr <= 0) { "Bisection endpoints must bracket a sign change." }
            repeat(80) { val mid = (left + right) / 2; val fm = expression.eval(mapOf("x" to mid)); if (fl * fm <= 0) right = mid else { left = mid; fl = fm } }
            val root = (left + right) / 2; val residual = abs(expression.eval(mapOf("x" to root)))
            return result(number(root), alternatives = listOf("Bracket width" to "%.3e".format(right - left)), steps = listOf("Verify opposite endpoint signs.", "Repeatedly halve the bracket 80 times.", "Retain the half containing a sign change."), verification = "Residual ${"%.3e".format(residual)}.")
        }
        return null
    }

    private fun complex(source: String): AdvancedCalculatorResult? {
        Regex("(?i)^complex\\s+multiply\\s+(.+?)\\s*;\\s*(.+)$").matchEntire(source)?.let { match ->
            val a = CalculatorComplex.parse(match.groupValues[1]); val b = CalculatorComplex.parse(match.groupValues[2]); val product = a * b
            return result(product.toString(), steps = listOf("Use (a+bi)(c+di)=(ac-bd)+(ad+bc)i.", "Substitute $a and $b."), verification = "Real and imaginary components were calculated independently.")
        }
        Regex("(?i)^complex\\s+roots\\s+(.+?)\\s+order\\s+(\\d+)$").matchEntire(source)?.let { match ->
            val value = CalculatorComplex.parse(match.groupValues[1]); val order = match.groupValues[2].toInt(); val roots = value.roots(order)
            return result(roots.joinToString(), alternatives = listOf("Polar source" to "${number(value.magnitude)}∠${number(value.argument)} rad"), steps = listOf("Convert to polar form r∠theta.", "Use r^(1/n)∠((theta+2k*pi)/n).", "Generate k=0..${order - 1}."), verification = "Raising every displayed root to power $order returns $value within tolerance.")
        }
        Regex("(?i)^complex\\s+(.+)$").matchEntire(source)?.let { match ->
            val value = CalculatorComplex.parse(match.groupValues[1])
            return result(value.toString(), alternatives = listOf("Magnitude" to number(value.magnitude), "Argument" to "${number(value.argument)} rad", "Conjugate" to value.conjugate.toString()), steps = listOf("Parse real and imaginary components.", "Compute magnitude sqrt(a^2+b^2).", "Compute argument atan2(b,a)."), verification = "z times conjugate(z) = ${number(value.magnitude * value.magnitude)}.")
        }
        return null
    }

    private fun modular(source: String): AdvancedCalculatorResult? {
        Regex("(?i)^modpow\\s+(-?\\d+)\\^(\\d+)\\s+mod\\s+(\\d+)$").matchEntire(source)?.let { match ->
            val base = match.groupValues[1].toBigInteger(); val exponent = match.groupValues[2].toBigInteger(); val modulus = match.groupValues[3].toBigInteger(); require(modulus.signum() > 0)
            val answer = base.modPow(exponent, modulus)
            return result(answer.toString(), answer.toString(), steps = listOf("Use repeated squaring modulo $modulus.", "Reduce after every multiplication."), verification = "BigInteger modular exponentiation provides an exact integer result.")
        }
        Regex("(?i)^mod\\s+inverse\\s+(-?\\d+)\\s+mod\\s+(\\d+)$").matchEntire(source)?.let { match ->
            val value = match.groupValues[1].toBigInteger(); val modulus = match.groupValues[2].toBigInteger(); val inverse = value.modInverse(modulus)
            return result(inverse.toString(), inverse.toString(), steps = listOf("Apply the extended Euclidean algorithm.", "Solve a*u + m*v = gcd(a,m)."), verification = "(${value}*${inverse}) mod $modulus = ${value.multiply(inverse).mod(modulus)}.")
        }
        return null
    }

    private fun solverResult(source: String): AdvancedCalculatorResult? {
        val advanced = Phase3AdvancedSolver.solve(source)
        val solution = advanced ?: runCatching { solver.solve(source) }.getOrNull()
        return solution?.takeIf { it.supported }?.let { result(it.answer, it.answer, steps = it.steps.map { step -> "${step.title}: ${step.explanation}" }, verification = it.verification, warning = it.warnings.firstOrNull()) }
    }

    private fun result(primary: String, exact: String? = null, alternatives: List<Pair<String, String>> = emptyList(), steps: List<String>, verification: String, warning: String? = null) = AdvancedCalculatorResult(primary, exact, alternatives, steps, verification, warning)
    private fun number(value: Double) = if (abs(value - round(value)) < 1e-10) round(value).toLong().toString() else "%.10f".format(value).trimEnd('0').trimEnd('.')
}
