package com.indianservers.aiexplorer

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.core.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual visible-key tap/hold gestures in the full app; no paste, text replacement, or injected case expressions. */
class NewMathKeyboard300EmulatorTouchTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val reports get() = File(instrumentation.targetContext.filesDir, "new-keyboard-300").also { it.mkdirs() }
    private var page = "123"
    private fun touchDescription(description: String) {
        val nodes = compose.onAllNodesWithContentDescription(description, useUnmergedTree = true)
        nodes[nodes.fetchSemanticsNodes().lastIndex].performTouchInput { click() }
    }
    private fun touchTextIfPresent(text: String) {
        compose.waitForIdle()
        val nodes = compose.onAllNodesWithText(text)
        if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes[0].performTouchInput { click() }
    }
    private fun key(token: String, power: Boolean = false) {
        val nextPage = when {
            token == ";" || token.single().isLetter() && token !in listOf("x", "y") -> "abc"
            token in listOf("<", ">") -> "sym"
            else -> "123"
        }
        if (page != nextPage) {
            compose.onNodeWithTag("math.tab.$nextPage", useUnmergedTree = true).performTouchInput { click() }
            page = nextPage
        }
        val matches = compose.onAllNodesWithTag("math.key.$token", useUnmergedTree = true)
        val nodes = matches.fetchSemanticsNodes()
        if (power) {
            matches[nodes.lastIndex].performTouchInput { longClick(durationMillis = 650) }
            return
        }
        val origin = IntArray(2)
        instrumentation.runOnMainSync {
            WindowInspector.getGlobalWindowViews().last { it.javaClass.name.contains("PopupLayout") }.getLocationOnScreen(origin)
        }
        val point = nodes.last().boundsInWindow.center + Offset(origin[0].toFloat(), origin[1].toFloat())
        val time = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, point.x, point.y, 0)
        instrumentation.sendPointerSync(down); down.recycle()
        SystemClock.sleep(25)
        val up = MotionEvent.obtain(time, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, point.x, point.y, 0)
        instrumentation.sendPointerSync(up); up.recycle()
        instrumentation.waitForIdleSync(); SystemClock.sleep(25)
    }
    private fun enter(source: String) {
        page = "123"
        var i = 0
        while (i < source.length) {
            if (source[i] == '^') { key(source[i + 1].toString(), true); i += 2 }
            else { key(source[i].toString()); i++ }
        }
    }
    private fun canonical(source: String) = MathExpressionNormalizer.normalize(source)
        .replace(Regex("\\^\\(([0-9])\\)"), "^$1").replace(" ", "")
    private fun screenshot(id: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        // Wait for the emulator compositor to present the already-idle Compose frame.
        SystemClock.sleep(250)
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(reports, "$id.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun newCasesEnteredOnlyThroughOnscreenMathKeyboard() {
        val cases = JSONArray(instrumentation.context.assets.open("math-keyboard-300-new.json").bufferedReader().use { it.readText() })
        val args = InstrumentationRegistry.getArguments()
        val limit = args.getString("caseLimit")?.toInt() ?: 150
        val start = args.getString("startCase")?.toInt() ?: 0
        val selectedIds = args.getString("caseIds")?.split(',')?.toSet()
        val results = JSONArray()
        val failures = mutableListOf<String>()
        for (index in 0 until cases.length()) {
            if (index < start || index % 150 >= limit) continue
            val c = cases.getJSONObject(index)
            if (selectedIds != null && c.getString("id") !in selectedIds) continue
            val d = c.getInt("dimension"); val id = c.getString("id"); val source = c.getString("source")
            Log.i("Keyboard300", "START $id $source")
            val result = JSONObject().put("id", id).put("source", source).put("inputMethod", "onscreen touchscreen keys")
            var scenario: ActivityScenario<GraphVerificationActivity>? = null
            try {
                val intent = Intent(instrumentation.targetContext, GraphVerificationActivity::class.java)
                    .putExtra("verify_graph_mode", "${d}d").putExtra("verify_graph_expression", "0")
                scenario = ActivityScenario.launch(intent)
                val add = if (d == 2) "Add a graph equation to the workspace" else "Add a 3D graph equation to the workspace"
                compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription(add).fetchSemanticsNodes().isNotEmpty() }
                touchDescription(add)
                val editor = if (d == 2) "Editable g(x) equation" else "Editable 3D surface"
                compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(editor, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
                touchDescription(editor)
                if (d == 2) touchDescription("Clear complete input")
                enter(source)
                var imeVisible = false
                scenario.onActivity { imeVisible = it.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true }
                assertFalse("$id native keyboard must remain hidden", imeVisible)
                if ((index + 1) % 25 == 0 || selectedIds != null) screenshot("$id-keyboard")
                touchDescription("Finish math entry")
                if (d == 2) { touchTextIfPresent("Done"); touchTextIfPresent("Collapse ▲") }
                else {
                    touchTextIfPresent("Plot")
                    compose.waitUntil(10_000) { compose.onAllNodesWithText("3D Equation").fetchSemanticsNodes().isEmpty() }
                }
                var entered = ""; var rendered = ""
                scenario.onActivity { activity ->
                    val vm = activity.verificationViewModel
                    entered = if (d == 2) vm.state.functions.last().expression else {
                        val layer = vm.state.surfaceLayers.last()
                        if (layer.kind == SpatialSurfaceKind.Parametric) "x=${layer.expression};y=${layer.expressionY};z=${layer.expressionZ}" else layer.expression
                    }
                    if (d == 2) {
                        val function = vm.state.functions.last()
                        val resolved = vm.mathObjectGraphSnapshot(emptyMap()).graphObjects.firstOrNull { it.rowId == function.id }?.resolvedExpression
                        rendered = graphRenderExpression(entered, resolved, 1.0)
                    }
                }
                val kind = c.getString("kind")
                assertEquals("$id submitted expression", canonical(if (kind == "explicit") stripEquation(source) else source), canonical(if (kind == "explicit") stripEquation(entered) else entered))
                val x = c.getDouble("x"); val y = c.getDouble("y"); val expected = c.getDouble("expected")
                val engine = ExpressionEngine()
                if (d == 2) {
                    val graph = TypedGraphEngine()
                    val parsed = TypedGraphExpressionParser.parse(rendered)
                    assertEquals("$id renderer must preserve the graph mode", TypedGraphExpressionParser.parse(entered)::class.java, parsed::class.java)
                    when (kind) {
                        "explicit" -> assertEquals(id, expected, graph.evaluate(parsed, x)!!.y, 1e-7)
                        "polar" -> { val point = graph.evaluate(parsed, x)!!; assertEquals(id, 0.0, point.x, 1e-7); assertEquals(id, expected, point.y, 1e-7) }
                        "parametric" -> { val point = graph.evaluate(parsed, x)!!; assertEquals(id, source.substringAfter("x(t)=").substringBefore('*').toDouble() * x, point.x, 1e-7); assertEquals(id, expected, point.y, 1e-7) }
                        "implicit" -> assertEquals(id, 0.0, engine.compile((parsed as TypedGraphExpression.Implicit).residual).eval(mapOf("x" to x, "y" to y)), 1e-7)
                    }
                    val sample = graph.sample(parsed, samples = 96)
                    assertTrue("$id must produce geometry", sample.curves.any { it.points.isNotEmpty() } || sample.implicitSegments.isNotEmpty() || sample.inequalityCells.isNotEmpty())
                    assertTrue(sample.curves.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() })
                } else {
                    val parsed = SurfaceInputInterpreter.interpret(entered).getOrThrow()
                    when (kind) {
                        "explicit" -> assertEquals(id, expected, engine.compile(parsed.expression).eval(mapOf("x" to x, "y" to y)), 1e-7)
                        "parametric" -> assertEquals(id, expected, engine.compile(parsed.expressionZ).eval(mapOf("u" to x, "v" to y)), 1e-7)
                        "implicit" -> { val sides = parsed.expression.split('=', limit = 2); assertEquals(id, 0.0, engine.compile("(${sides[0]})-(${sides[1]})").eval(mapOf("x" to x, "y" to y, "z" to c.getDouble("z"))), 1e-7) }
                    }
                }
                if ((index + 1) % 25 == 0 || limit < 150 || selectedIds != null) screenshot(id)
                result.put("status", "PASS").put("submitted", entered).put("rendered", rendered).put("nativeKeyboardVisible", imeVisible)
                Log.i("Keyboard300", "PASS $id")
            } catch (failure: Throwable) {
                val error = "$id: ${failure.message}"; failures += error
                result.put("status", "FAIL").put("error", error)
                screenshot("$id-failure"); Log.e("Keyboard300", error, failure)
            } finally {
                results.put(result); File(reports, "results.json").writeText(results.toString(2)); scenario?.close()
            }
        }
        assertEquals((0 until cases.length()).count {
            it >= start && it % 150 < limit && (selectedIds == null || cases.getJSONObject(it).getString("id") in selectedIds)
        }, results.length())
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
