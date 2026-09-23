package com.indianservers.aiexplorer.mathworkspace.lineartransform

enum class Orientation { Preserved, Reversed, Collapsed }
object DeterminantEngine {
    fun orientation(matrix:Matrix2):Orientation{val s=listOf(kotlin.math.abs(matrix.a),kotlin.math.abs(matrix.b),kotlin.math.abs(matrix.c),kotlin.math.abs(matrix.d)).maxOrNull()?:return Orientation.Collapsed;if(s==0.0)return Orientation.Collapsed;val d=(matrix.a/s)*(matrix.d/s)-(matrix.b/s)*(matrix.c/s);return when{d>0->Orientation.Preserved;d<0->Orientation.Reversed;else->Orientation.Collapsed}}
    fun areaScale(matrix:Matrix2)=kotlin.math.abs(matrix.determinant)
}
