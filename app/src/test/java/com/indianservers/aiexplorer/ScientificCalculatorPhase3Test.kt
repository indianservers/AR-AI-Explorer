package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.CalculatorFavourites
import com.indianservers.aiexplorer.core.CalculatorHandoffTarget
import com.indianservers.aiexplorer.core.CalculatorRecognitionAdapters
import com.indianservers.aiexplorer.core.ProfessionalScientificCalculator
import com.indianservers.aiexplorer.core.SmartScientificCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScientificCalculatorPhase3Test {
    private val calculator = ProfessionalScientificCalculator()

    @Test fun matrixAndVectorOperationsAreVerified() {
        assertEquals("-2", calculator.evaluate("determinant [[1,2],[3,4]]")!!.primary)
        assertEquals("32", calculator.evaluate("vector dot <1,2,3> ; <4,5,6>")!!.primary)
        assertEquals("<0, 0, 1>", calculator.evaluate("vector cross <1,0,0> ; <0,1,0>")!!.primary)
    }

    @Test fun statisticsAndProbabilityReturnRichResults() {
        val stats = calculator.evaluate("stats 2,3,3,5,7,10")!!
        assertEquals("5", stats.primary)
        assertEquals("4", stats.details.first { it.first == "Median" }.second)
        val normal = calculator.evaluate("normal cdf 0 mean 0 sd 1")!!
        assertTrue(normal.primary.toDouble() in .499999..0.500001)
        assertTrue(calculator.evaluate("binomial pmf 3 n 10 p .5")!!.primary.toDouble() > .1)
    }

    @Test fun unitsDimensionsUncertaintyAndSigFigsAreSafe() {
        assertEquals("20 m/s", calculator.evaluate("convert 72 km/h to m/s")!!.primary)
        try {
            calculator.evaluate("dimension 3 m + 5 s")
            fail("Dimension mismatch should be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("mismatch", ignoreCase = true))
        }
        assertTrue(calculator.evaluate("uncertainty 5 +/- .1 * 2 +/- .2")!!.primary.startsWith("10 +/-"))
        assertTrue(calculator.evaluate("sigfig 1234.567 4")!!.primary.contains("e"))
    }

    @Test fun programmerFinanceAndConstantsAreAvailable() {
        val programmer = calculator.evaluate("programmer 255")!!
        assertEquals("FF", programmer.details.first { it.first == "Hex" }.second)
        assertEquals("8", calculator.evaluate("bitwise 12 and 10")!!.primary)
        assertEquals("40", calculator.evaluate("shift 5 left 3")!!.primary)
        assertTrue(calculator.evaluate("emi principal 500000 rate 8 years 20")!!.primary.toDouble() > 4_000)
        assertTrue(calculator.evaluate("constant light")!!.details.any { it.first == "Unit" })
    }

    @Test fun recognitionFavouritesAndHandoffsFormProductionAdapters() {
        assertEquals("2*pi+sqrt(9)", CalculatorRecognitionAdapters.ocr("2×π+√(9)").normalized)
        assertEquals("2+3", CalculatorRecognitionAdapters.voice("calculate 2 plus 3").normalized)
        assertTrue(CalculatorRecognitionAdapters.voice("2 plus 3", .5).warnings.isNotEmpty())
        val favourites = CalculatorFavourites(listOf("sin("))
        assertTrue("cos(" in favourites.toggle("cos("))
        assertTrue("sin(" !in favourites.toggle("sin("))
        val result = calculator.evaluate("stats 1,2,3")!!
        val handoffs = calculator.handoffs("stats 1,2,3", result)
        assertTrue(handoffs.map { it.target }.containsAll(listOf(CalculatorHandoffTarget.Graph, CalculatorHandoffTarget.Solver, CalculatorHandoffTarget.Notebook, CalculatorHandoffTarget.Spreadsheet)))
    }

    @Test fun sharedSmartCalculatorRoutesProfessionalCommands() {
        val outcome = SmartScientificCalculator().evaluate("convert 72 km/h to m/s")
        assertEquals("20 m/s", outcome.primary)
        assertTrue(outcome.steps.isNotEmpty())
    }
}
