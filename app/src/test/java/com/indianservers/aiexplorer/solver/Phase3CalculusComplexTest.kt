package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationData
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3CalculusComplexTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun supportedDerivativesHaveTangentVisualsAndFiniteDifferenceVerification() {
        val inputs = listOf(
            "differentiate x^3",
            "differentiate 4*x^5-2*x+7",
            "differentiate sin(x)",
            "differentiate cos(x)",
            "differentiate exp(x)",
            "differentiate ln(x)",
            "differentiate x*sin(x)",
            "differentiate sin(x^2)",
            "differentiate (x^2+1)/(x+2)",
        )
        inputs.forEach { source ->
            val solution = engine.solve(source)
            assertTrue("$source should be supported: ${solution.message}", solution.supported)
            assertEquals(ProblemType.Calculus, solution.classification.type)
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.visualisations.any { it.type == VisualisationType.DerivativeTangent })
            assertTrue(solution.assumptions.none { it.statement.isBlank() })
        }
    }

    @Test
    fun supportedIntegralsHaveAreaOrTransformationVisualsAndReverseChecks() {
        listOf(
            "integrate x^2",
            "integrate 3*x^2+cos(x)",
            "integrate sin(x)",
            "integrate exp(x)",
            "integrate x^2 from 0 to 2",
        ).forEach { source ->
            val solution = engine.solve(source)
            assertTrue("$source should be supported: ${solution.message}", solution.supported)
            assertEquals(ProblemType.Calculus, solution.classification.type)
            assertTrue(solution.verification.message.isNotBlank())
            assertTrue(solution.visualisations.isNotEmpty())
            if (" from " in source) {
                val area = solution.visualisations.first { it.type == VisualisationType.IntegralArea }
                val data = area.mathematicalData as VisualisationData.IntegralArea
                assertTrue(data.rectangles.isNotEmpty())
                assertTrue(data.rectangles.all { it.height.isFinite() })
            } else {
                assertTrue(solution.assumptions.any { "constant" in it.reason.lowercase() || "arbitrary" in it.statement.lowercase() })
            }
        }
    }

    @Test
    fun removableLimitRetainsPuncturedDomainReasoning() {
        val solution = engine.solve("limit (x^2-4)/(x-2) as x -> 2")
        assertTrue(solution.supported)
        assertEquals(ProblemType.Calculus, solution.classification.type)
        assertTrue(solution.verification.message.isNotBlank())
        assertTrue(solution.steps.isNotEmpty())
    }

    @Test
    fun complexRectangularPolarAndRootsRetainBranchConvention() {
        val values = listOf(
            "complex 3+4i",
            "complex -2+5i",
            "complex multiply 1+2i ; 3-4i",
            "complex roots 1+i order 3",
            "complex roots -1 order 4",
        )
        values.forEach { source ->
            val solution = engine.solve(source)
            assertTrue("$source should be supported: ${solution.message}", solution.supported)
            assertEquals(ProblemType.ComplexNumbers, solution.classification.type)
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.assumptions.any { "argument" in it.statement.lowercase() })
            val plane = solution.visualisations.firstOrNull { it.type == VisualisationType.ComplexPlane }
            assertNotNull("Expected Argand-plane visual for $source", plane)
            val data = plane!!.mathematicalData as VisualisationData.ComplexPlane
            assertTrue(data.points.all { it.x.isFinite() && it.y.isFinite() })
            assertTrue(data.branchConvention.isNotBlank())
        }
    }

    @Test
    fun complexRootsExposeEveryRequestedBranch() {
        (2..10).forEach { order ->
            val solution = engine.solve("complex roots 1+i order $order")
            assertTrue(solution.supported)
            assertTrue(solution.assumptions.any { "k=0" in it.statement })
            val data = solution.visualisations.first { it.type == VisualisationType.ComplexPlane }.mathematicalData as VisualisationData.ComplexPlane
            assertTrue("Expected root points for order $order", data.points.size >= order)
        }
    }

    @Test
    fun invalidCalculusAndComplexRequestsFailClosed() {
        listOf(
            "differentiate",
            "integrate",
            "complex roots 1+i order 0",
            "complex roots 1+i order 25",
            "complex hello",
        ).forEach { source ->
            val solution = engine.solve(source)
            assertFalse("$source must not be presented as correct", solution.canPresentAsCorrect)
            assertTrue(solution.visualisations.isEmpty())
        }
    }
}
