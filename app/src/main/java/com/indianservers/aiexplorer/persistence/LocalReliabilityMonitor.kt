package com.indianservers.aiexplorer.persistence

import android.content.Context
import androidx.core.content.edit

data class LocalReliabilitySnapshot(val sessions: Long, val interruptedSessions: Long) {
    val cleanSessionRate: Double get() = if (sessions == 0L) 1.0 else (sessions - interruptedSessions).coerceAtLeast(0).toDouble() / sessions
}

/** Privacy-preserving on-device evidence for unexpected foreground session termination. */
class LocalReliabilityMonitor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("local_reliability_v1", Context.MODE_PRIVATE)

    @Synchronized fun beginSession(): LocalReliabilitySnapshot {
        val sessions = preferences.getLong("sessions", 0) + 1
        val interrupted = preferences.getLong("interrupted", 0) + if (preferences.getBoolean("session_open", false)) 1 else 0
        preferences.edit { putLong("sessions", sessions); putLong("interrupted", interrupted); putBoolean("session_open", true) }
        return LocalReliabilitySnapshot(sessions, interrupted)
    }

    @Synchronized fun endSession() { preferences.edit { putBoolean("session_open", false) } }

    fun snapshot() = LocalReliabilitySnapshot(preferences.getLong("sessions", 0), preferences.getLong("interrupted", 0))
}
