package com.indianservers.aiexplorer.smartboard

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.indianservers.aiexplorer.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SmartBoardMultiSubjectPhase1UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun openSubjectSettings() {
        compose.onNodeWithText("Smart Board · Draw, select and recognize").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Input & Display Settings").performClick()
        compose.onNodeWithText("Board subject").assertExists()
    }

    @Test
    fun oneSelectorOffersEveryPhaseOneSubject() {
        listOf("Auto Detect", "Mathematics", "Physics", "Chemistry", "English", "Biology", "General").forEach {
            compose.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun autoShapeSuggestionsUseTheExistingSettingsPanel() {
        compose.onNodeWithText("Auto-shape suggestions").assertExists()
        compose.onNodeWithText("Suggestions never replace ink until you accept them.").assertExists()
    }

    @Test
    fun subjectCanChangeWithoutLeavingTheCanvas() {
        compose.onNodeWithText("Chemistry").performClick()
        compose.onNodeWithText("Board mode changed to Chemistry; existing content was preserved").assertExists()
        compose.onNodeWithText("Biology").performClick()
        compose.onNodeWithText("Board mode changed to Biology; existing content was preserved").assertExists()
        compose.onNodeWithText("Board subject").assertExists()
    }

    @Test
    fun subjectLockCanBeEnabledAndDisabled() {
        compose.onNodeWithText("Lock Board subject").performClick()
        compose.onNodeWithText("Mathematics subject locked").assertExists()
        compose.onNodeWithText("Lock Board subject").performClick()
        compose.onNodeWithText("Subject lock removed").assertExists()
    }
}
