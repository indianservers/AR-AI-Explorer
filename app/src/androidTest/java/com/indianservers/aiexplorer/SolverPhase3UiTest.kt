package com.indianservers.aiexplorer

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

class SolverPhase3UiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calculusResultExposesAccessibleStepLinkedVisualControls() {
        solve("differentiate x^3")
        composeRule.onNodeWithText("Visual explanation").assertIsDisplayed()
        composeRule.onNodeWithText("Secants approach the tangent").assertIsDisplayed()
        composeRule.onNodeWithText("Previous").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun tappingAStepSynchronizesTheVisualState() {
        solve("differentiate x^3")
        composeRule.onNodeWithContentDescription("Select step 1", substring = true).performClick()
        composeRule.onNodeWithText("What changed in step 1").assertIsDisplayed()
    }

    @Test
    fun formulaUnderstandingUsesProgressiveDisclosure() {
        solve("differentiate x^3")
        composeRule.onNodeWithText("Why this formula works").performClick()
        composeRule.onNodeWithText("Why the derivative power rule works").assertIsDisplayed()
        composeRule.onNodeWithText("Conditions").assertIsDisplayed()
        composeRule.onNodeWithText("Verification:", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullScreenVisualHasTextAlternativeAndCloseAction() {
        solve("complex 3+4i")
        composeRule.onNodeWithText("Full screen").performClick()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Argand plane", substring = true).assertIsDisplayed()
    }

    private fun solve(expression: String) {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("Open Offline Solver", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isEmpty()
        ) {
            composeRule.onNodeWithContentDescription("Open Offline Solver", substring = true).performClick()
        }
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement(expression)
        composeRule.onNodeWithContentDescription("Collapse math keyboard").performClick()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Show steps", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        repeat(3) {
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
        composeRule.onNodeWithText("Show steps").performClick()
        repeat(5) {
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
    }
}
