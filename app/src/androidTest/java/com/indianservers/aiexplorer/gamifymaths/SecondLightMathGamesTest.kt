package com.indianservers.aiexplorer.gamifymaths

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SecondLightMathGamesTest {
    @get:Rule val compose=createComposeRule()

    @Test fun captureNextTenGamesAndAllMasteriesOnCompactPhone() {
        val games=gamifyGamesForAudit().filter{it.id in secondLightMathGameIds}
        assertEquals(10,games.size)
        val cases=games.flatMap{game->listOf(game to 0,game to 120,game to 121)}+listOf(games.first{it.id=="math-market"} to 2)
        var index by mutableIntStateOf(0)
        compose.setContent {Box(Modifier.fillMaxSize().safeDrawingPadding()){key(index){
            GeneratedMathGameScreen(cases[index].first,cases[index].second,{}, {},initiallyPlaying=true,initialSeed=42L)
        }}}
        val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"light-maths-games-batch2").apply{mkdirs()}
        val rows=mutableListOf("game,level,scroll_dp")
        val overflow=mutableListOf<String>()
        cases.forEachIndexed { i,(game,level)->
            compose.runOnIdle{index=i};compose.waitForIdle()
            val scroll=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
                .maxOfOrNull{it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()/it.layoutInfo.density.density}?:0f
            rows+="${game.title},$level,$scroll"
            if(scroll>1f)overflow+="${game.title}/$level: $scroll"
            val suffix=when(level){0->"";2->" - Basket";else->" - Mastery ${level-119}"}
            val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
            File(directory,"${game.title}$suffix.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            compose.onNodeWithContentDescription("Construction drop zone").assertIsDisplayed()
        }
        File(directory,"layout.csv").writeText(rows.joinToString("\n"))
        assertTrue("Compact layout overflow: $overflow",overflow.isEmpty())
    }

    private fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
    private fun drag(description:String) {
        val node=compose.onNodeWithContentDescription(description).performScrollTo()
        val bounds=node.fetchSemanticsNode().boundsInRoot
        val target=compose.onNodeWithContentDescription("Construction drop zone").fetchSemanticsNode().boundsInRoot
        node.performTouchInput{swipe(center,target.center-bounds.topLeft,400)}
    }

    @Test fun newConstructionNavigationAndCircuitBoardsSolveByTapAndDrag() {
        val cases=listOf("forge" to 0,"number-bonds" to 0,"shapes" to 120,"math-market" to 2,
            "integer-expedition" to 121,"matrix-mission" to 120,"function-machine" to 121,"logic" to 120)
        var index by mutableIntStateOf(0)
        var completions=0
        compose.setContent {Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){key(index){
            val(id,level)=cases[index]
            MathWorkshopBoard(gamifyGamesForAudit().first{it.id==id},generateWorkshopChallenge(id,level,0,42L),false){completions++}
        }}}
        cases.forEachIndexed { caseIndex,(id,level)->
            compose.runOnIdle{index=caseIndex};compose.waitForIdle()
            val round=generateWorkshopChallenge(id,level,0,42L)
            when(round.kind) {
                WorkshopKind.Pieces->{
                    val source=round.source!!
                    val pieces=when(source.interaction) {
                        RoundInteraction.BuildPlaceValue->{var number=source.answer.toInt();var place=1;buildList{while(number>0){repeat(number%10){add(place)};number/=10;place*=10}}}
                        RoundInteraction.BuildShapes->{
                            fun search(left:Int,sides:Int,curves:Int):List<Int>? {
                                if(left==0)return if(sides==0 && curves==0)emptyList() else null
                                if(sides<0 || curves<0)return null
                                for(value in source.pieces) {val rest=search(left-1,sides-value,curves-if(value==0)1 else 0);if(rest!=null)return listOf(value)+rest}
                                return null
                            }
                            search(source.requiredPieces,source.answer.toInt(),source.requiredCurves)!!
                        }
                        else->{
                            (0 until (1 shl source.pieces.size)).map{mask->source.pieces.filterIndexed{i,_->mask and (1 shl i)!=0}}
                                .first{it.size==source.requiredPieces && it.sum()==source.answer.toInt()}
                        }
                    }
                    pieces.forEachIndexed { i,value->
                        val description=if(source.interaction==RoundInteraction.BuildShapes)"Add piece with $value straight sides" else "Add $value core"
                        if(i==0)drag(description)else tap(description)
                    }
                    compose.onNodeWithText("Check build").performScrollTo().performClick()
                }
                WorkshopKind.Navigation->{
                    round.witness.forEachIndexed { i,value->val move=round.moves[value.toInt()];val description="Move by (${move.first},${move.second})";if(i==0)drag(description)else tap(description)}
                    compose.onNodeWithText("Land ship").performScrollTo().performClick()
                }
                WorkshopKind.Machine->{
                    round.witness.chunked(2).forEachIndexed { i,(op,value)->tap("Select core $value");if(i==0)drag("Add $op block")else tap("Add $op block")}
                    compose.onNodeWithText("Test circuit").performScrollTo().performClick()
                }
                WorkshopKind.Configurations->{
                    round.witness.forEach{formation->formation.split(',').forEach{tap("Choose robot ${('A'.code+it.toInt()).toChar()}")};compose.onNodeWithText("Collect").performScrollTo().performClick()}
                    compose.onNodeWithText("Check").performScrollTo().performClick()
                }
                else->error("Missing test for ${round.kind}")
            }
            compose.onNodeWithText("Construction verified").performScrollTo().assertIsDisplayed()
            assertEquals(id,caseIndex+1,completions)
        }
    }

    @Test fun navigationAndMachineEditsSurviveRestorationAndUndo() {
        val state=StateRestorationTester(compose)
        var index by mutableIntStateOf(0)
        state.setContent {Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){key(index){
            val id=if(index==0)"matrix-mission" else "function-machine"
            MathWorkshopBoard(gamifyGamesForAudit().first{it.id==id},generateWorkshopChallenge(id,0,0,42L),false){}
        }}}
        val round=generateWorkshopChallenge("matrix-mission",0,0,42L)
        val move=round.moves.first()
        drag("Move by (${move.first},${move.second})")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Ship (${round.parameters[0]+move.first},${round.parameters[1]+move.second}) · 1/${round.parameters[4]} moves").assertIsDisplayed()
        compose.onNodeWithText("Undo move").performClick()
        compose.onNodeWithText("Ship (${round.parameters[0]},${round.parameters[1]}) · 0/${round.parameters[4]} moves").assertIsDisplayed()
        compose.runOnIdle{index=1}
        tap("Add + block")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Remove block 1").assertIsDisplayed()
        compose.onNodeWithText("Undo block").performClick()
        compose.onNodeWithContentDescription("Remove block 1").assertDoesNotExist()
    }
}
