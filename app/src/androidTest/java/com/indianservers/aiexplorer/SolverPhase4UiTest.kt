package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class SolverPhase4UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun hintOnlyModeHidesFinalAnswerAndRevealsProgressively() {
        openSolver()
        input("3x+5=20")
        composeRule.onNodeWithText("Hint only").performClick()
        composeRule.onNodeWithText("Hint-only mode", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Give me only a hint").assertIsDisplayed()
        composeRule.onNodeWithText("Another hint").performClick()
        composeRule.onNodeWithText("Hint 2 of 6", substring = true).assertIsDisplayed()
    }

    @Test
    fun nextStepTutorUsesExistingMathKeyboardField() {
        solve("3x+5=20")
        composeRule.onNodeWithText("Ask me the next step").performClick()
        composeRule.onNodeWithContentDescription("Tutor next mathematical step input").assertIsDisplayed()
        composeRule.onNodeWithText("Check this step").assertIsDisplayed()
    }

    @Test
    fun practiceModesStayCollapsedUntilRequested() {
        solve("3x+5=20")
        composeRule.onNodeWithText("Practise similar").performClick()
        composeRule.onNodeWithText("Practise similar problems").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Practice answer using mathematical keyboard").assertIsDisplayed()
    }

    @Test
    fun calculatorCatalogueAndLocalLearningControlsAreDiscoverable() {
        openSolver()
        composeRule.onNodeWithText("Calculators").performClick()
        composeRule.onNodeWithContentDescription("Search shared Solver calculators").assertIsDisplayed()
        composeRule.onNodeWithText("Derivative calculator").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("Learning").performClick()
        composeRule.onNodeWithText("Private, local and approximate").assertIsDisplayed()
        composeRule.onNodeWithText("Clear Solver learning data").assertIsDisplayed()
    }

    private fun solve(expression: String) {
        openSolver()
        input(expression)
        composeRule.onNodeWithText("Solve").performClick()
    }

    private fun input(expression: String) {
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement(expression)
    }

    private fun openSolver() {
        composeRule.onNodeWithText("AI Maths Explorer", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Solver").performClick()
        composeRule.onNodeWithText("OFFLINE | KEYBOARD ONLY").assertIsDisplayed()
    }
}
