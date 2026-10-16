package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiGraphTransformTest {
    private fun near(a:Vec3,b:Vec3) { assertEquals(a.x,b.x,1e-7);assertEquals(a.y,b.y,1e-7);assertEquals(a.z,b.z,1e-7) }
    @Test fun meshUsesLocalScaleThenRotationThenTranslation() {
        val layer=SpatialSurfaceLayer("plane","x+2*y")
        val transform=GraphTransform(position=Vec3(3.0,-2.0,5.0),scale=Vec3(2.0,3.0,4.0)).rotated("z",90.0,Vec3(3.0,-2.0,5.0))
        val original=GraphSurfaceGeometry.mesh(layer,8);val changed=GraphSurfaceGeometry.mesh(layer.copy(graphTransform=transform),8)
        original.vertices.zip(changed.vertices).forEach { (a,b)->near(Vec3(3-3*a.y,-2+2*a.x,5+4*a.z),b) }
        assertEquals(original.triangleIndices,changed.triangleIndices)
    }
    @Test fun groupRotationAndTranslationPreserveDistancesAndUndoExactly() {
        val a=GraphAssistant();val h=CommandHistory();var state=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        fun run(s:String) { val r=a.execute(state,s,h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);state=r.state }
        run("Create two cubes.");run("Group the selected cubes.")
        val before=state;val distance=(before.surfaceLayers[0].graphTransform.position-before.surfaceLayers[1].graphTransform.position).magnitude()
        run("Rotate the group 90 degrees around the Z-axis.");run("Move the group to (3, 2, 1).")
        assertEquals(distance,(state.surfaceLayers[0].graphTransform.position-state.surfaceLayers[1].graphTransform.position).magnitude(),1e-8)
        near(Vec3(3.0,2.0,1.0),(state.surfaceLayers[0].graphTransform.position+state.surfaceLayers[1].graphTransform.position)*.5)
        run("Undo 2 actions.");assertEquals(before.surfaceLayers,state.surfaceLayers)
        run("Ungroup the selected objects.");assertTrue(state.surfaceLayers.all { it.graphGroup.isEmpty() })
    }
    @Test fun saveAndLinkedProjectionRoundTripTransformsAndGroups() {
        val t=GraphTransform(position=Vec3(-3.0,2.0,1.0),scale=Vec3(.5,2.0,3.0)).rotated("y",37.0,Vec3(0.0,0.0,0.0))
        val before=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=listOf(SpatialSurfaceLayer("surface-main","x+y",graphTransform=t,graphGroup="group-a",colorHex="#FF0000")))
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(before)).state!!
        assertEquals(before.surfaceLayers,restored.surfaceLayers)
        val document=UniversalWorkspaceBridge.fromWorkspace(before)
        assertTrue(document.objects["surface-main"]!!.presentation.styleKey.contains(t.encode()))
        assertTrue(document.objects["surface-main"]!!.presentation.styleKey.endsWith("group-a"))
    }
    @Test fun invalidAndUnderspecifiedTransformsDoNotTouchSceneOrHistory() {
        val a=GraphAssistant();val h=CommandHistory();val before=a.execute(WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList()),"Create a cube.",h).state
        for(text in listOf("Move the cube to (abc, 2, 3).","Scale the cube by 0.","Rotate the cube by 999999 degrees.","Make the cube infinitely large.")) {
            val protocol=h.protocol;val result=a.execute(before,text,h);assertEquals(text,GraphCommandStatus.INVALID,result.status);assertEquals(before,result.state);assertEquals(protocol,h.protocol)
        }
        for(text in listOf("Make the cube bigger.","Move the cube along the X-axis.","Rotate the cube around its own center.")) { val result=a.execute(before,text,h);assertEquals(text,GraphCommandStatus.CLARIFICATION,result.status);assertEquals(before,result.state) }
    }
    @Test fun languageModelCannotSupplyArgumentsOrBypassConfidenceAndIntentGates() {
        val previous=GraphLanguageRouter.classifier
        try {
            GraphLanguageRouter.classifier=GraphIntentClassifier { GraphIntentPrediction("MOVE",.99f,.95f) }
            assertEquals("move the cube to (3, 2, 1)",GraphLanguageRouter.canonical("relocate the cube to (3, 2, 1)"))
            assertNull(GraphLanguageRouter.canonical("do not relocate the cube"))
            assertNull(GraphLanguageRouter.canonical("erase the cube"))
            GraphLanguageRouter.classifier=GraphIntentClassifier { GraphIntentPrediction("MOVE",.5f,.1f) }
            assertNull(GraphLanguageRouter.canonical("relocate the cube to (3, 2, 1)"))
        } finally { GraphLanguageRouter.classifier=previous }
    }
    @Test fun completeCompoundGroupEditIsAtomicAndKeepsSurvivingIdentities() {
        val a=GraphAssistant();val h=CommandHistory();val before=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        val r=a.execute(before,"Create five cubes, group them, rotate the group 45 degrees, ungroup them, and delete only the first cube.",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(4,r.state.surfaceLayers.size)
        assertEquals(listOf("ruhi-graph-2","ruhi-graph-3","ruhi-graph-4","ruhi-graph-5"),r.state.surfaceLayers.map{it.id})
        assertTrue(r.state.surfaceLayers.all{it.graphGroup.isEmpty() && it.graphTransform.orientation!=GraphTransform().orientation})
        assertEquals(1,h.protocol.size)
        assertTrue(h.undo(r.state).surfaceLayers.isEmpty())
    }
    @Test fun distantMoveKeepsCoordinatesAndExpandsView() {
        val a=GraphAssistant();val h=CommandHistory();val start=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        val result=a.execute(start,"Reset everything, create a sphere, and move it to (5, 5, 5).",h)
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        near(Vec3(5.0,5.0,5.0),result.state.surfaceLayers.single().graphTransform.position)
        assertTrue(result.state.graph3DView.zoom<.35f)
        assertEquals(1,h.protocol.size)
        val undone=h.undo(result.state);assertTrue(undone.surfaceLayers.isEmpty());assertEquals(start.graph3DView,undone.graph3DView)
    }
    @Test fun multiLayerBudgetPreservesSingleLayerQualityAndEveryLayer() {
        assertEquals(52,GraphSurfaceGeometry.sceneDensity(52,1))
        assertEquals(8,GraphSurfaceGeometry.sceneDensity(24,10))
        val a=GraphAssistant();val result=a.execute(WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList()),"Generate 10 random spheres.",CommandHistory())
        assertEquals(GraphCommandStatus.APPLIED,result.status)
        assertEquals(10,result.state.surfaceLayers.size)
        assertTrue(result.state.surfaceLayers.all { GraphSurfaceGeometry.mesh(it,GraphSurfaceGeometry.sceneDensity(24,10)).triangleIndices.isNotEmpty() })
    }
}
