package com.indianservers.aiexplorer.core

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

enum class AngleMode(val label: String) { Degrees("DEG"), Radians("RAD"), Gradians("GRAD") }

data class CalculatorConstant(
    val key: String,
    val label: String,
    val value: Double,
    val unit: String,
    val note: String,
)

data class CalculatorReferenceCard(
    val title: String,
    val expression: String,
    val description: String,
    val examples: List<String>,
)

data class UnitConversion(
    val title: String,
    val fromUnit: String,
    val toUnit: String,
    val factor: Double,
    val example: String,
)

data class ScientificCalculatorResult(
    val input: String,
    val normalizedExpression: String,
    val value: Double,
    val decimal: String,
    val scientific: String,
    val engineering: String,
    val exactHint: String?,
    val steps: List<String>,
    val warnings: List<String> = emptyList(),
)

class ScientificCalculator(private val expressionEngine: ExpressionEngine = ExpressionEngine()) {
    val constants = listOf(
        CalculatorConstant("pi", "π", PI, "", "Circle constant; circumference / diameter."),
        CalculatorConstant("e", "e", E, "", "Natural exponential base."),
        CalculatorConstant("c", "c", 299_792_458.0, "m/s", "Speed of light in vacuum."),
        CalculatorConstant("g", "g", 9.80665, "m/s²", "Standard gravity."),
        CalculatorConstant("h", "h", 6.62607015e-34, "J·s", "Planck constant."),
        CalculatorConstant("k", "k", 1.380649e-23, "J/K", "Boltzmann constant."),
    )

    val referenceCards = listOf(
        CalculatorReferenceCard("Arithmetic", "a+b, a-b, a*b, a/b", "Core precedence-aware arithmetic with implicit multiplication.", listOf("2(3+4)", "12/5+7")),
        CalculatorReferenceCard("Powers & roots", "x^n, sqrt(x)", "Powers, square roots and fractional exponents.", listOf("2^10", "sqrt(81)", "27^(1/3)")),
        CalculatorReferenceCard("Trigonometry", "sin(x), cos(x), tan(x)", "Uses selected DEG/RAD angle mode.", listOf("sin(30)", "cos(pi/3)", "tan(45)")),
        CalculatorReferenceCard("Inverse trig", "asin(x), acos(x), atan(x)", "Returns answers in the selected angle mode.", listOf("asin(0.5)", "atan(1)")),
        CalculatorReferenceCard("Logs", "ln(x), log(x), exp(x)", "Natural log, base-10 log and exponential.", listOf("ln(e^2)", "log(1000)", "exp(1)")),
        CalculatorReferenceCard("Percent", "x%", "Percent shorthand is treated as division by 100.", listOf("15%*200", "250+18%*250")),
        CalculatorReferenceCard("Factorial", "n!", "Exact integer factorials for non-negative whole numbers.", listOf("5!", "7!/(5!*2!)")),
        CalculatorReferenceCard("Min/Max", "min(a,b), max(a,b)", "Compare values directly in expressions.", listOf("max(12, 7^2)", "min(3.1, pi)")),
        CalculatorReferenceCard("Rounding helpers", "floor(x), ceil(x)", "Integer-boundary helper functions.", listOf("floor(3.9)", "ceil(2.1)")),
        CalculatorReferenceCard("Scientific constants", "pi, e, c, g, h, k", "Useful constants can be inserted as symbols or names.", listOf("c/1000000", "g*12", "h*5")),
    )

    val conversions = listOf(
        UnitConversion("Length", "cm", "m", 0.01, "250 cm = 2.5 m"),
        UnitConversion("Length", "km", "m", 1000.0, "3.2 km = 3200 m"),
        UnitConversion("Area", "acre", "m²", 4046.8564224, "1 acre ≈ 4046.856 m²"),
        UnitConversion("Mass", "kg", "g", 1000.0, "1.75 kg = 1750 g"),
        UnitConversion("Speed", "km/h", "m/s", 1.0 / 3.6, "72 km/h = 20 m/s"),
        UnitConversion("Energy", "kWh", "J", 3_600_000.0, "1 kWh = 3.6 MJ"),
        UnitConversion("Angle", "degree", "radian", PI / 180.0, "180° = π rad"),
        UnitConversion("Pressure", "atm", "Pa", 101_325.0, "1 atm = 101325 Pa"),
    )

    fun evaluate(source: String, angleMode: AngleMode = AngleMode.Degrees, precision: Int = 8): ScientificCalculatorResult {
        val cleaned = source.trim()
        require(cleaned.isNotEmpty()) { "Enter an expression to calculate." }
        val normalized = normalize(cleaned, angleMode)
        val value = expressionEngine.compile(normalized).eval()
        val warnings = buildList {
            if (!value.isFinite()) add("Result is not finite; check domain or division by zero.")
            if (angleMode == AngleMode.Degrees && cleaned.contains(Regex("\\b(sin|cos|tan|sec|csc|cot)\\s*\\(", RegexOption.IGNORE_CASE))) {
                add("Trig inputs interpreted as degrees.")
            }
        }
        return ScientificCalculatorResult(
            input = source,
            normalizedExpression = normalized,
            value = value,
            decimal = formatDecimal(value, precision),
            scientific = formatScientific(value, precision),
            engineering = formatEngineering(value, precision),
            exactHint = exactHint(cleaned, value),
            steps = listOf(
                "Normalize symbols, constants, percentages, factorials and angle mode.",
                "Evaluate using standard precedence: brackets, powers, unary signs, multiplication/division, addition/subtraction.",
                "Return decimal, scientific and engineering notation for the same value.",
            ),
            warnings = warnings,
        )
    }

    fun convert(value: Double, conversion: UnitConversion): Double = value * conversion.factor

    private fun normalize(source: String, angleMode: AngleMode): String {
        var expression = source
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "pi")
            .replace("√", "sqrt")
        expression = expression.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*°")) { "(${it.groupValues[1]}*pi/180)" }
        expression = expression.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*%")) { "(${it.groupValues[1]}/100)" }
        expression = replaceConstants(expression)
        expression = replaceFactorials(expression)
        return when (angleMode) {
            AngleMode.Degrees -> rewriteAngleFunctions(expression, "pi/180", "180/pi")
            AngleMode.Gradians -> rewriteAngleFunctions(expression, "pi/200", "200/pi")
            AngleMode.Radians -> expression
        }
    }

    private fun replaceConstants(source: String): String {
        var expression = source
        constants.filter { it.key !in setOf("pi", "e") }.forEach { constant ->
            expression = expression.replace(Regex("\\b${Regex.escape(constant.key)}\\b"), "(${constantLiteral(constant.value)})")
        }
        return expression
    }

    private fun constantLiteral(value: Double): String = when {
        abs(value) < 1e-6 -> {
            val exponent = floor(log10(abs(value))).toInt()
            val mantissa = value / 10.0.pow(exponent)
            "${trimToPrecision(mantissa, 12)}*10^$exponent"
        }
        abs(value) >= 1e9 -> {
            val exponent = floor(log10(abs(value))).toInt()
            val mantissa = value / 10.0.pow(exponent)
            "${trimToPrecision(mantissa, 12)}*10^$exponent"
        }
        else -> trimToPrecision(value, 12)
    }

    private fun replaceFactorials(source: String): String {
        var expression = source
        val simple = Regex("(\\d+)!")
        while (simple.containsMatchIn(expression)) {
            expression = simple.replace(expression) { match ->
                factorial(match.groupValues[1].toInt()).toString()
            }
        }
        return expression
    }

    private fun rewriteAngleFunctions(source: String, forwardScale: String, inverseScale: String): String {
        val circular = setOf("sin", "cos", "tan", "sec", "csc", "cot")
        val inverse = setOf("asin", "acos", "atan")
        var index = 0
        val output = StringBuilder()
        while (index < source.length) {
            val match = Regex("[A-Za-z]+").find(source, index)
            if (match == null) {
                output.append(source.substring(index))
                break
            }
            output.append(source.substring(index, match.range.first))
            val name = match.value
            val lower = name.lowercase()
            var cursor = match.range.last + 1
            while (cursor < source.length && source[cursor].isWhitespace()) cursor++
            if ((lower in circular || lower in inverse) && source.getOrNull(cursor) == '(') {
                val close = closingParen(source, cursor)
                if (close != null) {
                    val inner = rewriteAngleFunctions(source.substring(cursor + 1, close), forwardScale, inverseScale)
                    if (lower in circular) output.append("$name(($inner)*$forwardScale)")
                    else output.append("($name($inner)*$inverseScale)")
                    index = close + 1
                } else {
                    output.append(name)
                    index = cursor
                }
            } else {
                output.append(name)
                index = cursor
            }
        }
        return output.toString()
    }

    private fun closingParen(source: String, open: Int): Int? {
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun factorial(n: Int): Long {
        require(n in 0..20) { "Factorial supports whole numbers from 0 to 20." }
        var result = 1L
        for (i in 2..n) result *= i
        return result
    }

    private fun exactHint(input: String, value: Double): String? {
        if (abs(value - round(value)) < 1e-10) return round(value).toLong().toString()
        if (input.contains("pi", true) || input.contains("π")) {
            val ratio = value / PI
            if (abs(ratio - round(ratio)) < 1e-10) return "${round(ratio).toLong()}π"
            if (abs(ratio - 0.5) < 1e-10) return "π/2"
        }
        return null
    }

    private fun formatDecimal(value: Double, precision: Int): String = trimToPrecision(value, precision)

    private fun formatScientific(value: Double, precision: Int): String = "%.${precision.coerceIn(1, 12)}e".format(value)

    private fun formatEngineering(value: Double, precision: Int): String {
        if (value == 0.0 || !value.isFinite()) return formatDecimal(value, precision)
        val exponent = floor(log10(abs(value)) / 3.0).toInt() * 3
        val mantissa = value / 10.0.pow(exponent)
        return "${trimToPrecision(mantissa, precision)}e$exponent"
    }

    private fun trimToPrecision(value: Double, precision: Int): String {
        if (!value.isFinite()) return value.toString()
        val rounded = "%.${precision.coerceIn(0, 12)}f".format(value)
        return rounded.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }
}
