package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.interaction.*
import kotlin.math.*

/** Real mathematical 3D fallback; camera gestures and entity gestures have separate ownership. */
@Composable
fun ArCadPreviewCanvas(modifier:Modifier, scene:SpatialRenderScene, selection:ArSelectionState, kind:ArSubObjectKind,
    onSelect:(ArPickHit?)->Unit,onStart:()->Unit,onEdit:(ArPickHit,SpatialGeometry,Vec3,Double,Double)->Unit,onEnd:(Boolean)->Unit,onInspect:()->Unit,showLabels:Boolean=false,transparentBackground:Boolean=false,onProjection:(HandPreviewProjection)->Unit={}) {
    var yaw by remember { mutableFloatStateOf(-25f) }; var pitch by remember { mutableFloatStateOf(20f) }; var zoom by remember { mutableFloatStateOf(60f) }
    var viewport by remember { mutableStateOf(Offset(1f,1f)) }
    var viewCenter by remember { mutableStateOf(Vec3(0.0,0.0,0.0)) }
    val select by rememberUpdatedState(onSelect); val start by rememberUpdatedState(onStart); val edit by rememberUpdatedState(onEdit); val end by rememberUpdatedState(onEnd); val inspect by rememberUpdatedState(onInspect)
    val currentScene by rememberUpdatedState(scene); val currentSelection by rememberUpdatedState(selection); val currentKind by rememberUpdatedState(kind)
    fun world(g:SpatialPrimitive)=g.geometry.copy(vertices=g.geometry.vertices.map { p -> val t=g.localTransform; val v=t.orientation.rotate(ArVector3(p.x*t.axisScale.x,p.y*t.axisScale.y,p.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters; Vec3(v.x,v.y,v.z) })
    fun project(worldPoint:Vec3):Vec3 { val p=worldPoint-viewCenter; val y=yaw*PI/180; val x=pitch*PI/180; val xx=p.x*cos(y)+p.z*sin(y); val z=-p.x*sin(y)+p.z*cos(y); val yy=p.y*cos(x)-z*sin(x); return Vec3(viewport.x*.5+xx*zoom,viewport.y*.52-yy*zoom,p.y*sin(x)+z*cos(x)) }
    val modelIds=scene.primitives.filter { it.visible && (it.selectable || it.metadata["cadType"]!=null) }.map { it.id }
    LaunchedEffect(modelIds,viewport) {
        if(viewport.x>1 && viewport.y>1) {
            val points=scene.primitives.filter { it.id in modelIds }.flatMap { world(it).vertices }
            if(points.isNotEmpty()) {
                val center=Vec3((points.minOf { it.x }+points.maxOf { it.x })/2,(points.minOf { it.y }+points.maxOf { it.y })/2,(points.minOf { it.z }+points.maxOf { it.z })/2)
                val y=yaw*PI/180; val x=pitch*PI/180
                val projected=points.map { point -> val p=point-center; val xx=p.x*cos(y)+p.z*sin(y); val z=-p.x*sin(y)+p.z*cos(y); xx to p.y*cos(x)-z*sin(x) }
                val width=projected.maxOf { it.first }-projected.minOf { it.first }; val height=projected.maxOf { it.second }-projected.minOf { it.second }
                zoom=minOf(60.0,viewport.x*.78/maxOf(.1,width),viewport.y*.55/maxOf(.1,height)).toFloat().coerceAtLeast(8f)
                viewCenter=center
            }
        }
    }
    fun distance(p:Offset,a:Vec3,b:Vec3):Double { val dx=b.x-a.x; val dy=b.y-a.y; val length=dx*dx+dy*dy; val t=if(length>1e-9) ((p.x-a.x)*dx+(p.y-a.y)*dy)/length else 0.0; val q=t.coerceIn(0.0,1.0); return hypot(p.x-a.x-dx*q,p.y-a.y-dy*q) }
    fun pick(p:Offset):ArPickHit? {
        val hits=mutableListOf<Pair<Double,ArPickHit>>()
        currentScene.primitives.filter { it.visible && it.selectable }.forEach { obj ->
            val g=world(obj); val points=g.vertices.map(::project)
            fun add(index:Int,type:ArSubObjectKind,indices:List<Int>,exact:Vec3?=null) { val point=exact ?: indices.map(g.vertices::get).reduce(Vec3::plus)*(1.0/indices.size); hits+=indices.map { points[it].z }.average() to ArPickHit(obj.id,type,if(type==ArSubObjectKind.Whole) null else index,0.0,ArVector3(point.x,point.y,point.z)) }
            if(currentKind in setOf(ArSubObjectKind.Vertex,ArSubObjectKind.Whole)) points.forEachIndexed { i,v -> if(hypot(p.x-v.x,p.y-v.y)<16) add(i,if(currentKind==ArSubObjectKind.Vertex) currentKind else ArSubObjectKind.Whole,listOf(i)) }
            if(currentKind in setOf(ArSubObjectKind.Edge,ArSubObjectKind.Whole)) ArCadTopology.edges(g).forEachIndexed { i,(a,b) -> if(distance(p,points[a],points[b])<12) add(i,if(currentKind==ArSubObjectKind.Edge) currentKind else ArSubObjectKind.Whole,listOf(a,b)) }
            if(currentKind in setOf(ArSubObjectKind.Face,ArSubObjectKind.Whole)) g.triangles.chunked(3).forEachIndexed { i,f ->
                val a=points[f[0]]; val b=points[f[1]]; val c=points[f[2]]; val den=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y)
                if(abs(den)>1e-9) { val u=((b.y-c.y)*(p.x-c.x)+(c.x-b.x)*(p.y-c.y))/den; val v=((c.y-a.y)*(p.x-c.x)+(a.x-c.x)*(p.y-c.y))/den; if(u>=0 && v>=0 && u+v<=1) add(i,currentKind,f,g.vertices[f[0]]*u+g.vertices[f[1]]*v+g.vertices[f[2]]*(1-u-v)) }
            }
        }
        return hits.maxByOrNull { it.first }?.second
    }
    Canvas(modifier.semantics { contentDescription="CAD 3D preview" }.pointerInput(Unit) {
        awaitEachGesture {
            val down=awaitFirstDown(false)
            val sphere=currentScene.primitives.firstOrNull { it.id==currentSelection.primaryObjectId && it.metadata["cadType"]=="Sphere" && currentKind==ArSubObjectKind.Whole }
            val radiusPoint=sphere?.let { obj -> val vertices=world(obj).vertices; Vec3(vertices.maxOf { it.x },(vertices.minOf { it.y }+vertices.maxOf { it.y })*.5,(vertices.minOf { it.z }+vertices.maxOf { it.z })*.5) }?.let(::project)
            val radiusHit=radiusPoint!=null && hypot(radiusPoint.x-down.position.x,radiusPoint.y-down.position.y)<22
            val hit=if(radiusHit && sphere!=null) ArPickHit(sphere.id,ArSubObjectKind.Whole,-1,0.0,ArVector3.Zero) else pick(down.position); val editing=hit!=null && currentSelection.primaryObjectId==hit.objectId
            val geometry=hit?.let { h -> currentScene.primitives.firstOrNull { it.id==h.objectId }?.geometry }
            select(hit)
            var pan=Offset.Zero; var rotation=0.0; var scale=1.0
            if(editing) start()
            try {
                do {
                    val event=awaitPointerEvent(); val delta=event.calculatePan(); pan+=delta; rotation+=event.calculateRotation(); scale*=event.calculateZoom()
                    if(editing && hit!=null && geometry!=null) {
                        val y=yaw*PI/180; val x=pitch*PI/180; val a=pan.x/zoom; val b=-pan.y/zoom
                        val units=Vec3(a*cos(y)-b*sin(x)*sin(y),b*cos(x),a*sin(y)+b*sin(x)*cos(y))
                        edit(hit,geometry,units,rotation,scale)
                    } else if(event.changes.count { it.pressed }>1) zoom=(zoom*event.calculateZoom()).coerceIn(8f,180f)
                    else if(pan.getDistance()>12) { yaw+=delta.x*.25f; pitch=(pitch+delta.y*.25f).coerceIn(-85f,85f) }
                    event.changes.forEach { it.consume() }
                } while(event.changes.any { it.pressed })
                if(editing) end(false)
                if(hit!=null && pan.getDistance()<12 && android.os.SystemClock.uptimeMillis()-down.uptimeMillis>500) inspect()
            } finally { if(editing) end(true) }
        }
    }) {
        viewport=Offset(size.width,size.height)
        onProjection(HandPreviewProjection(size.width.toDouble(),size.height.toDouble(),yaw.toDouble(),pitch.toDouble(),zoom.toDouble(),ArVector3(viewCenter.x,viewCenter.y,viewCenter.z)))
        if (!transparentBackground) drawRect(Color(0xFF08131B))
        data class Face(val points:List<Vec3>,val color:Color)
        val faces=mutableListOf<Face>()
        scene.primitives.filter { it.visible }.forEach { obj ->
            val g=world(obj); val points=g.vertices.map(::project); val rgba=obj.material.colorRgba; val color=Color(rgba[0],rgba[1],rgba[2],rgba[3])
            if(obj.metadata["filled"]!="false") g.triangles.chunked(3).forEach { f -> faces+=Face(f.map(points::get),color) }
        }
        faces.sortedBy { it.points.map(Vec3::z).average() }.forEach { f -> val path=Path(); f.points.forEachIndexed { i,p -> if(i==0) path.moveTo(p.x.toFloat(),p.y.toFloat()) else path.lineTo(p.x.toFloat(),p.y.toFloat()) }; path.close(); drawPath(path,f.color) }
        scene.primitives.filter { it.visible }.forEach { obj ->
            val g=world(obj); val points=g.vertices.map(::project); val rgba=obj.material.colorRgba; val color=Color(rgba[0],rgba[1],rgba[2],rgba[3])
            g.lines.forEach { (a,b) -> drawLine(color,Offset(points[a].x.toFloat(),points[a].y.toFloat()),Offset(points[b].x.toFloat(),points[b].y.toFloat()),if(obj.metadata["cadType"]=="Curve") (g.pointRadius*zoom).toFloat().coerceIn(1f,12f) else 2f) }
            if(g.triangles.isEmpty() && g.lines.isEmpty()) points.forEach { p -> drawCircle(color,7f,Offset(p.x.toFloat(),p.y.toFloat())) }
            if(obj.id==selection.primaryObjectId && obj.metadata["cadType"]=="Sphere" && kind==ArSubObjectKind.Whole) {
                val vertices=g.vertices; val handle=project(Vec3(vertices.maxOf { it.x },(vertices.minOf { it.y }+vertices.maxOf { it.y })*.5,(vertices.minOf { it.z }+vertices.maxOf { it.z })*.5))
                drawCircle(Color(0xFFFFBD46),10f,Offset(handle.x.toFloat(),handle.y.toFloat()))
            }
        }
        if(showLabels) drawArCadLabels(scene.primitives.filter { it.visible && it.id !in setOf("cad-selection","cad-hover") && (it.metadata["cadType"]!=null || it.kind==SpatialPrimitiveKind.Surface) && !it.id.startsWith("analysis-") && !it.id.startsWith("scalar-") }.take(12).map { obj ->
            val p=project(world(obj).vertices.let { it.reduce(Vec3::plus)*(1.0/it.size) })
            ArCadLabel(obj.label,p.x.toFloat(),p.y.toFloat()+14,14f*density)
        })
    }
}
