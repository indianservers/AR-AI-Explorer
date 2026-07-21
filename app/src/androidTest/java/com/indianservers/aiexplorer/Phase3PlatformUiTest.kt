package com.indianservers.aiexplorer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.indianservers.aiexplorer.phase3.ui.Phase3PlatformWorkspace
import org.junit.Rule
import org.junit.Test

class Phase3PlatformUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun assessment_result_has_spoken_state_semantics() {
        compose.setContent { MaterialTheme { Phase3PlatformWorkspace(onExit = {}) } }
        compose.onNodeWithText("Score steps locally").performClick()
        compose.onNodeWithContentDescription("Assessment 4.0 of 4.0; first invalid none").assertIsDisplayed()
    }

    @Test fun exam_teacher_and_delivery_surfaces_are_reachable() {
        compose.setContent { MaterialTheme { Phase3PlatformWorkspace(onExit = {}) } }
        compose.onNodeWithText("Exam").performClick()
        compose.onNodeWithText("CBSE examination simulator").assertIsDisplayed()
        compose.onNodeWithText("Teacher").performClick()
        compose.onNodeWithText("Teacher classroom").assertIsDisplayed()
        compose.onNodeWithText("Delivery").performClick()
        compose.onNodeWithText("Offline-first").assertIsDisplayed()
    }
}
