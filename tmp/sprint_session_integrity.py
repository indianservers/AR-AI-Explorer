from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/SpeedCalculationGame.kt');s=p.read_text(encoding='utf-8')
s=s.replace('import androidx.compose.runtime.saveable.rememberSaveable','import androidx.compose.runtime.saveable.listSaver\nimport androidx.compose.runtime.saveable.rememberSaveable')
s=s.replace('val usedPrompts = remember { linkedSetOf<String>() }','val usedPrompts: MutableSet<String> = rememberSaveable(saver = listSaver<MutableSet<String>, String>(save = { it.toList() }, restore = { it.toMutableSet() })) { linkedSetOf<String>() }')
s=s.replace('var problem by remember {','var problem by rememberSaveable(stateSaver = listSaver<SpeedProblem, String>(save = { listOf(it.prompt, it.answer, it.topic) }, restore = { SpeedProblem(it[0], it[1], it[2]) })) {')
s=s.replace('                onSettings = { screenName = SpeedScreen.Settings.name },\n                onBack = onBack,','                onSettings = { screenName = SpeedScreen.Settings.name },\n                onBack = onBack,\n                onWrongAttempt = { attempted++; streak = 0; feedback = "Check your method and retry. The target answer remains hidden." },',1)
a=s.index('internal fun SpeedPlayScreen(');b=s.index(') {',a);header=s[a:b]
header=header.replace('    onBack: () -> Unit,','    onBack: () -> Unit,\n    onWrongAttempt: () -> Unit = {},');s=s[:a]+header+s[b:]
s=s.replace('MathWorkshopBoard(game, mission, false) { onSubmitChoice(problem.answer) }','MathWorkshopBoard(game, mission, false, onAttempt = { if (!it) onWrongAttempt() }) { onSubmitChoice(problem.answer) }')
p.write_text(s,encoding='utf-8')
