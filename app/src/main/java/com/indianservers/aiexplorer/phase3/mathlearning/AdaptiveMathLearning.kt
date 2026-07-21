package com.indianservers.aiexplorer.phase3.mathlearning

import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.grounding.GroundedRequestFactory
import com.indianservers.aiexplorer.learningintelligence.engine.KnowledgeGraphIndex
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningintelligence.validation.LocalAnswerValidationEngine
import com.indianservers.aiexplorer.learningintelligence.validation.ValidationRequest
import com.indianservers.aiexplorer.learningworkspace.ProofEvaluation
import com.indianservers.aiexplorer.learningworkspace.ProofStep
import com.indianservers.aiexplorer.learningworkspace.ProofStudioEngine
import com.indianservers.aiexplorer.learningworkspace.ProofTaskType
import com.indianservers.aiexplorer.practice.VerifiedPracticeCatalog
import com.indianservers.aiexplorer.practice.VerifiedPracticeGenerator
import com.indianservers.aiexplorer.tutor.MistakeAwareStepEvaluator
import com.indianservers.aiexplorer.tutor.PrerequisiteRepairEngine
import com.indianservers.aiexplorer.tutor.PrerequisiteRepairPlan
import com.indianservers.aiexplorer.tutor.SocraticPrompt
import com.indianservers.aiexplorer.tutor.SocraticTutorEngine
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceState
import java.time.Instant

data class WorkspaceLearningEvidence(
    val module: String,
    val functionCount: Int,
    val constructionCount: Int,
    val pointCount: Int,
    val parameterCount: Int,
    val summary: String,
)

data class AdaptiveMathTask(
    val id: String,
    val conceptId: String,
    val prompt: String,
    val expectedAnswer: String,
    val expectedSteps: List<ProofStep>,
    val invariants: Set<String>,
    val provenance: String,
)

data class AdaptiveMathLearningSession(
    val conceptId: String,
    val task: AdaptiveMathTask,
    val workspaceEvidence: WorkspaceLearningEvidence,
    val learnerState: LearnerConceptState,
    val answer: String = "",
    val learnerSteps: List<LearnerAnswerStep> = emptyList(),
    val confidence: LearnerConfidence = LearnerConfidence.FAIRLY_SURE,
    val validation: ValidationResult? = null,
    val proofEvaluation: ProofEvaluation? = null,
    val tutorPrompt: SocraticPrompt? = null,
    val hint: HintSelection? = null,
    val recommendation: LearningRecommendation? = null,
    val repairPlan: PrerequisiteRepairPlan? = null,
    val errorEntry: ErrorBookEntry? = null,
    val failedAttempts: Int = 0,
    val startedAt: Instant = Instant.now(),
)

class AdaptiveMathLearningEngine(
    val service: LocalLearningIntelligenceService = LocalLearningIntelligenceService(),
    private val validator: LocalAnswerValidationEngine = LocalAnswerValidationEngine(),
    private val proofStudio: ProofStudioEngine = ProofStudioEngine(),
    private val tutor: SocraticTutorEngine = SocraticTutorEngine(),
    private val stepEvaluator: MistakeAwareStepEvaluator = MistakeAwareStepEvaluator(),
    private val repair: PrerequisiteRepairEngine = PrerequisiteRepairEngine(),
) {
    fun start(workspace: WorkspaceState, requestedConceptId: String? = null, seed: Int = 1, now: Instant = Instant.now()): AdaptiveMathLearningSession {
        val conceptId = requestedConceptId ?: conceptFor(workspace)
        val current = service.state(conceptId)
        val state = if (!current.lessonStarted) service.emit(LearnerEvent.LessonStarted(conceptId, now)) else current
        val task = taskFor(conceptId, workspace, seed)
        val recommendation = service.recommend(now = now)
        return AdaptiveMathLearningSession(
            conceptId = conceptId,
            task = task,
            workspaceEvidence = evidenceFrom(workspace),
            learnerState = state,
            recommendation = recommendation,
            repairPlan = repair.plan(conceptId, service.repository.all().associateBy { it.conceptId }, task.id),
        )
    }

    fun submit(
        session: AdaptiveMathLearningSession,
        answer: String,
        learnerSteps: List<LearnerAnswerStep>,
        confidence: LearnerConfidence,
        now: Instant = Instant.now(),
    ): AdaptiveMathLearningSession {
        val baseResult = validationFor(session.task, answer, learnerSteps)
        val misconceptionId = LearningIntelligenceCatalog.concepts.single { it.conceptId == session.conceptId }.misconceptions.first().id
        val result = if (baseResult.valid) baseResult else baseResult.copy(misconceptionCandidateIds = setOf(misconceptionId))
        val proofSteps = learnerSteps.map { ProofStep(it.expression, it.explanation, session.task.invariants) }
        val proof = proofStudio.evaluate(ProofTaskType.ALGEBRAIC_TRANSFORMATION, session.task.expectedSteps, proofSteps, session.task.invariants)
        val state = service.emit(
            LearnerEvent.PracticeAnswered(
                session.conceptId,
                session.task.id,
                result,
                java.time.Duration.between(session.startedAt, now).toMillis().coerceAtLeast(1),
                session.hint?.independence ?: EvidenceIndependence.INDEPENDENT,
                confidence,
                now,
            ),
        )
        service.emit(LearnerEvent.ConfidenceSubmitted(session.conceptId, ConfidenceObservation(session.task.id, confidence, result.valid, now)))
        val evaluations = stepEvaluator.ordered(session.task.expectedSteps.map { it.statement }, learnerSteps, setOf(misconceptionId))
        val request = GroundedRequestFactory.local(
            session.conceptId,
            session.task.prompt,
            state.masteryState,
            session.hint?.hint?.level ?: HintLevel.NUDGE,
            ExplanationStyle.STEP_BY_STEP,
            steps = learnerSteps,
            misconceptions = result.misconceptionCandidateIds,
        )
        val error = if (!result.valid && result.errorType != null) {
            val concept = LearningIntelligenceCatalog.concepts.single { it.conceptId == session.conceptId }
            service.recordError(session.conceptId, concept.curriculumNodeId, session.task.id, answer, learnerSteps, result, confidence, session.task.expectedSteps.map { it.statement }, concept.activityId, now)
        } else null
        return session.copy(
            answer = answer,
            learnerSteps = learnerSteps,
            confidence = confidence,
            validation = result,
            proofEvaluation = proof,
            tutorPrompt = tutor.next(request, evaluations),
            learnerState = service.state(session.conceptId),
            recommendation = service.recommend(now = now),
            repairPlan = repair.plan(session.conceptId, service.repository.all().associateBy { it.conceptId }, session.task.id, result.firstInvalidStepIndex),
            errorEntry = error,
            failedAttempts = session.failedAttempts + if (result.valid) 0 else 1,
        )
    }

    fun requestHint(session: AdaptiveMathLearningSession): AdaptiveMathLearningSession {
        val selection = service.hint(session.conceptId, session.failedAttempts) ?: return session
        val request = GroundedRequestFactory.local(session.conceptId, session.task.prompt, service.state(session.conceptId).masteryState, selection.hint.level, ExplanationStyle.STEP_BY_STEP, steps = session.learnerSteps)
        return session.copy(hint = selection, tutorPrompt = tutor.next(request), learnerState = service.state(session.conceptId))
    }

    fun recordWorkspaceInteraction(session: AdaptiveMathLearningSession, now: Instant = Instant.now()) = session.copy(
        learnerState = service.emit(LearnerEvent.VisualisationInteracted(session.conceptId, session.task.id, true, true, false, now)),
    )

    fun conceptFor(workspace: WorkspaceState): String = when {
        workspace.module == MathModule.Trigonometry || workspace.functions.any { it.expression.contains(Regex("sin|cos|tan", RegexOption.IGNORE_CASE)) } -> "math-trigonometric-graphs"
        workspace.module in setOf(MathModule.Geometry2D, MathModule.Geometry3D, MathModule.SpatialAR) -> "math-triangles"
        workspace.module in setOf(MathModule.Graph2D, MathModule.Graph3D) -> "math-coordinate-geometry"
        else -> "math-linear-equations"
    }

    fun evidenceFrom(workspace: WorkspaceState): WorkspaceLearningEvidence {
        val parameters = workspace.graphSliderMetadata.values.count { it.value != null }
        return WorkspaceLearningEvidence(
            workspace.module.label,
            workspace.functions.size,
            workspace.shapes.size + workspace.solids.size,
            workspace.points.size,
            parameters,
            "${workspace.functions.size} function(s), ${workspace.shapes.size + workspace.solids.size} construction(s), ${workspace.points.size} point(s), and $parameters live parameter(s).",
        )
    }

    private fun validationFor(task: AdaptiveMathTask, answer: String, steps: List<LearnerAnswerStep>): ValidationResult {
        val answerResult = when (task.conceptId) {
            "math-fractions" -> validator.validate(ValidationRequest.Fraction(1, 2, answer))
            else -> validator.validate(ValidationRequest.Numeric(task.expectedAnswer.toDouble(), answer))
        }
        if (steps.isEmpty()) return answerResult
        val stepResult = validator.validate(ValidationRequest.OrderedSteps(task.expectedSteps.map { it.statement }, steps.map { it.expression }))
        return if (answerResult.valid && stepResult.valid) answerResult else stepResult.copy(errorType = stepResult.errorType ?: answerResult.errorType)
    }

    private fun taskFor(conceptId: String, workspace: WorkspaceState, seed: Int): AdaptiveMathTask {
        if (conceptId == "math-linear-equations") {
            val template = VerifiedPracticeCatalog.templates.single { it.conceptId == conceptId }
            val generated = requireNotNull(VerifiedPracticeGenerator().generate(template, seed).task)
            val a = generated.variables.getValue("a").toInt(); val b = generated.variables.getValue("b").toInt(); val x = generated.answer.toInt(); val rhs = a * x + b
            val steps = listOf(ProofStep("${a}x + $b = $rhs", "Given", setOf("equality")), ProofStep("${a}x = ${rhs - b}", "Apply the same inverse operation to both sides", setOf("equality")), ProofStep("x = $x", "Divide both sides by $a", setOf("equality")))
            return AdaptiveMathTask(generated.templateId + "-${generated.seed}", conceptId, generated.prompt, generated.answer.toString(), steps, setOf("equality"), "VerifiedPracticeGenerator / ${generated.validationStages.joinToString()}")
        }
        val (prompt, answer, steps, invariants) = when (conceptId) {
            "math-fractions" -> Quad("What fraction of a whole remains when one of two equal parts is selected?", "1/2", listOf("whole = 2 equal parts", "selected = 1 part", "fraction = 1/2"), setOf("whole", "equal-parts"))
            "math-triangles" -> Quad("Use the current construction to justify the interior angle sum of a triangle.", "180", listOf("draw a line parallel to the base", "match alternate interior angles", "angles form a straight line = 180"), setOf("angle", "parallelism"))
            "math-trigonometric-graphs" -> Quad("For y = 2 sin(x), what is the amplitude?", "2", listOf("compare y = a sin(x)", "a = 2", "amplitude = |a| = 2"), setOf("period", "midline"))
            else -> {
                val function = workspace.functions.firstOrNull()?.expression ?: "x^2 - 4*x + 3"
                Quad("A point on the active graph has x = 0. For f(x) = $function, use the visible y-intercept when possible; for the default graph enter 3.", "3", listOf("set x = 0", "evaluate f(0)", "y-intercept = 3"), setOf("same-function"))
            }
        }
        return AdaptiveMathTask("adaptive-$conceptId-$seed", conceptId, prompt, answer, steps.mapIndexed { i, s -> ProofStep(s, if (i == 0) "Identify the relationship" else "Preserve the stated invariant", invariants) }, invariants, "Reviewed local concept catalog + deterministic validator")
    }

    private data class Quad(val prompt: String, val answer: String, val steps: List<String>, val invariants: Set<String>)
}
