package com.indianservers.aiexplorer.input

data class NormalizedMathOcr(
    val rawText: String,
    val editableText: String,
    val changes: List<String>,
)

/**
 * Conservative OCR cleanup. It repairs typography and spacing while leaving
 * ambiguous characters editable instead of silently changing the mathematics.
 */
object MathOcrNormalizer {
    fun normalize(rawText: String): NormalizedMathOcr {
        var text = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
        val changes = mutableListOf<String>()

        fun replace(old: String, new: String, description: String) {
            if (old in text) {
                text = text.replace(old, new)
                changes += description
            }
        }

        replace("−", "-", "Normalized minus signs")
        replace("–", "-", "Normalized dash as minus")
        replace("×", "*", "Converted multiplication sign")
        replace("÷", "/", "Converted division sign")
        replace("²", "^2", "Expanded squared exponent")
        replace("³", "^3", "Expanded cubed exponent")
        replace("≤", "<=", "Expanded less-than-or-equal sign")
        replace("≥", ">=", "Expanded greater-than-or-equal sign")
        replace("π", "pi", "Expanded pi constant")

        val beforeWhitespace = text
        text = text
            .lineSequence()
            .map { line ->
                line.trim()
                    .replace(Regex("""[ \t]+"""), " ")
                    .replace(Regex("""\s*(<=|>=|[=+\-*/^(),<>])\s*"""), " $1 ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            }
            .filter(String::isNotBlank)
            .joinToString("\n")
        if (text != beforeWhitespace) changes += "Cleaned OCR spacing"

        val beforeFunctions = text
        text = text.replace(
            Regex("""\b(sin|cos|tan|ln|log|sqrt)\s+([a-zA-Z0-9.]+)""", RegexOption.IGNORE_CASE),
        ) { match -> "${match.groupValues[1]}(${match.groupValues[2]})" }
        if (text != beforeFunctions) changes += "Added clear function brackets"

        return NormalizedMathOcr(
            rawText = rawText,
            editableText = text,
            changes = changes.distinct(),
        )
    }
}
