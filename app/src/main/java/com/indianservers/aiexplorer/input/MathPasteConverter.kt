package com.indianservers.aiexplorer.input

data class MathPasteConversion(
    val original: String,
    val converted: String,
    val changes: List<String>,
    val warnings: List<String>,
) {
    val needsPreview: Boolean get() = original != converted || warnings.isNotEmpty()
    val canInsertConverted: Boolean get() = warnings.isEmpty() && original != converted
}

/** Clipboard-only conversion. Never applies OCR guesses or changes evaluator semantics. */
object MathPasteConverter {
    fun convert(original: String): MathPasteConversion {
        val changes = linkedSetOf<String>()
        return try {
            var source = original
            val trimmed = source.trim()
            val delimiters = listOf("$$" to "$$", "$" to "$", "\\(" to "\\)", "\\[" to "\\]")
            val wrapper = delimiters.firstOrNull { trimmed.startsWith(it.first) }
            if (wrapper != null) {
                require(trimmed.length >= wrapper.first.length + wrapper.second.length && trimmed.endsWith(wrapper.second)) {
                    "Unclosed LaTeX math delimiter"
                }
                source = trimmed.substring(wrapper.first.length, trimmed.length - wrapper.second.length)
                changes += "Removed LaTeX math delimiters"
            }
            val converted = Reader(source, changes).read()
            MathPasteConversion(original, converted, changes.toList(), emptyList())
        } catch (error: IllegalArgumentException) {
            MathPasteConversion(original, original, emptyList(), listOf(error.message ?: "Unsupported math notation"))
        }
    }

    private class Reader(val source: String, val changes: MutableSet<String>) {
        var position = 0
        var depth = 0
        val latex = '\\' in source || '$' in source
        val scaledDelimiters = mutableListOf<Char>()

        fun read(): String {
            val result = sequence(null)
            require(scaledDelimiters.isEmpty()) { "Unclosed LaTeX scalable delimiter" }
            return result
        }

        private fun sequence(close: Char?): String {
            val result = StringBuilder()
            while (position < source.length) {
                val char = source[position]
                if (char == close) { position++; return result.toString() }
                when {
                    char == '\\' -> {
                        val replacement = command()
                        if (replacement.length == 1 && replacement[0] in ")]|") {
                            require(replacement[0] == close) { "Unmatched LaTeX delimiter" }
                            return result.toString()
                        }
                        if (replacement.length == 1 && replacement[0] in "([|") {
                            val closing = when (replacement[0]) { '(' -> ')'; '[' -> ']'; else -> '|' }
                            require(++depth <= 64) { "Math nesting is too deep to convert" }
                            result.append(replacement).append(sequence(closing)).append(closing)
                            depth--
                        } else result.append(replacement)
                    }
                    char == '$' -> throw IllegalArgumentException("Unexpected LaTeX math delimiter")
                    char == '{' && (latex || close != null) -> result.append("(" + group('{', '}') + ")")
                    char == '}' && (latex || close != null) -> throw IllegalArgumentException("Unmatched LaTeX brace")
                    char in "([" && (latex || close != null) -> {
                        val closing = if (char == '(') ')' else ']'
                        result.append(char).append(group(char, closing)).append(closing)
                    }
                    char in ")]" && (latex || close != null) -> throw IllegalArgumentException("Unmatched math group")
                    char in "^_" && source.getOrNull(position + 1) == '{' -> {
                        position++
                        result.append(char).append('(').append(group('{', '}')).append(')')
                        changes += "Converted grouped powers or subscripts"
                    }
                    superscripts.containsKey(char) -> {
                        val exponent = StringBuilder()
                        while (position < source.length && superscripts.containsKey(source[position])) {
                            exponent.append(superscripts.getValue(source[position++]))
                        }
                        result.append("^(").append(exponent).append(')')
                        changes += "Converted Unicode superscripts"
                    }
                    char == '√' -> {
                        position++
                        result.append("sqrt(").append(operand()).append(')')
                        changes += "Converted Unicode square root"
                    }
                    unicode.containsKey(char) -> {
                        require(char !in vulgarFractions || source.getOrNull(position - 1)?.isDigit() != true) {
                            "Mixed-number notation is ambiguous; use an explicit addition or multiplication"
                        }
                        position++
                        result.append(unicode.getValue(char))
                        changes += "Converted Unicode symbols or fractions"
                    }
                    else -> { result.append(char); position++ }
                }
            }
            require(close == null) { "Unclosed math group" }
            return result.toString()
        }

        private fun group(open: Char, close: Char): String {
            require(source.getOrNull(position) == open) { "Expected '$open' group" }
            require(++depth <= 64) { "Math nesting is too deep to convert" }
            position++
            val result = sequence(close)
            depth--
            return result
        }

        private fun spaces() { while (source.getOrNull(position)?.isWhitespace() == true) position++ }

        private fun requiredGroup(): String {
            spaces()
            return group('{', '}')
        }

        private fun operand(): String {
            spaces()
            return when (source.getOrNull(position)) {
                '(' -> group('(', ')')
                '{' -> group('{', '}')
                else -> {
                    val match = Regex("(?:[A-Za-z][A-Za-z0-9_]*|[0-9]+(?:\\.[0-9]+)?|π)")
                        .find(source, position)?.takeIf { it.range.first == position }
                    require(match != null) { "Square root needs an explicit operand" }
                    position += match.value.length
                    require(source.getOrNull(position)?.let { it == '(' || it == '^' || it in superscripts || it.isLetterOrDigit() } != true) {
                        "Use parentheses around the complete square-root operand"
                    }
                    if (match.value == "π") "pi" else match.value
                }
            }
        }

        private fun command(): String {
            position++
            val start = position
            while (source.getOrNull(position)?.let { it in 'a'..'z' || it in 'A'..'Z' } == true) position++
            val name = source.substring(start, position)
            require(name.isNotEmpty()) { "Unsupported LaTeX escape" }
            changes += "Converted LaTeX notation"
            return when (name) {
                "frac", "dfrac", "tfrac" -> "((${requiredGroup()})/(${requiredGroup()}))"
                "sqrt" -> {
                    spaces()
                    val index = if (source.getOrNull(position) == '[') group('[', ']') else null
                    val argument = requiredGroup()
                    if (index == null) "sqrt($argument)" else "nthroot($index,$argument)"
                }
                "left", "right" -> {
                    spaces()
                    require(source.getOrNull(position) in listOf('(', ')', '[', ']')) {
                        "Unsupported LaTeX scalable delimiter"
                    }
                    val delimiter = source[position++]
                    if (name == "left") {
                        require(delimiter in "([") { "Expected an opening LaTeX delimiter" }
                        scaledDelimiters += if (delimiter == '(') ')' else ']'
                    } else {
                        require(scaledDelimiters.lastOrNull() == delimiter) { "Unmatched LaTeX scalable delimiter" }
                        scaledDelimiters.removeAt(scaledDelimiters.lastIndex)
                    }
                    delimiter.toString()
                }
                in functions -> {
                    spaces()
                    val base = if (name == "log" && source.getOrNull(position) == '_') {
                        position++
                        requiredGroup()
                    } else null
                    spaces()
                    val exponent = if (source.getOrNull(position) == '^') {
                        position++
                        requiredGroup()
                    } else null
                    spaces()
                    val call =
                    if (source.startsWith("\\left", position)) {
                        require(command() == "(") { "Function needs an explicit argument group" }
                        // Consume the matching \\right inside the argument through normal parsing.
                        require(++depth <= 64) { "Math nesting is too deep to convert" }
                        val argument = sequence(')')
                        depth--
                        functionCall(name, base, argument)
                    } else {
                        val argument = when (source.getOrNull(position)) {
                            '(' -> group('(', ')')
                            '{' -> group('{', '}')
                            else -> throw IllegalArgumentException("\\$name needs an explicit argument group")
                        }
                        functionCall(name, base, argument)
                    }
                    if (exponent == null) call else "($call)^($exponent)"
                }
                in commands -> commands.getValue(name)
                else -> throw IllegalArgumentException("Unsupported LaTeX command: \\$name")
            }
        }
    }

    private fun functionName(name: String): String = when (name) {
        "arcsin" -> "asin"
        "arccos" -> "acos"
        "arctan" -> "atan"
        else -> name
    }

    // The evaluator's log function is base ten. Use change of base instead of
    // serializing a second argument that some existing evaluation paths ignore.
    private fun functionCall(name: String, base: String?, argument: String): String =
        if (base == null) "${functionName(name)}($argument)" else "((ln($argument))/(ln($base)))"

    private val functions = setOf("sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "ln", "log", "exp", "arcsin", "arccos", "arctan")
    private val commands = mapOf("pi" to "pi", "times" to "*", "cdot" to "*", "div" to "/", "le" to "<=", "leq" to "<=", "ge" to ">=", "geq" to ">=", "ne" to "!=", "neq" to "!=")
    private val superscripts = "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻".zip("0123456789+-").toMap()
    private val vulgarFractions = "½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞"
    private val unicode = mapOf(
        '×' to "*", '·' to "*", '⋅' to "*", '÷' to "/", '⁄' to "/", '−' to "-",
        '≤' to "<=", '≥' to ">=", '≠' to "!=", 'π' to "pi",
        '½' to "((1)/(2))", '⅓' to "((1)/(3))", '⅔' to "((2)/(3))", '¼' to "((1)/(4))", '¾' to "((3)/(4))",
        '⅕' to "((1)/(5))", '⅖' to "((2)/(5))", '⅗' to "((3)/(5))", '⅘' to "((4)/(5))",
        '⅙' to "((1)/(6))", '⅚' to "((5)/(6))", '⅛' to "((1)/(8))", '⅜' to "((3)/(8))", '⅝' to "((5)/(8))", '⅞' to "((7)/(8))",
    )
}
