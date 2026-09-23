package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import kotlin.math.*

object SpirographEngine {
    fun sample(R:Double,r:Double,d:Double,type:SpiroType,phase:Double,rotations:Double,count:Int=5000):List<ArtPoint>{
        require(R.isFinite()&&r.isFinite()&&d.isFinite()&&r!=0.0&&rotations.isFinite()&&rotations>0)
        val end=rotations*2*PI
        val n=count.coerceIn(200,12000)
        return (0..n).map{i->val t=end*i/n+phase;val q=when(type){SpiroType.Hypotrochoid->(R-r);SpiroType.Epitrochoid->(R+r)};val u=q*t/r
            when(type){SpiroType.Hypotrochoid->ArtPoint(q*cos(t)+d*cos(u),q*sin(t)-d*sin(u));SpiroType.Epitrochoid->ArtPoint(q*cos(t)-d*cos(u),q*sin(t)-d*sin(u))}}
    }
}
