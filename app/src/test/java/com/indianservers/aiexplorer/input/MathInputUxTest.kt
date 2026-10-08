package com.indianservers.aiexplorer.input

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.MathInputContext
import com.indianservers.aiexplorer.core.MathInputIntelligence
import org.junit.Assert.*
import org.junit.Test

class MathInputUxTest {
    @Test fun nextSlotWalksNestedStructuresAndDoesNotWrap() {
        val source = "(sin(x^(2)))/(sqrt(y))+if(x,1,2)+[[1,2],[3,4]]"
        val slots = StructuredMathEditing.editableSlots(source).sortedBy { it.contentStart }
        assertTrue(slots.any { it.mode == MathInputMode.NUMERATOR })
        assertTrue(slots.any { it.mode == MathInputMode.DENOMINATOR })
        assertEquals(4, slots.count { it.mode == MathInputMode.MATRIX_CELL })
        assertEquals(3, slots.count { it.owner.startsWith("if:") })
        var value = TextFieldValue(source, TextRange(0))
        slots.map { it.contentStart }.distinct().filter { it > 0 }.forEach { offset ->
            value = nextMathSlot(value)
            assertEquals(offset, value.selection.end)
            assertEquals(source, value.text)
        }
        assertEquals(TextRange(source.length), nextMathSlot(value).selection)
        assertEquals(TextRange(source.length), nextMathSlot(TextFieldValue(source, TextRange(source.length))).selection)
    }

    @Test fun exitLeavesInnermostStructureAndBaselineIsUnchanged() {
        val source = "sin(x^(2))+1"
        val value = TextFieldValue(source, TextRange(7))
        assertEquals(TextRange(9), exitMathStructure(value).selection)
        assertEquals(TextRange(10), exitMathStructure(exitMathStructure(value)).selection)
        val baseline = TextFieldValue(source, TextRange(source.length))
        assertEquals(baseline, exitMathStructure(baseline))
    }

    @Test fun incompleteFunctionsAndMatrixArgumentsRemainNavigable() {
        val source = "if(det([[1,2],[3,4]]),sqrt("
        val slots = StructuredMathEditing.editableSlots(source)
        assertEquals(2, slots.count { it.owner.startsWith("if:") })
        assertTrue(slots.any { it.mode == MathInputMode.RADICAND && it.isPlaceholder })
        assertEquals(TextRange(source.length), exitMathStructure(TextFieldValue(source, TextRange(source.length))).selection)
    }

    @Test fun transformsPreserveGroupingAndReversedSelections() {
        val value = TextFieldValue("1+x+2", TextRange(5, 2))
        val engine = ExpressionEngine()
        val expected = mapOf(MathSelectionTransform.SQUARE to 26.0, MathSelectionTransform.ROOT to 1 + kotlin.math.sqrt(5.0), MathSelectionTransform.NEGATE to -4.0, MathSelectionTransform.WRAP to 6.0)
        expected.forEach { (action, result) ->
            val transformed = transformMathSelection(value, action)
            assertEquals(result, engine.compile(transformed.text).eval(mapOf("x" to 3.0)), 1e-9)
            assertTrue(transformed.selection.collapsed)
        }
        val collapsed = value.copy(selection = TextRange(1))
        assertEquals(collapsed, transformMathSelection(collapsed, MathSelectionTransform.ROOT))
    }

    @Test fun bracketMatchingAndTermSelectionHandleNestedAndBrokenInput() {
        assertEquals(5 to 7, matchingMathBrackets("a+(x+[y])", 6))
        assertNull(matchingMathBrackets("([)]", 2))
        assertEquals(TextRange(2, 7), mathTermSelection("x+alpha+1", 4))
    }

    @Test fun everyPlaceholderMapsToItsSourceAndNeverSerializesBoxes() {
        val source = "()/()+x^()+sqrt()+min(,)+[[,],[,]]"
        val transformed = StructuredMathVisualLayout.render(source)
        StructuredMathEditing.editableSlots(source).filter { it.isPlaceholder }.forEach { slot ->
            val visual = transformed.offsetMapping.originalToTransformed(slot.contentStart)
            assertEquals("slot $slot", '□', transformed.text[visual])
            assertEquals(slot.contentStart, mathSourceOffsetAtVisual(source, transformed, visual))
        }
        assertFalse('□' in StructuredMathCodec.toParser(TextFieldValue(source)).text)
    }

    @Test fun codecPreservesCursorInsideReorderedLogArguments() {
        val editor = TextFieldValue("logbase(2,8)", TextRange(10, 11))
        val parser = StructuredMathCodec.toParser(editor)
        assertEquals("log(8,2)", parser.text)
        assertEquals("8", parser.text.substring(parser.selection.min, parser.selection.max))
        assertEquals(editor, StructuredMathCodec.fromParser(parser))
        val entireArguments = StructuredMathCodec.toParser(editor.copy(selection = TextRange(8, 11)))
        assertEquals("8,2", entireArguments.text.substring(entireArguments.selection.min, entireArguments.selection.max))
        val reversed = StructuredMathCodec.toParser(editor.copy(selection = TextRange(11, 8)))
        assertEquals(TextRange(7, 4), reversed.selection)
    }

    @Test fun visualHitMappingSkipsHiddenRootAndScriptSyntax() {
        val root = StructuredMathVisualLayout.render("sqrt(x)")
        val offset = root.text.indexOf('x')
        assertEquals(5, mathSourceOffsetAtVisual("sqrt(x)", root, offset))
        val script = StructuredMathVisualLayout.render("x^(12)")
        assertEquals(3, mathSourceOffsetAtVisual("x^(12)", script, script.text.indexOf('1')))
    }

    @Test fun favoritesHaveStableDefaultsAndAllowExplicitEmpty() {
        assertEquals(5, defaultMathFavoriteIds.size)
        assertEquals(emptyList<String>(), sanitizeMathFavorites(emptyList()))
        assertEquals(defaultMathFavoriteIds, sanitizeMathFavorites(defaultMathFavoriteIds + defaultMathFavoriteIds + "missing"))
        assertEquals(8, sanitizeMathFavorites(mathFavoriteCatalog.keys.take(10)).size)
    }

    @Test fun contextualSuggestionsAreDeduplicatedAndRespectNestedArguments() {
        val assistance = mathKeyboardAssistance("x^(2)", 3, MathInputContext.General)
        assertEquals(assistance.actions.size, assistance.actions.distinctBy { listOf(it.replacement, it.replaceStart, it.replaceEnd) }.size)
        assertTrue(assistance.actions.any { it.replacement == "-1" })
        val source = "if(det([[1,2],[3,4]]),1,0)"
        assertEquals(0, MathInputIntelligence.assist(source, source.indexOf("4")).functionHint?.activeParameter)
        assertEquals(1, MathInputIntelligence.assist(source, source.indexOf(",1,0") + 1).functionHint?.activeParameter)
        assertEquals("value", MathInputIntelligence.assist("nthroot(3,8)", 10).functionHint?.parameterName)
        val variadic = MathInputIntelligence.assist("min(1,2,3,4)", 10).functionHint
        assertEquals(3, variadic?.activeParameter)
        assertEquals("next value", variadic?.parameterName)
        val derivative = MathInputIntelligence.assist("derivative(x,x,2)", 15)
        assertTrue(derivative.actions.any { it.replacement == "2" })
        assertEquals("positive integer order (optional)", derivative.functionHint?.description)
    }

    @Test fun repeatTimingAcceleratesAtTwoSeconds() {
        assertEquals(100L, backspaceRepeatInterval(400))
        assertEquals(100L, backspaceRepeatInterval(1999))
        assertEquals(60L, backspaceRepeatInterval(2000))
    }

    @Test fun incompleteMatricesAndBracesExposeEmptySlots() {
        assertEquals(2, StructuredMathEditing.editableSlots("[[1,").count { it.mode == MathInputMode.MATRIX_CELL })
        val brace = StructuredMathEditing.editableSlots("{}").single()
        assertEquals(MathInputMode.BRACKET_CONTENT, brace.mode)
        assertTrue(brace.isPlaceholder)
        assertEquals(TextRange(0, 7), mathTermSelection("sqrt(x)", 1))
    }
}
