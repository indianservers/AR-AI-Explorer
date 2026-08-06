package com.indianservers.aiexplorer.solver.data.local

import android.content.Context
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile

class SolverPhase2Preferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun explanationProfile(): ExplanationProfile =
        preferences.getString(KEY_PROFILE, null)?.let { runCatching { ExplanationProfile.valueOf(it) }.getOrNull() }
            ?: ExplanationProfile.SchoolExamination

    fun setExplanationProfile(profile: ExplanationProfile) {
        preferences.edit().putString(KEY_PROFILE, profile.name).apply()
    }

    fun bookmarkedHistoryIds(): Set<String> = preferences.getStringSet(KEY_BOOKMARKS, emptySet()).orEmpty()

    fun toggleBookmark(id: String): Set<String> {
        val updated = bookmarkedHistoryIds().toMutableSet().apply { if (!add(id)) remove(id) }
        preferences.edit().putStringSet(KEY_BOOKMARKS, updated).apply()
        return updated
    }

    private companion object {
        const val PREFERENCES = "solver_phase_2_preferences"
        const val KEY_PROFILE = "explanation_profile"
        const val KEY_BOOKMARKS = "bookmarked_history_ids"
    }
}
