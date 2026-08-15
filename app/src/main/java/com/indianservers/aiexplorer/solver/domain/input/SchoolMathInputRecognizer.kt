package com.indianservers.aiexplorer.solver.domain.input

enum class RecognitionReadiness { Ready, NeedsDetail, Unsupported }

data class SchoolMathRecognition(
    val topic: String,
    val readiness: RecognitionReadiness,
    val confidencePercent: Int,
    val canonicalInput: String? = null,
    val guidance: String,
    val assumptions: List<String> = emptyList(),
    val choices: List<RecognitionChoice> = emptyList(),
)

data class RecognitionChoice(val label: String, val canonicalInput: String, val explanation: String)

/** Fast, deterministic preview and canonicalization for common classroom input. */
object SchoolMathInputRecognizer {
    fun recognize(source: String): SchoolMathRecognition? {
        val clean = normalizeTypography(source).trim()
        if (clean.isBlank()) return null
        ambiguity(clean)?.let { (topic, choices) ->
            return SchoolMathRecognition(topic, RecognitionReadiness.NeedsDetail, 99, guidance = "This notation has more than one standard interpretation. Choose the intended form.", choices = choices)
        }
        canonicalNaturalLanguage(clean)?.let { (topic, canonical) ->
            return SchoolMathRecognition(topic, RecognitionReadiness.Ready, 96, canonical, "Classroom wording recognized. Review the mathematical form before solving.")
        }
        val lower = clean.lowercase()
        val topic = when {
            Regex("(?i)\\b(?:mean|median|mode|standard deviation|stats|regression|binomial|probability)\\b").containsMatchIn(clean) -> "Probability & statistics"
            lower.startsWith("matrix") || lower.startsWith("det(") || lower.startsWith("inverse(") ||
                lower.startsWith("rref(") || lower.startsWith("transpose(") || lower.startsWith("rank(") -> "Matrices"
            Regex("(?i)\\b(?:differentiate|derivative|integrate|integral|limit|continuity)\\b").containsMatchIn(clean) -> "Calculus"
            Regex("(?i)\\b(?:sin|cos|tan|trigonometry|degree|radian)\\b").containsMatchIn(clean) -> "Trigonometry"
            Regex("(?i)\\b(?:gcd|hcf|lcm|prime|factor|mod|base)\\b").containsMatchIn(clean) -> "Number theory"
            ';' in clean && clean.count { it == '=' } >= 2 -> "System of equations"
            '=' in clean && Regex("(?i)x\\s*\\^?\\s*2|x²").containsMatchIn(clean) -> "Quadratic equation"
            '=' in clean || Regex("[a-zA-Z]").containsMatchIn(clean) -> "Algebra"
            Regex("[0-9]").containsMatchIn(clean) -> "Arithmetic"
            else -> "School mathematics"
        }
        val incomplete = when {
            lower in setOf("differentiate", "derivative", "integrate", "integral", "limit") -> "Add the expression to the $lower instruction."
            clean.count { it == '(' } != clean.count { it == ')' } -> "Close every open parenthesis before solving."
            clean.endsWith("+") || clean.endsWith("-") || clean.endsWith("*") || clean.endsWith("/") || clean.endsWith("=") -> "Add the missing value or expression after the final operator."
            lower.startsWith("limit") && "->" !in clean -> "Specify the variable and approach value, for example: limit sin(x)/x as x -> 0."
            else -> null
        }
        val unsupported = SolverInputIntentGuard.rejectionReason(clean)
        return when {
            incomplete != null -> SchoolMathRecognition(topic, RecognitionReadiness.NeedsDetail, 92, guidance = incomplete)
            unsupported != null -> SchoolMathRecognition(topic, RecognitionReadiness.Unsupported, 90, guidance = unsupported)
            else -> SchoolMathRecognition(topic, RecognitionReadiness.Ready, if (topic == "School mathematics") 65 else 90, guidance = "Ready for verified offline solving.", assumptions = inferredAssumptions(clean, topic))
        }
    }

    fun canonicalize(source: String): String = recognize(source)?.canonicalInput ?: normalizeTypography(source).trim()

    private fun canonicalNaturalLanguage(source: String): Pair<String, String>? {
        Regex("(?i)^what\\s+is\\s+([0-9.]+)\\s+percent\\s+of\\s+([0-9.]+)\\??$").matchEntire(source)?.let {
            return "Percentage" to "${it.groupValues[1]}% of ${it.groupValues[2]}"
        }
        Regex("(?i)^(?:solve\\s+for\\s+[a-z]\\s*:?|solve\\s*:?)\\s*(.+)$").matchEntire(source)?.let {
            return "Equation" to classroomOperators(it.groupValues[1])
        }
        Regex("(?i)^find\\s+the\\s+derivative\\s+of\\s+(.+)$").matchEntire(source)?.let {
            return "Calculus" to "differentiate ${it.groupValues[1]}"
        }
        Regex("(?i)^find\\s+the\\s+integral\\s+of\\s+(.+)$").matchEntire(source)?.let {
            return "Calculus" to "integrate ${it.groupValues[1]}"
        }
        Regex("(?i)^calculate\\s+the\\s+(mean|median)\\s+of\\s+(.+)$").matchEntire(source)?.let {
            return "Statistics" to "${it.groupValues[1].lowercase()}(${it.groupValues[2]})"
        }
        Regex("(?i)^([0-9.]+)\\s+ka\\s+([0-9.]+)\\s+(?:percent|pratishat)\\??$").matchEntire(source)?.let {
            return "Percentage" to "${it.groupValues[2]}% of ${it.groupValues[1]}"
        }
        Regex("^([0-9.]+)\\s+का\\s+([0-9.]+)\\s+प्रतिशत\\??$").matchEntire(source)?.let {
            return "Percentage" to "${it.groupValues[2]}% of ${it.groupValues[1]}"
        }
        Regex("(?i)^(.+?)\\s+(?:hal\\s+karo|solve\\s+karo)$").matchEntire(source)?.let {
            return "Equation" to classroomOperators(it.groupValues[1])
        }
        Regex("^(.+?)\\s+हल\\s+करो$").matchEntire(source)?.let {
            return "Equation" to classroomOperators(it.groupValues[1])
        }
        return null
    }

    private fun classroomOperators(source: String) = source
        .replace("जमा", "+").replace("घटा", "-").replace("गुणा", "*").replace("भाग", "/").replace("बराबर", "=")
        .replace(Regex("(?i)\\bplus\\b"), "+")
        .replace(Regex("(?i)\\bjod\\b|\\bजमा\\b"), "+")
        .replace(Regex("(?i)\\bminus\\b"), "-")
        .replace(Regex("(?i)\\bghata\\b|\\bघटा\\b"), "-")
        .replace(Regex("(?i)\\btimes\\b|\\bmultiplied\\s+by\\b"), "*")
        .replace(Regex("(?i)\\bguna\\b|\\bगुणा\\b"), "*")
        .replace(Regex("(?i)\\bdivided\\s+by\\b"), "/")
        .replace(Regex("(?i)\\bbhaag\\b|\\bभाग\\b"), "/")
        .replace(Regex("(?i)\\bequals?\\b"), "=")
        .replace(Regex("(?i)\\bbarabar\\b|\\bबराबर\\b"), "=")
        .replace(Regex("\\s+"), " ").trim().trimEnd('.', '?')

    private fun normalizeTypography(source: String) = source
        .replace('−', '-').replace('×', '*').replace('÷', '/').replace('⁄', '/')
        .replace("≤", "<=").replace("≥", ">=").replace("→", "->")

    private fun inferredAssumptions(source: String, topic: String) = buildList {
        if (topic in setOf("Algebra", "Equation", "Quadratic equation") && !source.contains("complex", true)) add("Solve over real numbers unless stated otherwise.")
        if (topic == "Trigonometry" && !Regex("(?i)\\b(?:deg|degree|rad|radian)\\b").containsMatchIn(source)) add("Angles use the notation supplied; add deg or rad when ambiguous.")
    }

    private fun ambiguity(source: String): Pair<String, List<RecognitionChoice>>? {
        Regex("(?i)^(sin|cos|tan)\\s*\\(?\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)?$").matchEntire(source)?.let {
            val function = it.groupValues[1].lowercase(); val value = it.groupValues[2]
            return "Trigonometry" to listOf(
                RecognitionChoice("$value degrees", "$function(${value}deg)", "Interpret the angle in degrees."),
                RecognitionChoice("$value radians", "$function($value)", "Interpret the angle in radians."),
            )
        }
        Regex("(?i)^log\\s*\\(?\\s*([0-9.]+)\\s*\\)?$").matchEntire(source)?.let {
            val value = it.groupValues[1]
            return "Logarithms" to listOf(
                RecognitionChoice("Base 10", "log base 10 of $value", "Common logarithm."),
                RecognitionChoice("Natural log", "ln($value)", "Logarithm to base e."),
            )
        }
        Regex("^([0-9.]+)\\s*/\\s*([0-9.]+)\\s*([a-zA-Z])$").matchEntire(source)?.let {
            val a=it.groupValues[1]; val b=it.groupValues[2]; val variable=it.groupValues[3]
            return "Algebra" to listOf(
                RecognitionChoice("Fraction times $variable", "($a/$b)*$variable", "Multiply the fraction by $variable."),
                RecognitionChoice("Divide by ${b}$variable", "$a/($b*$variable)", "Place the variable in the denominator."),
            )
        }
        return null
    }
}
