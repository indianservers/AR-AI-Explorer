package com.indianservers.aiexplorer.assistant.contracts

import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep

enum class AssistantCapability { SOCRATIC_TUTORING, EXPLANATION_REWRITE, STEP_ANALYSIS, PRACTICE_WORDING, CAMERA_QUESTION_INTERPRETATION, VOICE_DIALOGUE, VIVA_DIALOGUE, TRANSLATION_DRAFT }
enum class ExplanationStyle { INTUITIVE, VISUAL, FORMAL, EXAM_FOCUSED, ANALOGY_BASED, CONCISE, STEP_BY_STEP }
enum class AssistantResponseType { SOCRATIC_QUESTION, EXPLANATION, STEP_FEEDBACK, PRACTICE_WORDING, VISUAL_NARRATION, VIVA_PROMPT, FALLBACK }
enum class AssistantVerificationStatus { LOCALLY_AUTHORED, VERIFIED, PARTIALLY_VERIFIED, REJECTED, FALLBACK_USED }
enum class GroundedContentKind { DEFINITION, EXPLANATION, FORMULA, HINT, SAFETY, LIMITATION, VISUAL_STATE }
enum class VisualActionType { FOCUS, HIGHLIGHT, SHOW_LAYER, ANIMATE, COMPARE, RESTORE_STATE }
enum class AssistantIntent { SOLVE, HINT, EXPLAIN, QUIZ, DEFINITION, DIAGRAM_HELP, FORMULA, WORKSPACE_ACTION, OCR_REPAIR, VIVA, SEARCH, UNKNOWN }
enum class AssistantKnowledgeKind { CONCEPT, DIAGNOSTIC_QUESTION, HINT, MISCONCEPTION, WORKED_EXAMPLE, WORKED_STEP, FORMULA, THEOREM, DICTIONARY, LESSON }
enum class AssistantMemoryEventType { QUESTION_ASKED, RESPONSE_GIVEN, CONCEPT_OPENED, OBJECT_SELECTED, MISTAKE_DETECTED, HINT_USED, LEVEL_CHANGED }
enum class AssistantVerificationBadgeTone { TRUSTED, CAUTION, BLOCKED }

data class GroundedContentBlock(val id: String, val kind: GroundedContentKind, val text: String, val reviewed: Boolean = true)
data class AssistantVerificationBadge(
    val label: String,
    val tone: AssistantVerificationBadgeTone,
    val explanation: String,
) {
    companion object {
        fun from(status: AssistantVerificationStatus): AssistantVerificationBadge = when (status) {
            AssistantVerificationStatus.LOCALLY_AUTHORED -> AssistantVerificationBadge("Offline reviewed", AssistantVerificationBadgeTone.TRUSTED, "Built from reviewed local content.")
            AssistantVerificationStatus.VERIFIED -> AssistantVerificationBadge("Verified", AssistantVerificationBadgeTone.TRUSTED, "Grounding and citations passed local checks.")
            AssistantVerificationStatus.PARTIALLY_VERIFIED -> AssistantVerificationBadge("Partly verified", AssistantVerificationBadgeTone.CAUTION, "Uses local grounding but needs extra checking.")
            AssistantVerificationStatus.REJECTED -> AssistantVerificationBadge("Blocked", AssistantVerificationBadgeTone.BLOCKED, "Rejected by local safety or grounding checks.")
            AssistantVerificationStatus.FALLBACK_USED -> AssistantVerificationBadge("Offline fallback", AssistantVerificationBadgeTone.CAUTION, "Replaced an unverifiable response with local reviewed content.")
        }
    }
}
data class AssistantKnowledgeSearchResult(
    val id: String,
    val kind: AssistantKnowledgeKind,
    val title: String,
    val snippet: String,
    val conceptId: String?,
    val subject: SchoolSubject?,
    val score: Double,
)
data class AssistantMemoryEvent(
    val type: AssistantMemoryEventType,
    val conceptId: String? = null,
    val selectedObjectId: String? = null,
    val text: String = "",
    val timestampMs: Long = 0L,
)
data class AssistantConversationMemory(
    val currentConceptId: String? = null,
    val currentTopic: String? = null,
    val selectedObjectId: String? = null,
    val recentMistakeIds: List<String> = emptyList(),
    val preferredLevel: SchoolClassLevel? = null,
    val recentQuestions: List<String> = emptyList(),
)
data class SimulationStateSnapshot(
    val id: String,
    val simulationId: String,
    val simulationVersion: Int,
    val timeMs: Long,
    val values: Map<String, Double>,
    val units: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap(),
)
data class AssistantVisualAction(val type: VisualActionType, val targetId: String, val value: String? = null)

data class GroundedAssistantRequest(
    val conceptId: String,
    val curriculumNodeId: String?,
    val subject: SchoolSubject,
    val learningLevel: SchoolClassLevel?,
    val learnerQuestion: String,
    val verifiedContent: List<GroundedContentBlock>,
    val verifiedFormulaIds: List<String>,
    val currentSimulationState: SimulationStateSnapshot?,
    val selectedVisualElements: Set<String>,
    val learnerSteps: List<LearnerAnswerStep>,
    val suspectedMisconceptionIds: Set<String>,
    val masteryState: ConceptMasteryState,
    val maximumAllowedHintLevel: HintLevel,
    val explanationStyle: ExplanationStyle,
)

data class AssistantResponse(
    val text: String,
    val responseType: AssistantResponseType,
    val groundingReferences: List<String>,
    val proposedVisualActions: List<AssistantVisualAction>,
    val verificationStatus: AssistantVerificationStatus,
    val validationIssues: List<String>,
    val providerId: String?,
    val detectedIntent: AssistantIntent = AssistantIntent.UNKNOWN,
    val verificationBadge: AssistantVerificationBadge = AssistantVerificationBadge.from(verificationStatus),
    val localSearchResults: List<AssistantKnowledgeSearchResult> = emptyList(),
)

interface LearningAssistantProvider {
    val id: String
    val capabilities: Set<AssistantCapability>
    suspend fun respond(request: GroundedAssistantRequest): AssistantResponse
}

data class VisualStateExplanation(
    val title: String,
    val summary: String,
    val observations: List<String>,
    val causalExplanation: List<String>,
    val formulaConnections: List<String>,
    val limitations: List<String>,
    val accessibleNarration: String,
)
