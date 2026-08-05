package com.indianservers.aiexplorer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.input.MathKey
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.input.MathTextEditing
import com.indianservers.aiexplorer.input.commonMathKeys
import com.indianservers.aiexplorer.input.filterMathCommands
import com.indianservers.aiexplorer.input.mathKeyboardCommands
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
    fun rapidTanEntryKeepsEveryCharacterInsideTheFunction() {
        var value = TextFieldValue("")
        listOf(
            MathKey("tan", "tan()", cursorBack = 1),
            MathKey("1"),
            MathKey("2"),
        ).forEach { key -> value = MathTextEditing.insert(value, key) }

        assertEquals("tan(12)", value.text)
        assertEquals(6, value.selection.start)
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

    @Test
    fun moreBrowserIncludesAdvancedMathCategoriesInGraphWorkspaces() {
        val categories = mathKeyboardCommands.map { it.category }.toSet()

        listOf(
            "Statistics",
            "Discrete Maths",
            "Probability",
            "Finance",
            "Matrices",
            "Vectors",
            "Logic",
            "Lists",
            "Optimisation",
            "Transformations",
        ).forEach { category -> assertTrue("$category is missing", category in categories) }

        val graphStatistics = filterMathCommands(
            category = "Statistics",
            context = MathKeyboardContext.GRAPH_2D,
        )
        assertTrue(graphStatistics.any { it.name == "Mean" })
        assertTrue(graphStatistics.any { it.name == "Standard deviation" })
    }

    @Test
    fun moreBrowserSearchesNamesCategoriesAndDescriptions() {
        assertEquals("Combination", filterMathCommands(query = "choose items").single().name)
        assertTrue(filterMathCommands(query = "discrete").any { it.name == "Factorial" })
        assertTrue(filterMathCommands(query = "normal").any { it.category == "Probability" })
    }

    @Test
    fun commonKeysAreAvailableAndEditAtTheCursor() {
        assertEquals(listOf("=", "+", "−", "×", "÷", "( )", ","), commonMathKeys.map { it.label })

        var value = TextFieldValue("x", TextRange(1))
        listOf("=", "( )", "+").forEach { label ->
            value = MathTextEditing.insert(value, commonMathKeys.single { it.label == label })
        }

        assertEquals("x=(+)", value.text)
        assertEquals(4, value.selection.start)
        assertEquals("x=()", MathTextEditing.backspace(value).text)
    }

    @Test
    fun everyAdvancedCommandInsertsAUsableTemplate() {
        mathKeyboardCommands.forEach { command ->
            val result = MathTextEditing.insert(TextFieldValue(""), command.template)

            assertTrue("${command.name} inserted nothing", result.text.isNotBlank())
            assertTrue(
                "${command.name} left the cursor outside its template",
                result.selection.start in 0..result.text.length &&
                    result.selection.end in 0..result.text.length,
            )
        }
    }
}
