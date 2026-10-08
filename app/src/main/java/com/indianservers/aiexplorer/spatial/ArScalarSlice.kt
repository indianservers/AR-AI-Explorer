package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

data class ArScalarSliceResult(val primitives:List<SpatialPrimitive>,val minimum:Double,val maximum:Double)
object ArScalarSlice {
    fun build(expression:String,z:Double,bound:Double,density:Int=20,contour:Double=0.0):ArScalarSliceResult {
        require(bound>0 && bound.isFinite() && z.isFinite() && density in 4..40)
        val compiled=ExpressionEngine().compile(expression)
        val stride=density+1
        val points=(0..density).flatMap { i -> (0..density).map { j -> Vec3(-bound+2*bound*i/density,-bound+2*bound*j/density,z) } }
        val values=points.map { p -> runCatching { compiled.eval(mapOf("x" to p.x,"y" to p.y,"z" to p.z)) }.getOrDefault(Double.NaN) }
        val valid=values.filter(Double::isFinite); require(valid.isNotEmpty()) { "Scalar field is undefined on this slice" }
        val min=valid.min(); val max=valid.max(); val bins=Array(16) { mutableListOf<Vec3>() }; val segments=mutableListOf<Vec3>()
        fun triangle(indices:List<Int>) {
            if(indices.any { !values[it].isFinite() }) return
            val average=indices.map { values[it] }.average(); val t=if(max-min<1e-12) .5 else (average-min)/(max-min)
            bins[(t*15).toInt().coerceIn(0,15)].addAll(indices.map { points[it] })
            val crossings=mutableListOf<Vec3>()
            for(i in 0..2) {
                val a=indices[i]; val b=indices[(i+1)%3]; val va=values[a]-contour; val vb=values[b]-contour
                if(va*vb<0) crossings+=points[a]+(points[b]-points[a])*(va/(va-vb))
                else if(abs(va)<1e-10 && abs(vb)>1e-10) crossings+=points[a]
            }
            val unique=crossings.distinct()
            if(unique.size==2) segments.addAll(unique)
        }
        for(i in 0 until density) for(j in 0 until density) { val a=i*stride+j; triangle(listOf(a,a+stride,a+1)); triangle(listOf(a+1,a+stride,a+stride+1)) }
        val primitives=bins.indices.filter { bins[it].isNotEmpty() }.map { i ->
            val t=i/15f
            SpatialPrimitive("scalar-slice-$i",SpatialPrimitiveKind.Surface,SpatialGeometry(bins[i],bins[i].indices.toList()),SpatialMaterial("Scalar color",listOf(t,.35f,1-t,.7f),blendMode=SpatialBlendMode.Transparent),"Scalar slice",selectable=false)
        }.toMutableList()
        if(segments.isNotEmpty()) primitives+=SpatialPrimitive("scalar-contour",SpatialPrimitiveKind.Curve,SpatialGeometry(segments,lines=(segments.indices step 2).map { it to it+1 }),SpatialMaterial("Scalar contour",listOf(1f,1f,1f,1f)),"Scalar contour = $contour",selectable=false)
        return ArScalarSliceResult(primitives,min,max)
    }
}
