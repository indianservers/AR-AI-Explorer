package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.SetExpression
import com.indianservers.aiexplorer.core.SetExpressionEvaluator
import com.indianservers.aiexplorer.core.SetExpressionParser
import com.indianservers.aiexplorer.core.SetLawVerifier
import com.indianservers.aiexplorer.core.SetTheoryStudioEngine
import com.indianservers.aiexplorer.core.VennRegionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetTheoryExpressionEngineTest {
    private val universe = setOf("1", "2", "3", "4", "5", "6")
    private val sets = mapOf(
        "A" to setOf("1", "2", "3"),
        "B" to setOf("3", "4"),
        "C" to setOf("3", "5"),
    )

    @Test
    fun coreOperationsEvaluateAgainstUniverse() {
        assertEquals(setOf("1", "2", "3", "4"), evaluate("A ∪ B"))
        assertEquals(setOf("3"), evaluate("A ∩ B"))
        assertEquals(setOf("1", "2"), evaluate("A - B"))
        assertEquals(setOf("4", "5", "6"), evaluate("A'"))
        assertEquals(setOf("1", "2", "4"), evaluate("A Δ B"))
    }

    @Test
    fun parserHonoursIntersectionBeforeUnionAndParentheses() {
        assertEquals(setOf("1", "2", "3"), evaluate("A ∪ B ∩ C"))
        assertEquals(setOf("3"), evaluate("(A ∪ B) ∩ C"))
        assertEquals(setOf("3"), evaluate("A intersection (B union C)"))
    }

    @Test
    fun parserReturnsAstAndClearErrors() {
        val parsed = SetExpressionParser.parse("(A ∪ B)'")
        assertTrue(parsed.valid)
        assertTrue(parsed.expression is SetExpression.Complement)
        assertFalse(SetExpressionParser.parse("A ∪ (B").valid)
        assertFalse(SetExpressionParser.parse("A + B").valid)
    }

    @Test
    fun membershipMasksMatchThreeSetRegions() {
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7), regions("A ∪ B ∪ C"))
        assertEquals(setOf(7), regions("A ∩ B ∩ C"))
        assertEquals(setOf(3, 5, 6), regions("exactlyTwoReplacement"))
        assertEquals(setOf(0), regions("(A ∪ B ∪ C)'"))
    }

    @Test
    fun deMorganAndDistributiveLawsAreVerifiedByRegions() {
        val leftDeMorgan = parse("(A ∪ B)'")
        val rightDeMorgan = parse("A' ∩ B'")
        val leftDistributive = parse("A ∩ (B ∪ C)")
        val rightDistributive = parse("(A ∩ B) ∪ (A ∩ C)")

        assertTrue(SetLawVerifier.equivalent(leftDeMorgan, rightDeMorgan))
        assertTrue(SetLawVerifier.equivalent(leftDistributive, rightDistributive))
    }

    @Test
    fun subsetPowerProductAndInclusionExclusionStayExact() {
        assertTrue(SetTheoryStudioEngine.isSubset(setOf("1", "2"), sets.getValue("A")))
        assertTrue(SetTheoryStudioEngine.isProperSubset(setOf("1", "2"), sets.getValue("A")))
        assertEquals(8, SetTheoryStudioEngine.powerSet(listOf("a", "b", "c")).size)
        assertEquals(6, SetTheoryStudioEngine.cartesianProduct(listOf("1", "2"), listOf("x", "y", "z")).size)
        assertEquals(5, SetTheoryStudioEngine.inclusionExclusion(sets.getValue("A"), sets.getValue("B"), sets.getValue("C")))
    }

    private fun evaluate(source: String) = SetExpressionEvaluator.evaluate(parse(source), sets, universe)
    private fun parse(source: String) = SetExpressionParser.parse(source).expression ?: error("Could not parse $source")
    private fun regions(source: String): Set<Int> {
        if (source == "exactlyTwoReplacement") {
            val a = parse("A ∩ B ∩ C'")
            val b = parse("A ∩ C ∩ B'")
            val c = parse("B ∩ C ∩ A'")
            return VennRegionEngine.highlighted(SetExpression.Union(SetExpression.Union(a, b), c), 3)
        }
        return VennRegionEngine.highlighted(parse(source), 3)
    }
}
