package com.indianservers.aiexplorer.arengine.session

import com.indianservers.aiexplorer.arengine.contract.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ArSurfaceIntelligence100Test(private val case: Case) {
    data class Case(val name: String, val hit: ArHitCandidate, val target: ArSurfaceTarget, val frame: ArFrameSnapshot, val expected: Boolean) {
        override fun toString() = name
    }
    @Test fun selectsOnlyReliableMatchingSurface() {
        assertEquals(case.name, case.expected, ArSurfaceIntelligence().selectHit(case.frame, listOf(case.hit), case.target) != null)
    }
    companion object {
        private val floor = ArPlaneSnapshot("floor", ArPose(), ArPlaneOrientation.HorizontalUp, 2.0, 2.0)
        private val wall = floor.copy(id = "wall", orientation = ArPlaneOrientation.Vertical)
        private val frame = ArFrameSnapshot(1_000_000_000L, ArCameraSnapshot(ArPose(), ArTrackingState.Tracking), listOf(floor, wall))
        private val hit = ArHitCandidate("hit", ArHitType.Plane, ArPose(), 1.0, .9, .02, "floor", ArPlaneOrientation.HorizontalUp)
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}") fun cases(): List<Array<Any>> {
            val cases = mutableListOf<Case>()
            fun add(name: String, candidate: ArHitCandidate = hit, target: ArSurfaceTarget = ArSurfaceTarget.FloorTable, snapshot: ArFrameSnapshot = frame, expected: Boolean) {
                cases += Case(name, candidate, target, snapshot, expected)
            }
            for (type in ArHitType.entries) for (orientation in ArPlaneOrientation.entries) for (target in listOf(ArSurfaceTarget.FloorTable, ArSurfaceTarget.Wall)) {
                val id = if (orientation == ArPlaneOrientation.Vertical) "wall" else "floor"
                add("$type/$orientation/$target", hit.copy(type = type, planeOrientation = orientation, trackableId = id), target,
                    expected = type != ArHitType.InstantPlacement && type != ArHitType.Simulator && orientation == if (target == ArSurfaceTarget.Wall) ArPlaneOrientation.Vertical else ArPlaneOrientation.HorizontalUp)
            }
            for (distance in listOf(0.0, .01, .149, .15, .151, 1.0, 7.999, 8.0, 8.001, 100.0))
                add("range $distance", hit.copy(distanceMeters = distance), expected = distance in .15..8.0)
            for (confidence in listOf(0.0, .1, .3, .599, .6, .601, .7, .8, .99, 1.0))
                add("confidence $confidence", hit.copy(confidence = confidence), expected = confidence >= .6)
            for (uncertainty in listOf(0.0, .01, .1, .249, .25, .251, .3, .5, 1.0, 8.0))
                add("uncertainty $uncertainty", hit.copy(uncertaintyMeters = uncertainty), expected = uncertainty <= .25)
            for (tracking in ArTrackingState.entries)
                add("camera $tracking", snapshot = frame.copy(camera = frame.camera.copy(trackingState = tracking)), expected = tracking == ArTrackingState.Tracking)
            for (failure in ArTrackingFailure.entries)
                add("camera failure $failure", snapshot = frame.copy(camera = frame.camera.copy(trackingFailure = failure)), expected = failure == ArTrackingFailure.None)
            for (tracking in ArTrackingState.entries)
                add("hit $tracking", hit.copy(trackingState = tracking), expected = tracking == ArTrackingState.Tracking)
            for (extent in listOf(0.0, .119, .12, 2.0))
                add("plane extent $extent", snapshot = frame.copy(planes = listOf(floor.copy(extentXMeters = extent))), expected = extent >= .12)
            for (tracking in ArTrackingState.entries)
                add("plane $tracking", snapshot = frame.copy(planes = listOf(floor.copy(trackingState = tracking))), expected = tracking == ArTrackingState.Tracking)
            add("outside polygon", hit.copy(insideSurface = false), expected = false)
            add("expired plane", hit.copy(trackableId = "removed"), expected = false)
            add("zero timestamp", snapshot = frame.copy(timestampNanos = 0), expected = false)
            add("no planes", snapshot = frame.copy(planes = emptyList()), expected = false)
            add("wall target rejects floor", target = ArSurfaceTarget.Wall, expected = false)
            add("any target accepts floor", target = ArSurfaceTarget.Any, expected = true)
            require(cases.size == 100) { "Expected 100 cases, got ${cases.size}" }
            return cases.map { arrayOf<Any>(it) }
        }
    }
}
