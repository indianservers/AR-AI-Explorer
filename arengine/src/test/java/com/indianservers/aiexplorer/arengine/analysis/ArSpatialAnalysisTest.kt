package com.indianservers.aiexplorer.arengine.analysis

import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArSpatialAnalysisTest {
    @Test
    fun tracePointRemainsConstrainedAndProducesDifferential() {
        val handle = ArSurfaceAnalysisEngine.constrain("surface", triangle(), ArVector3(.2, .2, 3.0))
        assertNotNull(handle)
        val differential = ArSurfaceAnalysisEngine.differential(triangle(), handle!!, ArVector3(1.0, 0.0, 1.0))
        assertEquals(0.0, differential.pointUnits.z, 1e-9)
        assertEquals(1.0, differential.unitNormal.magnitude(), 1e-9)
        assertEquals(0.0, differential.gradient.dot(differential.unitNormal), 1e-9)
    }

    @Test
    fun crossSectionAndContourProduceInspectableSegments() {
        val mesh = tetrahedron()
        val section = ArCrossSectionEngine.section(mesh, ArPlane(ArVector3(0.0, 0.0, .25), ArVector3(0.0, 0.0, 1.0)))
        assertTrue(section.isNotEmpty())
        assertTrue(section.first().perimeterUnits > 0.0)
        assertTrue(ArContourEngine.horizontal(mesh, .25).segments.isNotEmpty())
    }

    @Test
    fun gradientPathCanScrubAndEditWhileRemainingOnSurface() {
        val mesh = triangle()
        val path = ArGradientPathEngine.generate(mesh, ArVector3(.1, .1, 0.0), { ArVector3(1.0, 0.0, 0.0) }, true, steps = 4, stepUnits = .05)
        assertTrue(path.pointsUnits.size >= 2)
        assertEquals(1, ArGradientPathEngine.scrub(path, 1).currentStep)
        val edited = ArGradientPathEngine.edit(path, 0, ArVector3(.3, .3, 5.0), mesh)
        assertEquals(0.0, edited.pointsUnits.first().z, 1e-9)
    }

    @Test
    fun environmentalMeasurementsCarryCombinedUncertaintyWhileExactValuesDoNot() {
        val budget = ArUncertaintyBudget(.01, .02, listOf(.01, .01), .02)
        val estimate = ArMeasurementEngine.distance(ArVector3.Zero, ArVector3(1.0, 0.0, 0.0), "m", ArMeasurementTruth.EnvironmentalEstimate, budget)
        val exact = ArMeasurementEngine.distance(ArVector3.Zero, ArVector3(1.0, 0.0, 0.0), "u", ArMeasurementTruth.ExactMathematical, budget)
        assertTrue(estimate.uncertainty > .02)
        assertTrue(estimate.display.contains("educational estimate"))
        assertEquals(0.0, exact.uncertainty, 0.0)
        assertTrue(exact.display.contains("exact mathematical"))
    }

    @Test
    fun openSectionIsNotReportedAsArea() {
        val section = ArCrossSectionEngine.section(triangle(), ArPlane(ArVector3(.25, 0.0, 0.0), ArVector3(1.0, 0.0, 0.0)))
        assertFalse(section.first().closed)
        assertEquals(0.0, section.first().areaSquareUnits, 0.0)
    }

    private fun triangle() = ArMesh(
        vertices = listOf(ArVector3.Zero, ArVector3(1.0, 0.0, 0.0), ArVector3(0.0, 1.0, 0.0)),
        triangleIndices = listOf(0, 1, 2),
        lineIndices = listOf(0, 1, 1, 2, 2, 0),
    )

    private fun tetrahedron() = ArMesh(
        vertices = listOf(
            ArVector3(0.0, 0.0, 0.0),
            ArVector3(1.0, 0.0, 0.0),
            ArVector3(0.0, 1.0, 0.0),
            ArVector3(0.0, 0.0, 1.0),
        ),
        triangleIndices = listOf(0, 2, 1, 0, 1, 3, 1, 2, 3, 2, 0, 3),
    )
}
