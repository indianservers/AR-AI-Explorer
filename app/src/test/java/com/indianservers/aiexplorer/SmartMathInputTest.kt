package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputAssistKind
import com.indianservers.aiexplorer.core.MathInputContext
import com.indianservers.aiexplorer.core.MathInputIntent
import com.indianservers.aiexplorer.core.MathInputTokenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMathInputTest {
    @Test
    fun classifiesMathsTokensForIdeStyleHighlighting() {
        val result = MathInputIntelligence.analyze("Differentiate sin(x^2) + pi/4")

        assertTrue(result.tokens.any { it.text.equals("Differentiate", true) && it.kind == MathInputTokenKind.Command })
        assertTrue(result.tokens.any { it.text == "sin" && it.kind == MathInputTokenKind.Function })
        assertTrue(result.tokens.any { it.text == "x" && it.kind == MathInputTokenKind.Variable })
        assertTrue(result.tokens.any { it.text == "2" && it.kind == MathInputTokenKind.Number })
        assertTrue(result.tokens.any { it.text == "pi" && it.kind == MathInputTokenKind.Constant })
        assertTrue(result.validBrackets)
    }

    @Test
    fun reportsNestedAndMismatchedBrackets() {
        val nested = MathInputIntelligence.analyze("sin((x+1)^2)")
        val invalid = MathInputIntelligence.analyze("cos(x]")

        assertTrue(nested.validBrackets)
        assertTrue(nested.tokens.filter { it.kind == MathInputTokenKind.Bracket }.map { it.depth }.distinct().size >= 2)
        assertFalse(invalid.validBrackets)
        assertTrue(invalid.tokens.any { it.kind == MathInputTokenKind.Error })
    }

    @Test
    fun identifiesIntentWhileTheUserTypesAcrossEveryMathWorkspace() {
        assertEquals(MathInputIntent.Equation, MathInputIntelligence.analyze("x^2-5*x+6=0").intent)
        assertEquals(MathInputIntent.Inequality, MathInputIntelligence.analyze("x^2<4").intent)
        assertEquals(MathInputIntent.Calculus, MathInputIntelligence.analyze("integrate sin(x) from 0 to pi").intent)
        assertEquals(MathInputIntent.Trigonometry, MathInputIntelligence.analyze("sin(x)+cos(x)").intent)
        assertEquals(MathInputIntent.LinearAlgebra, MathInputIntelligence.analyze("eigenvalues [[1,2],[3,4]]").intent)
        assertEquals(MathInputIntent.Units, MathInputIntelligence.analyze("convert 5 km to m").intent)
        assertEquals(MathInputIntent.DifferentialEquation, MathInputIntelligence.analyze("y'=2*y+1").intent)
        assertEquals(MathInputIntent.Construction, MathInputIntelligence.analyze("midpoint(M,A,B)").intent)
        assertEquals(MathInputIntent.Geometry3D, MathInputIntelligence.analyze("surface(s,z=x^2+y^2)").intent)
        assertEquals(MathInputIntent.Graph, MathInputIntelligence.analyze("y=2*x+1").intent)
    }

    @Test
    fun assignsDistinctSemanticKindsAndGuidesIncompleteInput() {
        val result = MathInputIntelligence.analyze("solve sin(x) >= 2.5e-3 m")
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Command })
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Function })
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Variable })
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Relation })
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Number && it.text == "2.5e-3" })
        assertTrue(result.tokens.any { it.kind == MathInputTokenKind.Unit })
        assertTrue(result.normalizedPreview.isNotBlank())

        val incomplete = MathInputIntelligence.analyze("x+")
        assertTrue(incomplete.message.contains("incomplete"))
        assertTrue("number" in incomplete.expectedNext)
    }

    @Test
    fun autocompletesAtTheCursorAndPlacesItInsideFunctionArguments() {
        val action = MathInputIntelligence.assist("si", 2, MathInputContext.Graph2D).actions
            .first { it.label == "sin" }

        val (text, cursor) = MathInputIntelligence.apply("si", action)

        assertEquals("sin()", text)
        assertEquals(4, cursor)
        assertEquals(MathInputAssistKind.Autocomplete, action.kind)
    }

    @Test
    fun offersConservativeSyntaxRepairsWithoutApplyingThemAutomatically() {
        val multiply = MathInputIntelligence.assist("2x+1").actions.first { it.label == "Add *" }
        val close = MathInputIntelligence.assist("sin(x+1").actions.first { it.label.startsWith("Close") }

        assertEquals("2*x+1", MathInputIntelligence.apply("2x+1", multiply).first)
        assertEquals("sin(x+1)", MathInputIntelligence.apply("sin(x+1", close).first)
    }

    @Test
    fun exposesActiveFunctionParameterAndUsefulValues() {
        val source = "integral(x^2,"
        val assistance = MathInputIntelligence.assist(source, source.length, MathInputContext.Calculus)

        assertEquals("integral", assistance.functionHint?.name)
        assertEquals(1, assistance.functionHint?.activeParameter)
        assertEquals("variable", assistance.functionHint?.parameterName)
        assertTrue(assistance.actions.any { it.kind == MathInputAssistKind.Parameter && it.label == "x" })
    }

    @Test
    fun blankInputsReceiveWorkspaceSpecificExamples() {
        val graph3d = MathInputIntelligence.assist("", context = MathInputContext.Graph3D)

        assertTrue(graph3d.actions.any { it.kind == MathInputAssistKind.Example && it.replacement.startsWith("z=") })
        assertTrue(graph3d.actions.all { it.replaceStart == 0 && it.replaceEnd == 0 })
    }

    @Test
    fun repairsNearMissFunctionNamesThroughTheSameSafeEditModel() {
        val repair = MathInputIntelligence.assist("sni", 3).actions.first { it.label == "Fix sin" }

        assertEquals("sin()", MathInputIntelligence.apply("sni", repair).first)
        assertEquals(MathInputAssistKind.Repair, repair.kind)
    }
}
