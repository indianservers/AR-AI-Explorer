package com.indianservers.aiexplorer.mathdictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MathDictionarySeedTest {
    private val seedText: String
        get() = File("src/main/assets/math_dictionary_terms.json").readText()

    @Test
    fun seedContainsExactly600UniqueTerms() {
        val keys = Regex("\"term_key\"\\s*:\\s*\"([^\"]+)\"").findAll(seedText).map { it.groupValues[1] }.toList()
        val normalizedWords = Regex("\"normalized_word\"\\s*:\\s*\"([^\"]+)\"").findAll(seedText).map { it.groupValues[1] }.toList()

        assertEquals(600, keys.size)
        assertEquals(600, keys.distinct().size)
        assertEquals(600, normalizedWords.distinct().size)
    }

    @Test
    fun seedCoversRequiredCategoryBreadth() {
        val categories = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"").findAll(seedText).map { it.groupValues[1] }.toSet()

        listOf(
            "Numbers and Number Systems",
            "Arithmetic",
            "Algebra",
            "Geometry",
            "Calculus",
            "Linear Algebra",
            "Probability",
            "Statistics",
            "Logic",
            "Number Theory",
            "Discrete Mathematics",
            "Mathematical Analysis",
        ).forEach { category ->
            assertTrue("Missing category $category", category in categories)
        }
    }

    @Test
    fun relatedTermKeysResolveToSeedTerms() {
        val keys = Regex("\"term_key\"\\s*:\\s*\"([^\"]+)\"").findAll(seedText).map { it.groupValues[1] }.toSet()
        val related = Regex("\"related_term_keys\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
            .findAll(seedText)
            .flatMap { match -> Regex("\"([^\"]+)\"").findAll(match.groupValues[1]).map { it.groupValues[1] } }
            .toList()

        assertTrue(related.isNotEmpty())
        assertTrue(related.all { it in keys })
    }

    @Test
    fun searchNormalizationTrimsCasesAndCollapsesSpaces() {
        assertEquals("quadratic equation", MathDictionarySearch.normalize("  Quadratic   Equation  "))
    }
}
