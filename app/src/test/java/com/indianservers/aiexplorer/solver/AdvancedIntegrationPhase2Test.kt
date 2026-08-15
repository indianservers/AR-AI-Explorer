package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.AdvancedIntegrationEngine
import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.IntegralConvergence
import com.indianservers.aiexplorer.core.IntegrationBound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedIntegrationPhase2Test {
    private val engine = AdvancedIntegrationEngine()

    @Test fun improperIntegralCertifiesConvergenceAndRejectsDivergence() {
        val convergent = engine.improperToPositiveInfinity("1/x^2", "x", 1.0)
        val divergent = engine.improperToPositiveInfinity("1/x", "x", 1.0)

        assertEquals(IntegralConvergence.Convergent, convergent.convergence)
        assertEquals(1.0, convergent.value!!, 5e-4)
        assertEquals(IntegralConvergence.Divergent, divergent.convergence)
        assertTrue(divergent.value == null)
    }

    @Test fun doubleAndTripleIntegralsRespectDeclaredOrderAndBounds() {
        val double = engine.doubleIntegral("x+y", IntegrationBound("x", 0.0, 1.0), IntegrationBound("y", 0.0, 1.0))
        val triple = engine.tripleIntegral("x+y+z", IntegrationBound("x", 0.0, 1.0), IntegrationBound("y", 0.0, 1.0), IntegrationBound("z", 0.0, 1.0))

        assertEquals(1.0, double.value!!, 1e-7)
        assertEquals(1.5, triple.value!!, 1e-6)
        assertTrue(double.verification.contains("differs"))
    }

    @Test fun parameterizedIntegralKeepsParameterSeparateFromIntegrationVariable() {
        val result = engine.parameterized("a*x", IntegrationBound("x", 0.0, 1.0), "a", listOf(0.0, 2.0, 4.0))

        assertTrue(result.continuousOnSamples)
        assertEquals(0.0, result.samples.getValue(0.0).value!!, 1e-9)
        assertEquals(1.0, result.samples.getValue(2.0).value!!, 1e-8)
        assertEquals(2.0, result.samples.getValue(4.0).value!!, 1e-8)
    }

    @Test fun scientificCalculatorRoutesAdvancedIntegrationCommands() {
        val improper = AdvancedScientificCalculator().evaluate("improper integrate 1/x^2 from 1 to infinity")!!
        val multiple = AdvancedScientificCalculator().evaluate("double integrate x+y x 0 to 1 y 0 to 1")!!

        assertEquals("1", improper.primary)
        assertTrue(improper.alternatives.any { it.first == "Convergence" && it.second == "Convergent" })
        assertEquals("1", multiple.primary)
    }
}
