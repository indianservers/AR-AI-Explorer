package com.indianservers.aiexplorer.input

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.ExpressionEngine
import org.junit.Assert.*
import org.junit.Test

class MathPasteConverterTest {
    private fun converted(input: String, expected: String) {
        val result = MathPasteConverter.convert(input)
        assertEquals(expected, result.converted)
        assertTrue(result.warnings.toString(), result.warnings.isEmpty())
        assertTrue(result.needsPreview)
        assertTrue(result.canInsertConverted)
    }

    @Test fun ordinaryTextAndAmbiguousSymbolsRemainUntouched() {
        listOf("x+1", "  x + 1  ", "x ≈ y", "α + β", "sin x", "x+").forEach {
            val result = MathPasteConverter.convert(it)
            assertEquals(it, result.converted)
            assertFalse(result.needsPreview)
        }
    }

    @Test fun unicodeExamplesAndSignedPowers() {
        converted("x² + π", "x^(2) + pi")
        converted("½ × x", "((1)/(2)) * x")
        converted("x⁻¹² ÷ 2 − 1 ≤ 3", "x^(-12) / 2 - 1 <= 3")
        converted("√(x+(1))", "sqrt(x+(1))")
        converted("√25 + √π", "sqrt(25) + sqrt(pi)")
        converted("x ≈ π", "x ≈ pi")
    }

    @Test fun nestedLatexAndRoots() {
        converted("\\frac{1}{\\sqrt{x}}", "((1)/(sqrt(x)))")
        converted("1/\\frac{2}{3}", "1/((2)/(3))")
        converted("\\frac{\\frac{1}{2}}{3}", "((((1)/(2)))/(3))")
        converted("\\sqrt[3]{x^{2}+1}", "nthroot(3,x^(2)+1)")
        converted("x_{12}", "x_(12)")
    }

    @Test fun mathWrappersAndExplicitFunctions() {
        listOf("$" to "$", "$$" to "$$", "\\(" to "\\)", "\\[" to "\\]").forEach { (left, right) ->
            converted(left + "\\pi" + right, "pi")
        }
        converted("\\sin(x+(1)) + \\ln{x}", "sin(x+(1)) + ln(x)")
        converted("\\cos\\left(x+\\frac{1}{2}\\right)", "cos(x+((1)/(2)))")
        converted("\\left[x+1\\right]", "[x+1]")
        converted("\\log_{2}{x}", "((ln(x))/(ln(2)))")
        converted("\\arcsin(x)", "asin(x)")
        converted("\\sin^{2}(x)", "(sin(x))^(2)")
        converted("\\pi \\cdot x ≥ ½", "pi * x >= ((1)/(2))")
    }

    @Test fun malformedAndUnsupportedNotationNeverProducesPartialConversion() {
        listOf("\\frac{1}", "\\sqrt{x", "\\unknown{x}+π", "\\sin x", "$" + "x", "\\left(x\\right]", "\\left(x)", "2½", "√", "√sin(x)", "\\sin(x", "\\frac{1}{2}}", "\\approx").forEach {
            val result = MathPasteConverter.convert(it)
            assertEquals(it, result.converted)
            assertTrue(it, result.warnings.isNotEmpty())
            assertTrue(result.needsPreview)
            assertFalse(result.canInsertConverted)
        }
    }

    @Test fun convertedInsertionReplacesReversedSelectionAndPlacesCursorAfterPaste() {
        val conversion = MathPasteConverter.convert("x²")
        val before = TextFieldValue("a+old+b", TextRange(5, 2))
        val after = MathTextEditing.replaceSelection(before, conversion.converted)
        assertEquals("a+x^(2)+b", after.text)
        assertEquals(TextRange(7), after.selection)
        assertEquals("a+old+b", before.text)
    }

    @Test fun excessiveNestingFailsSafely() {
        val source = "\\sqrt{".repeat(70) + "x" + "}".repeat(70)
        assertFalse(MathPasteConverter.convert(source).canInsertConverted)
    }

    @Test fun conversionPreservesFractionRootAndLogEvaluation() {
        val engine = ExpressionEngine()
        val examples = mapOf(
            "1/\\frac{2}{3}" to 1.5,
            "\\frac{1}{\\sqrt{4}}" to 0.5,
            "\\sqrt[3]{8}" to 2.0,
            "\\log_{2}{8}" to 3.0,
            "2 ÷ ½" to 4.0,
        )
        examples.forEach { (input, expected) ->
            val result = MathPasteConverter.convert(input)
            assertTrue(result.warnings.toString(), result.canInsertConverted)
            val parserText = StructuredMathCodec.toParser(TextFieldValue(result.converted)).text
            assertEquals(input, expected, engine.compile(parserText).eval(), 1e-9)
        }
    }
}
