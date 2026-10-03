package com.indianservers.aiexplorer.arengine.session

import com.indianservers.aiexplorer.arengine.contract.*
import com.indianservers.aiexplorer.arengine.arcore.ArCoreConfiguration
import com.indianservers.aiexplorer.arengine.arcore.ArSessionFeatures
import org.junit.Assert.*
import org.junit.Test

class ArSurfaceTrackingTest {
    private val floor = ArPlaneSnapshot("floor", ArPose(), ArPlaneOrientation.HorizontalUp, 2.0, 2.0)
    private fun frame(t: Long, planes: List<ArPlaneSnapshot> = listOf(floor), position: ArVector3 = ArVector3.Zero) =
        ArFrameSnapshot(t, ArCameraSnapshot(ArPose(position), ArTrackingState.Tracking), planes)
    @Test fun requiresStableDistinctFrames() {
        val engine = ArSurfaceIntelligence()
        assertFalse(engine.observe(frame(1_000_000_000)).guidance.placementReady)
        assertFalse(engine.observe(frame(1_000_000_000)).guidance.placementReady)
        assertFalse(engine.observe(frame(1_249_000_000)).guidance.placementReady)
        assertTrue(engine.observe(frame(1_250_000_000)).guidance.placementReady)
    }
    @Test fun fastMotionResetsStability() {
        val engine = ArSurfaceIntelligence()
        engine.observe(frame(1_000_000_000))
        val moving = engine.observe(frame(1_100_000_000, position = ArVector3(1.0, 0.0, 0.0)))
        assertEquals(ArStudentTrackingStatus.MoveSlowly, moving.guidance.status)
        assertFalse(moving.guidance.placementReady)
    }
    @Test fun wallRequiresVerticalPlaneAndFreshStability() {
        val engine = ArSurfaceIntelligence()
        engine.observe(frame(1_000_000_000), ArSurfaceTarget.FloorTable)
        assertTrue(engine.observe(frame(1_300_000_000), ArSurfaceTarget.FloorTable).guidance.placementReady)
        assertFalse(engine.observe(frame(1_400_000_000), ArSurfaceTarget.Wall).guidance.placementReady)
        val wall = floor.copy(id = "wall", orientation = ArPlaneOrientation.Vertical)
        assertFalse(engine.observe(frame(1_500_000_000, listOf(wall)), ArSurfaceTarget.Wall).guidance.placementReady)
        assertTrue(engine.observe(frame(1_800_000_000, listOf(wall)), ArSurfaceTarget.Wall).guidance.placementReady)
    }
    @Test fun lostPlanePauseAndLongGapRequireRescan() {
        val engine = ArSurfaceIntelligence()
        engine.observe(frame(1_000_000_000))
        assertTrue(engine.observe(frame(1_300_000_000)).guidance.placementReady)
        assertFalse(engine.observe(frame(1_400_000_000, emptyList())).guidance.placementReady)
        assertFalse(engine.observe(frame(1_500_000_000)).guidance.placementReady)
        assertTrue(engine.observe(frame(1_800_000_000)).guidance.placementReady)
        assertFalse(engine.observe(frame(4_000_000_000)).guidance.placementReady)
        engine.reset()
        assertFalse(engine.assessment.guidance.placementReady)
    }
    @Test fun lightingFailureNeverClaimsStableSurface() {
        val engine = ArSurfaceIntelligence()
        val dark = frame(1_000_000_000).let { it.copy(camera = it.camera.copy(trackingFailure = ArTrackingFailure.InsufficientLight)) }
        assertEquals("More light needed", engine.observe(dark).guidance.title)
        assertFalse(engine.assessment.guidance.placementReady)
    }
    @Test fun featureDowngradesIncludeBaselineWithoutUnsupportedDepth() {
        for (depth in listOf(false, true)) for (hdr in listOf(false, true)) for (instant in listOf(false, true)) {
            val candidates = ArCoreConfiguration.candidates(depth, hdr, instant)
            assertEquals(candidates.size, candidates.distinct().size)
            assertEquals(ArSessionFeatures(false, false, false), candidates.last())
            assertTrue(candidates.all { (!it.depth || depth) && (!it.hdr || hdr) && (!it.instantPlacement || instant) })
        }
    }
}
