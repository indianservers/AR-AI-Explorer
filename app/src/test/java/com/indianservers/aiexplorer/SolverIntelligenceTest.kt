package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.MistakeKind
import com.indianservers.aiexplorer.core.ProblemKind
import com.indianservers.aiexplorer.core.SolverDestination
import com.indianservers.aiexplorer.core.SolverMethod
import com.indianservers.aiexplorer.core.SolverReveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverIntelligenceTest {
    private val tutor = MathSolverTutor()

    @Test
    fun selectsMethodAndKeepsAnswerHiddenInHintMode() {
        val guided = tutor.solve("x^2 - 5x + 6 = 0")

        assertEquals(SolverMethod.Factoring, guided.method)
        assertFalse(guided.answerVisible(SolverReveal.FirstHint))
        assertEquals(1, guided.visibleSteps(SolverReveal.FirstHint, 1).size)
        assertTrue(guided.why(0).isNotBlank())
        assertTrue(guided.solution.verification.contains("0 and 0"))
    }

    @Test
    fun supportsAlternateQuadraticMethodsAndRejectsInvalidOnes() {
        val square = tutor.solve("x^2 - 5x + 6 = 0", SolverMethod.CompletingSquare)
        val invalid = tutor.solve("Solve 2x + 3 = 11", SolverMethod.IntegrationParts)

        assertTrue(square.solution.supported)
        assertTrue(square.solution.steps.any { it.title.contains("complete the square") })
        assertFalse(invalid.solution.supported)
        assertTrue(invalid.solution.verification.contains("not valid"))
    }

    @Test
    fun modelsWordProblemBeforeSolving() {
        val guided = tutor.solve("Find time when distance 120 km and speed 60 km")

        val model = guided.wordModel
        assertNotNull(model)
        assertTrue(requireNotNull(model).relationships.contains("distance = speed × time"))
        assertEquals(listOf("t"), model.unknowns)
        assertTrue(guided.solution.supported)
        assertEquals("x = 2", guided.solution.answer)
    }

    @Test
    fun solvesExtendedCurriculumFoundationsDeterministically() {
        val limit = tutor.solve("Limit (x^2 - 9)/(x - 3) as x -> 3")
        val combination = tutor.solve("Combination 10 C 3")
        val finance = tutor.solve("Compound interest principal 10000 rate 8% time 3")
        val ode = tutor.solve("Solve differential equation dy/dx = 3y")
        val series = tutor.solve("Maclaurin series of sin")
        val numberTheory = tutor.solve("gcd 84 and 30")
        val uncertainty = tutor.solve("uncertainty 2.0 +/- 0.1 + 3.0 +/- 0.2")

        assertEquals("6", limit.solution.answer)
        assertEquals("120", combination.solution.answer)
        assertTrue(finance.solution.answer.contains("12597.12"))
        assertTrue(ode.solution.answer.contains("exp(3*x)"))
        assertTrue(series.solution.answer.contains("x^3/3!"))
        assertEquals("6", numberTheory.solution.answer)
        assertTrue(uncertainty.solution.answer.startsWith("5 ±"))
    }

    @Test
    fun diagnosesMistakesAndBuildsWorkspaceHandoffs() {
        val guided = tutor.solve("Solve 2x + 3 = 11")
        val issues = tutor.diagnose("Solve 2x + 3 = 11", "(2x + 3 = 11, therefore x = -4")

        assertTrue(issues.any { it.kind == MistakeKind.Bracket })
        assertTrue(issues.any { it.kind == MistakeKind.Sign })
        assertTrue(guided.handoffs.any { it.destination == SolverDestination.Graph && it.enabled })
        assertTrue(guided.handoffs.any { it.destination == SolverDestination.Notebook })
        assertTrue(guided.handoffs.any { it.destination == SolverDestination.Mcq })
    }

    @Test
    fun numericAndExactOutputsRemainVerified() {
        val result = tutor.solve("Integrate exp(-x^2) from 0 to 1", SolverMethod.NumericApproximation)

        assertEquals(ProblemKind.Integral, result.solution.kind)
        assertTrue(result.solution.supported)
        assertTrue(result.solution.verification.contains("reversed limits"))
        assertTrue(result.solution.steps.any { it.title.contains("numeric approximation") })
    }
}
