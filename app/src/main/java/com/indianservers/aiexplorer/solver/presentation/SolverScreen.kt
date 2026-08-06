package com.indianservers.aiexplorer.solver.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.Amber
import com.indianservers.aiexplorer.Cyan
import com.indianservers.aiexplorer.GlowButton
import com.indianservers.aiexplorer.Green
import com.indianservers.aiexplorer.Ink
import com.indianservers.aiexplorer.Muted
import com.indianservers.aiexplorer.SurfaceA
import com.indianservers.aiexplorer.SurfaceB
import com.indianservers.aiexplorer.Violet
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryEntry
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.presentation.components.SolverVisualisationPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverCataloguePanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverHintPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverMasteryPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverPracticePanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverTutorPanel
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun SolverScreen(
    onExit: () -> Unit,
    wide: Boolean,
    model: SolverViewModel = viewModel(),
) {
    BackHandler(onBack = onExit)
    val state = model.state
    LaunchedEffect(state.visualisationPlaying, state.selectedVisualisationIndex, state.solution?.visualisations?.size) {
        if (state.visualisationPlaying && !state.reducedMotion) {
            delay(1200)
            model.nextVisualisation(stopAtEnd = true)
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (wide) 920.dp else 620.dp)
                .fillMaxSize()
                .padding(top = 78.dp, bottom = 78.dp, start = 8.dp, end = 8.dp)
                .semantics { contentDescription = "Offline Solver screen with keyboard input, steps, verification, and local history" },
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                SolverHeader(
                    historyVisible = state.historyVisible,
                    onExit = onExit,
                    onClear = model::clearInput,
                    onHistory = model::toggleHistory,
                )
            }
            if (state.historyVisible) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Solver history", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (state.history.isNotEmpty()) GlowButton("Clear history", onClick = model::clearHistory)
                    }
                    OutlinedTextField(
                        value = state.historyQuery,
                        onValueChange = model::setHistoryQuery,
                        label = { Text("Search history") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search Solver history" },
                    )
                }
                val visibleHistory = state.history.filter {
                    state.historyQuery.isBlank() ||
                        it.originalInput.contains(state.historyQuery, true) ||
                        it.finalResult.contains(state.historyQuery, true) ||
                        it.problemType.name.contains(state.historyQuery, true)
                }.sortedByDescending { it.id in state.bookmarkedHistoryIds }
                if (visibleHistory.isEmpty()) {
                    item { SolverPanel { Text("No Solver history yet. Verified keyboard solutions appear here.", color = Muted) } }
                } else {
                    items(visibleHistory, key = SolverHistoryEntry::id) { entry ->
                        HistoryRow(
                            entry,
                            bookmarked = entry.id in state.bookmarkedHistoryIds,
                            onReopen = { model.reopen(entry) },
                            onBookmark = { model.toggleBookmark(entry.id) },
                            onDelete = { model.deleteHistory(entry.id) },
                        )
                    }
                }
            } else {
                if (state.catalogueVisible) {
                    item {
                        SolverCataloguePanel(
                            query = state.catalogueQuery,
                            onQuery = model::setCatalogueQuery,
                            onChoose = model::chooseCalculator,
                            onClose = model::toggleCatalogue,
                        )
                    }
                }
                if (state.masteryVisible) {
                    item {
                        SolverMasteryPanel(
                            summary = state.learningSummary,
                            onClear = model::clearLearningData,
                            onClose = model::toggleMastery,
                        )
                    }
                }
                item {
                    SolverPanel {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Keyboard input", color = Cyan, fontWeight = FontWeight.Bold)
                                Text("Fully offline | keyboard input only", color = Muted, fontSize = 9.sp)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlowButton("Calculators", onClick = model::toggleCatalogue)
                                GlowButton("Learning", onClick = model::toggleMastery)
                            }
                        }
                        IntentAwareMathValueField(
                            value = state.input,
                            onValueChange = model::updateInput,
                            label = "Solver expression",
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Editable Solver expression using the existing mathematical keyboard" },
                            placeholder = "Example: 3x + 5 = 20",
                            minLines = 2,
                            showLegend = false,
                            keyboardContext = MathKeyboardContext.GENERAL,
                            useMathKeyboard = true,
                            onDone = { model.run() },
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SolverOperation.entries.forEach { operation ->
                                GlowButton(
                                    if (state.operation == operation) "* ${operation.name}" else operation.name,
                                    enabled = state.input.text.isNotBlank(),
                                ) { model.run(operation) }
                            }
                            GlowButton("Hint only", enabled = state.input.text.isNotBlank(), onClick = model::runHintOnly)
                        }
                        Text("Explanation", color = Muted, fontSize = 10.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ExplanationProfile.entries.forEach { profile ->
                                GlowButton(if (state.explanationProfile == profile) "* ${profile.label}" else profile.label) {
                                    model.setExplanationProfile(profile)
                                }
                            }
                        }
                    }
                }
                if (state.solution == null && state.input.text.isBlank()) {
                    item { EmptySolverState(onExample = { model.updateInput(it); model.run() }) }
                }
                state.solution?.let { solution ->
                    item {
                        SolverResult(
                            solution = solution,
                            hideWorking = state.hintOnlyMode,
                            showApproximate = state.showApproximateAnswer,
                            onToggleAnswer = model::toggleAnswerForm,
                            onTryMethod = model::tryMethod,
                            onCopyExpression = model::copyExpression,
                            onCopyWorking = model::copyFullWorking,
                            selectedVisualisationIndex = state.selectedVisualisationIndex,
                            visualisationPlaying = state.visualisationPlaying,
                            visualisationExpanded = state.visualisationExpanded,
                            selectedStepId = state.selectedStepId,
                            reducedMotion = state.reducedMotion,
                            formulaExpanded = state.formulaUnderstandingExpanded,
                            onSelectStep = model::selectStep,
                            onPreviousVisualisation = model::previousVisualisation,
                            onNextVisualisation = { model.nextVisualisation() },
                            onPlayPause = model::toggleVisualisationPlayback,
                            onResetVisualisation = model::resetVisualisation,
                            onExpandVisualisation = model::toggleVisualisationExpanded,
                            onToggleFormula = model::toggleFormulaUnderstanding,
                        )
                    }
                    if (state.hintOnlyMode) {
                        item {
                            SolverHintPanel(
                                hint = state.hints.getOrNull(state.visibleHintIndex),
                                hintNumber = state.visibleHintIndex,
                                totalHints = state.hints.size.coerceAtLeast(6),
                                onAnotherHint = model::requestNextHint,
                                onRevealStep = model::revealNextTutorStepHint,
                                onReturnToSolving = model::returnToIndependentSolving,
                                onShowFullSolution = model::showFullSolution,
                            )
                        }
                    } else if (solution.supported) {
                        item {
                            SolverPanel(accent = Cyan) {
                                Text("Learn from this solution", color = Cyan, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    GlowButton("Give me only a hint", onClick = model::runHintOnly)
                                    GlowButton("Ask me the next step", onClick = model::toggleTutor)
                                    GlowButton("Practise similar", onClick = model::togglePractice)
                                }
                            }
                        }
                        if (state.tutorVisible) {
                            item {
                                SolverTutorPanel(
                                    input = state.tutorInput,
                                    stepIndex = state.tutorStepIndex,
                                    totalSteps = solution.steps.size,
                                    evaluations = state.tutorEvaluations,
                                    onInputChange = model::updateTutorInput,
                                    onCheck = model::evaluateTutorStep,
                                    onClose = model::toggleTutor,
                                )
                            }
                        }
                        if (state.practiceVisible) {
                            item {
                                SolverPracticePanel(
                                    problem = state.practiceProblem,
                                    mode = state.practiceMode,
                                    input = state.practiceInput,
                                    feedback = state.practiceFeedback,
                                    onMode = model::generatePractice,
                                    onInput = model::updatePracticeInput,
                                    onCheck = model::checkPractice,
                                    onNew = { model.generatePractice(state.practiceMode) },
                                    onClose = model::togglePractice,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolverHeader(
    historyVisible: Boolean,
    onExit: () -> Unit,
    onClear: () -> Unit,
    onHistory: () -> Unit,
) {
    SolverPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Solver", color = Cyan, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("OFFLINE | KEYBOARD ONLY", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                GlowButton("Back", icon = "back", iconOnly = true, onClick = onExit)
                GlowButton("Clear", onClick = onClear)
                GlowButton(if (historyVisible) "Close history" else "History", onClick = onHistory)
            }
        }
    }
}

@Composable
private fun EmptySolverState(onExample: (androidx.compose.ui.text.input.TextFieldValue) -> Unit) {
    SolverPanel {
        Text("Start with an example", color = Violet, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Every supported answer includes traceable steps and an independent check.", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("3x + 5 = 20", "2(x + 3) + 4x", "3/4 + 5/6", "2x + y = 7; x - y = 2", "-2x + 3 <= 9")
                .forEach { example ->
                    GlowButton(example) {
                        onExample(androidx.compose.ui.text.input.TextFieldValue(example, androidx.compose.ui.text.TextRange(example.length)))
                    }
                }
        }
    }
}

@Composable
private fun SolverResult(
    solution: SolverSolution,
    hideWorking: Boolean,
    showApproximate: Boolean,
    onToggleAnswer: () -> Unit,
    onTryMethod: (String) -> Unit,
    onCopyExpression: () -> Unit,
    onCopyWorking: () -> Unit,
    selectedVisualisationIndex: Int,
    visualisationPlaying: Boolean,
    visualisationExpanded: Boolean,
    selectedStepId: String?,
    reducedMotion: Boolean,
    formulaExpanded: Boolean,
    onSelectStep: (String) -> Unit,
    onPreviousVisualisation: () -> Unit,
    onNextVisualisation: () -> Unit,
    onPlayPause: () -> Unit,
    onResetVisualisation: () -> Unit,
    onExpandVisualisation: () -> Unit,
    onToggleFormula: () -> Unit,
) {
    SolverPanel(accent = if (solution.supported) Green else Amber) {
        Text(
            if (solution.supported) solution.classification.type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") else "Unsupported or incomplete",
            color = if (solution.supported) Green else Amber,
            fontWeight = FontWeight.Bold,
        )
        Text("${(solution.classification.confidence * 100).toInt()}% classification confidence", color = Muted, fontSize = 10.sp)
        solution.classification.evidence.forEach { Text("- $it", color = Muted, fontSize = 10.sp) }
        solution.parseError?.let { error ->
            ParseErrorDisplay(solution.input.normalized, error.span.start, error.span.endExclusive, error.message)
        }
        if (!solution.supported) {
            Text(solution.message, color = Amber, fontSize = 13.sp)
            Text("No solution was invented. Edit the notation or choose a supported offline method.", color = Muted, fontSize = 11.sp)
            return@SolverPanel
        }
        if (solution.methods.isNotEmpty()) {
            Text("Method", color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                solution.methods.filter(SolutionMethodOption::applicable).forEach { method ->
                    GlowButton(if (method.id == solution.selectedMethodId) "* ${method.label}" else method.label) {
                        onTryMethod(method.id)
                    }
                }
            }
            solution.methodReason?.let { Text(it, color = Muted, fontSize = 10.sp) }
        }
        if (solution.assumptions.isNotEmpty() || solution.restrictions.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().background(Amber.copy(alpha = .07f), RoundedCornerShape(8.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("Assumptions and restrictions", color = Amber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                solution.assumptions.forEach { Text("- ${it.statement}: ${it.reason}", color = Ink, fontSize = 10.sp) }
                solution.restrictions.forEach { Text("- ${it.condition}: ${it.reason}", color = Ink, fontSize = 10.sp) }
            }
        }
        if (hideWorking) {
            Text("Hint-only mode keeps the steps, verification and final answer hidden.", color = Green, fontSize = 11.sp)
            return@SolverPanel
        }
        solution.steps.forEachIndexed { index, step ->
            val rule = SolverRuleRegistry.get(step.ruleId)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceB.copy(alpha = .62f), RoundedCornerShape(8.dp))
                    .border(1.dp, (if (selectedStepId == step.id) Cyan else Violet).copy(alpha = .48f), RoundedCornerShape(8.dp))
                    .clickable { onSelectStep(step.id) }
                    .semantics { contentDescription = "Select step ${index + 1} to synchronize its mathematical visualisation" }
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Step ${index + 1} | ${rule.name}", color = Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(SolverExpressionRenderer.render(step.before), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("=> ${SolverExpressionRenderer.render(step.after)}", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(step.explanation, color = Ink, fontSize = 11.sp)
                step.optionalDetails.forEach { Text("${it.label}: ${it.value}", color = Muted, fontSize = 9.sp) }
                Text(if (step.reversible) "Equivalence preserved" else "Requires branch/domain check", color = Green, fontSize = 9.sp)
            }
        }
        solution.visualisations.getOrNull(selectedVisualisationIndex.coerceIn(0, (solution.visualisations.size - 1).coerceAtLeast(0)))?.let { specification ->
            Text("Visual explanation", color = Violet, fontWeight = FontWeight.Bold)
            SolverVisualisationPanel(
                specification = specification,
                position = selectedVisualisationIndex,
                count = solution.visualisations.size,
                playing = visualisationPlaying,
                reducedMotion = reducedMotion,
                expanded = visualisationExpanded,
                onPrevious = onPreviousVisualisation,
                onNext = onNextVisualisation,
                onPlayPause = onPlayPause,
                onReset = onResetVisualisation,
                onExpand = onExpandVisualisation,
            )
        }
        solution.formulaUnderstanding?.let { formula ->
            GlowButton(if (formulaExpanded) "Hide why this formula works" else "Why this formula works", onClick = onToggleFormula)
            if (formulaExpanded) {
                Column(
                    Modifier.fillMaxWidth().background(Violet.copy(alpha = .08f), RoundedCornerShape(8.dp)).padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(formula.title, color = Violet, fontWeight = FontWeight.Bold)
                    formula.symbolicDerivation.forEachIndexed { index, line ->
                        Text("${index + 1}. $line", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Text(formula.visualInterpretation, color = Ink, fontSize = 10.sp)
                    Text("Conditions", color = Amber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    formula.conditions.forEach { Text("- $it", color = Muted, fontSize = 9.sp) }
                    Text("Example: ${formula.example}", color = Ink, fontSize = 10.sp)
                    Text("Verification: ${formula.verification}", color = Green, fontSize = 9.sp)
                }
            }
        }
        (if (showApproximate) solution.approximateAnswer ?: solution.exactAnswer else solution.exactAnswer ?: solution.finalAnswer)?.let {
            Text("Final answer", color = Muted, fontSize = 10.sp)
            Text(it, color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (solution.approximateAnswer != null) {
                GlowButton(if (showApproximate) "Show exact" else "Show approximate", onClick = onToggleAnswer)
            }
            GlowButton("Copy expression", onClick = onCopyExpression)
            GlowButton("Copy full working", onClick = onCopyWorking)
        }
        if (solution.commonMistakes.isNotEmpty()) {
            Text("Common possible mistakes", color = Amber, fontWeight = FontWeight.Bold)
            solution.commonMistakes.take(3).forEach { mistake ->
                Column(
                    Modifier.fillMaxWidth().background(Color(0x332D1420), RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(mistake.incorrectTransformation, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(mistake.whyInvalid, color = Ink, fontSize = 10.sp)
                    Text("Counterexample: ${mistake.counterExample}", color = Muted, fontSize = 9.sp)
                    Text("Correct: ${mistake.correctMethod}", color = Green, fontSize = 9.sp)
                }
            }
        }
        if (solution.ruleCitations.isNotEmpty()) {
            Text("Rules used: ${solution.ruleCitations.joinToString()}", color = Muted, fontSize = 9.sp)
        }
        solution.visualVerification?.let { visual ->
            Text(
                "Visual verification: ${visual.summary}",
                color = if (visual.consistent) Green else Muted,
                fontSize = 10.sp,
            )
        }
        VerificationCard(solution)
    }
}

@Composable
private fun ParseErrorDisplay(input: String, start: Int, end: Int, message: String) {
    val safeStart = start.coerceIn(0, input.length)
    val width = (end - start).coerceAtLeast(1).coerceAtMost((input.length - safeStart).coerceAtLeast(1))
    Column(
        Modifier.fillMaxWidth().background(Color(0x553C1822), RoundedCornerShape(8.dp)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(message, color = Amber, fontWeight = FontWeight.Bold)
        Text(input, color = Ink, fontFamily = FontFamily.Monospace)
        Text(" ".repeat(safeStart) + "^".repeat(width), color = Amber, fontFamily = FontFamily.Monospace)
        Text("Position ${safeStart + 1}", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun VerificationCard(solution: SolverSolution) {
    val verification = solution.verification
    val color = when (verification.status) {
        VerificationStatus.Verified -> Green
        VerificationStatus.Failed -> Color(0xFFFF6688)
        VerificationStatus.Inconclusive -> Amber
        VerificationStatus.NotApplicable -> Muted
    }
    Column(
        Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(8.dp)).border(1.dp, color.copy(alpha = .45f), RoundedCornerShape(8.dp)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("Verification | ${verification.status}", color = color, fontWeight = FontWeight.Bold)
        Text(verification.message, color = Ink, fontSize = 11.sp)
        verification.checks.forEach { check ->
            Text("${if (check.passed) "PASS" else "FAIL"} | ${check.label}: expected ${check.expected}, got ${check.actual}", color = if (check.passed) Green else color, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HistoryRow(
    entry: SolverHistoryEntry,
    bookmarked: Boolean,
    onReopen: () -> Unit,
    onBookmark: () -> Unit,
    onDelete: () -> Unit,
) {
    SolverPanel(accent = Violet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).clickable(onClick = onReopen)) {
                Text(entry.originalInput, color = Cyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(entry.finalResult, color = Green, fontSize = 12.sp)
                Text(
                    "${entry.problemType} | ${entry.stepCount} steps | ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestamp))}",
                    color = Muted,
                    fontSize = 9.sp,
                )
            }
            GlowButton(if (bookmarked) "Unsave" else "Save", onClick = onBookmark)
            GlowButton("Delete", icon = "delete", iconOnly = true, onClick = onDelete)
        }
    }
}

@Composable
private fun SolverPanel(accent: Color = Cyan, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SurfaceA.copy(alpha = .94f), RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(10.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
