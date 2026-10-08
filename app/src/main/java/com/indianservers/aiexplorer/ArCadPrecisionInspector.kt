package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.interaction.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadPrecisionInspector(vm: ExplorerViewModel, scene: SpatialRenderScene, selection: ArSelectionState) {
    val obj=scene.primitives.firstOrNull { it.id==selection.primaryObjectId }
    if(obj==null) { Text("Select an object first",color=Color.White); return }
    val hit=selection.subObject
    val kind=hit?.kind ?: ArSubObjectKind.Whole
    val index=hit?.subObjectIndex ?: 0
    var coordinates by remember(obj.id,kind,index) { mutableStateOf("0, 0, 0") }
    var rotation by remember(obj.id,kind,index) { mutableStateOf("0, 0, 0") }
    var amount by remember(obj.id,kind,index) { mutableStateOf("1") }
    var error by remember(obj.id,kind,index) { mutableStateOf("") }
    fun number()=ArCadTopology.number(amount)
    fun triple(value: String): Vec3 { val p=value.split(',').map(ArCadTopology::number); require(p.size==3) { "Enter three expressions separated by commas" }; return Vec3(p[0],p[1],p[2]) }
    fun apply(action: () -> Unit) { runCatching(action).onSuccess { error="" }.onFailure { error=it.message ?: "Invalid edit" } }
    val entity=ArCadSelectionManager.resolve(scene,selection)
    Text("${obj.label} · ${entity?.kind?.name ?: kind.name}${if(kind==ArSubObjectKind.Whole) "" else " ${index+1}"}",color=Color.White,fontSize=14.sp)
    runCatching { ArCadTopology.inspector(obj.geometry,kind,index) }.getOrNull()?.forEach { (key,value) -> Text("$key: $value",color=Color(0xFFB7CAD7),fontSize=12.sp) }
    Text("Local math units · expressions such as π/2 and sqrt(2) are supported",color=Color(0xFFB7CAD7),fontSize=11.sp)
    if(vm.state.arGraphObject(obj.id).locked) {
        Text("Locked: unlock in Scene explorer to edit",color=Color(0xFFFFBF69))
        vm.state.arCadNodes()[obj.id]?.let { n -> Text(arCadEquationAndMeasurements(n,obj.geometry,vm.state.arGraphObject(obj.id),analytic=!vm.state.labSessionValues.containsKey("arCad.mesh.${obj.id}")).entries.joinToString("\n") { "${it.key}: ${it.value}" },color=Color.White) }
        return
    }
    OutlinedTextField(coordinates,{coordinates=it},label={Text("X, Y, Z / offset")},singleLine=true)
    OutlinedTextField(rotation,{rotation=it},label={Text("Rotation X, Y, Z (degrees)")},singleLine=true)
    OutlinedTextField(amount,{amount=it},label={Text(if(kind==ArSubObjectKind.Edge) "Length / scale" else "Offset / scale")},singleLine=true)
    FlowRow {
        GlowButton("Move") { apply {
            val delta=triple(coordinates)
            if(kind==ArSubObjectKind.Whole) vm.updateArGraphObject(obj.id,"Move CAD object") { it.copy(position=it.position+delta) }
            else vm.commitArCadGeometry(obj.id,ArCadTopology.move(obj.geometry,kind,index,delta),"Move CAD ${kind.name}")
        } }
        GlowButton("Rotate") { apply {
            val angle=triple(rotation)
            if(kind==ArSubObjectKind.Whole) vm.updateArGraphObject(obj.id,"Rotate CAD object") { it.copy(rotation=it.rotation+angle) }
            else vm.commitArCadGeometry(obj.id,ArCadTopology.move(obj.geometry,kind,index,Vec3(0.0,0.0,0.0),angle),"Rotate CAD ${kind.name}")
        } }
        GlowButton("Resize") { apply {
            val factor=number(); require(factor>0)
            if(kind==ArSubObjectKind.Whole) vm.updateArGraphObject(obj.id,"Scale CAD object") { it.copy(scale=(it.scale*factor).coerceIn(.1,10.0)) }
            else vm.commitArCadGeometry(obj.id,ArCadTopology.move(obj.geometry,kind,index,Vec3(0.0,0.0,0.0),scale=factor),"Resize CAD ${kind.name}")
        } }
    }
    if(kind==ArSubObjectKind.Vertex) GlowButton("Set exact coordinates") { apply { val delta=triple(coordinates)-obj.geometry.vertices[index]; vm.commitArCadGeometry(obj.id,ArCadTopology.move(obj.geometry,kind,index,delta),"Set exact vertex") } }
    if(kind==ArSubObjectKind.Edge) FlowRow {
        GlowButton("Set exact length") { apply {
            val edge=ArCadTopology.edges(obj.geometry)[index]; val a=obj.geometry.vertices[edge.first]; val b=obj.geometry.vertices[edge.second]; val length=number(); require(length>0 && (b-a).magnitude()>1e-10)
            val center=(a+b)*.5; val direction=(b-a).normalized()
            vm.commitArCadGeometry(obj.id,obj.geometry.copy(vertices=obj.geometry.vertices.mapIndexed { i,p -> when(i) { edge.first -> center-direction*(length*.5); edge.second -> center+direction*(length*.5); else -> p } }),"Set exact edge length")
        } }
        GlowButton("Midpoint") { apply { vm.commitArCadGeometry(obj.id,ArCadTopology.divide(obj.geometry,index,.5),"Divide CAD edge") } }
        GlowButton("Divide at ratio") { apply { vm.commitArCadGeometry(obj.id,ArCadTopology.divide(obj.geometry,index,number()),"Divide CAD edge") } }

    }
    if(kind==ArSubObjectKind.Face) FlowRow {
        GlowButton("Offset face") { apply {
            val f=obj.geometry.triangles.drop(index*3).take(3); val n=AnalyticGeometry3D.cross(obj.geometry.vertices[f[1]]-obj.geometry.vertices[f[0]],obj.geometry.vertices[f[2]]-obj.geometry.vertices[f[0]]).normalized()
            vm.commitArCadGeometry(obj.id,ArCadTopology.move(obj.geometry,kind,index,n*number()),"Offset CAD face")
        } }
        GlowButton("Extrude") { apply { val result=SpatialMeshEditor.extrude(EditableSpatialMesh(obj.geometry.vertices,obj.geometry.triangles),ArCadTopology.face(obj.geometry,index),number()); vm.commitArCadGeometry(obj.id,SpatialGeometry(result.mesh.vertices,result.mesh.triangles,SpatialMeshEditor.edges(result.mesh)),"Extrude CAD face") } }
    }
    if(kind!=ArSubObjectKind.Whole) {
        var reference by remember(obj.id) { mutableStateOf("") }
        var constraintKind by remember(obj.id) { mutableStateOf(ArCadConstraintKind.FixedLength) }
        Text("Persistent geometric relationships",color=Color.White)
        FlowRow { ArCadConstraintKind.entries.forEach { c -> GlowButton(c.name) { constraintKind=c } } }
        OutlinedTextField(reference,{reference=it},label={Text("Reference vertex indices (1 based, comma separated)")})
        GlowButton("Add constraint") { apply {
            val target=ArCadTopology.vertices(obj.geometry,kind,index).sorted()
            val refs=reference.split(',').map(String::trim).filter(String::isNotBlank).map { it.toInt()-1 }
            val c=ArCadConstraint(java.util.UUID.randomUUID().toString(),constraintKind,target,refs,number())
            vm.addArCadConstraint(obj.id,obj.geometry,c)
        } }
        vm.arCadConstraints(obj.id).forEach { c -> Text(c.kind.name,color=Color.White); GlowButton("Remove ${c.kind}") { vm.removeArCadConstraint(obj.id,c.id) } }
    }
    val node=vm.state.arCadNodes()[obj.id]?.let { n -> obj.metadata["radius"]?.takeIf(String::isNotBlank)?.let { n.copy(parameters=n.parameters+("radius" to it)) } ?: n }
    if(node!=null) {
        ArCadAnalyticTools(vm,node,obj.geometry) { error=it }
        var draft by remember(node.encode()) { mutableStateOf(node.parameters) }
        Text(if(vm.state.labSessionValues.containsKey("arCad.mesh.${obj.id}")) "Source parameters (Apply rebuilds mesh edits)" else "Analytic parameters / equations",color=Color.White)
        ArCadParameterFields(draft,onChange={ draft=it },onLive={ next -> draft=next; apply { vm.previewArCadParameters(node.id,next) } },onLiveFinished={ vm.endArGraphObjectGesture() })
        GlowButton("Apply parameters") { apply { vm.upsertArCadNode(node.copy(parameters=draft)) } }
        Text(arCadEquationAndMeasurements(node,obj.geometry,vm.state.arGraphObject(obj.id),analytic=vm.state.labSessionValues["arCad.mesh.${obj.id}"]==null).entries.joinToString("\n") { "${it.key}: ${it.value}" },color=Color(0xFFB7CAD7),fontSize=12.sp)
    }
    if(error.isNotBlank()) Text(error,color=Color(0xFFFFBF69),fontSize=12.sp)
}
