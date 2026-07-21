package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathInputIntelligence
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
}
