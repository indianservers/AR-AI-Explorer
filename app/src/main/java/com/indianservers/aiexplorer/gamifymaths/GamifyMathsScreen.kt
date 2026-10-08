package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.indianservers.aiexplorer.R
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

internal enum class GameDifficulty(val label: String) {
    Beginner("BEGINNER"),
    Intermediate("INTERMEDIATE"),
    Advanced("ADVANCED"),
    Expert("EXPERT"),
}

internal data class MathsGame(
    val id: String,
    val title: String,
    val currentTopic: String,
    val icon: String,
    val accent: Color,
    val subtopics: List<String>,
    val mechanic: String,
    val difficulty: GameDifficulty? = null,
)

private val MathsGame.levelCount: Int
    get() = if (id.startsWith("speed-")) 1 else GeneratedLevelCount

private val CoreGames = listOf(
    MathsGame(
        "speed-basic", "Calculation Sprint", "Rapid Arithmetic", "30s", Cyan,
        listOf("Addition", "Subtraction", "Multiplication", "Division", "Percentages", "1–3 digit numbers"),
        "Configure a timer and solve as many arithmetic problems as possible.",
    ),
    MathsGame(
        "speed-advanced", "Advanced Calculation Sprint", "Algebra & Trigonometry", "ƒx", Violet,
        listOf("Advanced calculation", "Trigonometry", "Algebra", "Order of operations", "Exact values"),
        "Configure advanced topics and race against the clock.",
    ),
    MathsGame(
        "forge", "Number Forge", "Prime Codes", "123", Violet,
        listOf("Place value", "Integers", "Factors", "Multiples", "Primes", "Divisibility", "Powers", "Roots"),
        "Drag number cores into the forge to construct the requested value.",
    ),
    MathsGame(
        "kitchen", "Maths Kitchen", "Operation Recipes", "MIX", Amber,
        listOf("Addition", "Subtraction", "Multiplication", "Division", "Order of operations", "Estimation", "Rounding", "Units"),
        "Drag the correct quantity into the recipe chamber.",
    ),
    MathsGame(
        "fractions", "Fraction Quest", "Equivalent Parts", "1/2", Cyan,
        listOf("Equivalent fractions", "Comparison", "Mixed numbers", "Operations", "Decimals", "Percentages", "Conversions"),
        "Drag the matching fraction segment into the assembly ring.",
    ),
    MathsGame(
        "potions", "Potion Lab", "Proportions", "2:3", Coral,
        listOf("Ratios", "Unit rates", "Direct proportion", "Inverse proportion", "Percentages", "Scale", "Mixtures", "Speed"),
        "Drag the correctly mixed vial into the analyser.",
    ),
    MathsGame(
        "balance", "Equation Escape", "Solve Equations", "x", Color(0xFF5EA4FF),
        listOf("Variables", "Expressions", "Equations", "Inequalities", "Identities", "Substitution", "Simultaneous equations"),
        "Drag the value that keeps both sides of the quantum balance equal.",
    ),
    MathsGame(
        "shapes", "Geometry Builder", "Angles & Symmetry", "△", Green,
        listOf("Angles", "Triangles", "Polygons", "Circles", "Symmetry", "Congruence", "Similarity", "Transformations"),
        "Drag the correct geometric component into the holographic blueprint.",
    ),
    MathsGame(
        "measure", "Rescue Engineer", "Measurement Missions", "m²", Color(0xFFFF8C5A),
        listOf("Length", "Mass", "Time", "Perimeter", "Area", "Surface area", "Volume", "Unit conversion"),
        "Drag the correct measurement module into the construction scanner.",
    ),
    MathsGame(
        "vectors", "Vector Voyager", "Coordinate Routes", "(x,y)", Color(0xFF4FD1C5),
        listOf("Coordinates", "Quadrants", "Slope", "Distance", "Midpoint", "Linear graphs", "Functions", "Transformations"),
        "Drag the correct navigation coordinate into the flight computer.",
    ),
    MathsGame(
        "patterns", "Pattern Detective", "Sequence Signals", "∞", Color(0xFFB98CFF),
        listOf("Visual patterns", "Arithmetic sequences", "Geometric sequences", "Recursive rules", "Function machines"),
        "Drag the missing signal into the sequence core.",
    ),
    MathsGame(
        "data", "Data Detective", "Evidence Charts", "BAR", Color(0xFF67B7FF),
        listOf("Tables", "Charts", "Mean", "Median", "Mode", "Range", "Outliers", "Misleading graphs"),
        "Drag the valid evidence card into the investigation console.",
    ),
    MathsGame(
        "chance", "Chance Reactor", "Probability Fields", "P", Color(0xFFFFD05A),
        listOf("Sample spaces", "Experimental probability", "Compound events", "Expected value", "Dependent events", "Fairness"),
        "Drag the correct probability crystal into the chance reactor.",
    ),
    MathsGame(
        "logic", "Logic Grid", "Deduction Paths", "IQ", Color(0xFFFF719A),
        listOf("Deduction", "Classification", "Permutations", "Combinations", "Counting paths", "Spatial reasoning", "Optimisation"),
        "Drag the only logically valid command into the escape grid.",
    ),
)

private fun coreDifficulty(gameId: String): GameDifficulty? = when (gameId) {
    "forge", "kitchen", "fractions", "shapes" -> GameDifficulty.Beginner
    "potions", "balance", "measure", "vectors", "patterns", "data", "chance" -> GameDifficulty.Intermediate
    "logic" -> GameDifficulty.Advanced
    else -> null
}

private val Games = CoreGames.map { it.copy(difficulty = coreDifficulty(it.id)) } + expandedMathsGames()
internal fun gamifyGamesForAudit(): List<MathsGame> = Games

private val DisplayGames: List<MathsGame>
    get() {
        val order = listOf("speed-basic", "speed-advanced", "forge", "kitchen", "fractions", "balance", "shapes", "potions", "measure", "vectors", "patterns", "data", "chance", "logic") +
            expandedMathsGameIds
        return Games.sortedBy { order.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
    }

internal data class GameCatalogueAudit(
    val id: String,
    val title: String,
    val difficulty: GameDifficulty?,
    val missionCount: Int,
)

internal fun gamifyCatalogueAudit(): List<GameCatalogueAudit> =
    Games.map { GameCatalogueAudit(it.id, it.title, it.difficulty, it.levelCount) }

internal data class GameWorkshopAudit(val gameId:String,val level:Int,val kind:WorkshopKind,val prompt:String,val mastery:Int)
internal fun gamifyWorkshopAudit():List<GameWorkshopAudit> = Games.filterNot{it.id.startsWith("speed-")}.flatMap{game->
    (0 until GeneratedLevelCount).map{level->
        val round=generateWorkshopChallenge(game.id,level,0,level.toLong())
        GameWorkshopAudit(game.id,level+1,round.kind,round.prompt,round.mastery)
    }
}

@Composable
fun GamifyMathsRoot(onExit: () -> Unit) {
    var destinationName by rememberSaveable { mutableStateOf(GameDestination.Home.name) }
    var selectedGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("maths_games", 0) }
    val completed = remember { mutableStateMapOf<String, Int>().apply {
        Games.forEach { put(it.id, preferences.getInt("progress_${it.id}", 0)) }
    } }
    val bestScores = remember { mutableStateMapOf<String, Int>().apply {
        Games.forEach { put(it.id, preferences.getInt("best_${it.id}", completed[it.id] ?: 0)) }
    } }
    var compactPlay by remember { mutableStateOf(preferences.getBoolean("compact_play", true)) }
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
            val previousBest = remember(selectedGame.id) { bestScores[selectedGame.id] ?: 0 }
            val linkedProgressKeys = listOf(selectedGame.id)
            val currentCompleted = linkedProgressKeys.maxOf { completed[it] ?: 0 }
            val recordComplete: (Int) -> Unit = { value ->
                linkedProgressKeys.forEach { key ->
                    bestScores[key] = maxOf(bestScores[key] ?: 0, value)
                    completed[key] = maxOf(completed[key] ?: 0, if (key.startsWith("speed-")) minOf(value, 1) else value)
                    preferences.edit().putInt("progress_$key", completed[key] ?: 0)
                        .putInt("best_$key", bestScores[key] ?: 0).apply()
                }
            }
            CompositionLocalProvider(LocalCompactPlayPreference provides compactPlay,
                LocalCompactGameLayout provides compactPlay,
                LocalPreviousGameBest provides previousBest) {
            when (selectedGame.id) {
                "speed-basic" -> SpeedCalculationGame(SpeedCalculationMode.Basic, { selectedGameId = null }, recordComplete)
                "speed-advanced" -> SpeedCalculationGame(SpeedCalculationMode.Advanced, { selectedGameId = null }, recordComplete)
                else -> GeneratedMathGameScreen(selectedGame, currentCompleted, { selectedGameId = null }, recordComplete)
            }
            }
        } else {
            when (destination) {
                GameDestination.Home -> GameHome(completed, bestScores, onExit, { destinationName = GameDestination.Profile.name }) { selectedGameId = it.id }
                GameDestination.Worlds -> WorldsScreen(bestScores) { selectedGameId = it.id }
                GameDestination.Progress -> ProgressScreen(completed)
                GameDestination.Profile -> PlayerProfileScreen(bestScores, compactPlay) { value ->
                    compactPlay = value
                    preferences.edit().putBoolean("compact_play", value).apply()
                }
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
private fun GameHome(completed: Map<String, Int>, bestScores: Map<String, Int>, onExit: () -> Unit, onSettings: () -> Unit, onOpenGame: (MathsGame) -> Unit) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundGameButton("⚙", Cyan, "Game settings", onSettings)
                GlossyPill("EXIT", Violet, onExit)
            }
        }
        JourneyCard(completed.values.sum(), onClick = { onOpenGame(continueGame) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Choose Your Mission", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("${Games.size} GAMES", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        GameCardGrid(bestScores, onOpenGame)
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
private fun DifficultyTag(difficulty: GameDifficulty, compact: Boolean = false) {
    val color = when (difficulty) {
        GameDifficulty.Beginner -> Green
        GameDifficulty.Intermediate -> Cyan
        GameDifficulty.Advanced -> Amber
        GameDifficulty.Expert -> Coral
    }
    Text(
        difficulty.label,
        color = color,
        fontSize = if (compact) 8.sp else 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = if (compact) 0.5.sp else 1.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(.13f))
            .border(1.dp, color.copy(.7f), CircleShape)
            .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = if (compact) 3.dp else 4.dp),
    )
}

@Composable
private fun GameCardGrid(completed: Map<String, Int>, onOpenGame: (MathsGame) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 12.dp) / 2
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DisplayGames.forEach { game ->
                GameCard(game, completed[game.id] ?: 0, Modifier.width(cardWidth), onOpenGame)
            }
        }
    }
}

@Composable
private fun GameCard(game: MathsGame, completed: Int, modifier: Modifier = Modifier, onClick: (MathsGame) -> Unit) {
    Column(
        modifier
            .aspectRatio(1f)
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = game.accent.copy(.35f), spotColor = game.accent.copy(.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(game.accent.copy(.28f), Panel, Color(0xF20A0C22))))
            .border(1.dp, game.accent.copy(.78f), RoundedCornerShape(22.dp))
            .clickable { onClick(game) }
            .semantics {
                contentDescription = buildString {
                    append("Open ${game.title}, current topic ${game.currentTopic}")
                    game.difficulty?.let { append(", ${it.label.lowercase()} difficulty") }
                }
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(18.dp)).background(game.accent.copy(.18f)).border(1.dp, game.accent.copy(.55f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(gameCardArtwork(game.id)), contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Text(game.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
        Spacer(Modifier.weight(1f))
        Text(if (game.id.startsWith("speed-")) "BEST $completed SOLVED" else "BEST $completed/${game.levelCount} MISSIONS", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        Text("${Games.size} Maths Games", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("${Games.count { it.difficulty != null }} tagged learning worlds from beginner to expert.", color = Muted, fontSize = 12.sp)
        GameCardGrid(completed, onOpenGame)
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
            Text("missions mastered", color = Cyan, fontWeight = FontWeight.Bold)
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
private fun PlayerProfileScreen(completed: Map<String, Int>, compactPlay: Boolean, onCompactPlay: (Boolean) -> Unit) {
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
                Text("∑", color = Space, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Column {
                Text("Maths Explorer", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your saved personal bests", color = Cyan, fontSize = 11.sp)
            }
        }
        SettingRow("Compact play", "Smaller boards and fewer instructions on phones", compactPlay, onCompactPlay)
        Text("Previous best scores", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        DisplayGames.forEach { game ->
            Text("${game.title}: ${completed[game.id] ?: 0} ${if (game.id.startsWith("speed-")) "solved" else "missions mastered"}", color = Muted, fontSize = 12.sp)
        }
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
internal fun GameMissionScreen(game: MathsGame, completedMissions: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    GeneratedMathGameScreen(game, completedMissions, onBack, onComplete)
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
