package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class MathematicalArtEngineTest {
    @Test
    fun polarArtReusesInteractiveTrigSamplingAndExportsGraphSource() {
        val art = MathematicalArtEngine.polar(PolarCurveType.Rose, parameter = 5.0, scale = 2.0)

        assertEquals(721, art.points.size)
        assertEquals("r=2.0*cos(5.0*t)", art.graphSource)
        assertTrue(art.points.maxOf { kotlin.math.hypot(it.x, it.y) } <= 2.0 + 1e-10)
    }

    @Test
    fun lissajousUsesSharedParametricGraphSamplerAndCloses() {
        val art = MathematicalArtEngine.lissajous(3, 2, phaseRadians = 0.0, samples = 480)

        assertNotNull(art.graphSource)
        assertEquals(481, art.points.size)
        assertTrue(art.points.first().distanceTo(art.points.last()) < 1e-9)
    }

    @Test
    fun kochAndSierpinskiGrowByTheirSimilarityRules() {
        for (depth in 0..5) {
            val koch = MathematicalArtEngine.koch(depth)
            assertEquals(4.0.pow(depth).toInt(), koch.points.size - 1)
            assertEquals(3.0.pow(depth).toInt(), MathematicalArtEngine.sierpinski(depth).size)
        }
    }

    @Test
    fun mandelbrotKeepsOriginBoundedAndEscapesPositiveOne() {
        val art = MathematicalArtEngine.mandelbrot(columns = 101, rows = 101, maximumIterations = 80, center = Vec2(0.0, 0.0), span = 2.0)
        val origin = art.cells.first { it.column == 50 && it.row == 50 }
        val positiveOne = art.cells.first { it.column == 100 && it.row == 50 }

        assertEquals(80, origin.iterations)
        assertTrue(positiveOne.iterations < 5)
        assertEquals(101 * 101, art.cells.size)
    }

    @Test
    fun radialCopiesPreserveDistanceAndRotateSeed() {
        val copies = MathematicalArtEngine.radialCopies(listOf(Vec2(1.0, 0.0)), 4)

        assertEquals(4, copies.size)
        copies.flatten().forEach { assertEquals(1.0, kotlin.math.hypot(it.x, it.y), 1e-12) }
        assertTrue(abs(copies[1][0].x) < 1e-12)
        assertEquals(1.0, copies[1][0].y, 1e-12)
    }
}
