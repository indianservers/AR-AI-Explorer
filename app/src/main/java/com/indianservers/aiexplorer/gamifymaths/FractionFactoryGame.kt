package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class FractionRoute { Intro, Map, Play }

private data class FractionTask(
    val title: String,
    val prompt: String,
    val left: Pair<Int, Int>,
    val right: Pair<Int, Int>?,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
)

private val FractionTasks = listOf(
    FractionTask("Equivalent Fractions", "Drag the fraction equivalent to 1/2.", 1 to 2, 2 to 4, listOf("2/4", "1/3", "3/4"), "2/4", "Multiplying 1/2 by 2/2 gives 2/4."),
    FractionTask("Comparing Fractions", "Which fraction is greater?", 3 to 5, 2 to 3, listOf(">", "=", "<"), "<", "3/5 = 0.6 while 2/3 is about 0.67, so 3/5 < 2/3."),
    FractionTask("Adding Fractions", "Solve 1/3 + 1/6.", 1 to 3, 1 to 6, listOf("1/2", "1/3", "1/6", "2/9"), "1/2", "1/3 is 2/6; 2/6 + 1/6 = 3/6 = 1/2."),
    FractionTask("Subtracting Fractions", "Solve 5/6 − 1/3.", 5 to 6, 1 to 3, listOf("1/6", "1/2", "2/3", "1/3"), "1/2", "1/3 is 2/6; 5/6 − 2/6 = 3/6 = 1/2."),
    FractionTask("Mixed Numbers", "Convert 1 1/2 to an improper fraction.", 1 to 2, null, listOf("2/2", "3/2", "3/4", "2/3"), "3/2", "One whole is 2/2; 2/2 + 1/2 = 3/2."),
    FractionTask("Fractions to Percent", "Convert 3/4 to a percentage.", 3 to 4, null, listOf("34%", "50%", "75%", "80%"), "75%", "3/4 = 0.75, which is 75%."),
)

private val FractionLevels = FractionTasks.mapIndexed { index, task ->
    GameLevel(task.title, listOf("Match equal parts", "Compare visual amounts", "Combine fraction pieces", "Remove fraction pieces", "Move between forms", "Connect fractions, decimals and percentages")[index], listOf(GameGreen, GameGold, GameBlue, GamePurple, Color(0xFFE75D94), Color(0xFF35C5C8))[index])
}

@Composable
internal fun FractionFactoryGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    var routeName by rememberSaveable { mutableStateOf(FractionRoute.Intro.name) }
    var level by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, FractionTasks.lastIndex)) }
    when (FractionRoute.valueOf(routeName)) {
        FractionRoute.Intro -> GameIntroScreen(
            number = 3,
            title = "Fraction Factory",
            subtitle = "Build, match and master fractions.",
            accent = GamePurple,
            concepts = listOf("◐" to "Visualise", "≷" to "Compare", "+/−" to "Operate", "0.5" to "Convert"),
            completed = completed,
            total = FractionTasks.size,
            onBack = onBack,
            onStart = { level = completed.coerceIn(0, FractionTasks.lastIndex); routeName = FractionRoute.Play.name },
            onMap = { routeName = FractionRoute.Map.name },
        )
        FractionRoute.Map -> LevelMapScreen("Fraction Factory", FractionLevels, completed.coerceAtMost(FractionTasks.lastIndex), GamePurple, { routeName = FractionRoute.Intro.name }) {
            level = it; routeName = FractionRoute.Play.name
        }
        FractionRoute.Play -> FractionTaskScreen(level, { routeName = FractionRoute.Map.name }) {
            onComplete(level + 1)
            if (level < FractionTasks.lastIndex) level++ else routeName = FractionRoute.Map.name
        }
    }
}

@Composable
private fun FractionTaskScreen(level: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val task = FractionTasks[level]
    var selected by rememberSaveable(level) { mutableStateOf("") }
    var result by rememberSaveable(level) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(level) { mutableStateOf(false) }
    GameScreen(task.title, level + 1, FractionLevels[level].accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(task.explanation, color = GameInk) }
        GlossyPanel(FractionLevels[level].accent) {
            Text(task.prompt, color = GameInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Row(
            Modifier.fillMaxWidth().height(240.dp).background(Color(0xFFEFF4FF), RoundedCornerShape(22.dp))
                .border(2.dp, FractionLevels[level].accent.copy(.65f), RoundedCornerShape(22.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FractionDisc(task.left.first, task.left.second, FractionLevels[level].accent)
            Text(
                when (level) { 1 -> if (selected.isBlank()) "?" else selected; 2 -> "+"; 3 -> "−"; else -> "=" },
                color = GamePurple, fontSize = 34.sp, fontWeight = FontWeight.Black,
            )
            task.right?.let { FractionDisc(it.first, it.second, GameGold) }
                ?: Box(Modifier.size(100.dp).background(Color.White, CircleShape).border(2.dp, GameMuted, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (selected.isBlank()) "?" else selected, color = GamePurple, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
        }
        Text("Drag the correct piece or symbol into the workspace.", color = GameMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            task.choices.forEachIndexed { index, choice ->
                DraggableGameTile(choice, listOf(GamePurple, GameBlue, GameGreen, GameGold)[index % 4]) {
                    selected = choice; result = null
                }
            }
        }
        PrimaryGameButton("Check", GameGreen, { result = selected == task.answer }, enabled = selected.isNotBlank())
        result?.let { ResultPanel(it, task.explanation, "Use the visible parts to compare or rewrite the fractions with a common denominator.", onSolved) }
    }
}

@Composable
private fun FractionDisc(numerator: Int, denominator: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(116.dp)) {
            val diameter = size.minDimension
            repeat(denominator) { index ->
                drawArc(
                    color = if (index < numerator) accent else Color(0xFFFFF8EB),
                    startAngle = -90f + index * (360f / denominator),
                    sweepAngle = 360f / denominator,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(diameter, diameter),
                )
            }
            repeat(denominator) { index ->
                val angle = Math.toRadians((-90.0 + index * 360.0 / denominator))
                drawLine(
                    Color(0xFF26375E),
                    Offset(diameter / 2f, diameter / 2f),
                    Offset(diameter / 2f + kotlin.math.cos(angle).toFloat() * diameter / 2f, diameter / 2f + kotlin.math.sin(angle).toFloat() * diameter / 2f),
                    2f,
                )
            }
            drawCircle(Color(0xFF26375E), diameter / 2f, style = Stroke(3f))
        }
        Text("$numerator/$denominator", color = GameSpace, fontSize = 18.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
