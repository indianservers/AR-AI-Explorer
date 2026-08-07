package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Graph3DSurfaceRenderingTest {
    private val graph = Graph3D()

    @Test
    fun acceptanceSurfacesProduceCompleteFiniteGridTopology() {
        val expressions = listOf(
            "z=sin(x)+cos(y)",
            "z=sin(sqrt(x^2+y^2))/sqrt(x^2+y^2)",
            "z=x^2+y^2",
            "z=x^2-y^2",
            "z=sin(x*y)",
            "z=exp(-(x^2+y^2))",
            "z=sqrt(max(0,9-x^2-y^2))",
            "z=sin(x)*cos(y)",
        )

        expressions.forEach { expression ->
            val mesh = graph.mesh(expression, density = 24)
            assertEquals("$expression must retain rectangular topology", mesh.rows * mesh.columns, mesh.vertices.size)
            assertTrue("$expression must not contain missing surface vertices", mesh.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
        }
    }

    @Test
    fun newAndMigratedLayersDefaultToSurfaceWithMeshAndKeepExplicitModes() {
        val defaultLayer = SpatialSurfaceLayer("default", "z=x^2+y^2")
        val surface = defaultLayer.copy(renderMode = SpatialSurfaceRenderMode.Surface)
        val wireframe = defaultLayer.copy(renderMode = SpatialSurfaceRenderMode.Wireframe)

        assertEquals(SpatialSurfaceRenderMode.SurfaceMesh, defaultLayer.renderMode)
        assertEquals(SpatialSurfaceRenderMode.Surface, surface.renderMode)
        assertEquals(SpatialSurfaceRenderMode.Wireframe, wireframe.renderMode)
    }
}
