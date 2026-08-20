package com.indianservers.aiexplorer

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.core.ExpressionEngine
import java.io.File
import java.util.Locale
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SolverCasUiGoldenDatasetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val arguments = InstrumentationRegistry.getArguments()
    private val expressions = ExpressionEngine()

    @Test
    fun committedGoldenDatasetRunsThroughActualSolverUi() {
        val cycle = arguments.getString("solverCycle")?.toIntOrNull() ?: 1
        val start = arguments.getString("solverStart")?.toIntOrNull()?.coerceIn(1, 1_200) ?: 1
        val end = arguments.getString("solverEnd")?.toIntOrNull()?.coerceIn(start, 1_200) ?: 1_200
        val cases = readDataset()
        check(cases.size == 1_200) { "Expected 1,200 asset cases, found ${cases.size}" }
        val selected = cases.subList(start - 1, end)
        val outputRoot = File(instrumentation.targetContext.getExternalFilesDir(null), "solver-cas-ui").apply { mkdirs() }
        val evidenceRoot = File(outputRoot, "cycle-$cycle-evidence").apply { mkdirs() }
        val resultFile = File(outputRoot, "SOLVER_CAS_UI_GOLDEN_RESULTS_cycle${cycle}_${start}_${end}.csv")
        resultFile.writeText(CSV_HEADER + "\n")
        writeDeviceMetadata(outputRoot, cycle, start, end)

        openSolver()
        if (start == 1) {
            validateSessionControls(cycle, evidenceRoot)
        } else {
            clearWholeSessionAtBatchStart()
        }

        val failures = mutableListOf<String>()
        val capturedCategories = mutableSetOf<String>()
        selected.forEachIndexed { offset, case ->
            val ordinal = start + offset
            val started = System.nanoTime()
            var actual = ""
            var verification = "not_rendered"
            var stepsStatus = "not_checked"
            var warningStatus = "none"
            var screenshot = ""
            var passed = false
            var detail = ""
            try {
                enterAndSolve(case.input)
                if (case.expectedSupported) {
                    actual = waitForAnswer()
                    val answerMatches = compare(case, actual)
                    // SolverScreen intentionally scrolls to the new result 200 ms after a solve.
                    // Advance past that transition so it cannot consume the Steps tap.
                    composeRule.mainClock.advanceTimeBy(300)
                    composeRule.waitForIdle()
                    composeRule.onNodeWithText("Steps", useUnmergedTree = true).performScrollTo().performClick()
                    composeRule.waitForIdle()
                    val stepNodes = waitForSteps()
                    val stepDescriptions = stepNodes.mapNotNull { node ->
                        runCatching { node.config[SemanticsProperties.ContentDescription] }.getOrNull()?.joinToString(" ")
                    }
                    val stepShapeValid = stepDescriptions.isNotEmpty() && stepDescriptions.all { "Before" in it && "After" in it }
                    val finalAgrees = stepDescriptions.lastOrNull()?.let { compact(it).contains(compact(actual)) } ?: false
                    stepsStatus = when {
                        !stepShapeValid -> "missing_or_malformed"
                        !finalAgrees -> "final_step_mismatch"
                        else -> "visible_${stepDescriptions.size}_steps_final_agrees"
                    }
                    warningStatus = if (case.expectedWarnings == "none") {
                        "none_expected"
                    } else if (composeRule.onAllNodesWithTag("solver.warnings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                        "visible"
                    } else {
                        "expected_but_missing"
                    }
                    composeRule.onNodeWithText("Verify", useUnmergedTree = true).performScrollTo().performClick()
                    verification = waitForTaggedText("solver.verification")
                    passed = answerMatches && stepShapeValid && finalAgrees &&
                        verification.contains("Verified") && warningStatus != "expected_but_missing"
                    detail = "answerMatches=$answerMatches; stepShape=$stepShapeValid; finalAgrees=$finalAgrees"
                } else {
                    val error = waitForTaggedText("solver.error")
                    val failClosed = waitForTaggedText("solver.fail_closed")
                    val answerNodes = composeRule.onAllNodesWithTag("solver.answer", useUnmergedTree = true).fetchSemanticsNodes()
                    actual = "NO_RESULT"
                    verification = "NotApplicable"
                    stepsStatus = if (findSteps().isEmpty()) "no_steps" else "fabricated_steps"
                    warningStatus = "${case.expectedErrorClassification}: $error"
                    passed = answerNodes.isEmpty() && stepsStatus == "no_steps" && failClosed.contains("No solution was invented")
                    detail = "errorVisible=${error.isNotBlank()}; failClosedVisible=${failClosed.isNotBlank()}"
                }

                if (passed && capturedCategories.add(case.category)) {
                    screenshot = capture(evidenceRoot, "cycle${cycle}_${case.id}_PASS.png")
                }
                if (!passed) {
                    screenshot = capture(evidenceRoot, "cycle${cycle}_${case.id}_FAIL.png")
                    failures += "${case.id}: $detail; actual=$actual; verification=$verification; steps=$stepsStatus; warning=$warningStatus"
                }
            } catch (error: Throwable) {
                detail = "${error::class.java.simpleName}: ${error.message}"
                screenshot = capture(evidenceRoot, "cycle${cycle}_${case.id}_FAIL.png")
                failures += "${case.id}: $detail"
            } finally {
                val durationMs = (System.nanoTime() - started) / 1_000_000
                appendResult(
                    resultFile,
                    listOf(
                        cycle.toString(), case.id, case.category, case.expected, actual,
                        verification, stepsStatus, warningStatus, durationMs.toString(),
                        if (passed) "PASS" else "FAIL", screenshot, detail,
                    ),
                )
                if (!passed) resetForNextCase()
            }
            if (ordinal % 100 == 0) {
                instrumentation.sendStatus(0, android.os.Bundle().apply {
                    putString("stream", "UI golden cycle $cycle progress: $ordinal/1200; failures=${failures.size}\n")
                })
            }
        }

        assertTrue(
            "${failures.size} UI golden cases failed in cycle $cycle [$start,$end]:\n${failures.take(80).joinToString("\n")}",
            failures.isEmpty(),
        )
    }

    private fun validateSessionControls(cycle: Int, evidenceRoot: File) {
        enterAndSolve("3x+5=20")
        check(waitForAnswer() == "x = 5")
        composeRule.onNodeWithTag("solver.solve", useUnmergedTree = true).performScrollTo().performClick()
        check(waitForAnswer() == "x = 5")

        composeRule.onNodeWithTag("solver.clear_input", useUnmergedTree = true).performScrollTo().performClick()
        waitForNoResult()
        composeRule.onNodeWithTag("solver.undo", useUnmergedTree = true).performScrollTo().performClick()
        check(waitForAnswer() == "x = 5")
        composeRule.onNodeWithTag("solver.redo", useUnmergedTree = true).performScrollTo().performClick()
        waitForNoResult()

        enterAndSolve("2+3")
        check(waitForAnswer() == "5")
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        check(waitForAnswer() == "5")
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
        check(waitForAnswer() == "5")
        capture(evidenceRoot, "cycle${cycle}_session_rotation_PASS.png")

        composeRule.onNodeWithTag("solver.clear_all", useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Confirm Clear All").performClick()
        waitForNoResult()
    }

    private fun clearWholeSessionAtBatchStart() {
        composeRule.onNodeWithTag("solver.clear_all", useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Confirm Clear All").performClick()
        waitForNoResult()
    }

    private fun enterAndSolve(input: String) {
        val field = composeRule.onNodeWithTag("solver.input", useUnmergedTree = true)
        field.performScrollTo().performTextReplacement(input)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("solver.answer", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithTag("solver.error", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        collapseKeyboardIfVisible()
        val retained = runCatching { field.fetchSemanticsNode().config[SemanticsProperties.EditableText] }.getOrNull()?.text.orEmpty()
        check(retained.isNotBlank()) {
            "UI input became blank after submitting '$input'"
        }
        composeRule.onNodeWithTag("solver.solve", useUnmergedTree = true).performScrollTo().performClick()
    }

    private fun resetForNextCase() {
        runCatching {
            val clear = composeRule.onAllNodesWithTag("solver.clear_input", useUnmergedTree = true)
            if (clear.fetchSemanticsNodes().isNotEmpty()) clear[0].performScrollTo().performClick()
        }
        runCatching {
            composeRule.onNodeWithTag("solver.input", useUnmergedTree = true).performScrollTo().performTextReplacement("")
        }
        collapseKeyboardIfVisible()
        runCatching { waitForNoResult() }
    }

    private fun collapseKeyboardIfVisible() {
        val nodes = composeRule.onAllNodesWithContentDescription("Collapse math keyboard", substring = true)
        if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes[0].performClick()
        composeRule.waitForIdle()
    }

    private fun waitForAnswer(): String {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("solver.answer", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        return waitForTaggedText("solver.answer")
    }

    private fun waitForTaggedText(tag: String): String {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        val config = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config
        return runCatching { config[SemanticsProperties.Text] }.getOrNull()?.joinToString("") { it.text }
            ?: runCatching { config[SemanticsProperties.StateDescription] }.getOrNull()
            ?: runCatching { config[SemanticsProperties.ContentDescription] }.getOrNull()?.joinToString(" ")
            ?: ""
    }

    private fun waitForSteps() = composeRule.run {
        waitUntil(15_000) { findSteps().isNotEmpty() }
        findSteps()
    }

    private fun findSteps() = composeRule.onAllNodes(STEP_MATCHER, useUnmergedTree = true).fetchSemanticsNodes()

    private fun waitForNoResult() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("solver.answer", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithTag("solver.error", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        val editable = composeRule.onNodeWithTag("solver.input", useUnmergedTree = true).fetchSemanticsNode()
            .config.let { runCatching { it[SemanticsProperties.EditableText] }.getOrNull() } ?: AnnotatedString("")
        check(editable.text.isEmpty()) { "Reset left input '${editable.text}'" }
    }

    private fun compare(case: GoldenCase, actual: String): Boolean = when (case.comparison) {
        "exact_compact" -> compact(actual) == compact(case.expected)
        "symbolic_numeric" -> numericallyEquivalent(actual, case.expected, false)
        "antiderivative_numeric" -> numericallyEquivalent(actual, case.expected, true)
        "numeric_tolerance" -> runCatching {
            abs(actual.toDouble() - case.approximate.toDouble()) <= case.tolerance
        }.getOrDefault(false)
        else -> false
    }

    private fun numericallyEquivalent(actual: String, expected: String, removeConstant: Boolean): Boolean = runCatching {
        fun prepared(value: String): String {
            val compact = compact(value)
            return if (removeConstant) compact.removeSuffix("+C").removeSuffix("-C") else compact
        }
        val left = expressions.compile(prepared(actual))
        val right = expressions.compile(prepared(expected))
        listOf(-3.0, -1.25, -0.5, 0.0, 0.75, 2.0, 4.0).all { x ->
            val a = left.eval(mapOf("x" to x))
            val b = right.eval(mapOf("x" to x))
            a.isFinite() && b.isFinite() && abs(a - b) <= 1e-9 * maxOf(1.0, abs(a), abs(b))
        }
    }.getOrDefault(false)

    private fun openSolver() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("solver.input", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("Open Offline Solver", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithTag("solver.input", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            composeRule.onNodeWithContentDescription("Open Offline Solver", substring = true).performClick()
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("solver.input", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(directory: File, name: String): String = runCatching {
        val target = File(directory, name)
        target.outputStream().use { stream ->
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        target.absolutePath
    }.getOrDefault("")

    private fun readDataset(): List<GoldenCase> = instrumentation.context.assets
        .open("SOLVER_CAS_GOLDEN_DATASET.jsonl").bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).mapIndexed { index, line -> parse(index + 1, line) }.toList()
        }

    private fun parse(lineNumber: Int, line: String): GoldenCase {
        val fields = FIELD.findAll(line).associate { it.groupValues[1] to unescape(it.groupValues[2]) }
        check(REQUIRED.all { !fields[it].isNullOrBlank() }) { "Line $lineNumber has incomplete metadata" }
        return GoldenCase(
            id = fields.getValue("case_id"),
            category = fields.getValue("category"),
            input = fields.getValue("input"),
            expected = fields.getValue("expected_exact_result"),
            approximate = fields.getValue("expected_approximate_result"),
            tolerance = fields.getValue("tolerance").toDouble(),
            expectedWarnings = fields.getValue("expected_warnings"),
            expectedErrorClassification = fields.getValue("expected_error_classification"),
            expectedSupported = fields.getValue("expected_supported").toBooleanStrict(),
            comparison = fields.getValue("comparison"),
        )
    }

    private fun writeDeviceMetadata(root: File, cycle: Int, start: Int, end: Int) {
        val context = instrumentation.targetContext
        val metrics = context.resources.displayMetrics
        val configuration = context.resources.configuration
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        File(root, "cycle${cycle}_${start}_${end}_device.txt").writeText(
            "manufacturer=${Build.MANUFACTURER}\nmodel=${Build.MODEL}\ndevice=${Build.DEVICE}\n" +
                "api=${Build.VERSION.SDK_INT}\nbuild=${Build.DISPLAY}\nappVersion=${packageInfo.versionName}\n" +
                "appVersionCode=${packageInfo.longVersionCode}\norientation=${configuration.orientation}\n" +
                "screenPx=${metrics.widthPixels}x${metrics.heightPixels}\ndensityDpi=${metrics.densityDpi}\n",
        )
    }

    private fun appendResult(file: File, values: List<String>) {
        file.appendText(values.joinToString(",") { csv(it) } + "\n")
    }

    private fun compact(value: String) = value.replace(Regex("\\s+"), "")
    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
    private fun unescape(value: String): String {
        val decoded = UNICODE.replace(value) { it.groupValues[1].toInt(16).toChar().toString() }
        return decoded.replace("\\\"", "\"").replace("\\\\", "\\")
    }

    private data class GoldenCase(
        val id: String,
        val category: String,
        val input: String,
        val expected: String,
        val approximate: String,
        val tolerance: Double,
        val expectedWarnings: String,
        val expectedErrorClassification: String,
        val expectedSupported: Boolean,
        val comparison: String,
    )

    private companion object {
        const val CSV_HEADER = "cycle,case_id,category,expected_result,actual_rendered_result,verification,steps_status,warning_error_status,duration_ms,status,screenshot_evidence,detail"
        val FIELD = Regex("\\\"([a-z_]+)\\\":\\\"((?:\\\\.|[^\\\"])*)\\\"")
        val UNICODE = Regex("\\\\u([0-9a-fA-F]{4})")
        val STEP_MATCHER = SemanticsMatcher("Solver visible step") {
            runCatching { it.config[SemanticsProperties.TestTag] }.getOrNull()?.startsWith("solver.step.") == true
        }
        val REQUIRED = listOf(
            "case_id", "category", "subcategory", "difficulty", "input", "normalized_input",
            "expected_exact_result", "expected_approximate_result", "tolerance", "expected_solution_set",
            "assumptions", "domain_constraints", "units", "angle_mode", "expected_warnings",
            "expected_error_classification", "expected_step_summary", "expected_verification",
            "independent_derivation", "tags", "regression_id", "expected_supported", "comparison",
        )
    }
}
