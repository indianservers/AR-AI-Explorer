package com.indianservers.aiexplorer.features.numbertheory.visualproofs.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofMode
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryWorkspaceSection
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.rendering.NumberTheoryProofCanvas
import kotlinx.coroutines.delay

@Composable
internal fun ProofWorkspace(ui: NumberTheoryVisualProofUiState, model: NumberTheoryVisualProofViewModel) {
    val state = ui.proof
    if (state !is NumberTheoryProofState.Ready) return
    LaunchedEffect(state.topic.id) {
        model.dispatch(NumberTheoryProofAction.SetReducedMotion(!android.animation.ValueAnimator.areAnimatorsEnabled()))
    }
    LaunchedEffect(state.playing, state.stepIndex) {
        if (state.playing && state.stepIndex < state.topic.steps.lastIndex) {
            delay(900)
            model.dispatch(NumberTheoryProofAction.Next)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ProofHeader(state.topic.title, "STEP ${state.stepIndex + 1}/${state.topic.steps.size}") { model.back() }
            ScrollStrip {
                ProofChip(if (state.topic.id in ui.saved) "Saved" else "Save", state.topic.id in ui.saved, ProofGreen) {
                    model.toggleSaved(state.topic.id)
                }
                ProofChip("Replay", false, ProofCyan) {
                    model.dispatch(NumberTheoryProofAction.ReplayStep)
                }
                ProofChip(if (state.reducedMotion) "Reduced motion" else "Motion on", state.reducedMotion, ProofAmber) {
                    model.dispatch(NumberTheoryProofAction.SetReducedMotion(!state.reducedMotion))
                }
                NumberTheoryProofMode.entries.forEach { mode ->
                    ProofChip(mode.label, state.mode == mode, ProofAmber) {
                        model.dispatch(NumberTheoryProofAction.SelectMode(mode))
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumberTheoryProofCanvas(state)
            Text(
                state.topic.steps[state.stepIndex].expression,
                color = ProofCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { contentDescription = state.topic.steps[state.stepIndex].spokenExpression },
            )
            Text(state.topic.steps[state.stepIndex].observation, color = ProofMuted, fontSize = 11.sp)
            ParameterControls(state, model::dispatch)
            ProofNavigation(state, model::dispatch)
            ScrollStrip {
                NumberTheoryWorkspaceSection.entries.forEach { section ->
                    ProofChip(section.label, state.section == section, ProofGreen) {
                        model.dispatch(NumberTheoryProofAction.SelectSection(section))
                    }
                }
            }
            WorkspaceSection(state, model::dispatch)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ParameterControls(state: NumberTheoryProofState.Ready, dispatch: (NumberTheoryProofAction) -> Unit) {
    state.topic.parameters.forEach { parameter ->
        val value = state.parameters.getValue(parameter.key)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("${parameter.label}: $value", color = ProofInk, fontSize = 10.sp, modifier = Modifier.width(94.dp))
            ProofChip("-", false, ProofCyan) { dispatch(NumberTheoryProofAction.UpdateParameter(parameter.key, value - 1)) }
            Slider(
                value = value.toFloat(),
                onValueChange = { dispatch(NumberTheoryProofAction.UpdateParameter(parameter.key, it.toInt())) },
                valueRange = parameter.minimum.toFloat()..parameter.maximum.toFloat(),
                steps = (parameter.maximum - parameter.minimum - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f).semantics { contentDescription = "${parameter.label}, value $value" },
            )
            ProofChip("+", false, ProofCyan) { dispatch(NumberTheoryProofAction.UpdateParameter(parameter.key, value + 1)) }
        }
    }
}

@Composable
private fun ProofNavigation(state: NumberTheoryProofState.Ready, dispatch: (NumberTheoryProofAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ProofAction("<", "Previous step", Modifier.weight(1f)) { dispatch(NumberTheoryProofAction.Previous) }
        ProofAction(if (state.playing) "Pause" else "Play", "Play proof", Modifier.weight(1f)) {
            dispatch(NumberTheoryProofAction.TogglePlaying)
        }
        ProofAction("Reset", "Reset proof", Modifier.weight(1f)) { dispatch(NumberTheoryProofAction.Reset) }
        ProofAction(">", "Next step", Modifier.weight(1f), ProofGreen) { dispatch(NumberTheoryProofAction.Next) }
    }
}

@Composable
private fun WorkspaceSection(state: NumberTheoryProofState.Ready, dispatch: (NumberTheoryProofAction) -> Unit) {
    val topic = state.topic
    Column(
        Modifier.fillMaxWidth().background(ProofPanel, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.section) {
            NumberTheoryWorkspaceSection.Discover -> {
                SectionTitle("Predict")
                Text(topic.discoveryQuestion, color = ProofInk, fontSize = 12.sp)
                Text("Change the values, then explain what remains unchanged.", color = ProofMuted, fontSize = 10.sp)
            }
            NumberTheoryWorkspaceSection.VisualProof -> {
                SectionTitle("What the picture proves")
                Text(state.evidence.accessibilityDescription, color = ProofInk, fontSize = 11.sp)
                state.evidence.labels["reasoningStatus"]?.let { status ->
                    Text(status, color = ProofAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                state.evidence.labels["counterexample"]?.let { counterexample ->
                    Text(counterexample, color = ProofCoral, fontSize = 10.sp)
                }
                Text(
                    if (state.evidence.holds) "Exact check passed" else "Adjust the construction",
                    color = if (state.evidence.holds) ProofGreen else ProofCoral,
                    fontSize = 10.sp,
                )
            }
            NumberTheoryWorkspaceSection.Steps -> topic.steps.forEachIndexed { index, step ->
                Text(
                    "${index + 1}. ${step.instruction}",
                    color = if (index == state.stepIndex) ProofCyan else ProofMuted,
                    fontSize = 10.sp,
                )
            }
            NumberTheoryWorkspaceSection.Formula -> {
                SectionTitle("Formula")
                if (state.formulaRevealed) {
                    Text(topic.statement, color = ProofCyan, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(state.evidence.labels["formula"].orEmpty(), color = ProofGreen, fontSize = 12.sp)
                } else {
                    ProofAction("Reveal formula", "Reveal formula") { dispatch(NumberTheoryProofAction.RevealFormula) }
                }
            }
            NumberTheoryWorkspaceSection.TryValues -> {
                SectionTitle("Exact values")
                state.evidence.values.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = ProofMuted, fontSize = 10.sp)
                        Text(value.toString(), color = ProofInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            NumberTheoryWorkspaceSection.PatternTable -> {
                SectionTitle("Pattern")
                Text(
                    state.evidence.sequence.joinToString("  |  ")
                        .ifBlank { "Change a parameter to inspect the pattern." },
                    color = ProofCyan,
                    fontSize = 11.sp,
                )
            }
            NumberTheoryWorkspaceSection.Why -> {
                SectionTitle("Why it works")
                topic.whyItWorks.forEachIndexed { index, line ->
                    Text("${index + 1}. $line", color = ProofInk, fontSize = 10.sp)
                }
            }
            NumberTheoryWorkspaceSection.Mistake -> {
                SectionTitle("Common mistake")
                Text(topic.commonMistake, color = ProofCoral, fontSize = 11.sp)
            }
            NumberTheoryWorkspaceSection.Practice, NumberTheoryWorkspaceSection.Challenge ->
                PracticeSection(state, dispatch)
        }
    }
}

@Composable
private fun PracticeSection(state: NumberTheoryProofState.Ready, dispatch: (NumberTheoryProofAction) -> Unit) {
    val practice = state.topic.practice
    var selected by remember(state.topic.id) { mutableIntStateOf(-1) }
    SectionTitle(
        if (state.section == NumberTheoryWorkspaceSection.Challenge) {
            "Challenge without hints"
        } else {
            "Check your understanding"
        },
    )
    Text(practice.prompt, color = ProofInk, fontSize = 11.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        practice.options.forEachIndexed { index, option ->
            ProofChip(option, selected == index, ProofAmber) {
                selected = index
                val answer = if (index == practice.answerIndex) state.evidence.labels["prediction"].orEmpty() else option
                dispatch(NumberTheoryProofAction.SubmitPrediction(answer))
            }
        }
    }
    if (selected >= 0) {
        val correct = selected == practice.answerIndex
        Text(
            if (correct) "Correct. ${practice.explanation}" else "Try again. ${practice.explanation}",
            color = if (correct) ProofGreen else ProofCoral,
            fontSize = 10.sp,
        )
    }
}
