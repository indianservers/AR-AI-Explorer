package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class ScientificCalculatorPhase1Test {
    @Test fun supportsGradianAngleMode() {
        val result = ScientificCalculator().evaluate("sin(100)", AngleMode.Gradians)
        assertEquals(1.0, result.value, 1e-10)
        assertTrue(result.warnings.any { it.contains("gradians") })
    }

    @Test fun naturalInputVariablesAndFunctionsShareOneSession() {
        val session = SmartScientificCalculator()
        assertEquals("7", session.evaluate("what is 3 plus 4").primary)
        session.evaluate("a = 5")
        session.evaluate("f(x)=x^2+a")
        val result = session.evaluate("f(3)")
        assertEquals("14", result.primary)
        assertEquals(5.0, session.variables["a"]!!, 0.0)
        assertEquals("x^2+a", session.functions["f"])
    }

    @Test fun solvesAndVerifiesAnEquation() {
        val result = SmartScientificCalculator().evaluate("solve 2*x+5=11")
        assertTrue(result.primary.contains("3"))
        assertTrue(result.verification.contains("residual", ignoreCase = true))
    }

    @Test fun interpretsDefiniteIntegralAndReturnsAlternatives() {
        val result = SmartScientificCalculator().evaluate("integrate x^2 from 0 to 3")
        assertEquals("9", result.primary)
        assertTrue(result.alternatives.any { it.first == "Scientific" })
        assertTrue(result.steps.any { it.contains("integral", ignoreCase = true) })
    }

    @Test fun diagnosticsTokensTemplatesAndEditorHistoryAreDeterministic() {
        assertTrue(CalculatorInputIntelligence.diagnostics("sin((30)").any { it.message.contains("not closed") })
        val tokens = CalculatorInputIntelligence.tokenize("sin(a)+30", setOf("a"))
        assertEquals(CalculatorTokenKind.Function, tokens.first().kind)
        assertTrue(tokens.any { it.text == "a" && it.kind == CalculatorTokenKind.Variable })
        assertTrue(CalculatorInputIntelligence.templates[CalculatorKeyboardLayer.Structural].orEmpty().any { it.label.contains("integral") })
        val editor = CalculatorEditorHistory("1")
        editor.edit("12"); editor.edit("123")
        assertEquals("12", editor.undo()); assertEquals("123", editor.redo())
    }

    @Test fun smartEditorPairsWrapsMultipliesAndDeletesStructuralTokens() {
        val function = CalculatorInputIntelligence.smartInsert("2", 1, 1, "sin(")
        assertEquals("2*sin()", function.text)
        assertEquals(function.text.indexOf(')'), function.cursor)

        val wrapped = CalculatorInputIntelligence.smartInsert("3+4", 0, 3, "sqrt(")
        assertEquals("sqrt(3+4)", wrapped.text)
        assertEquals(wrapped.text.length, wrapped.cursor)

        val skippedClose = CalculatorInputIntelligence.smartInsert("sin()", 4, 4, ")")
        assertEquals("sin()", skippedClose.text)
        assertEquals(5, skippedClose.cursor)

        val deleted = CalculatorInputIntelligence.smartBackspace("sin()", 4, 4)
        assertEquals("", deleted.text)
        assertEquals(0, deleted.cursor)

        val decimal = CalculatorInputIntelligence.smartInsert("12.3", 4, 4, ".")
        assertEquals("12.3", decimal.text)

        val negative = CalculatorInputIntelligence.toggleSign("12+34", 5, 5)
        assertEquals("12+-34", negative.text)
        val positive = CalculatorInputIntelligence.toggleSign(negative.text, negative.cursor, negative.cursor)
        assertEquals("12+34", positive.text)
        assertTrue(CalculatorInputIntelligence.suggestions("si", 2).contains("sin("))
    }

    @Test fun branchingHistoryPreservesOriginalInput() {
        val session = SmartScientificCalculator()
        session.evaluate("2+3")
        session.evaluate("7*8")
        assertEquals("2+3", session.branchFrom(0))
        assertEquals(2, session.history.size)
    }
}
