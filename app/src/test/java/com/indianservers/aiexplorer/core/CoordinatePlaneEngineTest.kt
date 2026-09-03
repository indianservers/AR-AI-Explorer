package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatePlaneEngineTest {
    @Test
    fun analysesOrdinaryLineExactly() {
        val result = requireNotNull(CoordinatePlaneEngine.analyse(Vec2(-2.0, 1.0), Vec2(4.0, 4.0)))

        assertEquals(6.0, result.delta.x, 1e-9)
        assertEquals(3.0, result.delta.y, 1e-9)
        assertEquals(0.5, result.slope ?: Double.NaN, 1e-9)
        assertEquals(Vec2(1.0, 2.5), result.midpoint)
        assertEquals("y = 0.5x + 2", result.equation())
        assertEquals("y = -2x + 4.5", result.perpendicularEquation(result.midpoint))
    }

    @Test
    fun handlesVerticalAndHorizontalLines() {
        val vertical = requireNotNull(CoordinatePlaneEngine.analyse(Vec2(3.0, -2.0), Vec2(3.0, 5.0)))
        val horizontal = requireNotNull(CoordinatePlaneEngine.analyse(Vec2(-4.0, 2.0), Vec2(6.0, 2.0)))

        assertNull(vertical.slope)
        assertEquals("x = 3", vertical.equation())
        assertEquals("y = 1.5", vertical.perpendicularEquation(Vec2(8.0, 1.5)))
        assertEquals(0.0, horizontal.slope ?: Double.NaN, 1e-9)
        assertEquals("y = 2", horizontal.equation())
        assertEquals("x = 1", horizontal.perpendicularEquation(Vec2(1.0, 9.0)))
    }

    @Test
    fun rejectsCoincidentPointsAndSnapsToHalfUnits() {
        assertNull(CoordinatePlaneEngine.analyse(Vec2(1.0, 1.0), Vec2(1.0, 1.0)))
        assertEquals(Vec2(1.5, -2.0), CoordinatePlaneEngine.snap(Vec2(1.31, -1.76)))
    }
}
