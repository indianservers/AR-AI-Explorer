package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.gamifymaths.DraggableGameTile
import com.indianservers.aiexplorer.gamifymaths.GameBlue
import com.indianservers.aiexplorer.gamifymaths.GameGold
import com.indianservers.aiexplorer.gamifymaths.GameGreen
import com.indianservers.aiexplorer.gamifymaths.GameInk
import com.indianservers.aiexplorer.gamifymaths.GameIntroScreen
import com.indianservers.aiexplorer.gamifymaths.GameMuted
import com.indianservers.aiexplorer.gamifymaths.GamePanel
import com.indianservers.aiexplorer.gamifymaths.GameProgress
import com.indianservers.aiexplorer.gamifymaths.GamePurple
import com.indianservers.aiexplorer.gamifymaths.GameRed
import com.indianservers.aiexplorer.gamifymaths.GameScreen
import com.indianservers.aiexplorer.gamifymaths.GameSpace
import com.indianservers.aiexplorer.gamifymaths.GlossyPanel
import com.indianservers.aiexplorer.gamifymaths.LocalCompactGameLayout
import com.indianservers.aiexplorer.gamifymaths.PrimaryGameButton
import com.indianservers.aiexplorer.gamifymaths.ResultPanel
import com.indianservers.aiexplorer.gamifymaths.RoundGameButton
import com.indianservers.aiexplorer.gamifymaths.ScorePill
import com.indianservers.aiexplorer.gamifymaths.SecondaryGameButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private enum class ArcadeRoute { Intro, Map, Play }

@Composable
internal fun ProbabilityStatisticsArcadeGame(
    completed: Int,
    initialGame: Int,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit,
) {
    val games = ProbabilityArcadeGames
    val total = games.sumOf { it.levels.size }
    var routeName by rememberSaveable { mutableStateOf(ArcadeRoute.Intro.name) }
    var gameIndex by rememberSaveable { mutableIntStateOf(initialGame.coerceIn(games.indices)) }
    var globalLevel by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, total - 1)) }
    when (ArcadeRoute.valueOf(routeName)) {
        ArcadeRoute.Intro -> GameIntroScreen(
            number = 11,
            title = "Probability Arcade",
            subtitle = "Probability, distributions and statistics • 15 games.",
            accent = Color(0xFF24AFC0),
            concepts = listOf("P" to "Chance", "↻" to "Experiments", "μ" to "Statistics", "σ" to "Distributions"),
            completed = completed,
            total = total,
            onBack = onBack,
            onStart = {
                globalLevel = completed.coerceIn(0, total - 1)
                gameIndex = globalLevel / 3
                routeName = ArcadeRoute.Play.name
            },
            onMap = { routeName = ArcadeRoute.Map.name },
        )
        ArcadeRoute.Map -> ProbabilityArcadeMap(
            games = games,
            completed = completed,
            onBack = { routeName = ArcadeRoute.Intro.name },
        ) { selected ->
            gameIndex = selected
            val first = selected * 3
            globalLevel = maxOf(first, completed.coerceAtMost(first + 2))
            routeName = ArcadeRoute.Play.name
        }
        ArcadeRoute.Play -> {
            val challenge = games[gameIndex].levels[globalLevel % 3]
            ArcadePlayScreen(
                game = games[gameIndex],
                stage = globalLevel % 3,
                challenge = challenge,
                globalLevel = globalLevel,
                onBack = { routeName = ArcadeRoute.Map.name },
                onSolved = {
                    onComplete(globalLevel + 1)
                    if (globalLevel < total - 1) {
                        globalLevel++
                        gameIndex = globalLevel / 3
                    } else routeName = ArcadeRoute.Map.name
                },
            )
        }
    }
}

@Composable
private fun ProbabilityArcadeMap(
    games: List<ArcadeMiniGame>,
    completed: Int,
    onBack: () -> Unit,
    onGame: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF25205B), GameSpace, Color(0xFF020715))))
            .verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundGameButton("‹", GamePurple, "Back", onBack)
                Column {
                    Text("15 FUN GAMES", color = GameInk, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("PROBABILITY • DISTRIBUTIONS • STATISTICS", color = Color(0xFF58E2DE), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
            ScorePill("${completed}/45", GameGold)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 1100.dp -> 5
                maxWidth >= 760.dp -> 3
                maxWidth >= 520.dp -> 2
                else -> 1
            }
            val gap = 12.dp
            val cardWidth = (maxWidth - gap * (columns - 1)) / columns
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                games.forEachIndexed { index, game ->
                    val unlocked = index <= completed / 3
                    ProbabilityGameCard(game, index, completed, unlocked, Modifier.width(cardWidth)) { onGame(index) }
                }
            }
        }
    }
}

@Composable
private fun ProbabilityGameCard(game: ArcadeMiniGame, index: Int, completed: Int, unlocked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val gameCompleted = (completed - index * 3).coerceIn(0, 3)
    Column(
        modifier.heightIn(min = 235.dp).background(if (unlocked) game.accent.copy(.2f) else Color.White.copy(.05f), RoundedCornerShape(22.dp))
            .border(1.dp, if (unlocked) game.accent.copy(.75f) else GameMuted.copy(.2f), RoundedCornerShape(22.dp))
            .clickable(enabled = unlocked, onClick = onClick).focusable(unlocked).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(38.dp).background(if (unlocked) game.accent else GameMuted.copy(.2f), CircleShape), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = if (unlocked) GameSpace else GameMuted, fontWeight = FontWeight.Black)
            }
            Text(if (unlocked) "PLAY ›" else "LOCK", color = if (unlocked) GameGreen else GameMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Text(game.title, color = if (unlocked) GameInk else GameMuted, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        ArcadeIcon(game.visual, game.accent, Modifier.fillMaxWidth().height(92.dp))
        Text(game.goal, color = GameMuted, fontSize = 10.sp, maxLines = 2)
        GameProgress(gameCompleted / 3f, game.accent)
    }
}

@Composable
private fun ArcadePlayScreen(
    game: ArcadeMiniGame,
    stage: Int,
    challenge: ArcadeChallenge,
    globalLevel: Int,
    onBack: () -> Unit,
    onSolved: () -> Unit,
) {
    var selected by rememberSaveable(game.id, stage) { mutableStateOf("") }
    var result by rememberSaveable(game.id, stage) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(game.id, stage) { mutableStateOf(false) }
    var experimentCount by rememberSaveable(game.id, stage) { mutableIntStateOf(0) }
    var successes by rememberSaveable(game.id, stage) { mutableIntStateOf(0) }
    GameScreen("${game.title} • ${stage + 1}/3", globalLevel + 1, game.accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(challenge.hint, color = GameInk) }
        GlossyPanel(game.accent) {
            Text(challenge.prompt, color = GameInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcadeExperiment(game, challenge, experimentCount, successes, Modifier.weight(1.15f)) { success ->
                        experimentCount++
                        if (success) successes++
                    }
                    ArcadeAnswerPanel(game, challenge, selected, Modifier.weight(.85f)) { selected = it; result = null }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeExperiment(game, challenge, experimentCount, successes) { success ->
                        experimentCount++
                        if (success) successes++
                    }
                    ArcadeAnswerPanel(game, challenge, selected) { selected = it; result = null }
                }
            }
        }
        PrimaryGameButton("Check Answer", GameGreen, { result = selected == challenge.answer }, enabled = selected.isNotBlank())
        result?.let { ResultPanel(it, challenge.explanation, "Review the outcomes, data or distribution and choose another result.", onSolved) }
    }
}

@Composable
private fun ArcadeExperiment(
    game: ArcadeMiniGame,
    challenge: ArcadeChallenge,
    experimentCount: Int,
    successes: Int,
    modifier: Modifier = Modifier,
    onExperiment: (Boolean) -> Unit,
) {
    val compact = LocalCompactGameLayout.current
    val rotation by animateFloatAsState(
        targetValue = experimentCount * 137f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 85f),
        label = "probability experiment animation",
    )
    Column(
        modifier.fillMaxWidth().heightIn(min = if (compact) 235.dp else 310.dp).background(
            Brush.radialGradient(listOf(game.accent.copy(.25f), GamePanel)),
            RoundedCornerShape(24.dp),
        ).border(1.dp, game.accent.copy(.7f), RoundedCornerShape(24.dp)).padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        ArcadeIcon(game.visual, game.accent, Modifier.fillMaxWidth().height(if (compact) 145.dp else 205.dp).graphicsLayer {
            if (game.visual == ArcadeVisual.Spinner) rotationZ = rotation
            if (game.visual == ArcadeVisual.Coin) rotationY = rotation
        }, challenge.values)
        if (game.visual in setOf(ArcadeVisual.Spinner, ArcadeVisual.Coin, ArcadeVisual.Sampling, ArcadeVisual.Median)) {
            Text("Trials: $experimentCount  •  Highlighted: $successes", color = GameInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            PrimaryGameButton(
                when (game.visual) {
                    ArcadeVisual.Spinner -> "Spin"
                    ArcadeVisual.Coin -> "Flip Coin"
                    ArcadeVisual.Sampling -> "Draw Sample"
                    else -> "Sort Data"
                },
                game.accent,
                { onExperiment((experimentCount + game.id.length) % 2 == 0) },
            )
        } else {
            Text("Interactive mathematical model", color = GameMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ArcadeAnswerPanel(game: ArcadeMiniGame, challenge: ArcadeChallenge, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(22.dp)).border(1.dp, game.accent.copy(.65f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SELECT YOUR RESULT", color = GameMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Box(
            Modifier.fillMaxWidth().height(if (compact) 54.dp else 66.dp).background(Color.White.copy(.09f), RoundedCornerShape(15.dp)).border(1.dp, game.accent.copy(.6f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(selected.ifBlank { "?" }, color = if (selected.isBlank()) GameMuted else game.accent, fontSize = if (selected.length > 12) 16.sp else 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            challenge.choices.forEachIndexed { index, choice ->
                DraggableGameTile(choice, listOf(game.accent, GamePurple, GameGreen, GameGold)[index % 4], "answer $choice") { onSelect(choice) }
            }
        }
        SecondaryGameButton("Clear", game.accent) { onSelect("") }
    }
}

@Composable
private fun ArcadeIcon(visual: ArcadeVisual, accent: Color, modifier: Modifier = Modifier, values: List<Double> = emptyList()) {
    Canvas(modifier.padding(7.dp)) {
        when (visual) {
            ArcadeVisual.Bag -> {
                drawRoundRect(GamePurple.copy(.75f), Offset(size.width * .25f, size.height * .28f), Size(size.width * .5f, size.height * .55f), androidx.compose.ui.geometry.CornerRadius(30f))
                listOf(GameRed, GameBlue, GameGreen, GameGold, Color(0xFFE85F9B)).forEachIndexed { i, color ->
                    drawCircle(color, size.minDimension * .07f, Offset(size.width * (.34f + (i % 3) * .16f), size.height * (.52f + (i / 3) * .19f)))
                }
            }
            ArcadeVisual.Spinner -> {
                val radius = size.minDimension * .38f
                val center = Offset(size.width / 2f, size.height / 2f)
                val colors = listOf(GameRed, GameGold, GameGreen, GameBlue, GamePurple, Color(0xFF36C7C2))
                colors.forEachIndexed { index, color -> drawArc(color, index * 60f, 60f, true, center - Offset(radius, radius), Size(radius * 2, radius * 2)) }
                drawCircle(GameSpace, radius * .18f, center)
                drawLine(Color.White, center, Offset(center.x + radius * .72f, center.y), 7f, StrokeCap.Round)
            }
            ArcadeVisual.Coin -> {
                drawCircle(GameGold, size.minDimension * .35f, Offset(size.width / 2f, size.height / 2f))
                drawCircle(Color(0xFFFFE89A), size.minDimension * .25f, Offset(size.width / 2f, size.height / 2f), style = Stroke(8f))
                drawContext.canvas.nativeCanvas.drawText("H/T", size.width / 2f, size.height * .58f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(104,68,0); textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
            }
            ArcadeVisual.Cards -> {
                repeat(4) { index ->
                    val x = size.width * (.2f + index * .15f)
                    drawRoundRect(Color.White, Offset(x, size.height * .2f + index * 3f), Size(size.width * .24f, size.height * .62f), androidx.compose.ui.geometry.CornerRadius(10f))
                    drawContext.canvas.nativeCanvas.drawText(listOf("A", "K", "Q", "J")[index], x + size.width * .12f, size.height * .55f, android.graphics.Paint().apply { color = if (index % 2 == 0) android.graphics.Color.RED else android.graphics.Color.BLACK; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
                }
            }
            ArcadeVisual.Tree -> {
                val root = Offset(size.width / 2f, size.height * .18f)
                val middle = listOf(Offset(size.width * .33f, size.height * .48f), Offset(size.width * .67f, size.height * .48f))
                val leaves = listOf(Offset(size.width * .2f, size.height * .8f), Offset(size.width * .42f, size.height * .8f), Offset(size.width * .58f, size.height * .8f), Offset(size.width * .8f, size.height * .8f))
                middle.forEach { drawLine(Color.White, root, it, 6f) }
                leaves.forEachIndexed { index, leaf -> drawLine(Color.White, middle[index / 2], leaf, 5f) }
                drawCircle(GameGreen, 18f, root); middle.forEach { drawCircle(GameBlue, 17f, it) }; leaves.forEachIndexed { i, p -> drawCircle(if (i % 2 == 0) GameGold else GameRed, 15f, p) }
            }
            ArcadeVisual.Permutation -> drawLetterTiles(listOf("A", "B", "C"), accent)
            ArcadeVisual.Combination -> {
                listOf(GameRed, GameBlue, GameGreen, GameGold, GamePurple).forEachIndexed { index, color ->
                    drawCircle(color, size.minDimension * .1f, Offset(size.width * (.22f + index * .14f), size.height * (.55f + (index % 2) * .13f)))
                }
            }
            ArcadeVisual.Binomial, ArcadeVisual.MixedStatistics -> drawBars(accent, values.ifEmpty { listOf(1.0, 2.5, 4.0, 5.0, 3.5, 2.0) })
            ArcadeVisual.Normal -> drawNormalCurve(accent)
            ArcadeVisual.ZScore -> {
                drawLine(Color.White, Offset(size.width * .1f, size.height * .7f), Offset(size.width * .9f, size.height * .7f), 5f)
                repeat(7) { i -> val x = size.width * (.2f + i * .1f); drawLine(Color.White, Offset(x, size.height * .65f), Offset(x, size.height * .75f), 3f) }
                val needleX = size.width * .65f
                drawLine(GameGold, Offset(needleX, size.height * .25f), Offset(needleX, size.height * .7f), 8f, StrokeCap.Round)
                drawCircle(GameRed, 13f, Offset(needleX, size.height * .25f))
            }
            ArcadeVisual.Sampling -> drawLetterTiles(listOf("A", "B", "C", "?"), accent)
            ArcadeVisual.Mean -> drawNumberTiles(values.ifEmpty { listOf(4.0, 7.0, 2.0, 9.0, 6.0) }, accent)
            ArcadeVisual.Median -> drawNumberTiles((values.ifEmpty { listOf(8.0, 3.0, 9.0, 1.0, 5.0) }), accent)
            ArcadeVisual.StandardDeviation -> {
                repeat(18) { i ->
                    val x = size.width * (.1f + (i % 9) * .1f)
                    val y = size.height * (.75f - ((i * 7) % 11) * .05f)
                    drawCircle(if (i % 3 == 0) GameGold else accent, 7f, Offset(x, y))
                }
                drawLine(Color.White, Offset(size.width * .1f, size.height * .78f), Offset(size.width * .9f, size.height * .25f), 4f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLetterTiles(labels: List<String>, accent: Color) {
    labels.forEachIndexed { index, label ->
        val w = size.width / (labels.size + 1)
        val x = w * (index + .5f)
        drawRoundRect(if (index % 2 == 0) accent else GameGreen, Offset(x, size.height * .35f), Size(w * .8f, size.height * .4f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawContext.canvas.nativeCanvas.drawText(label, x + w * .4f, size.height * .62f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 32f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNumberTiles(values: List<Double>, accent: Color) {
    values.take(7).forEachIndexed { index, value ->
        val w = size.width / (values.take(7).size + 1)
        val x = w * (index + .5f)
        drawRoundRect(accent.copy(if (index % 2 == 0) .9f else .6f), Offset(x, size.height * .4f), Size(w * .8f, size.height * .35f), androidx.compose.ui.geometry.CornerRadius(7f))
        drawContext.canvas.nativeCanvas.drawText(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString(), x + w * .4f, size.height * .63f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 26f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(accent: Color, values: List<Double>) {
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val barWidth = size.width * .72f / values.size
    values.forEachIndexed { index, value ->
        val h = size.height * .62f * (value / max).toFloat()
        drawRoundRect(listOf(accent, GameGreen, GameGold, GameBlue)[index % 4], Offset(size.width * .14f + index * barWidth, size.height * .82f - h), Size(barWidth * .72f, h), androidx.compose.ui.geometry.CornerRadius(5f))
    }
    drawLine(Color.White, Offset(size.width * .1f, size.height * .82f), Offset(size.width * .9f, size.height * .82f), 4f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNormalCurve(accent: Color) {
    val path = Path()
    repeat(101) { i ->
        val xValue = (i - 50) / 14.0
        val density = exp(-.5 * xValue * xValue)
        val x = size.width * (.08f + i / 100f * .84f)
        val y = size.height * (.82f - density.toFloat() * .68f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, accent, style = Stroke(7f, cap = StrokeCap.Round))
    drawLine(Color.White, Offset(size.width * .08f, size.height * .82f), Offset(size.width * .92f, size.height * .82f), 4f)
    drawLine(GameGold, Offset(size.width / 2f, size.height * .18f), Offset(size.width / 2f, size.height * .82f), 3f)
}
