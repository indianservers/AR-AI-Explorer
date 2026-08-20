package com.indianservers.aiexplorer

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
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
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Rule
import org.junit.Test

class SolverPhase1UiTest {
    private data class LiveCase(val id: String, val input: String, val expected: String)
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyboardInputSolvesAndShowsTraceableVerification() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("3x + 5 = 20")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("x = 5")
        scrollUntilTextDisplayed("x = 5")
        composeRule.onNodeWithText("Show steps").performClick()
        scrollUntilTextDisplayed("Division property of equality")
        scrollUntilTextDisplayed("Verification | Verified")
    }

    @Test
    fun parseErrorAndUnsupportedInputAreExplicit() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("2+")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("Unsupported or incomplete")
        composeRule.onNodeWithText("Unsupported or incomplete").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Position", substring = true).assertIsDisplayed()
        scrollToInput()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true).performTextReplacement("sin(x)")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("Phase 1 does not solve function(s): sin")
        composeRule.onNodeWithText("Phase 1 does not solve function(s): sin", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editorIsImmediateAndLegacyTopActionsAreAbsent() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true).assertIsDisplayed()
        listOf("Learning", "Keyboard input").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test
    fun screenExposesOfflineKeyboardAndAccessibilitySemanticsForLongInput() {
        openSolver()
        composeRule.onNodeWithContentDescription(
            "Offline Solver with editor-first input and direct answers",
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) * 2")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("110")
        scrollUntilTextDisplayed("110")
    }

    @Test
    fun inputAndCaretStateSurviveActivityRecreation() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("7x + 1 = 22")
        collapseKeyboard()
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("x = 3")
        scrollUntilTextDisplayed("x = 3")
    }

    @Test
    fun answerAndStepExposeSpokenStateBeyondColour() {
        openSolver()
        composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
            .performTextReplacement("3x + 5 = 20")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("x = 5")
        scrollUntilTextDisplayed("x = 5")
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Verified answer x = 5"),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show steps").performClick()
        composeRule.onNode(
            SemanticsMatcher("Step has spoken position and mathematical transformation") { node ->
                runCatching { node.config[SemanticsProperties.StateDescription] }.getOrNull()?.startsWith("Step 1 of") == true &&
                    runCatching { node.config[SemanticsProperties.ContentDescription] }.getOrNull()?.joinToString()?.contains("Before") == true
            },
        ).assertIsDisplayed()
    }

    @Test
    fun twentyFiveUndoAndRedoOperationsRestoreEditorState() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        (1..25).forEach { length -> input.performTextReplacement("x".repeat(length)) }
        collapseKeyboard()

        repeat(25) {
            composeRule.onNodeWithText("Undo").performScrollTo().performClick()
        }
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))

        repeat(25) {
            composeRule.onNodeWithText("Redo").performScrollTo().performClick()
        }
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("x".repeat(25))))
    }

    @Test
    fun clearAllCancelConfirmAndUndoRestoreWholeSession() {
        openSolver()
        val input = composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
        input.performTextReplacement("3x + 5 = 20")
        collapseKeyboard()
        composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
        waitForText("x = 5")

        composeRule.onNodeWithText("Clear All").performScrollTo().performClick()
        composeRule.onNodeWithText("Clear the entire Solver session?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        scrollUntilTextDisplayed("x = 5")

        composeRule.onNodeWithText("Clear All").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Confirm Clear All").performClick()
        scrollToInput()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        composeRule.onAllNodesWithText("x = 5").assertCountEquals(0)

        composeRule.onNodeWithText("Undo").performScrollTo().performClick()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("3x + 5 = 20")))
        scrollUntilTextDisplayed("x = 5")
    }

    @Test
    fun sixtyNumberedArithmeticFractionAndLinearCasesPassThroughLiveUi() {
        openSolver()
        val cases = buildList {
            (1..30).forEach { index ->
                add(LiveCase("P1-AR-${index.toString().padStart(3, '0')}", "$index + ${index + 1}", (index * 2 + 1).toString()))
            }
            (1..15).forEach { index ->
                val denominator = index * 2 + 1
                add(LiveCase("P1-FR-${index.toString().padStart(3, '0')}", "1/$denominator + 1/$denominator", "2/$denominator"))
            }
            (1..15).forEach { index ->
                add(LiveCase("P1-LIN-${index.toString().padStart(3, '0')}", "2x + $index = ${index * 3}", "x = $index"))
            }
        }
        cases.forEach { case ->
            scrollToInput()
            composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true)
                .performTextReplacement(case.input)
            collapseKeyboard()
            composeRule.onNodeWithText("Solve step by step", substring = true).performScrollTo().performClick()
            waitForAnswer(case.expected)
            composeRule.onNodeWithText("Clear All").performScrollTo().performClick()
            composeRule.onNodeWithContentDescription("Confirm Clear All").performClick()
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
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun waitForAnswer(answer: String) {
        val matcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Verified answer $answer")
        composeRule.waitUntil(15_000) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        repeat(12) {
            val displayed = runCatching {
                composeRule.onNode(matcher).assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (displayed) return
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
        composeRule.onNode(matcher).assertIsDisplayed()
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

    private fun scrollUntilTextDisplayed(text: String, maxSwipes: Int = 12) {
        repeat(maxSwipes) {
            val displayed = runCatching {
                composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (displayed) return
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeUp() }
        }
        composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    private fun scrollToInput() {
        repeat(8) {
            val displayed = runCatching {
                composeRule.onNodeWithContentDescription("Editable Solver expression", substring = true).assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (displayed) return
            composeRule.onNodeWithContentDescription("Offline Solver with editor-first input and direct answers")
                .performTouchInput { swipeDown() }
        }
    }
}
