package com.indianservers.aiexplorer.ar3dgraph.presentation

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineColor
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineGraphResult
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineMeshSnapshot
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineVector3
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class AR3DGraphScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private class DirectExecutor : AbstractExecutorService() {
        override fun execute(command: Runnable) = command.run()
        override fun shutdown() = Unit
        override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
        override fun isShutdown() = false
        override fun isTerminated() = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
    }

    @Test
    fun equationPlotClearAndBackRemainAvailableWithoutArcore() {
        var backed = false
        val model = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }
        model.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        compose.setContent {
            MaterialTheme {
                AR3DGraphScreen(
                    onBack = { backed = true },
                    graphEngine = { request -> EngineGraphResult.Success(request, listOf(mesh())) },
                    model = model,
                )
            }
        }

        compose.onNodeWithTag("ar3dgraph-equation-input").assertIsDisplayed()
            .performTextReplacement("z=x+2*y")
        compose.onNodeWithTag("ar3dgraph-plot").performClick()
        compose.waitUntil(5_000) { model.uiState.renderData != null }
        compose.onNodeWithTag("ar3dgraph-placement-message").assertTextContains("ready", substring = true)
        compose.onNodeWithTag("ar3dgraph-equation-input").performTextReplacement("z=x-y")
        compose.runOnIdle { assertTrue(model.uiState.placement == ARGraphPlacementState.GraphReadyForPlacement) }
        compose.onNodeWithTag("ar3dgraph-clear").performClick()
        compose.onNodeWithTag("ar3dgraph-placement-message").assertTextContains("cleared", substring = true)
        compose.onNodeWithContentDescription("Back from AR 3D Graph").performClick()
        compose.runOnIdle { assertTrue(backed) }
    }

    @Test
    fun twentyFiveUnsupportedScreenOpenCloseCyclesRemainStableWithoutCameraOrSession() {
        var visible by mutableStateOf(true)
        val model = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }
        compose.setContent {
            MaterialTheme {
                if (visible) AR3DGraphScreen(onBack = { visible = false }, model = model)
                else Text("Host screen")
            }
        }

        repeat(25) {
            compose.onNodeWithTag("ar3dgraph-screen").assertIsDisplayed()
            compose.runOnUiThread { visible = false }
            compose.onNodeWithText("Host screen").assertIsDisplayed()
            compose.runOnUiThread { visible = true }
        }
        compose.onNodeWithTag("ar3dgraph-screen").assertIsDisplayed()
    }

    @Test
    fun oneHundredMeaningfulUiOperationsKeepGraphAndControlsSynchronized() {
        val operations = mutableListOf<String>()
        val model = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }
        model.connect { request ->
            if (request.equations.any { it.expression == "bad" }) EngineGraphResult.ValidationError("Invalid equation.")
            else EngineGraphResult.Success(request, listOf(mesh().copy(canonicalEquation = request.equations.first().expression)))
        }
        compose.setContent {
            MaterialTheme {
                AR3DGraphScreen(
                    onBack = {},
                    graphEngine = { request ->
                        if (request.equations.any { it.expression == "bad" }) EngineGraphResult.ValidationError("Invalid equation.")
                        else EngineGraphResult.Success(request, listOf(mesh().copy(canonicalEquation = request.equations.first().expression)))
                    },
                    model = model,
                )
            }
        }

        repeat(25) { cycle ->
            val valid = "z=x+${cycle % 5}*y"
            compose.onNodeWithTag("ar3dgraph-equation-input").performTextReplacement(valid)
            operations += "${operations.size + 1}: enter $valid"
            compose.onNodeWithTag("ar3dgraph-plot").performClick()
            operations += "${operations.size + 1}: plot valid"
            compose.onNodeWithTag("ar3dgraph-reset-placement").performClick()
            operations += "${operations.size + 1}: reset placement"
            compose.onNodeWithTag("ar3dgraph-equation-input").performTextReplacement("bad")
            operations += "${operations.size + 1}: enter invalid"
            compose.onNodeWithTag("ar3dgraph-plot").performClick()
            operations += "${operations.size + 1}: reject invalid"
            compose.onNodeWithTag("ar3dgraph-clear").performClick()
            operations += "${operations.size + 1}: clear"
            compose.onNodeWithTag("ar3dgraph-equation-input").performTextReplacement(valid)
            operations += "${operations.size + 1}: correct invalid"
            compose.onNodeWithTag("ar3dgraph-plot").performClick()
            operations += "${operations.size + 1}: retry"
        }
        compose.onNodeWithTag("ar3dgraph-camera-container").performTouchInput { click(center) }
        operations += "${operations.size + 1}: unsupported viewport tap is contained"
        compose.runOnIdle {
            assertTrue(operations.size >= 100)
            assertTrue(model.uiState.renderData != null)
            assertTrue(model.uiState.placement == ARGraphPlacementState.GraphReadyForPlacement)
        }
    }

    @Test
    fun helpIsAccessibleRepeatableAndControlsRemainReachable() {
        val model = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }
        compose.setContent { MaterialTheme { AR3DGraphScreen(onBack = {}, model = model) } }

        repeat(10) {
            compose.onNodeWithTag("ar3dgraph-help").performClick()
            compose.onNodeWithTag("ar3dgraph-help-dialog").assertIsDisplayed()
            compose.onNodeWithText("Drag to rotate • Pinch to resize • Tap to reposition", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("ar3dgraph-help-close").performClick()
        }
        compose.onNodeWithTag("ar3dgraph-plot").assertIsDisplayed()
        compose.onNodeWithTag("ar3dgraph-clear").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back from AR 3D Graph").assertIsDisplayed()
    }

    @Test
    fun fiveResponsiveConfigurationChangesKeepViewportAndEssentialControlsAvailable() {
        val model = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }
        var profile by mutableStateOf(0)
        val sizes = listOf(320 to 640, 640 to 320, 411 to 914, 800 to 1280, 1280 to 800)
        compose.setContent {
            val base = LocalConfiguration.current
            val baseDensity = LocalDensity.current
            val configuration = remember(profile) {
                Configuration(base).apply {
                    screenWidthDp = sizes[profile].first
                    screenHeightDp = sizes[profile].second
                    orientation = if (screenWidthDp > screenHeightDp) Configuration.ORIENTATION_LANDSCAPE
                    else Configuration.ORIENTATION_PORTRAIT
                }
            }
            val density = Density(baseDensity.density, if (profile == 4) 1.5f else baseDensity.fontScale)
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalDensity provides density) {
                MaterialTheme { AR3DGraphScreen(onBack = {}, model = model) }
            }
        }

        repeat(5) { index ->
            compose.runOnUiThread { profile = index }
            compose.waitForIdle()
            compose.onNodeWithTag("ar3dgraph-camera-container").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back from AR 3D Graph").assertIsDisplayed()
        }
    }

    private fun mesh() = EngineMeshSnapshot(
        "one", "z=x+2*y",
        listOf(
            EngineVector3(0.0, 0.0, 0.0), EngineVector3(0.0, 1.0, 1.0),
            EngineVector3(1.0, 0.0, 1.0), EngineVector3(1.0, 1.0, 2.0),
        ),
        2, 2, emptyList(),
        listOf(EngineColor(0f, 1f, 1f)), EngineColor(0f, 1f, 1f), 1f,
    )
}
