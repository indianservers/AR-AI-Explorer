package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class ArSpaceRegressionTest {
    @Test fun everyLegacyArRouteUsesOneScreen() {
        ArSpaceNavigation.legacyRoutes.forEach { assertEquals(MathModule.SpatialAR, ArSpaceNavigation.canonicalRoute(it)) }
        assertEquals("AR Space", MathModule.SpatialAR.label)
        assertEquals(MathModule.Graph3D, ArSpaceNavigation.canonicalRoute(MathModule.Graph3D))
        assertEquals(7, ArMathWorkspaceMode.entries.size)
        assertEquals(1, MathCreationTools.count { it.title.startsWith("AR ") })
        assertTrue(MathCreationTools.any { it.title == "AR Space" })
        assertFalse(MathCreationTools.any { it.title == "AR Labs" || it.title == "AR 3D Graph" })
    }

    @Test fun coordinatePlaneUsesCanonicalPointsAndConstructions() {
        val state = WorkspaceState(points = listOf(Vec2(-2.0, 1.0), Vec2(3.0, 4.0)))
        val result = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.CoordinatePlane, state)
        assertEquals(ArMathWorkspaceMode.CoordinatePlane, result.mode)
        assertEquals(2, result.sourceObjectCount)
        assertTrue(result.scene.primitives.any { it.id == "point-0" })
        assertTrue(result.scene.primitives.any { it.id == "point-1" })
    }

    @Test fun vectorLabRendersVectorsWithoutUnrelatedSolids() {
        val state = WorkspaceState(solids = listOf(Solid(SolidType.Cube, width = 2.0)), vectors3D = listOf(Vector3D("a", Vec3(0.0, 0.0, 0.0), Vec3(2.0, 3.0, 1.0))))
        val result = ArMathWorkspaceBridge.build(ArMathWorkspaceMode.VectorLab, state)
        assertEquals(ArMathWorkspaceMode.VectorLab, result.mode)
        assertEquals(1, result.sourceObjectCount)
        assertTrue(result.scene.primitives.any { it.id == "vector-0" })
        assertFalse(result.scene.primitives.any { it.id == "solid-0" })
        assertEquals(Vec3(0.0, 0.0, 1.0), arSpaceCrossProduct(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)))
    }

    @Test fun studioCoordinatesRejectInvalidAndNonFiniteInput() {
        assertEquals(listOf(-2.5, 4.0), parseArSpaceCoordinates(" -2.5, 4 ", 2))
        assertNull(parseArSpaceCoordinates("", 2))
        assertNull(parseArSpaceCoordinates("1,2", 3))
        assertNull(parseArSpaceCoordinates("NaN,2", 2))
        assertNull(parseArSpaceCoordinates("Infinity,2,3", 3))
    }
}
