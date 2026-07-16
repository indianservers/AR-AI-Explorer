package com.indianservers.aiexplorer.core

enum class MathInputTokenKind { Function, Number, Variable, Constant, Operator, Bracket, Keyword, Text, Error }

data class MathInputToken(val start: Int, val end: Int, val text: String, val kind: MathInputTokenKind, val depth: Int = 0)
data class MathInputAnalysis(
    val tokens: List<MathInputToken>,
    val validBrackets: Boolean,
    val message: String,
    val suggestions: List<String>,
)

object MathInputIntelligence {
    val functions = setOf("sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "asin", "acos", "atan", "sqrt", "abs", "exp", "ln", "log", "min", "max")
    val constants = setOf("pi", "π", "e", "infinity", "∞")
    val keywords = setOf("solve", "evaluate", "simplify", "differentiate", "derivative", "partial", "integrate", "integral", "from", "to", "with", "respect", "mean", "median", "mode", "range", "system", "of")

    fun analyze(source: String): MathInputAnalysis {
        val tokens = mutableListOf<MathInputToken>()
        val brackets = ArrayDeque<Pair<Char, Int>>()
        var bracketError = false
        var i = 0
        while (i < source.length) {
            val start = i
            val character = source[i]
            when {
                character.isWhitespace() -> i++
                character.isDigit() || (character == '.' && source.getOrNull(i + 1)?.isDigit() == true) -> {
                    i++
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                    tokens += MathInputToken(start, i, source.substring(start, i), MathInputTokenKind.Number)
                }
                character.isLetter() || character == 'π' || character == '∞' -> {
                    i++
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                    val text = source.substring(start, i)
                    val normalized = text.lowercase()
                    val kind = when {
                        normalized in functions -> MathInputTokenKind.Function
                        normalized in constants -> MathInputTokenKind.Constant
                        normalized in keywords -> MathInputTokenKind.Keyword
                        text.length == 1 -> MathInputTokenKind.Variable
                        else -> MathInputTokenKind.Text
                    }
                    tokens += MathInputToken(start, i, text, kind)
                }
                character in "([{“" -> {
                    brackets.addLast(character to start)
                    i++
                    tokens += MathInputToken(start, i, character.toString(), MathInputTokenKind.Bracket, brackets.size)
                }
                character in ")] }”".replace(" ", "") -> {
                    val expected = when (character) { ')' -> '('; ']' -> '['; '}' -> '{'; '”' -> '“'; else -> character }
                    val matches = brackets.lastOrNull()?.first == expected
                    val depth = brackets.size.coerceAtLeast(1)
                    if (matches) brackets.removeLast() else bracketError = true
                    i++
                    tokens += MathInputToken(start, i, character.toString(), if (matches) MathInputTokenKind.Bracket else MathInputTokenKind.Error, depth)
                }
                character in "+-−*/×÷^=<>≤≥!%,;:∫∂" -> {
                    i++
                    if (i < source.length && source.substring(start, i + 1) in setOf("<=", ">=", "==", "!=")) i++
                    tokens += MathInputToken(start, i, source.substring(start, i), MathInputTokenKind.Operator)
                }
                else -> {
                    i++
                    tokens += MathInputToken(start, i, character.toString(), MathInputTokenKind.Text)
                }
            }
        }
        val valid = !bracketError && brackets.isEmpty()
        val message = when {
            source.isBlank() -> "Start typing a question or insert a maths function."
            bracketError -> "A closing bracket does not match its opening bracket."
            brackets.isNotEmpty() -> "${brackets.size} closing bracket${if (brackets.size == 1) "" else "s"} missing."
            tokens.any { it.kind == MathInputTokenKind.Text } -> "Natural-language text detected; known maths tokens are highlighted."
            else -> "Syntax structure looks balanced."
        }
        val lower = source.lowercase()
        val suggestions = buildList {
            if (lower.contains("derivative") || lower.contains("differentiate")) add("Try a higher or partial derivative")
            if (lower.contains("integral") || lower.contains("integrate") || lower.contains('∫')) add("Add 'from 0 to pi' for a definite integral")
            if (functions.any(lower::contains)) add("Functions support nested chain-rule expressions")
            if (isEmpty()) add("Try: Differentiate x*sin(x)")
        }
        return MathInputAnalysis(tokens, valid, message, suggestions)
    }
}
