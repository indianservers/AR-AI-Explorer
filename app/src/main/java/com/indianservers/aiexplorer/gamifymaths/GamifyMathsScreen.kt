package com.indianservers.aiexplorer.gamifymaths

import com.indianservers.aiexplorer.gamifymaths.probability.ProbabilityStatisticsArcadeGame
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Space = Color(0xFF05081A)
private val Panel = Color(0xE6121634)
private val Ink = Color(0xFFF5F7FF)
private val Muted = Color(0xFF9DA8CE)
private val Violet = Color(0xFF9B67FF)
private val Cyan = Color(0xFF43D9FF)
private val Green = Color(0xFF52E6B1)
private val Amber = Color(0xFFFFB84C)
private val Coral = Color(0xFFFF668F)

private enum class GameDestination { Home, Worlds, Progress, Profile }

private data class GameMission(
    val title: String,
    val prompt: String,
    val tokens: List<String>,
    val answer: String,
    val explanation: String,
)

private data class MathsGame(
    val id: String,
    val title: String,
    val currentTopic: String,
    val icon: String,
    val accent: Color,
    val subtopics: List<String>,
    val mechanic: String,
    val missions: List<GameMission>,
)

private val MathsGame.levelCount: Int
    get() = when (id) {
        "forge", "kitchen", "fractions" -> 6
        "balance" -> 72
        "shapes", "potions" -> 18
        "measure" -> 21
        "chance", "data" -> 45
        else -> missions.size
    }

private val Games = listOf(
    MathsGame(
        "forge", "Number Forge", "Prime Codes", "123", Violet,
        listOf("Place value", "Integers", "Factors", "Multiples", "Primes", "Divisibility", "Powers", "Roots"),
        "Drag number cores into the forge to construct the requested value.",
        listOf(
            GameMission("Prime ignition", "Drag the only prime number into the reactor.", listOf("21", "29", "39", "51"), "29", "29 has exactly two positive factors: 1 and 29."),
            GameMission("Factor shield", "Which value is a factor of 84?", listOf("5", "7", "11", "13"), "7", "84 = 7 x 12, so 7 divides 84 exactly."),
            GameMission("Power cell", "Select the value of 3 squared.", listOf("6", "8", "9", "12"), "9", "3 squared means 3 x 3, which equals 9."),
        ),
    ),
    MathsGame(
        "kitchen", "Maths Kitchen", "Operation Recipes", "MIX", Amber,
        listOf("Addition", "Subtraction", "Multiplication", "Division", "Order of operations", "Estimation", "Rounding", "Units"),
        "Drag the correct quantity into the recipe chamber.",
        listOf(
            GameMission("Orbital recipe", "A batch needs 6 portions of 4 units. How many units?", listOf("10", "20", "24", "28"), "24", "Six groups of four make 6 x 4 = 24."),
            GameMission("Smart estimate", "Best estimate for 198 + 304?", listOf("400", "500", "600", "700"), "500", "198 is about 200 and 304 is about 300; together they are about 500."),
        ),
    ),
    MathsGame(
        "fractions", "Fraction Factory", "Equivalent Parts", "1/2", Cyan,
        listOf("Equivalent fractions", "Comparison", "Mixed numbers", "Operations", "Decimals", "Percentages", "Conversions"),
        "Drag the matching fraction segment into the assembly ring.",
        listOf(
            GameMission("Equivalent module", "Which fraction is equivalent to 3/4?", listOf("4/6", "6/8", "8/10", "9/16"), "6/8", "Multiplying 3/4 by 2/2 gives 6/8."),
            GameMission("Decimal port", "Which decimal is equal to 1/5?", listOf("0.1", "0.2", "0.25", "0.5"), "0.2", "1 divided by 5 equals 0.2."),
        ),
    ),
    MathsGame(
        "potions", "Potion Lab", "Proportions", "2:3", Coral,
        listOf("Ratios", "Unit rates", "Direct proportion", "Inverse proportion", "Percentages", "Scale", "Mixtures", "Speed"),
        "Drag the correctly mixed vial into the analyser.",
        listOf(
            GameMission("Neon mixture", "Blue:red = 2:3. If blue is 6 ml, how much red?", listOf("4 ml", "6 ml", "9 ml", "12 ml"), "9 ml", "The scale factor is 3, so red is 3 x 3 = 9 ml."),
            GameMission("Unit rate", "A rover travels 24 km in 3 h. Select its unit rate.", listOf("6 km/h", "8 km/h", "12 km/h", "21 km/h"), "8 km/h", "24 divided by 3 is 8 kilometres per hour."),
        ),
    ),
    MathsGame(
        "balance", "Balance Vault", "Solve Equations", "x", Color(0xFF5EA4FF),
        listOf("Variables", "Expressions", "Equations", "Inequalities", "Identities", "Substitution", "Simultaneous equations"),
        "Drag the value that keeps both sides of the quantum balance equal.",
        listOf(
            GameMission("Vault equation", "Balance x + 5 = 12. What is x?", listOf("5", "6", "7", "17"), "7", "Subtract 5 from both sides: x = 12 - 5 = 7."),
            GameMission("Double lock", "Balance 2x = 18. What is x?", listOf("7", "8", "9", "16"), "9", "Divide both sides by 2: x = 9."),
        ),
    ),
    MathsGame(
        "shapes", "Shape Architect", "Angles & Symmetry", "△", Green,
        listOf("Angles", "Triangles", "Polygons", "Circles", "Symmetry", "Congruence", "Similarity", "Transformations"),
        "Drag the correct geometric component into the holographic blueprint.",
        listOf(
            GameMission("Triangle core", "Two angles are 55 and 65 degrees. Select the third.", listOf("50°", "60°", "70°", "80°"), "60°", "Angles in a triangle total 180 degrees; 180 - 55 - 65 = 60."),
            GameMission("Symmetry scan", "How many lines of symmetry does a square have?", listOf("2", "3", "4", "8"), "4", "A square has two diagonal and two midpoint symmetry lines."),
        ),
    ),
    MathsGame(
        "measure", "Rescue Engineer", "Measurement Missions", "m²", Color(0xFFFF8C5A),
        listOf("Length", "Mass", "Time", "Perimeter", "Area", "Surface area", "Volume", "Unit conversion"),
        "Drag the correct measurement module into the construction scanner.",
        listOf(
            GameMission("Habitat floor", "A floor is 8 m by 5 m. Select its area.", listOf("13 m²", "26 m²", "40 m²", "80 m²"), "40 m²", "Area of a rectangle is length x width: 8 x 5 = 40 square metres."),
            GameMission("Unit gate", "Convert 2.5 metres to centimetres.", listOf("25 cm", "250 cm", "2,500 cm", "0.25 cm"), "250 cm", "One metre is 100 centimetres, so 2.5 x 100 = 250."),
        ),
    ),
    MathsGame(
        "vectors", "Vector Voyager", "Coordinate Routes", "(x,y)", Color(0xFF4FD1C5),
        listOf("Coordinates", "Quadrants", "Slope", "Distance", "Midpoint", "Linear graphs", "Functions", "Transformations"),
        "Drag the correct navigation coordinate into the flight computer.",
        listOf(
            GameMission("Quadrant jump", "Which point lies in Quadrant II?", listOf("(3,4)", "(-3,4)", "(-3,-4)", "(3,-4)"), "(-3,4)", "In Quadrant II, x is negative and y is positive."),
            GameMission("Slope drive", "Slope from (0,0) to (3,6)?", listOf("1/2", "2", "3", "6"), "2", "Slope is rise/run = 6/3 = 2."),
        ),
    ),
    MathsGame(
        "patterns", "Pattern Core", "Sequence Signals", "∞", Color(0xFFB98CFF),
        listOf("Visual patterns", "Arithmetic sequences", "Geometric sequences", "Recursive rules", "Function machines"),
        "Drag the missing signal into the sequence core.",
        listOf(
            GameMission("Signal sequence", "Complete 4, 7, 10, 13, ...", listOf("14", "15", "16", "17"), "16", "The sequence increases by 3 each time, so 13 + 3 = 16."),
            GameMission("Growth pulse", "Complete 3, 6, 12, 24, ...", listOf("30", "36", "48", "72"), "48", "Each term doubles, so 24 x 2 = 48."),
        ),
    ),
    MathsGame(
        "data", "Data Detective", "Evidence Charts", "BAR", Color(0xFF67B7FF),
        listOf("Tables", "Charts", "Mean", "Median", "Mode", "Range", "Outliers", "Misleading graphs"),
        "Drag the valid evidence card into the investigation console.",
        listOf(
            GameMission("Central clue", "Find the mean of 4, 6, 8, 10.", listOf("6", "7", "8", "9"), "7", "The total is 28; dividing by 4 values gives a mean of 7."),
            GameMission("Range scan", "Find the range of 3, 11, 7, 5.", listOf("6", "7", "8", "11"), "8", "Range = maximum - minimum = 11 - 3 = 8."),
        ),
    ),
    MathsGame(
        "chance", "Chance Reactor", "Probability Fields", "P", Color(0xFFFFD05A),
        listOf("Sample spaces", "Experimental probability", "Compound events", "Expected value", "Dependent events", "Fairness"),
        "Drag the correct probability crystal into the chance reactor.",
        listOf(
            GameMission("Dice field", "Probability of rolling an even number on a fair six-sided die?", listOf("1/6", "1/3", "1/2", "2/3"), "1/2", "Three of six outcomes are even: 2, 4 and 6. So 3/6 = 1/2."),
            GameMission("Coin gate", "Probability of two heads from two fair coin flips?", listOf("1/2", "1/3", "1/4", "3/4"), "1/4", "The equally likely outcomes are HH, HT, TH and TT; only HH works."),
        ),
    ),
    MathsGame(
        "logic", "Logic Grid", "Deduction Paths", "IQ", Color(0xFFFF719A),
        listOf("Deduction", "Classification", "Permutations", "Combinations", "Counting paths", "Spatial reasoning", "Optimisation"),
        "Drag the only logically valid command into the escape grid.",
        listOf(
            GameMission("Code deduction", "All Zips are Lums. No Lums are Tars. Can a Zip be a Tar?", listOf("Always", "Sometimes", "Never", "Unknown"), "Never", "Every Zip is a Lum, and no Lum can be a Tar; therefore no Zip can be a Tar."),
            GameMission("Route count", "Two shirts and three trousers make how many outfits?", listOf("5", "6", "8", "9"), "6", "For each of 2 shirts there are 3 choices: 2 x 3 = 6."),
        ),
    ),
)

private val DisplayGames: List<MathsGame>
    get() {
        val order = listOf("forge", "kitchen", "fractions", "balance", "shapes", "potions", "measure", "vectors", "patterns", "data", "chance", "logic")
        return Games.sortedBy { order.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
    }

@Composable
fun GamifyMathsRoot(onExit: () -> Unit) {
    var destinationName by rememberSaveable { mutableStateOf(GameDestination.Home.name) }
    var selectedGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val completed = remember { mutableStateMapOf<String, Int>() }
    val destination = GameDestination.valueOf(destinationName)
    val selectedGame = Games.firstOrNull { it.id == selectedGameId }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF20205B), Space, Color(0xFF030510)),
                    center = Offset(260f, 140f),
                    radius = 1050f,
                ),
            )
            .semantics { contentDescription = "GamifyMaths interactive games module" },
    ) {
        StarField()
        if (selectedGame != null) {
            val linkedProgressKeys = if (selectedGame.id in setOf("chance", "data")) listOf("chance", "data") else listOf(selectedGame.id)
            val currentCompleted = linkedProgressKeys.maxOf { completed[it] ?: 0 }
            val recordComplete: (Int) -> Unit = { value ->
                linkedProgressKeys.forEach { key -> completed[key] = maxOf(completed[key] ?: 0, value) }
            }
            when (selectedGame.id) {
                "forge" -> NumberForgeGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "kitchen" -> MathsKitchenGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "fractions" -> FractionFactoryGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "potions" -> PotionLabGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "balance" -> AlgebraAdventureGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "shapes" -> ShapeArchitectGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "measure" -> RescueEngineerGame(currentCompleted, { selectedGameId = null }, recordComplete)
                "chance" -> ProbabilityStatisticsArcadeGame(currentCompleted, 0, { selectedGameId = null }, recordComplete)
                "data" -> ProbabilityStatisticsArcadeGame(currentCompleted, 11, { selectedGameId = null }, recordComplete)
                else -> GameMissionScreen(
                    game = selectedGame,
                    completedMissions = currentCompleted,
                    onBack = { selectedGameId = null },
                    onComplete = recordComplete,
                )
            }
        } else {
            when (destination) {
                GameDestination.Home -> GameHome(completed, onExit) { selectedGameId = it.id }
                GameDestination.Worlds -> WorldsScreen(completed) { selectedGameId = it.id }
                GameDestination.Progress -> ProgressScreen(completed)
                GameDestination.Profile -> PlayerProfileScreen(completed)
            }
            BottomNavigation(
                selected = destination,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { destinationName = it.name }
        }
    }
}

@Composable
private fun StarField() {
    Canvas(Modifier.fillMaxSize()) {
        val points = listOf(.08f to .12f, .22f to .07f, .41f to .16f, .68f to .08f, .87f to .2f, .13f to .43f, .74f to .48f)
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 2 == 0) Cyan.copy(.38f) else Violet.copy(.42f),
                radius = if (index % 3 == 0) 3.5f else 2f,
                center = Offset(size.width * point.first, size.height * point.second),
            )
        }
    }
}

@Composable
private fun GameHome(completed: Map<String, Int>, onExit: () -> Unit, onOpenGame: (MathsGame) -> Unit) {
    val continueGame = DisplayGames.firstOrNull { (completed[it.id] ?: 0) < it.levelCount } ?: DisplayGames.first()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 94.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("GamifyMaths", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("PLAY  •  THINK  •  MASTER", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            }
            GlossyPill("EXIT", Violet, onExit)
        }
        JourneyCard(completed.values.sum(), onClick = { onOpenGame(continueGame) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Choose Your Mission", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("12 WORLDS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DisplayGames.forEach { game -> GameCard(game, completed[game.id] ?: 0, Modifier.weight(1f), onOpenGame) }
        }
    }
}

@Composable
private fun JourneyCard(totalCompleted: Int, onClick: () -> Unit) {
    val progress = (totalCompleted / Games.sumOf { it.levelCount }.toFloat()).coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF34216E), Color(0xFF193B70))))
            .border(1.dp, Violet.copy(.7f), RoundedCornerShape(25.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("YOUR JOURNEY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Explorer World", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Mission ${totalCompleted + 1}", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("↗", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Violet,
            trackColor = Space.copy(.7f),
        )
        Text("${(progress * 100).toInt()}% universe mastery  •  Tap to continue", color = Ink, fontSize = 10.sp)
    }
}

@Composable
private fun GameCard(game: MathsGame, completed: Int, modifier: Modifier = Modifier, onClick: (MathsGame) -> Unit) {
    Column(
        modifier
            .width(164.dp)
            .heightIn(min = 178.dp)
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = game.accent.copy(.35f), spotColor = game.accent.copy(.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(game.accent.copy(.28f), Panel, Color(0xF20A0C22))))
            .border(1.dp, game.accent.copy(.78f), RoundedCornerShape(22.dp))
            .clickable { onClick(game) }
            .semantics { contentDescription = "Open ${game.title}, current topic ${game.currentTopic}" }
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(game.accent.copy(.18f)).border(1.dp, game.accent.copy(.55f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(game.icon, color = game.accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Text(game.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
        Text(game.currentTopic, color = game.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("${completed}/${game.levelCount} MISSIONS", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { (completed / game.levelCount.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = game.accent,
            trackColor = Color.White.copy(.09f),
        )
    }
}

@Composable
private fun WorldsScreen(completed: Map<String, Int>, onOpenGame: (MathsGame) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 94.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("12 Maths Worlds", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Every world teaches a different way of thinking.", color = Muted, fontSize = 12.sp)
        DisplayGames.forEach { game ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel)
                    .border(1.dp, game.accent.copy(.5f), RoundedCornerShape(20.dp)).clickable { onOpenGame(game) }.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(game.accent.copy(.2f)), contentAlignment = Alignment.Center) {
                    Text(game.icon, color = game.accent, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text(game.title, color = Ink, fontWeight = FontWeight.ExtraBold)
                    Text(game.subtopics.take(4).joinToString(" • "), color = Muted, fontSize = 9.sp, maxLines = 2)
                }
                Text("${completed[game.id] ?: 0}/${game.levelCount}", color = game.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressScreen(completed: Map<String, Int>) {
    val earned = completed.values.sum()
    val total = Games.sumOf { it.levelCount }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 94.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Mission Progress", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(Violet.copy(.3f), Cyan.copy(.15f))))
                .border(1.dp, Violet.copy(.6f), RoundedCornerShape(24.dp)).padding(18.dp),
        ) {
            Text("$earned / $total", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("missions mastered this session", color = Cyan, fontWeight = FontWeight.Bold)
        }
        DisplayGames.forEach { game ->
            val value = completed[game.id] ?: 0
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(game.title, color = Ink, fontWeight = FontWeight.Bold)
                    Text("$value/${game.levelCount}", color = game.accent)
                }
                LinearProgressIndicator(
                    progress = { (value / game.levelCount.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = game.accent,
                    trackColor = Color.White.copy(.08f),
                )
            }
        }
    }
}

@Composable
private fun PlayerProfileScreen(completed: Map<String, Int>) {
    var sound by rememberSaveable { mutableStateOf(true) }
    var haptics by rememberSaveable { mutableStateOf(true) }
    var highContrast by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 94.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Explorer Profile", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Panel).border(1.dp, Violet.copy(.6f), RoundedCornerShape(24.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(62.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan, Violet))), contentAlignment = Alignment.Center) {
                Text("12", color = Space, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Column {
                Text("Maths Explorer", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("${completed.values.sum()} missions mastered", color = Cyan, fontSize = 11.sp)
            }
        }
        SettingRow("Mission sounds", "Audio feedback for moves and discoveries", sound) { sound = it }
        SettingRow("Haptic energy", "Tactile feedback when objects snap into place", haptics) { haptics = it }
        SettingRow("High contrast", "Stronger edges around draggable objects", highContrast) { highContrast = it }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun GameMissionScreen(game: MathsGame, completedMissions: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    var missionIndex by rememberSaveable(game.id) { mutableIntStateOf(completedMissions.coerceIn(0, game.missions.lastIndex)) }
    var selected by rememberSaveable(game.id, missionIndex) { mutableStateOf<String?>(null) }
    var result by rememberSaveable(game.id, missionIndex) { mutableStateOf<Boolean?>(null) }
    val mission = game.missions[missionIndex]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlossyPill("‹ WORLDS", game.accent, onBack)
            Text("MISSION ${missionIndex + 1}/${game.missions.size}", color = game.accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Column {
            Text(game.title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(game.mechanic, color = Muted, fontSize = 11.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            game.subtopics.forEachIndexed { index, topic ->
                Text(
                    topic,
                    color = if (index == missionIndex) Space else Ink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(if (index == missionIndex) game.accent else Color.White.copy(.07f))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(game.accent.copy(.23f), Panel)))
                .border(1.dp, game.accent.copy(.65f), RoundedCornerShape(24.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(mission.title.uppercase(), color = game.accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text(mission.prompt, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
        DropReactor(game, selected, result)
        Text("DRAG A CORE INTO THE REACTOR", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mission.tokens.forEach { token ->
                DraggableAnswer(token, game.accent) {
                    selected = token
                    result = token == mission.answer
                    if (result == true) onComplete(missionIndex + 1)
                }
            }
        }
        if (result != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background((if (result == true) Green else Coral).copy(.13f))
                    .border(1.dp, (if (result == true) Green else Coral).copy(.65f), RoundedCornerShape(20.dp)).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (result == true) "CORE STABLE — CORRECT" else "ENERGY MISMATCH — TRY AGAIN", color = if (result == true) Green else Coral, fontWeight = FontWeight.Black)
                Text(if (result == true) mission.explanation else "That core does not satisfy the mission. Compare it with the mathematical condition and drag another.", color = Ink, fontSize = 12.sp)
                if (result == true) {
                    Button(
                        onClick = {
                            if (missionIndex < game.missions.lastIndex) {
                                missionIndex += 1
                                selected = null
                                result = null
                            } else onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = game.accent, contentColor = Space),
                    ) { Text(if (missionIndex < game.missions.lastIndex) "Next mission" else "Complete world", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun DropReactor(game: MathsGame, selected: String?, result: Boolean?) {
    Box(
        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(listOf(game.accent.copy(.28f), Color(0xFF090D26))))
            .border(2.dp, game.accent.copy(if (selected == null) .38f else .9f), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(120.dp)) {
            drawCircle(game.accent.copy(.12f), radius = size.minDimension * .47f)
            drawCircle(game.accent.copy(.65f), radius = size.minDimension * .40f, style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
            drawArc(game.accent, 22f, 245f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(8f, cap = StrokeCap.Round))
        }
        Text(
            selected ?: game.icon,
            color = when (result) { true -> Green; false -> Coral; null -> game.accent },
            fontSize = if (selected == null) 22.sp else 28.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun DraggableAnswer(label: String, accent: Color, onDropped: () -> Unit) {
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .width(78.dp)
            .height(58.dp)
            .graphicsLayer { translationX = dragX; translationY = dragY }
            .shadow(8.dp, RoundedCornerShape(17.dp), ambientColor = accent.copy(.3f), spotColor = accent.copy(.3f))
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(.3f), Panel)))
            .border(1.dp, accent.copy(.75f), RoundedCornerShape(17.dp))
            .pointerInput(label) {
                detectDragGestures(
                    onDragEnd = {
                        val movedUp = dragY < -35f
                        dragX = 0f
                        dragY = 0f
                        if (movedUp) onDropped()
                    },
                    onDragCancel = { dragX = 0f; dragY = 0f },
                ) { change, amount ->
                    change.consume()
                    dragX += amount.x
                    dragY += amount.y
                }
            }
            .clickable(onClick = onDropped)
            .semantics { contentDescription = "Drag answer $label into reactor" },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Ink, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomNavigation(
    selected: GameDestination,
    modifier: Modifier = Modifier,
    onSelect: (GameDestination) -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).shadow(22.dp, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp)).background(Color(0xF20B0E26)).border(1.dp, Violet.copy(.55f), RoundedCornerShape(25.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameDestination.entries.forEach { destination ->
            val active = selected == destination
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(17.dp)).background(if (active) Violet.copy(.22f) else Color.Transparent)
                    .clickable { onSelect(destination) }.padding(vertical = 7.dp)
                    .semantics { contentDescription = "Open ${destination.name}" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    when (destination) {
                        GameDestination.Home -> "⌂"
                        GameDestination.Worlds -> "◎"
                        GameDestination.Progress -> "▥"
                        GameDestination.Profile -> "○"
                    },
                    color = if (active) Violet else Muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(destination.name, color = if (active) Ink else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GlossyPill(label: String, accent: Color, onClick: () -> Unit) {
    Text(
        label,
        color = Ink,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clip(CircleShape).background(Brush.horizontalGradient(listOf(accent.copy(.4f), Color.White.copy(.08f))))
            .border(1.dp, accent.copy(.7f), CircleShape).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 10.dp),
    )
}
