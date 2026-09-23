package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import kotlin.math.*

object RoseCurveEngine {
    fun sample(a:Double,k:Double,phase:Double,sine:Boolean,turns:Double=8.0,count:Int=5000):List<ArtPoint>{
        require(listOf(a,k,phase,turns).all(Double::isFinite)&&turns>0)
        val n=count.coerceIn(300,12000);return (0..n).map{i->val t=turns*2*PI*i/n;val r=a*(if(sine)sin(k*t+phase)else cos(k*t+phase));ArtPoint(r*cos(t),r*sin(t))}
    }
    fun typicalPetals(k:Double):Int?=if(k.isFinite()&&k>0&&abs(k-round(k))<1e-9){val i=round(k).toInt();if(i%2==0)2*i else i}else null
}
