package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class SolverPhase2UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun allExplanationLevelsCanBeSelectedWithoutChangingInput() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("gcd(84,30)")
        listOf("Child-friendly", "School examination", "University", "Rigorous").forEach { level ->
            composeRule.onNodeWithText(level, substring = true).performClick()
            composeRule.onNodeWithText(level, substring = true).assertIsDisplayed()
        }
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("6").assertIsDisplayed()
    }

    @Test
    fun alternativeMethodCanBeChosenWithoutReenteringProblem() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("gcd(84,30)")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Prime factorisation").performClick()
        composeRule.onNodeWithText("* Prime factorisation").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
    }

    @Test
    fun restrictionsMistakesCopyAndVerificationAreAccessible() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("domain sqrt(x-2)/(x-3)")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Assumptions and restrictions").assertIsDisplayed()
        composeRule.onNodeWithText("Copy expression").assertIsDisplayed()
        composeRule.onNodeWithText("Copy full working").assertIsDisplayed()
        composeRule.onNodeWithText("Verification", substring = true).assertIsDisplayed()
    }

    @Test
    fun historySearchAndBookmarksRemainLocal() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("distance(0,0,3,4)")
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithContentDescription("Search Solver history").performTextReplacement("distance")
        composeRule.onNodeWithText("distance(0,0,3,4)").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Unsave").assertIsDisplayed()
    }

    private fun openSolver() {
        composeRule.onNodeWithText("AI Maths Explorer", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Solver").performClick()
        composeRule.onNodeWithText("OFFLINE | KEYBOARD ONLY").assertIsDisplayed()
    }
}
