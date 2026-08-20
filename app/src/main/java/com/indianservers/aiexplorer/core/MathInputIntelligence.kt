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

enum class MathInputAssistKind { Repair, Autocomplete, Parameter, Example }

enum class MathInputContext {
    General, Graph2D, Graph3D, Calculus, Matrix, Sets, Statistics, Science
}

data class MathInputAssistAction(
    val label: String,
    val detail: String,
    val replacement: String,
    val replaceStart: Int,
    val replaceEnd: Int,
    val cursorInReplacement: Int = replacement.length,
    val kind: MathInputAssistKind,
)

data class MathFunctionHint(
    val name: String,
    val signature: String,
    val description: String,
    val activeParameter: Int,
    val parameterName: String,
    val example: String,
)

data class MathInputAssistance(
    val actions: List<MathInputAssistAction> = emptyList(),
    val functionHint: MathFunctionHint? = null,
) {
    val primaryMessage: String?
        get() = functionHint?.let { "${it.signature} - ${it.parameterName}: ${it.description}" }
            ?: actions.firstOrNull()?.detail
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
    private data class InputDefinition(
        val name: String,
        val signature: String,
        val template: String,
        val description: String,
        val parameters: List<String>,
        val example: String,
        val contexts: Set<MathInputContext> = emptySet(),
        val command: Boolean = false,
    )

    private val definitions = listOf(
        InputDefinition("sin", "sin(angle)", "sin()", "angle in radians", listOf("angle"), "sin(pi/2)", setOf(MathInputContext.Graph2D, MathInputContext.Graph3D, MathInputContext.Calculus)),
        InputDefinition("cos", "cos(angle)", "cos()", "angle in radians", listOf("angle"), "cos(x)", setOf(MathInputContext.Graph2D, MathInputContext.Graph3D, MathInputContext.Calculus)),
        InputDefinition("tan", "tan(angle)", "tan()", "angle in radians", listOf("angle"), "tan(pi/4)", setOf(MathInputContext.Graph2D, MathInputContext.Calculus)),
        InputDefinition("sqrt", "sqrt(value)", "sqrt()", "value must be non-negative for real results", listOf("value"), "sqrt(x+4)"),
        InputDefinition("abs", "abs(value)", "abs()", "value or expression", listOf("value"), "abs(x-2)"),
        InputDefinition("ln", "ln(value)", "ln()", "value must be positive for real results", listOf("value"), "ln(x)"),
        InputDefinition("log", "log(value)", "log()", "base-ten logarithm", listOf("value"), "log(100)"),
        InputDefinition("logbase", "logbase(base, value)", "logbase(,)", "base, then positive value", listOf("base", "value"), "logbase(2,8)"),
        InputDefinition("min", "min(a, b, ...)", "min(,)", "two or more comparable values", listOf("first value", "next value"), "min(2,x)"),
        InputDefinition("max", "max(a, b, ...)", "max(,)", "two or more comparable values", listOf("first value", "next value"), "max(2,x)"),
        InputDefinition("if", "if(condition, trueValue, falseValue)", "if(,,)", "condition and the two result branches", listOf("condition", "true value", "false value"), "if(x<0,-x,x)", setOf(MathInputContext.Graph2D)),
        InputDefinition("derivative", "derivative(expression, variable[, order])", "derivative(,x)", "expression and differentiation variable", listOf("expression", "variable", "order"), "derivative(x^3,x)", setOf(MathInputContext.Calculus)),
        InputDefinition("integral", "integral(expression, variable[, lower, upper])", "integral(,x)", "expression, variable, and optional bounds", listOf("expression", "variable", "lower bound", "upper bound"), "integral(sin(x),x,0,pi)", setOf(MathInputContext.Calculus)),
        InputDefinition("limit", "limit(expression, variable, target)", "limit(,x,)", "expression, variable, and approach value", listOf("expression", "variable", "target"), "limit(sin(x)/x,x,0)", setOf(MathInputContext.Calculus)),
        InputDefinition("sum", "sum(expression, index, start, end)", "sum(,n,,)", "term, index, and inclusive bounds", listOf("expression", "index", "start", "end"), "sum(n^2,n,1,10)", setOf(MathInputContext.Calculus, MathInputContext.Statistics)),
        InputDefinition("product", "product(expression, index, start, end)", "product(,n,,)", "factor, index, and inclusive bounds", listOf("expression", "index", "start", "end"), "product(n,n,1,5)", setOf(MathInputContext.Calculus)),
        InputDefinition("mean", "mean(values)", "mean()", "list or comma-separated values", listOf("values"), "mean([2,4,6])", setOf(MathInputContext.Statistics)),
        InputDefinition("stdev", "stdev(values)", "stdev()", "list or comma-separated values", listOf("values"), "stdev([2,4,6])", setOf(MathInputContext.Statistics)),
        InputDefinition("det", "det(matrix)", "det()", "square matrix", listOf("matrix"), "det([[1,2],[3,4]])", setOf(MathInputContext.Matrix)),
        InputDefinition("surface", "surface(name, z=expression)", "surface(s,z=)", "name and a surface equation", listOf("name", "equation"), "surface(s,z=x^2+y^2)", setOf(MathInputContext.Graph3D)),
        InputDefinition("point", "point(name, x, y)", "point(P,,)", "name and 2D coordinates", listOf("name", "x coordinate", "y coordinate"), "point(A,2,3)", setOf(MathInputContext.Graph2D)),
        InputDefinition("simplify", "simplify expression", "simplify ", "reduce an expression without changing its value", listOf("expression"), "simplify (x^2-1)/(x-1)", command = true),
        InputDefinition("factor", "factor expression", "factor ", "factor an algebraic expression", listOf("expression"), "factor x^2-5*x+6", command = true),
        InputDefinition("solve", "solve equation [for variable]", "solve ", "equation or system and optional target variable", listOf("equation", "variable"), "solve 2*x+3=11", command = true),
        InputDefinition("plot", "plot expression", "plot ", "expression, equation, or relation", listOf("expression"), "plot y=sin(x)", setOf(MathInputContext.Graph2D), command = true),
    )

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
                    if (matches) brackets.removeAt(brackets.lastIndex) else bracketError = true
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

    fun assist(
        source: String,
        cursor: Int = source.length,
        context: MathInputContext = MathInputContext.General,
        limit: Int = 6,
    ): MathInputAssistance {
        val safeCursor = cursor.coerceIn(0, source.length)
        val actions = mutableListOf<MathInputAssistAction>()
        actions += repairActions(source, safeCursor)
        actions += completionActions(source, safeCursor, context)
        val hint = activeFunctionHint(source, safeCursor)
        if (hint != null) actions += parameterActions(source, safeCursor, hint)
        if (source.isBlank()) actions += exampleActions(context)
        return MathInputAssistance(
            actions = actions.distinctBy { listOf(it.replacement, it.replaceStart, it.replaceEnd) }.take(limit),
            functionHint = hint,
        )
    }

    fun apply(source: String, action: MathInputAssistAction): Pair<String, Int> {
        val start = action.replaceStart.coerceIn(0, source.length)
        val end = action.replaceEnd.coerceIn(start, source.length)
        val next = source.replaceRange(start, end, action.replacement)
        val cursor = (start + action.cursorInReplacement).coerceIn(0, next.length)
        return next to cursor
    }

    private fun completionActions(source: String, cursor: Int, context: MathInputContext): List<MathInputAssistAction> {
        var start = cursor
        while (start > 0 && (source[start - 1].isLetterOrDigit() || source[start - 1] == '_')) start--
        var end = cursor
        while (end < source.length && (source[end].isLetterOrDigit() || source[end] == '_')) end++
        val fragment = source.substring(start, cursor).lowercase()
        if (fragment.isBlank() || fragment.first().isDigit()) return emptyList()
        val exactFollowedByCall = definitions.any { it.name == fragment } && source.getOrNull(end) == '('
        if (exactFollowedByCall) return emptyList()
        return definitions.asSequence()
            .mapNotNull { definition ->
                val prefix = definition.name.startsWith(fragment)
                val typo = fragment.length >= 3 && (
                    editDistance(fragment, definition.name) <= 1 ||
                        isAdjacentTransposition(fragment, definition.name)
                    )
                if (!prefix && !typo) return@mapNotNull null
                val replacement = definition.template
                val cursorInReplacement = when {
                    replacement.endsWith("()") -> replacement.length - 1
                    replacement.contains("(,") -> replacement.indexOf('(') + 1
                    replacement.endsWith(" ") -> replacement.length
                    else -> replacement.indexOf('=').takeIf { it >= 0 }?.plus(1) ?: replacement.length
                }
                val contextual = context in definition.contexts
                Triple(
                    if (typo && !prefix) 0 else if (contextual) 1 else 2,
                    definition.name,
                    MathInputAssistAction(
                        label = if (typo && !prefix) "Fix ${definition.name}" else definition.name,
                        detail = if (typo && !prefix) "Repair '$fragment' to ${definition.name}" else definition.signature,
                        replacement = replacement,
                        replaceStart = start,
                        replaceEnd = end,
                        cursorInReplacement = cursorInReplacement,
                        kind = if (typo && !prefix) MathInputAssistKind.Repair else MathInputAssistKind.Autocomplete,
                    ),
                )
            }
            .sortedWith(compareBy<Triple<Int, String, MathInputAssistAction>> { it.first }.thenBy { it.second.length }.thenBy { it.second })
            .map { it.third }
            .take(5)
            .toList()
    }

    private fun repairActions(source: String, cursor: Int): List<MathInputAssistAction> = buildList {
        val closers = missingClosingDelimiters(source)
        if (closers != null && closers.isNotEmpty()) add(
            MathInputAssistAction(
                label = "Close $closers",
                detail = "Add ${closers.length} missing closing delimiter${if (closers.length == 1) "" else "s"}",
                replacement = closers,
                replaceStart = source.length,
                replaceEnd = source.length,
                kind = MathInputAssistKind.Repair,
            ),
        )
        val normalized = normalize(source)
        if (normalized != source.trim()) add(
            MathInputAssistAction("Normalize", "Use parser-safe operators", normalized, 0, source.length, normalized.length, MathInputAssistKind.Repair),
        )
        val multiplied = source
            .replace(Regex("(?<=\\d)(?=[A-Za-z(])"), "*")
            .replace(Regex("(?<=\\))(?=[A-Za-z0-9(])"), "*")
        if (multiplied != source) add(
            MathInputAssistAction("Add *", "Insert explicit multiplication", multiplied, 0, source.length, cursor + (multiplied.length - source.length), MathInputAssistKind.Repair),
        )
        val functionNames = definitions.filterNot { it.command }.joinToString("|") { Regex.escape(it.name) }
        val wrapped = source.replace(Regex("\\b($functionNames)\\s+([A-Za-z0-9_.]+)\\b", RegexOption.IGNORE_CASE), "$1($2)")
        if (wrapped != source) add(
            MathInputAssistAction("Add ( )", "Wrap the function argument", wrapped, 0, source.length, wrapped.length, MathInputAssistKind.Repair),
        )
    }

    private fun missingClosingDelimiters(source: String): String? {
        val stack = ArrayDeque<Char>()
        source.forEach { character ->
            when (character) {
                '(', '[', '{' -> stack.addLast(character)
                ')', ']', '}' -> {
                    val expected = when (character) { ')' -> '('; ']' -> '['; else -> '{' }
                    if (stack.lastOrNull() != expected) return null
                    stack.removeLast()
                }
            }
        }
        return stack.reversed().joinToString("") { when (it) { '(' -> ")"; '[' -> "]"; else -> "}" } }
    }

    private fun activeFunctionHint(source: String, cursor: Int): MathFunctionHint? {
        data class Frame(val name: String, var argument: Int = 0)
        val frames = mutableListOf<Frame>()
        for (index in 0 until cursor) {
            when (source[index]) {
                '(' -> {
                    var end = index
                    var start = end
                    while (start > 0 && (source[start - 1].isLetterOrDigit() || source[start - 1] == '_')) start--
                    frames += Frame(source.substring(start, end).lowercase())
                }
                ',' -> frames.lastOrNull()?.let { it.argument++ }
                ')' -> if (frames.isNotEmpty()) frames.removeAt(frames.lastIndex)
            }
        }
        val frame = frames.lastOrNull { it.name.isNotBlank() } ?: return null
        val definition = definitions.firstOrNull { it.name == frame.name } ?: return null
        val active = frame.argument.coerceIn(0, definition.parameters.lastIndex.coerceAtLeast(0))
        return MathFunctionHint(
            name = definition.name,
            signature = definition.signature,
            description = definition.description,
            activeParameter = active,
            parameterName = definition.parameters.getOrElse(active) { "argument ${active + 1}" },
            example = definition.example,
        )
    }

    private fun parameterActions(source: String, cursor: Int, hint: MathFunctionHint): List<MathInputAssistAction> {
        val values = when (hint.parameterName) {
            "angle" -> listOf("x", "pi/2", "pi", "theta")
            "variable", "index" -> listOf("x", "y", "n", "t")
            "matrix" -> listOf("[[1,0],[0,1]]", "[[1,2],[3,4]]")
            "condition" -> listOf("x<0", "x>=0")
            "lower bound", "start" -> listOf("0", "1", "-infinity")
            "upper bound", "end" -> listOf("1", "pi", "infinity")
            "target" -> listOf("0", "infinity", "pi")
            "name" -> listOf("P", "A", "s")
            else -> listOf("x", "0", "1", hint.example)
        }
        return values.distinct().take(4).map { value ->
            MathInputAssistAction(value, "Set ${hint.parameterName}", value, cursor, cursor, value.length, MathInputAssistKind.Parameter)
        }
    }

    private fun exampleActions(context: MathInputContext): List<MathInputAssistAction> {
        val examples = when (context) {
            MathInputContext.Graph2D -> listOf("y=sin(x)", "x^2+y^2=25", "y=if(x<0,-x,x)")
            MathInputContext.Graph3D -> listOf("z=sin(x)*cos(y)", "x^2+y^2+z^2=25", "surface(s,z=x^2+y^2)")
            MathInputContext.Calculus -> listOf("derivative(x^3,x)", "integral(sin(x),x,0,pi)", "limit(sin(x)/x,x,0)")
            MathInputContext.Matrix -> listOf("det([[1,2],[3,4]])", "rref([[1,2],[3,4]])")
            MathInputContext.Sets -> listOf("{1,2,3}", "A union B", "x in {1,2,3}")
            MathInputContext.Statistics -> listOf("mean([2,4,6])", "stdev([2,4,6])")
            MathInputContext.Science -> listOf("convert 5 km to m", "9.8 m/s^2")
            MathInputContext.General -> listOf("solve 2*x+3=11", "factor x^2-5*x+6", "sin(pi/2)")
        }
        return examples.map { example ->
            MathInputAssistAction(example, "Insert example", example, 0, 0, example.length, MathInputAssistKind.Example)
        }
    }

    private fun editDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { leftIndex, leftChar ->
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1
            right.forEachIndexed { rightIndex, rightChar ->
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + if (leftChar == rightChar) 0 else 1,
                )
            }
            previous = current
        }
        return previous[right.length]
    }

    private fun isAdjacentTransposition(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        val differences = left.indices.filter { left[it] != right[it] }
        return differences.size == 2 &&
            differences[1] == differences[0] + 1 &&
            left[differences[0]] == right[differences[1]] &&
            left[differences[1]] == right[differences[0]]
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
