package com.indianservers.aiexplorer.gamifymaths

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LightMathGamesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureTenGamesAndTheirAdvancedBoards() {
        val games = gamifyGamesForAudit().filter { it.id in firstLightMathGameIds }
        assertEquals(10, games.size)
        val cases = games.flatMap { game -> if (game.id.startsWith("speed-")) listOf(game to 0) else listOf(game to 0, game to 120, game to 121) }
        var index by mutableIntStateOf(0)
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) {
            val (game, level) = cases[index]
            if (game.id.startsWith("speed-")) {
                val problem = if (game.id == "speed-basic") SpeedProblem("4 − 4", "0", "Subtraction") else SpeedProblem("10tan(−315°)", "10", "Trigonometry")
                SpeedPlayScreen(game.title, game.accent, 45, 45, problem, "", 0, 0, 0, null, SpeedAnswerMode.Typed, {}, {}, {}, {}, {})
            } else GeneratedMathGameScreen(game, level, {}, {}, initiallyPlaying = true, initialSeed = 42L)
        } } }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "light-maths-games").apply { mkdirs() }
        val rows = mutableListOf("game,level,scroll_dp")
        cases.forEachIndexed { i, (game, level) ->
            compose.runOnIdle { index = i }; compose.waitForIdle()
            val scroll = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
                .maxOfOrNull { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() / it.layoutInfo.density.density } ?: 0f
            rows += "${game.title},$level,$scroll"
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(directory, "${game.title}${if (level == 0) "" else " - Mastery ${level - 119}"}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            compose.onNodeWithContentDescription("Construction drop zone").assertIsDisplayed()
        }
        File(directory, "layout.csv").writeText(rows.joinToString("\n"))
    }

    private fun drag(description: String, valid: Boolean = true) {
        val source = compose.onNodeWithContentDescription(description).performScrollTo()
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithContentDescription("Construction drop zone").fetchSemanticsNode().boundsInRoot
        source.performTouchInput { swipe(center, if (valid) target.center - sourceBounds.topLeft else center + Offset(0f, 100f), 450) }
    }

    @Test fun expressionUsesRealDropBoundsAndRunDoesNotAwardPoints() {
        var completions = 0
        val game = gamifyGamesForAudit().first { it.id == "decimal-harbor" }
        val source = GeneratedRound("Decimal Cargo", "Add 7.6 + 6.6.", 14.2, "Add both quantities.", "Align decimals.", emptyList(), operands = listOf(7.6, 6.6))
        val round = WorkshopChallenge(WorkshopKind.Expression, source.topic, source.prompt, source.hint, source.explanation, source,
            numbers = listOf(7.6, 6.6, 0.0, 1.0, 2.0))
        val state = StateRestorationTester(compose)
        state.setContent { Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MathWorkshopBoard(game, round, false) { completions++ }
        } }
        drag("Place number 7.6", valid = false)
        compose.onNodeWithContentDescription("Remove token 1").assertDoesNotExist()
        drag("Place number 7.6")
        compose.onNodeWithContentDescription("Place operation +").performClick()
        drag("Place number 6.6")
        state.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Remove token 3").assertIsDisplayed()
        compose.onNodeWithText("Run").performClick()
        compose.onNodeWithText("=  14.2").assertIsDisplayed()
        assertEquals(0, completions)
        compose.onNodeWithText("Check").performClick()
        assertEquals(1, completions)
    }

    @Test fun fractionsAndRobotsAcceptDraggingInBothDirections() {
        var index by mutableIntStateOf(0)
        var completions = 0
        val cases = listOf("fractions", "combinatorics", "balance")
        compose.setContent { Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { key(index) {
            val id = cases[index]
            val game = gamifyGamesForAudit().first { it.id == id }
            val round = generateWorkshopChallenge(id, 0, 0, 42L, if (id == "combinatorics") 0 else -1)
            MathWorkshopBoard(game, round, false) { completions++ }
        } } }
        val fraction = generateWorkshopChallenge("fractions", 0, 0, 42L)
        fraction.witness.forEach { drag("Add $it/${fraction.parameters[0]} core") }
        compose.onNodeWithText("Check build").performClick()
        assertEquals(1, completions)
        compose.runOnIdle { index = 1 }
        val robot = generateWorkshopChallenge("combinatorics", 0, 0, 42L, 0)
        robot.witness.forEach { formation ->
            formation.split(',').forEach { drag("Choose robot ${('A'.code + it.toInt()).toChar()}") }
            compose.onNodeWithText("Collect").performClick()
        }
        compose.onNodeWithText("Check").performClick()
        assertEquals(2, completions)
        compose.runOnIdle { index = 2 }
        val balance = generateWorkshopChallenge("balance", 0, 0, 42L)
        balance.witness.chunked(2).forEach { (op, amount) ->
            val available = balance.numbers.take(4).distinct()
            if (amount.toDouble() in available) compose.onNodeWithContentDescription("Select core $amount").performClick()
            else {
                val start = available.first().toInt(); compose.onNodeWithContentDescription("Select core $start").performClick()
                repeat(kotlin.math.abs(amount.toInt() - start)) { compose.onNodeWithText(if (amount.toInt() > start) "+1" else "−1").performClick() }
            }
            drag("Apply $op to both sides")
        }
        compose.onNodeWithText("Unlock").performClick()
        assertEquals(3, completions)
    }
}
