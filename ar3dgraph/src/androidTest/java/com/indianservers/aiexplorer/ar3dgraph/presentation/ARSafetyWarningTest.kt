package com.indianservers.aiexplorer.ar3dgraph.presentation

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ARSafetyWarningTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun warningBlocksArContentUntilAcknowledged() {
        compose.setContent {
            MaterialTheme {
                if (!awaitARSafetyAcknowledgement("test", {})) Text("AR content")
            }
        }
        compose.onNodeWithText("AR safety warning").assertIsDisplayed()
        compose.onNodeWithText("Parental supervision is important.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Be aware of your surroundings", substring = true).assertIsDisplayed()
        compose.onNodeWithText("AR content").assertDoesNotExist()
        compose.onNodeWithText("Continue safely").performClick()
        compose.onNodeWithText("AR content").assertIsDisplayed()
        compose.onNodeWithText("AR safety warning").assertDoesNotExist()
    }

    @Test
    fun exitDoesNotAcceptWarningOrComposeArContent() {
        var exited = false
        compose.setContent {
            MaterialTheme {
                if (!awaitARSafetyAcknowledgement("test", { exited = true })) Text("AR content")
            }
        }
        compose.onNodeWithText("Exit AR").performClick()
        compose.runOnIdle { assertTrue(exited) }
        compose.onNodeWithText("AR content").assertDoesNotExist()
    }
}
