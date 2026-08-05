package com.indianservers.aiexplorer.probabilitystats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.DescriptiveStatistics
import com.indianservers.aiexplorer.core.HistogramBin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private val PhaseSpace = Color(0xFF030817)
private val PhasePanel = Color(0xEC0B1730)
private val PhaseInk = Color(0xFFF5F8FF)
private val PhaseMuted = Color(0xFFA9B7D6)
private val PhaseCyan = Color(0xFF22D8FF)
private val PhasePurple = Color(0xFFA66BFF)
private val PhaseGreen = Color(0xFF4DE5A8)
private val PhaseAmber = Color(0xFFFFB83E)
private val PhasePink = Color(0xFFFF67A6)
private val PhaseRed = Color(0xFFFF637D)

internal enum class Phase1Route { Hub, Data, Descriptive, Charts }
internal enum class Phase1Chart(val label: String) { DotPlot("Dot plot"), Histogram("Histogram"), BoxPlot("Box plot") }
internal enum class Phase1Tab { Visual, Calculation, Explanation }

internal data class Phase1Row(val id: Long, val raw: String)

internal data class ProbabilityPhase1State(
    val route: Phase1Route = Phase1Route.Hub,
    val rows: List<Phase1Row> = defaultRows(),
    val columnName: String = "Observation",
    val selectedChart: Phase1Chart = Phase1Chart.DotPlot,
    val selectedTab: Phase1Tab = Phase1Tab.Visual,
    val binCount: Int = 6,
    val selectedPoint: Int? = null,
    val revision: Int = 0,
) {
    val values: List<Double>
        get() = rows.mapNotNull { it.raw.trim().takeIf(String::isNotEmpty)?.toDoubleOrNull() }
    val invalidRows: Set<Long>
        get() = rows.filter { it.raw.isNotBlank() && it.raw.toDoubleOrNull()?.isFinite() != true }.mapTo(mutableSetOf()) { it.id }
}

private fun defaultRows(): List<Phase1Row> =
    listOf(4.0, 5.0, 5.0, 6.0, 7.0, 8.0, 8.0, 8.0, 9.0, 10.0, 12.0, 18.0)
        .mapIndexed { index, value -> Phase1Row(index.toLong() + 1, trimPhase(value)) }

internal object ProbabilityPhase1DatasetEngine {
    fun parsePasted(text: String): List<String> =
        text.lineSequence()
            .flatMap { line -> line.split(',', '\t', ';', ' ').asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

    fun accessibleHistogramSummary(bins: List<HistogramBin>): String {
        if (bins.isEmpty()) return "Histogram has no valid observations."
        val highest = bins.maxBy { it.count }
        return "Histogram with ${bins.size} bins. Highest frequency is ${highest.count} from ${trimPhase(highest.lower)} to ${trimPhase(highest.upper)}."
    }
}

internal class ProbabilityPhase1ViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProbabilityPhase1State())
    val state: StateFlow<ProbabilityPhase1State> = _state.asStateFlow()
    private var nextId = 100L

    fun open(route: Phase1Route, chart: Phase1Chart? = null) {
        _state.value = _state.value.copy(route = route, selectedChart = chart ?: _state.value.selectedChart)
    }

    fun home() {
        _state.value = _state.value.copy(route = Phase1Route.Hub, selectedPoint = null)
    }

    fun renameColumn(name: String) {
        _state.value = _state.value.copy(columnName = name.take(28), revision = _state.value.revision + 1)
    }

    fun editRow(id: Long, raw: String) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.map { if (it.id == id) it.copy(raw = raw.take(24)) else it },
            revision = _state.value.revision + 1,
        )
    }

    fun addRow() {
        _state.value = _state.value.copy(rows = _state.value.rows + Phase1Row(nextId++, ""))
    }

    fun deleteRow(id: Long) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.filterNot { it.id == id }.ifEmpty { listOf(Phase1Row(nextId++, "")) },
            revision = _state.value.revision + 1,
        )
    }

    fun paste(text: String) {
        val parsed = ProbabilityPhase1DatasetEngine.parsePasted(text)
        if (parsed.isEmpty()) return
        _state.value = _state.value.copy(
            rows = parsed.map { Phase1Row(nextId++, it.take(24)) },
            revision = _state.value.revision + 1,
        )
    }

    fun sort() {
        val valid = _state.value.values.sorted()
        _state.value = _state.value.copy(
            rows = valid.map { Phase1Row(nextId++, trimPhase(it)) },
            revision = _state.value.revision + 1,
        )
    }

    fun generate(seed: Int = 42) {
        val random = Random(seed + _state.value.revision)
        val generated = List(24) {
            val center = random.nextDouble(35.0, 75.0)
            val adjustment = random.nextDouble(-12.0, 12.0)
            (center + adjustment).coerceIn(0.0, 100.0)
        }
        _state.value = _state.value.copy(
            rows = generated.map { Phase1Row(nextId++, trimPhase(it)) },
            columnName = "Generated sample",
            revision = _state.value.revision + 1,
        )
    }

    fun preset(values: List<Double>, name: String) {
        _state.value = _state.value.copy(
            rows = values.map { Phase1Row(nextId++, trimPhase(it)) },
            columnName = name,
            revision = _state.value.revision + 1,
        )
    }

    fun selectTab(tab: Phase1Tab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun selectChart(chart: Phase1Chart) {
        _state.value = _state.value.copy(selectedChart = chart)
    }

    fun setBins(count: Int) {
        _state.value = _state.value.copy(binCount = count.coerceIn(2, 20))
    }

    fun dragValue(index: Int, value: Double) {
        val validRows = _state.value.rows.filter { it.raw.toDoubleOrNull()?.isFinite() == true }
        val target = validRows.getOrNull(index) ?: return
        editRow(target.id, trimPhase(value))
        _state.value = _state.value.copy(selectedPoint = index)
    }
}

@Composable
internal fun ProbabilityStatisticsPhase1Screen(
    onExit: () -> Unit,
    onOpenPhase2: () -> Unit,
    onOpenLegacy: () -> Unit,
    model: ProbabilityPhase1ViewModel = viewModel(),
) {
    val state by model.state.collectAsState()
    val summary = state.values.takeIf(List<Double>::isNotEmpty)?.let {
        runCatching { AdvancedStatisticsEngine.summarize(it) }.getOrNull()
    }
    val histogram = state.values.takeIf(List<Double>::isNotEmpty)?.let {
        runCatching { AdvancedStatisticsEngine.histogram(it, state.binCount) }.getOrDefault(emptyList())
    }.orEmpty()

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(Color(0xFF122A56), PhaseSpace, Color(0xFF01030B)),
                center = Offset(240f, 80f),
                radius = 1050f,
            ),
        ),
    ) {
        when (state.route) {
            Phase1Route.Hub -> Phase1Hub(model, onExit, onOpenPhase2, onOpenLegacy)
            Phase1Route.Data -> Phase1DataWorkspace(state, summary, model, onExit)
            Phase1Route.Descriptive -> Phase1DescriptiveWorkspace(state, summary, histogram, model, onExit)
            Phase1Route.Charts -> Phase1ChartsWorkspace(state, summary, histogram, model, onExit)
        }
    }
}

@Composable
private fun Phase1Hub(model: ProbabilityPhase1ViewModel, onExit: () -> Unit, onOpenPhase2: () -> Unit, onOpenLegacy: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val tools = listOf(
        HubTool("Data Explorer", "Edit, paste, sort and generate datasets", "▦", PhaseCyan, Phase1Route.Data, null),
        HubTool("Descriptive Statistics", "Mean, median, mode, spread and outliers", "x̄", PhasePurple, Phase1Route.Descriptive, Phase1Chart.DotPlot),
        HubTool("Draggable Dot Plot", "Move observations and inspect live changes", "••", PhaseGreen, Phase1Route.Descriptive, Phase1Chart.DotPlot),
        HubTool("Histogram", "Adjust bins and inspect frequencies", "▥", PhaseAmber, Phase1Route.Charts, Phase1Chart.Histogram),
        HubTool("Box Plot", "Quartiles, whiskers and linked outliers", "⊢⊣", PhasePink, Phase1Route.Charts, Phase1Chart.BoxPlot),
    )
    val normalizedQuery = query.trim().lowercase()
    val visibleTools = tools.filter {
        normalizedQuery.isBlank() || normalizedQuery in it.title.lowercase() || normalizedQuery in it.description.lowercase()
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        PhaseTopBar("Probability & Statistics", "PHASE 1 · DATA FOUNDATIONS", onExit)
        PhasePanelCard(PhaseCyan) {
            Text("Native interactive statistics workspace", color = PhaseInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Edit one dataset and watch every statistic and visualization update together.", color = PhaseMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PhaseBadge("LIVE DATA", PhaseGreen)
                PhaseBadge("DRAGGABLE", PhasePurple)
                PhaseBadge("EXPLAINED", PhaseAmber)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(60) },
            placeholder = { Text("Search Phase 1 tools") },
            leadingIcon = { Text("⌕", color = PhasePurple, fontSize = 22.sp) },
            trailingIcon = { Text("5 TOOLS", color = PhaseCyan, fontSize = 9.sp, fontWeight = FontWeight.Black) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("START EXPLORING", color = PhaseCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 700.dp) 3 else 2
            val gap = 8.dp
            val availableWidth = maxWidth
            val width = (availableWidth - gap * (columns - 1)) / columns
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                visibleTools.forEachIndexed { index, tool ->
                    PhaseHubCard(
                        tool,
                        Modifier.width(if (columns == 2 && index == visibleTools.lastIndex && visibleTools.size % 2 == 1) availableWidth else width),
                    ) { model.open(tool.route, tool.chart) }
                }
            }
            if (visibleTools.isEmpty()) {
                Text("No Phase 1 tools match “$query”.", color = PhaseAmber, modifier = Modifier.padding(12.dp))
            }
        }
        PhasePanelCard(PhaseAmber) {
            Text("RECOMMENDED ACTIVITY", color = PhaseAmber, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("Mean versus median", color = PhaseInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Move the largest point farther right. Observe which measure changes more and explain why.", color = PhaseMuted, fontSize = 11.sp)
            PhaseButton("Start activity", PhaseAmber) { model.open(Phase1Route.Descriptive, Phase1Chart.DotPlot) }
        }
        PhasePanelCard(PhasePurple) {
            Text("PHASE 2 AVAILABLE", color = PhasePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("Chance, rules and distributions", color = PhaseInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Run probability experiments, explore Venn rules, combinatorics, distributions, normal probability and the Central Limit Theorem.", color = PhaseMuted, fontSize = 11.sp)
            PhaseButton("Open Phase 2", PhasePurple, onOpenPhase2)
        }
        PhasePanelCard(PhasePurple) {
            Text("EXISTING ADVANCED LABS", color = PhasePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("Distributions, simulations, inference and learning materials remain available while the new module is built phase by phase.", color = PhaseMuted, fontSize = 11.sp)
            PhaseOutlineButton("Open existing labs", PhasePurple, onOpenLegacy)
        }
    }
}

private data class HubTool(
    val title: String,
    val description: String,
    val icon: String,
    val accent: Color,
    val route: Phase1Route,
    val chart: Phase1Chart?,
)

@Composable
private fun PhaseHubCard(tool: HubTool, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.heightIn(min = 142.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(tool.accent.copy(.2f), PhasePanel)))
            .border(1.dp, tool.accent.copy(.62f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).focusable().padding(12.dp)
            .semantics { contentDescription = "Open ${tool.title}. ${tool.description}" },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tool.accent.copy(.18f)), contentAlignment = Alignment.Center) {
                Text(tool.icon, color = tool.accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
            Text("OPEN ›", color = tool.accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
        Text(tool.title, color = PhaseInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
        Text(tool.description, color = PhaseMuted, fontSize = 9.sp, maxLines = 3)
    }
}

@Composable
private fun Phase1DataWorkspace(
    state: ProbabilityPhase1State,
    summary: DescriptiveStatistics?,
    model: ProbabilityPhase1ViewModel,
    onExit: () -> Unit,
) {
    PhaseWorkspaceScaffold(
        title = "Data Explorer",
        subtitle = "${state.values.size} valid · ${state.invalidRows.size} invalid · revision ${state.revision}",
        onBack = model::home,
        onExit = onExit,
        controls = {
            DatasetEditor(state, model)
        },
        visualization = {
            LinkedDataPreview(state, summary, model)
        },
    )
}

@Composable
private fun DatasetEditor(state: ProbabilityPhase1State, model: ProbabilityPhase1ViewModel) {
    varPasteEditor(state, model)
}

@Composable
private fun varPasteEditor(state: ProbabilityPhase1State, model: ProbabilityPhase1ViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DATASET", color = PhaseCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            OutlinedTextField(
                value = state.columnName,
                onValueChange = model::renameColumn,
                label = { Text("Column name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            var pasteText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            OutlinedTextField(
                value = pasteText,
                onValueChange = { pasteText = it },
                label = { Text("Paste comma, space, tab or line-separated values") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PhaseSmallButton("Paste", PhaseCyan, enabled = pasteText.isNotBlank()) {
                    model.paste(pasteText)
                    pasteText = ""
                }
                PhaseSmallButton("Add row", PhaseGreen, onClick = model::addRow)
                PhaseSmallButton("Sort", PhasePurple, onClick = model::sort)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PhaseSmallButton("Generate 24", PhaseAmber, onClick = model::generate)
                PhaseSmallButton("Outlier set", PhasePink) {
                    model.preset(listOf(10.0, 11.0, 11.0, 12.0, 12.0, 13.0, 13.0, 14.0, 40.0), "Outlier study")
                }
            }
            Text("ROWS", color = PhaseMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
            LazyColumn(
                Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(.18f)),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.rows, key = { it.id }) { row ->
                    val invalid = row.id in state.invalidRows
                    Row(
                        Modifier.fillMaxWidth().background(if (invalid) PhaseRed.copy(.08f) else Color.Transparent).padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text((state.rows.indexOf(row) + 1).toString(), color = PhaseMuted, fontSize = 9.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                        OutlinedTextField(
                            value = row.raw,
                            onValueChange = { model.editRow(row.id, it) },
                            label = if (invalid) ({ Text("Invalid number", color = PhaseRed) }) else null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "×",
                            color = PhaseRed,
                            fontSize = 18.sp,
                            modifier = Modifier.clip(CircleShape).clickable { model.deleteRow(row.id) }.padding(10.dp),
                        )
                    }
                }
            }
            if (state.invalidRows.isNotEmpty()) {
                Text("${state.invalidRows.size} invalid value(s) are excluded until corrected.", color = PhaseRed, fontSize = 10.sp)
            }
    }
}

@Composable
private fun LinkedDataPreview(state: ProbabilityPhase1State, summary: DescriptiveStatistics?, model: ProbabilityPhase1ViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("LINKED PREVIEW", color = PhasePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(state.columnName.ifBlank { "Untitled dataset" }, color = PhaseInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            PhaseBadge("LIVE", PhaseGreen)
        }
        if (summary == null) {
            EmptyPhaseState("Enter at least one finite number.")
        } else {
            DraggableDotPlot(state.values, summary, state.selectedPoint, model::dragValue)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricTile("Mean", trimPhase(summary.mean), PhaseGreen, Modifier.weight(1f))
                MetricTile("Median", trimPhase(summary.median), PhasePurple, Modifier.weight(1f))
                MetricTile("Sample s", trimPhase(summary.sampleStandardDeviation), PhaseAmber, Modifier.weight(1f))
            }
            Text("Drag any point horizontally. The corresponding row and every result update instantly.", color = PhaseMuted, fontSize = 10.sp)
            PhaseButton("Open full analysis", PhasePurple) { model.open(Phase1Route.Descriptive, Phase1Chart.DotPlot) }
        }
    }
}

@Composable
private fun Phase1DescriptiveWorkspace(
    state: ProbabilityPhase1State,
    summary: DescriptiveStatistics?,
    histogram: List<HistogramBin>,
    model: ProbabilityPhase1ViewModel,
    onExit: () -> Unit,
) {
    PhaseWorkspaceScaffold(
        title = "Descriptive Statistics",
        subtitle = "Visual · calculation · explanation",
        onBack = model::home,
        onExit = onExit,
        controls = {
            PhaseTabSelector(state.selectedTab, model::selectTab)
            when (state.selectedTab) {
                Phase1Tab.Visual -> {
                    Text("DRAG AN OBSERVATION", color = PhaseCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("The dataset is the single source of truth. Moving a point edits its row.", color = PhaseMuted, fontSize = 11.sp)
                    DatasetPresetButtons(model)
                }
                Phase1Tab.Calculation -> CalculationSteps(state.values, summary)
                Phase1Tab.Explanation -> DataExplanation(state.values, summary)
            }
            PhaseOutlineButton("Edit dataset", PhaseCyan) { model.open(Phase1Route.Data) }
        },
        visualization = {
            if (summary == null) EmptyPhaseState("Enter a valid dataset in Data Explorer.")
            else {
                DraggableDotPlot(state.values, summary, state.selectedPoint, model::dragValue)
                DescriptiveMetricGrid(summary)
                val frequency = AdvancedStatisticsEngine.frequencyTable(state.values)
                Text("FREQUENCY TABLE", color = PhaseCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                frequency.take(8).forEach { item ->
                    PhaseInsight(trimPhase(item.value), "${item.count} · ${(item.relativeFrequency * 100).toInt()}% · cumulative ${item.cumulativeFrequency}", PhaseCyan)
                }
                if (frequency.size > 8) Text("+ ${frequency.size - 8} more distinct values", color = PhaseMuted, fontSize = 10.sp)
            }
        },
    )
}

@Composable
private fun Phase1ChartsWorkspace(
    state: ProbabilityPhase1State,
    summary: DescriptiveStatistics?,
    histogram: List<HistogramBin>,
    model: ProbabilityPhase1ViewModel,
    onExit: () -> Unit,
) {
    PhaseWorkspaceScaffold(
        title = "Charts & Graphs",
        subtitle = "Linked native Canvas visualizations",
        onBack = model::home,
        onExit = onExit,
        controls = {
            Text("CHART TYPE", color = PhaseCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            PhaseChartSelector(state.selectedChart, model::selectChart)
            if (state.selectedChart == Phase1Chart.Histogram) {
                Text("Bins: ${state.binCount}", color = PhaseInk, fontWeight = FontWeight.Bold)
                Slider(
                    value = state.binCount.toFloat(),
                    onValueChange = { model.setBins(it.toInt()) },
                    valueRange = 2f..20f,
                    steps = 17,
                )
                Text(ProbabilityPhase1DatasetEngine.accessibleHistogramSummary(histogram), color = PhaseMuted, fontSize = 10.sp)
            }
            DatasetPresetButtons(model)
            PhaseOutlineButton("Edit dataset", PhaseCyan) { model.open(Phase1Route.Data) }
        },
        visualization = {
            if (summary == null) EmptyPhaseState("Enter a valid dataset in Data Explorer.")
            else {
                when (state.selectedChart) {
                    Phase1Chart.DotPlot -> DraggableDotPlot(state.values, summary, state.selectedPoint, model::dragValue)
                    Phase1Chart.Histogram -> NativeHistogram(histogram)
                    Phase1Chart.BoxPlot -> NativeBoxPlot(state.values, summary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricTile("n", summary.count.toString(), PhaseCyan, Modifier.weight(1f))
                    MetricTile("Centre", trimPhase(summary.median), PhasePurple, Modifier.weight(1f))
                    MetricTile("IQR", trimPhase(summary.interquartileRange), PhaseAmber, Modifier.weight(1f))
                }
            }
        },
    )
}

@Composable
private fun PhaseWorkspaceScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onExit: () -> Unit,
    controls: @Composable () -> Unit,
    visualization: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            PhaseTopBar(title, subtitle, onBack, onExit)
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PhasePanelCard(PhaseCyan, Modifier.weight(.38f).fillMaxHeight().verticalScroll(rememberScrollState()), controls)
                    PhasePanelCard(PhasePurple, Modifier.weight(.62f).fillMaxHeight().verticalScroll(rememberScrollState()), visualization)
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    PhasePanelCard(PhasePurple, content = visualization)
                    PhasePanelCard(PhaseCyan, content = controls)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PhaseTopBar(title: String, subtitle: String, onBack: () -> Unit, onExit: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PhaseCyan.copy(.15f))
                .border(1.dp, PhaseCyan.copy(.6f), RoundedCornerShape(13.dp)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = PhaseCyan, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f)) {
            Text(title, color = PhaseInk, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = PhaseCyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        if (onExit != null) {
            Text(
                "MATH HOME",
                color = PhaseMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable(onClick = onExit)
                    .border(1.dp, PhaseMuted.copy(.35f), RoundedCornerShape(11.dp)).padding(horizontal = 9.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun PhasePanelCard(accent: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(accent.copy(.09f), PhasePanel)))
            .border(1.dp, accent.copy(.42f), RoundedCornerShape(22.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) { content() }
}

@Composable
private fun PhaseBadge(label: String, accent: Color) {
    Text(
        label,
        color = accent,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clip(CircleShape).background(accent.copy(.12f))
            .border(1.dp, accent.copy(.4f), CircleShape).padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun PhaseButton(label: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = PhaseSpace),
    ) { Text(label, fontWeight = FontWeight.Black) }
}

@Composable
private fun PhaseOutlineButton(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(.08f))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(15.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = accent, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PhaseSmallButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.height(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(if (enabled) .14f else .04f))
            .border(1.dp, accent.copy(if (enabled) .48f else .15f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) accent else PhaseMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PhaseTabSelector(selected: Phase1Tab, onSelect: (Phase1Tab) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Phase1Tab.entries.forEach { tab ->
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (tab == selected) PhasePurple.copy(.25f) else Color.White.copy(.04f))
                    .border(1.dp, if (tab == selected) PhasePurple else PhaseMuted.copy(.2f), RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) { Text(tab.name, color = if (tab == selected) PhaseInk else PhaseMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PhaseChartSelector(selected: Phase1Chart, onSelect: (Phase1Chart) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Phase1Chart.entries.forEach { chart ->
            PhaseSmallButton(if (selected == chart) "● ${chart.label}" else chart.label, if (selected == chart) PhasePurple else PhaseCyan) { onSelect(chart) }
        }
    }
}

@Composable
private fun DatasetPresetButtons(model: ProbabilityPhase1ViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PhaseSmallButton("Symmetric", PhaseCyan) { model.preset(listOf(2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0), "Symmetric") }
        PhaseSmallButton("Skewed", PhaseAmber) { model.preset(listOf(1.0, 1.0, 2.0, 2.0, 3.0, 5.0, 8.0, 13.0, 21.0), "Right-skewed") }
        PhaseSmallButton("Outlier", PhasePink) { model.preset(listOf(10.0, 11.0, 11.0, 12.0, 12.0, 13.0, 13.0, 14.0, 40.0), "Outlier study") }
    }
}

@Composable
private fun DescriptiveMetricGrid(summary: DescriptiveStatistics) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple("Count", summary.count.toString(), PhaseCyan),
            Triple("Sum", trimPhase(summary.sum), PhaseCyan),
            Triple("Mean", trimPhase(summary.mean), PhaseGreen),
            Triple("Median", trimPhase(summary.median), PhasePurple),
            Triple("Mode", summary.modes.joinToString { trimPhase(it) }.ifBlank { "none" }, PhaseAmber),
            Triple("Range", trimPhase(summary.range), PhaseCyan),
            Triple("IQR", trimPhase(summary.interquartileRange), PhasePurple),
            Triple("Population σ", trimPhase(summary.populationStandardDeviation), PhaseGreen),
            Triple("Sample s", trimPhase(summary.sampleStandardDeviation), PhaseAmber),
            Triple("Variance", trimPhase(summary.sampleVariance), PhaseCyan),
            Triple("Skewness", summary.skewness?.let(::trimPhase) ?: "n ≥ 3", PhasePurple),
            Triple("Kurtosis", summary.excessKurtosis?.let(::trimPhase) ?: "n ≥ 4", PhaseAmber),
        ).forEach { (label, value, color) -> MetricTile(label, value, color, Modifier.width(105.dp)) }
    }
    PhaseInsight("Five-number summary", listOf(summary.fiveNumber.minimum, summary.fiveNumber.firstQuartile, summary.median, summary.fiveNumber.thirdQuartile, summary.fiveNumber.maximum).joinToString { trimPhase(it) }, PhasePurple)
    PhaseInsight("Outliers", summary.outliers.joinToString { trimPhase(it) }.ifBlank { "none" }, if (summary.outliers.isEmpty()) PhaseGreen else PhasePink)
}

@Composable
private fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(accent.copy(.1f))
            .border(1.dp, accent.copy(.35f), RoundedCornerShape(14.dp)).padding(8.dp),
    ) {
        Text(label, color = PhaseMuted, fontSize = 8.sp)
        Text(value, color = PhaseInk, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun PhaseInsight(label: String, value: String, accent: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.035f)).padding(9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PhaseMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.25f))
    }
}

@Composable
private fun CalculationSteps(values: List<Double>, summary: DescriptiveStatistics?) {
    Text("CALCULATION", color = PhasePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
    if (summary == null) {
        EmptyPhaseState("A valid dataset is required.")
        return
    }
    Text("Mean", color = PhaseInk, fontWeight = FontWeight.Bold)
    Text("x̄ = Σx / n", color = PhaseCyan, fontFamily = FontFamily.Monospace)
    Text("= ${values.joinToString(" + ") { trimPhase(it) }} / ${values.size}", color = PhaseMuted, fontSize = 10.sp)
    Text("= ${trimPhase(summary.sum)} / ${summary.count} = ${trimPhase(summary.mean)}", color = PhaseGreen, fontWeight = FontWeight.Bold)
    Text("Median", color = PhaseInk, fontWeight = FontWeight.Bold)
    Text("Ordered: ${values.sorted().joinToString { trimPhase(it) }}", color = PhaseMuted, fontSize = 10.sp)
    Text("Middle position gives ${trimPhase(summary.median)}", color = PhasePurple, fontWeight = FontWeight.Bold)
    Text("Sample standard deviation", color = PhaseInk, fontWeight = FontWeight.Bold)
    Text("s = √[Σ(x − x̄)² / (n − 1)] = ${trimPhase(summary.sampleStandardDeviation)}", color = PhaseAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
}

@Composable
private fun DataExplanation(values: List<Double>, summary: DescriptiveStatistics?) {
    Text("EXPLANATION", color = PhaseAmber, fontSize = 9.sp, fontWeight = FontWeight.Black)
    if (summary == null) {
        EmptyPhaseState("A valid dataset is required.")
        return
    }
    val relation = when {
        abs(summary.mean - summary.median) < max(.01, summary.populationStandardDeviation * .08) -> "The mean and median are close, so the centre is fairly balanced."
        summary.mean > summary.median -> "The mean is above the median. Larger observations are pulling the arithmetic centre to the right."
        else -> "The mean is below the median. Smaller observations are pulling the arithmetic centre to the left."
    }
    Text(relation, color = PhaseInk, fontSize = 12.sp)
    Text(
        if (summary.outliers.isEmpty()) "No observations cross the 1.5 × IQR fences."
        else "${summary.outliers.size} observation(s) cross the 1.5 × IQR fences: ${summary.outliers.joinToString { trimPhase(it) }}.",
        color = if (summary.outliers.isEmpty()) PhaseGreen else PhasePink,
        fontSize = 11.sp,
    )
    Text("The middle 50% spans ${trimPhase(summary.interquartileRange)} units. Sample spread is s = ${trimPhase(summary.sampleStandardDeviation)}.", color = PhaseMuted, fontSize = 11.sp)
    Text("Try dragging ${values.maxOrNull()?.let(::trimPhase)} farther right and compare how the mean and median respond.", color = PhaseAmber, fontSize = 11.sp)
}

@Composable
private fun DraggableDotPlot(
    values: List<Double>,
    summary: DescriptiveStatistics,
    selectedPoint: Int?,
    onDragValue: (Int, Double) -> Unit,
) {
    val minimum = values.minOrNull() ?: 0.0
    val maximum = values.maxOrNull() ?: 1.0
    val spread = (maximum - minimum).takeIf { it > 0 } ?: 1.0
    val domainMin = minimum - spread * .15
    val domainMax = maximum + spread * .15
    var active = selectedPoint
    Canvas(
        Modifier.fillMaxWidth().height(255.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.22f))
            .pointerInput(values, domainMin, domainMax) {
                detectDragGestures(
                    onDragStart = { start ->
                        active = values.indices.minByOrNull { index ->
                            val x = ((values[index] - domainMin) / (domainMax - domainMin) * size.width).toFloat()
                            abs(x - start.x)
                        }
                    },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                ) { change, _ ->
                    change.consume()
                    val index = active ?: return@detectDragGestures
                    val value = domainMin + change.position.x.coerceIn(0f, size.width.toFloat()) / size.width * (domainMax - domainMin)
                    onDragValue(index, value)
                }
            }
            .semantics {
                contentDescription = "Draggable dot plot with ${values.size} observations. Mean ${trimPhase(summary.mean)} and median ${trimPhase(summary.median)}."
            },
    ) {
        val left = 18f
        val right = size.width - 18f
        val bottom = size.height - 32f
        val plotWidth = right - left
        fun x(value: Double) = left + ((value - domainMin) / (domainMax - domainMin) * plotWidth).toFloat()
        repeat(5) { index ->
            val gridX = left + plotWidth * index / 4f
            drawLine(PhaseMuted.copy(.13f), Offset(gridX, 14f), Offset(gridX, bottom), 1f)
        }
        drawLine(PhaseMuted.copy(.6f), Offset(left, bottom), Offset(right, bottom), 2f)
        drawLine(PhaseGreen.copy(.8f), Offset(x(summary.mean), 16f), Offset(x(summary.mean), bottom), 3f)
        drawLine(PhasePurple.copy(.8f), Offset(x(summary.median), 16f), Offset(x(summary.median), bottom), 3f)
        val stacks = mutableMapOf<Int, Int>()
        values.forEachIndexed { index, value ->
            val bucket = (x(value) / 14f).toInt()
            val level = stacks.getOrDefault(bucket, 0)
            stacks[bucket] = level + 1
            val center = Offset(x(value), bottom - 16f - level * 18f)
            drawCircle(if (index == active) PhaseAmber else if (index == selectedPoint) PhasePink else PhaseCyan, if (index == active) 10f else 8f, center)
            drawCircle(Color.White.copy(.55f), if (index == active) 10f else 8f, center, style = Stroke(1.5f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("MEAN ${trimPhase(summary.mean)}", color = PhaseGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("Drag points horizontally", color = PhaseMuted, fontSize = 8.sp)
        Text("MEDIAN ${trimPhase(summary.median)}", color = PhasePurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NativeHistogram(bins: List<HistogramBin>) {
    var selectedBin by remember(bins) { mutableIntStateOf(-1) }
    Canvas(
        Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.22f))
            .pointerInput(bins) {
                detectTapGestures { tap ->
                    if (bins.isNotEmpty()) {
                        val usable = (size.width - 34f).coerceAtLeast(1f)
                        selectedBin = (((tap.x - 22f) / usable) * bins.size).toInt().coerceIn(0, bins.lastIndex)
                    }
                }
            }
            .semantics { contentDescription = ProbabilityPhase1DatasetEngine.accessibleHistogramSummary(bins) },
    ) {
        if (bins.isEmpty()) return@Canvas
        val left = 22f
        val right = size.width - 12f
        val top = 18f
        val bottom = size.height - 26f
        val maximum = bins.maxOf { it.count }.coerceAtLeast(1)
        val width = (right - left) / bins.size
        drawLine(PhaseMuted.copy(.6f), Offset(left, bottom), Offset(right, bottom), 2f)
        bins.forEachIndexed { index, bin ->
            val height = (bottom - top) * bin.count / maximum
            val x = left + index * width
            val color = if (index == selectedBin) PhaseAmber else if (index % 2 == 0) PhaseCyan else PhasePurple
            drawRect(color.copy(.55f), Offset(x + 1, bottom - height), Size((width - 2).coerceAtLeast(1f), height))
            drawRect(color, Offset(x + 1, bottom - height), Size((width - 2).coerceAtLeast(1f), height), style = Stroke(1.4f))
        }
    }
    bins.getOrNull(selectedBin)?.let { bin ->
        Text(
            "${trimPhase(bin.lower)} to ${trimPhase(bin.upper)} · frequency ${bin.count} · ${(bin.relativeFrequency * 100).toInt()}%",
            color = PhaseAmber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NativeBoxPlot(values: List<Double>, summary: DescriptiveStatistics) {
    Canvas(
        Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.22f))
            .semantics {
                contentDescription = "Box plot. Minimum ${trimPhase(summary.fiveNumber.minimum)}, first quartile ${trimPhase(summary.fiveNumber.firstQuartile)}, median ${trimPhase(summary.median)}, third quartile ${trimPhase(summary.fiveNumber.thirdQuartile)}, maximum ${trimPhase(summary.fiveNumber.maximum)}. ${summary.outliers.size} outliers."
            },
    ) {
        val minValue = min(values.minOrNull() ?: 0.0, summary.lowerFence)
        val maxValue = max(values.maxOrNull() ?: 1.0, summary.upperFence)
        val spread = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val left = 24f
        val right = size.width - 24f
        val y = size.height * .5f
        fun x(value: Double) = left + ((value - minValue) / spread * (right - left)).toFloat()
        val lowWhisker = values.filter { it >= summary.lowerFence }.minOrNull() ?: summary.fiveNumber.minimum
        val highWhisker = values.filter { it <= summary.upperFence }.maxOrNull() ?: summary.fiveNumber.maximum
        drawLine(PhaseCyan, Offset(x(lowWhisker), y), Offset(x(highWhisker), y), 4f)
        drawLine(PhaseCyan, Offset(x(lowWhisker), y - 28), Offset(x(lowWhisker), y + 28), 4f)
        drawLine(PhaseCyan, Offset(x(highWhisker), y - 28), Offset(x(highWhisker), y + 28), 4f)
        drawRect(
            PhasePurple.copy(.3f),
            Offset(x(summary.fiveNumber.firstQuartile), y - 48),
            Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 96f),
        )
        drawRect(
            PhasePurple,
            Offset(x(summary.fiveNumber.firstQuartile), y - 48),
            Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 96f),
            style = Stroke(3f),
        )
        drawLine(PhaseAmber, Offset(x(summary.median), y - 48), Offset(x(summary.median), y + 48), 5f)
        drawCircle(PhaseGreen, 8f, Offset(x(summary.mean), y))
        summary.outliers.forEach { drawCircle(PhasePink, 8f, Offset(x(it), y)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("■ median", color = PhaseAmber, fontSize = 9.sp)
        Text("● mean", color = PhaseGreen, fontSize = 9.sp)
        Text("● outlier", color = PhasePink, fontSize = 9.sp)
    }
    Text(
        "Min ${trimPhase(summary.fiveNumber.minimum)} · Q1 ${trimPhase(summary.fiveNumber.firstQuartile)} · Median ${trimPhase(summary.median)} · Q3 ${trimPhase(summary.fiveNumber.thirdQuartile)} · Max ${trimPhase(summary.fiveNumber.maximum)}",
        color = PhaseMuted,
        fontSize = 9.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyPhaseState(message: String) {
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(.18f))
            .border(1.dp, PhaseAmber.copy(.35f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(message, color = PhaseAmber, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp)) }
}

private fun trimPhase(value: Double): String {
    if (!value.isFinite()) return "n/a"
    val rounded = kotlin.math.round(value * 1000.0) / 1000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
