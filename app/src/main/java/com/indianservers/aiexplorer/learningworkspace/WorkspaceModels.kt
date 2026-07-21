package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.assistant.contracts.SimulationStateSnapshot
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import java.time.Instant

data class NotebookVariable(val id: String, val name: String, val value: Double?, val unit: String?)
data class NotebookMeasurement(val variableId: String, val value: Double, val unit: String, val uncertainty: Double? = null, val recordedAt: Instant = Instant.now())
data class ExperimentNotebookEntry(
    val id: String, val subject: SchoolSubject, val conceptId: String, val activityId: String, val title: String,
    val question: String? = null, val hypothesis: String? = null, val prediction: String? = null,
    val independentVariables: List<NotebookVariable> = emptyList(), val dependentVariables: List<NotebookVariable> = emptyList(), val controlledVariables: List<NotebookVariable> = emptyList(),
    val measurements: List<NotebookMeasurement> = emptyList(), val observations: List<String> = emptyList(), val graphReferences: List<String> = emptyList(), val calculationSteps: List<String> = emptyList(),
    val uncertaintyNotes: List<String> = emptyList(), val conclusion: String? = null, val reflection: String? = null, val savedStateIds: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(), val updatedAt: Instant = Instant.now(),
)

enum class SimulationInputEventType { SET_VALUE, SELECT_MODE, PLAY, PAUSE, RESET, ANNOTATE }
data class SimulationInputEvent(val atMs: Long, val type: SimulationInputEventType, val targetId: String? = null, val numericValue: Double? = null, val textValue: String? = null)
data class SimulationReplay(val simulationId: String, val initialState: SimulationStateSnapshot, val events: List<SimulationInputEvent>, val snapshots: List<SimulationStateSnapshot>, val durationMs: Long)

sealed interface RepresentationAction { data class SetValue(val id: String, val value: Double) : RepresentationAction; data class SetMode(val id: String, val value: String) : RepresentationAction; data class Restore(val snapshot: SimulationStateSnapshot) : RepresentationAction; data object Reset : RepresentationAction }
data class CoordinatedConceptState(val snapshot: SimulationStateSnapshot, val formulaText: String, val graphPoints: List<Pair<Double, Double>>, val tableRows: List<Map<String, String>>, val explanation: String)

data class ParameterSweepDefinition(val simulationId: String, val independentVariableId: String, val startValue: Double, val endValue: Double, val sampleCount: Int, val fixedValues: Map<String, Double>, val measuredOutputIds: Set<String>)
data class SweepSample(val input: Double, val outputs: Map<String, Double>)
data class ParameterSweepResult(val definition: ParameterSweepDefinition, val samples: List<SweepSample>, val trendDescription: String, val minima: Map<String, Double>, val maxima: Map<String, Double>, val exceptionalValues: List<String>, val limitations: List<String>, val provenance: String = "theoretical simulation")

enum class ComparisonMode { SIDE_BY_SIDE, OVERLAY, DIFFERENCE_HIGHLIGHT }
data class StateDifference(val id: String, val firstValue: Double?, val secondValue: Double?, val unit: String?, val delta: Double?)
data class StateComparison(val firstStateId: String, val secondStateId: String, val changedInputs: List<StateDifference>, val changedOutputs: List<StateDifference>, val unchangedValues: List<String>, val visualComparisonMode: ComparisonMode, val explanation: List<String>)

data class MeasurementDataset(val measurementName: String, val unit: String, val values: List<Double>, val instrumentResolution: Double? = null, val expectedValue: Double? = null)
data class MeasurementAnalysis(val mean: Double?, val median: Double?, val range: Double?, val standardDeviation: Double?, val percentageError: Double?, val percentageUncertainty: Double?, val possibleOutlierIndices: List<Int>, val uncertaintyBand: ClosedFloatingPointRange<Double>?, val notes: List<String>)

data class ExperimentVariable(val id: String, val name: String, val unit: String?, val range: ClosedFloatingPointRange<Double>?)
data class MeasurementPlanStep(val order: Int, val instruction: String, val sampleCount: Int)
data class StudentExperimentDesign(val title: String, val question: String, val hypothesis: String?, val independentVariable: ExperimentVariable?, val dependentVariables: List<ExperimentVariable>, val controls: List<ExperimentVariable>, val apparatusIds: List<String>, val measurementPlan: List<MeasurementPlanStep>, val successCriteria: List<String>, val safetyAcknowledgements: List<String>, val additionalIndependentVariables: List<ExperimentVariable> = emptyList())
data class ExperimentDesignValidation(val valid: Boolean, val issues: List<String>)

enum class ProofTaskType { ALGEBRAIC_TRANSFORMATION, GEOMETRIC_CONSTRUCTION, GEOMETRIC_DISSECTION, STATEMENT_REASON, COUNTEREXAMPLE, INVARIANT_DISCOVERY }
data class ProofStep(val statement: String, val reason: String?, val invariantIds: Set<String> = emptySet())
data class ProofEvaluation(val valid: Boolean, val firstInvalidStep: Int?, val checkpointFeedback: List<String>, val preservedInvariants: Set<String>)

data class SketchPoint(val x: Double, val y: Double)
data class GraphStructure(val intercepts: List<Double> = emptyList(), val trend: Int = 0, val slopeSigns: List<Int> = emptyList(), val turningPoints: Int? = null, val asymptotes: List<Double> = emptyList(), val domain: ClosedFloatingPointRange<Double>? = null, val range: ClosedFloatingPointRange<Double>? = null)
data class GraphFeatureEvaluation(val correctIntercepts: Boolean?, val correctTrend: Boolean, val correctSlopeSigns: Boolean?, val correctTurningPoints: Boolean?, val correctAsymptotes: Boolean?, val correctDomain: Boolean?, val correctRange: Boolean?, val overallShapeScore: Double)

enum class DataComplexity { CLASSES_7_8, CLASSES_9_10, CLASSES_11_12, ADVANCED }
data class DataRow(val id: String, val values: Map<String, Double>)
data class DataAnalysisResult(val rows: List<DataRow>, val mean: Map<String, Double>, val median: Map<String, Double>, val range: Map<String, Double>, val standardDeviation: Map<String, Double>, val regressionSlope: Double?, val regressionIntercept: Double?, val residuals: List<Double>, val enabledTools: Set<String>)

data class JourneyStop(val subject: SchoolSubject, val conceptId: String, val whyItMatters: String, val prerequisiteConceptId: String?, val expectedMinutes: Int)
data class CrossSubjectJourney(val id: String, val title: String, val stops: List<JourneyStop>)
data class JourneyPosition(val journeyId: String, val stopIndex: Int, val returnStopIndex: Int?)
