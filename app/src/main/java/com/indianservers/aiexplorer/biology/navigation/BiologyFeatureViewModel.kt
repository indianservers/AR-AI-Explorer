package com.indianservers.aiexplorer.biology.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.biology.model.BiologyLearningLevel
import com.indianservers.aiexplorer.biology.model.BiologyNodeType
import com.indianservers.aiexplorer.biology.model.BiologySearchResult
import com.indianservers.aiexplorer.biology.repository.BiologyRepository
import com.indianservers.aiexplorer.biology.repository.OfflineBiologyRepository

data class BiologyFeatureUiState(
    val routeStack: List<BiologyRoute> = listOf(BiologyRoute.Home),
    val learningLevel: BiologyLearningLevel = BiologyLearningLevel.CLASS_10,
    val searchQuery: String = "",
    val searchResults: List<BiologySearchResult> = emptyList(),
    val bookmarks: Set<String> = emptySet(),
    val completedConcepts: Set<String> = emptySet(),
    val recentlyViewed: List<String> = emptyList(),
) { val route: BiologyRoute get() = routeStack.last() }

class BiologyFeatureViewModel(
    val repository: BiologyRepository = OfflineBiologyRepository(),
) : ViewModel() {
    var state by mutableStateOf(BiologyFeatureUiState())
        private set

    fun navigate(route: BiologyRoute) {
        val recent = if (route is BiologyRoute.Concept) (listOf(route.conceptId) + state.recentlyViewed.filterNot { it == route.conceptId }).take(12) else state.recentlyViewed
        state = state.copy(routeStack = state.routeStack + route, recentlyViewed = recent)
    }

    fun back(): Boolean {
        if (state.routeStack.size <= 1) return false
        state = state.copy(routeStack = state.routeStack.dropLast(1))
        return true
    }

    fun home() { state = state.copy(routeStack = listOf(BiologyRoute.Home)) }
    fun setLearningLevel(level: BiologyLearningLevel) { state = state.copy(learningLevel = level); search(state.searchQuery) }
    fun search(query: String) { state = state.copy(searchQuery = query, searchResults = repository.search(query, state.learningLevel)) }
    fun toggleBookmark(conceptId: String) { state = state.copy(bookmarks = if (conceptId in state.bookmarks) state.bookmarks - conceptId else state.bookmarks + conceptId) }
    fun toggleComplete(conceptId: String) { state = state.copy(completedConcepts = if (conceptId in state.completedConcepts) state.completedConcepts - conceptId else state.completedConcepts + conceptId) }

    fun openSearchResult(result: BiologySearchResult) {
        when (result.type) {
            BiologyNodeType.Domain -> navigate(BiologyRoute.Domain(result.id))
            BiologyNodeType.Unit -> navigate(BiologyRoute.Unit(result.id))
            BiologyNodeType.Chapter -> navigate(BiologyRoute.Chapter(result.id))
            BiologyNodeType.Topic -> navigate(BiologyRoute.Topic(result.id))
            BiologyNodeType.Concept -> navigate(BiologyRoute.Concept(result.id))
            BiologyNodeType.Glossary -> repository.getGlossaryTerms().firstOrNull { it.id == result.id }?.conceptId?.let { navigate(BiologyRoute.Concept(it)) }
        }
    }
}
