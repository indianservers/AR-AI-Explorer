package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.MathSpaceTransform
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import com.indianservers.aiexplorer.spatial.SpatialPose
import com.indianservers.aiexplorer.spatial.SpatialSafety
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.TransformSpatialPlacementCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialModelsTest {
    private val epsilon = 1e-8

    @Test
    fun mathAndAnchorCoordinatesRoundTripAcrossPoseAndScale() {
        val placement = SpatialScenePlacement(
            anchorId = "anchor",
            pose = SpatialPose(Vec3(1.2, .4, -2.1), Vec3(15.0, 35.0, -12.0), 1.7),
            metersPerMathUnit = .12,
        )
        val mathPoint = Vec3(2.5, -1.25, .75)
        val meters = MathSpaceTransform.mathToAnchorMeters(mathPoint, placement)
        val restored = MathSpaceTransform.anchorMetersToMath(meters, placement)

        assertEquals(mathPoint.x, restored.x, epsilon)
        assertEquals(mathPoint.y, restored.y, epsilon)
        assertEquals(mathPoint.z, restored.z, epsilon)
    }

    @Test
    fun placementScaleModesAndBoundsAreExplicit() {
        val placed = SpatialPlacementEngine.place(SpatialScenePlacement(), Vec3(0.0, 0.0, -1.0), 42L)
        val enlarged = SpatialPlacementEngine.scale(placed, 100.0)
        val oneToOne = SpatialPlacementEngine.setScaleMode(enlarged, ARScaleMode.OneToOne)

        assertTrue(placed.isPlaced)
        assertEquals(42L, placed.placedAt)
        assertEquals(8.0, enlarged.pose.uniformScale, epsilon)
        assertEquals(ARScaleMode.FitToSpace, enlarged.scaleMode)
        assertEquals(1.0, oneToOne.pose.uniformScale, epsilon)
        assertTrue(oneToOne.visibleScale.contains("unit"))
    }

    @Test
    fun degradedTrackingAlwaysBlocksPlacementAndPreservesGuidance() {
        assertTrue(SpatialSafety.guidance(TrackingQuality.Tracking).safeToPlace)
        assertFalse(SpatialSafety.guidance(TrackingQuality.Lost).safeToPlace)
        assertFalse(SpatialSafety.guidance(TrackingQuality.Tracking, movingTooFast = true).safeToPlace)
        assertTrue(SpatialSafety.guidance(TrackingQuality.Limited, lowLight = true).instruction.contains("well-lit"))
    }

    @Test
    fun spatialPlacementPersistsAndIsUndoable() {
        val initial = WorkspaceState()
        val placed = SpatialPlacementEngine.place(initial.spatialPlacement, Vec3(.2, 0.0, -1.4), 100L)
        val history = CommandHistory()
        val updated = history.execute(initial, TransformSpatialPlacementCommand(initial.spatialPlacement, placed, "Place scene"))
        val restored = history.undo(updated)
        val json = WorkspaceJson.export(updated)

        assertEquals(placed, updated.spatialPlacement)
        assertEquals(initial.spatialPlacement, restored.spatialPlacement)
        assertTrue(json.contains("\"spatialPlacement\""))
        assertTrue(json.contains("local-anchor-100"))
        assertTrue(json.contains("\"metersPerMathUnit\""))
    }
}
