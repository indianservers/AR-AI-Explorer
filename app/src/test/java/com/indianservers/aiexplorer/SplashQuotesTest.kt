package com.indianservers.aiexplorer

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashQuotesTest {
    @Test
    fun quoteCatalogContainsAllUniqueRequestedLines() {
        assertEquals(30, standardSplashQuotes.size)
        assertEquals(30, standardSplashQuotes.distinct().size)
        assertTrue("The Universe Speaks Mathematics" in standardSplashQuotes)
        assertTrue("See the Universe Through Mathematics" in standardSplashQuotes)
    }

    @Test
    fun requestedQuotesReceiveEqualEntriesInWeightedPool() {
        val weighted = weightedSplashQuotes()

        assertEquals(30, weighted.size)
        standardSplashQuotes.forEach { assertEquals(1, weighted.count { candidate -> candidate == it }) }
    }

    @Test
    fun randomQuoteAlwaysComesFromCatalog() {
        val catalog = standardSplashQuotes.toSet()

        repeat(500) { seed ->
            assertTrue(randomSplashQuote(Random(seed)) in catalog)
        }
    }
}
