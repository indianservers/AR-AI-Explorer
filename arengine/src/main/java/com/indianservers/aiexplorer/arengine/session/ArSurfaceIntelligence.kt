package com.indianservers.aiexplorer.arengine.session

import com.indianservers.aiexplorer.arengine.contract.*
import kotlin.math.abs
import kotlin.math.acos

enum class ArSurfaceTarget { Any, FloorTable, Wall }

data class ArSurfaceAssessment(
    val guidance: ArTrackingGuidance,
    val floors: Int = 0,
    val walls: Int = 0,
    val confidence: Double = 0.0,
)

/** Shared camera/surface decisions for every AR workspace; estimates never masquerade as planes. */
class ArSurfaceIntelligence {
    private var previous: ArFrameSnapshot? = null
    private var stableSince: Long? = null
    private var previousTarget: ArSurfaceTarget? = null
    var assessment = ArSurfaceAssessment(ArTrackingGuidancePolicy.evaluate(null))
        private set

    @Synchronized fun reset() {
        previous = null
        stableSince = null
        previousTarget = null
        assessment = ArSurfaceAssessment(ArTrackingGuidancePolicy.evaluate(null))
    }

    @Synchronized fun observe(frame: ArFrameSnapshot, target: ArSurfaceTarget = ArSurfaceTarget.Any): ArSurfaceAssessment {
        val prior = previous
        val delta = prior?.let { (frame.timestampNanos - it.timestampNanos) / 1e9 }
        val discontinuity = delta != null && (delta < 0 || delta > 1.0)
        val movingFast = if (prior != null && delta != null && delta > 0 && !discontinuity) {
            val distance = (frame.camera.pose.positionMeters - prior.camera.pose.positionMeters).magnitude()
            val a = frame.camera.pose.orientation.normalized()
            val b = prior.camera.pose.orientation.normalized()
            val dot = abs(a.x*b.x + a.y*b.y + a.z*b.z + a.w*b.w).coerceIn(0.0, 1.0)
            distance / delta > 1.5 || Math.toDegrees(2 * acos(dot)) / delta > 150
        } else false
        if (discontinuity || previousTarget != target) stableSince = null
        val floors = frame.planes.count { reliablePlane(it) && it.orientation == ArPlaneOrientation.HorizontalUp }
        val walls = frame.planes.count { reliablePlane(it) && it.orientation == ArPlaneOrientation.Vertical }
        val found = when (target) { ArSurfaceTarget.Any -> floors + walls > 0; ArSurfaceTarget.FloorTable -> floors > 0; ArSurfaceTarget.Wall -> walls > 0 }
        val base = ArTrackingGuidancePolicy.evaluate(
            if (frame.camera.trackingFailure != ArTrackingFailure.None && frame.camera.trackingState == ArTrackingState.Tracking)
                frame.copy(camera = frame.camera.copy(trackingState = ArTrackingState.Paused)) else frame)
        val cameraReady = frame.timestampNanos > 0 && frame.camera.trackingState == ArTrackingState.Tracking && frame.camera.trackingFailure == ArTrackingFailure.None
        val guidance = when {
            !cameraReady -> { stableSince = null; base.copy(placementReady = false) }
            movingFast -> { stableSince = null; ArTrackingGuidance(ArStudentTrackingStatus.MoveSlowly, "Move device slowly", "Move gently so the floor and walls can be mapped accurately.", false) }
            !found -> {
                stableSince = null
                val surface = when (target) { ArSurfaceTarget.Wall -> "wall"; ArSurfaceTarget.FloorTable -> "floor or table"; else -> "floor, table or wall" }
                ArTrackingGuidance(ArStudentTrackingStatus.SearchingForSurface, "Searching for a $surface", "Aim at a textured $surface, improve lighting and move slowly sideways.", false)
            }
            else -> {
                if (stableSince == null) stableSince = frame.timestampNanos
                val ready = frame.timestampNanos - requireNotNull(stableSince) >= 250_000_000L
                ArTrackingGuidance(if (ready) ArStudentTrackingStatus.TrackingStable else ArStudentTrackingStatus.SurfaceDetected,
                    if (ready) "Surface ready" else "Surface detected",
                    if (ready) "Tap the highlighted surface to place the construction." else "Hold steady briefly to confirm the surface.", ready)
            }
        }
        previous = frame
        previousTarget = target
        assessment = ArSurfaceAssessment(guidance, floors, walls, if (guidance.placementReady) .95 else if (found && cameraReady) .6 else 0.0)
        return assessment
    }

    fun selectHit(frame: ArFrameSnapshot?, hits: List<ArHitCandidate>, target: ArSurfaceTarget): ArHitCandidate? {
        if (frame == null || frame.camera.trackingState != ArTrackingState.Tracking || frame.camera.trackingFailure != ArTrackingFailure.None || frame.timestampNanos <= 0) return null
        return ArHitPolicy.rank(hits.filter { hit ->
            hit.trackingState == ArTrackingState.Tracking && hit.insideSurface && hit.distanceMeters in .15..8.0 &&
                hit.confidence >= .6 && hit.uncertaintyMeters <= .25 && accepts(hit, target) &&
                (hit.type != ArHitType.Plane || frame.planes.any { it.id == hit.trackableId && reliablePlane(it) })
        }).firstOrNull()
    }

    companion object {
        fun reliablePlane(plane: ArPlaneSnapshot) = plane.trackingState == ArTrackingState.Tracking &&
            plane.extentXMeters >= .12 && plane.extentZMeters >= .12

        fun accepts(hit: ArHitCandidate, target: ArSurfaceTarget): Boolean {
            if (hit.type == ArHitType.InstantPlacement || hit.type == ArHitType.Simulator) return false
            val normalY = hit.pose.orientation.rotate(ArVector3.Up).y
            val orientation = hit.planeOrientation ?: when {
                normalY >= .85 -> ArPlaneOrientation.HorizontalUp
                normalY <= -.85 -> ArPlaneOrientation.HorizontalDown
                abs(normalY) <= .35 -> ArPlaneOrientation.Vertical
                else -> ArPlaneOrientation.Arbitrary
            }
            return when (target) {
                ArSurfaceTarget.FloorTable -> orientation == ArPlaneOrientation.HorizontalUp
                ArSurfaceTarget.Wall -> orientation == ArPlaneOrientation.Vertical
                ArSurfaceTarget.Any -> orientation in setOf(ArPlaneOrientation.HorizontalUp, ArPlaneOrientation.Vertical)
            }
        }
    }
}
