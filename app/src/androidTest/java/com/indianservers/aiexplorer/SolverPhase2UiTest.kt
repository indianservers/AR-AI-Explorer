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

class SolverPhase2UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directAnswerAppearsWithoutExplanationModeSelection() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("gcd(84,30)")
        composeRule.onNodeWithText("Solve").performClick()
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
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Show steps").performClick()
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
        composeRule.onNodeWithText("Copy answer").assertIsDisplayed()
        composeRule.onNodeWithText("Show steps").performClick()
        composeRule.onNodeWithText("Assumptions and restrictions").assertIsDisplayed()
        composeRule.onNodeWithText("Copy full working").assertIsDisplayed()
        composeRule.onNodeWithText("Verification", substring = true).assertIsDisplayed()
    }

    @Test
    fun topLevelHistoryAndCatalogueControlsAreRemoved() {
        openSolver()
        listOf("History", "Calculators", "Learning", "Clear").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
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
