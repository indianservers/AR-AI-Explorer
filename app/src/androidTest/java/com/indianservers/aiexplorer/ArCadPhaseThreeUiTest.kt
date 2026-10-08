package com.indianservers.aiexplorer

import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.spatial.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ArCadPhaseThreeUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun dismissSystemFullscreenHint() {
        val root=InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return
        var hint=false; var button:android.view.accessibility.AccessibilityNodeInfo?=null
        fun visit(node:android.view.accessibility.AccessibilityNodeInfo,depth:Int) {
            if(depth>15) return
            if(node.text?.toString()=="Viewing full screen") hint=true
            if(node.text?.toString()=="Got it") button=node
            for(i in 0 until node.childCount) node.getChild(i)?.let { visit(it,depth+1) }
        }
        visit(root,0)
        if(hint && button!=null) { button!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK); Thread.sleep(200) }
    }
    private fun screenshot(name:String) {
        compose.waitForIdle(); Thread.sleep(300); dismissSystemFullscreenHint()
        val instrumentation=InstrumentationRegistry.getInstrumentation(); val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.FileOutputStream(java.io.File(instrumentation.targetContext.getExternalFilesDir(null),name)).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }
    private fun cancelShareChooser() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        fun resumed():Boolean {
            var result=false
            instrumentation.runOnMainSync { result=compose.activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
            return result
        }
        // Dialog-root package names vary by Android version; Activity lifecycle identifies the real chooser transition.
        dismissSystemFullscreenHint()
        compose.waitUntil(15_000) { dismissSystemFullscreenHint(); !resumed() }
        var returned=false
        for(attempt in 0..2) {
            dismissSystemFullscreenHint()
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            val deadline=android.os.SystemClock.uptimeMillis()+2500
            while(android.os.SystemClock.uptimeMillis()<deadline && !resumed()) Thread.sleep(100)
            if(resumed()) { returned=true; break }
        }
        assertTrue("Share chooser did not resume the app",returned)
        compose.waitUntil(10_000) { runCatching { compose.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        compose.waitForIdle()
    }
    @Test fun realArSpaceGuidedParaboloidAnalysisAndPresentation() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,android.Manifest.permission.CAMERA)
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Open AR Space").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Expand AR Space menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Expand AR Space menu").performClick()
        compose.onNodeWithContentDescription("AR Space studio 3D Graph").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Expand graph toolbar").performClick()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Guided activities").performScrollTo().performClick()
        compose.onNodeWithText("Gradient").performScrollTo().performClick()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Surface analysis").performScrollTo().performClick()
        compose.onNodeWithText("Point local X").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Apply Point local X").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("f(x,y): 1.25").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("f(x,y): 1.25").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed()
        screenshot("ar-phase3-analysis.png")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Scene tools").performScrollTo().performClick()
        compose.onNode(hasText("LOW") or hasText("Selected LOW")).performScrollTo().performClick()
        compose.onNodeWithText("Selected LOW").assertExists()
        compose.onNodeWithText("Save AR scene").performScrollTo().performClick()
        compose.onNodeWithText("Scene saved with equations, construction, edits and world scale").performScrollTo().assertIsDisplayed()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val cache=java.io.File(instrumentation.targetContext.cacheDir,"shared-maths")
        val reportStarted=System.currentTimeMillis()
        compose.onNodeWithText("Share equations and measurements").performScrollTo().performClick()
        compose.waitUntil(10_000) { java.io.File(cache,"ar-space-report.txt").let { it.exists() && it.lastModified()>=reportStarted } }
        Thread.sleep(300)
        assertTrue(java.io.File(cache,"ar-space-report.txt").readText().contains("FunctionSurface"))
        cancelShareChooser()
        val projectStarted=System.currentTimeMillis()
        compose.onNodeWithText("Share scene project").performScrollTo().performClick()
        compose.waitUntil(10_000) { cache.listFiles()?.any { it.extension=="aiexplorer" && it.lastModified()>=projectStarted }==true }
        Thread.sleep(300)
        val project=cache.listFiles()!!.filter { it.extension=="aiexplorer" }.maxBy { it.lastModified() }
        assertNotNull(com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec.decode(project.readText()).state)
        cancelShareChooser()
        val imageStarted=System.currentTimeMillis()
        compose.onNodeWithText("Share scene image").performScrollTo().performClick()
        compose.waitUntil(10_000) { cache.listFiles()?.any { it.extension=="png" && it.lastModified()>=imageStarted }==true }
        Thread.sleep(300)
        val image=cache.listFiles()!!.filter { it.extension=="png" }.maxBy { it.lastModified() }
        val bounds=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
        android.graphics.BitmapFactory.decodeFile(image.absolutePath,bounds)
        assertTrue(bounds.outWidth>0 && bounds.outHeight>0)
        cancelShareChooser()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Scene tools").performScrollTo().performClick()
        compose.onNodeWithText("Presentation mode").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Reveal graph toolbar").assertExists()
        screenshot("ar-phase3-presentation.png")
        compose.onNodeWithContentDescription("Reveal graph toolbar").performClick()
        compose.onNodeWithContentDescription("Expand graph toolbar").performClick()
        compose.onNodeWithText("Back").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun slicePlaybackStopsAndCommitsOneUndo() {
        val vm=ExplorerViewModel(SavedStateHandle()); launchArCadActivity(vm,ArCadActivity.Conics)
        val before=vm.state; val history=vm.universalHistoryDepth
        compose.runOnUiThread { compose.activity.setContent { MaterialTheme(colorScheme=darkColorScheme()) {
            val scene=ArCadSceneCompiler().build(SpatialRenderScene("test",emptyList()),vm.state).withArGraphObjects(vm.state,ArGraphGeometryCache())
            ArGraphPhaseOneSheet("Surface analysis",{}) { ArCadAnalysisPanel(vm,scene,ArCadAnalysis.enrich(scene,vm.state)) }
        } } }
        compose.onNodeWithText("Animate slice (+/- 1 unit)").performClick()
        Thread.sleep(700)
        compose.onNodeWithText("Stop slice animation").performClick()
        compose.runOnIdle { assertFalse(vm.arGestureInProgress); assertEquals(history+1,vm.universalHistoryDepth); assertNotEquals(before.labSessionValues,vm.state.labSessionValues); vm.undo(); assertEquals(before.labSessionValues,vm.state.labSessionValues) }
    }
}
