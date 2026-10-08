package com.indianservers.aiexplorer

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.interaction.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ArCadPhaseTwoUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun exactVertexInspectorEditsConnectedMeshAndUndo() {
        val vm=ExplorerViewModel(SavedStateHandle())
        vm.upsertArCadNode(ArCadNode("box",ArCadType.Cuboid,arCadDefaults(ArCadType.Cuboid)))
        val original=ArCadSceneCompiler().build(SpatialRenderScene("test",emptyList()),vm.state)
        val selection=ArSelectionEngine.select(ArSelectionState(),ArPickHit("box",ArSubObjectKind.Vertex,0,0.0,com.indianservers.aiexplorer.arengine.contract.ArVector3.Zero),false)
        compose.runOnUiThread { compose.activity.setContent {
            androidx.compose.material3.MaterialTheme(colorScheme=androidx.compose.material3.darkColorScheme()) { ArGraphPhaseOneSheet("Precision inspector",{}) { ArCadPrecisionInspector(vm,original.withArGraphObjects(vm.state,ArGraphGeometryCache()),selection) } }
        } }
        compose.onNodeWithText("X, Y, Z / offset").performTextReplacement("pi/2, 3/4, sqrt(2)")
        compose.onNodeWithText("Set exact coordinates").performScrollTo().performClick()
        compose.runOnIdle {
            val mesh=ArCadTopology.decode(vm.state.labSessionValues.getValue("arCad.mesh.box"))
            assertEquals(Math.PI/2,mesh.vertices[0].x,1e-12); assertEquals(.75,mesh.vertices[0].y,0.0)
            assertEquals(original.primitives.single().geometry.triangles,mesh.triangles)
            vm.undo(); assertNull(vm.state.labSessionValues["arCad.mesh.box"])
            vm.redo(); assertNotNull(vm.state.labSessionValues["arCad.mesh.box"])
        }
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation(); val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.FileOutputStream(java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"ar-phase2-vertex.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }

    @Test fun realPreviewPicksFaceEdgeVertexAndDragsConnectedVertex() {
        val vm=ExplorerViewModel(SavedStateHandle()); vm.upsertArCadNode(ArCadNode("box",ArCadType.Cuboid,arCadDefaults(ArCadType.Cuboid)))
        val original=ArCadSceneCompiler().build(SpatialRenderScene("test",emptyList()),vm.state)
        var selection by mutableStateOf(ArSelectionState()); var mode by mutableStateOf(ArSubObjectKind.Whole)
        compose.runOnUiThread { compose.activity.setContent {
            val scene=original.withArGraphObjects(vm.state,ArGraphGeometryCache())
            ArCadPreviewCanvas(Modifier.fillMaxSize(),arCadHighlight(scene,selection),selection,mode,
                onSelect={ hit -> selection=if(hit==null) ArSelectionState() else ArSelectionEngine.select(selection,hit,false) },onStart=vm::beginArGraphObjectGesture,
                onEdit={ hit,g,delta,_,_ -> if(hit.kind==ArSubObjectKind.Whole) vm.previewArGraphObject(hit.objectId,delta,Vec3(0.0,0.0,0.0),1.0) else vm.previewArCadSubObject(hit.objectId,g,hit.kind,hit.subObjectIndex!!,delta,Vec3(0.0,0.0,0.0),1.0) },onEnd=vm::endArGraphObjectGesture,onInspect={})
        } }
        val canvas=compose.onNodeWithContentDescription("CAD 3D preview")
        canvas.performTouchInput { click(androidx.compose.ui.geometry.Offset(width*.5f,height*.52f)) }
        compose.runOnIdle { assertEquals("box",selection.primaryObjectId); mode=ArSubObjectKind.Face }
        canvas.performTouchInput { click(androidx.compose.ui.geometry.Offset(width*.5f,height*.52f)) }
        compose.runOnIdle { assertEquals(ArSubObjectKind.Face,selection.subObject!!.kind); mode=ArSubObjectKind.Edge }
        canvas.performTouchInput { click(androidx.compose.ui.geometry.Offset(width*.5f+25.36f,height*.52f+37.78f)) }
        compose.runOnIdle { assertEquals(ArSubObjectKind.Edge,selection.subObject!!.kind); mode=ArSubObjectKind.Vertex }
        canvas.performTouchInput { click(androidx.compose.ui.geometry.Offset(width*.5f-29.02f,height*.52f+29.11f)) }
        compose.runOnIdle { assertEquals(ArSubObjectKind.Vertex,selection.subObject!!.kind) }
        canvas.performTouchInput { val start=androidx.compose.ui.geometry.Offset(width*.5f-29.02f,height*.52f+29.11f); swipe(start,start+androidx.compose.ui.geometry.Offset(35f,0f),600) }
        compose.runOnIdle {
            val edited=ArCadTopology.decode(vm.state.labSessionValues.getValue("arCad.mesh.box")); assertEquals(original.primitives.single().geometry.triangles,edited.triangles); assertNotEquals(original.primitives.single().geometry.vertices,edited.vertices)
            vm.undo(); assertNull(vm.state.labSessionValues["arCad.mesh.box"]); vm.redo(); assertNotNull(vm.state.labSessionValues["arCad.mesh.box"])
        }
        compose.waitForIdle(); val instrumentation=InstrumentationRegistry.getInstrumentation(); val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.FileOutputStream(java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"ar-phase2-preview.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }

    @Test fun arSpaceCreationAndSceneExplorerRenameAndDuplicateWork() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,android.Manifest.permission.CAMERA)
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Open AR Space").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Open AR Space").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Expand AR Space menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Expand AR Space menu").performClick()
        compose.onNodeWithContentDescription("AR Space studio 3D Graph").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Expand graph toolbar").performClick()
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Cuboid").performScrollTo().performClick()
        compose.onNodeWithText("Create Cuboid").performScrollTo().performClick()
        compose.onNodeWithText("Created Cuboid").assertExists()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Scene explorer").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Cuboid").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Object name").onLast().performScrollTo().performTextReplacement("CAD box")
        compose.onAllNodesWithText("Rename").onLast().performScrollTo().performClick()
        compose.onAllNodesWithText("Duplicate").onLast().performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("CAD box copy").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Hide").onLast().performScrollTo().performClick()
        compose.onAllNodesWithText("Show").onLast().assertExists()
        val instrumentation=InstrumentationRegistry.getInstrumentation(); val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.FileOutputStream(java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"ar-phase2-explorer.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }
}
