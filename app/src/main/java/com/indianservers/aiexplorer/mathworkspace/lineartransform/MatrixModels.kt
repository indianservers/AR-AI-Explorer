package com.indianservers.aiexplorer.mathworkspace.lineartransform

import kotlin.math.abs

data class Vec2(val x:Double,val y:Double){operator fun times(k:Double)=Vec2(x*k,y*k);operator fun plus(v:Vec2)=Vec2(x+v.x,y+v.y)}
data class Matrix2(val a:Double,val b:Double,val c:Double,val d:Double){
    operator fun times(v:Vec2)=Vec2(a*v.x+b*v.y,c*v.x+d*v.y)
    operator fun times(m:Matrix2)=Matrix2(a*m.a+b*m.c,a*m.b+b*m.d,c*m.a+d*m.c,c*m.b+d*m.d)
    val determinant get()=a*d-b*c
    val trace get()=a+d
    fun transpose()=Matrix2(a,c,b,d)
    fun inverse():Matrix2? {
        val scale=listOf(abs(a),abs(b),abs(c),abs(d)).maxOrNull()?:return null
        if(!scale.isFinite()||scale==0.0)return null
        val na=a/scale;val nb=b/scale;val nc=c/scale;val nd=d/scale;val det=na*nd-nb*nc
        if(!det.isFinite()||det==0.0)return null
        val factor=1.0/(det*scale)
        fun z(v:Double)=if(v==0.0)0.0 else v
        return Matrix2(z(nd*factor),z(-nb*factor),z(-nc*factor),z(na*factor)).takeIf{it.finite()}
    }
    fun finite()=listOf(a,b,c,d).all{it.isFinite()}
}
enum class MatrixMode(val title:String){Transform("Transform"),Basis("Basis"),Eigen("Eigen"),Composition("Composition"),Tools("Matrix Tools")}
enum class ShapeKind(val title:String){Square("Unit square"),Triangle("Triangle"),Circle("Circle")}
