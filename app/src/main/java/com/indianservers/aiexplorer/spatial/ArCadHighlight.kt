package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.arengine.interaction.*

fun arCadHighlight(scene: SpatialRenderScene, selection: ArSelectionState, hover:Boolean=false): SpatialRenderScene {
    val hit=selection.subObject?.takeIf { it.kind != ArSubObjectKind.Whole && it.subObjectIndex!=null } ?: return scene
    val source=scene.primitives.firstOrNull { it.id==hit.objectId } ?: return scene
    val g=source.geometry; val index=hit.subObjectIndex!!
    val selected=runCatching { ArCadTopology.vertices(g,hit.kind,index) }.getOrNull() ?: return scene
    val geometry=when(hit.kind) {
        ArSubObjectKind.Face -> g.copy(triangles=ArCadTopology.face(g,index).flatMap { g.triangles.drop(it*3).take(3) },lines=emptyList())
        ArSubObjectKind.Edge -> g.copy(triangles=emptyList(),lines=listOf(ArCadTopology.edges(g)[index]))
        else -> SpatialGeometry(selected.map(g.vertices::get),pointRadius=.08)
    }
    return scene.copy(primitives=scene.primitives+source.copy(id=if(hover) "cad-hover" else "cad-selection",geometry=geometry,material=SpatialMaterial("selected ${hit.kind}",if(hover) listOf(.5f,1f,.9f,.55f) else listOf(1f,.75f,.15f,.9f),emissive=.5f),selectable=false,metadata=source.metadata+("filled" to "true")))
}
