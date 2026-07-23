package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArColor
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArMaterial
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArObjectKind
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArScene
import com.indianservers.aiexplorer.arengine.contract.ArSceneObject
import com.indianservers.aiexplorer.arengine.contract.ArScenePlacement
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArInteractionEngineTest {
    @Test
    fun previewAndCommitUseTheSamePoseAndRankPlanesFirst() {
        val depth = hit("depth", ArHitType.Depth, .9, ArVector3(0.0, 0.0, -1.0))
        val plane = hit("plane", ArHitType.Plane, .7, ArVector3(1.0, 2.0, 3.0))
        val preview = ArPlacementCoordinator.preview(listOf(depth, plane), ArScenePlacement(), 5.0)
        assertEquals("plane", preview.hit?.id)
        assertTrue(preview.canCommit)
        val anchor = ArAnchorHandle("anchor", plane.pose, ArAnchorTrackingState.Tracking, 1L, 1L)
        val committed = ArPlacementCoordinator.commit(preview, anchor)
        assertEquals(preview.placement.anchorPose, committed.anchorPose)
        assertEquals("anchor", committed.anchorId)
    }

    @Test
    fun pickerReturnsWholeAndSubObjectsWithStableOcclusionOrder() {
        val hits = ArScenePicker.pickAll(
            scene(),
            ArRay(ArVector3(0.02, 0.02, 2.0), ArVector3(0.0, 0.0, -1.0)),
        )
        assertTrue(hits.any { it.kind == ArSubObjectKind.Whole })
        assertTrue(hits.any { it.kind == ArSubObjectKind.Face })
        assertEquals(hits.indices.toList(), hits.map { it.occlusionRank })
    }

    @Test
    fun selectionCyclesLocksHidesAndIsolatesWithoutChangingIds() {
        val hitA = ArPickHit("a", ArSubObjectKind.Whole, null, 1.0, ArVector3.Zero)
        val hitB = hitA.copy(objectId = "b", distance = 2.0)
        var state = ArSelectionEngine.select(ArSelectionState(), hitA, false)
        state = ArSelectionEngine.select(state, hitB, true)
        assertEquals(setOf("a", "b"), state.objectIds)
        state = ArSelectionEngine.toggleLock(state)
        assertEquals(setOf("a", "b"), state.lockedObjectIds)
        assertEquals(hitA, ArSelectionEngine.cycle(listOf(hitA, hitB), hitB))
        assertEquals(setOf("a", "b"), ArSelectionEngine.isolate(state).isolatedObjectIds)
        assertTrue(ArSelectionEngine.hideSelected(state).objectIds.isEmpty())
    }

    @Test
    fun gizmoGestureCoalescesToOneSemanticCommandAndLeavesAnchorUntouched() {
        val source = scene()
        val selection = ArSelectionState(setOf("triangle"), "triangle")
        val gesture = ArGizmoEngine.preview(
            ArGizmoEngine.begin(source, selection, ArGizmoMode.Translate, ArGizmoAxis.X),
            translation = ArVector3(1.0, 2.0, 3.0),
        )
        val changed = ArGizmoEngine.apply(source, gesture)
        val command = ArGizmoEngine.end(gesture)
        assertNotNull(command)
        assertEquals(ArVector3(1.0, 0.0, 0.0), changed.objects.single().localTransform.offsetMeters)
        assertEquals(source.placement.anchorPose, changed.placement.anchorPose)
        assertNull(ArGizmoEngine.end(ArGizmoEngine.begin(source, selection, ArGizmoMode.Scale, ArGizmoAxis.Uniform)))
    }

    @Test
    fun snappingProjectsToAxesAndFaces() {
        val axis = ArSnapTarget("x", ArSnapKind.Axis, ArVector3.Zero, direction = ArVector3(1.0, 0.0, 0.0))
        val result = ArConstraintSnapEngine.snap(ArVector3(.4, .02, .03), listOf(axis), .1)
        assertEquals(ArVector3(.4, 0.0, 0.0), result.snappedPoint)
        assertNotNull(result.target)
    }

    @Test
    fun clipboardAndDeleteMaintainAnnotationsAndMeasurementIntegrity() {
        val source = scene()
        val selected = ArSelectionState(setOf("triangle"), "triangle")
        val clipboard = ArSceneEditor.copy(source, selected)
        val pasted = ArSceneEditor.paste(source, selected, clipboard, { "$it-copy" })
        assertEquals(2, pasted.scene.objects.size)
        val deleted = ArSceneEditor.delete(pasted.scene, pasted.selection)
        assertEquals(listOf("triangle"), deleted.scene.objects.map { it.id })
        assertEquals(setOf("triangle-copy"), deleted.removedObjectIds)
    }

    @Test
    fun unreliablePlacementCannotCommit() {
        val preview = ArPlacementCoordinator.preview(
            listOf(hit("weak", ArHitType.InstantPlacement, .2, ArVector3.Zero)),
            ArScenePlacement(),
            1.0,
        )
        assertFalse(preview.canCommit)
    }

    @Test
    fun trackingLossAndRecoveryPreserveSceneAndSelection() {
        val source = scene()
        val selection = ArSelectionState(setOf("triangle"), "triangle")
        val lost = ArTrackingRecoveryCoordinator.lost(source, selection)
        assertFalse(lost.manipulationEnabled)
        assertEquals(source, lost.scene)
        assertEquals(selection, lost.selection)
        assertTrue(ArTrackingRecoveryCoordinator.recovered(lost).manipulationEnabled)
    }

    @Test
    fun stylusPrecisionIsPressureIndependentAndHoverAware() {
        val policy = ArInputPrecisionPolicy.forDevice(ArPointerDevice.Stylus, precisionMode = true, hovering = true)
        assertEquals(.2, policy.movementMultiplier, 0.0)
        assertTrue(policy.pressureIndependent)
        assertTrue(policy.hoverPreview)
    }

    private fun hit(id: String, type: ArHitType, confidence: Double, point: ArVector3) =
        ArHitCandidate(id, type, ArPose(point), 1.0, confidence, .02)

    private fun scene() = ArScene(
        id = "scene",
        revision = 0,
        objects = listOf(
            ArSceneObject(
                id = "triangle",
                kind = ArObjectKind.Surface,
                label = "Triangle",
                mesh = ArMesh(
                    vertices = listOf(ArVector3(0.0, 0.0, 0.0), ArVector3(1.0, 0.0, 0.0), ArVector3(0.0, 1.0, 0.0)),
                    triangleIndices = listOf(0, 1, 2),
                    lineIndices = listOf(0, 1, 1, 2, 2, 0),
                ),
                material = ArMaterial(ArColor(.2f, .7f, 1f)),
            ),
        ),
    )
}
