package com.indianservers.aiexplorer.assistant.routing

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.privacy.AssistantConsent
import com.indianservers.aiexplorer.assistant.verification.AssistantVerificationPipeline

enum class AssistanceNeed { ROUTINE, OPEN_ENDED, UNUSUAL_WORDING, HANDWRITING_INTERPRETATION, VIVA, NEW_STYLE, PRACTICE_LANGUAGE_VARIATION }

class AssistantRouter(
    private val local: LearningAssistantProvider,
    private val providers: Map<String, LearningAssistantProvider> = emptyMap(),
) {
    suspend fun respond(request: GroundedAssistantRequest, need: AssistanceNeed = AssistanceNeed.ROUTINE, selectedProviderId: String? = null, consent: AssistantConsent = AssistantConsent()): AssistantResponse {
        val cloudAllowed = need != AssistanceNeed.ROUTINE && consent.cloudEnabled && selectedProviderId != null
        val candidate = if (cloudAllowed) providers[selectedProviderId]?.respond(request) else local.respond(request)
        val verified = candidate?.let { AssistantVerificationPipeline.verify(request, it) }
        if (verified != null && verified.verificationStatus != AssistantVerificationStatus.REJECTED) return verified
        val localVerified = AssistantVerificationPipeline.verify(request, local.respond(request))
        return if (localVerified.verificationStatus != AssistantVerificationStatus.REJECTED) localVerified.copy(verificationStatus = AssistantVerificationStatus.FALLBACK_USED) else AssistantVerificationPipeline.safeFallback(request, verified?.validationIssues.orEmpty() + localVerified.validationIssues)
    }
}
