package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.spatial.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class RuhiImplicitSurfaceTopologyTest {
    @Test fun gridAlignedImplicitPlaneHasOneAreaAndNoDuplicateFaces() {
        val mesh=TypedSurfaceMesher().mesh(SurfaceDefinition3D.Implicit("aligned","z=0",SurfaceDomain(-1.0..1.0,-1.0..1.0,-1.0..1.0)),6).geometry
        val triangles=mesh.triangles.chunked(3).map { indices -> indices.map(mesh.vertices::get) }
        val area=triangles.sumOf { (a,b,c) -> com.indianservers.aiexplorer.core.AnalyticGeometry3D.triangleArea(a,b,c) }
        assertEquals("The bounded z=0 plane must have area 4, with no doubled faces",4.0,area,1e-9)
        val keys=triangles.map { triangle -> triangle.map { it.toString() }.sorted().joinToString("|") }
        assertEquals("Coincident tetrahedron faces must be emitted once",keys.distinct().size,keys.size)
    }
    @Test fun implicitPlaneHasSingleCoverageAndConsistentOutwardWinding() {
        listOf(.13,0.0).forEach(::verifyPlane)
    }
    private fun verifyPlane(offset:Double) {
        val mesh=TypedSurfaceMesher().mesh(SurfaceDefinition3D.Implicit("plane","y+z=$offset",SurfaceDomain(-1.0..1.0,-1.0..1.0,-1.0..1.0)),6).geometry
        val triangles=mesh.triangles.chunked(3).map { indices -> indices.map(mesh.vertices::get) }
        assertTrue(triangles.isNotEmpty())
        for (ix in 0..12) for (iy in 0..12) {
            val x=-.919+ix*sqrt(2.0)*.039
            val y=-.731+iy*sqrt(3.0)*.027
            val covers=triangles.count { (a,b,c) ->
                val denominator=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y)
                if(abs(denominator)<1e-14) false else {
                    val alpha=((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/denominator
                    val beta=((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/denominator
                    alpha>=-1e-9 && beta>=-1e-9 && 1-alpha-beta>=-1e-9
                }
            }
            assertEquals("Plane probe ($x,$y,${offset-y}) must be covered once, without holes or overlapping triangles",1,covers)
        }
        triangles.forEach { (a,b,c) ->
            val normal=com.indianservers.aiexplorer.core.AnalyticGeometry3D.cross(b-a,c-a)
            assertTrue("Outward normals must point toward increasing y+z",normal.y+normal.z>0)
        }
    }
}
