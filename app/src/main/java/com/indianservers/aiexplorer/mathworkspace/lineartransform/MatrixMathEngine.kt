package com.indianservers.aiexplorer.mathworkspace.lineartransform

import kotlin.math.abs

object MatrixMathEngine {
    val identity=Matrix2(1.0,0.0,0.0,1.0)
    fun multiply(a:Matrix2,b:Matrix2)=a*b
    fun rank(m:Matrix2,tolerance:Double=1e-10):Int{val s=listOf(abs(m.a),abs(m.b),abs(m.c),abs(m.d)).maxOrNull()?:return 0;if(s==0.0)return 0;val det=(m.a/s)*(m.d/s)-(m.b/s)*(m.c/s);return if(abs(det)>tolerance)2 else 1}
    fun solve(m:Matrix2,b:Vec2):LinearSolveResult {m.inverse()?.let{return LinearSolveResult.Unique(it*b)};if(rank(m)==0)return if(abs(b.x)<1e-9&&abs(b.y)<1e-9)LinearSolveResult.Infinite else LinearSolveResult.None;val row1=abs(m.a*b.y-m.b*b.x);val row2=abs(m.c*b.y-m.d*b.x);return if(row1<1e-9&&row2<1e-9)LinearSolveResult.Infinite else LinearSolveResult.None}
    fun rotation(degrees:Double):Matrix2{val t=Math.toRadians(degrees);return Matrix2(kotlin.math.cos(t),-kotlin.math.sin(t),kotlin.math.sin(t),kotlin.math.cos(t))}
}
sealed interface LinearSolveResult{data class Unique(val value:Vec2):LinearSolveResult;data object Infinite:LinearSolveResult;data object None:LinearSolveResult}
