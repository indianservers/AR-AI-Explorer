from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GeneratedMathRounds.kt')
s=p.read_text(encoding='utf-8').replace('val total=n(4,9+tier); val chosen=n(2,3)','val total=n(4,20+tier*5); val chosen=n(2,3)');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GamifyMathsScreen.kt')
s=p.read_text(encoding='utf-8').replace('GameCatalogueAudit(it.id, it.title, it.difficulty, it.missions.size)','GameCatalogueAudit(it.id, it.title, it.difficulty, it.levelCount)')
a=s.index('internal fun gamifyMissionAudit():'); b=s.index('\n@Composable',a)
s=s[:a]+'''internal fun gamifyMissionAudit(): List<GameMissionAudit> = Games.filterNot { it.id.startsWith("speed-") }.flatMap { game ->
    (0 until GeneratedLevelCount).mapNotNull { level ->
        val round = generateMathRound(game.id, level, 0, level.toLong())
        if (round.interaction != RoundInteraction.Answer) null else
            GameMissionAudit(game.id, "${round.topic} • Level ${level + 1}", round.prompt, round.choices,
                roundNumber(round.answer), round.explanation)
    }
}
'''+s[b:]
a=s.index('@Composable\ninternal fun GameMissionScreen'); b=s.index('\n@Composable\nprivate fun DropReactor',a)
s=s[:a]+'''@Composable
internal fun GameMissionScreen(game: MathsGame, completedMissions: Int, onBack: () -> Unit, onComplete: (Int) -> Unit) {
    GeneratedMathGameScreen(game, completedMissions, onBack, onComplete)
}
'''+s[b:]
p.write_text(s,encoding='utf-8')
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/gamifymaths/MobileGamesLayoutTest.kt');s=p.read_text(encoding='utf-8')
a=s.index('            repeat(6) { i -> add(Case("forge-');b=s.index('\n        }\n        var index',a)
s=s[:a]+'''            gamifyGamesForAudit().filterNot { it.id.startsWith("speed-") }.forEach { game ->
                val levels = ((0 until if(game.id=="chance") 15 else 6).toList() + listOf(30,60,90,119)).distinct()
                levels.forEach { i -> add(Case("${game.id}-$i") {
                    GeneratedMathGameScreen(game, i, {}, {}, initiallyPlaying=true, initialSeed=42L)
                }) }
            }'''+s[b:]
s=s.replace('Open Speed Calculation,','Open Calculation Sprint,').replace('Open Advanced Speed,','Open Advanced Calculation Sprint,')
s=s.replace('progress_number-bonds','progress_times-table').replace('best_number-bonds','best_times-table').replace('Open Number Bond Garden, current topic Flexible Addition, beginner difficulty','Open Times Table Galaxy, current topic Multiplication Patterns, beginner difficulty')
s=s.replace('            compose.onNodeWithContentDescription("Drag answer 4 into reactor").performClick()', '''            compose.onNodeWithText("Start playing").performClick()
            val prompt = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.Text] }.first { "rows contain" in it.text }.text
            val numbers = Regex("[0-9]+").findAll(prompt).map { it.value.toInt() }.toList()
            compose.onNodeWithText((numbers[0]*numbers[1]).toString()).performClick()''')
s=s.replace('BEST 1/5 MISSIONS','BEST 1/120 MISSIONS')
p.write_text(s,encoding='utf-8')
