package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.arengine.contract.*
import kotlin.math.*
import kotlinx.coroutines.launch

fun arCadGroupTransforms(scene:SpatialRenderScene,nodes:Map<String,ArCadNode>,values:Map<String,String>,members:Set<String>,delta:Vec3,rotation:Vec3,scale:Double):Map<String,String> {
    require(scale>0 && scale.isFinite())
    val roots=members.filter { id -> nodes[id]?.dependencies.orEmpty().none { it in members } }
    val objects=scene.primitives.filter { it.id in members }
    require(objects.isNotEmpty())
    val centers=objects.map { obj -> val pose=ArGraphObjectState.decode(values["arGraph.object.${obj.id}"]); val vertices=arCadWorldGeometry(obj.geometry,pose).vertices; vertices.reduce(Vec3::plus)*(1.0/vertices.size) }
    val center=centers.reduce(Vec3::plus)*(1.0/centers.size)
    val turn=ArQuaternion.fromEulerDegrees(rotation.x,rotation.y,rotation.z)
    var result=values
    roots.forEach { id ->
        val old=ArGraphObjectState.decode(values["arGraph.object.$id"]); require(!old.locked) { "Unlock group members before transforming" }
        val p=old.position-center; val moved=turn.rotate(ArVector3(p.x,p.y,p.z))*scale
        val q=(turn*old.transform().orientation).normalized()
        val angles=Vec3(Math.toDegrees(atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y))),Math.toDegrees(asin((2*(q.w*q.y-q.z*q.x)).coerceIn(-1.0,1.0))),Math.toDegrees(atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z))))
        result=result+("arGraph.object.$id" to old.copy(position=center+Vec3(moved.x,moved.y,moved.z)+delta,rotation=angles,scale=(old.scale*scale).coerceIn(.1,10.0)).encode())
    }
    val compiler=ArCadSceneCompiler()
    ArCadDependencies.order(nodes).filter { it in members && it !in roots }.forEach { id ->
        val node=nodes.getValue(id); val old=ArGraphObjectState.decode(values["arGraph.object.$id"])
        require(!old.locked) { "Unlock group members before transforming" }
        require(node.dependencies.all { it in members }) { "Include all dependencies of $id in this transform group" }
        val oldObject=objects.first { it.id==id }; val oldVertices=arCadWorldGeometry(oldObject.geometry,old).vertices; val oldCenter=oldVertices.reduce(Vec3::plus)*(1.0/oldVertices.size)
        val relative=oldCenter-center; val wanted=turn.rotate(ArVector3(relative.x,relative.y,relative.z))*scale
        val target=center+Vec3(wanted.x,wanted.y,wanted.z)+delta
        val rebuilt=compiler.build(SpatialRenderScene("group",emptyList()),com.indianservers.aiexplorer.workspace.WorkspaceState(labSessionValues=result)).primitives.first { it.id==id }.geometry
        val localCenter=rebuilt.vertices.reduce(Vec3::plus)*(1.0/rebuilt.vertices.size)
        val dependentShape=node.type in setOf(ArCadType.Point,ArCadType.Midpoint,ArCadType.Intersection,ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector,ArCadType.Triangle,ArCadType.Polygon)
        val q=if(dependentShape || node.type==ArCadType.Plane) (turn*old.transform().orientation*turn.conjugate()).normalized() else (turn*old.transform().orientation).normalized()
        val factor=if(dependentShape) old.scale else (old.scale*scale).coerceIn(.1,10.0)
        val local=q.rotate(ArVector3(localCenter.x*old.axisScale.x,localCenter.y*old.axisScale.y,localCenter.z*old.axisScale.z)*factor)
        val angles=Vec3(Math.toDegrees(atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y))),Math.toDegrees(asin((2*(q.w*q.y-q.z*q.x)).coerceIn(-1.0,1.0))),Math.toDegrees(atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z))))
        result=result+("arGraph.object.$id" to old.copy(position=target-Vec3(local.x,local.y,local.z),rotation=angles,scale=factor).encode())
    }
    return result
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadGroupsPanel(vm:ExplorerViewModel,scene:SpatialRenderScene) {
    val scope=rememberCoroutineScope()
    var members by remember { mutableStateOf("") }; var coordinates by remember { mutableStateOf("0,0,0") }; var rotation by remember { mutableStateOf("0,0,0") }; var scale by remember { mutableStateOf("1") }; var error by remember { mutableStateOf("") }
    fun triple(s:String):Vec3 { val p=s.split(',').map(ArCadTopology::number); require(p.size==3); return Vec3(p[0],p[1],p[2]) }
    fun apply(action:()->Unit) { runCatching(action).onSuccess { error="" }.onFailure { error=it.message ?: "Invalid group" } }
    Text("Group members by stable object ID",color=Color.White)
    Text(scene.primitives.filter { it.selectable }.joinToString("\n") { "${it.label}: ${it.id}" },color=Color(0xFFB7CAD7),fontSize=10.sp)
    OutlinedTextField(members,{members=it},label={Text("Member IDs, comma separated")})
    GlowButton("Create group") { apply { val ids=members.split(',').map(String::trim).filter(String::isNotBlank).toSet(); require(ids.size>=2 && ids.all { id -> scene.primitives.any { it.id==id } }); vm.executeArCadValues("Group CAD objects") { it+("arCad.group.${java.util.UUID.randomUUID()}" to ids.joinToString(",")) } } }
    OutlinedTextField(coordinates,{coordinates=it},label={Text("Move X,Y,Z")})
    OutlinedTextField(rotation,{rotation=it},label={Text("Rotate X,Y,Z degrees")})
    OutlinedTextField(scale,{scale=it},label={Text("Uniform scale")})
    vm.state.labSessionValues.filterKeys { it.startsWith("arCad.group.") }.forEach { (key,value) ->
        val ids=value.split(',').toSet(); Text("Group · ${ids.size} members",color=Color.White)
        FlowRow {
            GlowButton("Transform group") { apply {
                val before=vm.state.labSessionValues; val nodes=vm.state.arCadNodes(); val move=triple(coordinates); val rotate=triple(rotation); val factor=ArCadTopology.number(scale)
                scope.launch { runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { arCadGroupTransforms(scene,nodes,before,ids,move,rotate,factor) } }.onSuccess { changed -> if(vm.state.labSessionValues==before) vm.executeArCadValues("Transform CAD group") { changed } else error="Scene changed; retry group transform" }.onFailure { error=it.message ?: "Invalid group transform" } }
            } }
            GlowButton("Hide group") { vm.executeArCadValues("Hide CAD group") { values -> values+ids.associate { id -> "arGraph.object.$id" to ArGraphObjectState.decode(values["arGraph.object.$id"]).copy(visible=false).encode() } } }
            GlowButton("Show group") { vm.executeArCadValues("Show CAD group") { values -> values+ids.associate { id -> "arGraph.object.$id" to ArGraphObjectState.decode(values["arGraph.object.$id"]).copy(visible=true).encode() } } }
            GlowButton("Lock group") { vm.executeArCadValues("Lock CAD group") { values -> values+ids.associate { id -> "arGraph.object.$id" to ArGraphObjectState.decode(values["arGraph.object.$id"]).copy(locked=true).encode() } } }
            GlowButton("Unlock group") { vm.executeArCadValues("Unlock CAD group") { values -> values+ids.associate { id -> "arGraph.object.$id" to ArGraphObjectState.decode(values["arGraph.object.$id"]).copy(locked=false).encode() } } }
            GlowButton("Ungroup") { vm.executeArCadValues("Ungroup CAD objects") { it-key } }
        }
    }
    if(error.isNotBlank()) Text(error,color=Color(0xFFFFBF69))
}
