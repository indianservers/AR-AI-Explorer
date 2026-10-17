package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.spatial.SurfaceDomain
import com.indianservers.aiexplorer.spatial.TypedSurfaceMesher

/** One mesh boundary shared by the visible graph canvas, Ruhi validation, and the audit runner. */
object GraphSurfaceGeometry {
    private val cache = object : LinkedHashMap<List<Any>,SurfaceMesh>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<List<Any>,SurfaceMesh>?) = size > 32
    }
    fun sceneDensity(requested: Int, visibleLayers: Int) = if (visibleLayers <= 1) requested else minOf(requested, (24.0 / kotlin.math.sqrt(visibleLayers.coerceAtLeast(1).toDouble())).toInt().coerceAtLeast(8))
    fun mesh(layer: SpatialSurfaceLayer, density: Int = 24): SurfaceMesh {
        val key=listOf(layer.kind,layer.expression,layer.expressionY,layer.expressionZ,layer.domain,density)
        val base = synchronized(cache) { cache[key] } ?: localMesh(layer,density).also { synchronized(cache) { cache[key]=it } }
        return if(layer.graphTransform == GraphTransform()) base else base.copy(vertices=base.vertices.map(layer.graphTransform::point))
    }
    private fun localMesh(layer: SpatialSurfaceLayer, density: Int): SurfaceMesh {
        val domain = SurfaceDomain(layer.domain.uMin..layer.domain.uMax, layer.domain.vMin..layer.domain.vMax, layer.domain.uMin..layer.domain.uMax)
        if (layer.kind == SpatialSurfaceKind.Explicit) return Graph3D().mesh(layer.expression, layer.domain.uMin, layer.domain.uMax, density,layer.domain.vMin,layer.domain.vMax)
        val definition = if (layer.kind == SpatialSurfaceKind.Implicit) SurfaceDefinition3D.Implicit(layer.id, layer.expression, domain)
            else SurfaceDefinition3D.Parametric(layer.id, layer.expression, layer.expressionY, layer.expressionZ, domain = domain)
        val geometry = TypedSurfaceMesher().mesh(definition, density.coerceIn(4, 64)).geometry
        return SurfaceMesh(geometry.vertices, 1, geometry.vertices.size.coerceAtLeast(1), geometry.triangles)
    }
}
