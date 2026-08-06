package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase1SolverEngine
import com.indianservers.aiexplorer.solver.domain.engine.Phase2SolverEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2RegressionBoundaryTest {
    @Test
    fun phase1ExactAnswersRemainUnchangedAcrossPhase2Facade() {
        val phase1 = Phase1SolverEngine()
        val phase2 = Phase2SolverEngine()
        val inputs = buildList {
            for (i in 1..50) add("$i + ${i + 1} * 2")
            for (i in 1..30) add("${i}x + 3 = ${i * 2 + 3}")
            addAll(listOf("3/4+5/6", "25% of 80", "12:18", "-2x+3<=9", "x+y=7;x-y=1"))
        }
        inputs.forEach { input ->
            val old = phase1.solve(input)
            val upgraded = phase2.solve(input)
            assertEquals(input, old.finalAnswer, upgraded.finalAnswer)
            assertEquals(input, old.verification.status, upgraded.verification.status)
            assertTrue(input, upgraded.supported)
        }
    }
}
