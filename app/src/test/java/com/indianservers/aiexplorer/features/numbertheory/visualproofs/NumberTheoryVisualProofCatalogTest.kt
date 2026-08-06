package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.data.NumberTheoryVisualProofCatalog
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryVisualProofCatalogTest {
    @Test
    fun `phase roadmap has stable unique coverage`() {
        val topics = NumberTheoryVisualProofCatalog.topics
        assertEquals(40, topics.size)
        assertEquals(topics.size, topics.map { it.id }.distinct().size)
        assertEquals(12, topics.count { it.completedInPhase == 1 })
        assertEquals(18, topics.count { it.completedInPhase == 2 })
        assertEquals(10, topics.count { it.completedInPhase == 3 })
        assertEquals(40, NumberTheoryVisualProofCatalog.completedTopics.size)
        assertEquals(10, NumberTheoryProofCategory.entries.size)
        assertTrue(topics.all { it.completedInPhase in 1..3 })
    }

    @Test
    fun `all completed topics are fully authored`() {
        NumberTheoryVisualProofCatalog.completedTopics.forEach { topic ->
            assertTrue("${topic.id} needs at least four steps", topic.steps.size >= 4)
            assertTrue("${topic.id} needs a proof explanation", topic.whyItWorks.size >= 3)
            assertTrue(topic.parameters.isNotEmpty())
            assertTrue(topic.practice.options.size >= 3)
            assertTrue(topic.practice.answerIndex in topic.practice.options.indices)
            assertTrue(topic.statement.isNotBlank())
            assertTrue(topic.spokenStatement.isNotBlank())
        }
    }

    @Test
    fun `required launch proofs are present`() {
        val required = setOf(
            "natural-sum", "odd-sum", "triangular-numbers", "consecutive-squares",
            "arithmetic-sum", "divisibility-3", "factor-rectangles", "lcm-cycles",
            "euclidean-algorithm", "unique-factorization", "sieve", "modular-clock",
            "even-sum", "square-odd-difference", "consecutive-integer-sum",
            "divisibility-2", "divisibility-9", "divisibility-4", "divisibility-8",
            "divisibility-5-10", "divisibility-11", "parity-last-digit",
            "multiples-line", "gcd-grouping", "gcd-lcm-product", "prime-building-blocks",
            "composite-sqrt", "euclid-primes", "prime-gaps", "twin-primes",
        )
        required.forEach { assertNotNull(NumberTheoryVisualProofCatalog.topic(it)) }
    }

    @Test
    fun `search recognizes plain language aliases and respects level`() {
        assertEquals(
            "natural-sum",
            NumberTheoryVisualProofCatalog.search("sum 1 to n", NumberTheoryProofLevel.School).single().id,
        )
        assertTrue(
            NumberTheoryVisualProofCatalog.search("clock", NumberTheoryProofLevel.Undergraduate)
                .any { it.id == "modular-clock" },
        )
        assertTrue(NumberTheoryVisualProofCatalog.search("not a real proof", NumberTheoryProofLevel.Undergraduate).isEmpty())
    }
}
