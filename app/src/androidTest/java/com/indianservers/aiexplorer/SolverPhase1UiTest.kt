package com.indianservers.aiexplorer

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class SolverPhase1UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyboardInputSolvesAndShowsTraceableVerification() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("3x + 5 = 20")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("x = 5").assertIsDisplayed()
        composeRule.onNodeWithText("Division property of equality").assertIsDisplayed()
        composeRule.onNodeWithText("Verification | Verified").assertIsDisplayed()
    }

    @Test
    fun parseErrorAndUnsupportedInputAreExplicit() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("2+")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Unsupported or incomplete").assertIsDisplayed()
        composeRule.onNodeWithText("Position", substring = true).assertIsDisplayed()
        input.performTextReplacement("sin(x)")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Phase 1 does not solve function(s): sin", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyCanReopenAndClearAnOfflineSolution() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("3/4 + 5/6")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("Solver history").assertIsDisplayed()
        composeRule.onNodeWithText("3/4 + 5/6").performClick()
        composeRule.onNodeWithText("19/12").assertIsDisplayed()
        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("Clear history").performClick()
        composeRule.onNodeWithText("No Solver history yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun screenExposesOfflineKeyboardAndAccessibilitySemanticsForLongInput() {
        openSolver()
        composeRule.onNodeWithContentDescription(
            "Offline Solver screen with keyboard input, steps, verification, and local history",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("OFFLINE | KEYBOARD ONLY").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) * 2")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("110").assertIsDisplayed()
    }

    @Test
    fun inputAndCaretStateSurviveActivityRecreation() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("7x + 1 = 22")
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("x = 3").assertIsDisplayed()
    }

    private fun openSolver() {
        composeRule.onNodeWithText("AI Maths Explorer", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Solver").performClick()
        composeRule.onNodeWithText("Solver").assertIsDisplayed()
        composeRule.onNodeWithText("OFFLINE | KEYBOARD ONLY").assertIsDisplayed()
    }
}
