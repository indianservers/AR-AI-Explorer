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
import androidx.lifecycle.viewModelScope
import com.indianservers.aiexplorer.solver.data.history.LocalSolverHistoryRepository
import com.indianservers.aiexplorer.solver.data.local.SolverPhase2Preferences
import com.indianservers.aiexplorer.solver.data.local.SolverLearningRepository
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.input.SchoolMathInputRecognizer
import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorPreset
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryEntry
import com.indianservers.aiexplorer.solver.domain.practice.SolverPracticeGenerator
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepStatus
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHintEngine
import com.indianservers.aiexplorer.solver.domain.tutor.SolverStepEvaluationEngine
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var solveJob: Job? = null
    private val undoStack = ArrayDeque<SolverUiState>()
    private val redoStack = ArrayDeque<SolverUiState>()
    private val solutionCache = object : LinkedHashMap<String, SolverSolution>(SolverReleasePolicy.maximumCachedSolutions, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SolverSolution>?) = size > SolverReleasePolicy.maximumCachedSolutions
    }

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

    init {
        if (savedStateHandle.get<Boolean>(KEY_HAS_ACTIVE_SOLUTION) == true && state.input.text.isNotBlank()) {
            launchSolve(state.operation, hintOnly = false, requestedMethodId = savedStateHandle.get<String>(KEY_METHOD), recordInteraction = false)
        }
    }

    fun updateInput(value: TextFieldValue) {
        if (value != state.input) recordSnapshot()
        solveJob?.cancel()
        state = state.copy(input = value, solution = null, isSolving = false, solveStatus = null)
        savedStateHandle[KEY_INPUT] = value.text
        savedStateHandle[KEY_SELECTION_START] = value.selection.start
        savedStateHandle[KEY_SELECTION_END] = value.selection.end
        savedStateHandle[KEY_HAS_ACTIVE_SOLUTION] = false
    }

    fun selectOperation(operation: SolverOperation) {
        if (operation != state.operation) recordSnapshot()
        state = state.copy(operation = operation)
        savedStateHandle[KEY_OPERATION] = operation.name
    }

    fun run(operation: SolverOperation = state.operation) {
        recordSnapshot()
        if (operation != state.operation) {
            state = state.copy(operation = operation)
            savedStateHandle[KEY_OPERATION] = operation.name
        }
        launchSolve(operation, hintOnly = false)
    }

    fun runHintOnly() {
        launchSolve(state.operation, hintOnly = true)
    }

    fun tryMethod(methodId: String) {
        launchSolve(state.operation, hintOnly = false, requestedMethodId = methodId)
    }

    fun setExplanationProfile(profile: ExplanationProfile) {
        if (profile != state.explanationProfile) recordSnapshot()
        phase2Preferences.setExplanationProfile(profile)
        val method = state.solution?.selectedMethodId
        val shouldResolve = state.solution != null
        state = state.copy(explanationProfile = profile)
        if (shouldResolve) launchSolve(state.operation, hintOnly = state.hintOnlyMode, requestedMethodId = method, recordInteraction = false)
    }

    fun cancelSolve() {
        solveJob?.cancel()
        solveJob = null
        state = state.copy(isSolving = false, solveStatus = "Calculation cancelled. Your input is unchanged.")
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
        recordInteraction: Boolean = true,
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
            isSolving = false,
            solveStatus = if (solution.supported) "Completed with ${solution.verificationStrength.name.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()}." else "No verified strategy matched.",
        )
        savedStateHandle[KEY_HAS_ACTIVE_SOLUTION] = true
        savedStateHandle[KEY_METHOD] = solution.selectedMethodId
        if (recordInteraction && solution.supported && solution.finalAnswer != null && solution.verification.status != VerificationStatus.Failed) {
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

    private fun launchSolve(
        operation: SolverOperation,
        hintOnly: Boolean,
        requestedMethodId: String? = null,
        recordInteraction: Boolean = true,
    ) {
        val rawInput = state.input.text
        val input = SchoolMathInputRecognizer.canonicalize(rawInput)
        val profile = state.explanationProfile
        if (input.isBlank()) return
        if (input != rawInput) {
            state = state.copy(input = TextFieldValue(input, TextRange(input.length)))
            savedStateHandle[KEY_INPUT] = input
            savedStateHandle[KEY_SELECTION_START] = input.length
            savedStateHandle[KEY_SELECTION_END] = input.length
        }
        solveJob?.cancel()
        val cacheKey = listOf(input.trim(), operation.name, profile.name, requestedMethodId.orEmpty()).joinToString("\u0000")
        solutionCache[cacheKey]?.let { cached ->
            storeSolution(cached, hintOnly = hintOnly && cached.supported, recordInteraction = recordInteraction)
            if (hintOnly && cached.supported) requestNextHint()
            return
        }
        state = state.copy(isSolving = true, solveStatus = "Checking notation and selecting a verified strategy…")
        solveJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val warningJob = launch {
                delay(SolverReleasePolicy.solveWarningMillis)
                if (state.isSolving) state = state.copy(solveStatus = SolverReleasePolicy.longRunningMessage(System.currentTimeMillis() - startedAt))
                delay(SolverReleasePolicy.solveWarningMillis)
                if (state.isSolving) state = state.copy(solveStatus = SolverReleasePolicy.longRunningMessage(System.currentTimeMillis() - startedAt))
            }
            val solution = withContext(Dispatchers.Default) {
                engine.solve(input, operation, profile, requestedMethodId)
            }
            warningJob.cancel()
            solutionCache[cacheKey] = solution
            storeSolution(solution, hintOnly = hintOnly && solution.supported, recordInteraction = recordInteraction)
            if (hintOnly && solution.supported) requestNextHint()
            solveJob = null
        }
    }

    fun clearInput() {
        updateInput(TextFieldValue(""))
        state = state.copy(hintOnlyMode = false, hints = emptyList(), visibleHintIndex = -1)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        solveJob?.cancel()
        redoStack.addLast(snapshot(state))
        restoreSnapshot(undoStack.removeLast())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        solveJob?.cancel()
        undoStack.addLast(snapshot(state))
        restoreSnapshot(redoStack.removeLast())
    }

    fun requestClearAll() {
        state = state.copy(clearAllConfirmationVisible = true)
    }

    fun cancelClearAll() {
        state = state.copy(clearAllConfirmationVisible = false)
    }

    fun confirmClearAll() {
        recordSnapshot()
        solveJob?.cancel()
        historyRepository.clear()
        savedStateHandle[KEY_INPUT] = ""
        savedStateHandle[KEY_SELECTION_START] = 0
        savedStateHandle[KEY_SELECTION_END] = 0
        savedStateHandle[KEY_HAS_ACTIVE_SOLUTION] = false
        savedStateHandle[KEY_METHOD] = null
        state = SolverUiState(
            operation = state.operation,
            explanationProfile = state.explanationProfile,
            bookmarkedHistoryIds = state.bookmarkedHistoryIds,
            reducedMotion = state.reducedMotion,
            learningSummary = state.learningSummary,
            canUndo = undoStack.isNotEmpty(),
        )
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

    private fun recordSnapshot() {
        val candidate = snapshot(state)
        if (undoStack.lastOrNull() != candidate) {
            undoStack.addLast(candidate)
            while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        }
        redoStack.clear()
        state = state.copy(canUndo = undoStack.isNotEmpty(), canRedo = false)
    }

    private fun snapshot(value: SolverUiState) = value.copy(
        isSolving = false,
        solveStatus = null,
        clearAllConfirmationVisible = false,
        canUndo = false,
        canRedo = false,
    )

    private fun restoreSnapshot(value: SolverUiState) {
        historyRepository.clear()
        value.history.sortedBy(SolverHistoryEntry::timestamp).forEach(historyRepository::save)
        state = value.copy(
            history = historyRepository.entries(),
            isSolving = false,
            clearAllConfirmationVisible = false,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
        )
        savedStateHandle[KEY_INPUT] = state.input.text
        savedStateHandle[KEY_SELECTION_START] = state.input.selection.start
        savedStateHandle[KEY_SELECTION_END] = state.input.selection.end
        savedStateHandle[KEY_OPERATION] = state.operation.name
        savedStateHandle[KEY_HAS_ACTIVE_SOLUTION] = state.solution != null
        savedStateHandle[KEY_METHOD] = state.solution?.selectedMethodId
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
        const val KEY_HAS_ACTIVE_SOLUTION = "solver.active.solution"
        const val KEY_METHOD = "solver.active.method"
        const val MAX_UNDO = 100

        fun animatorScale(context: Context): Float = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
}
