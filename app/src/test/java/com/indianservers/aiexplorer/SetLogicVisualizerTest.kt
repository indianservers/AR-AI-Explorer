package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.SetLogicCatalog
import com.indianservers.aiexplorer.core.SetLogicEngine
import com.indianservers.aiexplorer.core.SetStudioTool
import com.indianservers.aiexplorer.core.SetTheoryLearningCatalog
import com.indianservers.aiexplorer.core.SetTheoryStudioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetLogicVisualizerTest {
    @Test
    fun everySetAndLogicFormulaIsVerifiedForEveryAssignment() {
        val laws = SetLogicCatalog.setLaws + SetLogicCatalog.logicLaws
        assertEquals(30, laws.size)
        assertEquals(laws.size, laws.map { it.id }.distinct().size)
        laws.forEach { law ->
            val rows = SetLogicEngine.rows(law)
            assertEquals(1 shl law.variables.size, rows.size)
            assertTrue("${law.title} has a counterexample", rows.all { it.equivalent })
            assertTrue(SetLogicEngine.verified(law))
        }
    }

    @Test
    fun interactiveAssignmentsProduceExpectedMembershipAndTruthValues() {
        val union = SetLogicCatalog.setLaws.first { it.id == "set-union" }
        assertTrue(SetLogicEngine.evaluate(union, mapOf("A" to false, "B" to true)).left)

        val implication = SetLogicCatalog.logicLaws.first { it.id == "logic-implication" }
        val falseCase = SetLogicEngine.evaluate(implication, mapOf("P" to true, "Q" to false))
        assertEquals(false, falseCase.left)
        assertTrue(falseCase.equivalent)
    }

    @Test
    fun setTheoryStudioContainsAllFiftyConceptsAndFiftyRoutedEnhancements() {
        assertEquals(50, SetTheoryLearningCatalog.concepts.size)
        assertEquals(50, SetTheoryLearningCatalog.concepts.map { it.id }.distinct().size)
        assertTrue(SetTheoryLearningCatalog.concepts.all { it.definition.isNotBlank() && it.example.isNotBlank() })
        assertEquals(50, SetTheoryLearningCatalog.features.size)
        assertEquals(50, SetTheoryLearningCatalog.features.map { it.id }.distinct().size)
        SetStudioTool.entries.forEach { tool -> assertTrue("$tool has routed UI enhancements", SetTheoryLearningCatalog.features.any { it.tool == tool }) }
    }

    @Test
    fun studioEnginesComputeSetsRelationsOrdersAndMappings() {
        assertEquals(listOf("1", "2", "3"), SetTheoryStudioEngine.parseElements("1, 2, 2; 3"))
        assertEquals(8, SetTheoryStudioEngine.powerSet(listOf("a", "b", "c")).size)
        assertEquals(4, SetTheoryStudioEngine.cartesianProduct(listOf("a", "b"), listOf("1", "2")).size)
        val domain = setOf("1", "2")
        val equality = setOf("1" to "1", "2" to "2")
        val relation = SetTheoryStudioEngine.analyzeRelation(domain, equality)
        assertTrue(relation.reflexive && relation.symmetric && relation.antisymmetric && relation.transitive)
        assertEquals(2, SetTheoryStudioEngine.equivalenceClasses(domain, equality).size)
        assertEquals(setOf(1 to 2, 2 to 4), SetTheoryStudioEngine.hasseCovers(setOf(1, 2, 4)))
        val mapping = SetTheoryStudioEngine.analyzeMapping(domain, setOf("a", "b"), mapOf("1" to "a", "2" to "b"))
        assertTrue(mapping.bijective)
        assertEquals(3, SetTheoryStudioEngine.inclusionExclusion(setOf("1", "2"), setOf("2", "3")))
    }
}
