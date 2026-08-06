package com.indianservers.aiexplorer.features.numbertheory.visualproofs.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.data.NumberTheoryVisualProofCatalog
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofMode
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryWorkspaceSection
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualProofTopic
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.rendering.NumberTheoryProofCanvas
import kotlinx.coroutines.delay

internal val ProofSpace = Color(0xFF030914)
internal val ProofPanel = Color(0xFF0B1727)
internal val ProofPanelSoft = Color(0xFF102339)
internal val ProofInk = Color(0xFFF4F7FC)
internal val ProofMuted = Color(0xFF9EB0C9)
internal val ProofCyan = Color(0xFF31D8EA)
internal val ProofGreen = Color(0xFF4DE3A7)
internal val ProofAmber = Color(0xFFFFBE4D)
internal val ProofCoral = Color(0xFFFF765E)

@Composable
internal fun NumberTheoryVisualProofScreen(
    onExit: () -> Unit,
    model: NumberTheoryVisualProofViewModel = viewModel(),
) {
    val ui by model.state.collectAsState()
    BackHandler {
        if (ui.route == NumberTheoryProofRoute.Home) onExit() else model.back()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071425), ProofSpace, Color.Black))),
    ) {
        when (ui.route) {
            NumberTheoryProofRoute.Home -> ProofHome(ui, model, onExit)
            NumberTheoryProofRoute.Category -> ProofCategory(ui, model)
            NumberTheoryProofRoute.Workspace -> ProofWorkspace(ui, model)
        }
    }
}

@Composable
private fun ProofHome(
    ui: NumberTheoryVisualProofUiState,
    model: NumberTheoryVisualProofViewModel,
    onExit: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProofHeader("Number Theory", "VISUAL PROOFS", onExit)
        Text("See the reason, not just the rule.", color = ProofInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "Build, rearrange, predict, and explain number patterns using exact values.",
            color = ProofMuted,
            fontSize = 11.sp,
        )
        OutlinedTextField(
            value = ui.query,
            onValueChange = model::search,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search Number Theory visual proofs" },
            placeholder = { Text("Search a proof, idea, or formula") },
            singleLine = true,
        )
        ScrollStrip {
            NumberTheoryProofLevel.entries.forEach { level ->
                ProofChip(level.label, ui.level == level, ProofCyan) { model.setLevel(level) }
            }
        }
        val available = NumberTheoryVisualProofCatalog.completedTopics
        LinearProgressIndicator(
            progress = { ui.completed.size.toFloat() / available.size },
            modifier = Modifier.fillMaxWidth(),
            color = ProofGreen,
            trackColor = ProofPanelSoft,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${ui.completed.size} of ${available.size} explored", color = ProofMuted, fontSize = 10.sp)
            ProofChip("Surprise me", false, ProofAmber, model::surprise)
        }

        val searchResults = remember(ui.query, ui.level) { NumberTheoryVisualProofCatalog.search(ui.query, ui.level) }
        if (ui.query.isNotBlank()) {
            Text("RESULTS", color = ProofCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            searchResults.forEach { topic -> TopicRow(topic, topic.id in ui.completed) { model.openTopic(topic.id) } }
        }

        ui.recent.firstOrNull()?.let { recentId ->
            NumberTheoryVisualProofCatalog.topic(recentId)?.let { topic ->
                Text("CONTINUE", color = ProofGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                TopicRow(topic, topic.id in ui.completed) { model.openTopic(topic.id) }
            }
        }
        val daily = available[java.time.LocalDate.now().dayOfYear.mod(available.size)]
        Text("DAILY NUMBER CHALLENGE", color = ProofAmber, fontSize = 10.sp, fontWeight = FontWeight.Black)
        TopicRow(daily, daily.id in ui.completed) { model.openTopic(daily.id) }
        if (ui.saved.isNotEmpty()) {
            Text("SAVED DISCOVERIES", color = ProofGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
            ui.saved.take(3).mapNotNull(NumberTheoryVisualProofCatalog::topic).forEach { topic ->
                TopicRow(topic, topic.id in ui.completed) { model.openTopic(topic.id) }
            }
        }
        Text("CHOOSE A PATH", color = ProofCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
        NumberTheoryProofCategory.entries.forEach { category ->
            val all = NumberTheoryVisualProofCatalog.topicsFor(category, includeRoadmap = true)
            val ready = all.count { it.completedInPhase <= 3 }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(ProofPanel, RoundedCornerShape(8.dp))
                    .border(1.dp, ProofCyan.copy(alpha = .28f), RoundedCornerShape(8.dp))
                    .clickable { model.openCategory(category) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(category.icon, color = ProofAmber, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Column(Modifier.weight(1f)) {
                    Text(category.title, color = ProofInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(category.description, color = ProofMuted, fontSize = 9.sp, maxLines = 2)
                    val readyTopics = all.filter { it.completedInPhase <= 3 }
                    if (readyTopics.isNotEmpty()) {
                        val range = "${readyTopics.minOf { it.level.ordinal }.let { NumberTheoryProofLevel.entries[it].label }} to ${readyTopics.maxOf { it.level.ordinal }.let { NumberTheoryProofLevel.entries[it].label }}"
                        Text("${readyTopics.count { it.id in ui.completed }}/$ready completed | $range", color = ProofMuted, fontSize = 8.sp, maxLines = 1)
                    }
                }
                Text("$ready ready / ${all.size}", color = ProofGreen, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProofCategory(ui: NumberTheoryVisualProofUiState, model: NumberTheoryVisualProofViewModel) {
    val category = ui.category ?: return
    val topics = NumberTheoryVisualProofCatalog.topicsFor(category, includeRoadmap = true)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProofHeader(category.title, "${topics.count { it.completedInPhase == 1 }} READY") { model.back() }
        Text(category.description, color = ProofMuted, fontSize = 11.sp)
        topics.filter { it.completedInPhase <= 3 }.forEach { topic ->
            TopicRow(topic, topic.id in ui.completed) { model.openTopic(topic.id) }
        }
        val roadmap = topics.filter { it.completedInPhase > 3 }
        if (roadmap.isNotEmpty()) {
            Text("COMING IN PHASES 2 AND 3", color = ProofAmber, fontSize = 10.sp, fontWeight = FontWeight.Black)
            roadmap.forEach { topic ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(topic.title, color = ProofMuted, fontSize = 11.sp)
                    Text("Phase ${topic.completedInPhase}", color = ProofAmber, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: NumberTheoryVisualProofTopic, completed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ProofPanel, RoundedCornerShape(8.dp))
            .border(1.dp, if (completed) ProofGreen.copy(.55f) else ProofCyan.copy(.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open visual proof ${topic.title}" }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (completed) "OK" else topic.category.icon, color = if (completed) ProofGreen else ProofCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Column(Modifier.weight(1f)) {
            Text(topic.title, color = ProofInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(topic.statement, color = ProofMuted, fontSize = 10.sp, maxLines = 2)
        }
        Text("OPEN", color = ProofAmber, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun ProofHeader(title: String, status: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ProofAction("<", "Back", onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(title, color = ProofInk, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Text(status, color = ProofCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ScrollStrip(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

@Composable
internal fun ProofChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 38.dp)
            .background(if (selected) accent.copy(.24f) else ProofPanelSoft, RoundedCornerShape(7.dp))
            .border(1.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) accent else ProofInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ProofAction(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    accent: Color = ProofCyan,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .background(accent.copy(.12f), RoundedCornerShape(7.dp))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text.uppercase(), color = ProofAmber, fontSize = 10.sp, fontWeight = FontWeight.Black)
}
