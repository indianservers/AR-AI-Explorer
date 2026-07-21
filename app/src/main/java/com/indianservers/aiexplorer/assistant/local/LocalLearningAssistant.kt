package com.indianservers.aiexplorer.assistant.local

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.grounding.GroundingValidator
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel

class LocalLearningAssistantProvider : LearningAssistantProvider {
    override val id = "local-reviewed"
    override val capabilities = setOf(AssistantCapability.SOCRATIC_TUTORING, AssistantCapability.EXPLANATION_REWRITE, AssistantCapability.STEP_ANALYSIS, AssistantCapability.PRACTICE_WORDING, AssistantCapability.VOICE_DIALOGUE, AssistantCapability.VIVA_DIALOGUE)
    override suspend fun respond(request: GroundedAssistantRequest): AssistantResponse {
        val validation = GroundingValidator.validate(request)
        if (!validation.valid) return AssistantResponse("", AssistantResponseType.FALLBACK, emptyList(), emptyList(), AssistantVerificationStatus.REJECTED, validation.issues, id)
        val blocks = request.verifiedContent.filter { it.reviewed }
        val hint = blocks.lastOrNull { it.kind == GroundedContentKind.HINT }
        val definition = blocks.first()
        val text = when {
            request.learnerSteps.isNotEmpty() -> "Which operation connects your first uncertain step to the previous line, and was it applied to every required quantity?"
            request.maximumAllowedHintLevel == HintLevel.NUDGE -> "What is the question asking you to find, and which given fact seems most relevant?"
            request.suspectedMisconceptionIds.isNotEmpty() -> "Before calculating, how would you test the idea behind ${request.suspectedMisconceptionIds.first()} with the active visual?"
            request.explanationStyle == ExplanationStyle.CONCISE -> hint?.text ?: definition.text
            request.explanationStyle == ExplanationStyle.EXAM_FOCUSED -> "State the governing idea, show one justified step, include units where needed, and verify the result. ${hint?.text ?: definition.text}"
            request.explanationStyle == ExplanationStyle.VISUAL -> "Focus on the highlighted quantities. What changes, what stays fixed, and how does that support this idea: ${definition.text}"
            else -> "Start from this reviewed idea: ${definition.text} What would be a useful first step without completing the whole solution?"
        }
        val reference = listOfNotNull(hint?.id ?: definition.id)
        val visual = request.selectedVisualElements.firstOrNull()?.let { listOf(AssistantVisualAction(VisualActionType.FOCUS, it)) }.orEmpty()
        return AssistantResponse(text, if (request.learnerSteps.isEmpty()) AssistantResponseType.SOCRATIC_QUESTION else AssistantResponseType.STEP_FEEDBACK, reference, visual, AssistantVerificationStatus.LOCALLY_AUTHORED, emptyList(), id)
    }
}
