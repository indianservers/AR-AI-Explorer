package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private enum class AlgebraRoute { Intro, Chapters, Levels, Play }
internal enum class AlgebraVisualKind { Balance, Tiles, Inequality, System, Quadratic }

internal data class AlgebraChallenge(
    val id: String,
    val chapter: Int,
    val stage: Int,
    val title: String,
    val prompt: String,
    val choices: List<String>,
    val answer: String,
    val hint: String,
    val steps: List<String>,
    val visual: AlgebraVisualKind,
)

internal object AlgebraAdventureCatalog {
    val chapterNames = listOf(
        "The Basics",
        "Linear Equations",
        "Expressions & Terms",
        "Advanced Linear",
        "Systems of Equations",
        "Quadratic Equations",
    )

    val chapterDescriptions = listOf(
        "Variables and simple equations",
        "One-step to multi-step balance",
        "Simplify, combine and distribute",
        "Fractions, decimals and inequalities",
        "Solve two variables together",
        "Roots, factors and parabolas",
    )

    val challenges: List<AlgebraChallenge> = buildList {
        repeat(12) { index ->
            val root = index + 2
            when (index % 3) {
                0 -> {
                    val coefficient = 2 + index % 4
                    add(challenge(0, index, "Multiply to Unlock", "${coefficient}x = ${coefficient * root}", root.toString(),
                        "Divide both sides by $coefficient.",
                        listOf("Start with ${coefficient}x = ${coefficient * root}.", "Divide both sides by $coefficient.", "x = $root"), AlgebraVisualKind.Balance))
                }
                1 -> {
                    val addend = 3 + index % 5
                    add(challenge(0, index, "Addition Lock", "x + $addend = ${root + addend}", root.toString(),
                        "Subtract $addend from both sides.",
                        listOf("Start with x + $addend = ${root + addend}.", "Subtract $addend from both sides.", "x = $root"), AlgebraVisualKind.Balance))
                }
                else -> {
                    val subtract = 2 + index % 4
                    add(challenge(0, index, "Subtraction Lock", "x − $subtract = ${root - subtract}", root.toString(),
                        "Add $subtract to both sides.",
                        listOf("Start with x − $subtract = ${root - subtract}.", "Add $subtract to both sides.", "x = $root"), AlgebraVisualKind.Balance))
                }
            }
        }
        repeat(12) { index ->
            val root = index + 3
            val leftCoefficient = 2 + index % 4
            val rightCoefficient = 1 + index % 2
            val leftConstant = index % 5 - 2
            val rightConstant = (leftCoefficient - rightCoefficient) * root + leftConstant
            add(challenge(1, index, "Variables on Both Sides",
                "${leftCoefficient}x ${signed(leftConstant)} = ${rightCoefficient}x ${signed(rightConstant)}",
                root.toString(),
                "Move variable terms to one side and constants to the other.",
                listOf(
                    "Subtract ${rightCoefficient}x from both sides.",
                    "${leftCoefficient - rightCoefficient}x ${signed(leftConstant)} = $rightConstant",
                    "Move the constant and divide: x = $root",
                ),
                AlgebraVisualKind.Balance))
        }
        repeat(12) { index ->
            when {
                index < 4 -> {
                    val a = index + 2
                    val b = index + 4
                    val answer = "${a + b}x"
                    add(textChallenge(2, index, "Combine Like Terms", "${a}x + ${b}x", answer,
                        listOf("${a + b}x", "${a * b}x", "${a + b}x²", "${b - a}x"),
                        "Both terms contain x, so add their coefficients.",
                        listOf("${a}x + ${b}x", "= (${a} + ${b})x", "= $answer"), AlgebraVisualKind.Tiles))
                }
                index < 8 -> {
                    val k = index - 1
                    val a = index - 2
                    val answer = "${k}x + ${k * a}"
                    add(textChallenge(2, index, "Distributive Property", "$k(x + $a)", answer,
                        listOf(answer, "${k}x + $a", "${k + a}x", "${k * a}x"),
                        "Multiply every term inside the bracket by $k.",
                        listOf("$k(x + $a)", "= ${k}·x + ${k}·$a", "= $answer"), AlgebraVisualKind.Tiles))
                }
                else -> {
                    val a = index - 6
                    val b = index - 5
                    val sum = a + b
                    val product = a * b
                    val answer = "x² + ${sum}x + $product"
                    add(textChallenge(2, index, "Multiply Binomials", "(x + $a)(x + $b)", answer,
                        listOf(answer, "x² + ${product}x + $sum", "x² + ${sum}x + ${a + b}", "2x + $product"),
                        "Multiply every term in the first bracket by every term in the second.",
                        listOf("(x + $a)(x + $b)", "= x² + ${b}x + ${a}x + $product", "= $answer"), AlgebraVisualKind.Tiles))
                }
            }
        }
        repeat(12) { index ->
            when {
                index < 4 -> {
                    val denominator = index + 2
                    val root = denominator * (index + 2)
                    val addend = index + 1
                    val total = root / denominator + addend
                    add(challenge(3, index, "Fraction Equation", "x/$denominator + $addend = $total", root.toString(),
                        "Remove the constant, then multiply by $denominator.",
                        listOf("Subtract $addend: x/$denominator = ${total - addend}.", "Multiply by $denominator.", "x = $root"), AlgebraVisualKind.Balance))
                }
                index < 8 -> {
                    val root = (index - 2) * 2
                    val addend = index - 4
                    val total = root / 2.0 + addend
                    add(challenge(3, index, "Decimal Equation", "0.5x + $addend = ${format(total)}", root.toString(),
                        "Subtract the constant, then divide by 0.5.",
                        listOf("Subtract $addend from both sides.", "0.5x = ${format(total - addend)}", "x = $root"), AlgebraVisualKind.Balance))
                }
                else -> {
                    val boundary = index + 1
                    val shift = index - 6
                    val right = boundary + shift
                    val symbol = if (index % 2 == 0) "<" else ">"
                    val answer = "x $symbol $boundary"
                    add(textChallenge(3, index, "Inequality Gate", "x + $shift $symbol $right", answer,
                        listOf(answer, "x ${if (symbol == "<") ">" else "<"} $boundary", "x $symbol $right", "x = $boundary"),
                        "Undo the addition without changing the inequality direction.",
                        listOf("Start with x + $shift $symbol $right.", "Subtract $shift from both sides.", answer), AlgebraVisualKind.Inequality))
                }
            }
        }
        repeat(12) { index ->
            val x = index + 1
            val y = index % 5 + 2
            val sum = x + y
            val difference = x - y
            val answer = "($x, $y)"
            add(textChallenge(4, index, "Two-Variable Vault", "x + y = $sum;  x − y = $difference", answer,
                listOf(answer, "($y, $x)", "($sum, $difference)", "(${x + 1}, ${y - 1})"),
                "Add the equations to eliminate y, then substitute.",
                listOf("Add both equations: 2x = ${2 * x}.", "Divide by 2: x = $x.", "Substitute into x + y = $sum: y = $y."), AlgebraVisualKind.System))
        }
        repeat(12) { index ->
            val firstRoot = index % 5 + 1
            val secondRoot = firstRoot + 2 + index % 3
            val sum = firstRoot + secondRoot
            val product = firstRoot * secondRoot
            val answer = "x = $firstRoot or $secondRoot"
            add(textChallenge(5, index, "Quadratic Vault", "x² − ${sum}x + $product = 0", answer,
                listOf(answer, "x = ${-firstRoot} or ${-secondRoot}", "x = $sum or $product", "x = ${firstRoot + 1} or ${secondRoot - 1}"),
                "Find two numbers that add to $sum and multiply to $product.",
                listOf("Factor: (x − $firstRoot)(x − $secondRoot) = 0.", "Set each factor equal to zero.", answer), AlgebraVisualKind.Quadratic))
        }
    }

    private fun challenge(chapter: Int, stage: Int, title: String, prompt: String, answer: String, hint: String, steps: List<String>, visual: AlgebraVisualKind): AlgebraChallenge {
        val numeric = answer.toInt()
        val choices = listOf(numeric - 2, numeric - 1, numeric, numeric + 2).distinct().sorted().map(Int::toString)
        return textChallenge(chapter, stage, title, prompt, answer, choices, hint, steps, visual)
    }

    private fun textChallenge(chapter: Int, stage: Int, title: String, prompt: String, answer: String, choices: List<String>, hint: String, steps: List<String>, visual: AlgebraVisualKind) =
        AlgebraChallenge("algebra-${chapter + 1}-${stage + 1}", chapter, stage, title, prompt, choices.distinct(), answer, hint, steps, visual)

    private fun signed(value: Int): String = if (value >= 0) "+ $value" else "− ${abs(value)}"
    private fun format(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

private val AlgebraChapters = AlgebraAdventureCatalog.chapterNames.mapIndexed { index, title ->
    GameLevel(title, AlgebraAdventureCatalog.chapterDescriptions[index], listOf(GameGreen, GameBlue, GamePurple, GameGold, Color(0xFFE85E79), Color(0xFF8F65E8))[index])
}

@Composable
internal fun AlgebraAdventureGame(completed: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    val catalog = AlgebraAdventureCatalog.challenges
    var routeName by rememberSaveable { mutableStateOf(AlgebraRoute.Intro.name) }
    var selectedChapter by rememberSaveable { mutableIntStateOf((completed / 12).coerceAtMost(AlgebraChapters.lastIndex)) }
    var challengeIndex by rememberSaveable { mutableIntStateOf(completed.coerceIn(0, catalog.lastIndex)) }
    when (AlgebraRoute.valueOf(routeName)) {
        AlgebraRoute.Intro -> GameIntroScreen(
            number = 4,
            title = "Balance Vault",
            subtitle = "Algebra Adventure • Keep both sides balanced.",
            accent = GamePurple,
            concepts = listOf("x" to "Variables", "=" to "Equations", "ax+b" to "Expressions", "x²" to "Quadratics"),
            completed = completed,
            total = catalog.size,
            onBack = onBack,
            onStart = { challengeIndex = completed.coerceIn(0, catalog.lastIndex); routeName = AlgebraRoute.Play.name },
            onMap = { routeName = AlgebraRoute.Chapters.name },
        )
        AlgebraRoute.Chapters -> LevelMapScreen(
            "Algebra Adventure • Chapters",
            AlgebraChapters,
            (completed / 12).coerceAtMost(AlgebraChapters.lastIndex),
            GamePurple,
            { routeName = AlgebraRoute.Intro.name },
        ) { chapter -> selectedChapter = chapter; routeName = AlgebraRoute.Levels.name }
        AlgebraRoute.Levels -> {
            val start = selectedChapter * 12
            val levels = catalog.subList(start, start + 12).map { GameLevel("Level ${it.stage + 1}", it.title, AlgebraChapters[selectedChapter].accent) }
            val unlocked = when {
                completed >= start + 12 -> 11
                completed >= start -> (completed - start).coerceIn(0, 11)
                else -> 0
            }
            LevelMapScreen(
                "${AlgebraChapters[selectedChapter].title} • 12 Levels",
                levels,
                unlocked,
                AlgebraChapters[selectedChapter].accent,
                { routeName = AlgebraRoute.Chapters.name },
            ) { stage -> challengeIndex = start + stage; routeName = AlgebraRoute.Play.name }
        }
        AlgebraRoute.Play -> AlgebraChallengeScreen(
            challenge = catalog[challengeIndex],
            globalLevel = challengeIndex,
            onBack = { selectedChapter = catalog[challengeIndex].chapter; routeName = AlgebraRoute.Levels.name },
            onSolved = {
                onComplete(challengeIndex + 1)
                if (challengeIndex < catalog.lastIndex) challengeIndex++ else routeName = AlgebraRoute.Chapters.name
            },
        )
    }
}

@Composable
private fun AlgebraChallengeScreen(challenge: AlgebraChallenge, globalLevel: Int, onBack: () -> Unit, onSolved: () -> Unit) {
    var selected by rememberSaveable(challenge.id) { mutableStateOf("") }
    var result by rememberSaveable(challenge.id) { mutableStateOf<Boolean?>(null) }
    var hint by rememberSaveable(challenge.id) { mutableStateOf(false) }
    var showSteps by rememberSaveable(challenge.id) { mutableStateOf(false) }
    val accent = AlgebraChapters[challenge.chapter].accent
    GameScreen("${challenge.title} • ${challenge.stage + 1}/12", globalLevel + 1, accent, if (result == false) 2 else 3, onBack, { hint = !hint }) {
        if (hint) GlossyPanel(GameGold) { Text(challenge.hint, color = GameInk) }
        GlossyPanel(accent) {
            Text("SOLVE THE VAULT", color = GameGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text(challenge.prompt, color = GameInk, fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlgebraVisual(challenge, accent, Modifier.weight(1.15f))
                    AlgebraAnswerPanel(challenge, selected, accent, Modifier.weight(.85f)) { selected = it; result = null }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlgebraVisual(challenge, accent)
                    AlgebraAnswerPanel(challenge, selected, accent) { selected = it; result = null }
                }
            }
        }
        PrimaryGameButton("Check Equation", GameGreen, { result = selected == challenge.answer }, enabled = selected.isNotBlank())
        result?.let { correct ->
            ResultPanel(correct, "Vault unlocked. ${challenge.steps.last()}", "The balance is not equal yet. Use the hint or move terms again.", if (correct) { { showSteps = true } } else null)
        }
        if (showSteps) {
            GlossyPanel(GameGreen) {
                Text("WHY IT WORKS", color = GameGreen, fontWeight = FontWeight.Black)
                challenge.steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", color = GameInk, fontSize = 12.sp)
                }
                PrimaryGameButton("Next Level", GameGreen, onSolved)
                SecondaryGameButton("Review This Level", accent) { showSteps = false; selected = ""; result = null }
            }
        }
    }
}

@Composable
private fun AlgebraAnswerPanel(challenge: AlgebraChallenge, selected: String, accent: Color, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Column(
        modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(22.dp)).border(1.dp, accent.copy(.65f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("CHOOSE THE BALANCED RESULT", color = GameMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Box(
            Modifier.fillMaxWidth().height(68.dp).background(Color.White.copy(.1f), RoundedCornerShape(15.dp)).border(1.dp, accent.copy(.6f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(selected.ifBlank { "?" }, color = if (selected.isBlank()) GameMuted else GameGold, fontSize = if (selected.length > 15) 16.sp else 25.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            challenge.choices.forEachIndexed { index, choice ->
                DraggableGameTile(choice, listOf(GamePurple, GameGreen, GameGold, GameBlue)[index % 4], "answer $choice") { onSelect(choice) }
            }
        }
        SecondaryGameButton("Clear Selection", accent) { onSelect("") }
    }
}

@Composable
private fun AlgebraVisual(challenge: AlgebraChallenge, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 300.dp).background(
            Brush.radialGradient(listOf(accent.copy(.22f), Color(0xFF16305D), GamePanel)),
            RoundedCornerShape(24.dp),
        ).border(1.dp, accent.copy(.7f), RoundedCornerShape(24.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            when (challenge.visual) {
                AlgebraVisualKind.Balance -> "KEEP BOTH SIDES BALANCED"
                AlgebraVisualKind.Tiles -> "BUILD THE EXPRESSION"
                AlgebraVisualKind.Inequality -> "OPEN THE INEQUALITY GATE"
                AlgebraVisualKind.System -> "FIND THE INTERSECTION"
                AlgebraVisualKind.Quadratic -> "FIND BOTH ROOTS"
            },
            color = GameGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp,
        )
        Canvas(Modifier.fillMaxWidth().height(235.dp)) {
            when (challenge.visual) {
                AlgebraVisualKind.Balance -> drawAlgebraBalance(accent)
                AlgebraVisualKind.Tiles -> drawAlgebraTiles(accent)
                AlgebraVisualKind.Inequality -> drawInequalityLine(accent)
                AlgebraVisualKind.System -> drawSystemGraph(accent)
                AlgebraVisualKind.Quadratic -> drawQuadraticGraph(accent)
            }
        }
        Text(challenge.prompt, color = GameInk, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAlgebraBalance(accent: Color) {
    val center = Offset(size.width / 2f, size.height * .48f)
    drawLine(Color(0xFFC8D8F0), Offset(center.x, center.y), Offset(center.x, size.height * .86f), 12f)
    drawLine(Color(0xFF748DB7), Offset(center.x - 70f, size.height * .87f), Offset(center.x + 70f, size.height * .87f), 13f)
    drawLine(GameGold, Offset(size.width * .16f, center.y), Offset(size.width * .84f, center.y), 10f, StrokeCap.Round)
    drawCircle(Color.White, 10f, center)
    listOf(size.width * .24f, size.width * .34f, size.width * .66f, size.width * .76f).forEachIndexed { index, x ->
        drawRoundRect(if (index < 2) GamePurple else GameGreen, Offset(x - 27f, center.y - 52f), Size(54f, 42f), androidx.compose.ui.geometry.CornerRadius(7f))
    }
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
        drawText("x", size.width * .24f, center.y - 22f, paint)
        drawText("+", size.width * .34f, center.y - 22f, paint)
        drawText("x", size.width * .66f, center.y - 22f, paint)
        drawText("+", size.width * .76f, center.y - 22f, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAlgebraTiles(accent: Color) {
    val origin = Offset(size.width * .16f, size.height * .2f)
    drawRoundRect(accent.copy(.85f), origin, Size(size.width * .42f, size.height * .48f), androidx.compose.ui.geometry.CornerRadius(10f))
    repeat(4) { index ->
        drawRoundRect(GameBlue.copy(.85f), Offset(origin.x + index * size.width * .15f, origin.y + size.height * .53f), Size(size.width * .12f, size.height * .13f), androidx.compose.ui.geometry.CornerRadius(6f))
    }
    drawContext.canvas.nativeCanvas.drawText("x² / xy", origin.x + size.width * .21f, origin.y + size.height * .28f, android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 32f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
    })
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInequalityLine(accent: Color) {
    val y = size.height / 2f
    drawLine(Color.White, Offset(size.width * .1f, y), Offset(size.width * .9f, y), 6f, StrokeCap.Round)
    repeat(9) { index ->
        val x = size.width * (.1f + index * .1f)
        drawLine(Color.White, Offset(x, y - 12f), Offset(x, y + 12f), 3f)
    }
    val boundary = size.width * .55f
    drawCircle(Color.White, 15f, Offset(boundary, y), style = Stroke(5f))
    drawLine(accent, Offset(size.width * .12f, y), Offset(boundary - 17f, y), 12f, StrokeCap.Round)
    drawPath(Path().apply { moveTo(size.width * .1f, y); lineTo(size.width * .17f, y - 22f); lineTo(size.width * .17f, y + 22f); close() }, accent)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSystemGraph(accent: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawLine(Color.White, Offset(25f, center.y), Offset(size.width - 25f, center.y), 4f)
    drawLine(Color.White, Offset(center.x, 15f), Offset(center.x, size.height - 15f), 4f)
    drawLine(GameBlue, Offset(size.width * .14f, size.height * .78f), Offset(size.width * .86f, size.height * .2f), 7f, StrokeCap.Round)
    drawLine(GameGold, Offset(size.width * .14f, size.height * .22f), Offset(size.width * .86f, size.height * .72f), 7f, StrokeCap.Round)
    drawCircle(accent, 13f, center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuadraticGraph(accent: Color) {
    val center = Offset(size.width / 2f, size.height * .72f)
    drawLine(Color.White, Offset(25f, center.y), Offset(size.width - 25f, center.y), 4f)
    drawLine(Color.White, Offset(center.x, 15f), Offset(center.x, size.height - 15f), 4f)
    val path = Path()
    repeat(81) { index ->
        val t = (index - 40) / 40f
        val x = center.x + t * size.width * .36f
        val y = center.y - t * t * size.height * .58f
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, accent, style = Stroke(8f, cap = StrokeCap.Round))
    drawCircle(GameGold, 11f, Offset(center.x - size.width * .18f, center.y))
    drawCircle(GameGold, 11f, Offset(center.x + size.width * .18f, center.y))
}
