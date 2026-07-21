package com.indianservers.aiexplorer.assistant.verification

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.grounding.GroundingValidator

object AssistantVerificationPipeline {
    private val unsafePatterns = listOf("ignore the formula", "invented result", "diagnosis", "mix bleach", "drink", "taste the chemical")
    fun verify(request: GroundedAssistantRequest, response: AssistantResponse): AssistantResponse {
        val grounding = GroundingValidator.validate(request)
        val issues = response.validationIssues.toMutableList()
        if (!grounding.valid) issues += grounding.issues
        if (response.groundingReferences.any { it !in grounding.allowedReferenceIds }) issues += "Response cites a source outside the request grounding."
        if (response.text.isBlank()) issues += "Response is empty."
        if (unsafePatterns.any { response.text.contains(it, ignoreCase = true) }) issues += "Response contains unsafe or ungrounded guidance."
        val status = when {
            issues.isNotEmpty() -> AssistantVerificationStatus.REJECTED
            response.verificationStatus == AssistantVerificationStatus.LOCALLY_AUTHORED -> AssistantVerificationStatus.LOCALLY_AUTHORED
            response.groundingReferences.isNotEmpty() -> AssistantVerificationStatus.VERIFIED
            else -> AssistantVerificationStatus.PARTIALLY_VERIFIED
        }
        return response.copy(verificationStatus = status, validationIssues = issues.distinct())
    }
    fun safeFallback(request: GroundedAssistantRequest, issues: List<String>): AssistantResponse {
        val content = request.verifiedContent.firstOrNull { it.reviewed }
        return AssistantResponse(content?.text ?: "I cannot verify that response. Open the reviewed lesson and try one smaller step.", AssistantResponseType.FALLBACK, listOfNotNull(content?.id), emptyList(), AssistantVerificationStatus.FALLBACK_USED, issues, null)
    }
}
