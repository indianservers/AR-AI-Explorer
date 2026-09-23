package com.indianservers.aiexplorer.mathworkspace.calculus

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import kotlin.math.abs

class CalculusViewModel {
    val mode=mutableStateOf(CalculusMode.Function)
    val expression=mutableStateOf("x^3 - 3x")
    val pointX=mutableStateOf("1")
    val lower=mutableStateOf("0")
    val upper=mutableStateOf("2")
    val limitAt=mutableStateOf("0")
    val showDerivative=mutableStateOf(false)
    val sheet=mutableStateOf(WorkspaceSheetStop.Peek)
    val viewport=mutableStateOf(FunctionViewport())
    val inspectedX=mutableStateOf<Double?>(null)
    val recent=mutableStateListOf("sin(x)","x^2","1/x")
    fun resetView(){viewport.value=FunctionViewport()}
    fun autoFit(f:(Double)->Double) {
        val v=viewport.value;val span=14.0/v.scaleX;val low=v.centerX-span/2;val high=v.centerX+span/2
        val ys=(0..500).mapNotNull { i -> f(low+(high-low)*i/500).takeIf { it.isFinite() && abs(it)<1e12 } }.sorted()
        if(ys.size<2)return
        val minY=ys[(ys.size*.02).toInt().coerceIn(0,ys.lastIndex)];val maxY=ys[(ys.size*.98).toInt().coerceIn(0,ys.lastIndex)]
        val range=(maxY-minY).coerceAtLeast(1e-10)
        viewport.value=v.copy(centerY=(minY+maxY)/2,scaleY=(14.0/range).coerceIn(1e-12,1e12))
    }
    fun rememberFunction(text:String){if(text.isNotBlank()){recent.remove(text);recent.add(0,text);while(recent.size>8)recent.removeAt(recent.lastIndex)}}
    fun dragPoint(x:Double){pointX.value=x.toString()}
    fun dragLower(x:Double){lower.value=x.toString()}
    fun dragUpper(x:Double){upper.value=x.toString()}
}
