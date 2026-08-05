package com.indianservers.aiexplorer.probabilitystats

import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityPhase1Test {
    @Test
    fun pastedDatasetAcceptsCsvTabsSpacesAndLines() {
        val values = ProbabilityPhase1DatasetEngine.parsePasted("1, 2\n3\t4;5 6")
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), values)
    }

    @Test
    fun linkedSummaryAndHistogramRemainMathematicallyConsistent() {
        val values = listOf(2.0, 4.0, 4.0, 6.0, 9.0)
        val summary = AdvancedStatisticsEngine.summarize(values)
        val histogram = AdvancedStatisticsEngine.histogram(values, 3)

        assertEquals(5.0, summary.mean, 1e-12)
        assertEquals(values.size, histogram.sumOf { it.count })
        assertTrue(ProbabilityPhase1DatasetEngine.accessibleHistogramSummary(histogram).contains("3 bins"))
    }
}
