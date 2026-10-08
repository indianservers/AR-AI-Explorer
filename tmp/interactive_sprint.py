from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/SpeedCalculationGame.kt');s=p.read_text(encoding='utf-8')
s=s.replace('var duration by rememberSaveable { mutableIntStateOf(45) }','var duration by rememberSaveable { mutableIntStateOf(45) }\n    var mastery by rememberSaveable { mutableIntStateOf(0) }')
s=s.replace('nextUniqueSpeedProblem(mode, digits, selectedBasic, selectedAdvanced, usedPrompts)','nextUniqueSpeedProblem(mode, digits, selectedBasic, selectedAdvanced, usedPrompts, mastery = mastery)')
s=s.replace('            onStart = ::startRound,','            onStart = ::startRound,\n            mastery = mastery,\n            onMastery = { mastery = it },')
s=s.replace('    onStart: () -> Unit,\n) {','    onStart: () -> Unit,\n    mastery: Int,\n    onMastery: (Int) -> Unit,\n) {',1)
a=s.index('        GlossyPanel(accent) {\n            Text("ANSWER STYLE"');b=s.index('\n        val hasSelection',a)
s=s[:a]+'''        GlossyPanel(accent) {
            Text("REACTOR LEVEL", color = GameInk, fontWeight = FontWeight.Bold)
            ChoiceGrid(choices = listOf(0, 1, 2), selected = { it == mastery },
                label = { if (it == 0) "Standard" else "Mastery $it" }, accent = accent, onClick = onMastery)
        }
        Text("Construct and run calculations to power the timed reactor. Answers are computed offline for every new mission.", color = GameMuted, fontSize = 12.sp)
'''+s[b:]
s=s.replace('if (answerMode == SpeedAnswerMode.Typed) "Solve as many as you can. Enter a decimal for fractional answers."\n            else "Solve as many as you can. Tap the correct answer from four choices."', '"Assemble mathematical methods to charge the reactor before time runs out."')
# Keep the composable signature compatible with callers; replace the entire quiz UI body.
a=s.index('internal fun SpeedPlayScreen(');body=s.index(') {',a)+3;b=s.index('\n@Composable\nprivate fun SpeedAnswerChoiceGrid',body)
s=s[:body]+'''
    val source = GeneratedRound("Timed reactor", displayLatexFormula(problem.prompt), problem.answer.toDouble(),
        "Your constructed calculation satisfies the generated timed mission.", "Assemble a mathematical method before checking it.", emptyList(),
        operands = Regex("[0-9]+(?:\\\\.[0-9]+)?").findAll(problem.prompt).map { it.value.toDouble() }.toList())
    val mission = WorkshopChallenge(WorkshopKind.Expression, "Timed reactor", source.prompt, source.hint, source.explanation,
        source, numbers = (source.operands + listOf(0.0, 1.0, 2.0, 100.0)).distinct())
    val game = gamifyGamesForAudit().first { it.id == if (title.contains("Advanced", true) || title == "speed-advanced") "speed-advanced" else "speed-basic" }
    Column(Modifier.fillMaxSize().background(GameSpace).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundGameButton("‹", accent, "Exit challenge", onBack)
            Text(title, color = GameInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            RoundGameButton("⚙", accent, "Open challenge settings", onSettings)
        }
        Text("${formatClock(secondsLeft)} • SCORE $correct • STREAK $streak • BEST ${LocalPreviousGameBest.current}", color = accent, fontSize = 12.sp)
        GameProgress(secondsLeft / duration.toFloat(), accent)
        Text(source.prompt, color = GameInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        key(problem.prompt) { MathWorkshopBoard(game, mission, false) { onSubmitChoice(problem.answer) } }
        feedback?.let { Text(it, color = GameGreen, fontSize = 11.sp) }
    }
}
'''+s[b:]
# Drop the obsolete multiple-choice grid, leave shared input utilities for any other callers.
a=s.index('@Composable\nprivate fun SpeedAnswerChoiceGrid');b=s.index('@Composable',a+12)
s=s[:a]+s[b:]
# Add a generated mastery family, used consistently including pool-exhaustion fallback.
s=s.replace('    random: Random = Random.Default,\n): SpeedProblem {','    random: Random = Random.Default,\n    mastery: Int = 0,\n): SpeedProblem {',1)
s=s.replace('val candidate = if (mode == SpeedCalculationMode.Basic) {','val candidate = if (mastery > 0) {\n            speedMasteryProblem(mode, mastery, random)\n        } else if (mode == SpeedCalculationMode.Basic) {')
a=s.index('            val (expression, value) = listOf(');b=s.index('            val scale =',a)
s=s[:a]+'''            val function = listOf("sin", "cos", "tan").random(random)
            var angle = random.nextInt(-24, 25) * if (function == "tan") 45 else 30
            while (function == "tan" && kotlin.math.abs(kotlin.math.cos(Math.toRadians(angle.toDouble()))) < 1e-9) angle = random.nextInt(-24, 25) * 45
            val radians = Math.toRadians(angle.toDouble())
            val value = when (function) { "sin" -> kotlin.math.sin(radians); "cos" -> kotlin.math.cos(radians); else -> kotlin.math.tan(radians) }
            val expression = """\\$function $angle^\\circ"""
'''+s[b:]
s=s.replace('SpeedProblem(prompt, compactNumber(scale * value + offset), topic)','SpeedProblem(prompt, roundNumber(scale * value + offset), topic)')
s+='''
internal fun speedMasteryProblem(mode:SpeedCalculationMode,mastery:Int,random:Random):SpeedProblem {
    require(mastery in 1..2)
    val a=random.nextInt(3,20);val b=random.nextInt(2,14);val c=random.nextInt(2,10);val d=random.nextInt(2,10)
    if(mode==SpeedCalculationMode.Basic) return if(mastery==1)
        SpeedProblem("($a + $b) × $c − $d",((a+b)*c-d).toString(),"Mastery I • chained operations")
    else SpeedProblem("(${a*20} − $b × $c) ÷ $d",roundNumber((a*20-b*c).toDouble()/d),"Mastery II • grouped division")
    return if(mastery==1) SpeedProblem("f(x) = ${c}x² + ${b}x; find f′($a)",(2*c*a+b).toString(),"Mastery I • calculus")
    else {
        val x=random.nextInt(-12,13);val y=random.nextInt(-12,13)
        SpeedProblem("x + y = ${x+y}; ${c}x − y = ${c*x-y}; find x",x.toString(),"Mastery II • simultaneous equations")
    }
}
'''
p.write_text(s,encoding='utf-8')
