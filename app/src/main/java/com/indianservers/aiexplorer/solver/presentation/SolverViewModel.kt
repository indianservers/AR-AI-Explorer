package com.indianservers.aiexplorer.solver.presentation

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.solver.data.history.LocalSolverHistoryRepository
import com.indianservers.aiexplorer.solver.data.local.SolverPhase2Preferences
import com.indianservers.aiexplorer.solver.data.local.SolverLearningRepository
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorPreset
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryEntry
import com.indianservers.aiexplorer.solver.domain.practice.SolverPracticeGenerator
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepStatus
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHintEngine
import com.indianservers.aiexplorer.solver.domain.tutor.SolverStepEvaluationEngine
import java.util.UUID

class SolverViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val engine = Phase3SolverEngine()
    private val historyRepository = LocalSolverHistoryRepository(application)
    private val phase2Preferences = SolverPhase2Preferences(application)
    private val learningRepository = SolverLearningRepository(application)
    private val stepEvaluator = SolverStepEvaluationEngine()
    private val practiceGenerator = SolverPracticeGenerator(engine)
    private var interactionStartedAt = System.currentTimeMillis()

    var state by mutableStateOf(
        SolverUiState(
            input = restoredInput(),
            operation = savedStateHandle.get<String>(KEY_OPERATION)?.let { runCatching { SolverOperation.valueOf(it) }.getOrNull() }
                ?: SolverOperation.Solve,
            history = historyRepository.entries(),
            explanationProfile = phase2Preferences.explanationProfile(),
            bookmarkedHistoryIds = phase2Preferences.bookmarkedHistoryIds(),
            reducedMotion = animatorScale(application) == 0f,
            learningSummary = learningRepository.summary(),
        ),
    )
        private set

    fun updateInput(value: TextFieldValue) {
        state = state.copy(input = value, solution = null)
        savedStateHandle[KEY_INPUT] = value.text
        savedStateHandle[KEY_SELECTION_START] = value.selection.start
        savedStateHandle[KEY_SELECTION_END] = value.selection.end
    }

    fun selectOperation(operation: SolverOperation) {
        state = state.copy(operation = operation)
        savedStateHandle[KEY_OPERATION] = operation.name
    }

    fun run(operation: SolverOperation = state.operation) {
        selectOperation(operation)
        val solution = engine.solve(state.input.text, operation, state.explanationProfile)
        storeSolution(solution, hintOnly = false)
    }

    fun runHintOnly() {
        val solution = engine.solve(state.input.text, state.operation, state.explanationProfile)
        storeSolution(solution, hintOnly = solution.supported)
        if (solution.supported) requestNextHint()
    }

    fun tryMethod(methodId: String) {
        val solution = engine.solve(state.input.text, state.operation, state.explanationProfile, methodId)
        storeSolution(solution, hintOnly = false)
    }

    fun setExplanationProfile(profile: ExplanationProfile) {
        phase2Preferences.setExplanationProfile(profile)
        state = state.copy(
            explanationProfile = profile,
            solution = state.solution?.let { engine.solve(state.input.text, state.operation, profile, it.selectedMethodId) },
        )
    }

    fun setHistoryQuery(query: String) {
        state = state.copy(historyQuery = query)
    }

    fun toggleAnswerForm() {
        state = state.copy(showApproximateAnswer = !state.showApproximateAnswer)
    }

    fun toggleBookmark(id: String) {
        state = state.copy(bookmarkedHistoryIds = phase2Preferences.toggleBookmark(id))
    }

    fun selectStep(stepId: String) {
        val visualisations = state.solution?.visualisations.orEmpty()
        val index = visualisations.indexOfFirst { stepId in it.linkedStepIds }.takeIf { it >= 0 }
            ?: state.selectedVisualisationIndex
        state = state.copy(selectedStepId = stepId, selectedVisualisationIndex = index, visualisationPlaying = false)
    }

    fun previousVisualisation() {
        val count = state.solution?.visualisations?.size ?: 0
        if (count == 0) return
        state = state.copy(selectedVisualisationIndex = (state.selectedVisualisationIndex - 1 + count) % count, visualisationPlaying = false)
    }

    fun nextVisualisation(stopAtEnd: Boolean = false) {
        val count = state.solution?.visualisations?.size ?: 0
        if (count == 0) return
        val atEnd = state.selectedVisualisationIndex >= count - 1
        state = if (stopAtEnd && atEnd) {
            state.copy(visualisationPlaying = false)
        } else {
            state.copy(selectedVisualisationIndex = (state.selectedVisualisationIndex + 1) % count)
        }
    }

    fun toggleVisualisationPlayback() {
        if (state.reducedMotion) {
            nextVisualisation()
        } else {
            state = state.copy(visualisationPlaying = !state.visualisationPlaying)
        }
    }

    fun resetVisualisation() {
        state = state.copy(selectedVisualisationIndex = 0, selectedStepId = null, visualisationPlaying = false)
    }

    fun toggleVisualisationExpanded() {
        state = state.copy(visualisationExpanded = !state.visualisationExpanded, visualisationPlaying = false)
    }

    fun toggleFormulaUnderstanding() {
        state = state.copy(formulaUnderstandingExpanded = !state.formulaUnderstandingExpanded)
    }

    fun requestNextHint() {
        val solution = state.solution ?: return
        val hints = if (state.hints.isEmpty()) SolverHintEngine.hints(solution, state.tutorStepIndex, state.explanationProfile) else state.hints
        if (hints.isEmpty()) return
        val next = (state.visibleHintIndex + 1).coerceAtMost(hints.lastIndex)
        if (next != state.visibleHintIndex) {
            learningRepository.recordHint(skill(solution))
            state = state.copy(hints = hints, visibleHintIndex = next, learningSummary = learningRepository.summary())
        }
    }

    fun revealNextTutorStepHint() {
        val solution = state.solution ?: return
        val hints = SolverHintEngine.hints(solution, state.tutorStepIndex, state.explanationProfile)
        if (hints.isEmpty()) return
        learningRepository.recordHint(skill(solution))
        state = state.copy(hints = hints, visibleHintIndex = hints.lastIndex, learningSummary = learningRepository.summary())
    }

    fun returnToIndependentSolving() {
        state = state.copy(solution = null, hintOnlyMode = false, hints = emptyList(), visibleHintIndex = -1)
    }

    fun showFullSolution() {
        state = state.copy(hintOnlyMode = false)
    }

    fun toggleTutor() {
        state = state.copy(tutorVisible = !state.tutorVisible, tutorInput = TextFieldValue(""), tutorEvaluations = emptyList(), tutorStepIndex = 0)
    }

    fun updateTutorInput(value: TextFieldValue) {
        state = state.copy(tutorInput = value)
    }

    fun evaluateTutorStep() {
        val solution = state.solution ?: return
        val evaluation = stepEvaluator.evaluate(solution, state.tutorStepIndex, state.tutorInput.text)
        val accepted = evaluation.status in setOf(
            LearnerStepStatus.CorrectNextStep,
            LearnerStepStatus.CorrectLargeJump,
            LearnerStepStatus.CorrectAlternativeMethod,
        )
        if (!accepted) learningRepository.recordIncorrectStep(skill(solution), evaluation.misconception?.id)
        val nextStep = if (accepted) (state.tutorStepIndex + 1).coerceAtMost(solution.steps.size) else state.tutorStepIndex
        state = state.copy(
            tutorEvaluations = state.tutorEvaluations + evaluation,
            tutorStepIndex = nextStep,
            tutorInput = TextFieldValue(""),
            learningSummary = learningRepository.summary(),
        )
    }

    fun togglePractice() {
        val visible = !state.practiceVisible
        state = state.copy(practiceVisible = visible, practiceFeedback = null)
        if (visible) generatePractice(state.practiceMode)
    }

    fun generatePractice(mode: PracticeMode) {
        val solution = state.solution ?: return
        state = state.copy(
            practiceMode = mode,
            practiceProblem = practiceGenerator.generate(solution, mode, seed = System.nanoTime().toInt()),
            practiceInput = TextFieldValue(""),
            practiceFeedback = null,
        )
    }

    fun updatePracticeInput(value: TextFieldValue) {
        state = state.copy(practiceInput = value)
    }

    fun checkPractice() {
        val problem = state.practiceProblem ?: return
        val actual = state.practiceInput.text.replace(" ", "")
        val expected = problem.expectedAnswer.replace(" ", "")
        val correct = actual.equals(expected, true) ||
            engine.solve(state.practiceInput.text).let { it.supported && (it.exactAnswer ?: it.finalAnswer).orEmpty().replace(" ", "").equals(expected, true) }
        learningRepository.recordPractice(problem.skill, correct)
        state = state.copy(
            practiceFeedback = if (correct) "Correct. The answer matches the independently validated practice solution." else "Not yet. ${problem.hint}",
            learningSummary = learningRepository.summary(),
        )
    }

    fun toggleMastery() {
        state = state.copy(masteryVisible = !state.masteryVisible, learningSummary = learningRepository.summary())
    }

    fun clearLearningData() {
        learningRepository.clear()
        state = state.copy(learningSummary = learningRepository.summary())
    }

    fun toggleCatalogue() {
        state = state.copy(catalogueVisible = !state.catalogueVisible)
    }

    fun setCatalogueQuery(query: String) {
        state = state.copy(catalogueQuery = query)
    }

    fun chooseCalculator(preset: SolverCalculatorPreset) {
        updateInput(TextFieldValue(preset.starterExpression, TextRange(preset.starterExpression.length)))
        state = state.copy(catalogueVisible = false)
    }

    fun copyExpression() {
        copyToClipboard("Solver expression", state.input.text)
    }

    fun copyAnswer() {
        val answer = state.solution?.exactAnswer ?: state.solution?.finalAnswer ?: return
        copyToClipboard("Solver answer", answer)
    }

    fun copyFullWorking() {
        val solution = state.solution ?: return
        val text = buildString {
            appendLine(solution.input.original)
            appendLine("Method: ${solution.methods.firstOrNull { it.id == solution.selectedMethodId }?.label ?: "Exact method"}")
            solution.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.explanation}")
                appendLine("   ${SolverExpressionRenderer.render(step.after)}")
            }
            appendLine("Answer: ${solution.exactAnswer ?: solution.finalAnswer}")
            appendLine("Verification: ${solution.verification.message}")
        }
        copyToClipboard("Solver working", text)
    }

    private fun storeSolution(
        solution: com.indianservers.aiexplorer.solver.domain.model.SolverSolution,
        hintOnly: Boolean,
    ) {
        val elapsed = System.currentTimeMillis() - interactionStartedAt
        state = state.copy(
            solution = solution,
            selectedVisualisationIndex = 0,
            selectedStepId = solution.steps.firstOrNull()?.id,
            visualisationPlaying = false,
            visualisationExpanded = false,
            formulaUnderstandingExpanded = false,
            hintOnlyMode = hintOnly,
            hints = emptyList(),
            visibleHintIndex = -1,
            tutorVisible = false,
            tutorStepIndex = 0,
            tutorEvaluations = emptyList(),
            practiceVisible = false,
            practiceProblem = null,
            practiceFeedback = null,
        )
        if (solution.supported && solution.finalAnswer != null && solution.verification.status != VerificationStatus.Failed) {
            learningRepository.recordProblem(
                skill(solution),
                independentlySolved = !hintOnly,
                verified = solution.verification.status == VerificationStatus.Verified,
                method = solution.selectedMethodId,
                timeSpentMillis = elapsed,
            )
            historyRepository.save(
                SolverHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    originalInput = solution.input.original,
                    normalizedExpression = solution.input.normalized,
                    problemType = solution.classification.type,
                    finalResult = solution.finalAnswer,
                    timestamp = System.currentTimeMillis(),
                    stepCount = solution.steps.size,
                    verificationStatus = solution.verification.status,
                ),
            )
            refreshHistory()
            state = state.copy(learningSummary = learningRepository.summary())
        }
        interactionStartedAt = System.currentTimeMillis()
    }

    fun clearInput() {
        updateInput(TextFieldValue(""))
        state = state.copy(hintOnlyMode = false, hints = emptyList(), visibleHintIndex = -1)
    }

    fun toggleHistory() {
        state = state.copy(historyVisible = !state.historyVisible)
    }

    fun reopen(entry: SolverHistoryEntry) {
        updateInput(TextFieldValue(entry.originalInput, TextRange(entry.originalInput.length)))
        state = state.copy(historyVisible = false)
        run()
    }

    fun deleteHistory(id: String) {
        historyRepository.delete(id)
        refreshHistory()
    }

    fun clearHistory() {
        historyRepository.clear()
        refreshHistory()
    }

    private fun refreshHistory() {
        state = state.copy(history = historyRepository.entries())
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun skill(solution: com.indianservers.aiexplorer.solver.domain.model.SolverSolution) =
        solution.classification.type.name

    private fun restoredInput(): TextFieldValue {
        val text = savedStateHandle.get<String>(KEY_INPUT).orEmpty()
        val start = savedStateHandle.get<Int>(KEY_SELECTION_START)?.coerceIn(0, text.length) ?: text.length
        val end = savedStateHandle.get<Int>(KEY_SELECTION_END)?.coerceIn(0, text.length) ?: start
        return TextFieldValue(text, TextRange(start, end))
    }

    private companion object {
        const val KEY_INPUT = "solver.input"
        const val KEY_SELECTION_START = "solver.selection.start"
        const val KEY_SELECTION_END = "solver.selection.end"
        const val KEY_OPERATION = "solver.operation"

        fun animatorScale(context: Context): Float = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
}
