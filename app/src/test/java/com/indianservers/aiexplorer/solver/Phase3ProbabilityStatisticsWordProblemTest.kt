package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3ProbabilityStatisticsWordProblemTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun descriptiveStatisticsAreStructuredAndVerified() {
        val solution = engine.solve("stats 2, 3, 5, 8")
        assertTrue(solution.supported)
        assertEquals(ProblemType.Statistics, solution.classification.type)
        assertEquals(VerificationStatus.Verified, solution.verification.status)
        assertTrue(solution.finalAnswer!!.contains("mean=4.5"))
        assertTrue(solution.steps.size >= 3)
    }

    @Test
    fun probabilityCommandsCheckBoundsAndIndependentEvaluation() {
        val solution = engine.solve("binomial pmf 3 n 10 p 0.5")
        assertTrue(solution.supported)
        assertEquals(ProblemType.Probability, solution.classification.type)
        assertEquals(VerificationStatus.Verified, solution.verification.status)
        assertTrue(solution.verification.checks.size >= 2)
    }

    @Test
    fun pairedRegressionAndCombinatoricsUseExactSchemas() {
        val regression = engine.solve("regression x: 1,2,3; y: 2,4,5")
        assertTrue(regression.supported)
        assertEquals(ProblemType.Statistics, regression.classification.type)
        val combinations = engine.solve("ncr(10,3)")
        assertTrue(combinations.supported)
        assertEquals("120", combinations.finalAnswer)
    }

    @Test
    fun speedSchemaPreservesUnitsAndReverseChecks() {
        listOf(
            "speed distance 240 km time 3 h",
            "A train travels 240 km in 3 hours. What is its average speed?",
        ).forEach { source ->
            val solution = engine.solve(source)
            assertTrue("$source: ${solution.message}", solution.supported)
            assertEquals("80 km/h", solution.finalAnswer)
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.steps.any { it.optionalDetails.any { detail -> detail.value == "Normalize units" } })
        }
    }

    @Test
    fun invalidStructuredInputsFailClosed() {
        assertFalse(engine.solve("binomial pmf 3 n 2 p 0.5").supported)
        assertFalse(engine.solve("speed distance 10 km time 0 h").supported)
    }
}
