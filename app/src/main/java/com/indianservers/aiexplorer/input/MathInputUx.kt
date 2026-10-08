package com.indianservers.aiexplorer.input

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import com.indianservers.aiexplorer.core.MathInputAssistance
import com.indianservers.aiexplorer.core.MathInputAssistAction
import com.indianservers.aiexplorer.core.MathInputAssistKind
import com.indianservers.aiexplorer.core.MathInputContext
import com.indianservers.aiexplorer.core.MathInputIntelligence

internal fun mathKeyboardAssistance(source: String, cursor: Int, context: MathInputContext): MathInputAssistance {
    val existing = MathInputIntelligence.assist(source, cursor, context)
    val mode = StructuredMathEditing.modeAt(source, cursor)
    val values = when (mode) {
        MathInputMode.SUPERSCRIPT -> listOf("2", "3", "-1")
        MathInputMode.DENOMINATOR -> listOf("2", "x", "1")
        MathInputMode.ROOT_INDEX, MathInputMode.LOG_BASE -> listOf("2", "3", "10")
        MathInputMode.MATRIX_CELL -> listOf("0", "1", "-1")
        else -> emptyList()
    }
    val structural = values.map { MathInputAssistAction(it, "Insert $it in ${mathSlotLabel(mode).lowercase()}", it, cursor, cursor, it.length, MathInputAssistKind.Parameter) }
    return existing.copy(actions = (existing.actions.filter { it.kind == MathInputAssistKind.Repair } + structural + existing.actions)
        .distinctBy { listOf(it.replacement, it.replaceStart, it.replaceEnd) }.take(6))
}

enum class MathSelectionTransform(val label: String) { SQUARE("Square"), ROOT("Root"), NEGATE("Negate"), WRAP("Wrap in brackets") }

fun nextMathSlot(value: TextFieldValue): TextFieldValue {
    val next = StructuredMathEditing.editableSlots(value.text)
        .sortedWith(compareBy<MathEditableSlot> { it.contentStart }.thenBy { it.contentEnd })
        .firstOrNull { it.contentStart > value.selection.end }
    return value.copy(selection = TextRange(next?.contentStart ?: value.text.length))
}

fun exitMathStructure(value: TextFieldValue): TextFieldValue =
    StructuredMathEditing.activeSlot(value.text, value.selection.end)?.let {
        value.copy(selection = TextRange(it.sourceEnd.coerceAtMost(value.text.length)))
    } ?: value

fun transformMathSelection(value: TextFieldValue, action: MathSelectionTransform): TextFieldValue {
    if (value.selection.collapsed) return value
    val selected = value.text.substring(value.selection.min, value.selection.max)
    val replacement = when (action) {
        MathSelectionTransform.SQUARE -> "($selected)^(2)"
        MathSelectionTransform.ROOT -> "sqrt($selected)"
        MathSelectionTransform.NEGATE -> "-($selected)"
        MathSelectionTransform.WRAP -> "($selected)"
    }
    return MathTextEditing.replaceSelection(value, replacement)
}

fun mathSlotLabel(mode: MathInputMode): String = when (mode) {
    MathInputMode.BASELINE -> "Baseline"
    MathInputMode.SUPERSCRIPT -> "Exponent"
    MathInputMode.SUBSCRIPT -> "Subscript"
    MathInputMode.LOG_BASE -> "Logarithm base"
    MathInputMode.FUNCTION_ARGUMENT -> "Function argument"
    MathInputMode.NUMERATOR -> "Numerator"
    MathInputMode.DENOMINATOR -> "Denominator"
    MathInputMode.ROOT_INDEX -> "Root index"
    MathInputMode.RADICAND -> "Radicand"
    MathInputMode.MATRIX_CELL -> "Matrix cell"
    MathInputMode.BRACKET_CONTENT -> "Bracket content"
}

fun matchingMathBrackets(source: String, cursor: Int): Pair<Int, Int>? {
    val stack = mutableListOf<Int>()
    val pairs = mutableListOf<Pair<Int, Int>>()
    source.forEachIndexed { index, char ->
        when (char) {
            '(', '[', '{' -> stack += index
            ')', ']', '}' -> {
                val opening = stack.lastOrNull()
                val expected = when (char) { ')' -> '('; ']' -> '['; else -> '{' }
                if (opening != null && source[opening] == expected) {
                    stack.removeAt(stack.lastIndex)
                    pairs += opening to index
                } else stack.clear()
            }
        }
    }
    return pairs.filter { cursor in it.first..(it.second + 1) }
        .minByOrNull { it.second - it.first }
}

/** Hit placeholders before generic offset mapping, which can hide syntax around a slot. */
fun mathSourceOffsetAtVisual(source: String, transformed: TransformedText, visualOffset: Int): Int {
    val offset = visualOffset.coerceIn(0, transformed.text.length)
    StructuredMathEditing.editableSlots(source).filter { it.isPlaceholder }.forEach { slot ->
        val visual = transformed.offsetMapping.originalToTransformed(slot.contentStart)
        if (transformed.text.getOrNull(visual) == '□' && offset in visual..visual + 1) return slot.contentStart
    }
    return transformed.offsetMapping.transformedToOriginal(offset).coerceIn(0, source.length)
}

fun mathTermSelection(source: String, cursor: Int): TextRange {
    if (source.isEmpty()) return TextRange.Zero
    val index = cursor.coerceIn(0, source.lastIndex)
    matchingMathBrackets(source, index)?.takeIf { source[index] in "()[]{}" }?.let { return TextRange(it.first, it.second + 1) }
    fun term(c: Char) = c.isLetterOrDigit() || c == '.' || c == '_'
    if (!term(source[index])) return TextRange(index, index + 1)
    var start = index
    var end = index + 1
    while (start > 0 && term(source[start - 1])) start--
    while (end < source.length && term(source[end])) end++
    if (source.getOrNull(end) == '(') matchingMathBrackets(source, end)?.takeIf { it.first == end }?.let { return TextRange(start, it.second + 1) }
    return TextRange(start, end)
}
