package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.core.InteractiveParameterEngine

fun arCadDefaults(type: ArCadType): Map<String,String> {
    val common=linkedMapOf("x" to "0","y" to "0","z" to "0")
    common.putAll(when(type) {
        ArCadType.Line,ArCadType.Segment,ArCadType.Ray,ArCadType.Vector -> mapOf("end.x" to "2","end.y" to "1","end.z" to "0")
        ArCadType.Plane -> mapOf("nx" to "0","ny" to "0","nz" to "1","size" to "2")
        ArCadType.Sphere,ArCadType.Circle,ArCadType.Triangle,ArCadType.Polygon -> mapOf("radius" to "1","sides" to "5")
        ArCadType.Cube,ArCadType.Cuboid,ArCadType.Cylinder,ArCadType.Cone,ArCadType.Prism,ArCadType.Pyramid -> mapOf("width" to "2","height" to "2","depth" to "2","radius" to "1")
        ArCadType.Curve -> mapOf("expressionX" to "cos(t)","expressionY" to "sin(t)","expressionZ" to "t/4","tMin" to "0","tMax" to "2*pi","samples" to "48","thickness" to "0.03","parameterT" to "0")
        ArCadType.FunctionSurface -> mapOf("expressionZ" to "a*(x^2+y^2)+c","parameter.a" to "1","parameter.c" to "0","uMin" to "-3","uMax" to "3","vMin" to "-3","vMax" to "3","samples" to "32","opacity" to "0.8","wireframe" to "true","filled" to "true")
        ArCadType.ParametricSurface -> mapOf("expressionX" to "u","expressionY" to "v","expressionZ" to "sin(u)*cos(v)","uMin" to "-3","uMax" to "3","vMin" to "-3","vMax" to "3","samples" to "32","opacity" to "0.8","wireframe" to "true","filled" to "true")
        ArCadType.ImplicitSurface -> mapOf("expressionF" to "x^2+y^2+z^2-1","uMin" to "-2","uMax" to "2","vMin" to "-2","vMax" to "2","wMin" to "-2","wMax" to "2","samples" to "20","opacity" to "0.8","wireframe" to "false","filled" to "true")
        ArCadType.VectorField -> mapOf("expressionX" to "-y","expressionY" to "x","expressionZ" to "0","density" to "5","bound" to "2","arrowSize" to "0.3","normalize" to "true")
        else -> emptyMap()
    }); return common
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadCreationPanel(vm: ExplorerViewModel) {
    var type by remember { mutableStateOf(ArCadType.Point) }
    var parameters by remember(type) { mutableStateOf(arCadDefaults(type)) }
    var dependencies by remember(type) { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    FlowRow { ArCadType.entries.forEach { candidate -> GlowButton(if(type==candidate) "• ${candidate.name}" else candidate.name) { type=candidate } } }
    ArCadParameterFields(parameters) { parameters=it }
    Text("Dependencies: point IDs for constructions; midpoint needs 2; intersection needs 2 line points + 3 plane points",color=Color(0xFFB7CAD7),fontSize=11.sp)
    Text(vm.state.arCadNodes().values.joinToString { "${it.type}: ${it.id}" },color=Color(0xFFB7CAD7),fontSize=10.sp)
    OutlinedTextField(dependencies,{dependencies=it},label={Text("Dependency IDs, comma separated")})
    GlowButton("Create ${type.name}") {
        runCatching {
            val node=ArCadNode("cad-${java.util.UUID.randomUUID()}",type,parameters,dependencies.split(',').map(String::trim).filter(String::isNotBlank))
            vm.upsertArCadNode(node)
        }.onSuccess { error="Created ${type.name}" }.onFailure { error=it.message ?: "Invalid construction" }
    }
    if(error.isNotBlank()) Text(error,color=Color(0xFFFFBF69),fontSize=12.sp)
}

@Composable
fun ArCadParameterFields(parameters: Map<String,String>, onLive: ((Map<String,String>) -> Unit)? = null, onLiveFinished:(()->Unit)?=null, onChange: (Map<String,String>) -> Unit) {
    parameters.forEach { (name,value) ->
        if(name in setOf("wireframe","filled","normalize")) Row { Text(name,color=Color.White); Switch(value!="false",{ onChange(parameters+(name to it.toString())) }) }
        else OutlinedTextField(value,{ onChange(parameters+(name to it)) },label={Text(when(name) { "uMin" -> "X / U domain min"; "uMax" -> "X / U domain max"; "vMin" -> "Y / V domain min"; "vMax" -> "Y / V domain max"; "samples" -> "Resolution / sample count"; "expressionF" -> "Implicit F(x,y,z) = 0"; "density" -> "Vectors per axis (max 10)"; "bound" -> "Field bounds +/-"; "arrowSize" -> "Arrow visual scale"; else -> name })},singleLine=true)
    }
    val detected=InteractiveParameterEngine.discover(parameters.filterKeys { it.startsWith("expression") }.values,independentVariables=setOf("x","y","z","u","v","t"))
    detected.forEach { p ->
        val key="parameter.${p.name}"; val current=runCatching { ArCadTopology.number(parameters[key] ?: p.value.toString()) }.getOrDefault(p.value)
        Text("${p.name} = $current",color=Color.White)
        Slider(current.toFloat().coerceIn(p.minimum.toFloat(),p.maximum.toFloat()),{ val next=parameters+(key to p.snap(it.toDouble()).toString()); onChange(next); onLive?.invoke(next) },valueRange=p.minimum.toFloat()..p.maximum.toFloat(),onValueChangeFinished=onLiveFinished)
    }
}
