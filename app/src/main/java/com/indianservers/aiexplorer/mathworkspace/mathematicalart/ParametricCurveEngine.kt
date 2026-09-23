package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import com.indianservers.aiexplorer.mathworkspace.calculus.FunctionParser
import kotlin.math.*

object ParametricCurveEngine {
    fun sample(xExpression:String,yExpression:String,start:Double,end:Double,count:Int=2400):List<ArtPoint>{
        val x=FunctionParser.parse(xExpression.replace(Regex("\\bt\\b",RegexOption.IGNORE_CASE),"x"));val y=FunctionParser.parse(yExpression.replace(Regex("\\bt\\b",RegexOption.IGNORE_CASE),"x"))
        val n=count.coerceIn(100,12000);return (0..n).map{i->val t=start+(end-start)*i/n;val px=x.value(t);val py=y.value(t);if(px.isFinite()&&py.isFinite()&&abs(px)<1e12&&abs(py)<1e12)ArtPoint(px,py)else ArtPoint(Double.NaN,Double.NaN)}
    }
}
