package com.indianservers.aiexplorer.solver.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
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
import com.indianservers.aiexplorer.input.HandwritingMathInput
import com.indianservers.aiexplorer.input.OnDeviceMathOcr
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.input.RecognitionReadiness
import com.indianservers.aiexplorer.solver.domain.input.SchoolMathInputRecognizer
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.presentation.components.SolverVisualisationPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverCataloguePanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverHintPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverMasteryPanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverPracticePanel
import com.indianservers.aiexplorer.solver.presentation.components.SolverTutorPanel
import kotlinx.coroutines.delay

@Composable
fun SolverScreen(
    onExit: () -> Unit,
    wide: Boolean,
    model: SolverViewModel = viewModel(),
    onOpenGraph: (String) -> Unit = {},
    onOpenMatrices: () -> Unit = {},
    onOpenStatistics: () -> Unit = {},
    onOpenGeometry: () -> Unit = {},
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
    var captureVisible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.visualisationPlaying, state.selectedVisualisationIndex, state.solution?.visualisations?.size) {
        if (state.visualisationPlaying && !state.reducedMotion) {
            delay(1200)
            model.nextVisualisation(stopAtEnd = true)
        }
    }
    LaunchedEffect(state.solution) {
        if (state.solution != null) {
            delay(200)
            listState.scrollToItem(1)
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (wide) 920.dp else 620.dp)
                .fillMaxSize()
                .padding(top = workspaceTop, bottom = workspaceBottom, start = 8.dp, end = 8.dp)
                .testTag("solver.workspace")
                .semantics { contentDescription = "Offline Solver with editor-first input and direct answers" },
            state = listState,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SolverWorkspaceHeader(
                        historyCount = state.history.size,
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        onExit = onExit,
                        onHistory = model::toggleHistory,
                        onCatalogue = model::toggleCatalogue,
                        onMastery = model::toggleMastery,
                        onUndo = model::undo,
                        onRedo = model::redo,
                        onClearAll = model::requestClearAll,
                    )
                    SolverModeBar(
                        operation = state.operation,
                        profile = state.explanationProfile,
                        onOperation = model::selectOperation,
                        onProfile = model::setExplanationProfile,
                    )
                    IntentAwareMathValueField(
                        value = state.input,
                        onValueChange = model::updateInput,
                        label = "Solver expression",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Example: 3x + 5 = 20",
                        minLines = 2,
                        showLegend = false,
                        keyboardContext = MathKeyboardContext.GENERAL,
                        useMathKeyboard = true,
                        editorTestTag = "solver.input",
                        onDone = { model.run(SolverOperation.Solve) },
                    )
                    remember(state.input.text) { SchoolMathInputRecognizer.recognize(state.input.text) }?.let { recognition ->
                        SolverRecognitionPreview(
                            recognition = recognition,
                            onApply = { canonical ->
                                model.updateInput(androidx.compose.ui.text.input.TextFieldValue(canonical, androidx.compose.ui.text.TextRange(canonical.length)))
                            },
                        )
                    }
                    SolverInputBuilderPanel(onTemplate = model::updateInput)
                    GlowButton(
                        if (state.isSolving) "Working…" else "${state.operation.name} step by step  →",
                        enabled = state.input.text.isNotBlank() && !state.isSolving,
                        modifier = Modifier.fillMaxWidth().testTag("solver.solve"),
                    ) { model.run() }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Hint first", enabled = state.input.text.isNotBlank() && !state.isSolving, onClick = model::runHintOnly)
                        GlowButton("Calculators", onClick = model::toggleCatalogue)
                        GlowButton(if (captureVisible) "Hide capture" else "Scan or write") { captureVisible = !captureVisible }
                        if (state.isSolving) GlowButton("Cancel", onClick = model::cancelSolve)
                        if (state.input.text.isNotBlank()) GlowButton("Clear", modifier = Modifier.testTag("solver.clear_input"), onClick = model::clearInput)
                    }
                    if (captureVisible) {
                        SolverCapturePanel(
                            existing = state.input.text,
                            onConfirm = { confirmed ->
                                model.updateInput(androidx.compose.ui.text.input.TextFieldValue(confirmed, androidx.compose.ui.text.TextRange(confirmed.length)))
                                captureVisible = false
                            },
                        )
                    }
                    state.solveStatus?.let { status ->
                        Text(
                            status,
                            color = if (state.isSolving) Cyan else Muted,
                            fontSize = SolverReleasePolicy.minimumSupportingTextSp.sp,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                stateDescription = status
                            },
                        )
                    }
                }
            }
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
            if (state.historyVisible) {
                item { SolverHistoryPanel(state, model) }
            }
            if (state.masteryVisible) {
                item { SolverMasteryPanel(state.learningSummary, model::clearLearningData, model::toggleMastery) }
            }
            if (state.solution == null && state.input.text.isBlank()) {
                item { EmptySolverState(onExample = { model.updateInput(it); model.run(SolverOperation.Solve) }) }
            }
            state.solution?.let { solution ->
                if (state.hintOnlyMode) {
                    item {
                        SolverHintPanel(
                            hint = state.hints.getOrNull(state.visibleHintIndex),
                            hintNumber = state.visibleHintIndex,
                            totalHints = state.hints.size,
                            onAnotherHint = model::requestNextHint,
                            onRevealStep = model::revealNextTutorStepHint,
                            onReturnToSolving = model::returnToIndependentSolving,
                            onShowFullSolution = model::showFullSolution,
                        )
                    }
                } else {
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
                            onOpenGraph = { onOpenGraph(solution.input.original) },
                            onOpenMatrices = onOpenMatrices,
                            onOpenStatistics = onOpenStatistics,
                            onOpenGeometry = onOpenGeometry,
                        )
                    }
                }
                item {
                    SolverLearningActions(
                        hintOnly = state.hintOnlyMode,
                        tutorVisible = state.tutorVisible,
                        practiceVisible = state.practiceVisible,
                        onHint = model::runHintOnly,
                        onTutor = model::toggleTutor,
                        onPractice = model::togglePractice,
                        onMastery = model::toggleMastery,
                    )
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
        if (state.clearAllConfirmationVisible) {
            AlertDialog(
                onDismissRequest = model::cancelClearAll,
                title = { Text("Clear the entire Solver session?") },
                text = { Text("Inputs, results, steps, assumptions, and Solver history will be cleared. You can Undo this action afterward.") },
                confirmButton = {
                    TextButton(
                        onClick = model::confirmClearAll,
                        modifier = Modifier.semantics { contentDescription = "Confirm Clear All" },
                    ) { Text("Clear All") }
                },
                dismissButton = { TextButton(onClick = model::cancelClearAll) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun SolverRecognitionPreview(
    recognition: com.indianservers.aiexplorer.solver.domain.input.SchoolMathRecognition,
    onApply: (String) -> Unit,
) {
    val accent = when (recognition.readiness) {
        RecognitionReadiness.Ready -> Green
        RecognitionReadiness.NeedsDetail -> Amber
        RecognitionReadiness.Unsupported -> Color(0xFFFF6688)
    }
    Column(
        Modifier.fillMaxWidth().background(accent.copy(alpha = .07f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = .38f), RoundedCornerShape(12.dp)).padding(10.dp)
            .semantics {
                stateDescription = "${recognition.topic}, ${recognition.readiness.name}, ${recognition.confidencePercent} percent confidence"
            },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(recognition.topic, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${recognition.confidencePercent}% recognized · ${recognition.readiness.name.replace("NeedsDetail", "Needs detail")}", color = Muted, fontSize = 11.sp)
            }
            recognition.canonicalInput?.let { canonical -> GlowButton("Use math form") { onApply(canonical) } }
        }
        recognition.canonicalInput?.let { Text(it, color = Cyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
        Text(recognition.guidance, color = Ink, fontSize = 11.sp)
        recognition.assumptions.forEach { Text("Assumption: $it", color = Amber, fontSize = 11.sp) }
        if (recognition.choices.isNotEmpty()) {
            Text("Choose the intended meaning", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            recognition.choices.forEach { choice ->
                Column(Modifier.fillMaxWidth().background(SurfaceB.copy(alpha = .6f), RoundedCornerShape(9.dp)).padding(8.dp)) {
                    GlowButton(choice.label) { onApply(choice.canonicalInput) }
                    Text(choice.canonicalInput, color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text(choice.explanation, color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

private enum class SolverCaptureMode(val label: String) { Scan("Camera / image"), Handwrite("Handwrite") }

@Composable
private fun SolverCapturePanel(existing: String, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    val ocr = remember { OnDeviceMathOcr() }
    DisposableEffect(ocr) { onDispose { ocr.close() } }
    var mode by remember { mutableStateOf(SolverCaptureMode.Scan) }
    var draft by remember(existing) { mutableStateOf(existing) }
    var status by remember { mutableStateOf("Capture remains on this device. Recognition must be reviewed before insertion.") }

    fun acceptOcr(result: com.indianservers.aiexplorer.input.MathOcrResult) {
        draft = result.normalized.editableText
        val confidence = result.confidence?.let { "${(it * 100).toInt()}%" } ?: "confidence unavailable"
        val repairs = result.normalized.changes.take(3).joinToString().ifBlank { "no automatic notation repairs" }
        status = "Recognized ${result.lineCount} line(s), $confidence. $repairs. Review every symbol."
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            status = "Recognizing locally…"
            ocr.recognize(context, selected, ::acceptOcr) { status = "Recognition failed: ${it.message ?: "unreadable image"}. Type or handwrite the problem instead." }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) status = "Camera capture cancelled."
        else {
            status = "Recognizing locally…"
            ocr.recognize(bitmap, ::acceptOcr) { status = "Recognition failed: ${it.message ?: "unreadable image"}. Retake with stronger contrast." }
        }
    }
    var launchCameraAfterPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && launchCameraAfterPermission) camera.launch(null) else if (!granted) status = "Camera permission was not granted. You can choose an image or type instead."
        launchCameraAfterPermission = false
    }

    SolverPanel(accent = Violet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CAPTURE A PROBLEM", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Recognize → review → confirm", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("LOCAL", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SolverCaptureMode.entries.forEach { item -> GlowButton(if (item == mode) "● ${item.label}" else item.label) { mode = item } }
        }
        if (mode == SolverCaptureMode.Scan) {
            Text("Use a tight crop, even lighting and one problem at a time. Printed maths works best.", color = Muted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Open camera") {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null)
                    else { launchCameraAfterPermission = true; permission.launch(Manifest.permission.CAMERA) }
                }
                GlowButton("Choose image") { gallery.launch("image/*") }
            }
        } else {
            HandwritingMathInput(onInsert = { token -> draft += token })
        }
        Text(status, color = if (status.startsWith("Recognition failed")) Amber else Muted, fontSize = 11.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Confirmed mathematical transcription") },
            supportingText = { Text("Edit OCR mistakes, preserve line breaks, and confirm only when the transcription matches the question.") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Editable locally recognized mathematical transcription" },
        )
        remember(draft) { SchoolMathInputRecognizer.recognize(draft) }?.let { recognition ->
            Text("${recognition.topic} · ${recognition.readiness.name.replace("NeedsDetail", "Needs detail")} · ${recognition.guidance}", color = if (recognition.readiness == RecognitionReadiness.Ready) Green else Amber, fontSize = 11.sp)
        }
        GlowButton("Confirm and insert", enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { onConfirm(draft.trim()) }
    }
}

@Composable
private fun SolverInputBuilderPanel(onTemplate: (androidx.compose.ui.text.input.TextFieldValue) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SolverPanel(accent = Cyan) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("STRUCTURED INPUT", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Build an unambiguous problem", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            GlowButton(if (expanded) "Hide" else "Templates") { expanded = !expanded }
        }
        if (expanded) {
            val templates = listOf(
                "System" to "2x + y = 7; x - y = 2",
                "Matrix" to "matrixmultiply([[1,2],[3,4]],[[2,0],[1,2]])",
                "Definite integral" to "integrate x^2 from 0 to 3",
                "Derivative" to "differentiate x*sin(x)",
                "Sequence" to "arithmeticterm(3,4,10)",
                "Statistics" to "stats 2, 3, 5, 8",
                "Binomial" to "binomial pmf 3 n 10 p 0.5",
                "Regression" to "regression x: 1,2,3; y: 2,4,5",
                "Speed + units" to "speed distance 240 km time 3 h",
                "Angle units" to "30 degrees to radians",
                "Coordinates" to "distance(0,0,3,4)",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                templates.forEach { (label, value) ->
                    GlowButton(label) { onTemplate(androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))) }
                }
            }
            Text("Templates use verified local syntax; edit the numbers before solving.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SolverWorkspaceHeader(
    historyCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onExit: () -> Unit,
    onHistory: () -> Unit,
    onCatalogue: () -> Unit,
    onMastery: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
) {
    SolverPanel(accent = Violet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Σ  SOLVER", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text("Understand every step", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                Text("Exact when possible · verified offline · private by design", color = Muted, fontSize = 11.sp)
            }
            GlowButton("Back", onClick = onExit)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("History $historyCount", onClick = onHistory)
            GlowButton("Topics", onClick = onCatalogue)
            GlowButton("My learning", onClick = onMastery)
            GlowButton("Undo", enabled = canUndo, modifier = Modifier.testTag("solver.undo"), onClick = onUndo)
            GlowButton("Redo", enabled = canRedo, modifier = Modifier.testTag("solver.redo"), onClick = onRedo)
            GlowButton("Clear All", modifier = Modifier.testTag("solver.clear_all"), onClick = onClearAll)
        }
    }
}

@Composable
private fun SolverModeBar(
    operation: SolverOperation,
    profile: ExplanationProfile,
    onOperation: (SolverOperation) -> Unit,
    onProfile: (ExplanationProfile) -> Unit,
) {
    SolverPanel(accent = Cyan) {
        Text("WHAT SHOULD I DO?", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            SolverOperation.entries.forEach { item ->
                GlowButton(if (item == operation) "● ${item.name}" else item.name, modifier = Modifier.testTag("solver.mode.${item.name.lowercase()}")) { onOperation(item) }
            }
        }
        Text("EXPLANATION LEVEL", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ExplanationProfile.entries.forEach { item ->
                GlowButton(if (item == profile) "● ${item.label}" else item.label, modifier = Modifier.testTag("solver.profile.${item.name.lowercase()}")) { onProfile(item) }
            }
        }
    }
}

@Composable
private fun SolverLearningActions(
    hintOnly: Boolean,
    tutorVisible: Boolean,
    practiceVisible: Boolean,
    onHint: () -> Unit,
    onTutor: () -> Unit,
    onPractice: () -> Unit,
    onMastery: () -> Unit,
) {
    SolverPanel(accent = Green) {
        Text("LEARN FROM THIS PROBLEM", color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton(if (hintOnly) "● Guided hints" else "Guided hints", onClick = onHint)
            GlowButton(if (tutorVisible) "● Try next step" else "Try next step", onClick = onTutor)
            GlowButton(if (practiceVisible) "● Similar practice" else "Similar practice", onClick = onPractice)
            GlowButton("Learning progress", onClick = onMastery)
        }
    }
}

@Composable
private fun SolverHistoryPanel(state: SolverUiState, model: SolverViewModel) {
    val filtered = state.history.filter { entry ->
        state.historyQuery.isBlank() || entry.originalInput.contains(state.historyQuery, true) || entry.problemType.name.contains(state.historyQuery, true)
    }
    SolverPanel(accent = Amber) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Recent solutions", color = Amber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Stored only on this device", color = Muted, fontSize = 11.sp)
            }
            GlowButton("Close", onClick = model::toggleHistory)
        }
        OutlinedTextField(
            value = state.historyQuery,
            onValueChange = model::setHistoryQuery,
            label = { Text("Search history") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search Solver history" },
        )
        if (filtered.isEmpty()) Text("No matching solved problems yet.", color = Muted, fontSize = 11.sp)
        filtered.take(12).forEach { entry ->
            Column(
                Modifier.fillMaxWidth().background(SurfaceB.copy(alpha = .62f), RoundedCornerShape(8.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.originalInput, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (entry.id in state.bookmarkedHistoryIds) "Saved" else "", color = Amber, fontSize = 11.sp)
                }
                Text("${entry.problemType.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")} · ${entry.stepCount} steps · ${entry.verificationStatus.name}", color = Muted, fontSize = 11.sp)
                Text("= ${entry.finalResult}", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton("Open") { model.reopen(entry) }
                    GlowButton(if (entry.id in state.bookmarkedHistoryIds) "Unsave" else "Save") { model.toggleBookmark(entry.id) }
                    GlowButton("Delete") { model.deleteHistory(entry.id) }
                }
            }
        }
        if (state.history.isNotEmpty()) GlowButton("Clear all history", onClick = model::clearHistory)
    }
}

@Composable
private fun EmptySolverState(onExample: (androidx.compose.ui.text.input.TextFieldValue) -> Unit) {
    SolverPanel {
        Text("Explore mathematics", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Choose a starting point or type any supported problem above.", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("x²  Algebra", "∫  Calculus", "sin  Trigonometry", "▦  Matrices", "▥  Statistics").forEach { topic ->
                Text(
                    topic,
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Cyan.copy(alpha = .08f), RoundedCornerShape(10.dp)).border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Text("Try an example", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

private enum class SolverResultTab(val label: String) {
    Overview("Answer"),
    Steps("Steps"),
    Visualize("Visualize"),
    Verify("Verify"),
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
    onOpenGraph: () -> Unit,
    onOpenMatrices: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenGeometry: () -> Unit,
) {
    var resultTab by remember(solution) { mutableStateOf(SolverResultTab.Overview) }
    SolverPanel(accent = if (solution.supported) Green else Amber) {
        solution.parseError?.let { error ->
            ParseErrorDisplay(solution.input.normalized, error.span.start, error.span.endExclusive, error.message)
        }
        if (!solution.supported) {
            Text(solution.message, color = Amber, fontSize = 13.sp, modifier = Modifier.testTag("solver.error").semantics { liveRegion = LiveRegionMode.Assertive })
            Text("No solution was invented. Edit the notation or choose a supported offline method.", color = Muted, fontSize = 11.sp, modifier = Modifier.testTag("solver.fail_closed"))
            return@SolverPanel
        }
        val displayedAnswer =
            if (showApproximate) solution.approximateAnswer ?: solution.exactAnswer ?: solution.finalAnswer
            else solution.exactAnswer ?: solution.finalAnswer
        Text("Answer", color = Green, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text(
            displayedAnswer ?: "No answer",
            color = Cyan,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("solver.answer").semantics {
                liveRegion = LiveRegionMode.Polite
                stateDescription = "Verified answer ${displayedAnswer ?: "not available"}"
            },
        )
        Text(
            when (solution.verificationStrength) {
                com.indianservers.aiexplorer.solver.domain.model.VerificationStrength.SymbolicallyVerified -> "Symbolically verified offline"
                com.indianservers.aiexplorer.solver.domain.model.VerificationStrength.NumericallyVerified -> "Independently checked offline"
                com.indianservers.aiexplorer.solver.domain.model.VerificationStrength.PartiallyVerified -> "Kernel validated · independent check pending"
                com.indianservers.aiexplorer.solver.domain.model.VerificationStrength.Inconclusive -> "Verification inconclusive"
                com.indianservers.aiexplorer.solver.domain.model.VerificationStrength.Failed -> "Verification failed"
            },
            color = if (solution.verification.status == VerificationStatus.Verified) Green else Amber,
            fontSize = 11.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Show steps", modifier = Modifier.testTag("solver.show_steps")) { resultTab = SolverResultTab.Steps }
            GlowButton("Copy answer", onClick = onCopyAnswer)
            if (solution.approximateAnswer != null) {
                GlowButton(if (showApproximate) "Show exact" else "Show decimal", onClick = onToggleAnswer)
            }
        }
        SolverWorkspaceHandoffs(solution.classification.type, onOpenGraph, onOpenMatrices, onOpenStatistics, onOpenGeometry)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().background(SurfaceB.copy(alpha = .55f), RoundedCornerShape(10.dp)).padding(5.dp),
        ) {
            SolverResultTab.entries.forEach { tab ->
                GlowButton(if (tab == resultTab) "● ${tab.label}" else tab.label, modifier = Modifier.testTag("solver.result_tab.${tab.name.lowercase()}")) { resultTab = tab }
            }
        }
        if (resultTab == SolverResultTab.Overview) {
            Text(
                "${solution.steps.size} explained steps · ${solution.classification.type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")}",
                color = Muted,
                fontSize = 11.sp,
            )
            return@SolverPanel
        }
        if (resultTab == SolverResultTab.Steps) {
        Text(
            solution.classification.type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2"),
            color = Green,
            fontWeight = FontWeight.Bold,
        )
        if (solution.methods.isNotEmpty()) {
            Text("Method", color = Muted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                solution.methods.filter(SolutionMethodOption::applicable).forEach { method ->
                    GlowButton(if (method.id == solution.selectedMethodId) "* ${method.label}" else method.label) {
                        onTryMethod(method.id)
                    }
                }
            }
            solution.methodReason?.let { Text(it, color = Muted, fontSize = 11.sp) }
        }
        if (solution.assumptions.isNotEmpty() || solution.restrictions.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().testTag("solver.warnings").background(Amber.copy(alpha = .07f), RoundedCornerShape(8.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("Assumptions and restrictions", color = Amber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                solution.assumptions.forEach { Text("- ${it.statement}: ${it.reason}", color = Ink, fontSize = 11.sp) }
                solution.restrictions.forEach { Text("- ${it.condition}: ${it.reason}", color = Ink, fontSize = 11.sp) }
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
                    .testTag("solver.step.${index + 1}")
                    .semantics {
                        selected = selectedStepId == step.id
                        stateDescription = if (selectedStepId == step.id) "Selected step ${index + 1} of ${solution.steps.size}" else "Step ${index + 1} of ${solution.steps.size}"
                        contentDescription = "${rule.name}. Before ${SolverExpressionRenderer.render(step.before)}. After ${SolverExpressionRenderer.render(step.after)}. ${step.explanation}. Activate to synchronize the visual explanation."
                    }
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Step ${index + 1} | ${rule.name}", color = Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(SolverExpressionRenderer.render(step.before), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("=> ${SolverExpressionRenderer.render(step.after)}", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(step.explanation, color = Ink, fontSize = 11.sp)
                step.optionalDetails.forEach { Text("${it.label}: ${it.value}", color = Muted, fontSize = 11.sp) }
                Text(if (step.reversible) "Equivalence preserved" else "Requires branch/domain check", color = Green, fontSize = 11.sp)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Copy full working", onClick = onCopyWorking)
            GlowButton("Visual explanation") { resultTab = SolverResultTab.Visualize }
        }
        }
        if (resultTab == SolverResultTab.Visualize) {
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
                    Text(formula.visualInterpretation, color = Ink, fontSize = 11.sp)
                    Text("Conditions", color = Amber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    formula.conditions.forEach { Text("- $it", color = Muted, fontSize = 11.sp) }
                    Text("Example: ${formula.example}", color = Ink, fontSize = 11.sp)
                    Text("Verification: ${formula.verification}", color = Green, fontSize = 11.sp)
                }
            }
        }
        if (solution.visualisations.isEmpty() && solution.formulaUnderstanding == null) {
            Text("This result has no separate visual model. Its verified transformation steps remain available in Steps.", color = Muted, fontSize = 11.sp)
        }
        }
        if (resultTab == SolverResultTab.Verify) {
        if (solution.commonMistakes.isNotEmpty()) {
            Text("Common possible mistakes", color = Amber, fontWeight = FontWeight.Bold)
            solution.commonMistakes.take(3).forEach { mistake ->
                Column(
                    Modifier.fillMaxWidth().background(Color(0x332D1420), RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(mistake.incorrectTransformation, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(mistake.whyInvalid, color = Ink, fontSize = 11.sp)
                    Text("Counterexample: ${mistake.counterExample}", color = Muted, fontSize = 11.sp)
                    Text("Correct: ${mistake.correctMethod}", color = Green, fontSize = 11.sp)
                }
            }
        }
        if (solution.ruleCitations.isNotEmpty()) {
            Text("Rules used: ${solution.ruleCitations.joinToString()}", color = Muted, fontSize = 11.sp)
        }
        solution.visualVerification?.let { visual ->
            Text(
                "Visual verification: ${visual.summary}",
                color = if (visual.consistent) Green else Muted,
                fontSize = 11.sp,
            )
        }
        VerificationCard(solution)
        }
    }
}

@Composable
private fun SolverWorkspaceHandoffs(
    type: ProblemType,
    onOpenGraph: () -> Unit,
    onOpenMatrices: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenGeometry: () -> Unit,
) {
    val actions = buildList<Pair<String, () -> Unit>> {
        if (type in setOf(ProblemType.FunctionAnalysis, ProblemType.Calculus, ProblemType.TrigonometricProblem, ProblemType.LinearEquation, ProblemType.QuadraticEquation)) add("Explore graph" to onOpenGraph)
        if (type == ProblemType.MatrixOperation) add("Open Matrix Lab" to onOpenMatrices)
        if (type in setOf(ProblemType.Probability, ProblemType.Statistics)) add("Open Probability & Statistics Lab" to onOpenStatistics)
        if (type == ProblemType.CoordinateGeometry) add("Open 2D Geometry" to onOpenGeometry)
    }
    if (actions.isNotEmpty()) {
        Text("Continue exploring", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            actions.forEach { (label, action) -> GlowButton(label, onClick = action) }
        }
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
        Text("Position ${safeStart + 1}", color = Muted, fontSize = 11.sp)
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
        Text("Verification | ${verification.status}", color = color, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("solver.verification"))
        Text(verification.message, color = Ink, fontSize = 11.sp)
        verification.checks.forEach { check ->
            Text("${if (check.passed) "PASS" else "FAIL"} | ${check.label}: expected ${check.expected}, got ${check.actual}", color = if (check.passed) Green else color, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SolverPanel(accent: Color = Cyan, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = accent.copy(alpha = .12f), spotColor = accent.copy(alpha = .16f))
            .background(Brush.linearGradient(listOf(SurfaceA.copy(alpha = .98f), SurfaceB.copy(alpha = .92f))), shape)
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = .58f), Violet.copy(alpha = .24f))), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
