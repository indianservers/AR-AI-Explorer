package com.indianservers.aiexplorer.mathworkspace.lineartransform

object TransformAnimationEngine {
    fun interpolate(from:Matrix2,to:Matrix2,t:Double):Matrix2{val q=t.coerceIn(0.0,1.0);return Matrix2(from.a+(to.a-from.a)*q,from.b+(to.b-from.b)*q,from.c+(to.c-from.c)*q,from.d+(to.d-from.d)*q)}
    fun compositionAt(a:Matrix2,b:Matrix2,t:Double,reverse:Boolean=false):Matrix2{val first=if(reverse)a else b;val second=if(reverse)b else a;val p=t.coerceIn(0.0,1.0);return if(p<.5)interpolate(MatrixMathEngine.identity,first,p*2)else interpolate(first,second*first,(p-.5)*2)}
}
