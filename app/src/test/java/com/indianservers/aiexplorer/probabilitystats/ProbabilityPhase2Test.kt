package com.indianservers.aiexplorer.probabilitystats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityPhase2Test {
    @Test
    fun vennRegionsRespectInclusionExclusionAndConditionalProbability() {
        val result = Phase2ProbabilityEngine.calculateVenn(a = .6, b = .5, intersection = .3)

        assertTrue(result.valid)
        assertEquals(.8, result.union, 1e-12)
        assertEquals(.2, result.neither, 1e-12)
        assertEquals(.6, result.conditionalAGivenB ?: 0.0, 1e-12)
        assertTrue(result.independent)
    }

    @Test
    fun impossibleVennInputsReturnStructuredValidation() {
        val result = Phase2ProbabilityEngine.calculateVenn(a = .2, b = .3, intersection = .4)
        assertFalse(result.valid)
        assertTrue(result.error.orEmpty().contains("intersection"))
    }

    @Test
    fun seededSamplingIsReproducibleAndLargerSamplesReduceStandardError() {
        val small = Phase2SamplingEngine.simulate(Phase2Population.RightSkewed, sampleSize = 4, repetitions = 1_000, seed = 91)
        val repeat = Phase2SamplingEngine.simulate(Phase2Population.RightSkewed, sampleSize = 4, repetitions = 1_000, seed = 91)
        val large = Phase2SamplingEngine.simulate(Phase2Population.RightSkewed, sampleSize = 40, repetitions = 1_000, seed = 91)

        assertEquals(small.sampleMeans, repeat.sampleMeans)
        assertTrue(large.standardError < small.standardError)
    }

    @Test
    fun everyExperimentTheoreticalModelSumsToOne() {
        Phase2ExperimentKind.entries.forEach { kind ->
            assertEquals(1.0, Phase2ProbabilityEngine.theoretical(kind).values.sum(), 1e-12)
        }
    }

    @Test
    fun combinatoricsUsesExactIntegersBeyondLongRange() {
        val result = Phase2CombinatoricsEngine.calculate(n = 20, r = 20)
        assertEquals("2432902008176640000", result.factorial.toString())
        assertEquals("104857600000000000000000000", result.orderedWithReplacement.toString())
        assertEquals("1", result.combinations.toString())
    }
}
