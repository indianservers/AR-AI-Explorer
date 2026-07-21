package com.indianservers.aiexplorer.learningintelligence.model

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import java.time.Instant

enum class ConceptMasteryState { NOT_STARTED, INTRODUCED, LEARNING, PRACTISING, PROFICIENT, MASTERED, NEEDS_REVIEW }
enum class LearnerConfidence { GUESSING, UNSURE, FAIRLY_SURE, VERY_SURE }
enum class EvidenceIndependence(val score: Double) { INDEPENDENT(1.0), SMALL_HINT(.85), VISUAL_GUIDANCE(.7), PARTIAL_WORKING(.5), NEAR_COMPLETE_WORKING(.25), FULL_SOLUTION(0.0) }
enum class MasteryEvidenceType { EXPLANATION_RECALL, CONCEPT_QUESTION, NUMERICAL_PROBLEM, SYMBOLIC_PROBLEM, DIAGRAM_TASK, CONSTRUCTION_TASK, SIMULATION_PREDICTION, EXPERIMENT_RESULT, DATA_INTERPRETATION, APPLICATION_TASK, DELAYED_RETRIEVAL, VIVA_RESPONSE }
enum class EvidenceQuality { LOW, MODERATE, HIGH }
enum class MisconceptionConfidence { POSSIBLE, LIKELY, CONFIRMED, RESOLVED }
enum class HintLevel { NUDGE, CONCEPT_CUE, VISUAL_FOCUS, PARTIAL_STEP, NEAR_COMPLETE_WORKING, FULL_EXPLANATION }
enum class FadeStage { FULLY_WORKED, ONE_STEP_MISSING, MULTIPLE_STEPS_MISSING, METHOD_SELECTION, INDEPENDENT }
enum class ReviewUrgency { NOT_DUE, DUE_SOON, DUE_TODAY, OVERDUE, URGENT_REPAIR }
enum class ErrorBookStatus { OPEN, RETRY_SCHEDULED, IMPROVING, RESOLVED }
enum class LearnerErrorType { CONCEPTUAL, ARITHMETIC, ALGEBRAIC_TRANSFORMATION, UNIT_MISSING, DIMENSION, SIGN, VECTOR_DIRECTION, RANGE, SIGNIFICANT_FIGURES, ATOM_CONSERVATION, CHARGE_CONSERVATION, FORMULA, LABEL, SEQUENCE, CLASSIFICATION, PROBABILITY, GRAPH_INTERPRETATION }
enum class RecommendationReason { MISSING_PREREQUISITE, REVIEW_DUE, CONFIDENTLY_WRONG, MISCONCEPTION_REPAIR, LOW_APPLICATION_EVIDENCE, LOW_RETENTION, CONTINUE_LEARNING_PATH, READY_FOR_CHALLENGE, DIAGNOSTIC_BRIDGE, PRACTICE_VARIETY }
enum class KnowledgeNodeType { CURRICULUM_NODE, CONCEPT, FORMULA, DIAGRAM, SIMULATION, ACTIVITY, EXPERIMENT, QUESTION, MISCONCEPTION, REVISION_RESOURCE, APPLICATION }
enum class KnowledgeRelation { PREREQUISITE_OF, REQUIRES, USES_FORMULA, EXPLAINED_BY_DIAGRAM, EXPLORED_BY_SIMULATION, PRACTISED_BY_ACTIVITY, DEMONSTRATED_BY_EXPERIMENT, TESTED_BY_QUESTION, REMEDIATES_MISCONCEPTION, REVISED_BY, APPLIED_IN, COMMONLY_CONFUSED_WITH, EXTENDS_TO, CROSS_SUBJECT_LINK }

data class HintUsageSummary(val requests: Int = 0, val maximumLevel: HintLevel? = null, val fullSolutionCount: Int = 0)
data class ConfidenceSummary(val observations: Int = 0, val calibrated: Int = 0, val confidentlyWrong: Int = 0, val meanConfidence: Double? = null)
data class RetentionState(val successfulDelayedRetrievals: Int = 0, val failedDelayedRetrievals: Int = 0, val intervalDays: Int = 1, val forgettingRisk: Double = 1.0)
data class MasteryEvidence(val id: String, val conceptId: String, val type: MasteryEvidenceType, val score: Double, val independence: Double, val hintPenalty: Double, val confidence: LearnerConfidence?, val occurredAt: Instant, val sourceActivityId: String?) {
    init { require(score in 0.0..1.0 && independence in 0.0..1.0 && hintPenalty in 0.0..1.0) }
    val effectiveScore get() = score * independence * (1.0 - hintPenalty)
}
data class MisconceptionEvidence(val misconceptionId: String, val conceptId: String, val sourceQuestionId: String?, val weight: Double, val confidence: LearnerConfidence?, val occurredAt: Instant, val resolved: Boolean = false)
data class LearnerConceptState(
    val conceptId: String, val curriculumNodeIds: Set<String>, val subject: SchoolSubject,
    val exposureCount: Int = 0, val lessonStarted: Boolean = false, val lessonCompletionCount: Int = 0, val visualisationOpenCount: Int = 0,
    val meaningfulInteractionCount: Int = 0, val guidedActivityCount: Int = 0, val exploreModeCount: Int = 0, val challengeAttemptCount: Int = 0, val challengeCompletionCount: Int = 0,
    val practiceAttemptCount: Int = 0, val successfulRetrievalCount: Int = 0, val firstAttemptAccuracy: Double? = null, val rollingAccuracy: Double? = null,
    val averageResponseTimeMs: Long? = null, val hintUsage: HintUsageSummary = HintUsageSummary(), val confidenceSummary: ConfidenceSummary = ConfidenceSummary(),
    val misconceptionEvidence: List<MisconceptionEvidence> = emptyList(), val masteryState: ConceptMasteryState = ConceptMasteryState.NOT_STARTED,
    val masteryEvidence: List<MasteryEvidence> = emptyList(), val evidenceQuality: EvidenceQuality = EvidenceQuality.LOW,
    val retentionState: RetentionState = RetentionState(), val lastSuccessfulRetrievalAt: Instant? = null, val lastInteractionAt: Instant? = null, val nextReviewAt: Instant? = null
)

data class MasteryPolicy(val conceptId: String, val requiredEvidenceTypes: Set<MasteryEvidenceType>, val minimumIndependentEvidence: Int, val minimumDelayedRetrievals: Int, val minimumRollingAccuracy: Double, val maximumFullHintDependence: Double, val requiresApplicationEvidence: Boolean)
data class KnowledgeNode(val id: String, val type: KnowledgeNodeType, val subject: SchoolSubject?, val classLevels: Set<SchoolClassLevel>, val reviewStatus: ScientificReviewStatus)
data class KnowledgeEdge(val fromId: String, val toId: String, val relation: KnowledgeRelation, val strength: Double, val mandatory: Boolean) { init { require(strength in 0.0..1.0) } }
data class KnowledgeGraph(val nodes: List<KnowledgeNode>, val edges: List<KnowledgeEdge>)

enum class DiagnosticQuestionKind { CONCEPT, VISUAL, REASONING, PREREQUISITE_DISCRIMINATION }
data class DiagnosticQuestion(val id: String, val subject: SchoolSubject, val conceptId: String, val cluster: String, val classLevel: SchoolClassLevel, val kind: DiagnosticQuestionKind, val prompt: String, val options: List<String>, val correctIndex: Int, val misconceptionByWrongIndex: Map<Int, String> = emptyMap(), val informationWeight: Double = 1.0, val asksConfidence: Boolean = false)
data class DiagnosticResponse(val questionId: String, val selectedIndex: Int?, val correct: Boolean, val confidence: LearnerConfidence?, val responseTimeMs: Long, val occurredAt: Instant)
data class DiagnosticSession(val id: String, val subject: SchoolSubject, val selectedLevel: SchoolClassLevel, val startedAt: Instant, val responses: List<DiagnosticResponse> = emptyList(), val skippedQuestionIds: Set<String> = emptySet())
data class ConfidenceCalibrationResult(val observations: Int, val accuracy: Double, val overconfidenceRate: Double, val underconfidenceRate: Double, val explanation: String)
data class DiagnosticResult(val subject: SchoolSubject, val estimatedLevel: SchoolClassLevel, val strongConceptIds: Set<String>, val weakConceptIds: Set<String>, val suspectedMisconceptionIds: Set<String>, val recommendedStartConceptId: String, val recommendedBridgeConceptIds: List<String>, val confidenceCalibration: ConfidenceCalibrationResult, val evidenceSummary: List<String>)

sealed interface MisconceptionEvidenceRule { data class WrongOption(val questionId: String, val optionIndex: Int, val weight: Double = 1.0) : MisconceptionEvidenceRule; data class ErrorType(val type: LearnerErrorType, val weight: Double = .7) : MisconceptionEvidenceRule; data class PredictionPattern(val code: String, val weight: Double = .8) : MisconceptionEvidenceRule }
data class MisconceptionDefinition(val id: String, val conceptId: String, val title: String, val description: String, val evidenceRules: List<MisconceptionEvidenceRule>, val explanationSectionId: String, val remediationActivityId: String?, val retryQuestionIds: List<String>, val reviewStatus: ScientificReviewStatus)
data class MisconceptionAssessment(val definition: MisconceptionDefinition, val confidence: MisconceptionConfidence, val evidenceCount: Int, val totalWeight: Double)
data class HintDefinition(val id: String, val conceptId: String, val activityId: String?, val level: HintLevel, val text: String, val visualActionId: String?, val revealedStepIds: Set<String>, val misconceptionIds: Set<String>)
data class HintSelection(val hint: HintDefinition, val independence: EvidenceIndependence, val reason: String)
data class WorkedSolutionStep(val id: String, val instruction: String, val expression: String?, val explanation: String, val dependencies: Set<String>, val validationRuleId: String?)
data class FadedWorkedExample(val id: String, val conceptId: String, val steps: List<WorkedSolutionStep>, val supportedFadeStages: List<FadeStage>, val defaultStage: FadeStage, val validationRuleIds: List<String>)
data class ConfidenceObservation(val questionId: String, val confidence: LearnerConfidence, val correct: Boolean, val occurredAt: Instant)
data class ScheduledReview(val conceptId: String, val scheduledAt: Instant, val urgency: ReviewUrgency, val reason: String, val preferredEvidenceType: MasteryEvidenceType)
data class LearningRecommendation(val conceptId: String, val activityId: String?, val reason: RecommendationReason, val explanation: String, val expectedDurationMinutes: Int, val priorityScore: Double)
data class ValidationResult(val valid: Boolean, val score: Double, val firstInvalidStepIndex: Int?, val errorType: LearnerErrorType?, val feedbackCode: String, val misconceptionCandidateIds: Set<String> = emptySet())
data class LearnerAnswerStep(val expression: String, val explanation: String? = null)
data class ErrorBookEntry(val id: String, val conceptId: String, val curriculumNodeId: String, val questionId: String?, val learnerAnswer: String, val learnerSteps: List<LearnerAnswerStep>, val firstInvalidStepIndex: Int?, val errorType: LearnerErrorType, val misconceptionId: String?, val confidence: LearnerConfidence?, val hintDepth: HintLevel?, val correctedMethod: List<String>, val remediationActivityId: String?, val retryAt: Instant?, val status: ErrorBookStatus)

sealed interface LearnerEvent { val conceptId: String; val occurredAt: Instant
    data class LessonStarted(override val conceptId: String, override val occurredAt: Instant) : LearnerEvent
    data class LessonCompleted(override val conceptId: String, override val occurredAt: Instant) : LearnerEvent
    data class VisualisationInteracted(override val conceptId: String, val activityId: String, val meaningful: Boolean, val guided: Boolean, val explore: Boolean, override val occurredAt: Instant) : LearnerEvent
    data class PredictionSubmitted(override val conceptId: String, val activityId: String, val correct: Boolean, val confidence: LearnerConfidence?, override val occurredAt: Instant) : LearnerEvent
    data class PracticeAnswered(override val conceptId: String, val questionId: String, val result: ValidationResult, val responseTimeMs: Long, val independence: EvidenceIndependence, val confidence: LearnerConfidence?, override val occurredAt: Instant) : LearnerEvent
    data class HintRequested(override val conceptId: String, val hintId: String, val level: HintLevel, override val occurredAt: Instant) : LearnerEvent
    data class ChallengeCompleted(override val conceptId: String, val activityId: String, val independent: Boolean, override val occurredAt: Instant) : LearnerEvent
    data class ReviewCompleted(override val conceptId: String, val correct: Boolean, val independence: EvidenceIndependence, val confidence: LearnerConfidence?, override val occurredAt: Instant) : LearnerEvent
    data class ConfidenceSubmitted(override val conceptId: String, val observation: ConfidenceObservation, override val occurredAt: Instant = observation.occurredAt) : LearnerEvent
}
