package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossWorkspaceIntegrationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sharedLauncherGeometryEntriesNavigateToRealWorkspaces() {
        awaitHome()
        composeRule.onNodeWithContentDescription("Open 2D").performClick()
        awaitDescription("Breadcrumb Maths", "2D")
        composeRule.onNodeWithContentDescription("Open Maths menu").performClick()
        composeRule.onAllNodesWithText("3D Geometry")[0].performClick()
        awaitDescription("Interactive 3D workspace")
        composeRule.onNodeWithContentDescription("Interactive 3D workspace", substring = true).assertIsDisplayed()
    }

    @Test
    fun twentyCompleteFiveWorkspaceNavigationCyclesRemainStable() {
        awaitHome()
        repeat(20) {
            openAndReturn("Open 2D", "Breadcrumb Maths", "2D")
            openAndReturn("Open 3D", "Interactive 3D workspace")
            openAndReturn("Open Graphs", "Breadcrumb Maths", "Graph")
            openAndReturn("Open 3D Graph", "Breadcrumb Maths", "3D Graph")
            openAndReturn("Open Solver", "Offline Solver with editor-first input and direct answers")
        }
    }

    @Test
    fun graph3dAcceptsImplicitAndParametricSurfacesInOneRunningSession() {
        awaitHome()
        composeRule.onNodeWithContentDescription("Open 3D Graph").performClick()
        awaitDescription("Breadcrumb Maths", "3D Graph")

        composeRule.onNodeWithContentDescription("Add a 3D graph equation to the workspace").performClick()
        composeRule.onNodeWithContentDescription("Editable 3D surface").performTextReplacement("x^2+y^2+z^2=4")
        composeRule.onNodeWithText("Plot").performClick()
        composeRule.waitUntil(20_000) { composeRule.onAllNodesWithText("Implicit surface").fetchSemanticsNodes().isNotEmpty() || composeRule.onAllNodesWithText("+ Equation").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithContentDescription("Add a 3D graph equation to the workspace").performClick()
        composeRule.onNodeWithContentDescription("Editable 3D surface").performTextReplacement("x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)")
        composeRule.onNodeWithContentDescription("Editable 3D surface").performImeAction()
        composeRule.onNodeWithText("Layers").performClick()
        composeRule.onNodeWithText("Implicit surface").fetchSemanticsNode()
        composeRule.onNodeWithText("Parametric surface").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Search 3D surface layers").performTextReplacement("cos(u)")
        composeRule.onNodeWithText("Parametric surface").assertIsDisplayed()
    }

    @Test
    fun graph3dAddsTheNormalExplicitSurfaceFlow() {
        awaitHome()
        composeRule.onNodeWithContentDescription("Open 3D Graph").performClick()
        awaitDescription("Breadcrumb Maths", "3D Graph")

        composeRule.onNodeWithContentDescription("Add a 3D graph equation to the workspace").performClick()
        composeRule.onNodeWithContentDescription("Editable 3D surface").performTextReplacement("z=sin(x)+cos(y)")
        composeRule.onNodeWithContentDescription("Editable 3D surface").performImeAction()
        composeRule.onNodeWithText("Layers").performClick()
        composeRule.onNodeWithContentDescription("Search 3D surface layers").performTextReplacement("sin(x)")
        composeRule.onNodeWithText("Explicit surface").assertIsDisplayed()
    }

    @Test
    fun graph2dEnterPlotsRepeatedAddsAndKeepsEveryEarlierGraph() {
        awaitHome()
        composeRule.onNodeWithContentDescription("Open Graphs").performClick()
        awaitDescription("Breadcrumb Maths", "Graph")

        listOf("sin(x)", "cos(x)", "x^3-2*x").forEach { expression ->
            composeRule.onNodeWithContentDescription("Add a graph equation to the workspace").performClick()
            composeRule.onNodeWithContentDescription("Editable", substring = true).performTextReplacement(expression)
            composeRule.onNodeWithContentDescription("Editable", substring = true).performImeAction()
            composeRule.onNodeWithText("Collapse ▲").performClick()
        }

        composeRule.onNodeWithText("Equations (5)").performClick()
        assertTrue(composeRule.onAllNodesWithText("h(x)", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("i(x)", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("j(x)", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    private fun openAndReturn(openDescription: String, expectedDescription: String, expectedPart: String? = null) {
        composeRule.onNodeWithContentDescription(openDescription).performClick()
        awaitDescription(expectedDescription, expectedPart)
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitHome()
    }

    private fun awaitHome() {
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onAllNodesWithText("QUICK EXPLORE").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        composeRule.onNodeWithContentDescription("Mathematics Explorer menu").assertIsDisplayed()
    }

    private fun awaitDescription(prefix: String, requiredPart: String? = null) {
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onAllNodes(
                    androidx.compose.ui.test.SemanticsMatcher("description starts with $prefix") { node ->
                        val descriptions = runCatching {
                            node.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription]
                        }.getOrNull().orEmpty()
                        descriptions.any { description ->
                            description.startsWith(prefix) && (requiredPart == null || requiredPart in description)
                        }
                    },
                ).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
}
