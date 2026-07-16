package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import kotlin.math.PI
import org.junit.Assert.*
import org.junit.Test

class Phase2InteractiveMathTest {
    @Test fun circleAndWaveShareOneAngle() {
        val angle = InteractiveTrigEngine.angleFromPoint(0.0, 1.0)
        val snapshot = InteractiveTrigEngine.snapshot(angle)
        assertEquals(90.0, snapshot.degrees, 1e-9)
        assertEquals(1.0, snapshot.sine, 1e-9)
        assertNull(snapshot.tangent)
        assertEquals("1", snapshot.exactSine)
    }

    @Test fun transformedWaveUsesAmplitudePeriodPhaseAndShift() {
        val transform = TrigTransform(amplitude = 2.0, period = 4 * PI, phaseShift = 1.0, verticalShift = 3.0)
        assertEquals(3.0, transform.valueAt(1.0, TrigFunction.Sine), 1e-9)
        assertEquals(5.0, transform.valueAt(1.0 + PI, TrigFunction.Sine), 1e-9)
        assertEquals(121, InteractiveTrigEngine.waveSamples(transform, TrigFunction.Cosine, -PI, PI, 121).size)
    }

    @Test fun triangleSolversCoverSssSasAndAmbiguousSsa() {
        val sss = TriangleTrigSolver.sss(3.0, 4.0, 5.0)
        assertEquals(6.0, sss.area, 1e-9)
        assertEquals(90.0, sss.angleC, 1e-9)
        val sas = TriangleTrigSolver.sas(3.0, 4.0, 90.0)
        assertEquals(5.0, sas.c, 1e-9)
        assertEquals(2, TriangleTrigSolver.ssa(8.0, 10.0, 30.0).size)
        assertTrue(TriangleTrigSolver.ssa(2.0, 10.0, 70.0).isEmpty())
    }

    @Test fun identityLabUsesTrustedKernelAndDomains() {
        val pythagorean = InteractiveTrigIdentityLab().verify(0)
        assertTrue(pythagorean.evidence.equivalent)
        val tangent = InteractiveTrigIdentityLab().verify(1)
        assertTrue(tangent.evidence.equivalent)
        assertTrue(tangent.evidence.leftDomain.constraints.isNotEmpty())
    }

    @Test fun graphActionsAreValidatedReplayableAndUndoable() {
        val history = GraphActionHistory()
        history.dispatch(GraphWorkspaceAction.AddExpression("y=a*sin(x)"))
        assertTrue("a" in history.state.parameters)
        val row = history.state.rows.single()
        history.dispatch(GraphWorkspaceAction.ToggleVisibility(row.id))
        assertFalse(history.state.rows.single().visible)
        history.undo()
        assertTrue(history.state.rows.single().visible)
        history.redo()
        assertFalse(history.state.rows.single().visible)
    }

    @Test fun animatedGraphParameterWrapsInsideRange() {
        val reducer = GraphWorkspaceReducer()
        var state = reducer.reduce(GraphWorkspaceState(), GraphWorkspaceAction.AddExpression("a*x"))
        state = reducer.reduce(state, GraphWorkspaceAction.SetParameter("a", 9.9))
        state = reducer.reduce(state, GraphWorkspaceAction.AnimateParameter("a", true))
        state = reducer.animationFrame(state, 1.0)
        assertTrue(state.parameters.getValue("a").value in -10.0..10.0)
    }

    @Test fun adaptiveSamplerDoesNotBridgeReciprocalPole() {
        val sample = AdvancedGraphEngine().adaptiveExplicit(
            AdvancedGraphDefinition("1/x", AdvancedGraphKind.Explicit, GraphDomain(-2.0, 2.0)),
            maximumDepth = 12,
        )
        assertTrue(sample.discontinuities > 0)
        assertTrue(sample.segments.none { segment -> segment.points.any { it.x < 0 } && segment.points.any { it.x > 0 } })
    }

    @Test fun geometryIntersectsLinesCirclesAndEllipse() {
        val engine = DynamicGeometryEngine()
        var document = DynamicGeometryDocument()
        fun point(id: String, x: Double, y: Double) { document = engine.addPoint(document, DynamicPoint(id, DynamicPointRule.Free(Vec2(x, y))), "test") }
        point("O", 0.0, 0.0); point("R", 2.0, 0.0); point("A", -3.0, 0.0); point("B", 3.0, 0.0)
        point("F1", -1.0, 0.0); point("F2", 1.0, 0.0); point("E", 0.0, 2.0)
        val circle = DynamicGeometryObject.Circle("c", "O", "R")
        val line = DynamicGeometryObject.Line("l", "A", "B")
        val ellipse = DynamicGeometryObject.Ellipse("e", "F1", "F2", "E")
        document = engine.addObject(engine.addObject(engine.addObject(document, circle, "test"), line, "test"), ellipse, "test")
        val circleHits = engine.intersections(document, line, circle)
        assertEquals(listOf(-2.0, 2.0), circleHits.map { it.x }.sorted().map { kotlin.math.round(it * 1e8) / 1e8 })
        assertEquals(2, engine.intersections(document, line, ellipse).size)
    }
}
