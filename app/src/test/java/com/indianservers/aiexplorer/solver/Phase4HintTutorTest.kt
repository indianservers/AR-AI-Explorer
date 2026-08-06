package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.tutor.EquivalenceStatus
import com.indianservers.aiexplorer.solver.domain.tutor.HintLevel
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepStatus
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHintEngine
import com.indianservers.aiexplorer.solver.domain.tutor.SolverStepEvaluationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4HintTutorTest {
    private val engine = Phase3SolverEngine()
    private val evaluator = SolverStepEvaluationEngine()

    @Test
    fun sixHintsProgressWithoutPrematureAnswerReveal() {
        val solution = engine.solve("3x+5=20")
        ExplanationProfile.entries.forEach { profile ->
            val hints = SolverHintEngine.hints(solution, 0, profile)
            assertEquals(HintLevel.entries.toList(), hints.map { it.level })
            assertTrue(hints.take(5).none { it.revealsAnswer })
            assertTrue(hints.last().revealsAnswer)
            assertTrue(hints.all { it.text.isNotBlank() && it.relatedStepId != null })
            assertFalse(hints.first().text.contains(solution.finalAnswer.orEmpty(), ignoreCase = true))
        }
    }

    @Test
    fun verifiedExpectedStepsAreAccepted() {
        listOf("3x+5=20", "2(x+3)+4x", "3/4+5/6", "-2x+3<=9").forEach { source ->
            val solution = engine.solve(source)
            solution.steps.forEachIndexed { index, step ->
                val input = SolverExpressionRenderer.render(step.after)
                val evaluation = evaluator.evaluate(solution, index, input)
                assertEquals("$source step $index", LearnerStepStatus.CorrectNextStep, evaluation.status)
                assertEquals(EquivalenceStatus.Equivalent, evaluation.equivalence)
            }
        }
    }

    @Test
    fun largeVerifiedJumpIsAcceptedButNamed() {
        val solution = engine.solve("3x+5=20")
        val evaluation = evaluator.evaluate(solution, 0, solution.finalAnswer!!)
        assertEquals(LearnerStepStatus.CorrectLargeJump, evaluation.status)
        assertTrue(evaluation.feedback.explanation.contains("skipped", true))
    }

    @Test
    fun oneSidedEquationOperationGetsSpecificFeedback() {
        val solution = engine.solve("3x+5=20")
        val evaluation = evaluator.evaluate(solution, 0, "3x=20")
        assertEquals(LearnerStepStatus.RuleMisuse, evaluation.status)
        assertEquals("one-sided-operation", evaluation.misconception?.id)
        assertTrue(evaluation.feedback.explanation.contains("both sides", true))
    }

    @Test
    fun variableDivisionRequiresZeroCase() {
        val solution = engine.solve("x^2=x")
        val evaluation = evaluator.evaluate(solution, 0, "x^2/x=x/x")
        assertEquals(LearnerStepStatus.DomainViolation, evaluation.status)
        assertEquals(EquivalenceStatus.ConditionallyEquivalent, evaluation.equivalence)
        assertNotNull(evaluation.suggestedRecovery)
    }

    @Test
    fun squareOfSumMutationIsRejectedWithCounterRule() {
        val solution = engine.solve("(x+2)^2")
        val evaluation = evaluator.evaluate(solution, 0, "(x+2)^2=x^2+4")
        assertEquals(LearnerStepStatus.RuleMisuse, evaluation.status)
        assertEquals("square-of-sum", evaluation.misconception?.id)
        assertTrue(evaluation.feedback.explanation.contains("cross", true))
    }

    @Test
    fun malformedTutorInputIsAmbiguousRatherThanWrong() {
        val solution = engine.solve("3x+5=20")
        val evaluation = evaluator.evaluate(solution, 0, "3x+(")
        assertEquals(LearnerStepStatus.AmbiguousInput, evaluation.status)
        assertEquals(EquivalenceStatus.Unknown, evaluation.equivalence)
    }
}

