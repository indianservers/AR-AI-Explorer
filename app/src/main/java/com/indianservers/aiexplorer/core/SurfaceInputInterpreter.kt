package com.indianservers.aiexplorer.core

data class SurfaceInputInterpretation(
    val canonicalEquation: String,
    val expression: String,
    val kind: SpatialSurfaceKind = SpatialSurfaceKind.Explicit,
    val expressionY: String = "",
    val expressionZ: String = "",
)

/**
 * Converts common user-written explicit surface forms into canonical z = f(x, y).
 * Evaluation remains the graph engine's responsibility so parser errors stay
 * authoritative and all native-keyboard symbols share one normalization path.
 */
object SurfaceInputInterpreter {
    /** Accepts explicit z=f(x,y), implicit F(x,y,z)=0, or x=...; y=...; z=... parametric input. */
    fun interpret(rawInput: String): Result<SurfaceInputInterpretation> = runCatching {
        val normalized = MathExpressionNormalizer.normalize(rawInput).trim()
        require(normalized.isNotBlank()) { "Enter an explicit, implicit, or parametric surface." }
        if (';' in normalized) {
            val parts = normalized.split(';').map(String::trim).filter(String::isNotBlank)
            val coordinates = parts.associate { part ->
                val pair = part.split('=', limit = 2)
                require(pair.size == 2 && pair[0].trim().lowercase() in setOf("x", "y", "z")) {
                    "Parametric form uses x=...; y=...; z=... with parameters u and v."
                }
                pair[0].trim().lowercase() to pair[1].trim().also { require(it.isNotBlank()) }
            }
            require(coordinates.keys == setOf("x", "y", "z")) { "Parametric form requires x, y, and z components." }
            SurfaceInputInterpretation(
                canonicalEquation = "x=${coordinates.getValue("x")}; y=${coordinates.getValue("y")}; z=${coordinates.getValue("z")}",
                expression = coordinates.getValue("x"), kind = SpatialSurfaceKind.Parametric,
                expressionY = coordinates.getValue("y"), expressionZ = coordinates.getValue("z"),
            )
        } else {
            explicit(normalized).getOrElse {
                require('=' in normalized) { "Implicit surfaces use an equation such as x^2+y^2+z^2=4." }
                val sides = normalized.split('=', limit = 2)
                require(sides.all { it.isNotBlank() }) { "Both sides of the implicit equation are required." }
                require(normalized.any { it.lowercaseChar() in setOf('x', 'y', 'z') }) { "An implicit surface must contain x, y, or z." }
                SurfaceInputInterpretation(normalized, normalized, SpatialSurfaceKind.Implicit)
            }
        }
    }

    fun explicit(rawInput: String): Result<SurfaceInputInterpretation> = runCatching {
        val normalized = MathExpressionNormalizer.normalize(rawInput)
            .replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) {
            "Enter a surface equation, for example z = x^2 + y^2."
        }

        val equals = normalized.indexOf('=')
        val expression = if (equals < 0) {
            normalized
        } else {
            require(normalized.indexOf('=', equals + 1) < 0) {
                "Use one equation at a time."
            }
            val left = normalized.substring(0, equals).trim()
            val right = normalized.substring(equals + 1).trim()
            require(left.isNotBlank() && right.isNotBlank()) {
                "Both sides of the equation are required."
            }
            when {
                left.equals("z", ignoreCase = true) ||
                    left.matches(Regex("(?i)f\\s*\\(\\s*x\\s*,\\s*y\\s*\\)")) -> right
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
