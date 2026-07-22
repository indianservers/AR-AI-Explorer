package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.DeleteShapesCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidsCommand
import com.indianservers.aiexplorer.workspace.GeometryConstraint2D
import com.indianservers.aiexplorer.workspace.GeometryConstraint2DType
import com.indianservers.aiexplorer.workspace.GeometryGroup2D
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeCatalogDeletionTest {
    @Test fun selected2DObjectsDeleteAtomicallyAndUndoRestoresMetadata() {
        val shapes = listOf(
            Shape2D("a", Shape2DType.Triangle, listOf(0,1,2), "Triangle"),
            Shape2D("b", Shape2DType.Polygon, listOf(2,3,4), "Star"),
            Shape2D("c", Shape2DType.Circle, listOf(4,5), "Circle"),
        )
        val groups = listOf(GeometryGroup2D("g","Group",setOf("a","b","c")))
        val constraints = listOf(GeometryConstraint2D("constraint",GeometryConstraint2DType.EqualLength,shapeIds=listOf("a","b")))
        val state = WorkspaceState(shapes=shapes,geometryGroups=groups,geometryConstraints=constraints)
        val command = DeleteShapesCommand(setOf(0,1),shapes,state.points,state.pointDependencies,groups,constraints)
        val deleted = command.apply(state)
        assertEquals(listOf("c"),deleted.shapes.map{it.id})
        assertEquals(setOf("c"),deleted.geometryGroups.single().shapeIds)
        assertTrue(deleted.geometryConstraints.isEmpty())
        val restored = command.undo(deleted)
        assertEquals(shapes,restored.shapes);assertEquals(groups,restored.geometryGroups);assertEquals(constraints,restored.geometryConstraints)
    }

    @Test fun selected3DObjectsDeleteTogetherAndUndoRestoresOrder() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Capsule,2.0,3.0,radius=.8),Solid(SolidType.Wedge,2.0,1.5,2.5))
        val state=WorkspaceState(solids=solids)
        val command=DeleteSolidsCommand(setOf(0,2),solids)
        val deleted=command.apply(state)
        assertEquals(listOf(SolidType.Capsule),deleted.solids.map{it.type})
        assertEquals(solids,command.undo(deleted).solids)
    }

    @Test fun explorerOwnedPointsAreRemovedAndRemainingIndicesAreRemapped() {
        val points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(0.0,1.0),Vec2(4.0,4.0),Vec2(5.0,4.0))
        val shapes=listOf(Shape2D("shape-explorer-triangle",Shape2DType.Triangle,listOf(0,1,2),"Triangle"),Shape2D("kept",Shape2DType.Segment,listOf(3,4),"Segment"))
        val state=WorkspaceState(points=points,shapes=shapes)
        val command=DeleteShapesCommand(setOf(0),shapes,points,emptyList(),emptyList(),emptyList())
        val deleted=command.apply(state)
        assertEquals(listOf(Vec2(4.0,4.0),Vec2(5.0,4.0)),deleted.points)
        assertEquals(listOf(0,1),deleted.shapes.single().pointIndices)
        assertEquals(points,command.undo(deleted).points)
    }

    @Test fun new3DShapesHaveFiniteMeasurementsMeshesAndPersistence() {
        val types=listOf(SolidType.OctagonalPrism,SolidType.TriangularPyramid,SolidType.Wedge,SolidType.Capsule)
        val solids=types.map{type->Solid(type,width=2.0,height=3.0,depth=2.2,radius=.8)}
        solids.forEach { solid ->
            val measure=Geometry3D.measure(solid);val mesh=SolidMeshFactory.create(solid)
            assertTrue(measure.volume>0&&measure.surfaceArea>0)
            assertTrue(mesh.vertices.isNotEmpty()&&mesh.faces.isNotEmpty())
            assertFalse(mesh.vertices.any{!it.x.isFinite()||!it.y.isFinite()||!it.z.isFinite()})
        }
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(WorkspaceState(solids=solids)),recover=false).state!!
        assertEquals(types,restored.solids.map{it.type})
    }
}
