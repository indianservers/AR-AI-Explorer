package com.indianservers.aiexplorer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.indianservers.aiexplorer.curriculum.interaction.InteractiveActivityScreen
import org.junit.Rule
import org.junit.Test

class InteractiveReferenceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mathematicsTriangleSupportsExploreAndConstructionChallenge() {
        compose.setContent { InteractiveActivityScreen("math-triangle-lab") }
        compose.onNodeWithText("Triangle Invariant Lab").assertIsDisplayed()
        compose.onNodeWithText("Explore").performClick()
        compose.onNodeWithText("Isosceles").performClick()
        compose.onNodeWithText("angles:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Challenge").performClick()
        compose.onNodeWithText("Place the apex over the midpoint.").assertIsDisplayed()
    }

    @Test fun physicsMotionConnectsControlsGraphPredictionAndChallenge() {
        compose.setContent { InteractiveActivityScreen("physics-motion-graphs") }
        compose.onNodeWithText("Motion Graph Studio").assertIsDisplayed()
        compose.onNodeWithText("Braking").performClick()
        compose.onNodeWithText("At 6.0s:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Challenge").performClick()
        compose.onNodeWithText("Make final velocity zero").assertIsDisplayed()
        compose.onNodeWithText("Prediction").assertIsDisplayed()
    }

    @Test fun chemistryGasLawChangesPistonStateAndOffersTargets() {
        compose.setContent { InteractiveActivityScreen("chemistry-gas-laws") }
        compose.onNodeWithText("Ideal Gas Piston Lab").assertIsDisplayed()
        compose.onNodeWithText("Compressed").performClick()
        compose.onNodeWithText("P=", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Challenge").performClick()
        compose.onNodeWithText("Reach about 2 atm").assertIsDisplayed()
    }

    @Test fun biologyCellSelectsOrganelleTogglesLabelsAndChallenges() {
        compose.setContent { InteractiveActivityScreen("biology-cell-explorer") }
        compose.onNodeWithText("Layered Cell Structure Explorer").assertIsDisplayed()
        compose.onNodeWithText("Animal cell").performClick()
        compose.onNodeWithText("animal cell").assertIsDisplayed()
        compose.onNodeWithText("Challenge").performClick()
        compose.onNodeWithText("Show a plant chloroplast").assertIsDisplayed()
        compose.onNodeWithText("Text alternative:", substring = true).assertIsDisplayed()
    }
}
