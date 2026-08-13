package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.continuity.AssistantContinuityActionType
import com.indianservers.aiexplorer.assistant.continuity.AssistantContinuityPlanner
import com.indianservers.aiexplorer.assistant.continuity.AssistantContinuityPriority
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.EvidenceIndependence
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import com.indianservers.aiexplorer.learningintelligence.model.LearnerConfidence
import com.indianservers.aiexplorer.learningintelligence.model.LearnerErrorType
import com.indianservers.aiexplorer.learningintelligence.model.LearnerEvent
import com.indianservers.aiexplorer.learningintelligence.model.ReviewUrgency
import com.indianservers.aiexplorer.learningintelligence.model.ValidationResult
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AssistantPhase4Test {
    private val now: Instant = Instant.parse("2026-07-19T12:00:00Z")

    @Test fun reviewCardsExposeUrgentOfflineRepair() {
        val service = LocalLearningIntelligenceService()
        service.emit(
            LearnerEvent.ReviewCompleted(
                conceptId = "physics-velocity-acceleration",
                correct = false,
                independence = EvidenceIndependence.INDEPENDENT,
                confidence = LearnerConfidence.VERY_SURE,
                occurredAt = now,
            ),
        )

        val cards = AssistantContinuityPlanner.reviewCards(service, now)

        assertFalse(cards.isEmpty())
        assertEquals("physics-velocity-acceleration", cards.first().conceptId)
        assertEquals(ReviewUrgency.URGENT_REPAIR, cards.first().urgency)
        assertTrue(cards.first().prompt.contains("Repair"))
    }

    @Test fun retryCardsPrioritizeDueConfidentMistakes() {
        val service = LocalLearningIntelligenceService()
        val concept = LearningIntelligenceCatalog.concepts.single { it.conceptId == "math-linear-equations" }
        val result = ValidationResult(false, 0.0, 0, LearnerErrorType.ALGEBRAIC_TRANSFORMATION, "bad", setOf("math-move-change-sign"))
        service.recordError(
            conceptId = concept.conceptId,
            curriculumNodeId = concept.curriculumNodeId,
            questionId = "q1",
            answer = "x+2=5 -> x=5-2",
            steps = listOf(LearnerAnswerStep("x+2=5 -> x=5-2")),
            result = result,
            confidence = LearnerConfidence.VERY_SURE,
            corrected = listOf("Subtract 2 from both sides."),
            remediationActivityId = concept.activityId,
            now = now.minusSeconds(86_500),
        )

        val card = AssistantContinuityPlanner.retryCards(service, now).first()
        val action = AssistantContinuityPlanner.nextAction(service, now = now)

        assertEquals(AssistantContinuityPriority.URGENT, card.priority)
        assertEquals(AssistantContinuityActionType.RETRY_MISTAKE, action?.type)
        assertEquals("math-linear-equations", action?.conceptId)
    }

    @Test fun recapCountsDueReviewsAndOpenMistakes() {
        val service = LocalLearningIntelligenceService()
        val concept = LearningIntelligenceCatalog.concepts.single { it.conceptId == "math-linear-equations" }
        service.emit(LearnerEvent.ReviewCompleted("biology-cell-structure", false, EvidenceIndependence.INDEPENDENT, LearnerConfidence.VERY_SURE, now))
        service.recordError(
            concept.conceptId,
            concept.curriculumNodeId,
            "q2",
            "moved term",
            listOf(LearnerAnswerStep("x+2=5 -> x=5-2")),
            ValidationResult(false, 0.0, 0, LearnerErrorType.ALGEBRAIC_TRANSFORMATION, "bad", setOf("math-move-change-sign")),
            LearnerConfidence.UNSURE,
            listOf("Subtract 2 from both sides."),
            concept.activityId,
            now.minusSeconds(86_500),
        )

        val recap = AssistantContinuityPlanner.recap(service, now)

        assertEquals("Offline learning recap", recap.title)
        assertEquals(1, recap.dueReviewCount)
        assertEquals(1, recap.openMistakeCount)
        assertTrue(recap.highlights.isNotEmpty())
        assertEquals(AssistantContinuityActionType.RETRY_MISTAKE, recap.nextAction?.type)
    }

    @Test fun nextActionFallsBackToLearningRecommendationWhenNoUrgentRetryExists() {
        val service = LocalLearningIntelligenceService()

        val action = AssistantContinuityPlanner.nextAction(service, timeMinutes = 12, now = now)

        assertTrue(action != null)
        assertTrue(action!!.type in setOf(AssistantContinuityActionType.REPAIR_PREREQUISITE, AssistantContinuityActionType.CONTINUE_LESSON, AssistantContinuityActionType.PRACTICE_VARIANT))
        assertTrue(action.minutes in 3..20)
        assertFalse(action.reason.isBlank())
    }
}
