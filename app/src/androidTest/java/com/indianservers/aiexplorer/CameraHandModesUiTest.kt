package com.indianservers.aiexplorer

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class CameraHandModesUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun independentCameraIsDefaultAndCanReturnAfterArSelection() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName, Manifest.permission.CAMERA)
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Open AR Space").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("• Camera + Hand gestures").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("• Camera + Hand gestures").assertIsDisplayed()
        compose.onNodeWithContentDescription("Camera preview without AR").assertExists()
        compose.onNodeWithText("Camera + Hand gestures + AR").assertIsDisplayed().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Camera preview without AR").fetchSemanticsNodes().isEmpty() }
        // Allow the delayed camera handoff to attempt AR startup before switching back.
        Thread.sleep(1_500)
        compose.onNodeWithText("Camera + Hand gestures").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Camera preview without AR").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("• Camera + Hand gestures").assertIsDisplayed()
    }
}
