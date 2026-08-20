package com.indianservers.aiexplorer.solver.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryEntry
import com.indianservers.aiexplorer.solver.domain.analytics.SolverLearningSummary
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepEvaluation
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeProblem
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHint

data class SolverUiState(
    val input: TextFieldValue = TextFieldValue(""),
    val operation: SolverOperation = SolverOperation.Solve,
    val solution: SolverSolution? = null,
    val history: List<SolverHistoryEntry> = emptyList(),
    val historyVisible: Boolean = false,
    val historyQuery: String = "",
    val explanationProfile: ExplanationProfile = ExplanationProfile.SchoolExamination,
    val showApproximateAnswer: Boolean = false,
    val bookmarkedHistoryIds: Set<String> = emptySet(),
    val selectedVisualisationIndex: Int = 0,
    val visualisationPlaying: Boolean = false,
    val visualisationExpanded: Boolean = false,
    val selectedStepId: String? = null,
    val formulaUnderstandingExpanded: Boolean = false,
    val reducedMotion: Boolean = false,
    val hintOnlyMode: Boolean = false,
    val hints: List<SolverHint> = emptyList(),
    val visibleHintIndex: Int = -1,
    val tutorVisible: Boolean = false,
    val tutorInput: TextFieldValue = TextFieldValue(""),
    val tutorStepIndex: Int = 0,
    val tutorEvaluations: List<LearnerStepEvaluation> = emptyList(),
    val practiceVisible: Boolean = false,
    val practiceMode: PracticeMode = PracticeMode.SimilarDifficulty,
    val practiceProblem: PracticeProblem? = null,
    val practiceInput: TextFieldValue = TextFieldValue(""),
    val practiceFeedback: String? = null,
    val masteryVisible: Boolean = false,
    val learningSummary: SolverLearningSummary = SolverLearningSummary(emptyList()),
    val catalogueVisible: Boolean = false,
    val catalogueQuery: String = "",
    val isSolving: Boolean = false,
    val solveStatus: String? = null,
    val clearAllConfirmationVisible: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
) {
    fun withInput(text: String): SolverUiState = copy(input = TextFieldValue(text, TextRange(text.length)))
}
