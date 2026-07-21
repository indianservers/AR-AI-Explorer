package com.indianservers.aiexplorer.phase3.mathlearning

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import com.indianservers.aiexplorer.learningintelligence.model.LearnerConfidence
import com.indianservers.aiexplorer.workspace.WorkspaceState
import kotlinx.coroutines.launch

private val MathConcepts = listOf(
    "math-linear-equations" to "Equations",
    "math-coordinate-geometry" to "Coordinates",
    "math-triangles" to "Triangles",
    "math-trigonometric-graphs" to "Trig graphs",
    "math-fractions" to "Fractions",
    "math-probability" to "Probability",
)

@Composable
fun AdaptiveMathLearningScreen(workspace: WorkspaceState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { AdaptiveMathLearningStore(context) }
    var engine by remember { mutableStateOf<AdaptiveMathLearningEngine?>(null) }
    var session by remember { mutableStateOf<AdaptiveMathLearningSession?>(null) }
    var answer by remember { mutableStateOf("") }
    var reasoning by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(LearnerConfidence.FAIRLY_SURE) }
    var seed by remember { mutableIntStateOf(1) }

    LaunchedEffect(workspace.id) {
        val loaded = runCatching { store.load() }.getOrElse { com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService() }
        engine = AdaptiveMathLearningEngine(loaded)
        session = engine?.start(workspace)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (session == null || engine == null) {
            LearningSkeleton(onBack)
        } else {
            val active = session!!
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Column(Modifier.weight(1f)) {
                        Text("Maths > Adaptive Coach", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Learn from the maths you are building", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    AssistChip(onClick = {}, label = { Text(active.learnerState.masteryState.name.replace('_', ' ')) })
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f))) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Live workspace evidence", fontWeight = FontWeight.SemiBold)
                                Text(active.workspaceEvidence.summary)
                                Text("Module: ${active.workspaceEvidence.module}. This is context, not mastery evidence, until you answer or interact.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        Text("Choose focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MathConcepts.forEach { (id, label) -> FilterChip(selected = active.conceptId == id, onClick = {
                                session = engine!!.start(workspace, id, ++seed); answer = ""; reasoning = ""
                            }, label = { Text(label) }) }
                        }
                    }
                    item {
                        Card {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Verified adaptive task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(active.task.prompt)
                                Text("Checks: ${active.task.invariants.joinToString()} • ${active.task.provenance}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(answer, { answer = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(reasoning, { reasoning = it }, label = { Text("Reasoning steps — one per line; optional reason after ::") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                                Text("How sure are you?", style = MaterialTheme.typography.labelLarge)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LearnerConfidence.entries.forEach { value -> FilterChip(confidence == value, { confidence = value }, { Text(value.name.replace('_', ' ').lowercase()) }) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val steps = parseSteps(reasoning)
                                        session = engine!!.submit(active, answer, steps, confidence)
                                        scope.launch { store.save(engine!!.service) }
                                    }, enabled = answer.isNotBlank()) { Text("Check reasoning") }
                                    OutlinedButton(onClick = { session = engine!!.requestHint(active); scope.launch { store.save(engine!!.service) } }) { Text("Socratic hint") }
                                    OutlinedButton(onClick = { session = engine!!.recordWorkspaceInteraction(active); scope.launch { store.save(engine!!.service) } }) { Text("I explored it") }
                                }
                            }
                        }
                    }
                    active.validation?.let { result -> item { FeedbackCard(active, result.valid) } }
                    active.hint?.let { selection -> item {
                        LearningPanel("Hint ${selection.hint.level.name.replace('_', ' ')}", selection.hint.text, "Why now: ${selection.reason}", MaterialTheme.colorScheme.secondaryContainer)
                    } }
                    active.tutorPrompt?.let { prompt -> item {
                        LearningPanel("Tutor asks — ${prompt.state.name.replace('_', ' ')}", prompt.text, "Grounded in: ${prompt.referenceIds.joinToString().ifBlank { "reviewed local content" }}", MaterialTheme.colorScheme.tertiaryContainer)
                    } }
                    active.repairPlan?.let { plan -> item {
                        LearningPanel("5-minute prerequisite bridge", plan.reason, "Return is preserved: ${plan.returnState.conceptId} / ${plan.returnState.activityId}", MaterialTheme.colorScheme.errorContainer.copy(alpha = .55f))
                    } }
                    item {
                        Card {
                            Column(Modifier.padding(14.dp)) {
                                Text("Evidence, not points", fontWeight = FontWeight.Bold)
                                val accuracy = active.learnerState.rollingAccuracy ?: 0.0
                                LinearProgressIndicator(progress = { accuracy.toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                                Text("${active.learnerState.practiceAttemptCount} practice attempt(s) • ${(accuracy * 100).toInt()}% rolling accuracy • ${active.learnerState.evidenceQuality.name.lowercase()} evidence")
                                active.recommendation?.let { Text("Next: ${it.explanation}", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    item {
                        Button(onClick = { session = engine!!.start(workspace, active.conceptId, ++seed); answer = ""; reasoning = "" }, modifier = Modifier.fillMaxWidth()) { Text("Next verified task") }
                    }
                }
            }
        }
    }
}

@Composable private fun FeedbackCard(session: AdaptiveMathLearningSession, correct: Boolean) {
    val color = if (correct) Color(0xFFDDF5E3) else MaterialTheme.colorScheme.errorContainer
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(if (correct) "Verified — relationship preserved" else "Checkpoint needs repair", fontWeight = FontWeight.Bold)
            session.proofEvaluation?.checkpointFeedback?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Invariant: ${session.task.invariants.joinToString()} • ${if (correct) "preserved" else "recheck the first invalid transformation"}")
            session.errorEntry?.let { Text("Saved privately to the error book for a targeted retry.", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable private fun LearningPanel(title: String, body: String, note: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body); Spacer(Modifier.height(4.dp)); Text(note, style = MaterialTheme.typography.labelSmall) } }
}

@Composable private fun LearningSkeleton(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Preparing your adaptive maths session…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        repeat(4) { Box(Modifier.fillMaxWidth().height(if (it == 1) 190.dp else 72.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))) }
    }
}

private fun parseSteps(raw: String): List<LearnerAnswerStep> = raw.lineSequence().map(String::trim).filter(String::isNotBlank).map { line ->
    val pieces = line.split("::", limit = 2)
    LearnerAnswerStep(pieces[0].trim(), pieces.getOrNull(1)?.trim()?.takeIf(String::isNotBlank))
}.toList()
