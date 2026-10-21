package com.indianservers.aiexplorer.gamifymaths

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class GameNavigationScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedFormationAndMasterySettingsControlTheLiveGame() {
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { GamifyMathsRoot {} } }
        compose.onNode(hasContentDescription("Open Combinatorics Command,", substring = true)).performScrollTo().performClick()
        compose.onNodeWithText("Ordered codes").performClick()
        compose.onNode(hasText("Start playing") or hasText("Continue at level ", substring = true)).performScrollTo().performClick()
        compose.onNode(hasText("Collect ALL ordered codes", substring = true)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Game session settings").performClick()
        compose.onNodeWithText("Ordered codes").assertIsSelected()
        compose.onNodeWithText("Mastery II").performClick()
        compose.onNodeWithText("Formation type").assertDoesNotExist()
        compose.onNodeWithText("Start Mastery II").performScrollTo().performClick()
        compose.onNode(hasText("ADVANCED MASTERY 2", substring = true)).assertIsDisplayed()
        compose.onNode(hasText("total energy", substring = true)).assertIsDisplayed()
    }

    @Test fun everySettingsPageFitsCompactPhoneAndHasGameSpecificControls() {
        compose.setContent {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { GamifyMathsRoot {} }
        }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "compact-game-settings").apply { mkdirs() }
        val overflow = mutableListOf<String>()
        val rows = mutableListOf("game,scroll_dp")
        gamifyGamesForAudit().forEach { game ->
            compose.onNode(hasContentDescription("Open ${game.title},", substring = true))
                .performScrollTo().performClick()
            compose.waitForIdle()
            if (!game.id.startsWith("speed-")) {
                compose.onNodeWithText(gameSetupProfile(game.id).hintDescription).assertIsDisplayed()
                compose.onNodeWithText("Mastery I").assertIsDisplayed()
            }
            val scroll = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
                .fetchSemanticsNodes().maxOfOrNull {
                    it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() / it.layoutInfo.density.density
                } ?: 0f
            rows += "${game.title},$scroll"
            if (scroll > 1f) overflow += "${game.title}: $scroll"
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(directory, "${game.title}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            compose.onNodeWithContentDescription("Back to games").performClick()
        }
        File(directory, "layout.csv").writeText(rows.joinToString("\n"))
        org.junit.Assert.assertTrue("Settings overflow: $overflow", overflow.isEmpty())
    }

    @Test fun everyGameOpensAndBothBackButtonsReturnToCatalogue() {
        var exits = 0
        compose.setContent {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { GamifyMathsRoot { exits++ } }
        }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "competition-game-screenshots").apply { mkdirs() }
        val games = gamifyGamesForAudit()
        val captureScreenshots = InstrumentationRegistry.getArguments().getString("skipScreenshots") != "true"
        fun open(game: MathsGame) {
            compose.onNode(hasContentDescription("Open ${game.title},", substring = true))
                .performScrollTo().performClick()
            compose.waitForIdle()
        }
        fun play(game: MathsGame) {
            compose.onNode(if (game.id.startsWith("speed-"))
                hasText("Start ", substring = true) and hasText(" Sprint", substring = true)
            else hasText("Start playing") or hasText("Continue at level ", substring = true))
                .performScrollTo().performClick()
            compose.waitForIdle()
        }
        games.forEach { game ->
            open(game)
            play(game)
            if (captureScreenshots) {
                if (InstrumentationRegistry.getArguments().getString("hostScreenshots") == "true") {
                    val token=java.util.UUID.randomUUID().toString()
                    File(directory,"capture-request.txt").writeText("$token|${game.title}.png")
                    compose.waitUntil(timeoutMillis=30_000){File(directory,"capture-ack.txt").let{it.exists() && it.readText().trim()==token}}
                } else {
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                File(directory, "${game.title}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
                }
            }
            compose.onNodeWithContentDescription(if (game.id.startsWith("speed-")) "Exit challenge" else "Back")
                .performClick()
            compose.onNodeWithText("Choose Your Mission").assertIsDisplayed()
            assertEquals("Toolbar back must keep ${game.title} inside the games module", 0, exits)
            open(game)
            play(game)
            pressBack()
            compose.waitForIdle()
            compose.onNodeWithText("Choose Your Mission").assertIsDisplayed()
            assertEquals("System back must keep ${game.title} inside the games module", 0, exits)
        }
        pressBack()
        compose.runOnIdle { assertEquals("Catalogue back must exit to app home", 1, exits) }
        if (captureScreenshots) {
            File(directory, "manifest.txt").writeText(games.joinToString("\n") { "${it.title}.png" })
            File(directory,"capture-done.txt").writeText("done")
        }
    }
}
