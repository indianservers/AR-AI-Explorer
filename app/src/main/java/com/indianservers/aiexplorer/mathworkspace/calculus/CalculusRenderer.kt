package com.indianservers.aiexplorer.mathworkspace.calculus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import kotlin.math.*

@androidx.compose.runtime.Composable
fun CalculusRenderer(vm:CalculusViewModel, f:(Double)->Double, modifier:Modifier=Modifier) {
    val viewport=vm.viewport.value;val mode=vm.mode.value
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(P.snow).pointerInput(mode,viewport,vm.expression.value) {
        detectTransformGestures { centroid,pan,zoom,_ ->
            val spanX=14.0/viewport.scaleX;val spanY=14.0/viewport.scaleY
            val wx=viewport.centerX+(centroid.x-size.width/2)/size.width*spanX
            val targetMode=mode
            val shouldZoom = abs(zoom-1f)>.002f
            val target = when(targetMode){CalculusMode.Derivative->vm.pointX.value.toDoubleOrNull();CalculusMode.Integral->{val a=vm.lower.value.toDoubleOrNull()?:wx;val b=vm.upper.value.toDoubleOrNull()?:wx;if(abs(wx-a)<=abs(wx-b))a else b};CalculusMode.Limits->vm.limitAt.value.toDoubleOrNull();else->null}
            val nearTarget=target!=null&&abs(wx-target)<spanX*.08
            if(shouldZoom) vm.viewport.value=viewport.copy(centerX=viewport.centerX-pan.x/size.width*spanX,centerY=viewport.centerY+pan.y/size.height*spanY,scaleX=(viewport.scaleX*zoom).coerceIn(1e-12,1e12),scaleY=(viewport.scaleY*zoom).coerceIn(1e-12,1e12))
            else if(targetMode==CalculusMode.Derivative&&nearTarget) vm.dragPoint(wx)
            else if(targetMode==CalculusMode.Integral&&nearTarget) {
                val a=vm.lower.value.toDoubleOrNull()?:wx;val b=vm.upper.value.toDoubleOrNull()?:wx
                if(abs(wx-a)<=abs(wx-b))vm.dragLower(wx)else vm.dragUpper(wx)
            } else if(targetMode==CalculusMode.Limits&&nearTarget)vm.limitAt.value=wx.toString()
            else vm.viewport.value=viewport.copy(centerX=viewport.centerX-pan.x/size.width*spanX,centerY=viewport.centerY+pan.y/size.height*spanY,scaleX=(viewport.scaleX*zoom).coerceIn(1e-12,1e12),scaleY=(viewport.scaleY*zoom).coerceIn(1e-12,1e12))
        }
    }.pointerInput(mode,viewport) {
        detectTapGestures { pos ->
            val span=14.0/viewport.scaleX;val x=viewport.centerX+(pos.x-size.width/2)/size.width*span
            when(mode){CalculusMode.Derivative->vm.dragPoint(x);CalculusMode.Integral->{val a=vm.lower.value.toDoubleOrNull()?:x;val b=vm.upper.value.toDoubleOrNull()?:x;if(abs(x-a)<abs(x-b))vm.dragLower(x)else vm.dragUpper(x)};CalculusMode.Limits->vm.limitAt.value=x.toString();else->vm.inspectedX.value=x}
        }
    }) {
        val w=size.width.toDouble();val h=size.height.toDouble();val spanX=14.0/viewport.scaleX;val spanY=14.0/viewport.scaleY
        val x0=viewport.centerX-spanX/2;val x1=viewport.centerX+spanX/2;val y0=viewport.centerY-spanY/2;val y1=viewport.centerY+spanY/2
        fun sx(x:Double)=((x-x0)/spanX*w).toFloat()
        fun sy(y:Double)=(h-(y-y0)/spanY*h).toFloat()
        val step=tickStep(spanX)
        var xt=ceil(x0/step)*step
        while(xt<=x1){val x=sx(xt);drawLine(P.border.copy(alpha=.72f),Offset(x,0f),Offset(x,size.height),1f);drawContext.canvas.nativeCanvas.drawText(fmtTick(xt),x+2f,size.height-6f,android.graphics.Paint().apply{color=android.graphics.Color.rgb(101,122,155);textSize=10f});xt+=step}
        val yStep=tickStep(spanY);var yt=ceil(y0/yStep)*yStep
        while(yt<=y1){val y=sy(yt);drawLine(P.border.copy(alpha=.72f),Offset(0f,y),Offset(size.width,y),1f);drawContext.canvas.nativeCanvas.drawText(fmtTick(yt),3f,y-3f,android.graphics.Paint().apply{color=android.graphics.Color.rgb(101,122,155);textSize=10f});yt+=yStep}
        if(0.0 in y0..y1)drawLine(P.muted.copy(alpha=.8f),Offset(0f,sy(0.0)),Offset(size.width,sy(0.0)),1.25f)
        if(0.0 in x0..x1)drawLine(P.muted.copy(alpha=.8f),Offset(sx(0.0),0f),Offset(sx(0.0),size.height),1.25f)

        if(mode==CalculusMode.Integral){val a=vm.lower.value.toDoubleOrNull();val b=vm.upper.value.toDoubleOrNull();if(a!=null&&b!=null&&a.isFinite()&&b.isFinite()&&a!=b){
            val lo=max(min(a,b),x0);val hi=min(max(a,b),x1);val n=min(900,max(180,(w/1.5).roundToInt()));val baseline=sy(0.0)
            if(hi>lo){var active=Path();var priorSign=0;for(i in 0..n){val x=lo+(hi-lo)*i/n;val y=f(x);if(!y.isFinite()||abs(y)>1e8)continue;val sign=if(y>=0)1 else -1
                if(sign!=priorSign&&priorSign!=0){active.lineTo(sx(x),baseline);active.close();drawPath(active,if(priorSign>0)P.blue.copy(alpha=.18f)else P.red.copy(alpha=.18f));active=Path();active.moveTo(sx(x),baseline)}
                if(i==0||sign!=priorSign)active.lineTo(sx(x),baseline);active.lineTo(sx(x),sy(y));priorSign=sign
            };if(priorSign!=0){active.lineTo(sx(hi),baseline);active.close();drawPath(active,if(priorSign>0)P.blue.copy(alpha=.18f)else P.red.copy(alpha=.18f))}}
        }}
        val path=Path();var started=false;var lastY=0.0;val count=min(2400,max(400,w.roundToInt()))
        for(i in 0..count){val x=x0+spanX*i/count;val y=f(x);val py=sy(y)
            if(!y.isFinite()||!py.isFinite()||py < -size.height*1.5||py > size.height*2.5){started=false;continue}
            if(started&&abs(py-lastY)>size.height*.65f){started=false}
            if(!started){path.moveTo(sx(x),py);started=true}else path.lineTo(sx(x),py);lastY=py.toDouble()
        }
        drawPath(path,P.blue,style=Stroke(3f,cap=StrokeCap.Round))
        if(vm.showDerivative.value || mode==CalculusMode.Derivative){val d=Path();var start=false;var prev=0.0;for(i in 0..count){val x=x0+spanX*i/count;val y=CalculusMath.derivative(f,x);val py=sy(y);if(!py.isFinite()||py < -size.height||py>size.height*2){start=false;continue};if(start&&abs(py-prev)>size.height*.5f)start=false;if(!start){d.moveTo(sx(x),py);start=true}else d.lineTo(sx(x),py);prev=py.toDouble()};drawPath(d,P.violet,style=Stroke(2f,cap=StrokeCap.Round))}
        if(mode==CalculusMode.Derivative){val x=vm.pointX.value.toDoubleOrNull();if(x!=null&&x.isFinite()){val y=f(x);val slope=CalculusMath.derivative(f,x);if(y.isFinite()&&slope.isFinite()){val tangent=Path();tangent.moveTo(0f,sy(y+slope*(x0-x)));tangent.lineTo(size.width,sy(y+slope*(x1-x)));drawPath(tangent,P.green,style=Stroke(2f));drawCircle(P.violet,6f,Offset(sx(x),sy(y)))}}}
        listOf(vm.lower.value.toDoubleOrNull(),vm.upper.value.toDoubleOrNull()).forEachIndexed { i,x->if(mode==CalculusMode.Integral&&x!=null&&x in x0..x1){drawLine(P.blue,Offset(sx(x),0f),Offset(sx(x),size.height),1.3f);drawCircle(P.blue,6f,Offset(sx(x),sy(0.0).coerceIn(0f,size.height)))} }
        if(mode==CalculusMode.Limits){vm.limitAt.value.toDoubleOrNull()?.let{x->if(x in x0..x1)drawLine(P.amber,Offset(sx(x),0f),Offset(sx(x),size.height),1.7f)}}
        vm.inspectedX.value?.let{x->val y=f(x);if(x in x0..x1&&y.isFinite()&&y in y0..y1){drawLine(P.violet.copy(alpha=.65f),Offset(sx(x),0f),Offset(sx(x),size.height),1.1f);drawCircle(P.violet,5f,Offset(sx(x),sy(y)))}}
    }
}
private fun tickStep(span:Double):Double{val raw=span/8;val base=10.0.pow(floor(log10(raw.coerceAtLeast(1e-300))));val r=raw/base;return (if(r<1.5)1 else if(r<3.5)2 else if(r<7.5)5 else 10)*base}
private fun fmtTick(x:Double)=if(abs(x)>=1e5||(abs(x)<1e-3&&x!=0.0))"%.1e".format(x) else "%.3g".format(x)
