package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.assistant.local.LocalLearningAssistantProvider
import com.indianservers.aiexplorer.assistant.operations.AssistantAuditTrail
import com.indianservers.aiexplorer.assistant.operations.AssistantOfflineFeatureStatus
import com.indianservers.aiexplorer.assistant.operations.AssistantOfflineHealthEngine
import com.indianservers.aiexplorer.assistant.operations.AssistantPrivacyPayloadBuilder
import com.indianservers.aiexplorer.assistant.operations.AssistantRouteDecisionEngine
import com.indianservers.aiexplorer.assistant.operations.AssistantRouteTarget
import com.indianservers.aiexplorer.assistant.privacy.AssistantConsent
import com.indianservers.aiexplorer.assistant.privacy.SharedDataCategory
import com.indianservers.aiexplorer.assistant.routing.AssistanceNeed
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AssistantPhase5Test {
    @Test fun healthReportSummarizesOfflineAssistantReadiness() {
        val report = AssistantOfflineHealthEngine.assess(
            local = LocalLearningAssistantProvider(),
            service = LocalLearningIntelligenceService(),
            consent = AssistantConsent(cloudEnabled = false),
            now = Instant.parse("2026-07-19T12:00:00Z"),
        )

        assertTrue(report.readyCount >= 4)
        assertTrue(report.features.any { it.id == "cloud" && it.status == AssistantOfflineFeatureStatus.BLOCKED })
        assertTrue(report.summary.contains("ready"))
    }

    @Test fun privacyPayloadRedactsLearnerStepsWithoutConsent() {
        val request = GroundedRequestFactory.local(
            conceptId = "math-linear-equations",
            question = "Check my step",
            mastery = ConceptMasteryState.LEARNING,
            hintLevel = HintLevel.CONCEPT_CUE,
            style = ExplanationStyle.CONCISE,
            steps = listOf(LearnerAnswerStep("x+2=5")),
        )

        val payload = AssistantPrivacyPayloadBuilder.build(
            request,
            consent = AssistantConsent(cloudEnabled = true, learnerStepsMayBeSent = false),
        )

        assertEquals("Check my step", payload.fields["question"])
        assertEquals("[redacted]", payload.fields["learnerSteps"])
        assertTrue("learnerSteps" in payload.redactedFields)
        assertTrue(payload.fields.getValue("groundingReferenceIds").contains("definition-math-linear-equations"))
    }

    @Test fun privacyPayloadAllowsOnlyExplicitStepSharing() {
        val request = GroundedRequestFactory.local(
            "math-linear-equations",
            "Check my step",
            ConceptMasteryState.LEARNING,
            HintLevel.CONCEPT_CUE,
            ExplanationStyle.CONCISE,
            steps = listOf(LearnerAnswerStep("x+2=5")),
        )
        val consent = AssistantConsent(
            cloudEnabled = true,
            learnerStepsMayBeSent = true,
            allowedCategories = setOf(
                SharedDataCategory.CONFIRMED_QUESTION_TEXT,
                SharedDataCategory.CONCEPT_AND_LEVEL,
                SharedDataCategory.SELECTED_LEARNER_STEPS,
            ),
        )

        val payload = AssistantPrivacyPayloadBuilder.build(request, consent)

        assertEquals("x+2=5", payload.fields["learnerSteps"])
        assertFalse("learnerSteps" in payload.redactedFields)
    }

    @Test fun routeDecisionStaysLocalForRoutineAndFallsBackWhenProviderMissing() {
        val local = AssistantRouteDecisionEngine.decide(
            need = AssistanceNeed.ROUTINE,
            selectedProviderId = "openai",
            consent = AssistantConsent(cloudEnabled = true),
        )
        val fallback = AssistantRouteDecisionEngine.decide(
            need = AssistanceNeed.OPEN_ENDED,
            selectedProviderId = "missing",
            consent = AssistantConsent(cloudEnabled = true),
            providers = emptyMap(),
        )

        assertEquals(AssistantRouteTarget.LOCAL, local.target)
        assertEquals(AssistantRouteTarget.FALLBACK, fallback.target)
        assertTrue(fallback.reason.contains("unavailable"))
    }

    @Test fun auditTrailIsTamperEvidentAndExportable() {
        val trail = AssistantAuditTrail()
        trail.record(1000L, "route", "local")
        trail.record(2000L, "verify", "locally authored")

        val original = trail.events()
        val tampered = original.toMutableList().also { it[1] = it[1].copy(detail = "changed") }

        assertTrue(trail.verify())
        assertFalse(trail.verify(tampered))
        assertTrue(trail.exportText().contains("\"route\""))
    }
}
