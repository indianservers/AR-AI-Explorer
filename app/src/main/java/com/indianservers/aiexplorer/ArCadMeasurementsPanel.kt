package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*

enum class ArCadMeasureKind { Distance, Angle, PointPlaneDistance, PointLineDistance, LinePlaneAngle, PlanePlaneAngle, NormalsAngle, VectorProjection }
data class ArCadMeasurement(val kind:ArCadMeasureKind,val ids:List<String>) {
    fun encode()=kind.name+"|"+ids.joinToString(",")
    val angular:Boolean get()=kind in setOf(ArCadMeasureKind.Angle,ArCadMeasureKind.LinePlaneAngle,ArCadMeasureKind.PlanePlaneAngle,ArCadMeasureKind.NormalsAngle)
    fun value(scene:SpatialRenderScene):Double {
        val geometries=ids.map { id -> val obj=scene.primitives.firstOrNull { it.id==id && it.visible } ?: error("Measurement object unavailable"); val t=obj.localTransform; obj.geometry.copy(vertices=obj.geometry.vertices.map { p -> (t.orientation.rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(p.x*t.axisScale.x,p.y*t.axisScale.y,p.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters).let { Vec3(it.x,it.y,it.z) } }) }
        fun center(i:Int)=geometries[i].vertices.let { it.reduce(Vec3::plus)*(1.0/it.size) }
        fun direction(i:Int):Vec3 { val p=geometries[i].vertices; require(p.size>=2) { "A line or vector is required" }; val d=p[1]-p[0]; require(d.magnitude()>1e-10); return d }
        fun normal(i:Int):Vec3 { val p=geometries[i].vertices; require(p.size>=3) { "A plane is required" }; val n=AnalyticGeometry3D.cross(p[1]-p[0],p[2]-p[0]).normalized(); require(n.magnitude()>1e-10 && p.all { kotlin.math.abs((it-p[0]).dot(n))<1e-6 }) { "Object must be planar" }; return n }
        require(geometries.size==if(kind==ArCadMeasureKind.Angle) 3 else 2) { "Provide ${if(kind==ArCadMeasureKind.Angle) 3 else 2} objects in the listed order" }
        return when(kind) {
            ArCadMeasureKind.Distance -> (center(1)-center(0)).magnitude()
            ArCadMeasureKind.Angle -> ArCadVectorMath.angle(center(0)-center(1),center(2)-center(1))
            ArCadMeasureKind.PointPlaneDistance -> { require(geometries[0].vertices.size==1) { "First object must be a point" }; kotlin.math.abs((center(0)-geometries[1].vertices[0]).dot(normal(1))) }
            ArCadMeasureKind.PointLineDistance -> { require(geometries[0].vertices.size==1); AnalyticGeometry3D.cross(center(0)-geometries[1].vertices[0],direction(1)).magnitude()/direction(1).magnitude() }
            ArCadMeasureKind.LinePlaneAngle -> kotlin.math.asin(kotlin.math.abs(direction(0).normalized().dot(normal(1))).coerceIn(0.0,1.0))
            ArCadMeasureKind.PlanePlaneAngle -> kotlin.math.acos(kotlin.math.abs(normal(0).dot(normal(1))).coerceIn(0.0,1.0))
            ArCadMeasureKind.NormalsAngle -> ArCadVectorMath.angle(normal(0),normal(1))
            ArCadMeasureKind.VectorProjection -> direction(0).dot(direction(1).normalized())
        }
    }
    companion object { fun decode(s:String):ArCadMeasurement { val p=s.split('|'); return ArCadMeasurement(ArCadMeasureKind.valueOf(p[0]),p[1].split(',')) } }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadMeasurementsPanel(vm:ExplorerViewModel,scene:SpatialRenderScene) {
    var ids by remember { mutableStateOf("") }; var kind by remember { mutableStateOf(ArCadMeasureKind.Distance) }; var error by remember { mutableStateOf("") }
    Text("Live measurements by object ID; angle uses 3 points; other measures use 2 objects in the named order",color=Color.White)
    Text(scene.primitives.filter { it.selectable }.joinToString("\n") { "${it.label}: ${it.id}" },color=Color.White)
    FlowRow { ArCadMeasureKind.entries.forEach { type -> GlowButton(type.name) { kind=type } } }
    OutlinedTextField(ids,{ids=it},label={Text("Object IDs, comma separated")})
    GlowButton("Add live measurement") { runCatching {
        val measure=ArCadMeasurement(kind,ids.split(',').map(String::trim).filter(String::isNotBlank)); measure.value(scene)
        vm.executeArCadValues("Add live measurement") { it+("arCad.measure.${java.util.UUID.randomUUID()}" to measure.encode()) }
    }.onFailure { error=it.message ?: "Invalid measurement" }.onSuccess { error="" } }
    vm.state.labSessionValues.filterKeys { it.startsWith("arCad.measure.") }.forEach { (key,value) ->
        val measure=ArCadMeasurement.decode(value)
        val result=runCatching { measure.value(scene).toString()+if(measure.angular) " radians" else " math units" }.getOrElse { it.message ?: "Unavailable" }
        Text("${measure.kind}: $result",color=Color.White)
        GlowButton("Remove measurement") { vm.executeArCadValues("Remove live measurement") { it-key } }
    }
    if(error.isNotEmpty()) Text(error,color=Color(0xFFFFBF69))
}
