package com.indianservers.aiexplorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.DiscoverySequenceEngine
import com.indianservers.aiexplorer.core.DiscoveryStage
import com.indianservers.aiexplorer.core.FormulaDiscoveryState
import com.indianservers.aiexplorer.core.FormulaDifficulty
import com.indianservers.aiexplorer.core.FormulaEvaluationEngine
import com.indianservers.aiexplorer.core.FormulaVisualModel
import com.indianservers.aiexplorer.core.VisualFormulaDefinition
import com.indianservers.aiexplorer.core.VisualFormulaRegistry
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

private val VfSpace = Color(0xFF01050F)
private val VfPanel = Color(0xEE091426)
private val VfInk = Color(0xFFF4F8FF)
private val VfMuted = Color(0xFFA9B6CF)
private val VfCyan = Color(0xFF24DFFF)
private val VfViolet = Color(0xFFA76AFF)
private val VfGreen = Color(0xFF4BE5A1)
private val VfAmber = Color(0xFFFFB63C)
private val VfPink = Color(0xFFFF65A4)

private val FormulaDiscoveryStateSaver = listSaver<FormulaDiscoveryState, String>(
    save = {
        listOf(
            it.formulaId, it.currentStage.name,
            it.parameterValues.entries.joinToString(";") { entry -> "${entry.key}=${entry.value}" },
            it.investigatedCases.toString(), it.prediction, it.proofStepIndex.toString(),
            it.formulaAssembled.toString(), it.explanationCompleted.toString(),
            it.independentChallengeCompleted.toString(), it.hintsUsed.toString(),
        )
    },
    restore = { values ->
        FormulaDiscoveryState(
            formulaId = values[0],
            currentStage = DiscoveryStage.valueOf(values[1]),
            parameterValues = values[2].split(";").mapNotNull { item ->
                val key = item.substringBefore("=")
                item.substringAfter("=", "").toDoubleOrNull()?.let { key to it }
            }.toMap(),
            investigatedCases = values[3].toInt(),
            prediction = values[4],
            proofStepIndex = values[5].toInt(),
            formulaAssembled = values[6].toBoolean(),
            explanationCompleted = values[7].toBoolean(),
            independentChallengeCompleted = values[8].toBoolean(),
            hintsUsed = values[9].toInt(),
        )
    },
)

@Composable
internal fun VisualFormulaDiscoveryModule(vm: ExplorerViewModel, wide: Boolean) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val favourites = vm.favouriteVisualFormulaIds
    val completed = vm.completedVisualFormulaIds
    BackHandler {
        if (selectedId != null) selectedId = null else vm.returnToMathMenu()
    }
    selectedId?.let { id ->
        VisualFormulaActivity(
            formula = VisualFormulaRegistry.find(id) ?: return@let,
            wide = wide,
            completed = id in completed,
            onBack = { selectedId = null },
            onComplete = { vm.completeVisualFormula(id) },
        )
        return
    }
    val results = remember(query, category) { VisualFormulaRegistry.search(query, category) }
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF161E4A), VfSpace, Color.Black), radius = 1400f))
            .verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 26.dp else 9.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            VfButton("← BACK", VfCyan) { vm.returnToMathMenu() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VISUAL FORMULA LAB", color = VfInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("OBSERVE · MANIPULATE · DISCOVER", color = VfViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text("${completed.size}/20", color = VfGreen, fontWeight = FontWeight.Black)
        }
        VfPanelCard(VfViolet) {
            Text("Don’t memorise the answer—make it inevitable.", color = VfInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Every available card has a working native activity with live variables, prediction, transformation, formula assembly, explanation and an independent test.", color = VfMuted, fontSize = 11.sp)
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().semantics { contentDescription = "Search visual formulas by name or symbol" }, label = { Text("Search formulas and symbols") }, placeholder = { Text("triangle area, a²+b², mean") }, singleLine = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            VfChip("ALL", category == null, VfCyan) { category = null }
            VisualFormulaRegistry.formulas.map { it.category }.distinct().forEach { item ->
                VfChip(item.uppercase(), category == item, VfViolet) { category = if (category == item) null else item }
            }
        }
        Text("${results.size} OPERATIONAL DISCOVERIES", color = VfGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 760.dp) 3 else 2
            val gap = 8.dp
            val cardWidth = (maxWidth - gap * (columns - 1)) / columns
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                results.forEach { formula ->
                    val accent = formulaAccent(formula)
                    Column(
                        Modifier.width(cardWidth).heightIn(min = 165.dp)
                            .background(Brush.linearGradient(listOf(accent.copy(.2f), VfPanel)), RoundedCornerShape(18.dp))
                            .border(1.dp, accent.copy(.55f), RoundedCornerShape(18.dp))
                            .clickable { selectedId = formula.id }
                            .semantics { contentDescription = "Open ${formula.title}. ${formula.symbolicExpression}. ${if (formula.id in completed) "Completed" else "Not started"}" }
                            .padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(Modifier.size(39.dp).background(accent.copy(.16f), RoundedCornerShape(11.dp)).border(1.dp, accent, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                                Text(formulaIcon(formula), color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Text(if (formula.id in completed) "✓ DONE" else "${formula.minutes} MIN", color = if (formula.id in completed) VfGreen else accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Text(formula.title, color = VfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(formula.symbolicExpression, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("${formula.category} · ${formula.difficulty}", color = VfMuted, fontSize = 9.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("DISCOVER >", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text(if (formula.id in favourites) "★" else "☆", color = VfAmber, fontSize = 22.sp, modifier = Modifier.clickable {
                                vm.toggleVisualFormulaFavourite(formula.id)
                            }.semantics { contentDescription = "Toggle favourite ${formula.title}" })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun VisualFormulaActivity(
    formula: VisualFormulaDefinition,
    wide: Boolean,
    completed: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    var state by rememberSaveable(formula.id, stateSaver = FormulaDiscoveryStateSaver) { mutableStateOf(DiscoverySequenceEngine.start(formula)) }
    var prediction by rememberSaveable(formula.id) { mutableStateOf("") }
    var assembledText by rememberSaveable(formula.id) { mutableStateOf("") }
    var explanationChoice by rememberSaveable(formula.id) { mutableIntStateOf(-1) }
    var challengeAnswer by rememberSaveable(formula.id) { mutableStateOf("") }
    var feedback by rememberSaveable(formula.id) { mutableStateOf("") }
    var showFormula by rememberSaveable(formula.id) { mutableStateOf(false) }
    val proofEngine = remember { VisualProofEngine() }
    var proofPlayback by remember(formula.id) {
        mutableStateOf(formula.proofLabId?.let { proofEngine.start(it) })
    }
    val evaluation = remember(state.parameterValues) { FormulaEvaluationEngine.evaluate(formula, state.parameterValues) }
    LaunchedEffect(proofPlayback?.playing) {
        while (proofPlayback?.playing == true) {
            delay(850)
            proofPlayback = proofPlayback?.let(proofEngine::next)
        }
    }
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF161E4A), VfSpace), radius = 1200f))
            .verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 26.dp else 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            VfButton("← LAB", VfCyan, onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formula.title, color = VfInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("${state.currentStage.ordinal + 1}/7 · ${state.currentStage.name.uppercase()}", color = stageColor(state.currentStage), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            VfButton("RESET", VfAmber) {
                state = DiscoverySequenceEngine.start(formula); prediction = ""; assembledText = ""; explanationChoice = -1; challengeAnswer = ""; feedback = ""; showFormula = false
                proofPlayback = formula.proofLabId?.let { proofEngine.start(it) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DiscoveryStage.entries.forEach { stage ->
                Box(Modifier.weight(1f).height(5.dp).background(if (stage.ordinal <= state.currentStage.ordinal) stageColor(stage) else VfMuted.copy(.18f), RoundedCornerShape(5.dp)))
            }
        }
        VfPanelCard(stageColor(state.currentStage)) {
            Text("DISCOVERY PROMPT", color = stageColor(state.currentStage), fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(formula.prompts[state.currentStage.ordinal], color = VfInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (state.currentStage.ordinal < DiscoveryStage.Predict.ordinal)
                Text("The final formula stays hidden until you investigate at least three changes.", color = VfMuted, fontSize = 10.sp)
        }

        proofPlayback?.let { playback ->
            InteractiveVisualProofCanvas(playback) { name, value ->
                proofPlayback = proofEngine.setParameter(playback, name, value)
                state = DiscoverySequenceEngine.setParameter(state, formula, name, value)
            }
        } ?: GenericFormulaCanvas(formula, state.parameterValues, state.currentStage)

        VfPanelCard(VfCyan) {
            Text("VARIABLE CONTROLS", color = VfCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            formula.variables.forEach { variable ->
                val value = state.parameterValues[variable.symbol] ?: variable.defaultValue
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${variable.symbol} · ${variable.name}", color = VfInk, fontWeight = FontWeight.Bold)
                    Text(formatFormulaNumber(value), color = VfCyan)
                }
                Slider(value.toFloat(), {
                    state = DiscoverySequenceEngine.setParameter(state, formula, variable.symbol, it.toDouble())
                    proofPlayback = proofPlayback?.let { p -> proofEngine.setParameter(p, variable.symbol, it.toDouble()) }
                }, valueRange = variable.minimum.toFloat()..variable.maximum.toFloat(), steps = ((variable.maximum - variable.minimum).toInt() - 1).coerceAtLeast(0))
            }
            Text("Cases explored: ${state.investigatedCases} · live result ${formatFormulaNumber(evaluation.result)}", color = VfGreen, fontWeight = FontWeight.Bold)
        }

        when (state.currentStage) {
            DiscoveryStage.Experience, DiscoveryStage.Manipulate, DiscoveryStage.Notice -> {
                OutlinedTextField(prediction, { prediction = it }, Modifier.fillMaxWidth(), label = { Text(if (state.currentStage == DiscoveryStage.Notice) "What stayed the same?" else "Record what you notice") })
            }
            DiscoveryStage.Predict -> {
                OutlinedTextField(prediction, { prediction = it }, Modifier.fillMaxWidth(), label = { Text("Predict the missing relationship") })
                if (state.investigatedCases >= 2) VfButton("TEST MY PREDICTION", VfViolet) { showFormula = true; state = DiscoverySequenceEngine.advance(state) }
                else Text("Change a variable at least twice before testing.", color = VfAmber)
            }
            DiscoveryStage.Prove -> {
                proofPlayback?.let { playback ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VfButton("PREVIOUS", VfMuted) { proofPlayback = proofEngine.previous(playback) }
                        VfButton(if (playback.playing) "PAUSE" else "PLAY", VfViolet) { proofPlayback = proofEngine.togglePlaying(playback) }
                        VfButton("NEXT STEP", VfGreen) { proofPlayback = proofEngine.advance(playback) }
                    }
                }
                FormulaAssembly(formula, assembledText, { token ->
                    val candidate = if (assembledText.isBlank()) token else "$assembledText $token"
                    assembledText = candidate
                    if (FormulaEvaluationEngine.validateAssembly(formula, candidate.split(" "))) {
                        state = state.copy(formulaAssembled = true)
                        feedback = "Correct—the symbols match the visual transformation."
                    }
                }, { assembledText = ""; feedback = "" })
                Text("Your formula: ${assembledText.ifBlank { "tap blocks in order" }}", color = VfInk)
                if (feedback.isNotBlank()) Text(feedback, color = VfGreen, fontWeight = FontWeight.Bold)
            }
            DiscoveryStage.Explain -> {
                Text("Why does it work?", color = VfInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                formula.explanationOptions.forEachIndexed { index, option ->
                    VfChoice(option, explanationChoice == index, if (index == 0) VfGreen else VfViolet) {
                        explanationChoice = index
                        state = state.copy(explanationCompleted = index == formula.correctExplanation)
                        feedback = if (index == formula.correctExplanation) "Yes. This identifies the invariant—not just a numerical coincidence." else misconceptionFeedback(formula)
                    }
                }
                if (feedback.isNotBlank()) Text(feedback, color = if (state.explanationCompleted) VfGreen else VfAmber)
            }
            DiscoveryStage.Apply -> {
                val first = formula.variables.first()
                val challengeParameters = state.parameterValues + (first.symbol to ((state.parameterValues[first.symbol] ?: first.defaultValue) + 1.0).coerceAtMost(first.maximum))
                val challengeResult = FormulaEvaluationEngine.evaluate(formula, challengeParameters).result
                Text("Independent test: calculate the result after increasing the first control by one unit. Enter your prediction, then verify visually.", color = VfInk, fontWeight = FontWeight.Bold)
                OutlinedTextField(challengeAnswer, { challengeAnswer = it }, Modifier.fillMaxWidth(), label = { Text("Your numerical prediction") })
                VfButton("CHECK INDEPENDENT TEST", VfPink) {
                    val correct = challengeAnswer.toDoubleOrNull()?.let { kotlin.math.abs(it - challengeResult) < .02 } == true
                    state = state.copy(independentChallengeCompleted = correct)
                    feedback = if (correct) "Independent check passed. Discovery complete." else "Not yet. Recheck which quantities change and preserve the visual invariant."
                    if (correct) onComplete()
                }
                if (feedback.isNotBlank()) Text(feedback, color = if (state.independentChallengeCompleted) VfGreen else VfAmber)
            }
        }
        AnimatedVisibility(showFormula || state.currentStage.ordinal > DiscoveryStage.Predict.ordinal) {
            VfPanelCard(VfGreen) {
                Text("FORMULA DISCOVERED", color = VfGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(formula.symbolicExpression, color = VfInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(formula.spokenFormula, color = VfMuted, fontSize = 11.sp)
                Text("Tap a variable control to link each symbol to its highlighted visual quantity.", color = VfCyan, fontSize = 10.sp)
            }
        }
        if (state.currentStage != DiscoveryStage.Apply) {
            val canAdvance = when (state.currentStage) {
                DiscoveryStage.Manipulate, DiscoveryStage.Notice -> state.investigatedCases >= 2
                DiscoveryStage.Predict -> false
                DiscoveryStage.Prove -> state.formulaAssembled
                DiscoveryStage.Explain -> state.explanationCompleted
                else -> true
            }
            VfButton("CONTINUE TO ${DiscoveryStage.entries.getOrElse(state.currentStage.ordinal + 1) { DiscoveryStage.Apply }.name.uppercase()} →", if (canAdvance) VfGreen else VfMuted) {
                if (canAdvance) state = DiscoverySequenceEngine.advance(state) else feedback = "Complete this discovery step before continuing."
            }
        }
        if (completed) Text("✓ Previously completed", color = VfGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FormulaAssembly(formula: VisualFormulaDefinition, assembled: String, onToken: (String) -> Unit, onClear: () -> Unit) {
    VfPanelCard(VfViolet) {
        Text("ASSEMBLE THE FORMULA", color = VfViolet, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text("Accessible alternative to dragging: tap blocks in the correct order.", color = VfMuted, fontSize = 10.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            formula.assemblyTokens.shuffled(java.util.Random(formula.id.hashCode().toLong())).forEach { token -> VfButton(token, VfViolet) { onToken(token) } }
            VfButton("CLEAR", VfAmber, onClear)
        }
        Text(assembled, color = VfInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GenericFormulaCanvas(formula: VisualFormulaDefinition, p: Map<String, Double>, stage: DiscoveryStage) {
    Canvas(
        Modifier.fillMaxWidth().height(330.dp).background(Color(0xFF020A18), RoundedCornerShape(18.dp))
            .border(1.dp, VfCyan.copy(.55f), RoundedCornerShape(18.dp))
            .semantics { contentDescription = visualDescription(formula, p, stage) },
    ) {
        val w = size.width; val h = size.height
        fun value(key: String, fallback: Double = 1.0) = p[key] ?: fallback
        fun grid(cols: Int, rows: Int, split: Int? = null) {
            val cw = min((w * .72f / cols), (h * .66f / rows)); val left = (w - cw * cols) / 2; val top = (h - cw * rows) / 2
            repeat(rows) { y -> repeat(cols) { x ->
                val color = if (split != null && x >= split) VfPink else if ((x + y) % 2 == 0) VfCyan else VfViolet
                drawRect(color.copy(.35f), Offset(left + x * cw, top + y * cw), Size(cw - 2, cw - 2))
                drawRect(color, Offset(left + x * cw, top + y * cw), Size(cw - 2, cw - 2), style = Stroke(1.5f))
            } }
        }
        when (formula.visualModel) {
            FormulaVisualModel.Tiles -> {
                val cols = value(if (formula.id == "rectangle-area") "l" else if (formula.id == "multiply-array") "b" else "n").toInt().coerceIn(1,15)
                val rows = value(if (formula.id == "rectangle-area") "w" else if (formula.id == "multiply-array") "a" else "n").toInt().coerceIn(1,15)
                grid(cols, rows)
            }
            FormulaVisualModel.SplitArea -> grid((value("b")+value("c")).toInt().coerceAtLeast(2), value("a").toInt().coerceAtLeast(1), value("b").toInt())
            FormulaVisualModel.FractionGrid -> {
                val cols = value(if (formula.id == "fraction-product") "b" else "b").toInt().coerceIn(2,8)
                val rows = if (formula.id == "fraction-product") value("d").toInt().coerceIn(2,8) else value("k").toInt().coerceIn(1,5)
                val cw = min(w*.75f/cols,h*.65f/rows); val left=(w-cw*cols)/2; val top=(h-cw*rows)/2
                repeat(rows){y->repeat(cols){x-> val vertical=x<value("a").toInt(); val horizontal=formula.id!="fraction-product"||y<value("c").toInt(); val both=vertical&&horizontal
                    drawRect(if(both)VfGreen.copy(.65f) else if(vertical)VfCyan.copy(.25f) else if(horizontal)VfPink.copy(.25f) else VfPanel,Offset(left+x*cw,top+y*cw),Size(cw-2,cw-2));drawRect(VfMuted,Offset(left+x*cw,top+y*cw),Size(cw-2,cw-2),style=Stroke(1f))
                }}
            }
            FormulaVisualModel.AlgebraArea -> {
                val a=value("a",6.0).coerceAtLeast(value("b")); val b=value("b",2.0); val side=min(w*.62f,h*.68f); val cut=(a/(a+b)).toFloat()*side; val o=Offset((w-side)/2,(h-side)/2)
                drawRect(VfCyan.copy(.35f),o,Size(cut,cut));drawRect(VfPink.copy(.35f),o+Offset(cut,0f),Size(side-cut,cut));drawRect(VfPink.copy(.35f),o+Offset(0f,cut),Size(cut,side-cut));drawRect(VfViolet.copy(.38f),o+Offset(cut,cut),Size(side-cut,side-cut));drawRect(VfInk,o,Size(side,side),style=Stroke(3f))
            }
            FormulaVisualModel.Balance -> {
                val center=Offset(w*.5f,h*.55f);drawLine(VfInk,Offset(w*.2f,h*.48f),Offset(w*.8f,h*.48f),6f);drawLine(VfAmber,center,Offset(w*.5f,h*.82f),7f)
                repeat(value("a").toInt()){i->drawCircle(VfCyan,18f,Offset(w*.28f+i*38f,h*.41f))};repeat((value("c")/2).toInt().coerceAtMost(8)){i->drawCircle(VfPink,13f,Offset(w*.62f+(i%4)*30f,h*.42f-(i/4)*28f))}
            }
            FormulaVisualModel.Staircase -> {
                val n=value("n").toInt().coerceIn(2,15);val cell=min(w*.72f/n,h*.62f/n);val left=(w-cell*n)/2;val bottom=h*.82f
                repeat(n){x->repeat(x+1){y->drawRect(if(stage.ordinal>=DiscoveryStage.Prove.ordinal)VfGreen.copy(.5f) else VfViolet.copy(.55f),Offset(left+x*cell,bottom-(y+1)*cell),Size(cell-2,cell-2))}}
            }
            FormulaVisualModel.Venn -> {
                val r=min(w,h)*.27f;val a=Offset(w*.4f,h*.52f);val b=Offset(w*.6f,h*.52f);drawCircle(VfCyan.copy(.25f),r,a);drawCircle(VfPink.copy(.25f),r,b);drawCircle(VfCyan,r,a,style=Stroke(4f));drawCircle(VfPink,r,b,style=Stroke(4f));if(stage.ordinal>=DiscoveryStage.Prove.ordinal)drawCircle(VfAmber.copy(.45f),r*.52f,Offset(w*.5f,h*.52f))
            }
            FormulaVisualModel.Redistribution -> {
                val values=listOf(value("x₁"),value("x₂"),value("x₃"),value("x₄"));val max=12f;val bw=w*.12f;values.forEachIndexed{i,v->val target=if(stage.ordinal>=DiscoveryStage.Prove.ordinal)values.average() else v;drawRect(listOf(VfCyan,VfViolet,VfPink,VfGreen)[i].copy(.65f),Offset(w*.18f+i*w*.2f,h*.82f-(target/max).toFloat()*h*.62f),Size(bw,(target/max).toFloat()*h*.62f))}
            }
            else -> Unit
        }
    }
}

private fun visualDescription(formula: VisualFormulaDefinition, p: Map<String, Double>, stage: DiscoveryStage) =
    "${formula.title} interactive ${formula.visualModel.name} model. Stage ${stage.name}. " + p.entries.joinToString { "${it.key} ${formatFormulaNumber(it.value)}" }
private fun misconceptionFeedback(formula: VisualFormulaDefinition) = when (formula.id) {
    "triangle-area" -> "Use the perpendicular height, not the slanted side. Watch two copies form one parallelogram."
    "binomial-square" -> "The two a by b rectangles are visible; (a+b)² cannot omit 2ab."
    "probability-union" -> "The overlap is currently counted in both circles. It must be removed once."
    else -> "Compare the transformed objects: the valid explanation must preserve area, count, proportion, or balance for every tested case."
}
private fun formulaAccent(formula: VisualFormulaDefinition) = when (formula.category) {
    "Geometry" -> VfCyan; "Algebra" -> VfPink; "Fractions" -> VfGreen; "Probability", "Statistics" -> VfAmber; else -> VfViolet
}
private fun formulaIcon(formula: VisualFormulaDefinition) = when (formula.visualModel) {
    FormulaVisualModel.Tiles -> "▦"; FormulaVisualModel.FractionGrid -> "½"; FormulaVisualModel.Balance -> "="
    FormulaVisualModel.Circle -> "π"; FormulaVisualModel.Venn -> "∪"; FormulaVisualModel.Redistribution -> "x̄"
    FormulaVisualModel.Geometry -> "△"; FormulaVisualModel.Staircase -> "Σ"; else -> "Fx"
}
private fun stageColor(stage: DiscoveryStage) = listOf(VfCyan,VfViolet,VfAmber,VfPink,VfGreen,VfCyan,VfGreen)[stage.ordinal]
private fun formatFormulaNumber(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

@Composable private fun VfPanelCard(accent: Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.copy(.14f),VfPanel)),RoundedCornerShape(17.dp)).border(1.dp,accent.copy(.45f),RoundedCornerShape(17.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}
@Composable private fun VfButton(text: String, accent: Color, onClick: () -> Unit) {
    Text(text,color=accent,fontSize=9.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center,modifier=Modifier.heightIn(min=48.dp).background(accent.copy(.09f),RoundedCornerShape(10.dp)).border(1.dp,accent.copy(.58f),RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(horizontal=10.dp,vertical=12.dp))
}
@Composable private fun VfChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(text,color=if(selected)VfSpace else accent,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.heightIn(min=48.dp).background(if(selected)accent else accent.copy(.08f),RoundedCornerShape(10.dp)).border(1.dp,accent.copy(.5f),RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(horizontal=9.dp,vertical=12.dp))
}
@Composable private fun VfChoice(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(text,color=if(selected)VfInk else VfMuted,fontSize=12.sp,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).background(if(selected)accent.copy(.18f) else VfPanel,RoundedCornerShape(12.dp)).border(1.dp,if(selected)accent else VfMuted.copy(.25f),RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(12.dp))
}
