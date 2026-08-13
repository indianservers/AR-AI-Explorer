package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.assistant.tutoring.AssistantExamAnswerFormatter
import com.indianservers.aiexplorer.assistant.tutoring.AssistantHintLadderBuilder
import com.indianservers.aiexplorer.assistant.tutoring.AssistantPracticePromptGenerator
import com.indianservers.aiexplorer.assistant.tutoring.AssistantStepCheckEngine
import com.indianservers.aiexplorer.assistant.tutoring.MisconceptionAwareAssistant
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantSummarizer
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPhase3Test {
    @Test fun hintLadderRespectsMaximumAllowedHintLevel() {
        val request = GroundedRequestFactory.local(
            "math-linear-equations",
            "give me a hint",
            ConceptMasteryState.LEARNING,
            HintLevel.CONCEPT_CUE,
            ExplanationStyle.CONCISE,
        )

        val ladder = AssistantHintLadderBuilder.from(request)

        assertFalse(ladder.hints.isEmpty())
        assertTrue(ladder.hints.all { it.level.ordinal <= HintLevel.CONCEPT_CUE.ordinal })
        assertTrue(ladder.hints.none { it.revealsAnswer })
    }

    @Test fun stepCheckFindsFirstInvalidLineCompactly() {
        val result = AssistantStepCheckEngine().check(
            expectedSteps = listOf("2x=6", "x=3"),
            learnerSteps = listOf(LearnerAnswerStep("2x=6"), LearnerAnswerStep("x=4")),
            misconceptionIds = setOf("math-move-change-sign"),
        )

        assertFalse(result.correctSoFar)
        assertEquals(1, result.firstInvalidIndex)
        assertTrue("math-move-change-sign" in result.misconceptionIds)
        assertTrue("Step 2" in result.feedback)
    }

    @Test fun stepCheckFlagsOneSidedEquationOperation() {
        val result = AssistantStepCheckEngine().check(
            expectedSteps = listOf("x=3"),
            learnerSteps = listOf(LearnerAnswerStep("x+2=3")),
        )

        assertFalse(result.correctSoFar)
        assertEquals(setOf("one-sided-operation"), result.misconceptionIds)
        assertTrue(result.repairPrompt.contains("both sides", ignoreCase = true))
    }

    @Test fun misconceptionReplyUsesDetectedPattern() {
        val request = GroundedRequestFactory.local(
            "math-linear-equations",
            "why is this wrong?",
            ConceptMasteryState.LEARNING,
            HintLevel.PARTIAL_STEP,
            ExplanationStyle.STEP_BY_STEP,
        )
        val stepCheck = AssistantStepCheckEngine().check(
            expectedSteps = listOf("x=3"),
            learnerSteps = listOf(LearnerAnswerStep("x+2=3")),
        )

        val reply = MisconceptionAwareAssistant.reply(request, stepCheck)

        assertEquals("One sided operation", reply?.title)
        assertTrue(reply!!.explanation.contains("both sides", ignoreCase = true))
    }

    @Test fun examFormatterBuildsExpectedSections() {
        val state = WorkspaceState(
            module = MathModule.Geometry2D,
            points = listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 3.0)),
            shapes = listOf(Shape2D("tri-1", Shape2DType.Triangle, listOf(0, 1, 2), "Triangle A")),
        )
        val summary = WorkspaceAssistantSummarizer.summarize(state, selectedShapeIndex = 0)
        val draft = AssistantExamAnswerFormatter.from(summary, listOf("Use A = 1/2 x base x height.", "A = 1/2 x 4 x 3 = 6."), "6 square units")
        val text = draft.asPlainText()

        assertTrue(text.contains("Given:"))
        assertTrue(text.contains("Rule:"))
        assertTrue(text.contains("Working:"))
        assertTrue(text.contains("Answer:"))
        assertTrue(text.contains("Check:"))
    }

    @Test fun practicePromptGenerationIsDeterministicAndVerifiedOffline() {
        val generator = AssistantPracticePromptGenerator()
        val first = generator.generate("math-linear-equations", 42)
        val second = generator.generate("math-linear-equations", 42)
        val different = generator.generate("math-linear-equations", 43)

        assertEquals(first, second)
        assertTrue(first.prompt.contains("Solve"))
        assertTrue(first.validationNote.contains("Verified offline"))
        assertTrue(first.prompt != different.prompt || first.expectedAnswer != different.expectedAnswer)
    }
}
