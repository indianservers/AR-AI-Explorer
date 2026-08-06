package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round

data class InteractiveParameter(
    val name: String,
    val value: Double,
    val minimum: Double,
    val maximum: Double,
    val step: Double,
    val description: String,
) {
    fun snap(candidate: Double): Double {
        val bounded = candidate.coerceIn(minimum, maximum)
        val snapped = round(bounded / step) * step
        return if (abs(snapped) < 1e-10) 0.0 else snapped.coerceIn(minimum, maximum)
    }
}

/**
 * Shared parameter discovery for graph workspaces. Independent coordinates,
 * constants, and function names are excluded; every remaining identifier gets
 * a deterministic slider policy.
 */
object InteractiveParameterEngine {
    fun discover(
        expressions: Collection<String>,
        values: Map<String, Double> = emptyMap(),
        independentVariables: Set<String> = setOf("x", "y", "z", "t", "r", "theta"),
    ): List<InteractiveParameter> {
        val reserved = reservedIdentifiers + independentVariables.map(String::lowercase)
        return expressions
            .flatMap { source -> identifiers(source) }
            .filterNot { it.lowercase() in reserved }
            .distinctBy(String::lowercase)
            .sortedBy(String::lowercase)
            .map { name -> policy(name, values[name]) }
    }

    fun resolve(
        expression: String,
        values: Map<String, Double>,
        independentVariables: Set<String> = setOf("x", "y", "z", "t", "r", "theta"),
    ): String {
        val excluded = reservedIdentifiers + independentVariables.map(String::lowercase)
        var resolved = expression
        values.entries.sortedByDescending { it.key.length }.forEach { (name, value) ->
            if (name.lowercase() !in excluded) {
                resolved = resolved.replace(
                    Regex("""\b${Regex.escape(name)}\b""", RegexOption.IGNORE_CASE),
                    "(${trim(value)})",
                )
            }
        }
        return resolved
    }

    fun values(parameters: Collection<InteractiveParameter>): Map<String, Double> =
        parameters.associate { it.name to it.value }

    private fun identifiers(expression: String): List<String> =
        Regex("""[A-Za-z][A-Za-z0-9_]*""").findAll(expression).map { it.value }.toList()

    private fun policy(name: String, requestedValue: Double?): InteractiveParameter {
        val lower = name.lowercase()
        val (default, minimum, maximum, step, description) = when (lower) {
            "a" -> ParameterPolicy(1.0, -10.0, 10.0, .1, "leading coefficient")
            "b" -> ParameterPolicy(0.0, -10.0, 10.0, .1, "linear or horizontal parameter")
            "c" -> ParameterPolicy(0.0, -10.0, 10.0, .1, "constant or vertical parameter")
            "p", "q", "probability" -> ParameterPolicy(.5, 0.0, 1.0, .01, "probability")
            "n", "k", "count" -> ParameterPolicy(1.0, 1.0, 20.0, 1.0, "integer count")
            "angle", "alpha", "beta", "phi" -> ParameterPolicy(0.0, -PI, PI, .05, "angle in radians")
            "amplitude" -> ParameterPolicy(1.0, 0.0, 10.0, .1, "amplitude")
            "frequency" -> ParameterPolicy(1.0, .1, 10.0, .1, "frequency")
            else -> ParameterPolicy(1.0, -10.0, 10.0, .1, "interactive parameter")
        }
        val value = (requestedValue ?: default).coerceIn(minimum, maximum)
        return InteractiveParameter(name, value, minimum, maximum, step, description)
    }

    private data class ParameterPolicy(
        val default: Double,
        val minimum: Double,
        val maximum: Double,
        val step: Double,
        val description: String,
    )

    private val reservedIdentifiers = setOf(
        "pi", "e", "tau", "inf", "infinity",
        "sin", "cos", "tan", "sec", "csc", "cot",
        "sinh", "cosh", "tanh", "asin", "acos", "atan",
        "sqrt", "cbrt", "abs", "exp", "ln", "log", "floor", "ceil", "round",
        "sign", "min", "max", "if", "else", "mod",
    )
}
