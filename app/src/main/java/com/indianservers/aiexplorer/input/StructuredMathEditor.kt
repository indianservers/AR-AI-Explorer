package com.indianservers.aiexplorer.input

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

enum class MathInputMode {
    BASELINE,
    SUPERSCRIPT,
    SUBSCRIPT,
    LOG_BASE,
    FUNCTION_ARGUMENT,
    NUMERATOR,
    DENOMINATOR,
    ROOT_INDEX,
    RADICAND,
    MATRIX_CELL,
    BRACKET_CONTENT,
}

data class MathEditableSlot(
    val mode: MathInputMode,
    val contentStart: Int,
    val contentEnd: Int,
    val sourceEnd: Int,
    val owner: String,
) {
    val isPlaceholder: Boolean get() = contentStart == contentEnd
}

sealed interface StructuredMathNode {
    val sourceStart: Int
    val sourceEnd: Int
}

data class MathSequenceNode(
    val children: List<StructuredMathNode>,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathTextNode(
    val text: String,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathScriptNode(
    val content: MathSequenceNode,
    val mode: MathInputMode,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathFunctionNode(
    val name: String,
    val argument: MathSequenceNode,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathLogNode(
    val base: MathSequenceNode?,
    val argument: MathSequenceNode,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathFractionNode(
    val numerator: MathSequenceNode,
    val denominator: MathSequenceNode,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathRootNode(
    val index: MathSequenceNode?,
    val radicand: MathSequenceNode,
    override val sourceStart: Int,
    override val sourceEnd: Int,
) : StructuredMathNode

data class MathCursor(
    val sourceOffset: Int,
    val mode: MathInputMode,
)

data class MathEditorState(
    val root: MathSequenceNode,
    val cursor: MathCursor,
    val selection: TextRange?,
    val activeInputMode: MathInputMode,
    val structuredExpression: String,
    val serializedExpression: String,
    val latexExpression: String,
)

/**
 * Keeps the visual editor syntax separate from the parser contract. Most
 * notation is identical; logbase(base, argument) is reordered only when it
 * leaves or enters the editor.
 */
object StructuredMathCodec {
    fun fromParser(value: TextFieldValue): TextFieldValue {
        val converted = convertLogCallsToEditor(value.text)
        if (converted == value.text) return value
        return TextFieldValue(converted, TextRange(converted.length))
    }

    fun toParser(value: TextFieldValue): TextFieldValue {
        val converted = convertNthRootsToParser(
            convertCubeRootsToParser(convertLogBaseToParser(value.text)),
        )
        if (converted == value.text) return value
        return TextFieldValue(converted, TextRange(converted.length))
    }

    fun state(value: TextFieldValue): MathEditorState {
        val root = StructuredMathParser.parse(value.text)
        val mode = StructuredMathEditing.modeAt(value.text, value.selection.end)
        return MathEditorState(
            root = root,
            cursor = MathCursor(value.selection.end, mode),
            selection = value.selection.takeUnless { it.collapsed },
            activeInputMode = mode,
            structuredExpression = value.text,
            serializedExpression = toParser(value).text,
            latexExpression = toLatex(value.text),
        )
    }

    fun toLatex(source: String): String {
        var value = source
        value = Regex("""\^\(([^()]*)\)""").replace(value) { "^{${it.groupValues[1]}}" }
        value = Regex("""_\(([^()]*)\)""").replace(value) { "_{${it.groupValues[1]}}" }
        value = Regex("""logbase\(([^,]*),([^()]*)\)""").replace(value) {
            """\log_{${it.groupValues[1]}}\left(${it.groupValues[2]}\right)"""
        }
        value = Regex("""sqrt\(([^()]*)\)""").replace(value) {
            """\sqrt{${it.groupValues[1]}}"""
        }
        value = Regex("""cbrt\(([^()]*)\)""").replace(value) {
            """\sqrt[3]{${it.groupValues[1]}}"""
        }
        value = Regex("""nthroot\(([^,]*),([^()]*)\)""").replace(value) {
            """\sqrt[${it.groupValues[1]}]{${it.groupValues[2]}}"""
        }
        value = Regex("""\(([^()]*)\)/\(([^()]*)\)""").replace(value) {
            """\frac{${it.groupValues[1]}}{${it.groupValues[2]}}"""
        }
        return value
    }

    private fun convertLogCallsToEditor(source: String): String =
        rewriteCalls(source, "log") { args ->
            if (args.size == 2) "logbase(${args[1]},${args[0]})" else null
        }

    private fun convertLogBaseToParser(source: String): String =
        rewriteCalls(source, "logbase") { args ->
            if (args.size == 2) "log(${args[1]},${args[0]})" else null
        }

    private fun convertCubeRootsToParser(source: String): String =
        rewriteCalls(source, "cbrt") { args ->
            if (args.size == 1) "(${convertCubeRootsToParser(args[0])})^(1/3)" else null
        }

    private fun convertNthRootsToParser(source: String): String =
        rewriteCalls(source, "nthroot") { args ->
            if (args.size == 2) {
                "(${convertNthRootsToParser(args[1])})^(1/(${convertNthRootsToParser(args[0])}))"
            } else {
                null
            }
        }

    private fun rewriteCalls(
        source: String,
        function: String,
        replacement: (List<String>) -> String?,
    ): String {
        val output = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            if (!source.regionMatches(index, "$function(", 0, function.length + 1)) {
                output.append(source[index++])
                continue
            }
            val open = index + function.length
            val close = matchingParen(source, open)
            if (close < 0) {
                output.append(source[index++])
                continue
            }
            val arguments = splitTopLevel(source.substring(open + 1, close))
            val rewritten = replacement(arguments)
            if (rewritten == null) {
                output.append(source, index, close + 1)
            } else {
                output.append(rewritten)
            }
            index = close + 1
        }
        return output.toString()
    }
}

object StructuredMathEditing {
    fun modeAt(source: String, cursor: Int): MathInputMode {
        val safeCursor = cursor.coerceIn(0, source.length)
        return activeRegion(source, safeCursor)?.mode
            ?: MathInputMode.BASELINE
    }

    fun editableSlots(source: String): List<MathEditableSlot> =
        structuralRegions(source)
            .distinctBy { listOf(it.mode, it.contentStart, it.contentEnd, it.owner) }
            .map { MathEditableSlot(it.mode, it.contentStart, it.contentEnd, it.sourceEnd, it.owner) }

    fun activeSlot(source: String, cursor: Int): MathEditableSlot? =
        activeRegion(source, cursor)?.let {
            MathEditableSlot(it.mode, it.contentStart, it.contentEnd, it.sourceEnd, it.owner)
        }

    fun toggleSuperscript(value: TextFieldValue): TextFieldValue =
        toggleScript(value, '^', MathInputMode.SUPERSCRIPT)

    fun toggleSubscript(value: TextFieldValue): TextFieldValue =
        toggleScript(value, '_', MathInputMode.SUBSCRIPT)

    fun toggleFraction(value: TextFieldValue, template: MathKey): TextFieldValue {
        val active = activeRegion(value)
        if (active?.owner?.startsWith("fraction:") == true) {
            val regions = structuralRegions(value.text).filter { it.owner == active.owner }
            return when (active.mode) {
                MathInputMode.NUMERATOR -> value.copy(
                    selection = TextRange(regions.first { it.mode == MathInputMode.DENOMINATOR }.contentStart),
                )
                MathInputMode.DENOMINATOR -> value.copy(selection = TextRange(active.sourceEnd))
                else -> value
            }
        }
        return MathTextEditing.insert(value, template)
    }

    fun toggleRoot(value: TextFieldValue, template: MathKey): TextFieldValue {
        val active = activeRegion(value)
        return if (active?.owner?.startsWith("sqrt:") == true) {
            value.copy(selection = TextRange(active.sourceEnd))
        } else {
            MathTextEditing.insert(value, template)
        }
    }

    fun toggleCubeRoot(value: TextFieldValue, template: MathKey): TextFieldValue {
        val active = activeRegion(value)
        return if (active?.owner?.startsWith("cbrt:") == true) {
            value.copy(selection = TextRange(active.sourceEnd))
        } else {
            MathTextEditing.insert(value, template)
        }
    }

    fun toggleNthRoot(value: TextFieldValue, template: MathKey): TextFieldValue {
        val active = activeRegion(value)
        if (active?.owner?.startsWith("nthroot:") == true) {
            val regions = structuralRegions(value.text).filter {
                it.owner.substringBeforeLast(':') == active.owner.substringBeforeLast(':')
            }
            return when (active.mode) {
                MathInputMode.ROOT_INDEX -> value.copy(
                    selection = TextRange(regions.first { it.mode == MathInputMode.RADICAND }.contentStart),
                )
                MathInputMode.RADICAND -> value.copy(selection = TextRange(active.sourceEnd))
                else -> value
            }
        }
        return MathTextEditing.insert(value, template)
    }

    fun toggleLogBase(value: TextFieldValue): TextFieldValue {
        val active = activeRegion(value)
        if (active?.owner?.startsWith("logbase:") == true) {
            val regions = structuralRegions(value.text).filter { it.owner == active.owner }
            return when (active.mode) {
                MathInputMode.LOG_BASE -> value.copy(
                    selection = TextRange(regions.first { it.mode == MathInputMode.FUNCTION_ARGUMENT }.contentStart),
                )
                MathInputMode.FUNCTION_ARGUMENT -> value.copy(selection = TextRange(active.sourceEnd))
                else -> value
            }
        }
        return MathTextEditing.insert(value, MathKey("log base", "logbase(,)", 2))
    }

    fun isFractionActive(source: String, cursor: Int): Boolean =
        activeRegion(source, cursor)?.owner?.startsWith("fraction:") == true

    fun isRootActive(source: String, cursor: Int): Boolean =
        activeRegion(source, cursor)?.owner?.startsWith("sqrt:") == true

    fun isCubeRootActive(source: String, cursor: Int): Boolean =
        activeRegion(source, cursor)?.owner?.startsWith("cbrt:") == true

    fun isNthRootActive(source: String, cursor: Int): Boolean =
        activeRegion(source, cursor)?.owner?.startsWith("nthroot:") == true

    fun isLogBaseActive(source: String, cursor: Int): Boolean =
        activeRegion(source, cursor)?.owner?.startsWith("logbase:") == true

    fun move(value: TextFieldValue, amount: Int): TextFieldValue {
        if (!value.selection.collapsed) return MathTextEditing.move(value, amount)
        val cursor = value.selection.end
        if (amount > 0 && cursor < value.text.length && value.text[cursor] == ')') {
            return value.copy(selection = TextRange(cursor + 1))
        }
        return MathTextEditing.move(value, amount)
    }

    fun backspace(value: TextFieldValue): TextFieldValue {
        if (!value.selection.collapsed) return MathTextEditing.backspace(value)
        val cursor = value.selection.end
        if (cursor >= 2 && cursor < value.text.length) {
            val prefix = value.text.substring(cursor - 2, cursor)
            if ((prefix == "^(" || prefix == "_(") && value.text[cursor] == ')') {
                val next = value.text.removeRange(cursor - 2, cursor + 1)
                return TextFieldValue(next, TextRange(cursor - 2))
            }
        }
        return MathTextEditing.backspace(value)
    }

    private fun toggleScript(value: TextFieldValue, marker: Char, mode: MathInputMode): TextFieldValue {
        val active = activeRegion(value)
        if (active?.mode == mode) {
            return value.copy(selection = TextRange(active.sourceEnd))
        }
        val start = value.selection.min
        val end = value.selection.max
        val selected = value.text.substring(start, end)
        val insertion = "$marker($selected)"
        val next = value.text.replaceRange(start, end, insertion)
        val caret = if (selected.isEmpty()) start + 2 else start + insertion.length - 1
        return TextFieldValue(next, TextRange(caret))
    }

    private data class Region(
        val mode: MathInputMode,
        val contentStart: Int,
        val contentEnd: Int,
        val sourceEnd: Int,
        val owner: String,
    )

    private fun structuralRegions(source: String): List<Region> {
        val result = mutableListOf<Region>()
        var index = 0
        while (index < source.length - 1) {
            val mode = when {
                source.startsWith("^(", index) -> MathInputMode.SUPERSCRIPT
                source.startsWith("_(", index) -> MathInputMode.SUBSCRIPT
                else -> null
            }
            if (mode != null) {
                val open = index + 1
                val close = matchingParen(source, open)
                if (close >= 0) {
                    result += Region(mode, open + 1, close, close + 1, "script:$index")
                    index = open + 1
                    continue
                }
            }
            if (source.startsWith("logbase(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                if (close >= 0) {
                    val comma = topLevelComma(source, open + 1, close)
                    if (comma >= 0) {
                        val owner = "logbase:$index"
                        result += Region(MathInputMode.LOG_BASE, open + 1, comma, close + 1, owner)
                        result += Region(MathInputMode.FUNCTION_ARGUMENT, comma + 1, close, close + 1, owner)
                    }
                }
            }
            if (source.startsWith("sqrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close >= 0) {
                    result += Region(MathInputMode.RADICAND, open + 1, close, close + 1, "sqrt:$index")
                }
            }
            if (source.startsWith("cbrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close >= 0) {
                    result += Region(MathInputMode.RADICAND, open + 1, close, close + 1, "cbrt:$index")
                }
            }
            if (source.startsWith("nthroot(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                if (close >= 0) {
                    val comma = topLevelComma(source, open + 1, close)
                    if (comma >= 0) {
                        val owner = "nthroot:$index"
                        result += Region(MathInputMode.ROOT_INDEX, open + 1, comma, close + 1, "$owner:index")
                        result += Region(MathInputMode.RADICAND, comma + 1, close, close + 1, "$owner:radicand")
                    }
                }
            }
            val functionName = functionNameAt(source, index)
            if (functionName in structuredMultiArgumentFunctions) {
                val open = index + functionName.orEmpty().length
                val close = matchingParen(source, open)
                if (close >= 0) {
                    topLevelArgumentRanges(source, open + 1, close).forEachIndexed { argumentIndex, range ->
                        result += Region(
                            MathInputMode.FUNCTION_ARGUMENT,
                            range.first,
                            range.second,
                            close + 1,
                            "$functionName:$index:$argumentIndex",
                        )
                    }
                }
            }
            if (source[index] == '(') {
                val numeratorClose = matchingParen(source, index)
                val denominatorOpen = numeratorClose + 2
                if (numeratorClose >= 0 &&
                    source.getOrNull(numeratorClose + 1) == '/' &&
                    source.getOrNull(denominatorOpen) == '('
                ) {
                    val denominatorClose = matchingParen(source, denominatorOpen)
                    if (denominatorClose >= 0) {
                        val owner = "fraction:$index"
                        result += Region(
                            MathInputMode.NUMERATOR,
                            index + 1,
                            numeratorClose,
                            denominatorClose + 1,
                            owner,
                        )
                        result += Region(
                            MathInputMode.DENOMINATOR,
                            denominatorOpen + 1,
                            denominatorClose,
                            denominatorClose + 1,
                            owner,
                        )
                    }
                }
                val close = numeratorClose
                val functionName = functionNameBefore(source, index)
                val ownedByStructure =
                    source.getOrNull(index - 1) in setOf('^', '_') ||
                        functionName in setOf("sqrt", "cbrt", "nthroot", "logbase") ||
                        functionName in structuredMultiArgumentFunctions ||
                        fractionStartsAt(source, index) ||
                        source.getOrNull(index - 1) == '/'
                if (close >= 0 && !ownedByStructure) {
                    result += Region(
                        mode = if (functionName != null) MathInputMode.FUNCTION_ARGUMENT else MathInputMode.BRACKET_CONTENT,
                        contentStart = index + 1,
                        contentEnd = close,
                        sourceEnd = close + 1,
                        owner = if (functionName != null) "function:$functionName:$index" else "bracket:$index",
                    )
                }
            }
            index++
        }
        result += matrixCellRegions(source)
        return result
    }

    private fun activeRegion(value: TextFieldValue): Region =
        activeRegion(value.text, value.selection.end)
            ?: Region(MathInputMode.BASELINE, 0, value.text.length, value.text.length, "baseline")

    private fun activeRegion(source: String, cursor: Int): Region? {
        val safeCursor = cursor.coerceIn(0, source.length)
        return structuralRegions(source)
            .filter { safeCursor in it.contentStart..it.contentEnd }
            .maxWithOrNull(
                compareBy<Region> { it.contentStart }
                    .thenBy { -it.contentEnd }
                    .thenBy { slotPriority(it.mode) },
            )
    }

    private fun slotPriority(mode: MathInputMode): Int = when (mode) {
        MathInputMode.NUMERATOR,
        MathInputMode.DENOMINATOR,
        MathInputMode.SUPERSCRIPT,
        MathInputMode.SUBSCRIPT,
        MathInputMode.LOG_BASE,
        MathInputMode.RADICAND,
        MathInputMode.MATRIX_CELL,
        -> 3
        MathInputMode.FUNCTION_ARGUMENT,
        MathInputMode.BRACKET_CONTENT,
        -> 2
        else -> 1
    }

    private fun matrixCellRegions(source: String): List<Region> {
        val regions = mutableListOf<Region>()
        var rowStart = 0
        while (rowStart < source.length) {
            if (source[rowStart] != '[') {
                rowStart++
                continue
            }
            val rowEnd = matchingSquareBracket(source, rowStart)
            if (rowEnd < 0 || source.substring(rowStart + 1, rowEnd).contains('[')) {
                rowStart++
                continue
            }
            val owner = "matrix-row:$rowStart"
            var cellStart = rowStart + 1
            var cursor = cellStart
            var roundDepth = 0
            while (cursor <= rowEnd) {
                val character = source.getOrNull(cursor)
                when (character) {
                    '(' -> roundDepth++
                    ')' -> roundDepth--
                    ',', ']' -> if (roundDepth == 0) {
                        regions += Region(
                            mode = MathInputMode.MATRIX_CELL,
                            contentStart = cellStart,
                            contentEnd = cursor,
                            sourceEnd = rowEnd + 1,
                            owner = "$owner:$cellStart",
                        )
                        cellStart = cursor + 1
                    }
                }
                cursor++
            }
            rowStart = rowEnd + 1
        }
        return regions
    }

    private fun functionNameBefore(source: String, open: Int): String? {
        var start = open
        while (start > 0 && source[start - 1].isLetter()) start--
        return source.substring(start, open).takeIf { it.isNotEmpty() }
    }

    private fun fractionStartsAt(source: String, open: Int): Boolean {
        val numeratorClose = matchingParen(source, open)
        return numeratorClose >= 0 &&
            source.getOrNull(numeratorClose + 1) == '/' &&
            source.getOrNull(numeratorClose + 2) == '('
    }
}

object StructuredMathParser {
    fun parse(source: String): MathSequenceNode = parseRange(source, 0, source.length)

    private fun parseRange(source: String, start: Int, end: Int): MathSequenceNode {
        val children = mutableListOf<StructuredMathNode>()
        var textStart = start
        var index = start
        fun flush(until: Int) {
            if (until > textStart) children += MathTextNode(source.substring(textStart, until), textStart, until)
        }
        while (index < end) {
            val fraction = fractionRangeAt(source, index, end)
            if (fraction != null) {
                flush(index)
                children += MathFractionNode(
                    numerator = parseRange(source, fraction.numeratorStart, fraction.numeratorEnd),
                    denominator = parseRange(source, fraction.denominatorStart, fraction.denominatorEnd),
                    sourceStart = index,
                    sourceEnd = fraction.sourceEnd,
                )
                index = fraction.sourceEnd
                textStart = index
                continue
            }
            val markerMode = when {
                source.startsWith("^(", index) -> MathInputMode.SUPERSCRIPT
                source.startsWith("_(", index) -> MathInputMode.SUBSCRIPT
                else -> null
            }
            if (markerMode != null) {
                val open = index + 1
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    flush(index)
                    children += MathScriptNode(parseRange(source, open + 1, close), markerMode, index, close + 1)
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            if (source.startsWith("logbase(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                val comma = if (close >= 0) topLevelComma(source, open + 1, close) else -1
                if (close in (open + 1)..end && comma >= 0) {
                    flush(index)
                    children += MathLogNode(
                        base = parseRange(source, open + 1, comma),
                        argument = parseRange(source, comma + 1, close),
                        sourceStart = index,
                        sourceEnd = close + 1,
                    )
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            if (source.startsWith("sqrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    flush(index)
                    children += MathRootNode(
                        index = null,
                        radicand = parseRange(source, open + 1, close),
                        sourceStart = index,
                        sourceEnd = close + 1,
                    )
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            if (source.startsWith("cbrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    flush(index)
                    children += MathRootNode(
                        index = MathSequenceNode(
                            children = listOf(MathTextNode("3", open, open)),
                            sourceStart = open,
                            sourceEnd = open,
                        ),
                        radicand = parseRange(source, open + 1, close),
                        sourceStart = index,
                        sourceEnd = close + 1,
                    )
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            if (source.startsWith("nthroot(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                val comma = if (close >= 0) topLevelComma(source, open + 1, close) else -1
                if (close in (open + 1)..end && comma >= 0) {
                    flush(index)
                    children += MathRootNode(
                        index = parseRange(source, open + 1, comma),
                        radicand = parseRange(source, comma + 1, close),
                        sourceStart = index,
                        sourceEnd = close + 1,
                    )
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            val name = functionNameAt(source, index)
            if (name != null) {
                val open = index + name.length
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    flush(index)
                    val argument = parseRange(source, open + 1, close)
                    children += MathFunctionNode(name, argument, index, close + 1)
                    index = close + 1
                    textStart = index
                    continue
                }
            }
            index++
        }
        flush(end)
        return MathSequenceNode(children, start, end)
    }

    private data class FractionRange(
        val numeratorStart: Int,
        val numeratorEnd: Int,
        val denominatorStart: Int,
        val denominatorEnd: Int,
        val sourceEnd: Int,
    )

    private fun fractionRangeAt(source: String, start: Int, end: Int): FractionRange? {
        if (source.getOrNull(start) != '(') return null
        val numeratorClose = matchingParen(source, start)
        if (numeratorClose !in (start + 1) until end ||
            source.getOrNull(numeratorClose + 1) != '/' ||
            source.getOrNull(numeratorClose + 2) != '('
        ) return null
        val denominatorOpen = numeratorClose + 2
        val denominatorClose = matchingParen(source, denominatorOpen)
        if (denominatorClose !in (denominatorOpen + 1)..end) return null
        return FractionRange(
            numeratorStart = start + 1,
            numeratorEnd = numeratorClose,
            denominatorStart = denominatorOpen + 1,
            denominatorEnd = denominatorClose,
            sourceEnd = denominatorClose + 1,
        )
    }
}

class StructuredMathVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        StructuredMathVisualLayout.render(text.text)
}

object StructuredMathVisualLayout {
    private val scriptStyle = SpanStyle(
        baselineShift = BaselineShift.Superscript,
        fontSize = 0.72.em,
        fontWeight = FontWeight.SemiBold,
    )
    private val subscriptStyle = SpanStyle(
        baselineShift = BaselineShift.Subscript,
        fontSize = 0.72.em,
        fontWeight = FontWeight.SemiBold,
    )
    private val slotStyle = SpanStyle(
        color = IntentMathPalette.Command,
        fontSize = 0.72.em,
        fontWeight = FontWeight.Bold,
    )
    private val multiplicationStyle = SpanStyle(
        color = IntentMathPalette.Operator,
        fontSize = 1.18.em,
        fontWeight = FontWeight.Bold,
    )

    fun render(source: String): TransformedText {
        val builder = VisualBuilder(source)
        renderRange(source, 0, source.length, builder, null)
        return builder.build()
    }

    private fun renderRange(
        source: String,
        start: Int,
        end: Int,
        output: VisualBuilder,
        inheritedStyle: SpanStyle?,
    ) {
        var index = start
        while (index < end) {
            val determinantEnd = renderDeterminantAt(source, index, end, output, inheritedStyle)
            if (determinantEnd != null) {
                index = determinantEnd
                continue
            }
            val matrixEnd = renderMatrixAt(source, index, end, output, inheritedStyle)
            if (matrixEnd != null) {
                index = matrixEnd
                continue
            }
            val fraction = simpleFractionAt(source, index, end)
            if (fraction != null) {
                output.skipTo(fraction.numeratorStart)
                if (fraction.numeratorStart == fraction.numeratorEnd) {
                    output.appendReplacement(fraction.numeratorStart, fraction.numeratorEnd, "□", scriptStyle.merge(slotStyle))
                } else {
                    renderRange(source, fraction.numeratorStart, fraction.numeratorEnd, output, scriptStyle)
                }
                output.skipTo(fraction.slash)
                output.appendReplacement(fraction.slash, fraction.slash + 1, "⁄", inheritedStyle)
                output.skipTo(fraction.denominatorStart)
                if (fraction.denominatorStart == fraction.denominatorEnd) {
                    output.appendReplacement(fraction.denominatorStart, fraction.denominatorEnd, "□", subscriptStyle.merge(slotStyle))
                } else {
                    renderRange(source, fraction.denominatorStart, fraction.denominatorEnd, output, subscriptStyle)
                }
                output.skipTo(fraction.sourceEnd)
                index = fraction.sourceEnd
                continue
            }
            val scriptMode = when {
                source.startsWith("^(", index) -> MathInputMode.SUPERSCRIPT
                source.startsWith("_(", index) -> MathInputMode.SUBSCRIPT
                else -> null
            }
            if (scriptMode != null) {
                val open = index + 1
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    output.skipTo(open + 1)
                    val style = if (scriptMode == MathInputMode.SUPERSCRIPT) scriptStyle else subscriptStyle
                    if (close == open + 1) {
                        output.appendReplacement(open + 1, close, "□", style.merge(slotStyle))
                    } else {
                        renderRange(source, open + 1, close, output, style)
                    }
                    output.skipTo(close + 1)
                    index = close + 1
                    continue
                }
            }
            if ((source[index] == '^' || source[index] == '_') && index + 1 < end) {
                val style = if (source[index] == '^') scriptStyle else subscriptStyle
                output.skipTo(index + 1)
                output.appendReplacement(index + 1, index + 2, source[index + 1].toString(), style)
                index += 2
                continue
            }
            if (source.startsWith("logbase(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    val comma = topLevelComma(source, open + 1, close)
                    if (comma >= 0) {
                        output.appendReplacement(index, open + 1, "log", inheritedStyle)
                        val base = source.substring(open + 1, comma)
                        output.appendReplacement(open + 1, comma, base.ifEmpty { "□" }, subscriptStyle)
                        output.appendReplacement(comma, comma + 1, "(", inheritedStyle)
                        val argument = source.substring(comma + 1, close)
                        if (argument.isEmpty()) {
                            output.appendReplacement(comma + 1, close, "□", slotStyle)
                        } else {
                            renderRange(source, comma + 1, close, output, inheritedStyle)
                        }
                        output.appendReplacement(close, close + 1, ")", inheritedStyle)
                        index = close + 1
                        continue
                    }
                }
            }
            if (source.startsWith("sqrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    output.appendReplacement(index, open + 1, "√", inheritedStyle)
                    output.beginOverline()
                    if (close == open + 1) {
                        output.appendReplacement(open + 1, close, "□", slotStyle)
                    } else {
                        renderRange(source, open + 1, close, output, inheritedStyle)
                    }
                    output.endOverline()
                    output.skipTo(close + 1)
                    index = close + 1
                    continue
                }
            }
            if (source.startsWith("cbrt(", index)) {
                val open = index + 4
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    output.appendReplacement(index, open + 1, "∛", inheritedStyle)
                    output.beginOverline()
                    if (close == open + 1) {
                        output.appendReplacement(open + 1, close, "□", slotStyle)
                    } else {
                        renderRange(source, open + 1, close, output, inheritedStyle)
                    }
                    output.endOverline()
                    output.skipTo(close + 1)
                    index = close + 1
                    continue
                }
            }
            if (source.startsWith("nthroot(", index)) {
                val open = index + 7
                val close = matchingParen(source, open)
                val comma = if (close >= 0) topLevelComma(source, open + 1, close) else -1
                if (close in (open + 1)..end && comma >= 0) {
                    output.skipTo(open + 1)
                    if (comma == open + 1) {
                        output.appendReplacement(open + 1, comma, "□", scriptStyle.merge(slotStyle))
                    } else {
                        renderRange(source, open + 1, comma, output, scriptStyle)
                    }
                    output.appendReplacement(comma, comma + 1, "√", inheritedStyle)
                    output.beginOverline()
                    if (close == comma + 1) {
                        output.appendReplacement(comma + 1, close, "□", slotStyle)
                    } else {
                        renderRange(source, comma + 1, close, output, inheritedStyle)
                    }
                    output.endOverline()
                    output.skipTo(close + 1)
                    index = close + 1
                    continue
                }
            }
            if (source.startsWith("exp(", index)) {
                val open = index + 3
                val close = matchingParen(source, open)
                if (close in (open + 1)..end) {
                    output.appendReplacement(index, open + 1, "e", inheritedStyle)
                    if (close == open + 1) {
                        output.appendReplacement(open + 1, close, "□", scriptStyle.merge(slotStyle))
                    } else {
                        renderRange(source, open + 1, close, output, scriptStyle)
                    }
                    output.skipTo(close + 1)
                    index = close + 1
                    continue
                }
            }
            val calculusEnd = renderCalculusCall(source, index, end, output, inheritedStyle)
            if (calculusEnd != null) {
                index = calculusEnd
                continue
            }
            val emptyFunction = knownFunctionAt(source, index)?.takeIf {
                source.startsWith("$it()", index)
            }
            if (emptyFunction != null) {
                val close = index + emptyFunction.length + 1
                output.appendReplacement(index, close, "$emptyFunction(", inheritedStyle)
                output.appendReplacement(close, close, "□", slotStyle)
                output.appendReplacement(close, close + 1, ")", inheritedStyle)
                index = close + 1
                continue
            }
            if ((source[index] == '[' || source[index] == ',') &&
                source.getOrNull(index + 1) in setOf(',', ']')
            ) {
                output.appendReplacement(index, index + 1, source[index].toString(), inheritedStyle)
                output.appendReplacement(index + 1, index + 1, "□", slotStyle)
                index++
                continue
            }
            if (source[index] in setOf('(', '{') &&
                source.getOrNull(index + 1) == if (source[index] == '(') ')' else '}'
            ) {
                output.appendReplacement(index, index + 1, source[index].toString(), inheritedStyle)
                output.appendReplacement(index + 1, index + 1, "□", slotStyle)
                output.appendReplacement(index + 1, index + 2, source[index + 1].toString(), inheritedStyle)
                index += 2
                continue
            }
            if (source[index] == '*' || source[index] == '\u00d7') {
                output.appendReplacement(
                    index,
                    index + 1,
                    " \u00d7 ",
                    inheritedStyle?.merge(multiplicationStyle) ?: multiplicationStyle,
                )
                index++
                continue
            }
            output.appendReplacement(index, index + 1, source[index].toString(), inheritedStyle)
            index++
        }
    }

    private fun renderCalculusCall(
        source: String,
        index: Int,
        end: Int,
        output: VisualBuilder,
        inheritedStyle: SpanStyle?,
    ): Int? {
        val name = structuredCalculusFunctions.firstOrNull { source.startsWith("$it(", index) } ?: return null
        val open = index + name.length
        val close = matchingParen(source, open)
        if (close !in (open + 1)..end) return null
        val arguments = topLevelArgumentRanges(source, open + 1, close)

        fun renderArgument(argumentIndex: Int, style: SpanStyle? = inheritedStyle) {
            val (start, argumentEnd) = arguments[argumentIndex]
            if (start == argumentEnd) {
                output.appendReplacement(start, argumentEnd, "□", slotStyle)
            } else {
                renderRange(source, start, argumentEnd, output, style)
            }
        }

        when {
            name in setOf("derivative", "partial") && arguments.size in setOf(2, 3) -> {
                val symbol = if (name == "derivative") "d" else "∂"
                val order = arguments.getOrNull(2)
                    ?.let { source.substring(it.first, it.second).ifBlank { "□" } }
                    ?: "1"
                output.appendReplacement(index, arguments[0].first, symbol, inheritedStyle)
                if (order != "1") {
                    output.appendReplacement(arguments[0].first, arguments[0].first, order, scriptStyle)
                }
                output.appendReplacement(arguments[0].first, arguments[0].first, "(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, arguments[1].first, ")/$symbol", inheritedStyle)
                renderArgument(1)
                if (arguments.size == 3) {
                    output.appendReplacement(arguments[1].second, arguments[2].first, "", inheritedStyle)
                    renderArgument(2, scriptStyle)
                    output.appendReplacement(arguments[2].second, close + 1, "", inheritedStyle)
                } else {
                    output.appendReplacement(arguments[1].second, close + 1, "", inheritedStyle)
                }
            }
            name == "integral" && arguments.size in setOf(2, 4) -> {
                output.appendReplacement(index, arguments[0].first, "∫(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, arguments[1].first, ") d", inheritedStyle)
                renderArgument(1)
                if (arguments.size == 4) {
                    output.appendReplacement(arguments[1].second, arguments[2].first, " [", inheritedStyle)
                    renderArgument(2, subscriptStyle)
                    output.appendReplacement(arguments[2].second, arguments[3].first, ",", inheritedStyle)
                    renderArgument(3, scriptStyle)
                    output.appendReplacement(arguments[3].second, close + 1, "]", inheritedStyle)
                } else {
                    output.appendReplacement(arguments[1].second, close + 1, "", inheritedStyle)
                }
            }
            name in setOf("limit") && arguments.size == 3 -> {
                output.appendReplacement(index, arguments[0].first, "lim(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, arguments[1].first, "; ", inheritedStyle)
                renderArgument(1, subscriptStyle)
                output.appendReplacement(arguments[1].second, arguments[2].first, "→", subscriptStyle)
                renderArgument(2, subscriptStyle)
                output.appendReplacement(arguments[2].second, close + 1, ")", inheritedStyle)
            }
            name in setOf("sum", "product") && arguments.size == 4 -> {
                output.appendReplacement(index, arguments[0].first, if (name == "sum") "Σ(" else "Π(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, arguments[1].first, "; ", inheritedStyle)
                renderArgument(1, subscriptStyle)
                output.appendReplacement(arguments[1].second, arguments[2].first, "=", subscriptStyle)
                renderArgument(2, subscriptStyle)
                output.appendReplacement(arguments[2].second, arguments[3].first, "…", inheritedStyle)
                renderArgument(3, scriptStyle)
                output.appendReplacement(arguments[3].second, close + 1, ")", inheritedStyle)
            }
            name == "contour" && arguments.size == 2 -> {
                output.appendReplacement(index, arguments[0].first, "∮(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, arguments[1].first, ") d", inheritedStyle)
                renderArgument(1)
                output.appendReplacement(arguments[1].second, close + 1, "", inheritedStyle)
            }
            name in setOf("gradient", "theta", "delta") && arguments.size == 1 -> {
                val symbol = when (name) {
                    "gradient" -> "∇"
                    "theta" -> "θ"
                    else -> "δ"
                }
                output.appendReplacement(index, arguments[0].first, "$symbol(", inheritedStyle)
                renderArgument(0)
                output.appendReplacement(arguments[0].second, close + 1, ")", inheritedStyle)
            }
            else -> return null
        }
        output.skipTo(close + 1)
        return close + 1
    }

    private data class MatrixRowMatch(
        val open: Int,
        val close: Int,
        val cells: List<Pair<Int, Int>>,
    )

    private data class MatrixMatch(
        val close: Int,
        val rows: List<MatrixRowMatch>,
    )

    private fun renderDeterminantAt(
        source: String,
        index: Int,
        end: Int,
        output: VisualBuilder,
        inheritedStyle: SpanStyle?,
    ): Int? {
        if (!source.startsWith("det(", index)) return null
        val open = index + 3
        val close = matchingParen(source, open)
        if (close !in (open + 1)..end) return null
        val matrixStart = open + 1
        val matrix = matrixAt(source, matrixStart, close) ?: return null
        if (matrix.close != close - 1) return null
        output.appendReplacement(index, matrixStart, "", inheritedStyle)
        renderMatrix(source, matrixStart, matrix, output, inheritedStyle, determinant = true)
        output.appendReplacement(matrix.close + 1, close + 1, "", inheritedStyle)
        output.skipTo(close + 1)
        return close + 1
    }

    private fun renderMatrixAt(
        source: String,
        index: Int,
        end: Int,
        output: VisualBuilder,
        inheritedStyle: SpanStyle?,
    ): Int? {
        val matrix = matrixAt(source, index, end) ?: return null
        renderMatrix(source, index, matrix, output, inheritedStyle, determinant = false)
        output.skipTo(matrix.close + 1)
        return matrix.close + 1
    }

    private fun renderMatrix(
        source: String,
        start: Int,
        matrix: MatrixMatch,
        output: VisualBuilder,
        inheritedStyle: SpanStyle?,
        determinant: Boolean,
    ) {
        output.appendReplacement(start, start + 1, "", inheritedStyle)
        matrix.rows.forEachIndexed { rowIndex, row ->
            val left = when {
                determinant -> "│"
                matrix.rows.size == 1 -> "["
                rowIndex == 0 -> "⎡"
                rowIndex == matrix.rows.lastIndex -> "⎣"
                else -> "⎢"
            }
            val right = when {
                determinant -> "│"
                matrix.rows.size == 1 -> "]"
                rowIndex == 0 -> "⎤"
                rowIndex == matrix.rows.lastIndex -> "⎦"
                else -> "⎥"
            }
            output.appendReplacement(row.open, row.open + 1, left, inheritedStyle)
            row.cells.forEachIndexed { cellIndex, cell ->
                if (cell.first == cell.second) {
                    output.appendReplacement(cell.first, cell.second, "□", slotStyle)
                } else {
                    renderRange(source, cell.first, cell.second, output, inheritedStyle)
                }
                val separatorEnd = row.cells.getOrNull(cellIndex + 1)?.first ?: row.close
                if (cell.second < separatorEnd) {
                    output.appendReplacement(cell.second, separatorEnd, "  ", inheritedStyle)
                }
            }
            output.appendReplacement(row.close, row.close + 1, right, inheritedStyle)
            val nextOpen = matrix.rows.getOrNull(rowIndex + 1)?.open
            if (nextOpen != null) {
                output.appendReplacement(row.close + 1, nextOpen, "\n", inheritedStyle)
            }
        }
        output.appendReplacement(matrix.close, matrix.close + 1, "", inheritedStyle)
    }

    private fun matrixAt(source: String, start: Int, end: Int): MatrixMatch? {
        if (source.getOrNull(start) != '[' || source.getOrNull(start + 1) != '[') return null
        val close = matchingSquareBracket(source, start)
        if (close !in (start + 2) until end) return null
        val rows = mutableListOf<MatrixRowMatch>()
        var cursor = start + 1
        while (cursor < close) {
            while (source.getOrNull(cursor)?.isWhitespace() == true || source.getOrNull(cursor) == ',') cursor++
            if (source.getOrNull(cursor) != '[') return null
            val rowClose = matchingSquareBracket(source, cursor)
            if (rowClose !in (cursor + 1) until close) return null
            rows += MatrixRowMatch(cursor, rowClose, matrixCellRanges(source, cursor + 1, rowClose))
            cursor = rowClose + 1
        }
        return rows.takeIf { it.isNotEmpty() }?.let { MatrixMatch(close, it) }
    }

    private fun matrixCellRanges(source: String, start: Int, end: Int): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        var cellStart = start
        var roundDepth = 0
        var squareDepth = 0
        for (index in start until end) {
            when (source[index]) {
                '(' -> roundDepth++
                ')' -> roundDepth--
                '[' -> squareDepth++
                ']' -> squareDepth--
                ',' -> if (roundDepth == 0 && squareDepth == 0) {
                    ranges += cellStart to index
                    cellStart = index + 1
                }
            }
        }
        ranges += cellStart to end
        return ranges
    }

    private data class FractionMatch(
        val numeratorStart: Int,
        val numeratorEnd: Int,
        val slash: Int,
        val denominatorStart: Int,
        val denominatorEnd: Int,
        val sourceEnd: Int,
    )

    private fun simpleFractionAt(source: String, start: Int, end: Int): FractionMatch? {
        if (source.getOrNull(start) != '(') return null
        val numeratorClose = matchingParen(source, start)
        if (numeratorClose !in (start + 1) until end || source.getOrNull(numeratorClose + 1) != '/' ||
            source.getOrNull(numeratorClose + 2) != '('
        ) return null
        val denominatorOpen = numeratorClose + 2
        val denominatorClose = matchingParen(source, denominatorOpen)
        if (denominatorClose !in (denominatorOpen + 1)..end) return null
        return FractionMatch(
            numeratorStart = start + 1,
            numeratorEnd = numeratorClose,
            slash = numeratorClose + 1,
            denominatorStart = denominatorOpen + 1,
            denominatorEnd = denominatorClose,
            sourceEnd = denominatorClose + 1,
        )
    }

    private class VisualBuilder(private val source: String) {
        private val text = AnnotatedString.Builder()
        private val origins = mutableListOf<Int>()
        private val visualEnds = mutableListOf<Int>()
        private var consumed = 0
        private var overlineDepth = 0

        fun beginOverline() {
            overlineDepth++
        }

        fun endOverline() {
            overlineDepth = (overlineDepth - 1).coerceAtLeast(0)
        }

        fun skipTo(sourceOffset: Int) {
            consumed = maxOf(consumed, sourceOffset.coerceIn(0, source.length))
        }

        fun appendReplacement(start: Int, end: Int, replacement: String, style: SpanStyle?) {
            skipTo(start)
            val displayed = if (overlineDepth == 0) {
                replacement
            } else {
                buildString(replacement.length * 2) {
                    replacement.forEach { character ->
                        append(character)
                        if (!character.isWhitespace() && character != '\u0305') append('\u0305')
                    }
                }
            }
            val visualStart = text.length
            text.append(displayed)
            if (style != null && displayed.isNotEmpty()) text.addStyle(style, visualStart, text.length)
            repeat(displayed.length) { offset ->
                val mapped = if (end <= start) start else start + ((end - start) * offset / displayed.length.coerceAtLeast(1))
                origins += mapped.coerceIn(0, source.length)
                visualEnds += if (end <= start) start else (mapped + 1).coerceIn(0, source.length)
            }
            consumed = maxOf(consumed, end.coerceIn(0, source.length))
        }

        fun build(): TransformedText {
            val visual = text.toAnnotatedString()
            val originalToVisual = IntArray(source.length + 1)
            var visualIndex = 0
            for (original in 0..source.length) {
                while (visualIndex < origins.size && origins[visualIndex] < original) visualIndex++
                originalToVisual[original] = visualIndex.coerceIn(0, visual.length)
            }
            val visualToOriginal = IntArray(visual.length + 1)
            visualToOriginal[0] = 0
            for (index in origins.indices) {
                visualToOriginal[index + 1] = visualEnds[index]
            }
            return TransformedText(
                visual,
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int =
                        originalToVisual[offset.coerceIn(0, source.length)]

                    override fun transformedToOriginal(offset: Int): Int =
                        visualToOriginal[offset.coerceIn(0, visual.length)]
                },
            )
        }
    }
}

private val structuredFunctions = listOf(
    "derivative", "partial", "integral", "limit", "product", "contour", "gradient",
    "nthroot", "theta", "delta", "sum",
    "asech", "acsch", "acoth", "asinh", "acosh", "atanh",
    "asec", "acsc", "acot", "asin", "acos", "atan",
    "sech", "csch", "coth", "sinh", "cosh", "tanh",
    "sin", "cos", "tan", "exp", "abs", "sqrt", "cbrt", "log", "ln",
)

private val structuredCalculusFunctions = setOf(
    "derivative", "partial", "integral", "limit", "sum", "product",
    "contour", "gradient", "theta", "delta",
)

private val structuredMultiArgumentFunctions = structuredCalculusFunctions

private fun knownFunctionAt(source: String, index: Int): String? =
    structuredFunctions.firstOrNull { source.startsWith("$it(", index) }

private fun functionNameAt(source: String, index: Int): String? = knownFunctionAt(source, index)

internal fun matchingParen(source: String, open: Int): Int {
    if (source.getOrNull(open) != '(') return -1
    var depth = 0
    for (index in open until source.length) {
        when (source[index]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return -1
}

private fun topLevelComma(source: String, start: Int, end: Int): Int {
    var depth = 0
    for (index in start until end) {
        when (source[index]) {
            '(' -> depth++
            ')' -> depth--
            ',' -> if (depth == 0) return index
        }
    }
    return -1
}

private fun topLevelArgumentRanges(source: String, start: Int, end: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    var argumentStart = start
    var depth = 0
    for (index in start until end) {
        when (source[index]) {
            '(' -> depth++
            ')' -> depth--
            ',' -> if (depth == 0) {
                result += argumentStart to index
                argumentStart = index + 1
            }
        }
    }
    result += argumentStart to end
    return result
}

private fun splitTopLevel(source: String): List<String> {
    val result = mutableListOf<String>()
    var start = 0
    var depth = 0
    source.forEachIndexed { index, char ->
        when (char) {
            '(' -> depth++
            ')' -> depth--
            ',' -> if (depth == 0) {
                result += source.substring(start, index)
                start = index + 1
            }
        }
    }
    result += source.substring(start)
    return result
}

private fun matchingSquareBracket(source: String, open: Int): Int {
    if (source.getOrNull(open) != '[') return -1
    var depth = 0
    for (index in open until source.length) {
        when (source[index]) {
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return -1
}
