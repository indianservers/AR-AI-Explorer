package com.indianservers.aiexplorer.features.probabilitystatistics.calculation

import com.indianservers.aiexplorer.features.probabilitystatistics.models.AnalysisObjective
import com.indianservers.aiexplorer.features.probabilitystatistics.models.GroupStructure
import com.indianservers.aiexplorer.features.probabilitystatistics.models.OutcomeType
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticalTestRecommendation
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class DescriptiveResult(
    val count: Int,
    val mean: Double,
    val median: Double,
    val modes: List<Double>,
    val minimum: Double,
    val maximum: Double,
    val q1: Double,
    val q3: Double,
    val populationVariance: Double,
    val sampleVariance: Double?,
) {
    val range: Double get() = maximum - minimum
    val interquartileRange: Double get() = q3 - q1
    val populationStandardDeviation: Double get() = sqrt(populationVariance)
    val sampleStandardDeviation: Double? get() = sampleVariance?.let(::sqrt)
}

data class RegressionCalculation(
    val intercept: Double,
    val slope: Double,
    val correlation: Double,
    val rSquared: Double,
)

data class DatasetQuality(
    val validValues: List<Double>,
    val invalidTokens: List<String>,
    val missingCount: Int,
    val outliers: List<Double>,
)

data class BinomialSnapshot(
    val n: Int,
    val p: Double,
    val x: Int,
    val pmf: Double,
    val cdf: Double,
    val mean: Double,
    val variance: Double,
    val standardDeviation: Double,
    val mode: Int,
)

data class BinomialSimulation(
    val experiments: Int,
    val theoreticalProbability: Double,
    val empiricalProbability: Double,
    val successCounts: List<Int>,
)

object ProbabilityStatisticsEngine {
    fun descriptive(values: List<Double>): Result<DescriptiveResult> = runCatching {
        require(values.isNotEmpty()) { "Enter at least one finite value." }
        require(values.all(Double::isFinite)) { "All observations must be finite." }
        val sorted = values.sorted()
        val mean = stableMean(sorted)
        val frequencies = sorted.groupingBy { it }.eachCount()
        val highest = frequencies.maxOf { it.value }
        val modes = if (highest == 1) emptyList() else frequencies.filterValues { it == highest }.keys.sorted()
        val populationVariance = stableSquaredDeviation(sorted, mean) / sorted.size
        DescriptiveResult(
            count = sorted.size,
            mean = mean,
            median = percentileSorted(sorted, 0.5),
            modes = modes,
            minimum = sorted.first(),
            maximum = sorted.last(),
            q1 = percentileSorted(sorted, 0.25),
            q3 = percentileSorted(sorted, 0.75),
            populationVariance = populationVariance,
            sampleVariance = if (sorted.size > 1) stableSquaredDeviation(sorted, mean) / (sorted.size - 1) else null,
        )
    }

    fun weightedMean(values: List<Double>, weights: List<Double>): Result<Double> = runCatching {
        require(values.isNotEmpty() && values.size == weights.size) { "Values and weights must have the same non-zero size." }
        require(values.all(Double::isFinite) && weights.all { it.isFinite() && it >= 0.0 }) { "Values must be finite and weights non-negative." }
        val weight = weights.sum()
        require(weight > 0.0) { "At least one weight must be positive." }
        values.indices.sumOf { values[it] * weights[it] } / weight
    }

    fun percentile(values: List<Double>, probability: Double): Result<Double> = runCatching {
        require(values.isNotEmpty() && values.all(Double::isFinite)) { "Enter finite observations." }
        require(probability in 0.0..1.0) { "Percentile probability must be from 0 to 1." }
        percentileSorted(values.sorted(), probability)
    }

    fun covariance(x: List<Double>, y: List<Double>, sample: Boolean = true): Result<Double> = runCatching {
        require(x.size == y.size && x.isNotEmpty()) { "Paired datasets must have equal non-zero size." }
        require(x.all(Double::isFinite) && y.all(Double::isFinite)) { "All paired values must be finite." }
        require(!sample || x.size > 1) { "Sample covariance needs at least two pairs." }
        val mx = stableMean(x)
        val my = stableMean(y)
        x.indices.sumOf { (x[it] - mx) * (y[it] - my) } / if (sample) x.size - 1 else x.size
    }

    fun regression(x: List<Double>, y: List<Double>): Result<RegressionCalculation> = runCatching {
        require(x.size == y.size && x.size >= 2) { "Regression needs at least two paired observations." }
        require(x.all(Double::isFinite) && y.all(Double::isFinite)) { "All paired values must be finite." }
        val mx = stableMean(x)
        val my = stableMean(y)
        val sxx = x.sumOf { (it - mx).pow(2) }
        val syy = y.sumOf { (it - my).pow(2) }
        require(sxx > 1e-15 && syy > 1e-15) { "Regression requires variation in both variables." }
        val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val slope = sxy / sxx
        val correlation = (sxy / sqrt(sxx * syy)).coerceIn(-1.0, 1.0)
        RegressionCalculation(my - slope * mx, slope, correlation, correlation * correlation)
    }

    fun combinations(n: Int, r: Int): Result<Long> = runCatching {
        require(n in 0..66 && r in 0..n) { "Use 0 ≤ r ≤ n ≤ 66." }
        val k = minOf(r, n - r)
        var result = 1L
        for (i in 1..k) result = Math.multiplyExact(result, (n - k + i).toLong()) / i
        result
    }

    fun permutations(n: Int, r: Int): Result<Long> = runCatching {
        require(n in 0..20 && r in 0..n) { "Use 0 ≤ r ≤ n ≤ 20." }
        (n - r + 1..n).fold(1L) { total, value -> Math.multiplyExact(total, value.toLong()) }
    }

    fun binomial(n: Int, p: Double, x: Int): Result<BinomialSnapshot> = runCatching {
        validateBinomial(n, p, x)
        val pmf = binomialPmf(n, p, x)
        val cdf = (0..x).sumOf { binomialPmf(n, p, it) }.coerceIn(0.0, 1.0)
        val mean = n * p
        val variance = n * p * (1 - p)
        BinomialSnapshot(n, p, x, pmf, cdf, mean, variance, sqrt(variance), kotlin.math.floor((n + 1) * p).toInt().coerceIn(0, n))
    }

    fun binomialPmf(n: Int, p: Double, x: Int): Double {
        validateBinomial(n, p, x)
        if (p == 0.0) return if (x == 0) 1.0 else 0.0
        if (p == 1.0) return if (x == n) 1.0 else 0.0
        val logCombination = logFactorial(n) - logFactorial(x) - logFactorial(n - x)
        return exp(logCombination + x * ln(p) + (n - x) * ln(1 - p)).coerceIn(0.0, 1.0)
    }

    fun simulateBinomial(n: Int, p: Double, target: Int, experiments: Int, seed: Long): Result<BinomialSimulation> = runCatching {
        validateBinomial(n, p, target)
        require(experiments in 1..20_000) { "Use 1 to 20,000 experiments." }
        val random = Random(seed)
        val counts = List(experiments) {
            var successes = 0
            repeat(n) { if (random.nextDouble() < p) successes++ }
            successes
        }
        val empirical = counts.count { it == target }.toDouble() / experiments
        BinomialSimulation(experiments, binomialPmf(n, p, target), empirical, counts)
    }

    fun normalPdf(x: Double, mean: Double, standardDeviation: Double): Result<Double> = runCatching {
        require(x.isFinite() && mean.isFinite() && standardDeviation.isFinite() && standardDeviation > 0) { "Use finite values and positive standard deviation." }
        val z = (x - mean) / standardDeviation
        exp(-0.5 * z * z) / (standardDeviation * sqrt(2 * PI))
    }

    fun meanConfidenceInterval(values: List<Double>, level: Double = 0.95): Result<Pair<Double, Double>> = runCatching {
        require(values.size >= 2 && values.all(Double::isFinite)) { "A mean interval needs at least two finite observations." }
        require(level in 0.8..0.999) { "Confidence level must be from 0.80 to 0.999." }
        val summary = descriptive(values).getOrThrow()
        val z = inverseStandardNormal(0.5 + level / 2)
        val margin = z * summary.sampleStandardDeviation!! / sqrt(values.size.toDouble())
        summary.mean - margin to summary.mean + margin
    }

    fun inspectDataset(text: String): DatasetQuality {
        val tokens = text.split(Regex("[,;\\s]+"))
        val missing = tokens.count { it.isBlank() }
        val invalid = tokens.filter { it.isNotBlank() && it.toDoubleOrNull()?.isFinite() != true }
        val values = tokens.mapNotNull { it.toDoubleOrNull()?.takeIf(Double::isFinite) }.take(5_000)
        val outliers = descriptive(values).getOrNull()?.let { result ->
            val low = result.q1 - 1.5 * result.interquartileRange
            val high = result.q3 + 1.5 * result.interquartileRange
            values.filter { it < low || it > high }
        }.orEmpty()
        return DatasetQuality(values, invalid, missing, outliers)
    }

    private fun validateBinomial(n: Int, p: Double, x: Int) {
        require(n in 1..1_000) { "Number of trials must be from 1 to 1,000." }
        require(p.isFinite() && p in 0.0..1.0) { "Success probability must be from 0 to 1." }
        require(x in 0..n) { "Success count must be from 0 to n." }
    }

    private fun logFactorial(n: Int): Double = (2..n).sumOf { ln(it.toDouble()) }

    private fun stableMean(values: List<Double>): Double {
        var mean = 0.0
        values.forEachIndexed { index, value -> mean += (value - mean) / (index + 1) }
        return mean
    }

    private fun stableSquaredDeviation(values: List<Double>, mean: Double): Double {
        var sum = 0.0
        var correction = 0.0
        for (value in values) {
            val term = (value - mean).pow(2) - correction
            val next = sum + term
            correction = (next - sum) - term
            sum = next
        }
        return sum
    }

    private fun percentileSorted(sorted: List<Double>, probability: Double): Double {
        if (sorted.size == 1) return sorted.first()
        val position = probability * (sorted.size - 1)
        val lower = position.toInt()
        val fraction = position - lower
        return sorted[lower] + fraction * (sorted[minOf(lower + 1, sorted.lastIndex)] - sorted[lower])
    }

    private fun inverseStandardNormal(p: Double): Double {
        require(p in 0.0..1.0 && p != 0.0 && p != 1.0)
        val a = doubleArrayOf(-39.6968302866538, 220.946098424521, -275.928510446969, 138.357751867269, -30.6647980661472, 2.50662827745924)
        val b = doubleArrayOf(-54.4760987982241, 161.585836858041, -155.698979859887, 66.8013118877197, -13.2806815528857)
        val c = doubleArrayOf(-0.00778489400243029, -0.322396458041136, -2.40075827716184, -2.54973253934373, 4.37466414146497, 2.93816398269878)
        val d = doubleArrayOf(0.00778469570904146, 0.32246712907004, 2.445134137143, 3.75440866190742)
        val low = 0.02425
        return when {
            p < low -> {
                val q = sqrt(-2 * ln(p))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
            p > 1 - low -> {
                val q = sqrt(-2 * ln(1 - p))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
            else -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
            }
        }
    }
}

object StatisticalTestGuide {
    fun recommend(
        objective: AnalysisObjective,
        outcome: OutcomeType,
        groups: GroupStructure,
        parametricAssumptions: Boolean,
    ): StatisticalTestRecommendation {
        val method = when {
            objective == AnalysisObjective.Predict && outcome == OutcomeType.Binary -> "Logistic regression"
            objective == AnalysisObjective.Predict && outcome == OutcomeType.TimeSeries -> "Time-series regression or ARIMA"
            objective == AnalysisObjective.Predict -> "Linear regression"
            objective == AnalysisObjective.Associate && outcome == OutcomeType.Categorical -> "Chi-square test of independence"
            objective == AnalysisObjective.Associate && parametricAssumptions -> "Pearson correlation"
            objective == AnalysisObjective.Associate -> "Spearman rank correlation"
            objective == AnalysisObjective.Fit && outcome == OutcomeType.Categorical -> "Chi-square goodness-of-fit test"
            groups == GroupStructure.One && outcome == OutcomeType.Quantitative -> if (parametricAssumptions) "One-sample t-test" else "Wilcoxon signed-rank test"
            groups == GroupStructure.TwoIndependent && outcome == OutcomeType.Quantitative -> if (parametricAssumptions) "Welch independent-samples t-test" else "Mann-Whitney U test"
            groups == GroupStructure.TwoPaired && outcome == OutcomeType.Quantitative -> if (parametricAssumptions) "Paired t-test" else "Wilcoxon signed-rank test"
            groups == GroupStructure.Many && outcome == OutcomeType.Quantitative -> if (parametricAssumptions) "One-way ANOVA" else "Kruskal-Wallis test"
            else -> "Chi-square test of independence"
        }
        val alternative = when (method) {
            "Welch independent-samples t-test" -> "Mann-Whitney U test when distributional assumptions are not credible."
            "Paired t-test" -> "Wilcoxon signed-rank test for paired ordinal or strongly non-normal data."
            "One-way ANOVA" -> "Kruskal-Wallis test for independent groups without parametric assumptions."
            "Pearson correlation" -> "Spearman rank correlation for monotonic or ordinal relationships."
            else -> "Use resampling or an exact procedure when standard assumptions are doubtful."
        }
        return StatisticalTestRecommendation(
            method = method,
            reason = "This procedure matches a ${objective.label.lowercase()} objective, a ${outcome.label.lowercase()} outcome, and ${groups.label.lowercase()}.",
            assumptions = when {
                "regression" in method.lowercase() -> listOf("Independent observations", "Correct model form", "Inspect residuals and influential points")
                "Chi-square" in method -> listOf("Independent counts", "Mutually exclusive categories", "Adequate expected cell counts")
                "ANOVA" in method || "t-test" in method -> listOf("Independent observational units", "Representative sampling or random assignment", "Check distribution and variance assumptions")
                else -> listOf("Representative observations", "Appropriate measurement scale", "Independent observational units")
            },
            alternative = alternative,
            topicId = if ("regression" in method.lowercase() || "correlation" in method.lowercase()) "regression-correlation" else "hypothesis-testing",
        )
    }
}
