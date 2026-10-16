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
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RuhiSharedRenderingTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val evidence get()=File(context.getExternalFilesDir(null),"ruhi-shared-audit").apply{mkdirs()}
    private fun launch(engine:RuhiEngine):ActivityScenario<GraphVerificationActivity> {
        val intent=Intent(context,GraphVerificationActivity::class.java)
        when(engine){RuhiEngine.GRAPH_2D->intent.putExtra("verify_graph_mode","2d").putExtra("verify_graph_expression","0");RuhiEngine.GRAPH_3D->intent.putExtra("verify_graph_mode","3d").putExtra("verify_graph_expression","0");RuhiEngine.GEOMETRY_2D->intent.putExtra("verify_ux_workspace","geometry2d");RuhiEngine.GEOMETRY_3D->intent.putExtra("verify_ux_workspace","geometry3d")}
        return ActivityScenario.launch(intent)
    }
    private fun matcher(engine:RuhiEngine)=when(engine){RuhiEngine.GRAPH_2D->hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations");RuhiEngine.GRAPH_3D->hasContentDescription("Interactive 3D graph canvas");RuhiEngine.GEOMETRY_2D->hasContentDescription("Interactive coordinate geometry canvas.",substring=true);RuhiEngine.GEOMETRY_3D->hasContentDescription("Interactive 3D geometry canvas")}
    private fun capture(engine:RuhiEngine):Bitmap=compose.onNode(matcher(engine),useUnmergedTree=true).captureToImage().asAndroidBitmap()
    private fun save(bitmap:Bitmap,name:String){File(evidence,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
    private fun changed(a:Bitmap,b:Bitmap):Int {
        require(a.width==b.width && a.height==b.height);val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height)
        var n=0;for(row in (a.height*.2).toInt() until (a.height*.75).toInt())for(col in (a.width*.15).toInt() until (a.width*.85).toInt())if(x[row*a.width+col]!=y[row*a.width+col])n++;return n
    }
    @Test fun allFourStudioAdaptersChangeTheirRealCanvasAndPreserveOtherSceneData() {
        val commands=mapOf(RuhiEngine.GRAPH_2D to "Ruhi, draw y equals x squared.",RuhiEngine.GRAPH_3D to "Plot z = x+y.",RuhiEngine.GEOMETRY_2D to "Create a square with side length four.",RuhiEngine.GEOMETRY_3D to "Create a sphere with radius three.")
        val records=mutableListOf<String>()
        for((engine,command) in commands) launch(engine).use { scenario ->
            compose.waitUntil(30_000){compose.onAllNodes(matcher(engine),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.clearCurrentWorkspace()};compose.waitForIdle();val before=capture(engine)
            scenario.onActivity { activity ->
                val vm=activity.verificationViewModel;val old=vm.state;val r=vm.executeRuhiAssistant(command)
                assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(engine,r.command!!.engine);assertEquals(1,r.command!!.schemaVersion)
                if(engine==RuhiEngine.GEOMETRY_3D){assertEquals(36*Math.PI,Geometry3D.measure(vm.state.solids.single()).volume,1e-7);assertTrue(SolidMeshFactory.create(vm.state.solids.single()).vertices.isNotEmpty())}
                if(engine==RuhiEngine.GEOMETRY_2D){assertEquals(16.0,kotlin.math.abs(Geometry2D.polygonArea(vm.state.points)),1e-8);assertEquals(old.functions,vm.state.functions)}
                if(engine==RuhiEngine.GRAPH_3D)assertTrue(GraphSurfaceGeometry.mesh(vm.state.surfaceLayers.single()).vertices.all{kotlin.math.abs(it.z-it.x-it.y)<1e-8})
                if(engine==RuhiEngine.GRAPH_2D)assertTrue(GraphAnalysis().sampleDefinition(vm.state.functions.single().expression,-2.0,2.0).points.isNotEmpty())
            }
            compose.waitForIdle();val after=capture(engine);val pixels=changed(before,after);assertTrue("$engine canvas did not change",pixels>0);save(after,"$engine.png");records+="{\"engine\":\"$engine\",\"changed_pixels\":$pixels,\"passed\":true}"
            before.recycle();after.recycle()
        }
        File(evidence,"four-engines.json").writeText("{\"results\":[${records.joinToString()}]}")
    }
    @Test fun spokenMathAndContextUseTheExistingVisibleAssistant() {
        launch(RuhiEngine.GRAPH_2D).use { scenario ->
            compose.waitUntil(30_000){compose.onAllNodes(matcher(RuhiEngine.GRAPH_2D),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.clearCurrentWorkspace()}
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            fun ask(text:String,expected:(WorkspaceState)->Boolean) {
                compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var ready=false;scenario.onActivity{ready=expected(it.verificationViewModel.state)};ready}
            }
            ask("Ruhi, draw y equals x squared."){it.functions.size==1}
            ask("Make it red."){it.functions.singleOrNull()?.colorKey=="#FF0000"}
            ask("Shift it three units right."){it.functions.singleOrNull()?.expression?.contains("x-3.0")==true}
            ask("Show the vertex."){it.labSessionValues.keys.any{key->key.startsWith("ruhi.graph2d.vertex.")}}
            compose.onNodeWithText("Vertex = (3.0, 0.0)",substring=true).assertExists()
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle();save(capture(RuhiEngine.GRAPH_2D),"spoken-math-context.png")
            scenario.onActivity { vmActivity->val vm=vmActivity.verificationViewModel;val id=vm.state.functions.single().id;assertTrue(vm.state.labSessionValues["ruhi.graph2d.vertex.$id"]!!.endsWith("3.0,0.0"));assertEquals(0.0,ExpressionEngine().compile(stripEquation(vm.state.functions.single().expression)).eval(mapOf("x" to 3.0)),1e-8) }
        }
    }
    @Test fun tensorflowEngineProposalsRunThroughTheBundledAndroidInterpreter() {
        TensorFlowEngineNlp(context).use { model ->
            val samples=mapOf("plot the function y = x^2" to RuhiEngine.GRAPH_2D,"plot the surface z = x*y" to RuhiEngine.GRAPH_3D,"construct a square" to RuhiEngine.GEOMETRY_2D,"draw a 3d cone" to RuhiEngine.GEOMETRY_3D)
            val records=samples.map{(text,expected)->val p=model.predict(text);assertEquals(text,expected,p.engine);assertTrue(p.confidence.isFinite()&&p.margin.isFinite());"{\"expected\":\"$expected\",\"actual\":\"${p.engine}\",\"confidence\":${p.confidence},\"margin\":${p.margin}}"}
            File(evidence,"engine-native.json").writeText("{\"results\":[${records.joinToString()}],\"passed\":true}")
        }
    }
    @Test fun rotatingConeUsesTimeBasedPoseAndTouchUndoRestoresAnimation() {
        compose.mainClock.autoAdvance=false
        try { launch(RuhiEngine.GEOMETRY_3D).use { scenario ->
            compose.mainClock.advanceTimeBy(64)
            scenario.onActivity { activity ->
                val vm=activity.verificationViewModel;vm.clearCurrentWorkspace();val r=vm.executeRuhiAssistant("Ruhi, create a rotating 3D cone.",RuhiInputModality.VOICE_TRANSCRIPT)
                assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(RuhiInputModality.VOICE_TRANSCRIPT,r.command!!.modality)
                val state=vm.state;val motion=RuhiAngularMotion.decode(state.labSessionValues[RuhiMotionEngine.PREFIX+0]!!)
                assertEquals(30.0,RuhiMotionEngine.solids(state,motion.startMillis+1000).single().rotation.y,1e-6)
            }
            compose.mainClock.advanceTimeBy(64);val before=capture(RuhiEngine.GEOMETRY_3D)
            Thread.sleep(500);compose.mainClock.advanceTimeBy(64);val after=capture(RuhiEngine.GEOMETRY_3D);val pixels=changed(before,after);assertTrue("Animated cone pixels unchanged",pixels>0);save(after,"rotating-cone.png")
            scenario.onActivity { activity ->
                val vm=activity.verificationViewModel;val depth=vm.universalHistoryDepth
                vm.beginSolidDrag(0);vm.previewSolidDrag(0,Vec3(1.0,0.0,0.0));vm.endSolidDrag()
                assertFalse(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)});assertEquals(depth+1,vm.universalHistoryDepth)
                vm.undo();assertTrue(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
                val stop=vm.executeRuhiAssistant("Stop the rotation.");assertEquals(GraphCommandStatus.APPLIED,stop.status);assertFalse(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
            }
            scenario.onActivity { activity ->
                val vm=activity.verificationViewModel
                vm.executeRuhiAssistant("Create a rotating cone.")
                assertTrue(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
                vm.addPoint3D(Vec3(1.0,2.0,3.0));vm.addVector3D();vm.clearCurrentWorkspace();assertTrue(vm.state.solids.isEmpty());assertTrue(vm.state.points3D.isEmpty());assertTrue(vm.state.vectors3D.isEmpty());assertFalse(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
                vm.undo();assertTrue(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
                val animated=vm.state.solids.lastIndex
                vm.beginSolidDrag(animated);vm.deleteSelectedSolids(setOf(animated))
                val creation=vm.executeRuhiAssistant("Create a sphere with radius 1.");assertEquals(GraphCommandStatus.APPLIED,creation.status)
                val baseline=vm.state.solids
                vm.beginSolidDrag(vm.state.solids.lastIndex);vm.previewSolidDrag(vm.state.solids.lastIndex,Vec3(2.0,0.0,0.0));vm.endSolidDrag();vm.undo()
                assertEquals("A stale animated drag must not restore a deleted solid",baseline,vm.state.solids)
                vm.clearCurrentWorkspace();vm.executeRuhiAssistant("Create a sphere with radius 1.");assertFalse(vm.state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
            }
            File(evidence,"motion.json").writeText("{\"changed_pixels\":$pixels,\"touch_undo_resumes_animation\":true,\"clear_undo_and_native_delete_metadata_verified\":true,\"passed\":true}")
        } } finally { compose.mainClock.autoAdvance=true }
    }
}
