package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ForgeRoute { Intro, Map, Play }

private val ForgeLevels = listOf(
    GameLevel("Build the Number", "Use hundreds, tens and ones blocks.", GameGreen),
    GameLevel("Expanded Form", "Break a number into place values.", GameGold),
    GameLevel("How Many?", "Count base-ten blocks.", GameBlue),
    GameLevel("Compare Numbers", "Use greater than, equal or less than.", GamePurple),
    GameLevel("Word to Number", "Translate number words into digits.", Color(0xFF35C67A)),
    GameLevel("Place Value Challenge", "Combine all place-value skills.", Color(0xFF27BFC4)),
)

@Composable
internal fun NumberForgeGame(
    completed: Int,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit,
) {
    var routeName by rememberSaveable { mutableStateOf(ForgeRoute.Intro.name) }
    var level by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, ForgeLevels.lastIndex)) }
    val route = ForgeRoute.valueOf(routeName)
    when (route) {
        ForgeRoute.Intro -> GameIntroScreen(
            number = 1,
            title = "Number Forge",
            subtitle = "Build numbers using hundreds, tens and ones.",
            accent = GameBlue,
            concepts = listOf("100" to "Hundreds", "10" to "Tens", "1" to "Ones", "?" to "Compare"),
            completed = completed,
            total = ForgeLevels.size,
            onBack = onBack,
            onStart = { level = completed.coerceIn(0, ForgeLevels.lastIndex); routeName = ForgeRoute.Play.name },
            onMap = { routeName = ForgeRoute.Map.name },
        )
        ForgeRoute.Map -> LevelMapScreen(
            worldTitle = "Number Forge",
            levels = ForgeLevels,
            unlocked = completed.coerceAtMost(ForgeLevels.lastIndex),
            accent = GameBlue,
            onBack = { routeName = ForgeRoute.Intro.name },
        ) { selected -> level = selected; routeName = ForgeRoute.Play.name }
        ForgeRoute.Play -> ForgeLevel(
            level = level,
            onBack = { routeName = ForgeRoute.Map.name },
            onSolved = {
                onComplete(level + 1)
                if (level < ForgeLevels.lastIndex) level += 1 else routeName = ForgeRoute.Map.name
            },
        )
    }
}

@Composable
private fun ForgeLevel(level: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val targets = listOf(245, 347, 246, 0, 352, 583)
    val target = targets[level]
    var hundreds by rememberSaveable(level) { mutableIntStateOf(0) }
    var tens by rememberSaveable(level) { mutableIntStateOf(0) }
    var ones by rememberSaveable(level) { mutableIntStateOf(0) }
    var chosen by rememberSaveable(level) { mutableStateOf("") }
    var result by rememberSaveable(level) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(level) { mutableStateOf(false) }
    val hearts = if (result == false) 2 else 3

    GameScreen(
        title = ForgeLevels[level].title,
        level = level + 1,
        accent = ForgeLevels[level].accent,
        hearts = hearts,
        onBack = onBack,
        onHint = { hint = !hint },
    ) {
        if (hint) {
            GlossyPanel(GameGold) {
                Text(
                    when (level) {
                        0, 1, 5 -> "Read the target from left to right: hundreds, then tens, then ones."
                        2 -> "Count each flat as 100, each rod as 10 and each cube as 1."
                        3 -> "Compare hundreds first. If equal, compare tens, then ones."
                        else -> "The words tell you the hundreds digit, tens digit and ones digit."
                    },
                    color = GameInk,
                )
            }
        }
        when (level) {
            0, 1, 5 -> {
                Text(
                    if (level == 1) "Make $target in expanded form." else "Build the number shown.",
                    color = GameInk, fontSize = 13.sp,
                )
                TargetNumber(target, ForgeLevels[level].accent)
                PlaceValueBins(hundreds, tens, ones, ForgeLevels[level].accent)
                BaseTenVisual(hundreds, tens, ones)
                Text("Drag blocks into their place-value columns.", color = GameMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DraggableGameTile("100", GameGreen, "one hundred block") { if (hundreds < 9) hundreds++; result = null }
                    DraggableGameTile("10", GameGold, "one tens rod") { if (tens < 9) tens++; result = null }
                    DraggableGameTile("1", GameBlue, "one unit cube") { if (ones < 9) ones++; result = null }
                }
                if (hundreds + tens + ones > 0) {
                    SecondaryGameButton("Undo last blocks", ForgeLevels[level].accent) {
                        when {
                            ones > 0 -> ones--
                            tens > 0 -> tens--
                            hundreds > 0 -> hundreds--
                        }
                        result = null
                    }
                }
                PrimaryGameButton("Check", GameGreen, onClick = { result = hundreds * 100 + tens * 10 + ones == target })
            }
            2 -> {
                Text("Count the blocks and build the number.", color = GameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                GlossyPanel(GameBlue) { BaseTenVisual(2, 4, 6) }
                DigitSlots(chosen, 3, GamePurple)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("2", "4", "6", "8").forEach { digit ->
                        DraggableGameTile(digit, if (digit == "2") GameGreen else if (digit == "4") GameGold else GameBlue) {
                            if (chosen.length < 3) chosen += digit
                            result = null
                        }
                    }
                }
                SecondaryGameButton("Clear digits", GameBlue) { chosen = ""; result = null }
                PrimaryGameButton("Check", GameGreen, onClick = { result = chosen == "246" })
            }
            3 -> {
                Text("Which number is greater?", color = GameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    PlaceNumberCard(285)
                    Box(
                        Modifier.height(60.dp).background(GamePurple.copy(.25f), RoundedCornerShape(18.dp))
                            .border(1.dp, GamePurple, RoundedCornerShape(18.dp)).padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (chosen.isBlank()) "?" else chosen, color = GameInk, fontSize = 27.sp, fontWeight = FontWeight.Black) }
                    PlaceNumberCard(287)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(">", "=", "<").forEach { symbol ->
                        DraggableGameTile(symbol, GamePurple) { chosen = symbol; result = null }
                    }
                }
                PrimaryGameButton("Check", GameGreen, onClick = { result = chosen == "<" })
            }
            else -> {
                Text("Write the number in digits.", color = GameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                GlossyPanel(Color(0xFF35C67A)) {
                    Text("Three hundred fifty-two", color = GameInk, fontSize = 23.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                DigitSlots(chosen, 3, Color(0xFF35C67A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("3", "5", "2", "7").forEachIndexed { index, digit ->
                        DraggableGameTile(digit, listOf(GameGreen, GameGold, GameBlue, GamePurple)[index]) {
                            if (chosen.length < 3) chosen += digit
                            result = null
                        }
                    }
                }
                SecondaryGameButton("Clear digits", Color(0xFF35C67A)) { chosen = ""; result = null }
                PrimaryGameButton("Check", GameGreen, onClick = { result = chosen == "352" })
            }
        }
        result?.let { correct ->
            ResultPanel(
                correct,
                when (level) {
                    0 -> "245 is 2 hundreds, 4 tens and 5 ones."
                    1 -> "347 = 300 + 40 + 7."
                    2 -> "2 hundreds + 4 tens + 6 ones = 246."
                    3 -> "285 is less than 287 because 5 ones is less than 7 ones."
                    4 -> "Three hundred fifty-two is written 352."
                    else -> "583 is 5 hundreds, 8 tens and 3 ones."
                },
                "Check the place-value columns and try another arrangement.",
                onSolved,
            )
        }
    }
}

@Composable
private fun TargetNumber(target: Int, accent: Color) {
    Box(
        Modifier.fillMaxWidth().height(82.dp).background(Color.Black.copy(.3f), RoundedCornerShape(20.dp))
            .border(2.dp, accent.copy(.65f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(target.toString(), color = GameInk, fontSize = 43.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp) }
}

@Composable
private fun PlaceValueBins(hundreds: Int, tens: Int, ones: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(Triple("Hundreds", hundreds, GameGreen), Triple("Tens", tens, GameGold), Triple("Ones", ones, GameBlue)).forEach { (label, value, color) ->
            Column(
                Modifier.weight(1f).height(88.dp).background(color.copy(.2f), RoundedCornerShape(16.dp))
                    .border(1.dp, color.copy(.75f), RoundedCornerShape(16.dp)).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, color = GameInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value.toString(), color = color, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun DigitSlots(value: String, count: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { index ->
            Box(
                Modifier.padding(5.dp).height(62.dp).weight(1f).background(accent.copy(.22f), RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(value.getOrNull(index)?.toString() ?: "?", color = GameInk, fontSize = 28.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun PlaceNumberCard(value: Int) {
    Column(
        Modifier.background(GamePanel, RoundedCornerShape(18.dp)).border(1.dp, GameBlue.copy(.65f), RoundedCornerShape(18.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("H   T   O", color = GameInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value.toString().toCharArray().joinToString("   "), color = GameInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}
