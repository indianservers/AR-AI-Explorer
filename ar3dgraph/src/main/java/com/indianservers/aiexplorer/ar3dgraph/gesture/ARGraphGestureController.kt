package com.indianservers.aiexplorer.ar3dgraph.gesture

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class ARGesturePointer(val id: Int, val x: Float, val y: Float)

enum class ARGestureAction { Down, PointerDown, Move, PointerUp, Up, Cancel }

data class ARGestureEvent(
    val action: ARGestureAction,
    val pointers: List<ARGesturePointer>,
    val eventTimeMillis: Long,
    val insideViewport: Boolean = true,
)

data class ARGestureAvailability(
    val graphReady: Boolean,
    val graphPlaced: Boolean,
    val trackingUsable: Boolean,
)

data class ARQuaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    val magnitudeSquared: Float get() = x * x + y * y + z * z + w * w
}

data class ARGraphTransformState(
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val uniformScale: Float = 1f,
) {
    val rotation: ARQuaternion get() {
        val yaw = Math.toRadians(yawDegrees.toDouble()).toFloat() * 0.5f
        val pitch = Math.toRadians(pitchDegrees.toDouble()).toFloat() * 0.5f
        val cy = cos(yaw); val sy = sin(yaw); val cp = cos(pitch); val sp = sin(pitch)
        return ARQuaternion(cy * sp, sy * cp, -sy * sp, cy * cp)
    }
}

sealed interface ARGestureMode {
    data object Idle : ARGestureMode
    data object PossibleTap : ARGestureMode
    data object Rotating : ARGestureMode
    data object Scaling : ARGestureMode
    data object Cancelled : ARGestureMode
}

sealed interface ARGestureOutcome {
    data object None : ARGestureOutcome
    data class TransformChanged(val transform: ARGraphTransformState) : ARGestureOutcome
    data class RepositionTap(val x: Float, val y: Float) : ARGestureOutcome
}

/** Platform-independent, deterministic tap/drag/pinch arbitration for the AR viewport. */
class ARGraphGestureController(
    private val touchSlopPixels: Float,
    initialTransform: ARGraphTransformState = ARGraphTransformState(),
    private val rotationDegreesPerPixel: Float = 0.25f,
    private val minimumScale: Float = 0.35f,
    private val maximumScale: Float = 3f,
    private val maximumTapMillis: Long = 500L,
) {
    var mode: ARGestureMode = ARGestureMode.Idle
        private set
    var transform: ARGraphTransformState = initialTransform.sanitized()
        private set
    private var disposed = false
    private var down: ARGesturePointer? = null
    private var previous: ARGesturePointer? = null
    private var downTime = 0L
    private var pinchDistance = 0f
    private var pinchStartScale = 1f

    fun onEvent(event: ARGestureEvent, availability: ARGestureAvailability): ARGestureOutcome {
        if (disposed) return ARGestureOutcome.None
        if (event.action == ARGestureAction.Cancel || !event.insideViewport || event.pointers.size > 2) {
            mode = ARGestureMode.Cancelled
            return ARGestureOutcome.None
        }
        if (!availability.graphReady || !availability.trackingUsable) {
            mode = if (event.action == ARGestureAction.Up) ARGestureMode.Idle else ARGestureMode.Cancelled
            return ARGestureOutcome.None
        }
        when (event.action) {
            ARGestureAction.Down -> {
                val pointer = event.pointers.firstOrNull() ?: return ARGestureOutcome.None
                down = pointer; previous = pointer; downTime = event.eventTimeMillis
                mode = ARGestureMode.PossibleTap
            }
            ARGestureAction.PointerDown -> {
                if (!availability.graphPlaced || event.pointers.size != 2) {
                    mode = ARGestureMode.Cancelled
                } else {
                    pinchDistance = distance(event.pointers).coerceAtLeast(1f)
                    pinchStartScale = transform.uniformScale
                    mode = ARGestureMode.Scaling
                }
            }
            ARGestureAction.Move -> {
                if (event.pointers.size == 2) {
                    if (!availability.graphPlaced || mode == ARGestureMode.Cancelled) return ARGestureOutcome.None
                    if (mode != ARGestureMode.Scaling) {
                        pinchDistance = distance(event.pointers).coerceAtLeast(1f)
                        pinchStartScale = transform.uniformScale
                        mode = ARGestureMode.Scaling
                        return ARGestureOutcome.None
                    }
                    transform = transform.copy(
                        uniformScale = (pinchStartScale * distance(event.pointers) / pinchDistance)
                            .coerceIn(minimumScale, maximumScale),
                    )
                    return ARGestureOutcome.TransformChanged(transform)
                }
                val pointer = event.pointers.firstOrNull() ?: return ARGestureOutcome.None
                if (mode == ARGestureMode.PossibleTap && movedBeyondSlop(pointer)) {
                    mode = if (availability.graphPlaced) ARGestureMode.Rotating else ARGestureMode.Cancelled
                }
                if (mode == ARGestureMode.Rotating) {
                    val last = previous ?: pointer
                    transform = transform.copy(
                        yawDegrees = wrap(transform.yawDegrees + (pointer.x - last.x) * rotationDegreesPerPixel),
                        pitchDegrees = (transform.pitchDegrees + (pointer.y - last.y) * rotationDegreesPerPixel).coerceIn(-80f, 80f),
                    )
                    previous = pointer
                    return ARGestureOutcome.TransformChanged(transform)
                }
                previous = pointer
            }
            ARGestureAction.PointerUp -> mode = ARGestureMode.Cancelled
            ARGestureAction.Up -> {
                val pointer = event.pointers.firstOrNull() ?: down
                val tap = mode == ARGestureMode.PossibleTap && pointer != null &&
                    !movedBeyondSlop(pointer) && event.eventTimeMillis - downTime <= maximumTapMillis
                mode = ARGestureMode.Idle
                if (tap) return ARGestureOutcome.RepositionTap(pointer.x, pointer.y)
            }
            ARGestureAction.Cancel -> Unit
        }
        return ARGestureOutcome.None
    }

    fun resetView(): ARGraphTransformState {
        transform = ARGraphTransformState(); mode = ARGestureMode.Idle
        return transform
    }

    fun cancelGesture() { mode = ARGestureMode.Cancelled }
    fun clearGraph(): ARGraphTransformState = resetView()
    fun dispose() { disposed = true; mode = ARGestureMode.Cancelled }

    private fun movedBeyondSlop(pointer: ARGesturePointer): Boolean {
        val start = down ?: return true
        return hypot(pointer.x - start.x, pointer.y - start.y) > touchSlopPixels
    }

    private fun distance(pointers: List<ARGesturePointer>): Float =
        hypot(pointers[0].x - pointers[1].x, pointers[0].y - pointers[1].y)

    private fun wrap(value: Float): Float = atan2(
        sin(Math.toRadians(value.toDouble())), cos(Math.toRadians(value.toDouble())),
    ).let { Math.toDegrees(it).toFloat() }

    private fun ARGraphTransformState.sanitized() = ARGraphTransformState(
        yawDegrees = if (yawDegrees.isFinite()) wrap(yawDegrees) else 0f,
        pitchDegrees = if (pitchDegrees.isFinite()) pitchDegrees.coerceIn(-80f, 80f) else 0f,
        uniformScale = if (uniformScale.isFinite()) uniformScale.coerceIn(minimumScale, maximumScale) else 1f,
    )
}
