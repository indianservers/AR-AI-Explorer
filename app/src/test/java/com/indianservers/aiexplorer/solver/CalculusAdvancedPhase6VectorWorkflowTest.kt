package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorCatalogue
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculusAdvancedPhase6VectorWorkflowTest {
    private val solver = Phase3SolverEngine()

    @Test fun constrainedAndOrientedIntegralsAreSolverWorkflows() {
        val commands = listOf(
            "lagrange x+y constraint x^2+y^2 level 1 seeds 1,1|-1,-1|1,-1",
            "line integral 1 curve cos(t);sin(t) from 0 to 6.283185307179586",
            "work integral -y;x curve cos(t);sin(t) from 0 to 6.283185307179586",
            "surface flux 0;0;1 surface u;v;0 u 0 to 2 v 0 to 3",
        )
        commands.forEach { command ->
            val result = solver.solve(command)
            assertTrue("Unsupported: $command -> ${result.message}", result.supported)
            assertEquals(VerificationStatus.Verified, result.verification.status)
            assertTrue(result.steps.isNotEmpty())
        }
    }

    @Test fun fundamentalVectorTheoremsReturnPassingCertificates() {
        val commands = listOf(
            "green -y;x x -1 to 1 y -1 to 1",
            "gauss x;y;z x 0 to 1 y 0 to 1 z 0 to 1",
            "stokes -y;x;0 x -1 to 1 y -1 to 1",
        )
        commands.forEach { command ->
            val result = solver.solve(command)
            assertTrue(result.supported)
            assertEquals(VerificationStatus.Verified, result.verification.status)
            assertTrue(result.methodReason!!.contains("compares", true))
        }
    }

    @Test fun phaseSixToolsAreDiscoverable() {
        val ids = SolverCalculatorCatalogue.presets.filter { it.supported }.map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("lagrange", "line-integral", "work-integral", "surface-flux", "green-theorem", "gauss-theorem", "stokes-theorem")))
    }
}
