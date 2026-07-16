package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.InferentialStatistics
import com.indianservers.aiexplorer.core.StatisticsCurriculum
import com.indianservers.aiexplorer.core.StatisticsStudyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedStatisticsTest {
    @Test
    fun calculatesCenterSpreadShapeAndRobustSummaries() {
        val result = AdvancedStatisticsEngine.summarize(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

        assertEquals(3.0, result.mean, 1e-12)
        assertEquals(3.0, result.median, 1e-12)
        assertTrue(result.modes.isEmpty())
        assertEquals(2.0, result.fiveNumber.firstQuartile, 1e-12)
        assertEquals(4.0, result.fiveNumber.thirdQuartile, 1e-12)
        assertEquals(2.0, result.populationVariance, 1e-12)
        assertEquals(2.5, result.sampleVariance, 1e-12)
        assertEquals(0.0, result.skewness!!, 1e-12)
    }

    @Test
    fun detectsModesOutliersAndBuildsFrequencyModels() {
        val values = listOf(10.0, 11.0, 11.0, 12.0, 12.0, 12.0, 13.0, 14.0, 40.0)
        val result = AdvancedStatisticsEngine.summarize(values)
        val histogram = AdvancedStatisticsEngine.histogram(values, 5)
        val frequency = AdvancedStatisticsEngine.frequencyTable(values)

        assertEquals(listOf(12.0), result.modes)
        assertEquals(listOf(40.0), result.outliers)
        assertEquals(values.size, histogram.sumOf { it.count })
        assertEquals(values.size, frequency.last().cumulativeFrequency)
        assertEquals(1.0, histogram.sumOf { it.relativeFrequency }, 1e-12)
    }

    @Test
    fun createsEcdfAndNormalQqChartModels() {
        val values = listOf(2.0, 4.0, 6.0, 8.0)
        val ecdf = AdvancedStatisticsEngine.empiricalCdf(values)
        val qq = AdvancedStatisticsEngine.normalQq(values)

        assertEquals(.25, ecdf.first().y, 1e-12)
        assertEquals(1.0, ecdf.last().y, 1e-12)
        assertEquals(values.size, qq.size)
        assertTrue(qq.zipWithNext().all { it.first.x < it.second.x })
    }

    @Test
    fun performsConfidenceIntervalsAndTTests() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val interval = InferentialStatistics.meanConfidenceInterval(values)
        val test = InferentialStatistics.oneSampleT(values, 0.0)

        assertEquals(3.0, interval.estimate, 1e-12)
        assertEquals(1.0368, interval.lower, 2e-3)
        assertEquals(4.9632, interval.upper, 2e-3)
        assertEquals(4.24264, test.statistic, 1e-4)
        assertEquals(.0132, test.pValueTwoSided, 2e-3)
        assertTrue(test.rejectAtFivePercent)
    }

    @Test
    fun calculatesPearsonSpearmanAndWelchStatistics() {
        val correlation = InferentialStatistics.correlation(listOf(1.0, 2.0, 3.0, 4.0), listOf(2.0, 4.0, 6.0, 8.0))
        val welch = InferentialStatistics.welchT(listOf(10.0, 11.0, 12.0, 13.0), listOf(20.0, 21.0, 22.0, 23.0))

        assertEquals(1.0, correlation.pearson, 1e-12)
        assertEquals(1.0, correlation.spearman, 1e-12)
        assertTrue(welch.pValueTwoSided < .001)
    }

    @Test
    fun curriculumCoversSchoolThroughPostgraduate() {
        assertEquals(StatisticsStudyLevel.entries.toSet(), StatisticsCurriculum.lessons.keys)
        assertTrue(StatisticsCurriculum.lessons.getValue(StatisticsStudyLevel.Postgraduate).any { "Bayesian" in it.title })
        assertTrue(StatisticsCurriculum.lessons.values.flatten().all { it.concepts.isNotEmpty() && it.lab.isNotBlank() && it.outcome.isNotBlank() })
    }
}
