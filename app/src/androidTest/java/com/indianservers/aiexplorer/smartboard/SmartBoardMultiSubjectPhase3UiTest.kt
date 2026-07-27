package com.indianservers.aiexplorer.smartboard

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.indianservers.aiexplorer.MainActivity
import org.junit.Rule
import org.junit.Test

class SmartBoardMultiSubjectPhase3UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun oneUnifiedTutorPanelExposesCoreModes() {
        compose.onNodeWithText("Smart Board · Draw, select and recognize").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Smart Board Tutor").performClick()
        compose.onNodeWithText("Smart Board Tutor").assertExists()
        compose.onNodeWithText("Ask").assertExists()
        compose.onNodeWithText("Hint").assertExists()
        compose.onNodeWithText("Next step").assertExists()
        compose.onNodeWithText("Check my work").assertExists()
        compose.onNodeWithText("Find my mistake").assertExists()
    }

    @Test
    fun tutorDoesNotReplaceBoardWithoutUserInsert() {
        compose.onNodeWithText("Smart Board · Draw, select and recognize").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Smart Board Tutor").performClick()
        compose.onNodeWithText("Insert into Board").assertExists()
        compose.onNodeWithText("Send").assertExists()
        compose.onNodeWithText("Clear thread").assertExists()
    }
}
