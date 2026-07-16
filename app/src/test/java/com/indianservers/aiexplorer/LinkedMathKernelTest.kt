package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.LinkedMathKernel
import com.indianservers.aiexplorer.workspace.LinkedMathView
import com.indianservers.aiexplorer.workspace.ProbabilityLinkRequest
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedMathKernelTest {
    @Test
    fun functionObjectLinksCasAlgebraGraphAndTableViews() {
        val state = WorkspaceState(
            functions = listOf(FunctionDefinition("f", "f(x)", "x^2 - 4*x + 3", "cyan")),
        )
        val snapshot = LinkedMathKernel().snapshot(state, tableInputs = listOf(0.0, 1.0, 2.0), probability = null)
        val function = snapshot.objectByName("f(x)")!!

        assertTrue(LinkedMathView.CAS in function.views)
        assertTrue(LinkedMathView.Algebra in function.views)
        assertTrue(LinkedMathView.Graph in function.views)
        assertTrue(LinkedMathView.Table in function.views)
        assertEquals("quadratic polynomial", function.algebra!!.exactClassification)
        assertTrue(function.algebra.exactFacts.any { it == "Delta=4" })
        assertEquals(listOf("3", "0", "-1"), function.table.map { it.output.exact })
        assertTrue(function.graph!!.roots.map { it.exact }.containsAll(listOf("1", "3")))
    }

    @Test
    fun geometryObjectKeepsExactMeasurementProvenance() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(3.0, 4.0)),
            shapes = listOf(Shape2D("ab", Shape2DType.Segment, listOf(0, 1), "AB")),
            functions = emptyList(),
        )
        val shape = LinkedMathKernel().snapshot(state, probability = null).objectByName("AB")!!

        assertTrue(LinkedMathView.Geometry in shape.views)
        assertEquals("sqrt(25)", shape.geometry!!.measurements.getValue("distance").exact)
        assertEquals("3/2", shape.geometry.measurements.getValue("midpoint.x").exact)
        assertEquals("computed from parent points [0, 1]", shape.geometry.measurements.getValue("distance").provenance)
    }

    @Test
    fun probabilityObjectLinksDistributionToAlgebraGraphTableAndProbabilityViews() {
        val snapshot = LinkedMathKernel().snapshot(
            WorkspaceState(functions = emptyList(), shapes = emptyList()),
            probability = ProbabilityLinkRequest(DistributionKind.Normal, first = 0.0, second = 1.0, lower = -1.0, upper = 1.0),
        )
        val distribution = snapshot.objectsFor(LinkedMathView.Probability).single()

        assertTrue(LinkedMathView.Graph in distribution.views)
        assertTrue(LinkedMathView.Table in distribution.views)
        assertEquals("Normal distribution", distribution.name)
        assertEquals(0.0, distribution.probability!!.summary.mean, 1e-9)
        assertEquals(1.0, distribution.probability.summary.variance, 1e-9)
        assertEquals(12, distribution.table.size)
        assertTrue(distribution.probability.intervalProbability.decimal in 0.68..0.69)
    }
}
