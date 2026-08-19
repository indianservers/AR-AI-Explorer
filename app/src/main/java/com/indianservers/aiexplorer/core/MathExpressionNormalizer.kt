package com.indianservers.aiexplorer.core

/**
 * Converts symbols produced by Android keyboards, handwriting keyboards and
 * pasted textbook notation into the ASCII vocabulary understood by the engine.
 *
 * Normalization happens only for evaluation; editors keep the learner's exact
 * text, cursor, selection and IME composition.
 */
object MathExpressionNormalizer {
    private val superscripts = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-',
    )

    fun normalize(source: String): String {
        var value = source
            .replace("π", "pi")
            .replace("Π", "pi")
            .replace('−', '-')
            .replace('–', '-')
            .replace('×', '*')
            .replace('·', '*')
            .replace('⋅', '*')
            .replace('÷', '/')
            .replace('⁄', '/')
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace("≠", "!=")
            .replace("≈", "==")
            .replace("½", "(1/2)")
            .replace("⅓", "(1/3)")
            .replace("⅔", "(2/3)")
            .replace("¼", "(1/4)")
            .replace("¾", "(3/4)")

        value = value.replace(Regex("""√\s*\("""), "sqrt(")
        value = Regex("""√\s*([A-Za-z_][A-Za-z0-9_]*|\d+(?:\.\d+)?)""")
            .replace(value) { match -> "sqrt(${match.groupValues[1]})" }
        value = Regex("""(?i)\bpie\b""").replace(value, "pi")
        return normalizeFunctionPowers(expandSuperscripts(value)).trim()
    }

    private fun normalizeFunctionPowers(source: String): String {
        var value = source
        val functionNames = "sin|cos|tan|sec|csc|cot|sinh|cosh|tanh|sech|csch|coth"
        val parenthesizedPowerCall = Regex("""(?i)\b($functionNames)\s*\^\s*\(\s*([+]?\d+)\s*\)\s*\(""")
        val plainPowerCall = Regex("""(?i)\b($functionNames)\s*\^\s*([+]?\d+)\s*\(""")

        var changed: Boolean
        do {
            changed = false
            val afterParenthesized = replaceFunctionPowerCall(value, parenthesizedPowerCall)
            if (afterParenthesized != value) changed = true
            value = afterParenthesized

            val afterPlain = replaceFunctionPowerCall(value, plainPowerCall)
            if (afterPlain != value) changed = true
            value = afterPlain
        } while (changed)
        return value
    }

    private fun replaceFunctionPowerCall(source: String, pattern: Regex): String {
        val match = pattern.find(source) ?: return source
        val functionName = match.groupValues[1]
        val exponent = match.groupValues[2].trimStart('+')
        val argumentStart = match.range.last
        val argumentEnd = findClosingParenthesis(source, argumentStart) ?: return source
        val argument = source.substring(argumentStart + 1, argumentEnd)
        val replacement = "($functionName($argument))^$exponent"
        return source.substring(0, match.range.first) + replacement + source.substring(argumentEnd + 1)
    }

    private fun findClosingParenthesis(source: String, openIndex: Int): Int? {
        var depth = 0
        for (index in openIndex until source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun expandSuperscripts(source: String): String {
        val result = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            if (superscripts[source[index]] == null) {
                result.append(source[index++])
                continue
            }
            val exponent = StringBuilder()
            while (index < source.length) {
                exponent.append(superscripts[source[index]] ?: break)
                index++
            }
            val text = exponent.toString()
            if (text.startsWith('+') || text.startsWith('-')) {
                result.append("^(").append(text).append(')')
            } else {
                result.append('^').append(text)
            }
        }
        return result.toString()
    }
}
