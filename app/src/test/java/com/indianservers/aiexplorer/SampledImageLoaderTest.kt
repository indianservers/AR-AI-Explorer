package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.input.SampledImageLoader
import org.junit.Assert.assertEquals
import org.junit.Test

class SampledImageLoaderTest {
    @Test
    fun sampleSizeKeepsBothDimensionsWithinBounds() {
        assertEquals(1, SampledImageLoader.calculateSampleSize(1200, 800))
        assertEquals(2, SampledImageLoader.calculateSampleSize(2400, 1600))
        assertEquals(8, SampledImageLoader.calculateSampleSize(12000, 1000))
        assertEquals(4, SampledImageLoader.calculateSampleSize(4000, 6000))
    }

    @Test
    fun invalidDimensionsFallBackToAValidSampleSize() {
        assertEquals(1, SampledImageLoader.calculateSampleSize(0, 100))
        assertEquals(1, SampledImageLoader.calculateSampleSize(100, 0))
        assertEquals(1, SampledImageLoader.calculateSampleSize(100, 100, 0, 100))
    }
}
