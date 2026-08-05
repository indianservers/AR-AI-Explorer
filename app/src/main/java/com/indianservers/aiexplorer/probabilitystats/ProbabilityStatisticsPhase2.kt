package com.indianservers.aiexplorer.probabilitystats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.core.DistributionDomain
import com.indianservers.aiexplorer.core.DistributionEngine
import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.DistributionPoint
import com.indianservers.aiexplorer.core.NormalDistribution
import com.indianservers.aiexplorer.core.ProbabilityDistribution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val P2Space = Color(0xFF020615)
private val P2Panel = Color(0xEE0A1632)
private val P2Ink = Color(0xFFF7F9FF)
private val P2Muted = Color(0xFFA9B8DA)
private val P2Cyan = Color(0xFF29DFFF)
private val P2Purple = Color(0xFFA969FF)
private val P2Green = Color(0xFF50E7A8)
private val P2Amber = Color(0xFFFFB83F)
private val P2Pink = Color(0xFFFF68A7)
private val P2Red = Color(0xFFFF637C)

internal enum class Phase2Route { Hub, Experiments, Rules, Combinatorics, Distributions, Normal, Sampling }
internal enum class Phase2ExperimentKind(val label: String) { Coin("Coin"), Dice("Dice"), Spinner("Spinner"), Cards("Cards"), Marbles("Marbles") }
internal enum class Phase2Population(val label: String) { Normal("Normal"), Uniform("Uniform"), RightSkewed("Right-skewed"), Bimodal("Bimodal") }
internal enum class VennRule(val label: String) { Union("A ∪ B"), Intersection("A ∩ B"), ComplementA("Aᶜ"), Conditional("P(A|B)"), Independence("Independent?") }

internal data class VennCalculation(
    val valid: Boolean,
    val error: String? = null,
    val onlyA: Double = 0.0,
    val onlyB: Double = 0.0,
    val intersection: Double = 0.0,
    val neither: Double = 0.0,
    val union: Double = 0.0,
    val conditionalAGivenB: Double? = null,
    val independent: Boolean = false,
)

internal object Phase2ProbabilityEngine {
    fun calculateVenn(a: Double, b: Double, intersection: Double): VennCalculation {
        if (a !in 0.0..1.0 || b !in 0.0..1.0 || intersection !in 0.0..1.0) {
            return VennCalculation(false, "Every probability must be between 0 and 1.")
        }
        if (intersection > min(a, b) + 1e-12) {
            return VennCalculation(false, "The intersection cannot exceed either event probability.")
        }
        val union = a + b - intersection
        if (union > 1.0 + 1e-12) {
            return VennCalculation(false, "P(A ∪ B) cannot exceed 1. Increase the intersection or reduce A/B.")
        }
        return VennCalculation(
            valid = true,
            onlyA = a - intersection,
            onlyB = b - intersection,
            intersection = intersection,
            neither = 1 - union,
            union = union,
            conditionalAGivenB = if (b > 0) intersection / b else null,
            independent = abs(intersection - a * b) < 1e-6,
        )
    }

    fun theoretical(kind: Phase2ExperimentKind): Map<String, Double> = when (kind) {
        Phase2ExperimentKind.Coin -> linkedMapOf("Heads" to .5, "Tails" to .5)
        Phase2ExperimentKind.Dice -> (1..6).associate { it.toString() to 1.0 / 6 }
        Phase2ExperimentKind.Spinner -> listOf("Blue", "Green", "Amber", "Pink").associateWith { .25 }
        Phase2ExperimentKind.Cards -> listOf("Hearts", "Diamonds", "Clubs", "Spades").associateWith { .25 }
        Phase2ExperimentKind.Marbles -> linkedMapOf("Blue" to .5, "Green" to .3, "Red" to .2)
    }

    fun nextOutcome(kind: Phase2ExperimentKind, random: Random): String = when (kind) {
        Phase2ExperimentKind.Coin -> if (random.nextBoolean()) "Heads" else "Tails"
        Phase2ExperimentKind.Dice -> (random.nextInt(6) + 1).toString()
        Phase2ExperimentKind.Spinner -> listOf("Blue", "Green", "Amber", "Pink")[random.nextInt(4)]
        Phase2ExperimentKind.Cards -> listOf("Hearts", "Diamonds", "Clubs", "Spades")[random.nextInt(4)]
        Phase2ExperimentKind.Marbles -> random.nextDouble().let { if (it < .5) "Blue" else if (it < .8) "Green" else "Red" }
    }
}

internal data class SamplingResult(
    val populationPreview: List<Double>,
    val latestSample: List<Double>,
    val sampleMeans: List<Double>,
    val populationMean: Double,
    val populationStandardDeviation: Double,
) {
    val meanOfMeans: Double get() = sampleMeans.average()
    val standardError: Double get() {
        val mean = sampleMeans.average()
        return sqrt(sampleMeans.sumOf { (it - mean).pow(2) } / sampleMeans.size.coerceAtLeast(1))
    }
}

internal object Phase2SamplingEngine {
    fun simulate(shape: Phase2Population, sampleSize: Int, repetitions: Int, seed: Long): SamplingResult {
        require(sampleSize in 1..1_000 && repetitions in 1..20_000)
        val populationRandom = Random(seed)
        val population = List(5_000) { drawPopulation(shape, populationRandom) }
        val random = Random(seed + 1)
        var latest = emptyList<Double>()
        val means = List(repetitions) {
            latest = List(sampleSize) { population[random.nextInt(population.size)] }
            latest.average()
        }
        val mean = population.average()
        val sd = sqrt(population.sumOf { (it - mean).pow(2) } / population.size)
        return SamplingResult(population.take(240), latest, means, mean, sd)
    }

    private fun drawPopulation(shape: Phase2Population, random: Random): Double = when (shape) {
        Phase2Population.Normal -> 50 + 12 * random.nextGaussian()
        Phase2Population.Uniform -> random.nextDouble() * 100
        Phase2Population.RightSkewed -> -18 * ln((1 - random.nextDouble()).coerceAtLeast(1e-12))
        Phase2Population.Bimodal -> if (random.nextBoolean()) 30 + 7 * random.nextGaussian() else 70 + 7 * random.nextGaussian()
    }
}

internal data class ExactCombinatoricsResult(
    val factorial: BigInteger,
    val permutations: BigInteger,
    val combinations: BigInteger,
    val orderedWithReplacement: BigInteger,
)

internal object Phase2CombinatoricsEngine {
    fun calculate(n: Int, r: Int): ExactCombinatoricsResult {
        require(n >= 0 && r in 0..n)
        val factorial = factorialBig(n)
        val permutations = factorial / factorialBig(n - r)
        return ExactCombinatoricsResult(
            factorial = factorial,
            permutations = permutations,
            combinations = permutations / factorialBig(r),
            orderedWithReplacement = n.toBigInteger().pow(r),
        )
    }
}

internal data class Phase2State(
    val route: Phase2Route = Phase2Route.Hub,
    val experimentKind: Phase2ExperimentKind = Phase2ExperimentKind.Coin,
    val experimentTarget: Int = 100,
    val experimentCompleted: Int = 0,
    val experimentCounts: Map<String, Int> = emptyMap(),
    val latestOutcome: String? = null,
    val convergence: List<Double> = emptyList(),
    val experimentRunning: Boolean = false,
    val experimentPaused: Boolean = false,
    val seed: Long = 42,
    val vennA: Double = .55,
    val vennB: Double = .45,
    val vennIntersection: Double = .2,
    val vennRule: VennRule = VennRule.Union,
    val combinationN: Int = 8,
    val combinationR: Int = 3,
    val distributionKind: DistributionKind = DistributionKind.Normal,
    val distributionFirst: Double = 0.0,
    val distributionSecond: Double = 1.0,
    val distributionLower: Double = -1.0,
    val distributionUpper: Double = 1.0,
    val normalMean: Double = 50.0,
    val normalSd: Double = 10.0,
    val normalLower: Double = 40.0,
    val normalUpper: Double = 60.0,
    val population: Phase2Population = Phase2Population.RightSkewed,
    val sampleSize: Int = 5,
    val sampleRepetitions: Int = 100,
    val samplingResult: SamplingResult? = null,
    val samplingRunning: Boolean = false,
)

internal class ProbabilityPhase2ViewModel : ViewModel() {
    private val _state = MutableStateFlow(Phase2State())
    val state: StateFlow<Phase2State> = _state.asStateFlow()
    private var experimentJob: Job? = null
    private var samplingJob: Job? = null

    fun open(route: Phase2Route) { _state.value = _state.value.copy(route = route) }
    fun home() { experimentJob?.cancel(); samplingJob?.cancel(); _state.value = _state.value.copy(route = Phase2Route.Hub, experimentRunning = false, samplingRunning = false) }
    fun experimentKind(kind: Phase2ExperimentKind) { resetExperiment(); _state.value = _state.value.copy(experimentKind = kind) }
    fun experimentTarget(target: Int) { _state.value = _state.value.copy(experimentTarget = target.coerceIn(1, 100_000)) }
    fun seed(seed: Long) { _state.value = _state.value.copy(seed = seed) }

    fun startExperiment(target: Int = _state.value.experimentTarget) {
        experimentJob?.cancel()
        val initial = _state.value.copy(
            experimentTarget = target.coerceIn(1, 100_000),
            experimentCompleted = 0,
            experimentCounts = emptyMap(),
            convergence = emptyList(),
            latestOutcome = null,
            experimentRunning = true,
            experimentPaused = false,
        )
        _state.value = initial
        experimentJob = viewModelScope.launch {
            val random = Random(initial.seed)
            val counts = linkedMapOf<String, Int>()
            val convergence = mutableListOf<Double>()
            val tracked = Phase2ProbabilityEngine.theoretical(initial.experimentKind).keys.first()
            for (trial in 1..initial.experimentTarget) {
                while (_state.value.experimentPaused) delay(16)
                val outcome = Phase2ProbabilityEngine.nextOutcome(initial.experimentKind, random)
                counts[outcome] = counts.getOrDefault(outcome, 0) + 1
                if (trial == 1 || trial % max(1, initial.experimentTarget / 120) == 0 || trial == initial.experimentTarget) {
                    convergence += counts.getOrDefault(tracked, 0).toDouble() / trial
                    _state.value = _state.value.copy(
                        experimentCompleted = trial,
                        experimentCounts = counts.toMap(),
                        latestOutcome = outcome,
                        convergence = convergence.toList(),
                    )
                    if (initial.experimentTarget > 500) delay(1)
                }
            }
            _state.value = _state.value.copy(experimentRunning = false, experimentPaused = false)
        }
    }

    fun togglePause() {
        if (_state.value.experimentRunning) _state.value = _state.value.copy(experimentPaused = !_state.value.experimentPaused)
    }

    fun resetExperiment() {
        experimentJob?.cancel()
        _state.value = _state.value.copy(experimentCompleted = 0, experimentCounts = emptyMap(), convergence = emptyList(), latestOutcome = null, experimentRunning = false, experimentPaused = false)
    }

    fun venn(a: Double? = null, b: Double? = null, intersection: Double? = null) {
        _state.value = _state.value.copy(vennA = a ?: _state.value.vennA, vennB = b ?: _state.value.vennB, vennIntersection = intersection ?: _state.value.vennIntersection)
    }
    fun vennRule(rule: VennRule) { _state.value = _state.value.copy(vennRule = rule) }
    fun combinatorics(n: Int? = null, r: Int? = null) {
        val nextN = (n ?: _state.value.combinationN).coerceIn(1, 20)
        val nextR = (r ?: _state.value.combinationR).coerceIn(0, nextN)
        _state.value = _state.value.copy(combinationN = nextN, combinationR = nextR)
    }

    fun distribution(kind: DistributionKind) {
        val defaults = when (kind) {
            DistributionKind.Normal -> listOf(0.0, 1.0, -1.0, 1.0)
            DistributionKind.Binomial -> listOf(12.0, .5, 3.0, 8.0)
            DistributionKind.Poisson -> listOf(4.0, 1.0, 1.0, 7.0)
            DistributionKind.Uniform -> listOf(0.0, 10.0, 2.0, 7.0)
            DistributionKind.Exponential -> listOf(1.0, 1.0, 0.0, 2.0)
        }
        _state.value = _state.value.copy(distributionKind = kind, distributionFirst = defaults[0], distributionSecond = defaults[1], distributionLower = defaults[2], distributionUpper = defaults[3])
    }
    fun distributionParameters(first: Double? = null, second: Double? = null, lower: Double? = null, upper: Double? = null) {
        _state.value = _state.value.copy(
            distributionFirst = first ?: _state.value.distributionFirst,
            distributionSecond = second ?: _state.value.distributionSecond,
            distributionLower = lower ?: _state.value.distributionLower,
            distributionUpper = upper ?: _state.value.distributionUpper,
        )
    }
    fun normal(mean: Double? = null, sd: Double? = null, lower: Double? = null, upper: Double? = null) {
        _state.value = _state.value.copy(
            normalMean = mean ?: _state.value.normalMean,
            normalSd = sd ?: _state.value.normalSd,
            normalLower = lower ?: _state.value.normalLower,
            normalUpper = upper ?: _state.value.normalUpper,
        )
    }
    fun normalPreset(sigmas: Int) {
        val mean = _state.value.normalMean
        val width = _state.value.normalSd * sigmas
        _state.value = _state.value.copy(normalLower = mean - width, normalUpper = mean + width)
    }
    fun samplingSettings(population: Phase2Population? = null, size: Int? = null, repetitions: Int? = null) {
        _state.value = _state.value.copy(
            population = population ?: _state.value.population,
            sampleSize = (size ?: _state.value.sampleSize).coerceIn(1, 500),
            sampleRepetitions = (repetitions ?: _state.value.sampleRepetitions).coerceIn(1, 10_000),
        )
    }
    fun runSampling() {
        samplingJob?.cancel()
        val state = _state.value
        _state.value = state.copy(samplingRunning = true)
        samplingJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                Phase2SamplingEngine.simulate(state.population, state.sampleSize, state.sampleRepetitions, state.seed)
            }
            _state.value = _state.value.copy(samplingResult = result, samplingRunning = false)
        }
    }
}

@Composable
internal fun ProbabilityStatisticsPhase2Screen(
    onBackToPhase1: () -> Unit,
    onOpenPhase3: () -> Unit,
    onExit: () -> Unit,
    model: ProbabilityPhase2ViewModel = viewModel(),
) {
    val state by model.state.collectAsState()
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF251B56), P2Space, Color(0xFF01030B)), center = Offset(260f, 80f), radius = 1100f),
        ),
    ) {
        when (state.route) {
            Phase2Route.Hub -> Phase2Hub(model, onBackToPhase1, onOpenPhase3, onExit)
            Phase2Route.Experiments -> ExperimentLab(state, model, onExit)
            Phase2Route.Rules -> ProbabilityRulesLab(state, model, onExit)
            Phase2Route.Combinatorics -> CombinatoricsPhase2Lab(state, model, onExit)
            Phase2Route.Distributions -> DistributionPhase2Lab(state, model, onExit)
            Phase2Route.Normal -> NormalPhase2Lab(state, model, onExit)
            Phase2Route.Sampling -> SamplingPhase2Lab(state, model, onExit)
        }
    }
}

@Composable
private fun Phase2Hub(model: ProbabilityPhase2ViewModel, onBack: () -> Unit, onOpenPhase3: () -> Unit, onExit: () -> Unit) {
    val tools = listOf(
        P2Tool("Probability Experiments", "Coin, dice, spinner, cards and marbles", "⚄", P2Cyan, Phase2Route.Experiments),
        P2Tool("Probability Rules", "Interactive Venn regions and conditional probability", "∪", P2Purple, Phase2Route.Rules),
        P2Tool("Combinatorics", "Permutations, combinations and replacement", "nCr", P2Green, Phase2Route.Combinatorics),
        P2Tool("Distribution Explorer", "Discrete and continuous probability models", "ƒ", P2Amber, Phase2Route.Distributions),
        P2Tool("Normal Distribution", "Z-scores, interval probability and presets", "μσ", P2Pink, Phase2Route.Normal),
        P2Tool("Sampling & CLT", "Population, repeated samples and standard error", "x̄", Color(0xFF71A7FF), Phase2Route.Sampling),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        P2TopBar("Probability & Statistics", "PHASE 2 · CHANCE & DISTRIBUTIONS", onBack, onExit)
        P2Panel(P2Purple) {
            Text("From random events to stable patterns", color = P2Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Run seeded experiments, manipulate probability regions, explore distributions and watch the Central Limit Theorem emerge.", color = P2Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                P2Badge("SEEDED", P2Cyan)
                P2Badge("10K TRIALS", P2Green)
                P2Badge("LIVE MODELS", P2Amber)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 720.dp) 3 else 2
            val available = maxWidth
            val gap = 8.dp
            val width = (available - gap * (columns - 1)) / columns
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                tools.forEach { tool -> P2HubCard(tool, Modifier.width(width)) { model.open(tool.route) } }
            }
        }
        P2Panel(P2Amber) {
            Text("GUIDED CHALLENGE", color = P2Amber, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("Does experimental probability converge?", color = P2Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Compare 10, 100 and 10,000 coin tosses using the same seed. Watch the convergence trace.", color = P2Muted, fontSize = 11.sp)
            P2PrimaryButton("Open experiment", P2Amber) { model.open(Phase2Route.Experiments) }
        }
        P2Panel(P2Purple) {
            Text("PHASE 3 AVAILABLE", color = P2Purple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("Analyze, infer and preserve", color = P2Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Explore live regression, confidence intervals, guided hypothesis tests, saved workspaces and safe commands.", color = P2Muted, fontSize = 11.sp)
            P2PrimaryButton("Open Phase 3", P2Purple, onOpenPhase3)
        }
    }
}

private data class P2Tool(val title: String, val description: String, val icon: String, val accent: Color, val route: Phase2Route)

@Composable
private fun P2HubCard(tool: P2Tool, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.heightIn(min = 138.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(tool.accent.copy(.18f), P2Panel)))
            .border(1.dp, tool.accent.copy(.58f), RoundedCornerShape(20.dp)).clickable(onClick = onClick)
            .focusable().padding(12.dp).semantics { contentDescription = "Open ${tool.title}. ${tool.description}" },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tool.accent.copy(.17f)), contentAlignment = Alignment.Center) {
                Text(tool.icon, color = tool.accent, fontWeight = FontWeight.Black)
            }
            Text("OPEN ›", color = tool.accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
        Text(tool.title, color = P2Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text(tool.description, color = P2Muted, fontSize = 9.sp, maxLines = 3)
    }
}

@Composable
private fun ExperimentLab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    P2Workspace(
        title = "Probability Experiments",
        subtitle = "SEE CONVERGENCE IN REAL TIME",
        onBack = model::home,
        onExit = onExit,
        controls = {
            P2ChoiceFlow(
                Phase2ExperimentKind.entries,
                state.experimentKind,
                { it.label },
                P2Cyan,
                model::experimentKind,
            )
            Text("Trials: ${state.experimentTarget}", color = P2Ink, fontWeight = FontWeight.Bold)
            Slider(state.experimentTarget.toFloat(), { model.experimentTarget(it.roundToInt()) }, valueRange = 1f..10_000f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 10, 100, 1_000, 10_000).forEach { count ->
                    P2SmallButton(if (count >= 1000) "${count / 1000}K" else "$count", P2Purple) { model.startExperiment(count) }
                }
            }
            var seedText by remember(state.seed) { mutableStateOf(state.seed.toString()) }
            OutlinedTextField(
                value = seedText,
                onValueChange = {
                    seedText = it.filter(Char::isDigit).take(12)
                    it.toLongOrNull()?.let(model::seed)
                },
                label = { Text("Replay seed") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                P2SmallButton(if (state.experimentPaused) "Resume" else "Pause", P2Amber, state.experimentRunning, model::togglePause)
                P2SmallButton("Reset", P2Red, onClick = model::resetExperiment)
            }
        },
        visual = {
            val theoretical = Phase2ProbabilityEngine.theoretical(state.experimentKind)
            Text(state.latestOutcome ?: "Ready", color = P2Cyan, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("${state.experimentCompleted} / ${state.experimentTarget} trials", color = P2Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            ExperimentBars(state.experimentCounts, theoretical, state.experimentCompleted)
            theoretical.forEach { (label, expected) ->
                val count = state.experimentCounts[label] ?: 0
                val observed = if (state.experimentCompleted == 0) 0.0 else count.toDouble() / state.experimentCompleted
                P2Insight(label, "$count · observed ${p2Trim(observed)} · theoretical ${p2Trim(expected)}", if (abs(observed - expected) < .03 && state.experimentCompleted >= 100) P2Green else P2Cyan)
            }
            ConvergencePlot(state.convergence, theoretical.values.first())
            if (state.experimentRunning) P2Badge(if (state.experimentPaused) "PAUSED" else "RUNNING", if (state.experimentPaused) P2Amber else P2Green)
        },
    )
}

@Composable
private fun ProbabilityRulesLab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    val result = Phase2ProbabilityEngine.calculateVenn(state.vennA, state.vennB, state.vennIntersection)
    P2Workspace(
        title = "Probability Rules",
        subtitle = "TWO-SET VENN LAB",
        onBack = model::home,
        onExit = onExit,
        controls = {
            P2ProbabilitySlider("P(A)", state.vennA) { model.venn(a = it) }
            P2ProbabilitySlider("P(B)", state.vennB) { model.venn(b = it) }
            P2ProbabilitySlider("P(A ∩ B)", state.vennIntersection) { model.venn(intersection = it) }
            P2ChoiceFlow(VennRule.entries, state.vennRule, { it.label }, P2Purple, model::vennRule)
            if (!result.valid) Text(result.error.orEmpty(), color = P2Red, fontSize = 11.sp)
            else {
                val formula = when (state.vennRule) {
                    VennRule.Union -> "P(A∪B) = P(A)+P(B)−P(A∩B)"
                    VennRule.Intersection -> "P(A∩B) = ${p2Trim(result.intersection)}"
                    VennRule.ComplementA -> "P(Aᶜ) = 1−P(A)"
                    VennRule.Conditional -> "P(A|B) = P(A∩B)/P(B)"
                    VennRule.Independence -> "Independent iff P(A∩B)=P(A)P(B)"
                }
                Text(formula, color = P2Cyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        visual = {
            VennCanvas(state.vennRule, result)
            if (result.valid) {
                P2Insight("A only", p2Trim(result.onlyA), P2Cyan)
                P2Insight("A ∩ B", p2Trim(result.intersection), P2Purple)
                P2Insight("B only", p2Trim(result.onlyB), P2Green)
                P2Insight("Neither", p2Trim(result.neither), P2Muted)
                P2Insight("Union", p2Trim(result.union), P2Amber)
                P2Insight("P(A|B)", result.conditionalAGivenB?.let(::p2Trim) ?: "undefined", P2Pink)
                P2Insight("Independence", if (result.independent) "Yes" else "No", if (result.independent) P2Green else P2Amber)
            }
        },
    )
}

@Composable
private fun CombinatoricsPhase2Lab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    val result = Phase2CombinatoricsEngine.calculate(state.combinationN, state.combinationR)
    P2Workspace(
        title = "Combinatorics",
        subtitle = "ORDERED VS UNORDERED SELECTION",
        onBack = model::home,
        onExit = onExit,
        controls = {
            Text("Objects n = ${state.combinationN}", color = P2Ink, fontWeight = FontWeight.Bold)
            Slider(state.combinationN.toFloat(), { model.combinatorics(n = it.roundToInt()) }, valueRange = 1f..20f, steps = 18)
            Text("Choose r = ${state.combinationR}", color = P2Ink, fontWeight = FontWeight.Bold)
            Slider(state.combinationR.toFloat(), { model.combinatorics(r = it.roundToInt()) }, valueRange = 0f..state.combinationN.toFloat(), steps = (state.combinationN - 1).coerceAtLeast(0))
            Text("nPr counts arrangements where order matters. nCr groups arrangements containing the same objects.", color = P2Muted, fontSize = 11.sp)
        },
        visual = {
            ObjectSelectionVisual(state.combinationN, state.combinationR)
            P2Insight("n!", result.factorial.toString(), P2Cyan)
            P2Insight("Permutations nPr", result.permutations.toString(), P2Purple)
            P2Insight("Combinations nCr", result.combinations.toString(), P2Green)
            P2Insight("Ordered with replacement nʳ", result.orderedWithReplacement.toString(), P2Amber)
            Text("${state.combinationN}P${state.combinationR} = ${state.combinationN}! / (${state.combinationN}−${state.combinationR})!", color = P2Purple, fontFamily = FontFamily.Monospace)
            Text("${state.combinationN}C${state.combinationR} = ${state.combinationN}P${state.combinationR} / ${state.combinationR}!", color = P2Green, fontFamily = FontFamily.Monospace)
        },
    )
}

@Composable
private fun DistributionPhase2Lab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    val distribution = remember(state.distributionKind, state.distributionFirst, state.distributionSecond) {
        runCatching { DistributionEngine.create(state.distributionKind, state.distributionFirst, state.distributionSecond) }.getOrNull()
    }
    P2Workspace(
        title = "Distribution Explorer",
        subtitle = "PMF · PDF · CDF · QUANTILES",
        onBack = model::home,
        onExit = onExit,
        controls = {
            P2ChoiceFlow(DistributionKind.entries, state.distributionKind, { it.name }, P2Amber, model::distribution)
            DistributionControls(state, model)
            P2RangeSlider("Lower", state.distributionLower, distributionRange(state).first) { model.distributionParameters(lower = min(it, state.distributionUpper)) }
            P2RangeSlider("Upper", state.distributionUpper, distributionRange(state).second) { model.distributionParameters(upper = max(it, state.distributionLower)) }
        },
        visual = {
            if (distribution == null) {
                P2Empty("Choose valid parameters.")
            } else {
                DistributionCanvas(distribution, state.distributionLower, state.distributionUpper)
                val probability = runCatching { distribution.probabilityBetween(min(state.distributionLower, state.distributionUpper), max(state.distributionLower, state.distributionUpper)) }.getOrNull()
                P2Insight("Interval probability", probability?.let(::p2Trim) ?: "invalid", P2Green)
                P2Insight("Expected value", p2Trim(distribution.summary.mean), P2Cyan)
                P2Insight("Variance", p2Trim(distribution.summary.variance), P2Purple)
                P2Insight("Standard deviation", p2Trim(distribution.summary.standardDeviation), P2Amber)
                P2Insight("CDF at upper", p2Trim(distribution.cumulative(state.distributionUpper)), P2Cyan)
                P2Insight("90th percentile", p2Trim(distribution.quantile(.9)), P2Pink)
            }
        },
    )
}

@Composable
private fun NormalPhase2Lab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    val normal = remember(state.normalMean, state.normalSd) { NormalDistribution(state.normalMean, state.normalSd.coerceAtLeast(.01)) }
    val probability = normal.probabilityBetween(min(state.normalLower, state.normalUpper), max(state.normalLower, state.normalUpper))
    P2Workspace(
        title = "Normal Distribution",
        subtitle = "RAW VALUES · Z-SCORES · SHADED AREA",
        onBack = model::home,
        onExit = onExit,
        controls = {
            P2RangeSlider("Mean μ", state.normalMean, -50f..150f) { model.normal(mean = it) }
            P2RangeSlider("Standard deviation σ", state.normalSd, .5f..50f) { model.normal(sd = it) }
            P2RangeSlider("Lower value", state.normalLower, (state.normalMean - 4 * state.normalSd).toFloat()..(state.normalMean + 4 * state.normalSd).toFloat()) { model.normal(lower = min(it, state.normalUpper)) }
            P2RangeSlider("Upper value", state.normalUpper, (state.normalMean - 4 * state.normalSd).toFloat()..(state.normalMean + 4 * state.normalSd).toFloat()) { model.normal(upper = max(it, state.normalLower)) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..3).forEach { sigma -> P2SmallButton("±${sigma}σ", P2Purple) { model.normalPreset(sigma) } }
            }
        },
        visual = {
            DistributionCanvas(normal, state.normalLower, state.normalUpper)
            Text("P(${p2Trim(state.normalLower)} ≤ X ≤ ${p2Trim(state.normalUpper)}) = ${p2Trim(probability)}", color = P2Green, fontSize = 18.sp, fontWeight = FontWeight.Black)
            P2Insight("Lower z-score", p2Trim((state.normalLower - state.normalMean) / state.normalSd), P2Cyan)
            P2Insight("Upper z-score", p2Trim((state.normalUpper - state.normalMean) / state.normalSd), P2Purple)
            P2Insight("Lower tail", p2Trim(normal.cumulative(state.normalLower)), P2Amber)
            P2Insight("Upper tail", p2Trim(1 - normal.cumulative(state.normalUpper)), P2Pink)
            Text("Changing μ shifts the curve. Changing σ alters spread while total area remains 1.", color = P2Muted, fontSize = 11.sp)
        },
    )
}

@Composable
private fun SamplingPhase2Lab(state: Phase2State, model: ProbabilityPhase2ViewModel, onExit: () -> Unit) {
    P2Workspace(
        title = "Sampling & CLT",
        subtitle = "POPULATION → SAMPLE → SAMPLING DISTRIBUTION",
        onBack = model::home,
        onExit = onExit,
        controls = {
            P2ChoiceFlow(Phase2Population.entries, state.population, { it.label }, P2Cyan, { model.samplingSettings(population = it) })
            Text("Sample size n = ${state.sampleSize}", color = P2Ink, fontWeight = FontWeight.Bold)
            Slider(state.sampleSize.toFloat(), { model.samplingSettings(size = it.roundToInt()) }, valueRange = 1f..100f, steps = 98)
            Text("Repeated samples = ${state.sampleRepetitions}", color = P2Ink, fontWeight = FontWeight.Bold)
            Slider(state.sampleRepetitions.toFloat(), { model.samplingSettings(repetitions = it.roundToInt()) }, valueRange = 10f..1_000f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 100, 1_000).forEach { count -> P2SmallButton("$count", P2Purple) { model.samplingSettings(repetitions = count); model.runSampling() } }
            }
            P2PrimaryButton(if (state.samplingRunning) "Sampling…" else "Run simulation", P2Green) { if (!state.samplingRunning) model.runSampling() }
        },
        visual = {
            val result = state.samplingResult
            if (result == null) {
                P2Empty("Choose a population and run repeated sampling.")
            } else {
                Text("1 · POPULATION", color = P2Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                MiniHistogram(result.populationPreview, P2Cyan, "Population ${state.population.label}")
                Text("2 · LATEST SAMPLE", color = P2Purple, fontSize = 9.sp, fontWeight = FontWeight.Black)
                DotStrip(result.latestSample, P2Purple)
                Text("3 · SAMPLING DISTRIBUTION OF x̄", color = P2Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                MiniHistogram(result.sampleMeans, P2Green, "Sampling distribution with ${result.sampleMeans.size} means")
                P2Insight("Population mean", p2Trim(result.populationMean), P2Cyan)
                P2Insight("Mean of sample means", p2Trim(result.meanOfMeans), P2Green)
                P2Insight("Observed standard error", p2Trim(result.standardError), P2Purple)
                P2Insight("Theoretical σ/√n", p2Trim(result.populationStandardDeviation / sqrt(state.sampleSize.toDouble())), P2Amber)
                Text(
                    if (state.sampleSize >= 25) "With larger n, the distribution of sample means becomes tighter and more nearly normal—even when the population is not normal."
                    else "Increase sample size to see the sampling distribution tighten and approach a normal shape.",
                    color = P2Muted,
                    fontSize = 11.sp,
                )
            }
        },
    )
}

@Composable
private fun P2Workspace(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onExit: () -> Unit,
    controls: @Composable () -> Unit,
    visual: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            P2TopBar(title, subtitle, onBack, onExit)
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                    P2Panel(P2Cyan, Modifier.weight(.38f), controls)
                    P2Panel(P2Purple, Modifier.weight(.62f), visual)
                }
            } else {
                P2Panel(P2Purple, content = visual)
                P2Panel(P2Cyan, content = controls)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun P2TopBar(title: String, subtitle: String, onBack: () -> Unit, onExit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(P2Cyan.copy(.13f))
                .border(1.dp, P2Cyan.copy(.55f), RoundedCornerShape(13.dp)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = P2Cyan, fontSize = 27.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, color = P2Ink, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = P2Cyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
        }
        Text("MATH HOME", color = P2Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(11.dp)).border(1.dp, P2Muted.copy(.3f), RoundedCornerShape(11.dp)).clickable(onClick = onExit).padding(9.dp))
    }
}

@Composable
private fun P2Panel(accent: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(Brush.linearGradient(listOf(accent.copy(.08f), P2Panel)))
            .border(1.dp, accent.copy(.4f), RoundedCornerShape(21.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) { content() }
}

@Composable
private fun P2Badge(label: String, accent: Color) {
    Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(CircleShape).background(accent.copy(.12f)).border(1.dp, accent.copy(.38f), CircleShape).padding(horizontal = 8.dp, vertical = 5.dp))
}

@Composable
private fun P2PrimaryButton(label: String, accent: Color, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp)).background(accent).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = P2Space, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun P2SmallButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.height(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(if (enabled) .14f else .04f))
            .border(1.dp, accent.copy(if (enabled) .45f else .13f), RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) accent else P2Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun <T> P2ChoiceFlow(choices: List<T>, selected: T, label: (T) -> String, accent: Color, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { choice -> P2SmallButton(if (choice == selected) "● ${label(choice)}" else label(choice), if (choice == selected) accent else P2Muted) { onSelect(choice) } }
    }
}

@Composable
private fun P2ProbabilitySlider(label: String, value: Double, onChange: (Double) -> Unit) {
    Text("$label = ${p2Trim(value)}", color = P2Ink, fontWeight = FontWeight.Bold)
    Slider(value.toFloat(), { onChange(it.toDouble()) }, valueRange = 0f..1f)
}

@Composable
private fun P2RangeSlider(label: String, value: Double, range: ClosedFloatingPointRange<Float>, onChange: (Double) -> Unit) {
    Text("$label = ${p2Trim(value)}", color = P2Ink, fontWeight = FontWeight.Bold)
    Slider(value.toFloat().coerceIn(range.start, range.endInclusive), { onChange(it.toDouble()) }, valueRange = range)
}

@Composable
private fun P2Insight(label: String, value: String, accent: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.035f)).padding(9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = P2Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun P2Empty(message: String) {
    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(17.dp)).background(Color.Black.copy(.2f)).border(1.dp, P2Amber.copy(.35f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
        Text(message, color = P2Amber, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun ExperimentBars(counts: Map<String, Int>, theoretical: Map<String, Double>, total: Int) {
    val labels = theoretical.keys.toList()
    Canvas(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(17.dp)).background(Color.Black.copy(.2f)).semantics { contentDescription = "Experimental outcome chart with $total trials" }) {
        if (labels.isEmpty()) return@Canvas
        val width = size.width / labels.size
        labels.forEachIndexed { index, label ->
            val observed = if (total == 0) 0f else (counts[label] ?: 0).toFloat() / total
            val expected = theoretical[label]?.toFloat() ?: 0f
            val barHeight = observed * size.height * .82f
            val x = index * width
            drawRect(if (index % 2 == 0) P2Cyan.copy(.6f) else P2Purple.copy(.6f), Offset(x + 5, size.height - barHeight), Size((width - 10).coerceAtLeast(2f), barHeight))
            val expectedY = size.height - expected * size.height * .82f
            drawLine(P2Amber, Offset(x + 3, expectedY), Offset(x + width - 3, expectedY), 3f)
        }
    }
}

@Composable
private fun ConvergencePlot(values: List<Double>, theoretical: Double) {
    Canvas(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(.18f)).semantics { contentDescription = "Convergence plot toward theoretical probability ${p2Trim(theoretical)}" }) {
        val expectedY = size.height * (1 - theoretical.toFloat())
        drawLine(P2Amber.copy(.8f), Offset(0f, expectedY), Offset(size.width, expectedY), 2f)
        if (values.size >= 2) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val point = Offset(size.width * index / (values.size - 1), size.height * (1 - value.toFloat()))
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, P2Cyan, style = Stroke(3f))
        }
    }
}

@Composable
private fun VennCanvas(rule: VennRule, result: VennCalculation) {
    Canvas(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.2f)).semantics { contentDescription = "Two-set Venn diagram showing ${rule.label}" }) {
        val radius = min(size.width, size.height) * .27f
        val a = Offset(size.width * .42f, size.height * .5f)
        val b = Offset(size.width * .58f, size.height * .5f)
        val aFill = when (rule) { VennRule.Union, VennRule.Conditional -> P2Cyan.copy(.24f); VennRule.ComplementA -> Color.Transparent; else -> P2Cyan.copy(.08f) }
        val bFill = when (rule) { VennRule.Union, VennRule.Conditional -> P2Purple.copy(.24f); else -> P2Purple.copy(.08f) }
        if (rule == VennRule.ComplementA) drawRect(P2Amber.copy(.1f), Offset.Zero, size)
        drawCircle(aFill, radius, a)
        drawCircle(bFill, radius, b)
        drawCircle(P2Cyan, radius, a, style = Stroke(3f))
        drawCircle(P2Purple, radius, b, style = Stroke(3f))
        if (rule == VennRule.Intersection || rule == VennRule.Conditional || rule == VennRule.Independence) {
            drawCircle(P2Pink.copy(.16f), radius * .42f, Offset(size.width * .5f, size.height * .5f))
        }
        if (!result.valid) drawRect(P2Red.copy(.12f), Offset.Zero, size)
    }
}

@Composable
private fun ObjectSelectionVisual(n: Int, r: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("OBJECTS", color = P2Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(n) { index ->
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (index < r) P2Green.copy(.28f) else P2Cyan.copy(.1f)).border(1.dp, if (index < r) P2Green else P2Cyan.copy(.4f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(('A'.code + index).toChar().toString(), color = P2Ink, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("ORDERED SLOTS", color = P2Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(r) { index ->
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(P2Purple.copy(.14f)).border(1.dp, P2Purple.copy(.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text((index + 1).toString(), color = P2Purple, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DistributionControls(state: Phase2State, model: ProbabilityPhase2ViewModel) {
    when (state.distributionKind) {
        DistributionKind.Normal -> {
            P2RangeSlider("Mean μ", state.distributionFirst, -10f..10f) { model.distributionParameters(first = it) }
            P2RangeSlider("Std dev σ", state.distributionSecond, .1f..8f) { model.distributionParameters(second = it) }
        }
        DistributionKind.Binomial -> {
            P2RangeSlider("Trials n", state.distributionFirst, 1f..60f) { model.distributionParameters(first = it.roundToInt().toDouble()) }
            P2RangeSlider("Success p", state.distributionSecond, 0f..1f) { model.distributionParameters(second = it) }
        }
        DistributionKind.Poisson -> P2RangeSlider("Rate λ", state.distributionFirst, .1f..30f) { model.distributionParameters(first = it) }
        DistributionKind.Uniform -> {
            P2RangeSlider("Minimum a", state.distributionFirst, -10f..9f) { model.distributionParameters(first = min(it, state.distributionSecond - .1)) }
            P2RangeSlider("Maximum b", state.distributionSecond, -9f..10f) { model.distributionParameters(second = max(it, state.distributionFirst + .1)) }
        }
        DistributionKind.Exponential -> P2RangeSlider("Rate λ", state.distributionFirst, .1f..10f) { model.distributionParameters(first = it) }
    }
}

private fun distributionRange(state: Phase2State): Pair<ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>> = when (state.distributionKind) {
    DistributionKind.Normal -> (-20f..20f) to (-20f..20f)
    DistributionKind.Binomial -> (0f..state.distributionFirst.toFloat()) to (0f..state.distributionFirst.toFloat())
    DistributionKind.Poisson -> (0f..30f) to (0f..30f)
    DistributionKind.Uniform -> (state.distributionFirst.toFloat()..state.distributionSecond.toFloat()) to (state.distributionFirst.toFloat()..state.distributionSecond.toFloat())
    DistributionKind.Exponential -> (0f..10f) to (0f..10f)
}

@Composable
private fun DistributionCanvas(distribution: ProbabilityDistribution, lower: Double, upper: Double) {
    val points = remember(distribution) { distribution.plotPoints().take(500) }
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    Canvas(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.2f))
            .pointerInput(points) {
                detectTapGestures { tap ->
                    if (points.isNotEmpty()) selectedIndex = (tap.x / size.width * points.size).toInt().coerceIn(0, points.lastIndex)
                }
            }
            .semantics { contentDescription = "${distribution.summary.kind} ${distribution.summary.domain} distribution plot with shaded interval ${p2Trim(lower)} to ${p2Trim(upper)}" },
    ) {
        if (points.isEmpty()) return@Canvas
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }.takeIf { it > minX } ?: minX + 1
        val maxY = points.maxOf { it.probability }.coerceAtLeast(1e-9)
        fun x(value: Double) = ((value - minX) / (maxX - minX) * size.width).toFloat()
        fun y(value: Double) = size.height - (value / maxY * size.height * .88).toFloat()
        if (distribution.summary.domain == DistributionDomain.Discrete) {
            val width = (size.width / points.size * .75f).coerceAtLeast(2f)
            points.forEachIndexed { index, point ->
                val color = if (point.x in min(lower, upper)..max(lower, upper)) P2Green else P2Cyan
                drawRect(if (index == selectedIndex) P2Amber else color.copy(.65f), Offset(x(point.x) - width / 2, y(point.probability)), Size(width, size.height - y(point.probability)))
            }
        } else {
            val area = Path()
            points.forEachIndexed { index, point ->
                if (point.x in min(lower, upper)..max(lower, upper)) {
                    if (area.isEmpty) area.moveTo(x(point.x), size.height) else Unit
                    area.lineTo(x(point.x), y(point.probability))
                }
            }
            if (!area.isEmpty) { area.lineTo(x(max(lower, upper)), size.height); area.close(); drawPath(area, P2Green.copy(.25f)) }
            val curve = Path()
            points.forEachIndexed { index, point -> if (index == 0) curve.moveTo(x(point.x), y(point.probability)) else curve.lineTo(x(point.x), y(point.probability)) }
            drawPath(curve, P2Cyan, style = Stroke(3f))
            drawLine(P2Amber, Offset(x(lower), 0f), Offset(x(lower), size.height), 2f)
            drawLine(P2Pink, Offset(x(upper), 0f), Offset(x(upper), size.height), 2f)
        }
    }
    points.getOrNull(selectedIndex)?.let { Text("x=${p2Trim(it.x)} · probability/density=${p2Trim(it.probability)}", color = P2Amber, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
}

@Composable
private fun MiniHistogram(values: List<Double>, accent: Color, description: String) {
    Canvas(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(.18f)).semantics { contentDescription = description }) {
        if (values.isEmpty()) return@Canvas
        val minimum = values.min()
        val maximum = values.max().takeIf { it > minimum } ?: minimum + 1
        val bins = 18
        val counts = IntArray(bins)
        values.forEach { counts[((it - minimum) / (maximum - minimum) * bins).toInt().coerceIn(0, bins - 1)]++ }
        val highest = counts.max().coerceAtLeast(1)
        val width = size.width / bins
        counts.forEachIndexed { index, count ->
            val height = size.height * count / highest
            drawRect(accent.copy(.65f), Offset(index * width + 1, size.height - height), Size((width - 2).coerceAtLeast(1f), height))
        }
    }
}

@Composable
private fun DotStrip(values: List<Double>, accent: Color) {
    Canvas(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(.16f)).semantics { contentDescription = "Latest sample with ${values.size} observations" }) {
        if (values.isEmpty()) return@Canvas
        val minimum = values.min()
        val maximum = values.max().takeIf { it > minimum } ?: minimum + 1
        values.forEachIndexed { index, value ->
            drawCircle(accent, 5f, Offset(((value - minimum) / (maximum - minimum) * size.width).toFloat(), size.height * (.3f + (index % 3) * .2f)))
        }
    }
}

private fun factorialBig(n: Int): BigInteger = (2..n).fold(BigInteger.ONE) { total, value -> total * value.toBigInteger() }

private fun p2Trim(value: Double): String {
    if (!value.isFinite()) return "n/a"
    val rounded = kotlin.math.round(value * 10_000.0) / 10_000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
