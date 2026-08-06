package com.indianservers.aiexplorer.features.numbertheory.visualproofs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryVisualProofEngine
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.data.NumberTheoryVisualProofCatalog
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class NumberTheoryProofRoute { Home, Category, Workspace }

internal data class NumberTheoryVisualProofUiState(
    val route: NumberTheoryProofRoute = NumberTheoryProofRoute.Home,
    val level: NumberTheoryProofLevel = NumberTheoryProofLevel.School,
    val query: String = "",
    val category: NumberTheoryProofCategory? = null,
    val proof: NumberTheoryProofState? = null,
    val recent: List<String> = emptyList(),
    val completed: Set<String> = emptySet(),
    val saved: Set<String> = emptySet(),
)

internal class NumberTheoryVisualProofViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val engine = NumberTheoryVisualProofEngine()
    private val _state = MutableStateFlow(
        NumberTheoryVisualProofUiState(
            recent = savedStateHandle.get<ArrayList<String>>(RECENT_KEY).orEmpty(),
            completed = savedStateHandle.get<ArrayList<String>>(COMPLETED_KEY).orEmpty().toSet(),
            saved = savedStateHandle.get<ArrayList<String>>(SAVED_KEY).orEmpty().toSet(),
        ),
    )
    val state: StateFlow<NumberTheoryVisualProofUiState> = _state.asStateFlow()

    fun search(value: String) {
        _state.value = _state.value.copy(query = value.take(80))
    }

    fun setLevel(level: NumberTheoryProofLevel) {
        _state.value = _state.value.copy(level = level)
    }

    fun openCategory(category: NumberTheoryProofCategory) {
        _state.value = _state.value.copy(route = NumberTheoryProofRoute.Category, category = category, query = "")
    }

    fun openTopic(id: String) {
        val proof = engine.start(id)
        if (proof is NumberTheoryProofState.Error) return
        _state.value = _state.value.copy(
            route = NumberTheoryProofRoute.Workspace,
            proof = proof,
            recent = (listOf(id) + _state.value.recent.filterNot { it == id }).take(5),
        )
        persistProgress()
    }

    fun dispatch(action: NumberTheoryProofAction) {
        val proof = _state.value.proof ?: return
        val updated = engine.reduce(proof, action)
        val completed = if (updated is NumberTheoryProofState.Ready && updated.completed) {
            _state.value.completed + updated.topic.id
        } else {
            _state.value.completed
        }
        _state.value = _state.value.copy(proof = updated, completed = completed)
        persistProgress()
    }

    fun toggleSaved(id: String) {
        val saved = if (id in _state.value.saved) _state.value.saved - id else _state.value.saved + id
        _state.value = _state.value.copy(saved = saved)
        persistProgress()
    }

    fun surprise() {
        val available = NumberTheoryVisualProofCatalog.completedTopics.filter { it.level.ordinal <= _state.value.level.ordinal }
        val topic = available[(_state.value.recent.size * 7 + _state.value.completed.size * 3).mod(available.size)]
        openTopic(topic.id)
    }

    fun back() {
        _state.value = when (_state.value.route) {
            NumberTheoryProofRoute.Workspace -> _state.value.copy(route = NumberTheoryProofRoute.Category, proof = null)
            NumberTheoryProofRoute.Category -> _state.value.copy(route = NumberTheoryProofRoute.Home, category = null)
            NumberTheoryProofRoute.Home -> _state.value
        }
    }

    private fun persistProgress() {
        savedStateHandle[RECENT_KEY] = ArrayList(_state.value.recent)
        savedStateHandle[COMPLETED_KEY] = ArrayList(_state.value.completed)
        savedStateHandle[SAVED_KEY] = ArrayList(_state.value.saved)
    }

    private companion object {
        const val RECENT_KEY = "numberTheoryProofs.recent"
        const val COMPLETED_KEY = "numberTheoryProofs.completed"
        const val SAVED_KEY = "numberTheoryProofs.saved"
    }
}
