package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArVector2
import kotlin.math.*

enum class ArHandTool { Move, Rotate, Scale }
data class ArTrackedHand(
    val id: String,
    val landmarks: List<ArVector2>,
    /** Handedness classification confidence, not a per-landmark confidence estimate. */
    val confidence: Float = 1f,
    val handedness: String = id,
    val worldLandmarks: List<com.indianservers.aiexplorer.arengine.contract.ArVector3> = emptyList(),
)
data class ArHandFrame(val timestampMillis: Long, val hands: List<ArTrackedHand>)
enum class ArHandPhase { Idle, Begin, Update, End, Cancel }
data class ArHandAction(
    val phase: ArHandPhase,
    val cursor: ArVector2? = null,
    val pan: ArVector2 = ArVector2(0f, 0f),
    val rotationDegrees: Float = 0f,
    val scale: Float = 1f,
)

/** Screen-normalized hand gestures, independent of the camera/ML provider and mathematical scene. */
class ArHandGestureController(private val smoothing: Float = 1f, private val deadZone: Float = 0f) {
    private var filteredCenters = emptyList<ArVector2>()
    private var lastTimestamp = -1L
    private var armedSince: Long? = null
    private var activeIds = emptyList<String>()
    private var pinchedIds = emptySet<String>()
    private var baseline = emptyList<ArVector2>()
    private var pendingIds = emptyList<String>()
    private var activeTool: ArHandTool? = null
    private var aspect = 1f

    fun reset(cancel: Boolean = true): ArHandAction {
        val hadGesture = activeIds.isNotEmpty()
        filteredCenters = emptyList()
        activeIds = emptyList(); pinchedIds = emptySet(); baseline = emptyList()
        pendingIds = emptyList(); armedSince = null; activeTool = null; lastTimestamp = -1L
        return ArHandAction(if (hadGesture) if (cancel) ArHandPhase.Cancel else ArHandPhase.End else ArHandPhase.Idle)
    }

    fun update(frame: ArHandFrame, tool: ArHandTool, trackingReady: Boolean, viewportAspect: Float = 1f): ArHandAction {
        if (!trackingReady) return reset()
        val nextAspect = viewportAspect.takeIf { it.isFinite() && it > 0f } ?: 1f
        if (activeIds.isNotEmpty() && abs(nextAspect - aspect) > .01f) {
            aspect = nextAspect
            return reset()
        }
        aspect = nextAspect
        if (frame.timestampMillis <= lastTimestamp) return ArHandAction(ArHandPhase.Idle)
        if (lastTimestamp >= 0 && frame.timestampMillis - lastTimestamp > 350) {
            val stopped = reset()
            if (stopped.phase == ArHandPhase.Cancel) return stopped
        }
        lastTimestamp = frame.timestampMillis
        val valid = frame.hands.filter { it.landmarks.size == 21 && it.confidence >= .65f && it.landmarks.all { p -> p.x.isFinite() && p.y.isFinite() } }
            .groupBy { it.id }.values.filter { it.size == 1 }.map { it.single() }
            .sortedBy { it.id }.take(2)
        val pinches = valid.filter { hand ->
            val p = hand.landmarks
            val palm = distance(p[0], p[9])
            val ratio = if (palm > .015f) distance(p[4], p[8]) / palm else Float.POSITIVE_INFINITY
            ratio < if (hand.id in pinchedIds) .50f else .32f
        }
        pinchedIds = pinches.map { it.id }.toSet()
        val ids = pinches.map { it.id }
        val rawCenters = pinches.map { midpoint(it.landmarks[4], it.landmarks[8]) }
        val centers = if (ids == activeIds && filteredCenters.size == rawCenters.size) rawCenters.mapIndexed { i, p ->
            val old = filteredCenters[i]; val alpha = smoothing.coerceIn(.05f, 1f)
            ArVector2(old.x + alpha * (p.x-old.x), old.y + alpha * (p.y-old.y))
        } else rawCenters
        filteredCenters = centers
        val cursor = centers.firstOrNull() ?: valid.firstOrNull()?.landmarks?.get(8)
        if (activeIds.isNotEmpty() && (ids != activeIds || activeTool != tool)) {
            activeIds = emptyList(); baseline = emptyList(); armedSince = null; pendingIds = emptyList()
            return ArHandAction(ArHandPhase.End, cursor)
        }
        if (ids.isEmpty() || centers.any { it.x !in 0f..1f || it.y !in 0f..1f }) {
            armedSince = null; pendingIds = emptyList()
            if (activeIds.isNotEmpty()) return reset(cancel = false)
            return ArHandAction(ArHandPhase.Idle, cursor)
        }
        if (activeIds.isEmpty()) {
            if (pendingIds != ids) { pendingIds = ids; armedSince = frame.timestampMillis }
            if (frame.timestampMillis - (armedSince ?: frame.timestampMillis) < 120) return ArHandAction(ArHandPhase.Idle, cursor)
            activeIds = ids; activeTool = tool; baseline = centers
            return ArHandAction(ArHandPhase.Begin, cursor)
        }
        val start = average(baseline); val now = average(centers)
        val dx = (now.x - start.x).coerceIn(-.5f, .5f).let { if (abs(it) < deadZone) 0f else it }
        val dy = (now.y - start.y).coerceIn(-.5f, .5f).let { if (abs(it) < deadZone) 0f else it }
        val rotation = if (centers.size == 2) {
            val a = angle(centers); val b = angle(baseline)
            ((a - b + 540f) % 360f - 180f)
        } else dx * 180f
        val scale = if (centers.size == 2) distance(centers[0], centers[1]) / distance(baseline[0], baseline[1]).coerceAtLeast(.03f)
            else exp(-dy * 3f)
        return ArHandAction(ArHandPhase.Update, cursor,
            if (tool == ArHandTool.Move) ArVector2(dx, dy) else ArVector2(0f, 0f),
            if (tool == ArHandTool.Rotate) rotation else 0f,
            if (tool == ArHandTool.Scale) scale.coerceIn(.25f, 4f) else 1f)
    }
    private fun distance(a: ArVector2, b: ArVector2) = hypot((a.x-b.x)*aspect, a.y-b.y)
    private fun midpoint(a: ArVector2, b: ArVector2) = ArVector2((a.x+b.x)/2, (a.y+b.y)/2)
    private fun average(p: List<ArVector2>) = ArVector2(p.map { it.x }.average().toFloat(), p.map { it.y }.average().toFloat())
    private fun angle(p: List<ArVector2>) = Math.toDegrees(atan2((p[1].y-p[0].y).toDouble(), ((p[1].x-p[0].x)*aspect).toDouble())).toFloat()
}
