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
import androidx.compose.runtime.mutableIntStateOf
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

private enum class PotionRoute { Intro, Map, Play }

private data class PotionChallenge(
    val title: String,
    val instruction: String,
    val formula: String,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
)

private val PotionConcepts = listOf(
    GameLevel("Mix & Add", "Combine potion amounts.", GameGreen),
    GameLevel("Mix & Subtract", "Remove one amount from another.", Color(0xFFFF7458)),
    GameLevel("Multiply Potions", "Build equal groups of potion.", GamePurple),
    GameLevel("Divide & Pour", "Share potion equally.", GameBlue),
    GameLevel("Fraction Potions", "Match fractional bottle fills.", Color(0xFFE85E9A)),
    GameLevel("Mega Challenge", "Mix operations and proportions.", Color(0xFF31C2C4)),
)

private val PotionChallenges = listOf(
    PotionChallenge("Mix & Add", "Mix the potions. What is the total?", "8 + 7 = ?", listOf("10", "15", "14", "16"), "15", "Eight plus seven equals fifteen."),
    PotionChallenge("Mix & Add", "Combine both beakers.", "12 + 9 = ?", listOf("19", "20", "21", "22"), "21", "Twelve plus nine equals twenty-one."),
    PotionChallenge("Mix & Add", "Complete the triple mixture.", "6 + 5 + 4 = ?", listOf("11", "14", "15", "16"), "15", "Six plus five is eleven; eleven plus four is fifteen."),
    PotionChallenge("Mix & Subtract", "Remove the green potion. What remains?", "12 − 5 = ?", listOf("6", "7", "8", "9"), "7", "Twelve minus five leaves seven."),
    PotionChallenge("Mix & Subtract", "Drain six units from the flask.", "18 − 6 = ?", listOf("10", "11", "12", "13"), "12", "Eighteen minus six leaves twelve."),
    PotionChallenge("Mix & Subtract", "Find the remaining concentrate.", "25 − 9 = ?", listOf("14", "15", "16", "17"), "16", "Twenty-five minus nine is sixteen."),
    PotionChallenge("Multiply Potions", "Multiply the potion groups.", "3 × 4 = ?", listOf("7", "12", "16", "20"), "12", "Three groups of four make twelve."),
    PotionChallenge("Multiply Potions", "Prepare five equal vials.", "5 × 6 = ?", listOf("25", "30", "35", "36"), "30", "Five groups of six make thirty."),
    PotionChallenge("Multiply Potions", "Double the nine-unit formula.", "2 × 9 = ?", listOf("11", "16", "18", "20"), "18", "Two groups of nine make eighteen."),
    PotionChallenge("Divide & Pour", "Pour equally into three bottles.", "12 ÷ 3 = ?", listOf("3", "4", "6", "9"), "4", "Twelve shared by three gives four per bottle."),
    PotionChallenge("Divide & Pour", "Share twenty units among five vials.", "20 ÷ 5 = ?", listOf("2", "4", "5", "10"), "4", "Twenty divided by five equals four."),
    PotionChallenge("Divide & Pour", "Fill six bottles equally.", "42 ÷ 6 = ?", listOf("6", "7", "8", "9"), "7", "Forty-two divided by six equals seven."),
    PotionChallenge("Fraction Potions", "Choose the matching bottle fill.", "Fill = 2/3", listOf("1/3", "1/2", "2/3", "3/4"), "2/3", "Two of three equal bottle sections must be filled."),
    PotionChallenge("Fraction Potions", "Choose the half-full bottle.", "Fill = 1/2", listOf("1/4", "1/3", "1/2", "2/3"), "1/2", "One of two equal parts is one-half."),
    PotionChallenge("Fraction Potions", "Choose three quarters full.", "Fill = 3/4", listOf("1/2", "2/3", "3/4", "4/5"), "3/4", "Three of four equal sections must be filled."),
    PotionChallenge("Mega Challenge", "Use multiplication before addition.", "4 + 3 × 2 = ?", listOf("10", "11", "14", "16"), "10", "Multiply first: three times two is six; four plus six is ten."),
    PotionChallenge("Mega Challenge", "Scale a 2:3 mixture. Blue is 8.", "Red = ?", listOf("10", "12", "14", "16"), "12", "The ratio scale factor is four, so three times four is twelve."),
    PotionChallenge("Mega Challenge", "Find 25% of the potion stock.", "25% of 80 = ?", listOf("15", "20", "25", "40"), "20", "Twenty-five percent is one quarter; one quarter of eighty is twenty."),
)

@Composable
internal fun PotionLabGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    var routeName by rememberSaveable { mutableStateOf(PotionRoute.Intro.name) }
    var challengeIndex by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, PotionChallenges.lastIndex)) }
    when (PotionRoute.valueOf(routeName)) {
        PotionRoute.Intro -> GameIntroScreen(
            number = 6,
            title = "Potion Lab",
            subtitle = "Mix, measure and solve.",
            accent = GamePurple,
            concepts = listOf("+" to "Add", "−" to "Subtract", "×" to "Multiply", "⅔" to "Fractions"),
            completed = completed,
            total = PotionChallenges.size,
            onBack = onBack,
            onStart = { challengeIndex = completed.coerceIn(0, PotionChallenges.lastIndex); routeName = PotionRoute.Play.name },
            onMap = { routeName = PotionRoute.Map.name },
        )
        PotionRoute.Map -> LevelMapScreen(
            "Potion Lab",
            PotionConcepts,
            (completed / 3).coerceAtMost(PotionConcepts.lastIndex),
            GamePurple,
            { routeName = PotionRoute.Intro.name },
        ) { concept ->
            val first = concept * 3
            challengeIndex = maxOf(first, completed.coerceAtMost(first + 2))
            routeName = PotionRoute.Play.name
        }
        PotionRoute.Play -> PotionPlayScreen(challengeIndex, { routeName = PotionRoute.Map.name }) {
            onComplete(challengeIndex + 1)
            if (challengeIndex < PotionChallenges.lastIndex) challengeIndex++ else routeName = PotionRoute.Map.name
        }
    }
}

@Composable
private fun PotionPlayScreen(index: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val challenge = PotionChallenges[index]
    val concept = index / 3
    val stage = index % 3
    var selected by rememberSaveable(index) { mutableStateOf("") }
    var result by rememberSaveable(index) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(index) { mutableStateOf(false) }
    val accent = PotionConcepts[concept].accent
    GameScreen("${challenge.title} • ${stage + 1}/3", index + 1, accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(challenge.explanation, color = GameInk) }
        GlossyPanel(accent) {
            Text(challenge.instruction, color = GameInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(challenge.formula, color = GameInk, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PotionWorkspace(selected, accent, Modifier.weight(1.15f))
                    PotionPalette(challenge.choices, Modifier.weight(.85f)) { selected = it; result = null }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PotionWorkspace(selected, accent)
                    PotionPalette(challenge.choices) { selected = it; result = null }
                }
            }
        }
        GameComponentControls(
            status = if (selected.isBlank()) "Flask is empty" else "$selected units in flask",
            accent = accent,
            actions = listOf(
                GameComponentAction("Remove potion", "−", selected.isNotBlank(), "Remove the potion from the answer flask") {
                    selected = ""
                    result = null
                },
                GameComponentAction("Empty flask", "×", selected.isNotBlank(), "Empty the answer flask") {
                    selected = ""
                    result = null
                },
            ),
            guidance = "Tap or drag a potion to add it. Selecting another potion replaces the current amount.",
        )
        PrimaryGameButton("Check Potion", GameGreen, { result = selected == challenge.answer }, enabled = selected.isNotBlank())
        result?.let { ResultPanel(it, challenge.explanation, "That potion amount does not satisfy the formula. Recalculate and try another flask.", onSolved) }
    }
}

@Composable
private fun PotionWorkspace(selected: String, accent: Color, modifier: Modifier = Modifier) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().height(if (compact) 210.dp else 270.dp).background(
            Brush.radialGradient(listOf(accent.copy(.28f), GamePanel)),
            RoundedCornerShape(24.dp),
        ).border(2.dp, accent.copy(.7f), RoundedCornerShape(24.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ANSWER FLASK", color = GameMuted, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
        PotionFlask(accent, if (selected.isBlank()) "?" else selected, Modifier.size(if (compact) 140.dp else 180.dp))
        Text(if (selected.isBlank()) "Drag a potion here" else "$selected units selected", color = GameInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PotionPalette(choices: List<String>, modifier: Modifier = Modifier, onPotion: (String) -> Unit) {
    FlowRow(
        modifier.fillMaxWidth().background(Color.Black.copy(.2f), RoundedCornerShape(22.dp)).border(1.dp, GamePurple.copy(.55f), RoundedCornerShape(22.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        choices.forEachIndexed { index, choice ->
            DraggablePotion(choice, listOf(GameBlue, GamePurple, GameGold, GameGreen)[index % 4]) { onPotion(choice) }
        }
    }
}

@Composable
private fun DraggablePotion(value: String, color: Color, onDrop: () -> Unit) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier.width(82.dp).height(112.dp).graphicsLayer { translationX = dx; translationY = dy }
            .background(GamePanel, RoundedCornerShape(17.dp)).border(1.dp, color.copy(.7f), RoundedCornerShape(17.dp))
            .pointerInput(value) {
                detectDragGestures(
                    onDragEnd = { val moved = dy < -24f || kotlin.math.abs(dx) > 45f; dx = 0f; dy = 0f; if (moved) onDrop() },
                    onDragCancel = { dx = 0f; dy = 0f },
                ) { change, amount -> change.consume(); dx += amount.x; dy += amount.y }
            }.clickable(onClick = onDrop).focusable().semantics { contentDescription = "Drag potion $value" }.padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PotionFlask(color, value, Modifier.size(73.dp))
        Text(value, color = GameInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun PotionFlask(color: Color, label: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().padding(5.dp)) {
            val outline = Path().apply {
                moveTo(size.width * .38f, size.height * .08f)
                lineTo(size.width * .62f, size.height * .08f)
                lineTo(size.width * .62f, size.height * .35f)
                cubicTo(size.width * .62f, size.height * .42f, size.width * .82f, size.height * .57f, size.width * .84f, size.height * .79f)
                cubicTo(size.width * .86f, size.height * .94f, size.width * .72f, size.height * .97f, size.width * .5f, size.height * .97f)
                cubicTo(size.width * .28f, size.height * .97f, size.width * .14f, size.height * .94f, size.width * .16f, size.height * .79f)
                cubicTo(size.width * .18f, size.height * .57f, size.width * .38f, size.height * .42f, size.width * .38f, size.height * .35f)
                close()
            }
            drawPath(outline, Color(0xFFD9F3FF).copy(.35f))
            drawPath(Path().apply {
                moveTo(size.width * .18f, size.height * .67f)
                lineTo(size.width * .82f, size.height * .67f)
                lineTo(size.width * .84f, size.height * .85f)
                quadraticTo(size.width * .75f, size.height * .95f, size.width * .5f, size.height * .95f)
                quadraticTo(size.width * .25f, size.height * .95f, size.width * .16f, size.height * .85f)
                close()
            }, color.copy(.9f))
            drawPath(outline, Color(0xFFBDEBFF), style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
            drawLine(Color(0xFFBDEBFF), Offset(size.width * .34f, size.height * .08f), Offset(size.width * .66f, size.height * .08f), 5f)
        }
        Text(label, color = GameInk, fontSize = if (label.length > 2) 14.sp else 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 35.dp))
    }
}
