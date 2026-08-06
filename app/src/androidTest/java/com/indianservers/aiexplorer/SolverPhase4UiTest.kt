package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
    fun solveShowsAnswerBeforeOptionalWorking() {
        solve("3x+5=20")
        composeRule.onNodeWithText("Answer").assertIsDisplayed()
        composeRule.onNodeWithText("x = 5").assertIsDisplayed()
        composeRule.onNodeWithText("Show steps").assertIsDisplayed()
        composeRule.onAllNodesWithText("Step 1", substring = true).assertCountEquals(0)
    }

    @Test
    fun stepsRemainAvailableThroughOneClearAction() {
        solve("3x+5=20")
        composeRule.onNodeWithText("Show steps").performClick()
        composeRule.onNodeWithText("Step 1", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Hide steps").assertIsDisplayed()
    }

    @Test
    fun advancedModesAndLegacyTopControlsDoNotCrowdSolver() {
        openSolver()
        listOf(
            "Clear",
            "History",
            "Calculators",
            "Learning",
            "Hint only",
            "Child-friendly",
            "School examination",
            "University",
            "Rigorous",
        ).forEach { composeRule.onAllNodesWithText(it, substring = true).assertCountEquals(0) }
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
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("AI Maths Explorer", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) return
        composeRule.onNodeWithText("AI Maths Explorer", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Solver").performClick()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true).assertIsDisplayed()
    }
}
