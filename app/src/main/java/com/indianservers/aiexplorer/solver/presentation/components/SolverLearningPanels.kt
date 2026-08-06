package com.indianservers.aiexplorer.solver.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.Amber
import com.indianservers.aiexplorer.Cyan
import com.indianservers.aiexplorer.GlowButton
import com.indianservers.aiexplorer.Green
import com.indianservers.aiexplorer.Ink
import com.indianservers.aiexplorer.Muted
import com.indianservers.aiexplorer.SurfaceA
import com.indianservers.aiexplorer.Violet
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.solver.domain.analytics.SolverLearningSummary
import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorCatalogue
import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorPreset
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepEvaluation
import com.indianservers.aiexplorer.solver.domain.tutor.LearnerStepStatus
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeProblem
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHint
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun SolverHintPanel(
    hint: SolverHint?,
    hintNumber: Int,
    totalHints: Int,
    onAnotherHint: () -> Unit,
    onRevealStep: () -> Unit,
    onReturnToSolving: () -> Unit,
    onShowFullSolution: () -> Unit,
) {
    LearningPanel(Green) {
        Text("Give me only a hint", color = Green, fontWeight = FontWeight.Bold)
        if (hint == null) {
            Text("Request a progressive hint without revealing the complete solution.", color = Muted, fontSize = 10.sp)
        } else {
            Text("Hint ${hintNumber + 1} of $totalHints | ${hint.level.name}", color = Muted, fontSize = 9.sp)
            Text(hint.text, color = Ink, fontSize = 12.sp)
            if (hint.revealsAnswer) Text("This hint reveals the verified next step, not the entire solution.", color = Amber, fontSize = 9.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Another hint", enabled = hintNumber < totalHints - 1, onClick = onAnotherHint)
            GlowButton("Reveal next step", onClick = onRevealStep)
            GlowButton("Solve independently", onClick = onReturnToSolving)
            GlowButton("Full solution", onClick = onShowFullSolution)
        }
    }
}

@Composable
fun SolverTutorPanel(
    input: TextFieldValue,
    stepIndex: Int,
    totalSteps: Int,
    evaluations: List<LearnerStepEvaluation>,
    onInputChange: (TextFieldValue) -> Unit,
    onCheck: () -> Unit,
    onClose: () -> Unit,
) {
    LearningPanel(Cyan) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Ask me the next step", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Step ${(stepIndex + 1).coerceAtMost(totalSteps)} of $totalSteps", color = Muted, fontSize = 9.sp)
            }
            GlowButton("Close", onClick = onClose)
        }
        if (stepIndex >= totalSteps) {
            Text("Tutor route complete. The final verified result has been reached.", color = Green, fontSize = 12.sp)
            val accepted = evaluations.count {
                it.status in setOf(
                    LearnerStepStatus.CorrectNextStep,
                    LearnerStepStatus.CorrectLargeJump,
                    LearnerStepStatus.CorrectAlternativeMethod,
                    LearnerStepStatus.EquivalentReformatting,
                )
            }
            val revisions = evaluations.size - accepted
            Text(
                "Session summary: $accepted accepted step${if (accepted == 1) "" else "s"} | " +
                    "$revisions revision${if (revisions == 1) "" else "s"}",
                color = Muted,
                fontSize = 10.sp,
            )
        } else {
            IntentAwareMathValueField(
                value = input,
                onValueChange = onInputChange,
                label = "Your proposed next line",
                placeholder = "Enter one mathematically justified step",
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Tutor next mathematical step input" },
                keyboardContext = MathKeyboardContext.GENERAL,
                useMathKeyboard = true,
                showLegend = false,
                onDone = onCheck,
            )
            GlowButton("Check this step", enabled = input.text.isNotBlank(), onClick = onCheck)
        }
        evaluations.lastOrNull()?.let { evaluation ->
            val positive = evaluation.status in setOf(
                LearnerStepStatus.CorrectNextStep,
                LearnerStepStatus.CorrectLargeJump,
                LearnerStepStatus.CorrectAlternativeMethod,
                LearnerStepStatus.EquivalentReformatting,
            )
            Column(
                Modifier.fillMaxWidth().background((if (positive) Green else Amber).copy(alpha = .08f), RoundedCornerShape(6.dp)).padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(evaluation.feedback.headline, color = if (positive) Green else Amber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(evaluation.feedback.explanation, color = Ink, fontSize = 10.sp)
                Text("Small clue: ${evaluation.feedback.smallestUsefulClue}", color = Muted, fontSize = 9.sp)
                evaluation.misconception?.let { Text("Pattern: ${it.category} | ${it.mathematicalReason}", color = Amber, fontSize = 9.sp) }
            }
        }
    }
}

@Composable
fun SolverPracticePanel(
    problem: PracticeProblem?,
    mode: PracticeMode,
    input: TextFieldValue,
    feedback: String?,
    onMode: (PracticeMode) -> Unit,
    onInput: (TextFieldValue) -> Unit,
    onCheck: () -> Unit,
    onNew: () -> Unit,
    onClose: () -> Unit,
) {
    LearningPanel(Violet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Practise similar problems", color = Violet, fontWeight = FontWeight.Bold)
            GlowButton("Close", onClick = onClose)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PracticeMode.entries.forEach { item -> GlowButton(if (item == mode) "* ${item.label}" else item.label) { onMode(item) } }
        }
        if (problem == null) {
            Text("No verified practice template is available for this problem yet.", color = Amber, fontSize = 10.sp)
        } else {
            Text(problem.prompt, color = Ink, fontSize = 12.sp)
            Text("${problem.skill} | ${problem.difficulty.level} | estimate ${problem.difficulty.score}", color = Muted, fontSize = 9.sp)
            if (problem.choices.isNotEmpty()) {
                problem.choices.forEach { Text("- $it", color = Cyan, fontSize = 10.sp) }
            }
            IntentAwareMathValueField(
                value = input,
                onValueChange = onInput,
                label = "Practice answer",
                placeholder = "Enter your answer",
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Practice answer using mathematical keyboard" },
                keyboardContext = MathKeyboardContext.GENERAL,
                useMathKeyboard = true,
                showLegend = false,
                onDone = onCheck,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                GlowButton("Check answer", enabled = input.text.isNotBlank(), onClick = onCheck)
                GlowButton("New problem", onClick = onNew)
            }
            feedback?.let { Text(it, color = if (it.startsWith("Correct")) Green else Amber, fontSize = 10.sp) }
            Text("Local validation: ${problem.validationMessage}", color = Muted, fontSize = 8.sp)
        }
    }
}

@Composable
fun SolverMasteryPanel(
    summary: SolverLearningSummary,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    LearningPanel(Amber) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Learning estimate", color = Amber, fontWeight = FontWeight.Bold)
                Text("Private, local and approximate", color = Muted, fontSize = 9.sp)
            }
            GlowButton("Close", onClick = onClose)
        }
        Text("Only aggregate skill counts are stored. Raw tutor entries are not retained or shared.", color = Ink, fontSize = 10.sp)
        if (summary.skills.isEmpty()) Text("No Solver learning activity recorded.", color = Muted, fontSize = 10.sp)
        summary.skills.forEach { skill ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(skill.skill.replace(Regex("([a-z])([A-Z])"), "$1 $2"), color = Ink, fontSize = 10.sp)
                Text("${skill.estimatePercent}% | ${skill.trend}", color = if (skill.trend == "needs review") Amber else Green, fontSize = 10.sp)
            }
        }
        if (summary.needsReview.isNotEmpty()) Text("Review: ${summary.needsReview.joinToString()}", color = Amber, fontSize = 9.sp)
        GlowButton("Clear Solver learning data", enabled = summary.skills.isNotEmpty(), onClick = onClear)
    }
}

@Composable
fun SolverCataloguePanel(
    query: String,
    onQuery: (String) -> Unit,
    onChoose: (SolverCalculatorPreset) -> Unit,
    onClose: () -> Unit,
) {
    LearningPanel(Cyan) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Solver calculators", color = Cyan, fontWeight = FontWeight.Bold)
            GlowButton("Close", onClick = onClose)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Search calculators") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search shared Solver calculators" },
        )
        SolverCalculatorCatalogue.search(query).filter { it.supported }.groupBy { it.group }.forEach { (group, presets) ->
            Text(group.label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEach { preset -> GlowButton(preset.title) { onChoose(preset) } }
            }
        }
    }
}

@Composable
private fun LearningPanel(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(SurfaceA.copy(alpha = .94f), RoundedCornerShape(7.dp))
            .border(1.dp, accent.copy(alpha = .5f), RoundedCornerShape(7.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}
