package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.visualisation.SolverVisualisationValidator
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationData
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3VisualisationTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun moreThanTwoHundredVisualSpecificationsAreLinkedAccessibleAndValid() {
        var cases = 0
        for (left in -12..12) {
            for (right in -4..4) {
                val solution = engine.solve("$left + $right")
                assertVisualIntegrity(solution.visualisations, solution.steps.map { it.id }.toSet())
                cases++
            }
        }
        assertTrue("Expected at least 200 visual cases, got $cases", cases >= 200)
    }

    @Test
    fun arithmeticMovementMatchesTheExactResult() {
        for (start in -20..20 step 2) {
            for (delta in -5..5) {
                val solution = engine.solve("$start + $delta")
                val line = solution.visualisations.first { it.type == VisualisationType.NumberLine }.mathematicalData as VisualisationData.NumberLine
                assertEquals((start + delta).toDouble(), line.end, 1e-10)
                assertTrue(line.start in line.minimum..line.maximum)
                assertTrue(line.end in line.minimum..line.maximum)
            }
        }
    }

    @Test
    fun linearEquationVisualLinksToEveryStructuredStep() {
        (1..40).forEach { value ->
            val solution = engine.solve("2x + $value = ${value + 8}")
            assertTrue(solution.supported)
            val balance = solution.visualisations.first { it.type == VisualisationType.BalanceScale }
            assertEquals(solution.steps.map { it.id }, balance.linkedStepIds)
            assertTrue(SolverVisualisationValidator.validate(balance).isEmpty())
        }
    }

    @Test
    fun graphSamplesNeverJoinNonFiniteValues() {
        listOf(
            "x^2 - 4 = 0",
            "x^2 + 3x + 2 = 0",
            "x^2 - 9 = 0",
            "x^2 + 2x - 8 = 0",
        ).forEach { source ->
            val solution = engine.solve(source)
            val graph = solution.visualisations.firstOrNull {
                it.type == VisualisationType.QuadraticGeometry || it.type == VisualisationType.CoordinateGraph
            }
            assertTrue("Expected a graph for $source", graph != null)
            assertTrue(SolverVisualisationValidator.validate(graph!!).isEmpty())
            val data = graph.mathematicalData as VisualisationData.CoordinateGraph
            assertTrue(data.series.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() })
        }
    }

    @Test
    fun unitCircleUsesExactGeometricInvariant() {
        listOf(0, 30, 45, 60, 90, 120, 180, 225, 270, 315).forEach { degrees ->
            val solution = engine.solve("sin(${degrees}deg)")
            val circle = solution.visualisations.first { it.type == VisualisationType.UnitCircle }
            val data = circle.mathematicalData as VisualisationData.UnitCircle
            assertEquals(1.0, data.sine * data.sine + data.cosine * data.cosine, 1e-12)
            assertTrue(solution.visualVerification?.consistent == true)
        }
    }

    @Test
    fun matrixVisualIsRectangularAndStepLinked() {
        val solution = engine.solve("matrixmultiply([[1,2],[3,4]],[[2,0],[1,2]])")
        assertTrue(solution.supported)
        val matrix = solution.visualisations.first { it.type == VisualisationType.RowReductionGrid }
        assertTrue(SolverVisualisationValidator.validate(matrix).isEmpty())
        assertTrue(matrix.linkedStepIds.all { id -> solution.steps.any { it.id == id } })
    }

    @Test
    fun unsupportedInputDoesNotInventAVisual() {
        val solution = engine.solve("complex roots nonsense order 3")
        assertFalse(solution.supported)
        assertTrue(solution.visualisations.isEmpty())
        assertFalse(solution.canPresentAsCorrect)
    }

    private fun assertVisualIntegrity(
        visualisations: List<com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationSpec>,
        stepIds: Set<String>,
    ) {
        assertTrue(visualisations.isNotEmpty())
        visualisations.forEach { specification ->
            assertTrue(SolverVisualisationValidator.validate(specification).isEmpty())
            assertTrue(specification.linkedStepIds.all(stepIds::contains))
            assertTrue(specification.accessibilityDescription.isNotBlank())
        }
    }
}
