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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ArchitectRoute { Intro, Map, Play }

private data class ArchitectChallenge(
    val title: String,
    val prompt: String,
    val targetLabel: String,
    val required: List<String>,
    val choices: List<String>,
    val explanation: String,
)

private val ArchitectConcepts = listOf(
    GameLevel("Identify Shapes", "Recognise shapes from their properties.", GameGreen),
    GameLevel("Squares & Rectangles", "Build quadrilaterals from pieces.", GameGold),
    GameLevel("Build Triangles", "Compose triangles from smaller shapes.", Color(0xFFFF795E)),
    GameLevel("Combine Shapes", "Build familiar objects from shapes.", GamePurple),
    GameLevel("Symmetry", "Complete shapes across a mirror line.", Color(0xFFE95F98)),
    GameLevel("Shape Challenge", "Use several geometry ideas together.", Color(0xFF32BFC1)),
)

private val ArchitectChallenges = buildList {
    listOf("square", "circle", "triangle").forEach { shape ->
        add(ArchitectChallenge("Identify Shapes", "Tap the shape that matches the name.", shape.replaceFirstChar(Char::uppercase), listOf(shape), listOf("square", "circle", "triangle", "rectangle"), "A $shape has its own defining sides, corners and curves."))
    }
    add(ArchitectChallenge("Squares & Rectangles", "Build a wide rectangle from two squares.", "Wide rectangle", listOf("square", "square"), listOf("square", "rectangle", "triangle", "circle"), "Two equal squares placed side-by-side make a rectangle."))
    add(ArchitectChallenge("Squares & Rectangles", "Build a tall rectangle from two rectangles.", "Tall tower", listOf("rectangle", "rectangle"), listOf("rectangle", "square", "triangle", "circle"), "Stacking two matching rectangles keeps four right angles."))
    add(ArchitectChallenge("Squares & Rectangles", "Build a window using four squares.", "Four-pane window", listOf("square", "square", "square", "square"), listOf("square", "rectangle", "triangle", "circle"), "Four equal squares can form one larger square window."))
    add(ArchitectChallenge("Build Triangles", "Use two triangles to complete the roof.", "Triangle roof", listOf("triangle", "triangle"), listOf("triangle", "square", "rectangle", "circle"), "Two smaller triangles can combine into a larger triangular roof."))
    add(ArchitectChallenge("Build Triangles", "Build a triangle using three triangular pieces.", "Three-piece triangle", listOf("triangle", "triangle", "triangle"), listOf("triangle", "square", "rectangle", "circle"), "The three pieces fill the triangular boundary without gaps."))
    add(ArchitectChallenge("Build Triangles", "Choose the shape with exactly three sides.", "Three-sided gate", listOf("triangle"), listOf("circle", "rectangle", "triangle", "square"), "Every triangle has exactly three straight sides."))
    add(ArchitectChallenge("Combine Shapes", "Build a house: body, roof and door.", "House", listOf("square", "triangle", "rectangle"), listOf("square", "triangle", "rectangle", "circle"), "A square body, triangular roof and rectangular door form the house."))
    add(ArchitectChallenge("Combine Shapes", "Build a tree: crown and trunk.", "Tree", listOf("circle", "rectangle"), listOf("circle", "rectangle", "square", "triangle"), "The curved crown sits above the rectangular trunk."))
    add(ArchitectChallenge("Combine Shapes", "Build a rocket: body, nose and window.", "Rocket", listOf("rectangle", "triangle", "circle"), listOf("rectangle", "triangle", "circle", "square"), "The three shapes create the rocket silhouette."))
    add(ArchitectChallenge("Symmetry", "Complete the square across the mirror.", "Square reflection", listOf("square"), listOf("square", "circle", "triangle", "rectangle"), "A reflected half must match the original distance and shape."))
    add(ArchitectChallenge("Symmetry", "Complete the butterfly with a matching curved wing.", "Butterfly reflection", listOf("circle"), listOf("circle", "square", "triangle", "rectangle"), "Mirror symmetry gives both wings the same outline."))
    add(ArchitectChallenge("Symmetry", "Complete the triangular kite across the mirror.", "Kite reflection", listOf("triangle"), listOf("triangle", "rectangle", "circle", "square"), "The second triangular half mirrors the first."))
    add(ArchitectChallenge("Shape Challenge", "Construct a castle tower.", "Castle tower", listOf("rectangle", "square", "triangle"), listOf("rectangle", "square", "triangle", "circle"), "The rectangle, square and triangle satisfy the tower blueprint."))
    add(ArchitectChallenge("Shape Challenge", "Select the only shape with no corners.", "Zero corners", listOf("circle"), listOf("triangle", "circle", "square", "rectangle"), "A circle has a continuous curved boundary and no vertices."))
    add(ArchitectChallenge("Shape Challenge", "Build a symmetric robot face.", "Robot face", listOf("square", "circle", "circle"), listOf("square", "circle", "triangle", "rectangle"), "Two matching circular eyes placed on a square face create symmetry."))
}

@Composable
internal fun ShapeArchitectGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    var routeName by rememberSaveable { mutableStateOf(ArchitectRoute.Intro.name) }
    var challengeIndex by rememberSaveable { mutableStateOf(completed.coerceIn(0, ArchitectChallenges.lastIndex)) }
    when (ArchitectRoute.valueOf(routeName)) {
        ArchitectRoute.Intro -> GameIntroScreen(
            number = 5,
            title = "Shape Architect",
            subtitle = "Build shapes. Learn geometry.",
            accent = GameBlue,
            concepts = listOf("□" to "Identify", "▭" to "Build", "△" to "Triangles", "↔" to "Symmetry"),
            completed = completed,
            total = ArchitectChallenges.size,
            onBack = onBack,
            onStart = { challengeIndex = completed.coerceIn(0, ArchitectChallenges.lastIndex); routeName = ArchitectRoute.Play.name },
            onMap = { routeName = ArchitectRoute.Map.name },
        )
        ArchitectRoute.Map -> LevelMapScreen(
            "Shape Architect",
            ArchitectConcepts,
            (completed / 3).coerceAtMost(ArchitectConcepts.lastIndex),
            GameBlue,
            { routeName = ArchitectRoute.Intro.name },
        ) { concept ->
            val first = concept * 3
            challengeIndex = maxOf(first, completed.coerceAtMost(first + 2))
            routeName = ArchitectRoute.Play.name
        }
        ArchitectRoute.Play -> ArchitectPlayScreen(challengeIndex, { routeName = ArchitectRoute.Map.name }) {
            onComplete(challengeIndex + 1)
            if (challengeIndex < ArchitectChallenges.lastIndex) challengeIndex++ else routeName = ArchitectRoute.Map.name
        }
    }
}

@Composable
private fun ArchitectPlayScreen(index: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val challenge = ArchitectChallenges[index]
    val concept = index / 3
    val stage = index % 3
    val selected = remember(index) { mutableStateListOf<String>() }
    var result by rememberSaveable(index) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(index) { mutableStateOf(false) }
    val accent = ArchitectConcepts[concept].accent
    GameScreen("${challenge.title} • ${stage + 1}/3", index + 1, accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(challenge.explanation, color = GameInk) }
        GlossyPanel(accent) {
            Text(challenge.prompt, color = GameInk, fontSize = if (LocalCompactGameLayout.current) 14.sp else 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArchitectWorkspace(challenge.targetLabel, selected, accent, Modifier.weight(1.2f)) { piece ->
                        selected.removeAt(piece); result = null
                    }
                    ShapePalette(challenge.choices, accent, Modifier.weight(.8f)) { if (selected.size < challenge.required.size) selected += it; result = null }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(if (LocalCompactGameLayout.current) 8.dp else 12.dp)) {
                    ArchitectWorkspace(challenge.targetLabel, selected, accent) { piece ->
                        selected.removeAt(piece); result = null
                    }
                    ShapePalette(challenge.choices, accent) { if (selected.size < challenge.required.size) selected += it; result = null }
                }
            }
        }
        GameComponentControls(
            status = "${selected.size}/${challenge.required.size} pieces",
            accent = accent,
            actions = listOf(
                GameComponentAction("Undo last", "↶", selected.isNotEmpty(), "Remove the last placed shape") {
                    selected.removeAt(selected.lastIndex)
                    result = null
                },
                GameComponentAction("Clear all", "×", selected.isNotEmpty(), "Remove all placed shapes") {
                    selected.clear()
                    result = null
                },
            ),
            guidance = "Tap a shape card to add it. Tap a placed shape to remove that exact piece, or use Undo last.",
        )
        PrimaryGameButton("Check Blueprint", GameGreen, {
            result = selected.sorted() == challenge.required.sorted()
        }, enabled = selected.size == challenge.required.size)
        result?.let { ResultPanel(it, challenge.explanation, "The pieces do not match the blueprint yet. Check the shape names and required count.", onSolved) }
    }
}

@Composable
private fun ArchitectWorkspace(
    target: String,
    selected: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    onRemove: (Int) -> Unit,
) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().height(if (compact) 148.dp else 290.dp).background(Color(0xFFF7F3E9), RoundedCornerShape(24.dp))
            .border(2.dp, accent.copy(.65f), RoundedCornerShape(24.dp)).padding(if (compact) 10.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        Text(target, color = GamePurple, fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Black)
        Box(
            Modifier.fillMaxWidth().weight(1f).background(Color.White.copy(.7f), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF7A6B62).copy(.5f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected.isEmpty()) {
                Text("DRAG SHAPES HERE", color = Color(0xFF786E78), fontWeight = FontWeight.Bold)
            } else {
                FlowRow(horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.Center) {
                    selected.forEachIndexed { i, shape ->
                        ShapeGlyph(
                            shape,
                            listOf(GameGreen, GameBlue, GameGold, GamePurple)[i % 4],
                            Modifier.size(if (compact) 42.dp else 74.dp).clickable { onRemove(i) },
                        )
                    }
                }
            }
        }
        Text("${selected.size} pieces placed", color = Color(0xFF574B64), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShapePalette(choices: List<String>, accent: Color, modifier: Modifier = Modifier, onShape: (String) -> Unit) {
    val compact = LocalCompactGameLayout.current
    FlowRow(
        modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.6f), RoundedCornerShape(22.dp)).padding(if (compact) 7.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        choices.distinct().forEachIndexed { index, shape ->
            DraggableShape(shape, listOf(GameGreen, GameBlue, Color(0xFFFF654F), GameGold)[index % 4]) { onShape(shape) }
        }
    }
}

@Composable
private fun DraggableShape(shape: String, color: Color, onDrop: () -> Unit) {
    val compact = LocalCompactGameLayout.current
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier.width(if (compact) 70.dp else 88.dp).height(if (compact) 78.dp else 108.dp).graphicsLayer { translationX = dx; translationY = dy }
            .background(Color(0xFFFFF5DC), RoundedCornerShape(17.dp)).border(1.dp, color.copy(.7f), RoundedCornerShape(17.dp))
            .pointerInput(shape) {
                detectDragGestures(
                    onDragEnd = { val moved = dy < -24f || kotlin.math.abs(dx) > 45f; dx = 0f; dy = 0f; if (moved) onDrop() },
                    onDragCancel = { dx = 0f; dy = 0f },
                ) { change, amount -> change.consume(); dx += amount.x; dy += amount.y }
            }.clickable(onClick = onDrop).focusable().semantics { contentDescription = "Drag $shape" }.padding(if (compact) 4.dp else 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShapeGlyph(shape, color, Modifier.size(if (compact) 48.dp else 66.dp))
        Text(shape.replaceFirstChar(Char::uppercase), color = GameSpace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShapeGlyph(shape: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(7.dp)) {
        when (shape) {
            "circle" -> drawCircle(color, size.minDimension / 2f)
            "triangle" -> drawPath(Path().apply {
                moveTo(size.width / 2f, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }, color)
            "rectangle" -> drawRoundRect(color, Offset(size.width * .15f, 0f), Size(size.width * .7f, size.height), androidx.compose.ui.geometry.CornerRadius(7f))
            else -> drawRoundRect(color, Offset.Zero, Size(size.width, size.height), androidx.compose.ui.geometry.CornerRadius(7f))
        }
    }
}
