package com.indianservers.aiexplorer

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.persistence.MathFileExchange
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadSceneToolsPanel(vm:ExplorerViewModel,scene:SpatialRenderScene,analysis:ArCadAnalysisResult,arView:ARCoreCompositorView?,depthAvailable:Boolean,onPresentation:()->Unit,onImageShare:(()->Unit)?=null) {
    val activity=androidx.activity.compose.LocalActivity.current; val scope=rememberCoroutineScope(); var status by remember { mutableStateOf("") }
    fun run(action:suspend ()->Unit) { scope.launch { runCatching { action() }.onFailure { status=it.message ?: "Action failed" } } }
    Text("Rendering quality",color=Color.White)
    FlowRow { listOf("AUTO","LOW","MEDIUM","HIGH").forEach { quality -> GlowButton(if((vm.state.labSessionValues["arAnalysis.quality"] ?: "AUTO")==quality) "Selected $quality" else quality) { vm.executeArCadValues("Rendering quality") { it+("arAnalysis.quality" to quality) } } } }
    Text("Auto adapts visual detail to frame time, thermal state, battery saver and distance. Mathematical geometry stays exact.",color=Color.White)
    Row { Text(if(depthAvailable) "Depth occlusion supported" else "Depth unavailable: regular mathematical rendering",color=Color.White); Switch(vm.state.spatialPlacement.depthOcclusionEnabled,{vm.setDepthOcclusion(it)},enabled=depthAvailable) }
    Row { Text("Lock edits during presentation",color=Color.White); Switch(vm.state.labSessionValues["arAnalysis.presentationLock"]!="false",{ vm.executeArCadValues("Presentation edit lock") { values -> values+("arAnalysis.presentationLock" to it.toString()) } }) }
    Text("Presentation measurement",color=Color.White)
    GlowButton("Hide presentation measurement") { vm.executeArCadValues("Presentation measurement") { it-"arAnalysis.presentationMeasurement" } }
    vm.state.labSessionValues.filterKeys { it.startsWith("arCad.measure.") }.forEach { (key,encoded) ->
        val measure=ArCadMeasurement.decode(encoded)
        GlowButton("Show ${measure.kind}: ${measure.ids.joinToString()}") { vm.executeArCadValues("Presentation measurement") { it+("arAnalysis.presentationMeasurement" to key) } }
    }
    GlowButton("Presentation mode",onClick=onPresentation)
    Row { Text("Smart object labels",color=Color.White); Switch(vm.state.labSessionValues["arAnalysis.labels"]=="true",{ vm.executeArCadValues("Object labels") { values -> values+("arAnalysis.labels" to it.toString()) } }) }
    GlowButton("Save AR scene") { vm.saveWorkspace(); status="Scene saved with equations, construction, edits and world scale" }
    vm.savedWorkspaces.take(8).forEach { saved -> GlowButton("Restore ${saved.name}") { vm.restoreArScene(saved); status="Restored. Place a new AR anchor." } }
    GlowButton("Share scene project",enabled=activity!=null) { run { MathFileExchange.shareProject(activity!!,vm.state) } }
    GlowButton("Share scene image",enabled=activity!=null) { if(onImageShare!=null) onImageShare() else run { if(arView!=null) MathFileExchange.shareArImage(activity!!,arView) else MathFileExchange.sharePng(activity!!,vm.state) } }
    GlowButton("Share equations and measurements",enabled=activity!=null) { run {
        val report=buildString {
            appendLine("AR Space mathematical scene")
            vm.state.surfaceLayers.forEach { layer -> appendLine("${layer.id}: ${layer.kind} ${layer.expression}; domain ${layer.domain}") }
            vm.state.arCadNodes().values.forEach { node ->
                appendLine("${node.id}: ${node.type}"); node.parameters.forEach { (key,value) -> appendLine("  $key = $value") }
                scene.primitives.firstOrNull { it.id==node.id }?.let { obj -> arCadEquationAndMeasurements(node,obj.geometry,vm.state.arGraphObject(node.id),vm.state.labSessionValues["arCad.mesh.${node.id}"]==null).forEach { (key,value) -> appendLine("  $key: $value") } }
            }
            analysis.values.forEach { (key,value) -> appendLine("$key: $value") }
            vm.state.labSessionValues.filterKeys { it.startsWith("arCad.measure.") }.values.forEach { encoded -> val measure=ArCadMeasurement.decode(encoded); appendLine("${measure.kind}: ${runCatching { measure.value(scene) }.getOrNull()} ${if(measure.angular) "radians" else "units"}") }
        }
        MathFileExchange.shareArReport(activity!!,report)
    } }
    Text("History: ${vm.universalHistoryDepth} / ${vm.arHistoryTimeline.size}",color=Color.White)
    GlowButton("Initial history state") { vm.jumpArHistory(0) }
    vm.arHistoryTimeline.forEachIndexed { index,label -> GlowButton("${index+1}: $label") { vm.jumpArHistory(index+1) } }
    if(status.isNotBlank()) Text(status,color=Color.White)
}
