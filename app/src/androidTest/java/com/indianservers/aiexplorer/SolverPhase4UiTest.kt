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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Show steps", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        repeat(3) {
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
    }

    private fun input(expression: String) {
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement(expression)
        composeRule.onNodeWithContentDescription("Collapse math keyboard").performClick()
    }

    private fun openSolver() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("Open Offline Solver", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) return
        composeRule.onNodeWithContentDescription("Open Offline Solver", substring = true).performClick()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true).assertIsDisplayed()
    }
}
