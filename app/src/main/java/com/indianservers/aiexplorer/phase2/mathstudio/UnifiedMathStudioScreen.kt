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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.UnifiedConstructionEngine
import com.indianservers.aiexplorer.core.UnifiedConstructionHistory
import com.indianservers.aiexplorer.core.UnifiedConstructionSession
import com.indianservers.aiexplorer.workspace.MathGraphObject
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.persistence.MathFileExchange
import kotlinx.coroutines.launch

private val StudioBackground = Color(0xFF03070C)
private val StudioSurface = Color(0xEE09131F)
private val StudioInk = Color(0xFFEAF5FF)
private val StudioMuted = Color(0xFF9EB0C8)
private val StudioCyan = Color(0xFF20D9FF)
private val StudioViolet = Color(0xFF985DFF)
private val StudioGreen = Color(0xFF48E0A4)
private val StudioAmber = Color(0xFFFFC857)

private enum class StudioPane(val label: String) { Algebra("Algebra"), Graph("Graph"), Table("Table"), Results("Results") }

@Composable
fun UnifiedMathStudioScreen(
    initialWorkspace: WorkspaceState,
    onWorkspaceChange: (WorkspaceState) -> Unit,
    onBack: () -> Unit,
) {
    val engine = remember { UnifiedMathStudioEngine() }
    val history = remember(initialWorkspace.id) { UnifiedStudioHistory(engine.fromWorkspace(initialWorkspace)) }
    var session by remember(initialWorkspace.id) { mutableStateOf(history.current) }
    var input by remember { mutableStateOf("h(x) = a*sin(x) + 1") }
    var pane by remember { mutableStateOf(StudioPane.Algebra) }
    val constructionEngine = remember { UnifiedConstructionEngine() }
    val constructionHistory = remember(initialWorkspace.id) { UnifiedConstructionHistory(engine = constructionEngine) }
    var construction by remember(initialWorkspace.id) { mutableStateOf(UnifiedConstructionSession()) }
    var constructionInput by remember { mutableStateOf("point2d(A,0,0)") }
    var constructionStatus by remember { mutableStateOf("Graph, 2D and 3D commands share one dependency-aware palette.") }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val projection = remember(session.document.revision, session.parameterValues) { engine.projection(session) }
    fun apply(next: UnifiedStudioSession) { session = history.apply(next) }
    val importGeoGebra = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || activity == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { MathFileExchange.readGeoGebra(activity, uri, engine.toWorkspace(session)) }
                .onSuccess { result -> session = history.apply(engine.fromWorkspace(result.workspace).copy(message = "Imported ${result.coverage.imported} GeoGebra objects; ${result.coverage.skipped.size} unsupported objects reported.")) }
                .onFailure { session = history.apply(session.copy(message = "GeoGebra import failed: ${it.message ?: "invalid package"}")) }
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
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(session.message, color = StudioMuted, fontSize = 10.sp, modifier = Modifier.widthIn(min = 220.dp, max = 600.dp))
            OutlinedButton({ importGeoGebra.launch(arrayOf("application/vnd.geogebra.file", "application/zip", "application/octet-stream")) }) { Text("Import .ggb") }
            OutlinedButton({
                if (activity != null) scope.launch { runCatching { MathFileExchange.shareGeoGebra(activity, engine.toWorkspace(session)) }.onFailure { session = history.apply(session.copy(message = "GeoGebra export failed: ${it.message}")) } }
            }, enabled = activity != null) { Text("Export .ggb") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                constructionInput, { constructionInput = it }, Modifier.weight(1f).semantics { contentDescription = "Graph geometry and 3D construction command" },
                label = { Text("Unified command") }, placeholder = { Text("midpoint(M,A,B) · surface(s,z=x^2+y^2)") }, singleLine = true,
            )
            Button({
                runCatching { constructionHistory.execute(constructionInput) }
                    .onSuccess { construction = it; constructionStatus = "Created ${constructionEngine.tokens(it).last().accessibleLabel}" }
                    .onFailure { constructionStatus = it.message ?: "The command could not be completed." }
            }, enabled = constructionInput.isNotBlank()) { Text("Construct") }
            OutlinedButton({ construction = constructionHistory.undo(); constructionStatus = "Unified construction undone." }, enabled = constructionHistory.canUndo()) { Text("↶") }
            OutlinedButton({ construction = constructionHistory.redo(); constructionStatus = "Unified construction restored." }, enabled = constructionHistory.canRedo()) { Text("↷") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { contentDescription = constructionStatus }, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(constructionStatus, color = if (constructionStatus.startsWith("Created")) StudioGreen else StudioMuted, fontSize = 10.sp)
            constructionEngine.tokens(construction).forEach { token ->
                FilterChip(false, {}, label = { Text("${token.view.name} · ${token.id}") }, modifier = Modifier.semantics { contentDescription = token.accessibleLabel })
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 820.dp
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgebraPane(session, projection, input, { input = it }, { apply(engine.add(session, input)); input = "" }, { apply(it) }, engine, Modifier.weight(.38f).fillMaxHeight())
                    Column(Modifier.weight(.62f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphPane(projection.graph.graphObjects, projection.keyPoints, session.selectedId, { apply(engine.select(session, it)) }, Modifier.weight(.62f))
                        Row(Modifier.weight(.38f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TablePane(projection, Modifier.weight(1f).fillMaxHeight())
                            ResultPane(session, projection.diagnostics, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPane.entries.forEach { value -> FilterChip(pane == value, { pane = value }, label = { Text(value.label) }) }
                }
                when (pane) {
                    StudioPane.Algebra -> AlgebraPane(session, projection, input, { input = it }, { apply(engine.add(session, input)); input = "" }, { apply(it) }, engine, Modifier.fillMaxSize())
                    StudioPane.Graph -> GraphPane(projection.graph.graphObjects, projection.keyPoints, session.selectedId, { apply(engine.select(session, it)) }, Modifier.fillMaxSize())
                    StudioPane.Table -> TablePane(projection, Modifier.fillMaxSize())
                    StudioPane.Results -> ResultPane(session, projection.diagnostics, Modifier.fillMaxSize())
                }
            }
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
    StudioCard(modifier, "ALGEBRA & OBJECTS", "Changes recalculate every linked view") {
        OutlinedTextField(input, onInput, Modifier.fillMaxWidth().semantics { contentDescription = "Unified studio expression input" }, label = { Text("f(x) = expression") }, singleLine = true)
        Button(onAdd, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add linked object") }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StudioTransform.entries.forEach { operation -> OutlinedButton({ onSession(engine.transform(session, operation)) }) { Text(operation.label) } }
            OutlinedButton({ onSession(engine.explain(session)) }) { Text("Explain") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(projection.expressions, key = { it.id }) { expression ->
                val selected = expression.id == session.selectedId
                Column(
                    Modifier.fillMaxWidth().background(if (selected) StudioViolet.copy(.18f) else Color(0x66101B29), RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) StudioViolet else StudioCyan.copy(.25f), RoundedCornerShape(12.dp))
                        .clickable { onSession(engine.select(session, expression.id)) }.padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(studioColor(expression.colorKey), RoundedCornerShape(8.dp)))
                        Text("  ${expression.name}", color = StudioInk, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(if (expression.dependencies.isEmpty()) "INDEPENDENT" else "LINKED ${expression.dependencies.size}", color = if (expression.dependencies.isEmpty()) StudioMuted else StudioGreen, fontSize = 9.sp)
                        OutlinedButton({ onSession(engine.remove(session, expression.id)) }) { Text("Delete") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton({ onSession(engine.toggleVisible(session, expression.id)) }) { Text(if (expression.visible) "Hide" else "Show") }
                        OutlinedButton({ onSession(engine.cycleColor(session, expression.id)) }) { Text("Colour") }
                    }
                    StudioExpressionEditor(expression.source) { onSession(engine.edit(session, expression.id, it)) }
                    if (expression.source != expression.resolvedSource) Text("Resolved: ${expression.resolvedSource}", color = StudioGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    expression.error?.let { Text(it, color = StudioAmber, fontSize = 10.sp) }
                }
            }
        }
        projection.graph.parameterRows.forEach { parameter ->
            val value = session.parameterValues[parameter.name] ?: parameter.value
            Text("${parameter.name} = ${format(value)}", color = StudioCyan, fontWeight = FontWeight.Bold)
            Slider(value.toFloat(), { onSession(engine.parameter(session, parameter.name, it.toDouble())) }, valueRange = parameter.min.toFloat()..parameter.max.toFloat(), modifier = Modifier.semantics { contentDescription = "Parameter ${parameter.name}" })
        }
    }
}

@Composable
private fun GraphPane(objects: List<MathGraphObject>, keyPoints: Map<String, List<Vec2>>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier) {
    StudioCard(modifier, "GRAPH & GEOMETRY", "Roots and extrema are shared geometry points") {
        Canvas(Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = "Linked graph with roots and extrema" }) {
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
        OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), singleLine = true, label = { Text("Expression") })
        Button({ onApply(draft) }, enabled = draft.isNotBlank() && draft != source) { Text("Apply") }
    }
}

private fun studioColor(key: String) = when (key.lowercase()) { "violet" -> StudioViolet; "green" -> StudioGreen; "amber" -> StudioAmber; "pink" -> Color(0xFFFF6FB5); else -> StudioCyan }
private fun format(value: Double) = if (!value.isFinite()) "—" else String.format(java.util.Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
