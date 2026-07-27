package com.indianservers.aiexplorer.core

data class SurfaceInputInterpretation(
    val canonicalEquation: String,
    val expression: String,
)

/**
 * Converts common user-written explicit surface forms into the canonical z = f(x, y) form.
 * Evaluation remains the responsibility of the graph engine so parser errors stay authoritative.
 */
object SurfaceInputInterpreter {
    fun explicit(rawInput: String): Result<SurfaceInputInterpretation> = runCatching {
        val normalized = rawInput
            .trim()
            .replace('−', '-')
            .replace('×', '*')
            .replace("²", "^2")
            .replace("³", "^3")
            .replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) { "Enter a surface equation, for example z = x^2 + y^2." }

        val equals = normalized.indexOf('=')
        val expression = if (equals < 0) {
            normalized
        } else {
            require(normalized.indexOf('=', equals + 1) < 0) { "Use one equation at a time." }
            val left = normalized.substring(0, equals).trim()
            val right = normalized.substring(equals + 1).trim()
            require(left.isNotBlank() && right.isNotBlank()) { "Both sides of the equation are required." }
            when {
                left.equals("z", ignoreCase = true) || left.matches(Regex("(?i)f\\s*\\(\\s*x\\s*,\\s*y\\s*\\)")) -> right
                right.equals("z", ignoreCase = true) -> left
                else -> error("Enter an explicit surface such as z = f(x, y).")
            }
        }.trim()

        require(expression.isNotBlank()) { "Enter the expression for z." }
        SurfaceInputInterpretation(
            canonicalEquation = "z = $expression",
            expression = expression,
        )
    }
}
