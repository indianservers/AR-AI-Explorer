package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

enum class SpatialHitType { Plane, Depth, Point, InstantPlacement, Simulator }
data class SpatialHit(
    val type: SpatialHitType,
    val positionMeters: Vec3,
    val normal: Vec3 = Vec3(0.0, 1.0, 0.0),
    val distanceMeters: Double,
    val confidence: Double,
    val uncertaintyMeters: Double,
    val trackableId: String? = null,
)
enum class AnchorTrackingState { Tracking, Paused, Stopped, Relocalizing, Lost }
data class PersistentSpatialAnchor(
    val id: String,
    val pose: SpatialPose,
    val createdAt: Long,
    val updatedAt: Long,
    val trackingState: AnchorTrackingState = AnchorTrackingState.Tracking,
    val cloudAnchorId: String? = null,
    val lessonId: String? = null,
)
data class EnvironmentLighting(
    val pixelIntensity: Float = 1f,
    val mainLightDirection: Vec3 = Vec3(0.0, -1.0, 0.0),
    val mainLightIntensity: Vec3 = Vec3(1.0, 1.0, 1.0),
    val sphericalHarmonics: List<Float> = emptyList(),
    val valid: Boolean = false,
)
data class ARFrameState(
    val timestampNanos: Long,
    val trackingQuality: TrackingQuality,
    val cameraPose: SpatialPose,
    val hitsAvailable: Boolean,
    val trackedPlaneCount: Int,
    val depthAvailable: Boolean,
    val lighting: EnvironmentLighting,
    val relocalizing: Boolean,
    val displayGeometryChanged: Boolean,
    val viewMatrix: List<Float> = identityMatrix,
    val projectionMatrix: List<Float> = identityMatrix,
)

private val identityMatrix = listOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

enum class ThermalLevel { Nominal, Light, Moderate, Severe, Critical }
enum class RenderQuality { Ultra, High, Balanced, Low, Safety }
data class SpatialPerformancePolicy(
    val quality: RenderQuality,
    val targetFps: Int,
    val surfaceDensity: Int,
    val shadows: Boolean,
    val depthOcclusion: Boolean,
    val hdrLighting: Boolean,
    val message: String,
)

object SpatialPerformanceManager {
    fun policy(thermal: ThermalLevel, averageFrameMillis: Double, batterySaver: Boolean = false): SpatialPerformancePolicy = when {
        thermal == ThermalLevel.Critical -> SpatialPerformancePolicy(RenderQuality.Safety, 20, 12, false, false, false, "AR paused soon: device temperature is critical.")
        thermal == ThermalLevel.Severe || averageFrameMillis > 45 -> SpatialPerformancePolicy(RenderQuality.Low, 24, 16, false, false, false, "Reduced detail protects device temperature and tracking.")
        thermal == ThermalLevel.Moderate || batterySaver || averageFrameMillis > 28 -> SpatialPerformancePolicy(RenderQuality.Balanced, 30, 24, false, true, true, "Balanced detail for stable spatial learning.")
        averageFrameMillis < 14 && thermal == ThermalLevel.Nominal -> SpatialPerformancePolicy(RenderQuality.Ultra, 60, 48, true, true, true, "High-quality spatial rendering.")
        else -> SpatialPerformancePolicy(RenderQuality.High, 45, 36, true, true, true, "Adaptive high-quality rendering.")
    }
}

data class SafePlacementBounds(
    val minimumDistanceMeters: Double = .35,
    val maximumDistanceMeters: Double = 4.0,
    val maximumExtentMeters: Double = 2.5,
    val minimumHeightMeters: Double = -.5,
    val maximumHeightMeters: Double = 2.2,
)
data class PlacementValidation(val accepted: Boolean, val adjustedPosition: Vec3, val messages: List<String>)

object SpatialPlacementSafety {
    fun validate(hit: SpatialHit, extentMeters: Double, bounds: SafePlacementBounds = SafePlacementBounds()): PlacementValidation {
        val messages = mutableListOf<String>()
        val distance = hit.distanceMeters.coerceIn(bounds.minimumDistanceMeters, bounds.maximumDistanceMeters)
        if (abs(distance - hit.distanceMeters) > 1e-9) messages += "Placement distance was clamped to the safe learning range."
        if (extentMeters > bounds.maximumExtentMeters) messages += "Scene is too large for the safe placement bounds; use Fit mode."
        val adjusted = Vec3(hit.positionMeters.x, hit.positionMeters.y.coerceIn(bounds.minimumHeightMeters, bounds.maximumHeightMeters), hit.positionMeters.z)
        if (adjusted.y != hit.positionMeters.y) messages += "Placement height was adjusted."
        val accepted = hit.confidence >= .35 && hit.uncertaintyMeters <= .25 && extentMeters <= bounds.maximumExtentMeters
        if (!accepted) messages += "Scan the surface again for a more reliable educational estimate."
        return PlacementValidation(accepted, adjusted, messages)
    }
}

object SpatialMeasurementEngine {
    fun measure(first: Vec3, second: Vec3, firstUncertainty: Double, secondUncertainty: Double, scaleUncertaintyFraction: Double = .02): SpatialMeasurementOverlay {
        val dx = second.x - first.x; val dy = second.y - first.y; val dz = second.z - first.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        val uncertainty = sqrt(firstUncertainty * firstUncertainty + secondUncertainty * secondUncertainty + (distance * scaleUncertaintyFraction) * (distance * scaleUncertaintyFraction))
        return SpatialMeasurementOverlay("measure", first, second, distance, "m", uncertainty, educationalEstimate = true)
    }
}

object SpatialAnchorPersistence {
    fun serialize(anchors: List<PersistentSpatialAnchor>): String = buildString {
        append("{\"schemaVersion\":1,\"anchors\":[")
        append(anchors.joinToString(",") { anchor ->
            "{\"id\":\"" + anchor.id.escapeJson() + "\",\"position\":[" + anchor.pose.positionMeters.x + "," + anchor.pose.positionMeters.y + "," + anchor.pose.positionMeters.z +
                "],\"rotation\":[" + anchor.pose.rotationDegrees.x + "," + anchor.pose.rotationDegrees.y + "," + anchor.pose.rotationDegrees.z +
                "],\"scale\":" + anchor.pose.uniformScale + ",\"createdAt\":" + anchor.createdAt + ",\"updatedAt\":" + anchor.updatedAt +
                ",\"tracking\":\"" + anchor.trackingState.name + "\",\"cloudAnchorId\":" + (anchor.cloudAnchorId?.let { "\"" + it.escapeJson() + "\"" } ?: "null") +
                ",\"lessonId\":" + (anchor.lessonId?.let { "\"" + it.escapeJson() + "\"" } ?: "null") + "}"
        })
        append("]}")
    }

    fun relocalize(saved: PersistentSpatialAnchor, observedPose: SpatialPose?, now: Long): PersistentSpatialAnchor =
        if (observedPose == null) saved.copy(updatedAt = now, trackingState = AnchorTrackingState.Relocalizing)
        else saved.copy(pose = observedPose, updatedAt = now, trackingState = AnchorTrackingState.Tracking)
}

enum class SpatialLessonSubject { Mathematics, PhysicsBridge, AstronomyBridge }
data class SpatialLesson(
    val id: String,
    val title: String,
    val subject: SpatialLessonSubject,
    val primitiveKinds: Set<SpatialPrimitiveKind>,
    val steps: List<String>,
    val learningGoal: String,
    val safetyPrompt: String,
    val futureNetworkShareable: Boolean = true,
)

object SpatialLessonCatalog {
    val lessons = listOf(
        SpatialLesson("conic-sections", "Walk through conic sections", SpatialLessonSubject.Mathematics, setOf(SpatialPrimitiveKind.Solid, SpatialPrimitiveKind.Curve), listOf("Place a cone.", "Move the cutting plane.", "Compare circle, ellipse, parabola and hyperbola."), "Relate plane angle to conic type.", "Keep the cone within arm’s reach."),
        SpatialLesson("vector-addition-ar", "Vector addition in space", SpatialLessonSubject.Mathematics, setOf(SpatialPrimitiveKind.Curve), listOf("Place u.", "Place v head-to-tail.", "Reveal u+v and the parallelogram."), "Understand 3D vector addition geometrically.", "Stay stationary while moving vector endpoints."),
        SpatialLesson("surface-gradients-ar", "Climb a surface gradient", SpatialLessonSubject.Mathematics, setOf(SpatialPrimitiveKind.Surface, SpatialPrimitiveKind.Curve), listOf("Place a surface.", "Move the trace point.", "Compare gradient and level curves."), "See the gradient as steepest ascent.", "Use Fit mode for large surfaces."),
        SpatialLesson("cross-sections-ar", "Volumes and cross-sections", SpatialLessonSubject.Mathematics, setOf(SpatialPrimitiveKind.Solid, SpatialPrimitiveKind.Surface), listOf("Place a solid.", "Sweep a section plane.", "Accumulate section areas."), "Connect cross-sections with volume.", "Do not walk backward while inspecting."),
        SpatialLesson("probability-surface-ar", "Probability landscapes", SpatialLessonSubject.Mathematics, setOf(SpatialPrimitiveKind.ProbabilitySurface), listOf("Place a joint density.", "Move interval walls.", "Read accumulated probability."), "Interpret probability as volume under a surface.", "Treat displayed measurements as estimates."),
        SpatialLesson("orbital-vectors-bridge", "Orbital and force vectors", SpatialLessonSubject.AstronomyBridge, setOf(SpatialPrimitiveKind.Solid, SpatialPrimitiveKind.Curve), listOf("Place a planet model.", "Show velocity and gravity vectors.", "Advance the orbit."), "Bridge vectors to future physics and astronomy labs.", "Use seated simulator if the room is small."),
    )
}

data class SpatialCollaborationEnvelope(
    val sessionId: String,
    val revision: Long,
    val anchorPayload: String,
    val sceneId: String,
    val participantAlias: String,
    val networkTransportAttached: Boolean = false,
)

data class SafetyChecklistItem(val id: String, val title: String, val requirement: String, val mandatory: Boolean = true)
object ARPrivacySafetyChecklist {
    val items = listOf(
        SafetyChecklistItem("camera-purpose", "Explain camera purpose", "Camera frames are used for local tracking and are not uploaded by default."),
        SafetyChecklistItem("permission", "Request permission in context", "Live AR starts only after explicit camera permission."),
        SafetyChecklistItem("fallback", "Keep simulator available", "Denying permission or lacking ARCore must not remove maths functionality."),
        SafetyChecklistItem("estimates", "Label measurements", "Every physical measurement displays uncertainty and educational-estimate status."),
        SafetyChecklistItem("awareness", "Protect physical awareness", "Placement guidance warns against walking backward or using unsafe bounds."),
        SafetyChecklistItem("anchors", "Minimize spatial retention", "Persist only mathematical poses and anchor identifiers, never camera imagery."),
        SafetyChecklistItem("sharing", "Future sharing is opt-in", "Network classroom spatial sessions require explicit teacher and learner consent."),
        SafetyChecklistItem("thermal", "Handle thermal pressure", "Reduce quality or pause before critical device temperature."),
    )
}

private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
