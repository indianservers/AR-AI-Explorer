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

data class GroundedContentBlock(val id: String, val kind: GroundedContentKind, val text: String, val reviewed: Boolean = true)
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
