package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs

internal val firstLightMathGameIds = setOf("calculus-climber", "chance", "combinatorics", "data", "data-story",
    "decimal-harbor", "balance", "fractions", "speed-basic", "speed-advanced")
internal val secondLightMathGameIds = setOf("function-machine", "shapes", "geometry-proof", "integer-expedition", "logic",
    "math-market", "kitchen", "matrix-mission", "number-bonds", "forge")
internal val lightMathGameIds = firstLightMathGameIds + secondLightMathGameIds
private val Paper = Color(0xFFF8FBFF)
private val Navy = Color(0xFF101D46)
private val Slate = Color(0xFF60789C)
private val Line = Color(0xFFDDE7F2)
private val Sky = Color(0xFF1677FF)
private val Ice = Color(0xFFEAF5FF)
private val Cream = Color(0xFFFFF6DA)
private val Mint = Color(0xFFE0F6EF)
internal val LocalMathBoardAccent = staticCompositionLocalOf { Color(0xFF1677FF) }

@Composable
internal fun LightMathFrame(game: MathsGame, level: Int?, time: String, score: Int, streak: Int,
    progress: Float, topic: String, prompt: String, onBack: () -> Unit, onSettings: () -> Unit,
    onHint: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    if (game.id in glossyMathGameIds) {
        GlossyMathFrame(game, level, time, score, streak, progress, topic, prompt, onBack, onSettings, onHint, content)
        return
    }
    IllustratedMathFrame(game,level,time,score,streak,progress,topic,prompt,onBack,onSettings,onHint,content)
}

@Composable
internal fun LightLearningGame(game: MathsGame, round: WorkshopChallenge, level: Int, solved: Boolean,
    showHint: Boolean, hints: Boolean, onHint: () -> Unit, onBack: () -> Unit, onSettings: () -> Unit,
    onComplete: () -> Unit, onNext: () -> Unit) {
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(solved) { while (!solved) { delay(1000); elapsed++ } }
    val prompt = round.playerPrompt()
    LightMathFrame(game, level + 1, "${elapsed / 60}:${(elapsed % 60).toString().padStart(2, '0')}", score, streak,
        if (level >= 120) 1f else (level / 30 + 1) / 4f, round.title, prompt, onBack, onSettings,
        if (hints) onHint else null) {
        if (showHint && hints) Text(round.hint, color = Slate, fontSize = 12.sp)
        key(game.id, round.signature) {
            MathWorkshopBoard(game, round, solved, onAttempt = { if (!it) streak = 0 }) {
                score++; streak++; onComplete()
            }
        }
        if (solved) {
            MathMissionDebrief(game.id,round)
            LightAction(if (level >= 120) "New mastery mission" else "Next level", primary = true, modifier = Modifier.fillMaxWidth(), onClick = onNext)
        }
    }
}

private class DropArea {
    var bounds by mutableStateOf(Rect.Zero)
    var hovering by mutableStateOf(false)
}

@Composable
private fun DropBoard(area: DropArea, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val Sky = LocalMathBoardAccent.current
    val glossy=LocalGlossyMath.current
    Box(modifier.fillMaxWidth().heightIn(min = 72.dp).onGloballyPositioned { area.bounds = it.boundsInRoot() }
        .semantics { contentDescription = "Construction drop zone" }
        .clip(RoundedCornerShape(13.dp)).background(Sky.copy(if(area.hovering) .12f else .025f))
        .drawBehind { drawRoundRect(if (area.hovering) Sky else Sky.copy(.3f), style = Stroke(1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx())) }
        .padding(if(glossy)8.dp else 10.dp), contentAlignment = Alignment.Center, content = content)
}

@Composable
private fun LightTile(label: String, description: String, area: DropArea?, enabled: Boolean, modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val accent=LocalMathBoardAccent.current
    val glossy=LocalGlossyMath.current
    val category=mathTokenCategory(label)
    val effectiveTint=category?.fill ?: if(tint==Color.Unspecified)accent.copy(.09f) else tint
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    val place by rememberUpdatedState(onClick)
    val available by rememberUpdatedState(enabled)
    Box(modifier.heightIn(min = if(glossy && !LocalCompactWorldControls.current)44.dp else 42.dp).onGloballyPositioned { bounds = it.boundsInRoot() }
        .graphicsLayer { translationX = drag.x; translationY = drag.y; alpha = if (enabled) 1f else .4f }
        .then(if(glossy)Modifier.shadow(4.dp,RoundedCornerShape(10.dp),spotColor=Color(0xFF9BB5D6)) else Modifier)
        .clip(RoundedCornerShape(10.dp)).background(if(glossy)Brush.linearGradient(listOf(Color.White,effectiveTint.compositeOver(Color.White)))else Brush.linearGradient(listOf(effectiveTint,effectiveTint)))
        .border(1.dp, category?.ink?.copy(.3f) ?: if (tint == Cream) Color(0xFFFFDA76) else accent.copy(.25f), RoundedCornerShape(10.dp))
        .semantics { contentDescription = description; category?.let{stateDescription="${it.label} tile"}; role = Role.Button; if (!enabled) disabled() }
        .pointerInput(area, enabled, label) {
            if (enabled && area != null) detectDragGestures(
                onDragStart = { touch = bounds.topLeft + it },
                onDrag = { change, amount -> change.consume(); drag += amount; touch += amount; area.hovering = area.bounds.contains(touch) },
                onDragEnd = { val valid = area.bounds.contains(touch); drag = Offset.Zero; area.hovering = false; if (valid && available) place() },
                onDragCancel = { drag = Offset.Zero; area.hovering = false })
        }.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 5.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        if(glossy && label.startsWith("🤖")) {
            val lines=label.lines();val robot=(lines.getOrNull(1)?.firstOrNull()?.code?:65)-65
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Image(painterResource(glossyRobots[robot.coerceIn(0,4)]),null,Modifier.size(56.dp,if(LocalCompactWorldControls.current)24.dp else 44.dp))
                Text(lines.drop(1).joinToString("\n"),color=Navy,fontSize=14.sp,lineHeight=if(LocalCompactWorldControls.current)14.sp else 17.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            }
        } else if(glossy && label=="⌨") GlossyIcon("keyboard",color=Navy)
        else Text(label, color = category?.ink ?: Navy, fontSize = if(glossy)17.sp else 14.sp, fontWeight = if(glossy)FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun LightAction(label: String, primary: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val Sky=LocalMathBoardAccent.current
    if(LocalGlossyMath.current) {
        val shape=RoundedCornerShape(15.dp)
        val ink=if(!enabled)Color(0xFF91A3C0) else if(primary)Color.White else Navy
        Row(modifier.heightIn(min=if(LocalCompactWorldControls.current)44.dp else 48.dp).then(if(enabled)Modifier.shadow(if(primary)8.dp else 4.dp,shape,spotColor=if(primary)Sky else Color(0xFFAABBD0))else Modifier)
            .clip(shape).background(Brush.linearGradient(if(primary && enabled)listOf(lerp(Sky,Color.White,.25f),Sky,lerp(Sky,Color.Black,.1f))else if(enabled)listOf(Color.White,Color(0xFFF0F5FC))else listOf(Color(0xFFE9EFF7),Color(0xFFE4EBF5))))
            .border(1.dp,if(primary && enabled)Color(0xFF8BD8FF)else Color.White,shape)
            .clickable(enabled=enabled,onClick=onClick).semantics{role=Role.Button;if(!enabled)disabled()}.padding(horizontal=6.dp,vertical=9.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            val icon=when {label=="Run"->"run";label.startsWith("Undo")->"undo";label.startsWith("Check")->"check";label=="Collect"->"score";else->null}
            if(icon!=null){GlossyIcon(icon,Modifier.size(19.dp),ink);Spacer(Modifier.width(6.dp))}
            Text(label,color=ink,fontSize=if(LocalCompactWorldControls.current)12.sp else 13.sp,fontWeight=FontWeight.Bold)
        }
        return
    }
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) Sky else Color(0xFFEDF2F8),
            contentColor = if (primary) Color.White else Navy, disabledContainerColor = Color(0xFFF0F3F7), disabledContentColor = Color(0xFF8D9BB0)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun LightWorkshopBoard(game: MathsGame, round: WorkshopChallenge, solved: Boolean,
    onAttempt: (Boolean) -> Unit, onSolved: () -> Unit) {
    val Sky=LocalMathBoardAccent.current
    var selection by rememberSaveable { mutableStateOf(arrayListOf<Int>()) }
    var assembly by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var marks by rememberSaveable { mutableStateOf(arrayListOf<Int>()) }
    var status by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    val area = remember { DropArea() }
    fun changed() { result = null; status = ""; checked=false }
    fun token(value: String) { if (!solved && assembly.size < 80) { assembly = ArrayList(assembly + value); changed() } }
    fun add(value: Int) { if (!solved) { selection = ArrayList(selection + value); changed() } }
    fun check() {
        if (solved) return
        val valid = round.accepts(selection, assembly, marks)
        onAttempt(valid)
        status = round.attemptFeedback(selection,assembly,marks)
        checked=true
        if (valid) onSolved()
    }
    when (round.kind) {
        WorkshopKind.Expression -> {
            DropBoard(area) {
                if (assembly.isEmpty()) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("☝", color = Slate, fontSize = 23.sp)
                    Text("Drag numbers and operators here", color = Slate, fontSize = 11.sp)
                } else FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    assembly.forEachIndexed { index, value -> LightTile(value, "Remove token ${index + 1}", null, !solved,
                        tint = Color(0xFFE3EDFF)) { assembly = ArrayList(assembly.filterIndexed { i, _ -> i != index }); changed() } }
                }
            }
            LightExpressionPalette(game, round, area, !solved, ::token) { assembly = arrayListOf(); changed() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LightAction("Run", primary = true, enabled = !solved, modifier = Modifier.weight(1f)) {
                    val built = evaluateMathAssembly(assembly)
                    result = built?.let { roundNumber(it.value) }
                    status = if (built == null) "Complete the expression: check brackets and missing operators." else "Your method produces ${roundNumber(built.value)}"
                }
                LightAction("Undo", enabled = !solved && assembly.isNotEmpty(), modifier = Modifier.weight(1f)) { assembly = ArrayList(assembly.dropLast(1)); changed() }
                LightAction("Check", enabled = !solved && assembly.isNotEmpty(), modifier = Modifier.weight(1f), onClick = ::check)
            }
            if (result != null) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F4FA)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Result", color = Slate, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("=  ${result ?: "—"}", color = Sky, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        WorkshopKind.Pieces -> {
            val source = round.source!!
            val unit = round.parameters.firstOrNull() ?: 1
            val fraction = unit > 1
            val shapes = source.interaction == RoundInteraction.BuildShapes
            DropBoard(area) {
                if (!fraction) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (selection.isEmpty()) {
                            Text(if (shapes) "△  □  ○" else if(game.id == "math-market") "🛒" else "＋", color = Sky, fontSize = 25.sp)
                            Text(if (shapes) "Drag shapes into your blueprint" else "Drag or tap a core to begin", color = Slate, fontSize = 11.sp)
                        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            selection.forEachIndexed { index, value ->
                                LightTile(if(shapes) shapeSymbol(value) else "$value", "Remove piece ${index+1}", null, !solved,
                                    Modifier.widthIn(min = 34.dp), Mint) { selection = ArrayList(selection.filterIndexed { i, _ -> i != index }); changed() }
                            }
                        }
                    }
                } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                        val capacity = maxOf(unit, kotlin.math.ceil(maxOf(source.answer, selection.sum().toDouble()) / unit).toInt() * unit)
                        drawRect(Color(0xFFEAF4FF))
                        drawRect(Color(0xFF81BDFF), size = Size(size.width * (selection.sum().toFloat() / capacity).coerceIn(0f, 1f), size.height))
                        for (i in 0..capacity) { val x = size.width * i / capacity; drawLine(if(i % unit == 0) Sky else Color(0xFFAACCEE), Offset(x, 0f), Offset(x, size.height), (if(i % unit == 0) 2.dp else .5.dp).toPx()) }
                    }
                    Text("Filled ${selection.sum()}/$unit • ${selection.size} tiles", color = Slate, fontSize = 12.sp)
                }
                }
            }
            if (!fraction) Text(if (shapes) "${selection.size} pieces · ${selection.sum()} sides · ${selection.count { it==0 }} curved"
                else "Total ${selection.sum()} · ${selection.size}${if(source.requiredPieces>0) "/${source.requiredPieces}" else ""} cores", color = Slate, fontSize = 12.sp)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                source.pieces.distinct().forEachIndexed { index, value ->
                    val gcd = gcd(value, unit)
                    val colors = listOf(Color(0xFFFFE7EC), Ice, Mint, Color(0xFFFFEDDB))
                    val label = if (shapes) shapeSymbol(value) else if(fraction) "${value / gcd}\n―\n${unit / gcd}" else "$value"
                    val available = if (shapes) selection.size < source.requiredPieces else selection.count { it == value } < source.pieces.count { it == value }
                    LightTile(label, if(shapes) "Add piece with $value straight sides" else if(fraction) "Add $value/$unit core" else "Add $value core", area,
                        !solved && available, Modifier.widthIn(min = if(fraction)60.dp else 44.dp), colors[index % colors.size]) { add(value) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LightAction("Undo", enabled = !solved && selection.isNotEmpty(), modifier = Modifier.weight(1f)) { selection = ArrayList(selection.dropLast(1)); changed() }
                LightAction("Clear", enabled = !solved && selection.isNotEmpty(), modifier = Modifier.weight(1f)) { selection = arrayListOf(); changed() }
                LightAction("Check build", primary = true, enabled = !solved && selection.isNotEmpty(), modifier = Modifier.weight(1.4f), onClick = ::check)
            }
        }
        WorkshopKind.Navigation -> {
            val p = round.parameters
            val x = p[0] + selection.sumOf { round.moves[it].first }
            val y = p[1] + selection.sumOf { round.moves[it].second }
            val line = game.id == "integer-expedition"
            DropBoard(area) {
                Canvas(Modifier.fillMaxWidth().height(if (line) 85.dp else 110.dp)) {
                    val points = mutableListOf(p[0] to p[1])
                    selection.forEach { index -> val previous=points.last(); points += previous.first+round.moves[index].first to previous.second+round.moves[index].second }
                    val all = points + (p[2] to p[3])
                    val minX=all.minOf { it.first }-2; val maxX=all.maxOf { it.first }+2
                    val minY=all.minOf { it.second }-2; val maxY=all.maxOf { it.second }+2
                    val inset=16.dp.toPx()
                    fun pos(point: Pair<Int,Int>) = Offset(inset+(point.first-minX).toFloat()/(maxX-minX)*(size.width-2*inset),
                        if(line) size.height/2 else size.height-inset-(point.second-minY).toFloat()/(maxY-minY)*(size.height-2*inset))
                    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(96,120,156);textSize=10.sp.toPx();textAlign=android.graphics.Paint.Align.CENTER }
                    fun text(value:String,point:Offset) { drawContext.canvas.nativeCanvas.drawText(value, point.x, point.y, paint) }
                    if(line) {
                        drawLine(Line,Offset(inset,size.height/2),Offset(size.width-inset,size.height/2),2.dp.toPx())
                        val step=maxOf(1,kotlin.math.ceil((maxX-minX)/10.0).toInt())
                        for(i in minX..maxX step step) {val point=pos(i to 0);drawLine(Line,point-Offset(0f,4.dp.toPx()),point+Offset(0f,4.dp.toPx()));text("$i",point+Offset(0f,20.dp.toPx()))}
                    } else {
                        repeat(9) { i -> val xx=inset+i*(size.width-2*inset)/8;val yy=inset+i*(size.height-2*inset)/8
                            drawLine(Line,Offset(xx,inset),Offset(xx,size.height-inset));drawLine(Line,Offset(inset,yy),Offset(size.width-inset,yy)) }
                    }
                    points.zipWithNext().forEach { (from,to) -> drawLine(Sky,pos(from),pos(to),3.dp.toPx()) }
                    drawCircle(Sky,4.dp.toPx(),pos(points.first()))
                    drawCircle(Color(0xFFFFAF2A),7.dp.toPx(),pos(p[2] to p[3]),style=Stroke(2.dp.toPx()))
                    drawCircle(Sky,6.dp.toPx(),pos(x to y))
                    if(!line) {text("Start (${p[0]},${p[1]})",Offset(size.width*.25f,10.sp.toPx()));text("Target (${p[2]},${p[3]})",Offset(size.width*.75f,10.sp.toPx()))}
                }
            }
            Text(if(line) "Position $x · ${selection.size}/${p[4]} moves" else "Ship ($x,$y) · ${selection.size}/${p[4]} moves",color=Navy,fontSize=12.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                round.moves.forEachIndexed { index,move -> LightTile(if(line) "${if(move.first>0)"+" else ""}${move.first}" else "(${move.first},${move.second})",
                    "Move by (${move.first},${move.second})",area,!solved && selection.size<p[4],Modifier.weight(1f)) {add(index)} }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                LightAction("Undo move",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1f)) {selection=ArrayList(selection.dropLast(1));changed()}
                LightAction("Land ship",primary=true,enabled=!solved,modifier=Modifier.weight(1f),onClick=::check)
            }
        }
        WorkshopKind.Machine -> {
            var operand by rememberSaveable { mutableStateOf(roundNumber(round.numbers.first())) }
            DropBoard(area) {
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if(LocalCompactWorldControls.current)2.dp else 5.dp)) {
                    Row(Modifier.fillMaxWidth()) {listOf("Input","Target","Your circuit").forEach {Text(it,color=Slate,fontSize=10.sp,lineHeight=14.sp,textAlign=TextAlign.Center,modifier=Modifier.weight(1f))}}
                    round.examples.forEach { (input,target) ->
                        Row(Modifier.fillMaxWidth()) {
                            listOf(roundNumber(input),roundNumber(target),if(assembly.isEmpty())"—" else round.machineOutput(input,assembly)?.let(::roundNumber)?:"Undefined").forEachIndexed { index,value ->
                                Text(value,color=if(index==2)Sky else Navy,fontSize=13.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,modifier=Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if(assembly.isNotEmpty()) FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                assembly.chunked(2).forEachIndexed { index,block -> LightTile(if(block[0]=="²")"x²" else block.joinToString(" "),"Remove block ${index+1}",null,!solved,
                    tint=Mint) {assembly=ArrayList(assembly.filterIndexed { i,_ -> i/2!=index });changed()} }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                round.numbers.distinct().forEach { value -> LightTile(roundNumber(value),"Select core ${roundNumber(value)}",area,!solved,Modifier.weight(1f),
                    if(operand==roundNumber(value))Color(0xFFCDE4FF) else Ice) {operand=roundNumber(value)} }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Core $operand · ${assembly.size/2}/6 blocks",color=Slate,fontSize=12.sp,modifier=Modifier.weight(1f))
                LightAction("−1",enabled=!solved) {operand=roundNumber(operand.toDouble()-1)}
                Spacer(Modifier.width(5.dp))
                LightAction("+1",enabled=!solved) {operand=roundNumber(operand.toDouble()+1)}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("+","×","²").forEach { op -> LightTile(if(op=="²")"x²" else "$op core","Add $op block",area,!solved && assembly.size<12,Modifier.weight(1f),Cream) {
                    assembly=ArrayList(assembly+listOf(op,if(op=="²")"2" else operand));changed()
                } }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                LightAction("Undo block",enabled=!solved && assembly.isNotEmpty(),modifier=Modifier.weight(1f)) {assembly=ArrayList(assembly.dropLast(2));changed()}
                LightAction("Test circuit",primary=true,enabled=!solved,modifier=Modifier.weight(1f),onClick=::check)
            }
        }
        WorkshopKind.Balance -> {
            val state = round.balanceState(assembly)!!
            var operand by rememberSaveable { mutableStateOf(roundNumber(round.numbers[0])) }
            DropBoard(area) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        LightPlate("${roundNumber(state[0])}x + ${roundNumber(state[1])}")
                        Text("=", color = Sky, fontSize = 26.sp)
                        LightPlate(if (abs(state[2]) < 1e-8) roundNumber(state[3]) else "${roundNumber(state[2])}x + ${roundNumber(state[3])}")
                    }
                    Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                        drawLine(Color(0xFF9DB0C5), Offset(size.width * .15f, 0f), Offset(size.width * .85f, 0f), 4.dp.toPx())
                        val path = Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width / 2 - 12.dp.toPx(), size.height); lineTo(size.width / 2 + 12.dp.toPx(), size.height); close() }
                        drawPath(path, Slate)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                round.numbers.take(4).distinct().forEach { number -> LightTile(roundNumber(number), "Select core ${roundNumber(number)}", area, !solved,
                    Modifier.weight(1f), if (operand == roundNumber(number)) Color(0xFFCDE4FF) else Ice) { operand = roundNumber(number) } }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Core $operand", color = Slate, fontSize = 12.sp, modifier = Modifier.weight(1f))
                LightAction("−1", enabled = !solved) { operand = roundNumber(operand.toDouble() - 1) }
                Spacer(Modifier.width(5.dp))
                LightAction("+1", enabled = !solved) { operand = roundNumber(operand.toDouble() + 1) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("+", "−", "÷", "−x").forEach { op -> LightTile(if (op == "−x") "− core·x" else op, "Apply $op to both sides", area,
                    !solved && assembly.size < 32, Modifier.weight(if (op == "−x") 1.8f else 1f), Cream) {
                    val next = ArrayList(assembly + listOf(op, operand))
                    if (round.balanceState(next) != null) { assembly = next; changed() } else status = "Choose a non-zero divisor."
                } }
            }
            if (assembly.isNotEmpty()) Text(assembly.chunked(2).joinToString(" → ") { it.joinToString(" ") }, color = Slate, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LightAction("Undo block", enabled = !solved && assembly.isNotEmpty(), modifier = Modifier.weight(1f)) { assembly = ArrayList(assembly.dropLast(2)); changed() }
                LightAction("Unlock", primary = true, enabled = !solved, modifier = Modifier.weight(1f), onClick = ::check)
            }
        }
        WorkshopKind.Configurations -> {
            // The formation board is below the palette; dropping in any other area cancels the move.
            FlowRow(Modifier.fillMaxWidth().zIndex(2f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(round.parameters[0]) { index ->
                    val color = listOf(Ice, Color(0xFFFFE8E5), Mint, Color(0xFFFFEED8), Color(0xFFEFE5FF))[index % 5]
                    LightTile("🤖\n${('A'.code + index).toChar()}" + (round.numbers.getOrNull(index)?.let { "\n${roundNumber(it)}" } ?: ""),
                        "Choose robot ${('A'.code + index).toChar()}", area,
                        !solved && index !in selection && selection.size < round.parameters[1], Modifier.width(if(LocalGlossyMath.current)(if(round.parameters[0]<=4)76 else 58).dp else 48.dp), color) { add(index) }
                }
            }
            DropBoard(area) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Your formation", color = Slate, fontSize = 11.sp,lineHeight=14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeat(round.parameters[1]) { slot -> Box(Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(Ice)
                            .border(1.dp, Line, RoundedCornerShape(8.dp))
                            .clickable(enabled=!solved && slot<selection.size){selection=ArrayList(selection.filterIndexed{i,_->i!=slot});changed()}
                            .semantics{contentDescription="Formation slot ${slot+1}"}, contentAlignment = Alignment.Center) {
                            Text(selection.getOrNull(slot)?.let { ('A'.code + it).toChar().toString() } ?: if(LocalGlossyMath.current)"+" else "·", color = Sky, fontSize = 18.sp)
                        } }
                    }
                    Text("Drag or tap robots to build a team", color = Slate, fontSize = 10.sp,lineHeight=14.sp)
                }
            }
            Text("Collected ${assembly.size} distinct formations", color = Navy, fontSize = 12.sp)
            if (assembly.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                assembly.takeLast(8).forEach { formation ->
                    val name = formation.split(',').joinToString("") { ('A'.code + it.toInt()).toChar().toString() }
                    LightTile(name, "Remove formation $name", null, !solved) { assembly = ArrayList(assembly - formation); changed() }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LightAction("Undo", enabled = !solved && (selection.isNotEmpty() || assembly.isNotEmpty()), modifier = Modifier.weight(1f)) {
                    if (selection.isNotEmpty()) selection = ArrayList(selection.dropLast(1)) else assembly = ArrayList(assembly.dropLast(1)); changed()
                }
                LightAction("Collect", enabled = !solved && selection.size == round.parameters[1], modifier = Modifier.weight(1f)) {
                    val key = (if (round.parameters[2] == 0) selection.sorted() else selection).joinToString(",")
                    if (!round.validConfiguration(selection)) status = "This formation does not meet the question rules. Try another team." else if (key in assembly) status = "Formation already collected. Try another." else { assembly = ArrayList(assembly + key); selection = arrayListOf(); changed() }
                }
                LightAction("Check", primary = true, enabled = !solved, modifier = Modifier.weight(1f), onClick = ::check)
            }
        }
        WorkshopKind.Sort -> {
            DropBoard(area) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Place observations, then tap slots to mark", color = Slate, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        selection.forEachIndexed { slot, index -> LightTile(roundNumber(round.numbers[index]), "Mark slot ${slot + 1}", null, !solved,
                            Modifier.width(38.dp), if (slot in marks) Cream else Ice) { marks = ArrayList(if (slot in marks) marks - slot else marks + slot) } }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                round.numbers.forEachIndexed { index, value -> LightTile(roundNumber(value), "Place observation ${index + 1}: ${roundNumber(value)}", area,
                    !solved && index !in selection, Modifier.width(40.dp)) { add(index) } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LightAction("Undo slot", enabled = !solved && selection.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    selection = ArrayList(selection.dropLast(1)); marks = ArrayList(marks.filter { it < selection.size }); changed()
                }
                LightAction("Verify evidence", primary = true, enabled = !solved, modifier = Modifier.weight(1.5f), onClick = ::check)
            }
        }
        else -> error("Unsupported light board ${round.kind}")
    }
    if (status.isNotEmpty()) MathFeedbackCue(status,status=="Construction verified")
    if(checked && status.isNotEmpty()) MathSolutionFeedback(round,game.id)
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)
private fun shapeSymbol(sides: Int): String = when(sides) {0 -> "○";3 -> "△";4 -> "□";5 -> "⬠";else -> "⬡"}

@Composable
private fun LightPlate(label: String) {
    Box(Modifier.widthIn(min = 90.dp, max = 140.dp).clip(RoundedCornerShape(10.dp)).background(Ice)
        .border(1.dp, Color(0xFFB8D7FF), RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LightExpressionPalette(game: MathsGame, round: WorkshopChallenge, area: DropArea, enabled: Boolean,
    onToken: (String) -> Unit, onClear: () -> Unit) {
    val Sky=LocalMathBoardAccent.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var custom by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    val operands = round.source?.operands ?: round.numbers
    val paletteNumbers = when (game.id) {
        "data", "data-story" -> operands + operands.size.toDouble()
        "calculus-climber" -> operands + listOf(0.0, 1.0, 2.0, 3.0)
        "chance" -> if (round.mastery > 0) operands else operands + listOf(0.0, 1.0, 2.0, 100.0)
        else -> round.numbers
    }.distinct()
    // Generated operands stay one-tap accessible. The keypad adds new values without concatenating separate operands.
    val numberTiles: @Composable () -> Unit = {
        paletteNumbers.forEach { value -> LightTile(roundNumber(value), "Place number ${roundNumber(value)}", area, enabled,
            Modifier.widthIn(min = 34.dp)) { onToken(roundNumber(value)) } }
        LightTile("⌨", "Custom core", null, enabled, Modifier.width(36.dp)) { editing = !editing; tab = 0 }
    }
    if(LocalGlossyMath.current) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=1.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){numberTiles()}
    else FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){numberTiles()}
    Row(Modifier.fillMaxWidth().then(if(LocalGlossyMath.current)Modifier.glossySurface(15)else Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFEDF2F8))), verticalAlignment = Alignment.CenterVertically) {
        listOf("Numbers", "Functions", "Operators", "Symbols").forEachIndexed { index, label ->
            val category=listOf(MathTokenCategory.Number,MathTokenCategory.Function,MathTokenCategory.Operator,MathTokenCategory.Symbol)[index]
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp)).background(if (tab == index) category.ink else category.fill.copy(.5f))
                .clickable { tab = index }.semantics { selected = tab == index }, contentAlignment = Alignment.Center) {
                Text(label, color = if (tab == index) Color.White else category.ink, fontSize = 10.sp)
            }
        }
        Box(Modifier.width(42.dp).height(36.dp).clickable(enabled = enabled, onClick = onClear), contentAlignment = Alignment.Center) {
            Text("Clear", color = Slate, fontSize = 10.sp)
        }
    }
    if (tab == 0) {
        // Compact number editor expands only when needed, keeping the initial phone board short.
        if (editing) {
            listOf(listOf("0", "1", "2", "3", "4", "⌫"), listOf("5", "6", "7", "8", "9", ".")).forEach { digits ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    digits.forEach { digit -> LightTile(digit, "Enter digit $digit", null, enabled, Modifier.weight(1f)) {
                        custom = if (digit == "⌫") custom.dropLast(1) else if (custom.length < 14) custom + digit else custom
                    } }
                }
            }
            LightAction("Place ${custom.ifBlank { "number" }}", enabled = enabled && custom.toDoubleOrNull()?.isFinite() == true, modifier = Modifier.fillMaxWidth()) {
                onToken(custom); custom = ""; editing = false
            }
        }
    }
    if (tab == 1 || (game.id == "speed-advanced" && tab == 0)) {
        val functions = when (game.id) {
            "calculus-climber" -> listOf("^", "√", "π", "e")
            "chance" -> listOf("C", "P", "!", "√")
            else -> listOf("sin", "cos", "tan", "√", "π", "e")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            functions.forEach { function -> LightTile(function, "Place operation $function", area, enabled,
                Modifier.weight(1f)) { onToken(function) } }
        }
    }
    if (tab == 3) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf("^", "√", "C", "P", "!").forEach { op -> LightTile(op, "Place operation $op", area, enabled, Modifier.weight(1f), Cream) { onToken(op) } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (listOf("+", "−", "×", "÷", "(", ")") + if(game.id=="calculus-climber") listOf("^", "√") else emptyList()).forEach { op -> LightTile(op, "Place operation $op", area, enabled, Modifier.weight(1f), if(game.id in themedMathGameIds)Color(0xFFF4F2F8) else Cream) { onToken(op) } }
    }
}
