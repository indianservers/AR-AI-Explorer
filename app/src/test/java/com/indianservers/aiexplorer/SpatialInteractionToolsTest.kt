package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.EditableSectionPlane
import com.indianservers.aiexplorer.core.ProjectedSpatialMesh
import com.indianservers.aiexplorer.core.ProjectedSpatialPoint
import com.indianservers.aiexplorer.core.SpatialSubObjectPicker
import com.indianservers.aiexplorer.core.SpatialSubObjectType
import com.indianservers.aiexplorer.core.SurfaceAnalysisHandleEngine
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.SurfaceMesh
import com.indianservers.aiexplorer.core.CrossSection3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.TransformGizmoAxis
import com.indianservers.aiexplorer.core.TransformGizmoEngine
import com.indianservers.aiexplorer.core.TransformGizmoHandle
import com.indianservers.aiexplorer.core.TransformGizmoKind
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialInteractionToolsTest {
    private fun mesh(index: Int, depth: Double) = ProjectedSpatialMesh(
        index,
        listOf(
            ProjectedSpatialPoint(Vec2(0.0, 0.0), depth, Vec3(0.0, 0.0, depth)),
            ProjectedSpatialPoint(Vec2(100.0, 0.0), depth, Vec3(1.0, 0.0, depth)),
            ProjectedSpatialPoint(Vec2(0.0, 100.0), depth, Vec3(0.0, 1.0, depth)),
        ),
        listOf(0 to 1, 1 to 2, 2 to 0),
        listOf(listOf(0, 1, 2)),
    )

    @Test fun subObjectPickingUsesPolygonContainmentAndDepthTieBreaking() {
        val meshes = listOf(mesh(1, 4.0), mesh(0, 1.0))
        val face = SpatialSubObjectPicker.pick(meshes, Vec2(20.0, 20.0), SpatialSubObjectType.Face, 12.0)
        val edge = SpatialSubObjectPicker.pick(meshes, Vec2(50.0, 3.0), SpatialSubObjectType.Edge, 8.0)
        val vertex = SpatialSubObjectPicker.pick(meshes, Vec2(2.0, 2.0), SpatialSubObjectType.Vertex, 8.0)

        assertEquals(0, face?.solidIndex)
        assertEquals(0, edge?.solidIndex)
        assertEquals(0, vertex?.solidIndex)
    }

    @Test fun transformGizmoHitTestingAndAxisConstraintsAreDeterministic() {
        val handles = listOf(
            TransformGizmoHandle(TransformGizmoAxis.X, Vec2(0.0, 0.0), Vec2(100.0, 0.0)),
            TransformGizmoHandle(TransformGizmoAxis.Y, Vec2(0.0, 0.0), Vec2(0.0, 100.0)),
        )
        val hit = TransformGizmoEngine.hitTest(Vec2(70.0, 4.0), TransformGizmoKind.Move, handles)

        assertEquals(TransformGizmoAxis.X, hit?.axis)
        assertEquals(Vec3(3.0, 0.0, 0.0), TransformGizmoEngine.constrainTranslation(Vec3(3.0, 4.0, 5.0), TransformGizmoAxis.X))
        assertEquals(Vec3(0.0, 25.0, 0.0), TransformGizmoEngine.rotationDelta(TransformGizmoAxis.Y, 25.0))
    }

    @Test fun editableSectionPlaneMaintainsNormalizedGeometryAndBasis() {
        val plane = EditableSectionPlane(normal = Vec3(1.0, 1.0, 0.0)).moved(2.0)
        val (u, v) = plane.basis()

        assertEquals(1.0, plane.unitNormal.magnitude(), 1e-10)
        assertEquals(2.0, plane.offset, 1e-10)
        assertEquals(0.0, u.dot(plane.unitNormal), 1e-10)
        assertEquals(0.0, v.dot(plane.unitNormal), 1e-10)
    }

    @Test fun surfaceHandleSnapsToRenderedMeshAndReportsTangentPlane() {
        val mesh = SurfaceMesh(listOf(Vec3(-1.0, 0.0, 1.0), Vec3(1.0, 0.0, 1.0)), 1, 2)
        val point = SurfaceAnalysisHandleEngine.pick(mesh, listOf(Vec2(10.0, 10.0), Vec2(90.0, 10.0)), Vec2(84.0, 12.0), 12.0)
        val differential = SurfaceCalculus().analyze("x^2+y^2", 1.0, 2.0)

        assertEquals(mesh.vertices[1], point)
        assertNotNull(differential.tangentPlane)
        assertTrue(SurfaceAnalysisHandleEngine.tangentPlaneEquation(differential).contains("z−5"))
    }

    @Test fun arbitrarySectionPlaneReturnsAnOrderedPlanarLoop() {
        val section = CrossSection3D.intersect(SolidMeshFactory.create(Solid(SolidType.Cube, 2.0)), Vec3(1.0, 1.0, 0.0), 0.0)

        assertTrue(section.size >= 4)
        assertTrue(section.all { kotlin.math.abs(it.x + it.y) < 1e-9 })
        assertEquals(section.size, section.distinct().size)
    }
}
