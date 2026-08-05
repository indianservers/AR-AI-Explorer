package com.indianservers.aiexplorer

internal fun displayLatexFormula(source: String): String {
    val cleaned = source
        .replace("\\\\", "\\")
        .replace(Regex("""\bsqrt\(([^()]*)\)""")) { "\\sqrt{${it.groupValues[1]}}" }
        .replace(Regex("""\bpi\b"""), "\\pi")
        .replace(Regex("""\btheta\b"""), "\\theta")
        .replace(Regex("""\blambda\b"""), "\\lambda")
        .replace("*", "\\times")
        .replace(Regex("""\\(?:begin|end)\{[^}]*\}"""), "")
        .replace(Regex("""\\(?:left|right|,|;|!| )"""), "")
        .replace(Regex("""\\text\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\operatorname\{([^}]*)\}""")) { it.groupValues[1] }

    return cleaned
        .renderLatexFractions()
        .renderLatexRoots()
        .renderLatexCommands()
        .replace(Regex("""\bpi\b"""), PI)
        .replace("theta", THETA)
        .replace("lambda", LAMBDA)
        .renderScripts('^', superscripts)
        .renderScripts('_', subscripts)
        .replace("/", DIVIDE)
        .replace("{", "")
        .replace("}", "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private const val PI = "\u03c0"
private const val THETA = "\u03b8"
private const val LAMBDA = "\u03bb"
private const val DIVIDE = " \u00f7 "
private const val ROOT = "\u221a"

private val superscripts = mapOf(
    '0' to '\u2070', '1' to '\u00b9', '2' to '\u00b2', '3' to '\u00b3', '4' to '\u2074',
    '5' to '\u2075', '6' to '\u2076', '7' to '\u2077', '8' to '\u2078', '9' to '\u2079',
    '+' to '\u207a', '-' to '\u207b', '=' to '\u207c', '(' to '\u207d', ')' to '\u207e',
    'n' to '\u207f', 'i' to '\u2071', 'x' to '\u02e3', 'y' to '\u02b8', 'T' to '\u1d40',
)

private val subscripts = mapOf(
    '0' to '\u2080', '1' to '\u2081', '2' to '\u2082', '3' to '\u2083', '4' to '\u2084',
    '5' to '\u2085', '6' to '\u2086', '7' to '\u2087', '8' to '\u2088', '9' to '\u2089',
    '+' to '\u208a', '-' to '\u208b', '=' to '\u208c', '(' to '\u208d', ')' to '\u208e',
    'a' to '\u2090', 'e' to '\u2091', 'h' to '\u2095', 'i' to '\u1d62', 'j' to '\u2c7c',
    'k' to '\u2096', 'l' to '\u2097', 'm' to '\u2098', 'n' to '\u2099', 'o' to '\u2092',
    'p' to '\u209a', 'r' to '\u1d63', 's' to '\u209b', 't' to '\u209c', 'u' to '\u1d64',
    'v' to '\u1d65', 'x' to '\u2093',
)

private fun String.renderLatexFractions(): String {
    var text = this
    while (true) {
        val start = text.indexOf("\\frac")
        if (start < 0) return text
        val numerator = text.latexGroupAfter(start + 5) ?: return text.replace("\\frac", "")
        val denominator = text.latexGroupAfter(numerator.nextIndex) ?: return text.replace("\\frac", "")
        val before = text.substring(0, start)
        val after = text.substring(denominator.nextIndex)
        text = before +
            numerator.value.renderLatexInline().asFractionPart() +
            DIVIDE +
            denominator.value.renderLatexInline().asFractionPart() +
            after
    }
}

private fun String.renderLatexRoots(): String {
    var text = this
    while (true) {
        val start = text.indexOf("\\sqrt")
        if (start < 0) return text
        val degree = if (text.getOrNull(start + 5) == '[') text.bracketGroupAfter(start + 5) else null
        val groupStart = degree?.nextIndex ?: (start + 5)
        val radicand = text.latexGroupAfter(groupStart) ?: return text.replace("\\sqrt", ROOT)
        val before = text.substring(0, start)
        val after = text.substring(radicand.nextIndex)
        val rootPrefix = degree?.value?.renderLatexInline()?.toSuperscript().orEmpty()
        text = before + rootPrefix + ROOT + "(" + radicand.value.renderLatexInline() + ")" + after
    }
}

private fun String.renderLatexCommands(): String {
    val replacements = linkedMapOf(
        "\\pm" to "\u00b1", "\\mp" to "\u2213", "\\times" to "\u00d7", "\\cdot" to "\u00b7", "\\div" to "\u00f7",
        "\\circ" to "\u00b0", "\\%" to "%",
        "\\leq" to "\u2264", "\\le" to "\u2264", "\\geq" to "\u2265", "\\ge" to "\u2265", "\\neq" to "\u2260",
        "\\approx" to "\u2248", "\\sim" to "\u223c", "\\equiv" to "\u2261", "\\to" to "\u2192", "\\infty" to "\u221e",
        "\\cup" to "\u222a", "\\cap" to "\u2229", "\\subseteq" to "\u2286", "\\subset" to "\u2282", "\\in" to "\u2208",
        "\\pi" to PI, "\\theta" to THETA, "\\lambda" to LAMBDA, "\\phi" to "\u03c6", "\\alpha" to "\u03b1",
        "\\beta" to "\u03b2", "\\gamma" to "\u03b3", "\\delta" to "\u03b4", "\\Delta" to "\u0394",
        "\\sigma" to "\u03c3", "\\Sigma" to "\u03a3", "\\mu" to "\u03bc", "\\rho" to "\u03c1",
        "\\omega" to "\u03c9", "\\Omega" to "\u03a9", "\\ell" to "\u2113",
        "\\varepsilon" to "\u03b5", "\\epsilon" to "\u03b5", "\\hat" to "", "\\bar" to "",
        "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan", "\\sec" to "sec", "\\csc" to "csc",
        "\\cot" to "cot", "\\log" to "log", "\\ln" to "ln", "\\lim" to "lim", "\\int" to "\u222b",
        "\\sum" to "\u03a3", "\\prod" to "\u03a0", "\\nabla" to "\u2207", "\\partial" to "\u2202",
        "\\vec" to "", "\\lVert" to "\u2016", "\\rVert" to "\u2016", "\\langle" to "\u27e8", "\\rangle" to "\u27e9",
        "\\tilde" to "", "\\mathbf" to "", "\\mathrm" to "", "\\begin" to "", "\\end" to "",
    )
    var text = this
    replacements.forEach { (raw, pretty) -> text = text.replace(raw, pretty) }
    return text
        .replace("<=>", "\u21d4")
        .replace("<=", "\u2264")
        .replace(">=", "\u2265")
        .replace("!=", "\u2260")
        .replace("->", "\u2192")
}

private fun String.renderScripts(marker: Char, alphabet: Map<Char, Char>): String {
    var text = this
    while (true) {
        val index = text.indexOf(marker)
        if (index < 0 || index == text.lastIndex) return text
        val group = text.latexGroupAfter(index + 1)
        if (group != null) {
            text = text.substring(0, index) + group.value.renderLatexInline().mapScript(alphabet) + text.substring(group.nextIndex)
            continue
        }
        val value = text.getOrNull(index + 1)?.toString().orEmpty()
        text = text.substring(0, index) + value.mapScript(alphabet) + text.substring((index + 2).coerceAtMost(text.length))
    }
}

private fun String.renderLatexInline() =
    renderLatexFractions()
        .renderLatexRoots()
        .renderLatexCommands()
        .renderScripts('^', superscripts)
        .renderScripts('_', subscripts)
        .replace("/", DIVIDE)
        .replace("{", "")
        .replace("}", "")
        .trim()

private fun String.asFractionPart(): String =
    if (length <= 3 || all { it.isLetterOrDigit() || it in simpleMathSymbols }) this else "($this)"

private val simpleMathSymbols = setOf(
    '\u2080', '\u2081', '\u2082', '\u2083', '\u2084', '\u2085', '\u2086', '\u2087', '\u2088', '\u2089',
    '\u2070', '\u00b9', '\u00b2', '\u00b3', '\u2074', '\u2075', '\u2076', '\u2077', '\u2078', '\u2079',
    PI.single(), THETA.single(), LAMBDA.single(), '\u03b1', '\u03b2', '\u03b3', '\u03c3', '\u03bc',
)

private fun String.mapScript(alphabet: Map<Char, Char>) = map { alphabet[it] ?: it }.joinToString("")

private fun String.toSuperscript() = mapScript(superscripts)

private data class LatexGroup(val value: String, val nextIndex: Int)

private fun String.latexGroupAfter(start: Int): LatexGroup? = delimitedGroupAfter(start, '{', '}')

private fun String.bracketGroupAfter(start: Int): LatexGroup? = delimitedGroupAfter(start, '[', ']')

private fun String.delimitedGroupAfter(start: Int, open: Char, close: Char): LatexGroup? {
    var index = start
    while (index < length && this[index].isWhitespace()) index++
    if (getOrNull(index) != open) return null
    var depth = 0
    for (cursor in index until length) {
        when (this[cursor]) {
            open -> depth++
            close -> {
                depth--
                if (depth == 0) return LatexGroup(substring(index + 1, cursor), cursor + 1)
            }
        }
    }
    return null
}
