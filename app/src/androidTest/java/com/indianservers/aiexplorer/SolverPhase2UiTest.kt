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

class SolverPhase2UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directAnswerAppearsWithoutExplanationModeSelection() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("gcd(84,30)")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("6")
        scrollWorkspaceUp()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        listOf("Child-friendly", "School examination", "University", "Rigorous").forEach {
            composeRule.onAllNodesWithText(it, substring = true).assertCountEquals(0)
        }
    }

    @Test
    fun alternativeMethodCanBeChosenWithoutReenteringProblem() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("gcd(84,30)")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("6")
        scrollWorkspaceUp()
        composeRule.onNodeWithText("Show steps").performClick()
        scrollWorkspaceUp()
        composeRule.onNodeWithText("Prime factorisation").performClick()
        composeRule.onNodeWithText("* Prime factorisation").assertIsDisplayed()
        composeRule.onNodeWithText("6").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun restrictionsMistakesCopyAndVerificationAreAccessible() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("domain sqrt(x-2)/(x-3)")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("Copy answer")
        scrollWorkspaceUp()
        composeRule.onNodeWithText("Copy answer").assertIsDisplayed()
        composeRule.onNodeWithText("Show steps").performClick()
        scrollWorkspaceUp()
        composeRule.onNodeWithText("Assumptions and restrictions").assertIsDisplayed()
        composeRule.onNodeWithText("Copy full working").assertIsDisplayed()
        composeRule.onNodeWithText("Verification", substring = true).assertIsDisplayed()
    }

    @Test
    fun topLevelHistoryAndCatalogueControlsAreRemoved() {
        openSolver()
        listOf("Learning").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
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

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
    }

    private fun collapseKeyboard() {
        composeRule.onNodeWithContentDescription("Collapse math keyboard").performClick()
        composeRule.waitForIdle()
    }

    private fun scrollWorkspaceUp() {
        repeat(3) {
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
        composeRule.waitForIdle()
    }
}
