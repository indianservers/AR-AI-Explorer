package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class WorldClassMathSuiteTest {
    @Test fun triangleCentersAndDivisionPointsRemainLiveDependencies() {
        val engine = DynamicGeometryEngine(); var document = DynamicGeometryDocument()
        document = engine.addPoint(document, DynamicPoint("A", DynamicPointRule.Free(Vec2(0.0, 0.0))), "test")
        document = engine.addPoint(document, DynamicPoint("B", DynamicPointRule.Free(Vec2(6.0, 0.0))), "test")
        document = engine.addPoint(document, DynamicPoint("C", DynamicPointRule.Free(Vec2(0.0, 8.0))), "test")
        document = engine.addPoint(document, DynamicPoint("D", DynamicPointRule.Section("A", "B", .25)), "test")
        document = engine.addPoint(document, DynamicPoint("G", DynamicPointRule.Centroid("A", "B", "C")), "test")
        document = engine.addPoint(document, DynamicPoint("O", DynamicPointRule.Circumcenter("A", "B", "C")), "test")
        document = engine.addPoint(document, DynamicPoint("I", DynamicPointRule.Incenter("A", "B", "C")), "test")
        document = engine.addPoint(document, DynamicPoint("H", DynamicPointRule.Orthocenter("A", "B", "C")), "test")
        val points = engine.resolve(document)
        assertEquals(Vec2(1.5, 0.0), points.getValue("D"))
        assertEquals(Vec2(2.0, 8.0 / 3.0), points.getValue("G"))
        assertEquals(Vec2(3.0, 4.0), points.getValue("O"))
        assertEquals(Vec2(0.0, 0.0), points.getValue("H"))
        val moved = engine.moveFreePoint(document, "B", Vec2(8.0, 0.0))
        assertNotEquals(points.getValue("G"), engine.resolve(moved).getValue("G"))
        assertEquals(listOf("A", "B", "C"), engine.inspectDependency(document, "O").direct)
    }

    @Test fun unifiedLanguageCoversDependenciesConicsTransformsAndConstraints() {
        val engine = UnifiedConstructionEngine(); var session = UnifiedConstructionSession()
        listOf(
            "point2d(A,0,0)", "point2d(B,4,0)", "point2d(C,0,3)",
            "line2d(base,A,B)", "circle(c,A,B)", "pointOn(P,c,0.25)",
            "centroid(G,A,B,C)", "circumcenter(O,A,B,C)", "incenter(I,A,B,C)", "orthocenter(H,A,B,C)",
            "parallel(par,C,A,B)", "perpendicular(perp,C,A,B)", "ellipse(e,A,B,C)",
            "translate(T,C,2,1)", "fixedLength(lengthAB,A,B,4)",
        ).forEach { session = engine.execute(session, it) }
        assertEquals(15, session.geometry.protocol.size)
        assertEquals(Vec2(2.0, 4.0), DynamicGeometryEngine().resolve(session.geometry).getValue("T"))
        assertTrue(DynamicGeometryEngine().evaluateConstraints(session.geometry).single().satisfied)
        assertTrue(engine.tokens(session).any { it.kind == StudioTokenKind.Conic })
        assertTrue(UnifiedConstructionCommandParser.catalog().getValue(MathStudioView.Geometry2D).size > 20)
    }

    @Test fun graphCalculusProducesAccurateDifferentialsAreasRootsAndLargeTables() {
        val calculus = CompetitiveGraphCalculus()
        val insight = calculus.differential("y=x^2-4*x+3", 2.0)
        assertEquals(-1.0, insight.point.y, 1e-9)
        assertEquals(0.0, insight.derivative, 1e-8)
        assertEquals(2.0, insight.secondDerivative, 1e-5)
        assertEquals("local minimum", insight.classification)
        val roots = calculus.roots("x^2-4*x+3", -2.0, 6.0)
        assertEquals(2, roots.size); assertEquals(1.0, roots[0], 1e-8); assertEquals(3.0, roots[1], 1e-8)
        val integral = calculus.integral("x", 0.0, 1.0)
        assertEquals(.5, integral.signedArea, 1e-9); assertEquals(.5, integral.geometricArea, 1e-9)
        val between = calculus.areaBetween("x", "x^2", 0.0, 1.0)
        assertEquals(1.0 / 6.0, between.geometricArea, 1e-8)
        assertEquals(10_000, calculus.table(listOf("x", "x^2"), -5.0, 5.0, 10_000).rows.size)
    }

    @Test fun surfaceCalculusFindsNormalsContoursAndGradientPathsWithoutGpu() {
        val surface = SurfaceDefinition3D.Explicit("bowl", "z=x^2+y^2")
        val calculus = CompetitiveSurfaceCalculus()
        val origin = calculus.differential(surface, 0.0, 0.0)
        assertEquals("local minimum", origin.classification)
        assertEquals(0.0, origin.unitNormal.x, 1e-12); assertEquals(0.0, origin.unitNormal.y, 1e-12); assertEquals(1.0, origin.unitNormal.z, 1e-12)
        val contour = calculus.contour(surface, 1.0, 48)
        assertTrue(contour.segments.size > 40)
        contour.segments.flatMap { listOf(it.first, it.second) }.forEach { assertEquals(1.0, it.z, 0.0) }
        val descent = calculus.gradientPath(surface, Vec2(1.0, 1.0), ascending = false, steps = 20)
        assertTrue(descent.last().z < descent.first().z)
    }

    @Test fun casNotebookSharesAssumptionsAndVerifiedAdvancedOperations() {
        val notebook = CompetitiveCasNotebook(); var state = CasNotebookState()
        state = notebook.assume(state, VariableAssumption("x", positive = true))
        state = notebook.evaluate(state, "s", CasNotebookOperation.Simplify, "sqrt(x^2)")
        state = notebook.evaluate(state, "f", CasNotebookOperation.Factor, "x^2-5*x+6")
        state = notebook.evaluate(state, "p", CasNotebookOperation.PartialFractions, "1/(x^2-1)")
        state = notebook.evaluate(state, "d", CasNotebookOperation.Derivative, "sin(x)*x^2")
        state = notebook.evaluate(state, "m", CasNotebookOperation.Determinant, "[[1,2],[3,4]]")
        state = notebook.evaluate(state, "o", CasNotebookOperation.Ode, "y'=2*y+1;y(0)=3")
        assertEquals("x", state.entries.first().result.exact)
        assertEquals("-2", state.entries.first { it.id == "m" }.result.exact)
        assertTrue(state.entries.all { it.result.supported && it.result.steps.isNotEmpty() })
    }

    @Test fun unifiedHistoryPersistenceLinksAndMacroRecordingRoundTrip() {
        val history = UnifiedConstructionHistory()
        history.execute("graph(f,y=x^2)"); history.execute("point2d(A,0,0)"); history.execute("point2d(B,2,0)"); history.execute("midpoint(M,A,B)")
        val inspector = CrossViewConstructionInspector(); history.apply(inspector.link(history.current, "result", setOf("f", "M")))
        val encoded = UnifiedConstructionSessionCodec.encode(history.current); val restored = UnifiedConstructionSessionCodec.decode(encoded)
        assertEquals(history.current.commands, restored.commands)
        assertEquals(history.current.links, restored.links)
        history.undo(); assertTrue(history.current.links.isEmpty()); history.redo(); assertEquals(1, history.current.links.size)
        val macro = ConstructionMacroRecorder.record(restored, "midpoint", setOf("A", "B", "M"))
        assertEquals(3, macro.commands.size)
        assertTrue(abs(CrossViewValueRegistry().with("distance", 5.0).with("scale", 2.0).evaluate("distance*scale") - 10.0) < 1e-12)
    }
}
