package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AIExplorerUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndShowsPrimaryNavigation() {
        composeRule.onNodeWithText("AI Explorer").assertIsDisplayed()
        composeRule.onNodeWithText("2D").assertIsDisplayed()
        composeRule.onNodeWithText("3D").assertIsDisplayed()
        composeRule.onNodeWithText("Graph").assertIsDisplayed()
        composeRule.onNodeWithText("3D Graph").assertIsDisplayed()
    }

    @Test
    fun graphCanvasHasAccessibleDescription() {
        composeRule.onNodeWithContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            .assertIsDisplayed()
    }
}

