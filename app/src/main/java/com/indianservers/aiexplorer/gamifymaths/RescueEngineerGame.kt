package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI

private enum class EngineerRoute { Intro, Map, Play }
private enum class MeasureVisual { Ruler, Rectangle, Grid, Triangle, Circle, Volume }

private data class MeasureChallenge(
    val title: String,
    val prompt: String,
    val formula: String,
    val visual: MeasureVisual,
    val dimensions: List<Int>,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
)

private data class BridgeMission(
    val span: Int,
    val inventory: Map<Int, Int>,
    val minSegments: Int,
    val title: String,
)

internal data class RescueBridgeAssessment(
    val span: Int,
    val exactSpan: Boolean,
    val connected: Boolean,
    val strong: Boolean,
) {
    val success: Boolean get() = exactSpan && connected && strong
}

internal fun assessRescueBridge(targetSpan: Int, beams: List<Int>, joints: Int, minimumSegments: Int): RescueBridgeAssessment {
    val span = beams.sum()
    val connected = joints >= (beams.size - 1).coerceAtLeast(0)
    return RescueBridgeAssessment(
        span = span,
        exactSpan = span == targetSpan,
        connected = connected,
        strong = beams.size >= minimumSegments && connected,
    )
}

private val EngineerConcepts = listOf(
    GameLevel("Measure Lines", "Read rulers and compare lengths.", GameGold),
    GameLevel("Perimeter Basics", "Measure the distance around shapes.", GameBlue),
    GameLevel("Area Basics", "Count and calculate square units.", Color(0xFFFF8A43)),
    GameLevel("Area of Triangles", "Use one-half base times height.", GamePurple),
    GameLevel("Area of Circles", "Use radius and pi.", Color(0xFFE95F98)),
    GameLevel("Volume & Units", "Build volume and convert units.", Color(0xFF31C1C3)),
    GameLevel("Rescue Engineer", "Build strong bridges with exact spans.", GameGreen),
)

private val MeasureChallenges = listOf(
    MeasureChallenge("Measure Lines", "Read the ruler. How long is the beam?", "end − start", MeasureVisual.Ruler, listOf(1, 9), listOf("7 cm", "8 cm", "9 cm", "10 cm"), "8 cm", "Length is the ending mark minus the starting mark: 9 − 1 = 8 cm."),
    MeasureChallenge("Measure Lines", "Measure the cable from 2 cm to 11 cm.", "11 − 2", MeasureVisual.Ruler, listOf(2, 11), listOf("8 cm", "9 cm", "10 cm", "13 cm"), "9 cm", "11 − 2 = 9 cm."),
    MeasureChallenge("Measure Lines", "Which reading matches the pipe?", "14 − 3", MeasureVisual.Ruler, listOf(3, 14), listOf("10 cm", "11 cm", "12 cm", "17 cm"), "11 cm", "The pipe spans 14 − 3 = 11 cm."),
    MeasureChallenge("Perimeter Basics", "Find the perimeter of the rectangle.", "P = 2(l + b)", MeasureVisual.Rectangle, listOf(8, 5), listOf("18 m", "23 m", "26 m", "40 m"), "26 m", "P = 2(8 + 5) = 26 m."),
    MeasureChallenge("Perimeter Basics", "Find the perimeter of a 7 m by 4 m garden.", "P = 2(l + b)", MeasureVisual.Rectangle, listOf(7, 4), listOf("11 m", "18 m", "22 m", "28 m"), "22 m", "P = 2(7 + 4) = 22 m."),
    MeasureChallenge("Perimeter Basics", "A square has side 6 m. Find its perimeter.", "P = 4s", MeasureVisual.Rectangle, listOf(6, 6), listOf("12 m", "18 m", "24 m", "36 m"), "24 m", "A square has four equal sides: 4 × 6 = 24 m."),
    MeasureChallenge("Area Basics", "Count the squares to find the area.", "A = l × b", MeasureVisual.Grid, listOf(6, 4), listOf("16", "20", "24", "28"), "24", "Six columns times four rows gives 24 square units."),
    MeasureChallenge("Area Basics", "Find the area of a 7 by 3 panel.", "A = l × b", MeasureVisual.Grid, listOf(7, 3), listOf("10", "18", "21", "24"), "21", "7 × 3 = 21 square units."),
    MeasureChallenge("Area Basics", "Find the area of the square floor.", "A = s²", MeasureVisual.Grid, listOf(5, 5), listOf("10", "20", "25", "30"), "25", "5 × 5 = 25 square units."),
    MeasureChallenge("Area of Triangles", "Find the area of the triangle.", "A = ½ × b × h", MeasureVisual.Triangle, listOf(8, 6), listOf("12", "18", "24", "30"), "24", "One-half × 8 × 6 = 24 square units."),
    MeasureChallenge("Area of Triangles", "Base 10 m, height 5 m. Find the area.", "A = ½ × b × h", MeasureVisual.Triangle, listOf(10, 5), listOf("15", "20", "25", "50"), "25", "One-half × 10 × 5 = 25 square metres."),
    MeasureChallenge("Area of Triangles", "Base 12 cm, height 7 cm. Find the area.", "A = ½ × b × h", MeasureVisual.Triangle, listOf(12, 7), listOf("38", "42", "72", "84"), "42", "One-half × 12 × 7 = 42 square centimetres."),
    MeasureChallenge("Area of Circles", "Use π = 22/7. Radius is 7 cm.", "A = πr²", MeasureVisual.Circle, listOf(7), listOf("132", "144", "154", "168"), "154", "22/7 × 7 × 7 = 154 square centimetres."),
    MeasureChallenge("Area of Circles", "Use π ≈ 3.14. Radius is 5 cm.", "A = πr²", MeasureVisual.Circle, listOf(5), listOf("31.4", "62.8", "78.5", "100"), "78.5", "3.14 × 5 × 5 = 78.5 square centimetres."),
    MeasureChallenge("Area of Circles", "Use π ≈ 3.14. Diameter is 12 cm.", "r = d/2; A = πr²", MeasureVisual.Circle, listOf(6), listOf("37.68", "75.36", "113.04", "144"), "113.04", "The radius is 6 cm; 3.14 × 36 = 113.04."),
    MeasureChallenge("Volume & Units", "Find the volume of the box.", "V = l × b × h", MeasureVisual.Volume, listOf(4, 3, 2), listOf("9", "12", "18", "24"), "24", "4 × 3 × 2 = 24 cubic units."),
    MeasureChallenge("Volume & Units", "Convert 2.5 metres to centimetres.", "1 m = 100 cm", MeasureVisual.Ruler, listOf(0, 10), listOf("25 cm", "250 cm", "2,500 cm", "0.25 cm"), "250 cm", "2.5 × 100 = 250 centimetres."),
    MeasureChallenge("Volume & Units", "A cube has side 3 cm. Find its volume.", "V = s³", MeasureVisual.Volume, listOf(3, 3, 3), listOf("9", "18", "27", "36"), "27", "3 × 3 × 3 = 27 cubic centimetres."),
)

private val BridgeMissions = listOf(
    BridgeMission(6, mapOf(2 to 3, 3 to 2), 2, "Canyon Crossing"),
    BridgeMission(9, mapOf(2 to 3, 3 to 3, 4 to 1), 3, "River Rescue"),
    BridgeMission(12, mapOf(2 to 4, 3 to 2, 4 to 3), 3, "Mountain Base Rescue"),
)

@Composable
internal fun RescueEngineerGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    val total = MeasureChallenges.size + BridgeMissions.size
    var routeName by rememberSaveable { mutableStateOf(EngineerRoute.Intro.name) }
    var challengeIndex by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, total - 1)) }
    when (EngineerRoute.valueOf(routeName)) {
        EngineerRoute.Intro -> GameIntroScreen(
            number = 7,
            title = "Rescue Engineer",
            subtitle = "Build using exact measurements.",
            accent = Color(0xFFFF8B28),
            concepts = listOf("↔" to "Length", "□" to "Perimeter", "▦" to "Area", "△" to "Structures"),
            completed = completed,
            total = total,
            onBack = onBack,
            onStart = { challengeIndex = completed.coerceIn(0, total - 1); routeName = EngineerRoute.Play.name },
            onMap = { routeName = EngineerRoute.Map.name },
        )
        EngineerRoute.Map -> LevelMapScreen(
            "Measurement & Mensuration",
            EngineerConcepts,
            (completed / 3).coerceAtMost(EngineerConcepts.lastIndex),
            Color(0xFFFF8B28),
            { routeName = EngineerRoute.Intro.name },
        ) { concept ->
            val first = concept * 3
            challengeIndex = maxOf(first, completed.coerceAtMost(first + 2))
            routeName = EngineerRoute.Play.name
        }
        EngineerRoute.Play -> {
            if (challengeIndex < MeasureChallenges.size) {
                MeasurementPlayScreen(challengeIndex, { routeName = EngineerRoute.Map.name }) {
                    onComplete(challengeIndex + 1)
                    challengeIndex++
                }
            } else {
                val bridgeIndex = challengeIndex - MeasureChallenges.size
                BridgeBuilderScreen(bridgeIndex, { routeName = EngineerRoute.Map.name }) {
                    onComplete(challengeIndex + 1)
                    if (challengeIndex < total - 1) challengeIndex++ else routeName = EngineerRoute.Map.name
                }
            }
        }
    }
}

@Composable
private fun MeasurementPlayScreen(index: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val task = MeasureChallenges[index]
    val concept = index / 3
    val stage = index % 3
    val accent = EngineerConcepts[concept].accent
    var selected by rememberSaveable(index) { mutableStateOf("") }
    var result by rememberSaveable(index) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(index) { mutableStateOf(false) }
    GameScreen("${task.title} • ${stage + 1}/3", index + 1, accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(task.explanation, color = GameInk) }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MeasurementWorkspace(task, accent, Modifier.weight(1.15f))
                    MeasurementAnswerPanel(task, selected, accent, Modifier.weight(.85f)) { selected = it; result = null }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MeasurementWorkspace(task, accent)
                    MeasurementAnswerPanel(task, selected, accent) { selected = it; result = null }
                }
            }
        }
        PrimaryGameButton("Check Measurement", GameGreen, { result = selected == task.answer }, enabled = selected.isNotBlank())
        result?.let { ResultPanel(it, task.explanation, "Recheck the dimensions and formula, then choose another measurement.", onSolved) }
    }
}

@Composable
private fun MeasurementWorkspace(task: MeasureChallenge, accent: Color, modifier: Modifier = Modifier) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().heightIn(min = if (compact) 235.dp else 310.dp).background(
            Brush.radialGradient(listOf(accent.copy(.2f), Color(0xFF0B2856), GamePanel)),
            RoundedCornerShape(24.dp),
        ).border(1.dp, accent.copy(.7f), RoundedCornerShape(24.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(task.prompt, color = GameInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(task.formula, color = GameGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
        MeasurementDiagram(task.visual, task.dimensions, accent, Modifier.fillMaxWidth().height(if (compact) 155.dp else 220.dp))
    }
}

@Composable
private fun MeasurementAnswerPanel(task: MeasureChallenge, selected: String, accent: Color, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.6f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SELECT THE RESULT", color = GameMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Box(
            Modifier.fillMaxWidth().height(if (compact) 54.dp else 66.dp).background(Color.White.copy(.1f), RoundedCornerShape(15.dp)).border(1.dp, accent.copy(.55f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(selected.ifBlank { "?" }, color = if (selected.isBlank()) GameMuted else accent, fontSize = 26.sp, fontWeight = FontWeight.Black) }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            task.choices.forEachIndexed { index, choice ->
                DraggableGameTile(choice, listOf(GameBlue, GamePurple, GameGreen, GameGold)[index]) { onSelect(choice) }
            }
        }
        SecondaryGameButton("Clear answer", accent) { onSelect("") }
    }
}

@Composable
private fun MeasurementDiagram(visual: MeasureVisual, dimensions: List<Int>, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (visual) {
            MeasureVisual.Ruler -> {
                val start = dimensions[0]
                val end = dimensions[1]
                val left = 32f
                val right = size.width - 32f
                val y = size.height * .62f
                drawRoundRect(Color(0xFFFFC443), Offset(left, y), Size(right - left, 54f), androidx.compose.ui.geometry.CornerRadius(8f))
                val maxMark = maxOf(15, end + 1)
                repeat(maxMark + 1) { mark ->
                    val x = left + (right - left) * mark / maxMark
                    drawLine(GameSpace, Offset(x, y), Offset(x, y + if (mark % 5 == 0) 34f else 22f), if (mark % 5 == 0) 3f else 2f)
                }
                val x1 = left + (right - left) * start / maxMark
                val x2 = left + (right - left) * end / maxMark
                drawLine(accent, Offset(x1, y - 38f), Offset(x2, y - 38f), 13f, StrokeCap.Round)
                drawLine(Color.White, Offset(x1, y - 62f), Offset(x1, y - 8f), 3f)
                drawLine(Color.White, Offset(x2, y - 62f), Offset(x2, y - 8f), 3f)
            }
            MeasureVisual.Rectangle -> {
                val rect = Size(size.width * .62f, size.height * .55f)
                val top = Offset((size.width - rect.width) / 2f, size.height * .18f)
                drawRoundRect(GameGreen.copy(.65f), top, rect, androidx.compose.ui.geometry.CornerRadius(8f))
                drawRoundRect(Color.White, top, rect, androidx.compose.ui.geometry.CornerRadius(8f), style = Stroke(5f))
                drawContext.canvas.nativeCanvas.apply {
                    drawText("${dimensions[0]} m", size.width / 2f, top.y - 12f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER })
                    drawText("${dimensions[1]} m", top.x - 18f, top.y + rect.height / 2f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 26f; textAlign = android.graphics.Paint.Align.RIGHT })
                }
            }
            MeasureVisual.Grid -> {
                val cols = dimensions[0]
                val rows = dimensions[1]
                val cell = minOf((size.width - 60f) / cols, (size.height - 40f) / rows)
                val origin = Offset((size.width - cols * cell) / 2f, (size.height - rows * cell) / 2f)
                repeat(cols) { col -> repeat(rows) { row ->
                    drawRect(accent.copy(.85f), Offset(origin.x + col * cell, origin.y + row * cell), Size(cell - 2f, cell - 2f))
                } }
                drawRect(Color.White, origin, Size(cols * cell, rows * cell), style = Stroke(4f))
            }
            MeasureVisual.Triangle -> {
                val path = Path().apply {
                    moveTo(size.width / 2f, size.height * .12f)
                    lineTo(size.width * .86f, size.height * .85f)
                    lineTo(size.width * .14f, size.height * .85f)
                    close()
                }
                drawPath(path, GameGold.copy(.85f))
                drawPath(path, Color.White, style = Stroke(4f))
                drawLine(Color.White, Offset(size.width / 2f, size.height * .12f), Offset(size.width / 2f, size.height * .85f), 3f)
            }
            MeasureVisual.Circle -> {
                val radius = size.minDimension * .34f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(GameBlue.copy(.4f), radius, center)
                drawCircle(Color.White, radius, center, style = Stroke(5f))
                drawLine(GameGold, center, Offset(center.x + radius, center.y), 5f)
            }
            MeasureVisual.Volume -> {
                val w = size.width * .48f
                val h = size.height * .48f
                val origin = Offset(size.width * .24f, size.height * .3f)
                drawRect(GameBlue.copy(.65f), origin, Size(w, h))
                drawRect(Color.White, origin, Size(w, h), style = Stroke(4f))
                val shift = Offset(42f, -34f)
                drawRect(GameGreen.copy(.35f), origin + shift, Size(w, h), style = Stroke(4f))
                listOf(origin, origin + Offset(w, 0f), origin + Offset(0f, h), origin + Offset(w, h)).forEach {
                    drawLine(Color.White, it, it + shift, 4f)
                }
            }
        }
    }
}

@Composable
private fun BridgeBuilderScreen(stage: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val mission = BridgeMissions[stage]
    val beams = remember(stage) { mutableStateListOf<Int>() }
    var joints by rememberSaveable(stage) { mutableIntStateOf(0) }
    var selectedIndex by rememberSaveable(stage) { mutableIntStateOf(-1) }
    var tested by rememberSaveable(stage) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(stage) { mutableStateOf(false) }
    val assessment = assessRescueBridge(mission.span, beams, joints, mission.minSegments)
    val span = assessment.span
    val strong = assessment.strong
    val available: (Int) -> Int = { length -> (mission.inventory[length] ?: 0) - beams.count { it == length } }

    GameScreen("${mission.title} • ${stage + 1}/3", 19 + stage, Color(0xFFFF8B28), if (tested == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text("Match the exact span, then add a joint between every neighbouring beam. Three or more segments form a stronger truss.", color = GameInk) }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    BridgeCanvas(mission.span, beams, joints, selectedIndex, Modifier.weight(1.35f)) { selectedIndex = it }
                    BridgeControls(mission, beams, joints, span, strong, available, Modifier.weight(.65f),
                        onBeam = { length -> if (available(length) > 0) { beams += length; selectedIndex = beams.lastIndex; tested = null } },
                        onJoint = { if (joints < (beams.size - 1).coerceAtLeast(0)) joints++; tested = null },
                        onDelete = { if (selectedIndex in beams.indices) { beams.removeAt(selectedIndex); selectedIndex = -1; joints = joints.coerceAtMost((beams.size - 1).coerceAtLeast(0)); tested = null } },
                        onUndo = { if (beams.isNotEmpty()) beams.removeAt(beams.lastIndex) else if (joints > 0) joints--; selectedIndex = -1; tested = null },
                        onReset = { beams.clear(); joints = 0; selectedIndex = -1; tested = null },
                        onTest = { tested = assessment.success },
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BridgeCanvas(mission.span, beams, joints, selectedIndex) { selectedIndex = it }
                    BridgeControls(mission, beams, joints, span, strong, available,
                        onBeam = { length -> if (available(length) > 0) { beams += length; selectedIndex = beams.lastIndex; tested = null } },
                        onJoint = { if (joints < (beams.size - 1).coerceAtLeast(0)) joints++; tested = null },
                        onDelete = { if (selectedIndex in beams.indices) { beams.removeAt(selectedIndex); selectedIndex = -1; joints = joints.coerceAtMost((beams.size - 1).coerceAtLeast(0)); tested = null } },
                        onUndo = { if (beams.isNotEmpty()) beams.removeAt(beams.lastIndex) else if (joints > 0) joints--; selectedIndex = -1; tested = null },
                        onReset = { beams.clear(); joints = 0; selectedIndex = -1; tested = null },
                        onTest = { tested = assessment.success },
                    )
                }
            }
        }
        tested?.let {
            ResultPanel(
                it,
                "Bridge passed: exact ${mission.span} m span and ${beams.size} connected structural segments.",
                when {
                    span != mission.span -> "The bridge spans $span m, but the mission requires exactly ${mission.span} m."
                    !strong -> "The span is correct, but the structure needs a joint between every beam and at least ${mission.minSegments} segments."
                    else -> "Review the material placement and test again."
                },
                onSolved,
            )
        }
    }
}

@Composable
private fun BridgeCanvas(target: Int, beams: List<Int>, joints: Int, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().heightIn(min = if (compact) 300.dp else 390.dp).background(
            Brush.verticalGradient(listOf(Color(0xFF52BCEF), Color(0xFFBDEAFF), Color(0xFF1E73A4))),
            RoundedCornerShape(24.dp),
        ).border(2.dp, GameBlue, RoundedCornerShape(24.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("RESCUE ENGINEER", color = GameSpace, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Build an exact, connected bridge.", color = GameSpace, fontSize = 11.sp)
            }
            ScorePill("${beams.sum()} / $target m", if (beams.sum() == target) GameGreen else GameGold)
        }
        Box(Modifier.fillMaxWidth().height(if (compact) 215.dp else 300.dp).background(Color.White.copy(.18f), RoundedCornerShape(18.dp))) {
            Canvas(Modifier.matchParentSize()) {
                val deckY = size.height * .52f
                val left = 38f
                val usable = size.width - 76f
                drawRect(Color(0xFF4B3425), Offset(0f, deckY + 70f), Size(left, size.height))
                drawRect(Color(0xFF4B3425), Offset(size.width - left, deckY + 70f), Size(left, size.height))
                drawLine(Color.White, Offset(left, 40f), Offset(size.width - left, 40f), 3f)
                val pxPerMetre = usable / target.toFloat()
                var x = left
                beams.forEachIndexed { index, length ->
                    val end = x + length * pxPerMetre
                    val beamColor = if (index == selected) GameGold else Color(0xFF8B531F)
                    drawLine(beamColor, Offset(x, deckY), Offset(end, deckY), 14f, StrokeCap.Round)
                    drawLine(Color(0xFF49270F), Offset(x, deckY), Offset(end, deckY), 3f, StrokeCap.Round)
                    if (index < joints) {
                        val mid = (x + end) / 2f
                        val down = deckY + 82f
                        drawLine(Color(0xFF744018), Offset(x, deckY), Offset(mid, down), 10f, StrokeCap.Round)
                        drawLine(Color(0xFF744018), Offset(mid, down), Offset(end, deckY), 10f, StrokeCap.Round)
                        drawCircle(Color(0xFFDCE8F5), 10f, Offset(end, deckY))
                    }
                    x = end
                }
                drawCircle(Color(0xFFDCE8F5), 11f, Offset(left, deckY))
                drawLine(if (beams.sum() == target) GameGreen else GameRed, Offset(left, 64f), Offset(left + beams.sum().coerceAtMost(target) * pxPerMetre, 64f), 7f, StrokeCap.Round)
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                beams.forEachIndexed { index, length ->
                    Text(
                        "$length m",
                        color = if (index == selected) GameSpace else GameInk,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.background(if (index == selected) GameGold else GamePanel, CircleShape)
                            .clickable { onSelect(index) }.focusable().padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BridgeControls(
    mission: BridgeMission,
    beams: List<Int>,
    joints: Int,
    span: Int,
    strong: Boolean,
    available: (Int) -> Int,
    modifier: Modifier = Modifier,
    onBeam: (Int) -> Unit,
    onJoint: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onTest: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(22.dp)).border(1.dp, GameBlue.copy(.65f), RoundedCornerShape(22.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text("MISSION", color = GameGold, fontWeight = FontWeight.Black)
        Text("Build a strong bridge spanning exactly ${mission.span} m.", color = GameInk)
        ChecklistRow("Span = ${mission.span} m", span == mission.span)
        ChecklistRow("Strong bridge", strong)
        ChecklistRow("Joints ${joints}/${(beams.size - 1).coerceAtLeast(0)}", joints >= (beams.size - 1).coerceAtLeast(0))
        Text("BEAMS", color = GameBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mission.inventory.keys.sorted().forEach { length ->
                BeamTile(length, available(length), enabled = available(length) > 0) { onBeam(length) }
            }
        }
        PrimaryGameButton("Add Joint", GameBlue, onJoint, enabled = beams.size >= 2 && joints < beams.size - 1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MiniEngineerButton("Delete", GameRed, Modifier.weight(1f), onDelete)
            MiniEngineerButton("Undo", GameGold, Modifier.weight(1f), onUndo)
            MiniEngineerButton("Reset", GameBlue, Modifier.weight(1f), onReset)
        }
        PrimaryGameButton("Test Bridge", GameGreen, onTest, enabled = beams.isNotEmpty())
    }
}

@Composable
private fun BeamTile(length: Int, remaining: Int, enabled: Boolean, onAdd: () -> Unit) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier.width(96.dp).height(82.dp).graphicsLayer { translationX = dx; translationY = dy }
            .background(if (enabled) Color(0xFFEEE4D5) else GameMuted.copy(.12f), RoundedCornerShape(14.dp))
            .border(1.dp, if (enabled) GameGold else GameMuted.copy(.25f), RoundedCornerShape(14.dp))
            .pointerInput(enabled, length) {
                if (enabled) detectDragGestures(
                    onDragEnd = { val moved = dy < -24f || kotlin.math.abs(dx) > 45f; dx = 0f; dy = 0f; if (moved) onAdd() },
                    onDragCancel = { dx = 0f; dy = 0f },
                ) { change, amount -> change.consume(); dx += amount.x; dy += amount.y }
            }.clickable(enabled = enabled, onClick = onAdd).focusable(enabled).semantics { contentDescription = "$length metre beam, $remaining remaining" }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            drawLine(Color(0xFF8B531F), Offset(5f, size.height / 2f), Offset(size.width - 5f, size.height / 2f), 13f, StrokeCap.Round)
            drawLine(Color(0xFFFFC66D), Offset(8f, size.height / 2f - 3f), Offset(size.width - 8f, size.height / 2f - 3f), 3f, StrokeCap.Round)
        }
        Text("$length m ×$remaining", color = if (enabled) GameSpace else GameMuted, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ChecklistRow(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(22.dp).background(if (checked) GameGreen else Color.Transparent, CircleShape).border(1.dp, if (checked) GameGreen else GameMuted, CircleShape), contentAlignment = Alignment.Center) {
            Text(if (checked) "✓" else "", color = GameInk, fontWeight = FontWeight.Black)
        }
        Text(label, color = if (checked) GameGreen else GameInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniEngineerButton(label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).background(accent.copy(.25f), RoundedCornerShape(13.dp)).border(1.dp, accent.copy(.75f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).focusable().semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = GameInk, fontSize = 10.sp, fontWeight = FontWeight.Black) }
}
