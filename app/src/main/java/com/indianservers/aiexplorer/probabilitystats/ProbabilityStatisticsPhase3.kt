package com.indianservers.aiexplorer.probabilitystats

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val P3Space = Color(0xFF010512)
private val P3Panel = Color(0xEE09152D)
private val P3Ink = Color(0xFFF7FAFF)
private val P3Muted = Color(0xFFAAB9D8)
private val P3Cyan = Color(0xFF27DFFF)
private val P3Purple = Color(0xFFAA6CFF)
private val P3Green = Color(0xFF4FE5A6)
private val P3Amber = Color(0xFFFFB83F)
private val P3Pink = Color(0xFFFF67A5)
private val P3Red = Color(0xFFFF657C)

internal data class StatPoint(val x: Double, val y: Double)
internal enum class RegressionModel(val label: String) {
    Linear("Linear"), Quadratic("Quadratic"), Exponential("Exponential"),
    Logarithmic("Logarithmic"), Power("Power"),
}
internal data class RegressionResult(
    val model: RegressionModel,
    val coefficients: List<Double>,
    val predicted: List<Double>,
    val residuals: List<Double>,
    val pearson: Double,
    val spearman: Double,
    val rSquared: Double,
    val equation: String,
    val error: String? = null,
)

internal object Phase3RegressionEngine {
    fun fit(points: List<StatPoint>, model: RegressionModel): RegressionResult {
        if (points.size < if (model == RegressionModel.Quadratic) 3 else 2) return invalid(model, points, "Add more points to fit this model.")
        val x = points.map { it.x }
        val y = points.map { it.y }
        val pearson = correlation(x, y)
        val spearman = correlation(ranks(x), ranks(y))
        val coefficients = when (model) {
            RegressionModel.Linear -> linear(x, y)
            RegressionModel.Quadratic -> quadratic(x, y)
            RegressionModel.Exponential -> {
                if (y.any { it <= 0 }) return invalid(model, points, "Exponential regression requires positive Y values.")
                val transformed = linear(x, y.map(::ln))
                listOf(exp(transformed[0]), transformed[1])
            }
            RegressionModel.Logarithmic -> {
                if (x.any { it <= 0 }) return invalid(model, points, "Logarithmic regression requires positive X values.")
                linear(x.map(::ln), y)
            }
            RegressionModel.Power -> {
                if (x.any { it <= 0 } || y.any { it <= 0 }) return invalid(model, points, "Power regression requires positive X and Y values.")
                val transformed = linear(x.map(::ln), y.map(::ln))
                listOf(exp(transformed[0]), transformed[1])
            }
        }
        val predicted = x.map { predict(model, coefficients, it) }
        if (predicted.any { !it.isFinite() }) return invalid(model, points, "This dataset cannot produce a stable fit.")
        val residuals = y.zip(predicted) { actual, fitted -> actual - fitted }
        val mean = y.average()
        val total = y.sumOf { (it - mean).pow(2) }
        val residual = residuals.sumOf { it * it }
        val r2 = if (total < 1e-12) 1.0 else (1 - residual / total).coerceAtMost(1.0)
        return RegressionResult(model, coefficients, predicted, residuals, pearson, spearman, r2, equation(model, coefficients))
    }

    fun predict(model: RegressionModel, c: List<Double>, x: Double): Double = when (model) {
        RegressionModel.Linear -> c[0] + c[1] * x
        RegressionModel.Quadratic -> c[0] + c[1] * x + c[2] * x * x
        RegressionModel.Exponential -> c[0] * exp(c[1] * x)
        RegressionModel.Logarithmic -> if (x > 0) c[0] + c[1] * ln(x) else Double.NaN
        RegressionModel.Power -> if (x > 0) c[0] * x.pow(c[1]) else Double.NaN
    }

    private fun linear(x: List<Double>, y: List<Double>): List<Double> {
        val mx = x.average()
        val my = y.average()
        val denominator = x.sumOf { (it - mx).pow(2) }
        val slope = if (denominator < 1e-12) 0.0 else x.indices.sumOf { (x[it] - mx) * (y[it] - my) } / denominator
        return listOf(my - slope * mx, slope)
    }

    private fun quadratic(x: List<Double>, y: List<Double>): List<Double> {
        val n = x.size.toDouble()
        val sx = x.sum()
        val sx2 = x.sumOf { it * it }
        val sx3 = x.sumOf { it.pow(3) }
        val sx4 = x.sumOf { it.pow(4) }
        val sy = y.sum()
        val sxy = x.indices.sumOf { x[it] * y[it] }
        val sx2y = x.indices.sumOf { x[it] * x[it] * y[it] }
        return solve3(
            arrayOf(doubleArrayOf(n, sx, sx2), doubleArrayOf(sx, sx2, sx3), doubleArrayOf(sx2, sx3, sx4)),
            doubleArrayOf(sy, sxy, sx2y),
        )
    }

    private fun solve3(matrix: Array<DoubleArray>, values: DoubleArray): List<Double> {
        for (pivot in 0..2) {
            val best = (pivot..2).maxBy { abs(matrix[it][pivot]) }
            val row = matrix[pivot]; matrix[pivot] = matrix[best]; matrix[best] = row
            val v = values[pivot]; values[pivot] = values[best]; values[best] = v
            if (abs(matrix[pivot][pivot]) < 1e-12) return listOf(0.0, 0.0, 0.0)
            val divisor = matrix[pivot][pivot]
            for (column in pivot..2) matrix[pivot][column] /= divisor
            values[pivot] /= divisor
            for (other in 0..2) if (other != pivot) {
                val factor = matrix[other][pivot]
                for (column in pivot..2) matrix[other][column] -= factor * matrix[pivot][column]
                values[other] -= factor * values[pivot]
            }
        }
        return values.toList()
    }

    private fun correlation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0
        val mx = x.average(); val my = y.average()
        val numerator = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val denominator = sqrt(x.sumOf { (it - mx).pow(2) } * y.sumOf { (it - my).pow(2) })
        return if (denominator < 1e-12) 0.0 else (numerator / denominator).coerceIn(-1.0, 1.0)
    }

    private fun ranks(values: List<Double>): List<Double> {
        val sorted = values.withIndex().sortedBy { it.value }
        val output = MutableList(values.size) { 0.0 }
        var start = 0
        while (start < sorted.size) {
            var end = start
            while (end + 1 < sorted.size && sorted[end + 1].value == sorted[start].value) end++
            val rank = (start + end + 2) / 2.0
            for (i in start..end) output[sorted[i].index] = rank
            start = end + 1
        }
        return output
    }

    private fun equation(model: RegressionModel, c: List<Double>): String = when (model) {
        RegressionModel.Linear -> "y = ${f(c[0])} ${signed(c[1])}x"
        RegressionModel.Quadratic -> "y = ${f(c[0])} ${signed(c[1])}x ${signed(c[2])}x²"
        RegressionModel.Exponential -> "y = ${f(c[0])}e^(${f(c[1])}x)"
        RegressionModel.Logarithmic -> "y = ${f(c[0])} ${signed(c[1])}ln(x)"
        RegressionModel.Power -> "y = ${f(c[0])}x^${f(c[1])}"
    }
    private fun signed(value: Double) = if (value >= 0) "+ ${f(value)}" else "- ${f(abs(value))}"
    private fun f(value: Double) = String.format(Locale.US, "%.3f", value)
    private fun invalid(model: RegressionModel, points: List<StatPoint>, message: String) =
        RegressionResult(model, emptyList(), emptyList(), emptyList(), 0.0, 0.0, 0.0, "", message)
}

internal enum class ConfidenceKind(val label: String) {
    OneMean("One mean"), OneProportion("One proportion"),
    DifferenceMeans("Difference of means"), DifferenceProportions("Difference of proportions"),
}
internal data class ConfidenceRequest(
    val kind: ConfidenceKind,
    val level: Double,
    val estimate1: Double,
    val spread1: Double,
    val size1: Int,
    val estimate2: Double = 0.0,
    val spread2: Double = 0.0,
    val size2: Int = 1,
)
internal data class ConfidenceResult(
    val estimate: Double, val standardError: Double, val criticalValue: Double,
    val margin: Double, val lower: Double, val upper: Double, val valid: Boolean = true,
    val error: String? = null,
)

internal object Phase3InferenceEngine {
    fun confidence(request: ConfidenceRequest): ConfidenceResult {
        if (request.level !in .50..0.999 || request.size1 < 2 || request.size2 < 1) return invalidCi("Check confidence level and sample sizes.")
        val estimate: Double
        val se: Double
        when (request.kind) {
            ConfidenceKind.OneMean -> {
                if (request.spread1 < 0) return invalidCi("Standard deviation cannot be negative.")
                estimate = request.estimate1
                se = request.spread1 / sqrt(request.size1.toDouble())
            }
            ConfidenceKind.OneProportion -> {
                if (request.estimate1 !in 0.0..1.0) return invalidCi("A proportion must be between 0 and 1.")
                estimate = request.estimate1
                se = sqrt(estimate * (1 - estimate) / request.size1)
            }
            ConfidenceKind.DifferenceMeans -> {
                if (request.size2 < 2 || request.spread1 < 0 || request.spread2 < 0) return invalidCi("Both samples need valid sizes and standard deviations.")
                estimate = request.estimate1 - request.estimate2
                se = sqrt(request.spread1.pow(2) / request.size1 + request.spread2.pow(2) / request.size2)
            }
            ConfidenceKind.DifferenceProportions -> {
                if (request.size2 < 2 || request.estimate1 !in 0.0..1.0 || request.estimate2 !in 0.0..1.0) return invalidCi("Both proportions must be between 0 and 1.")
                estimate = request.estimate1 - request.estimate2
                se = sqrt(request.estimate1 * (1 - request.estimate1) / request.size1 + request.estimate2 * (1 - request.estimate2) / request.size2)
            }
        }
        val critical = inverseNormal(0.5 + request.level / 2)
        val margin = critical * se
        return ConfidenceResult(estimate, se, critical, margin, estimate - margin, estimate + margin)
    }

    fun normalCdf(z: Double): Double {
        val sign = if (z < 0) -1 else 1
        val x = abs(z) / sqrt(2.0)
        val t = 1.0 / (1.0 + 0.3275911 * x)
        val erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - .284496736) * t + .254829592) * t * exp(-x * x)
        return .5 * (1 + sign * erf)
    }

    fun inverseNormal(probability: Double): Double {
        require(probability in 0.0..1.0)
        var low = -8.0; var high = 8.0
        repeat(80) {
            val mid = (low + high) / 2
            if (normalCdf(mid) < probability) low = mid else high = mid
        }
        return (low + high) / 2
    }

    fun oneSampleTest(mean: Double, nullMean: Double, standardDeviation: Double, size: Int, alpha: Double): HypothesisResult {
        if (size < 2 || standardDeviation <= 0 || alpha !in .001..0.25) return HypothesisResult(false, 0.0, 1.0, alpha, "Enter n >= 2, SD > 0 and a valid alpha.")
        val statistic = (mean - nullMean) / (standardDeviation / sqrt(size.toDouble()))
        val p = (2 * (1 - normalCdf(abs(statistic)))).coerceIn(0.0, 1.0)
        return HypothesisResult(true, statistic, p, alpha, if (p < alpha) "Reject the null hypothesis." else "Fail to reject the null hypothesis.")
    }

    private fun invalidCi(message: String) = ConfidenceResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, message)
}

internal enum class HypothesisKind(val label: String) {
    OneSampleZ("One-sample z"), OneSampleT("One-sample t"), TwoSampleT("Two-sample t"),
    PairedT("Paired t"), OneProportion("One proportion"), TwoProportion("Two proportions"),
    ChiSquareFit("Chi-square fit"), ChiSquareIndependence("Chi-square independence"), Anova("One-way ANOVA"),
}
internal data class HypothesisResult(
    val valid: Boolean, val statistic: Double, val pValue: Double, val alpha: Double, val conclusion: String,
)

internal sealed class CommandResult {
    data class Success(val text: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
internal object Phase3CommandEngine {
    private val pattern = Regex("""^\s*([A-Za-z]+)\s*\((.*)\)\s*$""")
    fun execute(command: String, points: List<StatPoint>): CommandResult {
        val match = pattern.matchEntire(command) ?: return CommandResult.Error("Use Function(arguments), for example Mean(Y).")
        val name = match.groupValues[1].lowercase()
        val args = match.groupValues[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val y = points.map { it.y }
        return when (name) {
            "mean" -> if (y.isEmpty()) CommandResult.Error("Dataset is empty.") else CommandResult.Success("Mean(Y) = ${p3f(y.average())}")
            "median" -> if (y.isEmpty()) CommandResult.Error("Dataset is empty.") else {
                val sorted = y.sorted(); val mid = sorted.size / 2
                val value = if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
                CommandResult.Success("Median(Y) = ${p3f(value)}")
            }
            "correlation" -> {
                val fit = Phase3RegressionEngine.fit(points, RegressionModel.Linear)
                if (fit.error != null) CommandResult.Error(fit.error) else CommandResult.Success("Correlation(X,Y) = ${p3f(fit.pearson)}")
            }
            "regression" -> {
                val model = args.lastOrNull()?.let { value -> RegressionModel.entries.firstOrNull { it.label.equals(value, true) } } ?: RegressionModel.Linear
                val fit = Phase3RegressionEngine.fit(points, model)
                if (fit.error != null) CommandResult.Error(fit.error) else CommandResult.Success("${fit.equation}; R² = ${p3f(fit.rSquared)}")
            }
            "normal" -> {
                val numbers = args.mapNotNull(String::toDoubleOrNull)
                if (numbers.size != 2 || numbers[1] <= 0) CommandResult.Error("Normal(mean, standardDeviation)") else CommandResult.Success("Normal distribution N(${p3f(numbers[0])}, ${p3f(numbers[1])}²)")
            }
            else -> CommandResult.Error("Unknown command. Try Mean, Median, Correlation, Regression or Normal.")
        }
    }
}

internal enum class Phase3Route { Hub, Regression, Confidence, Hypothesis, Workspaces, Commands, Guided }
internal data class SavedWorkspace(val id: Long, val name: String, val createdAt: Long, val points: List<StatPoint>)
internal data class Phase3State(
    val route: Phase3Route = Phase3Route.Hub,
    val points: List<StatPoint> = listOf(StatPoint(1.0, 2.0), StatPoint(2.0, 3.2), StatPoint(3.0, 3.8), StatPoint(4.0, 5.5), StatPoint(5.0, 5.9), StatPoint(6.0, 7.4)),
    val selectedPoint: Int? = null,
    val regressionModel: RegressionModel = RegressionModel.Linear,
    val confidenceKind: ConfidenceKind = ConfidenceKind.OneMean,
    val confidenceLevel: Double = .95,
    val estimate1: Double = 72.0,
    val spread1: Double = 12.0,
    val size1: Int = 40,
    val estimate2: Double = 68.0,
    val spread2: Double = 10.0,
    val size2: Int = 35,
    val hypothesisKind: HypothesisKind = HypothesisKind.OneSampleT,
    val nullMean: Double = 70.0,
    val alpha: Double = .05,
    val hypothesisStep: Int = 1,
    val command: String = "",
    val commandResult: String = "",
    val commandHistory: List<String> = emptyList(),
    val workspaces: List<SavedWorkspace> = emptyList(),
    val guidedStep: Int = 0,
    val guidedAttempted: Boolean = false,
)

internal class ProbabilityPhase3ViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("probability_phase3", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(Phase3State(workspaces = loadWorkspaces()))
    val state: StateFlow<Phase3State> = _state.asStateFlow()

    fun open(route: Phase3Route) { _state.value = _state.value.copy(route = route) }
    fun home() { _state.value = _state.value.copy(route = Phase3Route.Hub) }
    fun regressionModel(model: RegressionModel) { _state.value = _state.value.copy(regressionModel = model) }
    fun selectPoint(index: Int?) { _state.value = _state.value.copy(selectedPoint = index) }
    fun addPoint(point: StatPoint) { _state.value = _state.value.copy(points = (_state.value.points + point).take(80), selectedPoint = _state.value.points.size) }
    fun movePoint(index: Int, point: StatPoint) {
        if (index !in _state.value.points.indices) return
        _state.value = _state.value.copy(points = _state.value.points.mapIndexed { i, old -> if (i == index) point else old })
    }
    fun removeSelected() {
        val index = _state.value.selectedPoint ?: return
        _state.value = _state.value.copy(points = _state.value.points.filterIndexed { i, _ -> i != index }, selectedPoint = null)
    }
    fun samplePoints(kind: Int) {
        val points = (1..12).map { index ->
            val x = index.toDouble()
            val wobble = ((index * 17) % 7 - 3) * .3
            StatPoint(x, when (kind) { 0 -> 1.1 * x + wobble; 1 -> 15 - x + wobble; else -> (7 + ((index * 11) % 9 - 4)).toDouble() })
        }
        _state.value = _state.value.copy(points = points, selectedPoint = null)
    }
    fun confidenceKind(kind: ConfidenceKind) { _state.value = _state.value.copy(confidenceKind = kind) }
    fun confidence(level: Double? = null, estimate1: Double? = null, spread1: Double? = null, size1: Int? = null, estimate2: Double? = null, spread2: Double? = null, size2: Int? = null) {
        val old = _state.value
        _state.value = old.copy(
            confidenceLevel = level ?: old.confidenceLevel, estimate1 = estimate1 ?: old.estimate1,
            spread1 = spread1 ?: old.spread1, size1 = size1 ?: old.size1,
            estimate2 = estimate2 ?: old.estimate2, spread2 = spread2 ?: old.spread2, size2 = size2 ?: old.size2,
        )
    }
    fun hypothesisKind(kind: HypothesisKind) { _state.value = _state.value.copy(hypothesisKind = kind, hypothesisStep = 1) }
    fun hypothesis(nullMean: Double? = null, alpha: Double? = null) { _state.value = _state.value.copy(nullMean = nullMean ?: _state.value.nullMean, alpha = alpha ?: _state.value.alpha) }
    fun hypothesisStep(step: Int) { _state.value = _state.value.copy(hypothesisStep = step.coerceIn(1, 8)) }
    fun command(value: String) { _state.value = _state.value.copy(command = value.take(120)) }
    fun executeCommand() {
        val current = _state.value
        val result = Phase3CommandEngine.execute(current.command, current.points)
        val text = when (result) { is CommandResult.Success -> result.text; is CommandResult.Error -> "Error: ${result.message}" }
        _state.value = current.copy(commandResult = text, commandHistory = (listOf(current.command) + current.commandHistory).distinct().take(8))
    }
    fun guideAttempt() { _state.value = _state.value.copy(guidedAttempted = true) }
    fun guideNext() { _state.value = _state.value.copy(guidedStep = (_state.value.guidedStep + 1).coerceAtMost(2), guidedAttempted = false) }

    fun saveWorkspace() {
        val current = _state.value
        val saved = SavedWorkspace(System.currentTimeMillis(), "Regression ${current.workspaces.size + 1}", System.currentTimeMillis(), current.points)
        val next = listOf(saved) + current.workspaces
        persist(next)
        _state.value = current.copy(workspaces = next)
    }
    fun restore(workspace: SavedWorkspace) { _state.value = _state.value.copy(points = workspace.points, route = Phase3Route.Regression, selectedPoint = null) }
    fun delete(workspace: SavedWorkspace) {
        val next = _state.value.workspaces.filterNot { it.id == workspace.id }
        persist(next); _state.value = _state.value.copy(workspaces = next)
    }
    fun exportText(): String {
        val points = _state.value.points.joinToString(",") { """{"x":${it.x},"y":${it.y}}""" }
        return """{"version":1,"tool":"regression","name":"Shared Probability Workspace","points":[$points]}"""
    }

    private fun persist(workspaces: List<SavedWorkspace>) {
        preferences.edit().putString("items", workspaces.joinToString("\n", transform = ::encodeWorkspace)).apply()
    }
    private fun loadWorkspaces(): List<SavedWorkspace> = preferences.getString("items", "").orEmpty().lineSequence().mapNotNull(::decodeWorkspace).toList()
    private fun encodeWorkspace(workspace: SavedWorkspace): String =
        "${workspace.id}|${workspace.createdAt}|${workspace.name.replace("|", " ")}|${workspace.points.joinToString(";") { "${it.x},${it.y}" }}"
    private fun decodeWorkspace(value: String): SavedWorkspace? = runCatching {
        val parts = value.split("|", limit = 4)
        SavedWorkspace(parts[0].toLong(), parts[2], parts[1].toLong(), parts[3].split(";").map {
            val pair = it.split(","); StatPoint(pair[0].toDouble(), pair[1].toDouble())
        })
    }.getOrNull()
}

@Composable
internal fun ProbabilityStatisticsPhase3Screen(
    onBackToPhase2: () -> Unit,
    onExit: () -> Unit,
    model: ProbabilityPhase3ViewModel = viewModel(),
) {
    val state by model.state.collectAsState()
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF25134B), P3Space, Color.Black), center = Offset(250f, 50f), radius = 1200f))) {
        when (state.route) {
            Phase3Route.Hub -> Phase3Hub(model, onBackToPhase2, onExit)
            Phase3Route.Regression -> RegressionLab(state, model, onExit)
            Phase3Route.Confidence -> ConfidenceLab(state, model, onExit)
            Phase3Route.Hypothesis -> HypothesisLab(state, model, onExit)
            Phase3Route.Workspaces -> WorkspacesLab(state, model, onExit)
            Phase3Route.Commands -> CommandsLab(state, model, onExit)
            Phase3Route.Guided -> GuidedLab(state, model, onExit)
        }
    }
}

private data class P3Tool(val title: String, val detail: String, val icon: String, val accent: Color, val route: Phase3Route)

@Composable
private fun Phase3Hub(model: ProbabilityPhase3ViewModel, onBack: () -> Unit, onExit: () -> Unit) {
    val tools = listOf(
        P3Tool("Regression Studio", "Drag points; compare five live fits and residuals", "r", P3Cyan, Phase3Route.Regression),
        P3Tool("Confidence Intervals", "Means, proportions and two-sample intervals", "CI", P3Green, Phase3Route.Confidence),
        P3Tool("Hypothesis Testing", "Guided eight-step inference workflow", "H0", P3Pink, Phase3Route.Hypothesis),
        P3Tool("Saved Workspaces", "Save, restore, delete and share analyses", "S", P3Amber, Phase3Route.Workspaces),
        P3Tool("Command Console", "Safe typed statistics commands with history", ">", P3Purple, Phase3Route.Commands),
        P3Tool("Guided Activities", "Attempt, observe, explain and validate", "?", Color(0xFF7CA5FF), Phase3Route.Guided),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        P3TopBar("Probability & Statistics", "PHASE 3 - ANALYZE & EXPLAIN", onBack, onExit)
        P3Panel(P3Purple) {
            Text("Turn patterns into evidence", color = P3Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Fit models, quantify uncertainty, test claims and preserve your interactive work.", color = P3Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                P3Badge("LIVE FITS", P3Cyan); P3Badge("INFERENCE", P3Green); P3Badge("LOCAL SAVE", P3Amber)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 720.dp) 3 else 2
            val gap = 8.dp
            val cardWidth = (maxWidth - gap * (columns - 1)) / columns
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                tools.forEach { tool ->
                    Column(
                        Modifier.width(cardWidth).heightIn(min = 140.dp).background(Brush.linearGradient(listOf(tool.accent.copy(.2f), P3Panel)), RoundedCornerShape(20.dp))
                            .border(1.dp, tool.accent.copy(.65f), RoundedCornerShape(20.dp)).clickable { model.open(tool.route) }
                            .semantics { contentDescription = "Open ${tool.title}. ${tool.detail}" }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            P3Badge(tool.icon, tool.accent); Text("OPEN >", color = tool.accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Text(tool.title, color = P3Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(tool.detail, color = P3Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegressionLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    val result = Phase3RegressionEngine.fit(state.points, state.regressionModel)
    P3Workspace("Regression Studio", "Tap to add - drag to move - select to remove", P3Cyan, model::home, onExit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            RegressionModel.entries.forEach { P3Chip(it.label, it == state.regressionModel, P3Cyan) { model.regressionModel(it) } }
        }
        ScatterCanvas(state, result, model, Modifier.fillMaxWidth().height(275.dp))
        if (result.error != null) Text(result.error, color = P3Red, fontSize = 11.sp) else {
            Text(result.equation, color = P3Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                P3Metric("Pearson r", p3f(result.pearson), P3Cyan, Modifier.weight(1f))
                P3Metric("Spearman", p3f(result.spearman), P3Purple, Modifier.weight(1f))
                P3Metric("R squared", p3f(result.rSquared), P3Green, Modifier.weight(1f))
            }
            ResidualCanvas(result.residuals, Modifier.fillMaxWidth().height(92.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            P3Button("Positive", P3Green) { model.samplePoints(0) }; P3Button("Negative", P3Pink) { model.samplePoints(1) }
            P3Button("No correlation", P3Amber) { model.samplePoints(2) }
            P3OutlineButton("Remove selected", P3Red, model::removeSelected)
            P3OutlineButton("Save workspace", P3Purple, model::saveWorkspace)
        }
    }
}

@Composable
private fun ScatterCanvas(state: Phase3State, result: RegressionResult, model: ProbabilityPhase3ViewModel, modifier: Modifier) {
    fun point(offset: Offset, width: Float, height: Float) = StatPoint((offset.x / width * 14).coerceIn(0f, 14f).toDouble(), ((height - offset.y) / height * 16).coerceIn(0f, 16f).toDouble())
    Canvas(
        modifier.background(Color(0xFF030A1C), RoundedCornerShape(18.dp)).border(1.dp, P3Cyan.copy(.5f), RoundedCornerShape(18.dp))
            .pointerInput(state.points) {
                detectTapGestures { tap ->
                    val nearest = state.points.indices.minByOrNull { index ->
                        val px = (state.points[index].x / 14 * size.width).toFloat()
                        val py = (size.height - state.points[index].y / 16 * size.height).toFloat()
                        (Offset(px, py) - tap).getDistance()
                    }
                    if (nearest != null) {
                        val p = state.points[nearest]
                        val distance = (Offset((p.x / 14 * size.width).toFloat(), (size.height - p.y / 16 * size.height).toFloat()) - tap).getDistance()
                        if (distance < 38f) model.selectPoint(nearest) else model.addPoint(point(tap, size.width.toFloat(), size.height.toFloat()))
                    } else model.addPoint(point(tap, size.width.toFloat(), size.height.toFloat()))
                }
            }.pointerInput(state.points) {
                var dragIndex: Int? = null
                detectDragGestures(
                    onDragStart = { start ->
                        val nearest = state.points.indices.minByOrNull { index ->
                            val p = state.points[index]
                            (Offset((p.x / 14 * size.width).toFloat(), (size.height - p.y / 16 * size.height).toFloat()) - start).getDistance()
                        }
                        dragIndex = nearest
                        model.selectPoint(nearest)
                    },
                    onDragEnd = { dragIndex = null },
                    onDragCancel = { dragIndex = null },
                ) { change, _ ->
                    dragIndex?.let { model.movePoint(it, point(change.position, size.width.toFloat(), size.height.toFloat())) }
                    change.consume()
                }
            }.semantics { contentDescription = "Interactive scatter plot with ${state.points.size} points" },
    ) {
        for (i in 1..7) {
            val x = size.width * i / 8; val y = size.height * i / 8
            drawLine(P3Muted.copy(.12f), Offset(x, 0f), Offset(x, size.height))
            drawLine(P3Muted.copy(.12f), Offset(0f, y), Offset(size.width, y))
        }
        if (result.error == null) {
            val path = Path()
            (0..100).forEach { index ->
                val x = index / 100.0 * 14
                val y = Phase3RegressionEngine.predict(result.model, result.coefficients, x)
                val offset = Offset((x / 14 * size.width).toFloat(), (size.height - y / 16 * size.height).toFloat())
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, P3Cyan, style = Stroke(4f))
        }
        state.points.forEachIndexed { index, p ->
            val center = Offset((p.x / 14 * size.width).toFloat(), (size.height - p.y / 16 * size.height).toFloat())
            drawCircle(if (index == state.selectedPoint) P3Pink else P3Green, if (index == state.selectedPoint) 11f else 8f, center)
        }
    }
}

@Composable
private fun ResidualCanvas(residuals: List<Double>, modifier: Modifier) {
    Canvas(modifier.background(P3Panel, RoundedCornerShape(14.dp)).semantics { contentDescription = "Residual plot" }) {
        val mid = size.height / 2
        drawLine(P3Muted, Offset(0f, mid), Offset(size.width, mid), 2f)
        val scale = max(1.0, residuals.maxOfOrNull(::abs) ?: 1.0)
        residuals.forEachIndexed { index, value ->
            val x = size.width * (index + 1) / (residuals.size + 1)
            val y = mid - (value / scale * mid * .78).toFloat()
            drawLine(P3Purple.copy(.6f), Offset(x, mid), Offset(x, y), 3f); drawCircle(P3Purple, 6f, Offset(x, y))
        }
    }
}

@Composable
private fun ConfidenceLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    val request = ConfidenceRequest(state.confidenceKind, state.confidenceLevel, state.estimate1, state.spread1, state.size1, state.estimate2, state.spread2, state.size2)
    val result = Phase3InferenceEngine.confidence(request)
    P3Workspace("Confidence Intervals", "Estimate uncertainty, not just a point", P3Green, model::home, onExit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ConfidenceKind.entries.forEach { P3Chip(it.label, it == state.confidenceKind, P3Green) { model.confidenceKind(it) } }
        }
        P3Slider("Confidence level", state.confidenceLevel, .80, .99, { "${(it * 100).roundToInt()}%" }) { model.confidence(level = it) }
        P3Slider(if (state.confidenceKind.name.contains("Proportion")) "Proportion 1" else "Mean 1", state.estimate1, if (state.confidenceKind.name.contains("Proportion")) 0.0 else 0.0, if (state.confidenceKind.name.contains("Proportion")) 1.0 else 100.0, ::p3f) { model.confidence(estimate1 = it) }
        if (!state.confidenceKind.name.contains("Proportion")) P3Slider("Standard deviation 1", state.spread1, .1, 30.0, ::p3f) { model.confidence(spread1 = it) }
        P3Slider("Sample size 1", state.size1.toDouble(), 2.0, 500.0, { it.roundToInt().toString() }) { model.confidence(size1 = it.roundToInt()) }
        if (state.confidenceKind == ConfidenceKind.DifferenceMeans || state.confidenceKind == ConfidenceKind.DifferenceProportions) {
            P3Slider(if (state.confidenceKind == ConfidenceKind.DifferenceProportions) "Proportion 2" else "Mean 2", state.estimate2, 0.0, if (state.confidenceKind == ConfidenceKind.DifferenceProportions) 1.0 else 100.0, ::p3f) { model.confidence(estimate2 = it) }
            if (state.confidenceKind == ConfidenceKind.DifferenceMeans) P3Slider("Standard deviation 2", state.spread2, .1, 30.0, ::p3f) { model.confidence(spread2 = it) }
            P3Slider("Sample size 2", state.size2.toDouble(), 2.0, 500.0, { it.roundToInt().toString() }) { model.confidence(size2 = it.roundToInt()) }
        }
        if (!result.valid) Text(result.error.orEmpty(), color = P3Red) else {
            IntervalCanvas(result, Modifier.fillMaxWidth().height(145.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                P3Metric("Estimate", p3f(result.estimate), P3Cyan, Modifier.weight(1f))
                P3Metric("Std. error", p3f(result.standardError), P3Purple, Modifier.weight(1f))
                P3Metric("Margin", p3f(result.margin), P3Amber, Modifier.weight(1f))
            }
            Text("${p3f(result.lower)} < parameter < ${p3f(result.upper)}", color = P3Green, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text("Method: estimate +/- ${p3f(result.criticalValue)} x standard error. In repeated sampling, ${(state.confidenceLevel * 100).roundToInt()}% of intervals made this way capture the true parameter.", color = P3Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IntervalCanvas(result: ConfidenceResult, modifier: Modifier) {
    Canvas(modifier.background(P3Panel, RoundedCornerShape(16.dp)).semantics { contentDescription = "Confidence interval from ${p3f(result.lower)} to ${p3f(result.upper)}" }) {
        val left = size.width * .12f; val right = size.width * .88f; val y = size.height * .55f
        drawLine(P3Muted.copy(.5f), Offset(left, y), Offset(right, y), 3f)
        drawLine(P3Green, Offset(size.width * .25f, y), Offset(size.width * .75f, y), 8f)
        drawCircle(P3Cyan, 10f, Offset(size.width / 2, y))
        drawLine(P3Green, Offset(size.width * .25f, y - 22), Offset(size.width * .25f, y + 22), 5f)
        drawLine(P3Green, Offset(size.width * .75f, y - 22), Offset(size.width * .75f, y + 22), 5f)
    }
}

@Composable
private fun HypothesisLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    val result = Phase3InferenceEngine.oneSampleTest(state.estimate1, state.nullMean, state.spread1, state.size1, state.alpha)
    val steps = listOf("Select test", "Define hypotheses", "Enter data", "Select alpha", "Check assumptions", "Test statistic", "P-value", "Interpret")
    P3Workspace("Hypothesis Testing", "Guided evidence workflow - step ${state.hypothesisStep} of 8", P3Pink, model::home, onExit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            steps.indices.forEach { index -> Box(Modifier.weight(1f).height(5.dp).background(if (index < state.hypothesisStep) P3Pink else P3Muted.copy(.2f), CircleShape)) }
        }
        Text(steps[state.hypothesisStep - 1], color = P3Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        when (state.hypothesisStep) {
            1 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                HypothesisKind.entries.forEach { P3Chip(it.label, it == state.hypothesisKind, P3Pink) { model.hypothesisKind(it) } }
            }
            2 -> P3Panel(P3Pink) { Text("H0: population parameter = ${p3f(state.nullMean)}", color = P3Ink); Text("H1: population parameter is not equal to ${p3f(state.nullMean)}", color = P3Muted) }
            3 -> { P3Slider("Sample mean", state.estimate1, 0.0, 100.0, ::p3f) { model.confidence(estimate1 = it) }; P3Slider("Sample SD", state.spread1, .1, 30.0, ::p3f) { model.confidence(spread1 = it) }; P3Slider("Sample size", state.size1.toDouble(), 2.0, 500.0, { it.roundToInt().toString() }) { model.confidence(size1 = it.roundToInt()) } }
            4 -> P3Slider("Significance alpha", state.alpha, .01, .10, ::p3f) { model.hypothesis(alpha = it) }
            5 -> P3Panel(P3Green) { Text("Assumptions checklist", color = P3Green, fontWeight = FontWeight.Bold); Text("Independent observations; random/representative sample; population approximately normal or n >= 30.", color = P3Muted, fontSize = 11.sp) }
            6 -> { P3Metric("Test statistic", p3f(result.statistic), P3Pink, Modifier.fillMaxWidth()); NormalEvidenceCanvas(result, Modifier.fillMaxWidth().height(150.dp)) }
            7 -> { P3Metric("Two-sided p-value", p3f(result.pValue), P3Amber, Modifier.fillMaxWidth()); Text("The shaded tails represent outcomes at least as extreme as the observed statistic under H0.", color = P3Muted, fontSize = 11.sp) }
            8 -> { Text(result.conclusion, color = if (result.pValue < state.alpha) P3Pink else P3Green, fontSize = 19.sp, fontWeight = FontWeight.Black); Text("Because p = ${p3f(result.pValue)} ${if (result.pValue < state.alpha) "<" else ">="} alpha = ${p3f(state.alpha)}. This is a decision about evidence, not proof that either hypothesis is true.", color = P3Muted, fontSize = 11.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            P3OutlineButton("Previous", P3Muted) { model.hypothesisStep(state.hypothesisStep - 1) }
            P3Button(if (state.hypothesisStep == 8) "Restart" else "Next step", P3Pink) { model.hypothesisStep(if (state.hypothesisStep == 8) 1 else state.hypothesisStep + 1) }
        }
    }
}

@Composable
private fun NormalEvidenceCanvas(result: HypothesisResult, modifier: Modifier) {
    Canvas(modifier.background(P3Panel, RoundedCornerShape(16.dp))) {
        val path = Path(); val base = size.height * .82f
        for (i in 0..160) {
            val z = -4 + i / 20.0
            val density = exp(-z * z / 2)
            val point = Offset(size.width * i / 160, base - (density * size.height * .62).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, P3Pink, style = Stroke(4f))
        val marker = ((result.statistic.coerceIn(-4.0, 4.0) + 4) / 8 * size.width).toFloat()
        drawLine(P3Amber, Offset(marker, base), Offset(marker, size.height * .15f), 4f)
    }
}

@Composable
private fun WorkspacesLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    P3Workspace("Saved Workspaces", "Versioned local snapshots of your interactive data", P3Amber, model::home, onExit) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            P3Button("Save current", P3Amber, model::saveWorkspace)
            P3OutlineButton("Share data", P3Cyan) {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Probability workspace"); putExtra(Intent.EXTRA_TEXT, model.exportText())
                }, "Share workspace"))
            }
        }
        if (state.workspaces.isEmpty()) {
            P3Panel(P3Muted) { Text("No saved workspaces yet.", color = P3Ink, fontWeight = FontWeight.Bold); Text("Save the current scatter dataset, then restore it here at any time.", color = P3Muted, fontSize = 11.sp) }
        }
        state.workspaces.forEach { workspace ->
            P3Panel(P3Amber) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(workspace.name, color = P3Ink, fontWeight = FontWeight.Bold); Text("${workspace.points.size} points - version 1", color = P3Muted, fontSize = 10.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { P3Button("Restore", P3Green) { model.restore(workspace) }; P3OutlineButton("Delete", P3Red) { model.delete(workspace) } }
                }
            }
        }
        Text("Workspaces stay on this device. Shared exports contain numeric points only and never execute code.", color = P3Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CommandsLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    P3Workspace("Command Console", "A small, safe parser - no eval or arbitrary execution", P3Purple, model::home, onExit) {
        OutlinedTextField(value = state.command, onValueChange = model::command, label = { Text("Command") }, placeholder = { Text("Regression(A, Linear)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Mean(Y)", "Median(Y)", "Correlation(A)", "Regression(A, Linear)", "Regression(A, Quadratic)", "Normal(50, 10)").forEach {
                P3Chip(it, false, P3Purple) { model.command(it) }
            }
        }
        P3Button("Run command", P3Purple, model::executeCommand)
        if (state.commandResult.isNotBlank()) P3Panel(if (state.commandResult.startsWith("Error")) P3Red else P3Green) { Text(state.commandResult, color = P3Ink, fontFamily = FontFamily.Monospace) }
        if (state.commandHistory.isNotEmpty()) {
            Text("HISTORY", color = P3Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
            state.commandHistory.forEach { Text("> $it", color = P3Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { model.command(it) }) }
        }
    }
}

@Composable
private fun GuidedLab(state: Phase3State, model: ProbabilityPhase3ViewModel, onExit: () -> Unit) {
    val prompts = listOf(
        "Predict: If one point is dragged far above the pattern, what happens to Pearson r?",
        "Try it in Regression Studio. Compare Pearson and Spearman before and after moving the point.",
        "Explain: Pearson is strongly influenced by distance from the linear pattern. Spearman uses ranks, so a single extreme distance often changes it less.",
    )
    P3Workspace("Guided Activity", "Outliers and correlation", Color(0xFF7CA5FF), model::home, onExit) {
        P3Badge("STEP ${state.guidedStep + 1} OF 3", Color(0xFF7CA5FF))
        Text(prompts[state.guidedStep], color = P3Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (state.guidedStep == 0 && !state.guidedAttempted) {
            Text("Choose your prediction before the explanation is unlocked.", color = P3Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                P3Button("r gets weaker", P3Cyan, model::guideAttempt); P3OutlineButton("r gets stronger", P3Pink, model::guideAttempt)
            }
        } else {
            if (state.guidedStep == 1) P3Button("Open Regression Studio", P3Cyan) { model.open(Phase3Route.Regression) }
            if (state.guidedStep < 2) P3Button("Continue", Color(0xFF7CA5FF), model::guideNext)
            else P3Panel(P3Green) { Text("Checkpoint complete", color = P3Green, fontWeight = FontWeight.Black); Text("You connected a visual change to two different measures of association.", color = P3Muted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun P3Workspace(title: String, subtitle: String, accent: Color, onBack: () -> Unit, onExit: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        P3TopBar(title, subtitle, onBack, onExit)
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun P3TopBar(title: String, subtitle: String, onBack: () -> Unit, onExit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            P3OutlineButton("<", P3Cyan, onBack)
            Column { Text(title, color = P3Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(subtitle, color = P3Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        }
        P3OutlineButton("EXIT", P3Muted, onExit)
    }
}

@Composable
private fun P3Panel(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.copy(.15f), P3Panel)), RoundedCornerShape(18.dp)).border(1.dp, accent.copy(.45f), RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
private fun P3Badge(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(accent.copy(.14f), RoundedCornerShape(8.dp)).border(1.dp, accent.copy(.4f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
}

@Composable
private fun P3Chip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(text, color = if (selected) P3Space else accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(if (selected) accent else accent.copy(.1f), RoundedCornerShape(11.dp)).border(1.dp, accent.copy(.65f), RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 7.dp))
}

@Composable
private fun P3Button(text: String, accent: Color, onClick: () -> Unit) {
    Text(text, color = P3Space, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.background(accent, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 9.dp))
}

@Composable
private fun P3OutlineButton(text: String, accent: Color, onClick: () -> Unit) {
    Text(text, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.border(1.dp, accent.copy(.7f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp))
}

@Composable
private fun P3Metric(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.background(accent.copy(.1f), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(12.dp)).padding(9.dp)) {
        Text(label, color = P3Muted, fontSize = 9.sp); Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun P3Slider(label: String, value: Double, rangeStart: Double, rangeEnd: Double, format: (Double) -> String, onChange: (Double) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = P3Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(format(value), color = P3Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        Slider(value = value.toFloat().coerceIn(rangeStart.toFloat(), rangeEnd.toFloat()), onValueChange = { onChange(it.toDouble()) }, valueRange = rangeStart.toFloat()..rangeEnd.toFloat())
    }
}

private fun p3f(value: Double): String = when {
    !value.isFinite() -> "-"
    abs(value) >= 1000 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
}
