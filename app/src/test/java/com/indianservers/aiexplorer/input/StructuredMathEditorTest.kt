package com.indianservers.aiexplorer.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.BaselineShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredMathEditorTest {
    @Test
    fun editorViewportPinchZoomsAroundCentroidAndPansWithinBounds() {
        val zoomed = transformMathEditorViewport(
            viewport = MathEditorViewport(),
            editorSize = IntSize(400, 200),
            centroid = Offset(300f, 100f),
            panChange = Offset.Zero,
            zoomChange = 2f,
        )

        assertEquals(2f, zoomed.scale)
        assertEquals(-100f, zoomed.pan.x)
        assertEquals(0f, zoomed.pan.y)

        val panned = transformMathEditorViewport(
            viewport = zoomed,
            editorSize = IntSize(400, 200),
            centroid = Offset(200f, 100f),
            panChange = Offset(500f, -500f),
            zoomChange = 1f,
        )
        assertEquals(200f, panned.pan.x)
        assertEquals(-100f, panned.pan.y)
    }

    @Test
    fun editorViewportSupportsFingerZoomOutAndClearsPanBelowOriginalScale() {
        val zoomedOut = transformMathEditorViewport(
            viewport = MathEditorViewport(scale = 2f, pan = Offset(80f, -30f)),
            editorSize = IntSize(400, 200),
            centroid = Offset(200f, 100f),
            panChange = Offset(20f, 10f),
            zoomChange = .4f,
        )

        assertEquals(.8f, zoomedOut.scale)
        assertEquals(Offset.Zero, zoomedOut.pan)

        val clamped = transformMathEditorViewport(
            viewport = zoomedOut,
            editorSize = IntSize(400, 200),
            centroid = Offset(200f, 100f),
            panChange = Offset.Zero,
            zoomChange = .1f,
        )
        assertEquals(.75f, clamped.scale)
        assertEquals(Offset.Zero, clamped.pan)
    }

    @Test
    fun superscriptToggleKeepsParserSyntaxAndLeavesTheSlot() {
        var value = TextFieldValue("x", TextRange(1))

        value = StructuredMathEditing.toggleSuperscript(value)
        assertEquals("x^()", value.text)
        assertEquals(3, value.selection.end)
        assertEquals(MathInputMode.SUPERSCRIPT, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("2", "2"))
        value = StructuredMathEditing.toggleSuperscript(value)
        value = MathTextEditing.insert(value, MathKey("+", "+"))
        value = MathTextEditing.insert(value, MathKey("1", "1"))

        assertEquals("x^(2)+1", value.text)
        assertEquals("x^(2)+1", StructuredMathCodec.toParser(value).text)
        assertEquals("x2+1", render(value.text).text)
        assertTrue(
            render(value.text).spanStyles.any {
                it.item.baselineShift == BaselineShift.Superscript && it.start == 1
            },
        )
    }

    @Test
    fun subscriptToggleSupportsCombinedScripts() {
        var value = TextFieldValue("x", TextRange(1))
        value = StructuredMathEditing.toggleSubscript(value)
        value = MathTextEditing.insert(value, MathKey("1", "1"))
        value = StructuredMathEditing.toggleSubscript(value)
        value = StructuredMathEditing.toggleSuperscript(value)
        value = MathTextEditing.insert(value, MathKey("2", "2"))
        value = StructuredMathEditing.toggleSuperscript(value)

        assertEquals("x_(1)^(2)", value.text)
        assertEquals("x12", render(value.text).text)
        val styles = render(value.text).spanStyles
        assertTrue(styles.any { it.item.baselineShift == BaselineShift.Subscript })
        assertTrue(styles.any { it.item.baselineShift == BaselineShift.Superscript })
    }

    @Test
    fun emptyScriptBackspaceRemovesTheWholeStructure() {
        val value = TextFieldValue("x^()", TextRange(3))
        val result = StructuredMathEditing.backspace(value)

        assertEquals("x", result.text)
        assertEquals(1, result.selection.end)
    }

    @Test
    fun basedLogRoundTripsWithoutChangingEvaluatorContract() {
        val parserValue = TextFieldValue("log(8,2)", TextRange(8))
        val editorValue = StructuredMathCodec.fromParser(parserValue)

        assertEquals("logbase(2,8)", editorValue.text)
        assertEquals("log2(8)", render(editorValue.text).text)
        assertTrue(render(editorValue.text).spanStyles.any { it.item.baselineShift == BaselineShift.Subscript })
        assertEquals("log(8,2)", StructuredMathCodec.toParser(editorValue).text)
        assertTrue(StructuredMathCodec.state(editorValue).root.children.single() is MathLogNode)
    }

    @Test
    fun squareRootButtonTogglesIntoAndOutOfTheRadicand() {
        val key = MathKey("√", "sqrt()", 1, action = MathKeyAction.TOGGLE_ROOT)
        var value = TextFieldValue("x+", TextRange(2))

        value = StructuredMathEditing.toggleRoot(value, key)
        assertEquals("x+sqrt()", value.text)
        assertEquals(MathInputMode.RADICAND, StructuredMathEditing.modeAt(value.text, value.selection.end))
        assertTrue(StructuredMathEditing.isRootActive(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("9", "9"))
        value = StructuredMathEditing.toggleRoot(value, key)
        assertEquals("x+sqrt(9)", value.text)
        assertEquals(value.text.length, value.selection.end)
        assertFalse(StructuredMathEditing.isRootActive(value.text, value.selection.end))
    }

    @Test
    fun squareRootWrapsSelectionAndSecondTapExits() {
        val key = MathKey("√", "sqrt(%s)", 1, action = MathKeyAction.TOGGLE_ROOT)
        var value = TextFieldValue("x+1", TextRange(0, 3))

        value = StructuredMathEditing.toggleRoot(value, key)
        assertEquals("sqrt(x+1)", value.text)
        assertTrue(StructuredMathEditing.isRootActive(value.text, value.selection.end))

        value = StructuredMathEditing.toggleRoot(value, key)
        assertEquals(value.text.length, value.selection.end)
    }

    @Test
    fun cubeRootButtonCreatesAnIndependentRadicandAndSerializesSafely() {
        val key = MathKey("∛", "cbrt()", 1, action = MathKeyAction.TOGGLE_CUBE_ROOT)
        var value = TextFieldValue("", TextRange(0))

        value = StructuredMathEditing.toggleCubeRoot(value, key)
        assertEquals("cbrt()", value.text)
        assertEquals(MathInputMode.RADICAND, StructuredMathEditing.modeAt(value.text, value.selection.end))
        assertTrue(StructuredMathEditing.isCubeRootActive(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("27"))
        assertEquals("(27)^(1/3)", StructuredMathCodec.toParser(value).text)
        assertEquals("∛2̅7̅", render(value.text).text)

        value = StructuredMathEditing.toggleCubeRoot(value, key)
        assertEquals(value.text.length, value.selection.end)
        assertFalse(StructuredMathEditing.isCubeRootActive(value.text, value.selection.end))
        assertTrue(StructuredMathParser.parse(value.text).children.single() is MathRootNode)
    }

    @Test
    fun nthRootCyclesIndexRadicandAndRendersWithAnExtendedBar() {
        val key = MathKey("ⁿ√", "nthroot(,)", 2, action = MathKeyAction.TOGGLE_NTH_ROOT)
        var value = StructuredMathEditing.toggleNthRoot(TextFieldValue(""), key)
        assertEquals(MathInputMode.ROOT_INDEX, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("4"))
        value = StructuredMathEditing.toggleNthRoot(value, key)
        assertEquals(MathInputMode.RADICAND, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("16"))
        assertEquals("(16)^(1/(4))", StructuredMathCodec.toParser(value).text)
        assertEquals("4√1̅6̅", render(value.text).text)

        value = StructuredMathEditing.toggleNthRoot(value, key)
        assertEquals(value.text.length, value.selection.end)
        assertFalse(StructuredMathEditing.isNthRootActive(value.text, value.selection.end))
    }

    @Test
    fun calculusTemplatesRenderAsReadableMathematicsWithIndependentArguments() {
        assertEquals("d(x²)/dx", render("derivative(x²,x)").text)
        assertEquals("∂(x y)/∂x", render("partial(x*y,x)").text)
        val secondDerivative = render("derivative(y,x,2)")
        val thirdDerivative = render("derivative(y,x,3)")
        val fourthDerivative = render("derivative(y,x,4)")
        assertEquals("d2(y)/dx2", secondDerivative.text)
        assertEquals("d3(y)/dx3", thirdDerivative.text)
        assertEquals("d4(y)/dx4", fourthDerivative.text)
        assertTrue(secondDerivative.spanStyles.count { it.item.baselineShift == BaselineShift.Superscript } >= 2)
        assertTrue(thirdDerivative.spanStyles.count { it.item.baselineShift == BaselineShift.Superscript } >= 2)
        assertTrue(fourthDerivative.spanStyles.count { it.item.baselineShift == BaselineShift.Superscript } >= 2)
        assertEquals("∫(x²) dx", render("integral(x²,x)").text)
        assertEquals("∫(x) dx [0,1]", render("integral(x,x,0,1)").text)
        assertEquals("lim(sin(x)/x; x→0)", render("limit(sin(x)/x,x,0)").text)
        assertEquals("Σ(x; n=1…10)", render("sum(x,n,1,10)").text)

        val slots = StructuredMathEditing.editableSlots("integral(,x,,)")
            .filter { it.mode == MathInputMode.FUNCTION_ARGUMENT }
        assertEquals(4, slots.size)
        assertEquals(4, slots.map { it.owner }.distinct().size)
    }

    @Test
    fun exponentialAndMultiplicationUseConventionalVisualNotation() {
        val rendered = render("exp(600*x)*sin(x)")

        assertEquals("e600 x sin(x)", rendered.text)
        assertTrue(
            rendered.spanStyles.any {
                it.item.baselineShift == BaselineShift.Superscript
            },
        )
        assertFalse(rendered.text.contains("exp("))
        assertFalse(rendered.text.contains("*"))
    }

    @Test
    fun matricesAndDeterminantsRenderAsMathematicalLayouts() {
        val matrix = render("[[1,2,3],[0,-1,4],[2,1,0]]").text
        val determinant = render("det([[2,-1,3],[0,4,5],[1,2,-2]])").text

        assertEquals("⎡1  2  3⎤\n⎢0  -1  4⎥\n⎣2  1  0⎦", matrix)
        assertEquals("│2  -1  3│\n│0  4  5│\n│1  2  -2│", determinant)
        assertFalse(matrix.contains("["))
        assertFalse(determinant.contains("det"))
    }

    @Test
    fun nestedFractionsRetainNestedMathStyling() {
        val source = "(600+(a)/(b))/(1-(a)/(b))"
        val rendered = render(source)

        assertEquals(3, rendered.text.count { it == '⁄' })
        assertFalse(rendered.text.contains("/"))
        assertTrue(rendered.spanStyles.count { it.item.baselineShift != null } >= 4)
    }

    @Test
    fun fractionButtonCyclesNumeratorDenominatorAndExit() {
        val key = fractionTemplate(hasSelection = false)
        var value = TextFieldValue("")

        value = StructuredMathEditing.toggleFraction(value, key)
        assertEquals("()/()", value.text)
        assertEquals(MathInputMode.NUMERATOR, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("1", "1"))
        value = StructuredMathEditing.toggleFraction(value, key)
        assertEquals(MathInputMode.DENOMINATOR, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("2", "2"))
        value = StructuredMathEditing.toggleFraction(value, key)
        assertEquals("(1)/(2)", value.text)
        assertEquals(value.text.length, value.selection.end)
        assertFalse(StructuredMathEditing.isFractionActive(value.text, value.selection.end))
    }

    @Test
    fun basedLogButtonCyclesBaseArgumentAndExit() {
        var value = TextFieldValue("")

        value = StructuredMathEditing.toggleLogBase(value)
        assertEquals("logbase(,)", value.text)
        assertEquals(MathInputMode.LOG_BASE, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("2", "2"))
        value = StructuredMathEditing.toggleLogBase(value)
        assertEquals(MathInputMode.FUNCTION_ARGUMENT, StructuredMathEditing.modeAt(value.text, value.selection.end))

        value = MathTextEditing.insert(value, MathKey("8", "8"))
        value = StructuredMathEditing.toggleLogBase(value)
        assertEquals("logbase(2,8)", value.text)
        assertEquals(value.text.length, value.selection.end)
        assertEquals("log(8,2)", StructuredMathCodec.toParser(value).text)
    }

    @Test
    fun duplicateStructuralKeysShareTheSameSelectedState() {
        val root = MathKey("√", "sqrt()", 1, action = MathKeyAction.TOGGLE_ROOT)
        val fraction = fractionTemplate(hasSelection = false)
        val power = MathKey("xʸ", action = MathKeyAction.TOGGLE_SUPERSCRIPT)
        val subscript = MathKey("xₙ", action = MathKeyAction.TOGGLE_SUBSCRIPT)
        val logBase = MathKey("logₐ", action = MathKeyAction.TOGGLE_LOG_BASE)

        assertTrue(isStructuralKeyActive(root, "sqrt(9)", 6))
        assertFalse(isStructuralKeyActive(root, "sqrt(9)", 7))
        assertTrue(isStructuralKeyActive(fraction, "(1)/(2)", 2))
        assertTrue(isStructuralKeyActive(fraction, "(1)/(2)", 6))
        assertTrue(isStructuralKeyActive(power, "x^(2)", 4))
        assertTrue(isStructuralKeyActive(subscript, "x_(n)", 4))
        assertTrue(isStructuralKeyActive(logBase, "logbase(2,8)", 9))
    }

    @Test
    fun fractionAndRootRenderAsMathWithoutLeakingParserDelimiters() {
        val source = "(x+1)/(x-1)+sqrt(x^2+1)"
        val rendered = render(source)

        assertFalse(rendered.text.contains("sqrt"))
        assertFalse(rendered.text.contains("^"))
        assertTrue(rendered.text.contains("⁄"))
        assertTrue(rendered.text.contains("√"))
        val nodes = StructuredMathParser.parse(source).children
        assertTrue(nodes.any { it is MathFractionNode })
        assertTrue(nodes.any { it is MathRootNode })
    }

    @Test
    fun radicalBarCoversEveryVisibleRadicandCharacter() {
        val rendered = render("sqrt(x+12)")

        assertEquals("√x̅+̅1̅2̅", rendered.text)
        assertEquals(4, rendered.text.count { it == '\u0305' })
    }

    @Test
    fun visualOffsetMappingStaysWithinSourceAndDisplayBounds() {
        val source = "logbase(2,x^(12))+()/(y)"
        val transformed = StructuredMathVisualLayout.render(source)

        for (offset in 0..source.length) {
            val visual = transformed.offsetMapping.originalToTransformed(offset)
            assertTrue(visual in 0..transformed.text.length)
        }
        for (offset in 0..transformed.text.length) {
            val original = transformed.offsetMapping.transformedToOriginal(offset)
            assertTrue(original in 0..source.length)
        }
    }

    @Test
    fun editorStateExportsLatexAndSelection() {
        val value = TextFieldValue("a_(n)^(2)", TextRange(3, 4))
        val state = StructuredMathCodec.state(value)

        assertEquals("a_{n}^{2}", state.latexExpression)
        assertEquals(TextRange(3, 4), state.selection)
    }

    @Test
    fun everyStructuredPlaceholderHasAnIndependentSourceSlot() {
        val source = "()/()+x^()+a_()+sqrt()+logbase(,)+sin()+[[,],[,]]"
        val slots = StructuredMathEditing.editableSlots(source)

        assertEquals(2, slots.count { it.mode in setOf(MathInputMode.NUMERATOR, MathInputMode.DENOMINATOR) })
        assertEquals(1, slots.count { it.mode == MathInputMode.SUPERSCRIPT })
        assertEquals(1, slots.count { it.mode == MathInputMode.SUBSCRIPT })
        assertEquals(1, slots.count { it.mode == MathInputMode.RADICAND })
        assertEquals(1, slots.count { it.mode == MathInputMode.LOG_BASE })
        assertTrue(slots.any { it.mode == MathInputMode.FUNCTION_ARGUMENT && it.isPlaceholder })
        assertEquals(4, slots.count { it.mode == MathInputMode.MATRIX_CELL })
        assertEquals(
            slots.map { it.contentStart }.distinct().size,
            slots.map { it.contentStart }.size,
        )
    }

    @Test
    fun numeratorAndDenominatorResolveToDifferentActiveBoxes() {
        val source = "(a)/(b)"
        val numerator = StructuredMathEditing.activeSlot(source, 1)
        val denominator = StructuredMathEditing.activeSlot(source, 5)

        assertEquals(MathInputMode.NUMERATOR, numerator?.mode)
        assertEquals(1..2, numerator?.let { it.contentStart..it.contentEnd })
        assertEquals(MathInputMode.DENOMINATOR, denominator?.mode)
        assertEquals(5..6, denominator?.let { it.contentStart..it.contentEnd })
    }

    @Test
    fun visibleEmptyBoxesMapBackToTheirOwnZeroWidthOffsets() {
        val source = "()/()+[[],[]]"
        val transformed = StructuredMathVisualLayout.render(source)
        val placeholders = transformed.text.indices.filter { transformed.text[it] == '□' }
        val mapped = placeholders.map { visual ->
            transformed.offsetMapping.transformedToOriginal(visual + 1)
        }

        assertEquals(4, placeholders.size)
        assertEquals(mapped.distinct().size, mapped.size)
        assertTrue(mapped.all { it in 0..source.length })
    }

    private fun render(source: String) =
        StructuredMathVisualLayout.render(source).text
}
