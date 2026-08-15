package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMultivariableCalculusTest {
    private val calculator = AdvancedScientificCalculator()

    @Test fun partialDerivativesAndGradientsAreSymbolicAndPointAware() {
        val partial = calculator.evaluate("partial derivative x^2*y + sin(y) with respect to x")!!
        val gradient = calculator.evaluate("gradient x^2*y + sin(y) at x=2,y=0")!!

        assertEquals("2*x*y", partial.exact)
        assertTrue(gradient.exact!!.startsWith("["))
        assertTrue(gradient.exact!!.contains("2*x*y"))
        assertTrue(gradient.alternatives.any { it.first == "At point" && it.second == "[0, 5]" })
        assertTrue(partial.verification.contains("centred differences"))
    }

    @Test fun directionalDerivativeNormalizesDirectionBeforeDotProduct() {
        val result = calculator.evaluate("directional derivative x^2+y^2 at x=1,y=2 direction 3,4")!!

        assertEquals(4.4, result.primary.toDouble(), 1e-9)
        assertTrue(result.alternatives.any { it.first == "Unit direction" && it.second == "[0.6, 0.8]" })
        assertTrue(result.steps.any { it.contains("Normalize") })
    }

    @Test fun divergenceCurlAndTangentPlaneExposeDerivations() {
        val divergence = calculator.evaluate("divergence x^2; y^2 at x=1,y=2")!!
        val curl = calculator.evaluate("curl -y; x at x=1,y=2")!!
        val plane = calculator.evaluate("tangent plane x^2+y^2 at x=1,y=2")!!

        assertTrue(divergence.alternatives.any { it.first == "At point" && it.second == "6" })
        assertTrue(curl.alternatives.any { it.first == "At point" && it.second == "2" })
        assertEquals("z - 5 = 2*(x - 1) + 4*(y - 2)", plane.primary)
        assertTrue(plane.alternatives.any { it.first == "Normal" && it.second == "[-2, -4, 1]" })
    }

    @Test fun solverRoutesAndIndependentlyVerifiesMultivariableCommands() {
        val engine = Phase3SolverEngine()
        listOf(
            "partial derivative x^2*y + sin(y) with respect to x",
            "gradient x^2*y + sin(y)",
            "divergence x^2; y^2",
            "curl -y; x",
        ).forEach { source ->
            val solution = engine.solve(source)
            assertTrue("$source: ${solution.message}", solution.supported)
            assertEquals(ProblemType.Calculus, solution.classification.type)
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.steps.isNotEmpty())
            assertNotNull(solution.finalAnswer)
        }
    }

    @Test fun zeroDirectionAndIncompletePointsFailClosed() {
        listOf(
            "directional derivative x^2+y^2 at x=1,y=2 direction 0,0",
            "directional derivative x^2+y^2 at x=1 direction 1,1",
            "tangent plane x^2+y^2 at x=1",
        ).forEach { source ->
            assertTrue(runCatching { calculator.evaluate(source) }.isFailure)
        }
    }
}
