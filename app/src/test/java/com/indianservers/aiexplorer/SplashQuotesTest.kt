package com.indianservers.aiexplorer

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashQuotesTest {
    @Test
    fun quoteCatalogContainsAllUniqueRequestedLines() {
        val allQuotes = standardSplashQuotes + featuredSplashQuotes

        assertEquals(55, allQuotes.size)
        assertEquals(55, allQuotes.distinct().size)
    }

    @Test
    fun featuredQuotesReceiveFourEntriesInWeightedPool() {
        val weighted = weightedSplashQuotes()

        assertEquals(70, weighted.size)
        standardSplashQuotes.forEach { assertEquals(1, weighted.count { candidate -> candidate == it }) }
        featuredSplashQuotes.forEach { assertEquals(4, weighted.count { candidate -> candidate == it }) }
    }

    @Test
    fun randomQuoteAlwaysComesFromCatalog() {
        val catalog = (standardSplashQuotes + featuredSplashQuotes).toSet()

        repeat(500) { seed ->
            assertTrue(randomSplashQuote(Random(seed)) in catalog)
        }
    }
}
