package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorCatalogue
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculusAdvancedPhase5IntegrationTest {
    private val solver = Phase3SolverEngine()

    @Test
    fun `solver routes jacobian and hessian through verified advanced kernel`() {
        val jacobian = solver.solve("jacobian x^2+y;x+y^2 vars x,y at x=1,y=2")
        val hessian = solver.solve("hessian x^2+3*x*y+2*y^2 vars x,y at x=1,y=2")

        listOf(jacobian, hessian).forEach {
            assertTrue(it.supported)
            assertEquals(ProblemType.Calculus, it.classification.type)
            assertEquals(VerificationStatus.Verified, it.verification.status)
            assertTrue(it.steps.isNotEmpty())
        }
        assertTrue(jacobian.finalAnswer!!.contains("["))
        assertTrue(hessian.methodReason!!.contains("symmetry", ignoreCase = true))
    }

    @Test
    fun `solver routes integration and differential equation phase commands`() {
        val commands = listOf(
            "improper integrate exp(-x) from 0 to infinity",
            "double integrate x+y x 0 to 1 y 0 to 1",
            "linear ivp a 2 b 0 x0 0 y0 3 at 1",
            "rk4 x+y x0 0 y0 1 to 1 step 0.1",
            "ode series lambda 2 x0 0 y0 1 order 5",
        )

        commands.forEach { command ->
            val result = solver.solve(command)
            assertTrue("Expected supported Solver route for $command: ${result.message}", result.supported)
            assertTrue(result.steps.isNotEmpty())
            assertTrue(result.finalAnswer?.isNotBlank() == true)
        }
    }

    @Test
    fun `calculator catalogue exposes every phase five workflow`() {
        val ids = SolverCalculatorCatalogue.presets.filter { it.supported }.map { it.id }.toSet()
        assertTrue(
            ids.containsAll(
                setOf(
                    "improper-integral", "double-integral", "linear-ivp", "rk4", "ode-series",
                    "jacobian", "hessian", "gradient", "directional-derivative", "divergence", "curl", "tangent-plane",
                ),
            ),
        )
    }
}
