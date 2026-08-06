package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.BinomialDistribution
import com.indianservers.aiexplorer.core.CompareModeEngine
import com.indianservers.aiexplorer.core.ComparisonAttribute
import com.indianservers.aiexplorer.core.ComparisonItem
import com.indianservers.aiexplorer.core.NormalDistribution
import com.indianservers.aiexplorer.core.VisualProofCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareModeTest {
    @Test
    fun alignsSharedAndDifferentPropertiesByLabel() {
        val left = ComparisonItem(
            "a",
            "First",
            "x + 1",
            listOf(ComparisonAttribute("Type", "Explicit"), ComparisonAttribute("Degree", "1")),
        )
        val right = ComparisonItem(
            "b",
            "Second",
            "x^2",
            listOf(ComparisonAttribute("Degree", "2"), ComparisonAttribute("Type", "Explicit")),
        )

        val report = CompareModeEngine.compare(left, right)

        assertEquals(listOf("Type", "Degree"), report.rows.map { it.label })
        assertTrue(report.rows.first { it.label == "Type" }.matches)
        assertFalse(report.rows.first { it.label == "Degree" }.matches)
        assertEquals(1, report.sharedCount)
        assertEquals(1, report.differenceCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsComparingAnItemWithItself() {
        val item = ComparisonItem("same", "Same", "x", emptyList())
        CompareModeEngine.compare(item, item)
    }

    @Test
    fun proofComparisonUsesVerifiedCatalogMetadata() {
        val first = CompareModeEngine.proof(VisualProofCatalog.labs[0])
        val second = CompareModeEngine.proof(VisualProofCatalog.labs[1])
        val report = CompareModeEngine.compare(first, second)

        assertTrue(report.rows.any { it.label == "Method" && it.left.isNotBlank() && it.right.isNotBlank() })
        assertTrue(report.rows.any { it.label == "Assumptions" })
    }

    @Test
    fun distributionComparisonUsesCalculatedMomentsAndQuantiles() {
        val normal = CompareModeEngine.distribution(NormalDistribution())
        val binomial = CompareModeEngine.distribution(BinomialDistribution(10, .5))
        val report = CompareModeEngine.compare(normal, binomial)

        assertEquals("Continuous", report.rows.first { it.label == "Type" }.left)
        assertEquals("Discrete", report.rows.first { it.label == "Type" }.right)
        assertTrue(report.rows.first { it.label == "Mean" }.right.startsWith("5"))
        assertTrue(report.rows.any { it.label == "90th percentile" })
    }
}
