package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.indianservers.aiexplorer.arengine.contract.*
import android.opengl.Matrix

data class ArCadLabel(val text:String,val x:Float,val y:Float,val size:Float=14f)
data class ArCadLabelBounds(val label:ArCadLabel,val left:Float,val top:Float,val right:Float,val bottom:Float)
fun arCadLabelLayout(labels:List<ArCadLabel>,width:Float,height:Float):List<ArCadLabelBounds> {
    val placed=mutableListOf<ArCadLabelBounds>()
    for(label in labels.take(12)) {
        val w=(label.text.take(32).length*label.size*.62f+12).coerceAtMost(width-12); val h=label.size+12
        if(label.x !in 0f..width || label.y !in 0f..height) continue
        val left=(label.x-w/2).coerceIn(6f,(width-w-6).coerceAtLeast(6f))
        for(offset in listOf(0f,h,-h,2*h,-2*h)) {
            val top=label.y+offset
            if(top<64 || top+h>height-20) continue
            val box=ArCadLabelBounds(label,left,top,left+w,top+h)
            if(placed.none { box.left<it.right+4 && box.right>it.left-4 && box.top<it.bottom+4 && box.bottom>it.top-4 }) { placed+=box; break }
        }
    }
    return placed
}
fun DrawScope.drawArCadLabels(labels:List<ArCadLabel>) {
    val canvas=drawContext.canvas.nativeCanvas
    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    arCadLabelLayout(labels,size.width,size.height).forEach { box ->
        paint.color=android.graphics.Color.argb(220,8,19,27)
        canvas.drawRoundRect(box.left,box.top,box.right,box.bottom,6f,6f,paint)
        paint.color=android.graphics.Color.WHITE; paint.textSize=box.label.size
        canvas.drawText(box.label.text.take(32),box.left+6,box.bottom-7,paint)
    }
}
@Composable
fun ArCadSmartLabels(modifier:Modifier,scene:ArScene,frame:ArFrameSnapshot) {
    val positions=remember(scene) { scene.objects.filter { it.visible && it.id !in setOf("cad-selection","cad-hover") && (it.metadata["cadType"]!=null || it.kind==ArObjectKind.Surface) && !it.id.startsWith("analysis-") && !it.id.startsWith("scalar-") && it.mesh.vertices.isNotEmpty() }.take(12).map { obj ->
        val c=obj.mesh.vertices.reduce(ArVector3::plus)*(1.0/obj.mesh.vertices.size)
        val t=obj.localTransform
        obj.label to ArCoordinateTransform.mathToWorld(t.offsetMeters+t.orientation.rotate(ArVector3(c.x*t.axisScale.x,c.y*t.axisScale.y,c.z*t.axisScale.z)*t.uniformScale),scene.placement)
    } }
    Canvas(modifier) {
        val labels=positions.mapNotNull { (text,p) ->
            val view=FloatArray(4); val clip=FloatArray(4)
            Matrix.multiplyMV(view,0,frame.camera.viewMatrix.values.toFloatArray(),0,floatArrayOf(p.x.toFloat(),p.y.toFloat(),p.z.toFloat(),1f),0)
            Matrix.multiplyMV(clip,0,frame.camera.projectionMatrix.values.toFloatArray(),0,view,0)
            if(clip[3]<=.01) null else ArCadLabel(text,(clip[0]/clip[3]+1)*size.width/2,(1-clip[1]/clip[3])*size.height/2+12,(16f* density/(clip[3]*.15f+1)).coerceIn(12f*density,16f*density))
        }
        drawArCadLabels(labels)
    }
}
