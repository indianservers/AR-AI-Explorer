package com.indianservers.aiexplorer

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.assistant.workspace.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RuhiDialogueRenderingTest {
    @get:Rule val compose=createEmptyComposeRule()
    @Test fun conversationHeadRunsActualAndroidTensorFlowInference() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val model=TensorFlowTextModel(context,"ruhi_dialogue_nlp")
        val records=org.json.JSONArray()
        model.use {
            mapOf("pending equation y = sin(x) + 7" to "ANSWER_SLOT","idle make the height 39" to "MODIFY_SCENE","pending equation cancel it" to "CANCEL_PENDING","pending compare first explain quadratic equations" to "EXPLAIN").forEach{(text,expected)->
                val prediction=model.predict(text);assertEquals(text,expected,prediction.intent);assertTrue(prediction.confidence.isFinite())
                records.put(org.json.JSONObject().put("text",text).put("expected",expected).put("actual",prediction.intent).put("confidence",prediction.confidence).put("margin",prediction.margin))
            }
        }
        val folder=File(context.getExternalFilesDir(null),"ruhi-dialogue-audit").apply{mkdirs()}
        val metadata=org.json.JSONObject(context.assets.open("ruhi_dialogue_nlp/metadata.json").bufferedReader().use{it.readText()})
        File(folder,"dialogue-model-native.json").writeText(org.json.JSONObject().put("model_sha256",metadata.getString("model_sha256")).put("results",records).put("passed",true).toString(2))
    }
    @Test fun incompletePromptChipDrawsRealCurveThenConversationalEditChangesIt() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val evidence=File(instrumentation.targetContext.getExternalFilesDir(null),"ruhi-dialogue-audit").apply{mkdirs()}
        File(evidence,"result.json").delete()
        val intent=Intent(instrumentation.targetContext,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")
        ActivityScenario.launch<GraphVerificationActivity>(intent).use{scenario->
            compose.waitUntil(30_000){compose.onAllNodesWithContentDescription("Open AI Assistant").fetchSemanticsNodes().isNotEmpty()}
            compose.waitUntil(30_000){var focused=false;scenario.onActivity{focused=it.window.decorView.hasWindowFocus()};focused}
            scenario.onActivity{it.verificationViewModel.clearCurrentWorkspace()}
            fun screenshot(name:String):Bitmap {
                compose.waitForIdle();val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                File(evidence,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};return bitmap
            }
            val before=screenshot("before.png")
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            fun ask(text:String) {
                compose.onNodeWithText("Ask anything in maths").performTextClearance();compose.onNodeWithText("Ask anything in maths").performTextInput(text);compose.onNodeWithText("Ask anything in maths").performImeAction()
            }
            ask("Draw a graph.")
            compose.waitUntil(30_000){compose.onAllNodesWithText("What would you like to graph?").fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity{assertTrue(it.verificationViewModel.state.functions.isEmpty());assertTrue(it.verificationViewModel.ruhiAnswerChips.isNotEmpty())}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithText("y = sin(x)",useUnmergedTree=true).performScrollTo();screenshot("clarification-chips.png").recycle()
            compose.onNodeWithText("y = sin(x)",useUnmergedTree=true).performClick()
            compose.waitUntil(30_000){var ready=false;scenario.onActivity{ready=it.verificationViewModel.state.functions.size==1};ready}
            ask("Make it red.")
            compose.waitUntil(30_000){var ready=false;scenario.onActivity{ready=it.verificationViewModel.state.functions.singleOrNull()?.colorKey=="#FF0000"};ready}
            var confirmation="";scenario.onActivity{confirmation=it.verificationViewModel.status}
            compose.onNodeWithText(confirmation,useUnmergedTree=true).assertIsDisplayed()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(1,vm.state.functions.size);assertNull(RuhiDialogueMemory.read(vm.state).pending);assertTrue(vm.ruhiAnswerChips.isEmpty());assertNotNull(RuhiDialogueMemory.read(vm.state).turns.last())}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick()
            val after=screenshot("red-curve.png")
            val a=IntArray(before.width*before.height);val b=IntArray(a.size)
            before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height)
            var pixels=0
            for(y in before.height/3 until before.height*2/3)for(x in before.width/5 until before.width*4/5) if(a[y*before.width+x]!=b[y*before.width+x])pixels++
            assertTrue("Real graph pixels unchanged",pixels>0)
            compose.onNodeWithContentDescription("Open AI Assistant").performClick()
            var center=0.0;scenario.onActivity{center=it.verificationViewModel.state.graph2DView.centerX}
            compose.onNode(hasContentDescription("Interactive graphing canvas",substring=true),useUnmergedTree=true).performTouchInput {
                swipe(androidx.compose.ui.geometry.Offset(width*.25f,height*.32f),androidx.compose.ui.geometry.Offset(width*.35f,height*.32f),250)
            }
            compose.waitForIdle();scenario.onActivity{assertNotEquals("Exposed canvas must remain interactive while chat is open",center,it.verificationViewModel.state.graph2DView.centerX,1e-8)}
            screenshot("open-chat-canvas-pan.png").recycle()
            File(evidence,"result.json").writeText("{\"pending_question\":true,\"chips_clicked\":true,\"functions\":1,\"color\":\"#FF0000\",\"changed_canvas_region_pixels\":$pixels,\"canvas_interaction_while_open\":true,\"passed\":true}")
            before.recycle();after.recycle()
        }
    }
}
