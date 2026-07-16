package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AIExplorerUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openMaths() {
        composeRule.onNodeWithText("Maths").performClick()
    }

    @Test
    fun appLaunchesWithSubjectHubAndPlannedSciences() {
        composeRule.onNodeWithText("AI Explorer").assertIsDisplayed()
        composeRule.onNodeWithText("Maths").assertIsDisplayed()
        composeRule.onNodeWithText("Physics").assertIsDisplayed()
        composeRule.onNodeWithText("Chemistry").assertIsDisplayed()
        composeRule.onNodeWithText("Biology").assertIsDisplayed()
        composeRule.onNodeWithText("Astro Physics").assertIsDisplayed()
        composeRule.onNodeWithText("IQ Labs").assertIsDisplayed()
    }

    @Test
    fun mathsOpensCurrentExplorerAndResponsiveNavigation() {
        openMaths()
        composeRule.onNodeWithText("2D").assertIsDisplayed()
        composeRule.onNodeWithText("3D").assertIsDisplayed()
        composeRule.onNodeWithText("Graph").assertIsDisplayed()
        composeRule.onNodeWithText("G3D").assertIsDisplayed()
        composeRule.onNodeWithText("Trig").assertIsDisplayed()
        composeRule.onNodeWithText("AR").assertIsDisplayed()
    }

    @Test
    fun mathematicsMenuListsFutureSubmenus() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Formulas").assertIsDisplayed()
        composeRule.onNodeWithText("MCQs").assertIsDisplayed()
        composeRule.onNodeWithText("Visualize Formulas").assertIsDisplayed()
        composeRule.onNodeWithText("Theorems").assertIsDisplayed()
        composeRule.onNodeWithText("Visual Proofs").assertIsDisplayed()
        composeRule.onNodeWithText("Maths Dictionary").assertIsDisplayed()
        composeRule.onNodeWithText("Probability & Statistics").assertIsDisplayed()
    }

    @Test
    fun graphCanvasHasAccessibleDescription() {
        openMaths()
        composeRule.onNodeWithContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            .assertIsDisplayed()
    }

    @Test
    fun threeDimensionalWorkspaceExposesDirectTransformModes() {
        openMaths()
        composeRule.onNodeWithText("3D").performClick()
        composeRule.onNodeWithText("Move").assertIsDisplayed()
        composeRule.onNodeWithText("Rotate").assertIsDisplayed()
        composeRule.onNodeWithText("Scale").assertIsDisplayed()
        composeRule.onNodeWithText("Vertex").assertIsDisplayed()
        composeRule.onNodeWithText("Edge").assertIsDisplayed()
        composeRule.onNodeWithText("Face").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Interactive 3D workspace with object, vertex, edge and face selection")
            .assertIsDisplayed()
    }

    @Test
    fun geometryWorkspaceExposesDynamicConstructionTools() {
        openMaths()
        composeRule.onNodeWithText("2D").performClick()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Midpoint").assertIsDisplayed()
        composeRule.onNodeWithText("Circumcenter").assertIsDisplayed()
        composeRule.onNodeWithText("Perpendicular").assertIsDisplayed()
    }

    @Test
    fun graphWorkspaceExposesAdvancedDefinitionPresets() {
        openMaths()
        composeRule.onNodeWithText("Graph").performClick()
        composeRule.onNodeWithText("Fx").performClick()
        composeRule.onNodeWithText("Piecewise").assertIsDisplayed()
        composeRule.onNodeWithText("Polar").assertIsDisplayed()
        composeRule.onNodeWithText("Parametric").assertIsDisplayed()
        composeRule.onNodeWithText("Implicit").assertIsDisplayed()
    }

    @Test
    fun learningCoachExposesLearnerAndTeacherFlows() {
        openMaths()
        composeRule.onNodeWithText("⋮").performClick()
        composeRule.onNodeWithText("Learn").performClick()
        composeRule.onNodeWithText("Learner", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Teacher", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Check").assertIsDisplayed()
        composeRule.onNodeWithText("Hint").assertIsDisplayed()
        composeRule.onNodeWithText("Package").assertIsDisplayed()
    }

    @Test
    fun spatialModeProvidesArcoreAndAccessibleFallback() {
        openMaths()
        composeRule.onNodeWithText("AR").performClick()
        composeRule.onNodeWithText("AR Spatial Lab").assertIsDisplayed()
        composeRule.onNodeWithText("Enable ARCore").assertIsDisplayed()
        composeRule.onNodeWithText("Place", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("AR spatial mathematics preview with direct move, rotate and scale gestures")
            .assertIsDisplayed()
    }

    @Test
    fun threeDimensionalGraphExposesTouchCameraAndPlaneViews() {
        openMaths()
        composeRule.onNodeWithText("G3D").performClick()
        composeRule.onNodeWithText("X").assertIsDisplayed()
        composeRule.onNodeWithText("Y").assertIsDisplayed()
        composeRule.onNodeWithText("Z").assertIsDisplayed()
        composeRule.onNodeWithText("XY").assertIsDisplayed()
        composeRule.onNodeWithText("XZ").assertIsDisplayed()
        composeRule.onNodeWithText("YZ").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Interactive 3D graph: drag to orbit, two fingers pan, pinch zoom, twist roll, and tap axis or plane views")
            .assertIsDisplayed()
    }
}
