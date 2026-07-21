package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.CasAssumptionDraft
import com.indianservers.aiexplorer.core.CasInteractionEngine
import com.indianservers.aiexplorer.core.CasKeyboardCatalog
import com.indianservers.aiexplorer.core.CasKeyboardLayer
import com.indianservers.aiexplorer.core.CasSolutionMethod
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CasInteractionExperienceTest {
    private val interaction = CasInteractionEngine()

    @Test
    fun typedCommandControlsInterpretationAndRemovesCommandFromExpression() {
        val preview = interaction.interpret("integrate 3*x^2", selectedOperation = "factor")

        assertEquals("integral", preview.operation)
        assertEquals("3*x^2", preview.expression)
        assertEquals("x", preview.variable)
        assertTrue(preview.confidence > 0.0)
        assertTrue(preview.explanation.contains("integral"))
    }

    @Test
    fun assumptionDraftValidatesAndFeedsGuardedSimplification() {
        val assumption = CasAssumptionDraft(
            variable = "x",
            domain = MathNumberDomain.Real,
            nonNegative = true,
            nonZero = true,
            minimum = "0",
            maximum = "12",
        ).build()
        val assumptions = MathAssumptionSet().with(assumption)
        val preview = interaction.interpret("sqrt(x^2)", "simplify", assumptions)
        val row = interaction.evaluate(preview, assumptions, CasSolutionMethod.Direct)

        assertEquals("x", row.exact)
        assertTrue(row.assumptions.any { it.contains("x ≥ 0") })
        assertTrue(row.steps.any { it.title == "Assumptions" })
    }

    @Test
    fun invalidAssumptionBoundsAreRejectedBeforeEvaluation() {
        try {
            CasAssumptionDraft(variable = "x", minimum = "ten").build()
            fail("Expected a numeric-bound validation failure")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("numeric"))
        }

        try {
            CasAssumptionDraft(variable = "x", minimum = "5", maximum = "2").build()
            fail("Expected an ordered-bound validation failure")
        } catch (_: IllegalArgumentException) {
            // VariableAssumption enforces minimum <= maximum.
        }
    }

    @Test
    fun methodSelectionChangesPreparationAndAddsTransparentMethodStep() {
        val preview = interaction.interpret("(x+1)^2", "simplify")
        val expanded = interaction.evaluate(preview, MathAssumptionSet(), CasSolutionMethod.ExpandFirst)
        val verified = interaction.evaluate(preview, MathAssumptionSet(), CasSolutionMethod.Verify)

        assertEquals("1 + 2*x + x^2", expanded.exact)
        assertEquals("Expand first", expanded.steps.first().expression)
        assertTrue(verified.steps.any { it.title == "Independent check" })
        assertTrue(interaction.availableMethods("system").contains(CasSolutionMethod.Substitution))
    }

    @Test
    fun fullMathKeyboardCoversEveryLayerAndStructuredTemplates() {
        assertEquals(CasKeyboardLayer.entries.toSet(), CasKeyboardCatalog.layers.keys)
        assertTrue(CasKeyboardCatalog.layers.values.all { it.size >= 12 })
        assertTrue(CasKeyboardCatalog.layers.getValue(CasKeyboardLayer.Calculus).any { it.label == "∫" })
        assertTrue(CasKeyboardCatalog.layers.getValue(CasKeyboardLayer.Matrix).any { it.label == "2×2" })
        assertTrue(CasKeyboardCatalog.layers.getValue(CasKeyboardLayer.Relations).any { it.label == "≤" })
        assertTrue(CasKeyboardCatalog.layers.getValue(CasKeyboardLayer.Greek).any { it.label == "π" || it.label == "α" })
        assertTrue(CasKeyboardCatalog.layers.values.flatten().filter { it.cursorBack > 0 }.all { it.cursorBack <= it.insertion.length })
    }
}
