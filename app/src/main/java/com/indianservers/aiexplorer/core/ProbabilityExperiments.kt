package com.indianservers.aiexplorer.core

import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

enum class ExtendedDistributionKind { StudentT, ChiSquare, Gamma, Beta, Geometric, Hypergeometric }
data class ExtendedDistributionSummary(val kind: ExtendedDistributionKind, val parameters: Map<String, Double>, val mean: Double?, val variance: Double?)

class ExtendedProbabilityDistribution(val summary: ExtendedDistributionSummary) {
    fun density(x: Double): Double = when (summary.kind) {
        ExtendedDistributionKind.StudentT -> {
            val df = parameter("df"); exp(logGamma((df + 1) / 2) - logGamma(df / 2)) / sqrt(df * PI) * (1 + x * x / df).pow(-(df + 1) / 2)
        }
        ExtendedDistributionKind.ChiSquare -> {
            val df = parameter("df"); if (x <= 0) 0.0 else exp((df / 2 - 1) * ln(x) - x / 2 - df / 2 * ln(2.0) - logGamma(df / 2))
        }
        ExtendedDistributionKind.Gamma -> {
            val shape = parameter("shape"); val scale = parameter("scale"); if (x < 0) 0.0 else exp((shape - 1) * ln(x.coerceAtLeast(1e-300)) - x / scale - logGamma(shape) - shape * ln(scale))
        }
        ExtendedDistributionKind.Beta -> {
            val a = parameter("alpha"); val b = parameter("beta"); if (x !in 0.0..1.0) 0.0 else exp((a - 1) * ln(x.coerceAtLeast(1e-300)) + (b - 1) * ln((1 - x).coerceAtLeast(1e-300)) - logGamma(a) - logGamma(b) + logGamma(a + b))
        }
        ExtendedDistributionKind.Geometric -> {
            val p = parameter("p"); val k = x.toInt(); if (k < 1 || abs(x - k) > 1e-9) 0.0 else (1 - p).pow(k - 1) * p
        }
        ExtendedDistributionKind.Hypergeometric -> {
            val population = parameter("N").toInt(); val successes = parameter("K").toInt(); val draws = parameter("n").toInt(); val k = x.toInt()
            if (k < 0 || k > successes || k > draws || draws - k > population - successes) 0.0
            else exp(logChoose(successes, k) + logChoose(population - successes, draws - k) - logChoose(population, draws))
        }
    }

    fun cumulative(x: Double): Double = when (summary.kind) {
        ExtendedDistributionKind.Geometric, ExtendedDistributionKind.Hypergeometric -> {
            val lower = if (summary.kind == ExtendedDistributionKind.Geometric) 1 else 0
            (lower..x.toInt()).sumOf { density(it.toDouble()) }.coerceIn(0.0, 1.0)
        }
        else -> {
            val lower = when (summary.kind) { ExtendedDistributionKind.StudentT -> -12.0; ExtendedDistributionKind.Beta -> 0.0; else -> 0.0 }
            integrate(lower, x, 2_000, ::density).coerceIn(0.0, 1.0)
        }
    }

    private fun parameter(name: String) = summary.parameters.getValue(name)
}

object ExtendedDistributionEngine {
    fun create(kind: ExtendedDistributionKind, first: Double, second: Double = 1.0, third: Double = 1.0): ExtendedProbabilityDistribution {
        val summary = when (kind) {
            ExtendedDistributionKind.StudentT -> {
                require(first > 0); ExtendedDistributionSummary(kind, mapOf("df" to first), 0.0.takeIf { first > 1 }, (first / (first - 2)).takeIf { first > 2 })
            }
            ExtendedDistributionKind.ChiSquare -> {
                require(first > 0); ExtendedDistributionSummary(kind, mapOf("df" to first), first, 2 * first)
            }
            ExtendedDistributionKind.Gamma -> {
                require(first > 0 && second > 0); ExtendedDistributionSummary(kind, mapOf("shape" to first, "scale" to second), first * second, first * second * second)
            }
            ExtendedDistributionKind.Beta -> {
                require(first > 0 && second > 0); val sum = first + second
                ExtendedDistributionSummary(kind, mapOf("alpha" to first, "beta" to second), first / sum, first * second / (sum * sum * (sum + 1)))
            }
            ExtendedDistributionKind.Geometric -> {
                require(first in 0.0..1.0 && first > 0); ExtendedDistributionSummary(kind, mapOf("p" to first), 1 / first, (1 - first) / (first * first))
            }
            ExtendedDistributionKind.Hypergeometric -> {
                val population = first.toInt(); val successes = second.toInt(); val draws = third.toInt()
                require(population > 0 && successes in 0..population && draws in 0..population)
                val mean = draws * successes.toDouble() / population
                val variance = draws * successes.toDouble() / population * (1 - successes.toDouble() / population) * (population - draws).toDouble() / (population - 1).coerceAtLeast(1)
                ExtendedDistributionSummary(kind, mapOf("N" to population.toDouble(), "K" to successes.toDouble(), "n" to draws.toDouble()), mean, variance)
            }
        }
        return ExtendedProbabilityDistribution(summary)
    }
}

enum class RandomExperimentKind { Coin, Die, Cards, Bernoulli, MontyHall }
data class ExperimentOutcome(val label: String, val count: Int, val probability: Double)
data class RandomExperimentResult(val kind: RandomExperimentKind, val trials: Int, val seed: Long, val outcomes: List<ExperimentOutcome>, val expected: Map<String, Double>)

object RandomExperimentEngine {
    fun simulate(kind: RandomExperimentKind, trials: Int, seed: Long = 1L, probability: Double = .5): RandomExperimentResult {
        require(trials in 1..1_000_000 && probability in 0.0..1.0)
        val random = Random(seed); val counts = linkedMapOf<String, Int>()
        repeat(trials) {
            val label = when (kind) {
                RandomExperimentKind.Coin -> if (random.nextBoolean()) "Heads" else "Tails"
                RandomExperimentKind.Die -> (random.nextInt(6) + 1).toString()
                RandomExperimentKind.Cards -> listOf("Hearts", "Diamonds", "Clubs", "Spades")[random.nextInt(4)]
                RandomExperimentKind.Bernoulli -> if (random.nextDouble() < probability) "Success" else "Failure"
                RandomExperimentKind.MontyHall -> {
                    val prize = random.nextInt(3); val choice = random.nextInt(3)
                    if (prize != choice) "Switch wins" else "Stay wins"
                }
            }
            counts[label] = counts.getOrDefault(label, 0) + 1
        }
        val expected = when (kind) {
            RandomExperimentKind.Coin -> mapOf("Heads" to .5, "Tails" to .5)
            RandomExperimentKind.Die -> (1..6).associate { it.toString() to 1.0 / 6 }
            RandomExperimentKind.Cards -> listOf("Hearts", "Diamonds", "Clubs", "Spades").associateWith { .25 }
            RandomExperimentKind.Bernoulli -> mapOf("Success" to probability, "Failure" to 1 - probability)
            RandomExperimentKind.MontyHall -> mapOf("Switch wins" to 2.0 / 3, "Stay wins" to 1.0 / 3)
        }
        return RandomExperimentResult(kind, trials, seed, counts.map { ExperimentOutcome(it.key, it.value, it.value.toDouble() / trials) }, expected)
    }

    fun monteCarloPi(samples: Int, seed: Long = 1L): Pair<Double, Double> {
        require(samples > 0); val random = Random(seed); var inside = 0
        repeat(samples) { val x = random.nextDouble(); val y = random.nextDouble(); if (x * x + y * y <= 1) inside++ }
        val estimate = 4.0 * inside / samples; val standardError = 4 * sqrt((inside.toDouble() / samples) * (1 - inside.toDouble() / samples) / samples)
        return estimate to standardError
    }
}

data class BayesBranch(val hypothesis: String, val prior: Double, val evidenceLikelihood: Double, val posterior: Double)
data class BayesTree(val evidenceProbability: Double, val branches: List<BayesBranch>)

object ConditionalProbabilityEngine {
    fun bayes(priors: Map<String, Double>, likelihoods: Map<String, Double>): BayesTree {
        require(priors.isNotEmpty() && priors.keys == likelihoods.keys && priors.values.all { it in 0.0..1.0 } && likelihoods.values.all { it in 0.0..1.0 })
        require(abs(priors.values.sum() - 1) < 1e-9)
        val evidence = priors.entries.sumOf { it.value * likelihoods.getValue(it.key) }; require(evidence > 0)
        return BayesTree(evidence, priors.map { (name, prior) -> BayesBranch(name, prior, likelihoods.getValue(name), prior * likelihoods.getValue(name) / evidence) })
    }

    fun conditional(intersection: Double, condition: Double): Double {
        require(intersection in 0.0..1.0 && condition in 0.0..1.0 && condition > 0 && intersection <= condition)
        return intersection / condition
    }
}

data class CombinatoricsResult(val permutations: Long, val combinations: Long, val withReplacement: Long)
object CombinatoricsLab {
    fun calculate(n: Int, r: Int): CombinatoricsResult {
        require(n in 0..20 && r in 0..n)
        val permutations = ((n - r + 1)..n).fold(1L) { total, value -> total * value }
        val factorialR = (1..r).fold(1L) { total, value -> total * value }
        return CombinatoricsResult(permutations, permutations / factorialR, n.toDouble().pow(r).toLong())
    }
}

private fun integrate(from: Double, to: Double, steps: Int, f: (Double) -> Double): Double {
    if (to <= from) return 0.0
    val h = (to - from) / steps; var sum = f(from) + f(to)
    for (i in 1 until steps) sum += (if (i % 2 == 0) 2 else 4) * f(from + i * h)
    return sum * h / 3
}
private fun logChoose(n: Int, r: Int) = if (r !in 0..n) Double.NEGATIVE_INFINITY else logGamma(n + 1.0) - logGamma(r + 1.0) - logGamma(n - r + 1.0)
private fun logGamma(value: Double): Double {
    val coefficients = doubleArrayOf(676.5203681218851, -1259.1392167224028, 771.3234287776531, -176.6150291621406, 12.507343278686905, -.13857109526572012, 9.984369578019571e-6, 1.5056327351493116e-7)
    if (value < .5) return ln(PI) - ln(abs(kotlin.math.sin(PI * value))) - logGamma(1 - value)
    val z = value - 1; var x = .9999999999998099
    coefficients.forEachIndexed { index, coefficient -> x += coefficient / (z + index + 1) }
    val t = z + coefficients.size - .5
    return .5 * ln(2 * PI) + (z + .5) * ln(t) - t + ln(x)
}
