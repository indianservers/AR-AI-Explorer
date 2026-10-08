package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GeneratedGameSessionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun allTenMechanicsCanBeSolvedUsingTheirTouchControls() {
        val cases=listOf("kitchen" to 0,"fractions" to 0,"vectors" to 0,"balance" to 0,"potions" to 0,
            "times-table" to 0,"function-machine" to 0,"data" to 1,"number-theory" to 0,"logic" to 120)
        var index by mutableIntStateOf(0)
        var completions=0
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) {
            val (id,level)=cases[index]
            val game=gamifyGamesForAudit().first{it.id==id}
            val round=generateWorkshopChallenge(id,level,0,42L)
            Column(Modifier.verticalScroll(rememberScrollState())) { MathWorkshopBoard(game,round,false){completions++} }
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
        val before=texts().filter{listOf(" + "," − "," × "," ÷ ","%"," = "," of ").any(it::contains)}
        assertTrue(before.isNotEmpty())
        compose.onNodeWithContentDescription("Place number 2").performScrollTo().performClick()
        state.emulateSavedInstanceStateRestore()
        val after=texts()
        assertTrue(before.all{it in after})
        compose.onNodeWithContentDescription("Remove token 1").assertIsDisplayed()
    }

    @Test fun expertAndMasteryScreensFitEveryWorld() {
        val preferences=InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("maths_games",0)
        val games=gamifyGamesForAudit().filterNot{it.id.startsWith("speed-")}
        val keys=games.map{"difficulty_${it.id}"}
        val previous=keys.associateWith{preferences.all[it]}
        val cases=games.flatMap{game->listOf(game to 119,game to 120,game to 121)}
        var index by mutableIntStateOf(0)
        fun configure(i:Int) { preferences.edit().putInt("difficulty_${cases[i].first.id}",3).commit() }
        try {
            configure(0)
            compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) {
                GeneratedMathGameScreen(cases[index].first,cases[index].second,{}, {},initiallyPlaying=true,initialSeed=13L)
            } } }
            cases.forEachIndexed{i,(game,level)->
                compose.runOnIdle{configure(i);index=i}
                compose.waitForIdle()
                val scroll=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
                    .maxOfOrNull{it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()/it.layoutInfo.density.density} ?: 0f
                assertTrue("${game.id} / level=$level scroll=$scroll",scroll<=1f)
            }
        } finally {
            val editor=preferences.edit()
            previous.forEach{(key,value)->if(value is Int)editor.putInt(key,value)else editor.remove(key)}
            editor.commit()
        }
    }

    @Test fun questionAndCheckedAnswerSurviveRestorationThenNextLevelIsFresh() {
        val preferences=InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("maths_games",0)
        val key="difficulty_fractions"
        val previous=preferences.all[key]
        preferences.edit().putInt(key,0).commit()
        try {
            val game=gamifyGamesForAudit().first{it.id=="fractions"}
            val round=generateWorkshopChallenge(game.id,0,0,42L)
            val state=StateRestorationTester(compose)
            var completions=0
            state.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                GeneratedMathGameScreen(game,0,{}, { completions++ },initiallyPlaying=true,initialSeed=42L)
            } }
            fun hasPrompt()=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).fetchSemanticsNodes()
                .any{node -> node.config[SemanticsProperties.Text].any{it.text==round.prompt}}
            assertTrue(hasPrompt())
            state.emulateSavedInstanceStateRestore()
            assertTrue(hasPrompt())
            assertEquals(0,completions)
            val unit=round.parameters[0]
            round.witness.map(String::toInt).forEachIndexed { index,value ->
                val core=compose.onNodeWithContentDescription("Add $value/$unit core").performScrollTo()
                if(index==0) {
                    core.performTouchInput { swipe(center,center-Offset(0f,100f),400) }
                    state.emulateSavedInstanceStateRestore()
                } else core.performClick()
            }
            compose.onNodeWithText("Check build").performClick()
            assertEquals(1,completions)
            state.emulateSavedInstanceStateRestore()
            assertEquals(1,completions)
            compose.onNodeWithText("Next level").performClick()
            assertFalse(hasPrompt())
            assertEquals(1,completions)
        } finally {
            val editor=preferences.edit()
            if(previous is Int)editor.putInt(key,previous)else editor.remove(key)
            editor.commit()
        }
    }
}
