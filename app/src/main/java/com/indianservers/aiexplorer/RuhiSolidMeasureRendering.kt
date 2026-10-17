package com.indianservers.aiexplorer

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.assistant.workspace.RuhiSolidMeasureLanguage
import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Dimension endpoints use the same object pose and camera transform as the native solid. */
internal fun DrawScope.drawRuhiSolidMeasurements(d:RuhiSolidMeasureLanguage.Drawing,solids:List<Solid>,visible:Set<Int>,unit:com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Context,screen:(Vec3)->Offset) {
    val color=WorkspaceVisualStyles.ReferenceYellow
    val occupied=mutableListOf<Rect>()
    // The existing orientation control is centered on the right edge of this canvas.
    occupied+=Rect(Offset(size.width-100.dp.toPx(),size.height*.5f-65.dp.toPx()),Size(100.dp.toPx(),130.dp.toPx()))
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{textSize=25f;this.color=Color.White.toArgb()}
    fun label(text:String,anchor:Offset) {
        val width=(paint.measureText(text)+20f).coerceAtMost(size.width-16f)
        val x=anchor.x.coerceIn(8f,(size.width-width-8f).coerceAtLeast(8f))
        val y=anchor.y.coerceIn(8f,(size.height-44f).coerceAtLeast(8f))
        val yy=(sequenceOf(y)+(1..30).asSequence().flatMap{sequenceOf(y+it*43f,y-it*43f)}).firstOrNull{v->v>=8f&&v+38f<=size.height-8f&&occupied.none{it.overlaps(Rect(Offset(x,v),Size(width,38f)))}} ?: return
        occupied+=Rect(Offset(x,yy),Size(width,38f))
        drawRoundRect(Color(0xE6172234),Offset(x,yy),Size(width,38f),androidx.compose.ui.geometry.CornerRadius(5f))
        drawContext.canvas.nativeCanvas.drawText(text,x+10f,yy+28f,paint)
    }
    if(d.diagnostic.isNotEmpty()){label(d.diagnostic.take(70),Offset(8f,size.height*.21f));return}
    var row=0
    d.entities.forEach { e ->
        if(e.index!=null&&e.index !in visible)return@forEach
        val solid=e.index?.let{solids.getOrNull(it)} ?: e.solid
        fun tx(v:Vec3)=screen(solidLocalToWorld(solid,v))
        if(e.index==null) {
            if(e.quantities.all{it.name=="base_area"}&&solid.type==SolidType.Cylinder) {
                fun p(a:Double)=tx(Vec3(solid.radius*cos(a),0.0,solid.radius*sin(a)))
                (0 until 64).forEach{i->drawLine(WorkspaceVisualStyles.ReferenceCyan,p(i*2*PI/64),p((i+1)*2*PI/64),2f)}
            } else {
                val mesh=SolidMeshFactory.create(solid,16)
                mesh.edges.forEach{(a,b)->drawLine(WorkspaceVisualStyles.ReferenceCyan.copy(.65f),tx(mesh.vertices[a]),tx(mesh.vertices[b]),1.5f)}
            }
        }
        e.quantities.forEach { q ->
            val text=RuhiSolidMeasureLanguage.quantityLabel(solid,q,unit)
            label(text,Offset(8f,size.height*.21f+row++*43f))
        }
        e.segments.forEach { segment ->
            val a=tx(segment.a);val b=tx(segment.b)
            drawLine(color,a,b,3f);drawCircle(color,4f,a);drawCircle(color,4f,b)
            label(RuhiSolidMeasureLanguage.segmentLabel(segment,unit),(a+b)*.5f+Offset(12f,12f))
        }
    }
}
