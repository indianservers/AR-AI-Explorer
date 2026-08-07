package com.indianservers.aiexplorer.features.probabilitystatistics.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.features.probabilitystatistics.data.ProbabilityStatisticsCatalog
import com.indianservers.aiexplorer.adaptive.LocalAdaptiveDeviceProfile
import com.indianservers.aiexplorer.features.probabilitystatistics.models.MasteryState
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsCategory
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsLearningLevel
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsTopic
import com.indianservers.aiexplorer.features.probabilitystatistics.models.TopicProgress
import com.indianservers.aiexplorer.features.probabilitystatistics.models.TopicWorkspaceMode

internal val PsSpace = Color(0xFF040914)
internal val PsPanel = Color(0xF00B1728)
internal val PsPanelSoft = Color(0xFF101F34)
internal val PsInk = Color(0xFFF5F8FF)
internal val PsMuted = Color(0xFFA8B6CC)
internal val PsCyan = Color(0xFF29D8F0)
internal val PsGreen = Color(0xFF4CDEA4)
internal val PsAmber = Color(0xFFFFBE4A)
internal val PsCoral = Color(0xFFFF765F)
internal val PsViolet = Color(0xFF9A83FF)
internal val PsRed = Color(0xFFFF637D)

@Composable
internal fun ProbabilityStatisticsScreen(
    onExit: () -> Unit,
    onOpenClassicLabs: () -> Unit,
    model: ProbabilityStatisticsViewModel = viewModel(),
) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val state by model.state.collectAsState()
    BackHandler {
        if (state.route == ProbabilityStatisticsRoute.Home) onExit() else model.back()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF061222), PsSpace, Color(0xFF071018)),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = adaptiveProfile.navigationPolicy.maximumContentWidth)
                .fillMaxSize(),
        ) {
            when (state.route) {
                ProbabilityStatisticsRoute.Home -> StatisticsHome(state, model, onExit, onOpenClassicLabs)
                ProbabilityStatisticsRoute.Category -> StatisticsCategoryScreen(state, model)
                ProbabilityStatisticsRoute.Topic -> StatisticsTopicScreen(state, model)
                ProbabilityStatisticsRoute.DistributionExplorer -> DistributionExplorerScreen(model)
                ProbabilityStatisticsRoute.DatasetLab -> DatasetLabScreen(model)
                ProbabilityStatisticsRoute.TestGuide -> TestGuideScreen(model)
                ProbabilityStatisticsRoute.FormulaLibrary -> FormulaLibraryScreen(model)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatisticsHome(
    state: ProbabilityStatisticsUiState,
    model: ProbabilityStatisticsViewModel,
    onExit: () -> Unit,
    onOpenClassicLabs: () -> Unit,
) {
    val searchResults = ProbabilityStatisticsCatalog.search(state.query, state.level)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            FeatureTopBar("Probability & Statistics Lab", "Data, uncertainty and decisions", onExit)
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = model::search,
                label = { Text("Search topics, formulas or uses") },
                leadingIcon = { Text("⌕", color = PsCyan, fontSize = 22.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search probability and statistics topics" },
            )
        }
        item { LevelStrip(state.level, model::setLevel) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMetric("Progress", "${model.overallProgress()}%", PsGreen, Modifier.weight(1f))
                CompactMetric("Level", state.level.label, PsViolet, Modifier.weight(1.5f))
                CompactMetric("Offline", "Ready", PsCyan, Modifier.weight(1f))
            }
        }
        if (state.query.isNotBlank()) {
            item { SectionTitle("${searchResults.size} matching topics") }
            items(searchResults, key = { it.id }) { topic ->
                TopicRow(topic, state.progress[topic.id], onClick = { model.openTopic(topic.id) })
            }
        } else {
            item {
                ContinueLearningCard(state, model)
            }
            item { SectionTitle("Quick labs") }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickLabButton("ƒ", "Distributions", PsViolet) { model.open(ProbabilityStatisticsRoute.DistributionExplorer) }
                    QuickLabButton("▦", "Dataset Lab", PsGreen) { model.open(ProbabilityStatisticsRoute.DatasetLab) }
                    QuickLabButton("?", "Test Guide", PsAmber) { model.open(ProbabilityStatisticsRoute.TestGuide) }
                    QuickLabButton("Σ", "Formulas", PsCyan) { model.open(ProbabilityStatisticsRoute.FormulaLibrary) }
                    QuickLabButton("⚗", "Advanced Labs", PsCoral, onOpenClassicLabs)
                }
            }
            item { SectionTitle("Learning universe") }
            items(ProbabilityStatisticsCatalog.categories, key = { it.id }) { category ->
                CategoryCard(category, state, model)
            }
        }
    }
}

@Composable
private fun ContinueLearningCard(state: ProbabilityStatisticsUiState, model: ProbabilityStatisticsViewModel) {
    val topicId = state.recentlyExplored.firstOrNull() ?: "mean-median-mode"
    val topic = ProbabilityStatisticsCatalog.topic(topicId) ?: return
    val progress = state.progress[topicId] ?: TopicProgress(topicId)
    FeaturePanel(accent = PsGreen, modifier = Modifier.fillMaxWidth().clickable { model.openTopic(topicId) }) {
        Text(if (state.recentlyExplored.isEmpty()) "START HERE" else "CONTINUE LEARNING", color = PsGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(topic.title, color = PsInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(topic.shortDescription, color = PsMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = PsGreen, trackColor = PsPanelSoft)
    }
}

@Composable
private fun CategoryCard(category: StatisticsCategory, state: ProbabilityStatisticsUiState, model: ProbabilityStatisticsViewModel) {
    val topicProgress = category.topicIds.mapNotNull { state.progress[it] }
    val percent = if (topicProgress.isEmpty()) 0 else topicProgress.sumOf { it.percent } / topicProgress.size
    FeaturePanel(
        accent = categoryColor(category.id),
        modifier = Modifier.fillMaxWidth().clickable { model.openCategory(category.id) }.semantics {
            contentDescription = "${category.title}, ${category.topicIds.size} learning paths, $percent percent complete"
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(categoryColor(category.id).copy(alpha = .14f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(category.icon, color = categoryColor(category.id), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(category.title, color = PsInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(category.description, color = PsMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(if (percent > 0) "$percent%" else "Start", color = categoryColor(category.id), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatisticsCategoryScreen(state: ProbabilityStatisticsUiState, model: ProbabilityStatisticsViewModel) {
    val category = state.categoryId?.let(ProbabilityStatisticsCatalog::category) ?: return
    val topics = category.topicIds.mapNotNull(ProbabilityStatisticsCatalog::topic)
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { FeatureTopBar(category.title, category.description, model::back) }
        item {
            Text(
                "Choose a topic, then move between Learn, Explore, Simulate, Practice and Assessment without losing your place.",
                color = PsMuted,
                fontSize = 13.sp,
            )
        }
        items(topics, key = { it.id }) { topic ->
            TopicRow(topic, state.progress[topic.id], onClick = { model.openTopic(topic.id) })
        }
        if (topics.isEmpty()) {
            item { EmptyState("This level has no unlocked topics. Choose a higher learning level from the home screen.") }
        }
    }
}

@Composable
private fun TopicRow(topic: StatisticsTopic, progress: TopicProgress?, onClick: () -> Unit) {
    FeaturePanel(PsCyan, Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(topic.title, color = PsInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(topic.shortDescription, color = PsMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${topic.estimatedMinutes} min · ${topic.difficulty.name} · ${progress?.mastery?.displayName() ?: "Not started"}", color = PsCyan, fontSize = 11.sp)
            }
            Text("›", color = PsCyan, fontSize = 28.sp)
        }
    }
}

@Composable
internal fun FeatureTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SmallAction("‹", "Back", onBack)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PsInk, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(subtitle, color = PsMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun FeaturePanel(accent: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(PsPanel, RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text.uppercase(), color = PsAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
}

@Composable
internal fun SmallAction(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(PsPanelSoft, CircleShape)
            .border(1.dp, PsCyan.copy(alpha = .45f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = PsCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PrimaryAction(label: String, accent: Color = PsCyan, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = .2f), contentColor = accent),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    }
}

@Composable
private fun CompactMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    FeaturePanel(accent, modifier) {
        Text(label, color = PsMuted, fontSize = 10.sp)
        Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickLabButton(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .height(76.dp)
            .background(PsPanel, RoundedCornerShape(7.dp))
            .border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(icon, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PsInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

@Composable
private fun LevelStrip(selected: StatisticsLearningLevel, onSelect: (StatisticsLearningLevel) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatisticsLearningLevel.entries.forEach { level ->
            PrimaryAction(if (level == selected) "• ${level.label}" else level.label, if (level == selected) PsViolet else PsMuted) { onSelect(level) }
        }
    }
}

@Composable
internal fun EmptyState(message: String) {
    FeaturePanel(PsAmber, Modifier.fillMaxWidth()) {
        Text(message, color = PsMuted, fontSize = 13.sp)
    }
}

internal fun categoryColor(id: String): Color = when (id.hashCode().mod(5)) {
    0 -> PsCyan
    1 -> PsGreen
    2 -> PsAmber
    3 -> PsCoral
    else -> PsViolet
}

private fun MasteryState.displayName(): String = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
