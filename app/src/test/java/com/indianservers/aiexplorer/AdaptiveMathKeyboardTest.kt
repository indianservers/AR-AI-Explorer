package com.indianservers.aiexplorer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.input.MathKey
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.input.MathKeyTone
import com.indianservers.aiexplorer.input.MathTextEditing
import com.indianservers.aiexplorer.input.advancedNotationKeys
import com.indianservers.aiexplorer.input.basicNumberPadRows
import com.indianservers.aiexplorer.input.calculusKeys
import com.indianservers.aiexplorer.input.commonMathKeys
import com.indianservers.aiexplorer.input.filterMathCommands
import com.indianservers.aiexplorer.input.fractionTemplate
import com.indianservers.aiexplorer.input.functionKeys
import com.indianservers.aiexplorer.input.matrixTemplate
import com.indianservers.aiexplorer.input.matrixStructureKeys
import com.indianservers.aiexplorer.input.mathKeyboardCommands
import com.indianservers.aiexplorer.input.primaryMathKeyboardPages
import com.indianservers.aiexplorer.input.resolveMathKeyTone
import com.indianservers.aiexplorer.input.trigonometryKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveMathKeyboardTest {
    @Test
    fun primaryTabsExposeFrequentMathAndKeepAdvancedToolsInOverflow() {
        assertEquals(listOf("123", "f(x)", "abc", "trig", "Math+", "αβ", "…"), primaryMathKeyboardPages.map { it.label })
        assertTrue(trigonometryKeys(false).any { it.insertion == "sec()" })
        assertTrue(trigonometryKeys(false).any { it.insertion == "sinh()" })
        assertTrue(trigonometryKeys(false).any { it.insertion == "deg" })
    }

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
    fun clipboardEditsRespectCaretAndSelection() {
        val source = TextFieldValue("sin(x)+1", TextRange(4, 5))
        assertEquals("x", MathTextEditing.selectedOrAll(source))

        val pasted = MathTextEditing.replaceSelection(source, "theta")
        assertEquals("sin(theta)+1", pasted.text)
        assertEquals(9, pasted.selection.start)

        val cut = MathTextEditing.cutSelectionOrAll(source)
        assertEquals("sin()+1", cut.text)
        assertEquals(4, cut.selection.start)
        assertEquals("", MathTextEditing.cutSelectionOrAll(TextFieldValue("x+1", TextRange(2))).text)
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
    fun extendedTrigKeysEvaluateInsteadOfActingAsDecorativeTemplates() {
        val engine = ExpressionEngine()

        assertEquals(1.0, engine.compile("sech(0)").eval(), 1e-12)
        assertEquals(60.0, engine.compile("asec(2)").eval(), 1e-9)
        assertEquals(1.0, engine.compile("asinh(sinh(1))").eval(), 1e-9)
        assertEquals(2.0, engine.compile("acsch(csch(2))").eval(), 1e-9)
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
        assertEquals("Mean", filterMathCommands(query = "find the average of numbers").first().name)
        assertEquals("Definite integral", filterMathCommands(query = "area under curve").single().name)
    }

    @Test
    fun numberPadUsesFamiliarSerialRowsAndDistinctSemanticTones() {
        assertEquals(listOf("7", "8", "9", "÷"), basicNumberPadRows[0].map { it.label })
        assertEquals(listOf("4", "5", "6", "×"), basicNumberPadRows[1].map { it.label })
        assertEquals(listOf("1", "2", "3", "−"), basicNumberPadRows[2].map { it.label })
        assertEquals(MathKeyTone.NUMBER, resolveMathKeyTone(basicNumberPadRows[0][0]))
        assertEquals(MathKeyTone.OPERATOR, resolveMathKeyTone(basicNumberPadRows[1][3]))
        assertEquals(MathKeyTone.VARIABLE, resolveMathKeyTone(basicNumberPadRows[4][0]))
        assertEquals(MathKeyTone.CONSTANT, resolveMathKeyTone(basicNumberPadRows[4][2]))
        assertEquals(listOf("𝑥", "𝑦", "π", "(", ")", ","), basicNumberPadRows[4].map { it.label })
    }

    @Test
    fun fractionAndMatrixBuildersPlaceCaretInFirstEditableSlot() {
        val fraction = MathTextEditing.insert(TextFieldValue(""), fractionTemplate(false))
        assertEquals("()/()", fraction.text)
        assertEquals(1, fraction.selection.start)

        val wrapped = MathTextEditing.insert(TextFieldValue("x+1", TextRange(0, 3)), fractionTemplate(true))
        assertEquals("(x+1)/()", wrapped.text)
        assertEquals(7, wrapped.selection.start)

        val matrix = MathTextEditing.insert(TextFieldValue(""), matrixTemplate(3, 2))
        assertEquals("[[,],[,],[,]]", matrix.text)
        assertEquals(2, matrix.selection.start)
    }

    @Test
    fun commonKeysAreAvailableAndEditAtTheCursor() {
        assertEquals(listOf("=", "+", "−", "×", "÷", "(", ")", ","), commonMathKeys.map { it.label })

        var value = TextFieldValue("x", TextRange(1))
        listOf("=", "(", "+", ")").forEach { label ->
            value = MathTextEditing.insert(value, commonMathKeys.single { it.label == label })
        }

        assertEquals("x=(+)", value.text)
        assertEquals(5, value.selection.start)
        assertEquals("x=(+", MathTextEditing.backspace(value).text)
    }

    @Test
    fun functionAndTrigPagesHaveDistinctResponsibilities() {
        val directTrig = trigonometryKeys(inverse = false)
        val inverseTrig = trigonometryKeys(inverse = true)

        assertTrue(functionKeys.none { it.insertion in directTrig.map(MathKey::insertion) })
        assertTrue(functionKeys.none { it.insertion in inverseTrig.map(MathKey::insertion) })
        assertEquals(
            listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "sec⁻¹", "csc⁻¹", "cot⁻¹"),
            inverseTrig.take(6).map { it.label },
        )
        assertTrue(directTrig.any { it.insertion == "sech()" })
        assertTrue(directTrig.any { it.insertion == "csch()" })
        assertTrue(directTrig.any { it.insertion == "coth()" })
        assertTrue(inverseTrig.any { it.insertion == "asinh()" })
        assertTrue(inverseTrig.any { it.insertion == "asech()" })
        assertTrue(functionKeys.none { it.label in setOf("√", "∛", "xʸ", "xₙ", "logₐ") })
    }

    @Test
    fun mathPlusExposesNotationCalculusAndMatrixStructures() {
        assertTrue(advancedNotationKeys.any { it.action.name == "TOGGLE_NTH_ROOT" })
        assertTrue(calculusKeys.any { it.label == "∬" })
        assertTrue(calculusKeys.any { it.label == "∭" })
        assertTrue(calculusKeys.any { it.label == "∮" })
        assertTrue(matrixStructureKeys.any { it.label == "1×4" })
        assertTrue(matrixStructureKeys.any { it.label == "4×4" })
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
