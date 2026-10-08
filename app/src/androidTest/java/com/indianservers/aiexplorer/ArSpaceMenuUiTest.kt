package com.indianservers.aiexplorer

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class ArSpaceMenuUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun menuCollapsesAndEveryAvailableStudioStaysInArSpace() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName, Manifest.permission.CAMERA)
        compose.waitUntil(30_000) {
            compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Open AR Space").performScrollTo().performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithContentDescription("Expand AR Space menu").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("AR Space studio 2D Geometry").assertDoesNotExist()
        compose.onNodeWithContentDescription("Expand AR Space menu").performClick()
        for (studio in listOf("2D Geometry", "3D Geometry", "2D Graph", "3D Graph", "Coordinate Plane", "Vector Lab", "CAS Objects")) {
            compose.onNodeWithContentDescription("AR Space studio $studio").performScrollTo().performClick()
            if (studio == "3D Graph") {
                compose.onNodeWithContentDescription("Expand graph toolbar").performClick()
                compose.onNodeWithText("More").performClick()
                compose.onNodeWithText("Coordinate Plane").performScrollTo().performClick()
            }
            compose.onNodeWithText("AR Space").assertExists()
        }
        compose.onNodeWithContentDescription("Collapse AR Space menu").performScrollTo().performClick()
        compose.onNodeWithContentDescription("AR Space studio 2D Geometry").assertDoesNotExist()
        compose.onNodeWithContentDescription("Expand AR Space menu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hide AR Space controls").performClick()
        compose.onNodeWithContentDescription("Show AR controls").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Expand AR Space menu").assertIsDisplayed()
    }
}
