from pathlib import Path
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/gamifymaths/MobileGamesLayoutTest.kt')
s=p.read_text()
s='\n'.join(l for l in s.splitlines() if 'gamifymaths.probability.' not in l)+'\n'
s=s.replace('listOf(30,60,90,119)', 'listOf(30,60,90,119,120,121)')
s=s.replace('ArchitectPlayScreen(0, {}, {})','GeneratedMathGameScreen(gamifyGamesForAudit().first{it.id=="shapes"},0,{}, {},initiallyPlaying=true,initialSeed=42L)')
a=s.index('    @Test fun probabilityMenuHasTwoSquareCards()')
b=s.index('    @Test fun scoresAndSettingsSurviveReopening()',a)
s=s[:a]+'''    @Test fun everyGameHasDistinctArtworkAndBothMasteryButtons() {
        assertEquals(29,gamifyGamesForAudit().map{gameCardArtwork(it.id)}.distinct().size)
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            GeneratedMathGameScreen(gamifyGamesForAudit().first{it.id=="chance"},0,{}, {})
        } }
        compose.onNodeWithText("Mastery I").assertIsDisplayed()
        compose.onNodeWithText("Mastery II").assertIsDisplayed()
    }

'''+s[b:]
s=s.replace('"rows contain" in it.text','"rectangular star array" in it.text')
s=s.replace('compose.onNodeWithText((numbers[0]*numbers[1]).toString()).performClick()', '''repeat(numbers[1]-1){compose.onNodeWithContentDescription("Increase rows by 1").performClick()}
            repeat(numbers[0]/numbers[1]-1){compose.onNodeWithContentDescription("Increase columns by 1").performClick()}
            compose.onNodeWithText("Launch array").performClick()''')
s=s.replace('BEST 1/120 MISSIONS','BEST 1/122 MISSIONS')
s=s.replace('if (case.name.endsWith("-0"))','if (case.name.endsWith("-0") || case.name.endsWith("-120") || case.name.endsWith("-121"))')
p.write_text(s)
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/gamifymaths/GeneratedGameSessionTest.kt')
s=p.read_text().replace('import androidx.compose.ui.test.onNodeWithText','import androidx.compose.ui.test.onNodeWithContentDescription\nimport androidx.compose.ui.test.performScrollTo\nimport androidx.compose.ui.test.onNodeWithText')
s=s.replace('expertSixChoiceAndAnswerEntryScreensFitEveryWorld','expertAndMasteryScreensFitEveryWorld')
s=s.replace('listOf(game to false,game to true)','listOf(game to 119,game to 120,game to 121)').replace('(game,entry)=cases[i]','(game,level)=cases[i]').replace('.putBoolean("enter_answer_${game.id}",entry)','.putBoolean("enter_answer_${game.id}",false)').replace('cases[index].first,119','cases[index].first,cases[index].second').replace('(game,entry)->','(game,level)->').replace('/ entry=$entry','/ level=$level')
s=s.replace('generateMathRound(game.id,0,0,42L)','generateWorkshopChallenge(game.id,0,0,42L)')
s=s.replace('compose.onNodeWithText(roundNumber(round.answer)).performClick()', '''val unit=round.parameters[0]
            round.witness.map(String::toInt).forEach { value ->
                compose.onNodeWithContentDescription("Add $value/$unit core").performScrollTo().performClick()
            }
            compose.onNodeWithText("Check build").performClick()''')
p.write_text(s)
