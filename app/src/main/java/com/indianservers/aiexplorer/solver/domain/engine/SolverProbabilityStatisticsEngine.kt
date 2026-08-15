package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.ProbabilityStatisticsEngine
import com.indianservers.aiexplorer.solver.domain.model.*
import com.indianservers.aiexplorer.solver.domain.steps.SolverExplanationEngine
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class SolverProbabilityStatisticsEngine {
    fun solve(source: String, profile: ExplanationProfile): SolverSolution? {
        descriptive(source, profile)?.let { return it }
        binomial(source, profile)?.let { return it }
        normalPdf(source, profile)?.let { return it }
        regression(source, profile)?.let { return it }
        combinatorics(source, profile)?.let { return it }
        return null
    }

    private fun descriptive(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^(stats|mean|median|standard\\s*deviation)\\s*(?:\\(|\\s)(.+?)\\)?$").matchEntire(source.trim()) ?: return null
        val values = parseValues(match.groupValues[2]) ?: return unsupported(source, profile, "Enter at least one finite comma-separated observation.", ProblemType.Statistics)
        val summary = ProbabilityStatisticsEngine.descriptive(values).getOrElse { return unsupported(source, profile, it.message ?: "Invalid dataset.", ProblemType.Statistics) }
        val operation = match.groupValues[1].lowercase().replace(" ", "")
        val answer = when (operation) {
            "mean" -> "Mean = ${number(summary.mean)}"
            "median" -> "Median = ${number(summary.median)}"
            "standarddeviation" -> "Population standard deviation = ${number(summary.populationStandardDeviation, 6)}"
            else -> "mean=${number(summary.mean)}; median=${number(summary.median)}; sd=${number(summary.populationStandardDeviation)}"
        }
        val sum = values.sum()
        val steps = listOf(
            step(source, "Order and validate data", values.sorted().joinToString(", ", transform = ::number), "Use every finite observation.", profile),
            step(values.joinToString(","), "Calculate the centre", "sum=${number(sum)}, n=${values.size}, mean=${number(summary.mean)}, median=${number(summary.median)}", "Compute the requested measures from the complete dataset.", profile),
            step("summary", "Calculate spread", answer, "Population variance divides the squared deviations by n.", profile),
        )
        val independentMean = values.fold(0.0, Double::plus) / values.size
        return solution(source, ProblemType.Statistics, answer, steps, profile, listOf(
            VerificationCheck("Independent sum/count mean", abs(independentMean - summary.mean) < 1e-10, number(independentMean), number(summary.mean)),
            VerificationCheck("Observation count", summary.count == values.size, values.size.toString(), summary.count.toString()),
        ))
    }

    private fun binomial(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^binomial\\s+(pmf|cdf)\\s+(\\d+)\\s+n\\s+(\\d+)\\s+p\\s+(\\d*(?:\\.\\d+)?)$").matchEntire(source.trim()) ?: return null
        val kind = match.groupValues[1].lowercase(); val x = match.groupValues[2].toInt(); val n = match.groupValues[3].toInt(); val p = match.groupValues[4].toDouble()
        val snapshot = ProbabilityStatisticsEngine.binomial(n, p, x).getOrElse { return unsupported(source, profile, it.message ?: "Invalid binomial parameters.", ProblemType.Probability) }
        val answerValue = if (kind == "pmf") snapshot.pmf else snapshot.cdf
        val answer = number(answerValue)
        val formula = if (kind == "pmf") "C($n,$x)·${number(p)}^$x·${number(1-p)}^${n-x}" else "Σ P(X=k), k=0…$x"
        val steps = listOf(
            step(source, "Identify the model", "X ~ Binomial(n=$n, p=${number(p)})", "Trials are fixed, independent and have a constant success probability.", profile),
            step("model", "Apply ${kind.uppercase()}", formula, "Use the binomial probability law.", profile),
            step(formula, "Evaluate", answer, "The probability is constrained to [0,1].", profile),
        )
        val independent = if (kind == "pmf") independentBinomial(n, x, p) else (0..x).sumOf { independentBinomial(n, it, p) }
        return solution(source, ProblemType.Probability, answer, steps, profile, listOf(
            VerificationCheck("Independent combinatorial evaluation", abs(independent - answerValue) < 1e-10, number(independent), answer),
            VerificationCheck("Probability bounds", answerValue in 0.0..1.0, "0 ≤ P ≤ 1", answer),
        ))
    }

    private fun normalPdf(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^normal\\s+pdf\\s+(-?\\d+(?:\\.\\d+)?)\\s+mean\\s+(-?\\d+(?:\\.\\d+)?)\\s+sd\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source.trim()) ?: return null
        val x = match.groupValues[1].toDouble(); val mean = match.groupValues[2].toDouble(); val sd = match.groupValues[3].toDouble()
        val value = ProbabilityStatisticsEngine.normalPdf(x, mean, sd).getOrElse { return unsupported(source, profile, it.message ?: "Invalid normal parameters.", ProblemType.Probability) }
        val z = (x - mean) / sd
        val answer = number(value)
        val steps = listOf(
            step(source, "Standardize", "z=(${number(x)}-${number(mean)})/${number(sd)}=${number(z)}", "Measure the observation in standard deviations from the mean.", profile),
            step("z=${number(z)}", "Evaluate density", "φ(x)=exp(-z²/2)/(σ√(2π))", "Apply the normal density formula.", profile),
            step("density", "State the result", answer, "A density is non-negative; it is not itself an interval probability.", profile),
        )
        val independent = exp(-.5 * z * z) / (sd * sqrt(2 * Math.PI))
        return solution(source, ProblemType.Probability, answer, steps, profile, listOf(VerificationCheck("Independent density formula", abs(independent - value) < 1e-12, number(independent), answer)))
    }

    private fun regression(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^regression\\s+x\\s*:\\s*(.+?)\\s*;\\s*y\\s*:\\s*(.+)$").matchEntire(source.trim()) ?: return null
        val x = parseValues(match.groupValues[1]); val y = parseValues(match.groupValues[2])
        if (x == null || y == null) return unsupported(source, profile, "Enter finite comma-separated x and y datasets.", ProblemType.Statistics)
        val result = ProbabilityStatisticsEngine.regression(x, y).getOrElse { return unsupported(source, profile, it.message ?: "Invalid paired dataset.", ProblemType.Statistics) }
        val answer = "y=${number(result.intercept)}+${number(result.slope)}x; r=${number(result.correlation)}"
        val residualSum = x.indices.sumOf { y[it] - (result.intercept + result.slope * x[it]) }
        val steps = listOf(
            step(source, "Pair observations", "n=${x.size}", "Every x value must have one corresponding y value.", profile),
            step("paired data", "Fit least-squares line", "slope=${number(result.slope)}, intercept=${number(result.intercept)}", "Minimize the sum of squared vertical residuals.", profile),
            step("fit", "Report association", answer, "Correlation describes linear association, not causation.", profile),
        )
        return solution(source, ProblemType.Statistics, answer, steps, profile, listOf(
            VerificationCheck("Residuals sum to zero", abs(residualSum) < 1e-8, "0", number(residualSum)),
            VerificationCheck("Correlation bounds", result.correlation in -1.0..1.0, "-1 ≤ r ≤ 1", number(result.correlation)),
        ))
    }

    private fun combinatorics(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^(ncr|combinations|npr|permutations)\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)$").matchEntire(source.trim()) ?: return null
        val n = match.groupValues[2].toInt(); val r = match.groupValues[3].toInt(); val combination = match.groupValues[1].lowercase() in setOf("ncr", "combinations")
        val value = (if (combination) ProbabilityStatisticsEngine.combinations(n, r) else ProbabilityStatisticsEngine.permutations(n, r)).getOrElse { return unsupported(source, profile, it.message ?: "Invalid combinatorics parameters.", ProblemType.Probability) }
        val answer = value.toString(); val formula = if (combination) "$n!/($r!·${n-r}!)" else "$n!/${n-r}!"
        return solution(source, ProblemType.Probability, answer, listOf(
            step(source, "Choose counting model", if (combination) "Order does not matter" else "Order matters", "Distinguish selections from arrangements.", profile),
            step("model", "Apply formula", formula, "Use factorial cancellation before multiplying.", profile),
            step(formula, "Evaluate exactly", answer, "The result is a non-negative integer count.", profile),
        ), profile, listOf(VerificationCheck("Exact integer count", value >= 0, "non-negative integer", answer)))
    }

    private fun solution(source: String, type: ProblemType, answer: String, steps: List<SolutionStep>, profile: ExplanationProfile, checks: List<VerificationCheck>) = SolverSolution(
        SolverInput(source, source.trim()), node(source), ProblemClassification(type, .99f, listOf("structured probability/statistics command")), steps, node(answer), answer,
        VerificationResult(if (checks.all { it.passed }) VerificationStatus.Verified else VerificationStatus.Failed, VerificationMethod.ExactEvaluation, checks, "Recomputed through independent statistical invariants."),
        true, "Solved with the offline Probability & Statistics engine.", explanationProfile = profile, exactAnswer = answer,
        ruleCitations = listOf(SolverRuleRegistry.DATA_SUMMARY), verificationStrength = if (checks.all { it.passed }) VerificationStrength.NumericallyVerified else VerificationStrength.Failed,
    )

    private fun unsupported(source: String, profile: ExplanationProfile, message: String, type: ProblemType) = SolverSolution(
        SolverInput(source, source.trim()), node(source), ProblemClassification(type, .99f, listOf(message)), emptyList(), null, null,
        VerificationResult(VerificationStatus.NotApplicable, VerificationMethod.ExactEvaluation, emptyList(), message), false, message, explanationProfile = profile,
    )

    private fun step(before: String, title: String, after: String, explanation: String, profile: ExplanationProfile) = SolutionStep(
        "stats-${title.hashCode()}-${after.hashCode()}", node(before), node(after), StepOperation.Calculate, SolverRuleRegistry.DATA_SUMMARY, SolverRuleRegistry.DATA_SUMMARY,
        SolverExplanationEngine.explanation(SolverRuleRegistry.DATA_SUMMARY, profile, explanation), listOf(ExpressionPath(emptyList())), listOf(StepDetail("Stage", title)), true,
    )
    private fun node(text: String) = MathExpression.Variable(text, SourceSpan(0, text.length))
    private fun parseValues(source: String) = runCatching { source.trim().removePrefix("[").removeSuffix("]").split(Regex("[,;\\s]+")).filter(String::isNotBlank).map(String::toDouble).also { require(it.isNotEmpty() && it.all(Double::isFinite)) } }.getOrNull()
    private fun independentBinomial(n: Int, r: Int, p: Double): Double { if (r !in 0..n) return 0.0; var c=1.0; for(i in 1..minOf(r,n-r)) c=c*(n-minOf(r,n-r)+i)/i; return c*p.pow(r)*(1-p).pow(n-r) }
    private fun number(value: Double, decimals: Int = 10) = if (abs(value - value.toLong()) < 1e-10) value.toLong().toString() else String.format(java.util.Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
}
