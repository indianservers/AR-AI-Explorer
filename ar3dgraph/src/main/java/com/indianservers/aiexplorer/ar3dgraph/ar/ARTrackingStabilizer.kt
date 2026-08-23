package com.indianservers.aiexplorer.ar3dgraph.ar

/** Emits the first state immediately and subsequent changes only after they remain stable. */
class ARTrackingStabilizer(private val stabilityMillis: Long = 400L) {
    private var emitted: ARTrackingState? = null
    private var candidate: ARTrackingState? = null
    private var candidateSince = 0L

    fun update(state: ARTrackingState, nowMillis: Long): ARTrackingState? {
        if (emitted == null) {
            emitted = state
            return state
        }
        if (state == emitted) {
            candidate = null
            return null
        }
        if (state != candidate) {
            candidate = state
            candidateSince = nowMillis
            return null
        }
        if (nowMillis - candidateSince < stabilityMillis) return null
        emitted = state
        candidate = null
        return state
    }

    fun reset() {
        emitted = null
        candidate = null
        candidateSince = 0L
    }
}
