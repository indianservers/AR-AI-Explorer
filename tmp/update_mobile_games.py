from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths')
f=p/'GamifyMathsScreen.kt'
s=f.read_text(encoding='utf-8')
s=s.replace('import androidx.compose.foundation.Canvas','import androidx.compose.foundation.Image\nimport androidx.compose.foundation.layout.BoxWithConstraints\nimport androidx.compose.foundation.layout.aspectRatio\nimport androidx.compose.runtime.CompositionLocalProvider\nimport androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.painterResource\nimport com.indianservers.aiexplorer.R\nimport androidx.compose.foundation.Canvas',1)
s=s.replace('val completed = remember { mutableStateMapOf<String, Int>() }','''val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("maths_games", 0) }
    val completed = remember { mutableStateMapOf<String, Int>().apply {
        Games.forEach { put(it.id, preferences.getInt("progress_${it.id}", 0)) }
    } }
    var compactPlay by remember { mutableStateOf(preferences.getBoolean("compact_play", true)) }''')
s=s.replace('linkedProgressKeys.forEach { key -> completed[key] = maxOf(completed[key] ?: 0, value) }','''linkedProgressKeys.forEach { key ->
                    completed[key] = maxOf(completed[key] ?: 0, value)
                    preferences.edit().putInt("progress_$key", completed[key] ?: 0).apply()
                }''')
s=s.replace('            when (selectedGame.id) {','            CompositionLocalProvider(LocalCompactPlayPreference provides compactPlay) {\n            when (selectedGame.id) {',1)
s=s.replace('        } else {\n            when (destination)', '            }\n        } else {\n            when (destination)',1)
s=s.replace('GameHome(completed, onExit)', 'GameHome(completed, onExit, { destinationName = GameDestination.Profile.name })')
s=s.replace('PlayerProfileScreen(completed)','PlayerProfileScreen(completed, compactPlay) { value ->\n                    compactPlay = value\n                    preferences.edit().putBoolean("compact_play", value).apply()\n                }',1)
s=s.replace('onExit: () -> Unit, onOpenGame:', 'onExit: () -> Unit, onSettings: () -> Unit, onOpenGame:',1)
s=s.replace('GlossyPill("EXIT", Violet, onExit)','Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {\n                RoundGameButton("⚙", Cyan, "Game settings", onSettings)\n                GlossyPill("EXIT", Violet, onExit)\n            }',1)
start=s.index('        FlowRow(Modifier.fillMaxWidth()',s.index('private fun GameHome'))
end=s.index('\n    }\n}',start)
s=s[:start]+'''        GameCardGrid(completed, onOpenGame)'''+s[end:]
start=s.index('        DisplayGames.forEach',s.index('private fun WorldsScreen'))
end=s.index('\n    }\n}',start)
s=s[:start]+'''        GameCardGrid(completed, onOpenGame)'''+s[end:]
s=s.replace('.width(164.dp)\n            .heightIn(min = 178.dp)', '.aspectRatio(1f)')
s=s.replace('.padding(13.dp),\n        verticalArrangement = Arrangement.spacedBy(7.dp),','.padding(10.dp),\n        verticalArrangement = Arrangement.spacedBy(4.dp),',1)
s=s.replace('Modifier.size(56.dp)', 'Modifier.size(42.dp)',1)
s=s.replace('Text(game.icon, color = game.accent, fontSize = 20.sp, fontWeight = FontWeight.Black)','Image(painterResource(gameCardArtwork(game.id)), contentDescription = null, modifier = Modifier.size(40.dp))',1)
s=s.replace('fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2','fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2',1)
s=s.replace('        game.difficulty?.let { DifficultyTag(it, compact = true) }\n        Text(game.currentTopic', '        Text(game.currentTopic',1)
s=s.replace('Text("${completed}/${game.levelCount} MISSIONS",','Text(if (game.id.startsWith("speed-")) "BEST $completed SOLVED" else "BEST $completed/${game.levelCount} MISSIONS",',1)
s=s.replace('private fun PlayerProfileScreen(completed: Map<String, Int>) {','private fun PlayerProfileScreen(completed: Map<String, Int>, compactPlay: Boolean, onCompactPlay: (Boolean) -> Unit) {')
start=s.index('    var sound by',s.index('private fun PlayerProfileScreen'))
end=s.index('    Column(',start)
s=s[:start]+s[end:]
start=s.index('        SettingRow("Mission sounds"')
end=s.index('\n    }\n}',start)
s=s[:start]+'''        SettingRow("Compact play", "Smaller boards and fewer instructions on phones", compactPlay, onCompactPlay)
        Text("Previous best scores", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        DisplayGames.forEach { game ->
            Text("${game.title}: ${completed[game.id] ?: 0} ${if (game.id.startsWith("speed-")) "solved" else "missions mastered"}", color = Muted, fontSize = 12.sp)
        }'''+s[end:]
s=s.replace('missions mastered this session','missions mastered')
# All catalogue games use the same compact gameplay shell.
start=s.index('    Column(\n',s.index('private fun GameMissionScreen'))
end=s.index('        Column(\n            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))',start)
s=s[:start]+'''    GameScreen(game.title, missionIndex + 1, game.accent, if (result == false) 2 else 3, onBack, { showHint = !showHint }) {
'''+s[end:]
s=s.replace('.padding(18.dp),\n            verticalArrangement = Arrangement.spacedBy(8.dp),\n        ) {\n            Text(mission.title', '.padding(10.dp),\n            verticalArrangement = Arrangement.spacedBy(4.dp),\n        ) {\n            Text(mission.title')
s=s.replace('fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)', 'fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)')
s=s.replace('            Text("Learning goal: ${guidance.objective}", color = Muted, fontSize = 11.sp)','            if (!LocalCompactGameLayout.current) Text("Learning goal: ${guidance.objective}", color = Muted, fontSize = 11.sp)')
s=s.replace('        SecondaryGameButton(if (showHint) "Hide learning hint" else "Show learning hint", game.accent) { showHint = !showHint }\n','')
s=s.replace('.height(160.dp).clip(RoundedCornerShape(28.dp))','.height(if (LocalCompactGameLayout.current) 88.dp else 160.dp).clip(RoundedCornerShape(28.dp))')
s=s.replace('Canvas(Modifier.size(120.dp))','Canvas(Modifier.size(if (LocalCompactGameLayout.current) 72.dp else 120.dp))')
# Avoid four long answer tiles wrapping into a tall list.
s=s.replace('        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n            mission.tokens.forEach', '        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 2, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {\n            mission.tokens.forEach')
f.write_text(s,encoding='utf-8')
f=p/'GamifyMathsSharedUi.kt';s=f.read_text(encoding='utf-8')
s=s.replace('internal val LocalCompactGameLayout', 'internal val LocalCompactPlayPreference = staticCompositionLocalOf { true }\ninternal val LocalCompactGameLayout',1)
s=s.replace('val compact = maxHeight < 900.dp || maxWidth < 600.dp','val compact = LocalCompactPlayPreference.current && (maxHeight < 900.dp || maxWidth < 600.dp)')
s=s.replace('                GameLearningPhaseBanner(level, accent)','                if (!compact) GameLearningPhaseBanner(level, accent)')
s=s.replace('if (compact) 10.dp else 14.dp','if (compact) 8.dp else 14.dp').replace('if (compact) 7.dp else 10.dp','if (compact) 4.dp else 10.dp')
s=s.replace('if (compact) 38.dp else 42.dp','48.dp')
f.write_text(s,encoding='utf-8')
f=p/'GameComponentControls.kt';s=f.read_text(encoding='utf-8').replace('        Text(guidance,','        if (!LocalCompactGameLayout.current) Text(guidance,')
f.write_text(s,encoding='utf-8')
for name,obj in [('FractionFactoryGame.kt','task'),('MathsKitchenGame.kt','recipe'),('PotionLabGame.kt','challenge'),('RescueEngineerGame.kt','task'),('ShapeArchitectGame.kt','challenge')]:
 f=p/name;s=f.read_text(encoding='utf-8');s=s.replace(f'Text({obj}.explanation, color = GameInk)',f'Text(GameLearningCoach.guidance(1, {obj}.prompt).hint, color = GameInk)')
 if name=='ShapeArchitectGame.kt':
  s=s.replace('"Tap the shape that matches the name.", shape.replaceFirstChar(Char::uppercase)', 'when (shape) { "square" -> "Choose the shape with four equal sides and four right angles."; "circle" -> "Choose the shape with one curved boundary and no corners."; else -> "Choose the shape with three straight sides and three corners." }, "Shape detective"')
  s=s.replace('Text(shape.replaceFirstChar(Char::uppercase), color = GameSpace, fontSize = 9.sp, fontWeight = FontWeight.Bold)','')
  s=s.replace('contentDescription = "Drag $shape"','contentDescription = "Shape option. ${when (shape) { "circle" -> "Curved boundary, no corners"; "triangle" -> "Three straight sides"; "rectangle" -> "Four right angles, unequal adjacent sides"; else -> "Four equal sides and four right angles" }}"')
 f.write_text(s,encoding='utf-8')
f=p/'SpeedCalculationGame.kt';s=f.read_text(encoding='utf-8').replace('onComplete(if (correct > 0) 1 else 0)','onComplete(correct)').replace('Modifier.heightIn(min = 300.dp)','Modifier.heightIn(min = 180.dp)')
f.write_text(s,encoding='utf-8')
