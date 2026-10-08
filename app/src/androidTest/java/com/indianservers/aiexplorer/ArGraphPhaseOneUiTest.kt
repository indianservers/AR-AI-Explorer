package com.indianservers.aiexplorer

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class ArGraphPhaseOneUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"ar-phase1-$name.png")
        java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    @Test fun graphToolbarAndModesWorkInRunningApp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,Manifest.permission.CAMERA)
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Open AR Space").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Expand AR Space menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Expand AR Space menu").performClick()
        compose.onNodeWithContentDescription("AR Space studio 3D Graph").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Expand graph toolbar").assertIsDisplayed().performClick()
        capture("expanded")
        compose.onNodeWithText("Equation").assertIsDisplayed()
        compose.onNodeWithText("Move").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hand gestures on").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Hand gestures off").assertIsDisplayed()
        compose.onNodeWithText("More").performClick()
        capture("tools")
        compose.onNodeWithText("Touch",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Gesture",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Hybrid",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Scene explorer").performScrollTo().performClick()
        compose.onNodeWithText("Scene explorer").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Compact graph toolbar").performClick()
        compose.onNodeWithText("Equation").assertDoesNotExist()
        capture("compact")
        compose.onNodeWithContentDescription("Expand graph toolbar").performTouchInput {
            swipe(center, Offset(center.x,center.y-180f),300)
        }
        compose.onNodeWithContentDescription("Reveal graph toolbar").assertIsDisplayed()
        capture("immersive")
        compose.onNodeWithContentDescription("Reveal graph toolbar").performClick()
        compose.onNodeWithContentDescription("Expand graph toolbar").assertIsDisplayed()
    }
}
