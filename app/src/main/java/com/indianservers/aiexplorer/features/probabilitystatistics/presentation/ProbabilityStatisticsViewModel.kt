package com.indianservers.aiexplorer.features.probabilitystatistics.presentation

import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.features.probabilitystatistics.data.ProbabilityStatisticsCatalog
import com.indianservers.aiexplorer.features.probabilitystatistics.models.MasteryState
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsLearningLevel
import com.indianservers.aiexplorer.features.probabilitystatistics.models.TopicProgress
import com.indianservers.aiexplorer.features.probabilitystatistics.models.TopicWorkspaceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ProbabilityStatisticsRoute { Home, Category, Topic, DistributionExplorer, DatasetLab, TestGuide, FormulaLibrary }

internal data class ProbabilityStatisticsUiState(
    val route: ProbabilityStatisticsRoute = ProbabilityStatisticsRoute.Home,
    val level: StatisticsLearningLevel = StatisticsLearningLevel.SeniorSecondary,
    val query: String = "",
    val categoryId: String? = null,
    val topicId: String? = null,
    val topicMode: TopicWorkspaceMode = TopicWorkspaceMode.Overview,
    val progress: Map<String, TopicProgress> = emptyMap(),
    val recentlyExplored: List<String> = emptyList(),
)

internal class ProbabilityStatisticsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProbabilityStatisticsUiState())
    val state: StateFlow<ProbabilityStatisticsUiState> = _state.asStateFlow()

    fun setLevel(level: StatisticsLearningLevel) {
        _state.value = _state.value.copy(level = level)
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query.take(80))
    }

    fun openCategory(id: String) {
        _state.value = _state.value.copy(route = ProbabilityStatisticsRoute.Category, categoryId = id, query = "")
    }

    fun openTopic(id: String, mode: TopicWorkspaceMode = TopicWorkspaceMode.Overview) {
        if (ProbabilityStatisticsCatalog.topic(id) == null) return
        val current = _state.value
        val progress = current.progress[id] ?: TopicProgress(id)
        _state.value = current.copy(
            route = ProbabilityStatisticsRoute.Topic,
            topicId = id,
            topicMode = mode,
            progress = current.progress + (id to progress.copy(mastery = if (progress.mastery == MasteryState.NotStarted) MasteryState.Exploring else progress.mastery)),
            recentlyExplored = (listOf(id) + current.recentlyExplored.filterNot { it == id }).take(4),
        )
    }

    fun selectMode(mode: TopicWorkspaceMode) {
        val state = _state.value
        val id = state.topicId ?: return
        val previous = state.progress[id] ?: TopicProgress(id)
        val completed = previous.completedModes + state.topicMode
        val mastery = when {
            completed.size >= 8 -> MasteryState.Mastered
            completed.size >= 6 -> MasteryState.Proficient
            completed.size >= 3 -> MasteryState.Practising
            completed.isNotEmpty() -> MasteryState.Learning
            else -> MasteryState.Exploring
        }
        _state.value = state.copy(
            topicMode = mode,
            progress = state.progress + (id to previous.copy(mastery = mastery, completedModes = completed)),
        )
    }

    fun recordAnswer(correct: Boolean) {
        val state = _state.value
        val id = state.topicId ?: return
        val previous = state.progress[id] ?: TopicProgress(id)
        _state.value = state.copy(
            progress = state.progress + (
                id to previous.copy(
                    correctAnswers = previous.correctAnswers + if (correct) 1 else 0,
                    attempts = previous.attempts + 1,
                    mastery = if (correct && previous.attempts >= 2) MasteryState.Proficient else MasteryState.Practising,
                )
            ),
        )
    }

    fun open(route: ProbabilityStatisticsRoute) {
        _state.value = _state.value.copy(route = route, query = "")
    }

    fun back() {
        val state = _state.value
        _state.value = when (state.route) {
            ProbabilityStatisticsRoute.Topic -> state.copy(route = ProbabilityStatisticsRoute.Category)
            ProbabilityStatisticsRoute.Category,
            ProbabilityStatisticsRoute.DistributionExplorer,
            ProbabilityStatisticsRoute.DatasetLab,
            ProbabilityStatisticsRoute.TestGuide,
            ProbabilityStatisticsRoute.FormulaLibrary -> state.copy(route = ProbabilityStatisticsRoute.Home)
            ProbabilityStatisticsRoute.Home -> state
        }
    }

    fun overallProgress(): Int {
        val completed = _state.value.progress.values.sumOf { it.percent }
        return if (_state.value.progress.isEmpty()) 0 else completed / _state.value.progress.size
    }
}
