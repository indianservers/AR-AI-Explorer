package com.indianservers.aiexplorer

import android.opengl.Matrix
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.arengine.contract.*
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.spatial.*
import kotlin.math.*
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.core.ExpressionEngine

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
    private val expressions=object:LinkedHashMap<String,com.indianservers.aiexplorer.core.Expression>(16,.75f,true) { override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,com.indianservers.aiexplorer.core.Expression>?)=size>16 }
    @Synchronized private fun compile(expression:String)=expressions[expression] ?: runCatching { ExpressionEngine().compile(expression).also { expressions[expression]=it } }.getOrNull()
    private val snapshots=java.util.IdentityHashMap<SpatialPrimitive,MathObjectSnapshot>()
    fun objects(scene:SpatialRenderScene,selection:ArSelectionState,state:WorkspaceState):List<MathObjectSnapshot> = scene.primitives.filter { it.visible && it.selectable && it.geometry.vertices.isNotEmpty() }.map { p ->
        val pose=state.arGraphObject(p.id)
        val locked=p.id in selection.lockedObjectIds || pose.locked || state.shapes.firstOrNull { it.id==p.id }?.locked==true
        val selected=p.id in selection.objectIds
        snapshots[p]?.takeIf { it.locked==locked && it.selected==selected }?.let { return@map it }
        val t=p.localTransform
        val vertices=p.geometry.vertices.map { t.orientation.rotate(Vec3(it.x*t.axisScale.x,it.y*t.axisScale.y,it.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters }
        val name=p.metadata["cadType"] ?: p.label
        val type=when { p.id.startsWith("vector-")->MathSemanticType.VECTOR; name.equals("Cube",true)->MathSemanticType.CUBE; name.equals("Cuboid",true)->MathSemanticType.CUBOID; name.equals("Sphere",true)->MathSemanticType.SPHERE; name.equals("Circle",true)->MathSemanticType.CIRCLE; name.equals("Vector",true)->MathSemanticType.VECTOR; name.equals("Curve",true)->MathSemanticType.PARAMETRIC_CURVE; name.equals("Point",true)->MathSemanticType.POINT; name.equals("Line",true)->MathSemanticType.LINE; name.equals("Ray",true)->MathSemanticType.RAY; name.equals("Cylinder",true)->MathSemanticType.CYLINDER; name.equals("Cone",true)->MathSemanticType.CONE; p.kind==SpatialPrimitiveKind.Surface->MathSemanticType.FUNCTION_SURFACE; else->MathSemanticType.GENERIC_GEOMETRY }
        val dims=Vec3(vertices.maxOf { it.x }-vertices.minOf { it.x },vertices.maxOf { it.y }-vertices.minOf { it.y },vertices.maxOf { it.z }-vertices.minOf { it.z })
        val nodes=state.arCadNodes(); val node=nodes[p.id]
        val mutableBody=setOf(MathInteraction.TRANSLATE,MathInteraction.ROTATE,MathInteraction.SCALE,MathInteraction.INSPECT)
        val solid=state.solids.getOrNull(p.id.removePrefix("solid-").toIntOrNull() ?: -1)
        val allowed=if((solid!=null && solid.type !in setOf(com.indianservers.aiexplorer.core.SolidType.Cube,com.indianservers.aiexplorer.core.SolidType.Cuboid,com.indianservers.aiexplorer.core.SolidType.Sphere)) || p.dependencyIds.isNotEmpty() || node?.dependencies?.isNotEmpty()==true || p.kind==SpatialPrimitiveKind.Surface || node?.type in setOf(ArCadType.Curve,ArCadType.ParametricSurface,ArCadType.ImplicitSurface)) mutableBody else MathInteraction.entries.toSet()
        val expression=if(node?.type==ArCadType.FunctionSurface) node.parameters["expressionZ"] ?: "x^2+y^2" else if(p.kind==SpatialPrimitiveKind.Surface && node==null) state.surfaceLayers.firstOrNull { it.id==p.id.removePrefix("surface-") }?.expression ?: state.surfaceExpression else null
        val compiled=expression?.let(::compile)
        val parameters=node?.parameters?.filterKeys { it.startsWith("parameter.") }?.mapKeys { it.key.removePrefix("parameter.") }?.mapValues { ArCadTopology.number(it.value) } ?: emptyMap()
        val curveExpressions=if(node?.type==ArCadType.Curve) listOf("expressionX","expressionY","expressionZ").mapIndexed { i,key -> compile(node.parameters[key] ?: listOf("cos(t)","sin(t)","t/4")[i]) } else emptyList()
        val curveInspector:((Vec3)->Map<String,Double>)?=if(node?.type==ArCadType.Curve && curveExpressions.all { it!=null }) { point:Vec3 ->
            val raw=t.orientation.conjugate().rotate(point-t.offsetMeters)*(1/t.uniformScale);val local=Vec3(raw.x/t.axisScale.x,raw.y/t.axisScale.y,raw.z/t.axisScale.z)
            var nearest=Double.MAX_VALUE;var fraction=0.0
            p.geometry.lines.forEach { (a,b) -> val va=p.geometry.vertices[a];val vb=p.geometry.vertices[b];val start=Vec3(va.x,va.y,va.z);val d=Vec3(vb.x-va.x,vb.y-va.y,vb.z-va.z);val q=((local-start).dot(d)/d.dot(d).coerceAtLeast(1e-12)).coerceIn(0.0,1.0);val gap=(local-start-d*q).magnitude();if(gap<nearest) { nearest=gap;fraction=(a+(b-a)*q)/(p.geometry.vertices.size-1).coerceAtLeast(1) } }
            val min=ArCadTopology.number(node.parameters["tMin"] ?: "0");val max=ArCadTopology.number(node.parameters["tMax"] ?: "2*pi");val parameter=min+(max-min)*fraction;val h=1e-4
            val derivative=curveExpressions.map { expression -> (expression!!.eval(parameters+mapOf("t" to parameter+h))-expression.eval(parameters+mapOf("t" to parameter-h)))/(2*h) }
            linkedMapOf("t" to parameter,"dx/dt" to derivative[0],"dy/dt" to derivative[1],"dz/dt" to derivative[2]).apply { if(abs(derivative[0])>1e-8) put("slope",derivative[1]/derivative[0]) }.filterValues { it.isFinite() }
        } else null
        val evaluator:((Vec3)->Map<String,Double>)?=compiled?.let { f -> { point:Vec3 ->
            val local=t.orientation.conjugate().rotate(point-t.offsetMeters)*(1/t.uniformScale)
            val x=local.x/t.axisScale.x; val y=local.y/t.axisScale.y; val h=1e-4
            fun value(a:Double,b:Double)=f.eval(parameters+mapOf("x" to a,"y" to b))
            mapOf("value" to value(x,y),"gradient x" to (value(x+h,y)-value(x-h,y))/(2*h),"gradient y" to (value(x,y+h)-value(x,y-h))/(2*h)).filterValues { it.isFinite() }
        } }
        MathObjectSnapshot(p.id,type,vertices,p.geometry.triangles.toIntArray(),ArCadTopology.edges(p.geometry),locked=locked,selected=selected,dimensions=dims,radius=dims.x*.5,allowed=allowed,inspect=evaluator ?: curveInspector,orientation=if(p.id.startsWith("solid-")) state.solids.getOrNull(p.id.removePrefix("solid-").toIntOrNull() ?: -1)?.rotation?.let { ArQuaternion.fromEulerDegrees(it.x,it.y,it.z) } ?: t.orientation else t.orientation).also { if(snapshots.size>=64) snapshots.clear(); snapshots[p]=it }
    }
}

/** One bounded worker; landmark inference and intelligence never queue camera frames. */
class HandIntelligenceSession {
    private val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val busy=java.util.concurrent.atomic.AtomicBoolean(false)
    private val generation=java.util.concurrent.atomic.AtomicInteger(0)
    private val controller=com.indianservers.aiexplorer.handintelligence.interaction.HandInteractionController()
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    val recorder=com.indianservers.aiexplorer.handintelligence.debug.GestureReplayRecorder()
    @Volatile var projection:CoordinateMapper?=null
    @Volatile var latest:MathInteractionFrame?=null
    @Volatile var latestTracking:com.indianservers.aiexplorer.arengine.interaction.ArHandFrame?=null
    @Volatile var processingMicros=0L
    @Volatile var droppedFrames=0L
    @Volatile var developerEnabled=false
    @Volatile var profile=IntelligenceProfile.BALANCED
    @Volatile var trainingLabel:HandIntent?=null
    fun export(directory:java.io.File):java.io.File {
        directory.mkdirs(); val file=java.io.File(directory,"hand-replay-${System.currentTimeMillis()}.json")
        file.bufferedWriter().use(recorder::exportJson); java.io.File(directory,file.nameWithoutExtension+".csv").bufferedWriter().use(recorder::exportCsv); return file
    }
    fun replay(onResult:(List<MathInteractionFrame>)->Unit) { recorder.enabled=false; executor.execute { val frames=com.indianservers.aiexplorer.handintelligence.debug.GestureReplayPlayer().replay(recorder.snapshot()); main.post { onResult(frames) } } }
    fun submit(frame:com.indianservers.aiexplorer.arengine.interaction.ArHandFrame,scene:MathSceneSnapshot,onResult:(MathInteractionFrame)->Unit) {
        if(!busy.compareAndSet(false,true)) { droppedFrames++; return }
        val epoch=generation.get()
        try { executor.execute { try { if(epoch==generation.get()) { val start=System.nanoTime(); val actualScene=scene.copy(profile=profile); val result=controller.processFrame(frame,actualScene); processingMicros=(System.nanoTime()-start)/1000; if(epoch!=generation.get()) return@execute; latest=result; if(developerEnabled) latestTracking=frame; recorder.record(frame,actualScene,result,trainingLabel); main.post { if(epoch==generation.get()) onResult(result) } } } finally { busy.set(false) } } } catch(_:java.util.concurrent.RejectedExecutionException) { busy.set(false) }
    }
    fun reset() { generation.incrementAndGet(); controller.reset(); latest=null; latestTracking=null }
    fun close() { reset(); executor.shutdownNow() }
}
