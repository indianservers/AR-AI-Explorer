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

class GlossyMathGamesTest {
    @get:Rule val compose=createComposeRule()

    @Test fun captureReferenceThemesAndEveryMasteryOnCompactPhone(){
        val games=gamifyGamesForAudit().filter{it.id in glossyMathGameIds}
        assertEquals(5,games.size)
        val cases=games.flatMap{game->if(game.id.startsWith("speed-")) listOf(game to 0) else listOf(game to 0,game to 119,game to 120,game to 121)}
        var index by mutableIntStateOf(0)
        compose.setContent{Box(Modifier.fillMaxSize().safeDrawingPadding()){key(index){
            val (game,level)=cases[index]
            if(game.id.startsWith("speed-")) {
                val problem=if(game.id=="speed-basic")SpeedProblem("4 − 4","0","Subtraction")else SpeedProblem("10tan(−315°)","10","Trigonometry")
                SpeedPlayScreen(game.title,game.accent,45,45,problem,"",0,0,0,null,SpeedAnswerMode.Typed,{}, {}, {}, {}, {})
            } else GeneratedMathGameScreen(game,level,{}, {},initiallyPlaying=true,initialSeed=42L)
        }}}
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"glossy-maths-games").apply{mkdirs()}
        val rows=mutableListOf("game,level,scroll_dp");val overflow=mutableListOf<String>()
        cases.forEachIndexed{i,(game,level)->
            compose.runOnIdle{index=i};compose.waitForIdle()
            val scroll=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
                .maxOfOrNull{it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()/it.layoutInfo.density.density}?:0f
            rows+="${game.title},$level,$scroll";if(scroll>1f)overflow+="${game.title}/$level=$scroll"
            val suffix=when(level){0->"";119->" - Expert";else->" - Mastery ${level-119}"}
            val filename="${game.title}$suffix.png"
            if(InstrumentationRegistry.getArguments().getString("hostScreenshots")=="true"){
                val token=java.util.UUID.randomUUID().toString()
                File(dir,"capture-request.txt").writeText("$token|$filename")
                compose.waitUntil(timeoutMillis=30_000){File(dir,"capture-ack.txt").let{it.exists() && it.readText().trim()==token}}
            }else{
                val image=compose.onRoot().captureToImage().asAndroidBitmap()
                File(dir,filename).outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
            }
            compose.onNodeWithContentDescription(if(game.id.startsWith("speed-"))"Exit challenge" else "Back").assertIsDisplayed()
            compose.onNodeWithContentDescription(if(game.id.startsWith("speed-"))"Open challenge settings" else "Game session settings").assertIsDisplayed()
        }
        File(dir,"layout.csv").writeText(rows.joinToString("\n"))
        File(dir,"capture-done.txt").writeText("done")
        assertTrue("Glossy game overflow: $overflow", overflow.isEmpty())
    }

    @Test fun glossyExpressionsAndRobotsKeepDragUndoRunAndRestorationWorking() {
        var index by mutableIntStateOf(0)
        var completions=0
        val state=StateRestorationTester(compose)
        state.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) {
            val game=gamifyGamesForAudit().first{it.id==if(index==0)"chance" else "combinatorics"}
            val source=GeneratedRound("Chance Explorer","Find P(red) for 7 red and 3 blue counters.",.7,"Use red / total.","Use the total.",emptyList(),operands=listOf(7.0,3.0))
            val round=if(index==0)WorkshopChallenge(WorkshopKind.Expression,source.topic,source.prompt,source.hint,source.explanation,source,numbers=listOf(7.0,3.0))else generateWorkshopChallenge("combinatorics",0,0,42L,0)
            GlossyMathFrame(game,1,"0:00",0,0,.25f,round.title,round.prompt,{}, {},null){
                LightWorkshopBoard(game,round,false,{}, {completions++})
            }
        } } }
        fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        fun drag(description:String,valid:Boolean=true){
            val node=compose.onNodeWithContentDescription(description).performScrollTo()
            val a=node.fetchSemanticsNode().boundsInRoot
            val b=compose.onNodeWithContentDescription("Construction drop zone").fetchSemanticsNode().boundsInRoot
            node.performTouchInput{swipe(center,if(valid)b.center-a.topLeft else center+Offset(0f,120f),450)}
        }
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        compose.onNodeWithText("Check").assertIsNotEnabled()
        drag("Place number 7",false)
        compose.onNodeWithContentDescription("Remove token 1").assertDoesNotExist()
        drag("Place number 7")
        tap("Place operation ÷");tap("Place operation (");tap("Place number 7");tap("Place operation +");tap("Place number 3");tap("Place operation )")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Run").performScrollTo().performClick()
        compose.onNodeWithText("=  0.7").assertIsDisplayed()
        assertEquals(0,completions)
        compose.onNodeWithText("Check").performScrollTo().performClick()
        assertEquals(1,completions)
        compose.runOnIdle{index=1}
        tap("Choose robot A")
        tap("Formation slot 1")
        compose.onNodeWithContentDescription("Choose robot A").assertIsEnabled()
        val round=generateWorkshopChallenge("combinatorics",0,0,42L,0)
        round.witness.forEach{formation->
            formation.split(',').forEach{drag("Choose robot ${('A'.code+it.toInt()).toChar()}")}
            compose.onNodeWithText("Collect").performScrollTo().performClick()
        }
        compose.onNodeWithText("Check").performScrollTo().performClick()
        assertEquals(2,completions)
    }

}
