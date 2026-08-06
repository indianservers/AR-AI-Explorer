package com.indianservers.aiexplorer.core

enum class LearnerStepStatus { Correct, Incorrect, NeedsReview }

data class LearnerStepFeedback(
    val lineNumber: Int,
    val source: String,
    val status: LearnerStepStatus,
    val mistakeKind: MistakeKind? = null,
    val message: String,
    val hint: String,
    val evidence: String? = null,
)

data class LearnerWorkReport(
    val steps: List<LearnerStepFeedback>,
) {
    val firstIncorrect: LearnerStepFeedback? get() = steps.firstOrNull { it.status == LearnerStepStatus.Incorrect }
    val correctCount: Int get() = steps.count { it.status == LearnerStepStatus.Correct }
    val allCorrect: Boolean get() = steps.isNotEmpty() && steps.all { it.status == LearnerStepStatus.Correct }
    val summary: String
        get() = when {
            steps.isEmpty() -> "Enter one mathematical transformation per line."
            firstIncorrect != null -> "Check line ${firstIncorrect?.lineNumber}; earlier verified lines are preserved."
            steps.any { it.status == LearnerStepStatus.NeedsReview } -> "Some lines need a clearer mathematical statement."
            else -> "All ${steps.size} submitted transformations are mathematically equivalent."
        }
}

/** Verifies each learner transformation against the previous line. */
class LearnerWorkAnalyzer(
    private val kernel: TrustedMathKernel = TrustedMathKernel(),
    private val solver: MathProblemSolver = MathProblemSolver(),
) {
    fun analyze(question: String, learnerWork: String): LearnerWorkReport {
        val lines = learnerWork.lineSequence()
            .map(::cleanLine)
            .filter(String::isNotBlank)
            .toList()
        if (lines.isEmpty()) return LearnerWorkReport(emptyList())

        val baseline = extractMathStatement(question)
        var previous = baseline
        val feedback = lines.mapIndexed { index, current ->
            val lineNumber = index + 1
            val localIssue = localIssue(current, previous, lineNumber)
            val result = localIssue ?: verify(previous, current, lineNumber)
            previous = current
            result
        }
        return LearnerWorkReport(feedback)
    }

    private fun verify(previous: String, current: String, lineNumber: Int): LearnerStepFeedback {
        if (canonical(previous) == canonical(current)) {
            return correct(lineNumber, current, "This line restates the previous mathematical relationship.")
        }
        if (isEquation(previous) && isEquation(current)) {
            val previousSolution = solver.solve("Solve $previous")
            val currentSolution = solver.solve("Solve $current")
            if (previousSolution.supported && currentSolution.supported) {
                if (canonicalAnswer(previousSolution.answer) == canonicalAnswer(currentSolution.answer)) {
                    return correct(lineNumber, current, "Both equations have the same verified solution set.")
                }
                val kind = classifyChange(previous, current)
                return LearnerStepFeedback(
                    lineNumber,
                    current,
                    LearnerStepStatus.Incorrect,
                    kind,
                    messageFor(kind),
                    hintFor(kind),
                    "The verified solution set changes on this line.",
                )
            }
        }
        val evidence = kernel.verifyTransformation(previous, current)
        return when (evidence.status) {
            EquivalenceStatus.Exact -> correct(lineNumber, current, "The symbolic difference is exactly zero.")
            EquivalenceStatus.VerifiedNumerically -> correct(
                lineNumber,
                current,
                "The transformation agrees at ${evidence.samples.size} deterministic test values.",
            )
            EquivalenceStatus.DomainMismatch -> LearnerStepFeedback(
                lineNumber,
                current,
                LearnerStepStatus.Incorrect,
                MistakeKind.Domain,
                "This step changes the allowed values.",
                "State the restriction introduced by division, roots, powers, or logarithms.",
                evidence.explanation,
            )
            EquivalenceStatus.NotEquivalent -> {
                val kind = classifyChange(previous, current)
                val sample = evidence.samples.firstOrNull { it.residual > evidence.tolerance }
                LearnerStepFeedback(
                    lineNumber,
                    current,
                    LearnerStepStatus.Incorrect,
                    kind,
                    messageFor(kind),
                    hintFor(kind),
                    sample?.let {
                        val at = it.variables.entries.joinToString { entry -> "${entry.key}=${format(entry.value)}" }
                        "Counterexample: at $at, previous=${format(it.left)} but this line=${format(it.right)}."
                    } ?: evidence.explanation,
                )
            }
            EquivalenceStatus.Inconclusive -> LearnerStepFeedback(
                lineNumber,
                current,
                LearnerStepStatus.NeedsReview,
                message = "I cannot safely verify this line as written.",
                hint = "Write only the equation or expression for this step, using explicit brackets and operators.",
                evidence = evidence.explanation,
            )
        }
    }

    private fun localIssue(current: String, previous: String, lineNumber: Int): LearnerStepFeedback? {
        if (!balanced(current)) {
            return incorrect(
                lineNumber,
                current,
                MistakeKind.Bracket,
                "A bracket is missing or closed in the wrong place.",
                "Balance every grouping before performing the next operation.",
            )
        }
        if (Regex("""sqrt\s*\(\s*-|ln\s*\(\s*(?:0|-)|log\s*\(\s*(?:0|-)""", RegexOption.IGNORE_CASE).containsMatchIn(current)) {
            return incorrect(
                lineNumber,
                current,
                MistakeKind.Domain,
                "This line uses a real square root or logarithm outside its domain.",
                "Check that every square-root input is non-negative and every logarithm input is positive.",
            )
        }
        val units = Regex("""\b(mm|cm|m|km|mg|g|kg|s|min|h)\b""", RegexOption.IGNORE_CASE)
            .findAll(current).map { it.value.lowercase() }.toSet()
        if (units.size > 1 && !current.contains("convert", true)) {
            return incorrect(
                lineNumber,
                current,
                MistakeKind.Unit,
                "This line combines different units without an explicit conversion.",
                "Convert all quantities to compatible units before calculating.",
            )
        }
        if (previous.isBlank()) return LearnerStepFeedback(
            lineNumber,
            current,
            LearnerStepStatus.NeedsReview,
            message = "The original mathematical statement could not be isolated.",
            hint = "Start by rewriting the equation or expression from the question.",
        )
        return null
    }

    private fun classifyChange(previous: String, current: String): MistakeKind {
        val previousNumbers = numberPattern.findAll(previous).map { it.value }.toList()
        val currentNumbers = numberPattern.findAll(current).map { it.value }.toList()
        val signChanged = previous.count { it == '-' || it == '−' } != current.count { it == '-' || it == '−' }
        return when {
            signChanged && previousNumbers.any { it in currentNumbers } -> MistakeKind.Sign
            previousNumbers.isNotEmpty() && currentNumbers.isNotEmpty() -> MistakeKind.Arithmetic
            else -> MistakeKind.Formula
        }
    }

    private fun messageFor(kind: MistakeKind) = when (kind) {
        MistakeKind.Sign -> "The first invalid transformation changes a sign inconsistently."
        MistakeKind.Arithmetic -> "The numerical operation on this line does not preserve the previous value."
        MistakeKind.Formula -> "This transformation is not equivalent to the previous line."
        else -> "This line does not preserve the previous mathematical relationship."
    }

    private fun hintFor(kind: MistakeKind) = when (kind) {
        MistakeKind.Sign -> "Repeat the same inverse operation on both sides and track the sign before simplifying."
        MistakeKind.Arithmetic -> "Recalculate only this line, one operation at a time."
        MistakeKind.Formula -> "Name the rule you are applying, then check that its conditions are satisfied."
        else -> "Return to the previous verified line and make one explicit transformation."
    }

    private fun correct(line: Int, source: String, reason: String) = LearnerStepFeedback(
        line,
        source,
        LearnerStepStatus.Correct,
        message = "Valid transformation",
        hint = reason,
    )

    private fun incorrect(
        line: Int,
        source: String,
        kind: MistakeKind,
        message: String,
        hint: String,
    ) = LearnerStepFeedback(line, source, LearnerStepStatus.Incorrect, kind, message, hint)

    private fun extractMathStatement(question: String): String = question
        .replace(
            Regex(
                """(?i)^\s*(solve|calculate|evaluate|simplify|expand|factor|differentiate|integrate|find|what\s+is)\s*:?\s*""",
            ),
            "",
        )
        .trim().trimEnd('?')

    private fun cleanLine(line: String): String = line
        .replace(Regex("""^\s*(?:\d+[.)]|[-•])\s*"""), "")
        .replace(Regex("""(?i)^\s*(therefore|thus|so)\s*"""), "")
        .trim()

    private fun canonical(value: String) = value.replace(" ", "").replace("−", "-").lowercase()

    private fun canonicalAnswer(value: String): String {
        val numbers = numberPattern.findAll(value).map { it.value.toDouble() }.sorted().toList()
        return if (numbers.isNotEmpty()) numbers.joinToString(",") { format(it) } else canonical(value)
    }

    private fun isEquation(value: String): Boolean =
        value.count { it == '=' } == 1 && "<=" !in value && ">=" !in value && "!=" !in value

    private fun balanced(value: String): Boolean {
        var depth = 0
        value.forEach {
            if (it == '(' || it == '[' || it == '{') depth++
            if (it == ')' || it == ']' || it == '}') depth--
            if (depth < 0) return false
        }
        return depth == 0
    }

    private fun format(value: Double): String =
        if (kotlin.math.abs(value - value.toLong()) < 1e-9) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.5f", value).trimEnd('0').trimEnd('.')

    private companion object {
        val numberPattern = Regex("""-?\d+(?:\.\d+)?""")
    }
}
