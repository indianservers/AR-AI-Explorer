package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class GameSetupProfile(
    val scaleTitle: String,
    val scaleDescription: String,
    val masteryDescription: String,
    val hintDescription: String,
    val topics: List<String> = emptyList(),
)

internal fun gameSetupProfile(id: String): GameSetupProfile = when (id) {
    "forge" -> GameSetupProfile("Place-value range", "Start with 2, 3, 4 or 5 digits; later stages increase the range.", "Mastery I: six digits. Mastery II: seven digits.", "Place-value clues")
    "number-bonds" -> GameSetupProfile("Number-core challenge", "Higher settings increase tile values and the number of required cores.", "Mastery I: five cores. Mastery II: six signed cores.", "Target-sum clues")
    "shapes" -> GameSetupProfile("Shape collection", "Higher settings require more pieces while preserving side and curve constraints.", "Mastery I: seven shapes. Mastery II: eight, with more polygon types.", "Side and curve clues")
    "fractions" -> GameSetupProfile("Fraction mosaic", "Combine available fraction tiles to reach an exact area.", "Mastery boards use four tiles expressed in sixtieths.", "Common-denominator clues", listOf("Twelfths", "Sixtieths"))
    "kitchen" -> GameSetupProfile("Recipe quantities", "Higher settings increase the quantities used in recipe calculations.", "Mastery I: combine and share. Mastery II: scale batches, then share.", "Recipe calculation clues", listOf("Add", "Subtract", "Multiply", "Divide", "Order", "Estimate"))
    "measure" -> GameSetupProfile("Measurement scale", "Increase dimensions, beam lengths and construction size.", "Mastery I: five beams. Mastery II: six beams.", "Measurement and beam clues", listOf("Perimeter", "Area", "Volume", "Bridge"))
    "balance" -> GameSetupProfile("Equation coefficients", "Practise equivalent operations with generated coefficients.", "Mastery I: variables on both sides. Mastery II: larger, signed solutions.", "Equal-operation clues", listOf("One side", "Both sides"))
    "potions" -> GameSetupProfile("Potion recipe", "Match BOTH the total volume and the blue:red ratio.", "Mastery I: larger batches. Mastery II: ratios up to 20 and larger scales.", "Mixture scaling clues", listOf("Small ratios", "Wide ratios"))
    "ratio-rangers" -> GameSetupProfile("Ingredient ratios", "Scale two ingredients together to meet a target batch volume.", "Mastery I: larger ratios. Mastery II: larger ratios and batch scales.", "Ratio and volume clues", listOf("Small ratios", "Wide ratios"))
    "vectors" -> GameSetupProfile("Flight coordinates", "Reach the destination within the displayed move budget.", "Mastery I: generated vector basis. Mastery II: longer routes.", "Displacement clues", listOf("Unit moves", "Vector basis"))
    "integer-expedition" -> GameSetupProfile("Signed journey", "Navigate left and right to reach the exact target.", "Mastery I: longer routes. Mastery II: steps of 10 and 3.", "Signed-move clues", listOf("Steps 5 & 1", "Steps 10 & 3"))
    "matrix-mission" -> GameSetupProfile("Vector basis", "Navigate coordinates using the generated basis vectors.", "Mastery I: generated basis. Mastery II: larger basis and displacement.", "Basis and displacement clues", listOf("Small basis", "Wide basis"))
    "patterns" -> GameSetupProfile("Pattern coefficients", "Match a single rule to every input/output example.", "Mastery I: square, scale, add. Mastery II: scale, add, square.", "Pattern-rule clues", listOf("Linear", "Quadratic"))
    "function-machine" -> GameSetupProfile("Function coefficients", "Build a circuit matching all input/output pairs.", "Mastery I: quadratic then affine. Mastery II: affine then quadratic.", "Function-order clues", listOf("Linear", "Quadratic"))
    "times-table" -> GameSetupProfile("Star-array dimensions", "Choose rows and columns to match the star total.", "Mastery I: area and perimeter. Mastery II: area and row difference.", "Array-dimension clues", listOf("Tables 2–9", "Tables 2–12"))
    "data" -> GameSetupProfile("Evidence dataset", "Mean and range use this starting size. Sorting uses seven observations.", "Mastery I: descending order and middle slots. Mastery II: repeated modes.", "Sorting and statistics clues", listOf("Mean", "Sort median", "Range"))
    "data-story" -> GameSetupProfile("Story dataset", "Mean and range use this starting size. Sorting uses seven observations.", "Mastery I: descending order. Mastery II: mark all modal observations.", "Data-summary clues", listOf("Mean", "Sort median", "Range"))
    "chance" -> GameSetupProfile("Probability quantities", "Increase generated counts used by the selected probability family.", "Mastery I: three dependent draws. Mastery II: expected net payoff.", "Probability-method clues", listOf("Bags", "Spinners", "Coins", "Cards", "No replacement", "Permutations", "Combinations", "Binomial", "Normal", "Z-score", "Sampling", "Mean", "Median", "Std dev", "Variance"))
    "logic" -> GameSetupProfile("Robot formations", "Collect every distinct formation; order changes what counts.", "Mastery I: energy-constrained teams. Mastery II: ordered three-robot codes.", "Formation-counting clues", listOf("Teams", "Ordered codes"))
    "combinatorics" -> GameSetupProfile("Counting formations", "Collect unordered teams or ordered codes without duplicates.", "Mastery I: energy-constrained teams. Mastery II: ordered three-robot codes.", "Permutation and combination clues", listOf("Teams", "Ordered codes"))
    "decimal-harbor" -> GameSetupProfile("Cargo quantities", "Increase the quantities used in decimal calculations.", "Mastery I: trips and unloading. Mastery II: share the remaining cargo.", "Decimal place-value clues", listOf("Add", "Subtract", "Multiply", "Divide", "Round", "Shift"))
    "math-market" -> GameSetupProfile("Shopping budget", "Increase prices and basket size in generated shopping tasks.", "Mastery I: five-item basket. Mastery II: six-item basket.", "Price and budget clues", listOf("Change", "Total", "Basket"))
    "percent-studio" -> GameSetupProfile("Price and percentage", "Increase the amounts used in percentage calculations.", "Mastery I: successive discounts. Mastery II: discounts followed by tax.", "Percentage calculation clues", listOf("Percent of", "Discount", "Reverse %"))
    "geometry-proof" -> GameSetupProfile("Geometry quantities", "Increase dimensions used by generated geometry calculations.", "Mastery I: remove one triangular section. Mastery II: remove two.", "Geometry-formula clues", listOf("Triangle", "Polygon", "Similar", "Right triangle", "Radius", "Line angles"))
    "calculus-climber" -> GameSetupProfile("Calculus coefficients", "Increase coefficients and evaluation points.", "Mastery I: cubic derivative. Mastery II: quadratic definite integral.", "Differentiation and integration clues", listOf("Derivative", "Integral", "Limit"))
    "number-theory" -> GameSetupProfile("Prime-factor chain", "Multiply prime cores to unlock the generated product.", "Mastery I: five prime factors. Mastery II: seven factors.", "Prime-divisibility clues", listOf("Primes 2–7", "Primes 2–13"))
    "optimization-arena" -> GameSetupProfile("Cargo inventory", "Higher settings increase inventory size; maximize value within capacity.", "Mastery I: larger inventory. Mastery II: exactly five items.", "Capacity and value clues")
    else -> error("Missing settings for $id")
}

@Composable
internal fun CompactSettingsPage(title: String, accent: Color, best: String, onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GameSpace, Color(0xFF020714))))
        .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundGameButton("‹", accent, "Back to games", onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = GameInk, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Previous best: $best", color = GameMuted, fontSize = 11.sp)
            }
            Text("⚙", color = accent, fontSize = 25.sp, modifier = Modifier.padding(6.dp))
        }
        content()
    }
}

@Composable
internal fun SettingsCard(title: String, subtitle: String?, accent: Color, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(Brush.linearGradient(listOf(accent.copy(.12f), GamePanel.copy(.8f))))
        .border(1.dp, accent.copy(.35f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = GameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = GameMuted, fontSize = 11.sp, lineHeight = 14.sp)
        content()
    }
}

@Composable
internal fun SettingsChoices(labels: List<String>, selected: (Int) -> Boolean, accent: Color,
    onSelect: (Int) -> Unit, columns: Int = labels.size, enabled: Boolean = true) {
    labels.indices.chunked(columns.coerceAtLeast(1)).forEach { indices ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            indices.forEach { index ->
                val active = selected(index)
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (active) Brush.verticalGradient(listOf(accent, accent.copy(.65f))) else
                        Brush.verticalGradient(listOf(Color.White.copy(.07f), Color.White.copy(.025f))))
                    .border(1.dp, if (active) accent else GameMuted.copy(.3f), RoundedCornerShape(11.dp))
                    .clickable(enabled = enabled) { onSelect(index) }
                    .semantics { role = Role.RadioButton; this.selected = active; if (!enabled) disabled() }
                    .padding(horizontal = 2.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                    Text(labels[index], color = if (active && accent.luminance() > .45f) GameSpace else GameInk, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 13.sp)
                }
            }
            repeat(columns - indices.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
internal fun LearningGameSettings(game: MathsGame, completed: Int, difficulty: Int, hints: Boolean,
    focus: Int, board: Int, onDifficulty: (Int) -> Unit, onHints: (Boolean) -> Unit,
    onFocus: (Int) -> Unit, onBoard: (Int) -> Unit, onBack: () -> Unit, onStart: () -> Unit,
    onReplay: () -> Unit) {
    val profile = gameSetupProfile(game.id)
    val hasScale = game.id in setOf("forge", "number-bonds", "shapes", "kitchen", "measure", "data", "data-story",
        "chance", "decimal-harbor", "math-market", "percent-studio", "geometry-proof", "calculus-climber", "optimization-arena")
    val scaleLabels = when (game.id) {
        "forge" -> listOf("2 digits", "3 digits", "4 digits", "5 digits")
        "number-bonds" -> listOf("2 cores", "3 cores", "4 cores", "5 cores")
        "shapes" -> listOf("3–5 pieces", "4–6 pieces", "5–7 pieces", "6–8 pieces")
        "data", "data-story" -> listOf("5 values", "7 values", "9 values", "11 values")
        "optimization-arena" -> listOf("7 items", "8 items", "9 items", "10 items")
        else -> listOf("Small", "Medium", "Large", "Largest")
    }
    val focusTitle = when (game.id) {
        "kitchen" -> "Recipe operations"
        "measure" -> "Rescue mission"
        "balance" -> "Variable placement"
        "potions", "ratio-rangers" -> "Ratio range"
        "vectors" -> "Movement basis"
        "integer-expedition" -> "Number-line steps"
        "matrix-mission" -> "Basis-vector range"
        "fractions" -> "Fraction tile set"
        "times-table" -> "Multiplication range"
        "number-theory" -> "Available primes"
        "patterns", "function-machine" -> "Function rule"
        "data", "data-story" -> "Data task"
        "chance" -> "Probability family"
        "logic", "combinatorics" -> "Formation type"
        "decimal-harbor" -> "Decimal operation"
        "math-market" -> "Shopping task"
        "percent-studio" -> "Percentage task"
        "geometry-proof" -> "Geometry topic"
        "calculus-climber" -> "Calculus topic"
        else -> "Practice focus"
    }
    CompactSettingsPage(game.title, game.accent, "${LocalPreviousGameBest.current} levels", onBack) {
        if (hasScale && board == 0 && !(game.id in listOf("data", "data-story") && focus == 1)) SettingsCard(profile.scaleTitle, "Starting scale; increases with progression.", game.accent) {
            SettingsChoices(scaleLabels, { it == difficulty }, game.accent, onDifficulty)
        }
        if (board == 0 && profile.topics.isNotEmpty()) {
            SettingsCard(focusTitle, null, game.accent) {
                if (profile.topics.size > 3) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text((profile.topics.getOrNull(focus) ?: "Mixed") + " ▾", color = game.accent)
                        }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            (listOf("Mixed") + profile.topics).forEachIndexed { index, topic ->
                                DropdownMenuItem(text = { Text(topic) }, onClick = { onFocus(index - 1); expanded = false })
                            }
                        }
                    }
                } else SettingsChoices(listOf("Mixed") + profile.topics, { it - 1 == focus }, game.accent,
                    { onFocus(it - 1) })
            }
        }
        SettingsCard("Challenge boards", if (board == 0) {
            if (hasScale) "120 progressive missions; mastery adds special constraints." else profile.scaleDescription
        } else profile.masteryDescription, game.accent) {
            SettingsChoices(listOf("Standard", "Mastery I", "Mastery II"), { it == board }, game.accent, onBoard)
        }
        SettingsCard(profile.hintDescription, null, game.accent) {
            SettingsChoices(listOf("Hints: on", "Hints: off"), { (it == 0) == hints }, game.accent, { onHints(it == 0) })
        }
        PrimaryGameButton(if (board > 0) "Start Mastery ${if (board == 1) "I" else "II"}" else
            if (completed == 0) "Start playing" else "Continue at level ${completed.coerceAtMost(119) + 1}", game.accent, onStart)
        if (completed > 0 && board == 0) SecondaryGameButton("Replay from level 1 • fresh missions", game.accent, onReplay)
    }
}
