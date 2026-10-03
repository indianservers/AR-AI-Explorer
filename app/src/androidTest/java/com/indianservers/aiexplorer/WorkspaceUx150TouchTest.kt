package com.indianservers.aiexplorer

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real UI taps/holds/multi-touch. Fixture chooses only the starting workspace and baseline graph. */
class WorkspaceUx150TouchTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrument get() = InstrumentationRegistry.getInstrumentation()
    private val reports get() = File(instrument.targetContext.filesDir, "workspace-ux-150").also { it.mkdirs() }
    private lateinit var scenario: ActivityScenario<GraphVerificationActivity>
    private var workspace = ""
    private fun visibleText(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun hasDesc(text: String) = compose.onAllNodesWithContentDescription(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun text(text: String) { compose.onAllNodesWithText(text)[0].performTouchInput { click() }; compose.waitForIdle() }
    private fun desc(text: String) {
        val nodes = compose.onAllNodesWithContentDescription(text, useUnmergedTree = true)
        nodes[nodes.fetchSemanticsNodes().lastIndex].performTouchInput { click() }; compose.waitForIdle()
    }
    private fun geometryToolsHeader(description: String) {
        // The header contains Add/Clear buttons: use its title area, not its center.
        compose.onNodeWithContentDescription(description, useUnmergedTree = true)
            .performTouchInput { click(Offset(width*.15f,height*.5f)) }
        compose.waitForIdle()
    }
    private fun state(): WorkspaceState { var result: WorkspaceState? = null; scenario.onActivity { result = it.verificationViewModel.state }; return result!! }
    private fun count(s: WorkspaceState) = when(workspace) { "graph2d" -> s.functions.size; "graph3d" -> s.surfaceLayers.size; "geometry2d" -> s.shapes.size; else -> s.solids.size }
    private fun contents(s: WorkspaceState): Any = when(workspace) {
        "graph2d" -> s.functions
        "graph3d" -> s.surfaceLayers
        "geometry2d" -> s.points to s.shapes
        else -> s.solids
    }
    private fun canvas() = compose.onNode(hasContentDescription(when(workspace) {
        "graph2d" -> "Interactive graphing canvas with axes, curves, trace point, and annotations"
        "graph3d" -> "Interactive 3D graph canvas"
        "geometry2d" -> "Interactive coordinate geometry canvas."
        else -> "Interactive 3D geometry canvas"
    }, substring = workspace == "geometry2d"), useUnmergedTree = true)
    private fun inspectorOpen(): Boolean = when(workspace) {
        "graph2d" -> visibleText("Collapse ▲")
        "graph3d" -> hasDesc("Close Graph Properties")
        "geometry2d" -> hasDesc("Collapse 2D geometry tools")
        else -> hasDesc("Close 3D trackball tools")
    }
    private fun openInspector() {
        if (inspectorOpen()) return
        when(workspace) {
            "graph2d" -> text("Expand ▼")
            "graph3d" -> text("Layers")
            "geometry2d" -> geometryToolsHeader("Expand 2D geometry tools")
            else -> desc("Open 3D trackball tools")
        }
        assertTrue("Inspector must be open", inspectorOpen())
    }
    private fun closeInspector() {
        if (!inspectorOpen()) return
        when(workspace) {
            "graph2d" -> text("Collapse ▲")
            "graph3d" -> desc("Close Graph Properties")
            "geometry2d" -> geometryToolsHeader("Collapse 2D geometry tools")
            else -> desc("Close 3D trackball tools")
        }
        assertFalse("Explicit Close must close inspector", inspectorOpen())
    }
    private fun addControl(): String = when(workspace) {
        "graph2d" -> "Add a graph equation to the workspace"
        "graph3d" -> "Add a 3D graph equation to the workspace"
        "geometry2d" -> "+ Add"
        else -> "Add a 3D solid to the workspace"
    }
    private fun key(token: String, power: Boolean = false) {
        val nodes = compose.onAllNodesWithTag("math.key.$token", useUnmergedTree = true)
        val values = nodes.fetchSemanticsNodes()
        if (power) { nodes[values.lastIndex].performTouchInput { longClick(durationMillis = 650) }; return }
        val origin = IntArray(2)
        instrument.runOnMainSync { WindowInspector.getGlobalWindowViews().last { it.javaClass.name.contains("PopupLayout") }.getLocationOnScreen(origin) }
        val p = values.last().boundsInWindow.center + Offset(origin[0].toFloat(), origin[1].toFloat())
        val t = SystemClock.uptimeMillis()
        instrument.sendPointerSync(MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,p.x,p.y,0).also { })
        SystemClock.sleep(25)
        instrument.sendPointerSync(MotionEvent.obtain(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0))
        instrument.waitForIdleSync()
    }
    private fun addObject(third: Boolean) {
        // Add controls remain reachable after the inspector is explicitly collapsed.
        closeInspector(); desc(addControl())
        when(workspace) {
            "graph2d" -> if (third) {
                desc("Editable ${state().functions.last().name} equation"); desc("Clear complete input")
                key("y"); key("2", true); key("="); key("4")
                desc("Finish math entry"); if (visibleText("Done")) text("Done")
            }
            "graph3d" -> {
                desc("Editable 3D surface")
                key("x"); key("2", true); key("+"); key("y"); key("2", true)
                desc("Finish math entry"); if (visibleText("Plot")) text("Plot")
            }
            "geometry2d" -> {
                compose.onNode(hasSetTextAction() and hasText("Search shapes and tools")).performTextReplacement("Triangle")
                compose.onNode(hasText("Triangle") and !hasSetTextAction()).performScrollTo().performTouchInput { click() }
            }
            else -> compose.onNodeWithContentDescription("Add Cube").performScrollTo().performTouchInput { click() }
        }
        compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        instrument.uiAutomation.takeScreenshot()?.let { b -> File(reports,"$name.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; b.recycle() }
    }
    @Test fun graph2d() = runWorkspace("graph2d")
    @Test fun graph3d() = runWorkspace("graph3d")
    @Test fun geometry2d() = runWorkspace("geometry2d")
    @Test fun geometry3d() = runWorkspace("geometry3d")
    private fun runWorkspace(mode: String) {
        workspace = mode
        val all = instrument.context.assets.open("workspace-ux-150.json").bufferedReader().use { JSONArray(it.readText()) }
        val results = JSONArray(); val failures = mutableListOf<String>()
        scenario = ActivityScenario.launch(Intent(instrument.targetContext, GraphVerificationActivity::class.java).putExtra("verify_ux_workspace", mode))
        try {
            compose.waitUntil(30_000) { hasDesc(addControl()) }
            for(i in 0 until all.length()) {
                val c = all.getJSONObject(i); if(c.getString("workspace") != mode) continue
                val id = c.getString("id"); val op = c.getString("operation"); val p = c.getJSONObject("parameters")
                val result = JSONObject(c.toString()).put("inputMethod","Android emulator UI touch instrumentation")
                try {
                    val before = state(); val wasOpen = inspectorOpen()
                    when(op) {
                        "add" -> addObject(id.endsWith("038"))
                        "open" -> openInspector()
                        "close" -> closeInspector()
                        "toggle" -> { closeInspector(); openInspector() }
                        "tap" -> canvas().performTouchInput { click(Offset(width*p.getDouble("x").toFloat(),height*p.getDouble("y").toFloat())) }
                        "double" -> canvas().performTouchInput { doubleClick(Offset(width*.995f,height*.66f)) }
                        "hold" -> canvas().performTouchInput { longClick(Offset(width*.995f,height*.66f),durationMillis=650) }
                        "pan", "closed_pan" -> {
                            if(op == "closed_pan") closeInspector()
                            canvas().performTouchInput {
                                val start = Offset(width*(if(p.optString("direction") == "right") .005f else .995f),height*.25f)
                                val amount = if(op == "closed_pan") 45f else p.getInt("pixels").toFloat()
                                val delta = when(p.optString("direction","left")) { "right"->Offset(amount,0f); "up"->Offset(0f,-amount); "down"->Offset(0f,amount); else->Offset(-amount,0f) }
                                swipe(start,(start+delta).copy(x=(start.x+delta.x).coerceIn(2f,width-2f)),durationMillis=400)
                            }
                        }
                        "pinch" -> canvas().performTouchInput {
                            val x = width*.995f; val mid = height*.64f; val spread = 45f
                            down(0,Offset(x,mid-spread)); down(1,Offset(x,mid+spread))
                            moveTo(0,Offset(x,mid-spread*p.getDouble("factor").toFloat()))
                            moveTo(1,Offset(x,mid+spread*p.getDouble("factor").toFloat()))
                            advanceEventTime(300); up(0); up(1)
                        }
                        "cancel_back", "cancel_outside" -> {
                            if(mode == "graph2d") desc("Add equation") else desc(addControl())
                            if(op == "cancel_outside") canvas().performTouchInput { click(Offset(width*.995f,height*.80f)) }
                            // Equation editors are modeless; chooser dialogs support outside dismissal.
                            when(mode) {
                                "graph3d" -> if(hasDesc("Close 3D Equation")) desc("Close 3D Equation")
                                "geometry2d" -> if(hasDesc("Close Add 2D Shape")) desc("Close Add 2D Shape")
                                "geometry3d" -> if(hasDesc("Close shape library")) desc("Close shape library")
                                else -> if(op == "cancel_back") scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                            }
                        }
                        "resume" -> { scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED); scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED) }
                    }
                    compose.waitForIdle()
                    val after = state()
                    assertEquals("Workspace must remain active", before.module, after.module)
                    if(op == "add") assertEquals("One independent object added",count(before)+1,count(after))
                    else assertEquals("Navigation must preserve mathematical objects",contents(before),contents(after))
                    assertTrue("Objects must remain present",count(after)>0)
                    if(wasOpen && op in setOf("tap","double","hold","pan","pinch","resume")) assertTrue("Background interaction must retain inspector",inspectorOpen())
                    if(mode == "graph2d" && wasOpen && op in setOf("tap","double","hold","pan","pinch","resume"))
                        compose.onNodeWithContentDescription("Editable ${before.functions.last().name} equation", useUnmergedTree = true).assertExists()
                    canvas().assertExists()
                    result.put("status","PASS").put("objectCount",count(after)).put("inspectorOpen",inspectorOpen())
                    if(i%10 == 0) screenshot(id)
                } catch(e: Throwable) {
                    failures += "$id: ${e.message}"; result.put("status","FAIL").put("error",e.message); screenshot("$id-failed")
                }
                results.put(result); File(reports,"$mode.json").writeText(results.toString(2))
            }
            screenshot("$mode-final")
        } finally { scenario.close() }
        assertTrue(failures.joinToString("\n"),failures.isEmpty())
    }
}
