package com.indianservers.aiexplorer.mathworkspace.lineartransform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun TransformRenderer(vm:MatrixTransformViewModel){
    val viewport by vm.viewport;val baseGrid by vm.baseGrid;val transformedGrid by vm.transformedGrid;val axes by vm.showAxes;val original by vm.showOriginal;val transformed by vm.showTransformed;val basis by vm.showBasis;val mode by vm.mode;val compare by vm.compare
    Canvas(Modifier.fillMaxSize().semantics{contentDescription="Linear transformation canvas. Pan and pinch to navigate; drag the blue basis endpoints to edit matrix columns."}.pointerInput(Unit){
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false);var active=if(vm.showBasis.value&&vm.mode.value!=MatrixMode.Composition&&!vm.previewInverse.value)hitBasis(down.position,vm.displayed,vm.viewport.value,this.size,48.dp.toPx())else null
            var last=mapOf(down.id to down.position)
            do {
                val event=awaitPointerEvent();val pressed=event.changes.filter{it.pressed};val current=pressed.associate{it.id to it.position}
                val center=average(current.values);val oldCenter=average(pressed.mapNotNull{last[it.id]})
                if(pressed.size>=2){
                    active=null
                    val oldDistance=distance(pressed.mapNotNull{last[it.id]});val newDistance=distance(current.values)
                    if(oldDistance>1f&&newDistance>1f){val oldV=screenToWorld(oldCenter,vm.viewport.value,this.size);val factor=vm.viewport.value.scale*(newDistance/oldDistance);val unit=min(this.size.width,this.size.height)/4.0*factor;val cx=this.size.width/2.0;val cy=this.size.height/2.0;vm.viewport.value=vm.viewport.value.copy(scale=factor,centerX=oldV.x-(center.x-cx)/unit,centerY=oldV.y+(center.y-cy)/unit)}
                }else if(pressed.size==1){val change=pressed.first();val previous=last[change.id]?:change.position;val delta=change.position-previous
                    if(active!=null){val p=screenToWorld(change.position,vm.viewport.value,this.size);vm.dragBasis(active,p)}else{val v=vm.viewport.value;val unit=min(this.size.width,this.size.height)/4.0*v.scale;vm.viewport.value=v.copy(centerX=v.centerX-delta.x/unit,centerY=v.centerY+delta.y/unit)}
                }
                event.changes.forEach{it.consume()};last=current
            }while(pressed.isNotEmpty())
        }
    }){
        val width=size.width;val height=size.height;val ox=width/2f;val oy=height/2f;val minDim=min(width,height);val unit=minDim/4f*viewport.scale.toFloat();val matrix=vm.displayed;val target=vm.source
        fun point(p:Vec2)=Offset(ox+((p.x-viewport.centerX)*unit).toFloat(),oy-((p.y-viewport.centerY)*unit).toFloat())
        fun transformed(p:Vec2,matrix:Matrix2)=matrix*p
        val worldX=(width/unit).toDouble();val worldY=(height/unit).toDouble();val spacing=adaptiveStep(60f/unit).toDouble()
        if(baseGrid||transformedGrid){val stepsX=ceil(worldX/spacing).toInt().coerceAtMost(90);val stepsY=ceil(worldY/spacing).toInt().coerceAtMost(90)
            if(baseGrid){for(i in -stepsX..stepsX){val x=i*spacing;drawLine(Color(0xffe5edf7),point(Vec2(x,-worldY)),point(Vec2(x,worldY)),1f)};for(i in -stepsY..stepsY){val y=i*spacing;drawLine(Color(0xffe5edf7),point(Vec2(-worldX,y)),point(Vec2(worldX,y)),1f)}}
            if(transformedGrid){val maxStretch=max(abs(matrix.a)+abs(matrix.b),abs(matrix.c)+abs(matrix.d)).coerceAtLeast(1e-12);val transformedSpacing=adaptiveStep((60f/(unit*maxStretch.toFloat())).coerceAtLeast(1e-12f)).toDouble();val rangeX=worldX/maxOf(abs(matrix.a),abs(matrix.c),1e-12);val rangeY=worldY/maxOf(abs(matrix.b),abs(matrix.d),1e-12);val transformedStepsX=ceil(rangeX/transformedSpacing).toInt().coerceIn(1,90);val transformedStepsY=ceil(rangeY/transformedSpacing).toInt().coerceIn(1,90);val gridColor=Color(0xffb5c9e6)
                if(unit.toDouble()*transformedSpacing*hypot(matrix.a,matrix.c)>.5)for(i in -transformedStepsX..transformedStepsX){val x=i*transformedSpacing;drawLine(gridColor,point(matrix*Vec2(x,-worldY)),point(matrix*Vec2(x,worldY)),.9f)}
                if(unit.toDouble()*transformedSpacing*hypot(matrix.b,matrix.d)>.5)for(i in -transformedStepsY..transformedStepsY){val y=i*transformedSpacing;drawLine(gridColor,point(matrix*Vec2(-worldX,y)),point(matrix*Vec2(worldX,y)),.9f)}
            }
        }
        if(axes){drawLine(Color(0xff778da9),point(Vec2(-worldX,0.0)),point(Vec2(worldX,0.0)),1.5f);drawLine(Color(0xff778da9),point(Vec2(0.0,-worldY)),point(Vec2(0.0,worldY)),1.5f);drawLine(Color(0xff7aa6df),point(matrix*Vec2(-worldX,0.0)),point(matrix*Vec2(worldX,0.0)),1.2f);drawLine(Color(0xff7aa6df),point(matrix*Vec2(0.0,-worldY)),point(matrix*Vec2(0.0,worldY)),1.2f)}
        if(original)drawShape(ShapeModels.points(vm.shape.value),::point,Color(0xffa9b6c8),fill=Color.Transparent,stroke=1.5f)
        if(transformed){val color=if(DeterminantEngine.orientation(target)==Orientation.Reversed)Color(0xffd94b5b)else Color(0xff2878f0);val sq=ShapeModels.points(vm.shape.value).map{matrix*it};val fill=if(vm.shape.value==ShapeKind.Square)color.copy(alpha=.12f)else Color.Transparent;drawShape(sq,::point,color,fill,2.5f)}
        if(compare&&mode==MatrixMode.Composition){val ab=vm.source*vm.bMatrix;val ba=vm.bMatrix*vm.source;drawShape(ShapeModels.points(vm.shape.value).map{ab*it},::point,Color(0xff7552d9).copy(alpha=.7f),Color.Transparent,2f);drawShape(ShapeModels.points(vm.shape.value).map{ba*it},::point,Color(0xffd99422).copy(alpha=.8f),Color.Transparent,2f)}
        if(mode==MatrixMode.Eigen){val e=EigenEngine.compute(target);if(e.real)e.vectors.forEachIndexed{i,v->val col=Color(0xff7552d9).copy(alpha=if(i==0)1f else .55f);drawLine(col,point(v*(-max(worldX,worldY))),point(v*max(worldX,worldY)),2f)}}
        if(basis){
            val e1=Vec2(1.0,0.0);val e2=Vec2(0.0,1.0);val p1=matrix*e1;val p2=matrix*e2
            drawArrow(point(e1),point(Vec2(0.0,0.0)),Color(0xff17976d),2f);drawArrow(point(e2),point(Vec2(0.0,0.0)),Color(0xff13a777),2f)
            val transformedColor=if(DeterminantEngine.orientation(target)==Orientation.Reversed)Color(0xffd94b5b)else Color(0xff2878f0)
            drawArrow(point(p1),point(Vec2(0.0,0.0)),transformedColor,3f);drawArrow(point(p2),point(Vec2(0.0,0.0)),Color(0xff465fe0),3f)
            drawCircle(Color.White,14f,point(p1));drawCircle(transformedColor,11f,point(p1));drawCircle(Color.White,14f,point(p2));drawCircle(Color(0xff465fe0),11f,point(p2))
            if(vm.showLabels.value){
                fun label(text:String,at:Offset,tint:Int,dx:Float,dy:Float){drawContext.canvas.nativeCanvas.drawText(text,at.x+dx,at.y+dy,android.graphics.Paint(3).apply{color=tint;textSize=27f})}
                label("e₁",point(e1),android.graphics.Color.rgb(23,151,109),-30f,26f);label("e₂",point(e2),android.graphics.Color.rgb(19,167,119),8f,28f)
                if(hypot(p1.x-1.0,p1.y)>.05)label("Ae₁",point(p1),android.graphics.Color.rgb(40,120,240),8f,-8f)
                if(hypot(p2.x,p2.y-1.0)>.05)label("Ae₂",point(p2),android.graphics.Color.rgb(70,95,224),8f,-8f)
            }
        }
        if(vm.mode.value==MatrixMode.Tools){val v=vm.vector;val av=matrix*v;drawArrow(point(v),point(Vec2(0.0,0.0)),Color(0xff17976d),2.5f);drawArrow(point(av),point(Vec2(0.0,0.0)),Color(0xff2878f0),3f)}
    }
}

private fun DrawScope.drawShape(points:List<Vec2>,screen:(Vec2)->Offset,color:Color,fill:Color,stroke:Float){if(points.size<2)return;val path=Path();points.forEachIndexed{i,p->val q=screen(p);if(i==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)};path.close();if(fill.alpha>0f)drawPath(path,fill);drawPath(path,color,style=Stroke(stroke))}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(end:Offset,start:Offset,color:Color,width:Float){drawLine(color,start,end,width);val v=end-start;val length=hypot(v.x,v.y);if(length>1f){val u=v/length;val p=Offset(-u.y,u.x);val head=12f;drawLine(color,end,end-u*head+p*head*.55f,width);drawLine(color,end,end-u*head-p*head*.55f,width)}}
private fun hitBasis(pos:Offset,m:Matrix2,v:TransformViewport,size:IntSize,targetRadius:Float):Int?{val scale=min(size.width,size.height)/4f*v.scale.toFloat();val center=Offset(size.width/2f-v.centerX.toFloat()*scale,size.height/2f+v.centerY.toFloat()*scale);val pts=listOf(m*Vec2(1.0,0.0),m*Vec2(0.0,1.0));return pts.mapIndexed{index,p->val q=Offset(center.x+p.x.toFloat()*scale,center.y-p.y.toFloat()*scale);index to hypot(q.x-pos.x,q.y-pos.y)}.filter{it.second<targetRadius}.minByOrNull{it.second}?.first}
private fun screenToWorld(pos:Offset,v:TransformViewport,size:IntSize):Vec2{val unit=min(size.width,size.height)/4.0*v.scale;return Vec2(v.centerX+(pos.x-size.width/2.0)/unit,v.centerY-(pos.y-size.height/2.0)/unit)}
private fun average(points:Collection<Offset>):Offset=if(points.isEmpty())Offset.Zero else Offset(points.sumOf{it.x.toDouble()}.toFloat()/points.size,points.sumOf{it.y.toDouble()}.toFloat()/points.size)
private fun distance(points:Collection<Offset>):Float{if(points.size<2)return 0f;val p=points.toList();return hypot(p[0].x-p[1].x,p[0].y-p[1].y)}
private fun adaptiveStep(desired:Float):Float{if(!desired.isFinite()||desired<=0)return 1f;val pow=10.0.pow(floor(log10(desired.toDouble())));return listOf(1.0,2.0,5.0,10.0).map{it*pow}.first{it>=desired}.toFloat()}
