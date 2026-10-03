package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.contract.*
import org.junit.Assert.*
import org.junit.Test

class ArHandWorkspaceTransformTest {
    @Test fun handPreviewUsesBaselineAndReleaseCreatesOneUndo() {
        val vm = ExplorerViewModel(SavedStateHandle()); val before = vm.state.spatialPlacement
        vm.beginSpatialGesture()
        vm.previewSpatialHandGesture(Vec3(.1, .2, .3), 30f, 2f)
        vm.previewSpatialHandGesture(Vec3(.2, .3, .4), 60f, 3f)
        assertEquals(before.pose.positionMeters + Vec3(.2, .3, .4), vm.state.spatialPlacement.pose.positionMeters)
        assertEquals(3.0, vm.state.spatialPlacement.pose.uniformScale, 0.0)
        vm.endSpatialGesture(); assertTrue(vm.canUndo)
        vm.undo(); assertEquals(before, vm.state.spatialPlacement)
    }
    @Test fun trackingCancellationRestoresConstruction() {
        val vm = ExplorerViewModel(SavedStateHandle()); val before = vm.state.spatialPlacement
        vm.beginSpatialGesture(); vm.previewSpatialHandGesture(Vec3(.1, .2, .3), 30f, 2f)
        vm.cancelSpatialGesture(); assertEquals(before, vm.state.spatialPlacement); assertFalse(vm.canUndo)
    }
    @Test fun anchorRefinementPreservesAuthoredOffset() {
        val origin = Vec3(1.0, 2.0, -3.0)
        val placement = SpatialPlacementEngine.place(SpatialScenePlacement(), origin, 1)
        val moved = SpatialPlacementEngine.move(placement, Vec3(.2, .3, .4))
        val anchor = ArAnchorHandle("native", ArPose(positionMeters = ArVector3(1.1,2.1,-3.1)), ArAnchorTrackingState.Tracking, 1, 2)
        val actual = moved.anchoredPosition(anchor)
        assertEquals(1.3, actual.x, .00001); assertEquals(2.4, actual.y, .00001); assertEquals(-2.7, actual.z, .00001)
        assertEquals(moved.pose.positionMeters, moved.anchoredPosition(null))
    }
}
