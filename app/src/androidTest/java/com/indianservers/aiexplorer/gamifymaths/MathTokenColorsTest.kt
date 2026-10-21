package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MathTokenColorsTest {
    @get:Rule val compose=createComposeRule()
    @Test fun functionsNumbersBracketsAndConstantsKeepTheirIdentityAndEvaluate() {
        val game=gamifyGamesForAudit().first{it.id=="speed-advanced"}
        val source=GeneratedRound("Construct an expression","cos(60°)",.5,"Use cosine.","Angles are in degrees.",emptyList(),operands=listOf(60.0,2.0))
        val round=WorkshopChallenge(WorkshopKind.Expression,source.topic,source.prompt,source.hint,source.explanation,source,numbers=source.operands)
        var completed=0
        val state=StateRestorationTester(compose)
        state.setContent{Box(Modifier.fillMaxSize().safeDrawingPadding()){
            GlossyMathFrame(game,null,"0:45",0,0,1f,round.title,round.prompt,{}, {},null){LightWorkshopBoard(game,round,false,{}, {completed++})}
        }}
        fun tap(description:String)=compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        listOf("Place number 60" to "Number tile","Place operation cos" to "Function tile","Place operation (" to "Bracket tile","Place operation +" to "Operator tile","Place operation π" to "Constant tile").forEach{(description,category)->
            compose.onNodeWithContentDescription(description).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,category))
        }
        tap("Place operation π");tap("Place operation ×");tap("Place number 2")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Remove token 1").assertTextContains("π")
        compose.onNodeWithText("Run").performScrollTo().performClick()
        compose.onNodeWithText("=  6.283185").assertIsDisplayed()
        assertEquals(0,completed)
        compose.onNodeWithText("Clear").performScrollTo().performClick()
        tap("Place operation cos");tap("Place operation (");tap("Place number 60");tap("Place operation )")
        compose.onNodeWithText("Run").performScrollTo().performClick()
        compose.onNodeWithText("=  0.5").assertIsDisplayed()
        compose.onNodeWithText("Check").performScrollTo().performClick()
        assertEquals(1,completed)
    }
}
