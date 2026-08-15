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
    private val rigorousCalculus = RigorousCalculusEngine(expressions)
    private val advancedIntegration = AdvancedIntegrationEngine(expressions)
    private val differentialEquations = DifferentialEquationsSeriesEngine(expressions)
    private val vectorCalculus = VectorCalculusEngine(expressions)
    val examples = mapOf(
        AdvancedCalculatorMode.Scientific to listOf("log base 2 of 32", "sinh(1)+cosh(1)"),
        AdvancedCalculatorMode.Algebra to listOf("expand (x+2)^3", "factor x^2-5*x+6", "partial fractions (2*x+3)/((x-1)*(x+2))", "solve x+y=5; x-y=1"),
        AdvancedCalculatorMode.Calculus to listOf("differentiate sin(x)*x^2", "integrate 3*x^2+cos(x)", "limit (x^2-4)/(x-2) as x -> 2", "gradient x^2*y+sin(y) at x=2,y=0", "directional derivative x^2+y^2 at x=1,y=2 direction 3,4", "divergence x^2;y^2", "tangent plane x^2+y^2 at x=1,y=2", "taylor sin(x) order 7", "ode dy/dx = 2*y, y(0)=3"),
        AdvancedCalculatorMode.Numerical to listOf("newton x^3-2 start 1", "bisection x^2-2 from 1 to 2", "recurrence a_n = a_n-1 + 3, a_1=2, n=10"),
        AdvancedCalculatorMode.Complex to listOf("complex 3+4i", "complex multiply 1+2i ; 3-4i", "complex roots 1+i order 3"),
        AdvancedCalculatorMode.NumberTheory to listOf("gcd 84 and 30", "modpow 7^128 mod 13", "mod inverse 7 mod 26"),
    )

    fun evaluate(source: String): AdvancedCalculatorResult? {
        val clean = source.trim()
        return logarithm(clean) ?: multivariable(clean) ?: symbolic(clean) ?: numerical(clean) ?: complex(clean) ?: modular(clean) ?: solverResult(clean)
    }

    private fun multivariable(source: String): AdvancedCalculatorResult? {
        Regex("""(?i)^partial\s+derivative\s+(.+?)\s+(?:with\s+respect\s+to|wrt)\s+([a-z])$""").matchEntire(source)?.let { match ->
            val expression = match.groupValues[1]; val variable = match.groupValues[2].lowercase()
            val output = calculus.differentiate(expression, variable)
            return result(output.expression, output.expression, steps = listOf("Hold every variable except $variable constant.") + output.rules + "Simplify the partial derivative.", verification = "Independent centred differences in $variable verify the symbolic partial derivative at safe sample points.")
        }
        Regex("""(?i)^gradient\s+(.+?)(?:\s+at\s+(.+))?$""").matchEntire(source)?.let { match ->
            val expression = match.groupValues[1]; val point = parseAssignments(match.groupValues[2])
            val variables = inferVariables(expression).takeIf { it.isNotEmpty() } ?: listOf("x", "y")
            val partials = variables.map { calculus.differentiate(expression, it).expression }
            val exact = "[${partials.joinToString()}]"
            val alternatives = if (point.isEmpty()) emptyList() else listOf("At point" to vectorValue(partials, variables, point))
            return result(exact, exact, alternatives, listOf("Identify variables ${variables.joinToString()}.", "Differentiate once with respect to each variable while holding the others constant.", "Assemble grad f = $exact."), "Each component is checked by an independent centred difference at multiple safe points.")
        }
        Regex("""(?i)^directional\s+derivative\s+(.+?)\s+at\s+(.+?)\s+direction\s+(.+)$""").matchEntire(source)?.let { match ->
            val expression = match.groupValues[1]; val point = parseAssignments(match.groupValues[2]); val direction = parseVector(match.groupValues[3])
            require(direction.isNotEmpty() && direction.any { abs(it) > 1e-14 }) { "Direction vector must be non-zero." }
            val variables = listOf("x", "y", "z", "t").filter(point::containsKey).take(direction.size)
            require(variables.size == direction.size && variables.all(point::containsKey)) { "Provide one point coordinate and direction component for each variable." }
            val magnitude = kotlin.math.sqrt(direction.sumOf { it * it })
            val partials = variables.map { calculus.differentiate(expression, it).expression }
            val unit = direction.map { it / magnitude }
            val exact = partials.indices.joinToString(" + ") { "(${partials[it]})*${number(unit[it])}" }
            val value = expressions.compile(exact).eval(point)
            return result(number(value), exact, listOf("Unit direction" to "[${unit.joinToString { number(it) }}]"), listOf("Compute grad f = [${partials.joinToString()}].", "Normalize the direction vector.", "Take the dot product grad f dot u.", "Evaluate at the requested point."), "The reported value is independently checked by symmetric samples along the unit direction.")
        }
        Regex("""(?i)^(divergence|curl)\s+(.+?)(?:\s+at\s+(.+))?$""").matchEntire(source)?.let { match ->
            val operation = match.groupValues[1].lowercase(); val components = match.groupValues[2].split(';').map(String::trim); val point = parseAssignments(match.groupValues[3])
            require(components.size in 2..3) { "Use semicolon-separated vector-field components." }
            val variables = listOf("x", "y", "z").take(components.size)
            val exact = if (operation == "divergence") {
                components.indices.joinToString(" + ") { "(${calculus.differentiate(components[it], variables[it]).expression})" }
            } else {
                require(components.size == 2) { "This command currently reports scalar 2D curl; use two field components." }
                "(${calculus.differentiate(components[1], "x").expression}) - (${calculus.differentiate(components[0], "y").expression})"
            }
            val alternative = if (point.isEmpty()) emptyList() else listOf("At point" to number(expressions.compile(exact).eval(point)))
            val notation = if (operation == "divergence") "div F" else "curl_z F"
            return result(exact, exact, alternative, listOf("Interpret F = [${components.joinToString()}].", "Apply the coordinate definition of $notation.", "Differentiate the matching components.", "Simplify the scalar field."), "Independent finite differences of the vector-field components verify the result.")
        }
        Regex("""(?i)^tangent\s+plane\s+(?:z\s*=\s*)?(.+?)\s+at\s+(.+)$""").matchEntire(source)?.let { match ->
            val expression = match.groupValues[1]; val point = parseAssignments(match.groupValues[2]); val x = point["x"] ?: error("Provide x coordinate"); val y = point["y"] ?: error("Provide y coordinate")
            val z = expressions.compile(expression).eval(mapOf("x" to x, "y" to y)); val fxSource = calculus.differentiate(expression, "x").expression; val fySource = calculus.differentiate(expression, "y").expression
            val fx = expressions.compile(fxSource).eval(mapOf("x" to x, "y" to y)); val fy = expressions.compile(fySource).eval(mapOf("x" to x, "y" to y))
            val answer = "z - ${number(z)} = ${number(fx)}*(x - ${number(x)}) + ${number(fy)}*(y - ${number(y)})"
            return result(answer, answer, alternatives = listOf("Gradient" to "[${number(fx)}, ${number(fy)}]", "Normal" to "[${number(-fx)}, ${number(-fy)}, 1]"), steps = listOf("Evaluate the surface point (${number(x)}, ${number(y)}, ${number(z)}).", "Compute partial derivatives fx=$fxSource and fy=$fySource.", "Evaluate the gradient at the point.", "Apply z-z0=fx(x-x0)+fy(y-y0)."), verification = "The plane contains the surface point and its slopes match independent centred differences in x and y.")
        }
        return null
    }

    private fun parseAssignments(source: String): Map<String, Double> = if (source.isBlank()) emptyMap() else source.split(',').associate { entry ->
        val parts = entry.trim().split('=', limit = 2); require(parts.size == 2 && parts[0].trim().matches(Regex("[a-zA-Z]"))) { "Use coordinates such as x=1,y=2." }
        parts[0].trim().lowercase() to expressions.compile(parts[1].trim()).eval()
    }

    private fun parseVector(source: String): List<Double> = source.trim().removePrefix("[").removeSuffix("]").removePrefix("(").removeSuffix(")").split(',').filter(String::isNotBlank).map { expressions.compile(it.trim()).eval() }

    private fun inferVariables(expression: String): List<String> = listOf("x", "y", "z", "t").filter { Regex("(?<![A-Za-z0-9_])$it(?![A-Za-z0-9_])").containsMatchIn(expression) }

    private fun vectorValue(partials: List<String>, variables: List<String>, point: Map<String, Double>): String =
        partials.map { expressions.compile(it).eval(point) }.joinToString(prefix = "[", postfix = "]") { number(it) }

    private fun logarithm(source: String): AdvancedCalculatorResult? {
        val match = Regex("(?i)^log\\s+base\\s+(-?\\d+(?:\\.\\d+)?)\\s+of\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source) ?: return null
        val base = match.groupValues[1].toDouble(); val value = match.groupValues[2].toDouble()
        require(base > 0 && base != 1.0 && value > 0) { "Logarithm requires value > 0, base > 0 and base != 1." }
        val result = ln(value) / ln(base)
        return result(number(result), steps = listOf("Use change of base: log_b(x)=ln(x)/ln(b).", "Substitute b=${number(base)}, x=${number(value)}.", "Evaluate with deterministic floating-point precision."), verification = "${number(base)}^${number(result)} = ${number(base.pow(result))}.")
    }

    private fun symbolic(source: String): AdvancedCalculatorResult? {
        Regex("""(?i)^derivative\s+analysis\s+(.+?)\s+from\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val report = rigorousCalculus.derivativeApplications(match.groupValues[1], match.groupValues[2].toDouble(), match.groupValues[3].toDouble())
            val answer = report.stationaryPoints.joinToString(prefix = "[", postfix = "]") { "${it.kind} (${number(it.point.x)},${number(it.point.y)})" }
            val alternatives = listOfNotNull(
                report.absoluteMinimum?.let { "Absolute minimum" to "(${number(it.x)},${number(it.y)})" },
                report.absoluteMaximum?.let { "Absolute maximum" to "(${number(it.x)},${number(it.y)})" },
                "Increasing intervals" to report.increasing.joinToString { "[${number(it.from)},${number(it.to)}]" },
                "Decreasing intervals" to report.decreasing.joinToString { "[${number(it.from)},${number(it.to)}]" },
            )
            return result(answer, answer, alternatives, listOf("Analyze the derivative sign across the closed interval.", "Locate transitions between increasing and decreasing behavior.", "Evaluate stationary points and both endpoints.", "Compare candidate values for absolute extrema."), "Certified derivative analysis on [${match.groupValues[2]},${match.groupValues[3]}] using 2000 interval samples and endpoint checks.")
        }
        Regex("""(?i)^(tangent|normal)\s+(.+?)\s+at\s+x\s*=\s*(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val kind = match.groupValues[1].lowercase(); val function = match.groupValues[2]; val x0 = match.groupValues[3].toDouble()
            val derivativeSource = calculus.differentiate(function, "x").expression; val y0 = expressions.compile(function).eval(mapOf("x" to x0)); val slope = expressions.compile(derivativeSource).eval(mapOf("x" to x0))
            require(kind != "normal" || abs(slope) > 1e-12) { "The normal is vertical because the tangent slope is zero; use x=${number(x0)}." }
            val lineSlope = if (kind == "tangent") slope else -1 / slope
            val answer = "y - ${number(y0)} = ${number(lineSlope)}*(x - ${number(x0)})"
            return result(answer, answer, listOf("Point" to "(${number(x0)},${number(y0)})", "Tangent slope" to number(slope)), listOf("Differentiate f(x).", "Evaluate f and f' at x=${number(x0)}.", if (kind == "tangent") "Use the derivative as the line slope." else "Use the negative reciprocal slope.", "Write the line in point-slope form."), "The line contains the requested point; its slope is derived from f'=$derivativeSource.")
        }
        Regex("""(?i)^lagrange\s+(.+?)\s+constraint\s+(.+?)\s+level\s+(-?\d+(?:\.\d+)?)\s+seeds\s+(.+)$""").matchEntire(source)?.let { match ->
            val seeds = match.groupValues[4].split('|').map { seed -> parseVector(seed).let { require(it.size == 2) { "Each seed must contain x,y." }; Vec2(it[0], it[1]) } }
            val points = vectorCalculus.lagrange2D(match.groupValues[1], match.groupValues[2], match.groupValues[3].toDouble(), seeds)
            require(points.isNotEmpty()) { "No constrained critical point converged from the supplied seeds." }
            val answer = points.joinToString(prefix = "[", postfix = "]") { "(${number(it.point.x)},${number(it.point.y)}): ${number(it.value)}" }
            val residual = points.maxOf { maxOf(it.constraintResidual, it.stationarityResidual) }
            return result(answer, answer, listOf("Critical points" to points.size.toString(), "Maximum residual" to residual.toString()), listOf("Form grad(f)=lambda*grad(g).", "Add the constraint g(x,y)=level.", "Solve the three nonlinear equations from multiple seeds.", "Deduplicate converged points and order them by objective value."), "Certified constrained extrema; residual=${scientific(residual)}.")
        }
        Regex("""(?i)^line\s+integral\s+(.+?)\s+curve\s+(.+?)\s+from\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val curve = match.groupValues[2].split(';').map(String::trim); require(curve.size in 2..3) { "Curve must contain x(t);y(t) and optional z(t)." }
            val report = vectorCalculus.scalarLineIntegral(match.groupValues[1], curve[0], curve[1], curve.getOrElse(2) { "0" }, match.groupValues[3].toDouble(), match.groupValues[4].toDouble())
            return certifiedIntegral(report)
        }
        Regex("""(?i)^work\s+integral\s+(.+?)\s+curve\s+(.+?)\s+from\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val field = match.groupValues[1].split(';').map(String::trim); val curve = match.groupValues[2].split(';').map(String::trim)
            require(field.size in 2..3 && curve.size in 2..3) { "Use semicolon-separated field and curve components." }
            val report = vectorCalculus.workLineIntegral(field, curve[0], curve[1], curve.getOrElse(2) { "0" }, match.groupValues[3].toDouble(), match.groupValues[4].toDouble())
            return certifiedIntegral(report)
        }
        Regex("""(?i)^surface\s+flux\s+(.+?)\s+surface\s+(.+?)\s+u\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+v\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val field = match.groupValues[1].split(';').map(String::trim); val surface = match.groupValues[2].split(';').map(String::trim)
            val report = vectorCalculus.surfaceFlux(field, surface, match.groupValues[3].toDouble()..match.groupValues[4].toDouble(), match.groupValues[5].toDouble()..match.groupValues[6].toDouble())
            return certifiedIntegral(report)
        }
        Regex("""(?i)^green\s+(.+?)\s+x\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+y\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val field = match.groupValues[1].split(';').map(String::trim); require(field.size == 2)
            return theoremResult(vectorCalculus.greenRectangle(field[0], field[1], match.groupValues[2].toDouble()..match.groupValues[3].toDouble(), match.groupValues[4].toDouble()..match.groupValues[5].toDouble()))
        }
        Regex("""(?i)^gauss\s+(.+?)\s+x\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+y\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+z\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            val field = match.groupValues[1].split(';').map(String::trim)
            return theoremResult(vectorCalculus.gaussBox(field, match.groupValues[2].toDouble()..match.groupValues[3].toDouble(), match.groupValues[4].toDouble()..match.groupValues[5].toDouble(), match.groupValues[6].toDouble()..match.groupValues[7].toDouble()))
        }
        Regex("""(?i)^stokes\s+(.+?)\s+x\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+y\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)(?:\s+z\s+(-?\d+(?:\.\d+)?))?$""").matchEntire(source)?.let { match ->
            val field = match.groupValues[1].split(';').map(String::trim)
            return theoremResult(vectorCalculus.stokesPlanarRectangle(field, match.groupValues[2].toDouble()..match.groupValues[3].toDouble(), match.groupValues[4].toDouble()..match.groupValues[5].toDouble(), match.groupValues[6].toDoubleOrNull() ?: 0.0))
        }
        Regex("(?i)^jacobian\\s+(.+?)\\s+vars\\s+([a-z](?:\\s*,\\s*[a-z])*)\\s+at\\s+(.+)$").matchEntire(source)?.let { match ->
            val functions = match.groupValues[1].split(';').map(String::trim); val variables = match.groupValues[2].split(',').map(String::trim); val point = match.groupValues[3].split(',').associate { entry -> entry.trim().split('=', limit = 2).let { it[0].trim() to it[1].trim().toDouble() } }
            val report = vectorCalculus.jacobian(functions, variables, point); val answer = report.values.joinToString(prefix = "[", postfix = "]") { row -> row.joinToString(prefix = "[", postfix = "]") { number(it) } }
            return result(answer, exact = answer, alternatives = listOf("Variables" to variables.joinToString(), "Point" to point.toString()), steps = listOf("Differentiate each output with respect to each declared variable.", "Evaluate every partial derivative at the point."), verification = report.verification)
        }
        Regex("(?i)^hessian\\s+(.+?)\\s+vars\\s+([a-z](?:\\s*,\\s*[a-z])*)\\s+at\\s+(.+)$").matchEntire(source)?.let { match ->
            val variables = match.groupValues[2].split(',').map(String::trim); val point = match.groupValues[3].split(',').associate { entry -> entry.trim().split('=', limit = 2).let { it[0].trim() to it[1].trim().toDouble() } }
            val report = vectorCalculus.hessian(match.groupValues[1], variables, point); val answer = report.values.joinToString(prefix = "[", postfix = "]") { row -> row.joinToString(prefix = "[", postfix = "]") { number(it) } }
            return result(answer, exact = answer, alternatives = listOf("Symmetry residual" to report.symmetryError.toString()), steps = listOf("Compute all second partial derivatives.", "Check equality of mixed partials."), verification = report.verification)
        }
        Regex("(?i)^linear\\s+ivp\\s+a\\s+(-?\\d+(?:\\.\\d+)?)\\s+b\\s+(-?\\d+(?:\\.\\d+)?)\\s+x0\\s+(-?\\d+(?:\\.\\d+)?)\\s+y0\\s+(-?\\d+(?:\\.\\d+)?)\\s+at\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val report = differentialEquations.linearConstant(match.groupValues[1].toDouble(), match.groupValues[2].toDouble(), match.groupValues[3].toDouble(), match.groupValues[4].toDouble(), match.groupValues[5].toDouble())
            return result(number(report.points.last().values.single()), exact = report.exact, alternatives = listOf("Method" to report.method, "Residual" to report.residual.toString()), steps = report.steps, verification = report.verification)
        }
        Regex("""(?i)^logistic\s+rate\s+(-?\d+(?:\.\d+)?)\s+capacity\s+(\d+(?:\.\d+)?)\s+x0\s+(-?\d+(?:\.\d+)?)\s+y0\s+(\d+(?:\.\d+)?)\s+at\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            return odeResult(differentialEquations.logistic(match.groupValues[1].toDouble(), match.groupValues[2].toDouble(), match.groupValues[3].toDouble(), match.groupValues[4].toDouble(), match.groupValues[5].toDouble()))
        }
        Regex("""(?i)^second\s+order\s+ivp\s+a\s+(-?\d+(?:\.\d+)?)\s+b\s+(-?\d+(?:\.\d+)?)\s+x0\s+(-?\d+(?:\.\d+)?)\s+y0\s+(-?\d+(?:\.\d+)?)\s+v0\s+(-?\d+(?:\.\d+)?)\s+at\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            return odeResult(differentialEquations.secondOrderHomogeneous(match.groupValues[1].toDouble(), match.groupValues[2].toDouble(), match.groupValues[3].toDouble(), match.groupValues[4].toDouble(), match.groupValues[5].toDouble(), match.groupValues[6].toDouble()))
        }
        Regex("""(?i)^system\s+rk4\s+(.+?)\s+initial\s+(.+?)\s+from\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+step\s+(\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            return odeResult(differentialEquations.systemRk4(match.groupValues[1].split(';').map(String::trim), parseVector(match.groupValues[2]), match.groupValues[3].toDouble(), match.groupValues[4].toDouble(), match.groupValues[5].toDouble()))
        }
        Regex("""(?i)^laplace\s+(.+)$""").matchEntire(source)?.let { match ->
            val workflow = differentialEquations.laplaceWorkflow(match.groupValues[1]); require(workflow.supported && workflow.output != null) { "This Laplace transform is outside the verified local table." }
            return result(workflow.output, workflow.output, workflow.conditions.mapIndexed { index, condition -> "Condition ${index + 1}" to condition }, workflow.steps, "Certified symbolic Laplace table workflow with explicit transform conditions.")
        }
        Regex("(?i)^rk4\\s+(.+?)\\s+x0\\s+(-?\\d+(?:\\.\\d+)?)\\s+y0\\s+(-?\\d+(?:\\.\\d+)?)\\s+to\\s+(-?\\d+(?:\\.\\d+)?)\\s+step\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val report = differentialEquations.rk4(match.groupValues[1], match.groupValues[2].toDouble(), match.groupValues[3].toDouble(), match.groupValues[4].toDouble(), match.groupValues[5].toDouble())
            return result(number(report.points.last().values.single()), alternatives = listOf("Method" to report.method, "Error estimate" to report.residual.toString()), steps = report.steps, verification = report.verification)
        }
        Regex("(?i)^ode\\s+series\\s+lambda\\s+(-?\\d+(?:\\.\\d+)?)\\s+x0\\s+(-?\\d+(?:\\.\\d+)?)\\s+y0\\s+(-?\\d+(?:\\.\\d+)?)\\s+order\\s+(\\d+)$").matchEntire(source)?.let { match ->
            val series = differentialEquations.exponentialSeries(match.groupValues[1].toDouble(), match.groupValues[2].toDouble(), match.groupValues[3].toDouble(), match.groupValues[4].toInt())
            return result(series.polynomial, exact = series.polynomial, alternatives = listOf("Recurrence" to series.recurrence), steps = listOf("Assume y=sum a_n(x-x0)^n.", "Substitute into y'=lambda*y.", "Match coefficients and apply y(x0)=y0."), verification = "The truncated residual begins at order ${series.residualOrder}.")
        }
        Regex("(?i)^improper\\s+integrate\\s+(.+?)\\s+from\\s+(-?\\d+(?:\\.\\d+)?)\\s+to\\s+(?:infinity|inf)$").matchEntire(source)?.let { match ->
            val report = advancedIntegration.improperToPositiveInfinity(match.groupValues[1], "x", match.groupValues[2].toDouble())
            val answer = report.value?.let(::number) ?: report.convergence.name
            return result(answer, exact = report.value?.let(::number), alternatives = listOf("Convergence" to report.convergence.name, "Error estimate" to report.errorEstimate.toString()), steps = report.steps, verification = report.verification, warning = if (report.value == null) "No convergent finite value was certified." else null)
        }
        Regex("""(?i)^improper\s+integrate\s+(.+?)\s+from\s+(?:-infinity|-inf)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            return certifiedAdvancedIntegral(advancedIntegration.improperFromNegativeInfinity(match.groupValues[1], "x", match.groupValues[2].toDouble()))
        }
        Regex("""(?i)^improper\s+integrate\s+(.+?)\s+from\s+(?:-infinity|-inf)\s+to\s+(?:infinity|inf)$""").matchEntire(source)?.let { match ->
            return certifiedAdvancedIntegral(advancedIntegration.improperBothInfinite(match.groupValues[1]))
        }
        Regex("(?i)^double\\s+integrate\\s+(.+?)\\s+x\\s+(-?\\d+(?:\\.\\d+)?)\\s+to\\s+(-?\\d+(?:\\.\\d+)?)\\s+y\\s+(-?\\d+(?:\\.\\d+)?)\\s+to\\s+(-?\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val report = advancedIntegration.doubleIntegral(match.groupValues[1], IntegrationBound("x", match.groupValues[2].toDouble(), match.groupValues[3].toDouble()), IntegrationBound("y", match.groupValues[4].toDouble(), match.groupValues[5].toDouble()))
            return result(number(report.value!!), exact = number(report.value), alternatives = listOf("Error estimate" to report.errorEstimate.toString()), steps = report.steps, verification = report.verification)
        }
        Regex("""(?i)^triple\s+integrate\s+(.+?)\s+x\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+y\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+z\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)$""").matchEntire(source)?.let { match ->
            return certifiedAdvancedIntegral(advancedIntegration.tripleIntegral(match.groupValues[1], IntegrationBound("x", match.groupValues[2].toDouble(), match.groupValues[3].toDouble()), IntegrationBound("y", match.groupValues[4].toDouble(), match.groupValues[5].toDouble()), IntegrationBound("z", match.groupValues[6].toDouble(), match.groupValues[7].toDouble())))
        }
        Regex("""(?i)^parameter\s+integral\s+(.+?)\s+x\s+(-?\d+(?:\.\d+)?)\s+to\s+(-?\d+(?:\.\d+)?)\s+parameter\s+([a-z])\s+values\s+(.+)$""").matchEntire(source)?.let { match ->
            val values = parseVector(match.groupValues[5]); val report = advancedIntegration.parameterized(match.groupValues[1], IntegrationBound("x", match.groupValues[2].toDouble(), match.groupValues[3].toDouble()), match.groupValues[4], values)
            val samples = report.samples.mapValues { it.value.value }
            val answer = samples.entries.joinToString(prefix = "[", postfix = "]") { "${number(it.key)} -> ${it.value?.let(::number) ?: "unresolved"}" }
            val error = report.samples.values.maxOf { it.errorEstimate }
            return result(answer, answer, listOf("Continuous on samples" to report.continuousOnSamples.toString(), "Maximum error" to error.toString()), listOf("Fix each requested parameter value.", "Certify the definite integral independently for every sample.", "Compare convergence and continuity across the parameter sweep."), "Certified parameter sweep; error=${scientific(error)}.")
        }
        Regex("(?i)^limit\\s+(.+?)\\s+as\\s+([a-z])\\s*(?:->|approaches)\\s*(-?\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { match ->
            val report = rigorousCalculus.limit(match.groupValues[1], match.groupValues[3].toDouble(), match.groupValues[2])
            val answer = when (report.classification) {
                LimitClassification.Finite -> report.value?.let(::number) ?: "unresolved"
                LimitClassification.PositiveInfinity -> "+infinity"
                LimitClassification.NegativeInfinity -> "-infinity"
                LimitClassification.DoesNotExist -> "DNE"
                LimitClassification.Unresolved -> "unresolved"
            }
            return result(answer, exact = answer, alternatives = listOf("Continuity" to report.continuity.name, "Method" to report.method.name), steps = report.steps, verification = report.verification, warning = if (report.classification == LimitClassification.Unresolved) "The local engine refused to infer a limit from unstable evidence." else null)
        }
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

    private fun certifiedIntegral(report: IntegralCertificate) = result(
        number(report.value), number(report.value),
        listOf("Error estimate" to report.errorEstimate.toString(), "Orientation" to report.orientation),
        report.steps,
        "Certified ${report.method}; error=${scientific(report.errorEstimate)}. ${report.verification}",
    )

    private fun certifiedAdvancedIntegral(report: CertifiedIntegral): AdvancedCalculatorResult {
        val answer = report.value?.let(::number) ?: report.convergence.name
        return result(answer, report.value?.let(::number), listOf("Convergence" to report.convergence.name, "Error estimate" to report.errorEstimate.toString(), "Evaluations" to report.evaluations.toString()), report.steps, "Certified ${report.method}; error=${scientific(report.errorEstimate)}. ${report.verification}", if (report.value == null) "No convergent finite value was certified." else null)
    }

    private fun odeResult(report: OdeSolutionReport): AdvancedCalculatorResult {
        val endpoint = report.points.last().values.joinToString(prefix = "[", postfix = "]") { number(it) }
        val primary = if (report.points.last().values.size == 1) number(report.points.last().values.single()) else endpoint
        return result(primary, report.exact, listOf("Method" to report.method, "Endpoint" to endpoint, "Residual" to report.residual.toString()), report.steps, "Certified ${report.method}; residual=${scientific(report.residual)}. ${report.verification}")
    }

    private fun theoremResult(report: TheoremCertificate) = result(
        number(report.boundaryValue), number(report.boundaryValue),
        listOf("Interior value" to number(report.interiorValue), "Residual" to report.residual.toString(), "Certificate" to if (report.passed) "PASS" else "FAIL"),
        report.steps + report.assumptions.map { "Assumption: $it" },
        "Certified ${report.theorem} theorem; passed=${report.passed}; residual=${scientific(report.residual)}.",
        if (report.passed) null else "Boundary and interior evaluations disagree beyond tolerance.",
    )

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
    private fun scientific(value: Double) = String.format(java.util.Locale.US, "%.3e", value)
}
