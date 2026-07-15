package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test
    fun threeDimensionalWorkspaceExposesDirectTransformModes() {
        composeRule.onNodeWithText("3D").performClick()
        composeRule.onNodeWithText("Move").assertIsDisplayed()
        composeRule.onNodeWithText("Rotate").assertIsDisplayed()
        composeRule.onNodeWithText("Scale").assertIsDisplayed()
        composeRule.onNodeWithText("Vertex").assertIsDisplayed()
        composeRule.onNodeWithText("Edge").assertIsDisplayed()
        composeRule.onNodeWithText("Face").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Interactive 3D workspace with object, vertex, edge and face selection")
            .assertIsDisplayed()
    }

    @Test
    fun geometryWorkspaceExposesDynamicConstructionTools() {
        composeRule.onNodeWithText("2D").performClick()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Midpoint").assertIsDisplayed()
        composeRule.onNodeWithText("Circumcenter").assertIsDisplayed()
        composeRule.onNodeWithText("Perpendicular").assertIsDisplayed()
    }

    @Test
    fun graphWorkspaceExposesAdvancedDefinitionPresets() {
        composeRule.onNodeWithText("Graph").performClick()
        composeRule.onNodeWithText("Fx").performClick()
        composeRule.onNodeWithText("Piecewise").assertIsDisplayed()
        composeRule.onNodeWithText("Polar").assertIsDisplayed()
        composeRule.onNodeWithText("Parametric").assertIsDisplayed()
        composeRule.onNodeWithText("Implicit").assertIsDisplayed()
    }
}
