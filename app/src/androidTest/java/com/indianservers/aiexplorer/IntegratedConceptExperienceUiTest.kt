package com.indianservers.aiexplorer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.indianservers.aiexplorer.curriculum.experience.InteractiveConceptExperienceScreen
import org.junit.Rule
import org.junit.Test

class IntegratedConceptExperienceUiTest {
 @get:Rule val compose=createComposeRule()
 private fun verify(conceptId:String,title:String,misconception:String){compose.setContent{InteractiveConceptExperienceScreen(conceptId)};compose.onNodeWithText(title).assertIsDisplayed();compose.onNodeWithText("Explain").performScrollTo().assertIsDisplayed();compose.onNodeWithText("Tap to highlight").performScrollTo().performClick();compose.onNodeWithText("Misconception test").performScrollTo().assertIsDisplayed();compose.onNodeWithText(misconception,substring=true).assertExists();compose.onNodeWithText("What is happening?").performScrollTo().assertIsDisplayed();compose.onNodeWithText("Practice:",substring=true).performScrollTo().assertIsDisplayed()}
 @Test fun mathematicsCoordinatesTextVisualPredictionAndPractice(){verify("math-triangle-lab","Triangle Invariant Lab","wider-looking")}
 @Test fun physicsCoordinatesTextVisualPredictionAndPractice(){verify("physics-wave-speed","Wave Speed Explorer","particles travel forward")}
 @Test fun chemistryCoordinatesTextVisualPredictionAndPractice(){verify("chemistry-gas-laws","Ideal Gas Piston Lab","particles themselves expand")}
 @Test fun biologyCoordinatesTextVisualPredictionAndPractice(){verify("biology-mendelian-genetics","Mendelian Cross Explorer","dominant allele")}
}
