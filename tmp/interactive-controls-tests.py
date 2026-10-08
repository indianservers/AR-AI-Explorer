from pathlib import Path
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/gamifymaths/GeneratedGameSessionTest.kt')
s=p.read_text(encoding="utf-8").replace('import androidx.compose.foundation.layout.Box','import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.Column').replace('import androidx.compose.ui.test.onNodeWithText','import androidx.compose.ui.test.assertIsDisplayed\nimport androidx.compose.ui.test.onNodeWithText')
a=s.index('    @Test fun expertAndMastery')
s=s[:a]+'''    @Test fun allTenMechanicsCanBeSolvedUsingTheirTouchControls() {
        val cases=listOf("kitchen" to 0,"fractions" to 0,"vectors" to 0,"balance" to 0,"potions" to 0,
            "times-table" to 0,"function-machine" to 0,"data" to 1,"number-theory" to 0,"logic" to 120)
        var index by mutableIntStateOf(0)
        var completions=0
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) {
            val (id,level)=cases[index]
            val game=gamifyGamesForAudit().first{it.id==id}
            val round=generateWorkshopChallenge(id,level,0,42L)
            Column { MathWorkshopBoard(game,round,false){completions++} }
        } } }
        fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        cases.forEachIndexed { caseIndex,(id,level)->
            compose.runOnIdle{index=caseIndex}
            compose.waitForIdle()
            val round=generateWorkshopChallenge(id,level,0,42L)
            when(round.kind) {
                WorkshopKind.Expression->{
                    tap("Place number ${roundNumber(round.source!!.operands[0])}")
                    tap("Place operation +")
                    tap("Place number ${roundNumber(round.source.operands[1])}")
                    compose.onNodeWithText("Check").performClick()
                }
                WorkshopKind.Pieces->{
                    round.witness.forEach{tap("Add $it/${round.parameters[0]} core")}
                    compose.onNodeWithText("Check build").performClick()
                }
                WorkshopKind.Navigation->{
                    round.witness.map(String::toInt).forEach{val move=round.moves[it];tap("Move by (${move.first},${move.second})")}
                    compose.onNodeWithText("Land ship").performClick()
                }
                WorkshopKind.Balance,WorkshopKind.Machine->{
                    round.witness.chunked(2).forEach { (op,value)->
                        val available=(if(round.kind==WorkshopKind.Balance)round.numbers.take(4) else round.numbers).distinct()
                        if(value.toDouble() in available)tap("Select core $value") else {
                            val start=available.first().toInt();tap("Select core $start")
                            repeat(kotlin.math.abs(value.toInt()-start)){compose.onNodeWithText(if(value.toInt()>start)"+1" else "−1").performClick()}
                        }
                        tap(if(round.kind==WorkshopKind.Balance)"Apply $op to both sides" else "Add $op block")
                    }
                    compose.onNodeWithText(if(round.kind==WorkshopKind.Balance)"Unlock" else "Test circuit").performClick()
                }
                WorkshopKind.Mixture,WorkshopKind.Grid->{
                    round.witness.map(String::toInt).forEachIndexed { axis,value->
                        val subject=if(round.kind==WorkshopKind.Mixture)if(axis==0)"blue volume" else "red volume" else if(axis==0)"rows" else "columns"
                        val steps=value-if(round.kind==WorkshopKind.Grid)1 else 0
                        val increment=if(round.kind==WorkshopKind.Mixture)5 else 3
                        repeat(steps/increment){tap("Increase $subject by $increment")}
                        repeat(steps%increment){tap("Increase $subject by 1")}
                    }
                    compose.onNodeWithText(if(round.kind==WorkshopKind.Mixture)"Mix batch" else "Launch array").performClick()
                }
                WorkshopKind.Sort->{
                    val split=round.witness.indexOf("|")
                    round.witness.take(split).map(String::toInt).forEach{tap("Place observation ${it+1}: ${roundNumber(round.numbers[it])}")}
                    round.witness.drop(split+1).map(String::toInt).forEach{tap("Mark slot ${it+1}")}
                    compose.onNodeWithText("Verify evidence").performClick()
                }
                WorkshopKind.Factors->{
                    round.witness.forEach{tap("Add prime $it")}
                    compose.onNodeWithText("Unlock crypt").performClick()
                }
                WorkshopKind.Configurations->{
                    round.witness.forEach { formation->
                        formation.split(',').map(String::toInt).forEach{tap("Choose robot ${('A'.code+it).toChar()}")}
                        compose.onNodeWithText("Collect").performClick()
                    }
                    compose.onNodeWithText("Check").performClick()
                }
            }
            compose.onNodeWithText("Construction verified").assertIsDisplayed()
            assertEquals(caseIndex+1,completions)
        }
    }

    @Test fun sprintQuestionAndPlacedCoreSurviveRestoration() {
        val state=StateRestorationTester(compose)
        state.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { SpeedCalculationGame(SpeedCalculationMode.Basic,{}, {}) } }
        compose.onNodeWithText("Start 45s Sprint").performScrollTo().performClick()
        fun texts()=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).fetchSemanticsNodes().flatMap{it.config[SemanticsProperties.Text]}.map{it.text}
        val before=texts()
        compose.onNodeWithContentDescription("Place number 2").performScrollTo().performClick()
        state.emulateSavedInstanceStateRestore()
        val after=texts()
        assertTrue(before.filter{it.contains("?") || it.contains(" = ")}.all{it in after})
        compose.onNodeWithContentDescription("Remove token 1").assertIsDisplayed()
    }

'''+s[a:];s=s.replace('Start 45s Sprint','Start 45s Sprint')
p.write_text(s,encoding="utf-8")
p=Path('app/src/androidTest/java/com/indianservers/aiexplorer/gamifymaths/MobileGamesLayoutTest.kt')
s=p.read_text(encoding="utf-8");a=s.index('            gamifyGamesForAudit().filterNot')
s=s[:a]+'''            SpeedCalculationMode.entries.forEach { mode->
                (1..2).forEach { mastery->
                    val problem=speedMasteryProblem(mode,mastery,kotlin.random.Random(42L))
                    add(Case("speed-${mode.name.lowercase()}-mastery-$mastery") {
                        SpeedPlayScreen("Mastery sprint",GameBlue,45,45,problem,"",0,0,0,null,SpeedAnswerMode.MultipleChoice,{}, {}, {}, {}, {})
                    })
                }
            }
'''+s[a:];p.write_text(s,encoding="utf-8")

