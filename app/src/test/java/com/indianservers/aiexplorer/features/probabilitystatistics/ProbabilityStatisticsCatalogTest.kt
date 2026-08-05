package com.indianservers.aiexplorer.features.probabilitystatistics

import com.indianservers.aiexplorer.features.probabilitystatistics.calculation.StatisticalTestGuide
import com.indianservers.aiexplorer.features.probabilitystatistics.data.ProbabilityStatisticsCatalog
import com.indianservers.aiexplorer.features.probabilitystatistics.models.AnalysisObjective
import com.indianservers.aiexplorer.features.probabilitystatistics.models.GroupStructure
import com.indianservers.aiexplorer.features.probabilitystatistics.models.OutcomeType
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsLearningLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityStatisticsCatalogTest {
    @Test
    fun exposesAllRequiredCategoriesWithStableIdsAndReachableTopics() {
        assertEquals(23, ProbabilityStatisticsCatalog.categories.size)
        assertEquals(23, ProbabilityStatisticsCatalog.categories.map { it.id }.distinct().size)
        assertTrue(ProbabilityStatisticsCatalog.categories.all { category ->
            category.topicIds.isNotEmpty() && category.topicIds.all { ProbabilityStatisticsCatalog.topic(it) != null }
        })
    }

    @Test
    fun includesTenFullyAuthoredCoreTopicsAndBinomialReference() {
        val required = setOf(
            "mean-median-mode",
            "variance-standard-deviation",
            "basic-probability",
            "conditional-probability",
            "binomial-distribution",
            "normal-distribution",
            "sampling-distributions",
            "confidence-intervals",
            "hypothesis-testing",
            "regression-correlation",
        )
        required.forEach { id ->
            val topic = ProbabilityStatisticsCatalog.topic(id)
            assertNotNull(topic)
            assertTrue(topic!!.lessonSteps.size >= 4)
            assertTrue(topic.formulas.isNotEmpty())
            assertTrue(topic.examples.isNotEmpty())
            assertTrue(topic.practice.isNotEmpty())
            assertTrue(topic.applications.isNotEmpty())
        }
        assertTrue(ProbabilityStatisticsCatalog.topic("binomial-distribution")!!.lessonSteps.any { "fixed n" in it })
    }

    @Test
    fun searchUnderstandsAlternativeTerminologyAndLevelFiltering() {
        assertEquals("normal-distribution", ProbabilityStatisticsCatalog.search("bell curve", StatisticsLearningLevel.Postgraduate).single().id)
        assertEquals("mean-median-mode", ProbabilityStatisticsCatalog.search("average", StatisticsLearningLevel.Foundation).single().id)
        assertTrue(ProbabilityStatisticsCatalog.search("p value", StatisticsLearningLevel.Foundation).isEmpty())
        assertEquals("hypothesis-testing", ProbabilityStatisticsCatalog.search("p value", StatisticsLearningLevel.Postgraduate).single().id)
    }

    @Test
    fun recommendationsFollowPrerequisiteRelationships() {
        val next = ProbabilityStatisticsCatalog.nextTopics("binomial-distribution").map { it.id }
        assertTrue(next.isNotEmpty())
        assertTrue(next.size <= 3)
        assertTrue("normal-distribution" in next)
    }

    @Test
    fun testGuideSelectsParametricAndNonParametricAlternatives() {
        val parametric = StatisticalTestGuide.recommend(
            AnalysisObjective.Compare,
            OutcomeType.Quantitative,
            GroupStructure.TwoIndependent,
            true,
        )
        val robust = StatisticalTestGuide.recommend(
            AnalysisObjective.Compare,
            OutcomeType.Quantitative,
            GroupStructure.TwoIndependent,
            false,
        )

        assertEquals("Welch independent-samples t-test", parametric.method)
        assertEquals("Mann-Whitney U test", robust.method)
        assertTrue(parametric.assumptions.isNotEmpty())
        assertTrue(parametric.caution.isNotBlank())
    }
}
