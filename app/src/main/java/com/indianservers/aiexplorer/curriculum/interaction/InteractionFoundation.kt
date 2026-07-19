package com.indianservers.aiexplorer.curriculum.interaction

import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import kotlin.math.abs

enum class InteractionCoverageStatus { COMPLETE, PARTIAL, STATIC_ONLY, TEXT_ONLY, PLACEHOLDER, INCORRECT, MISSING, NOT_APPLICABLE, REQUIRES_REVIEW }
enum class InteractionSuitability { TIER_A, TIER_B, TIER_C, TIER_D, NOT_SUITABLE, REQUIRES_REVIEW }
enum class InteractionPriority { CRITICAL, HIGH, MEDIUM, LOW, NONE }
enum class ActivityMode { GUIDED, EXPLORE, CHALLENGE }
enum class VisualLearningType { MANIPULABLE_DIAGRAM, STEP_ANIMATION, VARIABLE_SIMULATION, INTERACTIVE_GRAPH, DRAG_DROP_ACTIVITY, VIRTUAL_EXPERIMENT, CONSTRUCTION_TOOL, COMPARISON_VIEW, PROCESS_VISUALISER, STRUCTURE_EXPLORER, CLASSIFICATION_TREE, TIMELINE, DATA_EXPLORER, FORMULA_VISUALISER, PREDICTION_CHALLENGE, LABEL_ACTIVITY, SEQUENCE_ACTIVITY, MEASUREMENT_TOOL, FUTURE_3D_MODEL }
enum class InteractionCapability { OBJECTIVE, GUIDANCE, MANIPULATION, LIVE_VISUAL, LIVE_VALUES, EXPLANATION, RESET, PRESETS, CHALLENGE, PREDICTION, OBSERVATION, REFLECTION, ACCESSIBLE_ALTERNATIVE, PROGRESS, UNITS, VALID_LABELS }

data class ConceptInteractionProfile(
    val conceptId: String,
    val subject: SchoolSubject,
    val classLevels: Set<SchoolClassLevel>,
    val interactionSuitability: InteractionSuitability,
    val requiredVisualTypes: Set<VisualLearningType>,
    val existingAssetIds: List<String>,
    val coverageStatus: InteractionCoverageStatus,
    val missingCapabilities: List<InteractionCapability>,
    val priority: InteractionPriority,
)

data class ActivityCompletionPolicy(
    val requiredInteractions: Int = 2,
    val requiresPrediction: Boolean = true,
    val requiresObservation: Boolean = true,
    val requiresChallenge: Boolean = true,
    val requiresReflectionQuestion: Boolean = false,
)

data class ActivityControl(
    val id: String,
    val label: String,
    val minimum: Double,
    val maximum: Double,
    val step: Double,
    val initial: Double,
    val unit: String,
    val spokenLabel: String = "$label, $unit",
) {
    init { require(maximum > minimum && initial in minimum..maximum && step > 0.0); require(label.isNotBlank() && unit.isNotBlank()) }
    fun clamp(value: Double) = value.coerceIn(minimum, maximum)
}

data class ActivityPreset(val id: String, val title: String, val values: Map<String, Double>)
data class PlotPoint(val x: Double, val y: Double, val series: String = "model")
data class DiagramPart(val id: String, val label: String, val description: String)
data class VisualState(
    val values: Map<String, Double>,
    val valueUnits: Map<String, String>,
    val labels: Map<String, String>,
    val points: List<PlotPoint>,
    val processSteps: List<String>,
    val explanation: String,
    val textAlternative: String,
    val highlightedPartId: String? = null,
)

data class ActivityChallenge(
    val id: String,
    val title: String,
    val instruction: String,
    val targetDescription: String,
    val isSatisfied: (VisualState) -> Boolean,
)

data class ActivityAccessibility(
    val visualStateTemplate: String,
    val reducedMotionSupported: Boolean = true,
    val highContrastSupported: Boolean = true,
    val colourIndependentIndicators: Boolean = true,
    val minimumTouchTargetDp: Int = 48,
    val keyboardAdjustable: Boolean = true,
    val stepByStepTextAvailable: Boolean = true,
)

interface InteractiveLearningActivity {
    val activityId: String
    val conceptId: String
    val learningObjectives: List<String>
    val supportedModes: Set<ActivityMode>
    val completionPolicy: ActivityCompletionPolicy
}

data class ActivityDefinition(
    override val activityId: String,
    override val conceptId: String,
    val subject: SchoolSubject,
    val classLevels: Set<SchoolClassLevel>,
    val title: String,
    override val learningObjectives: List<String>,
    val guidedInstructions: List<String>,
    val controls: List<ActivityControl>,
    val presets: List<ActivityPreset>,
    val challenges: List<ActivityChallenge>,
    val formulaOrProcess: String,
    val assumptions: List<String>,
    val diagramParts: List<DiagramPart>,
    val accessibility: ActivityAccessibility,
    val compute: (Map<String, Double>) -> VisualState,
    override val supportedModes: Set<ActivityMode> = ActivityMode.entries.toSet(),
    override val completionPolicy: ActivityCompletionPolicy = ActivityCompletionPolicy(),
) : InteractiveLearningActivity {
    init {
        require(title.isNotBlank() && learningObjectives.isNotEmpty() && guidedInstructions.isNotEmpty())
        require(controls.isNotEmpty() && presets.isNotEmpty() && challenges.size >= 3)
        require(formulaOrProcess.isNotBlank() && assumptions.isNotEmpty())
        require(diagramParts.map { it.id }.distinct().size == diagramParts.size)
    }
}

data class ActivityRunSnapshot(
    val mode: ActivityMode,
    val controls: Map<String, Double>,
    val visual: VisualState,
    val interactionCount: Int,
    val prediction: String?,
    val predictionCorrect: Boolean?,
    val observation: String?,
    val reflection: String?,
    val completedChallengeIds: Set<String>,
    val completed: Boolean,
    val focusTargetId: String? = null,
)

/** Stable, allocation-light state holder. Subject rules enter only through ActivityDefinition.compute. */
class FunctionalInteractiveActivity(val definition: ActivityDefinition) {
    private val initial = definition.controls.associate { it.id to it.initial }
    private var focusTargetId: String? = null
    var snapshot: ActivityRunSnapshot = make(ActivityMode.GUIDED, initial, 0, null, null, null, null, emptySet())
        private set

    fun setMode(mode: ActivityMode) { require(mode in definition.supportedModes); snapshot = snapshot.copy(mode = mode) }
    fun focus(targetId: String?) {
        if (targetId != null) require(definition.controls.any { it.id == targetId } || definition.diagramParts.any { it.id == targetId })
        focusTargetId = targetId
        snapshot = snapshot.copy(focusTargetId = targetId, visual = snapshot.visual.copy(highlightedPartId = targetId?.takeIf { id -> definition.diagramParts.any { it.id == id } }))
    }
    fun setControl(id: String, value: Double) {
        val control = definition.controls.singleOrNull { it.id == id } ?: error("Unknown control $id")
        val updated = snapshot.controls + (id to control.clamp(value))
        snapshot = make(snapshot.mode, updated, snapshot.interactionCount + 1, snapshot.prediction, snapshot.predictionCorrect, snapshot.observation, snapshot.reflection, snapshot.completedChallengeIds)
    }
    fun predict(text: String, correct: Boolean? = null) { snapshot = snapshot.copy(prediction = text.trim().takeIf(String::isNotEmpty), predictionCorrect = correct); refreshCompletion() }
    fun observe(text: String) { snapshot = snapshot.copy(observation = text.trim().takeIf(String::isNotEmpty)); refreshCompletion() }
    fun reflect(text: String) { snapshot = snapshot.copy(reflection = text.trim().takeIf(String::isNotEmpty)); refreshCompletion() }
    fun checkChallenge(id: String): Boolean {
        val challenge = definition.challenges.singleOrNull { it.id == id } ?: error("Unknown challenge $id")
        val passed = challenge.isSatisfied(snapshot.visual)
        if (passed) { snapshot = snapshot.copy(completedChallengeIds = snapshot.completedChallengeIds + id); refreshCompletion() }
        return passed
    }
    fun applyPreset(id: String) {
        val preset = definition.presets.singleOrNull { it.id == id } ?: error("Unknown preset $id")
        val valid = preset.values.mapValues { (key, value) -> definition.controls.single { it.id == key }.clamp(value) }
        snapshot = make(snapshot.mode, snapshot.controls + valid, snapshot.interactionCount + 1, snapshot.prediction, snapshot.predictionCorrect, snapshot.observation, snapshot.reflection, snapshot.completedChallengeIds)
    }
    fun reset() { focusTargetId = null; snapshot = make(ActivityMode.GUIDED, initial, 0, null, null, null, null, emptySet()) }
    fun update(deltaTimeSeconds: Float) { require(deltaTimeSeconds in 0f..0.1f); snapshot = snapshot.copy(visual = definition.compute(snapshot.controls)) }

    private fun make(mode: ActivityMode, controls: Map<String, Double>, interactions: Int, prediction: String?, predictionCorrect: Boolean?, observation: String?, reflection: String?, challenges: Set<String>): ActivityRunSnapshot {
        val computed = definition.compute(controls)
        val visual = computed.copy(highlightedPartId = focusTargetId?.takeIf { id -> definition.diagramParts.any { it.id == id } } ?: computed.highlightedPartId)
        val p = definition.completionPolicy
        val completed = interactions >= p.requiredInteractions && (!p.requiresPrediction || prediction != null) && (!p.requiresObservation || observation != null) && (!p.requiresChallenge || challenges.isNotEmpty()) && (!p.requiresReflectionQuestion || reflection != null)
        return ActivityRunSnapshot(mode, controls, visual, interactions, prediction, predictionCorrect, observation, reflection, challenges, completed, focusTargetId)
    }
    private fun refreshCompletion() { snapshot = make(snapshot.mode, snapshot.controls, snapshot.interactionCount, snapshot.prediction, snapshot.predictionCorrect, snapshot.observation, snapshot.reflection, snapshot.completedChallengeIds) }
}

data class ActivityProgressRecord(
    val activityId: String,
    val lastVariables: Map<String, Double>,
    val guidedCompleted: Boolean,
    val challengeScore: Int,
    val predictionAttempts: Int,
    val predictionCorrect: Int,
    val observations: List<String>,
    val favourite: Boolean,
    val mastery: Double,
)

interface ActivityProgressStore { fun load(activityId: String): ActivityProgressRecord?; fun save(record: ActivityProgressRecord) }
class InMemoryActivityProgressStore : ActivityProgressStore {
    private val records = mutableMapOf<String, ActivityProgressRecord>()
    override fun load(activityId: String) = records[activityId]
    override fun save(record: ActivityProgressRecord) { records[record.activityId] = record.copy(observations = record.observations.takeLast(20)) }
}

fun near(actual: Double?, expected: Double, tolerance: Double) = actual != null && abs(actual - expected) <= tolerance
