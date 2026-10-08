package com.indianservers.aiexplorer.gamifymaths

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isToggleable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class MobileGamesLayoutTest {
    @get:Rule val compose = createComposeRule()

    private data class Case(val name: String, val render: @Composable () -> Unit)

    @Test fun everyGameAndSpecializedLevelFitsPhoneWithoutScroll() {
        val cases = buildList {
            listOf("speed-basic" to SpeedProblem("7 + 8", "15", "Addition"),
                "speed-advanced" to SpeedProblem("2x + 5 = 17; x = ?", "6", "Algebra")).forEach { (id, problem) ->
                add(Case("$id-0") { SpeedPlayScreen(id, GameBlue, 45, 45, problem, "", 0, 0, 0, null,
                    SpeedAnswerMode.MultipleChoice, {}, {}, {}, {}, {}) })
            }
            SpeedCalculationMode.entries.forEach { mode->
                (1..2).forEach { mastery->
                    val problem=speedMasteryProblem(mode,mastery,kotlin.random.Random(42L))
                    add(Case("speed-${mode.name.lowercase()}-mastery-$mastery") {
                        SpeedPlayScreen("Mastery sprint",GameBlue,45,45,problem,"",0,0,0,null,SpeedAnswerMode.MultipleChoice,{}, {}, {}, {}, {})
                    })
                }
            }
            gamifyGamesForAudit().filterNot { it.id.startsWith("speed-") }.forEach { game ->
                val levels = ((0 until if(game.id=="chance") 15 else 6).toList() + listOf(30,60,90,119,120,121)).distinct()
                levels.forEach { i -> add(Case("${game.id}-$i") {
                    GeneratedMathGameScreen(game, i, {}, {}, initiallyPlaying=true, initialSeed=42L)
                }) }
            }
        }
        var index by mutableIntStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalCompactGameLayout provides true) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(index) { cases[index].render() } }
            }
        }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mobile-games-audit").apply { mkdirs() }
        File(directory, "catalogue.csv").writeText("id,title\n" + gamifyCatalogueAudit().joinToString("\n") { "${it.id},${it.title}" })
        val rows = mutableListOf("game,scroll_dp")
        val excessive = mutableListOf<String>()
        cases.forEachIndexed { i, case ->
            compose.runOnIdle { index = i }
            compose.waitForIdle()
            val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            val scroll = nodes.maxOfOrNull { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() / it.layoutInfo.density.density } ?: 0f
            rows += "${case.name},$scroll"
            if (scroll > 1f) excessive += "${case.name}: $scroll dp"
            if (case.name.endsWith("-0") || case.name.endsWith("-120") || case.name.endsWith("-121")) {
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                File(directory, "${case.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        File(directory, "layout.csv").writeText(rows.joinToString("\n"))
        assertTrue("Excessive play scrolling: $excessive", excessive.isEmpty())
    }

    @Test fun shapeChoicesHaveNoNameLabelsOrWorkedAnswerHints() {
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { GeneratedMathGameScreen(gamifyGamesForAudit().first{it.id=="shapes"},0,{}, {},initiallyPlaying=true,initialSeed=42L) } }
        listOf("Square", "Circle", "Triangle", "Rectangle").forEach {
            assertEquals(0, compose.onAllNodesWithText(it).fetchSemanticsNodes().size)
        }
        compose.onNodeWithContentDescription("Show hint").performClick()
        assertEquals(0, compose.onAllNodesWithText("A square has its own defining sides, corners and curves.").fetchSemanticsNodes().size)
    }

    @Test fun menuHasTwoSquareCardsAndSettings() {
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { GamifyMathsRoot {} } }
        val first = compose.onNodeWithContentDescription("Open Calculation Sprint, current topic Rapid Arithmetic").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithContentDescription("Open Advanced Calculation Sprint, current topic Algebra & Trigonometry").fetchSemanticsNode().boundsInRoot
        assertEquals(first.width, first.height, 2f)
        assertEquals(second.width, second.height, 2f)
        assertEquals(first.top, second.top, 2f)
        assertTrue(second.left >= first.right)
        val home = compose.onRoot().captureToImage().asAndroidBitmap()
        val homeDir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mobile-games-audit").apply { mkdirs() }
        File(homeDir, "home.png").outputStream().use { home.compress(Bitmap.CompressFormat.PNG, 100, it) }
        home.recycle()
        compose.onNodeWithContentDescription("Game settings").performClick()
        compose.onNodeWithText("Compact play").assertIsDisplayed()
        compose.onNodeWithText("Previous best scores").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mobile-games-audit").apply { mkdirs() }
        File(dir, "settings.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun everyGameHasDistinctArtworkAndBothMasteryButtons() {
        assertEquals(29,gamifyGamesForAudit().map{gameCardArtwork(it.id)}.distinct().size)
        compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            GeneratedMathGameScreen(gamifyGamesForAudit().first{it.id=="chance"},0,{}, {})
        } }
        compose.onNodeWithText("Mastery I").assertIsDisplayed()
        compose.onNodeWithText("Mastery II").assertIsDisplayed()
    }

    @Test fun scoresAndSettingsSurviveReopening() {
        val preferences = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("maths_games", 0)
        val previousProgress = preferences.getInt("progress_times-table", 0)
        val previousBest = preferences.getInt("best_times-table", 0)
        val previousCompact = preferences.getBoolean("compact_play", true)
        preferences.edit().putInt("progress_times-table", 0).putInt("best_times-table", 0).putBoolean("compact_play", true).commit()
        try {
            var generation by mutableIntStateOf(0)
            compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { key(generation) { GamifyMathsRoot {} } } }
            val description = "Open Times Table Galaxy, current topic Multiplication Patterns, beginner difficulty"
            compose.onNodeWithContentDescription(description).performScrollTo().performClick()
            compose.onNodeWithText("Start playing").performClick()
            val prompt = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.Text] }.first { "rectangular star array" in it.text }.text
            val numbers = Regex("[0-9]+").findAll(prompt).map { it.value.toInt() }.toList()
            repeat(numbers[1]-1){compose.onNodeWithContentDescription("Increase rows by 1").performClick()}
            repeat(numbers[0]/numbers[1]-1){compose.onNodeWithContentDescription("Increase columns by 1").performClick()}
            compose.onNodeWithText("Launch array").performClick()
            compose.runOnIdle {
                assertEquals(1, preferences.getInt("best_times-table", 0))
                assertEquals(1, preferences.getInt("progress_times-table", 0))
                generation++
            }
            compose.onNodeWithText("BEST 1/122 MISSIONS").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Game settings").performScrollTo().performClick()
            compose.onNode(isToggleable()).performClick()
            compose.runOnIdle { generation++ }
            compose.onNodeWithContentDescription("Game settings").performClick()
            compose.onNode(isToggleable()).assertIsOff()
        } finally {
            preferences.edit().putInt("progress_times-table", previousProgress).putInt("best_times-table", previousBest).putBoolean("compact_play", previousCompact).commit()
        }
    }
}
