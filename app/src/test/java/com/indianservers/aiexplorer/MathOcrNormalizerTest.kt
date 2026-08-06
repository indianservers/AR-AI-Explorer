package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.input.MathOcrNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathOcrNormalizerTest {
    @Test
    fun normalizesCommonPrintedMathSymbolsWithoutGuessingVariables() {
        val result = MathOcrNormalizer.normalize("x² − 4×x + 3 = 0")

        assertEquals("x ^ 2 - 4 * x + 3 = 0", result.editableText)
        assertTrue(result.changes.any { it.contains("squared") })
        assertTrue(result.changes.any { it.contains("multiplication") })
    }

    @Test
    fun addsFunctionBracketsAndPreservesMultipleLines() {
        val result = MathOcrNormalizer.normalize("Solve\nsin 30 = 1 ÷ 2")

        assertEquals("Solve\nsin(30) = 1 / 2", result.editableText)
    }
}
