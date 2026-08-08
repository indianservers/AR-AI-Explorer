package com.indianservers.aiexplorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Geometry2DControlsTest {
    @Test
    fun rotationSnapsNearFifteenDegreeIncrement() {
        val result = snapGeometryRotation(43.4)

        assertTrue(result.snapped)
        assertEquals(45.0, result.angle, 0.0)
    }

    @Test
    fun rotationRemainsContinuousAwayFromSnapPoint() {
        val result = snapGeometryRotation(41.0)

        assertFalse(result.snapped)
        assertEquals(41.0, result.angle, 0.0)
    }

    @Test
    fun rotationSnapsAcrossNegativeAngles() {
        val result = snapGeometryRotation(-14.0)

        assertTrue(result.snapped)
        assertEquals(-15.0, result.angle, 0.0)
    }

    @Test
    fun invalidAnglesAreNeverReportedAsSnapped() {
        val result = snapGeometryRotation(Double.NaN)

        assertFalse(result.snapped)
        assertTrue(result.angle.isNaN())
    }
}
