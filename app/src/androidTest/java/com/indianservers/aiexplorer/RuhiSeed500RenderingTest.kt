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
import com.indianservers.aiexplorer.workspace.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class RuhiSeed500RenderingTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val folder get()=File(context.getExternalFilesDir(null),"ruhi-seed500").apply{mkdirs()}
    private fun awaitRuhiInput() {
        compose.waitUntil(30_000){compose.onAllNodesWithText("Ask anything in maths").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
    }
    private fun captureCanvas(matcher:SemanticsMatcher,name:String):Bitmap = captureCommittedCanvas(compose,matcher,File(folder,"$name.png"))
    private fun askConstruction(scenario:ActivityScenario<GraphVerificationActivity>,text:String,expected:String) {
        var count=0;scenario.onActivity{count=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.size}
        compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextClearance();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextInput(text);awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performImeAction()
        try{compose.waitUntil(30_000){var done=false;scenario.onActivity{val turns=RuhiDialogueMemory.read(it.verificationViewModel.state).turns;done=turns.size>count&&turns.last().user==text&&turns.last().assistant.contains(expected)};done}}catch(error:Throwable){scenario.onActivity{File(folder,"construction-failure-state.txt").writeText(WorkspaceProjectCodec.encode(it.verificationViewModel.state))};File(folder,"construction-failure-semantics.txt").writeText(runCatching{compose.onRoot(useUnmergedTree=true).printToString()}.getOrElse{it.toString()});throw error}
        androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
    }
    private fun captureDistanceReply(text:String,name:String) {
        compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput()
        compose.onAllNodesWithText(text,substring=true).onLast().performScrollTo().assertIsDisplayed()
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"$name.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
        androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
    }
    @Test fun realRadicalDerivativeHelpersHaveFiniteSmoothZerosAndFollowSavedCurrentCurves() {
        TensorFlowGraphNlp.install(context)
        val inputs=listOf(Triple("x^2",0.0,0.0),Triple("sqrt(x^4)",0.0,0.0),Triple("sqrt(x^4)",2.0,4.0),Triple("sqrt((x-1.25)^4)",1.25,0.0),Triple("sqrt(x^2)-abs(x)+x",0.0,1.0),Triple("sqrt(x^2)",0.0,null),Triple("sqrt(x^6)",0.0,0.0),Triple("sqrt(x^2)",-2.0,-1.0))
        val records=JSONArray();val engine=ExpressionEngine()
        fun derivative(s:WorkspaceState)=RuhiDerivativeLanguage.drawing(s)!!.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar
        for((formula,x,expected) in inputs) {
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList());val r=RuhiDialogueManager().execute(initial,"Differentiate $formula",WorkspaceHistoryByModule())
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(initial.functions,r.state.functions);val c=derivative(r.state);val raw=engine.compile(c.expression).eval(mapOf("x" to x));val actual=raw.takeIf{it.isFinite()}
            if(expected==null)assertNull(actual)else assertEquals(formula,expected,actual!!,1e-10)
            records.put(JSONObject().put("formula",formula).put("x",x).put("expected",expected ?: JSONObject.NULL).put("actual",actual ?: JSONObject.NULL).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("root-helper,/907","Root907","y=sqrt(x^4)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle();val before=captureCanvas(matcher,"radical-helper-before")
            askConstruction(scenario,"Plot the derivative of the selected curve","Derivative")
            fun sampled(center:Double):Int {
                var count=0
                scenario.onActivity{val s=it.verificationViewModel.state;val c=derivative(s);assertEquals(0.0,engine.compile(c.expression).eval(mapOf("x" to center)),0.0);assertEquals(1,s.functions.size)
                    val native="y=${c.expression}"+(c.restriction?.let{" {$it}"} ?: "")
                    val sample=TypedGraphEngine().sample(TypedGraphExpressionParser.parse(native),GraphDomain(center-2,center+2),GraphDomain(-5.0,5.0,"y"),samples=256)
                    assertTrue(sample.curves.any{segment->segment.points.any{it.x<center}&&segment.points.any{it.x>center}});assertTrue(sample.curves.flatMap{it.points}.all{abs(it.y-2*(it.x-center))<1e-8});count=sample.curves.sumOf{it.points.size}}
                return count
            }
            val samples=sampled(0.0);val after=captureCanvas(matcher,"radical-helper-smooth-zero");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){it.copy(expression="y=sqrt((x-1.25)^4)")};vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            sampled(1.25);captureCanvas(matcher,"radical-helper-edited").recycle();askConstruction(scenario,"Plot the derivative of the selected curve","Derivative");captureDistanceReply("Derivative","radical-helper-answer")
            File(folder,"radical-helper-canvas.json").writeText(JSONObject().put("passed",true).put("cases",8).put("results",records).put("actual_helper_values",true).put("true_cusp_preserved",true).put("original_curve_preserved",true).put("native_sampler_bridges_smooth_zero",true).put("current_saved_curve",true).put("sample_points",samples).put("changed_pixels",pixels).put("edited_zero",1.25).put("committed_capture",true).toString(2));before.recycle();after.recycle()
        }
    }

    @Test fun realRadicalCriticalAndStationaryPointsPreserveDomainsAndFollowCurrentCurveEdits() {
        TensorFlowGraphNlp.install(context)
        val inputs=listOf(
            "Find critical points of y = abs(x)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt(x^2)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt((x-1.25)^2)+2*x" to listOf(Vec2(1.25,2.5)),
            "Find the stationary points of sqrt(x^2)" to emptyList(),
            "Find critical points of y = sqrt(x^4)" to listOf(Vec2(0.0,0.0)),
            "Find the stationary points of sqrt(x^4)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt(x^2)-sqrt(x^2)+x" to emptyList(),
            "Find critical points of y = 1/sqrt(x^2)" to emptyList())
        val records=JSONArray()
        for((source,expected) in inputs) {
            val s=RuhiSeed500Fixture.state("GRAPH_2D",344);val r=RuhiDialogueManager().execute(s,source,WorkspaceHistoryByModule(),RuhiSeed500Fixture.context(344,s))
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points)
            val actual=RuhiDerivativeLanguage.drawing(r.state)!!.points;assertEquals(source,expected.size,actual.size);assertTrue(source,expected.all{e->actual.any{it.distanceTo(e)<1e-6}})
            records.put(JSONObject().put("input",source).put("expected_points",JSONArray(expected.map{listOf(it.x,it.y)})).put("actual_points",JSONArray(actual.map{listOf(it.x,it.y)})).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("radical,/907","Root907","y=sqrt(x^2)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points")
            fun point(expected:Vec2) {
                scenario.onActivity{assertEquals(listOf(expected),RuhiDerivativeLanguage.drawing(it.verificationViewModel.state)!!.points);assertTrue(it.verificationViewModel.state.points.isEmpty())}
                val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(rendered,rendered.contains("(${expected.x}, ${expected.y})"))
            }
            point(Vec2(0.0,0.0));captureCanvas(matcher,"radical-critical-corner").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){it.copy(expression="y=sqrt((x-2)^2)+2*x")};vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            point(Vec2(2.0,4.0));captureCanvas(matcher,"radical-critical-edited").recycle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points");captureDistanceReply("(2.0, 4.0)","radical-critical-answer")
            File(folder,"radical-critical-canvas.json").writeText(JSONObject().put("passed",true).put("cases",8).put("results",records).put("original_domain_preserved",true).put("native_current_curve",true).put("actual_rendered_coordinates",true).put("initial_point",JSONArray(listOf(0,0))).put("edited_point",JSONArray(listOf(2,4))).put("committed_capture",true).toString(2))
        }
    }

    @Test fun selectedNativeSegmentRepliesResumeSavedMidpointAndSlopeWithoutEndpointSubstitution() {
        TensorFlowGraphNlp.install(context)
        for((id,source) in listOf(162 to "Mark the midpoint of the selected segment",179 to "Measure the slope of the selected line")) {
            val initial=RuhiSeed500Fixture.state("GEOMETRY_2D",id);val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
            val q=manager.execute(initial,source,history);assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
            val r=manager.execute(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!,"AB",history)
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(if(id==162)"FIND_MIDPOINT"else"CALCULATE_SLOPE",r.semanticFrame!!.intent)
            assertEquals(if(id==162)"selected_segment"else"selected_line",r.semanticFrame!!.parameters["target"]);assertEquals("line-ab",r.context.selectedShapeId)
            if(id==162){assertEquals(Vec2(.5,.5),r.state.points.last());assertEquals(1,history.forModule(initial.module).protocol.size)}
            else {assertTrue(r.message.contains("Slope = 1.0"));assertEquals(initial.points,r.state.points);assertEquals(initial.shapes,r.state.shapes);assertTrue(history.forModule(initial.module).protocol.isEmpty())}
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0)),shapes=listOf(Shape2D("native-segment,/907",Shape2DType.Segment,listOf(0,1),"SunMoon907")),labSessionValues=mapOf("ruhi.studio.point.Sun907" to "0","ruhi.studio.point.Moon811" to "1"))
            fun reset(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(initial,false,emptyList());vm.selectShape(-1)};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectShape(-1)};compose.waitForIdle()}
            reset();askConstruction(scenario,"Mark the midpoint of the selected segment","Select a line segment");restore()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("endpoint_shape",RuhiDialogueMemory.read(vm.state).pending!!.kind);vm.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("SunMoon907",substring=false).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"segment-selection-question").recycle();compose.onAllNodesWithText("SunMoon907",substring=false).onLast().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.contains("Midpoint Mid1 = (2.0, 3.0)")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(Vec2(2.0,3.0),vm.state.points.last());assertEquals(listOf(0,1),vm.state.pointDependencies.single().inputIndices);assertEquals(initial.shapes,vm.state.shapes);vm.undo();assertEquals(listOf(Vec2(0.0,0.0),Vec2(4.0,6.0)),vm.state.points);vm.redo();assertEquals(Vec2(2.0,3.0),vm.state.points.last())};compose.waitForIdle()
            captureCanvas(matcher,"segment-selection-midpoint").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(1,Vec2(2.0,4.0));assertEquals(Vec2(1.0,2.0),vm.state.points.last())};restore();captureCanvas(matcher,"segment-selection-midpoint-live").recycle()
            reset();askConstruction(scenario,"Measure the slope of the selected line","Select a line segment");restore();scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            askConstruction(scenario,"SunMoon907","Slope = 1.5; rise = 6.0, run = 4.0")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(2,vm.state.points.size);assertEquals(initial.shapes,vm.state.shapes);assertTrue(vm.state.pointDependencies.isEmpty());assertNull(RuhiDialogueMemory.read(vm.state).pending)}
            captureDistanceReply("Slope = 1.5; rise = 6.0, run = 4.0","segment-selection-slope-answer")
            scenario.onActivity{val vm=it.verificationViewModel;vm.undo();assertEquals(initial.points,vm.state.points)}
            File(folder,"segment-selection-canvas.json").writeText(JSONObject().put("passed",true).put("source_rows",JSONArray(listOf(162,179))).put("saved_object_chip",true).put("plain_name_reply",true).put("native_shape_id","native-segment,/907").put("original_target_roles",true).put("current_coordinates",true).put("midpoint",JSONArray(listOf(2,3))).put("edited_midpoint",JSONArray(listOf(1,2))).put("native_parent_dependency",true).put("atomic_undo_redo",true).put("slope",1.5).put("slope_query_no_undo",true).put("shapes_preserved",true).put("committed_capture",true).toString(2))
        }
    }

    @Test fun midpointReplacementLabelsPreserveExistingPointsAndSavedEndpointBindings() {
        TensorFlowGraphNlp.install(context)
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun initial(a:String,b:String)=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0),Vec2(-2.0,2.0)),labSessionValues=mapOf("ruhi.studio.point.$a" to "0","ruhi.studio.point.$b" to "1","ruhi.studio.point.M" to "2"))
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial("Sun907","Moon811"),false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Label the middle of AB as M","Which two endpoints");restore();askConstruction(scenario,"Sun907 and Moon811","already exists")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals("midpoint_label",RuhiDialogueMemory.read(s).pending!!.kind);assertEquals(3,s.points.size);assertTrue(RuhiDialogueMemory.read(s).lastChoices.none{it.answer.startsWith("Use endpoints")})};restore()
            captureDistanceReply("Choose an unused midpoint label","midpoint-label-question")
            scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            askConstruction(scenario,"Center907","Midpoint Center907 = (2.0, 3.0)")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(4,vm.state.points.size);assertEquals(Vec2(2.0,3.0),vm.state.points.last());assertEquals("2",vm.state.labSessionValues["ruhi.studio.point.M"]);assertEquals(Vec2(-2.0,2.0),vm.state.points[2]);assertEquals(listOf(0,1),vm.state.pointDependencies.single().inputIndices);vm.undo();assertEquals(listOf(Vec2(0.0,0.0),Vec2(4.0,6.0),Vec2(-2.0,2.0)),vm.state.points);vm.redo();assertEquals("3",vm.state.labSessionValues["ruhi.studio.point.Center907"])};compose.waitForIdle()
            captureDistanceReply("Midpoint Center907 = (2.0, 3.0)","midpoint-label-answer")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(1,Vec2(2.0,4.0));assertEquals(Vec2(1.0,2.0),vm.state.points.last())};restore();captureCanvas(matcher,"midpoint-label-live").recycle()
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial("A","B"),false,emptyList())};compose.waitForIdle();askConstruction(scenario,"Label the middle of AB as M","already exists");restore()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("Mid1",substring=false).onLast().performScrollTo().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.contains("Midpoint Mid1 = (3.0, 4.0)")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals("3",s.labSessionValues["ruhi.studio.point.Mid1"]);assertEquals("2",s.labSessionValues["ruhi.studio.point.M"]);assertEquals(Vec2(-2.0,2.0),s.points[2]);assertEquals(Vec2(3.0,4.0),s.points.last());assertNull(RuhiDialogueMemory.read(s).pending)}
            captureCanvas(matcher,"midpoint-label-chip-created").recycle()
            File(folder,"midpoint-label-canvas.json").writeText(JSONObject().put("passed",true).put("source_row",168).put("saved_label_question",true).put("endpoint_bindings_preserved",true).put("unused_label_chip_clicked",true).put("existing_M_preserved",true).put("atomic_undo_redo",true).put("new_label","Center907").put("created_point",JSONArray(listOf(2,3))).put("live_point",JSONArray(listOf(1,2))).put("chip_point",JSONArray(listOf(3,4))).put("native_parent_dependency",true).put("committed_capture",true).toString(2))
        }
    }
    @Test fun endpointClarificationsResumeSavedMidpointAndSlopeUsingActualCurrentPoints() {
        TensorFlowGraphNlp.install(context)
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0)),labSessionValues=mapOf("ruhi.studio.point.Sun907" to "0","ruhi.studio.point.Moon811" to "1"))
            fun reset(){scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()}
            reset();askConstruction(scenario,"Find the midpoint of AB","Which two endpoints");restore()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("endpoint_reference",RuhiDialogueMemory.read(vm.state).pending!!.kind);assertEquals(2,vm.state.points.size);vm.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("Sun907 + Moon811",substring=false).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"endpoint-pair-question").recycle();compose.onAllNodesWithText("Sun907 + Moon811",substring=false).onLast().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.contains("Midpoint Mid1 = (2.0, 3.0)")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(Vec2(2.0,3.0),vm.state.points.last());assertEquals(listOf(0,1),vm.state.pointDependencies.single().inputIndices);assertNull(RuhiDialogueMemory.read(vm.state).pending);vm.undo();assertEquals(listOf(Vec2(0.0,0.0),Vec2(4.0,6.0)),vm.state.points);vm.redo();assertEquals(Vec2(2.0,3.0),vm.state.points.last())};compose.waitForIdle()
            captureCanvas(matcher,"endpoint-pair-midpoint").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(1,Vec2(2.0,4.0));assertEquals(Vec2(1.0,2.0),vm.state.points.last())};restore();captureCanvas(matcher,"endpoint-pair-midpoint-edited").recycle()
            reset();askConstruction(scenario,"Find the slope of AB","Which two endpoints");restore();scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            askConstruction(scenario,"Sun907 and Moon811","Slope = 1.5; rise = 6.0, run = 4.0")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(2,s.points.size);assertTrue(s.shapes.isEmpty());assertTrue(s.pointDependencies.isEmpty());assertNull(RuhiDialogueMemory.read(s).pending)}
            captureDistanceReply("Slope = 1.5; rise = 6.0, run = 4.0","endpoint-pair-slope-answer")
            scenario.onActivity{val vm=it.verificationViewModel;vm.undo();assertEquals(initial.points,vm.state.points)}
            File(folder,"endpoint-pair-canvas.json").writeText(JSONObject().put("passed",true).put("source_rows",JSONArray(listOf(151,176))).put("restored_endpoint_chip",true).put("current_coordinates",true).put("midpoint",JSONArray(listOf(2,3))).put("edited_midpoint",JSONArray(listOf(1,2))).put("native_parent_dependency",true).put("atomic_undo_redo",true).put("slope",1.5).put("slope_query_no_undo",true).put("original_operation_preserved",true).put("committed_capture",true).toString(2))
        }
    }
    @Test fun mathematicalWordFormulaRepliesResumeOriginalAnalysisWithNativeCurrentResults() {
        TensorFlowGraphNlp.install(context)
        val records=JSONArray()
        for(family in listOf("derivative","integral"))for(formula in listOf("y=x^2","y equals x squared","x squared")) {
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList());val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
            val q=manager.execute(initial,if(family=="derivative")"Plot the derivative of the selected curve"else"Integrate the selected function",h)
            val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!;var r=manager.execute(restored,formula,h)
            if(family=="integral"){assertEquals(GraphCommandStatus.CLARIFICATION,r.status);r=manager.execute(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!,"0 to 2",h)}
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(if(family=="derivative")"DIFFERENTIATE"else"INTEGRATE",r.semanticFrame!!.intent)
            val actual=if(family=="integral")RuhiIntegralLanguage.drawing(r.state)!!.values!!.signed else {val query=RuhiDerivativeLanguage.decode(r.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));RuhiDerivativeMath.slope(RuhiDerivativeLanguage.resolve(r.state,query),2.0).value}
            val expected=if(family=="integral")8.0/3 else 4.0;assertEquals(expected,actual,1e-7);assertTrue(RuhiDialogueMemory.read(r.state).turns.any{it.user==formula});assertEquals(1,h.forModule(initial.module).protocol.size)
            records.put(JSONObject().put("family",family).put("formula_reply",formula).put("expected",expected).put("actual",actual).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun empty(){scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()}
            empty();askConstruction(scenario,"What is the instantaneous gradient here?","At which x coordinate");askConstruction(scenario,"1.5","Which explicit function");restore()
            askConstruction(scenario,"y equals three x squared plus two x","slope = 11.0")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(listOf(Vec2(1.5,9.75)),RuhiDerivativeLanguage.drawing(s)!!.points);assertEquals(1,s.functions.size);assertTrue(RuhiDialogueMemory.read(s).turns.any{it.user=="y equals three x squared plus two x"})}
            captureCanvas(matcher,"formula-word-derivative-marker").recycle();captureDistanceReply("slope = 11.0","formula-word-derivative-answer")
            empty();askConstruction(scenario,"Shade area under the currently selected curve between 1 and 4","Which explicit function");restore();askConstruction(scenario,"x squared","Signed integral =")
            scenario.onActivity{val s=it.verificationViewModel.state;val d=RuhiIntegralLanguage.drawing(s)!!;assertEquals(21.0,d.values!!.signed,1e-7);assertEquals(1.0 to 4.0,d.bounds);assertEquals(256,d.bands.size);assertTrue(d.bands.flatMap{it.vertices}.all{it.x in 1.0..4.0});assertTrue(RuhiDialogueMemory.read(s).turns.any{it.user=="x squared"})}
            captureCanvas(matcher,"formula-word-integral-shading").recycle();captureDistanceReply("Signed integral =","formula-word-integral-answer")
        }
        File(folder,"formula-word-canvas.json").writeText(JSONObject().put("passed",true).put("cases",6).put("results",records).put("original_wording_preserved",true).put("saved_pending_roles",true).put("derivative_x",1.5).put("derivative_y",9.75).put("derivative_slope",11).put("integral_area",21).put("integral_bounds",JSONArray(listOf(1,4))).put("native_shading",true).put("committed_capture",true).toString(2))
    }
    @Test fun emptyIntegralWorkspacePreservesFormulaBoundsAndNativeShadingAcrossRestores() {
        TensorFlowGraphNlp.install(context)
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun empty(){scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()}
            empty();askConstruction(scenario,"Shade area under the currently selected curve between 1 and 4","Which explicit function")
            scenario.onActivity{assertTrue(it.verificationViewModel.state.functions.isEmpty());assertEquals("integral_formula",RuhiDialogueMemory.read(it.verificationViewModel.state).pending!!.kind)};restore()
            captureDistanceReply("Which explicit function","integral-formula-question")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("y=x^2",substring=false).onLast().performScrollTo().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.startsWith("Signed integral")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(21.0,RuhiIntegralLanguage.drawing(vm.state)!!.values!!.signed,1e-7);assertEquals(1,vm.state.functions.size);assertEquals(1.0 to 4.0,RuhiIntegralLanguage.drawing(vm.state)!!.bounds);vm.undo();assertTrue(vm.state.functions.isEmpty());assertNull(RuhiIntegralLanguage.drawing(vm.state));vm.redo();assertEquals(21.0,RuhiIntegralLanguage.drawing(vm.state)!!.values!!.signed,1e-7)};compose.waitForIdle()
            var completed:WorkspaceState?=null
            scenario.onActivity{val vm=it.verificationViewModel;completed=vm.state;vm.importWorkspace(vm.state.copy(labSessionValues=vm.state.labSessionValues-RuhiIntegralLanguage.MARK),false,emptyList())};compose.waitForIdle();val unshaded=captureCanvas(matcher,"integral-formula-unshaded")
            scenario.onActivity{it.verificationViewModel.importWorkspace(completed!!,false,emptyList())};compose.waitForIdle();val shaded=captureCanvas(matcher,"integral-formula-shaded")
            assertEquals(unshaded.width,shaded.width);assertEquals(unshaded.height,shaded.height)
            val a=IntArray(unshaded.width*unshaded.height);val b=IntArray(a.size);unshaded.getPixels(a,0,unshaded.width,0,0,unshaded.width,unshaded.height);shaded.getPixels(b,0,shaded.width,0,0,shaded.width,shaded.height)
            val pixels=a.indices.count{a[it]!=b[it]};assertTrue("New integral shading did not render",pixels>100);unshaded.recycle();shaded.recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){it.copy(expression="y=3*x^2")};assertEquals(63.0,RuhiIntegralLanguage.drawing(vm.state)!!.values!!.signed,1e-7)};restore();captureCanvas(matcher,"integral-formula-edited").recycle()
            empty();askConstruction(scenario,"Integrate the selected function","Which explicit function");restore();askConstruction(scenario,"y=x^2","What lower and upper x bounds")
            scenario.onActivity{assertTrue(it.verificationViewModel.state.functions.isEmpty());assertEquals("integral_bounds",RuhiDialogueMemory.read(it.verificationViewModel.state).pending!!.kind)};restore()
            askConstruction(scenario,"10001 to 2","Use finite integration bounds");scenario.onActivity{assertTrue(it.verificationViewModel.state.functions.isEmpty())};restore()
            askConstruction(scenario,"0 to 2","Signed integral =")
            scenario.onActivity{val state=it.verificationViewModel.state;assertEquals(8.0/3,RuhiIntegralLanguage.drawing(state)!!.values!!.signed,1e-7);assertEquals(1,state.functions.size);assertTrue(state.points.isEmpty());assertNull(RuhiDialogueMemory.read(state).pending)}
            captureDistanceReply("Signed integral =","integral-formula-answer")
            File(folder,"integral-formula-canvas.json").writeText(JSONObject().put("passed",true).put("source_rows",JSONArray(listOf(364,372))).put("restored_formula_chip",true).put("bounds_preserved",true).put("required_bounds_resumed",true).put("invalid_bounds_no_creation",true).put("atomic_undo_redo",true).put("native_function_created",true).put("shading_pixels",pixels).put("initial_area",21).put("edited_area",63).put("required_bounds_area",8.0/3).put("committed_capture",true).toString(2))
        }
    }
    @Test fun emptyDerivativeWorkspaceResumesSavedFormulaAndDomainQuestionsAtomically() {
        TensorFlowGraphNlp.install(context)
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun empty(){scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()}
            empty();askConstruction(scenario,"Plot the derivative of the selected curve","Which explicit function")
            scenario.onActivity{val s=it.verificationViewModel.state;assertTrue(s.functions.isEmpty());assertEquals("derivative_formula",RuhiDialogueMemory.read(s).pending!!.kind);assertEquals(3,RuhiDialogueMemory.read(s).lastChoices.size)}
            restore();compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("y=x^3",substring=false).onLast().performScrollTo().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.startsWith("Derivative")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(1,vm.state.functions.size);val q=RuhiDerivativeLanguage.decode(vm.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals(vm.state.functions.single().id,q.source);assertEquals(12.0,RuhiDerivativeMath.slope(RuhiDerivativeLanguage.resolve(vm.state,q),2.0).value,1e-8)}
            captureCanvas(matcher,"derivative-formula-created").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.undo();assertTrue(vm.state.functions.isEmpty());assertFalse(vm.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));vm.redo();assertEquals(1,vm.state.functions.size)};compose.waitForIdle()
            empty();askConstruction(scenario,"What is the instantaneous gradient here?","At which x coordinate");askConstruction(scenario,"0","Which explicit function");restore()
            captureDistanceReply("Which explicit function","derivative-formula-question")
            askConstruction(scenario,"y=ln(x)","At which x coordinate")
            scenario.onActivity{val s=it.verificationViewModel.state;assertTrue(s.functions.isEmpty());assertEquals("derivative_x",RuhiDialogueMemory.read(s).pending!!.kind);assertTrue(RuhiDialogueMemory.read(s).lastChoices.all{it.label.substringAfter('=').trim().toDouble()>0})};restore()
            askConstruction(scenario,"2","slope = 0.5")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(1,s.functions.size);assertEquals(listOf(Vec2(2.0,ln(2.0))),RuhiDerivativeLanguage.drawing(s)!!.points);assertNull(RuhiDialogueMemory.read(s).pending)}
            val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(rendered,rendered.contains("(2.0, ${ln(2.0)})"))
            captureDistanceReply("slope = 0.5","derivative-formula-answer")
        }
        File(folder,"derivative-formula-canvas.json").writeText(JSONObject().put("passed",true).put("restored_formula_chip",true).put("native_function_created",true).put("atomic_undo_redo",true).put("undefined_x_reasked_before_creation",true).put("actual_rendered_coordinates",true).put("restored_x",2).put("log_slope",.5).put("committed_capture",true).toString(2))
    }
    @Test fun criticalPointsDistinguishCornersFromStationaryAndFollowCurrentCurveEdits() {
        TensorFlowGraphNlp.install(context)
        val inputs=listOf(
            "Find critical points of y = x^4 - 2*x^2" to listOf(Vec2(-1.0,-1.0),Vec2(0.0,0.0),Vec2(1.0,-1.0)),
            "Find critical points of y = abs(x)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = abs(x-2)+2*x" to listOf(Vec2(2.0,4.0)),
            "Find critical points of y = abs(x^2)" to listOf(Vec2(0.0,0.0)),
            "Find the stationary points of abs(x)" to emptyList(),
            "Find critical points of y = 1/x" to emptyList())
        val records=JSONArray()
        for((source,expected) in inputs) {
            val state=RuhiSeed500Fixture.state("GRAPH_2D",344)
            val r=RuhiDialogueManager().execute(state,source,WorkspaceHistoryByModule(),RuhiSeed500Fixture.context(344,state))
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val actual=RuhiDerivativeLanguage.drawing(r.state)!!.points
            assertEquals(source,expected.size,actual.size)
            assertTrue(source,expected.all{e->actual.any{it.distanceTo(e)<1e-6}})
            records.put(JSONObject().put("input",source).put("expected_points",JSONArray(expected.map{listOf(it.x,it.y)})).put("actual_points",JSONArray(actual.map{listOf(it.x,it.y)})).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("corner-907","Corner907","y=abs(x)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points")
            fun point(expected:Vec2) {
                scenario.onActivity{assertEquals(listOf(expected),RuhiDerivativeLanguage.drawing(it.verificationViewModel.state)!!.points);assertTrue(it.verificationViewModel.state.points.isEmpty())}
                val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
                assertTrue(rendered,rendered.contains("(${expected.x}, ${expected.y})"))
            }
            point(Vec2(0.0,0.0));captureCanvas(matcher,"critical-point-corner").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){f->f.copy(expression="y=abs(x-2)+2*x")};vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            point(Vec2(2.0,4.0));captureCanvas(matcher,"critical-point-after-edit").recycle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput()
            compose.onAllNodesWithText("(2.0, 4.0)",substring=true).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"critical-point-answer").recycle()
            compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
        }
        File(folder,"critical-point-canvas.json").writeText(JSONObject().put("passed",true).put("cases",6).put("results",records).put("actual_rendered_coordinates",true).put("saved_current_curve",true).put("committed_capture",true).put("initial_point",JSONArray(listOf(0,0))).put("edited_point",JSONArray(listOf(2,4))).toString(2))
    }
    @Test fun lexicalPointLabelsKeepTensorFlowRolesAndMathematicalDispatchInBothStudios() {
        TensorFlowGraphNlp.install(context)
        val labels=listOf("distance","point","line","slope","and","from","midpoint","gradient","is","circle","triangle","angle","Vertex907","alpha","Δelta42","点甲","origin")
        val records=JSONArray()
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(name in labels)for(source in listOf("distance between $name and Omega811","how far is $name from Omega811")) {
            val intent=RuhiLanguageServices.semanticClassifier!!.predict(RuhiSeedNlpInput.normalize(source));assertEquals("MEASURE_DISTANCE",intent.intent);assertTrue(intent.confidence>=.9f&&intent.margin>=.4f)
            val roles=RuhiLanguageServices.pointSlotClassifier!!.predict(source).filter{it.confidence>=.9f&&it.margin>=.4f}
            assertEquals(source,name,roles.single{it.role=="POINT_1"}.value);assertEquals("Omega811",roles.single{it.role=="POINT_2"}.value)
            val initial=WorkspaceState(module=module,points=listOf(Vec2(1.0,2.0),Vec2(4.0,6.0)),points3D=listOf(Point3D("lexical-left",name,Vec3(1.0,2.0,3.0)),Point3D("lexical-right","Omega811",Vec3(4.0,6.0,15.0))),labSessionValues=mapOf("ruhi.studio.point.$name" to "0","ruhi.studio.point.Omega811" to "1"))
            val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(initial)).state!!;val history=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(restored,source,history)
            val expected=if(module==MathModule.Geometry3D)13.0 else 5.0
            assertEquals(source,GraphCommandStatus.APPLIED,r.status);assertEquals(expected,(r.semanticFrame!!.parameters["distance_native"] as Number).toDouble(),1e-10)
            assertEquals(initial.points,r.state.points);assertEquals(initial.points3D,r.state.points3D);assertTrue(history.forModule(module).protocol.isEmpty())
            records.put(JSONObject().put("engine",module.name).put("name",name).put("source",source).put("distance",expected).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(1.0,2.0),Vec2(4.0,6.0)),labSessionValues=mapOf("ruhi.studio.point.slope" to "0","ruhi.studio.point.Omega811" to "1"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"distance between slope and Omega811","Distance slope to Omega811 = 5.0 units")
            scenario.onActivity{assertEquals(initial.points,it.verificationViewModel.state.points)}
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput()
            compose.onAllNodesWithText("Distance slope to Omega811 = 5.0 units",substring=true).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"lexical-point-distance-answer").recycle()
            compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
        }
        assertEquals(68,records.length())
        File(folder,"lexical-point-native.json").writeText(JSONObject().put("passed",true).put("cases",records.length()).put("tensorflow_roles_above_gate",true).put("both_studios",true).put("visible_slope_named_point_distance",5).put("points_and_history_preserved",true).put("committed_capture",true).put("results",records).toString(2))
    }
    @Test fun absoluteValueExplanationsUseExplicitAndRestoredCoordinatesOnTheRealCanvas() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val functions=listOf(FunctionDefinition("unrelated-log","Log","y=ln(x)","cyan"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,functions=functions,surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()
            fun point(x:Double) {
                scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(functions,s.functions);assertEquals(listOf(Vec2(x,kotlin.math.abs(x))),RuhiDerivativeLanguage.drawing(s)!!.points);assertTrue(s.points.isEmpty());assertNull(RuhiDialogueMemory.read(s).pending)}
                val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
                assertTrue(rendered,rendered.contains("($x, ${kotlin.math.abs(x)})"))
            }
            fun reply(expected:String,name:String) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput()
                compose.onAllNodesWithText(expected,substring=true).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,name).recycle()
                compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            askConstruction(scenario,"Explain why absolute value is not differentiable at x = 2","is differentiable at x=2.0");point(2.0)
            captureCanvas(matcher,"absolute-value-positive-marker").recycle();reply("is differentiable at x=2.0","absolute-value-positive-answer")
            askConstruction(scenario,"Explain why absolute value is not differentiable at minus three","is differentiable at x=-3.0");point(-3.0)
            captureCanvas(matcher,"absolute-value-negative-marker").recycle();reply("derivative is -1.0","absolute-value-negative-answer")
            askConstruction(scenario,"Explain why absolute value is not differentiable","At which x coordinate")
            scenario.onActivity{val vm=it.verificationViewModel;assertTrue(RuhiDialogueMemory.read(vm.state).lastChoices.any{c->c.label=="x = 0"});vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"2","is differentiable at x=2.0");point(2.0)
            askConstruction(scenario,"Explain why absolute value is not differentiable at zero","right-hand slope is 1");point(0.0)
            reply("not differentiable at zero","absolute-value-zero-answer")
            File(folder,"absolute-value-canvas.json").writeText(JSONObject().put("passed",true).put("explicit_positive_x",2).put("explicit_positive_slope",1).put("spoken_negative_x",-3).put("negative_slope",-1).put("restored_missing_x",2).put("zero_cusp_preserved",true).put("actual_rendered_coordinates",true).put("unrelated_curve_preserved",true).put("committed_capture",true).toString(2))
        }
    }
    @Test fun nativePointDistanceCentimetersMatchSelectedSegmentAndCurrentCoordinates() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,0.0)),shapes=listOf(Shape2D("distance,/907",Shape2DType.Segment,listOf(0,1))),labSessionValues=mapOf("ruhi.studio.point.Left_907" to "0","ruhi.studio.point.Right_811" to "1"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectShape(0)};compose.waitForIdle()
            askConstruction(scenario,"Use meters for coordinate units","Displaying measurements in m")
            askConstruction(scenario,"Find the distance between Left_907 and Right_811","= 6.0 m.")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectShape(0)};compose.waitForIdle()
            askConstruction(scenario,"Use centimeters instead","Distance Left_907 to Right_811 = 600.0 cm")
            val label=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(label,label.contains("length 600 cm"))
            scenario.onActivity{assertEquals(initial.points,it.verificationViewModel.state.points);assertEquals(initial.shapes,it.verificationViewModel.state.shapes)}
            captureCanvas(matcher,"point-distance-segment-centimeters").recycle();captureDistanceReply("Distance Left_907 to Right_811 = 600.0 cm","point-distance-answer-2d")
            askConstruction(scenario,"Find the distance between Left_907 and Right_811","= 600.0 cm.")
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(GeometryMeasurementUnits.Length.METER,GeometryMeasurementUnits.read(it.verificationViewModel.state).display);assertEquals(initial.points,it.verificationViewModel.state.points)}
            askConstruction(scenario,"Use centimeters instead","= 600.0 cm")
            scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(3.0,4.0))};compose.waitForIdle()
            askConstruction(scenario,"Use centimeters instead","= 500.0 cm")
            askConstruction(scenario,"Find the distance between Left_907 and Right_811","= 500.0 cm.")
            val editedLabel=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(editedLabel.contains("length 500 cm"))
            captureCanvas(matcher,"point-distance-segment-parent-edit").recycle()
            File(folder,"point-distance-units-2d.json").writeText(JSONObject().put("passed",true).put("restored_pair",true).put("immediate_display_distance",600).put("edited_distance",500).put("segment_label",label).put("edited_segment_label",editedLabel).put("conversion_preserved_coordinates",true).put("query_did_not_add_undo",true).toString(2))
        }
    }
    @Test fun nativeThreeDimensionalPointDistanceConvertsAllComponentsAndLiveParentEdit() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val points=listOf(Point3D("left,/907","Left_907",Vec3(0.0,0.0,0.0)),Point3D("right,/811","Right_811",Vec3(1.0,2.0,2.0)))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D,points3D=points),false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Use meters for coordinate units","Displaying measurements in m")
            askConstruction(scenario,"Find the distance between Left_907 and Right_811","= 3.0 m.")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Use centimeters instead","Distance Left_907 to Right_811 = 300.0 cm")
            scenario.onActivity{assertEquals(points,it.verificationViewModel.state.points3D)}
            captureDistanceReply("Distance Left_907 to Right_811 = 300.0 cm","point-distance-answer-3d")
            scenario.onActivity{it.verificationViewModel.transformPoint3D(1){p->p.copy(position=Vec3(2.0,3.0,6.0))}};compose.waitForIdle()
            askConstruction(scenario,"Use centimeters instead","Distance Left_907 to Right_811 = 700.0 cm")
            askConstruction(scenario,"Find the distance between Left_907 and Right_811","= 700.0 cm.")
            captureDistanceReply("Distance Left_907 to Right_811 = 700.0 cm","point-distance-answer-3d-parent-edit")
            scenario.onActivity{assertEquals(Vec3(2.0,3.0,6.0),it.verificationViewModel.state.points3D[1].position);assertEquals(points[0],it.verificationViewModel.state.points3D[0])}
            File(folder,"point-distance-units-3d.json").writeText(JSONObject().put("passed",true).put("restored_pair",true).put("immediate_display_distance",300).put("edited_distance",700).put("conversion_preserved_coordinates",true).put("all_three_components",true).toString(2))
        }
    }
    @Test fun nativeCentimetersConvertTriangleLabelsAndFollowCurrentParentEdit() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(3.0,0.0),Vec2(0.0,4.0)),shapes=listOf(Shape2D("units,/907",Shape2DType.Triangle,listOf(0,1,2))))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectShape(0)};compose.waitForIdle()
            askConstruction(scenario,"Use meters for coordinate units","Displaying measurements in m")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectShape(0)};compose.waitForIdle()
            val before=captureCanvas(matcher,"units-triangle-meters")
            askConstruction(scenario,"Use centimeters instead","area 60000 cm²")
            val label=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
            assertEquals("Selected measurements: area 60000 cm² - perimeter 1200 cm",label)
            scenario.onActivity{assertEquals(initial.points,it.verificationViewModel.state.points);assertEquals(initial.shapes,it.verificationViewModel.state.shapes)}
            val after=captureCanvas(matcher,"units-triangle-centimeters");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length.METER,GeometryMeasurementUnits.read(it.verificationViewModel.state).display);assertEquals(initial.points,it.verificationViewModel.state.points)}
            askConstruction(scenario,"Use centimeters instead","perimeter 1200 cm")
            scenario.onActivity{it.verificationViewModel.movePoint(2,Vec2(0.0,5.0));assertEquals(Vec2(0.0,5.0),it.verificationViewModel.state.points[2])};compose.waitForIdle()
            assertTrue(compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription].contains("area 75000 cm²"))
            captureCanvas(matcher,"units-triangle-parent-edit").recycle()
            File(folder,"units-triangle-canvas.json").writeText(JSONObject().put("passed",true).put("base_unit","m").put("display_unit","cm").put("area",60000).put("perimeter",1200).put("edited_area",75000).put("label",label).put("coordinates_unchanged",true).put("undo_restored",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativeCentimetersConvertCubeAreaVolumeAndDimensionLinesWithoutResizing() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=listOf(Solid(SolidType.Cube,2.0))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D,solids=initial),false,emptyList());it.verificationViewModel.selectSolid(0)};compose.waitForIdle()
            askConstruction(scenario,"Find surface area and volume of the selected solid","Cube surface area")
            askConstruction(scenario,"Use meters for coordinate units","Displaying measurements in m")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectSolid(0)};compose.waitForIdle()
            val before=captureCanvas(matcher,"units-cube-meters")
            askConstruction(scenario,"Use centimeters instead","volume 8000000 cm³")
            val label=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
            assertTrue(label,label.contains("240000.0 cm²"));assertTrue(label,label.contains("8000000 cm³"));assertTrue(label,label.contains("w = 200 cm"))
            scenario.onActivity{assertEquals(initial,it.verificationViewModel.state.solids);assertEquals(com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length.METER,GeometryMeasurementUnits.read(it.verificationViewModel.state).base)}
            val after=captureCanvas(matcher,"units-cube-centimeters");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length.METER,GeometryMeasurementUnits.read(it.verificationViewModel.state).display);assertEquals(initial,it.verificationViewModel.state.solids)}
            askConstruction(scenario,"Use centimeters instead","volume 8000000 cm³");askConstruction(scenario,"Scale the cube by 1.5","Applied 3D scale")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(3.0,s.solids.single().width,1e-9);val q=RuhiSolidMeasureLanguage.drawing(s)!!.entities.single().quantities.first{it.name=="volume"};assertEquals(27.0,q.value,1e-9);assertEquals(27000000.0,q.value*GeometryMeasurementUnits.read(s).factor(3),1e-6)}
            captureCanvas(matcher,"units-cube-parent-edit").recycle()
            File(folder,"units-cube-canvas.json").writeText(JSONObject().put("passed",true).put("base_unit","m").put("display_unit","cm").put("surface_area",240000).put("volume",8000000).put("side",200).put("edited_volume",27000000).put("label",label).put("native_geometry_unchanged",true).put("undo_restored",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativePreserveCubeConstrainsRestoredScaleAndUndoesOnlySafeEdit() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=listOf(Solid(SolidType.Cube,2.0,position=Vec3(-2.0,0.0,0.0)),Solid(SolidType.Sphere,4.0,position=Vec3(3.0,0.0,0.0)))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D,solids=initial,labSessionValues=mapOf("ruhi.studio.appearance.0" to "blue")),false,emptyList());it.verificationViewModel.selectSolid(-1)};compose.waitForIdle()
            askConstruction(scenario,"Scale it by 1.5","Which object do you mean?")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(initial,vm.state.solids);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectSolid(-1)};compose.waitForIdle()
            val before=captureCanvas(matcher,"dialogue-preserve-before")
            askConstruction(scenario,"Do not change the existing cube","Kept Cube 1 unchanged")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(initial[0],s.solids[0]);assertEquals("blue",s.labSessionValues["ruhi.studio.appearance.0"]);assertEquals(6.0,s.solids[1].width,1e-9);assertEquals(3.0,s.solids[1].radius,1e-9);assertEquals(36*PI,RuhiSolidMeasureMath.quantity(s.solids[1],"volume").value,1e-9);assertEquals("Do not change the existing cube",RuhiDialogueMemory.read(s).turns.last().user);assertNull(RuhiDialogueMemory.read(s).pending)}
            val after=captureCanvas(matcher,"dialogue-preserve-scaled");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(initial,it.verificationViewModel.state.solids)}
            File(folder,"dialogue-preserve-canvas.json").writeText(JSONObject().put("passed",true).put("restored_pending",true).put("cube_unchanged",true).put("cube_color","blue").put("sphere_radius",3).put("sphere_volume",36*PI).put("undo_restored",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativeOtherObjectReusesValidatedScaleAfterRestoreAndUndoesOnlyOtherEdit() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=listOf(Solid(SolidType.Cube,2.0,position=Vec3(-2.0,0.0,0.0)),Solid(SolidType.Cuboid,3.0,4.0,5.0,position=Vec3(3.0,0.0,0.0)))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D,solids=initial),false,emptyList());it.verificationViewModel.selectSolid(0)};compose.waitForIdle()
            askConstruction(scenario,"Scale the cube by 1.5","Applied 3D scale")
            var first=initial[0];scenario.onActivity{val vm=it.verificationViewModel;first=vm.state.solids[0];assertEquals(27.0,RuhiSolidMeasureMath.quantity(first,"volume").value,1e-9);assertEquals(initial[1],vm.state.solids[1]);assertEquals("scale",RuhiActionContext.read(vm.state)!!.kind);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectSolid(0)};compose.waitForIdle()
            val before=captureCanvas(matcher,"dialogue-other-before")
            askConstruction(scenario,"What about the other one","Applied 3D scale")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(first,s.solids[0]);assertEquals(initial[1].copy(width=4.5,height=6.0,depth=7.5,radius=2.25,topRadius=1.125),s.solids[1]);assertEquals(202.5,RuhiSolidMeasureMath.quantity(s.solids[1],"volume").value,1e-9);assertEquals("What about the other one",RuhiDialogueMemory.read(s).turns.last().user)}
            val after=captureCanvas(matcher,"dialogue-other-scaled");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(first,it.verificationViewModel.state.solids[0]);assertEquals(initial[1],it.verificationViewModel.state.solids[1])}
            File(folder,"dialogue-other-canvas.json").writeText(JSONObject().put("passed",true).put("restored_task",true).put("scale",1.5).put("first_volume",27).put("other_volume",202.5).put("first_preserved",true).put("undo_restored",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativeCompletedBlueCorrectionRestoresWrongEditAfterReloadAndUndoesAtomically() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,1.0,position=Vec3(-2.0,0.0,0.0)),Solid(SolidType.Cube,1.25,position=Vec3(0.0,-1.0,0.0))),labSessionValues=mapOf("ruhi.studio.appearance.0" to "red","ruhi.studio.appearance.1" to "blue"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectSolid(-1)};compose.waitForIdle()
            askConstruction(scenario,"Move the cube to (3, 2, 1)","Which solid should I transform?")
            askConstruction(scenario,"Choose the first suggested answer","Applied 3D position")
            val wrong=initial.solids.toMutableList().apply{this[0]=this[0].copy(position=Vec3(3.0,2.0,1.0))}.toList()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(wrong,vm.state.solids);assertNull(RuhiDialogueMemory.read(vm.state).pending);assertTrue(vm.state.labSessionValues.containsKey(RuhiAppliedTargetCorrection.KEY));vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectSolid(-1)};compose.waitForIdle()
            val before=captureCanvas(matcher,"dialogue-blue-applied-wrong")
            askConstruction(scenario,"Actually I meant the blue one","Restored the original object.")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(initial.solids[0],s.solids[0]);assertEquals(initial.solids[1].copy(position=Vec3(3.0,2.0,1.0)),s.solids[1]);assertEquals(initial.labSessionValues["ruhi.studio.appearance.0"],s.labSessionValues["ruhi.studio.appearance.0"]);assertEquals(initial.labSessionValues["ruhi.studio.appearance.1"],s.labSessionValues["ruhi.studio.appearance.1"]);assertNull(RuhiDialogueMemory.read(s).pending);assertEquals("Actually I meant the blue one",RuhiDialogueMemory.read(s).turns.last().user)}
            val after=captureCanvas(matcher,"dialogue-blue-applied-corrected");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            captureDistanceReply("Restored the original object.","dialogue-blue-applied-answer")
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(wrong,it.verificationViewModel.state.solids)}
            askConstruction(scenario,"Redo","Redid");scenario.onActivity{assertEquals(initial.solids[0],it.verificationViewModel.state.solids[0]);assertEquals(Vec3(3.0,2.0,1.0),it.verificationViewModel.state.solids[1].position)}
            File(folder,"dialogue-blue-applied-canvas.json").writeText(JSONObject().put("passed",true).put("restored_completed_action",true).put("red_restored_exactly",true).put("blue_position",JSONArray(listOf(3,2,1))).put("blue_original_size",1.25).put("appearance_preserved",true).put("atomic_undo_redo",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativeBlueTargetCorrectionRestoresPendingMovementAndPreservesRedCube() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0)),labSessionValues=mapOf("ruhi.studio.appearance.0" to "red","ruhi.studio.appearance.1" to "blue"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectSolid(-1)};compose.waitForIdle()
            askConstruction(scenario,"Move the cube to (3, 2, 1)","Which solid should I transform?")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(initial.solids,vm.state.solids);assertEquals("transform_solid",RuhiDialogueMemory.read(vm.state).pending!!.kind);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectSolid(-1)};compose.waitForIdle()
            val before=captureCanvas(matcher,"dialogue-blue-before")
            askConstruction(scenario,"Actually I meant the blue one","Applied 3D position")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(initial.solids[0],s.solids[0]);assertEquals(initial.solids[1].copy(position=Vec3(3.0,2.0,1.0)),s.solids[1]);assertNull(RuhiDialogueMemory.read(s).pending);assertEquals("Actually I meant the blue one",RuhiDialogueMemory.read(s).turns.last().user)}
            val after=captureCanvas(matcher,"dialogue-blue-moved");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(initial.solids,it.verificationViewModel.state.solids)}
            File(folder,"dialogue-blue-canvas.json").writeText(JSONObject().put("passed",true).put("restored_pending",true).put("blue_position",JSONArray(listOf(3,2,1))).put("red_unchanged",true).put("undo_restored",true).put("changed_pixels",pixels).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun nativeConstructionExplanationsStepsMeasurementsAndPlacementUseCurrentDependencies() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,0.0),Vec2(3.0,3.0),Vec2(0.0,6.0),Vec2(20.0,30.0)),shapes=listOf(Shape2D("roof,/907",Shape2DType.Triangle,listOf(0,1,2),"Roof_811")),pointDependencies=listOf(PointDependency(2,listOf(1,3),PointDependencyType.Midpoint)),labSessionValues=mapOf(RuhiSceneEditLanguage.PREFIX+"measure.roof,/907" to "false","ruhi.studio.point.Start_907" to "0","ruhi.studio.point.End_811" to "1","ruhi.studio.point.Half_662" to "2","ruhi.studio.point.Parent_553" to "3","ruhi.studio.point.Unrelated_442" to "4"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Explain the shape we are viewing","area 9.0 square units")
            askConstruction(scenario,"Show me the drawing step by step","midpoint of End_811, Parent_553")
            scenario.onActivity{val s=it.verificationViewModel.state;val reply=RuhiDialogueMemory.read(s).turns.last().assistant;assertFalse(reply.contains("Unrelated_442"));assertEquals(initial.points,s.points);assertEquals(initial.shapes,s.shapes)}
            assertEquals("Selected measurements hidden",compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription])
            val hidden=captureCanvas(matcher,"construction-measurements-hidden")
            askConstruction(scenario,"Now show the measurements","Showing measurements")
            scenario.onActivity{val s=it.verificationViewModel.state;assertTrue(RuhiSceneEditLanguage.measurements(s,"roof,/907"));assertEquals(initial.points,s.points);assertEquals(initial.shapes,s.shapes)}
            val visibleMeasure=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
            assertEquals("Selected measurements: area 9 - perimeter 14.485281",visibleMeasure)
            val shown=captureCanvas(matcher,"construction-measurements-shown");val a=IntArray(hidden.width*hidden.height);val b=IntArray(a.size);hidden.getPixels(a,0,hidden.width,0,0,hidden.width,hidden.height);shown.getPixels(b,0,shown.width,0,0,shown.width,shown.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue("Measurements did not appear",pixels>20)
            scenario.onActivity{it.verificationViewModel.movePoint(3,Vec2(0.0,10.0));assertEquals(Vec2(3.0,5.0),it.verificationViewModel.state.points[2])};compose.waitForIdle()
            askConstruction(scenario,"Explain the shape we are viewing","area 15.0 square units")
            assertEquals("Selected measurements: area 15 - perimeter 17.661904",compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription])
            askConstruction(scenario,"Show me the drawing step by step","(3.0, 5.0)")
            var original:WorkspaceState=initial;scenario.onActivity{original=it.verificationViewModel.state}
            askConstruction(scenario,"Draw it over there","copy's center");askConstruction(scenario,"(8,4)","Drew an independent copy")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(original.points,s.points.take(original.points.size));assertEquals(original.shapes,s.shapes.take(original.shapes.size));val clone=s.shapes.last();assertNotEquals("roof,/907",clone.id);val p=clone.pointIndices.map{s.points[it]};assertEquals(8.0,p.map{it.x}.average(),1e-9);assertEquals(4.0,p.map{it.y}.average(),1e-9);assertEquals(15.0,abs(Geometry2D.polygonArea(p)),1e-9);assertTrue(clone.pointIndices.none{it in original.shapes.single().pointIndices})}
            captureCanvas(matcher,"construction-copy-at-answer").recycle()
            askConstruction(scenario,"Undo","Undid")
            scenario.onActivity{assertEquals(original.points,it.verificationViewModel.state.points);assertEquals(original.shapes,it.verificationViewModel.state.shapes)}
            File(folder,"construction-2d-canvas.json").writeText(JSONObject().put("passed",true).put("initial_area",9).put("edited_area",15).put("actual_dependency_steps",true).put("measurement_label",visibleMeasure).put("type_aware_measurements",true).put("measurement_pixels",pixels).put("copy_center",JSONArray(listOf(8,4))).put("independent_copy",true).put("undo_restored",true).toString(2));hidden.recycle();shown.recycle()
        }
    }
    @Test fun nativeSolidPronounsExplanationsMeasurementsAndPlacedCopyPreserveOtherObjects() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D),false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Create a cube","Created");askConstruction(scenario,"Create a sphere","Created");askConstruction(scenario,"Create a cube","Created")
            var untouched:List<Solid> = emptyList();scenario.onActivity{untouched=it.verificationViewModel.state.solids.take(2)}
            askConstruction(scenario,"Make it larger","Applied 3D scale");askConstruction(scenario,"Rotate that","Applied 3D rotate");askConstruction(scenario,"Move the cube to (3,2,1)","Applied 3D position")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(untouched,s.solids.take(2));assertEquals(2.5,s.solids[2].width,1e-9);assertEquals(Vec3(0.0,45.0,0.0),s.solids[2].rotation);assertEquals(Vec3(3.0,2.0,1.0),s.solids[2].position)}
            askConstruction(scenario,"Explain the shape we are viewing","volume 15.625 cubic units");askConstruction(scenario,"Now show the measurements","Showing measurements")
            var before:List<Solid> = emptyList();scenario.onActivity{val s=it.verificationViewModel.state;before=s.solids;val q=RuhiSolidMeasureLanguage.decode(s.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK));assertEquals(listOf(2),q.indices);assertEquals(15.625,RuhiSolidMeasureLanguage.compute(s,q).entities.single().quantities.single{v->v.name=="volume"}.value,1e-9)}
            captureCanvas(matcher,"construction-solid-measurements").recycle()
            askConstruction(scenario,"Draw it over there","copy's center");askConstruction(scenario,"(-4,2,1)","Drew an independent copy")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(before,s.solids.take(3));assertEquals(before[2].copy(position=Vec3(-4.0,2.0,1.0)),s.solids[3])}
            captureCanvas(matcher,"construction-solid-copy-at-answer").recycle()
            askConstruction(scenario,"Undo","Undid");scenario.onActivity{assertEquals(before,it.verificationViewModel.state.solids)}
            File(folder,"construction-3d-canvas.json").writeText(JSONObject().put("passed",true).put("last_solid_width",2.5).put("default_rotation_y",45).put("natural_position",JSONArray(listOf(3,2,1))).put("current_volume",15.625).put("unchanged_other_solids",true).put("independent_copy",true).put("undo_restored",true).toString(2))
        }
    }
    @Test fun previousGraphComparisonUsesExistingIdentitiesAndLiveFormulasThroughTheAssistant() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D),false,emptyList())};compose.waitForIdle()
            fun ask(text:String,reply:String="") {
                var count=0;scenario.onActivity{count=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.size}
                compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextClearance();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextInput(text);awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var done=false;scenario.onActivity{val turns=RuhiDialogueMemory.read(it.verificationViewModel.state).turns;done=turns.size>count&&turns.last().user==text&&turns.last().assistant.contains(reply)};done}
                androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            ask("Plot y = x^2","Plotted");ask("Plot y = 2*x+3","Plotted")
            var first="";var second="";var before:List<FunctionDefinition> = emptyList()
            scenario.onActivity{val vm=it.verificationViewModel;first=vm.state.functions[0].id;second=vm.state.functions[1].id;vm.updateFunction(0){it.copy(name="Old_907",expression="y=x^2+7")};vm.updateFunction(1){it.copy(name="Current_811")};vm.importWorkspace(vm.state.copy(functions=vm.state.functions.reversed()),false,emptyList());before=vm.state.functions;assertEquals(listOf(first,second),RuhiDialogueMemory.read(vm.state).graphTimeline)};compose.waitForIdle()
            ask("Compare this with the previous graph","first minus second at x = 0 is -4.0")
            scenario.onActivity{assertEquals(before,it.verificationViewModel.state.functions);assertEquals(2,it.verificationViewModel.state.functions.size)}
            captureCanvas(matcher,"dialogue-previous-graph").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;val index=vm.state.functions.indexOfFirst{f->f.id==first};vm.updateFunction(index){it.copy(expression="y=x^2+11")};before=vm.state.functions};compose.waitForIdle()
            ask("Compare this with the previous graph","first minus second at x = 0 is -8.0")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(before,s.functions);assertEquals(listOf(second,first),s.functions.map{it.id});assertEquals(listOf(first,second),RuhiDialogueMemory.read(s).graphTimeline)}
            captureCanvas(matcher,"dialogue-previous-graph-edited").recycle()
            File(folder,"dialogue-previous-graph-canvas.json").writeText(JSONObject().put("passed",true).put("existing_graphs",2).put("reordered_and_renamed",true).put("first_difference",-4).put("live_difference",-8).put("identities_preserved",true).put("no_replacement_graphs",true).toString(2))
        }
    }
    @Test fun conversationalSixtyDegreeEditMovesNativeArmAndKeepsArcLive() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(4.0,1.0),Vec2(1.0,1.0),Vec2(1.0,6.0),Vec2(8.0,-3.0),Vec2(1.0,3.5)),shapes=listOf(Shape2D("roof,/907",Shape2DType.Triangle,listOf(0,1,2))),pointDependencies=listOf(PointDependency(4,listOf(1,2),PointDependencyType.Midpoint)),labSessionValues=mapOf(RuhiAngleLanguage.PREFIX+"arc907" to "0,1,2|interior|","ruhi.studio.point.Outer_907" to "0","ruhi.studio.point.Vertex_811" to "1","ruhi.studio.point.Arm_662" to "2"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            val before=captureCanvas(matcher,"dialogue-angle-before")
            val text="Can we make the angle 60 degrees?"
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextClearance();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextInput(text);awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performImeAction()
            try{compose.waitUntil(30_000){var done=false;scenario.onActivity{val turn=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull();done=turn?.user==text&&turn.assistant.startsWith("Set the native angle")};done}}catch(error:Throwable){scenario.onActivity{val vm=it.verificationViewModel;File(folder,"dialogue-angle-failure-state.txt").writeText("Selected point=${vm.selectedPoint}; shape=${vm.selectedShape}; status=${vm.status}\n"+WorkspaceProjectCodec.encode(vm.state))};File(folder,"dialogue-angle-failure-semantics.txt").writeText(runCatching{compose.onRoot(useUnmergedTree=true).printToString()}.getOrElse{it.toString()});throw error}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val s=it.verificationViewModel.state;val u=s.points[0]-s.points[1];val v=s.points[2]-s.points[1];val actual=Math.toDegrees(acos((u.x*v.x+u.y*v.y)/(hypot(u.x,u.y)*hypot(v.x,v.y))));assertEquals(60.0,actual,1e-7);assertEquals(initial.points.take(2),s.points.take(2));assertEquals(initial.points[3],s.points[3]);assertEquals((s.points[1]+s.points[2])*.5,s.points[4]);assertEquals(5.0,hypot(v.x,v.y),1e-8);assertEquals(initial.shapes,s.shapes)}
            val edited=captureCanvas(matcher,"dialogue-angle-sixty")
            val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);edited.getPixels(b,0,edited.width,0,0,edited.width,edited.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue("Native angle canvas unchanged",pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(0,Vec2(5.0,2.0));val current=RuhiAngleLanguage.markDegrees(vm.state,RuhiAngleLanguage.marks(vm.state).single());assertTrue(abs(current-60)>1);val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!;assertEquals(current,RuhiAngleLanguage.markDegrees(saved,RuhiAngleLanguage.marks(saved).single()),1e-9)};compose.waitForIdle()
            captureCanvas(matcher,"dialogue-angle-parent-edit").recycle()
            File(folder,"dialogue-angle-canvas.json").writeText(JSONObject().put("passed",true).put("native_angle_degrees",60).put("dependent_midpoint",true).put("anchors_preserved",true).put("live_parent_edit",true).put("changed_pixels",pixels).toString(2))
            before.recycle();edited.recycle()
        }
    }
    @Test fun typedDimensionCorrectionsReuseValidatedRadiusThroughTheVisibleAssistant() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D),false,emptyList())};compose.waitForIdle()
            fun ask(text:String) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick()
                compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull()?.user==text};done}
                androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            ask("Create a sphere with radius 2.75")
            var solids:List<Solid> = emptyList();scenario.onActivity{solids=it.verificationViewModel.state.solids;assertEquals(2.75,solids.single().radius,0.0)}
            ask("Find the volume of a cylinder with height 6")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("radius",RuhiDialogueMemory.read(vm.state).pending!!.targets.first());vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            ask("Use the same radius as before")
            scenario.onActivity{val s=it.verificationViewModel.state;val q=RuhiSolidMeasureLanguage.decode(s.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK));assertEquals(2.75,q.dimensions.getValue("radius"),0.0);assertEquals(6.0,q.dimensions.getValue("height"),0.0);assertEquals(6*PI*2.75*2.75,RuhiSolidMeasureLanguage.compute(s,q).entities.single().quantities.single().value,1e-9);assertEquals(solids,s.solids)}
            captureCanvas(matcher,"dialogue-prior-radius").recycle()
            ask("Find the volume of a cone with radius 2")
            scenario.onActivity{assertEquals("height",RuhiDialogueMemory.read(it.verificationViewModel.state).pending!!.targets.first())}
            ask("No, make it 7 instead")
            scenario.onActivity{val s=it.verificationViewModel.state;val q=RuhiSolidMeasureLanguage.decode(s.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK));assertEquals(2.0,q.dimensions.getValue("radius"),0.0);assertEquals(7.0,q.dimensions.getValue("height"),0.0);assertEquals(28*PI/3,RuhiSolidMeasureLanguage.compute(s,q).entities.single().quantities.single().value,1e-9);assertNull(RuhiDialogueMemory.read(s).pending);assertEquals(solids,s.solids)}
            captureCanvas(matcher,"dialogue-corrected-height").recycle()
            File(folder,"dialogue-roles-canvas.json").writeText(JSONObject().put("passed",true).put("prior_native_radius",2.75).put("cylinder_height_preserved",6).put("corrected_cone_height",7).put("cone_radius_preserved",2).put("existing_native_solids_preserved",true).put("pending_restored",true).toString(2))
        }
    }
    @Test fun contextualAnswersResumeRestoredGraphTasksThroughTheVisibleAssistant() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D),false,emptyList())};compose.waitForIdle()
            fun ask(text:String) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick()
                compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull()?.user==text};done}
                androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            ask("Draw a graph")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("equation",RuhiDialogueMemory.read(vm.state).pending!!.kind);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            ask("Use x squared for the equation")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(1,s.functions.size);assertTrue(s.functions.single().expression.contains("x^2"));assertNull(RuhiDialogueMemory.read(s).pending)}
            captureCanvas(matcher,"dialogue-equation-resumed").recycle()
            ask("Compare two functions")
            var choices:List<RuhiAnswerChip> = emptyList()
            scenario.onActivity{val vm=it.verificationViewModel;choices=RuhiDialogueMemory.read(vm.state).lastChoices;assertEquals(3,choices.size);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            ask("What choices do I have?")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(choices,RuhiDialogueMemory.read(s).lastChoices);assertEquals(1,s.functions.size)}
            ask("Choose the second suggested answer")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(3,s.functions.size);assertTrue(s.functions.any{it.expression.contains("sin")});assertTrue(s.functions.any{it.expression.contains("cos")});assertNull(RuhiDialogueMemory.read(s).pending);assertEquals("Choose the second suggested answer",RuhiDialogueMemory.read(s).turns.last().user)}
            captureCanvas(matcher,"dialogue-comparison-resumed").recycle()
            File(folder,"dialogue-follow-up-canvas.json").writeText(JSONObject().put("passed",true).put("restored_equation_resumed",true).put("displayed_composite_choice_preserved",true).put("student_history_preserved",true).put("existing_graph_preserved",true).toString(2))
        }
    }
    @Test fun all150DistanceRowsChooseExistingAndMixedEndpointsInBothNativeStudios() {
        TensorFlowGraphNlp.install(context)
        val seeds=JSONArray(context.assets.open("ruhi_semantic_nlp/seed_examples.json").bufferedReader().use{it.readText()});val records=JSONArray()
        for(i in 0 until 150) {
            val row=seeds.getJSONObject(i);val source=row.getString("text");val params=row.getJSONObject("parameters");val a=params.getString("point1");val b=params.getString("point2")
            assertEquals("MEASURE_DISTANCE",RuhiLanguageServices.semanticClassifier!!.predict(RuhiSeedNlpInput.normalize(source)).intent)
            for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(mode in listOf("one_existing","both_existing","mixed_creation")) {
                val names=if(mode=="one_existing")listOf(a,"Gamma811")else listOf("Gamma811","Delta907")
                val initial=WorkspaceState(module=module,points=listOf(Vec2(0.0,0.0),Vec2(3.0,4.0)),points3D=names.mapIndexed{n,name->Point3D("choose-native-$n",name,Vec3(n*3.0,n*4.0,0.0))},labSessionValues=names.mapIndexed{n,name->"ruhi.studio.point.$name" to "$n"}.toMap())
                var s=initial;val history=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
                fun say(text:String)=manager.execute(s,text,history).also{s=it.state}
                fun restore(){s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!}
                val q=say(source);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);restore()
                if(mode=="one_existing") {
                    val choice=RuhiDialogueMemory.read(s).lastChoices.single{it.label=="Use Gamma811 for $b"}
                    val out=say(choice.answer);assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("Distance $a to Gamma811 = 5.0 units"))
                } else {
                    say("Use Gamma811 for $a");restore()
                    if(mode=="both_existing") {
                        val out=say("Use Delta907 for $b");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("Distance Gamma811 to Delta907 = 5.0 units"))
                    } else {
                        say("Create point $b at ");restore()
                        val out=say(if(module==MathModule.Geometry2D)"(6,8)"else"(6,8,0)");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("Distance Gamma811 to $b = 10.0 units"))
                        assertEquals(1,history.forModule(module).protocol.size);say("Undo")
                    }
                }
                assertEquals(initial.points,s.points);assertEquals(initial.points3D,s.points3D);assertTrue(history.forModule(module).protocol.isEmpty());assertNull(RuhiDialogueMemory.read(s).pending)
                records.put(JSONObject().put("id",row.getInt("id")).put("engine",module.name).put("scenario",mode).put("passed",true))
            }
        }
        assertEquals(900,records.length())
        File(folder,"choose-point-matrix.json").writeText(JSONObject().put("passed",true).put("source_rows",150).put("studios",2).put("workflows",records.length()).put("results",records).toString(2))
    }
    @Test fun existingPointChipSurvivesReloadAndShowsDistanceFromCurrentScene() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val points=listOf(Point3D("anchor-native","Anchor907",Vec3(0.0,0.0,0.0)),Point3D("candidate-native","Gamma811",Vec3(3.0,4.0,0.0)))
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry3D,points3D=points),false,emptyList())};compose.waitForIdle()
            for(distance in listOf(5,10)) {
                if(distance==10)scenario.onActivity{it.verificationViewModel.transformPoint3D(1){p->p.copy(position=Vec3(6.0,8.0,0.0))}}
                askConstruction(scenario,"Distance between Anchor907 and Missing53","is missing")
                scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
                compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput()
                val choice=compose.onNodeWithText("Use Gamma811 for Missing53");choice.performScrollTo().assertIsDisplayed()
                if(distance==5)captureCanvas(matcher,"choose-point-question").recycle()
                choice.performClick()
                val expected="Distance Anchor907 to Gamma811 = $distance.0 units"
                compose.waitUntil(30_000){var done=false;scenario.onActivity{val memory=RuhiDialogueMemory.read(it.verificationViewModel.state);done=memory.pending==null&&memory.turns.last().assistant.contains(expected)};done}
                compose.onAllNodesWithText(expected,substring=true).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"choose-point-answer-$distance").recycle()
                compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
                scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(2,s.points3D.size);assertEquals(listOf("Anchor907","Gamma811"),s.points3D.map{p->p.name});assertEquals(points[0],s.points3D[0]);assertEquals(Vec3(if(distance==5)3.0 else 6.0,if(distance==5)4.0 else 8.0,0.0),s.points3D[1].position)}
            }
            File(folder,"choose-point-canvas.json").writeText(JSONObject().put("passed",true).put("restored_chip_clicked",true).put("first_distance",5).put("current_distance",10).put("points_unchanged_by_choice",true).put("committed_capture",true).toString(2))
        }
    }
    @Test fun all150DistancePoliciesResumeRestoreRejectAndCancelInBothNativeStudios() {
        TensorFlowGraphNlp.install(context)
        val seeds=JSONArray(context.assets.open("ruhi_semantic_nlp/seed_examples.json").bufferedReader().use{it.readText()})
        val records=JSONArray();var correct=0
        for(i in 0 until 150) {
            val row=seeds.getJSONObject(i);assertEquals("MEASURE_DISTANCE",row.getString("intent"));val original=row.getString("text");val params=row.getJSONObject("parameters");val originalA=params.getString("point1");val originalB=params.getString("point2")
            assertEquals("MEASURE_DISTANCE",RuhiLanguageServices.semanticClassifier!!.predict(RuhiSeedNlpInput.normalize(original)).intent);correct++
            for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(mode in listOf("one_missing","both_missing","invalid_cancel","renamed")) {
                val a=if(mode=="renamed")"Vertex_907"else originalA;val b=if(mode=="renamed")"Omega_811"else originalB
                val source=if(mode=="renamed")original.replace(Regex("\\b${Regex.escape(originalA)}\\b"),a).replace(Regex("\\b${Regex.escape(originalB)}\\b"),b)else original
                assertEquals(RuhiSeedNlpInput.normalize(original),RuhiSeedNlpInput.normalize(source))
                val initial=if(mode=="both_missing")WorkspaceState(module=module)else WorkspaceState(module=module,points=listOf(Vec2(0.0,0.0)),points3D=listOf(Point3D("native,/907",a,Vec3(0.0,0.0,0.0))),labSessionValues=mapOf("ruhi.studio.point.$a" to "0"))
                var s=initial;val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
                fun say(text:String)=manager.execute(s,text,h).also{s=it.state}
                fun restore(){s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!}
                fun count()=if(module==MathModule.Geometry2D)s.points.size else s.points3D.size
                val q=say(source);assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals(initial.points,s.points);assertEquals(initial.points3D,s.points3D);assertTrue(h.forModule(module).protocol.isEmpty());restore()
                if(mode=="both_missing") {
                    assertEquals(setOf(a,b),q.dialogue!!.suggestions.map{it.label.removePrefix("Create point ")}.toSet());say(q.dialogue!!.suggestions.first().answer);say(if(module==MathModule.Geometry2D)"(0,0)"else"(0,0,0)");assertEquals(1,count());assertNotNull(RuhiDialogueMemory.read(s).pending);restore()
                }else assertEquals(listOf("Create point $b"),q.dialogue!!.suggestions.map{it.label})
                val choice=RuhiDialogueMemory.read(s).lastChoices.single();assertEquals("Create point $b",choice.label)
                val ask=say(choice.answer);assertEquals(GraphCommandStatus.CLARIFICATION,ask.status);restore()
                if(mode=="invalid_cancel") {
                    val bad=say(if(module==MathModule.Geometry2D)"(abc,4)"else"(abc,4,0)");assertEquals(GraphCommandStatus.INVALID,bad.status);assertTrue(bad.dialogue!!.pending);assertEquals(initial.points,s.points);assertEquals(initial.points3D,s.points3D);assertTrue(h.forModule(module).protocol.isEmpty());restore();say("Cancel that");assertNull(RuhiDialogueMemory.read(s).pending);assertEquals(initial.points,s.points);assertEquals(initial.points3D,s.points3D)
                }else {
                    val r=say(if(module==MathModule.Geometry2D)"(3,4)"else"(3,4,0)");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("Distance $a to $b = 5.0 units"));assertEquals(2,count());assertNull(RuhiDialogueMemory.read(s).pending);assertEquals(if(mode=="both_missing")2 else 1,h.forModule(module).protocol.size)
                    if(mode!="both_missing"){say("Undo");assertEquals(initial.points,s.points);assertEquals(initial.points3D,s.points3D)}
                }
                records.put(JSONObject().put("id",row.getInt("id")).put("engine",module.name).put("scenario",mode).put("passed",true))
            }
        }
        assertEquals(150,correct);assertEquals(1200,records.length())
        File(folder,"distance-policy-matrix.json").writeText(JSONObject().put("passed",true).put("source_rows",150).put("correct_intents",correct).put("studios",2).put("workflows",records.length()).put("results",records).put("scope","One/both missing endpoints, restored coordinate replies, invalid input/cancellation and renamed labels; not every possible distance context.").toString(2))
    }
    @Test fun recognize500SeedsAndRecordEveryExecutionWithoutCountingMissingFeaturesAsPasses() {
        TensorFlowGraphNlp.install(context)
        val seeds=JSONArray(context.assets.open("ruhi_semantic_nlp/seed_examples.json").bufferedReader().use{it.readText()});assertEquals(500,seeds.length())
        val records=JSONArray();var intents=0;var distances=0;var midpoints=0;var slopes=0;var angles=0;var circles=0;var triangles=0;var functions=0;var intersections=0;var derivatives=0;var integrals=0;var integralQuestions=0;var surfaces=0;var solidMeasures=0;var transforms=0;var sceneEdits=0;var dialogues=0;var seedSlots=0
        for(i in 0 until seeds.length()) {
            val row=seeds.getJSONObject(i);val text=row.getString("text");val prediction=RuhiLanguageServices.semanticClassifier!!.predict(RuhiSeedNlpInput.normalize(text))
            if(prediction.intent==row.getString("intent"))intents++
            val s=RuhiSeed500Fixture.state(row.getString("engine"),row.getInt("id"));val c=RuhiSeed500Fixture.context(row.getInt("id"),s);val r=RuhiDialogueManager().execute(s,text,RuhiSeed500Fixture.history(row.getInt("id"),s),c)
            val check=RuhiSeedBehaviorOracle.check(row.getString("intent"),RuhiSeedJson.objectValue(row.getJSONObject("parameters").toString()),s,r,c);val verified=check.verified
            if(verified&&row.getString("intent")=="MEASURE_DISTANCE")distances++
            if(verified&&row.getString("intent")=="FIND_MIDPOINT")midpoints++
            if(verified&&row.getString("intent")=="CALCULATE_SLOPE")slopes++
            if(verified&&row.getString("intent")=="MEASURE_ANGLE")angles++
            if(verified&&row.getString("intent")=="CREATE_CIRCLE")circles++
            if(verified&&row.getString("intent")=="CREATE_TRIANGLE")triangles++
            if(verified&&row.getString("intent")=="PLOT_FUNCTION_2D")functions++
            if(verified&&row.getString("intent")=="FIND_INTERSECTION")intersections++
            if(verified&&row.getString("intent")=="DIFFERENTIATE")derivatives++
            if(verified&&row.getString("intent")=="INTEGRATE"){integrals++;if(r.status==GraphCommandStatus.CLARIFICATION)integralQuestions++}
            if(verified&&row.getString("intent")=="PLOT_SURFACE_3D")surfaces++
            if(verified&&row.getString("intent")=="MEASURE_SOLID")solidMeasures++
            if(verified&&row.getString("intent")=="TRANSFORM_3D")transforms++
            if(verified&&row.getString("intent")=="EDIT_SCENE")sceneEdits++
            if(verified&&row.getString("intent")=="DIALOGUE_CONTEXT")dialogues++
            if(row.getString("intent")=="MEASURE_DISTANCE") {
                val slots=RuhiLanguageServices.pointSlotClassifier!!.predict(text);val p=row.getJSONObject("parameters")
                if(slots.singleOrNull{it.role=="POINT_1"}?.value==p.getString("point1")&&slots.singleOrNull{it.role=="POINT_2"}?.value==p.getString("point2"))seedSlots++
            }
            records.put(JSONObject().put("id",row.getInt("id")).put("expected_intent",row.getString("intent")).put("actual_intent",prediction.intent).put("status",r.status.name).put("behavior_verified",verified).put("diagnostics",JSONArray(check.diagnostics)).put("message",r.message).put("text",text).put("engine",row.getString("engine")).put("semantic_intent",r.semanticFrame?.intent).put("semantic_engine",r.semanticFrame?.engine?.name).put("semantic_parameters",JSONObject(r.semanticFrame?.parameters.orEmpty())).put("prerequisite_state",if(row.getString("intent")=="DIALOGUE_CONTEXT")WorkspaceProjectCodec.encode(s)else JSONObject.NULL).put("result_state",if(row.getString("intent")=="DIALOGUE_CONTEXT")WorkspaceProjectCodec.encode(r.state)else JSONObject.NULL))
        }
        File(folder,"seed500-native.json").writeText(JSONObject().put("dialogue_behaviors_verified",dialogues).put("scene_edit_behaviors_verified",sceneEdits).put("transform_behaviors_verified",transforms).put("solid_measure_behaviors_verified",solidMeasures).put("rows",500).put("correct_intents",intents).put("seed_point_slots_verified",seedSlots).put("distance_behaviors_verified",distances).put("midpoint_behaviors_verified",midpoints).put("slope_behaviors_verified",slopes).put("angle_behaviors_verified",angles).put("circle_behaviors_verified",circles).put("triangle_behaviors_verified",triangles).put("function_behaviors_verified",functions).put("intersection_behaviors_verified",intersections).put("derivative_behaviors_verified",derivatives).put("integral_behaviors_verified",integrals).put("integral_bounds_questions_verified",integralQuestions).put("surface_behaviors_verified",surfaces).put("full_behavior_completion",false).put("results",records).toString(2))
        assertEquals("Seed intent recognition",500,intents);assertEquals("Point entity slots",150,seedSlots);assertEquals("Distance behavior",150,distances);assertEquals("Midpoint behavior",25,midpoints);assertEquals("Slope behavior",25,slopes)
        assertEquals("Angle behavior",25,angles)
        assertEquals("Circle behavior",25,circles)
        assertEquals("Triangle behavior",25,triangles)
        assertEquals("Function plotting behavior",25,functions)
        assertEquals("Intersection behavior",25,intersections)
        assertEquals("Derivative behavior",25,derivatives)
        assertEquals("Integration behavior",25,integrals);assertEquals("Required bounds questions",2,integralQuestions)
        assertEquals("Surface plotting behavior",25,surfaces)
        assertEquals("Solid measurement behavior",25,solidMeasures)
        assertEquals("3D transform behavior",25,transforms)
        assertEquals("Scene edit behavior",25,sceneEdits)
        assertEquals("Dialogue source behavior in declared restored contexts",25,dialogues)
    }
    @Test fun geometrySceneEditsRenderLabelsHighlightsViewChangesAndSaveThroughTheProjectLibrary() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-4.0,-1.0),Vec2(2.0,-1.0),Vec2(-1.0,3.0),Vec2(-2.0,-1.0)),shapes=listOf(Shape2D("edge,/907",Shape2DType.Segment,listOf(0,1),"Edge_907"),Shape2D("roof,/811",Shape2DType.Triangle,listOf(0,1,2),"Roof_811"),Shape2D("orb,/662",Shape2DType.Circle,listOf(0,3),"Orbit_662")),labSessionValues=mapOf("ruhi.studio.point.Anchor_907" to "0","ruhi.studio.point.East_811" to "1","ruhi.studio.point.Roof_662" to "2","ruhi.studio.point.Rim_553" to "3"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectShape(1)};compose.waitForIdle()
            val images=mutableListOf<Bitmap>();fun capture(name:String)=captureCanvas(matcher,name).also{images+=it}
            fun ask(t:String,op:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction();try{compose.waitUntil(30_000){run{var done=false;scenario.onActivity{val turn=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull();done=turn?.user==t&&turn.assistant.startsWith("Applied scene $op")};done&&compose.onAllNodesWithText("Applied scene $op",substring=true).fetchSemanticsNodes().isNotEmpty()}}}catch(e:Throwable){File(folder,"scene-edit-failure-semantics.txt").writeText("Command: $t\n"+compose.onRoot(useUnmergedTree=true).printToString());scenario.onActivity{File(folder,"scene-edit-failure-state.txt").writeText(WorkspaceProjectCodec.encode(it.verificationViewModel.state))};throw e};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val before=capture("scene-geometry-before");ask("Color the selected triangle red","color");val red=capture("scene-triangle-red")
            ask("Hide measurements on the triangle","measurements");val measures=capture("scene-hidden-measurements")
            ask("Hide the coordinate grid","grid");val noGrid=capture("scene-grid-hidden");ask("Show the coordinate grid","grid")
            ask("Label the circle as C1","label");val circle=capture("scene-circle-label")
            ask("Remove labels from all points","labels");val labels=capture("scene-labels-hidden");ask("Put a label next to point Anchor_907","point_label");val anchor=capture("scene-point-label")
            ask("Highlight the endpoints of Edge_907","endpoints");val endpoints=capture("scene-endpoints")
            ask("Center the viewport on point East_811","center");val centered=capture("scene-centered-point")
            scenario.onActivity{val v=it.verificationViewModel.state.graph2DView;assertEquals(2.0,v.centerX,1e-9);assertEquals(-1.0,v.centerY,1e-9)}
            ask("Reset zoom to fit all objects","fit");val fit=capture("scene-fit-all")
            ask("Duplicate the selected circle","duplicate");val duplicate=capture("scene-circle-copy")
            ask("Rename the current scene to Trial 907","rename");ask("Save the current construction","save")
            var saved:SavedWorkspace?=null
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(4,vm.state.shapes.size);assertEquals(6,vm.state.points.size);assertEquals(initial.points,vm.state.points.take(4));assertEquals("Trial 907",vm.state.name);saved=vm.savedWorkspaces.first();assertEquals(vm.state.points,saved!!.snapshot.points);assertEquals(vm.state.shapes,saved!!.snapshot.shapes)}
            val dbFolder=File(context.cacheDir,"ruhi-scene-save-${java.util.UUID.randomUUID()}").apply{mkdirs()}
            val isolated=object:android.content.ContextWrapper(context){
                override fun getApplicationContext():android.content.Context=this
                override fun getDatabasePath(name:String)=File(dbFolder,name)
                override fun openOrCreateDatabase(name:String,mode:Int,factory:android.database.sqlite.SQLiteDatabase.CursorFactory?)=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory)
                override fun openOrCreateDatabase(name:String,mode:Int,factory:android.database.sqlite.SQLiteDatabase.CursorFactory?,handler:android.database.DatabaseErrorHandler?)=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).path,factory,handler)
            }
            kotlinx.coroutines.runBlocking{com.indianservers.aiexplorer.persistence.DurableMathStore(isolated).replaceProjects(listOf(saved!!));val reopened=com.indianservers.aiexplorer.persistence.DurableMathStore(isolated).loadProjects().single();assertEquals(saved!!.snapshot.points,reopened.snapshot.points);assertEquals(saved!!.snapshot.shapes,reopened.snapshot.shapes);assertEquals(saved!!.snapshot.labSessionValues,reopened.snapshot.labSessionValues)}
            val restoredShapes=saved!!.snapshot.shapes;ask("Clear the canvas","clear");val empty=capture("scene-cleared");ask("Undo the last change","undo");val restored=capture("scene-clear-undone")
            scenario.onActivity{assertEquals(restoredShapes,it.verificationViewModel.state.shapes)}
            val checks=listOf(changed(before,red),changed(red,measures),changed(measures,noGrid),changed(circle,labels),changed(labels,anchor),changed(anchor,endpoints),changed(endpoints,centered),changed(centered,fit),changed(fit,duplicate),changed(duplicate,empty),changed(empty,restored));assertTrue(checks.toString(),checks.all{it>20})
            File(folder,"scene-geometry-canvas.json").writeText(JSONObject().put("passed",true).put("pixel_checks",JSONArray(checks)).put("native_geometry_preserved",true).put("real_project_library",true).put("sqlite_reopened",true).put("undo_clear",true).put("live_viewport",true).toString(2));images.forEach(Bitmap::recycle)
        }
    }
    @Test fun graphSceneStylesSoloAxesAndMeshOpacityChangeTheNativeCanvas() {
        var graphChecks:List<Int> = emptyList()
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("curve,/907","Bowl_907","y=x^2","#00FFFF"),FunctionDefinition("line,/811","Slope_811","y=x","#FF0000")),surfaceLayers=emptyList(),labSessionValues=mapOf(RuhiSceneEditLanguage.PREFIX+"axes" to "false"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList());it.verificationViewModel.selectGraphAssistantLayer("curve,/907")};compose.waitForIdle()
            val images=mutableListOf<Bitmap>();fun capture(name:String)=captureCanvas(matcher,name).also{images+=it}
            fun ask(t:String,op:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextClearance();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextInput(t);awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){var current=false;scenario.onActivity{current=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull()?.user==t};current&&compose.onAllNodesWithText("Applied scene $op",substring=true).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val before=capture("scene-graph-before");ask("Thicken the red line","width");val thick=capture("scene-graph-thick");ask("Make the last graph blue","color");val blue=capture("scene-graph-blue");ask("Make the plotted curve dashed","dash");val dashed=capture("scene-graph-dashed");ask("Show the x and y axes","axes");val axes=capture("scene-graph-axes");ask("Show a darker graph grid","contrast");val contrast=capture("scene-graph-grid-contrast");ask("Show only the second graph","solo");val solo=capture("scene-graph-solo")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(initial.functions.map{it.id},s.functions.map{it.id});assertEquals(initial.functions.map{it.expression},s.functions.map{it.expression});assertEquals(listOf(false,true),s.functions.map{it.visible});assertEquals(GraphLineStyle.Dashed,s.functions[0].appearance.line);assertEquals("#0000FF",s.functions[1].colorKey)}
            graphChecks=listOf(changed(before,thick),changed(thick,blue),changed(blue,dashed),changed(dashed,axes),changed(axes,contrast),changed(contrast,solo));assertTrue(graphChecks.toString(),graphChecks.all{it>100});images.forEach(Bitmap::recycle)
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D graph canvas");compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val s=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=listOf(SpatialSurfaceLayer("mesh,/907","x^2+y^2")));scenario.onActivity{it.verificationViewModel.importWorkspace(s,false,emptyList())};compose.waitForIdle();val before=captureCanvas(matcher,"scene-mesh-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextInput("Make the mesh 50 percent see-through");compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){compose.onAllNodesWithText("Applied scene opacity",substring=true).fetchSemanticsNodes().isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle();val after=captureCanvas(matcher,"scene-mesh-opacity")
            scenario.onActivity{val layer=it.verificationViewModel.state.surfaceLayers.single();assertEquals(.5,layer.opacity,1e-9);assertEquals(s.surfaceLayers.single().copy(opacity=.5),layer);assertEquals(GraphSurfaceGeometry.mesh(s.surfaceLayers.single(),13),GraphSurfaceGeometry.mesh(layer,13))}
            val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            File(folder,"scene-graph-canvas.json").writeText(JSONObject().put("passed",true).put("graph_pixel_checks",JSONArray(graphChecks)).put("mesh_opacity_pixels",pixels).put("same_formulas_and_ids",true).put("solo_preserves_layers",true).toString(2));before.recycle();after.recycle()
        }
    }
    @Test fun solidTransformsChangeVisibleMeshesStylesCameraAndSharedPivotAnimation() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String)=captureCanvas(matcher,name)
            fun load(s:WorkspaceState){scenario.onActivity{it.verificationViewModel.importWorkspace(s,false,emptyList());it.verificationViewModel.selectSolid(0)};compose.waitForIdle()}
            fun ask(t:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0,position=Vec3(-1.5,0.0,0.0)),Solid(SolidType.Cone,2.0,3.0,radius=1.0,position=Vec3(2.5,0.0,0.0))))
            load(initial);val before=capture("transform-before")
            ask("Rotate the cube 37 degrees around x","Applied 3D rotate");ask("Scale the cube by 1.5","Applied 3D scale");val pose=capture("transform-rotated-scaled")
            scenario.onActivity{val q=it.verificationViewModel.state.solids[0];assertEquals(37.0,q.rotation.x,1e-9);assertEquals(3.0,q.width,1e-9);assertEquals(initial.solids[1],it.verificationViewModel.state.solids[1])}
            ask("Undo that last 3D rotation","Undid the last 3D rotation")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(0.0,vm.state.solids[0].rotation.x,1e-9);assertEquals(3.0,vm.state.solids[0].width,1e-9);val redo=vm.executeRuhiAssistant("Redo");assertEquals(redo.message,GraphCommandStatus.APPLIED,redo.status);assertEquals(37.0,vm.state.solids[0].rotation.x,1e-9);assertEquals(3.0,vm.state.solids[0].width,1e-9)};compose.waitForIdle()
            ask("Give the cube 30 percent opacity","Applied 3D opacity");val transparent=capture("transform-opacity")
            ask("Change the cone to wireframe","Applied 3D wireframe");val wire=capture("transform-wireframe")
            ask("Explode the faces of the cube","Applied 3D explode");val exploded=capture("transform-exploded")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(.3,RuhiTransform3DLanguage.opacity(s,0),1e-9);assertTrue(RuhiTransform3DLanguage.wireframe(s,1));assertEquals(24,SolidMeshFactory.exploded(s.solids[0],RuhiTransform3DLanguage.explosion(s,0)).vertices.size)}
            ask("Put the exploded cube back together","Applied 3D assemble");val assembled=capture("transform-assembled")
            ask("Rotate camera to look down from above","Applied 3D view");val top=capture("transform-camera-top")
            scenario.onActivity{assertEquals(90.0,RuhiTransform3DLanguage.camera(it.verificationViewModel.state).x,1e-9)}
            ask("Show the cube from an isometric angle","Applied 3D view");val iso=capture("transform-camera-isometric")
            scenario.onActivity{val s=it.verificationViewModel.state;val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s),recover=false).state!!;assertEquals(s.solids,saved.solids);assertEquals(RuhiTransform3DLanguage.camera(s),RuhiTransform3DLanguage.camera(saved))}
            val group=initial.copy(labSessionValues=mapOf("ruhi.geometry3d.selection" to "0,1"));load(group)
            compose.mainClock.autoAdvance=false
            try {
                scenario.onActivity{val r=it.verificationViewModel.executeRuhiAssistant("Rotate both solids together around z");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(0,1),RuhiTransform3DLanguage.groupIds(r.state))}
                compose.mainClock.advanceTimeBy(64);val frameA=capture("transform-group-first")
                android.os.SystemClock.sleep(750);compose.mainClock.advanceTimeBy(64);val frameB=capture("transform-group-second")
                val motion=changed(frameA,frameB);assertTrue("Group animation did not alter the committed canvas",motion>100)
                scenario.onActivity{val vm=it.verificationViewModel;val r=vm.executeRuhiAssistant("Stop rotation");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(RuhiTransform3DLanguage.groupIds(r.state).isEmpty());assertEquals(r.state.solids,RuhiMotionEngine.solids(r.state,System.nanoTime()/1_000_000+5000))}
                compose.mainClock.advanceTimeBy(64)
                File(folder,"transform-canvas.json").writeText(JSONObject().put("passed",true).put("visible_scoped_undo",true).put("pose_pixels",changed(before,pose)).put("opacity_pixels",changed(pose,transparent)).put("wireframe_pixels",changed(transparent,wire)).put("explosion_pixels",changed(wire,exploded)).put("assembly_pixels",changed(exploded,assembled)).put("camera_pixels",changed(top,iso)).put("group_motion_pixels",motion).put("stopped_group",true).put("persistence",true).toString(2))
                frameA.recycle();frameB.recycle()
            }finally{compose.mainClock.autoAdvance=true}
            assertTrue(changed(before,pose)>100);assertTrue(changed(pose,transparent)>100);assertTrue(changed(transparent,wire)>100);assertTrue(changed(wire,exploded)>100);assertTrue(changed(exploded,assembled)>100);assertTrue(changed(top,iso)>100)
            listOf(before,pose,transparent,wire,exploded,assembled,top,iso).forEach(Bitmap::recycle)
        }
    }
    @Test fun solidMeasurementsAskForCurrentObjectsRenderDimensionsAndFollowNativeEdits() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun load(s:WorkspaceState){scenario.onActivity{it.verificationViewModel.importWorkspace(s,false,emptyList());it.verificationViewModel.selectSolid(0)};compose.waitForIdle()}
            fun capture(name:String)=captureCanvas(matcher,name)
            fun submit(t:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(t:String,answer:String){submit(t);compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};close()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,1.0,position=Vec3(-3.0,0.0,0.0)),Solid(SolidType.Sphere,4.0,radius=2.0),Solid(SolidType.Sphere,2.0,radius=1.0,position=Vec3(4.0,0.0,0.0))))
            load(initial);val before=capture("solid-measure-before")
            submit("What is volume of this sphere?")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Sphere 2 (4 x 4 x 4)").fetchSemanticsNodes().isNotEmpty()}
            val question=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"solid-measure-question.png").outputStream().use{question.compress(Bitmap.CompressFormat.PNG,100,it)};question.recycle()
            compose.onNodeWithText("Sphere 2 (4 x 4 x 4)").performClick();compose.waitUntil(30_000){compose.onAllNodesWithText("Sphere volume =",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val sphere=capture("solid-measure-sphere")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(3,vm.state.solids.size);assertEquals(32*PI/3,RuhiSolidMeasureLanguage.drawing(vm.state)!!.entities.single().quantities.single().value,1e-8);vm.replaceSolids("Native radius edit"){it.mapIndexed{i,s->if(i==1)s.copy(width=6.0,height=6.0,depth=6.0,radius=3.0)else s}};val d=RuhiSolidMeasureLanguage.drawing(vm.state)!!;assertEquals(36*PI,d.entities.single().quantities.single().value,1e-8);assertTrue(d.entities.single().segments.any{it.text=="r = 3"});val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(d,RuhiSolidMeasureLanguage.drawing(saved))};compose.waitForIdle();val parent=capture("solid-measure-after-radius-edit")
            load(WorkspaceState(module=MathModule.Geometry3D,solids=emptyList()));ask("Find total area of a cone with radius 3 and height 4","Cone surface area =");compose.onNodeWithText("Empty 3D canvas").assertDoesNotExist();val cone=capture("solid-measure-cone")
            scenario.onActivity{val vm=it.verificationViewModel;assertTrue(vm.state.solids.isEmpty());assertEquals(24*PI,RuhiSolidMeasureLanguage.drawing(vm.state)!!.entities.single().quantities.single().value,1e-8)}
            load(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0))));ask("How many faces, edges and vertices does the cube have?","F=6; E=12; V=8");val topology=capture("solid-measure-topology");ask("Verify Euler formula on the selected convex polyhedron","8 - 12 + 6 = 2");val euler=capture("solid-measure-euler")
            val pixels=changed(before,sphere);val edit=changed(sphere,parent);assertTrue(pixels>100);assertTrue(edit>100)
            File(folder,"solid-measure-canvas.json").writeText(JSONObject().put("passed",true).put("measurement_pixels",pixels).put("parent_edit_pixels",edit).put("actual_solid_chip",true).put("live_radius",true).put("literal_reference_without_creation",true).put("native_cube_topology",true).put("persistence",true).toString(2))
            listOf(before,sphere,parent,cone,topology,euler).forEach(Bitmap::recycle)
        }
    }
    @Test fun explicitSurfacesRenderAsNativeWireframeHemisphereAndUnbridgedPoleLayers() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D graph canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String)=captureCanvas(matcher,name)
            fun empty(){scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList(),surfaceExpression="0"),false,emptyList())};compose.waitForIdle()}
            fun ask(t:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){compose.onAllNodesWithText("native 3D surface",substring=true).fetchSemanticsNodes().isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            empty();val before=capture("surface-before");ask("Plot z = x squared + y squared");val paraboloid=capture("surface-paraboloid")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(1,s.surfaceLayers.size);GraphSurfaceGeometry.mesh(s.surfaceLayers.single(),24).vertices.forEach{v->assertEquals(v.x*v.x+v.y*v.y,v.z,1e-7)}}
            empty();ask("Plot z = sin(x)sin(y) as wireframe");val wire=capture("surface-wireframe")
            scenario.onActivity{assertEquals(SpatialSurfaceRenderMode.Wireframe,it.verificationViewModel.state.surfaceLayers.single().renderMode)}
            empty();ask("Render a hemisphere z = sqrt(9-x squared-y squared)");val hemisphere=capture("surface-hemisphere")
            scenario.onActivity{val m=GraphSurfaceGeometry.mesh(it.verificationViewModel.state.surfaceLayers.single(),24);assertTrue(m.vertices.isNotEmpty());assertTrue(m.vertices.all{v->v.z>=0&&v.x*v.x+v.y*v.y<=9+1e-8&&abs(v.x*v.x+v.y*v.y+v.z*v.z-9)<1e-6});assertTrue(m.vertices.filter{it.z<1e-5&&abs(it.x*it.x+it.y*it.y-9)<1e-7}.distinctBy{round(it.x*1e6).toLong() to round(it.y*1e6).toLong()}.size>=12)}
            empty();ask("Show the surface z = x/y without connecting the singularity");val pole=capture("surface-pole")
            ask("Show two surfaces z = x squared+y squared and z = 4 together");val pair=capture("surface-paired")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(3,vm.state.surfaceLayers.size);val density=GraphSurfaceGeometry.sceneDensity(24,3);assertEquals(13,density);val m=GraphSurfaceGeometry.mesh(vm.state.surfaceLayers.first(),density);RuhiSurfaceOracle.triangles(m).forEach{tri->assertFalse(tri.minOf{it.y}<0&&tri.maxOf{it.y}>0)};val old=vm.state.surfaceLayers;assertTrue(vm.replaceSurfaceLayers(old.mapIndexed{i,l->if(i==0)l.copy(expression="x/(y-0.37)",domain=SurfaceDomain3D(-3.0,3.0,-2.17,2.83))else l},"Native surface parent edit").isSuccess);assertEquals(3,vm.state.surfaceLayers.size);val updated=GraphSurfaceGeometry.mesh(vm.state.surfaceLayers.first(),13);RuhiSurfaceOracle.triangles(updated).forEach{tri->assertFalse(tri.minOf{it.y}<.37&&tri.maxOf{it.y}>.37)};assertEquals(-2.17,updated.vertices.minOf{it.y},1e-8);assertEquals(2.83,updated.vertices.maxOf{it.y},1e-8);val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(vm.state.surfaceLayers,saved.surfaceLayers)};compose.waitForIdle();val parent=capture("surface-after-parent-edit")
            val pixels=changed(before,paraboloid);val edit=changed(pair,parent);assertTrue(pixels>100);assertTrue(edit>100)
            File(folder,"surface-canvas.json").writeText(JSONObject().put("passed",true).put("native_surface_pixels",pixels).put("parent_edit_pixels",edit).put("wireframe",true).put("hemisphere_domain",true).put("odd_density_pole_gap",true).put("independent_xy_domains",true).put("two_surface_transaction",true).put("persistence",true).toString(2))
            listOf(before,paraboloid,wire,hemisphere,pole,pair,parent).forEach(Bitmap::recycle)
        }
    }
    @Test fun integralBoundsChoicesShadedAreasAndApproximationPanelsRenderAndFollowLiveFunctions() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String)=captureCanvas(matcher,name)
            fun type(t:String){compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(t:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();type(t);compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};close()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()
            ask("Plot Energy_907(x) = x squared","Plotted");val before=capture("integral-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();type("Integrate the selected function");compose.waitUntil(30_000){compose.onAllNodesWithText("Visible x-range").fetchSemanticsNodes().isNotEmpty()}
            val question=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"integral-bounds-question.png").outputStream().use{question.compress(Bitmap.CompressFormat.PNG,100,it)};question.recycle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("integral_bounds",RuhiDialogueMemory.read(vm.state).pending!!.kind);assertNull(RuhiIntegralLanguage.drawing(vm.state));assertEquals(1,vm.state.functions.size)}
            compose.onNodeWithText("0 to 1").performClick();compose.waitUntil(30_000){compose.onAllNodesWithText("Signed integral =",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val shade=capture("integral-preset-shading")
            scenario.onActivity{val d=RuhiIntegralLanguage.drawing(it.verificationViewModel.state)!!;assertEquals(1.0/3,d.values!!.signed,1e-7);assertNull(RuhiDialogueMemory.read(it.verificationViewModel.state).pending)}
            ask("Shade area under the currently selected curve between 1 and 4","Signed integral =");val selected=capture("integral-selected-area")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(21.0,RuhiIntegralLanguage.drawing(vm.state)!!.values!!.signed,1e-7);vm.updateFunction(0){it.copy(expression="y=3*x^2")};assertEquals(63.0,RuhiIntegralLanguage.drawing(vm.state)!!.values!!.signed,1e-7);assertEquals(1,vm.state.functions.size)};compose.waitForIdle();val parent=capture("integral-after-parent-edit")
            ask("Shade the region between x squared and 4","Bounds derived from visible intersections");val between=capture("integral-between-curves")
            scenario.onActivity{val d=RuhiIntegralLanguage.drawing(it.verificationViewModel.state)!!;assertEquals(-2.0,d.bounds!!.first,1e-7);assertEquals(2.0,d.bounds!!.second,1e-7);assertEquals(32.0/3,d.values!!.absolute,1e-7)}
            ask("Highlight positive and negative area separately on sin(x) over 0 to 2*pi","positive area =");val signs=capture("integral-split-signs")
            fun coloredNear(p:Vec2,positive:Boolean):Int{val x=(signs.width/2.0+p.x*signs.width/14).roundToInt();val y=(signs.height/2.0-p.y*signs.width/14).roundToInt();var count=0;for(px in x-3..x+3)for(py in y-3..y+3){val color=signs.getPixel(px,py);val r=android.graphics.Color.red(color);val g=android.graphics.Color.green(color);val b=android.graphics.Color.blue(color);if(if(positive)g>r+10&&b>r+15 else r>g+20&&b>g+20)count++};return count}
            assertTrue("Positive cyan area missing",coloredNear(Vec2(PI/2,.5),true)>10);assertTrue("Negative magenta area missing",coloredNear(Vec2(3*PI/2,-.5),false)>10)
            scenario.onActivity{val v=RuhiIntegralLanguage.drawing(it.verificationViewModel.state)!!.values!!;assertEquals(0.0,v.signed,1e-7);assertEquals(4.0,v.absolute,1e-7);assertEquals(2.0,v.positive,1e-7);assertEquals(-2.0,v.negative,1e-7)}
            ask("Use a Riemann sum for x squared on [0,2]","12 panels:");val riemann=capture("integral-riemann")
            scenario.onActivity{val d=RuhiIntegralLanguage.drawing(it.verificationViewModel.state)!!;assertEquals(12,d.estimate!!.count);assertEquals(8.0/3-2.0/(3*144),d.estimate!!.midpoint,1e-7);assertEquals(12,d.bands.count{it.approximation})}
            ask("Visualize trapezoid-rule approximation of sin(x) over [0,pi]","12 panels:");val trapezoid=capture("integral-trapezoid")
            ask("Show integral accumulation from 0 to x for sin(t)","Accumulation from");val accumulation=capture("integral-accumulation")
            scenario.onActivity{val vm=it.verificationViewModel;val d=RuhiIntegralLanguage.drawing(vm.state)!!;val curve=d.helpers.single{it.name=="Accumulation"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(2.0,RuhiIntersectionMath.y(curve,PI)!!,1e-7);val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(d,RuhiIntegralLanguage.drawing(loaded));assertEquals(1,vm.state.functions.size);assertTrue(vm.state.points.isEmpty())}
            val first=changed(before,shade);val edit=changed(selected,parent);assertTrue(first>100);assertTrue(edit>100)
            File(folder,"integral-canvas.json").writeText(JSONObject().put("passed",true).put("bounds_chip_resumed",true).put("shading_pixels",first).put("parent_edit_pixels",edit).put("live_function",true).put("intersection_bounds",true).put("split_sign_pixels",true).put("riemann_trapezoid_panels",true).put("accumulation",true).put("persistence",true).toString(2))
            listOf(before,shade,selected,parent,between,signs,riemann,trapezoid,accumulation).forEach(Bitmap::recycle)
        }
    }
    @Test fun nativeDerivativeDomainChoicesRejectUndefinedXAndResumeCurrentCurve() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("domain,/907","Square907","y=x^2","cyan"),FunctionDefinition("domain,/811","Log811","y=ln(x)","red")),surfaceLayers=emptyList())
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(initial,false,emptyList());vm.selectCoordinatePoint(-1);vm.selectGraphAssistantLayer(null)};compose.waitForIdle()
            askConstruction(scenario,"What is the instantaneous gradient here?","At which x coordinate")
            askConstruction(scenario,"0","Which curve should I differentiate?")
            askConstruction(scenario,"Log811","The function is undefined at x=0.0.")
            scenario.onActivity{val vm=it.verificationViewModel;val saved=RuhiDialogueMemory.read(vm.state);assertEquals("derivative_x",saved.pending!!.kind);assertEquals("domain,/811",saved.pending!!.slots["curve_id"]);assertNull(saved.pending!!.slots["x"]);assertTrue(saved.lastChoices.size in 1..3);assertTrue(saved.lastChoices.all{it.answer.substringAfterLast(' ').toDouble()>0});assertEquals(initial.functions,vm.state.functions);assertTrue(vm.state.points.isEmpty())}
            captureDistanceReply("The function is undefined at x=0.0.","derivative-domain-log-question")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(-1)};compose.waitForIdle()
            askConstruction(scenario,"2","slope = 0.5")
            scenario.onActivity{val s=it.verificationViewModel.state;val q=RuhiDerivativeLanguage.decode(s.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals("domain,/811",q.source);assertEquals(listOf(2.0),q.xs);assertEquals(Vec2(2.0,ln(2.0)),RuhiDerivativeLanguage.drawing(s)!!.points.single());assertEquals(initial.functions,s.functions);assertTrue(s.points.isEmpty());assertNull(RuhiDialogueMemory.read(s).pending)}
            captureDistanceReply("slope = 0.5","derivative-domain-log-answer")
            val corner=WorkspaceState(module=MathModule.Graph2D,points=listOf(Vec2(0.0,0.0)),functions=listOf(FunctionDefinition("corner,/441","Corner441","y=abs(x)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(corner,false,emptyList());vm.selectCoordinatePoint(0)};compose.waitForIdle()
            askConstruction(scenario,"Show the tangent slope at the selected point","At which x coordinate should I evaluate the curve instead?")
            scenario.onActivity{val saved=RuhiDialogueMemory.read(it.verificationViewModel.state);assertEquals("derivative_x",saved.pending!!.kind);assertTrue(saved.lastChoices.isNotEmpty());assertTrue(saved.lastChoices.none{it.answer.substringAfterLast(' ').toDouble()==0.0});assertEquals(corner.points,it.verificationViewModel.state.points)}
            askConstruction(scenario,"-1","slope = -1.0")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(corner.points,s.points);assertEquals(corner.functions,s.functions);assertEquals(Vec2(-1.0,1.0),RuhiDerivativeLanguage.drawing(s)!!.points.single())}
            captureCanvas(matcher,"derivative-domain-corner-answer").recycle()
            File(folder,"derivative-domain-canvas.json").writeText(JSONObject().put("passed",true).put("undefined_x_reasked",true).put("positive_log_examples",true).put("restored_curve","domain,/811").put("log_x",2).put("log_slope",.5).put("corner_zero_excluded",true).put("corner_x",-1).put("corner_slope",-1).put("source_geometry_preserved",true).toString(2))
        }
    }
    @Test fun nativeDerivativeClarificationsRetainXCurveAndPointAcrossRestores() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("curve,/907","Square907","y=x^2","cyan"),FunctionDefinition("curve,/811","Cube811","y=x^3","red")),surfaceLayers=emptyList())
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(initial,false,emptyList());vm.selectCoordinatePoint(-1);vm.selectGraphAssistantLayer(null)};compose.waitForIdle()
            askConstruction(scenario,"Show the tangent slope at the selected point","At which x coordinate")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(-1)};compose.waitForIdle()
            askConstruction(scenario,"2","Which curve should I differentiate?")
            scenario.onActivity{val vm=it.verificationViewModel;val p=RuhiDialogueMemory.read(vm.state).pending!!;assertEquals("derivative_graph",p.kind);assertEquals("2.0",p.slots["x"]);assertEquals(initial.functions,vm.state.functions);assertTrue(vm.state.points.isEmpty());vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(-1)};compose.waitForIdle()
            captureDistanceReply("Which curve should I differentiate?","derivative-slots-curve-question")
            askConstruction(scenario,"Cube811","slope = 12.0")
            scenario.onActivity{val s=it.verificationViewModel.state;val q=RuhiDerivativeLanguage.decode(s.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals("curve,/811",q.source);assertEquals(listOf(2.0),q.xs);assertEquals(Vec2(2.0,8.0),RuhiDerivativeLanguage.drawing(s)!!.points.single());assertEquals(initial.functions,s.functions);assertTrue(s.points.isEmpty());assertNull(RuhiDialogueMemory.read(s).pending)}
            captureDistanceReply("slope = 12.0","derivative-slots-numeric-answer")
            val pointScene=initial.copy(points=listOf(Vec2(2.0,4.0)))
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(pointScene,false,emptyList());vm.selectCoordinatePoint(0);vm.selectGraphAssistantLayer(null)};compose.waitForIdle()
            askConstruction(scenario,"What is the instantaneous gradient here?","Which curve should I differentiate?")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("0",RuhiDialogueMemory.read(vm.state).pending!!.slots["point_index"]);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(-1)};compose.waitForIdle()
            askConstruction(scenario,"Square907","slope = 4.0")
            scenario.onActivity{val s=it.verificationViewModel.state;val q=RuhiDerivativeLanguage.decode(s.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals("curve,/907",q.source);assertTrue(q.pointKey.isNotEmpty());assertEquals("0",s.labSessionValues[q.pointKey]);assertEquals(pointScene.points,s.points);assertEquals(initial.functions,s.functions);assertNull(RuhiDialogueMemory.read(s).pending)}
            captureCanvas(matcher,"derivative-slots-point-answer").recycle()
            File(folder,"derivative-slots-canvas.json").writeText(JSONObject().put("passed",true).put("restored_x",2).put("curve_id","curve,/811").put("computed_slope",12).put("natural_numeric_reply",true).put("natural_curve_name_reply",true).put("restored_point_index",0).put("point_slope",4).put("point_binding_preserved",true).put("source_geometry_preserved",true).toString(2))
        }
    }
    @Test fun nativeSelectedDerivativePointFollowsLiveCoordinatesAndRejectsOffCurveSelection() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,points=listOf(Vec2(-2.0,4.0),Vec2(1.0,1.0)),functions=listOf(FunctionDefinition("native,/motion907","Motion907","y=x^2","cyan")),surfaceLayers=emptyList(),labSessionValues=mapOf(GraphAssistant.selectionKey(MathModule.Graph2D) to "native,/motion907"))
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(initial,false,emptyList());vm.selectCoordinatePoint(1)};compose.waitForIdle()
            askConstruction(scenario,"Show the tangent slope at the selected point","slope = 2.0")
            scenario.onActivity{val vm=it.verificationViewModel;val q=RuhiDerivativeLanguage.decode(vm.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertTrue(q.pointKey.isNotEmpty());assertEquals("1",vm.state.labSessionValues[q.pointKey]);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(1)};compose.waitForIdle()
            val before=captureCanvas(matcher,"derivative-point-before")
            scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(3.0,9.0))};compose.waitForIdle()
            scenario.onActivity{val s=it.verificationViewModel.state;val d=RuhiDerivativeLanguage.drawing(s)!!;assertTrue(d.diagnostic.isEmpty());assertEquals(Vec2(3.0,9.0),d.points.single());val curve=d.helpers.single().curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(6.0,RuhiIntersectionMath.y(curve,3.0)!!,1e-8);assertEquals(initial.functions,s.functions);assertEquals(initial.points[0],s.points[0])}
            val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(rendered,rendered.contains("(3.0, 9.0)"));assertFalse(rendered.contains("(1.0, 1.0)"))
            val moved=captureCanvas(matcher,"derivative-point-moved")
            askConstruction(scenario,"What is the instantaneous gradient here?","slope = 6.0")
            scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(3.0,8.0))};compose.waitForIdle()
            scenario.onActivity{val d=RuhiDerivativeLanguage.drawing(it.verificationViewModel.state)!!;assertTrue(d.diagnostic.contains("not on"));assertTrue(d.points.isEmpty());assertTrue(d.helpers.isEmpty())}
            val suppressed=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(suppressed,suppressed.startsWith("Derivative unavailable:"));assertFalse(suppressed.contains("(3.0, 9.0)"))
            askConstruction(scenario,"Show the tangent slope at the selected point","The selected point is not on the current curve.")
            captureDistanceReply("The selected point is not on the current curve.","derivative-point-off-curve-question")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("derivative_x",RuhiDialogueMemory.read(vm.state).pending!!.kind);vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectCoordinatePoint(1)};compose.waitForIdle()
            askConstruction(scenario,"Use derivative x 2","slope = 4.0")
            scenario.onActivity{val s=it.verificationViewModel.state;val d=RuhiDerivativeLanguage.drawing(s)!!;assertEquals(Vec2(2.0,4.0),d.points.single());assertEquals(Vec2(3.0,8.0),s.points[1]);assertEquals(initial.functions,s.functions);assertTrue(RuhiDerivativeLanguage.decode(s.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).pointKey.isEmpty())}
            val explicit=captureCanvas(matcher,"derivative-point-explicit-x")
            val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);moved.getPixels(b,0,moved.width,0,0,moved.width,moved.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            File(folder,"derivative-point-canvas.json").writeText(JSONObject().put("passed",true).put("restored_binding",true).put("live_point",JSONArray(listOf(3,9))).put("live_slope",6).put("off_curve_rejected",true).put("stale_overlay_suppressed",true).put("rendered_live_state",rendered).put("rendered_off_curve_state",suppressed).put("restored_clarification",true).put("explicit_x",2).put("source_geometry_preserved",true).put("changed_pixels",pixels).toString(2));listOf(before,moved,explicit).forEach(Bitmap::recycle)
        }
    }
    @Test fun derivativesTangentsAndNormalsRenderAndFollowTheLiveFunction() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun ask(text:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()
            ask("Plot Motion_907(x) = x squared","Plotted");val before=capture("derivative-before")
            ask("Plot the derivative of the selected curve","Derivative:");val derivative=capture("derivative-curve")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(1,vm.state.functions.size);val d=RuhiDerivativeLanguage.drawing(vm.state)!!;val curve=d.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(4.0,RuhiIntersectionMath.y(curve,2.0)!!,1e-7);vm.updateFunction(0){it.copy(expression="y=3*x^2")};val edited=RuhiDerivativeLanguage.drawing(vm.state)!!.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(12.0,RuhiIntersectionMath.y(edited,2.0)!!,1e-7);val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(RuhiDerivativeLanguage.drawing(vm.state),RuhiDerivativeLanguage.drawing(loaded))};compose.waitForIdle();val edit=capture("derivative-after-parent-edit")
            ask("Show how the tangent changes as x moves","Move the existing Trace x slider")
            for(x in listOf(1f,-1f)){
                compose.onNodeWithContentDescription("Graph trace x").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(x)};compose.waitForIdle()
                scenario.onActivity{val vm=it.verificationViewModel;val actual=vm.state.labSessionValues["Graph2D::ruhi.derivative.trace_x"]!!.toDouble();assertEquals(x.toDouble(),actual,1e-6);val d=RuhiDerivativeLanguage.drawing(vm.state)!!;assertTrue(d.points.single().distanceTo(Vec2(actual,3*actual*actual))<1e-6);val line=d.helpers.single{it.name=="slider"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(3*actual*actual+6*actual*.5,RuhiIntersectionMath.y(line,actual+.5)!!,1e-6);assertEquals(1,vm.state.functions.size)}
            }
            val slider=capture("derivative-slider");scenario.onActivity{it.verificationViewModel.hidePanels()};compose.waitForIdle()
            ask("Draw a tangent to y = x squared at x = 2","slope = 4.0");val tangent=capture("derivative-tangent")
            ask("Draw the normal to y = x squared at x = 1","drawn normal");val normal=capture("derivative-normal")
            scenario.onActivity{val vm=it.verificationViewModel;val d=RuhiDerivativeLanguage.drawing(vm.state)!!;assertTrue(d.points.single().distanceTo(Vec2(1.0,1.0))<1e-8);val line=d.helpers.single{it.name=="normal"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(.5,RuhiIntersectionMath.y(line,2.0)!!,1e-7);assertEquals(1,vm.state.functions.size);assertTrue(vm.state.points.isEmpty())}
            ask("Explain why absolute value is not differentiable at zero","left-hand slope is -1");val corner=capture("derivative-absolute-corner")
            val first=changed(before,derivative);val live=changed(derivative,edit);assertTrue(first>100);assertTrue(live>100)
            File(folder,"derivative-canvas.json").writeText(JSONObject().put("passed",true).put("source_curve_count",1).put("derivative_pixels",first).put("parent_edit_pixels",live).put("live_function",true).put("tangent_slider",true).put("tangent_normal",true).put("absolute_corner_explained",true).put("persistence",true).toString(2))
            listOf(before,derivative,edit,slider,tangent,normal,corner).forEach(Bitmap::recycle)
        }
    }
    @Test fun intersectionsUseVisibleSelectionAndFollowNativeCurveAndGeometryEditsAndPanning() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun type(text:String){awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextClearance();awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performTextInput(text);awaitRuhiInput();compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(text:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();type(text);compose.waitUntil(30_000){var current=false;scenario.onActivity{current=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.lastOrNull()?.user==text};current&&compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()};close()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()
            ask("Plot y = x squared","Plotted");ask("Plot y = x","Plotted");ask("Clear curve pair selection","Curve pair selection cleared")
            val before=capture("intersection-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();type("Do the selected curves intersect at all?")
            compose.waitUntil(30_000){compose.onAllNodesWithText("f(x) + g(x)").fetchSemanticsNodes().isNotEmpty()}
            val question=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"intersection-pair-question.png").outputStream().use{question.compress(Bitmap.CompressFormat.PNG,100,it)};question.recycle()
            compose.onNodeWithText("f(x) + g(x)").performClick();compose.waitUntil(30_000){compose.onAllNodesWithText("Yes. 2 intersections",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val crossing=capture("intersection-crossings")
            ask("Ruhi, could you count the intersections of these plotted curves?","Intersection count: 2.")
            scenario.onActivity{val vm=it.verificationViewModel;val d=RuhiIntersectionLanguage.drawing(vm.state)!!;assertEquals(2,d.points.size);assertTrue(d.points.any{it.distanceTo(Vec2(1.0,1.0))<1e-6});assertNull(RuhiDialogueMemory.read(vm.state).pending);assertNotNull(vm.state.labSessionValues[RuhiIntersectionLanguage.ASPECT]);vm.updateFunction(1){it.copy(expression="y=2*x")};val updated=RuhiIntersectionLanguage.drawing(vm.state)!!;assertEquals(2,updated.points.size);assertTrue(updated.points.any{it.distanceTo(Vec2(2.0,4.0))<1e-6});val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(updated.points,RuhiIntersectionLanguage.drawing(loaded)!!.points)};compose.waitForIdle();val edited=capture("intersection-after-curve-edit")
            val geometry=WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList(),points=listOf(Vec2(-1.0,0.0),Vec2(1.0,0.0),Vec2(1.0,0.0),Vec2(3.0,0.0)),shapes=listOf(Shape2D("orb-a",Shape2DType.Circle,listOf(0,2),"Orb_907"),Shape2D("orb-b",Shape2DType.Circle,listOf(1,3),"Orb_811")),labSessionValues=mapOf("ruhi.studio.point.Anchor_907" to "0","ruhi.studio.point.East_811" to "1"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(geometry,false,emptyList())};compose.waitForIdle()
            ask("Find intersection of circles centered at Anchor_907 and East_811","Intersections:");val circles=capture("intersection-circles")
            scenario.onActivity{val vm=it.verificationViewModel;val points=RuhiIntersectionLanguage.drawing(vm.state)!!.points;assertEquals(2,points.size);assertTrue(points.all{abs(it.x)<1e-8&&abs(abs(it.y)-sqrt(3.0))<1e-8});vm.movePoint(2,Vec2(2.0,0.0));vm.movePoint(3,Vec2(4.0,0.0));val moved=RuhiIntersectionLanguage.drawing(vm.state)!!.points;assertEquals(2,moved.size);assertTrue(moved.all{abs(it.x)<1e-8&&abs(abs(it.y)-sqrt(8.0))<1e-8});assertEquals(4,vm.state.points.size);assertEquals(geometry.shapes,vm.state.shapes)};compose.waitForIdle();val circleMoved=capture("intersection-circles-after-parent-edit")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(2,Vec2(0.0,0.0));vm.movePoint(3,Vec2(2.0,0.0));assertEquals(listOf(Vec2(0.0,0.0)),RuhiIntersectionLanguage.drawing(vm.state)!!.points)};compose.waitForIdle();val tangent=capture("intersection-tangent")
            var controls:List<Vec2> = emptyList();scenario.onActivity{controls=it.verificationViewModel.state.points}
            compose.onNode(matcher,useUnmergedTree=true).performTouchInput{val x=width*.15f;val y=height*.5f;down(0,androidx.compose.ui.geometry.Offset(x,y-60));down(1,androidx.compose.ui.geometry.Offset(x,y+60));moveTo(0,androidx.compose.ui.geometry.Offset(width*.9f,y-60));moveTo(1,androidx.compose.ui.geometry.Offset(width*.9f,y+60));advanceEventTime(300);up(0);up(1)};compose.waitForIdle();val panned=capture("intersection-after-pan")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(controls,vm.state.points);assertTrue(RuhiIntersectionLanguage.drawing(vm.state)!!.points.isEmpty());val aspect=vm.state.labSessionValues[RuhiIntersectionLanguage.ASPECT]!!.toDouble();assertEquals(panned.height.toDouble()/panned.width,aspect,.01)}
            val markerPixels=changed(before,crossing);val editPixels=changed(crossing,edited);val circlePixels=changed(circles,circleMoved);val panPixels=changed(tangent,panned);assertTrue(markerPixels>100);assertTrue(editPixels>100);assertTrue(circlePixels>100);assertTrue(panPixels>100)
            val implicitScene=WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList(),functions=listOf(FunctionDefinition("implicit,/907","Orbit_907","x^2+y^2=4","#00FFFF"),FunctionDefinition("line,/811","Diagonal_811","y=x","#FF0000")))
            scenario.onActivity{it.verificationViewModel.importWorkspace(implicitScene,false,emptyList())};compose.waitForIdle()
            fun assertVisibleMarkers(b:Bitmap,expected:List<Vec2>){
                scenario.onActivity{val view=it.verificationViewModel.state.graph2DView;assertEquals(0.0,view.centerX,1e-6);assertEquals(0.0,view.centerY,1e-6);assertEquals(1.0,view.zoom.toDouble(),1e-6)}
                // Project from this captured bitmap and the requested view, independently of
                // mutable viewport metadata (the IME can resize it after the capture).
                expected.forEach{p->val x=(b.width/2.0+p.x*b.width/14).roundToInt();val y=(b.height/2.0-p.y*b.width/14).roundToInt();var blue=0
                    for(px in max(0,x-12)..min(b.width-1,x+12))for(py in max(0,y-12)..min(b.height-1,y+12)){val color=b.getPixel(px,py);if(android.graphics.Color.blue(color)>150&&android.graphics.Color.red(color)<70&&android.graphics.Color.green(color) in 60..190)blue++}
                    assertTrue("Visible blue marker missing near ($x,$y) for $p",blue>10)
                }
            }
            ask("Where do Orbit_907 and Diagonal_811 intersect?","Intersections:");val implicit=capture("intersection-implicit")
            assertVisibleMarkers(implicit,listOf(Vec2(sqrt(2.0),sqrt(2.0)),Vec2(-sqrt(2.0),-sqrt(2.0))))
            scenario.onActivity{val vm=it.verificationViewModel;val p=RuhiIntersectionLanguage.drawing(vm.state)!!.points;assertEquals(2,p.size);assertTrue(p.all{abs(abs(it.x)-sqrt(2.0))<1e-6&&abs(it.x-it.y)<1e-8});vm.updateFunction(0){it.copy(expression="x^2+y^2=9")};val moved=RuhiIntersectionLanguage.drawing(vm.state)!!.points;assertEquals(2,moved.size);assertTrue(moved.all{abs(abs(it.x)-3/sqrt(2.0))<1e-6&&abs(it.x-it.y)<1e-8});assertEquals(2,vm.state.functions.size);assertTrue(vm.state.points.isEmpty())};compose.waitForIdle();val implicitEdited=capture("intersection-implicit-after-edit")
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(1){it.copy(expression="x=1")};val p=RuhiIntersectionLanguage.drawing(vm.state)!!.points;assertEquals(2,p.size);assertTrue(p.all{abs(it.x-1)<1e-8&&abs(abs(it.y)-sqrt(8.0))<1e-6});val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(p,RuhiIntersectionLanguage.drawing(loaded)!!.points)};compose.waitForIdle();val implicitVertical=capture("intersection-implicit-vertical")
            assertVisibleMarkers(implicitEdited,listOf(Vec2(3/sqrt(2.0),3/sqrt(2.0)),Vec2(-3/sqrt(2.0),-3/sqrt(2.0))))
            assertVisibleMarkers(implicitVertical,listOf(Vec2(1.0,sqrt(8.0)),Vec2(1.0,-sqrt(8.0))))
            val implicitPixels=changed(implicit,implicitEdited);assertTrue(implicitPixels>100)
            File(folder,"intersection-canvas.json").writeText(JSONObject().put("marker_pixels",markerPixels).put("curve_edit_pixels",editPixels).put("circle_edit_pixels",circlePixels).put("pan_pixels",panPixels).put("pair_question_resumed",true).put("polite_instruction_alias",true).put("implicit_explicit_vertical_live",true).put("implicit_edit_pixels",implicitPixels).put("live_curve_and_circle_dependencies",true).put("tangency_deduplicated",true).put("actual_canvas_aspect",true).put("viewport_hides_outside_markers",true).put("persistence",true).put("passed",true).toString(2))
            listOf(implicit,implicitEdited,implicitVertical).forEach(Bitmap::recycle)
            listOf(before,crossing,edited,circles,circleMoved,tangent,panned).forEach(Bitmap::recycle)
        }
    }
    @Test fun typedFunctionsRenderWithRestrictionsAndInteractiveAxes() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun reset(){scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList()),false,emptyList())};compose.waitForIdle()}
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun type(text:String){compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(text:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();type(text);try{compose.waitUntil(30_000){var ready=false;scenario.onActivity{val s=it.verificationViewModel.state;ready=s.functions.size==1&&RuhiDialogueMemory.read(s).turns.lastOrNull()?.let{it.user==text&&it.assistant.startsWith("Plotted")}==true};ready&&compose.onAllNodesWithText("Plotted",substring=true).fetchSemanticsNodes().isNotEmpty()}}catch(e:Throwable){File(folder,"function-failure-semantics.txt").writeText("Command: $text\n"+compose.onRoot(useUnmergedTree=true).printToString());throw e};close()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            reset();val before=capture("function-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();type("Draw a function")
            compose.waitUntil(30_000){compose.onAllNodesWithText("What would you like to graph?",substring=true).fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("y = x^2").performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText("Plotted",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val parabola=capture("function-parabola")
            scenario.onActivity{val vm=it.verificationViewModel;assertNull(RuhiDialogueMemory.read(vm.state).pending);assertEquals(1,vm.state.functions.size)}
            reset();ask("Draw y = x squared only for x between -2 and 2");val restricted=capture("function-restricted")
            scenario.onActivity{val vm=it.verificationViewModel;val d=TypedGraphExpressionParser.parse(vm.state.functions.single().expression);assertTrue(d is TypedGraphExpression.Explicit);val e=TypedGraphEngine();assertNull(e.evaluate(d,2.1));assertNull(e.evaluate(d,-2.1));assertEquals(4.0,e.evaluate(d,2.0)!!.y,1e-9);assertTrue(e.sample(d).curves.flatMap{it.points}.all{it.x in -2.0..2.0});val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(vm.state.functions,saved.functions)}
            reset();ask("Graph the line x = 4");val vertical=capture("function-vertical")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);val sample=TypedGraphEngine().sample(d,samples=520);assertTrue(sample.implicitSegments.isNotEmpty());assertTrue(sample.implicitSegments.all{seg->abs(seg.start.x-4)<1e-8&&abs(seg.end.x-4)<1e-8})}
            reset();ask("Plot the polar curve r = 2cos(3theta)");val polar=capture("function-polar")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);assertTrue(d is TypedGraphExpression.Polar);assertEquals(2.0,TypedGraphEngine().evaluate(d,0.0)!!.x,1e-9)}
            reset();ask("Draw the piecewise function y = x if x>=0, otherwise -x");val piecewise=capture("function-piecewise")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);assertTrue(d is TypedGraphExpression.Piecewise);assertEquals(3.0,TypedGraphEngine().evaluate(d,-3.0)!!.y,1e-9)}
            reset();ask("Show y = floor(x)");val floor=capture("function-floor")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);val curves=TypedGraphEngine().sample(d).curves;assertTrue(curves.size>2);assertTrue(curves.all{seg->seg.points.zipWithNext().all{pair->pair.first.y==pair.second.y}})}
            reset();ask("Draw the upper half of x squared + y squared = 9");val upper=capture("function-upper-circle")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);assertTrue(TypedGraphEngine().sample(d).curves.flatMap{it.points}.all{p->p.y>=0&&abs(p.x*p.x+p.y*p.y-9)<1e-8})}
            reset();ask("Plot y = piecewise(x>=1,2*x,-3*x)");val jump=capture("function-piecewise-jump")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);assertTrue(TypedGraphEngine().sample(d).curves.none{seg->seg.points.zipWithNext().any{pair->pair.first.x<1&&pair.second.x>=1}})}
            reset();ask("Draw y = 1/(x-3) only for x between -2 and 5");val restrictedPole=capture("function-restricted-pole")
            scenario.onActivity{val d=TypedGraphExpressionParser.parse(it.verificationViewModel.state.functions.single().expression);val sample=TypedGraphEngine().sample(d);assertTrue(sample.curves.size>1);assertTrue(sample.curves.none{seg->seg.points.zipWithNext().any{pair->pair.first.x<3&&pair.second.x>3}})}
            reset();ask("Graph x = cos(t), y = sin(t)");val parametric=capture("function-parametric")
            var curves:List<FunctionDefinition> = emptyList();scenario.onActivity{curves=it.verificationViewModel.state.functions;val d=TypedGraphExpressionParser.parse(curves.single().expression);assertTrue(d is TypedGraphExpression.Parametric);assertTrue(TypedGraphEngine().sample(d).curves.flatMap{it.points}.all{p->abs(p.x*p.x+p.y*p.y-1)<1e-8})}
            compose.onNode(matcher,useUnmergedTree=true).performTouchInput{val x=width*.8f;val y=height*.45f;down(0,androidx.compose.ui.geometry.Offset(x,y-60));down(1,androidx.compose.ui.geometry.Offset(x,y+60));moveTo(0,androidx.compose.ui.geometry.Offset(x-70,y-100));moveTo(1,androidx.compose.ui.geometry.Offset(x-70,y+20));advanceEventTime(300);up(0);up(1)};compose.waitForIdle();val panned=capture("function-after-pan")
            scenario.onActivity{assertEquals(curves,it.verificationViewModel.state.functions)}
            val createPixels=changed(before,parabola);val panPixels=changed(parametric,panned);assertTrue(createPixels>100);assertTrue(panPixels>100)
            File(folder,"function-canvas.json").writeText(JSONObject().put("created_pixels",createPixels).put("pan_pixels",panPixels).put("missing_equation_chip_resumed",true).put("restricted_domain_persists",true).put("vertical_line_on_sampling_grid",true).put("polar_parametric_piecewise_and_floor",true).put("piecewise_jump_and_restricted_pole",true).put("interactive_axes_preserve_functions",true).put("passed",true).toString(2))
            listOf(before,parabola,restricted,vertical,polar,piecewise,floor,upper,jump,restrictedPole,parametric,panned).forEach(Bitmap::recycle)
        }
    }
    @Test fun triangleConstraintsRenderAndLiveCopiesFollowParentEdits() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun type(text:String){compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(text:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();type(text);compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};close()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D),false,emptyList())};compose.waitForIdle();val before=capture("triangle-before")
            ask("Make an equilateral triangle of side 5","Triangle1: vertices");val equilateral=capture("triangle-equilateral")
            scenario.onActivity{val vm=it.verificationViewModel;val p=vm.state.shapes.single().pointIndices.map{vm.state.points[it]};assertTrue(RuhiTriangleLanguage.measures(p).first.all{abs(it-5)<1e-8})}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D),false,emptyList())};compose.waitForIdle()
            ask("Construct a right triangle with legs 3 and 4","Triangle1: vertices");val right=capture("triangle-right")
            scenario.onActivity{val vm=it.verificationViewModel;val p=vm.state.shapes.single().pointIndices.map{vm.state.points[it]};val lengths=RuhiTriangleLanguage.measures(p).first;assertTrue(lengths.any{abs(it-5)<1e-8});assertTrue(RuhiTriangleLanguage.measures(p).second.any{abs(it-90)<1e-8})}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-3.0,0.0),Vec2(0.0,0.0)),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Finish_811" to "1"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();type("Create the triangle defined by points Start_907, Finish_811, and Tip_662")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Create point Tip_662").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Create point Tip_662").performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText("Where should point Tip_662 go?",substring=true).fetchSemanticsNodes().isNotEmpty()};type("(-1,3)")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Triangle1: vertices",substring=true).fetchSemanticsNodes().isNotEmpty()};close()
            ask("Construct a second triangle congruent to the first","Triangle2: vertices");val copied=capture("triangle-congruent")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(2,Vec2(-1.0,4.0));val a=vm.state.shapes[0].pointIndices.map{vm.state.points[it]};val b=vm.state.shapes[1].pointIndices.map{vm.state.points[it]};assertTrue(a.indices.all{abs(a[it].distanceTo(a[(it+1)%3])-b[it].distanceTo(b[(it+1)%3]))<1e-8});assertNull(RuhiDialogueMemory.read(vm.state).pending);val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(vm.state.points,saved.points);assertEquals(vm.state.pointDependencies,saved.pointDependencies)};compose.waitForIdle();val moved=capture("triangle-congruent-after-parent-drag")
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val pixels=changed(before,equilateral);val edits=changed(copied,moved);assertTrue(pixels>100);assertTrue(edits>100)
            File(folder,"triangle-canvas.json").writeText(JSONObject().put("created_pixels",pixels).put("parent_edit_pixels",edits).put("equilateral_side",5).put("right_legs",JSONArray(listOf(3,4))).put("missing_point_resumed_triangle",true).put("congruent_copy_follows_parent",true).put("persistence",true).put("passed",true).toString(2))
            listOf(before,equilateral,right,copied,moved).forEach(Bitmap::recycle)
        }
    }
    @Test fun circleConstructionsRenderAndFollowTheirParentPointsAndRelations() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D),false,emptyList())};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun type(text:String){compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(text:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();type(text);compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};close()}
            fun changed(a:Bitmap,b:Bitmap):Int{val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val before=capture("circle-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();type("Draw a circle centered on point Anchor_907")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Create point Anchor_907").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Create point Anchor_907").performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText("Where should point Anchor_907 go?",substring=true).fetchSemanticsNodes().isNotEmpty()};type("(1,1)")
            compose.waitUntil(30_000){compose.onAllNodesWithText("C1: center (1.0, 1.0), radius 1.0",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val named=capture("circle-named-center");val pixels=changed(before,named);assertTrue(pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;assertNull(RuhiDialogueMemory.read(vm.state).pending);assertEquals(2,vm.state.points.size);val id=vm.state.shapes.single().id;vm.movePoint(0,Vec2(2.0,2.0));assertEquals(id,vm.state.shapes.single().id);val d=RuhiCircleLanguage.describe(vm.state,vm.state.shapes.single());assertEquals(Vec2(2.0,2.0),d.center);assertEquals(1.0,d.radius,1e-9)};compose.waitForIdle();val moved=capture("circle-after-center-drag")
            val triangle=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-2.0,0.0),Vec2(2.0,0.0),Vec2(0.0,3.0)),shapes=listOf(Shape2D("tri",Shape2DType.Triangle,listOf(0,1,2),"Tri_907")),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Finish_811" to "1","ruhi.studio.point.Tip_662" to "2"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(triangle,false,emptyList())};compose.waitForIdle()
            ask("Construct the incircle of triangle Tri_907","C1: center")
            ask("Show the circumcircle of triangle Tri_907","C2: center")
            val triangleCircles=capture("circle-triangle-constructions")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(2,Vec2(0.0,4.0));val inc=RuhiCircleLanguage.describe(vm.state,vm.state.shapes[1]);val circum=RuhiCircleLanguage.describe(vm.state,vm.state.shapes[2]);assertEquals(inc.radius,inc.center.y,1e-8);assertEquals(Vec2(0.0,1.5),circum.center);assertEquals(2.5,circum.radius,1e-9);val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!;assertEquals(vm.state.points,restored.points);assertEquals(vm.state.pointDependencies,restored.pointDependencies)};compose.waitForIdle();val triangleMoved=capture("circle-triangle-after-parent-drag")
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D),false,emptyList())};compose.waitForIdle();ask("Draw two externally tangent circles","C2: center")
            val tangent=capture("circle-externally-tangent")
            scenario.onActivity{val vm=it.verificationViewModel;val controls=vm.state.shapes[0].pointIndices;vm.movePoint(controls[1],Vec2(2.0,0.0));val a=RuhiCircleLanguage.describe(vm.state,vm.state.shapes[0]);val b=RuhiCircleLanguage.describe(vm.state,vm.state.shapes[1]);assertEquals(2.0,a.radius,1e-9);assertEquals(2.0,b.radius,1e-9);assertEquals(a.radius+b.radius,a.center.distanceTo(b.center),1e-9)};compose.waitForIdle();val tangentMoved=capture("circle-tangent-after-parent-drag")
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D),false,emptyList())};compose.waitForIdle();ask("Add a red circle centered at (0,0)","C1: center")
            val red=capture("circle-red");val redPixels=IntArray(red.width*red.height);red.getPixels(redPixels,0,red.width,0,0,red.width,red.height);val redCount=redPixels.count{android.graphics.Color.red(it)>180&&android.graphics.Color.green(it)<80&&android.graphics.Color.blue(it)<80};assertTrue("Red circle did not render",redCount>100)
            File(folder,"circle-canvas.json").writeText(JSONObject().put("named_center_changed_pixels",pixels).put("missing_point_resumed_circle",true).put("named_center_follows_parent",true).put("incircle_and_circumcircle_follow_triangle",true).put("externally_tangent_after_radius_edit",true).put("red_pixels",redCount).put("persistence",true).put("passed",true).toString(2))
            listOf(before,named,moved,triangleCircles,triangleMoved,tangent,tangentMoved,red).forEach(Bitmap::recycle)
        }
    }
    @Test fun nativePlanesAreCreatedThroughRuhiAndDihedralArcFollowsNativePointEdits() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(Point3D("corner-907","Anchor_907",Vec3(0.0,0.0,0.0)),Point3D("edge-811","East_811",Vec3(2.0,0.0,0.0)),Point3D("north-662","North_662",Vec3(0.0,2.0,0.0)),Point3D("up-553","Up_553",Vec3(0.0,0.0,2.0))))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial.copy(universalMathDocument=UniversalWorkspaceBridge.fromWorkspace(initial)),false,emptyList())};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun submit(text:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            fun ask(text:String,answer:String){submit(text);compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};close()}
            val before=capture("planes-before")
            ask("Create plane Floor through Anchor_907, East_811 and North_662","Created plane Floor")
            ask("Create plane Wall through Anchor_907, East_811, and Up_553","Created plane Wall")
            ask("Select planes Floor and Wall","Selected planes Floor and Wall")
            val selected=capture("planes-selected")
            ask("Clear plane selection","Plane selection cleared")
            submit("Measure the dihedral angle between two selected planes")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Floor + Wall").fetchSemanticsNodes().isNotEmpty()}
            val question=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"plane-angle-question.png").outputStream().use{question.compress(Bitmap.CompressFormat.PNG,100,it)};question.recycle()
            compose.onNodeWithText("Floor + Wall").performClick();compose.waitUntil(30_000){compose.onAllNodesWithText("90.0 degrees",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val right=capture("plane-angle-right")
            fun changed(a:Bitmap,b:Bitmap):Int {val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            val planePixels=changed(before,selected);val arcPixels=changed(selected,right);assertTrue(planePixels>500);assertTrue(arcPixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(2,vm.state.planes3D.size);assertEquals(initial.points3D,vm.state.points3D);assertEquals(90.0,RuhiSpatialPlaneLanguage.drawings(vm.state).single().degrees,1e-9);assertNull(RuhiDialogueMemory.read(vm.state).pending);vm.transformPoint3D(3){it.copy(position=Vec3(0.0,1.0,1.0))};assertEquals(45.0,RuhiSpatialPlaneLanguage.drawings(vm.state).single().degrees,1e-9);assertEquals(4,vm.state.points3D.size);val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false);assertNotNull(saved.diagnostics.toString(),saved.state);assertEquals(vm.state.planes3D,saved.state!!.planes3D);assertEquals(45.0,RuhiSpatialPlaneLanguage.drawings(saved.state!!).single().degrees,1e-9);assertTrue(UniversalMathDocumentEngine().validate(vm.state.universalMathDocument!!).valid)}
            compose.waitForIdle();val moved=capture("plane-angle-after-point-edit")
            File(folder,"plane-angle-canvas.json").writeText(JSONObject().put("planes_created_through_assistant",2).put("plane_changed_pixels",planePixels).put("arc_changed_pixels",arcPixels).put("pair_chip_resumed",true).put("initial_degrees",90).put("edited_degrees",45).put("same_point_ids",true).put("canonical_dependencies",true).put("persistence",true).put("no_helper_points",true).put("passed",true).toString(2))
            listOf(before,selected,right,moved).forEach(Bitmap::recycle)
        }
    }
    @Test fun spatialVectorPairQuestionAndArcUseCurrentThreeDimensionalDirections() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use{scenario->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val scene=WorkspaceState(module=MathModule.Geometry3D,vectors3D=listOf(Vector3D("native-907",Vec3(0.0,0.0,0.0),Vec3(3.0,0.0,0.0),"Sun_907"),Vector3D("native-811",Vec3(0.0,0.0,0.0),Vec3(0.0,0.0,3.0),"Moon_811")))
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(scene,false,emptyList());vm.selectVector3D(0)};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun submit(text:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()}
            fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            submit("Select vectors Sun_907 and Moon_811")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Selected vectors Sun_907 and Moon_811",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val selected=capture("spatial-angle-selected")
            scenario.onActivity{it.verificationViewModel.selectVector3D(0)};compose.waitForIdle()
            submit("What is the angle between these two 3D vectors?")
            compose.waitUntil(30_000){compose.onAllNodesWithText("Sun_907 + Moon_811").fetchSemanticsNodes().isNotEmpty()}
            val question=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(folder,"spatial-angle-question.png").outputStream().use{question.compress(Bitmap.CompressFormat.PNG,100,it)};question.recycle()
            compose.onNodeWithText("Sun_907 + Moon_811").performClick();compose.waitUntil(30_000){compose.onAllNodesWithText("90.0 degrees",substring=true).fetchSemanticsNodes().isNotEmpty()};close();val right=capture("spatial-angle-right")
            val a=IntArray(selected.width*selected.height);val b=IntArray(a.size);selected.getPixels(a,0,selected.width,0,0,selected.width,selected.height);right.getPixels(b,0,right.width,0,0,right.width,right.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue("3D arc did not change the selected-vector canvas",pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;assertNull(RuhiDialogueMemory.read(vm.state).pending);assertEquals(scene.vectors3D,vm.state.vectors3D);assertEquals(90.0,RuhiSpatialAngleLanguage.drawings(vm.state).single().degrees,1e-9);vm.transformVector3D(1){it.copy(end=Vec3(3.0,0.0,3.0))};assertEquals(45.0,RuhiSpatialAngleLanguage.drawings(vm.state).single().degrees,1e-9);assertEquals(2,vm.state.vectors3D.size);assertTrue(vm.state.points3D.isEmpty());val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!;assertEquals(RuhiSpatialAngleLanguage.drawings(vm.state),RuhiSpatialAngleLanguage.drawings(restored))}
            compose.waitForIdle();val moved=capture("spatial-angle-after-vector-edit")
            File(folder,"spatial-angle-canvas.json").writeText(JSONObject().put("changed_pixels",pixels).put("pair_chip_resumed",true).put("initial_degrees",90).put("edited_degrees",45).put("edited_z_component",3).put("stable_ids",true).put("persistence",true).put("no_helper_geometry",true).put("passed",true).toString(2))
            listOf(selected,right,moved).forEach(Bitmap::recycle)
        }
    }
    @Test fun referenceAngleArcsFollowIntersectionsAndUseTheVirtualOriginAndAxis() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val scene=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-4.0,0.0),Vec2(4.0,0.0),Vec2(0.0,-4.0),Vec2(0.0,4.0)),shapes=listOf(Shape2D("left",Shape2DType.Line,listOf(0,1),"Track_907"),Shape2D("right",Shape2DType.Line,listOf(2,3),"Track_811")))
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene,false,emptyList())};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun ask(text:String,answer:String){compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction();compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()};androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
            val before=capture("reference-angle-before")
            ask("Show the angle between intersecting lines Track_907 and Track_811","90.0 degrees")
            val right=capture("reference-angle-intersection")
            val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);right.getPixels(b,0,right.width,0,0,right.width,right.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(3,Vec2(4.0,4.0));val measured=RuhiReferenceAngleLanguage.measure(vm.state,RuhiReferenceAngleLanguage.marks(vm.state).single());assertEquals(2.0,measured.arms[1].x,1e-9);assertEquals(0.0,measured.arms[1].y,1e-9);assertEquals(63.43494882292201,measured.degrees,1e-9);assertEquals(4,vm.state.points.size)};compose.waitForIdle();val moved=capture("reference-angle-intersection-after-parent-drag")
            val originScene=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(2.0,0.0),Vec2(0.0,3.0),Vec2(99.0,99.0)),shapes=listOf(Shape2D("edge",Shape2DType.Segment,listOf(0,1),"AxisEdge")),labSessionValues=mapOf("ruhi.studio.point.Arm_907" to "0","ruhi.studio.point.Arm_811" to "1","ruhi.studio.point.O" to "2"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(originScene,false,emptyList())};compose.waitForIdle();ask("Show the angle at origin between Arm_907 and Arm_811","90.0 degrees");val origin=capture("reference-angle-origin")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(originScene.points,s.points);assertEquals(Vec2(0.0,0.0),RuhiReferenceAngleLanguage.measure(s,RuhiReferenceAngleLanguage.marks(s).single()).arms[1])}
            scenario.onActivity{it.verificationViewModel.importWorkspace(originScene,false,emptyList())};compose.waitForIdle();ask("Find angle between the x-axis and AxisEdge","123.690");val axis=capture("reference-angle-axis")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(originScene.points,s.points);assertEquals(123.69006752597979,RuhiReferenceAngleLanguage.measure(s,RuhiReferenceAngleLanguage.marks(s).single()).degrees,1e-9)}
            File(folder,"reference-angle-canvas.json").writeText(JSONObject().put("changed_pixels",pixels).put("intersection_initial_degrees",90).put("intersection_edited_degrees",63.43494882292201).put("intersection_edited_x",2).put("virtual_origin_ignores_named_O",true).put("axis_degrees",123.69006752597979).put("no_helper_points",true).put("passed",true).toString(2))
            listOf(before,right,moved,origin,axis).forEach(Bitmap::recycle)
        }
    }
    @Test fun angleArcAppearsAndUsesEditedCoordinates() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val scene=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(4.0,0.0),Vec2(0.0,0.0),Vec2(0.0,3.0)),shapes=listOf(Shape2D("triangle",Shape2DType.Triangle,listOf(0,1,2))),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Corner_811" to "1","ruhi.studio.point.Finish_662" to "2"))
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene,false,emptyList())};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun ask(text:String,answer:String) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()}
                androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            val before=capture("angle-before")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextInput("Measure angle Start_907, Corner_811, Finish_662");compose.onNodeWithText("Ask anything in maths").performImeAction()
            compose.waitUntil(30_000){compose.onAllNodesWithText("90.0 degrees",substring=true).fetchSemanticsNodes().isNotEmpty()}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            val after=capture("angle-right")
            val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue("Angle annotation must render",pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(0,Vec2(3.0,3.0));val mark=RuhiAngleLanguage.marks(vm.state).single();val p=mark.indices.map{vm.state.points[it]};assertEquals(45.0,RuhiAngleLanguage.degrees(p[0],p[1],p[2]),1e-9);assertEquals(3,vm.state.points.size)}
            compose.waitForIdle();val moved=capture("angle-after-parent-drag")
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene,false,emptyList())};compose.waitForIdle()
            ask("Show the angle bisector of ∠Start_907Corner_811Finish_662","Constructed the internal angle bisector")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(2,s.shapes.size);assertEquals(Shape2DType.AngleBisector,s.shapes.last().type);assertEquals(listOf(0,1,2),s.shapes.last().pointIndices);assertEquals(scene.points,s.points)}
            val bisector=capture("angle-bisector")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(0,Vec2(3.0,3.0));assertEquals(listOf(0,1,2),vm.state.shapes.last().pointIndices)};compose.waitForIdle();val bisectorMoved=capture("angle-bisector-after-parent-drag")
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene,false,emptyList())};compose.waitForIdle()
            ask("Angle from Corner_811Finish_662 to Corner_811Start_907 counterclockwise","270.0 degrees")
            scenario.onActivity{assertTrue(RuhiAngleLanguage.marks(it.verificationViewModel.state).single().oriented)}
            val oriented=capture("angle-counterclockwise")
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(0,Vec2(-4.0,0.0));val p=RuhiAngleLanguage.marks(vm.state).single().indices.map{vm.state.points[it]};assertEquals(90.0,RuhiAngleLanguage.orientedDegrees(p[0],p[1],p[2]),1e-9)};compose.waitForIdle();val orientedMoved=capture("angle-counterclockwise-after-parent-drag")
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(scene,false,emptyList());vm.selectShape(0)};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput("Is this angle acute or obtuse?");compose.onNodeWithText("Ask anything in maths").performImeAction()
            compose.waitUntil(30_000){compose.onAllNodesWithText("Vertex Corner_811").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Vertex Corner_811").performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText("90.0 degrees (right)",substring=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{assertNull(RuhiDialogueMemory.read(it.verificationViewModel.state).pending);assertEquals(1,it.verificationViewModel.selectedPoint)}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle();val selected=capture("angle-selected-vertex")
            val concave=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(1.0,1.0),Vec2(0.0,4.0)),shapes=listOf(Shape2D("quad",Shape2DType.Polygon,listOf(0,1,2,3))))
            scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(concave,false,emptyList());vm.selectCoordinatePoint(2)};compose.waitForIdle()
            ask("Is this angle acute or obtuse?","(reflex)")
            scenario.onActivity{val s=it.verificationViewModel.state;assertTrue(RuhiAngleLanguage.markDegrees(s,RuhiAngleLanguage.marks(s).single())>180)}
            val reflex=capture("angle-reflex")
            scenario.onActivity{it.verificationViewModel.movePoint(2,Vec2(4.0,4.0))};compose.waitForIdle();val convex=capture("angle-reflex-after-parent-drag")
            scenario.onActivity{val s=it.verificationViewModel.state;assertEquals(90.0,RuhiAngleLanguage.markDegrees(s,RuhiAngleLanguage.marks(s).single()),1e-8)}
            File(folder,"angle-canvas.json").writeText(JSONObject().put("changed_pixels",pixels).put("initial_degrees",90).put("edited_degrees",45).put("same_marker",true).put("native_bisector",true).put("bisector_parent_references",true).put("oriented_degrees",270).put("oriented_edited_degrees",90).put("vertex_chip_resumed",true).put("native_selected_point",true).put("reflex_became_right",true).put("passed",true).toString(2))
            listOf(before,after,moved,bisector,bisectorMoved,oriented,orientedMoved,selected,reflex,convex).forEach(Bitmap::recycle)
        }
    }
    @Test fun pointSlotModelRecoversUnseenNamesOnHeldoutPhrasingsAndSceneValuesChangeAnswers() {
        TensorFlowGraphNlp.install(context)
        val probes=JSONArray(context.assets.open("ruhi_semantic_nlp/slot_probes.json").bufferedReader().use{it.readText()});val records=JSONArray()
        for(i in 0 until probes.length()) {
            val p=probes.getJSONObject(i);val text=p.getString("text");val slots=RuhiLanguageServices.pointSlotClassifier!!.predict(text)
            if(p.has("roles")) {
                val expected=p.getJSONObject("roles");val actual=slots.filter{it.role!="OTHER"}.associate{it.value to it.role};val roles=expected.keys().asSequence().associateWith{expected.getString(it)}
                assertEquals(text,roles,actual);records.put(JSONObject().put("text",text).put("roles",JSONObject(actual)));continue
            }
            val first=slots.singleOrNull{it.role=="POINT_1"};val second=slots.singleOrNull{it.role=="POINT_2"}
            records.put(JSONObject().put("text",text).put("point1",first?.value).put("point2",second?.value))
            assertEquals(p.getString("point1"),first?.value);assertEquals(p.getString("point2"),second?.value)
        }
        var count=0
        val seeds=JSONArray(context.assets.open("ruhi_semantic_nlp/seed_examples.json").bufferedReader().use{it.readText()})
        for(i in 0 until 150) {
            val row=seeds.getJSONObject(i);val p=row.getJSONObject("parameters");var text=row.getString("text")
            text=text.replace(Regex("\\b${Regex.escape(p.getString("point1"))}\\b"),"Vertex_907").replace(Regex("\\b${Regex.escape(p.getString("point2"))}\\b"),"Omega_811")
            for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(offset in listOf(0.0,17.0)) {
                val a=Vec3(-3.0+offset,2.0,1.0);val b=Vec3(9.0,7.0+offset,-4.0)
                val s=WorkspaceState(module=module,points=listOf(Vec2(a.x,a.y),Vec2(b.x,b.y)),points3D=listOf(Point3D("p","Vertex_907",a),Point3D("q","Omega_811",b)),labSessionValues=mapOf("ruhi.studio.point.Vertex_907" to "0","ruhi.studio.point.Omega_811" to "1"))
                val r=RuhiDialogueManager().execute(s,text,WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
                val actual=Regex("= ([0-9.eE+-]+) units").find(r.message)!!.groupValues[1].toDouble();assertEquals(if(module==MathModule.Geometry2D)hypot(a.x-b.x,a.y-b.y) else(a-b).magnitude(),actual,1e-9);count++
            }
        }
        File(folder,"unseen-labels-native.json").writeText(JSONObject().put("heldout_slot_templates",records).put("scene_cases",count).put("passed",true).toString(2));assertEquals(600,count)
    }
    @Test fun visibleAssistantUsesArbitraryPointLabelsAndCurrentCoordinates() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use { scenario->
            compose.waitUntil(30_000){compose.onAllNodesWithContentDescription("Interactive 3D geometry canvas",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            fun scene(x:Double,y:Double)=WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(Point3D("alpha","Alpha7",Vec3(0.0,0.0,0.0)),Point3D("beta","Beta9",Vec3(x,y,0.0))))
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene(3.0,4.0).copy(points3D=scene(3.0,4.0).points3D.take(1)),false,emptyList())};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            fun ask(text:String,expected:String) {
                var priorTurns=0;scenario.onActivity{priorTurns=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.size}
                compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var current=false;scenario.onActivity{val turns=RuhiDialogueMemory.read(it.verificationViewModel.state).turns;current=turns.size>priorTurns&&turns.last().user==text&&turns.last().assistant.contains(expected)};current}
                compose.onNodeWithText(expected,substring=true).assertIsDisplayed()
            }
            ask("How far is Alpha7 from Beta9?","Point Beta9 is missing")
            compose.onNodeWithText("Create point Beta9").performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText("Where should point Beta9 go?",substring=true).fetchSemanticsNodes().isNotEmpty()}
            ask("(3,4,0)","Distance Alpha7 to Beta9 = 5.0 units")
            scenario.onActivity{it.verificationViewModel.importWorkspace(scene(6.0,8.0),false,emptyList())};compose.waitForIdle()
            ask("How far is Alpha7 from Beta9?","Distance Alpha7 to Beta9 = 10.0 units")
            captureCanvas(hasContentDescription("Interactive 3D geometry canvas"),"named-point-answer").recycle()
            scenario.onActivity{assertEquals(2,it.verificationViewModel.state.points3D.size)}
            File(folder,"visible-assistant.json").writeText(JSONObject().put("first_distance",5).put("changed_coordinate_distance",10).put("same_utterance",true).put("missing_point_chip",true).put("coordinate_answer_resumed_distance",true).put("current_turn_verified",true).put("committed_capture",true).put("no_extra_points",true).put("passed",true).toString(2))
        }
    }
    @Test fun midpointMarkerAndSlopeTriangleAppearAndFollowNativePointEdits() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use { scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{it.verificationViewModel.importWorkspace(WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(4.0,2.0)),shapes=listOf(Shape2D("edge",Shape2DType.Segment,listOf(0,1),"Edge_907",styleKey="#0000FF")),labSessionValues=mapOf("ruhi.studio.point.Alpha7" to "0","ruhi.studio.point.Beta9" to "1")),false,emptyList())};compose.waitForIdle()
            fun capture(name:String):Bitmap=captureCanvas(matcher,name)
            fun changed(a:Bitmap,b:Bitmap):Int {assertEquals(a.width,b.width);assertEquals(a.height,b.height);val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.indices.count{x[it]!=y[it]}}
            fun ask(text:String,ready:(WorkspaceState)->Boolean) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick();compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){var done=false;scenario.onActivity{done=ready(it.verificationViewModel.state)};done}
                androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            val before=capture("midpoint-before")
            ask("Mark the midpoint between Alpha7 and Beta9."){it.points.size==3&&it.points.last()==Vec2(2.0,1.0)}
            val midpoint=capture("midpoint-marker");val pointPixels=changed(before,midpoint);assertTrue("Midpoint marker did not change the canvas",pointPixels>30)
            ask("Show the slope triangle of Edge_907."){RuhiSlopeLanguage.marks(it).singleOrNull()?.triangle==true}
            val triangle=capture("slope-triangle");val trianglePixels=changed(midpoint,triangle);assertTrue("Slope triangle did not change the canvas",trianglePixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(1,Vec2(6.0,4.0));assertEquals(Vec2(3.0,2.0),vm.state.points.last());assertEquals(1,RuhiSlopeLanguage.marks(vm.state).size);val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!;assertEquals(vm.state.points,restored.points);assertEquals(RuhiSlopeLanguage.marks(vm.state),RuhiSlopeLanguage.marks(restored))}
            compose.waitForIdle();val moved=capture("midpoint-after-parent-drag");assertTrue(changed(triangle,moved)>100)
            File(folder,"midpoint-slope-canvas.json").writeText(JSONObject().put("midpoint_changed_pixels",pointPixels).put("slope_triangle_changed_pixels",trianglePixels).put("parent_edit_updated_same_marker",true).put("persistence",true).put("passed",true).toString(2))
            listOf(before,midpoint,triangle,moved).forEach(Bitmap::recycle)
        }
    }
    @Test fun nativePointLinkedLinesSegmentsAndRaysRenderAndFollowSavedParentEdits() {
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry3d")).use { scenario ->
            val matcher=hasContentDescription("Interactive 3D geometry canvas")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(
                Point3D("anchor-907","Anchor_907",Vec3(-2.0,-1.0,0.0)),
                Point3D("beta-811","Beta_811",Vec3(1.0,1.0,0.0)),
                Point3D("gamma-662","Gamma_662",Vec3(0.0,0.0,2.0))))
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial.copy(universalMathDocument=UniversalWorkspaceBridge.fromWorkspace(initial)),false,emptyList())}
            compose.waitForIdle()
            fun capture(name:String)=captureCanvas(matcher,name)
            fun ask(text:String,answer:String) {
                compose.onNodeWithContentDescription("Open AI Assistant").performClick()
                compose.onNodeWithText("Ask anything in maths").performTextClearance()
                compose.onNodeWithText("Ask anything in maths").performTextInput(text)
                compose.onNodeWithText("Ask anything in maths").performImeAction()
                compose.waitUntil(30_000){compose.onAllNodesWithText(answer,substring=true).fetchSemanticsNodes().isNotEmpty()}
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            }
            fun changed(a:Bitmap,b:Bitmap):Int {
                assertEquals(a.width,b.width);assertEquals(a.height,b.height)
                val x=IntArray(a.width*a.height);val y=IntArray(x.size)
                a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height)
                return x.indices.count{x[it]!=y[it]}
            }
            val before=capture("spatial-linear-before")
            ask("Draw a line from Anchor_907 to Beta_811.","Created line Line1")
            val lineImage=capture("spatial-linear-line")
            ask("Join Beta_811 and Gamma_662 with a segment.","Created segment Segment1")
            val segmentImage=capture("spatial-linear-segment")
            ask("Draw a ray starting at Anchor_907 and going through Gamma_662.","Created ray Ray1")
            val rayImage=capture("spatial-linear-ray")
            val records=JSONArray()
            scenario.onActivity { activity ->
                val vm=activity.verificationViewModel
                assertEquals(3,vm.state.linears3D.size);assertEquals(initial.points3D,vm.state.points3D)
                assertTrue(vm.state.solids.isEmpty());assertTrue(vm.state.vectors3D.isEmpty())
                val expected=listOf(listOf("anchor-907","beta-811"),listOf("beta-811","gamma-662"),listOf("anchor-907","gamma-662"))
                vm.state.linears3D.forEachIndexed { index,obj ->
                    assertEquals(expected[index],obj.pointIds)
                    val linear=NativeLinearGeometry.evaluate(vm.state,obj)
                    assertEquals(initial.points3D.single{it.id==obj.pointIds[0]}.position,linear.at(0.0))
                    assertEquals(initial.points3D.single{it.id==obj.pointIds[1]}.position,linear.at(1.0))
                    assertEquals(index==0,linear.containsParameter(-1.0));assertEquals(index!=1,linear.containsParameter(2.0))
                    val canonical=vm.state.universalMathDocument!!.objects.getValue("linear3d-${obj.id}")
                    assertEquals(UniversalMathKind.valueOf(obj.kind.name),canonical.kind)
                    assertEquals(obj.pointIds.map{"point3d-$it"}.toSet(),canonical.dependencies)
                    records.put(JSONObject().put("kind",obj.kind.name).put("point_ids",JSONArray(obj.pointIds)).put("negative_parameter",linear.containsParameter(-1.0)).put("beyond_second_point",linear.containsParameter(2.0)))
                }
                vm.transformPoint3D(2){it.copy(position=Vec3(0.0,-2.0,2.0))}
                val edited=NativeLinearGeometry.evaluate(vm.state,vm.state.linears3D.last())
                assertEquals(Vec3(2.0,-1.0,2.0),edited.direction)
                assertEquals(initial.points3D.take(2),vm.state.points3D.take(2))
                val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false)
                assertNotNull(saved.diagnostics.toString(),saved.state);assertEquals(vm.state.linears3D,saved.state!!.linears3D)
                assertEquals(edited,NativeLinearGeometry.evaluate(saved.state!!,saved.state!!.linears3D.last()))
                vm.importWorkspace(saved.state!!,false,emptyList())
            }
            compose.waitForIdle();val editedImage=capture("spatial-linear-edited")
            val pixels=listOf(changed(before,lineImage),changed(lineImage,segmentImage),changed(segmentImage,rayImage),changed(rayImage,editedImage))
            assertTrue("Each native construction and current parent edit must change the actual canvas: $pixels",pixels.all{it>100})
            File(folder,"spatial-linear-canvas.json").writeText(JSONObject().put("passed",true).put("cases",records).put("changed_pixels",JSONArray(pixels)).put("current_parent",true).put("canonical_dependencies",true).put("persistence",true).put("no_helper_vectors_or_points",true).put("committed_capture",true).toString(2))
            listOf(before,lineImage,segmentImage,rayImage,editedImage).forEach(Bitmap::recycle)
        }
    }

    @Test fun missingPriorContextQuestionsRestoreResumeAndRequireCompleteInstructions() {
        val rows=listOf(480 to "Use the same radius as before",481 to "No, make it 7 instead",483 to "Both of them",486 to "Continue where we stopped",490 to "Use x squared for the equation",491 to "Choose the second suggested answer")
        val records=JSONArray()
        for(isSpatial in listOf(true,false)) {
            ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace",if(isSpatial)"geometry3d"else"graph2d")).use { scenario ->
                val matcher=hasContentDescription(if(isSpatial)"Interactive 3D geometry canvas"else"Interactive graphing canvas with axes, curves, trace point, and annotations")
                compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
                fun open(){compose.onNodeWithContentDescription("Open AI Assistant").performClick()}
                fun close(){androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()}
                fun submit(t:String){compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(t);compose.onNodeWithText("Ask anything in maths").performImeAction()}
                fun waitText(t:String){compose.waitUntil(30_000){compose.onAllNodesWithText(t,substring=true).fetchSemanticsNodes().isNotEmpty()}}
                for((id,text) in rows.filter{(it.first in setOf(480,481,483))==isSpatial}) {
                    val initial=WorkspaceState(module=if(isSpatial)MathModule.Geometry3D else MathModule.Graph2D,surfaceLayers=emptyList())
                    scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
                    open();submit(text)
                    val question=RuhiDialogueFollowUp.missingContextQuestion(text)!!;waitText(question)
                    if(id==480||id==490) {
                        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                        File(folder,if(isSpatial)"missing-prior-spatial-question.png"else"missing-prior-graph-question.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
                    }
                    close()
                    scenario.onActivity { activity ->
                        val vm=activity.verificationViewModel;val memory=RuhiDialogueMemory.read(vm.state)
                        assertEquals("missing_prior_context",memory.pending!!.kind);assertEquals(text,memory.pending!!.source)
                        assertTrue(memory.lastChoices.isEmpty());assertTrue(vm.state.solids.isEmpty());assertTrue(vm.state.functions.isEmpty())
                        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state),recover=false).state!!
                        assertEquals(question,RuhiDialogueMemory.read(saved).pendingPrompt)
                        vm.importWorkspace(saved,false,emptyList())
                    }
                    compose.waitForIdle();open();waitText(question);submit("Continue");waitText(question)
                    submit("7");waitText(question)
                    scenario.onActivity{assertTrue(it.verificationViewModel.state.solids.isEmpty());assertTrue(it.verificationViewModel.state.functions.isEmpty());assertTrue(it.verificationViewModel.state.points.isEmpty())}
                    submit(if(isSpatial)"Create a sphere with radius 3"else"Plot y = x^2")
                    waitText("Ruhi — Applied");close()
                    scenario.onActivity { activity ->
                        val state=activity.verificationViewModel.state;assertNull(RuhiDialogueMemory.read(state).pending)
                        val value=if(isSpatial){assertEquals(1,state.solids.size);state.solids.single().radius}
                            else {assertEquals(1,state.functions.size);com.indianservers.aiexplorer.core.ExpressionEngine().compile(state.functions.single().expression.substringAfter('=')).eval(mapOf("x" to 2.0))}
                        assertEquals(if(isSpatial)3.0 else 4.0,value,1e-9)
                        records.put(JSONObject().put("source_row",id).put("module",state.module.name).put("question_saved",true).put("empty_until_instruction",true).put("applied",true).put("value",value))
                    }
                    if(id==480||id==490){val image=captureCanvas(matcher,if(isSpatial)"missing-prior-spatial-applied"else"missing-prior-graph-applied");image.recycle()}
                }
            }
        }
        File(folder,"missing-prior-context-native.json").writeText(JSONObject().put("passed",true).put("cases",records).put("saved_question",true).put("resume_and_partial_reply",true).put("no_fabricated_choices",true).put("no_geometry_until_complete_instruction",true).put("committed_capture",true).toString(2))
    }

}
