from pathlib import Path
root=Path(r'C:\Indian Servers\AIExplorer')
p=root/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt'
p.write_text('''package com.indianservers.aiexplorer

import android.opengl.Matrix
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.arengine.contract.*
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.spatial.*
import kotlin.math.*

/** Immutable projection also used by the camera-only mathematical renderer. */
class HandPreviewProjection(val width:Double,val height:Double,val yaw:Double,val pitch:Double,val zoom:Double,val center:Vec3):CoordinateMapper {
    private val y=yaw*PI/180; private val x=pitch*PI/180
    private fun inverse(a:Double,b:Double,z:Double)=Vec3(a*cos(y)-(z*cos(x)-b*sin(x))*sin(y),b*cos(x)+z*sin(x),a*sin(y)+(z*cos(x)-b*sin(x))*cos(y))
    override fun project(point:Vec3):Vec3 { val p=point-center; val a=p.x*cos(y)+p.z*sin(y); val z=-p.x*sin(y)+p.z*cos(y); return Vec3(.5+a*zoom/width,.52-(p.y*cos(x)-z*sin(x))*zoom/height,0.0) }
    override fun atDepth(normalized:Vec3,reference:Vec3):Vec3 { val p=reference-center; val z=p.y*sin(x)+(-p.x*sin(y)+p.z*cos(y))*cos(x); return center+inverse((normalized.x-.5)*width/zoom,(.52-normalized.y)*height/zoom,z) }
    override fun screenToRay(normalized:Vec3):Ray3 { val a=(normalized.x-.5)*width/zoom; val b=(.52-normalized.y)*height/zoom; return Ray3(center+inverse(a,b,1000.0),inverse(0.0,0.0,-1.0).unit()) }
}

class HandArProjection(frame:ArFrameSnapshot,private val placement:ArScenePlacement):CoordinateMapper {
    private val vp=FloatArray(16).also { Matrix.multiplyMM(it,0,frame.camera.projectionMatrix.values.toFloatArray(),0,frame.camera.viewMatrix.values.toFloatArray(),0) }
    private val inverse=FloatArray(16).also { Matrix.invertM(it,0,vp,0) }
    private val normal=frame.camera.pose.orientation.rotate(Vec3(0.0,0.0,-1.0))
    override fun project(point:Vec3):Vec3? { val p=ArCoordinateTransform.mathToWorld(point,placement); val o=FloatArray(4); Matrix.multiplyMV(o,0,vp,0,floatArrayOf(p.x.toFloat(),p.y.toFloat(),p.z.toFloat(),1f),0); if(o[3]<=0) return null; return Vec3((o[0]/o[3]+1)*.5,(1-o[1]/o[3])*.5,0.0) }
    private fun unproject(p:Vec3,z:Float):Vec3 { val o=FloatArray(4); Matrix.multiplyMV(o,0,inverse,0,floatArrayOf((p.x*2-1).toFloat(),(1-p.y*2).toFloat(),z,1f),0); return Vec3(o[0]/o[3].toDouble(),o[1]/o[3].toDouble(),o[2]/o[3].toDouble()) }
    override fun screenToRay(normalized:Vec3):Ray3 { val a=ArCoordinateTransform.worldToMath(unproject(normalized,-1f),placement); val b=ArCoordinateTransform.worldToMath(unproject(normalized,1f),placement); return Ray3(a,(b-a).unit()) }
    override fun atDepth(normalized:Vec3,reference:Vec3):Vec3? { val a=unproject(normalized,-1f); val d=(unproject(normalized,1f)-a).unit(); val denom=d.dot(normal); if(abs(denom)<1e-8) return null; val t=(ArCoordinateTransform.mathToWorld(reference,placement)-a).dot(normal)/denom; return ArCoordinateTransform.worldToMath(a+d*t,placement) }
}

object HandMathSceneAdapter {
    fun objects(scene:SpatialRenderScene,selection:ArSelectionState):List<MathObjectSnapshot> = scene.primitives.filter { it.visible && it.selectable && it.geometry.vertices.isNotEmpty() }.map { p ->
        val t=p.localTransform
        val vertices=p.geometry.vertices.map { t.orientation.rotate(Vec3(it.x*t.axisScale.x,it.y*t.axisScale.y,it.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters }
        val name=p.metadata["cadType"] ?: p.label
        val type=when { p.id.startsWith("vector-")->MathSemanticType.VECTOR; name.equals("Cube",true)->MathSemanticType.CUBE; name.equals("Cuboid",true)->MathSemanticType.CUBOID; name.equals("Sphere",true)->MathSemanticType.SPHERE; name.equals("Circle",true)->MathSemanticType.CIRCLE; name.equals("Vector",true)->MathSemanticType.VECTOR; name.equals("Cylinder",true)->MathSemanticType.CYLINDER; name.equals("Cone",true)->MathSemanticType.CONE; p.kind==SpatialPrimitiveKind.Surface->MathSemanticType.FUNCTION_SURFACE; else->MathSemanticType.GENERIC_GEOMETRY }
        val dims=Vec3(vertices.maxOf { it.x }-vertices.minOf { it.x },vertices.maxOf { it.y }-vertices.minOf { it.y },vertices.maxOf { it.z }-vertices.minOf { it.z })
        MathObjectSnapshot(p.id,type,vertices,p.geometry.triangles.toIntArray(),ArCadTopology.edges(p.geometry),locked=p.id in selection.lockedObjectIds,selected=p.id in selection.objectIds,dimensions=dims,radius=dims.x*.5)
    }
}

/** One bounded worker; landmark inference and intelligence never queue camera frames. */
class HandIntelligenceSession {
    private val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val busy=java.util.concurrent.atomic.AtomicBoolean(false)
    private val generation=java.util.concurrent.atomic.AtomicInteger(0)
    private val controller=com.indianservers.aiexplorer.handintelligence.interaction.HandInteractionController()
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    @Volatile var projection:CoordinateMapper?=null
    fun submit(frame:com.indianservers.aiexplorer.arengine.interaction.ArHandFrame,scene:MathSceneSnapshot,onResult:(MathInteractionFrame)->Unit) {
        if(!busy.compareAndSet(false,true)) return
        val epoch=generation.get()
        try { executor.execute { try { if(epoch==generation.get()) { val result=controller.processFrame(frame,scene); main.post { if(epoch==generation.get()) onResult(result) } } } finally { busy.set(false) } } } catch(_:java.util.concurrent.RejectedExecutionException) { busy.set(false) }
    }
    fun reset() { generation.incrementAndGet(); controller.reset() }
    fun close() { reset(); executor.shutdownNow() }
}
''',encoding='utf-8')
p=root/'app/src/main/java/com/indianservers/aiexplorer/ArCadPreviewCanvas.kt'
s=p.read_text().replace('transparentBackground:Boolean=false) {','transparentBackground:Boolean=false,onProjection:(HandPreviewProjection)->Unit={}) {')
s=s.replace('viewport=Offset(size.width,size.height)','viewport=Offset(size.width,size.height)\n        onProjection(HandPreviewProjection(size.width.toDouble(),size.height.toDouble(),yaw.toDouble(),pitch.toDouble(),zoom.toDouble(),ArVector3(viewCenter.x,viewCenter.y,viewCenter.z)))')
p.write_text(s,encoding='utf-8')
