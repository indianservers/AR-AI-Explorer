package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*
import androidx.compose.ui.graphics.Color

@Composable
fun ArtRenderer(vm:MathematicalArtViewModel){
    val layers by vm.layers;val viewport by vm.viewport;val grid by vm.showGrid;val axes by vm.showAxes;val origin by vm.showOrigin;val progress by vm.progress
    Canvas(Modifier.fillMaxSize().pointerInput(viewport){detectTransformGestures{centroid,pan,zoom,_->val s=(viewport.scale*zoom).coerceIn(1e-12,1e12);val unit=min(size.width,size.height)/4f*s.toFloat();vm.viewport.value=viewport.copy(scale=s,centerX=viewport.centerX-pan.x/unit,centerY=viewport.centerY+pan.y/unit)}}.pointerInput(Unit){detectTapGestures(onDoubleTap={vm.fitArtwork()})}){
        val cx=size.width/2f;val cy=size.height/2f;val factor=min(size.width,size.height)/4f*viewport.scale.toFloat()
        fun screen(p:ArtPoint)=Offset(cx+((p.x-viewport.centerX)*factor).toFloat(),cy-((p.y-viewport.centerY)*factor).toFloat())
        if(grid){val spacing=60f;var x=cx%spacing;while(x<size.width){drawLine(Color(0xffe9eff7),Offset(x,0f),Offset(x,size.height),1f);x+=spacing};var y=cy%spacing;while(y<size.height){drawLine(Color(0xffe9eff7),Offset(0f,y),Offset(size.width,y),1f);y+=spacing}}
        if(axes){drawLine(Color(0xffbdcbe0),Offset(0f,cy),Offset(size.width,cy),1.2f);drawLine(Color(0xffbdcbe0),Offset(cx,0f),Offset(cx,size.height),1.2f)}
        if(origin)drawCircle(Color(0xff95a7c2),3f,Offset(cx-viewport.centerX.toFloat()*factor,cy+viewport.centerY.toFloat()*factor))
        layers.filter{it.visible}.forEach{layer->val n=(layer.points.size*progress).toInt().coerceIn(0,layer.points.size);var path=Path();var previous:Offset?=null;var first:Offset?=null
            layer.points.take(n).forEach{p->if(!p.x.isFinite()||!p.y.isFinite()){if(!path.isEmpty)drawPath(path,layer.color,style=Stroke(width=2.5f));path=Path();previous=null;first=null}else{val q=screen(p);if(previous==null){path.moveTo(q.x,q.y);first=q}else path.lineTo(q.x,q.y);previous=q}}
            if(!path.isEmpty)drawPath(path,layer.color,style=Stroke(width=2.5f))
            if(vm.animate.value&&n>0&&n<layer.points.size)drawCircle(layer.color,5f,screen(layer.points[n-1]))
        }
    }
}
