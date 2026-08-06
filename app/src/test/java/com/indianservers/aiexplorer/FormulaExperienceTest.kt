package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.learning.FormulaDimension
import com.indianservers.aiexplorer.learning.FormulaExperienceEngine
import com.indianservers.aiexplorer.learning.FormulaFilterSet
import com.indianservers.aiexplorer.learning.FormulaPurpose
import com.indianservers.aiexplorer.learning.FormulaUnits
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaExperienceTest {
    @Test
    fun everyFormulaReceivesCompleteOfflineLearningMetadata() {
        MathKnowledgeCatalog.formulas.forEach { formula ->
            val detail = FormulaExperienceEngine.details(formula)
            assertTrue("${formula.title} needs use guidance", detail.whenToUse.isNotBlank())
            assertTrue("${formula.title} needs assumptions", detail.assumptions.isNotEmpty())
            assertTrue("${formula.title} needs limitations", detail.limitations.isNotEmpty())
            assertTrue("${formula.title} needs derivation", detail.derivation.size >= 4)
            assertTrue("${formula.title} needs examples", detail.examples.size >= 2)
            assertTrue("${formula.title} needs equivalent forms", detail.equivalentForms.size >= 3)
            assertNotNull(detail.workspace)
        }
    }

    @Test
    fun circleAreaCalculatesWithMixedLengthUnits() {
        val formula = MathKnowledgeCatalog.formulas.first { it.title == "Circle area" }
        val detail = FormulaExperienceEngine.details(formula)
        val result = FormulaExperienceEngine.calculate(detail, mapOf("r" to "100 cm"))

        assertTrue(result.messages.joinToString(), result.valid)
        assertEquals(Math.PI, result.result!!, 1e-6)
        assertEquals("m^2", result.resultUnit)
    }

    @Test
    fun unitsConvertAndRejectIncompatibleDimensions() {
        assertEquals(1.0, FormulaUnits.parse("100 cm", FormulaDimension.Length)!!, 1e-9)
        assertEquals(180.0, FormulaUnits.parse("${Math.PI} rad", FormulaDimension.Angle)!!, 1e-6)
        assertEquals(.25, FormulaUnits.parse("25%", FormulaDimension.Probability)!!, 1e-9)
        assertEquals(null, FormulaUnits.parse("2 s", FormulaDimension.Length))
    }

    @Test
    fun semanticFiltersFindRequestedFormulaFamily() {
        val area = FormulaExperienceEngine.filter(
            MathKnowledgeCatalog.formulas,
            "circle",
            FormulaFilterSet(purpose = FormulaPurpose.Area, dimension = FormulaDimension.Area),
        )

        assertTrue(area.any { it.title == "Circle area" })
        assertTrue(area.all { FormulaExperienceEngine.details(it).purpose == FormulaPurpose.Area })
        assertFalse(area.any { it.title == "Circle circumference" })
    }

    @Test
    fun plainEnglishCatalogueSearchIgnoresCommandWords() {
        val result = MathKnowledgeCatalog.search("show me all circle area formulas")

        assertTrue(result.formulas.any { it.title == "Circle area" })
        assertFalse(result.formulas.any { it.title == "Circle circumference" })
    }

    @Test
    fun practiceAndExportsAreReadyWithoutNetwork() {
        val detail = FormulaExperienceEngine.details(MathKnowledgeCatalog.formulas.first { it.title == "Triangle area" })
        val practice = FormulaExperienceEngine.practice(detail, 42)
        val notes = FormulaExperienceEngine.exportText(detail)
        val mathMl = FormulaExperienceEngine.exportMathMl(detail.formula)

        assertTrue(practice.prompt.contains("Find"))
        assertTrue(notes.contains("When to use"))
        assertTrue(notes.contains("Derivation"))
        assertTrue(mathMl.startsWith("<math"))
    }
}
