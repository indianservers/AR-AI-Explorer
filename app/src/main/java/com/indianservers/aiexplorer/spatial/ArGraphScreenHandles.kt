package com.indianservers.aiexplorer.spatial

import android.opengl.Matrix
import com.indianservers.aiexplorer.arengine.contract.*
import com.indianservers.aiexplorer.arengine.interaction.*
import kotlin.math.hypot

data class ArGraphScreenHandle(val axis: ArGizmoAxis, val x: Float, val y: Float, val direction: ArVector3? = null, val label: String = axis.name)
fun arGraphScreenHandles(scene: ArScene, frame: ArFrameSnapshot?, id: String?, width: Int, height: Int, vertexIndex: Int? = null, kind: ArSubObjectKind? = null): List<ArGraphScreenHandle> {
    if (frame == null || id == null || width <= 0 || height <= 0) return emptyList()
    val obj = scene.objects.firstOrNull { it.id == id && it.visible } ?: return emptyList()
    val t = obj.localTransform
    val vertex = vertexIndex?.let { index ->
        val geometry=SpatialGeometry(obj.mesh.vertices.map { com.indianservers.aiexplorer.core.Vec3(it.x,it.y,it.z) },obj.mesh.triangleIndices,obj.mesh.lineIndices.chunked(2).map { it[0] to it[1] })
        runCatching { val ids=ArCadTopology.vertices(geometry,kind ?: ArSubObjectKind.Vertex,index); val points=ids.map { obj.mesh.vertices[it] }; points.reduce(ArVector3::plus)*(1.0/points.size) }.getOrNull()
    }
    val radiusObject=obj.metadata["cadType"]=="Sphere" && (kind==null || kind==ArSubObjectKind.Whole)
    val centerVertex=if(radiusObject) ArVector3((obj.mesh.vertices.minOf { it.x }+obj.mesh.vertices.maxOf { it.x })*.5,(obj.mesh.vertices.minOf { it.y }+obj.mesh.vertices.maxOf { it.y })*.5,(obj.mesh.vertices.minOf { it.z }+obj.mesh.vertices.maxOf { it.z })*.5) else vertex
    val origin = if (centerVertex == null) t.offsetMeters else t.offsetMeters + t.orientation.rotate(ArVector3(centerVertex.x*t.axisScale.x,centerVertex.y*t.axisScale.y,centerVertex.z*t.axisScale.z)*t.uniformScale)
    fun project(point: ArVector3): ArVector2? {
        val world = ArCoordinateTransform.mathToWorld(point, scene.placement)
        val clip = FloatArray(4); val view = FloatArray(4)
        Matrix.multiplyMV(view, 0, frame.camera.viewMatrix.values.toFloatArray(), 0, floatArrayOf(world.x.toFloat(),world.y.toFloat(),world.z.toFloat(),1f),0)
        Matrix.multiplyMV(clip,0,frame.camera.projectionMatrix.values.toFloatArray(),0,view,0)
        if (clip[3] <= .01f) return null
        return ArVector2((clip[0]/clip[3]+1)*width*.5f,(1-clip[1]/clip[3])*height*.5f)
    }
    val center = project(origin) ?: return emptyList()
    if(radiusObject) {
        val radius=(obj.metadata["radius"]?.toDoubleOrNull() ?: 1.0)*t.uniformScale*t.axisScale.x
        val p=project(origin+ArVector3(radius,0.0,0.0))
        if(p!=null) return listOf(ArGraphScreenHandle(ArGizmoAxis.Uniform,center.x,center.y),ArGraphScreenHandle(ArGizmoAxis.Uniform,p.x,p.y,ArVector3(1.0,0.0,0.0),"R"))
    }
    val worldAxes = listOf(ArGizmoAxis.X to ArVector3(1.0,0.0,0.0),ArGizmoAxis.Y to ArVector3(0.0,1.0,0.0),ArGizmoAxis.Z to ArVector3(0.0,0.0,1.0))
    val g=SpatialGeometry(obj.mesh.vertices.map { com.indianservers.aiexplorer.core.Vec3(it.x,it.y,it.z) },obj.mesh.triangleIndices,obj.mesh.lineIndices.chunked(2).map { it[0] to it[1] })
    val special=runCatching {
        when(kind) {
            ArSubObjectKind.Face -> { val f=g.triangles.drop(requireNotNull(vertexIndex)*3).take(3); val u=(g.vertices[f[1]]-g.vertices[f[0]]).normalized(); val n=com.indianservers.aiexplorer.core.AnalyticGeometry3D.cross(g.vertices[f[1]]-g.vertices[f[0]],g.vertices[f[2]]-g.vertices[f[0]]).normalized(); val v=com.indianservers.aiexplorer.core.AnalyticGeometry3D.cross(n,u).normalized(); listOf(Triple(ArGizmoAxis.X,n,"N"),Triple(ArGizmoAxis.Y,u,"U"),Triple(ArGizmoAxis.Z,v,"V")) }
            ArSubObjectKind.Edge -> { val e=ArCadTopology.edges(g)[requireNotNull(vertexIndex)]; val d=(g.vertices[e.second]-g.vertices[e.first]).normalized(); listOf(Triple(ArGizmoAxis.X,d,"D")) }
            else -> emptyList()
        }
    }.getOrDefault(emptyList())
    val axes=if(special.isEmpty()) worldAxes.map { Triple(it.first,it.second,it.first.name) } else special.map { (axis,v,label) -> Triple(axis,t.orientation.rotate(ArVector3(v.x,v.y,v.z)),label) }
    return listOf(ArGraphScreenHandle(ArGizmoAxis.Uniform,center.x,center.y)) + axes.mapNotNull { (axis,v,label) ->
        val p = project(origin + v*.25) ?: return@mapNotNull null
        val dx = p.x-center.x; val dy = p.y-center.y
        val length = hypot(dx,dy)
        if (length < 2f) null else ArGraphScreenHandle(axis,center.x+dx/length*(if(kind==null || kind==ArSubObjectKind.Whole) 72f else 42f),center.y+dy/length*(if(kind==null || kind==ArSubObjectKind.Whole) 72f else 42f),if(special.isEmpty()) null else v,label)
    }
}
