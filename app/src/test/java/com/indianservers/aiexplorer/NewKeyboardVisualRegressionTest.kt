package com.indianservers.aiexplorer

import androidx.compose.ui.text.AnnotatedString
import com.indianservers.aiexplorer.input.IntentAwareMathVisualTransformation
import org.junit.Assert.*
import org.junit.Test

class NewKeyboardVisualRegressionTest {
    @Test fun nestedBracketPairsHaveMatchingDistinctColors() {
        val source = "(x+(y+1))"
        val visual = IntentAwareMathVisualTransformation().filter(AnnotatedString(source)).text
        fun colorAt(offset: Int) = visual.spanStyles.last { offset >= it.start && offset < it.end }.item.color
        assertEquals(colorAt(0), colorAt(8))
        assertEquals(colorAt(3), colorAt(7))
        assertNotEquals(colorAt(0), colorAt(3))
        assertNotEquals(colorAt(1), colorAt(6))
    }

    @Test fun coloringPreservesSuperscriptAndEveryCursorOffset() {
        val source = "y=(x+1)^(2)"
        val visual = IntentAwareMathVisualTransformation().filter(AnnotatedString(source))
        assertTrue(visual.text.spanStyles.isNotEmpty())
        for (offset in 0..source.length) {
            assertTrue(visual.offsetMapping.originalToTransformed(offset) in 0..visual.text.length)
        }
        for (offset in 0..visual.text.length) {
            assertTrue(visual.offsetMapping.transformedToOriginal(offset) in 0..source.length)
        }
    }
}
