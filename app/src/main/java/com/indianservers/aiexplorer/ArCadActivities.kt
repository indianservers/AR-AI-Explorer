package com.indianservers.aiexplorer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.spatial.*

enum class ArCadActivity(val instruction:String,val type:ArCadType) {
    XYZ("Move a point along X, Y and Z. Use the precision inspector to compare coordinates.",ArCadType.Point),
    Vectors("Change vector endpoints; compare components, magnitude and projection.",ArCadType.Vector),
    Planes("Move and rotate the plane; inspect its normal and equation.",ArCadType.Plane),
    Sphere("Change the sphere radius and compare area and volume.",ArCadType.Sphere),
    Conics("Select the plane as the slice. Rotate it to compare circle, ellipse, parabola and hyperbola classifications.",ArCadType.Cone),
    Gradient("Move the analysis point on z=x^2+y^2; compare gradient and directional derivative.",ArCadType.FunctionSurface),
    Tangent("Move the analysis point; observe the tangent plane and normal change.",ArCadType.FunctionSurface),
    VectorField("Inspect F=(-y,x,0). Change normalization, arrow scale, density and bounds.",ArCadType.VectorField),
    Implicit("Inspect x^2+y^2+z^2=1; try a torus expression in the source parameters.",ArCadType.ImplicitSurface)
}
fun launchArCadActivity(vm:ExplorerViewModel,activity:ArCadActivity) {
    val id="activity-${java.util.UUID.randomUUID()}"
    val node=ArCadNode(id,activity.type,arCadDefaults(activity.type))
    val nodes=mutableListOf(node); val settings=mutableMapOf<String,String>("arAnalysis.activity" to activity.name)
    if(activity==ArCadActivity.Conics) {
        val plane=ArCadNode("$id-plane",ArCadType.Plane,arCadDefaults(ArCadType.Plane)+mapOf("nx" to "0","ny" to "1","nz" to "0"))
        nodes+=plane; settings["arAnalysis.slice"]=plane.id
    }
    if(activity in setOf(ArCadActivity.Gradient,ArCadActivity.Tangent)) {
        settings.putAll(mapOf("arAnalysis.surface" to id,"arAnalysis.x" to "0.5","arAnalysis.y" to "0.5","arAnalysis.gradient" to "true","arAnalysis.tangent" to "true","arAnalysis.normal" to "true"))
    }
    vm.executeArCadValues("Start ${activity.name} activity") { values -> values+nodes.associate { "arCad.node.${it.id}" to it.encode() }+settings }
}
@Composable
fun ArCadActivitiesPanel(vm:ExplorerViewModel) {
    Text("Each activity adds editable mathematical objects to this scene. Undo removes the activity in one step.",color=Color.White)
    ArCadActivity.entries.forEach { activity -> GlowButton(activity.name) { launchArCadActivity(vm,activity) }; Text(activity.instruction,color=Color.White) }
}
