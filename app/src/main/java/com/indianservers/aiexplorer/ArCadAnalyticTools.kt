package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadAnalyticTools(vm:ExplorerViewModel,node:ArCadNode,geometry:SpatialGeometry,onError:(String)->Unit) {
    var input by remember(node.id) { mutableStateOf("") }
    fun apply(action:()->Unit) { runCatching(action).onFailure { onError(it.message ?: "Invalid analytic edit") }.onSuccess { onError("") } }
    fun values():List<Double> = input.split(',').map(ArCadTopology::number)
    if(node.type==ArCadType.Sphere || node.type==ArCadType.Plane) {
        Text(if(node.type==ArCadType.Sphere) "Local sphere equation: center a,b,c and radius r" else "Local plane equation: ax+by+cz=d",color=Color.White)
        OutlinedTextField(input,{input=it},label={Text(if(node.type==ArCadType.Sphere) "a, b, c, r" else "a, b, c, d")})
        GlowButton("Apply equation coefficients") { apply {
            require(node.dependencies.isEmpty()) { "Edit the dependency points for this construction" }
            val v=values(); require(v.size==4)
            val p=if(node.type==ArCadType.Sphere) { require(v[3]>0); mapOf("x" to v[0].toString(),"y" to v[1].toString(),"z" to v[2].toString(),"radius" to v[3].toString()) }
            else { val n=Vec3(v[0],v[1],v[2]); require(n.dot(n)>1e-12); val c=n*(v[3]/n.dot(n)); mapOf("x" to c.x.toString(),"y" to c.y.toString(),"z" to c.z.toString(),"nx" to n.x.toString(),"ny" to n.y.toString(),"nz" to n.z.toString()) }
            vm.upsertArCadNode(node.copy(parameters=node.parameters+p))
        } }
        if(node.type==ArCadType.Sphere) {
            var reference by remember(node.id) { mutableStateOf("") }
            OutlinedTextField(reference,{reference=it},label={Text("Reference sphere ID")})
            FlowRow {
                GlowButton("Equal radius") { apply { vm.upsertArCadNode(node.copy(parameters=node.parameters+("radiusReference" to reference.trim()))) } }
                GlowButton("External tangent") { apply { vm.upsertArCadNode(node.copy(parameters=node.parameters+("tangentReference" to reference.trim()))) } }
                GlowButton("Remove analytic relations") { apply { val refs=listOfNotNull(node.parameters["radiusReference"],node.parameters["tangentReference"]); vm.upsertArCadNode(node.copy(parameters=node.parameters-"radiusReference"-"tangentReference",dependencies=node.dependencies-refs.toSet())) } }
            }
        }
    }
    if(node.type==ArCadType.Vector && geometry.vertices.size>=2) {
        val world=arCadWorldGeometry(geometry,vm.state.arGraphObject(node.id)); val a=world.vertices[1]-world.vertices[0]
        var reference by remember(node.id) { mutableStateOf("") }; var result by remember { mutableStateOf("") }
        GlowButton("Normalize vector") { apply {
            require(node.dependencies.isEmpty()) { "Edit the endpoint dependency instead" }
            val start=geometry.vertices[0]; val d=(geometry.vertices[1]-start).normalized(); require(d.magnitude()>1e-10)
            val end=start+d
            vm.upsertArCadNode(node.copy(parameters=node.parameters+mapOf("end.x" to end.x.toString(),"end.y" to end.y.toString(),"end.z" to end.z.toString())))
        } }
        OutlinedTextField(reference,{reference=it},label={Text("Second vector components X, Y, Z")})
        GlowButton("Calculate vectors") { apply {
            val v=reference.split(',').map(ArCadTopology::number); require(v.size==3); val b=Vec3(v[0],v[1],v[2])
            result=(ArCadVectorMath.calculate(a,b).entries.map { "${it.key}: ${it.value}" }+listOf("Dot product: ${a.dot(b)}","Angle (radians): ${ArCadVectorMath.angle(a,b)}")).joinToString("\n")
        } }
        if(result.isNotEmpty()) Text(result,color=Color.White)
    }
    if(node.type==ArCadType.Curve) {
        val min=ArCadTopology.number(node.parameters["tMin"] ?: "0"); val max=ArCadTopology.number(node.parameters["tMax"] ?: "2*pi")
        val t=ArCadTopology.number(node.parameters["parameterT"] ?: "0").coerceIn(min,max)
        Text("Moving point t=$t",color=Color.White)
        Slider(t.toFloat(),{ value -> apply { vm.previewArCadParameters(node.id,node.parameters+("parameterT" to value.toString())) } },valueRange=min.toFloat()..max.toFloat(),onValueChangeFinished={ vm.endArGraphObjectGesture() })
    }
}
