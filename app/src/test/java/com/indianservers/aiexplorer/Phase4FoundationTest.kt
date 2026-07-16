package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.CombinatoricsLab
import com.indianservers.aiexplorer.core.ConditionalProbabilityEngine
import com.indianservers.aiexplorer.core.DynamicGeometryDocument
import com.indianservers.aiexplorer.core.DynamicGeometryEngine
import com.indianservers.aiexplorer.core.DynamicGeometryObject
import com.indianservers.aiexplorer.core.DynamicPoint
import com.indianservers.aiexplorer.core.DynamicPointRule
import com.indianservers.aiexplorer.core.ExtendedDistributionEngine
import com.indianservers.aiexplorer.core.ExtendedDistributionKind
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.MissingDataPolicy
import com.indianservers.aiexplorer.core.Phase4Statistics
import com.indianservers.aiexplorer.core.RandomExperimentEngine
import com.indianservers.aiexplorer.core.RandomExperimentKind
import com.indianservers.aiexplorer.core.SpreadsheetAddress
import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4FoundationTest {
    @Test
    fun dynamicGeometryRecomputesChildrenAndReplaysProtocol() {
        val engine = DynamicGeometryEngine()
        var document = DynamicGeometryDocument()
        document = engine.addPoint(document, DynamicPoint("A", DynamicPointRule.Free(Vec2(0.0, 0.0))), "free")
        document = engine.addPoint(document, DynamicPoint("B", DynamicPointRule.Free(Vec2(4.0, 0.0))), "free")
        document = engine.addPoint(document, DynamicPoint("M", DynamicPointRule.Midpoint("A", "B")), "midpoint theorem")
        document = engine.addPoint(document, DynamicPoint("C", DynamicPointRule.Rotate("B", "A", 90.0)), "rotation preserves distance")
        document = engine.addObject(document, DynamicGeometryObject.Circle("c", "A", "B"), "center and point")

        assertEquals(Vec2(2.0, 0.0), engine.resolve(document).getValue("M"))
        assertEquals(0.0, engine.resolve(document).getValue("C").x, 1e-9)
        assertEquals(4.0, engine.resolve(document).getValue("C").y, 1e-9)
        val moved = engine.moveFreePoint(document, "B", Vec2(6.0, 0.0))
        assertEquals(Vec2(3.0, 0.0), engine.resolve(moved).getValue("M"))
        assertEquals("6", engine.distance(moved, "A", "B").exact)
        assertEquals(18.0, engine.polygonArea(moved, listOf("A", "B", "C")).decimal, 1e-9)
        assertEquals(listOf("A", "B"), engine.dependencyTree(document).getValue("M"))
        assertEquals(2, engine.replay(document, 2).points.size)
        assertFalse(engine.setStepVisible(document, "M", false).protocol.first { it.id == "M" }.visible)
    }

    @Test
    fun spreadsheetFormulasCsvMissingDataAndLinkedPlotsStaySynchronized() {
        val engine = MathSpreadsheetEngine()
        var document = engine.importCsv("x,y\n1,2\n2,4\n3,6\n4,NA")
        document = engine.setCell(document, SpreadsheetAddress(2, 0), "=SUM(B1:B3)")
        document = engine.setCell(document, SpreadsheetAddress(2, 1), "=MEAN(A1:A3)")
        val snapshot = engine.evaluate(document, MissingDataPolicy.Skip)

        assertEquals(12.0, snapshot.evaluated.getValue(SpreadsheetAddress(2, 0)).value!!, 1e-9)
        assertEquals(2.0, snapshot.evaluated.getValue(SpreadsheetAddress(2, 1)).value!!, 1e-9)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), snapshot.namedLists.getValue("x"))
        assertEquals(3, engine.linkedSeries(snapshot, 0, 1).points.size)
        assertTrue(engine.exportCsv(snapshot).startsWith("x,y"))

        val updated = engine.setCell(document, SpreadsheetAddress(1, 1), "5")
        val linked = engine.linkedSeries(engine.evaluate(updated), 0, 1)
        assertEquals(5.0, linked.points[1].y, 1e-9)
        assertTrue(linked.revision > snapshot.document.revision)
    }

    @Test
    fun statisticalProceduresReportAssumptionsDiagnosticsAndIntervals() {
        val groups = listOf(listOf(2.0, 3.0, 4.0), listOf(7.0, 8.0, 9.0), listOf(11.0, 12.0, 13.0))
        val first = Phase4Statistics.oneWayAnova(groups, permutations = 500, seed = 77)
        val second = Phase4Statistics.oneWayAnova(groups, permutations = 500, seed = 77)
        val chi = Phase4Statistics.chiSquare(listOf(listOf(20, 10), listOf(10, 20)))
        val nonParametric = Phase4Statistics.mannWhitney(listOf(1.0, 2.0, 3.0), listOf(6.0, 7.0, 8.0))
        val regression = Phase4Statistics.linearRegression(listOf(0.0, 1.0, 2.0, 3.0), listOf(1.0, 3.0, 5.0, 7.0))
        val logistic = Phase4Statistics.logisticRegression(listOf(-2.0, -1.0, 1.0, 2.0), listOf(0, 0, 1, 1))
        val bootstrap = Phase4Statistics.bootstrapMean(listOf(1.0, 2.0, 3.0, 4.0), 500, seed = 9)

        assertEquals(first.pValue, second.pValue, 0.0)
        assertTrue(first.assumptions.isNotEmpty() && first.diagnostics.isNotEmpty())
        assertTrue(chi.pValue in 0.0..1.0)
        assertTrue(nonParametric.pValue in 0.0..1.0)
        assertEquals(2.0, regression.coefficients[1], 1e-9)
        assertEquals(4, regression.intervals.size)
        assertTrue(regression.diagnostics.isNotEmpty())
        assertTrue(regression.diagnostics.any { it.name == "Residual mean" && it.passed })
        assertTrue(logistic.coefficients[1] > 0)
        assertEquals(9L, bootstrap.seed)
    }

    @Test
    fun probabilityExperimentsAreSeededAndBayesIsNormalized() {
        val first = RandomExperimentEngine.simulate(RandomExperimentKind.Die, 2_000, 42)
        val second = RandomExperimentEngine.simulate(RandomExperimentKind.Die, 2_000, 42)
        val piA = RandomExperimentEngine.monteCarloPi(10_000, 12)
        val piB = RandomExperimentEngine.monteCarloPi(10_000, 12)
        val bayes = ConditionalProbabilityEngine.bayes(mapOf("D" to .1, "not D" to .9), mapOf("D" to .9, "not D" to .05))
        val combinations = CombinatoricsLab.calculate(10, 3)
        val beta = ExtendedDistributionEngine.create(ExtendedDistributionKind.Beta, 2.0, 3.0)

        assertEquals(first, second)
        assertEquals(piA, piB)
        assertEquals(1.0, bayes.branches.sumOf { it.posterior }, 1e-12)
        assertEquals(120L, combinations.combinations)
        assertEquals(.4, beta.summary.mean!!, 1e-12)
        assertTrue(beta.density(.5) > 0)
    }
}
