package com.indianservers.aiexplorer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class FormulaLibraryState(
    val favorites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
    val collections: Map<String, Set<String>> = emptyMap(),
)

internal class FormulaLibraryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("formula_library", Context.MODE_PRIVATE)

    fun load(): FormulaLibraryState {
        val favorites = preferences.getStringSet(FAVORITES, emptySet()).orEmpty().toSet()
        val recent = runCatching {
            val array = JSONArray(preferences.getString(RECENT, "[]") ?: "[]")
            List(array.length()) { index -> array.getString(index) }
        }.getOrDefault(emptyList())
        val collections = runCatching {
            val json = JSONObject(preferences.getString(COLLECTIONS, "{}") ?: "{}")
            json.keys().asSequence().associateWith { name ->
                val array = json.getJSONArray(name)
                List(array.length()) { index -> array.getString(index) }.toSet()
            }
        }.getOrDefault(emptyMap())
        return FormulaLibraryState(favorites, recent, collections)
    }

    fun toggleFavorite(state: FormulaLibraryState, formulaId: String): FormulaLibraryState {
        val favorites = if (formulaId in state.favorites) state.favorites - formulaId else state.favorites + formulaId
        preferences.edit().putStringSet(FAVORITES, favorites).apply()
        return state.copy(favorites = favorites)
    }

    fun viewed(state: FormulaLibraryState, formulaId: String): FormulaLibraryState {
        val recent = (listOf(formulaId) + state.recent.filterNot { it == formulaId }).take(30)
        preferences.edit().putString(RECENT, JSONArray(recent).toString()).apply()
        return state.copy(recent = recent)
    }

    fun toggleCollection(state: FormulaLibraryState, collectionName: String, formulaId: String): FormulaLibraryState {
        val name = collectionName.trim().take(32)
        if (name.isBlank()) return state
        val current = state.collections[name].orEmpty()
        val updated = if (formulaId in current) current - formulaId else current + formulaId
        val collections = if (updated.isEmpty()) state.collections - name else state.collections + (name to updated)
        val json = JSONObject()
        collections.toSortedMap().forEach { (key, ids) -> json.put(key, JSONArray(ids.sorted())) }
        preferences.edit().putString(COLLECTIONS, json.toString()).apply()
        return state.copy(collections = collections)
    }

    companion object {
        private const val FAVORITES = "favorites"
        private const val RECENT = "recent"
        private const val COLLECTIONS = "collections"
    }
}
