package com.indianservers.aiexplorer.curriculum.interaction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.round

/** Route-ready 2D renderer. It deliberately has no subject rules and creates no per-frame history. */
@Composable
fun InteractiveActivityScreen(activityId: String, focusTargetId: String? = null, presetRequestId: String? = null, presetRequestNonce: Int = 0, embedded: Boolean = false, onProgress: (ActivityRunSnapshot) -> Unit = {}) {
    val engine = remember(activityId) { ReferenceActivityRegistry.create(activityId) }
    var run by remember(activityId) { mutableStateOf(engine.snapshot) }
    var prediction by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }
    fun sync() { run = engine.snapshot; onProgress(run) }
    LaunchedEffect(focusTargetId) { engine.focus(focusTargetId); sync() }
    LaunchedEffect(presetRequestId, presetRequestNonce) { presetRequestId?.let { engine.applyPreset(it); sync() } }
    val container = if (embedded) Modifier.fillMaxWidth().padding(16.dp) else Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    Column(container, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(engine.definition.title, style = MaterialTheme.typography.headlineSmall)
        Text("Objective: ${engine.definition.learningObjectives.joinToString()}")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            engine.definition.supportedModes.sortedBy { it.ordinal }.forEachIndexed { index, mode ->
                SegmentedButton(selected = run.mode == mode, onClick = { engine.setMode(mode); sync() }, shape = SegmentedButtonDefaults.itemShape(index, engine.definition.supportedModes.size), label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        if (run.mode == ActivityMode.GUIDED) engine.definition.guidedInstructions.forEachIndexed { i, instruction -> Text("${i + 1}. $instruction") }
        OutlinedTextField(prediction, { prediction = it }, Modifier.fillMaxWidth(), label = { Text("Prediction") }, supportingText = { Text("Stored separately from final challenge accuracy") })
        Button(onClick = { engine.predict(prediction); sync() }, enabled = prediction.isNotBlank()) { Text("Lock prediction") }
        ModelCanvas(run.visual)
        run.focusTargetId?.let { target ->
            val name = engine.definition.diagramParts.firstOrNull { it.id == target }?.let { "${it.label}: ${it.description}" }
                ?: engine.definition.controls.firstOrNull { it.id == target }?.let { "${it.label}, current value ${run.controls[target]} ${it.unit}" }
                ?: target
            Text("Focused visual quantity: $name", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = "Focused visual quantity. $name" })
        }
        Text(run.visual.explanation)
        Text("Connection: ${engine.definition.formulaOrProcess}", style = MaterialTheme.typography.titleMedium)
        run.visual.labels.forEach { (name, value) -> Text("$name: $value") }
        engine.definition.controls.forEach { control ->
            val value = run.controls.getValue(control.id)
            Column(Modifier.semantics { contentDescription = "${control.spokenLabel}; current value $value" }) {
                Text("${control.label}: ${"%.2f".format(value)} ${control.unit}")
                Slider(value.toFloat(), { raw -> val snapped = round(raw / control.step) * control.step; engine.setControl(control.id, snapped); sync() }, valueRange = control.minimum.toFloat()..control.maximum.toFloat())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            engine.definition.presets.take(2).forEach { preset -> OutlinedButton(onClick = { engine.applyPreset(preset.id); sync() }) { Text(preset.title) } }
            Button(onClick = { engine.reset(); prediction = ""; observation = ""; sync() }) { Text("Reset") }
        }
        if (run.mode == ActivityMode.CHALLENGE) engine.definition.challenges.forEach { challenge ->
            OutlinedButton(onClick = { engine.checkChallenge(challenge.id); sync() }, modifier = Modifier.fillMaxWidth()) { Text(if (challenge.id in run.completedChallengeIds) "✓ ${challenge.title}" else challenge.instruction) }
        }
        OutlinedTextField(observation, { observation = it }, Modifier.fillMaxWidth(), label = { Text("What did you observe?") })
        Button(onClick = { engine.observe(observation); sync() }, enabled = observation.isNotBlank()) { Text("Record observation") }
        Text(if (run.completed) "Activity complete: interaction, prediction, observation, and challenge evidence recorded." else "Checkpoint: manipulate values, lock a prediction, record an observation, and pass a challenge.")
        Text("Text alternative: ${run.visual.textAlternative}", modifier = Modifier.semantics { contentDescription = run.visual.textAlternative })
    }
}

@Composable
private fun ModelCanvas(visual: VisualState) {
    Canvas(Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = visual.textAlternative }) {
        if (visual.points.isEmpty()) return@Canvas
        val finite=visual.points.filter{it.x.isFinite()&&it.y.isFinite()};if(finite.isEmpty())return@Canvas
        val minX=finite.minOf{it.x};val maxX=finite.maxOf{it.x}.let{if(it==minX)it+1 else it};val minY=finite.minOf{it.y};val maxY=finite.maxOf{it.y}.let{if(it==minY)it+1 else it}
        fun point(p:PlotPoint)=Offset(((p.x-minX)/(maxX-minX)*size.width).toFloat(),(size.height-(p.y-minY)/(maxY-minY)*size.height).toFloat())
        val colors=listOf(Color(0xFF1565C0),Color(0xFFE65100),Color(0xFF2E7D32),Color(0xFF6A1B9A));val groups=finite.groupBy{it.series}
        groups.entries.forEachIndexed{index,entry->val pts=entry.value;for(i in 1 until pts.size)drawLine(colors[index%colors.size],point(pts[i-1]),point(pts[i]),strokeWidth=5f);pts.forEach{drawCircle(colors[index%colors.size],7f,point(it))}}
        if (visual.highlightedPartId != null) drawRect(Color(0xFFFFA000), style = Stroke(width = 8f))
    }
}
