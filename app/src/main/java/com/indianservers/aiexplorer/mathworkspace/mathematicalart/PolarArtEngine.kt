package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import com.indianservers.aiexplorer.mathworkspace.calculus.FunctionParser
import kotlin.math.*

object PolarArtEngine {
    fun sample(expression:String,start:Double,end:Double,count:Int=3600):List<ArtPoint>{
        val f=FunctionParser.parse(expression.replace("theta","x",ignoreCase=true));val n=count.coerceIn(200,12000)
        return (0..n).map{i->val t=start+(end-start)*i/n;val r=f.value(t);if(r.isFinite()&&abs(r)<1e9)ArtPoint(r*cos(t),r*sin(t))else ArtPoint(Double.NaN,Double.NaN)}
    }
    fun preset(name:String)=when(name){"Cardioid"->"1 + cos(theta)";"Limacon"->"1.4 + cos(theta)";"Spiral"->"theta/5";"Lemniscate"->"sqrt(cos(2*theta))";else->"2*sin(3*theta)"}
}
