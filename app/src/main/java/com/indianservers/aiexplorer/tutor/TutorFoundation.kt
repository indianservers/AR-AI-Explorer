package com.indianservers.aiexplorer.tutor

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.learningintelligence.engine.KnowledgeGraphIndex
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningintelligence.validation.LocalAnswerValidationEngine
import com.indianservers.aiexplorer.learningintelligence.validation.ValidationRequest

enum class SocraticTutorState { IDENTIFY_GOAL, CHECK_UNDERSTANDING, FOCUS_RELEVANT_INFORMATION, SELECT_METHOD, EXECUTE_STEP, VERIFY_RESULT, REFLECT, COMPLETE }
data class SocraticPrompt(val state: SocraticTutorState, val text: String, val referenceIds: List<String>, val visualTargetId: String? = null)
data class AuthoredSocraticFlow(val conceptId: String, val prompts: Map<SocraticTutorState, String>)

object AuthoredSocraticFlows {
    private val titles = mapOf(
        "math-fractions" to "equal parts", "math-linear-equations" to "balance", "math-triangles" to "corresponding properties", "math-coordinate-geometry" to "rise and run", "math-probability" to "equally likely outcomes", "math-trigonometric-graphs" to "amplitude, period and phase",
        "physics-distance-displacement" to "motion evidence", "physics-velocity-acceleration" to "graph slope", "physics-newton-second-law" to "net force", "physics-wave-speed" to "speed-frequency-wavelength relationship", "physics-electric-circuits" to "charge and energy",
        "chemistry-atomic-number-mass-number" to "protons and nucleons", "chemistry-ions-isotopes" to "electron configuration", "chemistry-periodic-trends" to "particle trend", "chemistry-chemical-bonding" to "valence interaction", "chemistry-acids-bases" to "ionisation and concentration", "chemistry-gas-laws" to "pressure, volume and kelvin temperature",
        "biology-cell-structure" to "structure and function", "biology-diffusion-osmosis" to "concentration gradient", "biology-mitosis-meiosis" to "division outcome", "biology-blood-circulation" to "direction of flow", "biology-mendelian-inheritance" to "allele separation",
    )
    val flows: Map<String, AuthoredSocraticFlow> = titles.mapValues { (id, focus) ->
        AuthoredSocraticFlow(id, mapOf(
            SocraticTutorState.IDENTIFY_GOAL to "What are you trying to find or explain?",
            SocraticTutorState.CHECK_UNDERSTANDING to "What does $focus mean in this situation?",
            SocraticTutorState.FOCUS_RELEVANT_INFORMATION to "Which visible value or feature gives evidence about $focus?",
            SocraticTutorState.SELECT_METHOD to "Which reviewed relationship connects the known information to your goal?",
            SocraticTutorState.EXECUTE_STEP to "Apply just the next operation and say why it preserves the relationship.",
            SocraticTutorState.VERIFY_RESULT to "How can units, the visual, or a simple case check this result?",
            SocraticTutorState.REFLECT to "What stayed invariant, and what would change in a nearby example?",
            SocraticTutorState.COMPLETE to "Summarise the reason in one sentence.",
        ))
    }
}

class SocraticTutorEngine {
    fun next(request: GroundedAssistantRequest, evaluations: List<StepEvaluation> = emptyList(), current: SocraticTutorState = SocraticTutorState.IDENTIFY_GOAL): SocraticPrompt {
        val flow = AuthoredSocraticFlows.flows[request.conceptId]
        val firstInvalid = evaluations.firstOrNull { !it.valid }
        val state = when {
            firstInvalid != null -> SocraticTutorState.EXECUTE_STEP
            request.suspectedMisconceptionIds.isNotEmpty() -> SocraticTutorState.CHECK_UNDERSTANDING
            request.learnerSteps.isNotEmpty() && current.ordinal < SocraticTutorState.VERIFY_RESULT.ordinal -> SocraticTutorState.VERIFY_RESULT
            else -> current
        }
        val base = flow?.prompts?.get(state) ?: "What reviewed fact would justify your next step?"
        val text = firstInvalid?.let { "Your earlier work is usable. At step ${it.stepIndex + 1}, ${it.feedbackCode.replace('_', ' ')}. $base" } ?: base
        val refs = request.verifiedContent.filter { it.reviewed }.take(2).map { it.id }
        return SocraticPrompt(state, text, refs, request.selectedVisualElements.firstOrNull())
    }
}

data class StepEvaluation(
    val stepIndex: Int,
    val valid: Boolean,
    val transformationType: String?,
    val expectedRelationship: String?,
    val errorType: LearnerErrorType?,
    val misconceptionCandidateIds: Set<String>,
    val feedbackCode: String,
)

class MistakeAwareStepEvaluator(private val validator: LocalAnswerValidationEngine = LocalAnswerValidationEngine()) {
    fun ordered(expected: List<String>, learner: List<LearnerAnswerStep>, misconceptionIds: Set<String> = emptySet()): List<StepEvaluation> {
        var failureSeen = false
        return expected.indices.map { index ->
            val actual = learner.getOrNull(index)?.expression
            val valid = !failureSeen && actual != null && normal(actual) == normal(expected[index])
            if (!valid) failureSeen = true
            StepEvaluation(index, valid, if (index == 0) "identify" else "equivalent transformation", expected[index], if (valid) null else LearnerErrorType.ALGEBRAIC_TRANSFORMATION, if (valid) emptySet() else misconceptionIds, if (valid) "step_valid" else if (actual == null) "step_missing" else "relationship_not_preserved")
        }
    }
    fun unitSubstitution(expectedValue: Double, requiredUnit: String, values: List<Pair<String, String?>>): List<StepEvaluation> = values.mapIndexed { index, (value, unit) ->
        val result = validator.validate(ValidationRequest.Physics(expectedValue, value, requiredUnit, unit))
        StepEvaluation(index, result.valid, "formula substitution", "value expressed in $requiredUnit", result.errorType, result.misconceptionCandidateIds, result.feedbackCode)
    }
    fun firstInvalid(evaluations: List<StepEvaluation>) = evaluations.firstOrNull { !it.valid }
    private fun normal(value: String) = value.lowercase().replace(" ", "").replace("−", "-")
}

data class LearningReturnState(val conceptId: String, val activityId: String?, val learnerStepIndex: Int?, val simulationStateId: String?)
data class PrerequisiteRepairPlan(
    val sourceConceptId: String,
    val missingPrerequisiteConceptId: String,
    val reason: String,
    val estimatedMinutes: Int,
    val explanationSectionIds: List<String>,
    val visualActivityId: String,
    val checkQuestionIds: List<String>,
    val returnState: LearningReturnState,
)

class PrerequisiteRepairEngine(private val index: KnowledgeGraphIndex = KnowledgeGraphIndex(LearningIntelligenceCatalog.graph)) {
    fun plan(sourceConceptId: String, states: Map<String, LearnerConceptState>, activityId: String? = null, stepIndex: Int? = null): PrerequisiteRepairPlan? {
        val missing = index.mandatoryPrerequisites(sourceConceptId).firstOrNull { states[it]?.masteryState !in setOf(ConceptMasteryState.PROFICIENT, ConceptMasteryState.MASTERED) } ?: return null
        val concept = LearningIntelligenceCatalog.concepts.single { it.conceptId == missing }
        return PrerequisiteRepairPlan(sourceConceptId, missing, "$missing is required for the uncertain step in $sourceConceptId.", 5, listOf(concept.misconceptions.first().explanationSectionId), concept.activityId ?: "visual-$missing", concept.diagnosticQuestions.take(1).map { it.id }, LearningReturnState(sourceConceptId, activityId, stepIndex, null))
    }
    fun returnToOriginal(plan: PrerequisiteRepairPlan) = plan.returnState
}
