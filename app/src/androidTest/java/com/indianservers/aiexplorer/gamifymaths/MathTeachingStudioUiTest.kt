package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class MathTeachingStudioUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun allGamesOfferAccessiblePlanCheckAndReflectionWithoutReplacingTheQuestion(){
        val games=gamifyGamesForAudit()
        var index by mutableIntStateOf(0)
        compose.setContent{Box(Modifier.fillMaxSize().safeDrawingPadding()){key(index){
            val game=games[index]
            if(game.id.startsWith("speed-"))SpeedCalculationGame(if(game.id=="speed-basic")SpeedCalculationMode.Basic else SpeedCalculationMode.Advanced,{}, {})
            else GeneratedMathGameScreen(game,0,{}, {},initiallyPlaying=true,initialSeed=17L)
        }}}
        games.forEachIndexed{i,game->
            compose.runOnIdle{index=i}
            if(game.id.startsWith("speed-"))compose.onNode(hasText("Start ",substring=true) and hasText(" Sprint",substring=true)).performScrollTo().performClick()
            compose.onNodeWithText("How to play · Strategy ⓘ").performScrollTo().performClick()
            compose.onNodeWithText("THE MATHEMATICAL MODEL").assertIsDisplayed()
            compose.onNodeWithContentDescription("Strategy Check").performClick()
            compose.onNodeWithText("Watch out").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Strategy Reflect").performScrollTo().performClick()
            compose.onNodeWithText("Explain your choice of method before trying another mission.").assertIsDisplayed()
            compose.onNodeWithText("Back to mission").performClick()
            compose.onNodeWithText("How to play · Strategy ⓘ").assertIsDisplayed()
        }
    }
}
