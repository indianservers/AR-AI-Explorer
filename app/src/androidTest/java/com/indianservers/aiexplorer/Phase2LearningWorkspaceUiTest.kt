package com.indianservers.aiexplorer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.indianservers.aiexplorer.learningworkspace.ui.Phase2LearningWorkspace
import org.junit.Rule
import org.junit.Test

class Phase2LearningWorkspaceUiTest {
    @get:Rule val compose = createComposeRule()
    private fun launch(){compose.setContent{MaterialTheme{Phase2LearningWorkspace{}}}}

    @Test fun localTutorRespondsAndShowsGrounding(){launch();compose.onNodeWithContentDescription("Ask the local Socratic tutor").performTextInput("Help me solve this");compose.onNodeWithText("Ask locally").performClick();compose.waitUntil(3_000){compose.onAllNodesWithText("Sources:",substring=true).fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("LOCALLY AUTHORED",substring=true).assertExists()}
    @Test fun cameraTranscriptionIsEditableAndConfirmedLocally(){launch();compose.onNodeWithText("Input").performClick();compose.onNodeWithText("Editable recognised text").performTextReplacement("electric circuit current voltage resistance");compose.onNodeWithText("Confirm local transcription").performClick();compose.onNodeWithText("Mapped locally",substring=true).assertExists();compose.onNodeWithText("PHYSICS",substring=true).assertExists()}
    @Test fun voiceCommandIsValidatedBeforeApplying(){launch();compose.onNodeWithText("Input").performClick();compose.onNodeWithText("Validate voice command").performClick();compose.onNodeWithText("Validated SET_VALUE",substring=true).assertExists()}
    @Test fun simulationChangeUpdatesFormulaAndExplanation(){launch();compose.onNodeWithText("Experiment").performClick();compose.onNodeWithText("PV = nRT",substring=true).assertExists();compose.onNodeWithText("Ideal gas workspace").assertExists()}
    @Test fun uncertaintyWorkspaceReportsAnalysis(){launch();compose.onNodeWithText("Analyse").performClick();compose.onNodeWithText("Mean:",substring=true).assertExists();compose.onNodeWithText("never deleted",substring=true).assertExists()}
    @Test fun settingsMakeCloudOptionalAndConsentGranular(){launch();compose.onNodeWithText("Settings").performClick();compose.onNodeWithText("Cloud AI is optional",substring=true).assertExists();compose.onNodeWithText("Camera crop may be sent").assertExists();compose.onNodeWithText("API key").assertExists()}
}
