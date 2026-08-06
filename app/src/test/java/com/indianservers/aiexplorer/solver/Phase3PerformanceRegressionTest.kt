package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase2SolverEngine
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.testing.SolverPerformanceProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3PerformanceRegressionTest {
    @Test
    fun phaseTwoResultsRemainStableThroughPhaseThreeFacade() {
        val phase2 = Phase2SolverEngine()
        val phase3 = Phase3SolverEngine()
        val inputs = buildList {
            (1..30).forEach { add("$it + ${it + 2}") }
            (1..20).forEach { add("2x + $it = ${it + 8}") }
            addAll(
                listOf(
                    "gcd(84,30)",
                    "lcm(12,18)",
                    "distance(0,0,3,4)",
                    "midpoint(0,2,4,6)",
                    "sin(30deg)",
                    "matrixadd([[1,2],[3,4]],[[2,3],[4,5]])",
                    "arithmeticterm(3,2,10)",
                    "geometricsum(2,1/2,5)",
                ),
            )
        }
        inputs.forEach { source ->
            val previous = phase2.solve(source)
            val current = phase3.solve(source)
            assertEquals("Support changed for $source", previous.supported, current.supported)
            assertEquals("Answer changed for $source", previous.exactAnswer, current.exactAnswer)
            assertEquals("Verification changed for $source", previous.verification.status, current.verification.status)
        }
    }

    @Test
    fun localPipelinePerformanceIsBoundedForRepresentativeWorkloads() {
        val engine = Phase3SolverEngine()
        val probe = SolverPerformanceProbe()
        val inputs = listOf(
            "27 + 38",
            "3x + 5 = 20",
            "x^2-5x+6=0",
            "differentiate x^3+2*x",
            "integrate x^2 from 0 to 3",
            "complex roots 1+i order 5",
            "matrixmultiply([[1,2],[3,4]],[[2,0],[1,2]])",
        )
        repeat(8) {
            inputs.forEach { source ->
                val solution = probe.measure("solve-and-specify") { engine.solve(source) }
                assertTrue(solution.verification.status != VerificationStatus.Failed)
            }
        }
        val summary = probe.summaries().single()
        assertTrue("Median ${summary.medianMillis}ms is unexpectedly high", summary.medianMillis < 500.0)
        assertTrue("P95 ${summary.p95Millis}ms is unexpectedly high", summary.p95Millis < 2_000.0)
    }

    @Test
    fun visualCacheMakesRepeatedGenerationStable() {
        val engine = Phase3SolverEngine()
        val first = engine.solve("differentiate x^4-3*x")
        repeat(30) {
            val next = engine.solve("differentiate x^4-3*x")
            assertEquals(first.visualisations, next.visualisations)
        }
    }
}
