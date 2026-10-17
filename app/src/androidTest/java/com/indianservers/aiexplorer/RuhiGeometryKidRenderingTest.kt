package com.indianservers.aiexplorer

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RuhiGeometryKidRenderingTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val folder get()=File(context.getExternalFilesDir(null),"ruhi-geometry3d-300").apply{mkdirs()}
    private val canvas get()=compose.onNodeWithContentDescription("Interactive 3D geometry canvas",useUnmergedTree=true)
    private fun screenshot(name:String):Bitmap = captureCommittedCanvas(compose,hasContentDescription("Interactive 3D geometry canvas"),File(folder,"$name.png"))
    private fun changes(a:Bitmap,b:Bitmap):Int {
        require(a.width==b.width&&a.height==b.height);val x=IntArray(a.width*a.height);val y=IntArray(x.size)
        a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height)
        return x.indices.count{x[it]!=y[it]}
    }
    @Test fun bundledTensorFlowRecognizesAll300AndNativeScenesMatchReviewedOutcomes() {
        TensorFlowGraphNlp.install(context)
        val examples=JSONArray(context.assets.open("ruhi_geometry3d_nlp/command_examples.json").bufferedReader().use{it.readText()})
        assertEquals(300,examples.length());val results=JSONArray()
        for(i in 0 until examples.length()) {
            val e=examples.getJSONObject(i);val prediction=RuhiLanguageServices.geometryClassifier!!.predict(RuhiGeometry3DLanguage.normalize(e.getString("text")))
            val r=RuhiGeometryKidAudit.run(RuhiGeometryKidAudit.Case(e.getInt("id"),e.getString("text"),e.getString("expected_status")))
            val errors=r.errors.toMutableList();if(prediction.intent!=e.getString("intent"))errors+="Expected intent ${e.getString("intent")}, got ${prediction.intent}"
            if(!prediction.confidence.isFinite()||!prediction.margin.isFinite())errors+="Nonfinite TensorFlow scores"
            results.put(JSONObject().put("id",r.id).put("status",r.status).put("intent",prediction.intent).put("confidence",prediction.confidence.toDouble()).put("margin",prediction.margin.toDouble()).put("errors",JSONArray(errors)).put("message",r.message))
        }
        val failed=(0 until results.length()).map{results.getJSONObject(it)}.filter{it.getJSONArray("errors").length()>0}
        File(folder,"native-300.json").writeText(JSONObject().put("total",300).put("passed",300-failed.size).put("rendering","separate representative canvas test").put("results",results).toString(2))
        assertTrue(failed.joinToString("\n"){it.toString()},failed.isEmpty())
    }
    @Test fun kidRequestsDrawAndModifyTheRealGeometryCanvas() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use { scenario ->
            compose.waitUntil(30_000){compose.onAllNodesWithContentDescription("Interactive 3D geometry canvas",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.clearCurrentWorkspace()};compose.waitForIdle();val before=screenshot("empty")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            fun ask(text:String,ready:()->Boolean) {
                compose.onNodeWithText("Ask anything in maths").performTextClearance()
                compose.onNodeWithText("Ask anything in maths").performTextInput(text)
                compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){ready()}
            }
            fun stateReady(test:(com.indianservers.aiexplorer.workspace.WorkspaceState)->Boolean):Boolean {var ready=false;scenario.onActivity{ready=test(it.verificationViewModel.state)};return ready}
            ask("Can you draw a cube for me?"){stateReady{it.solids.size==1}}
            ask("Change the cube's side length to 5."){stateReady{it.solids.singleOrNull()?.width==5.0}}
            ask("Color the cube red."){stateReady{it.labSessionValues["ruhi.studio.appearance.0"]=="red"}}
            ask("Move the cube 2 units to the right."){stateReady{it.solids.singleOrNull()?.position?.x==2.0}}
            ask("Rotate the cube 90 degrees around the z-axis."){stateReady{it.solids.singleOrNull()?.rotation?.z==90.0}}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            val after=screenshot("red-cube-transformed");val pixels=changes(before,after);assertTrue("Canvas did not change",pixels>100)
            scenario.onActivity {val vm=it.verificationViewModel;val q=vm.state.solids.single();assertEquals(125.0,Geometry3D.measure(q).volume,1e-8);val r=vm.executeRuhiAssistant("What is the volume of this cube?");assertTrue(r.message.contains("125.0"));val old=vm.state.solids;assertEquals(GraphCommandStatus.INVALID,vm.executeRuhiAssistant("Draw a sphere with radius minus 3.").status);assertEquals(old,vm.state.solids)}
            File(folder,"canvas-result.json").writeText(JSONObject().put("changed_pixels",pixels).put("volume",125).put("invalid_radius_rejected",true).put("visible_assistant",true).put("passed",true).toString(2))
            before.recycle();after.recycle()
        }
    }
}
