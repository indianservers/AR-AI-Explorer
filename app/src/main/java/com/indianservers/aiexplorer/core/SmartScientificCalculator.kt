package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.round

enum class CalculatorTokenKind { Number, Function, Variable, Constant, Operator, Bracket, Unit, Text, Error }
enum class CalculatorKeyboardLayer { Basic, Scientific, Structural }
data class CalculatorToken(val text: String, val start: Int, val endExclusive: Int, val kind: CalculatorTokenKind)
data class CalculatorDiagnostic(val message: String, val position: Int? = null, val error: Boolean = true)
data class CalculatorTemplate(val label: String, val source: String, val cursorBack: Int = 0)
data class SmartEditorResult(val text: String, val cursor: Int)
data class CalculatorHistoryEntry(val input: String, val primary: String, val exact: String?, val angleMode: AngleMode, val branch: Int)
data class SmartCalculatorOutcome(
    val interpretedInput: String,
    val primary: String,
    val exact: String?,
    val decimal: String?,
    val alternatives: List<Pair<String, String>>,
    val steps: List<String>,
    val verification: String,
    val diagnostics: List<CalculatorDiagnostic> = emptyList(),
    val base: ScientificCalculatorResult? = null,
)

object CalculatorInputIntelligence {
    private val functions = setOf("sin", "cos", "tan", "sec", "csc", "cot", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "ln", "log", "exp", "min", "max", "floor", "ceil")
    private val constants = setOf("pi", "e", "c", "g", "h", "k")
    val templates = mapOf(
        CalculatorKeyboardLayer.Basic to listOf(CalculatorTemplate("fraction", "()/()", 4), CalculatorTemplate("power", "^()", 1), CalculatorTemplate("root", "sqrt()", 1)),
        CalculatorKeyboardLayer.Scientific to listOf(CalculatorTemplate("sin", "sin()", 1), CalculatorTemplate("log base", "ln()/ln()", 6), CalculatorTemplate("absolute", "abs()", 1)),
        CalculatorKeyboardLayer.Structural to listOf(
            CalculatorTemplate("derivative", "differentiate ()", 1), CalculatorTemplate("indefinite integral", "integrate ()", 1), CalculatorTemplate("definite integral", "integrate () from () to ()", 18),
            CalculatorTemplate("limit", "limit () as x -> ()", 14), CalculatorTemplate("Taylor", "taylor sin(x) order 7"), CalculatorTemplate("system", "solve x+y=5; x-y=1"),
            CalculatorTemplate("Newton", "newton () start 1", 9), CalculatorTemplate("bisection", "bisection () from 0 to 1", 13), CalculatorTemplate("ODE", "ode dy/dx = 2*y, y(0)=1"),
            CalculatorTemplate("recurrence", "recurrence a_n = a_n-1 + 1, a_1=1, n=10"), CalculatorTemplate("complex", "complex 3+4i"), CalculatorTemplate("matrix", "[[,],[,]]", 8), CalculatorTemplate("vector", "vector dot <,,> ; <,,>", 10),
            CalculatorTemplate("statistics", "stats "), CalculatorTemplate("probability", "normal cdf 0 mean 0 sd 1"), CalculatorTemplate("units", "convert  to "), CalculatorTemplate("uncertainty", "uncertainty  +/-  *  +/- "),
            CalculatorTemplate("programmer", "programmer "), CalculatorTemplate("finance", "future value  rate  years "), CalculatorTemplate("sum", "sum(,=,,)", 7),
        ),
    )

    fun interpret(source: String): String {
        var value = source.trim()
            .replace(Regex("(?i)^(calculate|evaluate|compute|what is)\\s*:?[ ]*"), "")
            .replace(Regex("(?i)square root of\\s+([^ ]+)"), "sqrt($1)")
            .replace(Regex("(?i)\\bmultiplied by\\b"), "*")
            .replace(Regex("(?i)\\bdivided by\\b"), "/")
            .replace(Regex("(?i)\\bplus\\b"), "+")
            .replace(Regex("(?i)\\bminus\\b"), "-")
        if (value.startsWith("solve ", true)) value = value.substring(6).trim()
        return value
    }

    fun diagnostics(source: String): List<CalculatorDiagnostic> {
        val result = mutableListOf<CalculatorDiagnostic>()
        val stack = ArrayDeque<Int>()
        source.forEachIndexed { index, char -> when (char) {
            '(' -> stack.addLast(index)
            ')' -> if (stack.isEmpty()) result += CalculatorDiagnostic("Closing bracket has no matching opening bracket.", index) else stack.removeLast()
        } }
        stack.forEach { result += CalculatorDiagnostic("Opening bracket is not closed.", it) }
        if (Regex("[+*/^]\\s*$").containsMatchIn(source)) result += CalculatorDiagnostic("Expression ends with an operator.", source.lastIndex)
        if (Regex("/\\s*0(?:\\.0*)?(?:\\D|$)").containsMatchIn(source)) result += CalculatorDiagnostic("Division by zero is undefined.")
        return result
    }

    fun smartInsert(source: String, selectionStart: Int, selectionEnd: Int, rawToken: String): SmartEditorResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)
        val selected = source.substring(start, end)
        val token = when (rawToken) { "×" -> "*"; "÷" -> "/"; "−" -> "-"; "π" -> "pi"; else -> rawToken }

        if (token == ")" && selected.isEmpty() && source.getOrNull(start) == ')') return SmartEditorResult(source, start + 1)
        if (token == "." && selected.isEmpty()) {
            val numberStart = source.substring(0, start).indexOfLast { !it.isDigit() && it != '.' } + 1
            if (source.substring(numberStart, start).contains('.')) return SmartEditorResult(source, start)
        }
        if (token.matches(Regex("[+*/^]")) && selected.isEmpty()) {
            val previous = source.substring(0, start).indexOfLast { !it.isWhitespace() }
            if (previous >= 0 && source[previous] in "+-*/^") {
                val replaced = source.replaceRange(previous, start, token)
                return SmartEditorResult(replaced, previous + token.length)
            }
        }

        val functionCall = token.endsWith("(") && token.dropLast(1).matches(Regex("[A-Za-z][A-Za-z0-9]*"))
        val insertion: String
        val cursorBack: Int
        when {
            functionCall -> { insertion = "$token$selected)"; cursorBack = if (selected.isEmpty()) 1 else 0 }
            token == "(" -> { insertion = "($selected)"; cursorBack = if (selected.isEmpty()) 1 else 0 }
            token == "sqrt(" -> { insertion = "sqrt($selected)"; cursorBack = if (selected.isEmpty()) 1 else 0 }
            else -> { insertion = token; cursorBack = 0 }
        }
        val prefix = source.substring(0, start)
        val needsMultiply = insertion.firstOrNull()?.let { first ->
            val previous = prefix.lastOrNull { !it.isWhitespace() }
            previous != null && (previous.isDigit() || previous == ')' || previous == ']' || previous.isLetter()) &&
                (first.isLetter() || first == '(' || (first.isDigit() && (previous == ')' || previous == ']'))) &&
                !(previous.isLetter() && first.isLetter())
        } == true
        val inserted = (if (needsMultiply) "*" else "") + insertion
        val text = source.replaceRange(start, end, inserted)
        return SmartEditorResult(text, start + inserted.length - cursorBack)
    }

    fun smartBackspace(source: String, selectionStart: Int, selectionEnd: Int): SmartEditorResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)
        if (start != end) return SmartEditorResult(source.removeRange(start, end), start)
        if (start == 0) return SmartEditorResult(source, 0)
        if (source.getOrNull(start - 1) == '(' && source.getOrNull(start) == ')') {
            val nameStart = source.substring(0, start - 1).indexOfLast { !it.isLetter() } + 1
            val removeFrom = if (nameStart < start - 1) nameStart else start - 1
            return SmartEditorResult(source.removeRange(removeFrom, start + 1), removeFrom)
        }
        return SmartEditorResult(source.removeRange(start - 1, start), start - 1)
    }

    fun toggleSign(source: String, selectionStart: Int, selectionEnd: Int): SmartEditorResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)
        if (start != end) {
            val replacement = "-(${source.substring(start, end)})"
            return SmartEditorResult(source.replaceRange(start, end, replacement), start + replacement.length)
        }
        if (source.isBlank()) return SmartEditorResult("-", 1)
        var numberEnd = start
        while (numberEnd < source.length && (source[numberEnd].isDigit() || source[numberEnd] == '.')) numberEnd++
        var numberStart = start
        while (numberStart > 0 && (source[numberStart - 1].isDigit() || source[numberStart - 1] == '.')) numberStart--
        if (numberStart == numberEnd) return smartInsert(source, start, start, "-")
        val minus = numberStart - 1
        val unaryMinus = minus >= 0 && source[minus] == '-' && (minus == 0 || source[minus - 1] in "+-*/^(")
        return if (unaryMinus) SmartEditorResult(source.removeRange(minus, minus + 1), (start - 1).coerceAtLeast(minus))
        else SmartEditorResult(source.substring(0, numberStart) + "-" + source.substring(numberStart), start + 1)
    }

    fun suggestions(source: String, cursor: Int): List<String> {
        val safeCursor = cursor.coerceIn(0, source.length)
        val prefix = source.substring(0, safeCursor).takeLastWhile { it.isLetter() }.lowercase()
        if (prefix.isBlank()) return emptyList()
        return (functions.map { "$it(" } + constants)
            .filter { it.removeSuffix("(").startsWith(prefix) && it.removeSuffix("(") != prefix }
            .sortedWith(compareBy<String> { it.removeSuffix("(").length }.thenBy { it })
            .take(4)
    }

    fun tokenize(source: String, knownVariables: Set<String> = emptySet()): List<CalculatorToken> {
        val pattern = Regex("[A-Za-z_][A-Za-z0-9_]*|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|[+\\-*/^%=!,]|[()\\[\\]]|\\s+|.")
        return pattern.findAll(source).map { match ->
            val text = match.value
            val lower = text.lowercase()
            val kind = when {
                text.isBlank() -> CalculatorTokenKind.Text
                text.first().isDigit() -> CalculatorTokenKind.Number
                lower in functions -> CalculatorTokenKind.Function
                lower in constants -> CalculatorTokenKind.Constant
                lower in knownVariables -> CalculatorTokenKind.Variable
                text.matches(Regex("[+\\-*/^%=!,]")) -> CalculatorTokenKind.Operator
                text.matches(Regex("[()\\[\\]]")) -> CalculatorTokenKind.Bracket
                text.first().isLetter() -> CalculatorTokenKind.Variable
                else -> CalculatorTokenKind.Error
            }
            CalculatorToken(text, match.range.first, match.range.last + 1, kind)
        }.toList()
    }
}

class CalculatorEditorHistory(initial: String = "") {
    private val undo = mutableListOf<String>()
    private val redo = mutableListOf<String>()
    var current: String = initial; private set
    fun edit(next: String): String { if (next != current) { undo += current; current = next; redo.clear() }; return current }
    fun undo(): String { if (undo.isNotEmpty()) { redo += current; current = undo.removeAt(undo.lastIndex) }; return current }
    fun redo(): String { if (redo.isNotEmpty()) { undo += current; current = redo.removeAt(redo.lastIndex) }; return current }
}

class SmartScientificCalculator(
    private val calculator: ScientificCalculator = ScientificCalculator(),
    private val graph: GraphAnalysis = GraphAnalysis(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
    private val advanced: AdvancedScientificCalculator = AdvancedScientificCalculator(),
    private val professional: ProfessionalScientificCalculator = ProfessionalScientificCalculator(),
) {
    val variables = linkedMapOf<String, Double>()
    val functions = linkedMapOf<String, String>()
    val history = mutableListOf<CalculatorHistoryEntry>()
    private var branch = 0

    fun evaluate(source: String, angleMode: AngleMode = AngleMode.Degrees, precision: Int = 8): SmartCalculatorOutcome {
        val interpreted = CalculatorInputIntelligence.interpret(source)
        val diagnostics = CalculatorInputIntelligence.diagnostics(interpreted)
        require(diagnostics.none { it.error }) { diagnostics.first().message }

        Regex("^([A-Za-z][A-Za-z0-9_]*)\\(x\\)\\s*=\\s*(.+)$").matchEntire(interpreted)?.let { match ->
            val name = match.groupValues[1]; val body = match.groupValues[2]
            ExpressionEngine().compile(expand(body))
            functions[name] = body
            return remember(source, angleMode, SmartCalculatorOutcome(interpreted, "$name(x) = $body", body, null, emptyList(), listOf("Parse a named function definition.", "Validate its body with the deterministic expression engine.", "Store it for cursor-safe substitution in later calculations."), "Function body parsed successfully."))
        }
        Regex("^([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*([^=]+)$").matchEntire(interpreted)?.takeIf { !interpreted.contains("==") }?.let { match ->
            val name = match.groupValues[1]; val base = calculator.evaluate(expand(match.groupValues[2]), angleMode, precision)
            variables[name] = base.value
            return remember(source, angleMode, SmartCalculatorOutcome(interpreted, "$name = ${base.decimal}", base.exactHint, base.decimal, alternatives(base), listOf("Recognize variable assignment.", "Evaluate the right-hand expression.", "Store $name for later expressions."), "Stored value reproduces the evaluated right-hand side.", base = base))
        }

        Regex("(?i)^integrate\\s+(.+)\\s+from\\s+(-?[^ ]+)\\s+to\\s+(-?[^ ]+)$").matchEntire(interpreted)?.let { match ->
            val expression = expand(match.groupValues[1]); val from = calculator.evaluate(expand(match.groupValues[2]), angleMode).value; val to = calculator.evaluate(expand(match.groupValues[3]), angleMode).value
            val value = graph.integral(expression, from, to); val base = calculator.evaluate(value.toString(), angleMode, precision)
            return remember(source, angleMode, SmartCalculatorOutcome(interpreted, base.decimal, base.exactHint, base.decimal, alternatives(base), listOf("Interpret a definite integral.", "Evaluate bounds: ${baseNumber(from)} to ${baseNumber(to)}.", "Apply adaptive numerical integration to $expression."), "Numerical integral is recomputed from the same deterministic integrand.", base = base))
        }

        if (interpreted.count { it == '=' } == 1) {
            val (left, right) = interpreted.split('=', limit = 2).map(::expand)
            val residual = "($left)-($right)"
            val roots = graph.roots(residual, -1000.0, 1000.0)
            require(roots.isNotEmpty()) { "No real solution was found in the supported search interval." }
            val shown = roots.joinToString { baseNumber(it) }
            val maxResidual = roots.maxOf { root -> abs(ExpressionEngine().compile(residual).eval(mapOf("x" to root))) }
            return remember(source, angleMode, SmartCalculatorOutcome(interpreted, "x = $shown", shown, null, listOf("Real solutions" to shown), listOf("Move all terms to one side: $residual = 0.", "Bracket sign changes and refine each root.", "Deduplicate numerically identical solutions."), "Maximum substitution residual ${"%.3e".format(maxResidual)}."))
        }

        professional.evaluate(interpreted)?.let { result ->
            return remember(source, angleMode, SmartCalculatorOutcome(
                interpreted, result.primary, null, null, result.details, result.steps, result.verification,
                result.warning?.let { listOf(CalculatorDiagnostic(it, error = false)) }.orEmpty(),
            ))
        }

        advanced.evaluate(interpreted)?.let { result ->
            return remember(source, angleMode, SmartCalculatorOutcome(
                interpreted, result.primary, result.exact, null, result.alternatives, result.steps, result.verification,
                result.warning?.let { listOf(CalculatorDiagnostic(it, error = false)) }.orEmpty(),
            ))
        }

        val expanded = expand(interpreted)
        val base = calculator.evaluate(expanded, angleMode, precision)
        val casRow = cas.simplify(expanded)
        val exact = when { casRow.supported && casRow.exact != expanded -> casRow.exact; else -> base.exactHint }
        val outcome = SmartCalculatorOutcome(
            interpreted, base.decimal, exact, base.decimal, alternatives(base),
            listOf("Interpret input as: $interpreted.", "Substitute stored variables and functions: $expanded.") + casRow.steps.map { "${it.title}: ${it.explanation}" } + base.steps,
            "Re-evaluation returns ${base.decimal}; ${base.warnings.joinToString().ifBlank { "no domain warning detected" }}.",
            base.warnings.map { CalculatorDiagnostic(it, error = false) }, base,
        )
        return remember(source, angleMode, outcome)
    }

    fun branchFrom(index: Int): String { require(index in history.indices); branch++; return history[index].input }

    private fun remember(source: String, mode: AngleMode, outcome: SmartCalculatorOutcome): SmartCalculatorOutcome {
        history += CalculatorHistoryEntry(source, outcome.primary, outcome.exact, mode, branch)
        return outcome
    }
    private fun expand(source: String): String {
        var value = source
        repeat(8) {
            functions.forEach { (name, body) -> value = Regex("\\b${Regex.escape(name)}\\(([^()]*)\\)").replace(value) { "(${body.replace(Regex("\\bx\\b"), "(${it.groupValues[1]})")})" } }
        }
        variables.forEach { (name, number) -> value = value.replace(Regex("\\b${Regex.escape(name)}\\b"), "(${number})") }
        return value
    }
    private fun alternatives(result: ScientificCalculatorResult) = buildList {
        add("Decimal" to result.decimal); add("Scientific" to result.scientific); add("Engineering" to result.engineering); result.exactHint?.let { add("Exact" to it) }
        if (result.value.isFinite()) add("Percent" to "${baseNumber(result.value * 100)}%")
    }
    private fun baseNumber(value: Double) = if (abs(value - round(value)) < 1e-10) round(value).toLong().toString() else "%.8f".format(value).trimEnd('0').trimEnd('.')
}
