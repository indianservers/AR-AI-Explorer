package com.indianservers.aiexplorer.core

enum class MathInputTokenKind {
    Command, Function, Number, Variable, Constant, Unit, Operator, Relation, Bracket, Separator, Keyword, Text, Error
}

enum class MathInputIntent(val label: String) {
    Empty("Ready for maths"), Arithmetic("Arithmetic"), Equation("Equation solving"), Inequality("Inequality"),
    Graph("Graphing"), Geometry2D("2D geometry"), Geometry3D("3D geometry"), Calculus("Calculus"),
    Trigonometry("Trigonometry"), Algebra("Symbolic algebra"), LinearAlgebra("Linear algebra"),
    Statistics("Statistics"), Units("Units and conversion"), DifferentialEquation("Differential equation"),
    Construction("Construction command"), NaturalLanguage("Maths question"), Unknown("Mathematical expression")
}

data class MathInputToken(
    val start: Int,
    val end: Int,
    val text: String,
    val kind: MathInputTokenKind,
    val depth: Int = 0,
)

data class MathInputAnalysis(
    val tokens: List<MathInputToken>,
    val validBrackets: Boolean,
    val message: String,
    val suggestions: List<String>,
    val intent: MathInputIntent = MathInputIntent.Unknown,
    val confidence: Double = 0.0,
    val normalizedPreview: String = "",
    val expectedNext: List<String> = emptyList(),
    val variables: Set<String> = emptySet(),
) {
    val hasErrors: Boolean get() = tokens.any { it.kind == MathInputTokenKind.Error }
    val accessibleSummary: String get() = buildString {
        append(intent.label); append(". "); append(message)
        if (variables.isNotEmpty()) append(" Variables: ${variables.joinToString()}.")
    }
}

/** Deterministic, local and cursor-safe understanding for every mathematical editor. */
object MathInputIntelligence {
    val functions = setOf(
        "sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "asin", "acos", "atan",
        "arcsin", "arccos", "arctan", "sqrt", "abs", "exp", "ln", "log", "log10", "min", "max",
        "floor", "ceil", "round", "det", "trace", "rank", "eigenvalues", "eigenvectors", "limit", "sum", "product"
    )
    val constants = setOf("pi", "π", "e", "i", "infinity", "∞", "phi", "φ")
    val commands = setOf(
        "solve", "evaluate", "simplify", "expand", "factor", "differentiate", "derivative", "integrate", "integral",
        "plot", "graph", "calculate", "convert", "approximate", "substitute", "limit", "rref", "determinant",
        "point", "point2d", "point3d", "midpoint", "section", "centroid", "circumcenter", "incenter", "orthocenter",
        "line2d", "line3d", "plane3d", "circle", "ellipse", "parabola", "hyperbola", "parallel", "perpendicular",
        "intersection", "tangent", "surface", "implicitsurface", "parametricsurface", "rotate", "reflect", "translate", "dilate"
    )
    val keywords = setOf(
        "partial", "from", "to", "as", "approaches", "with", "respect", "mean", "median", "mode", "range",
        "system", "of", "for", "where", "assuming", "given", "and", "or", "over", "through", "about", "matrix"
    )
    val units = setOf(
        "mm", "cm", "m", "km", "in", "ft", "yd", "mi", "mg", "g", "kg", "s", "ms", "min", "h", "hr",
        "rad", "deg", "hz", "khz", "n", "pa", "kpa", "j", "kj", "w", "kw", "v", "a", "c", "k", "mol", "l", "ml", "%"
    )

    fun analyze(source: String): MathInputAnalysis {
        if (source.isBlank()) return MathInputAnalysis(
            emptyList(), true, "Type naturally or enter an exact expression.",
            listOf("Try: solve x² - 5x + 6 = 0", "Try: surface(s, z=x²+y²)"), MathInputIntent.Empty, 1.0,
            expectedNext = listOf("number", "variable", "function", "command"),
        )
        val tokens = mutableListOf<MathInputToken>(); val brackets = ArrayDeque<Pair<Char, Int>>(); val unmatchedOpeners = mutableSetOf<Int>()
        var bracketError = false; var index = 0
        while (index < source.length) {
            val start = index; val character = source[index]
            when {
                character.isWhitespace() -> index++
                character.isDigit() || character == '.' && source.getOrNull(index + 1)?.isDigit() == true -> {
                    index = scanNumber(source, index)
                    tokens += MathInputToken(start, index, source.substring(start, index), MathInputTokenKind.Number)
                }
                character.isLetter() || character == '_' || character in "π∞φ" -> {
                    index++
                    while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
                    val text = source.substring(start, index); val normalized = text.lowercase(); val previous = tokens.lastOrNull()
                    val kind = when {
                        normalized in commands -> MathInputTokenKind.Command
                        normalized in functions -> MathInputTokenKind.Function
                        normalized in constants -> MathInputTokenKind.Constant
                        normalized in units && previous?.kind in setOf(MathInputTokenKind.Number, MathInputTokenKind.Bracket, MathInputTokenKind.Unit) -> MathInputTokenKind.Unit
                        normalized in keywords -> MathInputTokenKind.Keyword
                        text.length <= 2 || source.getOrNull(index) == '(' -> MathInputTokenKind.Variable
                        else -> MathInputTokenKind.Text
                    }
                    tokens += MathInputToken(start, index, text, kind)
                }
                character in "([{“" -> {
                    brackets.addLast(character to start); index++
                    tokens += MathInputToken(start, index, character.toString(), MathInputTokenKind.Bracket, brackets.size)
                }
                character in ")] }”".replace(" ", "") -> {
                    val expected = when (character) { ')' -> '('; ']' -> '['; '}' -> '{'; '”' -> '“'; else -> character }
                    val matches = brackets.lastOrNull()?.first == expected; val depth = brackets.size.coerceAtLeast(1)
                    if (matches) brackets.removeLast() else bracketError = true
                    index++; tokens += MathInputToken(start, index, character.toString(), if (matches) MathInputTokenKind.Bracket else MathInputTokenKind.Error, depth)
                }
                source.startsWith("<=", index) || source.startsWith(">=", index) || source.startsWith("!=", index) || source.startsWith("==", index) || source.startsWith("->", index) -> {
                    index += 2; tokens += MathInputToken(start, index, source.substring(start, index), MathInputTokenKind.Relation)
                }
                character in "=<>≤≥≈≠∈→" -> { index++; tokens += MathInputToken(start, index, character.toString(), MathInputTokenKind.Relation) }
                character in "+-−*/×÷^!%∫∂'" -> { index++; tokens += MathInputToken(start, index, character.toString(), MathInputTokenKind.Operator) }
                character in ",;:" -> { index++; tokens += MathInputToken(start, index, character.toString(), MathInputTokenKind.Separator) }
                else -> { index++; tokens += MathInputToken(start, index, character.toString(), MathInputTokenKind.Text) }
            }
        }
        brackets.forEach { unmatchedOpeners += it.second }
        val decorated = tokens.map { token -> if (token.start in unmatchedOpeners) token.copy(kind = MathInputTokenKind.Error) else token }
        val valid = !bracketError && brackets.isEmpty(); val intent = inferIntent(source, decorated); val confidence = confidence(intent, decorated, valid)
        val variables = decorated.filter { it.kind == MathInputTokenKind.Variable }.map { it.text }.toSortedSet()
        val message = when {
            bracketError -> "A closing delimiter does not match its opening delimiter."
            brackets.isNotEmpty() -> "Complete ${brackets.size} open delimiter${if (brackets.size == 1) "" else "s"}."
            decorated.lastOrNull()?.kind in setOf(MathInputTokenKind.Operator, MathInputTokenKind.Relation, MathInputTokenKind.Separator) -> "Expression is incomplete; enter the next value."
            intent == MathInputIntent.NaturalLanguage -> "Understood as a mathematical question; exact tokens remain editable."
            else -> "Recognized ${intent.label.lowercase()} input."
        }
        return MathInputAnalysis(
            decorated, valid, message, suggestions(source, intent, valid), intent, confidence,
            normalizedPreview = normalize(source), expectedNext = expectedNext(decorated, valid), variables = variables,
        )
    }

    private fun scanNumber(source: String, start: Int): Int {
        var i = start; var decimal = false
        while (i < source.length && (source[i].isDigit() || source[i] == '.' && !decimal)) { if (source[i] == '.') decimal = true; i++ }
        if (source.getOrNull(i) in setOf('e', 'E') && (source.getOrNull(i + 1)?.isDigit() == true || source.getOrNull(i + 1) in setOf('+', '-'))) {
            i++; if (source.getOrNull(i) in setOf('+', '-')) i++; while (source.getOrNull(i)?.isDigit() == true) i++
        }
        return i
    }

    private fun inferIntent(source: String, tokens: List<MathInputToken>): MathInputIntent {
        val lower = source.lowercase(); val command = tokens.firstOrNull { it.kind == MathInputTokenKind.Command }?.text?.lowercase()
        return when {
            Regex("(?:dy/dx|d²y|y''|y'|dydx)").containsMatchIn(lower) -> MathInputIntent.DifferentialEquation
            command in setOf("point", "point2d", "midpoint", "section", "centroid", "circumcenter", "incenter", "orthocenter", "line2d", "circle", "ellipse", "parabola", "hyperbola", "parallel", "perpendicular", "intersection", "tangent", "rotate", "reflect", "translate", "dilate") -> MathInputIntent.Construction
            command in setOf("point3d", "line3d", "plane3d", "surface", "implicitsurface", "parametricsurface") || Regex("""\b(?:plane|sphere|surface|vector)\b""").containsMatchIn(lower) -> MathInputIntent.Geometry3D
            Regex("""\b(?:integrat|deriv|differentiat|limit|series|gradient|hessian)\w*\b""").containsMatchIn(lower) || source.any { it in "∫∂" } -> MathInputIntent.Calculus
            tokens.any { it.kind == MathInputTokenKind.Function && it.text.lowercase() in setOf("sin", "cos", "tan", "sec", "csc", "cot", "asin", "acos", "atan") } -> MathInputIntent.Trigonometry
            Regex("""\b(?:matrix|determinant|eigen|rref|rank|inverse|vector)\w*\b""").containsMatchIn(lower) || source.contains("[[") || source.contains("{{") -> MathInputIntent.LinearAlgebra
            Regex("""\b(?:mean|median|mode|variance|standard deviation|regression|probability)\b""").containsMatchIn(lower) -> MathInputIntent.Statistics
            command == "convert" || tokens.any { it.kind == MathInputTokenKind.Unit } -> MathInputIntent.Units
            command in setOf("plot", "graph") || Regex("""^[yfr]\s*=|^[a-z]\([a-z]\)\s*=""").containsMatchIn(lower.trim()) -> MathInputIntent.Graph
            command in setOf("factor", "expand", "simplify", "substitute") -> MathInputIntent.Algebra
            tokens.any { it.kind == MathInputTokenKind.Relation && it.text in setOf("<", ">", "<=", ">=", "≤", "≥", "≠") } -> MathInputIntent.Inequality
            tokens.any { it.kind == MathInputTokenKind.Relation && it.text in setOf("=", "==", "≈") } -> MathInputIntent.Equation
            command in setOf("solve", "evaluate", "calculate", "approximate") -> MathInputIntent.Arithmetic
            tokens.any { it.kind == MathInputTokenKind.Text } -> MathInputIntent.NaturalLanguage
            tokens.any { it.kind in setOf(MathInputTokenKind.Number, MathInputTokenKind.Variable, MathInputTokenKind.Operator) } -> MathInputIntent.Arithmetic
            else -> MathInputIntent.Unknown
        }
    }

    private fun confidence(intent: MathInputIntent, tokens: List<MathInputToken>, valid: Boolean): Double {
        if (intent == MathInputIntent.Empty) return 1.0
        var result = when (intent) { MathInputIntent.Unknown -> .45; MathInputIntent.NaturalLanguage -> .72; else -> .9 }
        if (!valid) result -= .2
        if (tokens.any { it.kind == MathInputTokenKind.Error }) result -= .2
        return result.coerceIn(.1, .99)
    }

    private fun expectedNext(tokens: List<MathInputToken>, valid: Boolean): List<String> = when {
        !valid -> listOf("matching delimiter")
        tokens.isEmpty() -> listOf("number", "variable", "function", "command")
        tokens.last().kind in setOf(MathInputTokenKind.Operator, MathInputTokenKind.Relation, MathInputTokenKind.Separator) -> listOf("number", "variable", "function", "opening bracket")
        tokens.last().kind == MathInputTokenKind.Function -> listOf("(")
        else -> listOf("operator", "relation", "closing bracket")
    }

    private fun suggestions(source: String, intent: MathInputIntent, valid: Boolean): List<String> = buildList {
        if (!valid) add("Match (), [] and {} before evaluating")
        when (intent) {
            MathInputIntent.Calculus -> add("Add bounds with ‘from … to …’ for a definite integral")
            MathInputIntent.Equation -> add("Add ‘solve for x’ when several variables are present")
            MathInputIntent.Graph -> add("Add a domain such as x=-5..5")
            MathInputIntent.Geometry2D, MathInputIntent.Construction -> add("Use named points so dependencies remain inspectable")
            MathInputIntent.Geometry3D -> add("Specify x, y and z ranges for a bounded surface")
            MathInputIntent.LinearAlgebra -> add("Use [[a,b],[c,d]] for an editable matrix")
            MathInputIntent.Units -> add("Include both source and target units")
            MathInputIntent.Empty -> add("Try: Differentiate x*sin(x)")
            else -> if (source.length < 4) add("Keep typing—intent updates live")
        }
    }.distinct()

    private fun normalize(source: String) = source
        .replace('×', '*').replace('÷', '/').replace('−', '-').replace("≤", "<=").replace("≥", ">=")
        .replace("π", "pi").replace("∞", "infinity").trim().replace(Regex("\\s+"), " ")
}
