package com.indianservers.aiexplorer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.input.MathKey
import com.indianservers.aiexplorer.input.MathTextEditing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveMathKeyboardTest {
    @Test
    fun templateWrapsSelectionAndKeepsCursorStable() {
        val source = TextFieldValue("x+1", TextRange(0, 3))

        val result = MathTextEditing.insert(source, MathKey("square", "(%s)^2"))

        assertEquals("(x+1)^2", result.text)
        assertEquals(result.text.length, result.selection.start)
    }

    @Test
    fun functionTemplatePlacesCursorInsideArgument() {
        val result = MathTextEditing.insert(
            TextFieldValue("z=", TextRange(2)),
            MathKey("sin", "sin()", cursorBack = 1),
        )

        assertEquals("z=sin()", result.text)
        assertEquals(6, result.selection.start)
    }

    @Test
    fun backspaceRemovesEmptyBracketPairTogether() {
        val result = MathTextEditing.backspace(TextFieldValue("sin()", TextRange(4)))

        assertEquals("sin", result.text)
        assertEquals(3, result.selection.start)
    }

    @Test
    fun backspaceReplacesSelectedRange() {
        val result = MathTextEditing.backspace(TextFieldValue("x+123", TextRange(2, 5)))

        assertEquals("x+", result.text)
        assertEquals(2, result.selection.start)
    }

    @Test
    fun calculusTemplatesEvaluateInExpressionKernel() {
        val engine = ExpressionEngine()

        val derivative = engine.compile("derivative(sin(x),x)").eval(mapOf("x" to 0.0))
        val integral = engine.compile("integral(x,x,0,2)").eval()
        val sum = engine.compile("sum(n,n,1,4)").eval()
        val limit = engine.compile("limit(sin(x)/x,x,0)").eval()

        assertEquals(1.0, derivative, 1e-5)
        assertEquals(2.0, integral, 1e-5)
        assertEquals(10.0, sum, 1e-9)
        assertTrue(limit in 0.99999..1.00001)
    }
}
