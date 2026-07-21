package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.SpatialConstructionEngine
import org.junit.Assert.*
import org.junit.Test

class CompetitiveMathParityTest {
    @Test fun phaseOneCommandsCreateLinkedGraphGeometryAndSpatialObjects() {
        val engine = UnifiedConstructionEngine()
        var session = UnifiedConstructionSession()
        listOf(
            "graph(f,y=a*x^2)",
            "point2d(A,0,0)", "point2d(B,2,0)", "midpoint(M,A,B)",
            "line2d(base,A,B)", "circle(unit,A,M)",
            "point3d(P,0,0,0)", "point3d(Q,1,0,0)", "point3d(R,0,1,0)",
            "vector3d(v,P,Q)", "line3d(axis,P,Q)", "plane3d(floor,P,Q,R)",
            "surface(bowl,z=x^2+y^2)", "implicitSurface(sphere,x^2+y^2+z^2=4)",
        ).forEach { session = engine.execute(session, it) }

        assertEquals(Vec2(1.0, 0.0), DynamicGeometryEngine().resolve(session.geometry).getValue("M"))
        assertTrue(session.graph.parameters.containsKey("a"))
        assertEquals(2, session.surfaces.size)
        assertEquals(6, SpatialConstructionEngine().resolve(session.spatial).size)
        assertEquals(setOf(MathStudioView.Graph, MathStudioView.Geometry2D, MathStudioView.Spatial3D), engine.tokens(session).map { it.view }.toSet())
        assertEquals(14, session.commands.size)
    }

    @Test fun phaseOneCatalogExposesAllThreeWorkspacesAndSurfaceForms() {
        val catalog = UnifiedConstructionCommandParser.catalog()
        assertEquals(MathStudioView.entries.toSet(), catalog.keys)
        assertTrue(catalog.getValue(MathStudioView.Spatial3D).any { it.startsWith("implicitSurface") })
        assertTrue(catalog.getValue(MathStudioView.Spatial3D).any { it.startsWith("parametricSurface") })
        assertTrue(catalog.getValue(MathStudioView.Geometry2D).any { it.startsWith("midpoint") })
    }

    @Test fun phaseTwoSelectionInspectionAndKeyboardOrderAreDeterministic() {
        val engine = UnifiedConstructionEngine()
        var session = UnifiedConstructionSession()
        listOf("graph(f,y=x^2)", "point2d(A,0,0)", "point2d(B,4,0)", "midpoint(M,A,B)")
            .forEach { session = engine.execute(session, it) }
        val inspector = CrossViewConstructionInspector(engine)
        session = inspector.link(session, "linked-result", setOf("f", "M"))

        val selection = inspector.select(session, "M")
        assertEquals(setOf("f", "M"), selection.selected)
        assertEquals(setOf(MathStudioView.Graph, MathStudioView.Geometry2D), selection.views)
        val dependency = inspector.inspect(session, "M")
        assertEquals(listOf("A", "B"), dependency.direct)
        assertEquals(setOf("A", "B"), dependency.transitive)
        assertTrue(dependency.valid)
        assertEquals(listOf("f", "A", "B", "M"), inspector.keyboardOrder(session).map { it.id })
    }

    @Test fun phaseTwoSnappingProvidesPointGridAngleAndAxisTargets() {
        val point = ProductionSnapEngine.snap2D(Vec2(1.04, .03), mapOf("A" to Vec2(1.0, 0.0)))
        assertEquals(ProductionSnapKind.ExistingPoint, point.kind)
        assertEquals("A", point.targetId)
        val grid = ProductionSnapEngine.snap2D(Vec2(2.08, 2.09), emptyMap())
        assertEquals(ProductionSnapKind.Grid, grid.kind)
        val angle = ProductionSnapEngine.snap2D(Vec2(.99, .03), emptyMap(), grid = 10.0, angleOrigin = Vec2(0.0, 0.0))
        assertEquals(ProductionSnapKind.Angle, angle.kind)
        val axis = ProductionSnapEngine.snap3D(Vec3(2.0, .02, .03))
        assertEquals(ProductionSnapKind.Axis, axis.kind)
    }

    @Test fun phaseTwoMacrosReplayReusableConstructionProtocols() {
        val macro = ConstructionMacro(
            name = "segment midpoint",
            parameters = listOf("x1", "y1", "x2", "y2"),
            commands = listOf(
                "point2d({{prefix}}A,{{x1}},{{y1}})",
                "point2d({{prefix}}B,{{x2}},{{y2}})",
                "line2d({{prefix}}line,{{prefix}}A,{{prefix}}B)",
                "midpoint({{prefix}}M,{{prefix}}A,{{prefix}}B)",
            ),
        )
        val session = ConstructionMacroEngine.replay(
            UnifiedConstructionSession(), macro,
            mapOf("x1" to "0", "y1" to "0", "x2" to "6", "y2" to "2"),
            prefix = "demo",
        )
        assertEquals(Vec2(3.0, 1.0), DynamicGeometryEngine().resolve(session.geometry).getValue("demoM"))
        assertEquals(4, session.commands.size)
    }
}
