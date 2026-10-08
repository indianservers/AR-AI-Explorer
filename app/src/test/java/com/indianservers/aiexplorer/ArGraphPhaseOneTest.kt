package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.interaction.ArGizmoAxis
import org.junit.Assert.*
import org.junit.Test

class ArGraphPhaseOneTest {
    @Test fun graphTransformIsIndependentAndOneUndoPerGesture() {
        val vm = ExplorerViewModel(SavedStateHandle())
        val origin = vm.state.spatialPlacement
        val geometry = vm.state.surfaceLayers
        vm.beginArGraphObjectGesture()
        vm.previewArGraphObject("surface-main",Vec3(1.0,2.0,3.0),Vec3(0.0,30.0,0.0),2.0)
        vm.previewArGraphObject("surface-main",Vec3(2.0,3.0,4.0),Vec3(0.0,60.0,0.0),3.0)
        vm.endArGraphObjectGesture()
        assertEquals(Vec3(2.0,3.0,4.0),vm.state.arGraphObject("surface-main").position)
        assertEquals(origin,vm.state.spatialPlacement)
        assertEquals(geometry,vm.state.surfaceLayers)
        vm.undo(); assertEquals(ArGraphObjectState(),vm.state.arGraphObject("surface-main")); assertFalse(vm.canUndo)
        vm.redo(); assertEquals(3.0,vm.state.arGraphObject("surface-main").scale,0.0)
    }
    @Test fun lockedObjectCannotBeTransformedAndVisibilityHasUndo() {
        val vm = ExplorerViewModel(SavedStateHandle())
        vm.updateArGraphObject("surface-main","Lock graph") { it.copy(locked=true) }
        vm.beginArGraphObjectGesture(); vm.previewArGraphObject("surface-main",Vec3(1.0,0.0,0.0),Vec3(0.0,0.0,0.0),2.0); vm.endArGraphObjectGesture()
        assertEquals(Vec3(0.0,0.0,0.0),vm.state.arGraphObject("surface-main").position)
        vm.undo(); assertFalse(vm.state.arGraphObject("surface-main").locked)
        vm.updateArGraphObject("surface-main","Hide graph") { it.copy(visible=false) }
        vm.undo(); assertTrue(vm.state.arGraphObject("surface-main").visible)
    }
    @Test fun cancelRestoresAllObjectAndPointEdits() {
        val vm = ExplorerViewModel(SavedStateHandle()); val before = vm.state
        vm.beginArGraphObjectGesture(); vm.previewArGraphVertex("surface-main",0,Vec3(1.0,2.0,0.0)); vm.endArGraphObjectGesture(true)
        assertEquals(before,vm.state); assertFalse(vm.canUndo)
    }
    @Test fun axisScaleAndUnicodeLabelsRoundTrip() {
        val vm = ExplorerViewModel(SavedStateHandle())
        vm.updateArGraphObject("surface-main","Rename") { it.copy(label="曲面 | α") }
        vm.beginArGraphObjectGesture(); vm.previewArGraphObject("surface-main",Vec3(0.0,0.0,0.0),Vec3(0.0,0.0,0.0),2.0,ArGizmoAxis.X); vm.endArGraphObjectGesture()
        val obj = vm.state.arGraphObject("surface-main")
        assertEquals(Vec3(2.0,1.0,1.0),obj.axisScale)
        assertEquals(obj,ArGraphObjectState.decode(obj.encode()))
    }
    @Test fun toolbarHasOnlyThreeStatesAndTransitionsRetainNoSceneReferences() {
        assertEquals(3,ArGraphToolbarState.entries.size)
        assertEquals(ArGraphToolbarState.Compact,ArGraphToolbarState.Expanded.collapse())
        assertEquals(ArGraphToolbarState.Immersive,ArGraphToolbarState.Compact.collapse())
        assertEquals(ArGraphToolbarState.Compact,ArGraphToolbarState.Immersive.expand())
    }
    @Test fun projectAndActivityStatePreserveObjectPresentation() {
        val handle = SavedStateHandle()
        val vm = ExplorerViewModel(handle)
        vm.updateArGraphObject("surface-main","Rename") { it.copy(label="Saved graph",locked=true) }
        val restored = ExplorerViewModel(handle)
        assertEquals(vm.state.arGraphObject("surface-main"),restored.state.arGraphObject("surface-main"))
        val encoded = com.indianservers.aiexplorer.workspace.WorkspaceSnapshotCodec.encode(vm.state)
        val recovered = com.indianservers.aiexplorer.workspace.WorkspaceSnapshotCodec.decode(encoded).state!!
        assertEquals(vm.state.arGraphObject("surface-main"),recovered.arGraphObject("surface-main"))
    }
    @Test fun movingEditedGraphReusesItsGeometry() {
        val vm = ExplorerViewModel(SavedStateHandle())
        val scene = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.Graph3D,vm.state).scene
        val id = vm.state.surfaceLayers.first().id
        val cache = ArGraphGeometryCache()
        vm.beginArGraphObjectGesture(); vm.previewArGraphVertex(id,0,Vec3(.1,.2,0.0)); vm.endArGraphObjectGesture()
        val edited = scene.withArGraphObjects(vm.state,cache).primitives.first { it.id == id }.geometry
        vm.beginArGraphObjectGesture(); vm.previewArGraphObject(id,Vec3(1.0,0.0,0.0),Vec3(0.0,0.0,0.0),1.0); vm.endArGraphObjectGesture()
        val moved = scene.withArGraphObjects(vm.state,cache).primitives.first { it.id == id }.geometry
        assertSame(edited,moved)
    }
    @Test fun undoDuringPreviewCancelsGestureBeforeApplyingPreviousCommand() {
        val vm = ExplorerViewModel(SavedStateHandle())
        vm.updateArGraphObject("surface-main","Rename") { it.copy(label="A") }
        vm.beginArGraphObjectGesture(); vm.previewArGraphObject("surface-main",Vec3(1.0,0.0,0.0),Vec3(0.0,0.0,0.0),1.0)
        vm.undo(); vm.endArGraphObjectGesture()
        assertEquals(ArGraphObjectState(),vm.state.arGraphObject("surface-main"))
        assertFalse(vm.canUndo)
    }
}
