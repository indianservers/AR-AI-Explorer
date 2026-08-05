@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.indianservers.aiexplorer.features.probabilitystatistics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.BinomialSimulation
import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.ProbabilityStatisticsEngine
import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.StatisticalTestGuide
import com.indianservers.aiexplorer.features.probabilitystatistics.data.ProbabilityStatisticsCatalog
import com.indianservers.aiexplorer.features.probabilitystatistics.models.AnalysisObjective
import com.indianservers.aiexplorer.features.probabilitystatistics.models.GroupStructure
import com.indianservers.aiexplorer.features.probabilitystatistics.models.OutcomeType
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsTopic
import com.indianservers.aiexplorer.features.probabilitystatistics.models.TopicWorkspaceMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun StatisticsTopicScreen(state: ProbabilityStatisticsUiState, model: ProbabilityStatisticsViewModel) {
    val topic = state.topicId?.let(ProbabilityStatisticsCatalog::topic) ?: return
    val progress = state.progress[topic.id]
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar(topic.title, "${topic.difficulty.name} · ${topic.estimatedMinutes} min · ${progress?.percent ?: 0}% complete", model::back) }
        item { TopicModeStrip(state.topicMode, model::selectMode) }
        item {
            FeaturePanel(modeColor(state.topicMode), Modifier.fillMaxWidth()) {
                TopicModeContent(topic, state.topicMode, model)
            }
        }
        if (state.topicMode != TopicWorkspaceMode.Assessment) {
            item {
                val next = ProbabilityStatisticsCatalog.nextTopics(topic.id)
                if (next.isNotEmpty()) {
                    SectionTitle("Recommended next")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        next.forEach { item ->
                            PrimaryAction(item.title, categoryColor(item.categoryId)) { model.openTopic(item.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicModeStrip(selected: TopicWorkspaceMode, onSelect: (TopicWorkspaceMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TopicWorkspaceMode.entries.forEach { mode ->
            PrimaryAction(if (selected == mode) "• ${mode.label}" else mode.label, if (selected == mode) modeColor(mode) else PsMuted) {
                onSelect(mode)
            }
        }
    }
}

@Composable
private fun TopicModeContent(topic: StatisticsTopic, mode: TopicWorkspaceMode, model: ProbabilityStatisticsViewModel) {
    when (mode) {
        TopicWorkspaceMode.Overview -> {
            Text(topic.shortDescription, color = PsInk, fontSize = 16.sp)
            LabelValue("Why it matters", topic.applications.joinToString())
            LabelValue("Prerequisites", topic.prerequisites.mapNotNull { ProbabilityStatisticsCatalog.topic(it)?.title }.ifEmpty { listOf("None") }.joinToString())
            LabelValue("You will learn", topic.learningOutcomes.joinToString(" · "))
        }
        TopicWorkspaceMode.Learn -> {
            Text("Guided lesson", color = PsCyan, fontWeight = FontWeight.Bold)
            topic.lessonSteps.forEachIndexed { index, step -> LessonStep(index + 1, step) }
            Text("Common mistake", color = PsAmber, fontWeight = FontWeight.Bold)
            Text(commonMistake(topic.id), color = PsMuted, fontSize = 13.sp)
        }
        TopicWorkspaceMode.Visualize, TopicWorkspaceMode.Explore, TopicWorkspaceMode.Simulate -> {
            if (topic.id == "binomial-distribution") {
                BinomialInteractiveLab(mode)
            } else {
                GenericTopicExplorer(topic, mode)
            }
        }
        TopicWorkspaceMode.Formula -> {
            topic.formulas.forEach { formula ->
                Text(formula.expression, color = PsCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = formula.spoken })
                Text(formula.spoken, color = PsMuted, fontSize = 13.sp)
                formula.terms.forEach { (term, meaning) -> LabelValue(term, meaning) }
            }
        }
        TopicWorkspaceMode.Examples -> {
            topic.examples.forEach { example ->
                Text(example.question, color = PsInk, fontWeight = FontWeight.Bold)
                example.steps.forEachIndexed { index, step -> LessonStep(index + 1, step) }
                LabelValue("Answer", example.answer)
            }
        }
        TopicWorkspaceMode.Practice, TopicWorkspaceMode.Assessment -> {
            PracticeBlock(topic, model, assessment = mode == TopicWorkspaceMode.Assessment)
        }
        TopicWorkspaceMode.Applications -> {
            topic.applications.forEach { application ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(5.dp).height(30.dp).background(PsGreen, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(9.dp))
                    Text(application, color = PsInk, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Ask what the variables mean, how observations were collected, and what decisions the analysis can honestly support.", color = PsMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GenericTopicExplorer(topic: StatisticsTopic, mode: TopicWorkspaceMode) {
    var sampleSize by rememberSaveable(topic.id) { mutableIntStateOf(20) }
    val values = remember(topic.id, sampleSize) {
        val base = topic.id.hashCode().toLong()
        List(sampleSize) { index -> 50 + 12 * kotlin.math.sin(index * .71 + base.mod(19)) + (index % 4) * 1.7 }
    }
    val summary = ProbabilityStatisticsEngine.descriptive(values).getOrNull()
    Text(
        when (mode) {
            TopicWorkspaceMode.Simulate -> "Deterministic sample experiment"
            TopicWorkspaceMode.Explore -> "Adjust the sample and inspect the summary"
            else -> "Live data shape"
        },
        color = PsCyan,
        fontWeight = FontWeight.Bold,
    )
    Slider(value = sampleSize.toFloat(), onValueChange = { sampleSize = it.roundToInt() }, valueRange = 5f..80f, steps = 14)
    Text("Sample size: $sampleSize", color = PsMuted)
    MiniDataBars(values, PsViolet, "Interactive sample visualization with $sampleSize observations")
    summary?.let {
        MetricGrid(listOf("Mean" to f(it.mean), "Median" to f(it.median), "Std dev" to f(it.populationStandardDeviation), "IQR" to f(it.interquartileRange)))
    }
    Text("Change the sample size. Watch which summaries remain stable and which react to the generated observations.", color = PsMuted, fontSize = 13.sp)
}

@Composable
private fun BinomialInteractiveLab(mode: TopicWorkspaceMode) {
    var n by rememberSaveable { mutableIntStateOf(10) }
    var p by rememberSaveable { mutableStateOf(.5) }
    var x by rememberSaveable { mutableIntStateOf(5) }
    var seed by rememberSaveable { mutableLongStateOf(42L) }
    var simulation by remember { mutableStateOf<BinomialSimulation?>(null) }
    val result = remember(n, p, x) { ProbabilityStatisticsEngine.binomial(n, p, x).getOrNull() }
    Text("Binomial controls", color = PsCyan, fontWeight = FontWeight.Bold)
    Slider(value = n.toFloat(), onValueChange = { n = it.roundToInt(); x = x.coerceAtMost(n) }, valueRange = 1f..50f, steps = 48)
    Text("Trials n = $n", color = PsMuted)
    Slider(value = p.toFloat(), onValueChange = { p = it.toDouble() }, valueRange = 0f..1f, steps = 19)
    Text("Success probability p = ${f(p)}", color = PsMuted)
    Slider(value = x.toFloat(), onValueChange = { x = it.roundToInt() }, valueRange = 0f..n.toFloat(), steps = max(0, n - 1))
    Text("Target successes x = $x", color = PsMuted)
    BinomialBars(n, p, x) { x = it }
    result?.let {
        MetricGrid(
            listOf(
                "P(X = x)" to f(it.pmf),
                "P(X ≤ x)" to f(it.cdf),
                "Mean" to f(it.mean),
                "Variance" to f(it.variance),
                "Std dev" to f(it.standardDeviation),
                "Mode" to it.mode.toString(),
            ),
        )
    }
    if (mode == TopicWorkspaceMode.Simulate) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction("Run 1,000", PsGreen) {
                simulation = ProbabilityStatisticsEngine.simulateBinomial(n, p, x, 1_000, seed).getOrNull()
                seed++
            }
            PrimaryAction("Reset", PsAmber) { simulation = null; seed = 42 }
        }
        simulation?.let {
            LabelValue("Empirical P(X=$x)", f(it.empiricalProbability))
            LabelValue("Theoretical P(X=$x)", f(it.theoreticalProbability))
            LabelValue("Absolute error", f(abs(it.empiricalProbability - it.theoreticalProbability)))
        }
    } else {
        Text("Tap a bar or move the target slider. The PMF, cumulative probability and moments update together.", color = PsMuted, fontSize = 13.sp)
    }
}

@Composable
private fun BinomialBars(n: Int, p: Double, selected: Int, onSelect: (Int) -> Unit) {
    val probabilities = remember(n, p) { (0..n).map { ProbabilityStatisticsEngine.binomialPmf(n, p, it) } }
    val highest = probabilities.maxOrNull()?.coerceAtLeast(1e-12) ?: 1.0
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(PsSpace, RoundedCornerShape(6.dp))
            .pointerInput(n) {
                detectTapGestures { offset ->
                    val index = ((offset.x / size.width) * (n + 1)).toInt().coerceIn(0, n)
                    onSelect(index)
                }
            }
            .semantics { contentDescription = "Binomial probability bar chart. Selected outcome $selected." },
    ) {
        val slot = size.width / (n + 1)
        probabilities.forEachIndexed { index, probability ->
            val barHeight = (probability / highest * (size.height - 28f)).toFloat()
            drawRect(
                color = if (index == selected) PsAmber else PsCyan.copy(alpha = .72f),
                topLeft = Offset(index * slot + 1f, size.height - barHeight - 18f),
                size = Size((slot - 2f).coerceAtLeast(1f), barHeight),
            )
        }
        val meanX = (n * p).toFloat() * slot + slot / 2
        drawLine(PsGreen, Offset(meanX, 0f), Offset(meanX, size.height), strokeWidth = 3f)
    }
}

@Composable
private fun PracticeBlock(topic: StatisticsTopic, model: ProbabilityStatisticsViewModel, assessment: Boolean) {
    val questions = if (assessment) List(5) { topic.practice[it % topic.practice.size] } else topic.practice
    var index by rememberSaveable(topic.id, assessment) { mutableIntStateOf(0) }
    var selected by rememberSaveable(topic.id, assessment, index) { mutableIntStateOf(-1) }
    val question = questions[index]
    Text(if (assessment) "Quick check ${index + 1} of ${questions.size}" else "Practice", color = if (assessment) PsAmber else PsGreen, fontWeight = FontWeight.Bold)
    Text(question.prompt, color = PsInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    question.options.forEachIndexed { answerIndex, answer ->
        val accent = when {
            selected < 0 -> PsMuted
            answerIndex == question.answerIndex -> PsGreen
            answerIndex == selected -> PsRed
            else -> PsMuted
        }
        FeaturePanel(accent, Modifier.fillMaxWidth().clickable(enabled = selected < 0) {
            selected = answerIndex
            model.recordAnswer(answerIndex == question.answerIndex)
        }) {
            Text(answer, color = if (selected >= 0 && answerIndex == question.answerIndex) PsGreen else PsInk)
        }
    }
    if (selected >= 0) {
        Text(if (selected == question.answerIndex) "Correct" else "Review this idea", color = if (selected == question.answerIndex) PsGreen else PsAmber, fontWeight = FontWeight.Bold)
        Text(question.explanation, color = PsMuted, fontSize = 13.sp)
        if (index < questions.lastIndex) PrimaryAction("Next question", PsCyan) { index++; selected = -1 }
        else LabelValue("Recommendation", "Continue the lesson, retry the explorer, or apply this concept to a dataset.")
    }
}

@Composable
internal fun DistributionExplorerScreen(model: ProbabilityStatisticsViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar("Distribution Explorer", "PMF, CDF, moments and simulation", model::back) }
        item {
            FeaturePanel(PsViolet, Modifier.fillMaxWidth()) {
                Text("Binomial reference model", color = PsInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                BinomialInteractiveLab(TopicWorkspaceMode.Explore)
            }
        }
        item {
            FeaturePanel(PsCyan, Modifier.fillMaxWidth()) {
                Text("More validated distributions", color = PsCyan, fontWeight = FontWeight.Bold)
                Text("Normal, Poisson, uniform, exponential, sampling and comparison workspaces remain available in Advanced Labs from the home screen.", color = PsMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun DatasetLabScreen(model: ProbabilityStatisticsViewModel) {
    var raw by rememberSaveable { mutableStateOf(ProbabilityStatisticsCatalog.datasets.first().values.joinToString(", ")) }
    val quality = remember(raw) { ProbabilityStatisticsEngine.inspectDataset(raw) }
    val summary = remember(quality.validValues) { ProbabilityStatisticsEngine.descriptive(quality.validValues).getOrNull() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar("Dataset Lab", "Enter, inspect and summarize offline data", model::back) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProbabilityStatisticsCatalog.datasets.forEach { dataset ->
                    PrimaryAction(dataset.title, PsGreen) { raw = dataset.values.joinToString(", ") }
                }
            }
        }
        item {
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it.take(40_000) },
                label = { Text("Values separated by commas, spaces or lines") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).semantics { contentDescription = "Dataset value editor" },
            )
        }
        item {
            FeaturePanel(if (quality.invalidTokens.isEmpty()) PsGreen else PsAmber, Modifier.fillMaxWidth()) {
                SectionTitle("Data quality")
                LabelValue("Valid observations", quality.validValues.size.toString())
                LabelValue("Invalid entries", quality.invalidTokens.take(5).joinToString().ifBlank { "None" })
                LabelValue("Potential IQR outliers", quality.outliers.joinToString { f(it) }.ifBlank { "None" })
            }
        }
        summary?.let {
            item {
                FeaturePanel(PsCyan, Modifier.fillMaxWidth()) {
                    SectionTitle("Descriptive summary")
                    MetricGrid(listOf("Mean" to f(it.mean), "Median" to f(it.median), "Std dev" to f(it.populationStandardDeviation), "Range" to f(it.range), "Q1" to f(it.q1), "Q3" to f(it.q3)))
                    MiniDataBars(quality.validValues, PsCyan, "Dataset distribution preview")
                    LabelValue("Recommended visualization", if (quality.validValues.size < 30) "Dot plot and box plot" else "Histogram and box plot")
                    LabelValue("Why", "The outcome is quantitative; these displays reveal centre, spread, clusters and unusual values.")
                }
            }
        } ?: item { EmptyState("Enter at least one valid number to calculate a summary.") }
    }
}

@Composable
internal fun TestGuideScreen(model: ProbabilityStatisticsViewModel) {
    var objective by rememberSaveable { mutableStateOf(AnalysisObjective.Compare) }
    var outcome by rememberSaveable { mutableStateOf(OutcomeType.Quantitative) }
    var groups by rememberSaveable { mutableStateOf(GroupStructure.TwoIndependent) }
    var assumptions by rememberSaveable { mutableStateOf(true) }
    val result = StatisticalTestGuide.recommend(objective, outcome, groups, assumptions)
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar("Which Test Should I Use?", "A deterministic method-selection guide", model::back) }
        item { ChoicePanel("1. Objective", AnalysisObjective.entries, objective, { it.label }) { objective = it } }
        item { ChoicePanel("2. Outcome type", OutcomeType.entries, outcome, { it.label }) { outcome = it } }
        item { ChoicePanel("3. Groups or samples", GroupStructure.entries, groups, { it.label }) { groups = it } }
        item {
            FeaturePanel(PsViolet, Modifier.fillMaxWidth()) {
                Text("4. Are parametric assumptions credible?", color = PsInk, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(if (assumptions) "• Yes" else "Yes", PsGreen) { assumptions = true }
                    PrimaryAction(if (!assumptions) "• No / unsure" else "No / unsure", PsAmber) { assumptions = false }
                }
            }
        }
        item {
            FeaturePanel(PsAmber, Modifier.fillMaxWidth()) {
                Text(result.method, color = PsAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(result.reason, color = PsInk)
                LabelValue("Required checks", result.assumptions.joinToString(" · "))
                LabelValue("Alternative", result.alternative)
                Text(result.caution, color = PsMuted, fontSize = 11.sp)
                PrimaryAction("Open lesson", PsCyan) { model.openTopic(result.topicId) }
            }
        }
    }
}

@Composable
private fun <T> ChoicePanel(title: String, choices: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FeaturePanel(PsCyan, Modifier.fillMaxWidth()) {
        Text(title, color = PsInk, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { choice ->
                PrimaryAction(if (choice == selected) "• ${label(choice)}" else label(choice), if (choice == selected) PsCyan else PsMuted) { onSelect(choice) }
            }
        }
    }
}

@Composable
internal fun FormulaLibraryScreen(model: ProbabilityStatisticsViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val formulas = ProbabilityStatisticsCatalog.topics
        .filter { query.isBlank() || query.lowercase() in it.title.lowercase() || it.aliases.any { alias -> query.lowercase() in alias.lowercase() } }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar("Formula Library", "Search by topic, name or use", model::back) }
        item { OutlinedTextField(query, { query = it.take(60) }, label = { Text("Search formulas") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        items(formulas, key = { it.id }) { topic ->
            FeaturePanel(categoryColor(topic.categoryId), Modifier.fillMaxWidth().clickable { model.openTopic(topic.id, TopicWorkspaceMode.Formula) }) {
                Text(topic.title, color = PsInk, fontWeight = FontWeight.Bold)
                topic.formulas.forEach { formula ->
                    Text(formula.expression, color = PsCyan, fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = formula.spoken })
                    Text(formula.spoken, color = PsMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MiniDataBars(values: List<Double>, accent: Color, description: String) {
    if (values.isEmpty()) return
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: min + 1
    val width = (max - min).coerceAtLeast(1e-9)
    val bins = IntArray(12)
    values.forEach { bins[(((it - min) / width) * bins.lastIndex).roundToInt().coerceIn(0, bins.lastIndex)]++ }
    val highest = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
    Canvas(Modifier.fillMaxWidth().height(150.dp).background(PsSpace, RoundedCornerShape(6.dp)).semantics { contentDescription = description }) {
        val slot = size.width / bins.size
        bins.forEachIndexed { index, count ->
            val h = count.toFloat() / highest * (size.height - 16f)
            drawRect(accent.copy(alpha = .78f), Offset(index * slot + 2f, size.height - h), Size(slot - 4f, h))
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        metrics.forEach { (label, value) ->
            Column(
                Modifier.width(108.dp).background(PsPanelSoft, RoundedCornerShape(6.dp)).border(1.dp, PsCyan.copy(.25f), RoundedCornerShape(6.dp)).padding(8.dp),
            ) {
                Text(label, color = PsMuted, fontSize = 10.sp)
                Text(value, color = PsInk, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LessonStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.width(28.dp).height(28.dp).background(PsCyan.copy(.16f), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = PsCyan, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Text(text, color = PsInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = PsMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = PsInk, fontSize = 14.sp)
    }
}

private fun modeColor(mode: TopicWorkspaceMode): Color = when (mode) {
    TopicWorkspaceMode.Overview -> PsCyan
    TopicWorkspaceMode.Learn -> PsGreen
    TopicWorkspaceMode.Visualize, TopicWorkspaceMode.Explore -> PsViolet
    TopicWorkspaceMode.Simulate -> PsCoral
    TopicWorkspaceMode.Formula -> PsCyan
    TopicWorkspaceMode.Examples, TopicWorkspaceMode.Applications -> PsAmber
    TopicWorkspaceMode.Practice, TopicWorkspaceMode.Assessment -> PsGreen
}

private fun commonMistake(topicId: String): String = when (topicId) {
    "mean-median-mode" -> "Reporting the mean as 'typical' without checking skew or extreme observations."
    "variance-standard-deviation" -> "Mixing population division by N with sample division by n−1."
    "conditional-probability" -> "Reversing P(A|B) and P(B|A); they generally answer different questions."
    "binomial-distribution" -> "Using the model when trials are dependent or the success probability changes."
    "hypothesis-testing" -> "Treating a p-value as the probability that the null hypothesis is true."
    "regression-correlation" -> "Reading association as proof of causation."
    else -> "Applying a formula before checking the variable type, assumptions and data-collection process."
}

internal fun f(value: Double): String = when {
    !value.isFinite() -> "undefined"
    abs(value) >= 1_000_000 || (abs(value) in 0.0..0.0001 && value != 0.0) -> String.format(Locale.US, "%.3e", value)
    else -> String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
}
