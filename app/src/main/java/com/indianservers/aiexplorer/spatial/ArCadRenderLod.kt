package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.*

/** Visual-only clustering. Picking, constraints and mathematics retain the full source mesh. */
class ArCadRenderLod {
    private data class Entry(val source:SpatialGeometry,val tier:Int,val result:SpatialGeometry)
    private val cache=linkedMapOf<String,Entry>()
    fun scene(source:SpatialRenderScene,tier:Int):SpatialRenderScene {
        if(tier>=3) return source
        val visible=source.primitives.map { primitive ->
            if(primitive.id in setOf("cad-selection","cad-hover") || primitive.id.startsWith("analysis-") || primitive.geometry.vertices.size<256) primitive
            else {
                val old=cache[primitive.id]
                val result=if(old?.source===primitive.geometry && old.tier==tier) old.result else reduce(primitive.geometry,tier,primitive.kind==SpatialPrimitiveKind.VectorField).also { cache[primitive.id]=Entry(primitive.geometry,tier,it) }
                primitive.copy(geometry=result)
            }
        }
        cache.keys.retainAll(source.primitives.map { it.id }.toSet())
        while(cache.size>64) cache.remove(cache.keys.first())
        return source.copy(primitives=visible)
    }
    fun reduce(g:SpatialGeometry,tier:Int,field:Boolean=false):SpatialGeometry {
        require(tier in 0..3)
        if(tier==3 || g.vertices.size<256) return g
        if(field) {
            val stride=when(tier) { 0 -> 4; 1 -> 3; else -> 2 }
            val groups=(0 until g.vertices.size/4 step stride).toList(); val vertices=groups.flatMap { g.vertices.subList(it*4,it*4+4) }
            return g.copy(vertices=vertices,lines=groups.indices.flatMap { val i=it*4; listOf(i to i+1,i+1 to i+2,i+1 to i+3) })
        }
        // Curves retain exact continuity; clustering is only used for triangulated surfaces.
        if(g.triangles.isEmpty()) return g
        val low=Vec3(g.vertices.minOf { it.x },g.vertices.minOf { it.y },g.vertices.minOf { it.z })
        val high=Vec3(g.vertices.maxOf { it.x },g.vertices.maxOf { it.y },g.vertices.maxOf { it.z })
        val extent=maxOf(high.x-low.x,high.y-low.y,high.z-low.z)
        if(extent<1e-12) return g
        val cells=when(tier) { 0 -> 12; 1 -> 20; else -> 32 }; val step=extent/cells
        val buckets=linkedMapOf<Triple<Long,Long,Long>,Int>(); val sums=mutableListOf<Vec3>(); val counts=mutableListOf<Int>()
        val remap=g.vertices.map { v ->
            val key=Triple(floor((v.x-low.x)/step).toLong(),floor((v.y-low.y)/step).toLong(),floor((v.z-low.z)/step).toLong())
            val index=buckets.getOrPut(key) { sums.add(Vec3(0.0,0.0,0.0)); counts.add(0); sums.lastIndex }
            sums[index]=sums[index]+v; counts[index]++; index
        }
        val vertices=sums.indices.map { sums[it]*(1.0/counts[it]) }
        val faces=g.triangles.chunked(3).map { f -> f.map { remap[it] } }.filter { it.toSet().size==3 }.distinct()
        val lines=g.lines.map { remap[it.first] to remap[it.second] }.filter { it.first!=it.second }.distinct()
        return g.copy(vertices=vertices,triangles=faces.flatten(),lines=lines)
    }
}
