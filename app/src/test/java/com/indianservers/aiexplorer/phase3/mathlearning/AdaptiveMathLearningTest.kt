package com.indianservers.aiexplorer.phase3.mathlearning

import com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import com.indianservers.aiexplorer.learningintelligence.model.LearnerConfidence
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class AdaptiveMathLearningTest {
    @Test fun `workspace selects relevant concept and exposes evidence without granting mastery`() {
        val engine = AdaptiveMathLearningEngine()
        val session = engine.start(WorkspaceState(module = MathModule.Trigonometry), now = Instant.parse("2026-07-21T10:00:00Z"))

        assertEquals("math-trigonometric-graphs", session.conceptId)
        assertTrue(session.workspaceEvidence.functionCount > 0)
        assertEquals(ConceptMasteryState.INTRODUCED, session.learnerState.masteryState)
        assertEquals(0, session.learnerState.practiceAttemptCount)
    }

    @Test fun `verified independent answer creates practice evidence and proof checkpoints`() {
        val engine = AdaptiveMathLearningEngine()
        val started = engine.start(WorkspaceState(module = MathModule.Manipulatives), "math-linear-equations", seed = 7, now = Instant.parse("2026-07-21T10:00:00Z"))
        val steps = started.task.expectedSteps.map { LearnerAnswerStep(it.statement, it.reason) }
        val result = engine.submit(started, started.task.expectedAnswer, steps, LearnerConfidence.VERY_SURE, Instant.parse("2026-07-21T10:01:00Z"))

        assertTrue(result.validation!!.valid)
        assertTrue(result.proofEvaluation!!.valid)
        assertEquals(1, result.learnerState.practiceAttemptCount)
        assertTrue(result.learnerState.masteryEvidence.isNotEmpty())
        assertNull(result.errorEntry)
    }

    @Test fun `wrong transformation records misconception error and Socratic repair`() {
        val engine = AdaptiveMathLearningEngine()
        val started = engine.start(WorkspaceState(), "math-linear-equations", seed = 3, now = Instant.parse("2026-07-21T10:00:00Z"))
        val result = engine.submit(started, "999", listOf(LearnerAnswerStep("x = 999", "guessed")), LearnerConfidence.VERY_SURE, Instant.parse("2026-07-21T10:01:00Z"))

        assertFalse(result.validation!!.valid)
        assertTrue(result.validation!!.misconceptionCandidateIds.isNotEmpty())
        assertNotNull(result.errorEntry)
        assertNotNull(result.tutorPrompt)
        assertTrue(engine.service.repository.entries().isNotEmpty())
    }

    @Test fun `hint use is tracked and reduces independence of later evidence`() {
        val engine = AdaptiveMathLearningEngine()
        val started = engine.start(WorkspaceState(), "math-linear-equations", seed = 5)
        val hinted = engine.requestHint(started)
        assertNotNull(hinted.hint)
        assertEquals(1, hinted.learnerState.hintUsage.requests)

        val steps = hinted.task.expectedSteps.map { LearnerAnswerStep(it.statement, it.reason) }
        val submitted = engine.submit(hinted, hinted.task.expectedAnswer, steps, LearnerConfidence.FAIRLY_SURE)
        assertTrue(submitted.learnerState.masteryEvidence.last().independence < 1.0)
    }
}
