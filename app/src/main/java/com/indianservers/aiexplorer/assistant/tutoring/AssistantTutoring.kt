package com.indianservers.aiexplorer.assistant.tutoring

import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.contracts.GroundedAssistantRequest
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantSummary
import com.indianservers.aiexplorer.learningintelligence.model.HintDefinition
import com.indianservers.aiexplorer.learningintelligence.model.HintLevel
import com.indianservers.aiexplorer.learningintelligence.model.LearnerAnswerStep
import com.indianservers.aiexplorer.learningintelligence.model.LearnerErrorType
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.practice.GeneratedPractice
import com.indianservers.aiexplorer.practice.VerifiedPracticeCatalog
import com.indianservers.aiexplorer.practice.VerifiedPracticeGenerator
import com.indianservers.aiexplorer.tutor.MistakeAwareStepEvaluator
import com.indianservers.aiexplorer.tutor.StepEvaluation
import java.util.Locale

data class AssistantHint(
    val id: String,
    val level: HintLevel,
    val text: String,
    val referenceIds: List<String>,
    val revealsAnswer: Boolean = level == HintLevel.FULL_EXPLANATION,
)

data class AssistantHintLadder(
    val conceptId: String,
    val allowedMaximum: HintLevel,
    val hints: List<AssistantHint>,
) {
    val nextHint: AssistantHint? get() = hints.firstOrNull()
}

data class AssistantStepCheckResult(
    val correctSoFar: Boolean,
    val firstInvalidIndex: Int?,
    val feedback: String,
    val repairPrompt: String,
    val misconceptionIds: Set<String> = emptySet(),
    val evaluations: List<StepEvaluation> = emptyList(),
)

data class MisconceptionReply(
    val title: String,
    val explanation: String,
    val repairQuestion: String,
    val referenceIds: List<String>,
)

data class ExamAnswerDraft(
    val title: String,
    val sections: List<Pair<String, String>>,
) {
    fun asPlainText(): String = buildString {
        appendLine(title)
        sections.forEach { (heading, body) ->
            appendLine()
            appendLine("$heading:")
            appendLine(body)
        }
    }.trim()
}

data class AssistantPracticePrompt(
    val conceptId: String,
    val prompt: String,
    val expectedAnswer: String,
    val firstHint: String,
    val validationNote: String,
    val seed: Int,
)

object AssistantHintLadderBuilder {
    fun from(request: GroundedAssistantRequest, previousHintIds: Set<String> = emptySet()): AssistantHintLadder {
        val catalogHints = LearningIntelligenceCatalog.hints
            .filter { it.conceptId == request.conceptId }
            .filter { it.level.ordinal <= request.maximumAllowedHintLevel.ordinal }
            .filter { it.id !in previousHintIds }
            .sortedBy { it.level.ordinal }

        val hints = catalogHints.ifEmpty { fallbackHints(request) }
            .take(4)
            .map { it.toAssistantHint(request) }

        return AssistantHintLadder(request.conceptId, request.maximumAllowedHintLevel, hints)
    }

    private fun fallbackHints(request: GroundedAssistantRequest): List<HintDefinition> {
        val text = request.verifiedContent.firstOrNull { it.reviewed }?.text
            ?: "Use one reviewed relationship and change only one thing at a time."
        return listOf(
            HintDefinition(
                id = "fallback-${request.conceptId}-${request.maximumAllowedHintLevel.name.lowercase(Locale.US)}",
                conceptId = request.conceptId,
                activityId = null,
                level = minOf(request.maximumAllowedHintLevel, HintLevel.CONCEPT_CUE),
                text = text,
                visualActionId = request.selectedVisualElements.firstOrNull(),
                revealedStepIds = emptySet(),
                misconceptionIds = request.suspectedMisconceptionIds,
            ),
        )
    }

    private fun HintDefinition.toAssistantHint(request: GroundedAssistantRequest): AssistantHint {
        val references = buildList {
            add(id)
            visualActionId?.let(::add)
            request.verifiedContent.firstOrNull { it.reviewed }?.id?.let(::add)
        }.distinct()
        return AssistantHint(id, level, text, references)
    }
}

class AssistantStepCheckEngine(
    private val evaluator: MistakeAwareStepEvaluator = MistakeAwareStepEvaluator(),
) {
    fun check(expectedSteps: List<String>, learnerSteps: List<LearnerAnswerStep>, misconceptionIds: Set<String> = emptySet()): AssistantStepCheckResult {
        if (expectedSteps.isEmpty()) {
            return AssistantStepCheckResult(
                correctSoFar = false,
                firstInvalidIndex = null,
                feedback = "I do not have a verified step route for this question yet.",
                repairPrompt = "Ask for a reviewed explanation first, then check the next line.",
            )
        }

        detectEquationBalanceMisuse(learnerSteps.firstOrNull()?.expression)?.let { return it }

        val evaluations = evaluator.ordered(expectedSteps, learnerSteps, misconceptionIds)
        val invalid = evaluator.firstInvalid(evaluations)
        return if (invalid == null) {
            AssistantStepCheckResult(
                correctSoFar = true,
                firstInvalidIndex = null,
                feedback = "All entered steps match the verified route so far.",
                repairPrompt = "Now write the next line and name the rule you used.",
                evaluations = evaluations,
            )
        } else {
            AssistantStepCheckResult(
                correctSoFar = false,
                firstInvalidIndex = invalid.stepIndex,
                feedback = "Step ${invalid.stepIndex + 1}: ${invalid.feedbackCode.replace('_', ' ')}.",
                repairPrompt = "Go back to ${invalid.expectedRelationship ?: "the previous line"} and make one smaller justified change.",
                misconceptionIds = invalid.misconceptionCandidateIds,
                evaluations = evaluations,
            )
        }
    }

    private fun detectEquationBalanceMisuse(expression: String?): AssistantStepCheckResult? {
        val compact = expression?.replace(" ", "") ?: return null
        if (!Regex("""[A-Za-z]\+\d+=\d+""").containsMatchIn(compact)) return null
        val values = Regex("""-?\d+""").findAll(compact).mapNotNull { it.value.toIntOrNull() }.toList()
        if (values.size < 2) return null
        return AssistantStepCheckResult(
            correctSoFar = false,
            firstInvalidIndex = 0,
            feedback = "The equation appears to change only one side of the balance.",
            repairPrompt = "Apply the same operation to both sides before simplifying.",
            misconceptionIds = setOf("one-sided-operation"),
            evaluations = listOf(
                StepEvaluation(
                    stepIndex = 0,
                    valid = false,
                    transformationType = "equation balance",
                    expectedRelationship = "same operation on both sides",
                    errorType = LearnerErrorType.ALGEBRAIC_TRANSFORMATION,
                    misconceptionCandidateIds = setOf("one-sided-operation"),
                    feedbackCode = "one_sided_operation",
                ),
            ),
        )
    }
}

object MisconceptionAwareAssistant {
    fun reply(request: GroundedAssistantRequest, stepCheck: AssistantStepCheckResult? = null): MisconceptionReply? {
        val id = stepCheck?.misconceptionIds?.firstOrNull() ?: request.suspectedMisconceptionIds.firstOrNull() ?: return null
        val authored = LearningIntelligenceCatalog.misconceptions.firstOrNull { it.id == id }
        val title = authored?.title ?: id.replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val explanation = authored?.description ?: when (id) {
            "one-sided-operation" -> "An equation stays equivalent only when the same valid operation is applied to both sides."
            "divide-by-variable" -> "Division by a variable needs a separate check for the zero case."
            "square-of-sum" -> "The middle product appears when the two brackets are multiplied term by term."
            else -> "This pattern usually means one reviewed condition or invariant was skipped."
        }
        return MisconceptionReply(
            title = title,
            explanation = explanation,
            repairQuestion = "Which condition must stay true before the next line is allowed?",
            referenceIds = request.verifiedContent.filter { it.reviewed }.take(2).map { it.id },
        )
    }
}

object AssistantExamAnswerFormatter {
    fun from(summary: WorkspaceAssistantSummary, workingSteps: List<String>, finalAnswer: String? = null, style: ExplanationStyle = ExplanationStyle.EXAM_FOCUSED): ExamAnswerDraft {
        val concise = style == ExplanationStyle.CONCISE
        val sections = buildList {
            add("Given" to summary.description)
            if (summary.formulas.isNotEmpty()) add("Rule" to summary.formulas.joinToString("\n"))
            if (workingSteps.isNotEmpty()) add("Working" to workingSteps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n"))
            add("Answer" to (finalAnswer ?: summary.measurements.firstOrNull() ?: "Use the verified workspace measurements."))
            if (!concise) add("Check" to (summary.measurements.take(3).joinToString("; ").ifBlank { "Check units, labels, and whether the result fits the figure." }))
        }
        return ExamAnswerDraft(summary.title, sections)
    }
}

class AssistantPracticePromptGenerator(
    private val generator: VerifiedPracticeGenerator = VerifiedPracticeGenerator(),
) {
    fun generate(conceptId: String, seed: Int): AssistantPracticePrompt {
        val template = VerifiedPracticeCatalog.templates.firstOrNull { it.conceptId == conceptId }
        val generated = template?.let { generator.generate(it, seed).task }
        return if (generated != null) generated.toPrompt() else fallback(conceptId, seed)
    }

    private fun GeneratedPractice.toPrompt(): AssistantPracticePrompt = AssistantPracticePrompt(
        conceptId = conceptId,
        prompt = prompt,
        expectedAnswer = formatAnswer(answer),
        firstHint = "Keep the invariant visible: use one reviewed relationship and check the domain before calculating.",
        validationNote = "Verified offline with ${validationStages.size} deterministic checks.",
        seed = seed,
    )

    private fun fallback(conceptId: String, seed: Int): AssistantPracticePrompt {
        val number = (kotlin.math.abs(seed) % 7) + 2
        return AssistantPracticePrompt(
            conceptId = conceptId,
            prompt = "Create one example for $conceptId using the number $number, then explain the rule used.",
            expectedAnswer = "A valid worked example with the rule named.",
            firstHint = "Start from the definition, then substitute the chosen value.",
            validationNote = "Fallback prompt uses reviewed concept wording and no network access.",
            seed = seed,
        )
    }

    private fun formatAnswer(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.3f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}
