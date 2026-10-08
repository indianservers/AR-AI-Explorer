package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import kotlin.math.hypot

enum class ArRecognizedGesture { NONE, POINT, PINCH_START, PINCH_HOLD, PINCH_RELEASE, OPEN_PALM, FIST, TWO_HAND_SCALE, TWO_HAND_ROTATE }
data class ArGestureObservation(val gesture: ArRecognizedGesture, val cursor: ArVector2?, val handCount: Int)

/** Landmark semantics and dwell are kept outside Compose. Pinch transforms use the baseline controller. */
class ArGestureRecognizer {
    private var pinch = false
    private var candidate = ArRecognizedGesture.NONE
    private var stable = ArRecognizedGesture.NONE
    private var count = 0
    private var lastTimestamp = -1L
    fun reset() { pinch = false; candidate = ArRecognizedGesture.NONE; stable = candidate; count = 0; lastTimestamp = -1 }
    fun update(frame: ArHandFrame, tool: ArHandTool): ArGestureObservation {
        if (frame.timestampMillis <= lastTimestamp) return ArGestureObservation(stable, null, 0)
        if (lastTimestamp >= 0 && frame.timestampMillis-lastTimestamp > 350) reset()
        lastTimestamp = frame.timestampMillis
        val hands = frame.hands.filter { it.confidence >= .65f && it.landmarks.size == 21 }
        val hand = hands.firstOrNull()
        if (hand == null) { val released = pinch; reset(); return ArGestureObservation(if (released) ArRecognizedGesture.PINCH_RELEASE else ArRecognizedGesture.NONE, null, 0) }
        fun distance(a: ArVector2, b: ArVector2) = hypot(a.x-b.x, a.y-b.y)
        fun pinched(h: ArTrackedHand): Boolean {
            val p = h.landmarks; val palm = distance(p[0], p[9])
            return palm > .015f && distance(p[4], p[8])/palm < if (pinch) .50f else .32f
        }
        val grabbing = pinched(hand)
        val p = hand.landmarks
        fun extended(tip: Int, pip: Int, mcp: Int) = distance(p[tip], p[0]) > distance(p[pip], p[0])*1.12f && distance(p[tip], p[mcp]) > distance(p[pip], p[mcp])*1.15f
        val fingers = listOf(extended(8,6,5), extended(12,10,9), extended(16,14,13), extended(20,18,17))
        val raw = when {
            grabbing && !pinch -> ArRecognizedGesture.PINCH_START
            !grabbing && pinch -> ArRecognizedGesture.PINCH_RELEASE
            grabbing && hands.size >= 2 && pinched(hands[1]) -> if (tool == ArHandTool.Rotate) ArRecognizedGesture.TWO_HAND_ROTATE else ArRecognizedGesture.TWO_HAND_SCALE
            grabbing -> ArRecognizedGesture.PINCH_HOLD
            fingers.all { it } -> ArRecognizedGesture.OPEN_PALM
            fingers.none { it } -> ArRecognizedGesture.FIST
            fingers[0] && fingers.drop(1).none { it } -> ArRecognizedGesture.POINT
            else -> ArRecognizedGesture.NONE
        }
        pinch = grabbing
        if (candidate == raw) count++ else { candidate = raw; count = 1 }
        if (raw in setOf(ArRecognizedGesture.PINCH_START, ArRecognizedGesture.PINCH_RELEASE, ArRecognizedGesture.PINCH_HOLD, ArRecognizedGesture.TWO_HAND_SCALE, ArRecognizedGesture.TWO_HAND_ROTATE) || count >= 3) stable = raw
        return ArGestureObservation(stable, p[8], hands.size)
    }
    companion object {
        fun palmNormal(hand: ArTrackedHand): ArVector3? {
            val p = hand.worldLandmarks
            if (p.size != 21) return null
            val a = p[5]-p[0]; val b = p[17]-p[0]
            val cross = ArVector3(a.y*b.z-a.z*b.y, a.z*b.x-a.x*b.z, a.x*b.y-a.y*b.x)
            return cross.takeIf { it.magnitude() > 1e-8 }?.let { it * (1.0 / it.magnitude()) }
        }
    }
}

/** Hover confirmation never changes the selected object. */
class ArStableHover(private val dwellMillis: Long = 180) {
    private var candidate: String? = null
    private var since = 0L
    var target: String? = null; private set
    fun update(id: String?, timestamp: Long): String? {
        if (id != candidate) { candidate = id; since = timestamp; target = null }
        if (id != null && timestamp-since >= dwellMillis) target = id
        return target
    }
    fun reset() { candidate = null; target = null; since = 0 }
}
