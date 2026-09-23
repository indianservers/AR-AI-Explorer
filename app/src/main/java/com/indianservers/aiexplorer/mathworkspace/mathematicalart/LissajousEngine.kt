package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import kotlin.math.*

object LissajousEngine {
    fun sample(A:Double,B:Double,a:Double,b:Double,delta:Double,turns:Double=4.0,count:Int=5000):List<ArtPoint>{
        require(listOf(A,B,a,b,delta,turns).all(Double::isFinite)&&turns>0)
        val n=count.coerceIn(300,12000);return (0..n).map{i->val t=turns*2*PI*i/n;ArtPoint(A*sin(a*t+delta),B*sin(b*t))}
    }
}
