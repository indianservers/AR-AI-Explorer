package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.steps.MathRuleKnowledgeBase
import com.indianservers.aiexplorer.solver.domain.steps.SolverExplanationEngine
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2RuleKnowledgeBaseTest {
    @Test
    fun atLeastOneHundredFiftyRuleProfileChecksAreStructuredAndSpecific() {
        val rules = MathRuleKnowledgeBase.all()
        assertTrue(rules.size >= 20)
        var checks = 0
        rules.forEach { rule ->
            assertEquals(rule.id, SolverRuleRegistry.get(rule.id).id)
            assertTrue(rule.formalStatement.isNotBlank())
            checks++
            assertTrue(rule.conditions.isNotEmpty())
            checks++
            assertTrue(rule.examples.isNotEmpty())
            checks++
            ExplanationProfile.entries.forEach { profile ->
                val explanation = rule.explanation(profile)
                assertTrue(explanation.isNotBlank())
                assertFalseFiller(explanation)
                checks++
            }
            rule.conditions.forEach {
                assertTrue(it.statement.isNotBlank())
                checks++
            }
            rule.examples.forEach {
                assertTrue(it.expression.isNotBlank())
                checks++
            }
        }
        assertTrue("Expected at least 150 rule-level checks, got $checks", checks >= 150)
    }

    @Test
    fun explanationProfilesShareRulesButVaryLanguage() {
        val rule = MathRuleKnowledgeBase.get(SolverRuleRegistry.DIVISION_EQUALITY)
        assertNotNull(rule)
        val explanations = ExplanationProfile.entries.map { SolverExplanationEngine.explanation(rule!!.id, it, "fallback") }
        assertEquals(4, explanations.distinct().size)
        assertNotEquals(explanations.first(), explanations.last())
    }

    private fun assertFalseFiller(text: String) {
        listOf("simplify the equation", "do the calculation", "apply algebra", "solve normally", "using the formula")
            .forEach { filler -> assertTrue("Generic filler found: $text", !text.equals(filler, true)) }
    }
}
