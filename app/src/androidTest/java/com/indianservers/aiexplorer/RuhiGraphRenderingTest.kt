package com.indianservers.aiexplorer

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.workspace.CommandHistory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RuhiGraphRenderingTest {
    @get:Rule val compose=createEmptyComposeRule()
    private var redrawCanvas: (() -> Unit)? = null
    private val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"ruhi-graph-audit").apply { mkdirs() }
    private fun canvas(): Bitmap {
        redrawCanvas?.invoke()
        compose.waitForIdle()
        val bounds=compose.onNode(hasContentDescription("Interactive 3D graph canvas"),useUnmergedTree=true).fetchSemanticsNode().boundsInWindow
        // Capture the real display and crop to the measured native canvas. Unlike PixelCopy's
        // Compose helper this does not wait for a new buffer on an unchanged scene.
        val screen=requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()) { "Native display capture unavailable" }
        val left=bounds.left.toInt().coerceIn(0,screen.width-1)
        val top=bounds.top.toInt().coerceIn(0,screen.height-1)
        val width=bounds.width.toInt().coerceIn(1,screen.width-left)
        val height=bounds.height.toInt().coerceIn(1,screen.height-top)
        val cropped=Bitmap.createBitmap(screen,left,top,width,height)
        if(cropped!==screen) screen.recycle()
        return cropped
    }
    private fun difference(before:Bitmap,after:Bitmap):Int {
        require(before.width==after.width && before.height==after.height)
        val first=IntArray(before.width*before.height);val second=IntArray(first.size)
        before.getPixels(first,0,before.width,0,0,before.width,before.height)
        after.getPixels(second,0,after.width,0,0,after.width,after.height)
        // Capture is bounded by the canvas, but native chrome can overlap its edges. Compare
        // the central plot area so a changed response/status label cannot fake a render pass.
        var changed=0
        for (y in (before.height*.20).toInt() until (before.height*.75).toInt())
            for (x in (before.width*.15).toInt() until (before.width*.85).toInt()) {
                val i=y*before.width+x
                if(first[i]!=second[i]) changed++
            }
        return changed
    }
    private fun save(bitmap:Bitmap,name:String) { File(directory,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }

    @Test fun bundledTensorFlowModelRecognizesAndExecutesParaphrases() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        TensorFlowGraphNlp(context).use { classifier ->
            val samples=mapOf("relocate the cube to (7, 4, 1)" to "MOVE", "enlarge all cubes by a factor of 3" to "SCALE", "turn the pyramid by 60 degrees around the x axis" to "ROTATE", "cluster the selected spheres" to "GROUP", "erase the last sphere" to "DELETE", "do not delete the sphere" to "OTHER")
            samples.forEach { (sentence,expected) -> val p=classifier.predict(sentence);assertEquals(sentence,expected,p.intent);if(expected!="OTHER") assertTrue(sentence,p.confidence>=.85f && p.margin>=.35f) }
            val old=GraphLanguageRouter.classifier
            try {
                GraphLanguageRouter.classifier=classifier
                val history=CommandHistory();val a=GraphAssistant()
                val initial=a.execute(com.indianservers.aiexplorer.workspace.WorkspaceState(module=com.indianservers.aiexplorer.workspace.MathModule.Graph3D,surfaceLayers=emptyList()),"Create a cube.",history).state
                val result=a.execute(initial,"Please relocate the cube to (7, 4, 1).",history)
                assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
                assertEquals(com.indianservers.aiexplorer.core.Vec3(7.0,4.0,1.0),result.state.surfaceLayers.single().graphTransform.position)
            } finally { GraphLanguageRouter.classifier=old }
        }
    }

    @Test fun requestedCommandExamplesHaveCorrectTensorFlowIntents() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val corpus=org.json.JSONObject(context.assets.open("ruhi_nlp/command_examples.json").bufferedReader().use { it.readText() }).getJSONArray("commands")
        val records=org.json.JSONArray()
        TensorFlowGraphNlp(context).use { classifier ->
            for(i in 0 until corpus.length()) {
                val sample=corpus.getJSONObject(i)
                val prediction=classifier.predict(sample.getString("text"))
                records.put(org.json.JSONObject().put("id",sample.getInt("id")).put("expected",sample.getString("intent")).put("actual",prediction.intent).put("confidence",prediction.confidence).put("margin",prediction.margin))
                assertEquals(sample.getString("text"),sample.getString("intent"),prediction.intent)
                assertTrue(prediction.confidence.isFinite() && prediction.margin.isFinite())
            }
        }
        val metadata=org.json.JSONObject(context.assets.open("ruhi_nlp/metadata.json").bufferedReader().use { it.readText() })
        File(directory,"requested-intents-native.json").writeText(org.json.JSONObject().put("model_sha256",metadata.getString("model_sha256")).put("benchmark","Training-fit recognition replay; does not certify engine execution or unseen-language accuracy").put("results",records).toString(2))
        assertEquals(89,corpus.length())
    }

    @Test fun twoDVisibleAssistantInputPlotsACurveAndChangesCanvasPixels() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val intent=Intent(instrumentation.targetContext,GraphVerificationActivity::class.java)
            .putExtra("verify_graph_mode","2d").putExtra("verify_graph_expression","0")
        ActivityScenario.launch<GraphVerificationActivity>(intent).use { scenario ->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000) { compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity { assertEquals(GraphCommandStatus.APPLIED,it.verificationViewModel.executeGraphAssistant("Clear all.").status) }
            compose.waitForIdle()
            val before=compose.onNode(matcher,useUnmergedTree=true).captureToImage().asAndroidBitmap()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            compose.onNodeWithText("Ask anything in maths").performTextInput("Plot y = x².")
            compose.onNodeWithText("Ask anything in maths").performImeAction()
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Ruhi — Applied").fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity { activity ->
                val state=activity.verificationViewModel.state
                assertEquals(1,state.functions.size)
                assertEquals("x^2",com.indianservers.aiexplorer.core.stripEquation(state.functions.single().expression).replace(" ",""))
            }
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithContentDescription("Close AI Assistant").performClick()
            compose.waitForIdle()
            val after=compose.onNode(matcher,useUnmergedTree=true).captureToImage().asAndroidBitmap()
            val changed=difference(before,after)
            save(before,"2d-ask-before");save(after,"2d-ask-after")
            File(directory,"2d-ask-result.json").writeText("""{"command":"Plot y = x².","input":"visible assistant text field and IME Search action","function_count":1,"changed_canvas_pixels":$changed,"passed":${changed>0}}""")
            assertTrue("Visible assistant must change the actual 2D graph canvas",changed>0)
            before?.recycle();after?.recycle()
        }
    }

    @Test fun original150RunThroughRealViewModelAndCanvas() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val filter=InstrumentationRegistry.getArguments().getString("caseIds")?.split(',')?.map(String::toInt)?.toSet()
        val cases=GraphAuditCases.all.filter { filter==null || it.id in filter }
        val outcomes=mutableListOf<GraphAuditOutcome>()
        val intent=Intent(instrumentation.targetContext,GraphVerificationActivity::class.java)
            .putExtra("verify_graph_mode","3d").putExtra("verify_graph_expression","0")
        ActivityScenario.launch<GraphVerificationActivity>(intent).use { scenario ->
            redrawCanvas = {
                scenario.onActivity { activity ->
                    fun invalidateTree(view: android.view.View) {
                        view.invalidate()
                        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) invalidateTree(view.getChildAt(i))
                    }
                    invalidateTree(activity.window.decorView)
                }
            }
            compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("Interactive 3D graph canvas"),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
            cases.forEach { case ->
                var prepared:PreparedGraphCase?=null
                var result:GraphCommandResult?=null
                scenario.onActivity { activity ->
                    val vm=activity.verificationViewModel
                    assertEquals(GraphCommandStatus.APPLIED,vm.executeGraphAssistant("Reset all.").status)
                    case.setup.forEach { command -> val setup=vm.executeGraphAssistant(command); assertEquals("Precondition ${case.id}: $command: ${setup.message}",GraphCommandStatus.APPLIED,setup.status) }
                    prepared=PreparedGraphCase(case,vm.state,CommandHistory())
                }
                compose.waitForIdle()
                val beforeCapture=runCatching { canvas() }
                val before=beforeCapture.getOrNull()
                scenario.onActivity { result=it.verificationViewModel.executeGraphAssistant(case.command) }
                compose.waitForIdle()
                val afterCapture=runCatching { canvas() }
                val after=afterCapture.getOrNull()
                val changed=if (before != null && after != null) difference(before,after) else null
                val checked=GraphAuditRunner.verify(prepared!!,result!!)
                // Rejections must leave canvas pixels unchanged. Case 145 is an atomic create/delete.
                val expectChange=result!!.status==GraphCommandStatus.APPLIED && case.id !in setOf(62,63,68,74,117,118,131,145)
                val renderPassed=changed?.let {
                    // A sphere/cylinder/cone can retain its silhouette after a symmetry rotation;
                    // triangulation may still change. Numeric orientation is asserted independently.
                    if(case.id in setOf(62,63,68,74)) true else if(expectChange) it>0 else it==0
                }
                val outcome=checked.copy(rendering=when(renderPassed) { true -> "PASS"; false -> "FAIL"; null -> "NOT_RUN" },changedPixels=changed, assertions=checked.assertions + listOfNotNull(beforeCapture.exceptionOrNull()?.let { "Before capture unavailable: ${it.message}" },afterCapture.exceptionOrNull()?.let { "After capture unavailable: ${it.message}" }))
                outcomes+=outcome
                if(after != null && (result!!.status==GraphCommandStatus.APPLIED || renderPassed == false)) save(after,"${case.id}-after")
                if(before != null && renderPassed == false) save(before,"${case.id}-before")
                File(directory,"results.json").writeText(GraphAuditRunner.json(outcomes))
                before?.recycle();after?.recycle()
            }
        }
        assertEquals(cases.size,outcomes.size)
        assertTrue(outcomes.filter { it.status=="FAILED" }.joinToString("\n") { "${it.case.id}: ${it.errors} pixels=${it.changedPixels}: ${it.result.message}" },outcomes.none { it.status=="FAILED" })
    }
}
