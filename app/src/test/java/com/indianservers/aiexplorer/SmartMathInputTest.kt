package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputTokenKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMathInputTest {
    @Test
    fun classifiesMathsTokensForIdeStyleHighlighting() {
        val result = MathInputIntelligence.analyze("Differentiate sin(x^2) + pi/4")

        assertTrue(result.tokens.any { it.text.equals("Differentiate", true) && it.kind == MathInputTokenKind.Keyword })
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
}
