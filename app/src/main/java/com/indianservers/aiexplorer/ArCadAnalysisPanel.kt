package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.spatial.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadAnalysisPanel(vm:ExplorerViewModel,scene:SpatialRenderScene,result:ArCadAnalysisResult) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    fun save(key:String,value:String) {
        focus.clearFocus()
        vm.executeArCadValues("Change analysis") { it+(key to value) }
    }
    val settings=vm.state.labSessionValues
    var slicePlaying by remember { mutableStateOf(false) }
    val sliceId=settings["arAnalysis.slice"]
    LaunchedEffect(slicePlaying,sliceId) {
        if(slicePlaying && !sliceId.isNullOrBlank()) {
            val plane=scene.primitives.firstOrNull { it.id==sliceId } ?: return@LaunchedEffect
            val vertices=arCadWorldGeometry(plane.geometry,vm.state.arGraphObject(sliceId)).vertices
            val normal=com.indianservers.aiexplorer.core.AnalyticGeometry3D.cross(vertices[1]-vertices[0],vertices[2]-vertices[0]).normalized()
            vm.beginArGraphObjectGesture()
            var seconds=0.0
            try {
                while(vm.arGestureInProgress) {
                    seconds+=.1
                    vm.previewArGraphObject(sliceId,normal*kotlin.math.sin(seconds*kotlin.math.PI/3),com.indianservers.aiexplorer.core.Vec3(0.0,0.0,0.0),1.0)
                    kotlinx.coroutines.delay(100)
                }
            } finally { vm.endArGraphObjectGesture(false); slicePlaying=false }
        }
    }
    GlowButton(if(slicePlaying) "Stop slice animation" else "Animate slice (+/- 1 unit)",enabled=!sliceId.isNullOrBlank() && !vm.state.arGraphObject(sliceId ?: "").locked) { slicePlaying=!slicePlaying }

    Text("Slice with a CAD plane. Move or rotate it using the same touch, hand and precision controls as other objects.",color=Color.White)
    FlowRow {
        GlowButton("Slice off") { save("arAnalysis.slice","") }
        scene.primitives.filter { it.metadata["cadType"]=="Plane" && it.visible }.forEach { plane -> GlowButton("Slice: ${plane.label}") { save("arAnalysis.slice",plane.id) } }
    }
    Text("Explicit surface analysis",color=Color.White)
    FlowRow { scene.primitives.filter { it.id in vm.state.arExplicitAnalysisNodes() && it.visible }.forEach { obj -> GlowButton(obj.label) { save("arAnalysis.surface",obj.id) } }; GlowButton("Analysis off") { save("arAnalysis.surface","") } }
    listOf("x" to "Point local X","y" to "Point local Y","dx" to "Direction X","dy" to "Direction Y","dz" to "Direction Z","scale" to "Gradient visual scale").forEach { (name,label) ->
        val key="arAnalysis.$name"; var draft by remember(key,settings[key]) { mutableStateOf(settings[key] ?: when(name) { "dx" -> "1"; "scale" -> "0.5"; else -> "0" }) }
        var error by remember { mutableStateOf("") }
        OutlinedTextField(draft,{draft=it},label={Text(label)},singleLine=true)
        GlowButton("Apply $label") { runCatching { ArCadTopology.number(draft); save(key,draft) }.onSuccess { error="" }.onFailure { error=it.message ?: "Invalid number" } }
        if(error.isNotBlank()) Text(error,color=Color(0xFFFFBF69))
    }
    listOf("direction" to "Direction vector","gradient" to "Gradient vector","normalize" to "Normalize visual gradient","normal" to "Surface normal","tangent" to "Tangent plane").forEach { (key,label) -> Row { Text(label,color=Color.White); Switch(if(key=="direction") settings["arAnalysis.$key"]!="false" else settings["arAnalysis.$key"]=="true",{save("arAnalysis.$key",it.toString())}) } }
    val analyzed=scene.primitives.firstOrNull { it.id==settings["arAnalysis.surface"] }
    if(analyzed!=null) FlowRow {
        scene.primitives.filter { it.visible && it.metadata["cadType"]=="Vector" }.forEach { obj ->
            GlowButton("Direction: ${obj.label}") {
                val g=arCadWorldGeometry(obj.geometry,vm.state.arGraphObject(obj.id)); val d=g.vertices[1]-g.vertices[0]
                val t=analyzed.localTransform; val local=t.orientation.conjugate().rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(d.x,d.y,d.z))*(1.0/t.uniformScale)
                vm.executeArCadValues("Select derivative direction") { it+mapOf("arAnalysis.dx" to (local.x/t.axisScale.x).toString(),"arAnalysis.dy" to (local.y/t.axisScale.y).toString(),"arAnalysis.dz" to (local.z/t.axisScale.z).toString()) }
            }
        }
    }
    Text("Scalar field slice and level contour",color=Color.White)
    Row { Text("Show scalar slice",color=Color.White); Switch(settings["arAnalysis.scalar"]=="true",{save("arAnalysis.scalar",it.toString())}) }
    listOf("scalarExpression" to "Scalar F(x,y,z)","scalarZ" to "Slice Z","scalarBound" to "Slice bound","scalarContour" to "Contour level").forEach { (name,label) ->
        val key="arAnalysis.$name"; var draft by remember(key,settings[key]) { mutableStateOf(settings[key] ?: when(name) { "scalarExpression" -> "x^2+y^2+z^2"; "scalarBound" -> "2"; "scalarContour" -> "1"; else -> "0" }) }
        var error by remember { mutableStateOf("") }
        OutlinedTextField(draft,{draft=it},label={Text(label)},singleLine=true)
        GlowButton("Apply $label") { runCatching { if(name=="scalarExpression") com.indianservers.aiexplorer.core.ExpressionEngine().compile(draft) else ArCadTopology.number(draft); save(key,draft) }.onSuccess { error="" }.onFailure { error=it.message ?: "Invalid expression" } }
        if(error.isNotBlank()) Text(error,color=Color(0xFFFFBF69))
    }
    result.values.forEach { (label,value) -> Text("$label: $value",color=Color.White) }
}
