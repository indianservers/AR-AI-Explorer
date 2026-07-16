package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class Phase3FiveStarSolverTest {
    private val tutor = MathSolverTutor()

    @Test fun interpreterNormalizesNotationAndStatesAssumptions() {
        val interpreted = DeterministicMathQueryInterpreter.interpret("What is log(100) × 2²")
        assertEquals(MathQueryIntent.Evaluate, interpreted.selected.intent)
        assertEquals("log(100) * 2^2", interpreted.selected.normalizedQuery)
        assertEquals(InterpretationStatus.AssumptionRequired, interpreted.status)
        assertTrue(interpreted.assumptions.any { it.contains("base 10") })
    }

    @Test fun interpreterCitesAmbiguousInverseTrigInsteadOfGuessingSilently() {
        val interpreted = DeterministicMathQueryInterpreter.interpret("simplify sin^-1(x)")
        assertEquals(InterpretationStatus.NeedsClarification, interpreted.status)
        assertEquals(2, interpreted.alternatives.size)
        assertTrue(interpreted.ambiguities.single().contains("arcsin"))
    }

    @Test fun guidedSolutionExposesAlternateMethodsAndResultForms() {
        val guided = tutor.solve("x^2 - 5x + 6 = 0")
        assertTrue(guided.alternatives.any { it.method == SolverMethod.CompletingSquare })
        assertTrue(guided.alternatives.any { it.method == SolverMethod.QuadraticFormula })
        assertTrue(guided.resultForms.any { it.kind == SolverResultKind.Exact && it.available })
        assertTrue(guided.resultForms.any { it.kind == SolverResultKind.Verification && it.value.isNotBlank() })
        assertTrue(guided.resultForms.any { it.kind == SolverResultKind.Graph && it.available })
    }

    @Test fun exactFractionGetsDerivedDecimalWithoutReplacingAuthority() {
        val guided = tutor.solve("Calculate 1/3 + 1/6")
        assertEquals("1/2", guided.solution.answer)
        assertEquals("0.500000000000", guided.resultForms.first { it.kind == SolverResultKind.Decimal }.value)
        assertEquals("1/2", guided.resultForms.first { it.kind == SolverResultKind.Exact }.value)
    }

    @Test fun taylorExpansionRespectsRequestedOrderAndRemainder() {
        val result = tutor.solve("Maclaurin series of cos(x) through order 6")
        assertTrue(result.solution.answer.contains("x^6/6!"))
        assertTrue(result.solution.answer.contains("O(x^7)"))
        assertTrue(result.solution.verification.contains("degree 6"))
    }

    @Test fun twoSidedLimitIsApproximateAndCarriesResidualEvidence() {
        val result = tutor.solve("Limit (sin(x)/x) as x -> 0")
        assertTrue(result.solution.supported)
        assertTrue(result.solution.answer.contains("1"))
        assertTrue(result.solution.answer.startsWith("≈"))
        assertTrue(result.solution.verification.contains("residual"))
    }

    @Test fun divergentTwoSidedLimitRefusesSafely() {
        val result = tutor.solve("Limit (1/x) as x -> 0")
        assertFalse(result.solution.supported)
        assertTrue(result.solution.verification.contains("left and right"))
    }

    @Test fun linearOdeUsesInitialConditionAndVerifiesFamily() {
        val result = tutor.solve("Solve differential equation dy/dx = 2y + 4, y(0)=3")
        assertTrue(result.solution.supported)
        assertTrue(result.solution.answer.contains("-2"))
        assertTrue(result.solution.answer.contains("5*exp(2*x)"))
        assertTrue(result.solution.verification.contains("initial value"))
    }

    @Test fun rectangleWordProblemShowsModelBeforeCalculation() {
        val result = tutor.solve("A rectangle has length 8 and width 5. Find its area")
        assertNotNull(result.wordModel)
        assertEquals(listOf("x"), result.wordModel?.unknowns)
        assertTrue(result.wordModel?.relationships?.single()?.contains("area") == true)
        assertEquals("x = 40", result.solution.answer)
    }
}
