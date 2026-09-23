package com.indianservers.aiexplorer.mathworkspace.lineartransform

import kotlin.math.*

data class RealEigen(val value:Double,val vector:Vec2)
data class EigenResult(val trace:Double,val determinant:Double,val discriminant:Double,val real:Boolean,val eigenvalues:List<Double>,val vectors:List<Vec2>,val eigenspaceDimensions:List<Int>)
object EigenEngine {
    fun compute(m:Matrix2,tolerance:Double=1e-9):EigenResult{
        val tr=m.trace;val det=m.determinant;val disc=tr*tr-4*det
        if(disc < -tolerance)return EigenResult(tr,det,disc,false,emptyList(),emptyList(),emptyList())
        val root=sqrt(max(0.0,disc));val values=if(root<=tolerance)listOf(tr/2)else listOf((tr+root)/2,(tr-root)/2)
        val dimensions=values.map{lambda->if(values.size==1&&m.a==lambda&&m.d==lambda&&abs(m.b)<tolerance&&abs(m.c)<tolerance)2 else 1}
        val vectors=if(values.size==1&&dimensions.first()==2)listOf(Vec2(1.0,0.0),Vec2(0.0,1.0))else values.map{lambda->eigenvector(m,lambda,tolerance)}
        return EigenResult(tr,det,disc,true,values,vectors,dimensions)
    }
    private fun eigenvector(m:Matrix2,l:Double,eps:Double):Vec2{
        val x1=Vec2(m.b,l-m.a);val x2=Vec2(l-m.d,m.c)
        val v=if(x1.x*x1.x+x1.y*x1.y>=x2.x*x2.x+x2.y*x2.y)x1 else x2
        val n=hypot(v.x,v.y);return if(n<eps)Vec2(1.0,0.0)else Vec2(v.x/n,v.y/n)
    }
}
