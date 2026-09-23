package com.indianservers.aiexplorer.mathworkspace.lineartransform

object ShapeModels {
    fun points(kind:ShapeKind,count:Int=180):List<Vec2> = when(kind){
        ShapeKind.Square->listOf(Vec2(-.5,-.5),Vec2(.5,-.5),Vec2(.5,.5),Vec2(-.5,.5),Vec2(-.5,-.5))
        ShapeKind.Triangle->listOf(Vec2(0.0,.5),Vec2(-.433,-.25),Vec2(.433,-.25),Vec2(0.0,.5))
        ShapeKind.Circle->(0..count).map{i->val t=2*Math.PI*i/count;Vec2(kotlin.math.cos(t),kotlin.math.sin(t))}
    }
}
