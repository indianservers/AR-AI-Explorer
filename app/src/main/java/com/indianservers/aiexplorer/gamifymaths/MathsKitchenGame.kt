package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class KitchenRoute { Intro, Map, Play }

private data class Recipe(
    val title: String,
    val left: Int,
    val operation: String,
    val right: Int,
    val answer: Int,
    val instruction: String,
    val explanation: String,
)

private val Recipes = listOf(
    Recipe("Tasty Addition", 2, "+", 3, 5, "Combine both ingredient groups.", "2 + 3 = 5 ingredients."),
    Recipe("Subtraction Soup", 7, "−", 2, 5, "Remove two ingredients from seven.", "7 − 2 leaves 5."),
    Recipe("Multiplication Muffins", 3, "×", 4, 12, "Fill 3 rows with 4 berries each.", "3 groups of 4 make 12."),
    Recipe("Division Juice", 12, "÷", 3, 4, "Share twelve units equally between three glasses.", "12 divided into 3 equal groups gives 4 in each."),
    Recipe("Order Up!", 4, "+", 2, 10, "Calculate 4 + 2 × 3 using operation order.", "Multiply first: 2 × 3 = 6; then 4 + 6 = 10."),
    Recipe("Estimate It!", 198, "+", 304, 500, "Choose the best estimate.", "198 is about 200 and 304 is about 300, giving about 500."),
)

private val KitchenLevels = Recipes.mapIndexed { index, recipe ->
    GameLevel(recipe.title, listOf("Add ingredients", "Remove ingredients", "Make equal groups", "Share equally", "Use operation order", "Estimate and check")[index], listOf(GameGreen, GameGold, GameBlue, GamePurple, Color(0xFFE461A4), Color(0xFF3EC3C8))[index])
}

@Composable
internal fun MathsKitchenGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    var routeName by rememberSaveable { mutableStateOf(KitchenRoute.Intro.name) }
    var level by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, Recipes.lastIndex)) }
    when (KitchenRoute.valueOf(routeName)) {
        KitchenRoute.Intro -> GameIntroScreen(
            number = 2,
            title = "Maths Kitchen",
            subtitle = "Solve recipes with numbers.",
            accent = GameGold,
            concepts = listOf("+" to "Add", "−" to "Subtract", "×" to "Multiply", "÷" to "Divide"),
            completed = completed,
            total = Recipes.size,
            onBack = onBack,
            onStart = { level = completed.coerceIn(0, Recipes.lastIndex); routeName = KitchenRoute.Play.name },
            onMap = { routeName = KitchenRoute.Map.name },
        )
        KitchenRoute.Map -> LevelMapScreen("Maths Kitchen", KitchenLevels, completed.coerceAtMost(Recipes.lastIndex), GameGold, { routeName = KitchenRoute.Intro.name }) {
            level = it; routeName = KitchenRoute.Play.name
        }
        KitchenRoute.Play -> RecipeScreen(level, { routeName = KitchenRoute.Map.name }) {
            onComplete(level + 1)
            if (level < Recipes.lastIndex) level++ else routeName = KitchenRoute.Map.name
        }
    }
}

@Composable
private fun RecipeScreen(level: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    val recipe = Recipes[level]
    var selected by rememberSaveable(level) { mutableIntStateOf(0) }
    var result by rememberSaveable(level) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(level) { mutableStateOf(false) }
    val choices = when (level) {
        0 -> listOf(4, 5, 6, 7)
        1 -> listOf(3, 4, 5, 6)
        2 -> listOf(7, 10, 12, 14)
        3 -> listOf(3, 4, 6, 9)
        4 -> listOf(6, 8, 10, 18)
        else -> listOf(400, 500, 600, 700)
    }
    GameScreen(recipe.title, level + 1, KitchenLevels[level].accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(recipe.explanation, color = GameInk) }
        GlossyPanel(KitchenLevels[level].accent) {
            Text("RECIPE", color = GameGreen, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("${recipe.left}  ${recipe.operation}  ${recipe.right}  =  ", color = GameSpace, fontSize = 25.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(Color(0xFFFFF3D6), RoundedCornerShape(13.dp)).padding(12.dp))
                Text(if (selected == 0) " ?" else selected.toString(), color = GameInk, fontSize = 26.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(GamePurple, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp))
            }
            Text(recipe.instruction, color = GameInk, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        CookingPan(selected, KitchenLevels[level].accent)
        Text("Drag the correct ingredient card into the pan.", color = GameMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            choices.forEachIndexed { index, value ->
                DraggableGameTile(value.toString(), listOf(GameGreen, GameGold, GameBlue, GamePurple)[index]) {
                    selected = value; result = null
                }
            }
        }
        SecondaryGameButton("Clear pan", KitchenLevels[level].accent) { selected = 0; result = null }
        PrimaryGameButton("Check Recipe", GameGreen, { result = selected == recipe.answer }, enabled = selected != 0)
        result?.let { ResultPanel(it, recipe.explanation, "The recipe is not balanced yet. Recalculate and drag another quantity.", onSolved) }
    }
}

@Composable
private fun CookingPan(value: Int, accent: Color) {
    val compact = LocalCompactGameLayout.current
    Box(
        Modifier.fillMaxWidth().height(if (compact) 140.dp else 178.dp).background(
            Brush.radialGradient(listOf(accent.copy(.2f), GamePanel)),
            RoundedCornerShape(24.dp),
        ).border(1.dp, accent.copy(.6f), RoundedCornerShape(24.dp)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val panWidth = size.width * .62f
            val left = (size.width - panWidth) / 2f
            drawOval(Color(0xFF3A246F), Offset(left, 76f), Size(panWidth, 75f))
            drawOval(Color(0xFF7E45B7), Offset(left + 9f, 72f), Size(panWidth - 18f, 54f))
            drawOval(Color(0xFFFF7258).copy(.75f), Offset(left + 20f, 80f), Size(panWidth - 40f, 37f))
            drawRoundRect(Color(0xFF432866), Offset(left - 45f, 89f), Size(68f, 18f), androidx.compose.ui.geometry.CornerRadius(9f))
            drawRoundRect(Color(0xFF432866), Offset(left + panWidth - 22f, 89f), Size(68f, 18f), androidx.compose.ui.geometry.CornerRadius(9f))
        }
        Text(if (value == 0) "PAN" else value.toString(), color = GameInk, fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
    }
}
