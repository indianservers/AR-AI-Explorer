package com.indianservers.aiexplorer.gamifymaths

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

class ThemedMathGamesTest {
    @get:Rule val compose=createComposeRule()

    @Test fun captureReferenceThemesAndEveryMasteryOnCompactPhone(){
        val games=gamifyGamesForAudit().filter{it.id in themedMathGameIds}
        assertEquals(9,games.size)
        val cases=games.flatMap{game->listOf(game to 0,game to 120,game to 121)}+
            listOf(games.first{it.id=="measure"} to 3)
        var index by mutableIntStateOf(0)
        compose.setContent{Box(Modifier.fillMaxSize().safeDrawingPadding()){key(index){
            GeneratedMathGameScreen(cases[index].first,cases[index].second,{}, {},initiallyPlaying=true,initialSeed=42L)
        }}}
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"reference-themed-games").apply{mkdirs()}
        val rows=mutableListOf("game,level,scroll_dp");val overflow=mutableListOf<String>()
        cases.forEachIndexed{i,(game,level)->
            compose.runOnIdle{index=i};compose.waitForIdle()
            val scroll=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
                .maxOfOrNull{it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()/it.layoutInfo.density.density}?:0f
            rows+="${game.title},$level,$scroll";if(scroll>1f)overflow+="${game.title}/$level=$scroll"
            val suffix=when(level){0->"";3->" - Bridge";else->" - Mastery ${level-119}"}
            val filename="${game.title}$suffix.png"
            if(InstrumentationRegistry.getArguments().getString("hostScreenshots")=="true"){
                val token=java.util.UUID.randomUUID().toString()
                File(dir,"capture-request.txt").writeText("$token|$filename")
                compose.waitUntil(timeoutMillis=30_000){File(dir,"capture-ack.txt").let{it.exists() && it.readText().trim()==token}}
            }else{
                val image=compose.onRoot().captureToImage().asAndroidBitmap()
                File(dir,filename).outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
            }
            compose.onNodeWithContentDescription("Back").assertIsDisplayed()
            compose.onNodeWithContentDescription("Game session settings").assertIsDisplayed()
        }
        File(dir,"layout.csv").writeText(rows.joinToString("\n"))
        File(dir,"capture-done.txt").writeText("done")
        assertTrue("Reference theme overflow: $overflow",overflow.isEmpty())
    }

    private fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
    private fun drag(description:String){
        val source=compose.onNodeWithContentDescription(description).performScrollTo();val a=source.fetchSemanticsNode().boundsInRoot
        val b=compose.onNodeWithContentDescription("Construction drop zone").fetchSemanticsNode().boundsInRoot
        source.performTouchInput{swipe(center,b.center-a.topLeft,450)}
    }

    @Test fun themedFlasksStarsPrimeChainCargoAndFlightUseRealMath(){
        val ids=listOf("potions","ratio-rangers","times-table","number-theory","optimization-arena","vectors","patterns")
        var index by mutableIntStateOf(0);var completions=0
        compose.setContent{Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){key(index){
            CompositionLocalProvider(LocalMathBoardAccent provides Color(0xFF9B42E2)){
                MathWorkshopBoard(gamifyGamesForAudit().first{it.id==ids[index]},generateWorkshopChallenge(ids[index],0,0,42L),false){completions++}
            }
        }}}
        ids.forEachIndexed{i,id->
            compose.runOnIdle{index=i};compose.waitForIdle()
            val round=generateWorkshopChallenge(id,0,0,42L)
            when(round.kind){
                WorkshopKind.Mixture,WorkshopKind.Grid->{
                    round.witness.map(String::toInt).forEachIndexed{axis,value->
                        val subject=if(round.kind==WorkshopKind.Mixture)if(axis==0)"blue volume" else "red volume" else if(axis==0)"rows" else "columns"
                        val delta=if(round.kind==WorkshopKind.Mixture)5 else 3;val steps=value-if(round.kind==WorkshopKind.Grid)1 else 0
                        repeat(steps/delta){tap("Increase $subject by $delta")};repeat(steps%delta){tap("Increase $subject by 1")}
                    }
                    compose.onNodeWithText(if(round.kind==WorkshopKind.Mixture)"Mix batch" else "Launch array").performScrollTo().performClick()
                }
                WorkshopKind.Factors->{round.witness.forEachIndexed{j,value->if(j==0)drag("Add prime $value")else tap("Add prime $value")};compose.onNodeWithText("Unlock crypt").performScrollTo().performClick()}
                WorkshopKind.Pieces->{
                    val source=round.source!!
                    val selected=(0 until (1 shl source.pieces.size)).map{mask->source.pieces.filterIndexed{j,_->mask and (1 shl j)!=0}}
                        .filter{it.sumOf{value->source.weights.getValue(value)}<=source.capacity}.maxBy{it.sum()}
                    selected.forEachIndexed{j,value->val description="Add $value / ${source.weights.getValue(value)}kg core";if(j==0)drag(description)else tap(description)}
                    compose.onNodeWithText("Check build").performScrollTo().performClick()
                }
                WorkshopKind.Navigation->{round.witness.forEachIndexed{j,value->val move=round.moves[value.toInt()];val description="Move by (${move.first},${move.second})";if(j==0)drag(description)else tap(description)};compose.onNodeWithText("Land ship").performScrollTo().performClick()}
                WorkshopKind.Machine->{round.witness.chunked(2).forEachIndexed{j,(op,value)->tap("Select core $value");if(j==0)drag("Add $op block")else tap("Add $op block")};compose.onNodeWithText("Test circuit").performScrollTo().performClick()}
                else->error("Unexpected board ${round.kind}")
            }
            assertEquals(id,i+1,completions)
            compose.onNodeWithText("Construction verified").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun restoredPotionKeepsBothVolumes(){
        val state=StateRestorationTester(compose)
        state.setContent{Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            MathWorkshopBoard(gamifyGamesForAudit().first{it.id=="potions"},generateWorkshopChallenge("potions",0,0,42L),false){}
        }}
        tap("Increase blue volume by 5");tap("Increase red volume by 1")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("5 ml").assertIsDisplayed();compose.onNodeWithText("1 ml").assertIsDisplayed()
    }
}
