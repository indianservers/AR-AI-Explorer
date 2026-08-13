package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.contracts.AssistantConversationMemory
import com.indianservers.aiexplorer.assistant.contracts.AssistantIntent
import com.indianservers.aiexplorer.assistant.contracts.AssistantMemoryEvent
import com.indianservers.aiexplorer.assistant.contracts.AssistantMemoryEventType
import com.indianservers.aiexplorer.assistant.contracts.AssistantVerificationBadge
import com.indianservers.aiexplorer.assistant.contracts.AssistantVerificationBadgeTone
import com.indianservers.aiexplorer.assistant.contracts.AssistantVerificationStatus
import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.assistant.local.LocalLearningAssistantProvider
import com.indianservers.aiexplorer.assistant.offline.AssistantConversationMemoryReducer
import com.indianservers.aiexplorer.assistant.offline.LocalAssistantKnowledgeIndex
import com.indianservers.aiexplorer.assistant.offline.OfflineAssistantIntentClassifier
import com.indianservers.aiexplorer.assistant.routing.AssistanceNeed
import com.indianservers.aiexplorer.assistant.routing.AssistantRouter
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPhase1Test {
    @Test fun offlineIntentClassifierHandlesCoreNeeds() {
        assertEquals(AssistantIntent.HINT, OfflineAssistantIntentClassifier.classify("I am stuck, give me a small hint"))
        assertEquals(AssistantIntent.WORKSPACE_ACTION, OfflineAssistantIntentClassifier.classify("delete selected object"))
        assertEquals(AssistantIntent.OCR_REPAIR, OfflineAssistantIntentClassifier.classify("camera scan read this wrong"))
        assertEquals(AssistantIntent.VIVA, OfflineAssistantIntentClassifier.classify("ask me viva questions"))
    }

    @Test fun localKnowledgeSearchFindsReviewedConceptMaterial() {
        val results = LocalAssistantKnowledgeIndex().search("linear equation sign", conceptId = "math-linear-equations")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.conceptId == "math-linear-equations" })
        assertTrue(results.first().score > 0.0)
    }

    @Test fun verificationBadgeMatchesStatusTone() {
        assertEquals(AssistantVerificationBadgeTone.TRUSTED, AssistantVerificationBadge.from(AssistantVerificationStatus.LOCALLY_AUTHORED).tone)
        assertEquals(AssistantVerificationBadgeTone.CAUTION, AssistantVerificationBadge.from(AssistantVerificationStatus.FALLBACK_USED).tone)
        assertEquals(AssistantVerificationBadgeTone.BLOCKED, AssistantVerificationBadge.from(AssistantVerificationStatus.REJECTED).tone)
    }

    @Test fun conversationMemoryKeepsRecentContextOffline() {
        val memory = listOf(
            AssistantMemoryEvent(AssistantMemoryEventType.CONCEPT_OPENED, conceptId = "math-linear-equations", text = "Linear equations"),
            AssistantMemoryEvent(AssistantMemoryEventType.OBJECT_SELECTED, selectedObjectId = "shape-7", text = "Triangle"),
            AssistantMemoryEvent(AssistantMemoryEventType.MISTAKE_DETECTED, conceptId = "math-linear-equations", text = "math-move-change-sign"),
            AssistantMemoryEvent(AssistantMemoryEventType.QUESTION_ASKED, text = "Why did the sign change?"),
        ).fold(AssistantConversationMemory(), AssistantConversationMemoryReducer::reduce)
        assertEquals("math-linear-equations", memory.currentConceptId)
        assertEquals("shape-7", memory.selectedObjectId)
        assertEquals(listOf("math-move-change-sign"), memory.recentMistakeIds)
        assertEquals("Why did the sign change?", memory.recentQuestions.single())
    }

    @Test fun localAssistantAttachesIntentBadgeAndSearchResults() = runBlocking {
        val request = GroundedRequestFactory.local("math-linear-equations", "define linear equation", com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState.LEARNING, HintLevel.CONCEPT_CUE, ExplanationStyle.CONCISE)
        val response = LocalLearningAssistantProvider().respond(request)
        assertEquals(AssistantIntent.DEFINITION, response.detectedIntent)
        assertEquals(AssistantVerificationStatus.LOCALLY_AUTHORED, response.verificationStatus)
        assertEquals(AssistantVerificationBadgeTone.TRUSTED, response.verificationBadge.tone)
        assertFalse(response.localSearchResults.isEmpty())
    }

    @Test fun routerRemainsOfflineFirstForRoutineRequests() = runBlocking {
        val request = GroundedRequestFactory.local("math-linear-equations", "explain this", com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState.LEARNING, HintLevel.CONCEPT_CUE, ExplanationStyle.CONCISE)
        val response = AssistantRouter(LocalLearningAssistantProvider()).respond(request, AssistanceNeed.ROUTINE)
        assertEquals("local-reviewed", response.providerId)
        assertTrue(response.verificationBadge.tone != AssistantVerificationBadgeTone.BLOCKED)
    }
}
