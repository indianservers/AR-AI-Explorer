package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.assistant.workspace.RuhiIntersectionLanguage
import com.indianservers.aiexplorer.assistant.workspace.RuhiIntersectionMath
import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Query overlays share the existing axes and canvas; they never create duplicate mathematical objects. */
internal fun DrawScope.drawRuhiIntersectionOverlay(d:RuhiIntersectionLanguage.Drawing,tx:(Vec2)->Offset,minX:Double,maxX:Double,minY:Double,maxY:Double) {
    if(d.diagnostic.isNotEmpty())return
    val helperColor=WorkspaceVisualStyles.ReferenceCyan.copy(alpha=.5f)
    d.helpers.forEach{helper->when(val c=helper.curve){
        is RuhiIntersectionMath.Curve.Circle->{val center=tx(c.center);val radius=(tx(c.center+Vec2(c.radius,0.0))-center).getDistance();if(radius.isFinite())drawCircle(helperColor,radius,center,style=Stroke(2.5f))}
        is RuhiIntersectionMath.Curve.Vertical->drawLine(helperColor,tx(Vec2(c.x,minY)),tx(Vec2(c.x,maxY)),2.5f)
        is RuhiIntersectionMath.Curve.Line->{val delta=c.b-c.a;if(abs(delta.x)<1e-12)drawLine(helperColor,tx(Vec2(c.a.x,minY)),tx(Vec2(c.a.x,maxY)),2.5f)else{val m=delta.y/delta.x;drawLine(helperColor,tx(Vec2(minX,c.a.y+m*(minX-c.a.x))),tx(Vec2(maxX,c.a.y+m*(maxX-c.a.x))),2.5f)}}
        is RuhiIntersectionMath.Curve.Scalar->{val native="y=${c.expression}"+(c.restriction?.let{" {$it}"} ?: "");val sample=runCatching{TypedGraphEngine().sample(TypedGraphExpressionParser.parse(native),GraphDomain(minX,maxX),GraphDomain(minY,maxY,"y"),samples=256)}.getOrNull();sample?.curves?.forEach{segment->segment.points.zipWithNext().forEach{(a,b)->drawLine(helperColor,tx(a),tx(b),2.5f)}}}
        is RuhiIntersectionMath.Curve.Implicit->{val sample=runCatching{TypedGraphEngine().sample(TypedGraphExpression.Implicit("${c.residual}=0",c.residual,emptySet()),GraphDomain(minX,maxX),GraphDomain(minY,maxY,"y"),samples=720)}.getOrNull();sample?.implicitSegments?.forEach{segment->drawLine(helperColor,tx(segment.start),tx(segment.end),2.5f)}}
    }}
    val paint=android.graphics.Paint().apply{isAntiAlias=true;textSize=24f}
    d.points.forEachIndexed{i,p->
        val anchor=tx(p);if(!anchor.x.isFinite()||!anchor.y.isFinite()||anchor.x !in 0f..size.width||anchor.y !in 0f..size.height)return@forEachIndexed
        val color=if(i in d.highlighted)WorkspaceVisualStyles.ReferenceYellow else WorkspaceVisualStyles.ReferenceBlue
        drawCircle(color.copy(alpha=.22f),if(i in d.highlighted)20f else 15f,anchor);drawCircle(color,if(i in d.highlighted)10f else 7f,anchor)
        val text=d.labels[i];val width=(paint.measureText(text)+24f).coerceAtLeast(52f).coerceAtMost((size.width-8f).coerceAtLeast(1f))
        val x=(anchor.x+14f).coerceIn(4f,(size.width-width-4f).coerceAtLeast(4f));val y=(anchor.y+if(i%2==0)12f else -52f).coerceIn(4f,(size.height-42f).coerceAtLeast(4f))
        drawRoundRect(WorkspaceVisualStyles.ReferenceNavy,Offset(x,y),Size(width,38f),androidx.compose.ui.geometry.CornerRadius(9f,9f));paint.color=android.graphics.Color.argb(255,(color.red*255).toInt(),(color.green*255).toInt(),(color.blue*255).toInt());drawContext.canvas.nativeCanvas.drawText(text,x+12,y+26,paint)
    }
}
