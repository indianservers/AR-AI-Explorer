package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MathTemplateGameplayTest {
    @get:Rule val compose=createComposeRule()
    @Test fun wrongChecksShowVerifiedAnswersAndMethodsWithoutAwardingPoints(){
        var variant by mutableIntStateOf(0)
        var points=0
        val game=gamifyGamesForAudit().first{it.id=="chance"}
        fun mission(v:Int)=instantiateWorkshopTemplate("chance",generateBaseWorkshopChallenge("chance",0,0,57L),v,0,57L)
        val state=StateRestorationTester(compose)
        state.setContent{Box(Modifier.fillMaxSize().safeDrawingPadding()){key(variant){
            val round=mission(variant)
            GlossyMathFrame(game,1,"0:00",points,0,.25f,round.title,round.prompt,{}, {},null){LightWorkshopBoard(game,round,false,{}, {points++})}
        }}}
        fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        tap("Place number 0");tap("Place operation +");tap("Place number 1")
        compose.onNodeWithText("Check").performScrollTo().performClick()
        val first=mission(0)
        compose.onNodeWithText("Correct answer: ${roundNumber(first.source!!.answer)}").performScrollTo().assertIsDisplayed()
        assertEquals(0,points)
        compose.onNodeWithText("View worked solution").performScrollTo().performClick()
        compose.onNodeWithText("Worked solution").assertIsDisplayed()
        compose.onNode(hasText(first.explanation,substring=true)).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Correct answer: ${roundNumber(first.source.answer)}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear").performScrollTo().performClick()
        first.witness.forEach{token->tap(if(token.toDoubleOrNull()!=null)"Place number $token"else "Place operation $token")}
        compose.onNodeWithText("Check").performScrollTo().performClick()
        assertEquals(1,points)
        compose.runOnIdle{variant=4}
        val next=mission(4)
        assertNotEquals(first.templateId,next.templateId)
        assertNotEquals(first.prompt,next.prompt)
        compose.onNodeWithText(next.prompt).assertIsDisplayed()
        compose.onNodeWithContentDescription("Verified solution").assertDoesNotExist()
    }
}
