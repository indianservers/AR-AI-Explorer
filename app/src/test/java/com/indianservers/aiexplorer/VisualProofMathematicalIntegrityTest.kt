package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.VisualEvidenceType
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualProofMathematicalIntegrityTest {
    private val engine = VisualProofEngine()

    @Test
    fun approximationProofsExposeNonZeroErrorAndCertifiedConvergence() {
        val circleArea = engine.start("circle-area").frame
        assertTrue(circleArea.residual > 0.0)
        assertTrue(circleArea.residual <= circleArea.measurements.getValue("certified area error bound") + 1e-9)

        val coarseRatio = engine.setParameter(engine.start("circle-ratio"), "n", 6.0).frame
        val fineRatio = engine.setParameter(engine.start("circle-ratio"), "n", 240.0).frame
        assertTrue(coarseRatio.residual > fineRatio.residual)
        assertTrue(fineRatio.measurements.getValue("perimeter/diameter") > coarseRatio.measurements.getValue("perimeter/diameter"))
    }

    @Test
    fun coordinateAreaProofsReactToGeometryAndAgreeWithFormula() {
        listOf("matrix-transform", "shear-area", "triangle-area", "parallelogram-area", "trapezoid-area").forEach { id ->
            assertTrue("$id independently agrees at its initial values", engine.start(id).frame.holds)
        }
        val movedTriangle = engine.setParameter(engine.start("triangle-area"), "apex", 5.5).frame
        assertTrue(movedTriangle.holds)
        assertEquals(6.0, movedTriangle.measurements.getValue("coordinate triangle area"), 1e-9)
    }

    @Test
    fun circleAnglesAreMeasuredFromAllThreeExposedControls() {
        val start = engine.start("circle-angle")
        val movedArc = engine.setParameter(start, "arc", 140.0).frame
        val movedPoint = engine.setParameter(engine.start("circle-angle"), "c", 200.0).frame
        val movedRadius = engine.setParameter(engine.start("circle-angle"), "r", 5.0).frame
        assertTrue(movedArc.holds)
        assertTrue(movedPoint.holds)
        assertTrue(movedRadius.holds)
        assertEquals(140.0, movedArc.measurements.getValue("measured center angle"), 1e-7)
    }

    @Test
    fun logicalProofsEvaluateBothSidesInsteadOfHardcodingSuccess() {
        val absolute = engine.setParameter(engine.setParameter(engine.start("absolute-inequality"), "x", 4.0), "r", 3.0).frame
        assertEquals(0.0, absolute.measurements.getValue("|x|≤r"), 0.0)
        assertEquals(0.0, absolute.measurements.getValue("−r≤x≤r"), 0.0)
        assertTrue(absolute.holds)

        val deMorgan = engine.setParameter(engine.setParameter(engine.start("set-de-morgan"), "inA", 1.0), "inB", 0.0).frame
        assertEquals(4.0, deMorgan.measurements.getValue("matching truth rows"), 0.0)
        assertEquals(0.0, deMorgan.measurements.getValue("selected left expression"), 0.0)
        assertEquals(0.0, deMorgan.measurements.getValue("selected right expression"), 0.0)
    }

    @Test
    fun eigenvectorExplorerCanShowARealCounterexample() {
        val eigenvector = engine.start("eigenvector-direction").frame
        val generalVector = engine.setParameter(engine.start("eigenvector-direction"), "vy", 1.0).frame
        assertTrue(eigenvector.holds)
        assertFalse(generalVector.holds)
        assertTrue(generalVector.measurements.getValue("normalized turn") > 0.0)
    }

    @Test
    fun modularEquivalenceUsesIndependentIntegers() {
        val congruent = engine.start("modular-clock").frame
        val notCongruent = engine.setParameter(congruent.let { engine.start("modular-clock") }, "b", 6.0).frame
        assertEquals(1.0, congruent.measurements.getValue("same clock position"), 0.0)
        assertEquals(1.0, congruent.measurements.getValue("n divides a−b"), 0.0)
        assertEquals(0.0, notCongruent.measurements.getValue("same clock position"), 0.0)
        assertEquals(0.0, notCongruent.measurements.getValue("n divides a−b"), 0.0)
        assertTrue(notCongruent.holds)
    }

    @Test
    fun evidenceTypesAndCorrectedCategoriesAreExplicit() {
        assertEquals(VisualEvidenceType.DataCounterexample, VisualProofCatalog.labs.single { it.id == "anscombe-quartet" }.evidenceType)
        assertEquals(VisualEvidenceType.ApproximationProof, VisualProofCatalog.labs.single { it.id == "integral-area" }.evidenceType)
        assertEquals(setOf("set-de-morgan", "counting-paths"), VisualProofCatalog.labsFor("Discrete Mathematics").map { it.id }.toSet())
    }
}
