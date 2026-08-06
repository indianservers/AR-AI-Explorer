package com.indianservers.aiexplorer.solver.domain.tutor

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.steps.MathRule

enum class HintLevel {
    ProblemType,
    RelevantConcept,
    NextTarget,
    SuggestedOperation,
    PartialNextStep,
    RevealedNextStep,
}

data class SolverHint(
    val level: HintLevel,
    val text: String,
    val revealsAnswer: Boolean,
    val relatedStepId: String?,
    val explanationProfile: ExplanationProfile,
)

enum class LearnerStepStatus {
    CorrectNextStep,
    CorrectLargeJump,
    CorrectAlternativeMethod,
    EquivalentReformatting,
    ValidButUnhelpful,
    IncompleteStep,
    ArithmeticError,
    SignError,
    RuleMisuse,
    DomainViolation,
    NonEquivalentTransformation,
    AmbiguousInput,
    UnsupportedTransformation,
}

enum class EquivalenceStatus {
    Equivalent,
    ConditionallyEquivalent,
    NotEquivalent,
    Unknown,
}

data class TutorFeedback(
    val headline: String,
    val explanation: String,
    val smallestUsefulClue: String,
    val encouraging: Boolean,
)

data class TutorMisconception(
    val id: String,
    val category: String,
    val observedTransformation: String,
    val mathematicalReason: String,
)

data class RecoveryAction(
    val label: String,
    val target: String,
)

data class LearnerStepEvaluation(
    val status: LearnerStepStatus,
    val equivalence: EquivalenceStatus,
    val matchedRule: MathRule?,
    val matchedStrategyId: String?,
    val feedback: TutorFeedback,
    val misconception: TutorMisconception?,
    val suggestedRecovery: RecoveryAction?,
)

enum class PracticeMode(val label: String) {
    EasierNumbers("Same skill, easier"),
    SimilarDifficulty("Same skill, similar"),
    HarderNumbers("Same skill, harder"),
    DifferentRepresentation("Different representation"),
    MixedReview("Mixed review"),
    CorrectTheMistake("Correct the mistake"),
    FillMissingStep("Fill the missing step"),
    ChooseNextStep("Choose the next step"),
    VerificationChallenge("Verification challenge"),
    ExplainTheRule("Explain the rule"),
}

enum class DifficultyLevel {
    Foundation,
    Beginner,
    Intermediate,
    Advanced,
    OlympiadStyle,
    University,
    ProofOriented,
}

data class DifficultyAssessment(
    val level: DifficultyLevel,
    val score: Int,
    val factors: Map<String, Int>,
    val explanation: String,
)

data class PracticeProblem(
    val id: String,
    val prompt: String,
    val expectedAnswer: String,
    val skill: String,
    val mode: PracticeMode,
    val difficulty: DifficultyAssessment,
    val hint: String,
    val validationMessage: String,
    val choices: List<String> = emptyList(),
)

data class TutorSessionSummary(
    val acceptedSteps: Int,
    val incorrectSteps: Int,
    val hintsUsed: Int,
    val misconceptions: List<String>,
    val completed: Boolean,
    val message: String,
)

