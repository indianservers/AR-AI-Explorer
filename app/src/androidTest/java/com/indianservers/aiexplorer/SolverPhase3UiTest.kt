package com.indianservers.aiexplorer

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
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("AI Maths Explorer", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithContentDescription("Editable Solver expression", substring = true)
                .fetchSemanticsNodes().isEmpty()
        ) {
            composeRule.onNodeWithText("AI Maths Explorer", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Menu").performClick()
            composeRule.onNodeWithText("Solver").performClick()
        }
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement(expression)
        composeRule.onNodeWithText("Solve").performClick()
        composeRule.onNodeWithText("Show steps").performClick()
    }
}
