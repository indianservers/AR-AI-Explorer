package com.indianservers.aiexplorer

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class VisualProofMockupLessonUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allSixtyNineMockupRoutesRenderAndExposeAnInteraction() {
        val engine = VisualProofEngine()
        val missingInteractions = mutableListOf<String>()
        val screenshotDirectory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "visual-proof-audit",
        ).apply { mkdirs() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val persistentScreenshotDirectory = "/sdcard/Download/AIExplorerVisualProofAudit"
        AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("mkdir -p $persistentScreenshotDirectory"),
        ).use { it.readBytes() }
        var playback by mutableStateOf(engine.start(VisualProofCatalog.labs.first().id))
        compose.setContent {
            VisualProofMockupLessonScreen(
                playback = playback,
                onBack = {},
                onTogglePlaying = { playback = engine.togglePlaying(playback) },
                onReset = { playback = engine.reset(playback) },
                onSeekStep = { step -> playback = playback.copy(frame = playback.frame.copy(step = step)) },
                onParameterChange = { name, value -> playback = engine.setParameter(playback, name, value) },
            )
        }

        VisualProofCatalog.labs.forEachIndexed { index, lab ->
            compose.runOnUiThread { playback = engine.start(lab.id) }
            compose.waitForIdle()
            compose.onNodeWithContentDescription(
                "Visual proof route ${lab.id}",
                useUnmergedTree = true,
            ).assertExists()
            Thread.sleep(250)
            val screenshotFile = File(screenshotDirectory, "%03d_final.png".format(index + 1))
            screenshotFile.outputStream().use { stream ->
                val raw = instrumentation.uiAutomation.takeScreenshot()
                val scaled = Bitmap.createScaledBitmap(raw, 540, raw.height * 540 / raw.width, true)
                scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
                scaled.recycle()
                raw.recycle()
            }
            AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "cp ${screenshotFile.absolutePath} $persistentScreenshotDirectory/${screenshotFile.name}",
                ),
            ).use { it.readBytes() }
            val clickActions = compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
            if (clickActions.size < 2) {
                missingInteractions += lab.id
            } else {
                compose.onNodeWithTag(
                    "visual-proof-route-action",
                    useUnmergedTree = true,
                ).performClick()
                compose.waitForIdle()
            }
        }
        assertTrue(
            "These routes must expose a control in addition to Back: ${missingInteractions.joinToString()}",
            missingInteractions.isEmpty(),
        )
    }

    @Test
    fun captureProofsSixtyAndSixtyTwoAfterCanvasDrawSettles() {
        val engine = VisualProofEngine()
        var playback by mutableStateOf(engine.start("nt-modular-addition"))
        compose.setContent {
            VisualProofMockupLessonScreen(
                playback = playback,
                onBack = {},
                onTogglePlaying = { playback = engine.togglePlaying(playback) },
                onReset = { playback = engine.reset(playback) },
                onSeekStep = { step -> playback = playback.copy(frame = playback.frame.copy(step = step)) },
                onParameterChange = { name, value -> playback = engine.setParameter(playback, name, value) },
            )
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputDirectory = "/sdcard/Download/AIExplorerVisualProofAudit"
        AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("mkdir -p $outputDirectory"),
        ).use { it.readBytes() }

        listOf(60 to "nt-modular-addition", 62 to "nt-negative-modulo").forEach { (number, id) ->
            compose.runOnUiThread { playback = engine.start(id) }
            compose.onNodeWithContentDescription(
                "Visual proof route $id",
                useUnmergedTree = true,
            ).assertExists()
            compose.waitForIdle()
            Thread.sleep(1_200)
            instrumentation.uiAutomation.takeScreenshot().recycle()
            Thread.sleep(250)
            val raw = instrumentation.uiAutomation.takeScreenshot()
            val scaled = Bitmap.createScaledBitmap(raw, 540, raw.height * 540 / raw.width, true)
            val localFile = File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                "%03d_final.png".format(number),
            )
            localFile.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            scaled.recycle()
            raw.recycle()
            AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "cp ${localFile.absolutePath} $outputDirectory/${localFile.name}",
                ),
            ).use { it.readBytes() }
        }
    }

}
