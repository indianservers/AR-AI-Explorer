package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceInputInterpreterTest {
    @Test
    fun acceptsBareAndExplicitSurfaceEquations() {
        assertEquals(
            "z = x^2 + y^2",
            SurfaceInputInterpreter.explicit("x² + y²").getOrThrow().canonicalEquation,
        )
        assertEquals(
            "z = sin(x) + cos(y)",
            SurfaceInputInterpreter.explicit("z = sin(x) + cos(y)").getOrThrow().canonicalEquation,
        )
        assertEquals(
            "z = x^2-y^2",
            SurfaceInputInterpreter.explicit("x^2-y^2 = z").getOrThrow().canonicalEquation,
        )
    }

    @Test
    fun acceptsFunctionNotationAndNormalizesInputSymbols() {
        val interpretation = SurfaceInputInterpreter.explicit("f(x,y) = 2×x − y³").getOrThrow()

        assertEquals("z = 2*x - y^3", interpretation.canonicalEquation)
        assertEquals("2*x - y^3", interpretation.expression)
    }

    @Test
    fun rejectsNonExplicitAndIncompleteEquationsWithGuidance() {
        assertTrue(SurfaceInputInterpreter.explicit("x^2 + y^2 = 9").isFailure)
        assertTrue(SurfaceInputInterpreter.explicit("z = ").isFailure)
        assertTrue(SurfaceInputInterpreter.explicit("").isFailure)
    }
}
