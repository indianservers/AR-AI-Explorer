package com.indianservers.aiexplorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.SetExpression
import com.indianservers.aiexplorer.core.SetExpressionEvaluator
import com.indianservers.aiexplorer.core.SetExpressionParser
import com.indianservers.aiexplorer.core.SetLawVerifier
import com.indianservers.aiexplorer.core.SetTheoryLearningCatalog
import com.indianservers.aiexplorer.core.SetTheoryStudioEngine
import com.indianservers.aiexplorer.core.VennRegionEngine
import kotlin.math.min

private val SetSpace get() = themedColor(Color(0xFF010612), Background)
private val SetPanel get() = themedColor(Color(0xEE081528), ActiveAppPalette.surface)
private val SetInk get() = themedColor(Color(0xFFF5F9FF), Ink)
private val SetMuted get() = themedColor(Color(0xFFA9B7D3), Muted)
private val SetCyan get() = themedColor(Color(0xFF28DFFF), Cyan)
private val SetViolet get() = themedColor(Color(0xFFA86BFF), Violet)
private val SetGreen get() = themedColor(Color(0xFF4EE5A4), Green)
private val SetAmber get() = themedColor(Color(0xFFFFB83F), Amber)
private val SetPink get() = themedColor(Color(0xFFFF66A5), Coral)
private val SetRed get() = themedColor(Color(0xFFFF667C), Coral)

private enum class SetModulePage {
    Hub, Introduction, Types, Representation, Workspace, Subsets, PowerSet,
    Cartesian, Laws, InclusionExclusion, Relations, Guided, Saved,
}
private enum class VennInteractionMode(val label: String) { Elements("Elements"), Counts("Counts"), Shade("Shade") }

private data class EditableSets(
    val universe: String,
    val a: String,
    val b: String,
    val c: String,
) {
    fun parsed(): Map<String, Set<String>> = mapOf(
        "A" to SetTheoryStudioEngine.parseElements(a).toSet(),
        "B" to SetTheoryStudioEngine.parseElements(b).toSet(),
        "C" to SetTheoryStudioEngine.parseElements(c).toSet(),
    )
    fun universeSet(): Set<String> =
        SetTheoryStudioEngine.parseElements(universe).toSet() + parsed().values.flatten()
}

private val EditableSetsSaver = listSaver<EditableSets, String>(
    save = { listOf(it.universe, it.a, it.b, it.c) },
    restore = {
        EditableSets(
            universe = it.getOrElse(0) { "" },
            a = it.getOrElse(1) { "" },
            b = it.getOrElse(2) { "" },
            c = it.getOrElse(3) { "" },
        )
    },
)

@Composable
internal fun SetTheoryInteractiveModule(
    vm: ExplorerViewModel,
    wide: Boolean,
    onOpenLogic: () -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(SetModulePage.Hub.name) }
    var setCount by rememberSaveable { mutableIntStateOf(2) }
    var sets by rememberSaveable(stateSaver = EditableSetsSaver) {
        mutableStateOf(EditableSets("1, 2, 3, 4, 5, 6, 7, 8", "1, 2, 3, 6", "3, 4, 6", "2, 5, 6"))
    }
    var expression by rememberSaveable { mutableStateOf("A ∪ B") }
    var modeName by rememberSaveable { mutableStateOf(VennInteractionMode.Elements.name) }
    var selectedElement by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRegion by rememberSaveable { mutableIntStateOf(6) }
    var showResults by rememberSaveable { mutableStateOf(true) }
    var expressionHistory by remember { mutableStateOf(listOf("A ∪ B")) }
    var undo by remember { mutableStateOf<List<EditableSets>>(emptyList()) }
    var redo by remember { mutableStateOf<List<EditableSets>>(emptyList()) }
    val page = SetModulePage.valueOf(pageName)
    val mode = VennInteractionMode.valueOf(modeName)

    fun navigate(next: SetModulePage) { pageName = next.name }
    fun update(next: EditableSets) {
        if (next == sets) return
        undo = (undo + sets).takeLast(30)
        redo = emptyList()
        sets = next
    }
    fun openWorkspace(count: Int, requestedExpression: String = if (count == 2) "A ∪ B" else "A ∪ B ∪ C") {
        setCount = count
        expression = requestedExpression
        expressionHistory = (listOf(requestedExpression) + expressionHistory).distinct().take(12)
        navigate(SetModulePage.Workspace)
    }

    BackHandler {
        if (page != SetModulePage.Hub) navigate(SetModulePage.Hub) else vm.returnToMathMenu()
    }

    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        themedColor(Color(0xFF17214A), SetSpace),
                        SetSpace,
                        themedColor(Color.Black, SetSpace),
                    ),
                    center = Offset(260f, 30f),
                    radius = 1200f,
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 28.dp else 9.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SetButton("< BACK", themedColor(SetCyan, Color.Transparent)) { if (page == SetModulePage.Hub) vm.returnToMathMenu() else navigate(SetModulePage.Hub) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (page == SetModulePage.Hub) "Set Theory" else pageTitle(page), color = themedColor(SetInk, Color.Transparent), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("INTERACTIVE SETS & VENN DIAGRAMS", color = themedColor(SetViolet, Color.Transparent), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            SetButton("LOGIC", themedColor(SetViolet, Color.Transparent), onOpenLogic)
        }

        when (page) {
            SetModulePage.Hub -> SetTheoryHub(::navigate, ::openWorkspace)
            SetModulePage.Introduction -> SetIntroductionPanel()
            SetModulePage.Types -> SetTypesPanel(sets, ::update)
            SetModulePage.Representation -> SetRepresentationPanel(sets, ::update) { openWorkspace(2) }
            SetModulePage.Workspace -> {
                SetWorkspacePanel(
                    sets = sets,
                    setCount = setCount,
                    expression = expression,
                    mode = mode,
                    selectedElement = selectedElement,
                    selectedRegion = selectedRegion,
                    expressionHistory = expressionHistory,
                    showResults = showResults,
                    onSets = ::update,
                    onSetCount = { setCount = it },
                    onExpression = {
                        expression = it
                        if (SetExpressionParser.parse(it).valid) expressionHistory = (listOf(it) + expressionHistory).distinct().take(12)
                    },
                    onMode = { modeName = it.name },
                    onElement = { selectedElement = it },
                    onRegion = { selectedRegion = it },
                    onShowResults = { showResults = it },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetButton("UNDO", SetMuted) {
                        undo.lastOrNull()?.let { previous -> redo = redo + sets; sets = previous; undo = undo.dropLast(1) }
                    }
                    SetButton("REDO", SetMuted) {
                        redo.lastOrNull()?.let { next -> undo = undo + sets; sets = next; redo = redo.dropLast(1) }
                    }
                    SetButton("RESET", SetAmber) {
                        update(EditableSets("1, 2, 3, 4, 5, 6, 7, 8", "1, 2, 3, 6", "3, 4, 6", "2, 5, 6"))
                    }
                    SetButton("SAVE", SetGreen) { vm.saveSetTheoryWorkspace(encodeSetWorkspace(sets, setCount, expression)) }
                }
            }
            SetModulePage.Subsets -> SubsetPanel(sets, ::update)
            SetModulePage.PowerSet -> PowerSetPanel(sets)
            SetModulePage.Cartesian -> CartesianPanel(sets)
            SetModulePage.Laws -> SetLawsPanel(::openWorkspace)
            SetModulePage.InclusionExclusion -> InclusionExclusionPanel(sets, ::update)
            SetModulePage.Relations -> RelationsPanel(sets)
            SetModulePage.Guided -> GuidedSetChallenges(::openWorkspace)
            SetModulePage.Saved -> SavedSetWorkspaces(vm) { restored ->
                sets = restored.first
                setCount = restored.second
                expression = restored.third
                navigate(SetModulePage.Workspace)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SetTheoryHub(navigate: (SetModulePage) -> Unit, openWorkspace: (Int, String) -> Unit) {
    data class Tool(val title: String, val detail: String, val icon: String, val accent: Color, val action: () -> Unit)
    val tools = listOf(
        Tool("Introduction to Sets", "Create a collection and test membership", "{ }", SetCyan) { navigate(SetModulePage.Introduction) },
        Tool("Types of Sets", "Empty, singleton, equal, finite and disjoint", "○", SetViolet) { navigate(SetModulePage.Types) },
        Tool("Set Representation", "Roster, builder, verbal and Venn forms", "{x}", SetGreen) { navigate(SetModulePage.Representation) },
        Tool("Two-Set Venn", "Drag elements across four regions", "2V", SetPink) { openWorkspace(2, "A ∪ B") },
        Tool("Three-Set Venn", "Explore all eight membership regions", "3V", SetCyan) { openWorkspace(3, "A ∪ B ∪ C") },
        Tool("Set Operations", "Union, intersection, difference and complement", "∪∩", SetAmber) { openWorkspace(2, "A ∩ B") },
        Tool("Subsets & Supersets", "Build and validate candidate subsets", "⊂", SetGreen) { navigate(SetModulePage.Subsets) },
        Tool("Power Sets", "Generate and group every small subset", "2ⁿ", SetViolet) { navigate(SetModulePage.PowerSet) },
        Tool("Cartesian Products", "Lists, grids and ordered pairs", "×", SetCyan) { navigate(SetModulePage.Cartesian) },
        Tool("Set Laws", "Verify both sides by membership regions", "=", SetPink) { navigate(SetModulePage.Laws) },
        Tool("Inclusion–Exclusion", "Count overlaps without double-counting", "|∪|", SetAmber) { navigate(SetModulePage.InclusionExclusion) },
        Tool("Relations & Functions", "Ordered pairs, mappings and properties", "→", SetGreen) { navigate(SetModulePage.Relations) },
        Tool("Guided Challenges", "Attempt first, then validate and explain", "?", SetPink) { navigate(SetModulePage.Guided) },
        Tool("Saved Workspaces", "Restore editable sets and expressions", "S", SetViolet) { navigate(SetModulePage.Saved) },
    )
    SetPanelCard(SetViolet) {
        Text("Build sets. Move elements. See every operation.", color = SetInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("A single membership-mask engine keeps the expression, highlighted regions, roster and explanation synchronized.", color = SetMuted, fontSize = 11.sp)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 760.dp) 3 else 2
        val gap = 8.dp
        val width = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
            tools.forEach { tool ->
                Column(
                    Modifier.width(width).heightIn(min = 128.dp).background(Brush.linearGradient(listOf(tool.accent.copy(.17f), SetPanel)), RoundedCornerShape(18.dp))
                        .border(1.dp, tool.accent.copy(.52f), RoundedCornerShape(18.dp)).clickable(onClick = tool.action)
                        .semantics { contentDescription = "Open ${tool.title}. ${tool.detail}" }.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SetIcon(tool.icon, tool.accent); Text("OPEN >", color = tool.accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Text(tool.title, color = SetInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(tool.detail, color = SetMuted, fontSize = 9.sp, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun SetWorkspacePanel(
    sets: EditableSets,
    setCount: Int,
    expression: String,
    mode: VennInteractionMode,
    selectedElement: String?,
    selectedRegion: Int,
    expressionHistory: List<String>,
    showResults: Boolean,
    onSets: (EditableSets) -> Unit,
    onSetCount: (Int) -> Unit,
    onExpression: (String) -> Unit,
    onMode: (VennInteractionMode) -> Unit,
    onElement: (String?) -> Unit,
    onRegion: (Int) -> Unit,
    onShowResults: (Boolean) -> Unit,
) {
    val parsedSets = sets.parsed()
    val universe = sets.universeSet()
    val parse = remember(expression) { SetExpressionParser.parse(expression) }
    val highlighted = remember(parse.expression, setCount) { parse.expression?.let { VennRegionEngine.highlighted(it, setCount) }.orEmpty() }
    val result = remember(parse.expression, parsedSets, universe) { parse.expression?.let { SetExpressionEvaluator.evaluate(it, parsedSets, universe) }.orEmpty() }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SetChip("TWO SETS", setCount == 2, SetCyan) { onSetCount(2); onExpression("A ∪ B") }
        SetChip("THREE SETS", setCount == 3, SetViolet) { onSetCount(3); onExpression("A ∪ B ∪ C") }
        VennInteractionMode.entries.forEach { SetChip(it.label.uppercase(), mode == it, SetGreen) { onMode(it) } }
    }
    SetPanelCard(SetCyan) {
        Text("SETS & UNIVERSAL SET", color = SetCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(sets.universe, { onSets(sets.copy(universe = it)) }, Modifier.fillMaxWidth(), label = { Text("Universal set U") })
        OutlinedTextField(sets.a, { onSets(sets.copy(a = it)) }, Modifier.fillMaxWidth(), label = { Text("Set A elements") })
        OutlinedTextField(sets.b, { onSets(sets.copy(b = it)) }, Modifier.fillMaxWidth(), label = { Text("Set B elements") })
        if (setCount == 3) OutlinedTextField(sets.c, { onSets(sets.copy(c = it)) }, Modifier.fillMaxWidth(), label = { Text("Set C elements") })
        Text("Spaces are trimmed and duplicates are removed automatically.", color = SetMuted, fontSize = 9.sp)
    }
    OutlinedTextField(
        expression,
        onExpression,
        Modifier.fillMaxWidth().semantics { contentDescription = "Safe set expression editor" },
        label = { Text("Expression") },
        supportingText = { Text(parse.error ?: "Parsed safely into an expression tree · no eval") },
        isError = !parse.valid,
        singleLine = true,
    )
    val operations = if (setCount == 2) {
        listOf("A ∪ B", "A ∩ B", "A - B", "B - A", "A Δ B", "A'", "B'", "(A ∪ B)'", "(A ∩ B)'", "A ∩ B'", "A' ∩ B")
    } else {
        listOf("A ∪ B ∪ C", "A ∩ B ∩ C", "(A ∪ B) ∩ C", "(A ∩ B) ∪ C", "A - (B ∪ C)", "A ∩ (B ∪ C)", "A ∪ (B ∩ C)", "(A ∪ B ∪ C)'")
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        operations.forEach { SetChip(it, expression == it, SetViolet) { onExpression(it) } }
    }
    if (setCount == 3) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            SetButton("EXACTLY ONE", SetCyan) { onExpression("(A - (B ∪ C)) ∪ (B - (A ∪ C)) ∪ (C - (A ∪ B))") }
            SetButton("EXACTLY TWO", SetGreen) { onExpression("(A ∩ B ∩ C') ∪ (A ∩ C ∩ B') ∪ (B ∩ C ∩ A')") }
            SetButton("AT LEAST TWO", SetAmber) { onExpression("(A ∩ B) ∪ (A ∩ C) ∪ (B ∩ C)") }
            SetButton("NONE", SetPink) { onExpression("(A ∪ B ∪ C)'") }
        }
    }
    InteractiveMembershipVenn(
        setCount = setCount,
        sets = parsedSets,
        universe = universe,
        highlighted = highlighted,
        showElements = mode != VennInteractionMode.Shade,
        selectedElement = selectedElement,
        onSelectElement = onElement,
        onSelectRegion = onRegion,
        onMoveElement = { element, mask -> onSets(moveElementToMask(sets, element, mask)) },
        modifier = Modifier.fillMaxWidth().height(if (setCount == 3) 355.dp else 320.dp),
    )
    Text("Selected region: ${VennRegionEngine.threeSetRegionNames[selectedRegion]}", color = SetMuted, fontSize = 10.sp)
    if (mode == VennInteractionMode.Elements && universe.isNotEmpty()) {
        Text("ACCESSIBLE ELEMENT CONTROLS", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            universe.sorted().forEach { SetChip(it, selectedElement == it, SetGreen) { onElement(it) } }
        }
        selectedElement?.let { element ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val masks = if (setCount == 2) listOf(4, 6, 2, 0) else (0..7).toList()
                masks.forEach { mask ->
                    SetButton(VennRegionEngine.threeSetRegionNames.getValue(mask), SetCyan) {
                        onSets(moveElementToMask(sets, element, mask))
                    }
                }
            }
        }
    }
    if (mode == VennInteractionMode.Counts) {
        val regions = VennRegionEngine.elementsByRegion(universe, parsedSets)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (setCount == 2) listOf(0, 2, 4, 6) else (0..7).toList()).forEach { mask ->
                SetMetric(VennRegionEngine.threeSetRegionNames.getValue(mask), regions[mask].orEmpty().size.toString(), if (mask in highlighted) SetGreen else SetMuted)
            }
        }
    }
    SetPanelCard(if (parse.valid) SetGreen else SetRed) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RESULT", color = if (parse.valid) SetGreen else SetRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
            SetButton(if (showResults) "COLLAPSE" else "EXPAND", SetMuted) { onShowResults(!showResults) }
        }
        AnimatedVisibility(showResults) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!parse.valid) Text(parse.error.orEmpty(), color = SetRed)
                else {
                    Text(expression, color = SetCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif)
                    Text(SetTheoryStudioEngine.roster(result.sorted()), color = SetInk, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("|$expression| = ${result.size}", color = SetGreen, fontWeight = FontWeight.Bold)
                    Text("Selected regions: ${VennRegionEngine.describe(highlighted)}", color = SetMuted, fontSize = 10.sp)
                    Text("An element is included exactly when its A/B/C membership mask makes the expression true.", color = SetMuted, fontSize = 10.sp)
                }
            }
        }
    }
    if (expressionHistory.size > 1) {
        Text("HISTORY", color = SetMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            expressionHistory.forEach { SetChip(it, expression == it, SetMuted) { onExpression(it) } }
        }
    }
}

@Composable
private fun InteractiveMembershipVenn(
    setCount: Int,
    sets: Map<String, Set<String>>,
    universe: Set<String>,
    highlighted: Set<Int>,
    showElements: Boolean,
    selectedElement: String?,
    onSelectElement: (String?) -> Unit,
    onSelectRegion: (Int) -> Unit,
    onMoveElement: (String, Int) -> Unit,
    modifier: Modifier,
) {
    var dragged by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    fun positionFor(element: String, size: IntSize): Offset {
        val mask = VennRegionEngine.maskFor(element, sets).value
        val peers = universe.filter { VennRegionEngine.maskFor(it, sets).value == mask }.sorted()
        val index = peers.indexOf(element).coerceAtLeast(0)
        val base = regionAnchor(mask, size.width.toFloat(), size.height.toFloat())
        return base + Offset(((index % 3) - 1) * 34f, (index / 3) * 25f)
    }
    Canvas(
        modifier.background(Color(0xFF020A1C), RoundedCornerShape(20.dp)).border(1.dp, SetCyan.copy(.55f), RoundedCornerShape(20.dp))
            .pointerInput(setCount, sets, universe) {
                detectTapGestures { tap ->
                    val closest = universe.minByOrNull { (positionFor(it, size) - tap).getDistance() }
                    if (closest != null && (positionFor(closest, size) - tap).getDistance() < 30f) onSelectElement(closest)
                    else onSelectRegion(maskAt(tap, size.width.toFloat(), size.height.toFloat(), setCount))
                }
            }
            .pointerInput(setCount, sets, universe) {
                detectDragGestures(
                    onDragStart = { start ->
                        val closest = universe.minByOrNull { (positionFor(it, size) - start).getDistance() }
                        if (closest != null && (positionFor(closest, size) - start).getDistance() < 44f) {
                            dragged = closest
                            dragPosition = start
                            onSelectElement(closest)
                        }
                    },
                    onDragEnd = {
                        val element = dragged
                        val position = dragPosition
                        if (element != null && position != null) onMoveElement(element, maskAt(position, size.width.toFloat(), size.height.toFloat(), setCount))
                        dragged = null; dragPosition = null
                    },
                    onDragCancel = { dragged = null; dragPosition = null },
                ) { change, _ ->
                    if (dragged != null) dragPosition = change.position
                    change.consume()
                }
            }
            .semantics {
                contentDescription = "${if (setCount == 2) "Two" else "Three"}-set Venn diagram. Highlighted: ${VennRegionEngine.describe(highlighted)}. Drag an element or use the accessible move controls."
            },
    ) {
        val radius = min(size.width, size.height) * if (setCount == 2) .29f else .255f
        val centers = vennCenters(size.width, size.height, setCount)
        val step = 9f
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                val mask = maskAt(Offset(x, y), size.width, size.height, setCount)
                if (mask in highlighted) drawCircle(SetAmber.copy(.45f), step * .48f, Offset(x, y))
                x += step
            }
            y += step
        }
        drawRect(SetMuted.copy(.7f), style = Stroke(2f))
        centers.forEachIndexed { index, center ->
            val color = listOf(SetCyan, SetPink, SetGreen)[index]
            drawCircle(color.copy(.08f), radius, center)
            drawCircle(color, radius, center, style = Stroke(5f))
            drawContext.canvas.nativeCanvas.drawText(('A'.code + index).toChar().toString(), center.x, center.y - radius + 30f, android.graphics.Paint().apply {
                this.color = color.toArgb(); textSize = 30f; isFakeBoldText = true
            })
        }
        if (showElements) {
            universe.sorted().forEach { element ->
                if (element == dragged) return@forEach
                drawElementChip(element, positionFor(element, IntSize(size.width.toInt(), size.height.toInt())), element == selectedElement)
            }
            val moving = dragged
            val movingPosition = dragPosition
            if (moving != null && movingPosition != null) drawElementChip(moving, movingPosition, true)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElementChip(value: String, center: Offset, selected: Boolean) {
    val width = (value.length * 9f + 26f).coerceIn(38f, 100f)
    drawRoundRect(if (selected) SetViolet else Color(0xEE11233D), Offset(center.x - width / 2, center.y - 15f), Size(width, 30f), androidx.compose.ui.geometry.CornerRadius(12f))
    drawRoundRect(if (selected) SetPink else SetCyan.copy(.8f), Offset(center.x - width / 2, center.y - 15f), Size(width, 30f), androidx.compose.ui.geometry.CornerRadius(12f), style = Stroke(2f))
    drawContext.canvas.nativeCanvas.drawText(value.take(10), center.x, center.y + 6f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
    })
}

private fun vennCenters(width: Float, height: Float, setCount: Int): List<Offset> =
    if (setCount == 2) listOf(Offset(width * .38f, height * .52f), Offset(width * .62f, height * .52f))
    else listOf(Offset(width * .38f, height * .42f), Offset(width * .62f, height * .42f), Offset(width * .5f, height * .66f))

private fun maskAt(point: Offset, width: Float, height: Float, setCount: Int): Int {
    val radius = min(width, height) * if (setCount == 2) .29f else .255f
    val centers = vennCenters(width, height, setCount)
    return (if ((point - centers[0]).getDistance() <= radius) 4 else 0) or
        (if ((point - centers[1]).getDistance() <= radius) 2 else 0) or
        (if (setCount == 3 && (point - centers[2]).getDistance() <= radius) 1 else 0)
}

private fun regionAnchor(mask: Int, width: Float, height: Float): Offset = when (mask) {
    0 -> Offset(width * .1f, height * .12f)
    1 -> Offset(width * .5f, height * .82f)
    2 -> Offset(width * .74f, height * .39f)
    3 -> Offset(width * .64f, height * .64f)
    4 -> Offset(width * .26f, height * .39f)
    5 -> Offset(width * .36f, height * .64f)
    6 -> Offset(width * .5f, height * .36f)
    else -> Offset(width * .5f, height * .54f)
}

private fun moveElementToMask(current: EditableSets, element: String, mask: Int): EditableSets {
    fun moved(source: String, bit: Int): String {
        val values = SetTheoryStudioEngine.parseElements(source).filterNot { it == element }.toMutableList()
        if (mask and bit != 0) values += element
        return values.distinct().joinToString(", ")
    }
    val universe = (SetTheoryStudioEngine.parseElements(current.universe) + element).distinct().joinToString(", ")
    return current.copy(universe = universe, a = moved(current.a, 4), b = moved(current.b, 2), c = moved(current.c, 1))
}

@Composable
private fun SetIntroductionPanel() {
    var included by rememberSaveable { mutableStateOf(setOf("2", "4", "6")) }
    val candidates = listOf("1", "2", "3", "4", "5", "6")
    SetPanelCard(SetCyan) {
        Text("A set is a well-defined collection of distinct objects.", color = SetInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Tap values to create the set of even numbers.", color = SetMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            candidates.forEach { SetChip(it, it in included, SetCyan) { included = if (it in included) included - it else included + it } }
        }
        Text("E = ${SetTheoryStudioEngine.roster(included.sorted())}", color = SetGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("${included.size} distinct elements · order and repeated entries do not change a set.", color = SetMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SetTypesPanel(sets: EditableSets, onSets: (EditableSets) -> Unit) {
    val demos = listOf(
        "Empty set" to EditableSets("1, 2, 3", "", "1, 2", ""),
        "Singleton set" to EditableSets("1, 2, 3", "1", "2, 3", ""),
        "Equal sets" to EditableSets("1, 2, 3", "1, 2, 3", "3, 2, 1", ""),
        "Equivalent sets" to EditableSets("1, 2, 3, a, b, c", "1, 2, 3", "a, b, c", ""),
        "Disjoint sets" to EditableSets("1, 2, 3, 4", "1, 2", "3, 4", ""),
        "Overlapping sets" to EditableSets("1, 2, 3", "1, 2", "2, 3", ""),
    )
    val a = sets.parsed().getValue("A"); val b = sets.parsed().getValue("B")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        demos.forEach { (name, sample) -> SetButton(name.uppercase(), SetViolet) { onSets(sample) } }
    }
    SetPanelCard(SetViolet) {
        Text("A = ${SetTheoryStudioEngine.roster(a)}", color = SetCyan)
        Text("B = ${SetTheoryStudioEngine.roster(b)}", color = SetPink)
        Text(
            when {
                a.isEmpty() -> "A is empty because |A| = 0."
                a.size == 1 -> "A is a singleton because |A| = 1."
                a == b -> "A = B. Order is irrelevant because both contain exactly the same elements."
                a.size == b.size -> "A and B are equivalent because they have equal cardinality."
                a.intersect(b).isEmpty() -> "A and B are disjoint because A ∩ B = ∅."
                else -> "A and B overlap in ${SetTheoryStudioEngine.roster(a.intersect(b))}."
            },
            color = SetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SetRepresentationPanel(sets: EditableSets, onSets: (EditableSets) -> Unit, onVenn: () -> Unit) {
    OutlinedTextField(sets.a, { onSets(sets.copy(a = it)) }, Modifier.fillMaxWidth(), label = { Text("Finite set A") })
    val a = sets.parsed().getValue("A")
    SetPanelCard(SetGreen) {
        Text("ROSTER", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text("A = ${SetTheoryStudioEngine.roster(a)}", color = SetInk, fontSize = 16.sp)
        Text("SET-BUILDER", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(validatedBuilderForm(a), color = SetInk, fontSize = 15.sp, fontFamily = FontFamily.Serif)
        Text("VERBAL", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(validatedVerbalForm(a), color = SetMuted)
        SetButton("OPEN VENN REPRESENTATION", SetCyan, onVenn)
    }
}

private fun validatedBuilderForm(values: Set<String>): String {
    val numbers = values.mapNotNull(String::toIntOrNull).sorted()
    return when {
        numbers.size == values.size && numbers.isNotEmpty() && numbers.all { it % 2 == 0 } ->
            "A = {x ∈ ℤ | x is even and ${numbers.first()} ≤ x ≤ ${numbers.last()}}"
        numbers.size == values.size && numbers.zipWithNext().all { it.second - it.first == 1 } ->
            "A = {x ∈ ℤ | ${numbers.first()} ≤ x ≤ ${numbers.last()}}"
        else -> "A = {x ∈ U | x is one of ${values.sorted().joinToString()}}"
    }
}
private fun validatedVerbalForm(values: Set<String>): String = "A is the set containing ${values.sorted().joinToString().ifBlank { "no elements" }}."

@Composable
private fun SubsetPanel(sets: EditableSets, onSets: (EditableSets) -> Unit) {
    val a = sets.parsed().getValue("A")
    var candidate by rememberSaveable { mutableStateOf(setOf<String>()) }
    OutlinedTextField(sets.a, { onSets(sets.copy(a = it)); candidate = emptySet() }, Modifier.fillMaxWidth(), label = { Text("Parent set A") })
    Text("Select elements to build candidate subset B.", color = SetMuted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        a.sorted().forEach { element -> SetChip(element, element in candidate, SetGreen) { candidate = if (element in candidate) candidate - element else candidate + element } }
    }
    SetPanelCard(SetGreen) {
        Text("B = ${SetTheoryStudioEngine.roster(candidate)}", color = SetInk, fontSize = 16.sp)
        Text("B ⊆ A: ${SetTheoryStudioEngine.isSubset(candidate, a)}", color = SetCyan, fontWeight = FontWeight.Bold)
        Text("B ⊂ A: ${SetTheoryStudioEngine.isProperSubset(candidate, a)}", color = SetViolet, fontWeight = FontWeight.Bold)
        if (a.size <= 10) Text("${SetTheoryStudioEngine.powerSet(a.toList()).size} subsets available. Open Power Sets to inspect all of them.", color = SetMuted, fontSize = 10.sp)
        else Text("|P(A)| = 2^${a.size}. Full generation is safely limited.", color = SetAmber, fontSize = 10.sp)
    }
}

@Composable
private fun PowerSetPanel(sets: EditableSets) {
    val a = sets.parsed().getValue("A")
    var selectedMask by rememberSaveable { mutableIntStateOf(0) }
    SetPanelCard(SetViolet) {
        Text("A = ${SetTheoryStudioEngine.roster(a)}", color = SetInk)
        Text("|P(A)| = 2^${a.size}", color = SetViolet, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
    if (a.size > 10) {
        Text("This set is too large to generate every subset. Reduce A to 10 elements or fewer.", color = SetAmber)
    } else {
        val subsets = remember(a) { SetTheoryStudioEngine.powerSet(a.sorted()) }
        subsets.groupBy { it.size }.forEach { (cardinality, group) ->
            Text("CARDINALITY $cardinality", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                group.forEach { subset ->
                    val mask = a.sorted().indices.fold(0) { value, index -> if (a.sorted()[index] in subset) value or (1 shl index) else value }
                    SetChip("${mask.toString(2).padStart(a.size, '0')} ${SetTheoryStudioEngine.roster(subset)}", selectedMask == mask, SetViolet) { selectedMask = mask }
                }
            }
        }
    }
}

@Composable
private fun CartesianPanel(sets: EditableSets) {
    val a = sets.parsed().getValue("A").sorted(); val b = sets.parsed().getValue("B").sorted()
    var reverse by rememberSaveable { mutableStateOf(false) }
    val left = if (reverse) b else a; val right = if (reverse) a else b
    val product = SetTheoryStudioEngine.cartesianProduct(left, right)
    SetChip(if (reverse) "B × A" else "A × B", true, SetCyan) { reverse = !reverse }
    SetPanelCard(SetCyan) {
        Text("|${if (reverse) "B × A" else "A × B"}| = ${product.size}", color = SetCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(product.joinToString(prefix = "{", postfix = "}") { "(${it.first},${it.second})" }, color = SetInk, fontFamily = FontFamily.Monospace)
        Text("In general A × B ≠ B × A because ordered-pair position matters.", color = SetMuted, fontSize = 10.sp)
    }
    if (a.isNotEmpty() && b.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().border(1.dp, SetViolet.copy(.4f), RoundedCornerShape(14.dp)).padding(8.dp)) {
            Row { Spacer(Modifier.width(70.dp)); right.forEach { Text(it, color = SetPink, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp)) } }
            left.forEach { x -> Row { Text(x, color = SetCyan, modifier = Modifier.width(70.dp)); right.forEach { y -> Text("($x,$y)", color = SetInk, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp).padding(4.dp)) } } }
        }
    }
}

@Composable
private fun SetLawsPanel(openWorkspace: (Int, String) -> Unit) {
    val laws = listOf(
        "Commutative union" to ("A ∪ B" to "B ∪ A"),
        "Associative union" to ("A ∪ (B ∪ C)" to "(A ∪ B) ∪ C"),
        "Distributive intersection" to ("A ∩ (B ∪ C)" to "(A ∩ B) ∪ (A ∩ C)"),
        "De Morgan union" to ("(A ∪ B)'" to "A' ∩ B'"),
        "De Morgan intersection" to ("(A ∩ B)'" to "A' ∪ B'"),
        "Absorption" to ("A ∪ (A ∩ B)" to "A"),
        "Double complement" to ("(A')'" to "A"),
    )
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val law = laws[selected]
    val left = SetExpressionParser.parse(law.second.first).expression
    val right = SetExpressionParser.parse(law.second.second).expression
    laws.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth().background(if (selected == index) SetViolet.copy(.13f) else SetPanel, RoundedCornerShape(14.dp)).clickable { selected = index }.padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(item.first, color = SetInk, fontWeight = FontWeight.Bold); Text("${item.second.first} = ${item.second.second}", color = SetMuted, fontSize = 11.sp) }
            Text(if (left != null && right != null && SetLawVerifier.equivalent(left, right)) "VERIFIED" else "CHECK", color = SetGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
    SetButton("VISUALIZE LEFT SIDE", SetCyan) { openWorkspace(3, law.second.first) }
}

@Composable
private fun InclusionExclusionPanel(sets: EditableSets, onSets: (EditableSets) -> Unit) {
    var three by rememberSaveable { mutableStateOf(false) }
    val p = sets.parsed(); val a = p.getValue("A"); val b = p.getValue("B"); val c = p.getValue("C")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SetChip("TWO SETS", !three, SetAmber) { three = false }; SetChip("THREE SETS", three, SetAmber) { three = true }
    }
    OutlinedTextField(sets.a, { onSets(sets.copy(a = it)) }, Modifier.fillMaxWidth(), label = { Text("Set A") })
    OutlinedTextField(sets.b, { onSets(sets.copy(b = it)) }, Modifier.fillMaxWidth(), label = { Text("Set B") })
    if (three) OutlinedTextField(sets.c, { onSets(sets.copy(c = it)) }, Modifier.fillMaxWidth(), label = { Text("Set C") })
    SetPanelCard(SetAmber) {
        if (!three) {
            Text("|A ∪ B| = |A| + |B| - |A ∩ B|", color = SetInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${a.size} + ${b.size} - ${a.intersect(b).size} = ${SetTheoryStudioEngine.inclusionExclusion(a, b)}", color = SetAmber, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("The intersection was counted once in A and once in B, so subtract one copy.", color = SetMuted)
        } else {
            Text("|A ∪ B ∪ C| = singles - pair overlaps + triple overlap", color = SetInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${a.size}+${b.size}+${c.size} - ${a.intersect(b).size}-${a.intersect(c).size}-${b.intersect(c).size} + ${a.intersect(b).intersect(c).size} = ${SetTheoryStudioEngine.inclusionExclusion(a, b, c)}", color = SetAmber, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Add individual sets, remove pairwise double-counts, then restore the triple region.", color = SetMuted)
        }
    }
}

@Composable
private fun RelationsPanel(sets: EditableSets) {
    val a = sets.parsed().getValue("A"); val b = sets.parsed().getValue("B")
    val product = SetTheoryStudioEngine.cartesianProduct(a.sorted(), b.sorted())
    var relation by remember { mutableStateOf(product.filterIndexed { index, _ -> index % 2 == 0 }.toSet()) }
    Text("Tap ordered pairs to build relation R ⊆ A × B.", color = SetMuted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        product.forEach { pair -> SetChip("(${pair.first},${pair.second})", pair in relation, SetGreen) { relation = if (pair in relation) relation - pair else relation + pair } }
    }
    SetPanelCard(SetGreen) {
        Text("R = ${relation.joinToString(prefix = "{", postfix = "}") { "(${it.first},${it.second})" }}", color = SetInk, fontFamily = FontFamily.Monospace)
        Text("Domain used: ${SetTheoryStudioEngine.roster(relation.map { it.first }.toSet())}", color = SetCyan)
        Text("Range: ${SetTheoryStudioEngine.roster(relation.map { it.second }.toSet())}", color = SetPink)
    }
}

@Composable
private fun GuidedSetChallenges(openWorkspace: (Int, String) -> Unit) {
    val challenges = listOf(
        "Identify the union" to "A ∪ B",
        "Shade elements in A but not B" to "A - B",
        "Shade outside both sets" to "(A ∪ B)'",
        "Apply De Morgan's law" to "A' ∩ B'",
        "Find exactly two of three sets" to "(A ∩ B ∩ C') ∪ (A ∩ C ∩ B') ∪ (B ∩ C ∩ A')",
        "Find elements in at least one set" to "A ∪ B ∪ C",
    )
    var index by rememberSaveable { mutableIntStateOf(0) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var answer by rememberSaveable { mutableStateOf("") }
    val challenge = challenges[index]
    SetPanelCard(SetPink) {
        Text("OBJECTIVE ${index + 1} OF ${challenges.size}", color = SetPink, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(challenge.first, color = SetInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (!attempted) Text("Enter an expression before validation reveals the result.", color = SetMuted)
        OutlinedTextField(answer, { answer = it }, Modifier.fillMaxWidth(), label = { Text("Your set expression") })
        SetButton("VALIDATE", SetPink) { attempted = true }
        if (attempted) {
            val actual = SetExpressionParser.parse(answer).expression
            val expected = SetExpressionParser.parse(challenge.second).expression
            val correct = actual != null && expected != null && SetLawVerifier.equivalent(actual, expected)
            Text(if (correct) "Correct. The membership regions match." else "Not yet. Hint: translate each word into membership conditions.", color = if (correct) SetGreen else SetAmber, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SetButton("EXPLORE VISUALLY", SetCyan) { openWorkspace(if ("C" in challenge.second) 3 else 2, answer.ifBlank { challenge.second }) }
                SetButton("NEXT", SetGreen) { index = (index + 1) % challenges.size; attempted = false; answer = "" }
            }
        }
    }
}

@Composable
private fun SavedSetWorkspaces(vm: ExplorerViewModel, onRestore: (Triple<EditableSets, Int, String>) -> Unit) {
    if (vm.savedSetTheoryWorkspaces.isEmpty()) {
        SetPanelCard(SetViolet) { Text("No saved Set Theory workspaces yet.", color = SetInk, fontWeight = FontWeight.Bold); Text("Open a Venn workspace and tap Save.", color = SetMuted) }
    }
    vm.savedSetTheoryWorkspaces.forEach { saved ->
        val decoded = decodeSetWorkspace(saved)
        if (decoded != null) SetPanelCard(SetViolet) {
            Text(saved.substringBefore('|'), color = SetInk, fontWeight = FontWeight.Bold)
            Text("${decoded.second} sets · ${decoded.third}", color = SetMuted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SetButton("RESTORE", SetGreen) { onRestore(decoded) }
                SetButton("DELETE", SetRed) { vm.deleteSetTheoryWorkspace(saved) }
            }
        }
    }
}

private fun encodeSetWorkspace(sets: EditableSets, count: Int, expression: String): String {
    val name = "Set workspace ${System.currentTimeMillis().toString().takeLast(6)}"
    fun safe(value: String) = value.replace("|", " ").replace("~", " ")
    return "$name|$count|${safe(expression)}|${safe(sets.universe)}~${safe(sets.a)}~${safe(sets.b)}~${safe(sets.c)}"
}

private fun decodeSetWorkspace(source: String): Triple<EditableSets, Int, String>? = runCatching {
    val parts = source.split("|", limit = 5)
    val values = parts[4].split("~", limit = 4)
    Triple(EditableSets(values[0], values[1], values[2], values[3]), parts[1].toInt(), parts[2])
}.getOrNull()

private fun pageTitle(page: SetModulePage): String = when (page) {
    SetModulePage.Hub -> "Set Theory"
    SetModulePage.Introduction -> "Introduction to Sets"
    SetModulePage.Types -> "Types of Sets"
    SetModulePage.Representation -> "Set Representation"
    SetModulePage.Workspace -> "Interactive Venn Workspace"
    SetModulePage.Subsets -> "Subsets & Supersets"
    SetModulePage.PowerSet -> "Power Set Explorer"
    SetModulePage.Cartesian -> "Cartesian Product"
    SetModulePage.Laws -> "Set Laws Laboratory"
    SetModulePage.InclusionExclusion -> "Inclusion–Exclusion"
    SetModulePage.Relations -> "Relations & Functions"
    SetModulePage.Guided -> "Guided Challenges"
    SetModulePage.Saved -> "Saved Workspaces"
}

@Composable
private fun SetPanelCard(accent: Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.copy(.14f), SetPanel)), RoundedCornerShape(17.dp)).border(1.dp, accent.copy(.42f), RoundedCornerShape(17.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
private fun SetIcon(text: String, accent: Color) {
    Box(Modifier.size(40.dp).background(accent.copy(.14f), RoundedCornerShape(12.dp)).border(1.dp, accent.copy(.58f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(text, color = accent, fontSize = if (text.length > 3) 10.sp else 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SetButton(text: String, accent: Color, onClick: () -> Unit) {
    Text(text, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.heightIn(min = 48.dp).background(accent.copy(.09f), RoundedCornerShape(10.dp)).border(1.dp, accent.copy(.58f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp))
}

@Composable
private fun SetChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(text, color = if (selected) SetSpace else accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.heightIn(min = 48.dp).background(if (selected) accent else accent.copy(.07f), RoundedCornerShape(10.dp)).border(1.dp, accent.copy(.52f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp))
}

@Composable
private fun SetMetric(label: String, value: String, accent: Color) {
    Column(Modifier.width(130.dp).background(accent.copy(.1f), RoundedCornerShape(11.dp)).border(1.dp, accent.copy(.35f), RoundedCornerShape(11.dp)).padding(8.dp)) {
        Text(label, color = SetMuted, fontSize = 8.sp); Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}
