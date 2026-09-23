package com.indianservers.aiexplorer.mathworkspace.lineartransform

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlin.math.*

class MatrixTransformViewModel:ViewModel(){
    val mode=mutableStateOf(MatrixMode.Transform)
    val a=mutableStateOf("1");val b=mutableStateOf("0");val c=mutableStateOf("0");val d=mutableStateOf("1")
    val ba=mutableStateOf("1");val bb=mutableStateOf("1");val bc=mutableStateOf("0");val bd=mutableStateOf("1")
    val shape=mutableStateOf(ShapeKind.Square);val viewport=mutableStateOf(TransformViewport())
    val progress=mutableFloatStateOf(1f);val playing=mutableStateOf(false);val speed=mutableFloatStateOf(1f)
    val reverseOrder=mutableStateOf(false);val compare=mutableStateOf(false);val lockE1=mutableStateOf(false);val lockE2=mutableStateOf(false)
    val previewInverse=mutableStateOf(false)
    val baseGrid=mutableStateOf(true);val transformedGrid=mutableStateOf(true);val showAxes=mutableStateOf(true);val showLabels=mutableStateOf(true);val showOriginal=mutableStateOf(true);val showTransformed=mutableStateOf(true);val showBasis=mutableStateOf(true)
    val vectorX=mutableStateOf("1");val vectorY=mutableStateOf("0.5");val rhsX=mutableStateOf("1");val rhsY=mutableStateOf("2");val rotationDegrees=mutableStateOf("45")
    val sheet=mutableStateOf(com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop.Peek);val equationOpen=mutableStateOf(false);val eigenExpanded=mutableStateOf(false);val toolsExpanded=mutableStateOf(false)
    val target get()=listOf(a.value,b.value,c.value,d.value).map{it.toDoubleOrNull()}.takeIf{it.size==4&&it.all{v->v!=null&&v.isFinite()}}?.let{Matrix2(it[0]!!,it[1]!!,it[2]!!,it[3]!!)}
    val second get()=listOf(ba.value,bb.value,bc.value,bd.value).map{it.toDoubleOrNull()}.takeIf{it.size==4&&it.all{v->v!=null&&v.isFinite()}}?.let{Matrix2(it[0]!!,it[1]!!,it[2]!!,it[3]!!)}
    val source get()=target?:MatrixMathEngine.identity
    val bMatrix get()=second?:MatrixMathEngine.identity
    val result get()=if(reverseOrder.value)bMatrix*source else source*bMatrix
    val displayed get()=when(mode.value){MatrixMode.Composition->if(playing.value||progress.floatValue<1f)TransformAnimationEngine.compositionAt(source,bMatrix,progress.floatValue.toDouble(),reverseOrder.value)else result;else->if(previewInverse.value)target?.inverse()?:source else if(playing.value||progress.floatValue<1f)TransformAnimationEngine.interpolate(MatrixMathEngine.identity,source,progress.floatValue.toDouble())else source}
    val vector get()=Vec2(vectorX.value.toDoubleOrNull()?:1.0,vectorY.value.toDoubleOrNull()?:0.0)
    val rhs get()=Vec2(rhsX.value.toDoubleOrNull()?:0.0,rhsY.value.toDoubleOrNull()?:0.0)
    fun setMatrix(m:Matrix2){a.value=fmt(m.a);b.value=fmt(m.b);c.value=fmt(m.c);d.value=fmt(m.d);previewInverse.value=false;progress.floatValue=1f;fit()}
    fun setSecond(m:Matrix2){ba.value=fmt(m.a);bb.value=fmt(m.b);bc.value=fmt(m.c);bd.value=fmt(m.d)}
    fun dragBasis(index:Int,p:Vec2){if(index==0){if(lockE1.value)return;a.value=fmt(p.x);c.value=fmt(p.y)}else{if(lockE2.value)return;b.value=fmt(p.x);d.value=fmt(p.y)};progress.floatValue=1f}
    fun reset(){viewport.value=TransformViewport();fit()}
    fun fit(){val m=displayed;val w=max(abs(m.a)+abs(m.b),abs(m.c)+abs(m.d)).coerceAtLeast(1e-8);viewport.value=TransformViewport(0.0,0.0,(1.7/w).coerceIn(1e-14,1e10))}
    fun preset(p:MatrixPreset)=setMatrix(p.matrix)
    fun applyInverse(){target?.inverse()?.let(::setMatrix)}
    fun applyTranspose(){target?.transpose()?.let(::setMatrix)}
    fun applyRotation(){rotationDegrees.value.toDoubleOrNull()?.takeIf(Double::isFinite)?.let{setMatrix(MatrixMathEngine.rotation(it))}}
    fun applyComposition(){val combined=result;a.value=fmt(combined.a);b.value=fmt(combined.b);c.value=fmt(combined.c);d.value=fmt(combined.d);previewInverse.value=false;progress.floatValue=1f;setSecond(MatrixMathEngine.identity);fit()}
    fun setEntry(row:Int,col:Int,value:Double){val s=fmt(value);when(row*2+col){0->a.value=s;1->b.value=s;2->c.value=s;3->d.value=s};progress.floatValue=1f}
    companion object{fun fmt(v:Double)=if(!v.isFinite())"0"else if(abs(v)>=1e7||(abs(v)<1e-5&&v!=0.0))"%.4e".format(v)else "%.5g".format(v)}
}
data class TransformViewport(val centerX:Double=0.0,val centerY:Double=0.0,val scale:Double=1.0)
private fun fmt(v:Double)=MatrixTransformViewModel.fmt(v)
