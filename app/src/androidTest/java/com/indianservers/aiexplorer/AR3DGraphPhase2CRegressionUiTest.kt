package com.indianservers.aiexplorer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Test-only audit of the protected normal 3D workspace. No AR API is used here. */
class AR3DGraphPhase2CRegressionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun oneHundredTwentyOriginalGraph3dUiOperationsRemainStableWithoutCameraOrAr() {
        compose.waitUntil(15_000) {
            runCatching { compose.onAllNodesWithText("QUICK EXPLORE").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.onNodeWithContentDescription("Open 3D Graph").performClick()
        compose.waitUntil(10_000) {
            runCatching {
                compose.onAllNodesWithText("+ Equation").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }

        val equations = listOf(
            "z=x+y", "z=x^2-y^2", "z=sin(x)+cos(y)", "z=exp(-x^2-y^2)", "z=x+2*y",
            "z=2*x-3*y", "z=sqrt(x^2+y^2)", "z=(x^2-y^2)/(x^2+y^2+1)",
            "x^2+y^2+z^2=4", "x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)",
        )
        val operations = mutableListOf<String>()
        repeat(2) {
          equations.forEach { equation ->
            compose.onNodeWithContentDescription("Add a 3D graph equation to the workspace").performClick()
            operations += "add equation"
            compose.onNodeWithContentDescription("Editable 3D surface").performTextReplacement(equation)
            operations += "enter $equation"
            compose.onNodeWithText("Plot").performClick()
            operations += "plot"
            compose.onNodeWithText("Layers").performClick()
            operations += "open layers"
            val clear = compose.onAllNodesWithText("Clear all")
            clear[clear.fetchSemanticsNodes().lastIndex].performClick()
            operations += "clear"
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            operations += "close properties"
          }
        }

        assertTrue(operations.size >= 100)
        compose.onNodeWithContentDescription("Add a 3D graph equation to the workspace").assertIsDisplayed()
    }
}
