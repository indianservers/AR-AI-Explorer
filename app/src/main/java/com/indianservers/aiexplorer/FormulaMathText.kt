package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface FormulaLayoutNode
private data class FormulaSequence(val children: List<FormulaLayoutNode>) : FormulaLayoutNode
private data class FormulaGlyph(val value: String) : FormulaLayoutNode
private data class FormulaFraction(
    val numerator: FormulaLayoutNode,
    val denominator: FormulaLayoutNode,
) : FormulaLayoutNode
private data class FormulaRoot(
    val radicand: FormulaLayoutNode,
    val degree: FormulaLayoutNode?,
) : FormulaLayoutNode
private data class FormulaScript(
    val content: FormulaLayoutNode,
    val superscript: Boolean,
) : FormulaLayoutNode
private data class FormulaBinomial(
    val upper: FormulaLayoutNode,
    val lower: FormulaLayoutNode,
) : FormulaLayoutNode
private data class FormulaMatrix(val rows: List<List<FormulaLayoutNode>>) : FormulaLayoutNode

/**
 * Formula-library renderer for textbook notation. It keeps LaTeX as the source
 * of truth while presenting fractions, roots, scripts and matrices spatially.
 */
@Composable
internal fun FormulaLatexText(
    formula: String,
    color: Color,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
) {
    val layout = remember(formula) { FormulaLayoutParser(formula).parse() }
    Box(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = displayLatexFormula(formula) },
        contentAlignment = Alignment.CenterStart,
    ) {
        FormulaNode(layout, color, fontSize, fontWeight)
    }
}

@Composable
private fun FormulaNode(
    node: FormulaLayoutNode,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
) {
    when (node) {
        is FormulaGlyph -> Text(
            text = node.value,
            color = color,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
        )
        is FormulaSequence -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            node.children.forEach { FormulaNode(it, color, fontSize, fontWeight) }
        }
        is FormulaFraction -> Column(
            Modifier
                .width(IntrinsicSize.Max)
                .requiredWidthIn(min = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                FormulaNode(node.numerator, color, fontSize * .78f, fontWeight)
            }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx())
            }
            Box(Modifier.padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                FormulaNode(node.denominator, color, fontSize * .78f, fontWeight)
            }
        }
        is FormulaRoot -> Row(verticalAlignment = Alignment.CenterVertically) {
            node.degree?.let {
                Box(Modifier.padding(top = 1.dp), contentAlignment = Alignment.TopEnd) {
                    FormulaNode(it, color, fontSize * .48f, fontWeight)
                }
            }
            Text(
                "√",
                color = color,
                fontFamily = FontFamily.Serif,
                fontSize = fontSize * 1.22f,
                fontWeight = FontWeight.Normal,
            )
            Box(
                Modifier
                    .padding(top = 2.dp, end = 2.dp)
                    .drawBehind {
                        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.4.dp.toPx())
                    },
            ) {
                FormulaNode(node.radicand, color, fontSize * .88f, fontWeight)
            }
        }
        is FormulaScript -> Box(
            Modifier.padding(
                start = 1.dp,
                bottom = if (node.superscript) 11.dp else 0.dp,
                top = if (node.superscript) 0.dp else 11.dp,
            ),
        ) {
            FormulaNode(node.content, color, fontSize * .62f, fontWeight)
        }
        is FormulaBinomial -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("(", color = color, fontFamily = FontFamily.Serif, fontSize = fontSize * 1.35f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FormulaNode(node.upper, color, fontSize * .66f, fontWeight)
                FormulaNode(node.lower, color, fontSize * .66f, fontWeight)
            }
            Text(")", color = color, fontFamily = FontFamily.Serif, fontSize = fontSize * 1.35f)
        }
        is FormulaMatrix -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[", color = color, fontFamily = FontFamily.Serif, fontSize = fontSize * 1.7f)
            Column(
                Modifier.widthIn(min = 38.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                node.rows.forEach { cells ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        cells.forEach { cell ->
                            Box(Modifier.widthIn(min = 14.dp), contentAlignment = Alignment.Center) {
                                FormulaNode(cell, color, fontSize * .72f, fontWeight)
                            }
                        }
                    }
                }
            }
            Text("]", color = color, fontFamily = FontFamily.Serif, fontSize = fontSize * 1.7f)
        }
    }
}

private class FormulaLayoutParser(private val source: String) {
    private var index = 0

    fun parse(): FormulaLayoutNode = parseSequence()

    private fun parseSequence(stopAtBrace: Boolean = false): FormulaLayoutNode {
        val nodes = mutableListOf<FormulaLayoutNode>()
        val text = StringBuilder()
        fun flush() {
            if (text.isNotEmpty()) {
                nodes += FormulaGlyph(text.toString().replace(Regex("""\s+"""), " "))
                text.clear()
            }
        }
        while (index < source.length) {
            if (stopAtBrace && source[index] == '}') break
            when {
                source.startsWith("\\frac", index) -> {
                    flush()
                    index += 5
                    nodes += FormulaFraction(parseGroup(), parseGroup())
                }
                source.startsWith("\\sqrt", index) -> {
                    flush()
                    index += 5
                    val degree = if (source.getOrNull(index) == '[') parseBracketGroup() else null
                    nodes += FormulaRoot(parseGroup(), degree)
                }
                source.startsWith("\\binom", index) -> {
                    flush()
                    index += 6
                    nodes += FormulaBinomial(parseGroup(), parseGroup())
                }
                source.startsWith("\\begin{pmatrix}", index) ||
                    source.startsWith("\\begin{bmatrix}", index) -> {
                    flush()
                    nodes += parseMatrix()
                }
                source[index] == '^' || source[index] == '_' -> {
                    flush()
                    val superscript = source[index++] == '^'
                    val content = if (source.getOrNull(index) == '{') parseGroup() else {
                        FormulaGlyph(source.getOrNull(index++)?.toString().orEmpty())
                    }
                    nodes += FormulaScript(content, superscript)
                }
                source[index] == '{' -> {
                    flush()
                    nodes += parseGroup()
                }
                source[index] == '\\' -> {
                    flush()
                    parseCommand()?.let(nodes::add)
                }
                else -> text.append(source[index++])
            }
        }
        flush()
        return when (nodes.size) {
            0 -> FormulaGlyph("")
            1 -> nodes.single()
            else -> FormulaSequence(nodes)
        }
    }

    private fun parseGroup(): FormulaLayoutNode {
        while (source.getOrNull(index)?.isWhitespace() == true) index++
        if (source.getOrNull(index) != '{') {
            return FormulaGlyph(source.getOrNull(index++)?.toString().orEmpty())
        }
        index++
        val value = parseSequence(stopAtBrace = true)
        if (source.getOrNull(index) == '}') index++
        return value
    }

    private fun parseBracketGroup(): FormulaLayoutNode {
        index++
        val start = index
        while (index < source.length && source[index] != ']') index++
        val value = FormulaLayoutParser(source.substring(start, index)).parse()
        if (source.getOrNull(index) == ']') index++
        return value
    }

    private fun parseCommand(): FormulaLayoutNode? {
        index++
        if (source.getOrNull(index) == '\\') {
            index++
            return FormulaGlyph(" ")
        }
        val start = index
        while (source.getOrNull(index)?.isLetter() == true) index++
        val command = source.substring(start, index)
        if (command in setOf("left", "right", "quad", "qquad")) return null
        if (command in setOf(",", ";", "!", " ")) return FormulaGlyph(" ")
        if (command == "text" || command == "operatorname" || command == "mathrm" ||
            command == "mathbf" || command == "boldsymbol"
        ) {
            return parseGroup()
        }
        return FormulaGlyph(commandGlyphs[command] ?: extendedCommandGlyphs[command] ?: command)
    }

    private fun parseMatrix(): FormulaLayoutNode {
        val environment = if (source.startsWith("\\begin{pmatrix}", index)) "pmatrix" else "bmatrix"
        val begin = "\\begin{$environment}"
        val end = "\\end{$environment}"
        index += begin.length
        val endIndex = source.indexOf(end, index).takeIf { it >= 0 } ?: source.length
        val body = source.substring(index, endIndex)
        index = (endIndex + end.length).coerceAtMost(source.length)
        val rows = body.split("\\\\").map { row ->
            row.split('&').map { FormulaLayoutParser(it.trim()).parse() }
        }
        return FormulaMatrix(rows)
    }
}

private val extendedCommandGlyphs = mapOf(
    "Longleftrightarrow" to "\u21D4", "Leftrightarrow" to "\u21D4",
    "Rightarrow" to "\u21D2", "Leftarrow" to "\u21D0",
    "longrightarrow" to "\u27F6", "longleftarrow" to "\u27F5",
    "mid" to "\u2223", "nmid" to "\u2224", "varnothing" to "\u2205",
    "lfloor" to "\u230A", "rfloor" to "\u230B", "lceil" to "\u2308", "rceil" to "\u2309",
    "ldots" to "\u2026", "cdots" to "\u22EF", "perp" to "\u27C2", "parallel" to "\u2225",
    "forall" to "\u2200", "exists" to "\u2203", "oplus" to "\u2295", "otimes" to "\u2297",
    "sinh" to "sinh", "cosh" to "cosh", "tanh" to "tanh", "erf" to "erf",
    "min" to "min", "max" to "max", "limsup" to "lim sup", "liminf" to "lim inf",
)

private val commandGlyphs = mapOf(
    "pm" to "±", "mp" to "∓", "times" to "×", "cdot" to "·", "div" to "÷",
    "circ" to "°", "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥",
    "neq" to "≠", "approx" to "≈", "sim" to "∼", "equiv" to "≡", "to" to "→",
    "infty" to "∞", "cup" to "∪", "cap" to "∩", "subseteq" to "⊆",
    "subset" to "⊂", "in" to "∈", "pi" to "π", "theta" to "θ",
    "lambda" to "λ", "phi" to "φ", "alpha" to "α", "beta" to "β",
    "gamma" to "γ", "delta" to "δ", "Delta" to "Δ", "sigma" to "σ",
    "Sigma" to "Σ", "mu" to "μ", "rho" to "ρ", "omega" to "ω",
    "Omega" to "Ω", "ell" to "ℓ", "varepsilon" to "ε", "epsilon" to "ε",
    "sin" to "sin", "cos" to "cos", "tan" to "tan", "sec" to "sec",
    "csc" to "csc", "cot" to "cot", "log" to "log", "ln" to "ln",
    "lim" to "lim", "int" to "∫", "iint" to "∬", "iiint" to "∭",
    "sum" to "∑", "prod" to "∏", "nabla" to "∇", "partial" to "∂",
    "det" to "det", "vec" to "", "hat" to "", "bar" to "",
    "lVert" to "‖", "rVert" to "‖", "langle" to "⟨", "rangle" to "⟩",
)
