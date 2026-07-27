package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceBridge
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode
import com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArMathWorkspaceBridgeTest {
    @Test
    fun twoDimensionalGeometryKeepsCanonicalShapeIdsAndPointHandles() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(3.0, 0.0), Vec2(1.0, 2.0)),
            shapes = listOf(Shape2D("triangle-abc", Shape2DType.Triangle, listOf(0, 1, 2), "ABC")),
        )

        val result = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.Geometry2D, state)

        assertEquals(4, result.visualizedObjectCount)
        assertTrue(result.scene.primitives.any { it.id == "triangle-abc" && it.kind == SpatialPrimitiveKind.Surface })
        assertTrue((0..2).all { index -> result.scene.primitives.any { it.id == "point-$index" } })
    }

    @Test
    fun graphModeSamplesEveryVisibleFunctionUsingItsExistingId() {
        val state = WorkspaceState(
            functions = listOf(
                FunctionDefinition("curve-f", "f(x)", "x^2 - 1", "cyan"),
                FunctionDefinition("hidden-g", "g(x)", "x + 2", "violet", visible = false),
            ),
        )

        val result = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.Graph2D, state)

        assertEquals(1, result.sourceObjectCount)
        assertEquals(1, result.visualizedObjectCount)
        assertTrue(result.scene.primitives.first { it.id == "curve-f" }.geometry.lines.size > 100)
        assertTrue(result.scene.primitives.none { it.id == "hidden-g" })
    }

    @Test
    fun geometry3DReusesExistingSolidRendererAndSelectionIds() {
        val state = WorkspaceState(solids = listOf(Solid(SolidType.Cube, width = 2.0)))

        val result = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.Geometry3D, state)

        assertEquals(1, result.sourceObjectCount)
        assertTrue(result.scene.primitives.any { it.id == "solid-0" && it.kind == SpatialPrimitiveKind.Solid })
    }

    @Test
    fun casModeVisualizesGraphableSymbolicAndCoordinateObjects() {
        val function = UniversalMathObjectFactory.symbolic("cas-f", UniversalMathKind.Function, "f", "x^2", sourceView = "CAS")
        val point = UniversalMathObjectFactory.point2D("cas-a", "A", 2.0, 3.0)
        val document = UniversalMathDocument(objects = mapOf(function.id to function, point.id to point))

        val result = ArMathWorkspaceBridge.build(
            ArMathWorkspaceMode.CAS,
            WorkspaceState(universalMathDocument = document),
            universalDocument = document,
        )

        assertEquals(2, result.sourceObjectCount)
        assertTrue(result.scene.primitives.any { it.id == "cas-f" && it.kind == SpatialPrimitiveKind.Curve })
        assertTrue(result.scene.primitives.any { it.id == "cas-a" && it.kind == SpatialPrimitiveKind.Point })
    }
}
