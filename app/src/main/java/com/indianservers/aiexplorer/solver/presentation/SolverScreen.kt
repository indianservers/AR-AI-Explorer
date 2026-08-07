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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.indianservers.aiexplorer.adaptive.LocalAdaptiveDeviceProfile
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.presentation.components.SolverVisualisationPanel
import kotlinx.coroutines.delay

@Composable
fun SolverScreen(
    onExit: () -> Unit,
    wide: Boolean,
    model: SolverViewModel = viewModel(),
) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        78.dp
    }
    val workspaceBottom = if (adaptiveProfile.isTelevision) 8.dp else 78.dp
    BackHandler(onBack = onExit)
    val state = model.state
    val listState = rememberLazyListState()
    LaunchedEffect(state.visualisationPlaying, state.selectedVisualisationIndex, state.solution?.visualisations?.size) {
        if (state.visualisationPlaying && !state.reducedMotion) {
            delay(1200)
            model.nextVisualisation(stopAtEnd = true)
        }
    }
    LaunchedEffect(state.solution) {
        if (state.solution != null) {
            delay(80)
            listState.animateScrollToItem(1)
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (wide) 920.dp else 620.dp)
                .fillMaxSize()
                .padding(top = workspaceTop, bottom = workspaceBottom, start = 8.dp, end = 8.dp)
                .semantics { contentDescription = "Offline Solver with editor-first input and direct answers" },
            state = listState,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    IntentAwareMathValueField(
                        value = state.input,
                        onValueChange = model::updateInput,
                        label = "Solver expression",
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Editable Solver expression using the mathematical keyboard" },
                        placeholder = "Example: 3x + 5 = 20",
                        minLines = 2,
                        showLegend = false,
                        keyboardContext = MathKeyboardContext.GENERAL,
                        useMathKeyboard = true,
                        onDone = { model.run(SolverOperation.Solve) },
                    )
                    GlowButton(
                        "Solve",
                        enabled = state.input.text.isNotBlank(),
                    ) { model.run(SolverOperation.Solve) }
                }
            }
            if (state.solution == null && state.input.text.isBlank()) {
                item { EmptySolverState(onExample = { model.updateInput(it); model.run(SolverOperation.Solve) }) }
            }
            state.solution?.let { solution ->
                item {
                    SolverResult(
                            solution = solution,
                            showApproximate = state.showApproximateAnswer,
                            onToggleAnswer = model::toggleAnswerForm,
                            onTryMethod = model::tryMethod,
                            onCopyAnswer = model::copyAnswer,
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
    showApproximate: Boolean,
    onToggleAnswer: () -> Unit,
    onTryMethod: (String) -> Unit,
    onCopyAnswer: () -> Unit,
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
    var detailsExpanded by remember(solution) { mutableStateOf(false) }
    SolverPanel(accent = if (solution.supported) Green else Amber) {
        solution.parseError?.let { error ->
            ParseErrorDisplay(solution.input.normalized, error.span.start, error.span.endExclusive, error.message)
        }
        if (!solution.supported) {
            Text(solution.message, color = Amber, fontSize = 13.sp)
            Text("No solution was invented. Edit the notation or choose a supported offline method.", color = Muted, fontSize = 11.sp)
            return@SolverPanel
        }
        val displayedAnswer =
            if (showApproximate) solution.approximateAnswer ?: solution.exactAnswer ?: solution.finalAnswer
            else solution.exactAnswer ?: solution.finalAnswer
        Text("Answer", color = Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            displayedAnswer ?: "No answer",
            color = Cyan,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            if (solution.verification.status == VerificationStatus.Verified) "Verified offline" else solution.verification.status.name,
            color = if (solution.verification.status == VerificationStatus.Verified) Green else Amber,
            fontSize = 10.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton(if (detailsExpanded) "Hide steps" else "Show steps") {
                detailsExpanded = !detailsExpanded
            }
            GlowButton("Copy answer", onClick = onCopyAnswer)
            if (solution.approximateAnswer != null) {
                GlowButton(if (showApproximate) "Show exact" else "Show decimal", onClick = onToggleAnswer)
            }
        }
        if (!detailsExpanded) return@SolverPanel
        Text(
            solution.classification.type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2"),
            color = Green,
            fontWeight = FontWeight.Bold,
        )
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
