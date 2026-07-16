package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class AIExplorerUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openMaths() {
        composeRule.onNodeWithText("Maths").performClick()
    }

    private fun expandNavigation() {
        composeRule.onNodeWithContentDescription("Show navigation").performClick()
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
        expandNavigation()
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
        composeRule.onNodeWithText("Problem Solver").assertIsDisplayed()
        composeRule.onNodeWithText("Formulas").assertIsDisplayed()
        composeRule.onNodeWithText("MCQs").assertIsDisplayed()
        composeRule.onNodeWithText("Visualize Formulas").assertIsDisplayed()
        composeRule.onNodeWithText("Theorems").assertIsDisplayed()
        composeRule.onNodeWithText("Visual Proofs").assertIsDisplayed()
        composeRule.onNodeWithText("Maths Dictionary").assertIsDisplayed()
        composeRule.onNodeWithText("Probability & Statistics").assertIsDisplayed()
    }

    @Test
    fun problemSolverShowsVerifiedStepByStepAnswer() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Problem Solver").performClick()
        composeRule.onNodeWithText("Solve step by step").performClick()
        composeRule.onNodeWithText("x = 4").assertIsDisplayed()
        composeRule.onNodeWithText("Verification").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Step by step maths solution").assertIsDisplayed()
    }

    @Test
    fun problemSolverExposesSmartMathsEditor() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Problem Solver").performClick()
        composeRule.onNodeWithText("Hide editor").assertIsDisplayed()
        composeRule.onNodeWithText("sin(").assertIsDisplayed()
        composeRule.onNodeWithText("d/dx").assertIsDisplayed()
        composeRule.onNodeWithText("Syntax structure looks balanced.").assertIsDisplayed()
    }

    @Test
    fun problemSolverRunsExactCasCommands() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Problem Solver").performClick()
        composeRule.onNodeWithContentDescription("Maths question input").performTextReplacement("Calculate 1/3 + 1/6")
        composeRule.onNodeWithText("Solve step by step").performClick()
        composeRule.onNodeWithText("1/2").assertIsDisplayed()
        composeRule.onNodeWithText("Exact arithmetic").assertIsDisplayed()
    }

    @Test
    fun problemSolverRunsInequalityWithImprovedSummary() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Problem Solver").performClick()
        composeRule.onNodeWithText("Inequality").performClick()
        composeRule.onNodeWithText("2 <= x <= 3").assertIsDisplayed()
        composeRule.onNodeWithText("Inequality").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Solver confidence 98 percent").assertIsDisplayed()
    }

    @Test
    fun notebookCreatesReactiveNamedMathsCells() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Math Notebook").performClick()
        composeRule.onNodeWithContentDescription("Notebook maths input").performTextReplacement("a := 2")
        composeRule.onNodeWithText("Run cell").performClick()
        composeRule.onNodeWithText("#1 · Scalar · a").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reactive maths notebook cells").assertIsDisplayed()
    }

    @Test
    fun probabilityMenuOpensInteractiveDistributionFoundation() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Probability & Statistics").performClick()
        composeRule.onNodeWithText("Normal distribution").assertIsDisplayed()
        composeRule.onNodeWithText("Median (Q50)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Interactive probability distribution plot").assertIsDisplayed()
    }

    @Test
    fun mathsKnowledgeHubOpensFormulaAndMcqLibraries() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Formulas").performClick()
        composeRule.onNodeWithText("Knowledge Intelligence").assertIsDisplayed()
        composeRule.onNodeWithText("Quadratic roots").assertIsDisplayed()
        composeRule.onNodeWithText("MCQs").performClick()
        composeRule.onNodeWithText("Interactive MCQ Quiz").assertIsDisplayed()
        composeRule.onNodeWithText("Start 15-question quiz").performClick()
        composeRule.onNodeWithText("If b^2 - 4ac is negative, a real quadratic has:").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Interactive MCQ quiz score 0 of 15").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Maths knowledge content").assertIsDisplayed()
    }

    @Test
    fun statisticsLabProvidesInteractiveChartsAndDescriptiveMeasures() {
        openMaths()
        composeRule.onNodeWithText("Menu").performClick()
        composeRule.onNodeWithText("Probability & Statistics").performClick()
        composeRule.onNodeWithText("Statistics").performClick()
        composeRule.onNodeWithText("Histogram").assertIsDisplayed()
        composeRule.onNodeWithText("Mean").assertIsDisplayed()
        composeRule.onNodeWithText("Median").assertIsDisplayed()
        composeRule.onNodeWithText("Mode").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Statistics dataset editor").assertIsDisplayed()
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
        expandNavigation()
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
        expandNavigation()
        composeRule.onNodeWithText("2D").performClick()
        composeRule.onNodeWithText("Open tools").performClick()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Midpoint").assertIsDisplayed()
        composeRule.onNodeWithText("Circumcenter").assertIsDisplayed()
        composeRule.onNodeWithText("Perpendicular").assertIsDisplayed()
    }

    @Test
    fun graphWorkspaceExposesAdvancedDefinitionPresets() {
        openMaths()
        expandNavigation()
        composeRule.onNodeWithText("Graph").performClick()
        composeRule.onNodeWithText("Open tools").performClick()
        composeRule.onNodeWithText("Fx").performClick()
        composeRule.onNodeWithText("Piecewise").assertIsDisplayed()
        composeRule.onNodeWithText("Polar").assertIsDisplayed()
        composeRule.onNodeWithText("Parametric").assertIsDisplayed()
        composeRule.onNodeWithText("Implicit").assertIsDisplayed()
    }

    @Test
    fun graphToolsStayOffCanvasUntilMovableLauncherOpensThem() {
        openMaths()
        expandNavigation()
        composeRule.onNodeWithText("Graph").performClick()
        composeRule.onNodeWithContentDescription("Movable Open tools launcher. Tap to expand and drag to move").assertIsDisplayed()
        composeRule.onNodeWithText("Open tools").performClick()
        composeRule.onNodeWithText("Trace").performClick()
        composeRule.onNodeWithText("Tangent").assertIsDisplayed()
        composeRule.onNodeWithText("Probability").assertIsDisplayed()
    }

    @Test
    fun learningCoachExposesLearnerAndTeacherFlows() {
        openMaths()
        composeRule.onNodeWithText("⋮").performClick()
        composeRule.onNodeWithContentDescription("Expand quick actions").performClick()
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
        expandNavigation()
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
        expandNavigation()
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
