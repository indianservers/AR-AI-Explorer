package com.indianservers.aiexplorer.solver.domain.visualisation

enum class VisualisationType {
    NumberLine,
    BalanceScale,
    FractionArea,
    PercentageBar,
    RatioTable,
    AlgebraTiles,
    TransformationHighlight,
    CoordinateGraph,
    FunctionIntersection,
    QuadraticGeometry,
    UnitCircle,
    Triangle,
    MatrixTransformation,
    RowReductionGrid,
    SequencePattern,
    BarChart,
    ComplexPlane,
    DerivativeTangent,
    IntegralArea,
    VerificationComparison,
}

enum class InteractionMode {
    Static,
    StepLinked,
    Scrubbable,
    Inspectable,
}

data class VisualPoint(
    val x: Double,
    val y: Double,
    val label: String? = null,
)

data class VisualSeries(
    val id: String,
    val label: String,
    val points: List<VisualPoint>,
    val discontinuities: List<Double> = emptyList(),
)

sealed interface VisualisationData {
    data class NumberLine(
        val minimum: Double,
        val maximum: Double,
        val start: Double,
        val end: Double,
        val boundary: Double? = null,
        val boundaryClosed: Boolean = true,
    ) : VisualisationData

    data class BalanceScale(
        val leftLabel: String,
        val rightLabel: String,
        val leftWeight: Double,
        val rightWeight: Double,
    ) : VisualisationData

    data class FractionArea(
        val numerator: Int,
        val denominator: Int,
        val comparisonNumerator: Int? = null,
        val comparisonDenominator: Int? = null,
    ) : VisualisationData

    data class PercentageBar(val percentage: Double) : VisualisationData

    data class RatioTable(val headings: List<String>, val rows: List<List<String>>) : VisualisationData

    data class AlgebraTiles(
        val positiveVariables: Int,
        val negativeVariables: Int,
        val positiveUnits: Int,
        val negativeUnits: Int,
    ) : VisualisationData

    data class TransformationHighlight(
        val before: String,
        val after: String,
        val changedTerms: List<String>,
    ) : VisualisationData

    data class CoordinateGraph(
        val xMinimum: Double,
        val xMaximum: Double,
        val yMinimum: Double,
        val yMaximum: Double,
        val series: List<VisualSeries>,
        val markers: List<VisualPoint> = emptyList(),
        val excludedX: List<Double> = emptyList(),
    ) : VisualisationData

    data class UnitCircle(
        val angleRadians: Double,
        val sine: Double,
        val cosine: Double,
    ) : VisualisationData

    data class Triangle(
        val vertices: List<VisualPoint>,
        val sideLabels: List<String>,
        val angleLabels: List<String> = emptyList(),
    ) : VisualisationData

    data class MatrixGrid(
        val values: List<List<String>>,
        val highlightedRows: Set<Int> = emptySet(),
        val highlightedColumns: Set<Int> = emptySet(),
        val determinant: Double? = null,
    ) : VisualisationData

    data class SequencePattern(
        val terms: List<VisualPoint>,
        val partialSums: List<VisualPoint> = emptyList(),
        val convergent: Boolean? = null,
    ) : VisualisationData

    data class BarChart(
        val labels: List<String>,
        val values: List<Double>,
    ) : VisualisationData

    data class ComplexPlane(
        val points: List<VisualPoint>,
        val vectorsFromOrigin: Boolean = true,
        val branchConvention: String,
    ) : VisualisationData

    data class DerivativeTangent(
        val curve: VisualSeries,
        val point: VisualPoint,
        val tangent: VisualSeries,
        val secants: List<VisualSeries>,
        val slope: Double,
    ) : VisualisationData

    data class IntegralArea(
        val curve: VisualSeries,
        val from: Double,
        val to: Double,
        val rectangles: List<AreaRectangle>,
        val signedArea: Double,
    ) : VisualisationData

    data class VerificationComparison(
        val symbolicClaim: String,
        val checks: List<VisualVerificationDatum>,
    ) : VisualisationData
}

data class AreaRectangle(
    val left: Double,
    val right: Double,
    val height: Double,
)

data class VisualVerificationDatum(
    val label: String,
    val expected: Double,
    val actual: Double,
    val tolerance: Double,
) {
    val passed: Boolean get() = kotlin.math.abs(expected - actual) <= tolerance
}

data class VisualisationSpec(
    val id: String,
    val type: VisualisationType,
    val title: String,
    val linkedStepIds: List<String>,
    val mathematicalData: VisualisationData,
    val interactionMode: InteractionMode,
    val accessibilityDescription: String,
    val explanationKeys: List<String>,
    val domainStatement: String? = null,
)

data class SolverVisualisation(
    val specification: VisualisationSpec,
    val renderVersion: Int = 1,
)

interface SolverVisualisationRenderer {
    fun render(specification: VisualisationSpec): SolverVisualisation
}

object DeclarativeSolverVisualisationRenderer : SolverVisualisationRenderer {
    override fun render(specification: VisualisationSpec) = SolverVisualisation(specification)
}

data class VisualVerificationResult(
    val supported: Boolean,
    val consistent: Boolean,
    val summary: String,
    val checks: List<VisualVerificationDatum>,
)

data class FormulaUnderstanding(
    val id: String,
    val title: String,
    val symbolicDerivation: List<String>,
    val visualInterpretation: String,
    val conditions: List<String>,
    val example: String,
    val verification: String,
    val visualisation: VisualisationSpec,
)
