package com.indianservers.aiexplorer.phase2.mathstudio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.UnifiedConstructionEngine
import com.indianservers.aiexplorer.core.NextGenerationMathFeatureCatalog
import com.indianservers.aiexplorer.core.NextMathDomain
import com.indianservers.aiexplorer.core.CurveSculptCandidate
import com.indianservers.aiexplorer.core.CurveSculptEngine
import com.indianservers.aiexplorer.core.CurveSculptPreview
import com.indianservers.aiexplorer.workspace.MathGraphObject
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.persistence.MathFileExchange
import com.indianservers.aiexplorer.input.IntentAwareMathField
import kotlinx.coroutines.launch

private val StudioBackground = Color(0xFF03070C)
private val StudioSurface = Color(0xEE09131F)
private val StudioInk = Color(0xFFEAF5FF)
private val StudioMuted = Color(0xFF9EB0C8)
private val StudioCyan = Color(0xFF20D9FF)
private val StudioViolet = Color(0xFF985DFF)
private val StudioGreen = Color(0xFF48E0A4)
private val StudioAmber = Color(0xFFFFC857)

@Composable
fun UnifiedMathStudioScreen(
    initialWorkspace: WorkspaceState,
    onWorkspaceChange: (WorkspaceState) -> Unit,
    onBack: () -> Unit,
) {
    val engine = remember { UnifiedMathStudioEngine() }
    val shared = remember { SharedExperienceEngine() }
    val history = remember(initialWorkspace.id) { SharedStudioHistory(engine.fromWorkspace(initialWorkspace)) }
    val recorder = remember(initialWorkspace.id) { StudioSessionRecorder() }
    var session by remember(initialWorkspace.id) { mutableStateOf(history.current) }
    var input by remember { mutableStateOf("h(x) = a*sin(x) + 1") }
    var lastRecording by remember { mutableStateOf<RecordedStudioSession?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    val constructionEngine = remember { UnifiedConstructionEngine() }
    var constructionInput by remember { mutableStateOf("point2d(A,0,0)") }
    var constructionStatus by remember { mutableStateOf("Graph, 2D and 3D commands share one dependency-aware palette.") }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val projection = remember(session.document.revision, session.parameterValues) { engine.projection(session) }
    fun apply(next: UnifiedStudioSession, label: String = next.message) {
        session = history.apply(next, label)
        if (isRecording) recorder.capture(label, session)
    }
    val importGeoGebra = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || activity == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { MathFileExchange.readGeoGebra(activity, uri, engine.toWorkspace(session)) }
                .onSuccess { result -> apply(engine.fromWorkspace(result.workspace).copy(message = "Imported ${result.coverage.imported} GeoGebra objects; ${result.coverage.skipped.size} unsupported objects reported."), "Import GeoGebra document") }
                .onFailure { apply(session.copy(message = "GeoGebra import failed: ${it.message ?: "invalid package"}"), "GeoGebra import failed") }
        }
    }

    LaunchedEffect(session.document.revision, session.parameterValues) { onWorkspaceChange(engine.toWorkspace(session)) }

    Column(
        Modifier.fillMaxSize().background(StudioBackground).padding(8.dp)
            .semantics { contentDescription = "Unified live mathematics studio" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onBack) { Text("Back") }
            Column(Modifier.weight(1f)) {
                Text("UNIFIED MATH STUDIO", color = StudioInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("One object → algebra · graph · table · geometry · solver", color = StudioMuted, fontSize = 11.sp)
            }
            Text("LIVE r${session.document.revision}", color = StudioGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            OutlinedButton({ session = history.undo() }, enabled = history.canUndo) { Text("Undo") }
            OutlinedButton({ session = history.redo() }, enabled = history.canRedo) { Text("Redo") }
            OutlinedButton({
                if (isRecording) lastRecording = recorder.stop()
                else recorder.start(session.baseWorkspace.name, session)
                isRecording = !isRecording
            }) { Text(if (isRecording) "Stop" else "Record") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SharedWorkspaceMode.entries.forEach { mode ->
                FilterChip(session.experience.mode == mode, { apply(shared.setMode(session, mode), "Switch to ${mode.label} mode") }, label = { Text(mode.label) })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("VIEWS", color = StudioCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            SharedMathView.entries.forEach { view ->
                FilterChip(view in session.experience.layout.activeViews, {
                    val next = if (view in session.experience.layout.activeViews && view != session.experience.layout.focusedView) shared.focusView(session, view) else shared.toggleView(session, view)
                    apply(next, if (view == session.experience.layout.focusedView) "Toggle ${view.label} view" else "Focus ${view.label} view")
                }, label = { Text(view.label) })
            }
            Text(if (isRecording) "● RECORDING" else lastRecording?.let { "REPLAY ${it.events.size} STEPS" } ?: "", color = if (isRecording) Color(0xFFFF6F7D) else StudioGreen, fontSize = 9.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(session.message, color = StudioMuted, fontSize = 10.sp, modifier = Modifier.widthIn(min = 220.dp, max = 600.dp))
            OutlinedButton({ importGeoGebra.launch(arrayOf("application/vnd.geogebra.file", "application/zip", "application/octet-stream")) }) { Text("Import .ggb") }
            OutlinedButton({
                if (activity != null) scope.launch { runCatching { MathFileExchange.shareGeoGebra(activity, engine.toWorkspace(session)) }.onFailure { apply(session.copy(message = "GeoGebra export failed: ${it.message}"), "GeoGebra export failed") } }
            }, enabled = activity != null) { Text("Export .ggb") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IntentAwareMathField(
                constructionInput, { constructionInput = it }, "Unified command",
                Modifier.weight(1f).semantics { contentDescription = "Graph geometry and 3D construction command" },
                placeholder = "midpoint(M,A,B) · surface(s,z=x^2+y^2)", showLegend = false,
            )
            Button({
                runCatching { engine.construct(session, constructionInput) }
                    .onSuccess { next -> apply(next, "Construct ${constructionInput.substringBefore('(')}"); constructionStatus = "Created ${constructionEngine.tokens(next.construction).last().accessibleLabel}" }
                    .onFailure { constructionStatus = it.message ?: "The command could not be completed." }
            }, enabled = constructionInput.isNotBlank()) { Text("Construct") }
            OutlinedButton({ session = history.undo(); constructionStatus = "Unified construction undone." }, enabled = history.canUndo) { Text("↶") }
            OutlinedButton({ session = history.redo(); constructionStatus = "Unified construction restored." }, enabled = history.canRedo) { Text("↷") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { contentDescription = constructionStatus }, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(constructionStatus, color = if (constructionStatus.startsWith("Created")) StudioGreen else StudioMuted, fontSize = 10.sp)
            constructionEngine.tokens(session.construction).forEach { token ->
                val id = "construction:${token.id}"
                FilterChip(id in session.experience.selection.canonicalIds, { apply(shared.select(session, id), "Select ${token.id}") }, label = { Text("${token.view.name} · ${token.id}") }, modifier = Modifier.semantics { contentDescription = token.accessibleLabel })
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 820.dp
            val views = session.experience.layout.activeViews
            if (!wide) {
                LinkedStudioPane(session.experience.layout.focusedView, session, projection, input, { input = it }, {
                    apply(engine.add(session, input), "Add linked expression"); input = ""
                }, { apply(it) }, engine, shared, history, lastRecording, Modifier.fillMaxSize())
            } else if (views.size <= 2) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    views.forEach { view -> LinkedStudioPane(view, session, projection, input, { input = it }, {
                        apply(engine.add(session, input), "Add linked expression"); input = ""
                    }, { apply(it) }, engine, shared, history, lastRecording, Modifier.weight(1f).fillMaxHeight()) }
                }
            } else {
                val rows = views.chunked(2)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowViews ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowViews.forEach { view -> LinkedStudioPane(view, session, projection, input, { input = it }, {
                                apply(engine.add(session, input), "Add linked expression"); input = ""
                            }, { apply(it) }, engine, shared, history, lastRecording, Modifier.weight(1f).fillMaxHeight()) }
                            if (rowViews.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedStudioPane(
    view: SharedMathView,
    session: UnifiedStudioSession,
    projection: StudioProjection,
    input: String,
    onInput: (String) -> Unit,
    onAdd: () -> Unit,
    onSession: (UnifiedStudioSession) -> Unit,
    engine: UnifiedMathStudioEngine,
    shared: SharedExperienceEngine,
    history: SharedStudioHistory,
    recording: RecordedStudioSession?,
    modifier: Modifier,
) {
    when (view) {
        SharedMathView.Algebra -> AlgebraPane(session, projection, input, onInput, onAdd, onSession, engine, modifier)
        SharedMathView.Graph -> GraphPane(
            projection.graph.graphObjects, projection.keyPoints, session.selectedId, { onSession(engine.select(session, it)) }, modifier,
            onSculptPreview = { x, y ->
                val selected = projection.expressions.firstOrNull { it.id == session.selectedId } ?: return@GraphPane null
                val parameters = projection.graph.parameterRows.associate { it.name to (session.parameterValues[it.name] ?: it.value) }
                    .filterKeys { it in selected.dependencies || Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(selected.source) }
                if (parameters.isEmpty()) null else runCatching { CurveSculptEngine().preview(selected.resolvedSource, parameters, x, y) }.getOrNull()
            },
            onSculptCommit = { candidate -> onSession(engine.parameter(session, candidate.parameter, candidate.newValue)) },
        )
        SharedMathView.Table -> TablePane(projection, modifier)
        SharedMathView.Cas -> ResultPane(session, projection.diagnostics, modifier)
        SharedMathView.Geometry2D -> LinkedObjectPane(view, session, shared, onSession, modifier)
        SharedMathView.Spatial3D -> LinkedObjectPane(view, session, shared, onSession, modifier)
        SharedMathView.Inspector -> ProvenancePane(session, shared, modifier)
        SharedMathView.Proof -> ProofPane(session, shared, modifier)
        SharedMathView.Timeline -> TimelinePane(history, recording, onSession, modifier)
        SharedMathView.Labs -> AdvancedLabsPane(modifier)
    }
}

@Composable
private fun AdvancedLabsPane(modifier: Modifier) {
    var domain by remember { mutableStateOf(NextMathDomain.Graph) }
    StudioCard(modifier, "ADVANCED INTERACTION LABS", "42 production engines; direct manipulation stays primary") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            NextMathDomain.entries.forEach { value -> FilterChip(domain == value, { domain = value }, label = { Text(value.name) }) }
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(NextGenerationMathFeatureCatalog.domain(domain), key = { it.id }) { feature ->
                Column(Modifier.fillMaxWidth().background(Color(0x44111C29), RoundedCornerShape(10.dp)).padding(8.dp)) {
                    Text(feature.title, color = StudioInk, fontWeight = FontWeight.Bold)
                    Text(feature.interactions.joinToString(" · ") { it.name.uppercase() }, color = StudioCyan, fontSize = 9.sp)
                    Text(feature.description, color = StudioMuted, fontSize = 10.sp)
                    Text(feature.engine, color = StudioGreen, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun LinkedObjectPane(view: SharedMathView, session: UnifiedStudioSession, shared: SharedExperienceEngine, onSession: (UnifiedStudioSession) -> Unit, modifier: Modifier) {
    val graph = remember(session.document.revision, session.resultPods.size) { SharedObjectGraphBuilder.build(session) }
    val objects = graph.nodes.values.filter { it.view == view && it.role != SharedNodeRole.Canonical }
    StudioCard(modifier, if (view == SharedMathView.Geometry2D) "LINKED 2D OBJECTS" else "LINKED 3D SCENE", "Selection and dependencies are shared with every view") {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(objects, key = { it.id }) { node ->
                val selected = node.canonicalId in session.experience.selection.canonicalIds
                Column(Modifier.fillMaxWidth().background(if (selected) StudioViolet.copy(.2f) else Color(0x44111C29), RoundedCornerShape(10.dp)).clickable { onSession(shared.select(session, node.id)) }.padding(8.dp)) {
                    Text(node.name, color = if (selected) StudioGreen else StudioInk, fontWeight = FontWeight.Bold)
                    Text(node.accessibleLabel, color = StudioMuted, fontSize = 10.sp)
                }
            }
            if (objects.isEmpty()) item { Text("Create an object in this view; it will immediately join the shared document.", color = StudioMuted) }
        }
    }
}

@Composable
private fun ProvenancePane(session: UnifiedStudioSession, shared: SharedExperienceEngine, modifier: Modifier) {
    val id = session.selectedId
    val provenance = id?.let { runCatching { shared.inspectProvenance(session, it) }.getOrNull() }
    StudioCard(modifier, "PROVENANCE INSPECTOR", "Definition, assumptions and downstream effects") {
        if (provenance == null) Text("Select any linked object to inspect its origin.", color = StudioMuted)
        else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(provenance.name, color = StudioInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("ORIGIN · ${provenance.sourceView}", color = StudioCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(provenance.explanation, color = StudioMuted)
            Text("Direct inputs: ${provenance.directDependencies.joinToString().ifBlank { "none" }}", color = StudioGreen)
            Text("Full chain: ${provenance.dependencyChain.joinToString(" → ").ifBlank { "independent" }}", color = StudioMuted)
            Text("Updates: ${provenance.dependentObjects.joinToString().ifBlank { "none" }}", color = StudioAmber)
            Text("Assumptions: ${provenance.assumptions.joinToString().ifBlank { "none" }}", color = StudioViolet)
        }
    }
}

@Composable
private fun ProofPane(session: UnifiedStudioSession, shared: SharedExperienceEngine, modifier: Modifier) {
    val provenance = session.selectedId?.let { runCatching { shared.inspectProvenance(session, it) }.getOrNull() }
    StudioCard(modifier, "DEPENDENCY PROOF", "Each arrow is a live justification") {
        Text(provenance?.name ?: "Select a claim or construction", color = StudioInk, fontWeight = FontWeight.Bold)
        provenance?.directDependencies.orEmpty().forEach { Text("${it}  →  ${provenance?.objectId}", color = StudioGreen, fontFamily = FontFamily.Monospace) }
        Text(provenance?.explanation ?: "The proof view will isolate prerequisites and dependents.", color = StudioMuted)
    }
}

@Composable
private fun TimelinePane(history: SharedStudioHistory, recording: RecordedStudioSession?, onSession: (UnifiedStudioSession) -> Unit, modifier: Modifier) {
    var replayPosition by remember(recording?.durationMillis) { mutableStateOf(0f) }
    val branches = history.branches()
    StudioCard(modifier, "SEMANTIC TIMELINE", "Undo, branch comparison and replay use meaningful actions") {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(history.timeline(), key = { it.id }) { action ->
                Column(Modifier.fillMaxWidth().background(Color(0x44111C29), RoundedCornerShape(9.dp)).padding(7.dp)) {
                    Text(action.label, color = StudioInk, fontWeight = FontWeight.Bold)
                    Text("r${action.beforeRevision} → r${action.afterRevision} · ${action.selectedIds.joinToString().ifBlank { "workspace" }}", color = StudioMuted, fontSize = 9.sp)
                }
            }
            branches.forEach { branch -> item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BRANCH · ${branch.name} · ${branch.actionCount} actions", color = StudioViolet, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    OutlinedButton({ onSession(history.checkout(branch.id)) }) { Text("Open") }
                    OutlinedButton({ onSession(history.merge(branch.id)) }) { Text("Merge") }
                }
            } }
            if (branches.size >= 2) item {
                val comparison = history.compare(branches[0].id, branches[1].id)
                Text("COMPARE · ${comparison.changedObjects.joinToString().ifBlank { "visual state only" }}", color = StudioAmber, fontSize = 10.sp)
            }
            recording?.let { replay -> item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RECORDED · ${replay.title} · ${replay.events.size} states · ${replay.durationMillis} ms", color = StudioGreen, fontSize = 10.sp)
                    Slider(replayPosition, { replayPosition = it }, valueRange = 0f..replay.durationMillis.coerceAtLeast(1).toFloat())
                    OutlinedButton({ onSession(StudioSessionRecorder().stateAt(replay, replayPosition.toLong())) }, modifier = Modifier.fillMaxWidth()) { Text("Restore interactive state") }
                }
            } }
        }
    }
}

@Composable
private fun AlgebraPane(
    session: UnifiedStudioSession,
    projection: StudioProjection,
    input: String,
    onInput: (String) -> Unit,
    onAdd: () -> Unit,
    onSession: (UnifiedStudioSession) -> Unit,
    engine: UnifiedMathStudioEngine,
    modifier: Modifier,
) {
    var query by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val entries = remember(session.document.revision, session.algebraSort, session.algebraFilter, session.algebraStyles, session.hiddenIds, session.showAuxiliary, query) {
        UniversalAlgebraProjection.visible(session, query)
    }
    val selectedEntries = projection.algebraEntries.filter { it.id in session.algebraSelection }
    val suggestions = remember(input, session.algebraSelection, session.document.revision) { AlgebraCommandCatalog.suggest(input, selectedEntries) }
    val groupedEntries = remember(entries, session.algebraSort) {
        entries.groupBy { entry -> when (session.algebraSort) {
            AlgebraSortMode.Type -> entry.kind.name
            AlgebraSortMode.Dependency -> if (entry.free) "Free objects" else "Dependent objects"
            AlgebraSortMode.Layer -> "Layer ${entry.style.layer}"
            AlgebraSortMode.Construction -> "Construction order"
            AlgebraSortMode.Name -> entry.style.folder
        } }
    }
    StudioCard(modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false else when {
            event.isCtrlPressed && event.key == Key.A -> { onSession(engine.selectAllAlgebra(session, entries)); true }
            event.key == Key.DirectionDown || event.key == Key.DirectionUp -> {
                AlgebraKeyboardController.move(entries, session.selectedId, if (event.key == Key.DirectionDown) 1 else -1)?.let { onSession(engine.selectAlgebra(session, it)) }
                true
            }
            event.key == Key.Delete || event.key == Key.Backspace -> { onSession(engine.removeSelection(session)); true }
            event.key == Key.Spacebar -> { onSession(engine.styleSelection(session) { it.copy(visible = !it.visible) }); true }
            else -> false
        }
    }, "UNIVERSAL ALGEBRA", "Every mathematical object is live, typed, editable, and linked") {
        IntentAwareMathField(input, onInput, "Expression, equation, point, list, matrix, or command", Modifier.fillMaxWidth().semantics { contentDescription = "Universal typed Algebra input" }, placeholder = "A=(2,3) · x+y=5 · L={1,4,9} · Line(A,B)")
        Button(onAdd, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add linked object") }
        if (input.isNotBlank()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            suggestions.forEach { suggestion ->
                FilterChip(false, { onInput(suggestion.signature) }, label = { Text("${suggestion.name} · ${suggestion.signature}") },
                    modifier = Modifier.semantics { contentDescription = "${suggestion.description}. ${suggestion.compatibleObjectIds.size} selected objects compatible" })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StudioTransform.entries.forEach { operation -> OutlinedButton({ onSession(engine.transform(session, operation)) }) { Text(operation.label) } }
            OutlinedButton({ onSession(engine.explain(session)) }) { Text("Explain") }
        }
        IntentAwareMathField(query, { query = it }, "Search Algebra", Modifier.fillMaxWidth(), placeholder = "Filter by name, type, or definition", showLegend = false)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AlgebraSortMode.entries.forEach { mode -> FilterChip(session.algebraSort == mode, { onSession(engine.configureAlgebra(session, sort = mode)) }, label = { Text(mode.label) }) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AlgebraDisplayMode.entries.forEach { mode -> FilterChip(session.algebraDisplay == mode, { onSession(engine.configureAlgebra(session, display = mode)) }, label = { Text(mode.label) }) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AlgebraFilter.entries.forEach { filter -> FilterChip(session.algebraFilter == filter, { onSession(engine.configureAlgebra(session, filter = filter)) }, label = { Text(filter.label) }) }
            FilterChip(session.showAuxiliary, { onSession(session.copy(showAuxiliary = !session.showAuxiliary)) }, label = { Text("Auxiliary") })
        }
        if (session.algebraSelection.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${session.algebraSelection.size} SELECTED", color = StudioGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                listOf("Line", "Segment", "Midpoint", "Circle", "Intersect").forEach { command -> OutlinedButton({ onSession(engine.constructFromSelection(session, command)) }) { Text(command) } }
                OutlinedButton({ onSession(engine.duplicateSelection(session)) }) { Text("Duplicate") }
                OutlinedButton({ clipboard.setText(AnnotatedString(engine.copySelection(session))) }) { Text("Copy") }
                OutlinedButton({ onSession(engine.styleSelection(session) { it.copy(visible = false) }) }) { Text("Hide") }
                OutlinedButton({ onSession(engine.styleSelection(session) { it.copy(visible = true) }) }) { Text("Show") }
                OutlinedButton({ onSession(engine.setSelectionFolder(session, "Group ${session.algebraStyles.values.map { it.folder }.distinct().size + 1}")) }) { Text("Group") }
                if (selectedEntries.size == 2) OutlinedButton({ onSession(engine.replaceReferences(session, selectedEntries[0].id, selectedEntries[1].id)) }) {
                    Text("Replace ${selectedEntries[0].name} → ${selectedEntries[1].name}")
                }
                OutlinedButton({ onSession(engine.removeSelection(session)) }) { Text("Delete") }
                OutlinedButton({ onSession(engine.removeSelection(session, cascade = true)) }) { Text("Delete chain") }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            groupedEntries.forEach { (group, groupEntries) ->
                item(key = "group:$group") {
                    Row(Modifier.fillMaxWidth().clickable { onSession(engine.toggleAlgebraGroup(session, group)) }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (group in session.collapsedAlgebraGroups) "▶" else "▼", color = StudioCyan)
                        Text("  $group", color = StudioCyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${groupEntries.size}", color = StudioMuted, fontSize = 9.sp)
                    }
                }
                if (group !in session.collapsedAlgebraGroups) items(groupEntries, key = { it.id }) { entry ->
                val selected = entry.id in session.algebraSelection
                val dependencyContext = entry.id in session.experience.selection.contextCanonicalIds
                Column(
                    Modifier.fillMaxWidth().background(when { selected -> StudioViolet.copy(.18f); dependencyContext -> StudioAmber.copy(.12f); else -> Color(0x66101B29) }, RoundedCornerShape(12.dp))
                        .border(1.dp, when { selected -> StudioViolet; dependencyContext -> StudioAmber; else -> StudioCyan.copy(.25f) }, RoundedCornerShape(12.dp))
                        .clickable { onSession(engine.selectAlgebra(session, entry.id)) }.semantics { contentDescription = entry.spoken }.padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(studioColor(session.colorKeys[entry.id] ?: "cyan"), RoundedCornerShape(8.dp)))
                        Text("  ${entry.style.caption ?: entry.name}", color = StudioInk, fontWeight = FontWeight.Bold)
                        Text("  ${entry.kind.name.uppercase()}", color = StudioCyan, fontSize = 8.sp)
                        Spacer(Modifier.weight(1f))
                        Text(if (entry.free) "FREE" else "DEPENDENT ${entry.dependencies.size}", color = if (entry.free) StudioMuted else StudioGreen, fontSize = 9.sp)
                        OutlinedButton({ onSession(engine.selectAlgebra(session, entry.id, additive = true)) }) { Text(if (selected) "✓" else "+") }
                    }
                    Text("${entry.name} = ${entry.rendered(session.algebraDisplay)}", color = if (entry.valid) StudioInk else StudioAmber,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    if (dependencyContext) Text("CONSTRUCTION PARENT", color = StudioAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(visible = !it.visible) }) }) { Text(if (entry.style.visible) "Hide" else "Show") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(locked = !it.locked) }) }) { Text(if (entry.style.locked) "Unlock" else "Lock") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(auxiliary = !it.auxiliary) }) }) { Text(if (entry.style.auxiliary) "Primary" else "Aux") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(layer = (it.layer + 1) % 10) }) }) { Text("Layer ${entry.style.layer}") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(trace = !it.trace) }) }) { Text(if (entry.style.trace) "Trace on" else "Trace") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(animated = !it.animated) }) }) { Text(if (entry.style.animated) "Pause" else "Animate") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(selectionAllowed = !it.selectionAllowed) }) }) { Text(if (entry.style.selectionAllowed) "Selectable" else "Protected") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(opacity = if (it.opacity > .5f) .35f else 1f) }) }) { Text("Opacity") }
                        OutlinedButton({ onSession(engine.setAlgebraStyle(session, entry.id) { it.copy(strokeWidth = if (it.strokeWidth >= 5f) 1f else it.strokeWidth + 1f) }) }) { Text("Stroke ${entry.style.strokeWidth.toInt()}") }
                        OutlinedButton({ onSession(engine.cycleColor(session, entry.id)) }) { Text("Colour") }
                        OutlinedButton({ onSession(engine.duplicate(session, entry.id)) }) { Text("Duplicate") }
                        OutlinedButton({ onSession(engine.remove(session, entry.id)) }) { Text("Delete") }
                        if (entry.dependents.isNotEmpty()) OutlinedButton({ onSession(engine.remove(session, entry.id, cascade = true)) }) { Text("Delete chain") }
                    }
                    AlgebraNameEditor(entry.name) { onSession(engine.rename(session, entry.id, it)) }
                    AlgebraPropertyEditor(entry.style) { style -> onSession(engine.setAlgebraStyle(session, entry.id) { style }) }
                    AlgebraEquationActionBar(entry, session, engine, onSession)
                    if (!entry.style.locked && entry.free) StudioExpressionEditor(entry.definition) { draft -> onSession(engine.redefine(session, entry.id, draft)) }
                    if (!entry.style.locked && !entry.free) AlgebraDependentEditor(entry, session, engine, onSession)
                    if (entry.dependencies.isNotEmpty()) Text("Inputs: ${entry.dependencies.joinToString()} · updates ${entry.dependents.size} direct object(s)", color = StudioGreen, fontSize = 9.sp)
                    entry.issue?.let { Text("Corrective action: edit this row or duplicate it as a free object. $it", color = StudioAmber, fontSize = 10.sp) }
                }
            }
            }
            if (entries.isEmpty()) item { Text("No Algebra objects match this filter.", color = StudioMuted) }
        }
        projection.graph.parameterRows.forEach { parameter ->
            val value = session.parameterValues[parameter.name] ?: parameter.value
            Text("${parameter.name} = ${format(value)}", color = StudioCyan, fontWeight = FontWeight.Bold)
            Slider(value.toFloat(), { onSession(engine.parameter(session, parameter.name, it.toDouble())) }, valueRange = parameter.min.toFloat()..parameter.max.toFloat(), modifier = Modifier.semantics { contentDescription = "Parameter ${parameter.name}" })
        }
    }
}

@Composable
private fun AlgebraEquationActionBar(entry: AlgebraEntry, session: UnifiedStudioSession, engine: UnifiedMathStudioEngine, onSession: (UnifiedStudioSession) -> Unit) {
    val actions = remember(entry.kind, entry.definition) { AlgebraEquationActions.available(entry) }
    if (actions.isEmpty()) return
    var substitution by remember(entry.id) { mutableStateOf("x=0") }
    Column(Modifier.fillMaxWidth().background(StudioCyan.copy(.06f), RoundedCornerShape(9.dp)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("EQUATION ACTIONS", color = StudioCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            actions.filterNot { it == AlgebraEquationAction.Substitute }.forEach { action ->
                OutlinedButton({ onSession(engine.applyEquationAction(session, entry.id, action)) }) { Text(action.label) }
            }
        }
        if (AlgebraEquationAction.Substitute in actions) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            IntentAwareMathField(substitution, { substitution = it }, "Substitution", Modifier.weight(1f), placeholder = "x=2", showLegend = false)
            OutlinedButton({ onSession(engine.applyEquationAction(session, entry.id, AlgebraEquationAction.Substitute, substitution)) }) { Text("Substitute") }
        }
    }
}

@Composable
private fun AlgebraDependentEditor(entry: AlgebraEntry, session: UnifiedStudioSession, engine: UnifiedMathStudioEngine, onSession: (UnifiedStudioSession) -> Unit) {
    var draft by remember(entry.definition) { mutableStateOf(entry.definition) }
    val preview = remember(draft, session.document.revision) { engine.redefinitionPreview(session, entry.id, draft) }
    Column(Modifier.fillMaxWidth().background(StudioViolet.copy(.08f), RoundedCornerShape(10.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("DEPENDENT REDEFINITION", color = StudioViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        IntentAwareMathField(draft, { draft = it }, "New definition", Modifier.fillMaxWidth(), showLegend = false)
        Text(preview.message, color = if (preview.accepted) StudioGreen else StudioAmber, fontSize = 9.sp)
        if (preview.affectedObjects.isNotEmpty()) Text("Will update: ${preview.affectedObjects.joinToString()}", color = StudioMuted, fontSize = 9.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button({ onSession(engine.redefine(session, entry.id, draft)) }, enabled = preview.accepted && draft != entry.definition) { Text("Redefine") }
            OutlinedButton({ onSession(engine.makeIndependent(session, entry.id)) }) { Text("Make independent") }
            OutlinedButton({ onSession(engine.duplicateAsFree(session, entry.id)) }) { Text("Duplicate as free") }
        }
    }
}

@Composable
private fun AlgebraPropertyEditor(style: AlgebraObjectStyle, onApply: (AlgebraObjectStyle) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var caption by remember(style.caption) { mutableStateOf(style.caption.orEmpty()) }
    var folder by remember(style.folder) { mutableStateOf(style.folder) }
    var condition by remember(style.visibilityCondition) { mutableStateOf(style.visibilityCondition.orEmpty()) }
    OutlinedButton({ expanded = !expanded }) { Text(if (expanded) "Close inspector" else "Object inspector") }
    if (expanded) {
        IntentAwareMathField(caption, { caption = it }, "Caption", Modifier.fillMaxWidth(), showLegend = false)
        IntentAwareMathField(folder, { folder = it }, "Folder", Modifier.fillMaxWidth(), showLegend = false)
        IntentAwareMathField(condition, { condition = it }, "Visibility condition", Modifier.fillMaxWidth(), placeholder = "Example: a > 0", showLegend = false)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AlgebraLabelMode.entries.forEach { mode -> FilterChip(style.labelMode == mode, { onApply(style.copy(labelMode = mode)) }, label = { Text(mode.name) }) }
            Button({ onApply(style.copy(caption = caption.ifBlank { null }, folder = folder.ifBlank { "Workspace" }, visibilityCondition = condition.ifBlank { null })) }) { Text("Apply properties") }
        }
    }
}

@Composable
private fun AlgebraNameEditor(name: String, onApply: (String) -> Unit) {
    var draft by remember(name) { mutableStateOf(name) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        IntentAwareMathField(draft, { draft = it }, "Object name", Modifier.weight(1f), showLegend = false)
        OutlinedButton({ onApply(draft) }, enabled = draft != name && draft.isNotBlank()) { Text("Rename") }
    }
}

@Composable
private fun GraphPane(objects: List<MathGraphObject>, keyPoints: Map<String, List<Vec2>>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier, onSculptPreview: ((Double, Double) -> CurveSculptPreview?)? = null, onSculptCommit: ((CurveSculptCandidate) -> Unit)? = null) {
    var sculptPreview by remember(selectedId) { mutableStateOf<CurveSculptPreview?>(null) }
    StudioCard(modifier, "GRAPH & GEOMETRY", "Roots and extrema are shared geometry points") {
        Canvas(Modifier.weight(1f).fillMaxWidth().pointerInput(selectedId, onSculptPreview) {
            detectDragGestures(
                onDragEnd = { sculptPreview?.preferred?.let { onSculptCommit?.invoke(it) }; sculptPreview = null },
                onDragCancel = { sculptPreview = null },
            ) { change, _ ->
                change.consume()
                val x = -6.0 + 12.0 * change.position.x / size.width
                val y = 6.0 - 12.0 * change.position.y / size.height
                sculptPreview = onSculptPreview?.invoke(x, y)
            }
        }.semantics { contentDescription = "Linked graph with roots and extrema. Drag a selected parameterized curve to sculpt it." }) {
            val left = -6.0; val right = 6.0; val bottom = -6.0; val top = 6.0
            fun screen(point: Vec2) = Offset(((point.x - left) / (right - left) * size.width).toFloat(), ((top - point.y) / (top - bottom) * size.height).toFloat())
            for (i in -6..6) {
                val vertical = screen(Vec2(i.toDouble(), 0.0)).x; val horizontal = screen(Vec2(0.0, i.toDouble())).y
                drawLine(if (i == 0) Color.White.copy(.75f) else StudioCyan.copy(.12f), Offset(vertical, 0f), Offset(vertical, size.height), if (i == 0) 2f else 1f)
                drawLine(if (i == 0) Color.White.copy(.75f) else StudioCyan.copy(.12f), Offset(0f, horizontal), Offset(size.width, horizontal), if (i == 0) 2f else 1f)
            }
            objects.forEachIndexed { index, graphObject ->
                val color = studioColor(listOf("cyan", "violet", "green", "amber", "pink")[index % 5])
                val path = Path(); var started = false
                graphObject.sample.points.forEachIndexed { pointIndex, point ->
                    val p = screen(point)
                    if (!point.x.isFinite() || !point.y.isFinite() || point.y !in bottom * 2..top * 2 || pointIndex in graphObject.sample.breaks) started = false
                    else if (!started) { path.moveTo(p.x, p.y); started = true } else path.lineTo(p.x, p.y)
                }
                drawPath(path, color.copy(if (graphObject.rowId == selectedId) 1f else .65f), style = Stroke(if (graphObject.rowId == selectedId) 5f else 3f))
                keyPoints[graphObject.rowId].orEmpty().forEach { point -> if (point.x in left..right && point.y in bottom..top) { val p = screen(point); drawCircle(StudioAmber, 7f, p); drawCircle(Color.Black, 3f, p) } }
            }
            sculptPreview?.let { preview ->
                val a = screen(preview.anchor); val b = screen(preview.target)
                drawLine(StudioAmber, a, b, 3f); drawCircle(StudioAmber, 8f, b); drawCircle(StudioViolet, 6f, a)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            objects.forEach { graphObject -> FilterChip(graphObject.rowId == selectedId, { onSelect(graphObject.rowId) }, label = { Text(graphObject.name) }) }
        }
    }
}

@Composable
private fun TablePane(projection: StudioProjection, modifier: Modifier) {
    StudioCard(modifier, "LIVE TABLE", "Values come from the same resolved expressions") {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Row(Modifier.fillMaxWidth()) { Text("x", color = StudioCyan, modifier = Modifier.width(44.dp)); projection.graph.graphObjects.forEach { Text(it.name, color = StudioViolet, modifier = Modifier.widthIn(min = 72.dp).weight(1f)) } }
            }
            items(projection.graph.generatedTable) { row ->
                Row(Modifier.fillMaxWidth().background(Color(0x44111C29), RoundedCornerShape(7.dp)).padding(5.dp)) {
                    Text(format(row.input), color = StudioInk, modifier = Modifier.width(44.dp), fontFamily = FontFamily.Monospace)
                    projection.graph.graphObjects.forEach { graphObject -> Text(format(row.outputs[graphObject.name] ?: Double.NaN), color = StudioMuted, modifier = Modifier.widthIn(min = 72.dp).weight(1f), fontFamily = FontFamily.Monospace) }
                }
            }
        }
    }
}

@Composable
private fun ResultPane(session: UnifiedStudioSession, diagnostics: List<String>, modifier: Modifier) {
    StudioCard(modifier, "RESULT PODS", session.message) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            diagnostics.forEach { diagnostic -> item { Text("Diagnostic · $diagnostic", color = StudioAmber, fontSize = 11.sp) } }
            items(session.resultPods) { pod ->
                Column(Modifier.fillMaxWidth().background(if (pod.verified) StudioGreen.copy(.10f) else StudioAmber.copy(.10f), RoundedCornerShape(12.dp)).padding(9.dp)) {
                    Text("${if (pod.verified) "VERIFIED" else "BOUNDED"} · ${pod.title}", color = if (pod.verified) StudioGreen else StudioAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(pod.exact, color = StudioInk, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(pod.explanation, color = StudioMuted, fontSize = 10.sp)
                    pod.steps.take(3).forEach { Text("• $it", color = StudioMuted, fontSize = 10.sp) }
                }
            }
            if (session.resultPods.isEmpty()) item { Text("Select an expression, then simplify, expand, factor, differentiate, integrate, or explain it.", color = StudioMuted) }
        }
    }
}

@Composable
private fun StudioCard(modifier: Modifier, title: String, subtitle: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.background(StudioSurface, RoundedCornerShape(18.dp)).border(1.dp, StudioCyan.copy(.25f), RoundedCornerShape(18.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = StudioCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(subtitle, color = StudioMuted, fontSize = 10.sp)
        content()
    }
}

@Composable
private fun StudioExpressionEditor(source: String, onApply: (String) -> Unit) {
    var draft by remember(source) { mutableStateOf(source) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IntentAwareMathField(draft, { draft = it }, "Expression", Modifier.weight(1f), showLegend = false)
        Button({ onApply(draft) }, enabled = draft.isNotBlank() && draft != source) { Text("Apply") }
    }
}

private fun studioColor(key: String) = when (key.lowercase()) { "violet" -> StudioViolet; "green" -> StudioGreen; "amber" -> StudioAmber; "pink" -> Color(0xFFFF6FB5); else -> StudioCyan }
private fun format(value: Double) = if (!value.isFinite()) "—" else String.format(java.util.Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
