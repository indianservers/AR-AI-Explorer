package com.indianservers.aiexplorer.mathworkspace.vector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VectorMathTest {
    @Test fun requestedVectorOperationsUseEnteredCoordinates() {
        val a = MathVector3(2.0, 1.0)
        val b = MathVector3(-1.0, 4.0)
        assertEquals(MathVector3(1.0, 5.0), VectorMath.calculate(VectorOperation.Add, a, b, 1.0, 1.0).vector)
        assertEquals(2.0, VectorMath.calculate(VectorOperation.Dot, a, b, 1.0, 1.0).scalar!!, 1e-12)
        assertEquals(kotlin.math.sqrt(5.0), VectorMath.calculate(VectorOperation.Magnitude, a, b, 1.0, 1.0).scalar!!, 1e-12)
    }

    @Test fun coordinatesAreNotClampedAndZeroVectorAngleIsUndefined() {
        val large = MathVector3(2500.0, -840.0)
        assertEquals(2500.0, large.x, 0.0)
        assertEquals(-840.0, large.y, 0.0)
        val small = MathVector3(.0025, -.0008)
        assertEquals(.0025, small.x, 0.0)
        assertNull(MathVector3(0.0, 0.0).angleDegrees(MathVector3(1.0, 0.0)))
    }
}
