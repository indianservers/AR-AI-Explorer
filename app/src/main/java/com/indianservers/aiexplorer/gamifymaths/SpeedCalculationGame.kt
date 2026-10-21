package com.indianservers.aiexplorer.gamifymaths

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.displayLatexFormula
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

internal enum class SpeedCalculationMode { Basic, Advanced }

private enum class SpeedScreen { Settings, Playing, Results }
internal enum class SpeedAnswerMode(val label: String) {
    Typed("Type answer"),
    MultipleChoice("4 choices"),
}

internal data class SpeedProblem(
    val prompt: String,
    val answer: String,
    val topic: String,
    val solution: String = "",
    val templateId: String = "",
    val solutionTokens: List<String> = emptyList(),
)

private val DurationChoices = listOf(30, 45, 60, 120, 180, 300)
private val BasicOperations = listOf("Addition", "Subtraction", "Multiplication", "Division", "Percent")
private val AdvancedTopics = listOf("Advanced Calculation", "Trigonometry", "Algebra")

@Composable
internal fun SpeedCalculationGame(
    mode: SpeedCalculationMode,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit,
) {
    val accent = if (mode == SpeedCalculationMode.Basic) GameBlue else GamePurple
    val context=LocalContext.current
    val preferences=remember{context.getSharedPreferences("maths_games",0)}
    val title = if (mode == SpeedCalculationMode.Basic) "Calculation Sprint" else "Advanced Calculation Sprint"
    var screenName by rememberSaveable { mutableStateOf(SpeedScreen.Settings.name) }
    var duration by rememberSaveable { mutableIntStateOf(45) }
    var mastery by rememberSaveable { mutableIntStateOf(0) }
    var digits by rememberSaveable { mutableIntStateOf(1) }
    var selectedBasic by rememberSaveable { mutableStateOf(BasicOperations.toSet()) }
    var selectedAdvanced by rememberSaveable { mutableStateOf(AdvancedTopics.toSet()) }
    var answerModeName by rememberSaveable { mutableStateOf(SpeedAnswerMode.MultipleChoice.name) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(duration) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var attempted by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by rememberSaveable { mutableIntStateOf(0) }
    var answerText by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    val usedPrompts: MutableSet<String> = rememberSaveable(saver = listSaver<MutableSet<String>, String>(save = { it.toList() }, restore = { it.toMutableSet() })) {
        LinkedHashSet(preferences.getStringSet("recent_speed_questions_$mode",emptySet()).orEmpty())
    }
    var problem by rememberSaveable(stateSaver = listSaver<SpeedProblem, String>(save = { listOf(it.prompt, it.answer, it.topic,it.solution,it.templateId,it.solutionTokens.joinToString("|")) }, restore = { SpeedProblem(it[0], it[1], it[2],it.getOrElse(3){""},it.getOrElse(4){""},it.getOrElse(5){""}.split('|').filter{token->token.isNotEmpty()}) })) {
        mutableStateOf(nextUniqueSpeedProblem(mode, digits, selectedBasic, selectedAdvanced, usedPrompts, mastery = mastery))
    }
    val screen = SpeedScreen.valueOf(screenName)
    val answerMode = SpeedAnswerMode.valueOf(answerModeName)
    LaunchedEffect(screenName,problem.prompt) {
        if(screen==SpeedScreen.Playing) preferences.edit().putStringSet("recent_speed_questions_$mode",usedPrompts.toList().takeLast(RecentMathQuestionCount).toSet()).apply()
    }

    fun startRound() {
        secondsLeft = duration
        correct = 0
        attempted = 0
        streak = 0
        bestStreak = 0
        answerText = ""
        feedback = null
        problem = nextUniqueSpeedProblem(mode, digits, selectedBasic, selectedAdvanced, usedPrompts, mastery = mastery)
        screenName = SpeedScreen.Playing.name
    }

    LaunchedEffect(screenName, secondsLeft) {
        if (screen == SpeedScreen.Playing) {
            if (secondsLeft <= 0) {
                onComplete(correct)
                screenName = SpeedScreen.Results.name
            } else {
                delay(1_000)
                secondsLeft--
            }
        }
    }

    when (screen) {
        SpeedScreen.Settings -> SpeedSettingsScreen(
            title = title,
            mode = mode,
            accent = accent,
            duration = duration,
            digits = digits,
            selectedBasic = selectedBasic,
            selectedAdvanced = selectedAdvanced,
            answerMode = answerMode,
            onDuration = { duration = it },
            onDigits = { digits = it },
            onAnswerMode = { answerModeName = it.name },
            onToggleBasic = { item ->
                selectedBasic = toggleChoice(selectedBasic, item)
            },
            onToggleAdvanced = { item ->
                selectedAdvanced = toggleChoice(selectedAdvanced, item)
            },
            onBack = onBack,
            onStart = ::startRound,
            mastery = mastery,
            onMastery = { mastery = it },
        )
        SpeedScreen.Playing -> {
            fun submit(candidate: String = answerText) {
                if (candidate.isBlank()) return
                attempted++
                if (answersMatch(candidate, problem.answer)) {
                    correct++
                    streak++
                    bestStreak = maxOf(bestStreak, streak)
                    feedback = "Correct — method confirmed. +1"
                } else {
                    streak = 0
                    val guidance = GameLearningCoach.guidance(attempted, problem.prompt)
                    feedback = "Answer: ${problem.answer}. ${guidance.hint}"
                }
                answerText = ""
                problem = nextUniqueSpeedProblem(mode, digits, selectedBasic, selectedAdvanced, usedPrompts, mastery = mastery)
            }
            SpeedPlayScreen(
                title = title,
                accent = accent,
                secondsLeft = secondsLeft,
                duration = duration,
                problem = problem,
                answer = answerText,
                correct = correct,
                attempted = attempted,
                streak = streak,
                feedback = feedback,
                answerMode = answerMode,
                onAnswer = { answerText = it.take(18) },
                onSubmit = ::submit,
                onSubmitChoice = { choice ->
                    answerText = choice
                    submit(choice)
                },
                onSettings = { screenName = SpeedScreen.Settings.name },
                onBack = onBack,
                onWrongAttempt = { attempted++; streak = 0; feedback = "Correct answer: ${problem.answer}. Review the worked solution and retry." },
            )
        }
        SpeedScreen.Results -> SpeedResultsScreen(
            title = title,
            accent = accent,
            duration = duration,
            correct = correct,
            attempted = attempted,
            bestStreak = bestStreak,
            onAgain = ::startRound,
            onSettings = { screenName = SpeedScreen.Settings.name },
            onBack = onBack,
        )
    }
}

@Composable
private fun SpeedSettingsScreen(
    title: String,
    mode: SpeedCalculationMode,
    accent: Color,
    duration: Int,
    digits: Int,
    selectedBasic: Set<String>,
    selectedAdvanced: Set<String>,
    answerMode: SpeedAnswerMode,
    onDuration: (Int) -> Unit,
    onDigits: (Int) -> Unit,
    onAnswerMode: (SpeedAnswerMode) -> Unit,
    onToggleBasic: (String) -> Unit,
    onToggleAdvanced: (String) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    mastery: Int,
    onMastery: (Int) -> Unit,
) {
    CompactSettingsPage(title, accent, "${LocalPreviousGameBest.current} solved", onBack) {
        SettingsCard("Time Limit", "Choose duration", accent) {
            SettingsChoices(DurationChoices.map(::formatDuration), { DurationChoices[it] == duration }, accent,
                { onDuration(DurationChoices[it]) })
        }
        if (mastery == 0) {
            if (mode == SpeedCalculationMode.Basic) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val size: @Composable (Modifier) -> Unit = { modifier ->
                        SettingsCard("Number Size", "Operand / quotient range", accent, modifier) {
                            SettingsChoices(listOf("Single", "Double", "Triple"), { it + 1 == digits }, accent, { onDigits(it + 1) })
                        }
                    }
                    val operations: @Composable (Modifier) -> Unit = { modifier ->
                        SettingsCard("Operations", "Tap to include or exclude", accent, modifier) {
                            SettingsChoices(listOf("+", "−", "×", "÷", "%"), { BasicOperations[it] in selectedBasic }, accent,
                                { onToggleBasic(BasicOperations[it]) })
                        }
                    }
                    if (maxWidth >= 320.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        size(Modifier.weight(1f)); operations(Modifier.weight(1f))
                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        size(Modifier); operations(Modifier)
                    }
                }
            } else {
                SettingsCard("Challenge Topics", "Tap to include or exclude", accent) {
                    SettingsChoices(listOf("Calculation", "Trigonometry", "Algebra"), { AdvancedTopics[it] in selectedAdvanced }, accent,
                        { onToggleAdvanced(AdvancedTopics[it]) })
                }
            }
        }
        SettingsCard("Reactor Level", if (mode == SpeedCalculationMode.Basic)
            when (mastery) { 1 -> "Grouped arithmetic with generated operands."; 2 -> "Multi-step arithmetic with division."; else -> "Use your selected number size and operations." }
            else when (mastery) { 1 -> "Construct a derivative at a generated input."; 2 -> "Solve a generated two-equation system."; else -> "Use your selected calculation topics." }, accent) {
            SettingsChoices(listOf("Standard", "Mastery 1", "Mastery 2"), { it == mastery }, accent, onMastery)
        }
        val hasSelection = mastery > 0 || if (mode == SpeedCalculationMode.Basic) selectedBasic.isNotEmpty() else selectedAdvanced.isNotEmpty()
        PrimaryGameButton("Start ${formatDuration(duration)} Sprint", accent, onStart, enabled = hasSelection)
        Text("Build and check calculations before time runs out.", color = GameMuted, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun SpeedPlayScreen(
    title: String,
    accent: Color,
    secondsLeft: Int,
    duration: Int,
    problem: SpeedProblem,
    answer: String,
    correct: Int,
    attempted: Int,
    streak: Int,
    feedback: String?,
    answerMode: SpeedAnswerMode,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
    onSubmitChoice: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    onWrongAttempt: () -> Unit = {},
) {
    val source = GeneratedRound("Timed reactor", displayLatexFormula(problem.prompt), problem.answer.toDouble(),
        problem.solution.ifBlank{"The correct result is ${problem.answer}. Evaluate the displayed calculation using the order of operations."}, "Assemble a mathematical method before checking it.", emptyList(),
        operands = Regex("[0-9]+(?:\\.[0-9]+)?").findAll(problem.prompt).map { it.value.toDouble() }.toList())
    val mission = WorkshopChallenge(WorkshopKind.Expression, "Timed reactor", source.prompt, source.hint, source.explanation,
        source, numbers = (source.operands + listOf(0.0, 1.0, 2.0, 100.0)).distinct(),witness=problem.solutionTokens,templateId=problem.templateId)
    val game = gamifyGamesForAudit().first { it.id == if (title.contains("Advanced", true) || title == "speed-advanced") "speed-advanced" else "speed-basic" }
    LightMathFrame(game, null, formatClock(secondsLeft), correct, streak,
        secondsLeft / duration.toFloat().coerceAtLeast(1f), "Construct an expression", mission.playerPrompt(), onBack, onSettings) {
        key(problem.prompt) { MathWorkshopBoard(game, mission, false, onAttempt = { if (!it) onWrongAttempt() }) { onSubmitChoice(problem.answer) } }
        feedback?.let { Text(it, color = Color(0xFF31725D), fontSize = 11.sp) }
    }
}

@Composable
private fun GameAnswerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(GameGold),
        textStyle = TextStyle(
            color = GameInk,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(.34f), accent.copy(.12f), GamePanel.copy(.92f))))
                    .border(2.dp, accent.copy(.85f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isBlank()) {
                        Text("Type your answer here", color = GameMuted.copy(.7f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    innerTextField()
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(.22f))
                        .border(1.dp, accent.copy(.7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("KB", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        },
    )
}

@Composable
private fun SpeedResultsScreen(
    title: String,
    accent: Color,
    duration: Int,
    correct: Int,
    attempted: Int,
    bestStreak: Int,
    onAgain: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val accuracy = if (attempted == 0) 0 else correct * 100 / attempted
    val perMinute = if (duration == 0) 0 else (correct * 60f / duration).toInt()
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(accent.copy(.32f), GameSpace, Color(0xFF020714))))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundGameButton("‹", accent, "Back to games", onBack)
            Text("Sprint complete", color = GameInk, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            RoundGameButton("⚙", accent, "Open settings", onSettings)
        }
        GlossyPanel(accent) {
            Text(title.uppercase(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("$correct", color = GameInk, fontSize = 58.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("CORRECT ANSWERS", color = GameMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("ACCURACY", "$accuracy%", GameGreen, Modifier.weight(1f))
            StatTile("PER MIN", "$perMinute", GameBlue, Modifier.weight(1f))
            StatTile("BEST RUN", "$bestStreak", GameGold, Modifier.weight(1f))
        }
        Text("$attempted problems attempted in ${formatDuration(duration)}", color = GameMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        GlossyPanel(if (accuracy >= 85) GameGreen else GameGold) {
            Text("NEXT LEARNING MOVE", color = if (accuracy >= 85) GameGreen else GameGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    attempted == 0 -> "Start with a short 30-second round and solve deliberately."
                    accuracy < 60 -> "Reduce the digit count or topics. Write one clean step mentally before submitting; accuracy comes before speed."
                    accuracy < 85 -> "Keep these settings and repeat. Aim for at least 85% accuracy before increasing difficulty."
                    bestStreak < 5 -> "Accuracy is ready. Work on consistency: aim for a five-answer streak with the same settings."
                    else -> "Mastery reached at this setting. Add a digit, mix another topic, or shorten the response time."
                },
                color = GameInk,
                fontSize = 12.sp,
            )
        }
        PrimaryGameButton("Play Again", accent, onAgain)
        SecondaryGameButton("Change Settings", accent, onSettings)
        SecondaryGameButton("Back to Games", GameMuted, onBack)
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(GamePanel).border(1.dp, accent.copy(.5f), RoundedCornerShape(15.dp)).padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = GameInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun <T> ChoiceGrid(
    choices: List<T>,
    selected: (T) -> Boolean,
    label: (T) -> String,
    accent: Color,
    onClick: (T) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = if (maxWidth < 350.dp) (maxWidth - 7.dp) / 2 else (maxWidth - 14.dp) / 3
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            choices.forEach { choice ->
                val active = selected(choice)
                Box(
                    Modifier.width(width).height(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (active) accent.copy(.32f) else Color.White.copy(.05f))
                        .border(1.dp, if (active) accent else GameMuted.copy(.25f), RoundedCornerShape(13.dp))
                        .clickable { onClick(choice) }.focusable()
                        .semantics { contentDescription = "${label(choice)}, ${if (active) "selected" else "not selected"}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(choice), color = if (active) GameInk else GameMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun <T> toggleChoice(current: Set<T>, item: T): Set<T> =
    if (item in current) current - item else current + item

private fun formatDuration(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60} min"

private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun answersMatch(input: String, expected: String): Boolean {
    val enteredNumber = input.trim().replace(",", ".").toDoubleOrNull()
    val expectedNumber = expected.toDoubleOrNull()
    return if (enteredNumber != null && expectedNumber != null) abs(enteredNumber - expectedNumber) < 0.001
    else input.trim().equals(expected, ignoreCase = true)
}

internal fun speedAnswerChoices(problem: SpeedProblem, random: Random = Random.Default): List<String> {
    val correct = problem.answer.trim()
    val expectedNumber = correct.replace(",", ".").toDoubleOrNull()
    val choices = linkedSetOf(correct)

    if (expectedNumber != null) {
        val integerAnswer = expectedNumber % 1.0 == 0.0
        val baseStep = when {
            abs(expectedNumber) >= 100 -> 10.0
            integerAnswer -> 1.0
            else -> 0.5
        }
        var offset = 1
        while (choices.size < 4 && offset < 24) {
            listOf(-offset, offset, offset + 1, -(offset + 1)).forEach { direction ->
                if (choices.size < 4) {
                    val candidate = expectedNumber + direction * baseStep
                    if (candidate >= 0 || expectedNumber < 0) choices += compactNumber(candidate)
                }
            }
            offset += 2
        }
        var fallback = 2
        while (choices.size < 4) {
            choices += compactNumber(expectedNumber + fallback * baseStep)
            fallback++
        }
    } else {
        val variations = listOf(
            correct.reversed(),
            "$correct + 1",
            "$correct - 1",
            "Not $correct",
            "0",
        )
        choices += variations.filter { it.isNotBlank() && it != correct }
        var fallback = 1
        while (choices.size < 4) {
            choices += "Choice ${fallback++}"
        }
    }

    return choices.take(4).shuffled(random)
}

internal fun nextUniqueSpeedProblem(
    mode: SpeedCalculationMode,
    digits: Int,
    selectedBasic: Set<String>,
    selectedAdvanced: Set<String>,
    usedPrompts: MutableSet<String>,
    random: Random = Random.Default,
    mastery: Int = 0,
): SpeedProblem {
    repeat(512) {
        val candidate=generateTemplateSpeedProblem(mode,digits,selectedBasic,selectedAdvanced,random,mastery)
        if(usedPrompts.add(candidate.prompt))return candidate
    }
    val candidate=generateTemplateSpeedProblem(mode,digits,selectedBasic,selectedAdvanced,random,mastery)
    var offset=random.nextInt(1,1001)
    while(!usedPrompts.add("First solve: ${candidate.prompt}. Then add $offset to the result."))offset++
    val value=candidate.answer.toDouble()+offset
    return candidate.copy(prompt="First solve: ${candidate.prompt}. Then add $offset to the result.",answer=roundNumber(value),solution="${candidate.solution} Then add $offset to obtain ${roundNumber(value)}.",solutionTokens=listOf("(")+candidate.solutionTokens+listOf(")","+","$offset"))
}

private fun compactNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

internal fun speedMasteryProblem(mode:SpeedCalculationMode,mastery:Int,random:Random):SpeedProblem {
    require(mastery in 1..2)
    return generateTemplateSpeedProblem(mode,1,setOf("Addition"),setOf("Advanced Calculation"),random,mastery)
}
