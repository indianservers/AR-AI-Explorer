package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorCatalogue
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculusAdvancedAllPendingPhasesClosureTest {
    private val solver = Phase3SolverEngine()

    @Test fun phaseSevenCompletesAdvancedIntegrationRouting() {
        val commands = listOf(
            "improper integrate exp(-x^2) from -infinity to infinity",
            "triple integrate x+y+z x 0 to 1 y 0 to 1 z 0 to 1",
            "parameter integral a*x x 0 to 1 parameter a values 1,2,3",
        )
        commands.forEach(::assertVerifiedWorkflow)
    }

    @Test fun phaseEightCompletesOdeSystemsAndTransforms() {
        val commands = listOf(
            "logistic rate 1 capacity 100 x0 0 y0 10 at 2",
            "second order ivp a 0 b 1 x0 0 y0 0 v0 1 at 1.5707963267948966",
            "system rk4 y;-x initial 1,0 from 0 to 1.5707963267948966 step 0.02",
            "laplace sin(2*x)",
        )
        commands.forEach(::assertVerifiedWorkflow)
    }

    @Test fun phaseNineCompletesDerivativeApplicationsAndLineTools() {
        val analysis = solver.solve("derivative analysis x^3-3*x from -3 to 3")
        val tangent = solver.solve("tangent x^2 at x=1")
        val normal = solver.solve("normal x^2 at x=1")
        listOf(analysis, tangent, normal).forEach {
            assertTrue(it.supported)
            assertEquals(VerificationStatus.Verified, it.verification.status)
            assertTrue(it.steps.last().optionalDetails.isNotEmpty())
        }
        assertTrue(tangent.finalAnswer!!.contains("2*(x - 1)"))
        assertTrue(normal.finalAnswer!!.contains("-0.5*(x - 1)"))
    }

    @Test fun closureCatalogueHasNoPendingCalculusEntries() {
        val calculusGroups = SolverCalculatorCatalogue.presets.filter {
            it.group.name in setOf("Calculus", "DifferentialEquations", "MultivariableVectorCalculus")
        }
        assertTrue(calculusGroups.isNotEmpty())
        assertTrue(calculusGroups.all { it.supported })
    }

    private fun assertVerifiedWorkflow(command: String) {
        val result = solver.solve(command)
        assertTrue("Unsupported $command: ${result.message}", result.supported)
        assertEquals("Unverified $command: ${result.verification.message}", VerificationStatus.Verified, result.verification.status)
        assertTrue(result.steps.isNotEmpty())
        assertTrue(result.steps.last().optionalDetails.isNotEmpty())
    }
}
