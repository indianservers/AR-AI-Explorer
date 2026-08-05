package com.indianservers.aiexplorer.features.probabilitystatistics

import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.ProbabilityStatisticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityStatisticsEngineTest {
    @Test
    fun calculatesDescriptiveStatisticsAndSeparatesSampleVariance() {
        val result = ProbabilityStatisticsEngine.descriptive(listOf(2.0, 3.0, 3.0, 4.0, 18.0)).getOrThrow()

        assertEquals(6.0, result.mean, 1e-12)
        assertEquals(3.0, result.median, 1e-12)
        assertEquals(listOf(3.0), result.modes)
        assertEquals(16.0, result.range, 1e-12)
        assertTrue(result.sampleVariance!! > result.populationVariance)
    }

    @Test
    fun percentileUsesInterpolatedR7Positions() {
        val values = listOf(0.0, 10.0, 20.0, 30.0, 40.0)

        assertEquals(10.0, ProbabilityStatisticsEngine.percentile(values, .25).getOrThrow(), 1e-12)
        assertEquals(20.0, ProbabilityStatisticsEngine.percentile(values, .50).getOrThrow(), 1e-12)
        assertEquals(30.0, ProbabilityStatisticsEngine.percentile(values, .75).getOrThrow(), 1e-12)
    }

    @Test
    fun handlesSingleValueAndRejectsEmptyOrNonFiniteData() {
        val single = ProbabilityStatisticsEngine.descriptive(listOf(7.0)).getOrThrow()

        assertEquals(7.0, single.mean, 0.0)
        assertEquals(0.0, single.populationVariance, 0.0)
        assertEquals(null, single.sampleVariance)
        assertTrue(ProbabilityStatisticsEngine.descriptive(emptyList()).isFailure)
        assertTrue(ProbabilityStatisticsEngine.descriptive(listOf(Double.NaN)).isFailure)
    }

    @Test
    fun calculatesWeightedMeanCombinatoricsAndRegression() {
        assertEquals(16.0, ProbabilityStatisticsEngine.weightedMean(listOf(10.0, 20.0), listOf(2.0, 3.0)).getOrThrow(), 1e-12)
        assertEquals(120L, ProbabilityStatisticsEngine.permutations(6, 3).getOrThrow())
        assertEquals(20L, ProbabilityStatisticsEngine.combinations(6, 3).getOrThrow())
        val regression = ProbabilityStatisticsEngine.regression(listOf(0.0, 1.0, 2.0), listOf(1.0, 3.0, 5.0)).getOrThrow()
        assertEquals(1.0, regression.intercept, 1e-12)
        assertEquals(2.0, regression.slope, 1e-12)
        assertEquals(1.0, regression.rSquared, 1e-12)
    }

    @Test
    fun computesKnownBinomialProbabilityAndMoments() {
        val result = ProbabilityStatisticsEngine.binomial(n = 5, p = .5, x = 3).getOrThrow()

        assertEquals(.3125, result.pmf, 1e-12)
        assertEquals(.8125, result.cdf, 1e-12)
        assertEquals(2.5, result.mean, 1e-12)
        assertEquals(1.25, result.variance, 1e-12)
    }

    @Test
    fun binomialValidationRejectsImpossibleInputs() {
        assertTrue(ProbabilityStatisticsEngine.binomial(0, .5, 0).isFailure)
        assertTrue(ProbabilityStatisticsEngine.binomial(5, 1.2, 2).isFailure)
        assertTrue(ProbabilityStatisticsEngine.binomial(5, .5, 6).isFailure)
    }

    @Test
    fun seededSimulationIsReproducibleAndApproachesTheory() {
        val first = ProbabilityStatisticsEngine.simulateBinomial(10, .5, 5, 10_000, 91).getOrThrow()
        val second = ProbabilityStatisticsEngine.simulateBinomial(10, .5, 5, 10_000, 91).getOrThrow()

        assertEquals(first.successCounts, second.successCounts)
        assertEquals(first.empiricalProbability, second.empiricalProbability, 0.0)
        assertEquals(first.theoreticalProbability, first.empiricalProbability, .025)
    }

    @Test
    fun datasetInspectionFindsInvalidTokensAndOutliersWithoutCrashing() {
        val quality = ProbabilityStatisticsEngine.inspectDataset("1, 2, 2, 3, missing, 100")

        assertEquals(listOf("missing"), quality.invalidTokens)
        assertTrue(100.0 in quality.outliers)
        assertFalse(quality.validValues.any { !it.isFinite() })
    }

    @Test
    fun confidenceIntervalIsFiniteAndCentredOnMean() {
        val interval = ProbabilityStatisticsEngine.meanConfidenceInterval(listOf(8.0, 9.0, 10.0, 11.0, 12.0)).getOrThrow()

        assertTrue(interval.first.isFinite() && interval.second.isFinite())
        assertEquals(10.0, (interval.first + interval.second) / 2, 1e-10)
    }
}
